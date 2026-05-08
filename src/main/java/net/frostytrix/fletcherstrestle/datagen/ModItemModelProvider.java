package net.frostytrix.fletcherstrestle.datagen;

import net.frostytrix.fletcherstrestle.FletcherTrestle;
import net.frostytrix.fletcherstrestle.item.ModItems;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.neoforged.neoforge.client.model.generators.ItemModelBuilder;
import net.neoforged.neoforge.client.model.generators.ItemModelProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

import java.util.LinkedHashMap;
import java.util.Map;

public class ModItemModelProvider extends ItemModelProvider {
    public ModItemModelProvider(PackOutput output, ExistingFileHelper existingFileHelper) {
        super(output, FletcherTrestle.MOD_ID, existingFileHelper);
    }

    @Override
    protected void registerModels() {
        basicItem(ModItems.PLIABLE_OAK_LIMB.get());
        basicItem(ModItems.ROUGH_OAK_LIMB.get());

        basicItem(ModItems.PLIABLE_SPRUCE_LIMB.get());
        basicItem(ModItems.ROUGH_SPRUCE_LIMB.get());

        basicItem(ModItems.PLIABLE_BIRCH_LIMB.get());
        basicItem(ModItems.ROUGH_BIRCH_LIMB.get());

        basicItem(ModItems.PLIABLE_JUNGLE_LIMB.get());
        basicItem(ModItems.ROUGH_JUNGLE_LIMB.get());

        basicItem(ModItems.PLIABLE_ACACIA_LIMB.get());
        basicItem(ModItems.ROUGH_ACACIA_LIMB.get());

        basicItem(ModItems.PLIABLE_DARK_OAK_LIMB.get());
        basicItem(ModItems.ROUGH_DARK_OAK_LIMB.get());

        basicItem(ModItems.PLIABLE_MANGROVE_LIMB.get());
        basicItem(ModItems.ROUGH_MANGROVE_LIMB.get());

        basicItem(ModItems.PLIABLE_CHERRY_LIMB.get());
        basicItem(ModItems.ROUGH_CHERRY_LIMB.get());

        basicItem(ModItems.PLIABLE_PALE_OAK_LIMB.get());
        basicItem(ModItems.ROUGH_PALE_OAK_LIMB.get());

        basicItem(ModItems.PLIABLE_CRIMSON_LIMB.get());
        basicItem(ModItems.ROUGH_CRIMSON_LIMB.get());

        basicItem(ModItems.PLIABLE_WARPED_LIMB.get());
        basicItem(ModItems.ROUGH_WARPED_LIMB.get());

        basicItem(ModItems.WOOD_RISER.get());
        basicItem(ModItems.IRON_RISER.get());
        basicItem(ModItems.COPPER_RISER.get());

        basicItem(ModItems.HIGH_TENSION_STRING.get());
        basicItem(ModItems.FLAX.get());
        basicItem(ModItems.FLAX_SEEDS.get());
        basicItem(ModItems.FLAX_STRING.get());
        handheldItem(ModItems.DRAWKNIFE.get());

        basicItem(ModItems.QUIVER.get());
        basicItem(ModItems.MECHANICAL_TRIGGER.get());

        // TODO: Bow Data Gen

        Map<String, Float> limbs = new LinkedHashMap<>();
        limbs.put("oak", 0.1f);
        limbs.put("spruce", 0.2f);
        limbs.put("birch", 0.3f);
        limbs.put("jungle", 0.4f);
        limbs.put("acacia", 0.5f);
        limbs.put("dark_oak", 0.6f);
        limbs.put("mangrove", 0.7f);
        limbs.put("cherry", 0.8f);
        limbs.put("pale_oak", 0.9f);
        limbs.put("crimson", 1.0f);
        limbs.put("warped", 1.1f);

        Map<String, Float> risers = new LinkedHashMap<>();
        risers.put("wood", 0.1f);
        risers.put("iron", 0.2f);
        risers.put("copper", 0.3f);

        Map<String, Float> strings = new LinkedHashMap<>();
        strings.put("spider", 0.1f);
        strings.put("flax", 0.2f);
        strings.put("high_tension", 0.3f);

        ItemModelBuilder baseBow = getBuilder("modular_bow")
                .parent(getExistingFile(mcLoc("item/bow")))
                .texture("layer0", "item/risers/wood_riser")
                .texture("layer1", "item/limbs/oak_limb")
                .texture("layer2", "item/strings/spider_string");

        for (var limb : limbs.entrySet()) {
            for (var riser : risers.entrySet()) {
                for (var string : strings.entrySet()) {
                    buildBowPermutation(baseBow, limb.getKey(), limb.getValue(),
                            riser.getKey(), riser.getValue(),
                            string.getKey(), string.getValue());
                }
            }
        }

        // TODO: Arrow Data Gen

        Map<String, Float> heads = new LinkedHashMap<>();
        heads.put("flint", 0.1f);
        heads.put("broadhead", 0.2f);
        heads.put("bodkin_point", 0.3f);
        heads.put("resonance_tip", 0.4f);
        heads.put("barbed_tip", 0.5f);
        heads.put("weighted_blunt", 0.6f);

        // Can reuse "limbs" map for the shafts since they use the same wood types
        Map<String, Float> shafts = limbs;

        Map<String, Float> fletchings = new LinkedHashMap<>();
        fletchings.put("feather", 0.1f);
        fletchings.put("rigid", 0.2f);
        fletchings.put("trailing", 0.3f);
        fletchings.put("serrated", 0.4f);
        fletchings.put("bound", 0.5f);
        fletchings.put("vex", 0.6f);

        // The base arrow item (Fallback if it has no data)
        ItemModelBuilder baseArrow = getBuilder("modular_arrow")
                .parent(getExistingFile(mcLoc("item/generated")))
                .texture("layer0", "item/arrow/shafts/oak_shaft")
                .texture("layer1", "item/arrow/fletchings/feather_fletching")
                .texture("layer2", "item/arrow/heads/flint_head");

        for (var head : heads.entrySet()) {
            for (var shaft : shafts.entrySet()) {
                for (var fletching : fletchings.entrySet()) {
                    buildArrowPermutation(baseArrow,
                            head.getKey(), head.getValue(),
                            shaft.getKey(), shaft.getValue(),
                            fletching.getKey(), fletching.getValue());
                }
            }
        }

        generateCrossbowModels();


    }

