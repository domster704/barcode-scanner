package ru.lnkr.barecode

import android.content.Context
import android.content.pm.PackageManager
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import com.google.common.util.concurrent.ListenableFuture


/**
 * Компонент ViewModel для хранения стейтов приложения. Наследуется от
 * ViewModel.
 */
class BarcodeViewModel : ViewModel() {
    /** Результат расшифровки barcode. */
    var code by mutableStateOf("")

    /** Проверка на разрешение на использование камеры. */
    private lateinit var _hasCameraPermission: MutableState<Boolean>
    val hasCameraPermission get() = _hasCameraPermission.value

    /**
     * Провайдер для встраивания жизненного цикла камеры в основной цикл
     * приложения.
     */
    private lateinit var _cameraProviderFuture: MutableState<ListenableFuture<ProcessCameraProvider>>
    val cameraProviderFuture get() = _cameraProviderFuture.value

    var isOpenDialog by mutableStateOf(false)

    fun setHasCameraPermissionInit(context: Context) {
        _hasCameraPermission = mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    fun setCameraProviderFutureInit(context: Context) {
        _cameraProviderFuture = mutableStateOf(ProcessCameraProvider.getInstance(context))
    }

    fun setHasCameraPermission(hasCameraPermission: Boolean) {
        _hasCameraPermission.value = hasCameraPermission
    }
}

