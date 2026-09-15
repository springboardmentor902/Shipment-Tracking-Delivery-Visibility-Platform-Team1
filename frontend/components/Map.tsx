"use client";

import dynamic from "next/dynamic";

const LeafletMap = dynamic(
  () => import("./LeafletMap"),
  {
    ssr: false,
    loading: () => (
      <div className="flex h-full w-full items-center justify-center bg-slate-900 text-sm text-slate-400">
        Loading map...
      </div>
    ),
  }
);

type MapProps = {
  origin: [number, number];
  destination: [number, number];
  current?: [number, number];
};

export default function Map({
  origin,
  destination,
  current,
}: MapProps) {
  return (
    <LeafletMap
      origin={origin}
      destination={destination}
      current={current}
    />
  );
}