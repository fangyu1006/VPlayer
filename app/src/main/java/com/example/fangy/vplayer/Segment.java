package com.example.fangy.vplayer;

/**
 * Created by fangy on 2017/11/12.
 */

public class Segment {

    String media;
    String range;

    Segment() {
    }

    Segment(String media) {
        this.media = media;
    }

    Segment(String media, String range) {
        this(media);
        this.range = range;
    }

    public String getMedia() {
        return media;
    }

    public String getRange() {
        return range;
    }

    public boolean hasRange() {
        return range != null;
    }

    @Override
    public String toString() {
        return "Segment{" +
                "media='" + media + '\'' +
                ", range='" + range + '\'' +
                '}';
    }
}
