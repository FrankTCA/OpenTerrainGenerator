package com.pg85.otg.forge.gen;

import java.nio.file.Path;
<<<<<<< HEAD
import java.util.*;
import java.util.Map.Entry;
=======
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.Random;
>>>>>>> parent of 6ef79ba5a (Merge remote-tracking branch 'origin/1.16.4' into 1.16.4)
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import com.google.common.collect.Maps;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.pg85.otg.OTG;
import com.pg85.otg.config.dimensions.DimensionConfig;
import com.pg85.otg.config.dimensions.DimensionConfig.OTGDimension;
import com.pg85.otg.constants.Constants;
import com.pg85.otg.constants.SettingsEnums.CustomStructureType;
import com.pg85.otg.customobject.structures.CustomStructureCache;
import com.pg85.otg.exceptions.InvalidConfigException;
import com.pg85.otg.forge.materials.ForgeMaterialData;
import com.pg85.otg.forge.presets.ForgePresetLoader;
import com.pg85.otg.forge.biome.ForgeBiome;
import com.pg85.otg.forge.biome.OTGBiomeProvider;
import com.pg85.otg.gen.OTGChunkGenerator;
import com.pg85.otg.gen.OTGChunkDecorator;
import com.pg85.otg.interfaces.IBiome;
import com.pg85.otg.interfaces.ICachedBiomeProvider;
import com.pg85.otg.interfaces.ILayerSource;
import com.pg85.otg.interfaces.IMaterialReader;
import com.pg85.otg.interfaces.IWorldConfig;
import com.pg85.otg.presets.Preset;
import com.pg85.otg.util.ChunkCoordinate;
import com.pg85.otg.util.gen.ChunkBuffer;
import com.pg85.otg.util.gen.JigsawStructureData;
import com.pg85.otg.util.materials.LocalMaterialData;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraft.block.BlockState;
import net.minecraft.crash.CrashReport;
import net.minecraft.crash.ReportedException;
import net.minecraft.util.SharedSeedRandom;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.MutableBoundingBox;
import net.minecraft.util.math.SectionPos;
import net.minecraft.world.Blockreader;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.IWorld;
<<<<<<< HEAD
import net.minecraft.world.biome.BiomeGenerationSettings;
=======
import net.minecraft.world.biome.Biome;
>>>>>>> parent of 6ef79ba5a (Merge remote-tracking branch 'origin/1.16.4' into 1.16.4)
import net.minecraft.world.biome.BiomeManager;
import net.minecraft.world.biome.provider.BiomeProvider;
import net.minecraft.world.chunk.ChunkPrimer;
import net.minecraft.world.chunk.IChunk;
import net.minecraft.world.gen.ChunkGenerator;
import net.minecraft.world.gen.DimensionSettings;
import net.minecraft.world.gen.GenerationStage;
import net.minecraft.world.gen.Heightmap;
import net.minecraft.world.gen.Heightmap.Type;
<<<<<<< HEAD
import net.minecraft.world.gen.carver.ConfiguredCarver;
=======
>>>>>>> parent of 6ef79ba5a (Merge remote-tracking branch 'origin/1.16.4' into 1.16.4)
import net.minecraft.world.gen.WorldGenRegion;
import net.minecraft.world.gen.feature.jigsaw.JigsawJunction;
import net.minecraft.world.gen.feature.jigsaw.JigsawPattern;
import net.minecraft.world.gen.feature.structure.AbstractVillagePiece;
import net.minecraft.world.gen.feature.structure.Structure;
import net.minecraft.world.gen.feature.structure.StructureManager;
import net.minecraft.world.gen.feature.structure.StructurePiece;
import net.minecraft.world.gen.settings.DimensionStructuresSettings;
import net.minecraft.world.gen.settings.StructureSeparationSettings;
import net.minecraft.world.gen.settings.StructureSpreadSettings;
import net.minecraft.world.server.ServerWorld;
import net.minecraft.world.storage.FolderName;
import net.minecraft.world.gen.NoiseChunkGenerator;

public final class OTGNoiseChunkGenerator extends NoiseChunkGenerator
{
	// Create a codec to serialise/deserialise OTGNoiseChunkGenerator
	public static final Codec<OTGNoiseChunkGenerator> CODEC = RecordCodecBuilder.create(
		(p_236091_0_) ->
		{
			return p_236091_0_
				.group(
					Codec.STRING.fieldOf("preset_folder_name").forGetter(
						(p_236090_0_) -> {
							return p_236090_0_.preset.getFolderName();
						}
					),
					Codec.STRING.fieldOf("dim_config_name").forGetter(
						(p_236090_0_) -> {
							return p_236090_0_.dimConfigName;
						}
					),
					BiomeProvider.CODEC.fieldOf("biome_source").forGetter(
						(p_236096_0_) -> { return p_236096_0_.biomeSource; }
					),
					Codec.LONG.fieldOf("seed").stable().forGetter(
						(p_236093_0_) -> { return p_236093_0_.seed; }
					),
					DimensionSettings.CODEC.fieldOf("settings").forGetter(
						(p_236090_0_) -> { return p_236090_0_.settings; }
					)
				).apply(
					p_236091_0_,
					p_236091_0_.stable(OTGNoiseChunkGenerator::new)
				)
			;
		}
	);

	private final ShadowChunkGenerator shadowChunkGenerator;
	private final OTGChunkGenerator internalGenerator;
	private final OTGChunkDecorator chunkDecorator;
	private final Preset preset;
	private final String dimConfigName;
	private final DimensionConfig dimConfig;
	private CustomStructureCache structureCache; // TODO: Move this?
	
