"use client";

import { ArrowLeft, Camera, CheckCircle2, PenLine, Truck } from "lucide-react";
import Image from "next/image";
import Link from "next/link";
import { useParams, useRouter } from "next/navigation";
import { FormEvent, useEffect, useRef, useState } from "react";
import { Alert } from "@/components/Alert";
import { AppHeader } from "@/components/AppHeader";
import { SignaturePad, type SignaturePadHandle } from "@/components/SignaturePad";
import { ApiError, apiRequest } from "@/lib/api";
import { clearAuth, getAuth } from "@/lib/auth";
import type { AuthSession, ProofOfDelivery, Shipment } from "@/lib/types";

export default function CompleteDeliveryPage() {
  const params = useParams<{ shipmentId: string }>();
  const router = useRouter();
  const shipmentId = Number(params.shipmentId);
  const signatureRef = useRef<SignaturePadHandle>(null);
  const previewUrlRef = useRef<string | null>(null);
  const [session, setSession] = useState<AuthSession | null>(null);
  const [shipment, setShipment] = useState<Shipment | null>(null);
  const [photo, setPhoto] = useState<File | null>(null);
  const [photoPreview, setPhotoPreview] = useState<string | null>(null);
  const [submittedProof, setSubmittedProof] = useState<ProofOfDelivery | null>(null);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [message, setMessage] = useState("");

  useEffect(() => {
    let active = true;
    const timer = window.setTimeout(() => {
      const storedSession = getAuth();
      if (!storedSession?.token || storedSession.user.role !== "LOGISTICS_OPERATOR") {
        router.replace("/dashboard");
        return;
      }
      setSession(storedSession);
      apiRequest<Shipment>(`/shipments/${shipmentId}`, {}, storedSession.token)
        .then((record) => {
          if (!active) return;
          setShipment(record);
        })
        .catch((error: unknown) => {
          if (!active) return;
          if (error instanceof ApiError && error.status === 401) {
            clearAuth();
            router.replace("/login");
            return;
          }
          setMessage(error instanceof Error ? error.message : "Shipment could not be loaded.");
        })
        .finally(() => {
          if (active) setLoading(false);
        });
    }, 0);
    return () => {
      active = false;
      window.clearTimeout(timer);
    };
  }, [router, shipmentId]);

  useEffect(() => () => {
    if (previewUrlRef.current) URL.revokeObjectURL(previewUrlRef.current);
  }, []);

  function choosePhoto(file: File | null) {
    if (file && file.size > 5 * 1024 * 1024) {
      setMessage("Delivery photo must be 5 MB or smaller.");
      return;
    }
    if (previewUrlRef.current) URL.revokeObjectURL(previewUrlRef.current);
    const preview = file ? URL.createObjectURL(file) : null;
    previewUrlRef.current = preview;
    setPhoto(file);
    setPhotoPreview(preview);
  }

  async function submitProof(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!session || !shipment || !photo) {
      setMessage("Delivery photo is required.");
      return;
    }
    const fields = new FormData(event.currentTarget);
    setMessage("");
    setSaving(true);
    try {
      const signature = await signatureRef.current?.toFile();
      if (!signature) throw new Error("Recipient signature is required.");
      fields.set("signature", signature);
      fields.set("photo", photo);
      const proof = await apiRequest<ProofOfDelivery>(`/pod/${shipment.id}`, {
        method: "POST",
        body: fields,
      }, session.token);
      setSubmittedProof(proof);
    } catch (error) {
      setMessage(error instanceof Error ? error.message : "Proof of delivery could not be submitted.");
    } finally {
      setSaving(false);
    }
  }

  function logout() {
    clearAuth();
    router.replace("/login");
  }

  if (!session || loading) {
    return <main className="loading-screen"><span className="spinner dark-spinner" />Opening delivery form...</main>;
  }

  const canSubmit = shipment
    && shipment.status === "OUT_FOR_DELIVERY"
    && shipment.assignedOperatorId === session.user.id;

  return (
    <div className="dashboard-body detail-page-body">
      <AppHeader session={session} section="Complete delivery" onSignOut={logout} />

      <main className="pod-main">
        <Link className="tracking-back-link" href="/dashboard"><ArrowLeft size={14} /> Back to operations</Link>
        <Alert message={message} />

        {submittedProof ? (
          <section className="pod-success">
            <CheckCircle2 size={34} />
            <h1>Delivery completed</h1>
            <p>{submittedProof.trackingNumber} is now marked as delivered. Proof is waiting for verification.</p>
            <Link className="primary-button" href={`/shipments/${submittedProof.shipmentId}`}>View proof details</Link>
          </section>
        ) : !shipment ? (
          <section className="empty-card"><h2>Shipment unavailable</h2><p>This shipment is not assigned to your account.</p></section>
        ) : !canSubmit ? (
          <section className="empty-card"><Truck size={28} /><h2>Delivery cannot be completed</h2><p>The assigned shipment must be Out for Delivery before proof can be submitted.</p></section>
        ) : (
          <>
            <section className="pod-heading">
              <div><span className="eyebrow dark">Proof of delivery</span><h1>Complete {shipment.trackingNumber}</h1><p>Capture the recipient confirmation and delivery photo.</p></div>
              <span className="status-badge status-out_for_delivery">Out for delivery</span>
            </section>

            <form className="pod-form" onSubmit={(event) => void submitProof(event)}>
              <section className="detail-surface">
                <div className="detail-section-title"><PenLine size={16} /><div><strong>Recipient confirmation</strong><small>Name and signature</small></div></div>
                <div className="field">
                  <label htmlFor="recipientName">Recipient name</label>
                  <input id="recipientName" name="recipientName" defaultValue={shipment.receiverName} maxLength={120} required />
                </div>
                <label className="pod-field-label">Recipient signature</label>
                <SignaturePad ref={signatureRef} />
              </section>

              <section className="detail-surface">
                <div className="detail-section-title"><Camera size={16} /><div><strong>Delivery evidence</strong><small>Photo and optional notes</small></div></div>
                <label className="photo-upload">
                  <input
                    name="photo"
                    type="file"
                    accept="image/png,image/jpeg,image/webp"
                    capture="environment"
                    required
                    onChange={(event) => choosePhoto(event.target.files?.[0] ?? null)}
                  />
                  {photoPreview ? <Image src={photoPreview} alt="Selected delivery" width={640} height={360} unoptimized /> : <><Camera size={24} /><strong>Select delivery photo</strong><small>PNG, JPG or WebP · maximum 5 MB</small></>}
                </label>
                <div className="field pod-notes-field">
                  <label htmlFor="deliveryNotes">Delivery notes <span className="optional">optional</span></label>
                  <textarea id="deliveryNotes" name="deliveryNotes" maxLength={1000} placeholder="Package condition or delivery location" />
                </div>
                <button className="primary-button pod-submit" type="submit" disabled={saving}>
                  <CheckCircle2 size={16} />{saving ? "Submitting proof..." : "Confirm delivery"}
                </button>
              </section>
            </form>
          </>
        )}
      </main>
    </div>
  );
}
