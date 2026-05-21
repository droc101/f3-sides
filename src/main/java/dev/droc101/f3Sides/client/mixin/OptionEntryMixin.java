package dev.droc101.f3Sides.client.mixin;

import dev.droc101.f3Sides.client.DebugScreenEntryListInterface;
import dev.droc101.f3Sides.client.DebugScreenEntrySide;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.debug.DebugEntryNoop;
import net.minecraft.client.gui.components.debug.DebugScreenEntries;
import net.minecraft.client.gui.components.debug.DebugScreenEntryList;
import net.minecraft.client.gui.components.debug.DebugScreenEntryStatus;
import net.minecraft.client.gui.screens.debug.DebugOptionsScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net.minecraft.client.gui.screens.debug.DebugOptionsScreen$OptionEntry")
public abstract class OptionEntryMixin {
    @Unique
    private CycleButton<DebugScreenEntryStatus> statusButton;

    @Unique
    private CycleButton<DebugScreenEntrySide> sideButton;

    @Unique
    private static Component GetStatusText(DebugScreenEntryStatus status) {
        switch (status) {
            case ALWAYS_ON -> {
                return Component.translatable("debug.entry.always");
            }
            case IN_OVERLAY -> {
                return Component.translatable("debug.entry.overlay");
            }
            case NEVER -> {
                return Component.translatable("debug.entry.never");
            }
        }
        return Component.literal(status.getSerializedName());
    }

    @Unique
    private static Component GetSideText(DebugScreenEntrySide side) {
        switch (side) {
            case AUTO -> {
                return Component.translatable("debug.entry.auto");
            }
            case LEFT -> {
                return Component.translatable("debug.entry.left");
            }
            case RIGHT -> {
                return Component.translatable("debug.entry.right");
            }
        }
        return Component.literal(side.getSerializedName());
    }

    @Unique
    private void SetNextStatus(DebugOptionsScreen.OptionEntry entry) {
        DebugScreenEntryStatus status = Minecraft.getInstance().debugEntries.getStatus(entry.location);
        switch (status) {
            case ALWAYS_ON -> entry.setValue(entry.location, DebugScreenEntryStatus.NEVER);
            case IN_OVERLAY -> entry.setValue(entry.location, DebugScreenEntryStatus.ALWAYS_ON);
            case NEVER -> entry.setValue(entry.location, DebugScreenEntryStatus.IN_OVERLAY);
        }
    }

    @Unique
    private void SetNextSide(DebugOptionsScreen.OptionEntry entry) {
        DebugScreenEntryListInterface entries = (DebugScreenEntryListInterface)(Minecraft.getInstance().debugEntries);
        DebugScreenEntrySide side = entries.f3sides$getSide(entry.location);
        switch (side) {
            case AUTO -> setSide(entry.location, DebugScreenEntrySide.LEFT);
            case LEFT -> setSide(entry.location, DebugScreenEntrySide.RIGHT);
            case RIGHT -> setSide(entry.location, DebugScreenEntrySide.AUTO);
        }
    }

    @Unique
    public final void setSide(final Identifier location, final DebugScreenEntrySide side) {
        DebugOptionsScreen.OptionEntry entry = (DebugOptionsScreen.OptionEntry) ((Object) this);
        DebugScreenEntryList entries = Minecraft.getInstance().debugEntries;
        ((DebugScreenEntryListInterface)entries).f3sides$setSide(location, side);

        entry.refreshEntry();
    }

    @Inject(method = "<init>", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/debug/DebugOptionsScreen$OptionEntry;refreshEntry()V"))
    void CtorInject(CallbackInfo ci) {
        DebugOptionsScreen.OptionEntry entry = (DebugOptionsScreen.OptionEntry) ((Object) this);

        statusButton = CycleButton.builder(OptionEntryMixin::GetStatusText, DebugScreenEntryStatus.NEVER)
                .withValues(DebugScreenEntryStatus.values())
                .create(
                        10, 5, 80, 16,
                        Component.translatable("debug.entry.mode"),
                        (button, value) -> SetNextStatus(entry));

        sideButton = CycleButton.builder(OptionEntryMixin::GetSideText, DebugScreenEntrySide.AUTO)
                .withValues(DebugScreenEntrySide.values())
                .create(
                        10, 5, 80, 16,
                        Component.translatable("debug.entry.side"),
                        (button, value) -> SetNextSide(entry));

        entry.children.clear();
        entry.children.add(statusButton);
        entry.children.add(sideButton);
    }

    @Inject(method="extractContent", at= @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/CycleButton;setX(I)V"), cancellable = true)
    public void extractContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovered, float a, CallbackInfo ci) {
        DebugOptionsScreen.OptionEntry entry = (DebugOptionsScreen.OptionEntry) ((Object) this);

        int buttonsStartX = entry.getContentX() + entry.getContentWidth() - statusButton.getWidth() - 2 - sideButton.getWidth();

        statusButton.setX(buttonsStartX);
        sideButton.setX(statusButton.getX() + statusButton.getWidth() + 2);
        sideButton.setY(entry.getContentY());
        statusButton.setY(entry.getContentY());

        statusButton.extractRenderState(graphics, mouseX, mouseY, a);
        sideButton.extractRenderState(graphics, mouseX, mouseY, a);

        ci.cancel();
    }

    @Inject(method="refreshEntry", at=@At("TAIL"))
    void refreshEntry(CallbackInfo ci) {
        DebugOptionsScreen.OptionEntry entry = (DebugOptionsScreen.OptionEntry) ((Object) this);
        DebugScreenEntryList entries = Minecraft.getInstance().debugEntries;
        DebugScreenEntrySide side = ((DebugScreenEntryListInterface)entries).f3sides$getSide(entry.location);
        sideButton.setValue(side);
        DebugScreenEntryStatus statusValue = entries.getStatus(entry.location);
        statusButton.setValue(statusValue);

        boolean isNoop = DebugScreenEntries.getEntry(entry.location) instanceof DebugEntryNoop;
        if (isNoop) {
            sideButton.active = false;
        }
    }

}
