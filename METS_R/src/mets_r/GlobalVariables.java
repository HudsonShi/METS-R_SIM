package mets_r;

import java.util.Random;

import mets_r.data.input.NetworkEventObject;

/**
 * Central location for any useful variables (e.g. filenames).
 * 
 * @author Nick Malleson
 * @author Samiul Hasan (SH)
 * @author Xianyuan Zhan
 * @author Xinwu Qian
 * @author Hemant Gehlot
 * Above are the authors for A-RESCUE 1.0 and 2.0
 * @author Zengxiang Lei
 * @author Jiawei Xue
 */

/*
 * How to use Config File (Data.properties in config folder)
 * All the value of global variables are reading from the config file (Data.properties), and all data retrieved from the config file is string type.
 * To add a new global variable: 
 * 1. Define the variable's name and value in the Data.properties
 * 2. Adding corresponding global variable in the GlobalVariables.java
 * e.g.: To add int type new variable "test", first give value for test in Data.properties, as
 * 		 test = 0.5
 * 		 Then, create corresponding declaration for variable test, as
 * 		 public static int test = Integer.valueOf(loadConfig("test"))
 * The loadConfig method is used to load property's value from the config file, and data type transform is required. Useful transform method is list below:
 * To integer: Integer.valueOf(String s)
 * To Float: Float.valueOf(String s)
 * To Double: Double.valueOf(String s)
 * To Boolean: Boolean.valueOf(String s)
 * to single char: .atChar(0), e.g.: public static char whosRunning = loadConfig("whosRunning").charAt(0);
 * 
 * Note: 1. The value for each property in Config File should only include numeric data or string, such content of "/", "0.05f", "0.05 " cannot be reconized
 *       2. To add comment in any content, use "#", instead of "//" or "/*...". e.g. : "#Input Files"
 */

import java.io.FileInputStream;
import java.io.IOException;
import java.util.LinkedList;
import java.util.Properties;

public class GlobalVariables {
	public static Properties config;
	// Loading properties from configuration files, initialized used in ARESCUE
	// credit to Xianyuan Zhan and Christopher Thompson.
	private static String loadConfig(String property) {
		if (config == null) {
			config = new Properties();
			try {
				String working_dir = System.getProperty("user.dir");
				config.load(new FileInputStream(working_dir + "/data/Data.properties"));
			} catch (IOException ex) {
				ex.printStackTrace();
			}
		}
		return config.getProperty(property);
	}
	
