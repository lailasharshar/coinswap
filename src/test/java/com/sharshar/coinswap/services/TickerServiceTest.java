package com.sharshar.coinswap.services;

import com.sharshar.coinswap.TestCoinswapApplication;
import com.sharshar.coinswap.beans.Ticker;
import com.sharshar.coinswap.utils.ScratchConstants;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.List;

import static org.junit.Assert.*;

/**
 * Test the service that periodically checks to see what are the available tickers on an exchange
 *
 * Created by lsharshar on 8/26/2018.
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = TestCoinswapApplication.class)
public class TickerServiceTest {
	@Autowired
	private TickerService tickerService;

	@Test
	public void loadTickers() throws Exception {
	}

	@Test
	public void loadTickerListFromDb() throws Exception {
		List<Ticker> tickers = tickerService.loadTickerListFromDb();
		assertNotNull(tickers);
		assertTrue(tickers.size() > 0);
		// get the last ticker
		Ticker ticker = tickers.get(tickers.size() - 1);
		assertNotNull(ticker);
		assertNotNull(ticker.getAsset());
		assertNotNull(ticker.getBase());
		assertNotNull(ticker.getAssetAndBase());
		assertTrue(ticker.getExchange() > 0);
		assertTrue(ticker.getMaxQty() > 0);
		assertTrue(ticker.getMinQty() > 0);
		assertTrue(ticker.getTableId() > 0);
		assertTrue(ticker.getStepSize() > 0);
		assertNotNull(ticker.getUpdatedDate());
	}

	@Test
	public void getTickersFromExchange() throws Exception {
		List<Ticker> tickers = tickerService.getTickersFromExchange(ScratchConstants.Exchange.BINANCE);
		assertNotNull(tickers);
		assertTrue(tickers.size() > 0);
		Ticker ticker = tickerService.getInList("BNB", "BTC", ScratchConstants.Exchange.BINANCE.getValue(), tickers);
		assertNotNull(ticker);
	}

	@Test
	public void runTimedSimulation() throws Exception {
	}

	@Test
	public void runRandomSimulation() throws Exception {
	}

	@Test
	public void getInList() {

	}
}