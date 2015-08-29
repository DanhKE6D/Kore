package org.xbmc.kore.model;

import org.xbmc.kore.jsonrpc.type.ListType;

import java.util.List;

/**
 * Created by danh.le on 8/17/15.
 */
public class MediaPlayList {
    public int playlistType;
    public List<ListType.ItemsAll> playlist;
    public MediaPlayList(int type, List<ListType.ItemsAll> l) {
        this.playlistType = type;
        this.playlist = l;
    }
}
