package net.frostytrix.fletcherstrestle.client.model;

// TODO(port-26.1): The entire BakedModel system was rewritten in 26.1.
// IUnbakedGeometry / IGeometryBakingContext / ItemOverrides / BakedQuad /
// Material / ModelState are all gone — replaced by the new ItemModel
// interface and ClientItem.Properties data component pattern.
//
// This class used to drive the modular bow/crossbow's per-limb texture
// overlays. To restore: implement the new ItemModel.Unbaked pattern
// (see net.minecraft.client.renderer.item.ItemModel in 26.1 sources).
public final class ModularBakedModel {
    private ModularBakedModel() {}
}
