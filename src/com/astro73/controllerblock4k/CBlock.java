package com.astro73.controllerblock4k;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.inventory.ItemStack;
import org.bukkit.material.RedstoneWire;

import com.avaje.ebean.EbeanServer;
import com.avaje.ebean.ExpressionList;

@Entity
@Table(name="ControllerBlock_Lord")
public class CBlock {
	@Id
	long id = 0;
	
	@Column(name="world")
	String world;
	@Column(name="x")
	int x;
	@Column(name="y")
	int y;
	@Column(name="z")
	int z;
	
	@Column(name="protection")
	Protection protectedLevel = Protection.PROTECTED;
	
	@OneToMany(cascade=CascadeType.ALL)
	private Set<BlockDesc> placedBlocks;
	
	@Column(name="owner")
	String owner = null;

	@Column(name="ison")
	private boolean on = false;
	
	@Transient
	private Location loc = null;
	@Transient
	private ControllerBlock parent = null;
	@Transient
	private boolean edit = false;
	
	public enum Protection {
		PROTECTED, SEMIPROTECTED, UNPROTECTED
	}
	
	public CBlock() {
		parent = ControllerBlock.getInstance();
	}
	
	public CBlock(ControllerBlock p, Location l, String o, Protection pl) {
		parent = p;
		loc = l;
		world = l.getWorld().getName();
		x = l.getBlockX();
		y = l.getBlockY();
		z = l.getBlockZ();
		owner = o;
		protectedLevel = pl;
	}

	public CBlock(ControllerBlock p, long i, Location l, String o, Protection pl) {
		parent = p;
		id = i;
		loc = l;
		world = l.getWorld().getName();
		x = l.getBlockX();
		y = l.getBlockY();
		z = l.getBlockZ();
		owner = o;
		protectedLevel = pl;
	}

	public ControllerBlock getParent() {
		return parent;
	}

	public String getOwner() {
		return owner;
	}

	public Location getLocation() {
		if (loc == null) {
			loc = new Location(Util.getWorld(world), x, y, z);
		}
		return loc;
	}
	
	public void setLocation(Location l) {
		loc = l;
		world = l.getWorld().getName();
		x = l.getBlockX();
		y = l.getBlockY();
		z = l.getBlockZ();
	}
	
	@Deprecated
	public Location getLoc() {
		return getLocation();
	}

	public boolean addBlock(Block b) {
		BlockDesc bd = new BlockDesc(b);
		return addBlock(bd);
	}

	public boolean addBlock(BlockDesc bd) {
		parent.getDatabase().save(bd);
		placedBlocks.add(bd);
		return true;
	}

	public boolean delBlock(Block b) {
		EbeanServer db = parent.getDatabase();
		// Find BlockDesc
		BlockDesc bd = Util.FilterLocation(db.find(BlockDesc.class).where(), b.getLocation())
			.eq("lord", this).findUnique();
		
		// Remove
		db.delete(bd);
		
		// Find other available BlockDescs here
		BlockDesc replacement = Util.FilterLocation(db.find(BlockDesc.class).where(), b.getLocation())
			.ne("lord", this)
			.eq("lord.on", true).findIterate().next();
		
		if (replacement == null) {
			return false;
		} else {
			replacement.apply(true);
			return true;
		}
	}

	public int numBlocks() {
		return placedBlocks.size();
	}

	public BlockDesc getBlock(Location l) {
		return findBlocks(l).findUnique();
	}

	public boolean hasBlock(Location l) {
		return getBlock(l) != null;
	}
	
	/**
	 * Find all serfs at location
	 * @param l The location
	 * @return An ebean where() clause
	 */
	protected ExpressionList<BlockDesc> findBlocks(Location l) {
		return Util.FilterLocation(parent.getDatabase().find(BlockDesc.class), l).eq("lord", this);
	}
	
