package mekanism.common.integration.crafttweaker.chemical;

import java.util.function.Function;
import mekanism.api.chemical.Chemical;
import mekanism.api.chemical.ChemicalStack;
import mekanism.api.chemical.gas.Gas;
import mekanism.api.chemical.gas.GasStack;
import mekanism.api.chemical.infuse.InfuseType;
import mekanism.api.chemical.infuse.InfusionStack;
import mekanism.api.chemical.pigment.Pigment;
import mekanism.api.chemical.pigment.PigmentStack;
import mekanism.api.chemical.slurry.Slurry;
import mekanism.api.chemical.slurry.SlurryStack;
import mekanism.common.integration.crafttweaker.chemical.CrTMutableChemicalStack.CrTMutableGasStack;
import mekanism.common.integration.crafttweaker.chemical.CrTMutableChemicalStack.CrTMutableInfusionStack;
import mekanism.common.integration.crafttweaker.chemical.CrTMutableChemicalStack.CrTMutablePigmentStack;
import mekanism.common.integration.crafttweaker.chemical.CrTMutableChemicalStack.CrTMutableSlurryStack;
import mekanism.common.integration.crafttweaker.chemical.ICrTChemical.ICrTGas;
import mekanism.common.integration.crafttweaker.chemical.ICrTChemical.ICrTInfuseType;
import mekanism.common.integration.crafttweaker.chemical.ICrTChemical.ICrTPigment;
import mekanism.common.integration.crafttweaker.chemical.ICrTChemical.ICrTSlurry;
import mekanism.common.util.ChemicalUtil;

public abstract class CrTChemicalStack<CHEMICAL extends Chemical<CHEMICAL>, STACK extends ChemicalStack<CHEMICAL>,
      CRT_CHEMICAL extends ICrTChemical<CHEMICAL, STACK, CRT_CHEMICAL, CRT_STACK>, CRT_STACK extends ICrTChemicalStack<CHEMICAL, STACK, CRT_CHEMICAL, CRT_STACK>>
      extends BaseCrTChemicalStack<CHEMICAL, STACK, CRT_CHEMICAL, CRT_STACK> {

    private final Function<STACK, CRT_STACK> mutableStackConverter;

    public CrTChemicalStack(STACK stack, Function<STACK, CRT_STACK> stackConverter, Function<STACK, CRT_STACK> mutableStackConverter) {
        super(stack, stackConverter);
        this.mutableStackConverter = mutableStackConverter;
    }

    @Override
    public CRT_STACK setAmount(long amount) {
        return stackConverter.apply(ChemicalUtil.copyWithAmount(stack, amount));
    }

    @Override
    public CRT_STACK asMutable() {
        return mutableStackConverter.apply(stack);
    }

    @Override
    public CRT_STACK asImmutable() {
        return (CRT_STACK) this;
    }

    @Override
    public STACK getImmutableInternal() {
        return getInternal();
    }

    public static class CrTGasStack extends CrTChemicalStack<Gas, GasStack, ICrTGas, ICrTGasStack> implements ICrTGasStack {

        public CrTGasStack(GasStack stack) {
            super(stack, CrTGasStack::new, CrTMutableGasStack::new);
        }
    }

    public static class CrTInfusionStack extends CrTChemicalStack<InfuseType, InfusionStack, ICrTInfuseType, ICrTInfusionStack> implements ICrTInfusionStack {

        public CrTInfusionStack(InfusionStack stack) {
            super(stack, CrTInfusionStack::new, CrTMutableInfusionStack::new);
        }
    }

    public static class CrTPigmentStack extends CrTChemicalStack<Pigment, PigmentStack, ICrTPigment, ICrTPigmentStack> implements ICrTPigmentStack {

        public CrTPigmentStack(PigmentStack stack) {
            super(stack, CrTPigmentStack::new, CrTMutablePigmentStack::new);
        }
    }

    public static class CrTSlurryStack extends CrTChemicalStack<Slurry, SlurryStack, ICrTSlurry, ICrTSlurryStack> implements ICrTSlurryStack {

        public CrTSlurryStack(SlurryStack stack) {
            super(stack, CrTSlurryStack::new, CrTMutableSlurryStack::new);
        }
    }
}