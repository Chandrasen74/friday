// Override: font gate + frame mapping helpers for the 30fps/1560f export.
// build.mjs wraps the final window.renderFrame in an ASSETS_READY gate; the
// web-font loads are nested into that same gate here so frame 0 is never
// drawn with fallback faces (film rule: exactly two bundled typefaces).
(function(){
  window.FRAME_AT = (f) => f / 30;
  const load = (fam, wt) => document.fonts.load(`${wt} 16px "${fam}"`, 'F.R.I.D.A.Y 0123');
  window.ASSETS_READY = Promise.all([
    window.ASSETS_READY || Promise.resolve(),
    load('Orbitron', 700),
    load('Roboto', 400), load('Roboto', 500), load('Roboto', 600), load('Roboto', 700),
  ]);
  window.assertFridayTimeline = function(){
    const T = window.TIMELINE;
    if (!T) throw new Error('no timeline');
    if (T.frames !== 1560 || T.fps !== 30 || T.duration !== 52.0) throw new Error('bad spec');
    const hard = T.transitions.filter(t=>t.type==='hard-cut');
    if (hard.length !== 1 || hard[0].frame !== 660 || hard[0].t !== 22.0) throw new Error('bad hard cut');
    return true;
  };
  window.assertFridayTimeline();
})();
