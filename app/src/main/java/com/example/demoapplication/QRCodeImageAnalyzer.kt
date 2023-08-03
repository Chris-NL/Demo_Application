package com.example.demoapplication

import android.graphics.ImageFormat
import android.util.Log
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.zxing.BinaryBitmap
import com.google.zxing.ChecksumException
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.multi.qrcode.QRCodeMultiReader
import java.nio.ByteBuffer

class QRCodeImageAnalyzer(listener: QRCodeFoundListener) :
    ImageAnalysis.Analyzer {

    private val listener: QRCodeFoundListener

    init {
        this.listener = listener
    }

    override fun analyze(image: ImageProxy) {
        if (image.format == ImageFormat.YUV_420_888 || image.format == ImageFormat.YUV_422_888 || image.format == ImageFormat.YUV_444_888) {
            val byteBuffer: ByteBuffer = image.planes[0].buffer
            val imageData = ByteArray(byteBuffer.capacity())

            byteBuffer[imageData]
            val source = PlanarYUVLuminanceSource(imageData, image.width, image.height, 0,0, image.width, image.height, false)

            val binaryBitmap = BinaryBitmap(HybridBinarizer(source))

            try {
                val result: com.google.zxing.Result = QRCodeMultiReader().decode(binaryBitmap)
                listener.onQRCodeFound(result.text)
            } catch (e: Exception) {
                try {
                    val result: com.google.zxing.Result = QRCodeMultiReader().decode(BinaryBitmap(HybridBinarizer(source.invert())))
                    listener.onQRCodeFound(result.text)
                } catch (e: com.google.zxing.FormatException) {
                    Log.d("Analyze", "FormatException")
                    listener.qrCodeNotFound()
                } catch (e: ChecksumException) {
                    Log.d("Analyze", "ChecksumException")
                    listener.qrCodeNotFound()
                } catch (e: com.google.zxing.NotFoundException) {
                    Log.d("Analyze", "NotFoundException")
                    listener.qrCodeNotFound()
                }
            }
        }
        image.close()
    }
}