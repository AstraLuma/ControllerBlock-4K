package me.operon.controllerblockwe;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import org.bukkit.Location;

/**
 * This handles actually interfacing with the storage system.
 * @author astronouth7303
 *
 */
public class CBlockStore {
	private Connection conn;
	
	/**
	 * Connects to the database
	 * @param config The configuration object
	 * @throws SQLException From JDBC
	 */
	public CBlockStore(Config config) throws SQLException {
		conn = DriverManager.getConnection((String) config.getOpt(Config.Option.SqlConnection));
	}
	
	/**
	 * Stores the controlling block
	 * @param id The block's ID persistent across moves, or 0 if it has none yet
	 * @param loc The block's location
	 * @param owner The block's owner player
	 * @param pl The block's protection level
	 * @return The block's ID in the store.
	 */
	public long storeLordBlock(long id, Location loc, String owner, CBlock.Protection pl) {
		return 0;
	}
	
	/**
	 * Stores a controlled block
	 * @param lord The ID of the ControllerBlock controlling it
	 * @param bd The data on the block
	 * @return
	 */
	public long storeSerfBlock(long lord, BlockDesc bd) {
		return 0;
	}
}
