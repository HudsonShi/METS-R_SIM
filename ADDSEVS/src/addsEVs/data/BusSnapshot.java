package addsEVs.data;


import com.vividsolutions.jts.geom.Coordinate;

import addsEVs.vehiclecontext.Bus;


/**
 * 
 * This class is the simple data object for capturing the state 
 * of EV buses at a particular point in time.
 * 
 * This object is immutable and composed of simple data variables.
 * It should be trivial to serialize and reconstruct this object.
 * All values are available directly as public members and through
 * "get" methods.
 * 
 * Modified from Vehicle Snapshot from ARESCUE
 */
public class BusSnapshot {
    
    /** The number identifying this vehicle within the simulation. */
    final public int id;
    
    /** The number identifying this vehicle within the simulation. */
    final public int routeID;
    
    /** The X-axis (longitude) position of the vehicle in the previous epoch when snapshot was recorded for visualization interpolation. */
    final public double prev_x;
    
    /** The Y-axis (latitude) position of the vehicle in the previous epoch when snapshot was recorded for visualization interpolation. */
    final public double prev_y;
 
    /** The X-axis (longitude) position within the simulation. */
    final public double x;
    
    /** The Y position of the vehicle within the simulation. */
    final public double y;
   
    /** The current speed of the vehicle with the simulation. */
    final public float speed;
    
    /** The current acceleration of the vehicle with the simulation. */
    final public float acc;
    
    /** The current battery level of the bus. */
    final public double batteryLevel;
    
    /** The total energy consumption of the bus. */
    final public double totalConsumption;
    
    /** The road ID of the vehicle within the simulation. */
    final public int roadID;  
    
    final public int served_pass;
    
    /** The origin X-axis (longitude) position within the simulation. */      /** @author Jiawei Xue */
//    final public double originX;
//    
//    /** The origin Y position of the vehicle within the simulation. */
//    final public double originY;
//    
//    /** The destination X-axis (longitude) position within the simulation. */      
//    final public double destX;
//    
//    /** The destination Y position of the vehicle within the simulation. */
//    final public double destY;                                                             
// 
//    /** Vehicle is traveling on the last segment of its path, so close to destination. */
//    final public int nearlyArrived;
//    
//    /** Vehicle routing class. */
//    final public int vehicleClass;
//    
//    /** The road ID of the vehicle within the simulation. */
//    final public int roadID;  
    
        
    /** The Z position of the vehicle within the simulation. */
    /** final public double z;*/

    /** The start time of the vehicle's current trip in the simulation. */
    /**final public int departure;*/
    
    /** The end time of the vehicle's current trip in the simulation. */
    /** final public int arrival;*/
    
    /** The total distance traveled by the vehicle in the simulation. */
    /** final public float distance;*/
    
    /**
     * Construct the vehicle snapshot from the given vehicle and position.
     * 
     * @param vehicle the vehicle for which a snapshot is being made.
     * @param coordinate the vehicle's current position in the simulation.
     * @throws Throwable if the supplied vehicle object is not valid.
     */
    public BusSnapshot(Bus vehicle, 
                           Coordinate coordinate) throws Throwable {
        this(vehicle.getVehicleID(),vehicle.getRouteID(),
        	 vehicle.getpreviousEpochCoord().x,
           	 vehicle.getpreviousEpochCoord().y,	
           	 coordinate.x,
             coordinate.y,
           	 vehicle.currentSpeed(),
           	 vehicle.currentAcc(), vehicle.getBatteryLevel(),  vehicle.getTotalConsume(), vehicle.getRoad().getLinkid(), vehicle.served_pass);
    }
    
    
    /**
     * Construct the vehicle snapshot with the given ID and position.
     * 
     * @param id the identifier of the vehicle within the simulation.
     * @param x the x-axis position (longitude) within the simulation.
     * @param y the y-axis position (latitude) within the simulation.
     * @param z the z-axis position (altitude) within the simulation.
     * @param speed the current vehicle speed within the simulation.
     * @param departure the time the vehicle started moving for current trip.
     * @param arrival the expected time of arrival for vehicle's current trip.
     * @param distance the distance traveled so far for the vehicle's trip.
     * @throws Throwable if one of the supplied values is invalid.
     */
    public BusSnapshot(int id, 
    		           int route_id,
    					   double prev_x,
                           double prev_y,
                           double x,
                           double y,
                           float speed,
                           float acc,
                           double batteryLevel,
                           double energyConsumption,
                           int roadID,
                           int served_pass
                           //double z,
                           //int departure,
                           //int arrival,
                           //float distance,
                           ) throws Throwable {
        // all values are passed in as primitaves instead of objects,
        // so the compiler won't allow any to be null, no need to check
        
        // do basic validity checks against the values provided
//        if (roadID < 0) {
//            throw new Exception("Road ID cannot be negative.");
//        }
//        if (Double.isNaN(originX) || Double.isInfinite(originX)) {
//            throw new NumberFormatException("Original X-axis value is invalid.");
//        }
//        if (Double.isNaN(originY) || Double.isInfinite(originY)) {
//            throw new NumberFormatException("Original Y-axis value is invalid.");
//        }
//        if (Double.isNaN(destX) || Double.isInfinite(destX)) {
//            throw new NumberFormatException("Dest X-axis value is invalid.");
//        }
//        if (Double.isNaN(destY) || Double.isInfinite(destY)) {
//            throw new NumberFormatException("Dest Y-axis value is invalid.");
//        }
        if (id < 0) {
            throw new Exception("Vehicle ID cannot be negative.");
        }
        if (Double.isNaN(x) || Double.isInfinite(x)) {
            throw new NumberFormatException("X-axis value is invalid.");
        }
        if (Double.isNaN(y) || Double.isInfinite(y)) {
            throw new NumberFormatException("Y-axis value is invalid.");
        }
        // the model doesn't use the Z-axis
        //if (Double.isNaN(z) || Double.isInfinite(z)) {
        //    throw new NumberFormatException("Z-axis value is invalid.");
        //}
        if (Float.isNaN(speed) || Float.isInfinite(speed)) {
            throw new NumberFormatException("Speed value is invalid.");
        }
        
        if (Float.isNaN(acc) || Float.isInfinite(acc)) {
            throw new NumberFormatException("Acc value is invalid.");
        }
        //if (Float.isNaN(distance) || Float.isInfinite(distance)) {
            //throw new NumberFormatException("Distance value is invalid.");
        //}

        // store the values in the object
        this.id = id;
        this.routeID = route_id;
        this.prev_x = prev_x;
        this.prev_y = prev_y;
        this.x = x;
        this.y = y;
        this.acc = acc;
        this.speed = speed;
        this.batteryLevel = batteryLevel;
        this.totalConsumption = energyConsumption;
        this.served_pass = served_pass;
//        this.originX = originX;                     /** @author Jiawei Xue */
//        this.originY = originY;
//        this.destX = destX;
//        this.destY = destY;
//        this.nearlyArrived = nearlyArrived;
//        this.vehicleClass = vehicleClass;
        this.roadID = roadID;
        //this.z = 0.0d;
        //this.departure = departure;
        //this.arrival = arrival;
        //this.distance = distance;

    }
    
    
    /**
     * Returns the identity of the vehicle within the simulation.
     * 
     * @return the identity of the vehicle within the simulation.
     */
    public int getId() { return this.id; }
    