	public static void reset() {
		SYNCHRONIZED = Boolean.valueOf(loadConfig("SYNCHRONIZED"));
		STANDALONE = Boolean.valueOf(loadConfig("STANDALONE"));
		V2X = Boolean.valueOf(loadConfig("V2X"));
		RANDOM_SEED = Integer.valueOf(loadConfig("RANDOM_SEED"));
		RandomGenerator = new Random(RANDOM_SEED);
		SIMULATION_STEP_SIZE = Float.valueOf(loadConfig("SIMULATION_STEP_SIZE"));
		SIMULATION_ZONE_REFRESH_INTERVAL = (int) (Integer
				.valueOf(loadConfig("SIMULATION_ZONE_REFRESH_INTERVAL"))/SIMULATION_STEP_SIZE);
		SIMULATION_DEMAND_REFRESH_INTERVAL = (int) (Integer
				.valueOf(loadConfig("SIMULATION_DEMAND_REFRESH_INTERVAL"))/SIMULATION_STEP_SIZE);
		SIMULATION_SPEED_REFRESH_INTERVAL = (int) (Integer
				.valueOf(loadConfig("SIMULATION_SPEED_REFRESH_INTERVAL"))/SIMULATION_STEP_SIZE);
		SIMULATION_BUS_REFRESH_INTERVAL = (int) (Integer
				.valueOf(loadConfig("SIMULATION_BUS_REFRESH_INTERVAL"))/SIMULATION_STEP_SIZE);
		SIMULATION_CHARGING_STATION_REFRESH_INTERVAL = (int) (Integer
				.valueOf(loadConfig("SIMULATION_CHARGING_STATION_REFRESH_INTERVAL"))/SIMULATION_STEP_SIZE);
		SIMULATION_RH_MATCHING_WINDOW = (int) (Integer
				.valueOf(loadConfig("SIMULATION_RH_MATCHING_WINDOW")) / SIMULATION_STEP_SIZE);
		SIMULATION_RH_MAX_CRUISING_TIME = (int) (Integer
				.valueOf(loadConfig("SIMULATION_RH_MAX_CRUISING_TIME")) / SIMULATION_STEP_SIZE);
		SIMULATION_STOP_TIME = (int) (60 * Integer.valueOf(loadConfig("SIMULATION_STOP_TIME"))
				/ SIMULATION_STEP_SIZE);
		SIMULATION_SIGNAL_REFRESH_INTERVAL = SIMULATION_STEP_SIZE>1? 1: (int) (1 / SIMULATION_STEP_SIZE);
		DEMAND_DIFFUSION = Boolean.valueOf(loadConfig("DEMAND_DIFFUSION"));
		HOUR_OF_DEMAND = (int) Math.ceil(SIMULATION_STOP_TIME / (SIMULATION_DEMAND_REFRESH_INTERVAL + 0.0));
		HOUR_OF_SPEED = (int) Math.ceil(SIMULATION_STOP_TIME / (SIMULATION_SPEED_REFRESH_INTERVAL + 0.0));
		ROADS_SHAPEFILE = loadConfig("ROADS_SHAPEFILE");
		ROADS_CSV = loadConfig("ROADS_CSV");
		LANES_SHAPEFILE = loadConfig("LANES_SHAPEFILE");
		LANES_CSV = loadConfig("LANES_CSV");
		NETWORK_FILE = loadConfig("NETWORK_FILE");
		ZONES_SHAPEFILE = loadConfig("ZONES_SHAPEFILE");
		ZONE_CSV = loadConfig("ZONE_CSV");
		CHARGER_SHAPEFILE = loadConfig("CHARGER_SHAPEFILE");
		CHARGER_CSV = loadConfig("CHARGER_CSV");
		
		INITIAL_X = Double.valueOf(loadConfig("INITIAL_X"));
		INITIAL_Y = Double.valueOf(loadConfig("INITIAL_Y"));
		
		BT_EVENT_FILE = loadConfig("BT_EVENT_FILE");
		BT_STD_FILE = loadConfig("BT_STD_FILE");
		EV_DEMAND_FILE = loadConfig("EV_DEMAND_FILE");
		GV_DEMAND_FILE = loadConfig("GV_DEMAND_FILE");
		RH_DEMAND_FILE = loadConfig("RH_DEMAND_FILE");
		RH_WAITING_TIME = loadConfig("RH_WAITING_TIME");
		RH_SHARE_PERCENTAGE = loadConfig("RH_SHARE_PERCENTAGE");
		RH_DEMAND_SHARABLE = Boolean.valueOf(loadConfig("RH_DEMAND_SHARABLE"));
		RH_DEMAND_FACTOR = Double.valueOf(loadConfig("RH_DEMAND_FACTOR"));
		
		// Default bus schedule
		BUS_SCHEDULE = loadConfig("BUS_SCHEDULE");

		// Number of vehicles
		NUM_OF_EV = Integer.valueOf(loadConfig("NUM_OF_EV"));
		NUM_OF_BUS = Integer.valueOf(loadConfig("NUM_OF_BUS"));
		
		// EV batteries
		EV_BATTERY = Integer.valueOf(loadConfig("EV_BATTERY"));
		BUS_BATTERY = Integer.valueOf(loadConfig("BUS_BATTERY"));

		// Event file, placeholder for future extension
		EVENT_FILE = loadConfig("EVENT_FILE");
		EVENT_CHECK_FREQUENCY = Integer.valueOf(loadConfig("EVENT_CHECK_FREQUENCY"));
		

		/* Operation Options */
		K_SHORTEST_PATH = Boolean.valueOf(loadConfig("K_SHORTEST_PATH"));
		COLLABORATIVE_EV = Boolean.valueOf(loadConfig("COLLABORATIVE_EV"));
		BUS_PLANNING = Boolean.valueOf(loadConfig("BUS_PLANNING"));
		PROACTIVE_RELOCATION = Boolean.valueOf(loadConfig("PROACTIVE_RELOCATION"));

		// Eco-routing Parameters
		ENABLE_ECO_ROUTING_EV = Boolean.valueOf(loadConfig("ECO_ROUTING_EV"));
		ENABLE_ECO_ROUTING_BUS = Boolean.valueOf(loadConfig("ECO_ROUTING_BUS"));
		NUM_CANDIDATE_ROUTES = Integer.valueOf(loadConfig("NUM_CANDIDATE_ROUTES"));

		// Vehicle charging
		PROACTIVE_CHARGING = Boolean.valueOf(loadConfig("PROACTIVE_CHARGING"));
		RECHARGE_LEVEL_LOW = Double.valueOf(loadConfig("RECHARGE_LEVEL_LOW"));
		RECHARGE_LEVEL_HIGH = Double.valueOf(loadConfig("RECHARGE_LEVEL_HIGH"));
		BUS_RECHARGE_LEVEL_LOW = Double.valueOf(loadConfig("BUS_RECHARGE_LEVEL_LOW"));
		BUS_RECHARGE_LEVEL_HIGH = Double.valueOf(loadConfig("BUS_RECHARGE_LEVEL_HIGH"));
		
		/* Network Partitioning */
		MULTI_THREADING = Boolean.valueOf(loadConfig("MULTI_THREADING"));
		// Load the number of partitions from the config file
		N_Partition = Integer.valueOf(loadConfig("N_PARTITION"));
		N_THREADS = Integer.valueOf(loadConfig("N_THREADS"));
		SIMULATION_NETWORK_REFRESH_INTERVAL = Integer
				.valueOf(loadConfig("SIMULATION_NETWORK_REFRESH_INTERVAL"));
	    SIMULATION_PARTITION_REFRESH_INTERVAL = Integer
				.valueOf(loadConfig("SIMULATION_PARTITION_REFRESH_INTERVAL"));
		// Maximum network partitioning interval
		SIMULATION_MAX_PARTITION_REFRESH_INTERVAL = Integer
				.valueOf(loadConfig("SIMULATION_MAX_PARTITION_REFRESH_INTERVAL"));
		// Threshold amount of vehicles that requires more frequent network partitioning
		THRESHOLD_VEHICLE_NUMBER = Integer.valueOf(loadConfig("THRESHOLD_VEHICLE_NUMBER"));
		
		/* Data collection */
		ENABLE_DATA_COLLECTION = Boolean.valueOf(loadConfig("ENABLE_DATA_COLLECTION"));
		DEBUG_DATA_BUFFER = Boolean.valueOf(loadConfig("DEBUG_DATA_BUFFER"));
		DATA_CLEANUP_REFRESH = Integer.valueOf(loadConfig("DATA_CLEANUP_REFRESH"));

		// Parameters for the JSON output file writer (similar to as the csv parameters
		// except JSON_TICK_LIMIT_PER_FILE which represents the number of ticks are
		// written in a json file)
		ENABLE_JSON_WRITE = Boolean.valueOf(loadConfig("ENABLE_JSON_WRITE"));
		JSON_DEFAULT_FILENAME = loadConfig("JSON_DEFAULT_FILENAME");
		JSON_DEFAULT_EXTENSION = loadConfig("JSON_DEFAULT_EXTENSION");
		JSON_DEFAULT_PATH = loadConfig("JSON_DEFAULT_PATH");
		JSON_TICKS_BETWEEN_TWO_RECORDS = Integer
				.valueOf(loadConfig("JSON_TICKS_BETWEEN_TWO_RECORDS"));
		JSON_FREQ_RECORD_LINK_SNAPSHOT = Integer
				.valueOf(loadConfig("JSON_FREQ_RECORD_LINK_SNAPSHOT"));
		JSON_BUFFER_REFRESH = Integer.valueOf(loadConfig("JSON_BUFFER_REFRESH"));
		JSON_TICK_LIMIT_PER_FILE = Integer.valueOf(loadConfig("JSON_TICK_LIMIT_PER_FILE"));
		
		// Parameters for aggregated report writer
		AGG_DEFAULT_PATH = loadConfig("AGG_DEFAULT_PATH");

		// Parameters for handling network connections to remote programs
		DEBUG_NETWORK = Boolean.valueOf(loadConfig("DEBUG_NETWORK"));
		NETWORK_BUFFER_RERESH = Integer.valueOf(loadConfig("NETWORK_BUFFER_REFRESH"));
		NETWORK_STATUS_REFRESH = Integer.valueOf(loadConfig("NETWORK_STATUS_REFRESH"));
		NETWORK_LISTEN_PORT = Integer.valueOf(loadConfig("NETWORK_LISTEN_PORT"));
		NETWORK_MAX_MESSAGE_SIZE = Integer.valueOf(loadConfig("NETWORK_MAX_MESSAGE_SIZE"));
			
		// Displaying useful metrics
		ENABLE_METRICS_DISPLAY = Boolean.valueOf(loadConfig("ENABLE_METRICS_DISPLAY"));
		METRICS_DISPLAY_INTERVAL = Integer.valueOf(loadConfig("METRICS_DISPLAY_INTERVAL"));
		
		/* Constants */
		// For primitive move
	    TRAVEL_PER_TURN = Double.valueOf(loadConfig("TRAVEL_PER_TURN"));
		
		// For searching nearby facilities
		SEARCHING_BUFFER = Double.valueOf(loadConfig("SEARCHING_BUFFER")); 
		// For cruising nearby links
		CRUISING_BUFFER = Double.valueOf(loadConfig("CRUISING_BUFFER")); 
	    
		// Car following status
		STATUS_REGIME_FREEFLOWING = 0x00000000; // 0
		STATUS_REGIME_CARFOLLOWING = 0x00000080; // 128
		STATUS_REGIME_EMERGENCY = 0x00000100; // 256
		
		// For car following and lane changing
		ALPHA_DEC = Float.valueOf(loadConfig("ALPHA_DEC"));
		BETA_DEC = Float.valueOf(loadConfig("BETA_DEC"));
		GAMMA_DEC = Float.valueOf(loadConfig("GAMMA_DEC"));
		ALPHA_ACC = Float.valueOf(loadConfig("ALPHA_ACC"));
		BETA_ACC = Float.valueOf(loadConfig("BETA_ACC"));
		GAMMA_ACC = Float.valueOf(loadConfig("GAMMA_ACC"));
		
		// For K_SHORTEST_PATH
		K_VALUE = Integer.valueOf(loadConfig("K_VALUE"));
		THETA_LOGIT = Double.valueOf(loadConfig("THETA_LOGIT"));
		
		// For global variables of the adaptive network weighting
		PART_ALPHA = Integer.valueOf(loadConfig("PART_ALPHA"));
		PART_BETA = Integer.valueOf(loadConfig("PART_BETA"));
		PART_GAMMA = Integer.valueOf(loadConfig("PART_GAMMA"));
		
		// Number of times that the partition interval is larger than the network
		// refresh interval
		PART_REFRESH_MULTIPLIER = (int) (SIMULATION_PARTITION_REFRESH_INTERVAL
				/ SIMULATION_NETWORK_REFRESH_INTERVAL);
		SIMULATION_MULTIPLE_DEMAND_INPUTS = Boolean
				.valueOf(loadConfig("SIMULATION_MULTIPLE_DEMAND_INPUTS"));// If this is true then we input multiple demand
																			// files for batch runs, else we run single
		// Number of future road segments to be considered in counting shadow vehicles
		N_SHADOW = Integer.valueOf(loadConfig("N_SHADOW"));
		
		// For microscopic vehicle movement
		MIN_LEAD = Float.valueOf(loadConfig("MIN_LEAD"));; // (m/sec)
		MIN_LAG = Float.valueOf(loadConfig("MIN_LAG"));; // (m/sec)
		DEFAULT_VEHICLE_WIDTH = Float.valueOf(loadConfig("DEFAULT_VEHICLE_WIDTH")); // meters
		DEFAULT_VEHICLE_LENGTH = Float.valueOf(loadConfig("DEFAULT_VEHICLE_LENGTH")); // meters
		NO_LANECHANGING_LENGTH = Float.valueOf(loadConfig("NO_LANECHANGING_LENGTH")); // meters
		LANE_WIDTH = Float.valueOf(loadConfig("LANE_WIDTH"));
		LANE_CHANGING_PROB_PART1 = Float.valueOf(loadConfig("LANE_CHANGING_PROB_PART1"));
		LANE_CHANGING_PROB_PART2 = Float.valueOf(loadConfig("LANE_CHANGING_PROB_PART1"));
		H_UPPER = Float.valueOf(loadConfig("H_UPPER"));
		H_LOWER = Float.valueOf(loadConfig("H_LOWER"));
		FLT_INF = Float.MAX_VALUE;
		FLT_EPSILON = 1.0 / FLT_INF;
		
		STREET_SPEED = Float.valueOf(loadConfig("STREET_SPEED")); // mph
		HIGHWAY_SPEED = Float.valueOf(loadConfig("HIGHWAY_SPEED")); // mph
		BRIDGE_SPEED = Float.valueOf(loadConfig("BRIDGE_SPEED")); // mph
		TUNNEL_SPEED = Float.valueOf(loadConfig("TUNNEL_SPEED")); // mph
		DRIVEWAY_SPEED = Float.valueOf(loadConfig("DRIVEWAY_SPEED")); // mph
		RAMP_SPEED = Float.valueOf(loadConfig("RAMP_SPEED")); // mph
		UTURN_SPEED = Float.valueOf(loadConfig("UTURN_SPEED")); // mph
		
		// Parameters for MLC
		betaLeadMLC01 = 0.05;
		betaLeadMLC02 = 0.15;
		betaLagMLC01 = 0.15;
		betaLagMLC02 = 0.40;
		gama = 2.5e-5;
		critDisFraction = 0.6;
		
		// Parameters for DLC
		betaLeadDLC01 = 0.05;
		betaLeadDLC02 = 0.15;
		betaLagDLC01 = 0.15;
		betaLagDLC02 = 0.40;
		minLeadDLC = 0.05;
		minLagDLC = 0.05;
		
		/* DB storage, currently is a placeholder for future extention */
		DB_URL = String.valueOf(loadConfig("DB_URL"));
		
		BLOCKAGE_SPEED_FOREVENTS = 2.0; // In miles per hour
		newEventQueue = new LinkedList<NetworkEventObject>();// Global queue
		// Parameters for mode split
		BUS_TICKET_PRICE = Double.valueOf(loadConfig("BUS_TICKET_PRICE"));
		MS_ALPHA = Double.valueOf(loadConfig("MS_ALPHA"));
		MS_BETA = Double.valueOf(loadConfig("MS_BETA"));
		BASE_PRICE_TAXI = Double.valueOf(loadConfig("BASE_PRICE_TAXI"));
		INITIAL_PRICE_TAXI = Double.valueOf(loadConfig("INITIAL_PRICE_TAXI"));
		TAXI_BASE = Double.valueOf(loadConfig("TAXI_BASE"));
		BUS_BASE = Double.valueOf(loadConfig("BUS_BASE"));
		
		// Addressing the gridlock in the parallel mode
		MAX_STUCK_TIME = (int) (Integer.valueOf(loadConfig("MAX_STUCK_TIME"))/SIMULATION_STEP_SIZE);
	}
	
