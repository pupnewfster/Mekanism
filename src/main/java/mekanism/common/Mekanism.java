package mekanism.common;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import mekanism.api.Coord4D;
import mekanism.api.EnumColor;
import mekanism.api.MekanismAPI;
import mekanism.api.MekanismAPI.BoxBlacklistEvent;
import mekanism.api.MekanismConfig.general;
import mekanism.api.MekanismConfig.usage;
import mekanism.api.gas.Gas;
import mekanism.api.gas.GasNetwork.GasTransferEvent;
import mekanism.api.gas.GasRegistry;
import mekanism.api.gas.GasStack;
import mekanism.api.gas.OreGas;
import mekanism.api.infuse.InfuseObject;
import mekanism.api.infuse.InfuseRegistry;
import mekanism.api.infuse.InfuseType;
import mekanism.api.transmitters.DynamicNetwork.ClientTickUpdate;
import mekanism.api.transmitters.DynamicNetwork.NetworkClientRequest;
import mekanism.api.transmitters.DynamicNetwork.TransmittersAddedEvent;
import mekanism.api.transmitters.TransmitterNetworkRegistry;
import mekanism.client.ClientTickHandler;
import mekanism.client.model.item.EnergyCubeItemModel;
import mekanism.common.EnergyNetwork.EnergyTransferEvent;
import mekanism.common.FluidNetwork.FluidTransferEvent;
import mekanism.common.Tier.BaseTier;
import mekanism.common.Tier.EnergyCubeTier;
import mekanism.common.Tier.FactoryTier;
import mekanism.common.Tier.InductionCellTier;
import mekanism.common.Tier.InductionProviderTier;
import mekanism.common.base.IChunkLoadHandler;
import mekanism.common.base.IFactory.RecipeType;
import mekanism.common.base.IModule;
import mekanism.common.block.states.BlockStateMachine;
import mekanism.common.chunkloading.ChunkManager;
import mekanism.common.content.boiler.BoilerManager;
import mekanism.common.content.boiler.SynchronizedBoilerData;
import mekanism.common.content.matrix.SynchronizedMatrixData;
import mekanism.common.content.tank.SynchronizedTankData;
import mekanism.common.content.transporter.PathfinderCache;
import mekanism.common.content.transporter.TransporterManager;
import mekanism.common.entity.EntityBabySkeleton;
import mekanism.common.entity.EntityBalloon;
import mekanism.common.entity.EntityFlame;
import mekanism.common.entity.EntityObsidianTNT;
import mekanism.common.entity.EntityRobit;
import mekanism.common.frequency.Frequency;
import mekanism.common.frequency.FrequencyManager;
import mekanism.common.integration.MekanismHooks;
import mekanism.common.integration.OreDictManager;
import mekanism.common.multiblock.MultiblockManager;
import mekanism.common.network.PacketDataRequest.DataRequestMessage;
import mekanism.common.network.PacketSimpleGui;
import mekanism.common.network.PacketTransmitterUpdate.PacketType;
import mekanism.common.network.PacketTransmitterUpdate.TransmitterUpdateMessage;
import mekanism.common.recipe.BinRecipe;
import mekanism.common.recipe.RecipeHandler;
import mekanism.common.recipe.RecipeHandler.Recipe;
import mekanism.common.recipe.ShapedMekanismRecipe;
import mekanism.common.recipe.ShapelessMekanismRecipe;
import mekanism.common.recipe.inputs.ItemStackInput;
import mekanism.common.recipe.machines.SmeltingRecipe;
import mekanism.common.recipe.outputs.ItemStackOutput;
import mekanism.common.tile.TileEntityAdvancedBoundingBlock;
import mekanism.common.tile.TileEntityBoilerCasing;
import mekanism.common.tile.TileEntityBoilerValve;
import mekanism.common.tile.TileEntityBoundingBlock;
import mekanism.common.tile.TileEntityCardboardBox;
import mekanism.common.tile.TileEntityElectricBlock;
import mekanism.common.tile.TileEntityEntangledBlock;
import mekanism.common.tile.TileEntityLaser;
import mekanism.common.tile.TileEntityMetallurgicInfuser;
import mekanism.common.tile.TileEntityPressureDisperser;
import mekanism.common.tile.TileEntitySolarEvaporationBlock;
import mekanism.common.tile.TileEntitySolarEvaporationValve;
import mekanism.common.tile.TileEntitySuperheatingElement;
import mekanism.common.util.MekanismUtils;
import mekanism.common.voice.VoiceServerManager;
import mekanism.common.world.GenHandler;

import net.minecraft.client.resources.model.IBakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.entity.EnumCreatureType;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.item.crafting.FurnaceRecipes;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.ChunkCoordIntPair;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraft.world.biome.WorldChunkManager;
import net.minecraftforge.client.event.ModelBakeEvent;
import net.minecraftforge.common.ForgeChunkManager;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.event.world.ChunkDataEvent;
import net.minecraftforge.event.world.ChunkEvent;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.oredict.OreDictionary;
import net.minecraftforge.oredict.RecipeSorter;
import net.minecraftforge.oredict.RecipeSorter.Category;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import rebelkeithy.mods.metallurgy.api.IOreInfo;
import rebelkeithy.mods.metallurgy.api.MetallurgyAPI;

import com.mojang.authlib.GameProfile;

import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.IFuelHandler;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.Mod.Instance;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLInterModComms;
import net.minecraftforge.fml.common.event.FMLLoadCompleteEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.common.event.FMLServerStoppingEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.registry.EntityRegistry;
import net.minecraftforge.fml.common.registry.GameRegistry;

/**
 * Mekanism - a Minecraft mod
 * @author AidanBrady
 *
 */
@Mod(modid = "Mekanism", name = "Mekanism", version = "9.0.0", guiFactory = "mekanism.client.gui.ConfigGuiFactory",
		dependencies = "after:ForgeMultipart;after:BuildCraft;after:BuildCraftAPI;after:IC2;after:CoFHCore;" +
				"after:ComputerCraft;after:Galacticraft API;after:MetallurgyCore")
public class Mekanism
{
	/** Mekanism Packet Pipeline */
	public static PacketHandler packetHandler = new PacketHandler();

	/** Mekanism logger instance */
	public static Logger logger = LogManager.getLogger("Mekanism");
	
	/** Mekanism proxy instance */
	@SidedProxy(clientSide = "mekanism.client.ClientProxy", serverSide = "mekanism.common.CommonProxy")
	public static CommonProxy proxy;
	
    /** Mekanism mod instance */
	@Instance("Mekanism")
    public static Mekanism instance;
    
    /** Mekanism hooks instance */
    public static MekanismHooks hooks = new MekanismHooks();
    
    /** Mekanism configuration instance */
    public static Configuration configuration;
    
	/** Mekanism version number */
	public static Version versionNumber = new Version(9, 0, 0);
	
	/** MultiblockManagers for various structrures */
	public static MultiblockManager<SynchronizedTankData> tankManager = new MultiblockManager<SynchronizedTankData>("dynamicTank");
	public static MultiblockManager<SynchronizedMatrixData> matrixManager = new MultiblockManager<SynchronizedMatrixData>("inductionMatrix");
	public static MultiblockManager<SynchronizedBoilerData> boilerManager = new BoilerManager("thermoelectricBoiler");
	
	/** FrequencyManagers for various networks */
	public static FrequencyManager publicTeleporters = new FrequencyManager(Frequency.class);
	public static Map<String, FrequencyManager> privateTeleporters = new HashMap<String, FrequencyManager>();
	
	/** Mekanism creative tab */
	public static CreativeTabMekanism tabMekanism = new CreativeTabMekanism();
	
	/** List of Mekanism modules loaded */
	public static List<IModule> modulesLoaded = new ArrayList<IModule>();
	
	/** The latest version number which is received from the Mekanism server */
	public static String latestVersionNumber;
	
	/** The recent news which is received from the Mekanism server */
	public static String recentNews;
	
	/** The VoiceServer manager for walkie talkies */
	public static VoiceServerManager voiceManager;
	
	/** A list of the usernames of players who have donated to Mekanism. */
	public static List<String> donators = new ArrayList<String>();
	
	/** The server's world tick handler. */
	public static CommonWorldTickHandler worldTickHandler = new CommonWorldTickHandler();
	
	/** The Mekanism world generation handler. */
	public static GenHandler genHandler = new GenHandler();
	
	/** The version of ore generation in this version of Mekanism. Increment this every time the default ore generation changes. */
	public static int baseWorldGenVersion = 0;
	
	/** The GameProfile used by the dummy Mekanism player */
	public static GameProfile gameProfile = new GameProfile(UUID.nameUUIDFromBytes("mekanism.common".getBytes()), "[Mekanism]");
	
	public static KeySync keyMap = new KeySync();
	
	public static Set<String> jetpackOn = new HashSet<String>();
	public static Set<String> gasmaskOn = new HashSet<String>();
	public static Set<String> flamethrowerActive = new HashSet<String>();
	
	public static Set<Coord4D> activeVibrators = new HashSet<Coord4D>();

