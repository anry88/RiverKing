#!/usr/bin/env python3
from __future__ import annotations

import math
from pathlib import Path

from PIL import Image, ImageColor, ImageDraw, ImageFilter, ImageFont


ROOT = Path(__file__).resolve().parents[3]
RES = ROOT / "mobile" / "android-app" / "app" / "src" / "main" / "res"
DRAWABLE = RES / "drawable-nodpi"
DOCS_BRANDING = ROOT / "docs" / "branding"
GEORGIA_BOLD = "/System/Library/Fonts/Supplemental/Georgia Bold.ttf"
GEORGIA_REGULAR = "/System/Library/Fonts/Supplemental/Georgia.ttf"


RIVER_DEEP_NIGHT = "#071319"
RIVER_ABYSS = "#0B1D24"
RIVER_CURRENT = "#11333B"
RIVER_LAGOON = "#18454D"
RIVER_MIST = "#F0F5F1"
RIVER_FOG = "#A8C0BF"
RIVER_FOAM = "#74D6C8"
RIVER_TIDE = "#58B7C8"
RIVER_AMBER = "#D8B16D"
RIVER_CORAL = "#DC8F67"


def ensure_dirs() -> None:
    DRAWABLE.mkdir(parents=True, exist_ok=True)
    DOCS_BRANDING.mkdir(parents=True, exist_ok=True)


def radial_gradient(size: int, inner: str, outer: str) -> Image.Image:
    inner_rgb = ImageColor.getrgb(inner)
    outer_rgb = ImageColor.getrgb(outer)
    image = Image.new("RGBA", (size, size))
    pixels = image.load()
    cx = cy = size / 2
    max_dist = math.sqrt(2 * (cx**2))
    for y in range(size):
        for x in range(size):
            distance = math.sqrt((x - cx) ** 2 + (y - cy) ** 2)
            t = min(distance / max_dist, 1.0)
            pixels[x, y] = tuple(
                round(inner_rgb[channel] * (1 - t) + outer_rgb[channel] * t)
                for channel in range(3)
            ) + (255,)
    return image


def vertical_gradient(size: int, top: str, middle: str, bottom: str) -> Image.Image:
    top_rgb = ImageColor.getrgb(top)
    middle_rgb = ImageColor.getrgb(middle)
    bottom_rgb = ImageColor.getrgb(bottom)
    image = Image.new("RGBA", (size, size))
    pixels = image.load()
    for y in range(size):
        t = y / max(size - 1, 1)
        if t < 0.55:
            local = t / 0.55
            source = top_rgb
            target = middle_rgb
        else:
            local = (t - 0.55) / 0.45
            source = middle_rgb
            target = bottom_rgb
        row = tuple(
            round(source[channel] * (1 - local) + target[channel] * local)
            for channel in range(3)
        ) + (255,)
        for x in range(size):
            pixels[x, y] = row
    return image


