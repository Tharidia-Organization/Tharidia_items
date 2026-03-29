"""
database.py  –  All MariaDB query helpers for the Tharidia God Eye Monitor.
Every function returns a pandas DataFrame (empty on error).
"""

import logging
from datetime import date, timedelta

import pandas as pd
import pymysql

logger = logging.getLogger(__name__)


# ─── Connection ───────────────────────────────────────────────────────────────

def get_connection():
    from config import load_config
    c = load_config()["database"]
    return pymysql.connect(
        host=c["host"],
        port=int(c["port"]),
        database=c["database"],
        user=c["user"],
        password=c["password"],
        charset="utf8mb4",
        cursorclass=pymysql.cursors.DictCursor,
        connect_timeout=5,
        read_timeout=30,
    )


def query_df(sql, params=None):
    """Run a SELECT and return a DataFrame; empty DataFrame on any error."""
    df, _ = query_df_safe(sql, params)
    return df


def query_df_safe(sql, params=None):
    """Run a SELECT and return (DataFrame, error_string_or_None)."""
    try:
        conn = get_connection()
        try:
            with conn.cursor() as cur:
                # Only pass params when present — pymysql interprets %s in SQL
                # as a format specifier even if params=(), causing spurious errors
                # when DATE_FORMAT strings contain %s, %Y, etc.
                if params:
                    cur.execute(sql, params)
                else:
                    cur.execute(sql)
                rows = cur.fetchall()
            if not rows:
                return pd.DataFrame(), None
            return pd.DataFrame(rows), None
        finally:
            conn.close()
    except Exception as e:
        logger.error("DB error: %s | SQL: %.200s", e, sql)
        return pd.DataFrame(), str(e)


def scalar(sql, params=None, default=0):
    """Return the first cell of the first row, or default."""
    df = query_df(sql, params)
    if df.empty:
        return default
    v = df.iloc[0, 0]
    return int(v) if pd.notna(v) else default


def test_connection():
    try:
        conn = get_connection()
        conn.close()
        return True, "Connection successful ✓"
    except Exception as e:
        return False, str(e)


# ─── Filter helpers ───────────────────────────────────────────────────────────

def _date_f(col, start, end):
    if start and end:
        return f"AND DATE({col}) BETWEEN '{start}' AND '{end}'"
    if start:
        return f"AND DATE({col}) >= '{start}'"
    if end:
        return f"AND DATE({col}) <= '{end}'"
    return ""


def _player_f(col, players):
    if players:
        joined = ",".join(f"'{p}'" for p in players)
        return f"AND {col} IN ({joined})"
    return ""


def _server_f(col, servers):
    """Return a SQL fragment filtering by server_id list. Empty string = no filter."""
    if servers:
        joined = ",".join(f"'{s}'" for s in servers)
        return f"AND {col} IN ({joined})"
    return ""


def _spatial_f(x_col, z_col, cx, cz, radius):
    """Return a SQL fragment for spatial bounding box. Empty string = no filter."""
    if cx is None or cz is None or radius is None:
        return ""
    cx, cz, r = float(cx), float(cz), float(radius)
    return (f"AND {x_col} BETWEEN {cx - r} AND {cx + r} "
            f"AND {z_col} BETWEEN {cz - r} AND {cz + r}")


def get_server_ids():
    """Return sorted list of distinct server_id values found across event tables."""
    # Query a fast table that always has server_id data
    df = query_df("""
        SELECT DISTINCT server_id FROM player_logins WHERE server_id IS NOT NULL
        UNION
        SELECT DISTINCT server_id FROM player_events WHERE server_id IS NOT NULL
        ORDER BY server_id
    """)
    if df.empty:
        return []
    return df["server_id"].dropna().tolist()


def _default_dates():
    end = date.today().isoformat()
    start = (date.today() - timedelta(days=7)).isoformat()
    return start, end


# ─── Player list (for dropdowns) ─────────────────────────────────────────────

def get_player_names():
    df = query_df("SELECT DISTINCT username FROM players ORDER BY username")
    if not df.empty:
        return df["username"].tolist()
    df2 = query_df(
        "SELECT DISTINCT player_name FROM block_break_events ORDER BY player_name LIMIT 200"
    )
    return df2["player_name"].tolist() if not df2.empty else []


# ─── Overview ─────────────────────────────────────────────────────────────────

def get_overview_kpis(start=None, end=None, servers=None):
    if not start and not end:
        start, end = _default_dates()

    cond = f"DATE(timestamp) BETWEEN '{start}' AND '{end}'"
    sf = _server_f("server_id", servers)
    tables = {
        "deaths": ("player_deaths", cond),
        "attacks": ("attack_entity_events", cond),
        "blocks_broken": ("block_break_events", cond),
        "blocks_placed": ("block_place_events", cond),
        "item_drops": ("item_drop_events", cond),
        "item_pickups": ("item_pickup_events", cond),
        "chat_messages": ("player_chat", cond),
        "commands": ("player_command_events", cond),
        "crafts": ("crafting_event", cond),
        "kills": ("player_kill_events", cond),
        "advancements": ("player_advancement_events", cond),
        "consumed": ("item_consume_events", cond),
    }
    result = {"online_now": 0}
    try:
        conn = get_connection()
        try:
            with conn.cursor() as cur:
                # Online players
                cur.execute(
                    "SELECT COUNT(DISTINCT player_uuid) as v FROM player_logins WHERE logout_time IS NULL"
                )
                row = cur.fetchone()
                result["online_now"] = int(row["v"]) if row else 0
                # Total players ever (players table has no server_id — no filter)
                cur.execute("SELECT COUNT(*) as v FROM players")
                row = cur.fetchone()
                result["total_players"] = int(row["v"]) if row else 0
                # Per-table counts
                for key, (tbl, cnd) in tables.items():
                    try:
                        cur.execute(f"SELECT COUNT(*) as v FROM {tbl} WHERE {cnd} {sf}")
                        row = cur.fetchone()
                        result[key] = int(row["v"]) if row else 0
                    except Exception:
                        result[key] = 0
        finally:
            conn.close()
    except Exception as e:
        logger.error("KPI error: %s", e)
        for k in list(tables.keys()) + ["total_players"]:
            result.setdefault(k, 0)
    return result


def get_hourly_activity(start=None, end=None, servers=None):
    if not start and not end:
        cond = "timestamp >= DATE_SUB(NOW(), INTERVAL 24 HOUR)"
    else:
        cond = f"DATE(timestamp) BETWEEN '{start}' AND '{end}'"

    sf = _server_f("server_id", servers)
    parts = [
        ("Attacks", "attack_entity_events"),
        ("Block Breaks", "block_break_events"),
        ("Block Places", "block_place_events"),
        ("Chat", "player_chat"),
        ("Commands", "player_command_events"),
    ]
    dfs = []
    try:
        conn = get_connection()
        try:
            with conn.cursor() as cur:
                for label, tbl in parts:
                    try:
                        cur.execute(
                            f"SELECT DATE_FORMAT(timestamp, '%Y-%m-%d %H:00:00') as hour, "
                            f"COUNT(*) as cnt FROM {tbl} WHERE {cond} {sf} GROUP BY hour ORDER BY hour"
                        )
                        rows = cur.fetchall()
                        if rows:
                            df = pd.DataFrame(rows)
                            df["type"] = label
                            dfs.append(df)
                    except Exception as e:
                        logger.warning("Hourly activity [%s]: %s", tbl, e)
        finally:
            conn.close()
    except Exception as e:
        logger.error("Hourly activity: %s", e)
    return pd.concat(dfs, ignore_index=True) if dfs else pd.DataFrame(columns=["hour", "cnt", "type"])


def get_top_active_players(start=None, end=None, limit=10, servers=None):
    if not start and not end:
        start, end = _default_dates()
    cond = f"DATE(timestamp) BETWEEN '{start}' AND '{end}'"
    sf = _server_f("server_id", servers)
    sql = f"""
        SELECT player_name, COUNT(*) as events FROM (
            SELECT player_name, timestamp FROM attack_entity_events WHERE {cond} {sf}
            UNION ALL SELECT player_name, timestamp FROM block_break_events WHERE {cond} {sf}
            UNION ALL SELECT player_name, timestamp FROM block_place_events WHERE {cond} {sf}
            UNION ALL SELECT player_name, timestamp FROM item_drop_events WHERE {cond} {sf}
            UNION ALL SELECT player_name, timestamp FROM item_pickup_events WHERE {cond} {sf}
            UNION ALL SELECT player_name, timestamp FROM player_command_events WHERE {cond} {sf}
        ) combined
        GROUP BY player_name ORDER BY events DESC LIMIT {limit}
    """
    return query_df(sql)


def get_online_players():
    """Players with no logout recorded — genuinely online right now."""
    return query_df("""
        SELECT p.username,
               DATE_FORMAT(pl.login_time, '%Y-%m-%d %H:%i:%s') as login_time,
               TIMESTAMPDIFF(MINUTE, pl.login_time, NOW()) as minutes_online
        FROM player_logins pl
        JOIN players p ON pl.player_uuid = p.uuid
        WHERE pl.logout_time IS NULL
        ORDER BY pl.login_time ASC
    """)


# ─── Players ─────────────────────────────────────────────────────────────────

def get_all_players():
    return query_df("""
        SELECT username, custom_nickname, race, discord_id,
               DATE_FORMAT(first_seen, '%Y-%m-%d %H:%i') as first_seen,
               DATE_FORMAT(last_seen, '%Y-%m-%d %H:%i') as last_seen,
               last_dimension,
               ROUND(last_x,1) as last_x, ROUND(last_y,1) as last_y, ROUND(last_z,1) as last_z,
               claim_count
        FROM players ORDER BY last_seen DESC
    """)


def get_login_sessions(players=None, start=None, end=None, servers=None):
    pf = _player_f("p.username", players)
    df = _date_f("pl.login_time", start, end)
    sf = _server_f("pl.server_id", servers)
    return query_df(f"""
        SELECT p.username as player,
               DATE_FORMAT(pl.login_time, '%Y-%m-%d %H:%i:%s') as login_time,
               DATE_FORMAT(pl.logout_time, '%Y-%m-%d %H:%i:%s') as logout_time,
               TIMESTAMPDIFF(MINUTE, pl.login_time, COALESCE(pl.logout_time, NOW())) as duration_min,
               pl.ip_address
        FROM player_logins pl
        JOIN players p ON pl.player_uuid = p.uuid
        WHERE 1=1 {pf} {df} {sf}
        ORDER BY pl.login_time DESC LIMIT 2000
    """)


def get_session_stats(players=None, start=None, end=None, servers=None):
    pf = _player_f("p.username", players)
    df = _date_f("pl.login_time", start, end)
    sf = _server_f("pl.server_id", servers)
    return query_df(f"""
        SELECT p.username as player,
               COUNT(*) as sessions,
               ROUND(AVG(TIMESTAMPDIFF(MINUTE, pl.login_time, COALESCE(pl.logout_time, NOW()))), 1) as avg_session_min,
               ROUND(SUM(TIMESTAMPDIFF(MINUTE, pl.login_time, COALESCE(pl.logout_time, NOW()))) / 60.0, 2) as total_hours,
               DATE_FORMAT(MAX(pl.login_time), '%Y-%m-%d %H:%i') as last_login
        FROM player_logins pl
        JOIN players p ON pl.player_uuid = p.uuid
        WHERE 1=1 {pf} {df} {sf}
        GROUP BY p.username ORDER BY total_hours DESC
    """)


def get_logins_per_day(players=None, start=None, end=None, servers=None):
    pf = _player_f("p.username", players)
    df = _date_f("pl.login_time", start, end)
    sf = _server_f("pl.server_id", servers)
    return query_df(f"""
        SELECT DATE(pl.login_time) as day, COUNT(*) as logins
        FROM player_logins pl
        JOIN players p ON pl.player_uuid = p.uuid
        WHERE 1=1 {pf} {df} {sf}
        GROUP BY day ORDER BY day
    """)


# ─── Combat ──────────────────────────────────────────────────────────────────

def get_attack_events(players=None, start=None, end=None, limit=1000, servers=None):
    pf = _player_f("player_name", players)
    df = _date_f("timestamp", start, end)
    sf = _server_f("server_id", servers)
    return query_df(f"""
        SELECT player_name, entity_type,
               ROUND(original_damage, 2) as orig_dmg,
               ROUND(actual_damage, 2) as actual_dmg,
               ROUND(blocked_damage, 2) as blocked_dmg,
               ROUND(target_hp_before, 1) as hp_before,
               ROUND(target_hp_after, 1) as hp_after,
               ROUND(target_max_hp, 1) as max_hp,
               main_hand_item, off_hand_item, damage_type, dimension,
               DATE_FORMAT(timestamp, '%Y-%m-%d %H:%i:%s') as timestamp
        FROM attack_entity_events
        WHERE 1=1 {pf} {df} {sf}
        ORDER BY timestamp DESC LIMIT {limit}
    """)


def get_damage_stats(players=None, start=None, end=None, servers=None):
    pf = _player_f("player_name", players)
    df = _date_f("timestamp", start, end)
    sf = _server_f("server_id", servers)
    return query_df(f"""
        SELECT player_name,
               COUNT(*) as hits,
               ROUND(SUM(actual_damage), 1) as total_damage,
               ROUND(AVG(actual_damage), 2) as avg_damage,
               ROUND(MAX(actual_damage), 2) as max_hit,
               ROUND(SUM(blocked_damage), 1) as total_blocked
        FROM attack_entity_events
        WHERE 1=1 {pf} {df} {sf}
        GROUP BY player_name ORDER BY total_damage DESC
    """)


def get_entity_hit_counts(players=None, start=None, end=None, servers=None):
    pf = _player_f("player_name", players)
    df = _date_f("timestamp", start, end)
    sf = _server_f("server_id", servers)
    return query_df(f"""
        SELECT entity_type, COUNT(*) as hits, ROUND(AVG(actual_damage), 2) as avg_dmg
        FROM attack_entity_events
        WHERE 1=1 {pf} {df} {sf}
        GROUP BY entity_type ORDER BY hits DESC LIMIT 25
    """)


def get_time_to_kill(players=None, start=None, end=None, limit=1000, servers=None):
    pf = _player_f("player_name", players)
    df = _date_f("timestamp", start, end)
    sf = _server_f("server_id", servers)
    return query_df(f"""
        SELECT player_name, entity_type,
               duration_ticks,
               ROUND(duration_seconds, 2) as duration_s,
               dimension,
               DATE_FORMAT(timestamp, '%Y-%m-%d %H:%i:%s') as timestamp
        FROM time_to_kill_events
        WHERE 1=1 {pf} {df} {sf}
        ORDER BY timestamp DESC LIMIT {limit}
    """)


def get_ttk_by_entity(players=None, start=None, end=None, servers=None):
    pf = _player_f("player_name", players)
    df = _date_f("timestamp", start, end)
    sf = _server_f("server_id", servers)
    return query_df(f"""
        SELECT entity_type,
               COUNT(*) as kills,
               ROUND(AVG(duration_seconds), 2) as avg_s,
               ROUND(MIN(duration_seconds), 2) as min_s,
               ROUND(MAX(duration_seconds), 2) as max_s,
               ROUND(STDDEV(duration_seconds), 2) as stddev_s
        FROM time_to_kill_events
        WHERE 1=1 {pf} {df} {sf}
        GROUP BY entity_type ORDER BY kills DESC LIMIT 25
    """)


