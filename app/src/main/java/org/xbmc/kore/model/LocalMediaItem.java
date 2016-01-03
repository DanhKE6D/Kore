package org.xbmc.kore.model;

import java.util.Comparator;

/**
 * Created by danh.le on 1/1/16.
 */
/*
{
   "runtime":288,
   "file":"F:\\Karaoke Songs\\DON CA\\ASIA-AI VAN-LY CAY DA.mkv",
   "label":"ASIA-AI VAN-LY CAY DA.mkv",
   "artist": "",
   "thumbnail":"image://video@F%3a%5cKaraoke%20Songs%5cDON%20CA%5cASIA-AI%20VAN-LY%20CAY%20DA.mkv/",
}
*/

public class LocalMediaItem implements Comparable<LocalMediaItem> {
    String file, label, thumbnail;
    int runtime;
    String title, album, artist;

    public LocalMediaItem( String label, String file, String thumbnail, int runtime) {
        this.file = file;
        this.label = label;     // do we event need label after parsing
        this.thumbnail = thumbnail;
        this.runtime = runtime;

        // "ASIA-AI VAN-LY CAY DA.mkv"
        String [] parts = label.split("-");
        switch (parts.length) {
            case 0:
                // no deimiters found -- find out if there is a file extension, if there is get rid of it
                this.title = splitFileName(label);
                this.album = this.artist = "Unknown";
                break;
            case 1:
                // only one -- either the album or artis is missing
                this.album = this.artist = parts[0].trim();
                this.title = splitFileName(parts[1]).trim();
                break;
            case 2:
                // all delimiters are there
                this.album = parts[0].trim();
                this.artist = parts[1].trim();
                this.title = splitFileName(parts[2]);
                break;
            default:
                // assuming the last part is the song title
                this.album = parts[0].trim();
                this.artist = parts[1].trim();
                this.title = splitFileName(parts[parts.length - 1]);
                break;
        }
    }

    String splitFileName(String fname) {
        int fExtIndex = fname.lastIndexOf(".");
        if (fExtIndex > 0) {
            return fname.substring(0, fExtIndex).trim();
        }
        // otherwise return the same string
        return fname.trim();
    }
    public int compareTo(LocalMediaItem o) {
        if(this.title != null)
            return this.title.toUpperCase().compareTo(o.getTitle().toUpperCase());
        else
            throw new IllegalArgumentException();
    }

    public static Comparator<LocalMediaItem> LocalMediaItemComparator
            = new Comparator<LocalMediaItem>() {

        public int compare(LocalMediaItem s1, LocalMediaItem s2) {

            String songName1 = s1.getArtist().toUpperCase();
            String songName2 = s2.getArtist().toUpperCase();

            //ascending order
            return songName1.compareTo(songName2);

            // descending order
            // return songName2.compareTo(songName1);
        }

    };

    public String getTitle() { return this.title; }
    public String getMediaSource() { return this.file; }
    public String getThumbnail() { return this.thumbnail; }
    public int getRuntime() { return this.runtime; }
    public String getAlbum() { return this.album; }
    public String getArtist() { return this.artist; }
}
