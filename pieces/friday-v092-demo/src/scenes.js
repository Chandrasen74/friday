// ============================================================
//  FRIDAY v0.9.2 demo - scenes: film helpers + beats 1-6.
//  Camera = crop window over the 1080x2400 forged captures, full-bleed to 1080x1920.
//  Everything is pure f(TT); seeded RNG only; no rAF, no random(), no transitions.
// ============================================================

// ---- camera: capture-space crop window -----------------------------
function view(cx, cy, z) {
  const w = W / z, h = H / z;
  let tx = cx - w / 2, ty = cy - h / 2;
  if (tx < 0) tx = 0;
  if (ty < 0) ty = 0;
  if (tx + w > 1080) tx = 1080 - w;
  // no bottom clamp: views may run past the capture's bottom edge; blit()
  // clamps the source and the ground shows through (beat 7's resting void).
  return { cx, cy, z, w, h, tx, ty };
}
const S2C = (v, sx, sy) => [v.tx + sx / v.z, v.ty + sy / v.z];
const C2S = (v, x, y) => [(x - v.tx) * v.z, (y - v.ty) * v.z];
function blit(v, im) {   // draw a 1080x2400 capture through view v, full-bleed, clamp-safe
  let sx = v.tx, sy = v.ty, sw = v.w, sh = v.h, dx = 0, dy = 0;
  if (sx < 0) { dx = -sx * v.z; sw += sx; sx = 0; }
  if (sy < 0) { dy = -sy * v.z; sh += sy; sy = 0; }
  if (sx + sw > im.width) sw = im.width - sx;
  if (sy + sh > im.height) sh = im.height - sy;
  if (sw <= 0 || sh <= 0) return;
  ctx.drawImage(im, sx, sy, sw, sh, dx, dy, sw * v.z, sh * v.z);
}
function inView(v, fn) { ctx.save(); ctx.setTransform(v.z, 0, 0, v.z, -v.tx * v.z, -v.ty * v.z); fn(); ctx.restore(); }
// camera over a key list [[t, cx, cy, z, ease?]]: log-space zoom, eased positions
function camPath(keys) {
  const t = TT;
  if (t <= keys[0][0]) return view(keys[0][1], keys[0][2], keys[0][3]);
  for (let i = 0; i < keys.length - 1; i++) {
    const a = keys[i], b = keys[i + 1];
    if (t < b[0]) {
      const e = (a[4] || EZ.io)(seg(t, a[0], b[0]));
      return view(lerp(a[1], b[1], e), lerp(a[2], b[2], e), Math.exp(lerp(Math.log(a[3]), Math.log(b[3]), e)));
    }
  }
  const L = keys[keys.length - 1];
  return view(L[1], L[2], L[3]);
}

