package com.shiptrack.shiptrack_pro.tracking;

import com.shiptrack.shiptrack_pro.dto.DriverLocationRequest;
import com.shiptrack.shiptrack_pro.entity.Package;
import com.shiptrack.shiptrack_pro.entity.Route;
import com.shiptrack.shiptrack_pro.entity.Shipment;
import com.shiptrack.shiptrack_pro.entity.ShipmentPriority;
import com.shiptrack.shiptrack_pro.entity.ShipmentStatus;
import com.shiptrack.shiptrack_pro.entity.User;
import com.shiptrack.shiptrack_pro.repository.RouteRepository;
import com.shiptrack.shiptrack_pro.repository.ShipmentRepository;
import com.shiptrack.shiptrack_pro.repository.UserRepository;
import com.shiptrack.shiptrack_pro.security.JwtUtil;
import com.shiptrack.shiptrack_pro.service.RouteService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class LiveTrackingWebSocketIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ShipmentRepository shipmentRepository;

    @Autowired
    private RouteRepository routeRepository;

    @Autowired
    private RouteService routeService;

    @Autowired
    private JwtUtil jwtUtil;

    private WebSocketStompClient stompClient;
    private ThreadPoolTaskScheduler taskScheduler;
    private StompSession stompSession;
    private User operator;
    private Shipment shipment;
    private Route route;

    @BeforeEach
    void setUp() {
        routeRepository.deleteAll();
        shipmentRepository.deleteAll();
        userRepository.deleteAll();

        User customer = saveUser("Customer", "live.customer@example.com", "CUSTOMER");
        operator = saveUser("Operator", "live.operator@example.com", "LOGISTICS_OPERATOR");
        shipment = shipmentRepository.save(shipment(customer, operator));
        route = routeRepository.save(Route.builder()
                .shipment(shipment)
                .originAddress(shipment.getPickupAddress())
                .destinationAddress(shipment.getDeliveryAddress())
                .createdBy(operator)
                .build());
    }

    @AfterEach
    void tearDown() {
        if (stompSession != null && stompSession.isConnected()) {
            stompSession.disconnect();
        }
        if (stompClient != null) {
            stompClient.stop();
        }
        if (taskScheduler != null) {
            taskScheduler.shutdown();
        }
    }

    @Test
    void customerReceivesCommittedDriverLocationOnShipmentTopic() throws Exception {
        String customerToken = jwtUtil.generateToken("live.customer@example.com", "CUSTOMER");
        StompHeaders connectHeaders = new StompHeaders();
        connectHeaders.add("Authorization", "Bearer " + customerToken);

        taskScheduler = new ThreadPoolTaskScheduler();
        taskScheduler.setPoolSize(1);
        taskScheduler.setThreadNamePrefix("tracking-stomp-test-");
        taskScheduler.initialize();
        stompClient = new WebSocketStompClient(new StandardWebSocketClient());
        stompClient.setTaskScheduler(taskScheduler);
        stompSession = stompClient.connectAsync(
                        URI.create("ws://localhost:" + port + "/api/ws/tracking"),
                        new WebSocketHttpHeaders(),
                        connectHeaders,
                        new StompSessionHandlerAdapter() {
                        })
                .get(5, TimeUnit.SECONDS);

        CountDownLatch locationReceived = new CountDownLatch(1);
        AtomicReference<String> messageBody = new AtomicReference<>();
        stompSession.subscribe(
                TrackingLocationBroadcaster.destinationFor(shipment.getId()),
                new StompFrameHandler() {
                    @Override
                    public Type getPayloadType(StompHeaders headers) {
                        return byte[].class;
                    }

                    @Override
                    public void handleFrame(StompHeaders headers, Object payload) {
                        messageBody.set(new String((byte[]) payload, StandardCharsets.UTF_8));
                        locationReceived.countDown();
                    }
                });
        TimeUnit.MILLISECONDS.sleep(250);

        DriverLocationRequest request = new DriverLocationRequest();
        request.setLatitude(new BigDecimal("18.5314000"));
        request.setLongitude(new BigDecimal("73.8446000"));
        routeService.updateLocation(route.getId(), request, operator.getEmail());

        assertThat(locationReceived.await(5, TimeUnit.SECONDS)).isTrue();
        assertThat(messageBody.get())
                .contains("\"shipmentId\":" + shipment.getId())
                .contains("\"latitude\":18.5314000")
                .contains("\"longitude\":73.8446000");
    }

    private User saveUser(String name, String email, String role) {
        return userRepository.save(User.builder()
                .fullName(name)
                .email(email)
                .password("encoded-for-websocket-test")
                .role(role)
                .status("ACTIVE")
                .build());
    }

    private Shipment shipment(User creator, User assignedOperator) {
        Shipment record = Shipment.builder()
                .trackingNumber("SHP-LIVE123456")
                .senderName("Sender")
                .senderAddress("Pune, Maharashtra")
                .receiverName("Receiver")
                .receiverEmail("receiver@example.com")
                .receiverAddress("Mumbai, Maharashtra")
                .pickupAddress("Pune, Maharashtra")
                .deliveryAddress("Mumbai, Maharashtra")
                .priority(ShipmentPriority.EXPRESS)
                .packageDescription("Electronics")
                .weightKg(new BigDecimal("2.50"))
                .dimensions("30 x 20 x 15 cm")
                .quantity(1)
                .declaredValue(new BigDecimal("15000.00"))
                .fragile(true)
                .status(ShipmentStatus.IN_TRANSIT)
                .currentLocation("Pune, Maharashtra")
                .estimatedDeliveryDate(LocalDate.now().plusDays(2))
                .createdBy(creator)
                .assignedOperator(assignedOperator)
                .build();
        record.addPackage(Package.builder()
                .description("Electronics")
                .weightKg(new BigDecimal("2.50"))
                .dimensions("30 x 20 x 15 cm")
                .quantity(1)
                .declaredValue(new BigDecimal("15000.00"))
                .fragile(true)
                .build());
        return record;
    }
}
