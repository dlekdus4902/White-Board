package client;

import shape.CustomShape;

import java.io.Serializable;
import java.util.ArrayList;

public class State implements Serializable {
	
	private static final long serialVersionUID = -222327231242375916L;
	private ArrayList<CustomShape> customShapes;


	public State(ArrayList<CustomShape> customShapes) {
		this.customShapes = customShapes;
	}
	
	public ArrayList<CustomShape> getShapes() {
		return customShapes;
	}


}
