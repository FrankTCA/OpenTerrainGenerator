package com.pg85.otg.spigot.gen;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.Supplier;

import org.bukkit.craftbukkit.v1_16_R3.generator.CraftChunkData;

import com.pg85.otg.constants.Constants;
import com.pg85.otg.gen.OTGChunkGenerator;
import com.pg85.otg.interfaces.IBiome;
import com.pg85.otg.interfaces.ICachedBiomeProvider;
import com.pg85.otg.spigot.biome.SpigotBiome;
import com.pg85.otg.spigot.materials.SpigotMaterialData;
import com.pg85.otg.util.BlockPos2D;
import com.pg85.otg.util.ChunkCoordinate;
import com.pg85.otg.util.FifoMap;
import com.pg85.otg.util.gen.JigsawStructureData;
import com.pg85.otg.util.materials.LocalMaterialData;
import com.pg85.otg.util.materials.LocalMaterials;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import net.minecraft.server.v1_16_R3.BiomeBase;
import net.minecraft.server.v1_16_R3.BlockPosition;
import net.minecraft.server.v1_16_R3.ChunkCoordIntPair;
import net.minecraft.server.v1_16_R3.ChunkGenerator;
import net.minecraft.server.v1_16_R3.DefinedStructureManager;
import net.minecraft.server.v1_16_R3.HeightMap;
import net.minecraft.server.v1_16_R3.IBlockData;
import net.minecraft.server.v1_16_R3.IChunkAccess;
import net.minecraft.server.v1_16_R3.IRegistryCustom;
import net.minecraft.server.v1_16_R3.ProtoChunk;
import net.minecraft.server.v1_16_R3.SeededRandom;
import net.minecraft.server.v1_16_R3.StructureFeature;
import net.minecraft.server.v1_16_R3.StructureGenerator;
import net.minecraft.server.v1_16_R3.StructureManager;
import net.minecraft.server.v1_16_R3.StructureSettings;
import net.minecraft.server.v1_16_R3.StructureSettingsFeature;
import net.minecraft.server.v1_16_R3.WorldChunkManager;
import net.minecraft.server.v1_16_R3.WorldGenStage.Decoration;
import net.minecraft.server.v1_16_R3.WorldServer;

/**
 * Shadow chunk generation means generating base terrain for chunks
 * without using mc's world generation flow. OTG's chunkgenerator is
 * called internally to generate base terrain for dummy chunks. 
 * Shadowgenned chunks are stored in a fixed size FIFO cache, data is 
 * reused when base terraingen is requested for those chunks via 
 * normal worldgen. Shadowgen is used for BO4's and /otg mapterrain.
 *
 * Shadowgen can only be done for chunks that don't contain vanilla structures,
 * since those structures may use density based smoothing applied during noisegen,
 * which unfortunately is done in a non-thread-safe/blocking manner, necessitating
 * the use of a WorldGenRegion.
 */
public class ShadowChunkGenerator
{
	// TODO: Add a setting to the worldconfig for the size of these caches?
	private final FifoMap<BlockPos2D, LocalMaterialData[]> unloadedBlockColumnsCache = new FifoMap<BlockPos2D, LocalMaterialData[]>(1024);
	private final FifoMap<ChunkCoordinate, IChunkAccess> unloadedChunksCache = new FifoMap<ChunkCoordinate, IChunkAccess>(512);
	private final FifoMap<ChunkCoordinate, Integer> hasVanillaStructureChunkCache = new FifoMap<ChunkCoordinate, Integer>(2048);
	
	static Field heightMaps;
	static Field light;
	static Field sections;
	
	static
	{
		try
		{
			heightMaps = ProtoChunk.class.getDeclaredField("f");
			heightMaps.setAccessible(true);

			light = ProtoChunk.class.getDeclaredField("l");
			light.setAccessible(true);

			sections = ProtoChunk.class.getDeclaredField("j");
			sections.setAccessible(true);
		} catch (ReflectiveOperationException ex)
		{
			ex.printStackTrace();
		}
	}

