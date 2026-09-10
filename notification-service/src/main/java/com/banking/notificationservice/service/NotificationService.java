package com.banking.notificationservice.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@Slf4j
public class NotificationService {

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

            sendAlert( senderAccount,
                    "REFUND PROCESSED",
                    String.format(
                            "Your transaction of %s was cancelled " +
                            "Reason %s " +
                            "%s Has refunded to account %s ",

                            amount, reason, amount, senderAccount
                    ));

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

            sendAlert( accountNumber,
                    "PAYMENT SUCCESSFUL",
                    String.format(
                                    "Payment of %s completed " +
                                    "Razorpay id %s " +
                                    amount, payload.get("razorpayPaymentId")
                    ));

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
