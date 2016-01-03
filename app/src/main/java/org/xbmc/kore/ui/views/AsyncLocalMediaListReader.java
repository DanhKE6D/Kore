package org.xbmc.kore.ui.views;

import android.content.Context;
import android.support.v4.content.AsyncTaskLoader;
import android.util.Log;

import org.xbmc.kore.model.LocalMediaDataHandler;
import org.xbmc.kore.model.LocalMediaItem;
import org.xbmc.kore.utils.Config;
import org.xbmc.kore.utils.FileUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Created by danh.le on 1/1/16.
 */

public class AsyncLocalMediaListReader extends AsyncTaskLoader<LocalMediaDataHandler> {
    static final String TAG = AsyncLocalMediaListReader.class.getSimpleName();
    LocalMediaDataHandler mDataHandler;
    String mediaListFilename;
    Context myCtx = null;

    public AsyncLocalMediaListReader(Context context, String fileName) {
        super(context);
        this.myCtx = context;
        this.mediaListFilename = fileName;
        this.mDataHandler = new LocalMediaDataHandler();
    }

    public void setNewMediaListFileName(String fileName) {
        this.mediaListFilename = fileName;
    }

    @Override
    public LocalMediaDataHandler loadInBackground() {

        String localMediaListJSON = FileUtils.readFromFile(new File(mediaListFilename));
        mDataHandler = new LocalMediaDataHandler();
        if (localMediaListJSON.length() > 0) {
            LocalMediaItem[] mediaItemArray = Config.getGson().fromJson(localMediaListJSON, LocalMediaItem[].class);
            mDataHandler.setData(new ArrayList<>(Arrays.asList(mediaItemArray)));
        }
        else {
            mDataHandler.setData(new ArrayList<LocalMediaItem>());
        }
        Log.d(TAG, "Read local media list results from file completed, Number of media items = " + mDataHandler.getData().size());
        return mDataHandler;

    }

    @Override
    public void deliverResult(LocalMediaDataHandler data) {
        super.deliverResult(data);
    }
}