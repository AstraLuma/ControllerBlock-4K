package com.astro73.controllerblock4k;

import org.bukkit.Location;
import org.bukkit.Material;
//import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.Configuration;

public class Util {
	public static double getDistanceBetweenLocations(Location l1, Location l2) {
		if (!l1.getWorld().equals(l2.getWorld())) {
			return -1.0D;
		}
		return Math.sqrt(Math.pow(l1.getX() - l2.getX(), 2.0D)
				+ Math.pow(l1.getY() - l2.getY(), 2.0D)
				+ Math.pow(l1.getZ() - l2.getZ(), 2.0D));
	}

	public static Block getBlockAtLocation(Location l) {
		return getBlockAtLocation(l, 0, 0, 0);
	}

	public static Block getBlockAtLocation(Location l, int x, int y, int z) {
		return l.getWorld().getBlockAt(l.getBlockX() + x,
				l.getBlockY() + y, l.getBlockZ() + z);
	}

	public static String formatBlockCount(CBlock c) {
		int mbpc = c.getParent().getConfig().getInt("MaxBlocksPerController");
		if (mbpc > 0) {
			return "("
					+ c.numBlocks()
					+ "/"
					+ mbpc
					+ " blocks)";
		}
		return "(" + c.numBlocks() + " blocks)";
	}

	public static String formatLocation(Location l) {
		return "<" + l.getWorld().getName() + "," + l.getBlockX() + ","
				+ l.getBlockY() + "," + l.getBlockZ() + ">";
	}

/*	public static boolean typeEquals(Material t1, Material t2) {
		if ((t1.equals(Material.DIRT))
				|| ((t1.equals(Material.GRASS)) && (t2.equals(Material.DIRT)))
				|| (t2.equals(Material.GRASS))) {
			return true;
		}

		if ((t1.equals(Material.REDSTONE_TORCH_ON))
				|| ((t1.equals(Material.REDSTONE_TORCH_OFF)) && (t2
						.equals(Material.REDSTONE_TORCH_ON)))
				|| (t2.equals(Material.REDSTONE_TORCH_OFF))) {
			return true;
		}
		return t1.equals(t2);
	}*/

	public static boolean locEquals(Location l1, Location l2) {
		return (l1.getWorld().getName() == l2.getWorld().getName())
				&& (l1.getBlockX() == l2.getBlockX())
				&& (l1.getBlockY() == l2.getBlockY())
				&& (l1.getBlockZ() == l2.getBlockZ());
	}
	
	public static void sendError(CommandSender player, String msg) {
		player.sendMessage("§c"+msg);
	}
	
	/**
	 * Performs some magic to loosen up input requirements for materials.
	 * Namely, allows for IDs and case-insensitive name.
	 * 
	 * @param config The configuration object to do the lookup against
	 * @param path The path of the option
	 * @return
	 */
	public static Material getMaterial(Configuration config, String path) {
		Object mid = config.get(path);
		if (mid instanceof String) {
			return Material.matchMaterial((String)mid);
		} else if (mid instanceof Number) {
			return Material.getMaterial(((Number)mid).intValue());
		} else {
			config = config.getDefaults();
			if (config != null) {
				return getMaterial(config, path);
			} else {
				return null;
			}
		}
	}

	public static Material getMaterial(String value) {
		if (value.matches("\\d+")) {
			return Material.getMaterial(Integer.parseInt(value));
		} else {
			return Material.matchMaterial(value);
		}
	}
}