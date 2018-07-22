package com.sharshar.coinswap.beans;

import javax.persistence.*;

/**
 * Allows us to save and bootstrap swap configurations
 *
 * Created by lsharshar on 7/20/2018.
 */
@Entity
@Table(name="swap")
public class Swap {
	@Id
	@GeneratedValue(strategy= GenerationType.AUTO)
	private Long tableId;

	private String coin1;
	private String coin2;
	private short exchange;
	private String commissionCoin;
	@Column(name = "max_amount_coin1_to_buy")
	private double maxAmountCoin1ToBuy;
	@Column(name = "max_amount_coin2_to_buy")
	private double maxAmountCoin2ToBuy;
	private boolean active;
	private double desiredStdDev;

	public long getTableId() {
		return tableId;
	}

	public Swap setTableId(long tableId) {
		this.tableId = tableId;
		return this;
	}

	public String getCoin1() {
		return coin1;
	}

	public Swap setCoin1(String coin1) {
		this.coin1 = coin1;
		return this;
	}

	public String getCoin2() {
		return coin2;
	}

	public Swap setCoin2(String coin2) {
		this.coin2 = coin2;
		return this;
	}

	public short getExchange() {
		return exchange;
	}

	public Swap setExchange(short exchange) {
		this.exchange = exchange;
		return this;
	}

	public String getCommissionCoin() {
		return commissionCoin;
	}

	public Swap setCommissionCoin(String commissionCoin) {
		this.commissionCoin = commissionCoin;
		return this;
	}

	public double getMaxAmountCoin1ToBuy() {
		return maxAmountCoin1ToBuy;
	}

	public Swap setMaxAmountCoin1ToBuy(double maxAmountCoin1ToBuy) {
		this.maxAmountCoin1ToBuy = maxAmountCoin1ToBuy;
		return this;
	}

	public double getMaxAmountCoin2ToBuy() {
		return maxAmountCoin2ToBuy;
	}

	public Swap setMaxAmountCoin2ToBuy(double maxAmountCoin2ToBuy) {
		this.maxAmountCoin2ToBuy = maxAmountCoin2ToBuy;
		return this;
	}

	public boolean isActive() {
		return active;
	}

	public Swap setActive(boolean active) {
		this.active = active;
		return this;
	}

	public double getDesiredStdDev() {
		return desiredStdDev;
	}

	public Swap setDesiredStdDev(double desiredStdDev) {
		this.desiredStdDev = desiredStdDev;
		return this;
	}
}