def get_player_kills(players=None, start=None, end=None, limit=1000, servers=None):
    pf = _player_f("player_name", players)
    df = _date_f("timestamp", start, end)
    sf = _server_f("server_id", servers)
    return query_df(f"""
        SELECT player_name, entity_type, main_hand_item, dimension,
               ROUND(player_x,1) as x, ROUND(player_y,1) as y, ROUND(player_z,1) as z,
               DATE_FORMAT(timestamp, '%Y-%m-%d %H:%i:%s') as timestamp
        FROM player_kill_events
        WHERE 1=1 {pf} {df} {sf}
        ORDER BY timestamp DESC LIMIT {limit}
    """)


def get_kills_per_player(players=None, start=None, end=None, servers=None):
    pf = _player_f("player_name", players)
    df = _date_f("timestamp", start, end)
    sf = _server_f("server_id", servers)
    return query_df(f"""
        SELECT player_name, COUNT(*) as kills
        FROM player_kill_events
        WHERE 1=1 {pf} {df} {sf}
        GROUP BY player_name ORDER BY kills DESC
    """)


# ─── Deaths ──────────────────────────────────────────────────────────────────

def get_deaths(players=None, start=None, end=None, limit=1000, servers=None):
    pf = _player_f("p.username", players)
    df = _date_f("d.timestamp", start, end)
    sf = _server_f("d.server_id", servers)
    return query_df(f"""
        SELECT p.username as player, d.cause, d.killer_name, d.weapon_used,
               ROUND(d.player_hp, 1) as hp_at_death,
               d.player_food as food,
               ROUND(d.player_armor, 1) as armor,
               d.dimension,
               ROUND(d.x,1) as x, ROUND(d.y,1) as y, ROUND(d.z,1) as z,
               DATE_FORMAT(d.timestamp, '%Y-%m-%d %H:%i:%s') as timestamp
        FROM player_deaths d
        JOIN players p ON d.player_uuid = p.uuid
        WHERE 1=1 {pf} {df} {sf}
        ORDER BY d.timestamp DESC LIMIT {limit}
    """)


def get_death_causes(players=None, start=None, end=None, servers=None):
    pf = _player_f("p.username", players)
    df = _date_f("d.timestamp", start, end)
    sf = _server_f("d.server_id", servers)
    return query_df(f"""
        SELECT COALESCE(d.cause, 'unknown') as cause, COUNT(*) as deaths
        FROM player_deaths d
        JOIN players p ON d.player_uuid = p.uuid
        WHERE 1=1 {pf} {df} {sf}
        GROUP BY cause ORDER BY deaths DESC
    """)


def get_deaths_per_day(players=None, start=None, end=None, servers=None):
    pf = _player_f("p.username", players)
    df = _date_f("d.timestamp", start, end)
    sf = _server_f("d.server_id", servers)
    return query_df(f"""
        SELECT DATE(d.timestamp) as day, COUNT(*) as deaths
        FROM player_deaths d
        JOIN players p ON d.player_uuid = p.uuid
        WHERE 1=1 {pf} {df} {sf}
        GROUP BY day ORDER BY day
    """)


def get_deadliest_players(players=None, start=None, end=None, servers=None):
    pf = _player_f("p.username", players)
    df = _date_f("d.timestamp", start, end)
    sf = _server_f("d.server_id", servers)
    return query_df(f"""
        SELECT p.username as player, COUNT(*) as deaths,
               ROUND(AVG(d.player_hp), 1) as avg_hp_at_death
        FROM player_deaths d
        JOIN players p ON d.player_uuid = p.uuid
        WHERE 1=1 {pf} {df} {sf}
        GROUP BY p.username ORDER BY deaths DESC LIMIT 15
    """)


# ─── World ────────────────────────────────────────────────────────────────────

def get_block_breaks(players=None, start=None, end=None, limit=500, servers=None):
    pf = _player_f("player_name", players)
    df = _date_f("timestamp", start, end)
    sf = _server_f("server_id", servers)
    return query_df(f"""
        SELECT player_name, block_type, tool_used, dimension,
               block_x, block_y, block_z,
               DATE_FORMAT(timestamp, '%Y-%m-%d %H:%i:%s') as timestamp
        FROM block_break_events
        WHERE 1=1 {pf} {df} {sf}
        ORDER BY timestamp DESC LIMIT {limit}
    """)


def get_top_blocks_broken(players=None, start=None, end=None, servers=None):
    pf = _player_f("player_name", players)
    df = _date_f("timestamp", start, end)
    sf = _server_f("server_id", servers)
    return query_df(f"""
        SELECT block_type, COUNT(*) as count
        FROM block_break_events
        WHERE 1=1 {pf} {df} {sf}
        GROUP BY block_type ORDER BY count DESC LIMIT 25
    """)


def get_block_break_by_player(players=None, start=None, end=None, servers=None):
    pf = _player_f("player_name", players)
    df = _date_f("timestamp", start, end)
    sf = _server_f("server_id", servers)
    return query_df(f"""
        SELECT player_name, COUNT(*) as blocks_broken
        FROM block_break_events
        WHERE 1=1 {pf} {df} {sf}
        GROUP BY player_name ORDER BY blocks_broken DESC
    """)


def get_block_places(players=None, start=None, end=None, limit=500, servers=None):
    pf = _player_f("player_name", players)
    df = _date_f("timestamp", start, end)
    sf = _server_f("server_id", servers)
    return query_df(f"""
        SELECT player_name, block_type, dimension, block_x, block_y, block_z,
               DATE_FORMAT(timestamp, '%Y-%m-%d %H:%i:%s') as timestamp
        FROM block_place_events
        WHERE 1=1 {pf} {df} {sf}
        ORDER BY timestamp DESC LIMIT {limit}
    """)


def get_top_blocks_placed(players=None, start=None, end=None, servers=None):
    pf = _player_f("player_name", players)
    df = _date_f("timestamp", start, end)
    sf = _server_f("server_id", servers)
    return query_df(f"""
        SELECT block_type, COUNT(*) as count
        FROM block_place_events
        WHERE 1=1 {pf} {df} {sf}
        GROUP BY block_type ORDER BY count DESC LIMIT 25
    """)


def get_block_place_by_player(players=None, start=None, end=None, servers=None):
    pf = _player_f("player_name", players)
    df = _date_f("timestamp", start, end)
    sf = _server_f("server_id", servers)
    return query_df(f"""
        SELECT player_name, COUNT(*) AS blocks_placed
        FROM block_place_events
        WHERE 1=1 {pf} {df} {sf}
        GROUP BY player_name ORDER BY blocks_placed DESC
    """)


def get_block_interact(players=None, start=None, end=None, limit=500, servers=None):
    pf = _player_f("player_name", players)
    df = _date_f("timestamp", start, end)
    sf = _server_f("server_id", servers)
    return query_df(f"""
        SELECT player_name, block_type, dimension, block_x, block_y, block_z,
               DATE_FORMAT(timestamp, '%Y-%m-%d %H:%i:%s') as timestamp
        FROM block_interact_events
        WHERE 1=1 {pf} {df} {sf}
        ORDER BY timestamp DESC LIMIT {limit}
    """)


def get_fluid_places(players=None, start=None, end=None, limit=500, servers=None):
    pf = _player_f("player_name", players)
    df = _date_f("timestamp", start, end)
    sf = _server_f("server_id", servers)
    return query_df(f"""
        SELECT player_name, fluid_type, dimension, block_x, block_y, block_z,
               DATE_FORMAT(timestamp, '%Y-%m-%d %H:%i:%s') as timestamp
        FROM fluid_place_events
        WHERE 1=1 {pf} {df} {sf}
        ORDER BY timestamp DESC LIMIT {limit}
    """)


def get_entity_interact(players=None, start=None, end=None, limit=500, servers=None):
    pf = _player_f("player_name", players)
    df = _date_f("timestamp", start, end)
    sf = _server_f("server_id", servers)
    return query_df(f"""
        SELECT player_name, entity_type, dimension,
               ROUND(player_x,1) as x, ROUND(player_y,1) as y, ROUND(player_z,1) as z,
               DATE_FORMAT(timestamp, '%Y-%m-%d %H:%i:%s') as timestamp
        FROM entity_interact_events
        WHERE 1=1 {pf} {df} {sf}
        ORDER BY timestamp DESC LIMIT {limit}
    """)


# ─── Items ────────────────────────────────────────────────────────────────────

def get_item_drops(players=None, start=None, end=None, limit=500, servers=None):
    pf = _player_f("player_name", players)
    df = _date_f("timestamp", start, end)
    sf = _server_f("server_id", servers)
    return query_df(f"""
        SELECT player_name, item_type, item_count, dimension,
               DATE_FORMAT(timestamp, '%Y-%m-%d %H:%i:%s') as timestamp
        FROM item_drop_events
        WHERE 1=1 {pf} {df} {sf}
        ORDER BY timestamp DESC LIMIT {limit}
    """)


def get_item_pickups(players=None, start=None, end=None, limit=500, servers=None):
    pf = _player_f("player_name", players)
    df = _date_f("timestamp", start, end)
    sf = _server_f("server_id", servers)
    return query_df(f"""
        SELECT player_name, item_type, item_count, dimension,
               DATE_FORMAT(timestamp, '%Y-%m-%d %H:%i:%s') as timestamp
        FROM item_pickup_events
        WHERE 1=1 {pf} {df} {sf}
        ORDER BY timestamp DESC LIMIT {limit}
    """)


def get_top_items_dropped(players=None, start=None, end=None, servers=None):
    pf = _player_f("player_name", players)
    df = _date_f("timestamp", start, end)
    sf = _server_f("server_id", servers)
    return query_df(f"""
        SELECT item_type, SUM(item_count) as total, COUNT(*) as events
        FROM item_drop_events WHERE 1=1 {pf} {df} {sf}
        GROUP BY item_type ORDER BY total DESC LIMIT 25
    """)


def get_top_items_picked(players=None, start=None, end=None, servers=None):
    pf = _player_f("player_name", players)
    df = _date_f("timestamp", start, end)
    sf = _server_f("server_id", servers)
    return query_df(f"""
        SELECT item_type, SUM(item_count) as total, COUNT(*) as events
        FROM item_pickup_events WHERE 1=1 {pf} {df} {sf}
        GROUP BY item_type ORDER BY total DESC LIMIT 25
    """)


def get_crafting(players=None, start=None, end=None, limit=500, servers=None):
    pf = _player_f("player_name", players)
    df = _date_f("timestamp", start, end)
    sf = _server_f("server_id", servers)
    return query_df(f"""
        SELECT player_name, crafted_item, recipe_id, dimension,
               DATE_FORMAT(timestamp, '%Y-%m-%d %H:%i:%s') as timestamp
        FROM crafting_event
        WHERE 1=1 {pf} {df} {sf}
        ORDER BY timestamp DESC LIMIT {limit}
    """)


def get_top_crafted(players=None, start=None, end=None, servers=None):
    pf = _player_f("player_name", players)
    df = _date_f("timestamp", start, end)
    sf = _server_f("server_id", servers)
    return query_df(f"""
        SELECT crafted_item, COUNT(*) as count
        FROM crafting_event WHERE 1=1 {pf} {df} {sf}
        GROUP BY crafted_item ORDER BY count DESC LIMIT 25
    """)


def get_item_consume(players=None, start=None, end=None, limit=500, servers=None):
    pf = _player_f("player_name", players)
    df = _date_f("timestamp", start, end)
    sf = _server_f("server_id", servers)
    return query_df(f"""
        SELECT player_name, item_type,
               ROUND(player_hp_before,1) as hp_before,
               ROUND(player_hp_after,1) as hp_after,
               ROUND(player_hp_after - player_hp_before, 1) as hp_gain,
               player_food_before as food_before,
               player_food_after as food_after,
               player_food_after - player_food_before as food_gain,
               dimension,
               DATE_FORMAT(timestamp, '%Y-%m-%d %H:%i:%s') as timestamp
        FROM item_consume_events
        WHERE 1=1 {pf} {df} {sf}
        ORDER BY timestamp DESC LIMIT {limit}
    """)


def get_top_consumed(players=None, start=None, end=None, servers=None):
    pf = _player_f("player_name", players)
    df = _date_f("timestamp", start, end)
    sf = _server_f("server_id", servers)
    return query_df(f"""
        SELECT item_type, COUNT(*) as uses,
               ROUND(AVG(player_hp_after - player_hp_before), 2) as avg_hp_gain
        FROM item_consume_events WHERE 1=1 {pf} {df} {sf}
        GROUP BY item_type ORDER BY uses DESC LIMIT 25
    """)


# ─── Chat & Commands ──────────────────────────────────────────────────────────

def get_chat(players=None, start=None, end=None, limit=500, search=None, servers=None):
    pf = _player_f("player_name", players)
    df = _date_f("timestamp", start, end)
    sf = f"AND message LIKE '%{search.replace(chr(39), '')}%'" if search else ""
    svf = _server_f("server_id", servers)
    return query_df(f"""
        SELECT player_name as player, message,
               DATE_FORMAT(timestamp, '%Y-%m-%d %H:%i:%s') as timestamp
        FROM player_chat
        WHERE 1=1 {pf} {df} {sf} {svf}
        ORDER BY timestamp DESC LIMIT {limit}
    """)


def get_chat_per_player(players=None, start=None, end=None, servers=None):
    pf = _player_f("player_name", players)
    df = _date_f("timestamp", start, end)
    sf = _server_f("server_id", servers)
    return query_df(f"""
        SELECT player_name as player, COUNT(*) as messages
        FROM player_chat
        WHERE 1=1 {pf} {df} {sf}
        GROUP BY player_name ORDER BY messages DESC LIMIT 20
    """)


def get_commands(players=None, start=None, end=None, limit=500, search=None, servers=None):
    pf = _player_f("player_name", players)
    df = _date_f("timestamp", start, end)
    sf = f"AND command LIKE '%{search.replace(chr(39), '')}%'" if search else ""
    svf = _server_f("server_id", servers)
    return query_df(f"""
        SELECT player_name as player, command, dimension,
               DATE_FORMAT(timestamp, '%Y-%m-%d %H:%i:%s') as timestamp
        FROM player_command_events
        WHERE 1=1 {pf} {df} {sf} {svf}
        ORDER BY timestamp DESC LIMIT {limit}
    """)


def get_top_commands(players=None, start=None, end=None, servers=None):
    pf = _player_f("player_name", players)
    df = _date_f("timestamp", start, end)
    sf = _server_f("server_id", servers)
    return query_df(f"""
        SELECT SUBSTRING_INDEX(LTRIM(command), ' ', 1) as cmd, COUNT(*) as uses
        FROM player_command_events WHERE 1=1 {pf} {df} {sf}
        GROUP BY cmd ORDER BY uses DESC LIMIT 20
    """)


# ─── Advancements ────────────────────────────────────────────────────────────

def get_advancements(players=None, start=None, end=None, limit=500, servers=None):
    pf = _player_f("player_name", players)
    df = _date_f("timestamp", start, end)
    sf = _server_f("server_id", servers)
    return query_df(f"""
        SELECT player_name, advancement_id, dimension,
               ROUND(player_x,1) as x, ROUND(player_y,1) as y, ROUND(player_z,1) as z,
               DATE_FORMAT(timestamp, '%Y-%m-%d %H:%i:%s') as timestamp
        FROM player_advancement_events
        WHERE 1=1 {pf} {df} {sf}
        ORDER BY timestamp DESC LIMIT {limit}
    """)


