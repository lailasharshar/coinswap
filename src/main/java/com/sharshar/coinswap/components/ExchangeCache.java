package com.sharshar.coinswap.components;

import com.sharshar.coinswap.beans.PriceData;
import com.sharshar.coinswap.beans.SwapDescriptor;
import com.sharshar.coinswap.utils.AnalysisUtils;
import com.sharshar.coinswap.utils.CoinUtils;
import com.sharshar.coinswap.utils.LimitedArrayList;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.util.*;

import static com.sharshar.coinswap.components.ExchangeCache.Position.*;

/**
 * Used to cache the list of tickers so we don't have to continually go to the database
 *
 * Created by lsharshar on 3/19/2018.
 */

@Component
@Scope("prototype")
public class ExchangeCache {
	Logger logger = LogManager.getLogger();

	enum Position {
		InvalidData,
		InsufficientData,
		WithinDesiredRatio,
		AboveDesiredRatio,
		BelowDesiredRatio
	}

	SwapDescriptor swapDescriptor;

	private short exchange;
	private String ticker1;
	private String ticker2;
	private double lastStandardDeviation;
	private double lastMeanRatio;
	private double lowRatio;
	private double highRatio;
	private int cacheSize;

	private Map<String, List<PriceData>> priceCache;

	public ExchangeCache(SwapDescriptor swapDescriptor, int cacheSize, String baseCoin) {
		this.swapDescriptor = swapDescriptor;
		this.ticker1 = swapDescriptor.getCoin1() + baseCoin;
		this.ticker2 = swapDescriptor.getCoin2() + baseCoin;
		this.exchange = swapDescriptor.getExchange();
		this.cacheSize = cacheSize;
		if (this.cacheSize == 0) {
			this.cacheSize = 100;
		}
		priceCache = new HashMap<>();
		priceCache.put(ticker1, new LimitedArrayList<>(this.cacheSize));
		priceCache.put(ticker2, new LimitedArrayList<>(this.cacheSize));
	}

	public Position addPriceData(List<PriceData> priceData) {
		if (priceData == null) {
			logger.error("Invalid price data added to cache");
			return InvalidData;
		}
		PriceData currentPd1 = CoinUtils.getPriceData(ticker1, priceData);
		PriceData currentPd2 = CoinUtils.getPriceData(ticker2, priceData);
		List<PriceData> pd1List = priceCache.get(ticker1);
		List<PriceData> pd2List = priceCache.get(ticker2);
		pd1List.add(currentPd1);
		pd2List.add(currentPd2);
		if (pd1List.size() < cacheSize) {
			logger.info("Insufficient price data to swap");
			return InsufficientData;
		}
		return updateStats(currentPd1, currentPd2, swapDescriptor.getDesiredStdDev());
	}

	private Position updateStats(PriceData currentPd1, PriceData currentPd2, double desiredStdDeviation) {
		if (priceCache.size() < cacheSize) {
			return InsufficientData;
		}
		List<PriceData> pd1List = priceCache.get(ticker1);
		List<PriceData> pd2List = priceCache.get(ticker2);
		double currentRatio = currentPd1.getPrice()/currentPd2.getPrice();
		List<Double> ratioList = AnalysisUtils.getRatioList(pd1List, pd2List);
		lastMeanRatio = AnalysisUtils.getMean(ratioList);
		lastStandardDeviation = AnalysisUtils.getStdDev(ratioList, lastMeanRatio);
		logger.debug("Mean ratio: " + lastMeanRatio + ", Std Dev: " + lastStandardDeviation);
		lowRatio = lastMeanRatio - (desiredStdDeviation * lastStandardDeviation);
		if (lowRatio < 0) {
			lowRatio = 0;
		}
		highRatio = lastMeanRatio + (desiredStdDeviation * lastStandardDeviation);
		logger.debug("Mean ratio: " + lastMeanRatio + ", Std Dev: " + lastStandardDeviation
				+ ", low value: " + lowRatio + ", high value: " + highRatio);
		Position position = WithinDesiredRatio;
		if (currentRatio < lowRatio && lowRatio > 0) {
			return BelowDesiredRatio;
		}
		if (currentRatio > highRatio && highRatio > 0) {
			return AboveDesiredRatio;
		}
		return position;
	}

	public Date getLatestUpdate() {
		return priceCache.get(ticker1).stream()
				.map(PriceData::getUpdateTime).max(Date::compareTo).orElse(null);
	}

	public void clear() {
		priceCache.get(ticker1).clear();
		priceCache.get(ticker2).clear();
	}

	public List<PriceData> getTicker1Data() {
		return priceCache.get(ticker1);
	}

	public List<PriceData> getTicker2Data() {
		return priceCache.get(ticker2);
	}

	public int getCacheSize() {
		return cacheSize;
	}

	public ExchangeCache setCacheSize(int cacheSize) {
		this.cacheSize = cacheSize;
		changeOutList(ticker1, cacheSize);
		changeOutList(ticker2, cacheSize);
		return this;
	}

	private void changeOutList(String ticker, int newSize) {
		List<PriceData> oldData = priceCache.get(ticker);
		List<PriceData> newData = new LimitedArrayList<>(newSize);
		// If we are downsizing, remove older data
		if (newSize < oldData.size()) {
			int numToRemove = oldData.size() - newSize;
			for (int i=0; i<numToRemove; i++) {
				oldData.remove(0);
			}
		}
		// Add all the old data to the new list
		newData.addAll(oldData);
		priceCache.put(ticker, newData);
	}

	public short getExchange() {
		return exchange;
	}

	public String getTicker1() {
		return ticker1;
	}

	public String getTicker2() {
		return ticker2;
	}

	public double getLastStandardDeviation() {
		return lastStandardDeviation;
	}

	public double getLastMeanRatio() {
		return lastMeanRatio;
	}

	public double getLowRatio() {
		return lowRatio;
	}

	public ExchangeCache setLowRatio(double lowRatio) {
		this.lowRatio = lowRatio;
		return this;
	}

	public double getHighRatio() {
		return highRatio;
	}

	public PriceData getLastPriceData(String ticker) {
		return Collections.max(priceCache.get(ticker), Comparator.comparing(PriceData::getUpdateTime));
	}
}
