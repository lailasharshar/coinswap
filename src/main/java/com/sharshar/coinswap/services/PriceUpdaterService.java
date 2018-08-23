package com.sharshar.coinswap.services;

import com.sharshar.coinswap.beans.PriceData;
import com.sharshar.coinswap.beans.simulation.TradeAction;
import com.sharshar.coinswap.components.ExchangeCache;
import com.sharshar.coinswap.components.SwapExecutor;
import com.sharshar.coinswap.exchanges.AccountService;
import com.sharshar.coinswap.utils.AccountServiceFinder;
import com.sharshar.coinswap.utils.ScratchConstants;
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
	private MessageService messageService;

	@Autowired
	private AccountServiceFinder factory;

	@Autowired
	private MasterSettingsService masterSettingsService;

	@Scheduled(fixedRateString = "${timing.updatePrice}", initialDelayString = "${timing.initialDelay}")
	public void updatePriceData() {
		if (!masterSettingsService.areWeRunning()) {
			return;
		}
		logger.info("Updating price data");
		List<SwapService.Swap> swaps = swapService.getSwaps();
		Map<ScratchConstants.Exchange, List<PriceData>> priceDataForAllExchanges = getAllPriceDataForAllExchanges(swaps);
		for (SwapService.Swap swap : swaps) {
			ScratchConstants.Exchange exchange = swap.getSwapDescriptor().getExchangeObj();
			List<PriceData> exchangeData = priceDataForAllExchanges.get(exchange);
			ExchangeCache.Position position = swap.getSwapExecutor().getCache().addPriceData(exchangeData);
			TradeAction action = null;
			if (swap.getSwapExecutor().getCurrentSwapState() == SwapExecutor.CurrentSwapState.OWNS_COIN_1 &&
					position == ExchangeCache.Position.ABOVE_DESIRED_RATIO) {
				logger.info("Above desired ration - swapping: " + swap.getSwapDescriptor().getCoin1() + " for " +
						swap.getSwapDescriptor().getCoin2());
				action = swap.getSwapExecutor().swapCoin1ToCoin2(exchangeData, swap.getSwapDescriptor().getSimulate());
			} else {
				if (swap.getSwapExecutor().getCurrentSwapState() == SwapExecutor.CurrentSwapState.OWNS_COIN_2 &&
						position == ExchangeCache.Position.BELOW_DESIRED_RATIO) {
					logger.info("Below desired ration - swapping: " + swap.getSwapDescriptor().getCoin2() + " for " +
							swap.getSwapDescriptor().getCoin1());
					action = swap.getSwapExecutor().swapCoin2ToCoin1(exchangeData, swap.getSwapDescriptor().getSimulate());
				}
			}
			messageService.summarizeTrade(swap, action);
		}
	}

	private Map<ScratchConstants.Exchange, List<PriceData>> getAllPriceDataForAllExchanges(List<SwapService.Swap> swaps) {
		// Find the unique exchanges we're pulling data from
		List<ScratchConstants.Exchange> allExchanges = swaps.stream()
				.map(c -> c.getSwapDescriptor().getExchangeObj()).distinct().collect(Collectors.toList());

		// For each cache service, find it's account service and save it if is not known
		Map<ScratchConstants.Exchange, AccountService> services = new HashMap<>();
		for (SwapService.Swap swap : swaps) {
			services.computeIfAbsent(
					swap.getSwapDescriptor().getExchangeObj(),
					e -> factory.getAccountService(e));
		}

		// For each exchange, retrieve the updated price data
		Map<ScratchConstants.Exchange, List<PriceData>> updatedData = new HashMap<>();
		for (ScratchConstants.Exchange exchange : allExchanges) {
			AccountService service = services.get(exchange);
			List<PriceData> pd = service.getAllPrices();
			if (pd != null) {
				updatedData.put(exchange, pd);
			}
		}
		return updatedData;
	}
}
