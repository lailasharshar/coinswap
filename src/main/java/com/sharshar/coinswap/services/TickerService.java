package com.sharshar.coinswap.services;

import com.sharshar.coinswap.beans.PriceData;
import com.sharshar.coinswap.beans.Ticker;
import com.sharshar.coinswap.exchanges.binance.BinanceAccountServices;
import com.sharshar.coinswap.repositories.TickerRepository;
import com.sharshar.coinswap.utils.ScratchConstants;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Used to manage the list of active currencies on different exchanges. Periodically polls exchanges for all
 * supported coin/base pairs
 *
 * Created by lsharshar on 7/16/2018.
 */
@Service
public class TickerService {
	private Logger logger = LogManager.getLogger();

	@Autowired
	private BinanceAccountServices binanceAccountServices;

	@Autowired
	private TickerRepository tickerRepository;

	@Autowired
	private NotificationService notificationService;

	private List<Ticker> tickerList;

	@Value("#{'${baseCurrencies}'.split(',')}")
	private List<String> baseCurrencies;

	@Value("${defaultBaseCurrency}")
	private String defaultBaseCurrency;

	@Scheduled(fixedRate = 60000)
	public void loadTickers() {
		loadBinance();
	}

	public List<Ticker> getTickerList() {
		if (this.tickerList == null) {
			this.tickerList = new ArrayList<>();
			Iterable<Ticker> tickerIterable = tickerRepository.findAll();
			tickerIterable.forEach(this.tickerList::add);
		}
		return tickerList;
	}

	public void addTicker(String tickerWithBase, short exchange) {
		List<Ticker> tickers = getTickerList();
		Ticker ticker = new Ticker(tickerWithBase, exchange, baseCurrencies);
		List<Ticker> foundTickers = getInList(ticker);
		if (foundTickers == null || foundTickers.isEmpty()) {
			tickers.add(tickerRepository.save(ticker));
		}
	}

	public List<Ticker> retireTicker(String tickerWithBase, short exchange) {
		if (tickerWithBase == null || tickerWithBase.isEmpty() || exchange < 1) {
			return new ArrayList<>();
		}
		List<Ticker> tickers = getTickerList();
		Ticker ticker = new Ticker(tickerWithBase, exchange, baseCurrencies);
		List<Ticker> foundTickers = getInList(ticker);
		if (foundTickers != null && !foundTickers.isEmpty()) {
			Date now = new Date();
			for (Ticker ft : foundTickers) {
				ft.setRetired(now);
			}
			tickerRepository.saveAll(foundTickers);
		}
		return foundTickers;
	}

	public void loadBinance() {
		logger.info("Loading Binance Tickers");
		List<PriceData> priceData = binanceAccountServices.getAllPrices();
		logger.info(priceData.size() + " Tickers");
		System.out.println(priceData.stream().map(PriceData::getTicker).collect(Collectors.toList()));
		List<Ticker> tickers = getTickerList();
		List<String> addedTickers = new ArrayList<>();
		List<String> retiredTickers = new ArrayList<>();
		// Figure out which ones to add
		for (PriceData pd : priceData) {
			List<Ticker> found = getInList(pd);
			if (found == null || found.isEmpty()) {
				addTicker(pd.getTicker(), pd.getExchange());
				addedTickers.add(pd.getTicker() + " - " + ScratchConstants.EXCHANGES[pd.getExchange()]);
			}
		}
		// Figure out which ones that need to be retired
		List<Ticker> itemsToRemove = new ArrayList<>();
		for (Ticker ticker : tickers) {
			List<PriceData> found = getInList(priceData, ticker);
			if ((found == null || found.isEmpty()) && ticker.getRetired() == null) {
				itemsToRemove.addAll(retireTicker(ticker.getTicker() + ticker.getBase(), ticker.getExchange()));
				retiredTickers.add(ticker.getTicker() + ticker.getBase() + " - " + ScratchConstants.EXCHANGES[ticker.getExchange()]);
			}
		}
		tickers.removeAll(itemsToRemove);
		// Notify any changes
		notifyChanges(addedTickers, retiredTickers);
	}

	/*
	These methods remove some of the searching from other methods
	 */
	private List<PriceData> getInList(List<PriceData> priceData, Ticker ticker) {
		if (priceData == null || priceData.isEmpty()) {
			return new ArrayList<>();
		}
		return priceData.stream()
				.filter(c -> c.getExchange() == ticker.getExchange())
				.filter(c -> c.getTicker().equalsIgnoreCase(ticker.getTicker() + ticker.getBase()))
				.collect(Collectors.toList());
	}

	private List<Ticker> getInList(PriceData priceData) {
		if (priceData == null) {
			return new ArrayList<>();
		}
		Ticker ticker = new Ticker(priceData.getTicker(), priceData.getExchange(), baseCurrencies);
		return getInList(ticker);
	}

	private List<Ticker> getInList(Ticker ticker) {
		List<Ticker> tickers = getTickerList();
		if (tickers == null || tickers.isEmpty() || ticker == null) {
			return new ArrayList<>();
		}
		return tickers.stream().filter(c -> c.getBase() != null && c.getBase().equalsIgnoreCase(ticker.getBase()))
				.filter(c -> c.getTicker() != null && c.getTicker().equalsIgnoreCase(ticker.getTicker()))
				.filter(c -> c.getExchange() == ticker.getExchange())
				.collect(Collectors.toList());
	}

	private void notifyChanges(List<String> addedTickers, List<String> removedTickers) {
		if ((addedTickers == null || addedTickers.isEmpty())
				&& (removedTickers == null || removedTickers.isEmpty())) {
			// Both are empty, do nothing
			return;
		}
		StringBuilder content = new StringBuilder();
		if (addedTickers != null && !addedTickers.isEmpty()) {
			content.append("Added Tickers (" + addedTickers.size() + "): <br><br>");
			logger.info("Added Tickers (" + addedTickers.size() + ")");
			for (String s : addedTickers) {
				content.append(s).append("<br>");
			}
			content.append("<br><br>");
		}
		if (removedTickers != null && !removedTickers.isEmpty()) {
			content.append("Removed Tickers (" + removedTickers.size() + "): <br><br>");
			logger.info("Removed Tickers (" + removedTickers.size() + ")");
			for (String s : removedTickers) {
				content.append(s).append("<br>");
			}
			content.append("<br><br>");
		}
		try {
			StringBuilder subject = new StringBuilder();
			subject.append("Exchange Ticker Changes (")
					.append(addedTickers.size())
					.append(" Added/")
					.append(removedTickers.size())
					.append(" Removed");
			notificationService.notifyMe(subject.toString(), content.toString());
		} catch (Exception ex) {
			logger.error("Unable to alert to new/retired tickers: " + content.toString(), ex);
		}
	}

	public List<String> getBaseCurrencies() {
		return baseCurrencies;
	}

	public TickerService setBaseCurrencies(List<String> baseCurrencies) {
		this.baseCurrencies = baseCurrencies;
		return this;
	}

	public String getDefaultBaseCurrency() {
		return defaultBaseCurrency;
	}

	public TickerService setDefaultBaseCurrency(String defaultBaseCurrency) {
		this.defaultBaseCurrency = defaultBaseCurrency;
		return this;
	}
}
