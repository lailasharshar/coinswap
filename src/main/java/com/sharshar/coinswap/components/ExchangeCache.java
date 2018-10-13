package com.sharshar.coinswap.components;

import com.sharshar.coinswap.beans.PriceData;
import com.sharshar.coinswap.beans.SwapDescriptor;
import com.sharshar.coinswap.beans.Ticker;
import com.sharshar.coinswap.utils.AnalysisUtils;
import com.sharshar.coinswap.utils.CoinUtils;
import com.sharshar.coinswap.utils.LimitedArrayList;
import com.sharshar.coinswap.utils.ScratchConstants;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.*;

import static com.sharshar.coinswap.components.ExchangeCache.Position.*;

/**
 * Used to cache the list of tickers so we don't have to continually go to the database
 * <p>
 * Created by lsharshar on 3/19/2018.
 */

@Component
@Scope("prototype")
public class ExchangeCache {
	private Logger logger = LogManager.getLogger();

	public enum Position {
		INVALID_DATA,
		INSUFFICIENT_DATA,
		WITHIN_DESIRED_RATIO,
		ABOVE_DESIRED_RATIO,
		BELOW_DESIRED_RATIO
	}

	private SwapDescriptor swapDescriptor;

	private ScratchConstants.Exchange exchange;
	private Ticker ticker1;
	private Ticker ticker2;
	private Ticker commissionTicker;
	private double lastStandardDeviation;
	private double lastMeanRatio;
	private double lowRatio;
	private double highRatio;
	private int cacheSize;
	private Iterable<Ticker> allTickers;

	private Map<Ticker, List<PriceData>> priceCache;

	private Ticker loadTicker(String coin, String baseCoin, short exchangeVal) {
		if (coin.equalsIgnoreCase(baseCoin)) {
			return new Ticker().setUpdatedDate(new Date()).setFoundDate(new Date()).setAsset(coin).setBase(baseCoin)
			.setExchange(exchangeVal).setMinQty(0.0).setMaxQty(0.0).setStepSize(0.0);
		}
		if (allTickers == null) {
			return null;
		}

		for (Ticker ticker : allTickers) {
			if (ticker.getExchange() == exchangeVal && ticker.getRetired() == null
					&& ticker.getAsset().equalsIgnoreCase(coin) && ticker.getBase().equalsIgnoreCase(baseCoin)) {
				return ticker;
			}
		}
		return null;
	}

	public ExchangeCache(SwapDescriptor swapDescriptor, int cacheSize, Iterable<Ticker> allTickers) {
		// Init variables
		this.swapDescriptor = swapDescriptor;
		this.exchange = swapDescriptor.getExchangeObj();
		this.cacheSize = cacheSize;
		this.allTickers = allTickers;
		if (this.cacheSize == 0) {
			this.cacheSize = 100;
		}

		// Map them to real ticker information and create an array for the data for each
		this.ticker1 = loadTicker(swapDescriptor.getCoin1(), swapDescriptor.getBaseCoin(), exchange.getValue());
		this.ticker2 = loadTicker(swapDescriptor.getCoin2(), swapDescriptor.getBaseCoin(), exchange.getValue());
		this.commissionTicker = loadTicker(swapDescriptor.getCommissionCoin(), swapDescriptor.getBaseCoin(), exchange.getValue());

		priceCache = new HashMap<>();
		priceCache.put(ticker1, new LimitedArrayList<>(this.cacheSize));
		priceCache.put(ticker2, new LimitedArrayList<>(this.cacheSize));
		priceCache.put(commissionTicker, new LimitedArrayList<>(this.cacheSize));
	}

	public int getAmountCachePopulated() {
		if (priceCache == null || priceCache.get(ticker1) == null) {
			return 0;
		}
		return priceCache.get(ticker1).size();
	}

	public Position addPriceData(List<PriceData> priceData) {
		if (priceData == null) {
			logger.error("Invalid price data added to cache");
			return INVALID_DATA;
		}
		PriceData currentPd1 = CoinUtils.getPriceData(ticker1.getAssetAndBase(), priceData);
		PriceData currentPd2 = CoinUtils.getPriceData(ticker2.getAssetAndBase(), priceData);
		PriceData commissionPd = CoinUtils.getPriceData(commissionTicker.getAssetAndBase(), priceData);
		if (currentPd1 == null || currentPd2 == null || commissionTicker == null) {
			logger.error("Could not find price data for one of the coins");
			return INVALID_DATA;
		}
		priceCache.get(ticker1).add(currentPd1);
		priceCache.get(ticker2).add(currentPd2);
		priceCache.get(commissionTicker).add(commissionPd);

		if (priceCache.get(ticker1).size() < cacheSize) {
			logger.debug("Insufficient price data to swap");
			return INSUFFICIENT_DATA;
		}
		return updateStats(currentPd1, currentPd2, swapDescriptor.getDesiredStdDev());
	}

