package com.sharshar.coinswap.services;

import com.sharshar.coinswap.TestCoinswapApplication;
import com.sharshar.coinswap.beans.OwnedAsset;
import com.sharshar.coinswap.beans.PriceData;
import com.sharshar.coinswap.beans.SwapDescriptor;
import com.sharshar.coinswap.exchanges.binance.BinanceAccountServices;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Created by lsharshar on 9/30/2018.
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = TestCoinswapApplication.class)
public class PurchaseAdvisorTest {
	@Autowired
	SwapService swapService;

	@Autowired
	BinanceAccountServices binanceAccountServices;

	@Test
	public void getAmountToBuy() throws Exception {
		List<PriceData> priceData = binanceAccountServices.getAllPrices();
		List<OwnedAsset> balances = binanceAccountServices.getAllBalances();
		List<SwapService.Swap> swaps = swapService.getSwaps();
		SwapDescriptor swapDescriptor = swaps.get(0).getSwapDescriptor();
		double amt = PurchaseAdvisor.getAmountToBuy(balances, swapDescriptor, "BCD", "BTC", 100, priceData, swaps);
		System.out.println(amt);
	}

}