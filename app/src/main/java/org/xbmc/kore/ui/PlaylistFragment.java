/*
 * Copyright 2015 Synced Synapse. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.xbmc.kore.ui;

import android.app.Dialog;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v7.widget.CardView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.PopupMenu;
import android.widget.RelativeLayout;
import android.widget.TextView;

import org.xbmc.kore.R;
import org.xbmc.kore.host.HostConnectionObserver;
import org.xbmc.kore.host.HostConnectionObserver.PlayerEventsObserver;
import org.xbmc.kore.host.HostInfo;
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
import org.xbmc.kore.model.MediaPlayList;
import org.xbmc.kore.utils.Config;
import org.xbmc.kore.utils.FileUtils;
import org.xbmc.kore.utils.LogUtils;
import org.xbmc.kore.utils.UIUtils;
import org.xbmc.kore.utils.Utils;

import java.io.File;
import java.util.List;

import ar.com.daidalos.afiledialog.FileChooserDialog;
import butterknife.ButterKnife;
import butterknife.InjectView;

/**
 * Playlist view
 */
public class PlaylistFragment extends Fragment
        implements PlayerEventsObserver {
    private static final String TAG = LogUtils.makeLogTag(PlaylistFragment.class);

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
     * Current playlist
     */
    private int currentPlaylistId = -1;

    /**
     * Playlist adapter
     */
    private PlayListAdapter playListAdapter;


    enum PlaylistItemMode { None, ClearPlaylist, InsertToPlaylist }
    PlaylistItemMode playlistItemMode = PlaylistItemMode.None;
    MediaPlayList playlistToInsert = null;
    int curPlaylistItem = 0;
    enum FileSelectionMode { None, FileLoad, FileSave}
    FileSelectionMode fileSelectionMode = FileSelectionMode.None;

    /**
     * Injectable views
     */
    @InjectView(R.id.info_panel) RelativeLayout infoPanel;
    @InjectView(R.id.playlist) GridView playlistGridView;

    @InjectView(R.id.info_title) TextView infoTitle;
    @InjectView(R.id.info_message) TextView infoMessage;

//    @InjectView(R.id.play) ImageButton playButton;
//    @InjectView(R.id.stop) ImageButton stopButton;
//    @InjectView(R.id.previous) ImageButton previousButton;
//    @InjectView(R.id.next) ImageButton nextButton;
//    @InjectView(R.id.rewind) ImageButton rewindButton;
//    @InjectView(R.id.fast_forward) ImageButton fastForwardButton;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        hostManager = HostManager.getInstance(getActivity());
        hostConnectionObserver = hostManager.getHostConnectionObserver();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        ViewGroup root = (ViewGroup) inflater.inflate(R.layout.fragment_playlist, container, false);
        ButterKnife.inject(this, root);

        playListAdapter = new PlayListAdapter();
        playlistGridView.setAdapter(playListAdapter);

        // When clicking on an item, play it
        playlistGridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Player.Open action = new Player.Open(Player.Open.TYPE_PLAYLIST, currentPlaylistId, position);
                action.execute(hostManager.getConnection(), defaultStringActionCallback, callbackHandler);
            }
        });

