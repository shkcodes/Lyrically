package com.shkmishra.lyrically;

import android.app.IntentService;
import android.content.Intent;


public class StopService extends IntentService {
    public StopService() {
        super("StopService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        stopService(new Intent(this, LyricsService.class));
    }

}
