package com.nuvexa.truthtest.ui.history

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.nuvexa.truthtest.R
import com.nuvexa.truthtest.data.HistoryRepository
import com.nuvexa.truthtest.data.QuestionRepository
import com.nuvexa.truthtest.data.model.TestResult
import com.nuvexa.truthtest.databinding.FragmentHistoryBinding
import com.nuvexa.truthtest.share.ResultCardRenderer
import com.nuvexa.truthtest.share.ShareTheme
import com.nuvexa.truthtest.share.ShareThemeContext
import java.text.DateFormat
import java.util.Date
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HistoryFragment : Fragment() {
    private var _binding: FragmentHistoryBinding? = null
    private val binding get() = _binding!!
    private val repository by lazy { HistoryRepository(requireContext()) }
    private val adapter by lazy { HistoryAdapter(::showResultDetails) }
    private var activeFilter = HistoryFilter.ALL

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        state: Bundle?
    ): View {
        _binding = FragmentHistoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, state: Bundle?) {
        binding.historyList.layoutManager = LinearLayoutManager(requireContext())
        binding.historyList.adapter = adapter
        binding.historyFilterButton.setOnClickListener { showFilterPicker() }
        updateFilterLabel()
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    private fun refresh() {
        val items = repository.getAll().filter(activeFilter::matches)
        adapter.submit(items)
        binding.emptyText.visibility = if (items.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun showFilterPicker() {
        val filters = HistoryFilter.entries
        val labels = filters.map(::filterLabel).toTypedArray()
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.history_filter_title)
            .setSingleChoiceItems(labels, filters.indexOf(activeFilter)) { dialog, which ->
                activeFilter = filters[which]
                updateFilterLabel()
                refresh()
                dialog.dismiss()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun updateFilterLabel() {
        binding.historyFilterButton.text = getString(
            R.string.history_filter_format,
            filterLabel(activeFilter)
        )
    }

    private fun filterLabel(filter: HistoryFilter): String = getString(
        when (filter) {
            HistoryFilter.ALL -> R.string.history_filter_all
            HistoryFilter.SOLO -> R.string.solo_test
            HistoryFilter.DUEL -> R.string.duel_mode
            HistoryFilter.GROUP -> R.string.group_mode
            HistoryFilter.CUSTOM -> R.string.custom_question
            HistoryFilter.LIGHT -> R.string.intensity_light
            HistoryFilter.MEDIUM -> R.string.intensity_medium
            HistoryFilter.BOLD -> R.string.intensity_bold
        }
    )

    private fun showResultDetails(item: TestResult) {
        val date = DateFormat.getDateTimeInstance(DateFormat.FULL, DateFormat.SHORT)
            .format(Date(item.timestamp))
        val details = buildString {
            append(getString(R.string.history_score_label)).append(": ").append(item.score).append("%\n")
            append(getString(R.string.history_mode_label)).append(": ").append(modeName(item.mode)).append("\n")
            append(getString(R.string.history_category_label)).append(": ").append(categoryName(item.category)).append("\n")
            item.intensity?.let {
                append(getString(R.string.history_intensity_label)).append(": ").append(intensityName(it)).append("\n")
            }
            append(getString(R.string.history_date_label)).append(": ").append(date).append("\n\n")
            append(item.question)
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.history_result_details)
            .setMessage(details)
            .setPositiveButton(R.string.history_share) { _, _ -> shareResult(item) }
            .setNeutralButton(R.string.history_delete) { _, _ -> confirmDelete(item) }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun confirmDelete(item: TestResult) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.history_delete_title)
            .setMessage(R.string.history_delete_message)
            .setPositiveButton(R.string.history_delete) { _, _ ->
                repository.delete(item.id)
                refresh()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun shareResult(item: TestResult) {
        val appContext = requireContext().applicationContext
        val sharePrefs = appContext.getSharedPreferences("truth_test_share", Context.MODE_PRIVATE)
        val theme = ShareTheme.fromStorage(sharePrefs.getString("share_theme", null))
        viewLifecycleOwner.lifecycleScope.launch {
            val uri = withContext(Dispatchers.Default) {
                ResultCardRenderer.render(
                    context = ShareThemeContext.wrap(appContext, theme),
                    question = item.question,
                    score = item.score
                )
            }
            startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_TEXT, getString(R.string.history_share_text, item.score))
                clipData = ClipData.newUri(requireContext().contentResolver, getString(R.string.share_result), uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }, getString(R.string.share_result)))
        }
    }

    private fun modeName(mode: String): String = when {
        mode == "solo" -> getString(R.string.solo_test)
        mode == "custom" -> getString(R.string.custom_question)
        mode.startsWith("duel_") -> getString(R.string.duel_mode)
        mode.startsWith("group") -> getString(R.string.group_mode)
        else -> mode
    }

    private fun categoryName(category: String): String = when (category) {
        "embarrassing" -> getString(R.string.embarrassing)
        "funny" -> getString(R.string.funny)
        "bold" -> getString(R.string.bold)
        "romantic" -> getString(R.string.romantic)
        "friendship" -> getString(R.string.friendship)
        "family" -> getString(R.string.family)
        "custom" -> getString(R.string.custom_question)
        else -> category
    }

    private fun intensityName(intensity: String): String = getString(
        when (intensity) {
            QuestionRepository.INTENSITY_LIGHT -> R.string.intensity_light
            QuestionRepository.INTENSITY_BOLD -> R.string.intensity_bold
            else -> R.string.intensity_medium
        }
    )

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
