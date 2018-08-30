package com.sharshar.coinswap.services;

import com.sharshar.coinswap.beans.SwapDescriptor;
import com.sharshar.coinswap.components.SwapExecutor;
import com.sharshar.coinswap.exchanges.AccountService;
import com.sharshar.coinswap.repositories.SwapRepository;
import com.sharshar.coinswap.repositories.TickerRepository;
import com.sharshar.coinswap.utils.AccountServiceFinder;
import com.sharshar.coinswap.utils.ScratchConstants;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Enables us to load, add and remove swap components. This includes the descriptor from the database
 * and the executor that is responsible for executing the swap
 * <p>
 * Created by lsharshar on 7/16/2018.
 */
@Service
public class SwapService {
	private Logger logger = LogManager.getLogger();

	@Autowired
	private MasterSettingsService masterSettingsService;


	/**
	 * An object that contains all the information about a swap in one place
	 */
	public class Swap {
		private SwapExecutor swapExecutor;
		private SwapDescriptor swapDescriptor;

		public SwapExecutor getSwapExecutor() {
			return swapExecutor;
		}

		private Swap setSwapExecutor(SwapExecutor swapExecutor) {
			this.swapExecutor = swapExecutor;
			return this;
		}

		public SwapDescriptor getSwapDescriptor() {
			return swapDescriptor;
		}

		private Swap setSwapDescriptor(SwapDescriptor swapDescriptor) {
			this.swapDescriptor = swapDescriptor;
			return this;
		}
	}

	@Autowired
	private CacheService cacheService;

	@Autowired
	private SwapRepository swapRepository;

	@Autowired
	private TickerRepository tickerRepository;

	@Autowired
	private ApplicationContext applicationContext;

	@Autowired
	AccountServiceFinder accountServiceFinder;

	@Value("${defaultBaseCurrency}")
	private String defaultBaseCurrency;

	@Value("#{'${baseCurrencies}'.split(',')}")
	private List<String> baseCurrencies;

	private List<Swap> swaps;

	/**
	 * Bootstrap the swaps by loading the descriptors from the database and creating swap executors with that data
	 */
	@PostConstruct
	public void bootstrapSwaps() {
		logger.info("Bootstrapping swaps");
		if (swaps == null) {
			swaps = new ArrayList<>();
		}
		Iterable<SwapDescriptor> swapDescriptors = swapRepository.findAll();
		for (SwapDescriptor swapDescriptor : swapDescriptors) {
			Swap swap = createComponent(swapDescriptor);
			if (swap != null) {
				swaps.add(swap);
			}
		}
	}

	public void updateVolume() {
		for (SwapService.Swap swap : getSwaps()) {
			SwapDescriptor descriptor = swap.getSwapDescriptor();
			AccountService accountService = accountServiceFinder.getAccountService(descriptor.getExchangeObj());
			descriptor.setLastVolume1(accountService.get24HourVolume(descriptor.getCoin1() + descriptor.getBaseCoin()));
			descriptor.setLastVolume2(accountService.get24HourVolume(descriptor.getCoin2() + descriptor.getBaseCoin()));
			swapRepository.save(descriptor);
		}
	}


	public void shutdown() {
		logger.info("Shutting down trade service");
		masterSettingsService.shutdown();
	}

	public void pause() {
		logger.info("Pausing trade service");
		masterSettingsService.pauseService();
	}

	public void resume() {
		logger.info("Resuming trade service - refreshing data");
		masterSettingsService.resumeService();
		// If we've paused, we need to reset so we don't act on old data
		if (swaps != null) {
			for (Swap swap : swaps) {
				swap.getSwapExecutor().getCache().clear();
				swap.getSwapExecutor().backFillData();
			}
		}
	}
	public Swap createComponent(SwapDescriptor swapDescriptor) {
		return createComponent(swapDescriptor, false);
	}

	/**
	 * Given a swap db descriptor, create its corresponding executing object.
	 *
	 * @param swapDescriptor - The database object
	 * @return the object that knows how to execute the swap
	 */
	public Swap createComponent(SwapDescriptor swapDescriptor, boolean test) {
		if (!validAddition(swapDescriptor.getExchangeObj(),
				swapDescriptor.getCoin1(), swapDescriptor.getCoin2(), swapDescriptor.getActive(),
				swapDescriptor.getSimulate(), test)) {
			return null;
		}
		logger.info("Creating component - " + swapDescriptor);
		AccountService service = accountServiceFinder.getAccountService(swapDescriptor.getExchangeObj());
		swapDescriptor.setLastVolume1(service.get24HourVolume(swapDescriptor.getCoin1() + swapDescriptor.getBaseCoin()));
		swapDescriptor.setLastVolume2(service.get24HourVolume(swapDescriptor.getCoin2() + swapDescriptor.getBaseCoin()));
		SwapExecutor component = applicationContext.getBean(SwapExecutor.class, swapDescriptor, service);
		Swap swap = new Swap();
		swap.setSwapDescriptor(swapDescriptor);
		swap.setSwapExecutor(component);
		component.setCache(cacheService.createCache(swapDescriptor, tickerRepository.findAll()));
		component.backFillData();
		if (swapDescriptor.getSimulate() != null && swapDescriptor.getSimulate()) {
			component.seedMeMoney(1.0);
		}
		return swap;
	}

