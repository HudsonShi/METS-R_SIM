package addsEVs.citycontext;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import org.geotools.referencing.GeodeticCalculator;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
//import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.operation.distance.DistanceOp;
//import com.vividsolutions.jts.geom.LineSegment;

import addsEVs.*;
import addsEVs.routing.RouteV;
import repast.simphony.context.DefaultContext;
import repast.simphony.space.gis.Geography;
import repast.simphony.space.graph.Network;
import repast.simphony.space.graph.RepastEdge;
import repast.simphony.essentials.RepastEssentials;

public class CityContext extends DefaultContext<Object> {

	/* To memory a link between Roads (in the RoadGeography) and Edges in the RoadNetwork */
	// Stores the linkIDs of Repast edges (Edge as key)
	private HashMap<RepastEdge<?>, Integer> edgeLinkID_KeyEdge;
	// Stores the TOIDs of edges (Edge as key)
	private HashMap<RepastEdge<?>, Integer> edgeIdNum_KeyEdge;
	// Stores the TOIDs of edges (TOID as key)
	private HashMap<Integer, RepastEdge<?>> edgeIDs_KeyIDNum;
	private HashMap<Integer, Lane> lane_KeyLaneID;
	private HashMap<Integer, Road> road_KeyLinkID;
	private HashMap<Integer, Junction> junction_KeyJunctionID;

	/* Cache the nearest road Coordinate to every zone/spawn point for efficiency  */
	private Map<Coordinate, Coordinate> nearestRoadCoordCache;

	// Constructs a CityContextContext and creates a Geography (called RoadNetworkGeography) which is part of this context.
	public CityContext() {
		super("CityContext"); // Very important otherwise repast complains
		this.edgeLinkID_KeyEdge = new HashMap<RepastEdge<?>, Integer>();
		this.edgeIdNum_KeyEdge = new HashMap<RepastEdge<?>, Integer>();
		this.edgeIDs_KeyIDNum = new HashMap<Integer, RepastEdge<?>>();
		this.lane_KeyLaneID = new HashMap<Integer, Lane>();
		this.junction_KeyJunctionID = new HashMap<Integer, Junction>();
		this.road_KeyLinkID = new HashMap<Integer, Road>();

	}

	public void createSubContexts() {
		this.addSubContext(new RoadContext());
		this.addSubContext(new JunctionContext());
		this.addSubContext(new LaneContext());
		this.addSubContext(new ZoneContext());
		this.addSubContext(new ChargingStationContext());
		for(Lane lane: ContextCreator.getLaneGeography().getAllObjects()){
			Coordinate[] coords = ContextCreator.getLaneGeography().getGeometry(lane).getCoordinates();
			double distance = 0;
			for (int i = 0; i < coords.length - 1; i++) {
				distance += getDistance(coords[i], coords[i + 1]);
			}
			if(Math.abs(distance-lane.getLength())>1){
				// detect distance inconsistent between the provided length and the geometry length
				ContextCreator.logger.debug("Lane ID: " + lane.getLaneid() + "," + " Calculated distance: "+ distance+","+"Real distance: " + lane.getLength());
			}
			lane.setLength(distance);
		}
	}
	
	private double getDistance(Coordinate c1, Coordinate c2) {
		GeodeticCalculator calculator = new GeodeticCalculator(ContextCreator
				.getLaneGeography().getCRS());
		calculator.setStartingGeographicPoint(c1.x, c1.y);
		calculator.setDestinationGeographicPoint(c2.x, c2.y);
		double distance;
		try {
			distance = calculator.getOrthodromicDistance();
		} catch (AssertionError ex) {
			System.err.println("Error with finding distance");
			distance = 0.0;
		}
		return distance;
	}
	
