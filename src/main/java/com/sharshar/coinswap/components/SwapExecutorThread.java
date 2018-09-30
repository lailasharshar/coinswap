package com.sharshar.coinswap.components;

import com.sharshar.coinswap.beans.PriceData;
import com.sharshar.coinswap.beans.simulation.TradeAction;
import com.sharshar.coinswap.services.MessageService;
import com.sharshar.coinswap.services.SwapService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Created by lsharshar on 9/24/2018.
 */
@Component
@Scope("prototype")
public class SwapExecutorThread implements Runnable {
	Logger logger = LogManager.getLogger();

	@Autowired
	private MessageService messageService;

	private SwapService.Swap swap;
	private boolean buyCoin1;
	private List<PriceData> priceData;

	public SwapExecutorThread(List<PriceData> priceData, SwapService.Swap swap, boolean buyCoin1) {
		this.swap = swap;
		this.buyCoin1 = buyCoin1;
		this.priceData = priceData;
	}

	@Override
	public void run() {
		TradeAction action = null;
		if (buyCoin1) {
			action = swap.getSwapExecutor().swapCoin2ToCoin1(priceData, swap.getSwapExecutor().isSimulate());
		} else {
			action = swap.getSwapExecutor().swapCoin1ToCoin2(priceData, swap.getSwapExecutor().isSimulate());
		}
		messageService.summarizeTrade(swap, action);
	}
}
