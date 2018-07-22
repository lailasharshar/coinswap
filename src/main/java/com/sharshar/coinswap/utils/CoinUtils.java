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
	public static OwnedAsset getAssetValue(String coin, List<OwnedAsset> ownedAssets) {
		OwnedAsset coinAsset = ownedAssets.stream()
				.filter(c -> c.getAsset().equalsIgnoreCase(coin)).findFirst().orElse(null);
		if (coinAsset == null) {
			return null;
		}
		return coinAsset;
	}

	public static double getAmountOwnedValueFree(OwnedAsset ownedAsset) {
		if (ownedAsset == null) {
			return 0;
		}
		return ownedAsset.getFree();
	}

	public static PriceData getPriceData(String ticker, List<PriceData> priceData) {
		return priceData.stream()
				.filter(c -> c.getTicker().equalsIgnoreCase(ticker))
				.findFirst().orElse(null);
	}

	public static double getPrice(String ticker, List<PriceData> priceData) {
		PriceData pd = getPriceData(ticker, priceData);
		if (pd == null) {
			return 0.0;
		}
		return pd.getPrice();
	}

	/**
	 * Returns whether a ticker is in the list of price data
	 *
	 * @param ticker - the ticker symbol (coin + basecoin, i.e., XMRBTC
	 * @param pd - the list of price data
	 * @return if price data exists
	 */
	public static boolean hasPriceData(String ticker, List<PriceData> pd) {
		return getPriceData(ticker, pd) != null;
	}
}
