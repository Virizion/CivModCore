package vg.civcraft.mc.civmodcore.serialization;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import java.io.IOException;
import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.server.v1_14_R1.NBTBase;
import net.minecraft.server.v1_14_R1.NBTCompressedStreamTools;
import net.minecraft.server.v1_14_R1.NBTReadLimiter;
import net.minecraft.server.v1_14_R1.NBTTagCompound;
import net.minecraft.server.v1_14_R1.NBTTagDouble;
import net.minecraft.server.v1_14_R1.NBTTagFloat;
import net.minecraft.server.v1_14_R1.NBTTagList;
import net.minecraft.server.v1_14_R1.NBTTagLong;
import net.minecraft.server.v1_14_R1.NBTTagShort;
import net.minecraft.server.v1_14_R1.NBTTagString;
import org.bukkit.craftbukkit.v1_14_R1.inventory.CraftItemStack;
import org.bukkit.inventory.ItemStack;
import vg.civcraft.mc.civmodcore.api.ItemAPI;
import vg.civcraft.mc.civmodcore.util.Iteration;

/**
 * Wrapper class for NBTTagCompounds to make NBT serialisation and deserialisation as robust as possible. Intended to
 * replace {@link vg.civcraft.mc.civmodcore.itemHandling.TagManager TagManager} though the .mapToNBT and .listToNBT
 * APIs will not be re-implemented here as it's better to have a finer control of how data is written and read.
 */
public class NBTCompound implements Cloneable {

	public static final String NULL_STRING = "\u0000";

	private NBTTagCompound tag;

	/**
	 * Creates a new NBTCompound, generating a new NBTTagCompound.
	 */
	public NBTCompound() {
		this.tag = new NBTTagCompound();
	}

	/**
	 * Creates a new NBTCompound by wrapping an existing NBTTagCompound.
	 *
	 * @param tag The NBTTagCompound to wrap.
	 */
	public NBTCompound(NBTTagCompound tag) {
		Preconditions.checkArgument(tag != null);
		this.tag = tag;
	}

	/**
	 * Creates a new NBTCompound by wrapping and serialising an NBTSerializable object.
	 *
	 * @param <T> The type of the given NBTSerializable.
	 * @param object The NBTSerializable to wrap and serialize.
	 */
	public <T extends NBTSerializable> NBTCompound(T object) {
		this();
		Preconditions.checkArgument(object != null);
		object.serialize(this);
	}

	/**
	 * Returns the size of the tag compound.
	 *
	 * @return The size of tha tag compound.
	 */
	public int size() {
		return this.tag.d();
	}

	/**
	 * Checks if the tag compound is empty.
	 *
	 * @return Returns true if the tag compound is empty.
	 */
	public boolean isEmpty() {
		return this.tag.isEmpty();
	}

	/**
	 * Checks if the tag compound contains a particular key.
	 *
	 * @param key The key to check.
	 * @return Returns true if the contains the given key.
	 */
	public boolean hasKey(String key) {
		return this.tag.hasKey(key);
	}

	/**
	 * Removes a key and its respective value from the tag compound, if it exists.
	 *
	 * @param key The key to remove.
	 */
	public void remove(String key) {
		this.tag.remove(key);
	}

	/**
	 * Clears all values from the tag compound.
	 */
	public void clear() {
		this.tag.getKeys().forEach(this.tag::remove);
	}

	/**
	 * Clones the NBT compound.
	 *
	 * @return Returns a duplicated version of this NBT compound.
	 */
	@Override
	public NBTCompound clone() throws CloneNotSupportedException {
		NBTCompound clone = (NBTCompound) super.clone();
		clone.tag = this.tag.clone();
		return clone;
	}

	/**
	 * Returns the underlying NBTTagCompound.
	 *
	 * @return The wrapped NBTTagCompound.
	 */
	public NBTTagCompound getRAW() {
		return this.tag;
	}

	/**
	 * Adopts the NBT data from another compound.
	 *
	 * @param nbt The NBT data to copy and adopt.
	 */
	public void adopt(NBTCompound nbt) {
		Preconditions.checkArgument(nbt != null);
		this.tag = nbt.tag.clone();
	}

