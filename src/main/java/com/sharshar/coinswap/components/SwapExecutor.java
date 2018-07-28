package com.sharshar.coinswap.components;

import com.binance.api.client.domain.OrderStatus;
import com.binance.api.client.domain.account.NewOrderResponse;
import com.binance.api.client.domain.account.Order;
import com.binance.api.client.domain.account.Trade;
import com.sharshar.coinswap.beans.OwnedAsset;
import com.sharshar.coinswap.beans.PriceData;
import com.sharshar.coinswap.beans.SwapDescriptor;
import com.sharshar.coinswap.exchanges.AccountService;
import com.sharshar.coinswap.utils.CoinUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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
public class SwapExecutor {
	private Logger logger = LogManager.getLogger();

	public SwapExecutor setCache(ExchangeCache cache) {
		this.cache = cache;
		return this;
	}

	private ExchangeCache cache;

	public enum ResponseCode {
		SELL_ORDER_NEW,
		SELL_ORDER_FILLED,
		SELL_ORDER_PARTIAL_FILLED,
		SELL_ORDER_CANCELLED,
		SELL_ORDER_PENDING_CANCEL,
		SELL_ORDER_REJECTED,
		SELL_ORDER_EXPIRED,
		BUY_ORDER_NEW,
		BUY_ORDER_FILLED,
		BUY_ORDER_PARTIAL_FILLED,
		BUY_ORDER_CANCELLED,
		BUY_ORDER_PENDING_CANCEL,
		BUY_ORDER_REJECTED,
		BUY_ORDER_EXPIRED,
		NO_COIN_1_DEFINED,
		NO_COIN_2_DEFINED,
		NO_BASE_COIN_DEFINED,
		SELL_ORDER_ERROR,
		TRANSACTION_SUCCESSFUL,
		BUY_ORDER_ERROR,
		NOT_ENOUGH_COMMISSION_CURRENCY,
		NO_COMMISSION_COIN_DEFINED,
		NO_SWAP_DEFINITION_DEFINED,
		UNABLE_TO_UPDATE_COIN_BALANCES
	}

	public enum CurrentSwapState {
		OWNS_COIN_1,
		OWNS_COIN_2,
		OWNS_NOTHING
	}

	private SwapDescriptor swapDescriptor;

	private CurrentSwapState currentSwapState;
	private double amountCoin1OwnedFree;
	private double amountCoin2OwnedFree;
	private double amountBaseCoinOwnedFree;
	private double amountCommissionAssetFree;
	private String baseCoin;

	private AccountService accountService;

	public SwapExecutor(SwapDescriptor swapDescriptor, AccountService accountService, String baseCoin) {
		this.swapDescriptor = swapDescriptor;
		this.accountService = accountService;
		this.currentSwapState = CurrentSwapState.OWNS_COIN_1;
		this.baseCoin = baseCoin;
	}

	public String getBaseCoin() {
		return baseCoin;
	}

	/**
	 * Determines if all any necessary items are defined before we do any swap action
	 *
	 * @return the response code of the error or null if there is no errors
	 */
	private ResponseCode getAnyInitializationErrors() {
		if (swapDescriptor == null) {
			return ResponseCode.NO_SWAP_DEFINITION_DEFINED;
		}
		if (swapDescriptor.getCoin1() == null || swapDescriptor.getCoin1().isEmpty()) {
			return ResponseCode.NO_COIN_1_DEFINED;
		}
		if (swapDescriptor.getCoin2() == null || swapDescriptor.getCoin2().isEmpty()) {
			return ResponseCode.NO_COIN_2_DEFINED;
		}
		if (baseCoin == null || baseCoin.isEmpty()) {
			return ResponseCode.NO_BASE_COIN_DEFINED;
		}
		if (swapDescriptor.getCommissionCoin() == null || swapDescriptor.getCommissionCoin().isEmpty()) {
			return ResponseCode.NO_COMMISSION_COIN_DEFINED;
		}
		return null;
	}

