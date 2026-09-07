import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { CalendarX2, ChevronRight, Clock } from 'lucide-react';
import { listMyAppointments } from '../api/booking';
import { ApiError } from '../api/client';
import type { AppointmentResponse } from '../api/types';
import { StatusPill } from '../components/StatusPill';
import { CardSkeleton } from '../components/Skeleton';
import { EmptyState } from '../components/EmptyState';
import { useToast } from '../components/Toast';

export default function MyAppointments() {
  const [appointments, setAppointments] = useState<AppointmentResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [noRecord, setNoRecord] = useState(false);
  const toast = useToast();

  useEffect(() => {
    const patientId = localStorage.getItem('clinic.patientId');
    if (!patientId) {
      setNoRecord(true);
      setLoading(false);
      return;
    }
    listMyAppointments(patientId)
      .then(setAppointments)
      .catch((err) => toast.show(err instanceof ApiError ? err.message : 'Could not load your appointments.', 'error'))
      .finally(() => setLoading(false));
  }, []);

  return (
    <div className="animate-page">
      <h1 className="font-display text-2xl text-ink mb-6">My appointments</h1>

      {loading && <div className="space-y-3"><CardSkeleton /><CardSkeleton /></div>}

      {!loading && noRecord && (
        <EmptyState
          icon={CalendarX2}
          title="No patient record on this device"
          description="Please register or sign in again to see your appointments."
          action={<Link to="/register" className="btn-press rounded-full bg-teal px-5 py-2 text-sm text-white font-medium hover:bg-teal-dark focus-ring">Create record</Link>}
        />
      )}

      {!loading && !noRecord && appointments.length === 0 && (
        <EmptyState
          icon={Clock}
          title="No appointments yet"
          action={<Link to="/doctors" className="btn-press rounded-full bg-teal px-5 py-2 text-sm text-white font-medium hover:bg-teal-dark focus-ring">Find a doctor</Link>}
        />
      )}

      <div className="space-y-3">
        {appointments.map((a, i) => (
          <Link
            key={a.id}
            to={`/appointments/${a.id}`}
            style={{ animationDelay: `${i * 40}ms` }}
            className="animate-scale-in card-interactive flex items-center justify-between gap-3 rounded-card border border-line bg-paper p-4 hover:border-teal focus-ring"
          >
            <div className="min-w-0">
              <p className="font-medium text-ink">
                {new Date(a.scheduledStart).toLocaleDateString(undefined, { weekday: 'short', month: 'short', day: 'numeric' })}
                {' · '}
                {new Date(a.scheduledStart).toLocaleTimeString(undefined, { hour: 'numeric', minute: '2-digit' })}
              </p>
              {a.reason && <p className="text-sm text-ink/50 mt-0.5 truncate">{a.reason}</p>}
            </div>
            <div className="flex items-center gap-2 shrink-0">
              <StatusPill status={a.status} />
              <ChevronRight size={16} className="text-ink/20" />
            </div>
          </Link>
        ))}
      </div>
    </div>
  );
}
