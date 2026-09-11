import { useEffect, useMemo, useState } from 'react';
import { ChevronLeft, ChevronRight, LayoutGrid, List } from 'lucide-react';
import { searchAppointments } from '../api/appointments';
import { listClinics, searchPractitioners } from '../api/clinic';
import { ApiError } from '../api/client';
import type { AppointmentResponse, ClinicResponse, PractitionerResponse } from '../api/types';
import { Skeleton } from '../components/Skeleton';
import { useToast } from '../components/Toast';

const DAY_NAMES = ['Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat'];
const DAY_START_HOUR = 7;
const DAY_END_HOUR = 19;
const HOUR_HEIGHT_PX = 52;

// Six distinct appointment states now get six visually distinct treatments — previously
// CONFIRMED/CHECKED_IN/IN_QUEUE all shared the identical class, making them indistinguishable
// on the calendar grid.
const STATUS_COLOR: Record<string, string> = {
  PENDING_CONFIRMATION: 'bg-amber/20 border-amber text-amber',
  CONFIRMED: 'bg-teal/10 border-teal/50 text-teal-dark',
  CHECKED_IN: 'bg-teal/20 border-teal text-teal-dark',
  IN_QUEUE: 'bg-teal/20 border-teal border-dashed text-teal-dark font-semibold',
  IN_CONSULTATION: 'bg-teal border-teal-dark text-white font-semibold',
  COMPLETED: 'bg-ink/5 border-line text-ink/50',
  CANCELLED: 'bg-coral/10 border-coral text-coral line-through',
  NO_SHOW: 'bg-coral/10 border-coral text-coral',
};

function startOfWeek(d: Date) {
  const date = new Date(d);
  date.setHours(0, 0, 0, 0);
  date.setDate(date.getDate() - date.getDay());
  return date;
}

function startOfMonth(d: Date) {
  return new Date(d.getFullYear(), d.getMonth(), 1);
}

function addDays(d: Date, n: number) {
  const date = new Date(d);
  date.setDate(date.getDate() + n);
  return date;
}

function sameDay(a: Date, b: Date) {
  return a.toDateString() === b.toDateString();
}

