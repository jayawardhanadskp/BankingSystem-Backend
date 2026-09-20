package com.banking.accountservice.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FcmTokenRequest {

    @NotBlank(message = "fcmToken is required")
    private String fcmToken;
}
