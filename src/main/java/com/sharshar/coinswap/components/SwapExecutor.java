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
import com.sharshar.coinswap.services.HistoricalPriceCache;
import com.sharshar.coinswap.services.OrderHistoryService;
import com.sharshar.coinswap.services.PurchaseAdvisor;
import com.sharshar.coinswap.services.SwapService;
import com.sharshar.coinswap.utils.CoinUtils;
import com.sharshar.coinswap.utils.ScratchConstants;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import static com.sharshar.coinswap.components.SwapExecutor.ResponseCode.*;

/**
 * Encapsulates a coin swap service. This enables to instantiate more than one in this process
 * <p>
 * Created by lsharshar on 7/17/2018.
 */
@Component
@Scope("prototype")
public class SwapExecutor {

	public class CoinValues {
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
		BUY_ORDER_MOSTLY_FILLED,
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

	private static final Logger logger = LogManager.getLogger();

	@Autowired
	private SwapService swapService;

	@Autowired
	private OrderHistoryService orderHistoryService;

	private ExchangeCache cache;
	private SwapDescriptor swapDescriptor;
	private double amountCoin1OwnedFree;
	private double amountCoin2OwnedFree;
	private double amountBaseCoinOwnedFree;
	private double amountCommissionAssetFree;
	private boolean inSwap;
	private double amountExecuted;
	private double amountSelling;
	private double amountBuying;

	private AccountService accountService;

	@Autowired
	private HistoricalPriceCache historicalPriceCache;

	public SwapExecutor(SwapDescriptor swapDescriptor, AccountService accountService) {
		this.swapDescriptor = swapDescriptor;
		this.accountService = accountService;
		// This is not a simulation, so load the coin balances
		if (swapDescriptor != null && (swapDescriptor.getSimulate() != null && !swapDescriptor.getSimulate())) {
			this.loadBalances();
		}
	}

	public double getAmountExecuted() {
		return amountExecuted;
	}

	public double getAmountSelling() {
		return amountSelling;
	}

	public double getAmountBuying() {
		return amountBuying;
	}

	public SwapDescriptor getSwapDescriptor() {
		return SwapDescriptor.clone(swapDescriptor);
	}

	public SwapExecutor setCache(ExchangeCache cache) {
		this.cache = cache;
		return this;
	}

	public boolean isInSwap() {
		return inSwap;
	}

	public boolean isSimulate() {
		// default to true
		if (swapDescriptor == null || swapDescriptor.getSimulate() == null) {
			return true;
		}
		return swapDescriptor.getSimulate();
	}

	public List<PriceData> getBaseData(List<PriceData> sourceData, String baseCoin) {
		List<PriceData> pdResult = new ArrayList<>();
		for (PriceData pd : sourceData) {
			PriceData newPd = new PriceData().setExchange(pd.getExchange()).setPrice(1.0)
					.setTicker(baseCoin + baseCoin).setUpdateTime(pd.getUpdateTime());
			pdResult.add(newPd);
		}
		return pdResult;
	}

	/**
	 * When we first start the application, we have no historical trends when it comes to mean/std deviation
	 * of the two currencies. Originally, I just had it wait until the cache was filled to a specified number
	 * and started trading after that. No need to do that. Historical data is known (at least hourly). Prefill
	 * the cache with those values.
	 */
	public void backFillData() {
		List<PriceData> comm = historicalPriceCache.getHistoricalData(swapDescriptor.getCommissionCoin(), swapDescriptor.getBaseCoin(), cache.getCacheSize());
		List<PriceData> baseData = getBaseData(comm, swapDescriptor.getBaseCoin());
		List<PriceData> pd1 = null;
		if (swapDescriptor.getCoin1().equalsIgnoreCase(swapDescriptor.getBaseCoin())) {
			pd1 = baseData;
		} else {
			pd1 = historicalPriceCache.getHistoricalData(swapDescriptor.getCoin1(), swapDescriptor.getBaseCoin(), cache.getCacheSize());
		}
		cache.bulkLoadData(swapDescriptor.getCoin1() + swapDescriptor.getBaseCoin(), pd1);

		List<PriceData> pd2 = null;
		if (swapDescriptor.getCoin2().equalsIgnoreCase(swapDescriptor.getBaseCoin())) {
			pd2 = baseData;
		} else {
			pd2 = historicalPriceCache.getHistoricalData(swapDescriptor.getCoin2(), swapDescriptor.getBaseCoin(), cache.getCacheSize());
		}
		cache.bulkLoadData(swapDescriptor.getCoin2() + swapDescriptor.getBaseCoin(), pd2);
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
		if (ticker.getAsset().equals(swapDescriptor.getCoin1())) {
			amountCoin1OwnedFree += amount;
		}
		if (ticker.getAsset().equals(swapDescriptor.getCoin2())) {
			amountCoin2OwnedFree += amount;
		}
	}

