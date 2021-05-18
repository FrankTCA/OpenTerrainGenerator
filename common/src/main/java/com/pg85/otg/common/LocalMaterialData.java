package com.pg85.otg.common;

import com.pg85.otg.OTGEngine;
import com.pg85.otg.configuration.biome.BiomeConfig;
import com.pg85.otg.util.helpers.BlockHelper;
import com.pg85.otg.util.minecraft.defaults.DefaultMaterial;

// TODO: Make this class unmodifiable (parseForWorld modifies atm),
// implement a world-specific materials cache and ensure only one
// instance of each unique material (id+metadata) exists in memory.
// TODO: Do creation of new material instances in one place only?

/**
 * Represents one of Minecraft's materials. Also includes its data value.
 * Immutable.
 * 
 * @see OTGEngine#readMaterial(String)
 * @see OTGEngine#toLocalMaterialData(DefaultMaterial, int)
 */
public abstract class LocalMaterialData
{
	protected DefaultMaterial defaultMaterial;
	protected String rawEntry;
	protected boolean isBlank = false;
	protected boolean checkedFallbacks = false;
	protected boolean parsedDefaultMaterial = false;
	
    /**
     * Gets a {@code LocalMaterialData} of the given material and data.
     * @param material The material.
     * @param data     The block data.
     * @return The {@code LocalMaterialData} instance.
     */
	protected abstract LocalMaterialData ofDefaultMaterialPrivate(DefaultMaterial material, int data);
    
    /**
     * Gets the name of this material. If a {@link #toDefaultMaterial()
     * DefaultMaterial is available,} that name is used, otherwise it's up to
     * the mod that provided this block to name it. Block data is appended to
     * the name, separated with a colon, like "WOOL:2".
     * 
     * @return The name of this material.
     */
	public abstract String getName();

    /**
     * Gets the internal block id. At the moment, all of Minecraft's vanilla
     * materials have a static id, but this can change in the future. Mods
     * already have dynamic ids.
     * 
     * @return The internal block id.
     */
	public abstract int getBlockId();

    /**
     * Gets the internal block data. Block data represents things like growth
     * stage and rotation.
     * 
     * @return The internal block data.
     */
    public abstract byte getBlockData();

    /**
     * Gets whether this material is a liquid, like water or lava.
     * 
     * @return True if this material is a liquid, false otherwise.
     */
    public abstract boolean isLiquid();

    /**
     * Gets whether this material is solid. If there is a
     * {@link #toDefaultMaterial() DefaultMaterial available}, this property is
     * defined by {@link DefaultMaterial#isSolid()}. Otherwise, it's up to the
     * mod that provided this block to say whether it's solid or not.
     * 
     * @return True if this material is solid, false otherwise.
     */
    public abstract boolean isSolid();

    /**
     * Gets whether this material is air. This is functionally equivalent to
     * {@code isMaterial(DefaultMaterial.AIR)}, but may yield better
     * performance.
     * @return True if this material is air, false otherwise.
     */
    public abstract boolean isEmptyOrAir();
    
    public abstract boolean isAir();

    public abstract boolean isEmpty();
    
    /**
     * Gets the default material belonging to this material. The block data will
     * be lost. If the material is not one of the vanilla Minecraft materials,
     * {@link DefaultMaterial#UNKNOWN_BLOCK} is returned.
     * 
     * @return The default material.
     */
    public abstract DefaultMaterial toDefaultMaterial();

    /**
     * Gets whether snow can fall on this block.
     * 
     * @return True if snow can fall on this block, false otherwise.
     */
    public abstract boolean canSnowFallOn();

    /**
     * Gets whether the block is of the given material. Block data is ignored,
     * as {@link DefaultMaterial} doesn't include block data.
     * 
     * @param material
     *            The material to check.
     * @return True if this block is of the given material, false otherwise.
     */
    public abstract boolean isMaterial(DefaultMaterial material);

    /**
     * Gets an instance with the same material as this object, but with the
     * given block data. This instance is not modified.
     *
     * @param newData
     *            The new block data.
     * @return An instance with the given block data.
     */
    public abstract LocalMaterialData withBlockData(int newData);