def get_advancement_stats(players=None, start=None, end=None, servers=None):
    pf = _player_f("player_name", players)
    df = _date_f("timestamp", start, end)
    sf = _server_f("server_id", servers)
    return query_df(f"""
        SELECT player_name, COUNT(*) as total,
               DATE_FORMAT(MAX(timestamp), '%Y-%m-%d %H:%i') as latest
        FROM player_advancement_events
        WHERE 1=1 {pf} {df} {sf}
        GROUP BY player_name ORDER BY total DESC
    """)


def get_first_to_advance(start=None, end=None, servers=None):
    df = _date_f("timestamp", start, end)
    sf = _server_f("server_id", servers)
    return query_df(f"""
        SELECT advancement_id, player_name,
               DATE_FORMAT(timestamp, '%Y-%m-%d %H:%i:%s') as achieved_at
        FROM player_advancement_events
        WHERE 1=1 {df} {sf}
        ORDER BY timestamp ASC
    """)


# ─── Reports ──────────────────────────────────────────────────────────────────

REPORT_TABLES = {
    "Players": "players",
    "Attack Events": "attack_entity_events",
    "Block Breaks": "block_break_events",
    "Block Places": "block_place_events",
    "Block Interact": "block_interact_events",
    "Entity Interact": "entity_interact_events",
    "Item Drops": "item_drop_events",
    "Item Pickups": "item_pickup_events",
    "Player Kills": "player_kill_events",
    "Time to Kill": "time_to_kill_events",
    "Crafting": "crafting_event",
    "Deaths": "player_deaths",
    "Logins": "player_logins",
    "Chat": "player_chat",
    "Fluid Places": "fluid_place_events",
    "Item Consume": "item_consume_events",
    "Advancements": "player_advancement_events",
    "Commands": "player_command_events",
}


def get_table_data(table_name, players=None, start=None, end=None, limit=2000, servers=None):
    safe = {v: v for v in REPORT_TABLES.values()}
    if table_name not in safe:
        return pd.DataFrame()

    # Determine timestamp column for this table
    ts_cols = {
        "players": "last_seen",
        "player_logins": "login_time",
        "player_chat": "timestamp",
        "player_deaths": "timestamp",
    }
    ts_col = ts_cols.get(table_name, "timestamp")

    # Determine player column
    uuid_tables = {"player_logins", "player_deaths", "player_events"}
    if table_name in uuid_tables:
        pf = ""  # skip player filter for uuid tables for simplicity
    else:
        pf = _player_f("player_name", players)

    # Apply server filter only to event tables that have a server_id column
    no_server_id_tables = {"players"}
    if table_name not in no_server_id_tables:
        sf = _server_f("server_id", servers)
    else:
        sf = ""

    df = _date_f(ts_col, start, end)
    return query_df(f"SELECT * FROM {table_name} WHERE 1=1 {pf} {df} {sf} ORDER BY {ts_col} DESC LIMIT {limit}")


def get_table_row_count(table_name):
    safe = {v: v for v in REPORT_TABLES.values()}
    if table_name not in safe:
        return 0
    return scalar(f"SELECT COUNT(*) FROM {table_name}")


# ─── Investigation ────────────────────────────────────────────────────────────

def _esc(s):
    """Minimal SQL string escaping for player names coming from DB dropdowns."""
    return str(s).replace("'", "''") if s else ""


def get_player_timeline(player, start=None, end=None, limit=2000, servers=None):
    """Full chronological activity log for one player across all event tables.
    Each table is queried individually so a missing column in one table
    does not break the entire timeline.
    """
    if not player:
        return pd.DataFrame()
    p = _esc(player)
    df_ts = _date_f("timestamp", start, end)
    df_d  = _date_f("d.timestamp", start, end)
    df_c  = _date_f("c.timestamp", start, end)
    sf_ts = _server_f("server_id", servers)
    sf_d  = _server_f("d.server_id", servers)

    # List of (label, sql) pairs — each returns columns: ts, type, details, dimension, x, y, z
    queries = [
        ("Attack", f"""
            SELECT timestamp as ts, 'Attack' as type,
                CONCAT('Hit ', entity_type, ' for ', ROUND(actual_damage,1), ' dmg  [', COALESCE(main_hand_item,'?'), ']') as details,
                dimension, player_x as x, player_y as y, player_z as z
            FROM attack_entity_events WHERE player_name='{p}' {df_ts} {sf_ts}
        """),
        ("Kill", f"""
            SELECT timestamp as ts, 'Kill' as type,
                CONCAT('Killed ', entity_type, '  [', COALESCE(main_hand_item,'?'), ']') as details,
                dimension, player_x as x, player_y as y, player_z as z
            FROM player_kill_events WHERE player_name='{p}' {df_ts} {sf_ts}
        """),
        ("Death", f"""
            SELECT d.timestamp as ts, 'Death' as type,
                CONCAT('Died: ', COALESCE(d.cause,'?'),
                       CASE WHEN d.killer_name IS NOT NULL
                            THEN CONCAT('  \u2190 by ', d.killer_name, ' [', COALESCE(d.weapon_used,'?'), ']')
                            ELSE '' END,
                       '  HP:', ROUND(d.player_hp,1), '  Armor:', ROUND(d.player_armor,1)) as details,
                d.dimension, d.x as x, d.y as y, d.z as z
            FROM player_deaths d JOIN players p2 ON d.player_uuid=p2.uuid
            WHERE p2.username='{p}' {df_d} {sf_d}
        """),
        ("Block Break", f"""
            SELECT timestamp as ts, 'Block Break' as type,
                CONCAT('Broke ', block_type, CASE WHEN tool_used IS NOT NULL THEN CONCAT('  [', tool_used, ']') ELSE '' END) as details,
                dimension, CAST(block_x AS DOUBLE) as x, CAST(block_y AS DOUBLE) as y, CAST(block_z AS DOUBLE) as z
            FROM block_break_events WHERE player_name='{p}' {df_ts} {sf_ts}
        """),
        ("Block Place", f"""
            SELECT timestamp as ts, 'Block Place' as type,
                CONCAT('Placed ', block_type) as details,
                dimension, CAST(block_x AS DOUBLE) as x, CAST(block_y AS DOUBLE) as y, CAST(block_z AS DOUBLE) as z
            FROM block_place_events WHERE player_name='{p}' {df_ts} {sf_ts}
        """),
        ("Block Interact", f"""
            SELECT timestamp as ts, 'Block Interact' as type,
                CONCAT('Interacted: ', block_type) as details,
                dimension, CAST(block_x AS DOUBLE) as x, CAST(block_y AS DOUBLE) as y, CAST(block_z AS DOUBLE) as z
            FROM block_interact_events WHERE player_name='{p}' {df_ts} {sf_ts}
        """),
        ("Item Drop", f"""
            SELECT timestamp as ts, 'Item Drop' as type,
                CONCAT('Dropped ', item_count, 'x ', item_type) as details,
                dimension, player_x as x, player_y as y, player_z as z
            FROM item_drop_events WHERE player_name='{p}' {df_ts} {sf_ts}
        """),
        ("Item Pickup", f"""
            SELECT timestamp as ts, 'Item Pickup' as type,
                CONCAT('Picked up ', item_count, 'x ', item_type) as details,
                dimension, player_x as x, player_y as y, player_z as z
            FROM item_pickup_events WHERE player_name='{p}' {df_ts} {sf_ts}
        """),
        ("Craft", f"""
            SELECT timestamp as ts, 'Craft' as type,
                CONCAT('Crafted ', crafted_item) as details,
                dimension, NULL as x, NULL as y, NULL as z
            FROM crafting_event WHERE player_name='{p}' {df_ts} {sf_ts}
        """),
        ("Consumed", f"""
            SELECT timestamp as ts, 'Consumed' as type,
                CONCAT('Used ', item_type,
                       '  HP:', ROUND(player_hp_before,1), '\u2192', ROUND(player_hp_after,1),
                       '  Food:', player_food_before, '\u2192', player_food_after) as details,
                dimension, NULL as x, NULL as y, NULL as z
            FROM item_consume_events WHERE player_name='{p}' {df_ts} {sf_ts}
        """),
        ("Command", f"""
            SELECT timestamp as ts, 'Command' as type,
                CONCAT('/ ', command) as details,
                dimension, NULL as x, NULL as y, NULL as z
            FROM player_command_events WHERE player_name='{p}' {df_ts} {sf_ts}
        """),
        ("Advancement", f"""
            SELECT timestamp as ts, 'Advancement' as type,
                advancement_id as details,
                dimension, player_x as x, player_y as y, player_z as z
            FROM player_advancement_events WHERE player_name='{p}' {df_ts} {sf_ts}
        """),
        ("Fluid Place", f"""
            SELECT timestamp as ts, 'Fluid Place' as type,
                CONCAT('Placed fluid: ', fluid_type) as details,
                dimension, CAST(block_x AS DOUBLE) as x, CAST(block_y AS DOUBLE) as y, CAST(block_z AS DOUBLE) as z
            FROM fluid_place_events WHERE player_name='{p}' {df_ts} {sf_ts}
        """),
        ("Entity Interact", f"""
            SELECT timestamp as ts, 'Entity Interact' as type,
                CONCAT('Interacted with ', entity_type) as details,
                dimension, player_x as x, player_y as y, player_z as z
            FROM entity_interact_events WHERE player_name='{p}' {df_ts} {sf_ts}
        """),
        ("Chat", f"""
            SELECT timestamp as ts, 'Chat' as type,
                CONCAT('"', message, '"') as details,
                NULL as dimension, NULL as x, NULL as y, NULL as z
            FROM player_chat
            WHERE player_name='{p}' {df_c} {sf_ts}
        """),
    ]

    frames = []
    errors = []
    try:
        conn = get_connection()
        try:
            with conn.cursor() as cur:
                for label, sql in queries:
                    try:
                        cur.execute(sql)
                        rows = cur.fetchall()
                        if rows:
                            frames.append(pd.DataFrame(rows))
                    except Exception as e:
                        errors.append(f"{label}: {e}")
                        logger.warning("Timeline sub-query failed [%s]: %s", label, e)
        finally:
            conn.close()
    except Exception as e:
        logger.error("Timeline connection error: %s", e)
        return pd.DataFrame()

    if errors:
        logger.warning("Timeline skipped tables: %s", "; ".join(errors))

    if not frames:
        return pd.DataFrame()

    df = pd.concat(frames, ignore_index=True)
    df = df.sort_values("ts", ascending=False).head(limit).reset_index(drop=True)
    df["timestamp"] = df["ts"].apply(lambda v: str(v)[:19] if v else "")
    df = df.drop(columns=["ts"])
    df["x"] = pd.to_numeric(df["x"], errors="coerce").round(1)
    df["y"] = pd.to_numeric(df["y"], errors="coerce").round(1)
    df["z"] = pd.to_numeric(df["z"], errors="coerce").round(1)
    cols = ["timestamp", "type", "details", "dimension", "x", "y", "z"]
    return df[[c for c in cols if c in df.columns]]


def get_pvp_incidents(start=None, end=None, victim=None, killer=None, limit=1000, servers=None):
    """All PvP deaths (killer_name not null). Returns (DataFrame, error_str_or_None)."""
    dts = _date_f("d.timestamp", start, end)
    sf = _server_f("d.server_id", servers)
    vf = f"AND p.username = '{_esc(victim)}'" if victim else ""
    kf = f"AND d.killer_name = '{_esc(killer)}'" if killer else ""
    base = f"""
        FROM player_deaths d
        JOIN players p ON d.player_uuid = p.uuid
        WHERE d.killer_name IS NOT NULL {dts} {sf} {vf} {kf}
        ORDER BY d.timestamp DESC LIMIT {limit}
    """
    # Full query — uses enhancement columns (x/y/z, player_hp, armor, food, dimension)
    full = f"""
        SELECT p.username as victim, d.killer_name,
               COALESCE(d.weapon_used,'?') as weapon_used, COALESCE(d.cause,'?') as cause,
               ROUND(d.player_hp,1) as victim_hp, d.player_food as victim_food,
               ROUND(d.player_armor,1) as victim_armor,
               d.dimension,
               ROUND(d.x,1) as x, ROUND(d.y,1) as y, ROUND(d.z,1) as z,
               DATE_FORMAT(d.timestamp,'%Y-%m-%d %H:%i:%s') as timestamp
        {base}
    """
    df, err = query_df_safe(full)
    if err:
        logger.warning("PvP full query failed (%s) — trying minimal fallback", err)
        # Fallback: only columns guaranteed in original schema
        minimal = f"""
            SELECT p.username as victim, d.killer_name,
                   COALESCE(d.weapon_used,'?') as weapon_used,
                   COALESCE(d.cause,'?') as cause,
                   DATE_FORMAT(d.timestamp,'%Y-%m-%d %H:%i:%s') as timestamp
            {base}
        """
        df, err2 = query_df_safe(minimal)
        if err2:
            return pd.DataFrame(), f"Full: {err} | Fallback: {err2}"
        return df, f"Note: limited columns (schema missing x/y/z/hp/armor). Original error: {err}"
    return df, None


def get_item_transfers(start=None, end=None, time_window_sec=300, max_distance=30, limit=500, servers=None):
    """Detect possible item handoffs: same item dropped by A, picked up by B nearby and soon after."""
    df = _date_f("d.timestamp", start, end)
    sf = _server_f("d.server_id", servers)
    return query_df(f"""
        SELECT
            d.player_name as dropper,
            p.player_name as picker,
            d.item_type,
            d.item_count  as dropped_count,
            p.item_count  as picked_count,
            TIMESTAMPDIFF(SECOND, d.timestamp, p.timestamp) as seconds_apart,
            ROUND(SQRT(POW(d.player_x - p.player_x, 2) + POW(d.player_z - p.player_z, 2)), 1) as blocks_apart,
            d.dimension,
            ROUND(d.player_x,1) as drop_x, ROUND(d.player_z,1) as drop_z,
            DATE_FORMAT(d.timestamp, '%Y-%m-%d %H:%i:%s') as drop_time,
            DATE_FORMAT(p.timestamp, '%Y-%m-%d %H:%i:%s') as pickup_time
        FROM item_drop_events d
        JOIN item_pickup_events p
            ON  d.item_type    = p.item_type
            AND d.player_name != p.player_name
            AND p.timestamp BETWEEN d.timestamp
                            AND DATE_ADD(d.timestamp, INTERVAL {int(time_window_sec)} SECOND)
            AND SQRT(POW(d.player_x - p.player_x,2) + POW(d.player_z - p.player_z,2)) <= {float(max_distance)}
        WHERE 1=1 {df} {sf}
        ORDER BY d.timestamp DESC LIMIT {limit}
    """)


