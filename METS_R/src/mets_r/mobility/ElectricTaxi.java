package mets_r.mobility;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import org.apache.commons.math3.analysis.interpolation.SplineInterpolator;
import org.apache.commons.math3.analysis.polynomials.PolynomialSplineFunction;

import com.vividsolutions.jts.geom.Coordinate;

import mets_r.ContextCreator;
import mets_r.GlobalVariables;
import mets_r.data.DataCollector;
import mets_r.facility.ChargingStation;
import mets_r.facility.Road;
import mets_r.facility.Zone;
import mets_r.routing.RouteV;
import repast.simphony.essentials.RepastEssentials;
import util.Pair;

/**
 * Electric taxis
 * @author Zengxiang Lei, Jiawei Xue, Juan Suarez
 *
 */

public class ElectricTaxi extends Vehicle {
	/* Constant */
	public static double gravity = 9.8; // the gravity is 9.80N/kg for NYC
	public static double batteryCapacity = GlobalVariables.EV_BATTERY; // the storedEnergy is 50 kWh.
	
	// Parameters for Fiori (2016) model
	public static double p0 = 1.2256;
	public static double A = 2.3316;
	public static double cd = 0.28;
	public static double cr = 1.75;
	public static double c1 = 0.0328;
	public static double c2 = 4.575;
	public static double etaM = 0.92;
	public static double etaG = 0.91;
	public static double Pconst = 1500; // energy consumption by auxiliary accessories

	// Parameters for Maia (2012) model
	// public static double urr = 0.005; // 1996_1998_General Motor Model
	// public static double densityAir = 1.25; // air density = 1.25kg/m3
	// public static double A = 1.89; // A = 1.89m2
	// public static double Cd = 0.19; // Cv
	// Parameter for Fhc, Fla calculation
	// public static double etaM = 0.95; // etaM
	// public static double etaG = 0.95; // etaG
	// public static double Pconst = 1300.0; // pConst
	// public static double Voc = 325.0; // nominal system voltage
	// public static double rIn = 0.0; //
	// public static double T = 20.0; // assume the temperature is 20 Celsius
	// degree;
	// public static double k = 1.03; // k is the Peukert coefficient, k = 1.03;
	// public static double cp = 77.0; // cp = 77.0; ///nominal capacity: 77 AH
	// public static double c = 20.0;

	// Local variables
	private int numPeople_; // no of people inside the vehicle
	public Queue<Request> passengerWithAdditionalActivityOnTaxi;
	private double avgPersonMass_; // average mass of a person in lbs
	private double batteryLevel_; // current battery level
	private double lowerBatteryRechargeLevel_;
	private double higherBatteryRechargeLevel_;
	private double mass; // mass of the vehicle in kg
	private boolean onChargingRoute_ = false;
	private int cruisingTime_;

	// Parameters for storing energy consumptions
	private double tickConsume;
	private double totalConsume;
	private double linkConsume; // For UCB eco-routing, energy spent for passing current link, will be reset to
								// zero once this ev entering a new road.
	private double tripConsume; // For UCB testing
	
	// Service metrics
	public int served_pass = 0;
	public int charging_time = 0;
	public int charging_waiting_time = 0;
	public double initial_charging_state = 0;

	// Parameter to show which route has been chosen in eco-routing.
	private int routeChoice = -1;
	
	public ElectricTaxi() {
		super(Vehicle.ETAXI);
		this.setInitialParams();
	}

	public ElectricTaxi(float maximumAcceleration, float maximumDeceleration) {
		super(maximumAcceleration, maximumDeceleration, Vehicle.ETAXI);
		this.setInitialParams();
	}

	// Find the closest charging station and update the activity plan
	public void goCharging() {
		int current_dest_zone = this.getDestID();
		Coordinate current_dest_coord = ContextCreator.getCityContext().findZoneWithIntegerID(this.getDestID()).getCoord();
		// Add a charging activity
		ChargingStation cs = ContextCreator.getCityContext().findNearestChargingStation(this.getCurrentCoord());
		this.onChargingRoute_ = true;
		this.addPlan(cs.getIntegerID(), cs.getCoord(), (int) RepastEssentials.GetTickCount());
		this.setNextPlan();
		this.addPlan(current_dest_zone, current_dest_coord, (int) RepastEssentials.GetTickCount());
		this.setState(Vehicle.CHARGING_TRIP);
		this.departure();
		ContextCreator.logger.debug("Vehicle " + this.getId() + " is on route to charging");
	}
	
