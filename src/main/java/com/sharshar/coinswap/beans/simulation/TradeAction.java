package com.sharshar.coinswap.beans.simulation;

import com.sharshar.coinswap.components.SwapExecutor;
import lombok.Data;
import lombok.experimental.Accessors;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import java.util.Date;

/**
 * Describes a simulated trade
 *
 * Created by lsharshar on 7/31/2018.
 */
@Entity
@Data
@Accessors(chain = true)
public class TradeAction {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private long id;

	private long simulationId;
	private Date tradeDate;
	private SimulatorRecord.TradeDirection direction;
	private double amountCoin1;
	private double amountCoin2;
	private double priceCoin1;
	private double priceCoin2;
	private SwapExecutor.ResponseCode responseCode;
}