export default function Calendar() {
  const [view, setView] = useState<'week' | 'month'>('week');
  const [anchor, setAnchor] = useState(new Date());
  const [appointments, setAppointments] = useState<AppointmentResponse[]>([]);
  const [clinics, setClinics] = useState<ClinicResponse[]>([]);
  const [doctors, setDoctors] = useState<PractitionerResponse[]>([]);
  const [clinicId, setClinicId] = useState('');
  const [practitionerId, setPractitionerId] = useState('');
  const [loading, setLoading] = useState(true);
  const toast = useToast();

  useEffect(() => {
    Promise.all([listClinics(), searchPractitioners()]).then(([cls, docs]) => {
      setClinics(cls);
      setDoctors(docs);
    }).catch(() => {});
  }, []);

  const { rangeStart, rangeEnd } = useMemo(() => {
    if (view === 'week') {
      const start = startOfWeek(anchor);
      return { rangeStart: start, rangeEnd: addDays(start, 7) };
    }
    const monthStart = startOfMonth(anchor);
    const gridStart = startOfWeek(monthStart);
    return { rangeStart: gridStart, rangeEnd: addDays(gridStart, 42) };
  }, [view, anchor]);

  useEffect(() => {
    setLoading(true);
    searchAppointments({
      clinicId: clinicId || undefined,
      practitionerId: practitionerId || undefined,
      fromDate: rangeStart.toISOString(),
      toDate: rangeEnd.toISOString(),
    })
      .then(setAppointments)
      .catch((err) => toast.show(err instanceof ApiError ? err.message : 'Could not load appointments.', 'error'))
      .finally(() => setLoading(false));
  }, [rangeStart, rangeEnd, clinicId, practitionerId]);

  function navigate(direction: -1 | 1) {
    setAnchor((prev) => (view === 'week' ? addDays(prev, 7 * direction) : new Date(prev.getFullYear(), prev.getMonth() + direction, 1)));
  }

  function onAppointmentSelect(a: AppointmentResponse) {
    const time = new Date(a.scheduledStart).toLocaleTimeString(undefined, { hour: 'numeric', minute: '2-digit' });
    toast.show(`${time} · ${a.reason ?? a.status.replace(/_/g, ' ').toLowerCase()}`);
  }

  const rangeLabel = view === 'week'
    ? `${rangeStart.toLocaleDateString(undefined, { month: 'short', day: 'numeric' })} – ${addDays(rangeStart, 6).toLocaleDateString(undefined, { month: 'short', day: 'numeric' })}`
    : anchor.toLocaleDateString(undefined, { month: 'long', year: 'numeric' });

  return (
    <div className="animate-page">
      <div className="flex items-center justify-between mb-6 flex-wrap gap-3">
        <div className="flex items-center gap-3">
          <LayoutGrid size={20} className="text-teal" />
          <h1 className="text-xl font-semibold text-ink">Calendar</h1>
        </div>

        <div className="flex items-center gap-2">
          <select value={clinicId} onChange={(e) => setClinicId(e.target.value)} className="rounded-lg border border-line bg-panel px-3 py-2 text-sm focus-ring">
            <option value="">All clinics</option>
            {clinics.map((c) => <option key={c.id} value={c.id}>{c.name}</option>)}
          </select>
          <select value={practitionerId} onChange={(e) => setPractitionerId(e.target.value)} className="rounded-lg border border-line bg-panel px-3 py-2 text-sm focus-ring">
            <option value="">All doctors</option>
            {doctors.map((d) => <option key={d.id} value={d.id}>Dr. {d.lastName}</option>)}
          </select>
          <div className="flex rounded-lg border border-line overflow-hidden">
            <button onClick={() => setView('week')} className={`px-3 py-2 text-xs font-medium focus-ring ${view === 'week' ? 'bg-teal text-white' : 'bg-panel text-ink/60 hover:bg-paper'}`}>Week</button>
            <button onClick={() => setView('month')} className={`px-3 py-2 text-xs font-medium focus-ring ${view === 'month' ? 'bg-teal text-white' : 'bg-panel text-ink/60 hover:bg-paper'}`}>Month</button>
          </div>
        </div>
      </div>

      <div className="flex items-center justify-between mb-4">
        <div className="flex items-center gap-1">
          <button onClick={() => navigate(-1)} className="btn-press flex h-8 w-8 items-center justify-center rounded-lg border border-line hover:border-teal focus-ring"><ChevronLeft size={15} /></button>
          <button onClick={() => navigate(1)} className="btn-press flex h-8 w-8 items-center justify-center rounded-lg border border-line hover:border-teal focus-ring"><ChevronRight size={15} /></button>
          <button onClick={() => setAnchor(new Date())} className="btn-press ml-1 rounded-lg border border-line px-3 py-1.5 text-xs font-medium hover:border-teal focus-ring">Today</button>
        </div>
        <p className="text-sm font-medium text-ink tabular-nums">{rangeLabel}</p>
        <div className="flex items-center gap-1.5 text-xs text-ink/40 tabular-nums">
          <List size={13} /> {appointments.length} appointment{appointments.length === 1 ? '' : 's'}
        </div>
      </div>

      {loading ? (
        view === 'week' ? <WeekGridSkeleton /> : <MonthGridSkeleton />
      ) : view === 'week' ? (
        <WeekGrid weekStart={rangeStart} appointments={appointments} onSelect={onAppointmentSelect} />
      ) : (
        <MonthGrid gridStart={rangeStart} monthAnchor={anchor} appointments={appointments} onDayClick={(d) => { setAnchor(d); setView('week'); }} />
      )}
    </div>
  );
}

function WeekGridSkeleton() {
  return (
    <div className="panel overflow-hidden">
      <div className="grid grid-cols-[56px_repeat(7,1fr)] border-b border-line">
        <div />
        {Array.from({ length: 7 }).map((_, i) => (
          <div key={i} className="px-2 py-2.5 border-l border-line space-y-1.5">
            <Skeleton className="h-2.5 w-6 mx-auto" />
            <Skeleton className="h-4 w-4 mx-auto rounded-full" />
          </div>
        ))}
      </div>
      <div className="p-3 space-y-2.5">
        {Array.from({ length: 6 }).map((_, i) => <Skeleton key={i} className="h-8" />)}
      </div>
    </div>
  );
}

