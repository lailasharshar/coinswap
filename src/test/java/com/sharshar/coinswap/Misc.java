package com.sharshar.coinswap;

import com.binance.api.client.domain.market.OrderBook;
import com.binance.api.client.domain.market.OrderBookEntry;
import com.sharshar.coinswap.exchanges.binance.BinanceAccountServices;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Created by lsharshar on 8/27/2018.
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = TestCoinswapApplication.class)
public class Misc {
	@Autowired
	BinanceAccountServices services;

	@Test
	public void showBookOrders() {
		String ticker = "BCDBTC";
		//String ticker = "TUSDBTC";
		OrderBook order = services.getBookOrders(ticker);
		double totalBids = order.getBids().stream().mapToDouble(c -> Double.parseDouble(c.getQty())).sum();
		double totalAsks = order.getAsks().stream().mapToDouble(c -> Double.parseDouble(c.getQty())).sum();
		double totalBidPrice = order.getBids().stream()
				.mapToDouble(c ->  Double.parseDouble(c.getQty()) *  Double.parseDouble(c.getPrice())).sum();
		double averageBidPrice = totalBidPrice/totalBids;
		double totalAskPrice = order.getAsks().stream()
				.mapToDouble(c ->  Double.parseDouble(c.getQty()) *  Double.parseDouble(c.getPrice())).sum();
		double averageAskPrice = totalAskPrice/totalAsks;
		System.out.println("Total Bids: " + totalBids); // Total Requests to Buy
		System.out.println("Total Asks: " + totalAsks); // Total Requests to Sell

		double volume = services.get24HourVolume(ticker);
		//double volume = 1200.0;
		double maxAmtToSellBuy = 0.08 * volume;

		double currentPrice = services.getAllPrices().stream()
				.filter(c -> c.getTicker().equalsIgnoreCase(ticker)).findFirst().get().getPrice();

		double amountLeft = maxAmtToSellBuy;
		double totalPrice = 0.0;
		int totalBatches = 0;
		double minPrice = 0.0;
		double maxPrice = 0.0;
		for (OrderBookEntry entry : order.getAsks()) {
			double qty = Double.parseDouble(entry.getQty());
			double price = Double.parseDouble(entry.getPrice());
			if (minPrice < 0.000000001) {
				minPrice = price;
			}
			maxPrice = price;
			totalBatches++;
			if (qty > amountLeft) {
				System.out.println("Bought: " + String.format("%.6f", amountLeft) + "/" + String.format("%.6f", qty) + " @ " + String.format("%.6f", price));
				totalPrice += amountLeft * price;
				break;
			} else {
				System.out.println("Bought: " + String.format("%.6f", qty) + "/" + String.format("%.6f", qty) + " @ " + String.format("%.6f", price));
				totalPrice += qty * price;
				amountLeft -= qty;
			}
		}
		double avtAmount = totalPrice/maxAmtToSellBuy;
		System.out.println("Number of batches: " + totalBatches);
		System.out.println("Max to buy: " + String.format("%.6f", maxAmtToSellBuy));
		System.out.println("Avg price: " + String.format("%.6f", avtAmount));
		System.out.println("Current Price: " + String.format("%.6f", currentPrice));
		double percentIncrease = (maxPrice - minPrice) / minPrice * 100;
		System.out.println("Percent Increase: " + String.format("%.6f", percentIncrease));

		amountLeft = maxAmtToSellBuy;
		totalPrice = 0.0;
		totalBatches = 0;
		minPrice = 0.0;
		maxPrice = 0.0;
		for (OrderBookEntry entry : order.getBids()) {
			double qty = Double.parseDouble(entry.getQty());
			double price = Double.parseDouble(entry.getPrice());
			if (minPrice < 0.000000001) {
				minPrice = price;
			}
			maxPrice = price;
			totalBatches++;
			if (qty > amountLeft) {
				System.out.println("Sold: " + String.format("%.6f", amountLeft) + "/" + String.format("%.6f", qty) + " @ " + String.format("%.6f", price));
				totalPrice += amountLeft * price;
				break;
			} else {
				System.out.println("Sold: " + String.format("%.6f", qty) + "/" + String.format("%.6f", qty) + " @ " + String.format("%.6f", price));
				totalPrice += qty * price;
				amountLeft -= qty;
			}
		}
		avtAmount = totalPrice/maxAmtToSellBuy;
		System.out.println("Number of batches: " + totalBatches);
		System.out.println("Max to Sold: " + String.format("%.6f", maxAmtToSellBuy));
		System.out.println("Avg price: " + String.format("%.6f", avtAmount));
		System.out.println("Current Price: " + String.format("%.6f", currentPrice));
		double percentDecrease = (maxPrice - minPrice) / minPrice * 100;
		System.out.println("Percent Increase: " + String.format("%.6f", percentDecrease));
	}

	@Test
	public void testBaseCoin() {
		assertEquals(services.get24HourVolume("BTCBTC"), 0.0, 0.00000001);
		assertTrue(services.get24HourVolume("ETHBTC") > 0.00000001);
	}
}
