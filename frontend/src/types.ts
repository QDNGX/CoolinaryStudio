export type UserRole = 'CLIENT' | 'CHEF' | 'ADMIN';
export type SlotStatus = 'SCHEDULED' | 'CANCELLED_BY_STUDIO' | 'COMPLETED';
export type BookingStatus =
  | 'CONFIRMED'
  | 'CANCELLED_BY_CLIENT'
  | 'CANCELLED_BY_STUDIO'
  | 'COMPLETED'
  | 'NO_SHOW';
export type EquipmentChoice = 'OWN' | 'RENTAL';

export interface ChefPublic {
  id: string;
  name: string | null;
  photo: string | null;
  bio: string | null;
  averageRating: number | null;
  reviewsCount: number;
}

export interface Program {
  id: string;
  title: string;
  description: string;
  cuisineType: string;
  difficultyLevel: 'BEGINNER' | 'ADVANCED';
  requiresComplexEquipment: boolean;
  dishes: string[];
  photos: string[];
}

export interface ProgramInput {
  title: string;
  description: string;
  cuisineType: string;
  difficultyLevel: 'BEGINNER' | 'ADVANCED';
  requiresComplexEquipment: boolean;
  dishes: string[];
  photos: string[];
}

export interface SlotSummary {
  id: string;
  programId: string;
  programTitle: string;
  chef: ChefPublic;
  startAt: string;
  durationMinutes: number;
  capacityTotal: number;
  freeSpots: number;
  status: SlotStatus;
  cancellationReason: string | null;
  rentalSetsAvailable: number;
  rentalPricePerSet: number | null;
}

export interface SlotDetails extends SlotSummary {
  program: Program;
}

export interface ClientProfile {
  allergyNote: string | null;
  lateCancelCount: number;
  blockedUntil: string | null;
}

export interface CurrentUser {
  id: string;
  role: UserRole;
  name: string | null;
  email: string;
  enabled: boolean;
  createdAt: string;
  clientProfile?: ClientProfile;
}

export interface AuthResult {
  accessToken: string;
  tokenType: 'Bearer';
  isNewUser: boolean;
  user: CurrentUser;
}

export interface Booking {
  id: string;
  slot: SlotSummary;
  status: BookingStatus;
  equipmentChoice: EquipmentChoice;
  rentalPriceSnapshot: number | null;
  createdAt: string;
  cancelledAt: string | null;
  isLateCancellation: boolean | null;
  hasReview: boolean;
}

export interface ParticipantBooking {
  id: string;
  clientName: string | null;
  allergyNote: string | null;
  status: BookingStatus;
  equipmentChoice: EquipmentChoice;
}

export interface ChefResponse {
  id: string;
  name: string | null;
  photo: string | null;
  bio: string | null;
  averageRating: number | null;
  reviewsCount: number;
  email: string;
  createdAt: string;
}

export interface ChefCreateRequest {
  email: string;
  name: string;
  photo?: string;
  bio?: string;
}

export interface ChefUpdateRequest {
  photo?: string | null;
  bio?: string | null;
}

export interface SlotCreateRequest {
  programId: string;
  chefId: string;
  startAt: string;
  capacityTotal: number;
  rentalSetsAvailable: number;
  rentalPricePerSet: number | null;
}

export interface Review {
  id: string;
  bookingId: string;
  chefRating: number;
  programRating: number;
  comment: string | null;
  createdAt: string;
}

export interface StudioSettings {
  address: string;
  contactPhone: string;
  contactEmail: string;
}

export interface ApiErrorBody {
  code: string;
  message: string;
  details?: Record<string, unknown>;
  blockedUntil?: string;
}

export class ApiError extends Error {
  status: number;
  code: string;
  details?: Record<string, unknown>;
  blockedUntil?: string;

  constructor(status: number, body: ApiErrorBody) {
    super(body.message);
    this.status = status;
    this.code = body.code;
    this.details = body.details;
    this.blockedUntil = body.blockedUntil;
  }
}
