package mekanism.generators.common.tile;

import java.util.Optional;
import javax.annotation.Nonnull;
import mekanism.api.Action;
import mekanism.api.AutomationType;
import mekanism.api.IContentsListener;
import mekanism.api.RelativeSide;
import mekanism.api.math.FloatingLong;
import mekanism.common.capabilities.holder.slot.IInventorySlotHolder;
import mekanism.common.capabilities.holder.slot.InventorySlotHelper;
import mekanism.common.integration.computer.SpecialComputerMethodWrapper.ComputerIInventorySlotWrapper;
import mekanism.common.integration.computer.annotation.ComputerMethod;
import mekanism.common.integration.computer.annotation.WrappingComputerMethod;
import mekanism.common.inventory.container.MekanismContainer;
import mekanism.common.inventory.container.sync.SyncableBoolean;
import mekanism.common.inventory.container.sync.SyncableFloatingLong;
import mekanism.common.inventory.slot.EnergyInventorySlot;
import mekanism.common.tile.interfaces.IBoundingBlock;
import mekanism.common.util.MekanismUtils;
import mekanism.common.util.WorldUtils;
import mekanism.generators.common.config.MekanismGeneratorsConfig;
import mekanism.generators.common.registries.GeneratorsBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;

public class TileEntityWindGenerator extends TileEntityGenerator implements IBoundingBlock {

    public static final float SPEED = 32F;
    public static final float SPEED_SCALED = 256F / SPEED;

    private double angle;
    private FloatingLong currentMultiplier = FloatingLong.ZERO;
    private boolean isBlacklistDimension;
    @WrappingComputerMethod(wrapper = ComputerIInventorySlotWrapper.class, methodNames = "getEnergyItem")
    private EnergyInventorySlot energySlot;

    private double averageHeight = Double.NaN;
    private double averageHeightNorth = Double.NaN;
    private double averageHeightEast = Double.NaN;
    private double averageHeightSouth = Double.NaN;
    private double averageHeightWest = Double.NaN;

    public TileEntityWindGenerator(BlockPos pos, BlockState state) {
        super(GeneratorsBlocks.WIND_GENERATOR, pos, state, MekanismGeneratorsConfig.generators.windGenerationMax.get().multiply(2));
    }

    @Nonnull
    @Override
    protected IInventorySlotHolder getInitialInventory(IContentsListener listener) {
        InventorySlotHelper builder = InventorySlotHelper.forSide(this::getDirection);
        builder.addSlot(energySlot = EnergyInventorySlot.drain(getEnergyContainer(), listener, 143, 35));
        return builder.build();
    }

    @Override
    protected RelativeSide[] getEnergySides() {
        return new RelativeSide[]{RelativeSide.FRONT, RelativeSide.BOTTOM};
    }

    @Override
    protected void onUpdateServer() {
        super.onUpdateServer();
        energySlot.drainContainer();
        // If we're in a blacklisted dimension, there's nothing more to do
        if (isBlacklistDimension) {
            return;
        }
        if (ticker % 20 == 0) {
            // Recalculate the current multiplier once a second
            currentMultiplier = getMultiplier();
            setActive(MekanismUtils.canFunction(this) && !currentMultiplier.isZero());
        }
        if (!currentMultiplier.isZero() && MekanismUtils.canFunction(this) && !getEnergyContainer().getNeeded().isZero()) {
            getEnergyContainer().insert(MekanismGeneratorsConfig.generators.windGenerationMin.get().multiply(currentMultiplier), Action.EXECUTE, AutomationType.INTERNAL);
        }
    }

    @Override
    protected void onUpdateClient() {
        super.onUpdateClient();
        if (getActive()) {
            angle = (angle + (getBlockPos().getY() + 4F) / SPEED_SCALED) % 360;
        }
    }