	@SuppressWarnings("unused")
	private int cacheHits = 0;
	@SuppressWarnings("unused")
	private int cacheMisses = 0;

	public ShadowChunkGenerator() { }

	private SpigotChunkBuffer getUnloadedChunk(OTGChunkGenerator otgChunkGenerator, int worldHeightCap, Random random, ChunkCoordinate chunkCoordinate)
	{
		ProtoChunk chunk = new ProtoChunk(new ChunkCoordIntPair(chunkCoordinate.getChunkX(), chunkCoordinate.getChunkZ()), null);
		SpigotChunkBuffer buffer = new SpigotChunkBuffer(chunk);

		// This is where vanilla processes any noise affecting structures like villages, in order to spawn smoothing areas.
		// Doing this for unloaded chunks causes a hang on load since getChunk is called by StructureManager.
		// BO4's/shadowgen avoid villages, so this method should never be called to fetch unloaded chunks that contain villages,
		// so we can skip noisegen affecting structures here.
		// *TODO: Do we need to avoid any noisegen affecting structures other than villages?

		ObjectList<JigsawStructureData> structures = new ObjectArrayList<>(10);
		ObjectList<JigsawStructureData> junctions = new ObjectArrayList<>(32);

		otgChunkGenerator.populateNoise(worldHeightCap, random, buffer, buffer.getChunkCoordinate(), structures, junctions);
		return buffer;
	}

	public IChunkAccess getChunkFromCache(ChunkCoordinate chunkCoord)
	{
		IChunkAccess cachedChunk = this.unloadedChunksCache.get(chunkCoord);
		if(cachedChunk != null)
		{
			return cachedChunk;
		} else {
			return null;
		}
	}

	public void fillWorldGenChunkFromShadowChunk(ChunkCoordinate chunkCoord, org.bukkit.generator.ChunkGenerator.ChunkData chunk, IChunkAccess cachedChunk)
	{
		// Re-use base terrain generated via shadowgen for worldgen.	
		// TODO: Find some way to clone/swap chunk data efficiently, 
		// like we do for forge with accesstransformers.
		/*
		((ProtoChunk)chunk).sections = ((ProtoChunk)cachedChunk).sections;
		((ProtoChunk)chunk).heightmaps = ((ProtoChunk)cachedChunk).heightmaps;
		((ProtoChunk)chunk).lights = ((ProtoChunk)cachedChunk).lights;
		*/
		CraftChunkData data = (CraftChunkData) chunk;
		for (int x = 0; x < Constants.CHUNK_SIZE; x++)
		{
			for (int z = 0; z < Constants.CHUNK_SIZE; z++)
			{
				int endY = cachedChunk.a(HeightMap.Type.WORLD_SURFACE_WG).a(x, z);
				for (int y = 0; y <= endY; y++)
				{
					BlockPosition pos = new BlockPosition(x, y, z);
					data.setRegion(x, y, z, x + 1, y + 1, z + 1, cachedChunk.getType(pos));
				}
			}
		}
		
		this.cacheHits++;
		//OTG.log(LogMarker.INFO, "Cache hit " + this.cacheHits);
		this.unloadedChunksCache.remove(chunkCoord);
	}
	
	public void fillWorldGenChunkFromShadowChunk(ChunkCoordinate chunkCoord, IChunkAccess chunk, IChunkAccess cachedChunk)
	{
		// TODO: This is experimental and may be slower than not cloning it
		try
		{
			sections.set((ProtoChunk) chunk, sections.get((ProtoChunk) cachedChunk));
			light.set((ProtoChunk) chunk, light.get((ProtoChunk) cachedChunk));
			heightMaps.set((ProtoChunk) chunk, heightMaps.get((ProtoChunk) cachedChunk));
		} catch (ReflectiveOperationException e)
		{
			e.printStackTrace();
		}
		
		
		for (int x = 0; x < Constants.CHUNK_SIZE; x++)
		{
			for (int z = 0; z < Constants.CHUNK_SIZE; z++)
			{
				int endY = cachedChunk.a(HeightMap.Type.WORLD_SURFACE_WG).a(x, z);
				for (int y = 0; y <= endY; y++)
				{
					BlockPosition pos = new BlockPosition(x, y, z);
					chunk.setType(pos, cachedChunk.getType(pos), false);
				}
			}
		}
		
		this.cacheHits++;
		//OTG.log(LogMarker.INFO, "Cache hit " + this.cacheHits);
		this.unloadedChunksCache.remove(chunkCoord);
	}	

