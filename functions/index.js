const { onCall, HttpsError } = require("firebase-functions/v2/https");
const { onDocumentWritten } = require("firebase-functions/v2/firestore");
const { initializeApp } = require("firebase-admin/app");
const { getFirestore, FieldValue, Timestamp } = require("firebase-admin/firestore");
const { getMessaging } = require("firebase-admin/messaging");
const {
  validateCouponRedemption,
  validateReservationTransition,
} = require("./validators");

initializeApp();

const DATABASE_ID = "ai-studio-android-nightcof-928c5e97-6c5f-41fd-a1cd-16fb89fcedb9";
const db = getFirestore(DATABASE_ID);

async function assertAdmin(uid) {
  if (!uid) throw new HttpsError("unauthenticated", "Authentication required.");
  const snap = await db.collection("admins").doc(uid).get();
  if (!snap.exists || snap.data()?.active !== true) {
    throw new HttpsError("permission-denied", "Admin privileges required.");
  }
}

async function assertAdminOrShopStaff(uid, shopId) {
  if (!uid) throw new HttpsError("unauthenticated", "Authentication required.");
  const adminSnap = await db.collection("admins").doc(uid).get();
  if (adminSnap.exists && adminSnap.data()?.active === true) return true;

  const staffSnap = await db.collection("shop_staff").doc(`${shopId}_${uid}`).get();
  if (staffSnap.exists && staffSnap.data()?.active === true) return true;

  throw new HttpsError("permission-denied", "Shop staff or admin privileges required.");
}

exports.redeemCoupon = onCall(async (request) => {
  const uid = request.auth?.uid;
  if (!uid) throw new HttpsError("unauthenticated", "Sign in required to redeem coupons.");

  const couponId = String(request.data?.couponId || "").trim();
  if (!/^[a-zA-Z0-9_-]{1,128}$/.test(couponId)) {
    throw new HttpsError("invalid-argument", "Invalid couponId.");
  }

  const couponRef = db.collection("coupons").doc(couponId);
  const redemptionRef = db.collection("coupon_redemptions").doc(`${couponId}_${uid}`);

  const result = await db.runTransaction(async (tx) => {
    const [couponSnap, redemptionSnap] = await Promise.all([
      tx.get(couponRef),
      tx.get(redemptionRef),
    ]);

    const coupon = couponSnap.exists ? couponSnap.data() : null;
    const expiresAtMillis = coupon?.expiresAt?.toMillis ? coupon.expiresAt.toMillis() : 0;
    const check = validateCouponRedemption({
      coupon: coupon ? { ...coupon, expiresAtMillis } : null,
      existingRedemption: redemptionSnap.exists ? redemptionSnap.data() : null,
      nowMillis: Date.now(),
    });

    if (!check.ok) {
      throw new HttpsError(check.code, check.reason);
    }

    const discountLabel =
      coupon.discountType === "PERCENTAGE"
        ? `${coupon.discountValue}% OFF`
        : `৳${coupon.discountValue} OFF`;

    tx.update(couponRef, {
      redeemedCount: FieldValue.increment(1),
      updatedAt: FieldValue.serverTimestamp(),
    });

    tx.set(
      redemptionRef,
      {
        couponId,
        userId: uid,
        shopId: coupon.shopId,
        code: coupon.code,
        status: "REDEEMED",
        discountLabel,
        savedAt: redemptionSnap.exists && redemptionSnap.data()?.savedAt
          ? redemptionSnap.data().savedAt
          : FieldValue.serverTimestamp(),
        redeemedAt: FieldValue.serverTimestamp(),
      },
      { merge: true }
    );

    return {
      status: "REDEEMED",
      code: coupon.code,
      discountLabel,
    };
  });

  return result;
});

exports.updateReservationStatus = onCall(async (request) => {
  const uid = request.auth?.uid;
  const reservationId = String(request.data?.reservationId || "").trim();
  const nextStatus = String(request.data?.status || "").trim();
  const staffNote = String(request.data?.staffNote || "").trim().slice(0, 300);

  if (!/^[a-zA-Z0-9_-]{1,128}$/.test(reservationId)) {
    throw new HttpsError("invalid-argument", "Invalid reservationId.");
  }

  const resRef = db.collection("reservations").doc(reservationId);
  const snap = await resRef.get();
  if (!snap.exists) {
    throw new HttpsError("not-found", "Reservation not found.");
  }
  const existing = snap.data();
  await assertAdminOrShopStaff(uid, existing.shopId);

  const transition = validateReservationTransition(existing.status, nextStatus);
  if (!transition.ok) {
    throw new HttpsError("failed-precondition", transition.reason);
  }

  await resRef.update({
    status: nextStatus,
    staffNote,
    updatedAt: FieldValue.serverTimestamp(),
  });

  // Notify reservation owner
  await db.collection("notifications").add({
    userId: existing.userId,
    category: "RESERVATION_UPDATE",
    title: `Reservation ${nextStatus}`,
    body: `${existing.shopName} (${existing.dateDhaka} at ${existing.timeSlot}) is now ${nextStatus}.`,
    targetRoute: "reservations",
    targetId: reservationId,
    read: false,
    createdAt: FieldValue.serverTimestamp(),
  });

  const prefSnap = await db.collection("user_preferences").doc(existing.userId).get();
  const pref = prefSnap.exists ? prefSnap.data() : null;
  if (pref?.optInReservations && pref?.fcmToken) {
    await getMessaging()
      .send({
        token: pref.fcmToken,
        notification: {
          title: `Reservation ${nextStatus}`,
          body: `${existing.shopName} on ${existing.dateDhaka} at ${existing.timeSlot}`,
        },
        data: {
          targetRoute: "reservations",
          targetId: reservationId,
          category: "RESERVATION_UPDATE",
        },
      })
      .catch(() => {});
  }

  return { ok: true, status: nextStatus };
});

