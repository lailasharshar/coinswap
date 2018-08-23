package com.sharshar.coinswap.utils;

import com.sharshar.coinswap.beans.PriceData;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Make sure my means and standard deviations are correct
 *
 * Created by lsharshar on 8/23/2018.
 */
public class AnalysisUtilsTest {
	@Test
	public void getMean() throws Exception {
		List<Double> values = new ArrayList<>();
		values.add(1.0);
		values.add(2.0);
		values.add(3.0);
		values.add(4.0);
		values.add(5.0);
		double mean = AnalysisUtils.getMean(values);
		assertEquals(3, mean, 0.00001);
	}

	@Test
	public void getStdDev() throws Exception {
		List<Double> values = new ArrayList<>();
		values.add(9.0);
		values.add(2.0);
		values.add(5.0);
		values.add(4.0);
		values.add(12.0);
		values.add(7.0);
		double stddev = AnalysisUtils.getStdDev(values, AnalysisUtils.getMean(values));
		assertEquals(3.6193922141707713, stddev, 0.00001);
	}

	@Test
	public void getRatioList() throws Exception {
		List<PriceData> list1 = new ArrayList<>();
		list1.add(new PriceData().setPrice(10.0));
		list1.add(new PriceData().setPrice(2.0));
		List<PriceData> list2 = new ArrayList<>();
		list2.add(new PriceData().setPrice(5.0));
		list2.add(new PriceData().setPrice(8.0));
		List<Double> ratios = AnalysisUtils.getRatioList(list1, list2);
		assertEquals(2, ratios.size());
		assertEquals(2.0, ratios.get(0).doubleValue(), 0.0000001);
		assertEquals(0.25, ratios.get(1).doubleValue(), 0.0000001);
		list2.add(new PriceData().setPrice(5.0));
		ratios = AnalysisUtils.getRatioList(list1, list2);
		assertTrue(ratios.isEmpty());
	}

}