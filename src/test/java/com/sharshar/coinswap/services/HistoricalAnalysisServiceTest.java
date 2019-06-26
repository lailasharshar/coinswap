package com.sharshar.coinswap.services;

import com.sharshar.coinswap.TestCoinswapApplication;
import com.sharshar.coinswap.beans.PriceData;
import com.sharshar.coinswap.beans.SwapDescriptor;
import com.sharshar.coinswap.beans.Ticker;
import com.sharshar.coinswap.beans.simulation.SimulatorRecord;
import com.sharshar.coinswap.beans.simulation.SnapshotDescriptor;
import com.sharshar.coinswap.beans.simulation.TradeAction;
import com.sharshar.coinswap.exchanges.Data;
import com.sharshar.coinswap.exchanges.HistoricalDataPull;
import com.sharshar.coinswap.exchanges.binance.BinanceAccountServices;
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
import java.util.stream.Collectors;

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

	@Autowired
	HistoricalDataPull historicalDataPull;

	@Autowired
	BinanceAccountServices binance;

	//@Test
	public void getGoodBTC() {
		double stdDev = 0.1;
		double maxVolume = 0.08;
		List<Ticker> tickers = tickerService.getTickers();
		for (Ticker ticker : tickers) {
			if (!"BTC".equalsIgnoreCase(ticker.getAsset()) && "BTC".equalsIgnoreCase(ticker.getBase())
					&& ticker.getExchange() == ScratchConstants.Exchange.BINANCE.getValue()) {
				try {
					runSimulation("BTC", ticker.getAsset(), maxVolume, stdDev);
					Thread.sleep(20000L);
				} catch (Exception ex) {
					System.out.println("Error for : " + ticker.getAsset());
					ex.printStackTrace();
				}
			}
		}
	}

	@Test
	public void simulateHistoricalWithBTC() throws Exception {
		double stdDev = 0.30;
		while (stdDev < 0.40) {
			try {
				runSimulation("BTC", "HOT", 0.05, stdDev);
				stdDev += 0.02;
				//Thread.sleep(20000L);
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}
	}

	@Test
	public void testOldData() {
		List<Data> data = historicalDataPull.getData("BCD", "BTC", 200,
				ScratchConstants.Exchange.BINANCE.name());
		List<PriceData> coin1HistoryData = data.stream().map(c ->
				new PriceData().setExchange(ScratchConstants.Exchange.BINANCE).
						setPrice(c.getOpen()).setTicker("BCDBTC").setUpdateTime(c.getTime()))
				.collect(Collectors.toList());
		coin1HistoryData.forEach(c -> System.out.println(String.format("%.6f", c.getPrice())));
		List<PriceData> pd = binance.getAllPrices();
		PriceData pdval = pd.stream().filter(c -> c.getTicker().equals("BCDBTC")).findFirst().orElse(null);
		System.out.println("Price: " + String.format("%.6f", pdval.getPrice()));
	}

	@Test
	public void simulateHistoricalAnalysis() throws Exception {
		double stdDev = 0.08;
		while (stdDev < 0.10) {
			runSimulation("TUSD", "BCD", 0.08, stdDev);
			stdDev += 0.02;
		}
	}

	private void runSimulation(String coin1, String coin2, double maxPercentVolume, double stdDev) {
		SwapDescriptor swap = new SwapDescriptor().setCoin1(coin1).setCoin2(coin2).setBaseCoin("BTC")
				.setExchange(ScratchConstants.Exchange.BINANCE.getValue()).setCommissionCoin("BNB")
				.setActive(true).setSimulate(true).setMaxPercentVolume(maxPercentVolume).setDesiredStdDev(stdDev);
		long checkUpInterval = ScratchConstants.ONE_DAY;
		double seedMoney = 1.0;
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
		historical.saveSimulation(swap, snapshots, checkUpInterval, seedMoney, startTime,
				endTime, record.getTradeActionList());
	}

	//@Test
	public void simulateRandom() {
		//tickerService.loadTickers();
		//simulationRunner.runRandomSimulation();
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