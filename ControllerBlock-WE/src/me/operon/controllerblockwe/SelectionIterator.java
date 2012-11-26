package me.operon.controllerblockwe;

import java.util.Iterator;

import org.bukkit.Location;
import com.sk89q.worldedit.BlockVector;

import com.sk89q.worldedit.IncompleteRegionException;
import com.sk89q.worldedit.LocalWorld;
import com.sk89q.worldedit.bukkit.BukkitWorld;
import com.sk89q.worldedit.bukkit.selections.Selection;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.regions.RegionIterator;

class SelectionIterator implements Iterable<Location> {
	private Selection selection;
	
	public SelectionIterator(Selection selection) throws IncompleteRegionException, NonBukkitWorld {
		this.selection = selection;
		// This is to catch the errors at creation time in order to propagate them, instead of forcing us to deal with them below.
		new SelIterator(this.selection);
	}

	@Override
	public Iterator<Location> iterator() {
		try {
			return new SelIterator(this.selection);
		} catch (IncompleteRegionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		} catch (NonBukkitWorld e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
	}
	
	@SuppressWarnings("serial")
	class NonBukkitWorld extends Exception {
		
	}
	
	class SelIterator implements Iterator<Location> {
		private Region region;
		private RegionIterator riter;
		private BukkitWorld world;
		
		public SelIterator(Selection sel) throws IncompleteRegionException, NonBukkitWorld {
			this.region = sel.getRegionSelector().getRegion();
			this.riter = new RegionIterator(this.region);
			LocalWorld w = this.region.getWorld();
			if (w instanceof BukkitWorld) {
				this.world = (BukkitWorld)w;
			} else {
				System.out.println("WorldEdit gave us a non-bukkit LocalWorld; giving.");
				throw new NonBukkitWorld();
			}
		}
		
		@Override
		public boolean hasNext() {
			return this.riter.hasNext();
		}

		@Override
		public Location next() {
			BlockVector bv = this.riter.next();
			return new Location(this.world.getWorld(), bv.getX(), bv.getY(), bv.getZ());
		}

		@Override
		public void remove() {
			this.riter.remove();
		}

	}

}
