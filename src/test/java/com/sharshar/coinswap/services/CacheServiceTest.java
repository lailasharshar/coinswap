package com.sharshar.coinswap.services;

import com.sharshar.coinswap.beans.PriceData;
import com.sharshar.coinswap.components.ExchangeCache;
import com.sharshar.coinswap.utils.ScratchConstants;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Created by lsharshar on 7/27/2018.
 */
@RunWith(SpringRunner.class)
@SpringBootTest
@TestPropertySource(properties = {"cacheSize=300"})

public class CacheServiceTest {
	@Autowired
	CacheService cacheService;
	@Autowired
	SwapService swapService;
	String coin1 = "BCDBTC";
	String coin2 = "NEOBTC";

	@Test
	public void createCache() throws Exception {
	}

	@Test
	public void updateCaches() throws Exception {
	}

	@Test
	public void updatePriceData() throws Exception {
	}

}