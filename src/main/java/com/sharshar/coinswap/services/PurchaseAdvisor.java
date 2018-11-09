package com.sharshar.coinswap.services;

import com.sharshar.coinswap.beans.OwnedAsset;
import com.sharshar.coinswap.beans.PriceData;
import com.sharshar.coinswap.beans.SwapDescriptor;
import com.sharshar.coinswap.utils.CoinUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * If we are sharing coins between swaps (for example BTC, we need to be able to determine how much to use when
 * we want to do a buy of that coin
 * <p>
 * Created by lsharshar on 9/26/2018.
 */
public class PurchaseAdvisor {
	private PurchaseAdvisor() {
	}

	public static double getAmountToBuy(List<OwnedAsset> balances, SwapDescriptor swap, String coinToBuy, String base,
										double amount, List<PriceData> currentPrices, List<SwapService.Swap> allSwaps) {
		if (balances == null || swap == null || amount == 0) {
			return 0.0;
		}
		// Determine how much we own as expressed in base coin and the current amount actually owned in the base coin
		double totalAmountExpressedInBase = getAmountInBase(balances, allSwaps, base, currentPrices);
		OwnedAsset baseAsset = balances.stream().filter(c -> c.getAsset().equalsIgnoreCase(base)).findFirst().orElse(null);
		double totalAmountActuallyOwnedInBase = 0;
		if (baseAsset != null) {
			totalAmountActuallyOwnedInBase = baseAsset.getFree();
		}

		// Now we can determine what percentage we are allowed to use - It doesn't correct if values don't add up to
		// one. This is because we don't necessarily want to trade 100% of our assets. However, currently, we don't
		// check to see if we go over 100% so be careful
		double amountImAllowed = totalAmountExpressedInBase * swap.getPercentPie();

		// we can't purchase more than we own
		if (amountImAllowed > totalAmountActuallyOwnedInBase) {
			amountImAllowed = totalAmountActuallyOwnedInBase;
		}

		// Now determine how much to return
		double totalAmountToReturn = 0;
		// If this is the base coin, no conversion is necessary
		if (coinToBuy.equalsIgnoreCase(base)) {
			totalAmountToReturn = amountImAllowed;
		} else {
			// convert to the amount of the coin we want to buy
			double priceCoin1 = CoinUtils.getPrice(swap.getCoin1() + base, currentPrices);
			double priceCoin2 = CoinUtils.getPrice(swap.getCoin2() + base, currentPrices);
			if (coinToBuy.equalsIgnoreCase(swap.getCoin1())) {
				totalAmountToReturn = amountImAllowed / priceCoin1;
			}
			if (coinToBuy.equalsIgnoreCase(swap.getCoin2())) {
				totalAmountToReturn = amountImAllowed / priceCoin2;
			}
		}
		// Now, make sure we haven't gone above what we wanted to buy
		if (totalAmountToReturn > amount) {
			totalAmountToReturn = amount;
		}
		// We can't buy something if we don't have the coins for it in the base coin
		return totalAmountToReturn;
	}

	public static double getAmountInBase(List<OwnedAsset> ownedAssets, List<SwapService.Swap> allSwaps, String base,
										 List<PriceData> priceData) {
		List<OwnedAsset> relevantOwnedAssets = new ArrayList<>();
		for (SwapService.Swap swap : allSwaps) {
			relevantOwnedAssets.addAll(ownedAssets.stream().filter(c ->
					c.getAsset().equalsIgnoreCase(swap.getSwapDescriptor().getCoin1()) ||
					c.getAsset().equalsIgnoreCase(swap.getSwapDescriptor().getCoin2()) ||
					c.getAsset().equalsIgnoreCase(swap.getSwapDescriptor().getBaseCoin())).collect(Collectors.toList()));
		}
		List<OwnedAsset> relevantOwnedAssetsWithoutDups  = relevantOwnedAssets.stream().distinct().collect(Collectors.toList());
		double totalAmountExpressedInBase = 0.0;
		for (OwnedAsset asset : relevantOwnedAssetsWithoutDups) {
			double assetPrice = 1;
			if (!asset.getAsset().equalsIgnoreCase(base)) {
				assetPrice = CoinUtils.getPrice(asset.getAsset() + base, priceData);
			}
			totalAmountExpressedInBase += (asset.getFree() + asset.getLocked()) * assetPrice;
		}
		return totalAmountExpressedInBase;
	}
}
