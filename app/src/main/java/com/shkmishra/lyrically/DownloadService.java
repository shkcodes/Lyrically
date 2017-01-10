package com.shkmishra.lyrically;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.support.annotation.Nullable;
import android.support.v7.app.NotificationCompat;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

public class DownloadService extends Service {

    int notificationID = 92790914;
    ArrayList<Song> songArrayList = new ArrayList<>();
    int progress = 1, count;


    @Override
    public int onStartCommand(final Intent intent, final int flags, int startId) {

        final NotificationManager mNotifyManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        final NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this);

        try { // try - catch for a crash which occurs if the user removes Lyrically from the recents while the lyrics are being downloaded
            songArrayList = intent.getParcelableArrayListExtra("songs");
            count = songArrayList.size();
        } catch (NullPointerException e) {
            stopSelf();
            mNotifyManager.cancel(notificationID);
            return START_NOT_STICKY;
        }

        mBuilder.setContentText(getResources().getString(R.string.downloadingLyrics))
                .setContentTitle(songArrayList.get(0).getTrack() + " - " + songArrayList.get(0).getArtist())
                .setOngoing(true)
                .setProgress(count, progress, false)
                .setSmallIcon(R.mipmap.ic_launcher);
        mNotifyManager.notify(
                notificationID,
                mBuilder.build());

        File path = new File(Environment.getExternalStorageDirectory() + File.separator + "Lyrically/");
        final File notFound = new File(path, "No Lyrics Found.txt");
        notFound.delete();
        try {
            notFound.createNewFile();
            FileWriter fileWriter = new FileWriter(notFound);
            fileWriter.write("This file can be found at /sdcard/Lyrically/No Lyrics Found.txt\n\n");
            fileWriter.flush();
            fileWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        // handler to update the notification progress bar
        Handler handler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                if (count == progress) {
                    stopSelf();
                    Intent intent1 = new Intent(Intent.ACTION_VIEW);
                    intent1.setDataAndType(Uri.fromFile(notFound), "text/*");
                    PendingIntent pendingIntent = PendingIntent.getActivity(DownloadService.this, 0, intent1, PendingIntent.FLAG_UPDATE_CURRENT);
                    mBuilder.setOngoing(false).setContentIntent(pendingIntent).setProgress(0, 0, false).setContentTitle(getResources().getString(R.string.noLyricsFoundNotification)).setContentText("").setAutoCancel(true);
                    mNotifyManager.notify(notificationID, mBuilder.build());
                } else {
                    mBuilder.setContentText("(" + ++progress + "/" + (count) + ")");
                    mBuilder.setProgress(count, progress, false);
                    mBuilder.setContentTitle(songArrayList.get(progress - 1).getTrack() + " - " + songArrayList.get(progress - 1).getArtist());
                    mNotifyManager.notify(notificationID, mBuilder.build());
                }

            }
        };


        Messenger messenger = new Messenger(handler);
        for (Song song : songArrayList) { // fetch the lyrics for each song
            Intent intent1 = new Intent(this, FetchLyrics.class);
            intent1.putExtra("track", song.getTrack());
            intent1.putExtra("messenger", messenger);
            intent1.putExtra("artist", song.getArtist());
            intent1.putExtra("id", song.getId());
            startService(intent1);
        }
        return super.onStartCommand(intent, flags, startId);
    }


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
