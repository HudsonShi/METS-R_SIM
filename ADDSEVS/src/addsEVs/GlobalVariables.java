package addsEVs;

import java.util.Random;

/**
 * Central location for any useful variables (e.g. filenames).
 * 
 * @author Nick Malleson
 * @author Samiul Hasan (SH)
 * @author Xianyuan Zhan
 * Above are authors for A-RESCUE
 * @author Zengxiang Lei
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
 * 		 public static final int test = Integer.valueOf(loadConfig("test"))
 * The loadConfig method is used to load property's value from the config file, and data type transform is required. Useful transform method is list below:
 * To integer: Integer.valueOf(String s)
 * To Float: Float.valueOf(String s)
 * To Double: Double.valueOf(String s)
 * To Boolean: Boolean.valueOf(String s)
 * to single char: .atChar(0), e.g.: public static final char whosRunning = loadConfig("whosRunning").charAt(0);
 * 
 * Note: 1. The value for each property in Config File should only include numeric data or string, such content of "/", "0.05f", "0.05 " cannot be reconized
 *       2. To add comment in any content, use "#", instead of "//" or "/*...". e.g. : "#Input Files"
 */

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

public class GlobalVariables {
private static Properties config;
    
	// Loading properties from configuration files, initialized used in ARESCUE credit to Xianyuan Zhan and Christopher Thompson.
	private static String loadConfig(String property) {
	    if (config == null) {
	        config = new Properties();
	        try {
	        	String working_dir = System.getProperty("user.dir");
	        	config.load(new FileInputStream(working_dir+"/data/Data.properties"));

		    } catch (IOException ex) {
			    ex.printStackTrace();
		    }
	    }
	    
		return config.getProperty(property);
	}

	// Input Files
	public static final String ROADS_SHAPEFILE = loadConfig("ROADS_SHAPEFILE");

	public static final String LANES_SHAPEFILE = loadConfig("LANES_SHAPEFILE");

	public static final String ZONES_SHAPEFILE = loadConfig("ZONES_SHAPEFILE");
	
	public static final String CHARGER_SHAPEFILE = loadConfig("CHARGER_SHAPEFILE");
	
	public static final String CHARGER_CSV = loadConfig("CHARGER_CSV");
	
	public static final String ROADS_CSV = loadConfig("ROADS_CSV");

	public static final String LANES_CSV = loadConfig("LANES_CSV");
	
	public static final String ZONE_CSV = loadConfig("ZONE_CSV");
	
	public static final String BUS_SCHEDULE = loadConfig("BUS_SCHEDULE");

	// Path for the traffic incidence events, unused in the project
	public static final String EVENT_FILE = loadConfig("EVENT_FILE");
	public static final int EVENT_CHECK_FREQUENCY = Integer.valueOf(loadConfig("EVENT_CHECK_FREQUENCY"));
	
	// Path for background traffic event files
	public static final String BT_EVENT_FILE=loadConfig("BT_EVENT_FILE");
	public static final String BT_STD_FILE=loadConfig("BT_STD_FILE");
	public static final String DM_EVENT_FILE=loadConfig("DM_EVENT_FILE");
	
	// Simulation Setup
	public static final Random RandomGenerator = new Random(123456777); // random generator
	
	public static final String AGG_DEFAULT_PATH = loadConfig("AGG_DEFAULT_PATH");
	
	public static final float SIMULATION_STEP_SIZE = Float
			.valueOf(loadConfig("SIMULATION_STEP_SIZE"));
	
	public static final int SIMULATION_PASSENGER_ARRIVAL_INTERVAL =  Integer
			.valueOf(loadConfig("SIMULATION_PASSENGER_ARRIVAL_INTERVAL"));
	
	public static final int SIMULATION_PASSENGER_SERVE_INTERVAL =  Integer
			.valueOf(loadConfig("SIMULATION_PASSENGER_SERVE_INTERVAL"));
	
	public static final int SIMULATION_NETWORK_REFRESH_INTERVAL = Integer
			.valueOf(loadConfig("SIMULATION_NETWORK_REFRESH_INTERVAL"));
	
