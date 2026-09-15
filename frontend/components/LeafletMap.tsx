"use client";

import {
  MapContainer,
  TileLayer,
  Marker,
  Popup,
  Polyline,
} from "react-leaflet";
import L from "leaflet";
import "leaflet/dist/leaflet.css";

type MapProps = {
  origin: [number, number];
  destination: [number, number];
  current?: [number, number];
};

const originIcon = L.divIcon({
  className: "",
  html: `<div style="
    width:18px;
    height:18px;
    background:#22c55e;
    border:3px solid white;
    border-radius:50%;
    box-shadow:0 0 8px rgba(0,0,0,.5);
  "></div>`,
  iconSize: [18, 18],
  iconAnchor: [9, 9],
});

const destinationIcon = L.divIcon({
  className: "",
  html: `<div style="
    width:18px;
    height:18px;
    background:#ef4444;
    border:3px solid white;
    border-radius:50%;
    box-shadow:0 0 8px rgba(0,0,0,.5);
  "></div>`,
  iconSize: [18, 18],
  iconAnchor: [9, 9],
});

const currentIcon = L.divIcon({
  className: "",
  html: `<div style="
    width:20px;
    height:20px;
    background:#06b6d4;
    border:3px solid white;
    border-radius:50%;
    box-shadow:0 0 10px rgba(6,182,212,.8);
  "></div>`,
  iconSize: [20, 20],
  iconAnchor: [10, 10],
});

export default function LeafletMap({
  origin,
  destination,
  current,
}: MapProps) {
  const center = current || origin;

  const routePoints: [number, number][] = current
    ? [origin, current, destination]
    : [origin, destination];

  return (
    <MapContainer
      center={center}
      zoom={6}
      scrollWheelZoom={true}
      className="h-full w-full"
    >
      <TileLayer
        attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>'
        url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
      />

      <Polyline
        positions={routePoints}
        pathOptions={{
          color: "#06b6d4",
          weight: 5,
          opacity: 0.8,
        }}
      />

      <Marker position={origin} icon={originIcon}>
        <Popup>Shipment Origin</Popup>
      </Marker>

      {current && (
        <Marker position={current} icon={currentIcon}>
          <Popup>Current Shipment Location</Popup>
        </Marker>
      )}

      <Marker position={destination} icon={destinationIcon}>
        <Popup>Shipment Destination</Popup>
      </Marker>
    </MapContainer>
  );
}