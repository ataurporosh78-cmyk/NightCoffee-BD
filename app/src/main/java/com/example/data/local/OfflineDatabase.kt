package com.example.data.local

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.model.CoffeeEventDoc
import com.example.model.CouponDoc
import com.example.model.DigitalMenuItemDoc
import com.google.firebase.Timestamp
import java.util.Date
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "cached_menu_items")
data class CachedMenuItemEntity(
    @PrimaryKey val id: String,
    val shopId: String,
    val category: String,
    val name: String,
    val description: String,
    val imageUrl: String,
    val priceBdt: Long,
    val available: Boolean,
    val special: Boolean,
    val updatedBy: String,
    val cachedAtMillis: Long
) {
    fun toDoc(): DigitalMenuItemDoc = DigitalMenuItemDoc(
        id = id,
        shopId = shopId,
        category = category,
        name = name,
        description = description,
        imageUrl = imageUrl,
        priceBdt = priceBdt,
        available = available,
        special = special,
        visibility = "public",
        updatedBy = updatedBy
    )

    companion object {
        fun fromDoc(doc: DigitalMenuItemDoc, cachedAtMillis: Long): CachedMenuItemEntity =
            CachedMenuItemEntity(
                id = doc.id,
                shopId = doc.shopId,
                category = doc.category,
                name = doc.name,
                description = doc.description,
                imageUrl = doc.imageUrl,
                priceBdt = doc.priceBdt,
                available = doc.available,
                special = doc.special,
                updatedBy = doc.updatedBy,
                cachedAtMillis = cachedAtMillis
            )
    }
}

@Entity(tableName = "cached_coupons")
data class CachedCouponEntity(
    @PrimaryKey val id: String,
    val shopId: String,
    val shopName: String,
    val code: String,
    val title: String,
    val description: String,
    val discountType: String,
    val discountValue: Long,
    val minSpendBdt: Long,
    val maxUsageLimit: Long,
    val redeemedCount: Long,
    val terms: String,
    val expiresAtMillis: Long?,
    val active: Boolean,
    val cachedAtMillis: Long
) {
    fun toDoc(): CouponDoc = CouponDoc(
        id = id,
        shopId = shopId,
        shopName = shopName,
        code = code,
        title = title,
        description = description,
        discountType = discountType,
        discountValue = discountValue,
        minSpendBdt = minSpendBdt,
        maxUsageLimit = maxUsageLimit,
        redeemedCount = redeemedCount,
        terms = terms,
        expiresAt = expiresAtMillis?.let { Timestamp(Date(it)) },
        active = active,
        visibility = "public"
    )

    companion object {
        fun fromDoc(doc: CouponDoc, cachedAtMillis: Long): CachedCouponEntity =
            CachedCouponEntity(
                id = doc.id,
                shopId = doc.shopId,
                shopName = doc.shopName,
                code = doc.code,
                title = doc.title,
                description = doc.description,
                discountType = doc.discountType,
                discountValue = doc.discountValue,
                minSpendBdt = doc.minSpendBdt,
                maxUsageLimit = doc.maxUsageLimit,
                redeemedCount = doc.redeemedCount,
                terms = doc.terms,
                expiresAtMillis = doc.expiresAt?.toDate()?.time,
                active = doc.active,
                cachedAtMillis = cachedAtMillis
            )
    }
}

@Entity(tableName = "cached_events")
data class CachedEventEntity(
    @PrimaryKey val id: String,
    val title: String,
    val category: String,
    val description: String,
    val imageUrl: String,
    val shopId: String?,
    val venueName: String,
    val area: String,
    val eventDateDhaka: String,
    val startTime: String,
    val endTime: String,
    val organizer: String,
    val registrationInfo: String,
    val status: String,
    val cachedAtMillis: Long
) {
    fun toDoc(): CoffeeEventDoc = CoffeeEventDoc(
        id = id,
        title = title,
        category = category,
        description = description,
        imageUrl = imageUrl,
        shopId = shopId,
        venueName = venueName,
        area = area,
        eventDateDhaka = eventDateDhaka,
        startTime = startTime,
        endTime = endTime,
        organizer = organizer,
        registrationInfo = registrationInfo,
        status = status,
        visibility = "public"
    )

    companion object {
        fun fromDoc(doc: CoffeeEventDoc, cachedAtMillis: Long): CachedEventEntity =
            CachedEventEntity(
                id = doc.id,
                title = doc.title,
                category = doc.category,
                description = doc.description,
                imageUrl = doc.imageUrl,
                shopId = doc.shopId,
                venueName = doc.venueName,
                area = doc.area,
                eventDateDhaka = doc.eventDateDhaka,
                startTime = doc.startTime,
                endTime = doc.endTime,
                organizer = doc.organizer,
                registrationInfo = doc.registrationInfo,
                status = doc.status,
                cachedAtMillis = cachedAtMillis
            )
    }
}

