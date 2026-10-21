import os
import math
from PIL import Image, ImageDraw, ImageFont

ASSETS_DIR = r"C:\WORK\KidSee\google_play_assets"
os.makedirs(ASSETS_DIR, exist_ok=True)

WIDTH = 1080
HEIGHT = 1920

FONT_BOLD = r"C:\Windows\Fonts\segoeuib.ttf"
FONT_REGULAR = r"C:\Windows\Fonts\segoeui.ttf"

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

def draw_rounded_rect(draw, bbox, radius, fill, outline=None, width=1):
    draw.rounded_rectangle(bbox, radius=radius, fill=fill, outline=outline, width=width)

def draw_phone_mockup(canvas, x, y, w, h, content_drawer):
    draw = ImageDraw.Draw(canvas)
    # Phone shadow
    for i in range(15):
        alpha_offset = 15 - i
        draw.rounded_rectangle([x - i, y - i + 10, x + w + i, y + h + i + 10], radius=50, outline=(15, 23, 42))
    
    # Phone outer frame (Titanium/Dark blue)
    draw.rounded_rectangle([x, y, x + w, y + h], radius=46, fill=(15, 23, 42), outline=(51, 65, 85), width=4)
    # Screen bezel
    screen_padding = 16
    sx = x + screen_padding
    sy = y + screen_padding
    sw = w - 2 * screen_padding
    sh = h - 2 * screen_padding
    draw.rounded_rectangle([sx, sy, sx + sw, sy + sh], radius=36, fill=(10, 15, 30))
    
    # Dynamic Island / Camera notch
    notch_w = 160
    notch_h = 32
    nx = sx + (sw - notch_w) // 2
    ny = sy + 16
    draw.rounded_rectangle([nx, ny, nx + notch_w, ny + notch_h], radius=16, fill=(0, 0, 0))
    
    # Let specific drawer render inside screen area
    content_drawer(canvas, sx, sy, sw, sh)

