package com.example.fangy.vplayer;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by fangy on 2017/11/12.
 */

public class MPD {
    boolean isDynamic;
    long mediaPresentationDurationUs;
    Date availabilityStartTime;
    long timeShiftBufferDepthUs;
    long suggestedPresentationDelayUs;
    long maxSegmentDurationUs;
    long minBufferTimeUs;
    List<Period> periods;

    MPD() {
        periods = new ArrayList<Period>();
    }

    public long getMediaPresentationDurationUs() {
        return mediaPresentationDurationUs;
    }

    public long getMinBufferTimeUs() {
        return minBufferTimeUs;
    }

    public List<Period> getPeriods() {
        return periods;
    }

    public Period getFirstPeriod() {
        if(!periods.isEmpty()) {
            return periods.get(0);
        }
        return null;
    }

    @Override
    public String toString() {
        return "MPD{" +
                "mediaPresentationDurationUs=" + mediaPresentationDurationUs +
                ", minBufferTimeUs=" + minBufferTimeUs +
                //", representations=" + representations +
                '}';
    }
}