	public static final int SIMULATION_CHARGING_STATION_REFRESH_INTERVAL = Integer
			.valueOf(loadConfig("SIMULATION_CHARGING_STATION_REFRESH_INTERVAL"));
	
	public static final int SIMULATION_PARTITION_REFRESH_INTERVAL = Integer
			.valueOf(loadConfig("SIMULATION_PARTITION_REFRESH_INTERVAL"));
	
	// Maximum network partitioning interval
	public static final int SIMULATION_MAX_PARTITION_REFRESH_INTERVAL = Integer
			.valueOf(loadConfig("SIMULATION_MAX_PARTITION_REFRESH_INTERVAL"));
	// Threshold amount of vehicles that requires more frequent network partitioning
	public static final int THRESHOLD_VEHICLE_NUMBER = Integer
			.valueOf(loadConfig("THRESHOLD_VEHICLE_NUMBER"));
	public static int SIMULATION_SLEEPS = Integer
			.valueOf(loadConfig("SIMULATION_SLEEPS"));// This variable decides if simulator pauses for sometime to listen. If zero then it waits, else it moves forward. 
	
	// For global variables of the adaptive network weighting
	public static final int PART_ALPHA = Integer
			.valueOf(loadConfig("PART_ALPHA"));
	public static final int PART_BETA = Integer
			.valueOf(loadConfig("PART_BETA"));
	public static final int PART_GAMMA = Integer
			.valueOf(loadConfig("PART_GAMMA"));
	// Number of times that the partition interval is larger than the network refresh interval
	public static final int PART_REFRESH_MULTIPLIER = (int) (SIMULATION_PARTITION_REFRESH_INTERVAL / SIMULATION_NETWORK_REFRESH_INTERVAL);
	public static final int SIMULATION_INTERVAL_SIZE = Integer
			.valueOf(loadConfig("SIMULATION_INTERVAL_SIZE"));
	public static final int SIMULATION_STOP_TIME = Integer
			.valueOf(loadConfig("SIMULATION_STOP_TIME"));
	public static final double TRAVEL_PER_TURN = Double
			.valueOf(loadConfig("TRAVEL_PER_TURN"));

	public static final int Global_Vehicle_ID = Integer
			.valueOf(loadConfig("Global_Vehicle_ID"));
	public static final int Global_Road_ID = Integer
			.valueOf(loadConfig("Global_Road_ID"));
	public static final boolean Debug_On_Road = Boolean
			.valueOf(loadConfig("Debug_On_Road"));

	public static final double XXXX_BUFFER = Double
			.valueOf(loadConfig("XXXX_BUFFER")); // Used in CityContext.getRoadAtCoords()
	public static final boolean MULTI_THREADING = Boolean
			.valueOf(loadConfig("MULTI_THREADING"));
	
	public static final int N_THREADS = Integer
			.valueOf(loadConfig("N_THREADS"));
	
	// Load the number of partitions from the config file
	public static final int N_Partition = Integer.valueOf(loadConfig("N_PARTITION"));

	public static final float FREE_SPEED = Float
			.valueOf(loadConfig("FREE_SPEED"));

	public static final float MAX_ACCELERATION = Float
			.valueOf(loadConfig("MAX_ACCELERATION")); // meter/sec2
	public static final float MAX_DECELERATION = Float
			.valueOf(loadConfig("MAX_DECELERATION")); // meter/sec2

	public static final float DEFAULT_VEHICLE_WIDTH = Float
			.valueOf(loadConfig("DEFAULT_VEHICLE_WIDTH")); // meters
	public static final float DEFAULT_VEHICLE_LENGTH = Float
			.valueOf(loadConfig("DEFAULT_VEHICLE_LENGTH")); // meters
	public static final float INTERSECTION_BUFFER_LENGTH = Float
			.valueOf(loadConfig("INTERSECTION_BUFFER_LENGTH")); //meters
	public static final float NO_LANECHANGING_LENGTH = Float
			.valueOf(loadConfig("NO_LANECHANGING_LENGTH")); //meters
	public static final float MIN_UTURN_LENGTH = Float
			.valueOf(loadConfig("MIN_UTURN_LENGTH")); //meters

