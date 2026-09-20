package com.banking.accountservice.service;

import com.banking.accountservice.dto.AuthResponse;
import com.banking.accountservice.dto.LoginRequest;
import com.banking.accountservice.dto.RegisterRequest;
import com.banking.accountservice.entity.Account;
import com.banking.accountservice.entity.AccountStatus;
import com.banking.accountservice.entity.AccountType;
import com.banking.accountservice.exception.DuplicateResourceException;
import com.banking.accountservice.exception.InvalidCredentialsException;
import com.banking.accountservice.repository.AccountRepository;
import com.banking.accountservice.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@Slf4j
@RequiredArgsConstructor
public class AuthService {

    private final AccountRepository accountRepository;
    private final AccountService accountService;
    private final BCryptPasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthResponse register(RegisterRequest request) {
        log.info("Registering new account for username {}", request.getUsername());

        if (accountRepository.existsByUsername(request.getUsername())) {
            throw new DuplicateResourceException("Username already taken: " + request.getUsername());
        }
        if (accountRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("Account already exists for email: " + request.getEmail());
        }

        Account account = new Account();
        account.setUsername(request.getUsername());
        account.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        account.setAccountHolderName(request.getAccountHolderName());
        account.setEmail(request.getEmail());
        account.setPhone(request.getPhone());
        account.setAccountType(request.getAccountType());
        account.setStatus(AccountStatus.ACTIVE);
        account.setBalance(request.getInitialDeposit());
        account.setAccountNumber(accountService.generateAccountNumber());
        account.setDailyTransactionLimit(
                request.getAccountType() == AccountType.SAVINGS
                        ? new BigDecimal("100000")
                        : new BigDecimal("200000")
        );

        Account savedAccount = accountRepository.save(account);
        log.info("Account registered: {}", savedAccount.getAccountNumber());

        String token = jwtService.generateToken(savedAccount.getAccountNumber(), savedAccount.getUsername());

        return new AuthResponse(
                token,
                "Bearer",
                jwtService.getExpirationSeconds(),
                savedAccount.getAccountNumber(),
                savedAccount.getAccountHolderName()
        );
    }

    public AuthResponse login(LoginRequest request) {
        Account account = accountRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new InvalidCredentialsException("Invalid username or password"));

        if (!passwordEncoder.matches(request.getPassword(), account.getPasswordHash())) {
            throw new InvalidCredentialsException("Invalid username or password");
        }

        String token = jwtService.generateToken(account.getAccountNumber(), account.getUsername());

        return new AuthResponse(
                token,
                "Bearer",
                jwtService.getExpirationSeconds(),
                account.getAccountNumber(),
                account.getAccountHolderName()
        );
    }
}
