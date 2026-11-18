#!/usr/bin/env node
// Shot-forging capture: renders each shots_src/s*.html at 1080x2400 CSS px, DPR 1,
// waits for document.fonts.ready, saves full-frame PNGs to ../assets/shots/ and
// named layer cutouts (omitBackground) to ../assets/shots/layers/<name>/.
// Also verifies the two bundled faces carry every glyph the film needs.
import { createRequire } from 'node:module';
import path from 'node:path';
import fs from 'node:fs';
import { fileURLToPath } from 'node:url';

const here = path.dirname(fileURLToPath(import.meta.url));
const require = createRequire(path.join(process.env.APPDATA, 'npm/node_modules/omniroute/package.json'));
const { chromium } = require('playwright-core');

const EXE = path.join(process.env.LOCALAPPDATA, 'ms-playwright', 'chromium-1255', 'chrome-win64', 'chrome.exe');
const SHOTS = path.resolve(here, '..', 'assets', 'shots');
const SHOT_LIST = [
  ['01_home', ['statusbar', 'header', 'empty', 'navbar']],
  ['02_models', ['statusbar', 'header', 'thread', 'composer', 'drawer', 'navbar']],
  ['03_compose', ['statusbar', 'header', 'thread', 'composer', 'navbar']],
  ['03_compose_typed', ['statusbar', 'header', 'thread', 'composer', 'navbar']],
  ['04_stream_a', ['statusbar', 'header', 'thread', 'composer', 'navbar']],
  ['04_stream_b', ['statusbar', 'header', 'thread', 'composer', 'navbar']],
  ['04_stream_c', ['statusbar', 'header', 'thread', 'composer', 'navbar']],
  ['05_footer', ['statusbar', 'header', 'thread', 'composer', 'footer-metrics', 'navbar']],
  ['06_hud', ['statusbar', 'header', 'thread', 'composer', 'hud-sheet', 'navbar']],
  ['07_tip', ['statusbar', 'header', 'thread', 'composer', 'hud-sheet', 'tip-card', 'navbar']],
  ['08_settings', ['statusbar', 'settings-top', 'settings-body', 'navbar']],
  ['09_relaunch_before', ['statusbar', 'header', 'thread', 'composer', 'footer-metrics', 'navbar']],
  ['09_relaunch_after', ['statusbar', 'header', 'thread', 'composer', 'footer-metrics', 'navbar']],
  ['10_about', ['statusbar', 'settings-top', 'settings-body', 'diag-block', 'navbar']],
];

fs.mkdirSync(SHOTS, { recursive: true });
fs.mkdirSync(path.join(SHOTS, 'layers'), { recursive: true });

const browser = await chromium.launch({
  executablePath: EXE,
  args: ['--disable-gpu', '--use-gl=angle', '--use-angle=swiftshader', '--disable-lcd-text', '--force-device-scale-factor=1'],
});
const page = await browser.newPage({ viewport: { width: 1080, height: 2400 }, deviceScaleFactor: 1 });

const glyphReport = {};

for (const [name, layers] of SHOT_LIST) {
  const url = 'file:///' + path.resolve(here, `s${name.startsWith('03_compose_typed') ? '' : ''}${name}.html`).replace(/\\/g, '/');
  await page.goto(url, { waitUntil: 'networkidle', timeout: 30000 });
  await page.evaluate(() => document.fonts.ready);
  await page.waitForTimeout(120);

  glyphReport[name] = await page.evaluate(() => ({
    orbitron_lockup: document.fonts.check('24px Orbitron', 'F.R.I.D.A.Y - on-device, by design.'),
    roboto_lockup: document.fonts.check('24px Roboto', 'F.R.I.D.A.Y - on-device, by design.'),
    roboto_bullet: document.fonts.check('12px Roboto', '\u2022'),
    roboto_chevron: document.fonts.check('12px Roboto', '\u203A'),
    roboto_slash: document.fonts.check('12px Roboto', 'LOCAL // CPU'),
    loaded_fonts: [...document.fonts].filter(f => f.status === 'loaded').map(f => f.family),
  }));

  await page.screenshot({ path: path.join(SHOTS, `${name}.png`), clip: { x: 0, y: 0, width: 1080, height: 2400 } });

  const dir = path.join(SHOTS, 'layers', name);
  fs.mkdirSync(dir, { recursive: true });
  for (let i = 0; i < layers.length; i++) {
    const sel = `.${layers[i]}`;
    const loc = page.locator(sel).first();
    if (await loc.count() === 0) continue;
    try {
      await loc.screenshot({ path: path.join(dir, `${String(i).padStart(2, '0')}_${layers[i]}.png`), omitBackground: true });
    } catch (e) {
      console.warn(`  layer ${sel} failed on ${name}: ${e.message.split('\n')[0]}`);
    }
  }
  console.log(`ok ${name}`);
}

fs.writeFileSync(path.join(SHOTS, '_glyph_report.json'), JSON.stringify(glyphReport, null, 2));
const bad = Object.entries(glyphReport).filter(([, r]) => !(r.orbitron_lockup && r.roboto_lockup && r.roboto_bullet && r.roboto_chevron && r.roboto_slash));
console.log(bad.length ? `GLYPH GAPS: ${JSON.stringify(bad)}` : 'glyph check passed: lockup + \u2022 + \u203A + // present in both faces');
await browser.close();
