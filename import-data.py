#!/usr/bin/python
# -*- tab-width: 4; use-tabs: 1; coding: utf-8 -*-
# vim:tabstop=4:noexpandtab:
"""
Import data from other versions of ControllerBlock.
"""
import argparse, sys, mysql.connector
parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument("oldfiles", metavar='F', type=file, nargs='+',
                    help="The data file(s) to read from.")
parser.add_argument("-f", "--sql-file", dest="sqlfile",
                    help="write SQL statements to FILE", metavar="FILE")
parser.add_argument("-q", "--quiet",
                    action="store_false", dest="verbose", default=True,
                    help="Don't print status messages to stdout")
parser.add_argument("--dry-run",
                    action="store_false", dest="doit", default=True,
                    help="Don't actually change anything")

parser.add_argument("--mysql-server", dest="dbserver",
                    help="MySQL host to connect to", metavar="HOST")
parser.add_argument("--mysql-user", dest="dbuser",
                    help="MySQL user", metavar="USER")
parser.add_argument("--mysql-password", dest="dbpassword",
                    help="MySQL password", metavar="PASS")
parser.add_argument("--mysql-db", dest="dbname",
                    help="MySQL database", metavar="DATABASE")

BLOCK_IDS = dict(
	AIR = 0,
	STONE = 1,
	GRASS = 2,
	DIRT = 3,
	COBBLESTONE = 4,
	WOOD = 5,
	SAPLING = 6,
	BEDROCK = 7,
	WATER = 8,
	STATIONARY_WATER = 9,
	LAVA = 10,
	STATIONARY_LAVA = 11,
	SAND = 12,
	GRAVEL = 13,
	GOLD_ORE = 14,
	IRON_ORE = 15,
	COAL_ORE = 16,
	LOG = 17,
	LEAVES = 18,
	SPONGE = 19,
	GLASS = 20,
	LAPIS_ORE = 21,
	LAPIS_BLOCK = 22,
	DISPENSER = 23,
	SANDSTONE = 24,
	NOTE_BLOCK = 25,
	BED_BLOCK = 26,
	POWERED_RAIL = 27,
	DETECTOR_RAIL = 28,
	PISTON_STICKY_BASE = 29,
	WEB = 30,
	LONG_GRASS = 31,
	DEAD_BUSH = 32,
	PISTON_BASE = 33,
	PISTON_EXTENSION = 34,
	WOOL = 35,
	PISTON_MOVING_PIECE = 36,
	YELLOW_FLOWER = 37,
	RED_ROSE = 38,
	BROWN_MUSHROOM = 39,
	RED_MUSHROOM = 40,
	GOLD_BLOCK = 41,
	IRON_BLOCK = 42,
	DOUBLE_STEP = 43,
	STEP = 44,
	BRICK = 45,
	TNT = 46,
	BOOKSHELF = 47,
	MOSSY_COBBLESTONE = 48,
	OBSIDIAN = 49,
	TORCH = 50,
	FIRE = 51,
	MOB_SPAWNER = 52,
	WOOD_STAIRS = 53,
	CHEST = 54,
	REDSTONE_WIRE = 55,
	DIAMOND_ORE = 56,
	DIAMOND_BLOCK = 57,
	WORKBENCH = 58,
	CROPS = 59,
	SOIL = 60,
	FURNACE = 61,
	BURNING_FURNACE = 62,
	SIGN_POST = 63,
	WOODEN_DOOR = 64,
	LADDER = 65,
	RAILS = 66,
	COBBLESTONE_STAIRS = 67,
	WALL_SIGN = 68,
	LEVER = 69,
	STONE_PLATE = 70,
	IRON_DOOR_BLOCK = 71,
	WOOD_PLATE = 72,
	REDSTONE_ORE = 73,
	GLOWING_REDSTONE_ORE = 74,
	REDSTONE_TORCH_OFF = 75,
	REDSTONE_TORCH_ON = 76,
	STONE_BUTTON = 77,
	SNOW = 78,
	ICE = 79,
	SNOW_BLOCK = 80,
	CACTUS = 81,
	CLAY = 82,
	SUGAR_CANE_BLOCK = 83,
	JUKEBOX = 84,
	FENCE = 85,
	PUMPKIN = 86,
	NETHERRACK = 87,
	SOUL_SAND = 88,
	GLOWSTONE = 89,
	PORTAL = 90,
	JACK_O_LANTERN = 91,
	CAKE_BLOCK = 92,
	DIODE_BLOCK_OFF = 93,
	DIODE_BLOCK_ON = 94,
	LOCKED_CHEST = 95,
	TRAP_DOOR = 96,
	MONSTER_EGGS = 97,
	SMOOTH_BRICK = 98,
	HUGE_MUSHROOM_1 = 99,
	HUGE_MUSHROOM_2 = 100,
	IRON_FENCE = 101,
	THIN_GLASS = 102,
	MELON_BLOCK = 103,
	PUMPKIN_STEM = 104,
	MELON_STEM = 105,
	VINE = 106,
	FENCE_GATE = 107,
	BRICK_STAIRS = 108,
	SMOOTH_STAIRS = 109,
	MYCEL = 110,
	WATER_LILY = 111,
	NETHER_BRICK = 112,
	NETHER_FENCE = 113,
	NETHER_BRICK_STAIRS = 114,
	NETHER_WARTS = 115,
	ENCHANTMENT_TABLE = 116,
	BREWING_STAND = 117,
	CAULDRON = 118,
	ENDER_PORTAL = 119,
	ENDER_PORTAL_FRAME = 120,
	ENDER_STONE = 121,
	DRAGON_EGG = 122,
	REDSTONE_LAMP_OFF = 123,
	REDSTONE_LAMP_ON = 124
)

