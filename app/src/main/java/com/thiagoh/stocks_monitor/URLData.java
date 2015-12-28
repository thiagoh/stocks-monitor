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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.content.Context;

class URLData {

	/* URL data retrieval that supports caching */
	public static String getURLData(Context context, String url, Integer ttl) {

		PreferenceCache cache = new PreferenceCache(context);
		String data = null;

		// Return cached data if we have it
		if (ttl != null) {
			data = cache.get(url);
			if (data != null)
				return data;
		}

		// Get the data and update the cache
		data = _getURLData(url);

		if (data != null) {
			cache.put(url, data, ttl);
			return data;
		}

		return "";
	}

	/* URL data retrieval without caching */
	private static String _getURLData(String url) {

		// Ensure we always request some data
		if (!url.contains("INDU"))
			url = url.replace("&s=", "&s=INDU+");

		// Grab the data from the source
		String response = null;
		try {

			// Set connection timeout and socket timeout
			URLConnection connection = new URL(url).openConnection();
			connection.setConnectTimeout(30000);
			connection.setReadTimeout(60000);
			InputStream stream = connection.getInputStream();

			// Read information out of input stream
			BufferedReader r = new BufferedReader(new InputStreamReader(stream));
			StringBuilder builder = new StringBuilder();

			String line = null;

			while ((line = r.readLine()) != null)
				builder.append(line).append("\n");

			response = builder.toString();

		} catch (IOException e) {
			log.debug(e.getMessage(), e);
		}

		return response;
	}

	private static final Logger log = LoggerFactory.getLogger(URLData.class);
}
