// ============================================================
//  FRIDAY v0.9.2 demo - scenes: beats 7-11.
//  One carried element per cut, authored here as pure f(TT); era
//  boundaries are hard cuts. Same shared helpers as scenes.js.
// ============================================================

// ---------------------------------------------------------------------
//  Shared cross-cut draws (must render identically in beats 7 and 8).
//  The GET THE TIP button flies up and becomes the settings top bar strip:
//  a cyan band over capture rows 96..231 (the bar's identical zone; its bottom
//  edge at 231 is the seam that beat 8's body squash lands on).
// ---------------------------------------------------------------------
function carriedBand(v) {
  const u = seg(TT, 31.8, 32.15);                       // wipe-in L->R, crosses the 32.0 cut
  if (u <= 0) return;
  const fade = 1 - EZ.io(seg(TT, 32.6, 33.6));          // fill resolves in beat 8
  const sy = (231 - v.ty) * v.z;
  if (sy <= -30 || sy >= H + 30) return;                // gone with the dolly
  const wx = 1080 * EZ.io(u);
  inView(v, () => {
    if (fade > 0.01) {
      ctx.fillStyle = COL.cyan; ctx.globalAlpha = 0.16 * fade;
      ctx.fillRect(0, 96, wx, 135);
      ctx.globalAlpha = 1;
      lineAt(0, 96, wx, 96, 1.5 / v.z, COL.cyan, 0.5 * fade);
    }
    lineAt(0, 231, wx, 231, 2.5 / v.z, COL.cyan, 0.95); // the seam hairline
  });
}
function pinnedButton(v, tip7) {
  if (TT < 31.8 || !tip7) return;
  const al = TT < 33.3 ? 1 : clamp((33.9 - TT) / 0.6);  // exits as the dolly takes it off
  if (al <= 0.01) return;
  inView(v, () => { ctx.save(); ctx.globalAlpha = al;
    ctx.drawImage(tip7, 91, 2205, 315, 54, 0, 114, 682, 117); ctx.restore(); });
}

// =====================================================================
//  BEAT 7 (28-32) Tuned for this device.
//  Amber dot carried from the HUD macro (the 429px tip shift cancels in the
//  camera); the tip card grows out of its own icon and hands off to real
//  pixels; tap; the button flies up and is pinned on the carried band;
//  camera ends at identity so beat 8 is camera-continuous.
// =====================================================================
function sceneTip() {
  fillGround();
  const im = asset('07_tip.png'), card = asset('l07_tip.png'), tip7 = im;
  if (!im) return;
  const v = camPath([
    [28, 222.4, 1602.9, 2.6], [28.7, 222.4, 1602.9, 2.6],
    [29.4, 540, 2113.4, 1.2], [30.2, 540, 2113.4, 1.2],
    [30.6, 540, 2073.4, 1.26], [31.9, 540, 960, 1.0], [32, 540, 960, 1.0],
  ]);
  blit(v, im);
  // amber dot pulse rings continue from beat 6 (same formula -> seamless at the cut)
  if (TT < 29.0) {
    const dp = (TT - 26.4) % 0.9;
    inView(v, () => { ctx.save(); ctx.strokeStyle = COL.cyan; ctx.lineWidth = 2.6 / v.z;
      ctx.globalAlpha = 0.85 * (1 - dp); ctx.beginPath();
      ctx.arc(97.5, 1634.5, 12 + 38 * dp, 0, TAU); ctx.stroke(); ctx.restore(); });
  }
  // tip card grows about its icon dot; patch = the real surrounding base pixels
  const gk = springEase(TT - 28.4, 120, 23), kd = Math.min(gk, 1);
  if (TT >= 28.4 && card && kd < 0.9995) {
    inView(v, () => {
      ctx.fillStyle = COL.base; ctx.fillRect(0, 1908, 1080, 492);
      const x = G.tipDot[0] + (G.tipCard[0] - G.tipDot[0]) * kd, y = G.tipDot[1] + (G.tipCard[1] - G.tipDot[1]) * kd;
      ctx.drawImage(card, 0, 0, G.tipCard[2], G.tipCard[3], x, y, G.tipCard[2] * kd, G.tipCard[3] * kd);
    });
  }
  // tap flash on the button
  const tp = TT - 30.2;
  if (tp > 0 && tp < 0.35) {
    const [bx, by] = C2S(v, G.tipBtn[0], G.tipBtn[1]);
    rrect(bx, by, G.tipBtn[2] * v.z, G.tipBtn[3] * v.z, 14 * v.z, { line: '#FFFFFF', w: 3, al: 0.85 * (1 - tp / 0.35) });
  }
  // button flight in capture space (91,2205,315,54) -> (0,114,682,117), uniform ~2.17x
  const fl = TT - 30.9;
  if (fl > 0 && fl < 0.9) {
    const p = springEase(fl, 140, 18);
    inView(v, () => {
      if (p < 0.999) { ctx.fillStyle = COL.elev; ctx.fillRect(G.tipBtn[0], G.tipBtn[1], G.tipBtn[2], G.tipBtn[3]); }
      ctx.drawImage(im, 91, 2205, 315, 54, lerp(91, 0, p), lerp(2205, 114, p), lerp(315, 682, p), lerp(54, 117, p));
    });
  }
  carriedBand(v);
  pinnedButton(v, tip7);
  cap('Tuned for this device.', 28.6, 30.2, 1434);
}

