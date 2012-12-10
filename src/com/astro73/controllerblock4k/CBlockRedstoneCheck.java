package com.astro73.controllerblock4k;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;

public class CBlockRedstoneCheck implements Runnable {
	private ControllerBlock parent = null;

	public CBlockRedstoneCheck(ControllerBlock c) {
		parent = c;
	}

	public void run() {
		for (CBlock c : parent.getDatabase().find(CBlock.class).findList()) {
			if (!c.isBeingEdited()) {
				Block b = Util.getBlockAtLocation(c.getLocation());
				boolean on = c.isOn();
				if (b.getRelative(BlockFace.UP).getType().equals(Material.REDSTONE_TORCH_ON)) {
					if (on) {
						c.turnOff();
					}
				} else if (b.getRelative(BlockFace.UP).getType().equals(Material.REDSTONE_TORCH_OFF)) {
					if (!on) {
						c.turnOn();
					}
				} else if ((on) && (b.isBlockPowered())) {
					c.turnOff();
				} else if ((!on) && (!b.isBlockPowered())) {
					c.turnOn();
				}
			}
		}
	}
}