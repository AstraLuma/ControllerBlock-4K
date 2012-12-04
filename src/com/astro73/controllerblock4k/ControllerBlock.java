package com.astro73.controllerblock4k;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import com.sk89q.worldedit.BlockVector;
import com.sk89q.worldedit.IncompleteRegionException;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.bukkit.BukkitWorld;
import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import com.sk89q.worldedit.regions.Region;

public class ControllerBlock extends JavaPlugin implements Runnable {
	private PermissionHandler permissionHandler = new PermissionHandler(this);
	private final CBlockListener blockListener = new CBlockListener(this);
	private final CRedstoneListener redstoneListener = new CRedstoneListener(
			this);
	private final CPlayerListener playerListener = new CPlayerListener(this);
	private final CBlockRedstoneCheck checkRunner = new CBlockRedstoneCheck(
			this);

	public boolean blockPhysicsEditCheck = false;
	private boolean beenEnabled = false;

	public HashMap<Player, CBlock> map = new HashMap<Player, CBlock>();

	public List<CBlock> blocks = new ArrayList<CBlock>();

	HashMap<String, CBlock> movingCBlock = new HashMap<String, CBlock>();
	HashMap<String, Location> moveHere = new HashMap<String, Location>();

	public void onDisable() {
		getServer().getScheduler().cancelTasks(this);
	}

	public void onLoad() {
		saveDefaultConfig();
		getLogger().info(getDescription().getVersion()
				+ " by Zero9195 (Original by Hell_Fire),"
				+ " Updated for R6 by Sorklin,"
				+ " Edited for WorldEdit by Techzune,"
				+ " Edited for Four Kingdoms by astronouth7303");
	}

	public void onEnable() {
		if (!beenEnabled) {
			getServer().getPluginManager().registerEvents(blockListener, this);
			getServer().getPluginManager().registerEvents(playerListener, this);
			if (getServer().getScheduler().scheduleSyncRepeatingTask(this,
					blockListener, 1L, 1L) == -1) {
				getLogger().warning("Scheduling BlockListener anti-dupe check failed, falling back to old BLOCK_PHYSICS event");
				blockPhysicsEditCheck = true;
			}
			FileConfiguration config = getConfig();
			if (config.getBoolean("DisableEditDupeProtection")) {
				getLogger().warning("Edit dupe protection has been disabled, you're on your own from here");
			}
			if (!config.getBoolean("QuickRedstoneCheck")) {
				if (getServer().getScheduler().scheduleSyncRepeatingTask(this, checkRunner, 1L, 1L) == -1) {
					getLogger().warning("Scheduling CBlockRedstoneCheck task failed, falling back to quick REDSTONE_CHANGE event");
					config.set("QuickRedstoneCheck", true);
				}
			}
			if (config.getBoolean("QuickRedstoneCheck")) {
				getServer().getPluginManager().registerEvents(redstoneListener, this);
			}
			if (getServer().getScheduler().scheduleSyncDelayedTask(this, this,
					1L) == -1) {
				getLogger().severe("Failed to schedule loadData, loading now");
				loadData();
			}
			getLogger().info("Events registered");
			beenEnabled = true;
		}
	}