def get_events_near(cx, cz, radius, start=None, end=None, limit=500, servers=None):
    """All events within radius blocks of coordinate (cx, cz)."""
    r = float(radius)
    cx, cz = float(cx), float(cz)
    df_ts = _date_f("timestamp", start, end)
    df_d  = _date_f("d.timestamp", start, end)
    sf_ts = _server_f("server_id", servers)
    sf_d  = _server_f("d.server_id", servers)

    sql = f"""
    SELECT DATE_FORMAT(ts, '%Y-%m-%d %H:%i:%s') as timestamp,
           event_type, player, details, dimension,
           ROUND(x,1) as x, ROUND(y,1) as y, ROUND(z,1) as z
    FROM (
        SELECT timestamp as ts, 'Attack' as event_type, player_name as player,
            CONCAT('Hit ', entity_type, ' for ', ROUND(actual_damage,1), ' dmg') as details,
            dimension, player_x as x, player_y as y, player_z as z
        FROM attack_entity_events
        WHERE ABS(player_x - {cx}) <= {r} AND ABS(player_z - {cz}) <= {r} {df_ts} {sf_ts}

        UNION ALL
        SELECT timestamp, 'Kill', player_name,
            CONCAT('Killed ', entity_type, '  [', COALESCE(main_hand_item,'?'), ']'),
            dimension, player_x, player_y, player_z
        FROM player_kill_events
        WHERE ABS(player_x - {cx}) <= {r} AND ABS(player_z - {cz}) <= {r} {df_ts} {sf_ts}

        UNION ALL
        SELECT d.timestamp, 'Death', p.username,
            CONCAT('Died: ', COALESCE(d.cause,'?'),
                   CASE WHEN d.killer_name IS NOT NULL
                        THEN CONCAT(' ← by ', d.killer_name) ELSE '' END),
            d.dimension, d.x, d.y, d.z
        FROM player_deaths d JOIN players p ON d.player_uuid=p.uuid
        WHERE d.x IS NOT NULL AND ABS(d.x - {cx}) <= {r} AND ABS(d.z - {cz}) <= {r} {df_d} {sf_d}

        UNION ALL
        SELECT timestamp, 'Block Break', player_name, CONCAT('Broke ', block_type),
            dimension, CAST(block_x AS DOUBLE), CAST(block_y AS DOUBLE), CAST(block_z AS DOUBLE)
        FROM block_break_events
        WHERE ABS(block_x - {cx}) <= {r} AND ABS(block_z - {cz}) <= {r} {df_ts} {sf_ts}

        UNION ALL
        SELECT timestamp, 'Block Place', player_name, CONCAT('Placed ', block_type),
            dimension, CAST(block_x AS DOUBLE), CAST(block_y AS DOUBLE), CAST(block_z AS DOUBLE)
        FROM block_place_events
        WHERE ABS(block_x - {cx}) <= {r} AND ABS(block_z - {cz}) <= {r} {df_ts} {sf_ts}

        UNION ALL
        SELECT timestamp, 'Item Drop', player_name, CONCAT('Dropped ', item_count, 'x ', item_type),
            dimension, player_x, player_y, player_z
        FROM item_drop_events
        WHERE ABS(player_x - {cx}) <= {r} AND ABS(player_z - {cz}) <= {r} {df_ts} {sf_ts}

        UNION ALL
        SELECT timestamp, 'Item Pickup', player_name, CONCAT('Picked up ', item_count, 'x ', item_type),
            dimension, player_x, player_y, player_z
        FROM item_pickup_events
        WHERE ABS(player_x - {cx}) <= {r} AND ABS(player_z - {cz}) <= {r} {df_ts} {sf_ts}

        UNION ALL
        SELECT timestamp, 'Block Interact', player_name, CONCAT('Interacted: ', block_type),
            dimension, CAST(block_x AS DOUBLE), CAST(block_y AS DOUBLE), CAST(block_z AS DOUBLE)
        FROM block_interact_events
        WHERE ABS(block_x - {cx}) <= {r} AND ABS(block_z - {cz}) <= {r} {df_ts} {sf_ts}
    ) ev
    ORDER BY ts DESC LIMIT {limit}
    """
    return query_df(sql)


def get_player_proximity(player_a, player_b, start=None, end=None, distance=50, limit=500, servers=None):
    """Times when player_a and player_b had events within 'distance' blocks of each other."""
    if not player_a or not player_b:
        return pd.DataFrame()
    pa, pb = _esc(player_a), _esc(player_b)
    dist = float(distance)
    df_a = _date_f("a.timestamp", start, end)
    df_ts = _date_f("timestamp", start, end)
    sf_ts = _server_f("server_id", servers)

    # Use block events + attacks as position sources (most frequent)
    sql = f"""
    SELECT DATE_FORMAT(a.ts, '%Y-%m-%d %H:%i:%s') as time,
           '{player_a}' as player_a, ROUND(a.ax,1) as a_x, ROUND(a.ay,1) as a_y, ROUND(a.az,1) as a_z,
           '{player_b}' as player_b, ROUND(b.bx,1) as b_x, ROUND(b.by,1) as b_y, ROUND(b.bz,1) as b_z,
           ROUND(SQRT(POW(a.ax-b.bx,2)+POW(a.az-b.bz,2)),1) as distance_blocks
    FROM (
        SELECT timestamp as ts, player_x as ax, player_y as ay, player_z as az
        FROM block_break_events WHERE player_name='{pa}' {df_a} {sf_ts}
        UNION ALL
        SELECT timestamp, player_x, player_y, player_z
        FROM block_place_events WHERE player_name='{pa}' {df_ts} {sf_ts}
        UNION ALL
        SELECT timestamp, player_x, player_y, player_z
        FROM attack_entity_events WHERE player_name='{pa}' {df_ts} {sf_ts}
    ) a
    JOIN (
        SELECT timestamp as ts2, player_x as bx, player_y as by, player_z as bz
        FROM block_break_events WHERE player_name='{pb}' {df_ts} {sf_ts}
        UNION ALL
        SELECT timestamp, player_x, player_y, player_z
        FROM block_place_events WHERE player_name='{pb}' {df_ts} {sf_ts}
        UNION ALL
        SELECT timestamp, player_x, player_y, player_z
        FROM attack_entity_events WHERE player_name='{pb}' {df_ts} {sf_ts}
    ) b ON ABS(TIMESTAMPDIFF(SECOND, a.ts, b.ts2)) <= 30
    WHERE SQRT(POW(a.ax-b.bx,2)+POW(a.az-b.bz,2)) <= {dist}
    GROUP BY DATE_FORMAT(a.ts, '%Y-%m-%d %H:%i')
    ORDER BY a.ts DESC LIMIT {limit}
    """
    return query_df(sql)


# ─── Map Data ─────────────────────────────────────────────────────────────────

def get_fallen_events(players=None, start=None, end=None, limit=500, servers=None):
    """List of player_fallen_events with full detail."""
    pf = _player_f("player_name", players)
    df = _date_f("timestamp", start, end)
    sf = _server_f("server_id", servers)
    return query_df(f"""
        SELECT player_name, cause, killer_name, weapon_used,
               ROUND(x,1) as x, ROUND(y,1) as y, ROUND(z,1) as z,
               dimension, player_hp, player_food, player_armor,
               DATE_FORMAT(timestamp,'%Y-%m-%d %H:%i:%s') as timestamp
        FROM player_fallen_events
        WHERE 1=1 {df} {sf} {pf}
        ORDER BY timestamp DESC LIMIT {limit}
    """)


def get_revive_events(players=None, start=None, end=None, limit=500, servers=None):
    """List of player_revive_events with full detail."""
    pf = _player_f("fallen_player_name", players)
    df = _date_f("timestamp", start, end)
    sf = _server_f("server_id", servers)
    return query_df(f"""
        SELECT fallen_player_name, reviver_name, outcome,
               ROUND(x,1) as x, ROUND(y,1) as y, ROUND(z,1) as z,
               dimension, DATE_FORMAT(timestamp,'%Y-%m-%d %H:%i:%s') as timestamp
        FROM player_revive_events
        WHERE 1=1 {df} {sf} {pf}
        ORDER BY timestamp DESC LIMIT {limit}
    """)


def get_revive_stats(players=None, start=None, end=None, servers=None):
    """Per-player fallen/revived/died counts."""
    pf = _player_f("player_name", players)
    df = _date_f("timestamp", start, end)
    sf = _server_f("server_id", servers)
    fallen = query_df(f"""
        SELECT player_name, COUNT(*) as times_fallen
        FROM player_fallen_events WHERE 1=1 {df} {sf} {pf}
        GROUP BY player_name ORDER BY times_fallen DESC
    """)
    pf2 = _player_f("fallen_player_name", players)
    sf2 = _server_f("server_id", servers)
    df2 = _date_f("timestamp", start, end)
    revived = query_df(f"""
        SELECT fallen_player_name as player_name,
               SUM(outcome='revived') as times_revived,
               SUM(outcome='died') as times_died,
               SUM(outcome='logout') as times_logout
        FROM player_revive_events WHERE 1=1 {df2} {sf2} {pf2}
        GROUP BY fallen_player_name
    """)
    if fallen.empty:
        return fallen
    if revived.empty:
        return fallen
    return fallen.merge(revived, on="player_name", how="left").fillna(0)


def get_map_data(event_type, players=None, start=None, end=None, servers=None,
                 cx=None, cz=None, radius=None, limit=20000):
    """Coordinate data for the 2D/3D scatter map. Returns (DataFrame, error_str_or_None).

    When cx/cz/radius are provided only events inside that bounding box are returned
    (no row cap).  Without a spatial filter a safety cap of `limit` rows applies.
    """
    pf_n = _player_f("player_name", players)
    pf_p = _player_f("p.username", players)
    df_ts = _date_f("timestamp", start, end)
    df_d  = _date_f("d.timestamp", start, end)
    sf_n  = _server_f("server_id", servers)
    sf_d  = _server_f("d.server_id", servers)

    # Spatial filters — native column names differ by table
    spf_xz  = _spatial_f("player_x", "player_z", cx, cz, radius)
    spf_blk = _spatial_f("block_x",  "block_z",  cx, cz, radius)
    spf_d   = _spatial_f("d.x",      "d.z",      cx, cz, radius)
    spf_raw = _spatial_f("x",        "z",         cx, cz, radius)

    # No hard cap when a spatial filter is active; safety cap otherwise
    order_limit = "" if (cx is not None and cz is not None and radius is not None) \
                  else f"LIMIT {int(limit)}"

    # Each entry: (full_sql, fallback_sql_or_None)
    queries = {
        "deaths": (
            f"""SELECT p.username as player,
                   ROUND(d.x,1) as map_x, ROUND(d.z,1) as map_z, ROUND(d.y,1) as y,
                   CONCAT(COALESCE(d.cause,'?'),
                          CASE WHEN d.killer_name IS NOT NULL
                               THEN CONCAT(' \u2190 by ', d.killer_name) ELSE '' END) as details,
                   COALESCE(d.dimension,'?') as dimension,
                   DATE_FORMAT(d.timestamp,'%Y-%m-%d %H:%i:%s') as timestamp
            FROM player_deaths d JOIN players p ON d.player_uuid=p.uuid
            WHERE d.x IS NOT NULL {df_d} {sf_d} {pf_p} {spf_d}""",
            None,
        ),
        "pvp": (
            f"""SELECT p.username as player,
                   ROUND(d.x,1) as map_x, ROUND(d.z,1) as map_z, ROUND(d.y,1) as y,
                   CONCAT('Killed by ', d.killer_name, '  [', COALESCE(d.weapon_used,'?'), ']') as details,
                   COALESCE(d.dimension,'?') as dimension,
                   DATE_FORMAT(d.timestamp,'%Y-%m-%d %H:%i:%s') as timestamp
            FROM player_deaths d JOIN players p ON d.player_uuid=p.uuid
            WHERE d.killer_name IS NOT NULL {df_d} {sf_d} {pf_p} {spf_d}""",
            f"""SELECT p.username as player,
                   NULL as map_x, NULL as map_z, NULL as y,
                   CONCAT('Killed by ', d.killer_name, '  [', COALESCE(d.weapon_used,'?'), ']') as details,
                   NULL as dimension,
                   DATE_FORMAT(d.timestamp,'%Y-%m-%d %H:%i:%s') as timestamp
            FROM player_deaths d JOIN players p ON d.player_uuid=p.uuid
            WHERE d.killer_name IS NOT NULL {df_d} {sf_d} {pf_p}""",
        ),
        "kills": (
            f"""SELECT player_name as player,
                   ROUND(player_x,1) as map_x, ROUND(player_z,1) as map_z, ROUND(player_y,1) as y,
                   entity_type,
                   CONCAT('Killed ', entity_type, '  [', COALESCE(main_hand_item,'?'), ']') as details,
                   dimension, DATE_FORMAT(timestamp,'%Y-%m-%d %H:%i:%s') as timestamp
            FROM player_kill_events
            WHERE player_x IS NOT NULL {df_ts} {sf_n} {pf_n} {spf_xz}""",
            f"""SELECT player_name as player,
                   NULL as map_x, NULL as map_z, NULL as y,
                   entity_type,
                   CONCAT('Killed ', entity_type) as details,
                   NULL as dimension, DATE_FORMAT(timestamp,'%Y-%m-%d %H:%i:%s') as timestamp
            FROM player_kill_events WHERE 1=1 {df_ts} {sf_n} {pf_n}""",
        ),
        "attacks": (
            f"""SELECT player_name as player,
                   ROUND(player_x,1) as map_x, ROUND(player_z,1) as map_z, ROUND(player_y,1) as y,
                   CONCAT('Hit ', entity_type, ' for ', ROUND(actual_damage,1), ' dmg') as details,
                   dimension, DATE_FORMAT(timestamp,'%Y-%m-%d %H:%i:%s') as timestamp
            FROM attack_entity_events
            WHERE player_x IS NOT NULL {df_ts} {sf_n} {pf_n} {spf_xz}""",
            f"""SELECT player_name as player,
                   NULL as map_x, NULL as map_z, NULL as y,
                   CONCAT('Hit ', entity_type, ' for ', ROUND(actual_damage,1), ' dmg') as details,
                   NULL as dimension, DATE_FORMAT(timestamp,'%Y-%m-%d %H:%i:%s') as timestamp
            FROM attack_entity_events WHERE 1=1 {df_ts} {sf_n} {pf_n}""",
        ),
        "block_breaks": (
            f"""SELECT player_name as player,
                   CAST(block_x AS DOUBLE) as map_x, CAST(block_z AS DOUBLE) as map_z, block_y as y,
                   CONCAT('Broke ', block_type) as details,
                   dimension, DATE_FORMAT(timestamp,'%Y-%m-%d %H:%i:%s') as timestamp
            FROM block_break_events
            WHERE block_x IS NOT NULL {df_ts} {sf_n} {pf_n} {spf_blk}""",
            None,
        ),
        "block_places": (
            f"""SELECT player_name as player,
                   CAST(block_x AS DOUBLE) as map_x, CAST(block_z AS DOUBLE) as map_z, block_y as y,
                   CONCAT('Placed ', block_type) as details,
                   dimension, DATE_FORMAT(timestamp,'%Y-%m-%d %H:%i:%s') as timestamp
            FROM block_place_events
            WHERE block_x IS NOT NULL {df_ts} {sf_n} {pf_n} {spf_blk}""",
            None,
        ),
        "drops": (
            f"""SELECT player_name as player,
                   ROUND(player_x,1) as map_x, ROUND(player_z,1) as map_z, ROUND(player_y,1) as y,
                   CONCAT('Dropped ', item_count, 'x ', item_type) as details,
                   dimension, DATE_FORMAT(timestamp,'%Y-%m-%d %H:%i:%s') as timestamp
            FROM item_drop_events
            WHERE player_x IS NOT NULL {df_ts} {sf_n} {pf_n} {spf_xz}""",
            f"""SELECT player_name as player,
                   NULL as map_x, NULL as map_z, NULL as y,
                   CONCAT('Dropped ', item_count, 'x ', item_type) as details,
                   NULL as dimension, DATE_FORMAT(timestamp,'%Y-%m-%d %H:%i:%s') as timestamp
            FROM item_drop_events WHERE 1=1 {df_ts} {sf_n} {pf_n}""",
        ),
        "pickups": (
            f"""SELECT player_name as player,
                   ROUND(player_x,1) as map_x, ROUND(player_z,1) as map_z, ROUND(player_y,1) as y,
                   CONCAT('Picked up ', item_count, 'x ', item_type) as details,
                   dimension, DATE_FORMAT(timestamp,'%Y-%m-%d %H:%i:%s') as timestamp
            FROM item_pickup_events
            WHERE player_x IS NOT NULL {df_ts} {sf_n} {pf_n} {spf_xz}""",
            f"""SELECT player_name as player,
                   NULL as map_x, NULL as map_z, NULL as y,
                   CONCAT('Picked up ', item_count, 'x ', item_type) as details,
                   NULL as dimension, DATE_FORMAT(timestamp,'%Y-%m-%d %H:%i:%s') as timestamp
            FROM item_pickup_events WHERE 1=1 {df_ts} {sf_n} {pf_n}""",
        ),
        "crafts": (
            f"""SELECT player_name as player,
                   ROUND(player_x,1) as map_x, ROUND(player_z,1) as map_z, ROUND(player_y,1) as y,
                   CONCAT('Crafted ', COALESCE(crafted_item,'?')) as details,
                   dimension, DATE_FORMAT(timestamp,'%Y-%m-%d %H:%i:%s') as timestamp
            FROM crafting_event
            WHERE player_x IS NOT NULL {df_ts} {sf_n} {pf_n} {spf_xz}""",
            f"""SELECT player_name as player,
                   NULL as map_x, NULL as map_z, NULL as y,
                   CONCAT('Crafted ', COALESCE(crafted_item,'?')) as details,
                   NULL as dimension, DATE_FORMAT(timestamp,'%Y-%m-%d %H:%i:%s') as timestamp
            FROM crafting_event WHERE 1=1 {df_ts} {sf_n} {pf_n}""",
        ),
        "fallen": (
            f"""SELECT player_name as player,
                   ROUND(x,1) as map_x, ROUND(z,1) as map_z, ROUND(y,1) as y,
                   CONCAT('Fallen: ', COALESCE(cause,'?'),
                          CASE WHEN killer_name IS NOT NULL
                               THEN CONCAT(' ← by ', killer_name) ELSE '' END) as details,
                   COALESCE(dimension,'?') as dimension,
                   DATE_FORMAT(timestamp,'%Y-%m-%d %H:%i:%s') as timestamp
            FROM player_fallen_events
            WHERE x IS NOT NULL {df_ts} {sf_n} {_player_f("player_name", players)} {spf_raw}""",
            None,
        ),
        "revived": (
            f"""SELECT fallen_player_name as player,
                   ROUND(x,1) as map_x, ROUND(z,1) as map_z, ROUND(y,1) as y,
                   CONCAT('[', outcome, '] ',
                          CASE WHEN reviver_name IS NOT NULL
                               THEN CONCAT('by ', reviver_name) ELSE 'no reviver' END) as details,
                   COALESCE(dimension,'?') as dimension,
                   DATE_FORMAT(timestamp,'%Y-%m-%d %H:%i:%s') as timestamp
            FROM player_revive_events
            WHERE x IS NOT NULL {df_ts} {sf_n} {_player_f("fallen_player_name", players)} {spf_raw}""",
            None,
        ),
    }

    entry = queries.get(event_type)
    if not entry:
        return pd.DataFrame(), f"Unknown event type: {event_type}"

    full_sql, fallback_sql = entry
    df, err = query_df_safe(f"{full_sql} ORDER BY timestamp DESC {order_limit}")
    if err:
        logger.warning("Map full query [%s] failed (%s) — trying fallback", event_type, err)
        if fallback_sql:
            df, err2 = query_df_safe(f"{fallback_sql} ORDER BY timestamp DESC {order_limit}")
            if err2:
                return pd.DataFrame(), f"Full: {err} | Fallback: {err2}"
            return df, f"No coordinates available (schema missing position columns). Error: {err}"
        return pd.DataFrame(), err
    return df, None


