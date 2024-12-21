package com.example.healthcast.adapter


import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.healthcast.R
import com.example.healthcast.model.Story
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit

class RecyclerAdapterStory(var dataList: MutableList<Story>, var ctx: Context) :
    RecyclerView.Adapter<RecyclerAdapterStory.ViewHolder>(), Filterable {
    var dataListAll: List<Story> = ArrayList(dataList)
    private lateinit var firebaseAuth: FirebaseAuth
    private var onItemClickCallback: OnItemClickCallback? = null

    fun setOnclickCallback(onItemClickCallback: OnItemClickCallback?) {
        this.onItemClickCallback = onItemClickCallback
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val view = layoutInflater.inflate(R.layout.row_item_post, parent, false)
        val viewHolder: ViewHolder = ViewHolder(view)
        return viewHolder
    }

    @SuppressLint("SetTextI18n", "UseCompatLoadingForColorStateLists")
    override fun onBindViewHolder(holder: ViewHolder, @SuppressLint("RecyclerView") position: Int) {
        val item = dataList[position]
        holder.nama.text = item.user.fullName
        holder.deskripsi.text = item.description
        holder.tanggal.text = getRelativeTime(item.created_at)

        Glide.with(ctx).load(item.image_url).into(holder.image)

        val uuid = FirebaseAuth.getInstance().currentUser!!.uid

        if (item.likes.contains(uuid)) {
            holder.btnLike.imageTintList = ColorStateList.valueOf(ctx.resources.getColor(R.color.color_primary));
        }else {
            holder.btnLike.imageTintList = ColorStateList.valueOf(ctx.resources.getColor(R.color.black));
        }

        if (item.dislikes.contains(uuid)) {
            holder.btnUnlike.imageTintList = ColorStateList.valueOf(ctx.resources.getColor(R.color.color_primary));
        }else {
            holder.btnUnlike.imageTintList = ColorStateList.valueOf(ctx.resources.getColor(R.color.black));
        }

        if (uuid == item.user.id) {
            holder.btnDelete.visibility = View.VISIBLE
            holder.btnEdit.visibility = View.VISIBLE
        } else {
            holder.btnDelete.visibility = View.GONE
            holder.btnEdit.visibility = View.GONE
        }

        holder.btnLike.setOnClickListener {
            onItemClickCallback!!.onItemLike(
                dataList[position], position
            )
        }

        holder.btnUnlike.setOnClickListener {
            onItemClickCallback!!.onItemUnlike(
                dataList[position], position
            )
        }

        holder.btnKomentar.setOnClickListener {
            onItemClickCallback!!.onItemComment(
                dataList[position], position
            )
        }

        holder.allView.setOnClickListener {
            onItemClickCallback!!.onItemComment(
                dataList[position], position
            )
        }

        holder.btnEdit.setOnClickListener {
            onItemClickCallback!!.onItemClick(
                dataList[position], position
            )
        }
        holder.btnDelete.setOnClickListener {
            onItemClickCallback!!.onItemDelete(
                dataList[position],
                position
            )
        }

    }

    override fun getItemCount(): Int {
        return dataList.size
    }

    override fun getFilter(): Filter {
        return customFilter
    }

    var customFilter: Filter = object : Filter() {
        override fun performFiltering(charSequence: CharSequence): FilterResults {
            val filteredList: MutableList<Story> = ArrayList()
            if (charSequence.isEmpty() || charSequence == null) {
                filteredList.addAll(dataListAll)
            } else {
                for (item in dataListAll) {
                    if (item.description.lowercase(Locale.getDefault()).contains(
                            charSequence.toString().lowercase(
                                Locale.getDefault()
                            )
                        )
                    ) {
                        filteredList.add(item)
                    }
                }
            }

            val filterResults = FilterResults()
            filterResults.values = filteredList
            return filterResults
        }

        override fun publishResults(constraint: CharSequence, results: FilterResults) {
            dataList.clear()
            dataList.addAll(results.values as List<Story>)
            notifyDataSetChanged()
        }
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var image: ImageView = itemView.findViewById(R.id.imgv_image)
        var nama: TextView = itemView.findViewById(R.id.tv_nama)
        var deskripsi: TextView = itemView.findViewById(R.id.tv_dekripsi)
        var tanggal: TextView = itemView.findViewById(R.id.tv_date)
        var btnLike: ImageView = itemView.findViewById(R.id.btn_like)
        var btnUnlike: ImageView = itemView.findViewById(R.id.btn_unlike)
        var btnKomentar: ImageView = itemView.findViewById(R.id.btn_comment)
        var btnEdit: ImageView = itemView.findViewById(R.id.btn_edit)
        var btnDelete: ImageView = itemView.findViewById(R.id.btn_delete)
        var allView: LinearLayout = itemView.findViewById(R.id.item_view)
    }

    interface OnItemClickCallback {
        fun onItemClick(item: Story?, position: Int)
        fun onItemDelete(item: Story?, position: Int)
        fun onItemComment(item: Story?, position: Int)
        fun onItemLike(item: Story?, position: Int)
        fun onItemUnlike(item: Story?, position: Int)
    }

    @SuppressLint("SimpleDateFormat")
    fun getRelativeTime(createdAt: String): String {
        try {
            // Parse the date
            val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
            sdf.timeZone = TimeZone.getTimeZone("UTC")
            val postDate = sdf.parse(createdAt) ?: return "Invalid date"

            // Get current date
            val currentDate = Date()

            // Calculate the time difference in milliseconds
            val diff = currentDate.time - postDate.time

            // Convert the time difference to seconds, minutes, hours, days, etc.
            val seconds = TimeUnit.MILLISECONDS.toSeconds(diff)
            val minutes = TimeUnit.MILLISECONDS.toMinutes(diff)
            val hours = TimeUnit.MILLISECONDS.toHours(diff)
            val days = TimeUnit.MILLISECONDS.toDays(diff)

            // Return a human-readable relative time
            return when {
                seconds < 60 -> "$seconds second${if (seconds == 1L) "" else "s"} ago"
                minutes < 60 -> "$minutes minute${if (minutes == 1L) "" else "s"} ago"
                hours < 24 -> "$hours hour${if (hours == 1L) "" else "s"} ago"
                days < 7 -> "$days day${if (days == 1L) "" else "s"} ago"
                days < 30 -> "${days / 7} week${if (days / 7 == 1L) "" else "s"} ago"
                days < 365 -> "${days / 30} month${if (days / 30 == 1L) "" else "s"} ago"
                else -> "${days / 365} year${if (days / 365 == 1L) "" else "s"} ago"
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return "Invalid date"
        }
    }
}
