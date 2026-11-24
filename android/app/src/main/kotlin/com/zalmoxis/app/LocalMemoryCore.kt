package com.zalmoxis.app

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID

/**
 * Phase 0 foundation: explicit, scoped, durable facts.
 *
 * This is deliberately NOT automatic LLM memory promotion. External text and AI
 * synthesis cannot write to this store without a separate user approval path.
 * Scoping is applied to every read and mutation through SQL, not prompt text.
 */
internal data class MemoryFact(
    val id: String,
    val userId: String,
    val clientId: String,
    val text: String,
    val sourceType: String,
    val sourceRef: String?,
    val createdAt: Long,
    val modifiedAt: Long,
    val revision: Int,
)

internal object MemoryScopePolicy {
    const val PERSONAL_USER = "local-owner"
    const val PERSONAL_CLIENT = "personal"
    fun validScopePart(part: String): Boolean =
        part.isNotBlank() && part.length <= 120 &&
            part.all { it.isLetterOrDigit() || it == '-' || it == '_' || it == '.' }
    fun validFactText(text: String): Boolean = text.isNotBlank() && text.length <= 4000
}

/** Uses a single app-private database. No encrypted-at-rest claim is made. */
internal class LocalMemoryCore(context: Context) :
    SQLiteOpenHelper(context.applicationContext, "zalmoxis_memory_v1.db", null, 1) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """CREATE TABLE memory_facts (
                id TEXT PRIMARY KEY NOT NULL,
                user_id TEXT NOT NULL,
                client_id TEXT NOT NULL,
                fact_text TEXT NOT NULL,
                source_type TEXT NOT NULL,
                source_ref TEXT,
                created_at INTEGER NOT NULL,
                modified_at INTEGER NOT NULL,
                revision INTEGER NOT NULL DEFAULT 1,
                CHECK(length(fact_text) BETWEEN 1 AND 4000)
            )""".trimIndent()
        )
        db.execSQL("CREATE INDEX memory_scope_recent ON memory_facts (user_id, client_id, modified_at DESC)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Never silently drop or destroy memory on an unknown migration.
        error("Unsupported memory database migration from $oldVersion to $newVersion")
    }

    private fun verifyScope(userId: String, clientId: String) {
        require(MemoryScopePolicy.validScopePart(userId) &&
            MemoryScopePolicy.validScopePart(clientId)) { "Invalid memory scope." }
    }

    suspend fun saveUserFact(
        userId: String,
        clientId: String,
        text: String,
        sourceRef: String? = null,
    ): MemoryFact = withContext(Dispatchers.IO) {
        verifyScope(userId, clientId)
        val cleaned = text.trim()
        require(MemoryScopePolicy.validFactText(cleaned)) { "Fact must contain 1–4000 characters." }
        require(sourceRef == null || sourceRef.length <= 500) { "Source reference too long." }
        val now = System.currentTimeMillis()
        val record = MemoryFact(UUID.randomUUID().toString(), userId, clientId,
            cleaned, "explicit_user", sourceRef, now, now, 1)
        val values = ContentValues().apply {
            put("id", record.id)
            put("user_id", userId)
            put("client_id", clientId)
            put("fact_text", cleaned)
            put("source_type", record.sourceType)
            put("source_ref", sourceRef)
            put("created_at", now)
            put("modified_at", now)
            put("revision", 1)
        }
        check(writableDatabase.insertOrThrow("memory_facts", null, values) != -1L)
        record
    }

    suspend fun listFacts(userId: String, clientId: String, limit: Int = 200):
        List<MemoryFact> = withContext(Dispatchers.IO) {
        verifyScope(userId, clientId)
        require(limit in 1..500)
        val result = mutableListOf<MemoryFact>()
        readableDatabase.query(
            "memory_facts",
            arrayOf("id","user_id","client_id","fact_text","source_type",
                "source_ref","created_at","modified_at","revision"),
            "user_id=? AND client_id=?",
            arrayOf(userId, clientId), null, null,
            "modified_at DESC, id ASC", limit.toString(),
        ).use { cursor ->
            while (cursor.moveToNext()) {
                result += MemoryFact(
                    cursor.getString(0), cursor.getString(1), cursor.getString(2),
                    cursor.getString(3), cursor.getString(4),
                    if (cursor.isNull(5)) null else cursor.getString(5),
                    cursor.getLong(6), cursor.getLong(7), cursor.getInt(8),
                )
            }
        }
        result
    }

    /** Optimistic revision guard prevents silently overwriting concurrent edits. */
    suspend fun correctUserFact(
        userId: String, clientId: String, id: String, expectedRevision: Int, text: String,
    ): Boolean = withContext(Dispatchers.IO) {
        verifyScope(userId, clientId)
        require(expectedRevision >= 1 && id.isNotBlank())
        val cleaned = text.trim()
        require(MemoryScopePolicy.validFactText(cleaned))
        val values = ContentValues().apply {
            put("fact_text", cleaned)
            put("modified_at", System.currentTimeMillis())
            put("revision", expectedRevision + 1)
        }
        writableDatabase.update("memory_facts", values,
            "id=? AND user_id=? AND client_id=? AND revision=? AND source_type='explicit_user'",
            arrayOf(id, userId, clientId, expectedRevision.toString())) == 1
    }

    /** Scope-checked app-level deletion, followed by absence verification. */
    suspend fun deleteFact(userId: String, clientId: String, id: String): Boolean =
        withContext(Dispatchers.IO) {
            verifyScope(userId, clientId)
            require(id.isNotBlank())
            val db = writableDatabase
            db.beginTransaction()
            try {
                val deleted = db.delete("memory_facts", "id=? AND user_id=? AND client_id=?",
                    arrayOf(id, userId, clientId))
                val absent = db.query("memory_facts", arrayOf("id"),
                    "id=? AND user_id=? AND client_id=?", arrayOf(id, userId, clientId),
                    null, null, null).use { !it.moveToFirst() }
                check(absent) { "Fact still exists after deletion." }
                db.setTransactionSuccessful()
                deleted == 1
            } finally {
                db.endTransaction()
            }
        }
}
