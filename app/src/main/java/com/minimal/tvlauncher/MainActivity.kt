package com.minimal.tvlauncher

import android.app.Dialog
import android.content.Intent
import android.graphics.Rect
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.minimal.tvlauncher.model.TvInputItem
import com.minimal.tvlauncher.receiver.PackageChangeReceiver
import com.minimal.tvlauncher.receiver.TimeTickReceiver
import com.minimal.tvlauncher.util.AppManager
import com.minimal.tvlauncher.util.IconCache
import com.minimal.tvlauncher.util.PreferenceManager
import com.minimal.tvlauncher.util.SunTimeHelper
import com.minimal.tvlauncher.util.TvInputHelper
import com.minimal.tvlauncher.view.TvTileView

/**
 * Main Android TV Leanback Launcher Activity.
 * Supports 7 dedicated organized categories:
 * Recent, Entertainment, Media & Music, Games, Tools & VPN, System, and Inputs.
 * Features official curved squircle Play Store icons, automatic day/night solar themes,
 * smooth auto-centering D-pad scrolling, and hardware acceleration.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var prefManager: PreferenceManager
    private lateinit var timeTickReceiver: TimeTickReceiver
    private lateinit var packageChangeReceiver: PackageChangeReceiver

    private lateinit var mainRoot: ViewGroup
    private lateinit var contentScrollView: ScrollView
    private lateinit var topPill: View
    private lateinit var searchPill: View
    private lateinit var tvClock: TextView
    private lateinit var tvSearchPlaceholder: TextView
    private lateinit var btnHome: ImageButton
    private lateinit var btnAllApps: ImageButton

    private lateinit var headingRecent: TextView
    private lateinit var headingEntertainment: TextView
    private lateinit var headingMedia: TextView
    private lateinit var headingGames: TextView
    private lateinit var headingTools: TextView
    private lateinit var headingUtility: TextView
    private lateinit var headingInputs: TextView

    private lateinit var containerRecent: View
    private lateinit var containerEntertainment: View
    private lateinit var containerMedia: View
    private lateinit var containerGames: View
    private lateinit var containerTools: View
    private lateinit var containerUtility: View
    private lateinit var containerInputs: View

    private lateinit var recentTiles: List<TvTileView>
    private lateinit var entertainmentTiles: List<TvTileView>
    private lateinit var mediaTiles: List<TvTileView>
    private lateinit var gamesTiles: List<TvTileView>
    private lateinit var toolsTiles: List<TvTileView>
    private lateinit var utilityTiles: List<TvTileView>
    private lateinit var inputsTiles: List<TvTileView>

    private var currentInputs: List<TvInputItem> = emptyList()
    private var isCurrentlyLightMode: Boolean? = null
    private var lastFocusedViewId: Int = R.id.recent_tile_0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        prefManager = PreferenceManager(this)
        AppManager.initDefaultRowsIfFirstRun(this)

        bindViews()
        setupTopBar()
        setupDpadNavigation()
        setupClockReceiver()
        setupPackageChangeReceiver()
        applyDayNightTheme(force = true)
    }

    override fun onResume() {
        super.onResume()
        applyDayNightTheme(force = false)
        refreshAllRows()

        val targetFocus = findViewById<View>(lastFocusedViewId) ?: recentTiles.firstOrNull()
        targetFocus?.post {
            targetFocus.requestFocus()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        timeTickReceiver.unregister(this)
        packageChangeReceiver.unregister(this)
        IconCache.clear()
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        // Prevent exiting launcher on Back button
    }

    private fun bindViews() {
        mainRoot = findViewById(R.id.main_root)
        contentScrollView = findViewById(R.id.content_scroll_view)
        topPill = findViewById(R.id.top_left_pill)
        searchPill = findViewById(R.id.btn_search)
        tvClock = findViewById(R.id.tv_clock)
        tvSearchPlaceholder = findViewById(R.id.tv_search_placeholder)
        btnHome = findViewById(R.id.btn_home)
        btnAllApps = findViewById(R.id.btn_all_apps)

        headingRecent = findViewById(R.id.heading_recent)
        headingEntertainment = findViewById(R.id.heading_entertainment)
        headingMedia = findViewById(R.id.heading_media)
        headingGames = findViewById(R.id.heading_games)
        headingTools = findViewById(R.id.heading_tools)
        headingUtility = findViewById(R.id.heading_utility)
        headingInputs = findViewById(R.id.heading_inputs)

        containerRecent = findViewById(R.id.container_recent_row)
        containerEntertainment = findViewById(R.id.container_entertainment_row)
        containerMedia = findViewById(R.id.container_media_row)
        containerGames = findViewById(R.id.container_games_row)
        containerTools = findViewById(R.id.container_tools_row)
        containerUtility = findViewById(R.id.container_utility_row)
        containerInputs = findViewById(R.id.container_inputs_row)

        recentTiles = listOf(
            findViewById(R.id.recent_tile_0),
            findViewById(R.id.recent_tile_1),
            findViewById(R.id.recent_tile_2),
            findViewById(R.id.recent_tile_3),
            findViewById(R.id.recent_tile_more)
        )

        entertainmentTiles = listOf(
            findViewById(R.id.entertainment_tile_0),
            findViewById(R.id.entertainment_tile_1),
            findViewById(R.id.entertainment_tile_2),
            findViewById(R.id.entertainment_tile_3),
            findViewById(R.id.entertainment_tile_more)
        )

        mediaTiles = listOf(
            findViewById(R.id.media_tile_0),
            findViewById(R.id.media_tile_1),
            findViewById(R.id.media_tile_2),
            findViewById(R.id.media_tile_3),
            findViewById(R.id.media_tile_more)
        )

        gamesTiles = listOf(
            findViewById(R.id.games_tile_0),
            findViewById(R.id.games_tile_1),
            findViewById(R.id.games_tile_2),
            findViewById(R.id.games_tile_3),
            findViewById(R.id.games_tile_more)
        )

        toolsTiles = listOf(
            findViewById(R.id.tools_tile_0),
            findViewById(R.id.tools_tile_1),
            findViewById(R.id.tools_tile_2),
            findViewById(R.id.tools_tile_3),
            findViewById(R.id.tools_tile_more)
        )

        utilityTiles = listOf(
            findViewById(R.id.utility_tile_0),
            findViewById(R.id.utility_tile_1),
            findViewById(R.id.utility_tile_2),
            findViewById(R.id.utility_tile_3),
            findViewById(R.id.utility_tile_more)
        )

        inputsTiles = listOf(
            findViewById(R.id.inputs_tile_0),
            findViewById(R.id.inputs_tile_1),
            findViewById(R.id.inputs_tile_2),
            findViewById(R.id.inputs_tile_3),
            findViewById(R.id.inputs_tile_4)
        )

        val allFocusables = listOf(btnHome, btnAllApps, searchPill) +
                recentTiles + entertainmentTiles + mediaTiles + gamesTiles + toolsTiles + utilityTiles + inputsTiles

        for (view in allFocusables) {
            view.setOnFocusChangeListener { v, hasFocus ->
                if (hasFocus) {
                    lastFocusedViewId = v.id
                    if (v in recentTiles || v == btnHome || v == btnAllApps || v == searchPill) {
                        contentScrollView.smoothScrollTo(0, 0)
                    } else {
                        // Smooth auto-centering on focused row
                        val parentContainer = (v.parent as? View)
                        if (parentContainer != null) {
                            contentScrollView.requestChildRectangleOnScreen(
                                parentContainer,
                                Rect(0, -60, parentContainer.width, parentContainer.height + 40),
                                false
                            )
                        }
                    }
                }
            }
        }
    }

    private fun setupTopBar() {
        btnHome.setOnClickListener {
            contentScrollView.smoothScrollTo(0, 0)
            recentTiles.firstOrNull()?.requestFocus()
        }

        btnAllApps.setOnClickListener {
            openAppGrid()
        }

        searchPill.setOnClickListener {
            val intent = Intent(this, AppGridActivity::class.java).apply {
                putExtra(AppGridActivity.EXTRA_INITIAL_FILTER, "")
            }
            startActivity(intent)
        }
    }

    private fun setupDpadNavigation() {
        val rows = listOf(
            recentTiles,
            entertainmentTiles,
            mediaTiles,
            gamesTiles,
            toolsTiles,
            utilityTiles,
            inputsTiles
        )

        for (rowIndex in rows.indices) {
            val currentRow = rows[rowIndex].filter { it.visibility == View.VISIBLE }
            if (currentRow.isEmpty()) continue
            val isFirstRow = rowIndex == 0
            val isLastRow = rowIndex == rows.size - 1

            for (colIndex in currentRow.indices) {
                val tile = currentRow[colIndex]

                // Up navigation
                tile.nextFocusUpId = if (isFirstRow) {
                    if (colIndex <= 1) R.id.btn_all_apps else R.id.btn_search
                } else {
                    val prevRow = rows[rowIndex - 1].filter { it.visibility == View.VISIBLE }
                    if (prevRow.isNotEmpty()) {
                        prevRow[colIndex.coerceAtMost(prevRow.size - 1)].id
                    } else tile.id
                }

                // Down navigation (Hard rule: last row stays put)
                tile.nextFocusDownId = if (isLastRow) {
                    tile.id
                } else {
                    val nextRow = rows[rowIndex + 1].filter { it.visibility == View.VISIBLE }
                    if (nextRow.isNotEmpty()) {
                        nextRow[colIndex.coerceAtMost(nextRow.size - 1)].id
                    } else tile.id
                }

                // Left navigation (No wrap at start)
                tile.nextFocusLeftId = if (colIndex > 0) {
                    currentRow[colIndex - 1].id
                } else {
                    currentRow[0].id
                }

                // Right navigation (No wrap at end)
                tile.nextFocusRightId = if (colIndex < currentRow.size - 1) {
                    currentRow[colIndex + 1].id
                } else {
                    currentRow.last().id
                }
            }
        }

        // Top bar D-pad routing
        val firstRecent = recentTiles.firstOrNull { it.visibility == View.VISIBLE } ?: recentTiles[0]
        btnHome.nextFocusLeftId = R.id.btn_home
        btnHome.nextFocusRightId = R.id.btn_all_apps
        btnHome.nextFocusUpId = R.id.btn_home
        btnHome.nextFocusDownId = firstRecent.id

        btnAllApps.nextFocusLeftId = R.id.btn_home
        btnAllApps.nextFocusRightId = R.id.btn_search
        btnAllApps.nextFocusUpId = R.id.btn_all_apps
        btnAllApps.nextFocusDownId = firstRecent.id

        val searchDownTarget = recentTiles.filter { it.visibility == View.VISIBLE }.let {
            if (it.size > 2) it[2] else it.firstOrNull() ?: recentTiles[0]
        }
        searchPill.nextFocusLeftId = R.id.btn_all_apps
        searchPill.nextFocusRightId = R.id.btn_search
        searchPill.nextFocusUpId = R.id.btn_search
        searchPill.nextFocusDownId = searchDownTarget.id

        // Explicit key handlers for infallible transition between top bar and content rows
        btnHome.setOnKeyListener { _, keyCode, event ->
            if (event.action == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
                recentTiles.firstOrNull { it.visibility == View.VISIBLE }?.requestFocus()
                true
            } else false
        }

        btnAllApps.setOnKeyListener { _, keyCode, event ->
            if (event.action == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
                recentTiles.firstOrNull { it.visibility == View.VISIBLE }?.requestFocus()
                true
            } else false
        }

        searchPill.setOnKeyListener { _, keyCode, event ->
            if (event.action == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
                val visible = recentTiles.filter { it.visibility == View.VISIBLE }
                if (visible.size > 2) visible[2].requestFocus()
                else visible.firstOrNull()?.requestFocus()
                true
            } else false
        }

        for (i in recentTiles.indices) {
            val tile = recentTiles[i]
            tile.setOnKeyListener { _, keyCode, event ->
                if (event.action == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_DPAD_UP) {
                    if (i <= 1) btnAllApps.requestFocus()
                    else searchPill.requestFocus()
                    true
                } else false
            }
        }
    }

    private fun applyDayNightTheme(force: Boolean = false) {
        val isDay = SunTimeHelper.isDaytime()
        if (!force && isCurrentlyLightMode == isDay) return
        isCurrentlyLightMode = isDay

        val containers = listOf(
            containerRecent,
            containerEntertainment,
            containerMedia,
            containerGames,
            containerTools,
            containerUtility,
            containerInputs
        )

        val headings = listOf(
            headingRecent,
            headingEntertainment,
            headingMedia,
            headingGames,
            headingTools,
            headingUtility,
            headingInputs
        )

        // The upper bar permanently remains in sleek black and gray shades
        topPill.setBackgroundResource(R.drawable.bg_top_pill)
        searchPill.setBackgroundResource(R.drawable.bg_search_pill)
        tvClock.setTextColor(ContextCompat.getColor(this, R.color.clock_text))
        tvSearchPlaceholder.setTextColor(ContextCompat.getColor(this, R.color.search_text_hint))

        if (isDay) {
            // Light Theme
            mainRoot.setBackgroundColor(ContextCompat.getColor(this, R.color.canvas_background_light))
            val headingColor = ContextCompat.getColor(this, R.color.heading_text_light)
            for (h in headings) {
                h.setTextColor(headingColor)
            }
            for (c in containers) {
                c.setBackgroundResource(R.drawable.bg_row_container_light)
            }
        } else {
            // Dark Theme
            mainRoot.setBackgroundColor(ContextCompat.getColor(this, R.color.canvas_background))
            val headingColor = ContextCompat.getColor(this, R.color.heading_text)
            for (h in headings) {
                h.setTextColor(headingColor)
            }
            for (c in containers) {
                c.setBackgroundResource(R.drawable.bg_row_container)
            }
        }

        val allTiles = recentTiles + entertainmentTiles + mediaTiles + gamesTiles + toolsTiles + utilityTiles + inputsTiles
        for (tile in allTiles) {
            tile.updateTheme(isDay)
        }
    }

    private fun refreshAllRows() {
        AppManager.ensureAllCategoriesPopulated(this, prefManager)

        val allApps = AppManager.getAllLaunchableApps(this).map { it.packageName }
        val recentList = prefManager.getRecentApps().toMutableList()
        val entertainmentList = prefManager.getEntertainmentRow()
        val mediaList = prefManager.getMediaRow()
        val gamesList = prefManager.getGamesRow()
        val toolsList = prefManager.getToolsRow()
        val utilityList = prefManager.getUtilityRow()

        if (recentList.size < 4) {
            for (pkg in entertainmentList + mediaList + allApps) {
                if (!recentList.contains(pkg)) {
                    recentList.add(pkg)
                    if (recentList.size == 4) break
                }
            }
        }
        val effectiveRecent = recentList

        populateAppRow(recentTiles, effectiveRecent, Category.RECENT)
        populateAppRow(entertainmentTiles, entertainmentList, Category.ENTERTAINMENT)
        populateAppRow(mediaTiles, mediaList, Category.MEDIA)
        populateAppRow(gamesTiles, gamesList, Category.GAMES)
        populateAppRow(toolsTiles, toolsList, Category.TOOLS)
        populateAppRow(utilityTiles, utilityList, Category.UTILITY)
        setupInputsRow()
        setupDpadNavigation()
    }

    private enum class Category { RECENT, ENTERTAINMENT, MEDIA, GAMES, TOOLS, UTILITY }

    private fun populateAppRow(tiles: List<TvTileView>, packages: List<String>, category: Category) {
        for (i in 0..3) {
            val tile = tiles[i]
            if (i < packages.size) {
                val pkg = packages[i]
                val appItem = AppManager.getAppItem(this, pkg)
                val label = appItem?.label ?: pkg
                val icon = IconCache.getIcon(this, pkg, label) { brandDrawable ->
                    tile.setTileData(brandDrawable, label)
                }
                tile.visibility = View.VISIBLE
                tile.setTileData(icon, label)
                tile.setOnClickListener {
                    prefManager.addRecentApp(pkg)
                    AppManager.launchApp(this, pkg)
                }
                tile.onTileLongClickListener = {
                    showTileOptionsDialog(pkg, i, category)
                }
            } else {
                tile.visibility = View.GONE
                tile.onTileLongClickListener = null
            }
        }

        // 5th Tile: "..." More Tile
        val moreTile = tiles[4]
        moreTile.visibility = View.VISIBLE
        moreTile.setTileData(null, getString(R.string.more_apps))
        moreTile.setTileIcon(R.drawable.ic_more)
        moreTile.setOnClickListener {
            openAppGrid()
        }
    }

    private fun setupInputsRow() {
        currentInputs = TvInputHelper.getTvInputs(this)
        for (i in inputsTiles.indices) {
            val tile = inputsTiles[i]
            if (i < currentInputs.size) {
                val input = currentInputs[i]
                tile.visibility = View.VISIBLE
                tile.setTileLabel(input.label)
                if (input.isHdmi) {
                    tile.setTileIcon(R.drawable.ic_hdmi)
                } else {
                    tile.setTileIcon(R.drawable.ic_av)
                }
                tile.setOnClickListener {
                    val launched = TvInputHelper.launchInput(this, input)
                    if (!launched) {
                        Toast.makeText(this, "Opening ${input.label}...", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                tile.visibility = View.INVISIBLE
            }
        }
    }

    private fun showTileOptionsDialog(pkg: String, position: Int, category: Category) {
        val dialog = Dialog(this, R.style.TvDialogTheme)
        dialog.setContentView(R.layout.dialog_tile_options)

        val appItem = AppManager.getAppItem(this, pkg)
        val title = dialog.findViewById<TextView>(R.id.dialog_title)
        val btnMoveLeft = dialog.findViewById<Button>(R.id.btn_move_left)
        val btnMoveRight = dialog.findViewById<Button>(R.id.btn_move_right)
        val btnRemove = dialog.findViewById<Button>(R.id.btn_remove)

        title.text = appItem?.label ?: pkg
        btnMoveLeft.isEnabled = position > 0
        btnMoveRight.isEnabled = position < 3

        btnMoveLeft.setOnClickListener {
            moveTile(position, position - 1, category)
            dialog.dismiss()
        }

        btnMoveRight.setOnClickListener {
            moveTile(position, position + 1, category)
            dialog.dismiss()
        }

        btnRemove.setOnClickListener {
            removeTile(position, category)
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun moveTile(fromIndex: Int, toIndex: Int, category: Category) {
        val list = when (category) {
            Category.RECENT -> prefManager.getRecentApps()
            Category.ENTERTAINMENT -> prefManager.getEntertainmentRow()
            Category.MEDIA -> prefManager.getMediaRow()
            Category.GAMES -> prefManager.getGamesRow()
            Category.TOOLS -> prefManager.getToolsRow()
            Category.UTILITY -> prefManager.getUtilityRow()
        }
        if (fromIndex in list.indices && toIndex in list.indices) {
            val temp = list[fromIndex]
            list[fromIndex] = list[toIndex]
            list[toIndex] = temp
            saveCategoryList(category, list)
            refreshAllRows()
        }
    }

    private fun removeTile(index: Int, category: Category) {
        val list = when (category) {
            Category.RECENT -> prefManager.getRecentApps()
            Category.ENTERTAINMENT -> prefManager.getEntertainmentRow()
            Category.MEDIA -> prefManager.getMediaRow()
            Category.GAMES -> prefManager.getGamesRow()
            Category.TOOLS -> prefManager.getToolsRow()
            Category.UTILITY -> prefManager.getUtilityRow()
        }
        if (index in list.indices) {
            list.removeAt(index)
            saveCategoryList(category, list)
            refreshAllRows()
        }
    }

    private fun saveCategoryList(category: Category, list: List<String>) {
        when (category) {
            Category.RECENT -> {
                // Recent is managed by prefManager.addRecentApp
            }
            Category.ENTERTAINMENT -> prefManager.saveEntertainmentRow(list)
            Category.MEDIA -> prefManager.saveMediaRow(list)
            Category.GAMES -> prefManager.saveGamesRow(list)
            Category.TOOLS -> prefManager.saveToolsRow(list)
            Category.UTILITY -> prefManager.saveUtilityRow(list)
        }
    }

    private fun openAppGrid() {
        startActivity(Intent(this, AppGridActivity::class.java))
    }

    private fun setupClockReceiver() {
        timeTickReceiver = TimeTickReceiver { timeString ->
            tvClock.text = timeString
            applyDayNightTheme(force = false)
        }
        timeTickReceiver.register(this)
    }

    private fun setupPackageChangeReceiver() {
        packageChangeReceiver = PackageChangeReceiver {
            IconCache.clear()
            refreshAllRows()
        }
        packageChangeReceiver.register(this)
    }
}
