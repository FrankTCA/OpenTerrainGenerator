package com.pg85.otg.forge.gen;

import java.nio.file.Path;
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
import com.pg85.otg.constants.Constants;
import com.pg85.otg.constants.SettingsEnums.CustomStructureType;
import com.pg85.otg.customobject.structures.CustomStructureCache;
import com.pg85.otg.forge.materials.ForgeMaterialData;
import com.pg85.otg.forge.presets.ForgePresetLoader;
import com.pg85.otg.forge.biome.ForgeBiome;
import com.pg85.otg.forge.biome.OTGBiomeProvider;
import com.pg85.otg.gen.OTGChunkGenerator;
import com.pg85.otg.gen.OTGChunkDecorator;
import com.pg85.otg.interfaces.IBiome;
import com.pg85.otg.interfaces.ICachedBiomeProvider;
import com.pg85.otg.interfaces.ILayerSource;
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
import net.minecraft.world.biome.BiomeManager;
import net.minecraft.world.biome.provider.BiomeProvider;
import net.minecraft.world.chunk.ChunkPrimer;
import net.minecraft.world.chunk.IChunk;
import net.minecraft.world.gen.ChunkGenerator;
import net.minecraft.world.gen.DimensionSettings;
import net.minecraft.world.gen.GenerationStage;
import net.minecraft.world.gen.Heightmap;
import net.minecraft.world.gen.Heightmap.Type;
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
	private CustomStructureCache structureCache; // TODO: Move this?
	
	public OTGNoiseChunkGenerator(BiomeProvider biomeProvider, long seed, Supplier<DimensionSettings> dimensionSettingsSupplier)
	{
		this(OTG.getEngine().getPresetLoader().getDefaultPresetFolderName(), biomeProvider, biomeProvider, seed, dimensionSettingsSupplier);
	}

	public OTGNoiseChunkGenerator(String presetFolderName, BiomeProvider biomeProvider, long seed, Supplier<DimensionSettings> dimensionSettingsSupplier)
	{
		this(presetFolderName, biomeProvider, biomeProvider, seed, dimensionSettingsSupplier);
	}
	
	// TODO: Why are there 2 biome providers, and why does getBiomeProvider() return the second, while we're using the first?
	// It looks like vanilla just inserts the same biomeprovider twice?
		this.shadowChunkGenerator = new ShadowChunkGenerator(OTG.getEngine().getPluginConfig().getMaxWorkerThreads());
		this.internalGenerator = new OTGChunkGenerator(this.preset, seed, (ILayerSource) biomeProvider1,((ForgePresetLoader)OTG.getEngine().getPresetLoader()).getGlobalIdMapping(presetFolderName), OTG.getEngine().getLogger());
		this.chunkDecorator = new OTGChunkDecorator();
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

	@OnlyIn(Dist.CLIENT)
	@Override
	public ChunkGenerator withSeed(long seed)
	{
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
	}

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

		ChunkCoordinate chunkBeingDecorated = ChunkCoordinate.fromBlockCoords(worldX, worldZ);
		// World save folder name may not be identical to level name, fetch it.
		Path worldSaveFolder = worldGenRegion.getLevel().getServer().getWorldPath(FolderName.PLAYER_DATA_DIR).getParent();
		try
		{
			// TODO: Snow is handled per chunk, so this may cause some artifacts on biome borders.
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
}
