const BASE = '/api';
const TOKEN_KEY = 'babytracker_token';

export interface User { id: number; nickname: string }
export interface AuthPayload { token: string; user: User }
export interface BabyView {
  id: number; name: string; birthday: string; bloodType?: string;
  initialHeight?: number; initialWeight?: number; createdBy: number; role: string;
}
export interface Member {
  id: number; babyId: number; userId: number; role: string; nickname?: string;
}
export interface Invite {
  id: number; babyId: number; code: string; role: string; status: string; expiresAt: string;
}

let token = localStorage.getItem(TOKEN_KEY) ?? '';

export function setToken(value: string) {
  token = value;
  localStorage.setItem(TOKEN_KEY, value);
}

export function clearToken() {
  token = '';
  localStorage.removeItem(TOKEN_KEY);
}

async function request<T>(path: string, options: RequestInit = {}): Promise<T> {
  const headers: Record<string, string> = { 'Content-Type': 'application/json' };
  if (token) headers.Authorization = `Bearer ${token}`;
  const res = await fetch(BASE + path, { ...options, headers });
  const data = (await res.json().catch(() => ({}))) as Record<string, unknown>;
  if (!res.ok) throw new Error((data.message as string) || `请求失败（${res.status}）`);
  return data as unknown as T;
}

const post = <T>(path: string, body: unknown) =>
  request<T>(path, { method: 'POST', body: JSON.stringify(body) });

export const api = {
  register: (nickname: string) => post<AuthPayload>('/users/register', { nickname }),
  login: (nickname: string) => post<AuthPayload>('/users/login', { nickname }),
  myBabies: () => request<BabyView[]>('/babies'),
  createBaby: (baby: { name: string; birthday: string }) => post<BabyView>('/babies', baby),
  members: (babyId: number) => request<Member[]>(`/babies/${babyId}/members`),
  changeRole: (babyId: number, userId: number, role: string) =>
    request<Member>(`/babies/${babyId}/members/${userId}`, { method: 'PUT', body: JSON.stringify({ role }) }),
  removeMember: (babyId: number, userId: number) =>
    request<{ success: boolean }>(`/babies/${babyId}/members/${userId}`, { method: 'DELETE' }),
  leave: (babyId: number) => post<{ success: boolean }>(`/babies/${babyId}/leave`, {}),
  invites: (babyId: number) => request<Invite[]>(`/babies/${babyId}/invites`),
  createInvite: (babyId: number, role: string) => post<Invite>(`/babies/${babyId}/invites`, { role }),
  revokeInvite: (babyId: number, inviteId: number) =>
    post<{ success: boolean }>(`/babies/${babyId}/invites/${inviteId}/revoke`, {}),
  claimInvite: (code: string) => post<Member>('/invites/claim', { code }),
};
