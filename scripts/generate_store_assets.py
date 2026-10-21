import os
from PIL import Image, ImageDraw, ImageFont

os.makedirs('google_play_assets', exist_ok=True)

# 1. Generate 512x512 High-Res Icon
img_icon = Image.new('RGBA', (512, 512), (0, 0, 0, 0))
draw = ImageDraw.Draw(img_icon)

# Base background circle
draw.ellipse([16, 16, 496, 496], fill=(51, 153, 255, 255))

# Wheels
# Left wheel
draw.ellipse([130, 360, 205, 435], fill=(34, 38, 42, 255))
draw.ellipse([152, 382, 183, 413], fill=(255, 255, 255, 255))
# Right wheel
draw.ellipse([307, 360, 382, 435], fill=(34, 38, 42, 255))
draw.ellipse([329, 382, 360, 413], fill=(255, 255, 255, 255))

# Mascot White Body
draw.rounded_rectangle([96, 96, 416, 384], radius=110, fill=(255, 255, 255, 255))

# Eye Blue Visor
draw.rounded_rectangle([126, 126, 386, 276], radius=70, fill=(51, 153, 255, 255))

# Eye pupil
draw.ellipse([206, 151, 306, 251], fill=(22, 25, 29, 255))
# Eye highlight
draw.ellipse([225, 170, 255, 200], fill=(255, 255, 255, 255))

# Smile
draw.arc([200, 290, 312, 345], start=10, end=170, fill=(51, 153, 255, 255), width=12)

img_icon.save('google_play_assets/icon_512.png', 'PNG')
print('Icon 512x512 created successfully at google_play_assets/icon_512.png')

# 2. Generate 1024x500 Feature Graphic
img_feat = Image.new('RGB', (1024, 500), (246, 248, 252))
draw_f = ImageDraw.Draw(img_feat)

# Soft gradient background
for x in range(1024):
    factor = x / 1024.0
    r = int(242 * (1 - factor) + 215 * factor)
    g = int(247 * (1 - factor) + 235 * factor)
    b = int(255 * (1 - factor) + 255 * factor)
    draw_f.line([(x, 0), (x, 500)], fill=(r, g, b))

# Decorative background circles
draw_f.ellipse([640, -60, 1140, 440], fill=(225, 240, 255, 255))
draw_f.ellipse([730, 90, 1070, 430], fill=(200, 228, 255, 255))

# Mascot on the right
mascot_small = img_icon.resize((320, 320), Image.Resampling.LANCZOS)
img_feat.paste(mascot_small, (640, 90), mascot_small)

# Text / Titles on the left
try:
    font_large = ImageFont.truetype("arialbd.ttf", 64)
    font_sub = ImageFont.truetype("arial.ttf", 26)
    font_tag = ImageFont.truetype("arialbd.ttf", 16)
except:
    font_large = ImageFont.load_default()
    font_sub = ImageFont.load_default()
    font_tag = ImageFont.load_default()

# Badge: РОДИТЕЛЬСКИЙ КОНТРОЛЬ
draw_f.rounded_rectangle([70, 85, 340, 120], radius=16, fill=(51, 153, 255, 255))
draw_f.text((85, 94), "РОДИТЕЛЬСКИЙ КОНТРОЛЬ", fill=(255, 255, 255), font=font_tag)

# Title: KidSee
draw_f.text((70, 140), "KidSee", fill=(22, 25, 29), font=font_large)

# Bullet points
draw_f.text((70, 235), "• Живая трансляция экрана ребенка", fill=(50, 60, 70), font=font_sub)
draw_f.text((70, 275), "• Управление медиа и видеоплеерами", fill=(50, 60, 70), font=font_sub)
draw_f.text((70, 315), "• Мгновенная рация и блокировка", fill=(50, 60, 70), font=font_sub)
draw_f.text((70, 355), "• 100% приватность: работа в Wi-Fi сети", fill=(0, 140, 90), font=font_sub)

img_feat.save('google_play_assets/feature_graphic_1024x500.png', 'PNG')
print('Feature graphic 1024x500 created successfully at google_play_assets/feature_graphic_1024x500.png')
