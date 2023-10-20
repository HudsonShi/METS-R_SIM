package mets_r.facility;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import org.geotools.referencing.GeodeticCalculator;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;

import mets_r.*;
import mets_r.data.OpenDriveMap;
import mets_r.routing.RouteContext;

import repast.simphony.context.DefaultContext;
import repast.simphony.context.space.graph.NetworkFactory;
import repast.simphony.context.space.graph.NetworkFactoryFinder;
import repast.simphony.space.gis.Geography;
import repast.simphony.space.graph.Network;
import repast.simphony.space.graph.RepastEdge;

/**
 * Inherent from A-RESCUE
 * 
 * Initialize and maintain facility agents
 **/
public class CityContext extends DefaultContext<Object> {
	private HashMap<RepastEdge<?>, Integer> edgeRoadID_KeyEdge; // Store the TOIDs of edges (Edge as key)
	private HashMap<Integer, RepastEdge<?>> edgeIDEdge_KeyID; // Store the TOIDs of edges (TOID as key)
	private HashMap<Coordinate, Road> coordRoad_KeyCoord; // Cache the closest road
	private HashMap<Integer, HashMap<Integer, Road>> nodeIDRoad_KeyNodeID;
          
	public CityContext() {
		super("CityContext"); // Very important otherwise repast complains
		this.edgeRoadID_KeyEdge = new HashMap<RepastEdge<?>, Integer>();
		this.edgeIDEdge_KeyID = new HashMap<Integer, RepastEdge<?>>();
		this.coordRoad_KeyCoord = new HashMap<Coordinate, Road>();
		this.nodeIDRoad_KeyNodeID = new HashMap<Integer, HashMap<Integer, Road>>();
		
		/* Create a Network projection for the road network--->Network Projection */
		NetworkFactory netFac = NetworkFactoryFinder.createNetworkFactory(new HashMap<String, Object>());
		netFac.createNetwork("RoadNetwork", this, true);
	}

	public void createSubContexts() {
		this.addSubContext(new JunctionContext());
		this.addSubContext(new NodeContext());
		this.addSubContext(new SignalContext());
		this.addSubContext(new RoadContext());
		this.addSubContext(new LaneContext());
		this.addSubContext(new ZoneContext());
		this.addSubContext(new ChargingStationContext());
		this.initializeLaneDistance();
	}
	
	// Calculate the length of each length based on their geometries
	private void initializeLaneDistance() {
		for (Lane lane : ContextCreator.getLaneGeography().getAllObjects()) {
			Coordinate[] coords = ContextCreator.getLaneGeography().getGeometry(lane).getCoordinates();
			float distance = 0;
			for (int i = 0; i < coords.length - 1; i++) {
				distance += getDistance(coords[i], coords[i + 1]);
			}
			lane.setLength(distance);
		}
	}
	
	// Set the neighboring links/zones
	public void setNeighboringGraph() {
		Geography<Road> roadGeography = ContextCreator.getRoadGeography();
		for (Zone z1 : ContextCreator.getZoneContext().getAll()) {
			int threshold = 1000; // initial threshold as 1 km
			boolean flag = true;
			while (flag) {
				for (Zone z2 : ContextCreator.getZoneContext().getAll()) {
					if (this.getDistance(z1.getCoord(), z2.getCoord()) < threshold && z1 != z2) {
						z1.addNeighboringZone(z2.getID());
					}
				}
				if (z1.getNeighboringZoneSize() < ContextCreator.getZoneGeography().size() - 1) {
					threshold = threshold + 1000;
				} else {
					flag = false;
				}
			}
			GeometryFactory geomFac = new GeometryFactory();
			Point point = geomFac.createPoint(z1.getCoord());
			Geometry buffer = point.buffer(GlobalVariables.SEARCHING_BUFFER); 
			for (Road r : roadGeography.getObjectsWithin(buffer.getEnvelopeInternal(), Road.class)) {
				double dist = this.getDistance(z1.getCoord(), r.getStartCoord());
				if(dist < r.getDistToZone()) {
					r.setNeighboringZone(z1.getID());
					r.setDistToZone(dist);
				}
			} 
		}
		
		for (Road r: roadGeography.getAllObjects()) {
			if(r.getNeighboringZone() > 0) {
				ContextCreator.getZoneContext().get(r.getNeighboringZone()).addNeighboringLink(r.getID());
			}
		}
		
		for (Zone z: ContextCreator.getZoneContext().getAll()) {
			int i = 1;
			while (z.getNeighboringLinkSize() < 10) { // Take at least neighboring 10 links
				GeometryFactory geomFac = new GeometryFactory();
				Point point = geomFac.createPoint(z.getCoord());
				Geometry buffer = point.buffer(GlobalVariables.SEARCHING_BUFFER); 
				for (Road r : roadGeography.getObjectsWithin(buffer.getEnvelopeInternal(), Road.class)) {
					if(this.getDistance(z.getCoord(), r.getStartCoord()) < i * GlobalVariables.CRUISING_BUFFER) {
						z.addNeighboringLink(r.getID());
					}
				}
				i++;
			}
		}
		
	}
	
