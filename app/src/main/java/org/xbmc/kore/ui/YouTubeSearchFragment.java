package org.xbmc.kore.ui;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.widget.CardView;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AutoCompleteTextView;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.RelativeLayout;
import android.widget.SearchView;
import android.widget.TextView;

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
import org.xbmc.kore.ui.menuitemsearch.MenuItemSearchAction;
import org.xbmc.kore.ui.menuitemsearch.SearchPerformListener;
import org.xbmc.kore.utils.CharacterDrawable;
import org.xbmc.kore.utils.Config;
import org.xbmc.kore.utils.LogUtils;
import org.xbmc.kore.utils.SharedPreferencesUtils;
import org.xbmc.kore.utils.UIUtils;
import org.xbmc.kore.youtube.AsyncVideoListReader;
import org.xbmc.kore.youtube.PerformYouTubeSearchTask;
import org.xbmc.kore.youtube.model.YouTubeVideo;
import org.xbmc.kore.youtube.model.YouTubeVideoDataHandler;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.TimeUnit;
import butterknife.ButterKnife;
import butterknife.InjectView;

/**
 * Created by danh.le on 8/23/15.
 */
public class YouTubeSearchFragment extends Fragment implements
        LoaderManager.LoaderCallbacks<YouTubeVideoDataHandler>,
        PerformYouTubeSearchTask.OnUpdateSearchResultList,
        HostConnectionObserver.PlayerEventsObserver {
    private static final String TAG = YouTubeSearchFragment.class.getSimpleName();
    static final int LOAD_YT_VIDEOLIST = 1001;

    final int myLoaderID = LOAD_YT_VIDEOLIST;
    ListView listView;
    String lastSearchText;
    String prevSearchPageToken, nextSearchPageToken;
    AsyncVideoListReader asyncVideoListReader;
    List<YouTubeVideo> videoList = new ArrayList<YouTubeVideo>();
    Queue<YouTubeVideo> mediaQueue = new LinkedList<>();
    enum PlaylistItemMode { None, InsertToPlaylist }
    YouTubeSearchListAdapter vidSearchListAdapter;
    PlaylistItemMode playlistItemMode = PlaylistItemMode.None;
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
    /**
     * The current active player id
     */
    private int currentActivePlayerId = -1;

    /**
     * Injectable views
     */
    @InjectView(R.id.info_panel) RelativeLayout infoPanel;
    @InjectView(R.id.playlist) GridView ytSearchGridView;
    @InjectView(R.id.info_title) TextView infoTitle;
    @InjectView(R.id.info_message) TextView infoMessage;

    @Override
    public Loader<YouTubeVideoDataHandler> onCreateLoader(int arg0, Bundle arg1) {
        Log.i(TAG, "onCreateLoader: LoaderID = " + myLoaderID + " videoFile = " + Config.getSearchResultFile());
        asyncVideoListReader = new AsyncVideoListReader(YouTubeSearchFragment.this.getActivity(), Config.getSearchResultFile());
        asyncVideoListReader.forceLoad();
        return asyncVideoListReader;
    }

    @Override
    public void onLoadFinished(Loader<YouTubeVideoDataHandler> arg0, YouTubeVideoDataHandler arg1) {
        Log.i(TAG, "onLoadFinished: LoaderID = " + myLoaderID);
        videoList = arg1.getData();
        setSearchListAdapter();
    }

    @Override
    public void onLoaderReset(Loader<YouTubeVideoDataHandler> arg0) {
        vidSearchListAdapter = null;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        hostManager = HostManager.getInstance(getActivity());
        hostConnectionObserver = hostManager.getHostConnectionObserver();
        Log.i(TAG, "YouTubeSearchFragment.onCreate");
    }

    void setSearchListAdapter() {
        vidSearchListAdapter = new YouTubeSearchListAdapter();
        ytSearchGridView.setAdapter(vidSearchListAdapter);

        // When clicking on an item, play it
        ytSearchGridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                // click an item, add it to the playlist
                PlaylistType.Item item = new PlaylistType.Item();
                item.file = videoList.get(position).getWebPlayerUrl();
                queueMedia(item);

            }
        });
        if ((videoList == null) || (videoList.size() == 0)) {
            displayEmptySearchResultMessage();
        }
        else {
            switchToPanel(R.id.youtube_search_list);
            vidSearchListAdapter.setSearchListItems(videoList);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,Bundle savedInstanceState) {
        ViewGroup root = (ViewGroup) inflater.inflate(R.layout.fragment_playlist, container, false);
        ButterKnife.inject(this, root);
        setSearchListAdapter();
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
        lastSearchText = SharedPreferencesUtils.getString(getActivity(), Config.LAST_SEARCH_TEXT, "");
        prevSearchPageToken = SharedPreferencesUtils.getString(getActivity(), Config.PREV_SEARCH_TOKEN, "");
        nextSearchPageToken = SharedPreferencesUtils.getString(getActivity(), Config.NEXT_SEARCH_TOKEN, "");
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
        inflater.inflate(R.menu.youtube, menu);
        /*
        TODO: fix it!
        //MenuItemSearchAction menuItemSearchAction = new MenuItemSearchAction(getActivity(), menu, this);
        MenuItem item = menu.findItem(R.id.action_search);

        android.support.v7.widget.SearchView sv = new android.support.v7.widget.SearchView((getActivity()).getActionBar().getThemedContext());
        MenuItemCompat.setShowAsAction(item, MenuItemCompat.SHOW_AS_ACTION_COLLAPSE_ACTION_VIEW | MenuItemCompat.SHOW_AS_ACTION_IF_ROOM);
        MenuItemCompat.setActionView(item, sv);
        sv.setOnQueryTextListener(new android.support.v7.widget.SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                Log.i(TAG, "search query submit");
                return onQueryTextSubmit(query);
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                Log.i(TAG, "tap");
                return false;
            }
        });
        */
        super.onCreateOptionsMenu(menu, inflater);
    }

    // The following callbacks are called for the SearchView.OnQueryChangeListener
    public boolean onQueryTextChange(String newText) {
        //newText = newText.isEmpty() ? "" : "Query so far: " + newText;
        //mSearchText.setText(newText);
        //mSearchText.setTextColor(Color.GREEN);
        Log.i(TAG, "onQueryTextChange: Query so far: " + newText);
        return true;
    }

    boolean onQueryTextSubmit(String query) {
        //Toast.makeText(this, "Searching for: " + query + "...", Toast.LENGTH_SHORT).show();
        //mSearchText.setText("Searching for: " + query + "...");
        //mSearchText.setTextColor(Color.RED);
        Log.i(TAG, "onQueryTextSubmit: query:" + query);
        if (query.length() > 0) {
            // create an async task to search
            nextSearchPageToken = null;
            prevSearchPageToken = null;
            lastSearchText = query;
            performYouTubeSearch(lastSearchText, nextSearchPageToken);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        final MenuItem i = item;
        switch (item.getItemId()) {
            case R.id.action_search: {

                /*
                item.setActionView(R.layout.search_param);
                List<String> list = Config.getInstance().getYTSearchTextHistory();
                String history[] = new String[list.size()];
                history = list.toArray(history);
                final AutoCompleteTextView txtSearch = (AutoCompleteTextView) item.getActionView().findViewById(R.id.search_edit_text);
                final PopUpListAdapter adapter = new PopUpListAdapter(getActivity(), android.R.layout.simple_list_item_1, history);
                txtSearch.setAdapter(adapter);

                txtSearch.addTextChangedListener(new TextWatcher() {

                    @Override
                    public void onTextChanged(CharSequence cs, int arg1, int arg2, int arg3) {
                        // When user changed the Text
                        adapter.getFilter().filter(cs);
                    }

                    @Override
                    public void beforeTextChanged(CharSequence arg0, int arg1, int arg2,
                                                  int arg3) {
                        // TODO Auto-generated method stub

                    }

                    @Override
                    public void afterTextChanged(Editable arg0) {
                        // TODO Auto-generated method stub
                    }
                });
                //txtSearch.setText(lastSearchText);
                txtSearch.setTextColor(Color.BLACK);
                txtSearch.requestFocus();
                // Setting an action listener
                txtSearch.setOnEditorActionListener(new TextView.OnEditorActionListener() {
                    @Override
                    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                        Log.i(TAG, "onEditorAction:");
                        if ((actionId == EditorInfo.IME_ACTION_SEARCH) || (event.getAction() == KeyEvent.ACTION_DOWN &&
                                event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                            ((InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(
                                    txtSearch.getWindowToken(), 0);
                            i.collapseActionView();
                            lastSearchText = v.getText().toString();
                            if (lastSearchText.length() > 0) {
                                // create an async task to search
                                nextSearchPageToken = null;
                                prevSearchPageToken = null;
                                performYouTubeSearch(lastSearchText, nextSearchPageToken);
                            }
                            //listAdapter.notifyDataSetChanged();
                            return true;
                        } else {
                            return false;
                        }
                    }

                });
                InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.toggleSoftInput(InputMethodManager.SHOW_FORCED,0);
                //nextSearchPageToken = null;
                //prevSearchPageToken = null;
                //lastSearchText = "Quang Le";
                //performYouTubeSearch(lastSearchText, nextSearchPageToken);
                */
            }
            break;

            default:
                break;
        }

        return super.onOptionsItemSelected(item);
    }
/*
    @Override
    public void performSearch(String query) {
        Log.i(TAG, "performSearch: query string = " + query);
        if (query.length() > 0) {
            // create an async task to search
            nextSearchPageToken = null;
            prevSearchPageToken = null;
            lastSearchText = query;
            performYouTubeSearch(lastSearchText, nextSearchPageToken);
        }
    }
*/
    @Override
    public void updateSearchResultList(String searchText, String prevPgToken, String nextPgToken) {
        prevSearchPageToken = prevPgToken;
        nextSearchPageToken = nextPgToken;
        lastSearchText = searchText;
        Config.getInstance().setYTSearchTextParams(getActivity(), prevSearchPageToken, nextSearchPageToken, lastSearchText);
        if (asyncVideoListReader == null) {
            Log.i(TAG, "Starting new load");

            asyncVideoListReader = new AsyncVideoListReader(YouTubeSearchFragment.this.getActivity(), Config.getSearchResultFile());
            asyncVideoListReader.forceLoad();

        }
        else {
            Log.i(TAG, "Restarting load");
            // set new dbFileName
            asyncVideoListReader.setNewVideoListFileName(Config.getSearchResultFile());
        }
        getActivity().getSupportLoaderManager().restartLoader(myLoaderID, null, this);
    }

    /**
     * Switches the info panel shown (they are exclusive)
     * @param panelResId The panel to show
     */
    private void switchToPanel(int panelResId) {
        switch (panelResId) {
            case R.id.info_panel:
                infoPanel.setVisibility(View.VISIBLE);
                ytSearchGridView.setVisibility(View.GONE);
                break;
            case R.id.youtube_search_list:
                infoPanel.setVisibility(View.GONE);
                ytSearchGridView.setVisibility(View.VISIBLE);
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
        infoTitle.setText(R.string.search_result_empty);
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
     * Callback for clear and insert items to playlist
     */
    private ApiCallback<String> StringActionCallback = new ApiCallback<String>() {
        @Override
        public void onSuccess(String result) {
            // successfully insert one item into the playlist
            if (playlistItemMode == PlaylistItemMode.InsertToPlaylist) {
                YouTubeVideo v = mediaQueue.poll();
                if (v != null) {
                    PlaylistType.Item item = new PlaylistType.Item();
                    item.file = v.getWebPlayerUrl();
                    queueMedia(item);
                }
                else {
                    playlistItemMode = PlaylistItemMode.None;
                }
            }
        }

        @Override
        public void onError(int errorCode, String description) {
            // got an error, abort everything
            LogUtils.LOGE(TAG, "Callback error, code = " + errorCode + ", description = " + description);
            // TODO: need to deal with error during clear and insert item to playlist
        }
    };
    private void performYouTubeSearch(String searchTerm, String nextPageToken) {
        final PerformYouTubeSearchTask ytSearchTask = new PerformYouTubeSearchTask(getActivity(), this, Config.getSearchResultFile(),
                Config.getInstance().getYouTubeMediaSearchMode());
        String[] searchParam = new String[3];
        searchParam[0] = searchTerm;
        searchParam[1] = nextPageToken;
        searchParam[2] = getResources().getString(R.string.app_name);
        ytSearchTask.execute(searchParam);

        //setting timeout thread for async task
        Thread timerThread = new Thread(){
            public void run(){
                try {
                    ytSearchTask.get(60000, TimeUnit.MILLISECONDS);  //set time in millisecond(in this timeout is 90 seconds

                } catch (Exception e) {
                    ytSearchTask.cancel(true);
                    ytSearchTask.cancelDialog();
                    getActivity().runOnUiThread(new Runnable() {
                        public void run() {
                            // send failed -- should add some notification here
                            UIUtils.errorMesgBox(getActivity(), "Search YouTube Error",
                                    "Timeout error during searching YouTube");
                        }
                    });

                }
            }
        };
        timerThread.start();
    }

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

    void queueMedia(PlaylistType.Item item) {
        Log.d(TAG, "Item selected = " + item.file);
        Playlist.Add action = new Playlist.Add(PlaylistType.VIDEO_PLAYLISTID, item);
        action.execute(hostManager.getConnection(), StringActionCallback, callbackHandler);
    }

    void playYouTubeItem(int pos) {
        PlaylistType.Item item = new PlaylistType.Item();
        item.file = videoList.get(pos).getWebPlayerUrl();
        Log.d(TAG, "Item selected = " + item.file);
        Player.Open action = new Player.Open(item);
        action.execute(hostManager.getConnection(), StringActionCallback, callbackHandler);
    }

    /**
     * Adapter used to show the hosts in the ListView
     */
    private class YouTubeSearchListAdapter extends BaseAdapter
            implements ListAdapter {
        private View.OnClickListener searchItemMenuClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final int position = (Integer)v.getTag();
                final PopupMenu popupMenu = new PopupMenu(getActivity(), v);
                popupMenu.getMenuInflater().inflate(R.menu.youtube_item, popupMenu.getMenu());
                popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        switch (item.getItemId()) {
                            case R.id.action_play_item:
                                playYouTubeItem(position);
                                return true;
                            case R.id.action_queue_from_here:
                                mediaQueue.clear();
                                playlistItemMode = PlaylistItemMode.InsertToPlaylist;
                                int loc = position;
                                YouTubeVideo v;
                                for (int i = position + 1; i < videoList.size(); i++) {
                                    v = videoList.get(i);
                                    mediaQueue.add(v);
                                }
                                // start playing the selected one, then queue the rest make sure to queue
                                // the selected on last so the it does not lose its place in the queue
                                mediaQueue.add(videoList.get(loc));
                                playYouTubeItem(loc);
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
        List<YouTubeVideo> youtubeItems;
        int artWidth = getResources().getDimensionPixelSize(R.dimen.playlist_art_width);
        int artHeight = getResources().getDimensionPixelSize(R.dimen.playlist_art_heigth);

        int cardBackgroundColor, selectedCardBackgroundColor;

        public YouTubeSearchListAdapter(List<YouTubeVideo> items) {
            super();
            this.youtubeItems = items;

            Resources.Theme theme = getActivity().getTheme();
            TypedArray styledAttributes = theme.obtainStyledAttributes(new int[] {
                    R.attr.appCardBackgroundColor,
                    R.attr.appSelectedCardBackgroundColor});
            cardBackgroundColor = styledAttributes.getColor(0, R.color.dark_content_background);
            selectedCardBackgroundColor = styledAttributes.getColor(1, R.color.dark_selected_content_background);
            styledAttributes.recycle();
        }

        public YouTubeSearchListAdapter() {
            this(null);
        }

        /**
         * Manually set the items on the adapter
         * Calls notifyDataSetChanged()
         *
         * @param items Items
         */
        public void setSearchListItems(List<YouTubeVideo> items) {
            this.youtubeItems = items;
            notifyDataSetChanged();
        }

        /**
         * get the list of videos
         * @return playlistItems
         */
        public List<YouTubeVideo> getSearchListItems() {
            return this.youtubeItems;
        }

        @Override
        public int getCount() {
            if (youtubeItems == null) {
                return 0;
            } else {
                return youtubeItems.size();
            }
        }

        @Override
        public YouTubeVideo getItem(int position) {
            if (youtubeItems == null) {
                return null;
            } else {
                return youtubeItems.get(position);
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
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder viewHolder;

            if (convertView == null) {
                convertView = LayoutInflater.from(getActivity())
                        .inflate(R.layout.grid_item_playlist, parent, false);
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

            final YouTubeVideo item = this.getItem(position);

            String title = item.getTitle();
            String artUrl = item.getThumbnails().get(Config.getInstance().getDefaultThumbSize());
            String details = item.getUploader();
            int duration = (int) item.getDuration();

            viewHolder.title.setText(title);
            viewHolder.details.setText(details);
            viewHolder.duration.setText((duration > 0) ? UIUtils.formatTime(duration) : "");
            viewHolder.position = position;

            int cardColor = (position == ytSearchGridView.getCheckedItemPosition()) ?
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