//        // Pad main content view to overlap bottom system bar
//        UIUtils.setPaddingForSystemBars(getActivity(), playlistGridView, false, false, true);
//        playlistGridView.setClipToPadding(false);

        return root;
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
        inflater.inflate(R.menu.playlist, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_clear_playlist: {
                Playlist.Clear action = new Playlist.Clear(currentPlaylistId);
                action.execute(hostManager.getConnection(), defaultStringActionCallback, callbackHandler);
                // If we are playing something, refresh playlist
                forceRefreshPlaylist();
                }
                break;
            case R.id.action_load_music_pl:
                getPlaylistItems(PlaylistType.MUSIC_PLAYLISTID, false, lastGetItemResult);
                break;
            case R.id.action_load_video_pl:
                getPlaylistItems(PlaylistType.VIDEO_PLAYLISTID, false, lastGetItemResult);
                break;
            case R.id.action_save_current_pl:
                if ((currentPlaylistId != -1) && (playListAdapter.getCount() > 0)) {
                    fileSelectionMode = FileSelectionMode.FileSave;
                    FileChooserDialog dialog = new FileChooserDialog(getActivity());
                    // Assign listener for the select event.
                    dialog.addListener(PlaylistFragment.this.onFileSelectedListener);
                    // Define start folder.
                    dialog.loadFolder(Config.getPlaylistDirectory());
                    // Activate the button for create files.
                    dialog.setCanCreateFiles(true);
                    dialog.setShowConfirmation(true, true);
                    dialog.show();
                    // save will be performed in dialog callback
                }
                break;
            case R.id.action_load_pl_from_file: {
                fileSelectionMode = FileSelectionMode.FileLoad;
                FileChooserDialog dialog = new FileChooserDialog(getActivity());
                // Assign listener for the select event.
                dialog.addListener(PlaylistFragment.this.onFileSelectedListener);
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
            final String playlistFileName = file.getName();
            if (fileSelectionMode == FileSelectionMode.FileLoad) {
                String playlistJSON = FileUtils.readFromFile(file);
                playlistToInsert = Config.getGson().fromJson(playlistJSON, MediaPlayList.class);
                // If the selected playlist is not empty, clear the playlist, then push all the playlist item to the
                // new playlist
                if (playlistToInsert.playlist.size() > 0) {
                    // first clear the requested playlistID
                    playlistItemMode = PlaylistItemMode.ClearPlaylist;
                    curPlaylistItem = 0;
                    Playlist.Clear action = new Playlist.Clear(playlistToInsert.playlistType);
                    action.execute(hostManager.getConnection(), playlistStringActionCallback, callbackHandler);
                    // now when the callback for clear playlist comeback successfully, we go ahead and insert the
                    // playlist items to kodi playlist one by one
                }

            }
            else if (fileSelectionMode == FileSelectionMode.FileSave) {
                // save playlist
                MediaPlayList mediaPlaylist = new MediaPlayList(currentPlaylistId, playListAdapter.getPlaylistItems());
                final String playlistGson =  Config.getGson().toJson(mediaPlaylist);
                FileUtils.savePlaylistToFile(file, playlistGson);
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
                MediaPlayList mediaPlaylist = new MediaPlayList(currentPlaylistId, playListAdapter.getPlaylistItems());
                final String playlistGson =  Config.getGson().toJson(mediaPlaylist);
                FileUtils.savePlaylistToFile(newFile, playlistGson);
            }
        }
    };

