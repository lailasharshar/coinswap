package com.sharshar.coinswap.services;

import com.sharshar.coinswap.beans.OwnedAsset;
import com.sharshar.coinswap.beans.PriceData;
import com.sharshar.coinswap.exchanges.binance.BinanceAccountServices;
import com.sharshar.coinswap.utils.CoinUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Items to monitor and report summary stuff to me
 *
 * Created by lsharshar on 8/8/2018.
 */
@Service
public class MonitorService {
	private Logger logger = LogManager.getLogger();

	@Autowired
	BinanceAccountServices binanceAccountServices;

	@Autowired
	MessageService messageService;

	@Scheduled(cron = "0 0 8 * * *")
	public void notifyBalanceEveryDay() {
		try {
			logger.info("Sending out daily roundup for Binance");
			messageService.dailyRoundup(binanceAccountServices);
		} catch (Exception ex)	{
			logger.error("Unable to notify me of my daily roundup: ", ex);
		}
	}
}
