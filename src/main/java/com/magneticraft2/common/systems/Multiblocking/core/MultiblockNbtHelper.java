package com.magneticraft2.common.systems.Multiblocking.core;

import net.minecraft.nbt.CompoundTag;

/**
 * Centralizes the NBT keys used by multiblock block entities.
 *
 * Bind the helper to a CompoundTag once, then write values with only:
 *   put(TagType, dataInput)
 *
 * Legacy aliases are read for compatibility with existing worlds.
 */
public final class MultiblockNbtHelper {
    public enum TagType {
        BLUEPRINT_NAME("BlueprintName", "blueprintname"),
        FORMED("Formed", "formed"),
        REPLACEMENT_MODEL("ReplacementModel", "Repacementmodel", "repacementmodel"),
        CONTROLLER("MultiblockController"),
        STRUCTURE("MultiblockStructure");

        private final String key;
        private final String[] legacyKeys;

        TagType(String key, String... legacyKeys) {
            this.key = key;
            this.legacyKeys = legacyKeys;
        }

        public String key() {
            return key;
        }

        public String[] legacyKeys() {
            return legacyKeys;
        }
    }

    private final CompoundTag tag;

    public MultiblockNbtHelper(CompoundTag tag) {
        this.tag = tag;
    }

    public void put(TagType tagType, Object dataInput) {
        if (dataInput == null) {
            return;
        }

        switch (tagType) {
            case BLUEPRINT_NAME, REPLACEMENT_MODEL -> tag.putString(tagType.key(), (String) dataInput);
            case FORMED -> tag.putBoolean(tagType.key(), (Boolean) dataInput);
            case CONTROLLER, STRUCTURE -> tag.put(tagType.key(), (CompoundTag) dataInput);
        }
    }

    public String getString(TagType tagType) {
        String key = findExistingKey(tagType);
        return key == null ? "" : tag.getString(key);
    }

    public boolean getBoolean(TagType tagType) {
        String key = findExistingKey(tagType);
        return key != null && tag.getBoolean(key);
    }

    public CompoundTag getCompound(TagType tagType) {
        String key = findExistingKey(tagType);
        return key == null ? new CompoundTag() : tag.getCompound(key);
    }

    public boolean contains(TagType tagType) {
        return findExistingKey(tagType) != null;
    }

    private String findExistingKey(TagType tagType) {
        if (tag.contains(tagType.key())) {
            return tagType.key();
        }

        for (String legacyKey : tagType.legacyKeys()) {
            if (tag.contains(legacyKey)) {
                return legacyKey;
            }
        }

        return null;
    }
}
