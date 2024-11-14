//AI STATEMENT: AI was not used in this. This file was imported from https://github.com/MyScript/interactive-ink-examples-android
//into this Android SDK and then edited by our team

package com.myscript.iink.demo.domain

import android.content.Context
import android.graphics.Typeface
import android.util.Log
import androidx.annotation.VisibleForTesting
import com.myscript.iink.ContentBlock
import com.myscript.iink.ContentPart
import com.myscript.iink.ContentSelection
import com.myscript.iink.Editor
import com.myscript.iink.EditorError
import com.myscript.iink.IEditorListener
import com.myscript.iink.MimeType
import com.myscript.iink.PointerTool
import com.myscript.iink.PointerType
import com.myscript.iink.TextFormat
import com.myscript.iink.demo.data.IContentRepository
import com.myscript.iink.demo.data.ToolRepository
import com.myscript.iink.demo.ui.PartState
import com.myscript.iink.demo.ui.androidColor
import com.myscript.iink.demo.ui.iinkColor
import com.myscript.iink.demo.util.autoCloseable
import com.myscript.iink.graphics.Point
import com.myscript.iink.uireferenceimplementation.Canvas
import com.myscript.iink.uireferenceimplementation.ContextualActions
import com.myscript.iink.uireferenceimplementation.ContextualActionsHelper
import com.myscript.iink.uireferenceimplementation.ImageLoader
import com.myscript.iink.uireferenceimplementation.ImagePainter
import com.myscript.iink.uireferenceimplementation.InputController
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.util.Locale
import com.myscript.iink.graphics.Color as IInkColor


enum class PartType(private val stringValue: String) {
    TextDocument("Text Document");

    override fun toString(): String = stringValue

    companion object {
        fun fromString(value: String): PartType? {
            return entries.firstOrNull { it.stringValue == value }
        }
    }
}

enum class ToolType {
    PEN, ERASER
}


val ToolType.storageKey: String
    get() = when (this) {
        ToolType.PEN -> "pen"
        ToolType.ERASER -> "eraser"
    }

enum class PenBrush(val styleValue: String) {
    FELT_PEN("FeltPen"),
    FOUNTAIN_PEN("FountainPen"),
    CALLIGRAPHIC_BRUSH("CalligraphicBrush"),
    PENCIL("Extra-Pencil");

    companion object {
        fun fromStyleValue(value: String): PenBrush? {
            return entries.firstOrNull { it.styleValue == value }
        }
    }
}

enum class BlockType {
    Text;

    override fun toString(): String {
        return when (this) {
            Text -> "Text"
        }
    }

    companion object {
        fun fromString(value: String): BlockType? {
            return when (value) {
                "Text" -> Text
                else -> null
            }
        }
    }
}

enum class MenuAction {
    COPY,
    PASTE,
    DELETE,
    //CONVERT,
    EXPORT,
    ADD_BLOCK,
    FORMAT_TEXT,
    FORMAT_TEXT_H1,
    FORMAT_TEXT_H2,
    FORMAT_TEXT_PARAGRAPH,
    FORMAT_TEXT_LIST_BULLET,
    FORMAT_TEXT_LIST_CHECKBOX,
    FORMAT_TEXT_LIST_NUMBERED
}

data class PredictionSettings(val enabled: Boolean = false, val durationMs: Int = 0)

