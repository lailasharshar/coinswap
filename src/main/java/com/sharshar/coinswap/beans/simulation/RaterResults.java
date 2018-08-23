package com.sharshar.coinswap.beans.simulation;

import com.sharshar.coinswap.utils.AnalysisUtils;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.ArrayList;
import java.util.List;

/**
 * Creates summary information on the results of a simulation
 *
 * Created by lsharshar on 7/31/2018.
 */
@Data
@Accessors(chain = true)
public class RaterResults {
	private List<Double> multipliers = new ArrayList<>();
	private double mean;
	private double stdDev;

	public RaterResults(List<SnapshotDescriptor> snapshots) {
		getMultipliers(snapshots);
		mean = AnalysisUtils.getMean(multipliers);
		stdDev = AnalysisUtils.getStdDev(multipliers, mean);
	}

	private void getMultipliers(List<SnapshotDescriptor> snapshots) {
		List<Double> diffs = getDiffs(snapshots);
		multipliers = getMultipliersFromDiffs(diffs);
	}

	private List<Double> getMultipliersFromDiffs(List<Double> diffs) {
		List<Double> multipier = new ArrayList<>();
		if (diffs.size() < 2) {
			return multipier;
		}
		boolean first = true;
		double lastDiff = 0;
		for (double d : diffs) {
			if (first) {
				lastDiff = 0;
				first = false;
			} else {
				lastDiff = lastDiff/d;
			}
			multipier.add(lastDiff);
		}
		return multipier;
	}

	private List<Double> getDiffs(List<SnapshotDescriptor> snapshots) {
		List<Double> diffs = new ArrayList<>();
		double lastTotal = 0;
		for (SnapshotDescriptor snapshot : snapshots) {
			double totalValue = snapshot.getTotalValue();
			diffs.add(totalValue - lastTotal);
			lastTotal = totalValue;
		}
		return diffs;
	}
}
