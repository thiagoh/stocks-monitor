package com.thiagoh.stocks_monitor;


import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.text.Html;
import android.text.Spanned;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.concurrent.Callable;


/**
 * Created by thiago on 27/12/15.
 */
public class DialogTools {


	public static void showSimpleDialog(Context context, String title, String body) {
		alertWithCallback(context, title, body, "Close", null, null, null);
	}


	public static void alertWithCallback(Context context, String title, String body,
										 String positiveButtonText,
										 String negativeButtonText,
										 final Callable<?> positiveCallback,
										 final Callable<?> dismissCallaback) {
		// Create dialog
		AlertDialog alertDialog = new AlertDialog.Builder(context).create();
		alertDialog.setTitle(title);

		// Use HTML so we can stick bold in here
		Spanned html = Html.fromHtml(body);
		TextView text = new TextView(context);
		text.setPadding(10, 10, 10, 10);
		text.setTextSize(16);
		text.setText(html);

		// Scroll view to handle longer text
		ScrollView scroll = new ScrollView(context);
		scroll.setPadding(0, 0, 0, 0);
		scroll.addView(text);
		alertDialog.setView(scroll);

		// Set the close button text
		alertDialog.setButton(DialogInterface.BUTTON_POSITIVE, positiveButtonText,
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						if (positiveCallback != null) {
							try {
								positiveCallback.call();
							} catch (Exception ignored) {
							}
						}
					}
				});
		// Optional negative button
		if (negativeButtonText != null) {
			alertDialog.setButton(DialogInterface.BUTTON_NEGATIVE, negativeButtonText,
					new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
						}
					});
		}
		// Optional dismiss handler
		alertDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
			@Override
			public void onDismiss(DialogInterface dialog) {
				if (dismissCallaback != null) {
					try {
						dismissCallaback.call();
					} catch (Exception ignored) {
					}
				}
			}
		});
		alertDialog.show();
	}

	public static void choiceWithCallback(Context context, String title,
										  String negativeButtonText,
										  final CharSequence[] choices,
										  final InputAlertCallable callable) {
		// Create dialog
		AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(context);
		alertDialogBuilder.setTitle(title);

		// List click handler
		alertDialogBuilder.setItems(choices, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				if (callable != null) {
					try {
						callable.setInputValue(choices[which].toString());
						callable.call();
					} catch (Exception ignored) {
					}
				}
			}
		});

		// Optional negative button
		if (negativeButtonText != null) {
			alertDialogBuilder.setNegativeButton(negativeButtonText,
					new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
						}
					});
		}

		// Create alert dialog
		AlertDialog alertDialog = alertDialogBuilder.create();
		alertDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
			@Override
			public void onDismiss(DialogInterface dialog) {
			}
		});
		alertDialog.show();
	}

	public static void inputWithCallback(Context context, String title, String body,
										 String positiveButtonText, String negativeButtonText,
										 String defaultInputText,
										 final InputAlertCallable callable) {
		// Create dialog
		AlertDialog alertDialog = new AlertDialog.Builder(context).create();
		alertDialog.setTitle(title);
		alertDialog.setMessage(body);

		// Set an EditText view to get user input
		LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
				ViewGroup.LayoutParams.MATCH_PARENT,
				ViewGroup.LayoutParams.MATCH_PARENT);
		layoutParams.setMargins(20, 0, 20, 0);
		LinearLayout layout = new LinearLayout(context);
		final EditText input = new EditText(context);
		if (defaultInputText != null) {
			input.setText(defaultInputText);
		}
		layout.addView(input);
		input.setLayoutParams(layoutParams);
		alertDialog.setView(layout);

		// Set the close button text
		alertDialog.setButton(DialogInterface.BUTTON_POSITIVE, positiveButtonText,
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						if (callable != null) {
							try {
								callable.setInputValue(input.getText().toString());
								callable.call();
							} catch (Exception ignored) {
							}
						}
					}
				});
		// Optional negative button
		if (negativeButtonText != null) {
			alertDialog.setButton(DialogInterface.BUTTON_NEGATIVE, negativeButtonText,
					new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
						}
					});
		}
		alertDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
			@Override
			public void onDismiss(DialogInterface dialog) {
			}
		});
		alertDialog.show();
	}

	public abstract static class InputAlertCallable implements Callable {
		private String inputValue;

		public String getInputValue() {
			return this.inputValue;
		}

		public void setInputValue(String inputValue) {
			this.inputValue = inputValue;
		}
	}
}
