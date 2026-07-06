import {
  ApiError,
  ApiErrorBody,
  AuthResult,
  Booking,
  ChefCreateRequest,
  ChefResponse,
  ChefUpdateRequest,
  CurrentUser,
  EquipmentChoice,
  ParticipantBooking,
  Program,
  ProgramInput,
  Review,
  SlotCreateRequest,
  SlotDetails,
  SlotSummary,
  StudioSettings,
} from '../types';

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080/api';

type Json = Record<string, unknown>;

async function request<T>(path: string, options: RequestInit = {}, token?: string | null): Promise<T> {
  const headers = new Headers(options.headers);
  if (!headers.has('Content-Type') && options.body) {
    headers.set('Content-Type', 'application/json');
  }
  if (token) {
    headers.set('Authorization', `Bearer ${token}`);
  }

  const response = await fetch(`${API_BASE_URL}${path}`, {
    ...options,
    headers,
  });

  if (response.status === 204) {
    return undefined as T;
  }

  const text = await response.text();
  const data = text ? JSON.parse(text) : undefined;

  if (!response.ok) {
    throw new ApiError(response.status, data as ApiErrorBody);
  }

  return data as T;
}

export const api = {
  requestCode(email: string) {
    return request<void>('/auth/code/request', {
      method: 'POST',
      body: JSON.stringify({ email }),
    });
  },

  verifyCode(email: string, code: string) {
    return request<AuthResult>('/auth/code/verify', {
      method: 'POST',
      body: JSON.stringify({ email, code }),
    });
  },

  logout(token: string | null) {
    return request<void>('/auth/logout', { method: 'POST' }, token);
  },

  me(token: string) {
    return request<CurrentUser>('/me', {}, token);
  },

  updateMe(token: string, payload: Json) {
    return request<CurrentUser>('/me', {
      method: 'PATCH',
      body: JSON.stringify(payload),
    }, token);
  },

  slots(params: URLSearchParams, token?: string | null) {
    const query = params.toString();
    return request<SlotSummary[]>(`/slots${query ? `?${query}` : ''}`, {}, token);
  },

  slot(slotId: string) {
    return request<SlotDetails>(`/slots/${slotId}`);
  },

  createBooking(token: string, slotId: string, equipmentChoice: EquipmentChoice) {
    return request<Booking>('/bookings', {
      method: 'POST',
      body: JSON.stringify({ slotId, equipmentChoice }),
    }, token);
  },

  myBookings(token: string) {
    return request<Booking[]>('/me/bookings', {}, token);
  },

  cancelBooking(token: string, bookingId: string) {
    return request<Booking>(`/bookings/${bookingId}/cancel`, { method: 'POST' }, token);
  },

  programs() {
    return request<Program[]>('/programs');
  },

  program(programId: string) {
    return request<Program>(`/programs/${programId}`);
  },

  createProgram(token: string, payload: ProgramInput) {
    return request<Program>('/programs', {
      method: 'POST',
      body: JSON.stringify(payload),
    }, token);
  },

  updateProgram(token: string, programId: string, payload: ProgramInput) {
    return request<Program>(`/programs/${programId}`, {
      method: 'PUT',
      body: JSON.stringify(payload),
    }, token);
  },

  chefs(token: string) {
    return request<ChefResponse[]>('/chefs', {}, token);
  },

  createChef(token: string, payload: ChefCreateRequest) {
    return request<ChefResponse>('/chefs', {
      method: 'POST',
      body: JSON.stringify(payload),
    }, token);
  },

  updateChef(token: string, chefId: string, payload: ChefUpdateRequest) {
    return request<ChefResponse>(`/chefs/${chefId}`, {
      method: 'PATCH',
      body: JSON.stringify(payload),
    }, token);
  },

  createSlot(token: string, payload: SlotCreateRequest) {
    return request<SlotDetails>('/slots', {
      method: 'POST',
      body: JSON.stringify(payload),
    }, token);
  },

  cancelSlot(token: string, slotId: string, reason: string) {
    return request<SlotDetails>(`/slots/${slotId}/cancel`, {
      method: 'POST',
      body: JSON.stringify({ reason }),
    }, token);
  },

  slotBookings(token: string, slotId: string) {
    return request<ParticipantBooking[]>(`/slots/${slotId}/bookings`, {}, token);
  },

  markNoShow(token: string, bookingId: string) {
    return request<ParticipantBooking>(`/bookings/${bookingId}/no-show`, { method: 'POST' }, token);
  },

  revokeNoShow(token: string, bookingId: string) {
    return request<ParticipantBooking>(`/bookings/${bookingId}/no-show`, { method: 'DELETE' }, token);
  },

  chefSlots(token: string, period?: 'UPCOMING' | 'PAST') {
    const query = period ? `?period=${period}` : '';
    return request<SlotSummary[]>(`/chef/slots${query}`, {}, token);
  },

  reviews(token: string) {
    return request<Review[]>('/reviews', {}, token);
  },

  studioSettings() {
    return request<StudioSettings>('/studio-settings');
  },

  updateStudioSettings(token: string, payload: StudioSettings) {
    return request<StudioSettings>('/studio-settings', {
      method: 'PUT',
      body: JSON.stringify(payload),
    }, token);
  },
};
