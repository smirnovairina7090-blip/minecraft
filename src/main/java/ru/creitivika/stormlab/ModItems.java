package ru.creitivika.stormlab;

import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;

public final class ModItems {
    public static final Item STORM_CORE = register(
            new StormCoreItem(new Item.Properties().stacksTo(1)),
            "storm_core"
    );

    private ModItems() {
    }

    private static Item register(Item item, String id) {
        ResourceLocation itemId = ResourceLocation.fromNamespaceAndPath(StormLabMod.MOD_ID, id);
        return Registry.register(BuiltInRegistries.ITEM, itemId, item);
    }

    public static void initialize() {
        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.TOOLS_AND_UTILITIES)
                .register(entries -> entries.accept(STORM_CORE));
    }
}
