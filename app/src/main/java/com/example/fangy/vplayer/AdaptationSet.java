package com.example.fangy.vplayer;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by fangy on 2017/11/12.
 */

public class AdaptationSet {
    int group;
    String mimeType;
    int maxWidth;
    int maxHeight;
    float par; // picture aspect ratio (also called DAR - display aspect ratio)
    List<Representation> representations;

    AdaptationSet() {
        representations = new ArrayList<Representation>();
    }

    public int getGroup() {
        return group;
    }

    public String getMimeType() {
        return mimeType;
    }

    public List<Representation> getRepresentations() {
        return representations;
    }

    public boolean hasMaxDimensions() {
        return maxWidth > 0 && maxHeight > 0;
    }

    public boolean hasPAR() {
        return par > 0;
    }

    @Override
    public String toString() {
        return "AdaptationSet{" +
                "group=" + group +
                ", mimeType='" + mimeType + '\'' +
                ", maxWidth='" + maxWidth +
                ", maxHeight='" + maxHeight +
                ", par='" + par +
                //", representations=" + representations +
                '}';
    }

}