	/**
	 * Actually creates the road network. Runs through the RoadGeography and
	 * generate nodes from the end points of each line. These nodes are added to
	 * the RoadNetwork (part of the JunctionContext) as well as to edges.
	 */
	public void buildRoadNetwork() {
		// Get lane geography
		Geography<Lane> laneGeography = ContextCreator.getLaneGeography();
		Iterable<Lane> laneIt = laneGeography.getAllObjects();

		// Get road geography
		Geography<Road> roadGeography = ContextCreator.getRoadGeography();
		Geography<Junction> junctionGeography = ContextCreator
				.getJunctionGeography();
		JunctionContext junctionContext = ContextCreator.getJunctionContext();
		Network<Junction> roadNetwork = ContextCreator.getRoadNetwork();

		/* Create a GeometryFactory so we can create points/lines from
		* the junctions and roads (this is so they can be displayed
		* on the same display to check if the network has been created
		* successfully) */
		GeometryFactory geomFac = new GeometryFactory();

		Iterable<Road> roadIt = roadGeography.getAllObjects();
		for (Road road : roadIt) {
			this.road_KeyLinkID.put(road.getLinkid(), road);
			Geometry roadGeom = roadGeography.getGeometry(road);
			Coordinate c1 = roadGeom.getCoordinates()[0];
			Coordinate c2 = roadGeom.getCoordinates()[roadGeom.getNumPoints() - 1];
			// Create Junctions from these coordinates and add them to the
			// JunctionGeography (if they haven't been created already)
			Junction junc1, junc2;
			if (!junction_KeyJunctionID.containsKey(road.getFn())) {
				junc1 = new Junction(c1, road.getFn());
				this.junction_KeyJunctionID.put(road.getFn(), junc1);
				junctionContext.add(junc1);
				Point p1 = geomFac.createPoint(c1);
				junctionGeography.move(junc1, p1);
			} else
				junc1 = junction_KeyJunctionID.get(road.getFn());

			if (!junction_KeyJunctionID.containsKey(road.getTn())) {
				junc2 = new Junction(c2, road.getTn());
				this.junction_KeyJunctionID.put(road.getTn(), junc2);
				junctionContext.add(junc2);
				Point p2 = geomFac.createPoint(c2);
				junctionGeography.move(junc2, p2);
			} else
				junc2 = junction_KeyJunctionID.get(road.getTn());

			// Tell the road object who it's junctions are
			road.addJunction(junc1);
			road.addJunction(junc2);

			// Tell the junctions about this road
			junc1.addRoad(road);
			junc2.addRoad(road);

			RepastEdge<Junction> edge = new RepastEdge<Junction>(junc1, junc2,
					true, road.getLength()); // KD: replace weight

			// Store the road's TOID in a dictionary (one with edges as keys,
			try {
				this.edgeLinkID_KeyEdge.put(edge, road.getLinkid());
				this.edgeIdNum_KeyEdge.put(edge, road.getID());
				this.edgeIDs_KeyIDNum.put(road.getID(), edge);
			} catch (Exception e) {
				ContextCreator.logger.error(e.getMessage());
			}
			if (!roadNetwork.containsEdge(edge)) {
				roadNetwork.addEdge(edge);
			} else {
				ContextCreator.logger.error("CityContext: buildRoadNetwork: for some reason this edge that has just been created "
						+ "already exists in the RoadNetwork!");
			}

		} // For road
		roadIt = roadGeography.getAllObjects();
		ContextCreator.logger.info("Junction initialized!");
		// Assign the lanes to each road
		for (Lane lane : laneIt) {
			this.lane_KeyLaneID.put(lane.getLaneid(), lane);
			for (Road road : roadIt) {
				if (road.getLinkid() == (long) lane.getLink()) {
					lane.setRoad(road);
				}
			}
		}
		for (Road r : roadIt) {
			// r.sortLanes();
			roadMovementFromShapeFile(r);
			laneConnectionsFromShapeFile(r);
		}
	}

