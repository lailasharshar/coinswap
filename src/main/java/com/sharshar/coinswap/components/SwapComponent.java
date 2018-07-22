package com.sharshar.coinswap.components;

import com.binance.api.client.domain.OrderStatus;
import com.binance.api.client.domain.account.NewOrderResponse;
import com.binance.api.client.domain.account.Order;
import com.binance.api.client.domain.account.Trade;
import com.sharshar.coinswap.beans.OwnedAsset;
import com.sharshar.coinswap.beans.PriceData;
import com.sharshar.coinswap.exchanges.AccountService;
import com.sharshar.coinswap.services.CacheService;
import com.sharshar.coinswap.services.ExchangeCache;
import com.sharshar.coinswap.services.TickerService;
import com.sharshar.coinswap.utils.CoinUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Encapsulates a coin swap service. This enables to instantiate more than one in this process
 * <p>
 * Created by lsharshar on 7/17/2018.
 */
@Component
@Scope("prototype")
public class SwapComponent {
	private Logger logger = LogManager.getLogger();

	ExchangeCache cache;

	@Autowired
	private CacheService cacheService;

	private String commissionAsset;

	public enum ResponseCode {
		Coin1NotSoldCoin2NotBought,
		Coin1PartiallySoldCoin2NotBought,
		Coin1SoldCoin2NotBought,
		Coin1SoldCoin2Bought,
		SellOrderNew,
		SellOrderFilled,
		SellOrderParitalFilled,
		SellOrderCancelled,
		SellOrderPendingCancel,
		SellOrderRejected,
		SellOrderExpired,
		BuyOrderNew,
		BuyOrderFilled,
		BuyOrderParitalFilled,
		BuyOrderCancelled,
		BuyOrderPendingCancel,
		BuyOrderRejected,
		BuyOrderExpired,
		NoCoin1Defined,
		NoCoin2Defined,
		NoBaseCoinDefined,
		SellOrderError,
		TransactionSuccessful,
		BuyOrderError,
		NotEnoughCommissionCurrency,
		NoCommissionCoinDefined,
		UnableToUpdateCoinBalances
	}

	public enum CurrentSwapState {
		ownsCoin1,
		ownsCoin2
	}

	private CurrentSwapState currentSwapState;
	private String coin1;
	private double maxAmountCoin1ToBuy;
	private double maxAmountCoin2ToBuy;
	private String coin2;
	private short exchange;
	private String baseCoin;
	private double amountCoin1OwnedFree;
	private double amountCoin2OwnedFree;
	private double amountBaseCoinOwnedFree;
	private double amountCommissionAssetFree;
	private boolean active;

	private AccountService accountService;

	@Autowired
	private TickerService tickerService;

	public SwapComponent(String coin1, String coin2, String commissionAsset, AccountService accountService,
						 double maxAmountCoin1ToBuy, double maxAmountCoin2ToBuy, double desiredStdDev) {
		this.coin1 = coin1;
		this.coin2 = coin2;
		this.commissionAsset = commissionAsset;
		this.exchange = accountService.getExchange();
		this.accountService = accountService;
		this.maxAmountCoin1ToBuy = maxAmountCoin1ToBuy;
		this.maxAmountCoin2ToBuy = maxAmountCoin2ToBuy;
		deriveBaseCoin();
		cache = cacheService.createCache(coin1 + getBaseCoin(), coin2 + getBaseCoin(),
				accountService.getExchange(), desiredStdDev);
		cache.setOwningComponent(this);
	}

	public String getCoin1() {
		return coin1;
	}

	public SwapComponent setCoin1(String coin1) {
		this.coin1 = coin1;
		return this;
	}

	public String getCoin2() {
		return coin2;
	}

	public SwapComponent setCoin2(String coin2) {
		this.coin2 = coin2;
		return this;
	}

	public short getExchange() {
		return exchange;
	}

	public SwapComponent setExchange(short exchange) {
		this.exchange = exchange;
		return this;
	}

	public AccountService getAccountService() {
		return accountService;
	}

	public SwapComponent setAccountService(AccountService accountService) {
		this.accountService = accountService;
		return this;
	}

	public String getBaseCoin() {
		if (baseCoin == null || baseCoin.isEmpty()) {
			baseCoin = deriveBaseCoin();
		}
		return baseCoin;
	}