    private void buildBowPermutation(ItemModelBuilder baseBow, String limbName, float limbVal,
                                     String riserName, float riserVal,
                                     String stringName, float stringVal) {

        for (int pullStage = 0; pullStage < 4; pullStage++) {

            String modelName = "modular_bow_" + limbName + "_" + riserName + "_" + stringName;
            if (pullStage > 0) modelName += "_pulling_" + (pullStage - 1);

            String limbTex = "item/limbs/" + limbName + "_limb";
            String riserTex = "item/risers/" + riserName + "_riser";
            String stringTex = "item/strings/" + stringName + "_string";
            String arrowTex = "item/extras/no_arrow";

            if (pullStage > 0) {
                limbTex += "_pulling_" + (pullStage - 1);
                stringTex += "_pulling_" + (pullStage - 1);
                arrowTex = "item/extras/arrow_pulling_" + (pullStage - 1);
            }

            // Generate the JSON file for this exact frame
            ItemModelBuilder permutation = getBuilder(modelName)
                    .parent(getExistingFile(mcLoc("item/bow")))
                    .texture("layer0", limbTex)
                    .texture("layer1", riserTex)
                    .texture("layer2", stringTex)
                    .texture("layer3", arrowTex);

            float pullValue = pullStage == 2 ? 0.65f : (pullStage == 3 ? 0.90f : 0.0f);

            // Attach it to the base bow's overrides
            var override = baseBow.override()
                    .predicate(ResourceLocation.fromNamespaceAndPath(FletcherTrestle.MOD_ID, "limb"), limbVal)
                    .predicate(ResourceLocation.fromNamespaceAndPath(FletcherTrestle.MOD_ID, "riser"), riserVal)
                    .predicate(ResourceLocation.fromNamespaceAndPath(FletcherTrestle.MOD_ID, "string"), stringVal);

            if (pullStage > 0) {
                override.predicate(ResourceLocation.withDefaultNamespace("pulling"), 1.0f);
                if (pullStage > 1) {
                    override.predicate(ResourceLocation.withDefaultNamespace("pull"), pullValue);
                }
            }

            override.model(permutation).end();
        }
    }

