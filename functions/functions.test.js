const { test } = require("node:test");
const assert = require("node:assert/strict");
const {
  validateCouponRedemption,
  validateReservationTransition,
  isStaleReport,
} = require("./validators");

test("validateCouponRedemption blocks expired, exhausted, and duplicate redemptions", () => {
  const now = 1_700_000_000_000;
  const activeCoupon = {
    active: true,
    visibility: "public",
    expiresAtMillis: now + 60_000,
    maxUsageLimit: 10,
    redeemedCount: 3,
  };

  assert.equal(
    validateCouponRedemption({ coupon: activeCoupon, existingRedemption: null, nowMillis: now }).ok,
    true
  );

  // Already redeemed -> rejected
  assert.equal(
    validateCouponRedemption({
      coupon: activeCoupon,
      existingRedemption: { status: "REDEEMED" },
      nowMillis: now,
    }).ok,
    false
  );

  // Expired -> rejected
  assert.equal(
    validateCouponRedemption({
      coupon: { ...activeCoupon, expiresAtMillis: now - 1000 },
      existingRedemption: null,
      nowMillis: now,
    }).ok,
    false
  );

  // Exhausted limit -> rejected
  assert.equal(
    validateCouponRedemption({
      coupon: { ...activeCoupon, redeemedCount: 10 },
      existingRedemption: null,
      nowMillis: now,
    }).ok,
    false
  );
});

test("validateReservationTransition enforces strict status lifecycle", () => {
  assert.equal(validateReservationTransition("Pending", "Confirmed").ok, true);
  assert.equal(validateReservationTransition("Pending", "Rejected").ok, true);
  assert.equal(validateReservationTransition("Confirmed", "Completed").ok, true);
  assert.equal(validateReservationTransition("Cancelled", "Confirmed").ok, false);
  assert.equal(validateReservationTransition("Rejected", "Confirmed").ok, false);
});

test("isStaleReport flags missing or >3h old reports", () => {
  const now = 1_700_000_000_000;
  assert.equal(isStaleReport(now - 60_000, now), false);
  assert.equal(isStaleReport(now - 4 * 60 * 60 * 1000, now), true);
  assert.equal(isStaleReport(0, now), true);
});
