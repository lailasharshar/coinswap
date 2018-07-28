package com.sharshar.coinswap.services;

import com.sharshar.coinswap.beans.PriceData;
import com.sharshar.coinswap.beans.SwapDescriptor;
import com.sharshar.coinswap.components.SwapExecutor;
import com.sharshar.coinswap.exchanges.AccountService;
import com.sharshar.coinswap.repositories.SwapRepository;
import com.sharshar.coinswap.utils.AccountServiceFactory;
import com.sharshar.coinswap.utils.CoinUtils;
import com.sharshar.coinswap.utils.ScratchConstants;
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
	private AccountServiceFactory accountServiceFactory;

	@Autowired
	private CacheService cacheService;

	@Autowired
	private SwapRepository swapRepository;

	@Autowired
	private ApplicationContext applicationContext;

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
		if (swaps == null) {
			swaps = new ArrayList<>();
		}
		Iterable<SwapDescriptor> swapDescriptors = swapRepository.findAll();
		for (SwapDescriptor swapDescriptor : swapDescriptors) {
			Swap swap = createComponent(swapDescriptor);
			if (swap != null) {
				swaps.add(new Swap().setSwapDescriptor(swapDescriptor).setSwapExecutor(swap.getSwapExecutor()));
			}
		}
	}

	/**
	 * Given a swap db descriptor, create its corresponding executing object.
	 *
	 * @param swapDescriptor - The database object
	 * @return the object that knows how to execute the swap
	 */
	private Swap createComponent(SwapDescriptor swapDescriptor) {
		if (!validAddition(swapDescriptor.getExchange(), swapDescriptor.getCoin1(), swapDescriptor.getCoin2())) {
			return null;
		}
		SwapExecutor component = applicationContext.getBean(SwapExecutor.class, swapDescriptor,
				accountServiceFactory.getAccountService(swapDescriptor.getExchange()),
				deriveBaseCoin(accountServiceFactory.getAccountService(swapDescriptor.getExchange()),
						swapDescriptor.getCoin1(), swapDescriptor.getCoin2()));
		Swap swap = new Swap();
		swap.setSwapDescriptor(swapDescriptor);
		swap.setSwapExecutor(component);
		component.setCache(cacheService.createCache(swapDescriptor, component.getBaseCoin()));
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
		Swap swap = getMatch(swapDescriptor.getExchange(), swapDescriptor.getCoin1(), swapDescriptor.getCoin2());
		if (swap == null) {
			return null;
		}
		swapDescriptor.setActive(active);
		swap.getSwapExecutor().resetActive(active);
		swapDescriptor = swapRepository.save(swapDescriptor);
		return swapDescriptor;
	}

	public SwapDescriptor updateSwap(long id, SwapDescriptor values) {
		SwapDescriptor swapDescriptor = swapRepository.findById(id).orElse(null);
		if (swapDescriptor != null) {
			values.setTableId(id);
			swapDescriptor = swapRepository.save(values);
		}
		return swapDescriptor;
	}

	public SwapDescriptor addComponement(SwapDescriptor swapDescriptor) {
		Swap match = getMatch(swapDescriptor.getExchange(), swapDescriptor.getCoin1(), swapDescriptor.getCoin2());
		if (match != null) {
			return match.getSwapDescriptor();
		}
		if (!validAddition(swapDescriptor.getExchange(), swapDescriptor.getCoin1(), swapDescriptor.getCoin2())) {
			return null;
		}
		Swap component = createComponent(swapDescriptor);
		swaps.add(component);
		return swapRepository.save(swapDescriptor);
	}

	/**
	 * If we don't have a base coin defined, see if you can find one in which the two coins have one in common.
	 * Bitcoin is probably the safest, but in theory, it could find others.
	 *
	 * @return the best base coin to use
	 */
	private String deriveBaseCoin(AccountService accountService, String coin1, String coin2) {
		List<PriceData> priceData = accountService.getAllPrices();
		if (CoinUtils.hasPriceData(coin1 + defaultBaseCurrency, priceData)) {
			return defaultBaseCurrency;
		}
		for (String baseCurrency : baseCurrencies) {
			if (baseCurrency.equalsIgnoreCase(coin1)) {
				return baseCurrency;
			}
			if (baseCurrency.equalsIgnoreCase(coin2)) {
				return baseCurrency;
			}
			if (CoinUtils.hasPriceData(coin1 + baseCurrency, priceData) &&
					CoinUtils.hasPriceData(coin2 + baseCurrency, priceData)) {
				return baseCurrency;
			}
		}
		return null;
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
	public void removeSwapComponent(short exchange, String coin1, String coin2) {
		// Recursively find and remove each match from the list
		Swap match = getMatch(exchange, coin1, coin1);
		while (match != null) {
			swaps.remove(match);
			match = getMatch(exchange, coin1, coin1);
		}

		// Remove it from the database if it exists
		List<SwapDescriptor> swapDescriptors = swapRepository.findByCoin1AndCoin2AndExchange(coin2, coin2, exchange);
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
	private Swap getMatch(short exchange, String coin1, String coin2) {
		List<Swap> swapList = getSwaps();

		return swapList.stream()
				.filter(c -> coin1.equalsIgnoreCase(c.getSwapDescriptor().getCoin1()))
				.filter(c -> coin2.equalsIgnoreCase(c.getSwapDescriptor().getCoin2()))
				.filter(c -> c.getSwapDescriptor().getExchange() == exchange)
				.findFirst().orElse(null);
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
	private boolean validAddition(short exchange, String coin1, String coin2) {
		// We don't want to add anything that could conflict with another swap
		if (coin1 == null || coin2 == null || coin1.isEmpty() || coin2.isEmpty()) {
			return false;
		}
		if (coin1.equalsIgnoreCase(coin2)) {
			return false;
		}
		if (exchange <= 0 || exchange > ScratchConstants.EXCHANGES.length) {
			return false;
		}
		// find any matches that share the same coin(s)
		List<Swap> similarOnes = swaps.stream().filter(c ->
				c.getSwapDescriptor().getCoin1().equalsIgnoreCase(coin1) ||
						c.getSwapDescriptor().getCoin1().equalsIgnoreCase(coin2) ||
						c.getSwapDescriptor().getCoin2().equalsIgnoreCase(coin1) ||
						c.getSwapDescriptor().getCoin2().equalsIgnoreCase(coin2))
				.collect(Collectors.toList());

		// If there is no overlap, we're valid
		if (similarOnes == null || similarOnes.isEmpty()) {
			return true;
		}

		// If they are the same coin(s), but different exchanges, go for it. No conflict.
		Swap sameExchange = similarOnes.stream()
				.filter(c -> c.getSwapDescriptor().getExchange() == exchange).findFirst().orElse(null);

		return sameExchange == null;
	}
}
