package com.sharshar.coinswap.services;

import com.sharshar.coinswap.beans.SwapDescriptor;
import com.sharshar.coinswap.beans.Ticker;
import com.sharshar.coinswap.beans.simulation.*;
import com.sharshar.coinswap.repositories.SimulationRunRepository;
import com.sharshar.coinswap.repositories.TradeActionRepository;
import com.sharshar.coinswap.utils.ScratchConstants;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

import static com.sharshar.coinswap.utils.ScratchConstants.ONE_DAY;

/**
 * Service to simulate a coin swap.
 *
 * Created by lsharshar on 8/26/2018.
 */
@Service
public class SimulationRunner {
	private Logger logger = LogManager.getLogger();

	@Autowired
	private TickerService tickerService;

	@Autowired
	private HistoricalAnalysisService historicalAnalysisService;

	@Value("${defaultBaseCurrency}")
	private String defaultBaseCurrency;

	@Value("${commissionAsset}")
	private String commissionAsset;

	@Autowired
	private SimulationRunRepository simulationRunRepository;

	@Autowired
	private TradeActionRepository tradeActionRepository;

	@Scheduled(fixedRate = 5000)
	public void runTimedSimulation() {
		runRandomSimulation(true);
		runRandomSimulation(false);
	}

	public void runRandomSimulation(boolean btcOnly) {
		List<Ticker> tickers = tickerService.getTickers();
		if (tickers == null) {
			return;
		}
		List<Ticker> newTickers = new ArrayList<>();
		tickers.forEach(c -> newTickers.add(c));
		Ticker btcTicker = new Ticker().setAsset("BTC").setBase("BTC").setExchange(ScratchConstants.Exchange.BINANCE.getValue())
				.setStepSize(0.00001);
		newTickers.add(btcTicker);
		List<Ticker> itemsToUse = newTickers.stream()
				.filter(c -> c.getBase().equalsIgnoreCase(defaultBaseCurrency))
				.filter(c -> c.getRetired() == null)
				.collect(Collectors.toList());
		int numTickers = itemsToUse.size();
		Random random = new Random();
		int selectItem1 = random.nextInt(numTickers);
		int selectItem2 = random.nextInt(numTickers);
		while (selectItem2 == selectItem1) {
			selectItem2 = random.nextInt(numTickers);
		}
		double desiredStdDev = (Math.random() * 1.5);
		Ticker ticker1 = btcTicker;
		if (!btcOnly) {
			ticker1 = itemsToUse.get(selectItem1);
		}
		Ticker ticker2 = itemsToUse.get(selectItem2);
		SwapDescriptor sd = new SwapDescriptor().setCoin1(ticker1.getAsset()).setCoin2(ticker2.getAsset())
				.setActive(false).setCommissionCoin(commissionAsset).setDesiredStdDev(desiredStdDev)
				.setBaseCoin("BTC").setSimulate(true).setMaxPercentVolume(0.1)
				.setExchange(ScratchConstants.Exchange.BINANCE.getValue());
		runSimulation(sd, ONE_DAY);
	}

	private void runSimulation(SwapDescriptor swapDescriptor, long checkUpInterval) {
		logger.info("Running Simulation: " + swapDescriptor.getCoin1() + "/" + swapDescriptor.getCoin2()
				+ " - " + String.format("%.4f", swapDescriptor.getDesiredStdDev()));
		double seedMoney = 1.0;
		long startTime = System.currentTimeMillis();
		SimulatorRecord record = historicalAnalysisService.simulateHistoricalAnalysis(swapDescriptor, checkUpInterval, seedMoney);
		long endTime = System.currentTimeMillis();
		List<SnapshotDescriptor> snapshots = record.getSnapshotDescriptorList();
		historicalAnalysisService.saveSimulation(swapDescriptor, snapshots, checkUpInterval, seedMoney, startTime,
				endTime, record.getTradeActionList());
	}
}
