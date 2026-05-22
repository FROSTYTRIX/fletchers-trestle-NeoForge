package net.frostytrix.fletcherstrestle.capability;

import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

// Minimal one-way bridge from the (now-deprecated) FluidTank to the new
// ResourceHandler<FluidResource> shape that Capabilities.Fluid.BLOCK
// expects. The dipping vat still holds its state in a FluidTank — this
// adapter just lets pipes/buckets/etc. talk to it through the new API
// without rewriting every fill/drain call in the BE.
//
// TODO(port-26.1): once the vat is fully ported to FluidStacksResourceHandler
// this adapter goes away.
public final class FluidTankResourceAdapter implements ResourceHandler<FluidResource> {

    private final FluidTank tank;

    public FluidTankResourceAdapter(FluidTank tank) {
        this.tank = tank;
    }

    @Override
    public int size() {
        return 1;
    }

    @Override
    public FluidResource getResource(int slot) {
        FluidStack stack = tank.getFluid();
        return stack.isEmpty() ? FluidResource.EMPTY : FluidResource.of(stack);
    }

    @Override
    public long getAmountAsLong(int slot) {
        return tank.getFluidAmount();
    }

    @Override
    public long getCapacityAsLong(int slot, FluidResource resource) {
        return tank.getCapacity();
    }

    @Override
    public boolean isValid(int slot, FluidResource resource) {
        if (resource.isEmpty()) return true;
        return tank.isFluidValid(resource.toStack(1));
    }

    @Override
    public int insert(int slot, FluidResource resource, int amount, TransactionContext tx) {
        if (resource.isEmpty() || amount <= 0) return 0;
        // FluidTank.fill ignores the FluidStack count beyond `amount` — it
        // computes its own fit. Mirror that here.
        FluidStack toInsert = resource.toStack(amount);
        return tank.fill(toInsert, IFluidHandler.FluidAction.EXECUTE);
    }

    @Override
    public int extract(int slot, FluidResource resource, int amount, TransactionContext tx) {
        if (resource.isEmpty() || amount <= 0) return 0;
        FluidStack inTank = tank.getFluid();
        if (inTank.isEmpty()) return 0;
        // Only extract if the requested resource matches what the tank holds.
        if (!FluidResource.of(inTank).equals(resource)) return 0;
        FluidStack drained = tank.drain(amount, IFluidHandler.FluidAction.EXECUTE);
        return drained.getAmount();
    }
}
