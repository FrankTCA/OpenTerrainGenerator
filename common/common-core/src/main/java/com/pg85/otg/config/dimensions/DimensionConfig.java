package com.pg85.otg.config.dimensions;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.pg85.otg.OTG;
import com.pg85.otg.constants.Constants;

/**
 * Used for Forge MP at world creation, defines the overworld/nether/end and any custom dimensions for a world.
 * May also be used for ModPack Configs in the future, hence the yaml code.
 */
public class DimensionConfig
{
	private boolean isModpackConfig = false;
	// Use capitals since we're serialising to yaml and want to make it look nice.
	public int Version;
	public String ModpackName;
	public OTGOverWorld Overworld;
	public OTGDimension Nether;
	public OTGDimension End;
	public List<OTGDimension> Dimensions = new ArrayList<>();

	// Parameterless constructor for deserialisation
	public DimensionConfig() { }
	
	public boolean isModpackConfig()
	{
		return this.isModpackConfig;
	}
	
	public static DimensionConfig createDefaultConfig()
	{
		DimensionConfig config = new DimensionConfig();
		config.Overworld = new OTGOverWorld(null, -1, null, null);
		config.Nether = new OTGDimension(null, -1);
		config.End = new OTGDimension(null, -1);
		return config;
	}
	
	public static DimensionConfig fromDisk(String fileName)
	{		
		File dimensionConfig = new File(OTG.getEngine().getOTGRootFolder().toString(), Constants.DIMENSION_CONFIGS_FOLDER + File.separator + fileName + ".yaml");
		if(dimensionConfig.exists())
		{
			DimensionConfig dimConfig = new DimensionConfig();
	        String content = "";
	        try
	        {
	            content = new String(Files.readAllBytes(dimensionConfig.toPath()));
	        }
	        catch (IOException e) 
	        {
	            e.printStackTrace();
	        }
	        DimensionConfig loadedConfig = fromYamlString(content);
	        if(loadedConfig != null)
	        {
		        dimConfig.isModpackConfig = true;
		        dimConfig.Version = loadedConfig.Version;
		        dimConfig.ModpackName = loadedConfig.ModpackName;
		        dimConfig.Overworld = loadedConfig.Overworld;
		        dimConfig.Nether = loadedConfig.Nether;
		        dimConfig.End = loadedConfig.End;
		        dimConfig.Dimensions = loadedConfig.Dimensions;
		        return dimConfig;
	        }	        
		}
		return null;
	}

	public static DimensionConfig fromYamlString(String input)
	{
		ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
		DimensionConfig dimConfig = null;

		try {
			dimConfig = mapper.readValue(input, DimensionConfig.class);
		} catch (JsonParseException e) {
			e.printStackTrace();
		} catch (JsonMappingException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return dimConfig;
	}

	public String toYamlString()
	{
		ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
		try {
			return mapper.writeValueAsString(this);
		} catch (JsonProcessingException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	// Clone method for GUI, to accommodate cancel button / rollbacks.
	public DimensionConfig clone()
	{
		DimensionConfig clone = new DimensionConfig();

		clone.isModpackConfig = this.isModpackConfig;
		clone.Version = this.Version;
		clone.ModpackName = this.ModpackName;
		clone.Overworld = this.Overworld == null ? null : this.Overworld.clone();
		clone.Nether = this.Nether == null ? null : this.Nether.clone();
		clone.End = this.End == null ? null : this.End.clone();
		clone.Dimensions = new ArrayList<>();
		for(OTGDimension dim : this.Dimensions)
		{
			clone.Dimensions.add(dim.clone());
		}
		clone.Settings = this.Settings == null ? null : this.Settings.clone();
		clone.GameRules = this.GameRules == null ? null : this.GameRules.clone();
		
		return clone;
	}
	
	public static class OTGOverWorld extends OTGDimension
	{
		public String NonOTGWorldType; // Only used for MP atm to create non-otg overworlds.
		public String NonOTGGeneratorSettings; // Only used for MP atm to create non-otg overworlds.
		
		public OTGOverWorld()
		{
			super();
		}

		public OTGOverWorld(String presetFolderName, long seed, String nonOTGWorldType, String nonOTGGeneratorSettings)
		{
			super(presetFolderName, seed);
			this.NonOTGWorldType = nonOTGWorldType;
			this.NonOTGGeneratorSettings = nonOTGGeneratorSettings;
		}
		
		public OTGOverWorld clone()
		{
			return new OTGOverWorld(this.PresetFolderName, this.Seed, this.NonOTGWorldType, this.NonOTGGeneratorSettings);
		}
	}

	public static class OTGDimension
	{
		public String PresetFolderName;
		public OTGDimension() {}
		
		public OTGDimension(String presetFolderName, long seed)
		{
			this.PresetFolderName = presetFolderName;
			this.Seed = seed;
		}
		
		public OTGDimension clone()
		{
			OTGDimension otgDimension = new OTGDimension(this.PresetFolderName, this.Seed);
			otgDimension.PortalBlocks = this.PortalBlocks;
			otgDimension.PortalColor = this.PortalColor;
			otgDimension.PortalMob = this.PortalMob;
			otgDimension.PortalIgnitionSource = this.PortalIgnitionSource;
			return otgDimension;
		}
	}
}
