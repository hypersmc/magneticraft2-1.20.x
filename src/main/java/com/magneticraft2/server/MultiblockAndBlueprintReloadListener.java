package com.magneticraft2.server;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.magneticraft2.common.systems.Blueprint.core.BlueprintManager;
import com.magneticraft2.common.systems.Multiblocking.json.MultiblockManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

import java.util.Map;

import static com.magneticraft2.common.magneticraft2.MOD_ID;

/**
 * @author JumpWatch on 26-12-2024
 * @Project mgc2-1.20
 * v1.0.0
 */
public class MultiblockAndBlueprintReloadListener extends SimpleJsonResourceReloadListener {
    public static final String FOLDER_NAME = "multiblocks";

    public MultiblockAndBlueprintReloadListener() {
        super(new Gson(), FOLDER_NAME);
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> pObject, ResourceManager pResourceManager, ProfilerFiller pProfiler) {
        MultiblockManager.loadMultiblocks(MOD_ID, pResourceManager);
        BlueprintManager.loadBlueprints(MOD_ID, pResourceManager);
    }
}
