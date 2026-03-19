import json
import os

CONFIG_FILE = os.path.join(os.path.dirname(os.path.abspath(__file__)), "config.json")

DEFAULT_CONFIG = {
    "database": {
        "host": "localhost",
        "port": 3306,
        "database": "god_eye",
        "user": "root",
        "password": "",
    },
    "app": {
        "host": "127.0.0.1",
        "port": 8050,
        "default_refresh_interval": 30000,
    },
}


def load_config():
    if os.path.exists(CONFIG_FILE):
        with open(CONFIG_FILE, "r") as f:
            return json.load(f)
    save_config(DEFAULT_CONFIG)
    return DEFAULT_CONFIG


def save_config(cfg):
    with open(CONFIG_FILE, "w") as f:
        json.dump(cfg, f, indent=2)
