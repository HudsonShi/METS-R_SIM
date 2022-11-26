package mets_r.mobility;

import java.util.LinkedList;
import java.util.Queue;

import com.vividsolutions.jts.geom.Coordinate;

import mets_r.GlobalVariables;

/**
 * Trip request
 * 
 * @author: Zengxiang Lei
 **/

public class Request {
	private Integer origin;
	private Integer destination;
	private Coordinate originCoord;
	private Coordinate destCoord;
	private Integer maxWaitingTime;
	private Integer currentWaitingTime;
	private Boolean willingToShare;
	private Queue<Plan> activityPlan;
	
	public Request(Integer origin, Integer destination, Coordinate originCoord, Coordinate destCoord, Boolean willingToShare){
		this.activityPlan = new LinkedList<Plan>();
		this.origin = origin;
		this.destination = destination;
		this.originCoord = originCoord;
		this.destCoord = destCoord;
		this.maxWaitingTime = GlobalVariables.SIMULATION_STOP_TIME; // The passenger will not leave by default
		this.currentWaitingTime = 0;	
		this.willingToShare = willingToShare;
	}
	
	public Request(Integer origin,  Coordinate originCoord, Queue<Plan> activityPlan){
		this.activityPlan = activityPlan;
		this.maxWaitingTime = GlobalVariables.SIMULATION_STOP_TIME;;
		this.currentWaitingTime = 0;	
		this.willingToShare = false;
		this.origin = origin;
		this.originCoord = originCoord;
		this.destination = activityPlan.peek().getDestID();
	}
	
	public void waitNextTime(Integer waitingTime){
		this.currentWaitingTime+=waitingTime;
	}
	
	// Condition for passenger to left the service
	public boolean check(){
		if(this.currentWaitingTime > this.maxWaitingTime){
			return true;
		}
		else{
			return false;
		}
	}
	
	public int getCurrentWaitingTime(){
		return this.currentWaitingTime;
	}
	
	public void setCurrentWaitingTime(int waiting_time) {
		this.currentWaitingTime = waiting_time;
	}
	
	public int getMaxWaitingTime() {
		return this.maxWaitingTime;
	}
	
	public void setMaxWaitingTime(int waiting_time) {
		this.maxWaitingTime = waiting_time;
	}
	
	public void setOrigin(Integer origin){
		this.origin = origin;
	}
	
	public void setDestination(Integer destination){
		this.destination = destination;
	}
	
	public int getOrigin(){
		return this.origin;
	}
	
	public int getDestination(){
		return this.destination;
	}
	
	public Coordinate getOriginCoord(){
		return this.originCoord;
	}
	
	public Coordinate getDestCoord(){
		return this.destCoord;
	}
	
	public Queue<Plan> getActivityPlan(){
		return this.activityPlan;
	}
	
	public void moveToNextActivity() {
		this.currentWaitingTime = 0; // reset the waiting time
		Plan prevPlan = this.activityPlan.poll();
		this.origin = prevPlan.getDestID();
		this.originCoord = prevPlan.getLocation();
		Plan currPlan = this.activityPlan.peek();
		this.destination = currPlan.getDestID();
		this.destCoord = currPlan.getLocation();
	}
	
	public int lenOfActivity(){
		if(this.activityPlan!=null){
			return this.activityPlan.size();
		}
		else {
			return 0;
		}
	}
	
	public boolean isShareable(){
		return this.willingToShare;
	}
	
	public void clearActivityPlan() {
		this.activityPlan.clear();
	}
}