	/**
	 * Gets a primitive boolean value from a key.
	 *
	 * @param key The key to get the value of.
	 * @return The value of the key.
	 */
	public boolean getBoolean(String key) {
		Preconditions.checkArgument(!Strings.isNullOrEmpty(key));
		return this.tag.getBoolean(key);
	}

	/**
	 * Sets a primitive boolean value to a key.
	 *
	 * @param key The key to set to value to.
	 * @param value The value to set to the key.
	 */
	public void setBoolean(String key, boolean value) {
		Preconditions.checkArgument(!Strings.isNullOrEmpty(key));
		this.tag.setBoolean(key, value);
	}

	/**
	 * Gets an array of primitive booleans from a key.
	 *
	 * @param key The key to get the values of.
	 * @return The values of the key, which is never null.
	 */
	public boolean[] getBooleanArray(String key) {
		Preconditions.checkArgument(!Strings.isNullOrEmpty(key));
		byte[] cache = this.tag.getByteArray(key);
		boolean[] result = new boolean[cache.length];
		for (int i = 0; i < cache.length; i++) {
			result[i] = cache[i] == 0x1;
		}
		return result;
	}

	/**
	 * Sets an array of primitive booleans to a key.
	 *
	 * @param key The key to set to values to.
	 * @param values The values to set to the key.
	 */
	public void setBooleanArray(String key, boolean[] values) {
		Preconditions.checkArgument(!Strings.isNullOrEmpty(key));
		if (Iteration.isNullOrEmpty(values)) {
			remove(key);
		}
		else {
			byte[] cache = new byte[values.length];
			for (int i = 0; i < values.length; i++) {
				cache[i] = (byte) (values[i] ? 0x1 : 0x0);
			}
			this.tag.setByteArray(key, cache);
		}
	}

	/**
	 * Gets a primitive byte value from a key.
	 *
	 * @param key The key to get the value of.
	 * @return The value of the key.
	 */
	public byte getByte(String key) {
		Preconditions.checkArgument(!Strings.isNullOrEmpty(key));
		return this.tag.getByte(key);
	}

	/**
	 * Sets a primitive byte value to a key.
	 *
	 * @param key The key to set to value to.
	 * @param value The value to set to the key.
	 */
	public void setByte(String key, byte value) {
		Preconditions.checkArgument(!Strings.isNullOrEmpty(key));
		this.tag.setByte(key, value);
	}

	/**
	 * Gets an array of primitive bytes from a key.
	 *
	 * @param key The key to get the values of.
	 * @return The values of the key, which is never null.
	 */
	public byte[] getByteArray(String key) {
		Preconditions.checkArgument(!Strings.isNullOrEmpty(key));
		return this.tag.getByteArray(key);
	}

	/**
	 * Sets an array of primitive bytes to a key.
	 *
	 * @param key The key to set to values to.
	 * @param values The values to set to the key.
	 */
	public void setByteArray(String key, byte[] values) {
		Preconditions.checkArgument(!Strings.isNullOrEmpty(key));
		if (values == null || values.length < 1) {
			remove(key);
		}
		else {
			this.tag.setByteArray(key, values);
		}
	}

	/**
	 * Gets a primitive short value from a key.
	 *
	 * @param key The key to get the value of.
	 * @return The value of the key.
	 */
	public short getShort(String key) {
		Preconditions.checkArgument(!Strings.isNullOrEmpty(key));
		return this.tag.getShort(key);
	}

	/**
	 * Sets a primitive short value to a key.
	 *
	 * @param key The key to set to value to.
	 * @param value The value to set to the key.
	 */
	public void setShort(String key, short value) {
		Preconditions.checkArgument(!Strings.isNullOrEmpty(key));
		this.tag.setShort(key, value);
	}

