package com.gateway.repository;

import com.gateway.model.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, String> {
    List<Payment> findByMerchantIdOrderByCreatedAtDesc(UUID merchantId);
    Optional<Payment> findByIdAndMerchantId(String id, UUID merchantId);
    List<Payment> findByOrderId(String orderId);
}
