package com.shkmishra.lyrically;


import android.app.IntentService;
import android.content.Intent;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;

public class ShowLyrics extends IntentService {


    public ShowLyrics() {
        super("ShowLyrics");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Log.d("shk","cluck");
        Messenger messenger = (Messenger)  intent.getExtras().get("messenger");
        try {
            messenger.send(new Message());
        } catch (RemoteException e) {
            e.printStackTrace();
        }

    }
}