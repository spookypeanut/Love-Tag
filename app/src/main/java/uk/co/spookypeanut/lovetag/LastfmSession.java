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
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LastfmSession {
    Context mContext;
    SharedPreferences mSettings;
    String mSessionKey = "";
    UrlMaker mUrlMaker;

    public LastfmSession() {
        mContext = App.getContext();
        mSettings = mContext.getSharedPreferences("LastfmSession", mContext.MODE_MULTI_PROCESS);
        mUrlMaker = new UrlMaker();
        if (mSettings.contains("sessionKey")) {
            mSessionKey = mSettings.getString("session_key", "");
        }
    }

    public boolean isLoggedIn() {
        return mSessionKey != "";
    }

    public boolean logIn(String username, String authToken) {
        String tag = "Love&Tag.LastfmSession.logIn";
        Map<String, String> restparams = new HashMap<String, String>();
        restparams.put("authToken", authToken);
        restparams.put("method", "auth.getMobileSession");
        restparams.put("username", username);
        String urlString;
        urlString = mUrlMaker.from_hashmap(restparams);
        Log.i(tag, "log in url: " + urlString);
        try {
            mSessionKey = getSessionKey(urlString);
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

        InputStream in = null;
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
    private String getSessionKey(String url) throws
            XmlPullParserException, IOException {
        String tag = "Love&Tag.LastfmSession.parseXML";
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
    public String from_hashmap(Map<String, String> params) {
        if ((String) params.get("method") == "auth.getMobileSession") {
            String api_key = mContext.getString(R.string.lastfm_api_key);
            params.put("api_key", api_key);
        }
        params.put("api_sig", generate_api_sig(params));

        StringBuilder url = new StringBuilder();
        url.append(mContext.getString(R.string.base_url));
        List combined = new ArrayList();
        String delim = "";
        for (String key : params.keySet()) {
            String value = (String) params.get(key);
            if (value == null) {
                value = "null";
            }
            url.append(delim).append(key).append("=").append(value);
            delim = mContext.getString(R.string.url_param_separator);
        }
        return url.toString();
    }
    private String generate_api_sig(Map<String, String> params) {
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