	public static final double SPEED_EPSILON = Double
			.valueOf(loadConfig("SPEED_EPSILON")); // meter/sec
	public static final double ACC_EPSILON = Double
			.valueOf(loadConfig("ACC_EPSILON")); // meter/sec2

	public static final float LANE_WIDTH = Float
			.valueOf(loadConfig("LANE_WIDTH"));

	public static final float H_UPPER = Float.valueOf(loadConfig("H_UPPER"));
	public static final float H_LOWER = Float.valueOf(loadConfig("H_LOWER"));

	public static final double FLT_INF = Float.MAX_VALUE;
	public static final double FLT_EPSILON = 1.0 / FLT_INF;

	public static final int STATUS_REGIME_FREEFLOWING = 0x00000000; // 0
	public static final int STATUS_REGIME_CARFOLLOWING = 0x00000080; // 128
	public static final int STATUS_REGIME_EMERGENCY = 0x00000100; // 256

	public static final float ALPHA_DEC = Float
			.valueOf(loadConfig("ALPHA_DEC"));
	public static final float BETA_DEC = Float.valueOf(loadConfig("BETA_DEC"));
	public static final float GAMMA_DEC = Float
			.valueOf(loadConfig("GAMMA_DEC"));

	public static final float ALPHA_ACC = Float
			.valueOf(loadConfig("ALPHA_ACC"));
	public static final float BETA_ACC = Float.valueOf(loadConfig("BETA_ACC"));
	public static final float GAMMA_ACC = Float
			.valueOf(loadConfig("GAMMA_ACC"));

	public static final boolean SINGLE_SHORTEST_PATH = Boolean
			.valueOf(loadConfig("SINGLE_SHORTEST_PATH")); 
	public static final boolean K_SHORTEST_PATH = Boolean
			.valueOf(loadConfig("K_SHORTEST_PATH")); 
	
	public static final int K_VALUE = Integer.valueOf(loadConfig("K_VALUE"));
	public static final double THETA_LOGIT = Double
			.valueOf(loadConfig("THETA_LOGIT"));
	
	// Number of future road segments to be considered in counting shadow vehicles
	public static final int N_SHADOW = Integer.valueOf(loadConfig("N_SHADOW"));

	// Following are parameters used in lane changing model
	public static final double minLead = 3.0; // (m/sec)
	public static final double minLag = 5.0; // (m/sec)

	// Parameters for MLC
	public static final double betaLeadMLC01 = 0.05;
	public static final double betaLeadMLC02 = 0.15;
	public static final double betaLagMLC01 = 0.15;
	public static final double betaLagMLC02 = 0.40;
	public static final double gama = 2.5e-5;
	public static final double critDisFraction = 0.6;

	// Parameters for DLC
	public static final double betaLeadDLC01 = 0.05;
	public static final double betaLeadDLC02 = 0.15;
	public static final double betaLagDLC01 = 0.15;
	public static final double betaLagDLC02 = 0.40;
	public static final double minLeadDLC = 0.05;
	public static final double minLagDLC = 0.05;

	public static final String DB_URL = String.valueOf(loadConfig("DB_URL"));

	public static final boolean SET_DEMAND_FROM_ACTIVITY_MODELS = Boolean
			.valueOf(loadConfig("SET_DEMAND_FROM_ACTIVITY_MODELS"));
	
	// Parameters for the data collection buffer
	public static final boolean ENABLE_DATA_COLLECTION = 
			Boolean.valueOf(loadConfig("ENABLE_DATA_COLLECTION"));
	public static final boolean DEBUG_DATA_BUFFER =
	        Boolean.valueOf(loadConfig("DEBUG_DATA_BUFFER"));
	public static final int DATA_CLEANUP_REFRESH =
	        Integer.valueOf(loadConfig("DATA_CLEANUP_REFRESH"));
	
