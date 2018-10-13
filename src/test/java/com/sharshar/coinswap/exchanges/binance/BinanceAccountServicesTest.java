package com.sharshar.coinswap.exchanges.binance;

import com.sharshar.coinswap.beans.PriceData;
import org.apache.commons.lang3.time.DateUtils;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Created by lsharshar on 10/10/2018.
 */
public class BinanceAccountServicesTest {
	@Test
	public void padEmptyListItems() throws Exception {
		Date now = new Date();
		// Dates for the pull dates
		Date d10 = DateUtils.truncate(now, Calendar.HOUR);
		Date d09 = new Date(d10.getTime() - BinanceAccountServices.ONE_HOUR);
		Date d08 = new Date(d09.getTime() - BinanceAccountServices.ONE_HOUR);
		Date d07 = new Date(d08.getTime() - BinanceAccountServices.ONE_HOUR);
		Date d06 = new Date(d07.getTime() - BinanceAccountServices.ONE_HOUR);
		Date d05 = new Date(d06.getTime() - BinanceAccountServices.ONE_HOUR);
		Date d04 = new Date(d05.getTime() - BinanceAccountServices.ONE_HOUR);
		Date d03 = new Date(d04.getTime() - BinanceAccountServices.ONE_HOUR);
		Date d02 = new Date(d03.getTime() - BinanceAccountServices.ONE_HOUR);
		Date d01 = new Date(d02.getTime() - BinanceAccountServices.ONE_HOUR);
		Date startDate = new Date(d01.getTime() - BinanceAccountServices.ONE_HOUR/2);

		List<Double> prices = new ArrayList<>();
		for (int i=0; i<10; i++) {
			prices.add(Math.random());
		}

		// Create sequential price data
		PriceData p01 = new PriceData().setUpdateTime(d01).setPrice(prices.get(0));
		PriceData p02 = new PriceData().setUpdateTime(d02).setPrice(prices.get(1));
		PriceData p03 = new PriceData().setUpdateTime(d03).setPrice(prices.get(2));
		PriceData p04 = new PriceData().setUpdateTime(d04).setPrice(prices.get(3));
		PriceData p05 = new PriceData().setUpdateTime(d05).setPrice(prices.get(4));
		PriceData p06 = new PriceData().setUpdateTime(d06).setPrice(prices.get(5));
		PriceData p07 = new PriceData().setUpdateTime(d07).setPrice(prices.get(6));
		PriceData p08 = new PriceData().setUpdateTime(d08).setPrice(prices.get(7));
		PriceData p09 = new PriceData().setUpdateTime(d09).setPrice(prices.get(8));
		PriceData p10 = new PriceData().setUpdateTime(d10).setPrice(prices.get(9));

		// List that's missing something at the beginning
		List<PriceData> p1 = new ArrayList<>();
		p1.add(p03);
		p1.add(p04);
		p1.add(p05);
		p1.add(p06);
		p1.add(p07);
		p1.add(p08);
		p1.add(p09);
		p1.add(p10);

		// List that's missing something in the middle
		List<PriceData> p2 = new ArrayList<>();
		p2.add(p01);
		p2.add(p02);
		p2.add(p03);
		p2.add(p07);
		p2.add(p08);
		p2.add(p09);
		p2.add(p10);

		// List that's missing something at the end
		List<PriceData> p3 = new ArrayList<>();
		p3.add(p01);
		p3.add(p02);
		p3.add(p03);
		p3.add(p04);
		p3.add(p05);
		p3.add(p06);
		p3.add(p07);

		// List that's missing all of them
		List<PriceData> p4 = new ArrayList<>();
		p4.add(p02);
		p4.add(p03);
		p4.add(p06);
		p4.add(p07);
		p4.add(p09);

		List<PriceData> newP1 = BinanceAccountServices.padEmptyListItems(p1, startDate, now);
		List<PriceData> newP2 = BinanceAccountServices.padEmptyListItems(p2, startDate, now);
		List<PriceData> newP3 = BinanceAccountServices.padEmptyListItems(p3, startDate, now);
		List<PriceData> newP4 = BinanceAccountServices.padEmptyListItems(p4, startDate, now);

		// Make sure they're all filled out
		assertEquals(newP1.size(), 10);
		assertEquals(newP2.size(), 10);
		assertEquals(newP3.size(), 10);
		assertEquals(newP4.size(), 10);

		// Make sure the first item is the first date
		assertEquals(newP1.get(0).getUpdateTime().getTime(), d01.getTime());
		assertEquals(newP2.get(0).getUpdateTime().getTime(), d01.getTime());
		assertEquals(newP3.get(0).getUpdateTime().getTime(), d01.getTime());
		assertEquals(newP4.get(0).getUpdateTime().getTime(), d01.getTime());

		// Make sure the last item is the last date
		assertEquals(newP1.get(9).getUpdateTime().getTime(), d10.getTime());
		assertEquals(newP2.get(9).getUpdateTime().getTime(), d10.getTime());
		assertEquals(newP3.get(9).getUpdateTime().getTime(), d10.getTime());
		assertEquals(newP4.get(9).getUpdateTime().getTime(), d10.getTime());

		assertEquals(newP1.get(5).getUpdateTime().getTime(), d06.getTime());
		assertEquals(newP2.get(5).getUpdateTime().getTime(), d06.getTime());
		assertEquals(newP3.get(5).getUpdateTime().getTime(), d06.getTime());
		assertEquals(newP4.get(5).getUpdateTime().getTime(), d06.getTime());

		assertEquals(newP1.get(0).getPrice(), newP1.get(2).getPrice(), 0.000001);
		assertEquals(newP1.get(1).getPrice(), newP1.get(2).getPrice(), 0.000001);

		double avg = (newP2.get(2).getPrice() + newP2.get(6).getPrice())/2;
		assertEquals(newP2.get(3).getPrice(), avg, 0.000001);
		assertEquals(newP2.get(4).getPrice(), avg, 0.000001);
		assertEquals(newP2.get(5).getPrice(), avg, 0.000001);

		assertEquals(newP3.get(9).getPrice(), newP3.get(6).getPrice(), 0.000001);
		assertEquals(newP3.get(8).getPrice(), newP3.get(6).getPrice(), 0.000001);
		assertEquals(newP3.get(7).getPrice(), newP3.get(6).getPrice(), 0.000001);

		assertEquals(newP4.get(0).getPrice(), newP3.get(1).getPrice(), 0.000001);
		double avg2 = (newP4.get(2).getPrice() + newP4.get(5).getPrice())/2;
		assertEquals(newP4.get(3).getPrice(), avg2, 0.000001);
		assertEquals(newP4.get(4).getPrice(), avg2, 0.000001);
		double avg3 = (newP4.get(6).getPrice() + newP4.get(8).getPrice())/2;
		assertEquals(newP4.get(7).getPrice(), avg3, 0.000001);
		assertEquals(newP4.get(9).getPrice(), newP4.get(8).getPrice(), 0.000001);
	}
}