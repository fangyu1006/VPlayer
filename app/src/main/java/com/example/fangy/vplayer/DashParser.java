package com.example.fangy.vplayer;

import android.net.Uri;
import android.util.Log;
import android.util.Xml;

import net.protyposis.android.mediaplayer.UriSource;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import okhttp3.Headers;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Created by fangy on 2017/11/12.
 */

public class DashParser {
    private static final String TAG = DashParser.class.getSimpleName();

    private static Pattern PATTERN_TIME = Pattern.compile("PT((\\d+)H)?((\\d+)M)?((\\d+(\\.\\d+)?)S)");
    private static Pattern PATTERN_TEMPLATE = Pattern.compile("\\$(\\w+)(%0\\d+d)?\\$");
    private static DateFormat ISO8601UTC;

    static {
        ISO8601UTC = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
        ISO8601UTC.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    private static class SegmentTemplate {
        private static class SegmentTimelineEntry {
            // Segment start time
            long t;
            // Segment duration
            long d;
            // The segment repeat time. The number of contiguous segments with duration d.
            int r;

            long calculateDuration() {
                return d * (r + 1);
            }

            ;
        }


        long presentationTimeOffsetUs;
        long timescale;
        String init;
        String media;
        long duration;
        int startNumber;
        List<SegmentTimelineEntry> timeline = new ArrayList<>();

        long calculateDurationUs() {
            return calculateUs(duration, timescale);
        }

        boolean hasTimeline() {
            return !timeline.isEmpty();
        }
    }

    private Date serverDate;

    /**
     * Parses an MPD XML file. This needs to be executed off the main thread, else a
     * NetworkOnMainThreadException gets thrown.
     * @param source the URl of an MPD XML file
     * @param httpClient the http client instance to use for the request
     * @return a MPD object
     * @throws android.os.NetworkOnMainThreadException if executed on the main thread
     */
    public MPD parse(UriSource source, OkHttpClient httpClient) throws DashParserException {
        MPD mpd = null;
        Headers.Builder headers = new Headers.Builder();
        if((source.getHeaders() != null) && !source.getHeaders().isEmpty()) {
            for(String name : source.getHeaders().keySet()) {
                headers.add(name, source.getHeaders().get(name));
            }
        }

        Uri uri = source.getUri();

        Request.Builder request = new Request.Builder()
                .url(uri.toString())
                .headers(headers.build());
        try {
            Response response = httpClient.newCall(request.build()).execute();
            if(!response.isSuccessful()) {
                throw new IOException("error requesting the MPD");
            }

            // Determine this MPD's default BaseURL by removing the last path segment (which is the MPD file)
            Uri baseUrl = Uri.parse(uri.toString().substring(0, uri.toString().lastIndexOf("/") + 1));

            // Get the current datetime from the server for live stream time syncing
            serverDate = response.headers().getDate("Date");

            // Parse the MPD file
            mpd = parse(response.body().byteStream(), baseUrl);
        } catch (IOException e) {
            Log.e(TAG, "error downloading the MPD", e);
            throw new DashParserException("error downloading the MPD", e);
        } catch (XmlPullParserException e) {
            Log.e(TAG, "error parsing the MPD", e);
            throw new DashParserException("error parsing the MPD", e);
        }

        return mpd;
    }

    /**
     * Parses an MPD XML file. This needs to be executed off the main thread, else a
     * NetworkOnMainThreadException gets thrown.
     * @param source the URl of an MPD XML file
     * @return a MPD object
     * @throws android.os.NetworkOnMainThreadException if executed on the main thread
     */
    public MPD parse(UriSource source) throws DashParserException {
        return parse(source, new OkHttpClient());
    }

    private MPD parse(InputStream in, Uri baseUrl) throws XmlPullParserException, IOException, DashParserException {
        try {
            XmlPullParser parser = Xml.newPullParser();
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
            parser.setInput(in, null);

            MPD mpd = new MPD();
            Period currentPeriod = null;

            int type = 0;
            while ((type = parser.next()) >= 0) {
                if (type == XmlPullParser.START_TAG) {
                    String tagName = parser.getName();

                    if (tagName.equals("MPD")) { // parse MPD part in XML
                        mpd.isDynamic = getAttributeValue(parser, "type", "static").equals("dynamic");
                        if (mpd.isDynamic) {
                         // TODO  add support for dynamic streams
                       } else { // type is static
                            mpd.mediaPresentationDurationUs = getAttributeValueTime(parser, "mediaPresentationDuration");
                       }
                        mpd.minBufferTimeUs = getAttributeValueTime(parser, "minBufferTime");
                    } else if(tagName.equals("Period")){
                        currentPeriod = new Period();
                        // currentPeriod.id = getAttributeValue(parser, "id");
                        // currentPeriod.startUs = getAttributeValueTime(parser, "start");
                        currentPeriod.durationUs = getAttributeValueTime(parser, "duration");
                        //currentPeriod.segmentAlignment = getAttributeValueBoolean(parser, "segmentAlignment");
                    } else if(tagName.equals("AdaptationSet")) {
                        currentPeriod.adaptationSets.add(readAdaptationSet(mpd, currentPeriod, baseUrl, parser));
                    }
                } else if (type == XmlPullParser.END_TAG) {
                    String tagName = parser.getName();
                    if (tagName.equals("MPD")) {
                        break;
                    } else if (tagName.equals("Period")) {
                        mpd.periods.add(currentPeriod);
                        currentPeriod = null;
                    }
                }
            }
            Log.d(TAG, mpd.toString());

            return mpd;
        } finally {
            in.close();
        }
    }

    private AdaptationSet readAdaptationSet(MPD mpd, Period period, Uri baseUrl, XmlPullParser parser)
            throws XmlPullParserException, IOException, DashParserException {
        AdaptationSet adaptationSet = new AdaptationSet();

        //TODO need to be fixed
        adaptationSet.group = getAttributeValueInt(parser, "group");
        adaptationSet.mimeType = getAttributeValue(parser, "mimeType");
        adaptationSet.maxWidth = getAttributeValueInt(parser, "maxWidth");
        adaptationSet.maxHeight = getAttributeValueInt(parser, "maxHeight");
        adaptationSet.par = getAttributeValueRatio(parser, "par");

        SegmentTemplate segmentTemplate = null;

        int type = 0;

        while((type = parser.next()) >= 0) {
            if(type == XmlPullParser.START_TAG) {
                String tagName = parser.getName();

                if(tagName.equals("Representation")) {
                    try {
                        adaptationSet.representations.add(readRepresentation(
                                mpd, period, adaptationSet, baseUrl, parser, segmentTemplate));
                    } catch (Exception e) {
                        Log.e(TAG, "error reading representation: " + e.getMessage(), e);
                    }
                }
            } else if(type == XmlPullParser.END_TAG) {
                String tagName = parser.getName();
                if (tagName.equals("AdaptationSet")) {
                    return adaptationSet;
                }
            }
        }
        throw new DashParserException("invalid state");
    }

    private Representation readRepresentation(MPD mpd, Period period, AdaptationSet adaptationSet,
                                              Uri baseUrl, XmlPullParser parser,
                                              SegmentTemplate segmentTemplate)
            throws XmlPullParserException, IOException, DashParserException {
        Representation representation = new Representation();

        representation.id = getAttributeValue(parser, "id");
        representation.bandwidth = getAttributeValueInt(parser, "bandwidth");
        representation.width = getAttributeValueInt(parser, "width");
        representation.height = getAttributeValueInt(parser, "height");
        representation.mimeType = getAttributeValue(parser, "mimeType", adaptationSet.mimeType);
        // TODO need to be fixed
        representation.codec = getAttributeValue(parser, "codecs");
        // TODO need to be fixed
        representation.sar = getAttributeValueRatio(parser, "sar");

        int type = 0;
        while((type = parser.next()) >= 0) {
            String tagName = parser.getName();

            if(type == XmlPullParser.START_TAG) {
                if(tagName.equals("BaseURL")) {
                    // TODO need to be fixed
                    baseUrl = extendUrl(baseUrl, parser.nextText());
                } else if(tagName.equals("SegmentList")) {
                    long duration = getAttributeValueLong(parser, "duration");
                    long timescale = getAttributeValueLong(parser, "timescale", 1);
                    representation.segmentDurationUs = (long)(((double)duration / timescale) * 1000000d);
                } else if (tagName.equals("SegmentURL")) {
                    String media = getAttributeValue(parser, "media");
                    media = media != null ? extendUrl(baseUrl, media).toString() : baseUrl.toString();
                }

            } else if(type == XmlPullParser.END_TAG) {
                if(tagName.equals("Representation")) {
                    return representation;
                }
            }
        }
        throw new DashParserException("invalid state");
    }

    private void skip(XmlPullParser parser) throws XmlPullParserException, IOException {
        if (parser.getEventType() != XmlPullParser.START_TAG) {
            throw new IllegalStateException();
        }
        int depth = 1;
        while (depth != 0) {
            switch (parser.next()) {
                case XmlPullParser.END_TAG:
                    depth--;
                    break;
                case XmlPullParser.START_TAG:
                    depth++;
                    break;
            }
        }
    }

    /**
     * Parse a timestamp and return its duration in microseconds.
     * http://en.wikipedia.org/wiki/ISO_8601#Durations
     */
    private static long parseTime(String time) {
        Matcher matcher = PATTERN_TIME.matcher(time);

        if(matcher.matches()) {
            long hours = 0;
            long minutes = 0;
            double seconds = 0;

            String group = matcher.group(2);
            if (group != null) {
                hours = Long.parseLong(group);
            }
            group = matcher.group(4);
            if (group != null) {
                minutes = Long.parseLong(group);
            }
            group = matcher.group(6);
            if (group != null) {
                seconds = Double.parseDouble(group);
            }

            return (long) (seconds * 1000 * 1000)
                    + minutes * 60 * 1000 * 1000
                    + hours * 60 * 60 * 1000 * 1000;
        }

        return -1;
    }

    /**
     * Extends an URL with an extended path if the extension is relative, or replaces the entire URL
     * with the extension if it is absolute.
     */
    private static Uri extendUrl(Uri url, String urlExtension) {
        urlExtension = urlExtension.replace(" ", "%20"); // Convert spaces

        Uri newUrl = Uri.parse(urlExtension);

        if(newUrl.isRelative()) {
            /* Uri.withAppendedPath appends the extension to the end of the "real" server path,
             * instead of the end of the uri string.
             * Example: http://server.com/foo?file=http://server2.net/ + file1.mp4
             *           => http://server.com/foo/file1.mp4?file=http://server2.net/
             * To avoid this, we need to join as strings instead. */
            newUrl = Uri.parse(url.toString() + urlExtension);
        }
        return newUrl;
    }

    /**
     * Converts a time/timescale pair to microseconds.
     */
    private static long calculateUs(long time, long timescale) {
        return (long)(((double)time / timescale) * 1000000d);
    }

    private static long calculateTimescaleTime(long time, long timescale) {
        return (long)((time / 1000000d) * timescale);
    }

    private static String getAttributeValue(XmlPullParser parser, String name, String defValue) {
        String value = parser.getAttributeValue(null, name);
        return value != null ? value : defValue;
    }

    private static String getAttributeValue(XmlPullParser parser, String name) {
        return getAttributeValue(parser, name, null);
    }

    private static int getAttributeValueInt(XmlPullParser parser, String name) {
        return Integer.parseInt(getAttributeValue(parser, name, "0"));
    }

    private static int getAttributeValueInt(XmlPullParser parser, String name, int defValue) {
        return Integer.parseInt(getAttributeValue(parser, name, defValue+""));
    }

    private static long getAttributeValueLong(XmlPullParser parser, String name) {
        return Long.parseLong(getAttributeValue(parser, name, "0"));
    }

    private static long getAttributeValueLong(XmlPullParser parser, String name, long defValue) {
        return Long.parseLong(getAttributeValue(parser, name, defValue+""));
    }

    private static long getAttributeValueTime(XmlPullParser parser, String name) {
        return parseTime(getAttributeValue(parser, name, "PT0S"));
    }

    private static long getAttributeValueTime(XmlPullParser parser, String name, String defValue) {
        return parseTime(getAttributeValue(parser, name, defValue));
    }

    private static float getAttributeValueRatio(XmlPullParser parser, String name) {
        String value = getAttributeValue(parser, name);

        if(value != null) {
            String[] values = value.split(":");
            return (float)Integer.parseInt(values[0]) / Integer.parseInt(values[1]);
        }

        return 0;
    }

    private static boolean getAttributeValueBoolean(XmlPullParser parser, String name) {
        String value = getAttributeValue(parser, name, "false");
        return value.equals("true");
    }


    /**
     * Processes templates in media URLs.
     *
     * Example: $RepresentationID$_$Number%05d$.ts
     *
     * 5.3.9.4.4 Template-based Segment URL construction
     * Table 16 - Identifiers for URL templates
     */
    private static String processMediaUrl(String url, String representationId,
                                          Integer number, Integer bandwidth, Long time) {
        // RepresentationID
        if(representationId != null) {
            url = url.replace("$RepresentationID$", representationId);
        }

        // Number, Bandwidth & Time with formatting support
        // The following block converts DASH segment URL templates to a Java String.format expression

        List<String> templates = Arrays.asList("Number", "Bandwidth", "Time");
        Matcher matcher = PATTERN_TEMPLATE.matcher(url);

        while(matcher.find()) {
            String template = matcher.group(1);
            String pattern = matcher.group(2);
            int index = templates.indexOf(template);

            if(pattern != null) {
                url = url.replace("$" + template + pattern + "$",
                        "%" + (index + 1) + "$" + pattern.substring(1));
            } else {
                // Table 16: If no format tag is present, a default format tag with width=1 shall be used.
                url = url.replace("$" + template + "$", "%" + (index + 1) + "$01d");
            }
        }

        url = String.format(url, number, bandwidth, time); // order must match templates list above

        // $$
        // Replace this at the end, else it breaks directly consecutive template expressions,
        // e.g. $Bandwidth$$Number$.
        url = url.replace("$$", "$");

        return url;
    }
}
