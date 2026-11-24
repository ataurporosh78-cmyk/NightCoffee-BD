const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";
const ADMIN_UID = "admin_999";
const STAFF_UID = "staff_777";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

async function seedAdminAndStaff() {
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore();
    await db.collection("admins").doc(ADMIN_UID).set({
      active: true,
      role: "SUPER_ADMIN",
    });
    await db.collection("shop_staff").doc(`north-end-banani_${STAFF_UID}`).set({
      shopId: "north-end-banani",
      uid: STAFF_UID,
      role: "OWNER",
      active: true,
    });
  });
}

test("Legacy community_shops and shop_reviews security preserved", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("community_shops").where("visibility", "==", "public").get());
  await assertFails(unauthDb.collection("shop_reviews").where("visibility", "==", "public").get());

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const created = new Date(Date.now() - 5000);
  await assertSucceeds(
    aliceDb.collection("community_shops").doc("shop_1").set({
      ownerId: ALICE_UID,
      visibility: "public",
      name: "Bashundhara Night Brew",
      area: "Bashundhara",
      address: "Block C, Bashundhara R/A",
      openTime: "4:00 PM",
      closeTime: "03:00 AM",
      popularItems: ["Iced Spanish Latte"],
      priceRange: "250 - 450 BDT",
      phone: "+8801711223344",
      familyFriendly: true,
      createdAt: created,
      updatedAt: created,
    })
  );
  await assertSucceeds(aliceDb.collection("community_shops").where("visibility", "==", "public").get());
  await assertFails(aliceDb.collection("community_shops").get());
});

