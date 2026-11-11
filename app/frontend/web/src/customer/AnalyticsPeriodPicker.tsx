import { useEffect, useId, useRef, useState } from 'react';
import type { FormEvent } from 'react';

export const shiftDate = (value: string, days: number) => new Date(Date.parse(value) + days * 86400000).toISOString().slice(0, 10);
const monthShift = (value: string, offset: number) => { const date = new Date(value); date.setUTCDate(1); date.setUTCMonth(date.getUTCMonth() + offset); return date.toISOString().slice(0, 10); };
const display = (value: string) => /^\d{4}-\d{2}-\d{2}$/.test(value) ? `${value.slice(8)}/${value.slice(5, 7)}/${value.slice(0, 4)}` : '—';
export function parsePeriodInput(value: string) {
  const match = /^(\d{2})\/(\d{2})\/(\d{4})$/.exec(value), iso = match ? `${match[3]}-${match[2]}-${match[1]}` : '';
  return iso && Number.isFinite(Date.parse(iso)) && shiftDate(iso, 0) === iso ? iso : '';
}
export function periodPresets(today: string) {
  const tomorrow = shiftDate(today, 1), startMonth = today.slice(0, 7) + '-01';
  return [
    { label: 'Hôm nay', from: today, until: tomorrow },
    { label: 'Hôm qua', from: shiftDate(today, -1), until: today },
    ...[7, 14, 30, 90].map(days => ({ label: `${days} ngày`, from: shiftDate(tomorrow, -days), until: tomorrow })),
    { label: 'Tháng này', from: startMonth, until: tomorrow },
    { label: 'Tháng trước', from: monthShift(startMonth, -1), until: startMonth },
  ];
}
export function monthDates(month: string) {
  const end = monthShift(month, 1), days: string[] = [];
  for (let day = month; day < end; day = shiftDate(day, 1)) days.push(day);
  return { offset: (new Date(month).getUTCDay() + 6) % 7, days };
}
export function AnalyticsPeriodPicker({ from, until, apply }: { from: string; until: string; apply: (from: string, until: string) => void }) {
  const today = new Date(Date.now() + 7 * 3600000).toISOString().slice(0, 10), validUntil = /^\d{4}-\d{2}-\d{2}$/.test(until) && Number.isFinite(Date.parse(until)) ? shiftDate(until, -1) : today;
  const [open, setOpen] = useState(false), [start, setStart] = useState(display(from)), [end, setEnd] = useState(display(validUntil)), [choosingEnd, setChoosingEnd] = useState(false), [month, setMonth] = useState(monthShift(validUntil, -1)), [error, setError] = useState('');
  const [position, setPosition] = useState({ left: 16, top: 80 });
  const root = useRef<HTMLDivElement>(null), trigger = useRef<HTMLButtonElement>(null), id = useId();
  const parsedStart = parsePeriodInput(start), parsedEnd = parsePeriodInput(end), presets = periodPresets(today);
  useEffect(() => {
    if (!open) return;
    const outside = (event: PointerEvent) => { if (!root.current?.contains(event.target as Node)) setOpen(false); };
    const resize = () => setOpen(false);
    document.addEventListener('pointerdown', outside); window.addEventListener('resize', resize);
    return () => { document.removeEventListener('pointerdown', outside); window.removeEventListener('resize', resize); };
  }, [open]);
  function close() { setOpen(false); trigger.current?.focus(); }
  function toggle() {
    if (open) { close(); return; }
    setStart(display(from)); setEnd(display(validUntil)); setChoosingEnd(false); setMonth(monthShift(validUntil, -1)); setError('');
    const rect = trigger.current!.getBoundingClientRect(), width = Math.min(680, window.innerWidth - 24);
    setPosition({ left: Math.max(12, Math.min(rect.right - width, window.innerWidth - width - 12)), top: Math.min(rect.bottom + 8, Math.max(12, window.innerHeight - 460)) });
    setOpen(true);
  }
  function choose(day: string) {
    setError('');
    if (!choosingEnd || !parsedStart || day < parsedStart) { setStart(display(day)); setEnd(display(day)); setChoosingEnd(true); }
    else { setEnd(display(day)); setChoosingEnd(false); }
  }
  function confirm(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!parsedStart || !parsedEnd || parsedStart > parsedEnd) { setError('Nhập khoảng ngày hợp lệ theo dd/MM/yyyy.'); return; }
    if (parsedEnd > today || (Date.parse(parsedEnd) - Date.parse(parsedStart)) / 86400000 >= 366) { setError('Chọn tối đa 366 ngày và không vượt quá hôm nay (giờ Việt Nam).'); return; }
    apply(parsedStart, shiftDate(parsedEnd, 1)); close();
  }
  return <div className="an-period-picker" ref={root} onKeyDown={event => { if (event.key === 'Escape' && open) { event.stopPropagation(); close(); } }} onBlur={event => { if (!event.currentTarget.contains(event.relatedTarget)) setOpen(false); }}>
    <button type="button" ref={trigger} className="an-period-trigger" aria-label="Khoảng thời gian" aria-haspopup="dialog" aria-expanded={open} aria-controls={id} onClick={toggle}><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.6" aria-hidden="true"><path d="M4 5h16v16H4ZM8 2v6M16 2v6M4 10h16" /></svg><span>{display(from)} – {display(validUntil)}</span><span aria-hidden="true">⌄</span></button>
    {open && <section id={id} className="an-period-popover" role="dialog" aria-label="Chọn khoảng thời gian" style={position}>
      <div className="an-calendar-body"><div className="an-calendar-presets" aria-label="Khoảng thời gian nhanh">{presets.map(preset => <button type="button" key={preset.label} aria-pressed={from === preset.from && until === preset.until} onClick={() => { apply(preset.from, preset.until); close(); }}>{preset.label}</button>)}</div>
        <div className="an-calendar-months"><div className="an-calendar-navigation"><button type="button" aria-label="Lịch lùi một tháng" onClick={() => setMonth(monthShift(month, -1))}>‹</button><span>Chọn ngày bắt đầu và ngày kết thúc</span><button type="button" aria-label="Lịch tiến một tháng" disabled={month >= monthShift(today, -1)} onClick={() => setMonth(monthShift(month, 1))}>›</button></div><div className="an-calendar-pair">{[month, monthShift(month, 1)].map(value => {
          const dates = monthDates(value);
          return <section className="an-calendar-month" key={value}><h3>{new Date(value).toLocaleDateString('vi-VN', { month: 'long', year: 'numeric', timeZone: 'UTC' })}</h3><div className="an-calendar-weekdays" aria-hidden="true">{['T2', 'T3', 'T4', 'T5', 'T6', 'T7', 'CN'].map(label => <span key={label}>{label}</span>)}</div><div className="an-calendar-grid">{Array.from({ length: dates.offset }, (_, index) => <span key={'blank-' + index} />)}{dates.days.map(day => <button key={day} type="button" data-date={day} aria-label={display(day)} aria-pressed={day === parsedStart || day === parsedEnd} disabled={day > today} className={(day === parsedStart || day === parsedEnd ? 'is-edge ' : '') + (day >= parsedStart && day <= parsedEnd ? 'is-range ' : '') + (day === today ? 'is-today' : '')} onClick={() => choose(day)} onKeyDown={event => {
            const delta = ({ ArrowLeft: -1, ArrowRight: 1, ArrowUp: -7, ArrowDown: 7 } as Record<string, number>)[event.key];
            if (delta) { event.preventDefault(); const next = shiftDate(day, delta); root.current?.querySelector<HTMLButtonElement>(`[data-date="${next}"]:not(:disabled)`)?.focus(); }
          }}>{Number(day.slice(8))}</button>)}</div></section>;
        })}</div></div>
      </div>
      <form className="an-calendar-footer" onSubmit={confirm}><label>Từ ngày<input inputMode="numeric" maxLength={10} placeholder="dd/MM/yyyy" required value={start} aria-invalid={!!error} onChange={event => { setStart(event.target.value); setError(''); }} /></label><label>Đến ngày<input inputMode="numeric" maxLength={10} placeholder="dd/MM/yyyy" required value={end} aria-invalid={!!error} onChange={event => { setEnd(event.target.value); setError(''); }} /></label><button type="button" className="pw-secondary" onClick={close}>Hủy</button><button className="pw-primary">Xác nhận khoảng ngày</button>{error && <p role="alert">{error}</p>}</form>
    </section>}
  </div>;
}
