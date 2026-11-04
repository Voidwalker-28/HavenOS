#!/usr/bin/env python3
"""HavenOS 3.1 Phase E (E1) — original pixel-art sprite generator.

Draws Milo and Mila as 24x24-cell pixel sprites, one PNG sheet per
character plus a shared JSON atlas, and a review preview image showing
every frame at 4x on a gray AND a dark background.

Originality: both characters are plain blobs with a tuft / ribbon.
No red cap, no mustache, no overalls, no borrowed assets or sounds.
Accent-detail pixels (tuft tip, ribbon knot) are painted in the
sentinel magenta (255, 0, 255); the app replaces that color at load
with the current accent, so the mascot follows the accent.

Frame layout (shared, 41 frames, row-major sheet 41 x 24 px):
  idle 0-3, walk 4-9, jump 10-14, sit 15-16, sleep 17-19, wave 20-23,
  happy 24-27, surprised 28-29, sad 30-31, think 32-34, point 35-36,
  poof 37-40.
"""

import json
import os

from PIL import Image, ImageDraw, ImageFont

HERE = os.path.dirname(os.path.abspath(__file__))
RES = os.path.join(HERE, "..", "..", "app", "src", "main", "res")

# Palette keys: K outline, B base, S shade, H highlight, E eye,
# L blush, A accent sentinel, W white, . transparent.
PALETTES = {
    "milo": {
        "K": (32, 32, 32, 255),
        "B": (233, 180, 76, 255),
        "S": (198, 143, 46, 255),
        "H": (247, 220, 138, 255),
        "E": (31, 31, 31, 255),
        "L": (232, 138, 122, 255),
        "A": (255, 0, 255, 255),
        "W": (255, 255, 255, 255),
    },
    "mila": {
        "K": (32, 32, 32, 255),
        "B": (232, 155, 180, 255),
        "S": (201, 122, 147, 255),
        "H": (246, 201, 216, 255),
        "E": (31, 31, 31, 255),
        "L": (217, 106, 126, 255),
        "A": (255, 0, 255, 255),
        "W": (255, 255, 255, 255),
    },
}

ANIMS = [
    ("idle", 0, 4, 4),
    ("walk", 4, 6, 10),
    ("jump", 10, 5, 8),
    ("sit", 15, 2, 2),
    ("sleep", 17, 3, 1),
    ("wave", 20, 4, 6),
    ("happy", 24, 4, 4),
    ("surprised", 28, 2, 2),
    ("sad", 30, 2, 2),
    ("think", 32, 3, 2),
    ("point", 35, 2, 2),
    ("poof", 37, 4, 6),
]


class Grid:
    """24x24 pixel grid addressed as g[x, y] = palette key."""

    def __init__(self):
        self.p = {}

    def put(self, x, y, k):
        if 0 <= x < 24 and 0 <= y < 24:
            self.p[(x, y)] = k

    def rect(self, x0, y0, x1, y1, k):
        for y in range(y0, y1 + 1):
            for x in range(x0, x1 + 1):
                self.put(x, y, k)

    def copy(self):
        g = Grid()
        g.p = dict(self.p)
        return g


def body_mask_milo():
    """Squarish blob rows: y -> (x0, x1)."""
    rows = {}
    rows[5] = (6, 17)
    for y in range(6, 20):
        rows[y] = (4, 19)
    rows[20] = (6, 17)
    return rows


def body_mask_mila():
    """Rounder blob rows: y -> (x0, x1)."""
    rows = {
        4: (8, 15),
        5: (6, 17),
        6: (5, 18),
        7: (4, 19),
    }
    for y in range(8, 18):
        rows[y] = (4, 19)
    rows[18] = (5, 18)
    rows[19] = (6, 17)
    rows[20] = (7, 16)
    return rows


