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

package com.thiagoh.stocks_monitor.configure;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.view.KeyEvent;

import com.thiagoh.stocks_monitor.DialogTools;
import com.thiagoh.stocks_monitor.R;
import com.thiagoh.stocks_monitor.util.Tools;
import com.thiagoh.stocks_monitor.util.UserData;
import com.thiagoh.stocks_monitor.widget.WidgetBase;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Callable;

abstract class ConfigureBase extends Activity {

	int mWidgetSize = 0;

	private static final String[] DEFAULT_STOCKS = {"PETR3.SA", "PETR4.SA", "BBAS3.SA", "BBDC4.SA", "ITUB4.SA", "ABEV3.SA", "BBSE3.SA",
			"PSSA3.SA", "PRBC4.SA", "PINE4.SA", "OIBR3.SA", "OIBR4.SA", "ELET3.SA", "ELET6.SA", "CIEL3.SA"};

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {

		// Setup the widget if the back button is pressed
		if (keyCode == KeyEvent.KEYCODE_BACK)
			setupWidget(0);

		return super.onKeyDown(keyCode, event);
	}

	void setupWidget(int widgetSize) {

		// Update the widget when we end configuration
		Bundle extras = getIntent().getExtras();

		if (extras != null) {

			int appWidgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);

			Intent resultValue = new Intent();
			resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
			setResult(RESULT_OK, resultValue);

			// Add the appWidgetId to our list of added appWidgetIds
			// and add default preferences (view and Stock1)
			Context context = getBaseContext();

			UserData.addAppWidgetSize(context, appWidgetId, widgetSize);

			Editor editor = Tools.getWidgetPreferences(context, appWidgetId).edit();

			if (widgetSize == 0 || widgetSize == 2) {
				editor.putBoolean("show_percent_change", true);
			}

			for (int i = 0; i < DEFAULT_STOCKS.length && i < 20; i++) {
				editor.putString("Stock" + (i + 1), DEFAULT_STOCKS[i]);
				editor.putString("Stock" + (i + 1) + "_summary", "");
			}

			editor.commit();

			// Finally update
			WidgetBase.update(getApplicationContext(), appWidgetId, WidgetBase.VIEW_UPDATE);
		}

		finish();
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);

		// Hide the activity
		this.setVisible(false);

		DialogTools.alertWithCallback(this, getString(R.string.stocks_widget_added),
				getString(R.string.toque_esquerda_widget_options),
				getString(R.string.close), null, null,
				new Callable() {
					public Object call() throws Exception {
						setupWidget(mWidgetSize);
						return new Object();
					}
				});
	}

	private static final Logger log = LoggerFactory.getLogger(ConfigureBase.class);

}
