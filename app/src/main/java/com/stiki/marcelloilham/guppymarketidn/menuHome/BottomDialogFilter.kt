package com.stiki.marcelloilham.guppymarketidn.menuHome

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.slider.RangeSlider
import com.stiki.marcelloilham.guppymarketidn.R
import java.text.NumberFormat
import java.util.Locale

class BottomDialogFilter(
    private val onFilterApplied: ((sort: String, grade: String, location: String, minPrice: Float, maxPrice: Float) -> Unit)? = null
) : BottomSheetDialogFragment() {

    private var selectedSort = "LowToHigh"
    private var selectedGrade = "All"
    private var selectedLocation = "All"
    private var minPrice = 50000f
    private var maxPrice = 1000000f

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.bottom_dialog_filter, container, false)

        val btnClose = view.findViewById<ImageView>(R.id.btnCloseDialog)
        val btnReset = view.findViewById<TextView>(R.id.btnResetFilter)
        val btnApply = view.findViewById<TextView>(R.id.btnApplyFilter)

        val chipSortLowToHigh = view.findViewById<TextView>(R.id.chipSortLowToHigh)
        val chipSortHighToLow = view.findViewById<TextView>(R.id.chipSortHighToLow)

        val chipGradeAll = view.findViewById<TextView>(R.id.chipGradeAll)
        val chipGradeAPlus = view.findViewById<TextView>(R.id.chipGradeAPlus)
        val chipGradeA = view.findViewById<TextView>(R.id.chipGradeA)

        val chipLocAll = view.findViewById<TextView>(R.id.chipLocAll)
        val chipLocMalang = view.findViewById<TextView>(R.id.chipLocMalang)
        val chipLocJakarta = view.findViewById<TextView>(R.id.chipLocJakarta)
        val chipLocBogor = view.findViewById<TextView>(R.id.chipLocBogor)

        val priceRangeValue = view.findViewById<TextView>(R.id.priceRangeValue)
        val rangeSlider = view.findViewById<RangeSlider>(R.id.continuousRangeSlider)

        btnClose.setOnClickListener { dismiss() }

        // Sort By Handlers
        chipSortLowToHigh.setOnClickListener {
            selectedSort = "LowToHigh"
            setChipSelected(chipSortLowToHigh, listOf(chipSortHighToLow))
        }
        chipSortHighToLow.setOnClickListener {
            selectedSort = "HighToLow"
            setChipSelected(chipSortHighToLow, listOf(chipSortLowToHigh))
        }

        // Grade Handlers
        chipGradeAll.setOnClickListener {
            selectedGrade = "All"
            setChipSelected(chipGradeAll, listOf(chipGradeAPlus, chipGradeA))
        }
        chipGradeAPlus.setOnClickListener {
            selectedGrade = "Grade A+"
            setChipSelected(chipGradeAPlus, listOf(chipGradeAll, chipGradeA))
        }
        chipGradeA.setOnClickListener {
            selectedGrade = "Grade A"
            setChipSelected(chipGradeA, listOf(chipGradeAll, chipGradeAPlus))
        }

        // Location Handlers
        chipLocAll.setOnClickListener {
            selectedLocation = "All"
            setChipSelected(chipLocAll, listOf(chipLocMalang, chipLocJakarta, chipLocBogor))
        }
        chipLocMalang.setOnClickListener {
            selectedLocation = "Malang"
            setChipSelected(chipLocMalang, listOf(chipLocAll, chipLocJakarta, chipLocBogor))
        }
        chipLocJakarta.setOnClickListener {
            selectedLocation = "Jakarta"
            setChipSelected(chipLocJakarta, listOf(chipLocAll, chipLocMalang, chipLocBogor))
        }
        chipLocBogor.setOnClickListener {
            selectedLocation = "Bogor"
            setChipSelected(chipLocBogor, listOf(chipLocAll, chipLocMalang, chipLocJakarta))
        }

        // Slider Listener
        rangeSlider.addOnChangeListener { slider, _, _ ->
            minPrice = slider.values[0]
            maxPrice = slider.values[1]
            priceRangeValue.text = "${formatRupiah(minPrice)} - ${formatRupiah(maxPrice)}"
        }

        // Reset Listener
        btnReset.setOnClickListener {
            selectedSort = "LowToHigh"
            selectedGrade = "All"
            selectedLocation = "All"
            minPrice = 50000f
            maxPrice = 1000000f

            setChipSelected(chipSortLowToHigh, listOf(chipSortHighToLow))
            setChipSelected(chipGradeAll, listOf(chipGradeAPlus, chipGradeA))
            setChipSelected(chipLocAll, listOf(chipLocMalang, chipLocJakarta, chipLocBogor))

            rangeSlider.setValues(50000f, 1000000f)
            priceRangeValue.text = "${formatRupiah(50000f)} - ${formatRupiah(1000000f)}"
        }

        // Apply Listener
        btnApply.setOnClickListener {
            onFilterApplied?.invoke(selectedSort, selectedGrade, selectedLocation, minPrice, maxPrice)
            dismiss()
        }

        return view
    }

    private fun setChipSelected(selected: TextView, unselectedList: List<TextView>) {
        selected.setBackgroundResource(R.drawable.bg_chip_selected)
        selected.setTextColor(ContextCompat.getColor(requireContext(), R.color.guppy_primary))

        unselectedList.forEach { unselected ->
            unselected.setBackgroundResource(R.drawable.bg_chip_outline)
            unselected.setTextColor(ContextCompat.getColor(requireContext(), R.color.guppy_text_black))
        }
    }

    private fun formatRupiah(value: Float): String {
        val format = NumberFormat.getCurrencyInstance(Locale("in", "ID"))
        format.maximumFractionDigits = 0
        return format.format(value).replace("Rp", "Rp ").replace(",00", "")
    }
}
