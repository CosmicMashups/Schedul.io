export interface ApiResponse<T> {
  success: boolean;
  data: T | null;
  error: { code: string; message: string; details?: unknown } | null;
}

export interface PractitionerResponse {
  id: string;
  firstName: string;
  lastName: string;
  credentials: string | null;
  defaultConsultationFee: number | null;
  status: string;
  specialties: string[];
  specialtyIds: string[];
  clinics: string[];
  clinicIds: string[];
}

export interface ServiceResponse {
  id: string;
  name: string;
  description: string | null;
  durationMinutes: number;
  bufferMinutes: number;
  price: number | null;
  consultationMode: string;
  allowedSpecialty: string | null;
  allowedSpecialtyId: string | null;
  status: string;
  clinics: string[];
  clinicIds: string[];
}

export interface ClinicResponse {
  id: string;
  name: string;
  addressLine: string;
  city: string | null;
  province: string | null;
  postalCode: string | null;
  contactNumber: string | null;
  timezone: string;
  status: string;
}

export interface SlotResponse {
  slotId: string;
  practitionerId: string;
  clinicId: string;
  start: string;
  end: string;
  status: string;
}

export interface AvailabilityResponse {
  practitionerId: string;
  serviceId: string | null;
  slots: SlotResponse[];
}

export interface AppointmentTypeResponse {
  id: string;
  code: string;
  name: string;
  confirmationPolicy: string;
  cancellationWindowHours: number | null;
  advanceBookingLimitDays: number | null;
  active: boolean;
}

export interface AppointmentResponse {
  id: string;
  patientId: string;
  practitionerId: string;
  clinicId: string;
  serviceId: string;
  appointmentTypeId: string;
  slotId: string;
  scheduledStart: string;
  scheduledEnd: string;
  status: string;
  reason: string | null;
  source: string;
  previousAppointmentId: string | null;
  replacementAppointmentId: string | null;
  cancellationReason: string | null;
  cancellationNote: string | null;
}

export interface PatientResponse {
  id: string;
  firstName: string;
  middleName: string | null;
  lastName: string;
  suffix: string | null;
  birthDate: string;
  sex: string;
  mobileNumber: string | null;
  email: string | null;
  addressLine: string | null;
  preferredContactMethod: string;
}

export interface LoginResponse {
  accessToken: string;
  tokenType: string;
  expiresInSeconds: number;
  userId: string;
  tenantId: string;
  email: string;
  roles: string[];
}
