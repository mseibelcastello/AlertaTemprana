package com.example.alertatemprana.datos.device

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager

class Linterna(context: Context) {

    var isOn: Boolean = false
        private set

    private val cameraManager: CameraManager =
        context.getSystemService(Context.CAMERA_SERVICE) as CameraManager

    private val cameraId: String =
        cameraManager.cameraIdList.first { id ->
            cameraManager.getCameraCharacteristics(id)
                .get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
        }

    fun toggle() {
        cameraManager.setTorchMode(cameraId, isOn.not())
        isOn = !isOn
    }

    fun turnOff() {
        cameraManager.setTorchMode(cameraId, false)
        isOn = false
    }
}