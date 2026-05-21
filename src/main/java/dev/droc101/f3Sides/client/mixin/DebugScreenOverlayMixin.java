package dev.droc101.f3Sides.client.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import dev.droc101.f3Sides.client.DebugScreenDisplayerInterface;
import dev.droc101.f3Sides.client.DebugScreenEntryListInterface;
import dev.droc101.f3Sides.client.DebugScreenEntrySide;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.DebugScreenOverlay;
import net.minecraft.client.gui.components.debug.DebugScreenDisplayer;
import net.minecraft.client.gui.components.debug.DebugScreenEntryList;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Collection;
import java.util.List;
import java.util.Map;

@Mixin(DebugScreenOverlay.class)
public class DebugScreenOverlayMixin {

    @Inject(method = "extractRenderState", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/debug/DebugScreenEntry;display(Lnet/minecraft/client/gui/components/debug/DebugScreenDisplayer;Lnet/minecraft/world/level/Level;Lnet/minecraft/world/level/chunk/LevelChunk;Lnet/minecraft/world/level/chunk/LevelChunk;)V"))
    void setNextId(GuiGraphicsExtractor graphics, CallbackInfo ci, @Local(name = "id") Identifier id, @Local(name = "displayer") DebugScreenDisplayer displayer) {
        ((DebugScreenDisplayerInterface) displayer).f3_sides$setNextIdentifier(id);
    }

    @Redirect(method = "extractRenderState", at = @At(value = "INVOKE", target = "Ljava/util/List;isEmpty()Z", ordinal = 3))
    boolean addGroupsCorrectly(List<String> instance, @Local(name = "groups") Map<Identifier, Collection<String>> groups, @Local(name = "leftLines") List<String> leftLines, @Local(name = "rightLines") List<String> rightLines) {
        for (Map.Entry<Identifier, Collection<String>> group : groups.entrySet()) {
            Collection<String> lines = group.getValue();
            if (!lines.isEmpty()) {
                DebugScreenEntryList entries = Minecraft.getInstance().debugEntries;
                DebugScreenEntrySide side = ((DebugScreenEntryListInterface) entries).f3sides$getSide(group.getKey());
                if (side == DebugScreenEntrySide.AUTO) {
                    if (leftLines.size() > rightLines.size()) {
                        side = DebugScreenEntrySide.RIGHT;
                    } else {
                        side = DebugScreenEntrySide.LEFT;
                    }
                }
                if (side == DebugScreenEntrySide.LEFT) {
//                    leftLines.add(group.getKey().toString());
                    leftLines.addAll(lines);
                    leftLines.add("");
                } else {
//                    rightLines.add(group.getKey().toString());
                    rightLines.addAll(lines);
                    rightLines.add("");
                }
            }
        }

        return true;
    }

}
