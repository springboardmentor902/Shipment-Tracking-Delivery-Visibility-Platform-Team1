"use client";

import { MapPinned, Radio, Route as RouteIcon } from "lucide-react";
import {
  AttributionControl,
  type GeoJSONSource,
  LngLatBounds,
  Map as MapLibreMap,
  Marker as MapLibreMarker,
  NavigationControl,
} from "maplibre-gl";
import { useEffect, useRef, useState } from "react";

interface Coordinates {
  latitude: number;
  longitude: number;
}

interface LiveTrackingMapProps {
  currentLocation: Coordinates | null;
  origin: Coordinates | null;
  destination: Coordinates | null;
}

declare global {
  interface Window {
    __shipTrackGoogleMapsReady?: () => void;
  }
}

let googleMapsPromise: Promise<void> | null = null;

function loadGoogleMaps(apiKey: string) {
  if (typeof google !== "undefined") return Promise.resolve();
  if (googleMapsPromise) return googleMapsPromise;

  googleMapsPromise = new Promise<void>((resolve, reject) => {
    window.__shipTrackGoogleMapsReady = () => {
      delete window.__shipTrackGoogleMapsReady;
      resolve();
    };
    const script = document.createElement("script");
    script.dataset.shiptrackGoogleMaps = "true";
    script.src = `https://maps.googleapis.com/maps/api/js?key=${encodeURIComponent(apiKey)}&v=weekly&libraries=marker&callback=__shipTrackGoogleMapsReady`;
    script.async = true;
    script.onerror = () => {
      googleMapsPromise = null;
      reject(new Error("Google Maps could not be loaded."));
    };
    document.head.appendChild(script);
  });
  return googleMapsPromise;
}

function googlePoint(coordinates: Coordinates): google.maps.LatLngLiteral {
  return { lat: coordinates.latitude, lng: coordinates.longitude };
}

function geoPoint(coordinates: Coordinates): [number, number] {
  return [coordinates.longitude, coordinates.latitude];
}

function markerElement(kind: "driver" | "destination") {
  const host = document.createElement("div");
  host.className = "tracking-map-marker-host";
  host.innerHTML = kind === "driver"
    ? '<div class="tracking-map-marker driver"><span></span><strong>Driver</strong></div>'
    : '<div class="tracking-map-marker destination"><span></span><strong>Drop</strong></div>';
  return host;
}

function GoogleTrackingMap({
  apiKey,
  currentLocation,
  origin,
  destination,
  onError,
}: LiveTrackingMapProps & { apiKey: string; onError: (message: string) => void }) {
  const containerRef = useRef<HTMLDivElement>(null);
  const mapRef = useRef<google.maps.Map | null>(null);
  const driverMarkerRef = useRef<google.maps.marker.AdvancedMarkerElement | null>(null);
  const destinationMarkerRef = useRef<google.maps.marker.AdvancedMarkerElement | null>(null);
  const routeLineRef = useRef<google.maps.Polyline | null>(null);

  useEffect(() => {
    let cancelled = false;

    async function renderMap() {
      const center = currentLocation ?? origin ?? destination;
      if (!center || !containerRef.current) return;
      try {
        await loadGoogleMaps(apiKey);
        if (cancelled || !containerRef.current) return;
        if (!mapRef.current) {
          const { Map } = await google.maps.importLibrary("maps") as google.maps.MapsLibrary;
          await google.maps.importLibrary("marker");
          if (cancelled || !containerRef.current) return;
          mapRef.current = new Map(containerRef.current, {
            center: googlePoint(center),
            zoom: 13,
            mapId: process.env.NEXT_PUBLIC_GOOGLE_MAP_ID?.trim() || "DEMO_MAP_ID",
            disableDefaultUI: true,
            zoomControl: true,
            fullscreenControl: true,
          });
          routeLineRef.current = new google.maps.Polyline({
            map: mapRef.current,
            strokeColor: "#665CFF",
            strokeOpacity: 0.92,
            strokeWeight: 5,
            geodesic: true,
          });
        }

        if (currentLocation) {
          if (!driverMarkerRef.current) {
            driverMarkerRef.current = new google.maps.marker.AdvancedMarkerElement({
              map: mapRef.current,
              position: googlePoint(currentLocation),
              content: markerElement("driver"),
              title: "Driver's current location",
            });
          } else {
            driverMarkerRef.current.position = googlePoint(currentLocation);
          }
        }
        if (destination) {
          if (!destinationMarkerRef.current) {
            destinationMarkerRef.current = new google.maps.marker.AdvancedMarkerElement({
              map: mapRef.current,
              position: googlePoint(destination),
              content: markerElement("destination"),
              title: "Shipment destination",
            });
          } else {
            destinationMarkerRef.current.position = googlePoint(destination);
          }
        }

        const routeStart = currentLocation ?? origin;
        if (routeStart && destination) {
          routeLineRef.current?.setPath([googlePoint(routeStart), googlePoint(destination)]);
          const bounds = new google.maps.LatLngBounds();
          bounds.extend(googlePoint(routeStart));
          bounds.extend(googlePoint(destination));
          mapRef.current.fitBounds(bounds, 72);
        } else if (currentLocation) {
          mapRef.current.panTo(googlePoint(currentLocation));
        }
      } catch (error) {
        if (!cancelled) onError(error instanceof Error ? error.message : "Google Maps could not be loaded.");
      }
    }

    void renderMap();
    return () => { cancelled = true; };
  }, [apiKey, currentLocation, destination, onError, origin]);

  useEffect(() => () => {
    if (driverMarkerRef.current) driverMarkerRef.current.map = null;
    if (destinationMarkerRef.current) destinationMarkerRef.current.map = null;
    routeLineRef.current?.setMap(null);
  }, []);

  return <div className="tracking-map-canvas" ref={containerRef} aria-label="Live shipment map powered by Google" />;
}

