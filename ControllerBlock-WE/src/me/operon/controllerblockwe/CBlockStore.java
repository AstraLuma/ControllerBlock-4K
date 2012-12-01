package me.operon.controllerblockwe;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Iterator;
import java.util.NoSuchElementException;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;

/**
 * This handles actually interfacing with the storage system.
 * 
 * @author astronouth7303
 * 
 */
public class CBlockStore {
	private Connection conn;

	private PreparedStatement update_lord, insert_lord, update_serf,
			insert_serf;

	/**
	 * Connects to the database
	 * 
	 * @param config
	 *            The configuration object
	 * @throws SQLException
	 *             From JDBC
	 */
	public CBlockStore(Config config) throws SQLException {
		conn = DriverManager.getConnection((String) config
				.getOpt(Config.Option.SqlConnection));

		update_lord = conn
				.prepareStatement("UPDATE ControllerBlock_Lord SET world = ?, x = ?, y = ?, z = ?, owner = ?, protection = ? WHERE id = ?;");
		insert_lord = conn
				.prepareStatement(
						"INSERT INTO ControllerBlock_Lord SET world = ?, x = ?, y = ?, z = ?, owner = ?, protection = ?;",
						Statement.RETURN_GENERATED_KEYS);
		update_serf = conn
				.prepareStatement("UPDATE ControllerBlock_Serf SET world = ?, x = ?, y = ?, z = ?, material = ?,  meta = ? WHERE lord = ? AND id = ?;");
		insert_serf = conn
				.prepareStatement(
						"INSERT INTO ControllerBlock_Serf SET world = ?, x = ?, y = ?, z = ?, material = ?,  meta = ?, lord = ?;",
						Statement.RETURN_GENERATED_KEYS);
	}

