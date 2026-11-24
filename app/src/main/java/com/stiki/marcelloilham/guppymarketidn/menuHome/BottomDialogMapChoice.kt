package com.stiki.marcelloilham.guppymarketidn.menuHome

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.stiki.marcelloilham.guppymarketidn.MainActivity
import com.stiki.marcelloilham.guppymarketidn.R

class BottomDialogMapChoice(
    private val farmName: String = "Jakarta Guppy House",
    private val address: String = "Jl. Lebak Bulus Raya No. 42, Cilandak, Jakarta Selatan"
) : BottomSheetDialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.bottom_sheet_map_choice, container, false)

        val btnClose = view.findViewById<ImageView>(R.id.btnCloseMapChoice)
        val btnInAppMap = view.findViewById<View>(R.id.btnChoiceInAppMap)
        val btnGoogleMaps = view.findViewById<View>(R.id.btnChoiceGoogleMaps)

        btnClose.setOnClickListener { dismiss() }

        // Option 1: In-App MapLibre OpenStreetMap
        btnInAppMap.setOnClickListener {
            val intent = Intent(requireContext(), MainActivity::class.java).apply {
                putExtra("NAVIGATE_TO_TAB", "MAPS")
                putExtra("SEARCH_FARM_QUERY", farmName)
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            startActivity(intent)
            dismiss()
        }

        // Option 2: External Google Maps App
        btnGoogleMaps.setOnClickListener {
            val mapUri = Uri.parse("https://www.google.com/maps/search/?api=1&query=${Uri.encode("$farmName, $address")}")
            val mapIntent = Intent(Intent.ACTION_VIEW, mapUri)
            startActivity(mapIntent)
            dismiss()
        }

        return view
    }
}
