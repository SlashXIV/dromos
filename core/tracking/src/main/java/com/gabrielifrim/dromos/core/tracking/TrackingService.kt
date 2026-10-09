package com.gabrielifrim.dromos.core.tracking

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.gabrielifrim.dromos.core.domain.model.ActivityType
import com.gabrielifrim.dromos.core.domain.tracking.TrackingState
import com.gabrielifrim.dromos.core.ui.text.Format
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.time.Clock
import java.util.Locale
import javax.inject.Inject
import kotlin.math.floor

/**
 * Service de premier plan « localisation » : garde le suivi vivant écran éteint
 * ou application en arrière-plan. Il n'enregistre rien lui-même ; il tient le
 * processus éveillé et reflète l'état du moteur dans la notification.
 */
@AndroidEntryPoint
class TrackingService : Service() {

    @Inject lateinit var controller: TrackingController
    @Inject lateinit var clock: Clock

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var wakeLock: PowerManager.WakeLock? = null
    private var watchJob: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PAUSE -> scope.launch { controller.pause() }
            ACTION_RESUME -> scope.launch { controller.resume() }
            ACTION_STOP -> scope.launch { controller.stop() }
        }
        val active = controller.state.value as? TrackingState.Active
        if (active == null) {
            // Démarré sans sortie active (action tardive d'une notification) :
            // Android exige quand même un startForeground avant de s'arrêter.
            ServiceCompat.startForeground(this, NOTIFICATION_ID, placeholder(), FOREGROUND_TYPE)
            stopSelf()
            return START_NOT_STICKY
        }
        ServiceCompat.startForeground(
            this, NOTIFICATION_ID, buildNotification(active), FOREGROUND_TYPE,
        )
        if (wakeLock == null) {
            // Le GPS livre ses positions, mais sans ce verrou le processeur peut
            // s'endormir entre deux points en Doze et retarder les écritures.
            wakeLock = getSystemService(PowerManager::class.java)
                .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Dromos:suivi")
                .apply { acquire(MAX_WAKE_MS) }
        }
        if (watchJob == null) watchJob = scope.launch { watchState() }
        return START_NOT_STICKY
    }

    private suspend fun watchState() {
        controller.state
            // La notification ne change qu'à chaque pas de 10 m ou changement d'état :
            // le chrono, lui, tourne tout seul (setUsesChronometer).
            .map { state ->
                (state as? TrackingState.Active)?.let {
                    listOf(it.paused, it.autoPaused, floor(it.distanceMeters / 10), it.locationError)
                }
            }
            .distinctUntilChanged()
            .collect {
                val active = controller.state.value as? TrackingState.Active
                if (active == null) {
                    ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
                    stopSelf()
                } else {
                    getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID, buildNotification(active))
                }
            }
    }

    override fun onDestroy() {
        scope.cancel()
        wakeLock?.takeIf { it.isHeld }?.release()
        super.onDestroy()
    }

    private fun buildNotification(state: TrackingState.Active): Notification {
        val now = clock.instant()
        val moving = state.movingDurationAt(now)
        val km = getString(
            com.gabrielifrim.dromos.core.ui.R.string.unit_km,
            Format.kilometers(state.distanceMeters, Locale.getDefault()),
        )
        val typeLabel = getString(
            when (state.type) {
                ActivityType.WALK -> com.gabrielifrim.dromos.core.ui.R.string.activity_type_walk
                ActivityType.RUN -> com.gabrielifrim.dromos.core.ui.R.string.activity_type_run
            },
        )
        val title = when {
            state.paused -> getString(R.string.tracking_notification_paused, typeLabel)
            state.autoPaused -> getString(R.string.tracking_notification_auto_paused, typeLabel)
            state.locationError -> getString(R.string.tracking_notification_no_gps)
            else -> typeLabel
        }
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_dromos)
            .setContentTitle(title)
            .setContentText(km)
            .setContentIntent(openApp())
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_WORKOUT)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
        if (state.paused || state.autoPaused) {
            builder.setSubText(Format.duration(moving))
        }
        if (state.paused) {
            builder.addAction(0, getString(R.string.tracking_action_resume), action(ACTION_RESUME, 2))
        } else {
            // Chronomètre système : avance sans réveiller l'application chaque seconde.
            // En pause automatique, il est figé : la durée s'affiche en sous-texte.
            if (!state.autoPaused) {
                builder.setUsesChronometer(true).setShowWhen(true).setWhen(now.toEpochMilli() - moving.toMillis())
            }
            builder.addAction(0, getString(R.string.tracking_action_pause), action(ACTION_PAUSE, 1))
        }
        builder.addAction(0, getString(R.string.tracking_action_stop), action(ACTION_STOP, 3))
        return builder.build()
    }

    private fun placeholder(): Notification = NotificationCompat.Builder(this, CHANNEL_ID)
        .setSmallIcon(R.drawable.ic_stat_dromos)
        .setContentTitle(getString(R.string.tracking_channel))
        .setSilent(true)
        .build()

    private fun openApp(): PendingIntent? = packageManager.getLaunchIntentForPackage(packageName)?.let {
        PendingIntent.getActivity(
            this, 0, it.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }

    private fun action(action: String, requestCode: Int): PendingIntent = PendingIntent.getService(
        this, requestCode, Intent(this, TrackingService::class.java).setAction(action), PendingIntent.FLAG_IMMUTABLE,
    )

    companion object {
        const val CHANNEL_ID = "suivi"
        private const val NOTIFICATION_ID = 1
        private const val ACTION_PAUSE = "com.gabrielifrim.dromos.PAUSE"
        private const val ACTION_RESUME = "com.gabrielifrim.dromos.REPRISE"
        private const val ACTION_STOP = "com.gabrielifrim.dromos.FIN"
        /** Le type de service n'existe qu'à partir d'Android 10 ; avant, 0 suffit. */
        private val FOREGROUND_TYPE =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION else 0

        /** Garde-fou : un verrou oublié ne vide pas la batterie au-delà de 12 h. */
        private const val MAX_WAKE_MS = 12 * 60 * 60 * 1000L

        fun createChannel(context: Context) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.tracking_channel),
                NotificationManager.IMPORTANCE_LOW,
            ).apply { setShowBadge(false) }
            context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }
}
