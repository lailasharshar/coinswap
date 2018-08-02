package com.sharshar.coinswap.services;

import com.sharshar.coinswap.beans.PriceData;
import com.sharshar.coinswap.beans.SwapDescriptor;
import com.sharshar.coinswap.components.ExchangeCache;
import com.sharshar.coinswap.components.SwapExecutor;
import com.sharshar.coinswap.exchanges.AccountService;
import com.sharshar.coinswap.utils.AccountServiceFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.sharshar.coinswap.components.SwapExecutor.ResponseCode.TRANSACTION_SUCCESSFUL;

/**
 * Periodic update of prices and potentially trading
 * <p>
 * Created by lsharshar on 7/27/2018.
 */
@Service
public class PriceUpdaterService {

	private Logger logger = LogManager.getLogger();

	@Autowired
	private SwapService swapService;

	@Autowired
	private NotificationService notificationService;

	@Autowired
	AccountServiceFactory factory;

	//@Scheduled(fixedRateString = "${timing.updatePrice}", initialDelayString = "${timing.initialDelay}")
	public void updatePriceData() {

		List<SwapService.Swap> swaps = swapService.getSwaps();
		Map<Short, List<PriceData>> priceDataForAllExchanges = getAllPriceDataForAllExchanges(swaps);
		for (SwapService.Swap swap : swaps) {
			short exchange = swap.getSwapDescriptor().getExchange();
			List<PriceData> exchangeData = priceDataForAllExchanges.get(exchange);
			ExchangeCache.Position position = swap.getSwapExecutor().getCache().addPriceData(exchangeData);
			SwapExecutor.ResponseCode responseCode = null;
			if (swap.getSwapExecutor().getCurrentSwapState() == SwapExecutor.CurrentSwapState.OWNS_COIN_1 &&
					position == ExchangeCache.Position.ABOVE_DESIRED_RATIO) {
				responseCode = swap.getSwapExecutor().swapCoin1ToCoin2(exchangeData, !swap.getSwapDescriptor().isActive());
			} else {
				if (swap.getSwapExecutor().getCurrentSwapState() == SwapExecutor.CurrentSwapState.OWNS_COIN_2 &&
						position == ExchangeCache.Position.BELOW_DESIRED_RATIO) {
					responseCode = swap.getSwapExecutor().swapCoin2ToCoin1(exchangeData, !swap.getSwapDescriptor().isActive());
				}
			}
			String valString = summarizeSwap(responseCode, swap);
			if (valString != null && !valString.isEmpty()) {
				try {
					notificationService.notifyMe("Swap Result", valString);
				} catch (Exception ex) {
					logger.error("Unable to notify me\n" + valString);
				}
			}
		}
	}

