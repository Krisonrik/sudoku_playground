package com.example.sudoku_playground

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import timber.log.Timber

class SudokuCreationFragment : Fragment(R.layout.fragment_sudoku_creation) {

    private val viewModel: SudokuCreationViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.digitUpdate.collect { state ->
                // Handle the positional digit status update
                // For example, update the UI or perform any necessary actions
                activity?.let {
                    val resId = resources.getIdentifier(
                        "number_${state.mainBlockId}_${state.subIndex}",
                        "id",
                        it.packageName
                    )
//                    Timber.d("resId: $resId, mainBlockId: ${state.mainBlockId}, subIndex: ${state.subIndex}")
                    view.findViewById<TextView>(resId).text = state.digit.toString()
                }

            }
        }

        viewModel.fillAllValues()
    }
}