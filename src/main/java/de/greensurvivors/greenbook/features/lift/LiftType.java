package de.greensurvivors.greenbook.features.lift;

public enum LiftType {
    UP, //tps up
    DOWN, //tps down
    BOTH, //tps up or down, depending on where the player looked, defaults to down
    STOP //just let other signs tp to this floor
}
