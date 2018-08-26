package com.sharshar.coinswap.services;

import com.sharshar.coinswap.TestCoinswapApplication;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import static org.junit.Assert.*;

/**
 * Created by lsharshar on 8/26/2018.
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = TestCoinswapApplication.class)
public class NotificationServiceTest {

	@Autowired
	private NotificationService notificationService;

	@Test
	public void notifyMe() throws Exception {
		notificationService.notifyMe("Hello", "Hello World");
	}

	@Test
	public void textMe() throws Exception {
		notificationService.textMe("Hello", "Hello World");
	}
}