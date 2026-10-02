package com.magneticraft2.common.systems.GEAR;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Shared physical description of an open pulley belt.
 *
 * Rendering, entity collision, and future item transport all consume this same path so
 * they cannot slowly disagree about where the belt actually exists.
 */
public final class BeltPath {
    public static final double BELT_CLEARANCE = 0.030D;
    public static final double RENDER_HALF_WIDTH = 0.085D;
    public static final double RENDER_HALF_THICKNESS = 0.018D;

    // Collision is deliberately a little thicker than the pixels players see. A paper-thin
    // collision surface is prone to tunnelling at sprint speed, while this is still far below
    // a full block and remains easy to jump over or walk beneath.
    public static final double COLLISION_HALF_WIDTH = 0.095D;
    public static final double COLLISION_HALF_THICKNESS = 0.045D;
    public static final double STRAIGHT_COLLISION_SLICE_LENGTH = 1.00D;
    public static final double DIAGONAL_COLLISION_SLICE_LENGTH = 0.45D;

    private static final int ARC_SEGMENTS_PER_HALF_TURN = 16;

    private final BlockPos startPulley;
    private final BlockPos endPulley;
    private final Direction.Axis axis;
    private final List<Segment> segments;
    private final AABB bounds;
    private final double totalLength;

    private BeltPath(BlockPos startPulley,
                     BlockPos endPulley,
                     Direction.Axis axis,
                     List<Segment> segments,
                     AABB bounds,
                     double totalLength) {
        this.startPulley = startPulley;
        this.endPulley = endPulley;
        this.axis = axis;
        this.segments = Collections.unmodifiableList(segments);
        this.bounds = bounds;
        this.totalLength = totalLength;
    }

    @Nullable
    public static BeltPath create(BlockPos startPulley,
                                  BlockPos endPulley,
                                  Direction.Axis axis,
                                  double startPulleyRadius,
                                  double endPulleyRadius) {
        if (startPulley == null || endPulley == null || axis == null || startPulley.equals(endPulley)) {
            return null;
        }

        Vec3 startCenter = Vec3.atCenterOf(startPulley);
        Vec3 endCenter = Vec3.atCenterOf(endPulley);
        Vec3 centerLine = endCenter.subtract(startCenter);
        double centerDistance = centerLine.length();
        if (centerDistance < 0.0001D) {
            return null;
        }

        Vec3 runDirection = centerLine.scale(1.0D / centerDistance);
        Vec3 axisVector = axisVector(axis);
        Vec3 sideDirection = axisVector.cross(runDirection);
        if (sideDirection.lengthSqr() < 0.0001D) {
            return null;
        }
        sideDirection = sideDirection.normalize();

        double startRadius = startPulleyRadius + BELT_CLEARANCE;
        double endRadius = endPulleyRadius + BELT_CLEARANCE;

        double radiusDifference = startRadius - endRadius;
        if (Math.abs(radiusDifference) >= centerDistance) {
            return null;
        }

        // True external tangent for unequal pulley radii.
        double tangentRunComponent = radiusDifference / centerDistance;
        double tangentSideComponent = Math.sqrt(
                Math.max(0.0D, 1.0D - tangentRunComponent * tangentRunComponent)
        );

        Vec3 topRadial = runDirection.scale(tangentRunComponent)
                .add(sideDirection.scale(tangentSideComponent))
                .normalize();
        Vec3 bottomRadial = runDirection.scale(tangentRunComponent)
                .subtract(sideDirection.scale(tangentSideComponent))
                .normalize();

        Vec3 startTop = startCenter.add(topRadial.scale(startRadius));
        Vec3 endTop = endCenter.add(topRadial.scale(endRadius));
        Vec3 startBottom = startCenter.add(bottomRadial.scale(startRadius));
        Vec3 endBottom = endCenter.add(bottomRadial.scale(endRadius));

        List<Segment> segments = new ArrayList<>();
        double distance = 0.0D;

        distance = appendSegment(
                segments,
                startTop,
                endTop,
                axisVector,
                topRadial,
                SegmentType.STRAIGHT,
                distance
        );

        distance = appendArc(
                segments,
                endCenter,
                topRadial,
                bottomRadial,
                runDirection,
                runDirection,
                sideDirection,
                axisVector,
                endRadius,
                distance
        );

        distance = appendSegment(
                segments,
                endBottom,
                startBottom,
                axisVector,
                bottomRadial,
                SegmentType.STRAIGHT,
                distance
        );

        distance = appendArc(
                segments,
                startCenter,
                bottomRadial,
                topRadial,
                runDirection.scale(-1.0D),
                runDirection,
                sideDirection,
                axisVector,
                startRadius,
                distance
        );

        if (segments.isEmpty()) {
            return null;
        }

        AABB bounds = segmentBounds(
                segments.get(0).from(),
                segments.get(0).to(),
                segments.get(0).widthDirection(),
                segments.get(0).thicknessDirection(),
                COLLISION_HALF_WIDTH,
                COLLISION_HALF_THICKNESS
        );

        for (int i = 1; i < segments.size(); i++) {
            Segment segment = segments.get(i);
            bounds = union(
                    bounds,
                    segmentBounds(
                            segment.from(),
                            segment.to(),
                            segment.widthDirection(),
                            segment.thicknessDirection(),
                            COLLISION_HALF_WIDTH,
                            COLLISION_HALF_THICKNESS
                    )
            );
        }

        return new BeltPath(
                startPulley.immutable(),
                endPulley.immutable(),
                axis,
                segments,
                bounds,
                distance
        );
    }

