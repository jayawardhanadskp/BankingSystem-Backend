package com.banking.paymentservice.service;

import com.banking.paymentservice.dto.CreatePaymentRequest;
import com.banking.paymentservice.dto.PaymentOrderResponse;
import com.banking.paymentservice.entity.Payment;
import com.banking.paymentservice.entity.PaymentStatus;
import com.banking.paymentservice.repository.PaymentRepository;
import com.stripe.Stripe;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.PaymentIntent;
import com.stripe.model.StripeObject;
import com.stripe.net.Webhook;
import com.stripe.param.PaymentIntentCreateParams;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class PaymentService {

    private static final String CURRENCY = "usd";

    private final PaymentRepository paymentRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${stripe.secret-key}")
    private String secretKey;

    @Value("${stripe.publishable-key}")
    private String publishableKey;

    @Value("${stripe.webhook-secret}")
    private String webhookSecret;

    private static final String PAYMENT_COMPLETED_TOPIC = "payment.completed";
    private static final String PAYMENT_FAILED_TOPIC = "payment.failed";

    @PostConstruct
    void configureStripeApiKey() {
        Stripe.apiKey = secretKey;
    }

    /**
     * Create a Stripe PaymentIntent
     *
     * FLOW:
     * 1 create a PaymentIntent with Stripe
     * 2 save payment record in DB
     * 3 return the client secret to the frontend
     * 4 frontend confirms the payment using Stripe.js/Elements
     * 5 Stripe calls our webhook once the payment succeeds/fails
     *
     * @param request
     * @return
     */
    public PaymentOrderResponse createPaymentOrder(CreatePaymentRequest request) throws StripeException {
        log.info("Creating payment intent for account: {} amount: {}",
                request.getAccountNumber(), request.getAmount());

        // converted amount to the smallest currency unit (cents)
        long convertAmount = request.getAmount()
                .multiply(BigDecimal.valueOf(100))
                .longValue();

        PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                .setAmount(convertAmount)
                .setCurrency(CURRENCY)
                .setDescription(request.getDescription())
                .putMetadata("accountNumber", request.getAccountNumber())
                .setAutomaticPaymentMethods(
                        PaymentIntentCreateParams.AutomaticPaymentMethods.builder()
                                .setEnabled(true)
                                .build()
                )
                .build();

        PaymentIntent paymentIntent = PaymentIntent.create(params);

        log.info("Stripe payment intent created: {}", paymentIntent.getId());

        // save payment record
        Payment payment = new Payment();
        payment.setStripePaymentIntentId(paymentIntent.getId());
        payment.setAccountNumber(request.getAccountNumber());
        payment.setAmount(request.getAmount());
        payment.setCurrency(CURRENCY);
        payment.setStatus(PaymentStatus.CREATED);
        payment.setDescription(request.getDescription());

        Payment savePayment = paymentRepository.save(payment);

        return new PaymentOrderResponse(
                savePayment.getId(),
                paymentIntent.getId(),
                paymentIntent.getClientSecret(),
                request.getAmount(),
                CURRENCY,
                "CREATED",
                publishableKey
        );
    }

    public void handleWebhook(String payload, String signatureHeader) {

        Event event;
        try {
            event = Webhook.constructEvent(payload, signatureHeader, webhookSecret);
        } catch (SignatureVerificationException e) {
            log.error("Invalid Stripe webhook signature: {}", e.getMessage());
            return;
        }

        log.info("Received Stripe webhook: {}", event.getType());

        Optional<StripeObject> stripeObject = event.getDataObjectDeserializer().getObject();
        if (stripeObject.isEmpty() || !(stripeObject.get() instanceof PaymentIntent paymentIntent)) {
            log.warn("Ignoring webhook event {} with no PaymentIntent payload", event.getType());
            return;
        }

        if ("payment_intent.succeeded".equals(event.getType())) {
            handlePaymentSuccess(paymentIntent);
        } else if ("payment_intent.payment_failed".equals(event.getType())) {
            handlePaymentFailure(paymentIntent);
        }
    }

    private void handlePaymentSuccess(PaymentIntent paymentIntent) {

        try {
            Payment payment = paymentRepository.findByStripePaymentIntentId(paymentIntent.getId())
                    .orElseThrow(() -> new RuntimeException(
                            "Payment not found for payment intent " + paymentIntent.getId()
                    ));

            payment.setStatus(PaymentStatus.COMPLETED);
            paymentRepository.save(payment);

            //publish payment complete event
            Map<String, Object> event = new HashMap<>();
            event.put("paymentId", payment.getId());
            event.put("accountNumber", payment.getAccountNumber());
            event.put("amount", payment.getAmount());
            event.put("stripePaymentIntentId", payment.getStripePaymentIntentId());

            kafkaTemplate.send(PAYMENT_COMPLETED_TOPIC, payment.getId(), event);
            log.info("Payment completed: {}", payment.getId());

        } catch (Exception e) {
            log.error("Error handling payment success: {}", e.getMessage());
        }
    }

    private void handlePaymentFailure(PaymentIntent paymentIntent) {

        try {
            Payment payment = paymentRepository.findByStripePaymentIntentId(paymentIntent.getId())
                    .orElseThrow(() -> new RuntimeException(
                            "Payment not found for payment intent " + paymentIntent.getId()
                    ));

            payment.setStatus(PaymentStatus.FAILED);
            payment.setFailureReason("Payment failed via stripe");
            paymentRepository.save(payment);

            //publish payment failed event
            Map<String, Object> event = new HashMap<>();
            event.put("paymentId", payment.getId());
            event.put("accountNumber", payment.getAccountNumber());
            event.put("amount", payment.getAmount());
            event.put("reason", "Payment failed via stripe");

            kafkaTemplate.send(PAYMENT_FAILED_TOPIC, payment.getId(), event);
            log.warn("Payment failed: {}", payment.getId());

        } catch (Exception e) {
            log.error("Error handling payment failure: {}", e.getMessage());
        }
    }
}
