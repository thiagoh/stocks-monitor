/*
 The MIT License

 Copyright (c) 2015 Thiago Andrade http://github.com/thiagoh/stocks

 Permission is hereby granted, free of charge, to any person obtaining a copy
 of this software and associated documentation files (the "Software"), to deal
 in the Software without restriction, including without limitation the rights
 to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 copies of the Software, and to permit persons to whom the Software is
 furnished to do so, subject to the following conditions:

 The above copyright notice and this permission notice shall be included in
 all copies or substantial portions of the Software.

 THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 THE SOFTWARE.
 */

package com.thiagoh.stocks_monitor;

import android.content.Context;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class StockSuggestions {

	private static final Logger log = LoggerFactory.getLogger(StockSuggestions.class);

	//private static final String BASE_URL = "http://d.yimg.com/autoc.finance.yahoo.com/autoc?callback=YAHOO.Finance.SymbolSuggest.ssCallback&query=";

	private static final String BASE_URL = "http://d.yimg.com/aq/autoc?callback=YAHOO.util.ScriptNodeDataSource.callbacks";
	private static final Pattern PATTERN_RESPONSE = Pattern.compile("YAHOO\\.util\\.ScriptNodeDataSource\\.callbacks\\((\\{.*?\\})\\)");

	private static final String[][] LANGS_REGIONS = {
			{"pt-BR", "BR"},
			{"en-US", "US"},
	};

	// Example
	//http://d.yimg.com/aq/autoc?query=petr&region=BR&lang=pt-BR&callback=YAHOO.util.ScriptNodeDataSource.callbacks

	private static String getUrl(String lang, String region, String query) throws UnsupportedEncodingException {

		String url = BASE_URL;

		url += "&lang=" + URLEncoder.encode(lang, "UTF-8");
		url += "&region=" + URLEncoder.encode(region, "UTF-8");
		url += "&query=" + URLEncoder.encode(query, "UTF-8");

		return url;
	}

	public static List<Company> getSuggestions(Context context, String query) {

		List<Company> suggestions = new ArrayList<>();

		JSONArray array = getResults(context, query);

		if (array == null) {
			return suggestions;
		}

		for (int i = 0; i < array.length(); i++) {

			try {

				JSONObject json = array.getJSONObject(i);

				Company company = new Company(json.getString("symbol"), json.getString("name"));

				suggestions.add(company);

			} catch (JSONException e) {
				log.debug(e.getMessage(), e);
			}
		}

		return suggestions;
	}

	public static JSONArray getResults(Context context, String query) {

		JSONArray json = null;

		for (int i = 0; i < LANGS_REGIONS.length; i++) {

			String[] langRegion = LANGS_REGIONS[i];

			try {

				String url = getUrl(langRegion[0], langRegion[1], query);

				String body = URLData.getURLData(context, url, 86400);

				if (Validator.isNotNull(body)) {

					Matcher m = PATTERN_RESPONSE.matcher(body);

					if (m.find()) {

						body = m.group(1);

						json = new JSONObject(body).getJSONObject("ResultSet").getJSONArray("Result");

						return json;
					}
				}

			} catch (Exception e) {
				log.debug(e.getMessage(), e);
			}
		}

		return json;
	}
}
