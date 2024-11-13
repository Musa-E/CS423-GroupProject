//AI STATEMENT: AI was not used in this. This file was imported from https://github.com/MyScript/interactive-ink-examples-android
//into this Android SDK and then edited by our team

package com.myscript.iink.demo

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
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
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
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

import com.example.pdollarrecognizer.Gesture
import com.example.pdollarrecognizer.Point
import com.example.pdollarrecognizer.PointCloudRecognizer
import com.example.pdollarrecognizer.PointCloudRecognizer.classify


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
    get() {
        return when (this) {
    //        MenuAction.COPY -> R.string.editor_action_copy
    //        MenuAction.PASTE -> R.string.editor_action_paste
    //        MenuAction.DELETE -> R.string.editor_action_delete
    //       // MenuAction.CONVERT -> R.string.editor_action_convert
    //        MenuAction.EXPORT -> R.string.editor_action_export
    //        MenuAction.ADD_BLOCK -> R.string.editor_action_add_block
    //        MenuAction.FORMAT_TEXT -> R.string.editor_action_format_text
    //        MenuAction.FORMAT_TEXT_H1 -> R.string.editor_action_format_text_as_heading1
    //        MenuAction.FORMAT_TEXT_H2 -> R.string.editor_action_format_text_as_heading2
    //        MenuAction.FORMAT_TEXT_PARAGRAPH -> R.string.editor_action_format_text_as_paragraph
    //        MenuAction.FORMAT_TEXT_LIST_BULLET -> R.string.editor_action_format_text_as_list_bullet
    //        MenuAction.FORMAT_TEXT_LIST_CHECKBOX -> R.string.editor_action_format_text_as_list_checkbox
    //        MenuAction.FORMAT_TEXT_LIST_NUMBERED -> R.string.editor_action_format_text_as_list_numbered
            else -> {return 0}
        }
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

    private var partStateArrayList: ArrayList<PartState> = ArrayList()

    private lateinit var gestureDetector: GestureDetector
    private val touchPoints = mutableListOf<Point>()
    private val gestureRecognizer = PointCloudRecognizer()
    private var gestureTemplates = mutableListOf<Gesture>()
    private var strokeNum: Int = 0;

    private var isPenActivated = false;
    private val listenerStateSaved = MutableLiveData<Boolean>()
    private var canGesture = false;

    // Array of gesture help resources (they show which gestures are accepted)
    private val images = arrayOf(
        R.drawable.undo_gesture_help_icon,
        R.drawable.redo_gesture_help_icon,
        R.drawable.convert_gesture_help_icon,
        R.drawable.complete_gesture_help_icon,
        R.drawable.delete_gesture_help_icon,
        R.drawable.highlight_gesture_help_icon,
        R.drawable.touch_to_edit_gesture_help_icon
    )
    private var currentImageIndex = 0 // Keep track of which element in the above array the dialog box is in

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
    // private var colorsAdapter = ColorsAdapter { viewModel.changeColor(it) }
    // private var thicknessesAdapter = ThicknessesAdapter { viewModel.changeThickness(it) }
    private val penBrushesAdapter by lazy {
        ArrayAdapter<String>(this, R.layout.toolbar_pen_brush_row, R.id.toolbar_pen_brush_row_label)
    }
    //listens for pen brush to be changed
//    private val penBrushSelectedListener = object : AdapterView.OnItemSelectedListener {
//        override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
//            val penBrushLabel = penBrushesAdapter.getItem(position) ?: return
//            val penBrush = when (penBrushLabel) {
//                getString(R.string.pen_brush_felt_pen) -> PenBrush.FELT_PEN
//                getString(R.string.pen_brush_fountain_pen) -> PenBrush.FOUNTAIN_PEN
//                getString(R.string.pen_brush_calligraphic_brush) -> PenBrush.CALLIGRAPHIC_BRUSH
//                getString(R.string.pen_brush_pencil_brush) -> PenBrush.PENCIL
//                else -> null
//            }
//            if (penBrush != null) {
//                viewModel.changePenBrush(PenBrushState(penBrush, true))
//            }
//        }
//
//        override fun onNothingSelected(parent: AdapterView<*>?) = Unit
//    }

    private companion object {
        const val EnableCapturePredictionByDefault: Boolean = true
        const val DefaultMinimumPredictionDurationMs: Int = 16 // 1 frame @60Hz, 2 frames @120Hz
    }

//    private val onSmartGuideMenuAction = SmartGuideView.MenuListener { x, y, blockId ->
//        val actionState = viewModel.requestSmartGuideActions(x, y, blockId)
//        showContextualActionDialog(actionState, blockId)
//    }

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

        // gestureHelpButton event handler; when the button is pressed, a dialog box will be shown
        val gestureHelpButton = findViewById<ImageButton>(R.id.gesturesHelpButton)
        gestureHelpButton.setOnClickListener {
            // Show the dialog
            showGestureHelpDialog()
        }

        //gestureDetector prof was walking about in class
        gestureDetector = GestureDetector(this, GestureListener())

        editorView = findViewById(com.myscript.iink.uireferenceimplementation.R.id.editor_view)
        smartGuideView = findViewById(com.myscript.iink.uireferenceimplementation.R.id.smart_guide_view)

        setSupportActionBar(binding.toolbar) //creates the bar at the top and makes it editable programmically

        //sets up the entire viewmodel, do not touch
        viewModel.enableActivePen.observe(this) { activePenEnabled ->
            binding.editorToolbar.switchActivePen.isChecked = !activePenEnabled
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

        editorData.editor?.let { editor ->
            viewModel.setEditor(editorData)
            setMargins(editor, R.dimen.editor_horizontal_margin, R.dimen.editor_vertical_margin)
            if (savedInstanceState == null) {
                configureDefaultCaptureStrokePrediction(editorView?.context ?: this)
            }
            smartGuideView?.setEditor(editor)
        }
        smartGuideView?.setTypeface(IInkApplication.DemoModule.defaultTypeface)

        with(binding.editorToolbarSheet) {
            toolbarTools.adapter = toolsAdapter
            // toolbarColors.itemAnimator = null
            // toolbarColors.adapter = colorsAdapter
            // toolbarThicknesses.adapter = thicknessesAdapter
            // penBrushDropdown.adapter = penBrushesAdapter
            // penBrushDropdown.onItemSelectedListener = penBrushSelectedListener
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
                Log.d("BUNDLE", "ITS BLANK")
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
                Log.d("BUNDLE", bundle["partId"] as String)
                Log.d("BUNDLE", bundle["isReady"].toString())
                Log.d("BUNDLE", partType.toString())
                Log.d("BUNDLE", bundle["partDate"] as String)
                Log.d("BUNDLE", bundle["partTitle"] as String)

                officialTitle = bundle["partTitle"] as String

               // viewModel.getPart(bundle["partId"].toString())
                viewModel.getPart(partState) //function that gets the PartState you want to load

            }
        }
        setUpGestureTemplates()
    }

    //here is our own custom touch event
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        if(canGesture) {
            CurrentBlockId.onError = false;
            event?.let {
                when (it.action) {
                    // this means when the user presses on the screen
                    MotionEvent.ACTION_DOWN -> {
                        // touchPoints.clear()
                        touchPoints.add(Point(it.x, it.y, strokeNum))
                        startTime = System.currentTimeMillis()
                    }
                    // this means when their finger is moving, as you can imagine
                    MotionEvent.ACTION_MOVE -> {
                        startTime = System.currentTimeMillis()
                        touchPoints.add(
                            Point(it.x, it.y, strokeNum)
                        ) //adding points to an array to look at later
                    }
                    // and then is when the user lifts their finger
                    MotionEvent.ACTION_UP -> {
                        endTime = System.currentTimeMillis()
                        val duration = (endTime - startTime) / 1000.0
                        Log.d("Time", duration.toString())

                        // timer?
                        // if it's been x seconds since the user has touched the screen, then check
                        // if the user touches the screen again, call this function again and add the points to the same array


                        // recognize the gesture and check what the result is

                        val inputGesture = Gesture(touchPoints, "test")
                        Log.d("GESTURE", inputGesture.toString())

                        val result = classify(inputGesture, gestureTemplates)

                        if(result.score < 0){
                            Log.d("GESTURE", result.name)
                        } else{
                            Log.d("GESTURE", "score: " + result.score)

                            if ((result.name == "underline" || result.name == "pureCircle" || result.name == "oval") && result.score >= 0.93) {
                                Log.d("GESTURE", "some sort of conversation was recognized")
                                val sharedPreferences =
                                    getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
                                val editor = sharedPreferences.edit()
                                editor.putString("universal_value", "New Value")
                                editor.apply() // or editor.commit()
                                Handler(Looper.getMainLooper()).postDelayed({
                                    viewModel.undo()
                                    viewModel.convertContent()
                                }, 500)

                            } else if (result.name == "flippedCShape" && result.score >= 0.87) {
                                Log.d("GESTURE", "undo was recognized")
                                onUndoGestureDetected()
                            } else if (result.name == "CShape" && result.score >= 0.89) {
                                Log.d("GESTURE", "redo was recognized")
                                onRedoGestureDetected()
                            } else if (result.name == "checkmark" && result.score >= 0.9) {
                                Log.d("GESTURE", "gesture is checkmark (${result.score})")
                                // do whatever checkmark does
                            }
                        }
                        // resets things after gesture has been recognized
                        touchPoints.clear();
                        strokeNum = 0;
                        startTime = 0
                        endTime = 0
                    }

                    else -> {}
                }
            }
        }
        return event?.let { gestureDetector.onTouchEvent(it) } == true || super.onTouchEvent(event)
    }

    private fun setUpGestureTemplates() {
        // underline one
        val underlinePoints = listOf( Point(-0.56F, 0.0F, 0), Point(-0.53F, 0.0F, 0), Point(-0.50F, 0.0F, 0), Point(-0.46F, 0.0F, 0), Point(-0.43F, 0.0F, 0), Point(-0.40F, 0.0F, 0), Point(-0.36F, 0.0F, 0), Point(-0.33F, 0.0F, 0), Point(-0.30F, 0.0F, 0), Point(-0.26F, 0.0F, 0), Point(-0.24F, 0.0F, 0), Point(-0.21F, 0.0F, 0), Point(-0.18F, 0.0F, 0), Point(-0.16F, 0.0F, 0), Point(-0.13F, 0.0F, 0), Point(-0.094F, 0.0F, 0), Point(-0.061F, 0.0F, 0), Point(-0.028F, 0.0F, 0), Point(0.005F, 0.0F, 0), Point(0.038F, 0.0F, 0), Point(0.071F, 0.0F, 0), Point(0.105F, 0.0F, 0), Point(0.138F, 0.0F, 0), Point(0.171F, 0.0F, 0), Point(0.205F, 0.0F, 0), Point(0.238F, 0.0F, 0), Point(0.272F, 0.0F, 0), Point(0.305F, 0.0F, 0), Point(0.338F, 0.0F, 0), Point(0.372F, 0.0F, 0), Point(0.404F, 0.0F, 0), Point(0.436F, 0.0F, 0) )
        val underline = Gesture(underlinePoints, "underline")
        gestureTemplates.add(underline)

        //underline two
        val underlinePoints2 = listOf(Point(-0.526022F, 0.09176853F, 0), Point(-0.51204747F, 0.0614896F, 0), Point(-0.48397118F, 0.047224864F, 0), Point(-0.45206022F, 0.037627544F, 0), Point(-0.41924703F, 0.031678174F, 0), Point(-0.38643384F, 0.025728801F, 0), Point(-0.35362065F, 0.019779427F, 0), Point(-0.3204012F, 0.01834774F, 0), Point(-0.28705302F, 0.01834774F, 0), Point(-0.25370485F, 0.01834774F, 0), Point(-0.22035667F, 0.01834774F, 0), Point(-0.1870085F, 0.01834774F, 0), Point(-0.15366033F, 0.01834774F, 0), Point(-0.120457254F, 0.017074827F, 0), Point(-0.08796461F, 0.0095695695F, 0), Point(-0.055471964F, 0.0020643133F, 0), Point(-0.02219391F, 0.0014491305F, 0), Point(0.011154264F, 0.0014491305F, 0), Point(0.044329256F, -1.1608016E-4F, 0), Point(0.07687074F, -0.007406687F, 0), Point(0.10941222F, -0.014697294F, 0), Point(0.1419537F, -0.0219879F, 0), Point(0.17449518F, -0.029278506F, 0), Point(0.20719263F, -0.035159484F, 0), Point(0.2405408F, -0.035159484F, 0), Point(0.273889F, -0.035159484F, 0), Point(0.30723715F, -0.035159484F, 0), Point(0.34058535F, -0.035159484F, 0), Point(0.37393352F, -0.035159484F, 0), Point(0.4072817F, -0.035159484F, 0), Point(0.44062987F, -0.035159484F, 0), Point(0.47397798F, -0.035159484F, 0),)
        val underline2 = Gesture(underlinePoints2, "underline")
        gestureTemplates.add(underline2)

        //underline three
        val underlinePoints3 = listOf(Point(-0.5770349F, 0.04371051F, 0), Point(-0.5501463F, 0.02521147F, 0), Point(-0.5220856F, 0.010484086F, 0), Point(-0.48944807F, 0.010484086F, 0), Point(-0.45681047F, 0.010484086F, 0), Point(-0.42464033F, 0.0055766003F, 0), Point(-0.39258957F, -5.846403E-4F, 0), Point(-0.36033636F, -0.004619906F, 0), Point(-0.32769883F, -0.0046199067F, 0), Point(-0.29506126F, -0.004619907F, 0), Point(-0.26242366F, -0.0046199067F, 0), Point(-0.2297861F, -0.0046199067F, 0), Point(-0.19714853F, -0.0046199067F, 0), Point(-0.16451098F, -0.0046199067F, 0), Point(-0.13187341F, -0.0046199067F, 0), Point(-0.099235855F, -0.0046199067F, 0), Point(-0.06659829F, -0.0046199067F, 0), Point(-0.033960722F, -0.0046199067F, 0), Point(-0.0013231561F, -0.0046199067F, 0), Point(0.031314407F, -0.0046199067F, 0), Point(0.06395198F, -0.0046199067F, 0), Point(0.096589535F, -0.0046199067F, 0), Point(0.12922709F, -0.0046199067F, 0), Point(0.16186467F, -0.0046199067F, 0), Point(0.19450223F, -0.0046199067F, 0), Point(0.2271398F, -0.0046199067F, 0), Point(0.25977734F, -0.0046199067F, 0), Point(0.2924149F, -0.0046199067F, 0), Point(0.32505247F, -0.0046199067F, 0), Point(0.35769004F, -0.0046199067F, 0), Point(0.39032763F, -0.0046199067F, 0), Point(0.4229651F, -0.0046199067F, 0),)
        val underline3 = Gesture(underlinePoints3, "underline")
        gestureTemplates.add(underline3)

        //underline four
        val underlinePoints4 = listOf(Point(-0.53F, -0.53F, 0), Point(-0.53F, -0.53F, 0), Point(-0.50F, -0.50F, 0), Point(-0.46F, -0.46F, 0), Point(-0.43F, -0.43F, 0), Point(-0.40F, -0.40F, 0), Point(-0.37F, -0.37F, 0), Point(-0.33F, -0.33F, 0), Point(-0.30F, -0.30F, 0), Point(-0.27F, -0.27F, 0), Point(-0.24F, -0.24F, 0), Point(-0.20F, -0.20F, 0), Point(-0.17F, -0.17F, 0), Point(-0.14F, -0.14F, 0), Point(-0.10F, -0.10F, 0), Point(-0.07F, -0.07F, 0), Point(-0.04F, -0.04F, 0), Point(-0.01F, -0.01F, 0), Point(0.03F, 0.03F, 0), Point(0.06F, 0.06F, 0), Point(0.09F, 0.09F, 0), Point(0.13F, 0.13F, 0), Point(0.16F, 0.16F, 0), Point(0.19F, 0.19F, 0), Point(0.23F, 0.23F, 0), Point(0.25F, 0.25F, 0), Point(0.29F, 0.29F, 0), Point(0.32F, 0.32F, 0), Point(0.35F, 0.35F, 0), Point(0.38F, 0.38F, 0), Point(0.42F, 0.42F, 0), Point(0.45F, 0.45F, 0),)
        val underline4 = Gesture(underlinePoints4, "underline")
        gestureTemplates.add(underline4)

        //underline five
        val underlinePoints5 = listOf(Point(-0.61F, -0.61F, 0), Point(-0.57F, -0.57F, 0), Point(-0.54F, -0.54F, 0), Point(-0.51F, -0.51F, 0), Point(-0.48F, -0.48F, 0), Point(-0.45F, -0.45F, 0), Point(-0.41F, -0.41F, 0), Point(-0.38F, -0.38F, 0), Point(-0.35F, -0.35F, 0), Point(-0.32F, -0.32F, 0), Point(-0.28F, -0.28F, 0), Point(-0.25F, -0.25F, 0), Point(-0.22F, -0.22F, 0), Point(-0.19F, -0.19F, 0), Point(-0.16F, -0.16F, 0), Point(-0.12F, -0.12F, 0), Point(-0.09F, -0.09F, 0), Point(-0.06F, -0.06F, 0), Point(-0.03F, -0.03F, 0), Point(0.01F, 0.01F, 0), Point(0.04F, 0.04F, 0), Point(0.07F, 0.07F, 0), Point(0.10F, 0.10F, 0), Point(0.14F, 0.14F, 0), Point(0.17F, 0.17F, 0), Point(0.20F, 0.20F, 0), Point(0.23F, 0.23F, 0), Point(0.26F, 0.26F, 0), Point(0.30F, 0.30F, 0), Point(0.33F, 0.33F, 0), Point(0.36F, 0.36F, 0), Point(0.39F, 0.39F, 0),)
        val underline5 = Gesture(underlinePoints5, "underline")
        gestureTemplates.add(underline5)

        //underline six
        val underlinePoints6 = listOf(Point(-0.63F, -0.63F, 0), Point(-0.60F, -0.60F, 0), Point(-0.57F, -0.57F, 0), Point(-0.53F, -0.53F, 0), Point(-0.50F, -0.50F, 0), Point(-0.47F, -0.47F, 0), Point(-0.44F, -0.44F, 0), Point(-0.40F, -0.40F, 0), Point(-0.37F, -0.37F, 0), Point(-0.34F, -0.34F, 0), Point(-0.31F, -0.31F, 0), Point(-0.27F, -0.27F, 0), Point(-0.24F, -0.24F, 0), Point(-0.21F, -0.21F, 0), Point(-0.18F, -0.18F, 0), Point(-0.14F, -0.14F, 0), Point(-0.11F, -0.11F, 0), Point(-0.08F, -0.08F, 0), Point(-0.05F, -0.05F, 0), Point(-0.02F, -0.02F, 0), Point(0.02F, 0.02F, 0), Point(0.05F, 0.05F, 0), Point(0.08F, 0.08F, 0), Point(0.11F, 0.11F, 0), Point(0.15F, 0.15F, 0), Point(0.18F, 0.18F, 0), Point(0.21F, 0.21F, 0), Point(0.24F, 0.24F, 0), Point(0.28F, 0.28F, 0), Point(0.31F, 0.31F, 0), Point(0.34F, 0.34F, 0), Point(0.37F, 0.37F, 0),)
        val underline6 = Gesture(underlinePoints6, "underline")
        gestureTemplates.add(underline6)

        // CShape one
        val cShapePoints = listOf( Point(0.46F, -0.42F, 0), Point(0.42F, -0.45F, 0), Point(0.35F, -0.47F, 0), Point(0.28F, -0.48F, 0), Point(0.20F, -0.48F, 0), Point(0.13F, -0.48F, 0), Point(0.056F, -0.48F, 0), Point(-0.018F, -0.48F, 0), Point(-0.055F, -0.449F, 0), Point(-0.09F, -0.40F, 0), Point(-0.108F, -0.35F, 0), Point(-0.124F, -0.28F, 0), Point(-0.15F, -0.23F, 0), Point(-0.185F, -0.18F, 0), Point(-0.21F, -0.12F, 0), Point(-0.219F, -0.046F, 0), Point(-0.242F, 0.006F, 0), Point(-0.24F, 0.081F, 0), Point(-0.24F, 0.15F, 0), Point(-0.21F, 0.218F, 0), Point(-0.185F, 0.27F, 0), Point(-0.17F, 0.33F, 0), Point(-0.14F, 0.378F, 0), Point(-0.105F, 0.424F, 0), Point(-0.061F, 0.45F, 0), Point(0.014F, 0.455F, 0), Point(0.067F, 0.478F, 0), Point(0.131F, 0.49F, 0), Point(0.207F, 0.49F, 0), Point(0.259F, 0.512F, 0), Point(0.328F, 0.497F, 0), Point(0.40F, 0.49F, 0) )
        val CShape = Gesture(cShapePoints, "CShape")
        gestureTemplates.add(CShape)

        // CShape two
        val cShapePoints2 = listOf( Point(0.528F, 0.353F, 0), Point(0.473F, 0.40F, 0), Point(0.401F, 0.430F, 0), Point(0.326F, 0.450F, 0), Point(0.250F, 0.467F, 0), Point(0.177F, 0.497F, 0), Point(0.103F, 0.526F, 0), Point(0.025F, 0.53F, 0), Point(-0.055F, 0.532F, 0), Point(-0.134F, 0.532F, 0), Point(-0.213F, 0.532F, 0), Point(-0.277F, 0.487F, 0), Point(-0.321F, 0.422F, 0), Point(-0.362F, 0.354F, 0), Point(-0.403F, 0.286F, 0), Point(-0.443F, 0.218F, 0), Point(-0.452F, 0.141F, 0), Point(-0.452F, 0.0615F, 0), Point(-0.426F, -0.011F, 0), Point(-0.385F, -0.079F, 0), Point(-0.354F, -0.151F, 0), Point(-0.298F, -0.194F, 0), Point(-0.245F, -0.251F, 0), Point(-0.185F, -0.302F, 0), Point(-0.120F, -0.346F, 0), Point(-0.044F, -0.368F, 0), Point(0.0309F, -0.391F, 0), Point(0.102F, -0.425F, 0), Point(0.169F, -0.468F, 0), Point(0.248F, -0.468F, 0), Point(0.327F, -0.468F, 0), Point(0.407F, -0.468F, 0) )
        val CShape2 = Gesture(cShapePoints2, "CShape")
        gestureTemplates.add(CShape2)

        // CShape three
        val cShapePoints3 = listOf(Point(0.25880083F, 0.30661562F, 0), Point(0.19568273F, 0.30661562F, 0), Point(0.13256463F, 0.30661562F, 0), Point(0.069446534F, 0.30661562F, 0), Point(0.0064835404F, 0.305745F, 0), Point(-0.052751042F, 0.28394687F, 0), Point(-0.11198562F, 0.26214874F, 0), Point(-0.17503355F, 0.2617549F, 0), Point(-0.23431057F, 0.2442413F, 0), Point(-0.29071933F, 0.21616642F, 0), Point(-0.3436231F, 0.18174133F, 0), Point(-0.39652684F, 0.14731623F, 0), Point(-0.4515484F, 0.11646409F, 0), Point(-0.49125397F, 0.07708108F, 0), Point(-0.4679516F, 0.020880407F, 0), Point(-0.43351257F, -0.032014273F, 0), Point(-0.39328146F, -0.07821383F, 0), Point(-0.33598176F, -0.104683384F, 0), Point(-0.27754068F, -0.12848327F, 0), Point(-0.21881518F, -0.15161783F, 0), Point(-0.16008966F, -0.1747524F, 0), Point(-0.10128024F, -0.19766805F, 0), Point(-0.04213333F, -0.21970299F, 0), Point(0.017013572F, -0.24173793F, 0), Point(0.076160476F, -0.26377288F, 0), Point(0.13530737F, -0.28580785F, 0), Point(0.19445428F, -0.30784276F, 0), Point(0.25627375F, -0.3150485F, 0), Point(0.31939185F, -0.31504846F, 0), Point(0.38250995F, -0.31504846F, 0), Point(0.44562805F, -0.3150485F, 0), Point(0.508746F, -0.3150485F, 0), )
        val CShape3 = Gesture(cShapePoints3, "CShape")
        gestureTemplates.add(CShape3)

        // CShape four
        val cShapePoints4 = listOf(Point(0.42F, 0.42F, 0), Point(0.37F, 0.37F, 0), Point(0.29F, 0.29F, 0), Point(0.22F, 0.22F, 0), Point(0.14F, 0.14F, 0), Point(0.08F, 0.08F, 0), Point(0.03F, 0.03F, 0), Point(-0.02F, -0.02F, 0), Point(-0.08F, -0.08F, 0), Point(-0.14F, -0.14F, 0), Point(-0.19F, -0.19F, 0), Point(-0.22F, -0.22F, 0), Point(-0.25F, -0.25F, 0), Point(-0.27F, -0.27F, 0), Point(-0.29F, -0.29F, 0), Point(-0.32F, -0.32F, 0), Point(-0.32F, -0.32F, 0), Point(-0.32F, -0.32F, 0), Point(-0.31F, -0.31F, 0), Point(-0.29F, -0.29F, 0), Point(-0.24F, -0.24F, 0), Point(-0.17F, -0.17F, 0), Point(-0.11F, -0.11F, 0), Point(-0.04F, -0.04F, 0), Point(0.02F, 0.02F, 0), Point(0.08F, 0.08F, 0), Point(0.14F, 0.14F, 0), Point(0.21F, 0.21F, 0), Point(0.29F, 0.29F, 0), Point(0.36F, 0.36F, 0), Point(0.44F, 0.44F, 0), Point(0.52F, 0.52F, 0), )
        val CShape4 = Gesture(cShapePoints4, "CShape")
        gestureTemplates.add(CShape4)

        // CShape five
        val cShapePoints5 = listOf(Point(0.25F, 0.25F, 0), Point(0.19F, 0.19F, 0), Point(0.13F, 0.13F, 0), Point(0.07F, 0.07F, 0), Point(0.03F, 0.03F, 0), Point(-0.01F, -0.01F, 0), Point(-0.06F, -0.06F, 0), Point(-0.09F, -0.09F, 0), Point(-0.11F, -0.11F, 0), Point(-0.13F, -0.13F, 0), Point(-0.15F, -0.15F, 0), Point(-0.16F, -0.16F, 0), Point(-0.18F, -0.18F, 0), Point(-0.20F, -0.20F, 0), Point(-0.20F, -0.20F, 0), Point(-0.18F, -0.18F, 0), Point(-0.18F, -0.18F, 0), Point(-0.15F, -0.15F, 0), Point(-0.14F, -0.14F, 0), Point(-0.11F, -0.11F, 0), Point(-0.09F, -0.09F, 0), Point(-0.07F, -0.07F, 0), Point(-0.05F, -0.05F, 0), Point(-0.01F, -0.01F, 0), Point(0.02F, 0.02F, 0), Point(0.06F, 0.06F, 0), Point(0.12F, 0.12F, 0), Point(0.18F, 0.18F, 0), Point(0.23F, 0.23F, 0), Point(0.29F, 0.29F, 0), Point(0.35F, 0.35F, 0), Point(0.41F, 0.41F, 0), )
        val CShape5 = Gesture(cShapePoints5, "CShape")
        gestureTemplates.add(CShape5)

        // CShape six
        val cShapePoints6 = listOf(Point(0.61F, 0.61F, 0), Point(0.53F, 0.53F, 0), Point(0.44F, 0.44F, 0), Point(0.36F, 0.36F, 0), Point(0.29F, 0.29F, 0), Point(0.21F, 0.21F, 0), Point(0.14F, 0.14F, 0), Point(0.06F, 0.06F, 0), Point(-0.02F, -0.02F, 0), Point(-0.10F, -0.10F, 0), Point(-0.16F, -0.16F, 0), Point(-0.22F, -0.22F, 0), Point(-0.28F, -0.28F, 0), Point(-0.32F, -0.32F, 0), Point(-0.36F, -0.36F, 0), Point(-0.37F, -0.37F, 0), Point(-0.37F, -0.37F, 0), Point(-0.39F, -0.39F, 0), Point(-0.38F, -0.38F, 0), Point(-0.34F, -0.34F, 0), Point(-0.30F, -0.30F, 0), Point(-0.24F, -0.24F, 0), Point(-0.17F, -0.17F, 0), Point(-0.10F, -0.10F, 0), Point(-0.01F, -0.01F, 0), Point(0.06F, 0.06F, 0), Point(0.14F, 0.14F, 0), Point(0.22F, 0.22F, 0), Point(0.31F, 0.31F, 0), Point(0.39F, 0.39F, 0), Point(0.48F, 0.48F, 0), Point(0.56F, 0.56F, 0), )
        val CShape6 = Gesture(cShapePoints6, "CShape")
        gestureTemplates.add(CShape6)

        // flippedCShape one
        val flippedCShapePoints = listOf( Point(-0.48F, -0.43F, 0), Point(-0.42F, -0.47F, 0), Point(-0.36F, -0.49F, 0), Point(-0.28F, -0.49F, 0), Point(-0.206F, -0.49F, 0), Point(-0.127F, -0.493F, 0), Point(-0.047F, -0.493F, 0), Point(-0.007F, -0.45F, 0), Point(0.066F, -0.439F, 0), Point(0.143F, -0.434F, 0), Point(0.18F, -0.39F, 0), Point(0.227F, -0.36F, 0), Point(0.247F, -0.288F, 0), Point(0.247F, -0.209F, 0), Point(0.247F, -0.13F, 0), Point(0.247F, -0.05F, 0), Point(0.247F, 0.029F, 0), Point(0.247F, 0.108F, 0), Point(0.227F, 0.168F, 0), Point(0.227F, 0.247F, 0), Point(0.227F, 0.327F, 0), Point(0.188F, 0.367F, 0), Point(0.132F, 0.39F, 0), Point(0.11F, 0.447F, 0), Point(0.052F, 0.468F, 0), Point(-0.0038F, 0.507F, 0), Point(-0.083F, 0.507F, 0), Point(-0.16F, 0.507F, 0), Point(-0.242F, 0.507F, 0), Point(-0.32F, 0.507F, 0), Point(-0.4F, 0.507F, 0), Point(-0.48F, 0.507F, 0) )
        val flippedCShape = Gesture(flippedCShapePoints, "flippedCShape")
        gestureTemplates.add(flippedCShape)

        // flippedCShape two
        val flippedCShapePoints2 = listOf( Point(-0.35F, 0.757F, 0), Point(-0.340F, 0.717F, 0), Point(-0.269F, 0.677F, 0), Point(-0.229F, 0.636F, 0), Point(-0.189F, 0.596F, 0), Point(-0.149F, 0.556F, 0), Point(-0.108F, 0.515F, 0), Point(-0.0696F, 0.474F, 0), Point(-0.0307F, 0.432F, 0), Point(0.0046F, 0.387F, 0), Point(0.0375F, 0.341F, 0), Point(0.0706F, 0.295F, 0), Point(0.111F, 0.254F, 0), Point(0.146F, 0.209F, 0), Point(0.174F, 0.160F, 0), Point(0.188F, 0.106F, 0), Point(0.188F, 0.049F, 0), Point(0.188F, -0.008F, 0), Point(0.188F, -0.065F, 0), Point(0.188F, -0.122F, 0), Point(0.188F, -0.179F, 0), Point(0.157F, -0.211F, 0), Point(0.114F, -0.243F, 0), Point(0.057F, -0.243F, 0), Point(-0.0004F, -0.243F, 0), Point(-0.057F, -0.243F, 0), Point(-0.114F, -0.243F, 0), Point(-0.171F, -0.243F, 0), Point(-0.217F, -0.215F, 0), Point(-0.257F, -0.175F, 0), Point(-0.297F, -0.135F, 0), Point(-0.35F, -0.124F, 0) )
        val flippedCShape2 = Gesture(flippedCShapePoints2, "flippedCShape")
        gestureTemplates.add(flippedCShape2)

        // flippedCShape three
        val flippedCShapePoints3 = listOf(Point(-0.28652596F, 0.25841972F, 0), Point(-0.22315463F, 0.25841972F, 0), Point(-0.1597833F, 0.25841972F, 0), Point(-0.09641197F, 0.25841972F, 0), Point(-0.033040643F, 0.25841972F, 0), Point(0.030330688F, 0.25841972F, 0), Point(0.09304829F, 0.2561376F, 0), Point(0.14680806F, 0.22258447F, 0), Point(0.20782867F, 0.21670032F, 0), Point(0.2658179F, 0.2036871F, 0), Point(0.31800964F, 0.1795552F, 0), Point(0.36389947F, 0.1358511F, 0), Point(0.40269804F, 0.087003335F, 0), Point(0.42885804F, 0.029554524F, 0), Point(0.4079797F, -0.025164299F, 0), Point(0.3631532F, -0.06995837F, 0), Point(0.31832668F, -0.114752434F, 0), Point(0.26491153F, -0.14867303F, 0), Point(0.2106954F, -0.18146607F, 0), Point(0.15533376F, -0.21230458F, 0), Point(0.09997214F, -0.24314308F, 0), Point(0.041042354F, -0.26606444F, 0), Point(-0.018730462F, -0.28711522F, 0), Point(-0.07850327F, -0.308166F, 0), Point(-0.13827609F, -0.3292168F, 0), Point(-0.19804892F, -0.3502676F, 0), Point(-0.25797245F, -0.37087864F, 0), Point(-0.31889415F, -0.38810408F, 0), Point(-0.38102818F, -0.39864254F, 0), Point(-0.44439948F, -0.39864254F, 0), Point(-0.50777084F, -0.3986425F, 0), Point(-0.57114196F, -0.3986425F, 0),)
        val flippedCShape3 = Gesture(flippedCShapePoints3, "flippedCShape")
        gestureTemplates.add(flippedCShape3)

        // flippedCShape four
        val flippedCShapePoints4 = listOf(Point(-0.47812015F, 0.39006054F, 0), Point(-0.41237044F, 0.3599987F, 0), Point(-0.3442252F, 0.33554554F, 0), Point(-0.27607995F, 0.31109235F, 0), Point(-0.20945852F, 0.2830779F, 0), Point(-0.13907316F, 0.2745527F, 0), Point(-0.06915256F, 0.26973504F, 0), Point(-0.027069978F, 0.21082163F, 0), Point(0.02833762F, 0.17780183F, 0), Point(0.10073742F, 0.17780185F, 0), Point(0.16693884F, 0.1520445F, 0), Point(0.22858438F, 0.11407666F, 0), Point(0.29120204F, 0.07824401F, 0), Point(0.3580269F, 0.053993266F, 0), Point(0.38485897F, -0.012791132F, 0), Point(0.40166295F, -0.08321385F, 0), Point(0.40204275F, -0.15556897F, 0), Point(0.38012674F, -0.22302191F, 0), Point(0.3401795F, -0.27746692F, 0), Point(0.26974866F, -0.29423678F, 0), Point(0.19778968F, -0.3009991F, 0), Point(0.12556367F, -0.30601272F, 0), Point(0.05333767F, -0.31102636F, 0), Point(-0.01888832F, -0.31603998F, 0), Point(-0.09115888F, -0.319768F, 0), Point(-0.16355868F, -0.31976798F, 0), Point(-0.23595849F, -0.31976798F, 0), Point(-0.30835828F, -0.319768F, 0), Point(-0.38075808F, -0.319768F, 0), Point(-0.45315784F, -0.319768F, 0), Point(-0.52555764F, -0.319768F, 0), Point(-0.59795725F, -0.319768F, 0),)
        val flippedCShape4 = Gesture(flippedCShapePoints4, "flippedCShape")
        gestureTemplates.add(flippedCShape4)

        // flippedCShape five
        val flippedCShapePoints5 = listOf(Point(-0.48F, -0.48F, 0), Point(-0.40F, -0.40F, 0), Point(-0.32F, -0.32F, 0), Point(-0.23F, -0.23F, 0), Point(-0.15F, -0.15F, 0), Point(-0.06F, -0.06F, 0), Point(-0.02F, -0.02F, 0), Point(0.03F, 0.03F, 0), Point(0.09F, 0.09F, 0), Point(0.15F, 0.15F, 0), Point(0.20F, 0.20F, 0), Point(0.25F, 0.25F, 0), Point(0.28F, 0.28F, 0), Point(0.30F, 0.30F, 0), Point(0.33F, 0.33F, 0), Point(0.36F, 0.36F, 0), Point(0.36F, 0.36F, 0), Point(0.34F, 0.34F, 0), Point(0.31F, 0.31F, 0), Point(0.28F, 0.28F, 0), Point(0.22F, 0.22F, 0), Point(0.17F, 0.17F, 0), Point(0.11F, 0.11F, 0), Point(0.03F, 0.03F, 0), Point(-0.02F, -0.02F, 0), Point(-0.10F, -0.10F, 0), Point(-0.17F, -0.17F, 0), Point(-0.25F, -0.25F, 0), Point(-0.34F, -0.34F, 0), Point(-0.42F, -0.42F, 0), Point(-0.47F, -0.47F, 0), Point(-0.53F, -0.53F, 0),)
        val flippedCShape5 = Gesture(flippedCShapePoints5, "flippedCShape")
        gestureTemplates.add(flippedCShape5)

        // flippedCShape six
        val flippedCShapePoints6 = listOf(Point(-0.59F, -0.59F, 0), Point(-0.51F, -0.51F, 0), Point(-0.43F, -0.43F, 0), Point(-0.36F, -0.36F, 0), Point(-0.28F, -0.28F, 0), Point(-0.20F, -0.20F, 0), Point(-0.12F, -0.12F, 0), Point(-0.04F, -0.04F, 0), Point(0.04F, 0.04F, 0), Point(0.10F, 0.10F, 0), Point(0.16F, 0.16F, 0), Point(0.22F, 0.22F, 0), Point(0.28F, 0.28F, 0), Point(0.32F, 0.32F, 0), Point(0.35F, 0.35F, 0), Point(0.39F, 0.39F, 0), Point(0.41F, 0.41F, 0), Point(0.39F, 0.39F, 0), Point(0.35F, 0.35F, 0), Point(0.31F, 0.31F, 0), Point(0.26F, 0.26F, 0), Point(0.20F, 0.20F, 0), Point(0.14F, 0.14F, 0), Point(0.08F, 0.08F, 0), Point(-0.00F, -0.00F, 0), Point(-0.08F, -0.08F, 0), Point(-0.16F, -0.16F, 0), Point(-0.24F, -0.24F, 0), Point(-0.31F, -0.31F, 0), Point(-0.39F, -0.39F, 0), Point(-0.47F, -0.47F, 0), Point(-0.55F, -0.55F, 0),)
        val flippedCShape6 = Gesture(flippedCShapePoints6, "flippedCShape")
        gestureTemplates.add(flippedCShape6)

        // checkmark one
        val checkmarkPoints = listOf(Point(-0.437F, 0.075F, 0), Point(-0.419F, 0.12F, 0), Point(-0.39F, 0.157F, 0), Point(-0.38F, 0.218F, 0), Point(-0.35F, 0.25F, 0), Point(-0.327F, 0.296F, 0), Point(-0.303F, 0.337F, 0), Point(-0.25F, 0.367F, 0), Point(-0.216F, 0.399F, 0), Point(-0.182F, 0.430F, 0), Point(-0.167F, 0.379F, 0), Point(-0.128F, 0.332F, 0), Point(-0.114F, 0.27F, 0), Point(-0.0698F, 0.231F, 0), Point(-0.04F, 0.174F, 0), Point(-0.025F, 0.125F, 0), Point(0.018F, 0.078F, 0), Point(0.034F, 0.020F, 0), Point(0.061F, -0.036F, 0), Point(0.097F, -0.089F, 0), Point(0.126F, -0.132F, 0), Point(0.149F, -0.173F, 0), Point(0.189F, -0.204F, 0), Point(0.221F, -0.255F, 0), Point(0.269F, -0.286F, 0), Point(0.311F, -0.321F, 0), Point(0.333F, -0.366F, 0), Point(0.38F, -0.406F, 0), Point(0.403F, -0.464F, 0), Point(0.449F, -0.482F, 0), Point(0.465F, -0.531F, 0), Point(0.491F, -0.57F, 0) )
        val checkmark = Gesture(checkmarkPoints, "checkmark")
        gestureTemplates.add(checkmark)

        // checkmark two
        val checkmarkPoints2 = listOf(Point(-0.23F, -0.23F, 0), Point(-0.21F, -0.21F, 0), Point(-0.21F, -0.21F, 0), Point(-0.18F, -0.18F, 0), Point(-0.15F, -0.15F, 0), Point(-0.13F, -0.13F, 0), Point(-0.11F, -0.11F, 0), Point(-0.09F, -0.09F, 0), Point(-0.07F, -0.07F, 0), Point(-0.06F, -0.06F, 0), Point(-0.03F, -0.03F, 0), Point(-0.01F, -0.01F, 0), Point(-0.01F, -0.01F, 0), Point(-0.01F, -0.01F, 0), Point(-0.01F, -0.01F, 0), Point(0.02F, 0.02F, 0), Point(0.02F, 0.02F, 0), Point(0.05F, 0.05F, 0), Point(0.05F, 0.05F, 0), Point(0.07F, 0.07F, 0), Point(0.08F, 0.08F, 0), Point(0.08F, 0.08F, 0), Point(0.09F, 0.09F, 0), Point(0.11F, 0.11F, 0), Point(0.11F, 0.11F, 0), Point(0.14F, 0.14F, 0), Point(0.14F, 0.14F, 0), Point(0.14F, 0.14F, 0), Point(0.17F, 0.17F, 0), Point(0.17F, 0.17F, 0), Point(0.19F, 0.19F, 0), Point(0.19F, 0.19F, 0))
        val checkmark2 = Gesture(checkmarkPoints2, "checkmark")
        gestureTemplates.add(checkmark2)

        // checkmark three
        val checkmarkPoints3 = listOf(Point(-0.14F, -0.14F, 0), Point(-0.16F, -0.16F, 0), Point(-0.15F, -0.15F, 0), Point(-0.14F, -0.14F, 0), Point(-0.12F, -0.12F, 0), Point(-0.10F, -0.10F, 0), Point(-0.09F, -0.09F, 0), Point(-0.05F, -0.05F, 0), Point(-0.03F, -0.03F, 0), Point(-0.02F, -0.02F, 0), Point(-0.00F, -0.00F, 0), Point(-0.00F, -0.00F, 0), Point(0.01F, 0.01F, 0), Point(0.01F, 0.01F, 0), Point(0.03F, 0.03F, 0), Point(0.04F, 0.04F, 0), Point(0.05F, 0.05F, 0), Point(0.06F, 0.06F, 0), Point(0.06F, 0.06F, 0), Point(0.06F, 0.06F, 0), Point(0.06F, 0.06F, 0), Point(0.07F, 0.07F, 0), Point(0.09F, 0.09F, 0), Point(0.10F, 0.10F, 0), Point(0.11F, 0.11F, 0), Point(0.11F, 0.11F, 0), Point(0.12F, 0.12F, 0), Point(0.13F, 0.13F, 0), Point(0.14F, 0.14F, 0), Point(0.15F, 0.15F, 0), Point(0.16F, 0.16F, 0), Point(0.17F, 0.17F, 0))
        val checkmark3 = Gesture(checkmarkPoints3, "checkmark")
        gestureTemplates.add(checkmark3)

        // checkmark four
        val checkmarkPoints4 = listOf(Point(-0.26F, -0.26F, 0), Point(-0.24F, -0.24F, 0), Point(-0.21F, -0.21F, 0), Point(-0.19F, -0.19F, 0), Point(-0.15F, -0.15F, 0), Point(-0.13F, -0.13F, 0), Point(-0.11F, -0.11F, 0), Point(-0.09F, -0.09F, 0), Point(-0.06F, -0.06F, 0), Point(-0.05F, -0.05F, 0), Point(-0.03F, -0.03F, 0), Point(-0.02F, -0.02F, 0), Point(-0.01F, -0.01F, 0), Point(0.00F, 0.00F, 0), Point(0.01F, 0.01F, 0), Point(0.01F, 0.01F, 0), Point(0.02F, 0.02F, 0), Point(0.04F, 0.04F, 0), Point(0.05F, 0.05F, 0), Point(0.07F, 0.07F, 0), Point(0.08F, 0.08F, 0), Point(0.09F, 0.09F, 0), Point(0.10F, 0.10F, 0), Point(0.12F, 0.12F, 0), Point(0.14F, 0.14F, 0), Point(0.15F, 0.15F, 0), Point(0.15F, 0.15F, 0), Point(0.17F, 0.17F, 0), Point(0.19F, 0.19F, 0), Point(0.19F, 0.19F, 0), Point(0.21F, 0.21F, 0), Point(0.23F, 0.23F, 0))
        val checkmark4 = Gesture(checkmarkPoints4, "checkmark")
        gestureTemplates.add(checkmark4)

        // checkmark five
        val checkmarkPoints5 = listOf(Point(-0.31F, -0.31F, 0), Point(-0.29F, -0.29F, 0), Point(-0.25F, -0.25F, 0), Point(-0.22F, -0.22F, 0), Point(-0.20F, -0.20F, 0), Point(-0.18F, -0.18F, 0), Point(-0.16F, -0.16F, 0), Point(-0.14F, -0.14F, 0), Point(-0.11F, -0.11F, 0), Point(-0.11F, -0.11F, 0), Point(-0.09F, -0.09F, 0), Point(-0.07F, -0.07F, 0), Point(-0.05F, -0.05F, 0), Point(-0.04F, -0.04F, 0), Point(-0.03F, -0.03F, 0), Point(-0.01F, -0.01F, 0), Point(0.01F, 0.01F, 0), Point(0.04F, 0.04F, 0), Point(0.05F, 0.05F, 0), Point(0.06F, 0.06F, 0), Point(0.08F, 0.08F, 0), Point(0.10F, 0.10F, 0), Point(0.10F, 0.10F, 0), Point(0.12F, 0.12F, 0), Point(0.15F, 0.15F, 0), Point(0.16F, 0.16F, 0), Point(0.18F, 0.18F, 0), Point(0.20F, 0.20F, 0), Point(0.21F, 0.21F, 0), Point(0.23F, 0.23F, 0), Point(0.25F, 0.25F, 0), Point(0.27F, 0.27F, 0))
        val checkmark5 = Gesture(checkmarkPoints5, "checkmark")
        gestureTemplates.add(checkmark5)

        // checkmark six
        val checkmarkPoints6 = listOf(Point(-0.28F, -0.28F, 0), Point(-0.28F, -0.28F, 0), Point(-0.28F, -0.28F, 0), Point(-0.25F, -0.25F, 0), Point(-0.21F, -0.21F, 0), Point(-0.18F, -0.18F, 0), Point(-0.16F, -0.16F, 0), Point(-0.13F, -0.13F, 0), Point(-0.10F, -0.10F, 0), Point(-0.08F, -0.08F, 0), Point(-0.06F, -0.06F, 0), Point(-0.06F, -0.06F, 0), Point(-0.04F, -0.04F, 0), Point(-0.02F, -0.02F, 0), Point(-0.00F, -0.00F, 0), Point(0.01F, 0.01F, 0), Point(0.03F, 0.03F, 0), Point(0.03F, 0.03F, 0), Point(0.05F, 0.05F, 0), Point(0.07F, 0.07F, 0), Point(0.08F, 0.08F, 0), Point(0.10F, 0.10F, 0), Point(0.11F, 0.11F, 0), Point(0.12F, 0.12F, 0), Point(0.12F, 0.12F, 0), Point(0.14F, 0.14F, 0), Point(0.14F, 0.14F, 0), Point(0.16F, 0.16F, 0), Point(0.17F, 0.17F, 0), Point(0.20F, 0.20F, 0), Point(0.22F, 0.22F, 0), Point(0.25F, 0.25F, 0))
        val checkmark6 = Gesture(checkmarkPoints6, "checkmark")
        gestureTemplates.add(checkmark6)

        // pureCircle one
        // does not correspond with a gesture, but 'O'/'o' kept being read as undo/redo
        val pureCirclePoints = listOf( Point(-0.221F, -0.418F, 0), Point(-0.311F, -0.386F, 0), Point(-0.374F, -0.328F, 0), Point(-0.407F, -0.238F, 0), Point(-0.448F, -0.145F, 0), Point(-0.448F, -0.023F, 0), Point(-0.448F, 0.099F, 0), Point(-0.448F, 0.221F, 0), Point(-0.413F, 0.307F, 0), Point(-0.359F, 0.394F, 0), Point(-0.269F, 0.449F, 0), Point(-0.189F, 0.492F, 0), Point(-0.067F, 0.492F, 0), Point(0.054F, 0.492F, 0), Point(0.176F, 0.492F, 0), Point(0.251F, 0.444F, 0), Point(0.324F, 0.396F, 0), Point(0.357F, 0.307F, 0), Point(0.417F, 0.243F, 0), Point(0.456F, 0.151F, 0), Point(0.472F, 0.045F, 0), Point(0.456F, -0.061F, 0), Point(0.414F, -0.152F, 0), Point(0.366F, -0.249F, 0), Point(0.324F, -0.328F, 0), Point(0.276F, -0.402F, 0), Point(0.212F, -0.460F, 0), Point(0.138F, -0.508F, 0), Point(0.016F, -0.508F, 0), Point(-0.105F, -0.508F, 0), Point(-0.179F, -0.459F, 0), Point(-0.269F, -0.418F, 0) )
        val pureCircle = Gesture(pureCirclePoints, "pureCircle")
        gestureTemplates.add(pureCircle)

        // pureCircle two
        val pureCirclePoints2 = listOf(Point(0.044942796F, -0.21606079F, 0), Point(-0.051692255F, -0.24786487F, 0), Point(-0.15403703F, -0.2545929F, 0), Point(-0.25010237F, -0.22446823F, 0), Point(-0.327656F, -0.15766697F, 0), Point(-0.37062344F, -0.06767777F, 0), Point(-0.37275344F, 0.034866817F, 0), Point(-0.347976F, 0.13388143F, 0), Point(-0.2937287F, 0.22072187F, 0), Point(-0.21787229F, 0.28965044F, 0), Point(-0.13211091F, 0.34591746F, 0), Point(-0.039127573F, 0.3891608F, 0), Point(0.059584588F, 0.41718835F, 0), Point(0.16181156F, 0.4243062F, 0), Point(0.26378942F, 0.41575113F, 0), Point(0.36073345F, 0.3820266F, 0), Point(0.45200515F, 0.33531114F, 0), Point(0.52432954F, 0.26315108F, 0), Point(0.5537817F, 0.16630082F, 0), Point(0.5488264F, 0.064231746F, 0), Point(0.5039629F, -0.027449803F, 0), Point(0.4431855F, -0.110158086F, 0), Point(0.37308457F, -0.18513231F, 0), Point(0.29454482F, -0.2512837F, 0), Point(0.20867337F, -0.30689883F, 0), Point(0.11033112F, -0.33539137F, 0), Point(0.007881339F, -0.3422763F, 0), Point(-0.09389658F, -0.33077335F, 0), Point(-0.1932391F, -0.3047585F, 0), Point(-0.2907721F, -0.27250552F, 0), Point(-0.38601267F, -0.23424092F, 0), Point(-0.4449654F, -0.15551046F, 0),)
        val pureCircle2 = Gesture(pureCirclePoints2, "pureCircle")
        gestureTemplates.add(pureCircle2)

        // pureCircle three
        val pureCirclePoints3 = listOf(Point(-0.33268917F, 0.0071404874F, 0), Point(-0.30328888F, 0.100730404F, 0), Point(-0.24752969F, 0.18338999F, 0), Point(-0.17736441F, 0.25363362F, 0), Point(-0.09322922F, 0.307103F, 0), Point(-0.0025462452F, 0.3486008F, 0), Point(0.09390674F, 0.3726743F, 0), Point(0.19350547F, 0.3769079F, 0), Point(0.29091108F, 0.36158144F, 0), Point(0.37343854F, 0.30614817F, 0), Point(0.44288686F, 0.23472135F, 0), Point(0.49525958F, 0.14998208F, 0), Point(0.53161466F, 0.0574063F, 0), Point(0.54278415F, -0.041428357F, 0), Point(0.53491396F, -0.14021501F, 0), Point(0.4971187F, -0.2324753F, 0), Point(0.45065048F, -0.320595F, 0), Point(0.38664863F, -0.39677754F, 0), Point(0.30292666F, -0.4502465F, 0), Point(0.2089316F, -0.48261756F, 0), Point(0.1097568F, -0.49053392F, 0), Point(0.01179029F, -0.47878796F, 0), Point(-0.07817734F, -0.43626428F, 0), Point(-0.1546516F, -0.3728222F, 0), Point(-0.22115237F, -0.29850197F, 0), Point(-0.280329F, -0.21821615F, 0), Point(-0.3350837F, -0.13491054F, 0), Point(-0.38223612F, -0.046995796F, 0), Point(-0.41959643F, 0.045511086F, 0), Point(-0.44463867F, 0.14186944F, 0), Point(-0.4550447F, 0.24097157F, 0), Point(-0.41408452F, 0.3280707F, 0),)
        val pureCircle3 = Gesture(pureCirclePoints3, "pureCircle")
        gestureTemplates.add(pureCircle3)

        // pureCircle four
        val pureCirclePoints4 = listOf(Point(-0.07F, -0.07F, 0), Point(-0.18F, -0.18F, 0), Point(-0.29F, -0.29F, 0), Point(-0.36F, -0.36F, 0), Point(-0.42F, -0.42F, 0), Point(-0.45F, -0.45F, 0), Point(-0.47F, -0.47F, 0), Point(-0.49F, -0.49F, 0), Point(-0.49F, -0.49F, 0), Point(-0.48F, -0.48F, 0), Point(-0.43F, -0.43F, 0), Point(-0.38F, -0.38F, 0), Point(-0.32F, -0.32F, 0), Point(-0.21F, -0.21F, 0), Point(-0.11F, -0.11F, 0), Point(0.00F, 0.00F, 0), Point(0.11F, 0.11F, 0), Point(0.22F, 0.22F, 0), Point(0.29F, 0.29F, 0), Point(0.37F, 0.37F, 0), Point(0.42F, 0.42F, 0), Point(0.46F, 0.46F, 0), Point(0.49F, 0.49F, 0), Point(0.51F, 0.51F, 0), Point(0.51F, 0.51F, 0), Point(0.48F, 0.48F, 0), Point(0.40F, 0.40F, 0), Point(0.31F, 0.31F, 0), Point(0.23F, 0.23F, 0), Point(0.13F, 0.13F, 0), Point(0.04F, 0.04F, 0), Point(-0.05F, -0.05F, 0))
        val pureCircle4 = Gesture(pureCirclePoints4, "pureCircle")
        gestureTemplates.add(pureCircle4)

        // pureCircle five
        val pureCirclePoints5 = listOf(Point(-0.16F, -0.16F, 0), Point(-0.27F, -0.27F, 0), Point(-0.36F, -0.36F, 0), Point(-0.43F, -0.43F, 0), Point(-0.47F, -0.47F, 0), Point(-0.49F, -0.49F, 0), Point(-0.52F, -0.52F, 0), Point(-0.52F, -0.52F, 0), Point(-0.47F, -0.47F, 0), Point(-0.43F, -0.43F, 0), Point(-0.38F, -0.38F, 0), Point(-0.29F, -0.29F, 0), Point(-0.19F, -0.19F, 0), Point(-0.08F, -0.08F, 0), Point(0.03F, 0.03F, 0), Point(0.14F, 0.14F, 0), Point(0.24F, 0.24F, 0), Point(0.32F, 0.32F, 0), Point(0.37F, 0.37F, 0), Point(0.42F, 0.42F, 0), Point(0.46F, 0.46F, 0), Point(0.48F, 0.48F, 0), Point(0.48F, 0.48F, 0), Point(0.48F, 0.48F, 0), Point(0.46F, 0.46F, 0), Point(0.40F, 0.40F, 0), Point(0.32F, 0.32F, 0), Point(0.22F, 0.22F, 0), Point(0.11F, 0.11F, 0), Point(0.00F, 0.00F, 0), Point(-0.10F, -0.10F, 0), Point(-0.20F, -0.20F, 0))
        val pureCircle5 = Gesture(pureCirclePoints5, "pureCircle")
        gestureTemplates.add(pureCircle5)

        // pureCircle six
        val pureCirclePoints6 = listOf(Point(0.31F, 0.31F, 0), Point(0.33F, 0.33F, 0), Point(0.29F, 0.29F, 0), Point(0.23F, 0.23F, 0), Point(0.17F, 0.17F, 0), Point(0.10F, 0.10F, 0), Point(0.01F, 0.01F, 0), Point(-0.10F, -0.10F, 0), Point(-0.18F, -0.18F, 0), Point(-0.24F, -0.24F, 0), Point(-0.30F, -0.30F, 0), Point(-0.33F, -0.33F, 0), Point(-0.37F, -0.37F, 0), Point(-0.39F, -0.39F, 0), Point(-0.39F, -0.39F, 0), Point(-0.39F, -0.39F, 0), Point(-0.37F, -0.37F, 0), Point(-0.34F, -0.34F, 0), Point(-0.29F, -0.29F, 0), Point(-0.22F, -0.22F, 0), Point(-0.13F, -0.13F, 0), Point(-0.03F, -0.03F, 0), Point(0.08F, 0.08F, 0), Point(0.18F, 0.18F, 0), Point(0.27F, 0.27F, 0), Point(0.32F, 0.32F, 0), Point(0.35F, 0.35F, 0), Point(0.39F, 0.39F, 0), Point(0.40F, 0.40F, 0), Point(0.38F, 0.38F, 0), Point(0.35F, 0.35F, 0), Point(0.33F, 0.33F, 0))
        val pureCircle6 = Gesture(pureCirclePoints6, "pureCircle")
        gestureTemplates.add(pureCircle6)

        Log.d("GESTURE", "set up templates")
        for (temp in gestureTemplates) {
            Log.d("GESTURE", "name: ${temp.Name}")
        }

    }

    //what to do when it is detected
    private fun onUndoGestureDetected() {
            Handler(Looper.getMainLooper()).postDelayed({
                //listenerStateSaved.value = true
                viewModel.undo();
                viewModel.undo();
            }, 600)
        //viewModel.undo()
        Toast.makeText(this, "Undo action detected!", Toast.LENGTH_SHORT).show()
    }

    private fun onRedoGestureDetected() {
        viewModel.redo()
        Handler(Looper.getMainLooper()).postDelayed({
            //listenerStateSaved.value = true
            viewModel.undo();
        }, 600)
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

    //this is launched after OnCreate(), actually. This is just more declaring that had to be done when other stuff was
    override fun onStart() {
        super.onStart()

        with(binding.editorToolbar) {
            switchActivePen.setOnCheckedChangeListener { _, isChecked ->
                viewModel.enableActivePen(!isChecked)
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

        setUpGestureTemplates()
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
            CurrentBlockId.onError = true;
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
        // colorsAdapter.submitList(colorStates)
        // binding.editorToolbarSheet.toolbarColors.isVisible = colorStates.isNotEmpty()
    }

    //updating the thickness in the toolsheet
    private fun onAvailableThicknessesUpdate(thicknessStates: List<ThicknessState>) {
        // thicknessesAdapter.submitList(thicknessStates)
        // binding.editorToolbarSheet.toolbarThicknesses.isVisible = thicknessStates.isNotEmpty()
    }

    //updating the toolsheet brushes
    private fun onAvailablePenBrushesUpdate(penBrushStates: List<PenBrushState>) {
        penBrushesAdapter.clear()
        if (penBrushStates.isNotEmpty()) {
            // penBrushesAdapter.addAll(penBrushStates.map { getString(it.penBrush.label) })
            // binding.editorToolbarSheet.penBrushDropdown.setSelection(penBrushStates.indexOfFirst(PenBrushState::isSelected))
        }
        penBrushesAdapter.notifyDataSetChanged()
        canGesture = penBrushStates.isNotEmpty()
        Log.d("polly", canGesture.toString())
        binding.editorToolbarSheet.toolbarPenBrushSection.isVisible = penBrushStates.isNotEmpty()
    }

    //this creates the options menu: the drop down, the three dots (i don't think we need but...)
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
//            it.findItem(R.id.editor_menu_convert).isEnabled = partState.isReady
//            it.findItem(R.id.editor_menu_prediction).isEnabled = true
//            it.findItem(R.id.editor_menu_export).isEnabled = partState.isReady
//            it.findItem(R.id.editor_menu_save).isEnabled = partState.isReady
//            it.findItem(R.id.editor_menu_import_file).isEnabled = true
//            it.findItem(R.id.editor_menu_share_file).isEnabled = partState.isReady
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

                viewModel.save()

                intent.putExtra("partId", partState.partId)
                intent.putExtra("isReady", partState.isReady)
                intent.putExtra("partType", partState.partType.toString())
                intent.putExtra("partDate", partState.dateCreated)
                intent.putExtra("partTitle", partState.title)


                startActivity(intent)
                finish()
            }
//            R.id.editor_menu_convert -> viewModel.convertContent()
//            R.id.editor_menu_prediction -> showPredictionSettingsDialog()
//            R.id.editor_menu_export -> onExport(viewModel.getExportMimeTypes())
//            R.id.editor_menu_save -> (partState as? PartState.Loaded)?.let { viewModel.save() }
//            R.id.editor_menu_import_file -> importIInkFileRequest.launch("*/*")
//            R.id.editor_menu_share_file -> (partState as? PartState.Loaded)?.let { onShareFile(it.partId) }
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
        partStateArrayList?.add(state)
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

    /**
     * showGestureHelpDialog()
     * @return Displays a dialog box with information on the different recognizable gestures
     */
    private fun showGestureHelpDialog() {
        // Create and set up the dialog
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.gesture_help_dialog)

        // Set dialog width and height (e.g., 80% of the screen width and wrap content height)
        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.8).toInt(), // 80% width
            (resources.displayMetrics.heightPixels * 0.5).toInt() // 50% width
        )

        // Allow the user to tap outside the dialog to dismiss it
        dialog.setCancelable(true)
        dialog.setCanceledOnTouchOutside(true)

        // References to the UI elements in the dialog
        val gestureImage = dialog.findViewById<ImageView>(R.id.gestureImage)
        val previousButton = dialog.findViewById<Button>(R.id.previousButton)
        val nextButton = dialog.findViewById<Button>(R.id.nextButton)

        // Initialize the image display
        gestureImage.setImageResource(images[currentImageIndex])
        updateButtonVisibility(previousButton, nextButton)

        // Handle "Next" button click
        nextButton.setOnClickListener {
            currentImageIndex++
            gestureImage.setImageResource(images[currentImageIndex])
            updateButtonVisibility(previousButton, nextButton)
        }

        // Handle "Previous" button click
        previousButton.setOnClickListener {
            currentImageIndex--
            gestureImage.setImageResource(images[currentImageIndex])
            updateButtonVisibility(previousButton, nextButton)
        }

        dialog.show() // actually show the dialog box
    }

    // Function to handle hiding/showing buttons based on the image index
    private fun updateButtonVisibility(previousButton: Button, nextButton: Button) {

        // Hide previous button if on the first image
        if (currentImageIndex == 0) {
            previousButton.visibility = Button.GONE
        } else {
            previousButton.visibility = Button.VISIBLE
        }

        // Hide next button if on the last image
        if (currentImageIndex == images.size - 1) {
            nextButton.visibility = Button.GONE
        } else {
            nextButton.visibility = Button.VISIBLE
        }
    }

}
