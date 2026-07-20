from __future__ import annotations

import math
import re
from pathlib import Path

from PIL import Image, ImageDraw, ImageFilter, ImageFont


ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / "releases" / "play-store-assets"

PRIMARY = "#1265E1"
SKY = "#2D9CFF"
LIGHT = "#67C3FF"
SOFT = "#E8F2FF"
NAVY = "#0A1A33"
DARK = "#1E2A3A"
SLATE = "#334155"
GREEN = "#10B981"
WHITE = "#FFFFFF"


def font(size: int, weight: str = "regular") -> ImageFont.FreeTypeFont:
    candidates = {
        "bold": [
            r"C:\Windows\Fonts\segoeuib.ttf",
            r"C:\Windows\Fonts\arialbd.ttf",
        ],
        "semibold": [
            r"C:\Windows\Fonts\seguisb.ttf",
            r"C:\Windows\Fonts\segoeuib.ttf",
        ],
        "regular": [
            r"C:\Windows\Fonts\segoeui.ttf",
            r"C:\Windows\Fonts\arial.ttf",
        ],
    }
    for path in candidates.get(weight, candidates["regular"]):
        if Path(path).exists():
            return ImageFont.truetype(path, size)
    return ImageFont.load_default()


def rounded(draw: ImageDraw.ImageDraw, box, radius, fill, outline=None, width=1):
    draw.rounded_rectangle(box, radius=radius, fill=fill, outline=outline, width=width)


def text(draw, xy, text, size, fill=NAVY, weight="regular", anchor=None, align="left"):
    draw.text(xy, text, font=font(size, weight), fill=fill, anchor=anchor, align=align)


def text_size(text_value: str, size: int, weight: str = "regular") -> tuple[int, int]:
    box = ImageDraw.Draw(Image.new("RGB", (1, 1))).textbbox((0, 0), text_value, font=font(size, weight))
    return box[2] - box[0], box[3] - box[1]


def wrap_text(draw, text_value, max_width, size, weight="regular"):
    words = text_value.split()
    lines = []
    current = ""
    f = font(size, weight)
    for word in words:
        candidate = word if not current else f"{current} {word}"
        width = draw.textbbox((0, 0), candidate, font=f)[2]
        if width <= max_width:
            current = candidate
        else:
            if current:
                lines.append(current)
            current = word
    if current:
        lines.append(current)
    return lines


def vertical_gradient(size, top, bottom):
    image = Image.new("RGB", size, top)
    draw = ImageDraw.Draw(image)
    h = size[1]
    top_rgb = tuple(int(top[i : i + 2], 16) for i in (1, 3, 5))
    bottom_rgb = tuple(int(bottom[i : i + 2], 16) for i in (1, 3, 5))
    for y in range(h):
        t = y / max(1, h - 1)
        rgb = tuple(int(top_rgb[i] * (1 - t) + bottom_rgb[i] * t) for i in range(3))
        draw.line([(0, y), (size[0], y)], fill=rgb)
    return image.convert("RGBA")


def radial_glow(base: Image.Image, center, radius, color, alpha=180):
    overlay = Image.new("RGBA", base.size, (0, 0, 0, 0))
    draw = ImageDraw.Draw(overlay)
    rgb = tuple(int(color[i : i + 2], 16) for i in (1, 3, 5))
    for r in range(radius, 0, -8):
        a = int(alpha * (1 - r / radius) ** 1.8)
        draw.ellipse((center[0] - r, center[1] - r, center[0] + r, center[1] + r), fill=(*rgb, a))
    base.alpha_composite(overlay)


def cubic(p0, p1, p2, p3, steps=24):
    pts = []
    for i in range(1, steps + 1):
        t = i / steps
        x = (
            (1 - t) ** 3 * p0[0]
            + 3 * (1 - t) ** 2 * t * p1[0]
            + 3 * (1 - t) * t**2 * p2[0]
            + t**3 * p3[0]
        )
        y = (
            (1 - t) ** 3 * p0[1]
            + 3 * (1 - t) ** 2 * t * p1[1]
            + 3 * (1 - t) * t**2 * p2[1]
            + t**3 * p3[1]
        )
        pts.append((x, y))
    return pts


