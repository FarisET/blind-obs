package com.surendramaran.yolov8tflite

import android.content.Intent
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.surendramaran.yolov8tflite.Constants.LABELS_PATH
import com.surendramaran.yolov8tflite.Constants.MODEL_PATH
import com.surendramaran.yolov8tflite.databinding.ActivityVideoSimulationBinding
import kotlin.math.min

class VideoSimulationActivity : AppCompatActivity(), Detector.DetectorListener {
    private lateinit var binding: ActivityVideoSimulationBinding
    private lateinit var detector: Detector
    private var videoUri: Uri? = null
    private var isProcessing = false
    private lateinit var handler: Handler
    private var frameExtractor: Runnable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVideoSimulationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        handler = Handler(Looper.getMainLooper())
        setupDetector()
        setupFilePicker()
        setupControls()
    }

    private fun setupDetector() {
        detector = Detector(
            baseContext,
            MODEL_PATH,
            LABELS_PATH,
            this
        )
    }

    private fun setupFilePicker() {
        binding.btnSelectVideo.setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                type = "video/*"
            }
            startActivityForResult(intent, REQUEST_PICK_VIDEO)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_PICK_VIDEO && resultCode == RESULT_OK) {
            data?.data?.let { uri ->
                videoUri = uri
                setupVideoPlayer(uri)
            }
        }
    }

    private fun setupVideoPlayer(uri: Uri) {
        binding.videoView.setVideoURI(uri)
        binding.videoView.setOnPreparedListener { mediaPlayer ->
            binding.progressBar.visibility = View.GONE
            mediaPlayer.isLooping = true
            binding.videoView.start()
        }
    }

    private fun setupControls() {
        binding.btnToggleProcessing.setOnClickListener {
            isProcessing = !isProcessing
            if (isProcessing) {
                startProcessing()
                binding.btnToggleProcessing.text = "Stop Processing"
            } else {
                stopProcessing()
                binding.btnToggleProcessing.text = "Start Processing"
            }
        }
    }

    private fun startProcessing() {
        videoUri?.let { uri ->
            try {
                val mediaRetriever = MediaMetadataRetriever().apply {
                    setDataSource(this@VideoSimulationActivity, uri)
                }

                frameExtractor = object : Runnable {
                    override fun run() {
                        if (!isProcessing) return

                        try {
                            val currentPos = binding.videoView.currentPosition
                            mediaRetriever.getFrameAtTime(
                                currentPos * 1000L,
                                MediaMetadataRetriever.OPTION_CLOSEST
                            )?.let { rawFrame ->
                                // Convert to ARGB_8888 if needed
                                val convertedBitmap = if (rawFrame.config != Bitmap.Config.ARGB_8888) {
                                    rawFrame.copy(Bitmap.Config.ARGB_8888, false)
                                } else {
                                    rawFrame
                                }

                                // Scale to model input size
                                val scaledBitmap = Bitmap.createScaledBitmap(
                                    convertedBitmap,
                                    detector.inputWidth,
                                    detector.inputHeight,
                                    true
                                )

                                // Recycle temporary bitmaps
                                if (convertedBitmap != rawFrame) {
                                    rawFrame.recycle()
                                }

                                detector.detect(scaledBitmap)
                                scaledBitmap.recycle()
                            }
                        } catch (e: Exception) {
                            Log.e("VideoProcessing", "Frame processing failed", e)
                        }

                        handler.postDelayed(this, (1000 / 30).toLong()) // 30 FPS
                    }
                }
                handler.post(frameExtractor!!)
            } catch (e: Exception) {
                Log.e("VideoProcessing", "Media setup failed", e)
                stopProcessing()
            }
        }
    }

    private fun stopProcessing() {
        isProcessing = false
        frameExtractor?.let { handler.removeCallbacks(it) }
        binding.overlay.clear()
    }

    override fun onEmptyDetect() {
        runOnUiThread {
            binding.overlay.clear()
        }
    }

    override fun onDetect(boundingBoxes: List<BoundingBox>, inferenceTime: Long) {
        runOnUiThread {
            binding.overlay.setResults(boundingBoxes)
            binding.overlay.invalidate()
            binding.inferenceTime.text = "${inferenceTime}ms"
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopProcessing()
        detector.close()
    }

    companion object {
        private const val REQUEST_PICK_VIDEO = 1001
    }
}