	public boolean onCommand(CommandSender sender, Command command,
			String label, String[] args) {
		Player player = null;
		if ((sender instanceof Player)) {
			player = (Player) sender;
		}
		if (label.equals("cblock")) {
			if (args[0].equalsIgnoreCase("help")) {
				if (args.length == 1) {
					sender.sendMessage("Commands: reload, add, REMOVE");
				} else {
					if (args[1].equalsIgnoreCase("add")
							|| args[1].equalsIgnoreCase("we")
							|| args[1].equalsIgnoreCase("a")) {
						sender.sendMessage(new String[] {
								"Aliases: add, we, a", "Usage: /cblock add",
								"Adds the current WorldEdit selection to the current ControllerBlock." });
					} else if (args[1].equalsIgnoreCase("REMOVE")
							|| args[1].equalsIgnoreCase("s")) {
						sender.sendMessage(new String[] { "Aliases: REMOVE, s",
								"Usage: /cblock REMOVE",
								"Removes the current WorldEdit selection from the current ControllerBlock." });
					} else if (args[1].equals("reload")) {
						sender.sendMessage(new String[] {
								"Usage: /cblock reload",
								"Reloads the ControllerBlock-4K configuration file." });
					}
				}
			} else if (args[0].equals("reload")) {
				reloadConfig();
				//TODO: Reload data
				getLogger().info("Config reloaded");
				sender.sendMessage("Config reloaded");
			} else if (args[0].equalsIgnoreCase("add")
					|| args[0].equalsIgnoreCase("we")
					|| args[0].equalsIgnoreCase("a")) {
				if (player == null) {
					sender.sendMessage("Must be a player");
					return false;
				}
				CBlock conBlock = map.get(player);
				if (!this.getPerm().canModify(player, conBlock)) {
					player.sendMessage("You're not allowed to modify this ControllerBlock");
					return false;
				}
				WorldEditPlugin wep = getwe();
				LocalSession ses = wep.getSession(player);
				World world = ((BukkitWorld)ses.getSelectionWorld()).getWorld();
				Region reg;
				try {
					reg = ses.getSelection(ses.getSelectionWorld());
				} catch (IncompleteRegionException e) {
					Util.sendError(sender,
							"Incomplete region. Finish your selection first.");
					return false;
				}
				if (reg == null) {
					player.sendMessage("No region found");
					return false;
				}
				int affected = 0;
				for (BlockVector bv : reg) {
					Location loc = new Location(world, bv.getX(), bv.getY(), bv.getZ());
					Block dablock = loc.getBlock();
					if (!conBlock.hasBlock(loc)) {
						if (conBlock.addBlock(dablock)) {
							affected++;
						}
					}
				}
				sender.sendMessage(affected
						+ " blocks added to ControllerBlock");
			} else if (args[0].equalsIgnoreCase("REMOVE")
					|| args[0].equalsIgnoreCase("s")) {
				if (player == null) {
					sender.sendMessage("Must be a player");
					return false;
				}
				CBlock conBlock = map.get(player);
				if (!this.getPerm().canModify(player, conBlock)) {
					player.sendMessage("You're not allowed to modify this ControllerBlock");
					return false;
				}
				WorldEditPlugin wep = getwe();
				LocalSession ses = wep.getSession(player);
				World world = ((BukkitWorld)ses.getSelectionWorld()).getWorld();
				Region reg;
				try {
					reg = ses.getSelection(ses.getSelectionWorld());
				} catch (IncompleteRegionException e) {
					Util.sendError(sender,
							"Incomplete region. Finish your selection first.");
					return false;
				}
				if (reg == null) {
					player.sendMessage("No region found");
					return false;
				}
				int affected = 0;
				for (BlockVector bv : reg) {
					Location loc = new Location(world, bv.getX(), bv.getY(), bv.getZ());
					Block dablock = loc.getBlock();
					player.sendMessage(String.valueOf(dablock.getData()));
					if (!conBlock.hasBlock(loc)) {
						if (conBlock.delBlock(dablock)) {
							affected++;
						}
					}
				}
				sender.sendMessage(affected
						+ " blocks removed from ControllerBlock");
			} else {
				sender.sendMessage("Unknown subcommand to cblock. Try /cblock help");
			}
		}

		return true;
	}

	public PermissionHandler getPerm() {
		return permissionHandler;
	}

	public CBlock createCBlock(Location l, String o, CBlock.Protection pl) {
		CBlock c = new CBlock(this, l, o, pl);
		blocks.add(c);
		return c;
	}

	private WorldEditPlugin getwe() {
		Plugin plugin = getServer().getPluginManager().getPlugin("WorldEdit");
		return (WorldEditPlugin) plugin;
	}

	public CBlock destroyCBlock(Location l, boolean drops) {
		CBlock block = getCBlock(l);
		if (block == null) {
			return block;
		}
		if (drops) {
			block.destroy();
		} else {
			block.destroyWithOutDrops();
		}
		blocks.remove(block);
		deleteData(l);
		return block;
	}

