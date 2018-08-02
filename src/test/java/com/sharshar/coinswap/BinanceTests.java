package com.sharshar.coinswap;

import com.binance.api.client.BinanceApiRestClient;
import com.binance.api.client.domain.account.Account;
import com.binance.api.client.domain.account.Order;
import com.binance.api.client.domain.account.Trade;
import com.sharshar.coinswap.beans.PriceData;
import com.sharshar.coinswap.beans.Ticker;
import com.sharshar.coinswap.components.SwapExecutor;
import com.sharshar.coinswap.exchanges.binance.BinanceAccountServices;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.List;

/**
 * Created by lsharshar on 7/19/2018.
 */
@RunWith(SpringRunner.class)
@SpringBootTest
public class BinanceTests {
	@Autowired
	BinanceAccountServices binanceAccountServices;

	@Autowired
	private BinanceApiRestClient binanceApiRestClient;

	@Test
	public void testOrderDetails() {

		//List<Trade> trades = binanceAccountServices.getMyTrades("BNBBTC", "58100614");
		//System.out.println("Trades: " + trades);
		//List<Ticker> tickers = binanceAccountServices.getTickerDefinitions();
		//System.out.println("Tickers: " + tickers);
	}
}
