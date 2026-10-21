import os
from PIL import Image, ImageDraw, ImageFont

BASE_DIR = r"C:\WORK\KidSee\google_play_assets"
EN_DIR = os.path.join(BASE_DIR, "en")
RU_DIR = os.path.join(BASE_DIR, "ru")
os.makedirs(EN_DIR, exist_ok=True)
os.makedirs(RU_DIR, exist_ok=True)

FONT_BOLD = r"C:\Windows\Fonts\segoeuib.ttf"
FONT_REGULAR = r"C:\Windows\Fonts\segoeui.ttf"

# ==================== 1. GENERATE ICON (512x512) ====================
def generate_icon():
    img_icon = Image.new('RGBA', (512, 512), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img_icon)
    draw.ellipse([16, 16, 496, 496], fill=(51, 153, 255, 255))
    # Wheels
    draw.ellipse([130, 360, 205, 435], fill=(34, 38, 42, 255))
    draw.ellipse([152, 382, 183, 413], fill=(255, 255, 255, 255))
    draw.ellipse([307, 360, 382, 435], fill=(34, 38, 42, 255))
    draw.ellipse([329, 382, 360, 413], fill=(255, 255, 255, 255))
    # Body
    draw.rounded_rectangle([96, 96, 416, 384], radius=110, fill=(255, 255, 255, 255))
    # Visor
    draw.rounded_rectangle([126, 126, 386, 276], radius=70, fill=(51, 153, 255, 255))
    # Pupil
    draw.ellipse([206, 151, 306, 251], fill=(22, 25, 29, 255))
    draw.ellipse([225, 170, 255, 200], fill=(255, 255, 255, 255))
    # Smile
    draw.arc([200, 290, 312, 345], start=10, end=170, fill=(51, 153, 255, 255), width=12)
    
    icon_path = os.path.join(BASE_DIR, "icon_512.png")
    img_icon.save(icon_path, 'PNG')
    return img_icon

