@file:Suppress("DEPRECATION")

package communication.hardware.clean.device.camera.cameranative

import android.content.pm.PackageManager
import android.hardware.Camera
import android.view.SurfaceHolder
import android.view.SurfaceView
import avila.domingo.lifecycle.ILifecycleObserver
import communication.hardware.clean.device.camera.model.ScreenSize
import communication.hardware.clean.device.camera.util.CameraRotationUtil
import kotlin.math.abs

class NativeCameraManager(
    private val packageManager: PackageManager,
    private val screenSize: ScreenSize,
    private val rangePicture: IntRange,
    private val surfaceView: SurfaceView,
    private val cameraRotationUtil: CameraRotationUtil,
    private val cameraId: Int,
    flashMode: String
) : INativeCamera, INativeFlash, ILifecycleObserver {

    private lateinit var currentCamera: Camera
    private var currentFlashMode = flashMode

    private val surfaceHolderCallback = object : SurfaceHolder.Callback {
        override fun surfaceChanged(
            holder: SurfaceHolder,
            format: Int,
            width: Int,
            height: Int
        ) {
        }

        override fun surfaceDestroyed(holder: SurfaceHolder) {}

        override fun surfaceCreated(holder: SurfaceHolder) {
            currentCamera.setPreviewDisplay(holder)
        }
    }

    override fun camera(): Camera = currentCamera

    override fun cameraId(): Int = cameraId

    override fun rotationDegrees(): Int = cameraRotationUtil.rotationDegreesImage(cameraId)

    override fun mode(mode: String) {
        currentCamera.parameters = currentCamera.parameters.apply {
            if (supportedFlashModes?.contains(mode) == true) {
                flashMode = mode
            }
        }
        currentFlashMode = mode
    }

    override fun mode(): String = currentFlashMode

    override fun isHardwareSupported(): Boolean = isSupportedCamera()

    private fun openCamera(cameraId: Int) {
        currentCamera = Camera.open(cameraId)
        configure()
    }

    private fun configure() {
        currentCamera.run {

            val customParameters = parameters
            customParameters.supportedFocusModes.run {
                when {
                    this.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE) -> customParameters.focusMode =
                        Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE
                    this.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO) -> customParameters.focusMode =
                        Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO
                    this.contains(Camera.Parameters.FOCUS_MODE_AUTO) -> customParameters.focusMode =
                        Camera.Parameters.FOCUS_MODE_AUTO
                }
            }

            val screenRatio = screenSize.witdh / screenSize.height.toFloat()

            var diff = Float.MAX_VALUE
            var previewWidth = 0
            var previewHeight = 0

            customParameters.supportedPreviewSizes
                .sortedBy { it.width }
                .filter {
                    it.width in rangePicture
                }
                .apply {
                    this.forEach {
                        val previewDiff =
                            abs((it.width / it.height.toFloat()) - screenRatio)
                        if (previewDiff < diff) {
                            diff = previewDiff
                            previewWidth = it.width
                            previewHeight = it.height
                        }
                    }
                }
                .filter { screenRatio == (it.width / it.height.toFloat()) }
                .run {
                    if (size > 1) {
                        get(1).let { customParameters.setPreviewSize(it.width, it.height) }
                    } else {
                        customParameters.setPreviewSize(previewWidth, previewHeight)
                    }
                }

            diff = Float.MAX_VALUE
            previewWidth = 0
            previewHeight = 0

            customParameters.supportedPictureSizes
                .sortedBy { it.width }
                .filter {
                    it.width in rangePicture
                }
                .apply {
                    this.forEach {
                        val previewDiff =
                            abs((it.width / it.height.toFloat()) - screenRatio)
                        if (previewDiff < diff) {
                            diff = previewDiff
                            previewWidth = it.width
                            previewHeight = it.height
                        }
                    }
                }
                .filter { screenRatio == (it.width / it.height.toFloat()) }
                .run {
                    if (size > 1) {
                        get(1).let { customParameters.setPictureSize(it.width, it.height) }
                    } else {
                        customParameters.setPictureSize(previewWidth, previewHeight)
                    }
                }

            customParameters.flashMode = currentFlashMode

            parameters = customParameters

            setDisplayOrientation(cameraRotationUtil.rotationDegreesPreview(cameraId))
        }
    }

    private fun isSupportedCamera(): Boolean =
        packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA) &&
                (cameraId == Camera.CameraInfo.CAMERA_FACING_BACK || packageManager.hasSystemFeature(
                    PackageManager.FEATURE_CAMERA_FRONT
                ))

    override fun resume() {
        if (isSupportedCamera()) {
            surfaceView.holder.addCallback(surfaceHolderCallback)
            openCamera(cameraId)
            currentCamera.setPreviewDisplay(surfaceView.holder)
            currentCamera.startPreview()
        }
    }

    override fun pause() {
        if (isSupportedCamera()) {
            surfaceView.holder.removeCallback(surfaceHolderCallback)
            currentCamera.stopPreview()
        }
    }
}