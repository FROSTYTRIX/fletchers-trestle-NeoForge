package net.frostytrix.fletcherstrestle.client.model;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import net.neoforged.neoforge.client.model.geometry.IGeometryLoader;

public class ModularModelLoader implements IGeometryLoader<ModularUnbakedGeometry> {
    public static final ModularModelLoader INSTANCE = new ModularModelLoader();

    @Override
    public ModularUnbakedGeometry read(JsonObject jsonObject, JsonDeserializationContext deserializationContext) {
        // Read the "base_path" from the JSON (e.g., "item/modular_bow")
        String basePath = jsonObject.has("base_path") ? jsonObject.get("base_path").getAsString() : "";
        return new ModularUnbakedGeometry(basePath);
    }
}