	/**
	 * Adds all in-game crafting, smelting and machine recipes.
	 */
	public void addRecipes()
	{
		//Storage Recipes
		CraftingManager.getInstance().getRecipeList().add(new ShapedMekanismRecipe(new ItemStack(MekanismBlocks.BasicBlock, 1, 3),
			"***", "***", "***", Character.valueOf('*'), new ItemStack(Items.coal, 1, 1)
		));
		CraftingManager.getInstance().getRecipeList().add(new ShapedMekanismRecipe(new ItemStack(Items.coal, 9, 1),
			"*", Character.valueOf('*'), new ItemStack(MekanismBlocks.BasicBlock, 1, 3)
		));
		CraftingManager.getInstance().getRecipeList().add(new ShapedMekanismRecipe(new ItemStack(MekanismBlocks.BasicBlock, 1, 2),
			"***", "***", "***", Character.valueOf('*'), "ingotRefinedObsidian"
		));
		CraftingManager.getInstance().getRecipeList().add(new ShapedMekanismRecipe(new ItemStack(MekanismItems.Ingot, 9, 0),
			"*", Character.valueOf('*'), new ItemStack(MekanismBlocks.BasicBlock, 1, 2)
		));
		CraftingManager.getInstance().getRecipeList().add(new ShapedMekanismRecipe(new ItemStack(MekanismBlocks.BasicBlock, 1, 4),
			"***", "***", "***", Character.valueOf('*'), "ingotRefinedGlowstone"
		));
		CraftingManager.getInstance().getRecipeList().add(new ShapedMekanismRecipe(new ItemStack(MekanismItems.Ingot, 9, 3),
			"*", Character.valueOf('*'), new ItemStack(MekanismBlocks.BasicBlock, 1, 4)
		));
		CraftingManager.getInstance().getRecipeList().add(new ShapedMekanismRecipe(new ItemStack(MekanismBlocks.BasicBlock, 1, 0),
			"XXX", "XXX", "XXX", Character.valueOf('X'), "ingotOsmium"
		));
		CraftingManager.getInstance().getRecipeList().add(new ShapedMekanismRecipe(new ItemStack(MekanismItems.Ingot, 9, 1),
			"*", Character.valueOf('*'), new ItemStack(MekanismBlocks.BasicBlock, 1, 0)
		));
		CraftingManager.getInstance().getRecipeList().add(new ShapedMekanismRecipe(new ItemStack(MekanismBlocks.BasicBlock, 1, 1),
			"***", "***", "***", Character.valueOf('*'), "ingotBronze"
		));
		CraftingManager.getInstance().getRecipeList().add(new ShapedMekanismRecipe(new ItemStack(MekanismItems.Ingot, 9, 2),
			"*", Character.valueOf('*'), new ItemStack(MekanismBlocks.BasicBlock, 1, 1)
		));
		CraftingManager.getInstance().getRecipeList().add(new ShapedMekanismRecipe(new ItemStack(MekanismBlocks.BasicBlock, 1, 5),
			"***", "***", "***", Character.valueOf('*'), "ingotSteel"
		));
		CraftingManager.getInstance().getRecipeList().add(new ShapedMekanismRecipe(new ItemStack(MekanismItems.Ingot, 9, 4),
			"*", Character.valueOf('*'), new ItemStack(MekanismBlocks.BasicBlock, 1, 5)
		));
		CraftingManager.getInstance().getRecipeList().add(new ShapedMekanismRecipe(new ItemStack(MekanismBlocks.BasicBlock, 1, 12),
			"***", "***", "***", Character.valueOf('*'), "ingotCopper"
		));
		CraftingManager.getInstance().getRecipeList().add(new ShapedMekanismRecipe(new ItemStack(MekanismItems.Ingot, 9, 5),
			"*", Character.valueOf('*'), new ItemStack(MekanismBlocks.BasicBlock, 1, 12)
		));
		CraftingManager.getInstance().getRecipeList().add(new ShapedMekanismRecipe(new ItemStack(MekanismBlocks.BasicBlock, 1, 13),
			"***", "***", "***", Character.valueOf('*'), "ingotTin"
		));
		CraftingManager.getInstance().getRecipeList().add(new ShapedMekanismRecipe(new ItemStack(MekanismItems.Ingot, 9, 6),
			"*", Character.valueOf('*'), new ItemStack(MekanismBlocks.BasicBlock, 1, 13)
		));
		CraftingManager.getInstance().getRecipeList().add(new ShapedMekanismRecipe(new ItemStack(MekanismBlocks.SaltBlock),
			"**", "**", Character.valueOf('*'), MekanismItems.Salt
		));
		
		//Base Recipes
		CraftingManager.getInstance().getRecipeList().add(new ShapedMekanismRecipe(new ItemStack(MekanismBlocks.ObsidianTNT, 1),
			"***", "XXX", "***", Character.valueOf('*'), Blocks.obsidian, Character.valueOf('X'), Blocks.tnt
		));
		CraftingManager.getInstance().getRecipeList().add(new ShapedMekanismRecipe(MekanismItems.ElectricBow.getUnchargedItem(),
			" AB", "E B", " AB", Character.valueOf('A'), MekanismItems.EnrichedAlloy, Character.valueOf('B'), Items.string, Character.valueOf('E'), MekanismItems.EnergyTablet.getUnchargedItem()
		));
		CraftingManager.getInstance().getRecipeList().add(new ShapedMekanismRecipe(MekanismItems.EnergyTablet.getUnchargedItem(),
			"RCR", "ECE", "RCR", Character.valueOf('C'), "ingotGold", Character.valueOf('R'), "dustRedstone", Character.valueOf('E'), MekanismItems.EnrichedAlloy
		));
		BlockStateMachine.MachineType.ENRICHMENT_CHAMBER.addRecipe(new ShapedMekanismRecipe(new ItemStack(MekanismBlocks.MachineBlock, 1, 0),
			"RCR", "iIi", "RCR", Character.valueOf('i'), "ingotIron", Character.valueOf('C'), MekanismUtils.getControlCircuit(BaseTier.BASIC), Character.valueOf('R'), "alloyBasic", Character.valueOf('I'), new ItemStack(MekanismBlocks.BasicBlock, 1, 8)
		));
		BlockStateMachine.MachineType.OSMIUM_COMPRESSOR.addRecipe(new ShapedMekanismRecipe(new ItemStack(MekanismBlocks.MachineBlock, 1, 1),
			"ECE", "BIB", "ECE", Character.valueOf('E'), "alloyAdvanced", Character.valueOf('C'), MekanismUtils.getControlCircuit(BaseTier.ADVANCED), Character.valueOf('B'), Items.bucket, Character.valueOf('I'), new ItemStack(MekanismBlocks.BasicBlock, 1, 8)
		));
		BlockStateMachine.MachineType.COMBINER.addRecipe(new ShapedMekanismRecipe(new ItemStack(MekanismBlocks.MachineBlock, 1, 2),
			"RCR", "SIS", "RCR", Character.valueOf('S'), Blocks.cobblestone, Character.valueOf('C'), MekanismUtils.getControlCircuit(BaseTier.ELITE), Character.valueOf('R'), "alloyElite", Character.valueOf('I'), new ItemStack(MekanismBlocks.BasicBlock, 1, 8)
		));
		BlockStateMachine.MachineType.CRUSHER.addRecipe(new ShapedMekanismRecipe(new ItemStack(MekanismBlocks.MachineBlock, 1, 3),
			"RCR", "LIL", "RCR", Character.valueOf('R'), "dustRedstone", Character.valueOf('L'), Items.lava_bucket, Character.valueOf('C'), MekanismUtils.getControlCircuit(BaseTier.BASIC), Character.valueOf('I'), new ItemStack(MekanismBlocks.BasicBlock, 1, 8)
		));
		CraftingManager.getInstance().getRecipeList().add(new ShapedMekanismRecipe(new ItemStack(MekanismItems.SpeedUpgrade),
			" G ", "ADA", " G ", Character.valueOf('G'), "blockGlass", Character.valueOf('A'), MekanismItems.EnrichedAlloy, Character.valueOf('D'), "dustOsmium"
		));
		CraftingManager.getInstance().getRecipeList().add(new ShapedMekanismRecipe(new ItemStack(MekanismItems.EnergyUpgrade),
			" G ", "ADA", " G ", Character.valueOf('G'), "blockGlass", Character.valueOf('A'), MekanismItems.EnrichedAlloy, Character.valueOf('D'), "dustGold"
		));
		CraftingManager.getInstance().getRecipeList().add(new ShapedMekanismRecipe(new ItemStack(MekanismItems.GasUpgrade),
			" G ", "ADA", " G ", Character.valueOf('G'), "blockGlass", Character.valueOf('A'), MekanismItems.EnrichedAlloy, Character.valueOf('D'), "dustIron"
		));
		CraftingManager.getInstance().getRecipeList().add(new ShapedMekanismRecipe(new ItemStack(MekanismItems.FilterUpgrade),
			" G ", "ADA", " G ", Character.valueOf('G'), "blockGlass", Character.valueOf('A'), MekanismItems.EnrichedAlloy, Character.valueOf('D'), "dustTin"
		));
		CraftingManager.getInstance().getRecipeList().add(new ShapedMekanismRecipe(new ItemStack(MekanismItems.MufflingUpgrade),
			" G ", "ADA", " G ", Character.valueOf('G'), "blockGlass", Character.valueOf('A'), MekanismItems.EnrichedAlloy, Character.valueOf('D'), "dustSteel"
		));
		CraftingManager.getInstance().getRecipeList().add(new ShapedMekanismRecipe(MekanismItems.AtomicDisassembler.getUnchargedItem(),
			"AEA", "ACA", " O ", Character.valueOf('A'), MekanismItems.EnrichedAlloy, Character.valueOf('E'), MekanismItems.EnergyTablet.getUnchargedItem(), Character.valueOf('C'), MekanismItems.AtomicAlloy, Character.valueOf('O'), "ingotRefinedObsidian"
		));
		CraftingManager.getInstance().getRecipeList().add(new ShapedMekanismRecipe(MekanismUtils.getEmptyGasTank(),
			"PPP", "PDP", "PPP", Character.valueOf('P'), "ingotOsmium", Character.valueOf('D'), "dustIron"
		));
		BlockStateMachine.MachineType.METALLURGIC_INFUSER.addRecipe(new ShapedMekanismRecipe(new ItemStack(MekanismBlocks.MachineBlock, 1, 8),
			"IFI", "ROR", "IFI", Character.valueOf('I'), "ingotIron", Character.valueOf('F'), Blocks.furnace, Character.valueOf('R'), "dustRedstone", Character.valueOf('O'), "ingotOsmium"
		));
		CraftingManager.getInstance().getRecipeList().add(new ShapedMekanismRecipe(new ItemStack(MekanismItems.TeleportationCore),
			"LAL", "GDG", "LAL", Character.valueOf('L'), new ItemStack(Items.dye, 1, 4), Character.valueOf('A'), MekanismItems.AtomicAlloy, Character.valueOf('G'), "ingotGold", Character.valueOf('D'), Items.diamond
		));
		CraftingManager.getInstance().getRecipeList().add(new ShapedMekanismRecipe(MekanismItems.PortableTeleporter.getUnchargedItem(),
			" E ", "CTC", " E ", Character.valueOf('E'), MekanismItems.EnergyTablet.getUnchargedItem(), Character.valueOf('C'), MekanismUtils.getControlCircuit(BaseTier.BASIC), Character.valueOf('T'), MekanismItems.TeleportationCore
		));
		BlockStateMachine.MachineType.TELEPORTER.addRecipe(new ShapedMekanismRecipe(new ItemStack(MekanismBlocks.MachineBlock, 1, 11),
			"COC", "OTO", "COC", Character.valueOf('C'), MekanismUtils.getControlCircuit(BaseTier.BASIC), Character.valueOf('O'), new ItemStack(MekanismBlocks.BasicBlock, 1, 8), Character.valueOf('T'), MekanismItems.TeleportationCore
		));
		BlockStateMachine.MachineType.PURIFICATION_CHAMBER.addRecipe(new ShapedMekanismRecipe(new ItemStack(MekanismBlocks.MachineBlock, 1, 9),
			"ECE", "ORO", "ECE", Character.valueOf('C'), MekanismUtils.getControlCircuit(BaseTier.ADVANCED), Character.valueOf('E'), "alloyAdvanced", Character.valueOf('O'), "ingotOsmium", Character.valueOf('R'), new ItemStack(MekanismBlocks.MachineBlock, 1, 0)
		));
		CraftingManager.getInstance().getRecipeList().add(new ShapedMekanismRecipe(MekanismItems.Configurator.getUnchargedItem(),
			" L ", "AEA", " S ", Character.valueOf('L'), new ItemStack(Items.dye, 1, 4), Character.valueOf('A'), MekanismItems.EnrichedAlloy, Character.valueOf('E'), MekanismItems.EnergyTablet.getUnchargedItem(), Character.valueOf('S'), Items.stick
		));
		CraftingManager.getInstance().getRecipeList().add(new ShapedMekanismRecipe(new ItemStack(MekanismBlocks.BasicBlock, 9, 7),
			"OOO", "OGO", "OOO", Character.valueOf('O'), "ingotRefinedObsidian", Character.valueOf('G'), "ingotRefinedGlowstone"
		));
		CraftingManager.getInstance().getRecipeList().add(new ShapedMekanismRecipe(new ItemStack(MekanismBlocks.BasicBlock, 1, 8),
			" S ", "SPS", " S ", Character.valueOf('S'), "ingotSteel", Character.valueOf('P'), "ingotOsmium"
		));
		BlockStateMachine.MachineType.ENERGIZED_SMELTER.addRecipe(new ShapedMekanismRecipe(new ItemStack(MekanismBlocks.MachineBlock, 1, 10),
			"RCR", "GIG", "RCR", Character.valueOf('C'), MekanismUtils.getControlCircuit(BaseTier.BASIC), Character.valueOf('R'), "alloyBasic", Character.valueOf('G'), "blockGlass", Character.valueOf('I'), new ItemStack(MekanismBlocks.BasicBlock, 1, 8)
		));
		BlockStateMachine.MachineType.ELECTRIC_PUMP.addRecipe(new ShapedMekanismRecipe(new ItemStack(MekanismBlocks.MachineBlock, 1, 12),
			" B ", "ECE", "OOO", Character.valueOf('B'), Items.bucket, Character.valueOf('E'), MekanismItems.EnrichedAlloy, Character.valueOf('C'), new ItemStack(MekanismBlocks.BasicBlock, 1, 8), Character.valueOf('O'), "ingotOsmium"
		));
		BlockStateMachine.MachineType.ELECTRIC_CHEST.addRecipe(new ShapedMekanismRecipe(new ItemStack(MekanismBlocks.MachineBlock, 1, 13),
			"SGS", "CcC", "SSS", Character.valueOf('S'), "ingotSteel", Character.valueOf('G'), "blockGlass", Character.valueOf('C'), Blocks.chest, Character.valueOf('c'), MekanismUtils.getControlCircuit(BaseTier.BASIC)
		));
		CraftingManager.getInstance().getRecipeList().add(new ShapedMekanismRecipe(new ItemStack(MekanismBlocks.BasicBlock, 4, 9),
			" I ", "IBI", " I ", Character.valueOf('I'), "ingotSteel", Character.valueOf('B'), Items.bucket
		));
		CraftingManager.getInstance().getRecipeList().add(new ShapedMekanismRecipe(new ItemStack(MekanismBlocks.BasicBlock, 4, 10),
			" I ", "IGI", " I ", Character.valueOf('I'), new ItemStack(MekanismBlocks.BasicBlock, 1, 9), Character.valueOf('G'), "blockGlass"
		));
		CraftingManager.getInstance().getRecipeList().add(new ShapedMekanismRecipe(new ItemStack(MekanismBlocks.BasicBlock, 2, 11),
			" I ", "ICI", " I ", Character.valueOf('I'), new ItemStack(MekanismBlocks.BasicBlock, 1, 9), Character.valueOf('C'), MekanismUtils.getControlCircuit(BaseTier.BASIC)
		));
		BlockStateMachine.MachineType.CHARGEPAD.addRecipe(new ShapedMekanismRecipe(new ItemStack(MekanismBlocks.MachineBlock, 1, 14),
			"PPP", "SES", Character.valueOf('P'), Blocks.stone_pressure_plate, Character.valueOf('S'), "ingotSteel", Character.valueOf('E'), MekanismItems.EnergyTablet.getUnchargedItem()
		));
		CraftingManager.getInstance().getRecipeList().add(new ShapedMekanismRecipe(MekanismItems.Robit.getUnchargedItem(),
			" S ", "ECE", "OIO", Character.valueOf('S'), "ingotSteel", Character.valueOf('E'), MekanismItems.EnergyTablet.getUnchargedItem(), Character.valueOf('C'), MekanismItems.AtomicAlloy, Character.valueOf('O'), "ingotRefinedObsidian", Character.valueOf('I'), new ItemStack(MekanismBlocks.MachineBlock, 1, 13)
		));
		CraftingManager.getInstance().getRecipeList().add(new ShapedMekanismRecipe(MekanismItems.NetworkReader.getUnchargedItem(),
			" G ", "AEA", " I ", Character.valueOf('G'), "blockGlass", Character.valueOf('A'), MekanismItems.EnrichedAlloy, Character.valueOf('E'), MekanismItems.EnergyTablet.getUnchargedItem(), Character.valueOf('I'), "ingotSteel"
		));
		CraftingManager.getInstance().getRecipeList().add(new ShapedMekanismRecipe(new ItemStack(MekanismItems.WalkieTalkie),
			"  O", "SCS", " S ", Character.valueOf('O'), "ingotOsmium", Character.valueOf('S'), "ingotSteel", Character.valueOf('C'), MekanismUtils.getControlCircuit(BaseTier.BASIC)
		));
		BlockStateMachine.MachineType.LOGISTICAL_SORTER.addRecipe(new ShapedMekanismRecipe(new ItemStack(MekanismBlocks.MachineBlock, 1, 15),
			"IPI", "ICI", "III", Character.valueOf('I'), "ingotIron", Character.valueOf('P'), Blocks.piston, Character.valueOf('C'), MekanismUtils.getControlCircuit(BaseTier.BASIC)
		));
		CraftingManager.getInstance().getRecipeList().add(new ShapedMekanismRecipe(new ItemStack(MekanismBlocks.BasicBlock, 1, 6),
			"SSS", "SCS", "SSS", Character.valueOf('S'), Blocks.cobblestone, Character.valueOf('C'), MekanismUtils.getControlCircuit(BaseTier.BASIC)
		));
		BlockStateMachine.MachineType.DIGITAL_MINER.addRecipe(new ShapedMekanismRecipe(new ItemStack(MekanismBlocks.MachineBlock, 1, 4),
			"ACA", "SES", "TIT", Character.valueOf('A'), MekanismItems.AtomicAlloy, Character.valueOf('C'), MekanismUtils.getControlCircuit(BaseTier.BASIC), Character.valueOf('S'), new ItemStack(MekanismBlocks.MachineBlock, 1, 15), Character.valueOf('E'), MekanismItems.Robit.getUnchargedItem(),
			Character.valueOf('I'), new ItemStack(MekanismBlocks.BasicBlock, 1, 8), Character.valueOf('T'), MekanismItems.TeleportationCore
		));
		BlockStateMachine.MachineType.ROTARY_CONDENSENTRATOR.addRecipe(new ShapedMekanismRecipe(new ItemStack(MekanismBlocks.MachineBlock2, 1, 0),
			"GCG", "tEI", "GCG", Character.valueOf('G'), "blockGlass", Character.valueOf('C'), MekanismUtils.getControlCircuit(BaseTier.BASIC), Character.valueOf('t'), MekanismUtils.getEmptyGasTank(), Character.valueOf('E'), MekanismItems.EnergyTablet.getUnchargedItem(),
			Character.valueOf('T'), new ItemStack(MekanismBlocks.BasicBlock, 1, 9), Character.valueOf('I'), new ItemStack(MekanismBlocks.MachineBlock2, 1, 11)
		));
		CraftingManager.getInstance().getRecipeList().add(new ShapedMekanismRecipe(MekanismItems.Jetpack.getEmptyItem(),
			"SCS", "TGT", " T ", Character.valueOf('S'), "ingotSteel", Character.valueOf('C'), MekanismUtils.getControlCircuit(BaseTier.BASIC), Character.valueOf('T'), "ingotTin", Character.valueOf('G'), MekanismUtils.getEmptyGasTank()
		));
		CraftingManager.getInstance().getRecipeList().add(new ShapedMekanismRecipe(new ItemStack(MekanismItems.Dictionary),
			"C", "B", Character.valueOf('C'), MekanismUtils.getControlCircuit(BaseTier.BASIC), Character.valueOf('B'), Items.book
		));
		CraftingManager.getInstance().getRecipeList().add(new ShapedMekanismRecipe(new ItemStack(MekanismItems.GasMask),
			" S ", "GCG", "S S", Character.valueOf('S'), "ingotSteel", Character.valueOf('G'), "blockGlass", Character.valueOf('C'), MekanismUtils.getControlCircuit(BaseTier.BASIC)
		));
		CraftingManager.getInstance().getRecipeList().add(new ShapedMekanismRecipe(MekanismItems.ScubaTank.getEmptyItem(),
			" C ", "ATA", "SSS", Character.valueOf('C'), MekanismUtils.getControlCircuit(BaseTier.BASIC), Character.valueOf('A'), MekanismItems.EnrichedAlloy, Character.valueOf('S'), "ingotSteel"
		));
		BlockStateMachine.MachineType.CHEMICAL_OXIDIZER.addRecipe(new ShapedMekanismRecipe(new ItemStack(MekanismBlocks.MachineBlock2, 1, 1),
			"ACA", "ERG", "ACA", Character.valueOf('C'), MekanismUtils.getControlCircuit(BaseTier.BASIC), Character.valueOf('R'), new ItemStack(MekanismBlocks.BasicBlock, 1, 9), Character.valueOf('G'), MekanismUtils.getEmptyGasTank(), Character.valueOf('E'), new ItemStack(MekanismBlocks.MachineBlock, 1, 13), Character.valueOf('A'), MekanismItems.EnrichedAlloy
		));
		BlockStateMachine.MachineType.CHEMICAL_INFUSER.addRecipe(new ShapedMekanismRecipe(new ItemStack(MekanismBlocks.MachineBlock2, 1, 2),
			"ACA", "GRG", "ACA", Character.valueOf('C'), MekanismUtils.getControlCircuit(BaseTier.BASIC), Character.valueOf('R'), new ItemStack(MekanismBlocks.BasicBlock, 1, 9), Character.valueOf('G'), MekanismUtils.getEmptyGasTank(), Character.valueOf('A'), MekanismItems.EnrichedAlloy
		));
		BlockStateMachine.MachineType.CHEMICAL_INJECTION_CHAMBER.addRecipe(new ShapedMekanismRecipe(new ItemStack(MekanismBlocks.MachineBlock2, 1, 3),
			"RCR", "GPG", "RCR", Character.valueOf('C'), MekanismUtils.getControlCircuit(BaseTier.ELITE), Character.valueOf('R'), "alloyElite", Character.valueOf('G'), "ingotGold", Character.valueOf('P'), new ItemStack(MekanismBlocks.MachineBlock, 1, 9)
		));
		BlockStateMachine.MachineType.ELECTROLYTIC_SEPARATOR.addRecipe(new ShapedMekanismRecipe(new ItemStack(MekanismBlocks.MachineBlock2, 1, 4),
			"IRI", "ECE", "IRI", Character.valueOf('I'), "ingotIron", Character.valueOf('R'), "dustRedstone", Character.valueOf('E'), MekanismItems.EnrichedAlloy, Character.valueOf('C'), MekanismItems.ElectrolyticCore
		));
		CraftingManager.getInstance().getRecipeList().add(new ShapedMekanismRecipe(new ItemStack(MekanismItems.ElectrolyticCore),
			"EPE", "IEG", "EPE", Character.valueOf('E'), MekanismItems.EnrichedAlloy, Character.valueOf('P'), "dustOsmium", Character.valueOf('I'), "dustIron", Character.valueOf('G'), "dustGold"
		));
		CraftingManager.getInstance().getRecipeList().add(new ShapedMekanismRecipe(new ItemStack(MekanismBlocks.CardboardBox),
			"SS", "SS", Character.valueOf('S'), "pulpWood"
		));
		CraftingManager.getInstance().getRecipeList().add(new ShapedMekanismRecipe(new ItemStack(Items.paper, 6),
			"SSS", Character.valueOf('S'), MekanismItems.Sawdust
		));
		BlockStateMachine.MachineType.PRECISION_SAWMILL.addRecipe(new ShapedMekanismRecipe(new ItemStack(MekanismBlocks.MachineBlock2, 1, 5),
			"ICI", "ASA", "ICI", Character.valueOf('I'), "ingotIron", Character.valueOf('C'), MekanismUtils.getControlCircuit(BaseTier.BASIC), Character.valueOf('A'), MekanismItems.EnrichedAlloy, Character.valueOf('S'), new ItemStack(MekanismBlocks.BasicBlock, 1, 8)
		));
		CraftingManager.getInstance().getRecipeList().add(new ShapedMekanismRecipe(new ItemStack(MekanismBlocks.BasicBlock, 1, 14),
			"CGC", "IBI", "CGC", Character.valueOf('C'), MekanismUtils.getControlCircuit(BaseTier.BASIC), Character.valueOf('G'), "paneGlass", Character.valueOf('I'), new ItemStack(MekanismBlocks.BasicBlock, 1, 8), Character.valueOf('B'), "blockCopper"
		));
		CraftingManager.getInstance().getRecipeList().add(new ShapedMekanismRecipe(new ItemStack(MekanismBlocks.BasicBlock, 1, 15),
			"ITI", "CBC", "ITI", Character.valueOf('I'), "ingotCopper", Character.valueOf('T'), new ItemStack(MekanismBlocks.BasicBlock, 1, 11), Character.valueOf('C'), MekanismUtils.getControlCircuit(BaseTier.BASIC), Character.valueOf('B'), new ItemStack(MekanismBlocks.BasicBlock, 1, 8)
		));
		CraftingManager.getInstance().getRecipeList().add(new ShapedMekanismRecipe(new ItemStack(MekanismBlocks.BasicBlock2, 1, 0),
			"CCC", "CTC", "CCC", Character.valueOf('C'), "ingotCopper", Character.valueOf('T'), new ItemStack(MekanismBlocks.BasicBlock, 1, 9)
		));
		BlockStateMachine.MachineType.CHEMICAL_DISSOLUTION_CHAMBER.addRecipe(new ShapedMekanismRecipe(new ItemStack(MekanismBlocks.MachineBlock2, 1, 6),
			"CGC", "EAE", "CGC", Character.valueOf('G'), MekanismUtils.getEmptyGasTank(), Character.valueOf('C'), MekanismUtils.getControlCircuit(BaseTier.BASIC), Character.valueOf('A'), MekanismItems.AtomicAlloy, Character.valueOf('E'), MekanismItems.EnrichedAlloy
		));
		BlockStateMachine.MachineType.CHEMICAL_WASHER.addRecipe(new ShapedMekanismRecipe(new ItemStack(MekanismBlocks.MachineBlock2, 1, 7),
			"CWC", "EIE", "CGC", Character.valueOf('W'), Items.bucket, Character.valueOf('C'), MekanismUtils.getControlCircuit(BaseTier.BASIC), Character.valueOf('E'), MekanismItems.EnrichedAlloy, Character.valueOf('G'), MekanismUtils.getEmptyGasTank(), Character.valueOf('I'), new ItemStack(MekanismBlocks.BasicBlock, 1, 8)
		));
		BlockStateMachine.MachineType.CHEMICAL_CRYSTALLIZER.addRecipe(new ShapedMekanismRecipe(new ItemStack(MekanismBlocks.MachineBlock2, 1, 8),
			"CGC", "ASA", "CGC", Character.valueOf('G'), MekanismUtils.getEmptyGasTank(), Character.valueOf('C'), MekanismUtils.getControlCircuit(BaseTier.BASIC), Character.valueOf('A'), MekanismItems.AtomicAlloy, Character.valueOf('S'), new ItemStack(MekanismBlocks.BasicBlock, 1, 8)
		));
		CraftingManager.getInstance().getRecipeList().add(new ShapedMekanismRecipe(MekanismItems.FreeRunners.getUnchargedItem(),
			"C C", "A A", "T T", Character.valueOf('C'), MekanismUtils.getControlCircuit(BaseTier.BASIC), Character.valueOf('A'), MekanismItems.EnrichedAlloy, Character.valueOf('T'), MekanismItems.EnergyTablet.getUnchargedItem()
		));
		CraftingManager.getInstance().getRecipeList().add(new ShapedMekanismRecipe(MekanismItems.ArmoredJetpack.getEmptyItem(),
			"D D", "BSB", " J ", Character.valueOf('D'), "dustDiamond", Character.valueOf('B'), "ingotBronze", Character.valueOf('S'), "blockSteel", Character.valueOf('J'), MekanismItems.Jetpack.getEmptyItem()
		));
		CraftingManager.getInstance().getRecipeList().add(new ShapedMekanismRecipe(new ItemStack(MekanismItems.FilterCard),
			" A ", "ACA", " A ", Character.valueOf('A'), MekanismItems.EnrichedAlloy, Character.valueOf('C'), MekanismUtils.getControlCircuit(BaseTier.BASIC)
		));
		CraftingManager.getInstance().getRecipeList().add(new ShapedMekanismRecipe(MekanismItems.SeismicReader.getUnchargedItem(),
			"SLS", "STS", "SSS", Character.valueOf('S'), "ingotSteel", Character.valueOf('L'), new ItemStack(Items.dye, 1, 4), Character.valueOf('T'), MekanismItems.EnergyTablet.getUnchargedItem()
		));
		BlockStateMachine.MachineType.SEISMIC_VIBRATOR.addRecipe(new ShapedMekanismRecipe(new ItemStack(MekanismBlocks.MachineBlock2, 1, 9),
			"TLT", "CIC", "TTT", Character.valueOf('T'), "ingotTin", Character.valueOf('L'), new ItemStack(Items.dye, 1, 4), Character.valueOf('C'), MekanismUtils.getControlCircuit(BaseTier.BASIC), Character.valueOf('I'), new ItemStack(MekanismBlocks.BasicBlock, 1, 8)
		));
		BlockStateMachine.MachineType.PRESSURIZED_REACTION_CHAMBER.addRecipe(new ShapedMekanismRecipe(new ItemStack(MekanismBlocks.MachineBlock2, 1, 10),
			"TET", "CIC", "GFG", Character.valueOf('S'), "ingotSteel", Character.valueOf('E'), MekanismItems.EnrichedAlloy, Character.valueOf('C'), MekanismUtils.getControlCircuit(BaseTier.BASIC), Character.valueOf('G'), MekanismUtils.getEmptyGasTank(),
			Character.valueOf('I'), new ItemStack(MekanismBlocks.MachineBlock, 1, 0), Character.valueOf('F'), new ItemStack(MekanismBlocks.BasicBlock, 1, 9)
		));
		BlockStateMachine.MachineType.PORTABLE_TANK.addRecipe(new ShapedMekanismRecipe(new ItemStack(MekanismBlocks.MachineBlock2, 1, 11),
			"III", "GCG", "III", Character.valueOf('I'), "ingotIron", Character.valueOf('G'), "blockGlass", Character.valueOf('C'), MekanismUtils.getControlCircuit(BaseTier.BASIC)
		));
		BlockStateMachine.MachineType.FLUIDIC_PLENISHER.addRecipe(new ShapedMekanismRecipe(new ItemStack(MekanismBlocks.MachineBlock2, 1, 12),
			"TTT", "CPC", "TTT", Character.valueOf('P'), new ItemStack(MekanismBlocks.MachineBlock, 1, 12), Character.valueOf('T'), "ingotTin", Character.valueOf('C'), MekanismUtils.getControlCircuit(BaseTier.BASIC)
		));
		CraftingManager.getInstance().getRecipeList().add(new ShapedMekanismRecipe(new ItemStack(Blocks.rail, 24),
			"O O", "OSO", "O O", Character.valueOf('O'), "ingotOsmium", Character.valueOf('S'), "stickWood"
		));
		CraftingManager.getInstance().getRecipeList().add(new ShapedMekanismRecipe(MekanismItems.Flamethrower.getEmptyItem(),
			"TTT", "TGS", "BCB", Character.valueOf('T'), "ingotTin", Character.valueOf('G'), MekanismUtils.getEmptyGasTank(), Character.valueOf('S'), Items.flint_and_steel, Character.valueOf('B'), "ingotBronze", Character.valueOf('C'), MekanismUtils.getControlCircuit(BaseTier.ADVANCED)
		));
		CraftingManager.getInstance().getRecipeList().add(new ShapedMekanismRecipe(MekanismItems.GaugeDropper.getEmptyItem(),
			" O ", "G G", "GGG", Character.valueOf('O'), "ingotOsmium", Character.valueOf('G'), "paneGlass"
		));
		BlockStateMachine.MachineType.SOLAR_NEUTRON_ACTIVATOR.addRecipe(new ShapedMekanismRecipe(new ItemStack(MekanismBlocks.MachineBlock3, 1, 1),
			"APA", "CSC", "BBB", Character.valueOf('A'), "alloyElite", Character.valueOf('S'), new ItemStack(MekanismBlocks.BasicBlock, 1, 8), Character.valueOf('P'), new ItemStack(MekanismItems.Polyethene, 1, 2), Character.valueOf('B'), "ingotBronze", Character.valueOf('C'), MekanismUtils.getControlCircuit(BaseTier.ELITE)
		));
		CraftingManager.getInstance().getRecipeList().add(new ShapedMekanismRecipe(new ItemStack(MekanismBlocks.BasicBlock2, 4, 1),
			" S ", "SES", " S ", Character.valueOf('S'), "ingotSteel", Character.valueOf('E'), MekanismItems.EnergyTablet.getUnchargedItem()
		));
		CraftingManager.getInstance().getRecipeList().add(new ShapedMekanismRecipe(new ItemStack(MekanismBlocks.BasicBlock2, 2, 2),
			" I ", "ICI", " I ", Character.valueOf('I'), new ItemStack(MekanismBlocks.BasicBlock2, 1, 1), Character.valueOf('C'), MekanismUtils.getControlCircuit(BaseTier.ELITE)
		));
		CraftingManager.getInstance().getRecipeList().add(new ShapedMekanismRecipe(new ItemStack(MekanismItems.FactoryInstaller, 1, 0),
			"RCR", "iWi", "RCR", Character.valueOf('R'), "alloyBasic", Character.valueOf('C'), MekanismUtils.getControlCircuit(BaseTier.BASIC), Character.valueOf('i'), "ingotIron", Character.valueOf('W'), "plankWood"
		));
		CraftingManager.getInstance().getRecipeList().add(new ShapedMekanismRecipe(new ItemStack(MekanismItems.FactoryInstaller, 1, 1),
			"ECE", "oWo", "ECE", Character.valueOf('E'), "alloyAdvanced", Character.valueOf('C'), MekanismUtils.getControlCircuit(BaseTier.ADVANCED), Character.valueOf('o'), "ingotOsmium", Character.valueOf('W'), "plankWood"
		));
		CraftingManager.getInstance().getRecipeList().add(new ShapedMekanismRecipe(new ItemStack(MekanismItems.FactoryInstaller, 1, 2),
			"RCR", "gWg", "RCR", Character.valueOf('R'), "alloyElite", Character.valueOf('C'), MekanismUtils.getControlCircuit(BaseTier.ELITE), Character.valueOf('g'), "ingotGold", Character.valueOf('W'), "plankWood"
		));
		BlockStateMachine.MachineType.OREDICTIONIFICATOR.addRecipe(new ShapedMekanismRecipe(new ItemStack(MekanismBlocks.MachineBlock3, 1, 3),
			"SGS", "CBC", "SWS", Character.valueOf('S'), "ingotSteel", Character.valueOf('G'), "paneGlass", Character.valueOf('C'), MekanismUtils.getControlCircuit(BaseTier.BASIC), Character.valueOf('B'), MekanismItems.Dictionary, Character.valueOf('W'), Blocks.chest
		));
		BlockStateMachine.MachineType.LASER.addRecipe(new ShapedMekanismRecipe(new ItemStack(MekanismBlocks.MachineBlock2, 1, 13),
			"RE ", "RCD", "RE ", Character.valueOf('R'), "alloyElite", Character.valueOf('E'), MekanismItems.EnergyTablet.getUnchargedItem(), Character.valueOf('C'), new ItemStack(MekanismBlocks.BasicBlock, 1, 8), Character.valueOf('D'), "gemDiamond"
		));
		BlockStateMachine.MachineType.LASER_AMPLIFIER.addRecipe(new ShapedMekanismRecipe(new ItemStack(MekanismBlocks.MachineBlock2, 1, 14),
			"SSS", "SED", "SSS", Character.valueOf('S'), "ingotSteel", Character.valueOf('E'), MekanismUtils.getEnergyCube(EnergyCubeTier.BASIC), Character.valueOf('D'), "gemDiamond"
		));
		BlockStateMachine.MachineType.LASER_TRACTOR_BEAM.addRecipe(new ShapedMekanismRecipe(new ItemStack(MekanismBlocks.MachineBlock2, 1, 15),
			"C", "F", Character.valueOf('C'), new ItemStack(MekanismBlocks.MachineBlock, 1, 13), Character.valueOf('F'), new ItemStack(MekanismBlocks.MachineBlock2, 1, 14)
		));
		CraftingManager.getInstance().getRecipeList().add(new ShapedMekanismRecipe(new ItemStack(MekanismBlocks.BasicBlock2, 1, 6),
			"SFS", "FAF", "SFS", Character.valueOf('S'), "ingotSteel", Character.valueOf('A'), MekanismItems.EnrichedAlloy, Character.valueOf('F'), Blocks.iron_bars
		));
		
		//Energy Cube recipes
		CraftingManager.getInstance().getRecipeList().add(new ShapedMekanismRecipe(MekanismUtils.getEnergyCube(EnergyCubeTier.BASIC),
			"RTR", "iIi", "RTR", Character.valueOf('R'), "alloyBasic", Character.valueOf('i'), "ingotIron", Character.valueOf('T'), MekanismItems.EnergyTablet.getUnchargedItem(), Character.valueOf('I'), new ItemStack(MekanismBlocks.BasicBlock, 1, 8)
		));
		CraftingManager.getInstance().getRecipeList().add(new ShapedMekanismRecipe(MekanismUtils.getEnergyCube(EnergyCubeTier.ADVANCED),
			"ETE", "oBo", "ETE", Character.valueOf('E'), "alloyAdvanced", Character.valueOf('o'), "ingotOsmium", Character.valueOf('T'), MekanismItems.EnergyTablet.getUnchargedItem(), Character.valueOf('B'), MekanismUtils.getEnergyCube(EnergyCubeTier.BASIC)
		));
		CraftingManager.getInstance().getRecipeList().add(new ShapedMekanismRecipe(MekanismUtils.getEnergyCube(EnergyCubeTier.ELITE),
			"RTR", "gAg", "RTR", Character.valueOf('R'), "alloyElite", Character.valueOf('g'), "ingotGold", Character.valueOf('T'), MekanismItems.EnergyTablet.getUnchargedItem(), Character.valueOf('A'), MekanismUtils.getEnergyCube(EnergyCubeTier.ADVANCED)
		));
		CraftingManager.getInstance().getRecipeList().add(new ShapedMekanismRecipe(MekanismUtils.getEnergyCube(EnergyCubeTier.ULTIMATE),
			"ATA", "dEd", "ATA", Character.valueOf('A'), "alloyUltimate", Character.valueOf('d'), "gemDiamond", Character.valueOf('T'), MekanismItems.EnergyTablet.getUnchargedItem(), Character.valueOf('E'), MekanismUtils.getEnergyCube(EnergyCubeTier.ELITE)
		));
		
		//Induction Cell recipes
		CraftingManager.getInstance().getRecipeList().add(new ShapedMekanismRecipe(MekanismUtils.getInductionCell(InductionCellTier.BASIC),
			"LTL", "TET", "LTL", Character.valueOf('T'), MekanismItems.EnergyTablet.getUnchargedItem(), Character.valueOf('E'), MekanismUtils.getEnergyCube(EnergyCubeTier.BASIC), Character.valueOf('L'), "dustLithium"
		));
		CraftingManager.getInstance().getRecipeList().add(new ShapedMekanismRecipe(MekanismUtils.getInductionCell(InductionCellTier.ADVANCED),
			"TCT", "CEC", "TCT", Character.valueOf('T'), MekanismItems.EnergyTablet.getUnchargedItem(), Character.valueOf('E'), MekanismUtils.getEnergyCube(EnergyCubeTier.ADVANCED), Character.valueOf('C'), MekanismUtils.getInductionCell(InductionCellTier.BASIC)
		));
		CraftingManager.getInstance().getRecipeList().add(new ShapedMekanismRecipe(MekanismUtils.getInductionCell(InductionCellTier.ELITE),
			"TCT", "CEC", "TCT", Character.valueOf('T'), MekanismItems.EnergyTablet.getUnchargedItem(), Character.valueOf('E'), MekanismUtils.getEnergyCube(EnergyCubeTier.ELITE), Character.valueOf('C'), MekanismUtils.getInductionCell(InductionCellTier.ADVANCED)
		));
		CraftingManager.getInstance().getRecipeList().add(new ShapedMekanismRecipe(MekanismUtils.getInductionCell(InductionCellTier.ULTIMATE),
			"TCT", "CEC", "TCT", Character.valueOf('T'), MekanismItems.EnergyTablet.getUnchargedItem(), Character.valueOf('E'), MekanismUtils.getEnergyCube(EnergyCubeTier.ULTIMATE), Character.valueOf('C'), MekanismUtils.getInductionCell(InductionCellTier.ELITE)
		));
		
		//Induction Provider recipes
		CraftingManager.getInstance().getRecipeList().add(new ShapedMekanismRecipe(MekanismUtils.getInductionProvider(InductionProviderTier.BASIC),
			"LCL", "CEC", "LCL", Character.valueOf('C'), MekanismUtils.getControlCircuit(BaseTier.BASIC), Character.valueOf('E'), MekanismUtils.getEnergyCube(EnergyCubeTier.BASIC), Character.valueOf('L'), "dustLithium"
		));
		CraftingManager.getInstance().getRecipeList().add(new ShapedMekanismRecipe(MekanismUtils.getInductionProvider(InductionProviderTier.ADVANCED),
			"CPC", "PEP", "CPC", Character.valueOf('C'), MekanismUtils.getControlCircuit(BaseTier.ADVANCED), Character.valueOf('E'), MekanismUtils.getEnergyCube(EnergyCubeTier.ADVANCED), Character.valueOf('P'), MekanismUtils.getInductionProvider(InductionProviderTier.BASIC)
		));
		CraftingManager.getInstance().getRecipeList().add(new ShapedMekanismRecipe(MekanismUtils.getInductionProvider(InductionProviderTier.ELITE),
			"CPC", "PEP", "CPC", Character.valueOf('C'), MekanismUtils.getControlCircuit(BaseTier.ELITE), Character.valueOf('E'), MekanismUtils.getEnergyCube(EnergyCubeTier.ELITE), Character.valueOf('P'), MekanismUtils.getInductionProvider(InductionProviderTier.ADVANCED)
		));
		CraftingManager.getInstance().getRecipeList().add(new ShapedMekanismRecipe(MekanismUtils.getInductionProvider(InductionProviderTier.ULTIMATE),
			"CPC", "PEP", "CPC", Character.valueOf('C'), MekanismUtils.getControlCircuit(BaseTier.ULTIMATE), Character.valueOf('E'), MekanismUtils.getEnergyCube(EnergyCubeTier.ULTIMATE), Character.valueOf('P'), MekanismUtils.getInductionProvider(InductionProviderTier.ELITE)
		));
		
		//Circuit recipes
		CraftingManager.getInstance().getRecipeList().add(new ShapedMekanismRecipe(new ItemStack(MekanismItems.ControlCircuit, 1, 1),
			"ECE", Character.valueOf('C'), new ItemStack(MekanismItems.ControlCircuit, 1, 0), Character.valueOf('E'), "alloyAdvanced"
		));
		CraftingManager.getInstance().getRecipeList().add(new ShapedMekanismRecipe(new ItemStack(MekanismItems.ControlCircuit, 1, 2),
			"RCR", Character.valueOf('C'), new ItemStack(MekanismItems.ControlCircuit, 1, 1), Character.valueOf('R'), "alloyElite"
		));
		CraftingManager.getInstance().getRecipeList().add(new ShapedMekanismRecipe(new ItemStack(MekanismItems.ControlCircuit, 1, 3),
			"ACA", Character.valueOf('C'), new ItemStack(MekanismItems.ControlCircuit, 1, 2), Character.valueOf('A'), "alloyUltimate"
		));

		//Factory recipes
		for(RecipeType type : RecipeType.values())
		{
			BlockStateMachine.MachineType.BASIC_FACTORY.addRecipe(new ShapedMekanismRecipe(MekanismUtils.getFactory(FactoryTier.BASIC, type),
				"RCR", "iOi", "RCR", Character.valueOf('R'), "alloyBasic", Character.valueOf('C'), MekanismUtils.getControlCircuit(BaseTier.BASIC), Character.valueOf('i'), "ingotIron", Character.valueOf('O'), type.getStack()
			));
			BlockStateMachine.MachineType.ADVANCED_FACTORY.addRecipe(new ShapedMekanismRecipe(MekanismUtils.getFactory(FactoryTier.ADVANCED, type),
				"ECE", "oOo", "ECE", Character.valueOf('E'), "alloyAdvanced", Character.valueOf('C'), MekanismUtils.getControlCircuit(BaseTier.ADVANCED), Character.valueOf('o'), "ingotOsmium", Character.valueOf('O'), MekanismUtils.getFactory(FactoryTier.BASIC, type)
			));
			BlockStateMachine.MachineType.ELITE_FACTORY.addRecipe(new ShapedMekanismRecipe(MekanismUtils.getFactory(FactoryTier.ELITE, type),
				"RCR", "gOg", "RCR", Character.valueOf('R'), "alloyElite", Character.valueOf('C'), MekanismUtils.getControlCircuit(BaseTier.ELITE), Character.valueOf('g'), "ingotGold", Character.valueOf('O'), MekanismUtils.getFactory(FactoryTier.ADVANCED, type)
			));
		}
		
		//Add the bin recipe system to the CraftingManager
		CraftingManager.getInstance().getRecipeList().add(new BinRecipe());
		
/*
        //Transmitters
		CraftingManager.getInstance().getRecipeList().add(new ShapedMekanismRecipe(new ItemStack(MekanismItems.PartTransmitter, 8, 0),
			"SRS", Character.valueOf('S'), "ingotSteel", Character.valueOf('R'), "dustRedstone"
		));
        CraftingManager.getInstance().getRecipeList().add(new ShapedMekanismRecipe(new ItemStack(MekanismItems.PartTransmitter, 8, 1),
            "TTT", "TET", "TTT", Character.valueOf('E'), "alloyAdvanced", Character.valueOf('T'), new ItemStack(MekanismItems.PartTransmitter, 1, 0)
        ));
        CraftingManager.getInstance().getRecipeList().add(new ShapedMekanismRecipe(new ItemStack(MekanismItems.PartTransmitter, 8, 2),
            "TTT", "TRT", "TTT", Character.valueOf('R'), "alloyElite", Character.valueOf('T'), new ItemStack(MekanismItems.PartTransmitter, 1, 1)
        ));
        CraftingManager.getInstance().getRecipeList().add(new ShapedMekanismRecipe(new ItemStack(MekanismItems.PartTransmitter, 8, 3),
            "TTT", "TAT", "TTT", Character.valueOf('A'), "alloyUltimate", Character.valueOf('T'), new ItemStack(MekanismItems.PartTransmitter, 1, 2)
        ));
        CraftingManager.getInstance().getRecipeList().add(new ShapedMekanismRecipe(new ItemStack(MekanismItems.PartTransmitter, 8, 4),
            "SBS", Character.valueOf('S'), "ingotSteel", Character.valueOf('B'), Items.bucket
        ));
		CraftingManager.getInstance().getRecipeList().add(new ShapedMekanismRecipe(new ItemStack(MekanismItems.PartTransmitter, 8, 5),
			"TTT", "TET", "TTT", Character.valueOf('E'), "alloyAdvanced", Character.valueOf('T'), new ItemStack(MekanismItems.PartTransmitter, 1, 4)
		));
		CraftingManager.getInstance().getRecipeList().add(new ShapedMekanismRecipe(new ItemStack(MekanismItems.PartTransmitter, 8, 6),
			"TTT", "TRT", "TTT", Character.valueOf('R'), "alloyElite", Character.valueOf('T'), new ItemStack(MekanismItems.PartTransmitter, 1, 5)
		));
		CraftingManager.getInstance().getRecipeList().add(new ShapedMekanismRecipe(new ItemStack(MekanismItems.PartTransmitter, 8, 7),
			"TTT", "TAT", "TTT", Character.valueOf('A'), "alloyUltimate", Character.valueOf('T'), new ItemStack(MekanismItems.PartTransmitter, 1, 6)
		));
        CraftingManager.getInstance().getRecipeList().add(new ShapedMekanismRecipe(new ItemStack(MekanismItems.PartTransmitter, 8, 8),
            "SGS", Character.valueOf('S'), "ingotSteel", Character.valueOf('G'), "blockGlass"
        ));
		CraftingManager.getInstance().getRecipeList().add(new ShapedMekanismRecipe(new ItemStack(MekanismItems.PartTransmitter, 8, 9),
			"TTT", "TET", "TTT", Character.valueOf('E'), "alloyAdvanced", Character.valueOf('T'), new ItemStack(MekanismItems.PartTransmitter, 1, 8)
		));
		CraftingManager.getInstance().getRecipeList().add(new ShapedMekanismRecipe(new ItemStack(MekanismItems.PartTransmitter, 8, 10),
			"TTT", "TRT", "TTT", Character.valueOf('R'), "alloyElite", Character.valueOf('T'), new ItemStack(MekanismItems.PartTransmitter, 1, 9)
		));
		CraftingManager.getInstance().getRecipeList().add(new ShapedMekanismRecipe(new ItemStack(MekanismItems.PartTransmitter, 8, 11),
			"TTT", "TAT", "TTT", Character.valueOf('A'), "alloyUltimate", Character.valueOf('T'), new ItemStack(MekanismItems.PartTransmitter, 1, 10)
		));
		CraftingManager.getInstance().getRecipeList().add(new ShapedMekanismRecipe(new ItemStack(MekanismItems.PartTransmitter, 8, 12),
			"SCS", Character.valueOf('S'), "ingotSteel", Character.valueOf('C'), MekanismUtils.getControlCircuit(BaseTier.BASIC)
		));
		CraftingManager.getInstance().getRecipeList().add(new ShapedMekanismRecipe(new ItemStack(MekanismItems.PartTransmitter, 8, 13),
			"TTT", "TET", "TTT", Character.valueOf('E'), "alloyAdvanced", Character.valueOf('T'), new ItemStack(MekanismItems.PartTransmitter, 1, 12)
		));
		CraftingManager.getInstance().getRecipeList().add(new ShapedMekanismRecipe(new ItemStack(MekanismItems.PartTransmitter, 8, 14),
			"TTT", "TRT", "TTT", Character.valueOf('R'), "alloyElite", Character.valueOf('T'), new ItemStack(MekanismItems.PartTransmitter, 1, 13)
		));
		CraftingManager.getInstance().getRecipeList().add(new ShapedMekanismRecipe(new ItemStack(MekanismItems.PartTransmitter, 8, 15),
			"TTT", "TAT", "TTT", Character.valueOf('A'), "alloyUltimate", Character.valueOf('T'), new ItemStack(MekanismItems.PartTransmitter, 1, 14)
		));
		CraftingManager.getInstance().getRecipeList().add(new ShapedMekanismRecipe(new ItemStack(MekanismItems.PartTransmitter, 2, 16),
			"SBS", Character.valueOf('S'), "ingotSteel", Character.valueOf('B'), Blocks.iron_bars
		));
		CraftingManager.getInstance().getRecipeList().add(new ShapedMekanismRecipe(new ItemStack(MekanismItems.PartTransmitter, 2, 17),
			"RRR", "SBS", "RRR", Character.valueOf('R'), "dustRedstone", Character.valueOf('S'), "ingotSteel", Character.valueOf('B'), Blocks.iron_bars
		));
		CraftingManager.getInstance().getRecipeList().add(new ShapedMekanismRecipe(new ItemStack(MekanismItems.PartTransmitter, 8, 18),
			"SCS", Character.valueOf('S'), "ingotSteel", Character.valueOf('C'), "ingotCopper"
		));
        CraftingManager.getInstance().getRecipeList().add(new ShapedMekanismRecipe(new ItemStack(MekanismItems.PartTransmitter, 8, 19),
            "TTT", "TET", "TTT", Character.valueOf('E'), "alloyAdvanced", Character.valueOf('T'), new ItemStack(MekanismItems.PartTransmitter, 1, 18)
        ));
        CraftingManager.getInstance().getRecipeList().add(new ShapedMekanismRecipe(new ItemStack(MekanismItems.PartTransmitter, 8, 20),
            "TTT", "TRT", "TTT", Character.valueOf('R'), "alloyElite", Character.valueOf('T'), new ItemStack(MekanismItems.PartTransmitter, 1, 19)
        ));
        CraftingManager.getInstance().getRecipeList().add(new ShapedMekanismRecipe(new ItemStack(MekanismItems.PartTransmitter, 8, 21),
            "TTT", "TAT", "TTT", Character.valueOf('A'), "alloyUltimate", Character.valueOf('T'), new ItemStack(MekanismItems.PartTransmitter, 1, 20)
        ));
*/

		//Plastic stuff
		CraftingManager.getInstance().getRecipeList().add(new ShapedMekanismRecipe(new ItemStack(MekanismItems.Polyethene, 1, 1),
			"PP", "PP", Character.valueOf('P'), new ItemStack(MekanismItems.Polyethene, 1, 0)
		));
		CraftingManager.getInstance().getRecipeList().add(new ShapedMekanismRecipe(new ItemStack(MekanismItems.Polyethene, 1, 2),
			"PPP", "P P", "PPP", Character.valueOf('P'), new ItemStack(MekanismItems.Polyethene, 1, 0)
		));
		CraftingManager.getInstance().getRecipeList().add(new ShapedMekanismRecipe(new ItemStack(MekanismItems.Polyethene, 1, 3),
			"R", "R", Character.valueOf('R'), new ItemStack(MekanismItems.Polyethene, 1, 1)
		));
		CraftingManager.getInstance().getRecipeList().add(new ShapedMekanismRecipe(new ItemStack(MekanismBlocks.PlasticBlock, 4, 15),
			"SSS", "S S", "SSS", Character.valueOf('S'), new ItemStack(MekanismItems.Polyethene, 1, 2)
		));
/*
		CraftingManager.getInstance().getRecipeList().add(new ShapedMekanismRecipe(new ItemStack(MekanismItems.GlowPanel, 2, 15),
			"PSP", "S S", "GSG", Character.valueOf('P'), "paneGlass", Character.valueOf('S'), new ItemStack(MekanismItems.Polyethene, 1, 2), Character.valueOf('G'), Items.glowstone_dust
		));
*/
		CraftingManager.getInstance().getRecipeList().add(new ShapedMekanismRecipe(new ItemStack(MekanismBlocks.PlasticFence, 3, 15),
			"BSB", "BSB", Character.valueOf('B'), new ItemStack(MekanismBlocks.PlasticBlock, 1, 15), Character.valueOf('S'), new ItemStack(MekanismItems.Polyethene, 1, 3)
		));

		for(int i = 0; i < EnumColor.DYES.length-1; i++)
		{
			CraftingManager.getInstance().getRecipeList().add(new ShapedMekanismRecipe(new ItemStack(MekanismBlocks.PlasticBlock, 4, i),
				"SSS", "SDS", "SSS", Character.valueOf('S'), new ItemStack(MekanismItems.Polyethene, 1, 2), Character.valueOf('D'), "dye" + EnumColor.DYES[i].dyeName
			));
/*
			CraftingManager.getInstance().getRecipeList().add(new ShapedMekanismRecipe(new ItemStack(MekanismItems.GlowPanel, 2, i),
				"PSP", "SDS", "GSG", Character.valueOf('P'), "paneGlass", Character.valueOf('S'), new ItemStack(MekanismItems.Polyethene, 1, 2), Character.valueOf('D'), "dye" + EnumColor.DYES[i].dyeName, Character.valueOf('G'), Items.glowstone_dust
			));
*/
		}

		for(int i = 0; i < EnumColor.DYES.length; i++)
        {
			CraftingManager.getInstance().getRecipeList().add(new ShapelessMekanismRecipe(new ItemStack(MekanismItems.Balloon, 2, i),
				Items.leather, Items.string, "dye" + EnumColor.DYES[i].dyeName
			));

			for(int j = 0; j < EnumColor.DYES.length; j++)
			{
				CraftingManager.getInstance().getRecipeList().add(new ShapelessMekanismRecipe(new ItemStack(MekanismItems.Balloon, 1, i),
					new ItemStack(MekanismItems.Balloon, 1, j), "dye" + EnumColor.DYES[i].dyeName
				));

				CraftingManager.getInstance().getRecipeList().add(new ShapedMekanismRecipe(new ItemStack(MekanismBlocks.PlasticBlock, 4, i),
					" P ", "PDP", " P ", Character.valueOf('P'), new ItemStack(MekanismBlocks.PlasticBlock, 1, j), Character.valueOf('D'), "dye" + EnumColor.DYES[i].dyeName
				));
				CraftingManager.getInstance().getRecipeList().add(new ShapedMekanismRecipe(new ItemStack(MekanismBlocks.SlickPlasticBlock, 4, i),
					" P ", "PDP", " P ", Character.valueOf('P'), new ItemStack(MekanismBlocks.SlickPlasticBlock, 1, j), Character.valueOf('D'), "dye" + EnumColor.DYES[i].dyeName
				));
				CraftingManager.getInstance().getRecipeList().add(new ShapedMekanismRecipe(new ItemStack(MekanismBlocks.GlowPlasticBlock, 4, i),
					" P ", "PDP", " P ", Character.valueOf('P'), new ItemStack(MekanismBlocks.GlowPlasticBlock, 1, j), Character.valueOf('D'), "dye" + EnumColor.DYES[i].dyeName
				));
				CraftingManager.getInstance().getRecipeList().add(new ShapedMekanismRecipe(new ItemStack(MekanismBlocks.ReinforcedPlasticBlock, 4, i),
					" P ", "PDP", " P ", Character.valueOf('P'), new ItemStack(MekanismBlocks.ReinforcedPlasticBlock, 1, j), Character.valueOf('D'), "dye" + EnumColor.DYES[i].dyeName
				));
/*
				CraftingManager.getInstance().getRecipeList().add(new ShapedMekanismRecipe(new ItemStack(MekanismItems.GlowPanel, 4, i),
					" P ", "PDP", " P ", Character.valueOf('P'), new ItemStack(MekanismItems.GlowPanel, 1, j), Character.valueOf('D'), "dye" + EnumColor.DYES[i].dyeName
				));
*/
			}

			CraftingManager.getInstance().getRecipeList().add(new ShapelessMekanismRecipe(new ItemStack(MekanismBlocks.GlowPlasticBlock, 3, i),
				new ItemStack(MekanismBlocks.PlasticBlock, 1, i), new ItemStack(MekanismBlocks.PlasticBlock, 1, i), new ItemStack(MekanismBlocks.PlasticBlock, 1, i), new ItemStack(Items.glowstone_dust)
			));
			CraftingManager.getInstance().getRecipeList().add(new ShapedMekanismRecipe(new ItemStack(MekanismBlocks.ReinforcedPlasticBlock, 4, i),
				" P ", "POP", " P ", Character.valueOf('P'), new ItemStack(MekanismBlocks.PlasticBlock, 1, i), Character.valueOf('O'), "dustOsmium"
			));
			CraftingManager.getInstance().getRecipeList().add(new ShapedMekanismRecipe(new ItemStack(MekanismBlocks.RoadPlasticBlock, 3, i),
				"SSS", "PPP", "SSS", Character.valueOf('S'), Blocks.sand, Character.valueOf('P'), new ItemStack(MekanismBlocks.SlickPlasticBlock, 1, i)
			));
        }
	
		//Furnace Recipes
		FurnaceRecipes.instance().addSmeltingRecipe(new ItemStack(MekanismBlocks.OreBlock, 1, 0), new ItemStack(MekanismItems.Ingot, 1, 1), 1.0F);
		FurnaceRecipes.instance().addSmeltingRecipe(new ItemStack(MekanismBlocks.OreBlock, 1, 1), new ItemStack(MekanismItems.Ingot, 1, 5), 1.0F);
		FurnaceRecipes.instance().addSmeltingRecipe(new ItemStack(MekanismBlocks.OreBlock, 1, 2), new ItemStack(MekanismItems.Ingot, 1, 6), 1.0F);
		FurnaceRecipes.instance().addSmeltingRecipe(new ItemStack(MekanismItems.Dust, 1, Resource.OSMIUM.ordinal()), new ItemStack(MekanismItems.Ingot, 1, 1), 0.0F);
		FurnaceRecipes.instance().addSmeltingRecipe(new ItemStack(MekanismItems.Dust, 1, Resource.IRON.ordinal()), new ItemStack(Items.iron_ingot), 0.0F);
		FurnaceRecipes.instance().addSmeltingRecipe(new ItemStack(MekanismItems.Dust, 1, Resource.GOLD.ordinal()), new ItemStack(Items.gold_ingot), 0.0F);
		FurnaceRecipes.instance().addSmeltingRecipe(new ItemStack(MekanismItems.OtherDust, 1, 1), new ItemStack(MekanismItems.Ingot, 1, 4), 0.0F);
		FurnaceRecipes.instance().addSmeltingRecipe(new ItemStack(MekanismItems.Dust, 1, Resource.COPPER.ordinal()), new ItemStack(MekanismItems.Ingot, 1, 5), 0.0F);
		FurnaceRecipes.instance().addSmeltingRecipe(new ItemStack(MekanismItems.Dust, 1, Resource.TIN.ordinal()), new ItemStack(MekanismItems.Ingot, 1, 6), 0.0F);
		
		//Enrichment Chamber Recipes
		RecipeHandler.addEnrichmentChamberRecipe(new ItemStack(Blocks.redstone_ore), new ItemStack(Items.redstone, 12));
        RecipeHandler.addEnrichmentChamberRecipe(new ItemStack(Blocks.obsidian), new ItemStack(MekanismItems.OtherDust, 2, 6));
		RecipeHandler.addEnrichmentChamberRecipe(new ItemStack(Items.coal, 1, 0), new ItemStack(MekanismItems.CompressedCarbon));
		RecipeHandler.addEnrichmentChamberRecipe(new ItemStack(Items.coal, 1, 1), new ItemStack(MekanismItems.CompressedCarbon));
		RecipeHandler.addEnrichmentChamberRecipe(new ItemStack(Items.redstone), new ItemStack(MekanismItems.CompressedRedstone));
		RecipeHandler.addEnrichmentChamberRecipe(new ItemStack(Blocks.lapis_ore), new ItemStack(Items.dye, 12, 4));
		RecipeHandler.addEnrichmentChamberRecipe(new ItemStack(Blocks.coal_ore), new ItemStack(Items.coal, 2));
		RecipeHandler.addEnrichmentChamberRecipe(new ItemStack(Blocks.diamond_ore), new ItemStack(Items.diamond, 2));
		RecipeHandler.addEnrichmentChamberRecipe(new ItemStack(Blocks.mossy_cobblestone), new ItemStack(Blocks.cobblestone));
		RecipeHandler.addEnrichmentChamberRecipe(new ItemStack(Blocks.stone), new ItemStack(Blocks.stonebrick, 1, 2));
		RecipeHandler.addEnrichmentChamberRecipe(new ItemStack(Blocks.sand), new ItemStack(Blocks.gravel));
		RecipeHandler.addEnrichmentChamberRecipe(new ItemStack(Blocks.gravel), new ItemStack(Blocks.cobblestone));
		RecipeHandler.addEnrichmentChamberRecipe(new ItemStack(Items.gunpowder), new ItemStack(Items.flint));
		RecipeHandler.addEnrichmentChamberRecipe(new ItemStack(Blocks.stonebrick, 1, 2), new ItemStack(Blocks.stonebrick, 1, 0));
		RecipeHandler.addEnrichmentChamberRecipe(new ItemStack(Blocks.stonebrick, 1, 0), new ItemStack(Blocks.stonebrick, 1, 3));
		RecipeHandler.addEnrichmentChamberRecipe(new ItemStack(Blocks.stonebrick, 1, 1), new ItemStack(Blocks.stonebrick, 1, 0));
		RecipeHandler.addEnrichmentChamberRecipe(new ItemStack(Blocks.glowstone), new ItemStack(Items.glowstone_dust, 4));
		RecipeHandler.addEnrichmentChamberRecipe(new ItemStack(Blocks.clay), new ItemStack(Items.clay_ball, 4));
		RecipeHandler.addEnrichmentChamberRecipe(new ItemStack(MekanismBlocks.SaltBlock), new ItemStack(MekanismItems.Salt, 4));
		RecipeHandler.addEnrichmentChamberRecipe(new ItemStack(Items.diamond), new ItemStack(MekanismItems.CompressedDiamond));
		RecipeHandler.addEnrichmentChamberRecipe(new ItemStack(MekanismItems.Polyethene, 3, 0), new ItemStack(MekanismItems.Polyethene, 1, 2));
		
		for(int i = 0; i < EnumColor.DYES.length; i++)
		{
			RecipeHandler.addEnrichmentChamberRecipe(new ItemStack(MekanismBlocks.PlasticBlock, 1, i), new ItemStack(MekanismBlocks.SlickPlasticBlock, 1, i));
		}
		
		//Combiner recipes
		RecipeHandler.addCombinerRecipe(new ItemStack(Items.redstone, 16), new ItemStack(Blocks.redstone_ore));
		RecipeHandler.addCombinerRecipe(new ItemStack(Items.dye, 16, 4), new ItemStack(Blocks.lapis_ore));
		RecipeHandler.addCombinerRecipe(new ItemStack(Items.flint), new ItemStack(Blocks.gravel));
		
		//Osmium Compressor Recipes
		RecipeHandler.addOsmiumCompressorRecipe(new ItemStack(Items.glowstone_dust), new ItemStack(MekanismItems.Ingot, 1, 3));
		
		//Crusher Recipes
		RecipeHandler.addCrusherRecipe(new ItemStack(Items.diamond), new ItemStack(MekanismItems.OtherDust, 1, 0));
        RecipeHandler.addCrusherRecipe(new ItemStack(Items.iron_ingot), new ItemStack(MekanismItems.Dust, 1, Resource.IRON.ordinal()));
        RecipeHandler.addCrusherRecipe(new ItemStack(Items.gold_ingot), new ItemStack(MekanismItems.Dust, 1, Resource.GOLD.ordinal()));
        RecipeHandler.addCrusherRecipe(new ItemStack(Blocks.gravel), new ItemStack(Blocks.sand));
        RecipeHandler.addCrusherRecipe(new ItemStack(Blocks.stone), new ItemStack(Blocks.cobblestone));
        RecipeHandler.addCrusherRecipe(new ItemStack(Blocks.cobblestone), new ItemStack(Blocks.gravel));
        RecipeHandler.addCrusherRecipe(new ItemStack(Blocks.stonebrick, 1, 2), new ItemStack(Blocks.stone));
        RecipeHandler.addCrusherRecipe(new ItemStack(Blocks.stonebrick, 1, 0), new ItemStack(Blocks.stonebrick, 1, 2));
        RecipeHandler.addCrusherRecipe(new ItemStack(Blocks.stonebrick, 1, 3), new ItemStack(Blocks.stonebrick, 1, 0));
        RecipeHandler.addCrusherRecipe(new ItemStack(Items.flint), new ItemStack(Items.gunpowder));
        RecipeHandler.addCrusherRecipe(new ItemStack(Blocks.sandstone), new ItemStack(Blocks.sand, 2));
        
        for(int i = 0; i < 16; i++)
        {
        	RecipeHandler.addCrusherRecipe(new ItemStack(Blocks.wool, 1, i), new ItemStack(Items.string, 4));
        }
        
		//BioFuel Crusher Recipes
		RecipeHandler.addCrusherRecipe(new ItemStack(Blocks.tallgrass), new ItemStack(MekanismItems.BioFuel, 4));
		RecipeHandler.addCrusherRecipe(new ItemStack(Items.reeds), new ItemStack(MekanismItems.BioFuel, 2));
		RecipeHandler.addCrusherRecipe(new ItemStack(Items.wheat_seeds), new ItemStack(MekanismItems.BioFuel, 2));
		RecipeHandler.addCrusherRecipe(new ItemStack(Items.wheat), new ItemStack(MekanismItems.BioFuel, 4));
		RecipeHandler.addCrusherRecipe(new ItemStack(Items.pumpkin_seeds), new ItemStack(MekanismItems.BioFuel, 2));
		RecipeHandler.addCrusherRecipe(new ItemStack(Items.melon_seeds), new ItemStack(MekanismItems.BioFuel, 2));
		RecipeHandler.addCrusherRecipe(new ItemStack(Items.apple), new ItemStack(MekanismItems.BioFuel, 4));
		RecipeHandler.addCrusherRecipe(new ItemStack(Items.bread), new ItemStack(MekanismItems.BioFuel, 4));
		RecipeHandler.addCrusherRecipe(new ItemStack(Items.potato), new ItemStack(MekanismItems.BioFuel, 4));
		RecipeHandler.addCrusherRecipe(new ItemStack(Items.carrot), new ItemStack(MekanismItems.BioFuel, 4));
		RecipeHandler.addCrusherRecipe(new ItemStack(Items.rotten_flesh), new ItemStack(MekanismItems.BioFuel, 2));
		RecipeHandler.addCrusherRecipe(new ItemStack(Items.melon), new ItemStack(MekanismItems.BioFuel, 4));
		RecipeHandler.addCrusherRecipe(new ItemStack(Blocks.pumpkin), new ItemStack(MekanismItems.BioFuel, 6));

		//Purification Chamber Recipes
        RecipeHandler.addPurificationChamberRecipe(new ItemStack(Blocks.gravel), new ItemStack(Items.flint));
        
        //Chemical Injection Chamber Recipes
        RecipeHandler.addChemicalInjectionChamberRecipe(new ItemStack(Blocks.dirt), "water", new ItemStack(Blocks.clay));
        RecipeHandler.addChemicalInjectionChamberRecipe(new ItemStack(Blocks.hardened_clay), "water", new ItemStack(Blocks.clay));
        RecipeHandler.addChemicalInjectionChamberRecipe(new ItemStack(Items.brick), "water", new ItemStack(Items.clay_ball));
        RecipeHandler.addChemicalInjectionChamberRecipe(new ItemStack(Items.gunpowder), "hydrogenChloride", new ItemStack(MekanismItems.OtherDust, 1, 3));
		
		//Precision Sawmill Recipes
		RecipeHandler.addPrecisionSawmillRecipe(new ItemStack(Blocks.ladder, 3), new ItemStack(Items.stick, 7));
		RecipeHandler.addPrecisionSawmillRecipe(new ItemStack(Blocks.torch, 4), new ItemStack(Items.stick), new ItemStack(Items.coal), 1);
		RecipeHandler.addPrecisionSawmillRecipe(new ItemStack(Blocks.chest), new ItemStack(Blocks.planks, 8));
		RecipeHandler.addPrecisionSawmillRecipe(new ItemStack(Blocks.trapdoor), new ItemStack(Blocks.planks, 3));
		RecipeHandler.addPrecisionSawmillRecipe(new ItemStack(Items.boat), new ItemStack(Blocks.planks, 5));
		RecipeHandler.addPrecisionSawmillRecipe(new ItemStack(Items.bed), new ItemStack(Blocks.planks, 3), new ItemStack(Blocks.wool, 3), 1);
		RecipeHandler.addPrecisionSawmillRecipe(new ItemStack(Items.oak_door), new ItemStack(Blocks.planks, 6, 0));
		RecipeHandler.addPrecisionSawmillRecipe(new ItemStack(Items.spruce_door), new ItemStack(Blocks.planks, 6, 1));
		RecipeHandler.addPrecisionSawmillRecipe(new ItemStack(Items.birch_door), new ItemStack(Blocks.planks, 6, 2));
		RecipeHandler.addPrecisionSawmillRecipe(new ItemStack(Items.jungle_door), new ItemStack(Blocks.planks, 6, 3));
		RecipeHandler.addPrecisionSawmillRecipe(new ItemStack(Items.acacia_door), new ItemStack(Blocks.planks, 6, 4));
		RecipeHandler.addPrecisionSawmillRecipe(new ItemStack(Items.dark_oak_door), new ItemStack(Blocks.planks, 6, 5));
		RecipeHandler.addPrecisionSawmillRecipe(new ItemStack(Blocks.jukebox), new ItemStack(Blocks.planks, 8), new ItemStack(Items.diamond), 1);
		RecipeHandler.addPrecisionSawmillRecipe(new ItemStack(Blocks.bookshelf), new ItemStack(Blocks.planks, 6), new ItemStack(Items.book, 3), 1);
		RecipeHandler.addPrecisionSawmillRecipe(new ItemStack(Blocks.wooden_pressure_plate), new ItemStack(Blocks.planks, 2));
		RecipeHandler.addPrecisionSawmillRecipe(new ItemStack(Blocks.oak_fence), new ItemStack(Items.stick, 3));
		RecipeHandler.addPrecisionSawmillRecipe(new ItemStack(Blocks.spruce_fence), new ItemStack(Items.stick, 3));
		RecipeHandler.addPrecisionSawmillRecipe(new ItemStack(Blocks.birch_fence), new ItemStack(Items.stick, 3));
		RecipeHandler.addPrecisionSawmillRecipe(new ItemStack(Blocks.jungle_fence), new ItemStack(Items.stick, 3));
		RecipeHandler.addPrecisionSawmillRecipe(new ItemStack(Blocks.acacia_fence), new ItemStack(Items.stick, 3));
		RecipeHandler.addPrecisionSawmillRecipe(new ItemStack(Blocks.dark_oak_fence), new ItemStack(Items.stick, 3));
		RecipeHandler.addPrecisionSawmillRecipe(new ItemStack(Blocks.oak_fence_gate), new ItemStack(Blocks.planks, 2, 0), new ItemStack(Items.stick, 4), 1);
		RecipeHandler.addPrecisionSawmillRecipe(new ItemStack(Blocks.spruce_fence_gate), new ItemStack(Blocks.planks, 2, 1), new ItemStack(Items.stick, 4), 1);
		RecipeHandler.addPrecisionSawmillRecipe(new ItemStack(Blocks.birch_fence_gate), new ItemStack(Blocks.planks, 2, 2), new ItemStack(Items.stick, 4), 1);
		RecipeHandler.addPrecisionSawmillRecipe(new ItemStack(Blocks.jungle_fence_gate), new ItemStack(Blocks.planks, 2, 3), new ItemStack(Items.stick, 4), 1);
		RecipeHandler.addPrecisionSawmillRecipe(new ItemStack(Blocks.acacia_fence_gate), new ItemStack(Blocks.planks, 2, 4), new ItemStack(Items.stick, 4), 1);
		RecipeHandler.addPrecisionSawmillRecipe(new ItemStack(Blocks.dark_oak_fence_gate), new ItemStack(Blocks.planks, 2, 5), new ItemStack(Items.stick, 4), 1);
		RecipeHandler.addPrecisionSawmillRecipe(new ItemStack(Blocks.noteblock), new ItemStack(Blocks.planks, 8), new ItemStack(Items.redstone), 1);
		RecipeHandler.addPrecisionSawmillRecipe(new ItemStack(Blocks.redstone_torch), new ItemStack(Items.stick), new ItemStack(Items.redstone), 1);
		RecipeHandler.addPrecisionSawmillRecipe(new ItemStack(Blocks.crafting_table), new ItemStack(Blocks.planks, 4));
		
        //Metallurgic Infuser Recipes
        RecipeHandler.addMetallurgicInfuserRecipe(InfuseRegistry.get("CARBON"), 10, new ItemStack(Items.iron_ingot), new ItemStack(MekanismItems.EnrichedIron));
        RecipeHandler.addMetallurgicInfuserRecipe(InfuseRegistry.get("CARBON"), 10, new ItemStack(MekanismItems.EnrichedIron), new ItemStack(MekanismItems.OtherDust, 1, 1));
        RecipeHandler.addMetallurgicInfuserRecipe(InfuseRegistry.get("REDSTONE"), 10, new ItemStack(Items.iron_ingot), new ItemStack(MekanismItems.EnrichedAlloy));
        RecipeHandler.addMetallurgicInfuserRecipe(InfuseRegistry.get("FUNGI"), 10, new ItemStack(Blocks.dirt), new ItemStack(Blocks.mycelium));
        RecipeHandler.addMetallurgicInfuserRecipe(InfuseRegistry.get("BIO"), 10, new ItemStack(Blocks.cobblestone), new ItemStack(Blocks.mossy_cobblestone));
        RecipeHandler.addMetallurgicInfuserRecipe(InfuseRegistry.get("BIO"), 10, new ItemStack(Blocks.stonebrick, 1, 0), new ItemStack(Blocks.stonebrick, 1, 1));
        RecipeHandler.addMetallurgicInfuserRecipe(InfuseRegistry.get("BIO"), 10, new ItemStack(Blocks.sand), new ItemStack(Blocks.dirt));
        RecipeHandler.addMetallurgicInfuserRecipe(InfuseRegistry.get("BIO"), 10, new ItemStack(Blocks.dirt), new ItemStack(Blocks.dirt, 1, 2));
        RecipeHandler.addMetallurgicInfuserRecipe(InfuseRegistry.get("DIAMOND"), 10, new ItemStack(MekanismItems.EnrichedAlloy), new ItemStack(MekanismItems.ReinforcedAlloy));
        RecipeHandler.addMetallurgicInfuserRecipe(InfuseRegistry.get("OBSIDIAN"), 10, new ItemStack(MekanismItems.ReinforcedAlloy), new ItemStack(MekanismItems.AtomicAlloy));
        
        //Chemical Infuser Recipes
        RecipeHandler.addChemicalInfuserRecipe(new GasStack(GasRegistry.getGas("oxygen"), 1), new GasStack(GasRegistry.getGas("sulfurDioxideGas"), 2), new GasStack(GasRegistry.getGas("sulfurTrioxideGas"), 2));
		RecipeHandler.addChemicalInfuserRecipe(new GasStack(GasRegistry.getGas("sulfurTrioxideGas"), 1), new GasStack(GasRegistry.getGas("water"), 1), new GasStack(GasRegistry.getGas("sulfuricAcid"), 1));
		RecipeHandler.addChemicalInfuserRecipe(new GasStack(GasRegistry.getGas("hydrogen"), 1), new GasStack(GasRegistry.getGas("chlorine"), 1), new GasStack(GasRegistry.getGas("hydrogenChloride"), 1));
		RecipeHandler.addChemicalInfuserRecipe(new GasStack(GasRegistry.getGas("deuterium"), 1), new GasStack(GasRegistry.getGas("tritium"), 1), new GasStack(GasRegistry.getGas("fusionFuelDT"), 2));

		//Electrolytic Separator Recipes
		RecipeHandler.addElectrolyticSeparatorRecipe(FluidRegistry.getFluidStack("water", 2), 2 * general.FROM_H2, new GasStack(GasRegistry.getGas("hydrogen"), 2), new GasStack(GasRegistry.getGas("oxygen"), 1));
		RecipeHandler.addElectrolyticSeparatorRecipe(FluidRegistry.getFluidStack("brine", 10), 2 * general.FROM_H2, new GasStack(GasRegistry.getGas("sodium"), 1), new GasStack(GasRegistry.getGas("chlorine"), 1));
		RecipeHandler.addElectrolyticSeparatorRecipe(FluidRegistry.getFluidStack("heavywater", 2), usage.heavyWaterElectrolysisUsage, new GasStack(GasRegistry.getGas("deuterium"), 2), new GasStack(GasRegistry.getGas("oxygen"), 1));
		
		//Solar Evaporation Plant Recipes
		RecipeHandler.addSolarEvaporationRecipe(FluidRegistry.getFluidStack("water", 10), FluidRegistry.getFluidStack("brine", 1));
		RecipeHandler.addSolarEvaporationRecipe(FluidRegistry.getFluidStack("brine", 10), FluidRegistry.getFluidStack("lithium", 1));
		
		//Chemical Crystallizer Recipes
		RecipeHandler.addChemicalCrystallizerRecipe(new GasStack(GasRegistry.getGas("lithium"), 100), new ItemStack(MekanismItems.OtherDust, 1, 4));
		RecipeHandler.addChemicalCrystallizerRecipe(new GasStack(GasRegistry.getGas("brine"), 15), new ItemStack(MekanismItems.Salt));
		
		//T4 Processing Recipes
		for(Gas gas : GasRegistry.getRegisteredGasses())
		{
			if(gas instanceof OreGas && !((OreGas)gas).isClean())
			{
				OreGas oreGas = (OreGas)gas;
				
				RecipeHandler.addChemicalWasherRecipe(new GasStack(oreGas, 1), new GasStack(oreGas.getCleanGas(), 1));
				RecipeHandler.addChemicalCrystallizerRecipe(new GasStack(oreGas.getCleanGas(), 200), new ItemStack(MekanismItems.Crystal, 1, Resource.getFromName(oreGas.getName()).ordinal()));
			}
		}

		//Pressurized Reaction Chamber Recipes
		RecipeHandler.addPRCRecipe(
				new ItemStack(MekanismItems.BioFuel, 2), new FluidStack(FluidRegistry.WATER, 10), new GasStack(GasRegistry.getGas("hydrogen"), 100),
				new ItemStack(MekanismItems.Substrate), new GasStack(GasRegistry.getGas("ethene"), 100),
				0,
				100
		);
		RecipeHandler.addPRCRecipe(
				new ItemStack(MekanismItems.Substrate), new FluidStack(FluidRegistry.getFluid("ethene"), 50), new GasStack(GasRegistry.getGas("oxygen"), 10),
				new ItemStack(MekanismItems.Polyethene), new GasStack(GasRegistry.getGas("oxygen"), 5),
				1000,
				60
		);
		RecipeHandler.addPRCRecipe(
				new ItemStack(MekanismItems.Substrate), new FluidStack(FluidRegistry.WATER, 200), new GasStack(GasRegistry.getGas("ethene"), 100),
				new ItemStack(MekanismItems.Substrate, 8), new GasStack(GasRegistry.getGas("oxygen"), 10),
				200,
				400
		);
		
		//Solar Neutron Activator Recipes
		RecipeHandler.addSolarNeutronRecipe(new GasStack(GasRegistry.getGas("lithium"), 1), new GasStack(GasRegistry.getGas("tritium"), 1));

        //Infuse objects
		InfuseRegistry.registerInfuseObject(new ItemStack(MekanismItems.BioFuel), new InfuseObject(InfuseRegistry.get("BIO"), 5));
		InfuseRegistry.registerInfuseObject(new ItemStack(Items.coal, 1, 0), new InfuseObject(InfuseRegistry.get("CARBON"), 10));
        InfuseRegistry.registerInfuseObject(new ItemStack(Items.coal, 1, 1), new InfuseObject(InfuseRegistry.get("CARBON"), 20));
        InfuseRegistry.registerInfuseObject(new ItemStack(MekanismItems.CompressedCarbon), new InfuseObject(InfuseRegistry.get("CARBON"), 100));
        InfuseRegistry.registerInfuseObject(new ItemStack(Items.redstone), new InfuseObject(InfuseRegistry.get("REDSTONE"), 10));
        InfuseRegistry.registerInfuseObject(new ItemStack(Blocks.redstone_block), new InfuseObject(InfuseRegistry.get("REDSTONE"), 90));
        InfuseRegistry.registerInfuseObject(new ItemStack(MekanismItems.CompressedRedstone), new InfuseObject(InfuseRegistry.get("REDSTONE"), 100));
        InfuseRegistry.registerInfuseObject(new ItemStack(Blocks.red_mushroom), new InfuseObject(InfuseRegistry.get("FUNGI"), 10));
        InfuseRegistry.registerInfuseObject(new ItemStack(Blocks.brown_mushroom), new InfuseObject(InfuseRegistry.get("FUNGI"), 10));
        InfuseRegistry.registerInfuseObject(new ItemStack(MekanismItems.CompressedDiamond), new InfuseObject(InfuseRegistry.get("DIAMOND"), 100));
        InfuseRegistry.registerInfuseObject(new ItemStack(MekanismItems.CompressedObsidian), new InfuseObject(InfuseRegistry.get("OBSIDIAN"), 100));
        
        //Fuels
        GameRegistry.registerFuelHandler(new IFuelHandler() {
			@Override
			public int getBurnTime(ItemStack fuel)
			{
				if(fuel.isItemEqual(new ItemStack(MekanismBlocks.BasicBlock, 1, 3)))
				{
					return 200*8*9;
				}
				
				return 0;
			}
		});

		//Fuel Gases
		FuelHandler.addGas(GasRegistry.getGas("hydrogen"), 1, general.FROM_H2);
		
		//RecipeSorter registrations
		RecipeSorter.register("mekanism_shaped", ShapedMekanismRecipe.class, Category.SHAPED, "");
		RecipeSorter.register("mekanism_shapeless", ShapelessMekanismRecipe.class, Category.SHAPELESS, "");
		RecipeSorter.register("bin", BinRecipe.class, Category.SHAPELESS, "");
	}

