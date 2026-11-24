function validateCouponRedemption({
  coupon,
  existingRedemption,
  nowMillis,
}) {
  if (!coupon) {
    return { ok: false, code: "not-found", reason: "Coupon does not exist." };
  }
  if (!coupon.active || coupon.visibility !== "public") {
    return { ok: false, code: "failed-precondition", reason: "This coupon is no longer active." };
  }
  const expiresAtMillis = coupon.expiresAtMillis ?? 0;
  if (expiresAtMillis > 0 && expiresAtMillis <= nowMillis) {
    return { ok: false, code: "failed-precondition", reason: "This coupon has expired." };
  }
  const redeemedCount = Number(coupon.redeemedCount || 0);
  const maxUsageLimit = Number(coupon.maxUsageLimit || 0);
  if (maxUsageLimit > 0 && redeemedCount >= maxUsageLimit) {
    return { ok: false, code: "resource-exhausted", reason: "Coupon usage limit has been reached." };
  }
  if (existingRedemption && existingRedemption.status === "REDEEMED") {
    return { ok: false, code: "already-exists", reason: "You have already redeemed this coupon." };
  }
  return { ok: true };
}

function validateReservationTransition(currentStatus, nextStatus) {
  const allowed = {
    Pending: ["Confirmed", "Rejected", "Cancelled"],
    Confirmed: ["Completed", "Cancelled"],
    Rejected: [],
    Cancelled: [],
    Completed: [],
  };
  const validTargets = allowed[currentStatus] || [];
  if (!validTargets.includes(nextStatus)) {
    return {
      ok: false,
      reason: `Cannot transition reservation from ${currentStatus} to ${nextStatus}.`,
    };
  }
  return { ok: true };
}

function isStaleReport(createdAtMillis, nowMillis, maxAgeMillis = 3 * 60 * 60 * 1000) {
  if (!createdAtMillis || createdAtMillis <= 0) return true;
  return nowMillis - createdAtMillis > maxAgeMillis;
}

module.exports = {
  validateCouponRedemption,
  validateReservationTransition,
  isStaleReport,
};