	// Parameters for the CSV output file writer
	public static final boolean ENABLE_CSV_WRITE =
	        Boolean.valueOf(loadConfig("ENABLE_CSV_WRITE"));
	public static final String CSV_DEFAULT_FILENAME =
	        loadConfig("CSV_DEFAULT_FILENAME");
	public static final String CSV_DEFAULT_EXTENSION =
	        loadConfig("CSV_DEFAULT_EXTENSION");
	public static final String CSV_DEFAULT_PATH = 
	        loadConfig("CSV_DEFAULT_PATH");
	public static final int CSV_BUFFER_REFRESH =
	        Integer.valueOf(loadConfig("CSV_BUFFER_REFRESH"));
	public static final int CSV_LINE_LIMIT =
	        Integer.valueOf(loadConfig("CSV_LINE_LIMIT"));
	
	// Parameters for the JSON output file writer (similar as the csv parameters except JSON_TICK_LIMIT_PER_FILE which represents the number of ticks are written in a json file)
		public static final boolean ENABLE_JSON_WRITE =
		        Boolean.valueOf(loadConfig("ENABLE_JSON_WRITE"));
		public static final String JSON_DEFAULT_FILENAME =
		        loadConfig("JSON_DEFAULT_FILENAME");
		public static final String JSON_DEFAULT_EXTENSION =
		        loadConfig("JSON_DEFAULT_EXTENSION");
		public static final String JSON_DEFAULT_PATH = 
		        loadConfig("JSON_DEFAULT_PATH");
		public static final int JSON_BUFFER_REFRESH =
		        Integer.valueOf(loadConfig("JSON_BUFFER_REFRESH"));
		public static final int JSON_TICK_LIMIT_PER_FILE =
		        Integer.valueOf(loadConfig("JSON_TICK_LIMIT_PER_FILE"));
	
	// Parameters for handling network connections to remote programs
	public static final boolean DEBUG_NETWORK =
	        Boolean.valueOf(loadConfig("DEBUG_NETWORK"));
	public static final boolean ENABLE_NETWORK =
	        Boolean.valueOf(loadConfig("ENABLE_NETWORK"));
	public static final int NETWORK_BUFFER_RERESH =
	        Integer.valueOf(loadConfig("NETWORK_BUFFER_REFRESH"));
	public static final int NETWORK_STATUS_REFRESH =
	        Integer.valueOf(loadConfig("NETWORK_STATUS_REFRESH"));
	public static final int NETWORK_LISTEN_PORT =
	        Integer.valueOf(loadConfig("NETWORK_LISTEN_PORT"));
	public static final int NETWORK_MAX_MESSAGE_SIZE =
	        Integer.valueOf(loadConfig("NETWORK_MAX_MESSAGE_SIZE"));
	
	// Parameter to determine how frequently (in terms of ticks) we should separately record the snapshot.
	public static final int FREQ_RECORD_VEH_SNAPSHOT_FORVIZ = 
			Integer.valueOf(loadConfig("FREQ_RECORD_VEH_SNAPSHOT_FORVIZ"));
	public static final int FREQ_RECORD_LINK_SNAPSHOT_FORVIZ = 
			Integer.valueOf(loadConfig("FREQ_RECORD_LINK_SNAPSHOT_FORVIZ"));
	
	public static double datacollection_start = 0.0;
	public static double datacollection_total = 0.0;	
	
	public static final double BLOCKAGE_SPEED_FOREVENTS = 2.0; //In miles per hour
	
	public static LinkedList<NetworkEventObject> newEventQueue = new LinkedList<NetworkEventObject>();//Global queue for storing events
	
	// Parameters for handling multiclass routing. Note that the proportion of original routing vehicles being generated is equal to 1 - (PROPORTION_OF_PREDEFINED_ROUTING_VEHICLES + PROPORTION_OF_LESS_FREQUENT_ROUTING_VEHICLES). 
	public static final boolean ENABLE_MULTICLASS_ROUTING =
	        Boolean.valueOf(loadConfig("ENABLE_MULTICLASS_ROUTING"));
	public static final double PROPORTION_OF_PREDEFINED_ROUTING_VEHICLES =
	        Double.valueOf(loadConfig("PROPORTION_OF_PREDEFINED_ROUTING_VEHICLES"));
	public static final double PROPORTION_OF_LESS_FREQUENT_ROUTING_VEHICLES =
	        Double.valueOf(loadConfig("PROPORTION_OF_LESS_FREQUENT_ROUTING_VEHICLES"));
	public static final double PROBABILITY_OF_UPDATING_ROUTING =
	        Double.valueOf(loadConfig("PROBABILITY_OF_UPDATING_ROUTING"));
	