// =====================================================================
//  BEAT 8 (32-36) Clarity at every layer.
//  Camera-continuous from beat 7 (identity at 32.0); dolly down the list with
//  sparse ticks; at 35.4 the frame decomposes invisibly (top bar band + body
//  layer, pixel-identical) and the body squashes into the seam line, whose
//  front lands at screen 1250 exactly where beat 9 holds its own line.
// =====================================================================
function sceneSettings() {
  fillGround();
  const im = asset('08_settings.png'), body = asset('l08_body.png'), tip7 = asset('07_tip.png');
  if (!im) return;
  const v = camPath([[32, 540, 960, 1.0], [35, 540, 1385, 1.18], [36, 540, 1385, 1.18]]);
  const swapped = TT >= 35.4;
  const k = swapped ? 1 - 0.3354 * EZ.io(seg(TT, 35.4, 36.0)) : 1;   // body scaleY 1 -> 0.6646
  if (!swapped) {
    blit(v, im);
  } else {
    inView(v, () => ctx.drawImage(im, 0, 0, 1080, 231, 0, 0, 1080, 231));   // top bar band, unsquashed
    ctx.save(); ctx.translate(0, -401.7); ctx.scale(1, k); ctx.translate(0, 401.7);
    inView(v, () => {
      if (body) ctx.drawImage(body, 0, 231, 1080, 2106, 0, 231, 1080, 2106);
      else ctx.drawImage(im, 0, 231, 1080, 2106, 0, 231, 1080, 2106);
    });
    ctx.restore();
    const fy = -401.7 + 2485.1 * k;                   // the compression front (body bottom edge)
    if (fy < H + 20) { lineAt(0, fy, W, fy, 2.5, COL.cyan, 0.95); lineAt(0, fy, W, fy, 1, '#EAF7FF', 0.6); }
  }
  carriedBand(v);
  pinnedButton(v, tip7);
  for (let i = 0; i < 3; i++) {                       // pass-marker ticks as the dolly scrolls the list
    const s = popS([33.0, 34.0, 35.0][i]); if (s <= 0.01) continue;
    ctx.save(); screen(); vtick(48, 960, 46 * s, 3.5, COL.cyan, 0.9); ctx.restore();
  }
  cap('Clarity at every layer.', 32.6, 34.0, 1434, { scrim: true });
}

