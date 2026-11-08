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
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.ritmo.app.domain.Difficulty
import com.ritmo.app.domain.DayRecord
import com.ritmo.app.domain.DayStatus
import com.ritmo.app.domain.Focus
import com.ritmo.app.domain.FocusMode
import com.ritmo.app.domain.HomeTabs
import com.ritmo.app.domain.Period
import com.ritmo.app.domain.Quadrant
import com.ritmo.app.domain.Screen
import com.ritmo.app.domain.ReminderPlan
import com.ritmo.app.domain.ThemeMode
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
    /** Horários dos lembretes, em minutos desde a meia-noite e em ordem crescente. */
    val reminders: List<Int> = emptyList(),
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
    val petId: String? = null,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val remindersEnabled: Boolean = true,
    val streakAlertEnabled: Boolean = true,
    val streakAlertMinutes: Int = ReminderPlan.DEFAULT_ALERT_MINUTES,
    val hapticsEnabled: Boolean = true,
    val weekStartsMonday: Boolean = false,
    val isPro: Boolean = false,
    /** Telas do rodapé, em ordem; vazio vale o padrão. Leia por [tabs]. */
    val homeTabs: String = "",
    val focusMinutes: Int = Focus.DEFAULT_FOCUS,
    val breakMinutes: Int = Focus.DEFAULT_BREAK,
) {
    val tabs: List<Screen> get() = HomeTabs.parse(homeTabs)
}

/**
 * Cópia local da conta conectada (a conta de verdade fica no Firebase). Só existe uma: [id] é sempre 0,
 * como em [Settings]. Sair apaga a linha junto com o progresso local, que continua guardado na conta.
 */
@Entity(tableName = "account")
data class Account(
    @PrimaryKey val id: Int = 0,
    val name: String,
    val email: String,
    /** Sobra da versão em que a senha ficava no aparelho; hoje sempre nulo, quem confere é o Firebase. */
    val password: String?,
    val signedIn: Boolean,
)

/** Lista livre (compras, exercícios, estudos...), independente da rotina diária. */
@Entity(tableName = "checklists")
data class Checklist(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val emoji: String,
    val colorIndex: Int,
    val createdDay: Long,
)

@Entity(tableName = "checklist_items", indices = [Index("listId")])
data class ChecklistItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val listId: Long,
    val title: String,
    val difficulty: Difficulty,
    /** Dia em que foi marcado; nulo enquanto estiver pendente. */
    val doneDay: Long? = null,
)

/** Uma tarefa da matriz de Eisenhower. */
@Entity(tableName = "matrix_tasks")
data class MatrixTask(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val quadrant: Quadrant,
    val createdDay: Long,
    /** Dia em que foi concluída; nulo enquanto estiver pendente. */
    val doneDay: Long? = null,
)

/** Um Pomodoro concluído ou um tempo de cronômetro encerrado. */
@Entity(tableName = "focus_sessions", indices = [Index("day")])
data class FocusSession(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val day: Long,
    val startedAt: Long,
    val seconds: Int,
    val mode: FocusMode,
    /** No que a pessoa focou, se escolheu algo. */
    val title: String? = null,
)

class Converters {
    @TypeConverter
    fun fromReminders(reminders: List<Int>): String = reminders.joinToString(",")

    @TypeConverter
    fun toReminders(value: String): List<Int> = value.split(',').mapNotNull { it.toIntOrNull() }
}

@Dao
interface RitmoDao {
    @Query("SELECT * FROM account WHERE id = 0")
    fun observeAccount(): Flow<Account?>

    @Query("SELECT * FROM account WHERE id = 0")
    suspend fun account(): Account?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveAccount(account: Account)

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

    @Query("SELECT * FROM checklists ORDER BY id")
    fun observeLists(): Flow<List<Checklist>>

    @Query("SELECT * FROM checklist_items ORDER BY id")
    fun observeItems(): Flow<List<ChecklistItem>>

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

    @Query("SELECT * FROM checklists ORDER BY id")
    suspend fun lists(): List<Checklist>

