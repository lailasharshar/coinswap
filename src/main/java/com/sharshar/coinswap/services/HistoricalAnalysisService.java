package com.sharshar.coinswap.services;

import com.sharshar.coinswap.beans.PriceData;
import com.sharshar.coinswap.beans.simulation.*;
import com.sharshar.coinswap.beans.SwapDescriptor;
import com.sharshar.coinswap.components.ExchangeCache;
import com.sharshar.coinswap.components.SwapExecutor;
import com.sharshar.coinswap.exchanges.Data;
import com.sharshar.coinswap.exchanges.HistoricalDataPull;
import com.sharshar.coinswap.repositories.SimulationRunRepository;
import com.sharshar.coinswap.repositories.TradeActionRepository;
import com.sharshar.coinswap.utils.ScratchConstants;
import org.apache.commons.lang3.SerializationUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Analyse historical trends and what money would have been made if you did the swap.
 *
 * Created by lsharshar on 7/30/2018.
 */
@Service
public class HistoricalAnalysisService {

	private static final Logger logger = LogManager.getLogger();

	@Autowired
	private HistoricalPriceCache historicalPriceCache;

	@Autowired
	private SwapService swapService;

	@Autowired
	private TradeActionRepository tradeActionRepository;

	@Autowired
	private SimulationRunRepository simulationRunRepository;


	/**
	 * Load the price data, clean up and reformat the list and simulate the swap
	 *
	 * @param descriptor - coin1/coin2/base coin/exchange/commission coin to use
	 * @param checkUpInterval - how often to do a snapshot
	 * @param seedMoney - the initial seed money to use in base coin
	 * @return the simulation results summary
	 */
	public SimulatorRecord simulateHistoricalAnalysis(SwapDescriptor descriptor, long checkUpInterval,
							  double seedMoney) {
		if (descriptor.getCoin1() == null || descriptor.getCoin2() == null ||
				descriptor.getBaseCoin() == null || descriptor.getCoin1().isEmpty() ||
				descriptor.getCoin2().isEmpty() || descriptor.getBaseCoin().isEmpty()) {
			logger.error ("Can't compare " + descriptor.getCoin1() + " against " + descriptor.getCoin2());
			return null;
		}

		if (descriptor.getCoin1().equalsIgnoreCase(descriptor.getCoin2())) {
			// Can't compare against itself
			logger.error ("Can't compare " + descriptor.getCoin1() + " against " + descriptor.getCoin2());
			return null;
		}
		List<List<PriceData>> priceData = loadPriceData(descriptor.getCoin1(), descriptor.getCoin2(),
				descriptor.getBaseCoin(), descriptor.getCommissionCoin(), 5000, descriptor.getExchangeObj());
		List<List<PriceData>> cleanedUpList = cleanUpData(priceData);
		List<List<PriceData>> distributedData = distributeDataByDate(cleanedUpList);
		SwapDescriptor swapDescriptor = SerializationUtils.clone(descriptor);
		swapDescriptor.setSimulate(true);
		swapDescriptor.setActive(true);
		return simulateOverTime(swapDescriptor, distributedData, checkUpInterval, seedMoney);
	}

