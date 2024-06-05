package ru.lnkr.barecode

import android.graphics.ImageFormat
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.common.HybridBinarizer
import java.nio.ByteBuffer

/**
 * Класс BarcodeAnalyzer, который реализует интерфейс
 * ImageAnalysis.Analyzer. Конструктор принимает лямбда-функцию
 * onBarcodeScanned, которая вызывается при успешном сканировании
 * штрих-кода.
 *
 * @property onBarcodeScanned - функция, которая вызывается при успешном
 *     сканировании штрих-кода
 */
class BarcodeAnalyzer(
    private val onBarcodeScanned: (String) -> Unit,
) : ImageAnalysis.Analyzer {

    /**
     * Список поддерживаемых форматов изображений. Нужно для определения
     * поддерживаемых форматов у image.format в функции [analyze]
     */
    private val supportedImageFormats = listOf(
        ImageFormat.YUV_420_888,
        ImageFormat.YUV_422_888,
        ImageFormat.YUV_444_888,
    )


    /**
     * Переопределяем метод analyze, который выполняется для анализа каждого
     * кадра.
     *
     * @param image - В Android камера предоставляет изображение в формате
     *     ImageProxy, которое содержит данные о кадре, полученном с камеры.
     *     Эти данные хранятся в виде плоскостей (planes), где каждая плоскость
     *     представляет собой часть изображения (например, яркость или цвет).
     */
    override fun analyze(image: ImageProxy) {
        // Проверяет, поддерживается ли формат изображения. Если нет, завершает метод.
        if (image.format !in supportedImageFormats) {
            return
        }

        // Извлекает байты из первого плана изображения и конвертирует их в массив байт
        val bytes = image.planes.first().buffer.toByteArray()

        /**
         * Источник данных из массива байт
         *
         * [PlanarYUVLuminanceSource] - это класс, который преобразует массив
         * байтов из изображения (в формате YUV) в формат, который можно
         * использовать для декодирования штрих-кодов. Он преобразует данные
         * изображения в яркость (luminance), игнорируя цветовую информацию.
         */
        val source = PlanarYUVLuminanceSource(
            bytes,
            image.width,
            image.height,
            0,
            0,
            image.width,
            image.height,
            false
        )

        /**
         * [BinaryBitmap] - это класс, представляющий бинарное изображение, которое
         * используется библиотекой ZXing для декодирования. Он принимает Binarizer
         * (в данном случае HybridBinarizer), который преобразует исходное
         * изображение в бинарный формат.
         *
         * [HybridBinarizer] - этот класс использует гибридный алгоритм для
         * преобразования изображения в бинарный формат (черно-белый). Бинаризация
         * — это процесс, в котором пиксели изображения преобразуются в два
         * цвета: черный и белый. Это упрощает задачу распознавания штрих-кодов.
         */
        val binaryBmp = BinaryBitmap(HybridBinarizer(source))
        try {
            /**
             * Декодирование штрих-кода. Если успешно, вызывает onBarcodeScanned с
             * результатом. В любом случае закрывает изображение.
             */
            val result = MultiFormatReader().apply {
                setHints(
                    mapOf(
                        DecodeHintType.POSSIBLE_FORMATS to arrayListOf(
                            BarcodeFormat.EAN_13,
                            BarcodeFormat.EAN_8,
                            BarcodeFormat.QR_CODE,
                        )
                    )
                )
            }.decode(binaryBmp)
            onBarcodeScanned(result.text)
        } catch (_: Exception) {
        } finally {
            image.close()
        }
    }

    /**
     * Вспомогательная функция для конвертации ByteBuffer в массив байт.
     *
     * @return - массив байт
     */
    private fun ByteBuffer.toByteArray(): ByteArray {
        rewind()
        return ByteArray(remaining()).also {
            get(it)
        }
    }
}