    private void buildArrowPermutation(ItemModelBuilder baseArrow,
                                      String headName, float headVal,
                                      String shaftName, float shaftVal,
                                      String fletchingName, float fletchingVal) {

        String modelName = "modular_arrow_" + headName + "_" + shaftName + "_" + fletchingName;

        // Pointing to your texture folders
        String shaftTex = "item/arrow/shafts/" + shaftName + "_shaft";
        String fletchingTex = "item/arrow/fletchings/" + fletchingName + "_fletching";
        String headTex = "item/arrow/heads/" + headName + "_head";

        // Generate the JSON file for this exact arrow
        ItemModelBuilder permutation = getBuilder(modelName)
                .parent(getExistingFile(mcLoc("item/generated")))
                .texture("layer0", shaftTex)
                .texture("layer1", fletchingTex)
                .texture("layer2", headTex);

        // Attach it to the base arrow's overrides
        baseArrow.override()
                .predicate(ResourceLocation.fromNamespaceAndPath(FletcherTrestle.MOD_ID, "arrow_head"), headVal)
                .predicate(ResourceLocation.fromNamespaceAndPath(FletcherTrestle.MOD_ID, "arrow_shaft"), shaftVal)
                .predicate(ResourceLocation.fromNamespaceAndPath(FletcherTrestle.MOD_ID, "arrow_fletching"), fletchingVal)
                .model(permutation).end();
    }

    public void generateCrossbowModels() {
        ItemModelBuilder crossbowBase = getBuilder("modular_crossbow_base")
                .parent(getExistingFile(mcLoc("item/generated")));
        applyCrossbowTransforms(crossbowBase);

        // 1. Create the base fallback item model
        ItemModelBuilder baseCrossbow = getBuilder("modular_crossbow")
                .parent(getExistingFile(modLoc("item/modular_crossbow_base")))
                .texture("layer0", "item/modular_crossbow/limbs/oak_limb")
                .texture("layer1", "item/modular_crossbow/risers/wood_riser")
                .texture("layer2", "item/modular_crossbow/strings/spider_string");

        String[] limbs = {"oak", "spruce", "birch", "jungle", "acacia", "dark_oak", "mangrove", "cherry", "pale_oak", "crimson", "warped"};
        String[] risers = {"wood", "iron", "copper"};
        String[] strings = {"spider", "flax", "high_tension"};

        // 2. Loop through all combinations and assign unique float values
        for (int l = 0; l < limbs.length; l++) {
            float limbVal = (l + 1) * 0.1f;
            for (int r = 0; r < risers.length; r++) {
                float riserVal = (r + 1) * 0.1f;
                for (int s = 0; s < strings.length; s++) {
                    float stringVal = (s + 1) * 0.1f;

                    buildCrossbowPermutation(baseCrossbow, limbs[l], limbVal, risers[r], riserVal, strings[s], stringVal);
                }
            }
        }
    }

