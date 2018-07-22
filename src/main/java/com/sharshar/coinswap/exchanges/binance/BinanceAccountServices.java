package com.sharshar.coinswap.exchanges.binance;

import com.binance.api.client.BinanceApiRestClient;
import com.binance.api.client.constant.BinanceApiConstants;
import com.binance.api.client.domain.OrderSide;
import com.binance.api.client.domain.OrderStatus;
import com.binance.api.client.domain.OrderType;
import com.binance.api.client.domain.TimeInForce;
import com.binance.api.client.domain.account.*;
import com.binance.api.client.domain.account.request.AllOrdersRequest;
import com.binance.api.client.domain.account.request.OrderStatusRequest;
import com.binance.api.client.domain.market.TickerPrice;
import com.sharshar.coinswap.beans.OrderHistory;
import com.sharshar.coinswap.beans.OwnedAsset;
import com.sharshar.coinswap.beans.PriceData;
import com.sharshar.coinswap.exchanges.AccountService;
import com.sharshar.coinswap.repositories.OrderHistoryRepository;
import com.sharshar.coinswap.utils.ScratchConstants;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
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

	@Value("${binance.transactionFee}")
	private double transactionFee;

	public double getDefaultTransactionFee() {
		return transactionFee;
	}

	public boolean checkUp() {
		Long serverTime = binanceApiRestClient.getServerTime();
		return serverTime != null && serverTime.longValue() > 0;
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

	public short getExchange() {
		return ScratchConstants.BINANCE;
	}

	public List<PriceData> getAllPrices() {
		List<TickerPrice> allPrices = binanceApiRestClient.getAllPrices();
		List<PriceData> priceData = new ArrayList<>();
		if (allPrices == null || allPrices.isEmpty()) {
			logger.error("Unable to load prices from Binance");
			return priceData;
		}
		Date now = new Date();
		for (TickerPrice tp : allPrices) {
			PriceData pd = new PriceData().setTicker(tp.getSymbol()).setExchange(ScratchConstants.BINANCE).setUpdateTime(now);
			try {
				double price = Double.parseDouble(tp.getPrice());
				pd.setPrice(price);
			} catch (Exception ex) {
				logger.error("Unable to parse value of : " + tp.getSymbol() + " - " + tp.getPrice());
				pd.setPrice(0.0);
			}
			priceData.add(pd);
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

	public void buyMarketTest(String ticker, double amount) {
		NewOrder newOrder = new NewOrder(
				ticker, OrderSide.BUY, OrderType.MARKET, TimeInForce.GTC, "" + amount);
		binanceApiRestClient.newOrderTest(newOrder);
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
}
