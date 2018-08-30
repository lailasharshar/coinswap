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
import com.binance.api.client.domain.market.OrderBook;
import com.binance.api.client.domain.market.TickerPrice;
import com.binance.api.client.domain.market.TickerStatistics;
import com.sharshar.coinswap.beans.OrderHistory;
import com.sharshar.coinswap.beans.OwnedAsset;
import com.sharshar.coinswap.beans.PriceData;
import com.sharshar.coinswap.beans.Ticker;
import com.sharshar.coinswap.exchanges.AccountService;
import com.sharshar.coinswap.exchanges.Data;
import com.sharshar.coinswap.exchanges.HistoricalDataPull;
import com.sharshar.coinswap.repositories.OrderHistoryRepository;
import com.sharshar.coinswap.utils.ScratchConstants;
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
		TickerStatistics stats = binanceApiRestClient.get24HrPriceStatistics(ticker);
		return Double.parseDouble(stats.getVolume());
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

	public NewOrderResponse createBuyMarketOrder(String ticker, double amount) {
		return  createMarketOrder(ticker, amount, OrderSide.BUY);
	}

	public NewOrderResponse createSellMarketOrder(String ticker, double amount) {
		return  createMarketOrder(ticker, amount, OrderSide.SELL);
	}

	public NewOrderResponse createMarketOrder(String ticker, double amount, OrderSide orderSide) {
		OrderHistory history = new OrderHistory().setAmount(amount).setSymbol(ticker).setSide(orderSide.name()).setStatus(OrderStatus.NEW.name());
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

	public List<PriceData> getBackfillData(int cacheSize, String coin, String baseCoin) {
		List<PriceData> pdList = new ArrayList<>();

		List<Data> historicalDataPullData = historicalDataPull.getData(coin, baseCoin, cacheSize, "Binance");
		if (historicalDataPullData == null) {
			return pdList;
		}
		return historicalDataPullData.stream().map(c ->
				new PriceData().setExchange(ScratchConstants.Exchange.BINANCE).setPrice(c.getOpen()).setTicker(coin + baseCoin).setUpdateTime(c.getTime()))
				.collect(Collectors.toList());
	}

	public OrderBook getBookOrders(String ticker) {
		return binanceApiRestClient.getOrderBook(ticker, 1000);
	}
}
