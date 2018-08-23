package com.sharshar.coinswap.beans.simulation;

import lombok.Data;
import lombok.experimental.Accessors;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import java.util.Date;

/**
 * Used to describe and summarize a simulation run
 *
 * Created by lsharshar on 8/2/2018.
 */
@Entity
@Data
@Accessors(chain = true)
public class SimulationRun {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private long id;
	private Date startDate;
	private Date endDate;
	private Date simulationStartTime;
	private Date simulationEndTime;
	private String coin1;
	private String coin2;
	private String baseCoin;
	private double stdDev;
	private String commissionCoin;
	private long snapshotInterval;
	private double startAmount;
	private double endAmount;
	private double meanChange;
	private double stdDevChange;
}
