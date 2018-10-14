package com.sharshar.coinswap.exchanges.binance;

import com.binance.api.client.BinanceApiRestClient;
import com.binance.api.client.domain.OrderSide;
import com.binance.api.client.domain.OrderStatus;
import com.binance.api.client.domain.OrderType;
import com.binance.api.client.domain.TimeInForce;
import com.binance.api.client.domain.account.*;
import com.binance.api.client.domain.account.request.AllOrdersRequest;
import com.binance.api.client.domain.account.request.OrderStatusRequest;
import com.binance.api.client.domain.general.*;
import com.binance.api.client.domain.market.*;
import com.sharshar.coinswap.beans.OrderHistory;
import com.sharshar.coinswap.beans.OwnedAsset;
import com.sharshar.coinswap.beans.PriceData;
import com.sharshar.coinswap.beans.Ticker;
import com.sharshar.coinswap.exchanges.AccountService;
import com.sharshar.coinswap.exchanges.HistoricalDataPull;
import com.sharshar.coinswap.repositories.OrderHistoryRepository;
import com.sharshar.coinswap.utils.ScratchConstants;
import org.apache.commons.lang3.time.DateUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Used to access and clean up any data from the binance client
 *
 * Created by lsharshar on 5/27/2018.
 */
@Service
public class BinanceAccountServices implements AccountService {
	private Logger logger = LogManager.getLogger();

	@Autowired
	private BinanceApiRestClient binanceApiRestClient;

	@Autowired
	private OrderHistoryRepository orderHistoryRepository;

	@Autowired
	private HistoricalDataPull historicalDataPull;

	@Value("${binance.transactionFee}")
	private double transactionFee;

	@Value("${maxBaseCoinChunks:0.03}")
	private double maxBaseCoinChunks;

	public static final long ONE_HOUR = (1000L * 60 * 60);

	public double getDefaultTransactionFee() {
		return transactionFee;
	}

	public boolean checkUp() {
		Long serverTime = binanceApiRestClient.getServerTime();
		return serverTime != null && serverTime > 0;
	}

	public Account getMyAccount() {
		return binanceApiRestClient.getAccount();
	}

	public List<Trade> getMyTrades(String ticker, String orderId) {
		if (orderId == null) {
			return new ArrayList<>();
		}
		List<Trade> trades = binanceApiRestClient.getMyTrades(ticker);
		return trades.stream().filter(c -> orderId.equalsIgnoreCase(c.getOrderId())).collect(Collectors.toList());
	}

	public ScratchConstants.Exchange getExchange() {
		return ScratchConstants.Exchange.BINANCE;
	}

	public List<Ticker> getTickerDefinitions() {
		List<Ticker> tickers = new ArrayList<>();
		ExchangeInfo info = binanceApiRestClient.getExchangeInfo();
		List<SymbolInfo> symbolInfo = info.getSymbols();
		for (SymbolInfo si : symbolInfo) {
			SymbolStatus status = si.getStatus();
			if (status != SymbolStatus.TRADING) {
				continue;
			}
			Ticker ticker = new Ticker().setAsset(si.getBaseAsset()).setBase(si.getQuoteAsset())
					.setExchange(ScratchConstants.Exchange.BINANCE.getValue());
			SymbolFilter lotFilter = si.getSymbolFilter(FilterType.LOT_SIZE);
			ticker.setMinQty(Double.parseDouble(lotFilter.getMinQty()));
			ticker.setMaxQty(Double.parseDouble(lotFilter.getMaxQty()));
			ticker.setStepSize(Double.parseDouble(lotFilter.getStepSize()));
			tickers.add(ticker);
		}
		return tickers;
	}

	public Double get24HourVolume(String ticker) {
		if (isBaseCoin(ticker)) {
			return 0.0;
		}
		TickerStatistics stats = binanceApiRestClient.get24HrPriceStatistics(ticker);
		return Double.parseDouble(stats.getVolume());
	}

