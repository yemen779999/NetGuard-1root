package com.example.data

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "saved_macs")
data class SavedMac(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val macAddress: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "mac_history")
data class MacHistory(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val macAddress: String,
    val oldMacAddress: String,
    val timestamp: Long
)

@Entity(tableName = "config_change_logs")
data class ConfigChangeLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val changeType: String, // "MAC" or "IP"
    val interfaceName: String,
    val oldValue: String,
    val newValue: String,
    val details: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "network_profiles")
data class NetworkProfile(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val profileName: String,
    val interfaceName: String,
    val customMac: String,
    val customIp: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "scanned_devices")
data class ScannedDeviceRecord(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val ip: String,
    val mac: String,
    val deviceName: String,
    val deviceType: String,
    val isBlocked: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "mac_manager_logs")
data class MacManagerLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val action: String, // "CHANGE_MAC", "DISABLE_RANDOM_MAC", "ENABLE_RANDOM_MAC"
    val interfaceUsed: String, // e.g. "wlan0"
    val targetMac: String, // e.g. "AA:BB:CC:DD:EE:FF" or "N/A"
    val isSuccess: Boolean,
    val message: String,
    val timestamp: Long = System.currentTimeMillis()
)

// 1. جدول الشبكات
@Entity(tableName = "networks")
data class NetworkEntity(
    @PrimaryKey(autoGenerate = true) 
    val networkId: Long = 0,
    val ssid: String,
    val bssid: String, // المعرف الفريد لشبكة الواي فاي
    val scanTimestamp: Long = System.currentTimeMillis()
)

// 2. جدول الأجهزة (مرتبط بالشبكة)
@Entity(
    tableName = "devices",
    foreignKeys = [
        ForeignKey(
            entity = NetworkEntity::class,
            parentColumns = ["networkId"],
            childColumns = ["networkOwnerId"],
            onDelete = ForeignKey.CASCADE // حذف الأجهزة تلقائياً عند حذف الشبكة
        )
    ],
    indices = [Index(value = ["networkOwnerId"])]
)
data class DeviceEntity(
    @PrimaryKey(autoGenerate = true) 
    val deviceId: Long = 0,
    val networkOwnerId: Long = 0, // المفتاح الأجنبي المرتبط بالشبكة
    val ipAddress: String,
    val macAddress: String,
    val customName: String = "",
    val isBlocked: Boolean = false
)

// 3. كلاس العلاقة (شبكة واحدة مع أجهزتها الخاصة فقط)
data class NetworkWithDevices(
    @Embedded val network: NetworkEntity,
    @Relation(
        parentColumn = "networkId",
        entityColumn = "networkOwnerId"
    )
    val devices: List<DeviceEntity>
)

@Dao
interface SavedMacDao {
    @Query("SELECT * FROM saved_macs ORDER BY timestamp DESC")
    fun getAll(): Flow<List<SavedMac>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(savedMac: SavedMac)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(macs: List<SavedMac>)

    @Delete
    suspend fun delete(savedMac: SavedMac)

    @Query("DELETE FROM saved_macs WHERE id = :id")
    suspend fun deleteById(id: Int)

    @Query("DELETE FROM saved_macs")
    suspend fun clearAll()
}

@Dao
interface MacHistoryDao {
    @Query("SELECT * FROM mac_history ORDER BY timestamp DESC")
    fun getAll(): Flow<List<MacHistory>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(macHistory: MacHistory)

    @Query("DELETE FROM mac_history WHERE id = :id")
    suspend fun deleteById(id: Int)

    @Query("DELETE FROM mac_history")
    suspend fun clearAll()
}

@Dao
interface ConfigChangeLogDao {
    @Query("SELECT * FROM config_change_logs ORDER BY timestamp DESC")
    fun getAll(): Flow<List<ConfigChangeLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(log: ConfigChangeLog)

    @Query("DELETE FROM config_change_logs WHERE id = :id")
    suspend fun deleteById(id: Int)

    @Query("DELETE FROM config_change_logs")
    suspend fun clearAll()
}

@Dao
interface NetworkProfileDao {
    @Query("SELECT * FROM network_profiles ORDER BY timestamp DESC")
    fun getAll(): Flow<List<NetworkProfile>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(profile: NetworkProfile)

    @Query("DELETE FROM network_profiles WHERE id = :id")
    suspend fun deleteById(id: Int)

    @Query("DELETE FROM network_profiles")
    suspend fun clearAll()
}

@Dao
interface ScannedDeviceDao {
    @Query("SELECT * FROM scanned_devices ORDER BY timestamp DESC")
    fun getAll(): Flow<List<ScannedDeviceRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(device: ScannedDeviceRecord)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(devices: List<ScannedDeviceRecord>)

    @Query("DELETE FROM scanned_devices WHERE id = :id")
    suspend fun deleteById(id: Int)