	// Whether the simulation is ran in the synchronized mode
	public static boolean SYNCHRONIZED = Boolean.valueOf(loadConfig("SYNCHRONIZED"));
	
	// Whether the simulation is ran in the standalone mode
	public static boolean STANDALONE = Boolean.valueOf(loadConfig("STANDALONE"));
	
	
	// Whether the simulation is ran with V2X enabled
	public static boolean V2X = Boolean.valueOf(loadConfig("V2X"));
	
	/* Simulation setup */
	public static int RANDOM_SEED = Integer
			.valueOf(loadConfig("RANDOM_SEED"));
	public static Random RandomGenerator = new Random(RANDOM_SEED);
	
	public static float SIMULATION_STEP_SIZE = Float.valueOf(loadConfig("SIMULATION_STEP_SIZE"));
	
	public static int SIMULATION_ZONE_REFRESH_INTERVAL = (int) (Integer
			.valueOf(loadConfig("SIMULATION_ZONE_REFRESH_INTERVAL"))/SIMULATION_STEP_SIZE);
	public static int SIMULATION_DEMAND_REFRESH_INTERVAL = (int) (Integer
			.valueOf(loadConfig("SIMULATION_DEMAND_REFRESH_INTERVAL"))/SIMULATION_STEP_SIZE);
	public static int SIMULATION_SPEED_REFRESH_INTERVAL = (int) (Integer
			.valueOf(loadConfig("SIMULATION_SPEED_REFRESH_INTERVAL"))/SIMULATION_STEP_SIZE);
	public static int SIMULATION_BUS_REFRESH_INTERVAL = (int) (Integer
			.valueOf(loadConfig("SIMULATION_BUS_REFRESH_INTERVAL"))/SIMULATION_STEP_SIZE);
	public static int SIMULATION_CHARGING_STATION_REFRESH_INTERVAL = (int) (Integer
			.valueOf(loadConfig("SIMULATION_CHARGING_STATION_REFRESH_INTERVAL"))/SIMULATION_STEP_SIZE);
	public static int SIMULATION_RH_MATCHING_WINDOW = (int) (Integer
			.valueOf(loadConfig("SIMULATION_RH_MATCHING_WINDOW")) / SIMULATION_STEP_SIZE);
	public static int SIMULATION_RH_MAX_CRUISING_TIME = (int) (Integer
			.valueOf(loadConfig("SIMULATION_RH_MAX_CRUISING_TIME")) / SIMULATION_STEP_SIZE);