	public void setChunkGenerated(ChunkCoordinate chunkCoord)
	{
		this.cacheMisses++;
		//OTG.log(LogMarker.INFO, "Cache miss " + + this.cacheMisses);
	}

	// Vanilla structure detection (avoidance)
	// Some vanilla structures use density based smoothing of terrain underneath, which is factored into noisegen.
	// Unfortunately this requires fetching structure data in a non-thread-safe manner, so we can't do async
	// chunkgen (base terrain) for these chunks and have to avoid them.

	public boolean checkHasVanillaStructureWithoutLoading(WorldServer serverWorld, ChunkGenerator chunkGenerator, WorldChunkManager biomeProvider, StructureSettings dimensionStructuresSettings, ChunkCoordinate chunkCoordinate, ICachedBiomeProvider cachedBiomeProvider)
	{
		// Since we can't check for structure components/references, only structure starts,
		// we'll keep a safe distance away from any vanilla structure start points.
		int radiusInChunks = 5;
		ProtoChunk chunk;
		ChunkCoordIntPair chunkpos;
		IBiome biome;
		if (serverWorld.getServer().getGenerateStructures())
		{
			List<ChunkCoordinate> chunksToHandle = new ArrayList<>();
			Map<ChunkCoordinate,Integer> chunksHandled = new HashMap<>();
			synchronized(this.hasVanillaStructureChunkCache)
			{
				if(checkHasVanillaStructureWithoutLoadingCache(this.hasVanillaStructureChunkCache, chunkCoordinate, radiusInChunks, chunksToHandle))
				{
					return true;
				}
			}
			
			@SuppressWarnings("unchecked")
			ArrayList<StructureGenerator<?>>[] structuresPerDistance = new ArrayList[radiusInChunks];
			structuresPerDistance[4] = new ArrayList<StructureGenerator<?>>(Arrays.asList(
				new StructureGenerator<?>[] {
					StructureGenerator.VILLAGE,
					StructureGenerator.ENDCITY,
					StructureGenerator.BASTION_REMNANT,
					StructureGenerator.MONUMENT,
					StructureGenerator.MANSION
				}
			));
			structuresPerDistance[3] = new ArrayList<StructureGenerator<?>>(Arrays.asList(new StructureGenerator<?>[]{}));
			structuresPerDistance[2] = new ArrayList<StructureGenerator<?>>(Arrays.asList(new StructureGenerator<?>[]{}));
			structuresPerDistance[1] = new ArrayList<StructureGenerator<?>>(Arrays.asList(
				new StructureGenerator<?>[] {
					StructureGenerator.JUNGLE_PYRAMID,
					StructureGenerator.DESERT_PYRAMID,
					StructureGenerator.RUINED_PORTAL,
					StructureGenerator.SWAMP_HUT,
					StructureGenerator.IGLOO,
					StructureGenerator.SHIPWRECK,
					StructureGenerator.PILLAGER_OUTPOST,
					StructureGenerator.OCEAN_RUIN
				}
			));
			structuresPerDistance[0] = new ArrayList<StructureGenerator<?>>(Arrays.asList(new StructureGenerator<?>[]{}));
		
			for(ChunkCoordinate chunkToHandle : chunksToHandle)
			{
				chunk = new ProtoChunk(new ChunkCoordIntPair(chunkToHandle.getChunkX(), chunkToHandle.getChunkZ()), null);
				chunkpos = chunk.getPos();
				int distance = (int)Math.floor(Math.sqrt(Math.pow (chunkToHandle.getChunkX() - chunkCoordinate.getChunkX(), 2) + Math.pow (chunkToHandle.getChunkZ() - chunkCoordinate.getChunkZ(), 2)));
				
				// Borrowed from STRUCTURE_STARTS phase of chunkgen, only determines structure start point
				// based on biome and resource settings (distance etc). Does not plot any structure components.

				// TODO: Optimise this for biome lookups, fetch a whole region of noise biome info at once?
				biome = cachedBiomeProvider.getNoiseBiome((chunkpos.x << 2) + 2, (chunkpos.z << 2) + 2);
				for(Supplier<StructureFeature<?, ?>> supplier : ((SpigotBiome)biome).getBiomeBase().e().a())
				{
					StructureFeature<?, ?> structure = supplier.get();
					if(structure.d.f() == Decoration.SURFACE_STRUCTURES)
					{
						for(int i = structuresPerDistance.length - 1; i > 0; i--)
						{
							ArrayList<StructureGenerator<?>> structuresAtDistance = structuresPerDistance[i];
							if(structuresAtDistance.contains(structure.d))
							{
								if(hasStructureStart(structure, dimensionStructuresSettings, serverWorld.r(), serverWorld.getStructureManager(), chunk, serverWorld.n(), chunkGenerator, biomeProvider, serverWorld.getSeed(), chunkpos, ((SpigotBiome)biome).getBiomeBase()))
								{
									chunksHandled.put(chunkToHandle, new Integer(i));
									if(i >= distance)
									{
										synchronized(this.hasVanillaStructureChunkCache)
										{
											this.hasVanillaStructureChunkCache.putAll(chunksHandled);
										}							
										return true;
									}
								}
								break;
							}
						}
					}
				}
				chunksHandled.putIfAbsent(chunkToHandle, new Integer(0));
			}
			synchronized(this.hasVanillaStructureChunkCache)
			{
				this.hasVanillaStructureChunkCache.putAll(chunksHandled);
			}
		}
		return false;
	}
	