	/**
	 * Find all serfs
	 * @return An ebean where() clause
	 */
	protected ExpressionList<BlockDesc> findBlocks() {
		return parent.getDatabase().find(BlockDesc.class).where().eq("lord", this);
	}
	
	public void updateBlock(Block b) {
		for (BlockDesc d : findBlocks(b.getLocation()).findList()) {
			d.update(b);
		}
	}

	public boolean isBeingEdited() {
		return edit;
	}

	public void editBlock(boolean b) {
		edit = b;
		if (edit) {
			turnOn();
		} else {
			doRedstoneCheck();
			parent.getDatabase().save(this);
		}
	}

	public void destroyWithOutDrops() {
		turnOff();
	}

	public void destroy() {
		if (!isOn()) {
			Map<Material, Integer> counts = new HashMap<Material, Integer>();
			for (BlockDesc bd : placedBlocks) {
				Integer ci = counts.get(bd.mat);
				int c = 0;
				if (ci != null) {
					c = ci;
				}
				c += 1;
				counts.put(bd.mat, c);
			}
			for (Material mat : counts.keySet()) {
				int i = counts.get(mat);
				int j = 0;
				while (i > 0) {
					if (i > 64) {
						j = 64;
						i -= 64;
					} else {
						j = i;
						i -= i;
					}
					loc.getWorld().dropItemNaturally(loc, new ItemStack(mat, j));
				}
			}
		}
		parent.removeCBlock(this);	
	}

	public boolean isOn() {
		return on;
	}

	public void doRedstoneCheck() {
		Block check = Util.getBlockAtLocation(loc).getRelative(
				BlockFace.UP);
		doRedstoneCheck(check.getState());
	}

	public void doRedstoneCheck(BlockState s) {
		if (isBeingEdited()) {
			return;
		}
		if (s.getType().equals(Material.REDSTONE_TORCH_ON)) {
			turnOff();
		} else if (s.getType().equals(Material.REDSTONE_TORCH_OFF)) {
			turnOn();
		} else if (s.getType().equals(Material.REDSTONE_WIRE)) {
			if (((RedstoneWire) s.getData()).isPowered()) {
				turnOff();
			} else {
				turnOn();
			}
		} else if (s.getType().equals(Material.AIR)) {
			turnOn();
		}
	}

	public void turnOff() {
		on = false;
		EbeanServer db = parent.getDatabase();
		db.save(this);
		for (BlockDesc d : placedBlocks) {
			Location loc = d.getLocation();

			// Find other available BlockDescs here
			BlockDesc replacement = Util.FilterLocation(db.find(BlockDesc.class).where(), loc)
				.ne("lord", this)
				.eq("lord.on", true).findIterate().next();
			
			if (replacement == null) {
			} else {
				replacement.apply(true);
			}
		}
	}

	public void turnOn() {
		for (BlockDesc b : placedBlocks) {
			Location loc = b.getLocation();
			Block cur = loc.getBlock();
			boolean applyPhysics = true;
			if (protectedLevel == Protection.PROTECTED) {
				if ((cur.getType().equals(Material.SAND))
						|| (cur.getType().equals(Material.GRAVEL))
						|| (cur.getType().equals(Material.TORCH))
						|| (cur.getType().equals(Material.REDSTONE_TORCH_OFF))
						|| (cur.getType().equals(Material.REDSTONE_TORCH_ON))
						|| (cur.getType().equals(Material.RAILS))
						|| (cur.getType().equals(Material.LADDER))
						|| (cur.getType().equals(Material.GRAVEL))
						|| (cur.getType().equals(Material.POWERED_RAIL))
						|| (cur.getType().equals(Material.DETECTOR_RAIL))) {
					applyPhysics = false;
				}
			}
			b.apply(applyPhysics);
		}
		on = true;
	}

	public void turnOn(Location l) {
		BlockDesc b = findBlocks(l).findUnique();
		b.apply(true);
	}

}