    @Query("DELETE FROM scanned_devices")
    suspend fun clearAll()
}

@Dao
interface MacManagerLogDao {
    @Query("SELECT * FROM mac_manager_logs ORDER BY timestamp DESC")
    fun getAll(): Flow<List<MacManagerLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(log: MacManagerLog)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(logs: List<MacManagerLog>)

    @Query("DELETE FROM mac_manager_logs WHERE id = :id")
    suspend fun deleteById(id: Int)

    @Query("DELETE FROM mac_manager_logs")
    suspend fun clearAll()
}

@Dao
interface NetworkDao {
    // حفظ أو تحديث بيانات الشبكة
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNetwork(network: NetworkEntity): Long

    // حفظ الأجهزة التابعة للشبكة
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDevices(devices: List<DeviceEntity>)

    // إجراء عملية حفظ متكاملة للشبكة وأجهزتها
    @Transaction
    suspend fun insertNetworkWithDevices(network: NetworkEntity, devices: List<DeviceEntity>): Long {
        val networkId = insertNetwork(network)
        val devicesWithOwner = devices.map { it.copy(networkOwnerId = networkId) }
        insertDevices(devicesWithOwner)
        return networkId
    }

    // جلب شبكة محددة مع أجهزتها الخاصة فقط عبر معرف الشبكة
    @Transaction
    @Query("SELECT * FROM networks WHERE networkId = :networkId")
    fun getNetworkWithDevices(networkId: Long): Flow<NetworkWithDevices?>

    // جلب شبكة محددة عبر الـ BSSID الخاص بالواي فاي الحالي
    @Transaction
    @Query("SELECT * FROM networks WHERE bssid = :bssid ORDER BY scanTimestamp DESC LIMIT 1")
    suspend fun getNetworkByBssid(bssid: String): NetworkWithDevices?

    // جلب جميع الشبكات المحفوظة (لعرض قائمة سجل الشبكات)
    @Query("SELECT * FROM networks ORDER BY scanTimestamp DESC")
    fun getAllSavedNetworks(): Flow<List<NetworkEntity>>

    // جلب جميع الشبكات المحفوظة مع أجهزتها
    @Transaction
    @Query("SELECT * FROM networks ORDER BY scanTimestamp DESC")
    fun getAllNetworksWithDevices(): Flow<List<NetworkWithDevices>>

    @Query("DELETE FROM networks WHERE networkId = :networkId")
    suspend fun deleteNetworkById(networkId: Long)

    @Query("DELETE FROM networks")
    suspend fun clearAllNetworks()
}

@Database(
    entities = [
        SavedMac::class,
        MacHistory::class,
        ConfigChangeLog::class,
        NetworkProfile::class,
        ScannedDeviceRecord::class,
        MacManagerLog::class,
        NetworkEntity::class,
        DeviceEntity::class
    ],
    version = 6,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun savedMacDao(): SavedMacDao
    abstract fun macHistoryDao(): MacHistoryDao
    abstract fun configChangeLogDao(): ConfigChangeLogDao
    abstract fun networkProfileDao(): NetworkProfileDao
    abstract fun scannedDeviceDao(): ScannedDeviceDao
    abstract fun macManagerLogDao(): MacManagerLogDao
    abstract fun networkDao(): NetworkDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "netguard_root_db"
                )
                .fallbackToDestructiveMigration(dropAllTables = true)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

class NetworkRepository(context: Context) {
    private val db = AppDatabase.getDatabase(context)
    private val savedMacDao = db.savedMacDao()
    private val macHistoryDao = db.macHistoryDao()
    private val configChangeLogDao = db.configChangeLogDao()
    private val networkProfileDao = db.networkProfileDao()
    private val scannedDeviceDao = db.scannedDeviceDao()
    private val macManagerLogDao = db.macManagerLogDao()
    private val networkDao = db.networkDao()

    val savedMacs: Flow<List<SavedMac>> = savedMacDao.getAll()
    val macHistoryList: Flow<List<MacHistory>> = macHistoryDao.getAll()
    val configChangeLogs: Flow<List<ConfigChangeLog>> = configChangeLogDao.getAll()
    val networkProfiles: Flow<List<NetworkProfile>> = networkProfileDao.getAll()
    val scannedDevicesHistory: Flow<List<ScannedDeviceRecord>> = scannedDeviceDao.getAll()
    val macManagerLogs: Flow<List<MacManagerLog>> = macManagerLogDao.getAll()
    val allNetworksWithDevices: Flow<List<NetworkWithDevices>> = networkDao.getAllNetworksWithDevices()
    val allSavedNetworks: Flow<List<NetworkEntity>> = networkDao.getAllSavedNetworks()

