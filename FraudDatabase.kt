package com.fraudx.app

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class FraudDatabase(context: Context) :
    SQLiteOpenHelper(context, "fraudx_db", null, 3) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE vault (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                type TEXT, content TEXT, risk TEXT,
                timestamp DATETIME DEFAULT CURRENT_TIMESTAMP
            )
        """)
        db.execSQL("""
            CREATE TABLE blocked_numbers (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                number TEXT UNIQUE,
                timestamp DATETIME DEFAULT CURRENT_TIMESTAMP
            )
        """)
        db.execSQL("""
            CREATE TABLE whitelist (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                number TEXT UNIQUE,
                timestamp DATETIME DEFAULT CURRENT_TIMESTAMP
            )
        """)
        db.execSQL("""
            CREATE TABLE scam_reports (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                number TEXT, reason TEXT,
                timestamp DATETIME DEFAULT CURRENT_TIMESTAMP
            )
        """)
        db.execSQL("""
            CREATE TABLE activity_log (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                type TEXT, number TEXT, preview TEXT,
                risk TEXT, timestamp INTEGER
            )
        """)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS vault")
        db.execSQL("DROP TABLE IF EXISTS blocked_numbers")
        db.execSQL("DROP TABLE IF EXISTS whitelist")
        db.execSQL("DROP TABLE IF EXISTS scam_reports")
        db.execSQL("DROP TABLE IF EXISTS activity_log")
        onCreate(db)
    }

    fun insertVaultEntry(type: String, content: String, risk: String) {
        val db = writableDatabase
        db.insert("vault", null, ContentValues().apply {
            put("type", type); put("content", content); put("risk", risk)
        })
        db.close()
    }

    fun getAllVaultEntries(): List<VaultItem> {
        val list = mutableListOf<VaultItem>()
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT * FROM vault ORDER BY timestamp DESC", null)
        if (cursor.moveToFirst()) {
            do {
                list.add(VaultItem(
                    cursor.getInt(0), cursor.getString(1),
                    cursor.getString(2), cursor.getString(3), cursor.getString(4)
                ))
            } while (cursor.moveToNext())
        }
        cursor.close(); db.close()
        return list
    }

    fun insertLog(type: String, number: String, preview: String, risk: String) {
        val db = writableDatabase
        db.insert("activity_log", null, ContentValues().apply {
            put("type", type); put("number", number)
            put("preview", preview); put("risk", risk)
            put("timestamp", System.currentTimeMillis())
        })
        db.close()
    }

    fun getAllLogs(): List<LogItem> {
        val list = mutableListOf<LogItem>()
        val db = readableDatabase
        val cursor = db.rawQuery(
            "SELECT type, number, preview, risk, timestamp FROM activity_log ORDER BY timestamp DESC LIMIT 100",
            null
        )
        if (cursor.moveToFirst()) {
            do {
                list.add(LogItem(
                    cursor.getString(0), cursor.getString(1),
                    cursor.getString(2), cursor.getString(3), cursor.getLong(4)
                ))
            } while (cursor.moveToNext())
        }
        cursor.close(); db.close()
        return list
    }

    fun blockNumber(number: String) {
        val db = writableDatabase
        db.insertWithOnConflict("blocked_numbers", null,
            ContentValues().apply { put("number", number) },
            SQLiteDatabase.CONFLICT_IGNORE)
        db.close()
    }

    fun unblockNumber(number: String) {
        val db = writableDatabase
        db.delete("blocked_numbers", "number=?", arrayOf(number))
        db.close()
    }

    fun isBlocked(number: String): Boolean {
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT id FROM blocked_numbers WHERE number=?", arrayOf(number))
        val exists = cursor.moveToFirst()
        cursor.close(); db.close()
        return exists
    }

    fun getAllBlockedNumbers(): List<String> {
        val list = mutableListOf<String>()
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT number FROM blocked_numbers ORDER BY timestamp DESC", null)
        if (cursor.moveToFirst()) {
            do { list.add(cursor.getString(0)) } while (cursor.moveToNext())
        }
        cursor.close(); db.close()
        return list
    }

    fun addToWhitelist(number: String) {
        val db = writableDatabase
        db.insertWithOnConflict("whitelist", null,
            ContentValues().apply { put("number", number) },
            SQLiteDatabase.CONFLICT_IGNORE)
        db.close()
    }

    fun isWhitelisted(number: String): Boolean {
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT id FROM whitelist WHERE number=?", arrayOf(number))
        val exists = cursor.moveToFirst()
        cursor.close(); db.close()
        return exists
    }

    fun reportScam(number: String, reason: String) {
        val db = writableDatabase
        db.insert("scam_reports", null, ContentValues().apply {
            put("number", number); put("reason", reason)
        })
        db.close()
    }
}