    @Query("SELECT * FROM checklist_items ORDER BY id")
    suspend fun items(): List<ChecklistItem>

    @Query("SELECT * FROM objectives WHERE id = :id")
    suspend fun objective(id: Long): Objective?

    @Query("SELECT * FROM day_records WHERE objectiveId = :id AND day = :day")
    suspend fun record(id: Long, day: Long): DayRecordRow?

    @Query("SELECT * FROM checklist_items WHERE id = :id")
    suspend fun item(id: Long): ChecklistItem?

    @Query("SELECT * FROM checklist_items WHERE listId = :listId ORDER BY id")
    suspend fun itemsOf(listId: Long): List<ChecklistItem>

    @Query("SELECT * FROM checklists WHERE id = :id")
    suspend fun list(id: Long): Checklist?

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

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveList(list: Checklist): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveItem(item: ChecklistItem): Long

    @Query("DELETE FROM checklists WHERE id = :id")
    suspend fun deleteList(id: Long)

    @Query("DELETE FROM checklist_items WHERE listId = :listId")
    suspend fun deleteItemsOf(listId: Long)

    @Query("DELETE FROM checklist_items WHERE id = :id")
    suspend fun deleteItem(id: Long)

    @Query("SELECT * FROM matrix_tasks ORDER BY id")
    fun observeTasks(): Flow<List<MatrixTask>>

    @Query("SELECT * FROM matrix_tasks ORDER BY id")
    suspend fun tasks(): List<MatrixTask>

    @Query("SELECT * FROM matrix_tasks WHERE id = :id")
    suspend fun task(id: Long): MatrixTask?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveTask(task: MatrixTask): Long

    @Query("DELETE FROM matrix_tasks WHERE id = :id")
    suspend fun deleteTask(id: Long)

    @Query("SELECT * FROM focus_sessions ORDER BY startedAt")
    fun observeSessions(): Flow<List<FocusSession>>

    @Query("SELECT * FROM focus_sessions ORDER BY startedAt")
    suspend fun sessions(): List<FocusSession>

    @Insert
    suspend fun addSession(session: FocusSession): Long
}

