package mekanism.common.block;

import java.util.Locale;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import mekanism.common.Mekanism;
import mekanism.common.block.interfaces.IHasGui;
import mekanism.common.block.interfaces.IHasInventory;
import mekanism.common.block.interfaces.IHasSecurity;
import mekanism.common.block.interfaces.IHasTileEntity;
import mekanism.common.block.interfaces.ISupportsRedstone;
import mekanism.common.block.interfaces.ITieredBlock;
import mekanism.common.block.states.BlockStateHelper;
import mekanism.common.block.states.IStateFacing;
import mekanism.common.item.block.ItemBlockGasTank;
import mekanism.common.tier.GasTankTier;
import mekanism.common.tile.base.TileEntityMekanism;
import mekanism.common.tile.base.WrenchResult;
import mekanism.common.tile.gas_tank.TileEntityAdvancedGasTank;
import mekanism.common.tile.gas_tank.TileEntityBasicGasTank;
import mekanism.common.tile.gas_tank.TileEntityCreativeGasTank;
import mekanism.common.tile.gas_tank.TileEntityEliteGasTank;
import mekanism.common.tile.gas_tank.TileEntityGasTank;
import mekanism.common.tile.gas_tank.TileEntityUltimateGasTank;
import mekanism.common.util.MekanismUtils;
import mekanism.common.util.SecurityUtils;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

public class BlockGasTank extends BlockMekanismContainer implements IHasGui, IStateFacing, ITieredBlock<GasTankTier>, IHasInventory, IHasSecurity, ISupportsRedstone,
      IHasTileEntity<TileEntityGasTank> {

    private static final AxisAlignedBB TANK_BOUNDS = new AxisAlignedBB(0.1875F, 0.0F, 0.1875F, 0.8125F, 1.0F, 0.8125F);

    private final GasTankTier tier;

    public BlockGasTank(GasTankTier tier) {
        super(Material.IRON);
        this.tier = tier;
        setHardness(3.5F);
        setResistance(8F);
        setRegistryName(new ResourceLocation(Mekanism.MODID, tier.getBaseTier().getSimpleName().toLowerCase(Locale.ROOT) + "_gas_tank"));
    }

    @Override
    public GasTankTier getTier() {
        return tier;
    }

    @Nonnull
    @Override
    protected BlockStateContainer createBlockState() {
        return BlockStateHelper.getBlockState(this);
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        //TODO
        return 0;
    }

    @Nonnull
    @Override
    @Deprecated
    public IBlockState getActualState(@Nonnull IBlockState state, IBlockAccess world, BlockPos pos) {
        return BlockStateHelper.getActualState(this, state, MekanismUtils.getTileEntitySafe(world, pos));
    }

    @Override
    public void setTileData(World world, BlockPos pos, IBlockState state, EntityLivingBase placer, ItemStack stack, @Nonnull TileEntityMekanism tile) {
        if (tile instanceof TileEntityGasTank) {
            TileEntityGasTank gasTank = (TileEntityGasTank) tile;
            gasTank.gasTank.setMaxGas(gasTank.tier.getStorage());
            gasTank.gasTank.setGas(((ItemBlockGasTank) stack.getItem()).getGas(stack));
        }
    }

    @Override
    @Deprecated
    public void neighborChanged(IBlockState state, World world, BlockPos pos, Block neighborBlock, BlockPos neighborPos) {
        if (!world.isRemote) {
            TileEntity tileEntity = world.getTileEntity(pos);
            if (tileEntity instanceof TileEntityMekanism) {
                ((TileEntityMekanism) tileEntity).onNeighborChange(neighborBlock);
            }
        }
    }

    @Override
    @Deprecated
    public float getPlayerRelativeBlockHardness(IBlockState state, @Nonnull EntityPlayer player, @Nonnull World world, @Nonnull BlockPos pos) {
        TileEntity tile = world.getTileEntity(pos);
        return SecurityUtils.canAccess(player, tile) ? super.getPlayerRelativeBlockHardness(state, player, world, pos) : 0.0F;
    }

    @Override
    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumHand hand, EnumFacing side, float hitX, float hitY, float hitZ) {
        if (world.isRemote) {
            return true;
        }
        TileEntityMekanism tileEntity = (TileEntityMekanism) world.getTileEntity(pos);
        if (tileEntity.tryWrench(state, player, hand, () -> new RayTraceResult(new Vec3d(hitX, hitY, hitZ), side, pos)) != WrenchResult.PASS) {
            return true;
        }
        if (tileEntity.openGui(player)) {
            return true;
        }
        return false;
    }

    @Override
    @Deprecated
    public boolean isOpaqueCube(IBlockState state) {
        return false;
    }

    @Nonnull
    @Override
    @Deprecated
    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess world, BlockPos pos) {
        return TANK_BOUNDS;
    }

    @Override
    public TileEntity createTileEntity(@Nonnull World world, @Nonnull IBlockState state) {
        switch (tier) {
            case BASIC:
                return new TileEntityBasicGasTank();
            case ADVANCED:
                return new TileEntityAdvancedGasTank();
            case ELITE:
                return new TileEntityEliteGasTank();
            case ULTIMATE:
                return new TileEntityUltimateGasTank();
            case CREATIVE:
                return new TileEntityCreativeGasTank();
        }
        return null;
    }

    @Override
    @Deprecated
    public boolean hasComparatorInputOverride(IBlockState state) {
        return true;
    }

    @Override
    @Deprecated
    public int getComparatorInputOverride(IBlockState state, World world, BlockPos pos) {
        TileEntityGasTank tileEntity = (TileEntityGasTank) world.getTileEntity(pos);
        return tileEntity.getRedstoneLevel();
    }

    @Override
    @Deprecated
    public boolean isFullBlock(IBlockState state) {
        return false;
    }

    @Override
    @Deprecated
    public boolean isFullCube(IBlockState state) {
        return false;
    }

    @Nonnull
    @Override
    @Deprecated
    public BlockFaceShape getBlockFaceShape(IBlockAccess world, IBlockState state, BlockPos pos, EnumFacing face) {
        return face == EnumFacing.UP || face == EnumFacing.DOWN ? BlockFaceShape.CENTER_BIG : BlockFaceShape.UNDEFINED;
    }

    @Override
    public int getGuiID() {
        return 10;
    }

    @Override
    public int getInventorySize() {
        return 2;
    }

    @Nullable
    @Override
    public Class<? extends TileEntityGasTank> getTileClass() {
        switch (tier) {
            case BASIC:
                return TileEntityBasicGasTank.class;
            case ADVANCED:
                return TileEntityAdvancedGasTank.class;
            case ELITE:
                return TileEntityEliteGasTank.class;
            case ULTIMATE:
                return TileEntityUltimateGasTank.class;
            case CREATIVE:
                return TileEntityCreativeGasTank.class;
        }
        return null;
    }
}