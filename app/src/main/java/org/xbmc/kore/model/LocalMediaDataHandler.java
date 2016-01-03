package org.xbmc.kore.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by danh.le on 1/1/16.
 */
public class LocalMediaDataHandler {
    List<LocalMediaItem> mediaList = new ArrayList<LocalMediaItem>();

    public void setData(List<LocalMediaItem> l) {
        mediaList = l;
    }

    public List<LocalMediaItem> getData() {
        return mediaList;
    }
}
