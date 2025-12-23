package com.THproject.tharidia_things.entity.animation;

import com.THproject.tharidia_things.entity.TrebuchetEntity;
import com.THproject.tharidia_things.entity.TrebuchetEntity.TrebuchetState;
import mod.azure.azurelib.common.animation.dispatch.command.AzCommand;
import mod.azure.azurelib.common.animation.play_behavior.AzPlayBehaviors;

/**
 * Sends AzCommands based on the trebuchet's current state.
 */
public class TrebuchetAnimationDispatcher {

    private static final String CONTROLLER = "base_controller";
    private static final AzCommand IDLE = AzCommand.create(CONTROLLER, "idle", AzPlayBehaviors.LOOP);
    private static final AzCommand LOAD = AzCommand.create(CONTROLLER, "load", AzPlayBehaviors.PLAY_ONCE);
    private static final AzCommand LOADED = AzCommand.create(CONTROLLER, "loaded", AzPlayBehaviors.LOOP);
    private static final AzCommand FIRE = AzCommand.create(CONTROLLER, "fire", AzPlayBehaviors.PLAY_ONCE);
    private static final AzCommand RELOAD = AzCommand.create(CONTROLLER, "reload", AzPlayBehaviors.PLAY_ONCE);

    private final TrebuchetEntity entity;
    private TrebuchetState activeState;

    public TrebuchetAnimationDispatcher(TrebuchetEntity entity) {
        this.entity = entity;
    }

    public void applyState(TrebuchetState state) {
        if (state == null || state == activeState) {
            return;
        }
        activeState = state;

        switch (state) {
            case LOADING -> LOAD.sendForEntity(entity);
            case LOADED -> LOADED.sendForEntity(entity);
            case FIRING -> FIRE.sendForEntity(entity);
            default -> IDLE.sendForEntity(entity);
        }
    }

    /**
     * Forces the next applyState call to resend the command (e.g. after the entity is added to the level).
     */
    public void reset() {
        this.activeState = null;
    }
}
