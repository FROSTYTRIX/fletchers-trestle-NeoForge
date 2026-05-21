package net.frostytrix.fletcherstrestle.compat.jei;

// TODO(port-26.1): JEI 29 has its own API rewrite — IRecipeCategory now
// requires getWidth()/getHeight() instead of getBackground(),
// addTooltipCallback was removed, Ingredient.getItems() is gone, etc.
// The entire compat layer is stubbed until JEI gets a stable 26.1
// migration guide. The mod runs without JEI just fine; recipes work
// at the gameplay level, they just don't show up in JEI search.
public final class FletchersTrestleJeiPlugin {
    private FletchersTrestleJeiPlugin() {}
}