	private boolean checkHasVanillaStructureWithoutLoadingCache(FifoMap<ChunkCoordinate, Integer> cache, ChunkCoordinate chunkCoordinate, int radiusInChunks, List<ChunkCoordinate> chunksToHandle)
	{
		for (int cycle = 0; cycle < radiusInChunks; ++cycle)
		{
			for (int xOffset = -cycle; xOffset <= cycle; ++xOffset)
			{
				for (int zOffset = -cycle; zOffset <= cycle; ++zOffset)
				{
					int distance = (int)Math.floor(Math.sqrt(Math.pow (xOffset, 2) + Math.pow (zOffset, 2)));
					if (distance == cycle)
					{
						ChunkCoordinate searchChunk = ChunkCoordinate.fromChunkCoords(chunkCoordinate.getChunkX() + xOffset, chunkCoordinate.getChunkZ() + zOffset);
						Integer result = cache.get(searchChunk);
						if(result != null)
						{
							if(result.intValue() > 0 && result.intValue() >= distance)
							{
								return true;
							}
						} else {
							chunksToHandle.add(searchChunk);
						}
					}
				}
			}
		}
		return false;
	}

	// Taken from PillagerOutpostStructure.isNearVillage
	private static boolean hasStructureStart(StructureFeature<?, ?> structureFeature, StructureSettings dimensionStructuresSettings, IRegistryCustom dynamicRegistries, StructureManager structureManager, IChunkAccess chunk, DefinedStructureManager templateManager, ChunkGenerator chunkGenerator, WorldChunkManager biomeProvider, long seed, ChunkCoordIntPair chunkPos, BiomeBase biome)
	{
		StructureSettingsFeature structureSeparationSettings = dimensionStructuresSettings.a(structureFeature.d);
		if (structureSeparationSettings != null)
		{
			SeededRandom sharedSeedRandom = new SeededRandom();
			ChunkCoordIntPair chunkPosPotential = structureFeature.d.a(structureSeparationSettings, seed, sharedSeedRandom, chunkPos.x, chunkPos.z);
			if (
				chunkPos.x == chunkPosPotential.x &&
				chunkPos.z == chunkPosPotential.z
			) {
				return true;
			}
			return false;
		}
		return false;
	}

