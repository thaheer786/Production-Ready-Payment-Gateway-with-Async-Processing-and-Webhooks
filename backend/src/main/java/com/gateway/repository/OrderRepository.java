package com.gateway.repository;

import com.gateway.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OrderRepository extends JpaRepository<Order, String> {
    List<Order> findByMerchantIdOrderByCreatedAtDesc(UUID merchantId);
    Optional<Order> findByIdAndMerchantId(String id, UUID merchantId);
}
