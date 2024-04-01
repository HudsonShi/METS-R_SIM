package mets_r.mobility;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import com.vividsolutions.jts.geom.Coordinate;

import mets_r.ContextCreator;
import mets_r.GlobalVariables;
import mets_r.facility.ChargingStation;
import mets_r.facility.Road;
import mets_r.facility.Zone;
import mets_r.routing.RouteContext;
import util.Pair;

/**
 * Electric buses
 * 
 * @author Zengxiang Lei, Jiawei Xue
 */

public class ElectricBus extends ElectricVehicle {
	/* Constants */
	// Parameters for Fiori (2016) model,
	// Modified frontal area and draft coefficient according to Bus 2 in the paper
	// "Aerodynamic Exterior Body Design of Bus"
	public static double A = 6.93; // frontal area of the vehicle
	public static double cd = 0.68; // draft coefficient
	public static double Pconst = 5500; // energy consumption by auxiliary accessories
	public static double batteryCapacity = GlobalVariables.BUS_BATTERY; // the storedEnergy is 250 kWh.
	
	/* Local variables */
	private int routeID;
	private int passNum;
	private int numSeat; // Capacity of the bus.
	private int nextStop; // nextStop =i means the i-th entry of ArrayList<Integer> busStop.
							// i=0,1,2,...,17.
	private ArrayList<Integer> busStop;
	// Each entry represents a bus stop (zone) along the bus route.
	// For instance, it is [0,1,2,3,4,5,6,7,8,9,8,7,6,5,4,3,2,1];
	private Dictionary<Integer, Integer> stopBus; // Reverse table for track the zone in the busStop;
	// Timetable variable here, the next departure time
	private int nextDepartureTime;
	
	private ArrayList<Integer> destinationDemandOnBus; // The destination distribution of passengers on the bus now.
	// [x0,x1,x2,x3,x4,x5,x6,x7,x8,x9]. xi means that there are xi passengers having
	// the destination of zone i.
	private double avgPersonMass_; // average mass of a person in lbs
	private double lowerBatteryRechargeLevel_;
	private double higherBatteryRechargeLevel_;
	private double mass_; // mass of the vehicle (consider passengers' weight) in kg for energy calculation
	private double mass; // mass of the vehicle in kg
	
	// Parameters for storing energy consumptions
	private double linkConsume; // for UCB eco-routing, energy spent for passing current link, will be reset to
								// zero once this EV entering a new road.
	
	/* Public variables */
	// For operational features
	public ArrayList<Queue<Request>> passengerWithAdditionalActivityOnBus;
	
	// Service metrics
	public int served_pass = 0;
	
	// Parameter to show which route has been chosen in eco-routing.
	private int routeChoice = -1;
	

	// Constructor
	public ElectricBus(int routeID, ArrayList<Integer> route, int nextDepartureTime) {
		super(1.2, -2.0, Vehicle.EBUS, Vehicle.VANILLA); // max acc, min dc, and vehicle class
		this.routeID = routeID;
		this.busStop = route;
		this.stopBus = new Hashtable<Integer, Integer>();
		for (int i = 0; i < route.size(); i++) {
			stopBus.put(route.get(i), i);
		}
		this.nextDepartureTime = nextDepartureTime;
		this.destinationDemandOnBus = new ArrayList<Integer>(Collections.nCopies(this.busStop.size(), 0));
		this.passengerWithAdditionalActivityOnBus = new ArrayList<Queue<Request>>();
		for (int i = 0; i < route.size(); i++) {
			this.passengerWithAdditionalActivityOnBus.add(new LinkedList<Request>());
		}
		this.passNum = 0;
		this.nextStop = Math.min(1, this.busStop.size() - 1);
		this.numSeat = 40;
		this.batteryLevel_ = GlobalVariables.BUS_RECHARGE_LEVEL_LOW * GlobalVariables.BUS_BATTERY
				+this.rand.nextDouble() * (1 - GlobalVariables.BUS_RECHARGE_LEVEL_LOW) * GlobalVariables.BUS_BATTERY; // unit:kWh
		this.lowerBatteryRechargeLevel_ = GlobalVariables.BUS_RECHARGE_LEVEL_LOW * GlobalVariables.BUS_BATTERY;
		this.higherBatteryRechargeLevel_ = GlobalVariables.BUS_RECHARGE_LEVEL_HIGH * GlobalVariables.BUS_BATTERY;
		this.mass = 18000.0; // the weight of bus is 18t.
		this.mass_ = mass * 1.05;
		this.avgPersonMass_ = 180.0;
	}

	// UpdateBatteryLevel
	@Override
	public void updateBatteryLevel() {
		if (this.routeID >= 0) {
			double tickEnergy = calculateEnergy(); // the energy consumption(kWh) for this tick
			tickConsume = tickEnergy;
			totalConsume += tickEnergy;
			linkConsume += tickEnergy;
			tripConsume += tickEnergy;
			batteryLevel_ -= tickEnergy;
		}
	}

