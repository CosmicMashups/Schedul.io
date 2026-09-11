import { useEffect, useRef } from 'react';
import { Link } from 'react-router-dom';
import { gsap } from 'gsap';
import {
  ArrowRight, CalendarCheck2, MessageCircleHeart, Search, ShieldCheck, Sparkles, UserRound,
} from 'lucide-react';
import { useScrollReveal } from '../hooks/useScrollReveal';

export default function Home() {
  const heroRef = useRef<HTMLDivElement>(null);

  // Hero gets its own on-mount timeline (not scroll-triggered — it's already in view on load):
  // the kicker, headline, subtext, CTAs, and floating card stagger in as a first impression,
  // rather than the page just popping in fully-formed.
  useEffect(() => {
    const root = heroRef.current;
    if (!root) return;
    const reduceMotion = window.matchMedia('(prefers-reduced-motion: reduce)').matches;
    const targets = root.querySelectorAll<HTMLElement>('[data-hero-in]');
    if (reduceMotion || targets.length === 0) {
      gsap.set(targets, { opacity: 1, y: 0 });
      return;
    }
    const ctx = gsap.context(() => {
      gsap.set(targets, { opacity: 0, y: 20 });
      gsap.to(targets, { opacity: 1, y: 0, duration: 0.7, ease: 'power3.out', stagger: 0.1 });
    }, root);
    return () => ctx.revert();
  }, []);

  return (
    <div className="animate-page">
      {/* ---------- Hero ---------- */}
      {/* This page deliberately renders outside the app's max-w-3xl <main> container (see
          App.tsx's routing) so this section can be genuinely full-width with no breakout
          trick needed. */}
      <div ref={heroRef} className="relative min-h-[560px] sm:min-h-[620px] overflow-hidden bg-mist">
          {/* Photo slot: place hero-consultation.jpg at patient-portal/public/images/.
              Bottom-left text composition (deliberately not the generic centered/left-text
              -right-image default) — the image should carry negative space in its lower-left
              third. Falls back to a dark teal-to-ink gradient (not a pale one) so the white
              hero text stays legible with or without the real photo in place. */}
          <div
            aria-hidden
            className="absolute inset-0 bg-cover bg-center"
            style={{ backgroundImage: "url('/images/hero-consultation.jpg'), linear-gradient(135deg, #0E6E66 0%, #17262B 100%)" }}
          />
          {/* Three stacked layers: a flat wash guarantees a contrast floor everywhere on the
              photo (a directional gradient alone left the subtext's row too light wherever it
              crossed the photo's bright subject), plus two directional scrims that darken
              further toward the bottom-left where the text actually sits. */}
          <div aria-hidden className="absolute inset-0 bg-ink/35" />
          <div aria-hidden className="absolute inset-0 bg-gradient-to-t from-ink/70 via-ink/25 to-transparent" />
          <div aria-hidden className="absolute inset-0 bg-gradient-to-r from-ink/60 via-ink/15 to-transparent" />

          <div className="relative flex min-h-[560px] sm:min-h-[620px] items-end sm:items-center px-5 sm:px-10 pb-10 sm:pb-0 max-w-6xl mx-auto">
            <div className="grid grid-cols-1 lg:grid-cols-[1.1fr_0.9fr] gap-10 w-full items-end sm:items-center">
              <div>
                <p data-hero-in className="font-mono text-xs uppercase tracking-widest text-teal-light mb-4">Patient access</p>
                <h1 data-hero-in className="font-display text-4xl sm:text-5xl leading-[1.05] text-white mb-5 text-balance">
                  See a doctor,<br />on your schedule.
                </h1>
                <p data-hero-in className="text-white/80 max-w-md mb-8">
                  Search by doctor or specialty, pick a time that works, and get a confirmation you can
                  trust — no phone calls, no waiting on hold.
                </p>
                <div data-hero-in className="flex flex-wrap items-center gap-3">
                  <Link to="/doctors" className="btn-press flex items-center gap-2 rounded-full bg-teal px-6 py-3 text-white text-sm font-medium hover:bg-teal-dark focus-ring shadow-sm shadow-teal/20">
                    Find a doctor <ArrowRight size={16} />
                  </Link>
                  <Link to="/register" className="btn-press rounded-full border border-white/40 px-6 py-3 text-white text-sm font-medium hover:border-white focus-ring">
                    Create your record
                  </Link>
                </div>
              </div>

              <div data-hero-in className="relative lg:justify-self-end w-full max-w-sm">
                <div className="pass-stub p-5">
                  <div className="flex items-center justify-between mb-4">
                    <p className="text-[10px] font-mono uppercase tracking-widest text-teal">Next available</p>
                    <Sparkles size={14} className="text-amber" />
                  </div>
                  <p className="font-display text-2xl text-ink mb-0.5">Dr. Amara Osei</p>
                  <p className="text-xs text-ink/50 mb-4">General Practice · Schedul.io</p>
                  <div className="flex items-center gap-2 pass-perforation pl-4">
                    <p className="font-mono text-3xl text-ink tabular-nums">10:30</p>
                    <p className="text-xs text-ink/40 leading-tight">
                      Tomorrow<br />Room 3
                    </p>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>

      <div className="max-w-6xl mx-auto px-5 sm:px-6">
        {/* ---------- How it works ---------- */}
        <HowItWorks />

        {/* ---------- Trust / reassurance ---------- */}
        <TrustBand />

        {/* ---------- Why choose us ---------- */}
        <WhyUs />
      </div>

      {/* ---------- Closing CTA ---------- */}
      <ClosingCta />
    </div>
  );
}

