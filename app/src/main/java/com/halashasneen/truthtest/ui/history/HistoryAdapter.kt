package com.halashasneen.truthtest.ui.history

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.halashasneen.truthtest.R
import com.halashasneen.truthtest.data.HistoryRepository
import com.halashasneen.truthtest.data.model.TestResult
import com.halashasneen.truthtest.databinding.ItemHistoryBinding
import java.text.DateFormat
import java.util.Date

class HistoryAdapter(
    private val onClick: (TestResult) -> Unit,
    private var items: List<TestResult> = emptyList()
) : RecyclerView.Adapter<HistoryAdapter.Holder>() {

    class Holder(val binding: ItemHistoryBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder = Holder(
        ItemHistoryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    )

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val item = items[position]
        val context = holder.itemView.context

        holder.binding.score.text = item.score.toString() + "%"
        holder.binding.question.text = item.question
        holder.binding.meta.text = context.getString(
            R.string.history_item_meta_format,
            modeName(context, item.mode),
            categoryName(context, item.category)
        )
        holder.binding.date.text = DateFormat.getDateTimeInstance(
            DateFormat.MEDIUM,
            DateFormat.SHORT
        ).format(Date(item.timestamp))
        holder.binding.root.setOnClickListener { onClick(item) }
    }

    fun submit(newItems: List<TestResult>) {
        items = newItems
        notifyDataSetChanged()
    }

    private fun modeName(context: android.content.Context, mode: String): String = when {
        mode == "solo" -> context.getString(R.string.solo_test)
        mode == "custom" -> context.getString(R.string.custom_question)
        mode == HistoryRepository.MODE_DAILY -> context.getString(R.string.daily_challenge)
        mode.startsWith("duel_") -> context.getString(R.string.duel_mode)
        mode.startsWith("group") -> context.getString(R.string.group_mode)
        else -> mode
    }

    private fun categoryName(context: android.content.Context, category: String): String = when (category) {
        "embarrassing" -> context.getString(R.string.embarrassing)
        "funny" -> context.getString(R.string.funny)
        "bold" -> context.getString(R.string.bold)
        "romantic" -> context.getString(R.string.romantic)
        "friendship" -> context.getString(R.string.friendship)
        "family" -> context.getString(R.string.family)
        "custom" -> context.getString(R.string.custom_question)
        else -> category
    }
}
