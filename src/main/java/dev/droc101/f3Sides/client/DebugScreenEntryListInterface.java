package dev.droc101.f3Sides.client;

import net.minecraft.resources.Identifier;

public interface DebugScreenEntryListInterface {
    default void f3sides$setSide(final Identifier location, final DebugScreenEntrySide side) {
        throw new AssertionError("implemented in mixin");
    }

    default DebugScreenEntrySide f3sides$getSide(final Identifier location) {
        throw new AssertionError("implemented in mixin");
    }
}
