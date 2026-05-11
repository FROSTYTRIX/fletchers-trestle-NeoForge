package net.frostytrix.fletcherstrestle.client.model;

import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.*;
import net.neoforged.neoforge.client.model.geometry.IGeometryBakingContext;
import net.neoforged.neoforge.client.model.geometry.IUnbakedGeometry;
import java.util.function.Function;

public class ModularUnbakedGeometry implements IUnbakedGeometry<ModularUnbakedGeometry> {
    private final String basePath;

    public ModularUnbakedGeometry(String basePath) {
        this.basePath = basePath;
    }

    @Override
    public BakedModel bake(IGeometryBakingContext context, ModelBaker baker, Function<Material, TextureAtlasSprite> spriteGetter, ModelState modelState, ItemOverrides overrides) {
        return new ModularBakedModel(baker, modelState, spriteGetter, context, this.basePath);
    }
}