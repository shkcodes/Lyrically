package com.shkmishra.lyrically;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.support.v4.widget.NestedScrollView;
import android.support.v7.app.NotificationCompat;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Calendar;

public class LyricsService extends Service {


    String title, lyrics;
    boolean offlineMusic = false;
    File[] lyricsFiles;
    boolean cacheAll = false;

    String track = "", artist = "", artistU, trackU;
    TextView titleTV, lyricsTV;
    NestedScrollView scrollView;
    ImageView refresh;
    ProgressBar progressBar;
    int notifID = 26181317;
    ArrayList<Song> songArrayList = new ArrayList<>();

    SharedPreferences sharedPreferences;
    WindowManager.LayoutParams triggerParams, lyricsPanelParams;
    DisplayMetrics displayMetrics;
    View bottomLayout, trigger;
    LinearLayout container;
    Vibrator vibrator;
    private WindowManager windowManager;
    Intent showLyrics;
    PendingIntent pendingIntent;


    @Override
    @SuppressLint("NewApi")
    public int onStartCommand(Intent intent, int flags, int startId) {


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && (!Settings.canDrawOverlays(this) || !(checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED))) {
            Toast.makeText(this, R.string.permissions_toast, Toast.LENGTH_SHORT).show();
            return 0;
        }
        NotificationManager mNotifyManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this);

        if (vibrator != null) { // this means the service is already running; so we just handle the intent and update the lyrics sheet
            // make the notification persistent
            mBuilder.setContentTitle("Lyrically")
                    .setOngoing(true)
                    .setContentIntent(pendingIntent)
                    .setPriority(Notification.PRIORITY_MIN)
                    .setSmallIcon(R.mipmap.ic_launcher);
            mNotifyManager.notify(
                    26181317,
                    mBuilder.build());
            handleIntent(intent);
            return 0;
        }
        vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        displayMetrics = new DisplayMetrics();
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        windowManager.getDefaultDisplay().getMetrics(displayMetrics);

        int width = (sharedPreferences.getInt("triggerWidth", 10)) * 2;
        int height = (sharedPreferences.getInt("triggerHeight", 10)) * 2;

        cacheAll = sharedPreferences.getBoolean("cacheAll", false);
        getSongsList(); // get list of songs present on the device
        File file = new File(Environment.getExternalStorageDirectory(), "Lyrically");
        if (!file.exists())
            file.mkdirs();

        // params for the invisible trigger
        triggerParams = new WindowManager.LayoutParams(
                width, height,

                WindowManager.LayoutParams.TYPE_PHONE,

                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE

                ,
                PixelFormat.TRANSLUCENT);


        int panelHeight = (sharedPreferences.getInt("panelHeight", 60)) * displayMetrics.heightPixels / 100;

        // params for the lyrics panel
        lyricsPanelParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                panelHeight,

                WindowManager.LayoutParams.TYPE_PHONE,

                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE

                ,
                PixelFormat.TRANSLUCENT);


        lyricsPanelParams.gravity = Gravity.BOTTOM;
        lyricsPanelParams.x = 0;
        lyricsPanelParams.y = 0;


        int triggerPosition = Integer.parseInt(sharedPreferences.getString("triggerPos", "1")); // 1 = left side of the screen, 2 = right side
        double offset = (double) (sharedPreferences.getInt("triggerOffset", 10)) / 100;

        switch (triggerPosition) {
            case 1:
                triggerParams.gravity = Gravity.TOP | Gravity.START;
                break;
            case 2:
                triggerParams.gravity = Gravity.TOP | Gravity.END;
                break;
        }
        triggerParams.x = 0;
        triggerParams.y = (int) (displayMetrics.heightPixels - (displayMetrics.heightPixels * offset));


        LayoutInflater layoutInflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
        trigger = new View(this);


        bottomLayout = layoutInflater.inflate(R.layout.lyrics_sheet, null);
        bottomLayout.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        container = new LinearLayout(this);

        // views in the lyrics panel
        scrollView = (NestedScrollView) bottomLayout.findViewById(R.id.lyricsScrollView);
        titleTV = (TextView) bottomLayout.findViewById(R.id.title);
        lyricsTV = (TextView) bottomLayout.findViewById(R.id.lyrics);
        progressBar = (ProgressBar) bottomLayout.findViewById(R.id.progressbar);
        progressBar.getIndeterminateDrawable().setColorFilter(getResources().getColor(R.color.colorAccent), PorterDuff.Mode.SRC_IN);
        refresh = (ImageView) bottomLayout.findViewById(R.id.refresh);

        // swipe listener to dismiss the lyrics panel
        bottomLayout.setOnTouchListener(new SwipeDismissTouchListener(bottomLayout, null, new SwipeDismissTouchListener.DismissCallbacks() {
            @Override
            public boolean canDismiss(Object token) {
                return true;
            }

            @Override
            public void onDismiss(View view, Object token) {
                container.removeView(bottomLayout);
                windowManager.removeView(container);
            }
        }));


        final int swipeDirection = Integer.parseInt(sharedPreferences.getString("swipeDirection", "1")); // swipe direction for the invisible trigger
        trigger.setOnTouchListener(new OnSwipeTouchListener(this) {
                                       @Override
                                       public void onSwipeUp() {
                                           super.onSwipeUp();
                                           if (swipeDirection == 1) {
                                               vibrate();
                                               windowManager.addView(container, lyricsPanelParams);
                                               container.addView(bottomLayout);
                                               Animation animation = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.slide_up);
                                               bottomLayout.startAnimation(animation);
                                           }
                                       }

                                       @Override
                                       public void onSwipeRight() {
                                           super.onSwipeRight();
                                           if (swipeDirection == 4) {
                                               vibrate();
                                               windowManager.addView(container, lyricsPanelParams);
                                               container.addView(bottomLayout);
                                               Animation animation = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.slide_up);
                                               bottomLayout.startAnimation(animation);
                                           }
                                       }

                                       @Override
                                       public void onSwipeLeft() {
                                           super.onSwipeLeft();
                                           if (swipeDirection == 3) {
                                               vibrate();
                                               windowManager.addView(container, lyricsPanelParams);
                                               container.addView(bottomLayout);
                                               Animation animation = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.slide_up);
                                               bottomLayout.startAnimation(animation);
                                           }
                                       }

                                       @Override
                                       public void onSwipeDown() {
                                           super.onSwipeDown();
                                           if (swipeDirection == 2) {
                                               vibrate();
                                               windowManager.addView(container, lyricsPanelParams);
                                               container.addView(bottomLayout);
                                               Animation animation = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.slide_up);
                                               bottomLayout.startAnimation(animation);
                                           }
                                       }
                                   }
        );

        // handler to show the lyrics panel when the notification is clicked
        final Handler handler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                try {
                    windowManager.addView(container, lyricsPanelParams);
                    container.addView(bottomLayout);
                    Animation animation = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.slide_up);
                    bottomLayout.startAnimation(animation);
                } catch (IllegalStateException e) {
                    e.printStackTrace();
                }
            }
        };

        // removes the invisible trigger if you click on it; otherwise the area taken by the trigger seems unresponsive to the user
        trigger.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                windowManager.removeView(trigger);
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        windowManager.addView(trigger, triggerParams);
                    }
                }, 5000);
            }
        });


        windowManager.addView(trigger, triggerParams);


        showLyrics = new Intent(this, ShowLyrics.class);
        showLyrics.putExtra("messenger", new Messenger(handler));
        pendingIntent = PendingIntent.getService(this, 1, showLyrics, PendingIntent.FLAG_UPDATE_CURRENT);


        mBuilder.setContentTitle("Lyrically")
                .setOngoing(true)
                .setContentIntent(pendingIntent)
                .setPriority(Notification.PRIORITY_MIN)
                .setSmallIcon(R.mipmap.ic_launcher);
        mNotifyManager.notify(
                notifID,
                mBuilder.build());

        handleIntent(intent);

        checkForUpdates();

        return Service.START_STICKY;
    }


    private void checkForUpdates() {
        int lastCheck = sharedPreferences.getInt("updateWeek", 0);
        int currentWeek = Calendar.getInstance().get(Calendar.WEEK_OF_YEAR);

        if (currentWeek != lastCheck) {
            startService(new Intent(this, UpdateService.class));
            sharedPreferences.edit().putInt("updateWeek", currentWeek).apply();
        }
    }

    private void handleIntent(Intent intent) {
        try {
            // check if the song was changed by comparing the current artist and title with those received from the broadcast
            if (!((artist.equalsIgnoreCase(intent.getStringExtra("artist")) && (track.equalsIgnoreCase(intent.getStringExtra("track")))))) {

                // clear the lyrics sheet
                title = "";
                lyrics = "";
                progressBar.setVisibility(View.VISIBLE);
                titleTV.setText("");
                lyricsTV.setText("");
                lyricsTV.setVisibility(View.INVISIBLE);
                refresh.setVisibility(View.GONE);

                artist = intent.getStringExtra("artist");
                track = intent.getStringExtra("track");

                for (Song song : songArrayList) {
                    if ((song.getArtist().equalsIgnoreCase(artist)) && (song.getTrack().equalsIgnoreCase(track))) { // check if the song is present on the device

                        lyrics = getLyrics(song); // gets the lyrics from the text file
                        if (!lyrics.equals("")) {  // indicates we have offline lyrics available
                            title = artist + " - " + track;
                            lyricsTV.setText(lyrics);
                            lyricsTV.setVisibility(View.VISIBLE);
                            scrollView.fullScroll(ScrollView.FOCUS_UP);
                            refresh.setVisibility(View.GONE);
                            titleTV.setText(title);
                            progressBar.setVisibility(View.GONE);
                        } else { // offline lyrics not found, fetch them from the Internet
                            lyricsTV.setText("");
                            lyricsTV.setVisibility(View.INVISIBLE);
                            artistU = artist.replaceAll(" ", "+");
                            trackU = track.replaceAll(" ", "+");
                            new FetchLyrics().execute();
                            break;
                        }
                        offlineMusic = true;
                    }
                }
                if (!offlineMusic) { // indicates that the song is being streamed
                    if (cacheAll) {
                        lyrics = getLyricsStreaming();
                        if (!lyrics.equals("")) {
                            title = artist + " - " + track;
                            lyricsTV.setText(lyrics);
                            lyricsTV.setVisibility(View.VISIBLE);
                            scrollView.fullScroll(ScrollView.FOCUS_UP);
                            refresh.setVisibility(View.GONE);
                            titleTV.setText(title);
                            progressBar.setVisibility(View.GONE);
                        } else {
                            artistU = artist.replaceAll(" ", "+");
                            trackU = track.replaceAll(" ", "+");
                            new FetchLyrics().execute();
                        }
                    } else {
                        artistU = artist.replaceAll(" ", "+");
                        trackU = track.replaceAll(" ", "+");
                        new FetchLyrics().execute();
                    }
                }

            }
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
    }

    private void vibrate() {
        boolean vibrate = sharedPreferences.getBoolean("triggerVibration", true);
        if (vibrate) vibrator.vibrate(125);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    // remove the lyrics panel and the trigger and unregister the receiver
    @Override
    public void onDestroy() {
        super.onDestroy();
        try {
            Animation animation = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.slide_down);
            bottomLayout.startAnimation(animation);
            animation.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {

                }

                @Override
                public void onAnimationEnd(Animation animation) {
                    container.removeView(bottomLayout);
                    windowManager.removeView(container);
                }

                @Override
                public void onAnimationRepeat(Animation animation) {

                }
            });
            windowManager.removeView(trigger);
        } catch (IllegalArgumentException | NullPointerException e) {
            e.printStackTrace();
        }

    }


    private void getSongsList() {

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
            }
        }
        cursor.close();


    }

    private String getLyrics(Song song) {

        StringBuilder stringBuilder = new StringBuilder();

        String path = Environment.getExternalStorageDirectory() + File.separator + "Lyrically/";
        File directory = new File(path);
        if (directory.exists())
            lyricsFiles = directory.listFiles(); // files present in the Lyrically folder

        if (lyricsFiles == null)
            return "";
        for (File file : lyricsFiles) {
            if (file.getName().equals(song.getId() + ".txt")) { // the text files are named after the song IDs

                try {
                    String line;
                    BufferedReader bufferedReader = new BufferedReader(new FileReader(file));
                    while ((line = bufferedReader.readLine()) != null) {
                        stringBuilder.append(line);
                        stringBuilder.append("\n");
                    }
                    bufferedReader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return stringBuilder.toString();
            }
        }
        return "";

    }

    private void saveLyricsOffline(String lyrics) {
        for (Song song : songArrayList) {
            if ((song.getArtist().equalsIgnoreCase(artist)) && (song.getTrack().equalsIgnoreCase(track))) {
                File path = new File(Environment.getExternalStorageDirectory() + File.separator + "Lyrically/");
                File lyricsFile = new File(path, song.getId() + ".txt");
                try {
                    FileWriter fileWriter = new FileWriter(lyricsFile);
                    fileWriter.write(lyrics);
                    fileWriter.flush();
                    fileWriter.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private String getLyricsStreaming() {

        StringBuilder stringBuilder = new StringBuilder();
        String fileName = Integer.toString((artist + track).hashCode());

        String path = Environment.getExternalStorageDirectory() + File.separator + "Lyrically/";
        File directory = new File(path);
        if (directory.exists())
            lyricsFiles = directory.listFiles(); // files present in the Lyrically folder

        if (lyricsFiles == null)
            return "";
        for (File file : lyricsFiles) {
            if (file.getName().equals(fileName + ".txt")) { // the text files are named after the song IDs

                try {
                    String line;
                    BufferedReader bufferedReader = new BufferedReader(new FileReader(file));
                    while ((line = bufferedReader.readLine()) != null) {
                        stringBuilder.append(line);
                        stringBuilder.append("\n");
                    }
                    bufferedReader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return stringBuilder.toString();
            }
        }
        return "";
    }

    private void saveLyricsStreaming(String lyrics) {
        String fileName = Integer.toString((artist + track).hashCode());

        File path = new File(Environment.getExternalStorageDirectory() + File.separator + "Lyrically/");
        File lyricsFile = new File(path, fileName + ".txt");
        try {
            FileWriter fileWriter = new FileWriter(lyricsFile);
            fileWriter.write(lyrics);
            fileWriter.flush();
            fileWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    // fetches the lyrics from the Internet
    class FetchLyrics extends AsyncTask {

        boolean found = true;

        String url = "s", lyricURL = "s";


        @Override
        protected Object doInBackground(Object[] params) {

            /*
            Currently using 3 providers : azlyrics, genius and lyrics.wikia; in that order
            Procedure :
            - Google the artist + song name + provider name
            - Grab the first result and if it is from the provider we wanted, get the lyrics
             */

            try {

                url = "https://www.google.com/search?q=" + URLEncoder.encode("lyrics+azlyrics+" + artistU + "+" + trackU, "UTF-8"); // Google URL
                Document document = Jsoup.connect(url).userAgent("Mozilla/5.0").timeout(10000).get();
                Element results = document.select("h3.r > a").first();

                lyricURL = results.attr("href").substring(7, results.attr("href").indexOf("&")); // grabbing the first result
                Element element;
                String temp;

                if (lyricURL.contains("azlyrics.com/lyrics")) { // checking if from the provider we wanted
                    document = Jsoup.connect(lyricURL).userAgent("Mozilla/5.0 (Windows NT 6.1) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/41.0.2228.0 Safari/537.36").get();
                    title = artist + " - " + track;
                    String page = document.toString();

                    page = page.substring(page.indexOf("that. -->") + 9);
                    page = page.substring(0, page.indexOf("</div>"));
                    temp = page;

                } else {

                    url = "https://www.google.com/search?q=" + URLEncoder.encode("genius+" + artistU + "+" + trackU + "lyrics", "UTF-8");
                    document = Jsoup.connect(url).userAgent("Mozilla/5.0").timeout(10000).get();

                    results = document.select("h3.r > a").first();
                    lyricURL = results.attr("href").substring(7, results.attr("href").indexOf("&"));


                    if (lyricURL.contains("genius")) {

                        document = Jsoup.connect(lyricURL).userAgent("Mozilla/5.0 (Windows NT 6.1) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/41.0.2228.0 Safari/537.36").get();
                        title = document.select("meta[property=og:title]").first().attr("content");

                        Elements selector = document.select("div.h2");

                        for (Element e : selector) {
                            e.remove();
                        }

                        element = document.select("div[class=song_body-lyrics]").first();
                        temp = element.toString().substring(0, element.toString().indexOf("</lyrics>"));
                    } else {

                        url = "https://www.google.com/search?q=" + URLEncoder.encode("lyrics.wikia+" + trackU + "+" + artistU, "UTF-8");


                        document = Jsoup.connect(url).userAgent("Mozilla/5.0").timeout(10000).get();

                        results = document.select("h3.r > a").first();
                        lyricURL = results.attr("href").substring(7, results.attr("href").indexOf("&"));


                        document = Jsoup.connect(lyricURL).userAgent("Mozilla/5.0 (Windows NT 6.1) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/41.0.2228.0 Safari/537.36").get();
                        title = document.select("meta[property=og:title]").first().attr("content");
                        title = title.replace(":", " - ");

                        element = document.select("div[class=lyricbox]").first();
                        temp = element.toString();

                    }


                }

                // preserving line breaks

                temp = temp.replaceAll("(?i)<br[^>]*>", "br2n");
                temp = temp.replaceAll("]", "]shk");
                temp = temp.replaceAll("\\[", "shk[");

                lyrics = Jsoup.parse(temp).text();
                lyrics = lyrics.replaceAll("br2n", "\n");
                lyrics = lyrics.replaceAll("]shk", "]\n");
                lyrics = lyrics.replaceAll("shk\\[", "\n [");
                if (lyricURL.contains("genius"))
                    lyrics = lyrics.substring(lyrics.indexOf("Lyrics") + 6);


            } catch (IOException e) {
                e.printStackTrace();
            } catch (NullPointerException e) {
                found = false;
            }
            return null;
        }


        @Override
        protected void onPreExecute() {
            super.onPreExecute();

        }

        @Override
        protected void onPostExecute(Object o) {
            if (!found || !(lyrics.length() > 0)) { // if no lyrics found
                lyricsTV.setText("");
                lyricsTV.setVisibility(View.INVISIBLE);
                progressBar.setVisibility(View.GONE);
                titleTV.setText(getResources().getString(R.string.noLyricsFound));
                refresh.setVisibility(View.VISIBLE);
                refresh.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {

                        new FetchLyrics().execute();
                        progressBar.setVisibility(View.VISIBLE);
                    }
                });
                return;
            }


            refresh.setVisibility(View.GONE);
            progressBar.setVisibility(View.GONE);
            scrollView.fullScroll(ScrollView.FOCUS_UP);
            titleTV.setText(title);
            lyricsTV.setText(lyrics);
            if (lyricsTV.getVisibility() != View.VISIBLE)
                lyricsTV.setVisibility(View.VISIBLE);

            if (offlineMusic) saveLyricsOffline(lyrics); // store the lyrics in a text file
            else if (cacheAll) saveLyricsStreaming(lyrics);

            super.onPostExecute(o);
        }
    }


}
