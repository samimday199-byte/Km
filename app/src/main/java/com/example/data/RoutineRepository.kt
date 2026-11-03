package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class RoutineRepository(private val dao: RoutineDao) {

    private val seedMutex = Mutex()

    fun observeAllStyles(): Flow<List<WidgetStyleEntity>> = dao.observeAllStyles()

    fun observeStyleForWidget(appWidgetId: Int): Flow<WidgetStyleEntity?> =
        dao.observeStyleForWidget(appWidgetId)

    fun observeBlocksForWidget(appWidgetId: Int): Flow<List<RoutineBlockEntity>> =
        dao.observeBlocksForWidget(appWidgetId)

    fun observeAllPresets(): Flow<List<StylePresetEntity>> =
        dao.observeAllPresets().map { list -> list.map { it.toDomain() } }

    suspend fun ensureSeeded() = seedMutex.withLock {
        val existingPresets = dao.getAllPresets()
        if (existingPresets.isEmpty()) {
            BuiltInPresets.all().forEach { preset ->
                dao.upsertPreset(StylePresetRecord.fromDomain(preset))
            }
        }

        val existingStyles = dao.getAllStyles()
        if (existingStyles.isEmpty()) {
            val defaultId = RoutineBlockEntity.DEFAULT_STUDIO_WIDGET_ID
            val defaultStyle = WidgetStyleEntity(
                appWidgetId = defaultId,
                widgetName = "Main Studio Widget (4×2)"
            )
            dao.upsertStyle(defaultStyle)
            dao.insertBlocks(defaultRoutineBlocks(defaultId))

            // Also seed a second profile so users can immediately test multi-widget switching
            val focusWidgetId = 2
            val focusStyle = BuiltInPresets.all()[1].styleSnapshot.copy(
                appWidgetId = focusWidgetId,
                widgetName = "Minimal Night Routine (2×1)",
                use24HourFormat = true,
                aspectRatioKey = AspectRatioPreset.RATIO_4_1.key,
                titleSizeSp = 20f
            )
            dao.upsertStyle(focusStyle)
            dao.insertBlocks(secondaryRoutineBlocks(focusWidgetId))
        }
    }

    suspend fun getOrCreateWidgetStyle(
        appWidgetId: Int,
        defaultName: String = "Widget #$appWidgetId"
    ): WidgetStyleEntity {
        ensureSeeded()
        val existing = dao.getStyleForWidget(appWidgetId)
        if (existing != null) return existing

        // Clone the primary studio widget's style & blocks when placing a new home-screen widget
        val templateStyle = dao.getStyleForWidget(RoutineBlockEntity.DEFAULT_STUDIO_WIDGET_ID)
            ?: WidgetStyleEntity()
        val newStyle = templateStyle.copy(
            appWidgetId = appWidgetId,
            widgetName = defaultName
        )
        dao.upsertStyle(newStyle)

        val existingBlocks = dao.getBlocksForWidget(appWidgetId)
        if (existingBlocks.isEmpty()) {
            val templateBlocks = dao.getBlocksForWidget(RoutineBlockEntity.DEFAULT_STUDIO_WIDGET_ID)
                .ifEmpty { defaultRoutineBlocks(appWidgetId) }
            dao.insertBlocks(templateBlocks.map { it.copy(id = 0L, appWidgetId = appWidgetId) })
        }
        return newStyle
    }

    suspend fun cloneProfileToWidget(
        sourceWidgetId: Int,
        targetWidgetId: Int,
        targetName: String
    ) {
        ensureSeeded()
        val sourceStyle = dao.getStyleForWidget(sourceWidgetId) ?: WidgetStyleEntity()
        val sourceBlocks = dao.getBlocksForWidget(sourceWidgetId)
            .ifEmpty { defaultRoutineBlocks(targetWidgetId) }

        dao.upsertStyle(sourceStyle.copy(appWidgetId = targetWidgetId, widgetName = targetName))
        dao.replaceBlocksForWidget(targetWidgetId, sourceBlocks)
    }

    suspend fun createNewStudioProfile(name: String): Int {
        ensureSeeded()
        val allStyles = dao.getAllStyles()
        val nextId = (allStyles.maxOfOrNull { it.appWidgetId } ?: 1).coerceAtLeast(1) + 1
        val baseStyle = BuiltInPresets.all().random().styleSnapshot.copy(
            appWidgetId = nextId,
            widgetName = name
        )
        dao.upsertStyle(baseStyle)
        dao.insertBlocks(defaultRoutineBlocks(nextId))
        return nextId
    }

    suspend fun getAllStyles(): List<WidgetStyleEntity> {
        ensureSeeded()
        return dao.getAllStyles()
    }

    suspend fun getBlocksForWidget(appWidgetId: Int): List<RoutineBlockEntity> {
        ensureSeeded()
        return dao.getBlocksForWidget(appWidgetId)
    }

    suspend fun getAllBlocks(): List<RoutineBlockEntity> {
        ensureSeeded()
        return dao.getAllBlocks()
    }

    suspend fun saveStyle(style: WidgetStyleEntity) {
        dao.upsertStyle(style)
    }

    suspend fun saveBlock(block: RoutineBlockEntity): Long {
        return dao.insertBlock(block)
    }

    suspend fun deleteBlock(blockId: Long) {
        dao.deleteBlockById(blockId)
    }

    suspend fun replaceBlocksForWidget(appWidgetId: Int, blocks: List<RoutineBlockEntity>) {
        dao.replaceBlocksForWidget(appWidgetId, blocks)
    }

    suspend fun deleteWidgetData(appWidgetId: Int) {
        if (appWidgetId == RoutineBlockEntity.DEFAULT_STUDIO_WIDGET_ID) return
        dao.deleteBlocksForWidget(appWidgetId)
        dao.deleteStyleForWidget(appWidgetId)
    }

    suspend fun saveUserPreset(name: String, style: WidgetStyleEntity): Long {
        return dao.upsertPreset(
            StylePresetRecord(
                id = 0L,
                name = name,
                isBuiltIn = false,
                styleSnapshot = style
            )
        )
    }

    suspend fun duplicatePreset(preset: StylePresetEntity): Long {
        return dao.upsertPreset(
            StylePresetRecord(
                id = 0L,
                name = "${preset.name} Copy",
                isBuiltIn = false,
                styleSnapshot = preset.styleSnapshot
            )
        )
    }

    suspend fun deleteUserPreset(presetId: Long) {
        dao.deleteUserPreset(presetId)
    }

    companion object {
        /**
         * Rich 24-hour routine spanning morning, deep work, afternoon, evening, and a midnight-crossing block
         * so there is always an active or upcoming block ready to preview at any hour of the day.
         */
        fun defaultRoutineBlocks(appWidgetId: Int): List<RoutineBlockEntity> = listOf(
            RoutineBlockEntity(
                appWidgetId = appWidgetId,
                title = "Morning Reset & Espresso",
                startMinuteOfDay = 6 * 60 + 30, // 6:30 AM
                endMinuteOfDay = 8 * 60 + 30,   // 8:30 AM
                daysOfWeekMask = RoutineBlockEntity.ALL_DAYS_MASK,
                accentColorArgb = 0xFF38BDF8L
            ),
            RoutineBlockEntity(
                appWidgetId = appWidgetId,
                title = "Deep Work: Writing",
                startMinuteOfDay = 8 * 60 + 30, // 8:30 AM
                endMinuteOfDay = 12 * 60,       // 12:00 PM
                daysOfWeekMask = RoutineBlockEntity.ALL_DAYS_MASK,
                accentColorArgb = 0xFF00E5FFL
            ),
            RoutineBlockEntity(
                appWidgetId = appWidgetId,
                title = "Midday Walk & Lunch",
                startMinuteOfDay = 12 * 60,      // 12:00 PM
                endMinuteOfDay = 13 * 60 + 30,  // 1:30 PM
                daysOfWeekMask = RoutineBlockEntity.ALL_DAYS_MASK,
                accentColorArgb = 0xFF34D399L
            ),
            RoutineBlockEntity(
                appWidgetId = appWidgetId,
                title = "Design & Prototyping",
                startMinuteOfDay = 13 * 60 + 30, // 1:30 PM
                endMinuteOfDay = 17 * 60 + 30,   // 5:30 PM
                daysOfWeekMask = RoutineBlockEntity.WEEKDAYS_MASK,
                accentColorArgb = 0xFFA855F7L
            ),
            RoutineBlockEntity(
                appWidgetId = appWidgetId,
                title = "Strength & Mobility",
                startMinuteOfDay = 17 * 60 + 30, // 5:30 PM
                endMinuteOfDay = 19 * 60,        // 7:00 PM
                daysOfWeekMask = RoutineBlockEntity.ALL_DAYS_MASK,
                accentColorArgb = 0xFFF43F5EL
            ),
            RoutineBlockEntity(
                appWidgetId = appWidgetId,
                title = "Creative Synthesis",
                startMinuteOfDay = 19 * 60 + 30, // 7:30 PM
                endMinuteOfDay = 22 * 60 + 30,   // 10:30 PM
                daysOfWeekMask = RoutineBlockEntity.ALL_DAYS_MASK,
                accentColorArgb = 0xFFF59E0BL
            ),
            RoutineBlockEntity(
                appWidgetId = appWidgetId,
                title = "Wind Down & Sleep",
                startMinuteOfDay = 22 * 60 + 30, // 10:30 PM
                endMinuteOfDay = 6 * 60 + 30,    // 6:30 AM (Crosses Midnight!)
                daysOfWeekMask = RoutineBlockEntity.ALL_DAYS_MASK,
                accentColorArgb = 0xFF6366F1L
            )
        )

        fun secondaryRoutineBlocks(appWidgetId: Int): List<RoutineBlockEntity> = listOf(
            RoutineBlockEntity(
                appWidgetId = appWidgetId,
                title = "Code Architecture",
                startMinuteOfDay = 9 * 60,
                endMinuteOfDay = 13 * 60,
                daysOfWeekMask = RoutineBlockEntity.WEEKDAYS_MASK,
                accentColorArgb = 0xFF10B981L
            ),
            RoutineBlockEntity(
                appWidgetId = appWidgetId,
                title = "Reading & Journaling",
                startMinuteOfDay = 21 * 60,
                endMinuteOfDay = 1 * 60, // Crosses midnight (9 PM -> 1 AM)
                daysOfWeekMask = RoutineBlockEntity.ALL_DAYS_MASK,
                accentColorArgb = 0xFF38BDF8L
            )
        )
    }
}