	// Randomly select a neighboring link and update the activity plan
	public void goCrusing(Zone z) {
		// Add a cruising activity
		Road r = z.getNeighboringLink(this.rand.nextInt(z.getNeighboringLinkSize()));
		while(r.getJunctions().get(0) == this.getRoad().getJunctions().get(1)) {
			r = z.getNeighboringLink(this.rand.nextInt(z.getNeighboringLinkSize()));
		}
		this.addPlan(z.getIntegerID(), r.getJunctions().get(1).getCoordinate(), (int) RepastEssentials.GetTickCount());
		this.setNextPlan();
		this.setState(Vehicle.CRUISING_TRIP);
		this.departure();
	}
	
	// Stop cruising
	public void stopCruising() {
		
		// Log the cruising trip here
		String formated_msg = RepastEssentials.GetTickCount() + "," + this.getVehicleID() + "," + this.getState()
		+ "," + this.getOriginID() + "," + this.getDestID() + "," + this.getAccummulatedDistance() + ","
		+ this.getDepTime() + "," + this.getTripConsume() + "," + this.getRouteChoice() + ","
		+ this.getNumPeople()+ "\r\n";
		try {
			ContextCreator.ev_logger.write(formated_msg);
		} catch (IOException e) {
			e.printStackTrace();
		}
		this.tripConsume = 0;
		
		if(!this.isOnLane()) { // The vehicle is currently in a junction, so the routing will fail if not leave the network
			this.leaveNetwork();
		}
		
		this.setState(Vehicle.NONE_OF_THE_ABOVE);
	}
	
	// Find the closest Zone with parking space and relocate to their
	public void goParking() {
		ContextCreator.getCityContext().findZoneWithIntegerID(this.getDestID()).removeOneCruisingVehicle();
		for(Zone z: ContextCreator.getCityContext().findZoneWithIntegerID(this.getDestID()).getNeighboringZones()) {
			if(z.getCapacity()>0) {
				ContextCreator.getVehicleContext().getVehiclesByZone(this.getDestID()).remove(this);
				ContextCreator.getCityContext().findZoneWithIntegerID(this.getDestID()).numberOfRelocatedVehicles += 1;
				z.addFutureSupply();
				this.relocation(this.getDestID(), z.getIntegerID());
				return;
			}
		}
	}
	
	// Parking at Zone z
	public void getParked(Zone z) {
		super.leaveNetwork();
		this.setState(Vehicle.PARKING);
	}

	@Override
	public void updateBatteryLevel() {
		double tickEnergy = calculateEnergy(); // The energy consumption(kWh) for this tick
		tickConsume = tickEnergy;
		linkConsume += tickEnergy; // Update energy consumption on current link
		totalConsume += tickEnergy;
		tripConsume += tickEnergy;
		batteryLevel_ -= tickEnergy;
	}

	// Relocate vehicle
	/**
	 * @param p
	 */
	public void relocation(int orginID, int destinationID) {
		this.stopCruising();
		this.addPlan(destinationID, ContextCreator.getCityContext().findZoneWithIntegerID(destinationID).getCoord(),
				(int) RepastEssentials.GetTickCount());
		this.setNextPlan();
		// Add vehicle to newqueue of corresponding road
		this.departure();
		this.setState(Vehicle.RELOCATION_TRIP);
	}

