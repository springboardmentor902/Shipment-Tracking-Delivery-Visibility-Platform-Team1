package com.shiptrack.shiptrack_pro.controller;

import com.shiptrack.shiptrack_pro.entity.Package;
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
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ShipmentVisibilityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ShipmentRepository shipmentRepository;

    @Autowired
    private RouteRepository routeRepository;

    @Autowired
    private UserRepository userRepository;

    private User firstCustomer;
    private User secondCustomer;
    private User operator;

    @BeforeEach
    void setUp() {
        routeRepository.deleteAll();
        shipmentRepository.deleteAll();
        userRepository.deleteAll();

        firstCustomer = saveUser("First Customer", "customer.one@example.com", "CUSTOMER");
        secondCustomer = saveUser("Second Customer", "customer.two@example.com", "CUSTOMER");
        operator = saveUser("Assigned Operator", "operator@example.com", "LOGISTICS_OPERATOR");
        saveUser("Administrator", "admin@example.com", "ADMINISTRATOR");

        shipmentRepository.save(shipment("SHP-CUSTOMER001", firstCustomer, operator));
        shipmentRepository.save(shipment("SHP-CUSTOMER002", secondCustomer, null));
        shipmentRepository.save(shipment("SHP-OPERATOR001", secondCustomer, operator));
    }

    @Test
    void shipmentListIsFilteredForCustomerOperatorAndAdministrator() throws Exception {
        mockMvc.perform(get("/api/shipments")
                        .with(user(firstCustomer.getEmail()).roles("CUSTOMER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].trackingNumber").value("SHP-CUSTOMER001"));

        mockMvc.perform(get("/api/shipments")
                        .with(user(operator.getEmail()).roles("LOGISTICS_OPERATOR")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));

        mockMvc.perform(get("/api/shipments")
                        .with(user("admin@example.com").roles("ADMINISTRATOR")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3));
    }

    @Test
    void customerCannotReadAnotherCustomersShipment() throws Exception {
        Shipment otherCustomersShipment = shipmentRepository
                .findAllByCreatedByIdOrderByCreatedAtDesc(secondCustomer.getId()).get(0);

        mockMvc.perform(get("/api/shipments/{id}", otherCustomersShipment.getId())
                        .with(user(firstCustomer.getEmail()).roles("CUSTOMER")))
                .andExpect(status().isForbidden());
    }

    @Test
    void administratorCanAssignShipmentToOperator() throws Exception {
        Shipment unassignedShipment = shipmentRepository
                .findAllByCreatedByIdOrderByCreatedAtDesc(secondCustomer.getId())
                .stream()
                .filter(candidate -> candidate.getAssignedOperator() == null)
                .findFirst()
                .orElseThrow();

        mockMvc.perform(patch("/api/shipments/{id}/operator", unassignedShipment.getId())
                        .with(user("admin@example.com").roles("ADMINISTRATOR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"operatorId\":" + operator.getId() + "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assignedOperatorId").value(operator.getId()))
                .andExpect(jsonPath("$.assignedOperator").value(operator.getEmail()));

        mockMvc.perform(get("/api/shipments")
                        .with(user(operator.getEmail()).roles("LOGISTICS_OPERATOR")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3));
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

    private Shipment shipment(String trackingNumber, User creator, User assignedOperator) {
        Shipment shipment = Shipment.builder()
                .trackingNumber(trackingNumber)
                .senderName(creator.getFullName())
                .senderAddress("Pune, Maharashtra")
                .receiverName("Receiver")
                .receiverEmail("receiver@example.com")
                .receiverAddress("Bengaluru, Karnataka")
                .pickupAddress("Pune, Maharashtra")
                .deliveryAddress("Bengaluru, Karnataka")
                .priority(ShipmentPriority.STANDARD)
                .packageDescription("Documents")
                .weightKg(new BigDecimal("1.50"))
                .dimensions("30 x 20 x 10 cm")
                .quantity(1)
                .declaredValue(new BigDecimal("500.00"))
                .fragile(false)
                .status(ShipmentStatus.CREATED)
                .currentLocation("Pune, Maharashtra")
                .estimatedDeliveryDate(LocalDate.now().plusDays(5))
                .createdBy(creator)
                .assignedOperator(assignedOperator)
                .build();
        shipment.addPackage(Package.builder()
                .description("Documents")
                .weightKg(new BigDecimal("1.50"))
                .dimensions("30 x 20 x 10 cm")
                .quantity(1)
                .declaredValue(new BigDecimal("500.00"))
                .fragile(false)
                .build());
        return shipment;
    }
}
