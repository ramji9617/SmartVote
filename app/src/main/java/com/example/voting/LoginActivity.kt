package com.example.voting
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.Dialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.view.KeyEvent
import android.view.LayoutInflater
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.databinding.DataBindingUtil
import com.example.voting.databinding.ActivityLoginBinding
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import `in`.aabhasjindal.otptextview.OTPListener
import java.io.Serializable
import java.util.concurrent.TimeUnit

class LoginActivity : AppCompatActivity() {

    private lateinit var voterinfo: HashMap<String, Any>
    lateinit var dialog: Dialog
    private lateinit var auth: FirebaseAuth
    private var phoneNum = ""
    private var storedVerificationId: String? = null
    private lateinit var resendToken: PhoneAuthProvider.ForceResendingToken
    private lateinit var callbacks: PhoneAuthProvider.OnVerificationStateChangedCallbacks
    private lateinit var countdownTimer: CountDownTimer
    private val db = Firebase.firestore
    var aadharNum: String = ""
    private var isVoted = false
   private lateinit var  alertDialog :AlertDialog
    private lateinit var binding: ActivityLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {

        binding = DataBindingUtil.inflate(LayoutInflater.from(this), R.layout.activity_login, null, false)
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        auth = FirebaseAuth.getInstance()



        // Initialize dialog
        dialog = Dialog(this).apply {
            setContentView(R.layout.progressbar)
            window?.setBackgroundDrawableResource(android.R.color.transparent)
            setCancelable(false)
        }



        setupUI()


        callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() { // function for phone number verification
            override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                dialog.dismiss()  // Hide the dialog on completion
                signInWithPhoneAuthCredential(credential)
            }


            // when verification failed
            override fun onVerificationFailed(e: FirebaseException) {
                dialog.dismiss()  // Hide the dialog if verification fails
                Log.w("MainActivity", "onVerificationFailed", e)
                Toast.makeText(this@LoginActivity, "Verification failed", Toast.LENGTH_SHORT).show()
            }

            override fun onCodeSent(
                verificationId: String,
                token: PhoneAuthProvider.ForceResendingToken
            ) {
                storedVerificationId = verificationId
                resendToken = token
                startResendCooldown()
                Toast.makeText(this@LoginActivity, "OTP sent", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
                binding.isMobile = true
                binding.showOTP.visibility = TextView.VISIBLE
                binding.otpTimer.visibility = TextView.VISIBLE
                binding.inputLayout.visibility = TextInputLayout.VISIBLE
            }
        }

        binding.btnNext.setOnClickListener {
            if (aadharNum.isNotEmpty() && aadharNum.length == 12) {
                if (binding.showOTP.visibility == TextView.INVISIBLE && binding.resendOtp.visibility == TextView.INVISIBLE) {
                    dialog.show()  // Show dialog while fetching data
                    fetchData()



                } else if (binding.showOTP.visibility == TextView.VISIBLE) {
                    if (binding.otpEditText.otp!!.length >= 6) {
                        dialog.show()  // Show dialog while verifying the OTP
                        val otp = binding.otpEditText.otp!!
                        verifyCode(otp)
                    }
                }
            }
        }

        binding.resendOtp.setOnClickListener {
            dialog.show()  // Show dialog when resending OTP
            resendVerificationCode(phoneNum, callbacks, resendToken)
        }

        binding.contactUs.setOnClickListener {
            val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:")
                putExtra(Intent.EXTRA_EMAIL, arrayOf("Ramjipathak666@gmail.com"))
                putExtra(Intent.EXTRA_SUBJECT, "SmartVote Application")
            }
            startActivity(emailIntent)
        }
    }

    private fun setupUI() {
        val otpListener: OTPListener = object : OTPListener {
            override fun onInteractionListener() {
                if (binding.otpTextView3.otp!!.length > 3 && binding.otpTextView2.otp!!.length > 3 && binding.otpTextView1.otp!!.length > 3) {
                    aadharNum =
                        binding.otpTextView1.otp!! + binding.otpTextView2.otp!! + binding.otpTextView3.otp!!
                    binding.btnNext.isClickable = true
                    binding.btnNext.background.setTint(getColor(R.color.green))

                    binding.isMobile = false
                    binding.aadharNumber = aadharNum
                } else {
                    binding.btnNext.isClickable = false
                    binding.btnNext.background.setTint(getColor(R.color.gry))
                }

                if (binding.otpTextView1.otp!!.length > 3 && binding.otpTextView1.hasFocus()) {
                    binding.otpTextView2.requestFocusOTP()
                }

                if (binding.otpTextView2.otp!!.length > 3 && binding.otpTextView2.hasFocus()) {
                    binding.otpTextView3.requestFocusOTP()
                }
            }

            override fun onOTPComplete(otp: String) {
                // Handle complete OTP entry
            }
        }

        binding.otpTextView1.otpListener = otpListener
        binding.otpTextView2.otpListener = otpListener
        binding.otpTextView3.otpListener = otpListener
    }

    private fun fetchData() {
        db.collection("Voter").document(aadharNum).get()
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val document = task.result
                    if (document != null && document.exists()) {
                        val mobile = document.getString("mobile") ?: ""
                        isVoted = document.getBoolean("isvoted") ?: false
                        voterinfo = (document.data as HashMap<String, Any>?)!!
                        if (mobile.isNotEmpty()) {
                            //  Toast.makeText(this@LoginActivity, mobile, Toast.LENGTH_LONG).show()
                            phoneNum = mobile
                            Log.d("map", voterinfo.toString())

                            if (isVoted){

                                val dialogBuilder = AlertDialog.Builder(this@LoginActivity)
                                dialogBuilder.setMessage("User Already Voted").setNegativeButton("Ok"){alert , _ ->
                                    alert.dismiss()


                                }
                                 alertDialog = dialogBuilder.create()
                                dialog.dismiss()
                                alertDialog.show()
                               return@addOnCompleteListener

                            }else{

                            sendVerificationCode(mobile, callbacks)}
                        } else {
                            dialog.dismiss()
                            Toast.makeText(
                                this@LoginActivity,
                                "Mobile number is empty",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    } else {
                        dialog.dismiss()
                        Toast.makeText(
                            this@LoginActivity,
                            "Document does not exist",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } else {
                    dialog.dismiss()
                    Toast.makeText(this@LoginActivity, "Failed to fetch data", Toast.LENGTH_SHORT)
                        .show()
                }
            }
    }

    private fun sendVerificationCode(
        phoneNumber: String,
        callbacks: PhoneAuthProvider.OnVerificationStateChangedCallbacks
    ) {
        val options = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(phoneNumber)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(this)
            .setCallbacks(callbacks)
            .build()
        PhoneAuthProvider.verifyPhoneNumber(options)

    }

    private fun resendVerificationCode(
        phoneNumber: String,
        callbacks: PhoneAuthProvider.OnVerificationStateChangedCallbacks,
        token: PhoneAuthProvider.ForceResendingToken
    ) {
        val options = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(phoneNumber)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(this)
            .setCallbacks(callbacks)
            .setForceResendingToken(token)
            .build()
        PhoneAuthProvider.verifyPhoneNumber(options)
        startResendCooldown()
    }

    private fun verifyCode(code: String) {
        val credential = PhoneAuthProvider.getCredential(storedVerificationId!!, code)
        signInWithPhoneAuthCredential(credential)
    }

    private fun startResendCooldown() {
        binding.resendOtp.isEnabled = false
        binding.resendOtp.visibility = TextView.INVISIBLE
        binding.otpTimer.visibility = TextView.VISIBLE
        countdownTimer = object : CountDownTimer(60000, 1000) {
            @SuppressLint("SetTextI18n")
            override fun onTick(millisUntilFinished: Long) {
                binding.otpTimer.text = "${millisUntilFinished / 1000}s"
            }

            override fun onFinish() {
                binding.resendOtp.isEnabled = true
                binding.resendOtp.visibility = TextView.VISIBLE
                binding.otpTimer.visibility = TextView.GONE
            }
        }.start()
    }

    private fun signInWithPhoneAuthCredential(credential: PhoneAuthCredential) {
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                dialog.dismiss()  // Hide dialog after sign-in attempt
                if (task.isSuccessful) {
                    Toast.makeText(this, "Verification successful!", Toast.LENGTH_SHORT).show()
                    val verifyIntent = Intent(this@LoginActivity, VerifyActivity::class.java)
                    verifyIntent.putExtra("voterInfo", voterinfo as Serializable)
                    verifyIntent.putExtra("aadhar" ,aadharNum)
                    startActivity(verifyIntent)
                } else {
                    Log.w("MainActivity", "signInWithCredential:failure", task.exception)
                    Toast.makeText(this, "Verification failed", Toast.LENGTH_SHORT).show()
                }
            }
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_DEL && binding.otpTextView2.otp!!.isEmpty() && binding.otpTextView2.hasFocus()) {
            binding.otpTextView1.requestFocusOTP()
        }
        if (keyCode == KeyEvent.KEYCODE_DEL && binding.otpTextView3.otp!!.isEmpty() && binding.otpTextView3.hasFocus()) {
            binding.otpTextView2.requestFocusOTP()
        }
        if (keyCode == KeyEvent.KEYCODE_DEL && binding.otpTextView1.hasFocus()) {
            binding.otpTextView1.requestFocusOTP()
        }
        return super.onKeyUp(keyCode, event)
    }

    override fun onDestroy() {
        super.onDestroy()
        countdownTimer.cancel() // Clean up the timer if the activity is destroyed
    }

}