// =====================================================================
//  BEAT 9 (36-42) Metrics survive relaunch.
//  The seam line is held at screen 1250 (carried from beat 8's compression
//  front). The bottom chrome slides away, everything above compresses into
//  the line, void, the metrics strip returns alone, the thread re-pours
//  upward behind a rising clip edge, then a stacked before/after comparison.
//  DEVIATION from shotlist: the strips stack (before above after) instead of
//  a wipe - logged in LOG.md.
// =====================================================================
function scenePersist() {
  fillGround();
  const im = asset('09_relaunch_after.png'), before = asset('l09_before.png'), strip = asset('l09_after.png');
  if (!im) return;
  const v = camPath([[36, 540, 1221, 1.12], [40.9, 540, 1221, 1.12], [42, 265.4, 1503.5, 2.3]]);
  const seamC = 1480, seamS = 1250, ty0 = 363.857;
  // above-seam content squashes into the line (38.2-39.0), then fades to void
  const kS = TT < 38.2 ? 1 : lerp(1, 0.03, EZ.i2(seg(TT, 38.2, 39.0)));
  const fadeS = TT < 39.0 ? 1 : clamp(1 - seg(TT, 39.0, 39.4));
  if (kS > 0.004 && fadeS > 0.01) {
    ctx.save(); ctx.translate(0, seamS); ctx.scale(1, kS); ctx.translate(0, -seamS);
    ctx.globalAlpha = fadeS;
    inView(v, () => ctx.drawImage(im, 0, 0, 1080, seamC, 0, 0, 1080, seamC));
    ctx.restore();
  }
  // below-seam chrome slides down and away (37.4-38.2), then stays gone
  const dy = TT < 37.4 ? 0 : TT < 38.2 ? 920 * EZ.i2(seg(TT, 37.4, 38.2)) : 920;
  if (dy < 919.5) inView(v, () => ctx.drawImage(im, 0, seamC, 1080, 920 - dy, 0, seamC + dy, 1080, 920 - dy));
  // the metrics strip returns alone (39.4-39.9)
  const lf = seg(TT, 39.4, 39.9);
  if (lf > 0) {
    const dy2 = 920 * (1 - EZ.o2(lf));
    inView(v, () => {
      if (strip) ctx.drawImage(strip, 0, 0, 321, 48, G.strip[0], G.strip[1] + dy2, G.strip[2], G.strip[3]);
      else ctx.drawImage(im, G.strip[0], G.strip[1], G.strip[2], G.strip[3], G.strip[0], G.strip[1] + dy2, G.strip[2], G.strip[3]);
    });
  }
  // wipe: the thread re-pours upward behind a rising clip edge (39.9-40.9)
  const wp = seg(TT, 39.9, 40.9);
  if (wp > 0) {
    const cE = seamC - (seamC - ty0) * EZ.io(wp);
    inView(v, () => ctx.drawImage(im, 0, cE, 1080, seamC - cE, 0, cE, 1080, seamC - cE));
    const ey = (cE - ty0) * v.z;
    ctx.save(); screen();
    lineAt(0, ey, W, ey, 2.5, COL.cyan, 0.9); lineAt(0, ey, W, ey, 1, '#EAF7FF', 0.55);
    ctx.restore();
  }
  // the seam line itself (capture-anchored so it tracks the comparison push, fades there)
  const la = TT < 40.9 ? 1 : clamp(1 - seg(TT, 40.9, 41.4));
  if (la > 0.01) {
    const pulse = (TT > 38.2 && TT < 39.2) ? Math.exp(-Math.abs(TT - 38.6) * 5) : 0;
    inView(v, () => {
      lineAt(0, seamC, 1080, seamC, (2.5 + 2.5 * pulse) / v.z, COL.cyan, 0.95 * la);
      lineAt(0, seamC, 1080, seamC, 1 / v.z, '#EAF7FF', (0.6 + 0.4 * pulse) * la);
    });
  }
  // stacked comparison: real after strip + before element above it
  const ca = TT - 41.3;
  if (ca > 0 && before) {
    const al = clamp(ca / 0.5);
    ctx.save(); screen(); ctx.globalAlpha = al;
    ctx.drawImage(before, 0, 0, 321, 48, 171, 830, 738, 110);
    ctx.restore();
    over(() => {
      ctx.save(); screen(); ctx.globalAlpha = al;
      ctx.font = `600 40px ${F_ROB}`; ctx.textBaseline = 'top';
      try { ctx.letterSpacing = '2px'; } catch (e) {}
      ctx.fillStyle = COL.text3;
      ctx.fillText('BEFORE', 171, 782); ctx.fillText('AFTER', 171, 1068);
      ctx.restore();
    });
    if (al > 0.9) illuTag(171, 1122);
  }
  // carried hairline: the before strip's top edge -> the diagnostics top edge in beat 10
  const hl = seg(TT, 41.6, 42.0);
  if (hl > 0) { ctx.save(); screen(); lineAt(0, 830, W, 830, 2.5, COL.cyan, 0.9 * hl); ctx.restore(); }
  cap('Metrics survive relaunch.', 36.6, 38.2, 1434);
}

