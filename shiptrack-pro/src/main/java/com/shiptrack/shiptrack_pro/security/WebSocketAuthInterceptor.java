package com.shiptrack.shiptrack_pro.security;

import com.shiptrack.shiptrack_pro.service.ShipmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.security.Principal;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@RequiredArgsConstructor
public class WebSocketAuthInterceptor implements ChannelInterceptor {

    private static final String BEARER_PREFIX = "Bearer ";
    private static final Pattern SHIPMENT_LOCATION_TOPIC =
            Pattern.compile("^/topic/shipments/(\\d+)/location$");

    private final JwtUtil jwtUtil;
    private final CustomUserDetailsService userDetailsService;
    private final ShipmentService shipmentService;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null) {
            return message;
        }
        StompCommand command = accessor.getCommand();

        if (StompCommand.CONNECT.equals(command)) {
            accessor.setUser(authenticate(accessor.getFirstNativeHeader("Authorization")));
        } else if (StompCommand.SUBSCRIBE.equals(command)) {
            authorizeSubscription(accessor);
        } else if (StompCommand.SEND.equals(command)) {
            throw new AccessDeniedException("Client messages are not accepted on the tracking socket");
        }

        return message;
    }

    private UsernamePasswordAuthenticationToken authenticate(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith(BEARER_PREFIX)) {
            throw new AuthenticationCredentialsNotFoundException("A Bearer token is required for WebSocket access");
        }

        try {
            String token = authorizationHeader.substring(BEARER_PREFIX.length());
            String email = jwtUtil.extractEmail(token);
            UserDetails userDetails = userDetailsService.loadUserByUsername(email);
            if (!jwtUtil.isTokenValid(token, userDetails.getUsername())) {
                throw new AuthenticationCredentialsNotFoundException("The WebSocket token is invalid or expired");
            }
            return new UsernamePasswordAuthenticationToken(
                    userDetails, null, userDetails.getAuthorities());
        } catch (AuthenticationCredentialsNotFoundException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new AuthenticationCredentialsNotFoundException(
                    "The WebSocket token is invalid or expired", exception);
        }
    }

    private void authorizeSubscription(StompHeaderAccessor accessor) {
        Principal user = accessor.getUser();
        if (user == null) {
            throw new AuthenticationCredentialsNotFoundException("Authenticate before subscribing");
        }

        String destination = accessor.getDestination();
        Matcher matcher = destination == null
                ? SHIPMENT_LOCATION_TOPIC.matcher("")
                : SHIPMENT_LOCATION_TOPIC.matcher(destination);
        if (!matcher.matches()) {
            throw new AccessDeniedException("Only shipment location channels can be subscribed to");
        }

        Long shipmentId = Long.valueOf(matcher.group(1));
        try {
            shipmentService.getShipmentById(shipmentId, user.getName());
        } catch (RuntimeException exception) {
            throw new AccessDeniedException("You do not have access to this shipment channel", exception);
        }
    }
}
