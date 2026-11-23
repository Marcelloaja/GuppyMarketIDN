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
    private val currentSelectedName: String = "",
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
            CategoryItem("Semua", R.drawable.skyblue, "Semua produk"),
            CategoryItem("Ikan Kontes", R.drawable.skyblue, "45 produk"),
            CategoryItem("Pakan Ikan", R.drawable.gsrts, "32 produk"),
            CategoryItem("Obat & Vitamin", R.drawable.platinumrd, "18 produk"),
            CategoryItem("Sol & Akrilik", R.drawable.mssrl, "24 produk"),
            CategoryItem("Indukan Breeder", R.drawable.skyblue, "29 produk"),
            CategoryItem("Aksesoris", R.drawable.gsrts, "15 produk"),
            CategoryItem("Tanaman Air", R.drawable.platinumrd, "12 produk"),
            CategoryItem("Filter & Pompa", R.drawable.mssrl, "20 produk")
        )

        val adapter = CategoryAdapter(requireContext(), categoriesList) { selected ->
            if (selected.name == "Semua") {
                onCategorySelected?.invoke("")
            } else {
                onCategorySelected?.invoke(selected.name)
            }
            dismiss()
        }

        adapter.setSelectedCategoryName(currentSelectedName)
        recycler.layoutManager = GridLayoutManager(requireContext(), 3)
        recycler.adapter = adapter

        return view
    }
}