	/**
	 * If we don't have a base coin defined, see if you can find one in which the two coins have one in common.
	 * Bitcoin is probably the safest, but in theory, it could find others.
	 *
	 * @return the best base coin to use
	 */
	public String deriveBaseCoin() {
		List<PriceData> priceData = accountService.getAllPrices();
		if (CoinUtils.hasPriceData(coin1 + tickerService.getDefaultBaseCurrency(), priceData)) {
			return tickerService.getDefaultBaseCurrency();
		}
		for (String baseCurrency : tickerService.getBaseCurrencies()) {
			if (baseCurrency.equalsIgnoreCase(coin1)) {
				return baseCurrency;
			}
			if (baseCurrency.equalsIgnoreCase(coin2)) {
				return baseCurrency;
			}
			if (CoinUtils.hasPriceData(coin1 + baseCurrency, priceData) &&
					CoinUtils.hasPriceData(coin2 + baseCurrency, priceData)) {
				return baseCurrency;
			}
		}
		return null;
	}

	/**
	 * Determines if all any necessary items are defined before we do any swap action
	 *
	 * @return the response code of the error or null if there is no errors
	 */
	private ResponseCode getAnyInitializationErrors() {
		if (coin1 == null || coin1.isEmpty()) {
			return ResponseCode.NoCoin1Defined;
		}
		if (coin2 == null || coin2.isEmpty()) {
			return ResponseCode.NoCoin2Defined;
		}
		if (getBaseCoin() == null || getBaseCoin().isEmpty()) {
			return ResponseCode.NoBaseCoinDefined;
		}
		if (commissionAsset == null || commissionAsset.isEmpty()) {
			return ResponseCode.NoCommissionCoinDefined;
		}
		return null;
	}

	/**
	 * Simulate trading to do analytics
	 *
	 * @param amountCoinAToSell - How much of coinA to sell. This is usually all of it
	 * @param coinA - The coin to sell (without coin base)
	 * @param coinB - the coin to buy (without coin base)
	 * @param maxCoinToBuy - the maximum amount you want to buy of coinB
	 * @param priceData - the latest price data so we can calculate things like transaction fees and how much of
	 *                  coinB we can buy
	 * @return the response code to the trade. This will almost always be TransactionSuccessful unless the component
	 * is not correctly initialized
	 */
	public ResponseCode simulateCoinSwap(double amountCoinAToSell, String coinA, String coinB, double maxCoinToBuy, List<PriceData> priceData) {
		ResponseCode initErrors = getAnyInitializationErrors();
		if (initErrors != null) {
			return initErrors;
		}
		/* We'll have to sell first */

		// No reason to sell if the base coin is the same as coinA
		if (!coinA.equalsIgnoreCase(getBaseCoin())) {
			// Do the trade
			logger.info("Sell order filled for: " + coinA + getBaseCoin());
			amountCoin1OwnedFree -= amountCoinAToSell;
			amountCommissionAssetFree -= getCommissionNeeded(coinA, amountCoinAToSell, accountService.getDefaultTransactionFee(), priceData);
		} else {
			double coinAPrice = CoinUtils.getPrice(coinA + getBaseCoin(), priceData);
			amountBaseCoinOwnedFree += amountCoinAToSell * coinAPrice;
		}

		/* Then we buy */

		// No reason to buy if the base coin is the same as coinB
		if (!coinB.equalsIgnoreCase(getBaseCoin())) {
			// Figure out how much we can buy
			double amountCoinToBuy = (CoinUtils.getPrice(coinA + getBaseCoin(), priceData) * amountCoinAToSell) / CoinUtils.getPrice(coinB + getBaseCoin(), priceData);
			if (amountCoinToBuy > maxCoinToBuy && maxCoinToBuy > 0) {
				amountCoinToBuy = maxCoinToBuy;
			}

			logger.info("Buy order filled for: " + coinB + getBaseCoin());
			amountCoin2OwnedFree += amountCoinToBuy;
			amountCommissionAssetFree -= getCommissionNeeded(coinB, amountCoinToBuy, accountService.getDefaultTransactionFee(), priceData);
		}

		return ResponseCode.TransactionSuccessful;
	}

