package ayx.copy.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import ayx.copy.R
import ayx.copy.database.Command
import ayx.copy.databinding.ItemCommandBinding
import ayx.copy.utils.AnimationUtil

class CommandAdapter(
    private val onItemLongClick: (Command) -> Unit,
    private val onFavoriteClick: (Command) -> Unit,
    private val onCopyClick: (Command) -> Unit
) : ListAdapter<Command, CommandAdapter.CommandViewHolder>(DIFF_CALLBACK) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommandViewHolder {
        val binding = ItemCommandBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return CommandViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CommandViewHolder, position: Int) {
        // No per-bind animation: replaying it on every scroll bind = jank.
        // Entrance stays smooth via the list itself; taps still bounce.
        holder.bind(getItem(position))
    }

    inner class CommandViewHolder(private val binding: ItemCommandBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(command: Command) {
            binding.tvCommand.text = command.command
            binding.tvText.text = command.replacementText
            binding.tvUseCount.text = if (command.useCount > 0) {
                "Used ${command.useCount}×"
            } else {
                "Not used yet"
            }

            binding.btnFavorite.setImageResource(
                if (command.isFavorite) R.drawable.ic_star_filled else R.drawable.ic_star
            )
            binding.btnFavorite.setColorFilter(
                holder.itemView.context.getColor(if (command.isFavorite) R.color.accent else R.color.text_tertiary),
                android.graphics.PorterDuff.Mode.SRC_IN
            )

            // Child TextViews must NOT consume touch — let card handle long press
            binding.tvCommand.isClickable  = false
            binding.tvText.isClickable     = false
            binding.tvUseCount.isClickable = false
            binding.tvCommand.isLongClickable  = false
            binding.tvText.isLongClickable     = false
            binding.tvUseCount.isLongClickable = false

            // Long press on the card → options sheet (Edit / Delete / Duplicate / Share)
            binding.card.setOnLongClickListener {
                AnimationUtil.scaleBounce(binding.card)
                onItemLongClick(command)
                true
            }

            // Star button — toggle favorite
            binding.btnFavorite.setOnClickListener {
                AnimationUtil.scaleBounce(binding.btnFavorite)
                onFavoriteClick(command)
            }

            // Copy button — explicit copy action
            binding.btnCopy.setOnClickListener {
                AnimationUtil.scaleBounce(binding.btnCopy)
                onCopyClick(command)
            }
        }
    }

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<Command>() {
            override fun areItemsTheSame(oldItem: Command, newItem: Command) =
                oldItem.id == newItem.id
            override fun areContentsTheSame(oldItem: Command, newItem: Command) =
                oldItem == newItem
        }
    }
}
