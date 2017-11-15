package com.example.fangy.vplayer;

import android.util.LruCache;

/**
 * Created by fangy on 2017/11/13.
 */

class SegmentLruCache extends LruCache<Integer, CachedSegment>{
    public SegmentLruCache(int maxBytes) {
        super(maxBytes);
    }

    @Override
    protected void entryRemoved(boolean evicted, Integer key, CachedSegment oldValue, CachedSegment newValue) {
        if(newValue != null && newValue == oldValue) {
            // When a value replaces itself, do nothing
            return;
        }

        // Delete the file upon cache removal, no matter if through a put or eviction
        oldValue.file.delete();
    }

    @Override
    protected int sizeOf(Integer key, CachedSegment value) {
        // Return the size of the file
        // NOTE an alternative would be to operate on time units and return the length of the segment
        return (int)value.file.length();
    }
}