	/**
	 * Perform a coin swap. First it sells the amount specifies. It assumes you take the entire amount of the sold
	 * proceeds to buy the other coin.
	 *
	 * @param amountCoinAToSell - The amount of coinA to sell
	 * @param coinA - The coin to sell (without coin base)
	 * @param coinB - the coin to buy (without coin base)
	 * @param maxCoinToBuy - the maximum amount you want to buy of coinB
	 * @param priceData - the latest price data so we can calculate things like transaction fees and how much of
	 *                  coinB we can buy
	 * @return the response code to the trade
	 */
	public ResponseCode coinSwap(double amountCoinAToSell, String coinA, String coinB, double maxCoinToBuy, List<PriceData> priceData) {

		ResponseCode initErrors = getAnyInitializationErrors();
		if (initErrors != null) {
			return initErrors;
		}

		/* We'll have to sell first */

		// No reason to sell if the base coin is the same as coin1
		if (!coinA.equalsIgnoreCase(getBaseCoin())) {

			// Make sure we have enough coin in commissions to do the trade
			boolean haveEnoughForCommission = haveEnoughForCommission(coinA, amountCoinAToSell, accountService.getDefaultTransactionFee(), priceData);
			if (!haveEnoughForCommission) {
				return ResponseCode.NotEnoughCommissionCurrency;
			}

			// Do the trade
			ResponseCode sellResponse = sellCoin(coinA, amountCoinAToSell);
			if (sellResponse == ResponseCode.SellOrderFilled) {
				logger.info("Sell order filled for: " + coinA + getBaseCoin());
				if (!loadBalances()) {
					return ResponseCode.UnableToUpdateCoinBalances;
				}
			} else {
				return sellResponse;
			}
		}

		/* Then we buy */

		// No reason to buy if the base coin is the same as coinB
		if (!coinB.equalsIgnoreCase(getBaseCoin())) {
			// Figure out how much we can buy
			double amountCoinToBuy = (CoinUtils.getPrice(coinA + getBaseCoin(), priceData) * amountCoinAToSell) / CoinUtils.getPrice(coinB + getBaseCoin(), priceData);
			if (amountCoinToBuy > maxCoinToBuy && maxCoinToBuy > 0) {
				amountCoinToBuy = maxCoinToBuy;
			}
			// Make sure we have enough coin in commission to do the trade
			boolean haveEnoughForCommission = haveEnoughForCommission(coinB, amountCoinToBuy, accountService.getDefaultTransactionFee(), priceData);
			if (!haveEnoughForCommission) {
				return ResponseCode.NotEnoughCommissionCurrency;
			}

			ResponseCode buyResponse = buyCoin(coinB, amountCoinToBuy);
			if (buyResponse == ResponseCode.BuyOrderFilled) {
				logger.info("Buy order filled for: " + coinB + getBaseCoin());
				if (!loadBalances()) {
					return ResponseCode.UnableToUpdateCoinBalances;
				}
			} else {
				return buyResponse;
			}
		}
		return ResponseCode.TransactionSuccessful;
	}

	/**
	 * Swaps coin1 to coin2
	 *
	 * @param priceData - the list of price data to estimate fees
	 * @param simulate - if we want to do it for real, or just simulate it
	 * @return the response code
	 */
	public ResponseCode swapCoin1ToCoin2(List<PriceData> priceData, boolean simulate) {
		ResponseCode code = null;
		if (simulate) {
			code = simulateCoinSwap(amountCoin1OwnedFree, coin1, coin2, maxAmountCoin2ToBuy, priceData);
		} else {
			code = coinSwap(amountCoin1OwnedFree, coin1, coin2, maxAmountCoin2ToBuy, priceData);
		}
		if (code == ResponseCode.BuyOrderFilled) {
			currentSwapState = CurrentSwapState.ownsCoin2;
		}
		return code;
	}

	/**
	 * Swaps coin2 to coin1
	 *
	 * @param priceData - the list of price data to estimate fees
	 * @param simulate - if we want to do it for real, or just simulate it
	 * @return the response code
	 */
	public ResponseCode swapCoin2ToCoin1(List<PriceData> priceData, boolean simulate) {
		ResponseCode code = null;
		if (simulate) {
			code = simulateCoinSwap(amountCoin2OwnedFree, coin2, coin1, maxAmountCoin1ToBuy, priceData);
		} else {
			code = coinSwap(amountCoin2OwnedFree, coin2, coin1, maxAmountCoin1ToBuy, priceData);
		}
		if (code == ResponseCode.BuyOrderFilled) {
			currentSwapState = CurrentSwapState.ownsCoin1;
		}
		return code;
	}