	public double getDistance(Coordinate c1, Coordinate c2) {
		GeodeticCalculator calculator = new GeodeticCalculator(ContextCreator.getLaneGeography().getCRS());
		calculator.setStartingGeographicPoint(c1.x, c1.y);
		calculator.setDestinationGeographicPoint(c2.x, c2.y);
		double distance = calculator.getOrthodromicDistance();
		return distance;
	}
	
	public void buildRoadNetwork() {
		
		System.out.println("Start builing road network");
		// Get lane geography
		Geography<Road> roadGeography = ContextCreator.getRoadGeography();
		Geography<Junction> junctionGeography = ContextCreator.getJunctionGeography();
		JunctionContext junctionContext = ContextCreator.getJunctionContext();
		Network<Node> roadNetwork = ContextCreator.getRoadNetwork();
		GeometryFactory geomFac = new GeometryFactory();
		
		// Create a repast network for routing
		if (GlobalVariables.NETWORK_FILE.length() == 0) {// Case 1: junction is not provided so we infer it from roads and lanes
			for (Road road : ContextCreator.getRoadContext().getAll()) {
				Geometry roadGeom = roadGeography.getGeometry(road);
				Coordinate c1 = roadGeom.getCoordinates()[0];
				Coordinate c2 = roadGeom.getCoordinates()[roadGeom.getNumPoints() - 1];
				// Create Junctions from road coordinates and add them to the
				// JunctionGeography (if they haven't been created already)
				Junction junc1, junc2;
				if (!ContextCreator.getJunctionContext().contains(road.getUpStreamJunction())) {
					junc1 = new Junction(road.getUpStreamJunction());
					junc1.setCoord(c1);
					ContextCreator.getJunctionContext().put(road.getUpStreamJunction(), junc1);
					Point p1 = geomFac.createPoint(c1);
					junctionGeography.move(junc1, p1);
				} else
					junc1 = ContextCreator.getJunctionContext().get(road.getUpStreamJunction());

				if (!ContextCreator.getJunctionContext().contains(road.getDownStreamJunction())) {
					junc2 = new Junction(road.getDownStreamJunction());
					junc2.setCoord(c2);
					ContextCreator.getJunctionContext().put(road.getDownStreamJunction(), junc2);
					Point p2 = geomFac.createPoint(c2);
					junctionGeography.move(junc2, p2);
				} else
					junc2 = ContextCreator.getJunctionContext().get(road.getDownStreamJunction());
				
				// Tell the road object who its junctions are
				road.setUpStreamJunction(junc1.getID());
				road.setDownStreamJunction(junc2.getID());
				this.coordRoad_KeyCoord.put(junc1.getCoord(), road);
				
				// Tell the junctions about their road
				junc1.addDownStreamRoad(road.getID());
				junc2.addUpStreamRoad(road.getID());
				
				Node node1, node2;
				node1 = new Node(100*road.getID()+1);
				node2 = new Node(100*road.getID()+2);
				ContextCreator.getNodeContext().put(node1.getID(), node1);
				ContextCreator.getNodeContext().put(node2.getID(), node2);
				// Tell the node about their junction
				node1.setJunction(junc1);
				node2.setJunction(junc2);
				
				// Tell the node about their road
				node1.setRoad(road);
				node2.setRoad(road);
				
				road.setUpStreamNode(node1);
				road.setDownStreamNode(node2);
				
				RepastEdge<Node> edge = new RepastEdge<Node>(node1, node2, true, road.getLength()/road.getFreeSpeed()); 
				
				if (!roadNetwork.containsEdge(edge)) {
					roadNetwork.addEdge(edge);
					this.edgeRoadID_KeyEdge.put(edge, road.getID());
					this.edgeIDEdge_KeyID.put(road.getID(), edge);
					if (this.nodeIDRoad_KeyNodeID.containsKey(node1.getID())) {
						this.nodeIDRoad_KeyNodeID.get(node1.getID()).put(node2.getID(), road);
					} else {
						HashMap<Integer, Road> tmp = new HashMap<Integer, Road>();
						tmp.put(node2.getID(), road);
						this.nodeIDRoad_KeyNodeID.put(node1.getID(), tmp);
					}
				} else {
					ContextCreator.logger
					.error("buildRoadNetwork1: this edge that has just been created "
							+ "already exists in the RoadNetwork!");
				}
			}
		} 
		else {// Case 2: junction is provided, xodr or SumoXML case
			OpenDriveMap odm = new OpenDriveMap(GlobalVariables.NETWORK_FILE);
			for (Junction j : odm.getJunction().values()) {
				ContextCreator.getJunctionContext().put(j.getID(), j);
			}
			for (Road r: ContextCreator.getRoadContext().getAll()) {
				Node node1, node2;
				node1 = new Node(10*r.getID()+(r.getID()>0?1:-1));
				node2 = new Node(10*r.getID()+(r.getID()>0?2:-2));
				ContextCreator.getNodeContext().put(node1.getID(), node1);
				ContextCreator.getNodeContext().put(node2.getID(), node2);
				// Tell the node about their road
				node1.setRoad(r);
				node2.setRoad(r);
				// Tell road about their node
				r.setUpStreamNode(node1);
				r.setDownStreamNode(node2);
			}
			for (int jid: odm.getRoadConnection().keySet()) {
				Junction j = ContextCreator.getJunctionContext().get(jid);
				int jx = 0;
				int jy = 0;
				for (List<Integer> rc: odm.getRoadConnection(jid)) {
					Road r1 = ContextCreator.getRoadContext().get(rc.get(0));
					Road r2 = ContextCreator.getRoadContext().get(rc.get(1));
					r1.setDownStreamJunction(jid);
					r2.setUpStreamJunction(jid);
					// set coordinates of the junction
					Geometry roadGeom1 = roadGeography.getGeometry(r1);
					Geometry roadGeom2 = roadGeography.getGeometry(r2);
					jx += roadGeom1.getCoordinates()[roadGeom1.getNumPoints()-1].x; 
					jy += roadGeom1.getCoordinates()[roadGeom1.getNumPoints()-1].y;
					jx += roadGeom2.getCoordinates()[0].x;
					jy += roadGeom2.getCoordinates()[0].y;
					// Tell the node about their junction
					Node node1 = r1.getDownStreamNode();
					Node node2 = r2.getUpStreamNode();
					node1.setJunction(j);
					node2.setJunction(j);
				}
				// set coordinates of the junction
				Coordinate jcoord = new Coordinate();
				jcoord.x = jx/odm.getRoadConnection(jid).size()/2;
				jcoord.y = jy/odm.getRoadConnection(jid).size()/2;
				ContextCreator.getJunctionContext().get(jid).setCoord(jcoord);
			}
			
			for(Road road: ContextCreator.getRoadContext().getAll()) {
				Node node1 = road.getUpStreamNode();
				Node node2 = road.getDownStreamNode();
				RepastEdge<Node> edge = new RepastEdge<Node>(node1, node2, true,
						road.getLength() / road.getFreeSpeed());

				if (!roadNetwork.containsEdge(edge)) {
					roadNetwork.addEdge(edge);
					this.edgeRoadID_KeyEdge.put(edge, road.getID());
					this.edgeIDEdge_KeyID.put(road.getID(), edge);
					if (this.nodeIDRoad_KeyNodeID.containsKey(node1.getID())) {
						this.nodeIDRoad_KeyNodeID.get(node1.getID()).put(node2.getID(), road);
					} else {
						HashMap<Integer, Road> tmp = new HashMap<Integer, Road>();
						tmp.put(node2.getID(), road);
						this.nodeIDRoad_KeyNodeID.put(node1.getID(), tmp);
					}
				} else {
					ContextCreator.logger.error("buildRoadNetwork1: this edge that has just been created "
							+ "already exists in the RoadNetwork!");
				}
			}
		}
		ContextCreator.logger.info("Junction initialized!");
		
		// Assign the lanes to each road
		for (Lane lane : ContextCreator.getLaneContext().getAll()) {
			Road road = ContextCreator.getRoadContext().get(lane.getRoad());
			road.addLane(lane);
		}
		for (Road r : ContextCreator.getRoadContext().getAll()) {
			r.sortLanes();
		}
		// Complete the lane connection
		// 1. handle the case when road is connected but there is no lane connection (U turn)
		// 2. handle upStreamLanes
		for (Road r1: ContextCreator.getRoadContext().getAll()) {
			for (int r2ID: r1.getDownStreamRoads()) {
				Road r2 = ContextCreator.getRoadContext().get(r2ID);
				boolean flag = true;
				for (Lane lane1: r1.getLanes()) {
					for (int lane2ID: lane1.getDownStreamLanes()) {
						Lane lane2 =  ContextCreator.getLaneContext().get(lane2ID);
						if(lane2.getRoad() == r2.getID()) {
							flag = false;
							break;
						}
					}
					if(!flag) break;
				}
				if(flag) {
					r1.getLane(0).addDownStreamLane(r2.getLane(0).getID());
				}
			}
		}
		for (Lane lane1 : ContextCreator.getLaneContext().getAll()) {
			for (int lane2ID: lane1.getDownStreamLanes()) {
				Lane lane2 = ContextCreator.getLaneContext().get(lane2ID);
				if(!lane2.getUpStreamLanes().contains(lane1.getID()))
					lane2.addUpStreamLane(lane1.getID());
			}
		}
		
		// TODO: check whether the traffic light info is provided, if so, process that given info.
		for (Junction j: ContextCreator.getJunctionContext().getAll()) {
			// Deduce the traffic light from road types, used when no traffic light info provided
			ArrayList<Integer> roadTypes = new ArrayList<Integer>();
			for(int r: j.getUpStreamRoads()) 
				roadTypes.add(ContextCreator.getRoadContext().get(r).getRoadType());
			for(int r: j.getDownStreamRoads()) 
				roadTypes.add(ContextCreator.getRoadContext().get(r).getRoadType());
			Collections.sort(roadTypes);
			// Establish control & signal & delay
			if (roadTypes.size() >= 2) {
				if ((roadTypes.get(0) == Road.Street)
						&& (roadTypes.get(j.getUpStreamRoads().size() - 1) <= Road.Highway)) {
					if (j.getUpStreamRoads().size() > 2) {
						j.setControlType(Junction.StaticSignal);
						int signalIndex=0;
						int signalNumber=j.getUpStreamRoads().size();
						for(int r1: j.getUpStreamRoads()) {
							for (int r2: ContextCreator.getRoadContext().get(r1).getDownStreamRoads()) {
								j.setDelay(r1, r2, 
										(int) Math.ceil((signalNumber-1)/signalNumber*
												(signalNumber-1)/signalNumber*
												15/GlobalVariables.SIMULATION_STEP_SIZE));
								Signal signal = new Signal(ContextCreator.generateAgentID(), 
										new ArrayList<Integer>(Arrays.asList(27,3,30*signalNumber)), 
												signalIndex*30);
								ContextCreator.getSignalContext().put(signal.getID(), signal);
								j.setSignal(r1, r2, signal);
							}
							signalIndex++;
						}
					}
					else {
						for(int r1: j.getUpStreamRoads()) {
							for (int r2: ContextCreator.getRoadContext().get(r1).getDownStreamRoads()) {
								j.setDelay(r1, r2, 0);
							}
						}
					}
				} else if ((roadTypes.get(0) == Road.Street)
						&& (roadTypes.get(j.getUpStreamRoads().size() - 1) == Road.Driveway)) {
					j.setControlType(Junction.StopSign);
					for(int r1: j.getUpStreamRoads()) {
						for (int r2: ContextCreator.getRoadContext().get(r1).getDownStreamRoads()) {
							j.setDelay(r1, r2, (int) Math.ceil(3/GlobalVariables.SIMULATION_STEP_SIZE));
						}
					}
				}
				else {
					for(int r1: j.getUpStreamRoads()) {
						for (int r2: ContextCreator.getRoadContext().get(r1).getDownStreamRoads()) {
							j.setDelay(r1, r2, 0);
						}
					}
				}
			}
			// Establish edges
			for(Integer r1: j.getDelay().keySet()) {
				for(Integer r2: j.getDelay().get(r1).keySet()) {
					Node node1 = ContextCreator.getRoadContext().get(r1).getDownStreamNode();
					Node node2 = ContextCreator.getRoadContext().get(r2).getUpStreamNode();
					RepastEdge<Node> edge = new RepastEdge<Node>(node1, node2, true,j.getDelay(r1, r2));
					if (!roadNetwork.containsEdge(edge)) {
						roadNetwork.addEdge(edge);
						this.edgeRoadID_KeyEdge.put(edge, -1); // negative value to signal this is not a road
					} else {
						ContextCreator.logger
								.error("buildRoadNetwork2: this edge that has just been created "
										+ "already exists in the RoadNetwork!");
					}
				}
			}
		}
		System.out.println("End builing road network");
	}

