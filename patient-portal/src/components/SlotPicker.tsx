import { useMemo, useRef } from 'react';
import { CalendarClock } from 'lucide-react';
import type { SlotResponse } from '../api/types';
import { EmptyState } from './EmptyState';
import { Skeleton } from './Skeleton';

function formatDay(iso: string) {
  return new Date(iso).toLocaleDateString(undefined, { weekday: 'short', month: 'short', day: 'numeric' });
}

function formatTime(iso: string) {
  return new Date(iso).toLocaleTimeString(undefined, { hour: 'numeric', minute: '2-digit' });
}

export function SlotPicker({
  slots, selectedSlotId, onSelect, loading = false,
}: { slots: SlotResponse[]; selectedSlotId: string | null; onSelect: (slot: SlotResponse) => void; loading?: boolean }) {
  // Flat index across every day's slots, in render order — lets arrow keys move linearly
  // through the whole picker (booking is the highest-conversion-critical control in this app,
  // worth keyboard support beyond default Tab-through).
  const buttonRefs = useRef<(HTMLButtonElement | null)[]>([]);

  const byDay = useMemo(() => {
    const map = new Map<string, SlotResponse[]>();
    for (const slot of slots) {
      const key = formatDay(slot.start);
      if (!map.has(key)) map.set(key, []);
      map.get(key)!.push(slot);
    }
    return map;
  }, [slots]);

  // Fixed: every caller fetches availability asynchronously and starts from an empty slots
  // array, so without a `loading` prop this flashed "No open slots in this range" for a moment
  // on every doctor/service/date change, before the real slots arrived.
  if (loading) {
    return (
      <div className="flex flex-wrap gap-2">
        {Array.from({ length: 6 }).map((_, i) => <Skeleton key={i} className="h-9 w-16 rounded-lg" />)}
      </div>
    );
  }

  if (slots.length === 0) {
    return <EmptyState icon={CalendarClock} title="No open slots in this range" description="Try a later date range." />;
  }

  function focusButton(index: number) {
    const clamped = Math.max(0, Math.min(slots.length - 1, index));
    buttonRefs.current[clamped]?.focus();
  }

  function onKeyDown(e: React.KeyboardEvent<HTMLButtonElement>, index: number) {
    switch (e.key) {
      case 'ArrowRight':
      case 'ArrowDown':
        e.preventDefault();
        focusButton(index + 1);
        break;
      case 'ArrowLeft':
      case 'ArrowUp':
        e.preventDefault();
        focusButton(index - 1);
        break;
      case 'Home':
        e.preventDefault();
        focusButton(0);
        break;
      case 'End':
        e.preventDefault();
        focusButton(slots.length - 1);
        break;
    }
  }

  let flatIndex = -1;

  return (
    <div className="space-y-5">
      {[...byDay.entries()].map(([day, daySlots]) => (
        <div key={day}>
          <h3 className="text-xs font-medium text-ink/50 uppercase tracking-wide mb-2">{day}</h3>
          <div className="flex flex-wrap gap-2">
            {daySlots.map((slot) => {
              flatIndex += 1;
              const index = flatIndex;
              return (
                <button
                  key={slot.slotId}
                  ref={(el) => { buttonRefs.current[index] = el; }}
                  onClick={() => onSelect(slot)}
                  onKeyDown={(e) => onKeyDown(e, index)}
                  className={`btn-press rounded-lg border px-3 py-2 text-sm font-mono tabular-nums focus-ring transition-colors ${
                    selectedSlotId === slot.slotId
                      ? 'border-teal bg-teal text-white shadow-sm shadow-teal/30'
                      : 'border-line bg-paper text-ink hover:border-teal'
                  }`}
                >
                  {formatTime(slot.start)}
                </button>
              );
            })}
          </div>
        </div>
      ))}
    </div>
  );
}
