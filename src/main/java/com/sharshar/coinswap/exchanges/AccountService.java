package com.sharshar.coinswap.exchanges;

import com.binance.api.client.domain.account.NewOrderResponse;
import com.binance.api.client.domain.account.Order;
import com.binance.api.client.domain.account.Trade;
import com.binance.api.client.domain.market.OrderBook;
import com.sharshar.coinswap.beans.OwnedAsset;
import com.sharshar.coinswap.beans.PriceData;
import com.sharshar.coinswap.beans.Ticker;
import com.sharshar.coinswap.utils.ScratchConstants;

import java.util.List;

/**
 * Created by lsharshar on 7/18/2018.
 */
public interface AccountService {
	ScratchConstants.Exchange getExchange();
	List<PriceData> getAllPrices();
	List<OwnedAsset> getAllBalances();
	List<OwnedAsset> getBalancesWithValues();
	OwnedAsset getBalance(String asset);
	double getDefaultTransactionFee();
	NewOrderResponse createBuyMarketOrder(String ticker, double amount);
	NewOrderResponse buyMarketTest(String ticker, double amount);
	NewOrderResponse createSellMarketOrder(String ticker, double amount);
	NewOrderResponse sellMarketTest(String ticker, double amount);
	List<Trade> getMyTrades(String ticker, String orderId);
	Order checkOrderStatus(String ticker, String origClientOrderId);
	boolean checkUp();
	List<PriceData> getBackfillData(int cacheSize,  String coin, String baseCoin);
	Double get24HourVolume(String ticker);
	List<Ticker> getTickerDefinitions();
	OrderBook getBookOrders(String ticker);
	double getMaxAmountAtTime();
}
