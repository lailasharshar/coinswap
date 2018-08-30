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

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
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
	private SimulationRunner simulationRunner;

	@Autowired
	private MonitorService monitorService;

	@Autowired
	NotificationService notificationService;

	@Test
	public void simulateHistoricalAnalysis() throws Exception {
		SwapDescriptor swap = new SwapDescriptor().setCoin1("TUSD").setCoin2("BCD").setBaseCoin("BTC")
				.setExchange(ScratchConstants.Exchange.BINANCE.getValue()).setCommissionCoin("BNB")
				.setActive(true).setSimulate(true).setMaxPercentVolume(0.09);
		double stdDev = 0.02;
		while (stdDev < 0.10) {
			swap.setDesiredStdDev(stdDev);
			long checkUpInterval = ScratchConstants.ONE_DAY;
			double seedMoney = 0.15;
			long startTime = System.currentTimeMillis();
			SimulatorRecord record = historical.simulateHistoricalAnalysis(swap, checkUpInterval, seedMoney);
			long endTime = System.currentTimeMillis();
			List<SnapshotDescriptor> snapshots = record.getSnapshotDescriptorList();
			List<TradeAction> actions = record.getTradeActionList();
			double prevValue = 0;
			double profit = 0.0;
			double dailyProfit = 0;
			int prevDayValue = 0;
			List<Double> dailyProfits = new ArrayList();
			Date prevDate = null;
			Calendar cal = Calendar.getInstance();
			for (TradeAction ta : actions) {
				if (ta.getDirection() == SimulatorRecord.TradeDirection.BUY_COIN_1) {
					if (prevValue > 0.00000001) {
						profit = ta.getAmountCoin1() - prevValue;
					} else {
						profit = ta.getAmountCoin1() - 6500.00;
					}
					prevValue = ta.getAmountCoin1();
					// Get the date
					cal.setTime(ta.getTradeDate());
					int taDay = cal.get(Calendar.DAY_OF_MONTH);
					if (prevDayValue != taDay) {
						if (prevDate != null) {
							System.out.println(prevDate + " Daily Profits: " + String.format("%.2f", dailyProfits.get(dailyProfits.size() - 1)));
						}
						dailyProfits.add(profit);
					} else {
						dailyProfits.set(dailyProfits.size() - 1, dailyProfits.get(dailyProfits.size() - 1) + profit);
					}
					System.out.println("Profit: " + String.format("%.2f", profit));
					prevDayValue = taDay;
					prevDate = ta.getTradeDate();
				}
				System.out.println(stdDev + " - MaxPercVolume " + swap.getMaxPercentVolume() + " - " + ta.toString());
			}
			if (!snapshots.isEmpty()) {
				SnapshotDescriptor descriptor = record.getSnapshotDescriptorList().get(record.getSnapshotDescriptorList().size() - 1);
				System.out.println("StdDev: " + stdDev + " " + String.format("%.4f", descriptor.getTotalValue()) + " Trades: " + record.getTradeActionList().size());
			}
			stdDev += 0.02;
			historical.saveSimulation(swap, snapshots, checkUpInterval, seedMoney, startTime,
					endTime, record.getTradeActionList());
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
		simulationRunner.runRandomSimulation();
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