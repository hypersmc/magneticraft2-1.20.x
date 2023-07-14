package com.magneticraft2.common.systems.Multiblocking.json;

import java.util.List;

/**
 * @author JumpWatch on 10-06-2023
 * @Project mgc2-1.20
 * v1.0.0
 */
public class MultiblockSettings {
    private final String replaceWhenFormed;
    private final boolean hasInventory;
    private final boolean hasPower;
    private final boolean hasFuel;
    private final boolean hasFluid;
    private final List<MultiblockInput> inputs;
    private final List<MultiblockOutput> outputs;

    public MultiblockSettings(String replaceWhenFormed, boolean hasInventory, boolean hasPower, boolean hasFuel, boolean hasFluid, List<MultiblockInput> inputs, List<MultiblockOutput> outputs) {
        this.replaceWhenFormed = replaceWhenFormed;
        this.hasInventory = hasInventory;
        this.hasPower = hasPower;
        this.hasFuel = hasFuel;
        this.hasFluid = hasFluid;
        this.inputs = inputs;
        this.outputs = outputs;
    }

    public String getReplaceWhenFormed() {
        return replaceWhenFormed;
    }

    public boolean hasInventory() {
        return hasInventory;
    }

    public boolean hasPower() {
        return hasPower;
    }

    public boolean hasFuel() {
        return hasFuel;
    }

    public boolean hasFluid() {
        return hasFluid;
    }

    public List<MultiblockInput> getInputs() {
        return inputs;
    }

    public List<MultiblockOutput> getOutputs() {
        return outputs;
    }
}