	/**
	 * Load balances for each of the coins (coin1, coin2, base asset, commission asset)
	 *
	 * @return if it was successfully done
	 */
	public boolean loadBalances() {
		List<OwnedAsset> assets = accountService.getAllBalances();
		if (assets == null) {
			return false;
		}
		OwnedAsset ownedAsset1 = CoinUtils.getAssetValue(coin1, assets);
		OwnedAsset ownedAsset2 = CoinUtils.getAssetValue(coin2, assets);
		OwnedAsset ownedAssetBaseAsset = CoinUtils.getAssetValue(getBaseCoin(), assets);
		OwnedAsset ownedCommisionAsset = CoinUtils.getAssetValue(commissionAsset, assets);
		amountCoin1OwnedFree = CoinUtils.getAmountOwnedValueFree(ownedAsset1);
		amountCoin2OwnedFree = CoinUtils.getAmountOwnedValueFree(ownedAsset2);
		amountBaseCoinOwnedFree = CoinUtils.getAmountOwnedValueFree(ownedAssetBaseAsset);
		amountCommissionAssetFree = CoinUtils.getAmountOwnedValueFree(ownedCommisionAsset);
		return !(ownedAsset1 == null || ownedAsset2 == null || ownedAssetBaseAsset == null || ownedCommisionAsset == null);
	}

	/**
	 * Estimate the amount of commission needed. A trade won't work if you don't have enough of the commission coin.
	 *
	 * @param coin - The coin you want to buy/sell
	 * @param amountCoin - the amount of coin you wan to buy or sell
	 * @param defaultTransactionFee - The default transaction fee
	 * @param priceData - the price data to do the conversion from the coin to the commission coin
	 * @return the amount of commission we should need
	 */
	private double getCommissionNeeded(String coin, double amountCoin, double defaultTransactionFee,
									  List<PriceData> priceData) {

		double priceForCoin = CoinUtils.getPrice(coin + getBaseCoin(), priceData);
		double priceForCommissionCoin = CoinUtils.getPrice(commissionAsset + getBaseCoin(), priceData);

		// Commission in base currency
		double commission = amountCoin * priceForCoin * defaultTransactionFee;
		return commission / priceForCommissionCoin;
	}

	/**
	 * Determine if we have enough commission coin to do a transaction. A trade won't work if you don't have enough of
	 * the commission coin. Assume a factor of 2 for a buffer
	 *
	 * @param coin - The coin you want to buy/sell
	 * @param amountCoin - the amount of coin you wan to buy or sell
	 * @param defaultTransactionFee - The default transaction fee
	 * @param priceData - the price data to do the conversion from the coin to the commission coin
	 * @return if we have enough to do the trade
	 */
	private boolean haveEnoughForCommission(String coin, double amountCoin, double defaultTransactionFee,
										   List<PriceData> priceData) {

		double commissionCoinNeeded = getCommissionNeeded(coin, amountCoin, defaultTransactionFee, priceData);

		// Create a buffer. Should have 2x the commission fee
		return (2.0 * commissionCoinNeeded < amountCommissionAssetFree);
	}

	/**
	 * Create a market order to sell a coin with the exchange
	 *
	 * @param coin - the coin to sell
	 * @param amountCoinToSell - the amount to sell
	 * @return the response code to the sell
	 */
	private ResponseCode sellCoin(String coin, double amountCoinToSell) {
		logger.info("Selling " + amountCoinToSell + " of " + coin);
		long startTime = System.currentTimeMillis();
		NewOrderResponse response = accountService.createSellMarketOrder(coin + getBaseCoin(), amountCoinToSell);
		if (response == null) {
			return ResponseCode.SellOrderError;
		}

		// Retrieve the transaction Id
		String clientId = response.getClientOrderId();
		Order order = waitForResponse(coin + getBaseCoin(), clientId);

		double totalTimeInSeconds = (System.currentTimeMillis() - startTime) / 1000;
		logger.info("Sell order " + order.getClientOrderId() + " (" + order.getOrderId() + ") completed in " + totalTimeInSeconds + " seconds");

		OrderStatus status = order.getStatus();
		// Check status of order
		switch (status) {
			case PARTIALLY_FILLED:
				return ResponseCode.SellOrderParitalFilled;
			case FILLED:
				return ResponseCode.SellOrderFilled;
			case NEW:
				return ResponseCode.SellOrderNew;
			case CANCELED:
				return ResponseCode.SellOrderCancelled;
			case PENDING_CANCEL:
				return ResponseCode.SellOrderPendingCancel;
			case REJECTED:
				return ResponseCode.SellOrderRejected;
			case EXPIRED:
				return ResponseCode.SellOrderExpired;
		}
		return ResponseCode.SellOrderFilled;
	}

