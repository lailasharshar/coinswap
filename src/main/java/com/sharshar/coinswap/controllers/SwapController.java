package com.sharshar.coinswap.controllers;

import com.sharshar.coinswap.beans.Swap;
import com.sharshar.coinswap.components.SwapComponent;
import com.sharshar.coinswap.services.TradeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by lsharshar on 7/20/2018.
 */
@RestController
public class SwapController {

	@Autowired
	TradeService tradeService;

	@PostMapping("/swaps")
	public Swap addSwapComponent(@RequestParam String coin1, @RequestParam String coin2,
								 @RequestParam String commissionAsset,
								 @RequestParam double maxAmountCoin1ToBuy,
								 @RequestParam double maxAmountCoin2ToBuy,
								 @RequestParam double stdDevToUse,
								 @RequestParam short exchange) {
		SwapComponent swapComponent = tradeService.addComponement(coin1, coin2, commissionAsset,
				maxAmountCoin1ToBuy, maxAmountCoin2ToBuy, exchange, stdDevToUse, false);
		return convertToSwap(swapComponent);
	}

	@DeleteMapping("/swaps")
	public boolean removeSwapComponent(@RequestParam String coin1, @RequestParam String coin2,
										  @RequestParam short exchange) {
		tradeService.removeSwapComponent(exchange, coin1, coin2);
		return true;
	}

	@GetMapping("/swaps")
	public List<Swap> getComponents() {
		return convertToSwap(tradeService.getSwapComponents());
	}

	@PostMapping("/swaps/{id}")
	private Swap setStatus(@PathVariable long id, @RequestParam Boolean active) {
		return tradeService.setActive(id, active);
	}

	@PutMapping("/swaps/{id}")
	private Swap setStatus(@PathVariable long id, @RequestBody Swap swap) {
		return tradeService.updateSwap(id, swap);
	}

	private List<Swap> convertToSwap(List<SwapComponent> swapComponents) {
		if (swapComponents == null) {
			return new ArrayList<>();
		}
		return swapComponents.stream().map(this::convertToSwap).collect(Collectors.toList());
	}


	private Swap convertToSwap(SwapComponent swapComponent) {
		if (swapComponent == null) {
			return null;
		}
		Swap swap = new Swap();
		swap.setCoin1(swapComponent.getCoin1());
		swap.setCoin2(swapComponent.getCoin2());
		swap.setExchange(swapComponent.getExchange());
		swap.setActive(swapComponent.isActive());
		swap.setCommissionCoin(swapComponent.getCommissionAsset());
		swap.setMaxAmountCoin1ToBuy(swapComponent.getMaxAmountCoin1ToBuy());
		swap.setMaxAmountCoin2ToBuy(swapComponent.getMaxAmountCoin2ToBuy());
		swap.setActive(swapComponent.isActive());
		swap.setDesiredStdDev(swapComponent.getCache().getDesiredStdDeviation());
		return swap;
	}
}