def paint_body(g, char, dy=0, squash=False):
    """Paint body + feet + outline. dy shifts the whole figure vertically.
    squash drops the bottom body row (landing frame)."""
    rows = body_mask_milo() if char == "milo" else body_mask_mila()
    if squash:
        rows = {y: r for y, r in rows.items() if y < 20}
    mask = set()
    for y, (x0, x1) in rows.items():
        for x in range(x0, x1 + 1):
            mask.add((x, y + dy))
    # Feet: two small blocks under the body, part of the mask so the
    # outline wraps them too.
    for fx in (7, 8, 9):
        mask.add((fx, 21 + dy))
    for fx in (14, 15, 16):
        mask.add((fx, 21 + dy))
    for (x, y) in mask:
        g.put(x, y, "B")
    # 1px dark outline: any body pixel with a 4-neighbour outside mask.
    for (x, y) in list(mask):
        if ((x - 1, y) not in mask or (x + 1, y) not in mask or
                (x, y - 1) not in mask or (x, y + 1) not in mask):
            g.put(x, y, "K")
    # Shade: bottom body row + right column; highlight: top-left patch.
    for (x, y) in mask:
        if y + dy >= 19 and g.p.get((x, y)) == "B":
            g.put(x, y, "S")
    for (x, y) in mask:
        if x >= 18 and g.p.get((x, y)) == "B":
            g.put(x, y, "S")
    for (x, y) in [(6, 7), (7, 6), (7, 7)]:
        if (x, y + dy) in mask:
            g.put(x, y + dy, "H")
    # Blush: two soft cheek patches.
    for (x, y) in [(6, 14), (7, 14), (16, 14), (17, 14)]:
        if (x, y + dy) in mask:
            g.put(x, y + dy, "L")
    return mask


def paint_tuft(g):
    g.put(11, 3, "S")
    g.put(12, 2, "A")  # tuft tip: accent-swapped at load
    g.put(12, 3, "B")
    g.put(13, 3, "S")


def paint_ribbon(g):
    g.put(15, 3, "S")
    g.put(16, 2, "B")
    g.put(18, 2, "B")
    g.put(19, 3, "S")
    g.put(17, 3, "A")  # ribbon knot: accent-swapped at load
    g.put(16, 4, "S")
    g.put(19, 4, "S")


def eyes(g, style, dy=0):
    """Eye styles at x 8-9 (left) and 14-15 (right), base row y 11."""
    y = 11 + dy
    if style == "open":
        g.rect(8, y, 9, y + 2, "E")
        g.rect(14, y, 14 + 1, y + 2, "E")
    elif style == "blink":
        g.rect(8, y + 1, 9, y + 1, "E")
        g.rect(14, y + 1, 15, y + 1, "E")
    elif style == "happy":
        g.put(8, y + 1, "E")
        g.put(9, y, "E")
        g.put(14, y, "E")
        g.put(15, y + 1, "E")
    elif style == "surprised":
        g.rect(8, y, 9, y + 2, "E")
        g.rect(14, y, 15, y + 2, "E")
        g.put(8, y, "W")
        g.put(14, y, "W")
    elif style == "sad":
        g.rect(8, y, 9, y + 1, "E")
        g.rect(14, y + 1, 15, y + 2, "E")
    elif style == "sleep":
        g.rect(8, y + 1, 9, y + 1, "E")
        g.rect(14, y + 1, 15, y + 1, "E")
    elif style == "think":
        g.rect(8, y, 9, y + 1, "E")
        g.rect(14, y, 15, y + 1, "E")
    elif style == "right":
        g.put(8, y, "W")
        g.put(9, y + 1, "E")
        g.put(14, y, "W")
        g.put(15, y + 1, "E")


def mouth(g, style, dy=0):
    y = 16 + dy
    if style == "line":
        g.put(11, y, "E")
        g.put(12, y, "E")
    elif style == "smile":
        g.put(10, y, "E")
        g.put(11, y + 1, "E")
        g.put(12, y + 1, "E")
        g.put(13, y, "E")
    elif style == "frown":
        g.put(10, y + 1, "E")
        g.put(11, y, "E")
        g.put(12, y, "E")
        g.put(13, y + 1, "E")
    elif style == "o":
        g.rect(11, y, 12, y + 1, "E")


def base_frame(char, eye="open", mth="line", dy=0, squash=False):
    g = Grid()
    paint_body(g, char, dy=dy, squash=squash)
    if char == "milo":
        paint_tuft(g)
    else:
        paint_ribbon(g)
    eyes(g, eye, dy=dy)
    mouth(g, mth, dy=dy)
    return g