	private TradeAction doBaseTrade(List<OwnedAsset> ownedAssets, double amountCoinToSell, Ticker coinA, Ticker coinB,
									List<PriceData> priceData) {
		TradeAction action = new TradeAction();
		double transFee = accountService.getDefaultTransactionFee();
		double priceForCommissionCoin = CoinUtils.getPrice(swapDescriptor.getCommissionCoin() + swapDescriptor.getBaseCoin(),
				priceData);
		if (coinA.getAsset().equalsIgnoreCase(coinA.getBase())) {
			// first coin is base coin - use it to buy second coin
			action.setPriceCoin1(1.0);
			double buyPrice = CoinUtils.getPrice(coinB.getAssetAndBase(), priceData);
			action.setPriceCoin2(buyPrice);
			double coinToBuyAmount = correctedAmount(coinB, amountCoinToSell / buyPrice,
					swapDescriptor.getMaxPercentVolume(), false, false, ownedAssets, priceData);
			if (coinToBuyAmount < coinB.getMinQty()) {
				action.setResponseCode(ResponseCode.NOT_ENOUGH_TO_SELL);
				return action;
			}
			double commCoin = (coinToBuyAmount * buyPrice * transFee) / priceForCommissionCoin;
			if (commCoin > 1.2 * amountCommissionAssetFree) {
				action.setResponseCode(ResponseCode.NOT_ENOUGH_COMMISSION_CURRENCY);
				return action;
			}
			ResponseCode buyResponse = breakUpAndBuyCoin(coinB, buyPrice,
					accountService.getMinAmountAtTime()/buyPrice,
					accountService.getMaxAmountAtTime()/buyPrice,
					coinToBuyAmount,
					coinToBuyAmount);
			if (buyResponse == ResponseCode.BUY_ORDER_FILLED || buyResponse == ResponseCode.BUY_ORDER_MOSTLY_FILLED) {
				logger.info("Buy order filled for: " + coinB.getAssetAndBase());
			} else {
				action.setResponseCode(buyResponse);
				return action;
			}
			action.setResponseCode(ResponseCode.TRANSACTION_SUCCESSFUL);
		} else if (coinB.getAsset().equalsIgnoreCase(coinB.getBase())) {
			// second coin is base coin - sell first coin
			action.setPriceCoin2(1.0);
			double sellPrice = CoinUtils.getPrice(coinA.getAssetAndBase(), priceData);
			action.setPriceCoin1(sellPrice);
			double coinToSellAmount = correctedAmount(coinA, amountCoinToSell, swapDescriptor.getMaxPercentVolume(),
					false, false, ownedAssets, priceData);
			if (coinToSellAmount < coinA.getMinQty()) {
				action.setResponseCode(ResponseCode.NOT_ENOUGH_TO_BUY);
				return action;
			}
			double commCoin = (coinToSellAmount * sellPrice * transFee) / priceForCommissionCoin;
			if (commCoin > 1.2 * amountCommissionAssetFree) {
				action.setResponseCode(ResponseCode.NOT_ENOUGH_COMMISSION_CURRENCY);
				return action;
			}
			ResponseCode sellResponse = breakUpAndSellCoin(coinA, sellPrice, coinToSellAmount);
			if (sellResponse == ResponseCode.SELL_ORDER_FILLED) {
				logger.info("Sell order filled for: " + coinA.getAssetAndBase());
			} else {
				action.setResponseCode(sellResponse);
				return action;
			}
		}
		if (!loadBalances()) {
			action.setResponseCode(ResponseCode.UNABLE_TO_UPDATE_COIN_BALANCES);
			return action;
		}
		action.setResponseCode(ResponseCode.TRANSACTION_SUCCESSFUL);
		action.setAmountCoin1(amountCoin1OwnedFree);
		action.setAmountCoin2(amountCoin2OwnedFree);
		action.setTradeDate(priceData.get(0).getUpdateTime());
		return action;
	}

