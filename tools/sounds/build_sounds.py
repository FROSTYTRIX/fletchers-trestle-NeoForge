#!/usr/bin/env python3
"""
Cuts the mod's sounds out of public-domain and CC0 recordings.

Every clip below names its source file, the stretch of it to keep, and how to
filter it. The script trims each one, fades its ends, filters out rumble or
hiss, levels its peak to -1 dBFS and writes a mono OGG Vorbis file (mono so
Minecraft positions it in the world) under assets/fletcherstrestle/sounds/.
Loudness and pitch per event live in sounds.json, not here.

The recordings aren't kept in the repo. Download them into one folder, laid
out as below, and point --sources at it:

    eagle/      Wikimedia Commons, National Park Service, Yellowstone sound
                library (public domain, a US federal work):
                Bald_Eagle_Yellowstone_National_Park.ogg
                Yellowstone_sound_library_-_Bald_Eagle_-_001.mp3
                Yellowstone_sound_library_-_Bald_Eagle_-_003.mp3
    medieval/x/ OpenGameArt, "Medieval sound effects - Weapon Textures" by
                Still North Media (CC0), both .7z parts extracted
    bart/       OpenGameArt, by bart (CC0): ticks.zip and steam_hisses.zip,
                extracted to ticks/ and steam_hisses/
    woodmetal/  OpenGameArt, "100 CC0 metal and wood SFX" by rubberduck (CC0),
                extracted to 100-CC0-wood-metal-SFX/
    kenney/     OpenGameArt, "50 RPG sound effects" by Kenney (CC0), extracted
                to RPGsounds_Kenney/

Needs ffmpeg and oggenc (Homebrew: ffmpeg, vorbis-tools).

    python3 tools/sounds/build_sounds.py --sources ~/Downloads/ft-sounds
"""

import argparse
import pathlib
import re
import subprocess
import tempfile

REPO = pathlib.Path(__file__).resolve().parent.parent.parent
OUT = REPO / "src/main/resources/assets/fletcherstrestle/sounds"

EAGLE_CLIP = 'eagle/Bald_Eagle_Yellowstone_National_Park.ogg'
EAGLE_LONG = 'eagle/Yellowstone_sound_library_-_Bald_Eagle_-_001.mp3'
EAGLE_SHORT = 'eagle/Yellowstone_sound_library_-_Bald_Eagle_-_003.mp3'
M = 'medieval/x/'
WOOD = 'woodmetal/100-CC0-wood-metal-SFX/'
KENNEY = 'kenney/RPGsounds_Kenney/OGG/'

# Band limits that keep an eagle's call (about 1.5 to 4.5 kHz, with harmonics)
# and drop the river and wind under it.
EAGLE_BAND = {'highpass': 900, 'lowpass': 7500}
# Studio recordings: just take off the low rumble.
CLEAN = {'highpass': 70}