    private void buildCrossbowPermutation(ItemModelBuilder baseCrossbow,
                                          String limbName, float limbVal,
                                          String riserName, float riserVal,
                                          String stringName, float stringVal) {

        String baseName = "modular_crossbow_" + limbName + "_" + riserName + "_" + stringName;

        ResourceLocation limbProp = ResourceLocation.fromNamespaceAndPath(FletcherTrestle.MOD_ID, "crossbow_limb");
        ResourceLocation riserProp = ResourceLocation.fromNamespaceAndPath(FletcherTrestle.MOD_ID, "crossbow_riser");
        ResourceLocation stringProp = ResourceLocation.fromNamespaceAndPath(FletcherTrestle.MOD_ID, "crossbow_string");

        // --- UPDATED TEXTURE PATHS ---
        // Risers stay shared, but Limbs and Strings move to the modular_crossbow subfolder!
        String riserTex = "item/modular_crossbow/risers/" + riserName + "_riser";
        String limbBase = "item/modular_crossbow/limbs/" + limbName + "_limb";
        String stringBase = "item/modular_crossbow/strings/" + stringName + "_string";

        // --- 1. UNCHARGED ---
        ItemModelBuilder uncharged = getBuilder(baseName)
                .parent(getExistingFile(modLoc("item/modular_crossbow_base")))
                .texture("layer0", limbBase)
                .texture("layer1", riserTex)
                .texture("layer2", stringBase);

        baseCrossbow.override()
                .predicate(limbProp, limbVal).predicate(riserProp, riserVal).predicate(stringProp, stringVal)
                .model(uncharged).end();

        // --- 2. PULLING 0 ---
        ItemModelBuilder pulling0 = getBuilder(baseName + "_pulling_0")
                .parent(getExistingFile(modLoc("item/modular_crossbow_base")))
                .texture("layer0", limbBase)
                .texture("layer1", riserTex)
                .texture("layer2", stringBase + "_pulling_0");

        baseCrossbow.override()
                .predicate(limbProp, limbVal).predicate(riserProp, riserVal).predicate(stringProp, stringVal)
                .predicate(ResourceLocation.withDefaultNamespace("pulling"), 1.0f)
                .model(pulling0).end();

        // --- 3. PULLING 1 (Mid Draw) ---
        ItemModelBuilder pulling1 = getBuilder(baseName + "_pulling_1")
                .parent(getExistingFile(modLoc("item/modular_crossbow_base")))
                .texture("layer0", limbBase)
                .texture("layer1", riserTex)
                .texture("layer2", stringBase + "_pulling_1");

        baseCrossbow.override()
                .predicate(limbProp, limbVal).predicate(riserProp, riserVal).predicate(stringProp, stringVal)
                .predicate(ResourceLocation.withDefaultNamespace("pulling"), 1.0f)
                .predicate(ResourceLocation.withDefaultNamespace("pull"), 0.58f)
                .model(pulling1).end();

        // --- 4. PULLING 2 (Full Draw) ---
        ItemModelBuilder pulling2 = getBuilder(baseName + "_pulling_2")
                .parent(getExistingFile(modLoc("item/modular_crossbow_base")))
                .texture("layer0", limbBase)
                .texture("layer1", riserTex)
                .texture("layer2", stringBase + "_pulling_2");

        baseCrossbow.override()
                .predicate(limbProp, limbVal).predicate(riserProp, riserVal).predicate(stringProp, stringVal)
                .predicate(ResourceLocation.withDefaultNamespace("pulling"), 1.0f)
                .predicate(ResourceLocation.withDefaultNamespace("pull"), 1.0f)
                .model(pulling2).end();

        // --- 5. CHARGED (With Arrow) ---
        ItemModelBuilder charged = getBuilder(baseName + "_charged")
                .parent(getExistingFile(modLoc("item/modular_crossbow_base")))
                .texture("layer0", limbBase)
                .texture("layer1", riserTex)
                .texture("layer2", stringBase + "_pulling_2")
                .texture("layer3", "item/modular_crossbow/arrow");

        baseCrossbow.override()
                .predicate(limbProp, limbVal).predicate(riserProp, riserVal).predicate(stringProp, stringVal)
                .predicate(ResourceLocation.withDefaultNamespace("charged"), 1.0f)
                .model(charged).end();

        // --- 6. CHARGED (With Firework) ---
        ItemModelBuilder firework = getBuilder(baseName + "_firework")
                .parent(getExistingFile(modLoc("item/modular_crossbow_base")))
                .texture("layer0", limbBase)
                .texture("layer1", riserTex)
                .texture("layer2", stringBase + "_pulling_2")
                .texture("layer3", "item/modular_crossbow/firework");

        baseCrossbow.override()
                .predicate(limbProp, limbVal).predicate(riserProp, riserVal).predicate(stringProp, stringVal)
                .predicate(ResourceLocation.withDefaultNamespace("charged"), 1.0f)
                .predicate(ResourceLocation.withDefaultNamespace("firework"), 1.0f)
                .model(firework).end();
    }

    private void applyCrossbowTransforms(ItemModelBuilder builder) {
        builder.transforms()
                .transform(ItemDisplayContext.THIRD_PERSON_RIGHT_HAND)
                .rotation(-90, 0, -60)
                .translation(2, 0.1f, -3)
                .scale(0.9f, 0.9f, 0.9f)
                .end()
                .transform(ItemDisplayContext.THIRD_PERSON_LEFT_HAND)
                .rotation(-90, 0, 30)
                .translation(2, 0.1f, -3)
                .scale(0.9f, 0.9f, 0.9f)
                .end()
                .transform(ItemDisplayContext.FIRST_PERSON_RIGHT_HAND)
                .rotation(-90, 0, -55)
                .translation(1.13f, 3.2f, 1.13f)
                .scale(0.68f, 0.68f, 0.68f)
                .end()
                .transform(ItemDisplayContext.FIRST_PERSON_LEFT_HAND)
                .rotation(-90, 0, 35)
                .translation(1.13f, 3.2f, 1.13f)
                .scale(0.68f, 0.68f, 0.68f)
                .end()
                .end();
    }
}
