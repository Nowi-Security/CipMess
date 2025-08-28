package com.cipmess

import android.os.Build
import androidx.annotation.RequiresApi
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.PBEParameterSpec
import javax.crypto.spec.SecretKeySpec

@RequiresApi(Build.VERSION_CODES.O)
fun derive_key (password: String, salt: ByteArray): SecretKey {

    return SecretKeySpec(SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(PBEKeySpec(password.toCharArray(), salt, 600_000, 256)).encoded, "AES")

}
