package org.xbmc.kore.youtube;

import android.content.Context;
import android.support.v4.content.AsyncTaskLoader;
import android.util.Log;

import org.xbmc.kore.utils.Config;
import org.xbmc.kore.utils.FileUtils;
import org.xbmc.kore.youtube.model.YouTubeVideo;
import org.xbmc.kore.youtube.model.YouTubeVideoDataHandler;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by danh.le on 8/30/15.
 */
public class AsyncVideoListReader extends AsyncTaskLoader<YouTubeVideoDataHandler> {
    static final String TAG = AsyncVideoListReader.class.getSimpleName();
    YouTubeVideoDataHandler mDataHandler;
    String videoListFilename;
    Context myCtx = null;

    public AsyncVideoListReader(Context context, String fileName) {
        super(context);
        this.myCtx = context;
        this.videoListFilename = fileName;
        this.mDataHandler = new YouTubeVideoDataHandler();
    }

    public void setNewVideoListFileName(String fileName) {
        this.videoListFilename = fileName;
    }

    @Override
    public YouTubeVideoDataHandler loadInBackground() {

        String searchResultJSON = FileUtils.readFromFile(new File(videoListFilename));
        mDataHandler = new YouTubeVideoDataHandler();
        YouTubeVideo[] ytArray = Config.getGson().fromJson(searchResultJSON, YouTubeVideo[].class);
        mDataHandler.setData(new ArrayList<>(Arrays.asList(ytArray)));
        Log.d(TAG, "Read search results from file completed, Number of videos = " + mDataHandler.getData().size());
        return mDataHandler;

    }

    @Override
    public void deliverResult(YouTubeVideoDataHandler data) {
        super.deliverResult(data);
    }
}
