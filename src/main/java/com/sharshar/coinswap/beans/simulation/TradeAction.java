package com.sharshar.coinswap.beans.simulation;

import com.sharshar.coinswap.components.SwapExecutor;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.Date;

/**
 * Describes a simulated trade
 *
 * Created by lsharshar on 7/31/2018.
 */
@Data
@Accessors(chain = true)
public class TradeAction {
	private Date tradeDate;
	private SimulatorRecord.TradeDirection direction;
	private double amountCoin1;
	private double amountCoin2;
	private SwapExecutor.ResponseCode responseCode;
}