# ─── Market ───────────────────────────────────────────────────────────────────
# Tables live in the `market` schema (same server/credentials).
# Timestamps are BIGINT milliseconds (Java System.currentTimeMillis()).

def _mdate_f(col, start, end):
    """AND-prefixed date filter for BIGINT millisecond timestamp columns."""
    if start and end:
        return (f"AND {col} BETWEEN UNIX_TIMESTAMP('{start} 00:00:00') * 1000 "
                f"AND UNIX_TIMESTAMP('{end} 23:59:59') * 1000")
    if start:
        return f"AND {col} >= UNIX_TIMESTAMP('{start} 00:00:00') * 1000"
    if end:
        return f"AND {col} <= UNIX_TIMESTAMP('{end} 23:59:59') * 1000"
    return ""


def get_market_kpis(start=None, end=None):
    # No _default_dates() fallback — None/None means "all time" for market data.
    mdf = _mdate_f("timestamp", start, end)
    result = {k: 0 for k in ["transactions", "transactions_all", "tax_collected",
                               "active_items", "active_traders", "total_tax_ever"]}
    try:
        conn = get_connection()
        try:
            with conn.cursor() as cur:
                # All transactions (includes fictional) — for diagnosis
                cur.execute(f"""
                    SELECT COUNT(*) as v FROM market.transactions WHERE 1=1 {mdf}
                """)
                row = cur.fetchone()
                result["transactions_all"] = int(row["v"]) if row else 0

                # Real transactions only
                cur.execute(f"""
                    SELECT COUNT(*) as v FROM market.transactions
                    WHERE is_fictional = FALSE {mdf}
                """)
                row = cur.fetchone()
                result["transactions"] = int(row["v"]) if row else 0

                cur.execute(f"""
                    SELECT COALESCE(SUM(tax_amount), 0) as v
                    FROM market.tax_records WHERE 1=1 {mdf}
                """)
                row = cur.fetchone()
                result["tax_collected"] = int(row["v"]) if row else 0

                cur.execute("SELECT COUNT(DISTINCT item_id) as v FROM market.markets")
                row = cur.fetchone()
                result["active_items"] = int(row["v"]) if row else 0

                cur.execute(f"""
                    SELECT COUNT(DISTINCT player_uuid) as v FROM (
                        SELECT seller_uuid as player_uuid
                        FROM market.transactions WHERE is_fictional = FALSE {mdf}
                        UNION
                        SELECT buyer_uuid
                        FROM market.transactions WHERE is_fictional = FALSE {mdf}
                    ) u
                """)
                row = cur.fetchone()
                result["active_traders"] = int(row["v"]) if row else 0

                cur.execute("""
                    SELECT COALESCE(total_tax_collected, 0) as v
                    FROM market.treasury WHERE id = 1
                """)
                row = cur.fetchone()
                result["total_tax_ever"] = int(row["v"]) if row else 0
        finally:
            conn.close()
    except Exception as e:
        logger.error("Market KPI error: %s", e)
    return result


def get_sell_buy_balance(start=None, end=None, limit=20):
    """Per player: trade count split as seller vs buyer."""
    mdf = _mdate_f("timestamp", start, end)
    return query_df(f"""
        SELECT
            player,
            SUM(as_seller)  AS sales,
            SUM(as_buyer)   AS purchases,
            SUM(as_seller) + SUM(as_buyer) AS total_trades,
            ROUND(SUM(as_seller) * 100.0 /
                  NULLIF(SUM(as_seller) + SUM(as_buyer), 0), 1) AS seller_pct
        FROM (
            SELECT seller_name AS player, 1 AS as_seller, 0 AS as_buyer
            FROM market.transactions WHERE is_fictional = FALSE {mdf}
            UNION ALL
            SELECT buyer_name, 0, 1
            FROM market.transactions WHERE is_fictional = FALSE {mdf}
        ) r
        GROUP BY player
        ORDER BY total_trades DESC LIMIT {limit}
    """)


def get_top_traded_items(start=None, end=None, limit=20):
    """Most traded items (goods side of transaction) by transaction count."""
    mdf = _mdate_f("t.timestamp", start, end)
    return query_df(f"""
        SELECT
            SUBSTRING_INDEX(ti.item_id, ':', -1)    AS item,
            ti.item_id                               AS item_id_full,
            COUNT(DISTINCT ti.transaction_id)        AS tx_count,
            SUM(ti.item_count)                       AS total_units,
            ROUND(m.current_price, 2)                AS current_price
        FROM market.transaction_items ti
        JOIN market.transactions t ON ti.transaction_id = t.transaction_id
        LEFT JOIN market.markets m ON ti.item_id = m.item_id
        WHERE t.is_fictional = FALSE AND ti.is_seller_item = TRUE {mdf}
        GROUP BY ti.item_id, m.current_price
        ORDER BY tx_count DESC LIMIT {limit}
    """)


def get_price_history_for_item(item_id, start=None, end=None):
    """Price history rows for a single item_id."""
    mdf = _mdate_f("timestamp", start, end)
    safe_id = item_id.replace("'", "''")
    return query_df(f"""
        SELECT
            FROM_UNIXTIME(timestamp / 1000) AS ts,
            ROUND(price, 2)                 AS price,
            volume
        FROM market.price_history
        WHERE item_id = '{safe_id}' {mdf}
        ORDER BY timestamp ASC LIMIT 2000
    """)


def get_price_volatility(start=None, end=None, limit=15):
    """Items ranked by price range % — most volatile first."""
    mdf = _mdate_f("timestamp", start, end)
    return query_df(f"""
        SELECT
            SUBSTRING_INDEX(item_id, ':', -1) AS item,
            item_id                           AS item_id_full,
            COUNT(*)                          AS price_points,
            ROUND(AVG(price),    2)           AS avg_price,
            ROUND(MIN(price),    2)           AS min_price,
            ROUND(MAX(price),    2)           AS max_price,
            ROUND(STDDEV(price), 2)           AS stddev,
            ROUND((MAX(price) - MIN(price)) /
                  NULLIF(AVG(price), 0) * 100, 1) AS range_pct
        FROM market.price_history
        WHERE 1=1 {mdf}
        GROUP BY item_id HAVING price_points >= 3
        ORDER BY range_pct DESC LIMIT {limit}
    """)


def get_tax_over_time(start=None, end=None):
    """Daily tax revenue and transaction count for timeline chart."""
    mdf = _mdate_f("timestamp", start, end)
    return query_df(f"""
        SELECT
            DATE(FROM_UNIXTIME(timestamp / 1000)) AS day,
            SUM(tax_amount)                       AS tax_total,
            COUNT(*)                              AS tax_events,
            ROUND(AVG(tax_rate) * 100, 2)         AS avg_rate_pct
        FROM market.tax_records
        WHERE 1=1 {mdf}
        GROUP BY day ORDER BY day
    """)


def get_suspicious_accumulators(start=None, end=None, limit=15):
    """Players who buy in ≥70% of their trades (potential hoarders)."""
    mdf = _mdate_f("timestamp", start, end)
    return query_df(f"""
        SELECT
            player,
            SUM(as_seller)  AS sales,
            SUM(as_buyer)   AS purchases,
            SUM(as_buyer) - SUM(as_seller) AS net_purchases,
            ROUND(SUM(as_buyer) * 100.0 /
                  NULLIF(SUM(as_seller) + SUM(as_buyer), 0), 1) AS buyer_pct
        FROM (
            SELECT seller_name AS player, 1 AS as_seller, 0 AS as_buyer
            FROM market.transactions WHERE is_fictional = FALSE {mdf}
            UNION ALL
            SELECT buyer_name, 0, 1
            FROM market.transactions WHERE is_fictional = FALSE {mdf}
        ) r
        GROUP BY player
        HAVING (SUM(as_seller) + SUM(as_buyer)) >= 5
           AND ROUND(SUM(as_buyer) * 100.0 /
                     NULLIF(SUM(as_seller) + SUM(as_buyer), 0), 1) >= 70
        ORDER BY buyer_pct DESC LIMIT {limit}
    """)


def get_recent_transactions(start=None, end=None, players=None, limit=200):
    """Recent transaction log with seller/buyer names and goods summary."""
    mdf = _mdate_f("t.timestamp", start, end)
    pf = ""
    if players:
        joined = ",".join(f"'{p}'" for p in players)
        pf = f"AND (t.seller_name IN ({joined}) OR t.buyer_name IN ({joined}))"
    return query_df(f"""
        SELECT
            t.seller_name  AS seller,
            t.buyer_name   AS buyer,
            DATE_FORMAT(FROM_UNIXTIME(t.timestamp / 1000),
                        '%Y-%m-%d %H:%i:%s')  AS timestamp,
            GROUP_CONCAT(
                CASE WHEN ti.is_seller_item = TRUE
                     THEN CONCAT(ti.item_count, 'x ',
                                 SUBSTRING_INDEX(ti.item_id, ':', -1))
                END ORDER BY ti.item_count DESC SEPARATOR ', '
            ) AS items_sold,
            SUM(CASE WHEN ti.is_seller_item = FALSE
                     THEN ti.item_count ELSE 0 END) AS currency_paid,
            t.is_fictional
        FROM market.transactions t
        LEFT JOIN market.transaction_items ti ON t.transaction_id = ti.transaction_id
        WHERE 1=1 {mdf} {pf}
        GROUP BY t.transaction_id, t.seller_name, t.buyer_name, t.timestamp, t.is_fictional
        ORDER BY t.timestamp DESC LIMIT {limit}
    """)


def get_sell_buy_balance_all(start=None, end=None, limit=20):
    """Same as get_sell_buy_balance but includes fictional transactions."""
    mdf = _mdate_f("timestamp", start, end)
    return query_df(f"""
        SELECT
            player,
            SUM(as_seller)  AS sales,
            SUM(as_buyer)   AS purchases,
            SUM(as_seller) + SUM(as_buyer) AS total_trades,
            ROUND(SUM(as_seller) * 100.0 /
                  NULLIF(SUM(as_seller) + SUM(as_buyer), 0), 1) AS seller_pct
        FROM (
            SELECT seller_name AS player, 1 AS as_seller, 0 AS as_buyer
            FROM market.transactions WHERE 1=1 {mdf}
            UNION ALL
            SELECT buyer_name, 0, 1
            FROM market.transactions WHERE 1=1 {mdf}
        ) r
        GROUP BY player
        ORDER BY total_trades DESC LIMIT {limit}
    """)


def get_top_traded_items_all(start=None, end=None, limit=20):
    """Same as get_top_traded_items but includes fictional transactions."""
    mdf = _mdate_f("t.timestamp", start, end)
    return query_df(f"""
        SELECT
            SUBSTRING_INDEX(ti.item_id, ':', -1)    AS item,
            ti.item_id                               AS item_id_full,
            COUNT(DISTINCT ti.transaction_id)        AS tx_count,
            SUM(ti.item_count)                       AS total_units,
            ROUND(m.current_price, 2)                AS current_price
        FROM market.transaction_items ti
        JOIN market.transactions t ON ti.transaction_id = t.transaction_id
        LEFT JOIN market.markets m ON ti.item_id = m.item_id
        WHERE ti.is_seller_item = TRUE {mdf}
        GROUP BY ti.item_id, m.current_price
        ORDER BY tx_count DESC LIMIT {limit}
    """)


def get_market_item_options():
    """All item IDs sorted by volume — for price history dropdown."""
    return query_df("""
        SELECT item_id,
               SUBSTRING_INDEX(item_id, ':', -1) AS label,
               total_volume
        FROM market.markets
        ORDER BY total_volume DESC
    """)


