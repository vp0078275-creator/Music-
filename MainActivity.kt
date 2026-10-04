package com.music.two

import android.Manifest
import android.animation.ObjectAnimator
import android.content.pm.PackageManager
import android.database.Cursor
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.animation.LinearInterpolator
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class MainActivity : AppCompatActivity() {

    private val STORAGE_PERMISSION_CODE = 101
    private var mediaPlayer: MediaPlayer? = null
    private var discAnimator: ObjectAnimator? = null
    
    private val audioTitleList = mutableListOf<String>()
    private val audioPathList = mutableListOf<String>()
    private var currentSongIndex = 0
    private var isPlaying = false

    private lateinit var currentSongTitle: TextView
    private lateinit var playButton: Button
    private lateinit var recyclerViewSongs: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        currentSongTitle = findViewById(R.id.currentSongTitle)
        playButton = findViewById(R.id.playButton)
        recyclerViewSongs = findViewById(R.id.recyclerViewSongs)
        val discImageView: ImageView = findViewById(R.id.discImageView)

        // Setup smooth disc rotation animation
        discAnimator = ObjectAnimator.ofFloat(discImageView, "rotation", 0f, 360f).apply {
            duration = 4000
            repeatCount = ObjectAnimator.INFINITE
            interpolator = LinearInterpolator()
        }

        recyclerViewSongs.layoutManager = LinearLayoutManager(this)

        // Check permissions and load songs
        checkAndRequestPermission()

        playButton.setOnClickListener {
            if (audioPathList.isNotEmpty()) {
                if (isPlaying) {
                    pauseMusic()
                } else {
                    playMusic(audioPathList[currentSongIndex], audioTitleList[currentSongIndex])
                }
            } else {
                Toast.makeText(this, "Koi gaana available nahi hai!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun checkAndRequestPermission() {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_AUDIO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

        if (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED) {
            loadAudioFiles()
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(permission), STORAGE_PERMISSION_CODE)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == STORAGE_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permission Granted!", Toast.LENGTH_SHORT).show()
                loadAudioFiles()
            } else {
                Toast.makeText(this, "Permission Denied!", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun loadAudioFiles() {
        val uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.DATA
        )

        val cursor: Cursor? = contentResolver.query(uri, projection, null, null, MediaStore.Audio.Media.TITLE + " ASC")
        cursor?.use {
            val titleColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val dataColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
            while (it.moveToNext()) {
                val title = it.getString(titleColumn)
                val path = it.getString(dataColumn)
                audioTitleList.add(title)
                audioPathList.add(path)
            }
        }

        if (audioPathList.isNotEmpty()) {
            val adapter = SongAdapter(audioTitleList) { position ->
                currentSongIndex = position
                playMusic(audioPathList[currentSongIndex], audioTitleList[currentSongIndex])
            }
            recyclerViewSongs.adapter = adapter
            Toast.makeText(this, "Songs Loaded: ${audioPathList.size}", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Phone mein koi audio file nahi mili!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun playMusic(path: String, title: String) {
        try {
            if (mediaPlayer == null) {
                mediaPlayer = MediaPlayer().apply {
                    setDataSource(path)
                    prepare()
                    start()
                }
            } else {
                mediaPlayer?.reset()
                mediaPlayer?.setDataSource(path)
                mediaPlayer?.prepare()
                mediaPlayer?.start()
            }
            isPlaying = true
            playButton.text = "Pause"
            currentSongTitle.text = title
            discAnimator?.start()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Gaana chalane mein error aaya!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun pauseMusic() {
        mediaPlayer?.pause()
        isPlaying = false
        playButton.text = "Play"
        discAnimator?.pause()
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.release()
        mediaPlayer = null
    }
}
