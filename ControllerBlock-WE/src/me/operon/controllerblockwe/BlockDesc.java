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
	//TODO: Inventory
	//TODO: Other data
	
	/**
	 * Populates the data
	 * @param l Location
	 * @param m Material
	 * @param b Metadata
	 */
	public BlockDesc(Location l, Material m, byte b) {
		loc = l;
		data = b;
		mat = m;
	}
}