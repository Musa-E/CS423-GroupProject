//AI STATEMENT: AI was not used in this. This file was imported from https://github.com/MyScript/interactive-ink-examples-android
//into this Android SDK and then edited by our team

package com.myscript.iink.demo

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.PointF
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.GestureDetector
import android.view.Menu
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.annotation.DimenRes
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ShareCompat
import androidx.core.content.FileProvider
import androidx.core.view.isVisible
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.coroutineScope
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.snackbar.Snackbar
import com.myscript.iink.Editor
import com.myscript.iink.MimeType
import com.myscript.iink.demo.databinding.MainActivityBinding
import com.myscript.iink.demo.di.EditorViewModelFactory
import com.myscript.iink.demo.domain.BlockType
import com.myscript.iink.demo.domain.MenuAction
import com.myscript.iink.demo.domain.PartType.Companion.fromString
import com.myscript.iink.demo.domain.PenBrush
import com.myscript.iink.demo.ui.ColorState
import com.myscript.iink.demo.ui.ColorsAdapter
import com.myscript.iink.demo.ui.ContextualActionState
import com.myscript.iink.demo.ui.EditorViewModel
import com.myscript.iink.demo.ui.Error
import com.myscript.iink.demo.ui.NewPartRequest
import com.myscript.iink.demo.ui.PartHistoryState
import com.myscript.iink.demo.ui.PartNavigationState
import com.myscript.iink.demo.ui.PartState
import com.myscript.iink.demo.ui.PenBrushState
import com.myscript.iink.demo.ui.ThicknessState
import com.myscript.iink.demo.ui.ThicknessesAdapter
import com.myscript.iink.demo.ui.ToolState
import com.myscript.iink.demo.ui.ToolsAdapter
import com.myscript.iink.demo.ui.primaryFileExtension
import com.myscript.iink.demo.util.launchActionChoiceDialog
import com.myscript.iink.demo.util.launchPredictionDialog
import com.myscript.iink.demo.util.launchSingleChoiceDialog
import com.myscript.iink.demo.util.launchTextBlockInputDialog
import com.myscript.iink.uireferenceimplementation.EditorView
import com.myscript.iink.uireferenceimplementation.FrameTimeEstimator
import com.myscript.iink.uireferenceimplementation.SmartGuideView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.Timer
import kotlin.math.roundToInt


