"""
app.py  –  Tharidia God Eye Monitor
A Dash web-based dashboard for the MariaDB god_eye database.
Run:  python app.py   then open  http://localhost:8050
"""

import threading
import webbrowser
from datetime import date, timedelta

import pandas as pd
import dash
import dash_bootstrap_components as dbc
import plotly.express as px
import plotly.graph_objects as go
from dash import Input, Output, State, dash_table, dcc, html

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
                        value="7d", clearable=False,
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
        dbc.Tab(label="🔍 Investigate",     tab_id="investigate"),
        dbc.Tab(label="🗺️ Map",             tab_id="map"),
        dbc.Tab(label="🌐 3D Map",          tab_id="map3d"),
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
    activity_df = db.get_hourly_activity(start, end, servers=servers)
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
    players_df = db.get_all_players()
    sessions_df = db.get_session_stats(players, start, end, servers=servers)
    logins_df = db.get_login_sessions(players, start, end, servers=servers)
    logins_per_day = db.get_logins_per_day(players, start, end, servers=servers)

    fig_lpd = no_data_fig("No login data")
    if not logins_per_day.empty:
        fig_lpd = px.bar(logins_per_day, x="day", y="logins",
                         labels={"day": "", "logins": "Logins"},
                         color_discrete_sequence=CHART_COLORS[:1],
                         template=CHART_TEMPLATE)
        fig_lpd = chart_layout(fig_lpd)
        fig_lpd.update_layout(title="Daily Logins", height=250)

    fig_hours = no_data_fig("No session data")
    if not sessions_df.empty:
        fig_hours = px.bar(sessions_df.head(15), x="player", y="total_hours",
                           labels={"player": "", "total_hours": "Hours"},
                           color="total_hours", color_continuous_scale="Blues",
                           template=CHART_TEMPLATE)
        fig_hours = chart_layout(fig_hours)
        fig_hours.update_layout(title="Total Playtime per Player (hours)", height=250,
                                showlegend=False, coloraxis_showscale=False)

    return html.Div([
        dbc.Row([
            dbc.Col(dcc.Graph(figure=fig_lpd, config={"displayModeBar": False}), md=6),
            dbc.Col(dcc.Graph(figure=fig_hours, config={"displayModeBar": False}), md=6),
        ], className="mb-3"),
        section("All Registered Players"),
        make_table(players_df, "players-all", page_size=20),
        html.Br(),
        section("Session Stats", "total time and average session length per player"),
        make_table(sessions_df, "sessions-stats", page_size=20),
        html.Br(),
        section("Login History", "last 2000 sessions"),
        make_table(logins_df, "logins", page_size=20),
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
        make_table(deaths_df, "deaths", page_size=20),
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

    return html.Div([
        section("🔍 Investigation Tools",
                "Trace actions, prove or disprove incidents — uses global date filter"),
        dbc.Tabs([tl, pvp, revive, tr, loc, prox], id="inv-subtabs", active_tab="inv-tl"),
    ])


# ─── Map View ─────────────────────────────────────────────────────────────────

def render_map_view(players, start, end):
    return html.Div([
        dcc.Store(id="map-data-store"),
        section("🗺️ Coordinate Map",
                "Top-down view: X = East/West axis, Z = North/South axis (South positive)"),

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
            ], md=4),
            dbc.Col([
                html.Label("Max points", className="text-muted small mb-1"),
                dcc.Dropdown(id="map-limit", options=[
                    {"label": "1 000",  "value": 1000},
                    {"label": "5 000",  "value": 5000},
                    {"label": "10 000", "value": 10000},
                ], value=5000, clearable=False, style=_dropdown_style(), className="dbc"),
            ], md=2),
            dbc.Col([
                html.Label("\u00a0", className="d-block small mb-1"),
                dbc.Button("▶ Load Map", id="map-load-btn", color="primary"),
            ], md=2),
        ], className="mb-2 align-items-end"),

        html.Div(
            html.Small(
                "💡 Tip: use the player filter above to isolate one player. "
                "Scroll to zoom, drag to pan, hover for details. "
                "Block events may have millions of rows — keep limit low or filter by player.",
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
            figure=no_data_fig("Click ▶ Load Map to render"),
            config={"displayModeBar": True, "scrollZoom": True},
            style={"height": "620px"},
        ),
        html.Br(),
        html.Div(id="map-table-output"),
    ])


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
    if "details" in cols:
        lines = lines + "<br>" + df["details"].fillna("").astype(str).str[:80]
    if "timestamp" in cols:
        lines = lines + "<br>🕐 " + df["timestamp"].fillna("").astype(str)
    return lines.tolist()


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
    if tab in ("investigate", "map", "map3d") and triggered not in ("main-tabs", None):
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
    if tab == "investigate":
        return render_investigate(players, start, end)
    if tab == "map":
        return render_map_view(players, start, end)
    if tab == "map3d":
        return render_map3d_view(players, start, end)
    if tab == "reports":
        return render_reports(players, start, end)
    if tab == "settings":
        return render_settings()
    return html.Div("Unknown tab.")


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

    cond = [
        {"if": {"filter_query": f'{{type}} = "{t}"'}, "backgroundColor": c, "color": TEXT}
        for t, c in _TL_COLORS.items()
    ] + [{"if": {"filter_query": '{type} = "Death"'}, "fontWeight": "bold"}]

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
                columns=[{"name": c.replace("_", " ").title(), "id": c} for c in df.columns],
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
        make_table(df, "loc-tbl", page_size=25),
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


