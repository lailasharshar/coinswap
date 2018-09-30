package com.sharshar.coinswap.beans;

import com.sharshar.coinswap.utils.ScratchConstants;
import lombok.Data;
import lombok.experimental.Accessors;

import javax.persistence.*;
import java.io.Serializable;

/**
 * Allows us to save and bootstrap swap configurations
 *
 * Created by lsharshar on 7/20/2018.
 */
@Entity
@Table(name="swap")
@Data
@Accessors(chain = true)
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
	private Double lastVolume1;
	private Double lastVolume2;
	private int coinOwned;

	public ScratchConstants.Exchange getExchangeObj() {
		if (exchange == 0) {
			return null;
		}
		return ScratchConstants.Exchange.valueOf(exchange);
	}

	/**
	 * Returns true if one of the coins is the base coin.
	 *
	 * @return true if coin1 or coin2 is the base coin, false otherwise (including error conditions)
	 */
	public boolean hasBaseCoin() {
		if (coin1 == null || baseCoin == null || coin2 == null) {
			return false;
		}
		if (coin1.equalsIgnoreCase(baseCoin) || coin2.equalsIgnoreCase(baseCoin)) {
			return true;
		}
		return false;
	}

	public ScratchConstants.CurrentSwapState getCurrentSwapState() {
		if (coinOwned == 0 || coinOwned == 1) {
			return ScratchConstants.CurrentSwapState.OWNS_COIN_1;
		}
		if (coinOwned == 2) {
			return ScratchConstants.CurrentSwapState.OWNS_COIN_2;
		}
		return ScratchConstants.CurrentSwapState.OWNS_NOTHING;
	}

	public SwapDescriptor setCurrentSwapState(ScratchConstants.CurrentSwapState swapState) {
		if (swapState == ScratchConstants.CurrentSwapState.OWNS_COIN_1) {
			this.coinOwned = 1;
		} else if (swapState == ScratchConstants.CurrentSwapState.OWNS_COIN_2) {
			this.coinOwned = 2;
		} else if (swapState == ScratchConstants.CurrentSwapState.OWNS_NOTHING) {
			this.coinOwned = 3;
		}
		return this;
	}

	public static SwapDescriptor clone(SwapDescriptor sd) {
		return new SwapDescriptor()
				.setActive(sd.getActive())
				.setBaseCoin(sd.getBaseCoin())
				.setCoin1(sd.getCoin1())
				.setCoin2(sd.getCoin2())
				.setCommissionCoin(sd.getCommissionCoin())
				.setExchange(sd.getExchange())
				.setSimulate(sd.getSimulate())
				.setCurrentSwapState(sd.getCurrentSwapState())
				.setDesiredStdDev(sd.getDesiredStdDev())
				.setMaxPercentVolume(sd.getMaxPercentVolume())
				.setLastVolume1(sd.getLastVolume1())
				.setLastVolume2(sd.getLastVolume2())
				.setTableId(sd.getTableId());
	}
}
