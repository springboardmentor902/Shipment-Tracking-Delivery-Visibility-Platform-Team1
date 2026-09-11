package com.shiptrack.shiptrack_pro.service;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class OpenRouteService {

    @Value("${openrouteservice.api-key:}")
    private String apiKey;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String GEOCODE_URL =
            "https://api.openrouteservice.org/geocode/search";

    private static final String DIRECTIONS_URL =
            "https://api.openrouteservice.org/v2/directions/driving-car";

    // ADDRESS -> LONGITUDE + LATITUDE
    public double[] geocode(String address) {

        if (apiKey == null || apiKey.isBlank()) {
            throw new RuntimeException(
                    "OpenRouteService API key is not configured"
            );
        }

        String url = GEOCODE_URL
                + "?api_key=" + apiKey
                + "&text=" + address.replace(" ", "%20")
                + "&size=1";

        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<String> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                String.class
        );

        try {
            JsonNode root = objectMapper.readTree(response.getBody());

            JsonNode features = root.path("features");

            if (features.isEmpty()) {
                throw new RuntimeException(
                        "Address not found: " + address
                );
            }

            JsonNode coordinates = features
                    .get(0)
                    .path("geometry")
                    .path("coordinates");

            double longitude = coordinates.get(0).asDouble();
            double latitude = coordinates.get(1).asDouble();

            return new double[]{
                    longitude,
                    latitude
            };

        } catch (Exception e) {
            throw new RuntimeException(
                    "Unable to geocode address: " + address,
                    e
            );
        }
    }

    // COORDINATES -> DISTANCE + ETA
    public RouteDetails getRouteDetails(
            double originLongitude,
            double originLatitude,
            double destinationLongitude,
            double destinationLatitude) {

        if (apiKey == null || apiKey.isBlank()) {
            throw new RuntimeException(
                    "OpenRouteService API key is not configured"
            );
        }

        HttpHeaders headers = new HttpHeaders();

        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", apiKey);

        Map<String, Object> body = new HashMap<>();

        body.put(
                "coordinates",
                new double[][]{
                        {
                                originLongitude,
                                originLatitude
                        },
                        {
                                destinationLongitude,
                                destinationLatitude
                        }
                }
        );

        HttpEntity<Map<String, Object>> entity =
                new HttpEntity<>(body, headers);

        ResponseEntity<String> response = restTemplate.exchange(
                DIRECTIONS_URL,
                HttpMethod.POST,
                entity,
                String.class
        );

        try {
            JsonNode root =
                    objectMapper.readTree(response.getBody());

            JsonNode routes = root.path("routes");

            if (routes.isEmpty()) {
                throw new RuntimeException(
                        "Route could not be calculated"
                );
            }

            JsonNode summary = routes
                    .get(0)
                    .path("summary");

            double distanceMeters =
                    summary.path("distance").asDouble();

            double durationSeconds =
                    summary.path("duration").asDouble();

            double distanceKm =
                    distanceMeters / 1000.0;

            int estimatedMinutes =
                    (int) Math.ceil(
                            durationSeconds / 60.0
                    );

            return new RouteDetails(
                    distanceKm,
                    estimatedMinutes
            );

        } catch (Exception e) {
            throw new RuntimeException(
                    "Unable to calculate route details",
                    e
            );
        }
    }

    public record RouteDetails(
            double distanceKm,
            int estimatedMinutes
    ) {
    }
}