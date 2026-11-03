package com.example.data

import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface RoutineDao {

    // --- Widget Styles ---
    @Query("SELECT * FROM widget_styles ORDER BY appWidgetId ASC")
    fun observeAllStyles(): Flow<List<WidgetStyleEntity>>

    @Query("SELECT * FROM widget_styles ORDER BY appWidgetId ASC")
    suspend fun getAllStyles(): List<WidgetStyleEntity>

    @Query("SELECT * FROM widget_styles WHERE appWidgetId = :appWidgetId LIMIT 1")
    fun observeStyleForWidget(appWidgetId: Int): Flow<WidgetStyleEntity?>

    @Query("SELECT * FROM widget_styles WHERE appWidgetId = :appWidgetId LIMIT 1")
    suspend fun getStyleForWidget(appWidgetId: Int): WidgetStyleEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertStyle(style: WidgetStyleEntity)

    @Query("DELETE FROM widget_styles WHERE appWidgetId = :appWidgetId")
    suspend fun deleteStyleForWidget(appWidgetId: Int)

    // --- Routine Blocks ---
    @Query("SELECT * FROM routine_blocks WHERE appWidgetId = :appWidgetId ORDER BY startMinuteOfDay ASC")
    fun observeBlocksForWidget(appWidgetId: Int): Flow<List<RoutineBlockEntity>>

    @Query("SELECT * FROM routine_blocks WHERE appWidgetId = :appWidgetId ORDER BY startMinuteOfDay ASC")
    suspend fun getBlocksForWidget(appWidgetId: Int): List<RoutineBlockEntity>

    @Query("SELECT * FROM routine_blocks ORDER BY startMinuteOfDay ASC")
    suspend fun getAllBlocks(): List<RoutineBlockEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBlock(block: RoutineBlockEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBlocks(blocks: List<RoutineBlockEntity>)

    @Update
    suspend fun updateBlock(block: RoutineBlockEntity)

    @Query("DELETE FROM routine_blocks WHERE id = :blockId")
    suspend fun deleteBlockById(blockId: Long)

    @Query("DELETE FROM routine_blocks WHERE appWidgetId = :appWidgetId")
    suspend fun deleteBlocksForWidget(appWidgetId: Int)

    // --- Style Presets ---
    @Query("SELECT * FROM style_presets ORDER BY isBuiltIn DESC, id ASC")
    fun observeAllPresets(): Flow<List<StylePresetRecord>>

    @Query("SELECT * FROM style_presets ORDER BY isBuiltIn DESC, id ASC")
    suspend fun getAllPresets(): List<StylePresetRecord>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPreset(preset: StylePresetRecord): Long

    @Query("DELETE FROM style_presets WHERE id = :presetId AND isBuiltIn = 0")
    suspend fun deleteUserPreset(presetId: Long)

    @Transaction
    suspend fun replaceBlocksForWidget(appWidgetId: Int, blocks: List<RoutineBlockEntity>) {
        deleteBlocksForWidget(appWidgetId)
        insertBlocks(blocks.map { it.copy(id = 0L, appWidgetId = appWidgetId) })
    }
}

/**
 * Room-compatible entity embedding the style snapshot with a prefix so KSP generates clean SQL columns.
 */
@androidx.room.Entity(tableName = "style_presets")
data class StylePresetRecord(
    @androidx.room.PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val name: String,
    val isBuiltIn: Boolean = false,
    @Embedded(prefix = "snap_")
    val styleSnapshot: WidgetStyleEntity
) {
    fun toDomain(): StylePresetEntity = StylePresetEntity(
        id = id,
        name = name,
        isBuiltIn = isBuiltIn,
        styleSnapshot = styleSnapshot
    )

    companion object {
        fun fromDomain(entity: StylePresetEntity): StylePresetRecord = StylePresetRecord(
            id = entity.id,
            name = entity.name,
            isBuiltIn = entity.isBuiltIn,
            styleSnapshot = entity.styleSnapshot
        )
    }
}
