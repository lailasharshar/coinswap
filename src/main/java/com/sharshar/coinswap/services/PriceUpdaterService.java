package com.sharshar.coinswap.services;

import com.sharshar.coinswap.beans.PriceData;
import com.sharshar.coinswap.components.ExchangeCache;
import com.sharshar.coinswap.components.SwapExecutorThread;
import com.sharshar.coinswap.config.ThreadPool;
import com.sharshar.coinswap.exchanges.AccountService;
import com.sharshar.coinswap.utils.AccountServiceFinder;
import com.sharshar.coinswap.utils.ScratchConstants;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Date;
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

	@Autowired
	private ThreadPool pool;

	@Scheduled(cron = "0 15 * * * *")
	public void updatePriceDataSchedule() {
		if (!masterSettingsService.areWeRunning()) {
			return;
		}
		List<SwapService.Swap> swaps = swapService.getSwaps();
		updatePriceData(swaps);
	}

	public void updatePriceData(List<SwapService.Swap> swaps) {
		logger.info("Updating price data");
		Map<ScratchConstants.Exchange, List<PriceData>> priceDataForAllExchanges = getAllPriceDataForAllExchanges(swaps);
		for (SwapService.Swap swap : swaps) {
			ScratchConstants.Exchange exchange = swap.getSwapDescriptor().getExchangeObj();
			List<PriceData> exchangeData = priceDataForAllExchanges.get(exchange);
			ExchangeCache.Position position = swap.getSwapExecutor().getCache().addPriceData(exchangeData);
			if (swap.getSwapExecutor().getCurrentSwapState() == ScratchConstants.CurrentSwapState.OWNS_COIN_1 &&
					position == ExchangeCache.Position.ABOVE_DESIRED_RATIO) {
				logger.info("Above desired ration - swapping: " + swap.getSwapDescriptor().getCoin1() + " for " +
						swap.getSwapDescriptor().getCoin2());
				launchThread(exchangeData, swap, false);
			} else if (swap.getSwapExecutor().getCurrentSwapState() == ScratchConstants.CurrentSwapState.OWNS_COIN_2 &&
					position == ExchangeCache.Position.BELOW_DESIRED_RATIO) {
				logger.info("Below desired ration - swapping: " + swap.getSwapDescriptor().getCoin2() + " for " +
						swap.getSwapDescriptor().getCoin1());
				launchThread(exchangeData, swap, true);
			}
		}
	}

	public void launchThread(List<PriceData> priceData, SwapService.Swap swap, boolean buyCoin1) {
		if (swap.getSwapExecutor().isInSwap()) {
			logger.info("Already in swap - Continue to wait");
			return;
		}
		logger.info("Launching swap thread");
		SwapExecutorThread thread = new SwapExecutorThread(priceData, swap, buyCoin1);
		pool.threadPoolTaskExecutor().execute(thread);
	}

	public Map<ScratchConstants.Exchange, List<PriceData>> getAllPriceDataForAllExchanges(List<SwapService.Swap> swaps) {
		Map<ScratchConstants.Exchange, List<PriceData>> updatedData = new HashMap<>();
		if (swaps == null) {
			return updatedData;
		}

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
		for (ScratchConstants.Exchange exchange : allExchanges) {
			AccountService service = services.get(exchange);
			List<PriceData> pd = service.getAllPrices();
			// Add the base prices
			if (pd != null) {
				Date updateDate = pd.get(0).getUpdateTime();
				pd.add(new PriceData().setUpdateTime(updateDate).setExchange(exchange).setPrice(1.0).setTicker("BTCBTC"));
				pd.add(new PriceData().setUpdateTime(updateDate).setExchange(exchange).setPrice(1.0).setTicker("ETHETH"));
				pd.add(new PriceData().setUpdateTime(updateDate).setExchange(exchange).setPrice(1.0).setTicker("BNBBNB"));
				updatedData.put(exchange, pd);
			}
		}
		return updatedData;
	}
}
