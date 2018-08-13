package com.sharshar.coinswap.exchanges;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * Holds the response of an historical data pull
 *
 * Created by lsharshar on 5/11/2018.
 */
@JsonIgnoreProperties
public class HistoryResponse {
	private String response;
	private Integer type;
	private Boolean aggregated;
	private List<Data> data;
	private Long timeTo;
	private Long timeFrom;
	private Boolean firstValueInArray;
	private ConversionType conversionType;
	private String message;
	private String yourCalls;

	public String getResponse() {
		return response;
	}

	public void setResponse(String response) {
		this.response = response;
	}

	public Integer getType() {
		return type;
	}

	public void setType(Integer type) {
		this.type = type;
	}

	public Boolean getAggregated() {
		return aggregated;
	}

	public void setAggregated(Boolean aggregated) {
		this.aggregated = aggregated;
	}

	public List<Data> getData() {
		return data;
	}

	public void setData(List<Data> data) {
		this.data = data;
	}

	public Long getTimeTo() {
		return timeTo;
	}

	public void setTimeTo(Long timeTo) {
		this.timeTo = timeTo;
	}

	public Long getTimeFrom() {
		return timeFrom;
	}

	public void setTimeFrom(Long timeFrom) {
		this.timeFrom = timeFrom;
	}

	public Boolean getFirstValueInArray() {
		return firstValueInArray;
	}

	public void setFirstValueInArray(Boolean firstValueInArray) {
		this.firstValueInArray = firstValueInArray;
	}

	public ConversionType getConversionType() {
		return conversionType;
	}

	public void setConversionType(ConversionType conversionType) {
		this.conversionType = conversionType;
	}

	public String getMessage() {
		return message;
	}

	public HistoryResponse setMessage(String message) {
		this.message = message;
		return this;
	}

	public String getYourCalls() {
		return yourCalls;
	}

	public HistoryResponse setYourCalls(String yourCalls) {
		this.yourCalls = yourCalls;
		return this;
	}
}
