package com.example.voting

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.provider.MediaStore
import android.util.Base64
import android.view.LayoutInflater
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import com.example.voting.databinding.ActivityVerifyBinding
import io.appwrite.Client
import io.appwrite.services.Storage
import kotlinx.coroutines.launch
import okhttp3.Call
import okhttp3.Callback
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONException
import org.json.JSONObject
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException

@Suppress("DEPRECATION")
class VerifyActivity : AppCompatActivity() {

    private var selectedImageUri: Bitmap? = null
    private var serverImgUri: Bitmap? = null
    private var aadharNum: String? = null
    lateinit var dialog: Dialog
    private var constituency: String? = null

    companion object {
        const val API_KEY = "nx89Cz0lRzLAm6oDbfS1YKEKT5iIAsSc"
        const val API_SECRET = "LL_xQN4H63TfivUCgc6uMuGjAzQiNpKY"
        const val PICK_IMAGE = 1
        const val FACE_COMPARE_URL = "https://api-us.faceplusplus.com/facepp/v3/compare"
    }

    private lateinit var binding: ActivityVerifyBinding

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        binding = DataBindingUtil.inflate(LayoutInflater.from(this), R.layout.activity_verify, null, false)
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        dialog = Dialog(this).apply {
            setContentView(R.layout.progressbar)
            window?.setBackgroundDrawableResource(android.R.color.transparent)
            setCancelable(false)
        }

        binding.backIcon.setOnClickListener { finish() }

        lifecycleScope.launch {
            try {
                dialog.show() // Show dialog while retrieving the image
                serverImgUri = getFileFromAppwrite()
            } catch (e: Exception) {
                Toast.makeText(this@VerifyActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                dialog.dismiss() // Ensure the dialog is dismissed
            }
        }

        @Suppress("DEPRECATION")
        val voterInfo = intent.getSerializableExtra("voterInfo") as? HashMap<*, *>
        aadharNum = intent.getStringExtra("aadhar")
        constituency = voterInfo?.get("constituency").toString()
        binding.constituency = constituency
        binding.name = voterInfo?.get("name").toString()
        binding.age = voterInfo?.get("age").toString() + "yrs"

        binding.chooseImg.setOnClickListener { pickImageFromCamera(PICK_IMAGE) }

        binding.btnNext.setOnClickListener {
            if (serverImgUri != null && selectedImageUri != null) {
                compareFaces(selectedImageUri!!, serverImgUri!!)
            } else {
                Toast.makeText(this, "Please choose an image first", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private suspend fun getFileFromAppwrite(): Bitmap {
        val client = Client(applicationContext)
            .setProject("67324b5c000f73b50932") // Your project ID

        val storage = Storage(client)
        val result = storage.getFileDownload(
            bucketId = "67324d4c0024e0e964c6",
            fileId = "595397966102"
        )

        val inputStream = ByteArrayInputStream(result)
        return BitmapFactory.decodeStream(inputStream)
    }

    @SuppressLint("QueryPermissionsNeeded")
    private fun pickImageFromCamera(requestCode: Int) {
        val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        cameraIntent.putExtra("android.intent.extras.CAMERA_FACING", 1) // Front camera
        cameraIntent.putExtra("android.intent.extras.LENS_FACING_FRONT", 1)
        cameraIntent.putExtra("android.intent.extra.USE_FRONT_CAMERA", true)

        if (cameraIntent.resolveActivity(packageManager) != null) {
            startActivityForResult(cameraIntent, requestCode)
        } else {
            Toast.makeText(this, "No Camera App Found", Toast.LENGTH_SHORT).show()
        }
    }

    @Deprecated("This method is deprecated. Use ActivityResult APIs instead.")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK && data != null) {
            when (requestCode) {
                PICK_IMAGE -> {
                    val imageBitmap = data.extras?.get("data") as? Bitmap
                    if (imageBitmap != null) {
                        binding.chooseImg.scaleType = ImageView.ScaleType.CENTER_CROP
                        selectedImageUri = imageBitmap
                        binding.chooseImg.setImageBitmap(imageBitmap)
                    } else {
                        Toast.makeText(this, "Failed to capture image", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun compareFaces(imageUri1: Bitmap, imageUri2: Bitmap) {
        dialog.show() // Show dialog before starting the request
        val encodedImage1 = encodeImageToBase64(imageUri1)
        val encodedImage2 = encodeImageToBase64(imageUri2)
        val client = OkHttpClient()
        val requestBody = FormBody.Builder()
            .add("api_key", API_KEY)
            .add("api_secret", API_SECRET)
            .add("image_base64_1", encodedImage1)
            .add("image_base64_2", encodedImage2)
            .build()

        val request = Request.Builder()
            .url(FACE_COMPARE_URL)
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    dialog.dismiss()
                    Toast.makeText(this@VerifyActivity, "Request Failed: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                dialog.dismiss()
                runOnUiThread {
                    try {
                        val responseString = response.body?.string()
                        val jsonResponse = JSONObject(responseString ?: "")
                        val confidence = jsonResponse.optDouble("confidence", 0.0)
                        val faces = jsonResponse.optJSONArray("faces1")

                        if (faces != null && faces.length() == 1) {
                          /*  Toast.makeText(
                                this@VerifyActivity,
                                "Similarity: $confidence%",
                                Toast.LENGTH_SHORT
                            ).show()*/

                            if (confidence >= 89) {
                                val voteIntent = Intent(this@VerifyActivity, VoteActivity::class.java)
                                voteIntent.putExtra("constituency", constituency)
                                voteIntent.putExtra("aadhar" , aadharNum)
                                startActivity(voteIntent)
                            }
                        } else {
                            Toast.makeText(this@VerifyActivity, "Oops! Multiple faces detected", Toast.LENGTH_LONG).show()
                        }
                    } catch (e: JSONException) {
                        Toast.makeText(this@VerifyActivity, "Failed to parse JSON response", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        })
    }

    private fun encodeImageToBase64(bitmap: Bitmap): String {
        val resizedBitmap = resizeBitmap(bitmap, 4000, 4000)
        var quality = 100
        var byteArrayOutputStream = ByteArrayOutputStream()
        resizedBitmap.compress(Bitmap.CompressFormat.JPEG, quality, byteArrayOutputStream)
        var byteArray = byteArrayOutputStream.toByteArray()

        while (byteArray.size > 1572864 && quality > 0) {
            quality -= 5
            byteArrayOutputStream = ByteArrayOutputStream()
            resizedBitmap.compress(Bitmap.CompressFormat.JPEG, quality, byteArrayOutputStream)
            byteArray = byteArrayOutputStream.toByteArray()
        }

        return Base64.encodeToString(byteArray, Base64.DEFAULT)
    }

    private fun resizeBitmap(bitmap: Bitmap, maxWidth: Int, maxHeight: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val ratioBitmap = width.toFloat() / height.toFloat()
        var finalWidth = maxWidth
        var finalHeight = maxHeight

        if (ratioBitmap > 1) {
            finalHeight = (finalWidth / ratioBitmap).toInt()
        } else {
            finalWidth = (finalHeight * ratioBitmap).toInt()
        }

        return Bitmap.createScaledBitmap(bitmap, finalWidth, finalHeight, true)
    }
}
