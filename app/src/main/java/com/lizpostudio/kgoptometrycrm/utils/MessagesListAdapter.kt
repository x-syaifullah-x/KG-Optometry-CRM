package com.lizpostudio.kgoptometrycrm.utils


import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.lizpostudio.kgoptometrycrm.data.source.remote.firebase.KGMessage
import com.lizpostudio.kgoptometrycrm.databinding.ItemMessageBinding


class MessagesListAdapter : ListAdapter<KGMessage, MessagesListAdapter.ViewHolder>(MessageItemDiffCallback()){

  //  var finalItemSelected = MutableLiveData<Patients>()

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val messageItem = getItem(position)
        holder.bind(messageItem)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder.from(parent)
    }

    class ViewHolder private constructor(val binding: ItemMessageBinding): RecyclerView.ViewHolder(binding.root) {

        fun bind(item: KGMessage) {
            binding.kgmessages = item
            binding.executePendingBindings()
        }
        companion object {
            fun from(parent: ViewGroup): ViewHolder {
                val layoutInflater = LayoutInflater.from(parent.context)
                val binding = ItemMessageBinding.inflate(layoutInflater, parent, false)
                return ViewHolder(binding)
            }
        }

    }
}

class MessageItemDiffCallback : DiffUtil.ItemCallback<KGMessage>() {
    override fun areItemsTheSame(oldItem: KGMessage, newItem: KGMessage): Boolean {
        return (oldItem.body == newItem.body)
    }

    override fun areContentsTheSame(oldItem: KGMessage, newItem: KGMessage): Boolean {
        return (oldItem.body == newItem.body)
    }
}