	// TODO: Modpack config specific, move this?
	private boolean portalDataProcessed = false;
	private List<LocalMaterialData> portalBlocks;
	private String portalColor;
	private String portalMob;
	private String portalIgnitionSource;
	private boolean cavesEnabled;
	private boolean ravinesEnabled;

	public OTGNoiseChunkGenerator(BiomeProvider biomeProvider, long seed, Supplier<DimensionSettings> dimensionSettingsSupplier)
	{
		this(OTG.getEngine().getPresetLoader().getDefaultPresetFolderName(), null, biomeProvider, biomeProvider, seed, dimensionSettingsSupplier);
	}

	public OTGNoiseChunkGenerator(String presetFolderName, String dimConfigName, BiomeProvider biomeProvider, long seed, Supplier<DimensionSettings> dimensionSettingsSupplier)
	{
		this(presetFolderName, dimConfigName, biomeProvider, biomeProvider, seed, dimensionSettingsSupplier);
	}

	// TODO: Why are there 2 biome providers, and why does getBiomeProvider() return the second, while we're using the first?
	// It looks like vanilla just inserts the same biomeprovider twice?
	private OTGNoiseChunkGenerator(String presetFolderName, String dimConfigName, BiomeProvider biomeProvider1, BiomeProvider biomeProvider2, long seed, Supplier<DimensionSettings> dimensionSettingsSupplier)
	{
		super(biomeProvider1, biomeProvider2, seed, overrideStructureSettings(dimensionSettingsSupplier.get(), presetFolderName));

		if (!(biomeProvider1 instanceof ILayerSource))
		{
			throw new RuntimeException("OTG has detected an incompatible biome provider- try using otg:otg as the biome source name");
		}

		this.preset = OTG.getEngine().getPresetLoader().getPresetByFolderName(presetFolderName);
		if(dimConfigName != null && dimConfigName.trim().length() > 0)
		{
			this.dimConfigName = dimConfigName;
			this.dimConfig = DimensionConfig.fromDisk(this.dimConfigName);
		} else {
			this.dimConfigName = "";
			this.dimConfig = null;
		}	
		this.shadowChunkGenerator = new ShadowChunkGenerator(OTG.getEngine().getPluginConfig().getMaxWorkerThreads());
		this.internalGenerator = new OTGChunkGenerator(this.preset, seed, (ILayerSource) biomeProvider1,((ForgePresetLoader)OTG.getEngine().getPresetLoader()).getGlobalIdMapping(presetFolderName), OTG.getEngine().getLogger());
		this.chunkDecorator = new OTGChunkDecorator();
	}
	