def parse_path(data: str):
    tokens = re.findall(r"[A-Za-z]|-?\d+(?:\.\d+)?", data)
    i = 0
    cmd = None
    x = y = 0.0
    start = (0.0, 0.0)
    pts = []
    while i < len(tokens):
        if re.match(r"[A-Za-z]", tokens[i]):
            cmd = tokens[i]
            i += 1
        if cmd == "M":
            x = float(tokens[i])
            y = float(tokens[i + 1])
            i += 2
            start = (x, y)
            pts.append((x, y))
            cmd = "L"
        elif cmd == "H":
            x = float(tokens[i])
            i += 1
            pts.append((x, y))
        elif cmd == "V":
            y = float(tokens[i])
            i += 1
            pts.append((x, y))
        elif cmd == "C":
            p0 = (x, y)
            p1 = (float(tokens[i]), float(tokens[i + 1]))
            p2 = (float(tokens[i + 2]), float(tokens[i + 3]))
            p3 = (float(tokens[i + 4]), float(tokens[i + 5]))
            i += 6
            pts.extend(cubic(p0, p1, p2, p3))
            x, y = p3
        elif cmd == "Z" or cmd == "z":
            pts.append(start)
            cmd = None
        else:
            break
    return pts


LOGO_PATHS = [
    ("#1265E1", 1.0, "M13,10 H34 C48.5,10 58,20.2 58,32 C58,45.4 48.3,54 34,54 H13 C9.4,54 7,51.8 7,48.6 C7,45.4 9.4,43.2 13,43.2 H34 C41.7,43.2 47.2,38.9 47.2,32 C47.2,24.6 41.8,20.8 34,20.8 H13 C9.3,20.8 6.8,18.6 6.8,15.4 C6.8,12.2 9.3,10 13,10 Z"),
    ("#2D9CFF", 0.42, "M13,10 H34 C46.5,10 55.4,17.6 57.5,28.1 C53.8,22.9 46.8,19 36,19 H13 C9.8,19 7.5,17.6 6.9,15.2 C7,12.1 9.4,10 13,10 Z"),
    ("#1265E1", 1.0, "M13.5,24 H20.4 C23.5,24 25.4,26 25.4,29.1 V42.9 C25.4,46 23.5,48 20.4,48 H13.5 C10.4,48 8.5,46 8.5,42.9 V29.1 C8.5,26 10.4,24 13.5,24 Z"),
    ("#2D9CFF", 0.32, "M13.5,24 H20.4 C23.5,24 25.4,26 25.4,29.1 V33.8 C21.4,31.9 16.3,31.6 8.5,32.5 V29.1 C8.5,26 10.4,24 13.5,24 Z"),
    ("#FFFFFF", 1.0, "M27,25 H34.8 C41,25 45.8,28.1 45.8,32.4 C45.8,37 41.1,40.1 34.8,40.1 H27 Z"),
]


def logo(size: int, color_override: str | None = None) -> Image.Image:
    scale = 4
    canvas = Image.new("RGBA", (size * scale, size * scale), (0, 0, 0, 0))
    draw = ImageDraw.Draw(canvas)
    s = size * scale / 64
    for color, alpha, data in LOGO_PATHS:
        if color_override and color != "#FFFFFF":
            color = color_override
            alpha = 1.0
        pts = [(x * s, y * s) for x, y in parse_path(data)]
        rgba = tuple(int(color[i : i + 2], 16) for i in (1, 3, 5)) + (int(255 * alpha),)
        if len(pts) > 2:
            draw.polygon(pts, fill=rgba)
    # Keyhole
    key_color = color_override or PRIMARY
    krgb = tuple(int(key_color[i : i + 2], 16) for i in (1, 3, 5))
    draw.ellipse((32.2 * s, 27.0 * s, 38.6 * s, 33.4 * s), fill=(*krgb, 255))
    draw.polygon([(33.7 * s, 33.4 * s), (37.1 * s, 33.4 * s), (38.4 * s, 39 * s), (32.4 * s, 39 * s)], fill=(*krgb, 255))
    return canvas.resize((size, size), Image.Resampling.LANCZOS)


