package com.sharshar.coinswap.services;

import com.sharshar.coinswap.TestCoinswapApplication;
import com.sharshar.coinswap.beans.OwnedAsset;
import com.sharshar.coinswap.beans.PriceData;
import com.sharshar.coinswap.beans.SwapDescriptor;
import com.sharshar.coinswap.exchanges.binance.BinanceAccountServices;
import com.sharshar.coinswap.utils.ScratchConstants;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Created by lsharshar on 9/30/2018.
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = TestCoinswapApplication.class)
public class PurchaseAdvisorTest {

	@Autowired
	BinanceAccountServices binanceAccountServices;

	@Autowired
	SwapService swapService;

	@Test
	public void getAmountToBuy() throws Exception {
		List<PriceData> priceData = binanceAccountServices.getAllPrices();
		List<OwnedAsset> balances = binanceAccountServices.getAllBalances();
		List<SwapService.Swap> swaps = swapService.getSwaps();
		SwapDescriptor swapDescriptor = swaps.get(0).getSwapDescriptor();
		double amt = PurchaseAdvisor.getAmountToBuy(balances, swapDescriptor, "BCD", "BTC", 100, priceData, swaps);
		System.out.println(amt);
	}

	@Test
	public void getAmountToBuy2() throws Exception {
		List<OwnedAsset> ownedAssets = new ArrayList<>();
		ownedAssets.add(new OwnedAsset().setAsset("BTC").setFree(0.18212934));
		ownedAssets.add(new OwnedAsset().setAsset("BNB").setFree(948.5565694));
		ownedAssets.add(new OwnedAsset().setAsset("XRP").setFree(1971.4171));
		ownedAssets.add(new OwnedAsset().setAsset("HOT").setFree(22955));
		ownedAssets.add(new OwnedAsset().setAsset("ETH").setFree(1.4531662));
		SwapDescriptor sd = new SwapDescriptor().setTableId(1L).setCoin1("BTC").setCoin2("HOT").setExchange((short) 1)
				.setBaseCoin("BTC").setCommissionCoin("BNB").setActive(true).setSimulate(true).setMaxPercentVolume(0.05)
				.setLastVolume1(0.0).setLastVolume2(2442622798.0).setCoinOwned(1).setPercentPie(0.45);
		SwapDescriptor sd2 = new SwapDescriptor().setTableId(1L).setCoin1("XRP").setCoin2("HOT").setExchange((short) 1)
				.setBaseCoin("BTC").setCommissionCoin("BNB").setActive(true).setSimulate(true).setMaxPercentVolume(0.05)
				.setLastVolume1(0.0).setLastVolume2(2442622798.0).setCoinOwned(1).setPercentPie(0.45);
		SwapDescriptor sd3 = new SwapDescriptor().setTableId(1L).setCoin1("XRP").setCoin2("ETH").setExchange((short) 1)
				.setBaseCoin("BTC").setCommissionCoin("BNB").setActive(true).setSimulate(true).setMaxPercentVolume(0.05)
				.setLastVolume1(0.0).setLastVolume2(2442622798.0).setCoinOwned(1).setPercentPie(0.45);
		List<PriceData> priceData = new ArrayList<>();
		Date now = new Date();
		priceData.add(new PriceData().setUpdateTime(now).setTicker("BTCBTC").setPrice(1.0).setExchange(ScratchConstants.Exchange.BINANCE));
		priceData.add(new PriceData().setUpdateTime(now).setTicker("BNBBTC").setPrice(0.0015135).setExchange(ScratchConstants.Exchange.BINANCE));
		priceData.add(new PriceData().setUpdateTime(now).setTicker("XRPBTC").setPrice(0.00007149).setExchange(ScratchConstants.Exchange.BINANCE));
		priceData.add(new PriceData().setUpdateTime(now).setTicker("HOTBTC").setPrice(0.00000016).setExchange(ScratchConstants.Exchange.BINANCE));
		priceData.add(new PriceData().setUpdateTime(now).setTicker("ETHBTC").setPrice(0.0459738190694).setExchange(ScratchConstants.Exchange.BINANCE));
		List<SwapService.Swap> swaps = new ArrayList<>();
		swaps.add(swapService.createComponent(sd2));

		double totalInBtc = (0.18212934) + (22955.0 * 0.00000016);
		double totalInBtc2 = (0.18212934) + (1971.4171 * 0.00007149) + (22955.0 * 0.00000016);
		double calcTotal = SummaryService.getTotalAmtInBase(Arrays.asList(ownedAssets.get(0), ownedAssets.get(3)), priceData);
		double calcTotal2 = SummaryService.getTotalAmtInBase(Arrays.asList(ownedAssets.get(0), ownedAssets.get(3), ownedAssets.get(2)), priceData);
		assertEquals(totalInBtc, calcTotal, 0.0000001);
		assertEquals(totalInBtc2, calcTotal2, 0.0000001);
		SwapService.Swap newSwap = swapService.createComponent(sd);
		double amt2 = PurchaseAdvisor.getAmountInBase(ownedAssets, swaps, "BTC", priceData);
		assertEquals(totalInBtc2, amt2, 0.0000001);
		swaps.add(swapService.createComponent(sd3));
		double amt3 = PurchaseAdvisor.getAmountInBase(ownedAssets, swaps, "BTC", priceData);

		double amt = PurchaseAdvisor.getAmountToBuy(ownedAssets, sd, "HOT", "BTC", 1124791.0, priceData, swaps);
	}


}