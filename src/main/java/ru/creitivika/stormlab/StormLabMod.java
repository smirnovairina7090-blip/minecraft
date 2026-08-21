package ru.creitivika.stormlab;

import net.fabricmc.api.ModInitializer;

public class StormLabMod implements ModInitializer {
    public static final String MOD_ID = "storm_lab";

    @Override
    public void onInitialize() {
        ModItems.initialize();
    }
}