def save_app_icon():
    size = 512
    img = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    shadow = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    sd = ImageDraw.Draw(shadow)
    rounded(sd, (46, 46, 466, 466), 92, (10, 26, 51, 38))
    shadow = shadow.filter(ImageFilter.GaussianBlur(20))
    img.alpha_composite(shadow)
    draw = ImageDraw.Draw(img)
    rounded(draw, (44, 38, 468, 462), 96, WHITE)
    mark = logo(342)
    img.alpha_composite(mark, (85, 84))
    img.save(OUT / "app-icon-512.png")


def save_feature_graphic():
    img = vertical_gradient((1024, 500), NAVY, "#061226")
    radial_glow(img, (290, 410), 360, PRIMARY, 120)
    radial_glow(img, (820, 130), 300, SKY, 80)
    draw = ImageDraw.Draw(img)
    # Horizon arc
    for w, a in [(8, 150), (18, 45), (40, 18)]:
        draw.arc((-120, 300, 580, 880), 190, 350, fill=(45, 156, 255, a), width=w)
    # Document constellation
    for idx, (x, y) in enumerate([(720, 92), (820, 66), (910, 150), (790, 330), (925, 322)]):
        rounded(draw, (x, y, x + 54, y + 70), 8, (232, 242, 255, 36), outline=(103, 195, 255, 100), width=2)
        draw.line((x + 14, y + 24, x + 40, y + 24), fill=(232, 242, 255, 130), width=2)
        draw.line((x + 14, y + 38, x + 36, y + 38), fill=(232, 242, 255, 90), width=2)
    # Logo card
    card = Image.new("RGBA", (188, 188), (0, 0, 0, 0))
    cd = ImageDraw.Draw(card)
    rounded(cd, (8, 8, 180, 180), 42, (255, 255, 255, 246))
    card.alpha_composite(logo(126), (31, 31))
    img.alpha_composite(card, (78, 138))
    text(draw, (305, 148), "DocuFind", 64, WHITE, "bold")
    text(draw, (307, 226), "Everything Important. One Secure Place.", 30, "#E8F2FF", "semibold")
    text(draw, (309, 284), "Secure vaults, smart reminders and private file previews.", 22, "#AFC7E8")
    img.convert("RGB").save(OUT / "feature-graphic-1024x500.png", quality=96)


def phone_base(title: str, subtitle: str | None = None):
    img = Image.new("RGBA", (1080, 1920), SOFT)
    draw = ImageDraw.Draw(img)
    text(draw, (72, 62), "9:30", 30, NAVY, "semibold")
    text(draw, (910, 62), "100%", 26, NAVY, "regular")
    text(draw, (72, 126), title, 54, PRIMARY, "bold")
    if subtitle:
        text(draw, (74, 192), subtitle, 27, SLATE)
    return img, draw


def bottom_nav(draw):
    rounded(draw, (0, 1746, 1080, 1920), 0, WHITE)
    items = [("Home", 102), ("Vault", 306), ("Add", 540), ("Reminders", 760), ("Settings", 970)]
    for label, x in items:
        fill = PRIMARY if label in ("Home", "Add") else "#64748B"
        if label == "Add":
            draw.ellipse((x - 48, 1766, x + 48, 1862), fill=SKY)
            text(draw, (x, 1795), "+", 54, WHITE, "regular", "mm")
        else:
            if label == "Home":
                draw.ellipse((x - 32, 1778, x + 32, 1842), fill=(232, 242, 255, 255), outline=fill, width=5)
            else:
                draw.ellipse((x - 26, 1784, x + 26, 1836), outline=fill, width=5)
        text(draw, (x, 1872), label, 26, fill, "semibold", "mm")


def draw_search(draw, y):
    rounded(draw, (60, y, 1020, y + 118), 34, WHITE, outline="#D7E5F5", width=2)
    draw.ellipse((100, y + 36, 142, y + 78), outline="#64748B", width=6)
    draw.line((136, y + 72, 166, y + 100), fill="#64748B", width=6)
    text(draw, (206, y + 58), "Search documents...", 38, "#1F2937", "regular", "lm")