	public static int SIMULATION_STOP_TIME = (int) (60 * Integer.valueOf(loadConfig("SIMULATION_STOP_TIME"))
			/ SIMULATION_STEP_SIZE);
	
	public static int SIMULATION_SIGNAL_REFRESH_INTERVAL = SIMULATION_STEP_SIZE>1? 1: (int) (1 / SIMULATION_STEP_SIZE);
	
	public static boolean DEMAND_DIFFUSION = Boolean.valueOf(loadConfig("DEMAND_DIFFUSION"));
	
	public static int HOUR_OF_DEMAND = (int) Math.ceil(SIMULATION_STOP_TIME / (SIMULATION_DEMAND_REFRESH_INTERVAL + 0.0));
	public static int HOUR_OF_SPEED = (int) Math.ceil(SIMULATION_STOP_TIME / (SIMULATION_SPEED_REFRESH_INTERVAL + 0.0));
	
	// Road Network
	public static String ROADS_SHAPEFILE = loadConfig("ROADS_SHAPEFILE");
	public static String ROADS_CSV = loadConfig("ROADS_CSV");
	public static String LANES_SHAPEFILE = loadConfig("LANES_SHAPEFILE");
	public static String LANES_CSV = loadConfig("LANES_CSV");
	public static String NETWORK_FILE = loadConfig("NETWORK_FILE");
	
