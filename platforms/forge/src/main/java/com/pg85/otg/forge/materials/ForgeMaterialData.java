package com.pg85.otg.forge.materials;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.pg85.otg.OTG;
import com.pg85.otg.common.LocalWorld;
import com.pg85.otg.common.materials.LocalMaterialData;
import com.pg85.otg.exception.InvalidConfigException;
import com.pg85.otg.logging.LogMarker;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.FallingBlock;
import net.minecraft.command.arguments.BlockStateArgument;
import net.minecraft.command.arguments.BlockStateInput;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Implementation of LocalMaterial that wraps one of Minecraft's Blocks.
 *
 */
public class ForgeMaterialData extends LocalMaterialData
{
	private BlockState blockData;

    private ForgeMaterialData(BlockState blockData)
    {
        this.blockData = blockData;
    }
    
    private ForgeMaterialData(BlockState blockData, String raw)
    {
        this.blockData = blockData;
        this.rawEntry = raw;
    }
    
    private ForgeMaterialData(String raw)
    {
    	this.blockData = null;
    	this.rawEntry = raw;
    }
    
    public static ForgeMaterialData getBlank()
    {
    	// TODO: this null should probably be replaced with air
    	ForgeMaterialData material = new ForgeMaterialData((BlockState)null, null);
    	material.isBlank = true;
    	return material;
    }

    public static ForgeMaterialData ofString(String input) throws InvalidConfigException
    {
    	if(input == null)
    	{
    		return null;
    	}

        // Try parsing as an internal Minecraft name
        // This is so that things like "minecraft:stone" aren't parsed
        // as the block "minecraft" with data "stone", but instead as the
        // block "minecraft:stone" with no block data.    
    	
    	// Used in BO4's as placeholder/detector block.
    	if(input.toLowerCase().equals("blank"))
    	{
    		return ForgeMaterialData.getBlank();
    	}
    	
    	// Try blockname[blockdata] / minecraft:blockname[blockdata] syntax
    	Block block = null;
    	
    	// Use mc /setblock command logic to parse block string for us <3
		BlockStateArgument blockStateArgument = new BlockStateArgument();
		BlockStateInput parseResult = null;
		try {
			String newInput = input.contains(":") ? input : "minecraft:" + input;
			parseResult = blockStateArgument.parse(new StringReader(newInput));
		} catch (CommandSyntaxException e) { }
		
		if(parseResult != null)
		{
			return new ForgeMaterialData(parseResult.getState(), input);
		}
		
    	String blockNameCorrected = input.trim().toLowerCase();
    	// Remove any old metadata, fe STONE:0 or STONE:1 -> STONE
    	// TODO: this only works for single-digit metadata ><
    	try
    	{
    		Integer.parseInt(blockNameCorrected.substring(blockNameCorrected.length() - 1));
    		if(blockNameCorrected.substring(blockNameCorrected.length() - 2, blockNameCorrected.length() - 1).equals(":"))
    		{
    			blockNameCorrected = blockNameCorrected.substring(0, blockNameCorrected.length() - 2);
    		}
    	} catch(NumberFormatException ex) { }
		
    	try
    	{
    		// This returns AIR if block is not found ><.
    		block = ForgeRegistries.BLOCKS.getValue(new ResourceLocation(blockNameCorrected));
    	} catch(net.minecraft.util.ResourceLocationException ex) { }
    	
    	if(block != null && (block != Blocks.AIR || blockNameCorrected.toLowerCase().endsWith("air")))
    	{
    		return ofMinecraftBlock(block, input);
    	}
    	
    	block = fromLegacyBlockName(blockNameCorrected);
    	if(block != null)
    	{
    		return ofMinecraftBlock(block, input);
    	}

		OTG.log(LogMarker.INFO, "Could not parse block: " + input + ", substituting AIR.");

		return ofMinecraftBlock(Blocks.AIR, input);
    }
    
    // TODO: Convert all the old blockname:metadata to new block names.
    private static Block fromLegacyBlockName(String oldBlockName)
    {    	
    	switch(oldBlockName.replace("minecraft:", ""))
    	{
    		case "stationary_water":
    			return Blocks.WATER;
    		case "stationary_lava":
    			return Blocks.LAVA;
    		case "stained_clay":
    			return Blocks.WHITE_TERRACOTTA;    			
    		case "hard_clay":
    			return Blocks.TERRACOTTA;
    		case "step":
    			return Blocks.STONE_STAIRS;
    		case "sugar_cane_block":
    			return Blocks.SUGAR_CANE;
    		case "melon_block":
    			return Blocks.MELON;
    		case "water_lily":
    			return Blocks.LILY_PAD;
    		case "mycel":
    			return Blocks.MYCELIUM;
    		case "snow_layer":
    			return Blocks.SNOW;

    		case "mcpitman":
    			return Blocks.CREEPER_HEAD;
    		case "pg85":
    			return Blocks.ZOMBIE_HEAD;
    		case "supercoder":
    			return Blocks.CAKE;
    		case "authvin":
				return Blocks.WET_SPONGE;
    		case "josh":
				return Blocks.BARREL;				
    		case "wahrheit":
				return Blocks.SEA_PICKLE;
    		case "lordsmellypants":
				return Blocks.FLOWER_POT;				
			default:
				return null;
    	}
    }
    
