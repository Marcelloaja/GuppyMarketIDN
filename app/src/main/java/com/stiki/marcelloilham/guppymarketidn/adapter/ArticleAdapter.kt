package com.stiki.marcelloilham.guppymarketidn.adapter

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.stiki.marcelloilham.guppymarketidn.R
import com.stiki.marcelloilham.guppymarketidn.menuProfile.WebViewActivity

class ArticleAdapter(
    private val context: Context,
    private var articleList: List<Article>
) : RecyclerView.Adapter<ArticleAdapter.ViewHolder>() {

    data class Article(
        val title: String,
        val category: String,
        val duration: String,
        val imageRes: Int,
        val categoryColorHex: String = "#225741"
    )

    private var filteredList: List<Article> = articleList

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imgThumb: ImageView = itemView.findViewById(R.id.imgArticleThumb)
        val tvCategory: TextView = itemView.findViewById(R.id.tvArticleCategory)
        val tvTitle: TextView = itemView.findViewById(R.id.tvArticleTitle)
        val tvDuration: TextView = itemView.findViewById(R.id.tvArticleDuration)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_article_card, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val article = filteredList[position]
        holder.imgThumb.setImageResource(article.imageRes)
        holder.tvCategory.text = article.category.uppercase()
        holder.tvCategory.setTextColor(Color.parseColor(article.categoryColorHex))
        holder.tvTitle.text = article.title
        holder.tvDuration.text = "🕒 ${article.duration}"

        holder.itemView.setOnClickListener {
            val intent = Intent(context, WebViewActivity::class.java).apply {
                putExtra("url", "https://drive.google.com/file/d/1AwopxNnAjbdJPYIPj43WU6jLmaz2ExpL/view?usp=drive_link")
            }
            context.startActivity(intent)
        }
    }

    override fun getItemCount(): Int = filteredList.size

    fun filterByQueryAndCategory(query: String, category: String) {
        filteredList = articleList.filter { item ->
            val matchesQuery = query.isEmpty() ||
                item.title.lowercase().contains(query.lowercase()) ||
                item.category.lowercase().contains(query.lowercase())

            val matchesCategory = category.equals("Semua", ignoreCase = true) ||
                item.category.lowercase().contains(category.lowercase())

            matchesQuery && matchesCategory
        }
        notifyDataSetChanged()
    }
}
