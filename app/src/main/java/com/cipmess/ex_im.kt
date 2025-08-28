package com.cipmess

import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.annotation.RequiresApi
import org.json.JSONArray
import org.json.JSONObject
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec

@RequiresApi(Build.VERSION_CODES.O)
fun export (context: Context, password: String, file_name: String, message: String) {

    val salt = SecureRandom().generateSeed(16)
    val key = derive_key(password, salt)

    val cip_test = Cipher.getInstance("AES/GCM/NoPadding")
    cip_test.init(Cipher.ENCRYPT_MODE, key)

    val c = Cipher.getInstance("AES/GCM/NoPadding")
    c.init(Cipher.ENCRYPT_MODE, key)


    val file = JSONObject().apply {
        put("salt", Base64.getEncoder().withoutPadding().encodeToString(salt))

        put("message", Base64.getEncoder().withoutPadding().encodeToString(c.doFinal(message.toByteArray())))
        put("iv", Base64.getEncoder().withoutPadding().encodeToString(c.iv))

        put("text_test", Base64.getEncoder().withoutPadding().encodeToString(cip_test.doFinal("CipMess".toByteArray())))
        put("iv_test", Base64.getEncoder().withoutPadding().encodeToString(cip_test.iv))

    }.toString()

    val file_values = ContentValues().apply {
        put(MediaStore.Files.FileColumns.DISPLAY_NAME, "$file_name.cm")
        put(MediaStore.Files.FileColumns.MIME_TYPE, "application/cm")
        put(MediaStore.Files.FileColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
    }

    val uri = context.contentResolver.insert(MediaStore.Files.getContentUri("external"), file_values)

    context.contentResolver.openOutputStream(uri!!)!!.write(file.toByteArray())
}

@RequiresApi(Build.VERSION_CODES.O)
fun import (jsonOb: JSONObject, password: String): String {

    val key = derive_key(password, Base64.getDecoder().decode(jsonOb.getString("salt")))

    val cip_test = Cipher.getInstance("AES/GCM/NoPadding")
    cip_test.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, Base64.getDecoder().decode(jsonOb.getString("iv_test"))))

    cip_test.doFinal(Base64.getDecoder().decode(jsonOb.getString("text_test")))

    val c = Cipher.getInstance("AES/GCM/NoPadding")
    c.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, Base64.getDecoder().decode(jsonOb.getString("iv"))))

    return String(c.doFinal(Base64.getDecoder().decode(jsonOb.getString("message"))))
}