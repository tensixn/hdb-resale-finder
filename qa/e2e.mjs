// Browser walkthrough of every use case at desktop and phone width.
// Run: npm i -D playwright && npx playwright install chromium && BASE=http://localhost:5173 node qa/e2e.mjs
import fs from 'node:fs';
import { chromium } from 'playwright';
const BASE = process.env.BASE || 'http://127.0.0.1:5173';
const OUT = './qa-shots';
fs.mkdirSync(OUT, { recursive: true });
const browser = await chromium.launch(process.env.CHROMIUM ? { executablePath: process.env.CHROMIUM } : {});
const ctx = await browser.newContext({ viewport: { width: 1440, height: 900 }, deviceScaleFactor: 1 });
const page = await ctx.newPage();
const errors = [];
let fails = 0;
const watch = (p, tag) => {
  p.on('console', (m) => { if (m.type() === 'error' && !/onemap|Failed to load resource|fonts\.g/i.test(m.text())) errors.push(tag + m.text()); });
  p.on('pageerror', (e) => errors.push(tag + 'PAGEERROR ' + e.message));
};
watch(page, '');
const step = async (p, name, fn) => { try { await fn(); console.log('ok   ', name); } catch (e) { fails++; console.log('FAIL ', name, e.message.split('\n')[0]); await p.screenshot({ path: `${OUT}/fail-${name.replace(/\W+/g, '_')}.png` }); } };
const noHScroll = async (p, label) => {
  const sw = await p.evaluate(() => document.documentElement.scrollWidth - document.documentElement.clientWidth);
  if (sw > 0) throw new Error(`${label}: horizontal scroll ${sw}px`);
};

