"use client";

import { forwardRef, useEffect, useImperativeHandle, useRef, useState } from "react";

export interface SignaturePadHandle {
  clear: () => void;
  toFile: () => Promise<File>;
}

export const SignaturePad = forwardRef<SignaturePadHandle>(function SignaturePad(_, ref) {
  const canvasRef = useRef<HTMLCanvasElement>(null);
  const drawing = useRef(false);
  const [signed, setSigned] = useState(false);

  function resetCanvas() {
    const canvas = canvasRef.current;
    if (!canvas) return;
    const context = canvas.getContext("2d");
    if (!context) return;
    const ratio = window.devicePixelRatio || 1;
    const width = canvas.clientWidth;
    const height = canvas.clientHeight;
    canvas.width = Math.max(1, Math.floor(width * ratio));
    canvas.height = Math.max(1, Math.floor(height * ratio));
    context.setTransform(ratio, 0, 0, ratio, 0, 0);
    context.fillStyle = "#ffffff";
    context.fillRect(0, 0, width, height);
    context.strokeStyle = "#111827";
    context.lineWidth = 2;
    context.lineCap = "round";
    context.lineJoin = "round";
    setSigned(false);
  }

  useEffect(() => {
    const timer = window.setTimeout(resetCanvas, 0);
    window.addEventListener("resize", resetCanvas);
    return () => {
      window.clearTimeout(timer);
      window.removeEventListener("resize", resetCanvas);
    };
  }, []);

  useImperativeHandle(ref, () => ({
    clear: resetCanvas,
    toFile: () => new Promise<File>((resolve, reject) => {
      const canvas = canvasRef.current;
      if (!canvas || !signed) {
        reject(new Error("Recipient signature is required."));
        return;
      }
      canvas.toBlob((blob) => {
        if (!blob) {
          reject(new Error("Signature could not be prepared."));
          return;
        }
        resolve(new File([blob], "signature.png", { type: "image/png" }));
      }, "image/png");
    }),
  }), [signed]);

  function point(event: React.PointerEvent<HTMLCanvasElement>) {
    const canvas = canvasRef.current;
    if (!canvas) return { x: 0, y: 0 };
    const bounds = canvas.getBoundingClientRect();
    return { x: event.clientX - bounds.left, y: event.clientY - bounds.top };
  }

  function start(event: React.PointerEvent<HTMLCanvasElement>) {
    const canvas = canvasRef.current;
    const context = canvas?.getContext("2d");
    if (!canvas || !context) return;
    canvas.setPointerCapture(event.pointerId);
    const current = point(event);
    context.beginPath();
    context.moveTo(current.x, current.y);
    drawing.current = true;
  }

  function move(event: React.PointerEvent<HTMLCanvasElement>) {
    if (!drawing.current) return;
    const context = canvasRef.current?.getContext("2d");
    if (!context) return;
    const current = point(event);
    context.lineTo(current.x, current.y);
    context.stroke();
    setSigned(true);
  }

  function stop() {
    drawing.current = false;
  }

  return (
    <div className="signature-pad">
      <canvas
        ref={canvasRef}
        aria-label="Recipient signature pad"
        onPointerDown={start}
        onPointerMove={move}
        onPointerUp={stop}
        onPointerCancel={stop}
      />
      <button type="button" onClick={resetCanvas}>Clear signature</button>
    </div>
  );
});