def walk_frame(char, step):
    """6 walk frames: feet alternate, body bobs 0/-1."""
    g = Grid()
    # Feet positions per step: (left foot y, right foot y, body dy).
    cycle = [(21, 22, 0), (21, 21, -1), (22, 21, 0),
             (22, 21, -1), (21, 22, 0), (21, 21, 0)]
    ly, ry, dy = cycle[step % 6]
    rows = body_mask_milo() if char == "milo" else body_mask_mila()
    mask = set()
    for y, (x0, x1) in rows.items():
        for x in range(x0, x1 + 1):
            mask.add((x, y + dy))
    for fx in (7, 8, 9):
        mask.add((fx, ly + dy))
    for fx in (14, 15, 16):
        mask.add((fx, ry + dy))
    for (x, y) in mask:
        g.put(x, y, "B")
    for (x, y) in list(mask):
        if ((x - 1, y) not in mask or (x + 1, y) not in mask or
                (x, y - 1) not in mask or (x, y + 1) not in mask):
            g.put(x, y, "K")
    for (x, y) in mask:
        if y >= 19 + dy and g.p.get((x, y)) == "B":
            g.put(x, y, "S")
    for (x, y) in mask:
        if x >= 18 and g.p.get((x, y)) == "B":
            g.put(x, y, "S")
    if char == "milo":
        paint_tuft(g)
    else:
        paint_ribbon(g)
    eyes(g, "open", dy=dy)
    mouth(g, "line", dy=dy)
    return g


def cloud(g, pixels):
    for (x, y) in pixels:
        g.put(x, y, "W")


def build_frames(char):
    frames = []
    # idle (4): neutral, neutral, blink, neutral.
    frames.append(base_frame(char))
    frames.append(base_frame(char))
    frames.append(base_frame(char, eye="blink"))
    frames.append(base_frame(char))
    # walk (6).
    for i in range(6):
        frames.append(walk_frame(char, i))
    # jump (5): rise, apex, fall, land (squash), land (normal).
    frames.append(base_frame(char, dy=-2))
    frames.append(base_frame(char, dy=-3))
    frames.append(base_frame(char, dy=-1))
    frames.append(base_frame(char, dy=1, squash=True))
    frames.append(base_frame(char))
    # sit (2): lowered body + flat feet, second frame blinks.
    g = base_frame(char, dy=2)
    g.put(6, 22, "S")
    g.put(17, 22, "S")
    frames.append(g)
    g = base_frame(char, eye="blink", dy=2)
    g.put(6, 22, "S")
    g.put(17, 22, "S")
    frames.append(g)
    # sleep (3): closed eyes + rising Z.
    for i, z in enumerate([[(18, 4)], [(19, 3), (18, 4)], [(20, 2), (19, 3), (18, 4)]]):
        g = base_frame(char, eye="sleep", mth="line")
        for (x, y) in z:
            g.put(x, y, "W")
        frames.append(g)
    # wave (4): right arm raised, alternating height, happy eyes.
    for ay in (8, 9, 10, 9):
        g = base_frame(char, eye="happy", mth="smile")
        g.put(20, ay, "B")
        g.put(21, ay, "B")
        g.put(21, ay - 1, "K")
        frames.append(g)
    # happy (4): happy eyes + smile, gentle bob.
    frames.append(base_frame(char, eye="happy", mth="smile"))
    frames.append(base_frame(char, eye="happy", mth="smile", dy=-1))
    frames.append(base_frame(char, eye="happy", mth="smile"))
    frames.append(base_frame(char, eye="happy", mth="smile", dy=-1))
    # surprised (2): big eyes + o mouth.
    frames.append(base_frame(char, eye="surprised", mth="o"))
    frames.append(base_frame(char, eye="surprised", mth="o", dy=-1))
    # sad (2): sad eyes + frown; second frame adds a tear pixel.
    frames.append(base_frame(char, eye="sad", mth="frown"))
    g = base_frame(char, eye="sad", mth="frown")
    g.put(9, 14, "W")
    frames.append(g)
    # think (3): eyes up + rising thought dots.
    for dots in ([(16, 3)], [(16, 3), (18, 2)], [(16, 3), (18, 2), (20, 1)]):
        g = base_frame(char, eye="think", mth="line")
        for (x, y) in dots:
            g.put(x, y, "W")
        frames.append(g)
    # point (2): arm extended right + arrow, eyes looking right.
    for dy in (0, -1):
        g = base_frame(char, eye="right", mth="line", dy=dy)
        g.put(20, 12 + dy, "B")
        g.put(21, 12 + dy, "B")
        g.put(22, 12 + dy, "K")
        g.put(22, 11 + dy, "K")
        g.put(23, 12 + dy, "K")
        g.put(22, 13 + dy, "K")
        frames.append(g)
    # poof (4): body + small puffs, half cloud, full cloud, fading cloud.
    g = base_frame(char)
    cloud(g, [(3, 6), (4, 5), (20, 6), (19, 5), (5, 20), (19, 20)])
    frames.append(g)
    g = base_frame(char)
    cloud(g, [(3, 5), (4, 4), (5, 5), (18, 5), (19, 4), (20, 5),
               (3, 19), (4, 20), (19, 20), (20, 19)])
    frames.append(g)
    g = Grid()
    cloud(g, [(4, 5), (5, 4), (6, 5), (7, 4), (8, 5), (15, 4), (16, 5),
               (17, 4), (18, 5), (4, 18), (5, 19), (18, 19), (19, 18),
               (10, 10), (11, 11), (12, 10), (13, 11)])
    frames.append(g)
    g = Grid()
    cloud(g, [(5, 6), (8, 5), (16, 5), (19, 6), (6, 18), (17, 18)])
    frames.append(g)
    assert len(frames) == 41, len(frames)
    return frames


