const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_diyar_123";
const BOB_UID = "bob_diyar_456";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function validPropertyPayload(ownerId, overrides = {}) {
  const now = new Date(Date.now() - 2000);
  return {
    ownerId,
    ownerDisplayName: "Diyar Verified Realty",
    titleEn: "Al Yasmin Contemporary Courtyard Villa",
    titleAr: "فيلا فناء معاصرة في الياسمين",
    descriptionEn:
      "Architectural 5-bedroom villa featuring floor-to-ceiling glazing, private pool, and certified finishes.",
    descriptionAr:
      "فيلا معمارية من 5 غرف نوم بواجهات زجاجية ممتدة ومسبح خاص وتشطيبات معتمدة.",
    listingType: "SALE",
    propertyType: "VILLA",
    status: "ACTIVE",
    verificationStatus: "VERIFIED",
    verified: true,
    deedReference: "DEED-4491-RYD",
    price: 3450000,
    currency: "SAR",
    areaSqm: 540,
    bedrooms: 5,
    bathrooms: 6,
    yearBuilt: 2024,
    amenities: ["POOL", "GARDEN", "SMART_HOME", "PARKING"],
    cityEn: "Riyadh",
    cityAr: "الرياض",
    neighborhoodEn: "Al Yasmin",
    neighborhoodAr: "الياسمين",
    latitude: 24.8235,
    longitude: 46.6345,
    hasVirtualTour: true,
    hasFloorPlan: true,
    heroPresetKey: "villa_exterior",
    ceilingHeightM: 3.6,
    floorPlanWidthM: 18.0,
    floorPlanLengthM: 30.0,
    rooms: [
      {
        nameEn: "Grand Living Hall",
        nameAr: "الصالة الرئيسية",
        widthM: 6.5,
        lengthM: 8.0,
        ceilingM: 3.8,
        paintNameEn: "Jotun Warm Sand",
        paintNameAr: "جوتن رمال دافئة",
        paintHex: "#EFE6D8",
        flooringEn: "Travertine Marble",
        flooringAr: "رخام ترافرتين",
      },
    ],
    tourScenes: [
      {
        sceneId: "living_main",
        titleEn: "Grand Living 360°",
        titleAr: "جولة الصالة الرئيسية ٣٦٠°",
        presetKey: "pano_living",
        initialYaw: 0,
      },
    ],
    nearbyLandmarks: [
      {
        category: "MOSQUE",
        nameEn: "Al Yasmin Grand Mosque",
        nameAr: "جامع الياسمين الكبير",
        distanceMeters: 280,
        walkMinutes: 4,
        latitude: 24.8245,
        longitude: 46.6352,
      },
    ],
    createdAt: now,
    updatedAt: now,
    ...overrides,
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

test("Unauthenticated user cannot read or write properties or user collections", async () => {
  const unauth = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauth.collection("properties").where("status", "==", "ACTIVE").get());
  await assertFails(
    unauth.collection("properties").doc("prop_1").set(validPropertyPayload(ALICE_UID))
  );
  await assertFails(unauth.collection("users").doc(ALICE_UID).get());
});

test("Property creation, query alignment, partial update, and cross-user isolation", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  // Checking existence of non-existent seed doc must succeed cleanly
  await assertSucceeds(aliceDb.collection("properties").doc("iq_seed_missing_1").get());

  // Alice creates valid active property with yearBuilt & amenities
  await assertSucceeds(
    aliceDb.collection("properties").doc("prop_1").set(validPropertyPayload(ALICE_UID))
  );

  // Alice creates valid IQD COMMERCIAL property
  await assertSucceeds(
    aliceDb
      .collection("properties")
      .doc("prop_comm_1")
      .set(
        validPropertyPayload(ALICE_UID, {
          propertyType: "COMMERCIAL",
          price: 1250000000,
          currency: "IQD",
          cityEn: "Baghdad",
          cityAr: "بغداد",
          galleryImages: ["preset:penthouse_exterior", "preset:pano_living", "preset:pano_kitchen"],
        })
      )
  );

  // Alice creates private draft property
  await assertSucceeds(
    aliceDb
      .collection("properties")
      .doc("prop_draft")
      .set(validPropertyPayload(ALICE_UID, { status: "DRAFT" }))
  );

  // Reject ghost field / @DocumentId `id` injection on create
  await assertFails(
    aliceDb
      .collection("properties")
      .doc("prop_bad_id")
      .set(validPropertyPayload(ALICE_UID, { id: "prop_bad_id" }))
  );

  // Bob can query ACTIVE properties (aligned with rule)
  await assertSucceeds(
    bobDb.collection("properties").where("status", "==", "ACTIVE").get()
  );

  // Unfiltered properties list query MUST fail
  await assertFails(bobDb.collection("properties").get());

  // Bob cannot read Alice's DRAFT property
  await assertFails(bobDb.collection("properties").doc("prop_draft").get());

  // Bob cannot update or delete Alice's property
  await assertFails(
    bobDb.collection("properties").doc("prop_1").update({
      price: 100,
      updatedAt: new Date(Date.now() - 500),
    })
  );
  await assertFails(bobDb.collection("properties").doc("prop_1").delete());

  // Alice can partially update price & updatedAt
  await assertSucceeds(
    aliceDb.collection("properties").doc("prop_1").update({
      price: 3300000,
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Alice cannot mutate immutable createdAt
  await assertFails(
    aliceDb.collection("properties").doc("prop_1").update({
      createdAt: new Date(Date.now() - 500),
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Terminal state lock: mark SOLD then block subsequent updates
  await assertSucceeds(
    aliceDb.collection("properties").doc("prop_1").update({
      status: "SOLD",
      updatedAt: new Date(Date.now() - 300),
    })
  );
  await assertFails(
    aliceDb.collection("properties").doc("prop_1").update({
      price: 3100000,
      updatedAt: new Date(Date.now() - 100),
    })
  );
});

test("User subcollections: private verifications PII, favorites with snapshot update, alert rules, alerts, and service requests", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const now = new Date(Date.now() - 2000);

  // Private verification PII
  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("private_verifications")
      .doc("prop_1")
      .set({
        userId: ALICE_UID,
        propertyId: "prop_1",
        fullDeedNumber: "DEED-99887766",
        licenseNumber: "REGA-123456",
        contactPhone: "+966500112233",
        verificationLevel: "VERIFIED",
        createdAt: now,
        updatedAt: now,
      })
  );
  await assertFails(
    bobDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("private_verifications")
      .doc("prop_1")
      .get()
  );

  // Favorites with status & price snapshot update
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("favorites").doc("prop_1").set({
      propertyId: "prop_1",
      titleEn: "Villa",
      titleAr: "فيلا",
      price: 3450000,
      status: "ACTIVE",
      cityEn: "Riyadh",
      cityAr: "الرياض",
      listingType: "SALE",
      propertyType: "VILLA",
      heroPresetKey: "villa_exterior",
      createdAt: now,
    })
  );
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("favorites").doc("prop_1").update({
      price: 3250000,
      status: "ACTIVE",
    })
  );
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("favorites").get()
  );
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("favorites").get()
  );

  // Alert rules & alerts
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("alert_rules").doc("rule_1").set({
      nameEn: "Riyadh Villas",
      nameAr: "فلل الرياض",
      listingType: "SALE",
      propertyType: "VILLA",
      maxPrice: 4000000,
      minAreaSqm: 350,
      cityFilter: "Riyadh",
      active: true,
      createdAt: now,
      updatedAt: now,
    })
  );
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("alerts").doc("alert_1").set({
      propertyId: "prop_1",
      ruleId: "rule_1",
      titleEn: "New Match: Al Yasmin Villa",
      titleAr: "تطابق جديد: فيلا الياسمين",
      messageEn: "Matches your Riyadh Villas alert rule.",
      messageAr: "يطابق تنبيه فلل الرياض الخاص بك.",
      price: 3450000,
      read: false,
      createdAt: now,
    })
  );
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("alerts").doc("alert_1").update({
      read: true,
    })
  );

  // Service requests
  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("service_requests")
      .doc("srv_1")
      .set({
        serviceCategory: "STAGING",
        packageTitleEn: "Full Interior Staging",
        packageTitleAr: "تجميل داخلي متكامل",
        propertyTitle: "Al Yasmin Villa",
        address: "Al Yasmin District, Riyadh",
        scheduledDate: "2026-11-25",
        notes: "Focus on living hall lighting",
        estimatedPrice: 6500,
        status: "REQUESTED",
        createdAt: now,
        updatedAt: now,
      })
  );
  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("service_requests")
      .doc("srv_1")
      .update({
        status: "CANCELLED",
        updatedAt: new Date(Date.now() - 500),
      })
  );
  // Terminal state lock on CANCELLED
  await assertFails(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("service_requests")
      .doc("srv_1")
      .update({
        status: "CONFIRMED",
        updatedAt: new Date(Date.now() - 200),
      })
  );
});