	/**
	 * Stores the controlling block
	 * 
	 * @param id
	 *            The block's ID persistent across moves, or 0 if it has none
	 *            yet
	 * @param loc
	 *            The block's location
	 * @param owner
	 *            The block's owner player
	 * @param pl
	 *            The block's protection level
	 * @return The block's ID in the store.
	 */
	public long storeLordBlock(long id, Location loc, String owner,
			CBlock.Protection pl) {
		PreparedStatement stmt = id == 0 ? insert_lord : update_lord;
		try {
			stmt.setString(0, loc.getWorld().getName());
			stmt.setInt(1, loc.getBlockX());
			stmt.setInt(2, loc.getBlockY());
			stmt.setInt(3, loc.getBlockZ());
			stmt.setString(4, owner);
			stmt.setString(5, pl.name());
			stmt.setString(5, pl.name());
			if (id == 0) {
				// INSERT new block
			} else {
				// UPDATE existing block
				stmt.setLong(6, id);
			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return 0;
		}
		try {
			stmt.executeUpdate();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		if (id == 0) {
			try {
				ResultSet rs = stmt.getGeneratedKeys();
				rs.next();
				return rs.getLong(1);
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return 0;
			}
		} else {
			return id;
		}
	}

	/**
	 * Stores a controlled block
	 * 
	 * @param lord
	 *            The ID of the ControllerBlock controlling it
	 * @param bd
	 *            The data on the block
	 * @return
	 */
	public long storeSerfBlock(long lord, BlockDesc bd) {
		PreparedStatement stmt = bd.id == 0 ? insert_serf : update_serf;
		try {
			stmt.setString(0, bd.loc.getWorld().getName());
			stmt.setInt(1, bd.loc.getBlockX());
			stmt.setInt(2, bd.loc.getBlockY());
			stmt.setInt(3, bd.loc.getBlockZ());
			stmt.setInt(4, bd.mat.getId());
			stmt.setByte(5, bd.data);
			stmt.setLong(6, lord);
			if (bd.id == 0) {
				// INSERT new block
			} else {
				// UPDATE existing block
				stmt.setLong(7, bd.id);
			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return 0;
		}
		try {
			stmt.executeUpdate();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		if (bd.id == 0) {
			try {
				ResultSet rs = stmt.getGeneratedKeys();
				rs.next();
				return rs.getLong(1);
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return 0;
			}
		} else {
			return bd.id;
		}
	}

	public Iterable<CBlock> loadAllLords(ControllerBlock parent) {
		try {
			PreparedStatement stmt = conn
					.prepareStatement("SELECT * FROM ControllerBlock_Lord;");
			ResultSet rs = stmt.executeQuery();
			return new LordIterable(this, rs, parent);
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
	}

	class LordIterable implements Iterable<CBlock> {
		private ResultSet rs;
		private CBlockStore store;
		ControllerBlock parent;

		public LordIterable(CBlockStore store, ResultSet rs, ControllerBlock p) {
			this.store = store;
			this.rs = rs;
			parent = p;
		}

		@Override
		public Iterator<CBlock> iterator() {
			return new LordIterator(store, rs, parent);
		}
	}

	class LordIterator implements Iterator<CBlock> {
		CBlockStore store;
		ResultSet rs;
		boolean checked, hasnext;
		ControllerBlock parent;

		public LordIterator(CBlockStore store, ResultSet rs, ControllerBlock p) {
			this.store = store;
			this.rs = rs;
			checked = hasnext = false;
			parent = p;
		}

		@Override
		public boolean hasNext() {
			if (!checked) {
				try {
					hasnext = rs.next();
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				checked = true;
			}
			return hasnext;
		}

		@Override
		public CBlock next() {
			if (!hasNext()) {
				throw new NoSuchElementException();
			}
			try {
				long id = rs.getLong("id");
				String ws = rs.getString("world");
				int x = rs.getInt("x");
				int y = rs.getInt("y");
				int z = rs.getInt("z");
				String owner = rs.getString("owner");
				int pi = rs.getInt("protection");
				CBlock.Protection pl = CBlock.Protection.values()[pi];
				World world = parent.getServer().getWorld(ws);
				Location loc = new Location(world, x, y, z);
				CBlock cb = new CBlock(parent, id, loc, owner, pl);
				cb.loadSerfs(store);
				return cb;
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return null;
			} finally {
				checked = false;
			}
		}

		@Override
		public void remove() {
			try {
				rs.deleteRow();
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				throw new IllegalStateException();
			}
		}
	}

	public Iterable<BlockDesc> loadAllSerfs(ControllerBlock parent, long lord) {
		try {
			PreparedStatement stmt = conn
					.prepareStatement("SELECT * FROM ControllerBlock_Serf WHERE lord = ?;");
			stmt.setLong(0, lord);
			ResultSet rs = stmt.executeQuery();
			return new SerfIterable(this, rs, parent);
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
	}

	class SerfIterable implements Iterable<BlockDesc> {
		private ResultSet rs;
		private CBlockStore store;
		ControllerBlock parent;

		public SerfIterable(CBlockStore store, ResultSet rs, ControllerBlock p) {
			this.store = store;
			this.rs = rs;
			parent = p;
		}

		@Override
		public Iterator<BlockDesc> iterator() {
			return new SerfIterator(store, rs, parent);
		}
	}

	class SerfIterator implements Iterator<BlockDesc> {
		CBlockStore store;
		ResultSet rs;
		boolean checked, hasnext;
		ControllerBlock parent;

		public SerfIterator(CBlockStore store, ResultSet rs, ControllerBlock p) {
			this.store = store;
			this.rs = rs;
			checked = hasnext = false;
			parent = p;
		}

		@Override
		public boolean hasNext() {
			if (!checked) {
				try {
					hasnext = rs.next();
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				checked = true;
			}
			return hasnext;
		}

		@Override
		public BlockDesc next() {
			if (!hasNext()) {
				throw new NoSuchElementException();
			}
			try {
				long id = rs.getLong("id");
				String ws = rs.getString("world");
				int x = rs.getInt("x");
				int y = rs.getInt("y");
				int z = rs.getInt("z");
				int mid = rs.getInt("material");
				byte data = rs.getByte("meta");
				World world = parent.getServer().getWorld(ws);
				Location loc = new Location(world, x, y, z);
				Material mat = Material.getMaterial(mid);
				BlockDesc cb = new BlockDesc(id, loc, mat, data);
				return cb;
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return null;
			} finally {
				checked = false;
			}
		}

		@Override
		public void remove() {
			try {
				rs.deleteRow();
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				throw new IllegalStateException();
			}
		}
	}

	public boolean removeLord(Location loc) {
		return false;
	}
}
