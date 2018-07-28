package com.sharshar.coinswap.services;

import com.sharshar.coinswap.beans.PriceData;
import com.sharshar.coinswap.beans.SwapDescriptor;
import com.sharshar.coinswap.components.ExchangeCache;
import com.sharshar.coinswap.utils.AccountServiceFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by lsharshar on 7/21/2018.
 */
@Service
public class CacheService {
	private Logger logger = LogManager.getLogger();
	private List<ExchangeCache> caches;

	@Autowired
	private ApplicationContext applicationContext;


	@Autowired
	AccountServiceFactory factory;

	@Value("${cacheSize}")
	private int cacheSize;


	public CacheService() {
		caches = new ArrayList<>();
	}

	public ExchangeCache createCache(SwapDescriptor swapDescriptor, String baseCoin) {
		ExchangeCache cache = applicationContext.getBean(ExchangeCache.class, swapDescriptor, cacheSize, baseCoin);
		caches.add(cache);
		return cache;
	}

	public void updateCaches(List<PriceData> priceDataList) {
		for (ExchangeCache cache : caches) {
			cache.addPriceData(priceDataList);
		}
	}
}