	/**
	 * Gets an array of primitive shorts from a key.
	 *
	 * @param key The key to get the values of.
	 * @return The values of the key, which is never null.
	 */
	public short[] getShortArray(String key) {
		Preconditions.checkArgument(!Strings.isNullOrEmpty(key));
		NBTTagList list = this.tag.getList(key, 2);
		short[] result = new short[list.size()];
		for (int i = 0; i < result.length; i++) {
			NBTBase base = list.get(i);
			if (base.getTypeId() != 2) {
				result[i] = 0;
			}
			else if (!(base instanceof NBTTagShort)) {
				result[i] = 0;
			}
			else {
				result[i] = ((NBTTagShort) base).asShort();
			}
		}
		return result;
	}

	/**
	 * Sets an array of primitive bytes to a key.
	 *
	 * @param key The key to set to values to.
	 * @param values The values to set to the key.
	 */
	public void setShortArray(String key, short[] values) {
		Preconditions.checkArgument(!Strings.isNullOrEmpty(key));
		if (values == null || values.length < 1) {
			remove(key);
		}
		else {
			NBTTagList list = new NBTTagList();
			for (short value : values) {
				list.add(new NBTTagShort(value));
			}
			this.tag.set(key, list);
		}
	}

	/**
	 * Gets a primitive integer value from a key.
	 *
	 * @param key The key to get the value of.
	 * @return The value of the key.
	 */
	public int getInteger(String key) {
		Preconditions.checkArgument(!Strings.isNullOrEmpty(key));
		return this.tag.getInt(key);
	}

	/**
	 * Sets a primitive integer value to a key.
	 *
	 * @param key The key to set to value to.
	 * @param value The value to set to the key.
	 */
	public void setInteger(String key, int value) {
		Preconditions.checkArgument(!Strings.isNullOrEmpty(key));
		this.tag.setInt(key, value);
	}

	/**
	 * Gets an array of primitive integers from a key.
	 *
	 * @param key The key to get the values of.
	 * @return The values of the key, which is never null.
	 */
	public int[] getIntegerArray(String key) {
		Preconditions.checkArgument(!Strings.isNullOrEmpty(key));
		return this.tag.getIntArray(key);
	}

	/**
	 * Sets an array of primitive integers to a key.
	 *
	 * @param key The key to set to values to.
	 * @param values The values to set to the key.
	 */
	public void setIntegerArray(String key, int[] values) {
		Preconditions.checkArgument(!Strings.isNullOrEmpty(key));
		if (values == null || values.length < 1) {
			remove(key);
		}
		else {
			this.tag.setIntArray(key, values);
		}
	}

	/**
	 * Gets a primitive long value from a key.
	 *
	 * @param key The key to get the value of.
	 * @return The value of the key.
	 */
	public long getLong(String key) {
		Preconditions.checkArgument(!Strings.isNullOrEmpty(key));
		return this.tag.getLong(key);
	}

	/**
	 * Sets a primitive long value to a key.
	 *
	 * @param key The key to set to value to.
	 * @param value The value to set to the key.
	 */
	public void setLong(String key, long value) {
		Preconditions.checkArgument(!Strings.isNullOrEmpty(key));
		this.tag.setLong(key, value);
	}

	/**
	 * Gets an array of primitive longs from a key.
	 *
	 * @param key The key to get the values of.
	 * @return The values of the key, which is never null.
	 */
	public long[] getLongArray(String key) {
		Preconditions.checkArgument(!Strings.isNullOrEmpty(key));
		NBTTagList list = this.tag.getList(key, 4);
		long[] result = new long[list.size()];
		for (int i = 0; i < result.length; i++) {
			NBTBase base = list.get(i);
			if (base.getTypeId() != 4) {
				result[i] = 0;
			}
			else if (!(base instanceof NBTTagLong)) {
				result[i] = 0;
			}
			else {
				result[i] = ((NBTTagLong) base).asLong();
			}
		}
		return result;
	}

