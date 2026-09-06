package com.shiptrack.shiptrack_pro.integration.maps;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriComponentsBuilder;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Service
@Slf4j
public class GeoapifyMapsRouteCalculator implements MapsRouteCalculator {

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final String geocodingApiKey;
    private final String routingApiKey;
    private final String geocodingUrl;
    private final String autocompleteUrl;
    private final String routingUrl;
    private final String countryCode;

    public GeoapifyMapsRouteCalculator(
            ObjectMapper objectMapper,
            @Value("${geoapify.maps.geocoding-api-key:}") String geocodingApiKey,
            @Value("${geoapify.maps.routing-api-key:}") String routingApiKey,
            @Value("${geoapify.maps.geocoding-url:https://api.geoapify.com/v1/geocode/search}")
            String geocodingUrl,
            @Value("${geoapify.maps.autocomplete-url:https://api.geoapify.com/v1/geocode/autocomplete}")
            String autocompleteUrl,
            @Value("${geoapify.maps.routing-url:https://api.geoapify.com/v1/routing}")
            String routingUrl,
            @Value("${geoapify.maps.country-code:in}") String countryCode
    ) {
        this.objectMapper = objectMapper;
        this.geocodingApiKey = geocodingApiKey;
        this.routingApiKey = routingApiKey;
        this.geocodingUrl = geocodingUrl;
        this.autocompleteUrl = autocompleteUrl;
        this.routingUrl = routingUrl;
        this.countryCode = countryCode;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(3))
                .build();
    }

    @Override
    public RouteCalculation calculate(String originAddress, String destinationAddress) {
        if (!StringUtils.hasText(geocodingApiKey) || !StringUtils.hasText(routingApiKey)) {
            return RouteCalculation.empty();
        }

        Coordinates origin = geocodeSafely(originAddress);
        Coordinates destination = geocodeSafely(destinationAddress);
        if (origin == null || destination == null) {
            return new RouteCalculation(origin, destination, null, null);
        }

        try {
            URI uri = UriComponentsBuilder.fromUriString(routingUrl)
                    .queryParam("waypoints", origin.latitude() + "," + origin.longitude()
                            + "|" + destination.latitude() + "," + destination.longitude())
                    .queryParam("mode", "drive")
                    .queryParam("units", "metric")
                    .queryParam("format", "json")
                    .queryParam("apiKey", routingApiKey)
                    .build()
                    .encode()
                    .toUri();
            JsonNode route = getJson(uri).path("results").path(0);
            if (!route.has("distance") || !route.has("time")) {
                log.warn("Geoapify Routing API returned no route metrics");
                return new RouteCalculation(origin, destination, null, null);
            }

            BigDecimal distanceKm = route.path("distance").decimalValue()
                    .divide(BigDecimal.valueOf(1000), 2, RoundingMode.HALF_UP);
            long durationSeconds = route.path("time").asLong(-1);
            if (durationSeconds < 0) {
                return new RouteCalculation(origin, destination, distanceKm, null);
            }
            long estimatedMinutes = Math.max(1, (durationSeconds + 59) / 60);
            return new RouteCalculation(origin, destination, distanceKm, estimatedMinutes);
        } catch (Exception exception) {
            log.warn("Geoapify Routing API failed; route will be saved without distance/time: {}",
                    exception.getMessage());
            return new RouteCalculation(origin, destination, null, null);
        }
    }

    private Coordinates geocodeSafely(String address) {
        try {
            Coordinates exactMatch = geocode(geocodingUrl, address);
            if (exactMatch != null) {
                return exactMatch;
            }
            Coordinates suggestedMatch = geocode(autocompleteUrl, address);
            if (suggestedMatch == null) {
                log.warn("Geoapify returned no coordinates for address: {}", address);
            }
            return suggestedMatch;
        } catch (Exception exception) {
            log.warn("Geoapify Geocoding API failed; route will be saved without complete metrics: {}",
                    exception.getMessage());
            return null;
        }
    }

    private Coordinates geocode(String endpoint, String address) throws Exception {
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(endpoint)
                .queryParam("text", address)
                .queryParam("format", "json")
                .queryParam("limit", 1)
                .queryParam("apiKey", geocodingApiKey);
        if (StringUtils.hasText(countryCode)) {
            builder.queryParam("filter", "countrycode:" + countryCode.trim().toLowerCase());
        }

        JsonNode location = getJson(builder.build().encode().toUri()).path("results").path(0);
        if (!location.has("lat") || !location.has("lon")) {
            return null;
        }
        return new Coordinates(
                location.path("lat").decimalValue(),
                location.path("lon").decimalValue()
        );
    }

    private JsonNode getJson(URI uri) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(Duration.ofSeconds(7))
                .header("Accept", "application/json")
                .GET()
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalStateException("Geoapify HTTP status " + response.statusCode());
        }
        return objectMapper.readTree(response.body());
    }
}