// ---- screen-space drawing helpers ----------------------------------
function over(fn) { DEFER.push(fn); }
function lineAt(x0, y0, x1, y1, w, c, al) {
  ctx.save(); ctx.strokeStyle = c || COL.cyan; ctx.lineWidth = w; ctx.globalAlpha = al ?? 1;
  ctx.lineCap = 'round'; ctx.beginPath(); ctx.moveTo(x0, y0); ctx.lineTo(x1, y1); ctx.stroke(); ctx.restore();
}
function vtick(x, yc, h, w, c, al) { lineAt(x, yc - h / 2, x, yc + h / 2, w, c, al); }
function rrect(x, y, w, h, r, o = {}) {   // fill and/or stroke a rounded rect (screen space)
  ctx.save();
  const P = rrectPts(x, y, w, h, r, 5);
  ctx.beginPath(); P.forEach((p, i) => i ? ctx.lineTo(p[0], p[1]) : ctx.moveTo(p[0], p[1])); ctx.closePath();
  if (o.fill) { ctx.fillStyle = o.fill; if (o.al != null) ctx.globalAlpha = o.al; ctx.fill(); ctx.globalAlpha = 1; }
  if (o.line) { ctx.strokeStyle = o.line; ctx.lineWidth = o.w || 3; if (o.al != null) ctx.globalAlpha = o.al; ctx.stroke(); }
  ctx.restore();
}
// tabular big number: fixed digit cells so counters never jitter
function bigNum(str, cx, cy, size, col, o = {}) {
  ctx.save();
  ctx.font = `${o.weight || 600} ${size}px ${o.font || F_ROB}`;
  ctx.textAlign = 'left'; ctx.textBaseline = 'alphabetic';
  const dw = ctx.measureText('0').width, pw = dw * 0.32;
  let tw = 0; for (const ch of str) tw += (ch === '.' || ch === ',') ? pw : dw;
  let x = cx - tw / 2;
  const y = cy + size * 0.355;
  ctx.fillStyle = col;
  for (const ch of str) {
    const w2 = ctx.measureText(ch).width;
    if (ch === '.' || ch === ',') { ctx.fillText(ch, x + (pw - w2) / 2, y); x += pw; }
    else { ctx.fillText(ch, x + (dw - w2) / 2, y); x += dw; }
  }
  ctx.restore();
  return tw;
}
function bigUnit(str, x, y, size, col, o = {}) {
  ctx.save(); ctx.font = `${o.weight || 600} ${size}px ${F_ROB}`;
  ctx.textAlign = o.align || 'left'; ctx.textBaseline = 'alphabetic';
  ctx.globalAlpha = o.alpha ?? 1;
  ctx.fillStyle = col; ctx.fillText(str, x, y + size * 0.355); ctx.restore();
}
// captions: Roboto 600 60px uppercase, left x 90 (top slot / custom y allowed), DEFER so they float on top
function cap(text, t0, t1, y = 1434, o = {}) {
  if (TT < t0 - 1e-6 || TT > t1 + 1e-6) return;
  const ain = clamp((TT - t0) / 0.24), aout = clamp((t1 - TT) / 0.24);
  const al = Math.min(ain, aout, 1); if (al <= 0.001) return;
  const rise = (1 - EZ.o2(ain)) * 16;
  over(() => {
    ctx.save(); screen();
    ctx.globalAlpha = al;
    ctx.font = `600 60px ${F_ROB}`; ctx.textAlign = o.align || 'left'; ctx.textBaseline = 'top';
    try { ctx.letterSpacing = '3px'; } catch (e) {}
    const w2 = ctx.measureText(text.toUpperCase()).width;
    if (o.scrim) {   // void patch behind the caption (used where content slides beneath)
      const P = rrectPts((o.x ?? 90) - 40, y + rise - 22, w2 + 80, 104, 22, 6);
      ctx.beginPath(); P.forEach((p, i) => i ? ctx.lineTo(p[0], p[1]) : ctx.moveTo(p[0], p[1])); ctx.closePath();
      ctx.fillStyle = 'rgba(9,9,12,0.72)'; ctx.fill();
    }
    ctx.fillStyle = o.c || COL.text;
    ctx.fillText(text.toUpperCase(), o.x ?? 90, y + rise);
    if (o.tick) vtick((o.x ?? 90) - 30, y + rise + 28, 44, 3, COL.cyan, 0.9 * al);
    ctx.restore();
  });
}
// ILLUSTRATIVE tag for beats 5 and 9 (placeholder values, documented in PLACEHOLDERS.md)
function illuTag(x, y) {
  const draw = () => {
    ctx.save(); screen();
    ctx.font = `500 30px ${F_ROB}`; ctx.textAlign = 'left'; ctx.textBaseline = 'top';
    ctx.fillStyle = COL.text3; ctx.fillText('ILLUSTRATIVE RUN', x, y);
    ctx.restore();
  };
  if (DEFER) over(draw);        // scene body: queue for the overlay pass
  else draw();                  // inside a flushed callback: draw right here
}
// 8-wedge facet logo (official friday_logo_blue geometry), converging as p: 0 exploded -> 1 locked
function facets(im, cx, cy, size, p, seed) {
  if (p >= 0.9995 || p <= 0) return;
  const R = RNG('fac', seed), r = size * 0.75;
  for (let i = 0; i < 8; i++) {
    const a0 = i / 8 * TAU - Math.PI / 2, a1 = (i + 1) / 8 * TAU - Math.PI / 2;
    const dx = R.r(-1, 1) * size * 1.05 * (1 - p), dy = R.r(-1, 1) * size * 1.05 * (1 - p);
    const rot = R.r(-0.55, 0.55) * (1 - p);
    ctx.save(); ctx.translate(cx + dx, cy + dy); ctx.rotate(rot);
    ctx.beginPath(); ctx.moveTo(0, 0); ctx.arc(0, 0, r, a0, a1); ctx.closePath(); ctx.clip();
    ctx.drawImage(im, -size / 2, -size / 2, size, size);
    ctx.restore();
  }
}