    suspend fun addMacManagerLog(
        action: String,
        interfaceUsed: String,
        targetMac: String,
        isSuccess: Boolean,
        message: String
    ) {
        macManagerLogDao.insert(
            MacManagerLog(
                action = action,
                interfaceUsed = interfaceUsed,
                targetMac = targetMac,
                isSuccess = isSuccess,
                message = message,
                timestamp = System.currentTimeMillis()
            )
        )
    }

    suspend fun deleteMacManagerLog(id: Int) {
        macManagerLogDao.deleteById(id)
    }

    suspend fun clearMacManagerLogs() {
        macManagerLogDao.clearAll()
    }

    fun getNetworkWithDevices(networkId: Long): Flow<NetworkWithDevices?> = networkDao.getNetworkWithDevices(networkId)

    suspend fun getNetworkByBssid(bssid: String): NetworkWithDevices? = networkDao.getNetworkByBssid(bssid)

    suspend fun saveNetworkScan(ssid: String, bssid: String, devices: List<DeviceEntity>): Long {
        val network = NetworkEntity(ssid = ssid, bssid = bssid, scanTimestamp = System.currentTimeMillis())
        return networkDao.insertNetworkWithDevices(network, devices)
    }

    suspend fun deleteNetworkScan(networkId: Long) {
        networkDao.deleteNetworkById(networkId)
    }

    suspend fun clearAllNetworkScans() {
        networkDao.clearAllNetworks()
    }

    suspend fun saveScanResults(devices: List<com.example.network.NetworkDevice>) {
        val records = devices.map { device ->
            ScannedDeviceRecord(
                ip = device.ip,
                mac = device.mac,
                deviceName = device.name,
                deviceType = device.type.name,
                isBlocked = device.isBlocked,
                timestamp = System.currentTimeMillis()
            )
        }
        scannedDeviceDao.insertAll(records)
    }

    suspend fun deleteScannedDeviceRecord(id: Int) {
        scannedDeviceDao.deleteById(id)
    }

    suspend fun clearScannedDeviceHistory() {
        scannedDeviceDao.clearAll()
    }

    suspend fun insertNetworkProfile(profileName: String, interfaceName: String, customMac: String, customIp: String = "") {
        networkProfileDao.insert(
            NetworkProfile(
                profileName = profileName,
                interfaceName = interfaceName,
                customMac = customMac,
                customIp = customIp
            )
        )
    }

    suspend fun deleteNetworkProfile(id: Int) {
        networkProfileDao.deleteById(id)
    }

    suspend fun clearNetworkProfiles() {
        networkProfileDao.clearAll()
    }

    suspend fun insertSavedMac(title: String, mac: String) {
        savedMacDao.insert(SavedMac(title = title, macAddress = mac))
    }

    suspend fun deleteSavedMac(savedMac: SavedMac) {
        savedMacDao.delete(savedMac)
    }

    suspend fun deleteSavedMacById(id: Int) {
        savedMacDao.deleteById(id)
    }

    suspend fun addHistoryEntry(mac: String, oldMac: String) {
        macHistoryDao.insert(MacHistory(macAddress = mac, oldMacAddress = oldMac, timestamp = System.currentTimeMillis()))
    }

    suspend fun deleteHistoryEntry(id: Int) {
        macHistoryDao.deleteById(id)
    }

    suspend fun clearHistory() {
        macHistoryDao.clearAll()
    }

    suspend fun addConfigChangeLog(changeType: String, interfaceName: String, oldValue: String, newValue: String, details: String) {
        configChangeLogDao.insert(
            ConfigChangeLog(
                changeType = changeType,
                interfaceName = interfaceName,
                oldValue = oldValue,
                newValue = newValue,
                details = details
            )
        )
    }

    suspend fun deleteConfigChangeLog(id: Int) {
        configChangeLogDao.deleteById(id)
    }

    suspend fun clearConfigChangeLogs() {
        configChangeLogDao.clearAll()
    }

    suspend fun restoreSavedMacs(macs: List<SavedMac>, clearExisting: Boolean = false) {
        if (clearExisting) {
            savedMacDao.clearAll()
        }
        savedMacDao.insertAll(macs)
    }

    suspend fun restoreFullBackup(
        savedMacs: List<SavedMac>,
        historyList: List<MacHistory>,
        logsList: List<ConfigChangeLog>,
        profilesList: List<NetworkProfile>,
        clearExisting: Boolean = true
    ) {
        if (clearExisting) {
            savedMacDao.clearAll()
            macHistoryDao.clearAll()
            configChangeLogDao.clearAll()
            networkProfileDao.clearAll()
        }
        savedMacDao.insertAll(savedMacs)
        for (h in historyList) { macHistoryDao.insert(h) }
        for (l in logsList) { configChangeLogDao.insert(l) }
        for (p in profilesList) { networkProfileDao.insert(p) }
    }
}
