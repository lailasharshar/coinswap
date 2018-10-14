package com.sharshar.coinswap.services;

import com.binance.api.client.domain.account.Trade;
import com.sharshar.coinswap.TestCoinswapApplication;
import com.sharshar.coinswap.exchanges.binance.BinanceAccountServices;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.List;

/**
 * Created by lsharshar on 10/14/2018.
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = TestCoinswapApplication.class)
public class HistoricalOrderTest {
	@Autowired
	BinanceAccountServices binanceAccountServices;

	@Test
	public void testHistory() {
		List<Trade> trades = binanceAccountServices.getMyTrades("BCDBTC", "19299439");
		System.out.println(trades.size());
	}
}
