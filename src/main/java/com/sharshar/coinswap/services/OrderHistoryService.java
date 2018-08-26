package com.sharshar.coinswap.services;

import com.sharshar.coinswap.beans.OrderHistory;
import com.sharshar.coinswap.repositories.OrderHistoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Used to retrieve order history informaiton
 * Created by lsharshar on 8/23/2018.
 */
@Service
public class OrderHistoryService {
	@Autowired
	private OrderHistoryRepository orderHistoryRepository;

	public Iterable<OrderHistory> getAllOrders() {
		return orderHistoryRepository.findAllByOrderByCreateDtmDesc();
	}
}
