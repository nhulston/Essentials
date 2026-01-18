package com.nhulston.essentials.afk;

import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.jetbrains.annotations.Nullable;

public class AfkComponent implements Component<EntityStore> {

    private Vector3d lastLocation;
    private int secondsSinceLastMoved;

    public AfkComponent(Vector3d lastLocation, int secondsSinceLastMoved) {
        this.lastLocation = lastLocation;
        this.secondsSinceLastMoved = secondsSinceLastMoved;
    }

    public AfkComponent(AfkComponent other) {
        this.lastLocation = other.lastLocation;
        this.secondsSinceLastMoved = other.secondsSinceLastMoved;
    }

    public AfkComponent() {
        this.lastLocation = new Vector3d(0, 0, 0);
        this.secondsSinceLastMoved = 0;
    }

    @Override
    public @Nullable Component<EntityStore> clone() {
        return new AfkComponent(this);
    }

    public Vector3d getLastLocation() {
        return lastLocation;
    }

    public void setLastLocation(Vector3d lastLocation) {
        this.lastLocation = lastLocation;
    }

    public int getSecondsSinceLastMoved() {
        return secondsSinceLastMoved;
    }

    public void setSecondsSinceLastMoved(int secondsSinceLastMoved) {
        this.secondsSinceLastMoved = secondsSinceLastMoved;
    }
}
