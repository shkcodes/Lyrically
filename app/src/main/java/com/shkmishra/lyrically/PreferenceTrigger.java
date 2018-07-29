package com.shkmishra.lyrically;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.os.IBinder;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

/*
  This file is almost identical to the LyricsService.java. I just removed the lyrics fetching code and added the SharedPrefs code.
 */

public class PreferenceTrigger extends Service {

    SharedPreferences sharedPreferences;

    SharedPreferences.OnSharedPreferenceChangeListener sharedPreferenceChangeListener;
    WindowManager.LayoutParams triggerParams, lyricsPanelParams;
    DisplayMetrics displayMetrics;
    View bottomLayout, trigger;
    LinearLayout container;
    Vibrator vibrator;
    private WindowManager windowManager;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {


        vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        displayMetrics = new DisplayMetrics();
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        windowManager.getDefaultDisplay().getMetrics(displayMetrics);

        int width = (sharedPreferences.getInt("triggerWidth", 10)) * 2;
        int height = (sharedPreferences.getInt("triggerHeight", 10)) * 2;


        triggerParams = new WindowManager.LayoutParams(
                width, height,

                WindowManager.LayoutParams.TYPE_PHONE,

                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                ,
                PixelFormat.TRANSLUCENT);

        int panelHeight = (sharedPreferences.getInt("panelHeight", 60)) * displayMetrics.heightPixels / 100;


        lyricsPanelParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                panelHeight,

                WindowManager.LayoutParams.TYPE_PHONE,

                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS

                ,
                PixelFormat.TRANSLUCENT);


        lyricsPanelParams.gravity = Gravity.BOTTOM;
        lyricsPanelParams.x = 0;
        lyricsPanelParams.y = 0;


        int triggerPosition = Integer.parseInt(sharedPreferences.getString("triggerPos", "1"));
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

        trigger.setBackgroundColor(getResources().getColor(R.color.colorAccent));


        bottomLayout = layoutInflater.inflate(R.layout.lyrics_sheet, null);
        TextView titleTV = (TextView) bottomLayout.findViewById(R.id.title);
        titleTV.setText(getResources().getString(R.string.songTitleHint));
        titleTV.setTextColor(Color.parseColor(sharedPreferences.getString("songTitleColor", "#fd5622")));

        bottomLayout.findViewById(R.id.content).setBackgroundColor(Color.parseColor(sharedPreferences.getString("panelColor", "#383F47")));

        ProgressBar progressBar = (ProgressBar) bottomLayout.findViewById(R.id.progressbar);
        progressBar.getIndeterminateDrawable().setColorFilter(Color.parseColor(sharedPreferences.getString("songTitleColor", "#fd5622")), android.graphics.PorterDuff.Mode.SRC_IN);

        TextView lyricsTV = (TextView) bottomLayout.findViewById(R.id.lyrics);
        lyricsTV.setText(getResources().getString(R.string.lyricsHint));
        lyricsTV.setVisibility(View.VISIBLE);
        lyricsTV.setTextColor(Color.parseColor(sharedPreferences.getString("lyricsTextColor", "#FFFFFF")));


        bottomLayout.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
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

        container = new LinearLayout(this);


        final int swipeDirection = Integer.parseInt(sharedPreferences.getString("swipeDirection", "1"));
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

        windowManager.addView(trigger, triggerParams);


        // check the onProgressChanged function in SeekBarPreference2.java
        sharedPreferenceChangeListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
            @Override
            public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
                switch (key) {
                    case "triggerOffset":
                        double offset = (double) (sharedPreferences.getInt("triggerOffset", 10)) / 100;
                        triggerParams.y = (int) (displayMetrics.heightPixels - (displayMetrics.heightPixels * offset));
                        windowManager.updateViewLayout(trigger, triggerParams);
                        break;

                    case "triggerHeight":
                        triggerParams.height = (sharedPreferences.getInt("triggerHeight", 10)) * 2;
                        windowManager.updateViewLayout(trigger, triggerParams);
                        break;

                    case "triggerWidth":
                        triggerParams.width = (sharedPreferences.getInt("triggerWidth", 10)) * 2;
                        windowManager.updateViewLayout(trigger, triggerParams);
                        break;
                }

            }
        };

        sharedPreferences.registerOnSharedPreferenceChangeListener(sharedPreferenceChangeListener);


        return super.onStartCommand(intent, flags, startId);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


    private void vibrate() {
        boolean vibrate = sharedPreferences.getBoolean("triggerVibration", true);
        if (vibrate) vibrator.vibrate(125);
    }


    @Override
    public void onDestroy() {
        super.onDestroy();

        try {
            container.removeView(bottomLayout);
            windowManager.removeView(container);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
        windowManager.removeView(trigger);

        sharedPreferences.unregisterOnSharedPreferenceChangeListener(sharedPreferenceChangeListener);
    }
}
