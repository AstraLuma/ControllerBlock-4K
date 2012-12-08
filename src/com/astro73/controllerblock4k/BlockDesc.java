package com.astro73.controllerblock4k;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;

/**
 * Description of an individual serf block
 * @author astronouth7303
 *
 */
@Entity
@Table(name="ControllerBlock_Serf")
public class BlockDesc {
	/**
	 * The storage ID
	 */
	@Id
	long id;
	
	@ManyToOne(cascade=CascadeType.ALL)
	CBlock lord;
	
	@Column(name="world")
	String world;
	@Column(name="x")
	int x;
	@Column(name="y")
	int y;
	@Column(name="z")
	int z;
	
	/**
	 * The location, including world
	 */
	@Transient
	private Location loc = null;
	
	/**
	 * The kind of block
	 */
	@Column(name="material")
	Material mat;
	
	/**
	 * The metadata
	 */
	@Column(name="meta")
	byte data;
	
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
		world = l.getWorld().getName();
		x = l.getBlockX();
		y = l.getBlockY();
		z = l.getBlockZ();
		data = b;
		mat = m;
	}

	public BlockDesc(long i, Location l, Material m, byte b) {
		id = i;
		loc = l;
		data = b;
		mat = m;
	}

	public Location getLocation() {
		if (loc == null) {
			loc = new Location(Util.getWorld(world), x, y, z);
		}
		return loc;
	}
	
	/**
	 * Applies this description to the game.
	 */
	public void apply(boolean applyPhysics) {
		Block b = getLocation().getBlock();
		b.setTypeIdAndData(mat.getId(), data, applyPhysics);
	}
}