package com.shkmishra.lyrically;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.NotificationCompat;


public class MusicReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {

        Bundle extras = intent.getExtras();
        boolean isPlaying = extras.getBoolean(extras.containsKey("playstate") ? "playstate" : "playing", true);

        if (isPlaying) {
            // if the music is playing, send an intent to LyricsService
            Intent intent1 = new Intent(context, LyricsService.class);
            intent1.putExtra("artist", intent.getStringExtra("artist"));
            intent1.putExtra("track", intent.getStringExtra("track"));
            context.startService(intent1);
        } else { // make the notification dismissible
            NotificationManager mNotifyManager =
                    (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(context);
            Intent stopIntent = new Intent(context, StopService.class);
            mBuilder.setContentTitle("Lyrically")
                    .setDeleteIntent(PendingIntent.getService(context, 0, stopIntent, 0)) // stop LyricsService when user swipes away the notification
                    .setOngoing(false)
                    .setPriority(Notification.PRIORITY_MIN)
                    .setSmallIcon(R.mipmap.ic_launcher);
            mNotifyManager.notify(
                    26181317,
                    mBuilder.build());
        }

    }


}
