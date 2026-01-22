package com.autodroid.teachitback.ui

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.fragment.app.DialogFragment
import com.autodroid.teachitback.databinding.DialogAddTopicBinding
import com.autodroid.teachitback.viewmodel.TopicsViewModel
import androidx.lifecycle.ViewModelProvider

class AddTopicDialogFragment : DialogFragment() {

    private var _binding: DialogAddTopicBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: TopicsViewModel
    private var onTopicAdded: (() -> Unit)? = null

    fun setOnTopicAddedListener(listener: () -> Unit) {
        onTopicAdded = listener
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        return dialog
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogAddTopicBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(requireActivity())[TopicsViewModel::class.java]

        setupClickListeners()
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.9).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    private fun setupClickListeners() {
        binding.cancelButton.setOnClickListener {
            dismiss()
        }

        binding.confirmButton.setOnClickListener {
            val title = binding.topicTitleInput.text.toString().trim()
            val description = binding.topicDescriptionInput.text.toString().trim()

            if (title.isNotEmpty()) {
                viewModel.insertTopic(title, description)
                onTopicAdded?.invoke()
                dismiss()
            } else {
                binding.topicTitleInput.error = "请输入主题名称"
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance(): AddTopicDialogFragment {
            return AddTopicDialogFragment()
        }
    }
}
