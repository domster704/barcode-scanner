package ru.lnkr.barecode

import android.os.Bundle
import android.util.Log
import android.util.Size
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import ru.lnkr.barecode.ui.theme.BarcodeScannerTheme

class MainActivity : ComponentActivity() {
    private val barcodeViewModel: BarcodeViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BarcodeScannerTheme {
                val context = LocalContext.current
                val lifecycleOwner = LocalLifecycleOwner.current

                barcodeViewModel.setHasCameraPermissionInit(context)
                barcodeViewModel.setCameraProviderFutureInit(context)

                /**
                 * Запуска запроса разрешений при запуске приложения.
                 * [rememberLauncherForActivityResult] - регистратор
                 * на запуск действия для получения запроса разрешений
                 */
                val launcher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission(),
                    onResult = { granted ->
                        barcodeViewModel.setHasCameraPermission(granted)
                    }
                )

                /**
                 * Запуск запроса разрешений при запуске приложения.
                 */
                LaunchedEffect(key1 = true) {
                    launcher.launch(android.Manifest.permission.CAMERA)
                }

                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    if (barcodeViewModel.hasCameraPermission) {
                        BarcodeCameraView(
                            vm = barcodeViewModel,
                            modifier = Modifier.weight(1f),
                            lifecycleOwner = lifecycleOwner,
                        )
                        Text(
                            text = barcodeViewModel.code,
                            fontSize = 20.sp,
                            modifier = Modifier
                                .padding(30.dp)
                                .fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun BarcodeCameraView(
    vm: BarcodeViewModel,
    modifier: Modifier,
    lifecycleOwner: LifecycleOwner
) {
    AndroidView(
        factory = { context ->
            val previewView = PreviewView(context)
            val preview = androidx.camera.core.Preview.Builder().build()
            val selector = CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .build()


            preview.setSurfaceProvider(previewView.surfaceProvider)
            Log.d("xd", "${previewView.width}, ${previewView.height}")
            val imageAnalysis = ImageAnalysis.Builder()
                .setResolutionSelector(
                    ResolutionSelector.Builder()
                        .setResolutionStrategy(
                            ResolutionStrategy(
                                Size(0, 0),
                                ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER
                            )
                        ).build()
                )
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
            imageAnalysis.setAnalyzer(
                ContextCompat.getMainExecutor(context),
                BarcodeAnalyzer { result ->
                    vm.code = result
                }
            )

            try {
                vm.cameraProviderFuture
                    .get()
                    .bindToLifecycle(
                        lifecycleOwner,
                        selector,
                        preview,
                        imageAnalysis
                    )
            } catch (e: Exception) {
                e.printStackTrace()
            }
            previewView
        },
        modifier = modifier
    )
}