// ---- shared geometry (probe-verified, capture space 1080x2400) ------
const G = {
  homeLogoBox: [414, 914, 252, 252], homeGlyph: [456, 958, 168, 168],
  bar: [58, 468, 8, 74], rows: [[42, 434, 756, 142], [42, 576, 756, 142], [42, 718, 756, 142], [42, 860, 756, 142]],
  capsule: [31.5, 2185, 875.25, 126], send: [930, 2193, 120, 119],
  userBubble: [317, 263, 732, 162], dotsBubble: [125, 445, 260, 176], dots: [159, 525, 77, 16],
  replyC: [125, 445, 840, 1059],
  strip: [105, 1497, 321, 48],
  hudSheetTop: 1489, hudShift: 429,
  cards: [[52.5, 1751.5, 474.4, 173.25], [553.1, 1751.5, 474.4, 173.25], [52.5, 1951, 474.4, 173.25], [553.1, 1951, 474.4, 173.25]],
  hudModel: [52.5, 2150.5, 975, 162.75], hudDot: [97.5, 2063.5], hudPlate: [60, 1812, 280, 68], heroC: [220, 1847],
  tipCard: [52, 1911, 976, 402], tipDot: [96.6, 1966], tipBtn: [91, 2205, 315, 54],
  settingsBar: [0, 63, 1080, 168], settingsBody: [0, 231, 1080, 2106],
  diag: [84, 771, 910, 512],
};

// =====================================================================
//  BEAT 1 (0-4) Instant on your phone.
//  Facets already mid-flight on frame 0, lock at 1.0; scan wipe 0.7-2.1; 8% push.
// =====================================================================
function sceneBoot() {
  fillGround();
  const home = asset('01_home.png'), logo = asset('friday_logo_blue.png');
  if (!home) return;
  const v = camPath([[0, 540, 1000, 1.02], [4, 540, 1085, 1.10]]);
  const u = seg(TT, 0.7, 2.1);
  if (u <= 0) { /* void + facets only */ }
  else if (u < 1) {
    const wx = EZ.io(u) * (W + 6) - 3;
    ctx.save(); ctx.beginPath(); ctx.rect(0, 0, wx, H); ctx.clip(); blit(v, home); ctx.restore();
    ctx.save(); screen(); ctx.globalAlpha = 0.85;
    vtick(wx, H / 2, H, 3, COL.cyan, 1);
    ctx.globalAlpha = 0.35; vtick(wx - 5, H / 2, H * 0.96, 1.5, COL.cyan2, 1);
    ctx.restore();
  } else blit(v, home);
  // facet logo: converge into the capture's own glyph (lock ~1.0, handed off to real pixels)
  if (u > 0.08 && logo) {
    const p = springEase(TT + 0.55, 120, 23);
    inView(v, () => facets(logo, 540, 1042, lerp(500, 168, p), p, 'boot'));
  }
  // scan tick hands off to the drawer's selected-row bar at the cut (bar lands at screen (65, 565))
  const tk = seg(TT, 3.15, 4.0);
  if (tk > 0 && tk < 1) {
    const x = lerp(880, 65, EZ.io(tk)), a = Math.min(clamp(tk / 0.18), 0.95);
    ctx.save(); screen(); vtick(x, 565.5, 84, 3, COL.cyan, a); ctx.restore();
  }
  cap('Instant on your phone.', 2.35, 4.35, 1180);
}

