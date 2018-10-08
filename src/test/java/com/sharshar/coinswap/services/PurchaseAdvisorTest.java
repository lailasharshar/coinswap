package com.sharshar.coinswap.services;

import com.sharshar.coinswap.beans.OwnedAsset;
import com.sharshar.coinswap.beans.PriceData;
import com.sharshar.coinswap.beans.SwapDescriptor;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Created by lsharshar on 9/30/2018.
 */
public class PurchaseAdvisorTest {
	@Test
	public void getAmountToBuy() throws Exception {
		// Try when the coin is coin1, coin2 or coin is base coin
		// If we don't have enough data or enough money

		List<PriceData> priceData = new ArrayList<>();
		priceData.add(new PriceData().setPrice(0.2).setTicker("BTDBTC"));
		priceData.add(new PriceData().setPrice(0.1).setTicker("ABCBTC"));
		priceData.add(new PriceData().setPrice(1.0).setTicker("BTCBTC"));

		List<OwnedAsset> balances = new ArrayList<>();
		balances.add(new OwnedAsset().setAsset("BTC").setFree(300));

		SwapDescriptor swap = new SwapDescriptor().setCoin1("BTD").setCoin2("ABC").setBaseCoin("BTC").setPercentPie(0.2);
		double amt = PurchaseAdvisor.getAmountToBuy(balances, swap, "BTD", "BTC", 100, priceData);
		System.out.println(amt);
	}

}