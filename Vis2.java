import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import javax.swing.*;
import javax.imageio.*;
import java.awt.image.*;
import javax.swing.filechooser.*;
import java.util.*;
import java.io.*;

public class Vis2 extends JApplet implements ActionListener{
	public static String version="5 Feb 2009";
	final static Color bg = Color.white;
	final static Color fg = Color.black;
	Dimension totalSize;
	public BufferedImage img; 
	public int size=500;
	int numParticles=0;
	int[][] OriginalMatrix;
	int mag=2; // Magnification factor 
	public CA2 ca;
	int counter=0; // Timesteps counter
	int defaultView=0; // Cells view
	JButton button;
	int timestep=0;
	boolean movie=true;
	public boolean play=true;
	JFileChooser fc;
	JLabel label;
	JCheckBox cb;
	String rootDir=".";


	public Vis2 (JButton b, JLabel l, JCheckBox _cb, float SCSymmetricDivBalance, int maxDivisions, int maxAge, float densityV)
	{
		fc = new JFileChooser();
		button = b;
		label = l;
		cb=_cb;
		ca = new CA2(SCSymmetricDivBalance, maxDivisions, maxAge,densityV);
		size=ca.size;
		mag = ca.mag;
		img = new BufferedImage (2*ca.size*ca.mag,1*ca.size*ca.mag,BufferedImage.TYPE_INT_ARGB);
	}
	
	public final BufferedImage scale(double scale, BufferedImage srcImg) 
	{ 
		if (scale == 1)  return srcImg; 
   		AffineTransformOp op = new AffineTransformOp( AffineTransform.getScaleInstance( scale, scale), null); 
        	return op.filter(srcImg, null); 
	}

	public void paint(Graphics g) {
        	Graphics2D g2 = (Graphics2D) g;
			BufferedImage img2=scale (mag,img);
			g2.drawImage(img2,null,null);
			if (ca.size!=size) {
				img = new BufferedImage (ca.size*ca.mag,ca.size*ca.mag,BufferedImage.TYPE_INT_ARGB);
				ca.size=size;
			}
	}

	public void nextTimeStep ()
	{
		ca.nextTimeStep();
		paintAll();
		timestep++;
	}
	

	public void paintAll()
	{
		int [][] lattice = ca.getCells();
		
		BufferedImage all = new BufferedImage (ca.size*2,ca.size*1,BufferedImage.TYPE_INT_ARGB);
		BufferedImage c= getCells();
		BufferedImage o=getConcentration(ca.getOxygen());
		for (int i=0;i<ca.size;i++)
			for (int j=0;j<size;j++) {
				all.setRGB(i,j,c.getRGB(i,j));
				all.setRGB(i+size,j,o.getRGB(i,j));
				img.setRGB(i,j,c.getRGB(i,j));
				img.setRGB(i+size,j,o.getRGB(i,j));
			}

		repaint();
		if (movie) try {
			File dir = new File (rootDir+"/images");
			dir.mkdir ();
			File fileAll = new File (rootDir+"/images/all"+counter+".png");
			ImageIO.write(img,"png",fileAll);
		} catch (Exception e) {
			e.printStackTrace();
		}	

		counter++;
	}

	public BufferedImage getCells()
	{
		int [][] lattice = ca.getCells();
		int [][] vasculature = ca.getVasculature();
		BufferedImage result = new BufferedImage (ca.size,ca.size,BufferedImage.TYPE_INT_ARGB);
		
		for (int i=0;i<ca.size;i++)
			for (int j=0;j<ca.size;j++) {
				int val=0;
				if (lattice[i][j]==0) val=Color.white.getRGB();
				else if (lattice[i][j]==1) val=Color.red.getRGB(); // Stem cells
				else if (lattice[i][j]==2) val=Color.green.getRGB(); // Progenitor cells
				else if (lattice[i][j]==3) val=Color.blue.getRGB(); // Differentiated (LU ab)
				if (vasculature[i][j]==1) val=Color.black.getRGB(); // Vasculature
				result.setRGB(i,j,val);
			}
		return result;
	}
	
