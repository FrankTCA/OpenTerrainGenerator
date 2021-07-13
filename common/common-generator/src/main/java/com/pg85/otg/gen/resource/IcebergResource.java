package com.pg85.otg.gen.resource;

import java.util.List;
import java.util.Random;

import com.pg85.otg.config.biome.BiomeResourceBase;
import com.pg85.otg.exceptions.InvalidConfigException;
import com.pg85.otg.interfaces.IBiomeConfig;
import com.pg85.otg.interfaces.ILogger;
import com.pg85.otg.interfaces.IMaterialReader;
import com.pg85.otg.interfaces.IWorldGenRegion;
import com.pg85.otg.util.helpers.MathHelper;
import com.pg85.otg.util.materials.LocalMaterialData;
import com.pg85.otg.util.materials.LocalMaterials;

public class IcebergResource extends BiomeResourceBase implements IBasicResource
{
	private final LocalMaterialData material;
	private final double rarity;

	public IcebergResource(IBiomeConfig biomeConfig, List<String> args, ILogger logger, IMaterialReader materialReader) throws InvalidConfigException
	{
		super(biomeConfig, args, logger, materialReader);
		assureSize(2, args);
		
		this.material = materialReader.readMaterial(args.get(0));
		this.rarity = readRarity(args.get(1));
	}

