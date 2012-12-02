package com.astro73.controllerblock4k;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import ru.tehkode.permissions.bukkit.PermissionsEx;

public class PermissionHandler {
	private ControllerBlock parent = null;

	private List<String> builtinAdminPlayers = new ArrayList<String>();

	private PermissionsEx pex = null;

	public PermissionHandler(ControllerBlock p) {
		parent = p;
	}

	public boolean checkPermissionsEx(Player p, String perm) {
		if (pex == null) {
			Plugin plug = parent.getServer().getPluginManager()
					.getPlugin("PermissionsEx");
			if (plug != null) {
				pex = ((PermissionsEx) plug);
				parent.getLogger().fine("PermissionsEx detected and enabled");
			}
		}
		if (pex != null) {
			parent.getLogger().finer("Running PermissionsEx check on " + p.getName()
					+ " for " + perm);
			return pex.has(p, perm);
		}
		return false;
	}

	public void addBuiltinAdminPlayer(String name) {
		builtinAdminPlayers.add(name);
	}

	public boolean isAdminPlayer(Player p) {
		parent.getLogger().finer("Checking if " + p.getName() + " is a CB admin");
		if ((parent.getConfigu().getBool(Config.Option.ServerOpIsAdmin))
				&& (p.isOp())) {
			parent.getLogger().finer(p.getName()
					+ " is a server operator, and serverOpIsAdmin is set");
			return true;
		}

		if (checkPermissionsEx(p, "controllerblock.admin")) {
			parent.getLogger().finer("PermissionsEx said " + p.getName()
					+ " has admin permissions");
			return true;
		}

		String pn = p.getName();
		Iterator<String> i = builtinAdminPlayers.iterator();
		while (i.hasNext()) {
			if (i.next().equals(pn)) {
				parent.getLogger().finer(p.getName()
						+ " is listed in the ControllerBlock.ini as an admin");
				return true;
			}
		}
		parent.getLogger().finer(p.getName() + " isn't an admin");
		return false;
	}

	public boolean canCreate(Player p) {
		if (isAdminPlayer(p)) {
			parent.getLogger().finer(p.getName() + " is an admin, can create");
			return true;
		}

		if (checkPermissionsEx(p, "controllerblock.create")) {
			parent.getLogger().finer("PermissionsEx said " + p.getName()
					+ " can create");
			return true;
		}

		if (parent.getConfigu().getBool(Config.Option.AnyoneCanCreate)) {
			parent.getLogger().finer("Anyone is allowed to create, letting "
					+ p.getName() + " create");
		}
		return parent.getConfigu().getBool(Config.Option.AnyoneCanCreate);
	}

	public boolean canModify(Player p) {
		if (isAdminPlayer(p)) {
			parent.getLogger().finer(p.getName() + " is an admin, can modify");
			return true;
		}

		if (checkPermissionsEx(p, "controllerblock.modifyOther")) {
			parent.getLogger().finer("PermissionsEx says " + p.getName()
					+ " has global modify permissions");
			return true;
		}

		if (parent.getConfigu().getBool(Config.Option.AnyoneCanModifyOther)) {
			parent.getLogger().finer("Anyone is allowed to modify anyones blocks, allowing "
							+ p.getName() + " to modify");
		}
		return parent.getConfigu().getBool(Config.Option.AnyoneCanModifyOther);
	}

	public boolean canModify(Player p, CBlock c) {
		if (p.getName().equals(c.getOwner())) {
			parent.getLogger().finer(p.getName()
					+ " owns this controller, allowing to modify");
			return true;
		}
		return canModify(p);
	}

	public boolean canDestroy(Player p) {
		if (isAdminPlayer(p)) {
			parent.getLogger().finer(p.getName() + " is an admin, allowing destroy");
			return true;
		}

		if (checkPermissionsEx(p, "controllerblock.destroyOther")) {
			parent.getLogger().finer("PermissionsEx says " + p.getName()
					+ " has global destroy permissions");
			return true;
		}

		if (parent.getConfigu().getBool(Config.Option.AnyoneCanDestroyOther)) {
			parent.getLogger().finer("Anyone is allowed to destroy anyones blocks, allowing "
							+ p.getName() + " to destroy");
		}
		return parent.getConfigu().getBool(Config.Option.AnyoneCanDestroyOther);
	}

	public boolean canDestroy(Player p, CBlock c) {
		if (p.getName().equals(c.getOwner())) {
			parent.getLogger().finer(p.getName()
					+ "owns this controller, allowing them to destroy it");
			return true;
		}
		return canDestroy(p);
	}
}