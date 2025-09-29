package com.example.tharidia_items.block.entity;

public class AlchimistTableDispatcher {
    public void playSlideshow(AlchimistTableBlockEntity entity) {
        entity.triggerAnim("base_controller", "slideshow");
    }
}