package com.gateway.controller;

import com.gateway.dto.*;
import com.gateway.model.Merchant;
import com.gateway.service.OrderService;
import com.gateway.service.PaymentService;
import com.gateway.service.RefundService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ApiController {
    
    private final OrderService orderService;
    private final PaymentService paymentService;
    private final RefundService refundService;
    
    @PostMapping("/orders")
    public ResponseEntity<OrderResponse> createOrder(
            @RequestAttribute("merchant") Merchant merchant,
            @RequestBody CreateOrderRequest request) {
        try {
            OrderResponse response = orderService.createOrder(merchant, request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    @GetMapping("/orders/{id}")
    public ResponseEntity<OrderResponse> getOrder(
            @RequestAttribute("merchant") Merchant merchant,
            @PathVariable String id) {
        try {
            OrderResponse response = orderService.getOrder(merchant, id);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }
    
    @GetMapping("/orders")
    public ResponseEntity<List<OrderResponse>> getOrders(
            @RequestAttribute("merchant") Merchant merchant) {
        List<OrderResponse> response = orderService.getOrders(merchant);
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/payments")
    public ResponseEntity<?> createPayment(
            @RequestAttribute("merchant") Merchant merchant,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @RequestBody CreatePaymentRequest request) {
        try {
            PaymentResponse response = paymentService.createPayment(merchant, request, idempotencyKey);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            ErrorResponse errorResponse = new ErrorResponse(
                    new ErrorResponse.ErrorDetail("BAD_REQUEST_ERROR", e.getMessage())
            );
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }
    
    @GetMapping("/payments/{id}")
    public ResponseEntity<PaymentResponse> getPayment(
            @RequestAttribute("merchant") Merchant merchant,
            @PathVariable String id) {
        try {
            PaymentResponse response = paymentService.getPayment(merchant, id);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }
    
    @GetMapping("/payments")
    public ResponseEntity<List<PaymentResponse>> getPayments(
            @RequestAttribute("merchant") Merchant merchant) {
        List<PaymentResponse> response = paymentService.getPayments(merchant);
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/payments/{id}/capture")
    public ResponseEntity<?> capturePayment(
            @RequestAttribute("merchant") Merchant merchant,
            @PathVariable String id,
            @RequestBody CapturePaymentRequest request) {
        try {
            PaymentResponse response = paymentService.capturePayment(merchant, id, request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            ErrorResponse errorResponse = new ErrorResponse(
                    new ErrorResponse.ErrorDetail("BAD_REQUEST_ERROR", e.getMessage())
            );
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }
    
    @PostMapping("/payments/{paymentId}/refunds")
    public ResponseEntity<?> createRefund(
            @RequestAttribute("merchant") Merchant merchant,
            @PathVariable String paymentId,
            @RequestBody CreateRefundRequest request) {
        try {
            RefundResponse response = refundService.createRefund(merchant, paymentId, request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            ErrorResponse errorResponse = new ErrorResponse(
                    new ErrorResponse.ErrorDetail("BAD_REQUEST_ERROR", e.getMessage())
            );
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }
    
    @GetMapping("/refunds/{id}")
    public ResponseEntity<RefundResponse> getRefund(
            @RequestAttribute("merchant") Merchant merchant,
            @PathVariable String id) {
        try {
            RefundResponse response = refundService.getRefund(merchant, id);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }
}