// =====================================================================
//  BEAT 10 (42-46) Crafted in v0.9.2.
//  The diagnostics block is carried in on its top edge (the hairline lands
//  at screen 830 from beat 9); v0.9.2 gets a cyan highlight; then the whole
//  frame contracts to the point (540,860) where the logo assembles.
// =====================================================================
function sceneCraft() {
  fillGround();
  const im = asset('10_about.png');
  if (!im) return;
  const v = camPath([[42, 540, 857.7, 1.5], [43.2, 540, 1027, 1.15], [46, 540, 1027, 1.15]]);
  const ct = seg(TT, 45.2, 46.0);
  const draw = () => {
    blit(v, im);
    ctx.transform(v.z, 0, 0, v.z, -v.tx * v.z, -v.ty * v.z);   // capture-anchored extras below
    const hlA = 1 - EZ.io(seg(TT, 42.6, 43.2));
    if (hlA > 0.01) {
      lineAt(-200, 771, 1280, 771, 2.2 / v.z, COL.cyan, 0.9 * hlA);
      if (TT < 42.6) {
        const bx = lerp(-140, 1220, EZ.io(seg(TT, 42.0, 42.6)));
        lineAt(bx - 90, 771, bx + 90, 771, 3.2 / v.z, '#EAF7FF', 0.9);
      }
    }
    const hk = Math.min(springEase(TT - 42.4, 240, 18), 1);
    if (hk > 0.01) {
      rrect(201 - 8 * hk, 928 - 8 * hk, 104 + 16 * hk, 29 + 16 * hk, 9, { line: COL.cyan, w: 2.6 / v.z, al: 0.9 });
    }
  };
  if (ct <= 0) { draw(); }
  else {
    const s = lerp(1, 0.03, EZ.i2(ct));
    const al = clamp((s - 0.03) / 0.12);
    if (al > 0.001) {
      ctx.save(); ctx.translate(540, 860); ctx.scale(s, s); ctx.translate(-540, -860);
      ctx.globalAlpha = al; draw(); ctx.restore();
    }
  }
  cap('Crafted in v0.9.2.', 42.8, 44.8, 1434);
}

// =====================================================================
//  BEAT 11 (46-52) on-device, by design.
//  One void beat, then the facets gather into the official mark, lock with
//  a ring at 48.0, wordmark, tagline, one last scan sweep. No caption.
// =====================================================================
function sceneLockup() {
  fillGround();
  const logo = asset('friday_logo_blue.png');
  if (!logo) return;
  const p = springEase(TT - 46.1, 120, 23);
  if (p >= 0.9995) {
    ctx.save(); screen(); ctx.drawImage(logo, 540 - 120, 860 - 120, 240, 240); ctx.restore();
    const lp = TT - 48.0;                             // lock click
    if (lp > 0 && lp < 0.55) {
      ctx.save(); screen(); ctx.strokeStyle = COL.cyan; ctx.lineWidth = 2.5;
      ctx.globalAlpha = 0.5 * (1 - lp / 0.55);
      ctx.beginPath(); ctx.arc(540, 860, 150 + 70 * EZ.io(lp / 0.55), 0, TAU); ctx.stroke(); ctx.restore();
    }
  } else if (p > 0.001) {
    ctx.save(); screen(); ctx.globalAlpha = clamp((TT - 46.1) / 0.25);
    facets(logo, 540, 860, 240, p, 'lock'); ctx.restore();
  }
  const wm = seg(TT, 48.15, 48.55);
  if (wm > 0) over(() => {
    ctx.save(); screen(); ctx.globalAlpha = wm;
    ctx.font = `700 64px ${F_ORB}`; ctx.textAlign = 'center'; ctx.textBaseline = 'alphabetic';
    ctx.fillStyle = COL.white;
    ctx.fillText('F.R.I.D.A.Y', 540, 1085 + (1 - EZ.o2(wm)) * 18);
    ctx.restore();
  });
  const tg = seg(TT, 48.55, 48.95);
  if (tg > 0) over(() => {
    ctx.save(); screen(); ctx.globalAlpha = tg;
    ctx.font = `500 30px ${F_ROB}`; ctx.textAlign = 'center'; ctx.textBaseline = 'alphabetic';
    try { ctx.letterSpacing = '5px'; } catch (e) {}
    ctx.fillStyle = COL.text2;
    ctx.fillText('READY FOR LOCAL INFERENCE', 540, 1140 + (1 - EZ.o2(tg)) * 14);
    ctx.restore();
  });
  const su = EZ.io(seg(TT, 49.7, 50.9));              // final scan sweep
  if (su > 0 && su < 1) {
    const y = lerp(700, 1200, su);
    ctx.save(); screen();
    lineAt(0, y, W, y, 2, COL.cyan, 0.45);
    lineAt(0, y + 5, W, y + 5, 1, COL.cyan2, 0.18);
    ctx.restore();
  }
}
