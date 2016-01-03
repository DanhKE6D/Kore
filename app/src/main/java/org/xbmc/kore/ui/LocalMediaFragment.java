package org.xbmc.kore.ui;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.widget.CardView;
import android.support.v7.widget.SearchView;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.google.api.services.youtube.model.PlaylistItem;

import org.xbmc.kore.R;
import org.xbmc.kore.host.HostConnectionObserver;
import org.xbmc.kore.host.HostManager;
import org.xbmc.kore.jsonrpc.ApiCallback;
import org.xbmc.kore.jsonrpc.ApiMethod;
import org.xbmc.kore.jsonrpc.method.GUI;
import org.xbmc.kore.jsonrpc.method.Player;
import org.xbmc.kore.jsonrpc.method.Playlist;
import org.xbmc.kore.jsonrpc.type.GUIType;
import org.xbmc.kore.jsonrpc.type.ListType;
import org.xbmc.kore.jsonrpc.type.PlayerType;
import org.xbmc.kore.jsonrpc.type.PlaylistType;
import org.xbmc.kore.model.LocalMediaDataHandler;
import org.xbmc.kore.model.LocalMediaItem;
import org.xbmc.kore.model.MediaPlayList;
import org.xbmc.kore.ui.views.AsyncLocalMediaListReader;
import org.xbmc.kore.utils.CharacterDrawable;
import org.xbmc.kore.utils.Config;
import org.xbmc.kore.utils.FileUtils;
import org.xbmc.kore.utils.LogUtils;
import org.xbmc.kore.utils.UIUtils;
import org.xbmc.kore.utils.Utils;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.TimeUnit;

import ar.com.daidalos.afiledialog.FileChooserDialog;
import butterknife.ButterKnife;
import butterknife.InjectView;

/**
 * Created by danh.le on 8/23/15.
 */