	// Parameters for multi-class vehicles (having different parameters).
	public static final boolean ENABLE_MULTICLASS_VEHICLES_DIFF_PARAMETERS =
	        Boolean.valueOf(loadConfig("ENABLE_MULTICLASS_VEHICLES_DIFF_PARAMETERS"));
	public static final double RATIO_OF_ORIGINIAL_CLASS =
	        Double.valueOf(loadConfig("RATIO_OF_ORIGINIAL_CLASS"));
	public static final float MAX_ACCELERATION_2 = Float
			.valueOf(loadConfig("MAX_ACCELERATION_2")); // meter/sec2
	public static final float MAX_DECELERATION_2 = Float
			.valueOf(loadConfig("MAX_DECELERATION_2")); // meter/sec2
	
	// Flags for enabling operational algorithms
	public static final boolean ENABLE_ECO_ROUTING_EV = Boolean
			.valueOf(loadConfig("ECO_ROUTING_EV"));
	
	public static final boolean ENABLE_ECO_ROUTING_BUS = Boolean
			.valueOf(loadConfig("ECO_ROUTING_BUS"));
	
	public static final boolean BUS_PLANNING = Boolean.valueOf(loadConfig("BUS_PLANNING"));
	
	// Displaying useful metrics
	public static final boolean ENABLE_METRICS_DISPLAY =
			Boolean.valueOf(loadConfig("ENABLE_METRICS_DISPLAY"));
	public static final int METRICS_DISPLAY_INTERVAL =
			Integer.valueOf(loadConfig("METRICS_DISPLAY_INTERVAL"));

	// Designating the hub ID in ZONE_SHP, JFK, LGA, Penn
	public static final List<Integer> HUB_INDEXES = new ArrayList<Integer>(Arrays.asList(131,140,180));
	
	// Number of eco-routing's candidate routes
	public static int NUM_CANDIDATE_ROUTES = Integer.valueOf(loadConfig("NUM_CANDIDATE_ROUTES"));
	
	// Demand factor for passenger generation
	public static final double PASSENGER_DEMAND_FACTOR = Double.valueOf(loadConfig("PASSENGER_DEMAND_FACTOR"));
	public static final double PASSENGER_SHARE_PERCENTAGE = Double.valueOf(loadConfig("PASSENGER_SHARE_PERCENTAGE"));
	
	public static final int MAX_STUCK_TIME = Integer.valueOf(loadConfig("MAX_STUCK_TIME"));
	
	// Demand prediction related global variables
	public static final String DEMAND_PREDICTION_RESULTS_PATH = loadConfig("DEMAND_PREDICTION_RESULTS_PATH");
	public static final String DEMAND_PREDICTION_HUBS =  loadConfig("DEMAND_PREDICTION_HUBS");
	
	public static int SERVE_PASS = 0;
	public static int EV_BATTERY = Integer.valueOf(loadConfig("EV_BATTERY"));
	public static int BUS_BATTERY = Integer.valueOf(loadConfig("BUS_BATTERY"));
	public static int NUM_OF_EV = Integer.valueOf(loadConfig("NUM_OF_EV"));
	public static int NUM_OF_BUS = Integer.valueOf(loadConfig("NUM_OF_BUS"));
	
	public static int HOUR_OF_SPEED = Integer.valueOf(loadConfig("HOUR_OF_SPEED"));
	public static int HOUR_OF_DEMAND = Integer.valueOf(loadConfig("HOUR_OF_DEMAND"));
	public static int NUM_OF_ZONE = Integer.valueOf(loadConfig("NUM_OF_ZONE"));
}
