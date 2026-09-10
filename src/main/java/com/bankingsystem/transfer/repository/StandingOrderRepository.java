package com.bankingsystem.transfer.repository;

import com.bankingsystem.transfer.domain.StandingOrder;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StandingOrderRepository extends JpaRepository<StandingOrder, String> {

    List<StandingOrder> findByActiveTrue();
}