# ==================== SCREENSHOT 1: LIVE SCREEN ====================
def make_screenshot_1():
    img = create_gradient(WIDTH, HEIGHT, (15, 23, 42), (30, 41, 59))
    draw = ImageDraw.Draw(img)
    
    font_title = ImageFont.truetype(FONT_BOLD, 68)
    font_subtitle = ImageFont.truetype(FONT_REGULAR, 36)
    
    # Header tag
    tag_box = [WIDTH//2 - 160, 100, WIDTH//2 + 160, 150]
    draw.rounded_rectangle(tag_box, radius=25, fill=(37, 99, 235))
    draw.text((WIDTH//2, 125), "LIVE MONITORING", font=ImageFont.truetype(FONT_BOLD, 24), fill=(255, 255, 255), anchor="mm")
    
    # Title & Subtitle
    draw.text((WIDTH//2, 210), "Real-Time Screen View", font=font_title, fill=(255, 255, 255), anchor="mm")
    draw.text((WIDTH//2, 275), "Watch child's device live with ultra-low latency", font=font_subtitle, fill=(148, 163, 184), anchor="mm")
    
    # Phone Mockup
    phone_w = 680
    phone_h = 1380
    px = (WIDTH - phone_w) // 2
    py = 370
    
    def content_1(cv, sx, sy, sw, sh):
        cdraw = ImageDraw.Draw(cv)
        # Status Bar
        cdraw.text((sx + 40, sy + 25), "09:41", font=ImageFont.truetype(FONT_BOLD, 22), fill=(203, 213, 225))
        cdraw.text((sx + sw - 80, sy + 25), "100%", font=ImageFont.truetype(FONT_BOLD, 20), fill=(52, 211, 153))
        
        # KidSee App Bar inside mockup
        cdraw.rectangle([sx, sy + 70, sx + sw, sy + 150], fill=(20, 30, 50))
        cdraw.text((sx + 30, sy + 110), "KidSee Parent", font=ImageFont.truetype(FONT_BOLD, 30), fill=(255, 255, 255), anchor="lm")
        # Online badge
        cdraw.rounded_rectangle([sx + sw - 160, sy + 95, sx + sw - 25, sy + 125], radius=15, fill=(16, 185, 129))
        cdraw.text((sx + sw - 92, sy + 110), "LIVE: 60 FPS", font=ImageFont.truetype(FONT_BOLD, 18), fill=(255, 255, 255), anchor="mm")
        
        # Video stream container
        stream_y = sy + 170
        stream_h = 800
        cdraw.rounded_rectangle([sx + 20, stream_y, sx + sw - 20, stream_y + stream_h], radius=24, fill=(30, 45, 75), outline=(59, 130, 246), width=2)
        
        # Simulated content on child screen (e.g. learning / game)
        cdraw.rounded_rectangle([sx + 40, stream_y + 40, sx + sw - 40, stream_y + 450], radius=16, fill=(40, 60, 95))
        cdraw.text((sx + sw//2, stream_y + 245), "Child Device Screen", font=ImageFont.truetype(FONT_BOLD, 38), fill=(241, 245, 249), anchor="mm")
        cdraw.text((sx + sw//2, stream_y + 305), "Active App: Educational Quiz", font=ImageFont.truetype(FONT_REGULAR, 26), fill=(148, 163, 184), anchor="mm")
        
        # Stats overlay inside stream
        cdraw.rounded_rectangle([sx + 40, stream_y + stream_h - 130, sx + sw - 40, stream_y + stream_h - 30], radius=16, fill=(15, 23, 42))
        cdraw.text((sx + 70, stream_y + stream_h - 80), "Resolution: 1080p | Latency: 42ms | Wi-Fi Local P2P", font=ImageFont.truetype(FONT_REGULAR, 20), fill=(52, 211, 153), anchor="lm")
        
        # Bottom controls in Parent View
        btn_y = stream_y + stream_h + 35
        # Intercom Mic button
        cdraw.rounded_rectangle([sx + 35, btn_y, sx + 225, btn_y + 110], radius=20, fill=(37, 99, 235))
        cdraw.text((sx + 130, btn_y + 45), "Talk (Mic)", font=ImageFont.truetype(FONT_BOLD, 22), fill=(255, 255, 255), anchor="mm")
        cdraw.text((sx + 130, btn_y + 75), "Intercom", font=ImageFont.truetype(FONT_REGULAR, 18), fill=(191, 219, 254), anchor="mm")
        
        # Media pause button
        cdraw.rounded_rectangle([sx + 245, btn_y, sx + 435, btn_y + 110], radius=20, fill=(79, 70, 229))
        cdraw.text((sx + 340, btn_y + 45), "Pause Media", font=ImageFont.truetype(FONT_BOLD, 22), fill=(255, 255, 255), anchor="mm")
        cdraw.text((sx + 340, btn_y + 75), "Playback", font=ImageFont.truetype(FONT_REGULAR, 18), fill=(224, 231, 255), anchor="mm")
        
        # Quick Lock button
        cdraw.rounded_rectangle([sx + 455, btn_y, sx + sw - 35, btn_y + 110], radius=20, fill=(225, 29, 72))
        cdraw.text((sx + 545, btn_y + 45), "Lock Screen", font=ImageFont.truetype(FONT_BOLD, 22), fill=(255, 255, 255), anchor="mm")
        cdraw.text((sx + 545, btn_y + 75), "Instant", font=ImageFont.truetype(FONT_REGULAR, 18), fill=(254, 205, 211), anchor="mm")
        
    draw_phone_mockup(img, px, py, phone_w, phone_h, content_1)
    img.save(os.path.join(ASSETS_DIR, "screenshot_1.png"), format="PNG")
    print("Saved screenshot_1.png")

# ==================== SCREENSHOT 2: MEDIA CONTROLS ====================
def make_screenshot_2():
    img = create_gradient(WIDTH, HEIGHT, (15, 23, 42), (40, 20, 60))
    draw = ImageDraw.Draw(img)
    
    font_title = ImageFont.truetype(FONT_BOLD, 68)
    font_subtitle = ImageFont.truetype(FONT_REGULAR, 36)
    
    # Header tag
    tag_box = [WIDTH//2 - 160, 100, WIDTH//2 + 160, 150]
    draw.rounded_rectangle(tag_box, radius=25, fill=(147, 51, 234))
    draw.text((WIDTH//2, 125), "REMOTE CONTROL", font=ImageFont.truetype(FONT_BOLD, 24), fill=(255, 255, 255), anchor="mm")
    
    # Title & Subtitle
    draw.text((WIDTH//2, 210), "Control Media & Sound", font=font_title, fill=(255, 255, 255), anchor="mm")
    draw.text((WIDTH//2, 275), "Remotely pause video, limit volume, or mute audio", font=font_subtitle, fill=(216, 180, 254), anchor="mm")
    
    phone_w = 680
    phone_h = 1380
    px = (WIDTH - phone_w) // 2
    py = 370
    
    def content_2(cv, sx, sy, sw, sh):
        cdraw = ImageDraw.Draw(cv)
        cdraw.text((sx + 40, sy + 25), "09:41", font=ImageFont.truetype(FONT_BOLD, 22), fill=(203, 213, 225))
        cdraw.rectangle([sx, sy + 70, sx + sw, sy + 150], fill=(25, 20, 45))
        cdraw.text((sx + 30, sy + 110), "KidSee: Media Manager", font=ImageFont.truetype(FONT_BOLD, 30), fill=(255, 255, 255), anchor="lm")
        
        # Now playing card
        card_y = sy + 200
        cdraw.rounded_rectangle([sx + 30, card_y, sx + sw - 30, card_y + 400], radius=24, fill=(45, 30, 75), outline=(168, 85, 247), width=2)
        cdraw.text((sx + sw//2, card_y + 60), "NOW PLAYING ON CHILD DEVICE", font=ImageFont.truetype(FONT_BOLD, 20), fill=(216, 180, 254), anchor="mm")
        cdraw.text((sx + sw//2, card_y + 130), "YouTube / Cartoons", font=ImageFont.truetype(FONT_BOLD, 36), fill=(255, 255, 255), anchor="mm")
        cdraw.text((sx + sw//2, card_y + 185), "Session Duration: 45 min", font=ImageFont.truetype(FONT_REGULAR, 24), fill=(192, 132, 252), anchor="mm")
        
        # Play / Pause big button
        cdraw.rounded_rectangle([sx + sw//2 - 120, card_y + 250, sx + sw//2 + 120, card_y + 350], radius=30, fill=(168, 85, 247))
        cdraw.text((sx + sw//2, card_y + 300), "PAUSE VIDEO", font=ImageFont.truetype(FONT_BOLD, 26), fill=(255, 255, 255), anchor="mm")
        
        # Volume slider card
        vcard_y = card_y + 430
        cdraw.rounded_rectangle([sx + 30, vcard_y, sx + sw - 30, vcard_y + 320], radius=24, fill=(35, 25, 60))
        cdraw.text((sx + 60, vcard_y + 55), "Device Volume Limit", font=ImageFont.truetype(FONT_BOLD, 28), fill=(255, 255, 255), anchor="lm")
        cdraw.text((sx + sw - 60, vcard_y + 55), "40%", font=ImageFont.truetype(FONT_BOLD, 28), fill=(168, 85, 247), anchor="rm")
        
        # Slider track
        cdraw.rounded_rectangle([sx + 60, vcard_y + 110, sx + sw - 60, vcard_y + 130], radius=10, fill=(75, 60, 100))
        # Slider filled
        cdraw.rounded_rectangle([sx + 60, vcard_y + 110, sx + 280, vcard_y + 130], radius=10, fill=(168, 85, 247))
        # Slider thumb
        cdraw.ellipse([sx + 265, vcard_y + 100, sx + 295, vcard_y + 140], fill=(255, 255, 255))
        
        # Mute switch
        cdraw.rounded_rectangle([sx + 60, vcard_y + 180, sx + sw - 60, vcard_y + 270], radius=16, fill=(50, 35, 80))
        cdraw.text((sx + 90, vcard_y + 225), "Mute All Sound Remotely", font=ImageFont.truetype(FONT_BOLD, 24), fill=(255, 255, 255), anchor="lm")
        cdraw.rounded_rectangle([sx + sw - 160, vcard_y + 205, sx + sw - 90, vcard_y + 245], radius=20, fill=(16, 185, 129))
        cdraw.text((sx + sw - 125, vcard_y + 225), "ON", font=ImageFont.truetype(FONT_BOLD, 18), fill=(255, 255, 255), anchor="mm")
        
    draw_phone_mockup(img, px, py, phone_w, phone_h, content_2)
    img.save(os.path.join(ASSETS_DIR, "screenshot_2.png"), format="PNG")
    print("Saved screenshot_2.png")

# ==================== SCREENSHOT 3: INSTANT SCREEN LOCK ====================
def make_screenshot_3():
    img = create_gradient(WIDTH, HEIGHT, (15, 23, 42), (50, 20, 25))
    draw = ImageDraw.Draw(img)
    
    font_title = ImageFont.truetype(FONT_BOLD, 68)
    font_subtitle = ImageFont.truetype(FONT_REGULAR, 36)
    
    # Header tag
    tag_box = [WIDTH//2 - 160, 100, WIDTH//2 + 160, 150]
    draw.rounded_rectangle(tag_box, radius=25, fill=(225, 29, 72))
    draw.text((WIDTH//2, 125), "FOCUS & SAFETY", font=ImageFont.truetype(FONT_BOLD, 24), fill=(255, 255, 255), anchor="mm")
    
    # Title & Subtitle
    draw.text((WIDTH//2, 210), "Instant Screen Lock", font=font_title, fill=(255, 255, 255), anchor="mm")
    draw.text((WIDTH//2, 275), "Pause distractions for homework, dinner, or bedtime", font=font_subtitle, fill=(254, 205, 211), anchor="mm")
    
    phone_w = 680
    phone_h = 1380
    px = (WIDTH - phone_w) // 2
    py = 370
    
    def content_3(cv, sx, sy, sw, sh):
        cdraw = ImageDraw.Draw(cv)
        cdraw.text((sx + 40, sy + 25), "09:41", font=ImageFont.truetype(FONT_BOLD, 22), fill=(203, 213, 225))
        cdraw.rectangle([sx, sy + 70, sx + sw, sy + 150], fill=(45, 15, 20))
        cdraw.text((sx + 30, sy + 110), "KidSee: Screen Lock", font=ImageFont.truetype(FONT_BOLD, 30), fill=(255, 255, 255), anchor="lm")
        
        # Center Lock Shield
        shield_cx = sx + sw // 2
        shield_cy = sy + 380
        cdraw.ellipse([shield_cx - 110, shield_cy - 110, shield_cx + 110, shield_cy + 110], fill=(225, 29, 72))
        cdraw.text((shield_cx, shield_cy - 10), "LOCKED", font=ImageFont.truetype(FONT_BOLD, 32), fill=(255, 255, 255), anchor="mm")
        cdraw.text((shield_cx, shield_cy + 30), "FOCUS MODE", font=ImageFont.truetype(FONT_REGULAR, 20), fill=(254, 205, 211), anchor="mm")
        
        # Status text
        cdraw.text((shield_cx, sy + 560), "Child Device Is Paused", font=ImageFont.truetype(FONT_BOLD, 34), fill=(255, 255, 255), anchor="mm")
        cdraw.text((shield_cx, sy + 610), "Screen interaction disabled by parent", font=ImageFont.truetype(FONT_REGULAR, 24), fill=(244, 63, 94), anchor="mm")
        
        # Timer box
        cdraw.rounded_rectangle([sx + 50, sy + 680, sx + sw - 50, sy + 820], radius=20, fill=(30, 15, 20), outline=(225, 29, 72), width=1)
        cdraw.text((shield_cx, sy + 725), "Lock Duration Remaining", font=ImageFont.truetype(FONT_REGULAR, 22), fill=(254, 205, 211), anchor="mm")
        cdraw.text((shield_cx, sy + 775), "24 : 18", font=ImageFont.truetype(FONT_BOLD, 42), fill=(255, 255, 255), anchor="mm")
        
        # Quick Action Buttons
        cdraw.rounded_rectangle([sx + 50, sy + 860, sx + sw - 50, sy + 950], radius=20, fill=(16, 185, 129))
        cdraw.text((shield_cx, sy + 905), "UNLOCK DEVICE NOW", font=ImageFont.truetype(FONT_BOLD, 26), fill=(255, 255, 255), anchor="mm")
        
    draw_phone_mockup(img, px, py, phone_w, phone_h, content_3)
    img.save(os.path.join(ASSETS_DIR, "screenshot_3.png"), format="PNG")
    print("Saved screenshot_3.png")

# ==================== SCREENSHOT 4: LOCAL P2P PRIVACY ====================
def make_screenshot_4():
    img = create_gradient(WIDTH, HEIGHT, (15, 23, 42), (10, 40, 45))
    draw = ImageDraw.Draw(img)
    
    font_title = ImageFont.truetype(FONT_BOLD, 68)
    font_subtitle = ImageFont.truetype(FONT_REGULAR, 36)
    
    # Header tag
    tag_box = [WIDTH//2 - 160, 100, WIDTH//2 + 160, 150]
    draw.rounded_rectangle(tag_box, radius=25, fill=(13, 148, 136))
    draw.text((WIDTH//2, 125), "PRIVATE & DIRECT", font=ImageFont.truetype(FONT_BOLD, 24), fill=(255, 255, 255), anchor="mm")
    
    # Title & Subtitle
    draw.text((WIDTH//2, 210), "100% Private Local Wi-Fi", font=font_title, fill=(255, 255, 255), anchor="mm")
    draw.text((WIDTH//2, 275), "Direct device-to-device streaming. Zero cloud storage.", font=font_subtitle, fill=(153, 246, 228), anchor="mm")
    
    phone_w = 680
    phone_h = 1380
    px = (WIDTH - phone_w) // 2
    py = 370
    
    def content_4(cv, sx, sy, sw, sh):
        cdraw = ImageDraw.Draw(cv)
        cdraw.text((sx + 40, sy + 25), "09:41", font=ImageFont.truetype(FONT_BOLD, 22), fill=(203, 213, 225))
        cdraw.rectangle([sx, sy + 70, sx + sw, sy + 150], fill=(15, 35, 40))
        cdraw.text((sx + 30, sy + 110), "KidSee: Security & Privacy", font=ImageFont.truetype(FONT_BOLD, 30), fill=(255, 255, 255), anchor="lm")
        
        # Privacy checklist cards
        items = [
            ("No External Cloud Servers", "Data never leaves your local Wi-Fi router"),
            ("Direct P2P Encrypted Stream", "Safe and low latency device handshake"),
            ("No Account Or Login Needed", "Instant connection via local QR code or PIN"),
            ("Zero Advertising Trackers", "Strictly on-device local telemetry")
        ]
        
        y_cursor = sy + 200
        for title, desc in items:
            cdraw.rounded_rectangle([sx + 30, y_cursor, sx + sw - 30, y_cursor + 130], radius=18, fill=(20, 45, 50), outline=(20, 184, 166), width=1)
            # Check circle
            cdraw.ellipse([sx + 55, y_cursor + 45, sx + 95, y_cursor + 85], fill=(20, 184, 166))
            cdraw.text((sx + 75, y_cursor + 65), "V", font=ImageFont.truetype(FONT_BOLD, 20), fill=(255, 255, 255), anchor="mm")
            
            cdraw.text((sx + 120, y_cursor + 45), title, font=ImageFont.truetype(FONT_BOLD, 24), fill=(255, 255, 255), anchor="lm")
            cdraw.text((sx + 120, y_cursor + 85), desc, font=ImageFont.truetype(FONT_REGULAR, 19), fill=(153, 246, 228), anchor="lm")
            y_cursor += 155
            
        # Large bottom badge
        cdraw.rounded_rectangle([sx + 30, y_cursor + 30, sx + sw - 30, y_cursor + 180], radius=24, fill=(13, 148, 136))
        cdraw.text((sx + sw//2, y_cursor + 80), "GDPR & COPPA Compliant", font=ImageFont.truetype(FONT_BOLD, 28), fill=(255, 255, 255), anchor="mm")
        cdraw.text((sx + sw//2, y_cursor + 130), "Your family data stays completely in your hands", font=ImageFont.truetype(FONT_REGULAR, 20), fill=(204, 251, 241), anchor="mm")
        
    draw_phone_mockup(img, px, py, phone_w, phone_h, content_4)
    img.save(os.path.join(ASSETS_DIR, "screenshot_4.png"), format="PNG")
    print("Saved screenshot_4.png")

make_screenshot_1()
make_screenshot_2()
make_screenshot_3()
make_screenshot_4()
print("All 4 screenshots created successfully!")
