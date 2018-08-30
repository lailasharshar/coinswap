package com.sharshar.coinswap.components;

import com.sharshar.coinswap.TestCoinswapApplication;
import com.sharshar.coinswap.beans.PriceData;
import com.sharshar.coinswap.beans.SwapDescriptor;
import com.sharshar.coinswap.beans.Ticker;
import com.sharshar.coinswap.services.SwapService;
import com.sharshar.coinswap.utils.ScratchConstants;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Tests operation of price data cache
 *
 * Created by lsharshar on 8/26/2018.
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = TestCoinswapApplication.class)
public class ExchangeCacheTest {
	@Autowired
	private SwapService swapService;

	private SwapDescriptor swapDescriptor;
	private SwapService.Swap swap;
	private List<PriceData> pd;
	private List<PriceData> pd2;
	private List<PriceData> com;
	private static final String coin1 = "TUSD";
	private static final String coin2 = "BCD";
	private static final String commCoin = "BNB";
	private static final String base = "BTC";
	private Date d2 = new Date();
	private Date d1 = new Date(d2.getTime() - 1000);

	private void setUp () {
		swapDescriptor = new SwapDescriptor().setCoin1(coin1).setCoin2(coin2).setBaseCoin(base)
				.setExchange(ScratchConstants.Exchange.BINANCE.getValue()).setCommissionCoin(commCoin)
				.setActive(true).setSimulate(true).setMaxPercentVolume(0.2).setDesiredStdDev(0.2);
		swap = swapService.createComponent(swapDescriptor);
		pd = new ArrayList<>();
		pd.add(new PriceData().setTicker(coin1 + base).setUpdateTime(d1).setPrice(1.01).setExchange(ScratchConstants.Exchange.BINANCE));
		pd.add(new PriceData().setTicker(coin1 + base).setUpdateTime(d2).setPrice(0.99).setExchange(ScratchConstants.Exchange.BINANCE));
		pd2 = new ArrayList<>();
		pd2.add(new PriceData().setTicker(coin2 + base).setUpdateTime(d1).setPrice(0.01).setExchange(ScratchConstants.Exchange.BINANCE));
		pd2.add(new PriceData().setTicker(coin2 + base).setUpdateTime(d2).setPrice(0.02).setExchange(ScratchConstants.Exchange.BINANCE));
		com = new ArrayList<>();
		com.add(new PriceData().setTicker(commCoin + base).setUpdateTime(d1).setPrice(0.1).setExchange(ScratchConstants.Exchange.BINANCE));
		com.add(new PriceData().setTicker(commCoin + base).setUpdateTime(d2).setPrice(0.2).setExchange(ScratchConstants.Exchange.BINANCE));

	}

	@Test
	public void testBasics() {
		setUp();
		assertNotNull(swap);
		assertNotNull(swap.getSwapExecutor());
		assertNotNull(swap.getSwapExecutor().getCache());
	}

	@Test
	public void getAmountCachePopulated() throws Exception {
		setUp();
		ExchangeCache cache = swap.getSwapExecutor().getCache();
		// Should be populated with last historical data
		assertEquals(cache.getCacheSize(), cache.getAmountCachePopulated());
	}

	@Test
	public void addPriceData() throws Exception {
	}

	@Test
	public void bulkLoadData() throws Exception {
		setUp();
		ExchangeCache cache = swap.getSwapExecutor().getCache();
		cache.clear();
		assertEquals(0, cache.getAmountCachePopulated());
		cache.bulkLoadData(swapDescriptor.getCoin1() + base, pd);
		cache.bulkLoadData(swapDescriptor.getCoin2() + base, pd2);
		cache.bulkLoadData(swapDescriptor.getCommissionCoin() + base, pd2);
		assertEquals(2, cache.getAmountCachePopulated());
		assertEquals(d2, cache.getLatestUpdate());
		assertEquals(2, cache.getTicker1Data().size());
		assertEquals(2, cache.getTicker2Data().size());
		assertEquals(2, cache.getCommissionData().size());
		assertEquals(ScratchConstants.Exchange.BINANCE, cache.getExchange());
	}

	@Test
	public void updatePriceData() {
		setUp();
		ExchangeCache cache = swap.getSwapExecutor().getCache();
		List<PriceData> newData = new ArrayList<>();
		Date d3 = new Date(d2.getTime() + 1000);
		newData.add(new PriceData().setTicker(coin1 + base).setUpdateTime(d3).setPrice(1.02).setExchange(ScratchConstants.Exchange.BINANCE));
		newData.add(new PriceData().setTicker(coin2 + base).setUpdateTime(d3).setPrice(0.02).setExchange(ScratchConstants.Exchange.BINANCE));
		newData.add(new PriceData().setTicker(commCoin + base).setUpdateTime(d3).setPrice(0.3).setExchange(ScratchConstants.Exchange.BINANCE));
		ExchangeCache.Position position = cache.addPriceData(newData);
		System.out.println(position);
	}

	@Test
	public void getTicker1() throws Exception {
		setUp();
		ExchangeCache cache = swap.getSwapExecutor().getCache();
		Ticker t1 = cache.getTicker1();
		assertNotNull(t1);
		assertEquals(t1.getAsset(), coin1);
		Ticker t2 = cache.getTicker2();
		assertTrue(t2.getMaxQty() > 0);
		assertNotNull(t2);
		assertEquals(t2.getAsset(), coin2);
		Ticker comm = cache.getCommissionTicker();
		assertNotNull(comm);
		assertEquals(comm.getAsset(), commCoin);
	}

	@Test
	public void getLastStandardDeviation() throws Exception {
		setUp();
		ExchangeCache cache = swap.getSwapExecutor().getCache();
		ExchangeCache.Position pos = cache.updateStats(pd.get(pd.size() - 1), pd2.get(pd2.size() - 1), 0.5);
		assertTrue(cache.getLastMeanRatio() > 0);
		assertTrue(cache.getLastStandardDeviation() > 0);
		PriceData p = cache.getLastPriceData(coin1 + base);
		assertTrue(p.getPrice() > 0);
	}
}