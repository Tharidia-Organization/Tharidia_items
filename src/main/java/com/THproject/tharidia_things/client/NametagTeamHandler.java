package com.THproject.tharidia_things.client;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.Team;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/**
 * Server-side handler that assigns every player to a scoreboard team with
 * nameTagVisibility = NEVER on join.
 *
 * This prevents vanilla's see-through nametag rendering (the semi-transparent
 * name shown through walls), which is controlled by the scoreboard/team system
 * independently of the client-side RenderNameTagEvent.
 *
 * Admins still see names because NeoForge fires RenderNameTagEvent even for
 * entities whose team says NEVER, and NametagVisibilityHandler forces
 * canRender = TRUE for op-4 viewers.
 */
@EventBusSubscriber(modid = "tharidiathings")
public class NametagTeamHandler {

    /** Scoreboard team name (≤ 16 characters). */
    private static final String TEAM_NAME = "th_nametag";

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        Scoreboard scoreboard = player.server.getScoreboard();

        // Create team if it doesn't exist yet
        PlayerTeam team = scoreboard.getPlayerTeam(TEAM_NAME);
        if (team == null) {
            team = scoreboard.addPlayerTeam(TEAM_NAME);
            team.setNameTagVisibility(Team.Visibility.NEVER);
            team.setCollisionRule(Team.CollisionRule.NEVER); // no side-effects from team
        }

        // Ensure visibility stays NEVER (in case someone changed it via command)
        team.setNameTagVisibility(Team.Visibility.NEVER);

        // Add player to the team (automatically removes from any previous team)
        PlayerTeam current = scoreboard.getPlayersTeam(player.getScoreboardName());
        if (current == null || !current.getName().equals(TEAM_NAME)) {
            scoreboard.addPlayerToTeam(player.getScoreboardName(), team);
        }
    }
}
