import { useEffect, useRef } from 'react';
import { gsap } from 'gsap';
import { ScrollTrigger } from 'gsap/ScrollTrigger';

gsap.registerPlugin(ScrollTrigger);

/**
 * Reveals the direct children of the returned ref, one at a time, as the section scrolls into
 * view — motivated by storytelling: the landing page's sections are a sequence (discover, book,
 * trust, act), and revealing them in order as the visitor scrolls reinforces that sequence
 * instead of dumping everything on screen at once. Honors prefers-reduced-motion by skipping
 * straight to the final state. GSAP is used here (not CSS `.animate-*` utilities) because the
 * trigger point depends on scroll position, not mount time.
 */
export function useScrollReveal<T extends HTMLElement>(selector = ':scope > *') {
  const ref = useRef<T | null>(null);

  useEffect(() => {
    const root = ref.current;
    if (!root) return;

    const reduceMotion = window.matchMedia('(prefers-reduced-motion: reduce)').matches;
    const targets = root.querySelectorAll<HTMLElement>(selector);
    if (targets.length === 0) return;

    if (reduceMotion) {
      gsap.set(targets, { opacity: 1, y: 0 });
      return;
    }

    const ctx = gsap.context(() => {
      gsap.set(targets, { opacity: 0, y: 28 });
      gsap.to(targets, {
        opacity: 1,
        y: 0,
        duration: 0.7,
        ease: 'power3.out',
        stagger: 0.12,
        scrollTrigger: {
          trigger: root,
          start: 'top 80%',
          once: true,
        },
      });
    }, root);

    return () => ctx.revert();
  }, [selector]);

  return ref;
}