	public static boolean isBaseCoin(String ticker) {
		if (ticker.length() % 2 != 0) {
			return false;
		}
		String one = ticker.substring(0, (ticker.length()/2));
		if ((one + one).equalsIgnoreCase(ticker)) {
			return true;
		}
		return false;
	}

	public List<PriceData> getAllPrices() {
		List<PriceData> priceData = new ArrayList<>();
		try {
			List<TickerPrice> allPrices = binanceApiRestClient.getAllPrices();
			if (allPrices == null || allPrices.isEmpty()) {
				logger.error("Unable to load prices from Binance");
				return priceData;
			}
			Date now = new Date();
			for (TickerPrice tp : allPrices) {
				PriceData pd = new PriceData().setTicker(tp.getSymbol()).setExchange(getExchange()).setUpdateTime(now);
				try {
					double price = Double.parseDouble(tp.getPrice());
					pd.setPrice(price);
				} catch (Exception ex) {
					logger.error("Unable to parse value of : " + tp.getSymbol() + " - " + tp.getPrice());
					pd.setPrice(0.0);
				}
				priceData.add(pd);
			}
		} catch (Exception ex) {
			logger.error("Unable to load price data for Binance", ex);
			return null;
		}
		return priceData;
	}

	public List<OwnedAsset> getAllBalances() {
		List<OwnedAsset> normalizedBalance = new ArrayList<>();
		List<AssetBalance> balances = binanceApiRestClient.getAccount().getBalances();
		if (balances == null) {
			return normalizedBalance;
		}
		for (AssetBalance balance : balances) {
			OwnedAsset asset = new OwnedAsset().setAsset(balance.getAsset());
			if (balance.getFree() == null || balance.getFree().trim().isEmpty()) {
				asset.setFree(0.0);
			} else {
				try {
					double free = Double.parseDouble(balance.getFree());
					asset.setFree(free);
				} catch (Exception ex) {
					logger.error("Unable to parse value of free balance for: " + balance.getAsset() + " - " + balance.getFree());
					asset.setFree(0.0);
				}
			}
			if (balance.getLocked() == null || balance.getLocked().trim().isEmpty()) {
				asset.setLocked(0.0);
			} else {
				try {
					double locked = Double.parseDouble(balance.getLocked());
					asset.setLocked(locked);
				} catch (Exception ex) {
					logger.error("Unable to parse value of locked balance for: " + balance.getAsset() + " - " + balance.getLocked());
					asset.setFree(0.0);
				}
			}
			normalizedBalance.add(asset);
		}
		return normalizedBalance;
	}

	public NewOrderResponse buyMarketTest(String ticker, double amount) {
		NewOrder newOrder = new NewOrder(
				ticker, OrderSide.BUY, OrderType.MARKET, TimeInForce.GTC, "" + amount);
		binanceApiRestClient.newOrderTest(newOrder);
		NewOrderResponse response = new NewOrderResponse();
		response.setClientOrderId("TESTBUY");
		response.setOrderId(12345L);
		response.setSymbol(ticker);
		response.setTransactTime(10L);
		return response;
	}

	public NewOrderResponse sellMarketTest(String ticker, double amount) {
		NewOrder newOrder = new NewOrder(
				ticker, OrderSide.SELL, OrderType.MARKET, TimeInForce.GTC, "" + amount);
		binanceApiRestClient.newOrderTest(newOrder);
		NewOrderResponse response = new NewOrderResponse();
		response.setClientOrderId("TESTSELL");
		response.setOrderId(12346L);
		response.setSymbol(ticker);
		response.setTransactTime(11L);
		return response;
	}

	public List<OwnedAsset> getBalancesWithValues() {
		List<OwnedAsset> balances = getAllBalances();
		return balances.stream().filter(c -> c.getFree() > 0.00001 || c.getLocked() > 0.00001).collect(Collectors.toList());
	}

