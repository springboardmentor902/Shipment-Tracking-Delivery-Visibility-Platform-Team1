package com.shiptrack.shiptrack_pro.dto;
 
import lombok.*;
 
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {
    @ToString.Exclude
    private String token;
    private String tokenType;
    private UserResponse user;
}
