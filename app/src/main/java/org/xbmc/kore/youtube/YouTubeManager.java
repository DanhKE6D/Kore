package org.xbmc.kore.youtube;

import android.util.Log;

import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.SearchListResponse;
import com.google.api.services.youtube.model.SearchResult;
import com.google.api.services.youtube.model.Video;
import com.google.api.services.youtube.model.VideoListResponse;

import org.xbmc.kore.utils.DeveloperKey;
import org.xbmc.kore.youtube.model.YouTubeVideo;

import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.LinkedList;
import java.util.List;
import java.util.TimeZone;

/**
 * Created by dql on 3/1/14.
 */
public class YouTubeManager {
    // API for dql

    public static final String YOUTUBE_WATCH_URL_PREFIX = "http://www.youtube.com/watch?v=";
    static final String YOUTUBE_KODI_PLUGIN_URL_PREFIX = "plugin://plugin.video.youtube/play/?video_id=";
    public static final String YOUTUBE_WATCH_PLAYLIST_PREFIX = "http://www.youtube.com/playlist?list=";
    public static final String YT_PUBLIC_VIDEO = "public";
    public static final String YT_VIDEO_KIND = "youtube#video";
    public static final String YT_PLAYLIST_KIND = "youtube#playlist";
    private static final String TAG = "YouTubeManager";
    private String clientID;
    final HttpTransport transport = AndroidHttp.newCompatibleTransport();
    final JsonFactory jsonFactory = new GsonFactory();
    YouTube youtube;
    String nextPageToken, prevPageToken;
    YouTubeVideo.YouTubeMediaType mediaSearchType;

    public YouTubeManager(String clientID, YouTubeVideo.YouTubeMediaType type) {
        this.clientID = clientID;
        this.mediaSearchType = type;
        youtube =
                new YouTube.Builder(transport, jsonFactory, new HttpRequestInitializer() {
                    public void initialize(HttpRequest request) throws IOException {
                    }
                }).setApplicationName(clientID).build();
    }

    public String getNextPageToken() {
        return nextPageToken;
    }
    public String getPrevPageToken() {
        return prevPageToken;
    }

    List<SearchResult> searchForMedia(String textQuery, String nextPgToken, long maxResults) throws GoogleJsonResponseException, IOException {
        List<SearchResult> l = null;
        // Define the API request for retrieving search results.
        Log.i(TAG, "YouTube.Search.List: Type = " + ((mediaSearchType == YouTubeVideo.YouTubeMediaType.Playlist) ? "Playlist" : "Video") +
                ", searchText = " + textQuery + ", pageToken = " + nextPgToken + ", maxResults = " + maxResults);
        YouTube.Search.List search = null;
        if (mediaSearchType == YouTubeVideo.YouTubeMediaType.Video) {
            // Restrict the search results to only include videos. See:
            // https://developers.google.com/youtube/v3/docs/search/list#type

            search = youtube.search().list("id");
            // Set your developer key from the {{ Google Cloud Console }} for
            // non-authenticated requests. See:
            // {{ https://cloud.google.com/console }}
            search.setKey(DeveloperKey.getSimpleAPIKey());
            search.setQ(textQuery);
            if (nextPgToken != null)
                search.setPageToken(nextPgToken);
            search.setType("video");
            search.setFields("items(id/kind,id/videoId),nextPageToken,prevPageToken");
        }
        else if (mediaSearchType == YouTubeVideo.YouTubeMediaType.Playlist) {
            // To increase efficiency, only retrieve the fields that the application uses.
            //search.setFields("items(id/kind,id/videoId,snippet/title,snippet/thumbnails/default/url)");
            //search.setFields("items(id/kind,id/videoId,snippet/title,snippet/thumbnails/default/url," +
            //        "snippet/thumbnails/medium/url, snippet/thumbnails/medium/url," +
            //        "snippet/thumbnails/medium/url, snippet/thumbnails/high/url," +
            //        "snippet/channelTitle),nextPageToken");
            search = youtube.search().list("snippet");
            // Set your developer key from the {{ Google Cloud Console }} for
            // non-authenticated requests. See:
            // {{ https://cloud.google.com/console }}

            search.setKey(DeveloperKey.getSimpleAPIKey());
            search.setQ(textQuery);
            if (nextPgToken != null)
                search.setPageToken(nextPgToken);
            search.setType("playlist");
            search.setFields("items(id/kind,id/playlistId,snippet/title,snippet/channelTitle,snippet/thumbnails/default/url," +
                    "snippet/thumbnails/medium/url,snippet/thumbnails/high/url),nextPageToken,prevPageToken");
        }
        search.setMaxResults(maxResults);

        Log.i(TAG, "Executing search...");
        // Call the API and print results.
        SearchListResponse searchResponse = search.execute();
        l = searchResponse.getItems();
        prevPageToken = searchResponse.getPrevPageToken();
        nextPageToken = searchResponse.getNextPageToken();
        Log.i(TAG, "prevPageToken = " + prevPageToken + " nextPageToken = " + nextPageToken);
        return l;
    }

