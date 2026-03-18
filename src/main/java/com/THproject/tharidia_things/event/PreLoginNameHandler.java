package com.THproject.tharidia_things.event;

/**
 * Previously handled pre-login name selection check.
 * This logic has been moved to CharacterEventHandler.onPlayerLoggedIn() to unify
 * the character creation flow (name → race → complete) in a single handler.
 *
 * This class is kept empty to avoid breaking the event bus registration in TharidiaThings.
 * It can be safely removed once the registration line is also removed.
 */
public class PreLoginNameHandler {
    // Intentionally empty — all logic moved to CharacterEventHandler
}
