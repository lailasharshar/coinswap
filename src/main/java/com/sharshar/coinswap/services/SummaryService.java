package com.sharshar.coinswap.services;

import com.sharshar.coinswap.beans.OwnedAsset;
import com.sharshar.coinswap.beans.PriceData;
import com.sharshar.coinswap.beans.uiresponses.Holdings;
import com.sharshar.coinswap.beans.uiresponses.OwnedAssetUI;
import com.sharshar.coinswap.exchanges.AccountService;
import com.sharshar.coinswap.utils.CoinUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by lsharshar on 10/17/2018.
 */
@Service
public class SummaryService {
	public Holdings getHoldings(AccountService service) {
		Holdings holdings = new Holdings();
		List<OwnedAsset> assetList = service.getBalancesWithValues();
		List<PriceData> priceData = service.getAllPrices();
		List<OwnedAssetUI> summarizedAssets = new ArrayList<>();
		double totalAmountOfBtc = getTotalAmtInBase(assetList, priceData);
		for (OwnedAsset asset : assetList) {
			OwnedAssetUI sumAsset = new OwnedAssetUI();
			double assetPrice = 1;
			if (!asset.getAsset().equalsIgnoreCase("BTC")) {
				assetPrice = CoinUtils.getPrice(asset.getAsset() + "BTC", priceData);
			}
			sumAsset.setPrice(assetPrice);
			double amountInBtc = (asset.getFree() + asset.getLocked()) * assetPrice;
			sumAsset.setAsset(asset.getAsset()).setInBTC(amountInBtc)
					.setTotal(asset.getFree() + asset.getLocked()).setPercentOfPortfolio(amountInBtc / totalAmountOfBtc * 1000);
			sumAsset.setInUSD(CoinUtils.getPrice("BTCUSDT", priceData) * amountInBtc);
			summarizedAssets.add(sumAsset);
		}
		double totalDollars = CoinUtils.getPrice("BTCUSDT", priceData) * totalAmountOfBtc;
		holdings.setAmountInBitcoin(totalAmountOfBtc);
		holdings.setAmountInUSD(totalDollars);
		holdings.setOwnedAssets(summarizedAssets);
		return holdings;
	}

	public static double getTotalAmtInBase(List<OwnedAsset> ownedAssets, List<PriceData> priceData) {
		double totalAmountOfBtc = 0.0;
		for (OwnedAsset asset : ownedAssets) {
			double assetPrice = 1;
			if (!asset.getAsset().equalsIgnoreCase("BTC")) {
				assetPrice = CoinUtils.getPrice(asset.getAsset() + "BTC", priceData);
			}
			double amountInBtc = (asset.getFree() + asset.getLocked()) * assetPrice;
			totalAmountOfBtc += amountInBtc;
		}
		return totalAmountOfBtc;
	}
}
