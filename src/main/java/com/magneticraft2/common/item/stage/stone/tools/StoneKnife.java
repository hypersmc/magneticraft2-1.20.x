package com.magneticraft2.common.item.stage.stone.tools;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;

/**
 * @author JumpWatch on 16-07-2023
 * @Project mgc2-1.20
 * v1.0.0
 */
public class StoneKnife extends SwordItem {
    public StoneKnife(Tier p_43269_, int p_43270_, float p_43271_, Properties p_43272_) {
        super(p_43269_, p_43270_, p_43271_, p_43272_);
    }

    @Override
    public boolean isValidRepairItem(ItemStack p_43311_, ItemStack p_43312_) {
        return p_43312_.getItem() == Items.COBBLESTONE.asItem();
    }
}
