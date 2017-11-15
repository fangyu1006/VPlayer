package com.example.fangy.vplayer;

/**
 * Created by fangy on 2017/11/12.
 */

public interface AdaptationLogic {

    // Returns an initial adaptation set to start, before any segments have been loaded.
    Representation initialize(AdaptationSet adaptationSet);

    // Receiver of performance data on downloaded segments in the {@link net.protyposis.android.mediaplayer.dash.DashMediaExtractor}.
    void reportSegmentDownload(AdaptationSet adaptationSet, Representation representation, Segment segment, int byteSize, long downloadTimeMs);

    // Returns the recommended representation at the time of calling
    Representation getRecommendedRepresentation(AdaptationSet adaptationSet);

}
