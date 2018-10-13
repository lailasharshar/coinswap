package com.sharshar.coinswap.services;

import com.sharshar.coinswap.beans.OrderHistory;
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

	public Iterable<OrderHistory> getAllOrders() {
		return orderHistoryRepository.findAllByOrderByCreateDtmDesc();
	}

	public List<OrderHistory> getAllOrdersForSwap(Long swapId) {
		return orderHistoryRepository.findBySwapId(swapId);
	}

	public boolean updateWithPrice(String clientId, String orderPrice) {
		if (orderPrice == null || orderPrice.isEmpty()) {
			return false;
		}
		try {
			double orderPriceD = Double.parseDouble(orderPrice);
			List<OrderHistory> oh = orderHistoryRepository.findByClientOrderId(clientId);
			if (oh == null || oh.isEmpty()) {
				return false;
			}
			OrderHistory o = oh.get(0);
			o.setPrice(orderPriceD);
			OrderHistory saved = orderHistoryRepository.save(o);
			return Math.abs(saved.getPrice() - orderPriceD) < 0.0001;
		} catch (Exception ex) {
			logger.error("Unable to determine the order price of order " + clientId);
			return false;
		}
	}
}
