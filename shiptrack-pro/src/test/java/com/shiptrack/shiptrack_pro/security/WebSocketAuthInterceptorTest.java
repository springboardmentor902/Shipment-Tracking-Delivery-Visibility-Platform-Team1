package com.shiptrack.shiptrack_pro.security;

import com.shiptrack.shiptrack_pro.service.ShipmentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WebSocketAuthInterceptorTest {

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private CustomUserDetailsService userDetailsService;

    @Mock
    private ShipmentService shipmentService;

    private WebSocketAuthInterceptor interceptor;
    private UserDetails customer;

    @BeforeEach
    void setUp() {
        interceptor = new WebSocketAuthInterceptor(jwtUtil, userDetailsService, shipmentService);
        customer = org.springframework.security.core.userdetails.User.builder()
                .username("customer@example.com")
                .password("unused")
                .authorities(List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER")))
                .build();
    }

    @Test
    void connectAuthenticatesBearerToken() {
        when(jwtUtil.extractEmail("valid-token")).thenReturn(customer.getUsername());
        when(userDetailsService.loadUserByUsername(customer.getUsername())).thenReturn(customer);
        when(jwtUtil.isTokenValid("valid-token", customer.getUsername())).thenReturn(true);

        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        accessor.setNativeHeader("Authorization", "Bearer valid-token");
        Message<byte[]> message = message(accessor);

        interceptor.preSend(message, null);

        assertThat(accessor.getUser()).isInstanceOf(UsernamePasswordAuthenticationToken.class);
        assertThat(accessor.getUser().getName()).isEqualTo(customer.getUsername());
    }

    @Test
    void subscribeChecksShipmentVisibility() {
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                customer, null, customer.getAuthorities());
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setUser(authentication);
        accessor.setDestination("/topic/shipments/42/location");

        interceptor.preSend(message(accessor), null);

        verify(shipmentService).getShipmentById(42L, customer.getUsername());
    }

    @Test
    void connectWithoutBearerTokenIsRejected() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);

        assertThatThrownBy(() -> interceptor.preSend(message(accessor), null))
                .isInstanceOf(AuthenticationCredentialsNotFoundException.class);
    }

    @Test
    void unrelatedSubscriptionDestinationIsRejected() {
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                customer, null, customer.getAuthorities());
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setUser(authentication);
        accessor.setDestination("/topic/all-locations");

        assertThatThrownBy(() -> interceptor.preSend(message(accessor), null))
                .isInstanceOf(AccessDeniedException.class);
    }

    private Message<byte[]> message(StompHeaderAccessor accessor) {
        accessor.setLeaveMutable(true);
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }
}