await step(page, 'home', async () => {
  await page.goto(BASE + '/');
  await page.getByText('Find a resale flat').waitFor();
  await page.screenshot({ path: `${OUT}/01-search.png`, fullPage: true });
});
await step(page, 'validation W1', async () => {
  await page.getByRole('button', { name: 'Search flats' }).click();
  await page.getByText('Enter Workplace 1 to search.').waitFor();
});
await step(page, 'search', async () => {
  await page.fill('#workplace1', 'Raffles Place');
  await page.keyboard.press('Escape');
  await page.getByText('Add a second workplace').click();
  await page.fill('#workplace2', 'Jurong East');
  await page.keyboard.press('Escape');
  await page.getByRole('button', { name: 'Drive' }).click();
  await page.fill('#maxPrice', '700000');
  await page.getByRole('button', { name: '4-room' }).click();
  await page.getByRole('button', { name: '5-room' }).click();
  await page.screenshot({ path: `${OUT}/01b-search-filled.png`, fullPage: true });
  await page.getByRole('button', { name: 'Search flats' }).click();
  await page.waitForURL('**/results');
  await page.locator('article').first().waitFor();
  await page.waitForTimeout(1500);
  const vs = await page.locator('article').first().locator('p.border-l-2').textContent();
  console.log('      #1 explanation:', vs);
  await page.screenshot({ path: `${OUT}/02-results.png` });
});
await step(page, 'list<->map sync', async () => {
  await page.locator('article').nth(1).hover();
  await page.waitForTimeout(300);
  if ((await page.locator('.rank-marker.is-active').count()) !== 1) throw new Error('hovering card did not highlight a rank marker');
  await page.locator('.maplibregl-popup').waitFor();
  await page.screenshot({ path: `${OUT}/02b-hover-sync.png` });
  // Map -> list: click the #3 marker, its card expands.
  await page.mouse.move(1300, 850);
  const m3 = page.locator('.rank-marker', { hasText: /^3$/ });
  await m3.click({ force: true });
  await page.waitForTimeout(500);
  const expanded = page.locator('article[aria-expanded=true]');
  await expanded.waitFor();
  const lbl = await expanded.getAttribute('aria-label');
  if (!/^Rank 3:/.test(lbl)) throw new Error('map click selected wrong card: ' + lbl);
  await expanded.getByText('Match score').waitFor();
  await page.screenshot({ path: `${OUT}/02c-map-select.png` });
  await expanded.getByRole('button', { name: 'Hide score breakdown' }).click();
});
await step(page, 'reweight', async () => {
  await page.getByRole('button', { name: 'Priorities' }).click();
  const before = await page.locator('article h3').first().textContent();
  const slider = page.locator('#w-lease');
  const box = await slider.boundingBox();
  await page.mouse.click(box.x + box.width * 0.85, box.y + box.height / 2);
  await page.getByText('Re-ranked for your new priorities').waitFor();
  await page.waitForTimeout(800);
  const lease = await page.locator('label[for=w-lease]').locator('..').locator('span.tabular-nums').textContent();
  const after = await page.locator('article h3').first().textContent();
  const moved = await page.locator('article').getByText(/^(Up|Down) \d+:/).count();
  console.log('      lease weight now', lease, '| top before:', before, '| after:', after, '| cards with move reason:', moved);
  if (!moved) throw new Error('no move reasons shown');
  await page.getByRole('button', { name: 'Priorities' }).click();
  await page.screenshot({ path: `${OUT}/03-reweighted.png` });
});
await step(page, 'preset', async () => {
  await page.getByRole('button', { name: /^Priorities/ }).click();
  await page.getByRole('button', { name: 'Safe & sound', exact: true }).click();
  await page.waitForFunction(() => document.querySelector('#w-safety')?.value === '55');
  await page.waitForTimeout(800);
  const pressed = await page.getByRole('button', { name: 'Safe & sound', exact: true }).getAttribute('aria-pressed');
  if (pressed !== 'true') throw new Error('preset not marked active');
  const label = await page.getByRole('button', { name: /^Priorities/ }).textContent();
  if (!/Safe & sound/.test(label)) throw new Error('toolbar does not name the preset: ' + label);
  await page.screenshot({ path: `${OUT}/03b-preset.png` });
  await page.getByRole('button', { name: 'Balanced', exact: true }).click();
  await page.waitForFunction(() => document.querySelector('#w-safety')?.value === '25');
  await page.getByRole('button', { name: /^Priorities/ }).click();
  await page.waitForTimeout(500);
});
await step(page, 'short version', async () => {
  const sv = page.getByRole('region', { name: 'The short version' });
  await sv.waitFor();
  await sv.getByRole('button', { name: /Lowest price/ }).click();
  await page.waitForTimeout(600);
  const sel = await page.getByRole('button', { name: 'Hide score breakdown' }).count();
  console.log('      selected cards after short-version pick:', sel); if (sel !== 1) throw new Error('pick did not select a card');
});
await step(page, 'filter', async () => {
  await page.getByRole('button', { name: /Filters/ }).click();
  await page.screenshot({ path: `${OUT}/04-filters-open.png` });
  await page.fill('#f-max', '500000');
  await page.fill('#f-lease', '85');
  await page.getByRole('button', { name: 'Apply filters' }).click();
  await page.waitForTimeout(800);
  const h = await page.locator('h1').first().textContent();
  console.log('      after filter:', h);
  const leases = await page.locator('article p.flat-meta').allTextContents();
  if (leases.some((t) => { const m = t.match(/(\d+) yrs left/); return m && Number(m[1]) < 85; })) throw new Error('lease filter not applied');
  await page.getByRole('button', { name: /Filters/ }).click();
  await page.getByRole('button', { name: 'Clear all' }).click();
  await page.waitForTimeout(600);
});
await step(page, 'keyboard', async () => {
  await page.locator('article').first().focus();
  await page.keyboard.press('Enter');
  await page.locator('article').first().getByText('Match score').waitFor();
  const ring = await page.locator('article').first().evaluate((el) => getComputedStyle(el).boxShadow);
  if (!ring || ring === 'none') throw new Error('no visible focus');
  await page.keyboard.press('Enter');
});
let flatUrl;
await step(page, 'detail', async () => {
  await page.locator('article').first().getByRole('button', { name: /Details/ }).click();
  await page.waitForURL('**/flats/*');
  await page.getByText('Our verdict').waitFor();
  await page.getByText('Fair-value pricing').waitFor();
  flatUrl = page.url();
  await page.waitForTimeout(500);
  await page.screenshot({ path: `${OUT}/05-detail.png`, fullPage: true });
});
await step(page, 'cpf', async () => {
  await page.getByText('CPF grants & affordability').click();
  await page.getByRole('button', { name: 'Estimate my grants' }).click();
  await page.getByText('Enter your monthly household income.').waitFor();
  await page.fill('#income', '6500');
  await page.getByRole('button', { name: 'Married' }).click();
  await page.fill('#ppc', '650123');
  await page.getByRole('button', { name: 'Estimate my grants' }).click();
  await page.getByText('Monthly instalment').first().waitFor();
  await page.getByText('Monthly instalment').first().scrollIntoViewIfNeeded();
  await page.screenshot({ path: `${OUT}/06-cpf.png` });
});
await step(page, 'guest bookmark -> signup -> back', async () => {
  await page.getByRole('button', { name: 'Bookmark', exact: true }).click();
  await page.waitForURL('**/login**');
  await page.getByText('Create an account').click();
  await page.fill('#email', `roen${Date.now()}@test.sg`);
  await page.fill('#password', 'abc');
  await page.fill('#confirm', 'abc');
  await page.getByRole('button', { name: 'Create account' }).click();
  await page.getByText('Use at least 8 characters.').waitFor();
  await page.fill('#password', 'homeMatch2026');
  await page.fill('#confirm', 'homeMatch2026');
  await page.getByRole('button', { name: 'Create account' }).click();
  await page.getByText('Please accept the personal data notice').waitFor();
  await page.locator('input[type=checkbox]').check();
  await page.getByRole('button', { name: 'Create account' }).click();
  await page.waitForURL('**/flats/*');
  await page.getByRole('button', { name: 'Bookmarked' }).waitFor();
});
await step(page, 'bookmark more + compare', async () => {
  await page.goto(BASE + '/results');
  await page.locator('article').first().waitFor();
  for (const i of [1, 2]) await page.locator('article').nth(i).getByRole('button', { name: 'Bookmark this flat' }).click();
  await page.waitForTimeout(600);
  await page.getByRole('link', { name: 'Bookmarks' }).first().click();
  await page.waitForURL('**/bookmarks');
  await page.locator('input[type=checkbox]').first().waitFor();
  const boxes = page.locator('input[type=checkbox]');
  const n = await boxes.count();
  for (let i = 0; i < n; i++) await boxes.nth(i).check();
  await page.getByRole('button', { name: /Compare/ }).click();
  await page.waitForURL('**/compare');
  await page.getByText('Compare flats').waitFor();
  await page.screenshot({ path: `${OUT}/07-compare.png` });
});
await step(page, 'profile', async () => {
  await page.getByRole('link', { name: 'Profile' }).first().click();
  await page.getByText('Financial profile').first().waitFor();
  await page.getByText('Last search').waitFor();
});
await step(page, 'help', async () => {
  await page.goto(BASE + '/help');
  await page.getByText('Data status').waitFor();
  await page.getByRole('button', { name: 'Refresh now' }).click();
  await page.getByText(/Refreshed:/).waitFor();
});
await step(page, 'sign out', async () => {
  await page.goto(BASE + '/profile');
  await page.getByRole('button', { name: 'Sign out' }).click();
  await page.waitForURL(BASE + '/');
  await page.getByRole('link', { name: 'Sign in' }).waitFor();
});

