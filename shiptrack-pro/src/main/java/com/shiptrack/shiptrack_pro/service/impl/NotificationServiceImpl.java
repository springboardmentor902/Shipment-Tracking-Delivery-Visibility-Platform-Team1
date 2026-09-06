package com.shiptrack.shiptrack_pro.service.impl;

import com.shiptrack.shiptrack_pro.dto.NotificationRequest;
import com.shiptrack.shiptrack_pro.dto.NotificationResponse;
import com.shiptrack.shiptrack_pro.entity.*;
import com.shiptrack.shiptrack_pro.repository.NotificationRepository;
import com.shiptrack.shiptrack_pro.repository.ShipmentRepository;
import com.shiptrack.shiptrack_pro.repository.UserRepository;
import com.shiptrack.shiptrack_pro.security.Role;
import com.shiptrack.shiptrack_pro.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final ShipmentRepository shipmentRepository;
    private final JavaMailSender mailSender;
    private final Clock clock;

    @Value("${app.notifications.duplicate-window-minutes:5}")
    private long duplicateWindowMinutes;

    @Value("${app.notifications.email-enabled:false}")
    private boolean emailEnabled;

    @Value("${app.notifications.from:}")
    private String fromAddress;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public NotificationResponse send(NotificationType type, User user, Shipment shipment) {
        LocalDateTime now = LocalDateTime.now(clock);
        LocalDateTime recentCutoff = now.minusMinutes(Math.max(1, duplicateWindowMinutes));
        var recent = notificationRepository
                .findFirstByUserIdAndShipmentIdAndTypeAndCreatedAtAfterOrderByCreatedAtDesc(
                        user.getId(), shipment.getId(), type, recentCutoff);
        if (recent.isPresent()) {
            return mapToResponse(recent.get());
        }

        Notification notification = Notification.builder()
                .user(user)
                .shipment(shipment)
                .type(type)
                .title(title(type, shipment))
                .message(message(type, shipment))
                .status(NotificationStatus.PENDING)
                .build();
        notificationRepository.save(notification);

        if (!emailEnabled) {
            notification.setStatus(NotificationStatus.FAILED);
        } else {
            try {
                deliverEmail(notification);
                notification.setStatus(NotificationStatus.SENT);
                notification.setSentAt(now);
            } catch (RuntimeException exception) {
                notification.setStatus(NotificationStatus.FAILED);
                log.warn("Email delivery failed for notification type {} and shipment {}: {}",
                        type, shipment.getId(), exception.getMessage());
            }
        }

        return mapToResponse(notificationRepository.save(notification));
    }

    @Override
    @Transactional(readOnly = true)
    public List<NotificationResponse> getForUser(String requesterEmail) {
        User user = requireUser(requesterEmail);
        return notificationRepository.findAllByUserIdOrderByCreatedAtDesc(user.getId())
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    public NotificationResponse markAsRead(Long notificationId, String requesterEmail) {
        User user = requireUser(requesterEmail);
        Notification notification = notificationRepository.findOneByIdAndUserId(notificationId, user.getId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Notification not found"));
        if (notification.getReadAt() == null) {
            notification.setReadAt(LocalDateTime.now(clock));
        }
        return mapToResponse(notificationRepository.save(notification));
    }

    @Override
    public NotificationResponse create(NotificationRequest request, String requesterEmail) {
        User requester = requireUser(requesterEmail);
        if (parseRole(requester) != Role.ADMINISTRATOR) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "Only an administrator can create a notification manually");
        }
        User recipient = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        Shipment shipment = shipmentRepository.findOneById(request.getShipmentId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Shipment not found"));
        return send(request.getType(), recipient, shipment);
    }

    private void deliverEmail(Notification notification) {
        Map<String, String> recipients = new LinkedHashMap<>();
        addRecipient(recipients, notification.getUser().getEmail());
        addRecipient(recipients, notification.getShipment().getReceiverEmail());
        if (recipients.isEmpty()) {
            throw new IllegalStateException("Recipient emails are unavailable");
        }

        RuntimeException deliveryFailure = null;
        for (String recipient : recipients.values()) {
            try {
                sendEmail(notification, recipient);
            } catch (RuntimeException exception) {
                if (deliveryFailure == null) {
                    deliveryFailure = exception;
                } else if (deliveryFailure != exception) {
                    deliveryFailure.addSuppressed(exception);
                }
            }
        }
        if (deliveryFailure != null) {
            throw deliveryFailure;
        }
    }

    private void sendEmail(Notification notification, String recipient) {
        SimpleMailMessage email = new SimpleMailMessage();
        if (StringUtils.hasText(fromAddress)) {
            email.setFrom(fromAddress.trim());
        }
        email.setTo(recipient);
        email.setSubject(notification.getTitle());
        email.setText(notification.getMessage());
        mailSender.send(email);
    }

    private void addRecipient(Map<String, String> recipients, String email) {
        if (StringUtils.hasText(email)) {
            String trimmed = email.trim();
            recipients.putIfAbsent(trimmed.toLowerCase(Locale.ROOT), trimmed);
        }
    }

    private String title(NotificationType type, Shipment shipment) {
        return switch (type) {
            case SHIPMENT_UPDATE -> "Shipment update: " + shipment.getTrackingNumber();
            case DELAY_WARNING -> "Delay warning: " + shipment.getTrackingNumber();
        };
    }

    private String message(NotificationType type, Shipment shipment) {
        return switch (type) {
            case SHIPMENT_UPDATE -> "Shipment " + shipment.getTrackingNumber()
                    + " is now " + readable(shipment.getStatus().name())
                    + ". Current location: "
                    + (StringUtils.hasText(shipment.getCurrentLocation())
                    ? shipment.getCurrentLocation() : "not available") + ".";
            case DELAY_WARNING -> "Shipment " + shipment.getTrackingNumber()
                    + " has a high delay risk. Please check the latest ETA and tracking details.";
        };
    }

    private String readable(String value) {
        String normalized = value.toLowerCase().replace('_', ' ');
        return Character.toUpperCase(normalized.charAt(0)) + normalized.substring(1);
    }

    private User requireUser(String email) {
        return userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "Authenticated user no longer exists"));
    }

    private Role parseRole(User user) {
        try {
            return Role.valueOf(user.getRole());
        } catch (IllegalArgumentException | NullPointerException exception) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "User has an unsupported role");
        }
    }

    private NotificationResponse mapToResponse(Notification notification) {
        return NotificationResponse.builder()
                .id(notification.getId())
                .userId(notification.getUser().getId())
                .shipmentId(notification.getShipment().getId())
                .trackingNumber(notification.getShipment().getTrackingNumber())
                .type(notification.getType())
                .title(notification.getTitle())
                .message(notification.getMessage())
                .status(notification.getStatus())
                .sentAt(notification.getSentAt())
                .readAt(notification.getReadAt())
                .createdAt(notification.getCreatedAt())
                .unread(notification.getReadAt() == null)
                .build();
    }
}
