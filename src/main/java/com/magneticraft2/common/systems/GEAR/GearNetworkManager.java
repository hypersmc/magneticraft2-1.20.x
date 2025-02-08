package com.magneticraft2.common.systems.GEAR;

import com.magneticraft2.common.blockentity.stage.copper.LargeGearBlockEntity_wood;
import com.magneticraft2.common.blockentity.stage.copper.LargeGearWithHandleBlockEntity_wood;
import com.magneticraft2.common.blockentity.stage.copper.MediumGearBlockEntity_wood;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.magneticraft2.common.magneticraft2.LOGGER;

/**
 * @author JumpWatch on 27-12-2024
 * @Project mgc2-1.20
 * v1.0.0
 */

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
    public void addGear(GearNode gear, Level level) {
        if (gear == null) return;
        gears.put(gear.getPosition(), gear);
        updateNetwork(level);
    }

    // Remove a gear from the network
    public void removeGear(BlockPos position, Level level) {
        gears.remove(position);
        updateNetwork(level);
    }

    // Update the network to propagate power and assign direction multipliers
    public void updateNetwork(Level level) {
        // Temporary map to hold updated speeds for all gears
        Map<BlockPos, Float> updatedSpeeds = new HashMap<>();

        // Loop through each gear in the network
        for (GearNode gear : gears.values()) {
            BlockPos pos = gear.getPosition();
            float currentSpeed = gear.getSpeed();

            // Reset direction multiplier
            gear.setDirectionMultiplier(1);

            // Find the power source for this gear
            BlockPos source = findPowerSource(pos, level);
            gear.setSourcePos(source);

            // If the gear has a speed, propagate it to its neighbors
            if (currentSpeed > 0) {
                for (BlockPos neighborPos : getConnectedGears(pos, level)) {
                    GearNode neighbor = gears.get(neighborPos);
                    if (neighbor != null) {
                        // Propagate speed to the neighbor, considering direction
                        float propagatedSpeed = currentSpeed * gear.getDirectionMultiplier();
                        int neighborDirection = -gear.getDirectionMultiplier(); // Alternate direction

                        // Update the neighbor's speed and direction multiplier
                        updatedSpeeds.put(neighborPos, Math.max(updatedSpeeds.getOrDefault(neighborPos, 0f), propagatedSpeed));
                        neighbor.setDirectionMultiplier(neighborDirection);

                    }
                }
            }

            // Apply speed decay to this gear
            float decayRate = 0.01f; // Example decay rate
            updatedSpeeds.put(pos, Math.max(0, currentSpeed - decayRate));
        }

        // Apply the updated speeds to all gears
        for (Map.Entry<BlockPos, Float> entry : updatedSpeeds.entrySet()) {
            GearNode gear = gears.get(entry.getKey());
            if (gear != null) {
                gear.setSpeed(entry.getValue());
            }
        }
    }




    // Find a power source (neighboring moving gear or handle) for the given position
    private BlockPos findPowerSource(BlockPos pos, Level level) {
        for (BlockPos neighbor : getConnectedGears(pos, level)) {
            GearNode neighborNode = gears.get(neighbor);
            if (neighborNode != null && neighborNode.getSpeed() != 0) {
                return neighbor; // Found a moving neighbor as the power source
            }
        }
        return null; // No power source found
    }

    // Propagate direction multiplier across connected gears
    private void propagateDirection(BlockPos pos, int direction, Level level) {
        for (BlockPos neighbor : getConnectedGears(pos, level)) {
            GearNode neighborNode = gears.get(neighbor);
            if (neighborNode != null && neighborNode.getDirectionMultiplier() == 0) {
                neighborNode.setDirectionMultiplier(-direction); // Alternate direction
                propagateDirection(neighbor, -direction, level);
            }
        }
    }

    // Get positions of connected gears (example: adjacent positions)
    private List<BlockPos> getConnectedGears(BlockPos pos, Level level) {
        List<BlockPos> connected = new ArrayList<>();

        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof LargeGearWithHandleBlockEntity_wood || blockEntity instanceof LargeGearBlockEntity_wood) {
            // For large gears, allow diagonal connections only
            BlockPos[] diagonalOffsets = {
                    pos.offset(1, 0, 1),   // Top-right corner
                    pos.offset(1, 0, -1),  // Bottom-right corner
                    pos.offset(-1, 0, 1),  // Top-left corner
                    pos.offset(-1, 0, -1)  // Bottom-left corner
            };

            for (BlockPos offset : diagonalOffsets) {
                if (gears.containsKey(offset)) {
                    connected.add(offset);
                }
            }
        } else if (blockEntity instanceof MediumGearBlockEntity_wood) {
            // For medium gears, allow connections in all orthogonal directions
            BlockPos[] orthogonalOffsets = {
                    pos.offset(1, 0, 0),   // Right
                    pos.offset(-1, 0, 0),  // Left
                    pos.offset(0, 0, 1),   // Front
                    pos.offset(0, 0, -1),  // Back
                    pos.offset(0, 1, 0),   // Above
                    pos.offset(0, -1, 0)   // Below
            };

            for (BlockPos offset : orthogonalOffsets) {
                if (gears.containsKey(offset)) {
                    connected.add(offset);
                }
            }
        }

        return connected;
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