	private static Supplier<DimensionSettings> overrideStructureSettings(DimensionSettings oldSettings, String presetFolderName)
	{
		Preset preset = OTG.getEngine().getPresetLoader().getPresetByFolderName(presetFolderName);
		IWorldConfig worldConfig = preset.getWorldConfig();

		Builder<Structure<?>, StructureSeparationSettings> separationSettings = ImmutableMap.<Structure<?>, StructureSeparationSettings>builder();
		if(worldConfig.getVillagesEnabled())
		{
			separationSettings.put(Structure.VILLAGE, new StructureSeparationSettings(worldConfig.getVillageSpacing(), worldConfig.getVillageSeparation(), 10387312));
		}
		if(worldConfig.getRareBuildingsEnabled())
		{
			separationSettings.put(Structure.DESERT_PYRAMID, new StructureSeparationSettings(worldConfig.getDesertPyramidSpacing(), worldConfig.getDesertPyramidSeparation(), 14357617));
		}
		if(worldConfig.getRareBuildingsEnabled())
		{
			separationSettings.put(Structure.IGLOO, new StructureSeparationSettings(worldConfig.getIglooSpacing(), worldConfig.getIglooSeparation(), 14357618));
		}
		if(worldConfig.getRareBuildingsEnabled())
		{
			separationSettings.put(Structure.JUNGLE_TEMPLE, new StructureSeparationSettings(worldConfig.getJungleTempleSpacing(), worldConfig.getJungleTempleSeparation(), 14357619));
		}
		if(worldConfig.getRareBuildingsEnabled())
		{
			separationSettings.put(Structure.SWAMP_HUT, new StructureSeparationSettings(worldConfig.getSwampHutSpacing(), worldConfig.getSwampHutSeparation(), 14357620));
		}
		if(worldConfig.getPillagerOutpostsEnabled())
		{
			separationSettings.put(Structure.PILLAGER_OUTPOST, new StructureSeparationSettings(worldConfig.getPillagerOutpostSpacing(), worldConfig.getPillagerOutpostSeparation(), 165745296));
		}
		if(worldConfig.getStrongholdsEnabled())
		{
			separationSettings.put(Structure.STRONGHOLD, new StructureSeparationSettings(worldConfig.getStrongholdSpacing(), worldConfig.getStrongholdSeparation(), 0));
		}
		if(worldConfig.getOceanMonumentsEnabled())
		{
			separationSettings.put(Structure.OCEAN_MONUMENT, new StructureSeparationSettings(worldConfig.getOceanMonumentSpacing(), worldConfig.getOceanMonumentSeparation(), 10387313));
		}
		if(worldConfig.getEndCitiesEnabled())
		{
			separationSettings.put(Structure.END_CITY, new StructureSeparationSettings(worldConfig.getEndCitySpacing(), worldConfig.getEndCitySeparation(), 10387313));
		}
		if(worldConfig.getWoodlandMansionsEnabled())
		{
			separationSettings.put(Structure.WOODLAND_MANSION, new StructureSeparationSettings(worldConfig.getWoodlandMansionSpacing(), worldConfig.getWoodlandMansionSeparation(), 10387319));
		}
		if(worldConfig.getBuriedTreasureEnabled())
		{
			separationSettings.put(Structure.BURIED_TREASURE, new StructureSeparationSettings(worldConfig.getBuriedTreasureSpacing(), worldConfig.getBuriedTreasureSeparation(), 0));
		}
		if(worldConfig.getMineshaftsEnabled())
		{
			separationSettings.put(Structure.MINESHAFT, new StructureSeparationSettings(worldConfig.getMineshaftSpacing(), worldConfig.getMineshaftSeparation(), 0));
		}
		if(worldConfig.getRuinedPortalsEnabled())
		{
			separationSettings.put(Structure.RUINED_PORTAL, new StructureSeparationSettings(worldConfig.getRuinedPortalSpacing(), worldConfig.getRuinedPortalSeparation(), 34222645));
		}
		if(worldConfig.getShipWrecksEnabled())
		{
			separationSettings.put(Structure.SHIPWRECK, new StructureSeparationSettings(worldConfig.getShipwreckSpacing(), worldConfig.getShipwreckSeparation(), 165745295));
		}
		if(worldConfig.getOceanRuinsEnabled())
		{
			separationSettings.put(Structure.OCEAN_RUIN, new StructureSeparationSettings(worldConfig.getOceanRuinSpacing(), worldConfig.getOceanRuinSeparation(), 14357621));
		}
		if(worldConfig.getBastionRemnantsEnabled())
		{
			separationSettings.put(Structure.BASTION_REMNANT, new StructureSeparationSettings(worldConfig.getBastionRemnantSpacing(), worldConfig.getBastionRemnantSeparation(), 30084232));
		}
		if(worldConfig.getNetherFortressesEnabled())
		{
			separationSettings.put(Structure.NETHER_BRIDGE, new StructureSeparationSettings(worldConfig.getNetherFortressSpacing(), worldConfig.getNetherFortressSeparation(), 30084232));
		}
		if(worldConfig.getNetherFossilsEnabled())
		{
			separationSettings.put(Structure.NETHER_FOSSIL, new StructureSeparationSettings(worldConfig.getNetherFossilSpacing(), worldConfig.getNetherFossilSeparation(), 14357921));
		}
		separationSettings.putAll(
			oldSettings.structureSettings().structureConfig().entrySet().stream().filter(a -> 
				a.getKey() != Structure.VILLAGE &&
				a.getKey() != Structure.DESERT_PYRAMID &&
				a.getKey() != Structure.IGLOO &&
				a.getKey() != Structure.JUNGLE_TEMPLE &&
				a.getKey() != Structure.SWAMP_HUT &&
				a.getKey() != Structure.PILLAGER_OUTPOST &&
				a.getKey() != Structure.STRONGHOLD &&
				a.getKey() != Structure.OCEAN_MONUMENT &&
				a.getKey() != Structure.END_CITY &&
				a.getKey() != Structure.WOODLAND_MANSION &&
				a.getKey() != Structure.BURIED_TREASURE &&
				a.getKey() != Structure.MINESHAFT &&
				a.getKey() != Structure.RUINED_PORTAL &&
				a.getKey() != Structure.SHIPWRECK &&
				a.getKey() != Structure.OCEAN_RUIN &&
				a.getKey() != Structure.BASTION_REMNANT &&
				a.getKey() != Structure.NETHER_BRIDGE &&
				a.getKey() != Structure.NETHER_FOSSIL
			).collect(Collectors.toMap(Entry::getKey, Entry::getValue))
		);

		DimensionStructuresSettings newSettings = new DimensionStructuresSettings(
			worldConfig.getStrongholdsEnabled() ? Optional.of(
				new StructureSpreadSettings(
					worldConfig.getStrongHoldDistance(),
					worldConfig.getStrongHoldSpread(),
					worldConfig.getStrongHoldCount()
				)
			) : Optional.empty(),
			Maps.newHashMap(separationSettings.build())
		);

		oldSettings.structureSettings = newSettings;
		
		return () -> { return oldSettings; };
	}

	public ICachedBiomeProvider getCachedBiomeProvider()
	{
		return this.internalGenerator.getCachedBiomeProvider();
	}

	public void saveStructureCache()
	{
		if (this.chunkDecorator.getIsSaveRequired() && this.structureCache != null)
		{
			this.structureCache.saveToDisk(OTG.getEngine().getLogger(), this.chunkDecorator);
		}
	}

	public Preset getPreset()
	{
		return this.preset;
	}

<<<<<<< HEAD
=======
	// TODO: Modpack config specific, move this?

	public boolean getCavesEnabled()
	{
		processDimensionConfigData();
		return this.cavesEnabled;
	}

	public boolean getRavinesEnabled()
	{
		processDimensionConfigData();
		return this.ravinesEnabled;
	}

	public String getPortalColor()
	{
		processDimensionConfigData();
		return this.portalColor;
	}

	public String getPortalMob()
	{
		processDimensionConfigData();
		return this.portalMob;
	}

	public String getPortalIgnitionSource()
	{
		processDimensionConfigData();
		return this.portalIgnitionSource;
	}
		
	public List<LocalMaterialData> getPortalBlocks()
	{
		processDimensionConfigData();
		return this.portalBlocks;
	}

