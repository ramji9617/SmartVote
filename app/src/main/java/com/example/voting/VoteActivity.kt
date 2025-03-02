package com.example.voting

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.swipebutton_library.SwipeButton
import com.example.voting.databinding.ActivityVoteBinding
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class VoteActivity : AppCompatActivity(), VoterAdapter.OnItemClickListener {

    private lateinit var binding: ActivityVoteBinding
    private val db = Firebase.firestore
  private  lateinit var cnfrmDialog : BottomSheetDialog
  private  lateinit var dialog: Dialog
  private  lateinit var  bottomView: View
    private val voterList = mutableListOf<VoterModel>() // Global list for data
    private lateinit var adapter: VoterAdapter // Adapter reference

    @SuppressLint("InflateParams")
    override fun onCreate(savedInstanceState: Bundle?) {
        binding = ActivityVoteBinding.inflate(layoutInflater)
        val constituency = intent.getStringExtra("constituency")?: "Rajnagar"
        dialog = Dialog(this).apply {
            setContentView(R.layout.progressbar)
            window?.setBackgroundDrawableResource(android.R.color.transparent)
            setCancelable(false)
        }
        if (constituency.isNotEmpty()) {
            fetchCandidates(constituency)
        } else {
            Toast.makeText(this, "Constituency not provided", Toast.LENGTH_SHORT).show()
        }
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        // Handle insets for system bars
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }



         bottomView = layoutInflater.inflate(R.layout.bottomdialog , null)

        // Initialize RecyclerView , dialog and Adapter
        adapter = VoterAdapter(voterList,this)
        binding.recyclerview.layoutManager = LinearLayoutManager(this)
        binding.recyclerview.adapter = adapter
        cnfrmDialog = BottomSheetDialog(this)
        cnfrmDialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        cnfrmDialog.setContentView(bottomView)


        // Fetch constituency from intent

    }
    override fun onItemClick(position: Int) {
        val clickedItem = voterList[position]
        Toast.makeText(this, "Clicked: ${clickedItem.party}", Toast.LENGTH_SHORT).show()
       setupBottomSheet(clickedItem.imgLogo , clickedItem.party ,clickedItem.candidate)
        cnfrmDialog.show()
        val aadharNum = intent.getStringExtra("aadhar")

        cnfrmDialog.findViewById<SwipeButton>(R.id.btnConfirm)?.setOnActiveListener {

            cnfrmDialog.dismiss()
            dialog.show()
            confirmVote(clickedItem.party, aadharNum.toString())

        }
    }
    private fun confirmVote(party: String, aadharNum: String) {
        val voteDocRef = db.collection("votes").document("votecount")
        // Firestore transaction ensures safe update
        db.runTransaction { transaction ->
            val snapshot = transaction.get(voteDocRef)
            if (snapshot.exists()) {
                val currentCount = snapshot.getLong(party.lowercase()) ?: 0
                transaction.update(voteDocRef, party.lowercase(), currentCount + 1)
            } else {
                throw Exception("Vote count document does not exist.")
            }
        }.addOnSuccessListener {
            markVoter(aadharNum) // Proceed to mark voter as voted
        }.addOnFailureListener { e ->
            Toast.makeText(this@VoteActivity, "Voting Failed: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun markVoter(aadharNum: String) {
        val voterDocRef = db.collection("Voter").document(aadharNum)
        Log.d("aadhar" , aadharNum)

        voterDocRef.update("isvoted", true)
            .addOnSuccessListener {
                Toast.makeText(this@VoteActivity, "Vote Confirmed!", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
               val finalIntent = Intent(this@VoteActivity , LoginActivity::class.java)
                finalIntent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                startActivity(finalIntent)
            }
            .addOnFailureListener { e ->
                Toast.makeText(this@VoteActivity, "Marking Voter Failed: ${e.message}", Toast.LENGTH_LONG).show()
                Log.d("e", e.message!!)
            }
    }

    private fun setupBottomSheet(imgResource : Int, partyTxt :String, candidate :String){

        cnfrmDialog.findViewById<ImageView>(R.id.partyLogo)!!.setImageResource(imgResource)
        val partyName = "You Choose $partyTxt"
        cnfrmDialog.findViewById<TextView>(R.id.partyName)!!.text = partyName

        cnfrmDialog.findViewById<TextView>(R.id.CandidateName)!!.text = candidate
        cnfrmDialog.findViewById<ImageButton>(R.id.btnCencel)!!.setOnClickListener{
            cnfrmDialog.dismiss()
        }

    }

      @SuppressLint("NotifyDataSetChanged")
    private fun fetchCandidates(constituency: String) {
        dialog.show()
        db.collection("candidates").document(constituency).get()
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val document = task.result
                    if (document.exists()) {
                        // Update the voterList with new data
                        voterList.apply {
                            clear() // Clear existing data
                            add(VoterModel(document.getString("bjp") ?: "N/A", "BJP", R.drawable.bjp))
                            add(VoterModel(document.getString("congress") ?: "N/A", "Congress", R.drawable.congress))
                            add(VoterModel(document.getString("Aap") ?: "N/A", "AAP", R.drawable.aap))
                            add(VoterModel(document.getString("bsp") ?: "N/A", "BSP", R.drawable.bsp))
                            add(VoterModel(document.getString("jdu") ?: "N/A", "JDU", R.drawable.jdu))
                            add(VoterModel(document.getString("sp") ?: "N/A", "SP", R.drawable.sp))
                        }
                        dialog.dismiss()
                        // Notify adapter about data changes
                        adapter.notifyDataSetChanged()
                    } else {
                        dialog.dismiss()
                        Toast.makeText(this, "No candidates found for this constituency", Toast.LENGTH_SHORT).show()
                    }
                } else {
                   // dialog.dismiss()
                    Toast.makeText(this, "Failed to fetch data: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }
}
