package com.sharshar.coinswap.services;

import com.sharshar.coinswap.beans.Swap;
import com.sharshar.coinswap.components.SwapComponent;
import com.sharshar.coinswap.repositories.SwapRepository;
import com.sharshar.coinswap.utils.AccountServiceFactory;
import com.sharshar.coinswap.utils.ScratchConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.inject.Provider;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by lsharshar on 7/16/2018.
 */
@Service
public class TradeService {
	@Autowired
	Provider<SwapComponent> swapComponentProvider;

	@Autowired
	AccountServiceFactory accountServiceFactory;

	@Autowired
	SwapRepository swapRepository;

	List<SwapComponent> swapComponents;

	@Autowired
	private ApplicationContext applicationContext;

	@PostConstruct
	public void bootstrapSwaps() {
		if (swapComponents == null) {
			swapComponents = new ArrayList<>();
		}
		Iterable<Swap> swaps = swapRepository.findAll();
		for (Swap swap : swaps) {
			swapComponents.add(loadComponent(swap.getCoin1(), swap.getCoin2(), swap.getCommissionCoin(),
					swap.getMaxAmountCoin1ToBuy(), swap.getMaxAmountCoin2ToBuy(), swap.getExchange(),
					swap.getDesiredStdDev()));
		}
	}

	private SwapComponent loadComponent(String coin1, String coin2, String commissionAsset, double maxAmountCoin1ToBuy,
										double maxAmountCoin2ToBuy, short exchange, double desiredStdDev) {
		SwapComponent component = applicationContext.getBean(SwapComponent.class, exchange, coin1, coin2,
				commissionAsset, maxAmountCoin1ToBuy, maxAmountCoin2ToBuy, exchange, desiredStdDev);
		component.setAccountService(accountServiceFactory.getAccountService(exchange));
		return component;
	}

	public Swap setActive(long id, boolean active) {
		Swap swap = swapRepository.findById(id).orElse(null);
		if (swap != null) {
			swap.setActive(active);
			swap = swapRepository.save(swap);
		}
		return swap;
	}

	public Swap updateSwap(long id, Swap values) {
		Swap swap = swapRepository.findById(id).orElse(null);
		if (swap != null) {
			values.setTableId(id);
			swap = swapRepository.save(values);
		}
		return swap;
	}

	public SwapComponent addComponement(String coin1, String coin2, String commissionAsset, double maxAmountCoin1ToBuy,
										double maxAmountCoin2ToBuy, short exchange, double desiredStdDev, boolean active) {
		SwapComponent match = getMatch(exchange, coin1, coin2);
		if (match != null) {
			return match;
		}
		if (!validAddition(exchange, coin2, coin2)) {
			return null;
		}
		SwapComponent component = applicationContext.getBean(SwapComponent.class, exchange, coin1, coin2,
				commissionAsset, maxAmountCoin1ToBuy, maxAmountCoin2ToBuy, exchange, desiredStdDev);
		component.setActive(active);
		if (swapComponents == null) {
			swapComponents = new ArrayList<>();
		}
		swapComponents.add(component);
		Swap swap = new Swap().setCoin1(coin1).setCoin2(coin2).setExchange(exchange).setCommissionCoin(commissionAsset)
				.setMaxAmountCoin1ToBuy(maxAmountCoin1ToBuy).setMaxAmountCoin2ToBuy(maxAmountCoin2ToBuy)
				.setDesiredStdDev(desiredStdDev).setActive(active);
		swapRepository.save(swap);
		return component;
	}

	public List<SwapComponent> getSwapComponents() {
		if (swapComponents == null) {
			swapComponents = new ArrayList<>();
		}
		return swapComponents;
	}

	public void removeSwapComponent(short exchange, String coin1, String coin2) {
		SwapComponent match = getMatch(exchange, coin1, coin1);
		while (match != null) {
			swapComponents.remove(match);
			match = getMatch(exchange, coin1, coin1);
		}
		List<Swap> swaps = swapRepository.findByCoin1AndCoin2AndExchange(coin2, coin2, exchange);
		if (swaps != null && !swaps.isEmpty()) {
			swapRepository.deleteAll(swaps);
		}
	}

	private SwapComponent getMatch(short exchange, String coin1, String coin2) {
		List<SwapComponent> components = getSwapComponents();
		return components.stream()
				.filter(c -> coin1.equalsIgnoreCase(c.getCoin1()))
				.filter(c -> coin2.equalsIgnoreCase(c.getCoin2()))
				.filter(c -> c.getAccountService() != null && exchange == c.getAccountService().getExchange())
				.findFirst().orElse(null);
	}

	public boolean validAddition(short exchange, String coin1, String coin2) {
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
		List<SwapComponent> similarOnes = swapComponents.stream().filter(c ->
				c.getCoin1().equalsIgnoreCase(coin1) ||
				c.getCoin1().equalsIgnoreCase(coin2) ||
				c.getCoin2().equalsIgnoreCase(coin1) ||
				c.getCoin2().equalsIgnoreCase(coin2)).collect(Collectors.toList());
		return !(similarOnes != null && !similarOnes.isEmpty());
	}
}