	/**
	 * Simulate a price swap over time
	 *
	 * @param sd - the description of the swap elements
	 * @param priceOverTime - the price data for the three coins over time increments
	 * @param checkInterval - how often to take a snapshot (so we can analyze consistency of trends)
	 * @param seedMoney - seed money in the base currency to start with
	 * @return a summary of the simulation
	 */
	private SimulatorRecord simulateOverTime(SwapDescriptor sd, List<List<PriceData>> priceOverTime, long checkInterval,
											 double seedMoney) {
		SimulatorRecord record = new SimulatorRecord();
		SwapService.Swap swap = swapService.createComponent(sd);
		if (swap == null) {
			logger.error("Invalid swap descriptor: " + sd.toString());
			return record;
		}
		SwapExecutor executor = swap.getSwapExecutor();

		// Clear out back filled data since when we create something, by default, it's filled with historic data
		// which we are now replacing
		executor.getCache().clear();

		record.setDescriptor(sd);
		record.setInitialBaseInvestment(seedMoney);
		record.setDesiredStdDev(sd.getDesiredStdDev());
		long intervalExpire = 0;

		boolean init = true;
		boolean seeded = false;
		for (List<PriceData> pdList : priceOverTime) {
			ExchangeCache.Position position = swap.getSwapExecutor().getCache().addPriceData(pdList);
			if (init) {
				// Don't seed money until we have the initial price data to accurately describe our initial
				// investment
				Date startDate = pdList.get(0).getUpdateTime();
				intervalExpire = startDate.getTime() + checkInterval;
				init = false;
				record.setStartDate(startDate);
			}
			if (position != ExchangeCache.Position.INSUFFICIENT_DATA && !seeded) {
				executor.seedMeMoney(seedMoney);
				seeded = true;
			}
			if (swap.getSwapExecutor().getCurrentSwapState() == ScratchConstants.CurrentSwapState.OWNS_COIN_1 &&
					position == ExchangeCache.Position.ABOVE_DESIRED_RATIO) {
				TradeAction action = swap.getSwapExecutor().swapCoin1ToCoin2(pdList, swap.getSwapDescriptor().getSimulate());
				record.addTradeAction(action);
			} else if (swap.getSwapExecutor().getCurrentSwapState() == ScratchConstants.CurrentSwapState.OWNS_COIN_2 &&
					position == ExchangeCache.Position.BELOW_DESIRED_RATIO) {
				TradeAction action = swap.getSwapExecutor().swapCoin2ToCoin1(pdList, swap.getSwapDescriptor().getSimulate());
				record.addTradeAction(action);
			}
			if (pdList.get(0).getUpdateTime().getTime() >= intervalExpire) {
				record.addSnapshot(pdList.get(0).getUpdateTime(), executor.getAmountCoin1OwnedFree(),
						executor.getAmountCoin2OwnedFree(), executor.getAmountCommissionAssetFree(),
						pdList.get(0), pdList.get(1), pdList.get(2));
				intervalExpire += checkInterval;
			}
		}
		Date endDate = priceOverTime.get(priceOverTime.size() - 1).get(0).getUpdateTime();
		record.addSnapshot(endDate, executor.getAmountCoin1OwnedFree(),
				executor.getAmountCoin2OwnedFree(), executor.getAmountCommissionAssetFree(),
				priceOverTime.get(priceOverTime.size() - 1).get(0), priceOverTime.get(priceOverTime.size() - 1).get(1),
				priceOverTime.get(priceOverTime.size() - 1).get(2));
		record.setEndDate(endDate);
		return record;
	}

	/**
	 * Currently, we have 3 lists of price data where:
	 * 		list of all price data for coin1 is in list[0]
	 * 		list of all price data for coin2 is in list[1]
	 * 		list of all price data for commission coin is in list[2]
	 *
	 * What we want is a list with each coin at a specific time so that:
	 * 		result[0] = list[0][0]
	 * 		result[1] = list[1][0]
	 * 		result[2] = list[2][0]
	 * 	This enables us to "add" price data like it's happening over time
	 *
	 * @param list - the three lists of price data
	 * @return the lists of price data for each coin over time
	 */
	private List<List<PriceData>> distributeDataByDate(List<List<PriceData>> list) {
		List<PriceData> coin1 = list.get(0);
		List<PriceData> coin2 = list.get(1);
		List<PriceData> coinCommission = list.get(2);

		List<List<PriceData>> finalList = new ArrayList<>();
		for (int i=0; i<coin1.size(); i++) {
			PriceData coin1Pd = coin1.get(i);
			PriceData coin2Pd = coin2.get(i);
			PriceData commissionPd = coinCommission.get(i);
			ArrayList<PriceData> priceData = new ArrayList<>();
			priceData.add(coin1Pd);
			priceData.add(coin2Pd);
			priceData.add(commissionPd);
			finalList.add(priceData);
		}
		return finalList;
	}