	public BufferedImage getConcentration (float[][] lattice)
	{
		BufferedImage result = new BufferedImage (ca.size,ca.size,BufferedImage.TYPE_INT_ARGB);
		int k=0;
		for (int i=0;i<ca.size;i++)
			for (int j=0;j<ca.size;j++) {
				Color c1, c;
				c = new Color (255,255,255);
				if ((lattice[i][j]>=0) && (lattice[i][j]<=0.01)) {
					c = new Color((int)(lattice[i][j]*255*100), 0,  0);
				} else if ((lattice[i][j]>0.01) && (lattice[i][j]<=0.1)) {
					c = new Color(255, (int)(lattice[i][j]*255*10),  0);
				} else if ((lattice[i][j]>0.1) && (lattice[i][j]<=1)) c = new Color(255, 255,(int)(lattice[i][j]*255));
				else System.err.println ("not possible: "+lattice[i][j]);
				result.setRGB(i,j,c.getRGB());
			}
		return result;
	}
	
	
	public void actionPerformed (ActionEvent e) {
		String res = e.getActionCommand();
		if ("play".equals(e.getActionCommand())) {
			if (play) {
				play=false;
				button.setText ("Play");
			}
			else {
				play=true;
				button.setText ("Pause");
			}
		}
		else if (res.compareTo("Movie")==0) {
			if (movie==false) movie=true;
			else movie=false;
		}
		else if (res.compareTo("therapyButton")==0) {
			System.out.println ("I hear you!");
        	} else { 
		}
		repaint();
	}
	public void mouseExited (MouseEvent e) {}	
	public void mouseEntered (MouseEvent e) {}
	public void mouseReleased (MouseEvent e) {}
	public void mousePressed (MouseEvent e) {}


	public static void main(String args[]) {
		int mag=1;
		int maxTS=1000;
		boolean movie;
	
		System.err.println ("# Vis2 version:"+Vis2.version);
		System.err.println ("# CA version:"+CA2.version);
		
		float SCSymmetricDivBalance=0.2f;
		int maxDivisions=10;
		int maxAge=10;
		float densityV=0.04f;
		if (args.length==5) {
			SCSymmetricDivBalance = Float.parseFloat (args[0]);
			maxDivisions = Integer.parseInt (args[1]);
			maxAge = Integer.parseInt (args[2]);
			maxTS=Integer.parseInt (args[3]);
			densityV=Float.parseFloat(args[4]);
			System.err.println ("Balance: "+SCSymmetricDivBalance+" maxDiv: "+maxDivisions+" maxAge: "+maxAge+ " maxTS: "+ maxTS);
		} else {
			System.err.println ("Arguments needed: s/a maxDivisions maxAge timesteps, densityV");
			System.exit(-1);
		}
		// Let's deal now with the main window
		JFrame f = new JFrame("CSCTherapy CA Visualisation "+CA2.version);
	        f.addWindowListener(new WindowAdapter() {
	        	public void windowClosing(WindowEvent e) {System.exit(0);}
	        });
		JLabel label = new JLabel ("Timesteps");
		
		
		//JButton therapyButton = new JButton("Therapy");
		JButton play = new JButton ("Pause");
		JCheckBox cb = new JCheckBox ("Movie",false);
		Vis2 m = new Vis2 (play,label,cb,SCSymmetricDivBalance,maxDivisions,maxAge,densityV);
		//therapyButton.addActionListener(m);
		play.setActionCommand("play");
		//therapyButton.setActionCommand ("therapyButton");
		play.addActionListener (m);
		cb.addActionListener(m);
		JPanel buttonPanel = new JPanel(); //use FlowLayout
		buttonPanel.add(cb);
		//buttonPanel.add(therapyButton);
		buttonPanel.add(play);
	
		f.getContentPane().add("Center", m);
		f.getContentPane().add("North",label);
		f.getContentPane().add("South",buttonPanel);
		
		
		f.pack();
		f.setSize(new Dimension(2*m.ca.mag*m.size,1*m.ca.mag*m.size+80));
		f.show();
		
		boolean finished=false;
		int ts=0;
		//for (int i=0;i<maxTS;i++) {
		while (ts!=maxTS) {
			if (m.play) {
				m.nextTimeStep();
				label.setText ("Timestep: "+ts);
				ts++;
			}
		}
		System.exit(-1);
	}

}
