package com.example.recorder

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Window
import android.view.WindowManager
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.squareup.picasso.Picasso
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.IOException
import java.net.URL


class MainActivity : AppCompatActivity() {

    private val root = android.os.Environment.getExternalStorageDirectory()

    private var mRecorder: MediaRecorder? = null
    private var mPlayer: MediaPlayer? = null
    private var fileName: String? = null
    private val RECORD_AUDIO_REQUEST_CODE = 101
    private var isRecordingAudio = false


    @RequiresApi(Build.VERSION_CODES.O)
    @SuppressLint("MissingPermission")
    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        // making full screen application
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        this.window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN)
        supportActionBar?.hide()
        setContentView(R.layout.activity_main)

        val start:Button = findViewById(R.id.button)
        val stop:Button = findViewById(R.id.button2)
        val play:Button = findViewById(R.id.button3)
        val msg: TextView = findViewById(R.id.textView)

        val signal:Button = findViewById(R.id.signal)
        val img:ImageView = findViewById(R.id.ImageView)

        getPermissionToRecordAudio()

        signal.setOnClickListener {
            val pic = "http://192.168.2.101:5000/get_image"
            runOnUiThread(Runnable {
                Picasso.get().load(pic).into(img) // load image from url
            })

        }

        start.setOnClickListener {
            msg.text = "Recording...."
            startRecording()
        }

        stop.setOnClickListener {
            msg.text = "Not Recording"
            stopRecording()
        }

        play.setOnClickListener {

            val MEDIA_TYPE_AUDIO = "audio/wav".toMediaType()

            Toast.makeText(this, "converted to bytes", Toast.LENGTH_SHORT).show()

            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("audio", "recording.m4a",
                    File(root.absolutePath + "/AudioRecord/" + "recording.m4a").asRequestBody(MEDIA_TYPE_AUDIO))
                .build()

            val client = OkHttpClient()

            val request = Request.Builder()
                .url("http://141.64.162.246:5000/post")
                .post(requestBody)
                .build()

            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    msg.text = "not found"
                }
                override fun onResponse(call: Call, response: Response) {
                    runOnUiThread(Runnable {
                        msg.text = response.body!!.string()
                    })
                }
            })
            startPlaying()
        }
    }

    private fun startRecording() {

        val file = File(root.absolutePath + "/AudioRecord/")
        if (!file.exists()) {
            file.mkdirs()
        }
        fileName = root.absolutePath + "/AudioRecord/" + "recording.m4a"
        Log.d("filename", fileName!!)

        isRecordingAudio = true
        mRecorder = MediaRecorder()
        mRecorder!!.setAudioSource(MediaRecorder.AudioSource.MIC)
        mRecorder!!.setAudioSamplingRate(16000)
        mRecorder!!.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
        //mRecorder!!.setAudioEncodingBitRate(206113)
        mRecorder!!.setOutputFile(fileName)
        mRecorder!!.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_WB)

        try {
            mRecorder!!.prepare()
            mRecorder!!.start()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }


    @RequiresApi(Build.VERSION_CODES.O)
    private fun stopRecording() {
        try {
            mRecorder!!.stop()
            mRecorder!!.release()

        } catch (e: Exception) {
            e.printStackTrace()
        }
        mRecorder = null
        //showing the play button
        Toast.makeText(this, "Recording saved successfully.", Toast.LENGTH_SHORT).show()
    }

    private fun startPlaying() {
        mPlayer = MediaPlayer()
        try {
            mPlayer!!.setDataSource(fileName)
            mPlayer!!.prepare()
            mPlayer!!.start()
        } catch (e: IOException) {
            Log.e("LOG_TAG", "prepare() failed")
        }
    }

    private fun getPermissionToRecordAudio() {
        // 1) Use the support library version ActivityCompat.checkSelfPermission(...) to avoid checking the build version since Context.checkSelfPermission(...) is only available in Marshmallow
        // 2) Always check for permission (even if permission has already been granted) since the user can revoke permissions at any time through Settings
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED
            || ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
            || ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.RECORD_AUDIO, Manifest.permission.WRITE_EXTERNAL_STORAGE), RECORD_AUDIO_REQUEST_CODE)
        }
    }

    // Callback with the request from calling requestPermissions(...)
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        // Make sure it's our original READ_CONTACTS request
        if (requestCode == RECORD_AUDIO_REQUEST_CODE) {
            if (grantResults.size == 3 && grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED && grantResults[2] == PackageManager.PERMISSION_GRANTED) {
                //Toast.makeText(this, "Record Audio permission granted", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "You must give permissions to use this app. App is exiting.", Toast.LENGTH_SHORT).show()
                finishAffinity()
            }
        }
    }

}