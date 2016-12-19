package com.shkmishra.lyrically;


import android.app.IntentService;
import android.content.Intent;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;

/*
  Used to show the lyrics panel when the user clicks on the notification
 */
public class ShowLyrics extends IntentService {

    public ShowLyrics() {
        super("ShowLyrics");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Messenger messenger = (Messenger)  intent.getExtras().get("messenger");
        try {
            messenger.send(new Message());
        } catch (RemoteException e) {
            e.printStackTrace();
        }

    }
}