// =====================================================================
//  BEAT 2 (4-8) Everything ready locally.  Drawer rows, glass pings, chips.
//  Selected-row bar at screen (65, 565.5); extension line grows to the right edge.
// =====================================================================
function sceneModels() {
  fillGround();
  const im = asset('02_models.png'); if (!im) return;
  const v = camPath([[4, 540, 1090, 1.11], [5.2, 430, 960, 1.12], [8, 440, 857, 1.12]]);
  blit(v, im);
  // glass pings on the session rows
  const pings = [4.5, 5.25, 6.0, 6.75];
  inView(v, () => {
    for (let i = 0; i < pings.length; i++) {
      const dt = TT - pings[i]; if (dt < 0 || dt > 0.7) continue;
      const al = 0.9 * Math.exp(-dt * 5.5);
      const r = G.rows[i];
      rrect(r[0], r[1] + 5, r[2], r[3] - 10, 12, { line: COL.cyan, w: 2.5 / v.z, al });
    }
  });
  // model chips (film language for the available-models list), 24px, wrap 2+1, pop in
  const chipRows = [['QWEN3-0.6B', 'GRANITE 4.0 350M INT8'], ['GRANITE 4.0 350M FP16']];
  const tcs = [5.0, 5.75, 6.5];
  over(() => {
    ctx.save(); screen();
    ctx.font = `500 24px ${F_ROB}`; ctx.textBaseline = 'middle';
    let ci = 0;
    for (let row = 0; row < chipRows.length; row++) {
      const list = chipRows[row], cy2 = 1190 + row * 60;
      const widths = list.map((c) => ctx.measureText(c).width + 36);
      const total = widths.reduce((a, b) => a + b, 0) + 20;
      let x = 540 - total / 2;
      for (let j = 0; j < list.length; j++, ci++) {
        const s = popS(tcs[ci]); if (s <= 0.01) { x += widths[j] + 20; continue; }
        const w = widths[j], h = 44, cx2 = x + w / 2;
        ctx.save(); ctx.translate(cx2, cy2); ctx.scale(s, s); ctx.translate(-cx2, -cy2);
        rrect(x, cy2 - h / 2, w, h, 22, { fill: COL.elev, line: 'rgba(56,189,248,0.35)', w: 2 });
        ctx.fillStyle = COL.cyan2; ctx.textAlign = 'left';
        ctx.fillText(list[j], x + 18, cy2);
        ctx.restore();
        x += w + 20;
      }
    }
    ctx.restore();
  });
  // bar extension: from the selected-row bar to the frame edge at screen y 565.5 (capsule-top carry)
  const ge = EZ.i2(seg(TT, 7.55, 8.0));
  if (ge > 0) {
    const x0 = 73.9, x1 = x0 + (W - x0) * ge;
    lineAt(x0, 565.5, x1, 565.5, 3, COL.cyan, 0.95);
    lineAt(x0, 565.5, x1, 565.5, 1, '#EAF7FF', 0.8);
  }
  cap('Everything ready locally.', 4.7, 7.3);
}

// =====================================================================
//  BEAT 3 (8-12) One clean send.
//  Carried line lands on the capsule top edge (capture 2185), border ignites, typed,
//  caret, press, bubble flies up the thread, whip up.
// =====================================================================
function sceneCompose() {
  fillGround();
  const a = asset('03_compose.png'), b = asset('03_compose_typed.png');
  if (!a || !b) return;
  const v = camPath([[8, 610, 1565, 1.15], [9.0, 610, 1750, 1.45], [10.3, 610, 1750, 1.45], [10.6, 700, 1720, 1.28], [11.4, 540, 914.3, 1.05], [12, 540, 914.3, 1.05]]);
  blit(v, TT < 9.0 ? a : b);
  // carried line: glides from screen 565.5 down onto the live capsule-top edge, then ignites the border
  const gl = seg(TT, 8.0, 8.35);
  if (gl > 0 && gl < 1) {
    const capTop = C2S(v, 0, G.capsule[1])[1];
    const y = lerp(565.5, capTop, EZ.io(gl)); lineAt(0, y, W, y, 3, COL.cyan, 0.9);
  }
  const ig = seg(TT, 8.30, 8.95);
  if (ig > 0) {
    const gx = lerp(31.5, 990, EZ.io(ig));
    inView(v, () => {
      ctx.save(); ctx.beginPath(); ctx.rect(0, 2160, gx, 180); ctx.clip();
      const P = rrectPts(G.capsule[0], G.capsule[1], G.capsule[2], G.capsule[3], 63, 6);
      ctx.beginPath(); P.forEach((p, i) => i ? ctx.lineTo(p[0], p[1]) : ctx.moveTo(p[0], p[1])); ctx.closePath();
      ctx.strokeStyle = COL.cyan; ctx.lineWidth = 3.4 / v.z; ctx.globalAlpha = 0.85; ctx.stroke();
      ctx.restore();
    });
  }
  // caret (after placeholder, then after the typed prompt), deterministic blink
  if (TT < 10.3) {
    const blink = (TT % 1.06) < 0.6;
    if (blink) {
      ctx.save(); ctx.font = `400 36.75px ${F_ROB}`;
      const txt = TT < 9.0 ? 'Message F.R.I.D.A.Y...' : 'Draft a professional email reply';
      const cx2 = 78.75 + ctx.measureText(txt).width + 6;
      inView(v, () => { ctx.fillStyle = COL.cyan; ctx.fillRect(cx2, 2221, 3.5, 53); });
      ctx.restore();
    }
  }
  // press flash on the SEND pill
  const pr = TT - 10.3;
  if (pr > 0 && pr < 0.35) {
    const [sx, sy] = C2S(v, G.send[0], G.send[1]);
    rrect(sx, sy, G.send[2] * v.z, G.send[3] * v.z, 14 * v.z, { line: '#FFFFFF', w: 3, al: 0.85 * (1 - pr / 0.35) });
  }
  // user bubble cut-out flies from the composer to its thread-top landing spot
  const fly = TT - 10.6;
  if (fly > 0 && fly < 1.4) {
    const p = springEase(fly, 140, 18);
    const tw = G.userBubble[2] * v.z, th = G.userBubble[3] * v.z;
    const [lx, ly] = C2S(v, 640, 1590);
    const ex = 305.9, ey = 276.2, ew = 768.6, eh = 170.1;
    const x = lerp(lx - tw * 0.28, ex, p), y = lerp(ly - th * 0.1, ey, p);
    const w = lerp(tw * 0.5, ew, p), h = lerp(th * 0.5, eh, p);
    const al = Math.min(clamp(fly / 0.15), 1) * (fly < 1.05 ? 1 : clamp((1.4 - fly) / 0.35));
    if (al > 0.01) {
      ctx.save(); ctx.globalAlpha = al;
      ctx.drawImage(b, G.userBubble[0], G.userBubble[1], G.userBubble[2], G.userBubble[3], x, y, w, h);
      ctx.restore();
    }
  }
  cap('One clean send.', 8.6, 10.2);
}

