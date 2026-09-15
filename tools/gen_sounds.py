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


# ------------------------------------------------------------ still life
# A wooden-and-plaster mannequin: dry creaks, stone scrapes, cracking joints.
def creak(dur, seed, f0=180.0, f1=120.0, gain=0.6):
    """A slow bowing-wood creak: filtered noise gated by a wavering pitch."""
    r = random.Random(seed)
    tt = t(dur)
    wob = np.array([r.uniform(-1, 1) for _ in range(len(tt))])
    alpha = 0.15 / (0.15 + 1.0 / SR)
    acc, wobf = 0.0, np.empty_like(tt)
    for i, v in enumerate(wob):
        acc += alpha * (v - acc)
        wobf[i] = acc
    freq = f0 + (f1 - f0) * (tt / dur) + 60.0 * wobf
    phase = 2 * math.pi * np.cumsum(freq) / SR
    # resonating body: two detuned saws (wooden harmonic ring)
    sig = 0.5 * saw(phase / (2 * math.pi)) + 0.35 * saw(phase / math.pi)
    sig += 0.25 * lp_noise(dur, seed=seed + 9, smooth=0.003)
    # slow irregular gate, like timber settling in short bursts
    chatter = 0.55 + 0.45 * np.sin(2 * math.pi * (3.0 + 2.0 * wobf) * tt)
    chatter = np.clip(chatter, 0.0, 1.0)
    sig *= chatter
    env = np.minimum(tt / 0.35, np.ones_like(tt))
    env *= np.clip((dur - tt) / 0.5, 0, 1) ** 0.6
    sig *= env
    sig /= max(1e-9, np.max(np.abs(sig)))
    return sig * gain


def scrape(dur, seed, gain=0.7):
    """Stone dragged on tile: band-passed-ish noise with slow stroke swells."""
    r = random.Random(seed)
    tt = t(dur)
    n = lp_noise(dur, seed=seed, smooth=0.0016, gain=1.0)
    strokes = 0.35 + 0.65 * np.clip(
        np.sin(2 * math.pi * r.uniform(1.5, 3.0) * tt + r.random()), 0, 1) ** 2
    sig = n * strokes
    # faint low grind underneath
    sig += 0.3 * lp_noise(dur, seed=seed + 4, smooth=0.03)
    env = np.minimum(tt / 0.15, np.ones_like(tt))
    env *= np.clip((dur - tt) / 0.3, 0, 1)
    sig *= env
    sig /= max(1e-9, np.max(np.abs(sig)))
    return sig * gain


def still_life_ambient():
    # Most of the time it is silent; when heard, a long distant creak-scrape.
    sig = 0.5 * creak(2.4, seed=121, f0=210, f1=150, gain=0.5)
    sig += 0.35 * scrape(2.4, seed=122, gain=0.35)
    sig /= max(1e-9, np.max(np.abs(sig)))
    sig *= 0.55
    write_ogg("still_life/ambient", sig)


def still_life_hurt():
    dur = 0.5
    tt = t(dur)
    # A sharp plaster/wood crack: two quick creak bursts + noise crack.
    sig = creak(dur, seed=131, f0=320, f1=180, gain=0.9)
    crack = lp_noise(dur, seed=132, smooth=0.0008)
    gate = np.exp(-tt * 18.0)
    sig += 0.7 * crack * gate
    sig /= max(1e-9, np.max(np.abs(sig)))
    sig *= 0.85
    write_ogg("still_life/hurt", sig)


def still_life_death():
    dur = 2.4
    tt = t(dur)
    # It topples: a long stone scrape ending in a heavy wooden crash.
    sig = 0.6 * scrape(1.8, seed=141, gain=0.6)
    crash_noise = lp_noise(dur, seed=142, smooth=0.0025)
    crash_env = np.clip((tt - 1.5) / 0.05, 0, 1) * np.clip((dur - tt) / 0.7, 0, 1)
    sig = np.pad(sig, (0, len(tt) - len(sig)), mode="constant")
    sig += 0.9 * crash_noise * crash_env
    sig += 0.5 * creak(dur, seed=143, f0=260, f1=70, gain=0.5) * crash_env
    sig /= max(1e-9, np.max(np.abs(sig)))
    sig *= 0.9
    write_ogg("still_life/death", sig)


hum()
still_life_ambient()
still_life_hurt()
still_life_death()
print("done")
