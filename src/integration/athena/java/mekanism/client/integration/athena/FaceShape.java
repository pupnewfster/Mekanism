package mekanism.client.integration.athena;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.block.state.properties.StairsShape;

public record FaceShape(QuadrantState topLeft, QuadrantState topRight, QuadrantState bottomLeft, QuadrantState bottomRight) {

    private static final FaceShape FULL = new FaceShape(QuadrantState.FLUSH, QuadrantState.FLUSH, QuadrantState.FLUSH, QuadrantState.FLUSH);
    private static final FaceShape FULL_RECESSED = new FaceShape(QuadrantState.RECESSED, QuadrantState.RECESSED, QuadrantState.RECESSED, QuadrantState.RECESSED);
    private static final FaceShape BOTTOM_SLAB_SIDE = new FaceShape(QuadrantState.NONE, QuadrantState.NONE, QuadrantState.FLUSH, QuadrantState.FLUSH);
    private static final FaceShape TOP_SLAB_SIDE = new FaceShape(QuadrantState.FLUSH, QuadrantState.FLUSH, QuadrantState.NONE, QuadrantState.NONE);

    private static final FaceShape STAIR = new FaceShape(QuadrantState.RECESSED, QuadrantState.RECESSED, QuadrantState.FLUSH, QuadrantState.FLUSH);
    private static final FaceShape UPSIDE_DOWN_STAIR = new FaceShape(QuadrantState.FLUSH, QuadrantState.FLUSH, QuadrantState.RECESSED, QuadrantState.RECESSED);

    private static final FaceShape LEFT_STAIR = new FaceShape(QuadrantState.FLUSH, QuadrantState.RECESSED, QuadrantState.FLUSH, QuadrantState.RECESSED);
    private static final FaceShape RIGHT_STAIR = new FaceShape(QuadrantState.RECESSED, QuadrantState.FLUSH, QuadrantState.RECESSED, QuadrantState.FLUSH);

    private static final FaceShape L_SHAPE_STAIR = new FaceShape(QuadrantState.FLUSH, QuadrantState.NONE, QuadrantState.FLUSH, QuadrantState.FLUSH);
    private static final FaceShape BACKWARDS_L_SHAPE_STAIR = new FaceShape(QuadrantState.NONE, QuadrantState.FLUSH, QuadrantState.FLUSH, QuadrantState.FLUSH);
    private static final FaceShape UPSIDE_DOWN_L_SHAPE_STAIR = new FaceShape(QuadrantState.FLUSH, QuadrantState.FLUSH, QuadrantState.FLUSH, QuadrantState.NONE);
    private static final FaceShape UPSIDE_DOWN_BACKWARDS_L_SHAPE_STAIR = new FaceShape(QuadrantState.FLUSH, QuadrantState.FLUSH, QuadrantState.NONE, QuadrantState.FLUSH);

    private static final FaceShape L_SHAPE_INNER_STAIR = new FaceShape(QuadrantState.FLUSH, QuadrantState.RECESSED, QuadrantState.FLUSH, QuadrantState.FLUSH);
    private static final FaceShape BACKWARDS_L_SHAPE_INNER_STAIR = new FaceShape(QuadrantState.RECESSED, QuadrantState.FLUSH, QuadrantState.FLUSH, QuadrantState.FLUSH);
    private static final FaceShape UPSIDE_DOWN_L_SHAPE_INNER_STAIR = new FaceShape(QuadrantState.FLUSH, QuadrantState.FLUSH, QuadrantState.FLUSH, QuadrantState.RECESSED);
    private static final FaceShape UPSIDE_DOWN_BACKWARDS_L_SHAPE_INNER_STAIR = new FaceShape(QuadrantState.FLUSH, QuadrantState.FLUSH, QuadrantState.RECESSED, QuadrantState.FLUSH);

    private static final FaceShape OUTER_LEFT = new FaceShape(QuadrantState.RECESSED, QuadrantState.NONE, QuadrantState.FLUSH, QuadrantState.FLUSH);
    private static final FaceShape OUTER_RIGHT = new FaceShape(QuadrantState.NONE, QuadrantState.RECESSED, QuadrantState.FLUSH, QuadrantState.FLUSH);
    private static final FaceShape UPSIDE_DOWN_OUTER_LEFT = new FaceShape(QuadrantState.FLUSH, QuadrantState.FLUSH, QuadrantState.RECESSED, QuadrantState.NONE);
    private static final FaceShape UPSIDE_DOWN_OUTER_RIGHT = new FaceShape(QuadrantState.FLUSH, QuadrantState.FLUSH, QuadrantState.NONE, QuadrantState.RECESSED);

