package uk.co.spookypeanut.lovetag;

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
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
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

    public LastfmSession() {
        mContext = App.getContext();
        String ss;
        ss = mContext.getString(R.string.session_setting);
        mSettings = mContext.getSharedPreferences(ss, mContext.MODE_MULTI_PROCESS);
        mUrlMaker = new UrlMaker();
        passiveLogin();
    }

    private void passiveLogin() {
        String tag = "Love&Tag.LastfmSession.passiveLogin";
        String ss = mContext.getString(R.string.session_setting);
        String us = mContext.getString(R.string.username_setting);
        if (mSettings.contains(ss)) {
            mSessionKey = mSettings.getString(ss, "");
            mUsername = mSettings.getString(us, "");
        } else {
            Log.i(tag, "Couldn't log in");
        }
    }

    public boolean isLoggedIn() {
        passiveLogin();
        return mSessionKey != "";
    }

    public boolean love(String artist, String track) {
        if (!isLoggedIn()) {
            throw(new IllegalStateException("Session is not logged in"));
        }
        Map<String, String> restparams = new HashMap<String, String>();
        restparams.put("method", "track.love");
        restparams.put("sk", mSessionKey);
        restparams.put("track", track);
        restparams.put("artist", artist);
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

    public boolean tag(String artist, String track, String tag_list) {
        if (!isLoggedIn()) {
            throw(new IllegalStateException("Session is not logged in"));
        }
        Map<String, String> rest_params = new HashMap<String, String>();
        rest_params.put("method", "track.addTags");
        rest_params.put("sk", mSessionKey);
        rest_params.put("track", track);
        rest_params.put("artist", artist);
        rest_params.put("tags", tag_list);
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

    public class RecentTrack {
        String mArtist;
        String mTitle;
        boolean mLoved;
        public RecentTrack(String artist, String title, boolean loved) {
            mArtist = artist;
            mTitle = title;
            mLoved = loved;
        }
        public RecentTrack(Map<String, String> params) {
            mArtist = params.get("artist");
            mTitle = params.get("title");
            if ("1".equals(params.get("loved"))) {
                mLoved = true;
            } else {
                mLoved = false;
            }
        }
    }

    public List<RecentTrack> getRecent () {
        String tag = "Love&Tag.LastfmSession.getRecent";
        if (!isLoggedIn()) {
            throw(new IllegalStateException("Session is not logged in"));
        }
        Map<String, String> rest_params = new HashMap<String, String>();
        rest_params.put("method", "user.getRecentTracks");
        rest_params.put("user", mUsername);
        // This is so we get loved information too
        rest_params.put("extended", "1");
        rest_params.put("limit", "10");
        String urlString = mUrlMaker.fromHashmap(rest_params);
        Log.i(tag, "Got url back");
        Map<String, List<String>> list_map = new HashMap<String, List<String>>();
        list_map.put("artist", Arrays.asList("lfm", "recenttracks",
                                             "track", "artist", "name"));
        list_map.put("title", Arrays.asList("lfm", "recenttracks",
                                            "track", "name"));
        list_map.put("loved", Arrays.asList("lfm", "recenttracks",
                "track", "loved"));

        XmlPullParser parser;
        List<RecentTrack> recentTracks = new ArrayList<>();
        try {
            parser = getUrlResponse(urlString);
            for (Map<String, String> map : getTagsFromLists(parser, list_map)) {
                recentTracks.add(new RecentTrack(map));
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return recentTracks;
    }

    private List<Map<String, String>> getTagsFromLists(XmlPullParser parser,
                                  Map<String, List<String>> tag_list_map)
                                  throws XmlPullParserException, IOException {
        String tag = "Love&Tag.LastfmSession.getTagsFromLists";
        int eventType = parser.getEventType();
        List<String> CurrentPos = new ArrayList<>();
        List<Map<String, String>> returnList = new ArrayList<>();
        Map<String, String> oneMap = new HashMap<String, String>();

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
                                oneMap = new HashMap<String, String>();
                            }
                        }
                    }
                    break;
            }
            eventType = parser.next();
        }
        Log.i(tag, returnList.toString());
        return returnList;
    }

    private void setSessionKey(String sk) {
        String ss;
        String tag = "Love&Tag.LastfmSession.setSessionKey";
        Log.i(tag, "Setting session key: " + sk);
        ss = mContext.getString(R.string.session_setting);
        mSettings.edit().putString(ss, sk).commit();
        mSessionKey = sk;
    }

    private void saveUsername(String username) {
        String us = mContext.getString(R.string.username_setting);
        mSettings.edit().putString(us, username).commit();
        mUsername = username;
    }

    public boolean logIn(String username, String authToken) {
        String tag = "Love&Tag.LastfmSession.logIn";
        Map<String, String> rest_params = new HashMap<String, String>();
        rest_params.put("authToken", authToken);
        rest_params.put("method", "auth.getMobileSession");
        rest_params.put("username", username);
        String urlString;
        urlString = mUrlMaker.fromHashmap(rest_params);
        Log.i(tag, "log in url: " + urlString);
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

    protected XmlPullParser getUrlResponse(String urlString) {
        String tag = "Love&Tag.LastfmSession.getUrlResponse";
        Log.d(tag, "url: " + urlString);

        InputStream in;
        XmlPullParserFactory pullParserFactory;
        XmlPullParser parser;
        parser = null;
        try {
            pullParserFactory = XmlPullParserFactory.newInstance();
        }
        catch (XmlPullParserException e) {
            e.printStackTrace();
            return parser;
        }

        try {
            URL url = new URL(urlString);
            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestMethod("POST");
            urlConnection.setRequestProperty("User-Agent","Mozilla/5.0 ( compatible ) ");
            urlConnection.setRequestProperty("Accept","*_/*"); // Remove the underscore
            in = new BufferedInputStream(urlConnection.getInputStream());
        } catch (Exception e) {
            Log.e(tag, "Exception: " + e.getMessage());
            e.printStackTrace();
            return parser;
        }
        try {
            parser = pullParserFactory.newPullParser();
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
            parser.setInput(in, null);
            return parser;
        }
        catch (XmlPullParserException e) {
            e.printStackTrace();
        }
        return parser;
    }
    private boolean getBoolean(String url) throws
            XmlPullParserException, IOException {
        String tag = "Love&Tag.LastfmSession.getBoolean";
        XmlPullParser parser;
        parser = getUrlResponse(url);
        Log.d(tag, "Got parser");
        int eventType = parser.getEventType();

        while(eventType != XmlPullParser.END_DOCUMENT) {
            String name;
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
        String tag = "Love&Tag.LastfmSession.getSessionKey";
        XmlPullParser parser;
        parser = getUrlResponse(url);
        Log.d(tag, "Got parser");
        int eventType = parser.getEventType();

        while(eventType != XmlPullParser.END_DOCUMENT) {
            String name;

            switch(eventType) {
                case XmlPullParser.START_TAG:
                    name = parser.getName().toString();
                    Log.d(tag, "|" + name + "|");
                    if (name.equals("key")) {
                        Log.i(tag, "Found key");
                        String result = parser.nextText();
                        Log.i(tag, "Key is " + result);
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
    public UrlMaker() {
        mContext = App.getContext();
        mMd5Maker = new Md5Maker();
    }
    public String fromHashmap(Map<String, String> params) {
        String tag = "Love&Tag.LastfmSession.UrlMaker.fromHashmap";
        String api_key = mContext.getString(R.string.lastfm_api_key);
        params.put("api_key", api_key);
        if (params.containsKey("sk")||params.containsKey("authToken")) {
            params.put("api_sig", generateApiSig(params));
        }

        StringBuilder url = new StringBuilder();
        url.append(mContext.getString(R.string.base_url));
        List combined = new ArrayList();
        String delim = "";
        for (String key : params.keySet()) {
            String raw_value = (String) params.get(key);
            if (raw_value == null) {
                raw_value = "null";
            }
            String value;
            try {
                value = URLEncoder.encode(raw_value, "utf-8");
                url.append(delim).append(key).append("=").append(value);
                delim = mContext.getString(R.string.url_param_separator);
            }
            catch (UnsupportedEncodingException e) {
                Log.e(tag, "raw_value: " + raw_value);
                e.printStackTrace();
            }
        }
        return url.toString();
    }
    private String generateApiSig(Map<String, String> params) {
        List<String> keys = asSortedList(params.keySet());
        String secret = mContext.getString(R.string.lastfm_api_secret);
        String pre_md5 = "";
        for (String key : keys) {
            String value = (String) params.get(key);
            if (value == null) {
                value = "null";
            }
            pre_md5 += key;
            pre_md5 += value;
        }
        pre_md5 += secret;
        Log.d("Love&Tag.UrlMaker", "pre_md5: " + pre_md5);
        String api_sig = mMd5Maker.encode(pre_md5);
        Log.d("Love&Tag.UrlMaker", "api_sig: " + api_sig);
        return api_sig;
    }
    public static <T extends Comparable<? super T>> List<T> asSortedList(Collection<T> c) {
        List<T> list = new ArrayList<T>(c);
        java.util.Collections.sort(list);
        return list;
    }
}