function MonthGridSkeleton() {
  return (
    <div className="panel overflow-hidden">
      <div className="grid grid-cols-7 border-b border-line">
        {DAY_NAMES.map((d) => (
          <div key={d} className="px-2 py-2 text-center text-[10px] uppercase tracking-wide text-ink/40 border-l border-line first:border-l-0">{d}</div>
        ))}
      </div>
      <div className="grid grid-cols-7">
        {Array.from({ length: 35 }).map((_, i) => (
          <div key={i} className="min-h-24 p-1.5 border-l border-t border-line first:border-l-0">
            <Skeleton className="h-4 w-4 rounded-full mb-1.5" />
            {i % 3 === 0 && <Skeleton className="h-3 w-full" />}
          </div>
        ))}
      </div>
    </div>
  );
}

/** Current-time indicator: a live horizontal line + dot across today's column, so the grid
    reads as a real-time ops view rather than a static schedule printout. Absent when today
    falls outside the visible week or outside clinic hours. */
function NowLine({ weekStart }: { weekStart: Date }) {
  const [now, setNow] = useState(new Date());
  useEffect(() => {
    const id = setInterval(() => setNow(new Date()), 60_000);
    return () => clearInterval(id);
  }, []);

  const dayIndex = Array.from({ length: 7 }, (_, i) => addDays(weekStart, i)).findIndex((d) => sameDay(d, now));
  const hour = now.getHours() + now.getMinutes() / 60;
  if (dayIndex === -1 || hour < DAY_START_HOUR || hour > DAY_END_HOUR) return null;

  const top = (hour - DAY_START_HOUR) * HOUR_HEIGHT_PX;
  const leftPct = (dayIndex / 7) * 100;
  const widthPct = 100 / 7;

  return (
    <div
      className="absolute z-20 flex items-center pointer-events-none"
      style={{ top, left: `${leftPct}%`, width: `${widthPct}%` }}
    >
      <span className="h-2 w-2 rounded-full bg-coral ring-4 ring-coral/20 -ml-1" />
      <span className="h-px flex-1 bg-coral/70" />
    </div>
  );
}

