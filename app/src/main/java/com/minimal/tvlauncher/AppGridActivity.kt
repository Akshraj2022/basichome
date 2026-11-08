package com.minimal.tvlauncher

import android.app.Dialog
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.minimal.tvlauncher.adapter.AppGridAdapter
import com.minimal.tvlauncher.model.AppItem
import com.minimal.tvlauncher.util.AppManager
import com.minimal.tvlauncher.util.PreferenceManager

/**
 * Full application grid activity displaying all installed launchable apps in alphabetical order.
 * Supports pinning apps to any of the 5 categories (Entertainment, Media, Games, Tools, System).
 */
class AppGridActivity : AppCompatActivity() {

    private lateinit var prefManager: PreferenceManager
    private lateinit var adapter: AppGridAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var searchInput: EditText
    private lateinit var appCountText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_app_grid)

        prefManager = PreferenceManager(this)
        recyclerView = findViewById(R.id.recycler_app_grid)
        searchInput = findViewById(R.id.et_app_filter)
        appCountText = findViewById(R.id.tv_app_count)

        setupRecyclerView()
        setupSearch()
        loadApps()
    }

    private fun setupRecyclerView() {
        adapter = AppGridAdapter(
            context = this,
            onItemClick = { item ->
                prefManager.addRecentApp(item.packageName)
                AppManager.launchApp(this, item.packageName)
                finish()
            },
            onItemLongClick = { item ->
                showPinOptionsDialog(item)
            }
        )

        recyclerView.layoutManager = GridLayoutManager(this, 5)
        recyclerView.adapter = adapter
        recyclerView.setHasFixedSize(true)
    }

    private fun setupSearch() {
        searchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                adapter.filter(s?.toString() ?: "")
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        val initialFilter = intent.getStringExtra(EXTRA_INITIAL_FILTER)
        if (!initialFilter.isNullOrEmpty()) {
            searchInput.setText(initialFilter)
            searchInput.requestFocus()
        }
    }

    private fun loadApps() {
        val apps = AppManager.getAllLaunchableApps(this)
        adapter.submitList(apps)
        appCountText.text = "${apps.size} Apps"
    }

    private fun showPinOptionsDialog(item: AppItem) {
        val categories = arrayOf("Entertainment", "Media & Music", "Games", "Tools & VPN", "System")
        val builder = androidx.appcompat.app.AlertDialog.Builder(this, R.style.TvDialogTheme)
        builder.setTitle(item.label)
        builder.setItems(categories) { _, which ->
            pinToRow(item.packageName, which, categories[which])
        }
        builder.show()
    }

    private fun pinToRow(packageName: String, targetRow: Int, rowName: String) {
        val current = when (targetRow) {
            0 -> prefManager.getEntertainmentRow()
            1 -> prefManager.getMediaRow()
            2 -> prefManager.getGamesRow()
            3 -> prefManager.getToolsRow()
            else -> prefManager.getUtilityRow()
        }

        if (current.contains(packageName)) {
            Toast.makeText(this, "Already pinned in $rowName", Toast.LENGTH_SHORT).show()
            return
        }

        if (current.size >= 4) {
            current[3] = packageName
        } else {
            current.add(packageName)
        }

        when (targetRow) {
            0 -> prefManager.saveEntertainmentRow(current)
            1 -> prefManager.saveMediaRow(current)
            2 -> prefManager.saveGamesRow(current)
            3 -> prefManager.saveToolsRow(current)
            else -> prefManager.saveUtilityRow(current)
        }
        Toast.makeText(this, "Pinned to $rowName", Toast.LENGTH_SHORT).show()
    }

    companion object {
        const val EXTRA_INITIAL_FILTER = "initial_filter"
    }
}
