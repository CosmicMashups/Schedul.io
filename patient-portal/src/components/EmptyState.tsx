import type { LucideIcon } from 'lucide-react';
import type { ReactNode } from 'react';

export function EmptyState({
  icon: Icon, title, description, action,
}: { icon: LucideIcon; title: string; description?: string; action?: ReactNode }) {
  return (
    <div className="text-center py-14 px-6">
      <div className="mx-auto mb-4 flex h-12 w-12 items-center justify-center rounded-full bg-teal/10 text-teal">
        <Icon size={22} strokeWidth={1.75} />
      </div>
      <p className="font-medium text-ink mb-1">{title}</p>
      {description && <p className="text-sm text-ink/50 max-w-xs mx-auto mb-4">{description}</p>}
      {action}
    </div>
  );
}
