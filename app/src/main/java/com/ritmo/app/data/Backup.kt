package com.ritmo.app.data

import android.content.ContentValues
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

/**
 * O progresso inteiro em um pacote só, para guardar na conta. Lê as tabelas de forma genérica, então
 * uma coluna nova entra sem mudar este código; ao restaurar, uma coluna que o banco atual não conhece
 * é ignorada e uma que falta no pacote fica com o valor padrão da tabela.
 *
 * A conta em si fica de fora: ela vem do serviço de login, não do pacote.
 */
class Backup(private val db: RitmoDatabase) {

    /** JSON compactado com gzip: o histórico de anos cabe com folga no limite de um documento. */
    suspend fun export(): ByteArray = withContext(Dispatchers.IO) {
        val tables = JSONObject()
        val sql = db.openHelper.readableDatabase
        TABLES.forEach { table ->
            val rows = JSONArray()
            sql.query("SELECT * FROM `$table`").use { cursor ->
                while (cursor.moveToNext()) rows.put(row(cursor))
            }
            tables.put(table, rows)
        }
        val json = JSONObject().put("schema", SCHEMA).put("tables", tables).toString()
        gzip(json.toByteArray())
    }

    /** Troca todo o progresso local pelo do pacote, de uma vez só. */
    suspend fun restore(bytes: ByteArray) = withContext(Dispatchers.IO) {
        val tables = JSONObject(String(gunzip(bytes))).getJSONObject("tables")
        db.runInTransaction {
            val sql = db.openHelper.writableDatabase
            TABLES.forEach { table ->
                sql.execSQL("DELETE FROM `$table`")
                val rows = tables.optJSONArray(table) ?: return@forEach
                val known = columns(table)
                for (i in 0 until rows.length()) {
                    sql.insert(table, SQLiteDatabase.CONFLICT_REPLACE, values(rows.getJSONObject(i), known))
                }
            }
        }
    }

    /** Ao sair da conta: o próximo a entrar neste aparelho não pode ver o progresso de outra pessoa. */
    suspend fun clear() = withContext(Dispatchers.IO) {
        db.runInTransaction {
            val sql = db.openHelper.writableDatabase
            (TABLES + "account").forEach { sql.execSQL("DELETE FROM `$it`") }
        }
    }

    private fun row(cursor: Cursor): JSONObject {
        val row = JSONObject()
        for (i in 0 until cursor.columnCount) {
            val value: Any = when (cursor.getType(i)) {
                Cursor.FIELD_TYPE_INTEGER -> cursor.getLong(i)
                Cursor.FIELD_TYPE_FLOAT -> cursor.getDouble(i)
                Cursor.FIELD_TYPE_STRING -> cursor.getString(i)
                else -> JSONObject.NULL
            }
            row.put(cursor.getColumnName(i), value)
        }
        return row
    }

    private fun columns(table: String): Set<String> {
        val names = mutableSetOf<String>()
        db.openHelper.readableDatabase.query("PRAGMA table_info(`$table`)").use { cursor ->
            val name = cursor.getColumnIndexOrThrow("name")
            while (cursor.moveToNext()) names += cursor.getString(name)
        }
        return names
    }

    private fun values(row: JSONObject, known: Set<String>) = ContentValues().apply {
        row.keys().forEach { column ->
            if (column !in known) return@forEach
            when (val value = row.get(column)) {
                JSONObject.NULL -> putNull(column)
                is Int -> put(column, value.toLong())
                is Long -> put(column, value)
                is Double -> put(column, value)
                is Boolean -> put(column, if (value) 1L else 0L)
                else -> put(column, value.toString())
            }
        }
    }

    private fun gzip(bytes: ByteArray): ByteArray =
        ByteArrayOutputStream().also { out -> GZIPOutputStream(out).use { it.write(bytes) } }.toByteArray()

    private fun gunzip(bytes: ByteArray): ByteArray = GZIPInputStream(ByteArrayInputStream(bytes)).use { it.readBytes() }

    companion object {
        /** Muda só se o formato do pacote mudar; colunas novas não pedem isso. */
        const val SCHEMA = 1

        /** Tudo o que é progresso ou preferência da pessoa. */
        val TABLES = listOf(
            "objectives",
            "day_records",
            "ledger",
            "unlocked_achievements",
            "settings",
            "checklists",
            "checklist_items",
        )
    }
}
