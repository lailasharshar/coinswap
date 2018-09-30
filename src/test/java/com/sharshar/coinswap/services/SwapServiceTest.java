package com.sharshar.coinswap.services;

import com.sharshar.coinswap.TestCoinswapApplication;
import com.sharshar.coinswap.beans.SwapDescriptor;
import com.sharshar.coinswap.utils.ScratchConstants;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.List;

import static org.junit.Assert.*;

/**
 * Created by lsharshar on 9/20/2018.
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = TestCoinswapApplication.class)
public class SwapServiceTest {
	@Autowired
	SwapService swapService;

	@Test
	public void testBootstrap() {
		List<SwapService.Swap> swaps = swapService.getSwaps();
		assertNotNull(swaps);
		assertTrue(swaps.size() > 0);
		SwapService.Swap one = swaps.get(0);
		ScratchConstants.CurrentSwapState currentSwapState = one.getSwapDescriptor().getCurrentSwapState();
		assertTrue(currentSwapState == ScratchConstants.CurrentSwapState.OWNS_COIN_1 || currentSwapState == ScratchConstants.CurrentSwapState.OWNS_COIN_2);

		ScratchConstants.CurrentSwapState otherState = null;
		if (currentSwapState == ScratchConstants.CurrentSwapState.OWNS_COIN_1) {
			otherState = ScratchConstants.CurrentSwapState.OWNS_COIN_2;
		}
		if (currentSwapState == ScratchConstants.CurrentSwapState.OWNS_COIN_2) {
			otherState = ScratchConstants.CurrentSwapState.OWNS_COIN_1;
		}
		SwapDescriptor sd = one.getSwapDescriptor();
		swapService.updateCoinOwned(sd, otherState);
		assertEquals(one.getSwapDescriptor().getCurrentSwapState(), otherState);
		SwapDescriptor sd2 = SwapDescriptor.clone(sd);
		swapService.updateCoinOwned(sd2, currentSwapState);
		assertEquals(one.getSwapDescriptor().getCurrentSwapState(), currentSwapState);
		SwapDescriptor sdSim = SwapDescriptor.clone(sd);
		sdSim.setTableId(0L);
		swapService.updateCoinOwned(sdSim, otherState);
		assertNotEquals(one.getSwapDescriptor().getCurrentSwapState(), otherState);
		assertEquals(sdSim.getCurrentSwapState(), otherState);
	}
}
