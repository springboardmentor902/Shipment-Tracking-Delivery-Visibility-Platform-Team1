package com.shiptrack.shiptrack_pro.controller;

import com.shiptrack.shiptrack_pro.entity.*;
import com.shiptrack.shiptrack_pro.repository.*;
import com.shiptrack.shiptrack_pro.service.TrackingEventService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class NotificationApiIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private NotificationRepository notificationRepository;
    @Autowired private ETAPredictionRepository etaPredictionRepository;
    @Autowired private TrackingEventRepository trackingEventRepository;
    @Autowired private RouteRepository routeRepository;
    @Autowired private ShipmentRepository shipmentRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private TrackingEventService trackingEventService;

    private User customer;
    private User otherCustomer;
    private User administrator;
    private Shipment shipment;

    @BeforeEach
    void setUp() {
        notificationRepository.deleteAll();
        etaPredictionRepository.deleteAll();
        trackingEventRepository.deleteAll();
        routeRepository.deleteAll();
        shipmentRepository.deleteAll();
        userRepository.deleteAll();

        customer = saveUser("Customer", "customer.notification@example.com", "CUSTOMER");
        otherCustomer = saveUser("Other", "other.notification@example.com", "CUSTOMER");
        administrator = saveUser("Admin", "admin.notification@example.com", "ADMINISTRATOR");
        shipment = shipmentRepository.save(shipment(customer));
    }

    @Test
    void trackingTriggerListReadAndManualCreationAreAuthorized() throws Exception {
        trackingEventService.record(
                shipment,
                TrackingEventType.STATUS_CHANGED,
                ShipmentStatus.IN_TRANSIT,
                "Delhi hub",
                null,
                null
        );
        trackingEventService.record(
                shipment,
                TrackingEventType.LOCATION_UPDATED,
                ShipmentStatus.IN_TRANSIT,
                "Delhi hub",
                null,
                null
        );

        assertThat(notificationRepository.count()).isEqualTo(1);
        Notification generated = notificationRepository.findAll().get(0);
        assertThat(generated.getType()).isEqualTo(NotificationType.SHIPMENT_UPDATE);

        mockMvc.perform(get("/api/notifications")
                        .with(user(customer.getEmail()).roles("CUSTOMER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].unread").value(true));

        mockMvc.perform(get("/api/notifications")
                        .with(user(otherCustomer.getEmail()).roles("CUSTOMER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        mockMvc.perform(patch("/api/notifications/{id}/read", generated.getId())
                        .with(user(otherCustomer.getEmail()).roles("CUSTOMER")))
                .andExpect(status().isNotFound());

        mockMvc.perform(patch("/api/notifications/{id}/read", generated.getId())
                        .with(user(customer.getEmail()).roles("CUSTOMER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unread").value(false))
                .andExpect(jsonPath("$.readAt").exists());

        String request = """
                {
                  "type": "DELAY_WARNING",
                  "userId": %d,
                  "shipmentId": %d
                }
                """.formatted(customer.getId(), shipment.getId());

        mockMvc.perform(post("/api/notification")
                        .with(user(customer.getEmail()).roles("CUSTOMER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/notification")
                        .with(user(administrator.getEmail()).roles("ADMINISTRATOR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.type").value("DELAY_WARNING"));

        mockMvc.perform(get("/api/notifications")
                        .with(user(customer.getEmail()).roles("CUSTOMER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].type").value("DELAY_WARNING"));
    }

    private User saveUser(String name, String email, String role) {
        return userRepository.save(User.builder()
                .fullName(name)
                .email(email)
                .password("encoded-for-mock-auth")
                .role(role)
                .status("ACTIVE")
                .build());
    }

    private Shipment shipment(User creator) {
        Shipment record = Shipment.builder()
                .trackingNumber("SHP-NOTIFICATION")
                .senderName("Sender")
                .senderAddress("Delhi")
                .receiverName("Receiver")
                .receiverEmail("receiver@example.com")
                .receiverAddress("Bengaluru")
                .pickupAddress("Delhi")
                .deliveryAddress("Bengaluru")
                .priority(ShipmentPriority.EXPRESS)
                .packageDescription("Documents")
                .weightKg(new BigDecimal("1.00"))
                .dimensions("10 x 10 x 10 cm")
                .quantity(1)
                .declaredValue(new BigDecimal("500.00"))
                .fragile(false)
                .status(ShipmentStatus.IN_TRANSIT)
                .currentLocation("Delhi")
                .estimatedDeliveryDate(LocalDate.now().plusDays(2))
                .createdBy(creator)
                .build();
        record.addPackage(com.shiptrack.shiptrack_pro.entity.Package.builder()
                .description("Documents")
                .weightKg(new BigDecimal("1.00"))
                .dimensions("10 x 10 x 10 cm")
                .quantity(1)
                .declaredValue(new BigDecimal("500.00"))
                .fragile(false)
                .build());
        return record;
    }
}
