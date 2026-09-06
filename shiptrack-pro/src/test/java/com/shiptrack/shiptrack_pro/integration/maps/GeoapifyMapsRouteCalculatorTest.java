package com.shiptrack.shiptrack_pro.integration.maps;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class GeoapifyMapsRouteCalculatorTest {

    private HttpServer server;
    private String baseUrl;

    @BeforeEach
    void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/geocode", exchange -> {
            String query = exchange.getRequestURI().getRawQuery();
            if (query.contains("kormangalla")) {
                respond(exchange, 200, "{\"results\":[]}");
                return;
            }
            boolean pune = query.contains("Pune");
            String location = pune
                    ? "{\"lat\":18.5204303,\"lon\":73.8567437}"
                    : "{\"lat\":12.9715987,\"lon\":77.5945660}";
            respond(exchange, 200, "{\"results\":[" + location + "]}");
        });
        server.createContext("/autocomplete", exchange -> respond(exchange, 200,
                "{\"results\":[{\"lat\":12.9357366,\"lon\":77.624081}]}"));
        server.createContext("/routing", exchange -> respond(exchange, 200, """
                {
                  "results": [{
                    "distance": 841420,
                    "time": 55800
                  }]
                }
                """));
        server.start();
        baseUrl = "http://localhost:" + server.getAddress().getPort();
    }

    @AfterEach
    void tearDown() {
        server.stop(0);
    }

    @Test
    void geocodesBothAddressesAndReadsRoutingMetrics() {
        GeoapifyMapsRouteCalculator calculator = new GeoapifyMapsRouteCalculator(
                JsonMapper.builder().build(),
                "test-api-key",
                "test-api-key",
                baseUrl + "/geocode",
                baseUrl + "/autocomplete",
                baseUrl + "/routing",
                "in");

        RouteCalculation result = calculator.calculate(
                "Pune, Maharashtra", "Bengaluru, Karnataka");

        assertThat(result.origin().latitude()).isEqualByComparingTo("18.5204303");
        assertThat(result.origin().longitude()).isEqualByComparingTo("73.8567437");
        assertThat(result.destination().latitude()).isEqualByComparingTo("12.9715987");
        assertThat(result.destination().longitude()).isEqualByComparingTo("77.5945660");
        assertThat(result.distanceKm()).isEqualByComparingTo("841.42");
        assertThat(result.estimatedTimeMinutes()).isEqualTo(930L);
        assertThat(result.hasCompleteMetrics()).isTrue();
    }

    @Test
    void usesAutocompleteWhenExactSearchCannotResolveMisspelledAddress() {
        GeoapifyMapsRouteCalculator calculator = new GeoapifyMapsRouteCalculator(
                JsonMapper.builder().build(),
                "test-api-key",
                "test-api-key",
                baseUrl + "/geocode",
                baseUrl + "/autocomplete",
                baseUrl + "/routing",
                "in");

        RouteCalculation result = calculator.calculate("Pune", "kormangalla");

        assertThat(result.destination().latitude()).isEqualByComparingTo("12.9357366");
        assertThat(result.destination().longitude()).isEqualByComparingTo("77.624081");
        assertThat(result.hasCompleteMetrics()).isTrue();
    }

    private void respond(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }
}
