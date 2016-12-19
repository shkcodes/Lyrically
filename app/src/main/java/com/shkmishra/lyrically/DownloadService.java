package com.shkmishra.lyrically;

import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.v7.app.NotificationCompat;

import java.io.File;
import java.util.ArrayList;

public class DownloadService extends Service {

    int notificationID = 92790914;
    ArrayList<Song> songArrayList = new ArrayList<>();
    int progress = 0, count = 1;


    @Override
    public int onStartCommand(Intent intent, final int flags, int startId) {

        // get the list of songs present on the device
        String selection = MediaStore.Audio.Media.IS_MUSIC + " != 0";
        String[] projection = {
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.DURATION
        };
        Cursor cursor = getContentResolver().query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                null,
                null);
        while (cursor.moveToNext()) {
            String artist = cursor.getString(1);
            String title = cursor.getString(2);
            long songID = Long.parseLong(cursor.getString(0));
            long duration = Long.parseLong(cursor.getString(3));
            if ((duration / 1000) > 40) {
                songArrayList.add(new Song(title, artist, songID));
                count++;
            }

        }

        cursor.close();

        final NotificationManager mNotifyManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        final NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this);
        mBuilder.setContentTitle(getResources().getString(R.string.downloadingLyrics))
                .setOngoing(true)
                .setProgress(count - 1, 0, false)
                .setSmallIcon(R.mipmap.ic_launcher);
        mNotifyManager.notify(
                notificationID,
                mBuilder.build());


        // handler to update the notification progress bar
        Handler handler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                mBuilder.setContentTitle(getResources().getString(R.string.downloadingLyrics) + " (" + ++progress + "/" + (count - 1) + ")");
                mBuilder.setProgress(count - 1, progress, false);
                mNotifyManager.notify(notificationID, mBuilder.build());
                if (count - 1 == progress) {
                    stopSelf();
                    mBuilder.setOngoing(false).setProgress(0, 0, false).setContentTitle(getResources().getString(R.string.lyricsDownloaded));
                    mNotifyManager.notify(notificationID, mBuilder.build());
                }

            }
        };



        File path = new File(Environment.getExternalStorageDirectory() + File.separator + "Lyrically/");
        File notFound = new File(path, "No Lyrics Found.txt");
        notFound.delete();

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
