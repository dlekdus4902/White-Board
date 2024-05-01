package shape;

import java.awt.Color;
import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

public class Rectangle extends CustomShape implements Serializable {

    private static final long serialVersionUID = 816998423238837305L;

    public Rectangle(java.awt.Shape shape, Color color, String author, int thickness, Boolean fill) {
        super(shape, color, author, thickness, fill);
    }

    private void writeObject(ObjectOutputStream oos) throws IOException {
        oos.defaultWriteObject();
        oos.writeUTF(Integer.toString(color.getRGB()));
        oos.writeUTF(author);
        oos.writeInt(thickness);
        oos.writeBoolean(fill);
        Rectangle2D.Double rectangle = (Rectangle2D.Double) shape;
        oos.writeDouble(rectangle.getX());
        oos.writeDouble(rectangle.getY());
        oos.writeDouble(rectangle.getWidth());
        oos.writeDouble(rectangle.getHeight());
    }

    private void readObject(ObjectInputStream ois) throws IOException, ClassNotFoundException {
        ois.defaultReadObject();
        color = new Color(Integer.parseInt(ois.readUTF()));
        author = ois.readUTF();
        thickness = ois.readInt();
        fill = ois.readBoolean();
        shape = new Rectangle2D.Double(ois.readDouble(), ois.readDouble(), ois.readDouble(), ois.readDouble());
    }

}
