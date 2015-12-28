package com.thiagoh.stocks_monitor;

import java.util.Calendar;

import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

import com.thiagoh.stocks_monitor.util.Tools;
import com.thiagoh.stocks_monitor.util.Validator;

public class PreferenceCache {

	private static final Logger log = LoggerFactory.getLogger(PreferenceCache.class);

	private static String mCache = "";
	private SharedPreferences preferences = null;

	public PreferenceCache(Context context) {

		if (context != null)
			preferences = Tools.getAppPreferences(context);
	}

	public void put(String key, String data, Integer ttl) {

		// Get cache
		JSONObject cache = getCache();

		if (cache == null) {
			cache = new JSONObject();
		}

		// Set expiration based on ttl
		Calendar calendar = Calendar.getInstance();
		calendar.add(Calendar.SECOND, ttl);
		Long expiry = calendar.getTimeInMillis();

		// Update cache
		try {

			JSONObject item = new JSONObject();

			item.put("value", data);
			item.put("expiry", expiry);

			cache.put(key, item);

		} catch (JSONException e) {
			log.debug(e.getMessage(), e);
		}

		/** TODO: Clean up expired items **/

		// Save cache
		if (preferences != null) {

			mCache = cache.toString();

			Editor editor = preferences.edit();
			editor.putString(key, mCache);
			editor.commit();
		}
	}

	public String get(String key) {

		// Get cache
		JSONObject cache = getCache();

		if (cache == null) {
			return null;
		}

		// Get cached value
		try {

			JSONObject item = cache.getJSONObject(key);

			// Return null if we are expired
			Calendar calendar = Calendar.getInstance();
			if (item.getLong("expiry") < calendar.getTimeInMillis())
				return null;

			return item.getString("value");

		} catch (JSONException e) {
			if (log.isDebugEnabled()) {
				log.debug(e.getMessage(), e);
			}
		}

		return null;
	}

	JSONObject getCache() {

		// Get cache
		if (preferences != null && mCache.equals(""))
			mCache = preferences.getString("JSONcache", "");

		if (Validator.isNull(mCache)) {
			return null;
		}

		JSONObject cache = new JSONObject();

		try {

			cache = new JSONObject(mCache);

		} catch (JSONException e) {
			log.debug(e.getMessage(), e);
		}

		return cache;
	}
}
