package com.sharshar.coinswap.components;

import com.binance.api.client.domain.OrderStatus;
import com.binance.api.client.domain.account.Order;
import com.sharshar.coinswap.TestCoinswapApplication;
import com.sharshar.coinswap.beans.PriceData;
import com.sharshar.coinswap.beans.SwapDescriptor;
import com.sharshar.coinswap.beans.Ticker;
import com.sharshar.coinswap.beans.simulation.TradeAction;
import com.sharshar.coinswap.exchanges.binance.BinanceAccountServices;
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

import static com.sharshar.coinswap.components.SwapExecutor.ResponseCode.BUY_ORDER_FILLED;
import static com.sharshar.coinswap.components.SwapExecutor.ResponseCode.SELL_ORDER_FILLED;
import static org.junit.Assert.*;

/**
 * Created by lsharshar on 7/29/2018.
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = TestCoinswapApplication.class)
public class SwapExecutorTest {
	@Autowired
	private SwapService swapService;

	@Autowired
	private BinanceAccountServices binance;

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
	public void breakItUp() {
		SwapDescriptor swapDescriptor = new SwapDescriptor().setActive(true)
				.setExchange(ScratchConstants.Exchange.BINANCE.getValue())
				.setCoin1("WTC").setCoin2("SALT").setSimulate(true).setBaseCoin("BTC").setMaxPercentVolume(0.15)
				.setCommissionCoin("BNB").setDesiredStdDev(1.0);

		// Bootstrap the exchange, cache
		SwapService.Swap swap = swapService.createComponent(swapDescriptor);
		SwapExecutor executor = swap.getSwapExecutor();
		{
			List<Double> amounts = executor.breakItUp(0.01, 0.001, 91);
			assertNotNull(amounts);
			assertEquals(amounts.size(), 3);
			assertEquals(amounts.get(0), 30.0, 0.0000001);
			assertEquals(amounts.get(1), 30.0, 0.0000001);
			assertEquals(amounts.get(2), 31.0, 0.0000001);
		}

		{
			List<Double> amounts2 = executor.breakItUp(0.01, 0.001, 20);
			assertEquals(amounts2.size(), 1);
			assertEquals(amounts2.get(0), 20.0, 0.0000001);
		}

		{
			double amtToBuy = 120.45;
			List<Double> amounts3 = executor.breakItUp(0.01, 0.002234234234, amtToBuy);
			double total = amounts3.stream().mapToDouble(c -> c).sum();
			assertEquals(amtToBuy, total, 0.0000001);
			//assertEquals(amounts3.size(), 1);
			//assertEquals(amounts3.get(0), 20.0, 0.0000001);
		}
	}

	@Test
	public void testBasicStuff() {
		// Create a swap definition
		SwapDescriptor swapDescriptor = new SwapDescriptor().setActive(true)
				.setExchange(ScratchConstants.Exchange.BINANCE.getValue())
				.setCoin1("WTC").setCoin2("SALT").setSimulate(true).setBaseCoin("BTC").setMaxPercentVolume(0.15)
				.setCommissionCoin("BNB").setDesiredStdDev(1.0);

		// Bootstrap the exchange, cache
		SwapService.Swap swap = swapService.createComponent(swapDescriptor);
		SwapExecutor executor = swap.getSwapExecutor();
		assertEquals(executor.getCurrentSwapState(), ScratchConstants.CurrentSwapState.OWNS_COIN_1);
		assertEquals(swapDescriptor.getBaseCoin(), "BTC");
		assertTrue(executor.getAmountCoin1OwnedFree() > 0);
		System.out.println("Amount " + executor.getCache().getTicker1().getAssetAndBase() + " = " + executor.getAmountCoin1OwnedFree());
		System.out.println("Amount Commission coin owned: " + executor.getAmountCommissionAssetFree());
		List<PriceData> priceData = new ArrayList<>();
		Date now = new Date();
		priceData.add(new PriceData().setExchange(ScratchConstants.Exchange.BINANCE).setPrice(0.0001).setTicker(executor.getCache().getTicker1().getAssetAndBase()).setUpdateTime(now));
		priceData.add(new PriceData().setExchange(ScratchConstants.Exchange.BINANCE).setPrice(0.001).setTicker(executor.getCache().getTicker2().getAssetAndBase()).setUpdateTime(now));
		priceData.add(new PriceData().setExchange(ScratchConstants.Exchange.BINANCE).setPrice(0.01).setTicker(swapDescriptor.getCommissionCoin() + swapDescriptor.getBaseCoin()).setUpdateTime(now));
		TradeAction action = executor.swapCoin1ToCoin2(priceData, true);

		SwapDescriptor swapDescriptor2 = new SwapDescriptor().setActive(true)
				.setExchange(ScratchConstants.Exchange.BINANCE.getValue())
				.setCoin1("TUSD").setCoin2("BAT").setSimulate(true).setMaxPercentVolume(0.15)
				.setCommissionCoin("BNB").setDesiredStdDev(1.0).setBaseCoin("BTC");
		SwapService.Swap swap2 = swapService.createComponent(swapDescriptor2);
		SwapExecutor executor2 = swap2.getSwapExecutor();
		assertTrue(executor2.getAmountCoin1OwnedFree() > 5000.0);
		priceData.add(new PriceData().setExchange(ScratchConstants.Exchange.BINANCE).setPrice(0.0001).setTicker(executor2.getCache().getTicker2().getAssetAndBase()).setUpdateTime(now));
		TradeAction action2 = executor2.swapCoin1ToCoin2(priceData, true);
	}

	@Test
	public void testTheRealStuff() {
		SwapDescriptor swapDescriptor = new SwapDescriptor().setActive(true)
				.setExchange(ScratchConstants.Exchange.BINANCE.getValue())
				.setCoin1("BNB").setCoin2("BCD").setSimulate(true).setBaseCoin("BTC").setMaxPercentVolume(0.15)
				.setCommissionCoin("BNB").setDesiredStdDev(1.0);
		SwapService.Swap swap2 = swapService.createComponent(swapDescriptor);
		SwapExecutor executor = swap2.getSwapExecutor();
		assertNotNull(executor);
		Order order = executor.waitForResponse("BNBBTC", "jvimE65CEob3du9e7OU6TW");
		assertEquals(order.getStatus(), OrderStatus.FILLED);
		System.out.println(order);
		double val = executor.getActualCommissions("BNBBTC", 58101316L);
		assertTrue(val > 0.000000001);
	}

	@Test
	public void getCommissionNeeded() {
		List<PriceData> pd = binance.getAllPrices();
		SwapDescriptor swapDescriptor = new SwapDescriptor().setActive(true)
				.setExchange(ScratchConstants.Exchange.BINANCE.getValue())
				.setCoin1("TUSD").setCoin2("BCD").setSimulate(true).setBaseCoin("BTC").setMaxPercentVolume(0.15)
				.setCommissionCoin("BNB").setDesiredStdDev(1.0);
		SwapService.Swap swap2 = swapService.createComponent(swapDescriptor);
		SwapExecutor executor = swap2.getSwapExecutor();
		double amountNeeded = executor.getCommissionNeeded(executor.getCache().getTicker1(), 100.0, 0.005, pd);
		System.out.println(amountNeeded);
	}

	@Test
	public void loadBalances() {
		SwapDescriptor swapDescriptor = new SwapDescriptor().setActive(true)
				.setExchange(ScratchConstants.Exchange.BINANCE.getValue())
				.setCoin1("XMR").setCoin2("BAT").setSimulate(false).setBaseCoin("BTC").setMaxPercentVolume(0.15)
				.setCommissionCoin("BNB").setDesiredStdDev(4.0);
		SwapService.Swap swap = swapService.createComponent(swapDescriptor);
		SwapExecutor executor = swap.getSwapExecutor();
		assertTrue(executor.loadBalances());
	}

	/* ACTUALLY BUYS COINS */
	@Test
	public void buyCoin() {
		SwapDescriptor swapDescriptor = new SwapDescriptor().setActive(true)
				.setExchange(ScratchConstants.Exchange.BINANCE.getValue())
				.setCoin1("TUSD").setCoin2("BCD").setSimulate(false).setBaseCoin("BTC").setMaxPercentVolume(0.15)
				.setCommissionCoin("BNB").setDesiredStdDev(4.0);
		SwapService.Swap swap = swapService.createComponent(swapDescriptor, true);
		SwapExecutor executor = swap.getSwapExecutor();
		SwapExecutor.ResponseCode code = executor.buyCoin(executor.getCache().getTicker1(), 2);
		assertEquals(code, BUY_ORDER_FILLED);
		executor.loadBalances();
	}

	/* ACTUALLY SELLS COINS */
	//@Test
	public void sellCoin() {
		SwapDescriptor swapDescriptor = new SwapDescriptor().setActive(true)
				.setExchange(ScratchConstants.Exchange.BINANCE.getValue())
				.setCoin1("XMR").setCoin2("BCD").setSimulate(false).setBaseCoin("BTC").setMaxPercentVolume(0.15)
				.setCommissionCoin("BNB").setDesiredStdDev(4.0);
		SwapService.Swap swap = swapService.createComponent(swapDescriptor, true);
		SwapExecutor executor = swap.getSwapExecutor();
		SwapExecutor.ResponseCode code = executor.sellCoin(executor.getCache().getTicker1(), 2);
		assertEquals(code, SELL_ORDER_FILLED);
		executor.loadBalances();
	}

	/* ACTUALLY BUYS/SELLS COINS */
	//@Test
	public void swapCoins() {
		SwapDescriptor swapDescriptor = new SwapDescriptor().setActive(true)
				.setExchange(ScratchConstants.Exchange.BINANCE.getValue())
				.setCoin1("TUSD").setCoin2("BCD").setSimulate(false).setBaseCoin("BTC").setMaxPercentVolume(0.08)
				.setCommissionCoin("BNB").setDesiredStdDev(4.0);
		SwapService.Swap swap = swapService.createComponent(swapDescriptor, true);
		SwapExecutor executor = swap.getSwapExecutor();
		List<PriceData> pd = binance.getAllPrices();
		TradeAction ta = executor.swapCoin1ToCoin2(pd, false);
		assertNotNull(ta);
	}

	/* ACTUALLY BUYS/SELLS COINS */
	//@Test
	public void swapCoinsBtc() {
		SwapDescriptor swapDescriptor = new SwapDescriptor().setActive(true)
				.setExchange(ScratchConstants.Exchange.BINANCE.getValue())
				.setCoin1("BTC").setCoin2("BCD").setSimulate(false).setBaseCoin("BTC").setMaxPercentVolume(0.08)
				.setCommissionCoin("BNB").setDesiredStdDev(4.0);
		SwapService.Swap swap = swapService.createComponent(swapDescriptor, true);
		SwapExecutor executor = swap.getSwapExecutor();
		List<PriceData> pd = binance.getAllPrices();
		TradeAction ta = executor.coinSwap(0.0001, executor.getCache().getTicker1(),
				executor.getCache().getTicker2(), pd, false);

	}

	/* ACTUALLY BUYS/SELLS COINS */
	//@Test
	public void swapCoinsBtc2() {
		SwapDescriptor swapDescriptor = new SwapDescriptor().setActive(true)
				.setExchange(ScratchConstants.Exchange.BINANCE.getValue())
				.setCoin1("BTC").setCoin2("BCD").setSimulate(false).setBaseCoin("BTC").setMaxPercentVolume(0.08)
				.setCommissionCoin("BNB").setDesiredStdDev(4.0);
		SwapService.Swap swap = swapService.createComponent(swapDescriptor, true);
		SwapExecutor executor = swap.getSwapExecutor();
		List<PriceData> pd = binance.getAllPrices();
		TradeAction ta = executor.coinSwap(0.1, executor.getCache().getTicker2(),
				executor.getCache().getTicker1(), pd, false);

	}

	/* ACTUALLY BUYS/SELLS COINS */
	//@Test
	public void buyCoin2() {
		SwapDescriptor swapDescriptor = new SwapDescriptor().setActive(true)
				.setExchange(ScratchConstants.Exchange.BINANCE.getValue())
				.setCoin1("TUSD").setCoin2("BCD").setSimulate(false).setBaseCoin("BTC").setMaxPercentVolume(0.15)
				.setCommissionCoin("BNB").setDesiredStdDev(4.0);
		SwapService.Swap swap = swapService.createComponent(swapDescriptor, true);
		SwapExecutor executor = swap.getSwapExecutor();
		SwapExecutor.ResponseCode code = executor.buyCoin(executor.getCache().getTicker2(), 4);
		assertEquals(code, BUY_ORDER_FILLED);
		executor.loadBalances();
	}

	/* ACTUALLY BUYS/SELLS COINS */
	//@Test
	public void testSwap2() {
		SwapDescriptor swapDescriptor = new SwapDescriptor().setActive(true)
				.setExchange(ScratchConstants.Exchange.BINANCE.getValue())
				.setCoin1("TUSD").setCoin2("BCD").setSimulate(false).setBaseCoin("BTC").setMaxPercentVolume(0.08)
				.setCommissionCoin("BNB").setDesiredStdDev(4.0);
		SwapService.Swap swap = swapService.createComponent(swapDescriptor, true);
		SwapExecutor executor = swap.getSwapExecutor();
		List<PriceData> pd = binance.getAllPrices();
		TradeAction ta = executor.swapCoin2ToCoin1(pd, false);
		assertNotNull(ta);
	}
}