	public void bulkLoadData(String ticker, List<PriceData> pd) {
		if (pd == null) {
			logger.error("Invalid price data added to cache");
			return;
		}
		Ticker t = null;
		try {
			t = getTickerObjFromString(ticker);
		} catch (Exception ex) {
			logger.error("Can't find ticker from string: " + ticker + ". It may be a typo or the ticker could have been retired");
			return;
		}
		List<PriceData> tickerPd = priceCache.get(t);
		if (tickerPd == null) {
			logger.error("Invalid ticker: " + ticker + " passed to method. Must be " + ticker1.getAssetAndBase() +
					" or " + ticker2.getAssetAndBase());
			return;
		}
		// Keep add, not add all since that's the method that checks to see if it's over the size limit
		for (PriceData p : pd) {
			tickerPd.add(p);
		}
	}

	private Ticker getTickerObjFromString(String ticker) {
		if (ticker.equalsIgnoreCase(ticker1.getAssetAndBase())) {
			return ticker1;
		}
		if (ticker.equalsIgnoreCase(ticker2.getAssetAndBase())) {
			return ticker2;
		}
		if (ticker.equalsIgnoreCase(commissionTicker.getAssetAndBase())) {
			return commissionTicker;
		}
		return null;
	}

	public Position updateStats(PriceData currentPd1, PriceData currentPd2, double desiredStdDeviation) {
		double stddev = desiredStdDeviation;
		if (stddev < 0.0000001) {
			stddev = 1.0;
		}
		if (this.priceCache.get(ticker1).size() < this.cacheSize || this.priceCache.get(ticker2).size() < this.cacheSize) {
			return INSUFFICIENT_DATA;
		}
		List<PriceData> pd1List = this.priceCache.get(ticker1);
		List<PriceData> pd2List = this.priceCache.get(ticker2);
		double currentRatio = currentPd1.getPrice() / currentPd2.getPrice();
		List<Double> ratioList = AnalysisUtils.getRatioList(pd1List, pd2List);
		this.lastMeanRatio = AnalysisUtils.getMean(ratioList);
		this.lastStandardDeviation = AnalysisUtils.getStdDev(ratioList, this.lastMeanRatio);
		double valueDistance = stddev * this.lastStandardDeviation;
		logger.debug("Mean ratio: " + lastMeanRatio + ", Std Dev: " + this.lastStandardDeviation);
		this.lowRatio = this.lastMeanRatio - valueDistance;
		if (this.lowRatio < 0) {
			this.lowRatio = 0;
		}
		this.highRatio = this.lastMeanRatio + valueDistance;
		logger.debug("Mean ratio: " + this.lastMeanRatio + ", Std Dev: " + this.lastStandardDeviation
				+ ", low value: " + this.lowRatio + ", high value: " + this.highRatio);
		Position position = WITHIN_DESIRED_RATIO;
		if (currentRatio < this.lowRatio && this.lowRatio > 0) {
			return BELOW_DESIRED_RATIO;
		}
		if (currentRatio > this.highRatio && this.highRatio > 0) {
			return ABOVE_DESIRED_RATIO;
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
		priceCache.get(commissionTicker).clear();
	}

	public List<PriceData> getTicker1Data() {
		return priceCache.get(ticker1);
	}

	public List<PriceData> getTicker2Data() {
		return priceCache.get(ticker2);
	}

	public List<PriceData> getCommissionData() {
		return priceCache.get(commissionTicker);
	}

	public int getCacheSize() {
		return cacheSize;
	}

	public ExchangeCache setCacheSize(int cacheSize) {
		this.cacheSize = cacheSize;
		changeOutList(ticker1, cacheSize);
		changeOutList(ticker2, cacheSize);
		changeOutList(commissionTicker, cacheSize);
		return this;
	}

	private void changeOutList(Ticker ticker, int newSize) {
		List<PriceData> oldData = priceCache.get(ticker);
		List<PriceData> newData = new LimitedArrayList<>(newSize);
		// If we are downsizing, remove older data
		if (newSize < oldData.size()) {
			int numToRemove = oldData.size() - newSize;
			for (int i = 0; i < numToRemove; i++) {
				oldData.remove(0);
			}
		}
		// Add all the old data to the new list
		newData.addAll(oldData);
		priceCache.put(ticker, newData);
	}

	public ScratchConstants.Exchange getExchange() {
		return exchange;
	}

	public Ticker getTicker1() {
		return ticker1;
	}

	public Ticker getTicker2() {
		return ticker2;
	}

	public Ticker getCommissionTicker() {
		return commissionTicker;
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
		Ticker t = null;
		try {
			t = getTickerObjFromString(ticker);
		} catch (Exception ex) {
			logger.error("Can't find ticker from string: " + ticker + ". It may be a typo or the ticker could have been retired");
			return null;
		}
		List<PriceData> pd = priceCache.get(t);
		if (pd == null || pd.isEmpty()) {
			return null;
		}
		return Collections.max(pd, Comparator.comparing(PriceData::getUpdateTime));
	}
}
