package com.shiptrack.shiptrack_pro.dto;
 
import jakarta.validation.constraints.*;
import lombok.Data;
import lombok.ToString;
 
@Data
public class LoginRequest {
 
    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    private String email;
 
    @NotBlank(message = "Password is required")
    @ToString.Exclude
    private String password;
}