    public BlockPos startPulley() {
        return startPulley;
    }

    public BlockPos endPulley() {
        return endPulley;
    }

    public Direction.Axis axis() {
        return axis;
    }

    public List<Segment> segments() {
        return segments;
    }

    public AABB bounds() {
        return bounds;
    }

    public double totalLength() {
        return totalLength;
    }

    /**
     * Axis-aligned collision boxes for the two exposed straight runs. These are used by
     * native collision proxy entities so Minecraft's normal entity-collision pipeline does
     * the actual movement resolution.
     */
    public List<AABB> collisionBoxes() {
        List<AABB> result = new ArrayList<>();

        for (Segment segment : segments) {
            if (segment.type() != SegmentType.STRAIGHT) {
                continue;
            }

            Vec3 delta = segment.to().subtract(segment.from());
            double length = delta.length();
            if (length < 0.00001D) {
                continue;
            }

            Vec3 direction = delta.scale(1.0D / length);

            // Keep collision proxies local. Large entity AABBs spanning an entire belt are
            // surprisingly expensive and also interact poorly with Minecraft's section-based
            // entity lookup. One-block sections are cheap enough for straight transmission
            // belts while still keeping every collider near the space it actually occupies.
            //
            // Sloped/diagonal belts need finer sections because an AABB cannot rotate. Short
            // boxes create a much better staircase approximation and allow entities to stand
            // on the visible slope instead of colliding with a huge rectangular envelope.
            double sliceLength = isNearlyAxisAligned(direction)
                    ? STRAIGHT_COLLISION_SLICE_LENGTH
                    : DIAGONAL_COLLISION_SLICE_LENGTH;

            int slices = Math.max(1, (int) Math.ceil(length / sliceLength));

            for (int i = 0; i < slices; i++) {
                double fromDistance = length * i / slices;
                double toDistance = length * (i + 1) / slices;
                Vec3 sliceStart = segment.from().add(direction.scale(fromDistance));
                Vec3 sliceEnd = segment.from().add(direction.scale(toDistance));

                result.add(segmentBounds(
                        sliceStart,
                        sliceEnd,
                        segment.widthDirection(),
                        segment.thicknessDirection(),
                        COLLISION_HALF_WIDTH,
                        COLLISION_HALF_THICKNESS
                ));
            }
        }

        return result;
    }

    /**
     * Debug/helper representation of the native collision boxes as VoxelShapes.
     */
    public List<VoxelShape> collisionShapes(AABB query) {
        if (query == null || !bounds.intersects(query)) {
            return List.of();
        }

        List<VoxelShape> result = new ArrayList<>();
        for (AABB box : collisionBoxes()) {
            if (box.intersects(query)) {
                result.add(Shapes.create(box));
            }
        }
        return result;
    }

