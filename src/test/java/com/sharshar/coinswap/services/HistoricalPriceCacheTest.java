package com.sharshar.coinswap.services;

import com.sharshar.coinswap.TestCoinswapApplication;
import com.sharshar.coinswap.beans.PriceData;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.List;

import static org.junit.Assert.*;

/**
 * Created by lsharshar on 10/8/2018.
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = TestCoinswapApplication.class)
public class HistoricalPriceCacheTest {
	@Autowired
	private HistoricalPriceCache historicalPriceCache;

	@Test
	public void getHistoricalData() throws Exception {
		List<PriceData> priceDataList = historicalPriceCache.getHistoricalData("BCD", "BTC", 10);
		assertNotNull(priceDataList);
		assertEquals(priceDataList.size(), 10);
		List<PriceData> priceDataList2 = historicalPriceCache.getHistoricalData("BCD", "BTC", 10);
		assertNotNull(priceDataList2);
		assertEquals(priceDataList2.size(), 10);
		// And yes, I expect them to be the same object at the same memory location
		assertEquals(priceDataList.get(0), priceDataList2.get(0));
	}

}