package com.shiptrack.shiptrack_pro.controller;

import com.shiptrack.shiptrack_pro.entity.Shipment;
import com.shiptrack.shiptrack_pro.entity.ShipmentStatus;
import com.shiptrack.shiptrack_pro.entity.User;
import com.shiptrack.shiptrack_pro.repository.ShipmentRepository;
import com.shiptrack.shiptrack_pro.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class ShipmentApiIntegrationTest {

    private static final String CREATE_REQUEST = """
            {
              "senderName": "Infosys Springboard",
              "senderPhone": "+91 99999 11111",
              "senderAddress": "Hinjewadi Phase 1, Pune",
              "receiverName": "Training Centre",
              "receiverPhone": "+91 99999 22222",
              "receiverEmail": "training@example.com",
              "receiverAddress": "Electronic City, Bengaluru",
              "pickupAddress": "Pune, Maharashtra",
              "deliveryAddress": "Bengaluru, Karnataka",
              "priority": "EXPRESS",
              "packageDescription": "Training material",
              "weightKg": 4.50,
              "dimensions": "40 x 30 x 20 cm",
              "quantity": 2,
              "declaredValue": 2500.00,
              "fragile": false
            }
            """;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ShipmentRepository shipmentRepository;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        shipmentRepository.deleteAll();
        userRepository.deleteAll();
        userRepository.save(User.builder()
                .fullName("Business Client")
                .email("business@example.com")
                .password("encoded-for-mock-auth")
                .role("BUSINESS_CLIENT")
                .status("ACTIVE")
                .build());
    }

    @Test
    void backendDoesNotServeLegacyFrontendPages() throws Exception {
        mockMvc.perform(get("/login.html"))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/register.html"))
                .andExpect(status().isForbidden());
    }

    @Test
    void publicRegistrationCannotCreateAdministrator() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "fullName": "Attempted Admin",
                                  "email": "attempted.admin@example.com",
                                  "password": "Passw0rd123",
                                  "role": "ADMINISTRATOR"
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value(
                        "Administrator accounts cannot be created through registration."));
    }

    @Test
    @WithMockUser(username = "customer@example.com", roles = "CUSTOMER")
    void customerCannotManageShipments() throws Exception {
        mockMvc.perform(post("/api/shipments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CREATE_REQUEST))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "business@example.com", roles = "BUSINESS_CLIENT")
    void businessClientCanCompleteShipmentApiFlow() throws Exception {
        mockMvc.perform(post("/api/shipments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CREATE_REQUEST))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.trackingNumber").value(
                        org.hamcrest.Matchers.startsWith("SHP-")))
                .andExpect(jsonPath("$.status").value("CREATED"))
                .andExpect(jsonPath("$.priority").value("EXPRESS"))
                .andExpect(jsonPath("$.createdBy").value("business@example.com"));

        Shipment shipment = shipmentRepository.findAll().get(0);

        mockMvc.perform(get("/api/shipments/{id}", shipment.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(shipment.getId()))
                .andExpect(jsonPath("$.receiverEmail").value("training@example.com"));

        mockMvc.perform(patch("/api/shipments/{id}/status", shipment.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "PICKED_UP",
                                  "currentLocation": "Pune sorting hub"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PICKED_UP"))
                .andExpect(jsonPath("$.currentLocation").value("Pune sorting hub"));

        mockMvc.perform(get("/api/shipments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(shipment.getId()));

        mockMvc.perform(delete("/api/shipments/{id}", shipment.getId())
                        .param("reason", "Client changed the delivery plan"))
                .andExpect(status().isNoContent());

        Shipment cancelled = shipmentRepository.findById(shipment.getId()).orElseThrow();
        assertThat(cancelled.getStatus()).isEqualTo(ShipmentStatus.CANCELLED);
        assertThat(cancelled.getCancellationReason()).isEqualTo("Client changed the delivery plan");
    }
}
