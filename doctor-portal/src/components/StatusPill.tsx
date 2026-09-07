const STATUS_STYLES: Record<string, string> = {
  REQUESTED: 'bg-amber/15 text-amber',
  PENDING_CONFIRMATION: 'bg-amber/15 text-amber',
  CONFIRMED: 'bg-teal/15 text-teal-light',
  CHECKED_IN: 'bg-teal/15 text-teal-light',
  IN_QUEUE: 'bg-teal/15 text-teal-light',
  IN_CONSULTATION: 'bg-teal/15 text-teal-light',
  COMPLETED: 'bg-white/10 text-ink/50',
  REJECTED: 'bg-coral/15 text-coral',
  CANCELLED: 'bg-coral/15 text-coral',
  RESCHEDULED: 'bg-white/10 text-ink/50',
  NO_SHOW: 'bg-coral/15 text-coral',
  LEFT_WITHOUT_BEING_SEEN: 'bg-coral/15 text-coral',
};

const STATUS_LABELS: Record<string, string> = {
  PENDING_CONFIRMATION: 'Awaiting confirmation',
  IN_QUEUE: 'In queue',
  IN_CONSULTATION: 'In consultation',
  NO_SHOW: 'No-show',
  LEFT_WITHOUT_BEING_SEEN: 'Left without being seen',
};

export function StatusPill({ status }: { status: string }) {
  const style = STATUS_STYLES[status] ?? 'bg-white/10 text-ink/50';
  const label = STATUS_LABELS[status] ?? status.charAt(0) + status.slice(1).toLowerCase().replace(/_/g, ' ');
  return (
    <span className={`inline-flex items-center rounded-full px-2.5 py-1 text-xs font-medium ${style}`}>
      {label}
    </span>
  );
}
