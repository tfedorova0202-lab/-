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

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
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

// 1. Default-deny & Unauthenticated boundaries
test("Unauthenticated user: denied read/write on users, debts, legal_actions, and unknown collections", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("users").get());
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("debts").get()
  );
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("legal_actions").get()
  );
  await assertFails(unauthDb.collection("arbitrary_collection").doc("x").get());
});

test("Authenticated user: default-deny blocks unknown collections and listing /users", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(aliceDb.collection("users").get());
  await assertFails(aliceDb.collection("unknown_col").doc("doc1").set({ a: 1 }));
  await assertFails(aliceDb.collection("unknown_col").doc("doc1").get());
});

// 2. User profile CRUD, bounds, spoofed UID, and cross-user isolation
test("Alice: valid Kotlin saveUserProfile payload succeeds (create, read, update, delete)", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const now = new Date(Date.now() - 1000);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      email: "alice@example.com",
      displayName: "Алиса Иванова",
      isIncognito: true,
      hideFamilyInfo: true,
      updatedAt: now,
    })
  );
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).get());
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      isIncognito: false,
      updatedAt: new Date(Date.now() - 500),
    })
  );
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).delete());
});

test("User profile: rejects spoofed userId, oversized fields, and future timestamps", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const future = new Date(Date.now() + 120000);

  // Spoofed userId inside own doc
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: BOB_UID,
      isIncognito: true,
      hideFamilyInfo: true,
    })
  );
  // Oversized email (> 200)
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      email: "a".repeat(201),
      isIncognito: true,
      hideFamilyInfo: true,
    })
  );
  // Oversized displayName (> 100)
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Б".repeat(101),
      isIncognito: true,
      hideFamilyInfo: true,
    })
  );
  // Tampered future timestamp
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      isIncognito: true,
      hideFamilyInfo: true,
      updatedAt: future,
    })
  );
});

test("Bob: cannot read, modify, or delete Alice's profile", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      isIncognito: true,
      hideFamilyInfo: true,
    });
  });

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).update({ isIncognito: false })
  );
  await assertFails(bobDb.collection("users").doc(ALICE_UID).delete());
});

// 3. Debts subcollection: Kotlin FirestoreManager.saveDebt() parity + negative bounds
test("Alice: valid Kotlin saveDebt payload succeeds (create, get, list, update, delete)", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const debtRef = aliceDb
    .collection("users")
    .doc(ALICE_UID)
    .collection("debts")
    .doc("debt_01-a");

  await assertSucceeds(
    debtRef.set({
      id: "debt_01-a",
      creditorName: "ПАО Сбербанк",
      contractDate: "12.04.2023",
      factAmount: 150000,
      penaltyAmount: 35000,
      insuranceAmount: 15000,
      planAmount: 100000,
      status: "ACTIVE",
      isMortgage: true,
      deadlineDate: "30.09.2026",
      deadlineType: "10 дней (ст. 128 ГПК)",
      legalBasis: "ст. 333 ГК РФ, ст. 958 ГК РФ",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  await assertSucceeds(debtRef.get());
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("debts").get()
  );
  await assertSucceeds(
    debtRef.update({
      status: "DISPUTED",
      planAmount: 90000,
      updatedAt: new Date(Date.now() - 500),
    })
  );
  await assertSucceeds(debtRef.delete());
});

test("Bob: cannot read, write, update, or delete Alice's debts", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context
      .firestore()
      .collection("users")
      .doc(ALICE_UID)
      .collection("debts")
      .doc("debt_1")
      .set({
        id: "debt_1",
        creditorName: "ООО МФО Быстроденьги",
        contractDate: "01.01.2024",
        factAmount: 50000,
        planAmount: 25000,
        status: "ACTIVE",
      });
  });

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const aliceDebtRef = bobDb
    .collection("users")
    .doc(ALICE_UID)
    .collection("debts")
    .doc("debt_1");

  await assertFails(aliceDebtRef.get());
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("debts").get()
  );
  await assertFails(aliceDebtRef.update({ planAmount: 1 }));
  await assertFails(aliceDebtRef.delete());
});

