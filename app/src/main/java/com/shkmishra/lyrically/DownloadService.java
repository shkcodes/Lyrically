package com.shkmishra.lyrically;

import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v7.app.NotificationCompat;

public class DownloadService extends Service {


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        final NotificationManager mNotifyManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        final NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this);
        mBuilder.setContentTitle(getResources().getString(R.string.notificationTitle))
                .setOngoing(true)
                .setProgress(100,0,false)
                .setSmallIcon(R.mipmap.ic_launcher);
        mNotifyManager.notify(
                927909,
                mBuilder.build());


        return super.onStartCommand(intent, flags, startId);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