/**
 * Versão 2: um objetivo passa a ter vários lembretes e surgem as listas. O SQLite do Android 8
 * não remove colunas, então `objectives` é recriada; o lembrete antigo vira o primeiro da lista.
 */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `objectives_new` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `title` TEXT NOT NULL, " +
                "`emoji` TEXT NOT NULL, `colorIndex` INTEGER NOT NULL, `daysMask` INTEGER NOT NULL, " +
                "`difficulty` TEXT NOT NULL, `period` TEXT NOT NULL, `reminders` TEXT NOT NULL, " +
                "`createdDay` INTEGER NOT NULL, `archivedDay` INTEGER)",
        )
        db.execSQL(
            "INSERT INTO `objectives_new` (`id`, `title`, `emoji`, `colorIndex`, `daysMask`, " +
                "`difficulty`, `period`, `reminders`, `createdDay`, `archivedDay`) " +
                "SELECT `id`, `title`, `emoji`, `colorIndex`, `daysMask`, `difficulty`, `period`, " +
                "COALESCE(CAST(`reminderMinutes` AS TEXT), ''), `createdDay`, `archivedDay` " +
                "FROM `objectives`",
        )
        db.execSQL("DROP TABLE `objectives`")
        db.execSQL("ALTER TABLE `objectives_new` RENAME TO `objectives`")
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `checklists` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `title` TEXT NOT NULL, " +
                "`emoji` TEXT NOT NULL, `colorIndex` INTEGER NOT NULL, `createdDay` INTEGER NOT NULL)",
        )
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `checklist_items` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `listId` INTEGER NOT NULL, " +
                "`title` TEXT NOT NULL, `difficulty` TEXT NOT NULL, `doneDay` INTEGER)",
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_checklist_items_listId` ON `checklist_items` (`listId`)")
    }
}

/**
 * Versão 3: entram as preferências e os acessórios dão lugar aos pets. `settings` é recriada
 * para trocar a coluna, e quem tinha comprado um acessório recebe as moedas de volta.
 */
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `settings_new` (" +
                "`id` INTEGER NOT NULL, `lastClosedDay` INTEGER NOT NULL, `themeId` TEXT, `petId` TEXT, " +
                "`themeMode` TEXT NOT NULL, `remindersEnabled` INTEGER NOT NULL, " +
                "`streakAlertEnabled` INTEGER NOT NULL, `streakAlertMinutes` INTEGER NOT NULL, " +
                "`hapticsEnabled` INTEGER NOT NULL, `weekStartsMonday` INTEGER NOT NULL, PRIMARY KEY(`id`))",
        )
        db.execSQL(
            "INSERT INTO `settings_new` (`id`, `lastClosedDay`, `themeId`, `petId`, `themeMode`, " +
                "`remindersEnabled`, `streakAlertEnabled`, `streakAlertMinutes`, `hapticsEnabled`, " +
                "`weekStartsMonday`) SELECT `id`, `lastClosedDay`, `themeId`, NULL, 'SYSTEM', 1, 1, " +
                "${ReminderPlan.DEFAULT_ALERT_MINUTES}, 1, 0 FROM `settings`",
        )
        db.execSQL("DROP TABLE `settings`")
        db.execSQL("ALTER TABLE `settings_new` RENAME TO `settings`")
        db.execSQL(
            "INSERT OR IGNORE INTO `ledger` (`key`, `xp`, `coins`, `freezes`, `day`) " +
                "SELECT 'refund:' || `key`, 0, -`coins`, 0, `day` FROM `ledger` " +
                "WHERE `key` LIKE 'purchase:acc_%'",
        )
    }
}

/** Versão 4: surge o plano Pro. Todo mundo começa no plano grátis, com o que já tinha. */
val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `settings` ADD COLUMN `isPro` INTEGER NOT NULL DEFAULT 0")
    }
}

/** Versão 5: surge a conta. Quem já usa o app a cria ao abrir, e o progresso fica vinculado a ela. */
val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `account` (" +
                "`id` INTEGER NOT NULL, `name` TEXT NOT NULL, `email` TEXT NOT NULL, " +
                "`password` TEXT, `signedIn` INTEGER NOT NULL, PRIMARY KEY(`id`))",
        )
    }
}

/** Versão 6: surgem a matriz de Eisenhower, o foco (Pomodoro e cronômetro) e o rodapé personalizável. */
val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `settings` ADD COLUMN `homeTabs` TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE `settings` ADD COLUMN `focusMinutes` INTEGER NOT NULL DEFAULT ${Focus.DEFAULT_FOCUS}")
        db.execSQL("ALTER TABLE `settings` ADD COLUMN `breakMinutes` INTEGER NOT NULL DEFAULT ${Focus.DEFAULT_BREAK}")
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `matrix_tasks` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `title` TEXT NOT NULL, " +
                "`quadrant` TEXT NOT NULL, `createdDay` INTEGER NOT NULL, `doneDay` INTEGER)",
        )
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `focus_sessions` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `day` INTEGER NOT NULL, " +
                "`startedAt` INTEGER NOT NULL, `seconds` INTEGER NOT NULL, `mode` TEXT NOT NULL, `title` TEXT)",
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_focus_sessions_day` ON `focus_sessions` (`day`)")
    }
}

/** Em ordem: propriedades de topo inicializam na ordem do arquivo. */
val MIGRATIONS = arrayOf(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6)

@Database(
    entities = [
        Objective::class,
        DayRecordRow::class,
        LedgerEntry::class,
        UnlockedAchievement::class,
        Settings::class,
        Checklist::class,
        ChecklistItem::class,
        Account::class,
        MatrixTask::class,
        FocusSession::class,
    ],
    version = 6,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class RitmoDatabase : RoomDatabase() {
    abstract fun dao(): RitmoDao

    companion object {
        fun create(context: Context): RitmoDatabase =
            Room.databaseBuilder(context, RitmoDatabase::class.java, "ritmo.db")
                .addMigrations(*MIGRATIONS)
                .build()
    }
}