	private TradeAction simulateBaseTrade(double amountCoinToSell, Ticker coinA, Ticker coinB, List<PriceData> priceData) {
		TradeAction action = new TradeAction();
		double transFee = accountService.getDefaultTransactionFee();
		double priceForCommissionCoin = CoinUtils.getPrice(swapDescriptor.getCommissionCoin() + swapDescriptor.getBaseCoin(),
				priceData);
		if (coinA.getAsset().equalsIgnoreCase(coinA.getBase())) {
			// first coin is base coin - use it to buy second coin
			double buyPrice = CoinUtils.getPrice(coinB.getAssetAndBase(), priceData);
			double coinToBuyAmount = correctedAmount(coinB, amountCoinToSell/buyPrice,
					swapDescriptor.getMaxPercentVolume(), true, true, null, priceData);
			if (coinToBuyAmount < coinB.getMinQty()) {
				action.setResponseCode(ResponseCode.NOT_ENOUGH_TO_SELL);
				return action;
			}
			double actualAmountToSell = coinToBuyAmount * buyPrice;
			modifyAmountFree(coinA, -1 * actualAmountToSell);
			modifyAmountFree(coinB, coinToBuyAmount);
			amountCommissionAssetFree -=  (coinToBuyAmount * buyPrice * transFee) / priceForCommissionCoin;
			action.setPriceCoin1(1.0);
			action.setPriceCoin2(buyPrice);
		} else if (coinB.getAsset().equalsIgnoreCase(coinB.getBase())) {
			// second coin is base coin - sell first coin
			double sellPrice = CoinUtils.getPrice(coinA.getAssetAndBase(), priceData);
			double coinToSellAmount = correctedAmount(coinA, amountCoinToSell, swapDescriptor.getMaxPercentVolume(),
					true, false, null, priceData);
			if (coinToSellAmount < coinA.getMinQty()) {
				action.setResponseCode(ResponseCode.NOT_ENOUGH_TO_BUY);
				return action;
			}
			modifyAmountFree(coinB, coinToSellAmount * sellPrice);
			modifyAmountFree(coinA, -1 * coinToSellAmount);
			amountCommissionAssetFree -=  (coinToSellAmount * sellPrice * transFee) / priceForCommissionCoin;
			action.setPriceCoin1(sellPrice);
			action.setPriceCoin2(1.0);
		}

		action.setResponseCode(ResponseCode.TRANSACTION_SUCCESSFUL);
		action.setAmountCoin1(amountCoin1OwnedFree);
		action.setAmountCoin2(amountCoin2OwnedFree);
		action.setTradeDate(priceData.get(0).getUpdateTime());
		return action;
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
		ResponseCode initErrors = getAnyInitializationErrors();
		if (initErrors != null) {
			action.setResponseCode(initErrors);
			return action;
		}
		if (swapDescriptor.hasBaseCoin()) {
			return simulateBaseTrade(amountCoinAToSell, coinA, coinB, priceData);
		}
		CoinValues coinValues = getAdjustmentAmounts(null, coinA, amountCoinAToSell, coinB,
				cache.getCommissionTicker(), priceData, true);
		coinValues = getPenaltyBookPrices(coinValues);

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
		logger.info(actionDate + " Sell   : " + coinA.getAssetAndBase() + " " + String.format("%.6f", coinValues.sellCoin)
				+ " @ " + String.format("%.6f", coinValues.sellPrice) + " (" + String.format("%.6f", (coinValues.sellCoin * coinValues.sellPrice)) + ")");
		logger.info(actionDate + " Buy    : " + coinB.getAssetAndBase() + " " + String.format("%.6f", coinValues.buyCoin)
				+ " @ " + String.format("%.6f", coinValues.buyPrice) + " (" + String.format("%.6f", (coinValues.buyCoin * coinValues.buyPrice)) + ")");
		logger.info("Amount Coin1: " +  String.format("%.6f", amountCoin1OwnedFree) +
				", Amount Coin2: " +  String.format("%.6f", amountCoin2OwnedFree) +
				", Commisison Coin: " +  String.format("%.6f", amountCommissionAssetFree));

		action.setResponseCode(ResponseCode.TRANSACTION_SUCCESSFUL);
		action.setAmountCoin1(amountCoin1OwnedFree);
		action.setAmountCoin2(amountCoin2OwnedFree);
		action.setPriceCoin1(coinValues.sellPrice);
		action.setPriceCoin2(coinValues.buyPrice);
		action.setTradeDate(priceData.get(0).getUpdateTime());
		return action;
	}

