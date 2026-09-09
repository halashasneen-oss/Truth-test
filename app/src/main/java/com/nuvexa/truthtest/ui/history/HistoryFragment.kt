package com.nuvexa.truthtest.ui.history

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.nuvexa.truthtest.data.HistoryRepository
import com.nuvexa.truthtest.databinding.FragmentHistoryBinding

class HistoryFragment : Fragment() {
    private var _binding: FragmentHistoryBinding? = null
    private val binding get() = _binding!!
    private val adapter = HistoryAdapter()
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, state: Bundle?): View { _binding = FragmentHistoryBinding.inflate(inflater, container, false); return binding.root }
    override fun onViewCreated(view: View, state: Bundle?) { binding.historyList.layoutManager = LinearLayoutManager(requireContext()); binding.historyList.adapter = adapter }
    override fun onResume() { super.onResume(); val items = HistoryRepository(requireContext()).getAll(); adapter.submit(items); binding.emptyText.visibility = if (items.isEmpty()) View.VISIBLE else View.GONE }
    override fun onDestroyView() { _binding = null; super.onDestroyView() }
}
