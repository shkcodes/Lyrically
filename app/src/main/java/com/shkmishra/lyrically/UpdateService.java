package com.shkmishra.lyrically;

import android.app.IntentService;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.support.v7.app.NotificationCompat;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.IOException;

public class UpdateService extends IntentService {

    public UpdateService() {
        super("UpdateService");
    }


    @Override
    protected void onHandleIntent(Intent intent) {


        try {
            PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            String currentVersion = pInfo.versionName; // get the installed version

            Document document = Jsoup.connect("https://github.com/shkcodes/Lyrically/releases").userAgent("Mozilla/5.0 (Windows NT 6.1) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/41.0.2228.0 Safari/537.36").get();
            Element element = document.select("ul[class=tag-references mt-2]").first();
            String latestVersion = element.select("span[class=css-truncate-target]").first().text(); // get the latest version

            // show update notification if these versions mismatch
            if (!currentVersion.equals(latestVersion)) {
                NotificationManager mNotifyManager =
                        (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this);
                Intent notificationIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/shkcodes/Lyrically/releases"));

                PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);

                mBuilder.setContentTitle(getString(R.string.updateTitle))
                        .setContentText(getString(R.string.updateText))
                        .setContentIntent(contentIntent)
                        .setAutoCancel(true)
                        .setSmallIcon(R.mipmap.ic_launcher);
                mNotifyManager.notify(
                        26181562,
                        mBuilder.build());
            }


        } catch (IOException e) {
            e.printStackTrace();
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

    }
}