    private static final FaceShape OUTER_TOP_LEFT = new FaceShape(QuadrantState.FLUSH, QuadrantState.RECESSED, QuadrantState.RECESSED, QuadrantState.RECESSED);
    private static final FaceShape OUTER_TOP_RIGHT = new FaceShape(QuadrantState.RECESSED, QuadrantState.FLUSH, QuadrantState.RECESSED, QuadrantState.RECESSED);
    private static final FaceShape OUTER_BOTTOM_LEFT = new FaceShape(QuadrantState.RECESSED, QuadrantState.RECESSED, QuadrantState.FLUSH, QuadrantState.RECESSED);
    private static final FaceShape OUTER_BOTTOM_RIGHT = new FaceShape(QuadrantState.RECESSED, QuadrantState.RECESSED, QuadrantState.RECESSED, QuadrantState.FLUSH);

    private static final FaceShape INNER_TOP_LEFT = new FaceShape(QuadrantState.RECESSED, QuadrantState.FLUSH, QuadrantState.FLUSH, QuadrantState.FLUSH);
    private static final FaceShape INNER_TOP_RIGHT = new FaceShape(QuadrantState.FLUSH, QuadrantState.RECESSED, QuadrantState.FLUSH, QuadrantState.FLUSH);
    private static final FaceShape INNER_BOTTOM_LEFT = new FaceShape(QuadrantState.FLUSH, QuadrantState.FLUSH, QuadrantState.RECESSED, QuadrantState.FLUSH);
    private static final FaceShape INNER_BOTTOM_RIGHT = new FaceShape(QuadrantState.FLUSH, QuadrantState.FLUSH, QuadrantState.FLUSH, QuadrantState.RECESSED);

    //TODO: Do we want to try and allow the recessed part of stairs to connect to the side of neighboring full blocks/slabs at the proper height?
    // It would require adding more context to what connections are checked
    public static ConnectionType isCompatible(BlockState self, BlockState fromAppearance, Direction direction, ConnectionDirection connectionDirection) {
        return getFaceShape(self, direction).isCompatible(getFaceShape(fromAppearance, direction), connectionDirection);
    }

    private static ConnectionType isCompatible(QuadrantState first, QuadrantState second, QuadrantState otherFirst, QuadrantState otherSecond,
          ConnectionType firstSplitFlush, ConnectionType firstSplitRecessed, ConnectionType secondSplitFlush, ConnectionType secondSplitRecessed) {
        if (first == otherFirst && first.isPresent()) {
            if (second == otherSecond) {
                if (first.isFlush() && second.isFlush()) {
                    return ConnectionType.FLUSH;
                } else if (first.isRecessed() && second.isRecessed()) {
                    return ConnectionType.RECESSED;
                }
                assert second.isPresent();
                return ConnectionType.BOTH;
            } else if (first.isFlush()) {
                return second.isFlush() ? secondSplitFlush : ConnectionType.FLUSH;
            } else if (first.isRecessed()) {
                return second.isRecessed() ? secondSplitRecessed : ConnectionType.RECESSED;
            }
        } else if (second == otherSecond && second.isPresent()) {
            if (second.isFlush()) {
                return first.isFlush() ? firstSplitFlush : ConnectionType.FLUSH;
            } else if (second.isRecessed()) {
                return first.isRecessed() ? firstSplitRecessed : ConnectionType.RECESSED;
            }
        }
        return ConnectionType.NONE;
    }

    /// @param other Other shape
    /// @param side  Side the other shape is connecting to this shape from
    private ConnectionType isCompatible(FaceShape other, ConnectionDirection side) {
        return switch (side) {
            case DOWN -> isCompatible(bottomLeft, bottomRight, other.topLeft, other.topRight,
                  ConnectionType.BOTTOM_RIGHT_FLUSH, ConnectionType.BOTTOM_RIGHT_RECESSED, ConnectionType.BOTTOM_LEFT_FLUSH, ConnectionType.BOTTOM_LEFT_RECESSED);
            case UP -> isCompatible(topLeft, topRight, other.bottomLeft, other.bottomRight,
                  ConnectionType.TOP_RIGHT_FLUSH, ConnectionType.TOP_RIGHT_RECESSED, ConnectionType.TOP_LEFT_FLUSH, ConnectionType.TOP_LEFT_RECESSED);
            case LEFT -> isCompatible(topLeft, bottomLeft, other.topRight, other.bottomRight,
                  ConnectionType.BOTTOM_LEFT_FLUSH, ConnectionType.BOTTOM_LEFT_RECESSED, ConnectionType.TOP_LEFT_FLUSH, ConnectionType.TOP_LEFT_RECESSED);
            case RIGHT -> isCompatible(topRight, bottomRight, other.topLeft, other.bottomLeft,
                  ConnectionType.BOTTOM_RIGHT_FLUSH, ConnectionType.BOTTOM_RIGHT_RECESSED, ConnectionType.TOP_RIGHT_FLUSH, ConnectionType.TOP_RIGHT_RECESSED);
            case UP_LEFT -> {
                if (topLeft.isPresent() && topLeft == other.bottomRight) {
                    yield topLeft.isFlush() ? ConnectionType.FLUSH : ConnectionType.RECESSED;
                }
                yield ConnectionType.NONE;
            }
            case UP_RIGHT -> {
                if (topRight.isPresent() && topRight == other.bottomLeft) {
                    yield topRight.isFlush() ? ConnectionType.FLUSH : ConnectionType.RECESSED;
                }
                yield ConnectionType.NONE;
            }
            case DOWN_LEFT -> {
                if (bottomLeft.isPresent() && bottomLeft == other.topRight) {
                    yield bottomLeft.isFlush() ? ConnectionType.FLUSH : ConnectionType.RECESSED;
                }
                yield ConnectionType.NONE;
            }
            case DOWN_RIGHT -> {
                if (bottomRight.isPresent() && bottomRight == other.topLeft) {
                    yield bottomRight.isFlush() ? ConnectionType.FLUSH : ConnectionType.RECESSED;
                }
                yield ConnectionType.NONE;
            }
        };
    }

