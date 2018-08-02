package com.sharshar.coinswap.services;

import com.sharshar.coinswap.TestCoinswapApplication;
import com.sharshar.coinswap.beans.simulation.SimulatorRecord;
import com.sharshar.coinswap.beans.simulation.SnapshotDescriptor;
import com.sharshar.coinswap.utils.ScratchConstants;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.List;

/**
 * Test for historical analysis
 *
 * Created by lsharshar on 7/31/2018.
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = TestCoinswapApplication.class)
public class HistoricalAnalysisServiceTest {
	@Autowired
	private HistoricalAnalysisService historical;

	@Autowired
	private TickerService tickerService;

	@Test
	public void simulateHistoricalAnalysis() throws Exception {
		SimulatorRecord record = historical.simulateHistoricalAnalysis("BCD", "CHAT", "BTC", ScratchConstants.BINANCE,
				"BNB", 86400000L /* 1 day */, 1.0, 1.0);
		List<SnapshotDescriptor> snapshots = record.getSnapshotDescriptorList();
		//System.out.println(snapshots.get(0).getAmountCoin1()/snapshots.get(0).getAmountCoin2());
		//System.out.println(snapshots.get(snapshots.size() - 1).getAmountCoin1()/snapshots.get(snapshots.size() - 1).getAmountCoin2());
	}

	@Test
	public void simulateRandom() {
		tickerService.loadTickers();
		tickerService.runRandomSimulation();
	}
}