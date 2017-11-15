package com.example.fangy.vplayer;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by fangy on 2017/11/12.
 */

public class Representation {
    String id;
    String codec;
    String mimeType;
    int width; // pixels
    int height; // pixels
    float sar; // storage aspect ratio
    int bandwidth; // bits/sec

    long segmentDurationUs;
   // Segment initSegment;
    List<Segment> segments;

    Representation() {
        segments = new ArrayList<Segment>();
    }

    public String getId() {
        return id;
    }

    public String getCodec() {
        return codec;
    }

    public String getMimeType() {
        return mimeType;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public boolean hasSAR() {
        return sar > 0;
    }

    public float calculatePAR() {
        float sizeRatio = (float) width / height;
        return sizeRatio * (hasSAR() ? sar : 1);
    }

    public int getBandwidth() {
        return bandwidth;
    }

    public long getSegmentDurationUs() {
        return segmentDurationUs;
    }
/*
    public Segment getInitSegment() {
        return initSegment;
    }
*/
    public List<Segment> getSegments() {
        return segments;
    }

    boolean hasSegments() {
        return !segments.isEmpty();
    }

    Segment getLastSegment() {
        return segments.get(segments.size() - 1);
    }

    @Override
    public String toString() {
        return "Representation{" +
                "id=" + id +
                ", codec='" + codec + '\'' +
                ", mimeType='" + mimeType + '\'' +
                ", width=" + width +
                ", height=" + height +
                ", dar=" + sar +
                ", bandwidth=" + bandwidth +
                //", initSegment=" + initSegment +
                ", segmentDurationUs=" + segmentDurationUs +
                //", segments=" + segments +
                '}';
    }

}
