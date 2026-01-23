"""
Generate a medieval-styled inventory panel PNG matching the realm_background.png style.
This script creates an inventory panel texture for use in the Tharidia mod GUI.
"""

from PIL import Image, ImageDraw, ImageFilter, ImageEnhance
import os

# Panel dimensions (scaled up for quality, will match in-game scale)
PANEL_WIDTH = 340  # 170 * 2 for higher resolution
PANEL_HEIGHT = 280  # 140 * 2 for higher resolution

# Colors extracted from realm_background.png style
PARCHMENT_BASE = (212, 188, 148)  # Main parchment color
PARCHMENT_DARK = (160, 128, 96)   # Darker parchment
WOOD_DARK = (62, 39, 35)          # Dark wood
BRONZE = (205, 127, 50)           # Bronze/gold
BRONZE_DARK = (139, 90, 43)       # Dark bronze
GOLD_BRIGHT = (255, 215, 0)       # Bright gold
GOLD_DARK = (184, 134, 11)        # Dark gold
LEATHER = (107, 68, 35)           # Leather brown
BLACK = (26, 26, 26)              # Near black

def create_gradient(draw, x, y, width, height, color1, color2, vertical=True):
    """Create a gradient fill."""
    for i in range(height if vertical else width):
        ratio = i / (height if vertical else width)
        r = int(color1[0] + (color2[0] - color1[0]) * ratio)
        g = int(color1[1] + (color2[1] - color1[1]) * ratio)
        b = int(color1[2] + (color2[2] - color1[2]) * ratio)
        if vertical:
            draw.line([(x, y + i), (x + width, y + i)], fill=(r, g, b))
        else:
            draw.line([(x + i, y), (x + i, y + height)], fill=(r, g, b))

def add_noise(image, intensity=10):
    """Add subtle noise for texture."""
    import random
    pixels = image.load()
    for i in range(image.width):
        for j in range(image.height):
            r, g, b, a = pixels[i, j]
            if a > 0:  # Only modify visible pixels
                noise = random.randint(-intensity, intensity)
                r = max(0, min(255, r + noise))
                g = max(0, min(255, g + noise))
                b = max(0, min(255, b + noise))
                pixels[i, j] = (r, g, b, a)
    return image

