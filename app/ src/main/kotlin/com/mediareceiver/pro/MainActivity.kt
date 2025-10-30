package com.mediareceiver.pro

import android.content.ClipboardManager
import android.content.ClipData
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {
    
    private lateinit var mediaServer: MediaServer
    private lateinit var startButton: Button
    private lateinit var stopButton: Button
    private lateinit var copyButton: Button
    private lateinit var folderButton: Button
    private lateinit var serverStatus: TextView
    private lateinit var tunnelStatus: TextView
    private lateinit var urlText: TextView
    private lateinit var visitorCount: TextView
    private lateinit var fileCount: TextView
    private lateinit var logText: TextView
    private lateinit var clipboard: ClipboardManager
    private var publicUrl: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initializeViews()
        setupClickListeners()

        clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager

        updateLog("📱 Media Receiver Pro Started (Kotlin)")
        updateLog("📍 Storage: ${Environment.getExternalStorageDirectory().absolutePath}/MediaReceiverPro")
        updateLog("💡 Press 'Start Server' to begin")
    }

    private fun initializeViews() {
        startButton = findViewById(R.id.startButton)
        stopButton = findViewById(R.id.stopButton)
        copyButton = findViewById(R.id.copyButton)
        folderButton = findViewById(R.id.folderButton)

        serverStatus = findViewById(R.id.serverStatus)
        tunnelStatus = findViewById(R.id.tunnelStatus)
        urlText = findViewById(R.id.urlText)
        visitorCount = findViewById(R.id.visitorCount)
        fileCount = findViewById(R.id.fileCount)
        logText = findViewById(R.id.logText)

        updateButtonStates(false)
    }

    private fun setupClickListeners() {
        startButton.setOnClickListener { startServer() }
        stopButton.setOnClickListener { stopServer() }
        copyButton.setOnClickListener { copyUrlToClipboard() }
        folderButton.setOnClickListener { openStorageFolder() }
        
        urlText.setOnClickListener {
            if (publicUrl.isNotEmpty()) {
                openUrlInBrowser(publicUrl)
            }
        }
    }

    private fun startServer() {
        if (::mediaServer.isInitialized && mediaServer.isRunning()) {
            showToast("Server is already running")
            return
        }

        updateLog("🚀 Starting server...")
        mediaServer = MediaServer(this)
        mediaServer.startServer()
        updateButtonStates(true)
    }

    private fun stopServer() {
        if (::mediaServer.isInitialized) {
            mediaServer.stopServer()
        }
        updateButtonStates(false)
        updateServerStatus(false)
        updateTunnelStatus(false)
    }

    private fun updateButtonStates(serverRunning: Boolean) {
        startButton.isEnabled = !serverRunning
        stopButton.isEnabled = serverRunning
        copyButton.isEnabled = serverRunning && publicUrl.isNotEmpty()

        startButton.alpha = if (serverRunning) 0.5f else 1.0f
        stopButton.alpha = if (serverRunning) 1.0f else 0.5f
        copyButton.alpha = if (serverRunning && publicUrl.isNotEmpty()) 1.0f else 0.5f
    }

    private fun copyUrlToClipboard() {
        if (publicUrl.isNotEmpty()) {
            val clip = ClipData.newPlainText("Public URL", publicUrl)
            clipboard.setPrimaryClip(clip)
            showToast("URL copied to clipboard")
            updateLog("📋 URL copied: $publicUrl")
        } else {
            showToast("No public URL available")
        }
    }

    private fun openStorageFolder() {
        val storageDir = FileUtils.getMediaStorageDir()
        val intent = Intent(Intent.ACTION_VIEW)
        val uri = Uri.fromFile(storageDir)
        intent.setDataAndType(uri, "resource/folder")

        if (intent.resolveActivity(packageManager) != null) {
            startActivity(intent)
        } else {
            showToast("No file manager app found")
            updateLog("📁 Storage path: ${storageDir.absolutePath}")
        }
    }

    private fun openUrlInBrowser(url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(intent)
            updateLog("🌐 Opening URL in browser: $url")
        } catch (e: Exception) {
            showToast("Cannot open browser")
            updateLog("❌ Browser error: ${e.message}")
        }
    }

    fun updateServerStatus(running: Boolean) {
        runOnUiThread {
            if (running) {
                serverStatus.text = "RUNNING"
                serverStatus.setTextColor(getColor(android.R.color.holo_green_dark))
            } else {
                serverStatus.text = "STOPPED"
                serverStatus.setTextColor(getColor(android.R.color.holo_red_dark))
            }
        }
    }

    fun updateTunnelStatus(active: Boolean) {
        runOnUiThread {
            if (active) {
                tunnelStatus.text = "ACTIVE"
                tunnelStatus.setTextColor(getColor(android.R.color.holo_green_dark))
            } else {
                tunnelStatus.text = "DISABLED"
                tunnelStatus.setTextColor(getColor(android.R.color.holo_red_dark))
            }
        }
    }

    fun updatePublicUrl(url: String) {
        runOnUiThread {
            publicUrl = url
            urlText.text = url
            updateButtonStates(::mediaServer.isInitialized && mediaServer.isRunning())
        }
    }

    fun updateVisitorCount(count: Int) {
        runOnUiThread {
            visitorCount.text = count.toString()
        }
    }

    fun updateFileCount(count: Int) {
        runOnUiThread {
            fileCount.text = count.toString()
        }
    }

    fun updateLog(message: String) {
        runOnUiThread {
            val timestamp = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
            val logEntry = "[$timestamp] $message\n"

            var currentLog = logText.text.toString()
            if (currentLog.length > 5000) {
                currentLog = currentLog.substring(0, 3000) + "\n... (log truncated)\n"
            }

            logText.text = logEntry + currentLog
        }
    }

    private fun showToast(message: String) {
        runOnUiThread {
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::mediaServer.isInitialized) {
            mediaServer.stopServer()
        }
    }
}
