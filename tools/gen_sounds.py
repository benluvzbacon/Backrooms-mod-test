#!/usr/bin/env python3
"""
Synthesizes all sounds for the mod (no external/copyrighted audio).
Writes OGG Vorbis into assets/backrooms/sounds/.
Requires: pip install imageio-ffmpeg numpy
"""
import math
import os
import random
import subprocess
import wave

import numpy as np
import imageio_ffmpeg

ROOT = os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources", "assets", "backrooms", "sounds")
os.makedirs(ROOT, exist_ok=True)
SR = 44100


def write_ogg(name, samples):
    samples = np.clip(samples, -1.0, 1.0)
    pcm = (samples * 32767).astype(np.int16)
    wav_path = os.path.join("/tmp", name.replace("/", "_") + ".wav")
    ogg_path = os.path.join(ROOT, name + ".ogg")
    os.makedirs(os.path.dirname(ogg_path), exist_ok=True)
    with wave.open(wav_path, "wb") as w:
        w.setnchannels(1)
        w.setsampwidth(2)
        w.setframerate(SR)
        w.writeframes(pcm.tobytes())
    ff = imageio_ffmpeg.get_ffmpeg_exe()
    subprocess.run([ff, "-y", "-loglevel", "error", "-i", wav_path,
                    "-c:a", "libvorbis", "-q:a", "4", ogg_path], check=True)
    print("wrote", os.path.relpath(ogg_path, ROOT))


def t(dur):
    return np.linspace(0, dur, int(SR * dur), endpoint=False)


def lp_noise(dur, seed, smooth=0.02, gain=1.0):
    """Very simple low-passed white noise (one-pole filter)."""
    r = random.Random(seed)
    x = np.array([r.uniform(-1, 1) for _ in range(int(SR * dur))])
    alpha = smooth / (smooth + 1.0 / SR)
    y = np.empty_like(x)
    acc = 0.0
    for i, v in enumerate(x):
        acc += alpha * (v - acc)
        y[i] = acc
    return y * gain


def saw(x):
    return 2.0 * (x - np.floor(x + 0.5))


# -------------------------------------------------------------------- hum
def hum():
    dur = 8.0
    tt = t(dur)
    # fluorescent-ballast room tone: two low sines + faint 100/120 Hz electrical hum
    sig = (
        0.55 * np.sin(2 * math.pi * 50 * tt)
        + 0.28 * np.sin(2 * math.pi * 100 * tt)
        + 0.10 * np.sin(2 * math.pi * 120 * tt)
    )
    rumble = lp_noise(dur, seed=7, smooth=0.08, gain=0.9)
    sig += 0.16 * rumble
    # faint high whine of the tubes
    sig += 0.015 * np.sin(2 * math.pi * 7200 * tt)
    # slow amplitude breathing (integer cycles over the loop -> seamless)
    sig *= 0.85 + 0.15 * np.sin(2 * math.pi * (1.0 / dur) * tt)
    # seamless crossfade of the noise component region
    xf = int(0.5 * SR)
    fade = np.linspace(0, 1, xf)
    sig[:xf] *= fade
    sig[:xf] += sig[-xf:] * (1 - fade)
    sig = sig[:-xf] if False else sig
    sig /= max(1e-9, np.max(np.abs(sig)))
    # Kept deliberately quiet: this loops continuously as biome ambience.
    sig *= 0.22
    write_ogg("ambient/hum", sig)


# ------------------------------------------------------------- bacteria
def growl(dur, seed, f0, f1, gurgle=6.0):
    r = random.Random(seed)
    tt = t(dur)
    base = f0 + (f1 - f0) * (tt / dur) ** 1.2
    # slow random pitch wobble (organic)
    wob = np.array([r.uniform(-1, 1) for _ in range(len(tt))])
    alpha = 0.25 / (0.25 + 1.0 / SR)
    acc, wobf = 0.0, np.empty_like(tt)
    for i, v in enumerate(wob):
        acc += alpha * (v - acc)
        wobf[i] = acc
    phase = 2 * math.pi * np.cumsum(base * (1.0 + 0.12 * wobf) + gurgle * wobf) / SR
    # raspy saw formant + sub sine
    sig = 0.55 * saw(phase / (2 * math.pi)) + 0.6 * np.sin(phase * 0.5)
    sig += 0.18 * lp_noise(dur, seed=seed + 3, smooth=0.004)
    # envelope: swell in, moan, decay
    env = np.minimum(tt / 0.25, np.ones_like(tt))
    env *= np.clip((dur - tt) / 0.4, 0, 1) ** 0.5
    sig *= env
    sig /= max(1e-9, np.max(np.abs(sig)))
    return sig


def bacteria_ambient():
    sig = growl(2.1, seed=21, f0=62, f1=48, gurgle=7)
    sig *= 0.75
    write_ogg("bacteria/ambient", sig)


def bacteria_hurt():
    dur = 0.45
    tt = t(dur)
    r = random.Random(44)
    base = 150 + 60 * np.sin(2 * math.pi * 9 * tt)
    phase = 2 * math.pi * np.cumsum(base) / SR
    sig = 0.7 * saw(phase / (2 * math.pi)) + 0.25 * np.array(
        [r.uniform(-1, 1) for _ in range(len(tt))])
    env = np.clip((dur - tt) / dur, 0, 1) ** 1.5
    sig *= env
    sig /= max(1e-9, np.max(np.abs(sig)))
    sig *= 0.8
    write_ogg("bacteria/hurt", sig)


def bacteria_death():
    sig = growl(2.6, seed=77, f0=90, f1=34, gurgle=11)
    # gurgling rattles at the end
    tt = t(2.6)
    rattle = 0.15 * lp_noise(2.6, seed=78, smooth=0.002)
    gate = (np.sin(2 * math.pi * 14 * tt) > 0.2).astype(float)
    sig += rattle * gate * np.clip(tt - 0.8, 0, 1)
    sig /= max(1e-9, np.max(np.abs(sig)))
    sig *= 0.85
    write_ogg("bacteria/death", sig)


hum()
bacteria_ambient()
bacteria_hurt()
bacteria_death()
print("done")