//This function is for the "exporting" feature that we probably are not going to use.
//I'm keeping it because I have the "you never know" mentality.
suspend fun Context.processUriFile(uri: Uri, file: File, logic: (File) -> Unit) {
    withContext(Dispatchers.IO) {
        runCatching {
            contentResolver.openInputStream(uri)?.use { inputStream ->
                file.outputStream().use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
        }
    }
    try {
        logic(file)
    } finally {
        file.deleteOnExit()
    }
}

//menu option for the drop-down that, again, we probably are not going to use
@get:StringRes
private val MenuAction.stringRes: Int
    get() = when (this) {
        MenuAction.COPY -> R.string.editor_action_copy
        MenuAction.PASTE -> R.string.editor_action_paste
        MenuAction.DELETE -> R.string.editor_action_delete
        MenuAction.CONVERT -> R.string.editor_action_convert
        MenuAction.EXPORT -> R.string.editor_action_export
        MenuAction.ADD_BLOCK -> R.string.editor_action_add_block
        MenuAction.FORMAT_TEXT -> R.string.editor_action_format_text
        MenuAction.FORMAT_TEXT_H1 -> R.string.editor_action_format_text_as_heading1
        MenuAction.FORMAT_TEXT_H2 -> R.string.editor_action_format_text_as_heading2
        MenuAction.FORMAT_TEXT_PARAGRAPH -> R.string.editor_action_format_text_as_paragraph
        MenuAction.FORMAT_TEXT_LIST_BULLET -> R.string.editor_action_format_text_as_list_bullet
        MenuAction.FORMAT_TEXT_LIST_CHECKBOX -> R.string.editor_action_format_text_as_list_checkbox
        MenuAction.FORMAT_TEXT_LIST_NUMBERED -> R.string.editor_action_format_text_as_list_numbered
    }

// Declares all the types of pens and their names for display
// R.String just is as it sounds: holds string values that you use consistently
@get:StringRes
private val PenBrush.label: Int
    get() = when (this) {
        PenBrush.FELT_PEN -> R.string.pen_brush_felt_pen
        PenBrush.FOUNTAIN_PEN -> R.string.pen_brush_fountain_pen
        PenBrush.CALLIGRAPHIC_BRUSH -> R.string.pen_brush_calligraphic_brush
        PenBrush.PENCIL -> R.string.pen_brush_pencil_brush
    }


//Now here is the actual class, believe it or not
class MainActivity : AppCompatActivity() {
    //variables for undo/redo gestures
    private var startTime: Long = 0
    private var endTime: Long = 0
    val timer: Timer = Timer()

    private lateinit var gestureDetector: GestureDetector
    private val touchPoints = mutableListOf<PointF>()
    private var isPenActivated = false;
    private val listenerStateSaved = MutableLiveData<Boolean>()
    private var canGesture = false;

    private var officialTitle: String = null.toString() //title of page that gets added to PartState.title
    private val exportsDir: File
        get() = File(cacheDir, "exports").apply(File::mkdirs) //variable we prob wont need
    private val binding by lazy { MainActivityBinding.inflate(layoutInflater) } //we need this, just sets up UI
    private var editorView: EditorView? = null //need this, creates the editing area
    private val viewModel: EditorViewModel by viewModels { EditorViewModelFactory() } //this is the entire UI page boxed into this
    private var navigationState: PartNavigationState = PartNavigationState() //let's, for now, not touch this: holds nav states (like, past), which I sort of got rid of, but, again, you never know
    private var partState: PartState = PartState.Unloaded //global var that holds the literal object with all the stuff, never delete
    private val editorBinding = IInkApplication.DemoModule.editorBinding
    private var smartGuideView: SmartGuideView? = null
    private var toolsAdapter = ToolsAdapter { viewModel.changeTool(it) }
    private var colorsAdapter = ColorsAdapter { viewModel.changeColor(it) }
    private var thicknessesAdapter = ThicknessesAdapter { viewModel.changeThickness(it) }
    private val penBrushesAdapter by lazy {
        ArrayAdapter<String>(this, R.layout.toolbar_pen_brush_row, R.id.toolbar_pen_brush_row_label)
    }
    //listens for pen brush to be changed
    private val penBrushSelectedListener = object : AdapterView.OnItemSelectedListener {
        override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
            val penBrushLabel = penBrushesAdapter.getItem(position) ?: return
            val penBrush = when (penBrushLabel) {
                getString(R.string.pen_brush_felt_pen) -> PenBrush.FELT_PEN
                getString(R.string.pen_brush_fountain_pen) -> PenBrush.FOUNTAIN_PEN
                getString(R.string.pen_brush_calligraphic_brush) -> PenBrush.CALLIGRAPHIC_BRUSH
                getString(R.string.pen_brush_pencil_brush) -> PenBrush.PENCIL
                else -> null
            }
            if (penBrush != null) {
                viewModel.changePenBrush(PenBrushState(penBrush, true))
            }
        }

        override fun onNothingSelected(parent: AdapterView<*>?) = Unit
    }

    private companion object {
        const val EnableCapturePredictionByDefault: Boolean = true
        const val DefaultMinimumPredictionDurationMs: Int = 16 // 1 frame @60Hz, 2 frames @120Hz
    }

    private val onSmartGuideMenuAction = SmartGuideView.MenuListener { x, y, blockId ->
        val actionState = viewModel.requestSmartGuideActions(x, y, blockId)
        showContextualActionDialog(actionState, blockId)
    }

    private val onBottomSheetStateChanged = object : BottomSheetBehavior.BottomSheetCallback() {
        @SuppressLint("SwitchIntDef")
        override fun onStateChanged(bottomSheet: View, newState: Int) {
            when (newState) {
                BottomSheetBehavior.STATE_COLLAPSED -> viewModel.expandColorPalette(false)
                BottomSheetBehavior.STATE_EXPANDED -> viewModel.expandColorPalette(true)
            }
        }
        override fun onSlide(bottomSheet: View, slideOffset: Float) = Unit
    }

    //Now this is something I don't believe we will need. This is just importing an object, if wanted
    private val importIInkFileRequest = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri == null) return@registerForActivityResult
        val mimeType = DocumentFile.fromSingleUri(this@MainActivity, uri)?.type ?: contentResolver.getType(uri)
        when (mimeType) {
            "binary/octet-stream",
            "application/zip",
            "application/octet-stream",
            "application/binary",
            "application/x-zip" -> lifecycle.coroutineScope.launch {
                processUriFile(uri, File(cacheDir, "import.iink")) { file ->
                    viewModel.importContent(file)
                }
            }
            else -> onError(Error(
                Error.Severity.WARNING,
                getString(R.string.app_error_unsupported_file_type_title),
                getString(R.string.app_error_unsupported_iink_file_type_message, mimeType)
            ))
        }
    }

    //Here is the function that is called, I swear, when the activity is called
    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        canGesture = true

        //gestureDetector prof was walking about in class
        gestureDetector = GestureDetector(this, GestureListener())

        editorView = findViewById(com.myscript.iink.uireferenceimplementation.R.id.editor_view)
        smartGuideView = findViewById(com.myscript.iink.uireferenceimplementation.R.id.smart_guide_view)

        setSupportActionBar(binding.toolbar) //creates the bar at the top and makes it editable programmically

        //sets up the entire viewmodel, do not touch
        viewModel.enableActivePen.observe(this) { activePenEnabled ->
            binding.editorToolbar.switchActivePen.isChecked = activePenEnabled
            isPenActivated = activePenEnabled;
        }
        viewModel.error.observe(this, this::onError)
        viewModel.toolSheetExpansionState.observe(this, this::onToolSheetExpansionStateUpdate)
        viewModel.availableTools.observe(this, this::onAvailableToolsUpdate)
        viewModel.availableColors.observe(this, this::onAvailableColorsUpdate)
        viewModel.availableThicknesses.observe(this, this::onAvailableThicknessesUpdate)
        viewModel.availablePenBrushes.observe(this, this::onAvailablePenBrushesUpdate)
        viewModel.partCreationRequest.observe(this, this::onPartCreationRequest)
        viewModel.partState.observe(this, this::onPartStateUpdate)
        viewModel.partHistoryState.observe(this, this::onPartHistoryUpdate)
        viewModel.partNavigationState.observe(this, this::onPartNavigationStateUpdate)

        // Extra brushes must be set prior to editor binding
        editorView?.extraBrushConfigs = IInkApplication.DemoModule.extraBrushes
        val editorData = editorBinding.openEditor(editorView)
        editorData.inputController?.setViewListener(editorView)

        //the touch listener for the editor view to include custom one
         editorView?.setOnTouchListener { _, event ->
            editorData.inputController?.onTouch(editorView, event)
             onTouchEvent(event)
            true
        }

        //this is a handler that pauses the time before an undo; trust me, it is needed *sobs*
        listenerStateSaved.observe(this) { isSaved ->
            if (isSaved) {
                viewModel.undo();
                listenerStateSaved.value = false
            }
        }

        editorData.editor?.let { editor ->
            viewModel.setEditor(editorData)
            setMargins(editor, R.dimen.editor_horizontal_margin, R.dimen.editor_vertical_margin)
            if (savedInstanceState == null) {
                configureDefaultCaptureStrokePrediction(editorView?.context ?: this)
            }
            smartGuideView?.setEditor(editor)
        }
        smartGuideView?.setMenuListener(onSmartGuideMenuAction)
        smartGuideView?.setTypeface(IInkApplication.DemoModule.defaultTypeface)

        with(binding.editorToolbarSheet) {
            toolbarTools.adapter = toolsAdapter
            toolbarColors.itemAnimator = null
            toolbarColors.adapter = colorsAdapter
            toolbarThicknesses.adapter = thicknessesAdapter
            penBrushDropdown.adapter = penBrushesAdapter
            penBrushDropdown.onItemSelectedListener = penBrushSelectedListener
        }

        //just an error response, dont touch
        if (IInkApplication.DemoModule.engine == null) {
            // the certificate provided in `DemoModule.provideEngine` is most likely incorrect
            onError(Error(
                Error.Severity.CRITICAL,
                getString(R.string.app_error_invalid_certificate_title),
                getString(R.string.app_error_invalid_certificate_message)
            ))
        }

        //by Polly: gets the "paramters" that you sent over from "TaskListView.java" and reads them and
        //responds accordingly
        val bundle = intent.extras
        if (bundle != null) {
            //I created "blank" to mean it is a new entry; it is the title the user put in
            if(bundle.getString("blank") != null){
                officialTitle = bundle["blank"] as String; //making the title equal what user put

                viewModel.requestNewPart(); //this is the function that gets a new PartState
                partState.title = officialTitle
            } //this is if the user clicked on the listview to open up this activity
            else if (bundle.getString("partId") != null){
                val partTypeString = bundle.getString("partType")
                val partType = fromString(partTypeString!!)

                //making global partState what the user sent in from "EditorViewModel.kt"
                partState = PartState(
                    bundle["partId"] as String?,
                    (bundle["isReady"] as Boolean?)!!,
                    partType,
                    bundle["partDate"] as String,
                    bundle["partTitle"] as String
                )

                officialTitle = bundle["partTitle"] as String

                viewModel.getPart(partState) //function that gets the PartState you want to load
            }
        }
    }

    //here is our own custom touch event, which we will prob use for more gestures
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        if(isPenActivated && canGesture) {
            event?.let {
                when (it.action) {
                    //this means when the user presses on the screen
                    MotionEvent.ACTION_DOWN -> {
                        touchPoints.clear()
                        touchPoints.add(PointF(it.x, it.y))
                        startTime = System.currentTimeMillis()
                        Log.d("TouchEvent", "ACTION_DOWN at (${it.x}, ${it.y})")
                    }
                    //this means when their finger is moving, as you can imagine
                    MotionEvent.ACTION_MOVE -> {
                        startTime = System.currentTimeMillis()
                        touchPoints.add(
                            PointF(
                                it.x,
                                it.y
                            )
                        ) //adding points to an array to look at later
                        Log.d("TouchEvent", "ACTION_MOVE at (${it.x}, ${it.y})")
                    }
                    //and then is when the user lifts their finger
                    MotionEvent.ACTION_UP -> {
                        Log.d("TouchEvent", "ACTION_UP at (${it.x}, ${it.y})")
                        endTime = System.currentTimeMillis()
                        val duration = (endTime - startTime) / 1000.0
                        Log.d("Time", duration.toString())
                        if (duration >= 1.00) {
                            viewModel.convertContent()
                        }
                        startTime = 0
                        endTime = 0


                        if (isUnderline(touchPoints)) {
                            viewModel.convertContent()
                            if (isPenActivated) {
                                Handler(Looper.getMainLooper()).postDelayed({
                                    listenerStateSaved.value = true
                                }, 200)
                            }

                        } else if (isFlippedCShape(touchPoints)) {
                            onUndoGestureDetected()
                            //if the pen is activiated, we gotta get rid of the WOOSH too
                            if (isPenActivated) {
                                Handler(Looper.getMainLooper()).postDelayed({
                                    listenerStateSaved.value = true
                                }, 200)
                            }
                        } else if (isCShape(touchPoints)) {
                            onRedoGestureDetected()
                            if (isPenActivated) {
                                Handler(Looper.getMainLooper()).postDelayed({
                                    listenerStateSaved.value = true
                                }, 200)
                            }
                        }
                        touchPoints.clear();
                    }

                    else -> {}
                }
            }
        }
        return event?.let { gestureDetector.onTouchEvent(it) } == true || super.onTouchEvent(event)
    }

    private fun isUnderline(points: List<PointF>): Boolean {
        val startX = points.first().x
        val startY = points.first().y
        val endX = points.last().x
        val endY = points.last().y

        val heightDifference = startY - endY
        val widthDifference = endX - startX

        return heightDifference < 20 && widthDifference > 70
    }

    //checks for undo
    private fun isFlippedCShape(points: List<PointF>): Boolean {
        if (points.size < 5) return false

        val startX = points.first().x
        val startY = points.first().y
        val endX = points.last().x
        val endY = points.last().y

        if (startY < endY || startX < endX) return false

        val heightDifference = startY - endY
        val widthDifference = endX - startX

        return heightDifference > 70 && widthDifference < 200
    }

    //checks for redo
    private fun isCShape(points: List<PointF>): Boolean {
        if (points.size < 5) return false

        val startX = points.first().x
        val startY = points.first().y
        val endX = points.last().x
        val endY = points.last().y

        if (startY < endY || startX > endX) return false

        val heightDifference = startY - endY
        val widthDifference = endX - startX

        return heightDifference > 70 && widthDifference < 200
    }


    //what to do when it is detected
    private fun onUndoGestureDetected() {
        if(isPenActivated){
            Handler(Looper.getMainLooper()).postDelayed({
                listenerStateSaved.value = true
            }, 400)
        }
        //viewModel.undo()
        Toast.makeText(this, "Undo action detected!", Toast.LENGTH_SHORT).show()
    }

    private fun onRedoGestureDetected() {
        viewModel.redo()
        Toast.makeText(this, "Redo action detected!", Toast.LENGTH_SHORT).show()
    }

    //this was the gesture listener prof told us to use. I haven't used it yet but here are swipe exmaples for you all
    private inner class GestureListener : GestureDetector.SimpleOnGestureListener() {
//        override fun onLongPress(e: MotionEvent) {
//            viewModel.convertContent()
//        }
//        override fun onFling(
//            e1: MotionEvent?,
//            e2: MotionEvent,
//            velocityX: Float,
//            velocityY: Float
//        ): Boolean {
//            if (e1 != null && e2 != null) {
//                val deltaX = e2.x - e1.x
//                val deltaY = e2.y - e1.y
//
//                if (Math.abs(deltaX) > Math.abs(deltaY) && Math.abs(deltaX) > 100 && Math.abs(velocityX) > 100) {
//                    if (deltaX > 0) {
//                        onSwipeRight()
//                    } else {
//                        onSwipeLeft()
//                    }
//                    return true
//                }
//            }
//            return false
//        }
    }

    private fun onSwipeRight() {
        Toast.makeText(this, "Swiped Right!", Toast.LENGTH_SHORT).show()
    }

    private fun onSwipeLeft() {
        Toast.makeText(this, "Swiped Left!", Toast.LENGTH_SHORT).show()
    }

    //let's not touch this
    private fun setMargins(editor: Editor, @DimenRes horizontalMarginRes: Int, @DimenRes verticalMarginRes: Int) {
        val displayMetrics = resources.displayMetrics
        with (editor.configuration) {
            val verticalMargin = resources.getDimension(verticalMarginRes)
            val horizontalMargin = resources.getDimension(horizontalMarginRes)
            val verticalMarginMM = 25.4f * verticalMargin / displayMetrics.ydpi
            val horizontalMarginMM = 25.4f * horizontalMargin / displayMetrics.xdpi
            setNumber("text.margin.top", verticalMarginMM)
            setNumber("text.margin.left", horizontalMarginMM)
            setNumber("text.margin.right", horizontalMarginMM)
            setNumber("math.margin.top", verticalMarginMM)
            setNumber("math.margin.bottom", verticalMarginMM)
            setNumber("math.margin.left", horizontalMarginMM)
            setNumber("math.margin.right", horizontalMarginMM)
        }
    }

    //this is a menu item that we do not need, but, again "You never know"
    private fun configureDefaultCaptureStrokePrediction(context: Context) {
        val durationMs = FrameTimeEstimator.getFrameTime(context)
            .roundToInt()
            .coerceAtLeast(DefaultMinimumPredictionDurationMs)
        viewModel.changePredictionSettings(EnableCapturePredictionByDefault, durationMs)
    }

    //again, you never know...
    private fun showContextualActionDialog(actionState: ContextualActionState, selectedBlockId: String? = null) {
        when (actionState) {
            is ContextualActionState.AddBlock -> {
                val blockTypes = actionState.items
                launchActionChoiceDialog(blockTypes.map(BlockType::toString)) { selected ->
                    when (val blockType = blockTypes[selected]) {
                        BlockType.Text -> {
                            canGesture = false
                            Log.d("Polly", canGesture.toString());
                            // Ensure bottom sheet is collapsed to avoid weird state when IME is dismissed.
                            viewModel.expandColorPalette(false)
                            launchTextBlockInputDialog { text ->
                                viewModel.insertText(actionState.x, actionState.y, text)
                            }
                        }
                    }
                }
            }
            is ContextualActionState.Action -> {
                val actions = actionState.items
                launchActionChoiceDialog(actions.map { getString(it.stringRes) }) { selected ->
                    when (val action = actions[selected]) {
                        MenuAction.ADD_BLOCK -> {
                            val blocks = viewModel.requestAddBlockActions(actionState.x, actionState.y)
                            showContextualActionDialog(blocks)
                        }
                        MenuAction.FORMAT_TEXT -> {
                            val formatTexts = viewModel.requestFormatTextActions(actionState.x, actionState.y, selectedBlockId)
                            showContextualActionDialog(formatTexts, selectedBlockId)
                        }
                        MenuAction.EXPORT -> {
                            val mimeTypes = viewModel.requestExportActions(actionState.x, actionState.y, selectedBlockId)
                            showContextualActionDialog(mimeTypes, selectedBlockId)
                        }
                        else -> viewModel.actionMenu(actionState.x, actionState.y, action, selectedBlockId)
                    }
                }
            }
            is ContextualActionState.Export -> onExport(actionState.items, actionState.x, actionState.y, selectedBlockId)
        }
    }

    //this is launched after OnCreate(), actually. This is just more declaring that had to be doen when other stuff was
    override fun onStart() {
        super.onStart()

        with(binding.editorToolbar) {
            switchActivePen.setOnCheckedChangeListener { _, isChecked ->
                viewModel.enableActivePen(isChecked)
                isPenActivated = true;
            }
            editorUndo.setOnClickListener { viewModel.undo() }
            editorRedo.setOnClickListener { viewModel.redo() }
            editorClearContent.setOnClickListener { viewModel.clearContent() } //clears content
        }

        with(binding.editorToolbarSheet) {
            BottomSheetBehavior.from(toolbarSettingsBottomSheet).addBottomSheetCallback(onBottomSheetStateChanged)
            toolbarSettingsBottomSheet.setOnClickListener {
                viewModel.toggleColorPalette()
            }
        }
    }

    //this is called right when you call finish()
    override fun onStop() {
        with(binding.editorToolbar) {
            switchActivePen.setOnCheckedChangeListener(null)
            editorUndo.setOnClickListener(null)
            editorRedo.setOnClickListener(null)
            editorClearContent.setOnClickListener(null)
        }

        with(binding.editorToolbarSheet) {
            BottomSheetBehavior.from(toolbarSettingsBottomSheet).removeBottomSheetCallback(onBottomSheetStateChanged)
            toolbarSettingsBottomSheet.setOnClickListener(null)
        }
        super.onStop()
    }

    //this is called after onStop()
    override fun onDestroy() {
        smartGuideView?.setEditor(null)
        smartGuideView?.setMenuListener(null)
        viewModel.setEditor(null)
        super.onDestroy()
    }

    //this is an error function that, again, i don't think is wise to touch, per say
    private fun onError(error: Error?) {
        if (error != null) {
            Log.e("MainActivity", error.toString(), error.exception)
        }
        when (error?.severity) {
            null -> Unit
            Error.Severity.ERROR,
            Error.Severity.CRITICAL ->
                AlertDialog.Builder(this)
                        .setTitle(error.title)
                        .setMessage(error.message)
                        .setPositiveButton(R.string.dialog_ok, null)
                        .show()
            else ->
                // Note: `EditorError` (if any) could be used to specialize the notification (adjust string, localize, notification nature, ...)
                Snackbar.make(binding.root, getString(R.string.app_error_notification, error.severity.name, error.message), Snackbar.LENGTH_LONG)
                        .setAnchorView(binding.editorToolbarSheet.toolbarSettingsBottomSheet)
                        .addCallback(object : Snackbar.Callback() {
                            override fun onDismissed(snackbar: Snackbar?, event: Int) {
                                snackbar?.removeCallback(this)
                                viewModel.dismissErrorMessage(error)
                            }
                        })
                        .show()
        }
    }

    //updating the toolsheet
    private fun onToolSheetExpansionStateUpdate(expanded: Boolean) {
        with(BottomSheetBehavior.from(binding.editorToolbarSheet.toolbarSettingsBottomSheet)) {
            state = if (expanded) BottomSheetBehavior.STATE_EXPANDED else BottomSheetBehavior.STATE_COLLAPSED
        }
    }

    //updating the toolsheet
    private fun onAvailableToolsUpdate(toolStates: List<ToolState>) {
        toolsAdapter.submitList(toolStates)
        binding.editorToolbarSheet.toolbarTools.isVisible = toolStates.isNotEmpty()
    }

    //updating the colors in the toolsheet
    private fun onAvailableColorsUpdate(colorStates: List<ColorState>) {
        colorsAdapter.submitList(colorStates)
        binding.editorToolbarSheet.toolbarColors.isVisible = colorStates.isNotEmpty()
    }

    //updating the thickness in the toolsheet
    private fun onAvailableThicknessesUpdate(thicknessStates: List<ThicknessState>) {
        thicknessesAdapter.submitList(thicknessStates)
        binding.editorToolbarSheet.toolbarThicknesses.isVisible = thicknessStates.isNotEmpty()
    }

    //updating the toolsheet brushes
    private fun onAvailablePenBrushesUpdate(penBrushStates: List<PenBrushState>) {
        penBrushesAdapter.clear()
        if (penBrushStates.isNotEmpty()) {
            penBrushesAdapter.addAll(penBrushStates.map { getString(it.penBrush.label) })
            binding.editorToolbarSheet.penBrushDropdown.setSelection(penBrushStates.indexOfFirst(PenBrushState::isSelected))
        }
        penBrushesAdapter.notifyDataSetChanged()
        canGesture = penBrushStates.isNotEmpty()
        Log.d("polly", canGesture.toString())
        binding.editorToolbarSheet.toolbarPenBrushSection.isVisible = penBrushStates.isNotEmpty()
    }

    //this creates the options menu: the drop down, the three dots (i know think we need but...)
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        super.onCreateOptionsMenu(menu)
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    //this is called to find the items and declare what to do
    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        menu?.let {
//            it.findItem(R.id.nav_menu_new_part).isEnabled = viewModel.requestPartTypes().isNotEmpty()
//            it.findItem(R.id.nav_menu_previous_part).isEnabled = navigationState.hasPrevious
//            it.findItem(R.id.nav_menu_next_part).isEnabled = navigationState.hasNext
            it.findItem(R.id.editor_menu_convert).isEnabled = partState.isReady
            it.findItem(R.id.editor_menu_prediction).isEnabled = true
            it.findItem(R.id.editor_menu_export).isEnabled = partState.isReady
            it.findItem(R.id.editor_menu_save).isEnabled = partState.isReady
            it.findItem(R.id.editor_menu_import_file).isEnabled = true
            it.findItem(R.id.editor_menu_share_file).isEnabled = partState.isReady
        }
        return super.onPrepareOptionsMenu(menu)
    }

    //what happens when you select
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
//            R.id.nav_menu_new_part -> viewModel.requestNewPart()
//            R.id.nav_menu_previous_part -> viewModel.previousPart()
//            R.id.nav_menu_next_part -> viewModel.nextPart()

            R.id.back_arrows -> {
                val intent = Intent(applicationContext, TaskListView::class.java)

                if(officialTitle != null){
                    partState.title = officialTitle.toString()
                }

                if(partState.dateCreated != null){
                    val c = Calendar.getInstance().time

                    val df = SimpleDateFormat("dd-MMM-yyyy", Locale.getDefault())
                    val formattedDate = df.format(c)

                    partState.dateCreated = formattedDate
                }

                intent.putExtra("partId", partState.partId)
                intent.putExtra("isReady", partState.isReady)
                intent.putExtra("partType", partState.partType.toString())
                intent.putExtra("partDate", partState.dateCreated)
                intent.putExtra("partTitle", partState.title)


                startActivity(intent)
                finish()
            }
            R.id.editor_menu_convert -> viewModel.convertContent()
            R.id.editor_menu_prediction -> showPredictionSettingsDialog()
            R.id.editor_menu_export -> onExport(viewModel.getExportMimeTypes())
            R.id.editor_menu_save -> (partState as? PartState.Loaded)?.let { viewModel.save() }
            R.id.editor_menu_import_file -> importIInkFileRequest.launch("*/*")
            R.id.editor_menu_share_file -> (partState as? PartState.Loaded)?.let { onShareFile(it.partId) }
            else -> return super.onOptionsItemSelected(item)
        }
        return true
    }

    //this is for sharing a file, but you never know
    private fun onShareFile(partId: String) {
        viewModel.extractPart(partId, exportsDir) { file ->
            if (file != null) {
                val uri = FileProvider.getUriForFile(this, "${BuildConfig.APPLICATION_ID}.export", file)
                ShareCompat.IntentBuilder(this)
                        .setType("application/octet-stream")
                        .setStream(uri)
                        .startChooser()
            }
        }
    }

    //this is for exporting, but you never know
    private fun onExport(mimeTypes: List<MimeType>, x: Float? = null, y: Float? = null, selectedBlockId: String? = null) {
        if (mimeTypes.isNotEmpty()) {
            val label = mimeTypes.map { mimeType ->
                val extension = mimeType.primaryFileExtension
                // prepend `*` to display `*.jpeg`
                getString(R.string.editor_export_type_label, mimeType.getName(), "*$extension")
            }
            launchSingleChoiceDialog(R.string.editor_menu_export, label, 0) {
                val mimeType = mimeTypes[it]
                viewModel.exportContent(mimeType, x, y, selectedBlockId, exportsDir) { file ->
                    if (file != null) {
                        val uri = FileProvider.getUriForFile(this, "${BuildConfig.APPLICATION_ID}.export", file)
                        if (mimeType == MimeType.HTML || mimeType.isImage) {
                            val intent = Intent(Intent.ACTION_VIEW).apply {
                                setDataAndType(uri, mimeType.typeName)
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            startActivity(Intent.createChooser(intent, uri.lastPathSegment))
                        } else {
                            ShareCompat.IntentBuilder(this)
                                    .setType(mimeType.typeName)
                                    .setStream(uri)
                                    .startChooser()
                        }
                    } else {
                        Toast.makeText(this, R.string.editor_export_failed, Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    //this is to request a new PartState
    private fun onPartCreationRequest(request: NewPartRequest?) {
        if (request != null) {
            val partTypes = request.availablePartTypes
            viewModel.createPart(partTypes[0])
        }
    }

    //this sets the title and date on the action bar
    private fun onPartStateUpdate(state: PartState) {
        partState = state
        supportActionBar?.let {
            var (title, subtitle) = when (state.partId) {
                null -> getString(R.string.app_name) to null
                else -> (state.partType?.toString() ?: "…") to state.partId //To-do: this must be changed to the class name musa comes up with, with the name of the person's list
            }

            //I know this looks convulted, but, i swear to god, the title doesn't set without it, so please don't touch
            if(partState.title != null){
                title = officialTitle
            }
            else{
                title = partState.title
            }
            it.title = title
            supportActionBar?.setDisplayShowHomeEnabled(true)

            //sets the date to current
            val c = Calendar.getInstance().time
            val df = SimpleDateFormat("dd-MMM-yyyy", Locale.getDefault())
            val formattedDate = df.format(c)

            if(partState.dateCreated != null){
                val c = Calendar.getInstance().time

                val df = SimpleDateFormat("dd-MMM-yyyy", Locale.getDefault())
                val formattedDate = df.format(c)
                it.subtitle = formattedDate

            }
            else{
                    it.subtitle = partState.dateCreated
                }
        }

        editorView?.isVisible = state.isReady

        binding.partEditorProgress.isVisible = state is PartState.Loading
        binding.editorToolbarSheet.toolbarSettingsBottomSheet.isVisible = state != PartState.Unloaded
        with(binding.editorToolbar) {
            partEditorControls.isVisible = state != PartState.Unloaded
            switchActivePen.isEnabled = state.isReady
            editorClearContent.isEnabled = state.isReady
        }
    }

    //this is the undo/redo history, which works, so let's not touch it
    private fun onPartHistoryUpdate(state: PartHistoryState) {
        with(binding.editorToolbar) {
            editorRedo.isEnabled = state.canRedo
            editorUndo.isEnabled = state.canUndo
        }
    }

    //no idea what this is for, lemme tell you, but sounds important
    private fun onPartNavigationStateUpdate(state: PartNavigationState) {
        navigationState = state
        invalidateOptionsMenu()
    }

    //this is something we don't need, but you never know...
    private fun showPredictionSettingsDialog() {
        val currentSettings = viewModel.predictionSettings
        launchPredictionDialog(currentSettings.enabled, currentSettings.durationMs) { enabled, durationMs ->
            viewModel.changePredictionSettings(enabled, durationMs)
        }
    }

}
