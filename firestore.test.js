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
const ADMIN_UID = "admin_789";
const ADMIN_EMAIL = "jacobvijay9597@gmail.com";

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

function pastDate(msOffset = 2000) {
  return new Date(Date.now() - msOffset);
}

test("Unauthenticated users cannot read or write any collection", async () => {
  const unauth = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauth.collection("users").doc(ALICE_UID).get());
  await assertFails(
    unauth.collection("community_reports").where("visibility", "==", "PUBLIC").get()
  );
  await assertFails(
    unauth.collection("official_advisories").where("published", "==", true).get()
  );
  await assertFails(
    unauth.collection("road_corridors").where("active", "==", true).get()
  );
});

test("User profile: owner create, partial update, and PII isolation", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID, {
    email: "alice@example.com",
    email_verified: true,
  }).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID, {
    email: "bob@example.com",
    email_verified: true,
  }).firestore();

  const ts = pastDate(5000);
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Alice",
      email: "alice@example.com",
      subscribedCorridors: ["ooty_coonoor_nh181"],
      alertsEnabled: true,
      riskThreshold: "MEDIUM",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Partial update succeeds
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      subscribedCorridors: ["ooty_coonoor_nh181", "kalhatti_ghat"],
      riskThreshold: "HIGH",
      updatedAt: pastDate(1000),
    })
  );

  // Cross-user PII read/write fails
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).update({
      alertsEnabled: false,
      updatedAt: pastDate(1000),
    })
  );

  // Unfiltered list on users fails for non-admin
  await assertFails(bobDb.collection("users").get());
  await assertSucceeds(
    aliceDb.collection("users").where("userId", "==", ALICE_UID).get()
  );
});

