package com.sharshar.coinswap.services;

import com.sharshar.coinswap.beans.SwapDescriptor;
import com.sharshar.coinswap.beans.Ticker;
import com.sharshar.coinswap.beans.simulation.TradeAction;
import com.sharshar.coinswap.beans.uiresponses.Holdings;
import com.sharshar.coinswap.beans.uiresponses.OwnedAssetUI;
import com.sharshar.coinswap.components.ExchangeCache;
import com.sharshar.coinswap.components.SwapExecutor;
import com.sharshar.coinswap.exchanges.AccountService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

import static com.sharshar.coinswap.components.SwapExecutor.ResponseCode.TRANSACTION_SUCCESSFUL;

/**
 * Move some of the notification logic into it's own class to prevent that notification logic getting mashed in with
 * the business logic.
 *
 * Created by lsharshar on 8/23/2018.
 */
@Service
public class MessageService {
	private static final Logger logger = LogManager.getLogger();

	@Autowired
	private NotificationService notificationService;

	@Autowired
	private SummaryService summaryService;

	public void notifyStatusErrors(String error) {
		if (error != null && !error.isEmpty()) {
			try {
				notificationService.notifyMe("Status Error", error);
			} catch (Exception ex) {
				logger.error("Unable to notify me of status error: " + error);
			}
		}
	}

	public void notifyChanges(List<Ticker> addedTickers, List<Ticker> removedTickers) {
		if ((addedTickers == null || addedTickers.isEmpty())
				&& (removedTickers == null || removedTickers.isEmpty())) {
			// Both are empty, do nothing
			return;
		}
		StringBuilder content = new StringBuilder();
		if (addedTickers != null && !addedTickers.isEmpty()) {
			content.append("Added Tickers (").append(addedTickers.size()).append("): <br><br>");
			logger.info("Added Tickers (" + addedTickers.size() + ")");
			for (Ticker s : addedTickers) {
				content.append(s.getTableId()).append(s.getBase()).append("<br>");
			}
			content.append("<br><br>");
		}
		if (removedTickers != null && !removedTickers.isEmpty()) {
			content.append("Removed Tickers (").append(removedTickers.size()).append("): <br><br>");
			logger.info("Removed Tickers (" + removedTickers.size() + ")");
			for (Ticker s : removedTickers) {
				content.append(s.getAsset()).append(s.getBase()).append("<br>");
			}
			content.append("<br><br>");
		}
		try {
			String subject = "Exchange Ticker Changes (" +
					(addedTickers == null ? 0 : addedTickers.size()) +
					" Added/" +
					(removedTickers == null ? 0 : removedTickers.size()) +
					" Removed";
			notificationService.notifyMe(subject, content.toString());
		} catch (Exception ex) {
			logger.error("Unable to alert to new/retired tickers: " + content.toString(), ex);
		}
	}

	public void dailyRoundup(AccountService service) {
		Holdings holdings = summaryService.getHoldings(service);
		StringBuilder results = new StringBuilder();
		for (OwnedAssetUI asset : holdings.getOwnedAssets()) {
			results.append("Asset: ").append(asset.getAsset()).append(" ")
					.append(String.format("%.4f", asset.getTotal()));
			double assetPrice = asset.getPrice();
			results.append(" @ ").append(String.format("%.7f", assetPrice)).append(" ");
			double amountInBtc = asset.getInBTC();
			double amountInUSD = asset.getInUSD();
			results.append(" = BTC ").append(String.format("%.7f", amountInBtc)).append(" ");
			results.append(" = $ ").append(String.format("%.7f", amountInUSD)).append("<br>");
		}
		NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(Locale.US);
		String totalDollarsString = currencyFormat.format(holdings.getAmountInUSD());
		results.append("<br>").append("Total: ").append(String.format("%.7f", holdings.getAmountInBitcoin()))
				.append(" = $ ").append(totalDollarsString).append("<br>");
		try {
			notificationService.notifyMe("Daily Roundup: " + totalDollarsString, results.toString());
		} catch (Exception ex)	{
			logger.error("Unable to notify me of my daily roundup: " + results.toString(), ex);
		}
	}

	public void summarizeTrade(SwapService.Swap swap, TradeAction action) {
		String valString = summarizeSwap(action, swap);
		if (valString != null && !valString.isEmpty()) {
			try {
				notificationService.notifyMe("Swap Result", valString);
				if (action == null) {
					action = new TradeAction();
				}
				notificationService.textMe("Swap Result", action.toString());
			} catch (Exception ex) {
				logger.error("Unable to notify me\n" + valString);
			}
		}
	}