// =====================================================================
//  BEAT 4 (12-18) Alive while thinking.
//  dots (12-13.5) -> short (13.5-15.5) -> full reply revealed by an 8th-stepped
//  clip sweep with a cyan edge line (15.5-17.2); footer element drops in 17.4-18.0.
// =====================================================================
function sceneStream() {
  fillGround();
  const sa = asset('04_stream_a.png'), sb = asset('04_stream_b.png'), sc = asset('04_stream_c.png');
  if (!sa || !sb || !sc) return;
  const v = camPath([[12, 540, 914.3, 1.05], [13.5, 490, 860, 1.12], [15.5, 430, 800, 1.2], [17, 340, 980, 1.5], [18, 280, 1355, 2.4]]);
  const sweepEnd = 17.2, inSweep = TT >= 15.5 && TT < sweepEnd;
  // base: b (short) under, c (full) revealed above the stepped edge while sweeping
  blit(v, inSweep ? sb : TT < 13.5 ? sa : sb);
  if (inSweep) {
    const u = seg(TT, 15.5, sweepEnd) * 7;
    const step = Math.min(Math.floor(u), 6) / 6;
    const cutY = 400 + (1560 - 400) * step;
    ctx.save(); ctx.beginPath(); ctx.rect(0, 0, W, (cutY - v.ty) * v.z); ctx.clip();
    blit(v, sc); ctx.restore();
    const ey = (cutY - v.ty) * v.z;
    ctx.save(); screen();
    lineAt(0, ey, W, ey, 3, COL.cyan, 0.95); lineAt(0, ey, W, ey, 1, '#EAF7FF', 0.85);
    ctx.restore();
  }
  // carried rect (beat 3's landing bubble) glides onto the dots bubble, then the dots pulse alive
  if (TT < 13.5) {
    const g = EZ.io(seg(TT, 12.0, 12.6));
    const [dx, dy] = C2S(v, G.dotsBubble[0], G.dotsBubble[1]);
    const x = lerp(305.9, dx, g), y = lerp(276.2, dy, g);
    const w = lerp(768.6, G.dotsBubble[2] * v.z, g), h = lerp(170.1, G.dotsBubble[3] * v.z, g);
    rrect(x, y, w, h, 14 * v.z, { line: COL.cyan, w: 2.5, al: 0.75 * (1 - g * 0.45) });
  }
  if (TT < 13.5) {   // typing dots alive over the real ones
    inView(v, () => {
      for (let i = 0; i < 3; i++) {
        const cx2 = 171 + i * 26, cy2 = 533;
        const s = 1 + 0.28 * Math.sin((TT * 7.2 - i * 0.9) * TAU / 4);
        ctx.save(); ctx.translate(cx2, cy2); ctx.scale(s, s);
        ctx.beginPath(); ctx.arc(0, 0, 11, 0, TAU); ctx.fillStyle = COL.cyan; ctx.fill(); ctx.restore();
      }
    });
  }
  // footer metrics element drops in at its capture-true position (lands exactly on the cut)
  const fd = seg(TT, 17.4, 18.0);
  if (fd > 0) {
    const l05 = asset('l05_footer.png');
    if (l05) {
      const drop = (1 - EZ.o2(fd)) * 64;
      inView(v, () => {
        ctx.save(); ctx.globalAlpha = clamp(fd * 2.5);
        ctx.drawImage(l05, G.strip[0], G.strip[1] + drop, G.strip[2], G.strip[3]);
        ctx.restore();
      });
    }
  }
  cap('Alive while thinking.', 12.7, 14.3, 1100);
}

