#!/usr/bin/env python3
"""
Halve a weapon's limb textures so one painted half serves as both limbs.

A composite bow draws its limb texture twice, the second copy flipped about the
sprite's diagonal, so one painted half serves as both limbs. That only looks
right if the texture really is one half: a full limb drawn twice renders
doubled. This clears the diagonal and everything under it, keeping the upper
half (the pixels where x > y).

Idempotent: it only ever clears pixels, so running it twice changes nothing,
and a texture already halved by hand is left alone.

Two ways to decide what to keep:

  --diagonal (default) clears the diagonal and everything under it. This is
  the bow's rule, and it matches the bow textures halved by hand.

  --mask-from FILE keeps only the pixels that FILE keeps, whatever shape that
  is. Use it when the cut is not a clean diagonal: the crossbow's silhouette
  includes a stock that has to go entirely, and no rule describes that as well
  as the hand-edited texture already does.

    python3 tools/halve_limb_textures.py --dry-run
    python3 tools/halve_limb_textures.py --dir DIR --mask-from DIR/oak_limb.png
"""

import argparse
import datetime
import pathlib
import shutil
import sys

from PIL import Image

REPO = pathlib.Path(__file__).resolve().parent.parent
LIMBS = REPO / "src/main/resources/assets/fletcherstrestle/textures/item/modular_bow/limbs"
CLEAR = (0, 0, 0, 0)


def read_mask(path):
    """The set of pixels a reference texture keeps."""
    image = Image.open(path).convert("RGBA")
    width, height = image.size
    pixels = image.load()
    return {(x, y) for y in range(height) for x in range(width) if pixels[x, y][3] != 0}


def halve(path, dry_run, mask=None):
    """Clear whatever this texture should not keep. Returns the pixels cleared."""
    image = Image.open(path).convert("RGBA")
    width, height = image.size
    pixels = image.load()

    cleared = 0
    for y in range(height):
        for x in range(width):
            if mask is None:
                # x * height <= y * width is "on or under the diagonal",
                # written so it still holds for a non-square sprite.
                drop = x * height <= y * width
            else:
                drop = (x, y) not in mask
            if drop and pixels[x, y][3] != 0:
                pixels[x, y] = CLEAR
                cleared += 1

    if cleared and not dry_run:
        image.save(path)
    return cleared


def main():
    parser = argparse.ArgumentParser(description=__doc__,
                                     formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--dir", type=pathlib.Path, default=LIMBS,
                        help="directory of limb textures (default: the modular bow's)")
    parser.add_argument("--dry-run", action="store_true",
                        help="report what would change without writing")
    parser.add_argument("--no-backup", action="store_true",
                        help="skip copying the originals aside first")
    parser.add_argument("--mask-from", type=pathlib.Path, default=None,
                        help="keep only the pixels this texture keeps, instead "
                             "of cutting along the diagonal")
    args = parser.parse_args()

    mask = read_mask(args.mask_from) if args.mask_from else None
    if mask is not None:
        print(f"Keeping the shape of {args.mask_from.name} ({len(mask)} px)\n")

    textures = sorted(args.dir.glob("*.png"))
    if not textures:
        sys.exit(f"No PNGs in {args.dir}")

    # Backups live under build/, which is gitignored: anything left inside
    # assets/ would be packed into the jar as a stray texture.
    if not args.dry_run and not args.no_backup:
        stamp = datetime.datetime.now().strftime("%Y%m%d-%H%M%S")
        backup = REPO / "build" / f"limb-texture-backup-{stamp}"
        backup.mkdir(parents=True, exist_ok=True)
        for texture in textures:
            shutil.copy2(texture, backup / texture.name)
        print(f"Originals copied to {backup.relative_to(REPO)}\n")

    total = 0
    for texture in textures:
        # An animated texture is a vertical strip of frames, so the diagonal of
        # the file is not the diagonal of the sprite.
        if texture.with_suffix(".png.mcmeta").exists():
            print(f"  skipped  {texture.name}  (animated)")
            continue
        cleared = halve(texture, args.dry_run, mask)
        total += cleared
        state = "already halved" if cleared == 0 else f"{cleared} px cleared"
        print(f"  {'would clear' if args.dry_run and cleared else 'ok         '}  {texture.name}  ({state})")

    verb = "would be cleared" if args.dry_run else "cleared"
    print(f"\n{total} px {verb} across {len(textures)} textures.")


if __name__ == "__main__":
    main()
