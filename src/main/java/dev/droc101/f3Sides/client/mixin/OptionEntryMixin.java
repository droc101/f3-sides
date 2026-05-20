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
    private static final Component LEFT_SIDE_TEXT = Component.literal("Left");
    @Unique
    private static final Component RIGHT_SIDE_TEXT = Component.literal("Right");

    @Unique
    private CycleButton<Boolean> left;
    @Unique
    private CycleButton<Boolean> right;

    @Unique
    private CycleButton<DebugScreenEntryStatus> status;

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
    private void SetNextStatus(DebugOptionsScreen.OptionEntry entry) {
        DebugScreenEntryStatus status = Minecraft.getInstance().debugEntries.getStatus(entry.location);
        switch (status) {
            case ALWAYS_ON -> {
                entry.setValue(entry.location, DebugScreenEntryStatus.NEVER);
            }
            case IN_OVERLAY -> {
                entry.setValue(entry.location, DebugScreenEntryStatus.ALWAYS_ON);
            }
            case NEVER -> {
                entry.setValue(entry.location, DebugScreenEntryStatus.IN_OVERLAY);
            }
        }
    }

    @Inject(method = "<init>", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/debug/DebugOptionsScreen$OptionEntry;refreshEntry()V"))
    void CtorInject(CallbackInfo ci) {
        DebugOptionsScreen.OptionEntry entry = (DebugOptionsScreen.OptionEntry) ((Object) this);
        left = CycleButton.booleanBuilder(
                LEFT_SIDE_TEXT.copy().withColor(0xffd0ffd0), LEFT_SIDE_TEXT.copy().withColor(-4539718), false
                ).displayOnlyValue()
                .create(
                        10, 5, 30, 16,
                        Component.literal(entry.name),
                        (button, newValue) -> setSide(entry.location, DebugScreenEntrySide.LEFT)
                );
        right = CycleButton.booleanBuilder(
                        RIGHT_SIDE_TEXT.copy().withColor(0xffd0f0ff), RIGHT_SIDE_TEXT.copy().withColor(-4539718), false
                ).displayOnlyValue()
                .create(
                        10, 5, 30, 16,
                        Component.literal(entry.name),
                        (button, newValue) -> setSide(entry.location, DebugScreenEntrySide.RIGHT)
                );

        status = CycleButton.builder(OptionEntryMixin::GetStatusText, DebugScreenEntryStatus.NEVER)
                .withValues(DebugScreenEntryStatus.values())
                .create(
                        10, 5, 80, 16,
                        Component.translatable("debug.entry.mode"),
                        (button, value) -> SetNextStatus(entry));

        entry.children.clear();
        entry.children.add(status);
        entry.children.add(left);
        entry.children.add(right);
        entry.never.setWidth(28);
        entry.overlay.setWidth(28);
        entry.always.setWidth(28);
    }

    @Inject(method="extractContent", at= @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/CycleButton;setX(I)V"), cancellable = true)
    public void extractContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovered, float a, CallbackInfo ci) {
        DebugOptionsScreen.OptionEntry entry = (DebugOptionsScreen.OptionEntry) ((Object) this);

        int buttonsStartX = entry.getContentX() + entry.getContentWidth() - status.getWidth() - 2 - left.getWidth() - right.getWidth();

        status.setX(buttonsStartX);
        left.setX(status.getX() + status.getWidth() + 2);
        right.setX(left.getX() + left.getWidth());

        left.setY(entry.getContentY());
        right.setY(entry.getContentY());
        status.setY(entry.getContentY());

        status.extractRenderState(graphics, mouseX, mouseY, a);
        left.extractRenderState(graphics, mouseX, mouseY, a);
        right.extractRenderState(graphics, mouseX, mouseY, a);

        ci.cancel();
    }

    @Inject(method="refreshEntry", at=@At("TAIL"))
    void refreshEntry(CallbackInfo ci) {
        DebugOptionsScreen.OptionEntry entry = (DebugOptionsScreen.OptionEntry) ((Object) this);
        DebugScreenEntryList entries = Minecraft.getInstance().debugEntries;
        DebugScreenEntrySide side = ((DebugScreenEntryListInterface)entries).f3sides$getSide(entry.location);
        left.setValue(side == DebugScreenEntrySide.LEFT);
        right.setValue(side == DebugScreenEntrySide.RIGHT);
        left.active = !left.getValue();
        right.active = !right.getValue();
        DebugScreenEntryStatus statusValue = entries.getStatus(entry.location);
        status.setValue(statusValue);

        boolean isNoop = DebugScreenEntries.getEntry(entry.location) instanceof DebugEntryNoop;
        if (isNoop) {
            left.active = false;
            right.active = false;
            left.setMessage(LEFT_SIDE_TEXT.copy().withColor(-4539718));
            right.setMessage(RIGHT_SIDE_TEXT.copy().withColor(-4539718));
        }
    }

    @Unique
    public final void setSide(final Identifier location, final DebugScreenEntrySide side) {
        DebugOptionsScreen.OptionEntry entry = (DebugOptionsScreen.OptionEntry) ((Object) this);
        DebugScreenEntryList entries = Minecraft.getInstance().debugEntries;
        ((DebugScreenEntryListInterface)entries).f3sides$setSide(location, side);

        entry.refreshEntry();
    }

}