	/**
	 * Registers specified items with the Ore Dictionary.
	 */
	public void registerOreDict()
	{
		//Add specific items to ore dictionary for recipe usage in other mods.
//		OreDictionary.registerOre("universalCable", new ItemStack(MekanismItems.PartTransmitter, 8, 0));
		OreDictionary.registerOre("battery", MekanismItems.EnergyTablet.getUnchargedItem());
		OreDictionary.registerOre("pulpWood", MekanismItems.Sawdust);
		OreDictionary.registerOre("dustWood", MekanismItems.Sawdust);
		OreDictionary.registerOre("blockSalt", MekanismBlocks.SaltBlock);
		
		//Alloys!
		OreDictionary.registerOre("alloyBasic", new ItemStack(Items.redstone));
		OreDictionary.registerOre("alloyAdvanced", new ItemStack(MekanismItems.EnrichedAlloy));
		OreDictionary.registerOre("alloyElite", new ItemStack(MekanismItems.ReinforcedAlloy));
		OreDictionary.registerOre("alloyUltimate", new ItemStack(MekanismItems.AtomicAlloy));
		
		//GregoriousT?
		OreDictionary.registerOre("itemSalt", MekanismItems.Salt);
		OreDictionary.registerOre("dustSalt", MekanismItems.Salt);
		
		OreDictionary.registerOre("dustDiamond", new ItemStack(MekanismItems.OtherDust, 1, 0));
		OreDictionary.registerOre("dustSteel", new ItemStack(MekanismItems.OtherDust, 1, 1));
		OreDictionary.registerOre("dustLead", new ItemStack(MekanismItems.OtherDust, 1, 2));
		OreDictionary.registerOre("dustSulfur", new ItemStack(MekanismItems.OtherDust, 1, 3));
		OreDictionary.registerOre("dustLithium", new ItemStack(MekanismItems.OtherDust, 1, 4));
		OreDictionary.registerOre("dustRefinedObsidian", new ItemStack(MekanismItems.OtherDust, 1, 5));
		OreDictionary.registerOre("dustObsidian", new ItemStack(MekanismItems.OtherDust, 1, 6));
		
		OreDictionary.registerOre("ingotRefinedObsidian", new ItemStack(MekanismItems.Ingot, 1, 0));
		OreDictionary.registerOre("ingotOsmium", new ItemStack(MekanismItems.Ingot, 1, 1));
		OreDictionary.registerOre("ingotBronze", new ItemStack(MekanismItems.Ingot, 1, 2));
		OreDictionary.registerOre("ingotRefinedGlowstone", new ItemStack(MekanismItems.Ingot, 1, 3));
		OreDictionary.registerOre("ingotSteel", new ItemStack(MekanismItems.Ingot, 1, 4));
		OreDictionary.registerOre("ingotCopper", new ItemStack(MekanismItems.Ingot, 1, 5));
		OreDictionary.registerOre("ingotTin", new ItemStack(MekanismItems.Ingot, 1, 6));
		
		OreDictionary.registerOre("blockOsmium", new ItemStack(MekanismBlocks.BasicBlock, 1, 0));
		OreDictionary.registerOre("blockBronze", new ItemStack(MekanismBlocks.BasicBlock, 1, 1));
		OreDictionary.registerOre("blockRefinedObsidian", new ItemStack(MekanismBlocks.BasicBlock, 1, 2));
		OreDictionary.registerOre("blockCharcoal", new ItemStack(MekanismBlocks.BasicBlock, 1, 3));
		OreDictionary.registerOre("blockRefinedGlowstone", new ItemStack(MekanismBlocks.BasicBlock, 1, 4));
		OreDictionary.registerOre("blockSteel", new ItemStack(MekanismBlocks.BasicBlock, 1, 5));
		OreDictionary.registerOre("blockCopper", new ItemStack(MekanismBlocks.BasicBlock, 1, 12));
		OreDictionary.registerOre("blockTin", new ItemStack(MekanismBlocks.BasicBlock, 1, 13));
		
		for(Resource resource : Resource.values())
		{
			OreDictionary.registerOre("dust" + resource.getName(), new ItemStack(MekanismItems.Dust, 1, resource.ordinal()));
			OreDictionary.registerOre("dustDirty" + resource.getName(), new ItemStack(MekanismItems.DirtyDust, 1, resource.ordinal()));
			OreDictionary.registerOre("clump" + resource.getName(), new ItemStack(MekanismItems.Clump, 1, resource.ordinal()));
			OreDictionary.registerOre("shard" + resource.getName(), new ItemStack(MekanismItems.Shard, 1, resource.ordinal()));
			OreDictionary.registerOre("crystal" + resource.getName(), new ItemStack(MekanismItems.Crystal, 1, resource.ordinal()));
		}
		
		OreDictionary.registerOre("oreOsmium", new ItemStack(MekanismBlocks.OreBlock, 1, 0));
		OreDictionary.registerOre("oreCopper", new ItemStack(MekanismBlocks.OreBlock, 1, 1));
		OreDictionary.registerOre("oreTin", new ItemStack(MekanismBlocks.OreBlock, 1, 2));
		
		if(general.controlCircuitOreDict)
		{
			OreDictionary.registerOre("circuitBasic", new ItemStack(MekanismItems.ControlCircuit, 1, 0));
			OreDictionary.registerOre("circuitAdvanced", new ItemStack(MekanismItems.ControlCircuit, 1, 1));
			OreDictionary.registerOre("circuitElite", new ItemStack(MekanismItems.ControlCircuit, 1, 2));
			OreDictionary.registerOre("circuitUltimate", new ItemStack(MekanismItems.ControlCircuit, 1, 3));
		}
		
		OreDictionary.registerOre("itemCompressedCarbon", new ItemStack(MekanismItems.CompressedCarbon));
		OreDictionary.registerOre("itemEnrichedAlloy", new ItemStack(MekanismItems.EnrichedAlloy));
		OreDictionary.registerOre("itemBioFuel", new ItemStack(MekanismItems.BioFuel));
	}
	
