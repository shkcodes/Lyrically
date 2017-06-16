package com.shkmishra.lyrically;

import android.app.IntentService;
import android.content.Intent;
import android.os.Environment;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URLEncoder;

/*
Fetches the lyrics and stores them in a text file. The fetching code is the same as in LyricsService.java.
 */
public class FetchLyrics extends IntentService {

    public FetchLyrics() {
        super("FetchLyrics");
    }

    String artist, track;
    long songID;
    Messenger messenger;
    File lyricsFile, notFound;

    @Override
    protected void onHandleIntent(Intent intent) {
        artist = intent.getStringExtra("artist");
        track = intent.getStringExtra("track");
        songID = intent.getLongExtra("id", 0);
        messenger = (Messenger) intent.getExtras().get("messenger");


        File path = new File(Environment.getExternalStorageDirectory() + File.separator + "Lyrically/");
        notFound = new File(path, "No Lyrics Found.txt");

        lyricsFile = new File(path, songID + ".txt");


        if (!lyricsFile.exists())
            getLyrics();
        else
            try { // if the text file with the current song ID already exists, skip fetching the lyrics
            messenger.send(new Message());
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

    }

    void getLyrics() {
        // same code as the doInBackground function from LyricsService.java
        try {
            String artistU = artist.replaceAll(" ", "+");
            String trackU = track.replaceAll(" ", "+");
            String url = "https://www.google.com/search?q=" + URLEncoder.encode("lyrics+azlyrics+" + artistU + "+" + trackU, "UTF-8");
            Document document = Jsoup.connect(url).userAgent("Mozilla/5.0").timeout(10000).get();
            Element results = document.select("h3.r > a").first();

            String lyricURL = results.attr("href").substring(7, results.attr("href").indexOf("&"));
            Element element;
            String temp;

            if (lyricURL.contains("azlyrics.com/lyrics")) {
                document = Jsoup.connect(lyricURL).userAgent("Mozilla/5.0 (Windows NT 6.1) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/41.0.2228.0 Safari/537.36").get();
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

                    Elements selector = document.select("div.h2");

                    for (Element e : selector) {
                        e.remove();
                    }

                    element = document.select("div[class=song_body-lyrics]").first();
                    temp = element.toString().substring(0, element.toString().indexOf("<!--/sse-->"));
                } else {

                    url = "https://www.google.com/search?q=" + URLEncoder.encode("lyrics.wikia+" + trackU + "+" + artistU, "UTF-8");


                    document = Jsoup.connect(url).userAgent("Mozilla/5.0").timeout(10000).get();

                    results = document.select("h3.r > a").first();
                    lyricURL = results.attr("href").substring(7, results.attr("href").indexOf("&"));
                    document = Jsoup.connect(lyricURL).userAgent("Mozilla/5.0 (Windows NT 6.1) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/41.0.2228.0 Safari/537.36").get();

                    element = document.select("div[class=lyricbox]").first();
                    temp = element.toString();
                }


            }

            temp = temp.replaceAll("(?i)<br[^>]*>", "br2n");
            temp = temp.replaceAll("]", "]shk");
            temp = temp.replaceAll("\\[", "shk[");


            String lyrics = Jsoup.parse(temp).text();
            lyrics = lyrics.replaceAll("br2n", "\n");
            lyrics = lyrics.replaceAll("]shk", "]\n");
            lyrics = lyrics.replaceAll("shk\\[", "\n [");
            if (lyricURL.contains("genius"))
                lyrics = lyrics.substring(lyrics.indexOf("Lyrics") + 6);

            writeToFile(lyrics); // write the lyrics to a text file


        } catch (IOException e) {
            e.printStackTrace();
            noLyricsFound();
        } catch (NullPointerException e) {
            e.printStackTrace();
            noLyricsFound();
        } finally {
            try {
                messenger.send(new Message());
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    void writeToFile(String lyrics) {
        try {
            FileWriter fileWriter = new FileWriter(lyricsFile);
            fileWriter.write(lyrics);
            fileWriter.flush();
            fileWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void noLyricsFound() {
        // if no lyrics were found, write the artist and song name to the No Lyrics Found.txt
        try {
            FileWriter fileWriter = new FileWriter(notFound, true);
            fileWriter.append(artist + " | " + track + "\n");
            fileWriter.flush();
            fileWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
