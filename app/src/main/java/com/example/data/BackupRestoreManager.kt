package com.example.data

import org.json.JSONArray
import org.json.JSONObject

data class FullBackupResult(
    val savedMacs: List<SavedMac>,
    val history: List<MacHistory>,
    val logs: List<ConfigChangeLog>,
    val profiles: List<NetworkProfile>,
    val timestamp: Long
)

object BackupRestoreManager {

    /**
     * Exports all saved MAC addresses to a structured JSON string.
     */
    fun exportSavedMacsToJson(savedMacs: List<SavedMac>): String {
        val root = JSONObject()
        root.put("type", "NETGUARD_SAVED_MACS")
        root.put("version", 1)
        root.put("timestamp", System.currentTimeMillis())

        val macsArray = JSONArray()
        for (mac in savedMacs) {
            val item = JSONObject()
            item.put("id", mac.id)
            item.put("title", mac.title)
            item.put("macAddress", mac.macAddress)
            item.put("timestamp", mac.timestamp)
            macsArray.put(item)
        }
        root.put("saved_macs", macsArray)
        return root.toString(4)
    }

    /**
     * Imports saved MAC addresses from a JSON string.
     */
    fun importSavedMacsFromJson(jsonString: String): List<SavedMac> {
        val result = mutableListOf<SavedMac>()
        val root = JSONObject(jsonString)
        
        val array = when {
            root.has("saved_macs") -> root.getJSONArray("saved_macs")
            root.has("items") -> root.getJSONArray("items")
            else -> JSONArray(jsonString)
        }

        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            val title = obj.optString("title", "MAC مستورد")
            val mac = obj.getString("macAddress")
            val time = obj.optLong("timestamp", System.currentTimeMillis())
            result.add(SavedMac(title = title, macAddress = mac, timestamp = time))
        }
        return result
    }

    /**
     * Exports FULL NetGuard Root application data (saved MACs, MAC history, config logs, network profiles) to JSON.
     */
    fun exportFullAppBackupToJson(
        savedMacs: List<SavedMac>,
        historyList: List<MacHistory>,
        logsList: List<ConfigChangeLog>,
        profilesList: List<NetworkProfile>
    ): String {
        val root = JSONObject()
        root.put("type", "NETGUARD_FULL_BACKUP")
        root.put("version", 1)
        root.put("app", "NetGuard Root")
        root.put("timestamp", System.currentTimeMillis())

        // 1. Saved MACs
        val savedMacsArray = JSONArray()
        for (item in savedMacs) {
            val obj = JSONObject()
            obj.put("title", item.title)
            obj.put("macAddress", item.macAddress)
            obj.put("timestamp", item.timestamp)
            savedMacsArray.put(obj)
        }
        root.put("saved_macs", savedMacsArray)

        // 2. MAC History
        val historyArray = JSONArray()
        for (item in historyList) {
            val obj = JSONObject()
            obj.put("macAddress", item.macAddress)
            obj.put("oldMacAddress", item.oldMacAddress)
            obj.put("timestamp", item.timestamp)
            historyArray.put(obj)
        }
        root.put("mac_history", historyArray)

        // 3. Config Change Logs
        val logsArray = JSONArray()
        for (item in logsList) {
            val obj = JSONObject()
            obj.put("changeType", item.changeType)
            obj.put("interfaceName", item.interfaceName)
            obj.put("oldValue", item.oldValue)
            obj.put("newValue", item.newValue)
            obj.put("details", item.details)
            obj.put("timestamp", item.timestamp)
            logsArray.put(obj)
        }
        root.put("config_logs", logsArray)

        // 4. Network Profiles
        val profilesArray = JSONArray()
        for (item in profilesList) {
            val obj = JSONObject()
            obj.put("profileName", item.profileName)
            obj.put("interfaceName", item.interfaceName)
            obj.put("customMac", item.customMac)
            obj.put("customIp", item.customIp)
            obj.put("timestamp", item.timestamp)
            profilesArray.put(obj)
        }
        root.put("network_profiles", profilesArray)

        return root.toString(4)
    }

    /**
     * Imports FULL NetGuard Root application backup data from JSON.
     */
    fun importFullAppBackupFromJson(jsonString: String): FullBackupResult {
        val root = JSONObject(jsonString)
        val timestamp = root.optLong("timestamp", System.currentTimeMillis())

        val savedMacs = mutableListOf<SavedMac>()
        if (root.has("saved_macs")) {
            val arr = root.getJSONArray("saved_macs")
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                savedMacs.add(
                    SavedMac(
                        title = obj.optString("title", "MAC مستورد"),
                        macAddress = obj.getString("macAddress"),
                        timestamp = obj.optLong("timestamp", System.currentTimeMillis())
                    )
                )
            }
        }

        val history = mutableListOf<MacHistory>()
        if (root.has("mac_history")) {
            val arr = root.getJSONArray("mac_history")
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                history.add(
                    MacHistory(
                        macAddress = obj.getString("macAddress"),
                        oldMacAddress = obj.optString("oldMacAddress", "غير معروف"),
                        timestamp = obj.optLong("timestamp", System.currentTimeMillis())
                    )
                )
            }
        }

        val logs = mutableListOf<ConfigChangeLog>()
        if (root.has("config_logs")) {
            val arr = root.getJSONArray("config_logs")
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                logs.add(
                    ConfigChangeLog(
                        changeType = obj.optString("changeType", "LOG"),
                        interfaceName = obj.optString("interfaceName", "wlan0"),
                        oldValue = obj.optString("oldValue", ""),
                        newValue = obj.optString("newValue", ""),
                        details = obj.optString("details", ""),
                        timestamp = obj.optLong("timestamp", System.currentTimeMillis())
                    )
                )
            }
        }

        val profiles = mutableListOf<NetworkProfile>()
        if (root.has("network_profiles")) {
            val arr = root.getJSONArray("network_profiles")
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                profiles.add(
                    NetworkProfile(
                        profileName = obj.getString("profileName"),
                        interfaceName = obj.optString("interfaceName", "wlan0"),
                        customMac = obj.getString("customMac"),
                        customIp = obj.optString("customIp", ""),
                        timestamp = obj.optLong("timestamp", System.currentTimeMillis())
                    )
                )
            }
        }

        return FullBackupResult(savedMacs, history, logs, profiles, timestamp)
    }
}
