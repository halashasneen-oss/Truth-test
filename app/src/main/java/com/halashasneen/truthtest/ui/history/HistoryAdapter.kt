package com.halashasneen.truthtest.ui.history

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
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
        holder.binding.score.text = "${item.score}%"
        holder.binding.question.text = item.question
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
}
