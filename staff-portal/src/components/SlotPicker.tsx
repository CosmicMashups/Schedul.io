import { CalendarClock } from 'lucide-react';
import type { SlotResponse } from '../api/types';
import { EmptyState } from './EmptyState';

function formatDay(iso: string) {
  return new Date(iso).toLocaleDateString(undefined, { weekday: 'short', month: 'short', day: 'numeric' });
}
function formatTime(iso: string) {
  return new Date(iso).toLocaleTimeString(undefined, { hour: 'numeric', minute: '2-digit' });
}

export function SlotPicker({
  slots, selectedSlotId, onSelect,
}: { slots: SlotResponse[]; selectedSlotId: string | null; onSelect: (slot: SlotResponse) => void }) {
  if (slots.length === 0) {
    return <EmptyState icon={CalendarClock} title="No open slots in this range" description="Try a different doctor or date range." />;
  }

  const byDay = new Map<string, SlotResponse[]>();
  for (const slot of slots) {
    const key = formatDay(slot.start);
    if (!byDay.has(key)) byDay.set(key, []);
    byDay.get(key)!.push(slot);
  }

  return (
    <div className="space-y-4 max-h-64 overflow-y-auto pr-1">
      {[...byDay.entries()].map(([day, daySlots]) => (
        <div key={day}>
          <h3 className="text-xs font-medium text-ink/50 uppercase tracking-wide mb-2">{day}</h3>
          <div className="flex flex-wrap gap-1.5">
            {daySlots.map((slot) => (
              <button
                key={slot.slotId}
                type="button"
                onClick={() => onSelect(slot)}
                className={`btn-press rounded-md border px-2.5 py-1.5 text-xs font-mono focus-ring transition-colors ${
                  selectedSlotId === slot.slotId ? 'border-teal bg-teal text-white' : 'border-line bg-panel text-ink hover:border-teal'
                }`}
              >
                {formatTime(slot.start)}
              </button>
            ))}
          </div>
        </div>
      ))}
    </div>
  );
}
