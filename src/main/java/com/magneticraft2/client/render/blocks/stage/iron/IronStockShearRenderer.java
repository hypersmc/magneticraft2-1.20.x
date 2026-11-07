package com.magneticraft2.client.render.blocks.stage.iron;

import com.magneticraft2.common.block.stage.iron.IronStockShearBlock;
import com.magneticraft2.common.blockentity.stage.copper.MechanicalInputModuleBlockEntity;
import com.magneticraft2.common.blockentity.stage.iron.IronStockShearBlockEntity;
import com.magneticraft2.common.registry.registers.BlockRegistry;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.event.ModelEvent;

/**
 * A real shaft -> equal pulleys -> crank -> fixed-length link -> guided blade.
 * Static table, bearings and guides are ordinary V2 model parts. All dimensions
 * are model pixels; moving components are translated/rotated, never stretched.
 */
public final class IronStockShearRenderer
        implements BlockEntityRenderer<IronStockShearBlockEntity> {
    private static final ResourceLocation BLADE =
            new ResourceLocation("magneticraft2", "multiblock/iron_stock_shear_blade");
    private static final ResourceLocation PULLEY = model("pulley");
    private static final ResourceLocation SHAFT = model("shaft");
    private static final ResourceLocation COUPLING = model("coupling");
    private static final ResourceLocation CRANK = model("crank");
    private static final ResourceLocation LINK = model("link");
    private static final ResourceLocation BELT = model("belt");

    private static ResourceLocation model(String part) {
        return new ResourceLocation("magneticraft2", "multiblock/iron_stock_shear_" + part);
    }

    /** Called by the existing client setup, including resource reloads. */
    public static void registerModels(ModelEvent.RegisterAdditional event) {
        // The existing setup already explicitly registers BLADE.
        event.register(PULLEY);
        event.register(SHAFT);
        event.register(COUPLING);
        event.register(CRANK);
        event.register(LINK);
        event.register(BELT);
    }

    public IronStockShearRenderer(BlockEntityRendererProvider.Context ignored) {}

    @Override public int getViewDistance() { return 64; }

    @Override public void render(IronStockShearBlockEntity shear, float partial,
            PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        if (!shear.isFormed()) return;
        Direction facing = shear.getBlockState().getValue(IronStockShearBlock.FACING);
        float yaw = switch (facing) {
            case WEST -> -90F;
            case NORTH -> 180F;
            case EAST -> 90F;
            default -> 0F;
        };
        // One module lookup per frame. The Gear V2 angle itself stops when the
        // physical network stops. Do not snap to zero when a batch completes.
        // Like a flywheel-driven shear, the unloaded mechanism can free-cycle;
        // only server-authorised processing consumes stock or wears the blade.
        MechanicalInputModuleBlockEntity input = shear.getMechanicalInput();
        float angle = input == null ? 0F : input.getVisualRotationDegrees(partial);
        // The shared input renderer rotates about the positive WORLD axis.
        // Our NORTH/WEST pose reverses local +Z; compensate exactly once.
        if (facing == Direction.NORTH || facing == Direction.WEST) angle = -angle;
        double radians = Math.toRadians(angle);
        double pinX = IronShearMotion.pinX(radians);
        double pinY = IronShearMotion.pinY(radians);
        double sliderY = IronShearMotion.sliderY(pinX, pinY);

        Minecraft mc = Minecraft.getInstance();
        BlockRenderDispatcher dispatcher = mc.getBlockRenderer();
        // Resolve each model once from the current manager, so F3+T cannot
        // leave stale baked-model references. The two pulleys share one model.
        BakedModel pulley = mc.getModelManager().getModel(PULLEY);
        pose.pushPose();
        pose.translate(.5D, 0D, .5D);
        pose.mulPose(Axis.YP.rotationDegrees(yaw));
        pose.translate(-.5D, 0D, -.5D);

        draw(dispatcher, mc.getModelManager().getModel(BELT), pose, buffers,
                light, overlay, 8, 8, -15, 0F, 8, 8, 8);
        draw(dispatcher, pulley, pose, buffers,
                light, overlay, 8, 8, -15, angle, 8, 8, 8);
        draw(dispatcher, pulley, pose, buffers,
                light, overlay, 8, 24, -15, angle, 8, 8, 8);
        draw(dispatcher, mc.getModelManager().getModel(COUPLING), pose, buffers,
                light, overlay, 8, 8, -16, angle, 8, 8, 0);
        draw(dispatcher, mc.getModelManager().getModel(SHAFT), pose, buffers,
                light, overlay, 8, 24, -15, angle, 8, 8, 0);
        draw(dispatcher, mc.getModelManager().getModel(CRANK), pose, buffers,
                light, overlay, 8, 24, -8.5, angle, 8, 8, 8);
        draw(dispatcher, mc.getModelManager().getModel(LINK), pose, buffers,
                light, overlay, pinX, pinY, -7.5,
                IronShearMotion.linkDegrees(pinX, pinY, sliderY), 8, 0, 8);
        draw(dispatcher, mc.getModelManager().getModel(BLADE), pose, buffers,
                light, overlay, 8, sliderY, -7.5, 0F, 8, 0, 8);

        ItemStack workpiece = shear.getWorkpiece();
        if (!workpiece.isEmpty()) {
            ItemRenderer itemRenderer = mc.getItemRenderer();
            pose.pushPose();
            // Full-size real rod, resting on the y=9 anvil and aligned with
            // the feed guide. The blade crosses it above the open bed slot.
            pose.translate(8D / 16D, 10D / 16D, -6D / 16D);
            itemRenderer.render(workpiece, ItemDisplayContext.NONE, false,
                    pose, buffers, light, overlay,
                    itemRenderer.getModel(workpiece, shear.getLevel(), null, 889));
            pose.popPose();
        }
        pose.popPose();
    }

    private static void draw(BlockRenderDispatcher dispatcher, BakedModel model,
            PoseStack pose, MultiBufferSource buffers, int light, int overlay,
            double x, double y, double z, float angle,
            double originX, double originY, double originZ) {
        pose.pushPose();
        pose.translate(x / 16D, y / 16D, z / 16D);
        pose.mulPose(Axis.ZP.rotationDegrees(angle));
        pose.translate(-originX / 16D, -originY / 16D, -originZ / 16D);
        dispatcher.getModelRenderer().renderModel(pose.last(),
                buffers.getBuffer(RenderType.cutout()),
                BlockRegistry.IRON_STOCK_SHEAR.get().defaultBlockState(),
                model, 1F, 1F, 1F, light, overlay);
        pose.popPose();
    }
}