# ==================== 2. FEATURE GRAPHIC (1024x500) ====================
def generate_feature_graphic(icon_img, lang="en", out_path=""):
    img = Image.new('RGB', (1024, 500), (246, 248, 252))
    draw = ImageDraw.Draw(img)
    
    # Soft background gradient
    for x in range(1024):
        factor = x / 1024.0
        r = int(240 * (1 - factor) + 215 * factor)
        g = int(245 * (1 - factor) + 230 * factor)
        b = int(255 * (1 - factor) + 252 * factor)
        draw.line([(x, 0), (x, 500)], fill=(r, g, b))
        
    # Decorative circles
    draw.ellipse([640, -60, 1140, 440], fill=(225, 240, 255))
    draw.ellipse([730, 90, 1070, 430], fill=(200, 228, 255))
    
    # Mascot
    mascot_small = icon_img.resize((320, 320), Image.Resampling.LANCZOS)
    img.paste(mascot_small, (650, 90), mascot_small)
    
    font_large = ImageFont.truetype(FONT_BOLD, 64)
    font_sub = ImageFont.truetype(FONT_REGULAR, 25)
    font_sub_bold = ImageFont.truetype(FONT_BOLD, 25)
    font_tag = ImageFont.truetype(FONT_BOLD, 17)
    
    if lang == "en":
        tag_text = "PARENTAL CONTROL"
        bullets = [
            "• Live child screen streaming (up to 60 FPS)",
            "• Remote media playback & volume control",
            "• Instant audio intercom & screen lock",
            "• 100% private: direct local Wi-Fi (no cloud)"
        ]
        tag_w = 260
    else:
        tag_text = "РОДИТЕЛЬСКИЙ КОНТРОЛЬ"
        bullets = [
            "• Живая трансляция экрана ребёнка (до 60 FPS)",
            "• Удалённое управление медиа и звуком",
            "• Мгновенная рация и блокировка экрана",
            "• 100% приватность: прямое Wi-Fi подключение"
        ]
        tag_w = 320
        
    draw.rounded_rectangle([70, 75, 70 + tag_w, 115], radius=16, fill=(37, 99, 235))
    draw.text((70 + tag_w // 2, 95), tag_text, fill=(255, 255, 255), font=font_tag, anchor="mm")
    
    draw.text((70, 140), "KidSee", fill=(15, 23, 42), font=font_large)
    
    y = 235
    for i, b in enumerate(bullets):
        fnt = font_sub_bold if i == 3 else font_sub
        clr = (13, 148, 136) if i == 3 else (51, 65, 85)
        draw.text((70, y), b, fill=clr, font=fnt)
        y += 42
        
    img.save(out_path, 'PNG')
    print(f"Generated feature graphic: {out_path}")

# ==================== 3. PHONE SCREENSHOTS (1080x1920) ====================
def create_gradient(w, h, top_color, bottom_color):
    base = Image.new("RGB", (w, h), top_color)
    top_r, top_g, top_b = top_color
    bot_r, bot_g, bot_b = bottom_color
    for y in range(h):
        factor = y / float(h)
        r = int(top_r + (bot_r - top_r) * factor)
        g = int(top_g + (bot_g - top_g) * factor)
        b = int(top_b + (bot_b - top_b) * factor)
        draw = ImageDraw.Draw(base)
        draw.line([(0, y), (w, y)], fill=(r, g, b))
    return base

def draw_phone_mockup(canvas, x, y, w, h, content_drawer):
    draw = ImageDraw.Draw(canvas)
    for i in range(15):
        draw.rounded_rectangle([x - i, y - i + 10, x + w + i, y + h + i + 10], radius=50, outline=(15, 23, 42))
    draw.rounded_rectangle([x, y, x + w, y + h], radius=46, fill=(15, 23, 42), outline=(51, 65, 85), width=4)
    screen_padding = 16
    sx = x + screen_padding
    sy = y + screen_padding
    sw = w - 2 * screen_padding
    sh = h - 2 * screen_padding
    draw.rounded_rectangle([sx, sy, sx + sw, sy + sh], radius=36, fill=(10, 15, 30))
    # Notch
    notch_w = 160
    notch_h = 32
    nx = sx + (sw - notch_w) // 2
    ny = sy + 16
    draw.rounded_rectangle([nx, ny, nx + notch_w, ny + notch_h], radius=16, fill=(0, 0, 0))
    content_drawer(canvas, sx, sy, sw, sh)

def generate_screenshots(lang="en", target_dir=""):
    W, H = 1080, 1920
    phone_w, phone_h = 680, 1380
    px = (W - phone_w) // 2
    py = 370
    
    font_title = ImageFont.truetype(FONT_BOLD, 64)
    font_subtitle = ImageFont.truetype(FONT_REGULAR, 34)
    font_tag = ImageFont.truetype(FONT_BOLD, 22)
    font_clock = ImageFont.truetype(FONT_BOLD, 22)
    font_card_t = ImageFont.truetype(FONT_BOLD, 30)
    font_badge = ImageFont.truetype(FONT_BOLD, 18)
    
    # ------------------ S1: LIVE SCREEN ------------------
    img1 = create_gradient(W, H, (15, 23, 42), (30, 41, 59))
    d1 = ImageDraw.Draw(img1)
    
    if lang == "en":
        t_tag, t_main, t_sub = "LIVE MONITORING", "Real-Time Screen View", "Watch child device live with ultra-low latency"
        app_bar, live_bdg = "KidSee Parent", "LIVE: 60 FPS"
        scr_t, scr_sub = "Child Device Screen", "Active App: Educational Quiz"
        stat_t = "Resolution: 1080p | Latency: 42ms | Wi-Fi Local P2P"
        btn1_t, btn1_s = "Talk (Mic)", "Intercom"
        btn2_t, btn2_s = "Pause Media", "Playback"
        btn3_t, btn3_s = "Lock Screen", "Instant"
        tag_w = 320
    else:
        t_tag, t_main, t_sub = "ПРЯМАЯ ТРАНСЛЯЦИЯ", "Просмотр экрана онлайн", "Наблюдайте за экраном ребёнка в 60 FPS"
        app_bar, live_bdg = "KidSee Родитель", "ОНЛАЙН: 60 FPS"
        scr_t, scr_sub = "Экран устройства ребёнка", "Активно: Обучающие уроки"
        stat_t = "Разрешение: 1080p | Задержка: 42мс | Wi-Fi P2P"
        btn1_t, btn1_s = "Рация", "Голос"
        btn2_t, btn2_s = "Пауза", "Медиа"
        btn3_t, btn3_s = "Блок", "Экран"
        tag_w = 360

    d1.rounded_rectangle([W//2 - tag_w//2, 100, W//2 + tag_w//2, 150], radius=25, fill=(37, 99, 235))
    d1.text((W//2, 125), t_tag, font=font_tag, fill=(255, 255, 255), anchor="mm")
    d1.text((W//2, 210), t_main, font=font_title, fill=(255, 255, 255), anchor="mm")
    d1.text((W//2, 275), t_sub, font=font_subtitle, fill=(148, 163, 184), anchor="mm")
    
    def content_1(cv, sx, sy, sw, sh):
        cdraw = ImageDraw.Draw(cv)
        cdraw.text((sx + 40, sy + 25), "09:41", font=font_clock, fill=(203, 213, 225))
        cdraw.text((sx + sw - 80, sy + 25), "100%", font=font_clock, fill=(52, 211, 153))
        cdraw.rectangle([sx, sy + 70, sx + sw, sy + 150], fill=(20, 30, 50))
        cdraw.text((sx + 30, sy + 110), app_bar, font=font_card_t, fill=(255, 255, 255), anchor="lm")
        cdraw.rounded_rectangle([sx + sw - 170, sy + 95, sx + sw - 20, sy + 125], radius=15, fill=(16, 185, 129))
        cdraw.text((sx + sw - 95, sy + 110), live_bdg, font=font_badge, fill=(255, 255, 255), anchor="mm")
        
        stream_y = sy + 170
        stream_h = 800
        cdraw.rounded_rectangle([sx + 20, stream_y, sx + sw - 20, stream_y + stream_h], radius=24, fill=(30, 45, 75), outline=(59, 130, 246), width=2)
        cdraw.rounded_rectangle([sx + 40, stream_y + 40, sx + sw - 40, stream_y + 450], radius=16, fill=(40, 60, 95))
        cdraw.text((sx + sw//2, stream_y + 245), scr_t, font=ImageFont.truetype(FONT_BOLD, 36), fill=(241, 245, 249), anchor="mm")
        cdraw.text((sx + sw//2, stream_y + 305), scr_sub, font=ImageFont.truetype(FONT_REGULAR, 26), fill=(148, 163, 184), anchor="mm")
        
        cdraw.rounded_rectangle([sx + 40, stream_y + stream_h - 130, sx + sw - 40, stream_y + stream_h - 30], radius=16, fill=(15, 23, 42))
        cdraw.text((sx + 55, stream_y + stream_h - 80), stat_t, font=ImageFont.truetype(FONT_REGULAR, 20), fill=(52, 211, 153), anchor="lm")
        
        btn_y = stream_y + stream_h + 35
        # 3 action buttons
        cdraw.rounded_rectangle([sx + 30, btn_y, sx + 225, btn_y + 110], radius=20, fill=(37, 99, 235))
        cdraw.text((sx + 127, btn_y + 45), btn1_t, font=ImageFont.truetype(FONT_BOLD, 22), fill=(255, 255, 255), anchor="mm")
        cdraw.text((sx + 127, btn_y + 75), btn1_s, font=ImageFont.truetype(FONT_REGULAR, 18), fill=(191, 219, 254), anchor="mm")
        
        cdraw.rounded_rectangle([sx + 240, btn_y, sx + 435, btn_y + 110], radius=20, fill=(79, 70, 229))
        cdraw.text((sx + 337, btn_y + 45), btn2_t, font=ImageFont.truetype(FONT_BOLD, 22), fill=(255, 255, 255), anchor="mm")
        cdraw.text((sx + 337, btn_y + 75), btn2_s, font=ImageFont.truetype(FONT_REGULAR, 18), fill=(224, 231, 255), anchor="mm")
        
        cdraw.rounded_rectangle([sx + 450, btn_y, sx + sw - 30, btn_y + 110], radius=20, fill=(225, 29, 72))
        cdraw.text((sx + 542, btn_y + 45), btn3_t, font=ImageFont.truetype(FONT_BOLD, 22), fill=(255, 255, 255), anchor="mm")
        cdraw.text((sx + 542, btn_y + 75), btn3_s, font=ImageFont.truetype(FONT_REGULAR, 18), fill=(254, 205, 211), anchor="mm")

    draw_phone_mockup(img1, px, py, phone_w, phone_h, content_1)
    img1.save(os.path.join(target_dir, "screenshot_1.png"), "PNG")
    
    # ------------------ S2: MEDIA CONTROL ------------------
    img2 = create_gradient(W, H, (15, 23, 42), (40, 20, 60))
    d2 = ImageDraw.Draw(img2)
    
    if lang == "en":
        t_tag, t_main, t_sub = "REMOTE CONTROL", "Control Media & Sound", "Remotely pause video, limit volume, or mute audio"
        card_sub, now_pl, dur_t = "NOW PLAYING ON CHILD DEVICE", "YouTube / Cartoons", "Session Duration: 45 min"
        pause_btn = "PAUSE VIDEO"
        vol_t, mute_t = "Device Volume Limit", "Mute All Sound Remotely"
        tag_w = 320
    else:
        t_tag, t_main, t_sub = "УДАЛЕННЫЙ КОНТРОЛЬ", "Управление медиа и звуком", "Ставьте видео на паузу и управляйте громкостью"
        card_sub, now_pl, dur_t = "СЕЙЧАС ВОСПРОИЗВОДИТСЯ", "YouTube / Мультфильмы", "Длительность: 45 мин"
        pause_btn = "ПАУЗА ВИДЕО"
        vol_t, mute_t = "Ограничение громкости", "Отключить весь звук"
        tag_w = 380

    d2.rounded_rectangle([W//2 - tag_w//2, 100, W//2 + tag_w//2, 150], radius=25, fill=(147, 51, 234))
    d2.text((W//2, 125), t_tag, font=font_tag, fill=(255, 255, 255), anchor="mm")
    d2.text((W//2, 210), t_main, font=font_title, fill=(255, 255, 255), anchor="mm")
    d2.text((W//2, 275), t_sub, font=font_subtitle, fill=(216, 180, 254), anchor="mm")
    
    def content_2(cv, sx, sy, sw, sh):
        cdraw = ImageDraw.Draw(cv)
        cdraw.text((sx + 40, sy + 25), "09:41", font=font_clock, fill=(203, 213, 225))
        cdraw.rectangle([sx, sy + 70, sx + sw, sy + 150], fill=(25, 20, 45))
        cdraw.text((sx + 30, sy + 110), "KidSee Media" if lang == "en" else "KidSee Медиа", font=font_card_t, fill=(255, 255, 255), anchor="lm")
        
        card_y = sy + 200
        cdraw.rounded_rectangle([sx + 30, card_y, sx + sw - 30, card_y + 400], radius=24, fill=(45, 30, 75), outline=(168, 85, 247), width=2)
        cdraw.text((sx + sw//2, card_y + 60), card_sub, font=ImageFont.truetype(FONT_BOLD, 20), fill=(216, 180, 254), anchor="mm")
        cdraw.text((sx + sw//2, card_y + 130), now_pl, font=ImageFont.truetype(FONT_BOLD, 36), fill=(255, 255, 255), anchor="mm")
        cdraw.text((sx + sw//2, card_y + 185), dur_t, font=ImageFont.truetype(FONT_REGULAR, 24), fill=(192, 132, 252), anchor="mm")
        
        cdraw.rounded_rectangle([sx + sw//2 - 140, card_y + 250, sx + sw//2 + 140, card_y + 350], radius=30, fill=(168, 85, 247))
        cdraw.text((sx + sw//2, card_y + 300), pause_btn, font=ImageFont.truetype(FONT_BOLD, 26), fill=(255, 255, 255), anchor="mm")
        
        vcard_y = card_y + 430
        cdraw.rounded_rectangle([sx + 30, vcard_y, sx + sw - 30, vcard_y + 320], radius=24, fill=(35, 25, 60))
        cdraw.text((sx + 60, vcard_y + 55), vol_t, font=ImageFont.truetype(FONT_BOLD, 26), fill=(255, 255, 255), anchor="lm")
        cdraw.text((sx + sw - 60, vcard_y + 55), "40%", font=ImageFont.truetype(FONT_BOLD, 26), fill=(168, 85, 247), anchor="rm")
        
        cdraw.rounded_rectangle([sx + 60, vcard_y + 110, sx + sw - 60, vcard_y + 130], radius=10, fill=(75, 60, 100))
        cdraw.rounded_rectangle([sx + 60, vcard_y + 110, sx + 280, vcard_y + 130], radius=10, fill=(168, 85, 247))
        cdraw.ellipse([sx + 265, vcard_y + 100, sx + 295, vcard_y + 140], fill=(255, 255, 255))
        
        cdraw.rounded_rectangle([sx + 60, vcard_y + 180, sx + sw - 60, vcard_y + 270], radius=16, fill=(50, 35, 80))
        cdraw.text((sx + 90, vcard_y + 225), mute_t, font=ImageFont.truetype(FONT_BOLD, 24), fill=(255, 255, 255), anchor="lm")
        cdraw.rounded_rectangle([sx + sw - 160, vcard_y + 205, sx + sw - 90, vcard_y + 245], radius=20, fill=(16, 185, 129))
        cdraw.text((sx + sw - 125, vcard_y + 225), "ВКЛ" if lang == "ru" else "ON", font=ImageFont.truetype(FONT_BOLD, 18), fill=(255, 255, 255), anchor="mm")

    draw_phone_mockup(img2, px, py, phone_w, phone_h, content_2)
    img2.save(os.path.join(target_dir, "screenshot_2.png"), "PNG")

    # ------------------ S3: INSTANT SCREEN LOCK ------------------
    img3 = create_gradient(W, H, (15, 23, 42), (50, 20, 25))
    d3 = ImageDraw.Draw(img3)
    
    if lang == "en":
        t_tag, t_main, t_sub = "FOCUS & SAFETY", "Instant Screen Lock", "Pause distractions for homework, dinner, or bedtime"
        shld_t, shld_s = "LOCKED", "FOCUS MODE"
        dev_t, dev_s = "Child Device Is Paused", "Screen interaction disabled by parent"
        tim_t, unlk_btn = "Lock Duration Remaining", "UNLOCK DEVICE NOW"
        tag_w = 320
    else:
        t_tag, t_main, t_sub = "ФОКУС И ОТДЫХ", "Мгновенная блокировка", "Сделайте паузу для уроков, ужина или сна"
        shld_t, shld_s = "БЛОКИРОВКА", "РЕЖИМ ФОКУСА"
        dev_t, dev_s = "Устройство на паузе", "Экран отключён родителем"
        tim_t, unlk_btn = "Осталось времени", "РАЗБЛОКИРОВАТЬ СЕЙЧАС"
        tag_w = 340

    d3.rounded_rectangle([W//2 - tag_w//2, 100, W//2 + tag_w//2, 150], radius=25, fill=(225, 29, 72))
    d3.text((W//2, 125), t_tag, font=font_tag, fill=(255, 255, 255), anchor="mm")
    d3.text((W//2, 210), t_main, font=font_title, fill=(255, 255, 255), anchor="mm")
    d3.text((W//2, 275), t_sub, font=font_subtitle, fill=(254, 205, 211), anchor="mm")
    
    def content_3(cv, sx, sy, sw, sh):
        cdraw = ImageDraw.Draw(cv)
        cdraw.text((sx + 40, sy + 25), "09:41", font=font_clock, fill=(203, 213, 225))
        cdraw.rectangle([sx, sy + 70, sx + sw, sy + 150], fill=(45, 15, 20))
        cdraw.text((sx + 30, sy + 110), "KidSee Lock" if lang == "en" else "KidSee Защита", font=font_card_t, fill=(255, 255, 255), anchor="lm")
        
        shield_cx = sx + sw // 2
        shield_cy = sy + 380
        cdraw.ellipse([shield_cx - 110, shield_cy - 110, shield_cx + 110, shield_cy + 110], fill=(225, 29, 72))
        cdraw.text((shield_cx, shield_cy - 10), shld_t, font=ImageFont.truetype(FONT_BOLD, 28), fill=(255, 255, 255), anchor="mm")
        cdraw.text((shield_cx, shield_cy + 30), shld_s, font=ImageFont.truetype(FONT_REGULAR, 20), fill=(254, 205, 211), anchor="mm")
        
        cdraw.text((shield_cx, sy + 560), dev_t, font=ImageFont.truetype(FONT_BOLD, 34), fill=(255, 255, 255), anchor="mm")
        cdraw.text((shield_cx, sy + 610), dev_s, font=ImageFont.truetype(FONT_REGULAR, 24), fill=(244, 63, 94), anchor="mm")
        
        cdraw.rounded_rectangle([sx + 50, sy + 680, sx + sw - 50, sy + 820], radius=20, fill=(30, 15, 20), outline=(225, 29, 72), width=1)
        cdraw.text((shield_cx, sy + 725), tim_t, font=ImageFont.truetype(FONT_REGULAR, 22), fill=(254, 205, 211), anchor="mm")
        cdraw.text((shield_cx, sy + 775), "24 : 18", font=ImageFont.truetype(FONT_BOLD, 42), fill=(255, 255, 255), anchor="mm")
        
        cdraw.rounded_rectangle([sx + 50, sy + 860, sx + sw - 50, sy + 950], radius=20, fill=(16, 185, 129))
        cdraw.text((shield_cx, sy + 905), unlk_btn, font=ImageFont.truetype(FONT_BOLD, 24), fill=(255, 255, 255), anchor="mm")

    draw_phone_mockup(img3, px, py, phone_w, phone_h, content_3)
    img3.save(os.path.join(target_dir, "screenshot_3.png"), "PNG")

    # ------------------ S4: LOCAL P2P PRIVACY ------------------
    img4 = create_gradient(W, H, (15, 23, 42), (10, 40, 45))
    d4 = ImageDraw.Draw(img4)
    
    if lang == "en":
        t_tag, t_main, t_sub = "PRIVATE & DIRECT", "100% Private Local Wi-Fi", "Direct device-to-device streaming. Zero cloud storage."
        items = [
            ("No External Cloud Servers", "Data never leaves your local Wi-Fi router"),
            ("Direct P2P Encrypted Stream", "Safe and low latency device handshake"),
            ("No Account Or Login Needed", "Instant connection via local QR code or PIN"),
            ("Zero Advertising Trackers", "Strictly on-device local telemetry")
        ]
        b_tit, b_sub = "GDPR & COPPA Compliant", "Your family data stays completely in your hands"
        tag_w = 320
    else:
        t_tag, t_main, t_sub = "ПРИВАТНОСТЬ И P2P", "100% локальная сеть Wi-Fi", "Прямая передача без внешних облачных серверов"
        items = [
            ("Без облачных серверов", "Данные не покидают домашний роутер"),
            ("Прямое P2P шифрование", "Безопасная передача с минимальным пингом"),
            ("Без аккаунтов и логинов", "Быстрое подключение по коду или QR"),
            ("Без сторонней рекламы", "Исключительно локальная работа")
        ]
        b_tit, b_sub = "Полная безопасность семьи", "Видеопоток и звук остаются только у вас"
        tag_w = 360

    d4.rounded_rectangle([W//2 - tag_w//2, 100, W//2 + tag_w//2, 150], radius=25, fill=(13, 148, 136))
    d4.text((W//2, 125), t_tag, font=font_tag, fill=(255, 255, 255), anchor="mm")
    d4.text((W//2, 210), t_main, font=font_title, fill=(255, 255, 255), anchor="mm")
    d4.text((W//2, 275), t_sub, font=font_subtitle, fill=(153, 246, 228), anchor="mm")
    
    def content_4(cv, sx, sy, sw, sh):
        cdraw = ImageDraw.Draw(cv)
        cdraw.text((sx + 40, sy + 25), "09:41", font=font_clock, fill=(203, 213, 225))
        cdraw.rectangle([sx, sy + 70, sx + sw, sy + 150], fill=(15, 35, 40))
        cdraw.text((sx + 30, sy + 110), "KidSee Privacy" if lang == "en" else "KidSee Приватность", font=font_card_t, fill=(255, 255, 255), anchor="lm")
        
        y_cursor = sy + 200
        for title, desc in items:
            cdraw.rounded_rectangle([sx + 30, y_cursor, sx + sw - 30, y_cursor + 130], radius=18, fill=(20, 45, 50), outline=(20, 184, 166), width=1)
            cdraw.ellipse([sx + 55, y_cursor + 45, sx + 95, y_cursor + 85], fill=(20, 184, 166))
            cdraw.text((sx + 75, y_cursor + 65), "V", font=ImageFont.truetype(FONT_BOLD, 20), fill=(255, 255, 255), anchor="mm")
            
            cdraw.text((sx + 120, y_cursor + 45), title, font=ImageFont.truetype(FONT_BOLD, 24), fill=(255, 255, 255), anchor="lm")
            cdraw.text((sx + 120, y_cursor + 85), desc, font=ImageFont.truetype(FONT_REGULAR, 19), fill=(153, 246, 228), anchor="lm")
            y_cursor += 155
            
        cdraw.rounded_rectangle([sx + 30, y_cursor + 30, sx + sw - 30, y_cursor + 180], radius=24, fill=(13, 148, 136))
        cdraw.text((sx + sw//2, y_cursor + 80), b_tit, font=ImageFont.truetype(FONT_BOLD, 26), fill=(255, 255, 255), anchor="mm")
        cdraw.text((sx + sw//2, y_cursor + 130), b_sub, font=ImageFont.truetype(FONT_REGULAR, 20), fill=(204, 251, 241), anchor="mm")

    draw_phone_mockup(img4, px, py, phone_w, phone_h, content_4)
    img4.save(os.path.join(target_dir, "screenshot_4.png"), "PNG")
    print(f"Generated 4 screenshots for {lang} in {target_dir}")

# EXECUTE ALL
icon = generate_icon()

# Generate Feature Graphics
generate_feature_graphic(icon, "en", os.path.join(EN_DIR, "feature_graphic.png"))
generate_feature_graphic(icon, "ru", os.path.join(RU_DIR, "feature_graphic.png"))
# Also update root feature graphic to EN default (for en-US default store listing)
generate_feature_graphic(icon, "en", os.path.join(BASE_DIR, "feature_graphic_1024x500.png"))
generate_feature_graphic(icon, "ru", os.path.join(BASE_DIR, "feature_graphic_ru_1024x500.png"))

# Generate Screenshots
generate_screenshots("en", EN_DIR)
generate_screenshots("ru", RU_DIR)
# Update root screenshots to EN default
generate_screenshots("en", BASE_DIR)

print("ALL ASSETS GENERATED FOR BOTH EN AND RU!")