	// /otg mapterrain

	// /otg mapterrain fetches chunks in order to create a map of base terrain, without touching any of the caches or
	// resources used for worldgen or bo4 shadowgen, since the chunks aren't actually supposed to generate in the world.
	// We won't get any density based smoothing applied to noisegen for vanilla structures, but that's ok for /otg mapterrain.

	public SpigotChunkBuffer getChunkWithoutLoadingOrCaching(OTGChunkGenerator otgChunkGenerator, int worldHeightCap, Random random, ChunkCoordinate chunkCoordinate)
	{
		return getUnloadedChunk(otgChunkGenerator, worldHeightCap, random, chunkCoordinate);
	}

	// BO4's / Smoothing Areas

	// BO4's and smoothing areas may do material and height checks in unloaded chunks during decoration.
	// Shadowgen is used to do this without causing cascades. Shadowgenned chunks are requested on-demand for the worldgen thread (BO4's).

	private LocalMaterialData[] getBlockColumnInUnloadedChunk(OTGChunkGenerator otgChunkGenerator, int worldHeightCap, Random worldRandom, int x, int z)
	{
		BlockPos2D blockPos = new BlockPos2D(x, z);
		ChunkCoordinate chunkCoord = ChunkCoordinate.fromBlockCoords(x, z);

		// Get internal coordinates for block in chunk
		byte blockX = (byte) (x &= 0xF);
		byte blockZ = (byte) (z &= 0xF);

		LocalMaterialData[] cachedColumn = this.unloadedBlockColumnsCache.get(blockPos);

		if (cachedColumn != null)
		{
			return cachedColumn;
		}

		IChunkAccess chunk = getChunkFromCache(chunkCoord);
		if (chunk == null)
		{
			// Generate a chunk without loading/decorating it
			chunk = getUnloadedChunk(otgChunkGenerator, worldHeightCap, worldRandom, chunkCoord).getChunk();
			this.unloadedChunksCache.put(chunkCoord, chunk);
		}

		cachedColumn = new LocalMaterialData[256];

		LocalMaterialData[] blocksInColumn = new LocalMaterialData[256];
		IBlockData blockInChunk;
		for (short y = 0; y < 256; y++)
		{
			blockInChunk = chunk.getType(new BlockPosition(blockX, y, blockZ));
			if (blockInChunk != null)
			{
				blocksInColumn[y] = SpigotMaterialData.ofBlockData(blockInChunk);
			} else {
				break;
			}
		}
		this.unloadedBlockColumnsCache.put(blockPos, cachedColumn);

		return blocksInColumn;
	}

	public LocalMaterialData getMaterialInUnloadedChunk(OTGChunkGenerator otgChunkGenerator, int worldHeightCap, Random worldRandom, int x, int y, int z)
	{
		LocalMaterialData[] blockColumn = getBlockColumnInUnloadedChunk(otgChunkGenerator, worldHeightCap, worldRandom, x, z);
		return blockColumn[y];
	}

	public int getHighestBlockYInUnloadedChunk(OTGChunkGenerator otgChunkGenerator, int worldHeightCap, Random worldRandom, int x, int z, boolean findSolid, boolean findLiquid, boolean ignoreLiquid, boolean ignoreSnow)
	{
		int height = -1;

		LocalMaterialData[] blockColumn = getBlockColumnInUnloadedChunk(otgChunkGenerator, worldHeightCap, worldRandom, x, z);
		SpigotMaterialData material;
		boolean isLiquid;
		boolean isSolid;

		for (int y = 255; y >= 0; y--)
		{
			material = (SpigotMaterialData) blockColumn[y];
			isLiquid = material.isLiquid();
			isSolid = material.isSolid() || (!ignoreSnow && material.isMaterial(LocalMaterials.SNOW));
			if (!(isLiquid && ignoreLiquid))
			{
				if ((findSolid && isSolid) || (findLiquid && isLiquid))
				{
					return y;
				}
				if ((findSolid && isLiquid) || (findLiquid && isSolid))
				{
					return -1;
				}
			}
		}
		return height;
	}
}
