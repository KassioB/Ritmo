package com.ritmo.app.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import com.ritmo.app.domain.Difficulty
import com.ritmo.app.domain.DayRecord
import com.ritmo.app.domain.DayStatus
import com.ritmo.app.domain.Period
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Entity(tableName = "objectives")
data class Objective(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val emoji: String,
    val colorIndex: Int,
    /** Bit 0 = segunda ... bit 6 = domingo. */
    val daysMask: Int,
    val difficulty: Difficulty,
    val period: Period,
    val reminderMinutes: Int?,
    val createdDay: Long,
    /** Objetivos são arquivados, nunca apagados, para o histórico continuar íntegro. */
    val archivedDay: Long? = null,
) {
    fun isScheduledOn(day: Long): Boolean {
        val weekday = LocalDate.ofEpochDay(day).dayOfWeek.value - 1
        return day >= createdDay &&
            (archivedDay == null || day < archivedDay) &&
            daysMask and (1 shl weekday) != 0
    }

    companion object {
        const val EVERY_DAY = 0b1111111
    }
}

@Entity(
    tableName = "day_records",
    primaryKeys = ["objectiveId", "day"],
    indices = [Index("day")],
)
data class DayRecordRow(val objectiveId: Long, val day: Long, val status: DayStatus) {
    fun toDomain() = DayRecord(objectiveId, day, status)
}

/**
 * Livro-razão de XP, moedas e bloqueios. A chave torna cada recompensa idempotente, e desmarcar
 * um objetivo apaga a entrada dele, então marcar e desmarcar não gera ganho.
 */
@Entity(tableName = "ledger")
data class LedgerEntry(
    @PrimaryKey val key: String,
    val xp: Int = 0,
    val coins: Int = 0,
    val freezes: Int = 0,
    val day: Long,
)

@Entity(tableName = "unlocked_achievements")
data class UnlockedAchievement(@PrimaryKey val id: String, val day: Long)

@Entity(tableName = "settings")
data class Settings(
    @PrimaryKey val id: Int = 0,
    val lastClosedDay: Long,
    val themeId: String? = null,
    val accessoryId: String? = null,
)

@Dao
interface RitmoDao {
    @Query("SELECT * FROM objectives ORDER BY id")
    fun observeObjectives(): Flow<List<Objective>>

    @Query("SELECT * FROM day_records")
    fun observeRecords(): Flow<List<DayRecordRow>>

    @Query("SELECT * FROM ledger")
    fun observeLedger(): Flow<List<LedgerEntry>>

    @Query("SELECT * FROM unlocked_achievements")
    fun observeUnlocked(): Flow<List<UnlockedAchievement>>

    @Query("SELECT * FROM settings WHERE id = 0")
    fun observeSettings(): Flow<Settings?>

    @Query("SELECT * FROM objectives ORDER BY id")
    suspend fun objectives(): List<Objective>

    @Query("SELECT * FROM day_records")
    suspend fun records(): List<DayRecordRow>

    @Query("SELECT * FROM ledger")
    suspend fun ledger(): List<LedgerEntry>

    @Query("SELECT * FROM unlocked_achievements")
    suspend fun unlocked(): List<UnlockedAchievement>

    @Query("SELECT * FROM settings WHERE id = 0")
    suspend fun settings(): Settings?

    @Query("SELECT * FROM objectives WHERE id = :id")
    suspend fun objective(id: Long): Objective?

    @Query("SELECT * FROM day_records WHERE objectiveId = :id AND day = :day")
    suspend fun record(id: Long, day: Long): DayRecordRow?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveObjective(objective: Objective): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveRecords(records: List<DayRecordRow>)

    @Query("DELETE FROM day_records WHERE objectiveId = :id AND day = :day")
    suspend fun deleteRecord(id: Long, day: Long)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addLedger(entry: LedgerEntry)

    @Query("DELETE FROM ledger WHERE `key` = :key")
    suspend fun deleteLedger(key: String)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addUnlocked(achievement: UnlockedAchievement)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveSettings(settings: Settings)
}

@Database(
    entities = [
        Objective::class,
        DayRecordRow::class,
        LedgerEntry::class,
        UnlockedAchievement::class,
        Settings::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class RitmoDatabase : RoomDatabase() {
    abstract fun dao(): RitmoDao

    companion object {
        fun create(context: Context): RitmoDatabase =
            Room.databaseBuilder(context, RitmoDatabase::class.java, "ritmo.db").build()
    }
}