public class LocalMediaFragment extends Fragment implements
        LoaderManager.LoaderCallbacks<LocalMediaDataHandler>,
        HostConnectionObserver.PlayerEventsObserver,
        SearchView.OnQueryTextListener {
    private static final String TAG = LocalMediaFragment.class.getSimpleName();
    static final int LOAD_LOCAL_MEDIA_LIST = 1010;

    final int myLoaderID = LOAD_LOCAL_MEDIA_LIST;
    ListView listView;

    AsyncLocalMediaListReader asyncLocalMediaListReader;
    List<LocalMediaItem> mediaList = new ArrayList<>();
    Queue<LocalMediaItem> mediaQueue = new LinkedList<>();
    enum PlaylistItemMode { None, InsertToPlaylist }
    LocalMediaListAdapter mediaListAdapter;
    PlaylistItemMode playlistItemMode = PlaylistItemMode.None;
    MenuItem searchItem = null;
    enum FileSelectionMode { None, FileLoad, FileSave}
    FileSelectionMode fileSelectionMode = FileSelectionMode.None;

    /**
     * Host manager from which to get info about the current XBMC
     */
    private HostManager hostManager;

    /**
     * Activity to communicate potential actions that change what's playing
     */
    private HostConnectionObserver hostConnectionObserver;

    /**
     * Handler on which to post RPC callbacks
     */
    private Handler callbackHandler = new Handler();

    // The search filter to use in the loader
    private String searchFilter = null;

    /**
     * Injectable views
     */
    @InjectView(R.id.info_panel) RelativeLayout infoPanel;
    @InjectView(R.id.playlist) GridView localMediaGridView;
    @InjectView(R.id.info_title) TextView infoTitle;
    @InjectView(R.id.info_message) TextView infoMessage;

    @Override
    public Loader<LocalMediaDataHandler> onCreateLoader(int arg0, Bundle arg1) {
        Log.i(TAG, "onCreateLoader: LoaderID = " + myLoaderID + " videoFile = " + Config.getSearchResultFile());
        asyncLocalMediaListReader = new AsyncLocalMediaListReader(LocalMediaFragment.this.getActivity(), Config.getLocalMediaListFile());
        asyncLocalMediaListReader.forceLoad();
        return asyncLocalMediaListReader;
    }

    @Override
    public void onLoadFinished(Loader<LocalMediaDataHandler> arg0, LocalMediaDataHandler arg1) {
        Log.i(TAG, "onLoadFinished: LoaderID = " + myLoaderID);
        mediaList = arg1.getData();
        setMediaListAdapter();
        // see if the search item is open. If it is, collapse it
        if (searchItem != null) {
            searchItem.collapseActionView();
        }
    }

    @Override
    public void onLoaderReset(Loader<LocalMediaDataHandler> arg0) {
        mediaListAdapter = null;
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        hostManager = HostManager.getInstance(getActivity());
        hostConnectionObserver = hostManager.getHostConnectionObserver();
        Log.i(TAG, "LocalMediaFragment.onCreate");
    }

    void setMediaListAdapter() {
        mediaListAdapter = new LocalMediaListAdapter();
        localMediaGridView.setAdapter(mediaListAdapter);

        // When clicking on an item, play it
        localMediaGridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                // click an item, add it to the playlist
                PlaylistType.Item item = new PlaylistType.Item();
                //item.file = mediaList.get(position).getWebPlayerUrl();
                //queueMedia(item);

            }
        });
        localMediaGridView.setTextFilterEnabled(true);
        if ((mediaList == null) || (mediaList.size() == 0)) {
            displayEmptySearchResultMessage();
        }
        else {
            switchToPanel(R.id.playlist);
            mediaListAdapter.setLocalMediaListItems(mediaList);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,Bundle savedInstanceState) {
        ViewGroup root = (ViewGroup) inflater.inflate(R.layout.fragment_playlist, container, false);
        ButterKnife.inject(this, root);
        setMediaListAdapter();
        getActivity().getSupportLoaderManager().initLoader(myLoaderID, null, this);
        return root;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        // We have options
        setHasOptionsMenu(true);
    }

    @Override
    public void onResume() {
        super.onResume();
        hostConnectionObserver.registerPlayerObserver(this, true);
    }

    @Override
    public void onPause() {
        super.onPause();
        hostConnectionObserver.unregisterPlayerObserver(this);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.local_media_list, menu);
        // Setup search view
        searchItem = menu.findItem(R.id.action_search);
        SearchView searchView = (SearchView) MenuItemCompat.getActionView(searchItem);
        searchView.setOnQueryTextListener(this);
        searchView.setQueryHint(getString(R.string.action_search));
        super.onCreateOptionsMenu(menu, inflater);
    }

    // The following callbacks are called for the SearchView.OnQueryChangeListener
    public boolean onQueryTextChange(String newText) {
        //newText = newText.isEmpty() ? "" : "Query so far: " + newText;
        //mSearchText.setText(newText);
        //mSearchText.setTextColor(Color.GREEN);
        Log.i(TAG, "onQueryTextChange: Query so far: " + newText);
        mediaListAdapter.getFilter().filter(newText);
        return true;
    }

    public boolean onQueryTextSubmit(String query) {
        //Toast.makeText(this, "Searching for: " + query + "...", Toast.LENGTH_SHORT).show();
        //mSearchText.setText("Searching for: " + query + "...");
        //mSearchText.setTextColor(Color.RED);
        Log.i(TAG, "onQueryTextSubmit: query:" + query);
        //InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        //imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
        searchFilter = query;
        if (query.length() > 0) {
            // create an async task to search
        }
        return true;
    }

    /**
     * @return text entered in searchview
     */
    public String getSearchFilter() {
        return searchFilter;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_cvt_to_media_list:
                // convert to playlist to media list
                {
                fileSelectionMode = FileSelectionMode.FileLoad;
                FileChooserDialog dialog = new FileChooserDialog(getActivity());
                // Assign listener for the select event.
                dialog.addListener(LocalMediaFragment.this.onFileSelectedListener);
                // Define start folder.
                dialog.loadFolder(Config.getPlaylistDirectory());
                dialog.setShowConfirmation(true, false);
                dialog.show();
                // load will be performed in dialog callback
                }
            break;
            default:
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    // ---- Methods for display the results ----- //
    private FileChooserDialog.OnFileSelectedListener onFileSelectedListener = new FileChooserDialog.OnFileSelectedListener() {
        public void onFileSelected(Dialog source, File file) {
            source.hide();
            if (fileSelectionMode == FileSelectionMode.FileLoad) {
                String playlistJSON = FileUtils.readFromFile(file);
                final MediaPlayList playlistToConvert = Config.getGson().fromJson(playlistJSON, MediaPlayList.class);
                if (playlistToConvert.playlist.size() > 0) {
                    // fork a runnable to convert this playlist to media list
                    mediaList = new ArrayList<LocalMediaItem>();
                    Thread t = new Thread() {
                        public void run() {
                            for (ListType.ItemsAll item : playlistToConvert.playlist) {
                                mediaList.add(new LocalMediaItem(item.label, item.file, item.thumbnail, item.runtime));
                            }
                            // save this result file to the default file
                            Collections.sort(mediaList);
                            File mediaFile = new File(Config.getLocalMediaListFile());
                            FileUtils.savePlaylistToFile(mediaFile, Config.getGson().toJson(mediaList));
                        }
                    };
                    try {
                        t.start();
                        t.join();
                        Log.i(TAG, "There are " + mediaList.size() + " media items in this list");
                        setMediaListAdapter();
                    } catch (InterruptedException e) {
                        Log.e(TAG, "Thread did not complete exception");
                    }
                }

            }
            else if (fileSelectionMode == FileSelectionMode.FileSave) {
                // save current list
            }


        }
        public void onFileSelected(Dialog source, File folder, String name) {
            source.hide();
            File newFile = new File(Config.getPlaylistDirectory() + "/" + folder.getName() + "/" + name);
            //Toast toast = Toast.makeText(getActivity(), "File created: " + folder.getName() + "/" + name, Toast.LENGTH_LONG);
            //toast.show();
            // can not be read from file -- we are creating new one
            if (fileSelectionMode == FileSelectionMode.FileSave) {
                // save it
            }
        }
    };

    /**
     * Switches the info panel shown (they are exclusive)
     * @param panelResId The panel to show
     */
    private void switchToPanel(int panelResId) {
        switch (panelResId) {
            case R.id.info_panel:
                infoPanel.setVisibility(View.VISIBLE);
                localMediaGridView.setVisibility(View.GONE);
                break;
            case R.id.playlist:
                infoPanel.setVisibility(View.GONE);
                localMediaGridView.setVisibility(View.VISIBLE);
                break;
        }
    }

    /**
     * Displays an error on the info panel
     * @param details Details message
     */
    private void displayErrorReadingFromCacheMessage(String details) {
        switchToPanel(R.id.info_panel);
        infoTitle.setText(R.string.error_reading_search_result);
        infoMessage.setText(String.format(getString(R.string.error_message), details));
    }

    /**
     * Displays empty search result
     */
    private void displayEmptySearchResultMessage() {
        switchToPanel(R.id.info_panel);
        infoTitle.setText(R.string.local_media_list_empty);
        infoMessage.setText(null);
    }
    void switchToFullScreen() {
        final GUI.GetProperties action = new GUI.GetProperties(GUI.GetProperties.FULLSCREEN);
        action.execute(hostManager.getConnection(), new ApiCallback<GUIType.PropertyValue>() {
            @Override
            public void onSuccess(GUIType.PropertyValue result) {
                if (!result.fullscreen) {
                    // switch to fullscreen
                    GUI.SetFullscreen actionSetFullscreen = new GUI.SetFullscreen();
                    actionSetFullscreen.execute(hostManager.getConnection(), null, null);
                }
            }

            @Override
            public void onError(int errorCode, String description) {
                // got an error, abort everything
                LogUtils.LOGE(TAG, "Callback error, code = " + errorCode + ", description = " + description);
            }
        }, callbackHandler);
    }

    /**
     * Default callback for methods that don't return anything
     */
    private ApiCallback<String> defaultStringActionCallback = ApiMethod.getDefaultActionCallback();


    /**
     * Last call results
     */
    private int lastCallResult = HostConnectionObserver.PlayerEventsObserver.PLAYER_NO_RESULT;
    private ListType.ItemsAll lastGetItemResult = null;
    private PlayerType.GetActivePlayersReturnType lastGetActivePlayerResult;
    private PlayerType.PropertyValue lastGetPropertiesResult;
    private List<ListType.ItemsAll> lastGetPlaylistItemsResult = null;

    /**
     * HostConnectionObserver.PlayerEventsObserver interface callbacks
     */
    public void playerOnPlay(PlayerType.GetActivePlayersReturnType getActivePlayerResult,
                             PlayerType.PropertyValue getPropertiesResult,
                             ListType.ItemsAll getItemResult) {

        // Save results
        lastCallResult = PLAYER_IS_PLAYING;
        lastGetActivePlayerResult = getActivePlayerResult;
        lastGetPropertiesResult = getPropertiesResult;
        lastGetItemResult = getItemResult;
    }

    public void playerOnPause(PlayerType.GetActivePlayersReturnType getActivePlayerResult,
                              PlayerType.PropertyValue getPropertiesResult,
                              ListType.ItemsAll getItemResult) {

        lastCallResult = PLAYER_IS_PAUSED;
        lastGetActivePlayerResult = getActivePlayerResult;
        lastGetPropertiesResult = getPropertiesResult;
        lastGetItemResult = getItemResult;
    }

    public void playerOnStop() {
        lastCallResult = PLAYER_IS_STOPPED;
    }

    public void playerOnConnectionError(int errorCode, String description) {
        lastCallResult = HostConnectionObserver.PlayerEventsObserver.PLAYER_CONNECTION_ERROR;
    }

    public void playerNoResultsYet() {
        lastCallResult = HostConnectionObserver.PlayerEventsObserver.PLAYER_NO_RESULT;
    }


    public void systemOnQuit() {
        playerNoResultsYet();
    }

    // Ignore this
    public void inputOnInputRequested(String title, String type, String value) {}
    public void observerOnStopObserving() {}

    void queueMedia(int pos) {
        PlaylistType.Item item = new PlaylistType.Item();
        item.file = mediaList.get(pos).getMediaSource();
        Log.d(TAG, "Item selected = " + item.file);
        Playlist.Add action = new Playlist.Add(PlaylistType.VIDEO_PLAYLISTID, item);
        action.execute(hostManager.getConnection(), defaultStringActionCallback, callbackHandler);
    }

    void playLocalMediaItem(int pos) {
        PlaylistType.Item item = new PlaylistType.Item();
        item.file = mediaList.get(pos).getMediaSource();
        //Log.d(TAG, "Item selected = " + item.file);
        Player.Open action = new Player.Open(item);
        action.execute(hostManager.getConnection(), defaultStringActionCallback, callbackHandler);
    }

    /**
     * Adapter used to show the hosts in the ListView
     */
    private class LocalMediaListAdapter extends BaseAdapter
            implements ListAdapter, Filterable {

        private View.OnClickListener searchItemMenuClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final int position = (Integer)v.getTag();
                final PopupMenu popupMenu = new PopupMenu(getActivity(), v);
                popupMenu.getMenuInflater().inflate(R.menu.medialist_item, popupMenu.getMenu());
                popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        switch (item.getItemId()) {
                            case R.id.action_medialist_play_item:
                                playLocalMediaItem(position);
                                return true;
                            case R.id.action_medialist_queue_item:
                                queueMedia(position);
                                return true;
                        }
                        return false;
                    }
                });
                popupMenu.show();
            }
        };

        /**
         * The playlist items
         */
        List<LocalMediaItem> localMediaItems;
        private List <LocalMediaItem> origData;

        int artWidth = getResources().getDimensionPixelSize(R.dimen.playlist_art_width);
        int artHeight = getResources().getDimensionPixelSize(R.dimen.playlist_art_heigth);

        int cardBackgroundColor, selectedCardBackgroundColor;

        public LocalMediaListAdapter(List<LocalMediaItem> items) {
            super();
            this.localMediaItems = items;
            this.origData = items;

            Resources.Theme theme = getActivity().getTheme();
            TypedArray styledAttributes = theme.obtainStyledAttributes(new int[] {
                    R.attr.appCardBackgroundColor,
                    R.attr.appSelectedCardBackgroundColor});
            cardBackgroundColor = styledAttributes.getColor(0, R.color.dark_content_background);
            selectedCardBackgroundColor = styledAttributes.getColor(1, R.color.dark_selected_content_background);
            styledAttributes.recycle();
        }

        public LocalMediaListAdapter() {
            this(null);
        }

        /**
         * Manually set the items on the adapter
         * Calls notifyDataSetChanged()
         *
         * @param items Items
         */
        public void setLocalMediaListItems(List<LocalMediaItem> items) {
            this.localMediaItems = items;
            this.origData = items;
            notifyDataSetChanged();
        }

        /**
         * get the list of videos
         * @return playlistItems
         */
        public List<LocalMediaItem> getLocalMediaListItems() {
            return this.localMediaItems;
        }

        @Override
        public int getCount() {
            if (localMediaItems == null) {
                return 0;
            } else {
                return localMediaItems.size();
            }
        }

        @Override
        public LocalMediaItem getItem(int position) {
            if (localMediaItems == null) {
                return null;
            } else {
                return localMediaItems.get(position);
            }
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public int getViewTypeCount () {
            return 1;
        }

        @Override
        public Filter getFilter() {
            Filter myFilter =  new Filter() {
                @Override
                protected FilterResults performFiltering(CharSequence cs) {
                    FilterResults results = new FilterResults();
                    //If there's nothing to filter on, return the original data for your list
                    if (cs == null || cs.length() == 0) {
                        results.values = origData;
                        results.count = origData.size();
                    }
                    else  {
                        ArrayList<LocalMediaItem> filterResultsData = new ArrayList<LocalMediaItem>();

                        for(LocalMediaItem s : origData) {
                            //In this loop, you'll filter through originalData and compare each item to charSequence.
                            //If you find a match, add it to your new ArrayList
                            //I'm not sure how you're going to do comparison, so you'll need to fill out this conditional
                            //if (data.getName().toUpperCase().contains(cs.toString().toUpperCase()) ||
                            //    data.getSinger().toUpperCase().contains(cs.toString().toUpperCase())) {
                            //    Log.i(TAG, "data.getName() = " + data.getName() +
                            //            ", data.getSinger() = " + data.getSinger() + ", cs = " + cs.toString());
                            if (Utils.flattenToAscii(s.getTitle()).toUpperCase().contains(cs.toString().toUpperCase())) {
                                Log.i(TAG, "Matched Title: s.getTitle() = " + s.getTitle() + ", cs = " + cs.toString());
                                filterResultsData.add(s);
                            }
                            if (Utils.flattenToAscii(s.getArtist()).toUpperCase().contains(cs.toString().toUpperCase())) {
                                Log.i(TAG, "Matched Artist: s.getArtist() = " + s.getArtist() + ", cs = " + cs.toString());
                                filterResultsData.add(s);
                            }
                        }
                        results.values = filterResultsData;
                        results.count = filterResultsData.size();
                        Log.i(TAG, "filterResultsData.size() = " + filterResultsData.size());
                    }
                    Log.i(TAG, "results.count = " + results.count);
                    return results;
                }

                @Override
                protected void publishResults(CharSequence cs, FilterResults filterResults) {
                    if (filterResults.count == 0) {
                        notifyDataSetInvalidated();
                    }
                    else {
                        localMediaItems = (ArrayList<LocalMediaItem>) filterResults.values;
                        notifyDataSetChanged();
                    }
                }
            };
            return myFilter;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder viewHolder;

            if (convertView == null) {
                convertView = LayoutInflater.from(getActivity())
                        .inflate(R.layout.grid_item_local_media, parent, false);
                // ViewHolder pattern
                viewHolder = new ViewHolder();
                viewHolder.art = (ImageView)convertView.findViewById(R.id.art);
                viewHolder.title = (TextView)convertView.findViewById(R.id.title);
                viewHolder.details = (TextView)convertView.findViewById(R.id.details);
                viewHolder.contextMenu = (ImageView)convertView.findViewById(R.id.list_context_menu);
                viewHolder.duration = (TextView)convertView.findViewById(R.id.duration);
                viewHolder.card = (CardView)convertView.findViewById(R.id.card);

                convertView.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) convertView.getTag();
            }

            final LocalMediaItem item = this.getItem(position);

            String title = item.getTitle();
            String artUrl = item.getThumbnail();
            int duration = item.getRuntime();

            viewHolder.title.setText(title);
            viewHolder.details.setText(item.getAlbum() + ", " + item.getArtist());
            viewHolder.duration.setText((duration > 0) ? UIUtils.formatTime(duration) : "");
            viewHolder.position = position;

            int cardColor = (position == localMediaGridView.getCheckedItemPosition()) ?
                    selectedCardBackgroundColor: cardBackgroundColor;
            viewHolder.card.setCardBackgroundColor(cardColor);

            // If not video, change aspect ration of poster to a square
            boolean isVideo = true;
            artWidth = artHeight;
            if (!isVideo) {
                ViewGroup.LayoutParams layoutParams = viewHolder.art.getLayoutParams();
                layoutParams.width = layoutParams.height;
                viewHolder.art.setLayoutParams(layoutParams);
                artWidth = artHeight;
            }

            loadThumbnailImage(getActivity(), hostManager,
                    artUrl, title,
                    viewHolder.art, artWidth, artHeight);


            // For the popupmenu
            viewHolder.contextMenu.setTag(position);
            viewHolder.contextMenu.setOnClickListener(searchItemMenuClickListener);

            return convertView;
        }

        // this is basically UIUtils.loadImageWithCharacterAvatar that load the thumbnail url to the listview if
        // it is not empty. Implement it here because changing the function signature to include an extra parameter
        // to bypass hostManager is too much trouble later on when the original source code in github changed so
        // much that we have to rebase our branch (playlist_enhancement)
        // DanhDroid -- 20151228
        void loadThumbnailImage(Context context, HostManager hostManager,
                                String imageUrl, String stringAvatar,
                                ImageView imageView,
                                int imageWidth, int imageHeight) {
            CharacterDrawable avatarDrawable = UIUtils.getCharacterAvatar(context, stringAvatar);
            if (TextUtils.isEmpty(imageUrl)) {
                imageView.setImageDrawable(avatarDrawable);
                return;
            }
            hostManager.getPicasso()
                    .load(imageUrl)
                    .placeholder(avatarDrawable)
                    .resize(imageWidth, imageHeight)
                    .centerCrop()
                    .into(imageView);

        }

        private class ViewHolder {
            ImageView art;
            TextView title;
            TextView details;
            ImageView contextMenu;
            TextView duration;
            CardView card;
            int position;
        }
    }

}
