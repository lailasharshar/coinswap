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

	public ScratchConstants.Exchange getExchangeObj() {
		if (exchange == 0) {
			return null;
		}
		return ScratchConstants.Exchange.valueOf(exchange);
	}
}