	private void processDimensionConfigData()
	{
		if(!this.portalDataProcessed)
		{
			this.portalDataProcessed = true;
			if(this.dimConfig != null)
			{
				if(this.dimConfig.Overworld != null && this.dimConfig.Overworld.PresetFolderName != null && this.preset.getFolderName().equals(this.dimConfig.Overworld.PresetFolderName))
				{
					this.cavesEnabled = this.dimConfig.Overworld.CarversEnabled && this.preset.getWorldConfig().getCavesEnabled();
					this.ravinesEnabled = this.dimConfig.Overworld.CarversEnabled && this.preset.getWorldConfig().getRavinesEnabled();
				}
				else if(this.dimConfig.Nether != null && this.dimConfig.Nether.PresetFolderName != null && this.preset.getFolderName().equals(this.dimConfig.Nether.PresetFolderName))
				{
					this.cavesEnabled = this.dimConfig.Nether.CarversEnabled && this.preset.getWorldConfig().getCavesEnabled();
					this.ravinesEnabled = this.dimConfig.Nether.CarversEnabled && this.preset.getWorldConfig().getRavinesEnabled();
				}
				else if(this.dimConfig.End != null && this.dimConfig.End.PresetFolderName != null && this.preset.getFolderName().equals(this.dimConfig.End.PresetFolderName))
				{
					this.cavesEnabled = this.dimConfig.End.CarversEnabled && this.preset.getWorldConfig().getCavesEnabled();
					this.ravinesEnabled = this.dimConfig.End.CarversEnabled && this.preset.getWorldConfig().getRavinesEnabled();
				} else {
					IMaterialReader materialReader = OTG.getEngine().getPresetLoader().getMaterialReader(this.preset.getFolderName());
					for(OTGDimension dim : this.dimConfig.Dimensions)
					{
						if(dim.PresetFolderName != null && this.preset.getFolderName().equals(dim.PresetFolderName))
						{
							if(dim.PortalBlocks != null && dim.PortalBlocks.trim().length() > 0)
							{
								String[] portalBlocks = dim.PortalBlocks.split(",");
								ArrayList<LocalMaterialData> materials = new ArrayList<LocalMaterialData>();					
								for(String materialString : portalBlocks)
								{
									LocalMaterialData material = null;
									try {
										material = materialReader.readMaterial(materialString.trim());
									} catch (InvalidConfigException e) { }
									if(material != null)
									{
										materials.add(material);
									}
								}
								this.portalBlocks = materials;
							}					
							this.portalColor = dim.PortalColor;
							this.portalMob = dim.PortalMob;
							this.portalIgnitionSource = dim.PortalIgnitionSource;
							this.cavesEnabled = dim.CarversEnabled && this.preset.getWorldConfig().getCavesEnabled();
							this.ravinesEnabled = dim.CarversEnabled && this.preset.getWorldConfig().getRavinesEnabled();
							break;
						}
					}
				}
			} else {
				this.cavesEnabled = this.preset.getWorldConfig().getCavesEnabled();
				this.ravinesEnabled = this.preset.getWorldConfig().getRavinesEnabled();
			}
			if(this.portalBlocks == null || this.portalBlocks.size() == 0)
			{
				this.portalBlocks = this.preset.getWorldConfig().getPortalBlocks(); 
			}
			if(this.portalColor == null)
			{
				this.portalColor = this.preset.getWorldConfig().getPortalColor();	
			}
			if(this.portalMob == null)
			{
				this.portalMob = this.preset.getWorldConfig().getPortalMob();
			}
			if(this.portalIgnitionSource == null)
			{
				this.portalIgnitionSource = this.preset.getWorldConfig().getPortalIgnitionSource();
			}
		}
	}
	
	//
	
>>>>>>> parent of 6ef79ba5a (Merge remote-tracking branch 'origin/1.16.4' into 1.16.4)
	@OnlyIn(Dist.CLIENT)
	@Override
	public ChunkGenerator withSeed(long seed)
	{
		return new OTGNoiseChunkGenerator(this.preset.getFolderName(), this.dimConfigName, this.biomeSource.withSeed(seed), seed, this.settings);
	}

	@Override
	public int getSeaLevel()
	{
		return this.preset.getWorldConfig().getWaterLevelMax();
	}

	// Base terrain gen