	public String summarizeSwap(SwapExecutor.ResponseCode code, SwapService.Swap swap) {
		if (code == null) {
			return null;
		}
		StringBuilder s = new StringBuilder();
		s.append("Response Code: ").append(code).append("\n");
		switch (code) {
			// Poorly formed

			case NO_COIN_1_DEFINED:
			case NO_COIN_2_DEFINED:
			case NO_BASE_COIN_DEFINED:
			case NO_COMMISSION_COIN_DEFINED:
			case NO_SWAP_DEFINITION_DEFINED:
				s.append("Swap poorly defined\n");
				break;

			// The incomplete

			case SELL_ORDER_NEW:
			case SELL_ORDER_FILLED:
			case SELL_ORDER_PARTIAL_FILLED:
			case BUY_ORDER_NEW:
			case BUY_ORDER_FILLED:
			case BUY_ORDER_PARTIAL_FILLED:
				s.append("Order Incomplete\n");
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
				s.append("Error Occurred\n");
				break;

			case NOT_ENOUGH_COMMISSION_CURRENCY:
				s.append("Not enough commission currency\n");
				break;
		}

		if (code != TRANSACTION_SUCCESSFUL) {
			return s.toString();
		}
		// Full success - specify current state
		SwapExecutor executor = swap.getSwapExecutor();
		ExchangeCache cache = executor.getCache();
		SwapDescriptor descriptor = swap.getSwapDescriptor();
		s.append("Setup:").append("-------------\n");
		s.append("Desired Std Deviation: ").append(descriptor.getDesiredStdDev()).append("\n");
		s.append("Cache Size: ").append(cache.getCacheSize()).append("\n");
		s.append("Max ").append(descriptor.getCoin1()).append(" to buy: ").append(cache.getTicker1().getMaxQty()).append("\n");
		s.append("Max ").append(descriptor.getCoin2()).append(" to buy: ").append(cache.getTicker2().getMaxQty()).append("\n");

		s.append("\nCurrent Status (as of ").append(cache.getLatestUpdate()).append("):").append("-------------\n");

		double lastPrice1 = cache.getLastPriceData(descriptor.getCoin1() + executor.getBaseCoin()).getPrice();
		double amountBtc1 = executor.getAmountCoin1OwnedFree() * lastPrice1;
		s.append("Current status: ").append(executor.getCurrentSwapState()).append("\n");
		s.append(cache.getTicker1()).append(" Owns: ")
				.append(String.format("%.6f", executor.getAmountCoin1OwnedFree())) .append(" @ ")
				.append(String.format("%.6f", lastPrice1)) .append(" = ")
				.append(String.format("%.6f", amountBtc1)) .append("\n");
		double lastPrice2 = cache.getLastPriceData(descriptor.getCoin2() + executor.getBaseCoin()).getPrice();
		double amountBtc2 = executor.getAmountCoin1OwnedFree() * lastPrice2;
		s.append(cache.getTicker2()).append(" Owns: ")
				.append(String.format("%.6f", executor.getAmountCoin2OwnedFree())) .append(" @ ")
				.append(String.format("%.6f", lastPrice2)) .append(" = ")
				.append(String.format("%.6f", amountBtc2)) .append("\n");

		double lastCommissionPrice = cache.getLastPriceData(descriptor.getCommissionCoin() + executor.getBaseCoin()).getPrice();
		double amountBtcCommission = executor.getAmountCoin1OwnedFree() * lastCommissionPrice;
		s.append(descriptor.getCommissionCoin()).append(" Owns: ")
				.append(String.format("%.6f", executor.getAmountCommissionAssetFree())) .append(" @ ")
				.append(String.format("%.6f", lastCommissionPrice)) .append(" = ")
				.append(String.format("%.6f", amountBtcCommission)) .append("\n");

		s.append("Last Mean Ratio: ").append(String.format("%.6f", cache.getLastMeanRatio())).append("\n");
		s.append("Last Std Dev: ").append(String.format("%.6f", cache.getLastStandardDeviation())).append("\n");
		s.append("Last High Ratio: ").append(String.format("%.6f", cache.getHighRatio())).append("\n");
		s.append("Last Low Ratio: ").append(String.format("%.6f", cache.getLowRatio())).append("\n");

		return s.toString();
	}

	private Map<Short, List<PriceData>> getAllPriceDataForAllExchanges(List<SwapService.Swap> swaps) {
		// Find the unique exchanges we're pulling data from
		List<Short> allExchanges = swaps.stream()
				.map(c -> c.getSwapDescriptor().getExchange()).distinct().collect(Collectors.toList());

		// For each cache service, find it's account service and save it if is not known
		Map<Short, AccountService> services = new HashMap<>();
		for (SwapService.Swap swap : swaps) {
			services.computeIfAbsent(swap.getSwapDescriptor().getExchange(), e -> factory.getAccountService(e));
		}

		// For each exchange, retrieve the updated price data
		Map<Short, List<PriceData>> updatedData = new HashMap<>();
		for (short exchange : allExchanges) {
			AccountService service = services.get(exchange);
			List<PriceData> pd = service.getAllPrices();
			if (pd != null) {
				updatedData.put(exchange, pd);
			}
		}
		return updatedData;
	}
}