function WeekGrid({ weekStart, appointments, onSelect }: { weekStart: Date; appointments: AppointmentResponse[]; onSelect: (a: AppointmentResponse) => void }) {
  const days = Array.from({ length: 7 }, (_, i) => addDays(weekStart, i));
  const hours = Array.from({ length: DAY_END_HOUR - DAY_START_HOUR }, (_, i) => DAY_START_HOUR + i);

  return (
    <div className="panel overflow-hidden">
      <div className="grid grid-cols-[56px_repeat(7,1fr)] border-b border-line">
        <div />
        {days.map((d) => (
          <div key={d.toISOString()} className={`px-2 py-2.5 text-center border-l border-line ${sameDay(d, new Date()) ? 'bg-teal/5' : ''}`}>
            <p className="text-[10px] uppercase tracking-wide text-ink/40">{DAY_NAMES[d.getDay()]}</p>
            <p className={`text-sm font-medium tabular-nums ${sameDay(d, new Date()) ? 'text-teal' : 'text-ink'}`}>{d.getDate()}</p>
          </div>
        ))}
      </div>

      <div className="grid grid-cols-[56px_repeat(7,1fr)] relative" style={{ height: hours.length * HOUR_HEIGHT_PX }}>
        <div>
          {hours.map((h) => (
            <div key={h} style={{ height: HOUR_HEIGHT_PX }} className="text-right pr-2 -translate-y-2">
              <span className="text-[10px] text-ink/30 font-mono tabular-nums">{h % 12 === 0 ? 12 : h % 12}{h < 12 ? 'am' : 'pm'}</span>
            </div>
          ))}
        </div>

        <NowLine weekStart={weekStart} />

        {days.map((day) => {
          const dayAppointments = appointments.filter((a) => sameDay(new Date(a.scheduledStart), day));
          return (
            <div key={day.toISOString()} className="relative border-l border-line">
              {hours.map((h) => <div key={h} style={{ height: HOUR_HEIGHT_PX }} className="border-b border-line/50" />)}
              {dayAppointments.map((a) => {
                const start = new Date(a.scheduledStart);
                const end = new Date(a.scheduledEnd);
                const startOffsetMin = (start.getHours() - DAY_START_HOUR) * 60 + start.getMinutes();
                const durationMin = Math.max((end.getTime() - start.getTime()) / 60000, 20);
                const top = (startOffsetMin / 60) * HOUR_HEIGHT_PX;
                const height = (durationMin / 60) * HOUR_HEIGHT_PX;
                const colorClass = STATUS_COLOR[a.status] ?? 'bg-ink/5 border-line text-ink/60';
                const label = `${start.toLocaleTimeString(undefined, { hour: 'numeric', minute: '2-digit' })} — ${a.reason ?? a.status}`;
                return (
                  <button
                    key={a.id}
                    onClick={() => onSelect(a)}
                    title={label}
                    aria-label={label}
                    className={`absolute left-0.5 right-0.5 text-left rounded-md border-l-2 px-1.5 py-1 text-[10px] leading-tight overflow-hidden transition-transform hover:scale-[1.02] hover:z-10 focus-ring ${colorClass}`}
                    style={{ top, height: Math.max(height, 18) }}
                  >
                    <p className="font-medium truncate tabular-nums">{start.toLocaleTimeString(undefined, { hour: 'numeric', minute: '2-digit' })}</p>
                    {height > 30 && <p className="truncate opacity-80">{a.reason ?? a.status}</p>}
                  </button>
                );
              })}
            </div>
          );
        })}
      </div>
    </div>
  );
}

function MonthGrid({
  gridStart, monthAnchor, appointments, onDayClick,
}: { gridStart: Date; monthAnchor: Date; appointments: AppointmentResponse[]; onDayClick: (d: Date) => void }) {
  const days = Array.from({ length: 42 }, (_, i) => addDays(gridStart, i));
  const currentMonth = monthAnchor.getMonth();

  return (
    <div className="panel overflow-hidden">
      <div className="grid grid-cols-7 border-b border-line">
        {DAY_NAMES.map((d) => (
          <div key={d} className="px-2 py-2 text-center text-[10px] uppercase tracking-wide text-ink/40 border-l border-line first:border-l-0">{d}</div>
        ))}
      </div>
      <div className="grid grid-cols-7">
        {days.map((day) => {
          const dayAppointments = appointments.filter((a) => sameDay(new Date(a.scheduledStart), day));
          const isCurrentMonth = day.getMonth() === currentMonth;
          const isToday = sameDay(day, new Date());
          return (
            <button
              key={day.toISOString()}
              onClick={() => onDayClick(day)}
              className={`text-left min-h-24 p-1.5 border-l border-t border-line first:border-l-0 focus-ring transition-colors hover:bg-paper ${isCurrentMonth ? '' : 'bg-paper/50'}`}
            >
              <span className={`inline-flex h-5 w-5 items-center justify-center rounded-full text-xs tabular-nums mb-1 ${
                isToday ? 'bg-teal text-white font-medium' : isCurrentMonth ? 'text-ink' : 'text-ink/30'
              }`}>
                {day.getDate()}
              </span>
              <div className="space-y-0.5">
                {dayAppointments.slice(0, 3).map((a) => (
                  <div key={a.id} className={`truncate rounded px-1 py-0.5 text-[10px] border-l-2 tabular-nums ${STATUS_COLOR[a.status] ?? 'bg-ink/5 border-line text-ink/60'}`}>
                    {new Date(a.scheduledStart).toLocaleTimeString(undefined, { hour: 'numeric', minute: '2-digit' })}
                  </div>
                ))}
                {dayAppointments.length > 3 && <p className="text-[10px] text-ink/40 pl-1">+{dayAppointments.length - 3} more</p>}
              </div>
            </button>
          );
        })}
      </div>
    </div>
  );
}
