package com.shiptrack.shiptrack_pro.service.impl;

import com.shiptrack.shiptrack_pro.dto.NotificationResponse;
import com.shiptrack.shiptrack_pro.entity.*;
import com.shiptrack.shiptrack_pro.repository.NotificationRepository;
import com.shiptrack.shiptrack_pro.repository.ShipmentRepository;
import com.shiptrack.shiptrack_pro.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceImplTest {

    @Mock
    private NotificationRepository notificationRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private ShipmentRepository shipmentRepository;
    @Mock
    private JavaMailSender mailSender;

    private NotificationServiceImpl service;
    private User user;
    private Shipment shipment;
    private LocalDateTime now;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(Instant.parse("2026-09-02T12:00:00Z"), ZoneOffset.UTC);
        now = LocalDateTime.now(clock);
        service = new NotificationServiceImpl(
                notificationRepository, userRepository, shipmentRepository, mailSender, clock);
        ReflectionTestUtils.setField(service, "duplicateWindowMinutes", 5L);
        ReflectionTestUtils.setField(service, "emailEnabled", true);
        ReflectionTestUtils.setField(service, "fromAddress", "updates@shiptrack.test");

        user = User.builder().id(11L).fullName("Customer").email("customer@example.com").build();
        shipment = Shipment.builder()
                .id(21L)
                .trackingNumber("SHP-NOTIFY123")
                .status(ShipmentStatus.IN_TRANSIT)
                .currentLocation("Delhi hub")
                .receiverEmail("receiver@example.com")
                .createdBy(user)
                .build();

        lenient().when(notificationRepository
                .findFirstByUserIdAndShipmentIdAndTypeAndCreatedAtAfterOrderByCreatedAtDesc(
                        eq(11L), eq(21L), any(), any()))
                .thenReturn(Optional.empty());
        lenient().when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> {
            Notification notification = invocation.getArgument(0);
            if (notification.getId() == null) notification.setId(31L);
            if (notification.getCreatedAt() == null) notification.setCreatedAt(now);
            return notification;
        });
    }

    @Test
    void sendsEmailAndMarksNotificationAsSent() {
        NotificationResponse response = service.send(NotificationType.SHIPMENT_UPDATE, user, shipment);

        assertThat(response.getStatus()).isEqualTo(NotificationStatus.SENT);
        assertThat(response.getSentAt()).isEqualTo(now);
        assertThat(response.getMessage()).contains("In transit", "Delhi hub");
        var emailCaptor = org.mockito.ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender, times(2)).send(emailCaptor.capture());
        assertThat(emailCaptor.getAllValues())
                .flatExtracting(email -> Arrays.asList(email.getTo()))
                .containsExactlyInAnyOrder("customer@example.com", "receiver@example.com");
    }

    @Test
    void sendsOnlyOneEmailWhenCreatorAndReceiverAddressMatch() {
        shipment.setReceiverEmail("CUSTOMER@example.com");

        NotificationResponse response = service.send(NotificationType.SHIPMENT_UPDATE, user, shipment);

        assertThat(response.getStatus()).isEqualTo(NotificationStatus.SENT);
        verify(mailSender).send(any(SimpleMailMessage.class));
    }

    @Test
    void returnsRecentNotificationWithoutSendingDuplicate() {
        Notification existing = Notification.builder()
                .id(41L)
                .user(user)
                .shipment(shipment)
                .type(NotificationType.SHIPMENT_UPDATE)
                .title("Existing")
                .message("Existing message")
                .status(NotificationStatus.SENT)
                .sentAt(now.minusMinutes(1))
                .createdAt(now.minusMinutes(1))
                .build();
        when(notificationRepository
                .findFirstByUserIdAndShipmentIdAndTypeAndCreatedAtAfterOrderByCreatedAtDesc(
                        eq(11L), eq(21L), eq(NotificationType.SHIPMENT_UPDATE), any()))
                .thenReturn(Optional.of(existing));

        NotificationResponse response = service.send(NotificationType.SHIPMENT_UPDATE, user, shipment);

        assertThat(response.getId()).isEqualTo(41L);
        verify(mailSender, never()).send(any(SimpleMailMessage.class));
        verify(notificationRepository, never()).save(any(Notification.class));
    }

    @Test
    void keepsFailedEmailAsAnInAppNotification() {
        doThrow(new IllegalStateException("SMTP unavailable"))
                .when(mailSender).send(any(SimpleMailMessage.class));

        NotificationResponse response = service.send(NotificationType.DELAY_WARNING, user, shipment);

        assertThat(response.getStatus()).isEqualTo(NotificationStatus.FAILED);
        assertThat(response.getSentAt()).isNull();
        assertThat(response.isUnread()).isTrue();
        verify(mailSender, times(2)).send(any(SimpleMailMessage.class));
    }
}
