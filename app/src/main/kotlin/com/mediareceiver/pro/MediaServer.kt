package com.mediareceiver.pro

import android.content.Context
import java.io.*
import java.net.*
import java.text.SimpleDateFormat
import java.util.*

class MediaServer(private val context: Context) {
    private var serverSocket: ServerSocket? = null
    private var isRunning = false
    private var port = 8080
    private var visitorCount = 0
    private var fileCount = 0
    
    private fun readHtmlFromAssets(): String {
        return try {
            val inputStream = context.assets.open("htmlapp/root/index.html")
            val reader = BufferedReader(InputStreamReader(inputStream, "UTF-8"))
            val stringBuilder = StringBuilder()
            var line: String?
            
            while (reader.readLine().also { line = it } != null) {
                stringBuilder.append(line).append("\n")
            }
            reader.close()
            stringBuilder.toString()
        } catch (e: Exception) {
            // إذا فشل تحميل الـ HTML، استخدم الافتراضي
            getDefaultHtmlTemplate()
        }
    }
    
    private fun getDefaultHtmlTemplate(): String {
        return """
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset='UTF-8'>
            <meta name='viewport' content='width=device-width, initial-scale=1.0'>
            <title>Media Receiver Pro</title>
            <style>
                body { 
                    font-family: Arial, sans-serif; 
                    margin: 0; 
                    padding: 20px; 
                    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
                    color: white;
                }
                .container { 
                    max-width: 500px; 
                    margin: 50px auto; 
                    background: rgba(255,255,255,0.1); 
                    padding: 30px; 
                    border-radius: 15px; 
                    text-align: center; 
                    backdrop-filter: blur(10px);
                }
                h1 { 
                    color: #fff; 
                    margin-bottom: 30px;
                }
                .upload-form {
                    background: rgba(255,255,255,0.2);
                    padding: 20px;
                    border-radius: 10px;
                    margin: 20px 0;
                }
                input, button { 
                    width: 100%; 
                    padding: 15px; 
                    margin: 10px 0; 
                    border: none;
                    border-radius: 8px;
                }
                input {
                    background: rgba(255,255,255,0.9);
                }
                button { 
                    background: #4CAF50; 
                    color: white; 
                    border: none; 
                    cursor: pointer; 
                    font-size: 16px;
                    font-weight: bold;
                }
                button:hover {
                    background: #45a049;
                }
                .info { 
                    margin-top: 15px; 
                    padding: 15px; 
                    background: rgba(255,255,255,0.2); 
                    border-radius: 8px; 
                    text-align: left;
                }
                .stats {
                    display: flex;
                    justify-content: space-around;
                    margin: 20px 0;
                }
                .stat-item {
                    background: rgba(255,255,255,0.2);
                    padding: 15px;
                    border-radius: 8px;
                    flex: 1;
                    margin: 0 5px;
                }
            </style>
        </head>
        <body>
            <div class='container'>
                <h1>📱 Media Receiver Pro</h1>
                <div class='upload-form'>
                    <form method='POST' enctype='multipart/form-data'>
                        <input type='file' name='file' accept='image/*,video/*,audio/*,.pdf,.doc,.docx' required>
                        <button type='submit'>📤 Upload File</button>
                    </form>
                </div>
                <div class='stats'>
                    <div class='stat-item'>
                        <strong>👤 Visitors:</strong><br>
                        <span style='font-size: 24px;'>$visitorCount</span>
                    </div>
                    <div class='stat-item'>
                        <strong>📁 Files:</strong><br>
                        <span style='font-size: 24px;'>$fileCount</span>
                    </div>
                </div>
                <div class='info'>
                    <strong>🌐 Your IP:</strong> {{CLIENT_IP}}<br>
                    <strong>🕒 Time:</strong> {{CURRENT_TIME}}<br>
                    <strong>🔢 Visitor #:</strong> {{VISITOR_NUMBER}}
                </div>
            </div>
        </body>
        </html>
        """.trimIndent()
    }
    
    fun startServer() {
        Thread {
            try {
                serverSocket = ServerSocket(port)
                isRunning = true
                
                (context as MainActivity).updateLog("✅ Local server started on port $port")
                (context as MainActivity).updateServerStatus(true)
                
                while (isRunning) {
                    try {
                        val clientSocket = serverSocket!!.accept()
                        handleClient(clientSocket)
                    } catch (e: IOException) {
                        if (isRunning) {
                            (context as MainActivity).updateLog("❌ Client connection error: ${e.message}")
                        }
                    }
                }
                
            } catch (e: IOException) {
                (context as MainActivity).updateLog("❌ Failed to start server: ${e.message}")
                (context as MainActivity).updateServerStatus(false)
            }
        }.start()
    }
    
    private fun handleClient(clientSocket: Socket) {
        Thread {
            try {
                val reader = BufferedReader(InputStreamReader(clientSocket.getInputStream()))
                val output = clientSocket.getOutputStream()
                val writer = PrintWriter(output)
                
                val requestLine = reader.readLine()
                if (requestLine == null) return@Thread
                
                val clientIP = clientSocket.inetAddress.hostAddress
                visitorCount++
                (context as MainActivity).updateVisitorCount(visitorCount)
                
                (context as MainActivity).updateLog("👤 Visitor #$visitorCount from: $clientIP")
                
                when {
                    requestLine.startsWith("GET") -> handleGetRequest(writer, clientIP)
                    requestLine.startsWith("POST") -> handlePostRequest(reader, writer, clientIP)
                    else -> sendErrorResponse(writer, 405, "Method Not Allowed")
                }
                
                clientSocket.close()
                
            } catch (e: IOException) {
                (context as MainActivity).updateLog("❌ Error handling client: ${e.message}")
            }
        }.start()
    }
    
    private fun handleGetRequest(writer: PrintWriter, clientIP: String) {
        try {
            var htmlContent = readHtmlFromAssets()
            
            // استبدال المتغيرات في الـ HTML
            htmlContent = htmlContent.replace("{{CLIENT_IP}}", clientIP)
            htmlContent = htmlContent.replace("{{VISITOR_NUMBER}}", visitorCount.toString())
            htmlContent = htmlContent.replace("{{TOTAL_FILES}}", fileCount.toString())
            htmlContent = htmlContent.replace("{{CURRENT_TIME}}", 
                SimpleDateFormat("HH:mm:ss").format(Date()))
            
            writer.println("HTTP/1.1 200 OK")
            writer.println("Content-Type: text/html; charset=utf-8")
            writer.println("Content-Length: ${htmlContent.toByteArray(Charsets.UTF_8).size}")
            writer.println()
            writer.println(htmlContent)
            writer.flush()
            
            (context as MainActivity).updateLog("📄 Sent HTML page to $clientIP")
            
        } catch (e: Exception) {
            (context as MainActivity).updateLog("❌ Error loading HTML: ${e.message}")
            sendDefaultPage(writer, clientIP)
        }
    }
    
    // ... باقي الدوال تبقى كما هي مع تعديلات Kotlin
}
