package com.sharshar.coinswap.utils;

import com.sharshar.coinswap.beans.OwnedAsset;
import com.sharshar.coinswap.beans.PriceData;

import java.util.List;

/**
 * Place to reuse common code for exchanges
 *
 * Created by lsharshar on 7/20/2018.
 */
public class CoinUtils {
	private CoinUtils() {}
	/**
	 * Retrieve the specific asset you are looking for, for example, BNB, XMR, BTC
	 *
	 * @param coin - the asset to look for
	 * @param ownedAssets - all owned assets
	 * @return the specific asset we are looking for, or null if you don't own any
	 */
	public static OwnedAsset getAssetValue(String coin, List<OwnedAsset> ownedAssets) {
		if (ownedAssets == null || coin == null) {
			return null;
		}
		OwnedAsset coinAsset = ownedAssets.stream()
				.filter(c -> c.getAsset().equalsIgnoreCase(coin)).findFirst().orElse(null);
		if (coinAsset == null) {
			return null;
		}
		return coinAsset;
	}

	/**
	 * Get the amount of free coins you own of a particular asset, or 0 if you don't own any
	 * @param ownedAsset - the asset in question
	 * @return the amoun you own of it
	 */
	public static double getAmountOwnedValueFree(OwnedAsset ownedAsset) {
		if (ownedAsset == null) {
			return 0;
		}
		return ownedAsset.getFree();
	}

	/**
	 * Given a list of price data, find a specific ticker (coin + base coin) you are searching for
	 *
	 * @param ticker - the ticker
	 * @param priceData - the list of price data
	 * @return - the price data object you are looking for, or null if it's not there
	 */
	public static PriceData getPriceData(String ticker, List<PriceData> priceData) {
		if (priceData == null || ticker == null) {
			return null;
		}
		return priceData.stream()
				.filter(c -> c.getTicker().equalsIgnoreCase(ticker))
				.findFirst().orElse(null);
	}

	/**
	 * Don't just look for the price data object, but return it's actual price in that price data list
	 * @param ticker - the ticker you are looking for
	 * @param priceData - the list of price data
	 * @return the price of the ticker
	 */
	public static double getPrice(String ticker, List<PriceData> priceData) {
		if (CoinUtils.inBaseCoin(ticker)) {
			return 1.0;
		}
		PriceData pd = getPriceData(ticker, priceData);
		if (pd == null) {
			return 0.0;
		}
		return pd.getPrice();
	}

	public static double getAmountOfCoinBFromCoinA(double amtCoinA, double priceCoinA, double priceCoinB) {
		double amountInBase = amtCoinA * priceCoinA;
		return amountInBase / priceCoinB;
	}

	public static boolean inBaseCoin(String ticker) {
		if (ticker == null || ticker.length() % 2 == 1) {
			return false;
		}
		int halfLength = ticker.length()/2;
		for (int i=0; i<halfLength; i++) {
			if (ticker.charAt(i) != ticker.charAt(i + halfLength)) {
				return false;
			}
		}
		return true;
	}
}
