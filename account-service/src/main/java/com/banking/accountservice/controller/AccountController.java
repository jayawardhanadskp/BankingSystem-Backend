package com.banking.accountservice.controller;

import com.banking.accountservice.dto.AccountResponse;
import com.banking.accountservice.dto.FcmTokenRequest;
import com.banking.accountservice.service.AccountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("api/v1/accounts")
@Slf4j
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    @GetMapping("/{accountNumber}")
    public ResponseEntity<AccountResponse> getAccount(
            @PathVariable String accountNumber
    ) {
        return ResponseEntity.ok(accountService.getAccount(accountNumber));
    }

    @GetMapping("/{accountNumber}/balance")
    public ResponseEntity<BigDecimal> getBalance(
            @PathVariable String accountNumber
    ) {
        return ResponseEntity.ok(accountService.getBalance(accountNumber));
    }

    @PutMapping("/{accountNumber}/block")
    public ResponseEntity<String> blocAccount(
            @PathVariable String accountNumber
    ) {
        accountService.blockAccount(accountNumber);
        return ResponseEntity.ok("Account blocked successfully");
    }




    /*
  SAGA 1 - deduct balance
  call transaction service when transfer is initiated
  */

    @PutMapping("/{accountNumber}/deduct")
    public ResponseEntity<String> deductBalance(
            @PathVariable String accountNumber,
            @RequestParam BigDecimal amount
    ) {
        accountService.deductBalance(accountNumber, amount);
        return ResponseEntity.ok("Balance deduct successfully");

    }

    /*
  SAGA 2 - compensating transaction endpoint
  called by transaction service in 2 scenarios
    1. fraud detected -> refund sender (undo step 1)
    2. complete -> credit receiver
  */

    @PutMapping("/{accountNumber}/credit")
    public ResponseEntity<String> creditBalance(
            @PathVariable String accountNumber,
            @RequestParam BigDecimal amount
    ) {
        accountService.creditBalance(accountNumber, amount);
        return ResponseEntity.ok("Balance credited successfully");
    }

    /*
  Registers/updates the FCM push-notification token for this account.
  Called by the client app after obtaining a token from Firebase.
  */
    @PutMapping("/{accountNumber}/fcm-token")
    public ResponseEntity<String> updateFcmToken(
            @PathVariable String accountNumber,
            @Valid @RequestBody FcmTokenRequest request
    ) {
        accountService.updateFcmToken(accountNumber, request.getFcmToken());
        return ResponseEntity.ok("FCM token updated successfully");
    }
}

