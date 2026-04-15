from __future__ import annotations

import math
import random
from pathlib import Path

from PIL import Image, ImageDraw, ImageFilter, ImageFont, ImageOps


ROOT = Path(__file__).resolve().parents[2]
BRANDING_DIR = ROOT / "docs" / "branding"
ANDROID_DRAWABLE_DIR = ROOT / "mobile" / "android-app" / "app" / "src" / "main" / "res" / "drawable-nodpi"

PALETTE = {
    "deep_night": "#071319",
    "abyss": "#0B1D24",
    "current": "#11333B",
    "lagoon": "#18454D",
    "mist": "#F0F5F1",
    "fog": "#A8C0BF",
    "foam": "#74D6C8",
    "shore": "#6B7C63",
    "moss": "#8CC58E",
    "amber": "#D8B16D",
}

FONT_TITLE = [
    Path("/System/Library/Fonts/NewYork.ttf"),
    Path("/System/Library/Fonts/Supplemental/Georgia Bold.ttf"),
]
FONT_SUBTITLE = [
    Path("/System/Library/Fonts/SFNS.ttf"),
    Path("/System/Library/Fonts/Supplemental/GillSans.ttc"),
]


def hex_to_rgb(value: str) -> tuple[int, int, int]:
    value = value.lstrip("#")
    return tuple(int(value[i : i + 2], 16) for i in (0, 2, 4))


def rgba(name: str, alpha: int = 255) -> tuple[int, int, int, int]:
    return (*hex_to_rgb(PALETTE[name]), alpha)


def mix_color(a: tuple[int, int, int], b: tuple[int, int, int], t: float) -> tuple[int, int, int]:
    return tuple(int(a[i] + (b[i] - a[i]) * t) for i in range(3))


def load_font(candidates: list[Path], size: int) -> ImageFont.FreeTypeFont:
    for path in candidates:
        if path.exists():
            return ImageFont.truetype(str(path), size=size)
    return ImageFont.load_default()


def multi_stop_gradient(size: tuple[int, int], stops: list[tuple[float, tuple[int, int, int]]]) -> Image.Image:
    width, height = size
    image = Image.new("RGBA", size)
    draw = ImageDraw.Draw(image)
    for y in range(height):
        t = y / max(height - 1, 1)
        for index in range(len(stops) - 1):
            left_pos, left_color = stops[index]
            right_pos, right_color = stops[index + 1]
            if left_pos <= t <= right_pos:
                local_t = 0 if right_pos == left_pos else (t - left_pos) / (right_pos - left_pos)
                draw.line([(0, y), (width, y)], fill=(*mix_color(left_color, right_color, local_t), 255))
                break
    return image


def add_soft_blob(
    base: Image.Image,
    bbox: tuple[float, float, float, float],
    color: tuple[int, int, int, int],
    blur_radius: float,
) -> None:
    overlay = Image.new("RGBA", base.size, (0, 0, 0, 0))
    draw = ImageDraw.Draw(overlay)
    draw.ellipse(bbox, fill=color)
    overlay = overlay.filter(ImageFilter.GaussianBlur(blur_radius))
    base.alpha_composite(overlay)


