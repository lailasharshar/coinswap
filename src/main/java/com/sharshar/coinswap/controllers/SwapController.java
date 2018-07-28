package com.sharshar.coinswap.controllers;

import com.sharshar.coinswap.beans.SwapDescriptor;
import com.sharshar.coinswap.components.SwapExecutor;
import com.sharshar.coinswap.services.SwapService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Allows us to add swap components
 *
 * Created by lsharshar on 7/20/2018.
 */
@RestController
public class SwapController {

	@Autowired
	SwapService swapService;

	@PostMapping("/swaps")
	public SwapDescriptor addSwapComponent(@RequestBody SwapDescriptor swap) {
		return swapService.addComponement(swap);
	}

	@DeleteMapping("/swaps")
	public List<SwapDescriptor> removeSwapComponent(@RequestParam String coin1, @RequestParam String coin2,
													@RequestParam short exchange) {
		swapService.removeSwapComponent(exchange, coin1, coin2);
		return getComponents();
	}

	@GetMapping("/swaps")
	public List<SwapDescriptor> getComponents() {
		return convertToSwap(swapService.getSwaps());
	}

	@PostMapping("/swaps/{id}")
	private SwapDescriptor setStatus(@PathVariable long id, @RequestParam Boolean active) {
		return swapService.setActive(id, active);
	}

	@PutMapping("/swaps/{id}")
	private SwapDescriptor setStatus(@PathVariable long id, @RequestBody SwapDescriptor swap) {
		return swapService.updateSwap(id, swap);
	}

	private List<SwapDescriptor> convertToSwap(List<SwapService.Swap> swapExecutors) {
		if (swapExecutors == null) {
			return new ArrayList<>();
		}
		return swapExecutors.stream().map(c -> c.getSwapDescriptor()).collect(Collectors.toList());
	}
}