	/**
	 * @param list of passengers
	 */
	public void servePassenger(List<Request> plist) {
		if (!plist.isEmpty()) {
			if(this.getState() == Vehicle.CRUISING_TRIP) {
				this.stopCruising();
				// Ask the vehicle to move back to serve the request
				this.addPlan(this.getDestID(),
						ContextCreator.getCityContext().findZoneWithIntegerID(this.getDestID()).getCoord(),
						(int) RepastEssentials.GetTickCount());
				this.setState(Vehicle.PICKUP_TRIP);
			}
			else if (this.getState() == Vehicle.PARKING) {
				this.setState(Vehicle.OCCUPIED_TRIP);
			}
			else {
				ContextCreator.logger.error("Something went wrong, the vehicle is not cruising or parking but still in the zone!");
			}
			
			for (Request p : plist) {
				this.addPlan(p.getDestination(),
						ContextCreator.getCityContext().findZoneWithIntegerID(p.getDestination()).getCoord(),
						(int) RepastEssentials.GetTickCount());
				this.served_pass += 1;
				this.setNumPeople(this.getNumPeople() + 1);
			}
			this.setNextPlan();
			// Add vehicle to new queue of corresponding road
			this.departure();
			
		}
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
			if (!ContextCreator.routeResult_received.isEmpty() && GlobalVariables.ENABLE_ECO_ROUTING_EV) {
				Pair<List<Road>, Integer> route_result = RouteV.ecoRoute(this.getOriginID(), this.getDestID());
				this.roadPath = route_result.getFirst();
				this.setRouteChoice(route_result.getSecond());
			}
			// Compute new route if eco-routing is not used or the OD pair is uncovered
			if (this.roadPath == null || this.roadPath.isEmpty() || this.roadPath.get(0) != this.getRoad()) {
				this.roadPath = RouteV.vehicleRoute(this, this.getDestCoord()); // K-shortest path or shortest path
			}
			this.setShadowImpact();
			if (this.roadPath == null || this.roadPath.size() < 2) { // The origin and destination share the same Junction
				this.atOrigin = false;
				this.nextRoad_ = null;
			} else {
				this.atOrigin = false;
				this.nextRoad_ = roadPath.get(1);
			}
		}
	}

	@Override
	public void setReachDest() {
		// Check if the vehicle was on a charging route
		if (this.onChargingRoute_) {
			String formated_msg = RepastEssentials.GetTickCount() + "," + this.getVehicleID() + ",4,"
					+ this.getOriginID() + "," + this.getDestID() + "," + this.getAccummulatedDistance() + ","
					+ this.getDepTime() + "," + this.getTripConsume() + ",-1" + "," + this.getNumPeople() + "\r\n";
			try {
				ContextCreator.ev_logger.write(formated_msg);
			} catch (IOException e) {
				e.printStackTrace();
			}
			super.setReachDest(); 
			super.leaveNetwork(); // remove from the network
			// Add to the charging station
			ContextCreator.logger.debug("Vehicle arriving at charging station:" + this.getId());
			ChargingStation cs = ContextCreator.getCityContext().findChargingStationWithID(this.getDestID());
			cs.receiveEV(this);
			this.tripConsume = 0;
		} else {
			// Log the trip consume here
			String formated_msg = RepastEssentials.GetTickCount() + "," + this.getVehicleID() + "," + this.getState()
					+ "," + this.getOriginID() + "," + this.getDestID() + "," + this.getAccummulatedDistance() + ","
					+ this.getDepTime() + "," + this.getTripConsume() + "," + this.getRouteChoice() + ","
					+ this.getNumPeople()+ "\r\n";
			try {
				ContextCreator.ev_logger.write(formated_msg);
			} catch (IOException e) {
				e.printStackTrace();
			}
			this.tripConsume = 0;

			Zone z = ContextCreator.getCityContext().findZoneWithIntegerID(this.getDestID()); // get destination zone info
			
			super.setReachDest(); // Update the vehicle status
			
			// Decide the next step
			if (this.getState() == Vehicle.OCCUPIED_TRIP) {
				this.setNumPeople(this.getNumPeople() - 1); // passenger arrived
				z.taxiServedRequest += 1;
				// if pass need to take the bus to complete his or her trip
				if (this.passengerWithAdditionalActivityOnTaxi.size() > 0) {
					// generate a pass and add it to the corresponding zone
					Request p = this.passengerWithAdditionalActivityOnTaxi.poll();
					p.moveToNextActivity();
					if (z.busReachableZone.contains(p.getDestination())) {
						z.insertBusPass(p); // if bus can reach the destination
					} else {
						z.insertTaxiPass(p); // this is called when we dynamically update bus schedules
					}
				}
				
				z.removeFutureSupply();
				if (this.getNumPeople() > 0) { // keep serving more passengers
					super.leaveNetwork();
					this.setNextPlan();
					ContextCreator.getCityContext().findZoneWithIntegerID(this.getDestID()).addFutureSupply();
					this.departure();
				}
				else { // charging or join the current zone
					if(batteryLevel_ <= lowerBatteryRechargeLevel_ || (GlobalVariables.PROACTIVE_CHARGING
							&& batteryLevel_ <= higherBatteryRechargeLevel_ && z.hasEnoughTaxi(5))) {
						this.goCharging();
					}
					else { // join the current zone
						ContextCreator.getVehicleContext().getVehiclesByZone(z.getIntegerID()).add(this);
						if(z.getCapacity() > 0) { // Has capacity
							z.addOneParkingVehicle();
		                	this.getParked(z);
					    }
		                else {
		                	z.addOneCrusingVehicle();
		                	// Select a neighboring link and cruise to there
		                	this.goCrusing(z);
		                	this.cruisingTime_ = 0;
		                }
					}
				}
			}
			else if(this.getState() == Vehicle.PICKUP_TRIP){
				this.setState(Vehicle.OCCUPIED_TRIP);
				super.leaveNetwork(); // Leave the network to pickup passengers
				this.setNextPlan();
				this.departure();
			}
			else if (this.getState() == Vehicle.CRUISING_TRIP) {
				if(this.cruisingTime_ <= GlobalVariables.MAX_CRUISING_TIME * 60 / GlobalVariables.SIMULATION_STEP_SIZE) {
					if(batteryLevel_ <= lowerBatteryRechargeLevel_ || (GlobalVariables.PROACTIVE_CHARGING
							&& batteryLevel_ <= higherBatteryRechargeLevel_ && z.hasEnoughTaxi(5))) {
						ContextCreator.getVehicleContext().getVehiclesByZone(z.getIntegerID()).remove(this);
						this.goCharging();
					}
					else {
						if(z.getCapacity() > 0) { // Has capacity
		                	z.removeOneCruisingVehicle();
		                	z.addOneParkingVehicle();
		                	this.cruisingTime_ = 0;
		    				this.getParked(z);
					    }
		                else {
		                	this.cruisingTime_ += RepastEssentials.GetTickCount() - this.getDepTime();
		                	// Keep cruising
		                	this.goCrusing(z);
		                }
					}
				}
				else {
					this.cruisingTime_ = 0;
					this.goParking();
				}
			}
			else if (this.getState() == Vehicle.RELOCATION_TRIP) {
				z.removeFutureSupply();
				if(batteryLevel_ <= lowerBatteryRechargeLevel_ || (GlobalVariables.PROACTIVE_CHARGING
						&& batteryLevel_ <= higherBatteryRechargeLevel_ && z.hasEnoughTaxi(5))) {
					this.goCharging();
				}
				else { // join the current zone
					ContextCreator.getVehicleContext().getVehiclesByZone(z.getIntegerID()).add(this);
					if(z.getCapacity() > 0) { // Has capacity
	                	z.addOneParkingVehicle();
	    				this.getParked(z);
				    }
	                else {
	                	z.addOneCrusingVehicle();
	                	this.cruisingTime_ = 0;
	                	this.goCrusing(z); // Select a neighboring link and cruise to there
	                	
	                }
				}
			}
			else {
				ContextCreator.logger.error("Vehicle does not belong to any of given states!");
			}
		}
	}

	public void setInitialParams() {
		this.numPeople_ = 0;
		this.cruisingTime_ = 0;
		this.batteryLevel_ = GlobalVariables.RECHARGE_LEVEL_LOW * GlobalVariables.EV_BATTERY
				+ GlobalVariables.RandomGenerator.nextDouble() * (1 - GlobalVariables.RECHARGE_LEVEL_LOW) * GlobalVariables.EV_BATTERY; // unit:kWh,
																											// times a
																											// large
																											// number to
																											// disable
																											// charging
		this.lowerBatteryRechargeLevel_ = GlobalVariables.RECHARGE_LEVEL_LOW * GlobalVariables.EV_BATTERY;
		this.higherBatteryRechargeLevel_ = GlobalVariables.RECHARGE_LEVEL_HIGH * GlobalVariables.EV_BATTERY;
		this.mass = 1521; // kg
		this.avgPersonMass_ = 60; // kg

		// Parameters for energy calculation
		this.tickConsume = 0.0; // kWh
		this.totalConsume = 0.0; // kWh
		// For Maia's model
//		double soc[] = {0.00, 0.10, 0.20, 0.40, 0.60, 0.80, 1.00};             
//		double r[] = {0.0419, 0.0288, 0.0221, 0.014, 0.0145, 0.0145, 0.0162}; 
//		PolynomialSplineFunction f = splineFit(soc,r);                         
//		this.splinef = f; 
		// Parameters for UCB calculation
		this.linkConsume = 0;
		this.passengerWithAdditionalActivityOnTaxi = new LinkedList<Request>();
	}

	public double getBatteryLevel() {
		return batteryLevel_;
	}

	public int getNumPeople() {
		return numPeople_;
	}

	public void setNumPeople(int numPeople) {
		numPeople_ = numPeople;
	}

	public double getMass() {
		return 1.05 * mass + numPeople_ * avgPersonMass_;
	}

	public boolean onChargingRoute() {
		return this.onChargingRoute_;
	}

	// Charge the battery.
	public void chargeItself(double batteryValue) {
		charging_time += GlobalVariables.SIMULATION_CHARGING_STATION_REFRESH_INTERVAL;
		batteryLevel_ += batteryValue;
	}

	// New EV energy consumption model
	// Fiori, C., Ahn, K., & Rakha, H. A. (2016). Power-based electric vehicle
	// energy consumption model: Model development and validation. Applied Energy,
	// 168, 257�268.
	// P = (ma + mgcos(\theta)\frac{C_r}{1000)(c_1v+c_2) + 1/2 \rho_0
	// AC_Dv^2+mgsin(\theta))v
	public double calculateEnergy() {
		double velocity = currentSpeed(); // obtain the speed
		double acceleration = currentAcc(); // obtain the acceleration
		if (!this.movingFlag) {
			velocity = 0;
			acceleration = 0;
		}
		double slope = 0.0f; // positive: uphill; negative: downhill, this is always 0, change this if the
								// slope data is available
		double dt = GlobalVariables.SIMULATION_STEP_SIZE; // this time interval, the length of one tick. 0.3
		double f1 = getMass() * acceleration;
		double f2 = getMass() * gravity * Math.cos(slope) * cr / 1000 * (c1 * velocity + c2);
		double f3 = 1 / 2 * p0 * A * cd * velocity * velocity;
		double f4 = getMass() * gravity * Math.sin(slope);
		double F = f1 + f2 + f3 + f4;
		double Pte = F * velocity;
		double Pbat;
		if (acceleration >= 0) {
			Pbat = (Pte + Pconst) / (etaM * etaG);
		} else {
			double nrb = 1 / Math.exp(0.0411 / Math.abs(acceleration));
			Pbat = (Pte + Pconst) * nrb;
		}
		double energyConsumption = Pbat * dt / (3600 * 1000); // wh to kw
		return energyConsumption;
	}

	// Old EV energy consumption model:
	/*
	 * R. Maia, M. Silva, R. Arajo, and U. Nunes, Electric vehicle simulator for
	 * energy consumption studies in electric mobility systems, in 2011 IEEE Forum
	 * on Integrated and Sustainable Transportation Systems, 2011, pp.232.
	 */
	// Xue, Juan 20191212: calculate the energy for each vehicle per tick. return:
	// kWh.
