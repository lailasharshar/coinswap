package com.sharshar.coinswap.utils;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Make sure that exchange enum is working correctly
 *
 * Created by lsharshar on 8/26/2018.
 */
public class ScratchConstantsTest {
	@Test
	public void testEnums() {
		assertEquals(ScratchConstants.Exchange.BINANCE, ScratchConstants.Exchange.valueOf(1));
		assertEquals(ScratchConstants.Exchange.BINANCE.name(), "BINANCE");
		assertEquals(ScratchConstants.Exchange.BINANCE.getExchangeName(), "Binance");
		assertEquals(1, ScratchConstants.Exchange.BINANCE.getValue());
		assertNull(ScratchConstants.Exchange.valueOf(0));
	}

}