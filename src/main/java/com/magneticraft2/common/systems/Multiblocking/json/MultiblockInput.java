package com.magneticraft2.common.systems.Multiblocking.json;

import java.util.List;

/**
 * @author JumpWatch on 10-06-2023
 * @Project mgc2-1.20
 * v1.0.0
 */
public class MultiblockInput {
    private final int slot;
    private final String type;
    private final List<String> allowedItems;
    private final int size;

    public MultiblockInput(int slot, String type, List<String> allowedItems, int size) {
        this.slot = slot;
        this.type = type;
        this.allowedItems = allowedItems;
        this.size = size;
    }

    public int getSlot() {
        return slot;
    }

    public String getType() {
        return type;
    }

    public List<String> getAllowedItems() {
        return allowedItems;
    }

    public int getSize() {
        return size;
    }
}
