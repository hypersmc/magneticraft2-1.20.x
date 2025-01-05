package com.magneticraft2.common.systems.GEAR;

import com.magneticraft2.common.block.general.GearBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author JumpWatch on 27-12-2024
 * @Project mgc2-1.20
 * v1.0.0
 */
import java.util.ArrayList;
import java.util.List;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.core.BlockPos;

public class GearNetworkManager {
    private static GearNetworkManager instance;
    private final Map<BlockPos, GearNode> gears = new HashMap<>();
    private GearNetworkManager() {}
    // Get the singleton instance
    public static GearNetworkManager getInstance() {
        if (instance == null) {
            instance = new GearNetworkManager();
        }
        return instance;
    }
    // Add a gear to the network
    public void addGear(GearNode gear) {
        if (gear == null) return;
        gears.put(gear.getPosition(), gear);
        updateNetwork();
    }

    // Remove a gear from the network
    public void removeGear(BlockPos position) {
        gears.remove(position);
        updateNetwork();
    }

    // Update the network to propagate power and assign direction multipliers
    public void updateNetwork() {
        for (GearNode gear : gears.values()) {
            BlockPos pos = gear.getPosition();
            gear.setDirectionMultiplier(1); // Reset direction multiplier

            // Find power source for the gear
            BlockPos source = findPowerSource(pos);
            gear.setSourcePos(source);

            // Assign direction multiplier to neighbors
            propagateDirection(pos, 1);
        }
    }

    // Find a power source (neighboring moving gear or handle) for the given position
    private BlockPos findPowerSource(BlockPos pos) {
        for (BlockPos neighbor : getConnectedGears(pos)) {
            GearNode neighborNode = gears.get(neighbor);
            if (neighborNode != null && neighborNode.getSpeed() != 0) {
                return neighbor; // Found a moving neighbor as the power source
            }
        }
        return null; // No power source found
    }

    // Propagate direction multiplier across connected gears
    private void propagateDirection(BlockPos pos, int direction) {
        for (BlockPos neighbor : getConnectedGears(pos)) {
            GearNode neighborNode = gears.get(neighbor);
            if (neighborNode != null && neighborNode.getDirectionMultiplier() == 0) {
                neighborNode.setDirectionMultiplier(-direction); // Alternate direction
                propagateDirection(neighbor, -direction);
            }
        }
    }

    // Get positions of connected gears (example: adjacent positions)
    private Iterable<BlockPos> getConnectedGears(BlockPos pos) {
        return BlockPos.betweenClosed(pos.offset(-1, 0, 0), pos.offset(1, 0, 0));
    }

    // Get a gear by its position
    public GearNode getGear(BlockPos position) {
        return gears.get(position);
    }

    // Debugging: Print the network state
    public void printNetworkState() {
        System.out.println("Gear Network State:");
        for (GearNode gear : gears.values()) {
            System.out.println("Gear at " + gear.getPosition() +
                    " | Speed: " + gear.getSpeed() +
                    " | Torque: " + gear.getTorque() +
                    " | Direction Multiplier: " + gear.getDirectionMultiplier() +
                    " | Source: " + gear.getSourcePos());
        }
    }
}