	/**
	 * Integrates the mod with other mods -- registering items and blocks with the Forge Ore Dictionary
	 * and adding machine recipes with other items' corresponding resources.
	 */
	public void addIntegratedItems()
	{		
		if(hooks.MetallurgyCoreLoaded)
		{
			try {
				String[] setNames = {"base", "precious", "nether", "fantasy", "ender", "utility"};
				
				for(String setName : setNames )
				{
					for(IOreInfo oreInfo : MetallurgyAPI.getMetalSet(setName).getOreList().values())
					{
						switch(oreInfo.getType()) 
						{
							case ALLOY: 
							{
								if(oreInfo.getIngot() != null && oreInfo.getDust() != null)
								{
									RecipeHandler.addCrusherRecipe(MekanismUtils.size(oreInfo.getIngot(), 1), MekanismUtils.size(oreInfo.getDust(), 1));
								}
								
								break;
							}
							case DROP: 
							{
								ItemStack ore = oreInfo.getOre();
								ItemStack drop = oreInfo.getDrop();
								
								if(drop != null && ore != null)
								{ 
									RecipeHandler.addEnrichmentChamberRecipe(MekanismUtils.size(ore, 1), MekanismUtils.size(drop, 12));
								}
								
								break;
							}
							default: 
							{
								ItemStack ore = oreInfo.getOre();
								ItemStack dust = oreInfo.getDust();
								ItemStack ingot = oreInfo.getIngot();
								
								if(ore != null && dust != null)
								{
									RecipeHandler.addEnrichmentChamberRecipe(MekanismUtils.size(ore, 1), MekanismUtils.size(dust, 2));
									RecipeHandler.addCombinerRecipe(MekanismUtils.size(dust, 8), MekanismUtils.size(ore, 1));
								}
								
								if(ingot != null && dust != null)
								{
									RecipeHandler.addCrusherRecipe(MekanismUtils.size(ingot, 1), MekanismUtils.size(dust, 1));
								}
								
								break;
							}
						}
					}
				}
			} catch(Exception e) {}
		}
	}
	
