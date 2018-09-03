package com.sharshar.coinswap.services;

import com.sharshar.coinswap.TestCoinswapApplication;
import com.sharshar.coinswap.beans.PriceData;
import com.sharshar.coinswap.beans.SwapDescriptor;
import com.sharshar.coinswap.components.SwapExecutor;
import com.sharshar.coinswap.exchanges.binance.BinanceAccountServices;
import com.sharshar.coinswap.utils.ScratchConstants;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * Test the main working part of the system - this updates the price and determines if we should "swap"
 *
 * Created by lsharshar on 8/26/2018.
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = TestCoinswapApplication.class)
public class PriceUpdaterServiceTest {
	@Autowired
	private SwapService swapService;

	@Autowired
	private PriceUpdaterService priceUpdaterService;

	@Autowired
	private BinanceAccountServices binance;

	@Test
	public void getAllPriceDataForAllExchanges() throws Exception {
		SwapDescriptor swapDescriptor = new SwapDescriptor().setActive(false).setExchange(ScratchConstants.Exchange.BINANCE.getValue())
				.setCoin1("WTC").setCoin2("SALT").setSimulate(true).setBaseCoin("BTC").setMaxPercentVolume(0.15)
				.setCommissionCoin("BNB").setDesiredStdDev(1.0).setActive(true);

		// Bootstrap the exchange, cache
		SwapService.Swap swap = swapService.createComponent(swapDescriptor);
		double volume1 = swap.getSwapDescriptor().getLastVolume1();
		double volume2 = swap.getSwapDescriptor().getLastVolume2();
		assertTrue(volume1 > 0);
		assertTrue(volume2 > 0);
		swapService.updateVolume();
		SwapExecutor executor = swap.getSwapExecutor();
		double volume1u = swap.getSwapDescriptor().getLastVolume1();
		double volume2u = swap.getSwapDescriptor().getLastVolume2();
		// should be nothing
		Map<ScratchConstants.Exchange, List<PriceData>> data = priceUpdaterService.getAllPriceDataForAllExchanges(null);
		assertEquals(data.size(), 0);
		List<SwapService.Swap> swaps = new ArrayList<>();
		data = priceUpdaterService.getAllPriceDataForAllExchanges(swaps);
		assertEquals(data.size(), 0);

		// Now should be one
		swaps.add(swap);
		data = priceUpdaterService.getAllPriceDataForAllExchanges(swaps);
		assertTrue(data.size() > 0);
	}

	@Test
	public void getHistoricalData() {
		//List<PriceData> priceData = binance.getBackfillData(200, "BCD", "BTC");
		//priceData.forEach(c -> System.out.println(String.format("%.6f", c.getPrice())));
		List<PriceData> priceData = binance.getBackfillData(1000, "BCD", "BTC");
		priceData.forEach(c -> System.out.println(String.format("%.6f", c.getPrice()) + " " + c.getUpdateTime()));
	}
}