    /**
     * Gets an instance with the same material as this object, but the default
     * block data of the material. This instance is not modified.
     *
     * @return An instance with the default block data.
     */
    public abstract LocalMaterialData withDefaultBlockData();

    /**
     * Gets whether this material equals another material. The block data is
     * taken into account.
     * 
     * @param other
     *            The other material.
     * @return True if the materials are equal, false otherwise.
     */
    public abstract boolean equals(Object other);

    /**
     * Gets the hashCode of the material, based on the block id and block data.
     * The hashCode must be unique, which is possible considering that there are
     * only 4096 * 16 possible materials.
     * 
     * @return The unique hashCode.
     */
    public abstract int hashCode();

    /**
     * Gets the hashCode of the material, based on only the block id. No
     * hashCode returned by this method may be the same as any hashCode returned
     * by {@link #hashCode()}.
     * 
     * @return The unique hashCode.
     */
    public int hashCodeWithoutBlockData()
    {
        // From 0 to 4095 when there are 4096 block ids
        return getBlockId();
    }
    
    public String toString()
    {
    	return getName();
    }   

    /**
     * Gets a new material that is rotated 90 degrees. North -> west -> south ->
     * east. If this material cannot be rotated, the material itself is
     * returned.
     * 
     * @return The rotated material.
     */
    public LocalMaterialData rotate()
    {
    	return rotate(1);
    }
    
    /**
     * Gets a new material that is rotated 90 degrees. North -> west -> south ->
     * east. If this material cannot be rotated, the material itself is
     * returned.
     * 
     * @return The rotated material.
     */
    public LocalMaterialData rotate(int rotateTimes)
    {
    	// TODO: Rotate modded blocks?
    	
        // Try to rotate
        DefaultMaterial defaultMaterial = toDefaultMaterial();
        if (defaultMaterial != null)
        {
            // We only know how to rotate vanilla blocks
        	byte blockDataByte = 0;
            int newData = 0;
            for(int i = 0; i < rotateTimes; i++)
            {
            	blockDataByte = getBlockData();
            	newData = BlockHelper.rotateData(defaultMaterial, blockDataByte);	
            }
            if (newData != blockDataByte)
            {
            	return ofDefaultMaterialPrivate(defaultMaterial, newData);
            }
        }

        // No changes, return object itself
        return this;
    }

    /**
     * Parses this material through the fallback system of the world if required.
     * 
     * @param world The world this material will be parsed through, each world may have different fallbacks.
     * @return The parsed material
     */
    public abstract LocalMaterialData parseForWorld(LocalWorld world);
    
	public LocalMaterialData parseWithBiomeAndHeight(LocalWorld world, BiomeConfig biomeConfig, int y)
	{	
        if (!biomeConfig.worldConfig.biomeConfigsHaveReplacement)
        {
            // Don't waste time here, ReplacedBlocks is empty everywhere
            return this;
        }
        return biomeConfig.replacedBlocks.replaceBlock(y, this);
	}

    /**
     * Gets whether this material falls down when no other block supports this
     * block, like gravel and sand do.
     * @return True if this material can fall, false otherwise.
     */
    public abstract boolean canFall();
    
    /**
     * Gets whether this material can be used as an anchor point for a smooth area    
     * 
     * @return True if this material is a solid block, false if it is a tile-entity, half-slab, stairs(?), water, wood or leaves
     */    
    public boolean isSmoothAreaAnchor(boolean allowWood, boolean ignoreWater)
    {
    	return
			(
				isSolid() || 
				(
					!ignoreWater && isLiquid()
				)
			) || (
	    		(
					isMaterial(DefaultMaterial.ICE) ||
					isMaterial(DefaultMaterial.PACKED_ICE) ||
					isMaterial(DefaultMaterial.FROSTED_ICE)
				) && (
					allowWood || 
					!(
						isMaterial(DefaultMaterial.LOG) || 
						isMaterial(DefaultMaterial.LOG_2)
					)
				) &&
				!isMaterial(DefaultMaterial.WATER_LILY)
			);
    }

	public abstract boolean hasData();
}