def generate_inventory_panel():
    """Generate the medieval inventory panel PNG."""
    # Create image with alpha channel
    img = Image.new('RGBA', (PANEL_WIDTH, PANEL_HEIGHT), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)

    # Border thickness
    border = 8
    inner_border = 4

    # --- OUTER SHADOW ---
    shadow_offset = 4
    draw.rectangle([shadow_offset, shadow_offset,
                    PANEL_WIDTH - 1 + shadow_offset, PANEL_HEIGHT - 1 + shadow_offset],
                   fill=(0, 0, 0, 100))

    # --- OUTER BRONZE FRAME ---
    # Dark bronze outer edge
    draw.rectangle([0, 0, PANEL_WIDTH - 1, PANEL_HEIGHT - 1],
                   fill=BRONZE_DARK + (255,), outline=None)

    # Bronze gradient for 3D effect
    for i in range(border):
        ratio = i / border
        r = int(BRONZE_DARK[0] + (BRONZE[0] - BRONZE_DARK[0]) * ratio)
        g = int(BRONZE_DARK[1] + (BRONZE[1] - BRONZE_DARK[1]) * ratio)
        b = int(BRONZE_DARK[2] + (BRONZE[2] - BRONZE_DARK[2]) * ratio)
        draw.rectangle([i, i, PANEL_WIDTH - 1 - i, PANEL_HEIGHT - 1 - i],
                       outline=(r, g, b, 255))

    # --- INNER GOLD BORDER ---
    inner_x = border + 2
    inner_y = border + 2
    inner_w = PANEL_WIDTH - 2 * (border + 2)
    inner_h = PANEL_HEIGHT - 2 * (border + 2)

    # Gold border with gradient
    for i in range(inner_border):
        ratio = i / inner_border
        r = int(GOLD_DARK[0] + (GOLD_BRIGHT[0] - GOLD_DARK[0]) * ratio)
        g = int(GOLD_DARK[1] + (GOLD_BRIGHT[1] - GOLD_DARK[1]) * ratio)
        b = int(GOLD_DARK[2] + (GOLD_BRIGHT[2] - GOLD_DARK[2]) * ratio)
        draw.rectangle([inner_x + i, inner_y + i,
                        inner_x + inner_w - 1 - i, inner_y + inner_h - 1 - i],
                       outline=(r, g, b, 255))

    # --- MAIN CONTENT AREA (Dark Wood) ---
    content_x = inner_x + inner_border + 2
    content_y = inner_y + inner_border + 2
    content_w = inner_w - 2 * (inner_border + 2)
    content_h = inner_h - 2 * (inner_border + 2)

    # Dark wood background with gradient
    for j in range(content_h):
        ratio = j / content_h
        # Slightly lighter at top, darker at bottom
        r = int(WOOD_DARK[0] + 15 - ratio * 30)
        g = int(WOOD_DARK[1] + 10 - ratio * 20)
        b = int(WOOD_DARK[2] + 8 - ratio * 16)
        r = max(0, min(255, r))
        g = max(0, min(255, g))
        b = max(0, min(255, b))
        draw.line([(content_x, content_y + j), (content_x + content_w, content_y + j)],
                  fill=(r, g, b, 255))

    # --- TITLE AREA ---
    title_height = 36
    title_y = content_y + 4

    # Title background (slightly lighter)
    draw.rectangle([content_x + 4, title_y,
                    content_x + content_w - 4, title_y + title_height],
                   fill=LEATHER + (255,))

    # Title border
    draw.rectangle([content_x + 4, title_y,
                    content_x + content_w - 4, title_y + title_height],
                   outline=BRONZE + (255,), width=2)

    # --- SLOT AREA BACKGROUND ---
    slot_area_y = title_y + title_height + 8
    slot_area_height = content_h - title_height - 16

    # Darker area for slots
    draw.rectangle([content_x + 4, slot_area_y,
                    content_x + content_w - 4, content_y + content_h - 4],
                   fill=(30, 25, 22, 255))

    # Slot area border
    draw.rectangle([content_x + 4, slot_area_y,
                    content_x + content_w - 4, content_y + content_h - 4],
                   outline=BRONZE_DARK + (255,), width=1)

    # --- DECORATIVE CORNER DETAILS ---
    corner_size = 12
    # Top-left
    draw.polygon([(0, 0), (corner_size, 0), (0, corner_size)],
                 fill=GOLD_BRIGHT + (255,))
    # Top-right
    draw.polygon([(PANEL_WIDTH - 1, 0), (PANEL_WIDTH - 1 - corner_size, 0),
                  (PANEL_WIDTH - 1, corner_size)],
                 fill=GOLD_BRIGHT + (255,))
    # Bottom-left
    draw.polygon([(0, PANEL_HEIGHT - 1), (corner_size, PANEL_HEIGHT - 1),
                  (0, PANEL_HEIGHT - 1 - corner_size)],
                 fill=GOLD_DARK + (255,))
    # Bottom-right
    draw.polygon([(PANEL_WIDTH - 1, PANEL_HEIGHT - 1),
                  (PANEL_WIDTH - 1 - corner_size, PANEL_HEIGHT - 1),
                  (PANEL_WIDTH - 1, PANEL_HEIGHT - 1 - corner_size)],
                 fill=GOLD_DARK + (255,))

    # --- ADD TEXTURE ---
    img = add_noise(img, intensity=5)

    return img


def main():
    # Generate the inventory panel
    panel = generate_inventory_panel()

    # Output path
    output_dir = os.path.join(os.path.dirname(__file__), '..',
                              'src', 'main', 'resources', 'assets',
                              'tharidiathings', 'textures', 'gui')
    output_path = os.path.join(output_dir, 'inventory_panel.png')

    # Ensure directory exists
    os.makedirs(output_dir, exist_ok=True)

    # Save the image
    panel.save(output_path)
    print(f"Generated inventory panel: {output_path}")
    print(f"Dimensions: {panel.size}")


if __name__ == '__main__':
    main()
