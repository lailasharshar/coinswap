package com.sharshar.coinswap.services;

import com.sharshar.coinswap.TestCoinswapApplication;
import com.sharshar.coinswap.beans.OrderHistory;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import static org.junit.Assert.*;

/**
 * Nothing more than testing to see if we can query past orders by date
 *
 * Created by lsharshar on 8/24/2018.
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = TestCoinswapApplication.class)
public class OrderHistoryServiceTest {

	@Autowired
	private OrderHistoryService orderHistoryService;
	@Test
	public void getAllOrders() throws Exception {
		Iterable<OrderHistory> orders = orderHistoryService.getAllOrders();
		assertNotNull(orders);
		orders.forEach(c -> System.out.println(c));
	}

}