# (output, source, start, end, filters). A None end keeps the whole file.
CLIPS = [
    # Calls: bouts of the bald eagle's chatter.
    ('eagle/call_1', EAGLE_CLIP, 0.10, 2.10, EAGLE_BAND),
    ('eagle/call_2', EAGLE_LONG, 54.05, 55.95, EAGLE_BAND),
    ('eagle/call_3', EAGLE_LONG, 62.60, 64.30, EAGLE_BAND),
    ('eagle/call_4', EAGLE_LONG, 129.95, 131.40, EAGLE_BAND),
    ('eagle/call_5', EAGLE_LONG, 41.40, 42.55, EAGLE_BAND),
    ('eagle/call_6', EAGLE_SHORT, 5.90, 6.70, EAGLE_BAND),
    # Single sharp notes, for when it's hurt.
    ('eagle/cry_1', EAGLE_CLIP, 0.12, 0.42, EAGLE_BAND),
    ('eagle/cry_2', EAGLE_LONG, 37.35, 37.85, EAGLE_BAND),
    ('eagle/cry_3', EAGLE_LONG, 42.05, 42.55, EAGLE_BAND),
    # A long chatter that trails off, for its death (pitched down in sounds.json).
    ('eagle/death_1', EAGLE_LONG, 72.00, 73.70, EAGLE_BAND),
    # A short, bright run of notes, for taming.
    ('eagle/tame_1', EAGLE_LONG, 56.40, 57.35, EAGLE_BAND),
    # A heavy arrow's passby, slowed in sounds.json into the rush of a stoop.
    ('eagle/dive_1', M + 'Scythian Recurve Heavy Arrow Passby.wav', 0.05, 3.05, CLEAN),
    # Cloth snapping in the air, slowed into wingbeats.
    ('eagle/flap_1', KENNEY + 'cloth1.ogg', 0.0, None, {}),
    ('eagle/flap_2', KENNEY + 'cloth2.ogg', 0.0, None, {}),
    ('eagle/flap_3', KENNEY + 'cloth3.ogg', 0.0, None, {}),
    ('eagle/flap_4', KENNEY + 'cloth4.ogg', 0.0, None, {}),

    # Drawing a bow: the stave and string creaking under load.
    ('bow/draw_1', M + 'English Longbow Draw.wav', 9.33, 10.25, CLEAN),
    ('bow/draw_2', M + 'English Longbow Draw.wav', 14.10, 14.90, CLEAN),
    ('bow/draw_3', M + 'English Longbow Draw.wav', 20.29, 21.10, CLEAN),
    ('bow/draw_4', M + 'Scythian Recurve Draw.wav', 2.54, 3.67, CLEAN),
    ('bow/draw_5', M + 'Scythian Recurve Draw.wav', 6.95, 7.96, CLEAN),
    # The arrow seating on the string at full draw.
    ('bow/nock_1', M + 'Scythian Recurve Nock Arrow.wav', 0.38, 0.62, CLEAN),
    ('bow/nock_2', M + 'Scythian Recurve Nock Arrow.wav', 1.48, 1.72, CLEAN),
    ('bow/nock_3', M + 'Scythian Recurve Nock Arrow.wav', 2.55, 2.82, CLEAN),
    # Releases: the longbow's deeper thrum, the recurve's snap, a crossbow's crack.
    ('bow/longbow_shoot_1', M + 'English Longbow Shoot.wav', 0.10, 0.43, CLEAN),
    ('bow/longbow_shoot_2', M + 'English Longbow Shoot.wav', 1.25, 1.70, CLEAN),
    ('bow/longbow_shoot_3', M + 'English Longbow Shoot.wav', 2.08, 2.53, CLEAN),
    ('bow/longbow_shoot_4', M + 'English Longbow Shoot.wav', 3.20, 3.65, CLEAN),
    ('bow/recurve_shoot_1', M + 'Scythian Recurve Shoot.wav', 0.21, 0.66, CLEAN),
    ('bow/recurve_shoot_2', M + 'Scythian Recurve Shoot.wav', 1.90, 2.35, CLEAN),
    ('bow/recurve_shoot_3', M + 'Scythian Recurve Shoot.wav', 2.95, 3.38, CLEAN),
    ('bow/recurve_shoot_4', M + 'Scythian Recurve Shoot.wav', 3.40, 3.82, CLEAN),
    ('bow/crossbow_shoot_1', M + 'Crossbow Shoot.wav', 0.32, 0.80, CLEAN),
    ('bow/crossbow_shoot_2', M + 'Crossbow Shoot.wav', 1.41, 1.90, CLEAN),
    ('bow/crossbow_shoot_3', M + 'Crossbow Shoot.wav', 2.49, 2.95, CLEAN),

    # The Bolt Warden's clockwork.
    ('garrison/tick_1', 'bart/ticks/ticking clock - tick1.wav', 0.0, None, {}),
    ('garrison/tick_2', 'bart/ticks/ticking clock - tick2.wav', 0.0, None, {}),
    ('garrison/tick_3', 'bart/ticks/ticking clock - tick3.wav', 0.0, None, {}),
    ('garrison/tick_4', 'bart/ticks/ticking clock - tick4.wav', 0.0, None, {}),
    # Winding up: a crossbow's nut being ratcheted back against its sear.
    ('garrison/wind_up_1', M + 'Crossbow Set Nut Against Sear.wav', 0.15, 2.40, CLEAN),
    # Single ratchet clicks, one per turn of the reload crank.
    ('garrison/crank_1', M + 'Crossbow Set Nut Against Sear.wav', 0.16, 0.38, CLEAN),
    ('garrison/crank_2', M + 'Crossbow Set Nut Against Sear.wav', 0.39, 0.62, CLEAN),
    ('garrison/crank_3', M + 'Crossbow Set Nut Against Sear.wav', 0.63, 0.89, CLEAN),
    ('garrison/crank_4', M + 'Crossbow Set Nut Against Sear.wav', 3.93, 4.20, CLEAN),
    # A trigger worked on an empty crossbow.
    ('garrison/empty_1', M + 'Crossbow Lever Trigger.wav', 1.21, 1.48, CLEAN),
    ('garrison/empty_2', M + 'Crossbow Lever Trigger.wav', 2.27, 2.55, CLEAN),
    ('garrison/empty_3', M + 'Crossbow Lever Trigger.wav', 4.76, 5.00, CLEAN),
    ('garrison/repair_1', WOOD + 'wood_hammer_01.ogg', 0.0, None, {}),
    ('garrison/repair_2', WOOD + 'wood_hammer_02.ogg', 0.0, None, {}),
    ('garrison/crack_1', WOOD + 'wood_cracking_01.ogg', 0.0, None, {}),
    ('garrison/crack_2', WOOD + 'wood_cracking_02.ogg', 0.0, None, {}),
    ('garrison/crack_3', WOOD + 'wood_cracking_03.ogg', 0.0, None, {}),
    ('garrison/crack_4', WOOD + 'wood_cracking_04.ogg', 0.0, None, {}),

    # The Steam Box letting off steam: short bursts, it hisses often.
    ('workshop/steam_hiss_1', 'bart/steam_hisses/steam hisses - Marker #1.wav', 0.0, 1.6, {}),
    ('workshop/steam_hiss_2', 'bart/steam_hisses/steam hisses - Marker #2.wav', 0.0, 1.6, {}),
    ('workshop/steam_hiss_3', 'bart/steam_hisses/steam hisses - Marker #3.wav', 0.0, 1.6, {}),
    ('workshop/steam_hiss_4', 'bart/steam_hisses/steam hisses - Marker #4.wav', 0.0, 1.6, {}),
    ('workshop/steam_hiss_5', 'bart/steam_hisses/steam hisses - Marker #5.wav', 0.0, 1.6, {}),
]

