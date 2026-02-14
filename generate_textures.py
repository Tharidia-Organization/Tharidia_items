import random
import os
from PIL import Image

random.seed(42)

BASE_PATH = os.path.join('C:', os.sep, 'Users', 'franc', 'IdeaProjects', 'Tharidia_items',
                         'src', 'main', 'resources', 'assets', 'tharidiathings', 'textures', 'block')


def clamp(v, lo=0, hi=255):
    return max(lo, min(hi, v))


def noise_color(base_rgb, variation=10):
    r, g, b = base_rgb
    return (
        clamp(r + random.randint(-variation, variation)),
        clamp(g + random.randint(-variation, variation)),
        clamp(b + random.randint(-variation, variation)),
        255
    )


def pick_palette(palette, weights=None):
    if weights:
        return random.choices(palette, weights=weights, k=1)[0]
    return random.choice(palette)


def generate_compressed_grass():
    img = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    base_colors = [
        (0x5B, 0x8C, 0x32),
        (0x6B, 0xA8, 0x3A),
        (0x3F, 0x6A, 0x23),
        (0x7B, 0xBF, 0x42),
    ]
    weights = [40, 25, 25, 10]
    for y in range(16):
        row_shift = random.randint(-12, 12)
        is_groove = (y % 3 == 0) and random.random() < 0.5
        for x in range(16):
            color = pick_palette(base_colors, weights)
            if is_groove:
                color = base_colors[2]
                variation = 8
            else:
                variation = 10
            r, g, b = color
            r = clamp(r + row_shift)
            g = clamp(g + row_shift)
            b = clamp(b + row_shift // 2)
            if random.random() < 0.15:
                r = clamp(r + 15)
                g = clamp(g + 18)
                b = clamp(b + 5)
            pixel = (
                clamp(r + random.randint(-variation, variation)),
                clamp(g + random.randint(-variation, variation)),
                clamp(b + random.randint(-variation, variation)),
                255
            )
            img.putpixel((x, y), pixel)
    img.save(os.path.join(BASE_PATH, 'compressed_grass.png'))
    print('Generated compressed_grass.png')


def generate_wet_compressed_grass():
    img = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    base_colors = [
        (0x3A, 0x6B, 0x3A),
        (0x2E, 0x5A, 0x30),
        (0x4A, 0x7D, 0x4A),
        (0x5A, 0x8B, 0x6A),
    ]
    weights = [35, 30, 25, 10]
    num_droplets = random.randint(5, 8)
    droplet_positions = set()
    while len(droplet_positions) < num_droplets:
        dx, dy = random.randint(0, 15), random.randint(0, 15)
        droplet_positions.add((dx, dy))
    for y in range(16):
        row_shift = random.randint(-8, 8)
        is_groove = (y % 3 == 0) and random.random() < 0.5
        for x in range(16):
            if (x, y) in droplet_positions:
                base = (0x7A, 0xAB, 0xA0)
                pixel = noise_color(base, variation=12)
                img.putpixel((x, y), pixel)
                continue
            color = pick_palette(base_colors, weights)
            if is_groove:
                color = base_colors[1]
                variation = 6
            else:
                variation = 8
            r, g, b = color
            r = clamp(r + row_shift)
            g = clamp(g + row_shift)
            b = clamp(b + row_shift // 2)
            b = clamp(b + random.randint(0, 8))
            if random.random() < 0.12:
                r = clamp(r + 10)
                g = clamp(g + 12)
                b = clamp(b + 8)
            pixel = (
                clamp(r + random.randint(-variation, variation)),
                clamp(g + random.randint(-variation, variation)),
                clamp(b + random.randint(-variation, variation)),
                255
            )
            img.putpixel((x, y), pixel)
    img.save(os.path.join(BASE_PATH, 'wet_compressed_grass.png'))
    print('Generated wet_compressed_grass.png')


def generate_dried_compressed_grass():
    img = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    base_colors = [
        (0xB5, 0xA2, 0x4A),
        (0xC4, 0xB3, 0x5C),
        (0x96, 0x7E, 0x32),
        (0x8A, 0x70, 0x28),
    ]
    weights = [35, 20, 25, 20]
    for y in range(16):
        row_shift = random.randint(-14, 14)
        is_groove = (y % 3 == 0) and random.random() < 0.45
        for x in range(16):
            color = pick_palette(base_colors, weights)
            if is_groove:
                color = base_colors[3]
                variation = 8
            else:
                variation = 10
            r, g, b = color
            r = clamp(r + row_shift)
            g = clamp(g + row_shift)
            b = clamp(b + row_shift // 3)
            if random.random() < 0.2:
                r = clamp(r - 10)
                g = clamp(g - 15)
                b = clamp(b - 8)
            elif random.random() < 0.15:
                r = clamp(r + 12)
                g = clamp(g + 10)
                b = clamp(b - 5)
            if random.random() < 0.12:
                r = clamp(r + 15)
                g = clamp(g + 12)
                b = clamp(b + 3)
            pixel = (
                clamp(r + random.randint(-variation, variation)),
                clamp(g + random.randint(-variation, variation)),
                clamp(b + random.randint(-variation, variation)),
                255
            )
            img.putpixel((x, y), pixel)
    img.save(os.path.join(BASE_PATH, 'dried_compressed_grass.png'))
    print('Generated dried_compressed_grass.png')


def generate_fertilized_dirt():
    img = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    base_colors = [
        (0x3A, 0x2A, 0x1A),
        (0x4D, 0x36, 0x20),
        (0x5A, 0x40, 0x30),
        (0x2D, 0x1F, 0x14),
    ]
    weights = [30, 30, 20, 20]
    num_specks = random.randint(15, 20)
    speck_positions = set()
    while len(speck_positions) < num_specks:
        sx, sy = random.randint(0, 15), random.randint(0, 15)
        speck_positions.add((sx, sy))
    num_dark = random.randint(8, 12)
    dark_positions = set()
    while len(dark_positions) < num_dark:
        dx, dy = random.randint(0, 15), random.randint(0, 15)
        if (dx, dy) not in speck_positions:
            dark_positions.add((dx, dy))
    for y in range(16):
        for x in range(16):
            if (x, y) in speck_positions:
                speck_colors = [(0x6B, 0x55, 0x40), (0x7A, 0x64, 0x50)]
                base = random.choice(speck_colors)
                pixel = noise_color(base, variation=8)
                img.putpixel((x, y), pixel)
                continue
            if (x, y) in dark_positions:
                base = (0x1E, 0x15, 0x10)
                pixel = noise_color(base, variation=5)
                img.putpixel((x, y), pixel)
                continue
            color = pick_palette(base_colors, weights)
            r, g, b = color
            clump_noise = random.randint(-15, 15)
            r = clamp(r + clump_noise)
            g = clamp(g + clump_noise)
            b = clamp(b + clump_noise // 2)
            if random.random() < 0.1:
                r = clamp(r + 8)
            pixel = (
                clamp(r + random.randint(-8, 8)),
                clamp(g + random.randint(-8, 8)),
                clamp(b + random.randint(-6, 6)),
                255
            )
            img.putpixel((x, y), pixel)
    img.save(os.path.join(BASE_PATH, 'fertilized_dirt.png'))
    print('Generated fertilized_dirt.png')


def generate_abnormal_grass():
    img = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    BRIGHT    = (0x55, 0xDD, 0x40)
    MEDIUM    = (0x44, 0xBB, 0x33)
    HIGHLIGHT = (0x77, 0xFF, 0x55)
    DARK_STEM = (0x33, 0x88, 0x22)
    TIP_HIGH  = (0x88, 0xFF, 0x66)
    GLOW      = (0xAA, 0xFF, 0x88)

    def blade_color(progress, is_highlight=False):
        if is_highlight:
            base = GLOW
        elif progress > 0.85:
            base = random.choice([TIP_HIGH, HIGHLIGHT])
        elif progress > 0.5:
            base = random.choice([BRIGHT, HIGHLIGHT, MEDIUM])
        elif progress > 0.25:
            base = random.choice([BRIGHT, MEDIUM])
        else:
            base = random.choice([DARK_STEM, MEDIUM])
        return noise_color(base, variation=12)

    blades = []

    center = []
    for y in range(15, 0, -1):
        cx_draw = 8 if y < 5 else 7
        center.append((cx_draw, y))
        if y >= 13:
            center.append((cx_draw + 1, y))
        if y >= 14:
            center.append((cx_draw - 1, y))
    blades.append(center)

    left_center = []
    for i, y in enumerate(range(15, 3, -1)):
        lx = 7 - (i // 3)
        left_center.append((lx, y))
        if y >= 13:
            left_center.append((lx - 1, y))
    blades.append(left_center)

    right_center = []
    for i, y in enumerate(range(15, 3, -1)):
        rx = 8 + (i // 3)
        right_center.append((rx, y))
        if y >= 13:
            right_center.append((rx + 1, y))
    blades.append(right_center)

    outer_left = []
    for i, y in enumerate(range(15, 5, -1)):
        olx = max(1, 7 - (i // 2))
        outer_left.append((olx, y))
    blades.append(outer_left)

    outer_right = []
    for i, y in enumerate(range(15, 5, -1)):
        orx = min(14, 8 + (i // 2))
        outer_right.append((orx, y))
    blades.append(outer_right)

    far_left = []
    for i, y in enumerate(range(14, 8, -1)):
        flx = max(1, 6 - (i // 2))
        far_left.append((flx, y))
    blades.append(far_left)

    far_right = []
    for i, y in enumerate(range(14, 8, -1)):
        frx = min(14, 9 + (i // 2))
        far_right.append((frx, y))
    blades.append(far_right)

    all_pixels = []
    for blade in blades:
        all_pixels.extend(blade)
    highlight_count = max(5, len(all_pixels) // 6)
    highlight_positions = set()
    highlight_candidates = list(set(all_pixels))
    random.shuffle(highlight_candidates)
    for pos in highlight_candidates[:highlight_count]:
        highlight_positions.add(pos)

    for blade in blades:
        if not blade:
            continue
        y_min = min(p[1] for p in blade)
        y_max = max(p[1] for p in blade)
        y_range = y_max - y_min if y_max != y_min else 1
        for (bx, by) in blade:
            progress = (y_max - by) / y_range
            is_hl = (bx, by) in highlight_positions
            color = blade_color(progress, is_highlight=is_hl)
            existing = img.getpixel((bx, by))
            if existing[3] == 0:
                img.putpixel((bx, by), color)
            else:
                if sum(color[:3]) > sum(existing[:3]):
                    img.putpixel((bx, by), color)

    img.save(os.path.join(BASE_PATH, 'abnormal_grass.png'))
    print('Generated abnormal_grass.png')


if __name__ == '__main__':
    os.makedirs(BASE_PATH, exist_ok=True)
    generate_compressed_grass()
    generate_wet_compressed_grass()
    generate_dried_compressed_grass()
    generate_fertilized_dirt()
    generate_abnormal_grass()
    print('')
    print('All 5 textures generated successfully\!')

