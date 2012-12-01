package me.operon.controllerblockwe;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.inventory.ItemStack;
import org.bukkit.material.RedstoneWire;

public class CBlock implements Iterable<BlockDesc> {
	private Location blockLocation = null;
	private Material blockType = null;
	private List<BlockDesc> placedBlocks = new ArrayList<BlockDesc>();
	private String owner = null;

	private ControllerBlock parent = null;
	private boolean on = false;
	private boolean edit = false;
	public Protection protectedLevel = Protection.PROTECTED;
	private long id = 0;
	
	public enum Protection {
		PROTECTED, SEMIPROTECTED, UNPROTECTED
	}
	
	public CBlock(ControllerBlock p, Location l, String o, Protection pl) {
		parent = p;
		blockLocation = l;
		owner = o;
		protectedLevel = pl;
	}

	public ControllerBlock getParent() {
		return parent;
	}

	public String getOwner() {
		return owner;
	}

	public Material getType() {
		return blockType;
	}

	public void setType(Material m) {
		blockType = m;
	}

	public Location getLoc() {
		return blockLocation;
	}

	@Override
	public Iterator<BlockDesc> iterator() {
		return placedBlocks.iterator();
	}
	
	public boolean addBlock(Block b) {
		if (b.getType().equals(blockType)) {
			Location bloc = b.getLocation();
			if (placedBlocks.isEmpty()) {
				placedBlocks
						.add(new BlockDesc(bloc, blockType, Byte.valueOf(b.getData())));
				return true;
			}
			;
			for (ListIterator<BlockDesc> i = placedBlocks.listIterator(); i.hasNext();) {
				BlockDesc loc = i.next();
				if (bloc.getBlockY() > loc.loc.getBlockY()) {
					i.previous();
					i.add(new BlockDesc(bloc, blockType, Byte.valueOf(b.getData())));
					return true;
				}
			}
			placedBlocks.add(new BlockDesc(bloc, blockType, Byte.valueOf(b.getData())));
			return true;
		}

		return false;
	}

	public boolean delBlock(Block b) {
		Location u = b.getLocation();
		for (Iterator<BlockDesc> i = placedBlocks.iterator(); i.hasNext();) {
			Location t = i.next().loc;
			if (t.equals(u)) {
				i.remove();
				CBlock check = parent.getControllerBlockFor(this, u, null,
						Boolean.valueOf(true));
				if (check != null) {
					b.setType(check.blockType);
					b.setData(check.getBlock(u).data);
				}
				return true;
			}
		}
		return false;
	}

	public int numBlocks() {
		return placedBlocks.size();
	}

	public BlockDesc getBlock(Location l) {
		for (BlockDesc d : placedBlocks) {
			if (d.loc.equals(l)) {
				return d;
			}
		}
		return null;
	}

	public boolean hasBlock(Location l) {
		return getBlock(l) != null;
	}

	public void updateBlock(Block b) {
		for (BlockDesc d : placedBlocks) {
			if (d.loc.equals(b.getLocation())) {
				d.data = b.getState().getData().getData();
				return;
			}
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
			parent.saveData(null, this);
			doRedstoneCheck();
		}
	}

	public void destroyWithOutDrops() {
		turnOff();
	}

	public void destroy() {
		turnOff();
		int i = placedBlocks.size();
		int j = 0;
		while (i > 0) {
			if (i > 64) {
				j = 64;
				i -= 64;
			} else {
				j = i;
				i -= i;
			}
			blockLocation.getWorld().dropItemNaturally(blockLocation,
					new ItemStack(blockType, j));
		}
	}

	public boolean isOn() {
		return on;
	}

	public void doRedstoneCheck() {
		Block check = Util.getBlockAtLocation(blockLocation).getRelative(
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
		for (BlockDesc d : placedBlocks) {
			Location loc = d.loc;
			CBlock check = parent.getControllerBlockFor(this, loc, null,
					Boolean.valueOf(true));
			Block cur = loc.getWorld().getBlockAt(loc.getBlockX(),
					loc.getBlockY(), loc.getBlockZ());
			boolean applyPhysics = true;

			if (check != null) {
				cur.setTypeIdAndData(check.blockType.getId(),
						check.getBlock(loc).data, applyPhysics);
			} else if (protectedLevel == Protection.PROTECTED) {
				cur.setType(Material.AIR);
			}
		}
		on = false;
	}

	public void turnOn() {
		for (BlockDesc b : placedBlocks) {
			Location loc = b.loc;
			Block cur = loc.getWorld().getBlockAt(loc.getBlockX(),
					loc.getBlockY(), loc.getBlockZ());
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
			cur.setTypeIdAndData(blockType.getId(), b.data, applyPhysics);
		}
		on = true;
	}

	public void turnOn(Location l) {
		for (BlockDesc b : placedBlocks) {
			if (l.equals(b.loc)) {
				Block cur = Util.getBlockAtLocation(l);
				boolean applyPhysics = true;
				if (protectedLevel == Protection.PROTECTED) {
					applyPhysics = false;
				}
				cur.setTypeIdAndData(blockType.getId(), b.data,
						applyPhysics);
			}
		}
	}

/*	public CBlock(ControllerBlock p, int version, String s) {
		parent = p;
		String[] args = s.split(",");

		if (((version < 3) && (args.length < 4))
				|| ((version >= 3) && (args.length < 5))) {
			parent.log
					.severe("ERROR: Invalid ControllerBlock description in data file, skipping");
			return;
		}

		if (version >= 4) {
			blockLocation = parseLocation(p.getServer(), args[0], args[1],
					args[2], args[3]);
			parent.log.debug("CB Location: "
					+ Util.formatLocation(blockLocation));
		}

		blockType = Material.getMaterial(args[3]);
		int i;
		if (version >= 3) {
			owner = args[5];
			i = 6;
		} else {
			owner = null;
			i = 7;
		}

		protectedLevel = Protection.PROTECTED;
		if (i < args.length) {
			if (args[i].equals("protected")) {
				protectedLevel = Protection.PROTECTED;
				i++;
			}
			if (args[i].equals("semi-protected")) {
				protectedLevel = Protection.SEMIPROTECTED;
				i++;
			} else if (args[i].equals("unprotected")) {
				protectedLevel = Protection.UNPROTECTED;
				i++;
			}
		}

		while (i < args.length) {
			if (version >= 4) {
				if (args.length - i >= 6) {
					placedBlocks.add(new BlockDesc(
							parseLocation(p.getServer(), args[(i++)],
									args[(i++)], args[(i++)], args[(i++)]),
							Byte.valueOf(Byte.parseByte(args[(i++)]))));
				} else {
					parent.log
							.severe("ERROR: Block description in save file is corrupt");
					return;
				}
			}
		}
	}*/

	public void serialize(CBlockStore store) {
		this.id = store.storeLordBlock(this.id, this.blockLocation, this.owner, this.protectedLevel);
		for (BlockDesc b: this.placedBlocks) {
			store.storeSerfBlock(this.id, b);
		}
	}

}