// Phone layout.
const m = await browser.newContext({ viewport: { width: 390, height: 844 }, deviceScaleFactor: 2, isMobile: true, hasTouch: true });
const mp = await m.newPage();
watch(mp, 'MOBILE ');
const smallTargets = async (label) => {
  const bad = await mp.evaluate(() => {
    const out = [];
    for (const el of document.querySelectorAll('button, a, input, select, [role=button]')) {
      if (el.closest('.maplibregl-ctrl, .maplibregl-popup')) continue;
      const r = el.getBoundingClientRect();
      if (!r.width || !r.height || r.bottom < 0 || r.top > innerHeight) continue;
      const st = getComputedStyle(el);
      if (st.visibility === 'hidden' || (el.type === 'checkbox')) continue;
      if (r.width < 44 || r.height < 44) {
        // inline text links inside paragraphs are exempt
        if (el.tagName === 'A' && el.closest('p, h3')) continue;
        out.push(`${el.tagName} "${(el.getAttribute('aria-label') || el.textContent || el.id).trim().slice(0, 30)}" ${Math.round(r.width)}x${Math.round(r.height)}`);
      }
    }
    return out;
  });
  if (bad.length) console.log(`      small targets on ${label}:`, bad.slice(0, 12));
};
await step(mp, 'mobile search', async () => {
  await mp.goto(BASE + '/');
  await mp.screenshot({ path: `${OUT}/08-phone-search.png`, fullPage: true });
  await smallTargets('search');
  await noHScroll(mp, 'search');
  await mp.fill('#workplace1', 'one-north');
  await mp.keyboard.press('Escape');
  await mp.getByRole('button', { name: 'Search flats' }).click();
  await mp.waitForURL('**/results');
  await mp.locator('article').first().waitFor();
  await mp.waitForTimeout(800);
  await mp.screenshot({ path: `${OUT}/09-phone-results.png` });
  await smallTargets('results');
  await noHScroll(mp, 'results');
});
await step(mp, 'mobile map', async () => {
  await mp.getByRole('button', { name: 'Map' }).click();
  await mp.waitForTimeout(1200);
  await mp.getByRole('button', { name: 'Next rank' }).click();
  await mp.waitForTimeout(600);
  await mp.screenshot({ path: `${OUT}/10-phone-map.png` });
  await smallTargets('map');
  await noHScroll(mp, 'map');
});
await step(mp, 'mobile detail', async () => {
  await mp.getByRole('button', { name: /Details/ }).click();
  await mp.waitForURL('**/flats/*');
  await mp.getByText('Our verdict').waitFor();
  await mp.waitForTimeout(500);
  await mp.screenshot({ path: `${OUT}/11-phone-detail.png`, fullPage: true });
  await smallTargets('detail');
  await noHScroll(mp, 'detail');
});
await step(mp, 'mobile reweight', async () => {
  await mp.goto(BASE + '/results');
  await mp.locator('article').first().waitFor();
  await mp.getByRole('button', { name: 'Priorities' }).click();
  const slider = mp.locator('#w-safety');
  const box = await slider.boundingBox();
  await mp.touchscreen.tap(box.x + box.width * 0.9, box.y + box.height / 2);
  await mp.locator('#w-safety').press('ArrowRight');
  await mp.getByText('Re-ranked for your new priorities').waitFor();
  await mp.getByRole('button', { name: 'Close panel' }).click();
  await mp.waitForTimeout(500);
  await mp.screenshot({ path: `${OUT}/12-phone-reweighted.png` });
  await noHScroll(mp, 'reweighted');
});
console.log('console errors:', errors.length ? errors : 'none');
console.log(fails ? `${fails} FAILED` : 'ALL PASSED');
await browser.close();
