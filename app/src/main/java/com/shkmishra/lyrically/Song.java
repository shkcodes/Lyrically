package com.shkmishra.lyrically;

public class Song {

    private String track, artist;
    private long id;

    public Song(String track, String artist, long id) {
        this.track = track;
        this.artist = artist;
        this.id = id;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getTrack() {
        return track;
    }

    public void setTrack(String track) {
        this.track = track;
    }

    public String getArtist() {
        return artist;
    }

    public void setArtist(String artist) {
        this.artist = artist;
    }
}
