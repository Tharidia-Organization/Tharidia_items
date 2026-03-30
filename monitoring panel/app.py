"""
app.py  –  Tharidia God Eye Monitor
A Dash web-based dashboard for the MariaDB god_eye database.
Run:  python app.py   then open  http://localhost:8050
"""

import threading
import webbrowser
from datetime import date, timedelta

import math

import numpy as np
import pandas as pd
import dash
import dash_bootstrap_components as dbc
import plotly.express as px
import plotly.graph_objects as go
from dash import Input, Output, State, dash_table, dcc, html
from dash.exceptions import PreventUpdate

import database as db
from config import load_config, save_config

# ─── App ─────────────────────────────────────────────────────────────────────
app = dash.Dash(
    __name__,
    external_stylesheets=[dbc.themes.DARKLY, dbc.icons.BOOTSTRAP],
    suppress_callback_exceptions=True,
    title="Tharidia God Eye",
    meta_tags=[{"name": "viewport", "content": "width=device-width, initial-scale=1"}],
)
server = app.server

# ─── Style constants ─────────────────────────────────────────────────────────
BG = "#1a1d27"
CARD_BG = "#22253a"
CARD_BG2 = "#2a2d3e"
BORDER = "#3a3d52"
TEXT = "#e0e4f0"
MUTED = "#7a8099"
CHART_TEMPLATE = "plotly_dark"
CHART_COLORS = px.colors.qualitative.Vivid

_CELL = {
    "backgroundColor": CARD_BG2,
    "color": TEXT,
    "fontSize": "12px",
    "padding": "5px 8px",
    "border": f"1px solid {BORDER}",
    "textAlign": "left",
    "overflow": "hidden",
    "textOverflow": "ellipsis",
    "maxWidth": "200px",
}
_HEADER = {
    "backgroundColor": "#1a1d27",
    "color": MUTED,
    "fontWeight": "600",
    "border": f"1px solid {BORDER}",
    "fontSize": "11px",
}
_FILTER = {"backgroundColor": "#1a1d27", "color": TEXT, "border": f"1px solid {BORDER}"}


# ─── Helpers ─────────────────────────────────────────────────────────────────

def make_table(df, tid, page_size=20, height="520px", cols=None):
    if df is None or df.empty:
        return html.Div("No data for selected filters.", className="text-muted p-3 fst-italic")
    display = cols or list(df.columns)
    return dash_table.DataTable(
        id=f"tbl-{tid}",
        data=df[display].to_dict("records"),
        columns=[{"name": c.replace("_", " ").title(), "id": c} for c in display],
        page_size=page_size,
        sort_action="native",
        filter_action="native",
        export_format="csv",
        style_table={"overflowX": "auto", "maxHeight": height, "overflowY": "auto"},
        style_cell=_CELL,
        style_header=_HEADER,
        style_filter=_FILTER,
        style_data_conditional=[{"if": {"row_index": "odd"}, "backgroundColor": "#1e2130"}],
        tooltip_delay=0,
        tooltip_duration=None,
    )


def _deaths_table(df, tid, page_size=20, height="520px"):
    """Deaths DataTable with PvP rows highlighted and a prominent killed_by column."""
    if df is None or df.empty:
        return html.Div("No deaths for selected filters.", className="text-muted p-3 fst-italic")

    df = df.copy()

    # Build killed_by column — "⚔️ Name" for PvP, "—" otherwise
    def _kb(row):
        k = str(row.get("killer_name", "") or "").strip()
        return f"⚔️ {k}" if k not in ("", "None", "nan", "—", "null") else "—"

    df.insert(df.columns.get_loc("cause") + 1 if "cause" in df.columns else 1,
              "killed_by", df.apply(_kb, axis=1))

    # Drop the raw killer_name (absorbed into killed_by)
    if "killer_name" in df.columns:
        df = df.drop(columns=["killer_name"])

    pvp_style = [
        {"if": {"filter_query": '{killed_by} != "—"'},
         "backgroundColor": "#3d1515", "color": "#ff9999"},
        {"if": {"row_index": "odd", "filter_query": '{killed_by} = "—"'},
         "backgroundColor": "#1e2130"},
    ]

    return dash_table.DataTable(
        id=f"tbl-{tid}",
        data=df.to_dict("records"),
        columns=[{"name": c.replace("_", " ").title(), "id": c} for c in df.columns],
        page_size=page_size,
        sort_action="native",
        filter_action="native",
        export_format="csv",
        style_table={"overflowX": "auto", "maxHeight": height, "overflowY": "auto"},
        style_cell=_CELL,
        style_header=_HEADER,
        style_filter=_FILTER,
        style_data_conditional=pvp_style,
        tooltip_delay=0,
        tooltip_duration=None,
    )


def _with_pvp_highlight(df, event_col="event_type", detail_col="details"):
    """Add a 'killed_by' column to any events DataFrame and return (df, pvp_style).
    Works when detail_col contains '← by <name>' for death/kill rows."""
    df = df.copy()

    def _kb(row):
        if str(row.get(event_col, "")).lower() not in ("death", "kill", "pvp"):
            return ""
        d = str(row.get(detail_col, "") or "")
        if "← by" in d:
            try:
                return "⚔️ " + d.split("← by")[1].split("  ")[0].strip()
            except Exception:
                return "⚔️ PvP"
        return ""

    df["killed_by"] = df.apply(_kb, axis=1)

    pvp_style = [
        {"if": {"filter_query": '{killed_by} != ""'},
         "backgroundColor": "#3d1515", "color": "#ff9999"},
        {"if": {"row_index": "odd", "filter_query": '{killed_by} = ""'},
         "backgroundColor": "#1e2130"},
    ]
    return df, pvp_style


def _events_table(df, tid, page_size=25, height="520px"):
    """Generic events table with PvP death rows highlighted."""
    if df is None or df.empty:
        return html.Div("No events found.", className="text-muted p-3 fst-italic")

    # Try both column name variants used across different event DataFrames
    event_col  = "event_type" if "event_type" in df.columns else "type"
    detail_col = "details"    if "details"    in df.columns else "detail"

    df, pvp_style = _with_pvp_highlight(df, event_col, detail_col)

    return dash_table.DataTable(
        id=f"tbl-{tid}",
        data=df.to_dict("records"),
        columns=[{"name": "Killed by" if c == "killed_by" else c.replace("_", " ").title(), "id": c}
                 for c in df.columns],
        page_size=page_size,
        sort_action="native",
        filter_action="native",
        export_format="csv",
        style_table={"overflowX": "auto", "maxHeight": height, "overflowY": "auto"},
        style_cell=_CELL,
        style_header=_HEADER,
        style_filter=_FILTER,
        style_data_conditional=pvp_style,
        tooltip_delay=0,
        tooltip_duration=None,
    )


def kpi(label, value, color="primary", icon="📊"):
    fmt = f"{value:,}" if isinstance(value, int) else str(value)
    return dbc.Card(
        dbc.CardBody([
            html.Div(icon, style={"fontSize": "1.6rem", "marginBottom": "4px"}),
            html.H3(fmt, className=f"text-{color} mb-0 fw-bold"),
            html.Small(label, className="text-muted"),
        ], className="text-center py-2"),
        style={"backgroundColor": CARD_BG, "border": f"1px solid {BORDER}"}, className="h-100",
    )


def section(title, subtitle=None):
    return html.Div([
        html.Div([
            html.Span(title, className="fw-semibold text-light"),
            html.Span(f"  — {subtitle}", className="text-muted small ms-2") if subtitle else None,
        ]),
        html.Hr(style={"borderColor": BORDER, "marginTop": "6px", "marginBottom": "12px"}),
    ])


def chart_layout(fig):
    fig.update_layout(
        paper_bgcolor="rgba(0,0,0,0)",
        plot_bgcolor="rgba(0,0,0,0)",
        font_color=TEXT,
        margin=dict(l=10, r=10, t=30, b=10),
        legend=dict(bgcolor="rgba(0,0,0,0)"),
    )
    fig.update_xaxes(gridcolor=BORDER, zerolinecolor=BORDER)
    fig.update_yaxes(gridcolor=BORDER, zerolinecolor=BORDER)
    return fig


def no_data_fig(msg="No data"):
    fig = go.Figure()
    fig.add_annotation(text=msg, xref="paper", yref="paper", x=0.5, y=0.5,
                       showarrow=False, font=dict(color=MUTED, size=14))
    fig.update_layout(paper_bgcolor="rgba(0,0,0,0)", plot_bgcolor="rgba(0,0,0,0)",
                      xaxis_visible=False, yaxis_visible=False, margin=dict(l=0, r=0, t=0, b=0))
    return fig


# ─── Layout ──────────────────────────────────────────────────────────────────
def _filter_bar():
    return dbc.Card(
        dbc.CardBody(
            dbc.Row([
                dbc.Col([
                    html.Label("Server", className="text-muted small mb-1"),
                    dcc.Dropdown(id="filter-servers", placeholder="All servers…", multi=True,
                                 style={"backgroundColor": CARD_BG2, "color": TEXT},
                                 className="dbc"),
                ], xs=12, sm=6, md=2),
                dbc.Col([
                    html.Label("Players", className="text-muted small mb-1"),
                    dcc.Dropdown(id="filter-players", placeholder="All players…", multi=True,
                                 style={"backgroundColor": CARD_BG2, "color": TEXT},
                                 className="dbc"),
                ], xs=12, sm=6, md=2),
                dbc.Col([
                    html.Label("Date range", className="text-muted small mb-1"),
                    dcc.DatePickerRange(
                        id="filter-dates",
                        start_date=(date.today() - timedelta(days=7)).isoformat(),
                        end_date=date.today().isoformat(),
                        display_format="DD/MM/YYYY",
                        style={"width": "100%"},
                    ),
                ], xs=12, sm=6, md=3),
                dbc.Col([
                    html.Label("Quick range", className="text-muted small mb-1"),
                    dcc.Dropdown(
                        id="quick-range",
                        options=[
                            {"label": "Today", "value": "today"},
                            {"label": "Last 7 days", "value": "7d"},
                            {"label": "Last 30 days", "value": "30d"},
                            {"label": "This month", "value": "month"},
                            {"label": "All time", "value": "all"},
                        ],
                        value="7d", clearable=True,
                        placeholder="Custom range…",
                        style={"backgroundColor": CARD_BG2},
                        className="dbc",
                    ),
                ], xs=6, sm=4, md=2),
                dbc.Col([
                    html.Label("Auto-refresh", className="text-muted small mb-1"),
                    dcc.Dropdown(
                        id="refresh-select",
                        options=[
                            {"label": "Off", "value": 0},
                            {"label": "10 s", "value": 10000},
                            {"label": "30 s", "value": 30000},
                            {"label": "1 min", "value": 60000},
                            {"label": "5 min", "value": 300000},
                        ],
                        value=30000, clearable=False,
                        style={"backgroundColor": CARD_BG2},
                        className="dbc",
                    ),
                ], xs=6, sm=4, md=2),
                dbc.Col([
                    html.Label("\u00a0", className="d-block small mb-1"),
                    dbc.Button("↻ Refresh", id="btn-refresh", color="primary", size="sm", className="w-100"),
                ], xs=12, sm=4, md=1, className="d-flex align-items-end"),
            ], align="end", className="g-2"),
            className="py-2 px-3",
        ),
        style={"backgroundColor": CARD_BG, "border": f"1px solid {BORDER}"}, className="mb-3",
    )


app.layout = dbc.Container([
    # Header
    dbc.Row([
        dbc.Col([
            html.Div([
                html.Span("🛡️", style={"fontSize": "2rem", "marginRight": "12px"}),
                html.Div([
                    html.H4("Tharidia God Eye Monitor", className="mb-0 fw-bold",
                            style={"color": "#7eb8f7"}),
                    html.Small("Server analytics & real-time event dashboard", className="text-muted"),
                ]),
            ], className="d-flex align-items-center"),
        ], width=8),
        dbc.Col([
            html.Div(id="connection-badge", className="text-end mt-2"),
        ], width=4),
    ], className="py-3 border-bottom mb-3", style={"borderColor": BORDER}),

    # Filter bar
    _filter_bar(),

    # Auto-refresh interval (disabled=True means no refresh)
    dcc.Interval(id="auto-refresh", interval=30000, n_intervals=0),

    # Tabs
    dbc.Tabs([
        dbc.Tab(label="📊 Overview",        tab_id="overview"),
        dbc.Tab(label="👥 Players",         tab_id="players"),
        dbc.Tab(label="⚔️ Combat",          tab_id="combat"),
        dbc.Tab(label="💀 Deaths",          tab_id="deaths"),
        dbc.Tab(label="🌍 World",           tab_id="world"),
        dbc.Tab(label="🎒 Items",           tab_id="items"),
        dbc.Tab(label="💬 Chat",            tab_id="chat"),
        dbc.Tab(label="🏆 Advancements",    tab_id="advancements"),
        dbc.Tab(label="💰 Economy",         tab_id="economy"),
        dbc.Tab(label="🕵️ Market Intel",    tab_id="market_intel"),
        dbc.Tab(label="🔗 Correlations",    tab_id="market_corr"),
        dbc.Tab(label="🛡️ Moderation",      tab_id="moderation"),
        dbc.Tab(label="🕸️ Relations",       tab_id="relations"),
        dbc.Tab(label="👤 Profiles",        tab_id="profiles"),
        dbc.Tab(label="🏅 Rankings",        tab_id="rankings"),
        dbc.Tab(label="🔍 Investigate",     tab_id="investigate"),
        dbc.Tab(label="📍 Area Query",      tab_id="area_query"),
        dbc.Tab(label="🗺️ Surface Map",      tab_id="map"),
        dbc.Tab(label="🌐 3D Map",          tab_id="map3d"),
        dbc.Tab(label="🧭 Positions",       tab_id="positions"),
        dbc.Tab(label="📰 Chronicle",       tab_id="chronicle"),
        dbc.Tab(label="⚔️ Kill Matrix",     tab_id="killmatrix"),
        dbc.Tab(label="⏱️ Activity Clock",  tab_id="activity"),
        dbc.Tab(label="🚚 Trade Routes",    tab_id="traderoutes"),
        dbc.Tab(label="📋 Reports",         tab_id="reports"),
        dbc.Tab(label="⚙️ Settings",        tab_id="settings"),
    ], id="main-tabs", active_tab="overview", className="mb-0"),

    # Content
    dbc.Card(
        dbc.CardBody(html.Div(id="tab-content"), className="p-3"),
        style={"backgroundColor": CARD_BG, "border": f"1px solid {BORDER}", "borderTop": "none"},
    ),

    # One-shot interval to trigger dark styling after DOM is ready
    dcc.Interval(id="_style-init", interval=300, max_intervals=1, n_intervals=0),
    html.Div(id="_style-dummy", style={"display": "none"}),
], fluid=True, className="px-3 px-md-4", style={"backgroundColor": BG, "minHeight": "100vh"})


# ─── Dark input style injection via JavaScript MutationObserver ───────────────
app.clientside_callback(
    """
    function(n) {
        var BG='#2a2d3e', TEXT='#e0e4f0', BORDER='#3a3d52', HOVER='#33364d', SEL='#3d4060';

        // Inject a <style> tag covering every known selector variant
        if (!document.getElementById('_ged-dark')) {
            var s = document.createElement('style');
            s.id = '_ged-dark';
            s.textContent = [
                'input,textarea,select{background-color:'+BG+'!important;color:'+TEXT+'!important;border-color:'+BORDER+'!important}',
                '.form-control,.form-control:focus,.form-control:disabled{background-color:'+BG+'!important;color:'+TEXT+'!important;border-color:'+BORDER+'!important}',
                '.input-group-text{background-color:'+HOVER+'!important;color:'+TEXT+'!important;border-color:'+BORDER+'!important}',
                /* react-select v1 (dash < 2.x) */
                '.Select-control{background-color:'+BG+'!important;border-color:'+BORDER+'!important}',
                '.Select-placeholder,.Select-value-label{color:'+TEXT+'!important}',
                '.Select-input>input{color:'+TEXT+'!important;background:transparent!important}',
                '.Select-menu-outer{background-color:'+BG+'!important;border:1px solid '+BORDER+'!important;z-index:9999!important}',
                '.Select-option{background-color:'+BG+'!important;color:'+TEXT+'!important}',
                '.Select-option.is-focused,.Select-option:hover{background-color:'+HOVER+'!important}',
                '.Select-option.is-selected{background-color:'+SEL+'!important}',
                '.Select-arrow{border-top-color:#7a8099!important}',
                '.is-open>.Select-control{background-color:'+HOVER+'!important}',
                /* react-select v2+ (dash 2.x, class prefix "Select") */
                '.Select__control{background-color:'+BG+'!important;border-color:'+BORDER+'!important}',
                '.Select__single-value,.Select__placeholder{color:'+TEXT+'!important}',
                '.Select__input-container,.Select__input-container input{color:'+TEXT+'!important;background:transparent!important}',
                '.Select__menu{background-color:'+BG+'!important;border:1px solid '+BORDER+'!important;z-index:9999!important}',
                '.Select__option{background-color:'+BG+'!important;color:'+TEXT+'!important}',
                '.Select__option--is-focused{background-color:'+HOVER+'!important}',
                '.Select__option--is-selected{background-color:'+SEL+'!important}',
                '.Select__dropdown-indicator,.Select__clear-indicator{color:#7a8099!important}',
                '.Select__indicator-separator{background-color:'+BORDER+'!important}',
                '.Select__multi-value{background-color:'+SEL+'!important}',
                /* DatePicker */
                '.DateInput_input,.DateRangePickerInput,.SingleDatePickerInput{background-color:'+BG+'!important;color:'+TEXT+'!important;border-color:'+BORDER+'!important}',
                '.DayPicker,.CalendarMonthGrid,.CalendarMonth,.DayPicker_transitionContainer{background:'+BG+'!important}',
                '.CalendarDay__default{background:'+BG+'!important;color:'+TEXT+'!important;border-color:'+BORDER+'!important}',
                '.CalendarDay__default:hover{background:'+HOVER+'!important}',
                '.CalendarDay__selected,.CalendarDay__selected:hover{background:'+SEL+'!important}',
                '.CalendarMonth_caption{color:'+TEXT+'!important}',
                '.DayPickerNavigation_button__horizontalDefault{background:'+HOVER+'!important;border-color:'+BORDER+'!important}',
            ].join('');
            document.head.appendChild(s);
        }

        // Also force inline styles directly on elements (overrides any inline style="")
        function sp(el, prop, val) { el.style.setProperty(prop, val, 'important'); }
        function paint() {
            var pairs = [
                ['.Select-control, .Select__control',                          [['background-color',BG],['border-color',BORDER]]],
                ['.Select-menu-outer, .Select__menu',                          [['background-color',BG],['border-color',BORDER]]],
                ['.Select-option, .Select__option',                            [['background-color',BG],['color',TEXT]]],
                ['.Select-placeholder, .Select-value-label, .Select__single-value, .Select__placeholder', [['color',TEXT]]],
                ['.DateInput_input, .DateRangePickerInput',                    [['background-color',BG],['color',TEXT]]],
            ];
            pairs.forEach(function(pair) {
                document.querySelectorAll(pair[0]).forEach(function(el) {
                    pair[1].forEach(function(p){ sp(el, p[0], p[1]); });
                });
            });
        }

        paint();
        new MutationObserver(paint).observe(document.body, {childList:true, subtree:true, attributes:true});
        return '';
    }
    """,
    Output("_style-dummy", "children"),
    Input("_style-init", "n_intervals"),
    prevent_initial_call=False,
)


# ─── Tab renderers ────────────────────────────────────────────────────────────

def render_overview(players, start, end, servers=None):
    kpis = db.get_overview_kpis(start, end, servers=servers)
    activity_df = db.get_activity_by_type(start, end, servers=servers)
    top_players_df = db.get_top_active_players(start, end, servers=servers)
    online_df = db.get_online_players()

    # KPI row 1
    row1 = dbc.Row([
        dbc.Col(kpi("Online Now",      kpis.get("online_now", 0),      "success",  "🟢"), md=2, sm=4, xs=6, className="mb-3"),
        dbc.Col(kpi("Total Players",   kpis.get("total_players", 0),   "info",     "👥"), md=2, sm=4, xs=6, className="mb-3"),
        dbc.Col(kpi("Deaths",          kpis.get("deaths", 0),          "danger",   "💀"), md=2, sm=4, xs=6, className="mb-3"),
        dbc.Col(kpi("Player Kills",    kpis.get("kills", 0),           "warning",  "⚔️"), md=2, sm=4, xs=6, className="mb-3"),
        dbc.Col(kpi("Attacks",         kpis.get("attacks", 0),         "warning",  "🗡️"), md=2, sm=4, xs=6, className="mb-3"),
        dbc.Col(kpi("Blocks Broken",   kpis.get("blocks_broken", 0),   "secondary","⛏️"), md=2, sm=4, xs=6, className="mb-3"),
    ], className="mb-1")

    row2 = dbc.Row([
        dbc.Col(kpi("Blocks Placed",   kpis.get("blocks_placed", 0),   "secondary","🧱"), md=2, sm=4, xs=6, className="mb-3"),
        dbc.Col(kpi("Items Dropped",   kpis.get("item_drops", 0),      "secondary","📦"), md=2, sm=4, xs=6, className="mb-3"),
        dbc.Col(kpi("Items Picked Up", kpis.get("item_pickups", 0),    "secondary","🎒"), md=2, sm=4, xs=6, className="mb-3"),
        dbc.Col(kpi("Crafts",          kpis.get("crafts", 0),          "secondary","🔨"), md=2, sm=4, xs=6, className="mb-3"),
        dbc.Col(kpi("Chat Messages",   kpis.get("chat_messages", 0),   "info",     "💬"), md=2, sm=4, xs=6, className="mb-3"),
        dbc.Col(kpi("Commands Used",   kpis.get("commands", 0),        "info",     "📟"), md=2, sm=4, xs=6, className="mb-3"),
    ], className="mb-3")

    # Activity chart
    if not activity_df.empty:
        fig_activity = px.line(
            activity_df, x="hour", y="cnt", color="type",
            labels={"hour": "", "cnt": "Events", "type": "Category"},
            color_discrete_sequence=CHART_COLORS, template=CHART_TEMPLATE,
        )
        fig_activity = chart_layout(fig_activity)
        fig_activity.update_layout(title="Event Activity Over Time", height=300)
    else:
        fig_activity = no_data_fig("No activity data for this period")

    # Top players bar
    if not top_players_df.empty:
        fig_top = px.bar(
            top_players_df.sort_values("events"), x="events", y="player_name",
            orientation="h", labels={"events": "Events", "player_name": ""},
            color="events", color_continuous_scale="Viridis", template=CHART_TEMPLATE,
        )
        fig_top = chart_layout(fig_top)
        fig_top.update_layout(title="Top Active Players", height=300, showlegend=False,
                              coloraxis_showscale=False)
    else:
        fig_top = no_data_fig("No player data for this period")

    # Online players table
    online_section = html.Div([
        section("🟢 Currently Online"),
        make_table(online_df, "online", page_size=10) if not online_df.empty
        else html.Div("No players currently online.", className="text-muted fst-italic"),
    ])

    return html.Div([
        row1, row2,
        dbc.Row([
            dbc.Col(dcc.Graph(figure=fig_activity, config={"displayModeBar": False}), md=8),
            dbc.Col(dcc.Graph(figure=fig_top, config={"displayModeBar": False}), md=4),
        ], className="mb-3"),
        online_section,
    ])


