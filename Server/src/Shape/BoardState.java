package Shape;

import java.io.Serializable;
import java.util.ArrayList;

public class BoardState implements Serializable {
	
	private static final long serialVersionUID = -6319277311434675916L;
	private ArrayList<MyShape> shapes;

	public BoardState(ArrayList<MyShape> shapes) {
		this.shapes = shapes;
	}
	
	public ArrayList<MyShape> getShapes() {
		return shapes;
	}
	
	public synchronized void addShapes(MyShape shape) {
		shapes.add(shape);
	}

}
