package com.sharshar.coinswap.beans.simulation;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import java.util.Date;

/**
 * Created by lsharshar on 8/2/2018.
 */
@Entity
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

	public long getId() {
		return id;
	}

	public SimulationRunSnapshot setId(long id) {
		this.id = id;
		return this;
	}

	public long getSimulationId() {
		return simulationId;
	}

	public SimulationRunSnapshot setSimulationId(long simulationId) {
		this.simulationId = simulationId;
		return this;
	}

	public Date getSnapshotDate() {
		return snapshotDate;
	}

	public SimulationRunSnapshot setSnapshotDate(Date snapshotDate) {
		this.snapshotDate = snapshotDate;
		return this;
	}

	public double getAmountCoin1() {
		return amountCoin1;
	}

	public SimulationRunSnapshot setAmountCoin1(double amountCoin1) {
		this.amountCoin1 = amountCoin1;
		return this;
	}

	public double getAmountCoin2() {
		return amountCoin2;
	}

	public SimulationRunSnapshot setAmountCoin2(double amountCoin2) {
		this.amountCoin2 = amountCoin2;
		return this;
	}

	public double getAmountCommissionCoin() {
		return amountCommissionCoin;
	}

	public SimulationRunSnapshot setAmountCommissionCoin(double amountCommissionCoin) {
		this.amountCommissionCoin = amountCommissionCoin;
		return this;
	}

	public double getCoin1Price() {
		return coin1Price;
	}

	public SimulationRunSnapshot setCoin1Price(double coin1Price) {
		this.coin1Price = coin1Price;
		return this;
	}

	public double getCoin2Price() {
		return coin2Price;
	}

	public SimulationRunSnapshot setCoin2Price(double coin2Price) {
		this.coin2Price = coin2Price;
		return this;
	}

	public double getCommissionPrice() {
		return commissionPrice;
	}

	public SimulationRunSnapshot setCommissionPrice(double commissionPrice) {
		this.commissionPrice = commissionPrice;
		return this;
	}
}