// =====================================================================
//  BEAT 5 (18-22) Every run, measured.
//  Pull back from the strip macro to the held frame; hero counters count, swap;
//  camera locks 19.2-22.0 (silence). Hero rect (400, 978) carries through the hard cut.
// =====================================================================
function sceneFooter() {
  fillGround();
  const im = asset('05_footer.png'); if (!im) return;
  const v = camPath([[18, 280, 1355, 2.4], [19.2, 470, 1136, 1.15], [22, 470, 1136, 1.15]]);
  blit(v, im);
  // hero counters at the shared rect (400, 978), 240px digits
  const cnt = seg(TT, 18.6, 19.6);
  const swap = TT >= 19.8;
  const a1 = swap ? clamp((19.95 - TT) / 0.15) : Math.min(clamp((TT - 18.55) / 0.2), 1);
  const a2 = swap ? clamp((TT - 19.8) / 0.2) : 0;
  const scrim = () => {   // focus panel: the 240px counters must read over the email body text
    const P = rrectPts(150, 872, 750, 218, 24, 6);
    ctx.beginPath(); P.forEach((p, i) => i ? ctx.lineTo(p[0], p[1]) : ctx.moveTo(p[0], p[1])); ctx.closePath();
    ctx.fillStyle = 'rgba(9,9,12,0.78)'; ctx.fill();
  };
  over(() => {
    if (a1 > 0.01 && !swap) {
      ctx.save(); screen(); ctx.globalAlpha = a1;
      scrim();
      const val = (12.6 * EZ.io(cnt)).toFixed(1);
      const w = bigNum(val, 400, 978, 240, COL.white);
      bigUnit('tok/s', 400 + w / 2 + 22, 978, 88, COL.cyan, { align: 'left' });
      ctx.restore(); illuTag(400 - 110, 1078);
    }
    if (a2 > 0.01) {
      ctx.save(); screen(); ctx.globalAlpha = a2;
      scrim();
      bigNum('2.41', 400, 978, 240, COL.white);
      bigUnit('s', 400 + bigNum('2.41', 400, 978, 240, COL.white) / 2 + 22, 978, 88, COL.cyan, { align: 'left' });
      ctx.restore(); illuTag(400 - 110, 1078);
    }
  });
  cap('Every run, measured.', 19.3, 21.6, 1434);
}

