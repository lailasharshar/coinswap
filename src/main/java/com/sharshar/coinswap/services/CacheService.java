package com.sharshar.coinswap.services;

import com.sharshar.coinswap.beans.SwapDescriptor;
import com.sharshar.coinswap.beans.Ticker;
import com.sharshar.coinswap.components.ExchangeCache;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

/**
 * Creates a price cache and updates values in it
 *
 * Created by lsharshar on 7/21/2018.
 */
@Service
public class CacheService {
	@Autowired
	private ApplicationContext applicationContext;

	@Value("${cacheSize}")
	private int cacheSize;

	/**
	 * Generate a price cache for a coin descriptor
	 *
	 * @param swapDescriptor - the object describing the swap
	 * @param tickers - all tickers so that we can use it's data
	 * @return the price data cache
	 */
	public ExchangeCache createCache(SwapDescriptor swapDescriptor, Iterable<Ticker> tickers) {
		return applicationContext.getBean(ExchangeCache.class, swapDescriptor, cacheSize, tickers);
	}
}
