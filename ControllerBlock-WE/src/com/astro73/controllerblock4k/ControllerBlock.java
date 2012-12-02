package com.astro73.controllerblock4k;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
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
	private static String configFile = "ControllerBlock.ini";
	private Config config = new Config();
	private PermissionHandler permissionHandler = new PermissionHandler(this);
	private final CBlockListener blockListener = new CBlockListener(this);
	private final CRedstoneListener redstoneListener = new CRedstoneListener(
			this);
	private final CPlayerListener playerListener = new CPlayerListener(this);
	private final CBlockRedstoneCheck checkRunner = new CBlockRedstoneCheck(
			this);

	public boolean blockPhysicsEditCheck = false;
	private boolean beenLoaded = false;
	private boolean beenEnabled = false;

	public HashMap<Player, CBlock> map = new HashMap<Player, CBlock>();

	public List<CBlock> blocks = new ArrayList<CBlock>();

	HashMap<String, CBlock> movingCBlock = new HashMap<String, CBlock>();
	HashMap<String, Location> moveHere = new HashMap<String, Location>();
	private Material CBlockType;
	private Material semiProtectedCBlockType;
	private Material unProtectedCBlockType;
	private List<Material> DisallowedTypesAll = new ArrayList<Material>();
	private List<Material> UnprotectedBlocks = new ArrayList<Material>();

	public void onDisable() {
		getServer().getScheduler().cancelTasks(this);
	}

	public void onLoad() {
		if (!beenLoaded) {
			getLogger().info(getDescription().getVersion()
					+ " by Zero9195 (Original by Hell_Fire),"
					+ " Updated for R6 by Sorklin,"
					+ " Edited for WorldEdit by Techzune,"
					+ " Edited for Four Kingdoms by astronouth7303");
			loadConfig();
			beenLoaded = true;
		}
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
			if (config.getBool(Config.Option.DisableEditDupeProtection)) {
				getLogger().warning("Edit dupe protection has been disabled, you're on your own from here");
			}
			if (!config.getBool(Config.Option.QuickRedstoneCheck)) {
				if (getServer().getScheduler().scheduleSyncRepeatingTask(this,
						checkRunner, 1L, 1L) == -1) {
					getLogger().warning("Scheduling CBlockRedstoneCheck task failed, falling back to quick REDSTONE_CHANGE event");
					config.setOpt(Config.Option.QuickRedstoneCheck,
							Boolean.valueOf(true));
				}
			}
			if (config.getBool(Config.Option.QuickRedstoneCheck)) {
				getServer().getPluginManager().registerEvents(redstoneListener,
						this);
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
					sender.sendMessage("Commands: reload, add, remove");
				} else {
					if (args[1].equalsIgnoreCase("add")
							|| args[1].equalsIgnoreCase("we")
							|| args[1].equalsIgnoreCase("a")) {
						sender.sendMessage(new String[] {
								"Aliases: add, we, a", "Usage: /cblock add",
								"Adds the current WorldEdit selection to the current ControllerBlock." });
					} else if (args[1].equalsIgnoreCase("remove")
							|| args[1].equalsIgnoreCase("s")) {
						sender.sendMessage(new String[] { "Aliases: remove, s",
								"Usage: /cblock remove",
								"Removes the current WorldEdit selection from the current ControllerBlock." });
					} else if (args[1].equals("reload")) {
						sender.sendMessage(new String[] {
								"Usage: /cblock reload",
								"Reloads the ControllerBlock-4K configuration file." });
					}
				}
			} else if (args[0].equals("reload")) {
				loadConfig();
				System.out.println("Config reloaded");
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
			} else if (args[0].equalsIgnoreCase("remove")
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

	public Config getConfigu() {
		return config;
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
		return getControllerBlockFor(null, l, null, null) != null;
	}

	public boolean isControlledBlock(Location l, Material m) {
		return getControllerBlockFor(null, l, m, null) != null;
	}

	public CBlock getControllerBlockFor(CBlock c, Location l, Material m,
			Boolean o) {
		for (Iterator<CBlock> i = blocks.iterator(); i.hasNext();) {
			CBlock block = i.next();

			if ((c != block)
					/*&& ((m == null) || (m.equals(block.getType())))*/
					&& ((o == null) || (o.equals(Boolean.valueOf(block.isOn()))))
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
			return new CBlockStore(this.config);
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
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
		return CBlockType;
	}

	public Material getSemiProtectedCBlockType() {
		return semiProtectedCBlockType;
	}

	public Material getUnProtectedCBlockType() {
		return unProtectedCBlockType;
	}

	public boolean isValidMaterial(Material m) {
		if (!m.isBlock()) {
			return false;
		}
		Iterator<Material> i = DisallowedTypesAll.iterator();
		while (i.hasNext()) {
			if (i.next().equals(m)) {
				return false;
			}
		}
		return true;
	}

	public boolean isUnprotectedMaterial(Material m) {
		if (!m.isBlock()) {
			return false;
		}
		Iterator<Material> i = UnprotectedBlocks.iterator();
		while (i.hasNext()) {
			if (i.next().equals(m)) {
				return true;
			}
		}
		return false;
	}

	private void loadError(String cmd, String arg, Integer line, String def) {
		if (def.length() != 0) {
			def = "defaulting to " + def;
		} else {
			def = "it has been skipped";
		}
		getLogger().warning("Couldn't parse " + cmd + " " + arg + " on line " + line
				+ ", " + def);
	}

	private void loadConfig() {
		Integer oldConfigLine = Integer.valueOf(-1);
		Integer l = Integer.valueOf(0);
		ConfigSections c = ConfigSections.oldConfig;
		List<String> configText = new ArrayList<String>();
		try {
			BufferedReader in = new BufferedReader(new InputStreamReader(
					new FileInputStream(getDataFolder() + "/" + configFile),
					"UTF-8"));
			String s;
			while ((s = in.readLine()) != null) {
				configText.add(s.trim());
				l = Integer.valueOf(l.intValue() + 1);
				if ((s.trim().isEmpty()) || (s.startsWith("#"))) {
					continue;
				}
				if (s.toLowerCase().trim().equals("[general]")) {
					c = ConfigSections.general;
				} else if (s.toLowerCase().trim().equals("[adminplayers]")) {
					c = ConfigSections.adminPlayers;
				} else if (s.toLowerCase().trim().equals("[disallowed]")) {
					c = ConfigSections.disallowedAll;
				} else if (s.toLowerCase().trim().equals("[unprotected]")) {
					c = ConfigSections.unprotectedBlocks;
				} else if (c.equals(ConfigSections.general)) {
					String[] line = s.split("=", 2);
					if (line.length >= 2) {
						String cmd = line[0].toLowerCase();
						String arg = line[1];
						if (cmd.equals("ControllerBlockType".toLowerCase())) {
							CBlockType = Material.getMaterial(arg);
							if (CBlockType == null) {
								loadError("ControllerBlockType", arg, l,
										"IRON_BLOCK");
								CBlockType = Material.IRON_BLOCK;
							}
							config.setOpt(Config.Option.ControllerBlockType,
									CBlockType);
						} else if (cmd
								.equals("SemiProtectedControllerBlockType"
										.toLowerCase())) {
							semiProtectedCBlockType = Material.getMaterial(arg);
							if (semiProtectedCBlockType == null) {
								loadError("SemiProtectedControllerBlockType",
										arg, l, "GOLD_BLOCK");
								semiProtectedCBlockType = Material.GOLD_BLOCK;
							}
							config.setOpt(
									Config.Option.SemiProtectedControllerBlockType,
									semiProtectedCBlockType);
						} else if (cmd.equals("UnProtectedControllerBlockType"
								.toLowerCase())) {
							unProtectedCBlockType = Material.getMaterial(arg);
							if (unProtectedCBlockType == null) {
								loadError("UnProtectedControllerBlockType",
										arg, l, "DIAMOND_BLOCK");
								unProtectedCBlockType = Material.DIAMOND_BLOCK;
							}
							config.setOpt(
									Config.Option.UnProtectedControllerBlockType,
									unProtectedCBlockType);
						} else if (cmd.equals("QuickRedstoneCheck"
								.toLowerCase())) {
							config.setOpt(Config.Option.QuickRedstoneCheck,
									Boolean.valueOf(Boolean.parseBoolean(arg)));
						} else if (cmd.equals("BlockProtectMode".toLowerCase())) {
							config.setOpt(Config.Option.BlockProtectMode,
									BlockProtectMode.valueOf(arg.toLowerCase()));
						} else if (cmd.equals("BlockEditProtectMode"
								.toLowerCase())) {
							config.setOpt(Config.Option.BlockEditProtectMode,
									BlockProtectMode.valueOf(arg.toLowerCase()));
						} else if (cmd.equals("BlockPhysicsProtectMode"
								.toLowerCase())) {
							config.setOpt(
									Config.Option.BlockPhysicsProtectMode,
									BlockProtectMode.valueOf(arg.toLowerCase()));
						} else if (cmd.equals("BlockFlowProtectMode"
								.toLowerCase())) {
							config.setOpt(Config.Option.BlockFlowProtectMode,
									BlockProtectMode.valueOf(arg.toLowerCase()));
						} else if (cmd.equals("DisableEditDupeProtection"
								.toLowerCase())) {
							config.setOpt(
									Config.Option.DisableEditDupeProtection,
									Boolean.valueOf(Boolean.parseBoolean(arg)));
						} else if (cmd.equals("PistonProtection".toLowerCase())) {
							config.setOpt(Config.Option.PistonProtection,
									Boolean.valueOf(Boolean.parseBoolean(arg)));
						} else if (cmd.equals("MaxBlocksPerController"
								.toLowerCase())) {
							config.setOpt(Config.Option.MaxBlocksPerController,
									Integer.valueOf(Integer.parseInt(arg)));
						} else if (cmd.equals("MaxDistanceFromController"
								.toLowerCase())) {
							config.setOpt(
									Config.Option.MaxDistanceFromController,
									Integer.valueOf(Integer.parseInt(arg)));
						} else if (cmd.equals("DisableNijikokunPermissions"
								.toLowerCase())) {
							config.setOpt(
									Config.Option.DisableNijikokunPermissions,
									Boolean.valueOf(Boolean.parseBoolean(arg)));
						} else if (cmd.equals("ServerOpIsAdmin".toLowerCase())) {
							config.setOpt(Config.Option.ServerOpIsAdmin,
									Boolean.valueOf(Boolean.parseBoolean(arg)));
						} else if (cmd.equals("AnyoneCanCreate".toLowerCase())) {
							config.setOpt(Config.Option.AnyoneCanCreate,
									Boolean.valueOf(Boolean.parseBoolean(arg)));
						} else if (cmd.equals("AnyoneCanModifyOther"
								.toLowerCase())) {
							config.setOpt(Config.Option.AnyoneCanModifyOther,
									Boolean.valueOf(Boolean.parseBoolean(arg)));
						} else if (cmd.equals("AnyoneCanDestroyOther"
								.toLowerCase())) {
							config.setOpt(Config.Option.AnyoneCanDestroyOther,
									Boolean.valueOf(Boolean.parseBoolean(arg)));
						} else if (cmd.equals("SqlConnection".toLowerCase())) {
							config.setOpt(Config.Option.SqlConnection, arg);
						}
					}
				} else if (c.equals(ConfigSections.adminPlayers)) {
					permissionHandler.addBuiltinAdminPlayer(s.trim());
				} else if (c.equals(ConfigSections.disallowedAll)) {
					Material m = Material.getMaterial(s.trim());
					if (m == null) {
						loadError("disallowed type", s.trim(), l, "");
					} else {
						DisallowedTypesAll.add(m);
					}
				} else if (c.equals(ConfigSections.unprotectedBlocks)) {
					Material m = Material.getMaterial(s.trim());
					if (m == null) {
						loadError("disallowed type", s.trim(), l, "");
					} else {
						UnprotectedBlocks.add(m);
					}

				} else if (c.equals(ConfigSections.oldConfig)) {
					if (oldConfigLine.intValue() == -1) {
						CBlockType = Material.getMaterial(s.trim());
						if (CBlockType == null) {
							getLogger().warning("Couldn't parse ControllerBlock type "
									+ s.trim() + ", defaulting to IRON_BLOCK");
							CBlockType = Material.IRON_BLOCK;
						}
						config.setOpt(Config.Option.ControllerBlockType,
								CBlockType);
						oldConfigLine = Integer.valueOf(oldConfigLine
								.intValue() + 1);
					} else {
						Material m = Material.getMaterial(s.trim());
						if (m == null) {
							getLogger().warning("Couldn't parse disallowed type "
									+ s.trim() + ", it has been skipped");
						} else {
							DisallowedTypesAll.add(m);
							oldConfigLine = Integer.valueOf(oldConfigLine
									.intValue() + 1);
						}
					}
				}
			}
			writeConfig(configText);
			in.close();
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			getLogger().warning("No config found, using defaults, writing defaults out to "
					+ configFile);
			writeConfig(null);
		} catch (IOException e) {
			e.printStackTrace();
		}
		CBlockType = (Material) config
				.getOpt(Config.Option.ControllerBlockType);
		getLogger().info("Using " + CBlockType + " (" + CBlockType.getId()
				+ ") as ControllerBlock, loaded " + DisallowedTypesAll.size()
				+ " disallowed types from config");
	}

	private String writePatch(ConfigSections c) {
		String dump = "";
		if (c == null) {
			return dump;
		}
		if (c.equals(ConfigSections.general)) {
			if (!config.hasOption(Config.Option.ControllerBlockType)) {
				dump = dump + "\n";
				dump = dump
						+ "# ControllerBlockType is the material allowed of new ControllerBlocks\n";
				dump = dump
						+ "# Doesn't affect already assigned ControllerBlocks\n";
				dump = dump + "ControllerBlockType="
						+ config.getOpt(Config.Option.ControllerBlockType)
						+ "\n";
			}
			if (!config
					.hasOption(Config.Option.SemiProtectedControllerBlockType)) {
				dump = dump + "\n";
				dump = dump
						+ "# SemiProtectedControllerBlockType is the material that semi-protected\n";
				dump = dump
						+ "# Controller Blocks are made from, this block will turn on in a protected\n";
				dump = dump
						+ "# state, but when turned off, blocks controlled won't disappear, instead\n";
				dump = dump
						+ "# they lose their protection and can be destroyed\n";
				dump = dump
						+ "SemiProtectedControllerBlockType="
						+ config.getOpt(Config.Option.SemiProtectedControllerBlockType)
						+ "\n";
			}
			if (!config.hasOption(Config.Option.UnProtectedControllerBlockType)) {
				dump = dump + "\n";
				dump = dump
						+ "# UnProtectedControllerBlockType is the material that unprotected\n";
				dump = dump
						+ "# Controller Blocks are made from, blocks controlled by this will create\n";
				dump = dump
						+ "# when turned on, but won't disappear when turned off, much like the\n";
				dump = dump
						+ "# semi-protected controlled blocks, however, blocks controlled have no\n";
				dump = dump
						+ "# protection against being broken even in the on state\n";
				dump = dump
						+ "UnProtectedControllerBlockType="
						+ config.getOpt(Config.Option.UnProtectedControllerBlockType)
						+ "\n";
			}
			if (!config.hasOption(Config.Option.QuickRedstoneCheck)) {
				dump = dump + "\n";
				dump = dump
						+ "# QuickRedstoneCheck to false enables per-tick per-controllerblock isBlockPowered() checks\n";
				dump = dump
						+ "# This is potentially laggier, but blocks can be powered like regular redstone blocks\n";
				dump = dump
						+ "# If set to true, wire needs to be run on top of the controller block\n";
				dump = dump + "QuickRedstoneCheck="
						+ config.getOpt(Config.Option.QuickRedstoneCheck)
						+ "\n";
			}
			if (!config.hasOption(Config.Option.BlockProtectMode)) {
				dump = dump + "\n";
				dump = dump
						+ "# BlockProtectMode changes how we handle destroying controlled blocks\n";
				dump = dump + "# It has 3 modes:\n";
				dump = dump
						+ "# protect - default, tries to prevent controlled blocks from being destroyed\n";
				dump = dump
						+ "# remove - removes controlled blocks from controller if destroyed\n";
				dump = dump
						+ "# none - don't do anything, this effectively makes controlled blocks dupable\n";
				dump = dump + "BlockProtectMode="
						+ config.getOpt(Config.Option.BlockProtectMode) + "\n";
			}
			if (!config.hasOption(Config.Option.BlockPhysicsProtectMode)) {
				dump = dump + "\n";
				dump = dump
						+ "# BlockPhysicsProtectMode changes how we handle changes against controlled blocks\n";
				dump = dump + "# It has 3 modes:\n";
				dump = dump
						+ "# protect - default, stops physics interactions with controlled blocks\n";
				dump = dump
						+ "# remove - removes controlled blocks from controller if changed\n";
				dump = dump
						+ "# none - don't do anything, could have issues with some blocks\n";
				dump = dump + "BlockPhysicsProtectMode="
						+ config.getOpt(Config.Option.BlockPhysicsProtectMode)
						+ "\n";
			}
			if (!config.hasOption(Config.Option.BlockFlowProtectMode)) {
				dump = dump + "\n";
				dump = dump
						+ "# BlockFlowProtectMode changes how we handle water/lava flowing against controlled blocks\n";
				dump = dump + "# It has 3 modes:\n";
				dump = dump
						+ "# protect - default, tries to prevent controlled blocks from being interacted\n";
				dump = dump
						+ "# remove - removes controlled blocks from controller if flow event on it\n";
				dump = dump
						+ "# none - don't do anything, things that drop when flowed over can be dupable\n";
				dump = dump + "BlockFlowProtectMode="
						+ config.getOpt(Config.Option.BlockFlowProtectMode)
						+ "\n";
			}
			if (!config.hasOption(Config.Option.DisableEditDupeProtection)) {
				dump = dump + "\n";
				dump = dump
						+ "# DisableEditDupeProtection set to true disables all the checks for changes while in\n";
				dump = dump
						+ "# edit mode, this will make sure blocks placed in a spot will always be in that spot\n";
				dump = dump
						+ "# even if they get removed by some kind of physics/flow event in the meantime\n";
				dump = dump
						+ "DisableEditDupeProtection="
						+ config.getOpt(Config.Option.DisableEditDupeProtection)
						+ "\n";
			}
			if (!config.hasOption(Config.Option.PistonProtection)) {
				dump = dump + "\n";
				dump = dump
						+ "# PistonProtection set to true disables the ability of Pistons to move\n";
				dump = dump + "# ControllerBlocks or controlled Blocks.\n";
				dump = dump + "PistonProtection="
						+ config.getOpt(Config.Option.PistonProtection) + "\n";
			}
			if (!config.hasOption(Config.Option.MaxDistanceFromController)) {
				dump = dump + "\n";
				dump = dump
						+ "# MaxDistanceFromController sets how far away controlled blocks are allowed\n";
				dump = dump
						+ "# to be attached and controlled to a controller block - 0 for infinte/across worlds\n";
				dump = dump
						+ "MaxDistanceFromController="
						+ config.getOpt(Config.Option.MaxDistanceFromController)
						+ "\n";
			}
			if (!config.hasOption(Config.Option.MaxBlocksPerController)) {
				dump = dump + "\n";
				dump = dump
						+ "# MaxControlledBlocksPerController sets how many blocks are allowed to be attached\n";
				dump = dump
						+ "# to a single controller block - 0 for infinite\n";
				dump = dump + "MaxBlocksPerController="
						+ config.getOpt(Config.Option.MaxBlocksPerController)
						+ "\n";
			}
			if (!config.hasOption(Config.Option.DisableNijikokunPermissions)) {
				dump = dump + "\n";
				dump = dump + "# Nijikokun Permissions support\n";
				dump = dump + "# The nodes for permissions are:\n";
				dump = dump
						+ "# controllerblock.admin - user isn't restricted by block counts or distance, able to\n";
				dump = dump
						+ "#                         create/modify/destroy other users controllerblocks\n";
				dump = dump
						+ "# controllerblock.create - user is allowed to setup controllerblocks\n";
				dump = dump
						+ "# controllerblock.modifyOther - user is allowed to modify other users controllerblocks\n";
				dump = dump
						+ "# controllerblock.destroyOther - user is allowed to destroy other users controllerblocks\n";
				dump = dump + "#\n";
				dump = dump
						+ "# DisableNijikokunPermissions will disable any lookups against Permissions if you\n";
				dump = dump
						+ "# do have it installed, but want to disable this plugins use of it anyway\n";
				dump = dump
						+ "# Note: You don't have to do this, the plugin isn't dependant on Permissions\n";
				dump = dump
						+ "DisableNijikokunPermissions="
						+ config.getOpt(Config.Option.DisableNijikokunPermissions)
						+ "\n";
			}
			if (!config.hasOption(Config.Option.ServerOpIsAdmin)) {
				dump = dump + "\n";
				dump = dump
						+ "# Users listed in ops.txt (op through server console) counts as an admin\n";
				dump = dump + "ServerOpIsAdmin="
						+ config.getOpt(Config.Option.ServerOpIsAdmin) + "\n";
			}
			if (!config.hasOption(Config.Option.AnyoneCanCreate)) {
				dump = dump + "\n";
				dump = dump
						+ "# Everyone on the server can create new ControllerBlocks\n";
				dump = dump + "AnyoneCanCreate="
						+ config.getOpt(Config.Option.AnyoneCanCreate) + "\n";
			}
			if (!config.hasOption(Config.Option.AnyoneCanModifyOther)) {
				dump = dump + "\n";
				dump = dump
						+ "# Everyone can modify everyone elses ControllerBlocks\n";
				dump = dump + "AnyoneCanModifyOther="
						+ config.getOpt(Config.Option.AnyoneCanModifyOther)
						+ "\n";
			}
			if (!config.hasOption(Config.Option.AnyoneCanDestroyOther)) {
				dump = dump + "\n";
				dump = dump
						+ "# Everyone can destroy everyone elses ControllerBlocks\n";
				dump = dump + "AnyoneCanDestroyOther="
						+ config.getOpt(Config.Option.AnyoneCanDestroyOther)
						+ "\n";
			}
			if (!config.hasOption(Config.Option.SqlConnection)) {
				dump = dump + "\n";
				dump = dump
						+ "# The JDBC URL to the database\n"
						+ "# For MySQL, see http://dev.mysql.com/doc/refman/5.0/en/connector-j-reference-configuration-properties.html for the format\n";
				dump = dump + "SqlConnection="
						+ config.getOpt(Config.Option.SqlConnection) + "\n";
			}
		}
		if (dump.length() != 0) {
			dump = dump + "\n";
		}
		return dump;
	}

	private void writeConfig(List<String> prevConfig) {
		String dump = "";
		if (prevConfig == null) {
			dump = "# ControllerBlock configuration file\n";
			dump = dump + "\n";
			dump = dump
					+ "# Blank lines and lines starting with # are ignored\n";
			dump = dump
					+ "# Material names can be found: http://javadoc.lukegb.com/Bukkit/d7/dd9/namespaceorg_1_1bukkit.html#ab7fa290bb19b9a830362aa88028ec80a\n";
			dump = dump + "\n";
		}
		boolean hasGeneral = false;
		boolean hasAdminPlayers = false;
		boolean hasDisallowed = false;
		ConfigSections c = null;

		if (prevConfig != null) {
			Iterator<String> pci = prevConfig.listIterator();
			while (pci.hasNext()) {
				String line = pci.next();
				if (line.toLowerCase().trim().equals("[general]")) {
					dump = dump + writePatch(c);
					c = ConfigSections.general;
					hasGeneral = true;
				} else if (line.toLowerCase().trim().equals("[adminplayers]")) {
					dump = dump + writePatch(c);
					c = ConfigSections.adminPlayers;
					hasAdminPlayers = true;
				} else if (line.toLowerCase().trim().equals("[disallowed]")) {
					dump = dump + writePatch(c);
					c = ConfigSections.disallowedAll;
					hasDisallowed = true;
				}
				dump = dump + line + "\n";
			}
			pci = null;
			dump = dump + writePatch(c);
		}

		if (!hasGeneral) {
			dump = dump + "[general]\n";
			dump = dump + writePatch(ConfigSections.general);
			dump = dump + "\n";
		}
		if (!hasAdminPlayers) {
			dump = dump + "[adminPlayers]\n";
			dump = dump
					+ "# One name per line, users listed here are admins, and can\n";
			dump = dump
					+ "# create/modify/destroy all ControllerBlocks on the server\n";
			dump = dump + "# Block restrictions don't apply to admins\n";
			dump = dump + "\n";
		}
		if (!hasDisallowed) {
			dump = dump + "[disallowed]\n";
			dump = dump
					+ "# Add disallowed blocks here, one Material per line.\n";
			dump = dump
					+ "# Item IDs higher than 255 are excluded automatically due to failing Material.isBlock() check\n";
			dump = dump
					+ "#RED_ROSE\n#YELLOW_FLOWER\n#RED_MUSHROOM\n#BROWN_MUSHROOM\n";
			dump = dump + "\n";
			Iterator<Material> i = DisallowedTypesAll.listIterator();
			while (i.hasNext()) {
				dump = dump + i.next() + "\n";
			}
			dump = dump + "[unprotected]\n";
			dump = dump
					+ "# Add unprotected blocks here, one Material per line.\n";
			dump = dump
					+ "# Item IDs higher than 255 are excluded automatically due to failing Material.isBlock() check\n";
			dump = dump
					+ "# These Blocks ARE allowed to be pushed by Pistons and to be used with (semi) unprotected CBlocks.\n";
			dump = dump
					+ "#RED_ROSE\n#YELLOW_FLOWER\n#RED_MUSHROOM\n#BROWN_MUSHROOM\n";
			dump = dump + "\n";
			i = UnprotectedBlocks.listIterator();
			while (i.hasNext()) {
				dump = dump + i.next() + "\n";
			}
		}
		try {
			Writer out = new OutputStreamWriter(new FileOutputStream(
					getDataFolder() + "/" + configFile), "UTF-8");
			out.write(dump);
			out.close();
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void run() {
		loadData();
	}

	public void removeCBlock(CBlock cb, long id) {
		blocks.remove(cb);
		getStore().removeLord(id);
	}
}