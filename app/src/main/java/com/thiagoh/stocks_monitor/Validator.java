package com.thiagoh.stocks_monitor;

/**
 * Created by thiago on 27/12/15.
 */
public class Validator {

	public static boolean isNull(String s) {

		if (s == null || s.trim().length() == 0) {
			return true;
		}

		return false;
	}

	public static boolean isNotNull(String s) {

		return !isNull(s);
	}
}
