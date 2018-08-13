package com.sharshar.coinswap.services;

import com.sharshar.coinswap.TestCoinswapApplication;
import com.sharshar.coinswap.beans.SwapDescriptor;
import com.sharshar.coinswap.beans.simulation.SimulatorRecord;
import com.sharshar.coinswap.beans.simulation.SnapshotDescriptor;
import com.sharshar.coinswap.beans.simulation.TradeAction;
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

	@Autowired
	private MonitorService monitorService;

	@Autowired
	NotificationService notificationService;

	@Test
	public void simulateHistoricalAnalysis() throws Exception {
		SwapDescriptor swap = new SwapDescriptor().setCoin1("TUSD").setCoin2("BCD").setBaseCoin("BTC")
				.setExchange(ScratchConstants.Exchange.BINANCE.getValue()).setCommissionCoin("BNB").setActive(true).setSimulate(true);
		double stdDev = 0.02;
		while (stdDev < 0.10) {
			swap.setDesiredStdDev(stdDev);
			SimulatorRecord record = historical.simulateHistoricalAnalysis(swap, 86400000L /* 1 day */, 0.15);
			List<SnapshotDescriptor> snapshots = record.getSnapshotDescriptorList();
			List<TradeAction> actions = record.getTradeActionList();
			for (TradeAction ta : actions) {
				System.out.println(stdDev + " - " + ta.toString());
			}
			if (!snapshots.isEmpty()) {
				SnapshotDescriptor descriptor = record.getSnapshotDescriptorList().get(record.getSnapshotDescriptorList().size() - 1);
				System.out.println("StdDev: " + stdDev + " " + String.format("%.4f", descriptor.getTotalValue()) + " Trades: " + record.getTradeActionList().size());
			}
			stdDev += 0.02;
//			if (!snapshots.isEmpty()) {
//				SnapshotDescriptor descriptor = record.getSnapshotDescriptorList().get(record.getSnapshotDescriptorList().size() - 1);
//				System.out.println("StdDev: " + stdDev + " " + String.format("%.4f", descriptor.getTotalValue()) + " Trades: " + record.getTradeActionList().size());
//			} else {
//				System.out.println("StdDev: " + stdDev + " " + String.format("%.4f", 1.0));
//			}
		//System.out.println(snapshots.get(0).getAmountCoin1()/snapshots.get(0).getAmountCoin2());
		//System.out.println(snapshots.get(snapshots.size() - 1).getAmountCoin1()/snapshots.get(snapshots.size() - 1).getAmountCoin2());
		}
	}

	//@Test
	public void simulateRandom() {
		tickerService.loadTickers();
		tickerService.runRandomSimulation();
	}


	@Test
	public void textMe() throws Exception {
		notificationService.textMe("Text", "Hello World");
	}
	@Test
	public void testDailyMonitor() {
		monitorService.notifyBalanceEveryDay();
	}
}