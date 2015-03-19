package uk.co.spookypeanut.loveandtag;

/**
 * Copyright (c) 2014 Henry Bush
 * Distributed under the GNU GPL v3. For full terms see the file COPYING.
 */

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InvalidObjectException;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LastfmSession {
    Context mContext;
    SharedPreferences mSettings;
    String mSessionKey = "";
    String mUsername;
    UrlMaker mUrlMaker;
    static final String PREFS_NAME = "LastfmSession";
    static final String SESSION_KEY = "session_key";
    static final String USERNAME = "username";
    static final short RETRIES = 5;

    public LastfmSession() {
        mContext = App.getContext();
        mSettings = mContext.getSharedPreferences(PREFS_NAME, 0);
        mUrlMaker = new UrlMaker();
        passiveLogin();
    }

    private void passiveLogin() {
        final String tag = "LastfmSession.passiveLogin";
        if (mSettings.contains(SESSION_KEY)) {
            mSessionKey = mSettings.getString(SESSION_KEY, "");
            mUsername = mSettings.getString(USERNAME, "");
        } else {
            Log.i(tag, "Couldn't log in");
        }
    }

    public boolean isLoggedIn() {
        if (!mSessionKey.equals("")) {
            return true;
        }
        passiveLogin();
        return !mSessionKey.equals("");
    }

    public boolean unlove(Track orig_track) {
        final String tag = "LastfmSession.unlove";
        Track track;
        try {
            track = preTrackAction(orig_track);
        }
        catch (InvalidObjectException e) {
            Log.e(tag, "preTrackAction failed, aborting");
            e.printStackTrace();
            return false;
        }
        Map<String, String> restparams = new HashMap<>();
        restparams.put("method", "track.unlove");
        restparams.put("sk", mSessionKey);
        restparams.put("track", track.mTitle);
        restparams.put("artist", track.mArtist);
        String urlString;
        urlString = mUrlMaker.fromHashmap(restparams);
        boolean response;
        try {
            response = getBoolean(urlString);
            return response;
        }
        catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
    public boolean love(Track orig_track) {
        final String tag = "LastfmSession.love";
        Track track;
        try {
            track = preTrackAction(orig_track);
        }
        catch (InvalidObjectException e) {
            Log.e(tag, "preTrackAction failed, aborting");
            e.printStackTrace();
            return false;
        }
        Map<String, String> restparams = new HashMap<>();
        restparams.put("method", "track.love");
        restparams.put("sk", mSessionKey);
        restparams.put("track", track.mTitle);
        restparams.put("artist", track.mArtist);
        String urlString;
        urlString = mUrlMaker.fromHashmap(restparams);
        boolean response;
        try {
            response = getBoolean(urlString);
            return response;
        }
        catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean tag(Track orig_track, String tag_cat) {
        final String tag = "LastfmSession.tag";
        Track track;
        try {
            track = preTrackAction(orig_track);
        }
        catch (InvalidObjectException e) {
            Log.e(tag, "preTrackAction failed, aborting");
            e.printStackTrace();
            return false;
        }
        Map<String, String> rest_params = new HashMap<>();
        rest_params.put("method", "track.addTags");
        rest_params.put("sk", mSessionKey);
        rest_params.put("track", track.mTitle);
        rest_params.put("artist", track.mArtist);
        rest_params.put("tags", tag_cat);
        String urlString;
        urlString = mUrlMaker.fromHashmap(rest_params);
        boolean response;
        try {
            response = getBoolean(urlString);
            return response;
        }
        catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean untag(Track orig_track, String tag_name) {
        final String tag = "LastfmSession.untag";
        Track track;
        try {
            track = preTrackAction(orig_track);
        }
        catch (InvalidObjectException e) {
            Log.e(tag, "preTrackAction failed, aborting");
            e.printStackTrace();
            return false;
        }
        Map<String, String> rest_params = new HashMap<>();
        rest_params.put("method", "track.removeTag");
        rest_params.put("sk", mSessionKey);
        rest_params.put("track", track.mTitle);
        rest_params.put("artist", track.mArtist);
        rest_params.put("tag", tag_name);
        String urlString;
        urlString = mUrlMaker.fromHashmap(rest_params);
        boolean response;
        try {
            response = getBoolean(urlString);
            return response;
        }
        catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public List<Track> getRecent () {
        final String tag = "LastfmSession.getRecent";
        if (!isLoggedIn()) {
            throw(new IllegalStateException("Session is not logged in"));
        }
        Map<String, String> rest_params = new HashMap<>();
        rest_params.put("method", "user.getRecentTracks");
        rest_params.put("user", mUsername);
        // This is so we get loved information too
        rest_params.put("extended", "1");
        rest_params.put("limit", "25");
        String urlString = mUrlMaker.fromHashmap(rest_params);
        Map<String, List<String>> list_map = new HashMap<>();
        list_map.put("artist", Arrays.asList("lfm", "recenttracks",
                                             "track", "artist", "name"));
        list_map.put("title", Arrays.asList("lfm", "recenttracks",
                                            "track", "name"));
        list_map.put("loved", Arrays.asList("lfm", "recenttracks",
                "track", "loved"));

        XmlPullParser parser;
        List<Track> recentTracks = new ArrayList<>();
        try {
            parser = getUrlResponse(urlString);
            if (parser == null) {
                Log.w(tag,"Invalid parser from getUrlResponse, " +
                          "returning empty list");
                return new ArrayList<>();
            }
            for (Map<String, String> map : getTagsFromLists(parser, list_map)) {
                recentTracks.add(new Track(map));
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return recentTracks;
    }

    public TagList getTrackTags(Track orig_track) {
        final String tag = "LastfmSession.getTrackTags";
        Track track;
        try {
            track = preTrackAction(orig_track);
        }
        catch (InvalidObjectException e) {
            Log.e(tag, "preTrackAction failed, aborting");
            e.printStackTrace();
            return new TagList();
        }
        Map<String, String> rest_params = new HashMap<>();
        rest_params.put("method", "track.getTags");
        rest_params.put("artist", track.mArtist);
        rest_params.put("track", track.mTitle);
        rest_params.put("user", mUsername);
        String urlString = mUrlMaker.fromHashmap(rest_params);
        Map<String, List<String>> list_map = new HashMap<>();
        list_map.put("tag", Arrays.asList("lfm", "tags", "tag", "name"));
        XmlPullParser parser;
        TagList tags = new TagList();
        try {
            parser = getUrlResponse(urlString);
            List<Map<String,String>> results = getTagsFromLists(parser,
                    list_map);
            for (Map<String, String> map : results) {
                Log.d(tag, map.toString());
                Tag t = new Tag(map.get("tag"));
                t.mPresent = true;
                t.mActive = true;
                tags.add(t);
            }
            return tags;
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return new TagList();

    }

    public boolean isLoved(Track orig_track) {
        final String tag = "LastfmSession.isLoved";
        Track info;
        try {
            info = getTrackInfo(orig_track);
        }
        catch (InvalidObjectException e) {
            Log.e(tag, "getTrackInfo failed, aborting");
            e.printStackTrace();
            return false;
        }
        if (info == null) {
            Log.e(tag, "Call to getTrackInfo failed");
            return false;
        }
        return info.mLoved;
    }

    private Track preTrackAction(Track orig_track) throws
            InvalidObjectException {
        // We do this so that we're always loving the auto-corrected version.
        // It does slow things down a little though.
        return getTrackInfo(orig_track);
    }

    public Track getTrackInfo(Track orig_track) throws InvalidObjectException {
        final String tag = "LastfmSession.getTrackInfo";
        Log.d(tag, "Getting info of " + orig_track.toString());
        if (!isLoggedIn()) {
            throw(new IllegalStateException("Session is not logged in"));
        }
        if (orig_track.mArtist == "" || orig_track.mTitle == "") {
            final String msg = "Track has no artist or title";
            throw new InvalidObjectException(msg);
        }
        Map<String, String> rest_params = new HashMap<>();
        rest_params.put("method", "track.getInfo");
        rest_params.put("artist", orig_track.mArtist);
        rest_params.put("track", orig_track.mTitle);
        // Always autocorrect: if you want the original name, you have it
        rest_params.put("autocorrect", "1");
        rest_params.put("username", mUsername);
        String urlString = mUrlMaker.fromHashmap(rest_params);
        Map<String, List<String>> list_map = new HashMap<>();
        list_map.put("loved", Arrays.asList("lfm", "track", "userloved"));
        list_map.put("artist", Arrays.asList("lfm", "track", "artist", "name"));
        list_map.put("title", Arrays.asList("lfm", "track", "name"));
        XmlPullParser parser;
        try {
            parser = getUrlResponse(urlString);
            if (parser == null) {
                Log.e(tag, "getUrlResponse invalid");
                return null;
            }
            // We get a list, but there's only one item in it
            List<Map<String,String>> results = getTagsFromLists(parser,
                    list_map);
            if (results.size() != 1) {
                Log.wtf(tag, "Got more than one result");
            }
            Map<String, String> map = results.get(0);
            Log.d(tag, map.toString());
            return new Track(map);
        }
        catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public TagList getGlobalTags() {
        final String tag = "LastfmSession.getGlobalTags";
        if (!isLoggedIn()) {
            throw(new IllegalStateException("Session is not logged in"));
        }
        Map<String, String> rest_params = new HashMap<>();
        rest_params.put("method", "user.getTopTags");
        rest_params.put("user", mUsername);
        rest_params.put("limit", "10");
        String urlString = mUrlMaker.fromHashmap(rest_params);
        Map<String, List<String>> list_map = new HashMap<>();
        list_map.put("tag", Arrays.asList("lfm", "toptags", "tag", "name"));
        XmlPullParser parser;
        TagList topTags = new TagList();
        try {
            parser = getUrlResponse(urlString);
            for (Map<String, String> map : getTagsFromLists(parser, list_map)) {
                Log.d(tag, map.toString());
                topTags.add(new Tag(map.get("tag")));
            }
        }
        catch (Exception e) {
            e.printStackTrace();
            topTags.mValid = false;
        }
        return topTags;
    }

    private List<Map<String, String>> getTagsFromLists(XmlPullParser parser,
                                  Map<String, List<String>> tag_list_map)
                                  throws XmlPullParserException, IOException {
        final String tag = "LastfmSession.getTagsFromLists";
        int eventType = parser.getEventType();
        List<String> CurrentPos = new ArrayList<>();
        List<Map<String, String>> returnList = new ArrayList<>();
        Map<String, String> oneMap = new HashMap<>();

        String name;
        while(eventType != XmlPullParser.END_DOCUMENT) {
            switch(eventType) {
                case XmlPullParser.START_TAG:
                    name = parser.getName();
                    CurrentPos.add(name);
                    break;
                case XmlPullParser.END_TAG:
                    CurrentPos.remove(CurrentPos.size() - 1);
                    break;
                case XmlPullParser.TEXT:
                    for (Map.Entry<String, List<String>> entry :
                                                    tag_list_map.entrySet()) {
                        if(CurrentPos.equals(entry.getValue())) {
                            String text = parser.getText();
                            Log.v(tag, "Matched: " + CurrentPos.toString());
                            Log.v(tag, "Matched: " + text);
                            oneMap.put(entry.getKey(), text);
                            if(oneMap.size() == tag_list_map.size()) {
                                returnList.add(oneMap);
                                oneMap = new HashMap<>();
                            }
                        }
                    }
                    break;
            }
            eventType = parser.next();
        }
        Log.d(tag, "Returning: " + returnList.toString());
        return returnList;
    }

    private void setSessionKey(String sk) {
        final String tag = "LastfmSession.setSessionKey";
        Log.d(tag, "Setting session key");
        mSettings.edit().putString(SESSION_KEY, sk).apply();
        mSessionKey = sk;
    }

    private void saveUsername(String username) {
        mSettings.edit().putString(USERNAME, username).apply();
        mUsername = username;
    }

    public boolean logIn(String username, String authToken) {
        Map<String, String> rest_params = new HashMap<>();
        rest_params.put("authToken", authToken);
        rest_params.put("method", "auth.getMobileSession");
        rest_params.put("username", username);
        String urlString;
        urlString = mUrlMaker.fromHashmap(rest_params);
        try {
            setSessionKey(getSessionKey(urlString));
            saveUsername(username);
        }
        catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public void logOut() {
        mSettings.edit().remove(SESSION_KEY).remove(USERNAME).apply();
    }

    private XmlPullParser getUrlResponse(String urlString) {
        final String tag = "LastfmSession.getUrlResponse";
        Log.v(tag, "url: " + urlString);

        InputStream in = null;
        XmlPullParserFactory pullParserFactory;
        try {
            pullParserFactory = XmlPullParserFactory.newInstance();
        }
        catch (XmlPullParserException e) {
            e.printStackTrace();
            return null;
        }
        XmlPullParser parser;
        short try_number = 1;
        URL url;
        try {
            url = new URL(urlString);
        } catch (MalformedURLException e) {
            Log.e(tag, "Malformed url: " + urlString);
            return null;
        }
        while (in == null) {
            try {
                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("POST");
                in = new BufferedInputStream(urlConnection.getInputStream());
            } catch (IOException e) {
                in = null;
                Log.e(tag, "Try number " + try_number +
                           " failed: " + e.getMessage());
                try_number += 1;
                if (try_number > RETRIES) {
                    Log.e(tag, "Failed after " + RETRIES + " attempts");
                    e.printStackTrace();
                    return null;
                }
            }
        }
        try {
            parser = pullParserFactory.newPullParser();
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
            parser.setInput(in, null);
        }
        catch (XmlPullParserException e) {
            e.printStackTrace();
            return null;
        }
        return parser;
    }

    private boolean getBoolean(String url) throws
            XmlPullParserException, IOException {
        final String tag = "LastfmSession.getBoolean";
        XmlPullParser parser;
        parser = getUrlResponse(url);
        Log.d(tag, "Got parser");
        int eventType = parser.getEventType();

        while(eventType != XmlPullParser.END_DOCUMENT) {
            String debug;
            debug = parser.getText();
            if (debug != null) {
                Log.i(tag, parser.getText());
            }
            switch(eventType) {
                case XmlPullParser.START_TAG:
                    if (1 != parser.getAttributeCount()) {
                        break;
                    }
                    if (!"status".equals(parser.getAttributeName(0))) {
                        break;
                    }
                    if ("ok".equals(parser.getAttributeValue(0))) {
                        return true;
                    }
                case XmlPullParser.END_TAG:
                    break;
            }
            eventType = parser.next();
        }
        return false;
    }

    private String getSessionKey(String url) throws
            XmlPullParserException, IOException {
        final String tag = "LastfmSession.getSessionKey";
        XmlPullParser parser;
        parser = getUrlResponse(url);
        Log.d(tag, "Got parser");
        int eventType = parser.getEventType();

        while(eventType != XmlPullParser.END_DOCUMENT) {
            String name;

            switch(eventType) {
                case XmlPullParser.START_TAG:
                    name = parser.getName();
                    Log.d(tag, "|" + name + "|");
                    if (name.equals("key")) {
                        String result = parser.nextText();
                        Log.d(tag, "Key is " + result);
                        return result;
                    }
                case XmlPullParser.END_TAG:
                    break;
            }
            eventType = parser.next();
        }
        throw(new IOException("Session key not found"));
    }
}

class UrlMaker {
    Context mContext;
    Md5Maker mMd5Maker;
    static final String BASE_URL = "https://ws.audioscrobbler.com/2.0/?";
    static final String URL_PARAM_SEP = "&";
    public UrlMaker() {
        mContext = App.getContext();
        mMd5Maker = new Md5Maker();
    }
    public String fromHashmap(Map<String, String> params) {
        final String tag = "LastfmSession.UrlMaker.fromHashmap";
        String api_key = mContext.getString(R.string.lastfm_api_key);
        params.put("api_key", api_key);
        if (params.containsKey("sk")||params.containsKey("authToken")) {
            params.put("api_sig", generateApiSig(params));
        }

        StringBuilder url = new StringBuilder();
        url.append(BASE_URL);
        String delim = "";
        for (String key : params.keySet()) {
            String raw_value = params.get(key);
            if (raw_value == null) {
                raw_value = "null";
            }
            String value;
            try {
                value = URLEncoder.encode(raw_value, "utf-8");
                url.append(delim).append(key).append("=").append(value);
                delim = URL_PARAM_SEP;
            }
            catch (UnsupportedEncodingException e) {
                Log.e(tag, "raw_value: " + raw_value);
                e.printStackTrace();
            }
        }
        Log.d(tag, "Returning url: " + url.toString());
        return url.toString();
    }
    private String generateApiSig(Map<String, String> params) {
        final String tag = "LastfmSession.UrlMaker.generateApiSig";
        List<String> keys = asSortedList(params.keySet());
        String secret = mContext.getString(R.string.lastfm_api_secret);
        String pre_md5 = "";
        for (String key : keys) {
            String value = params.get(key);
            if (value == null) {
                value = "null";
            }
            pre_md5 += key;
            pre_md5 += value;
        }
        pre_md5 += secret;
        Log.d(tag, "pre_md5: " + pre_md5);
        String api_sig = mMd5Maker.encode(pre_md5);
        Log.d(tag, "api_sig: " + api_sig);
        return api_sig;
    }
    public static <T extends Comparable<? super T>> List<T> asSortedList(Collection<T> c) {
        List<T> list = new ArrayList<>(c);
        java.util.Collections.sort(list);
        return list;
    }
}