	public OwnedAsset getBalance(String asset) {
		List<OwnedAsset> ownedAssets = getAllBalances();
		return ownedAssets.stream().filter(c -> c.getAsset().equalsIgnoreCase(asset)).findFirst().orElse(null);
	}

	public NewOrderResponse createBuyMarketOrder(String ticker, double amount, Long refId) {
		return  createMarketOrder(ticker, amount, OrderSide.BUY, refId);
	}

	public NewOrderResponse createSellMarketOrder(String ticker, double amount, Long refId) {
		return  createMarketOrder(ticker, amount, OrderSide.SELL, refId);
	}

	public NewOrderResponse createMarketOrder(String ticker, double amount, OrderSide orderSide, Long refId) {
		OrderHistory history = new OrderHistory().setAmount(amount).setSymbol(ticker).setSide(orderSide.name())
				.setStatus(OrderStatus.NEW.name()).setSwapId(refId == null || refId == 0 ? null : refId);
		logger.info("Attempting to " + orderSide + " " + amount + " of " + ticker);
		NewOrderResponse response = null;
		try {
			NewOrder newOrder = new NewOrder(
					ticker, orderSide, OrderType.MARKET, null, "" + amount);
			response = binanceApiRestClient.newOrder(newOrder);
			// Print out in case we have an issue
			logger.info(response);
			if (response == null) {
				logger.error("No response received - " + amount + " of " + ticker);
				return null;
			}
		} catch (Exception ex) {
			logger.error("Unable to " + orderSide + " " + amount + " of " + ticker, ex);
			return null;
		}
		try {
			history.setClientOrderId(response.getClientOrderId())
					.setOrderId(response.getOrderId())
					.setTransactTime(response.getTransactTime())
					.setCreateDtm(new Date());
			orderHistoryRepository.save(history);
		} catch (Exception ex) {
			logger.error("Unable to save " + orderSide + " " + amount + " of " + ticker, ex);
		}
		return response;
	}

	public Order checkOrderStatus(String ticker, String origClientOrderId) {
		OrderStatusRequest request = new OrderStatusRequest(ticker, origClientOrderId);
		return binanceApiRestClient.getOrderStatus(request);
	}

	public List<Order> getAllMyOrders(String ticker) {
		AllOrdersRequest request = new AllOrdersRequest(ticker);
		return binanceApiRestClient.getAllOrders(request);
	}

	public OrderBook getBookOrders(String ticker) {
		return binanceApiRestClient.getOrderBook(ticker, 1000);
	}

	@Override
	public double getMaxAmountAtTime() {
		return maxBaseCoinChunks;
	}

	public List<PriceData> getBackfillData(int cacheSize, String coin, String baseCoin) {
		int maxAtTime = 1000;
		Date now = new Date();
		Date originalStartDate = new Date(now.getTime() - cacheSize * ONE_HOUR);
		List<Candlestick> klines = new ArrayList<>();
		if (cacheSize <= maxAtTime) {
			klines.addAll(binanceApiRestClient.getCandlestickBars(coin + baseCoin, CandlestickInterval.HOURLY));
		} else {
			try {
				int remaining = cacheSize;
				Date lastDate = new Date();
				while (remaining > 0) {
					int numToGet = remaining > maxAtTime ? maxAtTime : remaining;
					long numHoursBackInMs = numToGet * ONE_HOUR;
					long startDateAsMs = lastDate.getTime() - numHoursBackInMs;
					Date startDate = new Date(startDateAsMs);
					List<Candlestick> returnedData = binanceApiRestClient.getCandlestickBars(
							coin + baseCoin, CandlestickInterval.HOURLY, numToGet, startDate.getTime(), lastDate.getTime());
					returnedData.addAll(klines);
					klines = returnedData;
					remaining = remaining - maxAtTime;
					lastDate = startDate;
				}
			} catch(Exception ex) {
				// There may be a max error if binance only goes back a certain amount of time, less than we specified
				logger.error(ex.getMessage());
			}
		}
		List<PriceData> pd = klines.stream()
				.map(c -> new PriceData()
						.setPrice(Double.parseDouble(c.getOpen()))
						.setTicker(coin + baseCoin)
						.setUpdateTime(new Date(c.getOpenTime()))
						.setExchange(ScratchConstants.Exchange.BINANCE))
				.collect(Collectors.toList());
		// Fill in holes so comparing prices between multiple coins doesn't fail
		pd = padEmptyListItems(pd, originalStartDate, now);

		if (pd == null || pd.size() <= cacheSize) {
			return pd;
		}
		while (pd.size() > cacheSize) {
			pd.remove(0);
		}
		return pd;
	}

