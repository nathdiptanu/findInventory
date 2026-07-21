from PIL import Image
import os


def make_adaptive_fg(src: str, dst: str, canvas: int = 1080, safe_ratio: float = 0.66) -> None:
    im = Image.open(src).convert("RGBA")
    pixels = im.load()
    w, h = im.size
    for y in range(h):
        for x in range(w):
            r, g, b, a = pixels[x, y]
            if r < 28 and g < 28 and b < 28:
                pixels[x, y] = (0, 0, 0, 0)
            elif r > 245 and g > 245 and b > 245:
                pixels[x, y] = (0, 0, 0, 0)
    bbox = im.getbbox()
    if not bbox:
        raise SystemExit(f"empty after key: {src}")
    crop = im.crop(bbox)
    safe = int(canvas * safe_ratio)
    crop.thumbnail((safe, safe), Image.Resampling.LANCZOS)
    out = Image.new("RGBA", (canvas, canvas), (0, 0, 0, 0))
    ox = (canvas - crop.size[0]) // 2
    oy = (canvas - crop.size[1]) // 2
    out.paste(crop, (ox, oy), crop)
    os.makedirs(os.path.dirname(dst), exist_ok=True)
    out.save(dst, "PNG")
    print("wrote", dst, out.size, "from", src)


candidates = [
    "releases/play-store-assets/app-icon-512.png",
    "app/src/main/res/drawable-nodpi/ic_docufind_app_icon.png",
    "app/src/main/res/drawable-nodpi/ic_docufind_mark_raster.png",
    "releases/branding/ic_launcher_foreground_1080x1080.png",
]
for c in candidates:
    if os.path.exists(c):
        make_adaptive_fg(c, "app/src/main/res/drawable-nodpi/ic_launcher_fg_safe.png", safe_ratio=0.62)
        make_adaptive_fg(c, "app/src/main/res/drawable-nodpi/ic_docufind_mark_clean.png", safe_ratio=0.90)
        break
print("done")
