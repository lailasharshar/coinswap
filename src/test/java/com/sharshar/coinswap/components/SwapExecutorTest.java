package com.sharshar.coinswap.components;

import com.sharshar.coinswap.TestCoinswapApplication;
import com.sharshar.coinswap.beans.PriceData;
import com.sharshar.coinswap.beans.SwapDescriptor;
import com.sharshar.coinswap.beans.simulation.TradeAction;
import com.sharshar.coinswap.services.SwapService;
import com.sharshar.coinswap.utils.ScratchConstants;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Created by lsharshar on 7/29/2018.
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = TestCoinswapApplication.class)
public class SwapExecutorTest {
	@Autowired
	private SwapService swapService;

	@Test
	public void correctForStep() throws Exception {
		assertEquals(100, SwapExecutor.correctForStep(1, 100.92283984), 0.000000000001);
		assertEquals(100.9, SwapExecutor.correctForStep(0.1, 100.92283984), 0.000000000001);
		assertEquals(100.92, SwapExecutor.correctForStep(0.01, 100.92283984), 0.000000000001);
		assertEquals(100.922, SwapExecutor.correctForStep(0.001, 100.92283984), 0.000000000001);
		assertEquals(100.9228, SwapExecutor.correctForStep(0.0001, 100.92283984), 0.000000000001);
		assertEquals(100.92283, SwapExecutor.correctForStep(0.00001, 100.92283984), 0.000000000001);
		assertEquals(100.922839, SwapExecutor.correctForStep(0.000001, 100.92283924), 0.000000000001);
	}

	@Test
	public void testBasicStuff() {
		// Create a swap definition
		SwapDescriptor swapDescriptor = new SwapDescriptor().setActive(false).setExchange(ScratchConstants.Exchange.BINANCE.getValue())
				.setCoin1("WTC").setCoin2("SALT").setSimulate(true).setBaseCoin("BTC").setMaxPercentVolume(0.15)
				.setCommissionCoin("BNB").setDesiredStdDev(1.0);

		// Bootstrap the exchange, cache
		SwapService.Swap swap = swapService.createComponent(swapDescriptor);
		SwapExecutor executor = swap.getSwapExecutor();
		assertEquals(executor.getCurrentSwapState(), SwapExecutor.CurrentSwapState.OWNS_COIN_1);
		assertEquals(swapDescriptor.getBaseCoin(), "BTC");
		assertTrue(executor.getAmountCoin1OwnedFree() > 0);
		System.out.println("Amount " + executor.getCache().getTicker1().getTickerBase() + " = " + executor.getAmountCoin1OwnedFree());
		System.out.println("Amount Commission coin owned: " + executor.getAmountCommissionAssetFree());
		List<PriceData> priceData = new ArrayList<>();
		Date now = new Date();
		priceData.add(new PriceData().setExchange(ScratchConstants.Exchange.BINANCE).setPrice(0.0001).setTicker(executor.getCache().getTicker1().getTickerBase()).setUpdateTime(now));
		priceData.add(new PriceData().setExchange(ScratchConstants.Exchange.BINANCE).setPrice(0.001).setTicker(executor.getCache().getTicker2().getTickerBase()).setUpdateTime(now));
		priceData.add(new PriceData().setExchange(ScratchConstants.Exchange.BINANCE).setPrice(0.01).setTicker(swapDescriptor.getCommissionCoin() + swapDescriptor.getBaseCoin()).setUpdateTime(now));
		TradeAction action = executor.swapCoin1ToCoin2(priceData, true);

		SwapDescriptor swapDescriptor2 = new SwapDescriptor().setActive(false).setExchange(ScratchConstants.Exchange.BINANCE.getValue())
				.setCoin1("BTC").setCoin2("BAT").setSimulate(true).setMaxPercentVolume(0.15)
				.setCommissionCoin("BNB").setDesiredStdDev(1.0).setBaseCoin("BTC");
		SwapService.Swap swap2 = swapService.createComponent(swapDescriptor2);
		SwapExecutor executor2 = swap2.getSwapExecutor();
		assertEquals(executor2.getAmountCoin1OwnedFree(), 1.0, 0.0000001);
		priceData.add(new PriceData().setExchange(ScratchConstants.Exchange.BINANCE).setPrice(0.0001).setTicker(executor2.getCache().getTicker2().getTickerBase()).setUpdateTime(now));
		TradeAction action2 = executor2.swapCoin1ToCoin2(priceData, true);
	}
}