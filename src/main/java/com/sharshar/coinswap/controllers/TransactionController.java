package com.sharshar.coinswap.controllers;

import com.sharshar.coinswap.beans.SwapDescriptor;
import com.sharshar.coinswap.beans.Ticker;
import com.sharshar.coinswap.components.SwapExecutor;
import com.sharshar.coinswap.services.SwapService;
import com.sharshar.coinswap.services.TickerService;
import com.sharshar.coinswap.utils.ScratchConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Created by lsharshar on 8/30/2018.
 */
@RestController
public class TransactionController {
	@Autowired
	private SwapService swapService;

	@Autowired
	private TickerService tickerService;

	@PostMapping("/buy")
	public String buy(@RequestParam double amount, @RequestParam String asset,
					  @RequestParam(required = false, defaultValue = "BTC") String base,
					  @RequestParam(required = false, defaultValue = "1") short exchange,
					  @RequestParam(required = false, defaultValue = "BNB") String commission) {
		SwapExecutor swapExecutor = getExecutor(asset, base, commission, exchange);
		if (swapExecutor == null) {
			return "Unable to define the executor";
		}
		return swapExecutor.buyCoin(swapExecutor.getCache().getTicker1(), amount).name();
	}

	@PostMapping("/sell")
	public String sell(@RequestParam double amount, @RequestParam String asset,
					  @RequestParam(required = false, defaultValue = "BTC") String base,
					  @RequestParam(required = false, defaultValue = "1") short exchange,
					  @RequestParam(required = false, defaultValue = "BNB") String commission) {
		SwapExecutor swapExecutor = getExecutor(asset, base, commission, exchange);
		if (swapExecutor == null) {
			return "Unable to define the executor";
		}
		return swapExecutor.sellCoin(swapExecutor.getCache().getTicker1(), amount).name();
	}

	private SwapExecutor getExecutor(String asset, String base, String commission, short exchange) {
		String exampleAsset = "XMR";
		if (asset.equalsIgnoreCase(exampleAsset)) {
			exampleAsset = "BAT";
		}
		SwapService.Swap swap = swapService.createComponent(new SwapDescriptor()
				.setActive(true).setDesiredStdDev(4.0).setExchange(exchange).setCommissionCoin(commission)
				.setBaseCoin(base).setCoin1(asset).setCoin2(exampleAsset));
		if (swap == null) {
			return null;
		}
		SwapExecutor swapExecutor = swap.getSwapExecutor();
		if (swapExecutor == null) {
			return null;
		}
		return swapExecutor;
	}

	private Ticker getTicker(String asset, String base, ScratchConstants.Exchange exchange) {
		List<Ticker> tickers = tickerService.getTickers();
		return tickerService.getInList(asset, base, exchange.getValue(), tickers);
	}
}
