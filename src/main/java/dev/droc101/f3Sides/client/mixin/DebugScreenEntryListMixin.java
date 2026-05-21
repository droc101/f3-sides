package dev.droc101.f3Sides.client.mixin;

import com.google.gson.JsonElement;
import com.google.gson.JsonSyntaxException;
import com.mojang.datafixers.DataFixer;
import com.mojang.serialization.Codec;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.droc101.f3Sides.client.DebugScreenEntryListInterface;
import dev.droc101.f3Sides.client.DebugScreenEntrySide;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.components.debug.DebugScreenEntryList;
import net.minecraft.resources.Identifier;
import net.minecraft.util.StrictJsonParser;
import org.apache.commons.io.FileUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Mixin(DebugScreenEntryList.class)
public abstract class DebugScreenEntryListMixin implements DebugScreenEntryListInterface {

    @Unique
    private final Map<Identifier, DebugScreenEntrySide> allSides = new HashMap<>();

    @Unique
    private File debugSidesFile;

    @Unique
    private Codec<DebugScreenEntryListMixin.SerializedSides> sidesCodec = SerializedSides.CODEC;

    @Inject(method = "<init>", at = @At("CTOR_HEAD"))
    void ctor(File workingDirectory, DataFixer dataFixer, CallbackInfo ci) {
        debugSidesFile = new File(workingDirectory, "debug-sides-profile.json");
    }

    @Inject(method = "load", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/debug/DebugScreenEntryList;rebuildCurrentList()V"))
    void load(CallbackInfo ci) {
        DebugScreenEntryList list = (DebugScreenEntryList) ((Object) this);

        try {
            if (!debugSidesFile.isFile()) {
                return;
            }

            Dynamic<JsonElement> data = new Dynamic<>(JsonOps.INSTANCE, StrictJsonParser.parse(FileUtils.readFileToString(debugSidesFile, StandardCharsets.UTF_8)));
            DebugScreenEntryListMixin.SerializedSides serializedOptions = sidesCodec.parse(data).getOrThrow((error) -> new IOException("Could not parse debug sides profile JSON: " + error));
            resetSides(serializedOptions.custom().orElse(Map.of()));
        } catch (JsonSyntaxException | IOException e) {
            DebugScreenEntryList.LOGGER.error("Couldn't read debug sides profile file {}, resetting to default", debugSidesFile, e);
            list.save();
        }

    }

    @Inject(method="save", at=@At("TAIL"))
    void save(CallbackInfo ci) {
        SerializedSides serializedOptions = new SerializedSides(Optional.of(allSides));

        try {
            FileUtils.writeStringToFile(debugSidesFile, sidesCodec.encodeStart(JsonOps.INSTANCE, serializedOptions).getOrThrow().toString(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            DebugScreenEntryList.LOGGER.error("Failed to save debug sides profile file {}", debugSidesFile, e);
        }
    }

    @Environment(EnvType.CLIENT)
    record SerializedSides(Optional<Map<Identifier, DebugScreenEntrySide>> custom) {
        private static final Codec<Map<Identifier, DebugScreenEntrySide>> CUSTOM_ENTRIES_CODEC;
        public static final Codec<DebugScreenEntryListMixin.SerializedSides> CODEC;

        static {
            CUSTOM_ENTRIES_CODEC = Codec.unboundedMap(Identifier.CODEC, DebugScreenEntrySide.CODEC);
            CODEC = RecordCodecBuilder.create((i) -> i.group(CUSTOM_ENTRIES_CODEC.optionalFieldOf("data").forGetter(DebugScreenEntryListMixin.SerializedSides::custom)).apply(i, DebugScreenEntryListMixin.SerializedSides::new));
        }
    }

    @Override
    public void f3sides$setSide(final Identifier location, final DebugScreenEntrySide side) {
        DebugScreenEntryList list = (DebugScreenEntryList) ((Object) this);
        allSides.put(location, side);
        list.rebuildCurrentList();
        list.save();
    }

    @Override
    public DebugScreenEntrySide f3sides$getSide(final Identifier location) {
        return allSides.getOrDefault(location, DebugScreenEntrySide.AUTO);
    }

    @Unique
    public final void resetSides(final Map<Identifier, DebugScreenEntrySide> newEntries) {
        allSides.clear();
        allSides.putAll(newEntries);
    }

}