interface GeoMapState {
  currentLocation: Coordinates | null;
  origin: Coordinates | null;
  destination: Coordinates | null;
}

function updateGeoapifyMap(
  map: MapLibreMap,
  state: GeoMapState,
  driverMarkerRef: React.MutableRefObject<MapLibreMarker | null>,
  destinationMarkerRef: React.MutableRefObject<MapLibreMarker | null>,
) {
  const { currentLocation, origin, destination } = state;
  if (currentLocation) {
    if (!driverMarkerRef.current) {
      driverMarkerRef.current = new MapLibreMarker({ element: markerElement("driver"), anchor: "bottom" })
        .setLngLat(geoPoint(currentLocation))
        .addTo(map);
    } else {
      driverMarkerRef.current.setLngLat(geoPoint(currentLocation));
    }
  }
  if (destination) {
    if (!destinationMarkerRef.current) {
      destinationMarkerRef.current = new MapLibreMarker({ element: markerElement("destination"), anchor: "bottom" })
        .setLngLat(geoPoint(destination))
        .addTo(map);
    } else {
      destinationMarkerRef.current.setLngLat(geoPoint(destination));
    }
  }

  const routeStart = currentLocation ?? origin;
  if (routeStart && destination && map.isStyleLoaded()) {
    const line = {
      type: "Feature" as const,
      properties: {},
      geometry: {
        type: "LineString" as const,
        coordinates: [geoPoint(routeStart), geoPoint(destination)],
      },
    };
    const source = map.getSource("shipment-route") as GeoJSONSource | undefined;
    if (source) {
      source.setData(line);
    } else {
      map.addSource("shipment-route", { type: "geojson", data: line });
      map.addLayer({
        id: "shipment-route-line",
        type: "line",
        source: "shipment-route",
        layout: { "line-cap": "round", "line-join": "round" },
        paint: { "line-color": "#706cff", "line-opacity": 0.92, "line-width": 5 },
      });
    }
    map.fitBounds(new LngLatBounds(geoPoint(routeStart), geoPoint(destination)), {
      padding: 72,
      maxZoom: 14,
      duration: 650,
    });
  } else if (currentLocation) {
    map.easeTo({ center: geoPoint(currentLocation), duration: 450 });
  }
}

function removeGeoapifyMap(
  mapRef: React.MutableRefObject<MapLibreMap | null>,
  driverMarkerRef: React.MutableRefObject<MapLibreMarker | null>,
  destinationMarkerRef: React.MutableRefObject<MapLibreMarker | null>,
) {
  driverMarkerRef.current?.remove();
  destinationMarkerRef.current?.remove();
  mapRef.current?.remove();
  driverMarkerRef.current = null;
  destinationMarkerRef.current = null;
  mapRef.current = null;
}

