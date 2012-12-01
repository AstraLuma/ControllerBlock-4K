package me.operon.controllerblockwe;

import org.bukkit.Location;
import org.bukkit.Material;

/**
 * Description of an individual serf block
 * @author astronouth7303
 *
 */
public class BlockDesc {
	/**
	 * The storage ID
	 */
	public long id;
	/**
	 * The location, including world
	 */
	public Location loc;
	/**
	 * The kind of block
	 */
	public Material mat;
	/**
	 * The metadata
	 */
	public byte data;
	//TODO: Inventory (If net.minecraft.server.IInventory, org.bukkit.inventory.InventoryHolder)
	//TODO: Other data -- need to figure out how to do this.
	
	/**
	 * Populates the data
	 * @param l Location
	 * @param m Material
	 * @param b Metadata
	 */
	public BlockDesc(Location l, Material m, byte b) {
		id = 0;
		loc = l;
		data = b;
		mat = m;
	}

	public BlockDesc(long i, Location l, Material m, byte b) {
		id = i;
		loc = l;
		data = b;
		mat = m;
	}
}