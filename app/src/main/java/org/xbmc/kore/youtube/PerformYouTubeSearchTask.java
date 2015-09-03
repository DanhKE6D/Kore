package org.xbmc.kore.youtube;

import android.app.Activity;
import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.util.Log;
import android.view.Gravity;
import android.view.Window;
import android.view.WindowManager;

import org.xbmc.kore.utils.Config;
import org.xbmc.kore.utils.FileUtils;
import org.xbmc.kore.utils.UIUtils;
import org.xbmc.kore.youtube.model.YouTubeVideo;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Created by dql on 3/6/14.
 */
public class PerformYouTubeSearchTask extends AsyncTask<String, Void, Boolean> {
    static final String TAG = PerformYouTubeSearchTask.class.getSimpleName();
    private ProgressDialog progressDialog;
    StringBuilder errString = new StringBuilder();
    String searchTerm;
    String nextPageToken;
    String appName;
    Activity myCtx;
    String searchResultFilename;
    private OnUpdateSearchResultList updateResultListListener = null;
    YouTubeManager ytm;
    YouTubeVideo.YouTubeMediaType searchType;

    public PerformYouTubeSearchTask(Activity ctx, OnUpdateSearchResultList listener, final String fileName, YouTubeVideo.YouTubeMediaType type) {
        myCtx = ctx;
        updateResultListListener = listener;
        searchResultFilename = fileName;
        searchType = type;
    }

    public interface OnUpdateSearchResultList {
        void updateSearchResultList(String searchTerm, String prevPgToken, String nextPgToken);
    }

    @Override
    protected void onPreExecute() {
        // perhaps show a dialog with a progress bar
        // to let your users know something is happening
    }

    @Override
    protected Boolean doInBackground(String... params) {

        searchTerm = params[0];
        nextPageToken = params[1];
        appName = params[2];


        myCtx.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                // change UI elements here
                //progressDialog.dismiss(); // no longer need because skipping check server connection
                progressDialog = new ProgressDialog(myCtx);
                progressDialog.setIndeterminate(true);
                progressDialog.setMessage("Searching for " + searchTerm + "\nWaiting for results from YouTube");
                progressDialog.setCancelable(false);
                Window w = progressDialog.getWindow();
                WindowManager.LayoutParams wlp = w.getAttributes();
                wlp.gravity = Gravity.BOTTOM;
                wlp.flags &= ~WindowManager.LayoutParams.FLAG_DIM_BEHIND;
                w.setAttributes(wlp);
                progressDialog.show();
            }
        });
        try {
            Log.i(TAG, "Retrieving search results from YouTube");
            ytm = new YouTubeManager(appName, searchType);

            List<YouTubeVideo> videos = ytm.retrieveVideos(searchTerm, nextPageToken, Config.getInstance().getmaxNumOfSearchResults());
            Log.i(TAG, "Found " + videos.size() + " related to " + searchTerm);
            final String searchResultListGson =  Config.getGson().toJson(videos);
            FileUtils.savePlaylistToFile(new File(searchResultFilename), searchResultListGson);
            Log.i(TAG, "Done saving to search results");
            return true;

        } catch (IOException e) {
            errString.append("IOException occurred during searching YouTube");
        } catch (Exception e1) {
            errString.append("Exception occurred during searching YouTube");
        }

        return false;
    }

    @Override
    protected void onPostExecute(Boolean result) {
        final boolean res = result;
        myCtx.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                cancelDialog();
                if (!res) {
                    // something failed during read
                    UIUtils.errorMesgBox(myCtx, "Error: Unable to get YouTube search results", errString.toString());
                }
                else {
                    if (updateResultListListener != null)
                        updateResultListListener.updateSearchResultList(searchTerm, ytm.getPrevPageToken(), ytm.getNextPageToken());
                }
            }
        });
    }

    public void cancelDialog() {
        if (progressDialog != null) {
            progressDialog.dismiss();
        }
    }

}

