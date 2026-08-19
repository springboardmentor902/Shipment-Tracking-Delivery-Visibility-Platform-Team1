package com.shiptrack.shiptrack_pro.controller;

import com.shiptrack.shiptrack_pro.entity.Package;
import com.shiptrack.shiptrack_pro.entity.Route;
import com.shiptrack.shiptrack_pro.entity.Shipment;
import com.shiptrack.shiptrack_pro.entity.ShipmentPriority;
import com.shiptrack.shiptrack_pro.entity.ShipmentStatus;
import com.shiptrack.shiptrack_pro.entity.User;
import com.shiptrack.shiptrack_pro.repository.RouteRepository;
import com.shiptrack.shiptrack_pro.repository.ShipmentRepository;
import com.shiptrack.shiptrack_pro.repository.UserRepository;
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
class RouteApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RouteRepository routeRepository;

    @Autowired
    private ShipmentRepository shipmentRepository;

    @Autowired
    private UserRepository userRepository;

    private User customer;
    private User operator;
    private Shipment shipment;

    @BeforeEach
    void setUp() {
        routeRepository.deleteAll();
        shipmentRepository.deleteAll();
        userRepository.deleteAll();

        customer = saveUser("Customer", "customer@example.com", "CUSTOMER");
        operator = saveUser("Operator", "operator@example.com", "LOGISTICS_OPERATOR");
        shipment = shipmentRepository.save(shipment(customer, operator));
    }

    @Test
    void operatorCanCreateRouteChangeDriverAndCustomerCanFetchOwnRoute() throws Exception {
        mockMvc.perform(post("/api/routes")
                        .with(user(customer.getEmail()).roles("CUSTOMER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"shipmentId\":" + shipment.getId() + "}"))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/routes")
                        .with(user(operator.getEmail()).roles("LOGISTICS_OPERATOR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "shipmentId": %d,
                                  "driverName": "Amit Kumar",
                                  "driverPhone": "+91 90000 00000",
                                  "vehicleNumber": "MH12AB1234"
                                }
                                """.formatted(shipment.getId())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.shipmentId").value(shipment.getId()))
                .andExpect(jsonPath("$.originAddress").value("Pune, Maharashtra"))
                .andExpect(jsonPath("$.destinationAddress").value("Bengaluru, Karnataka"))
                .andExpect(jsonPath("$.distanceKm").doesNotExist())
                .andExpect(jsonPath("$.estimatedTimeMinutes").doesNotExist());

        Route route = routeRepository.findByShipmentId(shipment.getId()).orElseThrow();

        mockMvc.perform(patch("/api/routes/{routeId}/driver", route.getId())
                        .with(user(operator.getEmail()).roles("LOGISTICS_OPERATOR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "driverName": "Neha Singh",
                                  "driverPhone": "+91 91111 11111",
                                  "vehicleNumber": "KA01CD5678"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.driverName").value("Neha Singh"))
                .andExpect(jsonPath("$.vehicleNumber").value("KA01CD5678"));

        mockMvc.perform(get("/api/routes/{shipmentId}", shipment.getId())
                        .with(user(customer.getEmail()).roles("CUSTOMER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.shipmentId").value(shipment.getId()))
                .andExpect(jsonPath("$.driverName").value("Neha Singh"));

        assertThat(routeRepository.count()).isEqualTo(1);
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
        Shipment shipment = Shipment.builder()
                .trackingNumber("SHP-ROUTE123456")
                .senderName("Sender")
                .senderAddress("Pune, Maharashtra")
                .receiverName("Receiver")
                .receiverEmail("receiver@example.com")
                .receiverAddress("Bengaluru, Karnataka")
                .pickupAddress("Pune, Maharashtra")
                .deliveryAddress("Bengaluru, Karnataka")
                .priority(ShipmentPriority.EXPRESS)
                .packageDescription("Electronics")
                .weightKg(new BigDecimal("2.50"))
                .dimensions("30 x 20 x 15 cm")
                .quantity(1)
                .declaredValue(new BigDecimal("15000.00"))
                .fragile(true)
                .status(ShipmentStatus.CREATED)
                .currentLocation("Pune, Maharashtra")
                .estimatedDeliveryDate(LocalDate.now().plusDays(2))
                .createdBy(creator)
                .assignedOperator(assignedOperator)
                .build();
        shipment.addPackage(Package.builder()
                .description("Electronics")
                .weightKg(new BigDecimal("2.50"))
                .dimensions("30 x 20 x 15 cm")
                .quantity(1)
                .declaredValue(new BigDecimal("15000.00"))
                .fragile(true)
                .build());
        return shipment;
    }
}
