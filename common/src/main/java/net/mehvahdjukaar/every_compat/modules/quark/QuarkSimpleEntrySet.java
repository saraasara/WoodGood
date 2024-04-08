package net.mehvahdjukaar.every_compat.modules.quark;

import com.google.common.base.Preconditions;
import com.google.common.base.Suppliers;
import com.mojang.datafixers.util.Pair;
import net.mehvahdjukaar.every_compat.EveryCompat;
import net.mehvahdjukaar.every_compat.api.CompatModule;
import net.mehvahdjukaar.every_compat.api.SimpleEntrySet;
import net.mehvahdjukaar.moonlight.api.events.AfterLanguageLoadEvent;
import net.mehvahdjukaar.moonlight.api.misc.Registrator;
import net.mehvahdjukaar.moonlight.api.resources.BlockTypeResTransformer;
import net.mehvahdjukaar.moonlight.api.resources.pack.DynamicDataPack;
import net.mehvahdjukaar.moonlight.api.resources.textures.Palette;
import net.mehvahdjukaar.moonlight.api.set.BlockType;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.resources.metadata.animation.AnimationMetadataSection;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import org.apache.commons.lang3.function.TriFunction;
import org.jetbrains.annotations.Nullable;
import org.violetmoon.quark.base.Quark;
import org.violetmoon.zeta.module.ZetaModule;

import java.util.Collection;
import java.util.List;
import java.util.function.*;

class QuarkSimpleEntrySet<T extends BlockType, B extends Block> extends SimpleEntrySet<T, B> {

    private final Function<T, B> blockSupplier;
    private final Supplier<ZetaModule> zetaModule;

    public QuarkSimpleEntrySet(Class<T> type,
                               String name, @Nullable String prefix,
                               Class<? extends ZetaModule> module,
                               Supplier<B> baseBlock,
                               Supplier<T> baseType,
                               Function<T, B> blockSupplier,
                               Supplier<ResourceKey<CreativeModeTab>> tab,
                               LootTableMode tableMode,
                               @Nullable TriFunction<T, B, Item.Properties, Item> itemFactory,
                               @Nullable SimpleEntrySet.ITileHolder<?> tileFactory,
                               @Nullable Supplier<Supplier<RenderType>> renderType,
                               @Nullable BiFunction<T, ResourceManager, Pair<List<Palette>, @Nullable AnimationMetadataSection>> paletteSupplier,
                               @Nullable Consumer<BlockTypeResTransformer<T>> extraTransform,
                               boolean mergedPalette,
                               Predicate<T> condition) {
        super(type, name, prefix, null, baseBlock, baseType, tab, tableMode, itemFactory, tileFactory, renderType, paletteSupplier, extraTransform, mergedPalette, condition);
        this.blockSupplier = blockSupplier;
        var m = Preconditions.checkNotNull(module);
        this.zetaModule = Suppliers.memoize(() -> Quark.ZETA.modules.get(m));
    }

    @Override
    public void generateLootTables(CompatModule module, DynamicDataPack pack, ResourceManager manager) {
        super.generateLootTables(module, pack, manager);
    }

    @Override
    public void addTranslations(CompatModule module, AfterLanguageLoadEvent lang) {
        super.addTranslations(module, lang);
    }

    @Override
    public void registerBlocks(CompatModule module, Registrator<Block> registry, Collection<T> woodTypes) {
        if (isDisabled()) return;
        Block base = getBaseBlock();
        if (base == null || base == Blocks.AIR)
            //?? wtf im using disabled to allow for null??
            throw new UnsupportedOperationException("Base block cant be null (" + this.typeName + " for " + module.modId + " module)");
        baseType.get().addChild(module.getModId() + ":" + typeName, (Object) base);

        for (T w : woodTypes) {
            String n = getBlockName(w);
            String name = module.shortenedId() + "/" + w.getNamespace() + "/" + n;
            if (w.isVanilla() || module.isEntryAlreadyRegistered(name, w, BuiltInRegistries.BLOCK)) continue;
            B block = blockSupplier.apply(w);
            if (block != null) {
                this.blocks.put(w, block);

                registry.register(EveryCompat.res(name), block); //does not set registry name
                w.addChild(module.getModId() + ":" + typeName, (Object) block);


                if (lootMode == LootTableMode.DROP_SELF && YEET_JSONS) {
                    SIMPLE_DROPS.add(block);
                }
            }
        }
    }

    @Override
    public void generateRecipes(CompatModule module, DynamicDataPack pack, ResourceManager manager) {
        ZetaModule mod = zetaModule.get();
        if (mod == null || mod.enabled) {
            super.generateRecipes(module, pack, manager);
        }
    }

    @Override
    public @Nullable Item getItemOf(T type) {
        ZetaModule mod = zetaModule.get();
        if (mod == null || mod.enabled) {
            return super.getItemOf(type);
        }
        return null;
    }


    //this does not work. all modules seem to be disabled here. why??
    /*
    @Override
    protected CreativeModeTab getTab(T w, B b) {
        boolean e = b instanceof IQuarkBlock qb ? qb.isEnabled() : ModuleLoader.INSTANCE.isModuleEnabled(quarkModule);
        return e ? super.getTab(w, b) : null;
    }*/

    public static <T extends BlockType, B extends Block> Builder<T, B> builder(
            Class<T> type,
            String name,
            Class<? extends ZetaModule> quarkModule,
            Supplier<B> baseBlock, Supplier<T> baseType,
            Function<T, B> factory) {
        return new Builder<>(type, name, null, quarkModule, baseType, baseBlock, factory);
    }

    public static <T extends BlockType, B extends Block> Builder<T, B> builder(
            Class<T> type,
            String name, String prefix,
            Class<? extends ZetaModule> quarkModule,
            Supplier<B> baseBlock, Supplier<T> baseType,
            Function<T, B> factory) {
        return new Builder<>(type, name, prefix, quarkModule, baseType, baseBlock, factory);
    }

    public static class Builder<T extends BlockType, B extends Block> extends SimpleEntrySet.Builder<T, B> {

        private final Function<T, B> blockSupplier;
        private final Class<? extends ZetaModule> quarkModule;

        protected Builder(Class<T> type, String name, @Nullable String prefix,
                          Class<? extends ZetaModule> quarkModule,
                          Supplier<T> baseType, Supplier<B> baseBlock, Function<T, B> factory) {
            super(type, name, prefix, baseType, baseBlock, null);
            this.quarkModule = quarkModule;
            this.blockSupplier = factory;
        }

        @Override
        public QuarkSimpleEntrySet<T, B> build() {
            var e = new QuarkSimpleEntrySet<>(type, name, prefix, quarkModule,
                    baseBlock, baseType, blockSupplier, tab, lootMode,
                    itemFactory, tileHolder, renderType, palette, extraModelTransform, useMergedPalette, condition);
            e.recipeLocations.addAll(this.recipes);
            e.tags.putAll(this.tags);
            e.textures.addAll(textures);
            return e;
        }
    }

}