def draw_background(size: int) -> Image.Image:
    base = vertical_gradient(size, RIVER_LAGOON, RIVER_CURRENT, RIVER_DEEP_NIGHT)
    vignette = radial_gradient(size, "#215A63", RIVER_ABYSS)
    base = Image.blend(base, vignette, 0.42)
    draw = ImageDraw.Draw(base, "RGBA")

    draw.rounded_rectangle(
        (32, 32, size - 32, size - 32),
        radius=220,
        outline=(255, 255, 255, 28),
        width=8,
    )

    for idx, alpha in enumerate((56, 36, 24)):
        inset = int(size * (0.12 + idx * 0.09))
        draw.ellipse(
            (inset, inset - 20, size - inset, size - inset + 20),
            outline=ImageColor.getrgb(RIVER_FOAM) + (alpha,),
            width=max(4, size // 40),
        )

    draw.ellipse(
        (size * 0.20, size * 0.20, size * 0.80, size * 0.80),
        fill=ImageColor.getrgb(RIVER_TIDE) + (22,),
    )
    draw.ellipse(
        (size * 0.28, size * 0.34, size * 0.76, size * 0.68),
        fill=ImageColor.getrgb(RIVER_DEEP_NIGHT) + (85,),
    )

    spark = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    spark_draw = ImageDraw.Draw(spark, "RGBA")
    spark_draw.ellipse(
        (size * 0.62, size * 0.14, size * 0.94, size * 0.46),
        fill=ImageColor.getrgb(RIVER_AMBER) + (72,),
    )
    spark_draw.ellipse(
        (size * 0.58, size * 0.08, size * 0.76, size * 0.26),
        fill=ImageColor.getrgb(RIVER_CORAL) + (38,),
    )
    spark = spark.filter(ImageFilter.GaussianBlur(radius=size * 0.04))
    base.alpha_composite(spark)
    return base


def fish_points(size: int) -> list[tuple[float, float]]:
    return [
        (0.17 * size, 0.58 * size),
        (0.32 * size, 0.42 * size),
        (0.52 * size, 0.34 * size),
        (0.72 * size, 0.39 * size),
        (0.84 * size, 0.50 * size),
        (0.74 * size, 0.62 * size),
        (0.52 * size, 0.68 * size),
        (0.34 * size, 0.66 * size),
    ]


def draw_foreground(size: int) -> Image.Image:
    image = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    draw = ImageDraw.Draw(image, "RGBA")

    body = fish_points(size)
    draw.polygon(body, fill=ImageColor.getrgb(RIVER_MIST) + (255,))

    tail = [
        (0.10 * size, 0.50 * size),
        (0.20 * size, 0.59 * size),
        (0.10 * size, 0.71 * size),
        (0.25 * size, 0.61 * size),
    ]
    draw.polygon(tail, fill=ImageColor.getrgb(RIVER_FOAM) + (255,))

    fin = [
        (0.40 * size, 0.34 * size),
        (0.52 * size, 0.18 * size),
        (0.60 * size, 0.36 * size),
    ]
    draw.polygon(fin, fill=ImageColor.getrgb(RIVER_TIDE) + (255,))

    under_fin = [
        (0.44 * size, 0.62 * size),
        (0.54 * size, 0.79 * size),
        (0.61 * size, 0.60 * size),
    ]
    draw.polygon(under_fin, fill=ImageColor.getrgb(RIVER_FOAM) + (220,))

    draw.ellipse(
        (0.63 * size, 0.45 * size, 0.71 * size, 0.53 * size),
        fill=ImageColor.getrgb(RIVER_DEEP_NIGHT) + (255,),
    )
    draw.ellipse(
        (0.655 * size, 0.47 * size, 0.68 * size, 0.495 * size),
        fill=ImageColor.getrgb(RIVER_AMBER) + (255,),
    )

    hook_width = max(10, size // 26)
    draw.arc(
        (0.63 * size, 0.08 * size, 0.96 * size, 0.55 * size),
        start=170,
        end=360,
        fill=ImageColor.getrgb(RIVER_AMBER) + (255,),
        width=hook_width,
    )
    draw.line(
        (0.80 * size, 0.02 * size, 0.80 * size, 0.14 * size),
        fill=ImageColor.getrgb(RIVER_AMBER) + (255,),
        width=hook_width,
    )
    draw.line(
        (0.87 * size, 0.49 * size, 0.92 * size, 0.58 * size),
        fill=ImageColor.getrgb(RIVER_AMBER) + (255,),
        width=max(8, size // 34),
    )
    draw.polygon(
        [
            (0.80 * size, 0.24 * size),
            (0.86 * size, 0.30 * size),
            (0.82 * size, 0.38 * size),
            (0.75 * size, 0.31 * size),
        ],
        fill=ImageColor.getrgb(RIVER_CORAL) + (245,),
    )

    wake = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    wake_draw = ImageDraw.Draw(wake, "RGBA")
    wake_draw.arc(
        (0.20 * size, 0.66 * size, 0.56 * size, 0.90 * size),
        start=190,
        end=350,
        fill=ImageColor.getrgb(RIVER_FOAM) + (160,),
        width=max(8, size // 34),
    )
    wake_draw.arc(
        (0.25 * size, 0.70 * size, 0.66 * size, 0.97 * size),
        start=195,
        end=344,
        fill=ImageColor.getrgb(RIVER_TIDE) + (96,),
        width=max(6, size // 42),
    )
    wake = wake.filter(ImageFilter.GaussianBlur(radius=size * 0.01))
    image.alpha_composite(wake)
    return image


def rounded_mask(size: int, radius: int) -> Image.Image:
    mask = Image.new("L", (size, size), 0)
    ImageDraw.Draw(mask).rounded_rectangle((0, 0, size, size), radius=radius, fill=255)
    return mask


def save_png(image: Image.Image, path: Path) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    image.save(path, format="PNG")


def make_cover(width: int, height: int, icon: Image.Image, title: str, subtitle: str, footer: str) -> Image.Image:
    canvas = Image.new("RGBA", (width, height), ImageColor.getrgb(RIVER_DEEP_NIGHT) + (255,))
    background = vertical_gradient(max(width, height), RIVER_LAGOON, RIVER_CURRENT, RIVER_DEEP_NIGHT)
    canvas.alpha_composite(background.resize((width, height)))
    overlay = Image.new("RGBA", (width, height), (0, 0, 0, 0))
    overlay_draw = ImageDraw.Draw(overlay, "RGBA")
    overlay_draw.ellipse(
        (width * 0.48, -height * 0.12, width * 1.02, height * 0.62),
        fill=ImageColor.getrgb(RIVER_AMBER) + (78,),
    )
    overlay_draw.ellipse(
        (-width * 0.12, height * 0.46, width * 0.54, height * 1.10),
        fill=ImageColor.getrgb(RIVER_FOAM) + (36,),
    )
    overlay = overlay.filter(ImageFilter.GaussianBlur(radius=height * 0.06))
    canvas.alpha_composite(overlay)

    draw = ImageDraw.Draw(canvas, "RGBA")
    for idx, alpha in enumerate((64, 34, 18)):
        inset = 44 + idx * 38
        draw.rounded_rectangle(
            (inset, inset, width - inset, height - inset),
            radius=52 + idx * 12,
            outline=ImageColor.getrgb(RIVER_FOAM) + (alpha,),
            width=4,
        )

    icon_size = int(height * 0.62)
    icon_box = (72, (height - icon_size) // 2, 72 + icon_size, (height + icon_size) // 2)
    framed = Image.new("RGBA", (icon_size, icon_size), (0, 0, 0, 0))
    framed_bg = draw_background(icon_size)
    framed_fg = draw_foreground(icon_size)
    framed.alpha_composite(framed_bg)
    framed.alpha_composite(framed_fg)
    canvas.alpha_composite(framed, (icon_box[0], icon_box[1]))

    title_font = ImageFont.truetype(GEORGIA_BOLD, size=int(height * 0.18))
    subtitle_font = ImageFont.truetype(GEORGIA_REGULAR, size=int(height * 0.072))
    footer_font = ImageFont.truetype(GEORGIA_REGULAR, size=int(height * 0.048))

    text_x = int(width * 0.47)
    draw.text((text_x, int(height * 0.17)), title, font=title_font, fill=ImageColor.getrgb(RIVER_MIST) + (255,))
    draw.text(
        (text_x, int(height * 0.43)),
        subtitle,
        font=subtitle_font,
        fill=ImageColor.getrgb(RIVER_FOAM) + (255,),
        spacing=10,
    )
    draw.text(
        (text_x, int(height * 0.73)),
        footer,
        font=footer_font,
        fill=ImageColor.getrgb(RIVER_FOG) + (255,),
    )
    return canvas


def main() -> None:
    ensure_dirs()

    adaptive_size = 432
    background = draw_background(adaptive_size)
    foreground = draw_foreground(adaptive_size)

    save_png(background, DRAWABLE / "ic_launcher_background.png")
    save_png(foreground, DRAWABLE / "ic_launcher_foreground.png")

    store_size = 1024
    store_background = draw_background(store_size)
    store_foreground = draw_foreground(store_size)
    store_image = Image.alpha_composite(store_background, store_foreground)
    store_image.putalpha(rounded_mask(store_size, 240))
    save_png(store_image, DOCS_BRANDING / "android-icon-1024.png")

    itch_cover = make_cover(
        width=1280,
        height=720,
        icon=store_image,
        title="RiverKing",
        subtitle="Telegram-connected\nfishing RPG for Android",
        footer="Quests, tournaments, clubs, referrals, and shared progress",
    )
    save_png(itch_cover, DOCS_BRANDING / "itch-cover-1280x720.png")

    play_feature = make_cover(
        width=1024,
        height=500,
        icon=store_image,
        title="RiverKing",
        subtitle="Fishing RPG for Android",
        footer="Shared mobile + Telegram progression",
    )
    save_png(play_feature, DOCS_BRANDING / "play-feature-1024x500.png")


if __name__ == "__main__":
    main()
