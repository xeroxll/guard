package com.guardian.app.ui.screens

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.guardian.app.R
import com.guardian.app.databinding.FragmentHomeBinding
import com.guardian.app.viewmodel.GuardianViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class HomeFragment : Fragment() {
    
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: GuardianViewModel by activityViewModels()
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViews()
        observeData()
    }
    
    private fun setupViews() {
        binding.shieldButton.setOnClickListener {
            viewModel.startScan()
        }
        
        binding.protectionToggle.setOnClickListener {
            viewModel.toggleProtection()
        }
        
        binding.usbCard.setOnClickListener {
            Toast.makeText(context, "Эта функция доступна только на Android", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun observeData() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.isProtectionEnabled.collectLatest { enabled ->
                updateProtectionUI(enabled)
            }
        }
        
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.usbStatus.collectLatest { status ->
                binding.usbStatusText.text = if (status) getString(R.string.usb_on) else getString(R.string.usb_off)
                binding.usbIcon.setColorFilter(
                    if (status) resources.getColor(R.color.red_danger, null) 
                    else resources.getColor(R.color.blue_info, null)
                )
            }
        }
        
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.stats.collectLatest { stats ->
                binding.threatsCount.text = stats.threats.toString()
                binding.blocksCount.text = stats.blocks.toString()
                binding.checksCount.text = stats.checks.toString()
            }
        }
        
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.isScanning.collectLatest { scanning ->
                if (scanning) {
                    binding.scanText.text = getString(R.string.scanning)
                    binding.shieldIcon.setImageResource(R.drawable.ic_alert)
                } else {
                    binding.scanText.text = ""
                }
                binding.shieldButton.isEnabled = !scanning
            }
        }
    }
    
    private fun updateProtectionUI(enabled: Boolean) {
        binding.protectionStatus.text = if (enabled) {
            getString(R.string.protection_active)
        } else {
            getString(R.string.protection_inactive)
        }
        
        binding.protectionBadge.text = if (enabled) {
            getString(R.string.protection_enabled)
        } else {
            getString(R.string.protection_disabled)
        }
        
        binding.protectionBadge.setBackgroundColor(
            if (enabled) resources.getColor(R.color.green_success, null)
            else resources.getColor(R.color.red_danger, null)
        )
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
