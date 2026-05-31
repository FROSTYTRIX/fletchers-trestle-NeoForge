package net.frostytrix.fletcherstrestle.compat.top;

import mcjty.theoneprobe.api.ITheOneProbe;

import java.util.function.Function;

/** Supplied to TheOneProbe via IMC; registers our probe provider. */
public class GetTheOneProbe implements Function<ITheOneProbe, Void> {

    @Override
    public Void apply(ITheOneProbe probe) {
        probe.registerProvider(new SteamBoxProbeProvider());
        return null;
    }
}