	/**
	 * Activate or deactivate a swap. This will require data to be reset so that it doesn't carry any data over from
	 * the simulation/actual trading.
	 *
	 * @param id - the database id of the swap object
	 * @param active - true to set it active or false to set it to similated only
	 * @return the saved object
	 */
	public SwapDescriptor setActive(long id, boolean active) {
		SwapDescriptor swapDescriptor = swapRepository.findById(id).orElse(null);
		if (swapDescriptor == null) {
			return null;
		}
		Swap swap = getMatch(swapDescriptor.getExchangeObj(), swapDescriptor.getCoin1(), swapDescriptor.getCoin2());
		if (swap == null) {
			return null;
		}
		logger.info("Setting " + active + " - " + swapDescriptor);
		swapDescriptor.setActive(active);
		swap.getSwapExecutor().resetActive(active);
		swapDescriptor = swapRepository.save(swapDescriptor);
		return swapDescriptor;
	}

	/**
	 * Update the values of the swap object
	 *
	 * @param id - The id to update
	 * @param values - The new values
	 * @return - the saved value
	 */
	public SwapDescriptor updateSwap(long id, SwapDescriptor values) {
		SwapDescriptor swapDescriptor = swapRepository.findById(id).orElse(null);
		if (swapDescriptor != null) {
			values.setTableId(id);
			swapDescriptor = swapRepository.save(values);
		}
		return swapDescriptor;
	}

	/**
	 * Create a new component. If it already exists, return the old one
	 *
	 * @param swapDescriptor - A description of the swap
	 * @return the swap descriptor
	 */
	public SwapDescriptor addComponement(SwapDescriptor swapDescriptor) {
		Swap match = getMatch(swapDescriptor.getExchangeObj(), swapDescriptor.getCoin1(), swapDescriptor.getCoin2());
		if (match != null) {
			return match.getSwapDescriptor();
		}
		if (!validAddition(swapDescriptor.getExchangeObj(), swapDescriptor.getCoin1(), swapDescriptor.getCoin2(),
				swapDescriptor.getActive(), swapDescriptor.getSimulate())) {
			return null;
		}
		Swap component = createComponent(swapDescriptor);
		swaps.add(component);
		return swapRepository.save(swapDescriptor);
	}

	public List<Swap> getSwaps() {
		return swaps;
	}

	/**
	 * Remove the swap from the swaps. The swap will no longer occur
	 *
	 * @param exchange - the exchange to remove it from
	 * @param coin1 - the first coin
	 * @param coin2 - the second coin
	 */
	public void removeSwapComponent(ScratchConstants.Exchange exchange, String coin1, String coin2) {
		// Recursively find and remove each match from the list
		Swap match = getMatch(exchange, coin1, coin1);
		while (match != null) {
			swaps.remove(match);
			logger.info("Removing: " + match.getSwapDescriptor());
			match = getMatch(exchange, coin1, coin1);
		}

		// Remove it from the database if it exists
		List<SwapDescriptor> swapDescriptors = swapRepository.findByCoin1AndCoin2AndExchange(coin2, coin2, exchange.getValue());
		if (swapDescriptors != null && !swapDescriptors.isEmpty()) {
			swapRepository.deleteAll(swapDescriptors);
		}
	}

	/**
	 * Given an exchange, coin1 and coin2, return if there is a match in our swap descriptors
	 *
	 * @param exchange - the exchange this swap will occur on
	 * @param coin1    - the first coin
	 * @param coin2    - the second coin
	 * @return any matches from the defined swaps
	 */
	private Swap getMatch(ScratchConstants.Exchange exchange, String coin1, String coin2) {
		List<Swap> swapList = getSwaps();

		return swapList.stream()
				.filter(c -> coin1.equalsIgnoreCase(c.getSwapDescriptor().getCoin1()))
				.filter(c -> coin2.equalsIgnoreCase(c.getSwapDescriptor().getCoin2()))
				.filter(c -> c.getSwapDescriptor().getExchangeObj() == exchange)
				.findFirst().orElse(null);
	}

	private boolean validAddition(ScratchConstants.Exchange exchange, String coin1, String coin2, Boolean active,
								  Boolean simulation) {
		return validAddition(exchange, coin1, coin2, active, simulation, false);
	}
	/**
	 * Do some basic validation that the swap is valid.
	 * - Does the exchange exist
	 * - Are coin1 and coin2 the same coin?
	 * - Does this swap overlap coins with another swap? Since we are swapping data from coin to coin, if
	 * another swap uses one of the coins, the results can be unclear - If the coins are the same coins,
	 * but on different exchanges, that's fine, the balances won't interfere.
	 *
	 * @param exchange - the exchange
	 * @param coin1    - The first coin
	 * @param coin2    - the second coin
	 * @return if it is a valid addition
	 */
	private boolean validAddition(ScratchConstants.Exchange exchange, String coin1, String coin2, Boolean active,
								  Boolean simulation, boolean test) {
		if (active == null || !active) {
			return false;
		}
		// We don't want to add anything that could conflict with another swap
		if (coin1 == null || coin2 == null || coin1.isEmpty() || coin2.isEmpty()) {
			return false;
		}
		if (coin1.equalsIgnoreCase(coin2)) {
			return false;
		}
		// find any matches that share the same coin(s)
		List<Swap> similarOnes = swaps.stream().filter(c ->
				c.getSwapDescriptor().getCoin1().equalsIgnoreCase(coin1) ||
						c.getSwapDescriptor().getCoin1().equalsIgnoreCase(coin2) ||
						c.getSwapDescriptor().getCoin2().equalsIgnoreCase(coin1) ||
						c.getSwapDescriptor().getCoin2().equalsIgnoreCase(coin2))
				.collect(Collectors.toList());

		// If there is no overlap, we're valid - or if we're just doing a simulation
		if (similarOnes == null || similarOnes.isEmpty() || (simulation != null && simulation) || test) {
			return true;
		}

		// If they are the same coin(s), but different exchanges, go for it. No conflict.
		Swap sameExchange = similarOnes.stream()
				.filter(c -> c.getSwapDescriptor().getExchangeObj() == exchange).findFirst().orElse(null);

		return sameExchange == null;
	}
}