	/**
	 * Simulate trading to do analytics
	 *
	 * @param amountCoinAToSell - How much of coinA to sell. This is usually all of it
	 * @param coinA             - The coin to sell (without coin base)
	 * @param coinB             - the coin to buy (without coin base)
	 * @param maxCoinToBuy      - the maximum amount you want to buy of coinB
	 * @param priceData         - the latest price data so we can calculate things like transaction fees and how much of
	 *                          coinB we can buy
	 * @return the response code to the trade. This will almost always be TRANSACTION_SUCCESSFUL unless the component
	 * is not correctly initialized
	 */
	private ResponseCode simulateCoinSwap(double amountCoinAToSell, String coinA, String coinB, double maxCoinToBuy, List<PriceData> priceData) {
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

		return ResponseCode.TRANSACTION_SUCCESSFUL;
	}

	/**
	 * Perform a coin swap. First it sells the amount specifies. It assumes you take the entire amount of the sold
	 * proceeds to buy the other coin.
	 *
	 * @param amountCoinAToSell - The amount of coinA to sell
	 * @param coinA             - The coin to sell (without coin base)
	 * @param coinB             - the coin to buy (without coin base)
	 * @param maxCoinToBuy      - the maximum amount you want to buy of coinB
	 * @param priceData         - the latest price data so we can calculate things like transaction fees and how much of
	 *                          coinB we can buy
	 * @return the response code to the trade
	 */
	private ResponseCode coinSwap(double amountCoinAToSell, String coinA, String coinB, double maxCoinToBuy, List<PriceData> priceData) {

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
				return ResponseCode.NOT_ENOUGH_COMMISSION_CURRENCY;
			}