	public CBlock destroyCBlock(Location l) {
		CBlock block = getCBlock(l);
		if (block == null) {
			return block;
		}
		block.destroy();
		blocks.remove(block);
		deleteData(l);
		return block;
	}

	public CBlock getCBlock(Location l) {
		for (Iterator<CBlock> i = blocks.iterator(); i.hasNext();) {
			CBlock block = i.next();
			if (Util.locEquals(block.getLoc(), l)) {
				return block;
			}
		}
		return null;
	}

	public boolean isControlBlock(Location l) {
		return getCBlock(l) != null;
	}

	public boolean isControlledBlock(Location l) {
		return getControllerBlockFor(l) != null;
	}

	public CBlock getControllerBlockFor(Location l) {
		return getControllerBlockFor(null, l, null);
	}
	
	/**
	 * Queries for the given controller block
	 * @param c Exclude this block, or null
	 * @param l Location of the block
	 * @param o Is it on, or null if you don't care
	 * @return
	 */
	public CBlock getControllerBlockFor(CBlock c, Location l, Boolean o) {
		for (Iterator<CBlock> i = blocks.iterator(); i.hasNext();) {
			CBlock block = i.next();

			if ((c != block)
					&& ((o == null) || (o.equals(block.isOn())))
					&& (block.hasBlock(l))) {
				return block;
			}
		}
		return null;
	}

	public CBlock moveControllerBlock(CBlock c, Location l) {
		Iterator<BlockDesc> oldBlockDescs = c.iterator();
		CBlock newC = createCBlock(l, c.getOwner(), c.protectedLevel);
		if (c.isOn()) {
			while (oldBlockDescs.hasNext()) {
				newC.addBlock(oldBlockDescs.next().loc.getBlock());
			}
			destroyCBlock(c.getLoc(), false);
			return newC;
		}
		return null;
	}

	public CBlockStore getStore(CBlockStore store) {
		if (store == null) {
			store = getStore();
		}
		return store;
	}

	public CBlockStore getStore() {
		try {
			return new CBlockStore(getConfig());
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			getLogger().throwing("CBlockStore", "<constructor>", e);
			return null;
		}
	}

	public void loadData() {
		int i = 0;
		CBlockStore store = getStore();
		for (CBlock lord : store.loadAllLords(this)) {
			this.blocks.add(lord);
			i++;
		}
		getLogger().info("Loaded SQL data - " + i + " ControllerBlocks loaded");
	}

	public void saveData(CBlockStore store, CBlock cblock) {
		getLogger().fine("Saving ControllerBlock data");
		store = getStore(store);
		cblock.serialize(store);
	}

	public void deleteData(Location l) {
		CBlockStore store = getStore();
		boolean success = store.removeLord(l);
		if (!success) {
			getLogger().warning("Error when attempting to delete block: "
					+ l.toString());
		} else {

		}
	}

	public Material getCBlockType() {
		return Util.getMaterial(getConfig(), "ControllerBlockType");
	}

	public Material getSemiProtectedCBlockType() {
		return Util.getMaterial(getConfig(), "SemiProtectedControllerBlockType");
	}

	public Material getUnProtectedCBlockType() {
		return Util.getMaterial(getConfig(), "UnProtectedControllerBlockType");
	}

	public boolean isValidMaterial(Material m) {
		if (!m.isBlock()) {
			return false;
		}
		
		for (String type : getConfig().getStringList("disallowed")) {
			Material i = Util.getMaterial(type);
			if (i.equals(m)) {
				return false;
			}
		}
		return true;
	}

	public boolean isUnprotectedMaterial(Material m) {
		if (!m.isBlock()) {
			return false;
		}
		
		for (String type : getConfig().getStringList("unprotected")) {
			Material i = Util.getMaterial(type);
			if (i.equals(m)) {
				return true;
			}
		}
		return false;
	}

	public void run() {
		loadData();
	}

	public void removeCBlock(CBlock cb, long id) {
		blocks.remove(cb);
		getStore().removeLord(id);
	}
}