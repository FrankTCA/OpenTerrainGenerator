package com.pg85.otg.gen.resource;

import com.pg85.otg.constants.Constants;
import com.pg85.otg.exception.InvalidConfigException;
import com.pg85.otg.gen.resource.util.PlantType;
import com.pg85.otg.logging.ILogger;
import com.pg85.otg.util.helpers.RandomHelper;
import com.pg85.otg.util.interfaces.IBiomeConfig;
import com.pg85.otg.util.interfaces.IMaterialReader;
import com.pg85.otg.util.interfaces.IWorldGenRegion;
import com.pg85.otg.util.materials.LocalMaterialData;
import com.pg85.otg.util.materials.MaterialSet;

import java.util.List;
import java.util.Random;

public class PlantResource extends FrequencyResourceBase
{
	private final int maxAltitude;
	private final int minAltitude;
	private final PlantType plant;
	private final MaterialSet sourceBlocks;

	public PlantResource(IBiomeConfig biomeConfig, List<String> args, ILogger logger, IMaterialReader materialReader) throws InvalidConfigException
	{
		super(biomeConfig, args, logger, materialReader);
		assureSize(6, args);

		this.plant = PlantType.getPlant(args.get(0), materialReader);
		this.frequency = readInt(args.get(1), 1, 100);
		this.rarity = readRarity(args.get(2));
		this.minAltitude = readInt(args.get(3), Constants.WORLD_DEPTH, Constants.WORLD_HEIGHT - 1);
		this.maxAltitude = readInt(args.get(4), this.minAltitude, Constants.WORLD_HEIGHT - 1);
		this.sourceBlocks = readMaterials(args, 5, materialReader);
	}

	@Override
	public void spawn(IWorldGenRegion worldGenregion, Random rand, boolean villageInChunk, int x, int z)
	{
		int y = RandomHelper.numberInRange(rand, this.minAltitude, this.maxAltitude);

		LocalMaterialData worldMaterial;
		LocalMaterialData worldMaterialBelow;
		
		int localX;
		int localY;
		int localZ;
		for (int i = 0; i < 64; i++)
		{
			localX = x + rand.nextInt(8) - rand.nextInt(8);
			localY = y + rand.nextInt(4) - rand.nextInt(4);
			localZ = z + rand.nextInt(8) - rand.nextInt(8);
			worldMaterial = worldGenregion.getMaterial(localX, localY, localZ);
			worldMaterialBelow = worldGenregion.getMaterial(localX, localY - 1, localZ);
			if (
				(worldMaterial == null || !worldMaterial.isAir()) ||
				(worldMaterialBelow == null || !this.sourceBlocks.contains(worldMaterialBelow))
			)
			{
				continue;
			}

			this.plant.spawn(worldGenregion, localX, localY, localZ);
		}
	}
	
	@Override
	public String toString()
	{
		return "Plant(" + this.plant.getName() + "," + this.frequency + "," + this.rarity + "," + this.minAltitude + "," + this.maxAltitude + makeMaterials(this.sourceBlocks) + ")";
	}	
}