	@Override
	public void spawnForChunkDecoration(IWorldGenRegion world, Random random, ILogger logger, IMaterialReader materialReader)
	{
		if (random.nextDouble() * 100.0 > this.rarity)
		{
			return;
		}
		
		int x = world.getDecorationArea().getChunkBeingDecorated().getBlockX();
		int z = world.getDecorationArea().getChunkBeingDecorated().getBlockZ();
		int y = world.getBiomeConfigForDecoration(x, z).getWaterLevelMax();
		boolean flag1 = random.nextDouble() > 0.7D;
		LocalMaterialData iceBergMaterial = this.material;
		double drandom1 = random.nextDouble() * 2.0D * Math.PI;
		int irandom1 = 11 - random.nextInt(5);
		int irandom2 = 3 + random.nextInt(3);
		boolean flag2 = random.nextDouble() > 0.7D;

		int ismoothheight = flag2 ? random.nextInt(6) + 6 : random.nextInt(15) + 3;
		if (!flag2 && random.nextDouble() > 0.9D)
		{
			ismoothheight += random.nextInt(19) + 7;
		}

		int irandom4 = Math.min(ismoothheight + random.nextInt(11), 18);
		int irandom5 = Math.min(ismoothheight + random.nextInt(7) - random.nextInt(5), 11);
		int irandom6 = flag2 ? irandom1 : 11;

		for (int x1 = -irandom6; x1 < irandom6; x1++)
		{
			for (int z1 = -irandom6; z1 < irandom6; z1++)
			{
				for (int y1 = 0; y1 < ismoothheight; y1++)
				{
					int heightDependentRadiusEllipse = flag2 ? heightDependentRadiusEllipse(y1, ismoothheight, irandom5) : heightDependentRadiusRound(random, y1, ismoothheight, irandom5);
					if (flag2 || x1 < heightDependentRadiusEllipse)
					{
						generateIcebergBlock(world, random, x, y, z, ismoothheight, x1, y1, z1, heightDependentRadiusEllipse, irandom6, flag2, irandom2, drandom1, flag1, iceBergMaterial);
					}
				}
			}
		}

<<<<<<< HEAD
		for (int xOffset = -lvt_17_1_; xOffset < lvt_17_1_; xOffset++)
		{
			for (int zOffset = -lvt_17_1_; zOffset < lvt_17_1_; zOffset++)
			{
				for (int yOffset = -1; yOffset > -lvt_15_1_; yOffset--)
				{
					int lvt_21_2_ = lvt_12_1_
							? MathHelper.ceil(lvt_17_1_ * (1.0F - (float) Math.pow(yOffset, 2.0D) / lvt_15_1_ * 8.0F))
							: lvt_17_1_;
					int lvt_22_1_ = heightDependentRadiusSteep(random, -yOffset, lvt_15_1_, lvt_16_1_);
					if (xOffset < lvt_22_1_)
					{
						generateIcebergBlock(world, random, x, y, z, lvt_15_1_, xOffset, yOffset, zOffset,
								lvt_22_1_, lvt_21_2_, lvt_12_1_, lvt_11_1_, lvt_8_1_, lvt_6_1_, iceBergMaterial);
=======
		smooth(world, x, y, z, irandom5, ismoothheight, flag2, irandom1);

		for (int x1 = -irandom6; x1 < irandom6; x1++)
		{
			for (int z1 = -irandom6; z1 < irandom6; z1++)
			{
				for (int y1 = -1; y1 > -irandom4; y1--)
				{
					int irandom7 = flag2 ? MathHelper.ceil((float)irandom6 * (1.0F - (float) Math.pow((double)y1, 2.0D) / ((float)irandom4 * 8.0F))) : irandom6;
					int heightDependentRadiusSteep = heightDependentRadiusSteep(random, -y1, irandom4, irandom5);
					if (x1 < heightDependentRadiusSteep)
					{
						generateIcebergBlock(world, random, x, y, z, irandom4, x1, y1, z1, heightDependentRadiusSteep, irandom7, flag2, irandom2, drandom1, flag1, iceBergMaterial);
>>>>>>> daf775313c7c12b81ed6606aab1c2d888e46ae1e
					}
				}
			}
		}
		boolean flag3 = flag2 ? random.nextDouble() > 0.1D : random.nextDouble() > 0.7D;
		if (flag3)
		{
			generateCutOut(random, world, irandom5, ismoothheight, x, y, z, flag2, irandom1, drandom1, irandom2);
		}
	}

	private void generateCutOut(Random random, IWorldGenRegion world, int irandom, int irandom1, int x, int y, int z, boolean flag, int irandom2, double drandom1, int irandom3)
	{
		int irandom4 = random.nextBoolean() ? -1 : 1;
		int irandom5 = random.nextBoolean() ? -1 : 1;
		int irandom6 = random.nextInt(Math.max(irandom / 2 - 2, 1));
		if (random.nextBoolean())
		{
			irandom6 = irandom / 2 + 1 - random.nextInt(Math.max(irandom - irandom / 2 - 1, 1));
		}

		int irandom7 = random.nextInt(Math.max(irandom / 2 - 2, 1));
		if (random.nextBoolean())
		{
			irandom7 = irandom / 2 + 1 - random.nextInt(Math.max(irandom - irandom / 2 - 1, 1));
		}

		if (flag)
		{
			irandom6 = irandom7 = random.nextInt(Math.max(irandom2 - 5, 1));
		}

		int x2 = irandom4 * irandom6;
		int z2 = irandom5 * irandom7;
		double drandom2 = flag ? drandom1 + (Math.PI / 2D) : random.nextDouble() * 2.0D * Math.PI;
		//double drandom2 = flag ? (drandom1 + 1.5707963267948966D) : (random.nextDouble() * 2.0D * Math.PI);

		for (int irandom8 = 0; irandom8 < irandom1 - 3; ++irandom8)
		{
			int heightDependentRadiusRound = heightDependentRadiusRound(random, irandom8, irandom1, irandom);
			carve(heightDependentRadiusRound, irandom8, x, y, z, world, false, drandom2, x2, z2, irandom2, irandom3);
		}
		for (int irandom8 = -1; irandom8 > -irandom1 + random.nextInt(5); --irandom8)
		{
			int heightDependentRadiusSteep = heightDependentRadiusSteep(random, -irandom8, irandom1, irandom);
			carve(heightDependentRadiusSteep, irandom8, x, y, z, world, true, drandom2, x2, z2, irandom2, irandom3);
		}
	}

	private void carve(int heightDependentRadius, int irandom, int x, int y, int z, IWorldGenRegion world, boolean flag, double drandom, int x3, int z3, int irandom1, int irandom2)
	{
		int irandom3 = heightDependentRadius + 1 + irandom1 / 3;
		int irandom4 = Math.min(heightDependentRadius - 3, 3) + irandom2 / 2 - 1;
		int x2;
		int y2;
		int z2;
		for (int x1 = -irandom3; x1 < irandom3; x1++)
		{
			for (int z1 = -irandom3; z1 < irandom3; z1++)
			{
				double signedDistanceEllipse = signedDistanceEllipse(x1, z1, x3, 0, z3, irandom3, irandom4, drandom);
				if (signedDistanceEllipse < 0.0D)
				{
					x2 = x + x1;
					y2 = y + irandom;
					z2 = z + z1;
					LocalMaterialData material = world.getMaterialDirect(x2, y2, z2);
					if (isIcebergBlock(material) || material.isMaterial(LocalMaterials.SNOW_BLOCK))
					{
						if (flag)
						{
							world.setBlockDirect(x2, y2, z2, LocalMaterials.WATER);
						} else {
							world.setBlockDirect(x2, y2, z2, LocalMaterials.AIR);
							removeFloatingSnowLayer(world, x2, y2, z2);
						}
					}
				}
			}
		}
	}

	private void removeFloatingSnowLayer(IWorldGenRegion world, int x, int y, int z)
	{
		LocalMaterialData materialAbove = world.getMaterialDirect(x, y + 1, z);	
		if (materialAbove.isMaterial(LocalMaterials.SNOW))
		{
			world.setBlockDirect(x, y + 1, z, LocalMaterials.AIR);
		}
	}

<<<<<<< HEAD
	private void generateIcebergBlock(
		IWorldGenRegion world, Random random, int x, int y, int z, int p_205181_4_, 
		int p_205181_5_, int p_205181_6_, int p_205181_7_, int p_205181_8_, int p_205181_9_, 
		boolean p_205181_10_, int p_205181_11_, double p_205181_12_, boolean p_205181_14_, 
		LocalMaterialData material
	)
	{
		double lvt_16_1_ = p_205181_10_
			? signedDistanceEllipse(p_205181_5_, p_205181_7_, 0, 0, p_205181_9_, getEllipseC(p_205181_6_, p_205181_4_, p_205181_11_), p_205181_12_)
			: signedDistanceCircle(p_205181_5_, p_205181_7_, 0, 0, p_205181_8_, random);
=======
	private void generateIcebergBlock(IWorldGenRegion worldGenRegion, Random random, int x, int y, int z, int irandom, int x1, int y1, int z1, int irandom1, int irandom2, boolean flag, int irandom3, double drandom, boolean flag1, LocalMaterialData material)
	{
		double signedDistance = flag
			? signedDistanceEllipse(x1, z1, 0, 0, 0, irandom2, getEllipseC(y1, irandom, irandom3), drandom)
			: signedDistanceCircle(x1, z1, 0, 0, 0, irandom1, random);
>>>>>>> daf775313c7c12b81ed6606aab1c2d888e46ae1e

		if (signedDistance < 0.0D)
		{
<<<<<<< HEAD
			int x2 = x + p_205181_5_;
			int y2 = y + p_205181_6_;
			int z2 = z + p_205181_7_;
			double lvt_19_1_ = p_205181_10_ ? -0.5D : (-6 - random.nextInt(3));
			if (lvt_16_1_ > lvt_19_1_ && random.nextDouble() > 0.9D)
			{
				return;
			}
			setIcebergBlock(x2, y2, z2, world, random, p_205181_4_ - p_205181_6_, p_205181_4_, p_205181_10_, p_205181_14_, material);
		}
	}

	private void setIcebergBlock(
		int x, int y, int z, IWorldGenRegion world, Random random, int p_205175_4_,
		int p_205175_5_, boolean p_205175_6_, boolean p_205175_7_, LocalMaterialData material
	)
=======
			int x2 = x + x1;
			int y2 = y + y1;
			int z2 = z + z1;
			double lvt_19_1_ = flag ? -0.5D : (double)(-6 - random.nextInt(3));
			if (signedDistance > lvt_19_1_ && random.nextDouble() > 0.9D)
			{
				return;
			}
			setIcebergBlock(x2, y2, z2, worldGenRegion, random, irandom - y1, irandom, flag, flag1, material);
		}
	}

	private void setIcebergBlock(int x, int y, int z, IWorldGenRegion world, Random random, int irandomy, int irandom, boolean flag, boolean flag1, LocalMaterialData material)
>>>>>>> daf775313c7c12b81ed6606aab1c2d888e46ae1e
	{
		LocalMaterialData current = world.getMaterialDirect(x, y, z);
		if (current.isAir() || current.isMaterial(LocalMaterials.SNOW_BLOCK) || current.isMaterial(LocalMaterials.ICE) || current.isMaterial(LocalMaterials.WATER))
		{
<<<<<<< HEAD
			boolean lvt_10_1_ = (!p_205175_6_ || random.nextDouble() > 0.05D);
			int lvt_11_1_ = p_205175_6_ ? 3 : 2;
			if (
				p_205175_7_ && (!current.isMaterial(LocalMaterials.WATER))
				&& p_205175_4_ <= random.nextInt(Math.max(1, p_205175_5_ / lvt_11_1_)) + p_205175_5_ * 0.6D
				&& lvt_10_1_
			)
=======
			boolean flag2 = !flag || random.nextDouble() > 0.05D;
			int irandom1 = flag ? 3 : 2;
			if (flag1 && !current.isMaterial(LocalMaterials.WATER) && (double)irandomy <= (double)random.nextInt(Math.max(1, irandom / irandom1)) + (double)irandom * 0.6D && flag2)
>>>>>>> daf775313c7c12b81ed6606aab1c2d888e46ae1e
			{
				world.setBlockDirect(x, y, z, LocalMaterials.SNOW_BLOCK);
			} else {
				world.setBlockDirect(x, y, z, material);
			}
		}
	}

	private int getEllipseC(int y1, int irandom1, int irandom2)
	{
		int ellipsec = irandom2;
		if (y1 > 0 && irandom1 - y1 <= 3)
		{
			ellipsec -= (4 - (irandom1 - y1));
		}

		return ellipsec;
	}

	private double signedDistanceCircle(int x1, int z1, int x, int y, int z, int irandom, Random random)
	{
		float frandom = 10.0F * MathHelper.clamp(random.nextFloat(), 0.2F, 0.8F) / (float)irandom;
		return (double)frandom + Math.pow((double)(x1 - x), 2.0D) + Math.pow((double)(z1 - z), 2.0D) - Math.pow((double)irandom, 2.0D);
	}

	private double signedDistanceEllipse(int x1, int z1, int x, int y, int z, int irandom, int iellipsec, double drandom)
	{
		return Math.pow(((double)(x1 - x) * Math.cos(drandom) - (double)(z1 - z) * Math.sin(drandom)) / (double)irandom, 2.0D) + Math.pow(((double)(x1 - x) * Math.sin(drandom) + (double)(z1 - z) * Math.cos(drandom)) / (double)iellipsec, 2.0D) - 1.0D;
	}

<<<<<<< HEAD
	private int heightDependentRadiusRound(Random random, int p_205183_2_, int p_205183_3_, int p_205183_4_)
	{
		float lvt_5_1_ = 3.5F - random.nextFloat();
		float lvt_6_1_ = (1.0F - (float) Math.pow(p_205183_2_, 2.0D) / p_205183_3_ * lvt_5_1_) * p_205183_4_;

		if (p_205183_3_ > 15 + random.nextInt(5))
		{
			int lvt_7_1_ = (p_205183_2_ < 3 + random.nextInt(6)) ? (p_205183_2_ / 2) : p_205183_2_;
			lvt_6_1_ = (1.0F - lvt_7_1_ / p_205183_3_ * lvt_5_1_ * 0.4F) * p_205183_4_;

=======
	private int heightDependentRadiusRound(Random random, int irandom1, int irandom2, int irandom3)
	{
		float frandom1 = 3.5F - random.nextFloat();
		float frandom2 = (1.0F - (float) Math.pow((double)irandom1, 2.0D) / ((float)irandom2 * frandom1)) * (float)irandom3;
		if (irandom2 > 15 + random.nextInt(5))
		{
			int irandom4 = irandom1 < 3 + random.nextInt(6) ? irandom1 / 2 : irandom1;
			frandom2 = (1.0F - (float)irandom4 / ((float)irandom2 * frandom1 * 0.4F)) * (float)irandom3;
>>>>>>> daf775313c7c12b81ed6606aab1c2d888e46ae1e
		}
		return MathHelper.ceil(frandom2 / 2.0F);
	}

	private int heightDependentRadiusEllipse(int irandomy, int irandom1, int irandom2)
	{
<<<<<<< HEAD
		float lvt_5_1_ = (1.0F - (float) Math.pow(p_205178_1_, 2.0D) / p_205178_2_ * 1.0F) * p_205178_3_;
		return MathHelper.ceil(lvt_5_1_ / 2.0F);
=======
		float f1 = 1.0F;
		float frandom = (1.0F - (float)Math.pow((double)irandomy, 2.0D) / ((float)irandom1 * 1.0F)) * (float)irandom2;
		return MathHelper.ceil(frandom / 2.0F);
>>>>>>> daf775313c7c12b81ed6606aab1c2d888e46ae1e
	}

	private int heightDependentRadiusSteep(Random random, int random1, int random2, int random3)
	{
		float frandom = 1.0F + random.nextFloat() / 2.0F;
		float frandom1 = (1.0F - (float)random1 / ((float)random2 * frandom)) * (float)random3;
		return MathHelper.ceil(frandom1 / 2.0F);
	}

	private boolean isIcebergBlock(LocalMaterialData material)
	{
		return material.isMaterial(LocalMaterials.PACKED_ICE) || material.isMaterial(LocalMaterials.SNOW_BLOCK) || material.isMaterial(LocalMaterials.BLUE_ICE);
	}

	private boolean belowIsAir(IWorldGenRegion world, int x, int y, int z)
	{
		return world.getMaterialDirect(x, y - 1, z).isAir();
	}

	private void smooth(IWorldGenRegion world, int x, int y, int z, int irandom1, int ismoothheight, boolean flag, int irandom3)
	{
<<<<<<< HEAD
		int radius = p_205186_5_ ? p_205186_6_ : (p_205186_3_ / 2);
=======
		int iradius = flag ? irandom3 : irandom1 / 2;
>>>>>>> daf775313c7c12b81ed6606aab1c2d888e46ae1e

		int x2;
		int y2;
		int z2;
<<<<<<< HEAD
		for (int xOffset = -radius; xOffset <= radius; xOffset++)
		{
			for (int zOffset = -radius; zOffset <= radius; zOffset++)
			{
				for (int yOffset = 0; yOffset <= p_205186_4_; yOffset++)
				{
					x2 = x + xOffset;
					y2 = y + yOffset;
					z2 = z + zOffset;
=======
		for (int x1 = -iradius; x1 <= iradius; x1++)
		{
			for (int z1 = -iradius; z1 <= iradius; z1++)
			{
				for (int y1 = 0; y1 <= ismoothheight; y1++)
				{
					x2 = x + x1;
					y2 = y + y1;
					z2 = z + z1;
>>>>>>> daf775313c7c12b81ed6606aab1c2d888e46ae1e
					LocalMaterialData material = world.getMaterialDirect(x2, y2, z2);
					if (isIcebergBlock(material) || material.isMaterial(LocalMaterials.SNOW))
					{
						if (belowIsAir(world, x2, y2, z2))
						{
							world.setBlockDirect(x2, y2, z2, LocalMaterials.AIR);
							world.setBlockDirect(x2, y2 + 1, z2, LocalMaterials.AIR);	
						}
						else if (isIcebergBlock(material))
<<<<<<< HEAD
						{	
							LocalMaterialData[] adjacentMaterials =
=======
						{
							LocalMaterialData[] materials =
>>>>>>> daf775313c7c12b81ed6606aab1c2d888e46ae1e
							{
								world.getMaterialDirect(x2 - 1, y2, z2), 
								world.getMaterialDirect(x2 + 1, y2, z2),
								world.getMaterialDirect(x2, y2, z2 - 1), 
								world.getMaterialDirect(x2, y2, z2 + 1) 
							};
<<<<<<< HEAD
							int count = 0;
							for (LocalMaterialData mat : adjacentMaterials)
							{
								if (!isIcebergBlock(mat))
								{
									count++;
								}
							}
							if (count >= 3)
=======
							int iheight = 0;
							for (LocalMaterialData mat : materials)
							{
								if (!isIcebergBlock(mat))
								{
									iheight++;
								}
							}
							if (iheight >= 3)
>>>>>>> daf775313c7c12b81ed6606aab1c2d888e46ae1e
							{
								world.setBlockDirect(x2, y2, z2, LocalMaterials.AIR);
							}
						}
					}
				}
			}
		}
	}

	@Override
	public String toString()
	{
		return "Iceberg(" + this.material.toString() + ", " + this.rarity + ")";
	}
}