	/*
	 * Get road movement from shapefile and put in an array list
	 */
	public void roadMovementFromShapeFile(Road road) {
		ArrayList<Integer> dsLinkIds = new ArrayList<Integer>();
		dsLinkIds.add(road.getLeft());
		dsLinkIds.add(road.getThrough());
		dsLinkIds.add(road.getRight());
		dsLinkIds.add(road.getTlinkid());
		Road opRoad = this.road_KeyLinkID.get(road.getTlinkid());
		road.setOppositeRoad(opRoad);

		for (int dsRoadId : dsLinkIds) {
			if (dsRoadId != 0) {
				Road dsRoad = this.road_KeyLinkID.get(dsRoadId);
				road.addDownStreamMovement(dsRoad);
			}
		}
	}

	public Road getRoadfromID(int roadId_) {
		// Road road = null;
		Geography<Road> roadGeography = ContextCreator.getRoadGeography();
		Iterable<Road> roadIt = roadGeography.getAllObjects();

		for (Road road : roadIt) {
			if (road.getLinkid() == roadId_) {
				return road;
			}
		}
		return null;
	}

	public Lane getLanefromID(int laneID) {
		// Lane lane = null;
		Geography<Lane> laneGeography = ContextCreator.getLaneGeography();
		Iterable<Lane> laneIt = laneGeography.getAllObjects();
		for (Lane lane : laneIt) {
			if (lane.getLaneid() == laneID) {
				return lane;
			}
		}
		return null;
	}

	public void laneConnectionsFromShapeFile(Road road) {
		ArrayList<Integer> dsLaneIds = new ArrayList<Integer>();
		int nLanes = road.getnLanes(); // number of lanes in current road
		Lane curLane, dsLane;
		for (int i = 0; i < nLanes; i++) {
			curLane = road.getLanes().get(i);
			dsLaneIds.clear();
			dsLaneIds.add(curLane.getLeft());
			dsLaneIds.add(curLane.getThrough());
			dsLaneIds.add(curLane.getRight());
			ContextCreator.logger.debug("Lane: " + curLane.getLaneid() + " from road "
			 + curLane.road_().getIdentifier() +
			 " has downstream connections: ");
			for (double dsLaneId : dsLaneIds) {
				if (dsLaneId != 0) {
					dsLane = this.lane_KeyLaneID.get((int) dsLaneId);
					curLane.addDnLane(dsLane);
					dsLane.addUpLane(curLane);
				}
			}
		}
		// Add u-connected lanes
		if (road.getOppositeRoad() != null) {
			curLane = road.getLanes().get(0);
			if (curLane.getLength() > GlobalVariables.MIN_UTURN_LENGTH) { 
				dsLane = road.getOppositeRoad().getLanes().get(0);
				curLane.addDnLane(dsLane);
				dsLane.addUpLane(curLane);
			}
		}
	}

