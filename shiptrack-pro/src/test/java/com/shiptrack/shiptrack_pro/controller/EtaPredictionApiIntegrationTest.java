package com.shiptrack.shiptrack_pro.controller;

import com.shiptrack.shiptrack_pro.entity.*;
import com.shiptrack.shiptrack_pro.repository.*;
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
class EtaPredictionApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ETAPredictionRepository etaPredictionRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private TrackingEventRepository trackingEventRepository;

    @Autowired
    private RouteRepository routeRepository;

    @Autowired
    private ShipmentRepository shipmentRepository;

    @Autowired
    private UserRepository userRepository;

    private User customer;
    private User otherCustomer;
    private User operator;
    private Shipment shipment;

    @BeforeEach
    void setUp() {
        notificationRepository.deleteAll();
        etaPredictionRepository.deleteAll();
        trackingEventRepository.deleteAll();
        routeRepository.deleteAll();
        shipmentRepository.deleteAll();
        userRepository.deleteAll();

        customer = saveUser("Business Customer", "business@example.com", "BUSINESS_CLIENT");
        otherCustomer = saveUser("Other Customer", "other@example.com", "CUSTOMER");
        operator = saveUser("Operator", "operator@example.com", "LOGISTICS_OPERATOR");
        shipment = shipmentRepository.save(shipment(customer, operator));
    }

    @Test
    void routeAndTrackingEventsAutomaticallyMaintainAuthorizedEtaPrediction() throws Exception {
        mockMvc.perform(post("/api/routes")
                        .with(user(operator.getEmail()).roles("LOGISTICS_OPERATOR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "shipmentId": %d,
                                  "trafficCondition": "SEVERE",
                                  "driverName": "Amit Kumar"
                                }
                                """.formatted(shipment.getId())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.trafficCondition").value("SEVERE"));

        assertThat(trackingEventRepository
                .findAllByShipmentIdOrderByRecordedAtAsc(shipment.getId()))
                .extracting(TrackingEvent::getEventType)
                .containsExactly(TrackingEventType.ROUTE_CREATED);
        assertThat(etaPredictionRepository.findByShipmentId(shipment.getId())).isPresent();
        assertThat(notificationRepository.findAll())
                .extracting(Notification::getType)
                .contains(NotificationType.SHIPMENT_UPDATE, NotificationType.DELAY_WARNING);

        mockMvc.perform(get("/api/eta/{shipmentId}", shipment.getId())
                        .with(user(customer.getEmail()).roles("BUSINESS_CLIENT")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.shipmentId").value(shipment.getId()))
                .andExpect(jsonPath("$.predictedDeliveryTime").exists())
                .andExpect(jsonPath("$.delayRiskScore").isNumber())
                .andExpect(jsonPath("$.confidenceScore").isNumber())
                .andExpect(jsonPath("$.factors").value(org.hamcrest.Matchers.containsString("Traffic severe")));

        mockMvc.perform(get("/api/eta/{shipmentId}", shipment.getId())
                        .with(user(otherCustomer.getEmail()).roles("CUSTOMER")))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/eta/{shipmentId}/predict", shipment.getId())
                        .with(user(customer.getEmail()).roles("BUSINESS_CLIENT")))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/eta/{shipmentId}/predict", shipment.getId())
                        .with(user(operator.getEmail()).roles("LOGISTICS_OPERATOR")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.delayRiskScore").isNumber());

        mockMvc.perform(patch("/api/shipments/{shipmentId}/status", shipment.getId())
                        .with(user(operator.getEmail()).roles("LOGISTICS_OPERATOR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "PICKED_UP",
                                  "currentLocation": "Pune sorting hub"
                                }
                                """))
                .andExpect(status().isOk());

        assertThat(trackingEventRepository
                .findAllByShipmentIdOrderByRecordedAtAsc(shipment.getId()))
                .extracting(TrackingEvent::getEventType)
                .containsExactly(TrackingEventType.ROUTE_CREATED, TrackingEventType.STATUS_CHANGED);
        ETAPrediction recalculated = etaPredictionRepository
                .findByShipmentId(shipment.getId()).orElseThrow();
        assertThat(recalculated.getFactors()).contains("2 tracking event(s) considered");
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

    private Shipment shipment(User creator, User assignedOperator) {
        Shipment record = Shipment.builder()
                .trackingNumber("SHP-ETA12345678")
                .senderName("Sender")
                .senderAddress("Pune, Maharashtra")
                .receiverName("Receiver")
                .receiverEmail("receiver@example.com")
                .receiverAddress("Bengaluru, Karnataka")
                .pickupAddress("Pune, Maharashtra")
                .deliveryAddress("Bengaluru, Karnataka")
                .priority(ShipmentPriority.EXPRESS)
                .packageDescription("Documents")
                .weightKg(new BigDecimal("1.50"))
                .dimensions("30 x 20 x 10 cm")
                .quantity(1)
                .declaredValue(new BigDecimal("1500.00"))
                .fragile(false)
                .status(ShipmentStatus.CREATED)
                .currentLocation("Pune, Maharashtra")
                .estimatedDeliveryDate(LocalDate.now().plusDays(2))
                .createdBy(creator)
                .assignedOperator(assignedOperator)
                .build();
        record.addPackage(com.shiptrack.shiptrack_pro.entity.Package.builder()
                .description("Documents")
                .weightKg(new BigDecimal("1.50"))
                .dimensions("30 x 20 x 10 cm")
                .quantity(1)
                .declaredValue(new BigDecimal("1500.00"))
                .fragile(false)
                .build());
        return record;
    }
}
