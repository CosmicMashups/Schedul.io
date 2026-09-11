import { useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { ArrowRight, CalendarCheck2, Hourglass } from 'lucide-react';
import { getAppointment } from '../api/booking';
import type { AppointmentResponse } from '../api/types';
import { StatusPill } from '../components/StatusPill';
import { CardSkeleton } from '../components/Skeleton';
export default function BookingConfirmation() {
  const { id } = useParams<{ id: string }>();
  const [appointment, setAppointment] = useState<AppointmentResponse | null>(null);

  useEffect(() => {
    if (id) getAppointment(id).then(setAppointment).catch(() => {});
  }, [id]);

  if (!appointment) return <div className="max-w-sm mx-auto"><CardSkeleton /></div>;

  const start = new Date(appointment.scheduledStart);
  const confirmationCode = appointment.id.slice(0, 8).toUpperCase();
  const isConfirmed = appointment.status === 'CONFIRMED';

  return (
    <div className="max-w-sm mx-auto text-center animate-page">
      <div className="relative mx-auto mb-5 flex h-14 w-14 items-center justify-center rounded-full bg-teal/10 text-teal animate-scale-in">
        {isConfirmed && <span className="pulse-ring absolute inset-0 rounded-full text-teal" />}
        {isConfirmed ? <CalendarCheck2 size={26} strokeWidth={1.75} className="relative" /> : <Hourglass size={26} strokeWidth={1.75} className="relative" />}
      </div>
      <p className="font-mono text-xs uppercase tracking-widest text-teal mb-3">
        {isConfirmed ? "You're booked" : 'Request received'}
      </p>
      <h1 className="font-display text-3xl text-ink mb-8">
        {isConfirmed ? 'See you soon.' : "We'll confirm shortly."}
      </h1>

      <div className="pass-stub overflow-hidden text-left animate-scale-in">
        <div className="p-5">
          <div className="flex items-start justify-between mb-4">
            <div>
              <p className="text-xs text-ink/50">Appointment</p>
              <p className="font-mono text-sm text-ink tabular-nums">{confirmationCode}</p>
            </div>
            <StatusPill status={appointment.status} />
          </div>
          <p className="font-display text-xl text-ink mb-1">
            {start.toLocaleDateString(undefined, { weekday: 'long', month: 'long', day: 'numeric' })}
          </p>
          <p className="text-ink/60 tabular-nums">
            {start.toLocaleTimeString(undefined, { hour: 'numeric', minute: '2-digit' })}
          </p>
          {appointment.reason && <p className="text-sm text-ink/50 mt-3 pt-3 border-t border-line">{appointment.reason}</p>}
        </div>
        <div className="pass-perforation px-5 py-3 bg-mist/50 flex justify-between items-center">
          <span className="text-xs text-ink/50">Schedul.io</span>
          <span className="font-mono text-xs text-ink/40 tabular-nums">#{confirmationCode}</span>
        </div>
      </div>

      <Link to="/appointments" className="inline-flex items-center gap-1.5 mt-8 text-sm text-teal hover:text-teal-dark focus-ring rounded">
        View all my appointments <ArrowRight size={14} />
      </Link>
    </div>
  );
}
