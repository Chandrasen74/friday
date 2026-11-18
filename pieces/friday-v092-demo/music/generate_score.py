import math, struct, wave, os

# FRIDAY v0.9.2 procedural score - 52.0s, 48kHz stereo, deterministic.
# Musical structure per spec: 0-9.2s sparse opening, 9.2-18.6s clean notes/pulse,
# 18.6-22.6s completion shimmer, 22.6-23.4s drop to near silence, 23.4s single
# loudest payoff hit, 23.4-41.8s warm continuation, 41.8-46.8s measured lift,
# 46.8-52.0s resolve and fade. No VO/lyrics/SFX track.

SR = 48000
DUR = 52.0
N = int(SR * DUR)

# Simple original chord family (Am/C ambiguity, calm): Am(add9) -> Fmaj7 -> Cmaj7 -> G6
# Notes in Hz
A2, E3, A3, B3, C4, D4, E4, F4, G4, A4, B4 = 110.0, 164.81, 220.0, 246.94, 261.63, 293.66, 329.63, 349.23, 392.0, 440.0, 493.88
D3, F3 = 146.83, 174.61
G2, G3 = 98.0, 196.0
C5 = 523.25
E5 = 659.25

def env_exp(t, a, d, s_level=0.0):
    if t < 0: return 0.0
    if t < a: return (t / max(a,1e-6))
    tt = t - a
    v = math.exp(-tt / max(d,1e-4))
    return v * (1.0 - s_level) + s_level

def soft_clip(x):
    return math.tanh(x * 1.2) / math.tanh(1.2)

def sine(f, t, ph=0.0): return math.sin(2*math.pi*f*t + ph)
def tri(f, t):
    x = (f*t) % 1.0
    return 4*abs(x-0.5)-1.0

L = [0.0]*N
R = [0.0]*N

def add_tone(freq, t0, dur, amp, kind='sine', pan=0.0, attack=0.01, decay=0.8, det=0.0, vib=0.0):
    s0 = max(0, int(t0*SR))
    s1 = min(N, int((t0+dur)*SR))
    if s1 <= s0: return
    lg = math.cos((pan+1)*math.pi/4)
    rg = math.sin((pan+1)*math.pi/4)
    for i in range(s0, s1):
        t = i/SR
        lt = t - t0
        e = 1.0
        if lt < attack: e = lt/attack
        else: e = math.exp(-(lt-attack)/max(decay,1e-3))
        rem = (t0+dur) - t
        if rem < 0.05: e *= max(0.0, rem/0.05)
        f = freq + det + (math.sin(2*math.pi*5.2*t)*vib if vib else 0.0)
        if kind=='sine': v = sine(f,t)
        elif kind=='tri': v = tri(f,t)*0.8
        else: v = sine(f,t)
        val = v * amp * e
        L[i] += val * lg
        R[i] += val * rg

def add_pad(freqs, t0, t1, amp, attack=0.6, release=0.8):
    s0 = max(0, int(t0*SR))
    s1 = min(N, int(t1*SR))
    for idx,f in enumerate(freqs):
        pan = (-0.25 + 0.5*(idx/max(1,len(freqs)-1))) if len(freqs)>1 else 0.0
        lg = math.cos((pan+1)*math.pi/4); rg = math.sin((pan+1)*math.pi/4)
        for i in range(s0, s1):
            t = i/SR
            e = 1.0
            if t < t0+attack: e = (t-t0)/max(attack,1e-4)
            if t > t1-release: e = min(e, (t1-t)/max(release,1e-4))
            e = max(0.0, min(1.0, e))
            mod = 1.0 + 0.02*math.sin(2*math.pi*0.12*t + idx)
            v = 0.7*sine(f*mod,t) + 0.25*sine(f*2.0,t*0.999+0.3) + 0.15*tri(f*0.5,t)
            val = v * amp * e
            L[i] += val * lg; R[i] += val * rg

# 0.0-9.2s sparse opening: soft low pad + clean opening tone
add_pad([A2, E3, A3, B3, E4], 0.0, 9.4, 0.055, attack=1.2, release=1.0)
add_tone(A4, 0.7, 2.4, 0.045, 'sine', pan=-0.1, attack=0.02, decay=1.6)
add_tone(E4, 2.4, 2.0, 0.03, 'sine', pan=0.1, attack=0.03, decay=1.4)
add_tone(B4, 4.0, 1.6, 0.028, 'sine', pan=0.0, attack=0.02, decay=1.2)
add_tone(C4, 1.0, 0.6, 0.05, 'tri', pan=0.0, attack=0.005, decay=0.25)

# 9.2-18.6s: a few clean notes and restrained forward pulse
add_pad([F3, A3, C4, E4], 9.0, 18.8, 0.05, attack=0.8, release=0.8)
pulse_notes = [(9.6,C4),(10.8,E4),(12.0,A4),(12.6,G4),(13.8,E4),(15.0,C4),(16.2,E4),(17.4,A4)]
for i,(tt,f) in enumerate(pulse_notes):
    add_tone(f, tt, 0.9, 0.03, 'sine', pan=(-0.15 if i%2==0 else 0.15), attack=0.008, decay=0.5)
for k in range(0, 38):
    tt = 9.2 + k*0.25
    if tt > 18.55: break
    add_tone(A2, tt, 0.06, 0.012, 'sine', pan=0.0, attack=0.003, decay=0.03)

# 18.6-22.6s completion shimmer and gathering tension
add_pad([A2, E3, C4, E4, G4], 18.4, 22.7, 0.055, attack=0.5, release=0.3)
for i,f in enumerate([E4,G4,B4,E4,C4,B4]):
    add_tone(f, 18.8 + i*0.55, 1.1, 0.026, 'sine', pan=(-0.2+i*0.07), attack=0.01, decay=0.7)
