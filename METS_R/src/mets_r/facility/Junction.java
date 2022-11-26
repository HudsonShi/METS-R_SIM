package mets_r.facility;

import java.util.ArrayList;
//import java.util.HashMap;
//import java.util.Map;

import com.vividsolutions.jts.geom.Coordinate;

import mets_r.ContextCreator;

public class Junction {
	private int ID;
	private int junctionID; // From shape file
	private Coordinate coord;
	protected int hasControl;
	private ArrayList<Road> roads = new ArrayList<Road>();

	public Junction(Coordinate coord, int id) {
		this.ID = ContextCreator.generateAgentID();
		this.coord = coord;
		this.junctionID = id;

	}

	@Override
	public String toString() {
		return "Junction " + this.ID + " at: " + this.coord.toString();
	}

	public int getID() {
		return ID;
	}

	public int getJunctionID() {
		return junctionID;
	}

	public void setID(int id) {
		this.ID = id;
	}

	public void setHasControl(int _cntrl) {
		this.hasControl = _cntrl;
	}

	public int getHasControl() {
		return this.hasControl;
	}

	public Coordinate getCoord() {
		return this.coord;
	}

	public boolean equals(Junction j) {
		if (this.coord.equals(j.getCoord()))
			return true;
		else
			return false;
	}

	public ArrayList<Road> getRoads() {
		return this.roads;
	}

	public void addRoad(Road road) {
		this.roads.add(road);
	}

}
