export function Alert({ message, success = false }: { message: string; success?: boolean }) {
  if (!message) return null;
  return (
    <div className={`alert ${success ? "success" : ""}`} role="alert" aria-live="polite">
      {message}
    </div>
  );
}
