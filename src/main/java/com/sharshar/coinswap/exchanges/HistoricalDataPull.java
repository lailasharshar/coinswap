package com.sharshar.coinswap.exchanges;

import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sharshar.coinswap.exchanges.Data;
import com.sharshar.coinswap.exchanges.HistoryResponse;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Pulls data from a historical data source
 *
 * Created by lsharshar on 5/11/2018.
 */
@Service
public class HistoricalDataPull {

	private Logger logger = LogManager.getLogger();

	@Value("${historic.dataUrl}")
	private String dataUrl;

	@Value("${historic.maxPull}")
	private int maxPull;

	/**
	 * Get historical data
	 *
	 * @param coin - the coin you want (XMR, NEO, ETH, for example)
	 * @param refCurrency - the base currency (BTC, USD, for example)
	 * @param endDate - the last date to search for the data (usually, now)
	 * @param exchange - the exchange to ask the data for. We are not usually querying them directly, but asking
	 *                 a third party like CryptoCompare what Binance's values were during that period.
	 * @param numPulled - the number to pull previous to the end data
	 * @return the historical data response
	 */
	public HistoryResponse getHistory(String coin, String refCurrency, Date endDate, String exchange, int numPulled) {
		RestTemplate restTemplate = new RestTemplate();
		ObjectMapper objectMapper = new ObjectMapper();
		objectMapper.configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true);
		if (numPulled <= 0) {
			numPulled = maxPull;
		}
		String data = restTemplate.getForObject(dataUrl + "?fsym=" + coin + "&tsym=" + refCurrency +
				"&limit=" + numPulled + "&toTs=" + endDate.getTime()/1000, String.class);
		if (exchange != null && !exchange.isEmpty()) {
			data += "&e=" + exchange;
		}
		HistoryResponse response = null;
		try {
			response = objectMapper.readValue(data, HistoryResponse.class);
		} catch (IOException e) {
			logger.error("Unable to get history", e);
		}
		return response;
	}

	/**
	 * Get the history and convert into a more usable form assuming the end date is right now.
	 *
	 * @param coin - the coin you want (XMR, NEO, ETH, for example)
	 * @param refCurrency - the base currency (BTC, USD, for example)
	 * @param exchange - the exchange to ask the data for. We are not usually querying them directly, but asking
	 *                 a third party like CryptoCompare what Binance's values were during that period.

	 * @return the price data
	 */
	public List<Data> getData(String coin, String refCurrency, int numPulled, String exchange) {
		List<Data> d = new ArrayList<>();
		Date endDate = new Date();
		logger.info("Getting historical data for " + coin + refCurrency);
		HistoryResponse response = getHistory(coin, refCurrency, endDate, exchange, numPulled);
		if (response == null || response.getData() == null || response.getData().isEmpty() &&
				!"Success".equalsIgnoreCase(response.getResponse())) {
			return d;
		}
		d.addAll(response.getData());
		return d.stream()
				.filter(c -> c.getOpen() > 0)
				.collect(Collectors.toList());
	}
}