	// Generates the base terrain for a chunk.
	@Override
	public void fillFromNoise(IWorld world, StructureManager manager, IChunk chunk)
	{
		ChunkCoordinate chunkCoord = ChunkCoordinate.fromChunkCoords(chunk.getPos().x, chunk.getPos().z);

		// Fetch any chunks that are cached in the WorldGenRegion, so we can
		// pre-emptively generate and cache base terrain for them asynchronously.
		this.shadowChunkGenerator.queueChunksForWorkerThreads((WorldGenRegion)world, manager, chunk, this, (OTGBiomeProvider)this.biomeSource, this.internalGenerator, this.getSettings(), this.preset.getWorldConfig().getWorldHeightCap());
		
		// If we've already (shadow-)generated and cached this	
		// chunk while it was unloaded, use cached data.
		ChunkBuffer buffer = new ForgeChunkBuffer((ChunkPrimer) chunk);
		IChunk cachedChunk = this.shadowChunkGenerator.getChunkWithWait(chunkCoord);
		if (cachedChunk != null)
		{
			this.shadowChunkGenerator.fillWorldGenChunkFromShadowChunk(chunk, cachedChunk);
		} else {			
			// Setup jigsaw data
			ObjectList<JigsawStructureData> structures = new ObjectArrayList<>(10);
			ObjectList<JigsawStructureData> junctions = new ObjectArrayList<>(32);
			ChunkPos pos = chunk.getPos();
			int chunkX = pos.x;
			int chunkZ = pos.z;
			int startX = chunkX << 4;
			int startZ = chunkZ << 4;

			// Iterate through all of the jigsaw structures (villages, pillager outposts, nether fossils)
			for(Structure<?> structure : Structure.NOISE_AFFECTING_FEATURES)
			{
				// Get all structure starts in this chunk
				manager.startsForFeature(SectionPos.of(pos, 0), structure).forEach((start) ->
				{
					// Iterate through the pieces in the structure
					for(StructurePiece piece : start.getPieces())
					{
						// Check if it intersects with this chunk
						if (piece.isCloseToChunk(pos, 12))
						{
							MutableBoundingBox box = piece.getBoundingBox();
							if (piece instanceof AbstractVillagePiece)
							{
								AbstractVillagePiece villagePiece = (AbstractVillagePiece) piece;
								// Add to the list if it's a rigid piece
								if (villagePiece.getElement().getProjection() == JigsawPattern.PlacementBehaviour.RIGID)
								{
									structures.add(new JigsawStructureData(box.x0, box.y0, box.z0, box.x1, villagePiece.getGroundLevelDelta(), box.z1, true, 0, 0, 0));
								}

								// Get all the junctions in this piece
								for(JigsawJunction junction : villagePiece.getJunctions())
								{
									int sourceX = junction.getSourceX();
									int sourceZ = junction.getSourceZ();

									// If the junction is in this chunk, then add to list
									if (sourceX > startX - 12 && sourceZ > startZ - 12 && sourceX < startX + 15 + 12 && sourceZ < startZ + 15 + 12)
									{
										junctions.add(new JigsawStructureData(0, 0, 0,0, 0, 0, false, junction.getSourceX(), junction.getSourceGroundY(), junction.getSourceZ()));
									}
								}
							} else {
								structures.add(new JigsawStructureData(box.x0, box.y0, box.z0,box.x1, 0, box.z1, false, 0, 0, 0));
							}
						}
					}
				});
			}
			this.internalGenerator.populateNoise(this.preset.getWorldConfig().getWorldHeightCap(), world.getRandom(), buffer, buffer.getChunkCoordinate(), structures, junctions);			
			this.shadowChunkGenerator.setChunkGenerated(chunkCoord);
		}
	}

	// Replaces surface and ground blocks in base terrain and places bedrock.
	@Override
	public void buildSurfaceAndBedrock(WorldGenRegion worldGenRegion, IChunk chunk)
	{
		// OTG handles surface/ground blocks during base terrain gen. For non-OTG biomes used
		// with TemplateForBiome, we want to use registered surfacebuilders though.

		ChunkPos chunkpos = chunk.getPos();
		int i = chunkpos.x;
		int j = chunkpos.z;
		SharedSeedRandom sharedseedrandom = new SharedSeedRandom();
		sharedseedrandom.setBaseChunkSeed(i, j);
		ChunkPos chunkpos1 = chunk.getPos();
		int chunkMinX = chunkpos1.getMinBlockX();
		int chunkMinZ = chunkpos1.getMinBlockZ();
		int worldX;
		int worldZ;
		int i2;
		double d1;
		IBiome[] biomesForChunk = this.internalGenerator.getCachedBiomeProvider().getBiomesForChunk(ChunkCoordinate.fromBlockCoords(chunkMinX, chunkMinZ));
		IBiome biome;
		
		for(int xInChunk = 0; xInChunk < Constants.CHUNK_SIZE; ++xInChunk)
		{
			for(int zInChunk = 0; zInChunk < Constants.CHUNK_SIZE; ++zInChunk)
			{
				worldX = chunkMinX + xInChunk;
				worldZ = chunkMinZ + zInChunk;
				biome = biomesForChunk[xInChunk * Constants.CHUNK_SIZE + zInChunk];
				if(biome.getBiomeConfig().getIsTemplateForBiome())
				{
					i2 = chunk.getHeight(Heightmap.Type.WORLD_SURFACE_WG, xInChunk, zInChunk) + 1;
					d1 = this.surfaceNoise.getSurfaceNoiseValue((double)worldX * 0.0625D, (double)worldZ * 0.0625D, 0.0625D, (double)xInChunk * 0.0625D) * 15.0D;
					((ForgeBiome)biome).getBiomeBase().buildSurfaceAt(sharedseedrandom, chunk, worldX, worldZ, i2, d1, ((ForgeMaterialData)biome.getBiomeConfig().getDefaultStoneBlock()).internalBlock(), ((ForgeMaterialData)biome.getBiomeConfig().getDefaultWaterBlock()).internalBlock(), this.getSeaLevel(), worldGenRegion.getSeed());
				}
			}
		}
		// Skip bedrock, OTG always handles that.
	}

	// Carvers: Caves and ravines