	/**
	 * Adds and registers all entities and tile entities.
	 */
	public void addEntities()
	{
		//Registrations
		EntityRegistry.registerModEntity(EntityObsidianTNT.class, "ObsidianTNT", 0, this, 64, 5, true);
		EntityRegistry.registerModEntity(EntityRobit.class, "Robit", 1, this, 64, 2, true);
		EntityRegistry.registerModEntity(EntityBalloon.class, "Balloon", 2, this, 64, 1, true);
		EntityRegistry.registerModEntity(EntityBabySkeleton.class, "BabySkeleton", 3, this, 64, 5, true);
		EntityRegistry.registerModEntity(EntityFlame.class, "Flame", 4, this, 64, 5, true);
		
		//Tile entities
		GameRegistry.registerTileEntity(TileEntityBoundingBlock.class, "BoundingBlock");
		GameRegistry.registerTileEntity(TileEntityAdvancedBoundingBlock.class, "AdvancedBoundingBlock");
		GameRegistry.registerTileEntity(TileEntityCardboardBox.class, "CardboardBox");
		GameRegistry.registerTileEntity(TileEntitySolarEvaporationValve.class, "SalinationValve");
		GameRegistry.registerTileEntity(TileEntitySolarEvaporationBlock.class, "SalinationTank");
		GameRegistry.registerTileEntity(TileEntityEntangledBlock.class, "EntangledBlock");
		GameRegistry.registerTileEntity(TileEntityPressureDisperser.class, "PressureDisperser");
		GameRegistry.registerTileEntity(TileEntitySuperheatingElement.class, "SuperheatingElement");
		GameRegistry.registerTileEntity(TileEntityBoilerCasing.class, "BoilerCasing");
		GameRegistry.registerTileEntity(TileEntityBoilerValve.class, "BoilerValve");
		GameRegistry.registerTileEntity(TileEntityMetallurgicInfuser.class, "MetallurgicInfuser");
		GameRegistry.registerTileEntity(TileEntityLaser.class, "Laser");

		//Load tile entities that have special renderers.
		proxy.registerSpecialTileEntities();
	}
	