	/**
	 * In reality, when we sell or buy, we will have to have to receive lower or pay higher amounts
	 * to fill our order - initial tests assume around 2%
	 *
	 * @param coinValues - our initial values
	 * @return adapted values
	 */
	private CoinValues getPenaltyBookPrices(CoinValues coinValues) {
		coinValues.sellPrice = coinValues.sellPrice - (coinValues.sellPrice * 0.05);
		coinValues.buyPrice = coinValues.buyPrice * 1.01;
		return coinValues;
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
	public CoinValues getAdjustmentAmounts(List<OwnedAsset> ownedAssets, Ticker tickerToSell, double amountToSell,
											Ticker tickerToBuy, Ticker commissionCoin, List<PriceData> priceData,
											boolean simulate) {
		double sellPrice = CoinUtils.getPrice(tickerToSell.getAssetAndBase(), priceData);
		double buyPrice = CoinUtils.getPrice(tickerToBuy.getAssetAndBase(), priceData);
		double coinToSellAmount = correctedAmount(tickerToSell, amountToSell, swapDescriptor.getMaxPercentVolume(),
				simulate, false, ownedAssets, priceData);
		double amountCoinToBuy = (sellPrice * amountToSell) / buyPrice;
		double correctedAmountCoinToBuy = correctedAmount(tickerToBuy, amountCoinToBuy,
				swapDescriptor.getMaxPercentVolume(), simulate, true, ownedAssets, priceData);
		if (correctedAmountCoinToBuy < amountCoinToBuy) {
			double refundOfBuy = amountCoinToBuy - correctedAmountCoinToBuy;
			double refundOfSell = (buyPrice * refundOfBuy) / sellPrice;
			coinToSellAmount = correctedAmount(tickerToSell, amountToSell - refundOfSell,
					swapDescriptor.getMaxPercentVolume(), simulate, false, ownedAssets, priceData);
		}
		CoinValues cv = new CoinValues();
		if (simulate) {
			cv.buyCoin = correctedAmountCoinToBuy;
		} else {
			cv.buyCoin = PurchaseAdvisor.getAmountToBuy(ownedAssets, swapDescriptor, tickerToBuy.getAsset(),
					tickerToBuy.getBase(), correctedAmountCoinToBuy, priceData, swapService.getSwaps());
		}
		cv.buyPrice = buyPrice;
		cv.sellPrice = sellPrice;
		cv.sellCoin = coinToSellAmount;

		double priceForCommissionCoin = CoinUtils.getPrice(commissionCoin.getAssetAndBase(), priceData);
		double transFee = accountService.getDefaultTransactionFee();
		cv.commCoin = (cv.sellCoin * sellPrice * transFee) / priceForCommissionCoin +
				(cv.buyCoin * buyPrice * transFee) / priceForCommissionCoin;

		return cv;
	}

	private TradeAction coinSwap(double amountCoinAToSell, Ticker coinA, Ticker coinB, List<PriceData> priceData) {
		return coinSwap(amountCoinAToSell, coinA, coinB, priceData, false);
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
	public TradeAction coinSwap(double amountCoinAToSell, Ticker coinA, Ticker coinB, List<PriceData> priceData, boolean test) {
		if (!test) {
			System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!! Live swap !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
		}

		List<OwnedAsset> ownedAssets = accountService.getAllBalances();
		TradeAction action = new TradeAction();
		if (coinB.getAsset().equalsIgnoreCase(swapDescriptor.getBaseCoin()) ||
				coinA.getAsset().equalsIgnoreCase(swapDescriptor.getBaseCoin())) {
			return doBaseTrade(ownedAssets, amountCoinAToSell, coinA, coinB, priceData);
		}
		ResponseCode initErrors = getAnyInitializationErrors();
		if (initErrors != null) {
			action.setResponseCode(initErrors);
			return action;
		}
		CoinValues coinValues = getAdjustmentAmounts(ownedAssets, coinA, amountCoinAToSell, coinB,
				cache.getCommissionTicker(), priceData, false);

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

		ResponseCode sellResponse = breakUpAndSellCoin(coinA, coinValues.sellPrice, coinValues.sellCoin);
		if (sellResponse == ResponseCode.SELL_ORDER_FILLED) {
			logger.info("Sell order filled for: " + coinA.getAssetAndBase());
			if (!loadBalances()) {
				action.setResponseCode(ResponseCode.UNABLE_TO_UPDATE_COIN_BALANCES);
				return action;
			}
		} else {
			action.setResponseCode(sellResponse);
			return action;
		}

		/* Then we buy */

		ResponseCode buyResponse = breakUpAndBuyCoin(coinB, coinValues.buyPrice,
				accountService.getMinAmountAtTime()/coinValues.buyPrice,
				accountService.getMaxAmountAtTime()/coinValues.buyPrice,
				coinValues.buyCoin,
				coinValues.buyCoin);
		if (buyResponse == ResponseCode.BUY_ORDER_FILLED || buyResponse == ResponseCode.BUY_ORDER_MOSTLY_FILLED) {
			logger.info("Buy order filled for: " + coinB.getAssetAndBase());
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
		action.setPriceCoin1(coinValues.sellPrice);
		action.setPriceCoin1(coinValues.buyPrice);
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
	private double correctedAmount(Ticker coin, double amount, Double maxVolume, boolean simulate, boolean buy,
								   List<OwnedAsset> ownedAssets, List<PriceData> priceData) {
		if (coin.getAsset().equalsIgnoreCase(coin.getBase())) {
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
		if (!simulate && buy) {
			amountToTransact = PurchaseAdvisor.getAmountToBuy(ownedAssets, swapDescriptor, coin.getAsset(),
					coin.getBase(), amountToTransact, priceData, swapService.getSwaps());
		}
		amountToTransact = 0.95 * correctForVolume(swapDescriptor, coin, amountToTransact, maxVolume);
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
	public static double correctForVolume(SwapDescriptor sd, Ticker coin, double amountToTransact, Double amountOfVolume) {
		double lastVolume = 0;
		if (sd.getCoin1().equalsIgnoreCase(coin.getAsset())) {
			lastVolume = sd.getLastVolume1();
		}
		if (sd.getCoin2().equalsIgnoreCase(coin.getAsset())) {
			lastVolume = sd.getLastVolume2();
		}
		if (lastVolume == 0) {
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
		inSwap = true;
		TradeAction action;
		try {
			if (simulate) {
				action = simulateCoinSwap(amountCoin1OwnedFree, cache.getTicker1(), cache.getTicker2(), priceData);
				action.setDirection(SimulatorRecord.TradeDirection.BUY_COIN_2);
			} else {
				action = coinSwap(amountCoin1OwnedFree, cache.getTicker1(), cache.getTicker2(), priceData);
				action.setDirection(SimulatorRecord.TradeDirection.BUY_COIN_2);
			}
			if (action.getResponseCode() == ResponseCode.TRANSACTION_SUCCESSFUL) {
				swapService.updateCoinOwned(swapDescriptor, ScratchConstants.CurrentSwapState.OWNS_COIN_2);
			}
			inSwap = false;
			return action;
		} catch (Exception ex) {
			// We just want to make sure we are not constantly "in the swap" status if we error out
			logger.error(ex.getMessage());
			inSwap = false;
			throw ex;
		}
	}

	/**
	 * Swaps coin2 to coin1
	 *
	 * @param priceData - the list of price data to estimate fees
	 * @param simulate  - if we want to do it for real, or just simulate it
	 * @return the response code
	 */
	public TradeAction swapCoin2ToCoin1(List<PriceData> priceData, boolean simulate) {
		inSwap = true;
		TradeAction action;
		try {
			if (simulate) {
				action = simulateCoinSwap(amountCoin2OwnedFree, cache.getTicker2(), cache.getTicker1(), priceData);
				action.setDirection(SimulatorRecord.TradeDirection.BUY_COIN_1);
			} else {
				action = coinSwap(amountCoin2OwnedFree, cache.getTicker2(), cache.getTicker1(), priceData);
				action.setDirection(SimulatorRecord.TradeDirection.BUY_COIN_1);
			}
			if (action.getResponseCode() == ResponseCode.TRANSACTION_SUCCESSFUL) {
				swapService.updateCoinOwned(swapDescriptor, ScratchConstants.CurrentSwapState.OWNS_COIN_1);
			}
			inSwap = false;
			return action;
		} catch (Exception ex) {
			// We just want to make sure we are not constantly "in the swap" status if we error out
			inSwap = false;
			logger.error(ex.getMessage());
			throw ex;
		}
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
		this.amountCoin1OwnedFree = correctedAmount(cache.getTicker1(), (inBaseCoin/lastTickerPrice),
				swapDescriptor.getMaxPercentVolume(), true, true, null, null);
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
	public double getCommissionNeeded(Ticker coin, double amountCoin, double defaultTransactionFee,
									   List<PriceData> priceData) {

		double priceForCoin = CoinUtils.getPrice(coin.getAssetAndBase(), priceData);
		double priceForCommissionCoin = CoinUtils.getPrice(swapDescriptor.getCommissionCoin() + swapDescriptor.getBaseCoin(), priceData);

		// Commission in base currency
		double commission = amountCoin * priceForCoin * defaultTransactionFee;
		// Commission in commission currency
		return commission / priceForCommissionCoin;
	}

	/**
	 * Create a market order to sell a coin with the exchange
	 *
	 * @param coin             - the coin to sell
	 * @param amountCoinToSell - the amount to sell
	 * @return the response code to the sell
	 */
	public ResponseCode sellCoin(Ticker coin, double amountCoinToSell) {
		logger.info("Selling " + amountCoinToSell + " of " + coin.getAssetAndBase());
		long startTime = System.currentTimeMillis();
		NewOrderResponse response = accountService.createSellMarketOrder(coin.getAssetAndBase(), amountCoinToSell,
				swapDescriptor.getTableId() == null ? 0 : swapDescriptor.getTableId());
		if (response == null) {
			return SELL_ORDER_ERROR;
		}

		// Retrieve the transaction Id
		String clientId = response.getClientOrderId();
		Order order = waitForResponse(coin.getAssetAndBase(), clientId);
		if (!orderHistoryService.updateWithPrice(order.getOrderId())) {
			logger.error("Unable to determine the order price of order " + clientId);
		}

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
	 * Because we can't buy fast enough for the current market price, let's find reasonable amounts to
	 * buy at a time. The account service has a price in the base coin. For example, if 0.03 BTC is reasonable,
	 * the price of the coin is 0.001 of BTC, then the amount to buy should not be more than 3.
	 *
	 * @param stepSize - The minimum increment to buy/sell in.
	 * @param price - the price relative to the base coin
	 * @param totalAmountToBuy - the total amount you want to buy
	 * @return the list of broken up pieces
	 */
	public List<Double> breakItUp(double stepSize, double minAmountAtTime, double maxAmountAtTime, double price, double totalAmountToBuy) {
		List<Double> amounts = new ArrayList<>();
		if (stepSize == 0 || totalAmountToBuy == 0) {
			return amounts;
		}
		if (minAmountAtTime > totalAmountToBuy) {
			return amounts;
		}
		double reasonableAmountAtTime = correctForStep(stepSize, maxAmountAtTime);
		if (reasonableAmountAtTime > totalAmountToBuy) {
			amounts.add(totalAmountToBuy);
			return amounts;
		}
		double amountLeft = totalAmountToBuy;
		while (amountLeft > 0) {
			double prevAmount = amountLeft;
			amountLeft -= reasonableAmountAtTime;
			if (amountLeft < 0) {
				if ((prevAmount / reasonableAmountAtTime) < 0.1){
					amounts.set(amounts.size() - 1, amounts.get(amounts.size() - 1) + prevAmount);
				} else {
					amounts.add(prevAmount);
				}
			} else {
				amounts.add(reasonableAmountAtTime);
			}
		}
		return amounts;
	}

	public ResponseCode breakUpAndSellCoin(Ticker ticker, Double price, double totalAmountToSell) {
		List<Double> itemsBrokenUp = breakItUp(ticker.getStepSize(),
				accountService.getMinAmountAtTime()/price,
				accountService.getMaxAmountAtTime()/price,
				price,
				totalAmountToSell);
		logger.info("Selling: " + String.format("%.4f", totalAmountToSell) + " in " + itemsBrokenUp.size() + " chunks");
		this.amountExecuted = 0;
		this.amountSelling = totalAmountToSell;
		ResponseCode errorCode = null;
		for (double amt : itemsBrokenUp) {
			ResponseCode responseCode = sellCoin(ticker, amt);
			if (responseCode != SELL_ORDER_FILLED) {
				errorCode = responseCode;
			} else {
				this.amountExecuted += amt;
			}
		}
		if (errorCode != null) {
			return errorCode;
		}
		amountExecuted = 0;
		this.amountSelling = 0;
		return SELL_ORDER_FILLED;
	}

	public ResponseCode breakUpAndBuyCoin(Ticker ticker, Double price, double minAmount, double maxAmount,
										  double totalAmountToBuy, double totalOrigAmountToBuy) {
		List<Double> itemsBrokenUp = breakItUp(ticker.getStepSize(), minAmount, maxAmount, price, totalAmountToBuy);
		logger.info("Buying: " + String.format("%.4f", totalAmountToBuy) + " in " + itemsBrokenUp.size() + " chunks");
		ResponseCode errorCode = null;
		this.amountExecuted = 0;
		this.amountBuying = totalAmountToBuy;
		try {
			for (double amt : itemsBrokenUp) {
				ResponseCode responseCode = buyCoin(ticker, amt);
				if (responseCode != BUY_ORDER_FILLED) {
					errorCode = responseCode;
				} else {
					this.amountExecuted += amt;
				}
			}
		} catch (Exception ex) {
			// We ran out of funds before we could buy all we wanted
			if (ex.getMessage().contains("insufficient balance")) {
				// If we've bought 90% of what we wanted, good enough
				if (this.amountExecuted / totalOrigAmountToBuy > 0.9) {
					return BUY_ORDER_MOSTLY_FILLED;
				}
				// If we are really low on the amount of base coin, but still have purchased a bunch, also
				// consider it a success
				OwnedAsset asset = accountService.getBalance(ticker.getBase());
				if (asset.getFree() < 0.001 && this.amountExecuted / totalOrigAmountToBuy > 0.7) {
					return BUY_ORDER_MOSTLY_FILLED;
				}

				// Let's break it up further and buy in smaller chunks
				return breakUpAndBuyCoin(ticker, price, minAmount, maxAmount / 2,
						totalAmountToBuy - this.amountExecuted, totalOrigAmountToBuy);
			} else {
				logger.error("Unable to buy coin", ex);
				return BUY_ORDER_ERROR;
			}
		}
		if (errorCode != null) {
			return errorCode;
		}
		this.amountExecuted = 0;
		this.amountBuying = 0;
		return BUY_ORDER_FILLED;
	}

	/**
	 * Purchase the coin from the exchange
	 *
	 * @param coin        - the coin to buy
	 * @param amountToBuy - the amount to buy
	 * @return the response code of the purchase
	 */
	public ResponseCode buyCoin(Ticker coin, double amountToBuy) {
		logger.info("Buying " + amountToBuy + " of " + coin.getAssetAndBase());
		long startTime = System.currentTimeMillis();
		NewOrderResponse response = accountService.createBuyMarketOrder(coin.getAssetAndBase(), amountToBuy,
				swapDescriptor.getTableId() == null ? 0 : swapDescriptor.getTableId());
		if (response == null) {
			return ResponseCode.BUY_ORDER_ERROR;
		}

		// Retrieve the transaction Id
		String clientId = response.getClientOrderId();
		// Wait for the order to be filled
		Order order = waitForResponse(coin.getAssetAndBase(), clientId);
		if (!orderHistoryService.updateWithPrice(order.getOrderId())) {
			logger.error("Unable to determine the order price of order " + clientId);
		}

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
	public Order waitForResponse(String ticker, String clientId) {
		Order order = accountService.checkOrderStatus(ticker, clientId);
		OrderStatus status = order.getStatus();
		while (status == OrderStatus.PARTIALLY_FILLED) {
			logger.info("Not quite filled");
			try {
				Thread.sleep(10000);
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

	public ScratchConstants.CurrentSwapState getCurrentSwapState() {
		return swapDescriptor.getCurrentSwapState();
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

		// Clear out the cache
		cache.clear();
	}
}