    /**
     * Returns the previous X-axis (when the last epoch for visualization interpolation happened) position within the simulation.
     * 
     * @return the previous X-axis position within the simulation.
     */
    public double getPrevX() { return this.prev_x; }
    
    
    /**
     * Returns the previous Y-axis (when the last epoch for visualization interpolation happened) position within the simulation.
     *  
     * @return the previous Y-axis position within the simulation.
     */
    public double getPrevY() { return this.prev_y; }
    
    /**
     * Returns the X-axis (longitude?) position within the simulation.
     * 
     * @return the X-axis (longitude?) position within the simulation.
     */
    public double getX() { return this.x; }
    
    /**
     * Returns the Y-axis (latitude?) position within the simulation.
     *  
     * @return the Y-axis (latitude?) position within the simulation.
     */
    public double getY() { return this.y; }
        
    /**
     * Returns the current speed of the vehicle within the simulation.
     * 
     * @return the current speed of the vehicle within the simulation.
     */
    public float getSpeed() { return this.speed; }
    
    public float getAcc() { return this.acc; }

	public String getRouteID() {
		// TODO Auto-generated method stub
		return Integer.toString(this.routeID);
	}   
	
	public double getTotalEnergyConsumption() { return this.totalConsumption;}
    
	public double getBatteryLevel(){ return this.batteryLevel;}
	
	public int getServedPass(){return this.served_pass; }
    /**
     * Returns the origin X-axis (longitude?) position within the simulation.
     * 
     * @return the origin X-axis (longitude?) position within the simulation.
     */    
    
    
    // public double getOriginX() { return this.originX; }

    /**
     * Returns the origin Y-axis position within the simulation.
     * 
     * @return the origin Y-axis position within the simulation.
     */
    // public double getOriginY() { return this.originY; }
    

    /**
     * Returns the destination X-axis (longitude?) position within the simulation.
     * 
     * @return the destination X-axis (longitude?) position within the simulation.
     */
    // public double getDestX() { return this.destX; }
     
    
    /**
     * Returns the destination Y-axis  position within the simulation.
     * 
     * @return the destination Y-axis  position within the simulation.
     */
    // public double getDestY() { return this.destY; }
    
    /**
     * Returns the whether the vehicle is near the destination.
     *   
     * @return Whether the vehicle is near the destination.
     */
    // public int getNearlyArrived() { return this.nearlyArrived; }
    
    /**
     * Returns the routing class of the vehicle.
     * 
     * @return the routing class of the vehicle.
     */
    // public int getvehicleClass() { return this.vehicleClass; }    
    
    /**
     * Returns the road ID of the vehicle within the simulation.
     * 
     * @return the road ID of the vehicle within the simulation.
     */
     public int getRoadID() { return this.roadID; }    
     
   
    /**
     * Returns the time of departure of the vehicle for the current trip.
     * 
     * @return the time of departure of the vehicle for the current trip.
     */
    //public int getDeparture() { return this.departure; }
    
    
    /**
     * Returns the expected arrival time of the vehicle for the current trip.
     * 
     * @return the expected arrival time of the vehicle for the current trip.
     */
    //public int getArrival() { return this.arrival; }
    
    
    /**
     * Returns the total distance traveled by the vehicle so far on this trip.
     * 
     * @return the total distance traveled by the vehicle so far on this trip.
     */
    //public float getDistance() { return this.distance; }

}