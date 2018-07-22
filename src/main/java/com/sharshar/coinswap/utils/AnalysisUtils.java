package com.sharshar.coinswap.utils;

import com.sharshar.coinswap.beans.PriceData;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by lsharshar on 7/21/2018.
 */
public class AnalysisUtils {
	/**
	 * Given a list of price data, calculate the mean of the price. Used to calculate start prices and end prices so
	 * that one outlier does not define the entire data set
	 *
	 * @param data - the price data to average
	 * @return the average price
	 */
	public static double getMean(List<Double> data) {
		if (data == null || data.isEmpty()) {
			return 0;
		}
		double sub = data.stream().mapToDouble(c -> c).sum();
		return sub/data.size();
	}

	public static double getStdDev(List<Double> data, double mean) {
		if (data == null || data.isEmpty()) {
			return 0;
		}
		double totalSquared = data.stream().mapToDouble(c -> Math.pow(Math.abs(c - mean), 2)).sum();
		double meanOfSquared = totalSquared/(data.size() - 1);
		return Math.sqrt(meanOfSquared);
	}

	public static List<Double> getRatioList(List<PriceData> pd1, List<PriceData> pd2) {
		if (pd1 == null || pd2 == null || pd1.size() != pd2.size()) {
			return new ArrayList<>();
		}
		List<Double> doubleList = new ArrayList<>();
		for (int i=0; i<pd2.size(); i++) {
			double p1 = pd1.get(i).getPrice();
			double p2 = pd2.get(i).getPrice();
			double ratio = p1/p2;
			doubleList.add(ratio);
		}
		return doubleList;
	}
}
