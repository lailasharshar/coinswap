package com.sharshar.coinswap.beans.simulation;

import lombok.Data;
import lombok.experimental.Accessors;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import java.util.Date;

/**
 * Describe a snapshot event in a simulation
 *
 * Created by lsharshar on 8/2/2018.
 */
@Entity
@Data
@Accessors(chain = true)
public class SimulationRunSnapshot {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private long id;
	private long simulationId;
	private Date snapshotDate;
	private double amountCoin1;
	private double amountCoin2;
	private double amountCommissionCoin;
	private double coin1Price;
	private double coin2Price;
	private double commissionPrice;
}