test("Verified badge and shop staff privilege escalation is strictly blocked", async () => {
  await seedAdminAndStaff();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const staffDb = testEnv.authenticatedContext(STAFF_UID).firestore();
  const adminDb = testEnv.authenticatedContext(ADMIN_UID).firestore();
  const created = new Date(Date.now() - 10000);

  // Regular user cannot create an admin doc or staff doc or verified shop
  await assertFails(aliceDb.collection("admins").doc(ALICE_UID).set({ active: true }));
  await assertFails(
    aliceDb.collection("shop_staff").doc(`north-end-banani_${ALICE_UID}`).set({
      shopId: "north-end-banani",
      uid: ALICE_UID,
      active: true,
    })
  );
  await assertFails(
    aliceDb.collection("shops").doc("north-end-banani").set({
      name: "North End",
      area: "Banani",
      address: "Road 11, Banani",
      openTime: "8:00 AM",
      closeTime: "1:00 AM",
      priceRange: "৳250 - ৳550",
      phone: "+8801711",
      familyFriendly: true,
      acAvailable: true,
      wifiAvailable: true,
      rooftopAvailable: false,
      verified: true,
      officialOverride: false,
      visibility: "public",
      createdAt: created,
      updatedAt: created,
    })
  );

  // Admin creates shop overlay
  await assertSucceeds(
    adminDb.collection("shops").doc("north-end-banani").set({
      name: "North End Coffee Roasters",
      area: "Banani",
      address: "Road 11, Banani",
      openTime: "8:00 AM",
      closeTime: "1:00 AM",
      priceRange: "৳250 - ৳550",
      phone: "+8801711002411",
      familyFriendly: true,
      acAvailable: true,
      wifiAvailable: true,
      rooftopAvailable: false,
      verified: true,
      verifiedBy: ADMIN_UID,
      ownerUid: STAFF_UID,
      liveStatus: "OPEN_NOW",
      officialOverride: true,
      visibility: "public",
      createdAt: created,
      updatedAt: created,
    })
  );

  // Staff can update liveStatus, but CANNOT alter verified flag
  await assertSucceeds(
    staffDb.collection("shops").doc("north-end-banani").update({
      liveStatus: "CLOSING_SOON",
      liveStatusUpdatedBy: STAFF_UID,
      officialOverride: true,
      updatedAt: new Date(Date.now() - 1000),
    })
  );
  await assertFails(
    staffDb.collection("shops").doc("north-end-banani").update({
      verified: false,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Guest (unauthenticated) can browse public shop overlays & menus
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertSucceeds(unauthDb.collection("shops").where("visibility", "==", "public").get());
});

test("Status reports, digital menus, and crowd reports follow role & unverified boundaries", async () => {
  await seedAdminAndStaff();
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const staffDb = testEnv.authenticatedContext(STAFF_UID).firestore();
  const now = new Date(Date.now() - 2000);
  const later = new Date(Date.now() + 2 * 60 * 60 * 1000);

  // Customer cannot self-verify a status report
  await assertFails(
    aliceDb.collection("shop_status_reports").doc("rep_bad").set({
      shopId: "north-end-banani",
      userId: ALICE_UID,
      reporterName: "Alice",
      reportedStatus: "CLOSED",
      note: "Looks closed",
      staffReport: false,
      verificationState: "VERIFIED",
      visibility: "public",
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(
    aliceDb.collection("shop_status_reports").doc("rep_ok").set({
      shopId: "north-end-banani",
      userId: ALICE_UID,
      reporterName: "Alice",
      reportedStatus: "CLOSED",
      note: "Shutters down early tonight",
      staffReport: false,
      verificationState: "UNVERIFIED",
      visibility: "public",
      createdAt: now,
      updatedAt: now,
    })
  );

  // Staff can publish menu items and guests can browse them without login
  await assertFails(
    aliceDb.collection("menu_items").doc("item_1").set({
      shopId: "north-end-banani",
      category: "Espresso",
      name: "Double Espresso",
      description: "Rich shot",
      imageUrl: "",
      priceBdt: 220,
      available: true,
      special: false,
      visibility: "public",
      updatedBy: ALICE_UID,
      createdAt: now,
      updatedAt: now,
    })
  );
  await assertSucceeds(
    staffDb.collection("menu_items").doc("item_1").set({
      shopId: "north-end-banani",
      category: "Espresso",
      name: "Double Espresso",
      description: "Rich double ristretto",
      imageUrl: "",
      priceBdt: 220,
      available: true,
      special: true,
      visibility: "public",
      updatedBy: STAFF_UID,
      createdAt: now,
      updatedAt: now,
    })
  );
  await assertSucceeds(unauthDb.collection("menu_items").where("visibility", "==", "public").get());

  // Crowd report valid creation
  await assertSucceeds(
    aliceDb.collection("crowd_reports").doc("crowd_1").set({
      shopId: "north-end-banani",
      userId: ALICE_UID,
      level: "BUSY",
      note: "Most indoor tables taken",
      flaggedIncorrectCount: 0,
      verificationState: "ACTIVE",
      visibility: "public",
      createdAt: now,
      expiresAt: later,
    })
  );
});

test("Coupons prevent customer forgery and duplicate redemptions", async () => {
  await seedAdminAndStaff();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const adminDb = testEnv.authenticatedContext(ADMIN_UID).firestore();
  const now = new Date(Date.now() - 2000);
  const expires = new Date(Date.now() + 86400000);

  await assertSucceeds(
    adminDb.collection("coupons").doc("coup_1").set({
      shopId: "north-end-banani",
      shopName: "North End Coffee Roasters",
      code: "NIGHT15",
      title: "Midnight 15% Off",
      description: "15% off espresso drinks after 10 PM",
      discountType: "PERCENTAGE",
      discountValue: 15,
      minSpendBdt: 300,
      maxUsageLimit: 100,
      redeemedCount: 0,
      terms: "Valid after 10 PM BDT.",
      expiresAt: expires,
      active: true,
      visibility: "public",
      createdBy: ADMIN_UID,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Customer cannot directly increment redeemedCount or forge REDEEMED status
  await assertFails(
    aliceDb.collection("coupons").doc("coup_1").update({
      redeemedCount: 1,
      updatedAt: new Date(Date.now() - 500),
    })
  );
  await assertFails(
    aliceDb.collection("coupon_redemptions").doc(`coup_1_${ALICE_UID}`).set({
      couponId: "coup_1",
      userId: ALICE_UID,
      shopId: "north-end-banani",
      code: "NIGHT15",
      status: "REDEEMED",
      discountLabel: "15% OFF",
      savedAt: now,
    })
  );

  // Saving a coupon under own deterministic ID succeeds
  await assertSucceeds(
    aliceDb.collection("coupon_redemptions").doc(`coup_1_${ALICE_UID}`).set({
      couponId: "coup_1",
      userId: ALICE_UID,
      shopId: "north-end-banani",
      code: "NIGHT15",
      status: "SAVED",
      discountLabel: "15% OFF",
      savedAt: now,
    })
  );
});

test("Reservations protect PII and require staff/admin to confirm", async () => {
  await seedAdminAndStaff();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const staffDb = testEnv.authenticatedContext(STAFF_UID).firestore();
  const now = new Date(Date.now() - 2000);

  // Customer cannot create a pre-confirmed reservation
  await assertFails(
    aliceDb.collection("reservations").doc("res_bad").set({
      shopId: "north-end-banani",
      shopName: "North End",
      userId: ALICE_UID,
      guestName: "Alice",
      contactPhone: "+8801711111111",
      dateDhaka: "2026-12-01",
      timeSlot: "10:30 PM",
      guestCount: 2,
      specialRequest: "Window seat",
      status: "Confirmed",
      staffNote: "",
      createdAt: now,
      updatedAt: now,
    })
  );

  // Pending reservation succeeds
  await assertSucceeds(
    aliceDb.collection("reservations").doc("res_1").set({
      shopId: "north-end-banani",
      shopName: "North End Coffee Roasters",
      userId: ALICE_UID,
      guestName: "Alice",
      contactPhone: "+8801711111111",
      dateDhaka: "2026-12-01",
      timeSlot: "10:30 PM",
      guestCount: 2,
      specialRequest: "Quiet corner",
      status: "Pending",
      staffNote: "",
      createdAt: now,
      updatedAt: now,
    })
  );

  // Other customer Bob cannot read Alice's reservation (protects phone PII)
  await assertFails(bobDb.collection("reservations").doc("res_1").get());

  // Staff can confirm; owner can cancel
  await assertSucceeds(
    staffDb.collection("reservations").doc("res_1").update({
      status: "Confirmed",
      staffNote: "Table 4 reserved",
      updatedAt: new Date(Date.now() - 500),
    })
  );
  await assertSucceeds(
    aliceDb.collection("reservations").doc("res_1").update({
      status: "Cancelled",
      updatedAt: new Date(Date.now() - 100),
    })
  );
});

test("Support tickets & verification requests remain private to owner and admin", async () => {
  await seedAdminAndStaff();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const adminDb = testEnv.authenticatedContext(ADMIN_UID).firestore();
  const now = new Date(Date.now() - 2000);

  await assertSucceeds(
    aliceDb.collection("support_tickets").doc("tic_1").set({
      userId: ALICE_UID,
      type: "INCORRECT_SHOP_INFO",
      targetId: "north-end-banani",
      targetName: "North End",
      subject: "Closing time updated",
      description: "Now closes at 12:30 AM on weekdays.",
      contactInfo: "alice@example.com",
      status: "Open",
      adminResponse: "",
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertFails(bobDb.collection("support_tickets").doc("tic_1").get());
  await assertSucceeds(adminDb.collection("support_tickets").doc("tic_1").get());

  await assertSucceeds(
    aliceDb.collection("verification_requests").doc("ver_1").set({
      shopId: "north-end-banani",
      shopName: "North End Coffee Roasters",
      requesterUid: ALICE_UID,
      businessOwnerName: "Alice Rahman",
      businessPhone: "+8801711000001",
      registrationNote: "Trade license #12345 in Banani",
      status: "Pending",
      reviewedBy: "",
      createdAt: now,
      updatedAt: now,
    })
  );

  // Requester cannot self-approve verification
  await assertFails(
    aliceDb.collection("verification_requests").doc("ver_1").update({
      status: "Approved",
      updatedAt: new Date(Date.now() - 500),
    })
  );
  await assertFails(bobDb.collection("verification_requests").doc("ver_1").get());
  await assertSucceeds(
    adminDb.collection("verification_requests").doc("ver_1").update({
      status: "Approved",
      reviewedBy: ADMIN_UID,
      updatedAt: new Date(Date.now() - 500),
    })
  );
});
