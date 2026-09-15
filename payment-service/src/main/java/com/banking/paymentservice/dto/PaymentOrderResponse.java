package com.banking.paymentservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentOrderResponse {

    private String paymentId;
    private String stripePaymentIntentId;
    private String stripeClientSecret;
    private BigDecimal amount;
    private String currency;
    private String status;
    private String stripePublishableKey;
}
