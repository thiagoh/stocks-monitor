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

package com.thiagoh.stocks_monitor.activities;

import android.app.SearchManager;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.widget.TimePicker;

import com.google.android.gms.common.api.GoogleApiClient;
import com.thiagoh.stocks_monitor.R;
import com.thiagoh.stocks_monitor.util.Tools;
import com.thiagoh.stocks_monitor.util.UserData;
import com.thiagoh.stocks_monitor.util.Validator;
import com.thiagoh.stocks_monitor.widget.WidgetBase;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;

public class Preferences extends PreferenceActivity implements SharedPreferences.OnSharedPreferenceChangeListener {

	private static final Logger log = LoggerFactory.getLogger(Preferences.class);

	public static final String PREFS_NAME = "stocks_monitor_prefs_file";

	// Constants
	private static final int STRING_TYPE = 0;
	private static final int LIST_TYPE = 1;
	private static final int CHECKBOX_TYPE = 2;
	// Public variables
	public static int mAppWidgetId = 0;
	// Private
	private static boolean mStocksDirty = false;
	private static String mSymbolSearchKey = "";
	// Fields for time pickers
	private TimePickerDialog.OnTimeSetListener mTimeSetListener;
	private String mTimePickerKey = null;
	private int mHour = 0;
	private int mMinute = 0;

	String getChangeLog() {

		return getString(R.string.change_log);
	}

	@Override
	public void onNewIntent(Intent intent) {

		if (Intent.ACTION_VIEW.equals(intent.getAction())) {

			setPreference(mSymbolSearchKey, intent.getDataString(), intent.getStringExtra(SearchManager.EXTRA_DATA_KEY));

		} else if (Intent.ACTION_SEARCH.equals(intent.getAction())) {

			String query = intent.getStringExtra(SearchManager.QUERY);
			startSearch(query, false, null, false);

		} else if (Intent.ACTION_EDIT.equals(intent.getAction())) {

			String query = intent.getStringExtra(SearchManager.QUERY);
			startSearch(query, false, null, false);
		}
	}

	public SharedPreferences getSharedPreferences() {

		return Tools.getAppPreferences(getApplicationContext());
	}

	@Override
	protected void onPause() {

		super.onPause();

		// Unregister the listener whenever a key changes
		getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
	}

	private void removePref(PreferenceScreen screen, String name) {

		try {

			screen.removePreference(findPreference(name));

		} catch (Exception ignored) {
		}
	}

	private void removePref(String screenName, String name) {

		PreferenceScreen screen = (PreferenceScreen) findPreference(screenName);
		try {
			screen.removePreference(findPreference(name));

		} catch (Exception ignored) {
		}
	}

	private void showRecentChanges() {

		// Return if the change log has already been viewed
		if (Tools.getAppPreferences(getApplicationContext()).getString("change_log_viewed", "").equals(Tools.BUILD_NUMBER)) {
			return;
		}

		// Cleanup preferences files
		UserData.cleanupPreferenceFiles(getApplicationContext());

		@SuppressWarnings("rawtypes")
		Callable callable = new Callable() {

			@Override
			public Object call() throws Exception {

				// Ensure we don't show this again
				SharedPreferences appPreferences = Tools.getAppPreferences(getApplicationContext());
				SharedPreferences.Editor editor = appPreferences.edit();
				editor.putString("change_log_viewed", Tools.BUILD_NUMBER);

				// Set first install if not set
				if (appPreferences.getString("install_date", "").equals("")) {
					editor.putString("install_date", new SimpleDateFormat("yyyyMMdd").format(new Date()).toUpperCase());
				}

				editor.commit();

				return new Object();
			}
		};

		Tools.alertWithCallback(this, "Build " + Tools.BUILD_NUMBER, getChangeLog(), "Fechar", null, callable);
	}

