package com.sharshar.coinswap.controllers;

import com.sharshar.coinswap.beans.SwapStatus;
import com.sharshar.coinswap.exchanges.binance.BinanceAccountServices;
import com.sharshar.coinswap.repositories.SwapRepository;
import com.sharshar.coinswap.services.HistoricalAnalysisService;
import com.sharshar.coinswap.services.MonitorService;
import com.sharshar.coinswap.services.SwapService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

/**
 * Controller to view and update the system status
 *
 * Created by lsharshar on 8/28/2018.
 */
@RestController
public class StatusController {
	@Autowired
	private SwapService swapService;

	@Autowired
	private BinanceAccountServices binance;

	@Autowired
	private SwapRepository swapRepository;

	@Autowired
	private HistoricalAnalysisService historicalAnalysisService;

	@Autowired
	private MonitorService monitorService;

	@GetMapping("/shutdown")
	private String shutdown() {
		swapService.shutdown();
		return "Shutdown";
	}
	@GetMapping("/pause")
	private String pause() {
		swapService.pause();
		return "Pausing";
	}
	@GetMapping("/resume")
	private String resume() {
		swapService.resume();
		return "Resuming...";
	}

	@GetMapping("/status")
	private String status() {
		return monitorService.getStatus();
	}

	@GetMapping("/swapStatus")
	private List<SwapStatus> getSwapStatuses() {
		List<SwapStatus> statuses = new ArrayList<>();
		List<SwapService.Swap> swaps = swapService.getSwaps();
		for (SwapService.Swap swap : swaps) {
			statuses.add(swap.getSwapStatus());
		}
		return statuses;
	}

	@GetMapping("/swapStatus/{id}")
	private SwapStatus getSwapStatus(@PathVariable Long id) {
		List<SwapService.Swap> swaps = swapService.getSwaps();
		for (SwapService.Swap swap : swaps) {
			if (id == swap.getSwapDescriptor().getTableId()) {
				return swap.getSwapStatus();
			}
		}
		return null;
	}
}
