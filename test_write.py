import random
import os
from PIL import Image

random.seed(42)

BASE_PATH = os.path.join('C:', os.sep, 'Users', 'franc', 'IdeaProjects', 'Tharidia_items',
                         'src', 'main', 'resources', 'assets', 'tharidiathings', 'textures', 'block')


def clamp(v, lo=0, hi=255):
    return max(lo, min(hi, v))