// =====================================================================
//  BEAT 6 (22-28) Open the HUD.  HARD CUT at 22.0 / frame 660.
//  Macro on the decode card (hero persists), pull back, card parallax,
//  CPU row expand to the backend card macro, amber dot pulse.
// =====================================================================
function sceneHud() {
  fillGround();
  const im = asset('06_hud.png'), sheet = asset('l06_hud.png');
  if (!im) return;
  const v = camPath([[22, 290, 1838, 2.0], [23, 290, 1838, 2.0], [24, 540, 1527, 1.10], [26, 540, 1527, 1.10], [28, 222.4, 2031.9, 2.6]]);
  blit(v, im);
  inView(v, () => {   // hide the nav pill during macros (kept identical across the 28.0 cut)
    ctx.fillStyle = COL.void; ctx.fillRect(0, 2332, 1080, 68);
  });
  // hero on the decode card's value plate (capture-anchored): grows 240 -> 300 screen px during the hold.
  // Patch covers the card's label + value rows (measured in 06_hud.png: label 93,1791-317,1810, cyan
  // value 93,1850-278,1884, card interior ends ~x 540 / y 1923); label redrawn above the digits.
  const grow = seg(TT, 22.0, 23.0);
  const heroSize = lerp(120, 150, EZ.io(grow));   // capture px: 240 -> 300 screen at z 2.0
  inView(v, () => {
    ctx.fillStyle = COL.elev; ctx.fillRect(56, 1768, 484, 154);
    ctx.font = `600 30px ${F_ROB}`; ctx.textAlign = 'left'; ctx.textBaseline = 'alphabetic';
    try { ctx.letterSpacing = '1px'; } catch (e) {}
    ctx.fillStyle = COL.text3; ctx.fillText('DECODE SPEED', 93, 1811);
    try { ctx.letterSpacing = '0px'; } catch (e) {}
    const tw = bigNum('12.6', G.heroC[0], 1870, heroSize, COL.white, { font: F_ROB, weight: 600 });
    // tok/s unit: dissolves as the 26.5+ exit dolly carries its ink toward the frame edge
    // (distance-keyed on the live C2S edge, never sliced); opening macro keeps full alpha
    ctx.font = `600 ${heroSize * 0.375}px ${F_ROB}`;
    const uw = ctx.measureText('tok/s').width;
    const uEdge = C2S(v, G.heroC[0] + tw / 2 + 24 + uw + 6, 0)[0];
    const ua = TT > 26.5 ? clamp((W - 24 - uEdge) / 120) : 1;
    bigUnit('tok/s', G.heroC[0] + tw / 2 + 24, 1870 + 0.355 * heroSize * 0.625, heroSize * 0.375, COL.cyan, { align: 'left', weight: 600, alpha: ua });
  });
  // four card cut-outs breathe apart, converge back by 26.0
  if (sheet) {
    const cw = seg(TT, 23.4, 24.4), back = EZ.io(seg(TT, 25.2, 26.0));
    if (cw > 0 && back < 1) {
      inView(v, () => {
        for (let i = 0; i < 4; i++) {
          const st = clamp((cw * 4 - i * 0.55) / 1.6); if (st <= 0) continue;
          const R = RNG('par', i);
          const ax = R.r(-1, 1) * 30, ay = R.r(-1, 1) * 22, w0 = R.r(0.7, 1.3);
          const k = st * (1 - back);
          const ox = ax * Math.sin(TT * w0 * 1.9 + i * 1.7) * k, oy = ay * Math.cos(TT * w0 * 1.6 + i * 2.3) * k;
          const c = G.cards[i];
          ctx.save();
          ctx.fillStyle = COL.base; ctx.fillRect(c[0] - 4, c[1] - 4, c[2] + 8, c[3] + 8);   // punch the card out
          const m = 6;
          ctx.drawImage(sheet, c[0] - m - G.hudSheetTop * 0, c[1] - m + G.hudSheetTop, c[2] + 2 * m, c[3] + 2 * m,
            c[0] - m + ox, c[1] - m + G.hudSheetTop + oy, (c[2] + 2 * m), (c[3] + 2 * m));
          ctx.restore();
        }
      });
    }
  }
  // CPU row expand: camera to the backend card, amber dot pulse, hairline underline
  if (TT > 26.2) {
    const dp = (TT - 26.4) % 0.9;
    if (TT > 26.4) {
      inView(v, () => {
        const r = 12 + 38 * dp, al = 0.85 * (1 - dp);
        ctx.save(); ctx.strokeStyle = COL.cyan; ctx.lineWidth = 2.6 / v.z; ctx.globalAlpha = al;
        ctx.beginPath(); ctx.arc(G.hudDot[0], G.hudDot[1], r, 0, TAU); ctx.stroke(); ctx.restore();
      });
    }
    const un = EZ.io(seg(TT, 27.0, 27.6));
    if (un > 0) inView(v, () => {
      ctx.save(); ctx.strokeStyle = COL.cyan; ctx.lineWidth = 2.6 / v.z; ctx.globalAlpha = 0.9;
      ctx.beginPath(); ctx.moveTo(97, 2100); ctx.lineTo(97 + 260 * un, 2100); ctx.stroke(); ctx.restore();
    });
  }
  cap('Open the HUD.', 22.6, 24.6, 330);
}
