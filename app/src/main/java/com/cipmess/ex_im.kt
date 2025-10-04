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

    try {


        val c_test = Cipher.getInstance("AES/GCM/NoPadding")
        c_test.init(Cipher.ENCRYPT_MODE, key)
        val pro = JSONObject().apply {
            put("text_test", Base64.getEncoder().withoutPadding().encodeToString(c_test.doFinal("CipMess".toByteArray())))
            put("iv_test", Base64.getEncoder().withoutPadding().encodeToString(c_test.iv))
        }
        val pro_array = JSONArray().apply {
            put(pro)
        }

        val c = Cipher.getInstance("AES/GCM/NoPadding")
        c.init(Cipher.ENCRYPT_MODE, key)
        val mess = JSONObject().apply {
            put("message", Base64.getEncoder().withoutPadding().encodeToString(c.doFinal(message.toByteArray())))
            put("iv", Base64.getEncoder().withoutPadding().encodeToString(c.iv))
        }
        val mess_array = JSONArray().apply {
            put(mess)
        }

        val file = JSONObject().apply {
            put("salt", Base64.getEncoder().withoutPadding().encodeToString(salt))
            put("pro_array", pro_array)
            put("mess_array", mess_array)
        }.toString()

        val file_values = ContentValues().apply {
            put(MediaStore.Files.FileColumns.DISPLAY_NAME, "$file_name.cm")
            put(MediaStore.Files.FileColumns.MIME_TYPE, "application/cm")
            put(MediaStore.Files.FileColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
        }

        val uri = context.contentResolver.insert(MediaStore.Files.getContentUri("external"), file_values)

        context.contentResolver.openOutputStream(uri!!)!!.write(file.toByteArray())
    }catch (e: Exception) {

    } finally {
        key.encoded.fill(0)
    }
}

@RequiresApi(Build.VERSION_CODES.O)
fun import (jsonOb: JSONObject, password: String): String {

    val key = derive_key(password, Base64.getDecoder().decode(jsonOb.getString("salt")))

    try {
        val pro_array = jsonOb.getJSONArray("pro_array").getJSONObject(0)
        val cip_test = Cipher.getInstance("AES/GCM/NoPadding")
        cip_test.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, Base64.getDecoder().decode(pro_array.getString("iv_test"))))

        cip_test.doFinal(Base64.getDecoder().decode(pro_array.getString("text_test")))

        val mess_array = jsonOb.getJSONArray("mess_array").getJSONObject(0)
        val c = Cipher.getInstance("AES/GCM/NoPadding")
        c.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, Base64.getDecoder().decode(mess_array.getString("iv"))))

        return String(c.doFinal(Base64.getDecoder().decode(mess_array.getString("message"))))

    } catch (e: Exception) {
        return "Desencription error"
    } finally {
        key.encoded.fill(0)
    }
}