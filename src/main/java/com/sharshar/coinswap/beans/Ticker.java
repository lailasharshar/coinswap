package com.sharshar.coinswap.beans;

import com.sharshar.coinswap.utils.ScratchConstants;
import lombok.Data;
import lombok.experimental.Accessors;

import javax.persistence.*;
import java.util.Date;
import java.util.List;

/**
 * Describes all coins in a particular exchange. Allows us to determine when they are added (or first time db is
 * populated)
 *
 * Created by lsharshar on 7/16/2018.
 */
@Entity
@Table(name="tickers")
@Data
@Accessors(chain = true)
public class Ticker {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	long tableId;
	@Column(name = "ticker")
	private String asset;
	private String base;
	private ScratchConstants.Exchange exchange;
	private Date foundDate;
	private Date retired;
	private Date updatedDate;
	private Double minQty;
	private Double maxQty;
	private Double stepSize;
	private Double lastVolume;

	public Ticker() {}

	public Ticker(String tickerAndBase, ScratchConstants.Exchange exchange, List<String> baseCurrencies) {
		this.exchange = exchange;
		if (baseCurrencies == null || tickerAndBase == null || tickerAndBase.isEmpty()) {
			return;
		}
		String foundBase = "";
		for (String base : baseCurrencies) {
			if (tickerAndBase.endsWith(base)) {
				if (base.length() > foundBase.length()) {
					foundBase = base;
				}
			}
		}
		if (foundBase.length() > 0) {
			this.asset = tickerAndBase.replaceAll(foundBase, "");
			this.base = foundBase;
		}
	}

	public String getAssetAndBase() {
		return asset + base;
	}
}