			// Do the trade
			ResponseCode sellResponse = sellCoin(coinA, amountCoinAToSell);
			if (sellResponse == ResponseCode.SELL_ORDER_FILLED) {
				logger.info("Sell order filled for: " + coinA + getBaseCoin());
				if (!loadBalances()) {
					return ResponseCode.UNABLE_TO_UPDATE_COIN_BALANCES;
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
				return ResponseCode.NOT_ENOUGH_COMMISSION_CURRENCY;
			}

			ResponseCode buyResponse = buyCoin(coinB, amountCoinToBuy);
			if (buyResponse == ResponseCode.BUY_ORDER_FILLED) {
				logger.info("Buy order filled for: " + coinB + getBaseCoin());
				if (!loadBalances()) {
					return ResponseCode.UNABLE_TO_UPDATE_COIN_BALANCES;
				}
			} else {
				return buyResponse;
			}
		}
		return ResponseCode.TRANSACTION_SUCCESSFUL;
	}

	/**
	 * Swaps coin1 to coin2
	 *
	 * @param priceData - the list of price data to estimate fees
	 * @param simulate  - if we want to do it for real, or just simulate it
	 * @return the response code
	 */
	public ResponseCode swapCoin1ToCoin2(List<PriceData> priceData, boolean simulate) {
		ResponseCode code = null;
		if (simulate) {
			code = simulateCoinSwap(amountCoin1OwnedFree, swapDescriptor.getCoin1(), swapDescriptor.getCoin2(),
					swapDescriptor.getMaxAmountCoin2ToBuy(), priceData);
		} else {
			code = coinSwap(amountCoin1OwnedFree, swapDescriptor.getCoin1(), swapDescriptor.getCoin2(),
					swapDescriptor.getMaxAmountCoin2ToBuy(), priceData);
		}
		if (code == ResponseCode.BUY_ORDER_FILLED) {
			currentSwapState = CurrentSwapState.OWNS_COIN_2;
		}
		return code;
	}

	/**
	 * Swaps coin2 to coin1
	 *
	 * @param priceData - the list of price data to estimate fees
	 * @param simulate  - if we want to do it for real, or just simulate it
	 * @return the response code
	 */
	public ResponseCode swapCoin2ToCoin1(List<PriceData> priceData, boolean simulate) {
		ResponseCode code = null;
		if (simulate) {
			code = simulateCoinSwap(amountCoin2OwnedFree, swapDescriptor.getCoin2(), swapDescriptor.getCoin1(),
					swapDescriptor.getMaxAmountCoin1ToBuy(), priceData);
		} else {
			code = coinSwap(amountCoin2OwnedFree, swapDescriptor.getCoin2(), swapDescriptor.getCoin1(),
					swapDescriptor.getMaxAmountCoin1ToBuy(), priceData);
		}
		if (code == ResponseCode.BUY_ORDER_FILLED) {
			currentSwapState = CurrentSwapState.OWNS_COIN_1;
		}
		return code;
	}

	/**
	 * Load balances for each of the coins (coin1, coin2, base asset, commission asset)
	 *
	 * @return if it was successfully done
	 */
	private boolean loadBalances() {
		List<OwnedAsset> assets = accountService.getAllBalances();
		if (assets == null) {
			return false;
		}
		OwnedAsset ownedAsset1 = CoinUtils.getAssetValue(swapDescriptor.getCoin1(), assets);
		OwnedAsset ownedAsset2 = CoinUtils.getAssetValue(swapDescriptor.getCoin2(), assets);
		OwnedAsset ownedAssetBaseAsset = CoinUtils.getAssetValue(getBaseCoin(), assets);
		OwnedAsset ownedCommisionAsset = CoinUtils.getAssetValue(swapDescriptor.getCommissionCoin(), assets);
		amountCoin1OwnedFree = CoinUtils.getAmountOwnedValueFree(ownedAsset1);
		amountCoin2OwnedFree = CoinUtils.getAmountOwnedValueFree(ownedAsset2);
		amountBaseCoinOwnedFree = CoinUtils.getAmountOwnedValueFree(ownedAssetBaseAsset);
		amountCommissionAssetFree = CoinUtils.getAmountOwnedValueFree(ownedCommisionAsset);
		return !(ownedAsset1 == null || ownedAsset2 == null || ownedAssetBaseAsset == null || ownedCommisionAsset == null);
	}

	/**
	 * Estimate the amount of commission needed. A trade won't work if you don't have enough of the commission coin.
	 *
	 * @param coin                  - The coin you want to buy/sell
	 * @param amountCoin            - the amount of coin you wan to buy or sell
	 * @param defaultTransactionFee - The default transaction fee
	 * @param priceData             - the price data to do the conversion from the coin to the commission coin
	 * @return the amount of commission we should need
	 */
	private double getCommissionNeeded(String coin, double amountCoin, double defaultTransactionFee,
									   List<PriceData> priceData) {

		double priceForCoin = CoinUtils.getPrice(coin + getBaseCoin(), priceData);
		double priceForCommissionCoin = CoinUtils.getPrice(swapDescriptor.getCommissionCoin() + getBaseCoin(), priceData);

		// Commission in base currency
		double commission = amountCoin * priceForCoin * defaultTransactionFee;
		return commission / priceForCommissionCoin;
	}

	/**
	 * Determine if we have enough commission coin to do a transaction. A trade won't work if you don't have enough of
	 * the commission coin. Assume a factor of 2 for a buffer
	 *
	 * @param coin                  - The coin you want to buy/sell
	 * @param amountCoin            - the amount of coin you wan to buy or sell
	 * @param defaultTransactionFee - The default transaction fee
	 * @param priceData             - the price data to do the conversion from the coin to the commission coin
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
	 * @param coin             - the coin to sell
	 * @param amountCoinToSell - the amount to sell
	 * @return the response code to the sell
	 */
	private ResponseCode sellCoin(String coin, double amountCoinToSell) {
		logger.info("Selling " + amountCoinToSell + " of " + coin);
		long startTime = System.currentTimeMillis();
		NewOrderResponse response = accountService.createSellMarketOrder(coin + getBaseCoin(), amountCoinToSell);
		if (response == null) {
			return ResponseCode.SELL_ORDER_ERROR;
		}

		// Retrieve the transaction Id
		String clientId = response.getClientOrderId();
		Order order = waitForResponse(coin + getBaseCoin(), clientId);

		double totalTimeInSeconds = ((double) System.currentTimeMillis() - startTime) / 1000;
		logger.info("Sell order " + order.getClientOrderId() + " (" + order.getOrderId() + ") completed in " + totalTimeInSeconds + " seconds");

		OrderStatus status = order.getStatus();
		// Check status of order
		switch (status) {
			case PARTIALLY_FILLED:
				return ResponseCode.SELL_ORDER_PARTIAL_FILLED;
			case FILLED:
				return ResponseCode.SELL_ORDER_FILLED;
			case NEW:
				return ResponseCode.SELL_ORDER_NEW;
			case CANCELED:
				return ResponseCode.SELL_ORDER_CANCELLED;
			case PENDING_CANCEL:
				return ResponseCode.SELL_ORDER_PENDING_CANCEL;
			case REJECTED:
				return ResponseCode.SELL_ORDER_REJECTED;
			case EXPIRED:
				return ResponseCode.SELL_ORDER_EXPIRED;
		}
		return ResponseCode.SELL_ORDER_FILLED;
	}

	/**
	 * Once we do a trade, determine the actual commission. We usually just reload the values so we don't need to do
	 * the calculation ourselves, but could be useful in other contexts
	 *
	 * @param ticker  - the ticker (coin + base currency) used in the trade
	 * @param orderId - the order id of the trade
	 * @return the amount of commission
	 */
	public double getActualCommissions(String ticker, long orderId) {
		List<Trade> myTrades = accountService.getMyTrades(ticker, String.valueOf(orderId));
		return myTrades.stream().mapToDouble(c -> Double.parseDouble(c.getCommission())).sum();
	}

	/**
	 * Purchase the coin from the exchange
	 *
	 * @param coin        - the coin to buy
	 * @param amountToBuy - the amount to buy
	 * @return the response code of the purchase
	 */
	private ResponseCode buyCoin(String coin, double amountToBuy) {
		logger.info("Buying " + amountToBuy + " of " + coin);
		long startTime = System.currentTimeMillis();
		NewOrderResponse response = accountService.createBuyMarketOrder(coin + getBaseCoin(), amountToBuy);
		if (response == null) {
			return ResponseCode.BUY_ORDER_ERROR;
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
				return ResponseCode.BUY_ORDER_PARTIAL_FILLED;
			case FILLED:
				return ResponseCode.BUY_ORDER_FILLED;
			case NEW:
				return ResponseCode.BUY_ORDER_NEW;
			case CANCELED:
				return ResponseCode.BUY_ORDER_CANCELLED;
			case PENDING_CANCEL:
				return ResponseCode.BUY_ORDER_PENDING_CANCEL;
			case REJECTED:
				return ResponseCode.BUY_ORDER_REJECTED;
			case EXPIRED:
				return ResponseCode.BUY_ORDER_EXPIRED;
		}
		return ResponseCode.BUY_ORDER_FILLED;
	}

	/**
	 * Doesn't return until the exchange buy/sell has completed or failed
	 *
	 * @param ticker   - the ticker (coin + base coin) to check
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

	public ExchangeCache getCache() {
		return cache;
	}

	/**
	 * If we change the active state, we have to reset any data
	 *
	 * @param active if the status is active
	 */
	public void resetActive(boolean active) {
		// Maybe already set, but make sure
		swapDescriptor.setActive(active);

		// Load balances
		this.loadBalances();

		// Determine which coin we currently own of the two swapped items
		currentSwapState = findOutCurrentState();

		// Clear out the cache
		cache.clear();
	}

	private CurrentSwapState findOutCurrentState() {
		// If we don't own anything, return owns nothing
		if (amountCoin1OwnedFree == 0 && amountCoin2OwnedFree == 0) {
			return CurrentSwapState.OWNS_NOTHING;
		}
		// If one value is 0 and the other one isn't, we own the other one.
		if (amountCoin1OwnedFree == 0 && amountCoin2OwnedFree > 0) {
			return CurrentSwapState.OWNS_COIN_2;
		}
		if (amountCoin2OwnedFree == 0 && amountCoin1OwnedFree > 0) {
			return CurrentSwapState.OWNS_COIN_1;
		}
		// If it's less obvious, choose the one with the largest value in BTC
		PriceData pd1 = cache.getLastPriceData(swapDescriptor.getCoin1() + baseCoin);
		PriceData pd2 = cache.getLastPriceData(swapDescriptor.getCoin2() + baseCoin);
		double amtBtc1 = pd1.getPrice() * amountCoin1OwnedFree;
		double amtBtc2 = pd2.getPrice() * amountCoin2OwnedFree;
		if (amtBtc1 > amtBtc2) {
			return CurrentSwapState.OWNS_COIN_1;
		}
		return CurrentSwapState.OWNS_COIN_2;
	}
}
