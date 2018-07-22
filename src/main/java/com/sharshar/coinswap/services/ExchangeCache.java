package com.sharshar.coinswap.services;

import com.sharshar.coinswap.beans.PriceData;
import com.sharshar.coinswap.components.SwapComponent;
import com.sharshar.coinswap.utils.AnalysisUtils;
import com.sharshar.coinswap.utils.CoinUtils;
import com.sharshar.coinswap.utils.LimitedArrayList;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

import static com.sharshar.coinswap.services.ExchangeCache.Position.*;

/**
 * Used to cache the list of tickers so we don't have to continually go to the database
 *
 * Created by lsharshar on 3/19/2018.
 */

@Service
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

	@Value("${cacheSize}")
	private int cacheSize;

	@Autowired
	TradeService tradeService;

	SwapComponent owningComponent;

	private short exchange;
	private String ticker1;
	private String ticker2;
	private double desiredStdDeviation;
	private double lastStandardDeviation;
	private double lastMeanRatio;
	private double lowRatio;
	private double highRatio;

	private Map<String, List<PriceData>> priceCache;

	public ExchangeCache(short exchange, String ticker1, String ticker2, double desiredStdDev) {
		this.ticker1 = ticker1;
		this.ticker2 = ticker2;
		this.desiredStdDeviation = desiredStdDev;
		this.exchange = exchange;
		if (cacheSize == 0) {
			cacheSize = 100;
		}
		priceCache = new HashMap<>();
		priceCache.put(ticker1, new LimitedArrayList<>(cacheSize));
		priceCache.put(ticker2, new LimitedArrayList<>(cacheSize));
	}

	public void addPriceData(List<PriceData> priceData) {
		if (priceData == null) {
			logger.error("Invalid price data added to cache");
			return;
		}
		PriceData currentPd1 = CoinUtils.getPriceData(ticker1, priceData);
		PriceData currentPd2 = CoinUtils.getPriceData(ticker2, priceData);
		List<PriceData> pd1List = priceCache.get(ticker1);
		List<PriceData> pd2List = priceCache.get(ticker2);
		pd1List.add(currentPd1);
		pd2List.add(currentPd2);
		if (pd1List.size() < cacheSize) {
			logger.info("Insufficient price data to swap");
			return;
		}
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
		if (highRatio > 1) {
			highRatio = 1;
		}
		logger.debug("Mean ratio: " + lastMeanRatio + ", Std Dev: " + lastStandardDeviation
			+ ", low value: " + lowRatio + ", high value: " + highRatio);
		Position position = WithinDesiredRatio;
		if (currentRatio < lowRatio && lowRatio > 0) {
			position = BelowDesiredRatio;
		}
		if (currentRatio > highRatio && highRatio > 0) {
			position = AboveDesiredRatio;
		}
		swap(position, priceData, owningComponent.isActive());
	}

	public void swap(Position position, List<PriceData> priceData, boolean simulate) {
		if (owningComponent.getCurrentSwapState() == SwapComponent.CurrentSwapState.ownsCoin2 &&
				position == Position.AboveDesiredRatio) {
			owningComponent.swapCoin2ToCoin1(priceData, simulate);
		}
		if (owningComponent.getCurrentSwapState() == SwapComponent.CurrentSwapState.ownsCoin1 &&
				position == Position.BelowDesiredRatio) {
			owningComponent.swapCoin1ToCoin2(priceData, simulate);
		}
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

	public ExchangeCache setExchange(short exchange) {
		this.exchange = exchange;
		return this;
	}

	public String getTicker1() {
		return ticker1;
	}

	public ExchangeCache setTicker1(String ticker1) {
		this.ticker1 = ticker1;
		return this;
	}

	public String getTicker2() {
		return ticker2;
	}

	public ExchangeCache setTicker2(String ticker2) {
		this.ticker2 = ticker2;
		return this;
	}

	public double getLastStandardDeviation() {
		return lastStandardDeviation;
	}

	public ExchangeCache setLastStandardDeviation(double lastStandardDeviation) {
		this.lastStandardDeviation = lastStandardDeviation;
		return this;
	}

	public double getLastMeanRatio() {
		return lastMeanRatio;
	}

	public ExchangeCache setLastMeanRatio(double lastMeanRatio) {
		this.lastMeanRatio = lastMeanRatio;
		return this;
	}

	public double getDesiredStdDeviation() {
		return desiredStdDeviation;
	}

	public ExchangeCache setDesiredStdDeviation(double desiredStdDeviation) {
		this.desiredStdDeviation = desiredStdDeviation;
		return this;
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

	public ExchangeCache setHighRatio(double highRatio) {
		this.highRatio = highRatio;
		return this;
	}

	public SwapComponent getOwningComponent() {
		return owningComponent;
	}

	public ExchangeCache setOwningComponent(SwapComponent owningComponent) {
		this.owningComponent = owningComponent;
		return this;
	}
}
