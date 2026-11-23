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
                    )
                        .filterNot { it.id.startsWith("iq_seed_") || it.id.startsWith("seed_") }
                        .sortedByDescending { it.createdAt?.seconds ?: 0L }
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
                    )
                        .filterNot { it.id.startsWith("iq_seed_") || it.id.startsWith("seed_") }
                        .sortedByDescending { it.createdAt?.seconds ?: 0L }
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
                "estimatedPrice" to request.estimatedPrice.coerceIn(0.0, 500_000_000.0),
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

    suspend fun cleanupDemoSeedProperties(): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        return try {
            val ownedSnapshot = db.collection("properties")
                .whereEqualTo("ownerId", uid)
                .get()
                .await()

            val legacyPrefix = "seed_${uid.take(8)}_"
            val iqSeedPrefix = "iq_seed_${uid.take(8)}_"
            for (doc in ownedSnapshot.documents) {
                if (doc.id.startsWith(legacyPrefix) || doc.id.startsWith(iqSeedPrefix)) {
                    try {
                        doc.reference.delete().await()
                    } catch (_: Exception) {
                    }
                }
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    companion object {
        const val WHATSAPP_SERVICE_NUMBER = "9647740080310"

        val defaultServicePackages: List<ServicePackage> = listOf(
            ServicePackage(
                id = "srv_staging_1",
                category = "STAGING",
                titleEn = "Real Estate Beautification & Restoration",
                titleAr = "التجميل العقاري",
                subtitleEn = "Repairing marks, wall cracks, dampness & humidity damage, and complete interior/exterior house painting.",
                subtitleAr = "إصلاح الآثار والتشققات والرطوبة وصبغ المنزل بالكامل بأعلى جودة.",
                durationEn = "By Agreement",
                durationAr = "حسب الاتفاق",
                priceSar = 0.0,
                rating = 4.9
            ),
            ServicePackage(
                id = "srv_maint_1",
                category = "MAINTENANCE",
                titleEn = "Split AC & Home Appliances Maintenance",
                titleAr = "صيانة السبالت والأجهزة المنزلية",
                subtitleEn = "Specialized technical maintenance and servicing for split AC units and all household appliances.",
                subtitleAr = "صيانة السبالت ومختلف الأجهزة المنزلية بأيدي فنيين مختصين.",
                durationEn = "By Agreement",
                durationAr = "حسب الاتفاق",
                priceSar = 0.0,
                rating = 4.9
            ),
            ServicePackage(
                id = "srv_moving_1",
                category = "MOVING",
                titleEn = "Professional Furniture Moving, Packing & Assembly",
                titleAr = "نقل الأثاث والتغليف والتركيب الاحترافي",
                subtitleEn = "Safe furniture relocation, protective wrapping, carpentry disassembly and reassembly across Baghdad & Iraq.",
                subtitleAr = "نقل الأثاث والتغليف والتركيب الاحترافي بعناية تامة داخل بغداد والمحافظات.",
                durationEn = "By Agreement",
                durationAr = "حسب الاتفاق",
                priceSar = 0.0,
                rating = 4.9
            ),
            ServicePackage(
                id = "srv_marketing_1",
                category = "MARKETING",
                titleEn = "Home Photography & Baghdad Broker Promotion",
                titleAr = "التصوير والترويج للمنزل",
                subtitleEn = "Professional photography and promotion distributed across all real estate offices in Baghdad to maximize sale chances.",
                subtitleAr = "التصوير والترويج للمنزل ونشره بجميع المكاتب العقارية في بغداد مما يزيد فرصة بيعه.",
                durationEn = "By Agreement",
                durationAr = "حسب الاتفاق",
                priceSar = 0.0,
                rating = 5.0
            )
        )
    }
}