	/**
	 * Once we do a trade, determine the actual commission. We usually just reload the values so we don't need to do
	 * the calculation ourselves, but could be useful in other contexts
	 *
	 * @param ticker - the ticker (coin + base currency) used in the trade
	 * @param orderId - the order id of the trade
	 * @return the amount of commission
	 */
	public double getActualCommissions(String ticker, long orderId) {
		List<Trade> myTrades = accountService.getMyTrades(ticker, String.valueOf(orderId));
		return myTrades.stream().mapToDouble(c -> Double.parseDouble(c.getCommission())).sum();
	}

	/**
	 * Purchase the coin from the exchange
	 * @param coin - the coin to buy
	 * @param amountToBuy - the amount to buy
	 * @return the response code of the purchase
	 */
	private ResponseCode buyCoin(String coin, double amountToBuy) {
		logger.info("Buying " + amountToBuy + " of " + coin);
		long startTime = System.currentTimeMillis();
		NewOrderResponse response = accountService.createBuyMarketOrder(coin + getBaseCoin(), amountToBuy);
		if (response == null) {
			return ResponseCode.BuyOrderError;
		}

		// Retrieve the transaction Id
		String clientId = response.getClientOrderId();
		// Wait for the order to be filled
		Order order = waitForResponse(coin + getBaseCoin(), clientId);

		double totalTimeInSeconds = ((double) (System.currentTimeMillis() - startTime)) / 1000;
		logger.info("Buy order " + order.getClientOrderId() + " (" + order.getOrderId() + ") completed in " + totalTimeInSeconds + " seconds");

		OrderStatus status = order.getStatus();
		// Check status of order
		switch (status) {
			case PARTIALLY_FILLED:
				return ResponseCode.BuyOrderParitalFilled;
			case FILLED:
				return ResponseCode.BuyOrderFilled;
			case NEW:
				return ResponseCode.BuyOrderNew;
			case CANCELED:
				return ResponseCode.BuyOrderCancelled;
			case PENDING_CANCEL:
				return ResponseCode.BuyOrderPendingCancel;
			case REJECTED:
				return ResponseCode.BuyOrderRejected;
			case EXPIRED:
				return ResponseCode.BuyOrderExpired;
		}
		return ResponseCode.BuyOrderFilled;
	}

	/**
	 * Doesn't return until the exchange buy/sell has completed or failed
	 * @param ticker - the ticker (coin + base coin) to check
	 * @param clientId - the client order id
	 * @return - the order in the completed state or error state
	 */
	private Order waitForResponse(String ticker, String clientId) {
		Order order = accountService.checkOrderStatus(ticker, clientId);
		OrderStatus status = order.getStatus();
		while (status == OrderStatus.PARTIALLY_FILLED) {
			try {
				Thread.sleep(1000);
			} catch (Exception ex) {
				logger.error("Unable to sleep thread between purchase and checking");
			}
			order = accountService.checkOrderStatus(ticker, clientId);
			status = order.getStatus();
		}
		return order;
	}

	public String getCommissionAsset() {
		return commissionAsset;
	}

	public double getAmountCoin1OwnedFree() {
		return amountCoin1OwnedFree;
	}

	public double getAmountCoin2OwnedFree() {
		return amountCoin2OwnedFree;
	}

	public double getAmountBaseCoinOwnedFree() {
		return amountBaseCoinOwnedFree;
	}

	public double getAmountCommissionAssetFree() {
		return amountCommissionAssetFree;
	}

	public CurrentSwapState getCurrentSwapState() {
		return currentSwapState;
	}

	public double getMaxAmountCoin1ToBuy() {
		return maxAmountCoin1ToBuy;
	}

	public SwapComponent setMaxAmountCoin1ToBuy(double maxAmountCoin1ToBuy) {
		this.maxAmountCoin1ToBuy = maxAmountCoin1ToBuy;
		return this;
	}

	public double getMaxAmountCoin2ToBuy() {
		return maxAmountCoin2ToBuy;
	}

	public SwapComponent setMaxAmountCoin2ToBuy(double maxAmountCoin2ToBuy) {
		this.maxAmountCoin2ToBuy = maxAmountCoin2ToBuy;
		return this;
	}

	public SwapComponent setBaseCoin(String baseCoin) {
		this.baseCoin = baseCoin;
		return this;
	}

	public boolean isActive() {
		return active;
	}

	public SwapComponent setActive(boolean active) {
		this.active = active;
		return this;
	}

	public ExchangeCache getCache() {
		return cache;
	}
}
