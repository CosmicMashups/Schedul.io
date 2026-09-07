import { api, setTenantId, setToken } from './client';
import type { LoginResponse } from './types';

export async function login(tenantId: string, email: string, password: string): Promise<LoginResponse> {
  setTenantId(tenantId);
  const res = await api.post<LoginResponse>('/api/v1/auth/login', { email, password });
  setToken(res.accessToken);
  return res;
}