	public static String ZONES_SHAPEFILE = loadConfig("ZONES_SHAPEFILE");
	public static String ZONE_CSV = loadConfig("ZONE_CSV");
	public static String CHARGER_SHAPEFILE = loadConfig("CHARGER_SHAPEFILE");
	public static String CHARGER_CSV = loadConfig("CHARGER_CSV");

	public static double INITIAL_X = Double.valueOf(loadConfig("INITIAL_X"));
	public static double INITIAL_Y = Double.valueOf(loadConfig("INITIAL_Y"));
	
	// Background traffic
	public static String BT_EVENT_FILE = loadConfig("BT_EVENT_FILE");
	public static String BT_STD_FILE = loadConfig("BT_STD_FILE");

	// Travel demand
	public static String EV_DEMAND_FILE = loadConfig("EV_DEMAND_FILE");
	public static String GV_DEMAND_FILE = loadConfig("GV_DEMAND_FILE");
	public static String RH_DEMAND_FILE = loadConfig("RH_DEMAND_FILE");
	public static String RH_WAITING_TIME = loadConfig("RH_WAITING_TIME");
	public static String RH_SHARE_PERCENTAGE = loadConfig("RH_SHARE_PERCENTAGE");
	public static boolean RH_DEMAND_SHARABLE = Boolean.valueOf(loadConfig("RH_DEMAND_SHARABLE"));
	public static double RH_DEMAND_FACTOR = Double.valueOf(loadConfig("RH_DEMAND_FACTOR"));
	
