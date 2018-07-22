package com.sharshar.coinswap.services;

import com.sharshar.coinswap.beans.PriceData;
import com.sharshar.coinswap.exchanges.AccountService;
import com.sharshar.coinswap.utils.AccountServiceFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Created by lsharshar on 7/21/2018.
 */
@Service
public class CacheService {
	private Logger logger = LogManager.getLogger();
	List<ExchangeCache> caches;

	@Autowired
	private ApplicationContext applicationContext;


	@Autowired
	AccountServiceFactory factory;

	public ExchangeCache createCache(String ticker1, String ticker2, short exchange, double desiredStdDev) {
		ExchangeCache cache = applicationContext.getBean(ExchangeCache.class, exchange, ticker1, ticker2, desiredStdDev);
		if (caches == null) {
			caches = new ArrayList<>();
		}
		caches.add(cache);
		return cache;
	}

	public void updateCaches(List<PriceData> priceDataList) {
		for (ExchangeCache cache : caches) {
			cache.addPriceData(priceDataList);
		}
	}

	@Scheduled(fixedRate = 60000)
	public void updatePriceData() {
		// Find the unique exchanges we're pulling data from
		List<Short> allExchanges = caches.stream()
				.map(ExchangeCache::getExchange).distinct().collect(Collectors.toList());

		// For each cache service, find it's account service and save it if is not known
		Map<Short, AccountService> services = new HashMap<>();
		for (ExchangeCache cache : caches) {
			short exchange = cache.getExchange();
			AccountService service = services.get(exchange);
			if (service == null) {
				services.put(exchange, factory.getAccountService(exchange));
			}
		}
		// For each exchange, retrieve the updated price data
		Map<Short, List<PriceData>> updatedData = new HashMap<>();
		for (short exchange : allExchanges) {
			AccountService service = services.get(exchange);
			List<PriceData> pd = service.getAllPrices();
			updatedData.put(exchange, pd);
		}

		// For each cache, update the cache and recalculate the mean/std. deviation
		for (ExchangeCache cache : caches) {
			short exchange = cache.getExchange();
			cache.addPriceData(updatedData.get(exchange));
		}
	}
}
