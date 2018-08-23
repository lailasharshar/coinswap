package com.sharshar.coinswap.utils;

import com.sharshar.coinswap.beans.OwnedAsset;
import com.sharshar.coinswap.beans.PriceData;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Test all the utilities to pull info from coin data
 *
 * Created by lsharshar on 8/23/2018.
 */
public class CoinUtilsTest {
	@Test
	public void getAssetValue() throws Exception {
		assertEquals(null, CoinUtils.getAssetValue("BTC", null));
		List<OwnedAsset> assets = new ArrayList<>();
		assets.add(new OwnedAsset().setAsset("BTC").setFree(1.0).setLocked(2.0));
		assets.add(new OwnedAsset().setAsset("ETH").setFree(3.0).setLocked(4.0));
		OwnedAsset asset = CoinUtils.getAssetValue("BTC", assets);
		assertNull(CoinUtils.getAssetValue(null, assets));
		assertNull(CoinUtils.getAssetValue("BOGUS", assets));
		assertEquals(asset.getAsset(), "BTC");
		assertEquals(asset.getFree(), 1.0, 0.0000001);
		assertEquals(asset.getLocked(), 2.0, 0.0000001);
		assertEquals(1.0, CoinUtils.getAmountOwnedValueFree(asset), 0.000001);
		assertEquals(0.0, CoinUtils.getAmountOwnedValueFree(null), 0.000001);
	}

	@Test
	public void getPriceData() throws Exception {
		assertNull(CoinUtils.getPriceData("ETHBTC", null));
		List<PriceData> prices = new ArrayList<>();
		PriceData pd = CoinUtils.getPriceData("ETHBTC", prices);
		assertNull(pd);
		prices.add(new PriceData().setTicker("NEOBTC").setPrice(2.0));
		prices.add(new PriceData().setTicker("ETHBTC").setPrice(5.0));
		pd = CoinUtils.getPriceData("ETHBTC", prices);
		assertEquals(pd.getPrice(), 5.0, 0.000001);
		assertEquals(2.0, CoinUtils.getPrice("NEOBTC", prices), 0.000001);
		assertEquals(0.0, CoinUtils.getPrice("NOTHING", prices), 0.000001);
	}
}