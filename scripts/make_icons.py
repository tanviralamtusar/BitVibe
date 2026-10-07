"""Builds BeatVibe launcher icons and the in-app logo from the source logo.

Usage (needs Pillow): python3 scripts/make_icons.py art/logo-source.webp app/src/main/res /tmp/icon-preview.png
"""
import math, sys, os
from PIL import Image, ImageDraw

src, res = sys.argv[1], sys.argv[2]
PURPLE = (0x5B, 0x2E, 0xFC)

im = Image.open(src).convert("RGB")
w, h = im.size
# Alpha from how far each pixel is from white (green channel: white≈255, purple≈46) — keeps anti-aliasing.
alpha = Image.new("L", (w, h))
px, ap = im.load(), alpha.load()
for y in range(h):
    for x in range(w):
        g = px[x, y][1]
        a = (250 - g) / (250 - 46)
        ap[x, y] = int(max(0.0, min(1.0, a)) * 255)
bbox = alpha.point(lambda v: 255 if v > 40 else 0).getbbox()
alpha = alpha.crop(bbox)
logo = Image.new("RGBA", alpha.size, PURPLE + (0,))
logo.putalpha(alpha)
lw, lh = logo.size

# Farthest opaque pixel from the logo's centre: scale so it stays inside the adaptive-icon safe circle.
cx, cy = lw / 2, lh / 2
la = alpha.load()
rmax = max(math.hypot(x + .5 - cx, y + .5 - cy) for y in range(lh) for x in range(lw) if la[x, y] > 40)
print("logo", logo.size, "rmax", round(rmax, 1))

def place(canvas_px, radius_px, bg=None, shape=None):
    """Logo centred on a canvas, scaled so its farthest pixel sits at radius_px."""
    scale = radius_px / rmax
    sized = logo.resize((max(1, round(lw * scale)), max(1, round(lh * scale))), Image.LANCZOS)
    out = Image.new("RGBA", (canvas_px, canvas_px), (0, 0, 0, 0))
    if bg:
        mask = Image.new("L", (canvas_px * 4, canvas_px * 4), 0)
        d = ImageDraw.Draw(mask)
        if shape == "circle":
            d.ellipse((0, 0, canvas_px * 4 - 1, canvas_px * 4 - 1), fill=255)
        else:
            d.rounded_rectangle((0, 0, canvas_px * 4 - 1, canvas_px * 4 - 1), radius=canvas_px * 4 * 0.18, fill=255)
        mask = mask.resize((canvas_px, canvas_px), Image.LANCZOS)
        out.paste(Image.new("RGBA", (canvas_px, canvas_px), bg + (255,)), (0, 0), mask)
    out.alpha_composite(sized, ((canvas_px - sized.width) // 2, (canvas_px - sized.height) // 2))
    return out

densities = {"mdpi": 1, "hdpi": 1.5, "xhdpi": 2, "xxhdpi": 3, "xxxhdpi": 4}
for name, d in densities.items():
    folder = os.path.join(res, f"mipmap-{name}")
    os.makedirs(folder, exist_ok=True)
    # Adaptive foreground: 108dp canvas, logo within a 30dp radius (safe zone is 33dp).
    place(round(108 * d), 30 * d).save(os.path.join(folder, "ic_launcher_foreground.png"), optimize=True)
    # Legacy icons (Android 7): 48dp, white shape behind the logo.
    place(round(48 * d), 17 * d, bg=(255, 255, 255)).save(os.path.join(folder, "ic_launcher.webp"), lossless=True)
    place(round(48 * d), 16 * d, bg=(255, 255, 255), shape="circle").save(os.path.join(folder, "ic_launcher_round.webp"), lossless=True)

# In-app logo, trimmed, transparent background.
os.makedirs(os.path.join(res, "drawable-nodpi"), exist_ok=True)
scale = 512 / max(lw, lh)
logo.resize((round(lw * scale), round(lh * scale)), Image.LANCZOS).save(
    os.path.join(res, "drawable-nodpi", "beatvibe_logo.png"), optimize=True)
# Preview sheet for review.
prev = Image.new("RGBA", (3 * 220, 220), (30, 30, 30, 255))
fg = place(432, 120)
for i, shape in enumerate(["circle", "rounded", "square"]):
    tile = Image.new("RGBA", (432, 432), (255, 255, 255, 255)); tile.alpha_composite(fg)
    m = Image.new("L", (432, 432), 0); dd = ImageDraw.Draw(m)
    # Launchers show the inner 72dp of the 108dp canvas.
    inner = (72, 72, 360, 360)
    if shape == "circle": dd.ellipse(inner, fill=255)
    elif shape == "rounded": dd.rounded_rectangle(inner, radius=70, fill=255)
    else: dd.rectangle(inner, fill=255)
    t = Image.new("RGBA", (432, 432), (0, 0, 0, 0)); t.paste(tile, (0, 0), m)
    prev.alpha_composite(t.crop(inner).resize((200, 200), Image.LANCZOS), (i * 220 + 10, 10))
prev.save(sys.argv[3])