	/**
	 * Sets an array of primitive longs to a key.
	 *
	 * @param key The key to set to values to.
	 * @param values The values to set to the key.
	 */
	public void setLongArray(String key, long[] values) {
		Preconditions.checkArgument(!Strings.isNullOrEmpty(key));
		if (values == null || values.length < 1) {
			remove(key);
		}
		else {
			NBTTagList list = new NBTTagList();
			for (long value : values) {
				list.add(new NBTTagLong(value));
			}
			this.tag.set(key, list);
		}
	}

	/**
	 * Gets a primitive float value from a key.
	 *
	 * @param key The key to get the value of.
	 * @return The value of the key.
	 */
	public float getFloat(String key) {
		Preconditions.checkArgument(!Strings.isNullOrEmpty(key));
		return this.tag.getFloat(key);
	}

	/**
	 * Sets a primitive float value to a key.
	 *
	 * @param key The key to set to value to.
	 * @param value The value to set to the key.
	 */
	public void setFloat(String key, float value) {
		Preconditions.checkArgument(!Strings.isNullOrEmpty(key));
		this.tag.setFloat(key, value);
	}

	/**
	 * Gets an array of primitive floats from a key.
	 *
	 * @param key The key to get the values of.
	 * @return The values of the key, which is never null.
	 */
	public float[] getFloatArray(String key) {
		Preconditions.checkArgument(!Strings.isNullOrEmpty(key));
		NBTTagList list = this.tag.getList(key, 5);
		float[] result = new float[list.size()];
		for (int i = 0; i < result.length; i++) {
			result[i] = list.i(i);
		}
		return result;
	}

	/**
	 * Sets an array of primitive floats to a key.
	 *
	 * @param key The key to set to values to.
	 * @param values The values to set to the key.
	 */
	public void setFloatArray(String key, float[] values) {
		Preconditions.checkArgument(!Strings.isNullOrEmpty(key));
		if (values == null || values.length < 1) {
			remove(key);
		}
		else {
			NBTTagList list = new NBTTagList();
			for (float value : values) {
				list.add(new NBTTagFloat(value));
			}
			this.tag.set(key, list);
		}
	}

	/**
	 * Gets a primitive double value from a key.
	 *
	 * @param key The key to get the value of.
	 * @return The value of the key.
	 */
	public double getDouble(String key) {
		Preconditions.checkArgument(!Strings.isNullOrEmpty(key));
		return this.tag.getDouble(key);
	}

	/**
	 * Sets a primitive double value to a key.
	 *
	 * @param key The key to set to value to.
	 * @param value The value to set to the key.
	 */
	public void setDouble(String key, double value) {
		Preconditions.checkArgument(!Strings.isNullOrEmpty(key));
		this.tag.setDouble(key, value);
	}

	/**
	 * Gets an array of primitive doubles from a key.
	 *
	 * @param key The key to get the values of.
	 * @return The values of the key, which is never null.
	 */
	public double[] getDoubleArray(String key) {
		Preconditions.checkArgument(!Strings.isNullOrEmpty(key));
		NBTTagList list = this.tag.getList(key, 6);
		double[] result = new double[list.size()];
		for (int i = 0; i < result.length; i++) {
			result[i] = list.h(i);
		}
		return result;
	}

	/**
	 * Sets an array of primitive doubles to a key.
	 *
	 * @param key The key to set to values to.
	 * @param values The values to set to the key.
	 */
	public void setDoubleArray(String key, double[] values) {
		Preconditions.checkArgument(!Strings.isNullOrEmpty(key));
		if (values == null || values.length < 1) {
			remove(key);
		}
		else {
			NBTTagList list = new NBTTagList();
			for (double value : values) {
				list.add(new NBTTagDouble(value));
			}
			this.tag.set(key, list);
		}
	}

	/**
	 * Gets a UUID value from a key.
	 *
	 * @param key The key to get the value of.
	 * @return The value of the key, which is never null.
	 */
	public UUID getUUID(String key) {
		Preconditions.checkArgument(!Strings.isNullOrEmpty(key));
		UUID uuid = this.tag.a(key);
		if (uuid == null) {
			return new UUID(0, 0);
		}
		return uuid;
	}