def get_economy_gini():
    """Wealth distribution proxy — total_currency_traded per player."""
    return query_df("""
        SELECT total_currency_traded AS wealth
        FROM market.player_trade_stats
        WHERE total_currency_traded > 0
        ORDER BY total_currency_traded ASC
    """)


# ─── Relations ────────────────────────────────────────────────────────────────

def get_kill_pairs(start=None, end=None, servers=None):
    """PvP kill counts per (killer, victim) pair — from player_deaths."""
    df = _date_f("d.timestamp", start, end)
    sf = _server_f("d.server_id", servers)
    return query_df(f"""
        SELECT d.killer_name AS killer, p.username AS victim, COUNT(*) AS kills
        FROM player_deaths d
        JOIN players p ON d.player_uuid = p.uuid
        WHERE d.killer_name IS NOT NULL AND d.killer_name != '' {df} {sf}
        GROUP BY d.killer_name, p.username
        ORDER BY kills DESC LIMIT 500
    """)


def get_market_trade_pairs(start=None, end=None):
    """Trade count per (seller, buyer) pair from market schema."""
    mdf = _mdate_f("timestamp", start, end)
    return query_df(f"""
        SELECT seller_name AS player_a, buyer_name AS player_b,
               COUNT(*) AS trade_count
        FROM market.transactions
        WHERE 1=1 {mdf}
        GROUP BY seller_name, buyer_name
        ORDER BY trade_count DESC LIMIT 500
    """)


def get_hourly_heatmap(players=None, start=None, end=None, servers=None):
    """Login count bucketed by day-of-week (1=Sun…7=Sat) × hour (0–23)."""
    pf = _player_f("p.username", players)
    df = _date_f("pl.login_time", start, end)
    sf = _server_f("pl.server_id", servers)
    return query_df(f"""
        SELECT
            DAYOFWEEK(pl.login_time)          AS dow,
            HOUR(pl.login_time)               AS hour,
            COUNT(*)                          AS logins,
            COUNT(DISTINCT pl.player_uuid)    AS unique_players
        FROM player_logins pl
        JOIN players p ON pl.player_uuid = p.uuid
        WHERE pl.login_time IS NOT NULL {pf} {df} {sf}
        GROUP BY dow, hour
        ORDER BY dow, hour
    """)


def get_session_anomalies(players=None, start=None, end=None, servers=None):
    """Sessions that are unusually short (<25% of player avg) or long (>3× avg)."""
    pf = _player_f("p.username", players)
    df = _date_f("pl.login_time", start, end)
    sf = _server_f("pl.server_id", servers)
    return query_df(f"""
        SELECT
            p.username AS player,
            DATE_FORMAT(pl.login_time,  '%Y-%m-%d %H:%i') AS login,
            DATE_FORMAT(pl.logout_time, '%Y-%m-%d %H:%i') AS logout,
            TIMESTAMPDIFF(MINUTE, pl.login_time, pl.logout_time) AS duration_min,
            ROUND(stats.avg_min, 0) AS player_avg_min,
            CASE
                WHEN TIMESTAMPDIFF(MINUTE, pl.login_time, pl.logout_time)
                     < stats.avg_min * 0.25 THEN 'very short'
                ELSE 'very long'
            END AS anomaly_type
        FROM player_logins pl
        JOIN players p ON pl.player_uuid = p.uuid
        JOIN (
            SELECT player_uuid,
                   AVG(TIMESTAMPDIFF(MINUTE, login_time, logout_time)) AS avg_min
            FROM player_logins
            WHERE logout_time IS NOT NULL
            GROUP BY player_uuid HAVING COUNT(*) >= 5
        ) stats ON pl.player_uuid = stats.player_uuid
        WHERE pl.logout_time IS NOT NULL {pf} {df} {sf}
          AND (
            TIMESTAMPDIFF(MINUTE, pl.login_time, pl.logout_time) < stats.avg_min * 0.25
            OR TIMESTAMPDIFF(MINUTE, pl.login_time, pl.logout_time) > stats.avg_min * 3.0
          )
        ORDER BY pl.login_time DESC LIMIT 300
    """)


# ─── Post-mortem ─────────────────────────────────────────────────────────────

def _ts_between(col, t0, t1):
    """Direct datetime BETWEEN filter — does NOT wrap in DATE(), preserves time."""
    return f"AND {col} BETWEEN '{t0}' AND '{t1}'"


def get_postmortem_before(player, t_before_str, t_death_str, servers=None):
    """Player's key events in the window before death — uses direct timestamp BETWEEN."""
    if not player:
        return pd.DataFrame()
    p  = _esc(player)
    tb = _ts_between
    sf = _server_f("server_id", servers)
    tw = _ts_between("timestamp", t_before_str, t_death_str)

    queries = [
        f"""SELECT DATE_FORMAT(timestamp,'%Y-%m-%d %H:%i:%s') AS ts, 'Attack' AS type,
                   CONCAT('Hit ', entity_type, ' for ', ROUND(actual_damage,1), ' dmg',
                          CASE WHEN main_hand_item IS NOT NULL THEN CONCAT('  [', main_hand_item, ']') ELSE '' END) AS details,
                   dimension, player_x AS x, player_y AS y, player_z AS z
            FROM attack_entity_events WHERE player_name='{p}' {tw} {sf}""",

        f"""SELECT DATE_FORMAT(timestamp,'%Y-%m-%d %H:%i:%s'), 'Kill',
                   CONCAT('Killed ', entity_type,
                          CASE WHEN main_hand_item IS NOT NULL THEN CONCAT('  [', main_hand_item, ']') ELSE '' END),
                   dimension, player_x, player_y, player_z
            FROM player_kill_events WHERE player_name='{p}' {tw} {sf}""",

        f"""SELECT DATE_FORMAT(d.timestamp,'%Y-%m-%d %H:%i:%s'), 'Death',
                   CONCAT('Died: ', COALESCE(d.cause,'?'),
                          CASE WHEN d.killer_name IS NOT NULL
                               THEN CONCAT('  ← by ', d.killer_name) ELSE '' END,
                          '  HP:', ROUND(d.player_hp,1)),
                   d.dimension, d.x, d.y, d.z
            FROM player_deaths d JOIN players p2 ON d.player_uuid=p2.uuid
            WHERE p2.username='{p}' {_ts_between('d.timestamp', t_before_str, t_death_str)}""",

        f"""SELECT DATE_FORMAT(timestamp,'%Y-%m-%d %H:%i:%s'), 'Block Break',
                   CONCAT('Broke ', block_type,
                          CASE WHEN tool_used IS NOT NULL THEN CONCAT('  [', tool_used, ']') ELSE '' END),
                   dimension, CAST(block_x AS DOUBLE), CAST(block_y AS DOUBLE), CAST(block_z AS DOUBLE)
            FROM block_break_events WHERE player_name='{p}' {tw} {sf}""",

        f"""SELECT DATE_FORMAT(timestamp,'%Y-%m-%d %H:%i:%s'), 'Block Place',
                   CONCAT('Placed ', block_type),
                   dimension, CAST(block_x AS DOUBLE), CAST(block_y AS DOUBLE), CAST(block_z AS DOUBLE)
            FROM block_place_events WHERE player_name='{p}' {tw} {sf}""",

        f"""SELECT DATE_FORMAT(timestamp,'%Y-%m-%d %H:%i:%s'), 'Item Drop',
                   CONCAT('Dropped ', item_count, 'x ', item_type),
                   dimension, player_x, player_y, player_z
            FROM item_drop_events WHERE player_name='{p}' {tw} {sf}""",

        f"""SELECT DATE_FORMAT(timestamp,'%Y-%m-%d %H:%i:%s'), 'Item Pickup',
                   CONCAT('Picked up ', item_count, 'x ', item_type),
                   dimension, player_x, player_y, player_z
            FROM item_pickup_events WHERE player_name='{p}' {tw} {sf}""",

        f"""SELECT DATE_FORMAT(timestamp,'%Y-%m-%d %H:%i:%s'), 'Chat',
                   CONCAT('"', message, '"'),
                   NULL, NULL, NULL, NULL
            FROM player_chat WHERE player_name='{p}' {_ts_between('timestamp', t_before_str, t_death_str)}""",

        f"""SELECT DATE_FORMAT(timestamp,'%Y-%m-%d %H:%i:%s'), 'Command',
                   CONCAT('/ ', command),
                   NULL, NULL, NULL, NULL
            FROM player_command_events WHERE player_name='{p}' {tw} {sf}""",

        f"""SELECT DATE_FORMAT(timestamp,'%Y-%m-%d %H:%i:%s'), 'Consumed',
                   CONCAT('Used ', item_type, '  HP:', ROUND(player_hp_before,1), '→', ROUND(player_hp_after,1)),
                   NULL, NULL, NULL, NULL
            FROM item_consume_events WHERE player_name='{p}' {tw} {sf}""",
    ]

    parts = []
    conn = get_connection()
    for sql in queries:
        try:
            with conn.cursor() as cur:
                cur.execute(f"SELECT * FROM ({sql}) t LIMIT 500")
                rows = cur.fetchall()
                if rows:
                    parts.append(pd.DataFrame(rows,
                        columns=["ts", "type", "details", "dimension", "x", "y", "z"]))
        except Exception:
            pass
    conn.close()

    if not parts:
        return pd.DataFrame()
    return pd.concat(parts, ignore_index=True).sort_values("ts")


def get_death_list(player, start=None, end=None, servers=None, limit=100):
    """All deaths for a player, with position and context, for post-mortem selection."""
    if not player:
        return pd.DataFrame()
    p   = _esc(player)
    df  = _date_f("d.timestamp", start, end)
    sf  = _server_f("d.server_id", servers)
    return query_df(f"""
        SELECT
            DATE_FORMAT(d.timestamp, '%Y-%m-%d %H:%i:%s')          AS ts,
            COALESCE(d.cause, 'unknown')                            AS cause,
            COALESCE(d.killer_name, '—')                            AS killer,
            COALESCE(d.weapon_used, '—')                            AS weapon,
            COALESCE(ROUND(d.player_hp, 1), '—')                   AS hp,
            COALESCE(CAST(ROUND(d.player_armor, 1) AS CHAR), '—')  AS armor,
            COALESCE(CAST(d.player_food AS CHAR), '—')             AS food,
            ROUND(d.x, 1) AS x, ROUND(d.y, 1) AS y, ROUND(d.z, 1) AS z,
            COALESCE(d.dimension, '?')                              AS dimension
        FROM player_deaths d
        JOIN players p2 ON d.player_uuid = p2.uuid
        WHERE p2.username = '{p}' {df} {sf}
        ORDER BY d.timestamp DESC
        LIMIT {int(limit)}
    """)


def get_postmortem_nearby(player, death_ts_str, death_x, death_z, death_dim,
                          radius=100, before_min=5, after_min=10, servers=None):
    """Other players' events within radius of death location around death time.
    Runs each table as a separate query so one failure doesn't silence all results."""
    from datetime import datetime, timedelta
    p  = _esc(player)
    sf = _server_f("server_id", servers)
    try:
        dt = datetime.strptime(str(death_ts_str), "%Y-%m-%d %H:%M:%S")
    except ValueError:
        return pd.DataFrame()
    t0 = (dt - timedelta(minutes=before_min)).strftime("%Y-%m-%d %H:%M:%S")
    t1 = (dt + timedelta(minutes=after_min)).strftime("%Y-%m-%d %H:%M:%S")
    dx, dz, r = float(death_x), float(death_z), float(radius)

    queries = [
        ("Attack", f"""
            SELECT player_name,
                   DATE_FORMAT(timestamp,'%Y-%m-%d %H:%i:%s') AS timestamp,
                   'Attack' AS event_type,
                   CONCAT('Hit ', entity_type, ' for ', ROUND(actual_damage,1), ' dmg') AS detail,
                   ROUND(player_x,1) AS x, ROUND(player_z,1) AS z
            FROM attack_entity_events
            WHERE player_name != '{p}'
              AND ABS(player_x - {dx}) <= {r} AND ABS(player_z - {dz}) <= {r}
              AND timestamp BETWEEN '{t0}' AND '{t1}' {sf}
        """),
        ("Kill", f"""
            SELECT player_name,
                   DATE_FORMAT(timestamp,'%Y-%m-%d %H:%i:%s'),
                   'Kill',
                   CONCAT('Killed ', entity_type),
                   ROUND(player_x,1), ROUND(player_z,1)
            FROM player_kill_events
            WHERE player_name != '{p}'
              AND ABS(player_x - {dx}) <= {r} AND ABS(player_z - {dz}) <= {r}
              AND timestamp BETWEEN '{t0}' AND '{t1}' {sf}
        """),
        ("Death", f"""
            SELECT px.username,
                   DATE_FORMAT(d2.timestamp,'%Y-%m-%d %H:%i:%s'),
                   'Death',
                   CONCAT('Died: ', COALESCE(d2.cause,'?'),
                          CASE WHEN d2.killer_name IS NOT NULL
                               THEN CONCAT(' ← by ', d2.killer_name) ELSE '' END),
                   ROUND(d2.x,1), ROUND(d2.z,1)
            FROM player_deaths d2
            JOIN players px ON d2.player_uuid = px.uuid
            WHERE px.username != '{p}'
              AND d2.x IS NOT NULL AND d2.z IS NOT NULL
              AND ABS(d2.x - {dx}) <= {r} AND ABS(d2.z - {dz}) <= {r}
              AND d2.timestamp BETWEEN '{t0}' AND '{t1}'
        """),
        ("Drop", f"""
            SELECT player_name,
                   DATE_FORMAT(timestamp,'%Y-%m-%d %H:%i:%s'),
                   'Item Drop',
                   CONCAT('Dropped ', item_count, 'x ', item_type),
                   ROUND(player_x,1), ROUND(player_z,1)
            FROM item_drop_events
            WHERE player_name != '{p}'
              AND ABS(player_x - {dx}) <= {r} AND ABS(player_z - {dz}) <= {r}
              AND timestamp BETWEEN '{t0}' AND '{t1}' {sf}
        """),
        ("Pickup", f"""
            SELECT player_name,
                   DATE_FORMAT(timestamp,'%Y-%m-%d %H:%i:%s'),
                   'Item Pickup',
                   CONCAT('Picked up ', item_count, 'x ', item_type),
                   ROUND(player_x,1), ROUND(player_z,1)
            FROM item_pickup_events
            WHERE player_name != '{p}'
              AND ABS(player_x - {dx}) <= {r} AND ABS(player_z - {dz}) <= {r}
              AND timestamp BETWEEN '{t0}' AND '{t1}' {sf}
        """),
    ]

    parts = []
    conn = get_connection()
    for _name, sql in queries:
        try:
            with conn.cursor() as cur:
                cur.execute(sql + " LIMIT 300")
                rows = cur.fetchall()
                if rows:
                    parts.append(pd.DataFrame(rows,
                        columns=["player_name", "timestamp", "event_type", "detail", "x", "z"]))
        except Exception:
            pass
    conn.close()

    if not parts:
        return pd.DataFrame()

    result = pd.concat(parts, ignore_index=True)
    result["dist_blocks"] = (
        (result["x"].astype(float) - dx).abs() + (result["z"].astype(float) - dz).abs()
    ).round(0)
    return result.sort_values(["timestamp", "dist_blocks"]).head(500)