class CBFile(object):
	def __init__(self, stream):
		self.stream = stream
	
	def __iter__(self):
		for line in self.stream:
			line = line.strip()
			if line.startswith('#'):
				continue
			fields = line.split(',')
			lord = {
				'world': fields[0],
				'x': int(fields[1]),
				'y': int(fields[2]),
				'z': int(fields[3]),
				'material': fields[4],
				'owner': fields[5],
				'serfs': [
					{
						'world': fields[n+0],
						'x': int(fields[n+1]),
						'y': int(fields[n+2]),
						'z': int(fields[n+3]),
						'meta': int(fields[n+4]),
					}
					for n in xrange(6, len(fields), 5)
				],
			}
			lord['blockid'] = BLOCK_IDS.get(lord['material'])
			yield lord

class Executor(object):
	"""
	Singleton to handle the conditional execution and printing of SQL.
	
	* doit: False if dry run
	* cursor: The database cursor to handle the operation
	* stream: A file-like object to print statements to.
	"""
	doit = True
	cursor = None
	stream = None
	
	@staticmethod
	def literal(o):
		return repr(o)

	def __call__(self, stmt, args=None):
		if self.stream is not None:
			print >> self.stream, stmt % tuple(map(self.literal, args))
		if self.doit and self.cursor is not None:
			self.cursor.execute(stmt, args)
			return self.cursor.lastrowid
execr = Executor()
			
if __name__ == '__main__':
	args = parser.parse_args()
	print args.doit
	if args.doit:
		connargs = {}
		if args.dbserver:   connargs['host'] = args.dbserver
		if args.dbuser:     connargs['user'] = args.dbuser
		if args.dbpassword: connargs['passwd'] = args.dbpassword
		if args.dbname:     connargs['db'] = args.dbname
		conn = mysql.connector.connect(**connargs)
		execr.cursor = conn.cursor()
	
	if args.sqlfile:
		if args.sqlfile == '-':
			execr.stream = sys.stdout
		else:
			execr.stream = file(args.sqlfile, 'wU')
	execr.doit = args.doit
	
	for oldfile in args.oldfiles:
		for lord in CBFile(oldfile):
			lid = execr("INSERT INTO ControllerBlock_Lord SET world = %s, x = %s, y = %s, z = %s, owner = %s;",
				[lord['world'], lord['x'], lord['y'], lord['z'], lord['owner']])
			for serf in lord['serfs']:
				execr("INSERT INTO ControllerBlock_Serf SET world = %s, x = %s, y = %s, z = %s, material = %s,  meta = %s, lord_id = %s;",
				[serf['world'], serf['x'], serf['y'], serf['z'], lord['blockid'], serf['meta'], lid])
			if args.doit:
				conn.commit()
			

