package com.jarvis.assistant

import android.Manifest
import android.app.Activity
import android.app.AlarmClock
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.view.Gravity
import android.view.ViewGroup
import android.widget.*
import android.graphics.Color
import android.graphics.Typeface
import android.text.InputType
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import kotlin.concurrent.thread

class MainActivity : ComponentActivity(), TextToSpeech.OnInitListener {
    private lateinit var root: LinearLayout
    private lateinit var transcript: TextView
    private lateinit var input: EditText
    private lateinit var status: TextView
    private lateinit var tts: TextToSpeech
    private var apiKey: String = ""
    private var voiceLanguage = "en-US"
    private val history = mutableListOf<Pair<String, String>>()

    private val micPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) startListening() else sayStatus("Microphone permission is needed for voice input.")
    }
    private val speechResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spoken = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
            if (!spoken.isNullOrBlank()) {
                input.setText(spoken)
                sendMessage(spoken)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        getPreferences(MODE_PRIVATE).getString("gemini_key", "")?.let { apiKey = it }
        tts = TextToSpeech(this, this)
        buildUi()
    }

    private fun buildUi() {
        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(22, 28, 22, 18)
            setBackgroundColor(Color.rgb(5, 10, 22))
        }
        val title = TextView(this).apply {
            text = "J.A.R.V.I.S"
            textSize = 30f
            setTextColor(Color.rgb(65, 205, 255))
            typeface = Typeface.create("sans-serif", Typeface.BOLD)
            gravity = Gravity.CENTER
        }
        val subtitle = TextView(this).apply {
            text = "PERSONAL AI ASSISTANT"
            textSize = 11f
            letterSpacing = 0.22f
            setTextColor(Color.LTGRAY)
            gravity = Gravity.CENTER
        }
        status = TextView(this).apply {
            text = "SYSTEM ONLINE • READY"
            textSize = 12f
            setTextColor(Color.rgb(100, 230, 180))
            gravity = Gravity.CENTER
            setPadding(0, 16, 0, 14)
        }
        transcript = TextView(this).apply {
            text = "JARVIS: Hello, Sir. I am ready.\n"
            textSize = 16f
            setTextColor(Color.WHITE)
            setPadding(14, 14, 14, 14)
        }
        val scroll = ScrollView(this).apply {
            setBackgroundColor(Color.rgb(13, 22, 40))
            addView(transcript)
        }
        input = EditText(this).apply {
            hint = "Ask JARVIS anything..."
            setHintTextColor(Color.GRAY)
            setTextColor(Color.WHITE)
            textSize = 16f
            setSingleLine(false)
            setPadding(12, 10, 12, 10)
            setBackgroundColor(Color.rgb(22, 34, 55))
        }
        val send = makeButton("SEND") { sendMessage(input.text.toString()) }
        val mic = makeButton("🎙  SPEAK") {
            if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) startListening()
            else micPermission.launch(Manifest.permission.RECORD_AUDIO)
        }
        val language = makeButton("LANGUAGE: ENGLISH / हिंदी") {
            voiceLanguage = if (voiceLanguage == "en-US") "hi-IN" else "en-US"
            sayStatus(if (voiceLanguage == "hi-IN") "Hindi voice input selected" else "English voice input selected")
        }
        val settings = makeButton("GEMINI API KEY") { showApiKeyDialog() }
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            addView(mic, LinearLayout.LayoutParams(0, 54, 1f).apply { rightMargin = 8 })
            addView(send, LinearLayout.LayoutParams(0, 54, 1f))
        }
        root.addView(title)
        root.addView(subtitle)
        root.addView(status)
        root.addView(scroll, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
        root.addView(input, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 58).apply { topMargin = 12 })
        root.addView(row, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 54).apply { topMargin = 8 })
        root.addView(language, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 48).apply { topMargin = 8 })
        root.addView(settings, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 48).apply { topMargin = 6 })
        setContentView(root)
    }

    private fun makeButton(label: String, action: () -> Unit): Button =
        Button(this).apply {
            text = label
            textSize = 12f
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.rgb(15, 91, 130))
            setOnClickListener { action() }
        }

    private fun append(who: String, message: String) {
        runOnUiThread {
            transcript.append("\n$who: $message\n")
            (transcript.parent as? ScrollView)?.post { (transcript.parent as ScrollView).fullScroll(ScrollView.FOCUS_DOWN) }
        }
    }

    private fun sayStatus(message: String) {
        runOnUiThread { status.text = message.uppercase(Locale.getDefault()) }
    }

    private fun speak(message: String) {
        val locale = if (voiceLanguage == "hi-IN") Locale("hi", "IN") else Locale.US
        tts.language = locale
        tts.setPitch(0.82f)
        tts.setSpeechRate(0.92f)
        tts.speak(message, TextToSpeech.QUEUE_FLUSH, null, "jarvis_reply")
    }

    private fun startListening() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, voiceLanguage)
            putExtra(RecognizerIntent.EXTRA_PROMPT, if (voiceLanguage == "hi-IN") "JARVIS सुन रहा है..." else "JARVIS is listening...")
        }
        try { speechResult.launch(intent) }
        catch (_: Exception) { sayStatus("Speech recognition is not available on this phone.") }
    }

    private fun sendMessage(message: String) {
        val clean = message.trim()
        if (clean.isEmpty()) { sayStatus("Type or speak a message first."); return }
        input.setText("")
        append("YOU", clean)
        val localReply = handlePhoneCommand(clean)
        if (localReply != null) {
            append("JARVIS", localReply)
            speak(localReply)
            return
        }
        if (apiKey.isBlank()) {
            val reply = "Please add your Gemini API key using the API KEY button. Phone commands can work without it."
            append("JARVIS", reply); speak(reply); return
        }
        sayStatus("JARVIS IS THINKING...")
        thread {
            try {
                val answer = askGemini(clean)
                history.add(clean to answer)
                append("JARVIS", answer)
                speak(answer)
                sayStatus("SYSTEM ONLINE • READY")
            } catch (e: Exception) {
                val error = "I couldn't connect to Gemini. Check your internet and API key, then try again."
                append("JARVIS", error); speak(error); sayStatus("CONNECTION ERROR")
            }
        }
    }

    private fun handlePhoneCommand(message: String): String? {
        val m = message.lowercase(Locale.ROOT)
        return try {
            when {
                m.contains("open youtube") || m.contains("यूट्यूब खोल") -> {
                    openApp("com.google.android.youtube"); "Opening YouTube."
                }
                m.contains("open camera") || m.contains("कैमरा खोल") -> {
                    startActivity(Intent("android.media.action.IMAGE_CAPTURE")); "Opening camera."
                }
                m.contains("set alarm") || m.contains("alarm") || m.contains("अलार्म") -> {
                    val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
                        putExtra(AlarmClock.EXTRA_MESSAGE, "JARVIS alarm")
                        putExtra(AlarmClock.EXTRA_SKIP_UI, false)
                    }
                    startActivity(intent); "Opening the alarm screen. Please choose the time."
                }
                m.contains("open settings") || m.contains("सेटिंग") -> {
                    startActivity(Intent(android.provider.Settings.ACTION_SETTINGS)); "Opening phone settings."
                }
                else -> null
            }
        } catch (_: Exception) { "Sorry, I couldn't open that app or phone feature." }
    }

    private fun openApp(packageName: String) {
        val launch = packageManager.getLaunchIntentForPackage(packageName)
        if (launch != null) startActivity(launch)
        else startActivity(Intent(Intent.ACTION_VIEW, android.net.Uri.parse("https://www.youtube.com")))
    }

    private fun askGemini(message: String): String {
        val endpoint = URL("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=$apiKey")
        val connection = (endpoint.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 20000
            readTimeout = 30000
            doOutput = true
            setRequestProperty("Content-Type", "application/json")
        }
        val body = JSONObject().apply {
            put("system_instruction", JSONObject().put("parts", JSONArray().put(JSONObject().put("text",
                "You are JARVIS, a helpful, calm personal AI assistant. Reply in the same language as the user (Hindi or English). Be concise, truthful, and friendly. Never claim you performed an action unless it happened."))))
            put("contents", JSONArray().put(JSONObject().put("role", "user").put("parts", JSONArray().put(JSONObject().put("text", message)))))
            put("generationConfig", JSONObject().put("temperature", 0.7))
        }
        connection.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
        val code = connection.responseCode
        val stream = if (code in 200..299) connection.inputStream else connection.errorStream
        val response = stream.bufferedReader().use { it.readText() }
        connection.disconnect()
        if (code !in 200..299) throw IllegalStateException("Gemini HTTP $code")
        val json = JSONObject(response)
        return json.getJSONArray("candidates").getJSONObject(0)
            .getJSONObject("content").getJSONArray("parts").getJSONObject(0).getString("text")
    }

    private fun showApiKeyDialog() {
        val field = EditText(this).apply {
            hint = "Paste your Gemini API key"
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            setText(apiKey)
        }
        AlertDialogBuilder.show(this, field) { value ->
            apiKey = value.trim()
            getPreferences(MODE_PRIVATE).edit().putString("gemini_key", apiKey).apply()
            sayStatus(if (apiKey.isBlank()) "API key cleared" else "Gemini key saved on this device")
        }
    }

    override fun onInit(statusCode: Int) {
        if (statusCode == TextToSpeech.SUCCESS) {
            tts.language = Locale.US
            tts.setPitch(0.82f)
        }
    }

    override fun onDestroy() {
        if (::tts.isInitialized) tts.shutdown()
        super.onDestroy()
    }
}

private object AlertDialogBuilder {
    fun show(activity: Activity, field: EditText, save: (String) -> Unit) {
        android.app.AlertDialog.Builder(activity)
            .setTitle("Gemini API key")
            .setMessage("Create a free key in Google AI Studio. Keep it private; it is saved only on this phone in app preferences.")
            .setView(field)
            .setPositiveButton("Save") { _, _ -> save(field.text.toString()) }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