@Entity(tableName = "pending_sync_actions")
data class PendingSyncActionEntity(
    @PrimaryKey val actionId: String,
    val actionType: String, // STATUS_REPORT, CROWD_REPORT, SUPPORT_TICKET, USER_PREFERENCE
    val targetCollection: String,
    val targetDocId: String,
    val payloadJson: String,
    val status: String = SYNC_STATUS_PENDING, // PENDING, FAILED_PERM
    val retryCount: Int = 0,
    val createdAtMillis: Long = System.currentTimeMillis()
) {
    companion object {
        const val TYPE_STATUS_REPORT = "STATUS_REPORT"
        const val TYPE_CROWD_REPORT = "CROWD_REPORT"
        const val TYPE_SUPPORT_TICKET = "SUPPORT_TICKET"
        const val TYPE_USER_PREFERENCE = "USER_PREFERENCE"

        const val SYNC_STATUS_PENDING = "PENDING"
        const val SYNC_STATUS_FAILED_PERM = "FAILED_PERM"
    }
}

@Dao
interface CachedMenuDao {
    @Query("SELECT * FROM cached_menu_items WHERE shopId = :shopId ORDER BY category ASC, name ASC")
    fun observeMenuForShop(shopId: String): Flow<List<CachedMenuItemEntity>>

    @Query("SELECT * FROM cached_menu_items WHERE shopId = :shopId")
    suspend fun getMenuForShopOnce(shopId: String): List<CachedMenuItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<CachedMenuItemEntity>)

    @Query("DELETE FROM cached_menu_items WHERE shopId = :shopId")
    suspend fun deleteForShop(shopId: String)
}

@Dao
interface CachedCouponDao {
    @Query("SELECT * FROM cached_coupons ORDER BY cachedAtMillis DESC")
    fun observeAllCoupons(): Flow<List<CachedCouponEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<CachedCouponEntity>)

    @Query("DELETE FROM cached_coupons")
    suspend fun clearAll()
}

@Dao
interface CachedEventDao {
    @Query("SELECT * FROM cached_events ORDER BY eventDateDhaka ASC, startTime ASC")
    fun observeAllEvents(): Flow<List<CachedEventEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<CachedEventEntity>)

    @Query("DELETE FROM cached_events")
    suspend fun clearAll()
}

@Dao
interface PendingSyncDao {
    @Query("SELECT * FROM pending_sync_actions WHERE status = 'PENDING' ORDER BY createdAtMillis ASC")
    suspend fun getPendingActions(): List<PendingSyncActionEntity>

    @Query("SELECT COUNT(*) FROM pending_sync_actions WHERE status = 'PENDING'")
    fun observePendingCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun enqueue(action: PendingSyncActionEntity)

    @Query("DELETE FROM pending_sync_actions WHERE actionId = :actionId")
    suspend fun deleteById(actionId: String)

    @Query("UPDATE pending_sync_actions SET retryCount = retryCount + 1, status = :status WHERE actionId = :actionId")
    suspend fun markRetryOrFailed(actionId: String, status: String)
}

@Database(
    entities = [
        CachedMenuItemEntity::class,
        CachedCouponEntity::class,
        CachedEventEntity::class,
        PendingSyncActionEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class NightCoffeeDatabase : RoomDatabase() {
    abstract fun cachedMenuDao(): CachedMenuDao
    abstract fun cachedCouponDao(): CachedCouponDao
    abstract fun cachedEventDao(): CachedEventDao
    abstract fun pendingSyncDao(): PendingSyncDao

    companion object {
        @Volatile
        private var instance: NightCoffeeDatabase? = null

        fun getInstance(context: Context): NightCoffeeDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    NightCoffeeDatabase::class.java,
                    "nightcoffee_offline_cache.db"
                )
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                    .also { instance = it }
            }
        }
    }
}