	private List<List<PriceData>> cleanUpData(List<List<PriceData>> rawData) {
		// If the list sizes are the same && the update time for the first and last update time are the same,
		// it's probably matched - should be anyway
		List<PriceData> coin1List = rawData.get(0);
		List<PriceData> coin2List = rawData.get(1);
		if (coin1List.size() == coin2List.size() &&
				coin1List.get(0).getUpdateTime().getTime() == coin2List.get(0).getUpdateTime().getTime() &&
				coin1List.get(coin1List.size() - 1).getUpdateTime().getTime() ==
						coin1List.get(coin2List.size() - 1).getUpdateTime().getTime()
				) {
			return rawData;
		}
		clipBeginnings(coin1List, coin2List);
		clipBeginnings(coin2List, coin1List);
		clipEndings(coin1List, coin2List);
		clipEndings(coin2List, coin1List);

		// If the don't match dates, give up
		if (coin2List.isEmpty() || coin1List.isEmpty()) {
			coin1List.clear();
			coin2List.clear();
		}
		if (coin1List.get(coin1List.size() - 1).getUpdateTime().getTime() !=
				coin2List.get(coin2List.size() - 1).getUpdateTime().getTime()) {
			rawData.get(0).clear();
			rawData.get(1).clear();
			return rawData;
		}
		return rawData;
	}

	private  void clipEndings(List<PriceData> list1, List<PriceData> list2) {
		while (list1.size() > list2.size()) {
			list1.remove(list2.size() - 1);
		}

	}

	private void clipBeginnings(List<PriceData> list1, List<PriceData> list2) {
		if (list1 == null || list1.isEmpty() || list2 == null || list2.isEmpty()) {
			if (list1 != null) {
				list1.clear();
			}
			if (list2 != null) {
				list2.clear();
			}
			return;
		}
		if (list1.get(0).getUpdateTime().getTime() < list2.get(0).getUpdateTime().getTime()) {
			while (list1.get(0).getUpdateTime().getTime() < list2.get(0).getUpdateTime().getTime() &&
					!list1.isEmpty()) {
				list1.remove(0);
			}
			if (list1.isEmpty()) {
				list2.clear();
			}
		}

	}

	/**
	 * Load historical price data from an exchange (currently using Crypto Compare in 1 hour intervals)
	 *
	 * @param coin1 - coin1 to use
	 * @param coin2 - coin2 to use
	 * @param baseCoin - base coin to use
	 * @param commissionCoin - commission coin to use
	 * @param numPulled - number of records to pull
	 * @param exchange - the exchange to pull the price data from
	 * @return a list of three lists, each with the price data of each coin
	 */
	public List<List<PriceData>> loadPriceData(String coin1, String coin2, String baseCoin,
												String commissionCoin, int numPulled, ScratchConstants.Exchange exchange) {
		List<List<PriceData>> pd = new ArrayList<>();
		List<PriceData> commissionHistoryPd;
		List<PriceData> coin1HistoryPd = new ArrayList<>();
		List<PriceData> coin2HistoryPd = new ArrayList<>();
		boolean needToFillInArray1 = false;
		if (coin1.equalsIgnoreCase(baseCoin)) {
			needToFillInArray1 = true;
		} else {
			coin1HistoryPd = historicalPriceCache.getHistoricalData(coin1, baseCoin, numPulled);
		}
		boolean needToFillInArray2 = false;
		if (coin2.equalsIgnoreCase(baseCoin)) {
			needToFillInArray2 = true;
		} else {
			coin2HistoryPd = historicalPriceCache.getHistoricalData(coin2, baseCoin, numPulled);
			if (needToFillInArray1) {
				coin1HistoryPd = generateEmptyList(coin2HistoryPd, exchange, baseCoin);
			}
			pd.add(coin1HistoryPd);
		}
		if (needToFillInArray2) {
			coin2HistoryPd = generateEmptyList(coin2HistoryPd, exchange, baseCoin);
		}
		if (commissionCoin.equalsIgnoreCase(baseCoin)) {
			commissionHistoryPd = generateEmptyList(coin2HistoryPd, exchange, baseCoin);
		} else {
			commissionHistoryPd = historicalPriceCache.getHistoricalData(commissionCoin, baseCoin, numPulled);
		}
		pd.add(coin2HistoryPd);
		pd.add(commissionHistoryPd);
		return pd;
	}