	/**
	 * Sets a UUID value to a key.
	 *
	 * @param key The key to set to value to.
	 * @param value The value to set to the key.
	 */
	public void setUUID(String key, UUID value) {
		Preconditions.checkArgument(!Strings.isNullOrEmpty(key));
		if (value == null) {
			remove(key);
		}
		else {
			this.tag.a(key, value);
		}
	}

	/**
	 * Gets a UUID value from a key.
	 *
	 * @param key The key to get the value of.
	 * @return The value of the key.
	 */
	public String getString(String key) {
		Preconditions.checkArgument(!Strings.isNullOrEmpty(key));
		if (!this.tag.hasKeyOfType(key, 8)) {
			return null;
		}
		else {
			String value = this.tag.getString(key);
			if (value.equals(NULL_STRING)) {
				return null;
			}
			return value;
		}
	}

	/**
	 * Sets a String value to a key.
	 *
	 * @param key The key to set to value to.
	 * @param value The value to set to the key.
	 */
	public void setString(String key, String value) {
		Preconditions.checkArgument(!Strings.isNullOrEmpty(key));
		if (Strings.isNullOrEmpty(value)) {
			remove(key);
		}
		else {
			this.tag.setString(key, value);
		}
	}

	/**
	 * Gets an array of Strings from a key.
	 *
	 * @param key The key to get the values of.
	 * @return The values of the key, which is never null.
	 */
	public String[] getStringArray(String key) {
		Preconditions.checkArgument(!Strings.isNullOrEmpty(key));
		NBTTagList list = this.tag.getList(key, 8);
		String[] result = new String[list.size()];
		for (int i = 0; i < result.length; i++) {
			NBTBase base = list.get(i);
			if (base.getTypeId() != 8) {
				result[i] = "";
			}
			else if (!(base instanceof NBTTagString)) {
				result[i] = "";
			}
			else {
				result[i] = base.asString();
				if (result[i].equals(NULL_STRING)) {
					result[i] = null;
				}
			}
		}
		return result;
	}

	/**
	 * Sets an array of Strings to a key.
	 *
	 * @param key The key to set to values to.
	 * @param values The values to set to the key.
	 */
	public void setStringArray(String key, String[] values) {
		Preconditions.checkArgument(!Strings.isNullOrEmpty(key));
		if (values == null || values.length < 1) {
			remove(key);
		}
		else {
			NBTTagList list = new NBTTagList();
			for (String value : values) {
				if (value == null) {
					list.add(new NBTTagString(NULL_STRING));
				}
				else {
					list.add(new NBTTagString(value));
				}
			}
			this.tag.set(key, list);
		}
	}

	/**
	 * Gets a tag compound value from a key.
	 *
	 * @param key The key to get the value of.
	 * @return The value of the key, which is never null.
	 */
	public NBTCompound getCompound(String key) {
		Preconditions.checkArgument(!Strings.isNullOrEmpty(key));
		return new NBTCompound(this.tag.getCompound(key));
	}

	/**
	 * Sets a tag compound value to a key.
	 *
	 * @param key The key to set to value to.
	 * @param value The value to set to the key.

	 */
	public void setCompound(String key, NBTCompound value) {
		Preconditions.checkArgument(!Strings.isNullOrEmpty(key));
		if (value == null || value.isEmpty()) {
			remove(key);
		}
		else {
			this.tag.set(key, value.tag);
		}
	}

	/**
	 * Gets an array of tag compounds from a key.
	 *
	 * @param key The key to get the values of.
	 * @return The values of the key, which is never null.
	 */
	public NBTCompound[] getCompoundArray(String key) {
		Preconditions.checkArgument(!Strings.isNullOrEmpty(key));
		NBTTagList list = this.tag.getList(key, 10);
		NBTCompound[] result = new NBTCompound[list.size()];
		for (int i = 0; i < result.length; i++) {
			NBTTagCompound base = list.getCompound(i);
			if (base.getTypeId() != 10) {
				result[i] = new NBTCompound();
			}
			else {
				result[i] = new NBTCompound(base);
			}
		}
		return result;
	}

