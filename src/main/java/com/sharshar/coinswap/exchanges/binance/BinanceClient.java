package com.sharshar.coinswap.exchanges.binance;

import com.binance.api.client.BinanceApiClientFactory;
import com.binance.api.client.BinanceApiRestClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Created by lsharshar on 1/15/2018.
 */
@Configuration
public class BinanceClient {

	@Value("${binance.apiKey}")
	private String binanceApiKey;

	@Value("${binance.apiSecretKey}")
	private String binanceSecretKey;

	@Bean
	public BinanceApiRestClient binanceApiRestClient() {
		BinanceApiClientFactory factory = BinanceApiClientFactory.newInstance(
				binanceApiKey, binanceSecretKey);
		BinanceApiRestClient client = factory.newRestClient();
		return client;
	}
}