	@Override
	protected void onResume() {

		super.onResume();

		showRecentChanges();

		// Add this widgetId if we don't have it
		Set<Integer> appWidgetIds = new HashSet<Integer>();

		for (int i : UserData.getAppWidgetIds2(getBaseContext())) {
			appWidgetIds.add(i);
		}

		if (!appWidgetIds.contains(mAppWidgetId)) {
			UserData.addAppWidgetId(getBaseContext(), mAppWidgetId);
		}

		// Hide preferences for certain widget sizes
		int widgetSize = UserData.getAppWidgetSize(getBaseContext(), mAppWidgetId);

		PreferenceScreen stock_setup = (PreferenceScreen) findPreference("stock_setup");

		// Remove extra stocks_monitor
		if (widgetSize == 0 || widgetSize == 1) {
			for (int i = 5; i < 21; i++) {
				removePref(stock_setup, "Stock" + i);
			}
		} else if (widgetSize == 3) {
			for (int i = 11; i < 21; i++) {
				removePref(stock_setup, "Stock" + i);
			}
		}

		// Remove extra widget views
		if (widgetSize == 1 || widgetSize == 3 || widgetSize == 4) {

			PreferenceScreen widget_views = (PreferenceScreen) findPreference("widget_views");
			removePref(widget_views, "show_percent_change");
			removePref(widget_views, "show_portfolio_change");
			removePref(widget_views, "show_profit_daily_change");
			removePref(widget_views, "show_profit_change");
		}

		// Hide Feedback option if not relevant
		String install_date = Tools.getAppPreferences(getApplicationContext()).getString("install_date", null);
		if (Tools.elapsedDays(install_date) < 30) {
			removePref("about_menu", "rate_app");
		}

		SharedPreferences sharedPreferences = Tools.getAppPreferences(getApplicationContext());
		SharedPreferences widgetPreferences = Tools.getWidgetPreferences(getApplicationContext(), mAppWidgetId);

		// Initialise the summaries when the preferences screen loads
		Map<String, ?> sharedPreferencesMap = sharedPreferences.getAll();
		Map<String, ?> widgetPreferencesMap = widgetPreferences.getAll();

		for (String key : sharedPreferencesMap.keySet()) {
			updateSummaries(sharedPreferences, key);
		}

		for (String key : widgetPreferencesMap.keySet()) {
			updateSummaries(widgetPreferences, key);
		}

		// Update version number
		findPreference("version").setSummary("BUILD " + Tools.BUILD_NUMBER);

		// Force update of global preferences
		// TODO Ensure the items below are included in the above list
		// rather than updating these items twice (potentially)
		updateSummaries(sharedPreferences, "update_interval");
		updateSummaries(sharedPreferences, "update_start");
		updateSummaries(sharedPreferences, "update_end");
		updateSummaries(sharedPreferences, "update_weekend");

		// Set up a listener whenever a key changes
		sharedPreferences.registerOnSharedPreferenceChangeListener(this);
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {

		// Perform some custom handling of some values
		if (key.startsWith("Stock") && !key.endsWith("_summary")) {
			updateStockValue(sharedPreferences, key);

			// Mark stock changed as dirty
			mStocksDirty = true;

		} else if (key.equals("update_interval")) {
			updateGlobalPref(sharedPreferences, key, LIST_TYPE);

			// Warning massage if necessary
			if (sharedPreferences.getString(key, "").equals("900000") || sharedPreferences.getString(key, "").equals("300000")) {

				String title = getString(R.string.short_update_interval);
				String body = getString(R.string.choosing_short_interval_drain_battery);
				Tools.showSimpleDialog(this, title, body);
			}

		} else if (key.equals("update_start") || key.equals("update_end")) {
			updateGlobalPref(sharedPreferences, key, STRING_TYPE);

		} else if (key.equals("update_weekend")) {
			updateGlobalPref(sharedPreferences, key, CHECKBOX_TYPE);
		}

		// Update the summary whenever the preference is changed
		updateSummaries(sharedPreferences, key);
	}

	void updateStockValue(SharedPreferences sharedPreferences, String key) {

		// Unregister the listener whenever a key changes
		getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);

		// Massages the value: remove whitespace and upper-case
		String value = sharedPreferences.getString(key, "");
		value = value.replace(" ", "");
		value = value.toUpperCase();

		SharedPreferences.Editor editor = sharedPreferences.edit();
		editor.putString(key, value);
		editor.commit();

		// Also update the UI
		EditTextPreference preference = (EditTextPreference) findPreference(key);
		preference.setText(value);

		// Set up a listener whenever a key changes
		sharedPreferences.registerOnSharedPreferenceChangeListener(this);
	}

	void updateFromGlobal(SharedPreferences sharedPreferences, String key, int valType) {

		log.debug("Preferences updateFromGlobal: Putting global preference with key: " + key);

		// Unregister the listener whenever a key changes
		getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);

		// Update the widget preferences with the interval
		SharedPreferences.Editor editor = sharedPreferences.edit();

