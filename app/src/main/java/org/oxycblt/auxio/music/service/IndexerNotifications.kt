/*
 * Copyright (c) 2022 Auxio Project
 * IndexerNotifications.kt is part of Auxio.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
 
package org.oxycblt.auxio.music.service

import android.content.Context
import android.os.SystemClock
import androidx.annotation.StringRes
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import org.oxycblt.auxio.BuildConfig
import org.oxycblt.auxio.IntegerTable
import org.oxycblt.auxio.R
import org.oxycblt.auxio.music.IndexingProgress
import org.oxycblt.auxio.util.logD
import org.oxycblt.auxio.util.newMainPendingIntent

/**
 * Wrapper around [NotificationCompat.Builder] intended for use for [NotificationCompat]s that
 * signal a Service's ongoing foreground state.
 *
 * @author Alexander Capehart (OxygenCobalt)
 */
abstract class IndexerNotification(context: Context, info: ChannelInfo) :
    NotificationCompat.Builder(context, info.id) {
    private val notificationManager = NotificationManagerCompat.from(context)

    init {
        // Set up the notification channel. Foreground notifications are non-substantial, and
        // thus make no sense to have lights, vibration, or lead to a notification badge.
        val channel =
            NotificationChannelCompat.Builder(info.id, NotificationManagerCompat.IMPORTANCE_LOW)
                .setName(context.getString(info.nameRes))
                .setLightsEnabled(false)
                .setVibrationEnabled(false)
                .setShowBadge(false)
                .build()
        notificationManager.createNotificationChannel(channel)
    }

    /**
     * The code used to identify this notification.
     *
     * @see NotificationManagerCompat.notify
     */
    abstract val code: Int

    /**
     * Reduced representation of a [NotificationChannelCompat].
     *
     * @param id The ID of the channel.
     * @param nameRes A string resource ID corresponding to the human-readable name of this channel.
     */
    data class ChannelInfo(val id: String, @StringRes val nameRes: Int)
}

/**
 * A dynamic [IndexerNotification] that shows the current music loading state.
 *
 * @param context [Context] required to create the notification.
 * @author Alexander Capehart (OxygenCobalt)
 */
class IndexingNotification(private val context: Context) :
    IndexerNotification(context, indexerChannel) {
    private var lastUpdateTime = -1L

    init {
        setSmallIcon(R.drawable.ic_indexer_24)
        setCategory(NotificationCompat.CATEGORY_PROGRESS)
        setShowWhen(false)
        setSilent(true)
        setContentIntent(context.newMainPendingIntent())
        setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
        setContentTitle(context.getString(R.string.lbl_indexing))
        setContentText(context.getString(R.string.lng_indexing))
        setProgress(0, 0, true)
    }

    override val code: Int
        get() = IntegerTable.INDEXER_NOTIFICATION_CODE

    /**
     * Update this notification with the new music loading state.
     *
     * @param progress The new music loading state to display in the notification.
     * @return true if the notification updated, false otherwise
     */
    fun updateIndexingState(progress: IndexingProgress): Boolean {
        when (progress) {
            is IndexingProgress.Indeterminate -> {
                // Indeterminate state, use a vaguer description and in-determinate progress.
                // These events are not very frequent, and thus we don't need to safeguard
                // against rate limiting.
                logD("Updating state to $progress")
                lastUpdateTime = -1
                setContentText(context.getString(R.string.lng_indexing))
                setProgress(0, 0, true)
                return true
            }
            is IndexingProgress.Songs -> {
                // Determinate state, show an active progress meter. Since these updates arrive
                // highly rapidly, only update every 1.5 seconds to prevent notification rate
                // limiting.
                val now = SystemClock.elapsedRealtime()
                if (lastUpdateTime > -1 && (now - lastUpdateTime) < 1500) {
                    return false
                }
                lastUpdateTime = SystemClock.elapsedRealtime()
                logD("Updating state to $progress")
                setContentText(
                    context.getString(R.string.fmt_indexing, progress.current, progress.total))
                setProgress(progress.total, progress.current, false)
                return true
            }
        }
    }
}

/**
 * A static [IndexerNotification] that signals to the user that the app is currently monitoring the
 * music library for changes.
 *
 * @author Alexander Capehart (OxygenCobalt)
 */
class ObservingNotification(context: Context) : IndexerNotification(context, indexerChannel) {
    init {
        setSmallIcon(R.drawable.ic_indexer_24)
        setCategory(NotificationCompat.CATEGORY_SERVICE)
        setShowWhen(false)
        setSilent(true)
        setContentIntent(context.newMainPendingIntent())
        setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
        setContentTitle(context.getString(R.string.lbl_observing))
        setContentText(context.getString(R.string.lng_observing))
    }

    override val code: Int
        get() = IntegerTable.INDEXER_NOTIFICATION_CODE
}

/** Notification channel shared by [IndexingNotification] and [ObservingNotification]. */
private val indexerChannel =
    IndexerNotification.ChannelInfo(
        id = BuildConfig.APPLICATION_ID + ".channel.INDEXER", nameRes = R.string.lbl_indexer)
