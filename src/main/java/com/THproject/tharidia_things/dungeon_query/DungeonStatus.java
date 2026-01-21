package com.THproject.tharidia_things.dungeon_query;

public enum DungeonStatus {
    // No players in doungeon
    IDLE,
    // players are in queue waiting to enter
    QUEUE,
    // dungeon are starting
    STARTING,
    // players are inside the dungeon
    RUNNING
}
