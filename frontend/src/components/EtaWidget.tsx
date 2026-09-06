"use client";

import { RefreshCw } from "lucide-react";
import { useEffect, useState } from "react";
import { ApiError, apiRequest } from "@/lib/api";
import type { ETAPrediction } from "@/lib/types";

interface EtaWidgetProps {
  shipmentId: number;
  token: string;
  canRecalculate?: boolean;
  refreshKey?: string | null;
  onPredictionChange?: (prediction: ETAPrediction | null) => void;
}

function formatDateTime(value: string) {
  return new Intl.DateTimeFormat("en-IN", {
    day: "2-digit",
    month: "short",
    year: "numeric",
    hour: "2-digit",
    minute: "2-digit",
  }).format(new Date(value));
}

function riskLevel(score: number) {
  if (score > 6) return { key: "high", label: "High risk" };
  if (score > 3) return { key: "medium", label: "Watch closely" };
  return { key: "low", label: "Low risk" };
}

export function EtaWidget({
  shipmentId,
  token,
  canRecalculate = false,
  refreshKey = null,
  onPredictionChange,
}: EtaWidgetProps) {
  const [prediction, setPrediction] = useState<ETAPrediction | null>(null);
  const [loading, setLoading] = useState(true);
  const [recalculating, setRecalculating] = useState(false);
  const [error, setError] = useState("");

  useEffect(() => {
    let active = true;
    const delay = refreshKey ? 550 : 0;
    const timer = window.setTimeout(() => {
      apiRequest<ETAPrediction>(`/eta/${shipmentId}`, {}, token)
        .then((record) => {
          if (!active) return;
          setPrediction(record);
          onPredictionChange?.(record);
          setError("");
        })
        .catch((requestError: unknown) => {
          if (!active) return;
          if (requestError instanceof ApiError && requestError.status === 404) {
            setPrediction(null);
            onPredictionChange?.(null);
            setError("");
            return;
          }
          setError(requestError instanceof Error ? requestError.message : "ETA could not be loaded.");
        })
        .finally(() => {
          if (active) setLoading(false);
        });
    }, delay);

    return () => {
      active = false;
      window.clearTimeout(timer);
    };
  }, [onPredictionChange, refreshKey, shipmentId, token]);

  async function recalculate() {
    setRecalculating(true);
    setError("");
    try {
      const record = await apiRequest<ETAPrediction>(`/eta/${shipmentId}/predict`, {
        method: "POST",
      }, token);
      setPrediction(record);
      onPredictionChange?.(record);
    } catch (requestError) {
      setError(requestError instanceof Error ? requestError.message : "ETA could not be recalculated.");
    } finally {
      setRecalculating(false);
    }
  }

  const risk = prediction ? riskLevel(Number(prediction.delayRiskScore)) : null;

  return (
    <section className={`eta-widget${risk ? ` eta-risk-${risk.key}` : ""}`}>
      <div className="eta-widget-heading">
        <strong>Estimated delivery</strong>
        {prediction && <span className={`eta-risk-badge risk-${risk?.key}`}>{risk?.label}</span>}
      </div>

      {loading ? (
        <div className="eta-loading"><span className="spinner dark-spinner" />Loading ETA...</div>
      ) : prediction ? (
        <>
          <div className="eta-primary-value"><strong>{formatDateTime(prediction.predictedDeliveryTime)}</strong></div>
          <div className="eta-summary">
            <span>Delay risk <b>{Number(prediction.delayRiskScore).toFixed(1)} / 10</b></span>
            <span>Confidence <b>{Number(prediction.confidenceScore).toFixed(0)}%</b></span>
          </div>
        </>
      ) : (
        <p className="eta-empty">ETA is not available. Create a route first.</p>
      )}

      {error && <p className="eta-error">{error}</p>}
      {canRecalculate && (
        <button className="secondary-button eta-refresh" type="button" onClick={() => void recalculate()} disabled={recalculating}>
          <RefreshCw size={13} className={recalculating ? "spin-icon" : ""} />
          {recalculating ? "Recalculating..." : "Recalculate ETA"}
        </button>
      )}
    </section>
  );
}
