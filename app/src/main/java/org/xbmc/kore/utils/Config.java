package org.xbmc.kore.utils;

import android.os.Environment;

import com.google.gson.Gson;

import org.xbmc.kore.youtube.model.YouTubeVideo;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by danh.le on 8/29/15.
 */
public class Config {
    static final String TAG = Config.class.getSimpleName();
    static final String appDirInSDCard = "Kore";
    static final String playlistDir = "Playlists";
    static final String playlistDirNet = "Net";
    static final String playlistDirLocal = "Local";
    public static final String ytSearchResultFile = "SearchResult.json";

    static Gson sGsonInstance = new Gson();
    private String prevPageToken, nextPageToken, searchText;
    private long maxNumOfSearchResults = 25;
    private YouTubeVideo.YouTubeMediaType searchType = YouTubeVideo.YouTubeMediaType.Video;
    List<String> ytSearchTextHistory = new ArrayList<String>();
    private int displayThumbnailSize = 0;
    public static Gson getGson() { return sGsonInstance; }


    private static class Loader {
        static Config INSTANCE = new Config();
    }

    private Config() {
        // set with default parameters
        createRootDirsIfNotAlreadyExisted();
    }

    public static Config getInstance() {
        return Loader.INSTANCE;
    }

    public void setYTSearchTextParams(String pToken, String nToken, String text) {
        prevPageToken = pToken;
        nextPageToken = nToken;
        searchText = text;
        setSearchTextHistory(searchText);
    }

    public List<String> getYTSearchTextHistory() {
        return ytSearchTextHistory;
    }

    public void setSearchTextHistory(String str) {
        // check to see if the search string is already in our history list, if it is not, add it
        // in
        boolean foundIt = false;
        for (int i = 0; i < ytSearchTextHistory.size(); i++) {
            String s = ytSearchTextHistory.get(i);
            if (str.equalsIgnoreCase(s)) {
                foundIt = true;
                break;
            }
        }
        if (!foundIt)
            ytSearchTextHistory.add(str);
    }

    public String getPrevPageToken() {
        return prevPageToken;
    }

    public String getNextPageToken() {
        return nextPageToken;
    }

    public String getSearchText() {
        return searchText;
    }

    static boolean createDirIfNotExists(String dir) {
        boolean ret = true;

        File file = new File(Environment.getExternalStorageDirectory().getPath() + "/" + dir);
        if (!file.exists()) {
            if (!file.mkdirs()) {
                ret = false;
            }
        }
        return ret;
    }

    static void createDefaultDirsOnSDCard() {
        createDirIfNotExists(appDirInSDCard);
        createDirIfNotExists(appDirInSDCard +"/"+ playlistDir);
        createDirIfNotExists(appDirInSDCard + "/" + playlistDir + "/" + playlistDirNet);
        createDirIfNotExists(appDirInSDCard + "/" + playlistDir + "/" + playlistDirLocal);
    }

    static void createRootDirsIfNotAlreadyExisted() {
        File file = new File(Environment.getExternalStorageDirectory().getPath() + "/" + appDirInSDCard);
        if (!file.exists()) {
            createDefaultDirsOnSDCard();
        }
    }

    public static String getSearchResultFile() {
        return Environment.getExternalStorageDirectory().getPath() +
                "/" + appDirInSDCard + "/" + playlistDir + "/" + ytSearchResultFile;
    }

    public static String getPlaylistDirectory() {
        return Environment.getExternalStorageDirectory().getPath() +
                "/" + appDirInSDCard + "/" + playlistDir + "/";
    }


    public long getmaxNumOfSearchResults() {
        return this.maxNumOfSearchResults;
    }
    public void setmaxNumOfSearchResults(long n) {
        this.maxNumOfSearchResults = n;
    }
    public int getDefaultThumbSize() {
        return this.displayThumbnailSize;
    }
    public void setDefaultThumbSize(int size) {
        this.displayThumbnailSize = size;
    }
    public void setYouTubeMediaSearchMode(YouTubeVideo.YouTubeMediaType type) {
        searchType = type;
    }
    public YouTubeVideo.YouTubeMediaType getYouTubeMediaSearchMode() {
        return searchType;
    }
}
