package com.example.voting

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import androidx.databinding.BindingAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.voting.databinding.CandidateItemsBinding

class VoterAdapter(private val items: List<VoterModel>,
                   private val clickListener: OnItemClickListener) :
    RecyclerView.Adapter<VoterAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: CandidateItemsBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = CandidateItemsBinding.inflate(inflater, parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.binding.items = item
        holder.binding.root.setOnClickListener {
            clickListener.onItemClick(position)
        }
        holder.binding.executePendingBindings()
    }

    override fun getItemCount(): Int = items.size

    interface OnItemClickListener {
        fun onItemClick(position: Int)
    }

    companion object {
        @Override
        @BindingAdapter("app:imageRes")
        @JvmStatic
        fun bindImage(imageView: ImageView, imageResId: Int) {
            imageView.setImageResource(imageResId)
        }
    }


}
