package com.example.fangy.vplayer;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by fangy on 2017/11/12.
 */

public class Period {
   // String id;
    //long startUs;
    long durationUs;
    boolean segmentAlignment;
    List<AdaptationSet> adaptationSets;

    Period() {
        adaptationSets = new ArrayList<AdaptationSet>();
    }

    public List<AdaptationSet> getAdaptationSets() {
        return adaptationSets;
    }

    public AdaptationSet getFirstSetOfType(String mime) {
        for(AdaptationSet as : adaptationSets) {
            if(as.mimeType != null && as.mimeType.startsWith(mime)) {
                return as;
            } else {
                for(Representation r : as.representations) {
                    if(r.mimeType.startsWith(mime)) {
                        return as;
                    }
                }
            }
        }
        return null;
    }

    public AdaptationSet getFirstVideoSet() {
        return getFirstSetOfType("video/");
    }

    public AdaptationSet getFirstAudioSet() {
        return getFirstSetOfType("audio/");
    }
}