    /**
     * Gets a {@code BukkitMaterialData} of the given Minecraft block. The
     * default block data (usually 0) will be used.
     * @param block The material.
     * @return The {@code BukkitMateialData} instance.
     */
    public static ForgeMaterialData ofMinecraftBlock(Block block, String raw)
    {
        return ofMinecraftBlockState(block.getDefaultState(), raw);
    }

    /**
     * Gets a {@code ForgeMaterialData} of the given Minecraft blockData.
     * @param blockData The material an data.
     * @return The {@code BukkitMateialData} instance.
     */
    public static ForgeMaterialData ofMinecraftBlockState(BlockState blockData)
    {
        return new ForgeMaterialData(blockData, null);
    }
    
    /**
     * Gets a {@code ForgeMaterialData} of the given Minecraft blockData.
     * @param blockData The material an data.
     * @return The {@code BukkitMateialData} instance.
     */
    public static ForgeMaterialData ofMinecraftBlockState(BlockState blockData, String raw)
    {
        return new ForgeMaterialData(blockData, raw);
    }   

    @Override
    public LocalMaterialData withDefaultBlockData()
    {
    	if(this.blockData == null)
    	{
    		return this;
    	}
        Block block = this.blockData.getBlock();
        //return this.withBlockData(block.getMetaFromState(block.getDefaultState()));
        return ofMinecraftBlock(block, this.rawEntry);
    }
    
    @Override
    public String getName()
    {
    	if(isBlank)
    	{
    		return "BLANK";
    	}
    	else if(this.blockData == null)
    	{
    		if(this.rawEntry != null)
    		{
    			return this.rawEntry;
    		} else {
    			return "Unknown";
    		}
    	} else {
	        Block block = this.blockData.getBlock();	
	        //byte data = getBlockData();
	        //boolean noData = this.blockData.getPropertyKeys().isEmpty();
	        // Note that the above line is not equivalent to data != 0, as for
	        // example pumpkins have a default data value of 2
	
	        //boolean nonDefaultData = !block.getDefaultState().equals(this.blockData);
	        
            // Use Minecraft's name
            //if (nonDefaultData)
            //{
            	//return Block.REGISTRY.getNameForObject(block) + (noData ? "" : ":" + data);
            //} else {
            	return block.getRegistryName().toString();
            //}
    	}
    }
    
    public BlockState internalBlock()
    {
        return this.blockData;
    }

    @Override
    public boolean isMaterial(LocalMaterialData material)
    {
    	// TODO: Compare registry names?
        return this.blockData.getBlock().equals(((ForgeMaterialData)material).internalBlock().getBlock());
    }
    
    @Override
    public boolean isLiquid()
    {
        return this.blockData == null ? false : this.blockData.getMaterial().isLiquid();
    }

    @Override
    public boolean isSolid()
    {   	
        return this.blockData == null ? false : this.blockData.getMaterial().isSolid();
    }
    
    @Override
    public boolean isEmptyOrAir()
    {
        return this.blockData == null ? true : this.blockData.getBlock() == Blocks.AIR;
    }
    
    @Override
    public boolean isAir()
    {
        return this.blockData != null && this.blockData.getBlock() == Blocks.AIR;
    }
    
    @Override
    public boolean isEmpty()
    {
        return this.blockData == null;
    }

    @Override
    public boolean canFall()
    {
        return this.blockData == null ? false : this.blockData.getBlock() instanceof FallingBlock;
    }

    @Override
    public boolean canSnowFallOn()
    {
        return this.blockData == null ? false : this.blockData.getMaterial().isSolid();    	
    }

	@Override
	public LocalMaterialData parseForWorld(LocalWorld world)
	{
        if (!this.checkedFallbacks && this.isEmpty() && this.rawEntry != null)
		{
			this.checkedFallbacks = true;
			ForgeMaterialData newMaterialData = ((ForgeMaterialData)world.getConfigs().getWorldConfig().parseFallback(this.rawEntry)); 
			if(newMaterialData != null && newMaterialData.blockData != null)
			{
				// TODO: Should blockData be a clone?
				this.blockData = newMaterialData.blockData;
				this.rawEntry = newMaterialData.rawEntry;
			}
		}
		return this;
	}
	
    @Override
    public boolean hasData()
    {
    	// TODO: Implement this for 1.16    	
    	return false;
    }
	
    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
        {
            return true;
        }
        if (!(obj instanceof ForgeMaterialData))
        {
            return false;
        }
        ForgeMaterialData other = (ForgeMaterialData) obj;
        return this.blockData.equals(other.blockData);
    }
    
    /**
     * Gets the hashCode of the material, based on the block id and block data.
     * The hashCode must be unique, which is possible considering that there are
     * only 4096 * 16 possible materials.
     * 
     * @return The unique hashCode.
     */
    @Override
    public int hashCode()
    {
    	// TODO: Implement this for 1.16
        return this.blockData == null ? -1 : this.blockData.hashCode();
    }
}