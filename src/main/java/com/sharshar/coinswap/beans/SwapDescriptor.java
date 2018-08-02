package com.sharshar.coinswap.beans;

import javax.persistence.*;

/**
 * Allows us to save and bootstrap swap configurations
 *
 * Created by lsharshar on 7/20/2018.
 */
@Entity
@Table(name="swap")
public class SwapDescriptor {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long tableId;

	private String coin1;
	private String coin2;
	private short exchange;
	private String commissionCoin;
	private boolean active;
	private double desiredStdDev;

	public Long getTableId() {
		return tableId;
	}

	public SwapDescriptor setTableId(long tableId) {
		this.tableId = tableId;
		return this;
	}

	public String getCoin1() {
		return coin1;
	}

	public SwapDescriptor setCoin1(String coin1) {
		this.coin1 = coin1;
		return this;
	}

	public String getCoin2() {
		return coin2;
	}

	public SwapDescriptor setCoin2(String coin2) {
		this.coin2 = coin2;
		return this;
	}

	public short getExchange() {
		return exchange;
	}

	public SwapDescriptor setExchange(short exchange) {
		this.exchange = exchange;
		return this;
	}

	public String getCommissionCoin() {
		return commissionCoin;
	}

	public SwapDescriptor setCommissionCoin(String commissionCoin) {
		this.commissionCoin = commissionCoin;
		return this;
	}

	public boolean isActive() {
		return active;
	}

	public SwapDescriptor setActive(boolean active) {
		this.active = active;
		return this;
	}

	public double getDesiredStdDev() {
		return desiredStdDev;
	}

	public SwapDescriptor setDesiredStdDev(double desiredStdDev) {
		this.desiredStdDev = desiredStdDev;
		return this;
	}
}
