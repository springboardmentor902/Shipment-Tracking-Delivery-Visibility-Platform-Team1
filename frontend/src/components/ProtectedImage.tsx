"use client";

import { useEffect, useState } from "react";
import Image from "next/image";

export function ProtectedImage({ src, alt, token }: { src: string; alt: string; token: string }) {
  const [objectUrl, setObjectUrl] = useState<string | null>(null);
  const [failed, setFailed] = useState(false);

  useEffect(() => {
    let active = true;
    let loadedUrl: string | null = null;
    const timer = window.setTimeout(() => {
      fetch(src, {
        headers: { Authorization: `Bearer ${token}` },
        cache: "no-store",
      })
        .then((response) => {
          if (!response.ok) throw new Error("Image could not be loaded");
          return response.blob();
        })
        .then((blob) => {
          if (!active) return;
          loadedUrl = URL.createObjectURL(blob);
          setObjectUrl(loadedUrl);
        })
        .catch(() => {
          if (active) setFailed(true);
        });
    }, 0);

    return () => {
      active = false;
      window.clearTimeout(timer);
      if (loadedUrl) URL.revokeObjectURL(loadedUrl);
    };
  }, [src, token]);

  if (failed) return <span className="proof-image-placeholder">Image unavailable</span>;
  if (!objectUrl) return <span className="proof-image-placeholder">Loading image...</span>;
  return <Image src={objectUrl} alt={alt} width={640} height={360} unoptimized />;
}
