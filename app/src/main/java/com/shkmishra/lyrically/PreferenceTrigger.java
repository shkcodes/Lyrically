package com.shkmishra.lyrically;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.PixelFormat;
import android.os.IBinder;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.LinearLayout;

public class PreferenceTrigger extends Service {

    SharedPreferences sharedPreferences;

    SharedPreferences.OnSharedPreferenceChangeListener sharedPreferenceChangeListener;

    private WindowManager windowManager;

    WindowManager.LayoutParams params,layoutParams;

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


        params = new WindowManager.LayoutParams(
                width,height,

                WindowManager.LayoutParams.TYPE_PHONE,

                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                ,
                PixelFormat.TRANSLUCENT);

        int panelHeight =  (sharedPreferences.getInt("panelHeight",60))*displayMetrics.heightPixels/100;


        layoutParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                panelHeight,

                WindowManager.LayoutParams.TYPE_PHONE,

                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS

                ,
                PixelFormat.TRANSLUCENT);


        layoutParams.gravity = Gravity.BOTTOM;
        layoutParams.x = 0;
        layoutParams.y = 0;


        int triggerPosition = Integer.parseInt(sharedPreferences.getString("triggerPos","1"));
        switch (triggerPosition){
            case 1 :  params.gravity = Gravity.TOP | Gravity.START; break;
            case 2 :  params.gravity = Gravity.TOP | Gravity.END; break;
        }
        params.x = 0;


        double offset = (double)(sharedPreferences.getInt("triggerOffset",10))/100;
        params.y =(int)( displayMetrics.heightPixels - (displayMetrics.heightPixels*offset));



        LayoutInflater layoutInflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
        trigger = new View(this);

        trigger.setBackgroundColor(getResources().getColor(R.color.colorAccent));


        bottomLayout =  layoutInflater.inflate(R.layout.lyrics_sheet,null);
        bottomLayout.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));


        container = new LinearLayout(this)
        {
            @Override
            public boolean dispatchKeyEvent(KeyEvent event) {
                if(event.getKeyCode() == KeyEvent.KEYCODE_BACK || event.getKeyCode() == KeyEvent.KEYCODE_HOME){
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
                    return true;
                }

                return super.dispatchKeyEvent(event);
            }
        };


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
                                               windowManager.addView(container, layoutParams);
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
                                               windowManager.addView(container, layoutParams);
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
                                               windowManager.addView(container,layoutParams);
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
                                               windowManager.addView(container, layoutParams);
                                               container.addView(bottomLayout);
                                               Animation animation = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.slide_up);
                                               bottomLayout.startAnimation(animation);
                                           }
                                       }
                                   }
        );

        windowManager.addView(trigger, params);



        sharedPreferenceChangeListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
            @Override
            public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
                if(key.equals("triggerOffset")) {
                    double offset = (double)(sharedPreferences.getInt("triggerOffset",10))/100;
                    params.y =(int)( displayMetrics.heightPixels - (displayMetrics.heightPixels*offset));
                    windowManager.updateViewLayout(trigger,params);
                }
                else if (key.equals("triggerHeight")){
                    params.height = (sharedPreferences.getInt("triggerHeight",10))*2;
                    windowManager.updateViewLayout(trigger,params);
                }

                else if (key.equals("triggerWidth")){
                    params.width = (sharedPreferences.getInt("triggerWidth",10))*2;
                    windowManager.updateViewLayout(trigger,params);
                }
            }};

        sharedPreferences.registerOnSharedPreferenceChangeListener(sharedPreferenceChangeListener);



        return super.onStartCommand(intent, flags, startId);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


    private void vibrate(){
        boolean vibrate = sharedPreferences.getBoolean("triggerVibration",true);
        if (vibrate) vibrator.vibrate(125);
    }


    @Override
    public void onDestroy() {
        super.onDestroy();

        try {
            container.removeView(bottomLayout);
            windowManager.removeView(container);
        }
        catch (IllegalArgumentException e){
            e.printStackTrace();
        }
        windowManager.removeView(trigger);

        sharedPreferences.unregisterOnSharedPreferenceChangeListener(sharedPreferenceChangeListener);
    }
}