function HowItWorks() {
  const ref = useScrollReveal<HTMLDivElement>();
  const steps = [
    { icon: Search, title: 'Search', desc: 'Browse doctors by specialty and see who has real open times this week.' },
    { icon: CalendarCheck2, title: 'Pick a time', desc: 'Hold your slot, confirm a few details, and you’re booked.' },
    { icon: UserRound, title: 'Show up', desc: 'Get a confirmation and a reminder — no call needed on either end.' },
  ];

  return (
    <section className="py-16 sm:py-20">
      <p className="font-mono text-xs uppercase tracking-widest text-teal mb-3">How it works</p>
      <h2 className="font-display text-3xl text-ink mb-10 max-w-md text-balance">Three steps, no phone tag.</h2>
      <div ref={ref} className="grid grid-cols-1 sm:grid-cols-3 gap-8 sm:gap-6">
        {steps.map((s, i) => (
          <div key={s.title} className="relative pl-1">
            <span className="font-display text-5xl text-teal/15 leading-none select-none">{String(i + 1).padStart(2, '0')}</span>
            <div className="mt-2 mb-3 flex h-9 w-9 items-center justify-center rounded-full bg-teal/10 text-teal">
              <s.icon size={18} strokeWidth={1.75} />
            </div>
            <p className="font-medium text-ink mb-1">{s.title}</p>
            <p className="text-sm text-ink/50 leading-relaxed max-w-[26ch]">{s.desc}</p>
          </div>
        ))}
      </div>
    </section>
  );
}

function TrustBand() {
  const ref = useScrollReveal<HTMLDivElement>('.reveal');
  return (
    <section className="pb-16 sm:pb-20">
      {/* Photo slot: place clinic-trust.jpg at patient-portal/public/images/. Airy, welcoming
          waiting-room composition with negative space in the upper third for this overlay
          card. Falls back to a warm gradient so the band still reads finished either way. */}
      <div
        ref={ref}
        className="relative overflow-hidden rounded-card min-h-[280px] sm:min-h-[340px] flex items-center bg-mist"
        style={{ backgroundImage: "url('/images/clinic-trust.jpg'), linear-gradient(135deg, #DCEAE5 0%, #EDF3F0 100%)", backgroundSize: 'cover', backgroundPosition: 'center' }}
      >
        {/* Flat wash + directional gradient, same reasoning as the hero scrim: a directional
            gradient alone left the tail of this text column sitting over the photo's bright
            furniture with too little contrast. */}
        <div aria-hidden className="absolute inset-0 bg-ink/30" />
        <div aria-hidden className="absolute inset-0" style={{ background: 'linear-gradient(90deg, rgba(23,38,43,0.6) 0%, rgba(23,38,43,0.3) 70%, transparent 100%)' }} />
        <div className="reveal relative p-6 sm:p-10 max-w-sm">
          <MessageCircleHeart size={22} className="text-white/80 mb-4" />
          <p className="font-display text-2xl text-white leading-snug mb-3 text-balance">
            "Booked a same-week appointment without a single phone call."
          </p>
          <p className="text-xs text-white/85">Every booking — online or at the front desk — runs through the same system your clinic already trusts.</p>
        </div>
      </div>
    </section>
  );
}

function WhyUs() {
  const ref = useScrollReveal<HTMLDivElement>();
  return (
    <section className="pb-16 sm:pb-20">
      <p className="font-mono text-xs uppercase tracking-widest text-teal mb-3">Why patients use this</p>
      <h2 className="font-display text-3xl text-ink mb-10 max-w-md text-balance">Built around your time, not the clinic's.</h2>
      <div ref={ref} className="grid grid-cols-1 sm:grid-cols-3 gap-4 sm:auto-rows-[1fr]">
        <Feature icon={Search} title="Search & compare" desc="Browse doctors by specialty and see real open times." className="sm:col-span-2" />
        <Feature icon={CalendarCheck2} title="Book in minutes" desc="Hold your slot, confirm details, done." />
        <Feature icon={ShieldCheck} title="Your data, protected" desc="Consent-based, privacy-first record keeping." />
      </div>
    </section>
  );
}

function ClosingCta() {
  const ref = useScrollReveal<HTMLDivElement>();
  return (
    <section className="bg-ink py-16 sm:py-20">
      <div ref={ref} className="max-w-6xl mx-auto px-5 sm:px-6 text-center">
        <h2 className="font-display text-3xl sm:text-4xl text-white mb-4 text-balance">Ready when you are.</h2>
        <p className="text-white/60 max-w-md mx-auto mb-8">Search the directory now, or create your patient record first if you already know who you'd like to see.</p>
        <div className="flex flex-wrap items-center justify-center gap-3">
          <Link to="/doctors" className="btn-press flex items-center gap-2 rounded-full bg-teal px-6 py-3 text-white text-sm font-medium hover:bg-teal-dark focus-ring shadow-sm shadow-teal/20">
            Find a doctor <ArrowRight size={16} />
          </Link>
          <Link to="/register" className="btn-press rounded-full border border-white/30 px-6 py-3 text-white text-sm font-medium hover:border-white focus-ring">
            Create your record
          </Link>
        </div>
      </div>
    </section>
  );
}

function Feature({ icon: Icon, title, desc, className = '' }: { icon: typeof Search; title: string; desc: string; className?: string }) {
  return (
    <div className={`card-interactive rounded-card border border-line bg-paper p-5 ${className}`}>
      <div className="mb-3 flex h-9 w-9 items-center justify-center rounded-full bg-teal/10 text-teal">
        <Icon size={18} strokeWidth={1.75} />
      </div>
      <p className="font-medium text-ink text-sm mb-1">{title}</p>
      <p className="text-xs text-ink/50 leading-relaxed">{desc}</p>
    </div>
  );
}
