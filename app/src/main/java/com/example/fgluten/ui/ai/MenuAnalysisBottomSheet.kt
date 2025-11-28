package com.example.fgluten.ui.ai

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.fgluten.R
import com.example.fgluten.data.ai.*
import com.example.fgluten.databinding.MenuAnalysisBottomSheetBinding
import com.example.fgluten.databinding.ItemAiMenuAnalysisBinding
import com.google.android.flexbox.FlexboxLayout
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.launch

/**
 * Bottom sheet dialog for displaying AI-powered menu analysis results
 * 
 * This dialog fragment provides a comprehensive interface for showing
 * gluten-free safety analysis results powered by AI. It displays:
 * 
 * - Overall restaurant safety level with confidence scoring
 * - Statistical breakdown of analyzed menu items
 * - Detailed analysis of individual menu items
 * - Expandable details showing reasoning and keywords
 * - Action buttons for retry and analysis management
 * 
 * The dialog integrates with AIMenuAnalysisViewModel for state management
 * and provides a professional Material Design interface.
 * 
 * @see AIMenuAnalysisViewModel for the underlying ViewModel
 * @see MenuAnalysisAdapter for item list management
 */
class MenuAnalysisBottomSheet : BottomSheetDialogFragment() {

    private var _binding: MenuAnalysisBottomSheetBinding? = null
    private val binding get() = _binding!!

    private val menuAnalysisViewModel: AIMenuAnalysisViewModel by viewModels()
    private lateinit var analysisAdapter: MenuAnalysisAdapter

