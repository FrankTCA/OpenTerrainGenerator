package com.pg85.otg.forge.events.server;

import java.io.File;
import net.minecraft.world.World;
import net.minecraft.world.storage.ISaveHandler;
import net.minecraft.world.storage.WorldInfo;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.fml.common.event.FMLServerAboutToStartEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import com.pg85.otg.OTG;
import com.pg85.otg.configuration.dimensions.DimensionConfig;
import com.pg85.otg.configuration.dimensions.DimensionsConfig;
import com.pg85.otg.configuration.standard.PluginStandardValues;
import com.pg85.otg.forge.ForgeEngine;
import com.pg85.otg.forge.OTGPlugin;
import com.pg85.otg.forge.blocks.portal.PortalColors;
import com.pg85.otg.forge.commands.OTGCommandHandler;
import com.pg85.otg.forge.dimensions.OTGDimensionManager;
import com.pg85.otg.forge.gui.GuiHandler;
import com.pg85.otg.forge.world.OTGWorldType;
import com.pg85.otg.logging.LogMarker;

public class ServerEventListener
{	
    public static void serverAboutToStart(FMLServerAboutToStartEvent event)
    {
    	// Default settings are not restored on world unload / server quit 
    	// because this was causing problems (unloading dimensions while 
    	// their worlds were still ticking etc).
    	// Unload all world and biomes on server start / connect instead, 
    	// for SP client where data is kept when leaving the game.
 
    	((ForgeEngine)OTG.getEngine()).getWorldLoader().unloadAndUnregisterAllWorlds();
    	GuiHandler.loadGuiPresets();
    }

    public static void serverLoad(FMLServerStartingEvent event)
    {
        event.registerServerCommand(new OTGCommandHandler());

        World overWorld = DimensionManager.getWorld(0);

        if(overWorld.getWorldInfo().getGeneratorOptions().equals(PluginStandardValues.PLUGIN_NAME) && !(overWorld.getWorldInfo().getTerrainType() instanceof OTGWorldType))
        {
            ISaveHandler isavehandler = overWorld.getSaveHandler();
            WorldInfo worldInfo = isavehandler.loadWorldInfo();

            if(worldInfo != null)
            {
            	overWorld.getWorldInfo().setTerrainType(OTGPlugin.OtgWorldType);
            	worldInfo.setTerrainType(OTGPlugin.OtgWorldType);
            	isavehandler.saveWorldInfo(worldInfo);
            }
            throw new RuntimeException("OTG has detected that you are loading an OTG world that has been used without OTG installed. OTG has fixed and saved the world data, you can now restart the game and enter the world.");
        }

		if(!overWorld.isRemote) // Server side only
		{
			// This is a vanilla overworld, a new OTG world or a legacy OTG world without a dimensionconfig
		    if(OTG.getDimensionsConfig() == null) // Only happens when creating a new world?
		    {
				// Check if there is a dimensionsConfig saved for this world
				DimensionsConfig dimsConfig = DimensionsConfig.loadFromFile(overWorld.getSaveHandler().getWorldDirectory(), OTG.getEngine().getOTGRootFolder());
				if(dimsConfig == null)
				{
					// If there is no DimensionsConfig saved for this world, create one
					// LoadCustomDimensionData will add dimensions if any were saved
					dimsConfig = new DimensionsConfig(overWorld.getSaveHandler().getWorldDirectory());
					// If this is a vanilla overworld then we can be sure no dimensions were saved,
					if(!overWorld.getWorldInfo().getGeneratorOptions().equals(PluginStandardValues.PLUGIN_NAME))
					{
						// Create a dummy overworld config
						dimsConfig.Overworld = new DimensionConfig();
						// Check if there is a modpack config for vanilla worlds, 
						// since we didn't get a chance to check via the OTG world creation UI
						DimensionsConfig modPackConfig = OTG.getEngine().getModPackConfigManager().getModPackConfig(null);
						if(modPackConfig != null)
						{
							dimsConfig.Overworld = modPackConfig.Overworld.clone();
							for(DimensionConfig dimConfig : modPackConfig.Dimensions)
							{
						    	if(!OTGDimensionManager.isDimensionNameRegistered(dimConfig.PresetName))
					    		{
						    		File worldConfigFile = new File(OTG.getEngine().getOTGRootFolder().getAbsolutePath() + File.separator + PluginStandardValues.PresetsDirectoryName + File.separator + dimConfig.PresetName + File.separator + "WorldConfig.ini");
						    		if(worldConfigFile.exists())
						    		{
						    			DimensionConfig newConfig = dimConfig.clone();
						    	        // Ensure the portal color is unique (not already in use), otherwise correct it.
						    			PortalColors.correctPortalColor(newConfig, dimsConfig.getAllDimensions());
					                	dimsConfig.Dimensions.add(newConfig);
						    		}
					    		}
							}
						}
					}
					dimsConfig.save();
				}
				OTG.setDimensionsConfig(dimsConfig);
			}
	
		    // Load any saved dimensions.
		    OTGDimensionManager.loadCustomDimensionData();	
		    for(DimensionConfig dimConfig : OTG.getDimensionsConfig().Dimensions)
		    {
		    	if(!OTGDimensionManager.isDimensionNameRegistered(dimConfig.PresetName))
	    		{
		    		File worldConfigFile = new File(OTG.getEngine().getOTGRootFolder().getAbsolutePath() + File.separator + PluginStandardValues.PresetsDirectoryName + File.separator + dimConfig.PresetName + File.separator + "WorldConfig.ini");
		    		if(!worldConfigFile.exists())
		    		{
		    			OTG.log(LogMarker.WARN, "Could not create dimension \"" + dimConfig.PresetName + "\", OTG preset " + dimConfig.PresetName + " could not be found or does not contain a WorldConfig.ini file.");
		    		} else {
		    			OTG.IsNewWorldBeingCreated = true;
		    			if(!OTGDimensionManager.createDimension(dimConfig, false))
		    			{
		    				OTG.log(LogMarker.WARN, "Could not create dimension \"" + dimConfig.PresetName + "\" at id " + dimConfig.DimensionId + ", the id is already taken.");	
		    			}
		    			OTG.IsNewWorldBeingCreated = false;
		    		}
	    		}
		    }
		    OTGDimensionManager.SaveDimensionData();
		}
    }
}