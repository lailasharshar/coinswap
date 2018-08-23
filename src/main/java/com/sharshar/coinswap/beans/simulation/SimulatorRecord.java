package com.sharshar.coinswap.beans.simulation;

import com.sharshar.coinswap.beans.PriceData;
import com.sharshar.coinswap.beans.SwapDescriptor;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Holds summary information for analysis
 * Created by lsharshar on 7/30/2018.
 */
@Data
@Accessors(chain = true)
public class SimulatorRecord {
	public enum TradeDirection {
		BUY_COIN_2,
		BUY_COIN_1
	}

	private double initialBaseInvestment;
	private SwapDescriptor descriptor;
	private List<SnapshotDescriptor> snapshotDescriptorList = new ArrayList<>();
	private double desiredStdDev;
	private List<TradeAction> tradeActionList = new ArrayList<>();
	private Date startDate;
	private Date endDate;

	public void addSnapshot(Date snapshotDate, double amountCoin1, double amountCoin2, double amountCommissionCoin,
			PriceData coinPd1, PriceData coinPd2, PriceData commissionCoin) {
		if (snapshotDescriptorList == null) {
			snapshotDescriptorList = new ArrayList<>();
		}
		SnapshotDescriptor sd = new SnapshotDescriptor();
		sd.setSnapshotDate(snapshotDate).setAmountCoin1(amountCoin1).setAmountCoin2(amountCoin2)
				.setAmountCommissionCoin(amountCommissionCoin).setCoinPd1(coinPd1).setCoinPd2(coinPd2)
				.setCommissionCoin(commissionCoin);
		getSnapshotDescriptorList().add(sd);
	}

	public void addTradeAction(TradeAction ta) {
		getTradeActionList().add(ta);
	}
}
