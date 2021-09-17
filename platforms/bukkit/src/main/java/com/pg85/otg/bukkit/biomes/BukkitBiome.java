package com.pg85.otg.bukkit.biomes;

import com.pg85.otg.bukkit.world.WorldHelper;
import com.pg85.otg.common.LocalBiome;
import com.pg85.otg.configuration.biome.BiomeConfig;
import com.pg85.otg.util.BiomeIds;

import net.minecraft.server.v1_12_R1.BiomeBase;
import net.minecraft.server.v1_12_R1.BlockPosition;

/**
 * The BukkitBiome is basically a wrapper for the BiomeBase. If you look at
 * the constructor and the method you will see that this is the case.
 */
public class BukkitBiome implements LocalBiome
{
    private final BiomeBase biomeBase;
    private final boolean isCustom;
    private final BiomeIds biomeIds;
    private final BiomeConfig biomeConfig;

    private BukkitBiome(BiomeConfig biomeConfig, BiomeBase biome)
    {
        this.biomeBase = biome;
        int savedBiomeId =  BiomeBase.a(biomeBase);
        if(biomeConfig.replaceToBiomeName != null) {
            this.biomeIds = new BiomeIds(WorldHelper.getOTGBiomeId(biomeBase), savedBiomeId, !biomeConfig.replaceToBiomeName.isEmpty());
        } else {
            throw new IllegalStateException("biomeConfig.replaceToBiomeName was null");
        }
        this.biomeConfig = biomeConfig;
        this.isCustom = biome instanceof OTGBiomeBase;
    }
    
    /**
     * Creates and registers a new custom biome with the config and ids.
     *
     * @param biomeConfig Config of the custom biome.
     * @param biomeIds    Ids of the custom biome.
     * @return The custom biome.
     */
    public static BukkitBiome forCustomBiome(BiomeConfig biomeConfig, BiomeIds biomeIds, String worldName, boolean isReload)
    {
        return new BukkitBiome(biomeConfig, OTGBiomeBase.createInstance(biomeConfig, biomeIds, worldName, isReload));
    }

    @Override
    public boolean isCustom()
    {
        return this.isCustom;
    }

    public BiomeBase getHandle()
    {
        return biomeBase;
    }

    @Override
    public String getName()
    {
        return this.biomeConfig.getName();
    }

    @Override
    public BiomeIds getIds()
    {
        return this.biomeIds;
    }

    @Override
    public float getTemperatureAt(int x, int y, int z)
    {
        return this.biomeBase.a(new BlockPosition(x, y, z));
    }

    @Override
    public BiomeConfig getBiomeConfig()
    {
        return this.biomeConfig;
    }

    @Override
    public String toString()
    {
        return getName();
    }
}