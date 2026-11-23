package com.stiki.marcelloilham.guppymarketidn.menuHome

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.stiki.marcelloilham.guppymarketidn.R
import com.stiki.marcelloilham.guppymarketidn.adapter.CategoryAdapter

class BottomDialogCategories(
    private val onCategorySelected: ((String) -> Unit)? = null
) : BottomSheetDialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.bottom_sheet_categories, container, false)

        val btnClose = view.findViewById<ImageView>(R.id.btnCloseCategories)
        val recycler = view.findViewById<RecyclerView>(R.id.recyclerCategories)

        btnClose.setOnClickListener {
            dismiss()
        }

        val categoriesList = listOf(
            CategoryItem("Semua", R.drawable.skyblue, "Semua varian"),
            CategoryItem("Albino", R.drawable.skyblue, "32 varian"),
            CategoryItem("Solid", R.drawable.gsrts, "28 varian"),
            CategoryItem("Tuxedo", R.drawable.platinumrd, "24 varian"),
            CategoryItem("Grass", R.drawable.mssrl, "20 varian"),
            CategoryItem("Lace", R.drawable.skyblue, "18 varian"),
            CategoryItem("Cobra", R.drawable.gsrts, "16 varian"),
            CategoryItem("Mozaik", R.drawable.platinumrd, "22 varian"),
            CategoryItem("Metal Head", R.drawable.mssrl, "14 varian"),
            CategoryItem("Sword Tail", R.drawable.skyblue, "12 varian"),
            CategoryItem("Round Tail", R.drawable.gsrts, "15 varian"),
            CategoryItem("Halfmoon", R.drawable.platinumrd, "26 varian"),
            CategoryItem("Crown Tail", R.drawable.mssrl, "10 varian"),
            CategoryItem("Koi", R.drawable.skyblue, "19 varian"),
            CategoryItem("Big Ear", R.drawable.gsrts, "21 varian"),
            CategoryItem("Dumbo Ear", R.drawable.platinumrd, "25 varian"),
            CategoryItem("Short Body", R.drawable.mssrl, "11 varian"),
            CategoryItem("Female", R.drawable.skyblue, "30 varian"),
            CategoryItem("Grade A+", R.drawable.platinumrd, "40 varian")
        )

        val adapter = CategoryAdapter(requireContext(), categoriesList) { selected ->
            if (selected.name == "Semua") {
                onCategorySelected?.invoke("")
            } else {
                onCategorySelected?.invoke(selected.name)
            }
            dismiss()
        }

        recycler.layoutManager = GridLayoutManager(requireContext(), 3)
        recycler.adapter = adapter

        return view
    }
}
