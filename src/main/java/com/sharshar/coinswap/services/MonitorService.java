package com.sharshar.coinswap.services;

import com.sharshar.coinswap.beans.OwnedAsset;
import com.sharshar.coinswap.beans.PriceData;
import com.sharshar.coinswap.exchanges.binance.BinanceAccountServices;
import com.sharshar.coinswap.utils.CoinUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Items to monitor and report summary stuff to me
 *
 * Created by lsharshar on 8/8/2018.
 */
@Service
public class MonitorService {
	private Logger logger = LogManager.getLogger();

	@Autowired
	NotificationService notificationService;

	@Autowired
	BinanceAccountServices binanceAccountServices;

	@Scheduled(cron = "0 0 8 * * *")
	public void notifyBalanceEveryDay() {
		StringBuilder results = new StringBuilder();
		List<OwnedAsset> assetList = binanceAccountServices.getBalancesWithValues();
		List<PriceData> priceData = binanceAccountServices.getAllPrices();
		double totalAmountOfBtc = 0;
		for (OwnedAsset asset : assetList) {
			results.append("Asset: ").append(asset.getAsset()).append(" ")
					.append(String.format("%.4f", asset.getFree()))
					.append("/").append(String.format("%.4f", asset.getLocked()));
			double assetPrice = 1;
			if (!asset.getAsset().equalsIgnoreCase("BTC")) {
				assetPrice = CoinUtils.getPrice(asset.getAsset() + "BTC", priceData);
			}
			double amountInBtc = (asset.getFree() + asset.getLocked()) * assetPrice;
			results.append(" = ").append(String.format("%.4f", amountInBtc)).append("<br>");
			totalAmountOfBtc += amountInBtc;
		}
		double totalDollars = CoinUtils.getPrice("BTCUSDT", priceData) * totalAmountOfBtc;
		results.append("<br>").append("Total: ").append(String.format("%.4f", totalAmountOfBtc))
				.append(" = ").append(String.format("%.4f", totalDollars)).append("<br>");
		try {
			notificationService.notifyMe("Daily Roundup", results.toString());
		} catch (Exception ex)	{
			logger.error("Unable to notify me of my daily roundup: " + results.toString(), ex);
		}
	}
}
