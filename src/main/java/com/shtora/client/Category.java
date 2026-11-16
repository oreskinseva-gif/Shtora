package com.shtora.client;

public enum Category {
    COMBAT("Combat", "Battle"),
    MOVEMENT("Movement", "Battle"),
    PLAYER("Player", "Battle"),
    RENDER("Render", "Visual"),
    MISC("Misc", "Other"),
    CLIENT("Client", "Other");

    public final String title;
    public final String section;

    Category(String title, String section) {
        this.title = title;
        this.section = section;
    }
}
