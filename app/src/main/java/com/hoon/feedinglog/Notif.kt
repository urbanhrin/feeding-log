package com.hoon.feedinglog

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat

/** 잠금화면에 늘 떠 있는 알림. 버튼(분유/이유식/유아식)은 설정에서 고른 것만 붙는다. */
object Notif {
    private const val CHANNEL = "quick_log"
    private const val ID = 1
    private const val FLAGS = PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    const val ACTION_REPOST = "com.hoon.feedinglog.REPOST"

    fun post(ctx: Context) {
        if (Build.VERSION.SDK_INT >= 33 &&
            ctx.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return

        val nm = ctx.getSystemService(NotificationManager::class.java)
        // 소리·진동 없음. 조용한 알림(LOW)은 잠금화면에서 숨겨지는 경우가 있어 DEFAULT로 둔다
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL, "빠른 기록", NotificationManager.IMPORTANCE_DEFAULT).apply {
                setSound(null, null)
                enableVibration(false)
                setShowBadge(false)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            },
        )

        val last = Repo.entries.value.firstOrNull()
        val options = Repo.options.value
        val open = Intent(ctx, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val repost = Intent(ctx, NotifReceiver::class.java).setAction(ACTION_REPOST)

        val b = NotificationCompat.Builder(ctx, CHANNEL)
            .setSmallIcon(R.drawable.ic_stat)
            .setColor(0xFFB8915E.toInt())
            .setContentTitle(if (last == null) "아직 기록이 없어요" else "마지막 수유 ${hm(last.time)}")
            .setContentText(last?.describe() ?: "버튼을 누르면 지금 시각으로 기록돼요")
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(PendingIntent.getActivity(ctx, 0, open, FLAGS))
            // 밀어서 지워도 바로 다시 띄운다
            .setDeleteIntent(PendingIntent.getBroadcast(ctx, 0, repost, FLAGS))

        // 마지막 수유부터 지난 시간이 알림에서 계속 흘러간다
        if (last != null) b.setWhen(last.time).setShowWhen(true).setUsesChronometer(true)
        else b.setShowWhen(false)

        FeedType.entries.filter { options.show[it.ordinal] }.forEach { t ->
            val i = Intent(ctx, QuickLogActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                .putExtra(QuickLogActivity.EXTRA_TYPE, t.ordinal)
            b.addAction(0, t.label, PendingIntent.getActivity(ctx, 10 + t.ordinal, i, FLAGS))
        }

        nm.notify(ID, b.build())
    }
}

/** 재부팅·앱 업데이트 뒤, 또는 알림을 밀어서 지웠을 때 알림을 다시 띄운다 */
class NotifReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Notif.post(context)
    }
}
