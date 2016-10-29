package com.shkmishra.lyrically;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.PixelFormat;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Vibrator;
import android.preference.PreferenceManager;
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

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;

public class LyricsService extends Service {



    String title,lyrics;

    String track = "",artist = "",artistU,trackU;
    TextView titleTV, lyricsTV;
    NestedScrollView scrollView;
    ImageView refresh;
    ProgressBar progressBar;
    int notifID = 26181317;

    SharedPreferences sharedPreferences;


    private WindowManager windowManager;

    WindowManager.LayoutParams triggerParams, lyricsPanelParams;

    DisplayMetrics displayMetrics;

    View bottomLayout,trigger;

    LinearLayout container;

    Vibrator vibrator;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {



        vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        displayMetrics = new DisplayMetrics();
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        windowManager.getDefaultDisplay().getMetrics(displayMetrics);

        int width = (sharedPreferences.getInt("triggerWidth",10))*2;
        int height = (sharedPreferences.getInt("triggerHeight",10))*2;

        triggerParams = new WindowManager.LayoutParams(
                width,height,

                WindowManager.LayoutParams.TYPE_PHONE,

                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE

                ,
                PixelFormat.TRANSLUCENT);


        int panelHeight =  (sharedPreferences.getInt("panelHeight",60))*displayMetrics.heightPixels/100;

        lyricsPanelParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                panelHeight,

                WindowManager.LayoutParams.TYPE_PHONE,

                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE

                ,
                PixelFormat.TRANSLUCENT);


        lyricsPanelParams.gravity = Gravity.BOTTOM;
        lyricsPanelParams.x = 0;
        lyricsPanelParams.y  = 0;


        int triggerPosition = Integer.parseInt(sharedPreferences.getString("triggerPos","1"));
        double offset = (double)(sharedPreferences.getInt("triggerOffset",10))/100;

        switch (triggerPosition){
            case 1 :  triggerParams.gravity = Gravity.TOP | Gravity.START; break;
            case 2 :  triggerParams.gravity = Gravity.TOP | Gravity.END; break;
        }
        triggerParams.x = 0;
        triggerParams.y =(int)( displayMetrics.heightPixels - (displayMetrics.heightPixels*offset));



        LayoutInflater layoutInflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
        trigger = new View(this);



        bottomLayout =  layoutInflater.inflate(R.layout.lyrics_sheet,null);
        bottomLayout.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        container = new LinearLayout(this);



        scrollView = (NestedScrollView) bottomLayout.findViewById(R.id.lyricsScrollView);
        titleTV = (TextView)bottomLayout.findViewById(R.id.title);
        lyricsTV = (TextView)bottomLayout.findViewById(R.id.lyrics);
        Typeface face= Typeface.createFromAsset(getAssets(), "fonts/BonvenoCF-Light.otf");
        lyricsTV.setTypeface(face);
        progressBar = (ProgressBar) bottomLayout.findViewById(R.id.progressbar);
        refresh = (ImageView) bottomLayout.findViewById(R.id.refresh);


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




        final int swipeDirection = Integer.parseInt(sharedPreferences.getString("swipeDirection","1"));
        trigger.setOnTouchListener(new OnSwipeTouchListener(this)
                                {
                                    @Override
                                    public void onSwipeUp() {
                                        super.onSwipeUp();
                                        if(swipeDirection==1) {
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
                                        if(swipeDirection==4) {
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
                                        if (swipeDirection==3){
                                            vibrate();
                                            windowManager.addView(container, lyricsPanelParams);
                                            container.addView(bottomLayout);
                                            Animation animation = AnimationUtils.loadAnimation(getApplicationContext(),R.anim.slide_up);
                                            bottomLayout.startAnimation(animation);
                                        }
                                    }

                                    @Override
                                    public void onSwipeDown() {
                                        super.onSwipeDown();
                                        if (swipeDirection==2) {
                                            vibrate();
                                            windowManager.addView(container, lyricsPanelParams);
                                            container.addView(bottomLayout);
                                            Animation animation = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.slide_up);
                                            bottomLayout.startAnimation(animation);
                                        }
                                    }
                                }
        );



        windowManager.addView(trigger, triggerParams);




        NotificationManager mNotifyManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationCompat.Builder mBuilder =  new NotificationCompat.Builder(this);

        PendingIntent pendingIntent = PendingIntent.getActivity(this,1,new Intent(this,MainActivity.class),PendingIntent.FLAG_UPDATE_CURRENT);

        mBuilder.setContentTitle("Lyrically")
                .setOngoing(true)
                .setContentIntent(pendingIntent)
                .setPriority(Notification.PRIORITY_MIN)
                .setSmallIcon(R.mipmap.ic_launcher);
        mNotifyManager.notify(
                notifID,
                mBuilder.build());



        IntentFilter iF = new IntentFilter();
        iF.addAction("com.spotify.music.metadatachanged");
        iF.addAction("com.spotify.music.playbackstatechanged");
        iF.addAction("com.android.music.metachanged");
        iF.addAction("com.android.music.playstatechanged");
        registerReceiver(musicReceiver, iF);




        return Service.START_STICKY;
    }