    /**
     * Future item transport hook: sample an exact world position/tangent by linear distance
     * along the continuous belt loop.
     */
    public Sample sample(double distance) {
        double wrappedDistance = positiveModulo(distance, totalLength);

        for (Segment segment : segments) {
            if (wrappedDistance <= segment.endDistance() + 0.000001D) {
                double length = segment.length();
                double t = length <= 0.000001D
                        ? 0.0D
                        : (wrappedDistance - segment.startDistance()) / length;
                t = Math.max(0.0D, Math.min(1.0D, t));

                Vec3 tangent = segment.to().subtract(segment.from());
                if (tangent.lengthSqr() > 0.000001D) {
                    tangent = tangent.normalize();
                }

                return new Sample(
                        segment.from().lerp(segment.to(), t),
                        tangent,
                        segment.widthDirection(),
                        segment.thicknessDirection(),
                        wrappedDistance,
                        segment.type()
                );
            }
        }

        Segment last = segments.get(segments.size() - 1);
        return new Sample(
                last.to(),
                last.to().subtract(last.from()).normalize(),
                last.widthDirection(),
                last.thicknessDirection(),
                wrappedDistance,
                last.type()
        );
    }

    /**
     * Future item pickup/attachment hook: find the nearest point on the belt and return its
     * linear distance coordinate.
     */
    public Projection project(Vec3 worldPoint) {
        double bestDistanceSquared = Double.POSITIVE_INFINITY;
        double bestPathDistance = 0.0D;
        Vec3 bestPoint = segments.get(0).from();

        for (Segment segment : segments) {
            Vec3 ab = segment.to().subtract(segment.from());
            double lengthSquared = ab.lengthSqr();
            double t = lengthSquared <= 0.000001D
                    ? 0.0D
                    : worldPoint.subtract(segment.from()).dot(ab) / lengthSquared;
            t = Math.max(0.0D, Math.min(1.0D, t));

            Vec3 point = segment.from().add(ab.scale(t));
            double distanceSquared = point.distanceToSqr(worldPoint);
            if (distanceSquared < bestDistanceSquared) {
                bestDistanceSquared = distanceSquared;
                bestPoint = point;
                bestPathDistance = segment.startDistance() + segment.length() * t;
            }
        }

        return new Projection(bestPoint, bestPathDistance, bestDistanceSquared);
    }

    private static double appendArc(List<Segment> segments,
                                    Vec3 center,
                                    Vec3 startRadial,
                                    Vec3 endRadial,
                                    Vec3 wantedOuterDirection,
                                    Vec3 runBasis,
                                    Vec3 sideBasis,
                                    Vec3 axisVector,
                                    double radius,
                                    double distance) {
        double startAngle = Math.atan2(startRadial.dot(sideBasis), startRadial.dot(runBasis));
        double endAngle = Math.atan2(endRadial.dot(sideBasis), endRadial.dot(runBasis));

        double shortDelta = wrapRadians(endAngle - startAngle);
        double longDelta = shortDelta >= 0.0D
                ? shortDelta - Math.PI * 2.0D
                : shortDelta + Math.PI * 2.0D;

        double shortScore = radialAt(
                startAngle + shortDelta * 0.5D,
                runBasis,
                sideBasis
        ).dot(wantedOuterDirection);

        double longScore = radialAt(
                startAngle + longDelta * 0.5D,
                runBasis,
                sideBasis
        ).dot(wantedOuterDirection);

        double delta = longScore > shortScore ? longDelta : shortDelta;
        int arcSegments = Math.max(
                4,
                (int) Math.ceil(ARC_SEGMENTS_PER_HALF_TURN * Math.abs(delta) / Math.PI)
        );

        Vec3 previousRadial = startRadial;
        Vec3 previousPoint = center.add(previousRadial.scale(radius));

        for (int i = 1; i <= arcSegments; i++) {
            double t = i / (double) arcSegments;
            Vec3 radial = radialAt(
                    startAngle + delta * t,
                    runBasis,
                    sideBasis
            );
            Vec3 nextPoint = center.add(radial.scale(radius));

            Vec3 thicknessDirection = previousRadial.add(radial);
            if (thicknessDirection.lengthSqr() < 0.0001D) {
                thicknessDirection = radial;
            } else {
                thicknessDirection = thicknessDirection.normalize();
            }

            distance = appendSegment(
                    segments,
                    previousPoint,
                    nextPoint,
                    axisVector,
                    thicknessDirection,
                    SegmentType.ARC,
                    distance
            );

            previousRadial = radial;
            previousPoint = nextPoint;
        }

        return distance;
    }