	@EventHandler
	public void serverStarting(FMLServerStartingEvent event)
	{
		if(general.voiceServerEnabled)
		{
			voiceManager.start();
		}
		
		//Load cached furnace recipes
		Recipe.ENERGIZED_SMELTER.get().clear();
		
		for(Object obj : FurnaceRecipes.instance().getSmeltingList().entrySet())
		{
			Map.Entry<ItemStack, ItemStack> entry = (Map.Entry<ItemStack, ItemStack>)obj;
			SmeltingRecipe recipe = new SmeltingRecipe(new ItemStackInput(entry.getKey()), new ItemStackOutput(entry.getValue()));
			Recipe.ENERGIZED_SMELTER.put(recipe);
		}
		
		event.registerServerCommand(new CommandMekanism());
	}
	
	@EventHandler
	public void serverStopping(FMLServerStoppingEvent event)
	{
		if(general.voiceServerEnabled)
		{
			voiceManager.stop();
		}
		
		//Clear all cache data
		jetpackOn.clear();
		gasmaskOn.clear();
		activeVibrators.clear();
		worldTickHandler.resetRegenChunks();
		privateTeleporters.clear();
		
		//Reset consistent managers
		MultiblockManager.reset();
		FrequencyManager.reset();
		TransporterManager.reset();
		PathfinderCache.reset();
		TransmitterNetworkRegistry.reset();
	}
	