    public static FaceShape getFaceShape(BlockState state, Direction side) {
        if (state.hasProperty(SlabBlock.TYPE)) {
            SlabType type = state.getValue(SlabBlock.TYPE);
            if (type == SlabType.DOUBLE) {
                return FULL;
            } else if (side == Direction.DOWN) {
                return type == SlabType.BOTTOM ? FULL : FULL_RECESSED;
            } else if (side == Direction.UP) {
                return type == SlabType.TOP ? FULL : FULL_RECESSED;
            }
            return type == SlabType.BOTTOM ? BOTTOM_SLAB_SIDE : TOP_SLAB_SIDE;
        } else if (state.hasProperty(StairBlock.FACING) && state.hasProperty(StairBlock.HALF) && state.hasProperty(StairBlock.SHAPE)) {
            //The direction of the full-block side
            Direction stairFacing = state.getValue(StairBlock.FACING);
            if (stairFacing.getAxis().isVertical()) {//TODO: Is it worth trying to handle this?
                return FULL;
            }
            //"straight" is the default stairs shape.
            //"inner" is an "inside corner" stair shape, with two full-block and two stair-shaped side faces.
            //"outer" is an "outside corner" stair shape, with two stair-shaped and two half-block side faces.
            //"left" and "right" specify in which direction is the higher part of the step.
            StairsShape stairsShape = state.getValue(StairBlock.SHAPE);
            boolean isUpsideDown = state.getValue(StairBlock.HALF) == Half.TOP;
            if (side.getAxis().isVertical()) {
                if (isUpsideDown == (side == Direction.UP)) {
                    //If it is the same side as the base of the stairs it is a full face
                    return FULL;
                }
                return switch (stairFacing) {
                    case NORTH -> switch (stairsShape) {
                        case STRAIGHT -> isUpsideDown ? STAIR : UPSIDE_DOWN_STAIR;
                        case INNER_LEFT -> isUpsideDown ? INNER_TOP_RIGHT : INNER_BOTTOM_RIGHT;
                        case INNER_RIGHT -> isUpsideDown ? INNER_TOP_LEFT : INNER_BOTTOM_LEFT;
                        case OUTER_LEFT -> isUpsideDown ? OUTER_BOTTOM_LEFT : OUTER_TOP_LEFT;
                        case OUTER_RIGHT -> isUpsideDown ? OUTER_BOTTOM_RIGHT : OUTER_TOP_RIGHT;
                    };
                    case EAST -> switch (stairsShape) {
                        case STRAIGHT -> RIGHT_STAIR;
                        case INNER_LEFT -> isUpsideDown ? INNER_TOP_LEFT : INNER_BOTTOM_LEFT;
                        case INNER_RIGHT -> isUpsideDown ? INNER_BOTTOM_LEFT : INNER_TOP_LEFT;
                        case OUTER_LEFT -> isUpsideDown ? OUTER_BOTTOM_RIGHT : OUTER_TOP_RIGHT;
                        case OUTER_RIGHT -> isUpsideDown ? OUTER_TOP_RIGHT : OUTER_BOTTOM_RIGHT;
                    };
                    case SOUTH -> switch (stairsShape) {
                        case STRAIGHT -> isUpsideDown ? UPSIDE_DOWN_STAIR : STAIR;
                        case INNER_LEFT -> isUpsideDown ? INNER_BOTTOM_LEFT : INNER_TOP_LEFT;
                        case INNER_RIGHT -> isUpsideDown ? INNER_BOTTOM_RIGHT : INNER_TOP_RIGHT;
                        case OUTER_LEFT -> isUpsideDown ? OUTER_TOP_RIGHT : OUTER_BOTTOM_RIGHT;
                        case OUTER_RIGHT -> isUpsideDown ? OUTER_TOP_LEFT : OUTER_BOTTOM_LEFT;
                    };
                    case WEST -> switch (stairsShape) {
                        case STRAIGHT -> LEFT_STAIR;
                        case INNER_LEFT -> isUpsideDown ? INNER_BOTTOM_RIGHT : INNER_TOP_RIGHT;
                        case INNER_RIGHT -> isUpsideDown ? INNER_TOP_RIGHT : INNER_BOTTOM_RIGHT;
                        case OUTER_LEFT -> isUpsideDown ? OUTER_TOP_LEFT : OUTER_BOTTOM_LEFT;
                        case OUTER_RIGHT -> isUpsideDown ? OUTER_BOTTOM_LEFT : OUTER_TOP_LEFT;
                    };
                    default -> throw new IllegalStateException("Unexpected value: " + stairFacing);
                };
            }
            return switch (stairsShape) {
                case STRAIGHT -> {
                    if (side == stairFacing) {
                        yield FULL;
                    } else if (side == stairFacing.getOpposite()) {
                        yield isUpsideDown ? UPSIDE_DOWN_STAIR : STAIR;
                    } else if (side == stairFacing.getCounterClockWise()) {
                        yield isUpsideDown ? UPSIDE_DOWN_L_SHAPE_STAIR : L_SHAPE_STAIR;
                    }//else if (side == stairFacing.getClockWise())
                    yield isUpsideDown ? UPSIDE_DOWN_BACKWARDS_L_SHAPE_STAIR : BACKWARDS_L_SHAPE_STAIR;
                }
                case INNER_LEFT -> {
                    if (side == stairFacing || side == stairFacing.getCounterClockWise()) {
                        yield FULL;
                    } else if (side == stairFacing.getOpposite()) {
                        yield isUpsideDown ? UPSIDE_DOWN_L_SHAPE_INNER_STAIR : L_SHAPE_INNER_STAIR;
                    }//else if (side == stairFacing.getClockWise())
                    yield isUpsideDown ? UPSIDE_DOWN_BACKWARDS_L_SHAPE_INNER_STAIR : BACKWARDS_L_SHAPE_INNER_STAIR;
                }
                case INNER_RIGHT -> {
                    if (side == stairFacing || side == stairFacing.getClockWise()) {
                        yield FULL;
                    } else if (side == stairFacing.getOpposite()) {
                        yield isUpsideDown ? UPSIDE_DOWN_BACKWARDS_L_SHAPE_INNER_STAIR : BACKWARDS_L_SHAPE_INNER_STAIR;
                    }//else if (side == stairFacing.getCounterClockWise())
                    yield isUpsideDown ? UPSIDE_DOWN_L_SHAPE_INNER_STAIR : L_SHAPE_INNER_STAIR;
                }
                case OUTER_LEFT -> {
                    //Only the left part of the back is present
                    if (side == stairFacing) {
                        yield isUpsideDown ? UPSIDE_DOWN_BACKWARDS_L_SHAPE_STAIR : BACKWARDS_L_SHAPE_STAIR;
                    } else if (side == stairFacing.getCounterClockWise()) {
                        yield isUpsideDown ? UPSIDE_DOWN_L_SHAPE_STAIR : L_SHAPE_STAIR;
                    } else if (side == stairFacing.getOpposite()) {
                        yield isUpsideDown ? UPSIDE_DOWN_OUTER_LEFT : OUTER_LEFT;
                    }
                    yield isUpsideDown ? UPSIDE_DOWN_OUTER_RIGHT : OUTER_RIGHT;
                }
                case OUTER_RIGHT -> {
                    //Only the right part of the back is present
                    if (side == stairFacing) {
                        yield isUpsideDown ? UPSIDE_DOWN_L_SHAPE_STAIR : L_SHAPE_STAIR;
                    } else if (side == stairFacing.getClockWise()) {
                        yield isUpsideDown ? UPSIDE_DOWN_BACKWARDS_L_SHAPE_STAIR : BACKWARDS_L_SHAPE_STAIR;
                    } else if (side == stairFacing.getOpposite()) {
                        yield isUpsideDown ? UPSIDE_DOWN_OUTER_RIGHT : OUTER_RIGHT;
                    }
                    yield isUpsideDown ? UPSIDE_DOWN_OUTER_LEFT : OUTER_LEFT;
                }
            };
        }
        //Not a slab or a stair, just treat it as a full block
        return FULL;
    }
}