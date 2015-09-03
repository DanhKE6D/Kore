package org.xbmc.kore.youtube.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by danh.le on 8/30/15.
 */
public class YouTubeVideoDataHandler {
    List<YouTubeVideo> videoList = new ArrayList<YouTubeVideo>();

    public void setData(List<YouTubeVideo> l) {
        videoList = l;
    }

    public List<YouTubeVideo> getData() {
        return videoList;
    }
}
