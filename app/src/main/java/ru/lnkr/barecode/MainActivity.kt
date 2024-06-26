package ru.lnkr.barecode

import android.os.Bundle
import android.util.Size
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
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

                /** Запуск запроса разрешений при запуске приложения. */
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
                        if (barcodeViewModel.code.isEmpty()) {
                            return@Column
                        }
                        if (!barcodeViewModel.isOpenDialog) {
                            return@Column
                        }
                        AlertDialog(
                            onDismissRequest = {},
                            confirmButton = {
                                Button(onClick = {
                                    barcodeViewModel.isOpenDialog = false
                                }) {
                                    Text(text = "ОК", fontSize = 16.sp)
                                }
                            },
                            title = { Text(text = "Код ${barcodeViewModel.code}") },
                            text = { Text("Приложение отсканировало код ${barcodeViewModel.code}") }
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
    /** [AndroidView] - компонент Compose для включения обычных Android Views */
    AndroidView(
        /**
         * factory -> context - лямбда-функция, создающая и возвращающая
         * [PreviewView]
         */
        factory = { context ->
            /** Вид для предварительного просмотра камеры */
            val previewView = PreviewView(context)

            /** Создает объект Preview для отображения предварительного просмотра. */
            val preview = androidx.camera.core.Preview.Builder().build()

            /** Создает [CameraSelector], указывающий, что используется задняя камера. */
            val selector = CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .build()

            /** Устанавливает поставщика поверхности для предварительного просмотра. */
            preview.setSurfaceProvider(previewView.surfaceProvider)

            val imageAnalysis = ImageAnalysis.Builder()
                .setTargetResolution(
                    Size(previewView.width, previewView.height)
                )
                /**
                 * Устанавливает стратегию обработки нагрузки (хранит только последнее
                 * изображение).
                 */
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
            imageAnalysis.setAnalyzer(
                /** Получает главный исполнитель для выполнения задач в основном потоке. */
                ContextCompat.getMainExecutor(context),
                BarcodeAnalyzer { result ->
                    vm.code = result
                    vm.isOpenDialog = true
                }
            )

            try {
                vm.cameraProviderFuture
                    .get()
                    /**
                     * Привязывает предварительный просмотр и анализ изображений к жизненному
                     * циклу [lifecycleOwner].
                     */
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