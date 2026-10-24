import os
from PIL import Image, ImageDraw, ImageFont

# Paths
BASE_DIR = r"C:\WORK\KidSee"
RES_DIR = os.path.join(BASE_DIR, "app", "src", "main", "res")
GP_DIR = os.path.join(BASE_DIR, "google_play_assets")
GP_RU = os.path.join(GP_DIR, "ru")
GP_EN = os.path.join(GP_DIR, "en")

RES_DRAWABLE = os.path.join(RES_DIR, "drawable")
RES_DRAWABLE_XHDPI = os.path.join(RES_DIR, "drawable-xhdpi")
RES_DRAWABLE_RU = os.path.join(RES_DIR, "drawable-ru")
RES_DRAWABLE_RU_XHDPI = os.path.join(RES_DIR, "drawable-ru-xhdpi")

for d in [GP_DIR, GP_RU, GP_EN, RES_DRAWABLE, RES_DRAWABLE_XHDPI, RES_DRAWABLE_RU, RES_DRAWABLE_RU_XHDPI]:
    os.makedirs(d, exist_ok=True)

ICON_PATH = os.path.join(GP_DIR, "icon_512.png")
FONT_BOLD = r"C:\Windows\Fonts\segoeuib.ttf"
FONT_REGULAR = r"C:\Windows\Fonts\segoeui.ttf"

def create_tv_banner(w, h, is_ru=False, for_store=True):
    # Base background: Vibrant royal blue gradient
    base = Image.new("RGB", (w, h), (24, 70, 180))
    draw = ImageDraw.Draw(base)

    top_c = (20, 55, 155)
    bot_c = (37, 99, 235)
    for x in range(w):
        factor = x / float(w)
        r = int(top_c[0] + (bot_c[0] - top_c[0]) * factor)
        g = int(top_c[1] + (bot_c[1] - top_c[1]) * factor)
        b = int(top_c[2] + (bot_c[2] - top_c[2]) * factor)
        draw.line([(x, 0), (x, h)], fill=(r, g, b))

    # Soft radial glow behind icon and text
    glow_img = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    gdraw = ImageDraw.Draw(glow_img)
    cx, cy = int(w * 0.32), int(h * 0.5)
    max_r = int(h * 0.75)
    step = max(1, int(15 * (h / 720.0)))
    for r in range(max_r, 0, -step):
        alpha = int(45 * (1.0 - r / float(max_r)))
        gdraw.ellipse([cx - r, cy - r, cx + r, cy + r], fill=(96, 165, 250, alpha))

    base = Image.alpha_composite(base.convert("RGBA"), glow_img).convert("RGB")
    draw = ImageDraw.Draw(base)

    scale = h / 720.0

    title_text = "KidScreen"
    sub_text = "Родительский контроль" if is_ru else "Parental Control"
    tag_text = "Безопасный просмотр для ТВ" if is_ru else "Safe Screen Time for TV"

    title_font_size = int(94 * scale)
    sub_font_size = int(42 * scale)
    tag_font_size = int(24 * scale)

    font_title = ImageFont.truetype(FONT_BOLD, title_font_size)
    font_sub = ImageFont.truetype(FONT_BOLD, sub_font_size)
    font_tag = ImageFont.truetype(FONT_REGULAR, tag_font_size)

    tb_title = draw.textbbox((0, 0), title_text, font=font_title)
    tb_sub = draw.textbbox((0, 0), sub_text, font=font_sub)
    tb_tag = draw.textbbox((0, 0), tag_text, font=font_tag) if for_store else (0, 0, 0, 0)

    w_title = tb_title[2] - tb_title[0]
    h_title = tb_title[3] - tb_title[1]

    w_sub = tb_sub[2] - tb_sub[0]
    h_sub = tb_sub[3] - tb_sub[1]

    w_tag = (tb_tag[2] - tb_tag[0]) if for_store else 0
    h_tag = (tb_tag[3] - tb_tag[1]) if for_store else 0

    icon_size = int(290 * scale)
    gap_icon_text = int(55 * scale)

    max_text_w = max(w_title, w_sub, w_tag)
    total_w = icon_size + gap_icon_text + max_text_w

    start_x = (w - total_w) // 2
    gap_title_sub = int(14 * scale)
    gap_sub_tag = int(14 * scale)

    if for_store:
        total_text_h = h_title + gap_title_sub + h_sub + gap_sub_tag + h_tag
    else:
        total_text_h = h_title + gap_title_sub + h_sub

    start_y_text = (h - total_text_h) // 2
    icon_y = (h - icon_size) // 2
    icon_x = start_x

    # Soft dark blue ring / accent behind icon
    circle_pad = int(12 * scale)
    draw.ellipse([icon_x - circle_pad, icon_y - circle_pad,
                  icon_x + icon_size + circle_pad, icon_y + icon_size + circle_pad],
                 fill=(29, 78, 216))

    if os.path.exists(ICON_PATH):
        icon = Image.open(ICON_PATH).convert("RGBA")
        icon = icon.resize((icon_size, icon_size), Image.Resampling.LANCZOS)
        base.paste(icon, (icon_x, icon_y), icon)

    tx = icon_x + icon_size + gap_icon_text

    # Title
    ty = start_y_text
    # Soft shadow
    draw.text((tx + int(2 * scale) - tb_title[0], ty + int(3 * scale) - tb_title[1]),
              title_text, fill=(15, 23, 42), font=font_title)
    draw.text((tx - tb_title[0], ty - tb_title[1]),
              title_text, fill=(255, 255, 255), font=font_title)

    # Subtitle
    sy = ty + h_title + gap_title_sub
    draw.text((tx - tb_sub[0], sy - tb_sub[1]),
              sub_text, fill=(219, 234, 254), font=font_sub)

    # Tagline (for store banner)
    if for_store:
        gy = sy + h_sub + gap_sub_tag
        draw.text((tx - tb_tag[0], gy - tb_tag[1]),
                  tag_text, fill=(147, 197, 253), font=font_tag)

    return base