function GeoapifyTrackingMap({
  apiKey,
  currentLocation,
  origin,
  destination,
  onError,
}: LiveTrackingMapProps & { apiKey: string; onError: (message: string) => void }) {
  const containerRef = useRef<HTMLDivElement>(null);
  const mapRef = useRef<MapLibreMap | null>(null);
  const driverMarkerRef = useRef<MapLibreMarker | null>(null);
  const destinationMarkerRef = useRef<MapLibreMarker | null>(null);
  const latestStateRef = useRef<GeoMapState>({ currentLocation, origin, destination });
  const disposedRef = useRef(false);
  const center = currentLocation ?? origin ?? destination;

  useEffect(() => {
    latestStateRef.current = { currentLocation, origin, destination };
    if (mapRef.current?.isStyleLoaded()) {
      updateGeoapifyMap(mapRef.current, latestStateRef.current, driverMarkerRef, destinationMarkerRef);
    }
  }, [currentLocation, destination, origin]);

  useEffect(() => {
    if (!center || !containerRef.current || mapRef.current) return;
    try {
      const map = new MapLibreMap({
        container: containerRef.current,
        style: {
          version: 8,
          sources: {
            "geoapify-basemap": {
              type: "raster",
              tiles: [`https://maps.geoapify.com/v1/tile/dark-matter/{z}/{x}/{y}.png?apiKey=${encodeURIComponent(apiKey)}`],
              tileSize: 256,
              attribution: "Powered by Geoapify | © OpenMapTiles © OpenStreetMap contributors",
            },
          },
          layers: [{
            id: "geoapify-basemap",
            type: "raster",
            source: "geoapify-basemap",
          }],
        },
        center: geoPoint(center),
        zoom: 12,
        attributionControl: false,
      });
      mapRef.current = map;
      map.addControl(new NavigationControl({ showCompass: false }), "top-right");
      map.addControl(new AttributionControl({ compact: true }));
      map.on("load", () => {
        if (!disposedRef.current) updateGeoapifyMap(map, latestStateRef.current, driverMarkerRef, destinationMarkerRef);
      });
      map.on("error", (event) => {
        if (!disposedRef.current && !map.isStyleLoaded()) {
          onError(event.error?.message || "Geoapify map could not be loaded.");
        }
      });
    } catch (error) {
      if (!disposedRef.current) onError(error instanceof Error ? error.message : "Geoapify map could not be loaded.");
    }
  }, [apiKey, center, onError]);

  useEffect(() => {
    disposedRef.current = false;
    return () => {
      disposedRef.current = true;
      removeGeoapifyMap(mapRef, driverMarkerRef, destinationMarkerRef);
    };
  }, []);

  return <div className="tracking-map-canvas" ref={containerRef} aria-label="Live shipment map powered by Geoapify" />;
}

function CoordinateFallback({
  currentLocation,
  destination,
  message,
}: Pick<LiveTrackingMapProps, "currentLocation" | "destination"> & { message: string }) {
  return (
    <div className="tracking-map-fallback">
      <span className="tracking-map-fallback-icon"><MapPinned size={24} /></span>
      <div>
        <span className="eyebrow dark">Live coordinates</span>
        <h3>{message}</h3>
        <p>The WebSocket remains active and incoming driver coordinates will continue updating below.</p>
      </div>
      <div className="coordinate-fallback-grid">
        <div><Radio size={15} /><span>Driver</span><strong>{currentLocation ? `${currentLocation.latitude.toFixed(6)}, ${currentLocation.longitude.toFixed(6)}` : "Awaiting first update"}</strong></div>
        <div><RouteIcon size={15} /><span>Destination</span><strong>{destination ? `${destination.latitude.toFixed(6)}, ${destination.longitude.toFixed(6)}` : "Route coordinates unavailable"}</strong></div>
      </div>
    </div>
  );
}

export function LiveTrackingMap(props: LiveTrackingMapProps) {
  const googleKey = process.env.NEXT_PUBLIC_GOOGLE_MAPS_API_KEY?.trim() ?? "";
  const geoapifyKey = process.env.NEXT_PUBLIC_GEOAPIFY_API_KEY?.trim() ?? "";
  const [googleError, setGoogleError] = useState("");
  const [geoapifyError, setGeoapifyError] = useState("");

  if (googleKey && !googleError) {
    return <GoogleTrackingMap {...props} apiKey={googleKey} onError={setGoogleError} />;
  }
  if (geoapifyKey && !geoapifyError) {
    return <GeoapifyTrackingMap {...props} apiKey={geoapifyKey} onError={setGeoapifyError} />;
  }
  return (
    <CoordinateFallback
      currentLocation={props.currentLocation}
      destination={props.destination}
      message={geoapifyError || googleError || "A browser map key is not configured"}
    />
  );
}