test("Community reports: valid Kotlin payload, anti-spoofing, terminal state lock, and filtered queries", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const adminDb = testEnv.authenticatedContext(ADMIN_UID, {
    email: ADMIN_EMAIL,
    email_verified: true,
  }).firestore();
  const ts = pastDate(5000);

  // Valid community report creation (must be UNVERIFIED & PUBLIC, no adminNotes, no @DocumentId id)
  await assertSucceeds(
    aliceDb.collection("community_reports").doc("rep_1").set({
      reporterUid: ALICE_UID,
      reporterName: "Alice",
      corridorId: "mettupalayam_coonoor",
      corridorName: "Mettupalayam – Coonoor (NH-181)",
      locationLabel: "Hairpin Bend 7 near Kallar",
      incidentType: "ROCKFALL",
      severity: "HIGH",
      description: "Loose boulders and mudflow obstructing uphill lane.",
      observedTimeText: "08:30 AM IST",
      verificationStatus: "UNVERIFIED",
      visibility: "PUBLIC",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Reject self-verification or ghost @DocumentId `id` field
  await assertFails(
    aliceDb.collection("community_reports").doc("rep_spoof").set({
      reporterUid: ALICE_UID,
      reporterName: "Alice",
      corridorId: "mettupalayam_coonoor",
      corridorName: "Mettupalayam – Coonoor (NH-181)",
      locationLabel: "Bend 2",
      incidentType: "LANDSLIDE",
      severity: "HIGH",
      description: "Attempted self-verified report.",
      observedTimeText: "09:00 AM",
      verificationStatus: "CONFIRMED",
      visibility: "PUBLIC",
      createdAt: ts,
      updatedAt: ts,
    })
  );
  await assertFails(
    aliceDb.collection("community_reports").doc("rep_with_id").set({
      id: "rep_with_id",
      reporterUid: ALICE_UID,
      reporterName: "Alice",
      corridorId: "mettupalayam_coonoor",
      corridorName: "Mettupalayam – Coonoor (NH-181)",
      locationLabel: "Bend 3",
      incidentType: "LANDSLIDE",
      severity: "MEDIUM",
      description: "Report containing disallowed id field.",
      observedTimeText: "09:00 AM",
      verificationStatus: "UNVERIFIED",
      visibility: "PUBLIC",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Bob cannot modify Alice's report
  await assertFails(
    bobDb.collection("community_reports").doc("rep_1").update({
      description: "Hijacked update",
      updatedAt: pastDate(1000),
    })
  );

  // Filtered public and owner queries succeed; unfiltered list fails for non-admin
  await assertSucceeds(
    bobDb.collection("community_reports").where("visibility", "==", "PUBLIC").get()
  );
  await assertSucceeds(
    aliceDb.collection("community_reports").where("reporterUid", "==", ALICE_UID).get()
  );
  await assertFails(bobDb.collection("community_reports").get());

  // Admin confirms report
  await assertSucceeds(
    adminDb.collection("community_reports").doc("rep_1").update({
      verificationStatus: "CONFIRMED",
      adminNotes: "Confirmed by Nilgiris patrol.",
      updatedAt: pastDate(1000),
    })
  );

  // Terminal state lock: Alice can no longer edit once CONFIRMED
  await assertFails(
    aliceDb.collection("community_reports").doc("rep_1").update({
      description: "Trying to alter confirmed record",
      updatedAt: pastDate(500),
    })
  );
});

test("Email spoofing test & admin-only official advisories / road corridors", async () => {
  const spoofedAdminDb = testEnv.authenticatedContext("spoof_uid", {
    email: ADMIN_EMAIL,
    email_verified: false,
  }).firestore();
  const realAdminDb = testEnv.authenticatedContext(ADMIN_UID, {
    email: ADMIN_EMAIL,
    email_verified: true,
  }).firestore();
  const travellerDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const ts = pastDate(4000);

  const advisoryPayload = {
    title: "Coonoor Ghat Traffic Advisory",
    corridorId: "mettupalayam_coonoor",
    corridorName: "Mettupalayam – Coonoor (NH-181)",
    severity: "HIGH",
    sourceType: "OFFICIAL_ADVISORY",
    sourceLabel: "Nilgiris District Control (Simulated Demo)",
    summary: "Heavy rain forecast along Kallar-Burliar stretch.",
    roadClosed: false,
    published: true,
    simulatedDemo: true,
    createdByUid: "spoof_uid",
    createdAt: ts,
    updatedAt: ts,
  };

  // Spoofed unverified admin email MUST fail
  await assertFails(
    spoofedAdminDb.collection("official_advisories").doc("adv_1").set(advisoryPayload)
  );

  // Real verified admin succeeds
  await assertSucceeds(
    realAdminDb.collection("official_advisories").doc("adv_1").set({
      ...advisoryPayload,
      createdByUid: ADMIN_UID,
    })
  );

  // Traveller can read published advisories with filtered query, not unfiltered
  await assertSucceeds(
    travellerDb.collection("official_advisories").where("published", "==", true).get()
  );
  await assertFails(travellerDb.collection("official_advisories").get());
  await assertFails(
    travellerDb.collection("official_advisories").doc("adv_1").update({
      roadClosed: true,
      updatedAt: pastDate(1000),
    })
  );

  // Road corridors: admin write, traveller filtered read
  await assertSucceeds(
    realAdminDb.collection("road_corridors").doc("kalhatti_ghat").set({
      corridorName: "Kalhatti Ghat (Ooty – Masinagudi)",
      status: "CAUTION",
      statusNote: "Steep 36-hairpin descent; check brake & rain status.",
      verifiedSafety: false,
      active: true,
      simulatedDemo: true,
      updatedByUid: ADMIN_UID,
      createdAt: ts,
      updatedAt: ts,
    })
  );
  await assertSucceeds(
    travellerDb.collection("road_corridors").where("active", "==", true).get()
  );
  await assertFails(travellerDb.collection("road_corridors").get());

  // Admin can list all community_reports and update corridors/advisories while preserving createdAt
  await assertSucceeds(realAdminDb.collection("community_reports").get());
  await assertSucceeds(
    realAdminDb.collection("road_corridors").doc("kalhatti_ghat").update({
      status: "CLOSED_OFFICIAL",
      statusNote: "Re-seeded official closure.",
      verifiedSafety: false,
      active: true,
      simulatedDemo: true,
      updatedByUid: ADMIN_UID,
      updatedAt: pastDate(500),
    })
  );
  await assertSucceeds(
    realAdminDb.collection("official_advisories").doc("demo_adv_burliar").set({
      title: "NH-181 Burliar Slope Precaution (Simulated Demo)",
      corridorId: "mettupalayam_coonoor",
      corridorName: "Mettupalayam – Coonoor Ghat",
      severity: "HIGH",
      sourceType: "OFFICIAL_ADVISORY",
      sourceLabel: "Simulated District Control Demo",
      summary: "Sample hackathon demonstration advisory.",
      roadClosed: false,
      published: true,
      simulatedDemo: true,
      createdByUid: ADMIN_UID,
      createdAt: ts,
      updatedAt: ts,
    })
  );
  await assertSucceeds(
    realAdminDb.collection("official_advisories").doc("demo_adv_burliar").update({
      summary: "Updated demonstration advisory text.",
      updatedAt: pastDate(500),
    })
  );
});
