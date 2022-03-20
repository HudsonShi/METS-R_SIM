package addsEVs;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import javax.measure.unit.NonSI;
import javax.measure.unit.SI;

import repast.simphony.context.Context;
import repast.simphony.dataLoader.ContextBuilder;
import repast.simphony.engine.environment.RunEnvironment;
import repast.simphony.engine.schedule.ISchedule;
import repast.simphony.engine.schedule.ScheduleParameters;
import repast.simphony.space.gis.Geography;
import repast.simphony.space.graph.Network;

import org.apache.log4j.Logger;
import org.geotools.referencing.GeodeticCalculator;
import org.json.simple.JSONObject;

import com.vividsolutions.jts.geom.Coordinate;

import addsEVs.GlobalVariables;
import addsEVs.citycontext.*;
import addsEVs.data.*;
import addsEVs.partition.*;
import addsEVs.routing.RouteV;
import addsEVs.vehiclecontext.*;

public class ContextCreator implements ContextBuilder<Object> {
	public static BufferedWriter ev_logger; // Vehicle Trip logger

	public static BufferedWriter bus_logger; // Vehicle Trip logger

	public static BufferedWriter link_logger; // Link energy logger

	public static BufferedWriter network_logger; // Road network vehicle logger

	public static BufferedWriter zone_logger; // Zone logger

	public static BufferedWriter charger_logger; // Charger logger

	public static Logger logger = Logger.getLogger(ContextCreator.class);

	private static Context<Object> mainContext; // Useful to keep a reference to the main context

	private static int agentID = 0; // Used to generate unique agent ids

	public static double startTime; // Start time of the simulation

	private static int duration_ = (int) (3600 / GlobalVariables.SIMULATION_STEP_SIZE); // Refreshing the background speed per hour (3600s)

	private static int duration02_ = GlobalVariables.SIMULATION_NETWORK_REFRESH_INTERVAL; // Refreshing the network for routing

	private static int duration03_ = GlobalVariables.SIMULATION_PARTITION_REFRESH_INTERVAL; // Time gap to repartition the network

	// Creating the network partitioning object
	public static MetisPartition partitioner = new MetisPartition(GlobalVariables.N_Partition);

	// Creating the event handler object
	public static NetworkEventHandler eventHandler = new NetworkEventHandler();

	// Reading background traffic file into treemap
	public static BackgroundTraffic backgroundTraffic = new BackgroundTraffic();

	// Reading travel demand into treemaps
	public static BackgroundDemand backgroundDemand = new BackgroundDemand();

	// Reading bus schedule
	public static BusSchedule busSchedule = new BusSchedule();

	CityContext cityContext;
	VehicleContext vehicleContext;
	DataCollectionContext dataContext;

	// Candidate path sets for eco-routing, id: origin-destination pair, value: n paths, each path is a list of LinkID
	public static HashMap<String, List<List<Integer>>> route_UCB = new HashMap<String, List<List<Integer>>>(); 

	// Candidate path sets for transit eco-routing, id: origin-destination pari, value: n paths, each path is a list of LinkID
	public static HashMap<String, List<List<Integer>>> route_UCB_bus = new HashMap<String, List<List<Integer>>>(); 

	// A volatile (thread-read-safe) flag to check if the route_UCB is fully populated
	public static volatile boolean isRouteUCBPopulated = false;
	//public static volatile boolean isRouteUCBBusPopulated = false;

	// Route results received from RemoteDataClient
	public static ConcurrentHashMap<String, Integer> routeResult_received = new ConcurrentHashMap<String, Integer>();
	//public static ConcurrentHashMap<String, Integer> routeResult_received_bus = new ConcurrentHashMap<String, Integer>();
	
	// Reading simulation properties stored at data/Data.properties
	public Properties readPropertyFile() {
		Properties propertiesFile = new Properties();
		String working_dir = System.getProperty("user.dir");
		try {
			propertiesFile.load(new FileInputStream(working_dir + "/data/Data.properties"));
		} catch (Exception e) {
			e.printStackTrace();
		}
		return propertiesFile;
	}
	
