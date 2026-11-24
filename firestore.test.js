const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ADMIN_UID = "admin_ragavan_uid";
const STAFF_NADEEM_UID = "staff_nadeem_uid";
const OUTSIDER_UID = "outsider_uid";
const WORKSPACE_ID = "concord_atoz_gas";
const INVITE_CODE = "CONCORD2026";

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

function pastDate(msOffset = 5000) {
  return new Date(Date.now() - msOffset);
}

async function seedWorkspaceWithAdminAndInvite() {
  const adminDb = testEnv
    .authenticatedContext(ADMIN_UID, {
      email: "atozgas26@gmail.com",
      email_verified: true,
    })
    .firestore();
  const ts = pastDate(10000);

  await assertSucceeds(
    adminDb.collection("workspace_invites").doc(INVITE_CODE).set({
      workspaceId: WORKSPACE_ID,
      ownerUid: ADMIN_UID,
      active: true,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertSucceeds(
    adminDb.collection("workspaces").doc(WORKSPACE_ID).set({
      companyName: "CONCORD & A TO Z GAS",
      ownerUid: ADMIN_UID,
      memberUids: [ADMIN_UID],
      activeInviteCode: INVITE_CODE,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  return { adminDb, ts };
}

test("Unauthenticated users cannot read or write any collection", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  const ts = pastDate();

  await assertFails(unauthDb.collection("users").doc(ADMIN_UID).get());
  await assertFails(unauthDb.collection("workspaces").doc(WORKSPACE_ID).get());
  await assertFails(
    unauthDb.collection("workspaces").where("memberUids", "array-contains", ADMIN_UID).get()
  );
  await assertFails(unauthDb.collection("workspace_invites").doc(INVITE_CODE).get());
  await assertFails(
    unauthDb.collection("users").doc(ADMIN_UID).set({
      userId: ADMIN_UID,
      displayName: "Anon",
      createdAt: ts,
      updatedAt: ts,
    })
  );
});

test("User profile enforces strict owner isolation and rejects ghost fields", async () => {
  const aliceDb = testEnv.authenticatedContext(ADMIN_UID).firestore();
  const bobDb = testEnv.authenticatedContext(STAFF_NADEEM_UID).firestore();
  const ts = pastDate(8000);

  await assertSucceeds(
    aliceDb.collection("users").doc(ADMIN_UID).set({
      userId: ADMIN_UID,
      displayName: "Ragavan",
      email: "atozgas26@gmail.com",
      activeWorkspaceId: WORKSPACE_ID,
      linkedStaffUsername: "admin_ragavan",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Partial update succeeds
  await assertSucceeds(
    aliceDb.collection("users").doc(ADMIN_UID).update({
      linkedStaffUsername: "nadeem",
      updatedAt: pastDate(1000),
    })
  );

  // Cross-user read/write blocked
  await assertFails(bobDb.collection("users").doc(ADMIN_UID).get());
  await assertFails(
    bobDb.collection("users").doc(ADMIN_UID).update({
      displayName: "Hijacked",
      updatedAt: pastDate(1000),
    })
  );

  // Ghost field & future timestamp rejected
  await assertFails(
    bobDb.collection("users").doc(STAFF_NADEEM_UID).set({
      userId: STAFF_NADEEM_UID,
      displayName: "Nadeem",
      isAdmin: true,
      createdAt: ts,
      updatedAt: ts,
    })
  );
  await assertFails(
    bobDb.collection("users").doc(STAFF_NADEEM_UID).set({
      userId: STAFF_NADEEM_UID,
      displayName: "Nadeem",
      createdAt: new Date(Date.now() + 60000),
      updatedAt: ts,
    })
  );
});

test("Workspace invite cannot be listed; staff joins workspace using active invite code", async () => {
  await seedWorkspaceWithAdminAndInvite();
  const staffDb = testEnv.authenticatedContext(STAFF_NADEEM_UID).firestore();
  const outsiderDb = testEnv.authenticatedContext(OUTSIDER_UID).firestore();

  // Listing invites must fail
  await assertFails(staffDb.collection("workspace_invites").get());

  // Direct lookup by invite code succeeds
  await assertSucceeds(staffDb.collection("workspace_invites").doc(INVITE_CODE).get());

  // Joining with wrong invite code fails
  await assertFails(
    staffDb.collection("workspaces").doc(WORKSPACE_ID).update({
      memberUids: [ADMIN_UID, STAFF_NADEEM_UID],
      lastJoinInviteCode: "WRONGCODE",
      updatedAt: pastDate(1000),
    })
  );

  // Joining with valid active invite code succeeds
  await assertSucceeds(
    staffDb.collection("workspaces").doc(WORKSPACE_ID).update({
      memberUids: [ADMIN_UID, STAFF_NADEEM_UID],
      lastJoinInviteCode: INVITE_CODE,
      updatedAt: pastDate(1000),
    })
  );

  // Outsider cannot read or list workspace
  await assertFails(outsiderDb.collection("workspaces").doc(WORKSPACE_ID).get());
  await assertFails(outsiderDb.collection("workspaces").get());
  await assertSucceeds(
    staffDb.collection("workspaces").where("memberUids", "array-contains", STAFF_NADEEM_UID).get()
  );
});

test("Email spoofing test: unverified admin email does not bypass membership", async () => {
  await seedWorkspaceWithAdminAndInvite();
  const spoofedDb = testEnv
    .authenticatedContext("spoof_uid", {
      email: "atozgas26@gmail.com",
      email_verified: false,
    })
    .firestore();

  await assertFails(spoofedDb.collection("workspaces").doc(WORKSPACE_ID).get());
});

test("Master data & Staff online transaction sync mirrors Kotlin payloads and blocks invalid/outsider writes", async () => {
  const { adminDb, ts } = await seedWorkspaceWithAdminAndInvite();
  const staffDb = testEnv.authenticatedContext(STAFF_NADEEM_UID).firestore();
  const outsiderDb = testEnv.authenticatedContext(OUTSIDER_UID).firestore();

  // Staff joins workspace
  await assertSucceeds(
    staffDb.collection("workspaces").doc(WORKSPACE_ID).update({
      memberUids: [ADMIN_UID, STAFF_NADEEM_UID],
      lastJoinInviteCode: INVITE_CODE,
      updatedAt: pastDate(1000),
    })
  );

  const members = [ADMIN_UID, STAFF_NADEEM_UID];

  // Admin creates product (no @DocumentId `id` field, `active` instead of `isActive`)
  await assertSucceeds(
    adminDb
      .collection("workspaces")
      .doc(WORKSPACE_ID)
      .collection("products")
      .doc("prod_1")
      .set({
        localId: 1,
        category: "INDUSTRIAL_GAS",
        name: "Oxygen",
        sortOrder: 101,
        active: true,
        memberUids: members,
        updatedByUid: ADMIN_UID,
        createdAt: ts,
        updatedAt: ts,
      })
  );

  // Reject product with forbidden `id` field
  await assertFails(
    adminDb
      .collection("workspaces")
      .doc(WORKSPACE_ID)
      .collection("products")
      .doc("prod_2")
      .set({
        id: "prod_2",
        localId: 2,
        category: "INDUSTRIAL_GAS",
        name: "Acetylene",
        sortOrder: 102,
        active: true,
        memberUids: members,
        updatedByUid: ADMIN_UID,
        createdAt: ts,
        updatedAt: ts,
      })
  );

  // Staff records online transaction (Nadeem selling Oxygen)
  const txId = "tx_1700000000_nadeem";
  await assertSucceeds(
    staffDb
      .collection("workspaces")
      .doc(WORKSPACE_ID)
      .collection("transactions")
      .doc(txId)
      .set({
        clientTxId: txId,
        businessDate: "2026-11-24",
        clientTimestampMs: Date.now() - 1000,
        type: "VEHICLE_CUSTOMER_SALE",
        productId: 1,
        vehicleId: 1,
        vehicleName: "NADEEM",
        staffId: 2,
        staffName: "Nadeem",
        quantity: 4,
        notes: "Morning customer delivery",
        createdByUid: STAFF_NADEEM_UID,
        memberUids: members,
        createdAt: pastDate(1000),
        updatedAt: pastDate(1000),
      })
  );

  // Filtered query matching Kotlin repository succeeds; unfiltered fails
  await assertSucceeds(
    adminDb
      .collection("workspaces")
      .doc(WORKSPACE_ID)
      .collection("transactions")
      .where("memberUids", "array-contains", ADMIN_UID)
      .get()
  );
  await assertFails(
    adminDb.collection("workspaces").doc(WORKSPACE_ID).collection("transactions").get()
  );

  // Negative/zero quantity transaction fails
  await assertFails(
    staffDb
      .collection("workspaces")
      .doc(WORKSPACE_ID)
      .collection("transactions")
      .doc("tx_bad_qty")
      .set({
        clientTxId: "tx_bad_qty",
        businessDate: "2026-11-24",
        clientTimestampMs: Date.now() - 1000,
        type: "VEHICLE_CUSTOMER_SALE",
        productId: 1,
        vehicleId: 1,
        vehicleName: "NADEEM",
        staffId: 2,
        staffName: "Nadeem",
        quantity: 0,
        notes: "",
        createdByUid: STAFF_NADEEM_UID,
        memberUids: members,
        createdAt: pastDate(1000),
        updatedAt: pastDate(1000),
      })
  );

  // Outsider cannot read or write transactions
  await assertFails(
    outsiderDb
      .collection("workspaces")
      .doc(WORKSPACE_ID)
      .collection("transactions")
      .doc(txId)
      .get()
  );
  await assertFails(
    outsiderDb
      .collection("workspaces")
      .doc(WORKSPACE_ID)
      .collection("transactions")
      .doc(txId)
      .delete()
  );

  // Admin can delete a transaction
  await assertSucceeds(
    adminDb
      .collection("workspaces")
      .doc(WORKSPACE_ID)
      .collection("transactions")
      .doc(txId)
      .delete()
  );
});

test("Purchase transactions with purchaseCompanyName and staff/vehicle name edits follow Kotlin rules", async () => {
  const { adminDb, ts } = await seedWorkspaceWithAdminAndInvite();
  const members = [ADMIN_UID];

  // Store purchase with purchaseCompanyName succeeds
  const purId = "tx_1700000001_purchase";
  await assertSucceeds(
    adminDb
      .collection("workspaces")
      .doc(WORKSPACE_ID)
      .collection("transactions")
      .doc(purId)
      .set({
        clientTxId: purId,
        businessDate: "2026-11-24",
        clientTimestampMs: Date.now() - 1000,
        type: "STORE_PURCHASE_FULL",
        productId: 1,
        staffId: 1,
        staffName: "Ragavan",
        quantity: 25,
        purchaseCompanyName: "Emirates Gas LLC",
        notes: "INV-9981",
        createdByUid: ADMIN_UID,
        memberUids: members,
        createdAt: pastDate(1000),
        updatedAt: pastDate(1000),
      })
  );

  // Oversized purchaseCompanyName (> 120 chars) fails
  await assertFails(
    adminDb
      .collection("workspaces")
      .doc(WORKSPACE_ID)
      .collection("transactions")
      .doc("tx_bad_company")
      .set({
        clientTxId: "tx_bad_company",
        businessDate: "2026-11-24",
        clientTimestampMs: Date.now() - 1000,
        type: "STORE_PURCHASE_FULL",
        productId: 1,
        staffId: 1,
        staffName: "Ragavan",
        quantity: 10,
        purchaseCompanyName: "X".repeat(121),
        notes: "",
        createdByUid: ADMIN_UID,
        memberUids: members,
        createdAt: pastDate(1000),
        updatedAt: pastDate(1000),
      })
  );

  // Staff account displayName edit succeeds; empty displayName fails
  await assertSucceeds(
    adminDb
      .collection("workspaces")
      .doc(WORKSPACE_ID)
      .collection("staff_accounts")
      .doc("user_nadeem")
      .set({
        localId: 2,
        username: "nadeem",
        displayName: "Nadeem",
        role: "STAFF",
        pin: "1001",
        active: true,
        memberUids: members,
        updatedByUid: ADMIN_UID,
        createdAt: ts,
        updatedAt: ts,
      })
  );
  await assertSucceeds(
    adminDb
      .collection("workspaces")
      .doc(WORKSPACE_ID)
      .collection("staff_accounts")
      .doc("user_nadeem")
      .update({
        displayName: "Nadeem Khan",
        pin: "1001",
        active: true,
        memberUids: members,
        updatedByUid: ADMIN_UID,
        updatedAt: pastDate(500),
      })
  );
  await assertFails(
    adminDb
      .collection("workspaces")
      .doc(WORKSPACE_ID)
      .collection("staff_accounts")
      .doc("user_nadeem")
      .update({
        displayName: "",
        updatedByUid: ADMIN_UID,
        updatedAt: pastDate(400),
      })
  );

  // Vehicle name edit succeeds; oversized name fails
  await assertSucceeds(
    adminDb
      .collection("workspaces")
      .doc(WORKSPACE_ID)
      .collection("vehicles")
      .doc("veh_1")
      .set({
        localId: 1,
        name: "NADEEM",
        staffUserId: 2,
        active: true,
        memberUids: members,
        updatedByUid: ADMIN_UID,
        createdAt: ts,
        updatedAt: ts,
      })
  );
  await assertSucceeds(
    adminDb
      .collection("workspaces")
      .doc(WORKSPACE_ID)
      .collection("vehicles")
      .doc("veh_1")
      .update({
        name: "NADEEM KHAN",
        staffUserId: 2,
        active: true,
        memberUids: members,
        updatedByUid: ADMIN_UID,
        updatedAt: pastDate(300),
      })
  );
  await assertFails(
    adminDb
      .collection("workspaces")
      .doc(WORKSPACE_ID)
      .collection("vehicles")
      .doc("veh_1")
      .update({
        name: "V".repeat(65),
        updatedByUid: ADMIN_UID,
        updatedAt: pastDate(200),
      })
  );
});