	public static List<PriceData> padEmptyListItems(List<PriceData> oldList, Date startDate, Date endDate) {
		if (oldList == null) {
			return oldList;
		}
		List<PriceData> newList = new ArrayList<>();
		PriceData firstItem = oldList.get(0);
		Date earlyDate = firstItem.getUpdateTime();
		// See if we need to pad the start of the list
		if (earlyDate.getTime() - startDate.getTime() > ONE_HOUR) {
			Date nearestHour = DateUtils.ceiling(startDate, Calendar.HOUR);
			while (nearestHour.getTime() < firstItem.getUpdateTime().getTime()) {
				PriceData priceData = new PriceData().setPrice(firstItem.getPrice()).setExchange(firstItem.getExchange())
						.setTicker(firstItem.getTicker()).setUpdateTime(new Date(nearestHour.getTime()));
				newList.add(priceData);
				nearestHour.setTime(nearestHour.getTime() + ONE_HOUR);
			}
		}
		// Fill in gaps in the middle
		double earlyPrice =  firstItem.getPrice();
		for (int i=0; i<oldList.size(); i++) {
			PriceData p = oldList.get(i);
			Date dateVal = p.getUpdateTime();
			if (dateVal.getTime() == earlyDate.getTime()) {
				newList.add(p);
				continue;
			}
			if (dateVal.getTime() - earlyDate.getTime() != ONE_HOUR) {
				Date nextDate = new Date(earlyDate.getTime() + ONE_HOUR);
				// Get the average between the earlier and next value - not perfect, but needed to pad it
				double avgPrice = (p.getPrice() + earlyPrice)/2;
				while (nextDate.getTime() < dateVal.getTime()) {
					PriceData priceData = new PriceData().setPrice(avgPrice).setExchange(firstItem.getExchange())
							.setTicker(firstItem.getTicker()).setUpdateTime(new Date(nextDate.getTime()));
					newList.add(priceData);
					nextDate = new Date(nextDate.getTime() + ONE_HOUR);
				}
				// Now that we've caught up, continue on with the first after the gap
				newList.add(p);
			} else {
				newList.add(p);
				earlyPrice =  p.getPrice();
			}
			earlyDate = dateVal;
		}
		PriceData lastItem = oldList.get(oldList.size() - 1);
		// See if we need to pad the end of the list
		if (endDate.getTime() - lastItem.getUpdateTime().getTime() > ONE_HOUR) {
			Date nearestHourBefore = DateUtils.truncate(endDate, Calendar.HOUR);
			Date nextDate = new Date(lastItem.getUpdateTime().getTime() + ONE_HOUR);
			while (nextDate.getTime() < endDate.getTime()) {
				PriceData priceData = new PriceData().setPrice(lastItem.getPrice()).setExchange(firstItem.getExchange())
						.setTicker(firstItem.getTicker()).setUpdateTime(new Date(nextDate.getTime()));
				newList.add(priceData);
				nextDate.setTime(nextDate.getTime() + ONE_HOUR);
			}
		}
		return newList;
	}
}
