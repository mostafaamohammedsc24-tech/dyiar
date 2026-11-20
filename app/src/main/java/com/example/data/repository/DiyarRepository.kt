package com.example.data.repository

import com.example.data.model.AlertRule
import com.example.data.model.FavoriteProperty
import com.example.data.model.NearbyLandmark
import com.example.data.model.PropertyAlert
import com.example.data.model.PropertyListing
import com.example.data.model.RoomSpec
import com.example.data.model.ServicePackage
import com.example.data.model.ServiceRequest
import com.example.data.model.TourScene
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class DiyarRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    fun observeActiveProperties(): Flow<List<PropertyListing>> = flow {
        val uid = requireUserId()
        emitAll(
            db.collection("properties")
                .whereEqualTo("status", "ACTIVE")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        PropertyListing::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .catch { error ->
                    val fsErr = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (fsErr == null && error is CancellationException) throw error
                    if (auth.currentUser?.uid == uid) {
                        handleFirestoreError(fsErr ?: error, OperationType.LIST, "properties")
                    }
                    throw error
                }
        )
    }

    fun observeMyProperties(userId: String): Flow<List<PropertyListing>> = flow {
        requireUserId()
        emitAll(
            db.collection("properties")
                .whereEqualTo("ownerId", userId)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        PropertyListing::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .catch { error ->
                    val fsErr = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (fsErr == null && error is CancellationException) throw error
                    if (auth.currentUser?.uid == userId) {
                        handleFirestoreError(fsErr ?: error, OperationType.LIST, "properties")
                    }
                    throw error
                }
        )
    }

    fun observeFavorites(userId: String): Flow<List<FavoriteProperty>> = flow {
        requireUserId()
        val path = "users/$userId/favorites"
        emitAll(
            db.collection("users").document(userId).collection("favorites")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        FavoriteProperty::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .catch { error ->
                    val fsErr = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (fsErr == null && error is CancellationException) throw error
                    if (auth.currentUser?.uid == userId) {
                        handleFirestoreError(fsErr ?: error, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    fun observeAlertRules(userId: String): Flow<List<AlertRule>> = flow {
        requireUserId()
        val path = "users/$userId/alert_rules"
        emitAll(
            db.collection("users").document(userId).collection("alert_rules")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        AlertRule::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .catch { error ->
                    val fsErr = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (fsErr == null && error is CancellationException) throw error
                    if (auth.currentUser?.uid == userId) {
                        handleFirestoreError(fsErr ?: error, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    fun observeAlerts(userId: String): Flow<List<PropertyAlert>> = flow {
        requireUserId()
        val path = "users/$userId/alerts"
        emitAll(
            db.collection("users").document(userId).collection("alerts")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        PropertyAlert::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .catch { error ->
                    val fsErr = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (fsErr == null && error is CancellationException) throw error
                    if (auth.currentUser?.uid == userId) {
                        handleFirestoreError(fsErr ?: error, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    fun observeServiceRequests(userId: String): Flow<List<ServiceRequest>> = flow {
        requireUserId()
        val path = "users/$userId/service_requests"
        emitAll(
            db.collection("users").document(userId).collection("service_requests")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        ServiceRequest::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .catch { error ->
                    val fsErr = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (fsErr == null && error is CancellationException) throw error
                    if (auth.currentUser?.uid == userId) {
                        handleFirestoreError(fsErr ?: error, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun ensureUserProfile(preferredLanguage: String): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val path = "users/$uid"
        return try {
            val docRef = db.collection("users").document(uid)
            val existing = docRef.get().await()
            val displayName = auth.currentUser?.displayName?.takeIf { it.isNotBlank() }?.take(80)
                ?: "Diyar Member"
            val photoUrl = auth.currentUser?.photoUrl?.toString()?.take(500)
            if (!existing.exists()) {
                val payload = mapOf(
                    "userId" to uid,
                    "displayName" to displayName,
                    "photoUrl" to photoUrl,
                    "preferredLanguage" to preferredLanguage,
                    "verifiedPublisher" to false,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                ).filterValues { it != null }
                docRef.set(payload).await()
            } else {
                docRef.update(
                    mapOf(
                        "preferredLanguage" to preferredLanguage,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun publishProperty(
        listing: PropertyListing,
        fullDeedNumber: String,
        licenseNumber: String,
        contactPhone: String
    ): Result<String> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val docRef = db.collection("properties").document()
        val propId = docRef.id
        return try {
            val maskedDeed = if (fullDeedNumber.length >= 4) {
                "DEED-****-${fullDeedNumber.takeLast(4)}"
            } else {
                "DEED-$fullDeedNumber"
            }
            val ownerName = (auth.currentUser?.displayName?.takeIf { it.isNotBlank() } ?: listing.ownerDisplayName)
                .ifBlank { "Diyar Verified Publisher" }
                .take(80)

            val propPayload = mapOf(
                "ownerId" to uid,
                "ownerDisplayName" to ownerName,
                "titleEn" to listing.titleEn.trim().take(140),
                "titleAr" to listing.titleAr.trim().take(140),
                "descriptionEn" to listing.descriptionEn.trim().take(2500),
                "descriptionAr" to listing.descriptionAr.trim().take(2500),
                "listingType" to listing.listingType,
                "propertyType" to listing.propertyType,
                "status" to "ACTIVE",
                "verificationStatus" to "VERIFIED",
                "verified" to true,
                "deedReference" to maskedDeed.take(64),
                "price" to listing.price.coerceIn(1.0, 20_000_000_000.0),
                "currency" to listing.currency.ifBlank { "IQD" }.take(8),
                "areaSqm" to listing.areaSqm.coerceIn(1.0, 1_000_000.0),
                "bedrooms" to listing.bedrooms.coerceIn(0L, 30L),
                "bathrooms" to listing.bathrooms.coerceIn(0L, 30L),
                "yearBuilt" to listing.yearBuilt.coerceIn(1950L, 2030L),
                "amenities" to listing.amenities.take(20),
                "cityEn" to listing.cityEn.trim().take(80),
                "cityAr" to listing.cityAr.trim().take(80),
                "neighborhoodEn" to listing.neighborhoodEn.trim().take(100),
                "neighborhoodAr" to listing.neighborhoodAr.trim().take(100),
                "latitude" to listing.latitude.coerceIn(-90.0, 90.0),
                "longitude" to listing.longitude.coerceIn(-180.0, 180.0),
                "hasVirtualTour" to listing.tourScenes.isNotEmpty(),
                "hasFloorPlan" to listing.rooms.isNotEmpty(),
                "heroPresetKey" to listing.heroPresetKey.ifBlank { "villa_exterior" }.take(64),
                "galleryImages" to listing.galleryImages.take(20),
                "ceilingHeightM" to listing.ceilingHeightM.coerceIn(2.0, 20.0),
                "floorPlanWidthM" to listing.floorPlanWidthM.coerceIn(1.0, 200.0),
                "floorPlanLengthM" to listing.floorPlanLengthM.coerceIn(1.0, 200.0),
                "rooms" to listing.rooms.take(20).map { it.toMap() },
                "tourScenes" to listing.tourScenes.take(15).map { it.toMap() },
                "nearbyLandmarks" to listing.nearbyLandmarks.take(20).map { it.toMap() },
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(propPayload).await()

            val verifRef = db.collection("users").document(uid)
                .collection("private_verifications").document(propId)
            verifRef.set(
                mapOf(
                    "userId" to uid,
                    "propertyId" to propId,
                    "fullDeedNumber" to fullDeedNumber.trim().take(64),
                    "licenseNumber" to licenseNumber.trim().take(64),
                    "contactPhone" to contactPhone.trim().take(30),
                    "verificationLevel" to "VERIFIED",
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            Result.success(propId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, "properties/$propId")
            Result.failure(e)
        }
    }

    suspend fun markPropertySold(propertyId: String): Result<Unit> {
        val path = "properties/$propertyId"
        return try {
            requireUserId()
            db.collection("properties").document(propertyId)
                .update(
                    mapOf(
                        "status" to "SOLD",
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteProperty(propertyId: String): Result<Unit> {
        val path = "properties/$propertyId"
        return try {
            requireUserId()
            db.collection("properties").document(propertyId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    suspend fun toggleFavorite(listing: PropertyListing, currentlySaved: Boolean): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val path = "users/$uid/favorites/${listing.id}"
        return try {
            val favRef = db.collection("users").document(uid)
                .collection("favorites").document(listing.id)
            if (currentlySaved) {
                favRef.delete().await()
            } else {
                favRef.set(
                    mapOf(
                        "propertyId" to listing.id,
                        "titleEn" to listing.titleEn.take(140),
                        "titleAr" to listing.titleAr.take(140),
                        "price" to listing.price,
                        "status" to listing.status,
                        "cityEn" to listing.cityEn.take(80),
                        "cityAr" to listing.cityAr.take(80),
                        "listingType" to listing.listingType,
                        "propertyType" to listing.propertyType,
                        "heroPresetKey" to listing.heroPresetKey.ifBlank { "villa_exterior" },
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun updateFavoriteSnapshot(listing: PropertyListing): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val path = "users/$uid/favorites/${listing.id}"
        return try {
            db.collection("users").document(uid)
                .collection("favorites").document(listing.id)
                .update(
                    mapOf(
                        "price" to listing.price,
                        "status" to listing.status,
                        "titleEn" to listing.titleEn.take(140),
                        "titleAr" to listing.titleAr.take(140)
                    )
                ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun createAlertRule(rule: AlertRule): Result<String> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val docRef = db.collection("users").document(uid).collection("alert_rules").document()
        return try {
            docRef.set(
                mapOf(
                    "nameEn" to rule.nameEn.trim().take(100),
                    "nameAr" to rule.nameAr.trim().take(100),
                    "listingType" to rule.listingType,
                    "propertyType" to rule.propertyType,
                    "maxPrice" to rule.maxPrice.coerceIn(0.0, 20_000_000_000.0),
                    "minAreaSqm" to rule.minAreaSqm.coerceIn(0.0, 1_000_000.0),
                    "cityFilter" to rule.cityFilter.trim().take(80),
                    "active" to true,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun toggleAlertRule(ruleId: String, active: Boolean): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val docRef = db.collection("users").document(uid).collection("alert_rules").document(ruleId)
        return try {
            docRef.update(
                mapOf(
                    "active" to active,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun deleteAlertRule(ruleId: String): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val docRef = db.collection("users").document(uid).collection("alert_rules").document(ruleId)
        return try {
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun createAlertIfNotExists(alertId: String, alert: PropertyAlert): Result<Boolean> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val docRef = db.collection("users").document(uid).collection("alerts").document(alertId)
        return try {
            val snap = docRef.get().await()
            if (snap.exists()) return Result.success(false)
            docRef.set(
                mapOf(
                    "propertyId" to alert.propertyId,
                    "ruleId" to alert.ruleId,
                    "titleEn" to alert.titleEn.take(140),
                    "titleAr" to alert.titleAr.take(140),
                    "messageEn" to alert.messageEn.take(300),
                    "messageAr" to alert.messageAr.take(300),
                    "price" to alert.price,
                    "read" to false,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(true)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun markAlertRead(alertId: String): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val docRef = db.collection("users").document(uid).collection("alerts").document(alertId)
        return try {
            docRef.update(mapOf("read" to true)).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun bookServiceRequest(request: ServiceRequest): Result<String> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val docRef = db.collection("users").document(uid).collection("service_requests").document()
        return try {
            val payload = mapOf(
                "serviceCategory" to request.serviceCategory,
                "packageTitleEn" to request.packageTitleEn.take(120),
                "packageTitleAr" to request.packageTitleAr.take(120),
                "propertyTitle" to request.propertyTitle.trim().ifBlank { "My Residence" }.take(140),
                "address" to request.address.trim().take(200),
                "scheduledDate" to request.scheduledDate.trim().take(40),
                "notes" to request.notes?.trim()?.takeIf { it.isNotEmpty() }?.take(500),
                "estimatedPrice" to request.estimatedPrice.coerceIn(1.0, 500_000_000.0),
                "status" to "REQUESTED",
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null }
            docRef.set(payload).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun cancelServiceRequest(requestId: String): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val docRef = db.collection("users").document(uid).collection("service_requests").document(requestId)
        return try {
            docRef.update(
                mapOf(
                    "status" to "CANCELLED",
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun seedInitialMarketplaceIfEmpty(): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        return try {
            val ownedSnapshot = db.collection("properties")
                .whereEqualTo("ownerId", uid)
                .get()
                .await()

            // Migrate away legacy Riyadh seed documents if owned by this user
            val legacyPrefix = "seed_${uid.take(8)}_"
            for (doc in ownedSnapshot.documents) {
                if (doc.id.startsWith(legacyPrefix)) {
                    try {
                        doc.reference.delete().await()
                    } catch (_: Exception) {
                    }
                }
            }

            val existingIds = ownedSnapshot.documents.map { it.id }.toSet()
            val showcaseList = buildShowcaseProperties(uid)
            for ((index, prop) in showcaseList.withIndex()) {
                val docId = "iq_seed_${uid.take(8)}_${index + 1}"
                if (existingIds.contains(docId)) continue
                val docRef = db.collection("properties").document(docId)
                val payload = mapOf(
                    "ownerId" to uid,
                    "ownerDisplayName" to prop.ownerDisplayName,
                    "titleEn" to prop.titleEn,
                    "titleAr" to prop.titleAr,
                    "descriptionEn" to prop.descriptionEn,
                    "descriptionAr" to prop.descriptionAr,
                    "listingType" to prop.listingType,
                    "propertyType" to prop.propertyType,
                    "status" to "ACTIVE",
                    "verificationStatus" to "VERIFIED",
                    "verified" to true,
                    "deedReference" to prop.deedReference,
                    "price" to prop.price,
                    "currency" to prop.currency,
                    "areaSqm" to prop.areaSqm,
                    "bedrooms" to prop.bedrooms,
                    "bathrooms" to prop.bathrooms,
                    "yearBuilt" to prop.yearBuilt,
                    "amenities" to prop.amenities,
                    "cityEn" to prop.cityEn,
                    "cityAr" to prop.cityAr,
                    "neighborhoodEn" to prop.neighborhoodEn,
                    "neighborhoodAr" to prop.neighborhoodAr,
                    "latitude" to prop.latitude,
                    "longitude" to prop.longitude,
                    "hasVirtualTour" to true,
                    "hasFloorPlan" to true,
                    "heroPresetKey" to prop.heroPresetKey,
                    "galleryImages" to prop.galleryImages,
                    "ceilingHeightM" to prop.ceilingHeightM,
                    "floorPlanWidthM" to prop.floorPlanWidthM,
                    "floorPlanLengthM" to prop.floorPlanLengthM,
                    "rooms" to prop.rooms.map { it.toMap() },
                    "tourScenes" to prop.tourScenes.map { it.toMap() },
                    "nearbyLandmarks" to prop.nearbyLandmarks.map { it.toMap() },
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(payload).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "properties/seed")
            Result.failure(e)
        }
    }

    companion object {
        val defaultServicePackages: List<ServicePackage> = listOf(
            ServicePackage(
                id = "srv_staging_1",
                category = "STAGING",
                titleEn = "Luxury Home Staging & Color Refresh",
                titleAr = "التجميل العقاري وتجديد الدهانات الفاخر",
                subtitleEn = "Curated modern furniture staging, warm architectural lighting, and Jotun feature wall repainting across Baghdad & Erbil.",
                subtitleAr = "تأثيث عصري متكامل، إضاءة معمارية دافئة، وطلاء جدران جوتن لإبراز قيمة العقار في بغداد والمحافظات.",
                durationEn = "2–3 Days",
                durationAr = "٢–٣ أيام",
                priceSar = 1_850_000.0,
                rating = 4.9
            ),
            ServicePackage(
                id = "srv_maint_1",
                category = "MAINTENANCE",
                titleEn = "Comprehensive HVAC, Solar & Plumbing Check",
                titleAr = "صيانة شاملة للتبريد والسباكة والمنظومة الذكية",
                subtitleEn = "36-point preventive engineering inspection covering split/central AC, backup inverter systems, and water pumps.",
                subtitleAr = "فحص هندسي وقائي من ٣٦ نقطة يشمل التبريد المركزي والسبلت ومنظومة الطاقة والكهرباء والسباكة.",
                durationEn = "Same Day (4 hrs)",
                durationAr = "نفس اليوم (٤ ساعات)",
                priceSar = 250_000.0,
                rating = 4.8
            ),
            ServicePackage(
                id = "srv_repair_1",
                category = "REPAIR",
                titleEn = "Marble Polishing, Gypsum & Structural Repair",
                titleAr = "تصليح وترميم الجبس وجلي المرمر",
                subtitleEn = "Precision wall crack restoration, rooftop waterproofing, and natural marble diamond-pad crystallization.",
                subtitleAr = "معالجة التشققات الدقيقة والعزل الحراري والمائي للأسطح وجلي وتلميع المرمر الإيطالي والتركي.",
                durationEn = "1–2 Days",
                durationAr = "١–٢ يوم",
                priceSar = 750_000.0,
                rating = 4.9
            ),
            ServicePackage(
                id = "srv_moving_1",
                category = "MOVING",
                titleEn = "White-Glove Furniture Moving & Assembly",
                titleAr = "نقل الأثاث والتغليف والتركيب الاحترافي",
                subtitleEn = "Enclosed moving trucks, multi-layer foam wrap protection, carpentry disassembly/reassembly across all governorates.",
                subtitleAr = "شاحنات مغلقة ومخصصة، تغليف حماية متعدد الطبقات، فك وتركيب الأثاث في جميع مناطق العراق.",
                durationEn = "1 Day",
                durationAr = "يوم واحد",
                priceSar = 550_000.0,
                rating = 4.9
            )
        )

        private fun buildShowcaseProperties(uid: String): List<PropertyListing> = listOf(
            PropertyListing(
                ownerId = uid,
                ownerDisplayName = "Diyar Baghdad Architectural Studio",
                titleEn = "Al-Mansour Courtyard Smart Villa",
                titleAr = "فيلا المنصور الذكية بفناء ومسبح",
                descriptionEn = "Flagship 5-bedroom villa near Al-Amirat Street with Iraqi limestone cladding, double-height glazing, private pool, backup hybrid solar system, room-by-room Jotun paint specs, and a complete 360° interactive walkthrough.",
                descriptionAr = "فيلا فاخرة قرب شارع الأميرات في المنصور بتصميم معماري حديث وواجهات حجر طبيعي، تضم ٥ غرف نوم ومسبحاً داخلياً ومنظومة طاقة شمسية متكاملة. موثقة بقياسات الغرف وأكواد الطلاء وجولة ٣٦٠°.",
                listingType = "SALE",
                propertyType = "VILLA",
                status = "ACTIVE",
                verificationStatus = "VERIFIED",
                verified = true,
                deedReference = "TAPU-****-8419",
                price = 950_000_000.0,
                currency = "IQD",
                areaSqm = 540.0,
                bedrooms = 5,
                bathrooms = 6,
                yearBuilt = 2025,
                amenities = listOf("POOL", "GARDEN", "SMART_HOME", "PARKING", "MAID_ROOM"),
                cityEn = "Baghdad",
                cityAr = "بغداد",
                neighborhoodEn = "Al-Mansour",
                neighborhoodAr = "المنصور",
                latitude = 33.3185,
                longitude = 44.3422,
                hasVirtualTour = true,
                hasFloorPlan = true,
                heroPresetKey = "villa_exterior",
                galleryImages = listOf("preset:villa_exterior", "preset:pano_living", "preset:pano_kitchen", "preset:services_hero"),
                ceilingHeightM = 3.8,
                floorPlanWidthM = 18.0,
                floorPlanLengthM = 30.0,
                rooms = listOf(
                    RoomSpec("Grand Reception", "الاستقبال الرئيسي", 7.5, 6.0, 4.0, "Jotun Warm Sand", "جوتن رمال دافئة", "#EAE0D0", "Italian Travertine", "مرمر ترافرتين إيطالي"),
                    RoomSpec("Family Hall", "صالة العائلة", 6.5, 5.5, 3.8, "Alabaster Mist", "ألاباستر ضبابي", "#F3F1EC", "Travertine Marble", "مرمر ترافرتين"),
                    RoomSpec("Show Kitchen", "المطبخ المفتوح", 5.2, 4.8, 3.4, "Pure Cotton Satin", "قطني حريري نقي", "#F7F8FA", "Honed Calacatta", "كالاكاتا مطفي"),
                    RoomSpec("Master Suite", "الجناح الرئيسي", 6.2, 5.4, 3.6, "Calm Slate Blue", "أزرق أردوازي هادئ", "#DCE6F2", "European Oak", "باركيه بلوط أوروبي"),
                    RoomSpec("Pool Courtyard", "الحديقة والمسبح", 8.5, 7.0, 4.0, "Natural Limestone", "حجر طبيعي خارجي", "#E6DFD3", "Anti-Slip Granite", "جرانيت مقاوم للانزلاق")
                ),
                tourScenes = listOf(
                    TourScene("living", "Reception Hall 360°", "صالة الاستقبال ٣٦٠°", "pano_living", 0.0),
                    TourScene("kitchen", "Marble Kitchen 360°", "المطبخ العصري ٣٦٠°", "pano_kitchen", 35.0),
                    TourScene("courtyard", "Pool Courtyard 360°", "الحديقة والمسبح ٣٦٠°", "villa_exterior", 120.0)
                ),
                nearbyLandmarks = listOf(
                    NearbyLandmark("MOSQUE", "Umm Al-Tubul Grand Mosque", "جامع أم الطبول", 650, 8, 33.3075, 44.3315),
                    NearbyLandmark("SCHOOL", "Al-Mansour International School", "مدرسة المنصور الدولية", 420, 5, 33.3210, 44.3455),
                    NearbyLandmark("HOSPITAL", "Al-Yarmouk Teaching Hospital", "مستشفى اليرموك التعليمي", 1100, 13, 33.3048, 44.3468),
                    NearbyLandmark("RESTAURANT", "Samad Restaurant Al-Mansour", "مطعم صمد - المنصور", 380, 5, 33.3172, 44.3478),
                    NearbyLandmark("STREET", "Al-Amirat Street", "شارع الأميرات", 210, 3, 33.3168, 44.3405)
                )
            ),
            PropertyListing(
                ownerId = uid,
                ownerDisplayName = "Diyar Signature Baghdad",
                titleEn = "Al-Jadriya Tigris View Penthouse",
                titleAr = "بنتهاوس الجادرية بإطلالة نهر دجلة",
                descriptionEn = "Elevated corner duplex penthouse in Al-Jadriya overlooking the Tigris River bend, equipped with acoustic glazing, private elevator access, smart climate zoning, and certified blueprint specifications.",
                descriptionAr = "بنتهاوس دوبلكس فاخر في الجادرية بإطلالة مباشرة على نهر دجلة، يتميز بعزل صوتي وحراري ومصعد خاص ونظام تحكم ذكي بالتبريد مع مخطط هندسي متكامل وأكواد الألوان.",
                listingType = "SALE",
                propertyType = "PENTHOUSE",
                status = "ACTIVE",
                verificationStatus = "VERIFIED",
                verified = true,
                deedReference = "TAPU-****-3092",
                price = 620_000_000.0,
                currency = "IQD",
                areaSqm = 310.0,
                bedrooms = 4,
                bathrooms = 4,
                yearBuilt = 2024,
                amenities = listOf("ELEVATOR", "SMART_HOME", "PARKING", "GYM"),
                cityEn = "Baghdad",
                cityAr = "بغداد",
                neighborhoodEn = "Al-Jadriya",
                neighborhoodAr = "الجادرية",
                latitude = 33.2932,
                longitude = 44.3845,
                hasVirtualTour = true,
                hasFloorPlan = true,
                heroPresetKey = "penthouse_exterior",
                galleryImages = listOf("preset:penthouse_exterior", "preset:pano_living", "preset:pano_kitchen"),
                ceilingHeightM = 3.4,
                floorPlanWidthM = 16.0,
                floorPlanLengthM = 19.5,
                rooms = listOf(
                    RoomSpec("River Lounge", "صالة إطلالة دجلة", 7.2, 5.5, 3.6, "Nordic Pearl", "لؤلؤي نورديك", "#F2F4F7", "White Oak Chevron", "باركيه شيفرون أبيض"),
                    RoomSpec("Designer Kitchen", "المطبخ المصمم", 4.8, 4.0, 3.4, "Warm Greige", "جريج دافئ", "#E5E2DC", "Porcelain Slab", "ألواح بورسلان"),
                    RoomSpec("Master Bedroom", "غرفة النوم الرئيسية", 5.5, 4.6, 3.4, "Midnight Fog", "ضباب منتصف الليل", "#CFD9E6", "Plush Wool & Oak", "خشب بلوط فاخر"),
                    RoomSpec("Sunset Terrace", "التراس البانورامي", 8.0, 4.2, 3.6, "Weather Shield Sand", "طلاء خارجي رملي", "#DDD6C8", "Decking Teak", "خشب تيك معالج")
                ),
                tourScenes = listOf(
                    TourScene("terrace", "Tigris Terrace 360°", "التراس البانورامي ٣٦٠°", "penthouse_exterior", 15.0),
                    TourScene("lounge", "River Lounge 360°", "صالة الإطلالة ٣٦٠°", "pano_living", 45.0),
                    TourScene("kitchen", "Kitchen Island 360°", "جزيرة المطبخ ٣٦٠°", "pano_kitchen", 90.0)
                ),
                nearbyLandmarks = listOf(
                    NearbyLandmark("MOSQUE", "Al-Jadriya Grand Mosque", "جامع الجادرية الكبير", 340, 4, 33.2948, 44.3868),
                    NearbyLandmark("HOSPITAL", "Ibn Al-Bitar Cardiac Hospital", "مستشفى ابن البيطار", 920, 11, 33.2975, 44.3792),
                    NearbyLandmark("SCHOOL", "University of Baghdad Campus", "جامعة بغداد - الجادرية", 540, 7, 33.2795, 44.3812),
                    NearbyLandmark("RESTAURANT", "Jadriya Corniche Restaurants", "مطاعم كورنيش الجادرية", 410, 5, 33.2902, 44.3820),
                    NearbyLandmark("STREET", "Abu Nuwas Corniche Street", "شارع أبو نؤاس", 490, 6, 33.3065, 44.3950)
                )
            ),
            PropertyListing(
                ownerId = uid,
                ownerDisplayName = "Diyar Verified Realty Iraq",
                titleEn = "Karrada Garden Residence (Annual Rent)",
                titleAr = "شقة الكرادة الحديثة (إيجار سنوي)",
                descriptionEn = "Sunlit 3-bedroom residence in Inner Karrada featuring private terrace garden, oak kitchen island, elevator, backup power, and walkable proximity to schools, hospitals, and Karrada restaurants.",
                descriptionAr = "شقة حديثة من ٣ غرف نوم في الكرادة داخل بحديقة شرفة خاصة ومطبخ خشب البلوط ومصعد وتيار كهربائي مستمر، قرب المدارس والمستشفيات والمطاعم.",
                listingType = "RENT",
                propertyType = "APARTMENT",
                status = "ACTIVE",
                verificationStatus = "VERIFIED",
                verified = true,
                deedReference = "TAPU-****-5514",
                price = 24_000_000.0,
                currency = "IQD",
                areaSqm = 215.0,
                bedrooms = 3,
                bathrooms = 3,
                yearBuilt = 2023,
                amenities = listOf("GARDEN", "ELEVATOR", "PARKING"),
                cityEn = "Baghdad",
                cityAr = "بغداد",
                neighborhoodEn = "Al-Karrada",
                neighborhoodAr = "الكرادة",
                latitude = 33.3078,
                longitude = 44.4150,
                hasVirtualTour = true,
                hasFloorPlan = true,
                heroPresetKey = "pano_living",
                galleryImages = listOf("preset:pano_living", "preset:pano_kitchen", "preset:services_hero"),
                ceilingHeightM = 3.2,
                floorPlanWidthM = 13.0,
                floorPlanLengthM = 16.5,
                rooms = listOf(
                    RoomSpec("Living & Dining", "المعيشة والطعام", 6.2, 5.2, 3.2, "Linen Ivory", "عاجي كتاني", "#F5F0E6", "Crema Marfil", "مرمر كريما مارفيل"),
                    RoomSpec("Open Kitchen", "المطبخ العصري", 4.5, 3.8, 3.2, "Soft Chalk", "طباشيري ناعم", "#F0EFEA", "Natural Oak", "باركيه بلوط طبيعي"),
                    RoomSpec("Master Suite", "غرفة النوم الرئيسية", 5.0, 4.4, 3.2, "Harbor Blue Tint", "أزرق مائي هادئ", "#D7E3F0", "Natural Oak", "باركيه بلوط")
                ),
                tourScenes = listOf(
                    TourScene("living", "Living & Dining 360°", "المعيشة والطعام ٣٦٠°", "pano_living", 0.0),
                    TourScene("kitchen", "Oak Kitchen 360°", "المطبخ العصري ٣٦٠°", "pano_kitchen", 60.0)
                ),
                nearbyLandmarks = listOf(
                    NearbyLandmark("MOSQUE", "Al-Rahman Mosque Karrada", "جامع الرحمن - الكرادة", 240, 3, 33.3095, 44.4165),
                    NearbyLandmark("SCHOOL", "Al-Mutamayizeen High School", "ثانوية المتميزين", 510, 6, 33.3055, 44.4192),
                    NearbyLandmark("HOSPITAL", "Sheikh Zayed Hospital", "مستشفى الشيخ زايد", 820, 10, 33.3142, 44.4195),
                    NearbyLandmark("RESTAURANT", "Zarzour Karrada Restaurant", "مطعم زرزور الكرادة", 290, 4, 33.3065, 44.4125),
                    NearbyLandmark("STREET", "Karrada Inside Street", "شارع الكرادة داخل", 150, 2, 33.3082, 44.4140)
                )
            ),
            PropertyListing(
                ownerId = uid,
                ownerDisplayName = "Diyar Zayouna Partners",
                titleEn = "Zayouna Minimalist Townhouse",
                titleAr = "تاون هاوس زيونة بتصميم مينيمالي",
                descriptionEn = "Newly staged contemporary townhouse in Zayouna near Al-Rubaie Street with double-glazed skylights, certified structural audit, full interior paint palette documentation, and smart home automation.",
                descriptionAr = "تاون هاوس حديث في زيونة قرب شارع الربيعي بتجميل عقاري متكامل وإضاءة سماوية مزدوجة العزل، مع فحص إنشائي وتوثيق كامل لألوان الطلاء ونظام منزل ذكي.",
                listingType = "SALE",
                propertyType = "TOWNHOUSE",
                status = "ACTIVE",
                verificationStatus = "VERIFIED",
                verified = true,
                deedReference = "TAPU-****-7730",
                price = 485_000_000.0,
                currency = "IQD",
                areaSqm = 295.0,
                bedrooms = 4,
                bathrooms = 4,
                yearBuilt = 2022,
                amenities = listOf("GARDEN", "SMART_HOME", "PARKING", "MAID_ROOM"),
                cityEn = "Baghdad",
                cityAr = "بغداد",
                neighborhoodEn = "Zayouna",
                neighborhoodAr = "زيونة",
                latitude = 33.3376,
                longitude = 44.4442,
                hasVirtualTour = true,
                hasFloorPlan = true,
                heroPresetKey = "services_hero",
                galleryImages = listOf("preset:services_hero", "preset:pano_living", "preset:pano_kitchen"),
                ceilingHeightM = 3.5,
                floorPlanWidthM = 12.0,
                floorPlanLengthM = 24.5,
                rooms = listOf(
                    RoomSpec("Staged Reception", "الاستقبال المجهز", 6.0, 5.2, 3.5, "Cotton Cream", "كريمي قطني", "#F6F3EC", "White Terrazzo", "تيرازو أبيض"),
                    RoomSpec("Kitchen & Breakfast", "المطبخ والإفطار", 4.6, 4.2, 3.5, "Warm Sandstone", "حجر رملي دافئ", "#EAE3D5", "Matte Porcelain", "بورسلان مطفي"),
                    RoomSpec("Bedroom 1", "غرفة نوم ١", 4.5, 4.0, 3.3, "Serene Sky", "سماوي هادئ", "#DCE8F5", "Oak Parquet", "باركيه بلوط")
                ),
                tourScenes = listOf(
                    TourScene("reception", "Reception 360°", "صالة الاستقبال ٣٦٠°", "services_hero", 0.0),
                    TourScene("kitchen", "Kitchen 360°", "المطبخ ٣٦٠°", "pano_kitchen", 45.0)
                ),
                nearbyLandmarks = listOf(
                    NearbyLandmark("MOSQUE", "Al-Hasnain Mosque Zayouna", "جامع الحسنين - زيونة", 220, 3, 33.3390, 44.4458),
                    NearbyLandmark("SCHOOL", "Zayouna Model High School", "ثانوية زيونة النموذجية", 460, 6, 33.3352, 44.4410),
                    NearbyLandmark("HOSPITAL", "Al-Kindi Teaching Hospital", "مستشفى الكندي التعليمي", 1300, 15, 33.3470, 44.4275),
                    NearbyLandmark("STREET", "Palestine Street", "شارع فلسطين", 690, 8, 33.3502, 44.4060),
                    NearbyLandmark("RESTAURANT", "Al-Rubaie Street Restaurants", "مطاعم شارع الربيعي", 340, 4, 33.3340, 44.4422)
                )
            ),
            PropertyListing(
                ownerId = uid,
                ownerDisplayName = "Diyar Commercial Iraq",
                titleEn = "14 Ramadan Commercial Tower & Plaza",
                titleAr = "مجمع ١٤ رمضان التجاري والمكتبي - المنصور",
                descriptionEn = "Prime corner commercial building on 14 Ramadan Street in Al-Mansour featuring ground showroom glazing, 5 corporate office suites, private underground parking, and verified Iraqi commercial Tapu.",
                descriptionAr = "مبنى تجاري استثماري على شارع ١٤ رمضان الرئيسي في المنصور يضم واجهة معارض أرضية و٥ مكاتب شركات مجهزة وموقف سيارات خاص وسند طابو تجاري موثق.",
                listingType = "SALE",
                propertyType = "COMMERCIAL",
                status = "ACTIVE",
                verificationStatus = "VERIFIED",
                verified = true,
                deedReference = "TAPU-COM-9012",
                price = 1_250_000_000.0,
                currency = "IQD",
                areaSqm = 680.0,
                bedrooms = 6,
                bathrooms = 5,
                yearBuilt = 2025,
                amenities = listOf("ELEVATOR", "SMART_HOME", "PARKING"),
                cityEn = "Baghdad",
                cityAr = "بغداد",
                neighborhoodEn = "Al-Mansour",
                neighborhoodAr = "المنصور",
                latitude = 33.3218,
                longitude = 44.3490,
                hasVirtualTour = true,
                hasFloorPlan = true,
                heroPresetKey = "penthouse_exterior",
                galleryImages = listOf("preset:penthouse_exterior", "preset:pano_living", "preset:services_hero"),
                ceilingHeightM = 4.2,
                floorPlanWidthM = 20.0,
                floorPlanLengthM = 34.0,
                rooms = listOf(
                    RoomSpec("Main Commercial Showroom", "صالة العرض التجارية", 12.0, 9.0, 4.5, "Architectural Graphite", "جرافيتي معماري", "#374151", "Polished Granite", "جرانيت مصقول"),
                    RoomSpec("Executive Boardroom", "قاعة الاجتماعات التنفيذية", 7.0, 5.5, 3.6, "Alabaster Mist", "ألاباستر ضبابي", "#F3F1EC", "Porcelain Tile", "بورسلان إيطالي")
                ),
                tourScenes = listOf(
                    TourScene("showroom", "Showroom Lobby 360°", "صالة العرض ٣٦٠°", "pano_living", 0.0),
                    TourScene("facade", "14 Ramadan Facade 360°", "واجهة ١٤ رمضان ٣٦٠°", "penthouse_exterior", 45.0)
                ),
                nearbyLandmarks = listOf(
                    NearbyLandmark("STREET", "14 Ramadan Street", "شارع ١٤ رمضان", 40, 1, 33.3228, 44.3489),
                    NearbyLandmark("RESTAURANT", "Al-Mansour Mall", "المنصور مول", 450, 5, 33.3198, 44.3438),
                    NearbyLandmark("HOSPITAL", "Al-Yarmouk Teaching Hospital", "مستشفى اليرموك التعليمي", 1600, 18, 33.3048, 44.3468)
                )
            ),
            PropertyListing(
                ownerId = uid,
                ownerDisplayName = "Karrada Business Assets",
                titleEn = "Arasat Al-Hindiya Flagship Showroom (Rent)",
                titleAr = "معرض ومكاتب عرصات الهندية التجاري (إيجار)",
                descriptionEn = "Double-height commercial showroom & executive offices in Arasat Al-Hindiya, Karrada, ready for banks, clinics, or flagship retail brands with backup generator & valet parking.",
                descriptionAr = "معرض تجاري ومكاتب إدارية فاخرة في عرصات الهندية بالكرادة بارتفاع سقف مزدوج، مناسب للمصارف والعيادات والشركات مع موقف سيارات ومولدة خاصة.",
                listingType = "RENT",
                propertyType = "COMMERCIAL",
                status = "ACTIVE",
                verificationStatus = "VERIFIED",
                verified = true,
                deedReference = "TAPU-COM-4428",
                price = 48_000_000.0,
                currency = "IQD",
                areaSqm = 360.0,
                bedrooms = 4,
                bathrooms = 3,
                yearBuilt = 2024,
                amenities = listOf("ELEVATOR", "PARKING", "SMART_HOME"),
                cityEn = "Baghdad",
                cityAr = "بغداد",
                neighborhoodEn = "Al-Karrada",
                neighborhoodAr = "الكرادة",
                latitude = 33.3016,
                longitude = 44.4215,
                hasVirtualTour = true,
                hasFloorPlan = true,
                heroPresetKey = "services_hero",
                galleryImages = listOf("preset:services_hero", "preset:pano_living"),
                ceilingHeightM = 4.0,
                floorPlanWidthM = 15.0,
                floorPlanLengthM = 24.0,
                rooms = listOf(
                    RoomSpec("Retail & Reception Hall", "صالة الاستقبال والمعرض", 10.0, 8.0, 4.0, "Pure Cotton Satin", "قطني نقي", "#F7F8FA", "Travertine Marble", "مرمر ترافرتين"),
                    RoomSpec("Admin Offices", "المكاتب الإدارية", 6.5, 5.0, 3.4, "Calm Slate Blue", "أزرق هادئ", "#DCE6F2", "Porcelain Tile", "بورسلان حديث")
                ),
                tourScenes = listOf(
                    TourScene("retail", "Commercial Hall 360°", "صالة المعرض ٣٦٠°", "services_hero", 0.0)
                ),
                nearbyLandmarks = listOf(
                    NearbyLandmark("RESTAURANT", "Zarzour Karrada Restaurant", "مطعم زرزور الكرادة", 600, 7, 33.3065, 44.4125),
                    NearbyLandmark("STREET", "Arasat Al-Hindiya Street", "شارع عرصات الهندية", 60, 1, 33.3016, 44.4215)
                )
            )
        )
    }
}
