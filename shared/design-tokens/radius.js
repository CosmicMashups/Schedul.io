/**
 * Shared card/panel radius for all three portals. Previously 14px (patient) / 10px (staff) /
 * 14px (doctor) — staff's 10px was an unexplained outlier. 12px is a deliberate compromise:
 * softer than staff's old value so its panels don't feel disconnected from the other two apps,
 * tighter than patient/doctor's old value so staff's denser tables/grids don't feel oversoft.
 */

export const radius = {
  card: '12px',
}
