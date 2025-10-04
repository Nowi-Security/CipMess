package com.cipmess

import android.annotation.SuppressLint
import android.app.ComponentCaller
import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.lifecycleScope
import com.google.android.material.imageview.ShapeableImageView
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec

class MainActivity : AppCompatActivity() {

    private var edit_status = true
    private lateinit var load_dialog: Dialog
    @SuppressLint("MissingInflatedId")
    fun load (info_text: String) {
        load_dialog = Dialog(this)
        val load_view = LayoutInflater.from(this).inflate(R.layout.load, null)


        val info = load_view.findViewById<TextView>(R.id.info)
        val progress = load_view.findViewById<ProgressBar>(R.id.load)

        info.text = info_text
        progress.isActivated = true

        load_dialog.setContentView(load_view)
        load_dialog.setCancelable(false)
        load_dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        load_dialog.show()
    }

    private lateinit var text_read: TextView
    private lateinit var text_scroll: ScrollView
    private lateinit var icon_modi: ShapeableImageView
    private lateinit var input_text: EditText
    private lateinit var input_visi: TextInputLayout
    private lateinit var edit: ConstraintLayout

    @RequiresApi(Build.VERSION_CODES.O)
    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        delegate.localNightMode = AppCompatDelegate.MODE_NIGHT_YES

        input_text = findViewById<EditText>(R.id.input_text)
        input_visi = findViewById<TextInputLayout>(R.id.input_visi)
        text_read = findViewById<TextView>(R.id.text_read)
        text_scroll = findViewById<ScrollView>(R.id.text_scroll)

        edit = findViewById<ConstraintLayout>(R.id.edit)
        icon_modi = findViewById<ShapeableImageView>(R.id.icon_modi)
        val delete = findViewById<ConstraintLayout>(R.id.delete)

        val import = findViewById<ShapeableImageView>(R.id.import_f)
        val export = findViewById<ShapeableImageView>(R.id.export_f)

        edit.visibility = View.INVISIBLE
        delete.visibility = View.INVISIBLE
        text_read.visibility = View.INVISIBLE
        text_scroll.visibility = View.INVISIBLE

        input_text.addTextChangedListener {dato ->
            if (dato!!.isNotEmpty()) {
                delete.visibility = View.VISIBLE
                edit.visibility = View.VISIBLE
            }else {
                delete.visibility = View.INVISIBLE
                edit.visibility = View.INVISIBLE
            }
        }

        delete.setOnClickListener {
            input_text.setText("")
        }