    /**
     * Determines the current output multiplier, taking sky visibility and height into account.
     **/
    private FloatingLong getMultiplier() {
        if (level != null) {
            BlockPos top = getBlockPos().above(4);
            if (level.getFluidState(top).isEmpty() && level.canSeeSky(top)) {
                //Validate it isn't fluid logged to help try and prevent https://github.com/mekanism/Mekanism/issues/7344
                ChunkPos chunkPos = new ChunkPos(top);
                //Lazy init the heights for the different chunks once they are loaded, and ignore any neighboring ones if not loaded
                if (Double.isNaN(averageHeight)) {
                    averageHeight = calculateAverageHeight(level, chunkPos);
                }
                if (Double.isNaN(averageHeightNorth)) {
                    averageHeightNorth = calculateAverageHeight(level, new ChunkPos(chunkPos.x, chunkPos.z - 1));
                }
                if (Double.isNaN(averageHeightEast)) {
                    averageHeightEast = calculateAverageHeight(level, new ChunkPos(chunkPos.x + 1, chunkPos.z));
                }
                if (Double.isNaN(averageHeightSouth)) {
                    averageHeightSouth = calculateAverageHeight(level, new ChunkPos(chunkPos.x, chunkPos.z + 1));
                }
                if (Double.isNaN(averageHeightWest)) {
                    averageHeightWest = calculateAverageHeight(level, new ChunkPos(chunkPos.x - 1, chunkPos.z));
                }
                //Take the main chunk's average into account twice to give it a higher weight than all the other ones when calculating overall average
                double average = calculateAverageSurrounding(averageHeight, averageHeight, averageHeightNorth, averageHeightEast, averageHeightSouth, averageHeightWest);
                if (Double.isNaN(average)) {
                    //No known height means no power as something went wrong
                    return FloatingLong.ZERO;
                }
                int penalty = 0;
                //Compare our base block (not the top) to the usable average
                if (getBlockPos().getY() < average) {
                    //Below average height, inflict a penalty
                    penalty = MekanismGeneratorsConfig.generators.windGenerationBelowPenalty.get();
                }
                //TODO: else, eventually we probably want to take the height compared to the world into less account than the average compared to the world

                //Clamp the height limits as the logical bounds of the world
                int minY = Math.max(MekanismGeneratorsConfig.generators.windGenerationMinY.get(), level.getMinBuildHeight());
                int maxY = Math.min(MekanismGeneratorsConfig.generators.windGenerationMaxY.get(), level.dimensionType().logicalHeight());
                float clampedY = Math.min(maxY, Math.max(minY, top.getY()));
                FloatingLong minG = MekanismGeneratorsConfig.generators.windGenerationMin.get();
                FloatingLong maxG = MekanismGeneratorsConfig.generators.windGenerationMax.get();
                FloatingLong slope = maxG.subtract(minG).divide(maxY - minY);
                FloatingLong toGen = minG.add(slope.multiply(clampedY - minY));

                if (penalty != 0) {
                    toGen = toGen.divideEquals(penalty);
                }
                return toGen.divide(minG);
            }
        }
        return FloatingLong.ZERO;
    }

    private double calculateAverageHeight(Level level, ChunkPos chunkPos) {
        //Validate it is in the world's bounds
        if (chunkPos.x >= -1_875_000 && chunkPos.z >= -1_875_000 && chunkPos.x < 1_875_000 && chunkPos.z < 1_875_000) {
            Optional<ChunkAccess> chunkIfLoaded = WorldUtils.getChunkIfLoaded(level, chunkPos.x, chunkPos.z);
            if (chunkIfLoaded.isPresent()) {
                int total = 0;
                int positions = 0;
                int increment = 4;
                ChunkAccess chunk = chunkIfLoaded.get();
                for (int x = chunkPos.getMinBlockX(); x <= chunkPos.getMaxBlockX(); x += increment) {
                    for (int z = chunkPos.getMinBlockZ(); z <= chunkPos.getMaxBlockZ(); z += increment) {
                        //From how Level#getHeight calls getHeight on the chunk
                        total += chunk.getHeight(Heightmap.Types.WORLD_SURFACE, x & 15, z & 15) + 1;
                        positions++;
                    }
                }
                return total / (double) positions;
            }
        }
        return Double.NaN;
    }

    private double calculateAverageSurrounding(double... surrounding) {
        double total = 0;
        double count = 0;
        for (double height : surrounding) {
            if (!Double.isNaN(height)) {
                total += height;
                count++;
            }
        }
        return count == 0 ? Double.NaN : total / count;
    }

    @Override
    public void setLevel(@Nonnull Level world) {
        super.setLevel(world);
        // Check the blacklist and force an update if we're in the blacklist. Otherwise, we'll never send
        // an initial activity status and the client (in MP) will show the windmills turning while not
        // generating any power
        isBlacklistDimension = MekanismGeneratorsConfig.generators.windGenerationDimBlacklist.get().contains(world.dimension().location());
        if (isBlacklistDimension) {
            setActive(false);
        }
        //And reset any cached average heights
        averageHeight = Double.NaN;
        averageHeightNorth = Double.NaN;
        averageHeightEast = Double.NaN;
        averageHeightSouth = Double.NaN;
        averageHeightWest = Double.NaN;
    }

    public FloatingLong getCurrentMultiplier() {
        return currentMultiplier;
    }

    public double getAngle() {
        return angle;
    }

    @ComputerMethod(nameOverride = "isBlacklistedDimension")
    public boolean isBlacklistDimension() {
        return isBlacklistDimension;
    }

    @Override
    public SoundSource getSoundCategory() {
        return SoundSource.WEATHER;
    }

    @Override
    public BlockPos getSoundPos() {
        return super.getSoundPos().above(4);
    }

    @Override
    public void addContainerTrackers(MekanismContainer container) {
        super.addContainerTrackers(container);
        container.track(SyncableFloatingLong.create(this::getCurrentMultiplier, value -> currentMultiplier = value));
        container.track(SyncableBoolean.create(this::isBlacklistDimension, value -> isBlacklistDimension = value));
    }

    @Nonnull
    @Override
    public AABB getRenderBoundingBox() {
        //Note: we just extend it to the max size it could be ignoring what direction it is actually facing
        return new AABB(worldPosition.offset(-2, 0, -2), worldPosition.offset(3, 7, 3));
    }

    //Methods relating to IComputerTile
    @ComputerMethod
    private FloatingLong getProductionRate() {
        return getActive() ? MekanismGeneratorsConfig.generators.windGenerationMin.get().multiply(getCurrentMultiplier()) : FloatingLong.ZERO;
    }
    //End methods IComputerTile
}