	// Default bus schedule
	public static String BUS_SCHEDULE = loadConfig("BUS_SCHEDULE");

	// Number of vehicles
	public static int NUM_OF_EV = Integer.valueOf(loadConfig("NUM_OF_EV"));
	public static int NUM_OF_BUS = Integer.valueOf(loadConfig("NUM_OF_BUS"));
	
	// EV batteries
	public static int EV_BATTERY = Integer.valueOf(loadConfig("EV_BATTERY"));
	public static int BUS_BATTERY = Integer.valueOf(loadConfig("BUS_BATTERY"));

	// Event file, placeholder for future extension
	public static String EVENT_FILE = loadConfig("EVENT_FILE");
	public static int EVENT_CHECK_FREQUENCY = Integer.valueOf(loadConfig("EVENT_CHECK_FREQUENCY"));
	

	/* Operation Options */
	public static boolean K_SHORTEST_PATH = Boolean.valueOf(loadConfig("K_SHORTEST_PATH"));
	public static boolean COLLABORATIVE_EV = Boolean.valueOf(loadConfig("COLLABORATIVE_EV"));
	public static boolean BUS_PLANNING = Boolean.valueOf(loadConfig("BUS_PLANNING"));
	public static boolean PROACTIVE_RELOCATION = Boolean.valueOf(loadConfig("PROACTIVE_RELOCATION"));

	// Eco-routing Parameters
	public static boolean ENABLE_ECO_ROUTING_EV = Boolean.valueOf(loadConfig("ECO_ROUTING_EV"));
	public static boolean ENABLE_ECO_ROUTING_BUS = Boolean.valueOf(loadConfig("ECO_ROUTING_BUS"));
	public static int NUM_CANDIDATE_ROUTES = Integer.valueOf(loadConfig("NUM_CANDIDATE_ROUTES"));

	// Vehicle charging
	public static boolean PROACTIVE_CHARGING = Boolean.valueOf(loadConfig("PROACTIVE_CHARGING"));
	public static double RECHARGE_LEVEL_LOW = Double.valueOf(loadConfig("RECHARGE_LEVEL_LOW"));
	public static double RECHARGE_LEVEL_HIGH = Double.valueOf(loadConfig("RECHARGE_LEVEL_HIGH"));
	public static double BUS_RECHARGE_LEVEL_LOW = Double.valueOf(loadConfig("BUS_RECHARGE_LEVEL_LOW"));
	public static double BUS_RECHARGE_LEVEL_HIGH = Double.valueOf(loadConfig("BUS_RECHARGE_LEVEL_HIGH"));
	
	
	/* Network Partitioning */
	public static boolean MULTI_THREADING = Boolean.valueOf(loadConfig("MULTI_THREADING"));
	// Load the number of partitions from the config file
	public static int N_Partition = Integer.valueOf(loadConfig("N_PARTITION"));
	public static int N_THREADS = Integer.valueOf(loadConfig("N_THREADS"));
	public static int SIMULATION_NETWORK_REFRESH_INTERVAL = Integer
			.valueOf(loadConfig("SIMULATION_NETWORK_REFRESH_INTERVAL"));
	public static int SIMULATION_PARTITION_REFRESH_INTERVAL = Integer
			.valueOf(loadConfig("SIMULATION_PARTITION_REFRESH_INTERVAL"));
	// Maximum network partitioning interval
	public static int SIMULATION_MAX_PARTITION_REFRESH_INTERVAL = Integer
			.valueOf(loadConfig("SIMULATION_MAX_PARTITION_REFRESH_INTERVAL"));
	// Threshold amount of vehicles that requires more frequent network partitioning
	public static int THRESHOLD_VEHICLE_NUMBER = Integer.valueOf(loadConfig("THRESHOLD_VEHICLE_NUMBER"));
	
