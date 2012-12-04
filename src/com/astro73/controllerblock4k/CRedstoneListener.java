package com.astro73.controllerblock4k;

import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockRedstoneEvent;
import org.bukkit.material.MaterialData;

public class CRedstoneListener implements Listener {
	private ControllerBlock parent;

	public CRedstoneListener(ControllerBlock controllerBlock) {
		parent = controllerBlock;
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void onBlockRedstoneChange(BlockRedstoneEvent e) {
		CBlock conBlock = null;
		if (parent.getConfig().getBoolean("QuickRedstoneCheck")) {
			conBlock = parent.getCBlock(e.getBlock()
					.getRelative(BlockFace.DOWN).getLocation());
		}
		if (conBlock == null) {
			return;
		}

		BlockState s = e.getBlock().getState();
		if (s.getType().equals(Material.REDSTONE_WIRE)) {
			MaterialData m = s.getData();
			m.setData((byte) e.getNewCurrent());
			s.setData(m);
		}
		conBlock.doRedstoneCheck(s);
	}
}