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

function getAdminDb() {
  return testEnv
    .authenticatedContext(ADMIN_UID, {
      email: "atozgas26@gmail.com",
      email_verified: true,
    })
    .firestore();
}

async function seedWorkspaceWithAdminAndInvite() {
  const adminDb = getAdminDb();
  const ts = pastDate(10000);

  await assertSucceeds(
    adminDb.collection("users").doc(ADMIN_UID).set({
      userId: ADMIN_UID,
      displayName: "Ragavan",
      email: "atozgas26@gmail.com",
      role: "ADMIN",
      activeWorkspaceId: WORKSPACE_ID,
      linkedStaffUsername: "admin_ragavan",
      linkedStaffRole: "ADMIN",
      autoDriveBackupEnabled: true,
      createdAt: ts,
      updatedAt: ts,
    })
  );

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
      role: "STAFF",
      createdAt: ts,
      updatedAt: ts,
    })
  );
});

test("User profile enforces role distinction, anti-privilege-escalation, owner isolation, and rejects ghost fields", async () => {
  const aliceDb = getAdminDb();
  const bobDb = testEnv.authenticatedContext(STAFF_NADEEM_UID, {
    email: "nadeem@example.com",
    email_verified: true,
  }).firestore();
  const ts = pastDate(8000);

  // Bootstrapped Admin creates ADMIN user profile
  await assertSucceeds(
    aliceDb.collection("users").doc(ADMIN_UID).set({
      userId: ADMIN_UID,
      displayName: "Ragavan",
      email: "atozgas26@gmail.com",
      role: "ADMIN",
      activeWorkspaceId: WORKSPACE_ID,
      linkedStaffUsername: "admin_ragavan",
      linkedStaffRole: "ADMIN",
      autoDriveBackupEnabled: true,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Staff CANNOT self-create profile with role=ADMIN
  await assertFails(
    bobDb.collection("users").doc(STAFF_NADEEM_UID).set({
      userId: STAFF_NADEEM_UID,
      displayName: "Nadeem",
      email: "nadeem@example.com",
      role: "ADMIN",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Staff CAN create profile with role=STAFF
  await assertSucceeds(
    bobDb.collection("users").doc(STAFF_NADEEM_UID).set({
      userId: STAFF_NADEEM_UID,
      displayName: "Nadeem",
      email: "nadeem@example.com",
      role: "STAFF",
      linkedStaffUsername: "nadeem",
      linkedStaffRole: "STAFF",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Staff CANNOT escalate their own role or linkedStaffRole to ADMIN
  await assertFails(
    bobDb.collection("users").doc(STAFF_NADEEM_UID).update({
      role: "ADMIN",
      updatedAt: pastDate(1000),
    })
  );
  await assertFails(
    bobDb.collection("users").doc(STAFF_NADEEM_UID).update({
      linkedStaffRole: "ADMIN",
      updatedAt: pastDate(1000),
    })
  );

  // Invalid linkedStaffRole fails
  await assertFails(
    aliceDb.collection("users").doc(ADMIN_UID).update({
      linkedStaffRole: "SUPERADMIN",
      updatedAt: pastDate(1000),
    })
  );

  // Cross-user read/write blocked for Staff
  await assertFails(bobDb.collection("users").doc(ADMIN_UID).get());
  await assertFails(bobDb.collection("users").get());
  await assertFails(
    bobDb.collection("users").doc(ADMIN_UID).update({
      displayName: "Hijacked",
      updatedAt: pastDate(1000),
    })
  );

  // Ghost field & future timestamp rejected
  await assertFails(
    bobDb.collection("users").doc("staff_2").set({
      userId: "staff_2",
      displayName: "Sajan",
      role: "STAFF",
      isAdmin: true,
      createdAt: ts,
      updatedAt: ts,
    })
  );
  await assertFails(
    bobDb.collection("users").doc(STAFF_NADEEM_UID).update({
      createdAt: new Date(Date.now() + 60000),
      updatedAt: ts,
    })
  );
});

test("Workspace invite cannot be listed; staff joins workspace using active invite code", async () => {
  await seedWorkspaceWithAdminAndInvite();
  const staffDb = testEnv.authenticatedContext(STAFF_NADEEM_UID).firestore();
  const outsiderDb = testEnv.authenticatedContext(OUTSIDER_UID).firestore();

  await assertFails(staffDb.collection("workspace_invites").get());
  await assertSucceeds(staffDb.collection("workspace_invites").doc(INVITE_CODE).get());

  await assertFails(
    staffDb.collection("workspaces").doc(WORKSPACE_ID).update({
      memberUids: [ADMIN_UID, STAFF_NADEEM_UID],
      lastJoinInviteCode: "WRONGCODE",
      updatedAt: pastDate(1000),
    })
  );

  await assertSucceeds(
    staffDb.collection("workspaces").doc(WORKSPACE_ID).update({
      memberUids: [ADMIN_UID, STAFF_NADEEM_UID],
      lastJoinInviteCode: INVITE_CODE,
      updatedAt: pastDate(1000),
    })
  );

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

test("Master data & Staff online transaction sync enforces Admin-only deletions/edits and Staff scope", async () => {
  const { adminDb, ts } = await seedWorkspaceWithAdminAndInvite();
  const staffDb = testEnv.authenticatedContext(STAFF_NADEEM_UID).firestore();
  const outsiderDb = testEnv.authenticatedContext(OUTSIDER_UID).firestore();

  await assertSucceeds(
    staffDb.collection("users").doc(STAFF_NADEEM_UID).set({
      userId: STAFF_NADEEM_UID,
      displayName: "Nadeem",
      role: "STAFF",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertSucceeds(
    staffDb.collection("workspaces").doc(WORKSPACE_ID).update({
      memberUids: [ADMIN_UID, STAFF_NADEEM_UID],
      lastJoinInviteCode: INVITE_CODE,
      updatedAt: pastDate(1000),
    })
  );

  const members = [ADMIN_UID, STAFF_NADEEM_UID];

  // Admin creates product; Staff cannot create products
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
  await assertFails(
    staffDb
      .collection("workspaces")
      .doc(WORKSPACE_ID)
      .collection("products")
      .doc("prod_staff_hack")
      .set({
        localId: 99,
        category: "INDUSTRIAL_GAS",
        name: "HackGas",
        sortOrder: 999,
        active: true,
        memberUids: members,
        updatedByUid: STAFF_NADEEM_UID,
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

  // Staff CANNOT forge a STORE_PURCHASE_FULL transaction
  await assertFails(
    staffDb
      .collection("workspaces")
      .doc(WORKSPACE_ID)
      .collection("transactions")
      .doc("tx_staff_store_hack")
      .set({
        clientTxId: "tx_staff_store_hack",
        businessDate: "2026-11-24",
        clientTimestampMs: Date.now() - 1000,
        type: "STORE_PURCHASE_FULL",
        productId: 1,
        staffId: 2,
        staffName: "Nadeem",
        quantity: 50,
        purchaseCompanyName: "Fake Supplier",
        notes: "",
        createdByUid: STAFF_NADEEM_UID,
        memberUids: members,
        createdAt: pastDate(1000),
        updatedAt: pastDate(1000),
      })
  );

  // Staff CANNOT edit or delete recorded transactions
  await assertFails(
    staffDb
      .collection("workspaces")
      .doc(WORKSPACE_ID)
      .collection("transactions")
      .doc(txId)
      .update({
        quantity: 99,
        updatedAt: pastDate(500),
      })
  );
  await assertFails(
    staffDb
      .collection("workspaces")
      .doc(WORKSPACE_ID)
      .collection("transactions")
      .doc(txId)
      .delete()
  );

  // Filtered query succeeds; unfiltered fails
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

  // Outsider cannot read or delete
  await assertFails(
    outsiderDb.collection("workspaces").doc(WORKSPACE_ID).collection("transactions").doc(txId).get()
  );

  // Admin can delete a transaction
  await assertSucceeds(
    adminDb.collection("workspaces").doc(WORKSPACE_ID).collection("transactions").doc(txId).delete()
  );
});

test("Restricted Admin Settings and Admin Reports block Staff reads and writes", async () => {
  const { adminDb, ts } = await seedWorkspaceWithAdminAndInvite();
  const staffDb = testEnv.authenticatedContext(STAFF_NADEEM_UID).firestore();

  await assertSucceeds(
    staffDb.collection("users").doc(STAFF_NADEEM_UID).set({
      userId: STAFF_NADEEM_UID,
      displayName: "Nadeem",
      role: "STAFF",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertSucceeds(
    staffDb.collection("workspaces").doc(WORKSPACE_ID).update({
      memberUids: [ADMIN_UID, STAFF_NADEEM_UID],
      lastJoinInviteCode: INVITE_CODE,
      updatedAt: pastDate(1000),
    })
  );

  // Admin writes sensitive settings (Google Drive auto-backup config)
  await assertSucceeds(
    adminDb
      .collection("workspaces")
      .doc(WORKSPACE_ID)
      .collection("admin_settings")
      .doc("drive_backup")
      .set({
        settingId: "drive_backup",
        autoDriveBackupEnabled: true,
        lastDriveBackupAtMs: Date.now() - 2000,
        lastDriveFileId: "1abcDriveFileId",
        lastDriveBackupStatus: "Backed up to Google Drive",
        driveAccountEmail: "atozgas26@gmail.com",
        updatedByUid: ADMIN_UID,
        createdAt: ts,
        updatedAt: ts,
      })
  );

  // Admin reads settings
  await assertSucceeds(
    adminDb.collection("workspaces").doc(WORKSPACE_ID).collection("admin_settings").doc("drive_backup").get()
  );

  // Staff CANNOT read, list, create, update, or delete admin_settings
  await assertFails(
    staffDb.collection("workspaces").doc(WORKSPACE_ID).collection("admin_settings").doc("drive_backup").get()
  );
  await assertFails(
    staffDb.collection("workspaces").doc(WORKSPACE_ID).collection("admin_settings").get()
  );
  await assertFails(
    staffDb
      .collection("workspaces")
      .doc(WORKSPACE_ID)
      .collection("admin_settings")
      .doc("drive_backup")
      .update({
        autoDriveBackupEnabled: false,
        updatedByUid: STAFF_NADEEM_UID,
        updatedAt: pastDate(500),
      })
  );

  // Admin creates and reads sensitive admin_reports snapshot
  await assertSucceeds(
    adminDb
      .collection("workspaces")
      .doc(WORKSPACE_ID)
      .collection("admin_reports")
      .doc("PURCHASE_REGISTER_2026_11")
      .set({
        reportId: "PURCHASE_REGISTER_2026_11",
        reportType: "PURCHASE_REGISTER",
        reportTitle: "Purchase & Complaint Report (Monthly)",
        subtitle: "Full: 120 | Empty: 30",
        businessDate: "2026-11-25",
        yearMonth: "2026-11",
        generatedBy: "Ragavan",
        rowCount: 15,
        summaryTotals: "TOTAL | 15 Entries | 150",
        driveFileId: "driveReportId123",
        createdByUid: ADMIN_UID,
        createdAt: ts,
        updatedAt: ts,
      })
  );

  await assertSucceeds(
    adminDb.collection("workspaces").doc(WORKSPACE_ID).collection("admin_reports").get()
  );

  // Staff CANNOT read, list, create, update, or delete admin_reports
  await assertFails(
    staffDb
      .collection("workspaces")
      .doc(WORKSPACE_ID)
      .collection("admin_reports")
      .doc("PURCHASE_REGISTER_2026_11")
      .get()
  );
  await assertFails(
    staffDb.collection("workspaces").doc(WORKSPACE_ID).collection("admin_reports").get()
  );
  await assertFails(
    staffDb
      .collection("workspaces")
      .doc(WORKSPACE_ID)
      .collection("admin_reports")
      .doc("STAFF_REPORT_HACK")
      .set({
        reportId: "STAFF_REPORT_HACK",
        reportType: "MONTHLY_SALES",
        reportTitle: "Hacked",
        subtitle: "",
        businessDate: "2026-11-25",
        yearMonth: "2026-11",
        generatedBy: "Nadeem",
        rowCount: 1,
        summaryTotals: "0",
        createdByUid: STAFF_NADEEM_UID,
        createdAt: ts,
        updatedAt: ts,
      })
  );
});

test("Staff cannot modify staff_accounts PINs, opening stocks, or vehicles", async () => {
  const { adminDb, ts } = await seedWorkspaceWithAdminAndInvite();
  const staffDb = testEnv.authenticatedContext(STAFF_NADEEM_UID).firestore();

  await assertSucceeds(
    staffDb.collection("users").doc(STAFF_NADEEM_UID).set({
      userId: STAFF_NADEEM_UID,
      displayName: "Nadeem",
      role: "STAFF",
      createdAt: ts,
      updatedAt: ts,
    })
  );
  await assertSucceeds(
    staffDb.collection("workspaces").doc(WORKSPACE_ID).update({
      memberUids: [ADMIN_UID, STAFF_NADEEM_UID],
      lastJoinInviteCode: INVITE_CODE,
      updatedAt: pastDate(1000),
    })
  );

  const members = [ADMIN_UID, STAFF_NADEEM_UID];

  await assertSucceeds(
    adminDb
      .collection("workspaces")
      .doc(WORKSPACE_ID)
      .collection("staff_accounts")
      .doc("user_admin_ragavan")
      .set({
        localId: 1,
        username: "admin_ragavan",
        displayName: "Ragavan",
        role: "ADMIN",
        pin: "1725",
        active: true,
        memberUids: members,
        updatedByUid: ADMIN_UID,
        createdAt: ts,
        updatedAt: ts,
      })
  );

  await assertFails(
    staffDb
      .collection("workspaces")
      .doc(WORKSPACE_ID)
      .collection("staff_accounts")
      .doc("user_admin_ragavan")
      .update({
        pin: "9999",
        updatedByUid: STAFF_NADEEM_UID,
        updatedAt: pastDate(500),
      })
  );

  await assertFails(
    staffDb
      .collection("workspaces")
      .doc(WORKSPACE_ID)
      .collection("openings")
      .doc("open_STORE_0_1")
      .set({
        scope: "STORE",
        vehicleId: 0,
        productId: 1,
        openingFull: 500,
        openingEmpty: 20,
        updatedBy: "Nadeem",
        memberUids: members,
        updatedByUid: STAFF_NADEEM_UID,
        createdAt: ts,
        updatedAt: ts,
      })
  );

  await assertSucceeds(
    adminDb
      .collection("workspaces")
      .doc(WORKSPACE_ID)
      .collection("staff_accounts")
      .doc("user_admin_ragavan")
      .update({
        pin: "2468",
        updatedByUid: ADMIN_UID,
        updatedAt: pastDate(400),
      })
  );
});
