package com.gateway.service;

import com.gateway.dto.CreateOrderRequest;
import com.gateway.dto.OrderResponse;
import com.gateway.model.Merchant;
import com.gateway.model.Order;
import com.gateway.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.ZoneOffset;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {
    
    private final OrderRepository orderRepository;
    
    private static final String ALPHANUMERIC = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private static final SecureRandom random = new SecureRandom();
    
    @Transactional
    public OrderResponse createOrder(Merchant merchant, CreateOrderRequest request) {
        // Validate amount
        if (request.getAmount() == null || request.getAmount() <= 0) {
            throw new RuntimeException("Invalid amount");
        }
        
        // Generate order ID
        String orderId = generateId("order_");
        
        // Create order
        Order order = new Order();
        order.setId(orderId);
        order.setMerchantId(merchant.getId());
        order.setAmount(request.getAmount());
        order.setCurrency(request.getCurrency() != null ? request.getCurrency() : "INR");
        order.setReceipt(request.getReceipt());
        order.setStatus("created");
        
        order = orderRepository.saveAndFlush(order);
        
        log.info("Created order {} for merchant {}", orderId, merchant.getId());
        
        return toOrderResponse(order);
    }
    
    public OrderResponse getOrder(Merchant merchant, String orderId) {
        Order order = orderRepository.findByIdAndMerchantId(orderId, merchant.getId())
                .orElseThrow(() -> new RuntimeException("Order not found"));
        return toOrderResponse(order);
    }
    
    public List<OrderResponse> getOrders(Merchant merchant) {
        return orderRepository.findByMerchantIdOrderByCreatedAtDesc(merchant.getId())
                .stream()
                .map(this::toOrderResponse)
                .collect(Collectors.toList());
    }
    
    private OrderResponse toOrderResponse(Order order) {
        return OrderResponse.builder()
                .id(order.getId())
                .merchantId(order.getMerchantId().toString())
                .amount(order.getAmount())
                .currency(order.getCurrency())
                .receipt(order.getReceipt())
                .status(order.getStatus())
                .createdAt(order.getCreatedAt().atZone(ZoneOffset.UTC).toString())
                .build();
    }
    
    private String generateId(String prefix) {
        StringBuilder sb = new StringBuilder(prefix);
        for (int i = 0; i < 16; i++) {
            sb.append(ALPHANUMERIC.charAt(random.nextInt(ALPHANUMERIC.length())));
        }
        return sb.toString();
    }
}