    companion object {
        private const val ARG_RESTAURANT_NAME = "restaurant_name"
        private const val ARG_MENU_TEXT = "menu_text"
        private const val ARG_SOURCE_TYPE = "source_type"

        /**
         * Create a new instance of MenuAnalysisBottomSheet with arguments
         * 
         * @param restaurantName Name of the restaurant being analyzed
         * @param menuText Menu text to be analyzed
         * @param sourceType Source of the menu data
         * @return New MenuAnalysisBottomSheet instance
         */
        fun newInstance(
            restaurantName: String,
            menuText: String,
            sourceType: AnalysisSource = AnalysisSource.WEBSITE
        ): MenuAnalysisBottomSheet {
            return MenuAnalysisBottomSheet().apply {
                arguments = Bundle().apply {
                    putString(ARG_RESTAURANT_NAME, restaurantName)
                    putString(ARG_MENU_TEXT, menuText)
                    putString(ARG_SOURCE_TYPE, sourceType.name)
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = MenuAnalysisBottomSheetBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupRecyclerView()
        setupClickListeners()
        observeViewModel()
        
        // Start analysis if arguments are provided
        arguments?.let { args ->
            val restaurantName = args.getString(ARG_RESTAURANT_NAME, "")
            val menuText = args.getString(ARG_MENU_TEXT, "")
            val sourceType = AnalysisSource.valueOf(
                args.getString(ARG_SOURCE_TYPE, AnalysisSource.WEBSITE.name)
            )
            
            if (restaurantName.isNotBlank() && menuText.isNotBlank()) {
                startAnalysis(restaurantName, menuText, sourceType)
            }
        }
    }

    /**
     * Setup RecyclerView for displaying detailed analysis items
     */
    private fun setupRecyclerView() {
        analysisAdapter = MenuAnalysisAdapter { item, position ->
            // Handle item click if needed
            showItemDetails(item, position)
        }
        
        binding.detailedItemsList.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = analysisAdapter
        }
    }

    /**
     * Setup click listeners for all interactive elements
     */
    private fun setupClickListeners() {
        binding.apply {
            // Action buttons
            viewDetailedAnalysis.setOnClickListener {
                toggleDetailedView()
            }
            
            retryAnalysis.setOnClickListener {
                menuAnalysisViewModel.retryAnalysis()
            }
            
            dismissAnalysis.setOnClickListener {
                dismiss()
            }
            
            analyzeNewMenu.setOnClickListener {
                showNewMenuDialog()
            }
        }
    }

    /**
     * Observe ViewModel state changes and update UI accordingly
     */
    private fun observeViewModel() {
        lifecycleScope.launch {
            menuAnalysisViewModel.uiState.collect { uiState ->
                updateUIForState(uiState)
            }
        }
        
        lifecycleScope.launch {
            menuAnalysisViewModel.aiStatus.collect { status ->
                updateStatusIndicator(status)
            }
        }
    }

    /**
     * Update UI based on current analysis state
     */
    private fun updateUIForState(uiState: AIMenuAnalysisUiState) {
        when (uiState) {
            is AIMenuAnalysisUiState.Initial -> {
                showLoadingState()
            }
            
            is AIMenuAnalysisUiState.Loading -> {
                showLoadingState()
            }
            
            is AIMenuAnalysisUiState.Success -> {
                showResultsState(uiState.result)
            }
            
            is AIMenuAnalysisUiState.Error -> {
                showErrorState(uiState.message)
            }
            
            is AIMenuAnalysisUiState.NoAnalysis -> {
                showNoAnalysisState(uiState.reason)
            }
        }
    }

    /**
     * Update AI service status indicator
     */
    private fun updateStatusIndicator(status: AIStatus) {
        val (color, text) = when (status) {
            is AIStatus.Ready -> Pair(
                R.color.ai_status_ready,
                "AI Ready"
            )
            is AIStatus.Downloading -> Pair(
                R.color.ai_status_downloading,
                "Downloading AI Models..."
            )
            is AIStatus.NotAvailable -> Pair(
                R.color.ai_status_not_available,
                "AI Unavailable"
            )
            is AIStatus.Error -> Pair(
                R.color.ai_status_error,
                "AI Error"
            )
        }
        
        // Update confidence indicator if available
        binding.confidenceIndicator.apply {
            isIndeterminate = status is AIStatus.Downloading
            visibility = if (status is AIStatus.Downloading) View.VISIBLE else View.VISIBLE
        }
    }

    /**
     * Show loading state in the UI
     */
    private fun showLoadingState() {
        binding.apply {
            loadingContainer.visibility = View.VISIBLE
            resultsContainer.visibility = View.GONE
            errorContainer.visibility = View.GONE
        }
    }

    /**
     * Show analysis results in the UI
     */
    private fun showResultsState(result: MenuAnalysisResult) {
        binding.apply {
            loadingContainer.visibility = View.GONE
            resultsContainer.visibility = View.VISIBLE
            errorContainer.visibility = View.GONE
            
            // Update confidence indicator
            confidenceIndicator.apply {
                setProgress((result.confidenceScore * 100).toInt(), true)
            }
            
            // Update safety level
            updateSafetyLevel(result.overallScore)
            
            // Update statistics
            val summary = result.getSummary()
            gfSafeCount.text = summary.gfSafeCount.toString()
            likelyGfCount.text = summary.likelyGFCount.toString()
            warningCount.text = summary.warningCount.toString()
            
            // Update detailed items list
            analysisAdapter.updateItems(result.analyzedItems)
        }
    }

    /**
     * Update safety level display
     */
    private fun updateSafetyLevel(level: GFSafetyLevel) {
        binding.apply {
            val (backgroundColor, titleColor, description) = when (level) {
                GFSafetyLevel.EXCELLENT -> Triple(
                    R.color.safety_excellent_bg,
                    R.color.safety_excellent_text,
                    "Restaurant has confirmed gluten-free options with high confidence"
                )
                GFSafetyLevel.GOOD -> Triple(
                    R.color.safety_good_bg,
                    R.color.safety_good_text,
                    "Restaurant appears to have reliable gluten-free choices"
                )
                GFSafetyLevel.LIMITED -> Triple(
                    R.color.safety_limited_bg,
                    R.color.safety_limited_text,
                    "Restaurant has some gluten-free options but may be limited"
                )
                GFSafetyLevel.POOR -> Triple(
                    R.color.safety_poor_bg,
                    R.color.safety_poor_text,
                    "Very limited or uncertain gluten-free options"
                )
                GFSafetyLevel.UNKNOWN -> Triple(
                    R.color.safety_unknown_bg,
                    R.color.safety_unknown_text,
                    "Unable to assess gluten-free options"
                )
            }
            
            safetyLevelCard.setCardBackgroundColor(
                resources.getColor(backgroundColor, requireContext().theme)
            )
            safetyLevelTitle.text = level.displayName
            safetyLevelTitle.setTextColor(
                resources.getColor(titleColor, requireContext().theme)
            )
            safetyLevelDescription.text = description
            safetyLevelDescription.setTextColor(
                resources.getColor(titleColor, requireContext().theme)
            )
        }
    }

    /**
     * Show error state in the UI
     */
    private fun showErrorState(message: String) {
        binding.apply {
            loadingContainer.visibility = View.GONE
            resultsContainer.visibility = View.GONE
            errorContainer.visibility = View.VISIBLE
            
            errorMessage.text = message
        }
    }

    /**
     * Show no analysis state
     */
    private fun showNoAnalysisState(reason: String) {
        binding.apply {
            loadingContainer.visibility = View.GONE
            resultsContainer.visibility = View.GONE
            errorContainer.visibility = View.VISIBLE
            
            errorMessage.text = reason
            retryAnalysis.visibility = View.GONE
        }
    }

    /**
     * Toggle detailed view visibility
     */
    private fun toggleDetailedView() {
        val isVisible = binding.detailedItemsList.visibility == View.VISIBLE
        binding.detailedItemsList.visibility = if (isVisible) View.GONE else View.VISIBLE
        
        val buttonText = if (isVisible) {
            "View Detailed Analysis"
        } else {
            "Hide Detailed Analysis"
        }
        binding.viewDetailedAnalysis.text = buttonText
    }

    /**
     * Start menu analysis
     */
    private fun startAnalysis(
        restaurantName: String,
        menuText: String,
        sourceType: AnalysisSource
    ) {
        menuAnalysisViewModel.analyzeMenu(menuText, restaurantName, sourceType)
    }

    /**
     * Show item details (placeholder for future implementation)
     */
    private fun showItemDetails(item: AnalyzedMenuItem, position: Int) {
        // Future implementation: show detailed dialog for individual items
        Toast.makeText(
            requireContext(),
            "Detailed view for: ${item.name}",
            Toast.LENGTH_SHORT
        ).show()
    }

    /**
     * Show dialog for analyzing new menu (placeholder for future implementation)
     */
    private fun showNewMenuDialog() {
        // Future implementation: show dialog to input new menu text
        Toast.makeText(
            requireContext(),
            "New menu analysis feature coming soon!",
            Toast.LENGTH_SHORT
        ).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

/**
 * RecyclerView adapter for displaying individual menu analysis items
 */
class MenuAnalysisAdapter(
    private val onItemClick: (AnalyzedMenuItem, Int) -> Unit
) : androidx.recyclerview.widget.ListAdapter<AnalyzedMenuItem, MenuAnalysisAdapter.ViewHolder>(
    AnalyzedMenuItemDiffCallback()
) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemAiMenuAnalysisBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding, onItemClick)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position), position)
    }

    fun updateItems(items: List<AnalyzedMenuItem>) {
        submitList(items)
    }

    class ViewHolder(
        private val binding: ItemAiMenuAnalysisBinding,
        private val onItemClick: (AnalyzedMenuItem, Int) -> Unit
    ) : androidx.recyclerview.widget.RecyclerView.ViewHolder(binding.root) {

        fun bind(item: AnalyzedMenuItem, position: Int) {
            binding.apply {
                // Classification
                classificationIcon.text = item.classification.displayIcon
                classificationText.text = item.classification.displayName
                
                // Confidence
                itemConfidence.setProgress((item.confidence * 100).toInt(), true)
                
                // Item details
                itemName.text = item.name
                itemDescription.text = item.description
                reasoningText.text = item.reasoning
                
                // Expandable details
                expandDetails.setOnClickListener {
                    val isVisible = keywordsContainer.visibility == View.VISIBLE
                    keywordsContainer.visibility = if (isVisible) View.GONE else View.VISIBLE
                    expandDetails.text = if (isVisible) "Show Details" else "Hide Details"
                    
                    if (!isVisible) {
                        updateKeywordsAndWarnings(item)
                    }
                }
                
                // Item click
                root.setOnClickListener {
                    onItemClick(item, position)
                }
            }
        }

        private fun updateKeywordsAndWarnings(item: AnalyzedMenuItem) {
            // Update keywords flow
            binding.keywordsFlow.removeAllViews()
            item.keywords.forEach { keyword ->
                val keywordView = createKeywordChip(keyword, true)
                binding.keywordsFlow.addView(keywordView)
            }
            
            // Update warnings flow
            if (item.warnings.isNotEmpty()) {
                binding.warningsLabel.visibility = View.VISIBLE
                binding.warningsFlow.visibility = View.VISIBLE
                
                binding.warningsFlow.removeAllViews()
                item.warnings.forEach { warning ->
                    val warningView = createKeywordChip(warning, false)
                    binding.warningsFlow.addView(warningView)
                }
            } else {
                binding.warningsLabel.visibility = View.GONE
                binding.warningsFlow.visibility = View.GONE
            }
        }

        private fun createKeywordChip(keyword: String, isPositive: Boolean): View {
            val chip = com.google.android.material.chip.Chip(
                binding.root.context
            ).apply {
                text = keyword
                if (isPositive) {
                    chipBackgroundColor = android.content.res.ColorStateList.valueOf(
                        binding.root.context.getColor(R.color.keyword_positive_bg)
                    )
                    setTextColor(
                        binding.root.context.getColor(R.color.keyword_positive_text)
                    )
                } else {
                    chipBackgroundColor = android.content.res.ColorStateList.valueOf(
                        binding.root.context.getColor(R.color.keyword_warning_bg)
                    )
                    setTextColor(
                        binding.root.context.getColor(R.color.keyword_warning_text)
                    )
                }
                isCheckable = false
                isClickable = false
            }
            return chip
        }
    }
}

/**
 * Diff callback for RecyclerView list updates
 */
class AnalyzedMenuItemDiffCallback : androidx.recyclerview.widget.DiffUtil.ItemCallback<AnalyzedMenuItem>() {
    override fun areItemsTheSame(oldItem: AnalyzedMenuItem, newItem: AnalyzedMenuItem): Boolean {
        return oldItem.name == newItem.name && oldItem.description == newItem.description
    }

    override fun areContentsTheSame(oldItem: AnalyzedMenuItem, newItem: AnalyzedMenuItem): Boolean {
        return oldItem == newItem
    }
}