	/**
	 * If we use a coin that is the same as the base coin. For example, if our coins are NEO, BTC and a base
	 * coin of BTC, we always know what the price of Bitcoin is with itself ... 1
	 *
	 * @param historicalData - the price data of another coin (so we can make sure the dates match up)
	 * @param exchange - the exchange
	 * @param baseCoin - the base coin
	 * @return a list of price data that always has a price of 1
	 */
	private List<PriceData> generateEmptyList(List<PriceData> historicalData, ScratchConstants.Exchange exchange, String baseCoin) {
		return historicalData.stream().map(c ->
				new PriceData().setExchange(exchange).setPrice(1.0)
						.setTicker(baseCoin + baseCoin).setUpdateTime(c.getUpdateTime()))
				.collect(Collectors.toList());
	}

	/**
	 * Convert the data that is returned from the historical data site and convert it into price data. The price
	 * data should be the base coin + the base coin as the price pair
	 *
	 * @param data - the data list
	 * @param coin - the coin
	 * @param baseCoin - the base coin
	 * @return the price data
	 */
	private List<PriceData> convertToPriceData(List<Data> data, String coin, String baseCoin) {
		return data.stream().map(c ->
				new PriceData().setExchange(ScratchConstants.Exchange.BINANCE).
						setPrice(c.getOpen()).setTicker(coin + baseCoin).setUpdateTime(c.getTime()))
				.collect(Collectors.toList());
	}

	public void saveSimulation(SwapDescriptor swapDescriptor, List<SnapshotDescriptor> snapshots, long checkUpInterval,
							   double seedMoney, long startTime, long endTime, List<TradeAction> tradeActions) {
		SimulationRun run = new SimulationRun().setBaseCoin(swapDescriptor.getBaseCoin()).setCoin1(swapDescriptor.getCoin1())
				.setCoin2(swapDescriptor.getCoin2()).setCommissionCoin(swapDescriptor.getCommissionCoin())
				.setStdDev(swapDescriptor.getDesiredStdDev())
				.setSnapshotInterval(checkUpInterval).setStartAmount(seedMoney)
				.setSimulationStartTime(new Date(startTime)).setSimulationEndTime(new Date(endTime));
		if (snapshots != null && !snapshots.isEmpty()) {
			run.setStartDate(snapshots.get(0).getSnapshotDate())
					.setEndDate(snapshots.get(snapshots.size() - 1).getSnapshotDate())
					.setEndAmount(snapshots.get(snapshots.size() - 1).getTotalValue());
		}
		RaterResults rater = new RaterResults(snapshots);
		//run.setMeanChange(rater.getMean());
		//run.setStdDev(swapDescriptor.getDesiredStdDev());
		//run.setStdDevChange(rater.getStdDev());
		try {
			run = simulationRunRepository.save(run);
		} catch (Exception ex) {
			logger.error("Standard Deviation: " + run.getStdDev() + ", end amount: " + run.getEndAmount() +
					", mean change: " + run.getMeanChange() + ", Standard Deviation Change: " + run.getStdDevChange(), ex);
			return;
		}
		long runId = run.getId();
		for (TradeAction action : tradeActions) {
			action.setSimulationId(runId);
		}
		tradeActionRepository.saveAll(tradeActions);
	}
}
