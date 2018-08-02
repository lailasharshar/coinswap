package com.sharshar.coinswap.services;

import com.sharshar.coinswap.beans.SwapDescriptor;
import com.sharshar.coinswap.beans.Ticker;
import com.sharshar.coinswap.beans.simulation.*;
import com.sharshar.coinswap.exchanges.AccountService;
import com.sharshar.coinswap.repositories.SimulationRunRepository;
import com.sharshar.coinswap.repositories.SimulationRunSnapshotRepository;
import com.sharshar.coinswap.repositories.TickerRepository;
import com.sharshar.coinswap.utils.AccountServiceFactory;
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
	private AccountServiceFactory accountServiceFactory;

	@Autowired
	private TickerRepository tickerRepository;

	@Autowired
	private NotificationService notificationService;

	private List<Ticker> tickerList;

	@Value("#{'${baseCurrencies}'.split(',')}")
	private List<String> baseCurrencies;

	@Value("${defaultBaseCurrency}")
	private String defaultBaseCurrency;

	@Value("${commissionAsset}")
	private String commissionAsset;

	//@Scheduled(fixedRateString = "${timing.updateTicker}", initialDelayString = "${timing.initialDelay}")
	public void loadTickers() {
		load(ScratchConstants.BINANCE);
	}

	private List<Ticker> getTickerList() {
		if (this.tickerList == null) {
			this.tickerList = new ArrayList<>();
			Iterable<Ticker> tickerIterable = tickerRepository.findAll();
			tickerIterable.forEach(this.tickerList::add);
		}
		return tickerList;
	}

	private void load(short exchange) {
		AccountService accountService = accountServiceFactory.getAccountService(exchange);
		// Retrieve the list from exchange
		logger.info("Loading " + ScratchConstants.EXCHANGES[exchange] + " Binance Tickers");
		List<Ticker> tickers;
		try {
			tickers = accountService.getTickerDefinitions();
		} catch (Exception ex) {
			logger.error("Unable to load Binance data", ex);
			return;
		}
		logger.info(tickers.size() + " Tickers");
		List<Ticker> addedTickers = new ArrayList<>();
		// If they already exist, update any data about them, otherwise insert them
		Date now = new Date();
		for (Ticker ticker : tickers) {
			ticker.setLastVolume(accountService.get24HourVolume(ticker.getTickerBase()));
			Ticker found = getInList(ticker, getTickerList());
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
		for (Ticker dbticker : getTickerList()) {
			Ticker found = getInList(dbticker, tickers);
			// We didn't find it in the new list, and it hasn't already been retired, retire it
			if (found == null && dbticker.getRetired() == null) {
				retiredTickers.add(dbticker);
				dbticker.setRetired(now);
				tickerRepository.save(dbticker);
			}
		}
		// Notify any changes
		notifyChanges(addedTickers, retiredTickers);
	}

	/*
	These methods remove some of the searching from other methods
	 */
	private Ticker getInList(Ticker ticker, List<Ticker> tickers) {
		if (tickers == null || tickers.isEmpty() || ticker == null) {
			return null;
		}
		return tickers.stream().filter(c -> c.getBase() != null && c.getBase().equalsIgnoreCase(ticker.getBase()))
				.filter(c -> c.getTicker() != null && c.getTicker().equalsIgnoreCase(ticker.getTicker()))
				.filter(c -> c.getExchange() == ticker.getExchange()).findFirst().orElse(null);
	}

	private void notifyChanges(List<Ticker> addedTickers, List<Ticker> removedTickers) {
		if ((addedTickers == null || addedTickers.isEmpty())
				&& (removedTickers == null || removedTickers.isEmpty())) {
			// Both are empty, do nothing
			return;
		}
		StringBuilder content = new StringBuilder();
		if (addedTickers != null && !addedTickers.isEmpty()) {
			content.append("Added Tickers (").append(addedTickers.size()).append("): <br><br>");
			logger.info("Added Tickers (" + addedTickers.size() + ")");
			for (Ticker s : addedTickers) {
				content.append(s.getTableId()).append(s.getBase()).append("<br>");
			}
			content.append("<br><br>");
		}
		if (removedTickers != null && !removedTickers.isEmpty()) {
			content.append("Removed Tickers (").append(removedTickers.size()).append("): <br><br>");
			logger.info("Removed Tickers (" + removedTickers.size() + ")");
			for (Ticker s : removedTickers) {
				content.append(s.getTicker()).append(s.getBase()).append("<br>");
			}
			content.append("<br><br>");
		}
		try {
			String subject = "Exchange Ticker Changes (" +
					(addedTickers == null ? 0 : addedTickers.size()) +
					" Added/" +
					(removedTickers == null ? 0 : removedTickers.size()) +
					" Removed";
			notificationService.notifyMe(subject, content.toString());
		} catch (Exception ex) {
			logger.error("Unable to alert to new/retired tickers: " + content.toString(), ex);
		}
	}

	@Scheduled(fixedRate = 5000)
	public void runTimedSimulation() {
		runRandomSimulation();
	}

	public void runRandomSimulation() {
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
		SwapDescriptor sd = new SwapDescriptor().setCoin1(ticker1.getTicker()).setCoin2(ticker2.getTicker())
				.setActive(false).setCommissionCoin(commissionAsset).setDesiredStdDev(desiredStdDev)
				.setExchange(ScratchConstants.BINANCE);
		runSimulation(sd, defaultBaseCurrency, ONE_DAY);
	}

	private void runSimulation(SwapDescriptor swapDescriptor, String baseCoin, long checkUpInterval) {
		logger.info("Running Simulation: " + swapDescriptor.getCoin1() + "/" + swapDescriptor.getCoin2()
				+ " - " + String.format("%.4f", swapDescriptor.getDesiredStdDev()));
		double seedMoney = 1.0;
		long startTime = System.currentTimeMillis();
		SimulatorRecord record = historicalAnalysisService.simulateHistoricalAnalysis(swapDescriptor.getCoin1(),
				swapDescriptor.getCoin2(), baseCoin, swapDescriptor.getExchange(), swapDescriptor.getCommissionCoin(),
				checkUpInterval, swapDescriptor.getDesiredStdDev(), seedMoney);
		long endTime = System.currentTimeMillis();
		List<SnapshotDescriptor> snapshots = record.getSnapshotDescriptorList();
		SimulationRun run = new SimulationRun().setBaseCoin(baseCoin).setCoin1(swapDescriptor.getCoin1())
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
			System.out.println("stddev: " + run.getStdDev() + ", end amount: " + run.getEndAmount() +
					", mean change: " + run.getMeanChange() + ", stddevchange: " + run.getStdDevChange());
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
