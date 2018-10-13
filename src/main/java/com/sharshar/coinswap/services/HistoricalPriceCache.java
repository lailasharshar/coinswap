package com.sharshar.coinswap.services;

import com.sharshar.coinswap.beans.PriceData;
import com.sharshar.coinswap.exchanges.binance.BinanceAccountServices;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * If we've pulled the data in the last hour or so, no need to get it again. Cache it and return it
 *
 * Created by lsharshar on 10/8/2018.
 */
@Service
public class HistoricalPriceCache {
	@Autowired
	private BinanceAccountServices services;
	private static final long RETIRE_TIME = 3600000L;
	private Map<String, List<PriceData>> historicalData = new HashMap<>();

	public List<PriceData> getHistoricalData(String coin, String baseCoin, int number) {
		Date now = new Date();
		List<PriceData> pd = historicalData.get(coin + baseCoin);
		if (pd == null || pd.isEmpty()) {
			pd = services.getBackfillData(number, coin, baseCoin);
			historicalData.put(coin + baseCoin, pd);
		} else {
			Date lastDate = pd.stream().map(PriceData::getUpdateTime).max(Date::compareTo).orElse(null);
			if (lastDate == null || (now.getTime() - lastDate.getTime()) > RETIRE_TIME || pd.size() < number) {
				pd = services.getBackfillData(number, coin, baseCoin);
				historicalData.put(coin + baseCoin, pd);
			}
		}
		if (pd.size() > number) {
			List<PriceData> newPriceData = new ArrayList<>();
			newPriceData.addAll(pd);
			while (newPriceData.size() > number) {
				newPriceData.remove(0);
			}
			return newPriceData;
		} else {
			return pd;
		}
	}
}