	/**
	 * Sets an array of tag compounds to a key.
	 *
	 * @param key The key to set to values to.
	 * @param values The values to set to the key.
	 */
	public void setCompoundArray(String key, NBTCompound[] values) {
		Preconditions.checkArgument(!Strings.isNullOrEmpty(key));
		if (values == null || values.length < 1) {
			remove(key);
		}
		else {
			NBTTagList list = new NBTTagList();
			for (NBTCompound value : values) {
				list.add(value.tag);
			}
			this.tag.set(key, list);
		}
	}

	/**
	 * Retrieves the NBT data from an item.
	 *
	 * @param item The item to retrieve the NBT form.
	 * @return Returns the item's NBT, which is never null.
	 */
	public static NBTCompound fromItem(ItemStack item) {
		if (item == null) {
			return null;
		}
		net.minecraft.server.v1_14_R1.ItemStack craftItem = CraftItemStack.asNMSCopy(item);
		if (craftItem == null) {
			return null;
		}
		NBTTagCompound tag = craftItem.getTag();
		if (tag == null) {
			return new NBTCompound();
		}
		return new NBTCompound(tag);
	}

	/**
	 * Sets an NBT compound to an item. You must use the returned item, instead of the item you pass in.
	 *
	 * @param item The item to set the NBT to.
	 * @param nbt The NBT to set to the item.
	 * @return The item with the saved NBT.
	 *
	 * @deprecated Use {@link NBTCompound#processItem(ItemStack, Consumer)} instead.
	 */
	@Deprecated
	public static ItemStack toItem(ItemStack item, NBTCompound nbt) {
		return processItem(item, (current) -> current.adopt(nbt));
	}

	/**
	 * Processes an item's NBT before setting again.
	 *
	 * @param item The item to process.
	 * @param processor The processor.
	 * @return Returns the given item with the processed NBT, or null if it could not be successfully processed.
	 */
	public static ItemStack processItem(ItemStack item, Consumer<NBTCompound> processor) {
		Preconditions.checkArgument(ItemAPI.isValidItem(item));
		Preconditions.checkArgument(processor != null);
		net.minecraft.server.v1_14_R1.ItemStack craftItem = CraftItemStack.asNMSCopy(item);
		if (craftItem == null) {
			return null;
		}
		NBTTagCompound raw = craftItem.getTag();
		if (raw == null) {
			raw = new NBTTagCompound();
		}
		NBTCompound nbt = new NBTCompound(raw);
		try {
			processor.accept(nbt);
		}
		catch (Exception exception) {
			return null;
		}
		craftItem.setTag(nbt.tag);
		return CraftItemStack.asBukkitCopy(craftItem);
	}

	/**
	 * Attempts to deserialize NBT data into an NBTCompound.
	 *
	 * @param bytes The NBT data as a byte array.
	 * @return Returns an NBTCompound if the deserialization was successful, or otherwise null.
	 */
	public static NBTCompound fromBytes(byte[] bytes) {
		if (Iteration.isNullOrEmpty(bytes)) {
			return null;
		}
		@SuppressWarnings("UnstableApiUsage")
		ByteArrayDataInput input = ByteStreams.newDataInput(bytes);
		NBTCompound nbt = new NBTCompound();
		try {
			nbt.tag = NBTCompressedStreamTools.a(input, NBTReadLimiter.a);
		}
		catch (IOException exception) {
			return null;
		}
		return nbt;
	}

	/**
	 * Attempts to serialize an NBTCompound into a data array.
	 *
	 * @param nbt The NBTCompound to serialize.
	 * @return Returns a data array representing the given NBTCompound serialized, or otherwise null.
	 */
	public static byte[] toBytes(NBTCompound nbt) {
		if (nbt == null) {
			return null;
		}
		@SuppressWarnings("UnstableApiUsage")
		ByteArrayDataOutput output = ByteStreams.newDataOutput();
		try {
			NBTCompressedStreamTools.a(nbt.tag, output);
		}
		catch (IOException exception) {
			return null;
		}
		return output.toByteArray();
	}

}
