package com.example.splitwise.controllers;

import java.math.BigDecimal;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.splitwise.model.Transaction;
import com.example.splitwise.service.PaymentService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/payments")
@Tag(name = "Payments", description = "Payment processing endpoints")
@SecurityRequirement(name = "bearerAuth")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    public static class PayDto {

        public Long debitorId;
        public Long payerUserId;
        public BigDecimal amount;
    }

    @Operation(summary = "Make payment", description = "Make a payment towards a split (partial or full settlement)")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Payment successful"),
        @ApiResponse(responseCode = "400", description = "Invalid payment data or debitor not found"),
        @ApiResponse(responseCode = "500", description = "Payment failed (optimistic lock or server error)")
    })
    @PostMapping("/pay")
    public ResponseEntity<?> pay(@RequestBody PayDto dto) {
        if (dto.debitorId == null || dto.payerUserId == null || dto.amount == null) {
            return ResponseEntity.badRequest().build();
        }
        try {
            Transaction tx = paymentService.payDebitor(dto.debitorId, dto.payerUserId, dto.amount);
            
            // Return safe DTO instead of full entity
            var response = java.util.Map.of(
                "id", tx.getId(),
                "amount", tx.getAmount(),
                "fromUserId", tx.getFromUser().getId(),
                "toUserId", tx.getToUser().getId(),
                "eventId", tx.getEventId() != null ? tx.getEventId() : 0L,
                "timestamp", tx.getTs(),
                "note", tx.getNote() != null ? tx.getNote() : ""
            );
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException | IllegalStateException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(java.util.Map.of("error", ex.getMessage()));
        } catch (Exception ex) {
            // generic fallback (e.g., optimistic lock)
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(java.util.Map.of("error", "payment_failed", "message", ex.getMessage()));
        }
    }
}
