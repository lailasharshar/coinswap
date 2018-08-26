package com.sharshar.coinswap.services;

import com.sharshar.coinswap.TestCoinswapApplication;
import com.sharshar.coinswap.beans.SwapDescriptor;
import com.sharshar.coinswap.beans.Ticker;
import com.sharshar.coinswap.components.ExchangeCache;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.List;

import static org.junit.Assert.*;

/**
 * Created by lsharshar on 7/27/2018.
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = TestCoinswapApplication.class)
@TestPropertySource(properties = {"cacheSize=300"})
public class CacheServiceTest {

	@Autowired
	private CacheService cacheService;
	@Autowired
	private SwapService swapService;
	private String coin1 = "BCD";
	private String coin2 = "NEO";
	private String baseCoin = "BTC";
	@Autowired
	private TickerService tickerService;

	@Test
	public void createCache() throws Exception {
		SwapDescriptor descriptor = new SwapDescriptor();
		descriptor.setCoin1(coin1);
		descriptor.setCoin2(coin2);
		descriptor.setSimulate(true);
		descriptor.setExchange((short) 1);
		descriptor.setCommissionCoin("BNB");
		descriptor.setBaseCoin(baseCoin);

		List<Ticker> tickers = tickerService.loadTickerListFromDb();
		ExchangeCache cache = cacheService.createCache(descriptor, tickers);
		assertNotNull(cache);
		assertNotNull(cache.getTicker1());
		assertEquals(coin1, cache.getTicker1().getAsset());
		assertNotNull(cache.getTicker2());
		assertEquals(coin2, cache.getTicker2().getAsset());
		assertNotNull(cache.getExchange());
		assertNotNull(cache.getCommissionTicker());
		assertEquals(baseCoin, cache.getTicker1().getBase());
		assertEquals(baseCoin, cache.getTicker2().getBase());
		assertEquals("BNB", cache.getCommissionTicker().getAsset());
	}
}