	public void goCharging() {
		int current_dest_zone = this.getDestID();
		Coordinate current_dest_coord = this.getDestCoord();
		this.onChargingRoute_ = true;
		
		ChargingStation cs = ContextCreator.getCityContext().findNearestBusChargingStation(this.getCurrentCoord());
		this.addPlan(cs.getID(), cs.getCoord(), (int) ContextCreator.getNextTick()); // instantly go to
																								// charging station
		this.setNextPlan();
		this.addPlan(current_dest_zone, current_dest_coord, (int) ContextCreator.getNextTick()); // Follow the old
																									// schedule
		this.setState(Vehicle.CHARGING_TRIP);
		this.departure();
		ContextCreator.logger.debug("Bus " + this.getID() + " is on route to charging station");
	}
	
	@Override
	public void setNextRoad() {
		if(!this.atOrigin) {
			super.setNextRoad();
		}
		else {
			// Clear legacy impact
			this.clearShadowImpact();
			this.roadPath = new ArrayList<Road>();
			if (!ContextCreator.routeResult_received_bus.isEmpty() && GlobalVariables.ENABLE_ECO_ROUTING_BUS) {
				Pair<List<Road>, Integer> route_result = RouteContext.ecoRouteBus(this.getRoad(), this.getOriginID(), this.getDestID());
				this.roadPath = route_result.getFirst();
				this.routeChoice = route_result.getSecond();
			}
			// Compute new route if eco-routing is not used or the OD pair is uncovered
			if (this.roadPath == null || this.roadPath.isEmpty()) {
				this.roadPath = RouteContext.shortestPathRoute(this.getRoad(), this.getDestCoord(), this.rand_route_only); // K-shortest path or shortest path
			}
			this.setShadowImpact();
			if (this.roadPath == null) {
				ContextCreator.logger.error("Routing fails with origin: " + this.getRoad().getID() + ", destination " + this.getDestCoord() + 
						", destination road " + this.getDestRoadID());
				this.atOrigin = false;
				this.nextRoad_ = null;
			}
			else if (this.roadPath.size() < 2) { // The origin and destination share the same Junction
				this.atOrigin = false;
				this.nextRoad_ = null;
			} else {
				this.atOrigin = false;
				this.nextRoad_ = roadPath.get(1);
				this.assignNextLane();
			}
		}
	}

