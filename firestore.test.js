const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");
const path = require("node:path");
const compatBase = path.dirname(require.resolve("@firebase/rules-unit-testing/package.json"));
const firebase = require(path.join(compatBase, "node_modules/firebase/compat/app"));
require(path.join(compatBase, "node_modules/firebase/compat/firestore"));

const serverTimestamp = () => firebase.firestore.FieldValue.serverTimestamp();

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ADMIN_UID = "admin_sbstudio";
const USER_UID = "shop_user_1";
const OTHER_UID = "shop_user_2";
const SPOOF_UID = "spoof_admin";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function validOfficialPayload(uid) {
  return {
    groupCode: "TG-001",
    category: "TEMPERED_GLASS",
    mainModel: "Oppo F31",
    brand: "Oppo",
    screenSize: '6.57"',
    compatibleModels: ["Vivo V30", "Realme X", "Samsung A25"],
    releaseNote: "May Official Update",
    isPublished: true,
    updatedByUid: uid,
    createdAt: serverTimestamp(),
    updatedAt: serverTimestamp(),
  };
}

function validProductPayload(uid, isPublished = false) {
  return {
    userId: uid,
    productName: "Oppo F31 Tempered Glass",
    category: "TEMPERED_GLASS",
    mainModel: "Oppo F31",
    brand: "Oppo",
    screenSize: '6.57"',
    stockQuantity: 10,
    minimumStock: 3,
    boxNumber: "B-012",
    universalGroupId: "TG-001",
    compatibleModels: ["Vivo V30", "Realme X"],
    notes: "9H matte",
    isPublished,
    createdAt: serverTimestamp(),
    updatedAt: serverTimestamp(),
  };
}

function validUniversalModelPayload(uid, isPublished = false, updatedByUid = uid) {
  return {
    userId: uid,
    updatedByUid,
    groupCode: "TG-001",
    category: "TEMPERED_GLASS",
    mainModel: "Oppo F31",
    brand: "Oppo",
    screenSize: '6.57"',
    compatibleModels: ["Vivo V30", "Realme X"],
    releaseNote: "Synced model group",
    isPublished,
    createdAt: serverTimestamp(),
    updatedAt: serverTimestamp(),
  };
}

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

test("Unauthenticated user: cannot read or write products, universal_models, or official_universal_groups", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("products").where("isPublished", "==", true).get());
  await assertFails(unauthDb.collection("products").doc("p1").set(validProductPayload("anon")));
  await assertFails(unauthDb.collection("universal_models").where("isPublished", "==", true).get());
  await assertFails(
    unauthDb.collection("universal_models").doc("u1").set(validUniversalModelPayload("anon"))
  );
  await assertFails(
    unauthDb.collection("official_universal_groups").where("isPublished", "==", true).get()
  );
  await assertFails(
    unauthDb.collection("official_universal_groups").doc("tg_001").set(validOfficialPayload("anon"))
  );
});

test("Products: owner can CRUD & query own products; cross-user, negative stock, ghost fields, and unauthorized publish rejected", async () => {
  const userDb = testEnv
    .authenticatedContext(USER_UID, { email: "shop1@gmail.com", email_verified: true })
    .firestore();
  const otherDb = testEnv
    .authenticatedContext(OTHER_UID, { email: "shop2@gmail.com", email_verified: true })
    .firestore();

  const p1 = userDb.collection("products").doc("prod_1");
  await assertSucceeds(p1.set(validProductPayload(USER_UID, false)));

  // Owner filtered query works; unfiltered list fails
  await assertSucceeds(userDb.collection("products").where("userId", "==", USER_UID).get());
  await assertFails(userDb.collection("products").get());

  // Owner partial stock update works
  await assertSucceeds(p1.update({ stockQuantity: 15, updatedAt: serverTimestamp() }));

  // Negative stock rejected
  await assertFails(p1.update({ stockQuantity: -1, updatedAt: serverTimestamp() }));

  // Ghost field rejected
  await assertFails(
    userDb.collection("products").doc("prod_ghost").set({
      ...validProductPayload(USER_UID, false),
      ghostField: "hack",
    })
  );

  // Regular user cannot set isPublished = true
  await assertFails(
    userDb.collection("products").doc("prod_pub").set(validProductPayload(USER_UID, true))
  );

  // Cross-user read/write/spoof rejected
  await assertFails(otherDb.collection("products").doc("prod_1").get());
  await assertFails(
    otherDb.collection("products").doc("prod_1").update({ stockQuantity: 0, updatedAt: serverTimestamp() })
  );
  await assertFails(otherDb.collection("products").doc("prod_1").delete());
  await assertFails(
    otherDb.collection("products").doc("prod_spoof").set(validProductPayload(USER_UID, false))
  );

  await assertSucceeds(p1.delete());
});