    private void vibrate(){
        boolean vibrate = sharedPreferences.getBoolean("triggerVibration",true);
        if (vibrate) vibrator.vibrate(125);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }



    @Override
    public void onDestroy() {
        super.onDestroy();
        Animation animation = AnimationUtils.loadAnimation(getApplicationContext(),R.anim.slide_down);
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

        unregisterReceiver(musicReceiver);

    }

    private BroadcastReceiver musicReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Bundle extras = intent.getExtras();

            boolean isPlaying = extras.getBoolean(extras.containsKey("playstate") ? "playstate" : "playing", true);
            try {

            if(isPlaying && !((artist.equalsIgnoreCase(intent.getStringExtra("artist")) && (track.equalsIgnoreCase(intent.getStringExtra("track"))))))
            {
                    progressBar.setVisibility(View.VISIBLE);
                    title = "";
                    lyrics = "";
                    artist = intent.getStringExtra("artist");
                    track = intent.getStringExtra("track");
                    artistU = artist.replaceAll(" ", "+");
                    trackU = track.replaceAll(" ", "+");
                    new FetchLyrics().execute();
                }
            }
            catch (NullPointerException e){

            }


        }
    } ;



    class FetchLyrics extends AsyncTask {

        boolean found = true;

        String url = "s",lyricURL = "s";



        @Override
        protected Object doInBackground(Object[] params) {

            url = "https://www.google.com/search?q=lyrics+genius+"+artistU+"+"+trackU;

            try {
                Document document = Jsoup.connect(url).userAgent("Mozilla/5.0").timeout(10000).get();
                Element results = document.select("h3.r > a").first();

                lyricURL = results.attr("href").substring(7, results.attr("href").indexOf("&"));
                Element element;
                String temp;
                if(lyricURL.contains("genius")) {

                    document = Jsoup.connect(lyricURL).userAgent("Mozilla/5.0 (Windows NT 6.1) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/41.0.2228.0 Safari/537.36").get();
                    title = document.select("meta[property=og:title]").first().attr("content");

                    Elements selector = document.select("div.h2");

                    for (Element e : selector) {
                        e.remove();
                    }

                    element = document.select("div[class=song_body-lyrics]").first();
                    temp = element.toString().substring(0,element.toString().indexOf("</lyrics>"));
                }
                else{
                    url = "https://www.google.com/search?q=lyrics.wikia+"+artistU+"+"+trackU;

                    document = Jsoup.connect(url).userAgent("Mozilla/5.0").timeout(10000).get();

                    results = document.select("h3.r > a").first();
                    lyricURL = results.attr("href").substring(7, results.attr("href").indexOf("&"));
                    document = Jsoup.connect(lyricURL).userAgent("Mozilla/5.0 (Windows NT 6.1) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/41.0.2228.0 Safari/537.36").get();
                    title = document.select("meta[property=og:title]").first().attr("content");
                    title = title.replace(":"," - ");

                    element = document.select("div[class=lyricbox]").first();
                    temp = element.toString();
                }

                temp = temp.replaceAll("(?i)<br[^>]*>", "br2n");
                temp = temp.replaceAll("]","]shk");
                temp = temp.replaceAll("\\[","shk[");


                lyrics = Jsoup.parse(temp).text();
                lyrics =   lyrics.replaceAll("br2n", "\n");
                lyrics = lyrics.replaceAll("]shk","]\n");
                lyrics = lyrics.replaceAll("shk\\[","\n [");
                if(lyricURL.contains("genius"))
                lyrics = lyrics.substring(lyrics.indexOf("Lyrics") + 6);



            } catch (IOException e) {
                e.printStackTrace();
            }
            catch (NullPointerException e){
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
            if(!found || !(lyrics.length()>0)) {
                lyricsTV.setText(getResources().getString(R.string.lyrics));
                lyricsTV.setVisibility(View.INVISIBLE);
                progressBar.setVisibility(View.GONE);
                titleTV.setText("No lyrics found");
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
            if(lyricsTV.getVisibility()!=View.VISIBLE)
            lyricsTV.setVisibility(View.VISIBLE);


            super.onPostExecute(o);
        }
    }




}
