package com.sharshar.coinswap.components;

import com.sharshar.coinswap.TestCoinswapApplication;
import com.sharshar.coinswap.beans.PriceData;
import com.sharshar.coinswap.beans.SwapDescriptor;
import com.sharshar.coinswap.exchanges.binance.BinanceAccountServices;
import com.sharshar.coinswap.services.PriceUpdaterService;
import com.sharshar.coinswap.services.SwapService;
import com.sharshar.coinswap.utils.ScratchConstants;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.List;

import static org.junit.Assert.*;

/**
 * Created by lsharshar on 10/7/2018.
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = TestCoinswapApplication.class)
public class SwapExecutorThreadTest {
	@Autowired
	SwapService swapService;

	@Autowired
	PriceUpdaterService priceUpdaterService;

	@Autowired
	BinanceAccountServices service;

	@Test
	public void testThread() {
		SwapDescriptor sd = new SwapDescriptor().setCoin1("BTC").setCoin2("TUSD").setBaseCoin("BTC")
				.setCommissionCoin("BNB").setActive(true).setDesiredStdDev(0.5).setSimulate(true)
				.setMaxPercentVolume(0.005).setLastVolume1(0.0).setLastVolume2(0.0).setCoinOwned(2)
				.setPercentPie(0.2).setExchange(ScratchConstants.Exchange.BINANCE.getValue());
		SwapService.Swap swap = swapService.createComponent(sd);
		List <PriceData> pd = service.getAllPrices();
		priceUpdaterService.launchThread(pd, swap, true);
		System.out.println("Set Off Thread");
	}
}