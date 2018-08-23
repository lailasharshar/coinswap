package com.sharshar.coinswap.services;

import com.sharshar.coinswap.beans.SwapDescriptor;
import com.sharshar.coinswap.beans.Ticker;
import com.sharshar.coinswap.beans.simulation.*;
import com.sharshar.coinswap.exchanges.AccountService;
import com.sharshar.coinswap.repositories.SimulationRunRepository;
import com.sharshar.coinswap.repositories.SimulationRunSnapshotRepository;
import com.sharshar.coinswap.repositories.TickerRepository;
import com.sharshar.coinswap.utils.AccountServiceFinder;
import com.sharshar.coinswap.utils.ScratchConstants;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

/**
 * Used to manage the list of active currencies on different exchanges. Periodically polls exchanges for all
 * supported coin/base pairs
 *
 * Created by lsharshar on 7/16/2018.
 */
@Service
public class TickerService {
	private static final long ONE_DAY = 1000L * 60 * 60 * 24;

	private Logger logger = LogManager.getLogger();

	@Autowired
	private SimulationRunRepository simulationRunRepository;

	@Autowired
	private SimulationRunSnapshotRepository simulationRunSnapshotRepository;

	@Autowired
	private HistoricalAnalysisService historicalAnalysisService;

	@Autowired
	private AccountServiceFinder accountServiceFactory;

	@Autowired
	private TickerRepository tickerRepository;

	@Autowired
	private MessageService messageService;

	private List<Ticker> tickerList;

	@Value("#{'${baseCurrencies}'.split(',')}")
	private List<String> baseCurrencies;

	@Value("${defaultBaseCurrency}")
	private String defaultBaseCurrency;

	@Value("${commissionAsset}")
	private String commissionAsset;

	@Scheduled(fixedRateString = "${timing.updateTicker}", initialDelayString = "${timing.initialDelay}")
	public void loadTickers() {
		reconcileDbListWithExchangeList(ScratchConstants.Exchange.BINANCE);
	}

	private List<Ticker> loadTickerListFromDb() {
		if (this.tickerList == null) {
			this.tickerList = new ArrayList<>();
			Iterable<Ticker> tickerIterable = tickerRepository.findAll();
			tickerIterable.forEach(this.tickerList::add);
		}
		return tickerList;
	}

	public List<Ticker> getTickersFromExchange(ScratchConstants.Exchange exchange) {
		AccountService accountService = accountServiceFactory.getAccountService(exchange);
		// Retrieve the list from exchange
		logger.info("Loading " + exchange.getExchangeName() + " Binance Tickers");
		List<Ticker> tickers = new ArrayList<>();
		try {
			tickers = accountService.getTickerDefinitions();
		} catch (Exception ex) {
			logger.error("Unable to reconcileDbListWithExchangeList " + exchange.getExchangeName() + " data", ex);
			return new ArrayList<>();
		}
		for (Ticker ticker : tickers) {
			ticker.setLastVolume(accountService.get24HourVolume(ticker.getAssetAndBase()));
		}
		return tickers;
	}

	private void reconcileDbListWithExchangeList(ScratchConstants.Exchange exchange) {
		List<Ticker> tickers = getTickersFromExchange(exchange);
		logger.info(tickers.size() + " Tickers");

		List<Ticker> addedTickers = new ArrayList<>();

		// If they already exist, update any data about them, otherwise insert them
		Date now = new Date();
		for (Ticker ticker : tickers) {
			Ticker found = getInList(ticker, loadTickerListFromDb());
			if (found == null) {
				addedTickers.add(ticker);
				ticker.setFoundDate(now);
			} else {
				// tie the original db item to the data so we can update any data if necessary
				ticker.setTableId(found.getTableId());
				// Probably already null, but if we found it in the db and it was returned, shouldn't be set
				ticker.setRetired(null);
			}
			ticker.setUpdatedDate(now);
		}
		tickerRepository.saveAll(tickers);

		// Find any that weren't in the list and retire them
		List<Ticker> retiredTickers = new ArrayList<>();

		// Figure out which ones that need to be retired
		for (Ticker dbTicker : loadTickerListFromDb()) {
			Ticker found = getInList(dbTicker, tickers);
			// We didn't find it in the new list, and it hasn't already been retired, retire it
			if (found == null && dbTicker.getRetired() == null) {
				retiredTickers.add(dbTicker);
				dbTicker.setRetired(now);
				tickerRepository.save(dbTicker);
			}
		}
		// Notify any changes
		messageService.notifyChanges(addedTickers, retiredTickers);
	}

	/*
	These methods remove some of the searching from other methods
	 */
	private Ticker getInList(Ticker ticker, List<Ticker> tickers) {
		if (tickers == null || tickers.isEmpty() || ticker == null) {
			return null;
		}
		return tickers.stream().filter(c -> c.getBase() != null && c.getBase().equalsIgnoreCase(ticker.getBase()))
				.filter(c -> c.getAsset() != null && c.getAsset().equalsIgnoreCase(ticker.getAsset()))
				.filter(c -> c.getExchange() == ticker.getExchange()).findFirst().orElse(null);
	}



	//@Scheduled(fixedRate = 5000)
	public void runTimedSimulation() {
		runRandomSimulation();
	}

	void runRandomSimulation() {
		if (tickerList == null) {
			loadTickers();
		}
		List<Ticker> itemsToUse = tickerList.stream()
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
		double desiredStdDev = (Math.random() * 3);
		Ticker ticker1 = itemsToUse.get(selectItem1);
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
		SimulationRun run = new SimulationRun().setBaseCoin(swapDescriptor.getBaseCoin()).setCoin1(swapDescriptor.getCoin1())
				.setCoin2(swapDescriptor.getCoin2()).setCommissionCoin(swapDescriptor.getCommissionCoin())
				.setStartDate(snapshots.get(0).getSnapshotDate())
				.setEndDate(snapshots.get(snapshots.size() - 1).getSnapshotDate())
				.setSimulationStartTime(new Date(startTime)).setSimulationEndTime(new Date(endTime))
				.setSnapshotInterval(checkUpInterval).setStartAmount(seedMoney)
				.setEndAmount(snapshots.get(snapshots.size() - 1).getTotalValue());
		RaterResults rater = new RaterResults(snapshots);
		run.setMeanChange(rater.getMean());
		run.setStdDev(swapDescriptor.getDesiredStdDev());
		run.setStdDevChange(rater.getStdDev());
		try {
			run = simulationRunRepository.save(run);
		} catch (Exception ex) {
			logger.error("Standard Deviation: " + run.getStdDev() + ", end amount: " + run.getEndAmount() +
					", mean change: " + run.getMeanChange() + ", Standard Deviation Change: " + run.getStdDevChange(), ex);
			return;
		}
		long runId = run.getId();
		List<SimulationRunSnapshot> simSnaps = new ArrayList<>();
		for (SnapshotDescriptor snapshotDescriptor : snapshots) {
			SimulationRunSnapshot snapshot = new SimulationRunSnapshot()
					.setSnapshotDate(snapshotDescriptor.getSnapshotDate())
					.setAmountCoin1(snapshotDescriptor.getAmountCoin1())
					.setAmountCoin2(snapshotDescriptor.getAmountCoin2())
					.setAmountCommissionCoin(snapshotDescriptor.getAmountCommissionCoin())
					.setSimulationId(runId);
			simSnaps.add(snapshot);
		}
		simulationRunSnapshotRepository.saveAll(simSnaps);
	}
}
