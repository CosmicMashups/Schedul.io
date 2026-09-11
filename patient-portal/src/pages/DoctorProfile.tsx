import { useEffect, useMemo, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { CalendarCheck, ClipboardList, MapPin, Stethoscope, UserX } from 'lucide-react';
import { getDoctor, listServices, listAppointmentTypes, getAvailability, holdSlot, releaseSlot, bookAppointment } from '../api/booking';
import { ApiError } from '../api/client';
import type { PractitionerResponse, ServiceResponse, SlotResponse } from '../api/types';
import { SelectField } from '../components/Field';
import { SlotPicker } from '../components/SlotPicker';
import { CardSkeleton } from '../components/Skeleton';
import { EmptyState } from '../components/EmptyState';
import { useToast } from '../components/Toast';
import { Spinner } from '../components/Spinner';

export default function DoctorProfile() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const toast = useToast();

  const [doctor, setDoctor] = useState<PractitionerResponse | null>(null);
  const [services, setServices] = useState<ServiceResponse[]>([]);
  const [serviceId, setServiceId] = useState('');
  const [slots, setSlots] = useState<SlotResponse[]>([]);
  const [selectedSlot, setSelectedSlot] = useState<SlotResponse | null>(null);
  const [reason, setReason] = useState('');
  const [loading, setLoading] = useState(true);
  const [booking, setBooking] = useState(false);
  const [availabilityLoading, setAvailabilityLoading] = useState(false);

  // Fixed gap: previously resolved clinicId by string-matching a clinic *name* returned on
  // the practitioner, since PractitionerResponse only exposed names. It now exposes
  // clinicIds directly (see backend PractitionerResponse), so this is a straight lookup.
  const clinicId = doctor?.clinicIds[0];

  const eligibleServices = useMemo(
    () => services.filter((s) => doctor && (!s.allowedSpecialtyId || doctor.specialtyIds.includes(s.allowedSpecialtyId))),
    [services, doctor]
  );

  useEffect(() => {
    if (!id) return;
    (async () => {
      setLoading(true);
      try {
        const [doc, svc] = await Promise.all([getDoctor(id), listServices()]);
        setDoctor(doc);
        setServices(svc);
      } catch (err) {
        toast.show(err instanceof ApiError ? err.message : 'Could not load this doctor right now.', 'error');
      } finally {
        setLoading(false);
      }
    })();
  }, [id]);

  async function loadAvailability(svcId: string) {
    if (!id) return;
    setSlots([]);
    setSelectedSlot(null);
    setAvailabilityLoading(true);
    const from = new Date().toISOString().slice(0, 10);
    const to = new Date(Date.now() + 14 * 86400_000).toISOString().slice(0, 10);
    try {
      const res = await getAvailability({ practitionerId: id, serviceId: svcId, clinicId, from, to });
      setSlots(res.slots);
    } catch (err) {
      toast.show(err instanceof ApiError ? err.message : 'Could not load availability.', 'error');
    } finally {
      setAvailabilityLoading(false);
    }
  }

  async function onSelectSlot(slot: SlotResponse) {
    if (selectedSlot && selectedSlot.slotId !== slot.slotId) {
      releaseSlot(selectedSlot.slotId).catch(() => {});
    }
    setSelectedSlot(slot);
    try {
      await holdSlot(slot.slotId);
    } catch (err) {
      toast.show(err instanceof ApiError ? err.message : 'That slot was just taken — please pick another.', 'error');
      setSelectedSlot(null);
      loadAvailability(serviceId);
    }
  }

  async function onBook() {
    const patientId = localStorage.getItem('clinic.patientId');
    if (!patientId) {
      toast.show('Please create your patient record first.', 'error');
      navigate('/register');
      return;
    }
    if (!id || !selectedSlot || !clinicId) return;

    setBooking(true);
    try {
      const types = await listAppointmentTypes();
      const defaultType = types.find((t) => t.code === 'GENERAL') ?? types[0];
      if (!defaultType) throw new Error('No appointment type is configured for this clinic yet.');

      const appointment = await bookAppointment({
        patientId, practitionerId: id, clinicId, serviceId,
        appointmentTypeId: defaultType.id, slotId: selectedSlot.slotId,
        reason: reason || undefined, source: 'ONLINE',
      });
      navigate(`/appointments/${appointment.id}/confirmed`);
    } catch (err) {
      toast.show(err instanceof ApiError ? err.message : 'Booking failed. Please try again.', 'error');
    } finally {
      setBooking(false);
    }
  }

  if (loading) {
    return <div className="space-y-4 animate-page"><CardSkeleton /><CardSkeleton /></div>;
  }
  if (!doctor) {
    return (
      <div className="animate-page">
        <EmptyState icon={UserX} title="Doctor not found" description="This profile may have been removed. Try searching again." />
      </div>
    );
  }

  return (
    <div className="animate-page">
      <div className="flex items-center gap-4 mb-1">
        <div className="flex h-14 w-14 shrink-0 items-center justify-center rounded-full bg-teal/10 text-teal">
          <Stethoscope size={24} strokeWidth={1.75} />
        </div>
        <div>
          <h1 className="font-display text-2xl text-ink">Dr. {doctor.firstName} {doctor.lastName}</h1>
          <p className="text-sm text-ink/60 flex items-center gap-1.5 mt-0.5">
            <MapPin size={13} /> {doctor.specialties.join(', ') || 'General practice'} · {doctor.clinics.join(', ')}
          </p>
        </div>
      </div>

      <div className="my-8 h-px bg-line" />

      <div className="mb-6">
        <SelectField
          label="What do you need?"
          value={serviceId}
          onChange={(v) => { setServiceId(v); loadAvailability(v); }}
          options={eligibleServices.map((s) => ({
            value: s.id,
            label: `${s.name} — ${s.durationMinutes} min${s.price ? ` · ₱${s.price.toLocaleString()}` : ''}`,
          }))}
        />
      </div>

      {serviceId && (
        <div className="mb-6 animate-scale-in">
          <h2 className="text-xs font-medium text-ink/60 uppercase tracking-wide mb-3 flex items-center gap-1.5">
            <CalendarCheck size={14} /> Available times
          </h2>
          <SlotPicker slots={slots} selectedSlotId={selectedSlot?.slotId ?? null} onSelect={onSelectSlot} loading={availabilityLoading} />
        </div>
      )}

      {selectedSlot && (
        <div className="pass-stub p-5 mb-6 animate-scale-in">
          <label className="block mb-4">
            <span className="flex items-center gap-1.5 text-xs font-medium text-ink/60 mb-1.5">
              <ClipboardList size={13} /> Reason for visit (optional)
            </span>
            <textarea
              value={reason}
              onChange={(e) => setReason(e.target.value)}
              rows={2}
              className="w-full rounded-lg border border-line bg-paper px-3.5 py-2.5 text-sm focus-ring"
              placeholder="e.g. Annual check-up, follow-up on lab results…"
            />
          </label>
          <div className="flex items-center justify-between">
            <p className="text-sm text-ink/70">
              {new Date(selectedSlot.start).toLocaleString(undefined, { weekday: 'long', month: 'long', day: 'numeric', hour: 'numeric', minute: '2-digit' })}
            </p>
            <button
              onClick={onBook}
              disabled={booking}
              className="btn-press flex items-center gap-2 rounded-full bg-teal px-6 py-2.5 text-white text-sm font-medium hover:bg-teal-dark focus-ring disabled:opacity-50 shadow-sm shadow-teal/20"
            >
              {booking && <Spinner size={14} />}
              {booking ? 'Booking…' : 'Confirm booking'}
            </button>
          </div>
        </div>
      )}
    </div>
  );
}
