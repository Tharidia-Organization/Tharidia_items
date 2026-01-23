"""
Generate a bitmap font texture for Crimson Text font for Minecraft.
Downloads the font from Google Fonts and creates a PNG texture + JSON config.
"""

import os
import urllib.request
import zipfile
from PIL import Image, ImageDraw, ImageFont
import json

# Font settings
FONT_SIZE = 16  # Size to render each character
CHARS_PER_ROW = 16
CHAR_WIDTH = 8   # Width per character cell in texture
CHAR_HEIGHT = 16  # Height per character cell in texture

# Characters to include (ASCII printable + extended)
CHARS = ''.join(chr(i) for i in range(32, 127))  # Basic ASCII
CHARS += "àèìòùáéíóúâêîôûäëïöüãñõçÀÈÌÒÙÁÉÍÓÚÂÊÎÔÛÄËÏÖÜÃÑÕÇ"  # Accented chars

def download_font():
    """Download Crimson Text font from Google Fonts."""
    font_url = "https://fonts.google.com/download?family=Crimson%20Text"
    zip_path = os.path.join(os.path.dirname(__file__), "crimson_text.zip")
    font_dir = os.path.join(os.path.dirname(__file__), "fonts")

    os.makedirs(font_dir, exist_ok=True)

    # Check if already downloaded
    font_path = os.path.join(font_dir, "CrimsonText-Regular.ttf")
    if os.path.exists(font_path):
        print(f"Font already exists: {font_path}")
        return font_path

    print("Downloading Crimson Text font...")
    try:
        urllib.request.urlretrieve(font_url, zip_path)

        # Extract
        with zipfile.ZipFile(zip_path, 'r') as zip_ref:
            zip_ref.extractall(font_dir)

        os.remove(zip_path)

        # Find the regular font file
        for root, dirs, files in os.walk(font_dir):
            for file in files:
                if "Regular" in file and file.endswith(".ttf"):
                    return os.path.join(root, file)

        # Fallback to any ttf
        for root, dirs, files in os.walk(font_dir):
            for file in files:
                if file.endswith(".ttf"):
                    return os.path.join(root, file)

    except Exception as e:
        print(f"Error downloading font: {e}")
        print("Please manually download Crimson Text and place the .ttf file in scripts/fonts/")
        return None

    return None

def generate_bitmap_font(font_path):
    """Generate bitmap font texture from TTF."""
    if not font_path or not os.path.exists(font_path):
        print("Font file not found. Using fallback.")
        # Try system font as fallback
        try:
            font = ImageFont.truetype("times.ttf", FONT_SIZE)
        except:
            font = ImageFont.load_default()
    else:
        font = ImageFont.truetype(font_path, FONT_SIZE)

    # Calculate texture dimensions
    num_chars = len(CHARS)
    rows = (num_chars + CHARS_PER_ROW - 1) // CHARS_PER_ROW
    tex_width = CHARS_PER_ROW * CHAR_WIDTH
    tex_height = rows * CHAR_HEIGHT

    # Ensure power of 2 for Minecraft
    tex_width = max(128, tex_width)
    tex_height = max(128, tex_height)

    # Create image
    img = Image.new('RGBA', (tex_width, tex_height), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)

    # Track character widths for proportional font
    char_widths = {}

    for i, char in enumerate(CHARS):
        row = i // CHARS_PER_ROW
        col = i % CHARS_PER_ROW

        x = col * CHAR_WIDTH
        y = row * CHAR_HEIGHT

        # Get character bounding box
        try:
            bbox = font.getbbox(char)
            char_w = bbox[2] - bbox[0] if bbox else CHAR_WIDTH
        except:
            char_w = CHAR_WIDTH

        char_widths[char] = min(char_w, CHAR_WIDTH)

        # Draw character
        draw.text((x, y), char, font=font, fill=(255, 255, 255, 255))

    return img, char_widths

def generate_font_json(char_widths):
    """Generate Minecraft font JSON configuration."""
    # Build the character string for the bitmap provider
    # Minecraft expects rows of 16 characters
    rows = []
    for i in range(0, len(CHARS), CHARS_PER_ROW):
        row_chars = CHARS[i:i+CHARS_PER_ROW]
        # Pad with spaces if needed
        while len(row_chars) < CHARS_PER_ROW:
            row_chars += ' '
        rows.append(row_chars)

    font_json = {
        "providers": [
            {
                "type": "bitmap",
                "file": "tharidiathings:font/crimson_text.png",
                "ascent": 7,
                "height": CHAR_HEIGHT,
                "chars": rows
            }
        ]
    }

    return font_json

def main():
    script_dir = os.path.dirname(__file__)

    # Download font
    font_path = download_font()

    # Generate bitmap
    print("Generating bitmap font texture...")
    img, char_widths = generate_bitmap_font(font_path)

    # Output paths
    texture_dir = os.path.join(script_dir, '..', 'src', 'main', 'resources',
                               'assets', 'tharidiathings', 'textures', 'font')
    font_dir = os.path.join(script_dir, '..', 'src', 'main', 'resources',
                            'assets', 'tharidiathings', 'font')

    os.makedirs(texture_dir, exist_ok=True)
    os.makedirs(font_dir, exist_ok=True)

    # Save texture
    texture_path = os.path.join(texture_dir, 'crimson_text.png')
    img.save(texture_path)
    print(f"Saved texture: {texture_path}")

    # Generate and save JSON
    font_json = generate_font_json(char_widths)
    json_path = os.path.join(font_dir, 'medieval.json')

    with open(json_path, 'w', encoding='utf-8') as f:
        json.dump(font_json, f, indent=2, ensure_ascii=False)
    print(f"Saved font JSON: {json_path}")

    print("\nFont generation complete!")
    print(f"Texture size: {img.size}")
    print(f"Characters included: {len(CHARS)}")
    print("\nTo use this font, reference 'tharidiathings:medieval' in your code.")

if __name__ == '__main__':
    main()
