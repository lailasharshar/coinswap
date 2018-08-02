package com.sharshar.coinswap.beans.simulation;

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

	public long getId() {
		return id;
	}

	public SimulationRun setId(long id) {
		this.id = id;
		return this;
	}

	public Date getStartDate() {
		return startDate;
	}

	public SimulationRun setStartDate(Date startDate) {
		this.startDate = startDate;
		return this;
	}

	public Date getEndDate() {
		return endDate;
	}

	public SimulationRun setEndDate(Date endDate) {
		this.endDate = endDate;
		return this;
	}

	public Date getSimulationStartTime() {
		return simulationStartTime;
	}

	public SimulationRun setSimulationStartTime(Date simulationStartTime) {
		this.simulationStartTime = simulationStartTime;
		return this;
	}

	public Date getSimulationEndTime() {
		return simulationEndTime;
	}

	public SimulationRun setSimulationEndTime(Date simulationEndTime) {
		this.simulationEndTime = simulationEndTime;
		return this;
	}

	public String getCoin1() {
		return coin1;
	}

	public SimulationRun setCoin1(String coin1) {
		this.coin1 = coin1;
		return this;
	}

	public String getCoin2() {
		return coin2;
	}

	public SimulationRun setCoin2(String coin2) {
		this.coin2 = coin2;
		return this;
	}

	public String getBaseCoin() {
		return baseCoin;
	}

	public SimulationRun setBaseCoin(String baseCoin) {
		this.baseCoin = baseCoin;
		return this;
	}

	public String getCommissionCoin() {
		return commissionCoin;
	}

	public SimulationRun setCommissionCoin(String commissionCoin) {
		this.commissionCoin = commissionCoin;
		return this;
	}

	public long getSnapshotInterval() {
		return snapshotInterval;
	}

	public SimulationRun setSnapshotInterval(long snapshotInterval) {
		this.snapshotInterval = snapshotInterval;
		return this;
	}

	public double getStartAmount() {
		return startAmount;
	}

	public SimulationRun setStartAmount(double startAmount) {
		this.startAmount = startAmount;
		return this;
	}

	public double getEndAmount() {
		return endAmount;
	}

	public SimulationRun setEndAmount(double endAmount) {
		this.endAmount = endAmount;
		return this;
	}

	public double getMeanChange() {
		return meanChange;
	}

	public SimulationRun setMeanChange(double meanChange) {
		this.meanChange = meanChange;
		return this;
	}

	public double getStdDevChange() {
		return stdDevChange;
	}

	public SimulationRun setStdDevChange(double stdDevChange) {
		this.stdDevChange = stdDevChange;
		return this;
	}

	public double getStdDev() {
		return stdDev;
	}

	public SimulationRun setStdDev(double stdDev) {
		this.stdDev = stdDev;
		return this;
	}
}