test("Universal Models: owner CRUD, cross-user isolation, and verified admin global publish work properly", async () => {
  const userDb = testEnv
    .authenticatedContext(USER_UID, { email: "shop1@gmail.com", email_verified: true })
    .firestore();
  const otherDb = testEnv
    .authenticatedContext(OTHER_UID, { email: "shop2@gmail.com", email_verified: true })
    .firestore();
  const adminDb = testEnv
    .authenticatedContext(ADMIN_UID, { email: "sbstudio17@gmail.com", email_verified: true })
    .firestore();

  // Regular user creates own private universal model for multi-device sync
  const u1 = userDb.collection("universal_models").doc("um_1");
  await assertSucceeds(u1.set(validUniversalModelPayload(USER_UID, false)));
  await assertSucceeds(userDb.collection("universal_models").where("userId", "==", USER_UID).get());
  await assertSucceeds(
    u1.update({
      compatibleModels: ["Vivo V30", "Realme X", "Oppo A79"],
      updatedAt: serverTimestamp(),
    })
  );

  // Regular user cannot publish globally
  await assertFails(
    userDb.collection("universal_models").doc("um_pub").set(validUniversalModelPayload(USER_UID, true))
  );

  // Other user cannot access private universal_models doc
  await assertFails(otherDb.collection("universal_models").doc("um_1").get());
  await assertFails(otherDb.collection("universal_models").doc("um_1").delete());

  // Verified admin publishes global universal_model + global product
  const globalUm = adminDb.collection("universal_models").doc("um_global");
  await assertSucceeds(globalUm.set(validUniversalModelPayload(ADMIN_UID, true)));
  const globalProd = adminDb.collection("products").doc("prod_global");
  await assertSucceeds(globalProd.set(validProductPayload(ADMIN_UID, true)));

  // All signed-in users can read globally published universal_models & products in real time
  await assertSucceeds(otherDb.collection("universal_models").where("isPublished", "==", true).get());
  await assertSucceeds(otherDb.collection("products").where("isPublished", "==", true).get());

  // Other users cannot tamper with or delete globally published docs
  await assertFails(
    otherDb.collection("universal_models").doc("um_global").update({
      mainModel: "Tampered",
      updatedByUid: OTHER_UID,
      updatedAt: serverTimestamp(),
    })
  );
  await assertFails(otherDb.collection("universal_models").doc("um_global").delete());

  await assertSucceeds(u1.delete());
  await assertSucceeds(globalUm.delete());
  await assertSucceeds(globalProd.delete());
});

test("Email spoofing test: unverified admin email is rejected on global write", async () => {
  const spoofDb = testEnv
    .authenticatedContext(SPOOF_UID, {
      email: "sbstudio17@gmail.com",
      email_verified: false,
    })
    .firestore();

  await assertFails(
    spoofDb.collection("official_universal_groups").doc("tg_001").set(validOfficialPayload(SPOOF_UID))
  );
  await assertFails(
    spoofDb.collection("universal_models").doc("um_spoof").set(validUniversalModelPayload(SPOOF_UID, true))
  );
  await assertFails(
    spoofDb.collection("products").doc("prod_spoof").set(validProductPayload(SPOOF_UID, true))
  );
});

test("Official Universal Groups: regular user read-only & admin CRUD preserved", async () => {
  const adminDb = testEnv
    .authenticatedContext(ADMIN_UID, {
      email: "sbstudio17@gmail.com",
      email_verified: true,
    })
    .firestore();
  const userDb = testEnv
    .authenticatedContext(USER_UID, {
      email: "otheruser@gmail.com",
      email_verified: true,
    })
    .firestore();

  const docRef = adminDb.collection("official_universal_groups").doc("tg_001");
  await assertSucceeds(docRef.set(validOfficialPayload(ADMIN_UID)));
  await assertSucceeds(
    userDb.collection("official_universal_groups").where("isPublished", "==", true).get()
  );
  await assertFails(userDb.collection("official_universal_groups").get());
  await assertFails(userDb.collection("official_universal_groups").doc("tg_001").delete());
  await assertSucceeds(docRef.delete());
});

test("Admins collection cannot be written or listed by any client", async () => {
  const userDb = testEnv
    .authenticatedContext(USER_UID, {
      email: "otheruser@gmail.com",
      email_verified: true,
    })
    .firestore();

  await assertFails(userDb.collection("admins").doc(USER_UID).set({ role: "admin" }));
  await assertFails(userDb.collection("admins").get());
});
