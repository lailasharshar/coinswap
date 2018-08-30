package com.sharshar.coinswap.repositories;

import com.sharshar.coinswap.beans.simulation.TradeAction;
import org.springframework.data.repository.CrudRepository;

/**
 * Created by lsharshar on 8/26/2018.
 */
public interface TradeActionRepository extends CrudRepository<TradeAction, Long> {
}