	// The setReachDest() function applies for three cases:
	// Case 1: arrive at the charging station.
	// Case 2: arrive at the start bus stop, and then go the charging station
	// Case 3: (arrive at the other bus stop), or (arrive at the start bus stop and
	// continue to move)
	@Override
	public void reachDest() {
		// Case 1: the bus arrives at the charging station
		if (onChargingRoute_) {
			String formated_msg = ContextCreator.getCurrentTick() + "," + this.getID() + "," + this.getRouteID()
					+ ",4," + this.getOriginID() + "," + this.getDestID() + "," + this.getAccummulatedDistance() + ","
					+ this.getDepTime() + "," + this.getTripConsume() + ",-1" + "," + this.getPassNum() + "\r\n";
			try {
				ContextCreator.agg_logger.bus_logger.write(formated_msg);
			} catch (IOException e) {
				e.printStackTrace();
			}
			super.reachDestButNotLeave(); 
			super.leaveNetwork(); // remove the bus from the network
			ContextCreator.logger.debug("Bus arriving at charging station:" + this.getID());
			ChargingStation cs = ContextCreator.getChargingStationContext().get(this.getDestID());
			cs.receiveBus(this);
			this.tripConsume = 0;
			// Case 2: the bus arrives at the start bus stop
		} else {
			// Case 3: (arrive at the other bus stop), or (arrive at the start bus stop and
			// continue to move)
			// drop off passengers at the stop
			if (this.getRouteID() > 0) {
				String formated_msg = ContextCreator.getCurrentTick() + "," + this.getID() + ","
						+ this.getRouteID() + ",3," + this.getOriginID() + "," + this.getDestID() + ","
						+ this.getAccummulatedDistance() + "," + this.getDepTime() + "," + this.getTripConsume() + ","
						+ this.routeChoice + "," + this.getPassNum() + "\r\n";
				try {
					ContextCreator.agg_logger.bus_logger.write(formated_msg);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}

			this.tripConsume = 0;

			ContextCreator.logger.debug("Bus arriving at bus stop: " + nextStop);
			this.setPassNum(this.getPassNum() - this.destinationDemandOnBus.get(nextStop % busStop.size()));
			ContextCreator.getZoneContext().get(
					this.getDestID()).busServedRequest += this.destinationDemandOnBus.get(nextStop % busStop.size());
			this.destinationDemandOnBus.set(nextStop % busStop.size(), 0);

			if (this.passengerWithAdditionalActivityOnBus.get(nextStop % busStop.size()).size() > 0) {
				for (Request p = this.passengerWithAdditionalActivityOnBus.get(nextStop % busStop.size())
						.poll(); !this.passengerWithAdditionalActivityOnBus.get(nextStop % busStop.size())
								.isEmpty(); p = this.passengerWithAdditionalActivityOnBus.get(nextStop % busStop.size())
										.poll()) {
					p.moveToNextActivity();
					ContextCreator.getZoneContext().get(this.getDestID()).insertTaxiPass(p);
				}
			}
			
			super.reachDestButNotLeave(); // Update the vehicle status
			// Decide the next step
			if (nextStop == busStop.size() || this.routeID == -1) { // arrive at the last stop
				this.routeID = -1; // Clear the previous route ID
				if (batteryLevel_ <= lowerBatteryRechargeLevel_) {
					this.goCharging();
				} else if (GlobalVariables.PROACTIVE_CHARGING && batteryLevel_ <= higherBatteryRechargeLevel_
						&& !ContextCreator.busSchedule.hasSchedule(this.busStop.get(0))) {
					this.goCharging();
				} else {
					ContextCreator.busSchedule.popSchedule(this.busStop.get(0), this);
					super.leaveNetwork();
					this.addPlan(busStop.get(nextStop),
							ContextCreator.getZoneContext().get(busStop.get(nextStop)).getCoord(),
							Math.max((int) ContextCreator.getNextTick(), nextDepartureTime));
					this.setNextPlan();
					this.departure();
				}
			} else {
				// Passengers get on board
				this.servePassenger();
				this.nextStop = nextStop + 1;
				// Head to the next Stop
				int destZoneID = busStop.get(nextStop % busStop.size());
				this.addPlan(destZoneID, ContextCreator.getZoneContext().get(destZoneID).getCoord(),
						Math.max((int) ContextCreator.getNextTick(), nextDepartureTime));
				this.setNextPlan();
				this.departure();
			}
			ContextCreator.logger.debug("Bus " + this.ID + " has arrive the next station: " + nextStop);
		}
	}

	private void servePassenger() {
		// ServePassengerByBus
		Zone arrivedZone = ContextCreator.getZoneContext().get(this.getNextStopZoneID());
		ArrayList<Request> passOnBoard = arrivedZone.servePassengerByBus(this.numSeat - this.passNum, busStop);
		for (Request p : passOnBoard) {
			this.destinationDemandOnBus.set(this.stopBus.get(p.getDestination()),
					destinationDemandOnBus.get(this.stopBus.get(p.getDestination())) + 1);
			if (p.lenOfActivity() > 1) {
				this.passengerWithAdditionalActivityOnBus.get(this.stopBus.get(p.getDestination())).add(p);
			}
		}
		this.served_pass += passOnBoard.size();
		this.setPassNum(this.getPassNum() + passOnBoard.size());
	}

	public int getPassNum() {
		return this.passNum;
	}

	public void setPassNum(int numPeople) {
		this.passNum = numPeople;
	}

	public int getNextStopZoneID() {
		return busStop.get(nextStop);
	}
	
	public int getCurrentStop() {
		return this.nextStop - 1;
	}

	public int getRouteID() {
		return this.routeID;
	}

	public double getMass() {
		return this.mass_ + this.avgPersonMass_ * passNum;
	}
	
	@Override
	public void finishCharging(Integer chargerID, String chargerType) {
		String formated_msg = ContextCreator.getCurrentTick() + "," + chargerID + "," + this.getID() + ","
				+ this.getVehicleClass() + "," + chargerType + "," + this.chargingWaitingTime + ","
				+ this.chargingTime + "," + this.initialChargingState + "\r\n";
		try {
			ContextCreator.agg_logger.charger_logger.write(formated_msg);
			this.chargingWaitingTime = 0;
			this.chargingTime = 0;
		} catch (IOException e) {
			e.printStackTrace();
		}
		this.onChargingRoute_ = false;
		this.setNextPlan();
		this.setState(Vehicle.BUS_TRIP);
		this.departure();
		
	}

	public double getLinkConsume() {
		return linkConsume;
	}

	// Reset link consume once a EV has passed a link
	public void resetLinkConsume() {
		this.linkConsume = 0;
	}

	public void updateSchedule(int newID, ArrayList<Integer> newRoute, ArrayList<Integer> departureTime) {
		if (newID == -1) {
			this.routeID = -1;
			this.busStop = new ArrayList<Integer>(Arrays.asList(this.busStop.get(0)));
			this.nextDepartureTime = (int) (ContextCreator.getCurrentTick()
					+ 600 / GlobalVariables.SIMULATION_STEP_SIZE); // Wait for 1 min
		} else {
			this.routeID = newID;
			this.busStop = newRoute;
			this.stopBus = new Hashtable<Integer, Integer>();
			for (int i = 0; i < this.busStop.size(); i++) {
				this.stopBus.put(this.busStop.get(i), i);
			}
			this.nextDepartureTime = departureTime.get(0);
		}
		this.nextStop = 0;

		this.passengerWithAdditionalActivityOnBus = new ArrayList<Queue<Request>>();
		for (int i = 0; i < this.busStop.size(); i++) {
			this.passengerWithAdditionalActivityOnBus.add(new LinkedList<Request>());
		}
		this.destinationDemandOnBus = new ArrayList<Integer>(Collections.nCopies(this.busStop.size(), 0));
		this.setPassNum(0);
	}

}