def draw_mountain_layer(
    base: Image.Image,
    seed: int,
    horizon: float,
    amplitude: float,
    fill: tuple[int, int, int, int],
    blur_radius: float,
    detail: int = 11,
) -> None:
    rng = random.Random(seed)
    width, height = base.size
    step = max(30, width // detail)
    overlay = Image.new("RGBA", base.size, (0, 0, 0, 0))
    draw = ImageDraw.Draw(overlay)
    points = [(0, height)]
    for x in range(-step, width + step, step):
        wobble = math.sin(x / max(width, 1) * math.pi * rng.uniform(0.9, 1.7)) * amplitude * 0.55
        lift = math.cos((x / max(width, 1)) * math.pi * rng.uniform(1.4, 2.2)) * amplitude * 0.25
        noise = rng.uniform(-amplitude * 0.16, amplitude * 0.16)
        y = horizon + wobble + lift + noise
        points.append((x, y))
    points.extend([(width, height), (0, height)])
    draw.polygon(points, fill=fill)
    overlay = overlay.filter(ImageFilter.GaussianBlur(blur_radius))
    base.alpha_composite(overlay)


def draw_shoreline(base: Image.Image, seed: int, y_start: float, side: str) -> None:
    rng = random.Random(seed)
    width, height = base.size
    overlay = Image.new("RGBA", base.size, (0, 0, 0, 0))
    draw = ImageDraw.Draw(overlay)
    if side == "left":
        points = [(0, height), (0, y_start)]
        for step in range(8):
            x = width * (0.06 + 0.035 * step)
            y = y_start + step * 18 + rng.uniform(-18, 22)
            points.append((x, y))
        points.append((width * 0.32, height))
    else:
        points = [(width, height), (width, y_start)]
        for step in range(8):
            x = width - width * (0.06 + 0.035 * step)
            y = y_start + step * 18 + rng.uniform(-18, 22)
            points.append((x, y))
        points.append((width * 0.68, height))
    draw.polygon(points, fill=rgba("shore", 170))
    for _ in range(18):
        x = rng.uniform(0, width * 0.28) if side == "left" else rng.uniform(width * 0.72, width)
        y = rng.uniform(y_start - 24, height)
        radius = rng.uniform(4, 16)
        draw.ellipse((x - radius, y - radius * 0.8, x + radius, y + radius * 0.8), fill=rgba("mist", 28))
    overlay = overlay.filter(ImageFilter.GaussianBlur(7))
    base.alpha_composite(overlay)


def draw_water(base: Image.Image, water_top: float, seed: int) -> None:
    width, height = base.size
    draw = ImageDraw.Draw(base)
    rng = random.Random(seed)
    for index in range(28):
        y = water_top + index * (height - water_top) / 28
        amplitude = 3 + index * 0.26
        points = []
        for x in range(-40, width + 41, 32):
            wave = math.sin((x / width) * math.pi * (3.2 + index * 0.05) + index * 0.8) * amplitude
            wave += math.cos((x / width) * math.pi * 2.0 + seed * 0.12) * amplitude * 0.45
            points.append((x, y + wave))
        alpha = 18 + index * 2
        tone = mix_color(hex_to_rgb(PALETTE["fog"]), hex_to_rgb(PALETTE["foam"]), min(index / 28, 1))
        draw.line(points, fill=(*tone, min(alpha, 90)), width=2 if index % 3 == 0 else 1)

    for _ in range(30):
        x = rng.uniform(width * 0.08, width * 0.92)
        y = rng.uniform(water_top + 10, height * 0.94)
        w = rng.uniform(width * 0.02, width * 0.08)
        h = w * rng.uniform(0.12, 0.22)
        draw.arc((x - w, y - h, x + w, y + h), start=180, end=360, fill=rgba("mist", 35), width=1)


def add_grain(base: Image.Image, opacity: int = 32) -> None:
    noise = Image.effect_noise(base.size, 8).convert("L")
    noise = ImageOps.autocontrast(noise)
    alpha = noise.point(lambda value: int(value * opacity / 255))
    grain = Image.new("RGBA", base.size, (*hex_to_rgb(PALETTE["mist"]), 0))
    grain.putalpha(alpha)
    grain = grain.filter(ImageFilter.GaussianBlur(0.5))
    base.alpha_composite(grain)


def add_vignette(base: Image.Image, strength: int = 170) -> None:
    width, height = base.size
    mask = Image.new("L", base.size, 0)
    mask_draw = ImageDraw.Draw(mask)
    inset_x = int(width * 0.12)
    inset_y = int(height * 0.10)
    mask_draw.ellipse((inset_x, inset_y, width - inset_x, height - inset_y), fill=255)
    mask = mask.filter(ImageFilter.GaussianBlur(width // 9))
    mask = ImageOps.invert(mask).point(lambda value: int(value * strength / 255))
    shade = Image.new("RGBA", base.size, (*hex_to_rgb(PALETTE["deep_night"]), 0))
    shade.putalpha(mask)
    base.alpha_composite(shade)


def draw_angler(base: Image.Image, anchor: tuple[float, float], scale: float = 1.0) -> None:
    draw = ImageDraw.Draw(base)
    x, y = anchor
    shadow = rgba("deep_night", 210)
    amber = rgba("amber", 130)
    dock_y = y + 8 * scale
    draw.line([(x - 36 * scale, dock_y), (x + 46 * scale, dock_y)], fill=rgba("fog", 62), width=max(1, int(2 * scale)))
    draw.line([(x - 18 * scale, dock_y), (x - 8 * scale, dock_y + 18 * scale)], fill=shadow, width=max(1, int(3 * scale)))
    draw.line([(x + 16 * scale, dock_y), (x + 24 * scale, dock_y + 20 * scale)], fill=shadow, width=max(1, int(3 * scale)))
    draw.line([(x, y - 20 * scale), (x, y + 8 * scale)], fill=shadow, width=max(1, int(5 * scale)))
    draw.line([(x - 12 * scale, y - 6 * scale), (x + 8 * scale, y + 2 * scale)], fill=shadow, width=max(1, int(4 * scale)))
    draw.ellipse((x - 8 * scale, y - 34 * scale, x + 8 * scale, y - 18 * scale), fill=shadow)
    draw.polygon(
        [(x - 10 * scale, y - 32 * scale), (x + 2 * scale, y - 40 * scale), (x + 13 * scale, y - 31 * scale)],
        fill=shadow,
    )
    rod = [
        (x + 2 * scale, y - 18 * scale),
        (x + 48 * scale, y - 56 * scale),
        (x + 98 * scale, y - 76 * scale),
        (x + 150 * scale, y - 72 * scale),
    ]
    draw.line(rod, fill=rgba("fog", 155), width=max(1, int(2 * scale)))
    draw.arc(
        (x + 130 * scale, y - 50 * scale, x + 202 * scale, y + 24 * scale),
        start=210,
        end=292,
        fill=amber,
        width=max(1, int(2 * scale)),
    )


def draw_fish_mark(base: Image.Image, center: tuple[float, float], size: float, fill: tuple[int, int, int, int]) -> None:
    overlay = Image.new("RGBA", base.size, (0, 0, 0, 0))
    draw = ImageDraw.Draw(overlay)
    cx, cy = center
    body_w = size
    body_h = size * 0.34
    silhouette = [
        (cx - body_w * 0.50, cy),
        (cx - body_w * 0.35, cy - body_h * 0.32),
        (cx - body_w * 0.02, cy - body_h * 0.42),
        (cx + body_w * 0.14, cy - body_h * 0.64),
        (cx + body_w * 0.26, cy - body_h * 0.36),
        (cx + body_w * 0.42, cy - body_h * 0.12),
        (cx + body_w * 0.58, cy - body_h * 0.18),
        (cx + body_w * 0.45, cy + body_h * 0.02),
        (cx + body_w * 0.60, cy + body_h * 0.18),
        (cx + body_w * 0.40, cy + body_h * 0.18),
        (cx + body_w * 0.20, cy + body_h * 0.40),
        (cx + body_w * 0.05, cy + body_h * 0.24),
        (cx - body_w * 0.16, cy + body_h * 0.30),
        (cx - body_w * 0.35, cy + body_h * 0.26),
    ]
    draw.polygon(silhouette, fill=fill)
    tail = [
        (cx - body_w * 0.48, cy),
        (cx - body_w * 0.74, cy - body_h * 0.48),
        (cx - body_w * 0.64, cy),
        (cx - body_w * 0.74, cy + body_h * 0.48),
    ]
    dorsal = [(cx - body_w * 0.01, cy - body_h * 0.34), (cx + body_w * 0.12, cy - body_h * 0.78), (cx + body_w * 0.21, cy - body_h * 0.28)]
    ventral = [(cx + body_w * 0.03, cy + body_h * 0.16), (cx + body_w * 0.16, cy + body_h * 0.58), (cx + body_w * 0.22, cy + body_h * 0.10)]
    draw.polygon(tail, fill=fill)
    draw.polygon(dorsal, fill=fill)
    draw.polygon(ventral, fill=fill)
    draw.line(
        [(cx - body_w * 0.08, cy - body_h * 0.02), (cx + body_w * 0.28, cy - body_h * 0.02)],
        fill=rgba("fog", 110),
        width=max(1, int(size * 0.01)),
    )
    draw.ellipse((cx + body_w * 0.18, cy - body_h * 0.10, cx + body_w * 0.25, cy - body_h * 0.03), fill=rgba("deep_night", 220))
    base.alpha_composite(overlay)


def scenic_background(size: tuple[int, int], seed: int, drama: float = 1.0) -> Image.Image:
    width, height = size
    rng = random.Random(seed)
    gradient = multi_stop_gradient(
        size,
        [
            (0.0, hex_to_rgb(PALETTE["deep_night"])),
            (0.26, hex_to_rgb(PALETTE["current"])),
            (0.56, mix_color(hex_to_rgb(PALETTE["lagoon"]), hex_to_rgb(PALETTE["fog"]), 0.32)),
            (1.0, mix_color(hex_to_rgb(PALETTE["abyss"]), hex_to_rgb(PALETTE["shore"]), 0.32)),
        ],
    )
    add_soft_blob(gradient, (-width * 0.15, -height * 0.12, width * 0.48, height * 0.42), rgba("foam", 52), width * 0.08)
    add_soft_blob(gradient, (width * 0.40, -height * 0.18, width * 1.02, height * 0.36), rgba("mist", 44), width * 0.09)
    add_soft_blob(gradient, (width * 0.08, height * 0.45, width * 0.92, height * 1.05), rgba("moss", 20), width * 0.12)

    draw_mountain_layer(gradient, seed + 11, height * (0.42 - drama * 0.03), height * 0.18, rgba("fog", 46), width * 0.018)
    draw_mountain_layer(gradient, seed + 17, height * (0.48 - drama * 0.02), height * 0.15, rgba("lagoon", 84), width * 0.014)
    draw_mountain_layer(gradient, seed + 23, height * (0.58 - drama * 0.015), height * 0.11, rgba("abyss", 166), width * 0.010)

    draw_shoreline(gradient, seed + 31, height * 0.56, "left")
    draw_shoreline(gradient, seed + 37, height * 0.52, "right")
    draw_water(gradient, height * 0.55, seed + 49)

    for _ in range(24):
        x = rng.uniform(width * 0.18, width * 0.82)
        y = rng.uniform(height * 0.22, height * 0.86)
        radius = rng.uniform(width * 0.012, width * 0.035)
        add_soft_blob(gradient, (x - radius, y - radius * 0.5, x + radius, y + radius * 0.5), rgba("mist", rng.randint(10, 26)), radius * 2.4)

    add_grain(gradient, 20)
    add_vignette(gradient, 145)
    return gradient


def draw_brand_lockup(base: Image.Image, title: str, subtitle: str, box: tuple[int, int, int, int]) -> None:
    x0, y0, x1, y1 = box
    width = x1 - x0
    height = y1 - y0
    card = Image.new("RGBA", base.size, (0, 0, 0, 0))
    draw = ImageDraw.Draw(card)
    draw.rounded_rectangle(box, radius=int(height * 0.18), fill=rgba("deep_night", 132), outline=rgba("fog", 38), width=2)
    card = card.filter(ImageFilter.GaussianBlur(0.6))
    base.alpha_composite(card)

    draw = ImageDraw.Draw(base)
    title_font = load_font(FONT_TITLE, max(40, int(height * 0.28)))
    subtitle_font = load_font(FONT_SUBTITLE, max(18, int(height * 0.10)))
    pill_font = load_font(FONT_SUBTITLE, max(14, int(height * 0.075)))

    pill_box = (x0 + int(width * 0.06), y0 + int(height * 0.10), x0 + int(width * 0.44), y0 + int(height * 0.28))
    draw.rounded_rectangle(pill_box, radius=int((pill_box[3] - pill_box[1]) / 2), fill=rgba("foam", 210))
    draw.text((pill_box[0] + int(width * 0.04), pill_box[1] + int(height * 0.02)), "MOBILE + TELEGRAM", font=pill_font, fill=rgba("deep_night", 235))

    title_y = pill_box[3] + int(height * 0.06)
    draw.text((x0 + int(width * 0.06), title_y), title, font=title_font, fill=rgba("mist", 240))
    subtitle_y = title_y + int(height * 0.31)
    draw.multiline_text((x0 + int(width * 0.06), subtitle_y), subtitle, font=subtitle_font, fill=rgba("fog", 230), spacing=4)
    draw.line(
        [(x0 + int(width * 0.06), y1 - int(height * 0.14)), (x0 + int(width * 0.28), y1 - int(height * 0.14))],
        fill=rgba("amber", 150),
        width=3,
    )


def make_icon_foreground(size: int, with_square_background: bool) -> Image.Image:
    base = Image.new("RGBA", (size, size), rgba("deep_night", 255) if with_square_background else (0, 0, 0, 0))
    medallion = scenic_background((int(size * 0.68), int(size * 0.68)), seed=12, drama=0.7)
    mask = Image.new("L", medallion.size, 0)
    ImageDraw.Draw(mask).ellipse((0, 0, medallion.size[0] - 1, medallion.size[1] - 1), fill=255)
    medallion.putalpha(mask)

    ring = Image.new("RGBA", medallion.size, (0, 0, 0, 0))
    ring_draw = ImageDraw.Draw(ring)
    ring_draw.ellipse((8, 8, medallion.size[0] - 8, medallion.size[1] - 8), outline=rgba("mist", 190), width=8)
    ring_draw.ellipse((26, 26, medallion.size[0] - 26, medallion.size[1] - 26), outline=rgba("amber", 84), width=3)
    medallion.alpha_composite(ring)

    draw_fish_mark(medallion, (medallion.size[0] * 0.52, medallion.size[1] * 0.47), medallion.size[0] * 0.36, rgba("mist", 230))
    medallion_draw = ImageDraw.Draw(medallion)
    for offset in (-0.11, 0.0, 0.11):
        crest_x = medallion.size[0] * (0.5 + offset)
        medallion_draw.line(
            [(crest_x, medallion.size[1] * 0.24), (crest_x, medallion.size[1] * (0.12 + abs(offset) * 0.10))],
            fill=rgba("amber", 160),
            width=max(2, medallion.size[0] // 80),
        )
    for wave in range(3):
        y = medallion.size[1] * (0.70 + wave * 0.07)
        medallion_draw.arc(
            (medallion.size[0] * 0.18, y - medallion.size[1] * 0.09, medallion.size[0] * 0.82, y + medallion.size[1] * 0.05),
            start=185,
            end=355,
            fill=rgba("foam", 165 - wave * 24),
            width=max(2, medallion.size[0] // 90),
        )

    paste_x = int((size - medallion.size[0]) / 2)
    paste_y = int((size - medallion.size[1]) / 2)
    if with_square_background:
        shadow = Image.new("RGBA", medallion.size, rgba("deep_night", 0))
        shadow_mask = Image.new("L", medallion.size, 0)
        ImageDraw.Draw(shadow_mask).ellipse((0, 0, medallion.size[0] - 1, medallion.size[1] - 1), fill=155)
        shadow.putalpha(shadow_mask.filter(ImageFilter.GaussianBlur(size * 0.02)))
        base.alpha_composite(shadow, (paste_x, paste_y + int(size * 0.018)))
    base.alpha_composite(medallion, (paste_x, paste_y))

    if with_square_background:
        add_soft_blob(base, (size * 0.12, size * 0.04, size * 0.88, size * 0.50), rgba("foam", 28), size * 0.12)
        add_vignette(base, 110)
        add_grain(base, 18)

    return base


def make_play_feature() -> Image.Image:
    base = scenic_background((1024, 500), seed=42, drama=0.78)
    add_soft_blob(base, (590, 62, 1030, 412), rgba("mist", 34), 54)
    draw_angler(base, (770, 230), scale=0.95)
    draw_brand_lockup(
        base,
        title="RiverKing",
        subtitle="Quiet fishing adventure with shared progression\nacross mobile and Telegram.",
        box=(66, 86, 472, 362),
    )
    return base


def make_itch_cover() -> Image.Image:
    base = scenic_background((1280, 720), seed=91, drama=1.0)
    add_soft_blob(base, (360, 260, 980, 720), rgba("amber", 14), 76)
    draw_angler(base, (690, 420), scale=1.24)

    draw = ImageDraw.Draw(base)
    title_font = load_font(FONT_TITLE, 108)
    subtitle_font = load_font(FONT_SUBTITLE, 34)
    kicker_font = load_font(FONT_SUBTITLE, 20)

    draw.rounded_rectangle((110, 108, 328, 154), radius=23, fill=rgba("foam", 215))
    draw.text((134, 120), "MOBILE + TELEGRAM", font=kicker_font, fill=rgba("deep_night", 240))
    draw.text((108, 186), "RiverKing", font=title_font, fill=rgba("mist", 244))
    draw.text((114, 310), "Atmospheric fishing progression across mobile and Telegram.", font=subtitle_font, fill=rgba("fog", 236))
    draw.line((112, 382, 364, 382), fill=rgba("amber", 150), width=4)
    return base


def save_image(image: Image.Image, path: Path) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    image.save(path, format="PNG", optimize=True)


def main() -> None:
    save_image(make_icon_foreground(1024, with_square_background=True), BRANDING_DIR / "android-icon-1024.png")
    save_image(make_play_feature(), BRANDING_DIR / "play-feature-1024x500.png")
    save_image(make_itch_cover(), BRANDING_DIR / "itch-cover-1280x720.png")
    save_image(make_icon_foreground(1024, with_square_background=False), ANDROID_DRAWABLE_DIR / "ic_launcher_foreground.png")
    print("Branding assets regenerated.")


if __name__ == "__main__":
    main()