def get_postmortem_loot(player, death_ts_str, death_x, death_z,
                        radius=50, after_min=10, servers=None):
    """Item pickups by OTHER players near death location AFTER the death."""
    from datetime import datetime, timedelta
    sf  = _server_f("server_id", servers)
    p   = _esc(player)
    try:
        dt  = datetime.strptime(str(death_ts_str), "%Y-%m-%d %H:%M:%S")
    except ValueError:
        return pd.DataFrame()
    ts0 = dt.strftime("%Y-%m-%d %H:%M:%S")
    t1  = (dt + timedelta(minutes=after_min)).strftime("%Y-%m-%d %H:%M:%S")
    dx, dz, r = float(death_x), float(death_z), float(radius)

    sql = f"""
        SELECT player_name,
               DATE_FORMAT(timestamp,'%Y-%m-%d %H:%i:%s') AS timestamp,
               item_type, item_count,
               ROUND(player_x,1) AS x, ROUND(player_z,1) AS z,
               ROUND(ABS(player_x - {dx}) + ABS(player_z - {dz}), 0) AS dist_blocks,
               TIMESTAMPDIFF(SECOND, '{ts0}', timestamp) AS secs_after_death
        FROM item_pickup_events
        WHERE player_name != '{p}'
          AND ABS(player_x - {dx}) <= {r} AND ABS(player_z - {dz}) <= {r}
          AND timestamp BETWEEN '{ts0}' AND '{t1}' {sf}
        ORDER BY timestamp
        LIMIT 200
    """
    try:
        conn = get_connection()
        with conn.cursor() as cur:
            cur.execute(sql)
            rows = cur.fetchall()
        conn.close()
        if not rows:
            return pd.DataFrame()
        return pd.DataFrame(rows, columns=[
            "player_name", "timestamp", "item_type", "item_count",
            "x", "z", "dist_blocks", "secs_after_death"
        ])
    except Exception:
        return pd.DataFrame()


# ─── Behavioral Profiles ─────────────────────────────────────────────────────

def get_profile_all_players(start=None, end=None, servers=None):
    """Aggregate combat/build/social stats per player for radar-chart profiling."""
    sdf_ts = _date_f("pl.login_time", start, end)
    ssf    = _server_f("pl.server_id", servers)
    # Sessions is the base — use all players who ever logged in
    result = query_df(f"""
        SELECT p.username AS player,
               COUNT(*) AS sessions,
               COALESCE(SUM(TIMESTAMPDIFF(MINUTE, pl.login_time,
                   COALESCE(pl.logout_time, NOW())) / 60.0), 0) AS session_hours
        FROM player_logins pl
        JOIN players p ON pl.player_uuid = p.uuid
        WHERE pl.login_time IS NOT NULL {sdf_ts} {ssf}
        GROUP BY p.username
    """)

    if result.empty:
        return result

    def _left_merge(base, extra_df, cols):
        if extra_df.empty:
            for c in cols:
                base[c] = 0
            return base
        return base.merge(extra_df[["player"] + cols], on="player", how="left")

    df_ts = _date_f("timestamp", start, end)
    sf    = _server_f("server_id", servers)

    # Total kills (PvP + PvE) — player_kill_events uses player_name directly
    kills_df = query_df(f"""
        SELECT player_name AS player, COUNT(*) AS kills
        FROM player_kill_events
        WHERE 1=1 {df_ts} {sf}
        GROUP BY player_name
    """)
    result = _left_merge(result, kills_df, ["kills"])

    # Deaths — requires JOIN with players table
    ddf_ts = _date_f("d.timestamp", start, end)
    dsf    = _server_f("d.server_id", servers)
    deaths_df = query_df(f"""
        SELECT p.username AS player, COUNT(*) AS deaths
        FROM player_deaths d
        JOIN players p ON d.player_uuid = p.uuid
        WHERE 1=1 {ddf_ts} {dsf}
        GROUP BY p.username
    """)
    result = _left_merge(result, deaths_df, ["deaths"])

    # Blocks broken
    broken_df = query_df(f"""
        SELECT player_name AS player, COUNT(*) AS blocks_broken
        FROM block_break_events
        WHERE 1=1 {df_ts} {sf}
        GROUP BY player_name
    """)
    result = _left_merge(result, broken_df, ["blocks_broken"])

    # Blocks placed
    placed_df = query_df(f"""
        SELECT player_name AS player, COUNT(*) AS blocks_placed
        FROM block_place_events
        WHERE 1=1 {df_ts} {sf}
        GROUP BY player_name
    """)
    result = _left_merge(result, placed_df, ["blocks_placed"])

    # Attacks (entity attacks)
    attacks_df = query_df(f"""
        SELECT player_name AS player, COUNT(*) AS attacks
        FROM attack_entity_events
        WHERE 1=1 {df_ts} {sf}
        GROUP BY player_name
    """)
    result = _left_merge(result, attacks_df, ["attacks"])

    # Chat messages
    chat_df = query_df(f"""
        SELECT player_name AS player, COUNT(*) AS chat_messages
        FROM player_chat
        WHERE 1=1 {df_ts} {sf}
        GROUP BY player_name
    """)
    result = _left_merge(result, chat_df, ["chat_messages"])

    return result.fillna(0)


def get_player_market_profile(start=None, end=None):
    """Per-player trade counts and currency totals from market.transactions."""
    mdf = _mdate_f("t.timestamp", start, end)
    return query_df(f"""
        SELECT player, SUM(total_sales) AS total_sales, SUM(total_buys) AS total_buys,
               SUM(currency_traded) AS currency_traded
        FROM (
            SELECT t.seller_name AS player,
                   COUNT(DISTINCT t.transaction_id) AS total_sales, 0 AS total_buys,
                   COALESCE(SUM(tc.item_count), 0) AS currency_traded
            FROM market.transactions t
            LEFT JOIN market.transaction_items tc
                ON tc.transaction_id = t.transaction_id AND tc.is_seller_item = FALSE
            WHERE t.seller_name IS NOT NULL AND t.seller_name != '' {mdf}
            GROUP BY t.seller_name
            UNION ALL
            SELECT t.buyer_name AS player,
                   0 AS total_sales, COUNT(DISTINCT t.transaction_id) AS total_buys,
                   COALESCE(SUM(tc.item_count), 0) AS currency_traded
            FROM market.transactions t
            LEFT JOIN market.transaction_items tc
                ON tc.transaction_id = t.transaction_id AND tc.is_seller_item = FALSE
            WHERE t.buyer_name IS NOT NULL AND t.buyer_name != '' {mdf}
            GROUP BY t.buyer_name
        ) t
        GROUP BY player
        ORDER BY currency_traded DESC LIMIT 500
    """)


def get_game_vs_market(start=None, end=None, servers=None):
    """Per-player merged game + market stats for correlation scatter."""
    game_df = get_profile_all_players(start, end, servers)
    market_df = get_player_market_profile(start, end)
    if game_df.empty:
        return pd.DataFrame()
    if not market_df.empty:
        df = game_df.merge(
            market_df[["player", "total_sales", "total_buys", "currency_traded"]],
            on="player", how="left",
        )
    else:
        df = game_df.copy()
        df["total_sales"] = 0
        df["total_buys"] = 0
        df["currency_traded"] = 0
    for col in ["total_sales", "total_buys", "currency_traded"]:
        df[col] = df[col].fillna(0)
    df["kd_ratio"] = (df["kills"] / df["deaths"].replace(0, 1)).round(2)
    return df


def get_player_daily_market(player, start=None, end=None):
    """Daily sell/buy count and volume for a specific player."""
    p = _esc(player)
    mdf = _mdate_f("t.timestamp", start, end)
    try:
        conn = get_connection()
        with conn.cursor() as cur:
            cur.execute(f"""
                SELECT DATE(FROM_UNIXTIME(t.timestamp / 1000)) AS day,
                       SUM(CASE WHEN t.seller_name = '{p}' THEN 1 ELSE 0 END) AS sales,
                       SUM(CASE WHEN t.buyer_name  = '{p}' THEN 1 ELSE 0 END) AS buys,
                       COALESCE(SUM(CASE WHEN t.seller_name = '{p}'
                                        THEN tc.item_count ELSE 0 END), 0)    AS sell_vol,
                       COALESCE(SUM(CASE WHEN t.buyer_name  = '{p}'
                                        THEN tc.item_count ELSE 0 END), 0)    AS buy_vol
                FROM market.transactions t
                LEFT JOIN market.transaction_items tc
                    ON tc.transaction_id = t.transaction_id AND tc.is_seller_item = FALSE
                WHERE (t.seller_name = '{p}' OR t.buyer_name = '{p}')
                  AND t.is_fictional = FALSE
                  {mdf}
                GROUP BY day ORDER BY day
            """)
            rows = cur.fetchall()
        conn.close()
        if not rows:
            return pd.DataFrame()
        df = pd.DataFrame(rows)
        df["day"] = pd.to_datetime(df["day"])
        return df
    except Exception as e:
        logger.warning("get_player_daily_market failed: %s", e)
        return pd.DataFrame()


def get_player_events_per_day(player, start=None, end=None, servers=None):
    """Daily kills + deaths + blocks_broken for a single player."""
    p = _esc(player)
    df_ts = _date_f("timestamp", start, end)
    df_d = _date_f("d.timestamp", start, end)
    sf = _server_f("server_id", servers)
    sf_d = _server_f("d.server_id", servers)

    kills_df = query_df(f"""
        SELECT DATE(timestamp) AS day, COUNT(*) AS kills
        FROM player_kill_events
        WHERE player_name = '{p}' {df_ts} {sf}
        GROUP BY day
    """)
    deaths_df = query_df(f"""
        SELECT DATE(d.timestamp) AS day, COUNT(*) AS deaths
        FROM player_deaths d
        JOIN players p2 ON d.player_uuid = p2.uuid
        WHERE p2.username = '{p}' {df_d} {sf_d}
        GROUP BY day
    """)
    blocks_df = query_df(f"""
        SELECT DATE(timestamp) AS day, COUNT(*) AS blocks_broken
        FROM block_break_events
        WHERE player_name = '{p}' {df_ts} {sf}
        GROUP BY day
    """)

    dfs = [d for d in [kills_df, deaths_df, blocks_df] if not d.empty]
    if not dfs:
        return pd.DataFrame()
    base = dfs[0]
    for d in dfs[1:]:
        base = base.merge(d, on="day", how="outer")
    base["day"] = pd.to_datetime(base["day"])
    return base.fillna(0).sort_values("day").reset_index(drop=True)


def get_mod_flags(start=None, end=None, servers=None):
    """Run rule-based flag checks across all players. Returns unified alert DataFrame."""
    flags = []

    # ── 1. Combat anomalies from profile data ──────────────────────────────
    try:
        profile_df = get_profile_all_players(start, end, servers)
        if not profile_df.empty:
            for _, r in profile_df.iterrows():
                kills  = float(r.get("kills",  0) or 0)
                deaths = float(r.get("deaths", 0) or 0)
                hours  = float(r.get("session_hours", 0) or 0)
                broken = float(r.get("blocks_broken", 0) or 0)
                player = str(r.get("player", "?"))

                if kills >= 5 and deaths == 0:
                    flags.append({"severity": "🔴 High", "player": player,
                                  "flag": "Immortal fighter",
                                  "detail": f"{int(kills)} kills · 0 deaths"})
                elif kills >= 10 and deaths > 0 and kills / deaths > 15:
                    flags.append({"severity": "🟡 Medium", "player": player,
                                  "flag": "Extreme K/D ratio",
                                  "detail": f"K/D = {kills/deaths:.1f}  ({int(kills)} kills / {int(deaths)} deaths)"})

                if hours > 0 and broken >= 500 and broken / hours > 5000:
                    flags.append({"severity": "🟡 Medium", "player": player,
                                  "flag": "Rapid mining",
                                  "detail": f"{int(broken/hours):,} blocks/hr  ({int(broken):,} total)"})
    except Exception as e:
        logger.warning("mod_flags profile check failed: %s", e)

    # ── 2. Market hoarders ─────────────────────────────────────────────────
    try:
        hoard_df = get_suspicious_accumulators(start, end)
        for _, r in hoard_df.iterrows():
            flags.append({"severity": "🟡 Medium", "player": str(r["player"]),
                          "flag": "Market hoarder",
                          "detail": (f"{float(r['buyer_pct']):.0f}% buys · "
                                     f"{int(r['purchases'])} purchases · "
                                     f"+{int(r['net_purchases'])} net")})
    except Exception as e:
        logger.warning("mod_flags hoard check failed: %s", e)

    # ── 3. Unusually long sessions (possible AFK/auto-clicker) ────────────
    try:
        sess_df = get_session_anomalies(None, start, end, servers)
        if not sess_df.empty:
            long_df = sess_df[sess_df["anomaly_type"] == "very long"]
            for _, r in long_df.iterrows():
                flags.append({"severity": "🔵 Info", "player": str(r["player"]),
                              "flag": "Unusually long session",
                              "detail": (f"{int(r['duration_min'])} min  "
                                         f"(player avg {int(r['player_avg_min'])} min) · "
                                         f"login {r['login']}")})
    except Exception as e:
        logger.warning("mod_flags session check failed: %s", e)

    if not flags:
        return pd.DataFrame(columns=["severity", "player", "flag", "detail"])
    return pd.DataFrame(flags)


def get_death_loot(start=None, end=None, servers=None, time_window_min=5, radius=20):
    """Item pickups by other players within time_window_min and radius blocks of each death."""
    df_d = _date_f("d.timestamp", start, end)
    sf_d = _server_f("d.server_id", servers)
    r    = float(radius)
    tw   = int(time_window_min)
    return query_df(f"""
        SELECT
            p2.username                                         AS victim,
            DATE_FORMAT(d.timestamp, '%Y-%m-%d %H:%i:%s')      AS death_time,
            d.killer_name                                       AS killer,
            pu.player_name                                      AS looter,
            pu.item_type,
            pu.item_count,
            TIMESTAMPDIFF(SECOND, d.timestamp, pu.timestamp)   AS seconds_after,
            ROUND(SQRT(POW(d.x - pu.player_x, 2)
                     + POW(d.z - pu.player_z, 2)), 1)          AS blocks_away
        FROM player_deaths d
        JOIN players p2 ON d.player_uuid = p2.uuid
        JOIN item_pickup_events pu
            ON  pu.timestamp BETWEEN d.timestamp
                             AND DATE_ADD(d.timestamp, INTERVAL {tw} MINUTE)
            AND SQRT(POW(d.x - pu.player_x, 2)
                   + POW(d.z - pu.player_z, 2)) <= {r}
            AND pu.player_name != p2.username
        WHERE d.x IS NOT NULL
          {df_d} {sf_d}
        ORDER BY d.timestamp DESC
        LIMIT 500
    """)


def get_item_supply_monopoly(start=None, end=None, limit=20):
    """Per-item: % of sales from the single top seller (monopoly indicator)."""
    mdf = _mdate_f("t.timestamp", start, end)
    return query_df(f"""
        SELECT inner_t.item_id,
               SUBSTRING_INDEX(inner_t.item_id, ':', -1) AS item,
               top_seller,
               seller_sales,
               total_sales,
               ROUND(seller_sales * 100.0 / total_sales, 1) AS monopoly_pct,
               ROUND(COALESCE(m.current_price, 0), 2)       AS avg_price
        FROM (
            SELECT ti.item_id,
                   t.seller_name                                                         AS top_seller,
                   COUNT(*)                                                              AS seller_sales,
                   SUM(COUNT(*)) OVER (PARTITION BY ti.item_id)                         AS total_sales,
                   RANK() OVER (PARTITION BY ti.item_id ORDER BY COUNT(*) DESC)         AS rnk
            FROM market.transaction_items ti
            JOIN market.transactions t ON ti.transaction_id = t.transaction_id
            WHERE t.seller_name IS NOT NULL AND t.is_fictional = FALSE
              AND ti.is_seller_item = TRUE {mdf}
            GROUP BY ti.item_id, t.seller_name
        ) inner_t
        LEFT JOIN market.markets m ON m.item_id = inner_t.item_id
        WHERE rnk = 1 AND total_sales >= 5
        ORDER BY monopoly_pct DESC, total_sales DESC
        LIMIT {limit}
    """)


