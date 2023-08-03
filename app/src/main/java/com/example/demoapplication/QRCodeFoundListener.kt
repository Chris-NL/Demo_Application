package com.example.demoapplication

interface QRCodeFoundListener {
    fun onQRCodeFound(qrCode: String?)

    fun qrCodeNotFound()
}