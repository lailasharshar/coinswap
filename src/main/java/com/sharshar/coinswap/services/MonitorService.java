package com.sharshar.coinswap.services;

import com.binance.api.client.domain.account.Account;
import com.sharshar.coinswap.beans.PriceData;
import com.sharshar.coinswap.exchanges.binance.BinanceAccountServices;
import com.sharshar.coinswap.repositories.SwapRepository;
import com.sharshar.coinswap.utils.ScratchConstants;
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
	private static final Logger logger = LogManager.getLogger();

	@Autowired
	BinanceAccountServices binanceAccountServices;

	@Autowired
	MessageService messageService;

	@Autowired
	private SwapService swapService;

	@Autowired
	private BinanceAccountServices binance;

	@Autowired
	private SwapRepository swapRepository;

	@Autowired
	private HistoricalAnalysisService historicalAnalysisService;

	@Scheduled(cron = "0 0 7 * * *")
	public void notifyBalanceEveryDay() {
		try {
			logger.info("Sending out daily roundup for Binance");
			messageService.dailyRoundup(binanceAccountServices);
		} catch (Exception ex)	{
			logger.error("Unable to notify me of my daily roundup: ", ex);
		}
	}

	@Scheduled(cron = "0 0,30,45 * * * ?")
	public void scheduledCheckup() {
		getStatus();
	}

	public String getStatus() {
		// Check the database
		String status = "";
		try {
			swapRepository.findAll();
		} catch (Exception ex) {
			status += "DB error: " + ex.getMessage() + "\n";
		}
		// Check to see if we have access to Binance
		try {
			Account account = binance.getMyAccount();
			if (account == null) {
				status += "Binance error: Account can't be accessed\n";
			}
		} catch (Exception ex) {
			status += "Binance error: " + ex.getMessage() + "\n";
		}
		// Check to see if we have access to CryptoCompare
		/*
		try {
			List<List<PriceData>> data = historicalAnalysisService.loadPriceData(
					"TUSD", "BTC", "BTC", "BNB", 10, ScratchConstants.Exchange.BINANCE);
			if (data == null || data.size() < 3 || data.get(0) == null || data.get(0).isEmpty()) {
				status += "CryptoCompare error: Not returning data\n";
			}
		} catch (Exception ex) {
			status += "CryptoCompare error: " + ex.getMessage() + "\n";
		}
		*/
		if (status.length() > 0) {
			messageService.notifyStatusErrors(status);
			return status;
		}
		return "SUCCESS";
	}
}