    private static double appendSegment(List<Segment> segments,
                                        Vec3 from,
                                        Vec3 to,
                                        Vec3 widthDirection,
                                        Vec3 thicknessDirection,
                                        SegmentType type,
                                        double startDistance) {
        double length = from.distanceTo(to);
        if (length < 0.000001D) {
            return startDistance;
        }

        double endDistance = startDistance + length;
        segments.add(new Segment(
                from,
                to,
                widthDirection.normalize(),
                thicknessDirection.normalize(),
                type,
                startDistance,
                endDistance
        ));
        return endDistance;
    }

    private static boolean isNearlyAxisAligned(Vec3 direction) {
        double dominant = Math.max(
                Math.abs(direction.x),
                Math.max(Math.abs(direction.y), Math.abs(direction.z))
        );
        return dominant >= 0.985D;
    }

    private static AABB segmentBounds(Vec3 from,
                                      Vec3 to,
                                      Vec3 widthDirection,
                                      Vec3 thicknessDirection,
                                      double halfWidth,
                                      double halfThickness) {
        Vec3 width = widthDirection.normalize().scale(halfWidth);
        Vec3 thickness = thicknessDirection.normalize().scale(halfThickness);

        double extentX = Math.abs(width.x) + Math.abs(thickness.x);
        double extentY = Math.abs(width.y) + Math.abs(thickness.y);
        double extentZ = Math.abs(width.z) + Math.abs(thickness.z);

        return new AABB(
                Math.min(from.x, to.x) - extentX,
                Math.min(from.y, to.y) - extentY,
                Math.min(from.z, to.z) - extentZ,
                Math.max(from.x, to.x) + extentX,
                Math.max(from.y, to.y) + extentY,
                Math.max(from.z, to.z) + extentZ
        );
    }

    private static AABB union(AABB first, AABB second) {
        return new AABB(
                Math.min(first.minX, second.minX),
                Math.min(first.minY, second.minY),
                Math.min(first.minZ, second.minZ),
                Math.max(first.maxX, second.maxX),
                Math.max(first.maxY, second.maxY),
                Math.max(first.maxZ, second.maxZ)
        );
    }

    private static Vec3 axisVector(Direction.Axis axis) {
        return switch (axis) {
            case X -> new Vec3(1.0D, 0.0D, 0.0D);
            case Y -> new Vec3(0.0D, 1.0D, 0.0D);
            case Z -> new Vec3(0.0D, 0.0D, 1.0D);
        };
    }

    private static Vec3 radialAt(double angle, Vec3 runBasis, Vec3 sideBasis) {
        return runBasis.scale(Math.cos(angle))
                .add(sideBasis.scale(Math.sin(angle)))
                .normalize();
    }

    private static double wrapRadians(double radians) {
        double wrapped = radians % (Math.PI * 2.0D);
        if (wrapped > Math.PI) {
            wrapped -= Math.PI * 2.0D;
        } else if (wrapped < -Math.PI) {
            wrapped += Math.PI * 2.0D;
        }
        return wrapped;
    }

    private static double positiveModulo(double value, double divisor) {
        if (divisor <= 0.000001D) {
            return 0.0D;
        }
        double result = value % divisor;
        return result < 0.0D ? result + divisor : result;
    }

    public enum SegmentType {
        STRAIGHT,
        ARC
    }

    public record Segment(Vec3 from,
                          Vec3 to,
                          Vec3 widthDirection,
                          Vec3 thicknessDirection,
                          SegmentType type,
                          double startDistance,
                          double endDistance) {
        public double length() {
            return endDistance - startDistance;
        }
    }

    public record Sample(Vec3 position,
                         Vec3 tangent,
                         Vec3 widthDirection,
                         Vec3 thicknessDirection,
                         double distance,
                         SegmentType type) {
    }

    public record Projection(Vec3 position,
                             double distance,
                             double distanceSquared) {
    }
}
