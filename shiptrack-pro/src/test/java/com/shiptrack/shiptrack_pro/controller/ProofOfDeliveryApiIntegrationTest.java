package com.shiptrack.shiptrack_pro.controller;

import com.shiptrack.shiptrack_pro.entity.*;
import com.shiptrack.shiptrack_pro.entity.Package;
import com.shiptrack.shiptrack_pro.repository.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class ProofOfDeliveryApiIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired ProofOfDeliveryRepository proofRepository;
    @Autowired NotificationRepository notificationRepository;
    @Autowired ETAPredictionRepository etaPredictionRepository;
    @Autowired TrackingEventRepository trackingEventRepository;
    @Autowired RouteRepository routeRepository;
    @Autowired ShipmentRepository shipmentRepository;
    @Autowired UserRepository userRepository;

    private User customer;
    private User otherCustomer;
    private User operator;
    private User otherOperator;
    private User support;
    private User admin;
    private Shipment shipment;

    @BeforeEach
    void setUp() {
        proofRepository.deleteAll();
        notificationRepository.deleteAll();
        etaPredictionRepository.deleteAll();
        trackingEventRepository.deleteAll();
        routeRepository.deleteAll();
        shipmentRepository.deleteAll();
        userRepository.deleteAll();

        customer = saveUser("Customer", "pod.customer@example.com", "CUSTOMER");
        otherCustomer = saveUser("Other Customer", "other.customer@example.com", "CUSTOMER");
        operator = saveUser("Assigned Operator", "pod.operator@example.com", "LOGISTICS_OPERATOR");
        otherOperator = saveUser("Other Operator", "other.operator@example.com", "LOGISTICS_OPERATOR");
        support = saveUser("Support Agent", "pod.support@example.com", "SUPPORT_AGENT");
        admin = saveUser("Administrator", "pod.admin@example.com", "ADMINISTRATOR");
        shipment = shipmentRepository.save(shipment(customer, operator));
    }

    @AfterEach
    void cleanUploadedFiles() throws Exception {
        Path directory = Path.of("target/test-uploads/pod");
        if (Files.isDirectory(directory)) {
            try (var files = Files.list(directory)) {
                files.filter(Files::isRegularFile).forEach(path -> {
                    try {
                        Files.deleteIfExists(path);
                    } catch (Exception ignored) {
                        // Test cleanup only.
                    }
                });
            }
        }
    }

    @Test
    void assignedOperatorCanSubmitOwnerCanViewAndSupportCanVerify() throws Exception {
        mockMvc.perform(patch("/api/shipments/{id}/status", shipment.getId())
                        .with(user(operator.getEmail()).roles("LOGISTICS_OPERATOR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"DELIVERED\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Submit proof of delivery to complete this shipment"));

        mockMvc.perform(multipart("/api/pod/{shipmentId}", shipment.getId())
                        .file(image("signature", "signature.png", "image/png"))
                        .file(image("photo", "doorstep.jpg", "image/jpeg"))
                        .param("recipientName", "Riya Sharma")
                        .param("deliveryNotes", "Delivered at the reception")
                        .with(user(otherOperator.getEmail()).roles("LOGISTICS_OPERATOR")))
                .andExpect(status().isForbidden());

        mockMvc.perform(multipart("/api/pod/{shipmentId}", shipment.getId())
                        .file(image("signature", "signature.png", "image/png"))
                        .file(image("photo", "doorstep.jpg", "image/jpeg"))
                        .param("recipientName", "Riya Sharma")
                        .param("deliveryNotes", "Delivered at the reception")
                        .with(user(operator.getEmail()).roles("LOGISTICS_OPERATOR")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.shipmentId").value(shipment.getId()))
                .andExpect(jsonPath("$.recipientName").value("Riya Sharma"))
                .andExpect(jsonPath("$.signatureUrl").value(org.hamcrest.Matchers.startsWith("/api/files/")))
                .andExpect(jsonPath("$.photoUrl").value(org.hamcrest.Matchers.startsWith("/api/files/")))
                .andExpect(jsonPath("$.verificationStatus").value("PENDING"));

        Shipment delivered = shipmentRepository.findById(shipment.getId()).orElseThrow();
        assertThat(delivered.getStatus()).isEqualTo(ShipmentStatus.DELIVERED);
        assertThat(delivered.getActualDeliveryDate()).isNotNull();
        assertThat(delivered.getCurrentLocation()).isEqualTo(delivered.getDeliveryAddress());
        assertThat(trackingEventRepository.findAllByShipmentIdOrderByRecordedAtAsc(shipment.getId()))
                .extracting(TrackingEvent::getEventType)
                .contains(TrackingEventType.STATUS_CHANGED);

        mockMvc.perform(get("/api/pod/{shipmentId}", shipment.getId())
                        .with(user(otherCustomer.getEmail()).roles("CUSTOMER")))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/pod/{shipmentId}", shipment.getId())
                        .with(user(customer.getEmail()).roles("CUSTOMER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deliveryNotes").value("Delivered at the reception"));
        mockMvc.perform(get("/api/pod/{shipmentId}", shipment.getId())
                        .with(user(support.getEmail()).roles("SUPPORT_AGENT")))
                .andExpect(status().isOk());

        ProofOfDelivery savedProof = proofRepository.findByShipmentId(shipment.getId()).orElseThrow();
        String photoFile = savedProof.getPhotoUrl().substring(savedProof.getPhotoUrl().lastIndexOf('/') + 1);
        mockMvc.perform(get("/api/files/{fileName}", photoFile)
                        .with(user(otherCustomer.getEmail()).roles("CUSTOMER")))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/files/{fileName}", photoFile)
                        .with(user(customer.getEmail()).roles("CUSTOMER")))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.IMAGE_JPEG))
                .andExpect(content().bytes(new byte[]{1, 2, 3}));

        mockMvc.perform(patch("/api/pod/{shipmentId}/verify", shipment.getId())
                        .with(user(customer.getEmail()).roles("CUSTOMER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"VERIFIED\"}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(patch("/api/pod/{shipmentId}/verify", shipment.getId())
                        .with(user(support.getEmail()).roles("SUPPORT_AGENT"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "VERIFIED",
                                  "notes": "Photo and signature checked"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verificationStatus").value("VERIFIED"))
                .andExpect(jsonPath("$.verifiedBy").value(support.getEmail()))
                .andExpect(jsonPath("$.verifiedAt").exists());

        ProofOfDelivery verified = proofRepository.findByShipmentId(shipment.getId()).orElseThrow();
        assertThat(verified.getVerifiedBy().getId()).isEqualTo(support.getId());
        assertThat(verified.getVerificationNotes()).isEqualTo("Photo and signature checked");

        mockMvc.perform(post("/api/pod/{shipmentId}", shipment.getId())
                        .with(user(admin.getEmail()).roles("ADMINISTRATOR")))
                .andExpect(status().isForbidden());
    }

    private MockMultipartFile image(String field, String name, String contentType) {
        return new MockMultipartFile(field, name, contentType, new byte[]{1, 2, 3});
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
        Shipment result = Shipment.builder()
                .trackingNumber("SHP-POD123456")
                .senderName("Sender")
                .senderAddress("Pune")
                .receiverName("Riya Sharma")
                .receiverEmail("riya@example.com")
                .receiverAddress("Bengaluru")
                .pickupAddress("Pune")
                .deliveryAddress("Bengaluru")
                .priority(ShipmentPriority.EXPRESS)
                .packageDescription("Documents")
                .weightKg(new BigDecimal("1.00"))
                .dimensions("20 x 10 x 5 cm")
                .quantity(1)
                .declaredValue(new BigDecimal("500.00"))
                .fragile(false)
                .status(ShipmentStatus.OUT_FOR_DELIVERY)
                .currentLocation("Bengaluru hub")
                .estimatedDeliveryDate(LocalDate.now())
                .createdBy(creator)
                .assignedOperator(assignedOperator)
                .build();
        result.addPackage(Package.builder()
                .description("Documents")
                .weightKg(new BigDecimal("1.00"))
                .dimensions("20 x 10 x 5 cm")
                .quantity(1)
                .declaredValue(new BigDecimal("500.00"))
                .fragile(false)
                .build());
        return result;
    }
}
