package com.sharshar.coinswap.exchanges;

import com.binance.api.client.domain.account.NewOrderResponse;
import com.binance.api.client.domain.account.Order;
import com.binance.api.client.domain.account.Trade;
import com.sharshar.coinswap.beans.OwnedAsset;
import com.sharshar.coinswap.beans.PriceData;

import java.util.List;

/**
 * Created by lsharshar on 7/18/2018.
 */
public interface AccountService {
	short getExchange();
	List<PriceData> getAllPrices();
	List<OwnedAsset> getAllBalances();
	List<OwnedAsset> getBalancesWithValues();
	OwnedAsset getBalance(String asset);
	double getDefaultTransactionFee();
	NewOrderResponse createBuyMarketOrder(String ticker, double amount);
	NewOrderResponse createSellMarketOrder(String ticker, double amount);
	List<Trade> getMyTrades(String ticker, String orderId);
	Order checkOrderStatus(String ticker, String origClientOrderId);
	boolean checkUp();
}