	/* We update node based routing while modify road network */
	public void modifyRoadNetwork() {
		int tickcount;
		Geography<Road> roadGeography = ContextCreator.getRoadGeography();
		Iterable<Road> roadIt = roadGeography.getAllObjects();
		for (Road road : roadIt) {
			road.setTravelTime();
			Junction junc1 = road.getJunctions().get(0);
			Junction junc2 = road.getJunctions().get(1);
			ContextCreator.getRoadNetwork().getEdge(junc1, junc2)
					.setWeight(road.getTravelTime());
		}

		// At beginning, initialize route object
		tickcount = (int) RepastEssentials.GetTickCount();
		ContextCreator.logger.info("Tick: " + tickcount);
		if (!ContextCreator.isRouteUCBMapPopulated()) {
			try {
				RouteV.createRoute();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		// Update next node routing matrix
		try {
			RouteV.updateRoute();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	

	/*
	 * Gets the ID String associated with a given edge. Useful for linking
	 * RepastEdges with spatial objects (i.e. Roads).
	 */
	public int getLinkIDFromEdge(RepastEdge<Junction> edge) {
		int id = 0;
		try {
			id = this.edgeLinkID_KeyEdge.get(edge);
		} catch (Exception e) {
			ContextCreator.logger.error("CityContext: getIDDromEdge: Error, probably no id found for edge "
							+ edge.toString()+ e.getStackTrace());
		}
		return id;
	}

	/**
	 * @param edge
	 * @return id
	 */
	public int getIdNumFromEdge(RepastEdge<Junction> edge) {
		int id = 0;
		try {
			id = this.edgeIdNum_KeyEdge.get(edge);
		} catch (Exception e) {
			System.err
					.println("CityContext: getIdNumFromEdge: Error, probably no id found for edge "
							+ edge.toString());
			System.err.println(e.getStackTrace());
		}
		return id;
	}

	/**
	 * Get the repast edge with the given Road ID number This is the easiest way
	 * to find an edge using the road info
	 * 
	 * @param ID of the road
	 */
	public RepastEdge<?> getEdgeFromIDNum(int id) {
		RepastEdge<?> edge = null;
		try {
			edge = this.edgeIDs_KeyIDNum.get(id);
		} catch (Exception e) {
			System.err
					.println("CityContext: getEdgeDromID: Error, probably no edge found for id "
							+ id);
			System.err.println(e.getStackTrace());
		}
		return edge;
	}

	/*
	 * Returns the road which is crosses the given coordinates (Actually it just
	 * returns the nearest road to the coords)
	 */
	public int findRoadIDAtCoordinates(Coordinate coord)
			throws NullPointerException {
		if (coord == null) {
			throw new NullPointerException(
					"CityContext: findRoadAtCoordinates: ERROR: the input coordinate is null");
		}
		GeometryFactory geomFac = new GeometryFactory();
		Geography<?> roadGeography = ContextCreator.getRoadGeography();
		// Use a buffer for efficiency
		Point point = geomFac.createPoint(coord);
		Geometry buffer = point.buffer(GlobalVariables.XXXX_BUFFER);
		double minDist = Double.MAX_VALUE;
		int nearestRoadID = 0;
		// Road nearestRoad = null;
		for (Road road : roadGeography.getObjectsWithin(
				buffer.getEnvelopeInternal(), Road.class)) {
			DistanceOp distOp = new DistanceOp(point,
					roadGeography.getGeometry(road));
			double thisDist = distOp.distance();
			if (thisDist < minDist) { // If thisDist < minDist
				minDist = thisDist;
				nearestRoadID = road.getLinkid();
			} 
		} 
		if (nearestRoadID == 0) { // For nearRoads
			ContextCreator.logger.error("CityContext: findRoadAtCoordinates (Coordinate coord): ERROR: couldn't find a road at these coordinates:\n\t"
							+ coord.toString());
		}
		return nearestRoadID;
	}

	/*
	 * Returns the road which is crosses the given coordinates (Actually it just
	 * Returns the nearest road to the coords)
	 */
	public Road findRoadAtCoordinates(Coordinate coord)
			throws NullPointerException {
		if (coord == null) {
			throw new NullPointerException(
					"CityContext: findRoadAtCoordinates: ERROR: the input coordinate is null");
		}
		GeometryFactory geomFac = new GeometryFactory();
		Geography<?> roadGeography = ContextCreator.getRoadGeography();
		// Use a buffer for efficiency
		Point point = geomFac.createPoint(coord);
		Geometry buffer = point.buffer(GlobalVariables.XXXX_BUFFER);
		double minDist = Double.MAX_VALUE;
		Road nearestRoad = null;
		for (Road road : roadGeography.getObjectsWithin(
				buffer.getEnvelopeInternal(), Road.class)) {
			DistanceOp distOp = new DistanceOp(point,
					roadGeography.getGeometry(road));
			double thisDist = distOp.distance();
			if (thisDist < minDist) { // If thisDist < minDist
				minDist = thisDist;
				nearestRoad = road;
			} 
		} 
		if (nearestRoad == null) { // for nearRoads
			ContextCreator.logger.error("CityContext: findRoadAtCoordinates (Coordinate coord): ERROR: couldn't find a road at these coordinates:\n\t"
							+ coord.toString());
		}
		return nearestRoad;
	}
	
	/*
	 * Returns the closest charging station from the currentLocation 
	 */
	public ChargingStation findNearestChargingStation(Coordinate coord) throws NullPointerException{
		if (coord == null) {
			throw new NullPointerException(
					"CityContext: findNearestChargingStation: ERROR: the input coordinate is null");
		}
		GeometryFactory geomFac = new GeometryFactory();
		Geography<ChargingStation> csGeography = ContextCreator.getChargingStationGeography();
		// Use a buffer for efficiency
		Point point = geomFac.createPoint(coord);
		Geometry buffer = point.buffer(GlobalVariables.XXXX_BUFFER);
		double minDist = Double.MAX_VALUE;
		ChargingStation nearestChargingStation = null;
		for (ChargingStation cs : csGeography.getObjectsWithin(buffer.getEnvelopeInternal(), ChargingStation.class)) {
			DistanceOp distOp = new DistanceOp(point, csGeography.getGeometry(cs));
			double thisDist = distOp.distance();
			if ((thisDist < minDist) && cs.capacity()>0) { // if thisDist < minDist
				minDist = thisDist;
				nearestChargingStation = cs;
			} 
		}
		if (nearestChargingStation == null) {
			ContextCreator.logger.error("CityContext: findNearestChargingStation (Coordinate coord): ERROR: couldn't find a charging station at these coordinates:\n\t"
							+ coord.toString());
		}

		return nearestChargingStation;
	}
	
	/*
	 * Returns the closest charging station for bus from the currentLocation 
	 */
	public ChargingStation findNearestBusChargingStation(Coordinate coord) throws NullPointerException{
		if (coord == null) {
			throw new NullPointerException(
					"CityContext: findNearestChargingStation: ERROR: the input coordinate is null");
		}
		GeometryFactory geomFac = new GeometryFactory();
		Geography<ChargingStation> csGeography = ContextCreator.getChargingStationGeography();
		// Use a buffer for efficiency
		Point point = geomFac.createPoint(coord);
		Geometry buffer = point.buffer(GlobalVariables.XXXX_BUFFER);
		double minDist = Double.MAX_VALUE;
		ChargingStation nearestChargingStation = null;
		for (ChargingStation cs : csGeography.getObjectsWithin(buffer.getEnvelopeInternal(), ChargingStation.class)) {
			DistanceOp distOp = new DistanceOp(point, csGeography.getGeometry(cs));
			double thisDist = distOp.distance();
			if ((thisDist < minDist)) { // if thisDist < minDist
				minDist = thisDist;
				nearestChargingStation = cs;
			} 
		}
		if (nearestChargingStation == null) {
			ContextCreator.logger.error("CityContext: findNearestChargingStation (Coordinate coord): ERROR: couldn't find a charging station at these coordinates:\n\t"
							+ coord.toString());
		}
		return nearestChargingStation;
	}
	 

	public Road findRoadAtCoordinates(Coordinate coord, boolean toDest)
			throws NullPointerException {
		if (coord == null) {
			throw new NullPointerException(
					"CityContext: findRoadAtCoordinates: ERROR: the input coordinate is null");
		}
		GeometryFactory geomFac = new GeometryFactory();
		Geography<?> roadGeography = ContextCreator.getRoadGeography();
		// Use a buffer for efficiency
		Point point = geomFac.createPoint(coord);
		Geometry buffer = point.buffer(GlobalVariables.XXXX_BUFFER);
		double minDist = Double.MAX_VALUE;
		Road nearestRoad = null;

		// New code when nearest road was found based on distance from junction
		// Requires the direction variable
		for (Road road : roadGeography.getObjectsWithin(
				buffer.getEnvelopeInternal(), Road.class)) {
			if (toDest) {
				Coordinate roadToNode = road.getJunctions().get(1)
						.getCoordinate();
				Point pointToNode = geomFac.createPoint(roadToNode);
				DistanceOp distOp = new DistanceOp(point, pointToNode);
				double thisDist = distOp.distance();
				if (thisDist < minDist) {
					minDist = thisDist;
					nearestRoad = road;
				} // If thisDist < minDist
			} else {
				Coordinate roadFromNode = road.getJunctions().get(0)
						.getCoordinate();
				Point pointFromNode = geomFac.createPoint(roadFromNode);
				DistanceOp distOp = new DistanceOp(point, pointFromNode);
				double thisDist = distOp.distance();
				if (thisDist < minDist) { // If thisDist < minDist
					minDist = thisDist;
					nearestRoad = road;
				} 
			}

		}
		if (nearestRoad == null) {  // for nearRoads
			ContextCreator.logger.error("CityContext: findRoadAtCoordinates (Coordinate coord, boolean toDest): ERROR: couldn't find a road at these coordinates:\n\t"
							+ coord.toString());
		}
		return nearestRoad;
	}

	public Road findRoadWithID(int id) {
		Geography<Road> roadGeography = ContextCreator.getRoadGeography();
		for (Road road : roadGeography.getAllObjects()) {
			if (road.getLinkid() == id)
				return road;
		}
		System.err
				.println("CityContext: findRoadWithID: Error, couldn't find a road woth id: "
						+ id);
		return new Road();
	}

	public Road findRoadBetweenJunctionIDs(int junc1, int junc2) {
		Geography<Road> roadGeography = ContextCreator.getRoadGeography();
		ArrayList<Junction> junctions;
		for (Road road : roadGeography.getAllObjects()) {
			junctions = road.getJunctions();
			if ((junctions.get(0).getJunctionID() == junc1
					&& junctions.get(1).getJunctionID() == junc2)) {
				return road;
			}
		}

		ContextCreator.logger.error("CityContext: findRoadBetweenJunctionIDs: Error, couldn't find a road with id: "
						+ junc1 + " to id: " + junc2);
		return null;
	}

	public Road findRoadWithLinkID(int linkID) {
		return this.road_KeyLinkID.get(linkID);
	}
	
	public Zone findZoneWithID(int id) {
		Geography<Zone> zoneGeography = ContextCreator.getZoneGeography();
		for (Zone zone : zoneGeography.getAllObjects()) {
			if (zone.getId() == id)
				return zone;
		}
		ContextCreator.logger.error("CityContext: findHouseWithID: Error, couldn't find a house with id: "
						+ id);
		return null;
	}

	public Zone findZoneWithDestID(int destid) {
		Geography<Zone> zoneGeography = ContextCreator.getZoneGeography();
		for (Zone zone : zoneGeography.getAllObjects()) {
			if (zone.getIntegerID() == destid)
				return zone;
		}
		ContextCreator.logger.error("CityContext: findZoneWithDestID: Error, couldn't find a house with id: "
						+ destid);
		return null;
	}
	
	public ChargingStation findChargingStationWithID(int destid) {
		Geography<ChargingStation> chargingStationGeogrpahy = ContextCreator.getChargingStationGeography();
		for (ChargingStation cs : chargingStationGeogrpahy.getAllObjects()) {
			if (cs.getIntegerID() == destid)
				return cs;
		}
		ContextCreator.logger.error("CityContext: findChargingStationWithDestID: Error, couldn't find a charging station with id: " + destid);
		return null;
	}

	public Road findRoadWithHouseID(int id) {
		Coordinate coord;
		Road road;
		Geography<Zone> zoneGeography = ContextCreator.getZoneGeography();
		for (Zone house : zoneGeography.getAllObjects()) {
			if (house.getId() == id) {
				coord = house.getCoord();
				road = findRoadWithID(findRoadIDAtCoordinates(coord));
				return road;
			}
		}
		ContextCreator.logger.error("CityContext: findRoadWithHouseID: Error, couldn't find a house with id: "
						+ id);
		return new Road();
	}

	public void createNearestRoadCoordCache() {
		this.nearestRoadCoordCache = new Hashtable<Coordinate, Coordinate>();
		Map<Road, Geometry> allRoads = new HashMap<Road, Geometry>();
		for (Road r : ContextCreator.getRoadGeography().getAllObjects()) {
			allRoads.put(r, ContextCreator.getRoadGeography().getGeometry(r));
		}

		for (Zone h : ContextCreator.getZoneGeography().getAllObjects()) {
			double minDist = Double.MAX_VALUE;
			Coordinate houseCoord = ContextCreator.getZoneGeography()
					.getGeometry(h).getCentroid().getCoordinate();
			Coordinate nearestPoint = null;
			Point coordGeom = ContextCreator.getZoneGeography().getGeometry(h)
					.getCentroid();
			for (Road r : allRoads.keySet()) {
				Geometry roadGeom = allRoads.get(r);
				DistanceOp distOp = new DistanceOp(coordGeom, roadGeom);
				double thisDist = distOp.distance();
				if (thisDist < minDist) { // If thisDist < minDist
					minDist = thisDist;
					// Two coordinates returned by closestPoints(), need to find the one which isn't the coord parameter
					for (Coordinate c : distOp.nearestPoints()) {
						if (!c.equals(houseCoord)) {
							nearestPoint = c;
							break;
						}
					}
				} 
			} // For allRoads
			this.nearestRoadCoordCache.put(houseCoord, nearestPoint);
		} 
	}

	public Coordinate getNearestRoadCoordFromCache(Coordinate c) {
		return this.nearestRoadCoordCache.get(c);
	}

	/**
	 * Return true if the roads nearest to each house have been cached.
	 * 
	 * @return
	 */
	public boolean nearestRoadsCached() {
		return (this.nearestRoadCoordCache != null);
	}

	public static double angle(Coordinate p0, Coordinate p1) {
		double dx = p1.x - p0.x;
		double dy = p1.y - p0.y;

		return Math.atan2(dy, dx);
	}
	
	public static double squaredEuclideanDistance(Coordinate p0, Coordinate p1){
		return (p0.x - p1.x)*(p0.x - p1.x) + (p0.y - p1.y)*(p0.y - p1.y);
	}
	
	/*
	 * Returns the closest zone from the currentLocation that has a transit
	 */
	public Zone findNearestZoneWithBus(Coordinate coord, Integer destID) throws NullPointerException{
		if (coord == null) {
			throw new NullPointerException(
					"CityContext: findNearestZoneWithBus: ERROR: the input coordinate is null");
		}
		GeometryFactory geomFac = new GeometryFactory();
		Geography<Zone> zoneGeography = ContextCreator.getZoneGeography();
		// Use a buffer for efficiency
		Point point = geomFac.createPoint(coord);
		double minDist = Double.MAX_VALUE;
		Zone nearestZone = null;
		for (Zone z : zoneGeography.getAllObjects()) {
			if(z.busReachableZone.contains(destID)) {
				DistanceOp distOp = new DistanceOp(point, zoneGeography.getGeometry(z));
				double thisDist = distOp.distance();
				if (thisDist < minDist) {
					minDist = thisDist;
					nearestZone = z;
				} 
			}
		}
//		if (nearestZone == null) {
//			ContextCreator.logger.error("CityContext: findNearestZoneWithBus (Coordinate coord): ERROR: "
//					+ "couldn't find any zone with bus to zone: " + destID + " at these coordinates:\n\t"
//							+ coord.toString());
//		}

		return nearestZone;
	}
}