    public List<YouTubeVideo> retrieveVideos(String textQuery, String nextPgToken, long maxResults) throws Exception {
        List<YouTubeVideo> youtubeVideoList = null;
        List<SearchResult> searchResultList = null;

        try {
            searchResultList = searchForMedia(textQuery, nextPgToken, maxResults);
            Log.i(TAG, "Retrieving result...");
            if (searchResultList != null) {
                if (mediaSearchType == YouTubeVideo.YouTubeMediaType.Video) {
                    //youtubeVideoList = convertVideos(searchResultList.iterator());
                    // now find the videos that has these ID and get the video duration

                    // then format a list of video ID to query for video contentDetails and snippet
                    StringBuilder sb = new StringBuilder();
                    for (int i = 0; i < searchResultList.size(); i++) {
                        if (YT_VIDEO_KIND.equalsIgnoreCase(searchResultList.get(i).getId().getKind())) {
                            sb.append(searchResultList.get(i).getId().getVideoId());
                            // don't want to append a ',' on the last item
                            if (i != (searchResultList.size() - 1))
                                sb.append(",");
                        }
                    }
                    Log.i(TAG, "Video ID list = " + sb.toString());
                    /*
                    YouTube.Videos.List vidSearch = youtube.videos().list("snippet,contentDetails");
                    vidSearch.setKey(DeveloperKey.getSimpleAPIKey());
                    vidSearch.setId(sb.toString());
                    //vidSearch.setFields("items(id,contentDetails/duration)");
                    vidSearch.setFields("items(kind,id,snippet/title,snippet/thumbnails/default/url," +
                            "snippet/thumbnails/medium/url,snippet/thumbnails/medium/url," +
                            "snippet/thumbnails/medium/url,snippet/thumbnails/high/url," +
                            "snippet/channelTitle,contentDetails/duration)");
                    vidSearch.setMaxResults(maxResults);
                    VideoListResponse vlr = vidSearch.execute();
                    // for (Video i : vlr.getItems()) {
                    //    Log.i(TAG, "Video ID: " + i.getId() + ", duration: " + i.getContentDetails().getDuration());
                    youtubeVideoList = convertVideos(vlr);
                    */
                    youtubeVideoList = getVideoDetails(sb.toString(), maxResults);
                }
                else if (mediaSearchType == YouTubeVideo.YouTubeMediaType.Playlist){
                    // playlist for now
                    youtubeVideoList = convertPlaylists(searchResultList);
                }
                else {
                    // channel
                }
            }
        } catch (GoogleJsonResponseException e) {
            Log.e(TAG, "There was a service error: " + e.getDetails().getCode() + " : "
                    + e.getDetails().getMessage());
            throw new Exception();
        } catch (IOException e) {
            Log.e(TAG, "There was an IO error: " + e.getCause() + " : " + e.getMessage());
            throw new Exception();
        } catch (Throwable t) {
            t.printStackTrace();
            throw new Exception();
        }
        return youtubeVideoList;
    }

    public List<YouTubeVideo> getVideoDetails(String vids, long maxResults) throws IOException {
        YouTube.Videos.List vidSearch = youtube.videos().list("snippet,contentDetails");
        vidSearch.setKey(DeveloperKey.getSimpleAPIKey());
        vidSearch.setId(vids);
        //vidSearch.setFields("items(id,contentDetails/duration)");
        vidSearch.setFields("items(kind,id,snippet/title,snippet/thumbnails/default/url," +
                "snippet/thumbnails/medium/url,snippet/thumbnails/medium/url," +
                "snippet/thumbnails/medium/url,snippet/thumbnails/high/url," +
                "snippet/channelTitle,contentDetails/duration)");
        vidSearch.setMaxResults(maxResults);
        VideoListResponse vlr = vidSearch.execute();
        // for (Video i : vlr.getItems()) {
        //    Log.i(TAG, "Video ID: " + i.getId() + ", duration: " + i.getContentDetails().getDuration());
        return convertVideos(vlr);
    }