def category_grid(draw, start_y, categories):
    x0, gap, w, h = 58, 34, 300, 214
    for i, (label, color) in enumerate(categories):
        row, col = divmod(i, 3)
        x = x0 + col * (w + gap)
        y = start_y + row * (h + 32)
        rounded(draw, (x, y, x + w, y + h), 30, WHITE, outline="#D9E5F2", width=2)
        draw.rectangle((x, y + 28, x + 8, y + h - 28), fill=color)
        rounded(draw, (x + 104, y + 34, x + 196, y + 126), 24, color + "22")
        text(draw, (x + 150, y + 80), label[:1], 40, WHITE, "bold", "mm")
        text(draw, (x + 150, y + 158), label, 28, PRIMARY, "bold", "mm")


def save_phone_screenshots():
    phone_dir = OUT / "phone-screenshots"
    phone_dir.mkdir(parents=True, exist_ok=True)
    # 1 onboarding
    img, draw = phone_base("Welcome to DocuFind", "Your life, securely organized.")
    img.alpha_composite(logo(260), (410, 394))
    for cx, cy, r in [(300, 890, 34), (780, 848, 28), (845, 1038, 22)]:
        draw.ellipse((cx - r, cy - r, cx + r, cy + r), fill=(45, 156, 255, 36))
    text(draw, (540, 760), "Secure document vault", 48, PRIMARY, "bold", "mm")
    lines = wrap_text(draw, "Store documents, IDs, reminders, family records and pet records privately on your device.", 780, 34)
    for idx, line in enumerate(lines):
        text(draw, (540, 842 + idx * 48), line, 34, SLATE, "regular", "mm")
    rounded(draw, (80, 1600, 1000, 1718), 34, SKY)
    text(draw, (540, 1658), "Get Started", 34, WHITE, "semibold", "mm")
    img.convert("RGB").save(phone_dir / "01-onboarding.png", quality=96)
    # 2 home
    img = Image.new("RGBA", (1080, 1920), SOFT)
    draw = ImageDraw.Draw(img)
    text(draw, (72, 62), "9:30", 30, NAVY, "semibold")
    text(draw, (910, 62), "100%", 26, NAVY, "regular")
    img.alpha_composite(logo(116), (60, 132))
    text(draw, (206, 134), "DocuFind", 34, SKY, "bold")
    text(draw, (206, 180), "Welcome, Bunty", 50, PRIMARY, "bold")
    text(draw, (206, 246), "Safe records, simple reminders.", 27, SLATE)
    draw_search(draw, 330)
    text(draw, (60, 520), "Quick Access", 42, PRIMARY, "bold")
    category_grid(draw, 600, [("Documents", "#2D9CFF"), ("ID Cards", "#3F51B5"), ("Cards", "#0EA5A4"), ("Medical", "#E91E63"), ("Prescriptions", "#9C27B0"), ("Vaccination", "#00897B"), ("Education", "#7E57C2"), ("Insurance", "#22A455"), ("Vehicle", "#F59E0B"), ("Warranty", "#F8B400"), ("Pets", "#D84315"), ("Family", "#673AB7")])
    bottom_nav(draw)
    img.convert("RGB").save(phone_dir / "02-home-categories.png", quality=96)
    # 3 vault
    img, draw = phone_base("Secure Vault", "Protected records stay organized.")
    rounded(draw, (60, 300, 1020, 520), 36, NAVY)
    img.alpha_composite(logo(110, WHITE), (100, 350))
    text(draw, (250, 366), "Your secure vault is ready", 38, WHITE, "bold")
    text(draw, (252, 426), "PIN and biometric protection for sensitive records.", 25, "#BBD7FF")
    for i, label in enumerate(["Documents", "ID Cards", "Insurance", "Warranty", "Medical"]):
        y = 610 + i * 150
        rounded(draw, (70, y, 1010, y + 112), 28, WHITE, outline="#D7E5F5", width=2)
        rounded(draw, (100, y + 24, 164, y + 88), 18, "#E8F2FF")
        text(draw, (132, y + 56), label[:1], 30, PRIMARY, "bold", "mm")
        text(draw, (196, y + 42), label, 32, NAVY, "bold")
        text(draw, (196, y + 78), "Secure, private, searchable", 22, SLATE)
        text(draw, (940, y + 56), ">", 34, "#94A3B8", "bold", "mm")
    bottom_nav(draw)
    img.convert("RGB").save(phone_dir / "03-secure-vault.png", quality=96)
    # 4 add form
    img, draw = phone_base("Add Record", "Capture details once. Find them fast.")
    fields = ["Category", "Document title", "Document type", "Issue date", "Expiry date", "Notes", "Attachments"]
    for i, label in enumerate(fields):
        y = 310 + i * 148
        text(draw, (76, y - 24), label, 24, SLATE, "semibold")
        rounded(draw, (70, y, 1010, y + 92), 24, WHITE, outline="#D7E5F5", width=2)
        placeholder = "Choose " + label.lower() if label in ("Category", "Document type") else "Tap to add"
        if "date" in label.lower():
            placeholder = "Select date"
        text(draw, (106, y + 46), placeholder, 29, "#64748B", "regular", "lm")
    rounded(draw, (70, 1495, 1010, 1605), 30, PRIMARY)
    text(draw, (540, 1550), "Save Record", 32, WHITE, "semibold", "mm")
    bottom_nav(draw)
    img.convert("RGB").save(phone_dir / "04-add-record.png", quality=96)
    # 5 preview
    img, draw = phone_base("File Preview", "Preview, open, share or delete safely.")
    rounded(draw, (90, 310, 990, 1120), 34, WHITE, outline="#D7E5F5", width=2)
    rounded(draw, (170, 390, 910, 920), 28, "#F8FBFF", outline="#D9E5F2", width=2)
    text(draw, (540, 580), "PDF", 96, PRIMARY, "bold", "mm")
    text(draw, (540, 685), "Preview available", 34, NAVY, "semibold", "mm")
    for i, (label, value) in enumerate([("File name", "insurance-policy.pdf"), ("Size", "2.4 MB"), ("Type", "PDF document"), ("Category", "Insurance")]):
        y = 1180 + i * 76
        text(draw, (110, y), label, 24, "#64748B")
        text(draw, (970, y), value, 25, NAVY, "semibold", "ra")
    for i, label in enumerate(["Open", "Share", "Delete"]):
        x = 90 + i * 310
        rounded(draw, (x, 1530, x + 270, 1624), 26, PRIMARY if label != "Delete" else "#FEE2E2")
        text(draw, (x + 135, 1577), label, 28, WHITE if label != "Delete" else "#B91C1C", "bold", "mm")
    bottom_nav(draw)
    img.convert("RGB").save(phone_dir / "05-file-preview.png", quality=96)
    # 6 reminders
    img, draw = phone_base("Reminders", "Never miss renewals or due dates.")
    for i, (title_value, due, accent) in enumerate([
        ("Passport expiry", "15 days before", PRIMARY),
        ("Medicine refill", "Tomorrow", GREEN),
        ("Vehicle insurance", "7 days before", "#F59E0B"),
        ("Pet vaccination", "Due soon", "#7E57C2"),
    ]):
        y = 320 + i * 190
        rounded(draw, (70, y, 1010, y + 148), 30, WHITE, outline="#D7E5F5", width=2)
        draw.ellipse((110, y + 42, 174, y + 106), fill=accent)
        text(draw, (142, y + 74), "!", 34, WHITE, "bold", "mm")
        text(draw, (210, y + 44), title_value, 34, NAVY, "bold")
        text(draw, (210, y + 92), due, 25, SLATE)
        rounded(draw, (820, y + 48, 950, y + 100), 20, "#E8F2FF")
        text(draw, (885, y + 74), "Done", 22, PRIMARY, "bold", "mm")
    rounded(draw, (70, 1260, 1010, 1370), 30, PRIMARY)
    text(draw, (540, 1315), "Create Custom Reminder", 30, WHITE, "semibold", "mm")
    bottom_nav(draw)
    img.convert("RGB").save(phone_dir / "06-reminders.png", quality=96)
    # 7 family pets
    img, draw = phone_base("Family & Pets", "Important profiles in one place.")
    for i, (section, desc, color) in enumerate([
        ("Family records", "DOB, blood group, emergency details and photos.", PRIMARY),
        ("Pet records", "Vaccines, vet visits, insurance and documents.", "#D84315"),
        ("Emergency contacts", "Fast access to trusted contacts.", GREEN),
    ]):
        y = 330 + i * 290
        rounded(draw, (70, y, 1010, y + 220), 34, WHITE, outline="#D7E5F5", width=2)
        rounded(draw, (116, y + 50, 236, y + 170), 32, color + "22")
        text(draw, (176, y + 110), section[:1], 54, color, "bold", "mm")
        text(draw, (280, y + 62), section, 36, NAVY, "bold")
        for li, line in enumerate(wrap_text(draw, desc, 610, 26)):
            text(draw, (280, y + 116 + li * 36), line, 26, SLATE)
    bottom_nav(draw)
    img.convert("RGB").save(phone_dir / "07-family-pets.png", quality=96)
    # 8 privacy
    img, draw = phone_base("Private by Design", "Your activity insights stay on your device.")
    rounded(draw, (110, 330, 970, 600), 40, NAVY)
    text(draw, (540, 410), "100% private", 52, WHITE, "bold", "mm")
    text(draw, (540, 485), "No ads. No tracking. Local-first storage.", 28, "#BBD7FF", "regular", "mm")
    for i, item in enumerate(["Documents stay on your device", "Encrypted backups you control", "PIN and biometric protection", "Local-only activity insights"]):
        y = 720 + i * 135
        rounded(draw, (90, y, 990, y + 96), 26, WHITE, outline="#D7E5F5", width=2)
        draw.ellipse((124, y + 26, 168, y + 70), fill=GREEN)
        text(draw, (146, y + 48), "✓", 25, WHITE, "bold", "mm")
        text(draw, (205, y + 48), item, 29, NAVY, "semibold", "lm")
    bottom_nav(draw)
    img.convert("RGB").save(phone_dir / "08-privacy-backup.png", quality=96)


