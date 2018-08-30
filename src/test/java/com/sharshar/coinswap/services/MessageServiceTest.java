package com.sharshar.coinswap.services;

import com.sharshar.coinswap.TestCoinswapApplication;
import com.sharshar.coinswap.exchanges.binance.BinanceAccountServices;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import static org.junit.Assert.*;

/**
 * Use this to check the format of the sent notifications
 *
 * Created by lsharshar on 8/26/2018.
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = TestCoinswapApplication.class)
public class MessageServiceTest {
	@Autowired
	private MessageService messageService;

	@Autowired
	private BinanceAccountServices binance;

	@Test
	public void dailyRoundup() throws Exception {
		messageService.dailyRoundup(binance);
	}
}