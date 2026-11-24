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

test("Unauthenticated user cannot read or write community_shops or shop_reviews", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("community_shops").where("visibility", "==", "public").get());
  await assertFails(unauthDb.collection("shop_reviews").where("visibility", "==", "public").get());
  await assertFails(
    unauthDb.collection("community_shops").doc("shop_1").set({
      ownerId: ALICE_UID,
      visibility: "public",
      name: "Midnight Roasters",
      area: "Banani",
      address: "Road 11, Banani",
      openTime: "10:00 AM",
      closeTime: "02:00 AM",
      popularItems: ["Latte"],
      priceRange: "250 - 450 BDT",
      phone: "+8801711000000",
      familyFriendly: true,
      createdAt: new Date(Date.now() - 1000),
      updatedAt: new Date(Date.now() - 1000),
    })
  );
});

test("Authenticated user can create, query, update, and delete own community_shop", async () => {
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
      popularItems: ["Iced Spanish Latte", "Pour Over"],
      priceRange: "250 - 450 BDT",
      phone: "+8801711223344",
      familyFriendly: true,
      createdAt: created,
      updatedAt: created,
    })
  );

  // Query aligned with rule: where("visibility", "==", "public")
  await assertSucceeds(
    aliceDb.collection("community_shops").where("visibility", "==", "public").get()
  );

  // Unfiltered query must fail
  await assertFails(aliceDb.collection("community_shops").get());

  // Partial update by owner succeeds
  await assertSucceeds(
    aliceDb.collection("community_shops").doc("shop_1").update({
      priceRange: "280 - 500 BDT",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Reject ghost field / @DocumentId 'id' / invalid area / future timestamp
  await assertFails(
    aliceDb.collection("community_shops").doc("shop_bad_id").set({
      id: "shop_bad_id",
      ownerId: ALICE_UID,
      visibility: "public",
      name: "Bad Shop",
      area: "Banani",
      address: "Road 11",
      openTime: "10:00",
      closeTime: "23:00",
      popularItems: ["Latte"],
      priceRange: "200 - 400",
      phone: "+8801700",
      familyFriendly: true,
      createdAt: created,
      updatedAt: created,
    })
  );
  await assertFails(
    aliceDb.collection("community_shops").doc("shop_future").set({
      ownerId: ALICE_UID,
      visibility: "public",
      name: "Future Shop",
      area: "Banani",
      address: "Road 11",
      openTime: "10:00",
      closeTime: "23:00",
      popularItems: ["Latte"],
      priceRange: "200 - 400",
      phone: "+8801700",
      familyFriendly: true,
      createdAt: new Date(Date.now() + 60000),
      updatedAt: new Date(Date.now() + 60000),
    })
  );

  // Cross-user update or delete must fail
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(
    bobDb.collection("community_shops").doc("shop_1").update({
      name: "Hijacked Name",
      updatedAt: new Date(Date.now() - 1000),
    })
  );
  await assertFails(bobDb.collection("community_shops").doc("shop_1").delete());

  // Owner delete succeeds
  await assertSucceeds(aliceDb.collection("community_shops").doc("shop_1").delete());
});

test("Authenticated user can create, query, and update own shop_review, while cross-user modification fails", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const created = new Date(Date.now() - 5000);

  await assertSucceeds(
    aliceDb.collection("shop_reviews").doc("rev_1").set({
      shopId: "north-end-banani",
      userId: ALICE_UID,
      authorName: "Alice",
      rating: 5,
      comment: "Great midnight espresso and calm family seating.",
      visibility: "public",
      createdAt: created,
      updatedAt: created,
    })
  );

  // Aligned list query succeeds; unfiltered query fails
  await assertSucceeds(
    bobDb.collection("shop_reviews").where("visibility", "==", "public").get()
  );
  await assertFails(bobDb.collection("shop_reviews").get());

  // Rating out of bounds (0 or 6) fails
  await assertFails(
    aliceDb.collection("shop_reviews").doc("rev_bad_rating").set({
      shopId: "north-end-banani",
      userId: ALICE_UID,
      authorName: "Alice",
      rating: 6,
      comment: "Too high",
      visibility: "public",
      createdAt: created,
      updatedAt: created,
    })
  );

  // Cross-user update/delete fails
  await assertFails(
    bobDb.collection("shop_reviews").doc("rev_1").update({
      comment: "Tampered",
      updatedAt: new Date(Date.now() - 1000),
    })
  );
  await assertFails(bobDb.collection("shop_reviews").doc("rev_1").delete());

  // Owner update succeeds
  await assertSucceeds(
    aliceDb.collection("shop_reviews").doc("rev_1").update({
      rating: 4,
      comment: "Updated review after second visit.",
      updatedAt: new Date(Date.now() - 1000),
    })
  );
});
