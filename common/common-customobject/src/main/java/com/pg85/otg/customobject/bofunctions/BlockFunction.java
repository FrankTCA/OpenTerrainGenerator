package com.pg85.otg.customobject.bofunctions;

import java.util.List;
import java.util.Random;

import com.pg85.otg.customobject.config.CustomObjectConfigFile;
import com.pg85.otg.customobject.config.CustomObjectConfigFunction;
import com.pg85.otg.util.nbt.NBTHelper;
import com.pg85.otg.exceptions.InvalidConfigException;
import com.pg85.otg.interfaces.ILogger;
import com.pg85.otg.interfaces.IMaterialReader;
import com.pg85.otg.interfaces.IWorldGenRegion;
import com.pg85.otg.util.biome.ReplaceBlockMatrix;
import com.pg85.otg.util.nbt.NamedBinaryTag;
import com.pg85.otg.util.materials.LocalMaterialData;
import com.pg85.otg.util.minecraft.BlockNames;

/**
 * Represents a block in a BO3.
 */
public abstract class BlockFunction<T extends CustomObjectConfigFile> extends CustomObjectConfigFunction<T>
{
	public LocalMaterialData material;
	public short y;
	public NamedBinaryTag nbt;
	public String nbtName;

	@Override
	public void load(List<String> args, ILogger logger, IMaterialReader materialReader) throws InvalidConfigException
	{
		assureSize(4, args);
		// Those limits are arbitrary, LocalWorld.setBlock will limit it
		// correctly based on what chunks can be accessed
		x = readInt(args.get(0), -100, 100);
		y = (short) readInt(args.get(1), -1000, 1000);
		z = readInt(args.get(2), -100, 100);

		material = readMaterial(args.get(3), materialReader);
		if(material == null)
		{
			throw new InvalidConfigException("Material \"" + args.get(3) + "\" could not be read.");
		}

		if (args.size() >= 5)
		{
			// Code that converts legacy block ids inside chests - Frank
			nbt = NBTHelper.loadMetadata(args.get(4), getHolder().getFile(), logger);
			if (nbt != null)
			{
				if (nbt.getTag("Items") != null) {
					for (NamedBinaryTag item : (NamedBinaryTag[]) nbt.getTag("Items").getValue()) {
						if (item.getTag("id").getType() == NamedBinaryTag.Type.TAG_Short) {
							short val = (short)item.getTag("id").getValue();
							item.removeSubTag(item.getTag("id"));
							NamedBinaryTag[] newItemValue = new NamedBinaryTag[((NamedBinaryTag[])item.getValue()).length + 1];
							System.arraycopy(item.getValue(), 0, newItemValue, 0, newItemValue.length - 1);
							String strVal = "minecraft:" + BlockNames.blockNameFromLegacyBlockId(val);
							newItemValue[newItemValue.length-2] = new NamedBinaryTag(NamedBinaryTag.Type.TAG_String, "id", strVal);
							newItemValue[newItemValue.length-1] = new NamedBinaryTag(NamedBinaryTag.Type.TAG_End, "", null);
							item.setValue(newItemValue);
						}
					}
				}
				nbtName = args.get(4);
			}
			if (nbt.getTag("SkullType") != null) {
				byte val = (byte)nbt.getTag("SkullType").getValue();
				switch ((int)val) {
					case 1:
						this.material = materialReader.readMaterial("minecraft:wither_skeleton_skull");
						break;
					case 2:
						this.material = materialReader.readMaterial("minecraft:zombie_head");
						break;
					case 3:
						this.material = materialReader.readMaterial("minecraft:player_head");
						break;
					case 4:
						this.material = materialReader.readMaterial("minecraft:creeper_head");
						break;
					case 5:
						this.material = materialReader.readMaterial("minecraft:dragon_head");
						break;
					default:
						this.material = materialReader.readMaterial("minecraft:skeleton_skull");
				}
			}


	public abstract void spawn(IWorldGenRegion worldGenRegion, Random random, int x, int y, int z);
	
	public abstract void spawn(IWorldGenRegion worldGenRegion, Random random, int x, int y, int z, ReplaceBlockMatrix replaceBlocks);
	
	@Override
	public boolean isAnalogousTo(CustomObjectConfigFunction<T> other)
	{
		if (!getClass().equals(other.getClass()))
		{
			return false;
		}
		BlockFunction<T> block = (BlockFunction<T>) other;
		return block.x == x && block.y == y && block.z == z;
	}
}
