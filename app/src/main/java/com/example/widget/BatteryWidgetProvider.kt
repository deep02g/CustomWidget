package com.example.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.BatteryManager
import android.os.Bundle
import kotlin.math.max
import android.widget.RemoteViews

class BatteryWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, mgr: AppWidgetManager, ids: IntArray) {
        ids.forEach { updateAppWidget(context, mgr, it) }
    }

    companion object {
        private const val ACTION_REFRESH = "com.example.widget.ACTION_REFRESH"

        fun updateAppWidget(context: Context, mgr: AppWidgetManager, id: Int) {
            val bm = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
            val percent = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)

            val layoutRes = pickLayoutForId(mgr, id)
            val views = RemoteViews(context.packageName, layoutRes)
            views.setTextViewText(R.id.tvPercent, "$percent%")

            val (cellsW, cellsH) = getCellSize(mgr, id)
            views.setTextViewText(R.id.tvSize, "${cellsW}x${cellsH}")

            val intent = Intent(context, BatteryWidgetProvider::class.java).apply { action = ACTION_REFRESH }
            val pi = PendingIntent.getBroadcast(
                context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.tvPercent, pi)

            mgr.updateAppWidget(id, views)
        }

        private fun pickLayoutForId(mgr: AppWidgetManager, id: Int): Int {
            val (minW, minH) = getMinDp(mgr, id)

            // Thresholds approximate cell counts using 70dp per cell minus padding.
            // Large ~ 4x2 or bigger, Medium ~ >= 2x2, Small ~ narrower/shorter.
            return when {
                minW >= 250 || minH >= 180 -> R.layout.widget_battery_large
                minW >= 110 && minH >= 110 -> R.layout.widget_battery_medium
                else -> R.layout.widget_battery_small
            }
        }

        private fun getMinDp(mgr: AppWidgetManager, id: Int): Pair<Int, Int> {
            val opts: Bundle = mgr.getAppWidgetOptions(id) ?: Bundle.EMPTY
            val minW = opts.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 110)
            val minH = opts.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 110)
            return minW to minH
        }

        private fun getCellSize(mgr: AppWidgetManager, id: Int): Pair<Int, Int> {
            val (minW, minH) = getMinDp(mgr, id)
            val cellsW = dpToCells(minW)
            val cellsH = dpToCells(minH)
            return cellsW to cellsH
        }

        private fun dpToCells(dp: Int): Int = max(1, (dp + 30) / 70)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_REFRESH) {
            val mgr = AppWidgetManager.getInstance(context)
            val ids = mgr.getAppWidgetIds(ComponentName(context, BatteryWidgetProvider::class.java))
            ids.forEach { updateAppWidget(context, mgr, it) }
        }
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle
    ) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
        updateAppWidget(context, appWidgetManager, appWidgetId)
    }
}