class PartEditor(
    private val typefaces: Map<String, Typeface>,
    private val theme: String,
    private val contentRepository: IContentRepository,
    private val toolRepository: ToolRepository,
    private var extraBrushConfigs: List<Canvas.ExtraBrushConfig> = emptyList(),
    private val mainDispatcher: CoroutineDispatcher = Dispatchers.Main,
    private val workDispatcher: CoroutineDispatcher = Dispatchers.Default
) {
    interface Listener {
        fun updateUndoRedoState(canUndo: Boolean, canRedo: Boolean)
        fun updatePartNavigationState(hasPrevious: Boolean, hasNext: Boolean)
        fun partLoaded(partId: String, partType: PartType)
        fun partLoading(partId: String)
        fun partLoadingError(partId: String, exception: Exception)
        fun editorError(blockId: String, error: EditorError, message: String)
        fun toolChanged(
            toolType: ToolType?,
            iinkColor: IInkColor,
            thickness: Float,
            penBrush: PenBrush?
        )

        fun colorChanged(toolType: ToolType, iinkColor: IInkColor?)
        fun thicknessChanged(toolType: ToolType, thickness: Float?)
        fun penBrushChanged(toolType: ToolType, penBrush: PenBrush)
        fun updateToolState(partType: PartType, toolType: ToolType, enableActivePen: Boolean)
        fun actionError(exception: Exception, blockId: String?)
    }

    var selectedTool: ToolType? = null
        private set

    private val parentJob = Job()
    private val scope = CoroutineScope(parentJob + mainDispatcher)

    @VisibleForTesting
    var editor by autoCloseable<Editor>(null) { oldEditor ->
        if (oldEditor != null) {
            oldEditor.removeListener(editorListener)
            oldEditor.part = null
            oldEditor.renderer.close()
        }
    }
    private var currentPart by autoCloseable<ContentPart>()
    private val currentPartType: PartType?
        get() = currentPart?.type?.let(PartType::fromString)
    private var currentIndex: Int = -1
    private var listener: Listener? = null
    private var allParts: List<String> = emptyList()
    var isActivePenEnabled: Boolean = false
    var inputController: InputController? = null

    private val editorListener: IEditorListener = object : IEditorListener {
        override fun partChanging(editor: Editor, oldPart: ContentPart?, newPart: ContentPart?) =
            Unit

        override fun partChanged(editor: Editor) {
            notifyState()
        }

        override fun contentChanged(editor: Editor, blockIds: Array<out String>) {
            notifyUndoRedoState()
        }

        override fun onError(editor: Editor, blockId: String, err: EditorError, message: String) {
            notifyError(blockId, err, message)
        }

        override fun selectionChanged(editor: Editor) {
             }

        override fun activeBlockChanged(editor: Editor, blockId: String) {
            CurrentBlockId.priorBlockID = CurrentBlockId.currentBlockID
            CurrentBlockId.currentBlockID = blockId
        }
    }

    private fun notifyUndoRedoState() {
        val canUndo = editor?.canUndo() ?: false
        val canRedo = editor?.canRedo() ?: false
        scope.launch(mainDispatcher) {
            listener?.updateUndoRedoState(canUndo, canRedo)
        }
    }

    private fun notifyState() {
        scope.launch(mainDispatcher) {
            val index = currentIndex
            listener?.updatePartNavigationState(
                hasPrevious = index > 0,
                hasNext = index < allParts.lastIndex
            )
            val partType = currentPartType
            if (partType != null) {
                selectedTool?.let { listener?.updateToolState(partType, it, isActivePenEnabled) }
            }
            notifyUndoRedoState()
        }
    }

    private fun notifyError(blockId: String, err: EditorError, message: String) {
        scope.launch(mainDispatcher) {
            listener?.editorError(blockId, err, message)
        }
    }

    fun saveCurrentPart() {
        if (currentIndex in allParts.indices) {
            val partId = allParts[currentIndex]
            contentRepository.savePart(partId)
        }
    }

    fun setListener(stateListener: Listener?) {
        this.listener = stateListener
    }

    fun setEditor(editor: Editor?, inputController: InputController?) {
        if (editor != null) {
            // configure multithreading for text recognition
            editor.configuration.setNumber("max-recognition-thread-count", 1)

            editor.addListener(editorListener)
            editor.theme = theme
            // full interactivity mode in Raw Content is enabled by several features:
            // gestures, conversion, smart eraser, shape & image rotation etc.
            editor.configuration.enableRawContentInteractivity()
            // also allow shape rotation in diagram parts
            editor.configuration.setStringArray("diagram.rotation", arrayOf("shape"))
            this.inputController = inputController
            editor.part = currentPart
        }
        this.editor = editor
        selectedTool?.let { changeTool(it) }
    }

    fun closeEditor() {
        inputController = null
        editor = null
    }

    fun lastChosenPartType(): PartType? {
        return contentRepository.lastChosenPartType?.let(PartType::fromString)
    }

    fun getPartTypes(): List<PartType> {
        return contentRepository.requestPartTypes().mapNotNull(PartType::fromString)
    }

    fun getExportMimeTypes(): List<MimeType> {
        return editor?.getSupportedExportMimeTypes(null)?.toList() ?: emptyList()
    }

    fun exportContent(
        mimeType: MimeType,
        x: Float?,
        y: Float?,
        selectedBlockId: String?,
        outputFile: File
    ) {
        editor?.let { editor ->
            val content = when {
                selectedBlockId != null -> editor.getBlockById(selectedBlockId)
                x != null && y != null -> editor.hitBlock(x, y)
                else -> null
            }
            val imagePainter = ImagePainter(extraBrushConfigs).apply {
                setImageLoader(ImageLoader(editor))
                setTypefaceMap(typefaces)
            }
            editor.waitForIdle()
            outputFile.parentFile?.mkdirs()
            editor.export_(content, outputFile.absolutePath, mimeType, imagePainter)
        }
    }

    fun copyPart(partId: String, outputDir: File): File {
        return contentRepository.copyPart(partId, outputDir)
    }

    fun loadParts() {
        val parts = contentRepository.allParts
        allParts = parts
        currentIndex = if (parts.isNotEmpty()) {
            // gracefully fallback to first part if last part id can't be found among available parts
            val lastPartId = contentRepository.lastOpenedPartId
            val index = parts.indexOf(lastPartId)
            if (index == -1) 0 else index
        } else {
            -1
        }
        if (parts.isNotEmpty() && currentIndex in parts.indices) {
            openPart(parts[currentIndex])
        } else {
            notifyState()
        }
    }

    fun createPart(partType: PartType): String {
        val partId = contentRepository.createPart(partType.toString())
        allParts = contentRepository.allParts
        contentRepository.lastChosenPartType = partType.toString()
        return partId
    }

    fun openPart(partId: String) {
        val index = allParts.indexOf(partId)
        if (index == -1) return
        scope.launch(mainDispatcher) {
            listener?.partLoading(partId)
            try {
                val contentPart = withContext(workDispatcher) {
                    contentRepository.getPart(partId)
                }

                currentPart = contentPart
                currentIndex = index
                contentRepository.lastOpenedPartId = partId

                editor?.part = contentPart
                editor?.renderer?.setViewOffset(0f, 0f)
                editor?.renderer?.viewScale = 1f

                changeTool(ToolType.PEN)

                notifyState()
                val partType = checkNotNull(currentPartType)
                listener?.partLoaded(partId, partType)
            } catch (e: Exception) {
                listener?.partLoadingError(partId, e)
            }
        }
    }


    fun getPart(partState: PartState) {
        if (partState != null) {
            partState.partId?.let { openPart(it) }
        }
    }

    fun previousPart() {
        if (currentIndex <= 0) return
        val partId = allParts[currentIndex - 1]
        openPart(partId)
    }

    fun nextPart() {
        if (currentIndex >= allParts.lastIndex) return
        val partId = allParts[currentIndex + 1]
        openPart(partId)
    }

    // Toolbar
    fun enableActivePen(enableActivePen: Boolean) {
        isActivePenEnabled = enableActivePen
        if (isActivePenEnabled) {
            selectedTool = ToolType.PEN
        }
        selectedTool?.let { changeTool(it) }

        val partType = currentPartType
        if (partType != null) {
            selectedTool?.let { listener?.updateToolState(partType, it, isActivePenEnabled) }
        }
    }

    private fun getToolColor(toolType: ToolType?): IInkColor {
        return toolRepository.getToolColor(toolType?.storageKey ?: "").iinkColor
    }

    private fun getToolThickness(toolType: ToolType?): Float {
        return toolRepository.getToolThickness(toolType?.storageKey ?: "")
    }

    private fun getPenBrush(toolType: ToolType?): PenBrush? {
        val style = toolRepository.getPenBrush(toolType?.storageKey ?: "")
        return style?.let(PenBrush.Companion::fromStyleValue)
    }

    fun changeColor(iinkColor: IInkColor) {
        val tool = selectedTool ?: return
        if (setToolStyle(tool, iinkColor, getToolThickness(tool), getPenBrush(tool))) {
            toolRepository.saveToolColor(tool.storageKey, iinkColor.androidColor)
            scope.launch(mainDispatcher) {
                listener?.colorChanged(tool, iinkColor)
            }
        }
    }

    fun changeThickness(thickness: Float) {
        val tool = selectedTool ?: return
        if (setToolStyle(tool, getToolColor(tool), thickness, getPenBrush(tool))) {
            toolRepository.saveToolThickness(tool.storageKey, thickness)
            scope.launch(mainDispatcher) {
                listener?.thicknessChanged(tool, thickness)
            }
        }
    }

    fun changeTool(tool: ToolType) {
        val iinkColor = getToolColor(tool)
        val thickness = getToolThickness(tool)
        val brush = getPenBrush(tool)
        if (setToolStyle(tool, iinkColor, thickness, brush)) {
            selectedTool = tool
            scope.launch(mainDispatcher) {
                listener?.toolChanged(tool, iinkColor, thickness, brush)
            }
        }
    }

    fun changePenBrush(penBrush: PenBrush) {
        val tool = selectedTool ?: return
        if (tool != ToolType.PEN) return

        if (setToolStyle(tool, getToolColor(tool), getToolThickness(tool), penBrush)) {
            toolRepository.savePenBrush(tool.storageKey, penBrush.styleValue)
            scope.launch(mainDispatcher) {
                listener?.penBrushChanged(tool, penBrush)
            }
        }
    }

    private fun setToolStyle(
        toolType: ToolType,
        iinkColor: IInkColor,
        thickness: Float,
        penBrush: PenBrush?
    ): Boolean {
        val editor = editor ?: return true

        val colorValue = "#%08X".format(0xFFFFFFFF and iinkColor.rgba.toLong())
        // force Locale.US to ensure dotted float formatting (`1.88` instead of `1,88`) whatever the device's locale setup
        val thicknessValue = "%.2f".format(Locale.US, thickness)
        var style = "color: $colorValue; -myscript-pen-width: $thicknessValue;"
        if (penBrush != null) {
            style += "-myscript-pen-brush: ${penBrush.styleValue};"
        }

        val pointerTool = toolType.toPointerTool()
        val pointerType = pointerType(pointerTool)

        return try {
            editor.toolController.setToolStyle(pointerTool, style)
            editor.toolController.setToolForType(pointerType, pointerTool)
            true
        } catch (e: IllegalStateException) {
            // a pointer event sequence is in progress, not allowed to re-configure or change tool
            false
        }
    }

    private fun pointerType(pointerTool: PointerTool): PointerType {
        return if (isActivePenEnabled) {
            inputController?.inputMode = InputController.INPUT_MODE_AUTO
            if (pointerTool == PointerTool.HAND) PointerType.TOUCH else PointerType.PEN
        } else {
            inputController?.inputMode =
                if (pointerTool == PointerTool.HAND) InputController.INPUT_MODE_FORCE_TOUCH else InputController.INPUT_MODE_FORCE_PEN
            PointerType.PEN
        }
    }

    // Selection & contextual menu
    fun getAddBlockActions(): List<BlockType> {
        val editor = editor ?: return emptyList()

        return editor.supportedAddBlockTypes.mapNotNull(BlockType::fromString)
    }

    fun getMenuActions(x: Float, y: Float): List<Unit> {
        val editor = editor ?: return emptyList()

        // priority chain to choose the right subject (ContentSelection > ContentBlock > root ContentBlock)
        val content = editor.hitSelection(x, y) ?: editor.hitBlock(x, y) ?: editor.rootBlock
        val actions = when (content) {
            is ContentBlock -> ContextualActionsHelper.getAvailableActionsForBlock(editor, content)
            else -> ContextualActionsHelper.getAvailableActionsForSelection(editor, content)
        }
        return actions.mapNotNull(ContextualActions::toMenuAction).toList()
    }

    fun getMenuActions(contentBlockId: String): List<Unit> {
        val editor = editor ?: return emptyList()

        return editor.getBlockById(contentBlockId)?.use { block ->
            val actions = ContextualActionsHelper.getAvailableActionsForBlock(editor, block)
            return actions.mapNotNull(ContextualActions::toMenuAction).toList()
        } ?: emptyList()
    }

    fun getFormatTextActions(x: Float, y: Float, selectedBlockId: String?): List<MenuAction> {
        val editor = editor ?: return emptyList()

        val content = when {
            editor.hasSelection() -> editor.selection
            selectedBlockId != null -> editor.getBlockById(selectedBlockId)
            else -> editor.hitBlock(x, y)
        }
        return content?.use {
            editor.getSupportedTextFormats(it).mapNotNull(TextFormat::toMenuAction).toList()
        } ?: emptyList()
    }

    fun getExportActions(x: Float, y: Float, selectedBlockId: String?): List<MimeType> {
        val editor = editor ?: return emptyList()

        val content = when {
            editor.hasSelection() -> editor.selection
            selectedBlockId != null -> editor.getBlockById(selectedBlockId)
            else -> editor.hitBlock(x, y) ?: editor.rootBlock
        }
        return content.use {
            editor.getSupportedExportMimeTypes(it).toList()
        }
    }

    fun applyActionMenu(x: Float, y: Float, action: MenuAction, selectedBlockId: String?) {
        val editor = editor ?: return

        val content = when {
            editor.hasSelection() -> editor.selection
            selectedBlockId != null -> editor.getBlockById(selectedBlockId)
            else -> editor.hitBlock(x, y)
        }

        try {
            when (action) {
//                MenuAction.COPY -> editor.copy(content)
//                MenuAction.PASTE -> editor.paste(x, y)
//                MenuAction.DELETE -> if (content != null) editor.erase(content)
//                //MenuAction.CONVERT -> convertContent(content)
//                MenuAction.EXPORT -> {}
//                MenuAction.ADD_BLOCK -> {}
//                MenuAction.FORMAT_TEXT -> {}
//                MenuAction.FORMAT_TEXT_H1 -> if (content != null) editor.setTextFormat(
//                    content,
//                    TextFormat.H1
//                )
//
//                MenuAction.FORMAT_TEXT_H2 -> if (content != null) editor.setTextFormat(
//                    content,
//                    TextFormat.H2
//                )
//
//                MenuAction.FORMAT_TEXT_PARAGRAPH -> if (content != null) editor.setTextFormat(
//                    content,
//                    TextFormat.PARAGRAPH
//                )
//
//                MenuAction.FORMAT_TEXT_LIST_BULLET -> if (content != null) editor.setTextFormat(
//                    content,
//                    TextFormat.LIST_BULLET
//                )
//
//                MenuAction.FORMAT_TEXT_LIST_CHECKBOX -> if (content != null) editor.setTextFormat(
//                    content,
//                    TextFormat.LIST_CHECKBOX
//                )
//
//                MenuAction.FORMAT_TEXT_LIST_NUMBERED -> if (content != null) editor.setTextFormat(
//                    content,
//                    TextFormat.LIST_NUMBERED
//                )
                else -> {}
            }
        } catch (e: Exception) {
            listener?.actionError(e, (content as? ContentBlock)?.id)
        } finally {
            content?.close()
        }
    }

    fun addBlock(x: Float, y: Float, blockType: BlockType) {
        editor?.addBlock(x, y, blockType.toString())?.also(ContentBlock::close)
    }

    fun addImage(imageFile: File, mimeType: MimeType) {
        editor?.let { editor ->
            val point = editor.newBlockScreenPosition()
            addImageAt(point.x, point.y, imageFile, mimeType)
        }
    }

    fun addImageAt(x: Float, y: Float, imageFile: File, mimeType: MimeType) {
        if (!imageFile.isFile) return
        require(mimeType == MimeType.PNG || mimeType == MimeType.JPEG) { "Unsupported mime type: $mimeType" }
        editor?.addImage(x, y, imageFile, mimeType)?.also(ContentBlock::close)
    }

    fun insertText(x: Float, y: Float, text: String) {
        editor?.addBlock(x, y, BlockType.Text.toString(), MimeType.TEXT, text)
            ?.also(ContentBlock::close)
    }

    fun zoomIn() {
        editor?.renderer?.zoom(110.0f / 100.0f)
    }

    fun zoomOut() {
        editor?.renderer?.zoom(100.0f / 110.0f)
    }

    fun resetView() {
        val renderer = editor?.renderer ?: return
        renderer.setViewOffset(0f, 0f)
        renderer.viewScale = 1f
    }

    fun undo() {
        editor?.undo()
    }

    fun redo() {
        editor?.redo()
    }

    fun clearContent() {
        editor?.clear()
    }

    fun convertContent(content: ContentSelection? = null) {
        val conversionState = editor?.getSupportedTargetConversionStates(content)

        Log.d("onError", CurrentBlockId.onError.toString())

        if(CurrentBlockId.onError){
            return
        }

        if (!conversionState.isNullOrEmpty()) {
            if (content is ContentBlock) {
                val originalBlock = content
                val transform = editor?.renderer?.viewTransform
                val topLeft = transform?.apply(originalBlock.box.x, originalBlock.box.y)
                val x = topLeft?.x
                val y = topLeft?.y

                val savedString: String? = editor?.export_(originalBlock, MimeType.JIIX)
                val jsonObject = JSONObject(savedString)

                val mainLabel = jsonObject.getString("label")

                if (savedString != null) {
                    Log.d("converttextt", mainLabel)
                }
                editor?.erase(originalBlock)
                editor?.waitForIdle()

                if (x != null && y != null) {
                    if (!mainLabel.contains("☐"))
                        this.insertText(x, y, "☐$mainLabel")
                    else
                        this.insertText(x, y, mainLabel)
                }
            } else {
                if (conversionState != null) {
                    var idBlock: String = null.toString()
                    if(CurrentBlockId.currentBlockID != null){
                        if (CurrentBlockId.currentBlockID == "Default Value" || CurrentBlockId.currentBlockID == ""){
                            idBlock = CurrentBlockId.priorBlockID
                        } else{
                            idBlock = CurrentBlockId.currentBlockID
                        }
                    } else if (CurrentBlockId.priorBlockID != null){
                        idBlock = CurrentBlockId.priorBlockID
                    } else {
                        return
                    }
                    Log.d("prior", CurrentBlockId.priorBlockID)
                    Log.d("cur", CurrentBlockId.currentBlockID)
                    Log.d("idblock", idBlock)


                    if(editor?.getBlockById(idBlock) == null){
                        return
                    }
                    val originalBlock = editor?.getBlockById(idBlock) as ContentBlock

                    val transform = editor?.renderer?.viewTransform
                    val topLeft = transform?.apply(originalBlock.box.x, originalBlock.box.y)
                    val x = topLeft?.x
                    val y = topLeft?.y

                    val savedString: String? = editor?.export_(originalBlock, MimeType.JIIX)
                    val jsonObject = JSONObject(savedString)

                    val mainLabel = jsonObject.getString("label")

                    if (savedString != null) {
                        Log.d("converttextt", mainLabel)
                    }


                    try {
                        editor?.erase(originalBlock as ContentSelection)
                        editor?.waitForIdle()
                        if (x != null && y != null) {
                            if (!mainLabel.contains("☐"))
                                this.insertText(x, y, "☐$mainLabel")
                            else
                                this.insertText(x, y, mainLabel)
                        }
                    } catch (e: Exception){
                        return
                    }
                }
            }
        }
    }


    fun waitForIdle() {
        editor?.waitForIdle()
    }

    fun closePart() {
        currentPart = null
    }

    fun getTools(partType: PartType, enableActivePen: Boolean): Map<ToolType, Boolean> {
        return partType.availableTools(ToolType.entries, enableActivePen)
    }

    fun importContent(file: File, onResult: (file: File, partIds: List<String>, exception: Exception?) -> Unit) {
        scope.launch(mainDispatcher) {
            val (partIds, exception) = withContext(Dispatchers.IO) {
                try {
                    val ids = contentRepository.importContent(file)
                    allParts = contentRepository.allParts
                    ids to null
                } catch (e: Exception) {
                    emptyList<String>() to e
                }
            }
            onResult(file, partIds, exception)
        }
    }

    fun getPredictionSettings(): PredictionSettings {
        return editor?.let { editor ->
            val enabled = editor.engine.configuration.getBoolean("renderer.prediction.enable", false)
            val durationMs = editor.engine.configuration.getNumber("renderer.prediction.duration", 0)
            PredictionSettings(enabled, durationMs.toInt())
        } ?: PredictionSettings()
    }

    fun changePredictionSettings(enable: Boolean, durationMs: Int) {
        editor?.let { editor ->
            editor.engine.configuration.setBoolean("renderer.prediction.enable", enable)
            editor.engine.configuration.setNumber("renderer.prediction.duration", durationMs)
        }
    }
}