	@EventHandler
	public void loadComplete(FMLLoadCompleteEvent event)
	{
		new IMCHandler().onIMCEvent(FMLInterModComms.fetchRuntimeMessages(this));
	}
	
	@EventHandler
	public void preInit(FMLPreInitializationEvent event)
	{
		File config = event.getSuggestedConfigurationFile();
		
		//Set the mod's configuration
		configuration = new Configuration(config);
		
		if(config.getAbsolutePath().contains("voltz"))
		{
			logger.info("Detected Voltz in root directory - hello, fellow user!");
		}
		else if(config.getAbsolutePath().contains("tekkit"))
		{
			logger.info("Detected Tekkit in root directory - hello, fellow user!");
		}
		
		GasRegistry.register(new Gas("hydrogen")).registerFluid();
		GasRegistry.register(new Gas("oxygen")).registerFluid();
		GasRegistry.register(new Gas("water")).registerFluid();
		GasRegistry.register(new Gas("chlorine")).registerFluid();
		GasRegistry.register(new Gas("sulfurDioxideGas")).registerFluid();
		GasRegistry.register(new Gas("sulfurTrioxideGas")).registerFluid();
		GasRegistry.register(new Gas("sulfuricAcid")).registerFluid();
		GasRegistry.register(new Gas("hydrogenChloride")).registerFluid();
		GasRegistry.register(new Gas("liquidOsmium").setVisible(false));
		GasRegistry.register(new Gas("liquidStone").setVisible(false));
		GasRegistry.register(new Gas("ethene").registerFluid());
		GasRegistry.register(new Gas("sodium").registerFluid());
		GasRegistry.register(new Gas("brine").registerFluid());
		GasRegistry.register(new Gas("deuterium")).registerFluid();
		GasRegistry.register(new Gas("tritium")).registerFluid();
		GasRegistry.register(new Gas("fusionFuelDT")).registerFluid();
		GasRegistry.register(new Gas("steam")).registerFluid();
		GasRegistry.register(new Gas("lithium")).registerFluid();
		
		FluidRegistry.registerFluid(new Fluid("heavyWater", new ResourceLocation("mekanism:heavy_water"), new ResourceLocation("mekanism:heavy_water")));
		
		for(Resource resource : Resource.values())
		{
			String name = resource.getName();
			
			OreGas clean = (OreGas)GasRegistry.register(new OreGas("clean" + name, "oregas." + name.toLowerCase()).setVisible(false));
			GasRegistry.register(new OreGas(name.toLowerCase(), "oregas." + name.toLowerCase()).setCleanGas(clean).setVisible(false));
		}
		
		Mekanism.proxy.preInit();

		//Register blocks and items
		MekanismItems.register();
		MekanismBlocks.register();

		//Register infuses
        InfuseRegistry.registerInfuseType(new InfuseType("CARBON", "mekanism:infuse/Carbon").setUnlocalizedName("carbon"));
        InfuseRegistry.registerInfuseType(new InfuseType("TIN", "mekanism:infuse/Tin").setUnlocalizedName("tin"));
        InfuseRegistry.registerInfuseType(new InfuseType("DIAMOND", "mekanism:infuse/Diamond").setUnlocalizedName("diamond"));
        InfuseRegistry.registerInfuseType(new InfuseType("REDSTONE", "mekanism:infuse/Redstone").setUnlocalizedName("redstone"));
        InfuseRegistry.registerInfuseType(new InfuseType("FUNGI", "mekanism:infuse/Fungi").setUnlocalizedName("fungi"));
		InfuseRegistry.registerInfuseType(new InfuseType("BIO", "mekanism:infuse/Bio").setUnlocalizedName("bio"));
		InfuseRegistry.registerInfuseType(new InfuseType("OBSIDIAN", "mekanism:infuse/Obsidian").setUnlocalizedName("obsidian"));
	}
	