add_tone(A4, 22.1, 0.7, 0.03, 'sine', pan=0.0, attack=0.02, decay=0.5)

# 22.6-23.4s pronounced drop to near silence (extremely restrained tail)
add_tone(E4, 22.65, 0.7, 0.006, 'sine', pan=0.0, attack=0.02, decay=0.6)

# 23.4s: single largest musical hit
hit_t = 23.4
add_tone(A2, hit_t, 1.8, 0.22, 'sine', pan=0.0, attack=0.002, decay=0.9)
add_tone(E3, hit_t, 1.6, 0.12, 'tri', pan=0.0, attack=0.002, decay=0.8)
add_tone(A3, hit_t, 1.5, 0.08, 'sine', pan=-0.05, attack=0.002, decay=0.9)
add_tone(C4, hit_t+0.01, 2.2, 0.07, 'sine', pan=0.05, attack=0.004, decay=1.4)
add_tone(E4, hit_t+0.02, 2.4, 0.06, 'sine', pan=0.12, attack=0.004, decay=1.6)
add_tone(B4, hit_t+0.03, 2.0, 0.045, 'sine', pan=-0.12, attack=0.005, decay=1.5)

# 23.4-41.8s warm controlled continuation
add_pad([C4/2, G3, C4, E4, G4], 23.5, 32.8, 0.048, attack=0.7, release=0.9)
add_pad([A2, E3, A3, C4, E4], 32.6, 41.9, 0.05, attack=0.8, release=1.0)
cont = [(24.2,G4),(25.4,E4),(26.6,C4),(28.2,E4),(29.4,G4),(30.8,A4),(31.8,G4),(33.2,E4),(34.4,C4),(35.6,E4),(36.8,G4),(38.0,A4),(39.2,G4),(40.4,E4)]
for i,(tt,f) in enumerate(cont):
    add_tone(f, tt, 1.0, 0.022, 'sine', pan=(-0.12 if i%2==0 else 0.12), attack=0.01, decay=0.7)
for tt in [33.0,33.5,34.0,34.5,35.0,35.5,36.0,36.5]:
    add_tone(E5, tt, 0.08, 0.01, 'sine', pan=0.2, attack=0.002, decay=0.05)

# 41.8-46.8s measured lift
add_pad([F3, A3, C4, E4, A4], 41.6, 46.9, 0.055, attack=0.8, release=0.8)
for i,(tt,f) in enumerate([(42.2,C4),(43.0,E4),(43.8,G4),(44.6,A4),(45.4,C5),(46.0,A4)]):
    add_tone(f, tt, 1.2, 0.028, 'sine', pan=(-0.1+i*0.03), attack=0.01, decay=0.8)

# 46.8-52.0s resolve and fade
add_pad([A2, E3, A3, B3, E4], 46.6, 52.0, 0.05, attack=0.7, release=1.6)
for i,(tt,f) in enumerate([(47.2,A4),(48.0,E4),(48.8,B4),(49.6,A4),(50.4,E4)]):
    add_tone(f, tt, 1.6, 0.03, 'sine', pan=(-0.08+i*0.04), attack=0.02, decay=1.2)
add_tone(A3, 50.8, 1.2, 0.02, 'sine', pan=0.0, attack=0.03, decay=1.0)

# Global dynamics
for i in range(N):
    t = i/SR
    g = 1.0
    if 22.6 <= t < 23.35:
        if t < 22.9:
            g *= 1.0 - 0.97*((t-22.6)/0.3)
        else:
            g *= 0.03
    if 23.4 <= t < 23.6:
        g *= 1.0
    if 0.0 <= t < 22.6:
        g *= 0.92
    if 23.6 <= t < 41.8:
        g *= 0.82
    if 41.8 <= t < 46.8:
        g *= 0.9
    if t > 51.2:
        g *= max(0.0, (52.0 - t)/0.8)
    L[i] *= g
    R[i] *= g

peak = 0.0
for i in range(N):
    peak = max(peak, abs(L[i]), abs(R[i]))
scale = (0.92/peak) if peak > 1e-9 else 1.0
for i in range(N):
    L[i] = soft_clip(L[i]*scale)
    R[i] = soft_clip(R[i]*scale)

# ensure hit loudest
hit_s = int(23.4*SR); hit_e = int(24.1*SR)
global_peak = max((max(abs(L[i]),abs(R[i])) for i in range(N)), default=0.0)
hit_peak2 = max((max(abs(L[i]),abs(R[i])) for i in range(hit_s, min(hit_e,N))), default=0.0)
if hit_peak2 < global_peak*0.999 and hit_peak2 > 0:
    boost = (global_peak*0.999)/hit_peak2
    for i in range(hit_s, min(hit_e,N)):
        L[i] = max(-0.98,min(0.98,L[i]*boost))
        R[i] = max(-0.98,min(0.98,R[i]*boost))

out_dir = os.path.join(os.path.dirname(__file__))
out_path = os.path.join(out_dir, 'procedural_score.wav')
with wave.open(out_path, 'w') as wf:
    wf.setnchannels(2)
    wf.setsampwidth(2)
    wf.setframerate(SR)
    frames = bytearray()
    for i in range(N):
        ls = int(max(-1.0, min(1.0, L[i])) * 32767)
        rs = int(max(-1.0, min(1.0, R[i])) * 32767)
        frames += struct.pack('<hh', ls, rs)
    wf.writeframes(frames)

print(out_path)
