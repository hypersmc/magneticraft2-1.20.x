package com.magneticraft2.client.hologram;

import com.magneticraft2.common.hologram.AncientProjectorBlockEntity;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;

/**
 * Shared client-only transform from canonical south-facing scene coordinates
 * to Minecraft world coordinates. Must match AncientProjectorRenderer's
 * PoseStack transform EXACTLY so audio and particle effects follow a filmed
 * mechanism through all four horizontal projector facings.
 *
 * Scene local positions use block units (1.0 = one full Minecraft block),
 * and do not include the projector's physical block position or the
 * recording placement offset.
 */
public final class HologramSceneCoordinates {
    private HologramSceneCoordinates() {}

    public static Vec3 toWorld(AncientProjectorBlockEntity projector,
                               HologramRecordingResourceLoader.Recording recording,
                               Vec3 scenePosition) {
        Direction facing = projector.getBlockState().getValue(
                BlockStateProperties.HORIZONTAL_FACING);
        double radians = Math.toRadians(-facing.toYRot());
        double cosine = Math.cos(radians);
        double sine = Math.sin(radians);

        double localX = recording.offsetX() + scenePosition.x - 0.5D;
        double localZ = recording.offsetZ() + scenePosition.z - 0.5D;
        return new Vec3(
                projector.getBlockPos().getX() + 0.5D
                        + cosine * localX + sine * localZ,
                projector.getBlockPos().getY() + recording.offsetY()
                        + scenePosition.y,
                projector.getBlockPos().getZ() + 0.5D
                        - sine * localX + cosine * localZ
        );
    }
}