private fun TextFormat.toMenuAction(): MenuAction = when (this) {
    TextFormat.H1 -> MenuAction.FORMAT_TEXT_H1
    TextFormat.H2 -> MenuAction.FORMAT_TEXT_H2
    TextFormat.PARAGRAPH -> MenuAction.FORMAT_TEXT_PARAGRAPH
    TextFormat.LIST_BULLET -> MenuAction.FORMAT_TEXT_LIST_BULLET
    TextFormat.LIST_CHECKBOX -> MenuAction.FORMAT_TEXT_LIST_CHECKBOX
    TextFormat.LIST_NUMBERED -> MenuAction.FORMAT_TEXT_LIST_NUMBERED
}

private fun ContextualActions.toMenuAction() {
    when (this) {
//    ContextualActions.COPY -> MenuAction.COPY
//    //ContextualActions.CONVERT -> MenuAction.CONVERT
//    ContextualActions.REMOVE -> MenuAction.DELETE
//    ContextualActions.EXPORT -> MenuAction.EXPORT
//    ContextualActions.ADD_BLOCK -> MenuAction.ADD_BLOCK
//    ContextualActions.PASTE -> MenuAction.PASTE
//    ContextualActions.FORMAT_TEXT -> MenuAction.FORMAT_TEXT
        else -> {return;}
    }
}

private fun ToolType.toPointerTool(): PointerTool = when (this) {
    ToolType.PEN -> PointerTool.PEN
    ToolType.ERASER -> PointerTool.ERASER
}

private fun Editor.newBlockScreenPosition(): Point {
    // in document coordinates
    val box = this.rootBlock?.box ?: return Point(0f, 0f)
    // add 10mm to ensure to be "on the next line"
    val yMax = box.y + box.height + 10
    // convert in screen coordinates expected when providing coordinates to iink APIs
    return renderer.viewTransform?.apply(box.x, yMax) ?: Point(0f, 0f)
}

/**
 * Define the list of available tools depending on the part's type and active pen mode (stylus vs touch).
 */
private fun PartType.availableTools(tools: List<ToolType>, enableActivePen: Boolean): Map<ToolType, Boolean> {
    val toolPen = tools.first { it == ToolType.PEN }
    val toolEraser = tools.first { it == ToolType.ERASER }

    return when (this) {
        PartType.TextDocument -> mapOf(
            toolPen to true,
            toolEraser to true
        )
    }
}