def render_sheet(char, frames, palette):
    img = Image.new("RGBA", (41 * 24, 24), (0, 0, 0, 0))
    for i, g in enumerate(frames):
        for (x, y), k in g.p.items():
            img.putpixel((i * 24 + x, y), palette[k])
    return img


def main():
    os.makedirs(os.path.join(RES, "drawable-nodpi"), exist_ok=True)
    os.makedirs(os.path.join(RES, "raw"), exist_ok=True)
    atlas = {"version": 1, "cell": 24, "frameCount": 41, "animations": {}}
    for name, start, count, fps in ANIMS:
        atlas["animations"][name] = {
            "frames": list(range(start, start + count)),
            "fps": fps,
        }
    for char in ("milo", "mila"):
        frames = build_frames(char)
        sheet = render_sheet(char, frames, PALETTES[char])
        sheet.save(os.path.join(RES, "drawable-nodpi", "mascot_%s.png" % char))
        print("wrote mascot_%s.png %s" % (char, sheet.size))
    with open(os.path.join(RES, "raw", "mascot_frames.json"), "w") as f:
        json.dump(atlas, f, indent=2)
    print("wrote mascot_frames.json")

    # Preview: all frames at 4x on gray AND dark, with labels.
    font = ImageFont.truetype(
        "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf", 13)
    font_b = ImageFont.truetype(
        "/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf", 16)
    cell = 96
    label_h = 18
    cols = 7
    rows = 6
    pad = 16
    panel_w = pad * 2 + cols * (cell + 8)
    names = []
    for name, start, count, _ in ANIMS:
        for i in range(count):
            names.append("%s %d" % (name, i))
    # Reviewable layout: 4 panels stacked — Milo gray, Milo dark,
    # Mila gray, Mila dark.
    panel_h2 = 34 + rows * (cell + label_h + 8)
    img = Image.new("RGB", (panel_w, panel_h2 * 4 + 24), (0, 0, 0))
    draw = ImageDraw.Draw(img)
    panels = []
    for char in ("milo", "mila"):
        sheet = Image.open(os.path.join(
            RES, "drawable-nodpi", "mascot_%s.png" % char))
        for bg, bgname in (((128, 128, 128), "GRAY"), ((26, 26, 26), "DARK")):
            panels.append((char, sheet, bg, bgname))
    for pi, (char, sheet, bg, bgname) in enumerate(panels):
        y0 = pi * (panel_h2 + 8)
        draw.rectangle([0, y0, panel_w, y0 + panel_h2], fill=bg)
        draw.text((pad, y0 + 8), "%s — %s background (4x)" % (char.upper(), bgname),
                  fill=(255, 255, 255), font=font_b)
        for idx, label in enumerate(names):
            r, c = divmod(idx, cols)
            x = pad + c * (cell + 8)
            y = y0 + 34 + r * (cell + label_h + 8)
            frame = sheet.crop((idx * 24, 0, idx * 24 + 24, 24))
            frame = frame.resize((cell, cell), Image.NEAREST)
            img.paste(frame, (x, y), frame)
            draw.text((x, y + cell + 2), label, fill=(255, 255, 255), font=font)
    img.save(os.path.join(HERE, "preview.png"))
    print("wrote preview.png", img.size)


if __name__ == "__main__":
    main()
