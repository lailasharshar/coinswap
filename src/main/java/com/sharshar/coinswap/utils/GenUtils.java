package com.sharshar.coinswap.utils;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Generic code that gets used over and over again
 *
 * Created by lsharshar on 3/16/2018.
 */
public class GenUtils {
	public static Date parseDate(String date, SimpleDateFormat sdf) {
		Date dateVal = null;
		try {
			dateVal = sdf.parse(date);
		} catch (Exception ex) {
			ex.printStackTrace();
			return null;
		}
		return dateVal;
	}
}
