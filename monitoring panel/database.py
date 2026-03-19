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

def get_map_data(event_type, players=None, start=None, end=None, limit=5000, servers=None):
    """Coordinate data for the 2D scatter map. Returns (DataFrame, error_str_or_None)."""
    pf_n = _player_f("player_name", players)
    pf_p = _player_f("p.username", players)
    df_ts = _date_f("timestamp", start, end)
    df_d  = _date_f("d.timestamp", start, end)
    sf_n  = _server_f("server_id", servers)
    sf_d  = _server_f("d.server_id", servers)

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
            WHERE d.x IS NOT NULL {df_d} {sf_d} {pf_p}""",
            # fallback: no x/y/z → can't plot but at least returns data
            None,
        ),
        "pvp": (
            f"""SELECT p.username as player,
                   ROUND(d.x,1) as map_x, ROUND(d.z,1) as map_z, ROUND(d.y,1) as y,
                   CONCAT('Killed by ', d.killer_name, '  [', COALESCE(d.weapon_used,'?'), ']') as details,
                   COALESCE(d.dimension,'?') as dimension,
                   DATE_FORMAT(d.timestamp,'%Y-%m-%d %H:%i:%s') as timestamp
            FROM player_deaths d JOIN players p ON d.player_uuid=p.uuid
            WHERE d.killer_name IS NOT NULL {df_d} {sf_d} {pf_p}""",
            # fallback without x/y/z
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
                   CONCAT('Killed ', entity_type, '  [', COALESCE(main_hand_item,'?'), ']') as details,
                   dimension, DATE_FORMAT(timestamp,'%Y-%m-%d %H:%i:%s') as timestamp
            FROM player_kill_events
            WHERE player_x IS NOT NULL {df_ts} {sf_n} {pf_n}""",
            f"""SELECT player_name as player,
                   NULL as map_x, NULL as map_z, NULL as y,
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
            WHERE player_x IS NOT NULL {df_ts} {sf_n} {pf_n}""",
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
            WHERE block_x IS NOT NULL {df_ts} {sf_n} {pf_n}""",
            None,
        ),
        "block_places": (
            f"""SELECT player_name as player,
                   CAST(block_x AS DOUBLE) as map_x, CAST(block_z AS DOUBLE) as map_z, block_y as y,
                   CONCAT('Placed ', block_type) as details,
                   dimension, DATE_FORMAT(timestamp,'%Y-%m-%d %H:%i:%s') as timestamp
            FROM block_place_events
            WHERE block_x IS NOT NULL {df_ts} {sf_n} {pf_n}""",
            None,
        ),
        "drops": (
            f"""SELECT player_name as player,
                   ROUND(player_x,1) as map_x, ROUND(player_z,1) as map_z, ROUND(player_y,1) as y,
                   CONCAT('Dropped ', item_count, 'x ', item_type) as details,
                   dimension, DATE_FORMAT(timestamp,'%Y-%m-%d %H:%i:%s') as timestamp
            FROM item_drop_events
            WHERE player_x IS NOT NULL {df_ts} {sf_n} {pf_n}""",
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
            WHERE player_x IS NOT NULL {df_ts} {sf_n} {pf_n}""",
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
            WHERE player_x IS NOT NULL {df_ts} {sf_n} {pf_n}""",
            f"""SELECT player_name as player,
                   NULL as map_x, NULL as map_z, NULL as y,
                   CONCAT('Crafted ', COALESCE(crafted_item,'?')) as details,
                   NULL as dimension, DATE_FORMAT(timestamp,'%Y-%m-%d %H:%i:%s') as timestamp
            FROM crafting_event WHERE 1=1 {df_ts} {sf_n} {pf_n}""",
        ),
    }

    entry = queries.get(event_type)
    if not entry:
        return pd.DataFrame(), f"Unknown event type: {event_type}"

    full_sql, fallback_sql = entry
    df, err = query_df_safe(f"{full_sql} ORDER BY timestamp DESC LIMIT {limit}")
    if err:
        logger.warning("Map full query [%s] failed (%s) — trying fallback", event_type, err)
        if fallback_sql:
            df, err2 = query_df_safe(f"{fallback_sql} ORDER BY timestamp DESC LIMIT {limit}")
            if err2:
                return pd.DataFrame(), f"Full: {err} | Fallback: {err2}"
            return df, f"No coordinates available (schema missing position columns). Error: {err}"
        return pd.DataFrame(), err
    return df, None
