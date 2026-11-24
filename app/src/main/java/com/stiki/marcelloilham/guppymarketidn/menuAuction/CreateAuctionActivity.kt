package com.stiki.marcelloilham.guppymarketidn.menuAuction

import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import com.stiki.marcelloilham.guppymarketidn.R
import com.stiki.marcelloilham.guppymarketidn.repository.FirestoreRepository

class CreateAuctionActivity : AppCompatActivity() {

    private lateinit var etTitle: EditText
    private lateinit var tvStrain: TextView
    private lateinit var tvGender: TextView
    private lateinit var etFishAge: EditText
    private lateinit var etFishQuantity: EditText
    private lateinit var etFishCondition: EditText
    private lateinit var etQuarantineDays: EditText

    private lateinit var etOpenBid: EditText
    private lateinit var etBidIncrement: EditText
    private lateinit var etBuyout: EditText
    private lateinit var etDescription: EditText

    private lateinit var chip12h: TextView
    private lateinit var chip24h: TextView
    private lateinit var chip48h: TextView

    private var selectedStrain: String = "Mosaic"
    private var selectedGender: String = "1 Pasang (Pair)"
    private var selectedDurationHours: Int = 24

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_auction)

        val topBar = findViewById<View>(R.id.topBarCreateAuction)
        topBar?.let {
            ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { _, insets ->
                val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                it.updatePadding(
                    top = systemBars.top + (8 * resources.displayMetrics.density).toInt()
                )
                insets
            }
        }

        val btnBack = findViewById<ImageView>(R.id.btnBackCreateAuction)
        btnBack.setOnClickListener { finish() }

        initViews()
        setupListeners()
    }

    private fun initViews() {
        etTitle = findViewById(R.id.etAuctionTitle)
        tvStrain = findViewById(R.id.tvSelectStrain)
        tvGender = findViewById(R.id.tvSelectGender)
        etFishAge = findViewById(R.id.etFishAge)
        etFishQuantity = findViewById(R.id.etFishQuantity)
        etFishCondition = findViewById(R.id.etFishCondition)
        etQuarantineDays = findViewById(R.id.etQuarantineDays)

        etOpenBid = findViewById(R.id.etOpenBidPrice)
        etBidIncrement = findViewById(R.id.etBidIncrementPrice)
        etBuyout = findViewById(R.id.etBuyoutPrice)
        etDescription = findViewById(R.id.etAuctionDescription)

        chip12h = findViewById(R.id.chipDuration12)
        chip24h = findViewById(R.id.chipDuration24)
        chip48h = findViewById(R.id.chipDuration48)
    }

    private fun setupListeners() {
        val cardVideo = findViewById<View>(R.id.cardUploadVideo)
        val cardPhoto1 = findViewById<View>(R.id.cardUploadPhoto1)
        val cardPhoto2 = findViewById<View>(R.id.cardUploadPhoto2)
        val cardPhoto3 = findViewById<View>(R.id.cardUploadPhoto3)

        val mediaUploadPrompt = { slotName: String ->
            Toast.makeText(this, "Membuka galeri untuk upload $slotName...", Toast.LENGTH_SHORT).show()
        }

        cardVideo?.setOnClickListener { mediaUploadPrompt("1 Video Pendek") }
        cardPhoto1?.setOnClickListener { mediaUploadPrompt("Foto Utama 1") }
        cardPhoto2?.setOnClickListener { mediaUploadPrompt("Foto 2") }
        cardPhoto3?.setOnClickListener { mediaUploadPrompt("Foto 3") }

        val strainOptions = arrayOf("Mosaic", "Albino", "Tuxedo", "Dragon", "Moscow", "Grass", "Koi", "Snakeskin")
        tvStrain.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Pilih Galur Varian")
                .setItems(strainOptions) { _, which ->
                    selectedStrain = strainOptions[which]
                    tvStrain.text = "Galur: $selectedStrain ∨"
                }
                .show()
        }

        val genderOptions = arrayOf("1 Pasang (Pair)", "Jantan Single", "Betina Single", "Koloni (3 Ekor)")
        tvGender.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Pilih Jenis Kelamin")
                .setItems(genderOptions) { _, which ->
                    selectedGender = genderOptions[which]
                    tvGender.text = selectedGender
                }
                .show()
        }

        chip12h.setOnClickListener { selectDuration(chip12h, 12) }
        chip24h.setOnClickListener { selectDuration(chip24h, 24) }
        chip48h.setOnClickListener { selectDuration(chip48h, 48) }

        val btnPublishTop = findViewById<TextView>(R.id.btnPublishTop)
        val btnPublishBottom = findViewById<TextView>(R.id.btnPublishBottom)

        val publishAction = {
            publishAuction()
        }

        btnPublishTop?.setOnClickListener { publishAction() }
        btnPublishBottom?.setOnClickListener { publishAction() }
    }

    private fun selectDuration(selected: TextView, hours: Int) {
        selectedDurationHours = hours
        val chips = listOf(chip12h, chip24h, chip48h)

        chips.forEach { chip ->
            if (chip == selected) {
                chip.setBackgroundResource(R.drawable.bg_chip_selected)
                chip.setTextColor(ContextCompat.getColor(this, R.color.guppy_primary))
                chip.typeface = androidx.core.content.res.ResourcesCompat.getFont(this, R.font.inter_bold)
            } else {
                chip.setBackgroundResource(R.drawable.bg_chip_outline)
                chip.setTextColor(ContextCompat.getColor(this, R.color.guppy_text_black))
                chip.typeface = androidx.core.content.res.ResourcesCompat.getFont(this, R.font.inter_medium)
            }
        }
    }

    private fun publishAuction() {
        val titleText = etTitle.text.toString().trim()
        val obText = etOpenBid.text.toString().trim()

        if (titleText.isEmpty()) {
            Toast.makeText(this, "Harap masukkan judul lelang", Toast.LENGTH_SHORT).show()
            return
        }

        if (obText.isEmpty()) {
            Toast.makeText(this, "Harap masukkan harga Open Bid (OB)", Toast.LENGTH_SHORT).show()
            return
        }

        val obAmount = obText.toLongOrNull() ?: 200000L
        val formattedPrice = "Rp ${java.text.NumberFormat.getInstance(java.util.Locale("in", "ID")).format(obAmount)}"

        val endTimeMillis = System.currentTimeMillis() + (selectedDurationHours * 3600000L)

        val newAuction = FirestoreRepository.AuctionModel(
            id = "auction_${System.currentTimeMillis()}",
            title = titleText,
            seller = "Toko Saya",
            location = "Bekasi",
            currentBid = formattedPrice,
            currentBidNumeric = obAmount,
            bidsCount = 0,
            endTimeMillis = endTimeMillis,
            imageRes = R.drawable.bg_image_grid,
            grade = "Grade A+"
        )

        Toast.makeText(this, "Sesi Lelang '$titleText' ($selectedGender, $selectedStrain) Berhasil Diterbitkan!", Toast.LENGTH_LONG).show()
        finish()
    }
}
