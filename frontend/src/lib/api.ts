interface ApiErrorBody {
  message?: string;
  detail?: string;
  validationErrors?: Record<string, string>;
}

export class ApiError extends Error {
  constructor(
    message: string,
    public readonly status: number,
  ) {
    super(message);
    this.name = "ApiError";
  }
}

export async function apiRequest<T>(
  path: string,
  options: RequestInit = {},
  token?: string,
): Promise<T> {
  const headers = new Headers(options.headers);
  if (options.body && !headers.has("Content-Type")) {
    headers.set("Content-Type", "application/json");
  }
  if (token) {
    headers.set("Authorization", `Bearer ${token}`);
  }

  const response = await fetch(`/api${path}`, {
    ...options,
    headers,
    cache: "no-store",
  });

  if (response.status === 204) {
    return undefined as T;
  }

  const isJson = response.headers.get("content-type")?.includes("application/json");
  const body = isJson ? ((await response.json()) as ApiErrorBody | T) : await response.text();

  if (!response.ok) {
    const errorBody = typeof body === "object" && body !== null ? (body as ApiErrorBody) : null;
    const validationMessage = errorBody?.validationErrors
      ? Object.values(errorBody.validationErrors).join(" ")
      : null;
    const message =
      validationMessage ||
      errorBody?.message ||
      errorBody?.detail ||
      (typeof body === "string" && body) ||
      "The request could not be completed.";
    throw new ApiError(message, response.status);
  }

  return body as T;
}