FADE_IN = 0.004
FADE_OUT = 0.08


def duration(path):
    out = subprocess.run(['ffprobe', '-v', 'error', '-show_entries', 'format=duration', '-of', 'csv=p=0', str(path)],
                         capture_output=True, text=True, check=True).stdout
    return float(out.strip())


def peak_db(path):
    out = subprocess.run(['ffmpeg', '-hide_banner', '-i', str(path), '-af', 'volumedetect', '-f', 'null', '-'],
                         capture_output=True, text=True).stderr
    return float(re.search(r'max_volume: (-?[\d.]+) dB', out).group(1))


def build(name, source, start, end, filters, sources, work):
    src = sources / source
    if not src.exists():
        raise SystemExit('missing source: ' + str(src))
    end = duration(src) if end is None else end
    length = end - start
    chain = []
    if 'highpass' in filters:
        chain.append('highpass=f=%d' % filters['highpass'])
    if 'lowpass' in filters:
        chain.append('lowpass=f=%d' % filters['lowpass'])
    fade_out = min(FADE_OUT, length / 3)
    chain.append('afade=t=in:d=%.3f' % FADE_IN)
    chain.append('afade=t=out:st=%.3f:d=%.3f' % (length - fade_out, fade_out))
    raw = work / (name.replace('/', '_') + '_raw.wav')
    subprocess.run(['ffmpeg', '-hide_banner', '-loglevel', 'error', '-y', '-ss', '%.3f' % start, '-t', '%.3f' % length,
                    '-i', str(src), '-ac', '1', '-ar', '44100', '-af', ','.join(chain), '-c:a', 'pcm_s16le', str(raw)],
                   check=True)
    level = work / (name.replace('/', '_') + '.wav')
    gain = -1.0 - peak_db(raw)
    subprocess.run(['ffmpeg', '-hide_banner', '-loglevel', 'error', '-y', '-i', str(raw), '-af', 'volume=%.2fdB' % gain,
                    '-c:a', 'pcm_s16le', str(level)], check=True)
    out = OUT / (name + '.ogg')
    out.parent.mkdir(parents=True, exist_ok=True)
    subprocess.run(['oggenc', '--quiet', '-q', '6', '-o', str(out), str(level)], check=True)
    return out, length


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument('--sources', type=pathlib.Path, required=True, help='folder holding the downloaded recordings')
    args = ap.parse_args()
    with tempfile.TemporaryDirectory() as tmp:
        for clip in CLIPS:
            out, length = build(*clip, args.sources.expanduser(), pathlib.Path(tmp))
            print('%-28s %5.2fs  %6.1f KB' % (clip[0], length, out.stat().st_size / 1024))


if __name__ == '__main__':
    main()