    private List<YouTubeVideo> convertVideos(VideoListResponse vl) {
        Log.i(TAG, "YouTubeManager.convertVideos:");

        List<YouTubeVideo> youtubeVideosList = new LinkedList<YouTubeVideo>();
        for (Video i : vl.getItems()) {
            // Confirm that the result represents a video. Otherwise, the
            // item will not contain a video ID.
            if (i.getKind().equals(YT_VIDEO_KIND)) {
                YouTubeVideo ytv = new YouTubeVideo();
                ytv.setMediaType(YouTubeVideo.YouTubeMediaType.Video);
                // make it compatible with sbConnectionMgr video ID
                //String webPlayerUrl = YOUTUBE_WATCH_URL_PREFIX + i.getId() /* + YOUTUBE_WATCH_URL_POSTFIX */;
                // kodi youtube plug in prefix -- dql 20150902
                String webPlayerUrl = YOUTUBE_KODI_PLUGIN_URL_PREFIX + i.getId() /* + YOUTUBE_WATCH_URL_POSTFIX */;
                Log.i(TAG, " Video Id: " + webPlayerUrl);
                ytv.setWebPlayerUrl(webPlayerUrl);
                //Log.i(TAG, " Title: " + i.getSnippet().getTitle());
                ytv.setTitle(i.getSnippet().getTitle());
                // Thumbnails
                // format: "url": "https://i1.ytimg.com/vi/DwD4yN1sDIk/default.jpg"    -- for default
                //         "url": "https://i1.ytimg.com/vi/DwD4yN1sDIk/mqdefault.jpg"  -- for medium
                //         "url": "https://i1.ytimg.com/vi/DwD4yN1sDIk/hqdefault.jpg"  -- for high
                ArrayList<String> thumbnails = new ArrayList<String>();
                thumbnails.add(i.getSnippet().getThumbnails().getDefault().getUrl());
                thumbnails.add(i.getSnippet().getThumbnails().getMedium().getUrl());
                thumbnails.add(i.getSnippet().getThumbnails().getHigh().getUrl());
                //Log.i(TAG, " Thumbnails: " + thumbnails.get(FavSongID.THUMBNAIL_SQ) + "," +
                //                             thumbnails.get(FavSongID.THUMBNAIL_MQ) + "," +
                //                             thumbnails.get(FavSongID.THUMBNAIL_HQ));
                ytv.setThumbnails(thumbnails);
                String duration = i.getContentDetails().getDuration();
                ytv.setDuration(convertPTDurationToSeconds(duration));
                ytv.setUploader(i.getSnippet().getChannelTitle());

                //Log.i(TAG, "-------------------------------------------------------------");
                youtubeVideosList.add(ytv);
            }
        }

        Log.i(TAG, "Retrieving result successful...");
        return youtubeVideosList;

    }
    private List<YouTubeVideo> convertPlaylists(List<SearchResult> pll) {
        Log.i(TAG, "YouTubeManager.convertPlaylists:");

        List<YouTubeVideo> youtubeVideosList = new LinkedList<YouTubeVideo>();
        for (int i = 0; i < pll.size(); i++) {
            SearchResult s = pll.get(i);
            if (YT_PLAYLIST_KIND.equalsIgnoreCase(s.getId().getKind())) {
                YouTubeVideo ytv = new YouTubeVideo();
                ytv.setMediaType(YouTubeVideo.YouTubeMediaType.Playlist);
                String webPlayerUrl = YOUTUBE_WATCH_PLAYLIST_PREFIX + s.getId().getPlaylistId();
                Log.i(TAG, " Playlist Id: " + webPlayerUrl);
                ytv.setWebPlayerUrl(webPlayerUrl);
                //Log.i(TAG, " Title: " + s.getSnippet().getTitle());
                ytv.setTitle(s.getSnippet().getTitle());
                // Thumbnails
                // format: "url": "https://i1.ytimg.com/vi/DwD4yN1sDIk/default.jpg"    -- for default
                //         "url": "https://i1.ytimg.com/vi/DwD4yN1sDIk/mqdefault.jpg"  -- for medium
                //         "url": "https://i1.ytimg.com/vi/DwD4yN1sDIk/hqdefault.jpg"  -- for high
                ArrayList<String> thumbnails = new ArrayList<String>();
                thumbnails.add(s.getSnippet().getThumbnails().getDefault().getUrl());
                thumbnails.add(s.getSnippet().getThumbnails().getMedium().getUrl());
                thumbnails.add(s.getSnippet().getThumbnails().getHigh().getUrl());
                //Log.i(TAG, " Thumbnails: " + thumbnails.get(FavSongID.THUMBNAIL_SQ) + "," +
                //        thumbnails.get(FavSongID.THUMBNAIL_MQ) + "," +
                //        thumbnails.get(FavSongID.THUMBNAIL_HQ));
                ytv.setThumbnails(thumbnails);
                ytv.setUploader(s.getSnippet().getChannelTitle());
                //Log.i(TAG, "-------------------------------------------------------------");
                youtubeVideosList.add(ytv);

            }
        }
        Log.i(TAG, "Retrieving result successful...");
        return youtubeVideosList;

    }

    long convertPTDurationToSeconds(String duration)  {
        long numberOfSeconds = 0;
        DateFormat df;

        if (duration.contains("H"))
            df = new SimpleDateFormat("'PT'hh'H'mm'M'ss'S'");
        else if (duration.contains("M"))
            df = new SimpleDateFormat("'PT'mm'M'ss'S'");
        else
            df = new SimpleDateFormat("'PT'ss'S'");
        Date d;
        try {
            d = df.parse(duration);
        } catch (ParseException e) {
            // TODO Auto-generated catch block
            // e.printStackTrace();
            return 0;		// can't parse -- can't convert
        }
        Calendar c = new GregorianCalendar();
        c.setTime(d);
        c.setTimeZone(TimeZone.getDefault());
        numberOfSeconds = c.get(Calendar.HOUR) * 3600 + c.get(Calendar.MINUTE) * 60 + c.get(Calendar.SECOND);
        //Log.i(TAG, "convertPTDurationToSeconds: " + duration + ", seconds = " + numberOfSeconds);
        return numberOfSeconds;
    }

}
