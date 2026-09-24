package com.schoolos.android.core.network

import okhttp3.CertificatePinner

object CertificatePinnerFactory {
    fun create(): CertificatePinner = CertificatePinner.DEFAULT
}
