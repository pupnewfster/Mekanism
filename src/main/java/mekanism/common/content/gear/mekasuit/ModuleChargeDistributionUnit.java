package mekanism.common.content.gear.mekasuit;

import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import java.util.Set;
import javax.annotation.ParametersAreNonnullByDefault;
import mekanism.api.Action;
import mekanism.api.energy.IEnergyContainer;
import mekanism.api.energy.IStrictEnergyHandler;
import mekanism.api.gear.ICustomModule;
import mekanism.api.gear.IModule;
import mekanism.api.gear.config.IModuleConfigItem;
import mekanism.api.gear.config.ModuleBooleanData;
import mekanism.api.gear.config.ModuleConfigItemCreator;
import mekanism.api.math.FloatingLong;
import mekanism.common.MekanismLang;
import mekanism.common.config.MekanismConfig;
import mekanism.common.content.network.distribution.EnergySaveTarget;
import mekanism.common.integration.energy.EnergyCompatUtils;
import mekanism.common.util.EmitUtils;
import mekanism.common.util.StorageUtils;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;

@ParametersAreNonnullByDefault
public class ModuleChargeDistributionUnit implements ICustomModule<ModuleChargeDistributionUnit> {

    private IModuleConfigItem<Boolean> chargeSuit;
    private IModuleConfigItem<Boolean> chargeInventory;

    @Override
    public void init(IModule<ModuleChargeDistributionUnit> module, ModuleConfigItemCreator configItemCreator) {
        chargeSuit = configItemCreator.createConfigItem("charge_suit", MekanismLang.MODULE_CHARGE_SUIT, new ModuleBooleanData());
        chargeInventory = configItemCreator.createConfigItem("charge_inventory", MekanismLang.MODULE_CHARGE_INVENTORY, new ModuleBooleanData(false));
    }

    @Override
    public void tickServer(IModule<ModuleChargeDistributionUnit> module, PlayerEntity player) {
        // charge inventory first
        if (chargeInventory.get()) {
            chargeInventory(module, player);
        }
        // distribute suit charge next
        if (chargeSuit.get()) {
            chargeSuit(player);
        }
    }

    private void chargeSuit(PlayerEntity player) {
        FloatingLong total = FloatingLong.ZERO;
        EnergySaveTarget saveTarget = new EnergySaveTarget(4);
        for (ItemStack stack : player.inventory.armor) {
            IEnergyContainer energyContainer = StorageUtils.getEnergyContainer(stack, 0);
            if (energyContainer != null) {
                saveTarget.addDelegate(energyContainer);
                total = total.plusEqual(energyContainer.getEnergy());
            }
        }
        EmitUtils.sendToAcceptors(saveTarget, total);
        saveTarget.save();
    }

    private void chargeInventory(IModule<ModuleChargeDistributionUnit> module, PlayerEntity player) {
        FloatingLong toCharge = MekanismConfig.gear.mekaSuitInventoryChargeRate.get();
        // first try to charge mainhand/offhand item
        toCharge = charge(module, player, player.getMainHandItem(), toCharge);
        toCharge = charge(module, player, player.getOffhandItem(), toCharge);

        for (ItemStack stack : player.inventory.items) {
            if (stack == player.getMainHandItem() || stack == player.getOffhandItem()) {
                continue;
            }
            if (toCharge.isZero()) {
                break;
            }
            toCharge = charge(module, player, stack, toCharge);
        }
    }

    /** return rejects */
    private FloatingLong charge(IModule<ModuleChargeDistributionUnit> module, PlayerEntity player, ItemStack stack, FloatingLong amount) {
        IStrictEnergyHandler handler = EnergyCompatUtils.getStrictEnergyHandler(stack);
        if (handler != null) {
            FloatingLong remaining = handler.insertEnergy(amount, Action.SIMULATE);
            if (remaining.smallerThan(amount)) {
                //If we can actually insert any energy into
                return handler.insertEnergy(module.useEnergy(player, amount.subtract(remaining), false), Action.EXECUTE).add(remaining);
            }
        }
        return amount;
    }
}