exports.reviewVerificationRequest = onCall(async (request) => {
  const adminUid = request.auth?.uid;
  await assertAdmin(adminUid);

  const requestId = String(request.data?.requestId || "").trim();
  const approve = Boolean(request.data?.approve);
  if (!/^[a-zA-Z0-9_-]{1,128}$/.test(requestId)) {
    throw new HttpsError("invalid-argument", "Invalid requestId.");
  }

  const reqRef = db.collection("verification_requests").doc(requestId);
  await db.runTransaction(async (tx) => {
    const reqSnap = await tx.get(reqRef);
    if (!reqSnap.exists) {
      throw new HttpsError("not-found", "Verification request not found.");
    }
    const req = reqSnap.data();
    const newStatus = approve ? "Approved" : "Rejected";
    tx.update(reqRef, {
      status: newStatus,
      reviewedBy: adminUid,
      reviewedAt: FieldValue.serverTimestamp(),
      updatedAt: FieldValue.serverTimestamp(),
    });

    if (approve) {
      const shopRef = db.collection("shops").doc(req.shopId);
      const staffRef = db.collection("shop_staff").doc(`${req.shopId}_${req.requesterUid}`);
      tx.set(
        shopRef,
        {
          name: req.shopName,
          verified: true,
          verifiedAt: FieldValue.serverTimestamp(),
          verifiedBy: adminUid,
          ownerUid: req.requesterUid,
          visibility: "public",
          updatedAt: FieldValue.serverTimestamp(),
        },
        { merge: true }
      );
      tx.set(staffRef, {
        shopId: req.shopId,
        uid: req.requesterUid,
        role: "OWNER",
        active: true,
        assignedBy: adminUid,
        createdAt: FieldValue.serverTimestamp(),
        updatedAt: FieldValue.serverTimestamp(),
      });
    }
  });

  return { ok: true };
});

exports.sendPlatformNotification = onCall(async (request) => {
  const adminUid = request.auth?.uid;
  await assertAdmin(adminUid);

  const category = String(request.data?.category || "ANNOUNCEMENT").trim();
  const title = String(request.data?.title || "").trim().slice(0, 100);
  const body = String(request.data?.body || "").trim().slice(0, 500);
  const targetRoute = String(request.data?.targetRoute || "notifications").trim().slice(0, 120);
  const targetId = String(request.data?.targetId || "").trim().slice(0, 128);
  const targetTopic = String(request.data?.topic || "").trim();

  if (title.length < 2 || body.length < 2) {
    throw new HttpsError("invalid-argument", "Title and body are required.");
  }

  await db.collection("notifications").add({
    userId: "broadcast",
    category,
    title,
    body,
    targetRoute,
    targetId,
    read: false,
    createdAt: FieldValue.serverTimestamp(),
  });

  if (targetTopic) {
    await getMessaging()
      .send({
        topic: targetTopic,
        notification: { title, body },
        data: { targetRoute, targetId, category },
      })
      .catch(() => {});
  }

  return { ok: true };
});

exports.syncReviewVoteCount = onDocumentWritten(
  {
    document: "review_votes/{voteId}",
    database: DATABASE_ID,
  },
  async (event) => {
    const data = event.data?.after?.data() || event.data?.before?.data();
    if (!data?.reviewId) return;
    const reviewId = data.reviewId;
    const votesSnap = await db
      .collection("review_votes")
      .where("reviewId", "==", reviewId)
      .get();
    await db.collection("reviews").doc(reviewId).update({
      helpfulCount: votesSnap.size,
    }).catch(() => {});
  }
);

exports.syncReviewReportCount = onDocumentWritten(
  {
    document: "review_reports/{reportId}",
    database: DATABASE_ID,
  },
  async (event) => {
    const data = event.data?.after?.data();
    if (!data?.reviewId) return;
    const reportsSnap = await db
      .collection("review_reports")
      .where("reviewId", "==", data.reviewId)
      .get();
    const updates = { reportCount: reportsSnap.size };
    if (reportsSnap.size >= 3) {
      updates.visibility = "flagged";
    }
    await db.collection("reviews").doc(data.reviewId).update(updates).catch(() => {});
  }
);
