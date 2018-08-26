package com.sharshar.coinswap.utils;

import com.sharshar.coinswap.beans.PriceData;

import java.util.ArrayList;
import java.util.List;

/**
 * Common methods used in analysis of algorithms
 *
 * Created by lsharshar on 7/21/2018.
 */
public class AnalysisUtils {
	private AnalysisUtils() {}
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

	/**
	 * Given a list of doubles, return the standard deviation from the mean
	 *
	 * @param data - the values
	 * @param mean - the previously calculated mean
	 * @return the standard deviation
	 */
	public static double getStdDev(List<Double> data, double mean) {
		if (data == null || data.size() < 2) {
			return 0;
		}
		double totalSquared = data.stream().mapToDouble(c -> Math.pow(Math.abs(c - mean), 2)).sum();
		double meanOfSquared = totalSquared/(data.size() - 1);
		return Math.sqrt(meanOfSquared);
	}

	/**
	 * Based on price data, calculate the ratios between pd1 and pd2 over the same time. We are
	 * assuming that the price data is identical across time. In other words:
	 * 		p1.size() = p2.size()
	 * 		p1[0].updateTime = p2[0].updateTime
	 * 		p1[n].updateTime = p2[n].updateTime
	 *
	 * @param pd1 - the first price data list
	 * @param pd2 - the second price data list
	 * @return - the list of ratios
	 */
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