        edit.setOnClickListener {

            if (edit_status) {
                edit_status = false
                text_read.visibility = View.VISIBLE
                text_scroll.visibility = View.VISIBLE
                icon_modi.setImageResource(R.drawable.edit)
                text_read.text = input_text.text.toString()
                input_text.setText("")
                input_visi.visibility = View.INVISIBLE
                edit.visibility = View.VISIBLE
                window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
            }else {
                if (BiometricManager.from(this).canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL) == BiometricManager.BIOMETRIC_SUCCESS) {

                    val promt = BiometricPrompt.PromptInfo.Builder().apply {
                        setTitle("Authenticate yourself")
                        setAllowedAuthenticators(BiometricManager.Authenticators.DEVICE_CREDENTIAL or BiometricManager.Authenticators.BIOMETRIC_STRONG)
                    }.build()

                    BiometricPrompt(this, ContextCompat.getMainExecutor(this), object: BiometricPrompt.AuthenticationCallback() {

                        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                            super.onAuthenticationSucceeded(result)
                            edit_status = true
                            input_visi.visibility = View.VISIBLE
                            icon_modi.setImageResource(R.drawable.read)
                            input_text.setText(text_read.text.toString())
                            text_read.text = ""
                            text_read.visibility = View.INVISIBLE
                            text_scroll.visibility = View.INVISIBLE
                            window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
                        }

                        override fun onAuthenticationError(
                            errorCode: Int,
                            errString: CharSequence
                        ) {
                            super.onAuthenticationError(errorCode, errString)
                            Toast.makeText(applicationContext, "You need to authenticate yourself", Toast.LENGTH_SHORT).show()
                        }
                    }).authenticate(promt)

                }
            }

        }

        export.setOnClickListener {
            val dialog_export = Dialog(this)
            val view_dialog = LayoutInflater.from(this).inflate(R.layout.export_file, null)


            val input_pass = view_dialog.findViewById<EditText>(R.id.input_pass)
            val progress = view_dialog.findViewById<LinearProgressIndicator>(R.id.progress)
            val file_name = view_dialog.findViewById<EditText>(R.id.input_name_f)

            val export_button = view_dialog.findViewById<ConstraintLayout>(R.id.export_butt)

            input_pass.addTextChangedListener {
                if (it!!.isNotEmpty()) {
                    entropy(it.toString(), progress)
                }
            }

            export_button.setOnClickListener {
                if (input_pass.text.isNotEmpty() && file_name.text.isNotEmpty()) {
                    val promt = BiometricPrompt.PromptInfo.Builder().apply {
                        setTitle("Authenticate yourself")
                        setAllowedAuthenticators(BiometricManager.Authenticators.DEVICE_CREDENTIAL or BiometricManager.Authenticators.BIOMETRIC_STRONG)
                    }.build()

                    BiometricPrompt(this, ContextCompat.getMainExecutor(this), object : BiometricPrompt.AuthenticationCallback() {

                            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                                super.onAuthenticationSucceeded(result)
                                dialog_export.dismiss()
                                load("Exporting your message")
                                lifecycleScope.launch(Dispatchers.IO) {
                                    export(applicationContext, input_pass.text.toString(), file_name.text.toString(), input_text.text.toString())

                                    withContext(Dispatchers.Main) {
                                        input_text.setText("")
                                        load_dialog.dismiss()
                                    }
                                    cancel()
                                }
                            }

                            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                                super.onAuthenticationError(errorCode, errString)
                                Toast.makeText(applicationContext, "You need to authenticate yourself", Toast.LENGTH_SHORT).show()
                            }
                        }).authenticate(promt)
                }

            }


            if (edit_status && input_text.text.isNotEmpty()) {
                dialog_export.setContentView(view_dialog)
                dialog_export.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                dialog_export.show()
            }else {
                Toast.makeText(this, "You are not in edit mode or the text is empty.", Toast.LENGTH_SHORT).show()
            }
        }

        import.setOnClickListener {

            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "*/*"
            }

            startActivityForResult(intent, 1001)

        }

        window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }


    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?,
        caller: ComponentCaller
    ) {
        super.onActivityResult(requestCode, resultCode, data, caller)

        if (resultCode == -1) {
            val uri = data?.data
            val query = contentResolver.query(uri!!, null, null, null, null)!!

            if (query.moveToFirst()) {
                val position = query.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val name = query.getString(position)


                if (name!!.matches(Regex(".*.cm.*"))) {

                    val file_reader = contentResolver.openInputStream(uri)?.bufferedReader()?.readText()

                    val json = JSONObject(file_reader)

                    val import_dialog = Dialog(this)
                    val import_view = LayoutInflater.from(this).inflate(R.layout.import_file, null)

                    val input_pass = import_view.findViewById<EditText>(R.id.input_pass)
                    val progress = import_view.findViewById<LinearProgressIndicator>(R.id.progress)
                    val import_butt = import_view.findViewById<ConstraintLayout>(R.id.import_butt)

                    input_pass.addTextChangedListener{
                        if (it!!.isNotEmpty()) {
                            entropy(it.toString(), progress)
                        }
                    }

                    import_butt.setOnClickListener {

                        if (json.has("salt") && json.has("pro_array") && json.has("mess_array")) {

                            import_dialog.dismiss()
                            load("Importing the message")

                            lifecycleScope.launch (Dispatchers.IO) {

                                try {
                                    val message = import(json, input_pass.text.toString())

                                    withContext(Dispatchers.Main) {
                                        load_dialog.dismiss()
                                        edit_status = false
                                        text_read.visibility = View.VISIBLE
                                        text_scroll.visibility = View.VISIBLE
                                        icon_modi.setImageResource(R.drawable.edit)
                                        text_read.text = input_text.text.toString()
                                        input_text.setText("")
                                        input_visi.visibility = View.INVISIBLE
                                        edit.visibility = View.VISIBLE
                                        window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)

                                        text_read.text = message
                                    }
                                }catch (e: Exception) {
                                    withContext(Dispatchers.Main) {
                                        Toast.makeText(applicationContext, "The password is not correct", Toast.LENGTH_SHORT).show()
                                        load_dialog.dismiss()
                                        cancel()
                                    }
                                }
                            }

                        }else {
                            Toast.makeText(this, "The structure is not correct", Toast.LENGTH_SHORT).show()
                        }

                    }

                    import_dialog.setContentView(import_view)
                    import_dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                    import_dialog.show()
                }
            }
        }

    }
}