def save_tablet_screenshots():
    tablet_dir = OUT / "tablet-screenshots"
    tablet_dir.mkdir(parents=True, exist_ok=True)
    for idx, (title_value, subtitle, cards) in enumerate([
        ("DocuFind Dashboard", "Organize every important record.", ["Documents", "ID Cards", "Insurance", "Vehicle", "Family", "Pets"]),
        ("Secure Vault", "Protected categories with clean details.", ["Documents", "Cards", "Medical", "Prescriptions", "Warranty", "Others"]),
        ("Smart Reminders", "Track renewals, medicines and due dates.", ["Passport expiry", "Insurance renewal", "Pet vaccination", "Warranty expiry", "Medicine refill", "Custom reminder"]),
        ("Preview & Privacy", "Preview files and keep activity local.", ["Image preview", "PDF placeholder", "Encrypted backup", "Local insights", "PIN unlock", "No tracking"]),
    ], start=1):
        img = Image.new("RGBA", (1600, 2560), SOFT)
        draw = ImageDraw.Draw(img)
        img.alpha_composite(logo(150), (96, 110))
        text(draw, (290, 118), "DocuFind", 52, SKY, "bold")
        text(draw, (96, 300), title_value, 76, PRIMARY, "bold")
        text(draw, (100, 396), subtitle, 38, SLATE)
        rounded(draw, (96, 510, 1504, 690), 44, WHITE, outline="#D7E5F5", width=2)
        text(draw, (160, 600), "Search documents, records and reminders...", 44, "#64748B", "regular", "lm")
        for i, card_title in enumerate(cards):
            row, col = divmod(i, 2)
            x = 96 + col * 720
            y = 820 + row * 360
            rounded(draw, (x, y, x + 660, y + 280), 42, WHITE, outline="#D7E5F5", width=2)
            rounded(draw, (x + 46, y + 50, x + 166, y + 170), 32, "#E8F2FF")
            text(draw, (x + 106, y + 110), card_title[:1], 54, PRIMARY, "bold", "mm")
            text(draw, (x + 210, y + 72), card_title, 42, NAVY, "bold")
            text(draw, (x + 210, y + 134), "Secure, searchable and organized", 30, SLATE)
        img.convert("RGB").save(tablet_dir / f"{idx:02d}-{title_value.lower().replace(' ', '-')}.png", quality=96)