//    @Override
//    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
//        super.onCreateContextMenu(menu, v, menuInfo);
//        // Add the options
//        menu.add(0, CONTEXT_MENU_REMOVE_ITEM, 1, R.string.remove);
//    }
//
//    @Override
//    public boolean onContextItemSelected(android.view.MenuItem item) {
//        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
//
//        switch (item.getItemId()) {
//            case CONTEXT_MENU_REMOVE_ITEM:
//                // Remove this item from the playlist
//                Playlist.Remove action = new Playlist.Remove(currentPlaylistId, info.position);
//                action.execute(hostManager.getConnection(), defaultStringActionCallback, callbackHandler);
//                forceRefreshPlaylist();
//                return true;
//        }
//        return super.onContextItemSelected(item);
//    }

    void forceRefreshPlaylist() {
        // If we are playing something, refresh playlist
        //if ((lastCallResult == PLAYER_IS_PLAYING) || (lastCallResult == PLAYER_IS_PAUSED)) {
            setupPlaylistInfo(lastGetActivePlayerResult, lastGetPropertiesResult, lastGetItemResult);
        //}
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
    private ApiCallback<String> playlistStringActionCallback = new ApiCallback<String>() {
        @Override
        public void onSuccess(String result) {
            if (playlistItemMode == PlaylistItemMode.ClearPlaylist) {
                // clear playlist is successful, now insert the first item from our playlist to kodi playlist
                playlistItemMode = PlaylistItemMode.InsertToPlaylist;       // advance to next state
                PlaylistType.Item item = new PlaylistType.Item();
                item.file = playlistToInsert.playlist.get(curPlaylistItem).file;
                curPlaylistItem++;
                Playlist.Add action = new Playlist.Add(playlistToInsert.playlistType, item);
                action.execute(hostManager.getConnection(), playlistStringActionCallback, callbackHandler);

            }
            else if (playlistItemMode == PlaylistItemMode.InsertToPlaylist) {
                // insert the next one until the end of the list
                if (curPlaylistItem < playlistToInsert.playlist.size()) {
                    PlaylistType.Item item = new PlaylistType.Item();
                    item.file = playlistToInsert.playlist.get(curPlaylistItem).file;
                    curPlaylistItem++;
                    Playlist.Add action = new Playlist.Add(playlistToInsert.playlistType, item);
                    action.execute(hostManager.getConnection(), playlistStringActionCallback, callbackHandler);

                }
                else {
                    // done inserting all the items from requested playlist
                    currentPlaylistId = playlistToInsert.playlistType;
                    playlistToInsert = null;
                    curPlaylistItem = 0;
                    // start playing the first item in the playlist
                    Player.Open action = new Player.Open(Player.Open.TYPE_PLAYLIST, currentPlaylistId, 0);
                    action.execute(hostManager.getConnection(), defaultStringActionCallback, callbackHandler);
                    // update the playlist
                    forceRefreshPlaylist();
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

//    /**
//     * Callback for methods that change the play speed
//     */
//    private ApiCallback<Integer> defaultPlaySpeedChangedCallback = new ApiCallback<Integer>() {
//        @Override
//        public void onSuccess(Integer result) {
//            UIUtils.setPlayPauseButtonIcon(getActivity(), playButton, result);
//        }
//
//        @Override
//        public void onError(int errorCode, String description) { }
//    };
//
//    /**
//     * Callbacks for bottom button bar
//     */
//    @OnClick(R.id.play)
//    public void onPlayClicked(View v) {
//        Player.PlayPause action = new Player.PlayPause(currentActivePlayerId);
//        action.execute(hostManager.getConnection(), defaultPlaySpeedChangedCallback, callbackHandler);
//    }
//
//    @OnClick(R.id.stop)
//    public void onStopClicked(View v) {
//        Player.Stop action = new Player.Stop(currentActivePlayerId);
//        action.execute(hostManager.getConnection(), defaultStringActionCallback, callbackHandler);
//        UIUtils.setPlayPauseButtonIcon(getActivity(), playButton, 0);
//    }
//
//    @OnClick(R.id.fast_forward)
//    public void onFastForwardClicked(View v) {
//        Player.SetSpeed action = new Player.SetSpeed(currentActivePlayerId, GlobalType.IncrementDecrement.INCREMENT);
//        action.execute(hostManager.getConnection(), defaultPlaySpeedChangedCallback, callbackHandler);
//    }
//
//    @OnClick(R.id.rewind)
//    public void onRewindClicked(View v) {
//        Player.SetSpeed action = new Player.SetSpeed(currentActivePlayerId, GlobalType.IncrementDecrement.DECREMENT);
//        action.execute(hostManager.getConnection(), defaultPlaySpeedChangedCallback, callbackHandler);
//    }
//
//    @OnClick(R.id.previous)
//    public void onPreviousClicked(View v) {
//        Player.GoTo action = new Player.GoTo(currentActivePlayerId, Player.GoTo.PREVIOUS);
//        action.execute(hostManager.getConnection(), defaultStringActionCallback, callbackHandler);
//    }
//
//    @OnClick(R.id.next)
//    public void onNextClicked(View v) {
//        Player.GoTo action = new Player.GoTo(currentActivePlayerId, Player.GoTo.NEXT);
//        action.execute(hostManager.getConnection(), defaultStringActionCallback, callbackHandler);
//    }

    /**
     * Last call results
     */
    private int lastCallResult = PlayerEventsObserver.PLAYER_NO_RESULT;
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
        if ((lastGetPlaylistItemsResult == null) ||
                (lastCallResult != PlayerEventsObserver.PLAYER_IS_PLAYING) ||
                (currentActivePlayerId != getActivePlayerResult.playerid) ||
                (lastGetItemResult.id != getItemResult.id)) {
            // Check if something is different, and only if so, start the chain calls
            setupPlaylistInfo(getActivePlayerResult, getPropertiesResult, getItemResult);
            currentActivePlayerId = getActivePlayerResult.playerid;
        } else {
            // Hopefully nothing changed, so just use the last results
            displayPlaylist(getItemResult, lastGetPlaylistItemsResult);
        }
        // Switch icon
//        UIUtils.setPlayPauseButtonIcon(getActivity(), playButton, getPropertiesResult.speed);

        // Save results
        lastCallResult = PLAYER_IS_PLAYING;
        lastGetActivePlayerResult = getActivePlayerResult;
        lastGetPropertiesResult = getPropertiesResult;
        lastGetItemResult = getItemResult;
        switchToFullScreen();
    }

    public void playerOnPause(PlayerType.GetActivePlayersReturnType getActivePlayerResult,
                              PlayerType.PropertyValue getPropertiesResult,
                              ListType.ItemsAll getItemResult) {
        if ((lastGetPlaylistItemsResult == null) ||
                (lastCallResult != PlayerEventsObserver.PLAYER_IS_PLAYING) ||
                (currentActivePlayerId != getActivePlayerResult.playerid) ||
                (lastGetItemResult.id != getItemResult.id)) {
            setupPlaylistInfo(getActivePlayerResult, getPropertiesResult, getItemResult);
            currentActivePlayerId = getActivePlayerResult.playerid;
        } else {
            // Hopefully nothing changed, so just use the last results
            displayPlaylist(getItemResult, lastGetPlaylistItemsResult);
        }
        // Switch icon
//        UIUtils.setPlayPauseButtonIcon(getActivity(), playButton, getPropertiesResult.speed);

        lastCallResult = PLAYER_IS_PAUSED;
        lastGetActivePlayerResult = getActivePlayerResult;
        lastGetPropertiesResult = getPropertiesResult;
        lastGetItemResult = getItemResult;
    }

    public void playerOnStop() {
        //Log.d(TAG, "playerOnStop(): currentPlaylistId = " + currentPlaylistId);
        // 20150809 - DanhDroid: What we want to do is if currentPlaylistId == -1, check to see
        // if the player is playing anything. If it is NOT PLAYING, read the Music and Video Playlist
        // then display one of them
        // If both are empty, then displayEmptyPlaylistMessage(). If the player is playing, do not
        // do anything
        getPlaylistItems((currentPlaylistId == -1) ? PlaylistType.MUSIC_PLAYLISTID : currentPlaylistId, true, lastGetItemResult);
        lastCallResult = PLAYER_IS_STOPPED;
    }

    public void playerOnConnectionError(int errorCode, String description) {
        HostInfo hostInfo = hostManager.getHostInfo();

        switchToPanel(R.id.info_panel);
        if (hostInfo != null) {
            infoTitle.setText(R.string.connecting);
            // TODO: check error code
            infoMessage.setText(String.format(getString(R.string.connecting_to), hostInfo.getName(), hostInfo.getAddress()));
        } else {
            infoTitle.setText(R.string.no_xbmc_configured);
            infoMessage.setText(null);
        }

        lastCallResult = PlayerEventsObserver.PLAYER_CONNECTION_ERROR;
    }

    public void playerNoResultsYet() {
        // Initialize info panel
        switchToPanel(R.id.info_panel);
        HostInfo hostInfo = hostManager.getHostInfo();
        if (hostInfo != null) {
            infoTitle.setText(R.string.connecting);
        } else {
            infoTitle.setText(R.string.no_xbmc_configured);
        }
        infoMessage.setText(null);
        lastCallResult = PlayerEventsObserver.PLAYER_NO_RESULT;
    }

    public void systemOnQuit() {
        playerNoResultsYet();
    }

    // Ignore this
    public void inputOnInputRequested(String title, String type, String value) {}
    public void observerOnStopObserving() {}

    /**
     * Starts the call chain to display the playlist
     */
    private void setupPlaylistInfo(final PlayerType.GetActivePlayersReturnType getActivePlayerResult,
                                   final PlayerType.PropertyValue getPropertiesResult,
                                   final ListType.ItemsAll getItemResult) {
        if (getPropertiesResult != null) {
            currentPlaylistId = getPropertiesResult.playlistid;
        }
        if (currentPlaylistId == -1) {
            // Couldn't find a playlist of the same type, just report empty
            displayEmptyPlaylistMessage();
        } else {
            getPlaylistItems(currentPlaylistId, false, getItemResult);
        }
    }

    private void getPlaylistItems(final int playlistID, boolean searchForNonEmptyList, final ListType.ItemsAll getItemResult) {
        // Call GetItems
        String[] propertiesToGet = new String[] {
                ListType.FieldsAll.ART,
                ListType.FieldsAll.ARTIST,
                ListType.FieldsAll.ALBUMARTIST,
                ListType.FieldsAll.ALBUM,
                ListType.FieldsAll.DISPLAYARTIST,
                ListType.FieldsAll.EPISODE,
                ListType.FieldsAll.FANART,
                ListType.FieldsAll.FILE,
                ListType.FieldsAll.SEASON,
                ListType.FieldsAll.SHOWTITLE,
                ListType.FieldsAll.STUDIO,
                ListType.FieldsAll.TAGLINE,
                ListType.FieldsAll.THUMBNAIL,
                ListType.FieldsAll.TITLE,
                ListType.FieldsAll.TRACK,
                ListType.FieldsAll.DURATION,
                ListType.FieldsAll.RUNTIME,
        };
        Playlist.GetItems getItems = new Playlist.GetItems(playlistID, propertiesToGet);
        getItems.execute(hostManager.getConnection(), new ApiCallback<List<ListType.ItemsAll>>() {
            @Override
            public void onSuccess(List<ListType.ItemsAll> result) {
                if (!isAdded()) return;
                // Ok, we've got all the info, save and display playlist
                lastGetPlaylistItemsResult = result;
                currentPlaylistId = playlistID;     // successful, keep this as our playlistID
                displayPlaylist(getItemResult, result);
            }

            @Override
            public void onError(int errorCode, String description) {
                if (!isAdded()) return;
                // Oops
                displayErrorGettingPlaylistMessage(description);
            }
        }, callbackHandler);
    }

    private void displayPlaylist(final ListType.ItemsAll getItemResult,
                                 final List<ListType.ItemsAll> playlistItems) {
        if (playlistItems.size() == 0) {
            displayEmptyPlaylistMessage();
            return;
        }
        switchToPanel(R.id.playlist);

        // Set items, which call notifyDataSetChanged
        playListAdapter.setPlaylistItems(playlistItems);
        // Present the checked item
        for (int i = 0; i < playlistItems.size(); i++) {
            if (getItemResult != null) {
                if ((playlistItems.get(i).id == getItemResult.id) &&
                        (playlistItems.get(i).type.equals(getItemResult.type))) {
                    playlistGridView.setItemChecked(i, true);
                    playlistGridView.setSelection(i);
                }
            }
        }
    }

    /**
     * Switches the info panel shown (they are exclusive)
     * @param panelResId The panel to show
     */
    private void switchToPanel(int panelResId) {
        switch (panelResId) {
            case R.id.info_panel:
                infoPanel.setVisibility(View.VISIBLE);
                playlistGridView.setVisibility(View.GONE);
                break;
            case R.id.playlist:
                infoPanel.setVisibility(View.GONE);
                playlistGridView.setVisibility(View.VISIBLE);
                break;
        }
    }

    /**
     * Displays an error on the info panel
     * @param details Details message
     */
    private void displayErrorGettingPlaylistMessage(String details) {
        switchToPanel(R.id.info_panel);
        infoTitle.setText(R.string.error_getting_playlist);
        infoMessage.setText(String.format(getString(R.string.error_message), details));
    }

    /**
     * Displays empty playlist
     */
    private void displayEmptyPlaylistMessage() {
        switchToPanel(R.id.info_panel);
        infoTitle.setText(R.string.playlist_empty);
        infoMessage.setText(null);
    }

    /**
     * Adapter used to show the hosts in the ListView
     */
    private class PlayListAdapter extends BaseAdapter
            implements ListAdapter {
        private View.OnClickListener playlistItemMenuClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final int position = (Integer)v.getTag();
                final PopupMenu popupMenu = new PopupMenu(getActivity(), v);
                popupMenu.getMenuInflater().inflate(R.menu.playlist_item, popupMenu.getMenu());
                popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        switch (item.getItemId()) {
                            case R.id.action_remove_playlist_item:
                                // Remove this item from the playlist
                                Playlist.Remove action = new Playlist.Remove(currentPlaylistId, position);
                                action.execute(hostManager.getConnection(), defaultStringActionCallback, callbackHandler);
                                forceRefreshPlaylist();
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
        List<ListType.ItemsAll> playlistItems;
        int artWidth = getResources().getDimensionPixelSize(R.dimen.playlist_art_width);
        int artHeight = getResources().getDimensionPixelSize(R.dimen.playlist_art_heigth);

        int cardBackgroundColor, selectedCardBackgroundColor;

        public PlayListAdapter(List<ListType.ItemsAll> playlistItems) {
            super();
            this.playlistItems = playlistItems;

            Resources.Theme theme = getActivity().getTheme();
            TypedArray styledAttributes = theme.obtainStyledAttributes(new int[] {
                    R.attr.appCardBackgroundColor,
                    R.attr.appSelectedCardBackgroundColor});
            cardBackgroundColor = styledAttributes.getColor(0,
                    getResources().getColor(R.color.dark_content_background));
            selectedCardBackgroundColor = styledAttributes.getColor(1,
                    getResources().getColor(R.color.dark_selected_content_background));
            styledAttributes.recycle();
        }

        public PlayListAdapter() {
            this(null);
        }

        /**
         * Manually set the items on the adapter
         * Calls notifyDataSetChanged()
         *
         * @param playlistItems Items
         */
        public void setPlaylistItems(List<ListType.ItemsAll> playlistItems) {
            this.playlistItems = playlistItems;
            notifyDataSetChanged();
        }

        /**
         * get the playlist
         * @return playlistItems
         */
        public List<ListType.ItemsAll> getPlaylistItems() {
            return this.playlistItems;
        }

        @Override
        public int getCount() {
            if (playlistItems == null) {
                return 0;
            } else {
                return playlistItems.size();
            }
        }

        @Override
        public ListType.ItemsAll getItem(int position) {
            if (playlistItems == null) {
                return null;
            } else {
                return playlistItems.get(position);
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

            final ListType.ItemsAll item = this.getItem(position);

            // Differentiate between media
            String title, details, artUrl;
            int duration;
            switch (item.type) {
                case ListType.ItemsAll.TYPE_MOVIE:
                    title = item.title;
                    details = item.tagline;
                    artUrl = item.thumbnail;
                    duration = item.runtime;
                    break;
                case ListType.ItemsAll.TYPE_EPISODE:
                    title = item.title;
                    String season = String.format(getString(R.string.season_episode_abbrev), item.season, item.episode);
                    details = String.format("%s | %s", item.showtitle, season);
                    artUrl = item.art.poster;
                    duration = item.runtime;
                    break;
                case ListType.ItemsAll.TYPE_SONG:
                    title = item.title;
                    details = item.displayartist + " | " + item.album;
                    artUrl = item.thumbnail;
                    duration = item.duration;
                    break;
                case ListType.ItemsAll.TYPE_MUSIC_VIDEO:
                    title = item.title;
                    details = Utils.listStringConcat(item.artist, ", ") + " | " + item.album;
                    artUrl = item.thumbnail;
                    duration = item.runtime;
                    break;
                default:
                    // Don't yet recognize this type
                    title = TextUtils.isEmpty(item.label)? item.file : item.label;
                    details = item.type;
                    artUrl = item.thumbnail;
                    duration = item.runtime;
                    break;
            }

            viewHolder.title.setText(title);
            viewHolder.details.setText(details);
            viewHolder.duration.setText((duration > 0) ? UIUtils.formatTime(duration) : "");
            viewHolder.position = position;

            int cardColor = (position == playlistGridView.getCheckedItemPosition()) ?
                    selectedCardBackgroundColor: cardBackgroundColor;
            viewHolder.card.setCardBackgroundColor(cardColor);

            // If not video, change aspect ration of poster to a square
            boolean isVideo = (item.type.equals(ListType.ItemsAll.TYPE_MOVIE)) ||
                    (item.type.equals(ListType.ItemsAll.TYPE_EPISODE));
            if (!isVideo) {
                ViewGroup.LayoutParams layoutParams = viewHolder.art.getLayoutParams();
                layoutParams.width = layoutParams.height;
                viewHolder.art.setLayoutParams(layoutParams);
                artWidth = artHeight;
            }
            UIUtils.loadImageWithCharacterAvatar(getActivity(), hostManager,
                                                 artUrl, title,
                                                 viewHolder.art, artWidth, artHeight);

            // For the popupmenu
            viewHolder.contextMenu.setTag(position);
            viewHolder.contextMenu.setOnClickListener(playlistItemMenuClickListener);

            return convertView;
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