//	public double calculateEnergy(){		
//		double velocity = currentSpeed();   // obtain the speed
//		double acceleration = currentAcc(); // obtain the acceleration
//		if(!this.movingFlag){
//			return 0;
//		}
//		double slope = 0.0f;          //positive: uphill; negative: downhill
//		double dt = GlobalVariables.SIMULATION_STEP_SIZE;   // this time interval. the length of one tick. 0.3
//		double currentSOC = Math.max(getBatteryLevel()/(this.batteryCapacity + 0.001), 0.001);     // currentSOC
//		// System.out.println("vehicle energy :"+splinef.value(currentSOC)+" "+currentSOC + " " + (getBatteryLevel()));
//		// System.out.println("currentSOC: " + currentSOC + " Battery Level: "+getBatteryLevel());	
//		//step 1: use the model: Fte = Frr + Fad + Fhc + Fla + Fwa. And Fwa = 0.
//		//mass_ = mass_ * 1.05;
//		double Frr = ElectricVehicle.urr * mass_ * ElectricVehicle.gravity;
//		//System.out.println("VID: "+ this.vehicleID_ + " urr: "+ GlobalVariables.urr + "mass_: " + mass_ + " gravity: " + GlobalVariables.gravity);
//		double Fad = 0.5 * ElectricVehicle.densityAir * ElectricVehicle.A * ElectricVehicle.Cd * velocity * velocity;
//		double Fhc = mass_ * ElectricVehicle.gravity * Math.sin(slope); //can be positive, can be negative  // mass loss // m = 1.05
//		double Fla = mass_ * acceleration;       //can be positive, can be negative
//		double Fte = Frr + Fad + Fhc + Fla;     //can be positive, can be negative
//		double Pte = Math.abs(Fte) * velocity;  //positive unit: W
//		
//		//step 2: two cases
//		double Pbat = 0.0f;
//		if (Fte >= 0){      //driven case
//			Pbat = (Pte+ElectricVehicle.Pconst)/ElectricVehicle.etaM/ElectricVehicle.etaG;		   //positive	
//		}else{              //regenerative case
//			Pbat = (Pte+ElectricVehicle.Pconst)*ElectricVehicle.etaM*ElectricVehicle.etaG;         //positive
//		}	
//		double rIn = splinef.value(currentSOC)*ElectricVehicle.c;          //c	
//		double Pmax = ElectricVehicle.Voc*ElectricVehicle.Voc/4.0/rIn;               //rIn
//		
//		//step 3: energy calculation
//		double kt = ElectricVehicle.k/1.03*(1.027 - 0.001122*ElectricVehicle.T + 1.586*Math.pow(10, -5*ElectricVehicle.T*ElectricVehicle.T));  //kt depends on T also.
//		double cpt = ElectricVehicle.cp/2.482*(2.482 + 0.0373*ElectricVehicle.T - 1.65*Math.pow(10, -4*ElectricVehicle.T*ElectricVehicle.T));  //cpt depends on T also. //real capacity: not 77 AH
//		
//		double CR = 0.0f;
//		double I = 0.0f;
//		if (Pbat > Pmax){ 
////			System.out.println("Error process, output error, need to recalculate vi"+Pbat+"," + Pmax+","+Fte+","+velocity+","+acceleration);
//			Pbat = Pmax;
//		}
//		
//        if(Pbat >= 0){  //driven case
//			I = (ElectricVehicle.Voc - Math.sqrt(ElectricVehicle.Voc*ElectricVehicle.Voc -4 * rIn * Pbat + 1e-6))/(2*rIn); // Prevent negative value by adding a tiny error
//			CR = Math.pow(I, kt)*dt;     //unit: AS                                       // I_kt??
//			//System.out.println("VID: "+ this.vehicleID_ + " Pte: "+ Pte + "Frr: " + Frr + " Fad: " + Fad + "Fhc: " + Fhc + "Fla: " + Fla +"V:" + velocity + "a: "+acceleration);
//			//System.out.println("VID: "+ this.vehicleID_ + " Pte: "+ Pte+ " batter level" + this.batteryLevel_ + " within sqrt:"+ Double.toString(GlobalVariables.Voc*GlobalVariables.Voc -4 * rIn * Pbat)); //LZX: Negative power!
//		}else{              //regenerative case
//			I = (0.0f - ElectricVehicle.Voc + Math.sqrt(ElectricVehicle.Voc*ElectricVehicle.Voc + 4 * rIn * Pbat + 1e-6))/(2*rIn);    // Prevent negative value by adding a tiny error
//			CR = -I * dt;    //unit: AS  //?
//		}
//		
//		double capacityConsumption = CR/(cpt*3600);   //unit: AH 
//		double energyConsumption = capacityConsumption * this.batteryCapacity;//ElectricVehicle.storedEnergy;  //unit: kWh
//		//System.out.println("ev energy :"+splinef.value(currentSOC)+" "+currentSOC);
////		if(Double.isNaN(energyConsumption)){
//////			System.out.println("v: "+ velocity + " acc: "+acceleration + " currentSOC: " + currentSOC);
//////			System.out.println("Frr: "+ Frr + " Fad: "+Fad + " Fhc: " + Fhc + " Fla: " + Fla);
//////			System.out.println("Fte: "+ Fte + " Pte: "+Pte + " Pbat: " + Pbat);
//////			System.out.println("rIn:"+ rIn + " Pmax: "+Pmax + " kt: " + kt);
//////			System.out.println("cpt:"+ cpt + " CR: "+CR + " I: " + I);
////			return -0.01;
////		}
//		if (energyConsumption > 0.1) {
//			System.out.println("v: " + velocity + " acc: " + acceleration + " currentSOC: " + currentSOC);
//			System.out.println("Frr: " + Frr + " Fad: " + Fad + " Fhc: " + Fhc + " Fla: " + Fla);
//			System.out.println("Fte: " + Fte + " Pte: " + Pte + " Pbat: " + Pbat);
//			System.out.println("rIn:" + rIn + " Pmax: " + Pmax + " kt: " + kt);
//			System.out.println("cpt:" + cpt + " CR: " + CR + " I: " + I);
//		}
//		return energyConsumption;
//	}

	// spline interpolation
	public PolynomialSplineFunction splineFit(double[] x, double[] y) {
		SplineInterpolator splineInt = new SplineInterpolator();
		PolynomialSplineFunction polynomialSpl = splineInt.interpolate(x, y);
		return polynomialSpl;
	}

	public double getTickConsume() {
		return tickConsume;
	}

	public double getTotalConsume() {
		return totalConsume;
	}

	public void finishCharging(Integer chargerID, String chargerType) {
		String formated_msg = RepastEssentials.GetTickCount() + "," + chargerID + "," + this.getVehicleID() + ","
				+ this.getVehicleClass() + "," + chargerType + "," + this.charging_waiting_time + ","
				+ this.charging_time + "," + this.initial_charging_state + "\r\n";
		try {
			ContextCreator.charger_logger.write(formated_msg);
			this.charging_waiting_time = 0;
			this.charging_time = 0;
		} catch (IOException e) {
			e.printStackTrace();
		}

		this.onChargingRoute_ = false;
		this.setNextPlan(); // Return to where it was before goCharging
		ContextCreator.getCityContext().findZoneWithIntegerID(this.getDestID()).addFutureSupply();
		this.setState(Vehicle.RELOCATION_TRIP);
		this.departure(); 
	}

	public double getLinkConsume() {
		return linkConsume;
	}

	// Reset link consume once a ev has passed a link
	public void resetLinkConsume() {
		this.linkConsume = 0;
	}

	public void recLinkSnaphotForUCB() {
		DataCollector.getInstance().recordLinkSnapshot(this.getRoad().getLinkid(), this.getLinkConsume());
	}

	public void recSpeedVehicle() {
		DataCollector.getInstance().recordSpeedVehilce(this.getRoad().getLinkid(), this.currentSpeed());
	}

	public double getTripConsume() {
		return tripConsume;
	}

	public void setRouteChoice(int i) {
		this.routeChoice = i;
	}

	public int getRouteChoice() {
		return this.routeChoice;
	}
}
