import { FileText } from 'lucide-react';

export default function Terms() {
  return (
    <div className="animate-page max-w-2xl">
      <div className="flex items-center gap-2 mb-1">
        <FileText size={20} className="text-teal" />
        <h1 className="font-display text-2xl text-ink">Terms of Service</h1>
      </div>
      <p className="text-xs text-ink/40 mb-8">Last updated: placeholder — review before real-world use</p>

      <div className="rounded-card border border-line bg-mist/60 px-4 py-3 mb-8">
        <p className="text-xs text-ink/60 leading-relaxed">
          This is template legal content written for a demo project, not reviewed by counsel. Replace it
          with terms drafted or approved by a qualified lawyer before using this in production.
        </p>
      </div>

      <div className="space-y-8 text-sm text-ink/70 leading-relaxed">
        <Section title="1. Acceptance of these terms">
          By creating an account or booking an appointment through Schedul.io, you agree to these Terms
          of Service and to the <a href="/privacy" className="text-teal hover:text-teal-dark underline underline-offset-2">Privacy Policy</a>.
          If you don't agree, please don't use the service.
        </Section>

        <Section title="2. What the service is">
          Schedul.io lets you search for doctors, book and manage appointments at a participating
          clinic, and receive related notifications. It is a scheduling tool — it does not provide
          medical advice, diagnosis, or treatment, and using it does not create a doctor-patient
          relationship on its own.
        </Section>

        <Section title="3. Your account">
          You're responsible for the accuracy of the information you provide and for keeping your
          login credentials confidential. Let your clinic know promptly if you believe your account
          has been accessed without your permission.
        </Section>

        <Section title="4. Appointments, cancellations, and no-shows">
          Booking a slot holds it temporarily while you complete your request; a clinic may confirm,
          decline, or ask you to reschedule an appointment per its own policies. Please cancel or
          reschedule as early as you can — repeated no-shows may affect your ability to book online.
        </Section>

        <Section title="5. Acceptable use">
          Don't use the service to book appointments on behalf of someone else without authorization,
          attempt to access another patient's records, or interfere with the platform's normal
          operation.
        </Section>

        <Section title="6. Health information">
          Any health-related information you provide is handled as described in our
          {' '}<a href="/privacy" className="text-teal hover:text-teal-dark underline underline-offset-2">Privacy Policy</a>. It's shared with the clinic and practitioners involved in
          your care, not sold to third parties.
        </Section>

        <Section title="7. Disclaimers">
          The service is provided "as is." We don't guarantee it will be uninterrupted or error-free,
          and we're not liable for decisions made by clinics or practitioners using the platform.
        </Section>

        <Section title="8. Changes to these terms">
          We may update these terms from time to time. If we make material changes, we'll let you
          know before they take effect.
        </Section>

        <Section title="9. Contact">
          Questions about these terms can be directed to your clinic's front desk or to the address
          listed in your clinic's own privacy notice.
        </Section>
      </div>
    </div>
  );
}

function Section({ title, children }: { title: string; children: React.ReactNode }) {
  return (
    <div>
      <h2 className="font-medium text-ink mb-2">{title}</h2>
      <p>{children}</p>
    </div>
  );
}
