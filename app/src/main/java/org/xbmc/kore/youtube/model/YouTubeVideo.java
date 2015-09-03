package org.xbmc.kore.youtube.model;

/**
 * Created by dql on 3/1/14.
 */
import java.util.ArrayList;

public class YouTubeVideo {

    public enum YouTubeMediaType {
        Video, Playlist, VideoChannel
    };

    private ArrayList<String> thumbnails;
    private String webPlayerUrl;
    private String title;
    private String uploader;
    private long duration;
    private YouTubeMediaType mediaType;

    public void setMediaType(YouTubeMediaType t) {mediaType = t; }
    public YouTubeMediaType getMediaType() { return mediaType; }
    public ArrayList<String> getThumbnails() {
        return thumbnails;
    }
    public void setThumbnails(ArrayList<String> thumbnails) {
        this.thumbnails = thumbnails;
    }
    public String getWebPlayerUrl() {
        return webPlayerUrl;
    }
    public void setWebPlayerUrl(String webPlayerUrl) {
        this.webPlayerUrl = webPlayerUrl;
    }
    public String getTitle() {
        return title;
    }
    public void setTitle(String t) {
        title = t;
    }
    public String getUploader() {
        return uploader;
    }
    public void setUploader(String n) {
        uploader = n;
    }
    public long getDuration() {
        return duration;
    }
    public void setDuration(long d) {
        duration = d;
    }
}