	/* Data collection */
	public static boolean ENABLE_DATA_COLLECTION = Boolean.valueOf(loadConfig("ENABLE_DATA_COLLECTION"));
	public static boolean DEBUG_DATA_BUFFER = Boolean.valueOf(loadConfig("DEBUG_DATA_BUFFER"));
	public static int DATA_CLEANUP_REFRESH = Integer.valueOf(loadConfig("DATA_CLEANUP_REFRESH"));

	// Parameters for the JSON output file writer (similar to as the csv parameters
	// except JSON_TICK_LIMIT_PER_FILE which represents the number of ticks are
	// written in a json file)
	public static boolean ENABLE_JSON_WRITE = Boolean.valueOf(loadConfig("ENABLE_JSON_WRITE"));
	public static String JSON_DEFAULT_FILENAME = loadConfig("JSON_DEFAULT_FILENAME");
	public static String JSON_DEFAULT_EXTENSION = loadConfig("JSON_DEFAULT_EXTENSION");
	public static String JSON_DEFAULT_PATH = loadConfig("JSON_DEFAULT_PATH");
	public static int JSON_TICKS_BETWEEN_TWO_RECORDS = Integer
			.valueOf(loadConfig("JSON_TICKS_BETWEEN_TWO_RECORDS"));
	public static int JSON_FREQ_RECORD_LINK_SNAPSHOT = Integer
			.valueOf(loadConfig("JSON_FREQ_RECORD_LINK_SNAPSHOT"));
	public static int JSON_BUFFER_REFRESH = Integer.valueOf(loadConfig("JSON_BUFFER_REFRESH"));
	public static int JSON_TICK_LIMIT_PER_FILE = Integer.valueOf(loadConfig("JSON_TICK_LIMIT_PER_FILE"));
	
	// Parameters for aggregated report writer
	public static String AGG_DEFAULT_PATH = loadConfig("AGG_DEFAULT_PATH");

	// Parameters for handling network connections to remote programs
	public static boolean DEBUG_NETWORK = Boolean.valueOf(loadConfig("DEBUG_NETWORK"));
	public static int NETWORK_BUFFER_RERESH = Integer.valueOf(loadConfig("NETWORK_BUFFER_REFRESH"));
	public static int NETWORK_STATUS_REFRESH = Integer.valueOf(loadConfig("NETWORK_STATUS_REFRESH"));
	public static int NETWORK_LISTEN_PORT = Integer.valueOf(loadConfig("NETWORK_LISTEN_PORT"));
	public static int NETWORK_MAX_MESSAGE_SIZE = Integer.valueOf(loadConfig("NETWORK_MAX_MESSAGE_SIZE"));
		
	// Displaying useful metrics
	public static boolean ENABLE_METRICS_DISPLAY = Boolean.valueOf(loadConfig("ENABLE_METRICS_DISPLAY"));
	public static int METRICS_DISPLAY_INTERVAL = Integer.valueOf(loadConfig("METRICS_DISPLAY_INTERVAL"));
	
	/* Constants */
	// For primitive move
    public static double TRAVEL_PER_TURN = Double.valueOf(loadConfig("TRAVEL_PER_TURN"));
	
	// For searching nearby facilities
	public static double SEARCHING_BUFFER = Double.valueOf(loadConfig("SEARCHING_BUFFER")); 
	// For cruising nearby links
	public static double CRUISING_BUFFER = Double.valueOf(loadConfig("CRUISING_BUFFER")); 
    
	// Car following status
	public static int STATUS_REGIME_FREEFLOWING = 0x00000000; // 0
	public static int STATUS_REGIME_CARFOLLOWING = 0x00000080; // 128
	public static int STATUS_REGIME_EMERGENCY = 0x00000100; // 256
	
	// For car following and lane changing
	public static float ALPHA_DEC = Float.valueOf(loadConfig("ALPHA_DEC"));
	public static float BETA_DEC = Float.valueOf(loadConfig("BETA_DEC"));
	public static float GAMMA_DEC = Float.valueOf(loadConfig("GAMMA_DEC"));
	public static float ALPHA_ACC = Float.valueOf(loadConfig("ALPHA_ACC"));
	public static float BETA_ACC = Float.valueOf(loadConfig("BETA_ACC"));
	public static float GAMMA_ACC = Float.valueOf(loadConfig("GAMMA_ACC"));
	
	// For K_SHORTEST_PATH
	public static int K_VALUE = Integer.valueOf(loadConfig("K_VALUE"));
	public static double THETA_LOGIT = Double.valueOf(loadConfig("THETA_LOGIT"));
	
	// For global variables of the adaptive network weighting
	public static int PART_ALPHA = Integer.valueOf(loadConfig("PART_ALPHA"));
	public static int PART_BETA = Integer.valueOf(loadConfig("PART_BETA"));
	public static int PART_GAMMA = Integer.valueOf(loadConfig("PART_GAMMA"));
	
