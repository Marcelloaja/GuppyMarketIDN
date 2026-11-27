package com.stiki.marcelloilham.guppymarketidn.menuHome

data class Item(
    val image: Int,
    val name: String,
    val type: String,
    val price: String,
    val id: String = "",
    val sellerId: String = "",
    val seller: String = "",
    val unit: String = "per pasang",
    val imageUrl: String = "",
    val photoDataUrl: String = "",
    val strain: String = "Semua",
    val location: String = "",
    val priceNumeric: Long = 0L,
    val stock: Int = 1,
    val description: String = ""
)
