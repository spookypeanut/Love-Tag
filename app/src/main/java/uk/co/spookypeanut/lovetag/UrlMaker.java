package uk.co.spookypeanut.lovetag;

import android.content.ClipData;
import android.content.Context;
import android.util.Log;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by hbush on 16/12/14.
 */
public class UrlMaker {
    Context mContext;
    Md5Maker mMd5Maker;
    public UrlMaker(Context context) {
        mContext = context;
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