# Map — step 1: load data into Store, populate dimension selector
@app.callback(
    Output("map-data-store", "data"),
    Output("map-dim-filter", "options"),
    Output("map-dim-filter", "value"),
    Output("map-dim-row", "style"),
    Output("map-table-output", "children"),
    Input("map-load-btn", "n_clicks"),
    State("map-event-type", "value"),
    State("map-limit", "value"),
    State("filter-servers", "value"),
    State("filter-players", "value"),
    State("filter-dates", "start_date"),
    State("filter-dates", "end_date"),
    prevent_initial_call=True,
)
def load_map(_n, event_type, limit, servers, players, start, end):
    df, err = db.get_map_data(event_type, players, start, end, int(limit or 5000), servers=servers)
    label = event_type.replace("_", " ").title()
    hidden = {"display": "none"}
    visible = {"display": "block"}

    if df.empty:
        msg = f"No data for '{label}'."
        msg += f" DB error: {err}" if err else " Try selecting 'All time' in Quick Range."
        tbl = dbc.Alert(msg, color="danger" if err else "info", dismissable=True)
        return None, [], "__all__", hidden, tbl

    warn = [dbc.Alert(err, color="warning", dismissable=True)] if err else []

    # Build dimension options
    dims = []
    if "dimension" in df.columns:
        dims = sorted(df["dimension"].dropna().unique().tolist())
    dim_opts = [{"label": "🌐 All dimensions", "value": "__all__"}] + [
        {"label": _dim_label(d), "value": d} for d in dims
    ]
    dim_style = visible if len(dims) > 1 else hidden

    tbl = html.Div([
        *warn,
        section(f"Map Data — {len(df):,} rows  ({len(dims)} dimension(s))"),
        make_table(df, "map-tbl", page_size=20),
    ])
    return df.to_dict("records"), dim_opts, "__all__", dim_style, tbl


# Map — step 2: re-render figure when dimension selection changes
@app.callback(
    Output("map-graph", "figure"),
    Input("map-data-store", "data"),
    Input("map-dim-filter", "value"),
    State("map-event-type", "value"),
    prevent_initial_call=True,
)
def render_map_figure(records, dim, event_type):
    if not records:
        return no_data_fig("Click ▶ Load Map to render")

    df = pd.DataFrame(records)
    label = (event_type or "events").replace("_", " ").title()

    # Filter by dimension
    if dim and dim != "__all__" and "dimension" in df.columns:
        df = df[df["dimension"] == dim]
        dim_tag = f" · {_dim_label(dim)}"
    else:
        dim_tag = ""

    has_coords = "map_x" in df.columns and df["map_x"].notna().any()
    if not has_coords:
        return no_data_fig(f"No coordinate data for '{label}' — schema missing position columns")

    plot_df = df.dropna(subset=["map_x", "map_z"])
    fig = px.scatter(
        plot_df, x="map_x", y="map_z", color="player",
        hover_data=[c for c in ["details", "y", "dimension", "timestamp"] if c in df.columns],
        labels={"map_x": "X (East →)", "map_z": "Z (South ↓)", "player": "Player"},
        color_discrete_sequence=CHART_COLORS, template=CHART_TEMPLATE,
    )
    fig = chart_layout(fig)
    fig.update_layout(
        title=f"{label}{dim_tag} — {len(plot_df):,} points  (scroll to zoom, drag to pan)",
        height=620,
        legend=dict(itemsizing="constant", bgcolor="rgba(0,0,0,0.3)"),
    )
    fig.update_yaxes(autorange="reversed", title="Z (South ↓)", scaleanchor="x", scaleratio=1)
    fig.update_xaxes(title="X (East →)")
    return fig


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
