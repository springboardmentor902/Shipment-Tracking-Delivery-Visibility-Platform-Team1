package com.shiptrack.shiptrack_pro.integration.maps;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Service
@Slf4j
public class GoogleMapsRouteCalculator implements MapsRouteCalculator {

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final String apiKey;
    private final String geocodingUrl;
    private final String directionsUrl;

    public GoogleMapsRouteCalculator(
            ObjectMapper objectMapper,
            @Value("${google.maps.api-key:}") String apiKey,
            @Value("${google.maps.geocoding-url:https://maps.googleapis.com/maps/api/geocode/json}")
            String geocodingUrl,
            @Value("${google.maps.directions-url:https://maps.googleapis.com/maps/api/directions/json}")
            String directionsUrl
    ) {
        this.objectMapper = objectMapper;
        this.apiKey = apiKey;
        this.geocodingUrl = geocodingUrl;
        this.directionsUrl = directionsUrl;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(3))
                .build();
    }

    @Override
    public RouteCalculation calculate(String originAddress, String destinationAddress) {
        if (!StringUtils.hasText(apiKey)) {
            log.info("GOOGLE_MAPS_API_KEY is not configured; route will be saved without map metrics");
            return RouteCalculation.empty();
        }

        Coordinates origin = geocodeSafely(originAddress);
        Coordinates destination = geocodeSafely(destinationAddress);
        if (origin == null || destination == null) {
            return new RouteCalculation(origin, destination, null, null);
        }

        try {
            URI uri = UriComponentsBuilder.fromUriString(directionsUrl)
                    .queryParam("origin", origin.latitude() + "," + origin.longitude())
                    .queryParam("destination", destination.latitude() + "," + destination.longitude())
                    .queryParam("mode", "driving")
                    .queryParam("key", apiKey)
                    .build()
                    .encode()
                    .toUri();
            JsonNode root = getJson(uri);
            if (!"OK".equals(root.path("status").asString())) {
                log.warn("Google Directions API returned status {}", root.path("status").asString());
                return new RouteCalculation(origin, destination, null, null);
            }

            JsonNode leg = root.path("routes").path(0).path("legs").path(0);
            if (leg.isMissingNode()) {
                return new RouteCalculation(origin, destination, null, null);
            }
            long distanceMeters = leg.path("distance").path("value").asLong(-1);
            long durationSeconds = leg.path("duration").path("value").asLong(-1);
            if (distanceMeters < 0 || durationSeconds < 0) {
                return new RouteCalculation(origin, destination, null, null);
            }

            BigDecimal distanceKm = BigDecimal.valueOf(distanceMeters)
                    .divide(BigDecimal.valueOf(1000), 2, RoundingMode.HALF_UP);
            long estimatedMinutes = Math.max(1, (durationSeconds + 59) / 60);
            return new RouteCalculation(origin, destination, distanceKm, estimatedMinutes);
        } catch (Exception exception) {
            log.warn("Google Directions API failed; route will be saved without distance/time: {}",
                    exception.getMessage());
            return new RouteCalculation(origin, destination, null, null);
        }
    }

    private Coordinates geocodeSafely(String address) {
        try {
            URI uri = UriComponentsBuilder.fromUriString(geocodingUrl)
                    .queryParam("address", address)
                    .queryParam("key", apiKey)
                    .build()
                    .encode()
                    .toUri();
            JsonNode root = getJson(uri);
            if (!"OK".equals(root.path("status").asString())) {
                log.warn("Google Geocoding API returned status {} for an address", root.path("status").asString());
                return null;
            }
            JsonNode location = root.path("results").path(0).path("geometry").path("location");
            if (!location.has("lat") || !location.has("lng")) {
                return null;
            }
            return new Coordinates(location.path("lat").decimalValue(), location.path("lng").decimalValue());
        } catch (Exception exception) {
            log.warn("Google Geocoding API failed; route will be saved without complete metrics: {}",
                    exception.getMessage());
            return null;
        }
    }

    private JsonNode getJson(URI uri) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(Duration.ofSeconds(5))
                .GET()
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalStateException("Google Maps HTTP status " + response.statusCode());
        }
        return objectMapper.readTree(response.body());
    }
}