# 1. Generate Google Play Store Android TV Banners (1280x720, 24-bit RGB)
banner_ru_1280 = create_tv_banner(1280, 720, is_ru=True, for_store=True)
banner_en_1280 = create_tv_banner(1280, 720, is_ru=False, for_store=True)

# Save into RU folder
banner_ru_1280.save(os.path.join(GP_RU, "tv_banner_1280x720.png"), "PNG")
banner_ru_1280.save(os.path.join(GP_RU, "tv_banner.png"), "PNG")

# Save into EN folder
banner_en_1280.save(os.path.join(GP_EN, "tv_banner_1280x720.png"), "PNG")
banner_en_1280.save(os.path.join(GP_EN, "tv_banner.png"), "PNG")

# Save into root assets directory for quick direct access
banner_ru_1280.save(os.path.join(GP_DIR, "tv_banner_1280x720_ru.png"), "PNG")
banner_en_1280.save(os.path.join(GP_DIR, "tv_banner_1280x720_en.png"), "PNG")
banner_ru_1280.save(os.path.join(GP_DIR, "tv_banner_1280x720.png"), "PNG") # default

# 2. Generate Leanback Launcher Banners for APK (320x180)
banner_ru_320 = create_tv_banner(320, 180, is_ru=True, for_store=False)
banner_en_320 = create_tv_banner(320, 180, is_ru=False, for_store=False)

# English / Default app banner
banner_en_320.save(os.path.join(RES_DRAWABLE, "tv_banner.png"), "PNG")
banner_en_320.save(os.path.join(RES_DRAWABLE_XHDPI, "tv_banner.png"), "PNG")

# Russian localized app banner
banner_ru_320.save(os.path.join(RES_DRAWABLE_RU, "tv_banner.png"), "PNG")
banner_ru_320.save(os.path.join(RES_DRAWABLE_RU_XHDPI, "tv_banner.png"), "PNG")

print("All TV banners successfully generated in both Russian and English!")