def get_chronicle_data(start=None, end=None, servers=None):
    """Aggregate key stats for a text-narrative chronicle. Returns a plain dict."""
    sf   = _server_f("server_id", servers)
    sf_d = _server_f("d.server_id", servers)
    df   = _date_f("timestamp", start, end)
    df_d = _date_f("d.timestamp", start, end)
    mdf  = _mdate_f("timestamp", start, end)
    data = {}

    def _one(sql):
        try:
            r = query_df(sql)
            return r if not r.empty else None
        except Exception:
            return None

    r = _one(f"SELECT COUNT(DISTINCT player_uuid) AS active, COUNT(*) AS sessions FROM player_logins WHERE 1=1 {df} {sf}")
    if r is not None:
        data["active_players"] = int(r.iloc[0]["active"])
        data["sessions"]       = int(r.iloc[0]["sessions"])

    r = _one(f"SELECT COUNT(*) AS n FROM player_deaths WHERE 1=1 {df_d} {sf_d}")
    if r is not None: data["deaths"] = int(r.iloc[0]["n"])

    r = _one(f"SELECT COUNT(*) AS n FROM player_kill_events WHERE 1=1 {df} {sf}")
    if r is not None: data["kills"] = int(r.iloc[0]["n"])

    r = _one(f"SELECT COUNT(*) AS n FROM block_break_events WHERE 1=1 {df} {sf}")
    if r is not None: data["blocks_broken"] = int(r.iloc[0]["n"])

    r = _one(f"SELECT COUNT(*) AS n FROM player_chat WHERE 1=1 {df} {sf}")
    if r is not None: data["chat_messages"] = int(r.iloc[0]["n"])

    r = _one(f"""SELECT COUNT(DISTINCT t.transaction_id) AS n,
                        COALESCE(SUM(tc.item_count), 0) AS vol
                 FROM market.transactions t
                 LEFT JOIN market.transaction_items tc
                     ON tc.transaction_id = t.transaction_id AND tc.is_seller_item = FALSE
                 WHERE t.is_fictional=FALSE {mdf}""")
    if r is not None:
        data["market_transactions"] = int(r.iloc[0]["n"])
        data["market_volume"]       = float(r.iloc[0]["vol"])

    try:
        tk = get_kills_per_player(None, start, end, servers)
        if not tk.empty:
            col_p = "player_name" if "player_name" in tk.columns else tk.columns[0]
            col_k = [c for c in tk.columns if c != col_p][0]
            data["top_killers"] = [(str(row[col_p]), int(row[col_k])) for _, row in tk.head(3).iterrows()]
    except Exception: pass

    try:
        tm = get_player_market_profile(start, end)
        if not tm.empty:
            data["top_traders"] = [(str(row["player"]), int(row.get("currency_traded", 0))) for _, row in tm.head(3).iterrows()]
    except Exception: pass

    try:
        dc = get_death_causes(None, start, end, servers)
        if not dc.empty:
            col_c = "cause" if "cause" in dc.columns else dc.columns[0]
            col_n = [c for c in dc.columns if c != col_c][0]
            data["top_death_cause"]   = str(dc.iloc[0][col_c])
            data["top_death_cause_n"] = int(dc.iloc[0][col_n])
    except Exception: pass

    try:
        pvp = query_df(f"""
            SELECT COUNT(*) AS n, killer_name AS k
            FROM player_deaths
            WHERE killer_name IS NOT NULL {df_d} {sf_d}
            GROUP BY killer_name ORDER BY n DESC LIMIT 1
        """)
        if not pvp.empty:
            data["top_pvp_killer"]   = str(pvp.iloc[0]["k"])
            data["top_pvp_killer_n"] = int(pvp.iloc[0]["n"])
    except Exception: pass

    return data


def get_player_position_trail(player, start=None, end=None, servers=None, limit=500):
    """Recent event coordinates for a player — approximates movement trail."""
    p    = _esc(player)
    df_ts = _date_f("timestamp", start, end)
    df_d  = _date_f("d.timestamp", start, end)
    sf    = _server_f("server_id", servers)
    sf_d  = _server_f("d.server_id", servers)
    return query_df(f"""
        SELECT ts, event_type,
               ROUND(x, 1) AS x, ROUND(y, 1) AS y, ROUND(z, 1) AS z, dimension
        FROM (
            SELECT timestamp AS ts, 'Kill'        AS event_type,
                   player_x  AS x, player_y AS y, player_z AS z, dimension
            FROM player_kill_events
            WHERE player_name = '{p}' {df_ts} {sf}

            UNION ALL
            SELECT timestamp, 'Attack',
                   player_x, player_y, player_z, dimension
            FROM attack_entity_events
            WHERE player_name = '{p}' {df_ts} {sf}

            UNION ALL
            SELECT d.timestamp, 'Death',
                   d.x, d.y, d.z, d.dimension
            FROM player_deaths d JOIN players p2 ON d.player_uuid = p2.uuid
            WHERE p2.username = '{p}' AND d.x IS NOT NULL {df_d} {sf_d}

            UNION ALL
            SELECT timestamp, 'Block Break',
                   CAST(block_x AS DOUBLE), CAST(block_y AS DOUBLE), CAST(block_z AS DOUBLE), dimension
            FROM block_break_events
            WHERE player_name = '{p}' {df_ts} {sf}

            UNION ALL
            SELECT timestamp, 'Item Drop',
                   player_x, player_y, player_z, dimension
            FROM item_drop_events
            WHERE player_name = '{p}' {df_ts} {sf}
        ) ev
        ORDER BY ts DESC
        LIMIT {limit}
    """)


def get_session_overlaps(start=None, end=None, servers=None, min_sessions=2):
    """Pairs of players who were online at the same time in N+ sessions."""
    df = _date_f("al.login_time", start, end)
    sf = _server_f("al.server_id", servers)
    return query_df(f"""
        SELECT pa.username AS player_a, pb.username AS player_b,
               COUNT(*) AS sessions_together
        FROM player_logins al
        JOIN players pa ON al.player_uuid = pa.uuid
        JOIN player_logins bl
            ON al.player_uuid < bl.player_uuid
           AND al.login_time  < COALESCE(bl.logout_time, NOW())
           AND bl.login_time  < COALESCE(al.logout_time, NOW())
        JOIN players pb ON bl.player_uuid = pb.uuid
        WHERE al.login_time IS NOT NULL {df} {sf}
        GROUP BY pa.username, pb.username
        HAVING COUNT(*) >= {int(min_sessions)}
        ORDER BY sessions_together DESC LIMIT 300
    """)


# ─── Kill Matrix ──────────────────────────────────────────────────────────────

def get_kill_matrix(start=None, end=None, servers=None, max_players=25):
    """PvP kill counts by (killer, victim) — for a heatmap. Long-form DataFrame."""
    df = _date_f("d.timestamp", start, end)
    sf = _server_f("d.server_id", servers)
    return query_df(f"""
        SELECT d.killer_name                     AS killer,
               p.username                        AS victim,
               COUNT(*)                          AS kills
        FROM player_deaths d
        JOIN players p ON d.player_uuid = p.uuid
        WHERE d.killer_name IS NOT NULL
          AND d.killer_name != p.username
          {df} {sf}
        GROUP BY d.killer_name, p.username
        ORDER BY kills DESC
        LIMIT {int(max_players) * int(max_players)}
    """)


# ─── Hourly Activity ──────────────────────────────────────────────────────────

def get_hourly_activity(start=None, end=None, servers=None):
    """Event count by hour-of-day (0–23) × day-of-week (1=Sun…7=Sat)."""
    if start and end:
        cond = f"DATE(timestamp) BETWEEN '{start}' AND '{end}'"
    else:
        cond = "1=1"
    sf = _server_f("server_id", servers)
    return query_df(f"""
        SELECT HOUR(timestamp)    AS hour,
               DAYOFWEEK(timestamp) AS dow,
               COUNT(*)           AS events
        FROM (
            SELECT timestamp FROM attack_entity_events  WHERE {cond} {sf}
            UNION ALL SELECT timestamp FROM block_break_events  WHERE {cond} {sf}
            UNION ALL SELECT timestamp FROM block_place_events  WHERE {cond} {sf}
            UNION ALL SELECT timestamp FROM item_drop_events    WHERE {cond} {sf}
            UNION ALL SELECT timestamp FROM item_pickup_events  WHERE {cond} {sf}
            UNION ALL SELECT timestamp FROM player_kill_events  WHERE {cond} {sf}
        ) all_events
        GROUP BY hour, dow
        ORDER BY dow, hour
    """)


# ─── Player Homes (from claim_positions JSON) ─────────────────────────────────

def get_player_homes():
    """
    Home position per player = first claim in players.claim_positions JSON.
    Falls back to last_x/last_z if no claims exist.
    """
    return query_df("""
        SELECT username AS player_name,
               CASE
                   WHEN claim_positions IS NOT NULL
                    AND claim_positions NOT IN ('null', '[]', '')
                    AND JSON_LENGTH(claim_positions) > 0
                   THEN CAST(JSON_UNQUOTE(JSON_EXTRACT(claim_positions, '$[0].x')) AS DECIMAL(10,1))
                   ELSE ROUND(last_x, 1)
               END AS home_x,
               CASE
                   WHEN claim_positions IS NOT NULL
                    AND claim_positions NOT IN ('null', '[]', '')
                    AND JSON_LENGTH(claim_positions) > 0
                   THEN CAST(JSON_UNQUOTE(JSON_EXTRACT(claim_positions, '$[0].z')) AS DECIMAL(10,1))
                   ELSE ROUND(last_z, 1)
               END AS home_z,
               CASE
                   WHEN claim_positions IS NOT NULL
                    AND claim_positions NOT IN ('null', '[]', '')
                    AND JSON_LENGTH(claim_positions) > 0
                   THEN JSON_UNQUOTE(JSON_EXTRACT(claim_positions, '$[0].name'))
                   ELSE NULL
               END AS home_label,
               CASE
                   WHEN claim_positions IS NOT NULL
                    AND claim_positions NOT IN ('null', '[]', '')
                    AND JSON_LENGTH(claim_positions) > 0
                   THEN 'claim' ELSE 'last_pos'
               END AS source
        FROM players
        WHERE last_x IS NOT NULL OR (
            claim_positions IS NOT NULL
            AND claim_positions NOT IN ('null', '[]', '')
        )
    """)


# ─── Trade Routes ─────────────────────────────────────────────────────────────

def get_trade_routes(start=None, end=None, time_window_sec=120, max_distance=50, servers=None):
    """
    Estimated inter-player trade routes.
    Returns DataFrame: from_player, to_player, item_type, count,
                       from_x, from_z, to_x, to_z, route_type ('barter'|'market')
    Barter = physical drop→pickup match (direct coords).
    Market = formal transaction, endpoints are player homes.
    """
    df_f  = _date_f("d.timestamp", start, end)
    mdf   = _mdate_f("t.timestamp", start, end)
    sf    = _server_f("d.server_id", servers)

    # ── Barter routes: drop→pickup with actual coordinates ────────────────────
    barter_df = query_df(f"""
        SELECT d.player_name                                                   AS from_player,
               p.player_name                                                   AS to_player,
               d.item_type,
               COUNT(*)                                                        AS count,
               ROUND(AVG(d.player_x), 1)                                      AS from_x,
               ROUND(AVG(d.player_z), 1)                                      AS from_z,
               ROUND(AVG(p.player_x), 1)                                      AS to_x,
               ROUND(AVG(p.player_z), 1)                                      AS to_z,
               DATE_FORMAT(MIN(d.timestamp), '%Y-%m-%d %H:%i')                AS first_trade,
               DATE_FORMAT(MAX(d.timestamp), '%Y-%m-%d %H:%i')                AS last_trade,
               'barter'                                                        AS route_type
        FROM item_drop_events d
        JOIN item_pickup_events p
            ON  d.item_type    = p.item_type
            AND d.player_name != p.player_name
            AND p.timestamp BETWEEN d.timestamp
                            AND DATE_ADD(d.timestamp, INTERVAL {int(time_window_sec)} SECOND)
            AND SQRT(POW(d.player_x - p.player_x,2) + POW(d.player_z - p.player_z,2))
                    <= {float(max_distance)}
        WHERE 1=1 {df_f} {sf}
        GROUP BY d.player_name, p.player_name, d.item_type
        ORDER BY count DESC
        LIMIT 1000
    """)

    # ── Market routes: formal transactions, home coords as endpoints ──────────
    homes_df = get_player_homes()
    market_routes = pd.DataFrame()

    if homes_df is not None and not homes_df.empty:
        homes = homes_df.set_index("player_name")[["home_x", "home_z"]]

        market_df = query_df(f"""
            SELECT t.seller_name AS from_player,
                   t.buyer_name  AS to_player,
                   COALESCE(SUBSTRING_INDEX(MAX(ti.item_id), ':', -1), '?') AS item_type,
                   COUNT(DISTINCT t.transaction_id)                          AS count,
                   DATE_FORMAT(FROM_UNIXTIME(MIN(t.timestamp)/1000),
                               '%Y-%m-%d %H:%i')                             AS first_trade,
                   DATE_FORMAT(FROM_UNIXTIME(MAX(t.timestamp)/1000),
                               '%Y-%m-%d %H:%i')                             AS last_trade
            FROM market.transactions t
            LEFT JOIN market.transaction_items ti
                ON ti.transaction_id = t.transaction_id
               AND ti.is_seller_item = TRUE
            WHERE t.seller_name IS NOT NULL AND t.buyer_name IS NOT NULL
              AND t.is_fictional = FALSE {mdf}
            GROUP BY t.seller_name, t.buyer_name
            ORDER BY count DESC
            LIMIT 500
        """)

        if market_df is not None and not market_df.empty:
            market_df["from_x"] = market_df["from_player"].map(
                lambda p: homes.at[p, "home_x"] if p in homes.index else None)
            market_df["from_z"] = market_df["from_player"].map(
                lambda p: homes.at[p, "home_z"] if p in homes.index else None)
            market_df["to_x"]   = market_df["to_player"].map(
                lambda p: homes.at[p, "home_x"] if p in homes.index else None)
            market_df["to_z"]   = market_df["to_player"].map(
                lambda p: homes.at[p, "home_z"] if p in homes.index else None)
            market_df["route_type"] = "market"
            market_routes = market_df.dropna(subset=["from_x", "from_z", "to_x", "to_z"]).copy()

    # ── Combine ───────────────────────────────────────────────────────────────
    parts = [d for d in [barter_df, market_routes] if d is not None and not d.empty]
    if not parts:
        return pd.DataFrame()
    combined = pd.concat(parts, ignore_index=True)
    for col in ["from_x", "from_z", "to_x", "to_z", "count"]:
        combined[col] = pd.to_numeric(combined[col], errors="coerce")
    return combined.dropna(subset=["from_x", "from_z", "to_x", "to_z"])