	// Update node based routing
	public void modifyRoadNetwork() {
		for (Road road : ContextCreator.getRoadContext().getAll()) {
			if(road.setTravelTime()) {
				Node node1 = road.getUpStreamNode();
				Node node2 = road.getDownStreamNode();
				ContextCreator.getRoadNetwork().getEdge(node1, node2).setWeight(road.getTravelTime());
			}
		}

		// At beginning, initialize route object
		if (!ContextCreator.isRouteUCBMapPopulated()) {
			try {
				RouteContext.createRoute();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	public int getRoadIDFromEdge(RepastEdge<Node> edge) {
		int id = this.edgeRoadID_KeyEdge.get(edge);
		return id;
	}

	public RepastEdge<?> getEdgeFromIDNum(int id) {
		RepastEdge<?> edge = this.edgeIDEdge_KeyID.get(id);
		return edge;
	}

	// Returns the closest charging station from the currentLocation
	public ChargingStation findNearestChargingStation(Coordinate coord) throws NullPointerException {
		if (coord == null) {
			throw new NullPointerException(
					"CityContext: findNearestChargingStation: ERROR: the input coordinate is null");
		}

		GeometryFactory geomFac = new GeometryFactory();
		Geography<ChargingStation> csGeography = ContextCreator.getChargingStationGeography();
		// Use a buffer for efficiency
		Point point = geomFac.createPoint(coord);
		Geometry buffer = point.buffer(GlobalVariables.SEARCHING_BUFFER);
		double minDist = Double.MAX_VALUE;
		ChargingStation nearestChargingStation = null;
		int num_tried = 0;
		while (nearestChargingStation == null && num_tried < 5) {
			for (ChargingStation cs : csGeography.getObjectsWithin(buffer.getEnvelopeInternal(), ChargingStation.class)) {
				double thisDist = this.getDistance(coord, cs.getCoord());
				if ((thisDist < minDist) && cs.capacity() > 0) {
					minDist = thisDist;
					nearestChargingStation = cs;
				}
			}
			num_tried += 1;
			buffer = point.buffer((num_tried + 1) * GlobalVariables.SEARCHING_BUFFER);
		}
		
		if (nearestChargingStation == null) { // Cannot find instant available charger, go the closest one and wait there
			for (ChargingStation cs : csGeography.getObjectsWithin(buffer.getEnvelopeInternal(),
					ChargingStation.class)) {
				double thisDist = this.getDistance(coord, cs.getCoord());
				if ((thisDist < minDist) && (cs.numL2() > 0 || cs.numL3() > 0)) {
					minDist = thisDist;
					nearestChargingStation = cs;
				}
			}
		}
		if (nearestChargingStation == null) {
			ContextCreator.logger.error(
					"CityContext: findNearestChargingStation (Coordinate coord): ERROR: couldn't find a charging station at these coordinates:\n\t"
							+ coord.toString());
		}

		return nearestChargingStation;
	}

	// Returns the closest charging station for bus from the currentLocation
	public ChargingStation findNearestBusChargingStation(Coordinate coord) throws NullPointerException {
		if (coord == null) {
			throw new NullPointerException(
					"CityContext: findNearestChargingStation: ERROR: the input coordinate is null");
		}
		GeometryFactory geomFac = new GeometryFactory();
		Geography<ChargingStation> csGeography = ContextCreator.getChargingStationGeography();
		// Use a buffer for efficiency
		Point point = geomFac.createPoint(coord);
		Geometry buffer = point.buffer(GlobalVariables.SEARCHING_BUFFER);
		double minDist = Double.MAX_VALUE;
		ChargingStation nearestChargingStation = null;
		for (ChargingStation cs : csGeography.getObjectsWithin(buffer.getEnvelopeInternal(), ChargingStation.class)) {
			double thisDist = this.getDistance(coord, cs.getCoord());
			if ((thisDist < minDist)) { // if thisDist < minDist
				minDist = thisDist;
				nearestChargingStation = cs;
			}
		}
		if (nearestChargingStation == null) {
			ContextCreator.logger.error(
					"CityContext: findNearestChargingStation (Coordinate coord): ERROR: couldn't find a charging station at these coordinates:\n\t"
							+ coord.toString());
		}
		return nearestChargingStation;
	}
	
	// Returns the closest road from the currentLocation 
	public Road findRoadAtCoordinates(Coordinate coord) throws NullPointerException {
		if (coord == null) {
			throw new NullPointerException("CityContext: findRoadAtCoordinates: ERROR: the input coordinate is null");
		}
		
		if(this.coordRoad_KeyCoord.containsKey(coord)) {
			return this.coordRoad_KeyCoord.get(coord);
		}
		else {
			GeometryFactory geomFac = new GeometryFactory();
			Geography<?> roadGeography = ContextCreator.getRoadGeography();
			// Use a buffer for efficiency
			Point point = geomFac.createPoint(coord);
			Geometry buffer = point.buffer(GlobalVariables.SEARCHING_BUFFER);
			double minDist = Double.MAX_VALUE;
			Road nearestRoad = null;
	
			// New code when nearest road was found based on distance from junction
			for (Road road : roadGeography.getObjectsWithin(buffer.getEnvelopeInternal(), Road.class)) {
				double thisDist = this.getDistance(coord, road.getStartCoord());
				if (thisDist < minDist) { 
					minDist = thisDist;
					nearestRoad = road;
				}
			}
			if (nearestRoad == null) { // for nearRoads
				ContextCreator.logger.error(
						"CityContext: findRoadAtCoordinates (Coordinate coord, boolean toDest): ERROR: couldn't find a road at these coordinates:\n\t"
								+ coord.toString());
			}
			else {
				this.coordRoad_KeyCoord.put(coord, nearestRoad);
			}
			return nearestRoad;
		}
	}

	public Road findRoadBetweenNodeIDs(int node1, int node2) {
		if(this.nodeIDRoad_KeyNodeID.containsKey(node1)) {
			if(this.nodeIDRoad_KeyNodeID.get(node1).containsKey(node2)) {
				return this.nodeIDRoad_KeyNodeID.get(node1).get(node2);
			}
		}
		return null;
	}

	public static double angle(Coordinate p0, Coordinate p1) {
		double dx = p1.x - p0.x;
		double dy = p1.y - p0.y;

		return Math.atan2(dy, dx);
	}

	public static double squaredEuclideanDistance(Coordinate p0, Coordinate p1) {
		return (p0.x - p1.x) * (p0.x - p1.x) + (p0.y - p1.y) * (p0.y - p1.y);
	}

	// Returns the closest zone from the currentLocation that has a bus available
	public Zone findNearestZoneWithBus(Coordinate coord, Integer destID) throws NullPointerException {
		if (coord == null) {
			throw new NullPointerException("CityContext: findNearestZoneWithBus: ERROR: the input coordinate is null");
		}
		// Use a buffer for efficiency
		double minDist = Double.MAX_VALUE;
		Zone nearestZone = null;
		for (Zone z : ContextCreator.getZoneContext().getAll()) {
			if (z.busReachableZone.contains(destID)) {
				double thisDist = this.getDistance(coord, z.getCoord());
				if (thisDist < minDist) {
					minDist = thisDist;
					nearestZone = z;
				}
			}
		}
		return nearestZone;
	}

	public void refreshTravelTime() {
		ContextCreator.logger.info("Update the estimation of travel time...");
		// Reset the travel time and travel distance estimation
		for (Zone z1 : ContextCreator.getZoneContext().getAll()) {
			z1.busTravelTime.clear();
			z1.busTravelDistance.clear();
			z1.nearestZoneWithBus.clear();
		}
		for (List<Integer> route : ContextCreator.busSchedule.getBusSchedule()) {
			ContextCreator.logger.info(route);
			// Retrieve stations in order, from hub to other places
			double travel_distance = 0;
			double travel_time = 0;

			for (int shift = 0; shift < route.size(); shift++) {
				if (GlobalVariables.HUB_INDEXES.contains(route.get(shift))) { // is hub
					Zone hub = ContextCreator.getZoneContext().get(route.get(shift));
					Zone z1 = hub;
					Zone z2;

					for (int i = 1; i < route.size(); i++) {
						int j = shift + i >= route.size() ? shift + i - route.size() : shift + i;
						z2 = ContextCreator.getZoneContext().get(route.get(j));
						List<Road> path = RouteContext.shortestPathRoute(z1.getCoord(), z2.getCoord());
						if (path != null) {
							for (Road r : path) {
								travel_distance += r.getLength();
								travel_time += r.getTravelTime();
							}
						}
						if (hub.busTravelDistance.containsKey(z2.getIntegerID())) {
							hub.busTravelDistance.put(z2.getIntegerID(),
									Math.min(hub.busTravelDistance.get(z2.getIntegerID()), (float) travel_distance));
							hub.busTravelTime.put(z2.getIntegerID(),
									Math.min(hub.busTravelTime.get(z2.getIntegerID()), (float) travel_time));
						} else {
							hub.busTravelDistance.put(z2.getIntegerID(), (float) travel_distance);
							hub.busTravelTime.put(z2.getIntegerID(), (float) travel_time);
						}
						z1 = z2;
					}
					ContextCreator.logger.debug(hub.busTravelDistance);
					ContextCreator.logger.debug(hub.busTravelTime);
					// Retrieve stations in back order, from other places to hub
					travel_distance = 0;
					travel_time = 0;
					z2 = hub;
					for (int i = route.size() - 1; i > 0; i--) {
						int j = shift + i >= route.size() ? shift + i - route.size() : shift + i;
						z1 = ContextCreator.getZoneContext().get(route.get(j));
						List<Road> path = RouteContext.shortestPathRoute(z1.getCoord(), z2.getCoord());
						if (path != null) {
							for (Road r : path) {
								travel_distance += r.getLength();
								travel_time += r.getTravelTime();
							}
						}
						if (z1.busTravelDistance.containsKey(hub.getIntegerID())) {
							z1.busTravelDistance.put(hub.getIntegerID(),
									Math.min(z1.busTravelDistance.get(hub.getIntegerID()), (float) travel_distance));
							z1.busTravelTime.put(hub.getIntegerID(),
									Math.min(z1.busTravelTime.get(hub.getIntegerID()), (float) travel_time));
						} else {
							z1.busTravelDistance.put(hub.getIntegerID(), (float) travel_distance);
							z1.busTravelTime.put(hub.getIntegerID(), (float) travel_time);
						}
						z2 = z1;
						ContextCreator.logger.debug(z1.busTravelDistance);
						ContextCreator.logger.debug(z1.busTravelTime);
					}
				}
			}
		}
	}
}
