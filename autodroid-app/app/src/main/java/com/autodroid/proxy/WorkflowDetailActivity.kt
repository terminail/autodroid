// WorkflowDetailActivity.kt
package com.autodroid.proxy

import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.gson.Gson
import com.google.gson.JsonElement
import java.util.*
import java.util.concurrent.Executors

class WorkflowDetailActivity : AppCompatActivity() {
    companion object {
        private const val TAG = "WorkflowDetailActivity"
    }

    private lateinit var gson: Gson

    private lateinit var workflowNameTextView: TextView
    private lateinit var workflowDescriptionTextView: TextView
    private lateinit var workflowPackageTextView: TextView
    private lateinit var workflowVersionTextView: TextView

    private lateinit var startButton: Button
    private lateinit var stopButton: Button
    private lateinit var toggleParamsButton: Button
    private lateinit var paramsContainer: LinearLayout
    private lateinit var executionInfoContainer: LinearLayout

    private lateinit var statusTextView: TextView
    private lateinit var startTimeTextView: TextView
    private lateinit var endTimeTextView: TextView
    private lateinit var durationTextView: TextView

    private var workflow: MutableMap<String, Any>? = null
    private val executorService = Executors.newSingleThreadExecutor()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_workflow_detail)

        // Initialize Gson
        gson = Gson()

        // Get workflow data from intent
        val workflowJson = intent.getStringExtra("workflow")
        if (workflowJson != null) {
            val workflowElement: JsonElement = gson.fromJson(workflowJson, JsonElement::class.java)
            workflow = gson.fromJson(workflowElement, Map::class.java) as MutableMap<String, Any>
        } else {
            Toast.makeText(this, "No workflow data provided", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Initialize UI components
        initUI()

        // Populate workflow details
        populateWorkflowDetails()

        // Set up click listeners
        setupClickListeners()
    }

    private fun initUI() {
        // Workflow info
        workflowNameTextView = findViewById(R.id.workflow_detail_name)
        workflowDescriptionTextView = findViewById(R.id.workflow_detail_description)
        workflowPackageTextView = findViewById(R.id.workflow_detail_package)
        workflowVersionTextView = findViewById(R.id.workflow_detail_version)

        // Execution controls
        startButton = findViewById(R.id.workflow_detail_start_button)
        stopButton = findViewById(R.id.workflow_detail_stop_button)

        // Parameter controls
        toggleParamsButton = findViewById(R.id.workflow_detail_toggle_params_button)
        paramsContainer = findViewById(R.id.workflow_detail_params_container)

        // Execution info
        executionInfoContainer = findViewById(R.id.workflow_detail_execution_info)
        statusTextView = findViewById(R.id.workflow_detail_status)
        startTimeTextView = findViewById(R.id.workflow_detail_start_time)
        endTimeTextView = findViewById(R.id.workflow_detail_end_time)
        durationTextView = findViewById(R.id.workflow_detail_duration)
    }

    private fun populateWorkflowDetails() {
        if (workflow == null) return

        // Set workflow basic info
        workflowNameTextView.text = workflow?.getOrDefault("name", "Unknown Workflow").toString()
        workflowDescriptionTextView.text = workflow?.getOrDefault("description", "No description available").toString()

        // Set metadata info
        val metadata = workflow?.getOrDefault("metadata", HashMap<String, Any>()) as Map<String, Any>
        workflowPackageTextView.text = "Package: ${metadata.getOrDefault("app_package", "Unknown")}"
        workflowVersionTextView.text = "Version Constraint: ${metadata.getOrDefault("version_constraint", "*")}"
    }

    private fun setupClickListeners() {
        // Toggle parameters button
        toggleParamsButton.setOnClickListener {
            if (paramsContainer.visibility == View.GONE) {
                // Show parameters and generate UI
                generateDynamicParametersUI(paramsContainer, workflow)
                paramsContainer.visibility = View.VISIBLE
                toggleParamsButton.text = "Hide Parameters"
            } else {
                // Hide parameters
                paramsContainer.visibility = View.GONE
                toggleParamsButton.text = "Show Parameters"
            }
        }

        // Start button
        startButton.setOnClickListener {
            // Collect parameters
            val parameters = collectDynamicParameterValues(paramsContainer)

            // Start workflow execution
            startWorkflowExecution(parameters)
        }

        // Stop button
        stopButton.setOnClickListener {
            stopWorkflowExecution()
        }

        // Back button
        val backButton: Button = findViewById(R.id.workflow_detail_back_button)
        backButton.setOnClickListener {
            finish()
        }
    }

    private fun generateDynamicParametersUI(paramsContainer: LinearLayout, workflow: MutableMap<String, Any>?) {
        // Clear any existing views
        paramsContainer.removeAllViews()

        // Check if workflow has parameter definitions
        if (workflow?.containsKey("parameter_definitions") == true) {
            val paramDefinitions = workflow["parameter_definitions"] as List<Map<String, Any>>

            for (paramDef in paramDefinitions) {
                // Create parameter UI based on definition
                val paramView = createParameterView(paramDef, workflow)
                if (paramView != null) {
                    paramsContainer.addView(paramView)
                }
            }
        } else {
            // No parameter definitions, show message
            val noParamsText = TextView(this)
            noParamsText.text = "No parameters defined for this workflow"
            noParamsText.textSize = 14f
            noParamsText.setTextColor(resources.getColor(android.R.color.darker_gray))
            noParamsText.setPadding(0, 8, 0, 8)
            paramsContainer.addView(noParamsText)
        }
    }

    private fun createParameterView(paramDef: Map<String, Any>, workflow: MutableMap<String, Any>): View? {
        // Get parameter definition details
        val paramName = paramDef["name"].toString()
        val paramType = paramDef.getOrDefault("type", "string").toString()
        val defaultValue = paramDef["default"]
        val description = paramDef.getOrDefault("description", "").toString()

        // Create parameter container
        val paramContainer = LinearLayout(this)
        paramContainer.orientation = LinearLayout.VERTICAL
        paramContainer.setPadding(0, 0, 0, 16)

        // Create parameter label
        val paramLabel = TextView(this)
        paramLabel.text = if (description.isEmpty()) paramName else "$paramName - $description"
        paramLabel.textSize = 14f
        paramLabel.setTypeface(paramLabel.typeface, android.graphics.Typeface.BOLD)
        paramLabel.setTextColor(resources.getColor(android.R.color.black))
        paramLabel.setPadding(0, 0, 0, 4)
        paramContainer.addView(paramLabel)

        // Create input based on parameter type
        val inputView: View? = when (paramType.lowercase(Locale.getDefault())) {
            "string" -> createStringInput(paramName, defaultValue, workflow)
            "number" -> createNumberInput(paramName, defaultValue, workflow)
            "boolean" -> createBooleanInput(paramName, defaultValue, workflow)
            "enum" -> createEnumInput(paramName, paramDef, defaultValue, workflow)
            else -> createStringInput(paramName, defaultValue, workflow) // Unsupported type, create string input as fallback
        }

        if (inputView != null) {
            paramContainer.addView(inputView)
        }

        return paramContainer
    }

    private fun createStringInput(paramName: String, defaultValue: Any?, workflow: MutableMap<String, Any>): EditText {
        val editText = EditText(this)
        editText.hint = "Enter $paramName"
        editText.setText(if (defaultValue != null) defaultValue.toString() else "")
        editText.inputType = android.text.InputType.TYPE_CLASS_TEXT
        editText.setBackgroundResource(R.drawable.edittext_background)
        editText.setPadding(12, 12, 12, 12)
        editText.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        editText.tag = "param_$paramName"
        return editText
    }

    private fun createNumberInput(paramName: String, defaultValue: Any?, workflow: MutableMap<String, Any>): EditText {
        val editText = EditText(this)
        editText.hint = "Enter $paramName"
        editText.setText(if (defaultValue != null) defaultValue.toString() else "")
        editText.inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
        editText.setBackgroundResource(R.drawable.edittext_background)
        editText.setPadding(12, 12, 12, 12)
        editText.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        editText.tag = "param_$paramName"
        return editText
    }

    private fun createBooleanInput(paramName: String, defaultValue: Any?, workflow: MutableMap<String, Any>): Switch {
        val switchView = Switch(this)
        switchView.isChecked = defaultValue != null && defaultValue as Boolean
        switchView.text = "Enable $paramName"
        switchView.textSize = 14f
        switchView.tag = "param_$paramName"
        return switchView
    }

    private fun createEnumInput(paramName: String, paramDef: Map<String, Any>, defaultValue: Any?, workflow: MutableMap<String, Any>): Spinner {
        val spinner = Spinner(this)

        // Get enum options from parameter definition
        val options = paramDef.getOrDefault("options", ArrayList<String>()) as List<String>
        val adapter = android.widget.ArrayAdapter(this,
            android.R.layout.simple_spinner_item, options)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = adapter

        // Set default value if exists
        if (defaultValue != null) {
            val position = adapter.getPosition(defaultValue.toString())
            if (position >= 0) {
                spinner.setSelection(position)
            }
        }

        spinner.setBackgroundResource(R.drawable.edittext_background)
        spinner.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        spinner.tag = "param_$paramName"
        return spinner
    }

    private fun collectDynamicParameterValues(paramsContainer: LinearLayout): MutableMap<String, Any> {
        val parameters: MutableMap<String, Any> = HashMap()

        // Iterate through all parameter views and collect values
        for (i in 0 until paramsContainer.childCount) {
            val paramContainer = paramsContainer.getChildAt(i)

            if (paramContainer is LinearLayout) {
                for (j in 0 until paramContainer.childCount) {
                    val inputView = paramContainer.getChildAt(j)

                    // Skip labels, only process input views
                    if (inputView is TextView && inputView !is EditText && inputView !is Switch) {
                        continue
                    }

                    // Get parameter name from tag
                    val tag = inputView.tag as String?
                    if (tag != null && tag.startsWith("param_")) {
                        val paramName = tag.substring(6) // Remove "param_" prefix

                        // Get value based on input type
                        val value: Any? = when (inputView) {
                            is EditText -> {
                                val textValue = inputView.text.toString()
                                // Try to parse as number if possible
                                try {
                                    textValue.toDouble()
                                } catch (e: NumberFormatException) {
                                    textValue
                                }
                            }
                            is Switch -> inputView.isChecked
                            is Spinner -> inputView.selectedItem.toString()
                            else -> null
                        }

                        if (value != null) {
                            parameters[paramName] = value
                        }
                    }
                }
            }
        }

        return parameters
    }

    private fun startWorkflowExecution(parameters: Map<String, Any>) {
        // Update UI to show running state
        statusTextView.text = "Running"
        statusTextView.setTextColor(resources.getColor(android.R.color.holo_blue_dark))
        startButton.isEnabled = false
        stopButton.isEnabled = true
        executionInfoContainer.visibility = View.VISIBLE

        // Set start time
        val startTime = java.text.SimpleDateFormat("HH:mm:ss").format(Date())
        startTimeTextView.text = "Start: $startTime"
        endTimeTextView.text = "End: N/A"
        durationTextView.text = "Duration: 00:00:00"

        // Log workflow start with parameters
        Log.d(TAG, "Starting workflow: ${workflow?.get("name")} with parameters: ${parameters.toString()}")

        // Simulate workflow execution
        simulateWorkflowExecution(parameters)
    }

    private fun stopWorkflowExecution() {
        // Update UI to show stopped state
        statusTextView.text = "Stopped"
        statusTextView.setTextColor(resources.getColor(android.R.color.holo_red_dark))
        startButton.isEnabled = true
        stopButton.isEnabled = false

        // Set end time
        val endTime = java.text.SimpleDateFormat("HH:mm:ss").format(Date())
        endTimeTextView.text = "End: $endTime"

        Log.d(TAG, "Stopping workflow: ${workflow?.get("name")}")
    }

    private fun simulateWorkflowExecution(parameters: Map<String, Any>) {
        // Simulate workflow execution by changing status after delay
        executorService.submit {
            try {
                // Simulate running for 5 seconds
                Thread.sleep(5000)

                // Update UI to show completed state
                runOnUiThread {
                    statusTextView.text = "Completed"
                    statusTextView.setTextColor(resources.getColor(android.R.color.holo_green_dark))
                    startButton.isEnabled = true
                    stopButton.isEnabled = false

                    // Set end time
                    val endTime = java.text.SimpleDateFormat("HH:mm:ss").format(Date())
                    endTimeTextView.text = "End: $endTime"

                    // Update duration
                    durationTextView.text = "Duration: 00:00:05"
                }
            } catch (e: InterruptedException) {
                Thread.currentThread().interrupt()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        executorService.shutdownNow()
    }
}