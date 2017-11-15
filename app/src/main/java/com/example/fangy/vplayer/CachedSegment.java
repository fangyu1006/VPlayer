package com.example.fangy.vplayer;

import java.io.File;

/**
 * Created by fangy on 2017/11/13.
 */

public class CachedSegment {
    int number;
    Segment segment;
    Representation representation;
    AdaptationSet adaptationSet;
    File file;
    long ptsOffsetUs;

    CachedSegment(int number, Segment segment, Representation representation, AdaptationSet adaptationSet) {
        this.number = number;
        this.segment = segment;
        this.representation = representation;
        this.adaptationSet = adaptationSet;
    }
}
