package com.sharshar.coinswap.repositories;

import com.sharshar.coinswap.beans.SwapDescriptor;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

/**
 * Created by lsharshar on 7/20/2018.
 */
public interface SwapRepository extends CrudRepository<SwapDescriptor, Long>{
	List<SwapDescriptor> findByCoin1AndCoin2AndExchange(String coin1, String coin2, short exchange);
}
