package com.sharshar.coinswap.exchanges;

import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sharshar.coinswap.exchanges.Data;
import com.sharshar.coinswap.exchanges.HistoryResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by lsharshar on 5/11/2018.
 */
@Service
public class HistoricalDataPull {

	@Value("${historic.dataUrl}")
	private String dataUrl;

	public HistoryResponse getHistory(String coin, String refCurrency, Date endDate, String exchange, int numPulled) {
		RestTemplate restTemplate = new RestTemplate();
		ObjectMapper objectMapper = new ObjectMapper();
		objectMapper.configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true);
		String data = restTemplate.getForObject(dataUrl + "?fsym=" + coin + "&tsym=" + refCurrency +
				"&limit=" + numPulled + "&toTs=" + endDate.getTime()/1000, String.class);
		if (exchange != null && !exchange.isEmpty()) {
			data += "&e=" + exchange;
		}
		System.out.println("Getting historical data from: " + data);
		HistoryResponse response = null;
		try {
			response = objectMapper.readValue(data, HistoryResponse.class);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return response;
	}

	public List<Data> getData(String coin, String refCurrency, int numPulled, String exchange) {
		List<Data> d = new ArrayList<>();
		Date endDate = new Date();
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
