package com.magneticraft2.common.item.general;

import net.minecraft.world.item.Item;

/**
 * @author JumpWatch on 18-07-2023
 * @Project mgc2-1.20
 * v1.0.0
 */
public class pebble extends Item {
    public pebble() {
        super(new Properties().stacksTo(64).setNoRepair());
    }
}