def render_players(players, start, end, servers=None):
    players_df   = db.get_all_players()
    sessions_df  = db.get_session_stats(players, start, end, servers=servers)
    logins_df    = db.get_login_sessions(players, start, end, servers=servers)
    lpd_df       = db.get_logins_per_day(players, start, end, servers=servers)
    heatmap_df   = db.get_hourly_heatmap(players, start, end, servers=servers)
    anomaly_df   = db.get_session_anomalies(players, start, end, servers=servers)

    # ── Daily logins bar ──────────────────────────────────────────────────────
    fig_lpd = no_data_fig("No login data")
    if not lpd_df.empty:
        fig_lpd = px.bar(lpd_df, x="day", y="logins",
                         labels={"day": "", "logins": "Logins"},
                         color_discrete_sequence=CHART_COLORS[:1],
                         template=CHART_TEMPLATE)
        fig_lpd = chart_layout(fig_lpd)
        fig_lpd.update_layout(title="Daily Logins", height=250)

    # ── Playtime per player bar ───────────────────────────────────────────────
    fig_hours = no_data_fig("No session data")
    if not sessions_df.empty:
        fig_hours = px.bar(sessions_df.head(15), x="player", y="total_hours",
                           labels={"player": "", "total_hours": "Hours"},
                           color="total_hours", color_continuous_scale="Blues",
                           template=CHART_TEMPLATE)
        fig_hours = chart_layout(fig_hours)
        fig_hours.update_layout(title="Total Playtime per Player (hours)", height=250,
                                showlegend=False, coloraxis_showscale=False)

    # ── 7×24 login heatmap ────────────────────────────────────────────────────
    _DAY_LABELS = ["Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"]
    fig_heatmap = no_data_fig("No login data for heatmap")
    if not heatmap_df.empty:
        # Build 7×24 matrix (rows = days, cols = hours)
        matrix = [[0] * 24 for _ in range(7)]
        for _, row in heatmap_df.iterrows():
            d = int(row["dow"]) - 1          # DAYOFWEEK 1=Sun → index 0
            h = int(row["hour"])
            matrix[d][h] = int(row["logins"])

        fig_heatmap = go.Figure(go.Heatmap(
            z=matrix,
            x=[f"{h:02d}:00" for h in range(24)],
            y=_DAY_LABELS,
            colorscale="YlOrRd",
            hoverongaps=False,
            hovertemplate="<b>%{y}  %{x}</b><br>Logins: %{z}<extra></extra>",
        ))
        fig_heatmap.update_layout(
            title="Login Activity Heatmap — Day of Week × Hour of Day",
            xaxis=dict(title="Hour", tickangle=-45),
            yaxis=dict(title=""),
            height=300, template=CHART_TEMPLATE,
            margin=dict(l=60, r=20, t=45, b=60),
        )
        fig_heatmap = chart_layout(fig_heatmap)

    # ── Session duration distribution ─────────────────────────────────────────
    fig_dist = no_data_fig("No session data")
    if not logins_df.empty and "duration_min" in logins_df.columns:
        dur = logins_df["duration_min"].dropna()
        dur = dur[(dur > 0) & (dur < 600)]          # cap at 10 h for readability
        if not dur.empty:
            fig_dist = go.Figure(go.Histogram(
                x=dur, nbinsx=40,
                marker_color="#7eb8f7", opacity=0.8,
                hovertemplate="Duration: %{x} min<br>Sessions: %{y}<extra></extra>",
            ))
            fig_dist = chart_layout(fig_dist)
            fig_dist.update_layout(
                title="Session Duration Distribution (minutes, capped at 600)",
                xaxis_title="Minutes", yaxis_title="Sessions",
                height=250, template=CHART_TEMPLATE,
            )

    # ── Anomalous sessions section ────────────────────────────────────────────
    anomaly_section = html.Div()
    if not anomaly_df.empty:
        # Color rows by anomaly type
        anom_cond = [
            {"if": {"filter_query": '{anomaly_type} = "very short"'},
             "backgroundColor": "#3d1a1a", "color": "#ff9999"},
            {"if": {"filter_query": '{anomaly_type} = "very long"'},
             "backgroundColor": "#1a2d3d", "color": "#99ccff"},
        ]
        anomaly_section = html.Div([
            html.Br(),
            section("⚠️ Anomalous Sessions",
                    "sessions shorter than 25% or longer than 3× the player's average"),
            dbc.Row([
                dbc.Col([
                    dbc.Badge(f"{(anomaly_df['anomaly_type']=='very short').sum()} very short",
                              color="danger",  className="me-2 p-2"),
                    dbc.Badge(f"{(anomaly_df['anomaly_type']=='very long').sum()} very long",
                              color="primary", className="p-2"),
                ], className="mb-2"),
            ]),
            dash_table.DataTable(
                id="tbl-anomalies",
                data=anomaly_df.to_dict("records"),
                columns=[{"name": c.replace("_", " ").title(), "id": c}
                         for c in anomaly_df.columns],
                page_size=20, sort_action="native", filter_action="native",
                export_format="csv",
                style_table={"overflowX": "auto", "maxHeight": "420px", "overflowY": "auto"},
                style_cell=_CELL, style_header=_HEADER, style_filter=_FILTER,
                style_data_conditional=anom_cond,
            ),
        ])

    return html.Div([
        # Row 1 — daily logins + playtime
        dbc.Row([
            dbc.Col(dcc.Graph(figure=fig_lpd,   config={"displayModeBar": False}), md=6),
            dbc.Col(dcc.Graph(figure=fig_hours, config={"displayModeBar": False}), md=6),
        ], className="mb-3"),

        # Row 2 — 7×24 heatmap (full width) + session distribution
        section("🕐 Session Patterns"),
        dbc.Row([
            dbc.Col(dcc.Graph(figure=fig_heatmap, config={"displayModeBar": False}), md=8),
            dbc.Col(dcc.Graph(figure=fig_dist,    config={"displayModeBar": False}), md=4),
        ], className="mb-3"),

        # Player table + session stats
        section("All Registered Players"),
        make_table(players_df, "players-all", page_size=20),
        html.Br(),
        section("Session Stats", "total time and average session length per player"),
        make_table(sessions_df, "sessions-stats", page_size=20),
        html.Br(),
        section("Login History", "last 2000 sessions"),
        make_table(logins_df, "logins", page_size=20),

        # Anomalies (only shown if data exists)
        anomaly_section,
    ])


def render_combat(players, start, end, servers=None):
    dmg_df = db.get_damage_stats(players, start, end, servers=servers)
    entity_df = db.get_entity_hit_counts(players, start, end, servers=servers)
    ttk_df = db.get_ttk_by_entity(players, start, end, servers=servers)
    kills_df = db.get_kills_per_player(players, start, end, servers=servers)
    attack_df = db.get_attack_events(players, start, end, limit=500, servers=servers)
    player_kills_df = db.get_player_kills(players, start, end, limit=500, servers=servers)
    ttk_raw_df = db.get_time_to_kill(players, start, end, limit=500, servers=servers)

    # Damage per player bar
    fig_dmg = no_data_fig("No damage data")
    if not dmg_df.empty:
        fig_dmg = px.bar(
            dmg_df.head(15), x="player_name", y="total_damage",
            error_y=None, hover_data=["hits", "avg_damage", "max_hit"],
            labels={"player_name": "", "total_damage": "Total Damage"},
            color="total_damage", color_continuous_scale="Reds", template=CHART_TEMPLATE,
        )
        fig_dmg = chart_layout(fig_dmg)
        fig_dmg.update_layout(title="Total Damage Dealt per Player", height=280,
                              showlegend=False, coloraxis_showscale=False)

    # Entity pie
    fig_ent = no_data_fig("No entity data")
    if not entity_df.empty:
        fig_ent = px.pie(
            entity_df.head(15), names="entity_type", values="hits",
            color_discrete_sequence=CHART_COLORS, template=CHART_TEMPLATE, hole=0.3,
        )
        fig_ent = chart_layout(fig_ent)
        fig_ent.update_layout(title="Entities Attacked (by hit count)", height=280)

    # Kills bar
    fig_kills = no_data_fig("No kill data")
    if not kills_df.empty:
        fig_kills = px.bar(
            kills_df, x="player_name", y="kills",
            labels={"player_name": "", "kills": "Kills"},
            color="kills", color_continuous_scale="Oranges", template=CHART_TEMPLATE,
        )
        fig_kills = chart_layout(fig_kills)
        fig_kills.update_layout(title="Kills per Player", height=280,
                                showlegend=False, coloraxis_showscale=False)

    # TTK box
    fig_ttk = no_data_fig("No TTK data")
    if not ttk_raw_df.empty and "entity_type" in ttk_raw_df.columns:
        top_entities = ttk_raw_df["entity_type"].value_counts().head(12).index
        ttk_box_df = ttk_raw_df[ttk_raw_df["entity_type"].isin(top_entities)]
        fig_ttk = px.box(
            ttk_box_df, x="entity_type", y="duration_s",
            labels={"entity_type": "", "duration_s": "Seconds to Kill"},
            color="entity_type", template=CHART_TEMPLATE,
            color_discrete_sequence=CHART_COLORS,
        )
        fig_ttk = chart_layout(fig_ttk)
        fig_ttk.update_layout(title="Time to Kill Distribution by Entity", height=300,
                              showlegend=False)

    return html.Div([
        dbc.Row([
            dbc.Col(dcc.Graph(figure=fig_dmg, config={"displayModeBar": False}), md=6),
            dbc.Col(dcc.Graph(figure=fig_ent, config={"displayModeBar": False}), md=6),
        ], className="mb-2"),
        dbc.Row([
            dbc.Col(dcc.Graph(figure=fig_kills, config={"displayModeBar": False}), md=6),
            dbc.Col(dcc.Graph(figure=fig_ttk, config={"displayModeBar": False}), md=6),
        ], className="mb-3"),
        section("Damage Stats per Player"),
        make_table(dmg_df, "dmg-stats"),
        html.Br(),
        section("Time to Kill — by Entity Type", "averages and spread"),
        make_table(ttk_df, "ttk-entity"),
        html.Br(),
        section("Player Kill Events", "last 500"),
        make_table(player_kills_df, "player-kills", page_size=20),
        html.Br(),
        section("Attack Events", "last 500"),
        make_table(attack_df, "attacks", page_size=20),
        html.Br(),
        section("Time to Kill — Raw Events", "last 500"),
        make_table(ttk_raw_df, "ttk-raw", page_size=20),
    ])


def render_deaths(players, start, end, servers=None):
    deaths_df = db.get_deaths(players, start, end, limit=1000, servers=servers)
    causes_df = db.get_death_causes(players, start, end, servers=servers)
    timeline_df = db.get_deaths_per_day(players, start, end, servers=servers)
    deadliest_df = db.get_deadliest_players(players, start, end, servers=servers)

    fig_cause = no_data_fig("No death data")
    if not causes_df.empty:
        fig_cause = px.pie(
            causes_df, names="cause", values="deaths",
            hole=0.35, color_discrete_sequence=CHART_COLORS, template=CHART_TEMPLATE,
        )
        fig_cause = chart_layout(fig_cause)
        fig_cause.update_layout(title="Deaths by Cause", height=300)

    fig_timeline = no_data_fig("No death data")
    if not timeline_df.empty:
        fig_timeline = px.area(
            timeline_df, x="day", y="deaths",
            labels={"day": "", "deaths": "Deaths"},
            color_discrete_sequence=["#e74c3c"], template=CHART_TEMPLATE,
        )
        fig_timeline = chart_layout(fig_timeline)
        fig_timeline.update_layout(title="Deaths per Day", height=300)

    fig_players = no_data_fig("No death data")
    if not deadliest_df.empty:
        fig_players = px.bar(
            deadliest_df, x="player", y="deaths",
            hover_data=["avg_hp_at_death"],
            color="deaths", color_continuous_scale="Reds",
            labels={"player": "", "deaths": "Deaths"}, template=CHART_TEMPLATE,
        )
        fig_players = chart_layout(fig_players)
        fig_players.update_layout(title="Deaths per Player", height=300,
                                  showlegend=False, coloraxis_showscale=False)

    return html.Div([
        dbc.Row([
            dbc.Col(dcc.Graph(figure=fig_cause, config={"displayModeBar": False}), md=4),
            dbc.Col(dcc.Graph(figure=fig_timeline, config={"displayModeBar": False}), md=5),
            dbc.Col(dcc.Graph(figure=fig_players, config={"displayModeBar": False}), md=3),
        ], className="mb-3"),
        section("Death Log", "last 1000 deaths"),
        _deaths_table(deaths_df, "deaths"),
    ])


def render_world(players, start, end, servers=None):
    bb_df = db.get_block_breaks(players, start, end, servers=servers)
    top_bb = db.get_top_blocks_broken(players, start, end, servers=servers)
    bb_by_player = db.get_block_break_by_player(players, start, end, servers=servers)
    bp_df = db.get_block_places(players, start, end, servers=servers)
    top_bp = db.get_top_blocks_placed(players, start, end, servers=servers)
    bi_df = db.get_block_interact(players, start, end, servers=servers)
    fp_df = db.get_fluid_places(players, start, end, servers=servers)
    ei_df = db.get_entity_interact(players, start, end, servers=servers)

    # Top broken blocks bar
    fig_bb = no_data_fig("No block break data")
    if not top_bb.empty:
        fig_bb = px.bar(
            top_bb.head(20).sort_values("count"), x="count", y="block_type",
            orientation="h", labels={"count": "Breaks", "block_type": ""},
            color="count", color_continuous_scale="Oranges", template=CHART_TEMPLATE,
        )
        fig_bb = chart_layout(fig_bb)
        fig_bb.update_layout(title="Top Blocks Broken", height=380,
                             showlegend=False, coloraxis_showscale=False)

    # Blocks broken per player
    fig_bb_player = no_data_fig("No block break data")
    if not bb_by_player.empty:
        fig_bb_player = px.bar(
            bb_by_player.head(15), x="player_name", y="blocks_broken",
            labels={"player_name": "", "blocks_broken": "Blocks"},
            color="blocks_broken", color_continuous_scale="YlOrBr", template=CHART_TEMPLATE,
        )
        fig_bb_player = chart_layout(fig_bb_player)
        fig_bb_player.update_layout(title="Blocks Broken per Player", height=280,
                                    showlegend=False, coloraxis_showscale=False)

    # Top placed
    fig_bp = no_data_fig("No block place data")
    if not top_bp.empty:
        fig_bp = px.bar(
            top_bp.head(20).sort_values("count"), x="count", y="block_type",
            orientation="h", labels={"count": "Places", "block_type": ""},
            color="count", color_continuous_scale="Greens", template=CHART_TEMPLATE,
        )
        fig_bp = chart_layout(fig_bp)
        fig_bp.update_layout(title="Top Blocks Placed", height=380,
                             showlegend=False, coloraxis_showscale=False)

    return html.Div([
        dbc.Row([
            dbc.Col(dcc.Graph(figure=fig_bb, config={"displayModeBar": False}), md=4),
            dbc.Col(dcc.Graph(figure=fig_bp, config={"displayModeBar": False}), md=4),
            dbc.Col(dcc.Graph(figure=fig_bb_player, config={"displayModeBar": False}), md=4),
        ], className="mb-3"),
        dbc.Tabs([
            dbc.Tab([html.Br(), make_table(bb_df, "bb", page_size=20)],
                    label="⛏️ Block Breaks", tab_id="t-bb"),
            dbc.Tab([html.Br(), make_table(bp_df, "bp", page_size=20)],
                    label="🧱 Block Places", tab_id="t-bp"),
            dbc.Tab([html.Br(), make_table(bi_df, "bi", page_size=20)],
                    label="🖱️ Block Interact", tab_id="t-bi"),
            dbc.Tab([html.Br(), make_table(fp_df, "fp", page_size=20)],
                    label="💧 Fluid Place", tab_id="t-fp"),
            dbc.Tab([html.Br(), make_table(ei_df, "ei", page_size=20)],
                    label="🐄 Entity Interact", tab_id="t-ei"),
        ], active_tab="t-bb"),
    ])


def render_items(players, start, end, servers=None):
    drops_df = db.get_item_drops(players, start, end, servers=servers)
    picks_df = db.get_item_pickups(players, start, end, servers=servers)
    top_drops = db.get_top_items_dropped(players, start, end, servers=servers)
    top_picks = db.get_top_items_picked(players, start, end, servers=servers)
    craft_df = db.get_crafting(players, start, end, servers=servers)
    top_craft = db.get_top_crafted(players, start, end, servers=servers)
    consume_df = db.get_item_consume(players, start, end, servers=servers)
    top_consume = db.get_top_consumed(players, start, end, servers=servers)

    def top_bar(df, x_col, y_col, title, colorscale):
        if df is None or df.empty:
            return no_data_fig(f"No data — {title}")
        fig = px.bar(
            df.head(20).sort_values(x_col), x=x_col, y=y_col,
            orientation="h", labels={x_col: x_col.replace("_", " ").title(), y_col: ""},
            color=x_col, color_continuous_scale=colorscale, template=CHART_TEMPLATE,
        )
        fig = chart_layout(fig)
        fig.update_layout(title=title, height=340, showlegend=False, coloraxis_showscale=False)
        return fig

    return html.Div([
        dbc.Row([
            dbc.Col(dcc.Graph(figure=top_bar(top_drops, "total", "item_type", "Top Dropped Items", "Reds"),
                              config={"displayModeBar": False}), md=3),
            dbc.Col(dcc.Graph(figure=top_bar(top_picks, "total", "item_type", "Top Picked-Up Items", "Greens"),
                              config={"displayModeBar": False}), md=3),
            dbc.Col(dcc.Graph(figure=top_bar(top_craft, "count", "crafted_item", "Top Crafted Items", "Blues"),
                              config={"displayModeBar": False}), md=3),
            dbc.Col(dcc.Graph(figure=top_bar(top_consume, "uses", "item_type", "Top Consumed Items", "Purples"),
                              config={"displayModeBar": False}), md=3),
        ], className="mb-3"),
        dbc.Tabs([
            dbc.Tab([html.Br(), make_table(drops_df,   "drops",   page_size=20)], label="📦 Drops",       tab_id="ti-d"),
            dbc.Tab([html.Br(), make_table(picks_df,   "picks",   page_size=20)], label="🎒 Pickups",     tab_id="ti-p"),
            dbc.Tab([html.Br(), make_table(craft_df,   "craft",   page_size=20)], label="🔨 Crafting",    tab_id="ti-c"),
            dbc.Tab([html.Br(), make_table(consume_df, "consume", page_size=20)], label="🍖 Consumption", tab_id="ti-co"),
        ], active_tab="ti-d"),
    ])


def render_chat(players, start, end, servers=None):
    chat_df = db.get_chat(players, start, end, limit=500, servers=servers)
    cmds_df = db.get_commands(players, start, end, limit=500, servers=servers)
    top_cmds = db.get_top_commands(players, start, end, servers=servers)
    chat_pp = db.get_chat_per_player(players, start, end, servers=servers)

    fig_cmds = no_data_fig("No command data")
    if not top_cmds.empty:
        fig_cmds = px.bar(
            top_cmds.sort_values("uses"), x="uses", y="cmd",
            orientation="h", labels={"uses": "Uses", "cmd": ""},
            color="uses", color_continuous_scale="Teal", template=CHART_TEMPLATE,
        )
        fig_cmds = chart_layout(fig_cmds)
        fig_cmds.update_layout(title="Most Used Commands", height=350,
                               showlegend=False, coloraxis_showscale=False)

    fig_chat = no_data_fig("No chat data")
    if not chat_pp.empty:
        fig_chat = px.bar(
            chat_pp, x="player", y="messages",
            labels={"player": "", "messages": "Messages"},
            color="messages", color_continuous_scale="Viridis", template=CHART_TEMPLATE,
        )
        fig_chat = chart_layout(fig_chat)
        fig_chat.update_layout(title="Messages per Player", height=350,
                               showlegend=False, coloraxis_showscale=False)

    return html.Div([
        dbc.Row([
            dbc.Col(dcc.Graph(figure=fig_chat, config={"displayModeBar": False}), md=6),
            dbc.Col(dcc.Graph(figure=fig_cmds, config={"displayModeBar": False}), md=6),
        ], className="mb-3"),
        dbc.Tabs([
            dbc.Tab([html.Br(), make_table(chat_df, "chat", page_size=25)],
                    label="💬 Chat Log", tab_id="tc-chat"),
            dbc.Tab([html.Br(), make_table(cmds_df, "cmds", page_size=25)],
                    label="📟 Command Log", tab_id="tc-cmds"),
        ], active_tab="tc-chat"),
    ])


def render_advancements(players, start, end, servers=None):
    adv_df = db.get_advancements(players, start, end, limit=500, servers=servers)
    stats_df = db.get_advancement_stats(players, start, end, servers=servers)
    first_df = db.get_first_to_advance(start, end, servers=servers)

    fig_stats = no_data_fig("No advancement data")
    if not stats_df.empty:
        fig_stats = px.bar(
            stats_df, x="player_name", y="total",
            labels={"player_name": "", "total": "Advancements"},
            color="total", color_continuous_scale="Viridis", template=CHART_TEMPLATE,
        )
        fig_stats = chart_layout(fig_stats)
        fig_stats.update_layout(title="Advancements per Player", height=300,
                                showlegend=False, coloraxis_showscale=False)

    return html.Div([
        dcc.Graph(figure=fig_stats, config={"displayModeBar": False}, className="mb-3"),
        dbc.Tabs([
            dbc.Tab([html.Br(), make_table(adv_df,   "adv",   page_size=25)], label="🏆 All Advancements",  tab_id="ta-all"),
            dbc.Tab([html.Br(), make_table(stats_df, "astats", page_size=25)], label="📊 Stats per Player", tab_id="ta-stats"),
            dbc.Tab([html.Br(), make_table(first_df, "first",  page_size=25)], label="🥇 First Achieved",   tab_id="ta-first"),
        ], active_tab="ta-all"),
    ])


def render_reports(players, start, end):
    table_options = [{"label": k, "value": v} for k, v in db.REPORT_TABLES.items()]
    return html.Div([
        section("📋 Custom Reports", "Select any table, filter by date/player, and export to CSV"),
        dbc.Row([
            dbc.Col([
                html.Label("Table", className="text-muted small mb-1"),
                dcc.Dropdown(
                    id="report-table-select",
                    options=table_options,
                    value="attack_entity_events",
                    clearable=False,
                    style={"backgroundColor": CARD_BG2},
                    className="dbc",
                ),
            ], md=4),
            dbc.Col([
                html.Label("Row limit", className="text-muted small mb-1"),
                dcc.Dropdown(
                    id="report-limit",
                    options=[
                        {"label": "500", "value": 500},
                        {"label": "1 000", "value": 1000},
                        {"label": "5 000", "value": 5000},
                        {"label": "10 000", "value": 10000},
                    ],
                    value=1000, clearable=False,
                    style={"backgroundColor": CARD_BG2},
                    className="dbc",
                ),
            ], md=2),
            dbc.Col([
                html.Label("\u00a0", className="d-block small mb-1"),
                dbc.Button("Load Table", id="btn-load-report", color="success", className="w-100"),
            ], md=2),
        ], className="mb-3"),
        html.Div(id="report-output"),
    ])


def render_settings():
    cfg = load_config()
    db_cfg = cfg["database"]
    app_cfg = cfg["app"]
    return html.Div([
        section("⚙️ Database Connection"),
        dbc.Row([
            dbc.Col([
                dbc.Label("Host", className="text-muted small"),
                dbc.Input(id="s-host", value=db_cfg.get("host", "localhost"), type="text",
                          style={"backgroundColor": CARD_BG2, "color": TEXT, "border": f"1px solid {BORDER}"}),
            ], md=4),
            dbc.Col([
                dbc.Label("Port", className="text-muted small"),
                dbc.Input(id="s-port", value=str(db_cfg.get("port", 3306)), type="number",
                          style={"backgroundColor": CARD_BG2, "color": TEXT, "border": f"1px solid {BORDER}"}),
            ], md=2),
            dbc.Col([
                dbc.Label("Database", className="text-muted small"),
                dbc.Input(id="s-database", value=db_cfg.get("database", "god_eye"), type="text",
                          style={"backgroundColor": CARD_BG2, "color": TEXT, "border": f"1px solid {BORDER}"}),
            ], md=3),
        ], className="mb-3"),
        dbc.Row([
            dbc.Col([
                dbc.Label("User", className="text-muted small"),
                dbc.Input(id="s-user", value=db_cfg.get("user", "root"), type="text",
                          style={"backgroundColor": CARD_BG2, "color": TEXT, "border": f"1px solid {BORDER}"}),
            ], md=3),
            dbc.Col([
                dbc.Label("Password", className="text-muted small"),
                dbc.Input(id="s-password", value=db_cfg.get("password", ""), type="password",
                          style={"backgroundColor": CARD_BG2, "color": TEXT, "border": f"1px solid {BORDER}"}),
            ], md=3),
        ], className="mb-3"),
        dbc.Row([
            dbc.Col([
                dbc.Button("Test Connection", id="btn-test-conn", color="info", className="me-2"),
                dbc.Button("Save Settings", id="btn-save-settings", color="success"),
            ]),
        ], className="mb-2"),
        html.Div(id="settings-feedback", className="mt-2"),
    ])


# ─── Investigate ─────────────────────────────────────────────────────────────

def _input_style():
    return {"backgroundColor": CARD_BG2, "color": TEXT, "border": f"1px solid {BORDER}"}


def _dropdown_style():
    return {"backgroundColor": CARD_BG2, "color": TEXT}


_DIM_ICONS = {
    "minecraft:overworld": "🌍",
    "minecraft:the_nether": "🔥",
    "minecraft:the_end": "🌌",
}

def _dim_label(dim):
    """Human-readable label for a dimension key."""
    icon = _DIM_ICONS.get(dim, "🗺️")
    name = dim.split(":")[-1].replace("_", " ").title() if dim else "Unknown"
    return f"{icon} {name}"


