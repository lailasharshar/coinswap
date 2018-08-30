package com.sharshar.coinswap.services;

import com.sharshar.coinswap.beans.Ticker;
import com.sharshar.coinswap.beans.simulation.*;
import com.sharshar.coinswap.exchanges.AccountService;
import com.sharshar.coinswap.repositories.SimulationRunRepository;
import com.sharshar.coinswap.repositories.TickerRepository;
import com.sharshar.coinswap.utils.AccountServiceFinder;
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
	private SimulationRunRepository simulationRunRepository;

	@Autowired
	private HistoricalAnalysisService historicalAnalysisService;

	@Autowired
	private AccountServiceFinder accountServiceFinder;

	@Autowired
	private SwapService swapService;

	@Autowired
	private TickerRepository tickerRepository;

	@Autowired
	private MessageService messageService;

	private List<Ticker> tickerList;

	@Value("#{'${baseCurrencies}'.split(',')}")
	private List<String> baseCurrencies;

	@Value("${defaultBaseCurrency}")
	private String defaultBaseCurrency;

	@Value("${commissionAsset}")
	private String commissionAsset;

	@Scheduled(cron = "0 5 * * * *")
	public void loadTickers() {
		reconcileDbListWithExchangeList(ScratchConstants.Exchange.BINANCE);
		swapService.updateVolume();
	}

	public List<Ticker> loadTickerListFromDb() {
		if (this.tickerList == null) {
			this.tickerList = new ArrayList<>();
			Iterable<Ticker> tickerIterable = tickerRepository.findAll();
			tickerIterable.forEach(this.tickerList::add);
		}
		return tickerList;
	}

	public List<Ticker> getTickersFromExchange(ScratchConstants.Exchange exchange) {
		AccountService accountService = accountServiceFinder.getAccountService(exchange);
		// Retrieve the list from exchange
		logger.info("Loading " + exchange.getExchangeName() + " Tickers");
		List<Ticker> tickers = new ArrayList<>();
		try {
			tickers = accountService.getTickerDefinitions();
		} catch (Exception ex) {
			logger.error("Unable to reconcileDbListWithExchangeList " + exchange.getExchangeName() + " data", ex);
			return new ArrayList<>();
		}
		return tickers;
	}

	private void reconcileDbListWithExchangeList(ScratchConstants.Exchange exchange) {
		List<Ticker> tickersFromExchange = getTickersFromExchange(exchange);
		logger.info(tickersFromExchange.size() + " Tickers from Exchange");
		List<Ticker> tickersFromDb = loadTickerListFromDb();
		logger.info(tickersFromDb.size() + " Tickers from Db");

		List<Ticker> addedTickers = new ArrayList<>();

		// If they already exist, update any data about them, otherwise insert them
		Date now = new Date();
		for (Ticker ticker : tickersFromExchange) {
			Ticker found = getInList(ticker.getAsset(), ticker.getBase(), ticker.getExchange(), tickersFromDb);
			if (found == null) {
				addedTickers.add(ticker);
				ticker.setFoundDate(now);
			} else {
				// tie the original db item to the data so we can update any data if necessary
				ticker.setTableId(found.getTableId());
				// Probably already null, but if we found it in the db and it was returned, shouldn't be set
				ticker.setRetired(null);
			}
			ticker.setUpdatedDate(now);
		}
		tickerRepository.saveAll(tickersFromExchange);

		// Find any that weren't in the list and retire them
		List<Ticker> retiredTickers = new ArrayList<>();

		// Figure out which ones that need to be retired
		for (Ticker dbTicker : tickersFromDb) {
			Ticker found = getInList(dbTicker.getAsset(), dbTicker.getBase(), dbTicker.getExchange(), tickersFromExchange);
			// We didn't find it in the new list, and it hasn't already been retired, retire it
			if (found == null && dbTicker.getRetired() == null) {
				retiredTickers.add(dbTicker);
				dbTicker.setRetired(now);
				tickerRepository.save(dbTicker);
			}
		}
		// Notify any changes
		messageService.notifyChanges(addedTickers, retiredTickers);
	}

	/*
	These methods remove some of the searching from other methods
	 */
	public Ticker getInList(String asset, String base, short exchange, List<Ticker> tickers) {
		if (tickers == null || tickers.isEmpty() || asset == null || base == null || exchange <= 0) {
			return null;
		}
		return tickers.stream().filter(c -> c.getBase() != null && c.getBase().equalsIgnoreCase(base))
				.filter(c -> c.getAsset() != null && c.getAsset().equalsIgnoreCase(asset))
				.filter(c -> c.getExchange() == exchange).findFirst().orElse(null);
	}

	public List<Ticker> getTickers() {
		if (tickerList == null) {
			loadTickers();
		}
		return tickerList;
	}
}
