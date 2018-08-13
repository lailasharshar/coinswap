package com.sharshar.coinswap.components;

import com.binance.api.client.domain.OrderStatus;
import com.binance.api.client.domain.account.NewOrderResponse;
import com.binance.api.client.domain.account.Order;
import com.binance.api.client.domain.account.Trade;
import com.sharshar.coinswap.beans.OwnedAsset;
import com.sharshar.coinswap.beans.PriceData;
import com.sharshar.coinswap.beans.SwapDescriptor;
import com.sharshar.coinswap.beans.Ticker;
import com.sharshar.coinswap.beans.simulation.SimulatorRecord;
import com.sharshar.coinswap.beans.simulation.TradeAction;
import com.sharshar.coinswap.exchanges.AccountService;
import com.sharshar.coinswap.utils.CoinUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
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

	private class CoinValues {
		double sellCoin;
		double sellPrice;
		double buyCoin;
		double buyPrice;
		double commCoin;
	}

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
		UNABLE_TO_UPDATE_COIN_BALANCES,
		NOT_ENOUGH_TO_SELL,
		NOT_ENOUGH_TO_BUY,
		OUT_OF_MONEY_COIN1,
		COIN_IS_BASECOIN, OUT_OF_MONEY_COIN2
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

	private AccountService accountService;

	public SwapExecutor(SwapDescriptor swapDescriptor, AccountService accountService) {
		this.swapDescriptor = swapDescriptor;
		this.accountService = accountService;
		this.currentSwapState = CurrentSwapState.OWNS_COIN_1;
	}

	/**
	 * When we first start the application, we have no historical trends when it comes to mean/std deviation
	 * of the two currencies. Originally, I just had it wait until the cache was filled to a specified number
	 * and started trading after that. No need to do that. Historical data is known (at least hourly). Prefill
	 * the cache with those values.
	 */
	public void backFillData() {
		List<PriceData> pd1 = accountService.getBackfillData(cache.getCacheSize(), swapDescriptor.getCoin1(), swapDescriptor.getBaseCoin());
		cache.bulkLoadData(swapDescriptor.getCoin1() + swapDescriptor.getBaseCoin(), pd1);

		List<PriceData> pd2 = accountService.getBackfillData(cache.getCacheSize(), swapDescriptor.getCoin2(), swapDescriptor.getBaseCoin());
		cache.bulkLoadData(swapDescriptor.getCoin2() + swapDescriptor.getBaseCoin(), pd2);

		List<PriceData> comm = accountService.getBackfillData(cache.getCacheSize(), swapDescriptor.getCommissionCoin(), swapDescriptor.getBaseCoin());
		cache.bulkLoadData(swapDescriptor.getCommissionCoin() + swapDescriptor.getBaseCoin(), comm);
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
		if (swapDescriptor.getBaseCoin() == null || swapDescriptor.getBaseCoin().isEmpty()) {
			return ResponseCode.NO_BASE_COIN_DEFINED;
		}
		if (swapDescriptor.getCommissionCoin() == null || swapDescriptor.getCommissionCoin().isEmpty()) {
			return ResponseCode.NO_COMMISSION_COIN_DEFINED;
		}
		return null;
	}

	private void modifyAmountFree(Ticker ticker, double amount) {
		if (ticker.getTicker().equals(swapDescriptor.getCoin1())) {
			amountCoin1OwnedFree += amount;
		}
		if (ticker.getTicker().equals(swapDescriptor.getCoin2())) {
			amountCoin2OwnedFree += amount;
		}
	}
	/**
	 * Simulate trading to do analytics
	 *
	 * @param amountCoinAToSell - How much of coinA to sell. This is usually all of it
	 * @param coinA             - The coin to sell (without coin base)
	 * @param coinB             - the coin to buy (without coin base)
	 * @param priceData         - the latest price data so we can calculate things like transaction fees and how much of
	 *                          coinB we can buy
	 * @return the response code to the trade. This will almost always be TRANSACTION_SUCCESSFUL unless the component
	 * is not correctly initialized
	 */
	private TradeAction simulateCoinSwap(double amountCoinAToSell, Ticker coinA, Ticker coinB, List<PriceData> priceData) {
		TradeAction action = new TradeAction();
		if (coinB.getTicker().equalsIgnoreCase(swapDescriptor.getBaseCoin()) ||
				coinA.getTicker().equalsIgnoreCase(swapDescriptor.getBaseCoin())) {
			action.setResponseCode(ResponseCode.COIN_IS_BASECOIN);
			return action;
		}
		ResponseCode initErrors = getAnyInitializationErrors();
		if (initErrors != null) {
			action.setResponseCode(initErrors);
			return action;
		}
		CoinValues coinValues = getAdjustmentAmounts(coinA, amountCoinAToSell, coinB, cache.getCommissionTicker(), priceData);

		// If we have nothing to sell :( return
		if (coinValues.sellCoin < coinA.getMinQty()) {
			action.setResponseCode(ResponseCode.NOT_ENOUGH_TO_SELL);
			return action;
		}

		if (coinValues.buyCoin < coinB.getMinQty()) {
			action.setResponseCode(ResponseCode.NOT_ENOUGH_TO_BUY);
			return action;
		}

		modifyAmountFree(coinA, -1 * coinValues.sellCoin);
		modifyAmountFree(coinB, coinValues.buyCoin);

		amountCommissionAssetFree -= coinValues.commCoin;

		SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy HH:mm");
		String actionDate = sdf.format(priceData.get(0).getUpdateTime());
		logger.debug(actionDate + " Sell   : " + coinA.getTickerBase() + " " + String.format("%.6f", coinValues.sellCoin)
				+ " @ " + String.format("%.6f", coinValues.sellPrice) + " (" + String.format("%.6f", (coinValues.sellCoin * coinValues.sellPrice)) + ")");
		logger.debug(actionDate + " Buy    : " + coinB.getTickerBase() + " " + String.format("%.6f", coinValues.buyCoin)
				+ " @ " + String.format("%.6f", coinValues.buyPrice) + " (" + String.format("%.6f", (coinValues.buyCoin * coinValues.buyPrice)) + ")");
		logger.debug("Amount Coin1: " +  String.format("%.6f", amountCoin1OwnedFree) +
				", Amount Coin2: " +  String.format("%.6f", amountCoin2OwnedFree) +
				", Commisison Coin: " +  String.format("%.6f", amountCommissionAssetFree));

		action.setResponseCode(ResponseCode.TRANSACTION_SUCCESSFUL);
		action.setAmountCoin1(amountCoin1OwnedFree);
		action.setAmountCoin2(amountCoin2OwnedFree);
		action.setTradeDate(priceData.get(0).getUpdateTime());
		return action;
	}

	/**
	 * We are "swapping" coins. The original assumption was that we were going to sell all of one and use
	 * those proceeds to buy as much of the second currency as we can. The problem with that is that we can't
	 * always buy in the increments resulting from that or at the necessary volume. For example, if we have
	 * $1000 of coin one, we want to sell it and buy $1000 of coin two, but we can can only buy $500 of coin two,
	 * we want to know how much of coin one needs to be used to buy that $500 of coin two and since it's a smaller
	 * transaction, what the expected transaction fee will be for transferring $500 from coin one to coin two
	 *
	 * @param tickerToSell - the ticker we want to sell
	 * @param amountToSell - the amount we want to sell of coin 1
	 * @param tickerToBuy - the ticker we want to buy
	 * @param commissionCoin - the commission coin
	 * @param priceData - the latest price data to perform conversions between the three tickers
	 * @return the corrected values to sell, buy and estimated commissions
	 */
	private CoinValues getAdjustmentAmounts(Ticker tickerToSell, double amountToSell, Ticker tickerToBuy,
										   Ticker commissionCoin, List<PriceData> priceData) {
		double sellPrice = CoinUtils.getPrice(tickerToSell.getTickerBase(), priceData);
		double buyPrice = CoinUtils.getPrice(tickerToBuy.getTickerBase(), priceData);
		double coinToSellAmount = correctedAmount(tickerToSell, amountToSell, swapDescriptor.getMaxPercentVolume());
		double amountCoinToBuy = (sellPrice * amountToSell) / buyPrice;
		double correctedAmountCoinToBuy = correctedAmount(tickerToBuy, amountCoinToBuy, swapDescriptor.getMaxPercentVolume());
		if (correctedAmountCoinToBuy < amountCoinToBuy) {
			double refundOfBuy = amountCoinToBuy - correctedAmountCoinToBuy;
			double refundOfSell = (buyPrice * refundOfBuy) / sellPrice;
			coinToSellAmount = correctedAmount(tickerToSell, amountToSell - refundOfSell, swapDescriptor.getMaxPercentVolume());
		}
		CoinValues cv = new CoinValues();
		cv.buyCoin = correctedAmountCoinToBuy;
		cv.buyPrice = buyPrice;
		cv.sellPrice = sellPrice;
		cv.sellCoin = coinToSellAmount;

		double priceForCommissionCoin = CoinUtils.getPrice(commissionCoin.getTickerBase(), priceData);
		double transFee = accountService.getDefaultTransactionFee();
		cv.commCoin = (cv.sellCoin * sellPrice * transFee) / priceForCommissionCoin +
				(cv.buyCoin * buyPrice * transFee) / priceForCommissionCoin;

		return cv;
	}

	/**
	 * Perform a coin swap. First it sells the amount specifies. It assumes you take the entire amount of the sold
	 * proceeds to buy the other coin.
	 *
	 * @param amountCoinAToSell - The amount of coinA to sell
	 * @param coinA             - The coin to sell (without coin base)
	 * @param coinB             - the coin to buy (without coin base)
	 * @param priceData         - the latest price data so we can calculate things like transaction fees and how much of
	 *                          coinB we can buy
	 * @return the response code to the trade
	 */
	private TradeAction coinSwap(double amountCoinAToSell, Ticker coinA, Ticker coinB, List<PriceData> priceData) {
		System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!! Live swap !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
		if (true) {
			// DON'T DO IT UNTIL WE'RE READY
			return null;
		}
		TradeAction action = new TradeAction();
		if (coinB.getTicker().equalsIgnoreCase(swapDescriptor.getBaseCoin()) ||
				coinA.getTicker().equalsIgnoreCase(swapDescriptor.getBaseCoin())) {
			action.setResponseCode(ResponseCode.COIN_IS_BASECOIN);
			return action;
		}
		ResponseCode initErrors = getAnyInitializationErrors();
		if (initErrors != null) {
			action.setResponseCode(initErrors);
			return action;
		}
		CoinValues coinValues = getAdjustmentAmounts(coinA, amountCoinAToSell, coinB, cache.getCommissionTicker(), priceData);

		// If we have nothing to sell :( return
		if (coinValues.sellCoin < coinA.getMinQty()) {
			action.setResponseCode(ResponseCode.NOT_ENOUGH_TO_SELL);
			return action;
		}

		if (coinValues.buyCoin < coinB.getMinQty()) {
			action.setResponseCode(ResponseCode.NOT_ENOUGH_TO_BUY);
			return action;
		}

		if (coinValues.commCoin > 1.2 * amountCommissionAssetFree) {
			action.setResponseCode(ResponseCode.NOT_ENOUGH_COMMISSION_CURRENCY);
			return action;
		}

		/* first we sell */

		ResponseCode sellResponse = sellCoin(coinA, coinValues.sellCoin);
		if (sellResponse == ResponseCode.SELL_ORDER_FILLED) {
			logger.info("Sell order filled for: " + coinA.getTickerBase());
			if (!loadBalances()) {
				action.setResponseCode(ResponseCode.UNABLE_TO_UPDATE_COIN_BALANCES);
				return action;
			}
		} else {
			action.setResponseCode(sellResponse);
			return action;
		}

		/* Then we buy */

		ResponseCode buyResponse = buyCoin(coinB, coinValues.buyCoin);
		if (buyResponse == ResponseCode.BUY_ORDER_FILLED) {
			logger.info("Buy order filled for: " + coinB.getTickerBase());
			if (!loadBalances()) {
				action.setResponseCode(ResponseCode.UNABLE_TO_UPDATE_COIN_BALANCES);
				return action;
			}
		} else {
			action.setResponseCode(buyResponse);
			return action;
		}
		action.setResponseCode(ResponseCode.TRANSACTION_SUCCESSFUL);
		action.setAmountCoin1(amountCoin1OwnedFree);
		action.setAmountCoin2(amountCoin2OwnedFree);
		action.setTradeDate(priceData.get(0).getUpdateTime());
		return action;
	}

	/**
	 * Given the amount of a coin we wish to buy/sell, calculate the amount that is
	 * allowed. We need to make sure we don't swamp the exchange with large orders
	 * for small coins and we do it in an allowed increments
	 *
	 * @param coin - the coin to buy/sell
	 * @param amount - the amount we want to buy/sell
	 * @param maxVolume - the max percent of daily volume we are willing to go up to
	 * @return the corrected amount
	 */
	private double correctedAmount(Ticker coin, double amount, double maxVolume) {
		if (coin.getTicker().equalsIgnoreCase(coin.getBase())) {
			return amount;
		}

		// Let's be hopeful
		double amountToTransact = amount;
		if (amountToTransact > coin.getMaxQty() && coin.getMaxQty() > 0) {
			amountToTransact = coin.getMaxQty();
		}
		if (amount < coin.getMinQty() && coin.getMinQty() > 0) {
			return 0;
		}
		amountToTransact = correctForVolume(coin, amountToTransact, maxVolume);
		return correctForStep(coin.getStepSize(), amountToTransact);
	}

	/**
	 * We don't want to trade too many at a time. You can't swamp the market and take over the
	 * currency (Oh, to have such problems). We will only use a percentage of the daily volume
	 *
	 * @param coin - The coin we want to buy/sell
	 * @param amountToTransact - The amount we wish to buy/sell
	 * @param amountOfVolume - The percentage of volume we are willing to go up to
	 * @return the amount we can buy/sell based on our volume restrictions
	 */
	private double correctForVolume(Ticker coin, double amountToTransact, double amountOfVolume) {
		Double lastVolume = coin.getLastVolume();
		if (lastVolume == null || lastVolume == 0) {
			return amountToTransact;
		}
		double maxAmount = amountOfVolume * lastVolume;
		if (amountToTransact > maxAmount) {
			return maxAmount;
		}
		return amountToTransact;
	}

	/**
	 * When buying coins, you are required to have a defined precision. For example, smaller coins, you have to
	 * buy whole coins, you can't buy fractions. Some, you can buy fractions, but only to a certain level. For example,
	 * ETHBTC can only be buy in increments of 0.001 of a coin. If you want to buy 10.3486534545 coins,
	 * we need to correct that to 10.348. We always take the floor value because we usually don't have more than
	 * the amount we're submitting.
	 *
	 * @param stepSize - The coin increment information (currently, they are: 0.00000100, 0.00001000, 0.00100000,
	 *                    0.01000000, 0.10000000, 1.00000000
	 * @param desiredAmountToBuy - What we want to buy
	 * @return the corrected value
	 */
	public static double correctForStep(double stepSize, double desiredAmountToBuy) {
		int numPrecision = 0;
		while (stepSize != 1) {
			stepSize *= 10;
			numPrecision++;
		}
		if (numPrecision == 0) {
			// we have to buy whole numbers
			return Math.floor(desiredAmountToBuy);
		}
		// May be a better way to do this, but convert it to a string with the correct precision, then back to a double
		StringBuilder precisionNumber = new StringBuilder();
		precisionNumber.append("#.");
		for (int i=0; i<numPrecision; i++) {
			precisionNumber.append("#");
		}
		DecimalFormat df = new DecimalFormat(precisionNumber.toString());
		df.setRoundingMode(RoundingMode.FLOOR);
		String result = df.format(desiredAmountToBuy);
		return Double.parseDouble(result);
	}

	/**
	 * Swaps coin1 to coin2
	 *
	 * @param priceData - the list of price data to estimate fees
	 * @param simulate  - if we want to do it for real, or just simulate it
	 * @return the response code
	 */
	public TradeAction swapCoin1ToCoin2(List<PriceData> priceData, boolean simulate) {
		TradeAction action;
		if (simulate) {
			action = simulateCoinSwap(amountCoin1OwnedFree, cache.getTicker1(), cache.getTicker2(), priceData);
			action.setDirection(SimulatorRecord.TradeDirection.BUY_COIN_2);
		} else {
			action = coinSwap(amountCoin1OwnedFree, cache.getTicker1(), cache.getTicker2(), priceData);
			action.setDirection(SimulatorRecord.TradeDirection.BUY_COIN_2);
		}
		if (action.getResponseCode() == ResponseCode.TRANSACTION_SUCCESSFUL) {
			currentSwapState = CurrentSwapState.OWNS_COIN_2;
		}
		return action;
	}

	/**
	 * Swaps coin2 to coin1
	 *
	 * @param priceData - the list of price data to estimate fees
	 * @param simulate  - if we want to do it for real, or just simulate it
	 * @return the response code
	 */
	public TradeAction swapCoin2ToCoin1(List<PriceData> priceData, boolean simulate) {
		TradeAction action;
		if (simulate) {
			action = simulateCoinSwap(amountCoin2OwnedFree, cache.getTicker2(), cache.getTicker1(), priceData);
			action.setDirection(SimulatorRecord.TradeDirection.BUY_COIN_1);
		} else {
			action = coinSwap(amountCoin2OwnedFree, cache.getTicker2(), cache.getTicker1(), priceData);
			action.setDirection(SimulatorRecord.TradeDirection.BUY_COIN_1);
		}
		if (action.getResponseCode() == ResponseCode.TRANSACTION_SUCCESSFUL) {
			currentSwapState = CurrentSwapState.OWNS_COIN_1;
		}
		return action;
	}

	/**
	 * If we are going to simulate swap, you need to seed with imaginary money
	 *
	 * @param inBaseCoin - the amount should be in the specified base coin
	 */
	public void seedMeMoney(double inBaseCoin) {
		if (swapDescriptor.getCoin1().equalsIgnoreCase(swapDescriptor.getBaseCoin())) {
			this.amountCoin1OwnedFree = inBaseCoin;
			return;
		}
		PriceData priceData = cache.getLastPriceData(swapDescriptor.getCoin1() + swapDescriptor.getBaseCoin());
		if (priceData == null) {
			return;
		}
		double lastTickerPrice = priceData.getPrice();
		this.amountCoin1OwnedFree = correctedAmount(cache.getTicker1(), (inBaseCoin/lastTickerPrice), swapDescriptor.getMaxPercentVolume());
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
		OwnedAsset ownedAssetBaseAsset = CoinUtils.getAssetValue(swapDescriptor.getBaseCoin(), assets);
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
	private double getCommissionNeeded(Ticker coin, double amountCoin, double defaultTransactionFee,
									   List<PriceData> priceData) {

		double priceForCoin = CoinUtils.getPrice(coin.getTickerBase(), priceData);
		double priceForCommissionCoin = CoinUtils.getPrice(swapDescriptor.getCommissionCoin() + swapDescriptor.getBaseCoin(), priceData);

		// Commission in base currency
		double commission = amountCoin * priceForCoin * defaultTransactionFee;
		return commission / priceForCommissionCoin;
	}

	/**
	 * Create a market order to sell a coin with the exchange
	 *
	 * @param coin             - the coin to sell
	 * @param amountCoinToSell - the amount to sell
	 * @return the response code to the sell
	 */
	private ResponseCode sellCoin(Ticker coin, double amountCoinToSell) {
		logger.info("Selling " + amountCoinToSell + " of " + coin.getTickerBase());
		long startTime = System.currentTimeMillis();
		NewOrderResponse response = accountService.createSellMarketOrder(coin.getTickerBase(), amountCoinToSell);
		if (response == null) {
			return ResponseCode.SELL_ORDER_ERROR;
		}

		// Retrieve the transaction Id
		String clientId = response.getClientOrderId();
		Order order = waitForResponse(coin.getTickerBase(), clientId);

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
	private ResponseCode buyCoin(Ticker coin, double amountToBuy) {
		logger.info("Buying " + amountToBuy + " of " + coin.getTickerBase());
		long startTime = System.currentTimeMillis();
		NewOrderResponse response = accountService.createBuyMarketOrder(coin.getTickerBase(), amountToBuy);
		if (response == null) {
			return ResponseCode.BUY_ORDER_ERROR;
		}

		// Retrieve the transaction Id
		String clientId = response.getClientOrderId();
		// Wait for the order to be filled
		Order order = waitForResponse(coin.getTickerBase(), clientId);

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
	 * @param ticker   - the coin/base coin to check
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
		// If it's less obvious, choose the one with the largest value in base coin
		PriceData pd1 = cache.getLastPriceData(swapDescriptor.getCoin1() + swapDescriptor.getBaseCoin());
		PriceData pd2 = cache.getLastPriceData(swapDescriptor.getCoin2() + swapDescriptor.getBaseCoin());
		double amt1 = pd1.getPrice() * amountCoin1OwnedFree;
		double amt2 = pd2.getPrice() * amountCoin2OwnedFree;
		if (amt1 > amt2) {
			return CurrentSwapState.OWNS_COIN_1;
		}
		return CurrentSwapState.OWNS_COIN_2;
	}
}