def render_investigate(players, start, end):
    player_opts = [{"label": p, "value": p} for p in db.get_player_names()]

    # ── Timeline ──
    tl = dbc.Tab([
        html.Br(),
        dbc.Row([
            dbc.Col([
                html.Label("Player", className="text-muted small mb-1"),
                dcc.Dropdown(id="inv-player-select", options=player_opts,
                             placeholder="Select a player…", style=_dropdown_style(), className="dbc"),
            ], md=4),
            dbc.Col([
                html.Label("\u00a0", className="d-block small mb-1"),
                dbc.Button("▶ Load Timeline", id="inv-load-timeline", color="primary"),
            ], md=2),
        ], className="mb-3 align-items-end"),
        html.Div([
            html.Small("Shows everything the player did in chronological order: attacks, deaths, blocks, chat, commands, items…",
                       className="text-muted fst-italic"),
        ], className="mb-3"),
        html.Div("Select a player and click Load Timeline.", className="text-muted fst-italic",
                 id="inv-timeline-output"),
    ], label="📜 Player Timeline", tab_id="inv-tl")

    # ── PvP ──
    pvp = dbc.Tab([
        html.Br(),
        dbc.Row([
            dbc.Col([
                html.Label("Victim (optional)", className="text-muted small mb-1"),
                dcc.Dropdown(id="pvp-victim-select", options=player_opts, placeholder="Any victim…",
                             style=_dropdown_style(), className="dbc"),
            ], md=3),
            dbc.Col([
                html.Label("Killer (optional)", className="text-muted small mb-1"),
                dcc.Dropdown(id="pvp-killer-select", options=player_opts, placeholder="Any killer…",
                             style=_dropdown_style(), className="dbc"),
            ], md=3),
            dbc.Col([
                html.Label("\u00a0", className="d-block small mb-1"),
                dbc.Button("▶ Load PvP", id="pvp-load-btn", color="danger"),
            ], md=2),
        ], className="mb-3 align-items-end"),
        html.Div([
            html.Small("Shows all player-vs-player deaths with position map (X/Z). "
                       "Filter by victim and/or killer.", className="text-muted fst-italic"),
        ], className="mb-2"),
        html.Div(id="pvp-output"),
    ], label="⚔️ PvP Incidents", tab_id="inv-pvp")

    # ── Item Transfers ──
    tr = dbc.Tab([
        html.Br(),
        dbc.Row([
            dbc.Col([
                html.Label("Max time between drop → pickup", className="text-muted small mb-1"),
                dcc.Dropdown(id="transfer-window", options=[
                    {"label": "30 seconds", "value": 30},
                    {"label": "1 minute",   "value": 60},
                    {"label": "2 minutes",  "value": 120},
                    {"label": "5 minutes",  "value": 300},
                    {"label": "10 minutes", "value": 600},
                ], value=300, clearable=False, style=_dropdown_style(), className="dbc"),
            ], md=3),
            dbc.Col([
                html.Label("Max distance (blocks)", className="text-muted small mb-1"),
                dbc.Input(id="transfer-distance", type="number", value=30, min=1, max=500,
                          style=_input_style()),
            ], md=2),
            dbc.Col([
                html.Label("\u00a0", className="d-block small mb-1"),
                dbc.Button("▶ Detect Transfers", id="transfer-load-btn", color="warning"),
            ], md=2),
        ], className="mb-3 align-items-end"),
        html.Div([
            html.Small("Detects when Player A drops an item and Player B picks up the same item type "
                       "within the chosen time and distance — possible illegal handoffs.",
                       className="text-muted fst-italic"),
        ], className="mb-2"),
        html.Div(id="transfer-output"),
    ], label="📦 Item Transfers", tab_id="inv-tr")

    # ── Location Query ──
    loc = dbc.Tab([
        html.Br(),
        dbc.Row([
            dbc.Col([
                html.Label("X coordinate", className="text-muted small mb-1"),
                dbc.Input(id="loc-x", type="number", placeholder="e.g. 100", style=_input_style()),
            ], md=2),
            dbc.Col([
                html.Label("Z coordinate", className="text-muted small mb-1"),
                dbc.Input(id="loc-z", type="number", placeholder="e.g. -500", style=_input_style()),
            ], md=2),
            dbc.Col([
                html.Label("Radius (blocks)", className="text-muted small mb-1"),
                dbc.Input(id="loc-radius", type="number", value=50, min=1, max=5000, style=_input_style()),
            ], md=2),
            dbc.Col([
                html.Label("\u00a0", className="d-block small mb-1"),
                dbc.Button("▶ Search Area", id="loc-load-btn", color="info"),
            ], md=2),
        ], className="mb-3 align-items-end"),
        html.Div([
            html.Small("Returns all recorded events (attacks, deaths, block breaks, item drops…) "
                       "within the radius of the given coordinates. Useful for investigating incidents at a location.",
                       className="text-muted fst-italic"),
        ], className="mb-2"),
        html.Div(id="loc-output"),
    ], label="📍 Location Query", tab_id="inv-loc")

    # ── Player Proximity ──
    prox = dbc.Tab([
        html.Br(),
        dbc.Row([
            dbc.Col([
                html.Label("Player A", className="text-muted small mb-1"),
                dcc.Dropdown(id="prox-player-a", options=player_opts, placeholder="Player A…",
                             style=_dropdown_style(), className="dbc"),
            ], md=3),
            dbc.Col([
                html.Label("Player B", className="text-muted small mb-1"),
                dcc.Dropdown(id="prox-player-b", options=player_opts, placeholder="Player B…",
                             style=_dropdown_style(), className="dbc"),
            ], md=3),
            dbc.Col([
                html.Label("Max distance (blocks)", className="text-muted small mb-1"),
                dbc.Input(id="prox-distance", type="number", value=50, min=1, max=1000, style=_input_style()),
            ], md=2),
            dbc.Col([
                html.Label("\u00a0", className="d-block small mb-1"),
                dbc.Button("▶ Find Proximity", id="prox-load-btn", color="success"),
            ], md=2),
        ], className="mb-3 align-items-end"),
        html.Div([
            html.Small("Shows times when both players had activity within the given distance of each other "
                       "(±30s time window, using block/attack events as position data).",
                       className="text-muted fst-italic"),
        ], className="mb-2"),
        html.Div(id="prox-output"),
    ], label="👥 Player Proximity", tab_id="inv-prox")

    revive = dbc.Tab([
        html.Br(),
        dbc.Row([
            dbc.Col([
                html.Label("Player (optional)", className="text-muted small mb-1"),
                dcc.Dropdown(id="revive-player-select", options=player_opts, placeholder="Any player…",
                             style=_dropdown_style(), className="dbc"),
            ], md=3),
            dbc.Col([
                html.Label("\u00a0", className="d-block small mb-1"),
                dbc.Button("▶ Load Revive Events", id="revive-load-btn", color="warning"),
            ], md=2),
        ], className="mb-3 align-items-end"),
        html.Div([
            html.Small("Fallen events (player went down) and revive outcomes (revived / died / logout).",
                       className="text-muted fst-italic"),
        ], className="mb-2"),
        html.Div(id="revive-output"),
    ], label="💫 Fallen / Revive", tab_id="inv-revive")

    # ── Post-mortem ──
    pm = dbc.Tab([
        html.Br(),
        dbc.Row([
            dbc.Col([
                html.Label("Player", className="text-muted small mb-1"),
                dcc.Dropdown(id="pm-player-select", options=player_opts,
                             placeholder="Select a player…", style=_dropdown_style(), className="dbc"),
            ], md=4),
            dbc.Col([
                html.Label("Radius (blocks)", className="text-muted small mb-1"),
                dbc.Input(id="pm-radius", type="number", value=100, min=10, max=500, style=_input_style()),
            ], md=2),
            dbc.Col([
                html.Label("Minutes before", className="text-muted small mb-1"),
                dbc.Input(id="pm-before-min", type="number", value=5, min=1, max=30, style=_input_style()),
            ], md=2),
            dbc.Col([
                html.Label("Minutes after", className="text-muted small mb-1"),
                dbc.Input(id="pm-after-min", type="number", value=10, min=1, max=30, style=_input_style()),
            ], md=2),
            dbc.Col([
                html.Label("\u00a0", className="d-block small mb-1"),
                dbc.Button("▶ Load Deaths", id="pm-load-btn", color="primary", className="w-100"),
            ], md=2, className="d-flex align-items-end"),
        ], className="mb-3 align-items-end"),
        html.Div(id="pm-death-picker", className="mb-3"),
        html.Div(id="pm-output"),
    ], label="🔬 Post-mortem", tab_id="inv-pm")

    return html.Div([
        section("🔍 Investigation Tools",
                "Trace actions, prove or disprove incidents — uses global date filter"),
        dbc.Tabs([tl, pvp, revive, tr, loc, prox, pm], id="inv-subtabs", active_tab="inv-tl"),
    ])


# ─── Map View ─────────────────────────────────────────────────────────────────

def render_map_view(players, start, end):
    return html.Div([
        dcc.Store(id="map-data-store"),
        section("🗺️ Surface Density Map",
                "3D heatmap: X/Z world coordinates, surface height = event density per cell"),

        # ── Load controls ──
        dbc.Row([
            dbc.Col([
                html.Label("Event type", className="text-muted small mb-1"),
                dcc.Dropdown(id="map-event-type", options=[
                    {"label": "💀 Deaths (all causes)",           "value": "deaths"},
                    {"label": "⚔️ PvP Deaths (player-on-player)", "value": "pvp"},
                    {"label": "🗡️ Kills (what each player killed)", "value": "kills"},
                    {"label": "🏹 Attacks (all hit events)",       "value": "attacks"},
                    {"label": "⛏️ Block Breaks",                   "value": "block_breaks"},
                    {"label": "🧱 Block Places",                   "value": "block_places"},
                    {"label": "📦 Item Drops",                     "value": "drops"},
                    {"label": "🎒 Item Pickups",                   "value": "pickups"},
                    {"label": "🔨 Crafting locations",             "value": "crafts"},
                    {"label": "💫 Fallen (went down)",             "value": "fallen"},
                    {"label": "❤️ Revived / Died while fallen",    "value": "revived"},
                ], value="deaths", clearable=False, style=_dropdown_style(), className="dbc"),
            ], md=3),
            dbc.Col([
                html.Label("Cell size (blocks)", className="text-muted small mb-1"),
                dcc.Dropdown(id="map-cell-size", options=[
                    {"label": "1 block",    "value": 1},
                    {"label": "2 blocks",   "value": 2},
                    {"label": "4 blocks",   "value": 4},
                    {"label": "8 blocks",   "value": 8},
                    {"label": "16 blocks",  "value": 16},
                    {"label": "32 blocks",  "value": 32},
                    {"label": "64 blocks",  "value": 64},
                    {"label": "128 blocks", "value": 128},
                    {"label": "256 blocks", "value": 256},
                    {"label": "512 blocks", "value": 512},
                ], value=64, clearable=False, style=_dropdown_style(), className="dbc"),
            ], md=2),
            dbc.Col([
                html.Label("Color scale", className="text-muted small mb-1"),
                dcc.Dropdown(id="map-colorscale", options=[
                    {"label": "Plasma",  "value": "Plasma"},
                    {"label": "Viridis", "value": "Viridis"},
                    {"label": "Hot",     "value": "Hot"},
                    {"label": "Inferno", "value": "Inferno"},
                    {"label": "YlOrRd",  "value": "YlOrRd"},
                    {"label": "Turbo",   "value": "Turbo"},
                ], value="Plasma", clearable=False, style=_dropdown_style(), className="dbc"),
            ], md=2),
            dbc.Col([
                html.Label("Zone  X / Z / radius", className="text-muted small mb-1"),
                dbc.InputGroup([
                    dbc.Input(id="map-cx", placeholder="X", type="number", style={"maxWidth": "80px"}),
                    dbc.InputGroupText("/"),
                    dbc.Input(id="map-cz", placeholder="Z", type="number", style={"maxWidth": "80px"}),
                    dbc.InputGroupText("±"),
                    dbc.Input(id="map-radius", placeholder="r", type="number", style={"maxWidth": "70px"}),
                ], size="sm"),
                html.Small("Blank = load all (capped at 100 000)", className="text-muted fst-italic"),
            ], md=3),
            dbc.Col([
                html.Label("\u00a0", className="d-block small mb-1"),
                dbc.Button("▶ Load Map", id="map-load-btn", color="primary"),
            ], md=2),
        ], className="mb-2 align-items-end"),

        html.Div(
            html.Small(
                "💡 Drag to rotate · Scroll to zoom · Surface height = events per cell. "
                "Use Zone to focus on a specific area. Cell size controls grid resolution.",
                className="text-muted fst-italic",
            ),
            className="mb-3",
        ),

        # ── Dimension selector — hidden until data loads ──
        dbc.Row(
            dbc.Col(
                html.Div(id="map-dim-row", style={"display": "none"}, children=[
                    html.Label("Dimension", className="text-muted small mb-1"),
                    dcc.Dropdown(
                        id="map-dim-filter",
                        options=[], value="__all__",
                        clearable=False,
                        style=_dropdown_style(), className="dbc",
                    ),
                ]),
                md=5,
            ),
            className="mb-3",
        ),

        dcc.Graph(
            id="map-graph",
            figure=_no_data_surface("Click ▶ Load Map to render"),
            config={"displayModeBar": True, "scrollZoom": True,
                    "toImageButtonOptions": {"format": "png", "scale": 2}},
            style={"height": "680px"},
        ),
        html.Div(id="map-table-output"),
    ])


def _no_data_surface(msg="No data"):
    fig = go.Figure()
    fig.add_annotation(text=msg, xref="paper", yref="paper", x=0.5, y=0.5,
                       showarrow=False, font=dict(color="#7a8099", size=14))
    fig.update_layout(
        template="plotly_dark",
        paper_bgcolor="#1e2130",
        margin=dict(l=0, r=0, t=30, b=0),
    )
    return fig


def _build_surface_figure(df, event_type, dim, cell_size, colorscale):
    """Build a go.Surface density heatmap from raw event coordinates."""
    label = (event_type or "events").replace("_", " ").title()
    dim_tag = f" · {_dim_label(dim)}" if dim and dim != "__all__" else ""

    df = df.copy()
    df["map_x"] = _to_num(df["map_x"])
    df["map_z"] = _to_num(df["map_z"])
    plot_df = df.dropna(subset=["map_x", "map_z"])
    if plot_df.empty:
        return _no_data_3d("No coordinate data for this selection")

    cell = int(cell_size or 64)

    x_min_r = math.floor(plot_df["map_x"].min() / cell) * cell
    x_max_r = math.ceil(plot_df["map_x"].max() / cell) * cell + cell
    z_min_r = math.floor(plot_df["map_z"].min() / cell) * cell
    z_max_r = math.ceil(plot_df["map_z"].max() / cell) * cell + cell

    # Cap grid: max 400 bins per axis to keep the surface renderable
    MAX_BINS = 400
    x_span = x_max_r - x_min_r
    z_span = z_max_r - z_min_r
    cell = max(cell, math.ceil(x_span / MAX_BINS), math.ceil(z_span / MAX_BINS))
    # Recompute edges with possibly enlarged cell
    x_min_r = math.floor(plot_df["map_x"].min() / cell) * cell
    x_max_r = math.ceil(plot_df["map_x"].max() / cell) * cell + cell
    z_min_r = math.floor(plot_df["map_z"].min() / cell) * cell
    z_max_r = math.ceil(plot_df["map_z"].max() / cell) * cell + cell

    x_edges = np.arange(x_min_r, x_max_r + cell, cell)
    z_edges = np.arange(z_min_r, z_max_r + cell, cell)

    counts, _, _ = np.histogram2d(
        plot_df["map_x"].values,
        plot_df["map_z"].values,
        bins=[x_edges, z_edges],
    )
    # counts[i, j] = events at x_edges[i..i+1], z_edges[j..j+1]
    # go.Surface: x=columns (Z axis), y=rows (X axis)
    x_mids = (x_edges[:-1] + x_edges[1:]) / 2
    z_mids = (z_edges[:-1] + z_edges[1:]) / 2

    cs = colorscale or "Plasma"
    axis_style = dict(
        showbackground=True,
        backgroundcolor="#1a1d27",
        gridcolor=BORDER,
        tickfont=dict(color=MUTED, size=10),
    )

    fig = go.Figure(go.Surface(
        x=z_mids.tolist(),       # scene X axis = world Z
        y=x_mids.tolist(),       # scene Y axis = world X
        z=counts.tolist(),       # shape (len(x_mids), len(z_mids))
        colorscale=cs,
        colorbar=dict(title="Events", thickness=14, len=0.65, tickfont=dict(color=TEXT)),
        hovertemplate="X: %{y:.0f}<br>Z: %{x:.0f}<br>Events: %{z}<extra></extra>",
        opacity=0.92,
    ))

    fig.update_layout(
        template="plotly_dark",
        paper_bgcolor="#1e2130",
        font=dict(color=TEXT),
        margin=dict(l=0, r=0, t=50, b=0),
        title=dict(
            text=(f"Surface Density · {label}{dim_tag} — "
                  f"{len(plot_df):,} events · {cell}b cells · "
                  f"{len(x_mids)}×{len(z_mids)} grid"),
            font=dict(size=14, color=TEXT),
        ),
        scene=dict(
            bgcolor="#1a1d27",
            xaxis=dict(title=dict(text="Z  (South ↓)", font=dict(color=MUTED)), **axis_style),
            yaxis=dict(title=dict(text="X  (East →)",  font=dict(color=MUTED)), **axis_style),
            zaxis=dict(title=dict(text="Events",        font=dict(color=MUTED)), **axis_style),
            camera=_3D_CAMERA_PRESETS["iso"],
            aspectmode="manual",
            aspectratio=dict(x=1.6, y=1.6, z=0.55),
        ),
        uirevision="map-surface",
    )
    return fig


# ─── 3D Map View ──────────────────────────────────────────────────────────────

_MAP_EVENT_OPTIONS = [
    {"label": "💀 Deaths (all causes)",            "value": "deaths"},
    {"label": "⚔️ PvP Deaths (player-on-player)",  "value": "pvp"},
    {"label": "🗡️ Kills (what each player killed)", "value": "kills"},
    {"label": "🏹 Attacks (all hit events)",        "value": "attacks"},
    {"label": "⛏️ Block Breaks",                    "value": "block_breaks"},
    {"label": "🧱 Block Places",                    "value": "block_places"},
    {"label": "📦 Item Drops",                      "value": "drops"},
    {"label": "🎒 Item Pickups",                    "value": "pickups"},
    {"label": "🔨 Crafting locations",              "value": "crafts"},
    {"label": "💫 Fallen (went down)",              "value": "fallen"},
    {"label": "❤️ Revived / Died while fallen",     "value": "revived"},
]

_COLOR_BY_OPTIONS = [
    {"label": "Player",    "value": "player"},
    {"label": "Y height",  "value": "y"},
    {"label": "Event type","value": "event_type"},
]

_3D_CAMERA_PRESETS = {
    "iso":  dict(eye=dict(x=1.5, y=1.5, z=1.0)),
    "top":  dict(eye=dict(x=0,   y=0,   z=2.5)),
    "side": dict(eye=dict(x=2.5, y=0,   z=0.2)),
    "front":dict(eye=dict(x=0,   y=2.5, z=0.2)),
}


def render_map3d_view(players, start, end):
    return html.Div([
        dcc.Store(id="map3d-data-store"),
        section("🌐 3D Coordinate Map",
                "Three-dimensional view: X = East/West · Y = Height · Z = North/South"),

        # ── Controls row ──
        dbc.Row([
            dbc.Col([
                html.Label("Event type", className="text-muted small mb-1"),
                dcc.Dropdown(id="map3d-event-type", options=_MAP_EVENT_OPTIONS,
                             value="deaths", clearable=False,
                             style=_dropdown_style(), className="dbc"),
            ], md=3),
            dbc.Col([
                html.Label("Colour by", className="text-muted small mb-1"),
                dcc.Dropdown(id="map3d-color-by", options=_COLOR_BY_OPTIONS,
                             value="player", clearable=False,
                             style=_dropdown_style(), className="dbc"),
            ], md=2),
            dbc.Col([
                html.Label("Zone  X / Z / radius", className="text-muted small mb-1"),
                dbc.InputGroup([
                    dbc.Input(id="map3d-cx", placeholder="X", type="number",
                              style={"maxWidth": "80px"}),
                    dbc.InputGroupText("/"),
                    dbc.Input(id="map3d-cz", placeholder="Z", type="number",
                              style={"maxWidth": "80px"}),
                    dbc.InputGroupText("±"),
                    dbc.Input(id="map3d-radius", placeholder="r", type="number", value=500,
                              style={"maxWidth": "70px"}),
                ], size="sm"),
                html.Small("Leave blank to load all (capped at 20 000)",
                           className="text-muted fst-italic"),
            ], md=4),
            dbc.Col([
                html.Label("\u00a0", className="d-block small mb-1"),
                dbc.Button("▶ Load 3D Map", id="map3d-load-btn", color="primary"),
            ], md=2),
        ], className="mb-2 align-items-end"),

        html.Div(
            html.Small(
                "💡 Drag to rotate · Scroll to zoom · Double-click legend to isolate · "
                "Hover for details. X = East/West, Y = Height (sea level ≈ 63), Z = North/South. "
                "Enter X/Z/radius to jump to a zone — reload to fetch a new area.",
                className="text-muted fst-italic",
            ),
            className="mb-3",
        ),

        # ── Dimension + camera presets row (hidden until data loads) ──
        html.Div(id="map3d-controls-row", style={"display": "none"}, children=[
            dbc.Row([
                dbc.Col([
                    html.Label("Dimension", className="text-muted small mb-1"),
                    dcc.Dropdown(id="map3d-dim-filter", options=[], value="__all__",
                                 clearable=False, style=_dropdown_style(), className="dbc"),
                ], md=4),
                dbc.Col([
                    html.Label("Camera preset", className="text-muted small mb-1"),
                    dbc.ButtonGroup([
                        dbc.Button("Isometric", id="map3d-cam-iso",   size="sm", outline=True, color="secondary"),
                        dbc.Button("Top-down",  id="map3d-cam-top",   size="sm", outline=True, color="secondary"),
                        dbc.Button("Side",      id="map3d-cam-side",  size="sm", outline=True, color="secondary"),
                        dbc.Button("Front",     id="map3d-cam-front", size="sm", outline=True, color="secondary"),
                    ]),
                ], md=4, className="d-flex flex-column"),
            ], className="mb-3 align-items-end"),
        ]),

        # ── 3D graph ──
        dcc.Graph(
            id="map3d-graph",
            figure=_no_data_3d("Click ▶ Load 3D Map to render"),
            config={"displayModeBar": True, "scrollZoom": True, "toImageButtonOptions": {"format": "png", "scale": 2}},
            style={"height": "680px"},
        ),

        # ── Stats row (hidden until loaded) ──
        html.Div(id="map3d-stats-row"),
    ])


def _no_data_3d(msg="No data"):
    fig = go.Figure()
    fig.add_annotation(text=msg, xref="paper", yref="paper", x=0.5, y=0.5,
                       showarrow=False, font=dict(color=MUTED, size=14))
    fig.update_layout(
        paper_bgcolor="rgba(0,0,0,0)",
        margin=dict(l=0, r=0, t=30, b=0),
    )
    return fig


def _to_num(series, default=0):
    """Safe numeric conversion that survives JSON round-trip (handles None, str, mixed types)."""
    return pd.to_numeric(series, errors="coerce").fillna(default)


