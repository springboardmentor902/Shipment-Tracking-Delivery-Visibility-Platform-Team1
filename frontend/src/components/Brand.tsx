import Image from "next/image";
import Link from "next/link";

export function Brand({ light = false, href = "/" }: { light?: boolean; href?: string }) {
  return (
    <Link className={`brand ${light ? "brand-light" : "brand-dark"}`} href={href}>
      <span className="brand-mark" aria-hidden="true">
        <Image src="/brand/shiptrack-mark.png" alt="" width={42} height={42} priority />
      </span>
      <span className="brand-name">ShipTrack</span>
      <span className="brand-edition">Pro</span>
    </Link>
  );
}
