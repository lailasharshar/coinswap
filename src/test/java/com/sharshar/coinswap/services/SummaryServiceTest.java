package com.sharshar.coinswap.services;

import com.sharshar.coinswap.TestCoinswapApplication;
import com.sharshar.coinswap.beans.OwnedAsset;
import com.sharshar.coinswap.beans.PriceData;
import com.sharshar.coinswap.utils.ScratchConstants;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Created by lsharshar on 10/17/2018.
 */
//@RunWith(SpringRunner.class)
//@SpringBootTest(classes = TestCoinswapApplication.class)
public class SummaryServiceTest {
	@Test
	public void getTotalAmtInBase() throws Exception {
		List<OwnedAsset> ownedAssets = new ArrayList<>();
		ownedAssets.add(new OwnedAsset().setAsset("ABC").setFree(10).setLocked(1));
		ownedAssets.add(new OwnedAsset().setAsset("DEF").setFree(30).setLocked(0));
		ownedAssets.add(new OwnedAsset().setAsset("BTC").setFree(4).setLocked(0));
		List<PriceData> pd = new ArrayList<>();
		Date now = new Date();
		pd.add(new PriceData().setTicker("ABCBTC").setPrice(4.0).setExchange(ScratchConstants.Exchange.BINANCE).setUpdateTime(now));
		pd.add(new PriceData().setTicker("DEFBTC").setPrice(.2).setExchange(ScratchConstants.Exchange.BINANCE).setUpdateTime(now));
		double totalDerived = SummaryService.getTotalAmtInBase(ownedAssets, pd);
		double totalShouldBe = (11 * 4) + (30 * 0.2) + (4);
		assertEquals(totalDerived, totalShouldBe, 0.000001);
	}

}