test("Debts validation: rejects mismatched ID, bad ID chars, empty/long creditor, negative amounts, bad status, and future timestamp", async () => {
  const aliceDebts = testEnv
    .authenticatedContext(ALICE_UID)
    .firestore()
    .collection("users")
    .doc(ALICE_UID)
    .collection("debts");

  const validBase = {
    id: "debt_valid",
    creditorName: "АО Т-Банк",
    contractDate: "01.02.2025",
    factAmount: 50000,
    penaltyAmount: 10000,
    insuranceAmount: 5000,
    planAmount: 35000,
    status: "ACTIVE",
    isMortgage: false,
  };

  // Mismatched docId vs data.id
  await assertFails(aliceDebts.doc("other_id").set(validBase));
  // Invalid characters in ID (spaces/punctuation)
  await assertFails(
    aliceDebts.doc("bad id!").set({ ...validBase, id: "bad id!" })
  );
  // Blank creditorName
  await assertFails(
    aliceDebts.doc("debt_blank").set({ ...validBase, id: "debt_blank", creditorName: "" })
  );
  // Oversized creditorName (> 150)
  await assertFails(
    aliceDebts
      .doc("debt_long")
      .set({ ...validBase, id: "debt_long", creditorName: "К".repeat(151) })
  );
  // Negative amounts
  await assertFails(
    aliceDebts.doc("debt_neg1").set({ ...validBase, id: "debt_neg1", factAmount: -1 })
  );
  await assertFails(
    aliceDebts.doc("debt_neg2").set({ ...validBase, id: "debt_neg2", penaltyAmount: -0.01 })
  );
  await assertFails(
    aliceDebts.doc("debt_neg3").set({ ...validBase, id: "debt_neg3", planAmount: -10 })
  );
  // Invalid status enum
  await assertFails(
    aliceDebts
      .doc("debt_status")
      .set({ ...validBase, id: "debt_status", status: "В работе" })
  );
  // Tampered future timestamp
  await assertFails(
    aliceDebts
      .doc("debt_future")
      .set({
        ...validBase,
        id: "debt_future",
        updatedAt: new Date(Date.now() + 120000),
      })
  );
});

// 4. Legal actions subcollection
test("Legal actions: owner CRUD succeeds, Bob denied, invalid actionType/oversized content rejected", async () => {
  const aliceActions = testEnv
    .authenticatedContext(ALICE_UID)
    .firestore()
    .collection("users")
    .doc(ALICE_UID)
    .collection("legal_actions");

  await assertSucceeds(
    aliceActions.doc("action_1").set({
      id: "action_1",
      title: "Возражение на судебный приказ",
      actionType: "CANCEL_COURT_ORDER",
      creditorName: "МФК Займер",
      status: "READY",
      deadlineDate: "15.10.2026",
      documentContent: "В Мировой суд...",
      updatedAt: new Date(Date.now() - 1000),
    })
  );
  await assertSucceeds(aliceActions.doc("action_1").get());
  await assertSucceeds(aliceActions.get());

  // Bob cannot read or delete Alice's legal actions
  const bobActions = testEnv
    .authenticatedContext(BOB_UID)
    .firestore()
    .collection("users")
    .doc(ALICE_UID)
    .collection("legal_actions");
  await assertFails(bobActions.doc("action_1").get());
  await assertFails(bobActions.doc("action_1").delete());

  // Invalid actionType
  await assertFails(
    aliceActions.doc("action_bad_type").set({
      id: "action_bad_type",
      title: "Некорректный тип",
      actionType: "HACK_BANK",
      status: "DRAFT",
    })
  );
  // Oversized documentContent (> 20000)
  await assertFails(
    aliceActions.doc("action_huge").set({
      id: "action_huge",
      title: "Огромный документ",
      actionType: "REDUCE_PENALTIES",
      status: "DRAFT",
      documentContent: "Т".repeat(20001),
    })
  );

  // Owner delete succeeds (mirrors FirestoreManager.deleteAllUserData)
  await assertSucceeds(aliceActions.doc("action_1").delete());
});
