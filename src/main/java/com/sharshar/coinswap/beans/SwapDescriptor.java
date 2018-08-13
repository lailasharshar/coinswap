package com.sharshar.coinswap.beans;

import com.sharshar.coinswap.utils.ScratchConstants;

import javax.persistence.*;
import java.io.Serializable;

/**
 * Allows us to save and bootstrap swap configurations
 *
 * Created by lsharshar on 7/20/2018.
 */
@Entity
@Table(name="swap")
public class SwapDescriptor implements Serializable{
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long tableId;

	private String coin1;
	private String coin2;
	private String baseCoin;
	private String commissionCoin;
	private short exchange;
	private Boolean active;
	private Boolean simulate;
	private double desiredStdDev;
	private Double maxPercentVolume;

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

	public ScratchConstants.Exchange getExchange() {
		return ScratchConstants.Exchange.valueOf(exchange);
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

	public Boolean isActive() {
		return active;
	}

	public SwapDescriptor setActive(Boolean active) {
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

	public SwapDescriptor setTableId(Long tableId) {
		this.tableId = tableId;
		return this;
	}

	public String getBaseCoin() {
		return baseCoin;
	}

	public SwapDescriptor setBaseCoin(String baseCoin) {
		this.baseCoin = baseCoin;
		return this;
	}

	public Boolean isSimulate() {
		return simulate;
	}

	public SwapDescriptor setSimulate(Boolean simulate) {
		this.simulate = simulate;
		return this;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("ID: ").append(tableId == null ? 0 : tableId).append(" - ").append(coin1)
				.append("/").append(coin2)
				.append("/").append(baseCoin)
				.append("/").append(commissionCoin)
				.append("/").append(String.format("%.4f", desiredStdDev))
				.append(" - ").append(getExchange().getExchangeName())
				.append(active ? " Active" : " Not Active")
				.append(simulate ? "/Simulate" : "/No Simulate");
		return sb.toString();
	}

	public double getMaxPercentVolume() {
		if (maxPercentVolume == null || maxPercentVolume == 0) {
			return 0.1;
		}
		return maxPercentVolume;
	}

	public SwapDescriptor setMaxPercentVolume(double maxPercentVolume) {
		this.maxPercentVolume = maxPercentVolume;
		return this;
	}
}