	@Override
	public void applyCarvers(long seed, BiomeManager biomeManager, IChunk chunk, GenerationStage.Carving stage)
<<<<<<< HEAD
	{		
		// OTG has its own caves and canyons carvers. We register default carvers to OTG biomes,
		// then check if they have been overridden by mods before using our own carvers.
		if (stage == GenerationStage.Carving.AIR)
		{
			ForgeBiome biome = (ForgeBiome)this.getCachedBiomeProvider().getNoiseBiome(chunk.getPos().x << 2, chunk.getPos().z << 2);
			BiomeGenerationSettings biomegenerationsettings = biome.getBiomeBase().getGenerationSettings();
			List<Supplier<ConfiguredCarver<?>>> list = biomegenerationsettings.getCarvers(stage);

			// Only use OTG carvers when default mc carvers are found
			List<String> defaultCaves = Arrays.asList("minecraft:cave", "minecraft:underwater_cave", "minecraft:nether_cave");			
			boolean cavesEnabled = this.preset.getWorldConfig().getCavesEnabled() && list.stream().anyMatch(
				a -> defaultCaves.stream().anyMatch(
					b -> b.equals(
						ForgeRegistries.WORLD_CARVERS.getKey(a.get().worldCarver).toString()
					)
				)
			);
			List<String> defaultRavines = Arrays.asList("minecraft:canyon", "minecraft:underwater_canyon");
			boolean ravinesEnabled = this.preset.getWorldConfig().getRavinesEnabled() && list.stream().anyMatch(
				a -> defaultRavines.stream().anyMatch(
					b -> b.equals(
						ForgeRegistries.WORLD_CARVERS.getKey(a.get().worldCarver).toString()
					)
				)
			);
			if(cavesEnabled || ravinesEnabled)
			{
				ChunkPrimer protoChunk = (ChunkPrimer) chunk;
				ChunkBuffer chunkBuffer = new ForgeChunkBuffer(protoChunk);
				BitSet carvingMask = protoChunk.getOrCreateCarvingMask(stage);
				this.internalGenerator.carve(chunkBuffer, seed, protoChunk.getPos().x, protoChunk.getPos().z, carvingMask, cavesEnabled, ravinesEnabled);
			}
=======
	{
		if (stage == GenerationStage.Carving.AIR)
		{
			ChunkPrimer protoChunk = (ChunkPrimer) chunk;
			ChunkBuffer chunkBuffer = new ForgeChunkBuffer(protoChunk);
			BitSet carvingMask = protoChunk.getOrCreateCarvingMask(stage);
			this.internalGenerator.carve(chunkBuffer, seed, protoChunk.getPos().x, protoChunk.getPos().z, carvingMask, this.getCavesEnabled(), this.getRavinesEnabled());
>>>>>>> parent of 6ef79ba5a (Merge remote-tracking branch 'origin/1.16.4' into 1.16.4)
		}
		super.applyCarvers(seed, biomeManager, chunk, stage);
	}

<<<<<<< HEAD
	public void applyNonOTGCarvers(long seed, BiomeManager biomeManager, IChunk chunk, GenerationStage.Carving stage)
	{
		BiomeManager biomemanager = biomeManager.withDifferentSource(this.biomeSource);
		SharedSeedRandom sharedseedrandom = new SharedSeedRandom();
		ChunkPos chunkpos = chunk.getPos();
		int j = chunkpos.x;
		int k = chunkpos.z;
		ForgeBiome biome = (ForgeBiome)this.getCachedBiomeProvider().getNoiseBiome(chunk.getPos().x << 2, chunk.getPos().z << 2);
		BiomeGenerationSettings biomegenerationsettings = biome.getBiomeBase().getGenerationSettings();
		BitSet bitset = ((ChunkPrimer)chunk).getOrCreateCarvingMask(stage);

		List<String> defaultCavesAndRavines = Arrays.asList("minecraft:cave", "minecraft:underwater_cave", "minecraft:nether_cave", "minecraft:canyon", "minecraft:underwater_canyon");					
		for(int l = j - 8; l <= j + 8; ++l)
		{
			for(int i1 = k - 8; i1 <= k + 8; ++i1)
			{
				List<Supplier<ConfiguredCarver<?>>> list = biomegenerationsettings.getCarvers(stage);
				ListIterator<Supplier<ConfiguredCarver<?>>> listiterator = list.listIterator();
				while(listiterator.hasNext())
				{
					int j1 = listiterator.nextIndex();
					ConfiguredCarver<?> configuredcarver = listiterator.next().get();
					String carverRegistryName = ForgeRegistries.WORLD_CARVERS.getKey(configuredcarver.worldCarver).toString();
					// OTG uses its own caves and canyon carvers, ignore the default ones.
					if(defaultCavesAndRavines.stream().noneMatch(a -> a.equals(carverRegistryName)))
					{
						sharedseedrandom.setLargeFeatureSeed(seed + (long)j1, l, i1);
						if (configuredcarver.isStartChunk(sharedseedrandom, l, i1))
						{
							configuredcarver.carve(chunk, biomemanager::getBiome, sharedseedrandom, this.getSeaLevel(), l, i1, j, k, bitset);
						}
					}
				}
			}
		}
	}

=======
>>>>>>> parent of 6ef79ba5a (Merge remote-tracking branch 'origin/1.16.4' into 1.16.4)
	// Decoration

	// Does decoration for a given pos/chunk
	@Override
	@SuppressWarnings("deprecation")	
	public void applyBiomeDecoration(WorldGenRegion worldGenRegion, StructureManager structureManager)
	{
		if(!OTG.getEngine().getPluginConfig().getDecorationEnabled())
		{
			return;
		}
		
		// Do OTG resource decoration, then MC decoration for any non-OTG resources registered to this biome, then snow.
		
		// Taken from vanilla
		int worldX = worldGenRegion.getCenterX() * Constants.CHUNK_SIZE;
		int worldZ = worldGenRegion.getCenterZ() * Constants.CHUNK_SIZE;
		BlockPos blockpos = new BlockPos(worldX, 0, worldZ);
		SharedSeedRandom sharedseedrandom = new SharedSeedRandom();
		long decorationSeed = sharedseedrandom.setDecorationSeed(worldGenRegion.getSeed(), worldX, worldZ);	
		//

		ChunkCoordinate chunkBeingDecorated = ChunkCoordinate.fromBlockCoords(worldX, worldZ);
<<<<<<< HEAD
		ForgeWorldGenRegion forgeWorldGenRegion = new ForgeWorldGenRegion(this.preset.getFolderName(), this.preset.getWorldConfig(), worldGenRegion, this);
=======
		ForgeWorldGenRegion forgeWorldGenRegion = new ForgeWorldGenRegion(this.preset.getFolderName(), this.preset.getWorldConfig(), worldGenRegion, this);		
		IBiome biome = this.internalGenerator.getCachedBiomeProvider().getNoiseBiome((worldGenRegion.getCenterX() << 2) + 2, (worldGenRegion.getCenterZ() << 2) + 2);
>>>>>>> parent of 6ef79ba5a (Merge remote-tracking branch 'origin/1.16.4' into 1.16.4)
		// World save folder name may not be identical to level name, fetch it.
		Path worldSaveFolder = worldGenRegion.getLevel().getServer().getWorldPath(FolderName.PLAYER_DATA_DIR).getParent();
		try
		{
			// TODO: Snow is handled per chunk, so this may cause some artifacts on biome borders.
<<<<<<< HEAD
			if(
				!biome.getBiomeConfig().getIsTemplateForBiome() ||
				!biome1.getBiomeConfig().getIsTemplateForBiome() ||
				!biome2.getBiomeConfig().getIsTemplateForBiome() ||
				!biome3.getBiomeConfig().getIsTemplateForBiome() ||				
				!biome4.getBiomeConfig().getIsTemplateForBiome()
			)
=======
			if(!biome.getBiomeConfig().getTemplateForBiome())
>>>>>>> parent of 6ef79ba5a (Merge remote-tracking branch 'origin/1.16.4' into 1.16.4)
			{
				this.chunkDecorator.doSnowAndIce(forgeWorldGenRegion, chunkBeingDecorated);
			}
		} catch (Exception exception) {
			CrashReport crashreport = CrashReport.forThrowable(exception, "Biome decoration");
			crashreport.addCategory("Generation").setDetail("CenterX", worldX).setDetail("CenterZ", worldZ).setDetail("Seed", decorationSeed);
			throw new ReportedException(crashreport);
		}
	}

	// Noise

	@Override
	public int getBaseHeight(int x, int z, Type heightmapType)
	{
		return this.sampleHeightmap(x, z, null, heightmapType.isOpaque());
	}

	// Provides a sample of the full column for structure generation.
	@Override
	public IBlockReader getBaseColumn(int x, int z)
	{
		BlockState[] ablockstate = new BlockState[this.internalGenerator.getNoiseSizeY() * 8];
		this.sampleHeightmap(x, x, ablockstate, null);
		return new Blockreader(ablockstate);
	}

	// Samples the noise at a column and provides a view of the blockstates, or fills a heightmap.
	private int sampleHeightmap(int x, int z, @Nullable BlockState[] blockStates, @Nullable Predicate<BlockState> predicate)
	{
		// Get all of the coordinate starts and positions
		int xStart = Math.floorDiv(x, 4);
		int zStart = Math.floorDiv(z, 4);
		int xProgress = Math.floorMod(x, 4);
		int zProgress = Math.floorMod(z, 4);
		double xLerp = (double) xProgress / 4.0;
		double zLerp = (double) zProgress / 4.0;
		// Create the noise data in a 2 * 2 * 32 grid for interpolation.
		double[][] noiseData = new double[4][this.internalGenerator.getNoiseSizeY() + 1];

		// Initialize noise array.
		for (int i = 0; i < noiseData.length; i++)
		{
			noiseData[i] = new double[this.internalGenerator.getNoiseSizeY() + 1];
		}

		// Sample all 4 nearby columns.
		this.internalGenerator.getNoiseColumn(noiseData[0], xStart, zStart);
		this.internalGenerator.getNoiseColumn(noiseData[1], xStart, zStart + 1);
		this.internalGenerator.getNoiseColumn(noiseData[2], xStart + 1, zStart);
		this.internalGenerator.getNoiseColumn(noiseData[3], xStart + 1, zStart + 1);

		// [0, 32] -> noise chunks
		for (int noiseY = this.internalGenerator.getNoiseSizeY() - 1; noiseY >= 0; --noiseY)
		{
			// Gets all the noise in a 2x2x2 cube and interpolates it together.
			// Lower pieces
			double x0z0y0 = noiseData[0][noiseY];
			double x0z1y0 = noiseData[1][noiseY];
			double x1z0y0 = noiseData[2][noiseY];
			double x1z1y0 = noiseData[3][noiseY];
			// Upper pieces
			double x0z0y1 = noiseData[0][noiseY + 1];
			double x0z1y1 = noiseData[1][noiseY + 1];
			double x1z0y1 = noiseData[2][noiseY + 1];
			double x1z1y1 = noiseData[3][noiseY + 1];

			// [0, 8] -> noise pieces
			for (int pieceY = 7; pieceY >= 0; --pieceY)
			{
				double yLerp = (double) pieceY / 8.0;
				// Density at this position given the current y interpolation
				double density = MathHelper.lerp3(yLerp, xLerp, zLerp, x0z0y0, x0z0y1, x1z0y0, x1z0y1, x0z1y0, x0z1y1, x1z1y0, x1z1y1);

				// Get the real y position (translate noise chunk and noise piece)
				int y = (noiseY * 8) + pieceY;

				//BlockState state = this.getBlockState(density, y, biomeConfig);
				BlockState state = this.generateBaseState(density, y);
				if (blockStates != null)
				{
					blockStates[y] = state;
				}

				// return y if it fails the check
				if (predicate != null && predicate.test(state))
				{
					return y + 1;
				}
			}
		}

		return 0;
	}

	// Getters / misc

	@Override
	protected Codec<? extends ChunkGenerator> codec()
	{
		return CODEC;
	}

	public CustomStructureCache getStructureCache(Path worldSaveFolder)
	{
		if(this.structureCache == null)
		{
			this.structureCache = OTG.getEngine().createCustomStructureCache(this.preset.getFolderName(), worldSaveFolder, this.seed, this.preset.getWorldConfig().getCustomStructureType() == CustomStructureType.BO4);
		}
		return this.structureCache;
	}

	double getBiomeBlocksNoiseValue(int blockX, int blockZ)
	{
		return this.internalGenerator.getBiomeBlocksNoiseValue(blockX, blockZ);
	}

	// Shadowgen
	
	public void stopWorkerThreads()
	{
		this.shadowChunkGenerator.stopWorkerThreads();
	}

	public Boolean checkHasVanillaStructureWithoutLoading(ServerWorld world, ChunkCoordinate chunkCoord)
	{
		return this.shadowChunkGenerator.checkHasVanillaStructureWithoutLoading(world, this, (OTGBiomeProvider)this.biomeSource, this.getSettings(), chunkCoord, this.internalGenerator.getCachedBiomeProvider(), false);
	}

	public int getHighestBlockYInUnloadedChunk(Random worldRandom, int x, int z, boolean findSolid, boolean findLiquid, boolean ignoreLiquid, boolean ignoreSnow)
	{
		return this.shadowChunkGenerator.getHighestBlockYInUnloadedChunk(this.internalGenerator, this.preset.getWorldConfig().getWorldHeightCap(), worldRandom, x, z, findSolid, findLiquid, ignoreLiquid, ignoreSnow);
	}

	public LocalMaterialData getMaterialInUnloadedChunk(Random worldRandom, int x, int y, int z)
	{
		return this.shadowChunkGenerator.getMaterialInUnloadedChunk(this.internalGenerator, this.preset.getWorldConfig().getWorldHeightCap(), worldRandom, x, y, z);
	}

	public ForgeChunkBuffer getChunkWithoutLoadingOrCaching(Random random, ChunkCoordinate chunkCoord)
	{
		return this.shadowChunkGenerator.getChunkWithoutLoadingOrCaching(this.internalGenerator, this.preset.getWorldConfig().getWorldHeightCap(), random, chunkCoord);
	}
<<<<<<< HEAD
	
	// Modpack config
	// TODO: Move this?

	public String getPortalColor()
	{
		processDimensionConfigData();
		return this.portalColor;
	}

	public String getPortalMob()
	{
		processDimensionConfigData();
		return this.portalMob;
	}

	public String getPortalIgnitionSource()
	{
		processDimensionConfigData();
		return this.portalIgnitionSource;
	}
		
	public List<LocalMaterialData> getPortalBlocks()
	{
		processDimensionConfigData();
		return this.portalBlocks;
	}

	private void processDimensionConfigData()
	{
		if(!this.portalDataProcessed)
		{
			this.portalDataProcessed = true;
			if(this.dimConfig != null)
			{
				IMaterialReader materialReader = OTG.getEngine().getPresetLoader().getMaterialReader(this.preset.getFolderName());
				for(OTGDimension dim : this.dimConfig.Dimensions)
				{
					if(dim.PresetFolderName != null && this.preset.getFolderName().equals(dim.PresetFolderName))
					{
						if(dim.PortalBlocks != null && dim.PortalBlocks.trim().length() > 0)
						{
							String[] portalBlocks = dim.PortalBlocks.split(",");
							ArrayList<LocalMaterialData> materials = new ArrayList<LocalMaterialData>();					
							for(String materialString : portalBlocks)
							{
								LocalMaterialData material = null;
								try {
									material = materialReader.readMaterial(materialString.trim());
								} catch (InvalidConfigException e) { }
								if(material != null)
								{
									materials.add(material);
								}
							}
							this.portalBlocks = materials;
						}					
						this.portalColor = dim.PortalColor;
						this.portalMob = dim.PortalMob;
						this.portalIgnitionSource = dim.PortalIgnitionSource;
						break;
					}
				}
			}
			if(this.portalBlocks == null || this.portalBlocks.size() == 0)
			{
				this.portalBlocks = this.preset.getWorldConfig().getPortalBlocks(); 
			}
			if(this.portalColor == null)
			{
				this.portalColor = this.preset.getWorldConfig().getPortalColor();	
			}
			if(this.portalMob == null)
			{
				this.portalMob = this.preset.getWorldConfig().getPortalMob();
			}
			if(this.portalIgnitionSource == null)
			{
				this.portalIgnitionSource = this.preset.getWorldConfig().getPortalIgnitionSource();
			}
		}
	}
=======
>>>>>>> parent of 6ef79ba5a (Merge remote-tracking branch 'origin/1.16.4' into 1.16.4)
}
