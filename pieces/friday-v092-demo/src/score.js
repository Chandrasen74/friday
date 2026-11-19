  // ============================================================
  //  FRIDAY v0.9.2 score body - A minor, 120 BPM, offset 0. Bar = 2.0s.
  //  Shape: hook -> build -> near-silence 20-22s -> the loudest hit of the
  //  film at 22.0s -> HUD payoff -> resolve at 48.0s -> fade by ~51.6s.
  //  All times come from TIMELINE.cues so audio and picture stay locked.
  // ============================================================
  const q = TIMELINE.cues;
  const BASSV = 0.2;
  to = 'm';

  // ---- harmony bed (Am F C, one chord per bar or two) ------------------
  pad(0.0, 4.0, ['A2','E3','A3'], 0.020, { type:'triangle', cut:850, att:0.45, rel:0.55, send:0.25 });
  pad(4.0, 8.0, ['F2','C3','A3'], 0.020, { type:'triangle', cut:900, att:0.45, rel:0.55, send:0.25 });
  pad(8.0, 12.0, ['C3','G3','E4'], 0.020, { type:'triangle', cut:1000, att:0.4, rel:0.55, send:0.25 });
  pad(12.0, 18.0, ['A2','E3','C4'], 0.022, { type:'triangle', cut:1100, att:0.8, rel:0.7, send:0.3 });
  pad(18.0, 20.0, ['A2','E3','A3','B3'], 0.026, { type:'triangle', cut:1300, att:0.35, rel:0.25, send:0.3 });
  // near-silence 20.0-22.0: nothing at all
  pad(22.0, 26.0, ['C3','G3','E4'], 0.030, { type:'triangle', cut:1400, att:0.06, rel:0.5, send:0.3 });
  pad(26.0, 28.0, ['G2','D3','B3'], 0.024, { type:'triangle', cut:1300, att:0.2, rel:0.4, send:0.3 });
  pad(28.0, 32.0, ['F2','C3','A3'], 0.022, { type:'triangle', cut:1100, att:0.3, rel:0.5, send:0.3 });
  pad(32.0, 36.0, ['C3','G3','E4'], 0.022, { type:'triangle', cut:1100, att:0.3, rel:0.5, send:0.3 });
  pad(36.0, 38.0, ['A2','E3','A3'], 0.024, { type:'triangle', cut:1200, att:0.25, rel:0.35, send:0.3 });
  pad(38.0, 40.0, ['F2','C3','A3'], 0.024, { type:'triangle', cut:1200, att:0.25, rel:0.35, send:0.3 });
  pad(40.0, 42.0, ['C3','G3','E4'], 0.024, { type:'triangle', cut:1200, att:0.25, rel:0.35, send:0.3 });
  pad(42.0, 44.0, ['F2','C3','A3'], 0.026, { type:'triangle', cut:1300, att:0.25, rel:0.4, send:0.3 });
  pad(44.0, 48.0, ['G2','D3','B3'], 0.026, { type:'triangle', cut:1300, att:0.3, rel:0.4, send:0.3 });
  pad(48.0, 51.6, ['A2','E3','A3','C4'], 0.030, { type:'triangle', cut:1400, att:0.06, rel:1.1, send:0.35 });

  // ---- bass pulses on the bar grid ------------------------------------
  for (const t of [0, 2, 4, 6, 8, 10, 12, 14, 16, 18]) bass(t, nz(t % 4 === 0 ? 'A2' : t % 8 === 6 ? 'F2' : 'A2'), BASSV);
  bass(22.0, nz('C2'), 0.32); bass(24.0, nz('C2'), 0.2); bass(26.0, nz('G2'), 0.2);
  bass(28.0, nz('F2'), 0.2); bass(30.0, nz('F2'), 0.18); bass(32.0, nz('C2'), 0.2); bass(34.0, nz('C2'), 0.18);
  bass(36.0, nz('A2'), 0.2); bass(38.0, nz('F2'), 0.2); bass(40.0, nz('C2'), 0.2);
  bass(42.0, nz('F2'), 0.2); bass(44.0, nz('G2'), 0.2); bass(46.0, nz('G2'), 0.2);
  sub(48.0, 0.55); bass(50.0, nz('A2'), 0.16);

  // ---- beat 1 (0-4) hook: impact, lock, first motif --------------------
  sub(0.0, 0.8); chime(0.0, [nz('A4'), nz('E5'), nz('A5')], 0.22);
  noiseHit(0.0, 0.5, 'lowpass', 1000, 0.8, 0.55, 0, 0.3, 140);
  pluck(0.5, nz('A4'), 0.10, -0.1, 0.7, 2600, 0.35);
  pluck(1.0, nz('E5'), 0.09, 0.1, 0.6, 2600, 0.35);
  pluck(2.3, nz('C5'), 0.08, -0.15, 0.5, 2400, 0.3);
  pluck(3.1, nz('B4'), 0.07, 0.15, 0.5, 2400, 0.3);

  // ---- beat 2 (4-8) glass pings per row, chips -------------------------
  to = 's';
  [4.5, 5.25, 6.0, 6.75].forEach((t, i) => blip(t, nz(['E5','A5','Cs5','E5'][i]) * 1.0, nz(['E5','A5','Cs5','E5'][i]) * 1.06, 0.09, 0.10, 0.25 - i * 0.15));
  to = 'm';
  [5.0, 5.75, 6.5].forEach((t, i) => pluck(t, nz(['A4','C5','E5'][i]), 0.06, 0.1 - i * 0.1, 0.3, 2800, 0.25));
  sweep(7.6, 8.0, 0.10, 500, 3200);

  // ---- beat 3 (8-12) ignite, type, press, send -------------------------
  noiseHit(8.0, 0.25, 'bandpass', 1800, 1.2, 0.14, 0, 0.3, 3600);
  chime(8.0, [nz('E5')], 0.10);
  for (let i = 0; i < 4; i++) noiseHit(9.0 + i * 0.09, 0.03, 'bandpass', 2200 + i * 180, 2.0, 0.05, 0.05 * (i - 1.5), 0.15);
  to = 's';
  noiseHit(10.3, 0.05, 'highpass', 2400, 0.8, 0.16, 0, 0.2);
  sweep(10.6, 11.4, 0.14, 700, 5200);
  to = 'm';
  sub(11.4, 0.22); pluck(11.45, nz('A4'), 0.07, 0, 0.5, 2400, 0.3);

  // ---- beat 4 (12-18) alive while thinking -----------------------------
  chime(12.6, [nz('A5'), nz('Cs6')], 0.08, 0.2);
  pluck(13.5, nz('E5'), 0.06, -0.1, 0.4, 2600, 0.3);
  to = 's';
  noiseHit(15.5, 1.7, 'bandpass', 900, 1.4, 0.06, 0.1, 0.25, 4200);
  blip(17.4, nz('A4'), nz('A4'), 0.08, 0.08, 0.2);
  to = 'm';

  // ---- beat 5 (18-22) riser, then near-silence -------------------------
  [18.6, 18.8, 19.0, 19.2, 19.4, 19.6].forEach((t, i) => blip(t, 700 + i * 90, 760 + i * 90, 0.06, 0.05, 0.15));
  noiseHit(19.8, 0.06, 'highpass', 2600, 0.8, 0.1, 0, 0.2);
  riser(18.2, 19.95, 0.13);

  // ---- 22.0 the loudest hit of the film --------------------------------
  sub(22.0, 0.95);
  noiseHit(22.0, 0.55, 'lowpass', 1200, 0.7, 0.85, 0, 0.35, 150);
  chime(22.0, [nz('A4'), nz('E5'), nz('A5')], 0.30);
  pluck(22.02, nz('A3'), 0.16, 0, 0.9, 2200, 0.35);

  // ---- beat 6 (22-28) HUD payoff ---------------------------------------
  to = 's';
  [23.4, 23.5, 23.6, 23.7].forEach((t, i) => blip(t, nz(['E5','A5','Cs5','E5'][i]), nz(['E5','A5','Cs5','E5'][i]) * 1.05, 0.07, 0.07, 0.2 - i * 0.12));
  to = 'm';
  blip(26.2, nz('A3'), nz('A3'), 0.12, 0.09, 0);
  [26.4, 27.3].forEach((t) => blip(t, nz('E5'), nz('E5'), 0.05, 0.05, 0.15));
  sweep(27.0, 27.6, 0.07, 2200, 600);

  // ---- beat 7 (28-32) tip card, tap, expand ----------------------------
  to = 's';
  chime(30.2, [nz('A4'), nz('C5')], 0.14, 0.1);
  sweep(30.9, 31.8, 0.12, 600, 4800);
  to = 'm';

  // ---- beat 8 (32-36) sparse pitched ticks, seam -----------------------
  [33.0, 34.0, 35.0].forEach((t, i) => blip(t, nz(['A4','C5','E5'][i]), nz(['A4','C5','E5'][i]), 0.07, 0.075, 0.12));
  sub(35.7, 0.3);

  // ---- beat 9 (36-42) the opening motif returns ------------------------
  pluck(36.1, nz('A4'), 0.09, -0.1, 0.6, 2600, 0.35);
  pluck(36.35, nz('C5'), 0.08, 0.05, 0.55, 2600, 0.35);
  pluck(36.6, nz('E5'), 0.08, 0.15, 0.5, 2600, 0.35);
  pluck(37.1, nz('B4'), 0.07, -0.05, 0.5, 2400, 0.3);
  blip(38.0, nz('E5'), nz('E5'), 0.06, 0.06, 0.1);
  blip(38.6, nz('A4'), nz('A4'), 0.09, 0.09, 0);
  to = 's';
  sweep(39.8, 40.9, 0.10, 500, 4600);
  to = 'm';

  // ---- beat 10 (42-46) harmonic lift, contract -------------------------
  chime(42.4, [nz('C5'), nz('E5')], 0.10, 0.15);
  pluck(43.2, nz('A5'), 0.06, 0.1, 0.6, 2800, 0.3);
  sweep(45.2, 46.0, 0.08, 3000, 260);

  // ---- beat 11 (46-52) gather, resolve at 48.0, fade -------------------
  chime(46.15, [nz('A5'), nz('E6')], 0.05, -0.2);
  chime(47.1, [nz('Cs6')], 0.04, 0.2);
  chime(48.0, [nz('A4'), nz('E5'), nz('A5'), nz('Cs6')], 0.24);
  noiseHit(48.0, 0.4, 'lowpass', 900, 0.7, 0.4, 0, 0.3, 200);
  pluck(48.15, nz('A5'), 0.10, 0, 0.8, 3000, 0.4);
  pluck(48.55, nz('E5'), 0.07, -0.1, 0.7, 2600, 0.35);
  to = 's';
  sweep(49.7, 50.9, 0.05, 3400, 700);
  to = 'm';
