import { describe, expect, it } from 'vitest';
import { flatTypeLabel, moneyShort, scoreColor, titleCase } from './format';

describe('format helpers', () => {
  it('shortens prices for cards and pins', () => {
    expect(moneyShort(612_400)).toBe('S$612k');
    expect(moneyShort(1_050_000)).toBe('S$1.05M');
    expect(moneyShort(1_000_000)).toBe('S$1M');
    expect(moneyShort(null)).toBe('–');
  });

  it('turns HDB data.gov.sg casing into readable labels', () => {
    expect(titleCase('KALLANG/WHAMPOA')).toBe('Kallang/Whampoa');
    expect(flatTypeLabel('4 ROOM')).toBe('4-room');
  });

  it('colours scores from red to green and clamps out-of-range values', () => {
    expect(scoreColor(0)).toBe('rgb(225,85,60)');
    expect(scoreColor(100)).toBe('rgb(20,120,100)');
    expect(scoreColor(150)).toBe(scoreColor(100));
  });
});