	// Number of times that the partition interval is larger than the network
	// refresh interval
	public static int PART_REFRESH_MULTIPLIER = (int) (SIMULATION_PARTITION_REFRESH_INTERVAL
			/ SIMULATION_NETWORK_REFRESH_INTERVAL);
	public static boolean SIMULATION_MULTIPLE_DEMAND_INPUTS = Boolean
			.valueOf(loadConfig("SIMULATION_MULTIPLE_DEMAND_INPUTS"));// If this is true then we input multiple demand
																		// files for batch runs, else we run single
	// Number of future road segments to be considered in counting shadow vehicles
	public static int N_SHADOW = Integer.valueOf(loadConfig("N_SHADOW"));
	
	// For microscopic vehicle movement
	public static double MIN_LEAD = Float.valueOf(loadConfig("MIN_LEAD"));; // (m/sec)
	public static double MIN_LAG = Float.valueOf(loadConfig("MIN_LAG"));; // (m/sec)
	public static float DEFAULT_VEHICLE_WIDTH = Float.valueOf(loadConfig("DEFAULT_VEHICLE_WIDTH")); // meters
	public static float DEFAULT_VEHICLE_LENGTH = Float.valueOf(loadConfig("DEFAULT_VEHICLE_LENGTH")); // meters
	public static float NO_LANECHANGING_LENGTH = Float.valueOf(loadConfig("NO_LANECHANGING_LENGTH")); // meters
	public static float LANE_WIDTH = Float.valueOf(loadConfig("LANE_WIDTH"));
	public static float LANE_CHANGING_PROB_PART1 = Float.valueOf(loadConfig("LANE_CHANGING_PROB_PART1"));
	public static float LANE_CHANGING_PROB_PART2 = Float.valueOf(loadConfig("LANE_CHANGING_PROB_PART1"));
	public static float H_UPPER = Float.valueOf(loadConfig("H_UPPER"));
	public static float H_LOWER = Float.valueOf(loadConfig("H_LOWER"));
	public static double FLT_INF = Float.MAX_VALUE;
	public static double FLT_EPSILON = 1.0 / FLT_INF;
	
	public static double STREET_SPEED = Float.valueOf(loadConfig("STREET_SPEED")); // mph
	public static double HIGHWAY_SPEED = Float.valueOf(loadConfig("HIGHWAY_SPEED")); // mph
	public static double BRIDGE_SPEED = Float.valueOf(loadConfig("BRIDGE_SPEED")); // mph
	public static double TUNNEL_SPEED = Float.valueOf(loadConfig("TUNNEL_SPEED")); // mph
	public static double DRIVEWAY_SPEED = Float.valueOf(loadConfig("DRIVEWAY_SPEED")); // mph
	public static double RAMP_SPEED = Float.valueOf(loadConfig("RAMP_SPEED")); // mph
	public static double UTURN_SPEED = Float.valueOf(loadConfig("UTURN_SPEED")); // mph
	
	// Parameters for MLC
	public static double betaLeadMLC01 = 0.05;
	public static double betaLeadMLC02 = 0.15;
	public static double betaLagMLC01 = 0.15;
	public static double betaLagMLC02 = 0.40;
	public static double gama = 2.5e-5;
	public static double critDisFraction = 0.6;
	
	// Parameters for DLC
	public static double betaLeadDLC01 = 0.05;
	public static double betaLeadDLC02 = 0.15;
	public static double betaLagDLC01 = 0.15;
	public static double betaLagDLC02 = 0.40;
	public static double minLeadDLC = 0.05;
	public static double minLagDLC = 0.05;
	
	// DB storage, currently is a placeholder for future extention 
	public static String DB_URL = String.valueOf(loadConfig("DB_URL"));
	
	public static double BLOCKAGE_SPEED_FOREVENTS = 2.0; // In miles per hour
	public static LinkedList<NetworkEventObject> newEventQueue = new LinkedList<NetworkEventObject>();
	
	// Parameters for mode split
	public static double BUS_TICKET_PRICE = Double.valueOf(loadConfig("BUS_TICKET_PRICE"));
	public static double MS_ALPHA = Double.valueOf(loadConfig("MS_ALPHA"));
	public static double MS_BETA = Double.valueOf(loadConfig("MS_BETA"));
	public static double BASE_PRICE_TAXI = Double.valueOf(loadConfig("BASE_PRICE_TAXI"));
	public static double INITIAL_PRICE_TAXI = Double.valueOf(loadConfig("INITIAL_PRICE_TAXI"));
	public static double TAXI_BASE = Double.valueOf(loadConfig("TAXI_BASE"));
	public static double BUS_BASE = Double.valueOf(loadConfig("BUS_BASE"));
	
	// Addressing the gridlock in the parallel mode
	public static int MAX_STUCK_TIME = (int) (Integer.valueOf(loadConfig("MAX_STUCK_TIME"))/SIMULATION_STEP_SIZE);
}