def save_chromebook_screenshots():
    chrome_dir = OUT / "chromebook-screenshots"
    chrome_dir.mkdir(parents=True, exist_ok=True)
    for idx, title_value in enumerate(["Dashboard", "Vault", "File Preview", "Reminders"], start=1):
        img = vertical_gradient((1920, 1080), "#F8FBFF", SOFT)
        draw = ImageDraw.Draw(img)
        rounded(draw, (60, 60, 360, 1020), 34, NAVY)
        img.alpha_composite(logo(92, WHITE), (104, 104))
        text(draw, (214, 124), "DocuFind", 35, WHITE, "bold")
        for i, item in enumerate(["Home", "Vault", "Add", "Reminders", "Settings"]):
            y = 260 + i * 90
            fill = (18, 101, 225, 255) if item == title_value or (title_value == "Dashboard" and item == "Home") else (255, 255, 255, 0)
            rounded(draw, (96, y, 320, y + 62), 20, fill)
            text(draw, (130, y + 31), item, 25, WHITE if fill[3] else "#BBD7FF", "semibold", "lm")
        text(draw, (440, 95), f"DocuFind {title_value}", 58, PRIMARY, "bold")
        text(draw, (444, 170), "Secure document organization for everyday records.", 30, SLATE)
        rounded(draw, (440, 250, 1820, 390), 34, WHITE, outline="#D7E5F5", width=2)
        text(draw, (500, 320), "Search documents, IDs, reminders, family and pet records...", 32, "#64748B", "regular", "lm")
        for i in range(6):
            row, col = divmod(i, 3)
            x = 440 + col * 460
            y = 470 + row * 250
            rounded(draw, (x, y, x + 410, y + 190), 30, WHITE, outline="#D7E5F5", width=2)
            text(draw, (x + 48, y + 52), ["Documents", "ID Cards", "Insurance", "Warranty", "Family", "Pets"][i], 32, NAVY, "bold")
            text(draw, (x + 48, y + 105), "Private, organized, searchable", 23, SLATE)
        img.convert("RGB").save(chrome_dir / f"{idx:02d}-{title_value.lower().replace(' ', '-')}.png", quality=96)


def save_index():
    content = """DocuFind Play Store Asset Pack

Upload these files in Play Console > Store listing > Graphics.

App icon:
- app-icon-512.png

Feature graphic:
- feature-graphic-1024x500.png

Phone screenshots:
- phone-screenshots/01-onboarding.png
- phone-screenshots/02-home-categories.png
- phone-screenshots/03-secure-vault.png
- phone-screenshots/04-add-record.png
- phone-screenshots/05-file-preview.png
- phone-screenshots/06-reminders.png
- phone-screenshots/07-family-pets.png
- phone-screenshots/08-privacy-backup.png

Tablet screenshots:
- tablet-screenshots/*.png

Chromebook screenshots:
- chromebook-screenshots/*.png

Android XR screenshots:
- Skip for now unless DocuFind is explicitly distributed to Android XR devices.
"""
    (OUT / "UPLOAD_GUIDE.txt").write_text(content, encoding="utf-8")


def main():
    OUT.mkdir(parents=True, exist_ok=True)
    save_app_icon()
    save_feature_graphic()
    save_phone_screenshots()
    save_tablet_screenshots()
    save_chromebook_screenshots()
    save_index()
    print(f"Generated Play Store assets at {OUT}")


if __name__ == "__main__":
    main()