	// Pausing the simulation
	public void handleSimulationSleep() {
		while (GlobalVariables.SIMULATION_SLEEPS == 0) {
			try {
				Thread.sleep(1000);
				System.out.println("Waiting for visualization");
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}
	}
	
	// Initializing sub-components, including: city (network + zone), vehicle, data collector
	public void buildSubContexts() {
		// create city context
		this.cityContext = new CityContext();
		mainContext.addSubContext(cityContext);
		this.cityContext.createSubContexts();
		this.cityContext.buildRoadNetwork();
		this.cityContext.createNearestRoadCoordCache();
		// create vehicle context
		this.vehicleContext = new VehicleContext();
		mainContext.addSubContext(vehicleContext);

		// create data collection context
		this.dataContext = new DataCollectionContext();
		mainContext.addSubContext(dataContext);

		// update the zone info
		for (ArrayList<Integer> route : busSchedule.busRoute) {
			int i = 0;
			for (int zoneID : route) {
				Zone zone = ContextCreator.getCityContext().findHouseWithDestID(zoneID);
				zone.setBusInfo(busSchedule.busGap.get(i));
				i += 1;
			}
		}

		// create the map of <(O,D), k potential paths>
		createUCBRoutes(); // generate UCB routes before the simulation is started, we store one copy of it to save time
		// createUCBBusRoutes(); // generate Bus UCB routes before the simulation is started, we store one copy of it to save time

		// create output files
		String outDir = GlobalVariables.AGG_DEFAULT_PATH;
		String timestamp = new SimpleDateFormat("YYYY-MM-dd-hh-mm-ss").format(new Date()); // get the current timestamp
		String outpath = outDir + File.separatorChar + GlobalVariables.NUM_OF_EV + "_" + GlobalVariables.NUM_OF_BUS; // create the overall file path, named after the demand filename
		try {
			Files.createDirectories(Paths.get(outpath));
		} catch (IOException e1) {
			e1.printStackTrace();
		}

		// initialize Vehicle Logger
		logger.info("Vehicle logger creating...");
		try {
			FileWriter fw = new FileWriter(outpath + File.separatorChar + "EVLog-" + timestamp + ".csv", false);
			ev_logger = new BufferedWriter(fw);
			ev_logger.write("tick,vehicleID,tripType,originID,destID,distance,departureTime,cost,choice,passNum");
			ev_logger.newLine();
			ev_logger.flush();
			logger.info("EV logger created!");
		} catch (IOException e) {
			e.printStackTrace();
			logger.error("EV logger failed.");
		}

		logger.info("Bus logger creating...");
		try {
			FileWriter fw = new FileWriter(outpath + File.separatorChar + "BusLog-" + timestamp + ".csv", false);
			bus_logger = new BufferedWriter(fw);
			bus_logger.write(
					"tick,vehicleID,routeID,tripType,originID,destID,distance,departureTime,cost,choice,passOnBoard");
			bus_logger.newLine();
			bus_logger.flush();
			logger.info("Bus logger created!");
		} catch (IOException e) {
			e.printStackTrace();
			logger.error("Bus logger failed.");
		}

		// initialize EnergyLogger
	    logger.info("Link energy logger creating...");
		try {
			FileWriter fw = new FileWriter(outpath + File.separatorChar + "LinkLog-" + timestamp + ".csv", false);
			link_logger = new BufferedWriter(fw);
			link_logger.write("tick,linkID,flow,consumption");
			link_logger.newLine();
			link_logger.flush();
			logger.info("Energy logger created!");
		} catch (IOException e) {
			e.printStackTrace();
			logger.error("Energy logger failed.");
		}

		// initialize NetworkLogger
		logger.info("Network logger creating...");
		try {
			FileWriter fw = new FileWriter(outpath + File.separatorChar + "NetworkLog-" + timestamp + ".csv", false);
			network_logger = new BufferedWriter(fw);
			network_logger.write(
					"tick,vehOnRoad,emptyTrip,chargingTrip,generatedTaxiPass,taxiServedPass,taxiLeavedPass,numWaitingTaxiPass,generatedBusPass,busServedPass,busLeavedPass,numWaitingBusPass");
			network_logger.newLine();
			network_logger.flush();
			logger.info("Network logger created!");
		} catch (IOException e) {
			e.printStackTrace();
			logger.error("Network logger failed.");
		}

		// initialize Zone Logger
		logger.info("Zone logger creating...");
		try {
			FileWriter fw = new FileWriter(outpath + File.separatorChar + "ZoneLog-" + timestamp + ".csv", false);
			zone_logger = new BufferedWriter(fw);
			zone_logger.write(
					"tick,zoneID,numTaxiPass,numBusPass,vehStock,taxiGeneratedPass,busGeneratedPass,taxiServedPass,busServedPass,taxiPassWaitingTime,busPassWaitingTime,taxiLeavedPass,busLeavedPass,taxiWaitingTime");
			zone_logger.newLine();
			zone_logger.flush();
			logger.info("Zone logger created!");
		} catch (IOException e) {
			e.printStackTrace();
			logger.error("Zone logger failed.");
		}

		// initialize Charger Logger
		logger.info("Charger logger creating...");
		try {
			FileWriter fw = new FileWriter(outpath + File.separatorChar + "ChargerLog-" + timestamp + ".csv", false);
			charger_logger = new BufferedWriter(fw);
			charger_logger
					.write("tick,chargerID,vehID,vehType,chargerType,waitingTime,chargingTime,initialBatteryLevel");
			charger_logger.newLine();
			charger_logger.flush();
			logger.info("Charger logger created!");
		} catch (IOException e) {
			e.printStackTrace();
			logger.error("Charger logger failed.");
		}
	}

	// update route_UCB
	@SuppressWarnings("unchecked")
	private void createUCBRoutes() {
		// Refresh road status
		cityContext.modifyRoadNetwork();
		// First try to read from the file. If the file does not exist, then create it.
		try {
			FileInputStream fileIn = new FileInputStream("data/NYC/candidate_routes.ser");
			ObjectInputStream in = new ObjectInputStream(fileIn);
			ContextCreator.route_UCB = (HashMap<String, List<List<Integer>>>) in.readObject();
			logger.info("Serialized data is loaded from data/candidate_routes.ser");
			in.close();
			fileIn.close();
		} catch (FileNotFoundException i) { // File does not exist, let's create one
			// i.printStackTrace();
			logger.info("Candidate routes initialization ...");
			// Get list of zones
			Geography<Zone> zoneGeography = ContextCreator.getZoneGeography();
			// Geography<ChargingStation> chargingStationGeography =
			// ContextCreator.getChargingStationGeography();

			// Loop over all OD pairs
			for (Zone origin : zoneGeography.getAllObjects()) {
				for (Zone destination : zoneGeography.getAllObjects()) {
					if (origin.getIntegerID() != destination.getIntegerID()
							&& (GlobalVariables.HUB_INDEXES.contains(origin.getIntegerID())
									|| GlobalVariables.HUB_INDEXES.contains(destination.getIntegerID()))) {
						logger.info(
								"Creating routes: " + origin.getIntegerID() + "," + destination.getIntegerID());
						try {
							List<List<Integer>> candidate_routes = RouteV.UCBRoute(origin.getCoord(),
									destination.getCoord());
							for (int k = 0; k < candidate_routes.size(); k++) {
								for (int j = 0; j < candidate_routes.get(k).size(); j++) {
									logger.info(candidate_routes.get(k).get(j));
								}
							}
							ContextCreator.route_UCB.put(
									Integer.toString(origin.getIntegerID()) + "," + destination.getIntegerID(),
									candidate_routes);
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
				}
				/*
				 * If vehicles check batter only when it arrive a zone, we can also do online
				 * routing for the vehicles heading to charging stations. for (ChargingStation
				 * destination : chargingStationGeogrpahy.getAllObjects()) { this.route_UCB =
				 * RouteV.UCBRoute(origin.getCoord(), destination.getCoord()); }
				 */
			}
			try {
				// Save the calculation result
				FileOutputStream fileOut = new FileOutputStream("data/NYC/candidate_routes.ser");
				ObjectOutputStream out = new ObjectOutputStream(fileOut);
				out.writeObject(ContextCreator.route_UCB);
				out.close();
				fileOut.close();
				logger.info("Serialized data is saved in data/candidate_routes.ser");
			} catch (IOException o) { // Result cannot be save
				o.printStackTrace();
				return;
			}
		} catch (ClassNotFoundException c) { // Result cannot be load
			c.printStackTrace();
			return;
		} catch (IOException o) { // Result cannot be load
			o.printStackTrace();
			return;
		}

		// Mark route_UCB is populated. This flag is checked in rdcm before starting the data server
		ContextCreator.isRouteUCBPopulated = true;
	}

	// update route_BusUCB, unused in the final experiment
//	@SuppressWarnings("unchecked")
//	private void createUCBBusRoutes() {
//		// Refresh road status
//		cityContext.modifyRoadNetwork();
//		// First try to read from the file. If the file does not exist, then create it.
//		try {
//			FileInputStream fileIn = new FileInputStream("data/NYC/candidate_routes_bus.ser");
//			ObjectInputStream in = new ObjectInputStream(fileIn);
//			ContextCreator.route_UCB_bus = (HashMap<String, List<List<Integer>>>) in.readObject();
//			logger.info("Serialized data is loaded from data/candidate_routes_bus.ser");
//			in.close();
//			fileIn.close();
//		} catch (FileNotFoundException i) { // File does not exist, let's create one
//			// i.printStackTrace();
//			logger.info("Candidate routes initialization ...");
//			// Geography<ChargingStation> chargingStationGeography =
//			// ContextCreator.getChargingStationGeography();
//			Geography<Zone> zoneGeography = ContextCreator.getZoneGeography();
//			// Loop over all OD pairs
//			for (Zone origin : zoneGeography.getAllObjects()) {
//				// if (house.getId()%GlobalVariables.Total_Person_Number==id)
//				for (Zone destination : zoneGeography.getAllObjects()) {
//					if (origin.getIntegerID() != destination.getIntegerID()) {
//						logger.info(
//								"Creating routes: " + origin.getIntegerID() + "," + destination.getIntegerID());
//						try {
//							ContextCreator.route_UCB_bus.put(
//									Integer.toString(origin.getIntegerID()) + "," + destination.getIntegerID(),
//									RouteV.UCBRoute(origin.getCoord(), destination.getCoord()));
//						} catch (Exception e) {
//							e.printStackTrace();
//						}
//					}
//				}
//			}
//			try {
//				// Save the calculation result
//				FileOutputStream fileOut = new FileOutputStream("data/NYC/candidate_routes_bus.ser");
//				ObjectOutputStream out = new ObjectOutputStream(fileOut);
//				out.writeObject(ContextCreator.route_UCB_bus);
//				out.close();
//				fileOut.close();
//				logger.info("Serialized data is saved in data/candidate_routes_bus.ser");
//			} catch (IOException o) { // Result cannot be save
//				o.printStackTrace();
//				return;
//			}
//		} catch (ClassNotFoundException c) { // Result cannot be load
//			c.printStackTrace();
//			return;
//		} catch (IOException o) { // Result cannot be load
//			o.printStackTrace();
//			return;
//		}
//
//		// Mark route_UCB is populated. This flag is checked in rdcm before starting the data server
//		ContextCreator.isRouteUCBBusPopulated = true;
//	}

	public static String getUCBRouteStringForOD(String OD) {
		HashMap<String, Object> jsonObj = new HashMap<String, Object>();
		jsonObj.put("MSG_TYPE", "OD_PAIR");
		jsonObj.put("OD", OD);
		List<List<Integer>> roadLists = ContextCreator.route_UCB.get(OD);
		jsonObj.put("road_lists", roadLists);
		String line = JSONObject.toJSONString(jsonObj);
		return line;
	}

	public static String getUCBRouteStringForODBus(String OD) {
		HashMap<String, Object> jsonObj = new HashMap<String, Object>();
		jsonObj.put("MSG_TYPE", "BOD_PAIR");
		jsonObj.put("BOD", OD);
		List<List<Integer>> roadLists = ContextCreator.route_UCB_bus.get(OD);
		jsonObj.put("road_lists", roadLists);
		String line = JSONObject.toJSONString(jsonObj);
		return line;
	}

	public static Set<String> getUCBRouteODPairs() {
		return ContextCreator.route_UCB.keySet();
	}

	public static Set<String> getUCBRouteODPairsBus() {
		return ContextCreator.route_UCB_bus.keySet();
	}

	public static boolean isRouteUCBMapPopulated() {
		return ContextCreator.isRouteUCBPopulated;
	}

//	public static boolean isRouteUCBBusMapPopulated() {
//		return ContextCreator.isRouteUCBPopulated;
//	}

	public void scheduleStartAndEnd() {
		ISchedule schedule = RunEnvironment.getInstance().getCurrentSchedule();

		RunEnvironment.getInstance().endAt(GlobalVariables.SIMULATION_STOP_TIME);

		logger.info("stop time =  " + GlobalVariables.SIMULATION_STOP_TIME);
		// schedule start of sim
		ScheduleParameters startParams = ScheduleParameters.createOneTime(1);
		schedule.schedule(startParams, this, "start");

		// schedule end of sim
		ScheduleParameters endParams = ScheduleParameters.createAtEnd(ScheduleParameters.LAST_PRIORITY);
		schedule.schedule(endParams, this, "end");

	}

	public void scheduleRoadNetworkRefresh() {
		ISchedule schedule = RunEnvironment.getInstance().getCurrentSchedule();
		// K: schedule parameter for network reloading
		ScheduleParameters agentParamsNW = ScheduleParameters.createRepeating(0, duration02_, 3);

		schedule.schedule(agentParamsNW, cityContext, "modifyRoadNetwork");
	}

	public void scheduleFreeFlowSpeedRefresh() {
		ISchedule schedule = RunEnvironment.getInstance().getCurrentSchedule();
		// speedProfileParams is the schedule parameter to update free flow speed for each road every hour
		ScheduleParameters speedProfileParams = ScheduleParameters.createRepeating(0, duration_, 4);
		for (Road r : getRoadContext().getObjects(Road.class)) {
			schedule.schedule(speedProfileParams, r, "updateFreeFlowSpeed");
		}
		for (Zone z : getZoneGeography().getAllObjects()) {
			schedule.schedule(speedProfileParams, z, "updateTravelEstimation");
		}
		// update the travel time estimation in each zone
		schedule.schedule(speedProfileParams, this, "refreshTravelTime");
	}

	public void scheduleNetworkEventHandling() {
		ISchedule schedule = RunEnvironment.getInstance().getCurrentSchedule();
		// schedule the check of the supply side events
		ScheduleParameters supplySideEventParams = ScheduleParameters.createRepeating(0,
				GlobalVariables.EVENT_CHECK_FREQUENCY, 1);
		schedule.schedule(supplySideEventParams, eventHandler, "checkEvents");
	}

	public void schedulePassengerArrivalAndServe() {
		ISchedule schedule = RunEnvironment.getInstance().getCurrentSchedule();

		// schedule the passenger serving events
		ScheduleParameters demandServeParams = ScheduleParameters.createRepeating(0,
				GlobalVariables.SIMULATION_PASSENGER_SERVE_INTERVAL, 1);
		for (Zone a : getZoneContext().getObjects(Zone.class)) {
			schedule.schedule(demandServeParams, a, "step");
		}
		// schedule the passenger arrival events
		ScheduleParameters demandGenerationParams = ScheduleParameters.createRepeating(0,
				GlobalVariables.SIMULATION_PASSENGER_ARRIVAL_INTERVAL, 1);
		for (Zone a : getZoneContext().getObjects(Zone.class)) {
			schedule.schedule(demandGenerationParams, a, "generatePassenger");
		}
	}

	public void scheduleChargingStation() {
		ISchedule schedule = RunEnvironment.getInstance().getCurrentSchedule();
		ScheduleParameters chargingServeParams = ScheduleParameters.createRepeating(0,
				GlobalVariables.SIMULATION_CHARGING_STATION_REFRESH_INTERVAL, 1);
		for (ChargingStation a : getChargingStationContext().getObjects(ChargingStation.class)) {
			schedule.schedule(chargingServeParams, a, "step");
		}

	}

	public void scheduleMultiThreadedRoadStep() {
		ISchedule schedule = RunEnvironment.getInstance().getCurrentSchedule();
		ThreadedScheduler s = new ThreadedScheduler(GlobalVariables.N_Partition);
		ScheduleParameters agentParaParams = ScheduleParameters.createRepeating(1, 1, 0);
		schedule.schedule(agentParaParams, s, "paraStep");
		// schedule shutting down the parallel thread pool
		ScheduleParameters endParallelParams = ScheduleParameters.createAtEnd(1);
		schedule.schedule(endParallelParams, s, "shutdownScheduler");

		// schedule the time counter
		ScheduleParameters timerParaParams = ScheduleParameters.createRepeating(1, duration03_, 0);
		schedule.schedule(timerParaParams, s, "reportTime");

		// schedule Parameters for the graph partitioning
		ScheduleParameters partitionParams = ScheduleParameters.createRepeating(duration03_, duration03_, 2);
		ScheduleParameters initialPartitionParams = ScheduleParameters.createOneTime(0, 2);
		schedule.schedule(initialPartitionParams, partitioner, "first_run");
		schedule.schedule(partitionParams, partitioner, "check_run");
	}

	public void scheuleSequentialRoadStep() {
		ISchedule schedule = RunEnvironment.getInstance().getCurrentSchedule();
		ScheduleParameters agentParams = ScheduleParameters.createRepeating(1, 1, 0);
		double delay = agentParams.getDuration();
		System.out.println("TIME BETWEEN TWO TICKS " + delay);

		for (Road r : getRoadContext().getObjects(Road.class)) {
			schedule.schedule(agentParams, r, "step");
		}
	}

	public void scheduleDataCollection() {
		double tickDuration = 1.0d;

		if (GlobalVariables.ENABLE_DATA_COLLECTION) {
			ISchedule schedule = RunEnvironment.getInstance().getCurrentSchedule();
			ScheduleParameters dataStartParams = ScheduleParameters.createOneTime(0.0,
					ScheduleParameters.FIRST_PRIORITY);
			schedule.schedule(dataStartParams, dataContext, "startCollecting");

			ScheduleParameters dataEndParams = ScheduleParameters.createAtEnd(ScheduleParameters.LAST_PRIORITY);
			schedule.schedule(dataEndParams, dataContext, "stopCollecting");

			ScheduleParameters tickStartParams = ScheduleParameters.createRepeating(0.0d, tickDuration,
					ScheduleParameters.FIRST_PRIORITY);
			schedule.schedule(tickStartParams, dataContext, "startTick");

			ScheduleParameters tickEndParams = ScheduleParameters.createRepeating(0.0d, tickDuration,
					ScheduleParameters.LAST_PRIORITY);
			schedule.schedule(tickEndParams, dataContext, "stopTick");

			ScheduleParameters recordRuntimeParams = ScheduleParameters.createRepeating(0,
					GlobalVariables.METRICS_DISPLAY_INTERVAL, 6);
			schedule.schedule(recordRuntimeParams, dataContext, "displayMetrics");
		}
	}

	public Context<Object> build(Context<Object> context) {
		logger.info("Reading property files");
		readPropertyFile();
		ContextCreator.mainContext = context;
		handleSimulationSleep();
		logger.info("Building subcontexts");
		buildSubContexts();

		// check if link length and geometry are consistent, fix the inconsistency if there is one.
		for (Lane lane : ContextCreator.getLaneGeography().getAllObjects()) {
			Coordinate[] coords = ContextCreator.getLaneGeography().getGeometry(lane).getCoordinates();
			double distance = 0;
			for (int i = 0; i < coords.length - 1; i++) {
				distance += getDistance(coords[i], coords[i + 1]);
			}
			lane.setLength(distance);
		}

		// schedule all simulation functions
		scheduleStartAndEnd();
		scheduleRoadNetworkRefresh();
		scheduleFreeFlowSpeedRefresh();
		scheduleNetworkEventHandling();
		schedulePassengerArrivalAndServe();
		scheduleChargingStation();

		// schedule parameters for both serial and parallel road updates
		if (GlobalVariables.MULTI_THREADING) {
			scheduleMultiThreadedRoadStep();

		} else {
			scheuleSequentialRoadStep();

		}

		// set up data collection
		if (GlobalVariables.ENABLE_DATA_COLLECTION) {
			scheduleDataCollection();
		}

		agentID = 0;

		return context;
	}

	public void printTick() {
		logger.info("Tick: " + RunEnvironment.getInstance().getCurrentSchedule().getTickCount());
	}

	public static void stopSim(Exception ex, Class<?> clazz) {
		ISchedule sched = RunEnvironment.getInstance().getCurrentSchedule();
		sched.setFinishing(true);
		sched.executeEndActions();
		logger.info("ContextCreator has been told to stop by  " + clazz.getName() + ex);
	}

	public static void end() {
		logger.info("Finished sim: " + (System.currentTimeMillis() - startTime));
		try {
			ev_logger.flush();
			bus_logger.flush();
			link_logger.flush();
			network_logger.flush();
			zone_logger.flush();
			ev_logger.close();
			bus_logger.close();
			link_logger.close();
			network_logger.close();
			zone_logger.close();
			// stopSim(null, null);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void start() {
		startTime = System.currentTimeMillis();
	}

	public static int generateAgentID() {
		return ContextCreator.agentID++;
	}

	public static VehicleContext getVehicleContext() {
		return (VehicleContext) mainContext.findContext("VehicleContext");
	}

	@SuppressWarnings("unchecked")
	public static Geography<Vehicle> getVehicleGeography() {
		return (Geography<Vehicle>) ContextCreator.getVehicleContext().getProjection(Geography.class,
				"VehicleGeography");
	}

	public static JunctionContext getJunctionContext() {
		return (JunctionContext) mainContext.findContext("JunctionContext");
	}

	@SuppressWarnings("unchecked")
	public static Geography<Junction> getJunctionGeography() {
		return ContextCreator.getJunctionContext().getProjection(Geography.class, "JunctionGeography");
	}

	@SuppressWarnings("unchecked")
	public static Network<Junction> getRoadNetwork() {
		return ContextCreator.getJunctionContext().getProjection(Network.class, "RoadNetwork");
	}

	public static RoadContext getRoadContext() {
		return (RoadContext) mainContext.findContext("RoadContext");
	}

	@SuppressWarnings("unchecked")
	public static Geography<Road> getRoadGeography() {
		return (Geography<Road>) ContextCreator.getRoadContext().getProjection("RoadGeography");
	}

	public static LaneContext getLaneContext() {
		return (LaneContext) mainContext.findContext("LaneContext");
	}

	@SuppressWarnings("unchecked")
	public static Geography<Lane> getLaneGeography() {
		return (Geography<Lane>) ContextCreator.getLaneContext().getProjection("LaneGeography");
	}

	public static CityContext getCityContext() {
		return (CityContext) mainContext.findContext("CityContext");
	}

	public static ZoneContext getZoneContext() {
		return (ZoneContext) mainContext.findContext("ZoneContext");
	}

	@SuppressWarnings("unchecked")
	public static Geography<Zone> getZoneGeography() {
		return (Geography<Zone>) ContextCreator.getZoneContext().getProjection("ZoneGeography");
	}

	public static ChargingStationContext getChargingStationContext() {
		return (ChargingStationContext) mainContext.findContext("ChargingStationContext");
	}

	@SuppressWarnings("unchecked")
	public static Geography<ChargingStation> getChargingStationGeography() {
		return (Geography<ChargingStation>) ContextCreator.getChargingStationContext()
				.getProjection("ChargingStationGeography");
	}

	public static DataCollectionContext getDataCollectionContext() {
		return (DataCollectionContext) mainContext.findContext("DataCollectionContext");
	}

	public static double convertToMeters(double dist) {
		double distInMeters = NonSI.NAUTICAL_MILE.getConverterTo(SI.METER).convert(dist * 60);
		return distInMeters;
	}

	public static TreeMap<Integer, ArrayList<Double>> getBackgroundTraffic() {
		return backgroundTraffic.backgroundTraffic;
	}

	public static TreeMap<Integer, ArrayList<Double>> getBackgroundTrafficStd() {
		return backgroundTraffic.backgroundStd;
	}

	public static TreeMap<Integer, ArrayList<Double>> getTravelDemand() {
		return backgroundDemand.travelDemand;
	}

	public static ArrayList<ArrayList<Integer>> getBusSchedule() {
		return busSchedule.busRoute;
	}

	public static ArrayList<Integer> getBusNum() {
		return busSchedule.busNum;
	}

	public static ArrayList<Integer> getBusGap() {
		return busSchedule.busGap;
	}

	private double getDistance(Coordinate c1, Coordinate c2) {
		//GeodeticCalculator calculator = new GeodeticCalculator(ContextCreator.getRoadGeography().getCRS());
		GeodeticCalculator calculator = new GeodeticCalculator(ContextCreator.getLaneGeography().getCRS());
		calculator.setStartingGeographicPoint(c1.x, c1.y);
		calculator.setDestinationGeographicPoint(c2.x, c2.y);
		double distance;
		try {
			distance = calculator.getOrthodromicDistance();
		} catch (AssertionError ex) {
			logger.error("Error with finding distance");
			distance = 0.0;
		}
		return distance;
	}

	public void refreshTravelTime() {
		logger.info("Update the estimation of travel time...");
		for (Zone z1 : getZoneGeography().getAllObjects()) {
			z1.busTravelTime = new HashMap<Integer, Float>();
			z1.busTravelDistance = new HashMap<Integer, Float>();
		}
		for (List<Integer> route : busSchedule.busRoute) {
			logger.debug(route);
			// retrieve stations in order, from hub to other places
			double travel_distance = 0;
			double travel_time = 0;
			Zone hub = this.cityContext.findHouseWithDestID(route.get(0));
			Zone z1 = hub;
			Zone z2;
			for (int i = 1; i < route.size(); i++) {
				z2 = this.cityContext.findHouseWithDestID(route.get(i));
				List<Road> path = RouteV.shortestPathRoute(z1.getCoord(), z2.getCoord());
				if (path != null) {
					for (Road r : path) {
						travel_distance += r.getLength();
						travel_time += r.getTravelTime();
					}
				}
				if(hub.busTravelDistance.containsKey(z2.getIntegerID())) {
					hub.busTravelDistance.put(z2.getIntegerID(), Math.min(hub.busTravelDistance.get(z2.getIntegerID()), (float) travel_distance));
					hub.busTravelTime.put(z2.getIntegerID(), Math.min(hub.busTravelTime.get(z2.getIntegerID()), (float) travel_time));
				}
				else {
					hub.busTravelDistance.put(z2.getIntegerID(), (float) travel_distance);
					hub.busTravelTime.put(z2.getIntegerID(), (float) travel_time);
				}
				z1 = z2;
			}
			logger.debug(hub.busTravelDistance);
			logger.debug(hub.busTravelTime);
			// retrieve stations in back order, from other places to hub
			travel_distance = 0;
			travel_time = 0;
			z2 = hub;
			for (int i = route.size() - 1; i > 0; i--) {
				z1 = this.cityContext.findHouseWithDestID(route.get(i));
				List<Road> path = RouteV.shortestPathRoute(z1.getCoord(), z2.getCoord());
				if (path != null) {
					for (Road r : path) {
						travel_distance += r.getLength();
						travel_time += r.getTravelTime();
					}
				}
				if(z1.busTravelDistance.containsKey(hub.getIntegerID())) {
					z1.busTravelDistance.put(hub.getIntegerID(), Math.min(z1.busTravelDistance.get(hub.getIntegerID()), (float) travel_distance));
					z1.busTravelTime.put(hub.getIntegerID(), Math.min(z1.busTravelTime.get(hub.getIntegerID()), (float) travel_time));
				}
				else {
					z1.busTravelDistance.put(hub.getIntegerID(), (float) travel_distance);
					z1.busTravelTime.put(hub.getIntegerID(), (float) travel_time);
				}
				z2 = z1;
				logger.debug(z1.busTravelDistance);
				logger.debug(z1.busTravelTime);
			}
		}
	}

}