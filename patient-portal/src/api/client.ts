import type { ApiResponse } from './types';

// Both are set once at login (see auth.ts) — every request needs X-Tenant-Id per the backend's
// tenant-isolation model (TenantResolutionFilter), and most need the Bearer token.
const TENANT_ID_KEY = 'clinic.tenantId';
const TOKEN_KEY = 'clinic.accessToken';

const BASE_URL = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080';

// Pre-login public pages (doctor directory browsing) have no session to carry a tenant, so fall
// back to a build-configured default tenant — this is a single-tenant demo deployment; a
// multi-tenant deployment would resolve this from the clinic's subdomain/slug instead.
const DEFAULT_TENANT_ID = import.meta.env.VITE_DEFAULT_TENANT_ID as string | undefined;

export function getTenantId(): string | null {
  return localStorage.getItem(TENANT_ID_KEY) ?? DEFAULT_TENANT_ID ?? null;
}

export function setTenantId(tenantId: string) {
  localStorage.setItem(TENANT_ID_KEY, tenantId);
}

export function getToken(): string | null {
  return localStorage.getItem(TOKEN_KEY);
}

export function setToken(token: string) {
  localStorage.setItem(TOKEN_KEY, token);
}

export function clearSession() {
  localStorage.removeItem(TOKEN_KEY);
}

export class ApiError extends Error {
  code: string;
  constructor(code: string, message: string) {
    super(message);
    this.code = code;
  }
}

async function request<T>(path: string, options: RequestInit = {}): Promise<T> {
  const headers: Record<string, string> = {
    'Content-Type': 'application/json',
    ...(options.headers as Record<string, string> | undefined),
  };

  const tenantId = getTenantId();
  if (tenantId) headers['X-Tenant-Id'] = tenantId;

  const token = getToken();
  if (token) headers['Authorization'] = `Bearer ${token}`;

  const res = await fetch(`${BASE_URL}${path}`, { ...options, headers });

  // 204 / empty-body responses (e.g. some POST actions) never carry an envelope.
  if (res.status === 204) return undefined as T;

  const body: ApiResponse<T> = await res.json();

  if (!res.ok || !body.success) {
    throw new ApiError(body.error?.code ?? 'UNKNOWN_ERROR', body.error?.message ?? `Request failed (${res.status})`);
  }

  return body.data as T;
}

export const api = {
  get: <T,>(path: string) => request<T>(path, { method: 'GET' }),
  post: <T,>(path: string, body?: unknown) =>
    request<T>(path, { method: 'POST', body: body !== undefined ? JSON.stringify(body) : undefined }),
  put: <T,>(path: string, body?: unknown) =>
    request<T>(path, { method: 'PUT', body: body !== undefined ? JSON.stringify(body) : undefined }),
};
