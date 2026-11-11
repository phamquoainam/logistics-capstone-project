/** Integer-first chart geometry. Exact source values are always retained for labels. */
export function chartAxis(values: string[]) {
  const maximum = values.reduce((max, value) => BigInt(value) > max ? BigInt(value) : max, 0n);
  const target = (maximum + 3n) / 4n;
  let power = 1n;
  while (power * 10n < target) power *= 10n;
  const step = [1n, 2n, 5n, 10n].map(n => n * power).find(n => n >= target) ?? power * 10n;
  return { maximum: step * 4n, ticks: [4n, 3n, 2n, 1n, 0n].map(n => n * step) };
}

export function chartPercent(value: string | bigint, maximum: bigint) {
  return maximum === 0n ? 0 : Number(BigInt(value) * 1000000n / maximum) / 10000;
}

export function axisLabel(value: bigint) {
  if (value < 1000n) return value.toLocaleString('vi-VN');
  const units: [bigint, string][] = [[1000000000n, 'tỷ'], [1000000n, 'tr'], [1000n, 'k']];
  if (value >= 1000000000000n) {
    const digits = value.toString(), fraction = digits[1] === '0' ? '' : ',' + digits[1];
    return digits[0] + fraction + ' ×10^' + (digits.length - 1);
  }
  const [divisor, suffix] = units.find(([n]) => value >= n)!;
  const tenths = value * 10n / divisor;
  return (tenths / 10n).toLocaleString('vi-VN') + (tenths % 10n ? ',' + tenths % 10n : '') + ' ' + suffix;
}

export function chartGeometry(values: string[], maximum: bigint) {
  const points = values.map((value, index) => ({ x: (index + .5) * 1000 / values.length, y: 180 - chartPercent(value, maximum) * 1.8 }));
  const line = points.map((p, index) => `${index ? 'L' : 'M'}${p.x},${p.y}`).join(' ');
  const area = points.length > 1 ? `${line} L${points.at(-1)!.x},180 L${points[0].x},180 Z` : '';
  return { points, line, area };
}
