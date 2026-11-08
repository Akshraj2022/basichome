package com.minimal.tvlauncher.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.minimal.tvlauncher.R
import com.minimal.tvlauncher.model.AppItem
import com.minimal.tvlauncher.util.IconCache
import com.minimal.tvlauncher.view.TvTileView
import java.util.Locale

/**
 * Adapter for the full application grid screen with curved Play Store icons.
 */
class AppGridAdapter(
    private val context: Context,
    private val onItemClick: (AppItem) -> Unit,
    private val onItemLongClick: (AppItem) -> Unit
) : RecyclerView.Adapter<AppGridAdapter.AppViewHolder>() {

    private val allItems = mutableListOf<AppItem>()
    private val filteredItems = mutableListOf<AppItem>()

    fun submitList(items: List<AppItem>) {
        allItems.clear()
        allItems.addAll(items)
        filteredItems.clear()
        filteredItems.addAll(items)
        notifyDataSetChanged()
    }

    fun filter(query: String) {
        val cleanQuery = query.trim().lowercase(Locale.getDefault())
        filteredItems.clear()
        if (cleanQuery.isEmpty()) {
            filteredItems.addAll(allItems)
        } else {
            for (item in allItems) {
                if (item.label.lowercase(Locale.getDefault()).contains(cleanQuery) ||
                    item.packageName.lowercase(Locale.getDefault()).contains(cleanQuery)
                ) {
                    filteredItems.add(item)
                }
            }
        }
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_app_grid, parent, false)
        return AppViewHolder(view)
    }

    override fun onBindViewHolder(holder: AppViewHolder, position: Int) {
        val item = filteredItems[position]
        holder.bind(item)
    }

    override fun getItemCount(): Int = filteredItems.size

    inner class AppViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tileView: TvTileView = itemView.findViewById(R.id.grid_tile_view)

        fun bind(item: AppItem) {
            val icon = IconCache.getIcon(context, item.packageName, item.label) { brandDrawable ->
                tileView.setTileData(brandDrawable, item.label)
            }
            tileView.setTileData(icon, item.label)

            tileView.setOnClickListener {
                onItemClick(item)
            }

            tileView.onTileLongClickListener = {
                onItemLongClick(item)
            }
        }
    }
}
