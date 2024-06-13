package Utils;

import java.awt.Image;
import java.awt.Insets;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;

public class ImageResizer {
	
	
	public static ImageIcon reSizeForLabel(ImageIcon icon, JLabel label) {
		label.setOpaque(false);
		Image img = icon.getImage();  
	    Image resizedImage = img.getScaledInstance(label.getWidth(), label.getHeight(),  java.awt.Image.SCALE_SMOOTH);  
	    return new ImageIcon(resizedImage);
	}

}
