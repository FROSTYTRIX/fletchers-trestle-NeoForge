package net.frostytrix.fletcherstrestle.event;

// TODO(port-26.1): IClientFluidTypeExtensions/ClientExtensionsEvent rewrite.
// 1.21.1: registered a fluid tint that read the potion id off CustomData and
// returned the potion's color. 26.1: Registry.getHolder(Identifier) removed,
// CompoundTag.getString returns Optional, fluid extension overrides changed.
public final class ModClientFluidEvents {
    private ModClientFluidEvents() {}
}
