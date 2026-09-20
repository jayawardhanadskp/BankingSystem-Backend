package com.banking.notificationservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class NotificationService {

    private final FcmService fcmService;

    @KafkaListener(topics = "transaction.otp.generated")
    public void consumeOtpGeneration(
            @Payload Map<String, Object> payload
    ) {
        try {
            String accountNumber = (String) payload.get("accountNumber");
            String otp = (String) payload.get("otp");
            String transactionId = (String) payload.get("transactionId");
            String amount = payload.get("amount").toString();
            String reason = (String) payload.get("reason");

            sendAlert(
                    "",
                    "TRANSACTION VERIFICATION REQUIRED",
                    String.format(
                            "Suspicious activity detected on your account, " +
                            "Reason %s" +
                            "A transaction of %s is pending verification" +
                            "Your OTP is %s. Valid for 5 min" +
                            "If this wasn't you - Ignore this"

                    )
            );

            Map<String, String> data = new HashMap<>();
            data.put("transactionId", transactionId);
            data.put("otp", otp);
            data.put("amount", amount);
            fcmService.sendPush(accountNumber, "OTP_REQUIRED", data,
                    "Transaction verification required",
                    "Your OTP is " + otp + ". Valid for 5 min.");

        } catch (Exception e) {
            log.error("Error sending OTP notification {}", e.getMessage());
        }
    }

    @KafkaListener(topics = "transaction.completed")
    public void consumeTransactionCompleted(
            @Payload Map<String, Object> payload
    ) {

        try {

            String senderAccount = (String) payload.get("senderAccountNumber");
            String receiverAccount = (String) payload.get("senderAccountNumber");
            String amount = payload.get("amount").toString();
            String transactionId = (String) payload.get("transactionId");

            //DEBIT ALERT
            sendAlert( senderAccount,
                    "DEBIT ALERT",
                    String.format(
                            "%s debited for account %s",
                            amount, senderAccount
                    ));

            //CREDIT ALERT
            sendAlert( receiverAccount,
                    "CREDIT ALERT",
                    String.format(
                            "%s credited for account %s",
                            amount, receiverAccount
                    ));

            Map<String, String> data = new HashMap<>();
            data.put("transactionId", transactionId);
            data.put("amount", amount);

            fcmService.sendPush(senderAccount, "TRANSACTION_COMPLETED", data,
                    "Debit Alert", amount + " debited from your account");
            fcmService.sendPush(receiverAccount, "TRANSACTION_COMPLETED", data,
                    "Credit Alert", amount + " credited to your account");


        } catch (Exception e) {
            log.error("Error sending transaction account number: {}", e.getMessage());
        }
    }

    @KafkaListener(topics = "fraud.detected")
    public void consumeFraudDetected(
            @Payload Map<String, Object> payload
    ) {
        try {

            String accountNumber = (String) payload.get("accountNumber");
            String reason = (String) payload.get("reason");

            sendAlert( accountNumber,
                    "SUSPICIOUS ACTIVITY DETECTED",
                    String.format(
                            "Your account %s has been blocked " +
                            "Reason %s " +
                            "Please contact your bank immediately",
                            accountNumber, reason
                    ));

            Map<String, String> data = new HashMap<>();
            data.put("reason", reason);
            fcmService.sendPush(accountNumber, "FRAUD_DETECTED", data,
                    "Suspicious activity detected",
                    "Your account has been blocked. Reason: " + reason);

        } catch (Exception e) {
            log.error("Error sending fraud alert: {}", e.getMessage());
        }
    }

    @KafkaListener(topics = "transaction.refunded")
    public void consumeTransactionRefund(
            @Payload Map<String, Object> payload
    ) {
        try {

            String senderAccount = (String) payload.get("senderAccountNumber");
            String amount = payload.get("amount").toString();
            String reason = (String) payload.get("reason");
            String transactionId = (String) payload.get("transactionId");

            sendAlert( senderAccount,
                    "REFUND PROCESSED",
                    String.format(
                            "Your transaction of %s was cancelled " +
                            "Reason %s " +
                            "%s Has refunded to account %s ",

                            amount, reason, amount, senderAccount
                    ));

            Map<String, String> data = new HashMap<>();
            data.put("transactionId", transactionId);
            data.put("amount", amount);
            data.put("reason", reason);
            fcmService.sendPush(senderAccount, "TRANSACTION_REFUNDED", data,
                    "Refund processed",
                    amount + " refunded to your account. Reason: " + reason);

        } catch (Exception e) {
            log.error("Error sending refund notification: {}", e.getMessage());
        }
    }

    @KafkaListener(topics = "payment.completed")
    public void consumePaymentCompleted(
            @Payload Map<String, Object> payload
    ) {
        try {

            String accountNumber = (String) payload.get("accountNumber");
            String amount = payload.get("amount").toString();
            String stripePaymentIntentId = (String) payload.get("stripePaymentIntentId");

            sendAlert( accountNumber,
                    "PAYMENT SUCCESSFUL",
                    String.format(
                                    "Payment of %s completed " +
                                    "Stripe payment intent %s ",
                                    amount, stripePaymentIntentId
                    ));

            Map<String, String> data = new HashMap<>();
            data.put("amount", amount);
            data.put("stripePaymentIntentId", stripePaymentIntentId);
            fcmService.sendPush(accountNumber, "PAYMENT_COMPLETED", data,
                    "Payment successful",
                    "Payment of " + amount + " completed");

        } catch (Exception e) {
            log.error("Error sending payment success notification: {}", e.getMessage());
        }
    }

    @KafkaListener(topics = "payment.failed")
    public void consumePaymentFailed(
            @Payload Map<String, Object> payload
    ) {
        try {

            String accountNumber = (String) payload.get("accountNumber");
            String amount = payload.get("amount").toString();

            sendAlert( accountNumber,
                    "PAYMENT FAILED",
                    String.format(
                                    "Payment of %s could not be processed " +
                                    "please try again or contact support ",
                                    amount
                    ));

            Map<String, String> data = new HashMap<>();
            data.put("amount", amount);
            fcmService.sendPush(accountNumber, "PAYMENT_FAILED", data,
                    "Payment failed",
                    "Payment of " + amount + " could not be processed");

        } catch (Exception e) {
            log.error("Error sending payment failure notification: {}", e.getMessage());
        }
    }

    private void sendAlert(String accountNumber, String subject, String message) {

        log.info("--------------------------------------------------------");
        log.info("Account: {}", accountNumber);
        log.info("Subject: {}", subject);
        log.info("Message: {}", message);
        log.info("--------------------------------------------------------");

    }
}
