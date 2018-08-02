package com.sharshar.coinswap.repositories;

import com.sharshar.coinswap.beans.simulation.SimulationRunSnapshot;
import org.springframework.data.repository.CrudRepository;

/**
 * Created by lsharshar on 8/2/2018.
 */
public interface SimulationRunSnapshotRepository extends CrudRepository<SimulationRunSnapshot, Long> {
}
