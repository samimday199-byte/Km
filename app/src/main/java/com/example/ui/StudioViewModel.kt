package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ActiveRoutineState
import com.example.data.AppDatabase
import com.example.data.RoutineBlockEntity
import com.example.data.RoutineCalculator
import com.example.data.RoutineRepository
import com.example.data.StylePresetEntity
import com.example.data.WidgetStyleEntity
import com.example.widget.RoutineAlarmScheduler
import com.example.widget.RoutineWidgetUpdater
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.ZonedDateTime

enum class StudioTab(val label: String) {
    PREVIEW("Preview"),
    ROUTINE("Routine"),
    BACKGROUND("Background"),
    SHAPE("Shape"),
    GLASS("Glass"),
    TEXT("Text")
}

data class UndoSnapshot(
    val style: WidgetStyleEntity,
    val blocks: List<RoutineBlockEntity>
)

data class StudioUiState(
    val selectedWidgetId: Int = RoutineBlockEntity.DEFAULT_STUDIO_WIDGET_ID,
    val allProfiles: List<WidgetStyleEntity> = emptyList(),
    val currentStyle: WidgetStyleEntity = WidgetStyleEntity(),
    val currentBlocks: List<RoutineBlockEntity> = emptyList(),
    val allPresets: List<StylePresetEntity> = emptyList(),
    val activeRoutineState: ActiveRoutineState = RoutineCalculator.evaluate(emptyList()),
    val selectedTab: StudioTab = StudioTab.PREVIEW,
    val selectedWallpaperIndex: Int = 0,
    val showCanvasGrid: Boolean = true,
    val canUndo: Boolean = false,
    val canRedo: Boolean = false,
    val exactAlarmPermitted: Boolean = true,
    val showBatteryTipDialog: Boolean = false,
    val editingBlock: RoutineBlockEntity? = null,
    val isCreatingNewBlock: Boolean = false,
    val statusBannerMessage: String? = null
)

class StudioViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = RoutineRepository(AppDatabase.getInstance(application).routineDao())

    private val _uiState = MutableStateFlow(StudioUiState())
    val uiState: StateFlow<StudioUiState> = _uiState.asStateFlow()

    private val undoStack = ArrayDeque<UndoSnapshot>()
    private val redoStack = ArrayDeque<UndoSnapshot>()
    private var isApplyingUndoRedo = false
    private var widgetSubscriptionJob: Job? = null

    init {
        viewModelScope.launch {
            repository.ensureSeeded()
            refreshExactAlarmStatus()

            // Observe global list of widget profiles and presets
            launch {
                combine(
                    repository.observeAllStyles(),
                    repository.observeAllPresets()
                ) { profiles, presets ->
                    profiles to presets
                }.collectLatest { (profiles, presets) ->
                    _uiState.update { state ->
                        val activeProfile = profiles.firstOrNull { it.appWidgetId == state.selectedWidgetId }
                            ?: profiles.firstOrNull()
                            ?: state.currentStyle
                        state.copy(
                            allProfiles = profiles,
                            allPresets = presets,
                            selectedWidgetId = activeProfile.appWidgetId,
                            currentStyle = activeProfile
                        )
                    }
                }
            }

            subscribeToSelectedWidget(RoutineBlockEntity.DEFAULT_STUDIO_WIDGET_ID)

            // Live 1-second ticker so the studio preview countdown and playhead update in real time
            launch {
                while (isActive) {
                    val blocks = _uiState.value.currentBlocks
                    val evaluated = RoutineCalculator.evaluate(blocks, ZonedDateTime.now())
                    _uiState.update { it.copy(activeRoutineState = evaluated) }
                    delay(1000L)
                }
            }
        }
    }

    private fun subscribeToSelectedWidget(appWidgetId: Int) {
        widgetSubscriptionJob?.cancel()
        widgetSubscriptionJob = viewModelScope.launch {
            combine(
                repository.observeStyleForWidget(appWidgetId),
                repository.observeBlocksForWidget(appWidgetId)
            ) { style, blocks ->
                style to blocks
            }.collectLatest { (style, blocks) ->
                val resolvedStyle = style ?: repository.getOrCreateWidgetStyle(appWidgetId)
                val evaluated = RoutineCalculator.evaluate(blocks, ZonedDateTime.now())
                _uiState.update {
                    it.copy(
                        selectedWidgetId = appWidgetId,
                        currentStyle = resolvedStyle,
                        currentBlocks = blocks,
                        activeRoutineState = evaluated
                    )
                }
            }
        }
    }

    fun selectWidgetProfile(appWidgetId: Int) {
        if (appWidgetId == _uiState.value.selectedWidgetId) return
        undoStack.clear()
        redoStack.clear()
        updateUndoRedoFlags()
        viewModelScope.launch {
            repository.getOrCreateWidgetStyle(appWidgetId)
            subscribeToSelectedWidget(appWidgetId)
        }
    }

    fun createNewProfile(name: String) {
        viewModelScope.launch {
            val newId = repository.createNewStudioProfile(name.ifBlank { "Custom Widget" })
            selectWidgetProfile(newId)
            showBanner("Created '$name'")
        }
    }

    fun selectTab(tab: StudioTab) {
        _uiState.update { it.copy(selectedTab = tab) }
    }

    fun cycleWallpaper() {
        _uiState.update { it.copy(selectedWallpaperIndex = (it.selectedWallpaperIndex + 1) % 5) }
    }

    fun toggleCanvasGrid() {
        _uiState.update { it.copy(showCanvasGrid = !it.showCanvasGrid) }
    }

    fun setShowBatteryTipDialog(show: Boolean) {
        _uiState.update { it.copy(showBatteryTipDialog = show) }
    }

    fun refreshExactAlarmStatus() {
        val permitted = RoutineAlarmScheduler.canScheduleExactAlarms(getApplication())
        _uiState.update { it.copy(exactAlarmPermitted = permitted) }
    }

    private fun recordSnapshotForUndo() {
        if (isApplyingUndoRedo) return
        val current = UndoSnapshot(
            style = _uiState.value.currentStyle,
            blocks = _uiState.value.currentBlocks
        )
        if (undoStack.lastOrNull() != current) {
            undoStack.addLast(current)
            if (undoStack.size > 40) {
                undoStack.removeFirst()
            }
            redoStack.clear()
            updateUndoRedoFlags()
        }
    }

    private fun updateUndoRedoFlags() {
        _uiState.update {
            it.copy(
                canUndo = undoStack.isNotEmpty(),
                canRedo = redoStack.isNotEmpty()
            )
        }
    }

    fun undo() {
        if (undoStack.isEmpty()) return
        val previous = undoStack.removeLast()
        val current = UndoSnapshot(
            style = _uiState.value.currentStyle,
            blocks = _uiState.value.currentBlocks
        )
        redoStack.addLast(current)
        updateUndoRedoFlags()
        applySnapshot(previous, "Undid last change")
    }

    fun redo() {
        if (redoStack.isEmpty()) return
        val next = redoStack.removeLast()
        val current = UndoSnapshot(
            style = _uiState.value.currentStyle,
            blocks = _uiState.value.currentBlocks
        )
        undoStack.addLast(current)
        updateUndoRedoFlags()
        applySnapshot(next, "Redid change")
    }

    private fun applySnapshot(snapshot: UndoSnapshot, message: String) {
        isApplyingUndoRedo = true
        viewModelScope.launch {
            try {
                repository.saveStyle(snapshot.style)
                repository.replaceBlocksForWidget(snapshot.style.appWidgetId, snapshot.blocks)
                syncWidgetAndAlarms(snapshot.style.appWidgetId)
                showBanner(message)
            } finally {
                isApplyingUndoRedo = false
            }
        }
    }

    fun updateStyle(
        recordUndo: Boolean = true,
        transform: (WidgetStyleEntity) -> WidgetStyleEntity
    ) {
        if (recordUndo) {
            recordSnapshotForUndo()
        }
        val updated = transform(_uiState.value.currentStyle)
        _uiState.update { it.copy(currentStyle = updated) }
        viewModelScope.launch {
            repository.saveStyle(updated)
            syncWidgetAndAlarms(updated.appWidgetId)
        }
    }

    fun openNewBlockEditor() {
        val defaultStart = 9 * 60
        val defaultEnd = 10 * 60 + 30
        val draft = RoutineBlockEntity(
            id = 0L,
            appWidgetId = _uiState.value.selectedWidgetId,
            title = "Focus Session",
            startMinuteOfDay = defaultStart,
            endMinuteOfDay = defaultEnd,
            daysOfWeekMask = RoutineBlockEntity.ALL_DAYS_MASK,
            accentColorArgb = 0xFF00E5FFL
        )
        _uiState.update { it.copy(editingBlock = draft, isCreatingNewBlock = true) }
    }

    fun openEditBlockEditor(block: RoutineBlockEntity) {
        _uiState.update { it.copy(editingBlock = block, isCreatingNewBlock = false) }
    }

    fun dismissBlockEditor() {
        _uiState.update { it.copy(editingBlock = null, isCreatingNewBlock = false) }
    }

    fun saveRoutineBlock(block: RoutineBlockEntity) {
        recordSnapshotForUndo()
        dismissBlockEditor()
        viewModelScope.launch {
            val cleanTitle = block.title.trim().ifEmpty { "Routine Block" }
            val entity = block.copy(
                appWidgetId = _uiState.value.selectedWidgetId,
                title = cleanTitle,
                startMinuteOfDay = ((block.startMinuteOfDay % 1440) + 1440) % 1440,
                endMinuteOfDay = ((block.endMinuteOfDay % 1440) + 1440) % 1440
            )
            repository.saveBlock(entity)
            syncWidgetAndAlarms(entity.appWidgetId)
            showBanner("Saved '${entity.title}'")
        }
    }

    fun shiftBlockMinutesOnTimeline(
        block: RoutineBlockEntity,
        deltaMinutes: Int,
        commitToUndo: Boolean
    ) {
        if (commitToUndo) {
            recordSnapshotForUndo()
        }
        val newStart = ((block.startMinuteOfDay + deltaMinutes) % 1440 + 1440) % 1440
        val newEnd = ((block.endMinuteOfDay + deltaMinutes) % 1440 + 1440) % 1440
        val updated = block.copy(startMinuteOfDay = newStart, endMinuteOfDay = newEnd)
        viewModelScope.launch {
            repository.saveBlock(updated)
            syncWidgetAndAlarms(updated.appWidgetId)
        }
    }

    fun deleteRoutineBlock(blockId: Long) {
        recordSnapshotForUndo()
        dismissBlockEditor()
        viewModelScope.launch {
            repository.deleteBlock(blockId)
            syncWidgetAndAlarms(_uiState.value.selectedWidgetId)
            showBanner("Removed routine block")
        }
    }

    fun applyStylePreset(preset: StylePresetEntity) {
        recordSnapshotForUndo()
        val current = _uiState.value.currentStyle
        val merged = preset.styleSnapshot.copy(
            appWidgetId = current.appWidgetId,
            widgetName = current.widgetName,
            use24HourFormat = current.use24HourFormat,
            idleTextMode = current.idleTextMode,
            customIdleText = current.customIdleText,
            showProgressBar = current.showProgressBar
        )
        viewModelScope.launch {
            repository.saveStyle(merged)
            syncWidgetAndAlarms(merged.appWidgetId)
            showBanner("Applied '${preset.name}'")
        }
    }

    fun saveCurrentStyleAsPreset(name: String) {
        val cleanName = name.trim().ifEmpty { "Custom Glass Preset" }
        viewModelScope.launch {
            repository.saveUserPreset(cleanName, _uiState.value.currentStyle)
            showBanner("Saved preset '$cleanName'")
        }
    }

    fun duplicateStylePreset(preset: StylePresetEntity) {
        viewModelScope.launch {
            repository.duplicatePreset(preset)
            showBanner("Duplicated '${preset.name}'")
        }
    }

    fun deleteUserStylePreset(preset: StylePresetEntity) {
        if (preset.isBuiltIn) return
        viewModelScope.launch {
            repository.deleteUserPreset(preset.id)
            showBanner("Deleted '${preset.name}'")
        }
    }

    fun forceRefreshAllWidgets() {
        viewModelScope.launch {
            RoutineWidgetUpdater.updateAllWidgets(getApplication())
            showBanner("Synced all home-screen widgets")
        }
    }

    private suspend fun syncWidgetAndAlarms(appWidgetId: Int) {
        val app = getApplication<Application>()
        RoutineWidgetUpdater.updateSingleWidget(app, appWidgetId)
        RoutineWidgetUpdater.updateAllWidgets(app)
    }

    private fun showBanner(message: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(statusBannerMessage = message) }
            delay(2200L)
            _uiState.update { state ->
                if (state.statusBannerMessage == message) {
                    state.copy(statusBannerMessage = null)
                } else {
                    state
                }
            }
        }
    }
}