	@EventHandler
	public void init(FMLInitializationEvent event) 
	{
		//Register the mod's world generators
		GameRegistry.registerWorldGenerator(genHandler, 1);
		
		//Register the mod's GUI handler
		NetworkRegistry.INSTANCE.registerGuiHandler(this, new CoreGuiHandler());
		
		//Register player tracker
		FMLCommonHandler.instance().bus().register(new CommonPlayerTracker());
		FMLCommonHandler.instance().bus().register(new CommonPlayerTickHandler());
		
		//Initialization notification
		logger.info("Version " + versionNumber + " initializing...");
		
		//Get data from server.
		new ThreadGetData();
		
		//Register with ForgeChunkManager
		ForgeChunkManager.setForcedChunkLoadingCallback(this, new ChunkManager());
		
		//Register to receive subscribed events
		FMLCommonHandler.instance().bus().register(this);
		MinecraftForge.EVENT_BUS.register(this);
		
		//Register this module's GUI handler in the simple packet protocol
		PacketSimpleGui.handlers.add(0, proxy);

		//Set up VoiceServerManager
		if(general.voiceServerEnabled)
		{
			voiceManager = new VoiceServerManager();
		}
		
		//Register with TransmitterNetworkRegistry
		TransmitterNetworkRegistry.initiate();
		
		//Load configuration
		proxy.loadConfiguration();
		proxy.onConfigSync(false);
		
		//Add baby skeleton spawner
		if(general.spawnBabySkeletons)
		{
			for(BiomeGenBase biome : WorldChunkManager.allowedBiomes) 
			{
				if(biome.getSpawnableList(EnumCreatureType.MONSTER) != null && biome.getSpawnableList(EnumCreatureType.MONSTER).size() > 0)
				{
					EntityRegistry.addSpawn(EntityBabySkeleton.class, 40, 1, 3, EnumCreatureType.MONSTER, biome);
				}
			}
		}

		//Load this module
		addRecipes();
		addEntities();
		
		//Integrate certain OreDictionary recipes
		registerOreDict();

		//Set up multiparts
//TODO reinstate Multipart support with MCMultipart		new MultipartMekanism();
		
		//Integrate with Waila
		FMLInterModComms.sendMessage("Waila", "register", "mekanism.common.integration.WailaDataProvider.register");

		//Integrate with OpenComputers
		if(Loader.isModLoaded("OpenComputers"))
		{
			hooks.loadOCDrivers();
		}

		if(Loader.isModLoaded("appliedenergistics2"))
		{
			hooks.registerAE2P2P();
		}

		//Packet registrations
		packetHandler.initialize();

		//Load proxy
		proxy.registerRenderInformation();
		proxy.loadUtilities();
		
		//Completion notification
		logger.info("Loading complete.");
		
		//Success message
		logger.info("Mod loaded.");
	}	
	
	@EventHandler
	public void postInit(FMLPostInitializationEvent event)
	{
		logger.info("Fake player readout: UUID = " + gameProfile.getId().toString() + ", name = " + gameProfile.getName());

		hooks.hook();
		
		MinecraftForge.EVENT_BUS.post(new BoxBlacklistEvent());
		
		addIntegratedItems();
		
		OreDictManager.init();
		
		//Update the config-dependent recipes after the recipes have actually been added in the first place
		Mekanism.proxy.updateConfigRecipes();
		
		logger.info("Hooking complete.");
	}
	
	@SubscribeEvent
	public void onEnergyTransferred(EnergyTransferEvent event)
	{
		try {
			packetHandler.sendToReceivers(new TransmitterUpdateMessage(PacketType.ENERGY, event.energyNetwork.transmitters.iterator().next().coord(), event.power), event.energyNetwork.getPacketRange());
		} catch(Exception e) {}
	}
	
	@SubscribeEvent
	public void onGasTransferred(GasTransferEvent event)
	{
		try {
			packetHandler.sendToReceivers(new TransmitterUpdateMessage(PacketType.GAS, event.gasNetwork.transmitters.iterator().next().coord(), event.transferType, event.didTransfer), event.gasNetwork.getPacketRange());
		} catch(Exception e) {}
	}
	
	@SubscribeEvent
	public void onLiquidTransferred(FluidTransferEvent event)
	{
		try {
			packetHandler.sendToReceivers(new TransmitterUpdateMessage(PacketType.FLUID, event.fluidNetwork.transmitters.iterator().next().coord(), event.fluidType, event.didTransfer), event.fluidNetwork.getPacketRange());
		} catch(Exception e) {}
	}

	@SubscribeEvent
	public void onTransmittersAddedEvent(TransmittersAddedEvent event)
	{
		try {
			packetHandler.sendToReceivers(new TransmitterUpdateMessage(PacketType.UPDATE, event.network.transmitters.iterator().next().coord(), event.newNetwork, event.newTransmitters), event.network.getPacketRange());
		} catch(Exception e) {}
	}
	
	@SubscribeEvent
	public void onNetworkClientRequest(NetworkClientRequest event)
	{
		try {
			packetHandler.sendToServer(new DataRequestMessage(Coord4D.get(event.tileEntity)));
		} catch(Exception e) {}
	}
	
	@SubscribeEvent
	public void onClientTickUpdate(ClientTickUpdate event)
	{
		try {
			if(event.operation == 0)
			{
				ClientTickHandler.tickingSet.remove(event.network);
			}
			else {
				ClientTickHandler.tickingSet.add(event.network);
			}
		} catch(Exception e) {}
	}
	
	@SubscribeEvent
	public void onBlacklistUpdate(BoxBlacklistEvent event)
	{
		MekanismAPI.addBoxBlacklist(MekanismBlocks.CardboardBox, OreDictionary.WILDCARD_VALUE);
		MekanismAPI.addBoxBlacklist(MekanismBlocks.BoundingBlock, OreDictionary.WILDCARD_VALUE);
		MekanismAPI.addBoxBlacklist(Blocks.bedrock, 0);
		MekanismAPI.addBoxBlacklist(Blocks.portal, OreDictionary.WILDCARD_VALUE);
		MekanismAPI.addBoxBlacklist(Blocks.end_portal, OreDictionary.WILDCARD_VALUE);
		MekanismAPI.addBoxBlacklist(Blocks.end_portal_frame, OreDictionary.WILDCARD_VALUE);
		MekanismAPI.addBoxBlacklist(Blocks.bed, OreDictionary.WILDCARD_VALUE);
		MekanismAPI.addBoxBlacklist(Blocks.oak_door, OreDictionary.WILDCARD_VALUE);
		MekanismAPI.addBoxBlacklist(Blocks.spruce_door, OreDictionary.WILDCARD_VALUE);
		MekanismAPI.addBoxBlacklist(Blocks.birch_door, OreDictionary.WILDCARD_VALUE);
		MekanismAPI.addBoxBlacklist(Blocks.jungle_door, OreDictionary.WILDCARD_VALUE);
		MekanismAPI.addBoxBlacklist(Blocks.acacia_door, OreDictionary.WILDCARD_VALUE);
		MekanismAPI.addBoxBlacklist(Blocks.dark_oak_door, OreDictionary.WILDCARD_VALUE);
		MekanismAPI.addBoxBlacklist(Blocks.iron_door, OreDictionary.WILDCARD_VALUE);
//		MekanismAPI.addBoxBlacklist(MultipartProxy.block(), OreDictionary.WILDCARD_VALUE);
		
		BoxBlacklistParser.load();
	}
	
	@SubscribeEvent
	public synchronized void onChunkLoad(ChunkEvent.Load event)
	{
		if(event.getChunk() != null && !event.world.isRemote)
		{
			Map copy = (Map)((HashMap)event.getChunk().getTileEntityMap()).clone();
			 
			for(Iterator iter = copy.values().iterator(); iter.hasNext();)
			{
				Object obj = iter.next();
	        	 
				if(obj instanceof TileEntity)
				{
					TileEntity tileEntity = (TileEntity)obj;
	
					if(tileEntity instanceof TileEntityElectricBlock && MekanismUtils.useIC2())
					{
						((TileEntityElectricBlock)tileEntity).register();
					}
					else if(tileEntity instanceof IChunkLoadHandler)
					{
						((IChunkLoadHandler)tileEntity).onChunkLoad();
					}
				}
			}
		}
	}
	
	@SubscribeEvent
	public void chunkSave(ChunkDataEvent.Save event) 
	{
		if(!event.world.isRemote)
		{
			NBTTagCompound nbtTags = event.getData();

			nbtTags.setInteger("MekanismWorldGen", baseWorldGenVersion);
			nbtTags.setInteger("MekanismUserWorldGen", general.userWorldGenVersion);
		}
	}
	
	@SubscribeEvent
	public synchronized void onChunkDataLoad(ChunkDataEvent.Load event)
	{
		if(!event.world.isRemote)
		{
			if(general.enableWorldRegeneration)
			{
				NBTTagCompound loadData = event.getData();
				
				if(loadData.getInteger("MekanismWorldGen") == baseWorldGenVersion && loadData.getInteger("MekanismUserWorldGen") == general.userWorldGenVersion)
				{
					return;
				}
	
				ChunkCoordIntPair coordPair = event.getChunk().getChunkCoordIntPair();
				worldTickHandler.addRegenChunk(event.world.provider.getDimensionId(), coordPair);
			}
		}
	}

	@SubscribeEvent
	public void onConfigChanged(ConfigChangedEvent.OnConfigChangedEvent event)
	{
		if(event.modID.equals("Mekanism"))
		{
			proxy.loadConfiguration();
			proxy.onConfigSync(false);
		}
	}

	@SubscribeEvent
	@SideOnly(Side.CLIENT)
	public void registerModels(ModelBakeEvent event) {
		ModelResourceLocation mrl = new ModelResourceLocation("mekanism:energy_cube", "inventory");
		IBakedModel b = event.modelRegistry.getObject(new ModelResourceLocation("mekanism:EnergyCube", "facing=north,tier=basic"));
		IBakedModel a = event.modelRegistry.getObject(new ModelResourceLocation("mekanism:EnergyCube", "facing=north,tier=advanced"));
		IBakedModel e = event.modelRegistry.getObject(new ModelResourceLocation("mekanism:EnergyCube", "facing=north,tier=elite"));
		IBakedModel u = event.modelRegistry.getObject(new ModelResourceLocation("mekanism:EnergyCube", "facing=north,tier=ultimate"));
		IBakedModel c = event.modelRegistry.getObject(new ModelResourceLocation("mekanism:EnergyCube", "facing=north,tier=creative"));
		event.modelRegistry.putObject(mrl, new EnergyCubeItemModel(new IBakedModel[]{b, a, e, u, c}));
	}
}
