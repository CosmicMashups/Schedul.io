import { useEffect, useRef, useState } from 'react';

/**
 * Animates a number from its previous value to `target` over `durationMs`. Used for dashboard
 * stat cards — a static number popping in feels flat; counting up gives the dashboard a sense
 * of "live data" without implying more precision than the underlying metric actually has.
 */
export function useCountUp(target: number, durationMs = 600) {
  const [value, setValue] = useState(0);
  const startRef = useRef<number | null>(null);
  const fromRef = useRef(0);

  useEffect(() => {
    fromRef.current = value;
    startRef.current = null;
    let frame: number;

    function step(timestamp: number) {
      if (startRef.current === null) startRef.current = timestamp;
      const progress = Math.min((timestamp - startRef.current) / durationMs, 1);
      const eased = 1 - Math.pow(1 - progress, 3); // ease-out-cubic
      setValue(Math.round(fromRef.current + (target - fromRef.current) * eased));
      if (progress < 1) frame = requestAnimationFrame(step);
    }
    frame = requestAnimationFrame(step);
    return () => cancelAnimationFrame(frame);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [target]);

  return value;
}
