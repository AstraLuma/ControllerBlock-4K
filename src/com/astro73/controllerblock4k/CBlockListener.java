package com.astro73.controllerblock4k;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.PlayerInventory;

public class CBlockListener implements Runnable, Listener {
	private ControllerBlock parent;

	public CBlockListener(ControllerBlock controllerBlock) {
		parent = controllerBlock;
	}

	public Player getPlayerEditing(CBlock c) {
		for (Map.Entry<Player, CBlock> e : parent.editing.entrySet()) {
			if (e.getValue().equals(c)) {
				return e.getKey();
			}
		}
		return null;
	}

	public void removePlayersEditing(CBlock c) {
		for (Map.Entry<Player, CBlock> e : parent.editing.entrySet()) {
			if (e.getValue().equals(c)) {
				parent.editing.remove(e.getKey());
			}
		}
	}

	public boolean isRedstone(Block b) {
		Material t = b.getType();

		return (t.equals(Material.REDSTONE_WIRE))
				|| (t.equals(Material.REDSTONE_TORCH_ON))
				|| (t.equals(Material.REDSTONE_TORCH_OFF));
	}
	
	
	/**
	 * FIXME: Redo block type filtering
	 * @param e
	 */
	@EventHandler(priority = EventPriority.HIGHEST)
	public void onBlockBreak(BlockBreakEvent e) {
		if (e.isCancelled()) {
			return;
		}
		Player player = e.getPlayer();
		
		Block b = e.getBlock();
		PlayerInventory inv = null; 
		Material item = null;
		if (player != null) {
			inv = player.getInventory();
			item = inv.getItemInHand().getType();
		}
		
		if ((player != null)
				&& (GameMode.CREATIVE.equals(player.getGameMode()))
				&& (item.isBlock())) {

			if (item.equals(Material.WOOD_PICKAXE)
					|| item.equals(Material.STONE_PICKAXE)
					|| item.equals(Material.IRON_PICKAXE)
					|| item.equals(Material.GOLD_PICKAXE)
					|| item.equals(Material.DIAMOND_PICKAXE)) {
				//TODO: Allow for other items
				return;
			}
			
			CBlock conBlock = parent.editing.get(player);
			if (conBlock != null) { // Only happens if player != null
				assert player != null;
				if (parent.isControlBlock(b.getLocation())) {
					conBlock.editBlock(false);
					parent.editing.remove(player);

					if (Util.locEquals(conBlock.getLocation(), b.getLocation())) {
						player.sendMessage("Finished editing ControllerBlock");
						e.setCancelled(true);
						return;
					}

					player.sendMessage("Finished editing previous ControllerBlock");
					e.setCancelled(true);
					conBlock = null;
				}

			}

			if (conBlock == null) {
				conBlock = parent.getCBlock(b.getLocation());
				if (conBlock == null) {
					if (!isRedstone(b.getRelative(BlockFace.UP))) {
						return;
					}
					CBlock.Protection cBType;
					String cBTypeStr = null;
					if (b.getType() == parent.getCBlockType()) {
						cBTypeStr = "protected";
						cBType = CBlock.Protection.PROTECTED;
					} else if (b.getType() == parent.getSemiProtectedCBlockType()) {
						cBTypeStr = "semi-protected";
						cBType = CBlock.Protection.SEMIPROTECTED;
					} else if (b.getType() == parent.getUnProtectedCBlockType()) {
						cBTypeStr = "unprotected";
						cBType = CBlock.Protection.UNPROTECTED;
					} else {
						e.setCancelled(true);
						return;
					}
					
					if (!parent.getPerm().canCreate(player)) {
						player.sendMessage("You're not allowed to create " + cBTypeStr + " ControllerBlocks");
						e.setCancelled(true);
						return;
					}
					if (parent.isControlledBlock(b.getLocation())) {
						player.sendMessage("This block is controlled, controlled blocks can't be controllers");
						e.setCancelled(true);
						return;
					}
					conBlock = parent.createCBlock(b.getLocation(), player.getName(), cBType);
					player.sendMessage("Created " + cBTypeStr + " controller block");
					e.setCancelled(true);
				}

				if (conBlock == null) {
					return;
				}
				if (!parent.getPerm().canModify(player, conBlock)) {
					player.sendMessage("You're not allowed to modify this ControllerBlock");
					e.setCancelled(true);
					return;
				}

				if (item.equals(Material.STICK)) {
					parent.movingCBlock.put(player.getName(), conBlock);
					player.sendMessage("ControllerBlock is registered as the next to move.   Right-Click the position where to move it.");
					e.setCancelled(true);
					return;
				}

				if (conBlock.numBlocks() == 0) {
					if (!parent.isValidMaterial(item)) {
						player.sendMessage("Can't set the ControllerBlock type to " + item);
						e.setCancelled(true);
						return;
					}

					if (((conBlock.protectedLevel == CBlock.Protection.SEMIPROTECTED) || (conBlock.protectedLevel == CBlock.Protection.UNPROTECTED))
							&& (!parent.isUnprotectedMaterial(item))) {
						if (player != null) 
							player.sendMessage("The Material is protected, can't use with (semi-)unprotected ControllerBlocks.");
						e.setCancelled(true);
						return;
					}
				}

				parent.editing.put(player, conBlock);
				conBlock.editBlock(true);
				player.sendMessage("You're now editing this block "
						+ Util.formatBlockCount(conBlock));
				e.setCancelled(true);
				return;
			}
		} //END player != null
		
		CBlock conBlock = parent.getCBlock(b.getLocation());
		if (conBlock != null) {
			if (!parent.getPerm().canDestroy(player, conBlock)) {
				if (player != null)
					player.sendMessage("You're not allowed to destroy this ControllerBlock");
				e.setCancelled(true);
				return;
			}
			conBlock = parent.destroyCBlock(b.getLocation());
			if (conBlock != null) {
				if (player != null)
					player.sendMessage("Destroyed controller block");
				removePlayersEditing(conBlock);
			}
		}

		conBlock = parent.editing.get(player);
		if ((conBlock != null) && (conBlock.hasBlock(b.getLocation()))
				/*&& (conBlock.getType().equals(b.getType()))*/) {
			if (conBlock.delBlock(b)) {
				if (player != null)
					player.sendMessage("Block removed from controller "
						+ Util.formatBlockCount(conBlock));
			}
		} else if ((conBlock = parent.getControllerBlockFor(b.getLocation())) != null) {
			switch (BlockProtectMode.fromConfig(parent.getConfig().getString("BlockProtectMode"))) {
				case PROTECT:
					if ((conBlock.protectedLevel != CBlock.Protection.PROTECTED)
							&& ((conBlock.isOn()) || (conBlock.protectedLevel == CBlock.Protection.UNPROTECTED))) {
						break;
					}
					if (player != null)
						player.sendMessage("This block is controlled by a controller block at "
							+ Util.formatLocation(conBlock.getLocation()));
					e.setCancelled(true);
					break;
				case REMOVE:
					conBlock.delBlock(b);
				case NONE:
			}
		}
	}
	
	
	/**
	 * FIXME: Type filtering
	 * @param e
	 */
	@EventHandler(priority = EventPriority.HIGHEST)
	public void onBlockDamage(BlockDamageEvent e) {
		Player player = e.getPlayer();

		if ((e.isCancelled()) && (e.getBlock().getType().equals(Material.AIR))) {
			CBlock conBlock;
			if ((conBlock = parent.destroyCBlock(e.getBlock().getLocation())) != null) {
				if (player != null)
					player.sendMessage("Destroyed controller block with superpickaxe?");
				removePlayersEditing(conBlock);
			}
		}
		if ((e.isCancelled())
				|| (player != null && GameMode.CREATIVE.equals(player.getGameMode()))) {
			return;
		}
		
		PlayerInventory inv = null; 
		Material item = null;
		if (player != null) {
			inv = player.getInventory();
			item = inv.getItemInHand().getType();
		}
		Block b = e.getBlock();

		if ((item != null) && (
				(item.equals(Material.WOOD_PICKAXE))
				|| (item.equals(Material.STONE_PICKAXE))
				|| (item.equals(Material.IRON_PICKAXE))
				|| (item.equals(Material.GOLD_PICKAXE))
				|| (item.equals(Material.DIAMOND_PICKAXE)))) {
			return;
		}
		
		CBlock conBlock = parent.editing.get(player);
		if (conBlock != null) {
			assert player != null;
			if (parent.isControlBlock(b.getLocation())) {
				conBlock.editBlock(false);
				parent.editing.remove(player);

				if (Util.locEquals(conBlock.getLocation(), b.getLocation())) {
					player.sendMessage("Finished editing ControllerBlock");
					return;
				}
				player.sendMessage("Finished editing previous ControllerBlock");
				conBlock = null;
			}

		}

		if (conBlock == null) {
			conBlock = parent.getCBlock(b.getLocation());
			if (conBlock == null) {
				if (!isRedstone(b.getRelative(BlockFace.UP))) {
					return;
				}
				CBlock.Protection cBType;
				String cBTypeStr = null;
				if (b.getType() == parent.getCBlockType()) {
					cBTypeStr = "protected";
					cBType = CBlock.Protection.PROTECTED;
				} else {
					if (b.getType() == parent.getSemiProtectedCBlockType()) {
						cBTypeStr = "semi-protected";
						cBType = CBlock.Protection.SEMIPROTECTED;
					} else {
						if (b.getType() == parent.getUnProtectedCBlockType()) {
							cBTypeStr = "unprotected";
							cBType = CBlock.Protection.UNPROTECTED;
						} else {
							return;
						}
					}
				}
				if (!parent.getPerm().canCreate(player)) {
					if (player != null)
						player.sendMessage("You're not allowed to create " + cBTypeStr + " ControllerBlocks");
					return;
				}
				if (parent.isControlledBlock(b.getLocation())) {
					if (player != null)
						player.sendMessage("This block is controlled, controlled blocks can't be controllers");
					return;
				}
				conBlock = parent.createCBlock(b.getLocation(), player.getName(), cBType);
				player.sendMessage("Created " + cBTypeStr + " controller block");
			}

			if (conBlock == null) {
				return;
			}
			if (!parent.getPerm().canModify(player, conBlock)) {
				if (player != null)
					player.sendMessage("You're not allowed to modify this ControllerBlock");
				return;
			}

			if (item != null && item.equals(Material.STICK)) {
				parent.movingCBlock.put(player.getName(), conBlock);
				player.sendMessage("ControllerBlock is registered as the next to move.   Right-Click the position where to move it.");
				return;
			}

			if (conBlock.numBlocks() == 0) {
				if (!parent.isValidMaterial(item)) {
					player.sendMessage("Can't set the ControllerBlock type to "
							+ item);
					return;
				}

				if (((conBlock.protectedLevel == CBlock.Protection.SEMIPROTECTED) || (conBlock.protectedLevel == CBlock.Protection.UNPROTECTED))
						&& (!parent.isUnprotectedMaterial(item))) {
					player.sendMessage("The Material is protected, can't use with (semi-)unprotected ControllerBlocks.");
					return;
				}
			}

			parent.editing.put(player, conBlock);
			conBlock.editBlock(true);
			player.sendMessage("You're now editing this block " + Util.formatBlockCount(conBlock));
		}
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onBlockPlace(BlockPlaceEvent e) {
		if (!e.canBuild()) {
			return;
		}
		Player player = e.getPlayer();
		CBlock conBlock = parent.editing.get(player);
		if (conBlock == null) { // Always happens for non-players
			return;
		}
		
		int maxblocks = parent.getConfig().getInt("MaxBlocksPerController");
		if (
				(maxblocks != 0)
				&& (conBlock.numBlocks() >= maxblocks)
				&& (!parent.getPerm().isAdminPlayer(player))
				) {
			player.sendMessage("Controller block is full " + Util.formatBlockCount(conBlock));
			return;
		}
		
		int maxdist = parent.getConfig().getInt("MaxDistanceFromController");
		if ((maxdist != 0)
				&& (!parent.getPerm().isAdminPlayer(player))
				&& (Util.getDistanceBetweenLocations(conBlock.getLocation(), e.getBlock().getLocation()) > maxdist)
				) {
			player.sendMessage("This block is too far away from the controller block to be controlled");
			return;
		}

		if (conBlock.addBlock(e.getBlock())) {
			player.sendMessage("Added block to controller "
					+ Util.formatBlockCount(conBlock));
		}
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onBlockPhysics(BlockPhysicsEvent e) {
		CBlock conBlock = parent.getControllerBlockFor(null, e.getBlock().getLocation(), true);
		if (conBlock == null) {
			return;
		}
		if (conBlock.isBeingEdited()) {
			if (!parent.blockPhysicsEditCheck) {
				return;
			}

			//TODO: Does this need more items in it?
			if ((e.getBlock().getType().equals(Material.FENCE))
					|| (e.getBlock().getType().equals(Material.THIN_GLASS))) {
				return;
			}

			/*Player player = getPlayerEditing(conBlock);

			if (!Util.typeEquals(conBlock.getType(), e.getChangedType())) {
				parent.log.debug("Block at "
						+ Util.formatLocation(e.getBlock().getLocation())
						+ " was changed to " + e.getChangedType()
						+ " but is supposed to be " + conBlock.getType()
						+ ", dupe!");
				conBlock.delBlock(e.getBlock());
				player.sendMessage("Removing block due to changed type while editing "
						+ Util.formatBlockCount(conBlock));
			}*/
		} else {
			BlockProtectMode protect = BlockProtectMode.fromConfig(parent.getConfig().getString("BlockPhysicsProtectMode"));
			switch (protect) {
			case PROTECT:
				e.setCancelled(true);
				break;
			case REMOVE:
				conBlock.delBlock(e.getBlock());
				break;
			}
		}
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onBlockFromTo(BlockFromToEvent e) {
		CBlock conBlock = parent.getControllerBlockFor(null, e.getToBlock()	.getLocation(), true);
		if (conBlock == null) {
			return;
		}
		if (conBlock.isBeingEdited()) {
			if (!parent.blockPhysicsEditCheck) {
				return;
			}
			Player player = getPlayerEditing(conBlock);
			parent.getLogger()
					.info("Block at "
							+ Util.formatLocation(e.getToBlock().getLocation())
							+ " was drowned while editing and removed from a controller");
			conBlock.delBlock(e.getToBlock());
			player.sendMessage("Removing block due to change while editing "
					+ Util.formatBlockCount(conBlock));
		} else {
			BlockProtectMode protect = BlockProtectMode.fromConfig(parent.getConfig().getString("BlockFlowProtectMode"));
			if (protect.equals(BlockProtectMode.PROTECT)) {
				e.setCancelled(true);
			} else if (protect.equals(BlockProtectMode.REMOVE)) {
				conBlock.delBlock(e.getBlock());
			}
		}
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onBlockPistonExtend(BlockPistonExtendEvent event) {
		if (parent.getConfig().getBoolean("PistonProtection")) {
			Block b = event.getBlock();
			CBlock conBlock = parent.getCBlock(b.getLocation());
			if (conBlock != null) {
				event.setCancelled(true);
				return;
			}
			List<?> pblocks = event.getBlocks();
			for (int i = 0; i < pblocks.size(); i++) {
				Block block = (Block) pblocks.get(i);
				if ((!parent.isControlledBlock(block.getLocation()))
						|| (parent.isUnprotectedMaterial(block.getType()))) {
					continue;
				}
				event.setCancelled(true);
			}
		}
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onBlockPistonRetract(BlockPistonRetractEvent event) {
		if (parent.getConfig().getBoolean("PistonProtection")) {
			Block b = event.getBlock();
			CBlock conBlock = parent.getCBlock(b.getLocation());
			if (conBlock != null) {
				event.setCancelled(true);
				return;
			}
			if (event.isSticky()) {
				Block block = b.getWorld().getBlockAt(
						event.getRetractLocation());

				if (parent.isControlledBlock(block.getLocation())
						&& (!parent.isUnprotectedMaterial(block.getType()))) {
					event.setCancelled(true);
					return;
				}
			}
		}
	}

	public void run() {
		if (!parent.getConfig().getBoolean("DisableEditDupeProtection")) {
			//FIXME: What is this checking exactly?
/*			for (Map.Entry<CBlock> e : parent.map.entrySet()) {
				Iterator<BlockDesc> i = e.getValue().iterator();
				while (i.hasNext()) {
					Block b = Util
							.getBlockAtLocation(i.next().loc);
					if (!Util.typeEquals(b.getType(), e.getValue().getType())) {
						parent.log
								.debug("Block at "
										+ Util.formatLocation(b.getLocation())
										+ " was " + b.getType()
										+ " but expected "
										+ e.getValue().getType()
										+ ", dupe!");
						i.remove();
						((Player) e.getKey())
								.sendMessage("Removing block due to changed while editing "
										+ Util.formatBlockCount((CBlock) e
												.getValue()));
						return;
					}
				}
			}*/
		}
		for (Map.Entry<Player, CBlock> e : parent.editing.entrySet()) {
			Iterator<BlockDesc> ibd = parent.getDatabase().find(BlockDesc.class).where().eq("lord", e.getValue()).findIterate();
			while (ibd.hasNext()) {
				BlockDesc bd = ibd.next();
				bd.apply(false);
			}
		}
	}
}