def _build_3d_figure(df, event_type, color_by, dim):
    """Build the Scatter3d figure from a dataframe already filtered by dimension."""
    label = (event_type or "events").replace("_", " ").title()
    dim_tag = f" · {_dim_label(dim)}" if dim and dim != "__all__" else ""

    # Safe numeric conversion after JSON round-trip
    df = df.copy()
    df["map_x"] = _to_num(df["map_x"])
    df["map_z"] = _to_num(df["map_z"])
    df["y"]     = _to_num(df["y"], default=64) if "y" in df.columns else 64

    plot_df = df.dropna(subset=["map_x", "map_z"])
    if plot_df.empty:
        return _no_data_3d("No coordinate data for this selection")

    y_col = plot_df["y"] if "y" in plot_df.columns else pd.Series([64] * len(plot_df), index=plot_df.index)

    # ── Determine colour series ──
    if color_by == "y":
        fig = go.Figure(go.Scatter3d(
            x=plot_df["map_x"].values,
            y=plot_df["map_z"].values,
            z=y_col.values,
            mode="markers",
            marker=dict(
                size=4,
                color=y_col.values,
                colorscale="Viridis",
                opacity=0.75,
                colorbar=dict(title="Y height", thickness=14, len=0.6,
                              bgcolor="rgba(0,0,0,0.4)", tickfont=dict(color=TEXT)),
                showscale=True,
            ),
            text=_hover_text(plot_df),
            hoverinfo="text",
            name="Events",
        ))
    else:
        # For kills: prefer entity_type grouping so player kills are visually distinct
        if "entity_type" in plot_df.columns and color_by == "player":
            group_col = "entity_type"
        else:
            group_col = "player" if color_by != "event_type" or "event_type" not in plot_df.columns else "event_type"
        if group_col not in plot_df.columns:
            # fallback: single trace, no grouping
            fig = go.Figure(go.Scatter3d(
                x=plot_df["map_x"].values,
                y=plot_df["map_z"].values,
                z=y_col.values,
                mode="markers",
                marker=dict(size=4, color=CHART_COLORS[0], opacity=0.75),
                text=_hover_text(plot_df),
                hoverinfo="text",
                name="Events",
            ))
        else:
            groups = sorted(plot_df[group_col].fillna("unknown").astype(str).unique())
            palette = CHART_COLORS * (len(groups) // len(CHART_COLORS) + 1)
            traces = []
            for i, grp in enumerate(groups):
                mask = plot_df[group_col].fillna("unknown").astype(str) == grp
                sub = plot_df[mask]
                sub_y = sub["y"].values if "y" in sub.columns else [64] * len(sub)
                traces.append(go.Scatter3d(
                    x=sub["map_x"].values,
                    y=sub["map_z"].values,
                    z=sub_y,
                    mode="markers",
                    marker=dict(size=4, color=palette[i], opacity=0.75),
                    text=_hover_text(sub),
                    hoverinfo="text",
                    name=grp,
                ))
            fig = go.Figure(traces)

    # ── Scene layout (only properties valid for scene axes in Plotly 3D) ──
    axis_style = dict(
        showbackground=True,
        backgroundcolor="#1a1d27",
        gridcolor=BORDER,
        tickfont=dict(color=MUTED, size=10),
    )
    fig.update_layout(
        paper_bgcolor="rgba(0,0,0,0)",
        font=dict(color=TEXT),
        margin=dict(l=0, r=0, t=44, b=0),
        title=dict(text=f"3D · {label}{dim_tag} — {len(plot_df):,} points",
                   font=dict(size=15, color=TEXT)),
        legend=dict(bgcolor="rgba(20,22,35,0.85)", bordercolor=BORDER, borderwidth=1,
                    font=dict(color=TEXT, size=11), itemsizing="constant"),
        scene=dict(
            bgcolor="#1a1d27",
            xaxis=dict(title=dict(text="X  (East →)", font=dict(color=MUTED)), **axis_style),
            yaxis=dict(title=dict(text="Z  (South ↓)", font=dict(color=MUTED)),
                       autorange="reversed", **axis_style),
            zaxis=dict(title=dict(text="Y  (Height)", font=dict(color=MUTED)), **axis_style),
            camera=_3D_CAMERA_PRESETS["iso"],
            aspectmode="manual",
            aspectratio=dict(x=1.6, y=1.6, z=0.6),
        ),
        uirevision="map3d",
    )
    return fig


def _hover_text(df):
    """Build hover strings safely — columns may have mixed types after JSON round-trip."""
    cols = set(df.columns)
    idx = df.index

    def _fmt_num(series):
        return pd.to_numeric(series, errors="coerce").fillna(0).round(0).astype(int).astype(str)

    lines = pd.Series([""] * len(df), index=idx, dtype=str)
    if "player" in cols:
        lines = "<b>" + df["player"].fillna("?").astype(str) + "</b>"
    if "map_x" in cols and "map_z" in cols:
        ys = _fmt_num(df["y"]) if "y" in cols else "?"
        lines = (lines
                 + "<br>X=" + _fmt_num(df["map_x"])
                 + "  Y=" + ys
                 + "  Z=" + _fmt_num(df["map_z"]))
    if "dimension" in cols:
        lines = lines + "<br>" + df["dimension"].fillna("").astype(str)
    if "entity_type" in cols:
        et = df["entity_type"].fillna("").astype(str)
        is_player_kill = et.str.lower().str.contains("player")
        label = et.where(~is_player_kill, "⚔️ " + et + " (PLAYER KILL)")
        lines = lines + "<br>Killed: " + label
    elif "details" in cols:
        lines = lines + "<br>" + df["details"].fillna("").astype(str).str[:80]
    if "timestamp" in cols:
        lines = lines + "<br>🕐 " + df["timestamp"].fillna("").astype(str)
    return lines.tolist()


# ─── Profiles ────────────────────────────────────────────────────────────────

_PROFILE_DIMS = ["Aggression", "Builder", "Miner", "Economy", "Survival", "Social", "Playtime"]

_ARCHETYPE_MAP = {
    "Aggression": "⚔️ Predator",
    "Builder":    "🏗️ Builder",
    "Miner":      "⛏️ Miner",
    "Economy":    "💰 Merchant",
    "Survival":   "🛡️ Survivor",
    "Social":     "💬 Socialite",
    "Playtime":   "🕹️ Grinder",
}


def _compute_scores(profile_df, market_df):
    """Return profile_df with sc_* columns (0–100) and archetype label per player."""
    df = profile_df.copy()

    # Market merge
    if not market_df.empty and "player" in market_df.columns:
        mkt = market_df[["player", "total_sales", "total_buys", "currency_traded"]].copy()
        df = df.merge(mkt, on="player", how="left")
    for col in ["total_sales", "total_buys", "currency_traded"]:
        if col not in df.columns:
            df[col] = 0.0
    df = df.fillna(0)

    # Raw dimension computations
    df["_aggression_raw"] = df.get("kills", 0)  * 3 + df.get("attacks", 0)
    df["_builder_raw"]    = df.get("blocks_placed", 0)
    df["_miner_raw"]      = df.get("blocks_broken", 0)
    df["_economy_raw"]    = df.get("currency_traded", 0) + df.get("total_sales", 0) + df.get("total_buys", 0)
    df["_survival_raw"]   = df.get("sessions", 1) / (df.get("deaths", 0) + 1)
    df["_social_raw"]     = df.get("chat_messages", 0)
    df["_playtime_raw"]   = df.get("session_hours", 0)

    raw_cols = {
        "Aggression": "_aggression_raw",
        "Builder":    "_builder_raw",
        "Miner":      "_miner_raw",
        "Economy":    "_economy_raw",
        "Survival":   "_survival_raw",
        "Social":     "_social_raw",
        "Playtime":   "_playtime_raw",
    }

    # Normalize 0–100 against server maximum
    for dim, col in raw_cols.items():
        max_val = df[col].max()
        if max_val > 0:
            df[f"sc_{dim}"] = (df[col] / max_val * 100).round(1)
        else:
            df[f"sc_{dim}"] = 0.0

    sc_cols = [f"sc_{d}" for d in _PROFILE_DIMS]
    df["archetype"] = df[sc_cols].idxmax(axis=1).str.replace("sc_", "", regex=False).map(_ARCHETYPE_MAP)

    return df


def render_profiles(players, start, end, servers=None):
    # Player list from the proven source (same as global filter dropdown)
    player_names = db.get_player_names()
    opts = [{"label": p, "value": p} for p in player_names]

    # Load profile stats for archetype table
    try:
        profile_df = db.get_profile_all_players(start, end, servers)
        market_df  = db.get_player_market_profile(start, end)
    except Exception as exc:
        profile_df = pd.DataFrame()
        market_df  = pd.DataFrame()

    if profile_df.empty:
        arch_content = dbc.Alert(
            "No behavioral stats found for this period. "
            "Check that player_logins / player_deaths tables have data.",
            color="info", dismissable=True,
        )
    else:
        scored = _compute_scores(profile_df, market_df)
        arch_cols = ["player", "archetype"] + [f"sc_{d}" for d in _PROFILE_DIMS]
        arch_cols = [c for c in arch_cols if c in scored.columns]
        arch_df = scored[arch_cols].sort_values(f"sc_{_PROFILE_DIMS[0]}", ascending=False)
        arch_df = arch_df.rename(columns={f"sc_{d}": d for d in _PROFILE_DIMS})
        arch_content = make_table(arch_df, "prof-arch-tbl", page_size=15, height="420px")

    return html.Div([
        section("👤 Behavioral Profiles",
                "Radar charts · Archetype detection · Player comparison"),
        dbc.Card(dbc.CardBody(
            dbc.Row([
                dbc.Col([
                    html.Label("Primary player", className="text-muted small mb-1"),
                    dcc.Dropdown(
                        id="prof-player-a", options=opts, placeholder="Select player…",
                        className="dash-dropdown-dark",
                    ),
                ], md=4),
                dbc.Col([
                    html.Label("Compare with (optional)", className="text-muted small mb-1"),
                    dcc.Dropdown(
                        id="prof-player-b", options=opts, placeholder="None — single view",
                        className="dash-dropdown-dark",
                    ),
                ], md=4),
                dbc.Col([
                    html.Label("\u00a0", className="d-block small mb-1"),
                    dbc.Button("▶ Build Profile", id="prof-build-btn",
                               color="primary", className="w-100"),
                ], md=2, className="d-flex align-items-end"),
            ], align="end", className="g-2"),
        ), style={"backgroundColor": CARD_BG2, "border": f"1px solid {BORDER}"},
           className="mb-3"),

        dbc.Row([
            dbc.Col([
                section("🏷️ Server Archetypes"),
                arch_content,
            ], md=5),
            dbc.Col([
                section("📡 Radar Chart"),
                html.Div(
                    id="prof-radar-output",
                    children=html.Div("Select a player and click ▶ Build Profile.",
                                      className="text-muted fst-italic p-3"),
                ),
            ], md=7),
        ]),
    ])


@app.callback(
    Output("prof-radar-output", "children"),
    Input("prof-build-btn", "n_clicks"),
    State("prof-player-a", "value"),
    State("prof-player-b", "value"),
    State("filter-servers", "value"),
    State("filter-dates", "start_date"),
    State("filter-dates", "end_date"),
    prevent_initial_call=True,
)
def build_player_profile(_n, player_a, player_b, servers, start, end):
    if not player_a:
        return dbc.Alert("Select at least one player.", color="warning", dismissable=True)

    profile_df = db.get_profile_all_players(start, end, servers)
    market_df  = db.get_player_market_profile(start, end)

    if profile_df.empty:
        return dbc.Alert("No profile data available.", color="info", dismissable=True)

    scored = _compute_scores(profile_df, market_df)

    players_sel = [p for p in [player_a, player_b] if p]
    sel = scored[scored["player"].isin(players_sel)]
    if sel.empty:
        return dbc.Alert("Selected player(s) not found in data.", color="warning", dismissable=True)

    fig = go.Figure()
    colors = ["#5bc0eb", "#f7931e"]
    for i, (_, row) in enumerate(sel.iterrows()):
        scores = [row.get(f"sc_{d}", 0) for d in _PROFILE_DIMS]
        arch   = row.get("archetype", "—")
        fig.add_trace(go.Scatterpolar(
            r=scores + [scores[0]],
            theta=_PROFILE_DIMS + [_PROFILE_DIMS[0]],
            fill="toself",
            name=f"{row['player']}  {arch}",
            line_color=colors[i % len(colors)],
            opacity=0.85,
        ))

    fig.update_layout(
        polar=dict(
            radialaxis=dict(range=[0, 100], showticklabels=True,
                            tickfont_color="#aaa", gridcolor="#444"),
            angularaxis=dict(tickfont_color="#ddd", gridcolor="#444"),
            bgcolor=CARD_BG,
        ),
        paper_bgcolor=CARD_BG,
        plot_bgcolor=CARD_BG,
        font_color="#ddd",
        legend=dict(orientation="h", yanchor="bottom", y=-0.25,
                    font_color="#ddd", bgcolor="rgba(0,0,0,0)"),
        margin=dict(l=60, r=60, t=40, b=80),
    )

    # Stats card per player
    stat_cards = []
    for _, row in sel.iterrows():
        p = row["player"]
        kv = [
            ("Archetype",    row.get("archetype", "—")),
            ("Kills",        int(row.get("kills", 0))),
            ("Deaths",       int(row.get("deaths", 0))),
            ("K/D",          f"{row.get('kills', 0) / max(row.get('deaths', 1), 1):.2f}"),
            ("Blocks placed",f"{int(row.get('blocks_placed', 0)):,}"),
            ("Blocks broken",f"{int(row.get('blocks_broken', 0)):,}"),
            ("Sessions",     int(row.get("sessions", 0))),
            ("Playtime (h)", f"{row.get('session_hours', 0):.1f}"),
            ("Chat msgs",    int(row.get("chat_messages", 0))),
            ("Trades",       int(row.get("total_sales", 0) + row.get("total_buys", 0))),
        ]
        rows = [html.Tr([html.Td(k, className="text-muted small pe-3"),
                         html.Td(html.Strong(str(v)))]) for k, v in kv]
        stat_cards.append(dbc.Col(
            dbc.Card(dbc.CardBody([
                html.H6(p, className="text-info mb-2"),
                html.Table(rows, className="table table-sm table-dark mb-0"),
            ]), style={"backgroundColor": CARD_BG2, "border": f"1px solid {BORDER}"}),
            md=6,
        ))

    return html.Div([
        dcc.Graph(figure=fig, config={"displayModeBar": False}),
        dbc.Row(stat_cards, className="mt-3"),
    ])


# ─── Relations ───────────────────────────────────────────────────────────────

def _build_relation_graph(kill_df, market_df, session_df, min_weight):
    try:
        import networkx as nx
    except ImportError:
        return no_data_fig("networkx not installed — run: pip install networkx")

    # Build weighted edge dicts — always undirected (sorted pair as key)
    kill_edges    = {}
    market_edges  = {}
    session_edges = {}

    for _, r in (kill_df.iterrows() if not kill_df.empty else iter([])):
        pair = tuple(sorted([str(r["killer"]), str(r["victim"])]))
        kill_edges[pair] = kill_edges.get(pair, 0) + int(r["kills"])

    for _, r in (market_df.iterrows() if not market_df.empty else iter([])):
        pair = tuple(sorted([str(r["player_a"]), str(r["player_b"])]))
        market_edges[pair] = market_edges.get(pair, 0) + int(r["trade_count"])

    for _, r in (session_df.iterrows() if not session_df.empty else iter([])):
        pair = tuple(sorted([str(r["player_a"]), str(r["player_b"])]))
        session_edges[pair] = session_edges.get(pair, 0) + int(r["sessions_together"])

    # Apply minimum weight filter
    mw = int(min_weight or 1)
    kill_edges    = {k: v for k, v in kill_edges.items()    if v >= mw}
    market_edges  = {k: v for k, v in market_edges.items()  if v >= mw}
    session_edges = {k: v for k, v in session_edges.items() if v >= mw}

    all_pairs = set(list(kill_edges) + list(market_edges) + list(session_edges))
    if not all_pairs:
        return no_data_fig("No relationships found — try lowering Min interactions or selecting All time")

    # Build networkx graph for layout
    G = nx.Graph()
    for pair in all_pairs:
        G.add_nodes_from(pair)
        w = kill_edges.get(pair, 0) * 1.5 + market_edges.get(pair, 0) + session_edges.get(pair, 0) * 0.5
        if G.has_edge(*pair):
            G[pair[0]][pair[1]]["weight"] += w
        else:
            G.add_edge(pair[0], pair[1], weight=max(w, 0.1))

    k_val = max(1.0, 2.0 / max(len(G.nodes) ** 0.5, 1))
    pos   = nx.spring_layout(G, weight="weight", seed=42, k=k_val, iterations=80)

    traces = []

    def _edge_trace(edges_dict, color, name, dash):
        ex, ey = [], []
        for (a, b) in edges_dict:
            if a in pos and b in pos:
                ex += [pos[a][0], pos[b][0], None]
                ey += [pos[a][1], pos[b][1], None]
        if ex:
            traces.append(go.Scatter(
                x=ex, y=ey, mode="lines", name=name,
                line=dict(width=1.8, color=color, dash=dash),
                hoverinfo="skip", opacity=0.65,
            ))

    _edge_trace(session_edges, "#4a9eff", "Online together", "dot")
    _edge_trace(market_edges,  "#2ecc71", "Market trades",   "solid")
    _edge_trace(kill_edges,    "#e74c3c", "PvP kills",       "solid")

    # Node attributes
    nx_list    = list(G.nodes)
    node_x     = [pos[n][0] for n in nx_list]
    node_y     = [pos[n][1] for n in nx_list]
    node_size  = [12 + G.degree(n) * 5 for n in nx_list]
    node_color = []
    node_hover = []

    for n in nx_list:
        nb = list(G.neighbors(n))
        k_w = sum(kill_edges.get(tuple(sorted([n, x])), 0)    for x in nb)
        m_w = sum(market_edges.get(tuple(sorted([n, x])), 0)  for x in nb)
        s_w = sum(session_edges.get(tuple(sorted([n, x])), 0) for x in nb)
        node_color.append(
            "#e74c3c" if k_w > m_w and k_w > s_w else
            "#2ecc71" if m_w >= s_w else
            "#4a9eff"
        )
        tip = [f"<b>{n}</b>  (connections: {G.degree(n)})"]
        if k_w: tip.append(f"PvP kill weight: {k_w}")
        if m_w: tip.append(f"Market trades: {m_w}")
        if s_w: tip.append(f"Sessions together: {s_w}")
        node_hover.append("<br>".join(tip))

    traces.append(go.Scatter(
        x=node_x, y=node_y, mode="markers+text",
        text=nx_list, textposition="top center",
        textfont=dict(size=10, color=TEXT),
        name="Players",
        marker=dict(size=node_size, color=node_color,
                    line=dict(width=1.5, color=BORDER)),
        hovertext=node_hover, hoverinfo="text",
    ))

    n_n = len(G.nodes)
    n_e = len(kill_edges) + len(market_edges) + len(session_edges)
    xs  = [p[0] for p in pos.values()]
    ys  = [p[1] for p in pos.values()]
    fig = go.Figure(traces)
    fig.update_layout(
        title=f"Player Relationship Graph — {n_n} nodes · {n_e} relationship pairs",
        height=650, template=CHART_TEMPLATE, showlegend=True,
        xaxis=dict(showgrid=False, zeroline=False, showticklabels=False,
                   range=[min(xs) - 0.25, max(xs) + 0.25]),
        yaxis=dict(showgrid=False, zeroline=False, showticklabels=False,
                   range=[min(ys) - 0.35, max(ys) + 0.35]),
        hovermode="closest",
        legend=dict(x=0.01, y=0.99, bgcolor="rgba(0,0,0,0.35)"),
        margin=dict(l=10, r=10, t=50, b=10),
    )
    return chart_layout(fig)


def render_relations(players, start, end, servers=None):
    return html.Div([
        section("🕸️ Player Relationship Graph",
                "PvP rivalries · market partnerships · co-play sessions"),
        html.Small(
            "🔴 Red node = fighter dominant  ·  🟢 Green = trader dominant  ·  "
            "🔵 Blue = social dominant  ·  Node size ∝ connections",
            className="text-muted fst-italic d-block mb-3",
        ),
        dbc.Card(dbc.CardBody(
            dbc.Row([
                dbc.Col([
                    html.Label("Edge types", className="text-muted small mb-1"),
                    dbc.Checklist(
                        id="rel-edge-types",
                        options=[
                            {"label": "  🔴 PvP Kills",        "value": "kills"},
                            {"label": "  🟢 Market Trades",     "value": "market"},
                            {"label": "  🔵 Online Together",   "value": "sessions"},
                        ],
                        value=["kills", "market", "sessions"],
                        inline=True, className="text-light small",
                    ),
                ], md=5),
                dbc.Col([
                    html.Label("Min interactions per pair", className="text-muted small mb-1"),
                    dcc.Slider(
                        id="rel-min-weight", min=1, max=15, step=1, value=1,
                        marks={1: "1", 3: "3", 5: "5", 10: "10", 15: "15"},
                        tooltip={"placement": "bottom", "always_visible": False},
                    ),
                ], md=4),
                dbc.Col([
                    html.Label("\u00a0", className="d-block small mb-1"),
                    dbc.Button("▶ Build Graph", id="rel-build-btn",
                               color="primary", className="w-100"),
                ], md=2, className="d-flex align-items-end"),
            ], align="end", className="g-2"),
        ), style={"backgroundColor": CARD_BG2, "border": f"1px solid {BORDER}"},
           className="mb-3"),
        html.Div(
            id="rel-graph-output",
            children=html.Div("Select options and click ▶ Build Graph.",
                              className="text-muted fst-italic p-3"),
        ),
    ])


@app.callback(
    Output("rel-graph-output", "children"),
    Input("rel-build-btn", "n_clicks"),
    State("rel-edge-types", "value"),
    State("rel-min-weight", "value"),
    State("filter-servers", "value"),
    State("filter-dates", "start_date"),
    State("filter-dates", "end_date"),
    prevent_initial_call=True,
)
def build_relation_graph(_n, edge_types, min_weight, servers, start, end):
    if not edge_types:
        return dbc.Alert("Select at least one edge type.", color="warning", dismissable=True)

    show_kills    = "kills"    in edge_types
    show_market   = "market"   in edge_types
    show_sessions = "sessions" in edge_types

    kill_df    = db.get_kill_pairs(start, end, servers)       if show_kills    else pd.DataFrame()
    market_df  = db.get_market_trade_pairs(start, end)        if show_market   else pd.DataFrame()
    session_df = db.get_session_overlaps(start, end, servers) if show_sessions else pd.DataFrame()

    if kill_df.empty and market_df.empty and session_df.empty:
        return dbc.Alert(
            "No relationship data for this period. Try selecting All time.",
            color="info", dismissable=True,
        )

    fig = _build_relation_graph(kill_df, market_df, session_df, min_weight or 1)

    # Summary tables
    cols = []
    if show_kills and not kill_df.empty:
        cols.append(dbc.Col([
            section("⚔️ Top PvP Pairs"),
            make_table(kill_df.head(15), "rel-pvp", page_size=10, height="320px"),
        ], md=4))
    if show_market and not market_df.empty:
        mkt = market_df.sort_values("trade_count", ascending=False).head(15)
        cols.append(dbc.Col([
            section("💰 Top Trade Pairs"),
            make_table(mkt, "rel-mkt", page_size=10, height="320px"),
        ], md=4))
    if show_sessions and not session_df.empty:
        cols.append(dbc.Col([
            section("🎮 Top Co-play Pairs"),
            make_table(session_df.head(15), "rel-ses", page_size=10, height="320px"),
        ], md=4))

    return html.Div([
        dcc.Graph(figure=fig, config={"displayModeBar": True, "scrollZoom": True}),
        html.Br(),
        dbc.Row(cols) if cols else html.Div(),
    ])


# ─── Economy ─────────────────────────────────────────────────────────────────

def _gini(values):
    """Compute Gini coefficient for a sorted list of non-negative values."""
    values = sorted(v for v in values if v > 0)
    n = len(values)
    if n < 2 or sum(values) == 0:
        return None
    total = sum(values)
    cumsum = sum(v * (2 * (i + 1) - n - 1) for i, v in enumerate(values))
    return round(cumsum / (n * total), 3)


# ─── Market Correlation View ──────────────────────────────────────────────────

_GAME_METRIC_OPTS = [
    {"label": "Session Hours",   "value": "session_hours"},
    {"label": "Kills",           "value": "kills"},
    {"label": "Deaths",          "value": "deaths"},
    {"label": "K/D Ratio",       "value": "kd_ratio"},
    {"label": "Blocks Broken",   "value": "blocks_broken"},
    {"label": "Blocks Placed",   "value": "blocks_placed"},
    {"label": "Attacks",         "value": "attacks"},
    {"label": "Chat Messages",   "value": "chat_messages"},
]
_MARKET_METRIC_OPTS = [
    {"label": "Currency Traded (total)", "value": "currency_traded"},
    {"label": "Total Sales",             "value": "total_sales"},
    {"label": "Total Buys",              "value": "total_buys"},
]


def _build_corr_scatter(df, x_col, y_col, size_col):
    if df is None or df.empty:
        return no_data_fig("No data — reload the tab to refresh")
    for c in [x_col, y_col]:
        if c not in df.columns:
            return no_data_fig(f"Column '{c}' not found")

    plot_df = df.copy()
    # Convert to numeric safely (handles post-JSON-roundtrip strings)
    for c in [x_col, y_col]:
        plot_df[c] = pd.to_numeric(plot_df[c], errors="coerce").fillna(0)

    if plot_df.empty:
        return no_data_fig("No player data for this period")

    size_col_actual = None
    size_label = "uniform"
    if size_col and size_col != "none" and size_col in plot_df.columns:
        # px.scatter requires strictly positive sizes — add 1 as offset
        plot_df = plot_df.copy()
        plot_df["_size"] = pd.to_numeric(plot_df[size_col], errors="coerce").fillna(0) + 1
        size_col_actual = "_size"
        size_label = size_col

    hover_cols = [c for c in ["kills", "deaths", "session_hours", "total_sales",
                               "total_buys", "currency_traded", "kd_ratio"]
                  if c in plot_df.columns and c not in (x_col, y_col, "_size")]
    labels = {
        "session_hours": "Hours", "kills": "Kills", "deaths": "Deaths",
        "kd_ratio": "K/D", "blocks_broken": "Blocks Broken",
        "blocks_placed": "Blocks Placed", "attacks": "Attacks",
        "chat_messages": "Chat", "currency_traded": "Currency",
        "total_sales": "Sales", "total_buys": "Buys",
        "_size": size_label,
    }
    try:
        fig = px.scatter(
            plot_df, x=x_col, y=y_col,
            size=size_col_actual,
            size_max=40,
            text="player",
            hover_name="player",
            hover_data={c: True for c in hover_cols},
            color="player",
            color_discrete_sequence=CHART_COLORS,
            template=CHART_TEMPLATE,
            labels=labels,
        )
    except Exception as e:
        return no_data_fig(f"Chart error: {e}")

    fig.update_traces(textposition="top center", textfont_size=9,
                      marker=dict(line=dict(width=0.5, color="#222")))
    fig = chart_layout(fig)
    fig.update_layout(
        showlegend=False,
        title=dict(text=f"Game Activity × Market Wealth  (bubble size = {size_label})",
                   font=dict(size=13)),
        margin=dict(l=50, r=20, t=50, b=50),
    )
    return fig


def _build_corr_timeline(player, game_df, mkt_df):
    if game_df.empty and mkt_df.empty:
        return no_data_fig(f"No data for {player} in this period")

    fig = go.Figure()

    if not game_df.empty:
        if "kills" in game_df.columns:
            fig.add_trace(go.Bar(
                x=game_df["day"], y=game_df["kills"].fillna(0),
                name="Kills", marker_color="#e05c5c", opacity=0.75, yaxis="y1",
            ))
        if "deaths" in game_df.columns:
            fig.add_trace(go.Bar(
                x=game_df["day"], y=game_df["deaths"].fillna(0),
                name="Deaths", marker_color="#888", opacity=0.55, yaxis="y1",
            ))
        if "blocks_broken" in game_df.columns:
            fig.add_trace(go.Scatter(
                x=game_df["day"], y=game_df["blocks_broken"].fillna(0),
                name="Blocks Mined", mode="lines",
                line=dict(color="#5cb85c", width=1.5, dash="dot"), yaxis="y1",
            ))

    if not mkt_df.empty:
        fig.add_trace(go.Scatter(
            x=mkt_df["day"], y=mkt_df["sell_vol"].fillna(0),
            name="Market Sales (vol)", mode="lines+markers",
            line=dict(color="#f5c518", width=2.5), marker=dict(size=5), yaxis="y2",
        ))
        fig.add_trace(go.Scatter(
            x=mkt_df["day"], y=mkt_df["buy_vol"].fillna(0),
            name="Market Buys (vol)", mode="lines+markers",
            line=dict(color="#69b3e7", width=1.5, dash="dash"), marker=dict(size=4), yaxis="y2",
        ))

    axis_style = dict(
        showgrid=False,
        tickfont=dict(color=MUTED, size=10),
        title=dict(font=dict(color=MUTED)),
    )
    fig.update_layout(
        template=CHART_TEMPLATE,
        paper_bgcolor="rgba(0,0,0,0)",
        plot_bgcolor="rgba(0,0,0,0)",
        font=dict(color=TEXT),
        margin=dict(l=55, r=65, t=44, b=50),
        barmode="stack",
        title=dict(text=f"{player}  —  game events (left axis)  ×  market volume (right axis)",
                   font=dict(size=13, color=TEXT)),
        xaxis=dict(showgrid=True, gridcolor=BORDER, tickfont=dict(color=MUTED, size=9)),
        yaxis=dict(title=dict(text="Events", font=dict(color=MUTED)),
                   tickfont=dict(color=MUTED, size=10), showgrid=False),
        yaxis2=dict(title=dict(text="Market Volume", font=dict(color="#f5c518")),
                    tickfont=dict(color="#f5c518", size=10),
                    overlaying="y", side="right", showgrid=False),
        legend=dict(bgcolor="rgba(0,0,0,0.3)", font=dict(color=TEXT, size=10)),
    )
    return fig


def render_market_corr_view(players, start, end, servers):
    combo_df = db.get_game_vs_market(start, end, servers)
    player_opts = [{"label": n, "value": n} for n in (db.get_player_names() or [])]
    scatter_fig = _build_corr_scatter(combo_df, "session_hours", "currency_traded", "kills")

    return html.Div([
        dcc.Store(id="corr-combo-store",
                  data=combo_df.to_dict("records") if not combo_df.empty else None),

        # ── Section 1: Scatter ────────────────────────────────────────────────
        section("🔍 Game Activity × Market Wealth",
                "Each dot = one player. Change axes to explore correlations. "
                "Only players with at least 1 market transaction appear on Y."),
        dbc.Row([
            dbc.Col([
                html.Label("X axis (game stat)", className="text-muted small mb-1"),
                dcc.Dropdown(id="corr-x", options=_GAME_METRIC_OPTS,
                             value="session_hours", clearable=False,
                             style=_dropdown_style(), className="dbc"),
            ], md=3),
            dbc.Col([
                html.Label("Y axis (market stat)", className="text-muted small mb-1"),
                dcc.Dropdown(id="corr-y", options=_MARKET_METRIC_OPTS,
                             value="currency_traded", clearable=False,
                             style=_dropdown_style(), className="dbc"),
            ], md=3),
            dbc.Col([
                html.Label("Bubble size", className="text-muted small mb-1"),
                dcc.Dropdown(
                    id="corr-size",
                    options=[{"label": "None (equal)", "value": "none"}] + _GAME_METRIC_OPTS,
                    value="kills", clearable=False,
                    style=_dropdown_style(), className="dbc",
                ),
            ], md=3),
        ], className="mb-3 align-items-end"),
        dcc.Graph(id="corr-scatter", figure=scatter_fig,
                  config={"displayModeBar": False}, style={"height": "440px"},
                  className="mb-2"),

        html.Hr(style={"borderColor": BORDER, "marginTop": "8px"}),

        # ── Section 2: Player timeline ────────────────────────────────────────
        section("📅 Player Activity Timeline",
                "Daily game events (bars, left axis) vs daily market volume (lines, right axis)."),
        dbc.Row([
            dbc.Col([
                html.Label("Player", className="text-muted small mb-1"),
                dcc.Dropdown(id="corr-player", options=player_opts,
                             placeholder="Select a player…",
                             clearable=True, style=_dropdown_style(), className="dbc"),
            ], md=4),
            dbc.Col([
                html.Label("\u00a0", className="d-block small mb-1"),
                dbc.Button("▶ Load Timeline", id="corr-timeline-btn", color="primary"),
            ], md=2),
        ], className="mb-3 align-items-end"),
        dcc.Graph(id="corr-timeline",
                  figure=no_data_fig("Select a player and click ▶ Load Timeline"),
                  config={"displayModeBar": False}, style={"height": "380px"},
                  className="mb-2"),

        html.Hr(style={"borderColor": BORDER, "marginTop": "8px"}),

        # ── Section 3: PvP → Market pipeline ─────────────────────────────────
        section("⚔️ → 💰 PvP × Market Wealth",
                "Top PvP killers ranked alongside their market activity — "
                "reveals who turns combat loot into market profit."),
        dbc.Row([
            dbc.Col([
                html.Label("\u00a0", className="d-block small mb-1"),
                dbc.Button("▶ Load PvP Pipeline", id="corr-pvp-btn", color="secondary"),
            ], md=3),
        ], className="mb-3"),
        html.Div(id="corr-pvp-output"),
    ])


# ─── Moderation View ──────────────────────────────────────────────────────────

def render_moderation_view(players, start, end, servers):
    return html.Div([
        section("🛡️ Moderation Dashboard",
                "Automatic rule-based flag detection + post-death loot analysis."),

        # ── Controls ──────────────────────────────────────────────────────────
        dbc.Row([
            dbc.Col([
                html.Label("Loot window (min after death)", className="text-muted small mb-1"),
                dcc.Dropdown(id="mod-loot-window", options=[
                    {"label": "1 min",  "value": 1},
                    {"label": "2 min",  "value": 2},
                    {"label": "5 min",  "value": 5},
                    {"label": "10 min", "value": 10},
                    {"label": "30 min", "value": 30},
                ], value=5, clearable=False, style=_dropdown_style(), className="dbc"),
            ], md=2),
            dbc.Col([
                html.Label("Loot radius (blocks)", className="text-muted small mb-1"),
                dcc.Dropdown(id="mod-loot-radius", options=[
                    {"label": "10 blocks",  "value": 10},
                    {"label": "20 blocks",  "value": 20},
                    {"label": "50 blocks",  "value": 50},
                    {"label": "100 blocks", "value": 100},
                ], value=20, clearable=False, style=_dropdown_style(), className="dbc"),
            ], md=2),
            dbc.Col([
                html.Label("\u00a0", className="d-block small mb-1"),
                dbc.Button("▶ Run All Checks", id="mod-run-btn", color="danger"),
            ], md=2),
        ], className="mb-4 align-items-end"),

        html.Div(id="mod-output"),
    ])


# ─── P8 Market Intel ─────────────────────────────────────────────────────────

def render_market_intel_view(players, start, end, servers):
    vol_df      = db.get_price_volatility(start, end, limit=15)
    monopoly_df = db.get_item_supply_monopoly(start, end, limit=20)
    _gini_df    = db.get_economy_gini()
    gini_val    = _gini(_gini_df["wealth"].tolist()) if not _gini_df.empty else None
    _item_opts_df = db.get_market_item_options()
    item_opts = (
        [{"label": r["label"], "value": r["item_id"]} for _, r in _item_opts_df.iterrows()]
        if _item_opts_df is not None and not _item_opts_df.empty else []
    )

    def _small_table(df, tid):
        if df is None or df.empty:
            return dbc.Alert("No data for this period.", color="info", className="py-2")
        return dash_table.DataTable(
            id=tid, data=df.to_dict("records"),
            columns=[{"name": c.replace("_", " ").title(), "id": c} for c in df.columns],
            page_size=15, sort_action="native",
            style_table={"overflowX": "auto"},
            style_header={"backgroundColor": "#1a1d27", "color": TEXT,
                          "fontWeight": "bold", "border": f"1px solid {BORDER}"},
            style_cell={"backgroundColor": "#181b28", "color": TEXT,
                        "border": f"1px solid {BORDER}", "fontSize": "12px",
                        "padding": "5px 8px", "fontFamily": "monospace"},
            style_data_conditional=[
                {"if": {"row_index": "odd"}, "backgroundColor": "#1e2130"}],
        )

    gini_card = dbc.Card(dbc.CardBody([
        html.H6("Gini Coefficient", className="text-muted small mb-1"),
        html.H3(f"{gini_val:.3f}" if gini_val is not None else "—",
                className="text-warning mb-0"),
        html.Small("0 = perfect equality · 1 = total monopoly", className="text-muted"),
    ]), className="mb-3", style={"backgroundColor": "#1a1d27", "border": f"1px solid {BORDER}"})

    return html.Div([
        section("🕵️ Market Intelligence",
                "Price volatility, supply monopolies and market Gini index."),

        dbc.Row([
            # Left: Gini + monopoly
            dbc.Col([
                gini_card,
                html.H6("Supply Monopoly", className="text-muted small mb-2"),
                html.Small("Top seller's share of total sales per item. "
                           "≥80% = near-monopoly.", className="text-muted fst-italic d-block mb-2"),
                _small_table(monopoly_df, "mi-monopoly-tbl"),
            ], md=5),
            # Right: volatility
            dbc.Col([
                html.H6("Price Volatility", className="text-muted small mb-2"),
                html.Small("Items with highest price swing (range ÷ avg).",
                           className="text-muted fst-italic d-block mb-2"),
                _small_table(vol_df, "mi-vol-tbl"),
            ], md=7),
        ], className="mb-4"),

        html.Hr(style={"borderColor": BORDER}),
        section("📈 Item Price History", "Select an item to view its full price timeline."),
        dbc.Row([
            dbc.Col([
                html.Label("Item", className="text-muted small mb-1"),
                dcc.Dropdown(id="mi-item-select", options=item_opts,
                             placeholder="Select an item…",
                             clearable=True, style=_dropdown_style(), className="dbc"),
            ], md=5),
        ], className="mb-3"),
        dcc.Graph(id="mi-price-chart",
                  figure=no_data_fig("Select an item above"),
                  config={"displayModeBar": False}, style={"height": "340px"}),
    ])


# ─── P9 Area Query ────────────────────────────────────────────────────────────

def render_area_query_view(players, start, end, servers):
    return html.Div([
        section("📍 Area Query — What happened here?",
                "Enter world coordinates and a radius to see every recorded event "
                "in that location during the selected time period."),
        dbc.Row([
            dbc.Col([
                html.Label("X coordinate", className="text-muted small mb-1"),
                dbc.Input(id="aq-x", type="number", placeholder="e.g. 87",
                          style={"backgroundColor": "#181b28", "color": TEXT,
                                 "border": f"1px solid {BORDER}"}),
            ], md=2),
            dbc.Col([
                html.Label("Z coordinate", className="text-muted small mb-1"),
                dbc.Input(id="aq-z", type="number", placeholder="e.g. 124",
                          style={"backgroundColor": "#181b28", "color": TEXT,
                                 "border": f"1px solid {BORDER}"}),
            ], md=2),
            dbc.Col([
                html.Label("Radius (blocks)", className="text-muted small mb-1"),
                dcc.Dropdown(id="aq-radius", options=[
                    {"label": "10 blocks",  "value": 10},
                    {"label": "25 blocks",  "value": 25},
                    {"label": "50 blocks",  "value": 50},
                    {"label": "100 blocks", "value": 100},
                    {"label": "200 blocks", "value": 200},
                    {"label": "500 blocks", "value": 500},
                ], value=50, clearable=False, style=_dropdown_style(), className="dbc"),
            ], md=2),
            dbc.Col([
                html.Label("Max events", className="text-muted small mb-1"),
                dcc.Dropdown(id="aq-limit", options=[
                    {"label": "500",   "value": 500},
                    {"label": "1 000", "value": 1000},
                    {"label": "2 000", "value": 2000},
                    {"label": "5 000", "value": 5000},
                ], value=1000, clearable=False, style=_dropdown_style(), className="dbc"),
            ], md=2),
            dbc.Col([
                html.Label("\u00a0", className="d-block small mb-1"),
                dbc.Button("▶ Query Area", id="aq-load-btn", color="primary"),
            ], md=2),
        ], className="mb-4 align-items-end"),
        html.Div(id="aq-output"),
    ])


# ─── P10 Rankings ─────────────────────────────────────────────────────────────

def _rank_card(title, df, col_player, col_value, value_label, color="#e0e4f0"):
    if df is None or df.empty:
        return dbc.Card(dbc.CardBody([
            html.H6(title, className="text-muted small mb-2"),
            html.Small("No data", className="text-muted"),
        ]), style={"backgroundColor": "#1a1d27", "border": f"1px solid {BORDER}",
                   "height": "100%"})
    rows = []
    for i, (_, r) in enumerate(df.head(10).iterrows(), 1):
        medal = {1: "🥇", 2: "🥈", 3: "🥉"}.get(i, f"{i}.")
        val = r.get(col_value, 0)
        val_str = f"{int(val):,}" if isinstance(val, (int, float)) and val == int(val) else f"{val:,.1f}" if isinstance(val, float) else str(val)
        rows.append(dbc.ListGroupItem(
            [html.Span(f"{medal} ", style={"minWidth": "28px", "display": "inline-block"}),
             html.Span(str(r.get(col_player, "?")), className="fw-bold me-auto"),
             html.Span(f"{val_str} {value_label}",
                       style={"color": color, "fontSize": "12px", "marginLeft": "8px"})],
            style={"backgroundColor": "#181b28", "border": f"1px solid {BORDER}",
                   "padding": "5px 10px", "display": "flex", "alignItems": "center"},
        ))
    return dbc.Card([
        dbc.CardHeader(html.H6(title, className="mb-0 small text-muted"),
                       style={"backgroundColor": "#1a1d27", "border": f"1px solid {BORDER}"}),
        dbc.ListGroup(rows, flush=True),
    ], style={"backgroundColor": "#1a1d27", "border": f"1px solid {BORDER}", "height": "100%"})


def render_rankings_view(players, start, end, servers):
    kills_df   = db.get_kills_per_player(None, start, end, servers)
    active_df  = db.get_top_active_players(start, end, limit=10, servers=servers)
    miner_df   = db.get_block_break_by_player(None, start, end, servers)
    market_df  = db.get_player_market_profile(start, end)

    try:
        dead_df = db.get_deadliest_players(None, start, end, servers)
    except Exception:
        dead_df = pd.DataFrame()

    try:
        builder_df = db.get_block_place_by_player(None, start, end, servers)
    except Exception:
        builder_df = pd.DataFrame()

    # kills col name (player_kill_events uses player_name)
    k_player = "player_name" if kills_df is not None and not kills_df.empty and "player_name" in kills_df.columns else "player"
    k_kills  = "kills"

    return html.Div([
        section("🏅 Rankings", "Top 10 per category for the selected period."),
        dbc.Row([
            dbc.Col(_rank_card("⚔️ Top Killers",    kills_df,   k_player,      k_kills,         "kills",   "#e05c5c"), md=4, className="mb-3"),
            dbc.Col(_rank_card("💀 Most Deaths",     dead_df,    "player",      "deaths",        "deaths",  "#aaa"),    md=4, className="mb-3"),
            dbc.Col(_rank_card("🕐 Most Online",     active_df,  "player_name", "events",        "events",  "#5cb85c"), md=4, className="mb-3"),
        ]),
        dbc.Row([
            dbc.Col(_rank_card("⛏️ Top Miners",      miner_df,   "player_name", "blocks_broken", "blocks",  "#f5c518"), md=4, className="mb-3"),
            dbc.Col(_rank_card("🧱 Top Builders",    builder_df, "player_name", "blocks_placed", "blocks",  "#69b3e7"), md=4, className="mb-3"),
            dbc.Col(_rank_card("💰 Top Traders",     market_df,  "player",      "currency_traded","coins",  "#c084fc"), md=4, className="mb-3"),
        ]),
    ])


# ─── P11 Chronicle ────────────────────────────────────────────────────────────

def render_chronicle_view(players, start, end, servers):
    return html.Div([
        section("📰 Server Chronicle",
                "Auto-generated narrative summary of the selected period."),
        dbc.Row([
            dbc.Col([
                html.Label("\u00a0", className="d-block small mb-1"),
                dbc.Button("📰 Generate Chronicle", id="chr-run-btn", color="primary"),
            ], md=3),
        ], className="mb-4"),
        html.Div(id="chr-output"),
    ])


# ─── P12 Position Trails ──────────────────────────────────────────────────────

def render_positions_view(players, start, end, servers):
    player_opts = [{"label": n, "value": n} for n in (db.get_player_names() or [])]
    return html.Div([
        section("🧭 Player Position Trail",
                "3D scatter of all recorded event coordinates for a player — "
                "approximates movement from kills, attacks, deaths, mining and drops."),
        dbc.Row([
            dbc.Col([
                html.Label("Player", className="text-muted small mb-1"),
                dcc.Dropdown(id="pos-player", options=player_opts,
                             placeholder="Select a player…",
                             clearable=True, style=_dropdown_style(), className="dbc"),
            ], md=4),
            dbc.Col([
                html.Label("Max points", className="text-muted small mb-1"),
                dcc.Dropdown(id="pos-limit", options=[
                    {"label": "200",   "value": 200},
                    {"label": "500",   "value": 500},
                    {"label": "1 000", "value": 1000},
                    {"label": "2 000", "value": 2000},
                ], value=500, clearable=False, style=_dropdown_style(), className="dbc"),
            ], md=2),
            dbc.Col([
                html.Label("\u00a0", className="d-block small mb-1"),
                dbc.Button("▶ Load Trail", id="pos-load-btn", color="primary"),
            ], md=2),
        ], className="mb-3 align-items-end"),
        dcc.Graph(id="pos-graph",
                  figure=_no_data_3d("Select a player and click ▶ Load Trail"),
                  config={"displayModeBar": True},
                  style={"height": "620px"}),
        html.Div(id="pos-table-output"),
    ])


def render_economy(players, start, end):
    kpis        = db.get_market_kpis(start, end)
    use_all     = kpis.get("transactions", 0) == 0 and kpis.get("transactions_all", 0) > 0
    balance_df  = db.get_sell_buy_balance_all(start, end) if use_all else db.get_sell_buy_balance(start, end)
    items_df    = db.get_top_traded_items_all(start, end) if use_all else db.get_top_traded_items(start, end)
    vol_df      = db.get_price_volatility(start, end)
    tax_df      = db.get_tax_over_time(start, end)
    acc_df      = db.get_suspicious_accumulators(start, end)
    tx_df       = db.get_recent_transactions(start, end, players)

    # ── KPI row ──────────────────────────────────────────────────────────────
    tx_real  = kpis.get("transactions", 0)
    tx_all   = kpis.get("transactions_all", 0)
    tx_label = "Transactions" if tx_real == tx_all else f"Transactions (real / {tx_all} tot)"
    row_kpi = dbc.Row([
        dbc.Col(kpi(tx_label,         tx_real,                        "info",     "🔄"), md=2, sm=4, xs=6, className="mb-3"),
        dbc.Col(kpi("Active Traders", kpis.get("active_traders", 0),  "primary",  "👥"), md=2, sm=4, xs=6, className="mb-3"),
        dbc.Col(kpi("Items on Market",kpis.get("active_items", 0),    "secondary","📦"), md=2, sm=4, xs=6, className="mb-3"),
        dbc.Col(kpi("Tax (period)",   kpis.get("tax_collected", 0),   "warning",  "🪙"), md=2, sm=4, xs=6, className="mb-3"),
        dbc.Col(kpi("Total Tax Ever", kpis.get("total_tax_ever", 0),  "success",  "🏛️"), md=2, sm=4, xs=6, className="mb-3"),
    ], className="mb-2")

    # ── Alert if all transactions are fictional ───────────────────────────────
    fictional_alert = html.Div()
    if kpis.get("transactions_all", 0) > 0 and kpis.get("transactions", 0) == 0:
        fictional_alert = dbc.Alert(
            "⚠️  All transactions in this period are marked as fictional (admin/command-generated). "
            "The charts below show fictional transactions as fallback.",
            color="warning", dismissable=True, className="mb-3",
        )

    # ── Gini coefficient ─────────────────────────────────────────────────────
    gini_section = html.Div()
    gini_df = db.get_economy_gini()
    if not gini_df.empty:
        g = _gini(gini_df["wealth"].tolist())
        if g is not None:
            color = "success" if g < 0.4 else ("warning" if g < 0.65 else "danger")
            gini_section = dbc.Row([
                dbc.Col(dbc.Alert([
                    html.Strong("Gini Coefficient: "),
                    dbc.Badge(str(g), color=color, className="me-2 fs-6"),
                    html.Span("0 = perfectly equal wealth · 1 = one player owns everything",
                              className="text-muted small ms-1"),
                ], color="dark", className="py-2 mb-3"), md=9),
            ])

    # ── Sales vs Purchases per player ────────────────────────────────────────
    fig_balance = no_data_fig("No trade data for this period")
    if not balance_df.empty:
        top = balance_df.head(15)
        fig_balance = go.Figure([
            go.Bar(name="Sales",     x=top["player"], y=top["sales"],
                   marker_color="#2ecc71"),
            go.Bar(name="Purchases", x=top["player"], y=top["purchases"],
                   marker_color="#e74c3c"),
        ])
        fig_balance.update_layout(barmode="group", title="Sales vs Purchases per Player",
                                   height=300, template=CHART_TEMPLATE)
        fig_balance = chart_layout(fig_balance)

    # ── Top traded items ─────────────────────────────────────────────────────
    fig_items = no_data_fig("No item trade data")
    if not items_df.empty:
        fig_items = px.bar(
            items_df, x="tx_count", y="item", orientation="h",
            color="tx_count", color_continuous_scale="Viridis",
            labels={"tx_count": "Transactions", "item": ""},
            hover_data=["total_units", "current_price"],
            template=CHART_TEMPLATE,
        )
        fig_items = chart_layout(fig_items)
        fig_items.update_layout(title="Most Traded Items", height=300,
                                 showlegend=False, coloraxis_showscale=False)

    # ── Tax revenue timeline ──────────────────────────────────────────────────
    fig_tax = no_data_fig("No tax data for this period")
    if not tax_df.empty:
        fig_tax = go.Figure([
            go.Bar(x=tax_df["day"], y=tax_df["tax_total"],
                   name="Tax collected", marker_color="#f39c12", yaxis="y"),
            go.Scatter(x=tax_df["day"], y=tax_df["tax_events"],
                       name="Transactions", line=dict(color="#7eb8f7", width=2),
                       yaxis="y2"),
        ])
        fig_tax.update_layout(
            title="Tax Revenue & Transaction Volume Over Time",
            yaxis=dict(title="Tax Amount"),
            yaxis2=dict(title="Transactions", overlaying="y", side="right",
                        showgrid=False),
            height=280, template=CHART_TEMPLATE, hovermode="x unified",
        )
        fig_tax = chart_layout(fig_tax)

    return html.Div([
        row_kpi,
        fictional_alert,
        gini_section,
        dbc.Row([
            dbc.Col(dcc.Graph(figure=fig_balance, config={"displayModeBar": False}), md=7),
            dbc.Col(dcc.Graph(figure=fig_items,   config={"displayModeBar": False}), md=5),
        ], className="mb-3"),
        dbc.Row([
            dbc.Col(dcc.Graph(figure=fig_tax, config={"displayModeBar": False}), md=12),
        ], className="mb-3"),
        dbc.Row([
            dbc.Col([
                section("⚠️ Suspicious Accumulators",
                        "players buying in ≥70% of their trades"),
                make_table(acc_df, "eco-acc", page_size=10, height="320px")
                if not acc_df.empty
                else html.Div("No suspicious patterns detected.",
                              className="text-muted fst-italic"),
            ], md=5),
            dbc.Col([
                section("📊 Price Volatility",
                        "most volatile items by price range %"),
                make_table(vol_df, "eco-vol", page_size=10, height="320px")
                if not vol_df.empty
                else html.Div("No price history data.",
                              className="text-muted fst-italic"),
            ], md=7),
        ], className="mb-3"),
        html.Br(),
        section("📋 Recent Transactions",
                f"last 200 · {'filtered by selected players' if players else 'all players'}"),
        make_table(tx_df, "eco-tx", page_size=20),
    ])


# ─── Kill Matrix ──────────────────────────────────────────────────────────────

def render_kill_matrix_view(players, start, end, servers):
    km_df = db.get_kill_matrix(start, end, servers)
    if km_df is None or km_df.empty:
        return html.Div([
            section("⚔️ Kill Matrix", "Who kills whom — PvP interaction heatmap."),
            dbc.Alert("No PvP deaths recorded in this period.", color="info"),
        ])

    pivot = km_df.pivot_table(index="killer", columns="victim", values="kills", fill_value=0)
    z = pivot.values.tolist()
    fig = go.Figure(go.Heatmap(
        z=z,
        x=list(pivot.columns),
        y=list(pivot.index),
        colorscale="Reds",
        colorbar=dict(title="Kills", tickfont=dict(color=TEXT), thickness=14),
        text=z,
        texttemplate="%{text}",
        hovertemplate="Killer: <b>%{y}</b><br>Victim: <b>%{x}</b><br>Kills: %{z}<extra></extra>",
    ))
    fig.update_layout(
        paper_bgcolor="#1e2130", plot_bgcolor="#1e2130",
        font=dict(color=TEXT, size=11),
        xaxis=dict(title="Victim", tickfont=dict(color=TEXT, size=10),
                   gridcolor=BORDER, side="bottom"),
        yaxis=dict(title="Killer", tickfont=dict(color=TEXT, size=10),
                   gridcolor=BORDER, autorange="reversed"),
        margin=dict(l=100, r=20, t=40, b=100),
        height=max(400, 30 * len(pivot) + 120),
    )
    return html.Div([
        section("⚔️ Kill Matrix",
                f"Kill counts between {len(pivot)} killers and {len(pivot.columns)} victims. "
                "Rows = killer, columns = victim."),
        dcc.Graph(figure=fig, config={"displayModeBar": True}),
    ])


# ─── Activity Clock ───────────────────────────────────────────────────────────

def render_activity_view(players, start, end, servers):
    act_df = db.get_hourly_activity(start, end, servers)
    if act_df is None or act_df.empty:
        return html.Div([
            section("⏱️ Activity Clock", "Events by hour × day of week."),
            dbc.Alert("No activity data in this period.", color="info"),
        ])

    DOW = ["Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"]
    grid = [[0] * 24 for _ in range(7)]
    for _, row in act_df.iterrows():
        d = int(row["dow"]) - 1   # DAYOFWEEK: 1=Sun → index 0
        h = int(row["hour"])
        grid[d][h] = int(row["events"])

    fig = go.Figure(go.Heatmap(
        z=grid,
        x=list(range(24)),
        y=DOW,
        colorscale="Viridis",
        colorbar=dict(title="Events", tickfont=dict(color=TEXT), thickness=14),
        hovertemplate="Day: <b>%{y}</b><br>Hour: %{x}:00<br>Events: <b>%{z}</b><extra></extra>",
    ))
    fig.update_layout(
        paper_bgcolor="#1e2130", plot_bgcolor="#1e2130",
        font=dict(color=TEXT),
        xaxis=dict(
            title="Hour of Day",
            tickvals=list(range(24)),
            ticktext=[f"{h:02d}:00" for h in range(24)],
            tickfont=dict(color=TEXT, size=9),
            gridcolor=BORDER,
        ),
        yaxis=dict(title="Day of Week", tickfont=dict(color=TEXT), gridcolor=BORDER),
        margin=dict(l=60, r=20, t=40, b=70),
        height=320,
    )
    total = sum(sum(row) for row in grid)
    peak_d, peak_h = max(
        ((d, h) for d in range(7) for h in range(24)),
        key=lambda dh: grid[dh[0]][dh[1]]
    )
    return html.Div([
        section("⏱️ Activity Clock",
                f"Total {total:,} events. Peak: {DOW[peak_d]} at {peak_h:02d}:00 "
                f"({grid[peak_d][peak_h]:,} events)."),
        dcc.Graph(figure=fig, config={"displayModeBar": False}),
    ])


# ─── Trade Routes ─────────────────────────────────────────────────────────────

def render_trade_routes_view(players, start, end, servers):
    return html.Div([
        section("🚚 Trade Routes",
                "Point-to-point estimated trade flow. Barter = physical drop→pickup. "
                "Market = formal transaction, endpoints are player home claims."),
        dbc.Row([
            dbc.Col([
                html.Label("Barter time window (sec)", className="text-muted small mb-1"),
                dcc.Input(id="tr-time-window", type="number", value=120,
                          min=30, max=600, step=30,
                          style={"width": "100%", "backgroundColor": CARD_BG2,
                                 "color": TEXT, "border": f"1px solid {BORDER}"}),
            ], md=2),
            dbc.Col([
                html.Label("Max barter distance (blocks)", className="text-muted small mb-1"),
                dcc.Input(id="tr-max-dist", type="number", value=50,
                          min=10, max=300, step=10,
                          style={"width": "100%", "backgroundColor": CARD_BG2,
                                 "color": TEXT, "border": f"1px solid {BORDER}"}),
            ], md=2),
            dbc.Col([
                html.Label("\u00a0", className="d-block small mb-1"),
                dbc.Button("🚚 Load Routes", id="tr-load-btn", color="primary"),
            ], md=2),
        ], className="mb-3 align-items-end"),
        html.Div(id="tr-stats", className="mb-2"),
        dcc.Graph(id="tr-map", style={"height": "680px"},
                  config={"displayModeBar": True, "scrollZoom": True}),
    ])


@app.callback(
    Output("tr-map",   "figure"),
    Output("tr-stats", "children"),
    Input("tr-load-btn", "n_clicks"),
    State("tr-time-window",    "value"),
    State("tr-max-dist",       "value"),
    State("filter-players",    "value"),
    State("filter-dates",      "start_date"),
    State("filter-dates",      "end_date"),
    State("filter-servers",    "value"),
    prevent_initial_call=True,
)
def load_trade_routes(n_clicks, time_window, max_dist, players, start, end, servers):
    if not n_clicks:
        raise PreventUpdate

    tw  = int(time_window or 120)
    md  = float(max_dist  or 50)
    df  = db.get_trade_routes(start, end, time_window_sec=tw, max_distance=md, servers=servers)

    empty_fig = go.Figure()
    empty_fig.update_layout(
        paper_bgcolor="#1e2130", plot_bgcolor="#1a1d27",
        font=dict(color=TEXT),
        xaxis=dict(gridcolor=BORDER, zeroline=False),
        yaxis=dict(gridcolor=BORDER, zeroline=False),
    )

    if df is None or df.empty:
        empty_fig.add_annotation(
            text="No routes found for this period / parameters.",
            xref="paper", yref="paper", x=0.5, y=0.5,
            font=dict(color=MUTED, size=14), showarrow=False,
        )
        return empty_fig, dbc.Alert("No trade routes found.", color="info", className="py-2")

    barter = df[df["route_type"] == "barter"]
    market = df[df["route_type"] == "market"]

    BARTER_COL = "#69b3e7"
    MARKET_COL = "#f5c518"

    traces = []

    # Lines using None-separator trick
    for sub, color, name in [(barter, BARTER_COL, "Barter"), (market, MARKET_COL, "Market")]:
        if sub.empty:
            continue
        lx, lz = [], []
        for _, r in sub.iterrows():
            lx += [float(r["from_x"]), float(r["to_x"]), None]
            lz += [float(r["from_z"]), float(r["to_z"]), None]
        traces.append(go.Scattergl(
            x=lz, y=lx, mode="lines", name=name,
            line=dict(color=color, width=1.5),
            hoverinfo="skip", opacity=0.55,
        ))

    # Destination markers (triangle = arrow tip)
    for sub, color, name in [(barter, BARTER_COL, "Barter dest"), (market, MARKET_COL, "Market dest")]:
        if sub.empty:
            continue
        def _hover(r):
            time_str = r.get("first_trade") or "?"
            if r.get("last_trade") and r.get("last_trade") != time_str:
                time_str = f"{r['first_trade']} … {r['last_trade']}"
            return (
                f"<b>{r['from_player']}</b> → <b>{r['to_player']}</b><br>"
                f"Item: {r['item_type']}  ×{int(r['count'])}<br>"
                f"From: X {r['from_x']:.0f} / Z {r['from_z']:.0f}<br>"
                f"To: X {r['to_x']:.0f} / Z {r['to_z']:.0f}<br>"
                f"Time: {time_str}"
            )
        hover = sub.apply(_hover, axis=1)
        traces.append(go.Scattergl(
            x=sub["to_z"].tolist(), y=sub["to_x"].tolist(),
            mode="markers", name=name, showlegend=False,
            marker=dict(color=color, size=7, symbol="triangle-up"),
            text=hover.tolist(),
            hovertemplate="%{text}<extra></extra>",
        ))

    # Origin markers (circle)
    for sub, color in [(barter, BARTER_COL), (market, MARKET_COL)]:
        if sub.empty:
            continue
        traces.append(go.Scattergl(
            x=sub["from_z"].tolist(), y=sub["from_x"].tolist(),
            mode="markers", showlegend=False,
            marker=dict(color=color, size=5, symbol="circle", opacity=0.4),
            hoverinfo="skip",
        ))

    fig = go.Figure(traces)
    fig.update_layout(
        paper_bgcolor="#1e2130", plot_bgcolor="#1a1d27",
        font=dict(color=TEXT),
        xaxis=dict(title="Z", gridcolor=BORDER, zeroline=False, tickfont=dict(color=TEXT)),
        yaxis=dict(title="X", gridcolor=BORDER, zeroline=False, tickfont=dict(color=TEXT),
                   scaleanchor="x", scaleratio=1),
        legend=dict(bgcolor="#1a1d27", bordercolor=BORDER, font=dict(color=TEXT)),
        margin=dict(l=60, r=20, t=20, b=60),
        height=680,
    )

    stats = dbc.Row([
        dbc.Col(dbc.Badge(f"🔵 Barter routes: {len(barter)}", color="primary", className="me-2 p-2")),
        dbc.Col(dbc.Badge(f"🟡 Market routes: {len(market)}", color="warning",  className="me-2 p-2")),
        dbc.Col(dbc.Badge(f"Total: {len(df)}", color="secondary", className="p-2")),
    ], className="g-2 mb-1")

    return fig, stats


# ─── Main tab callback ────────────────────────────────────────────────────────

@app.callback(
    Output("tab-content", "children"),
    Input("main-tabs", "active_tab"),
    Input("auto-refresh", "n_intervals"),
    Input("btn-refresh", "n_clicks"),
    Input("filter-servers", "value"),
    Input("filter-players", "value"),
    Input("filter-dates", "start_date"),
    Input("filter-dates", "end_date"),
    prevent_initial_call=False,
)
def render_tab(tab, _n, _clicks, servers, players, start, end):
    from dash import ctx
    triggered = ctx.triggered_id
    # Don't re-render investigate/map tabs on auto-refresh or filter changes — they use their own callbacks
    if tab in ("investigate", "map", "map3d", "relations", "profiles",
               "market_corr", "moderation", "market_intel", "area_query",
               "positions", "chronicle", "rankings",
               "traderoutes") and triggered not in ("main-tabs", None):
        return dash.no_update
    if tab == "overview":
        return render_overview(players, start, end, servers)
    if tab == "players":
        return render_players(players, start, end, servers)
    if tab == "combat":
        return render_combat(players, start, end, servers)
    if tab == "deaths":
        return render_deaths(players, start, end, servers)
    if tab == "world":
        return render_world(players, start, end, servers)
    if tab == "items":
        return render_items(players, start, end, servers)
    if tab == "chat":
        return render_chat(players, start, end, servers)
    if tab == "advancements":
        return render_advancements(players, start, end, servers)
    if tab == "economy":
        return render_economy(players, start, end)
    if tab == "market_intel":
        return render_market_intel_view(players, start, end, servers)
    if tab == "market_corr":
        return render_market_corr_view(players, start, end, servers)
    if tab == "moderation":
        return render_moderation_view(players, start, end, servers)
    if tab == "relations":
        return render_relations(players, start, end, servers)
    if tab == "profiles":
        return render_profiles(players, start, end, servers)
    if tab == "rankings":
        return render_rankings_view(players, start, end, servers)
    if tab == "investigate":
        return render_investigate(players, start, end)
    if tab == "area_query":
        return render_area_query_view(players, start, end, servers)
    if tab == "map":
        return render_map_view(players, start, end)
    if tab == "map3d":
        return render_map3d_view(players, start, end)
    if tab == "positions":
        return render_positions_view(players, start, end, servers)
    if tab == "chronicle":
        return render_chronicle_view(players, start, end, servers)
    if tab == "reports":
        return render_reports(players, start, end)
    if tab == "settings":
        return render_settings()
    if tab == "killmatrix":
        return render_kill_matrix_view(players, start, end, servers)
    if tab == "activity":
        return render_activity_view(players, start, end, servers)
    if tab == "traderoutes":
        return render_trade_routes_view(players, start, end, servers)
    return html.Div("Unknown tab.")


# ─── Market Correlation callbacks ────────────────────────────────────────────

@app.callback(
    Output("corr-scatter", "figure"),
    Input("corr-x", "value"),
    Input("corr-y", "value"),
    Input("corr-size", "value"),
    State("corr-combo-store", "data"),
    prevent_initial_call=True,
)
def update_corr_scatter(x_col, y_col, size_col, records):
    if not records:
        return no_data_fig("No data — reload the tab")
    return _build_corr_scatter(pd.DataFrame(records), x_col, y_col, size_col)


@app.callback(
    Output("corr-timeline", "figure"),
    Input("corr-timeline-btn", "n_clicks"),
    State("corr-player", "value"),
    State("filter-dates", "start_date"),
    State("filter-dates", "end_date"),
    State("filter-servers", "value"),
    prevent_initial_call=True,
)
def load_corr_timeline(_n, player, start, end, servers):
    if not player:
        return no_data_fig("Select a player first")
    try:
        game_df = db.get_player_events_per_day(player, start, end, servers)
        mkt_df  = db.get_player_daily_market(player, start, end)
    except Exception as e:
        return no_data_fig(f"DB error: {e}")
    return _build_corr_timeline(player, game_df, mkt_df)


@app.callback(
    Output("corr-pvp-output", "children"),
    Input("corr-pvp-btn", "n_clicks"),
    State("filter-dates", "start_date"),
    State("filter-dates", "end_date"),
    State("filter-servers", "value"),
    prevent_initial_call=True,
)
def load_pvp_pipeline(_n, start, end, servers):
    kills_df = db.get_kills_per_player(start, end, servers)
    mkt_df = db.get_player_market_profile(start, end)

    if kills_df.empty:
        return dbc.Alert("No kill data for this period.", color="info")

    kills_df = kills_df.rename(columns={"player_name": "player"})

    if not mkt_df.empty:
        df = kills_df.merge(
            mkt_df[["player", "total_sales", "currency_traded"]],
            on="player", how="left",
        )
    else:
        df = kills_df.copy()
        df["total_sales"] = 0
        df["currency_traded"] = 0

    df["total_sales"] = df["total_sales"].fillna(0).astype(int)
    df["currency_traded"] = df["currency_traded"].fillna(0).round(0).astype(int)
    df = df.sort_values("kills", ascending=False).head(30)

    # Flag potential looters: top-third killers who are also top-third traders
    kill_thresh = df["kills"].quantile(0.66)
    trade_thresh = df["currency_traded"].quantile(0.66)
    df["note"] = df.apply(
        lambda r: "⚔️💰 Possible looter"
        if r["kills"] >= kill_thresh and r["currency_traded"] >= trade_thresh and r["kills"] > 0
        else "",
        axis=1,
    )

    tbl = dash_table.DataTable(
        data=df.to_dict("records"),
        columns=[
            {"name": "Player",            "id": "player"},
            {"name": "Kills",             "id": "kills"},
            {"name": "Market Sales",      "id": "total_sales"},
            {"name": "Currency Traded",   "id": "currency_traded"},
            {"name": "Note",              "id": "note"},
        ],
        page_size=20,
        style_table={"overflowX": "auto", "maxHeight": "480px", "overflowY": "auto"},
        style_header={"backgroundColor": "#1a1d27", "color": TEXT,
                      "fontWeight": "bold", "border": f"1px solid {BORDER}"},
        style_cell={"backgroundColor": "#181b28", "color": TEXT,
                    "border": f"1px solid {BORDER}", "fontSize": "13px",
                    "padding": "6px 10px", "fontFamily": "monospace"},
        style_data_conditional=[
            {"if": {"filter_query": '{note} contains "looter"'},
             "backgroundColor": "#2d1a0e", "color": "#ffb347"},
            {"if": {"row_index": "odd"},
             "backgroundColor": "#1e2130"},
        ],
        sort_action="native",
    )
    return html.Div([
        html.Small(
            f"⚔️💰 Possible looter = top-third by kills AND top-third by currency traded "
            f"(kill threshold: {int(kill_thresh)}, trade threshold: {int(trade_thresh):,})",
            className="text-muted fst-italic mb-2 d-block",
        ),
        tbl,
    ])


# ─── Moderation callbacks ─────────────────────────────────────────────────────

@app.callback(
    Output("mod-output", "children"),
    Input("mod-run-btn", "n_clicks"),
    State("mod-loot-window", "value"),
    State("mod-loot-radius", "value"),
    State("filter-servers", "value"),
    State("filter-players", "value"),
    State("filter-dates", "start_date"),
    State("filter-dates", "end_date"),
    prevent_initial_call=True,
)
def run_mod_checks(_n, loot_window, loot_radius, servers, players, start, end):
    # ── Section 1: Auto-flags ──────────────────────────────────────────────
    flags_df = db.get_mod_flags(start, end, servers)

    if flags_df.empty:
        flags_section = dbc.Alert("✅ No flags detected for this period.", color="success")
    else:
        sev_order  = {"🔴 High": 0, "🟡 Medium": 1, "🔵 Info": 2}
        flags_df["_ord"] = flags_df["severity"].map(lambda s: sev_order.get(s, 9))
        flags_df = flags_df.sort_values(["_ord", "player"]).drop(columns=["_ord"])

        flags_table = dash_table.DataTable(
            data=flags_df.to_dict("records"),
            columns=[
                {"name": "Sev.",    "id": "severity"},
                {"name": "Player",  "id": "player"},
                {"name": "Flag",    "id": "flag"},
                {"name": "Detail",  "id": "detail"},
            ],
            page_size=30,
            style_table={"overflowX": "auto"},
            style_header={"backgroundColor": "#1a1d27", "color": "#e0e4f0",
                          "fontWeight": "bold", "border": "1px solid #3a3d52"},
            style_cell={"backgroundColor": "#181b28", "color": "#e0e4f0",
                        "border": "1px solid #3a3d52", "fontSize": "13px",
                        "padding": "6px 10px", "fontFamily": "monospace",
                        "whiteSpace": "normal", "textAlign": "left"},
            style_data_conditional=[
                {"if": {"filter_query": '{severity} contains "High"'},
                 "backgroundColor": "#2d0f0f", "color": "#ff8080"},
                {"if": {"filter_query": '{severity} contains "Medium"'},
                 "backgroundColor": "#2a2000", "color": "#ffd966"},
                {"if": {"filter_query": '{severity} contains "Info"'},
                 "backgroundColor": "#0d1a2a", "color": "#80c8ff"},
            ],
            sort_action="native",
        )
        high_n   = (flags_df["severity"].str.contains("High",   na=False)).sum()
        medium_n = (flags_df["severity"].str.contains("Medium", na=False)).sum()
        info_n   = (flags_df["severity"].str.contains("Info",   na=False)).sum()
        flags_section = html.Div([
            html.Small(
                [html.Span(f"🔴 {high_n} high  ", style={"color": "#ff8080"}),
                 html.Span(f"🟡 {medium_n} medium  ", style={"color": "#ffd966"}),
                 html.Span(f"🔵 {info_n} info", style={"color": "#80c8ff"})],
                className="mb-2 d-block",
            ),
            flags_table,
        ])

    # ── Section 2: Chi beneficia? ──────────────────────────────────────────
    loot_df = db.get_death_loot(start, end, servers,
                                time_window_min=int(loot_window or 5),
                                radius=int(loot_radius or 20))

    if loot_df.empty:
        loot_section = dbc.Alert(
            f"No item pickups found within {loot_window} min / {loot_radius} blocks of any death.",
            color="info",
        )
    else:
        # Aggregate: per (victim, looter) → total items looted
        agg = (loot_df.groupby(["victim", "looter", "killer"])
               .agg(items_looted=("item_count", "sum"),
                    pickups=("item_count", "count"),
                    avg_sec=("seconds_after", "mean"))
               .reset_index()
               .sort_values("items_looted", ascending=False))
        agg["avg_sec"] = agg["avg_sec"].round(0).astype(int)
        agg["is_killer_looter"] = agg.apply(
            lambda r: "⚔️ Yes" if str(r["looter"]) == str(r["killer"]) else "", axis=1
        )

        loot_table = dash_table.DataTable(
            data=agg.to_dict("records"),
            columns=[
                {"name": "Victim",        "id": "victim"},
                {"name": "Looter",        "id": "looter"},
                {"name": "Killer",        "id": "killer"},
                {"name": "Items Looted",  "id": "items_looted"},
                {"name": "Pickups",       "id": "pickups"},
                {"name": "Avg sec after", "id": "avg_sec"},
                {"name": "Killer=Looter", "id": "is_killer_looter"},
            ],
            page_size=25,
            style_table={"overflowX": "auto", "maxHeight": "480px", "overflowY": "auto"},
            style_header={"backgroundColor": "#1a1d27", "color": "#e0e4f0",
                          "fontWeight": "bold", "border": "1px solid #3a3d52"},
            style_cell={"backgroundColor": "#181b28", "color": "#e0e4f0",
                        "border": "1px solid #3a3d52", "fontSize": "13px",
                        "padding": "6px 10px", "fontFamily": "monospace"},
            style_data_conditional=[
                {"if": {"filter_query": '{is_killer_looter} = "⚔️ Yes"'},
                 "backgroundColor": "#2d1a0e", "color": "#ffb347"},
                {"if": {"row_index": "odd", "filter_query": '{is_killer_looter} = ""'},
                 "backgroundColor": "#1e2130"},
            ],
            sort_action="native",
        )
        loot_note = html.Small(
            f"Orange rows = killer looted their own victim within {loot_window} min. "
            f"Showing {len(loot_df):,} raw pickups → {len(agg):,} (victim, looter) pairs.",
            className="text-muted fst-italic mb-2 d-block",
        )
        loot_section = html.Div([loot_note, loot_table])

    return html.Div([
        html.H6("🚩 Automatic Flags", className="text-warning mb-2 mt-1"),
        flags_section,
        html.Hr(style={"borderColor": "#3a3d52", "marginTop": "16px"}),
        html.H6("💰 Chi beneficia? — Loot dopo le morti", className="text-info mb-2 mt-3"),
        loot_section,
    ])


# ─── P8 Market Intel callbacks ───────────────────────────────────────────────

@app.callback(
    Output("mi-price-chart", "figure"),
    Input("mi-item-select", "value"),
    State("filter-dates", "start_date"),
    State("filter-dates", "end_date"),
    prevent_initial_call=True,
)
def load_mi_price_chart(item_id, start, end):
    if not item_id:
        return no_data_fig("Select an item above")
    df = db.get_price_history_for_item(item_id, start, end)
    if df.empty:
        return no_data_fig(f"No price history for '{item_id}' in this period")
    fig = px.line(df, x="ts", y="price", template=CHART_TEMPLATE,
                  labels={"ts": "Date", "price": "Price"},
                  title=f"Price history · {item_id.split(':')[-1]}")
    if "volume" in df.columns:
        fig.add_bar(x=df["ts"], y=df["volume"], name="Volume",
                    marker_color="rgba(100,160,255,0.3)", yaxis="y2")
        fig.update_layout(
            yaxis2=dict(title=dict(text="Volume", font=dict(color=MUTED)),
                        overlaying="y", side="right",
                        tickfont=dict(color=MUTED, size=9), showgrid=False),
        )
    fig = chart_layout(fig)
    fig.update_layout(margin=dict(l=50, r=60, t=44, b=44))
    return fig


# ─── P9 Area Query callbacks ──────────────────────────────────────────────────

@app.callback(
    Output("aq-output", "children"),
    Input("aq-load-btn", "n_clicks"),
    State("aq-x", "value"),
    State("aq-z", "value"),
    State("aq-radius", "value"),
    State("aq-limit", "value"),
    State("filter-servers", "value"),
    State("filter-dates", "start_date"),
    State("filter-dates", "end_date"),
    prevent_initial_call=True,
)
def load_area_query(_n, cx, cz, radius, limit, servers, start, end):
    if cx is None or cz is None:
        return dbc.Alert("Enter both X and Z coordinates.", color="warning")
    df = db.get_events_near(cx, cz, radius or 50, start, end,
                            limit=int(limit or 1000), servers=servers)
    if df.empty:
        return dbc.Alert(f"No events found within {radius} blocks of ({cx}, {cz}).", color="info")

    # Type breakdown bar
    type_counts = df["event_type"].value_counts().reset_index()
    type_counts.columns = ["type", "count"]
    bar = px.bar(type_counts, x="type", y="count", template=CHART_TEMPLATE,
                 color="type", color_discrete_sequence=CHART_COLORS,
                 labels={"type": "Event Type", "count": "Count"},
                 title=f"Event breakdown · {len(df):,} events within {radius}b of ({int(cx)}, {int(cz)})")
    bar = chart_layout(bar)
    bar.update_layout(showlegend=False, height=280, margin=dict(l=40, r=20, t=44, b=40))

    tbl = _events_table(df, "aq-tbl", page_size=25, height="460px")

    return html.Div([
        dcc.Graph(figure=bar, config={"displayModeBar": False}, className="mb-3"),
        tbl,
    ])


# ─── P11 Chronicle callbacks ──────────────────────────────────────────────────

@app.callback(
    Output("chr-output", "children"),
    Input("chr-run-btn", "n_clicks"),
    State("filter-servers", "value"),
    State("filter-dates", "start_date"),
    State("filter-dates", "end_date"),
    prevent_initial_call=True,
)
def generate_chronicle(_n, servers, start, end):
    d = db.get_chronicle_data(start, end, servers)
    if not d:
        return dbc.Alert("No data for this period.", color="info")

    period = f"{start or '?'} → {end or '?'}"

    def _stat(icon, label, value, color="#e0e4f0"):
        return dbc.Col(dbc.Card(dbc.CardBody([
            html.Div(f"{icon} {label}", className="text-muted small mb-1"),
            html.H4(value, style={"color": color, "marginBottom": 0}),
        ]), style={"backgroundColor": "#1a1d27", "border": f"1px solid {BORDER}"}), md=3, className="mb-3")

    stats_row = dbc.Row([
        _stat("👥", "Active Players",   f"{d.get('active_players','—'):,}" if isinstance(d.get('active_players'), int) else "—", "#80c8ff"),
        _stat("💀", "Total Deaths",     f"{d.get('deaths','—'):,}"         if isinstance(d.get('deaths'), int) else "—",        "#ff8080"),
        _stat("⚔️", "Kills",            f"{d.get('kills','—'):,}"          if isinstance(d.get('kills'), int) else "—",         "#e05c5c"),
        _stat("⛏️", "Blocks Broken",    f"{d.get('blocks_broken','—'):,}"  if isinstance(d.get('blocks_broken'), int) else "—", "#f5c518"),
        _stat("💬", "Chat Messages",    f"{d.get('chat_messages','—'):,}"  if isinstance(d.get('chat_messages'), int) else "—", "#a8d8a8"),
        _stat("💰", "Market Txs",       f"{d.get('market_transactions','—'):,}" if isinstance(d.get('market_transactions'), int) else "—", "#c084fc"),
        _stat("💸", "Market Volume",    f"{d.get('market_volume',0):,.0f}" if 'market_volume' in d else "—", "#ffd966"),
    ])

    # Narrative text
    lines = [html.H5(f"📰 Chronicle  ·  {period}", className="text-warning mb-3")]

    p = d.get("active_players", 0)
    s = d.get("sessions", 0)
    if p:
        lines.append(html.P(f"During this period, {p} player{'s' if p!=1 else ''} "
                            f"were active across {s:,} session{'s' if s!=1 else ''}."))

    k = d.get("kills", 0)
    dth = d.get("deaths", 0)
    if k or dth:
        lines.append(html.P(f"Combat was {"fierce" if k > 50 else "moderate" if k > 10 else "light"}: "
                            f"{k:,} kills and {dth:,} deaths were recorded."))

    if d.get("top_killers"):
        top = d["top_killers"]
        names = ", ".join(f"{n} ({v} kills)" for n, v in top)
        lines.append(html.P(f"The most dangerous players were: {names}."))

    if d.get("top_pvp_killer"):
        lines.append(html.P(f"In PvP, {d['top_pvp_killer']} dominated with "
                            f"{d['top_pvp_killer_n']} player kills."))

    bb = d.get("blocks_broken", 0)
    if bb:
        lines.append(html.P(f"Miners and builders were hard at work: "
                            f"{bb:,} blocks were broken across the world."))

    mt = d.get("market_transactions", 0)
    mv = d.get("market_volume", 0)
    if mt:
        lines.append(html.P(f"The market saw {mt:,} transactions worth "
                            f"{mv:,.0f} coins in total."))
        if d.get("top_traders"):
            names = ", ".join(f"{n} ({v:,} coins)" for n, v in d["top_traders"])
            lines.append(html.P(f"Top traders: {names}."))

    if d.get("top_death_cause"):
        lines.append(html.P(f"The leading cause of death was "
                            f"'{d['top_death_cause']}' ({d['top_death_cause_n']} times)."))

    narrative = dbc.Card(dbc.CardBody(lines),
                         style={"backgroundColor": "#1a1d27", "border": f"1px solid {BORDER}",
                                "lineHeight": "1.8", "fontSize": "15px", "color": TEXT})

    return html.Div([stats_row, html.Hr(style={"borderColor": BORDER}), narrative])


# ─── P12 Position Trail callbacks ─────────────────────────────────────────────

@app.callback(
    Output("pos-graph", "figure"),
    Output("pos-table-output", "children"),
    Input("pos-load-btn", "n_clicks"),
    State("pos-player", "value"),
    State("pos-limit", "value"),
    State("filter-servers", "value"),
    State("filter-dates", "start_date"),
    State("filter-dates", "end_date"),
    prevent_initial_call=True,
)
def load_position_trail(_n, player, limit, servers, start, end):
    no_fig = _no_data_3d("Select a player and click ▶ Load Trail")
    if not player:
        return no_fig, None
    df = db.get_player_position_trail(player, start, end, servers, limit=int(limit or 500))
    if df.empty:
        return _no_data_3d(f"No position data for {player} in this period"), \
               dbc.Alert("No data found.", color="info")

    for c in ["x", "y", "z"]:
        df[c] = pd.to_numeric(df[c], errors="coerce")
    df = df.dropna(subset=["x", "y", "z"])

    color_map = {"Kill": "#e05c5c", "Attack": "#f5a623", "Death": "#aaaaaa",
                 "Block Break": "#f5c518", "Item Drop": "#69b3e7"}

    fig = go.Figure()
    for etype, grp in df.groupby("event_type"):
        fig.add_trace(go.Scatter3d(
            x=grp["z"].tolist(), y=grp["x"].tolist(), z=grp["y"].tolist(),
            mode="markers",
            name=etype,
            marker=dict(size=3, color=color_map.get(etype, "#cccccc"), opacity=0.75),
            hovertemplate=f"<b>{etype}</b><br>X: %{{y}}<br>Z: %{{x}}<br>Y: %{{z}}<extra></extra>",
        ))

    axis_style = dict(showbackground=True, backgroundcolor="#1a1d27",
                      gridcolor=BORDER, tickfont=dict(color=MUTED, size=9))
    fig.update_layout(
        template="plotly_dark",
        paper_bgcolor="#1e2130",
        font=dict(color=TEXT),
        margin=dict(l=0, r=0, t=44, b=0),
        title=dict(text=f"{player}  ·  {len(df):,} event positions",
                   font=dict(size=14, color=TEXT)),
        scene=dict(
            bgcolor="#1a1d27",
            xaxis=dict(title=dict(text="Z", font=dict(color=MUTED)), **axis_style),
            yaxis=dict(title=dict(text="X", font=dict(color=MUTED)), **axis_style),
            zaxis=dict(title=dict(text="Y (height)", font=dict(color=MUTED)), **axis_style),
            camera=_3D_CAMERA_PRESETS["iso"],
            aspectmode="data",
        ),
        legend=dict(bgcolor="rgba(0,0,0,0.4)", font=dict(color=TEXT, size=10)),
        uirevision="pos-trail",
    )

    tbl = make_table(df.head(200), "pos-tbl", page_size=20)
    return fig, html.Div([
        html.Small(f"Showing first 200 of {len(df):,} events in table.",
                   className="text-muted fst-italic mb-2 d-block"),
        tbl,
    ])


# ─── Server + Player dropdown population ──────────────────────────────────────

@app.callback(
    Output("filter-servers", "options"),
    Input("auto-refresh", "n_intervals"),
    prevent_initial_call=False,
)
def populate_servers(_n):
    ids = db.get_server_ids()
    return [{"label": s, "value": s} for s in ids]


@app.callback(
    Output("filter-players", "options"),
    Input("auto-refresh", "n_intervals"),
    prevent_initial_call=False,
)
def populate_players(_n):
    names = db.get_player_names()
    return [{"label": n, "value": n} for n in names]


# ─── Quick range → date picker ────────────────────────────────────────────────

@app.callback(
    Output("filter-dates", "start_date"),
    Output("filter-dates", "end_date"),
    Input("quick-range", "value"),
    prevent_initial_call=True,
)
def apply_quick_range(val):
    today = date.today()
    if val == "today":
        return today.isoformat(), today.isoformat()
    if val == "7d":
        return (today - timedelta(days=7)).isoformat(), today.isoformat()
    if val == "30d":
        return (today - timedelta(days=30)).isoformat(), today.isoformat()
    if val == "month":
        return today.replace(day=1).isoformat(), today.isoformat()
    if val == "all":
        return None, None
    return dash.no_update, dash.no_update


@app.callback(
    Output("quick-range", "value", allow_duplicate=True),
    Input("filter-dates", "start_date"),
    Input("filter-dates", "end_date"),
    prevent_initial_call=True,
)
def clear_quick_range_on_manual_date(_start, _end):
    """When the date picker changes (manually or after a preset), clear the quick-range label
    so the user sees they are in 'custom range' mode and it won't fire again unexpectedly."""
    return None


# ─── Auto-refresh interval ────────────────────────────────────────────────────

@app.callback(
    Output("auto-refresh", "interval"),
    Output("auto-refresh", "disabled"),
    Input("refresh-select", "value"),
)
def set_refresh(val):
    if not val or val == 0:
        return 30000, True
    return val, False


# ─── Connection badge ─────────────────────────────────────────────────────────

@app.callback(
    Output("connection-badge", "children"),
    Input("auto-refresh", "n_intervals"),
    Input("btn-refresh", "n_clicks"),
    prevent_initial_call=False,
)
def update_badge(_n, _c):
    ok, msg = db.test_connection()
    if ok:
        return dbc.Badge("● DB Connected", color="success", className="p-2")
    return dbc.Badge(f"● DB Error: {msg[:60]}", color="danger", className="p-2")


# ─── Reports callback ─────────────────────────────────────────────────────────

@app.callback(
    Output("report-output", "children"),
    Input("btn-load-report", "n_clicks"),
    State("report-table-select", "value"),
    State("report-limit", "value"),
    State("filter-servers", "value"),
    State("filter-players", "value"),
    State("filter-dates", "start_date"),
    State("filter-dates", "end_date"),
    prevent_initial_call=True,
)
def load_report(_n, table, limit, servers, players, start, end):
    if not table:
        return html.Div("Select a table first.", className="text-muted")
    total = db.get_table_row_count(table)
    df = db.get_table_data(table, players, start, end, limit or 1000, servers=servers)
    header = html.Div([
        html.Span(f"Table: ", className="text-muted me-1"),
        html.Strong(table, className="text-light me-3"),
        html.Span(f"Total rows in DB: ", className="text-muted me-1"),
        html.Strong(f"{total:,}", className="text-info me-3"),
        html.Span(f"Showing: ", className="text-muted me-1"),
        html.Strong(f"{len(df):,}", className="text-light"),
    ], className="mb-2 small")
    return html.Div([header, make_table(df, "report-tbl", page_size=25, height="600px")])


# ─── Investigation callbacks ─────────────────────────────────────────────────

# Timeline
_TL_COLORS = {
    "Death":          "#ff444433", "Kill":           "#ff880033",
    "Attack":         "#ffaa0022", "Chat":           "#44aaff22",
    "Command":        "#aa44ff22", "Block Break":    "#4488ff18",
    "Block Place":    "#44cc4418", "Item Drop":      "#ff669922",
    "Item Pickup":    "#44ffaa18", "Craft":          "#44bbbb18",
    "Consumed":       "#ffcc4418", "Advancement":    "#ffff4418",
    "Fluid Place":    "#4444ff18", "Block Interact": "#88888818",
    "Entity Interact":"#88888818",
}


@app.callback(
    Output("inv-timeline-output", "children"),
    Input("inv-load-timeline", "n_clicks"),
    State("inv-player-select", "value"),
    State("filter-servers", "value"),
    State("filter-dates", "start_date"),
    State("filter-dates", "end_date"),
    prevent_initial_call=True,
)
def load_timeline(_n, player, servers, start, end):
    if not player:
        return dbc.Alert("Select a player first.", color="warning", dismissable=True)
    ok, msg = db.test_connection()
    if not ok:
        return dbc.Alert(f"DB connection failed: {msg}", color="danger", dismissable=True)
    df = db.get_player_timeline(player, start, end, servers=servers)
    if df.empty:
        return dbc.Alert(
            f"No events found for '{player}' in the selected date range. "
            "Check that the date filter isn't too restrictive, or that this player has activity recorded.",
            color="info", dismissable=True,
        )

    # Add pvp indicator column — "⚔️ Name" when death was by a player
    def _pvp_label(row):
        if row.get("type") != "Death":
            return ""
        details = str(row.get("details", ""))
        if "← by" in details:
            try:
                return "⚔️ " + details.split("← by")[1].split("  ")[0].strip()
            except Exception:
                return "⚔️ PvP"
        return ""
    df["pvp"] = df.apply(_pvp_label, axis=1)

    cond = [
        {"if": {"filter_query": f'{{type}} = "{t}"'}, "backgroundColor": c, "color": TEXT}
        for t, c in _TL_COLORS.items()
    ] + [
        {"if": {"filter_query": '{type} = "Death"'}, "fontWeight": "bold"},
        {"if": {"filter_query": '{pvp} != ""'},
         "backgroundColor": "#3d1515", "color": "#ff9999", "fontWeight": "bold"},
    ]

    counts = df["type"].value_counts().reset_index()
    counts.columns = ["type", "count"]
    fig = px.bar(counts.sort_values("count"), x="count", y="type", orientation="h",
                 color="type", template=CHART_TEMPLATE, color_discrete_sequence=CHART_COLORS,
                 labels={"count": "Events", "type": ""})
    fig = chart_layout(fig)
    fig.update_layout(title=f"{player} — event breakdown", height=max(200, len(counts) * 22 + 60),
                      showlegend=False)

    return html.Div([
        dbc.Row([
            dbc.Col([
                html.Span("Player: ", className="text-muted me-1 small"),
                html.Strong(player, className="text-light me-3"),
                html.Span("Events shown: ", className="text-muted me-1 small"),
                html.Strong(f"{len(df):,}", className="text-info"),
            ], className="mb-2"),
        ]),
        dbc.Row([
            dbc.Col(dcc.Graph(figure=fig, config={"displayModeBar": False}), md=5),
            dbc.Col(dash_table.DataTable(
                id="tbl-timeline",
                data=df.to_dict("records"),
                columns=[{"name": "Killed by" if c == "pvp" else c.replace("_", " ").title(), "id": c}
                         for c in df.columns],
                page_size=50, sort_action="native", filter_action="native", export_format="csv",
                style_table={"overflowX": "auto", "maxHeight": "600px", "overflowY": "auto"},
                style_cell={**_CELL, "maxWidth": "500px"},
                style_header=_HEADER, style_filter=_FILTER,
                style_data_conditional=cond,
            ), md=7),
        ]),
    ])


# PvP Incidents
@app.callback(
    Output("pvp-output", "children"),
    Input("pvp-load-btn", "n_clicks"),
    State("pvp-victim-select", "value"),
    State("pvp-killer-select", "value"),
    State("filter-servers", "value"),
    State("filter-dates", "start_date"),
    State("filter-dates", "end_date"),
    prevent_initial_call=True,
)
def load_pvp(_n, victim, killer, servers, start, end):
    df, err = db.get_pvp_incidents(start=start, end=end, victim=victim, killer=killer, servers=servers)
    warnings = [dbc.Alert(err, color="warning", dismissable=True)] if err else []
    if df.empty:
        msg = ("No PvP incidents found. " +
               (f"DB error: {err}" if err else
                "Try selecting 'All time' from the Quick Range filter — data may be outside the current 7-day window."))
        return html.Div([*warnings, dbc.Alert(msg, color="info", dismissable=True)])

    fig = no_data_fig("No coordinates available (schema missing x/y/z)")
    if "x" in df.columns and df["x"].notna().any():
        hover = [c for c in ["victim", "weapon_used", "victim_hp", "victim_armor", "dimension", "timestamp"] if c in df.columns]
        fig = px.scatter(
            df.dropna(subset=["x", "z"]),
            x="x", y="z", color="killer_name",
            hover_data=hover,
            labels={"x": "X (East→)", "z": "Z (South↓)", "killer_name": "Killer"},
            template=CHART_TEMPLATE, color_discrete_sequence=CHART_COLORS,
            symbol="killer_name",
        )
        fig = chart_layout(fig)
        fig.update_layout(title=f"PvP Deaths — {len(df)} events", height=420)
        fig.update_yaxes(autorange="reversed", title="Z (South ↓)")
        fig.update_xaxes(title="X (East →)")

    # Killer leaderboard
    kl = df.groupby("killer_name").size().reset_index(name="kills").sort_values("kills", ascending=False)
    fig_kl = px.bar(kl, x="killer_name", y="kills", color="kills",
                    color_continuous_scale="Reds", template=CHART_TEMPLATE,
                    labels={"killer_name": "", "kills": "Kills"})
    fig_kl = chart_layout(fig_kl)
    fig_kl.update_layout(title="Kills by player", height=280, showlegend=False, coloraxis_showscale=False)

    return html.Div([
        *warnings,
        dbc.Row([
            dbc.Col(dcc.Graph(figure=fig, config={"displayModeBar": True, "scrollZoom": True}), md=8),
            dbc.Col(dcc.Graph(figure=fig_kl, config={"displayModeBar": False}), md=4),
        ], className="mb-3"),
        section(f"PvP Incident Log — {len(df)} events"),
        make_table(df, "pvp-tbl", page_size=25),
    ])


# Fallen / Revive
@app.callback(
    Output("revive-output", "children"),
    Input("revive-load-btn", "n_clicks"),
    State("revive-player-select", "value"),
    State("filter-servers", "value"),
    State("filter-dates", "start_date"),
    State("filter-dates", "end_date"),
    prevent_initial_call=True,
)
def load_revive(_n, player, servers, start, end):
    players = [player] if player else None
    df_fallen = db.get_fallen_events(players=players, start=start, end=end, servers=servers)
    df_revive = db.get_revive_events(players=players, start=start, end=end, servers=servers)
    df_stats  = db.get_revive_stats(players=players, start=start, end=end, servers=servers)

    if df_fallen.empty and df_revive.empty:
        return dbc.Alert(
            "No fallen/revive events found. Try 'All time' from the Quick Range filter.",
            color="info", dismissable=True)

    # Map of fallen positions
    fig_map = no_data_fig("No coordinates")
    if not df_fallen.empty and "x" in df_fallen.columns and df_fallen["x"].notna().any():
        fig_map = px.scatter(
            df_fallen.dropna(subset=["x", "z"]),
            x="x", y="z", color="player_name",
            hover_data=[c for c in ["cause", "killer_name", "dimension", "timestamp"] if c in df_fallen.columns],
            labels={"x": "X (East→)", "z": "Z (South↓)", "player_name": "Player"},
            template=CHART_TEMPLATE, color_discrete_sequence=CHART_COLORS,
        )
        fig_map = chart_layout(fig_map)
        fig_map.update_layout(title=f"Fallen positions — {len(df_fallen)} events", height=380)
        fig_map.update_yaxes(autorange="reversed", title="Z (South ↓)")
        fig_map.update_xaxes(title="X (East →)")

    # Outcome breakdown bar
    fig_outcome = no_data_fig("No revive outcome data")
    if not df_revive.empty:
        oc = df_revive.groupby("outcome").size().reset_index(name="count")
        fig_outcome = px.bar(oc, x="outcome", y="count", color="outcome",
                             color_discrete_map={"revived": "#2ecc71", "died": "#e74c3c", "logout": "#f39c12"},
                             template=CHART_TEMPLATE)
        fig_outcome = chart_layout(fig_outcome)
        fig_outcome.update_layout(title="Revive outcomes", height=260, showlegend=False)

    rows = []
    if not df_stats.empty:
        rows.append(dbc.Row([
            dbc.Col(html.H6("Per-player stats", className="text-muted mt-3 mb-2"), md=12),
            dbc.Col(make_table(df_stats, "revive-stats-tbl", page_size=10), md=12),
        ]))

    return html.Div([
        dbc.Row([
            dbc.Col(dcc.Graph(figure=fig_map, config={"scrollZoom": True}), md=8),
            dbc.Col(dcc.Graph(figure=fig_outcome), md=4),
        ]),
        *rows,
        html.H6("Fallen events", className="text-muted mt-3 mb-1"),
        make_table(df_fallen, "fallen-tbl", page_size=15),
        html.H6("Revive / outcome events", className="text-muted mt-3 mb-1"),
        make_table(df_revive, "revive-tbl", page_size=15),
    ])


# Item Transfers
@app.callback(
    Output("transfer-output", "children"),
    Input("transfer-load-btn", "n_clicks"),
    State("transfer-window", "value"),
    State("transfer-distance", "value"),
    State("filter-servers", "value"),
    State("filter-dates", "start_date"),
    State("filter-dates", "end_date"),
    prevent_initial_call=True,
)
def load_transfers(_n, window, distance, servers, start, end):
    df = db.get_item_transfers(start=start, end=end,
                               time_window_sec=int(window or 300),
                               max_distance=float(distance or 30),
                               servers=servers)
    if df.empty:
        return html.Div("No potential transfers detected. Try loosening time window or distance.",
                        className="text-muted fst-italic")

    # Pair frequency chart
    df["pair"] = df["dropper"] + " → " + df["picker"]
    pair_df = df.groupby("pair").size().reset_index(name="count").sort_values("count", ascending=False).head(15)
    fig = px.bar(pair_df, x="count", y="pair", orientation="h",
                 color="count", color_continuous_scale="YlOrRd", template=CHART_TEMPLATE,
                 labels={"count": "Transfers", "pair": ""})
    fig = chart_layout(fig)
    fig.update_layout(title="Most frequent transfer pairs", height=max(200, len(pair_df) * 24 + 60),
                      showlegend=False, coloraxis_showscale=False)

    return html.Div([
        dbc.Row([
            dbc.Col([
                dbc.Badge(f"{len(df)} potential transfers", color="warning", className="me-2 p-2"),
                html.Small(f"Time window: {window}s | Max distance: {distance} blocks",
                           className="text-muted"),
            ], className="mb-3"),
        ]),
        dbc.Row([
            dbc.Col(dcc.Graph(figure=fig, config={"displayModeBar": False}), md=5),
            dbc.Col(make_table(df.drop(columns=["pair"]), "transfers-tbl", page_size=20), md=7),
        ]),
    ])


# Location Query
@app.callback(
    Output("loc-output", "children"),
    Input("loc-load-btn", "n_clicks"),
    State("loc-x", "value"),
    State("loc-z", "value"),
    State("loc-radius", "value"),
    State("filter-servers", "value"),
    State("filter-dates", "start_date"),
    State("filter-dates", "end_date"),
    prevent_initial_call=True,
)
def load_location(_n, cx, cz, radius, servers, start, end):
    if cx is None or cz is None:
        return dbc.Alert("Enter both X and Z coordinates.", color="warning", dismissable=True)
    r = float(radius or 50)
    df = db.get_events_near(cx, cz, r, start, end, servers=servers)
    if df.empty:
        return html.Div(f"No events within {r:.0f} blocks of ({cx}, {cz}).",
                        className="text-muted fst-italic")

    fig = px.scatter(
        df.dropna(subset=["x", "z"]), x="x", y="z",
        color="event_type", symbol="player",
        hover_data=["player", "details", "dimension", "timestamp"],
        labels={"x": "X", "z": "Z", "event_type": "Type"},
        template=CHART_TEMPLATE, color_discrete_sequence=CHART_COLORS,
    )
    fig = chart_layout(fig)
    fig.update_layout(title=f"Events within {r:.0f} blocks of ({cx}, {cz})", height=420)
    fig.update_yaxes(autorange="reversed")
    fig.add_trace(go.Scatter(
        x=[cx], y=[cz], mode="markers",
        marker=dict(symbol="x", size=14, color="white", line=dict(width=3, color="red")),
        name="Target", showlegend=True,
    ))

    counts = df["event_type"].value_counts().reset_index()
    counts.columns = ["type", "count"]

    return html.Div([
        dbc.Row([
            dbc.Col(dcc.Graph(figure=fig, config={"displayModeBar": True, "scrollZoom": True}), md=8),
            dbc.Col([
                html.Br(),
                dbc.Badge(f"{len(df)} events found", color="info", className="mb-2 p-2 d-block"),
                make_table(counts, "loc-counts", page_size=20, height="360px"),
            ], md=4),
        ], className="mb-3"),
        section(f"All events near ({cx}, {cz}) — {len(df)} total"),
        _events_table(df, "loc-tbl", page_size=25),
    ])


# Player Proximity
@app.callback(
    Output("prox-output", "children"),
    Input("prox-load-btn", "n_clicks"),
    State("prox-player-a", "value"),
    State("prox-player-b", "value"),
    State("prox-distance", "value"),
    State("filter-servers", "value"),
    State("filter-dates", "start_date"),
    State("filter-dates", "end_date"),
    prevent_initial_call=True,
)
def load_proximity(_n, player_a, player_b, distance, servers, start, end):
    if not player_a or not player_b:
        return dbc.Alert("Select both players.", color="warning", dismissable=True)
    if player_a == player_b:
        return dbc.Alert("Select two different players.", color="warning", dismissable=True)
    df = db.get_player_proximity(player_a, player_b, start, end, distance or 50, servers=servers)
    if df.empty:
        return html.Div(
            f"No proximity detected between {player_a} and {player_b} within {distance} blocks.",
            className="text-muted fst-italic")

    fig = px.scatter(df, x="a_x", y="a_z", color="distance_blocks",
                     hover_data=["time", "a_y", "b_x", "b_z"],
                     color_continuous_scale="RdYlGn_r", template=CHART_TEMPLATE,
                     labels={"a_x": "X", "a_z": "Z", "distance_blocks": "Distance (blocks)"})
    fig = chart_layout(fig)
    fig.update_layout(title=f"{player_a} locations when near {player_b}", height=380)
    fig.update_yaxes(autorange="reversed")

    return html.Div([
        dbc.Row([
            dbc.Col([
                dbc.Badge(f"{len(df)} proximity events", color="success", className="me-2 p-2"),
                html.Small(f"{player_a} ↔ {player_b}  within {distance} blocks",
                           className="text-muted"),
            ], className="mb-3"),
        ]),
        dcc.Graph(figure=fig, config={"displayModeBar": True, "scrollZoom": True}, className="mb-3"),
        section("Proximity Event Log"),
        make_table(df, "prox-tbl", page_size=25),
    ])


# Surface Map — step 1: load raw data into Store, populate dimension selector
@app.callback(
    Output("map-data-store", "data"),
    Output("map-dim-filter", "options"),
    Output("map-dim-filter", "value"),
    Output("map-dim-row", "style"),
    Output("map-table-output", "children"),
    Input("map-load-btn", "n_clicks"),
    State("map-event-type", "value"),
    State("map-cx", "value"),
    State("map-cz", "value"),
    State("map-radius", "value"),
    State("filter-servers", "value"),
    State("filter-players", "value"),
    State("filter-dates", "start_date"),
    State("filter-dates", "end_date"),
    prevent_initial_call=True,
)
def load_map(_n, event_type, cx, cz, radius, servers, players, start, end):
    df, err = db.get_map_data(event_type, players, start, end, servers=servers,
                              cx=cx, cz=cz, radius=radius, limit=100_000)
    label = (event_type or "events").replace("_", " ").title()
    hidden = {"display": "none"}
    visible = {"display": "block"}

    if df.empty:
        msg = f"No data for '{label}'."
        msg += f" DB error: {err}" if err else " Try selecting 'All time' in Quick Range."
        tbl = dbc.Alert(msg, color="danger" if err else "info", dismissable=True)
        return None, [], "__all__", hidden, tbl

    warn = [dbc.Alert(err, color="warning", dismissable=True)] if err else []

    dims = []
    if "dimension" in df.columns:
        dims = sorted(df["dimension"].dropna().unique().tolist())
    dim_opts = [{"label": "🌐 All dimensions", "value": "__all__"}] + [
        {"label": _dim_label(d), "value": d} for d in dims
    ]
    dim_style = visible if len(dims) > 1 else hidden

    tbl = html.Div([
        *warn,
        html.Small(
            f"Loaded {len(df):,} events across {len(dims)} dimension(s). "
            "Adjust Cell size or Color scale and the surface updates instantly.",
            className="text-muted fst-italic",
        ),
    ])
    return df.to_dict("records"), dim_opts, "__all__", dim_style, tbl


# Surface Map — step 2: rebuild surface when dim/cell-size/colorscale changes
@app.callback(
    Output("map-graph", "figure"),
    Input("map-data-store", "data"),
    Input("map-dim-filter", "value"),
    Input("map-cell-size", "value"),
    Input("map-colorscale", "value"),
    State("map-event-type", "value"),
    prevent_initial_call=True,
)
def render_map_figure(records, dim, cell_size, colorscale, event_type):
    if not records:
        return _no_data_surface("Click ▶ Load Map to render")

    df = pd.DataFrame(records)
    if dim and dim != "__all__" and "dimension" in df.columns:
        df = df[df["dimension"] == dim]

    return _build_surface_figure(df, event_type, dim, cell_size, colorscale)


# ─── 3D Map callbacks ─────────────────────────────────────────────────────────

# Step 1: load data, populate dim selector
@app.callback(
    Output("map3d-data-store", "data"),
    Output("map3d-dim-filter", "options"),
    Output("map3d-dim-filter", "value"),
    Output("map3d-controls-row", "style"),
    Output("map3d-stats-row", "children"),
    Input("map3d-load-btn", "n_clicks"),
    State("map3d-event-type", "value"),
    State("map3d-cx", "value"),
    State("map3d-cz", "value"),
    State("map3d-radius", "value"),
    State("filter-servers", "value"),
    State("filter-players", "value"),
    State("filter-dates", "start_date"),
    State("filter-dates", "end_date"),
    prevent_initial_call=True,
)
def load_map3d(_n, event_type, cx, cz, radius, servers, players, start, end):
    df, err = db.get_map_data(event_type, players, start, end, servers=servers,
                              cx=cx, cz=cz, radius=radius)
    label = (event_type or "events").replace("_", " ").title()
    hidden = {"display": "none"}
    visible = {"display": "block"}

    if df.empty:
        msg = f"No data for '{label}'."
        msg += f" DB error: {err}" if err else " Try selecting 'All time' in Quick Range."
        stats = dbc.Alert(msg, color="danger" if err else "info", dismissable=True)
        return None, [], "__all__", hidden, stats

    warn = [dbc.Alert(err, color="warning", dismissable=True)] if err else []

    # Dimension options
    dims = sorted(df["dimension"].dropna().unique().tolist()) if "dimension" in df.columns else []
    dim_opts = [{"label": "🌐 All dimensions", "value": "__all__"}] + [
        {"label": _dim_label(d), "value": d} for d in dims
    ]

    # Quick stats badges
    has_y = "y" in df.columns and df["y"].notna().any()
    y_range = ""
    if has_y:
        y_min = int(df["y"].min())
        y_max = int(df["y"].max())
        y_range = f"  Y {y_min} → {y_max}"
    zone_tag = f"  ·  Zone ({cx}, {cz}) ±{radius}" if (cx is not None and cz is not None) else ""
    stats = html.Div([
        *warn,
        dbc.Row([
            dbc.Col(dbc.Badge(f"{len(df):,} points", color="primary", className="p-2 me-2"), width="auto"),
            dbc.Col(dbc.Badge(f"{len(dims)} dimension(s)", color="secondary", className="p-2 me-2"), width="auto"),
            dbc.Col(html.Small(f"{y_range}{zone_tag}", className="text-muted align-self-center"), width="auto")
            if (y_range or zone_tag) else None,
        ], className="mb-3 g-1"),
    ])

    return df.to_dict("records"), dim_opts, "__all__", visible, stats


# Step 2: render 3D figure when Store, dim, or color-by changes
@app.callback(
    Output("map3d-graph", "figure"),
    Input("map3d-data-store", "data"),
    Input("map3d-dim-filter", "value"),
    Input("map3d-color-by", "value"),
    State("map3d-event-type", "value"),
    prevent_initial_call=True,
)
def render_map3d_figure(records, dim, color_by, event_type):
    if not records:
        return _no_data_3d("Click ▶ Load 3D Map to render")
    try:
        df = pd.DataFrame(records)

        if dim and dim != "__all__" and "dimension" in df.columns:
            df = df[df["dimension"] == dim]

        if "map_x" not in df.columns:
            return _no_data_3d("No coordinate data — schema missing position columns")

        return _build_3d_figure(df, event_type, color_by, dim)
    except Exception as exc:
        import traceback
        traceback.print_exc()
        return _no_data_3d(f"Render error: {exc}")


# Camera preset buttons — update figure camera without reloading data
@app.callback(
    Output("map3d-graph", "figure", allow_duplicate=True),
    Input("map3d-cam-iso",   "n_clicks"),
    Input("map3d-cam-top",   "n_clicks"),
    Input("map3d-cam-side",  "n_clicks"),
    Input("map3d-cam-front", "n_clicks"),
    State("map3d-graph", "figure"),
    prevent_initial_call=True,
)
def map3d_set_camera(_iso, _top, _side, _front, current_fig):
    from dash import ctx
    if not current_fig:
        return dash.no_update
    trigger = ctx.triggered_id
    key = {"map3d-cam-iso": "iso", "map3d-cam-top": "top",
           "map3d-cam-side": "side", "map3d-cam-front": "front"}.get(trigger, "iso")
    fig = go.Figure(current_fig)
    fig.update_layout(scene_camera=_3D_CAMERA_PRESETS[key])
    return fig


# ─── Post-mortem callbacks ───────────────────────────────────────────────────

@app.callback(
    Output("pm-death-picker", "children"),
    Input("pm-load-btn", "n_clicks"),
    State("pm-player-select", "value"),
    State("filter-servers", "value"),
    State("filter-dates", "start_date"),
    State("filter-dates", "end_date"),
    prevent_initial_call=True,
)
def pm_load_deaths(_n, player, servers, start, end):
    if not player:
        return dbc.Alert("Select a player first.", color="warning", dismissable=True)

    df = db.get_death_list(player, start, end, servers, limit=100)
    if df.empty:
        return dbc.Alert("No deaths found for this player in the selected period.", color="info", dismissable=True)

    opts = []
    for _, row in df.iterrows():
        label = (f"[{row['ts']}]  {row['cause']}"
                 + (f"  ← {row['killer']}" if row['killer'] != '—' else '')
                 + f"  HP:{row['hp']}  @({row['x']}, {row['z']})  {row['dimension']}")
        opts.append({"label": label, "value": row["ts"]})

    return dbc.Row([
        dbc.Col([
            html.Label("Select death event", className="text-muted small mb-1"),
            dcc.Dropdown(
                id="pm-death-select", options=opts, placeholder="Choose a death…",
                style=_dropdown_style(), className="dbc",
            ),
        ], md=9),
        dbc.Col([
            html.Label("\u00a0", className="d-block small mb-1"),
            dbc.Button("🔬 Run Post-mortem", id="pm-run-btn", color="danger", className="w-100"),
        ], md=3, className="d-flex align-items-end"),
    ], className="mb-2")


@app.callback(
    Output("pm-output", "children"),
    Input("pm-run-btn", "n_clicks"),
    State("pm-player-select", "value"),
    State("pm-death-select", "value"),
    State("pm-radius", "value"),
    State("pm-before-min", "value"),
    State("pm-after-min", "value"),
    State("filter-servers", "value"),
    State("filter-dates", "start_date"),
    State("filter-dates", "end_date"),
    prevent_initial_call=True,
)
def pm_run(_n, player, death_ts, radius, before_min, after_min, servers, start, end):
    if not player or not death_ts:
        return dbc.Alert("Select a player and a death event first.", color="warning", dismissable=True)

    radius     = int(radius     or 100)
    before_min = int(before_min or 5)
    after_min  = int(after_min  or 10)

    # Reload death details by matching timestamp
    death_df = db.get_death_list(player, start, end, servers, limit=100)
    row = death_df[death_df["ts"] == death_ts]
    if row.empty:
        return dbc.Alert("Could not retrieve death details.", color="danger", dismissable=True)
    row = row.iloc[0]

    death_x   = row["x"]
    death_z   = row["z"]
    death_dim = row["dimension"]

    from datetime import datetime, timedelta
    try:
        dt = datetime.strptime(death_ts, "%Y-%m-%d %H:%M:%S")
    except ValueError:
        return dbc.Alert("Invalid death timestamp.", color="danger", dismissable=True)
    t_before = (dt - timedelta(minutes=before_min)).strftime("%Y-%m-%d %H:%M:%S")

    # ── Queries ──────────────────────────────────────────────────────────────
    timeline_df = db.get_postmortem_before(player, t_before, death_ts, servers)

    # Nearby / loot require valid coordinates
    import math
    coords_ok = (death_x is not None and death_z is not None
                 and not (isinstance(death_x, float) and math.isnan(death_x))
                 and not (isinstance(death_z, float) and math.isnan(death_z)))

    if coords_ok:
        nearby_df = db.get_postmortem_nearby(player, death_ts, death_x, death_z, death_dim,
                                             radius, before_min, after_min, servers)
        loot_df   = db.get_postmortem_loot(player, death_ts, death_x, death_z, radius, after_min, servers)
    else:
        nearby_df = pd.DataFrame()
        loot_df   = pd.DataFrame()

    # ── Death card ────────────────────────────────────────────────────────────
    killer_val  = str(row.get("killer", "") or "").strip()
    weapon_val  = str(row.get("weapon", "") or "").strip()
    cause_val   = str(row.get("cause",  "") or "unknown").strip()
    is_pvp      = killer_val not in ("", "—", "None", "null")

    # If killer not stored but cause contains a player name hint, use cause as fallback
    killer_display = killer_val if is_pvp else "—"

    def _fmt(v):
        sv = str(v) if v is not None else "—"
        return sv if sv not in ("None", "nan", "", "null") else "—"

    cause_badge = dbc.Badge(
        f"⚔️ PvP — killed by {killer_display}" if is_pvp else "🌍 PvE / Environment",
        color="danger" if is_pvp else "warning",
        className="me-2 fs-6",
    )
    death_card = dbc.Card(dbc.CardBody([
        dbc.Row([
            dbc.Col([
                html.H5([cause_badge], className="mb-2"),
                html.P([html.Strong("Player: "), player], className="mb-1 small text-light"),
                html.P([html.Strong("Time: "), death_ts], className="mb-1 small"),
                html.P([html.Strong("Cause: "), cause_val], className="mb-1 small"),
                *([html.P([html.Strong("Killed by: "),
                           dbc.Badge(killer_display, color="danger", className="ms-1")],
                          className="mb-1 small")] if is_pvp else []),
                *([html.P([html.Strong("Weapon: "), weapon_val],
                          className="mb-1 small")] if is_pvp and weapon_val not in ("—", "") else []),
            ], md=5),
            dbc.Col([
                html.P([html.Strong("HP at death: "), f"{_fmt(row.get('hp'))}/20"], className="mb-1 small"),
                html.P([html.Strong("Armor: "),       _fmt(row.get("armor"))],      className="mb-1 small"),
                html.P([html.Strong("Food: "),        _fmt(row.get("food"))],        className="mb-1 small"),
            ], md=3),
            dbc.Col([
                html.P([html.Strong("Position: "), f"X={_fmt(row.get('x'))}  Y={_fmt(row.get('y'))}  Z={_fmt(row.get('z'))}"], className="mb-1 small"),
                html.P([html.Strong("Dimension: "), death_dim], className="mb-1 small"),
                html.P(html.Small(f"Nearby radius: {radius} blocks  ·  "
                                  f"Timeline: −{before_min} min  ·  Loot window: +{after_min} min",
                                  className="text-muted fst-italic")),
            ], md=4),
        ]),
    ]), style={"backgroundColor": "#3a1a1a", "border": "1px solid #cc4444"}, className="mb-4")

    # ── Timeline before death ────────────────────────────────────────────────
    _TYPE_COLOR = {
        "Attack": "danger", "Kill": "danger", "Death": "dark",
        "Block Break": "secondary", "Block Place": "success",
        "Item Drop": "warning", "Item Pickup": "info",
        "Chat": "primary", "Command": "light",
    }
    if not timeline_df.empty:
        tl_rows = []
        for _, r in timeline_df.sort_values("ts").iterrows():
            color = _TYPE_COLOR.get(r.get("type", ""), "secondary")
            tl_rows.append(html.Tr([
                html.Td(str(r.get("ts", ""))[-8:], className="text-muted small pe-3", style={"whiteSpace": "nowrap"}),
                html.Td(dbc.Badge(str(r.get("type", "")), color=color, className="me-1"), style={"whiteSpace": "nowrap"}),
                html.Td(str(r.get("details", "")), className="small"),
                html.Td(f"({r.get('x','?')}, {r.get('z','?')})" if r.get("x") else "—",
                        className="text-muted small", style={"whiteSpace": "nowrap"}),
            ]))
        timeline_block = dbc.Card(dbc.CardBody([
            html.H6(f"📜 {player}'s actions in the {before_min} minutes before death ({len(timeline_df)} events)",
                    className="text-info mb-3"),
            html.Div(
                html.Table([html.Tbody(tl_rows)],
                           className="table table-sm table-dark table-hover mb-0"),
                style={"maxHeight": "320px", "overflowY": "auto"},
            ),
        ]), style={"backgroundColor": CARD_BG2, "border": f"1px solid {BORDER}"}, className="mb-3")
    else:
        timeline_block = dbc.Alert(f"No events recorded for {player} in the {before_min} min before death.",
                                   color="secondary", className="mb-3")

    # ── Who was nearby ───────────────────────────────────────────────────────
    if not nearby_df.empty:
        # Suspect summary: count events per player
        suspect_counts = (nearby_df.groupby("player_name")
                          .agg(events=("event_type", "count"),
                               first_seen=("timestamp", "min"),
                               last_seen=("timestamp", "max"))
                          .sort_values("events", ascending=False)
                          .reset_index())
        suspect_badges = [
            dbc.Badge(f"{r['player_name']} ({r['events']} events)",
                      color="warning" if i == 0 else "secondary",
                      className="me-2 mb-2 p-2")
            for i, (_, r) in enumerate(suspect_counts.iterrows())
        ]
        nearby_block = dbc.Card(dbc.CardBody([
            html.H6(f"👁️ Who was nearby (±{radius} blocks, ±{before_min}/{after_min} min) — "
                    f"{nearby_df['player_name'].nunique()} player(s)",
                    className="text-warning mb-2"),
            html.Div(suspect_badges, className="mb-3"),
            _events_table(nearby_df, "pm-nearby-tbl", page_size=15, height="280px"),
        ]), style={"backgroundColor": CARD_BG2, "border": f"1px solid {BORDER}"}, className="mb-3")
    elif not coords_ok:
        nearby_block = dbc.Alert("Death has no recorded coordinates — cannot query nearby events.",
                                 color="secondary", className="mb-3")
    else:
        nearby_block = dbc.Alert(f"No other players recorded within {radius} blocks around the death time.",
                                 color="secondary", className="mb-3")

    # ── Loot picked up after ─────────────────────────────────────────────────
    if not loot_df.empty:
        looters = loot_df["player_name"].unique().tolist()
        loot_block = dbc.Card(dbc.CardBody([
            html.H6(f"📦 Items picked up near death in +{after_min} min by: "
                    f"{', '.join(looters)}",
                    className="text-success mb-2"),
            make_table(loot_df, "pm-loot-tbl", page_size=10, height="220px"),
        ]), style={"backgroundColor": CARD_BG2, "border": f"1px solid {BORDER}"}, className="mb-3")
    elif not coords_ok:
        loot_block = dbc.Alert("Death has no recorded coordinates — cannot query loot.",
                               color="secondary", className="mb-3")
    else:
        loot_block = dbc.Alert(f"No item pickups by others within {radius} blocks in the {after_min} min after death.",
                               color="secondary", className="mb-3")

    return html.Div([
        death_card,
        dbc.Row([
            dbc.Col(timeline_block, md=12),
        ]),
        dbc.Row([
            dbc.Col(nearby_block, md=7),
            dbc.Col(loot_block, md=5),
        ]),
    ])


# ─── Settings callbacks ───────────────────────────────────────────────────────

@app.callback(
    Output("settings-feedback", "children"),
    Input("btn-test-conn", "n_clicks"),
    Input("btn-save-settings", "n_clicks"),
    State("s-host", "value"),
    State("s-port", "value"),
    State("s-database", "value"),
    State("s-user", "value"),
    State("s-password", "value"),
    prevent_initial_call=True,
)
def handle_settings(test_clicks, save_clicks, host, port, database, user, password):
    from dash import ctx
    trigger = ctx.triggered_id
    cfg = load_config()
    cfg["database"] = {
        "host": host or "localhost",
        "port": int(port or 3306),
        "database": database or "god_eye",
        "user": user or "root",
        "password": password or "",
    }
    if trigger == "btn-save-settings":
        save_config(cfg)
        ok, msg = db.test_connection()
        color = "success" if ok else "warning"
        return dbc.Alert(f"Settings saved. Connection test: {msg}", color=color, dismissable=True)
    if trigger == "btn-test-conn":
        save_config(cfg)
        ok, msg = db.test_connection()
        color = "success" if ok else "danger"
        return dbc.Alert(f"Connection test: {msg}", color=color, dismissable=True)
    return dash.no_update


# ─── Launch ───────────────────────────────────────────────────────────────────

def open_browser(host, port):
    import time
    time.sleep(1.5)
    webbrowser.open(f"http://{host}:{port}")


if __name__ == "__main__":
    cfg = load_config()
    host = cfg["app"].get("host", "127.0.0.1")
    port = int(cfg["app"].get("port", 8050))

    threading.Thread(target=open_browser, args=(host, port), daemon=True).start()

    print(f"\n{'='*50}")
    print(f"  Tharidia God Eye Monitor")
    print(f"  Running at  http://{host}:{port}")
    print(f"  Press Ctrl+C to stop")
    print(f"{'='*50}\n")

    app.run(host=host, port=port, debug=False)
