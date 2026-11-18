// No kit window-morph bridges: all continuity is authored in scenes as
// deterministic carried elements; era boundaries are hard cuts. Exactly one
// structural hard cut in the film: the HUD reveal at 22.0s (frame 660).
const ERA_BG = new Array(11).fill('#09090C');
function pieceCam(era, t) { return null; }
const BRIDGES = [];

(function(){
  const hard = (TIMELINE.transitions||[]).filter(t=>t.type==='hard-cut');
  if (hard.length !== 1) throw new Error('Expected exactly 1 hard-cut transition');
  if (hard[0].t !== 22.0 || hard[0].frame !== 660) throw new Error('Hard cut must be at 22.0s/frame 660');
})();
