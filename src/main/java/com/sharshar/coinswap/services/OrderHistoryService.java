package com.sharshar.coinswap.services;

import com.binance.api.client.domain.account.Trade;
import com.sharshar.coinswap.beans.OrderHistory;
import com.sharshar.coinswap.exchanges.binance.BinanceAccountServices;
import com.sharshar.coinswap.repositories.OrderHistoryRepository;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Used to retrieve order history informaiton
 * Created by lsharshar on 8/23/2018.
 */
@Service
public class OrderHistoryService {
	private Logger logger = LogManager.getLogger();

	@Autowired
	private OrderHistoryRepository orderHistoryRepository;

	@Autowired
	BinanceAccountServices binanceAccountServices;

	public Iterable<OrderHistory> getAllOrders() {
		return orderHistoryRepository.findAllByOrderByCreateDtmDesc();
	}

	public List<OrderHistory> getAllOrdersForSwap(Long swapId) {
		return orderHistoryRepository.findBySwapId(swapId);
	}

	public boolean updateWithPrice(Long orderId) {
		try {
			List<OrderHistory> oh = orderHistoryRepository.findByOrderId(orderId);
			if (oh == null || oh.isEmpty()) {
				return false;
			}
			OrderHistory o = oh.get(0);
			double avgPrice = getAveragePrice(o.getSymbol(), orderId);
			o.setPrice(avgPrice);
			OrderHistory saved = orderHistoryRepository.save(o);
			return Math.abs(saved.getPrice() - avgPrice) < 0.0001;
		} catch (Exception ex) {
			logger.error("Unable to determine the order price of order " + orderId);
			return false;
		}
	}

	private double getAveragePrice(String ticker, Long orderId) {
		List<Trade> trades = binanceAccountServices.getMyTrades(ticker, "" + orderId);
		if (trades == null || trades.isEmpty()) {
			return 0.0;
		}
		double totalPaid = 0.0;
		double totalAmountOfCoin = 0.0;
		for (Trade trade: trades) {
			double amt = Double.parseDouble(trade.getQty());
			double price = Double.parseDouble(trade.getPrice());
			totalPaid += amt * price;
			totalAmountOfCoin += amt;
		}
		return totalPaid/totalAmountOfCoin;
	}
}