	public String summarizeSwap(TradeAction action, SwapService.Swap swap) {
		if (action == null) {
			return null;
		}
		StringBuilder s = new StringBuilder();
		s.append("Response Code: ").append(action.getResponseCode()).append("<br>");
		switch (action.getResponseCode()) {
			// Poorly formed

			case NO_COIN_1_DEFINED:
			case NO_COIN_2_DEFINED:
			case NO_BASE_COIN_DEFINED:
			case NO_COMMISSION_COIN_DEFINED:
			case NO_SWAP_DEFINITION_DEFINED:
				s.append("Swap poorly defined<br>");
				break;

			// The incomplete

			case SELL_ORDER_NEW:
			case SELL_ORDER_FILLED:
			case SELL_ORDER_PARTIAL_FILLED:
			case BUY_ORDER_NEW:
			case BUY_ORDER_FILLED:
			case BUY_ORDER_PARTIAL_FILLED:
				s.append("Order Incomplete<br>");
				break;

			// The errors
			case SELL_ORDER_CANCELLED:
			case SELL_ORDER_PENDING_CANCEL:
			case SELL_ORDER_REJECTED:
			case SELL_ORDER_EXPIRED:
			case BUY_ORDER_CANCELLED:
			case BUY_ORDER_PENDING_CANCEL:
			case BUY_ORDER_REJECTED:
			case BUY_ORDER_EXPIRED:
			case SELL_ORDER_ERROR:
			case BUY_ORDER_ERROR:
			case UNABLE_TO_UPDATE_COIN_BALANCES:
				s.append("Error Occurred<br>");
				break;

			case NOT_ENOUGH_COMMISSION_CURRENCY:
				s.append("Not enough commission currency<br>");
				break;
		}

		if (action.getResponseCode() != TRANSACTION_SUCCESSFUL) {
			return s.toString();
		}
		// Full success - specify current state
		SwapExecutor executor = swap.getSwapExecutor();
		ExchangeCache cache = executor.getCache();
		SwapDescriptor descriptor = swap.getSwapDescriptor();
		s.append("Setup:<br><hr><br>");
		s.append("Desired Std Deviation: ").append(descriptor.getDesiredStdDev()).append("<br>");
		s.append("Max ").append(descriptor.getCoin1()).append(" to buy: ").append(String.format("%.6f", cache.getTicker1().getMaxQty())).append("<br>");
		s.append("Max ").append(descriptor.getCoin2()).append(" to buy: ").append(String.format("%.6f", cache.getTicker2().getMaxQty())).append("<br>");

		s.append("<br>Current Status (as of ").append(cache.getLatestUpdate()).append("):").append("<br><hr><br>");

		double lastPrice1 = cache.getLastPriceData(descriptor.getCoin1() + descriptor.getBaseCoin()).getPrice();
		double amountBtc1 = executor.getAmountCoin1OwnedFree() * lastPrice1;
		s.append("Current status: ").append(executor.getCurrentSwapState()).append("<br>");
		s.append(descriptor.getCoin1()).append(" Owns: ")
				.append(String.format("%.6f", executor.getAmountCoin1OwnedFree())) .append(" @ ")
				.append(String.format("%.6f", lastPrice1)) .append(" = ")
				.append(String.format("%.6f", amountBtc1)) .append("<br>");
		double lastPrice2 = cache.getLastPriceData(descriptor.getCoin2() + descriptor.getBaseCoin()).getPrice();
		double amountBtc2 = executor.getAmountCoin2OwnedFree() * lastPrice2;
		s.append(descriptor.getCoin2()).append(" Owns: ")
				.append(String.format("%.6f", executor.getAmountCoin2OwnedFree())) .append(" @ ")
				.append(String.format("%.6f", lastPrice2)) .append(" = ")
				.append(String.format("%.6f", amountBtc2)) .append("<br>");

		double lastCommissionPrice = cache.getLastPriceData(descriptor.getCommissionCoin() + descriptor.getBaseCoin()).getPrice();
		double amountBtcCommission = executor.getAmountCoin1OwnedFree() * lastCommissionPrice;
		s.append(descriptor.getCommissionCoin()).append(" Owns: ")
				.append(String.format("%.6f", executor.getAmountCommissionAssetFree())) .append(" @ ")
				.append(String.format("%.6f", lastCommissionPrice)) .append(" = ")
				.append(String.format("%.6f", amountBtcCommission)) .append("<br>");

		s.append("Last Mean Ratio: ").append(String.format("%.6f", cache.getLastMeanRatio())).append("<br>");
		s.append("Last Std Dev: ").append(String.format("%.6f", cache.getLastStandardDeviation())).append("<br>");
		s.append("Last High Ratio: ").append(String.format("%.6f", cache.getHighRatio())).append("<br>");
		s.append("Last Low Ratio: ").append(String.format("%.6f", cache.getLowRatio())).append("<br>");

		return s.toString();
	}
}
