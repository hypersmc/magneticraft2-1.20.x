package com.magneticraft2.client.hologram;

import com.magneticraft2.common.hologram.AncientProjectorBlockEntity;
import com.magneticraft2.common.magneticraft2;
import com.magneticraft2.common.registry.registers.ItemRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

/**
 * Client-only item lore, backed by the same data-driven recording that is
 * rendered in the world. Never load client scene assets on a dedicated server.
 * The intentionally scrambled fragments preserve the Discord teaser style.
 */
@Mod.EventBusSubscriber(modid = magneticraft2.MOD_ID, value = Dist.CLIENT,
        bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class AncientProjectorTooltip {
    private AncientProjectorTooltip() {}

    @SubscribeEvent
    public static void append(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        if (!stack.is(ItemRegistry.ITEM_ANCIENT_PROJECTOR.get())) return;

        ResourceLocation id = AncientProjectorBlockEntity.DEFAULT_RECORDING;
        if (stack.hasTag() && stack.getTag().contains("Recording")) {
            ResourceLocation parsed = ResourceLocation.tryParse(
                    stack.getTag().getString("Recording"));
            if (parsed != null) id = parsed;
        }
        HologramRecordingResourceLoader.Recording recording =
                HologramRecordingResourceLoader.get(id);
        List<Component> lines = event.getToolTip();
        if (recording == null) {
            lines.add(Component.literal("Archive data unreadable")
                    .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
            return;
        }

        String archive = recording.archive();
        String title = recording.title();
        // Keep enough of the text visible to suggest an old engineering
        // archive, but obscure a few characters rather than the entire line.
        MutableComponent archiveLine = Component.literal("Archive ")
                .withStyle(ChatFormatting.GRAY);
        int visibleDigits = Math.max(1, archive.length() - 2);
        archiveLine.append(Component.literal(archive.substring(0, visibleDigits)));
        if (visibleDigits < archive.length()) {
            archiveLine.append(Component.literal(archive.substring(visibleDigits))
                    .withStyle(ChatFormatting.OBFUSCATED));
        }
        archiveLine.append(Component.literal(" - "));
        int visibleTitle = Math.max(1, title.length() - 5);
        archiveLine.append(Component.literal(title.substring(0, visibleTitle)));
        if (visibleTitle < title.length()) {
            archiveLine.append(Component.literal(title.substring(visibleTitle))
                    .withStyle(ChatFormatting.OBFUSCATED));
        }
        lines.add(archiveLine);

        lines.add(Component.literal("An ins")
                .withStyle(ChatFormatting.DARK_GRAY)
                .append(Component.literal("cription")
                        .withStyle(ChatFormatting.OBFUSCATED))
                .append(Component.literal(" is etched along its side.")));

        HologramSceneBounds.Dimensions bounds =
                HologramSceneBounds.get(recording.animation());
        if (bounds == null || bounds.width() == 0) {
            lines.add(Component.literal("?? x ?? x ??")
                    .withStyle(ChatFormatting.DARK_GRAY));
        } else {
            lines.add(Component.literal(bounds.width() + " x ")
                    .withStyle(ChatFormatting.DARK_GRAY)
                    .append(Component.literal(Integer.toString(bounds.height()))
                            .withStyle(ChatFormatting.OBFUSCATED))
                    .append(Component.literal(" x " + bounds.depth())));
        }
    }
}
