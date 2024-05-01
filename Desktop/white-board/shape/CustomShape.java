package shape;

import java.awt.Color;
import java.awt.Shape;
import java.io.Serializable;

public class CustomShape implements Serializable {
	
	private static final long serialVersionUID = -10615232093432343L;
	protected Shape shape;
	protected Color color;
	protected String author;
	protected int thickness;
	protected Boolean fill;
	
	public CustomShape(Shape shape, Color color, String author, int thickness, Boolean fill) {
		this.shape = shape;
		this.color = color;
		this.author = author;
		this.thickness = thickness;
		this.fill = fill;
	}
	
	public Shape getShape() {
		return shape;
	}
	
	public Color getColor() {
		return color;
	}

	
	public int getThickness() {
		return thickness;
	}
	
	public Boolean getFill() {
		return fill;
	}
}