		SharedPreferences appPreference = Tools.getAppPreferences(getApplicationContext());

		if (valType == STRING_TYPE) {
			String value = appPreference.getString(key, "");
			if (!value.equals("")) {
				editor.putString(key, value);
			}

		} else if (valType == LIST_TYPE) {
			String value = appPreference.getString(key, "");
			if (!value.equals("")) {
				editor.putString(key, value);
				((ListPreference) findPreference(key)).setValue(value);
			}

		} else if (valType == CHECKBOX_TYPE) {
			Boolean value = appPreference.getBoolean(key, false);
			editor.putBoolean(key, value);

			CheckBoxPreference pref = ((CheckBoxPreference) findPreference(key));

			if (pref != null) {
				pref.setChecked(value);
			} else {
				log.warn("Preference with key " + key + " was not found");
			}
		}

		editor.commit();

		// Set up a listener whenever a key changes
		sharedPreferences.registerOnSharedPreferenceChangeListener(this);
	}

	void updateGlobalPref(SharedPreferences sharedPreferences, String key, int valType) {

		log.debug("Preferences updateGlobalPref: Putting global preference with key: " + key);

		// Unregister the listener whenever a key changes
		getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);

		SharedPreferences appPreference = Tools.getAppPreferences(getApplicationContext());

		// Update the global preferences with the widget update interval
		SharedPreferences.Editor editor = appPreference.edit();

		if (valType == STRING_TYPE || valType == LIST_TYPE)
			editor.putString(key, sharedPreferences.getString(key, ""));

		else if (valType == CHECKBOX_TYPE)
			editor.putBoolean(key, sharedPreferences.getBoolean(key, false));

		editor.commit();

		// Set up a listener whenever a key changes
		sharedPreferences.registerOnSharedPreferenceChangeListener(this);
	}

	void showDisclaimer() {

		String title = getString(R.string.license);
		String body = getString(R.string.disclaimer);
		Tools.showSimpleDialog(this, title, body);
	}

	void showHelp() {

		String title = getString(R.string.addingStocks);
		String body = getString(R.string.help_enter_stocs);
		Tools.showSimpleDialog(this, title, body);
	}

	void showHelpPrices() {

		String title = getString(R.string.updatingPrices);
		String body = getString(R.string.update_help);
		Tools.showSimpleDialog(this, title, body);
	}

	void showTimePickerDialog(Preference preference, String defaultValue) {

		// Get the raw value from the preferences
		String value = getSharedPreferences().getString(preference.getKey(), defaultValue);

		mHour = 0;
		mMinute = 0;

		if (value != null && !value.equals("")) {
			String[] items = value.split(":");
			mHour = Integer.parseInt(items[0]);
			mMinute = Integer.parseInt(items[1]);
		}

		mTimePickerKey = preference.getKey();
		new TimePickerDialog(this, mTimeSetListener, mHour, mMinute, true).show();
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {

		if (requestCode != 1) {
			super.onActivityResult(requestCode, resultCode, data);
		}
	}

	void setTimePickerPreference(int hourOfDay, int minute) {

		// Set the preference value
		SharedPreferences preferences = getSharedPreferences();

		SharedPreferences.Editor editor = preferences.edit();
		editor.putString(mTimePickerKey, String.valueOf(hourOfDay) + ":" + String.valueOf(minute));
		editor.commit();

		// Also update the UI
		updateSummaries(getSharedPreferences(), mTimePickerKey);
	}

	void setPreference(String key, String value, String summary) {

		if (Validator.isNull(key)) {
			return;
		}

		log.debug("Preferences setPreference: Putting preference with key: " + key);

		// Set the stock value
		SharedPreferences preferences = Tools.getWidgetPreferences(getBaseContext(), mAppWidgetId);

		// Ignore the remove and manual entry options
		if (value.endsWith(getString(R.string.andClose))) {
			value = "";
		} else if (value.startsWith(getString(R.string.use))) {
			value = value.replace(getString(R.string.use), "");
		}

		Preference preferenceScreen = findPreference(key);

		if (preferenceScreen != null) {
			log.debug("Preference screen " + preferenceScreen);
			preferenceScreen.setTitle(value);
			preferenceScreen.setSummary(summary);
		}

		// Set dirty
		mStocksDirty = true;
		preferences.edit().putString(key, value).putString(key + "_summary", summary).commit();
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);

		addPreferencesFromResource(R.xml.preferences);

		configStocks();

		configAbout();

		configHelp();

		configHelpUsage();

		configHelpPortfolio();

		configHelpPrices();

		configUpdateNow();

		configPortfolio();

		configRateApp();

		configFeedback();

		configChangeHistory();

		configUpdateInterval();

		configUpdateStart();

		configUpdateEnd();


		/*
		 * // Hook the Backup portfolio option to the backup portfolio method
		 * Preference backup = findPreference("backup_portfolio");
		 * backup.setOnPreferenceClickListener(new OnPreferenceClickListener() {
		 *
		 * @Override public boolean onPreferenceClick(Preference preference) {
		 * UserData.backupPortfolio(getApplicationContext());
		 *
		 * Intent intent = new Intent(Preferences.this, Portfolio.class);
		 * startActivity(intent); return true; } }); // Hook the Restore
		 * portfolio option to the restore portfolio method Preference restore =
		 * findPreference("restore_portfolio");
		 * restore.setOnPreferenceClickListener(new OnPreferenceClickListener()
		 * {
		 *
		 * @Override public boolean onPreferenceClick(Preference preference) {
		 * UserData.restorePortfolio(getApplicationContext());
		 *
		 * Intent intent = new Intent(Preferences.this, Portfolio.class);
		 * startActivity(intent); return true; } });
		 */

		// Callback received when the user sets the time in the dialog
		mTimeSetListener = new TimePickerDialog.OnTimeSetListener() {

			@Override
			public void onTimeSet(TimePicker view, int hourOfDay, int minute) {

				setTimePickerPreference(hourOfDay, minute);
			}
		};
	}

	private void configUpdateEnd() {
		Preference update_end = findPreference("update_end");

		if (update_end == null) {
			return;
		}

		update_end.setOnPreferenceClickListener(new OnPreferenceClickListener() {

			@Override
			public boolean onPreferenceClick(Preference preference) {

				showTimePickerDialog(preference, "23:59");
				return true;
			}
		});
	}

	private void configUpdateStart() {
		// Hook the Update schedule preferences up
		Preference update_start = findPreference("update_start");
		if (update_start == null) {
			return;
		}

		update_start.setOnPreferenceClickListener(new OnPreferenceClickListener() {

			@Override
			public boolean onPreferenceClick(Preference preference) {

				showTimePickerDialog(preference, "00:00");
				return true;
			}
		});
	}

	private void configChangeHistory() {
		// Hook the Change history preference to the Change history dialog
		Preference change_history = findPreference("change_history");

		if (change_history == null) {
			return;
		}

		change_history.setOnPreferenceClickListener(new OnPreferenceClickListener() {

			@Override
			public boolean onPreferenceClick(Preference preference) {

				showChangeLog();
				return true;
			}
		});
	}

	private void configFeedback() {
		// Hook the Feedback preference to the Portfolio activity
		Preference feedback = findPreference("feedback");

		if (feedback == null) {
			return;
		}

		feedback.setOnPreferenceClickListener(new OnPreferenceClickListener() {

			@Override
			public boolean onPreferenceClick(Preference preference) {

				// Open the e-mail client with destination and subject
				Intent intent = new Intent(Intent.ACTION_SEND);
				intent.setType("message/rfc822");

				String[] toAddress = {getString(R.string.email_address_to)};
				intent.putExtra(Intent.EXTRA_EMAIL, toAddress);
				intent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.app_name) + " BUILD " + Tools.BUILD_NUMBER);
				intent.setType("message/rfc822");

				// In case we can't launch e-mail, show a dialog
				try {

					startActivity(intent);

					return true;

				} catch (Exception e) {
					log.debug(e.getMessage(), e);

				} catch (Throwable e) {
					log.debug(e.getMessage(), e);
				}

				// Show dialog if launching e-mail fails
				Tools.showSimpleDialog(getApplicationContext(), getString(R.string.impossible_start_email_client),
						getString(R.string.impossible_start_email_client_description));

				return true;
			}
		});
	}

	private void configRateApp() {
		// Hook Rate Stocks preference to the market link
		Preference rate_app = findPreference("rate_app");

		if (rate_app == null) {
			return;
		}

		rate_app.setOnPreferenceClickListener(new OnPreferenceClickListener() {

			@Override
			public boolean onPreferenceClick(Preference preference) {

				showFeedbackOption();
				return true;
			}
		});
	}

	private void configPortfolio() {
		// Hook the Portfolio preference to the Portfolio activity
		Preference portfolio = findPreference("portfolio");

		if (portfolio == null) {
			return;
		}

		portfolio.setOnPreferenceClickListener(new OnPreferenceClickListener() {

			@Override
			public boolean onPreferenceClick(Preference preference) {

				Intent intent = new Intent(Preferences.this, Portfolio.class);
				startActivity(intent);
				return true;
			}
		});
	}

	private void configUpdateInterval() {
		// Hook the Update preference to the Help activity
		Preference updateInterval = findPreference("update_interval");

		if (updateInterval == null) {
			return;
		}

		updateInterval.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {

			@Override
			public boolean onPreferenceChange(Preference preference, Object newValue) {

				// Set the preference value
				SharedPreferences preferences = getSharedPreferences();

				SharedPreferences.Editor editor = preferences.edit();
				editor.putString(preference.getKey(), String.valueOf(newValue));
				editor.commit();

				// Also update the UI
				updateSummaries(getSharedPreferences(), preference.getKey());

				return true;
			}
		});
	}


	private void configUpdateNow() {
		// Hook the Update preference to the Help activity
		Preference updateNow = findPreference("update_now");

		if (updateNow == null) {
			return;
		}

		updateNow.setOnPreferenceClickListener(new OnPreferenceClickListener() {

			@Override
			public boolean onPreferenceClick(Preference preference) {

				// Update all widgets and quit
				WidgetBase.updateWidgets(getApplicationContext(), WidgetBase.VIEW_UPDATE);

				finish();
				return true;
			}
		});
	}

	private void configHelpPrices() {
		// Hook the Help preference to the Help activity
		Preference help_prices = findPreference("help_prices");

		if (help_prices == null) {
			return;
		}

		help_prices.setOnPreferenceClickListener(new OnPreferenceClickListener() {

			@Override
			public boolean onPreferenceClick(Preference preference) {

				showHelpPrices();
				return true;
			}
		});
	}

	private void configHelpPortfolio() {
		// Hook the Help preference to the Help activity
		Preference help_portfolio = findPreference("help_portfolio");

		if (help_portfolio == null) {
			return;
		}

		help_portfolio.setOnPreferenceClickListener(new OnPreferenceClickListener() {

			@Override
			public boolean onPreferenceClick(Preference preference) {

				showHelpPortfolio();
				return true;
			}
		});
	}

	private void configHelpUsage() {
		// Hook up the help preferences
		Preference help_usage = findPreference("help_usage");

		if (help_usage == null) {
			return;
		}

		help_usage.setOnPreferenceClickListener(new OnPreferenceClickListener() {

			@Override
			public boolean onPreferenceClick(Preference preference) {

				showHelpUsage();
				return true;
			}
		});
	}

	private void configStocks() {
		// Hook up the symbol search for the stock preferences
		for (int i = 1; i < 21; i++) {

			String key = "Stock" + i;

			Preference preference = findPreference(key);

			if (preference == null) {
				continue;
			}

			preference.setOnPreferenceClickListener(new OnPreferenceClickListener() {

				@Override
				public boolean onPreferenceClick(Preference preference) {

					mSymbolSearchKey = preference.getKey();

					// Start search with current value as query
					String query = getSharedPreferences().getString(mSymbolSearchKey, "");
					startSearch(query, false, null, false);

					return true;
				}
			});
		}
	}

	private void configHelp() {
		// Hook up the help preferences
		Preference help = findPreference("help");

		if (help == null) {
			return;
		}

		help.setOnPreferenceClickListener(new OnPreferenceClickListener() {

			@Override
			public boolean onPreferenceClick(Preference preference) {

				showHelp();
				return true;
			}
		});
	}

	private void configAbout() {
		// Hook the About preference to the About (Stocks) activity
		Preference about = findPreference("about");

		if (about == null) {
			return;
		}

		about.setOnPreferenceClickListener(new OnPreferenceClickListener() {

			@Override
			public boolean onPreferenceClick(Preference preference) {

				showDisclaimer();
				return true;
			}
		});
	}

	void updateSummaries(SharedPreferences sharedPreferences, String key) {

		Preference preference = findPreference(key);

		// Initialise the Stock summaries
		if (key.startsWith("Stock") && !key.endsWith("_summary")) {

			// Update the summary based on the stock value
			String value = sharedPreferences.getString(key, "");
			String summary = sharedPreferences.getString(key + "_summary", "");

			// Set the title
			if (value.equals("")) {
				value = key.replace("Stock", "Stock ");
				summary = "Set symbol";
			}
			// Set the summary appropriately
			else if (summary.equals("")) {
				summary = "No description";
			}

			if (preference != null) {
				preference.setTitle(value);
				preference.setSummary(summary);
			}

			// Initialise the ListPreference summaries
		} else if (key.startsWith("background") || key.startsWith("updated_colour") || key.startsWith("updated_display")
				|| key.startsWith("text_style")) {

			String value = sharedPreferences.getString(key, "");

			if (preference != null) {
				preference.setSummary(getString(R.string.selected) + value.substring(0, 1).toUpperCase() + value.substring(1));
			}

		}

		// Initialise the Update interval
		else if (key.startsWith("update_interval")) {

			// Update summary based on selected value
			String displayValue = "30 minutes";
			SharedPreferences appPreferences = Tools.getAppPreferences(getApplicationContext());
			String value = appPreferences.getString(key, "1800000");
			if (value.equals("300000")) {
				displayValue = "5 " + getString(R.string.minutes);
			} else if (value.equals("900000")) {
				displayValue = "15 " + getString(R.string.minutes);
			} else if (value.equals("1800000")) {
				displayValue = "30 " + getString(R.string.minutes);
			} else if (value.equals("3600000")) {
				displayValue = getString(R.string.one_hour);
			} else if (value.equals("10800000")) {
				displayValue = getString(R.string.three_hours);
			} else if (value.equals("86400000")) {
				displayValue = getString(R.string.daily);
			}

			if (preference != null) {
				preference.setSummary(getString(R.string.selected) + displayValue);
			}

			// Update the value of the update interval
			updateFromGlobal(sharedPreferences, "update_interval", LIST_TYPE);
		}
		// Update time picker summaries
		else if (key.equals("update_start") || key.equals("update_end")) {

			SharedPreferences appPreferences = Tools.getAppPreferences(getApplicationContext());
			String value = appPreferences.getString(key, null);

			mHour = 0;
			mMinute = 0;

			if (value != null) {
				String[] items = value.split(":");
				mHour = Integer.parseInt(items[0]);
				mMinute = Integer.parseInt(items[1]);
			}

			if (preference != null) {
				preference.setSummary("Time set: " + Tools.timeDigitPad(mHour) + ":" + Tools.timeDigitPad(mMinute));
			}

			// Update the value of the update limits
			updateFromGlobal(sharedPreferences, key, STRING_TYPE);
		} else if (key.equals("update_weekend")) {
			updateFromGlobal(sharedPreferences, key, CHECKBOX_TYPE);
		}
	}

	@Override
	protected void onStop() {

		super.onStop();

		// Update the widget when we quit the preferences, and if the dirty,
		// flag is true then do a web update, otherwise do a regular update
		if (mStocksDirty) {
			mStocksDirty = false;
			WidgetBase.updateWidgets(getApplicationContext(), WidgetBase.VIEW_UPDATE);

		} else {
			WidgetBase.update(getApplicationContext(), mAppWidgetId, WidgetBase.VIEW_NO_UPDATE);
		}

		finish();
	}

	private void showHelpUsage() {

		String title = getString(R.string.selecting_widget_views);
		String body = getString(R.string.help_usage);
		Tools.showSimpleDialog(this, title, body);
	}

	private void showHelpPortfolio() {

		String title = getString(R.string.using_portfolio);
		String body = getString(R.string.help_portfolio);
		Tools.showSimpleDialog(this, title, body);
	}

	private void showChangeLog() {

		String title = "BUILD " + Tools.BUILD_NUMBER;
		String body = getString(R.string.change_log);
		Tools.showSimpleDialog(this, title, body);
	}

	private void showFeedbackOption() {
		Callable callable = new Callable() {
			public Object call() throws Exception {

				startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=com.thiagoh.stocks_monitor")));

				return new Object();
			}
		};

		Tools.alertWithCallback(this, getString(R.string.rate_stocks), getString(R.string.please_support_stocks), getString(R.string.rate),
				getString(R.string.close), callable);
	}

	@Override
	public void onStart() {
		super.onStart();
	}
}
