import java.io.*;
import java.util.*;
import static java.lang.Math.*;

public class CA2 {
	public static String version="11th Sept 2013";
	MersenneTwisterFast random;
	public int mag=1;
	public float [][] Oxygen=null; 
	public int [][] Age;
	int [][] Cells=null;  // Cells  and the cell type
						 // 00 -> No Cell
						 // 1 -> Stem Cells
						 // 2 -> Progenitor Cells
						 // 3 -> Mature/Differentiated cells
	int [][] Vasculature=null; // Initial vasculature as a boolean-like lattice
	Bag cellList=null; // Used in the iterateCells method. Defined here for performance issues
	boolean finishedRun=false;
	
	int size = 500; // Size of the system lattice
	int timestep=0; // Current Number of timesteps in the simulation

	// Model Parameters
	float initOxygen=0.13f;
	float[] proliferation = { 
	 0.0f,	 /* no cells */ 
	 0.5f, /* stem cells*/
	 0.5f, /* progenitor cells */
	 0.0f, /*mature/differentiated cells */
	};
	
	//HERE
	float consumptionBasal=   0.001f;
	float consumptionDivision=0.005f; // The Oxygen consumption for cells that proliferate
    float hypoxia=            0.065f; // Threshold which is initOxygen/2
	//HERE
	
	int maxProDivisions;
	int maxMatureCellAge;
	float asymmetricRatio;
	float pMotility=0.00f; // COMPLETELY ARBITRARY INDEED
	//float densityVasculature=0.0001f;
	float densityVasculature=0.004f; // 1/250
	float avgVesselRad=2;
	
	// MEaSURement and STATISTICS
	int births=0;
	int deaths=0;

	// Probability of stem cells producing progenitor cells given by oxygen concentration.
	// Probability of progenitor cells of producing differentiated cells: same

	public CA2 (float SCSymmetricDivBalance, int maxDivisions, int maxAge, float densityV)
	{
		int time = (int) System.currentTimeMillis();
		random = new MersenneTwisterFast (time);
		asymmetricRatio=SCSymmetricDivBalance;
		maxProDivisions=maxDivisions;
		maxMatureCellAge=maxAge;
		densityVasculature=densityV;
		reset();
		resetVasculature();
	}
	
	public void reset ()
	{
		Oxygen = new float [size][size];
		Age = new int [size][size];
		for (int i=0;i<size;i++)
			for (int j=0;j<size;j++) {
				Oxygen[i][j]=initOxygen;
				Age[i][j]=0;
			}

		resetCells();
	}

	void resetVasculature ()
	{
		Vasculature = new int [size][size];	
		
		for (int i=0;i<size;i++)
			for (int j=0;j<size;j++)
				if ((random.nextFloat()<=densityVasculature) && (Vasculature[i][j]==0)) Vasculature[i][j]=1;
				// This should be a radius
	}
	void readVasculature (File file)
	{
		try {
			BufferedReader bufRdr  = new BufferedReader(new FileReader(file));
			String line = null;
			int row = 0;
			int col = 0;
 
			//read each line of text file
			while((line = bufRdr.readLine()) != null) {
				StringTokenizer st = new StringTokenizer(line,",");
				while (st.hasMoreTokens()) {
					//get next token and store it in the array
					int number = Integer.parseInt(st.nextToken());
					if (col<size) Vasculature[row][col] = number;
					else Vasculature[(int)(col/size)][col%size]=number;
					col++;
				}
				row++;
			}
			bufRdr.close(); 
		} catch (Exception e) {
			System.out.println ("Configuration file not found");
			e.printStackTrace();
		}
	}
	
	final int distance (int x, int y, int i, int j)
	{
		double dis=Math.sqrt((x-i)*(x-i)+(y-j)*(y-j));

		return (int)Math.round(dis);
	}

	final int[] convertCoordinates(int x, int y)
       {
               // This method converts the coordinates so they take on account
               // the boundaries of the lattice

               if (x < 0) x = size - 1;
               else if (x > size - 1) x = 0;
               if (y < 0) y = size - 1;
               else if (y > size - 1) y = 0;
               int[] result = new int[2];
               result[0] = x; result[1] = y;
               return result;
       }

	
	public void nextTimeStep ()
	{
		births=0;
		deaths=0;
		for (int i=0;i<100;i++) iterateOxygen();
		iterateCells();
		
		//NEW
		int totalCells=0;
		int totalStem=0;
		int totalProgenitor=0;
		int totalMature=0;
		for (int i=0;i<size;i++)
			for (int j=0;j<size;j++)
				if (Cells[i][j]>0) {
					totalCells++;
					if (Cells[i][j]==1) totalStem++;
					else if (Cells[i][j]==2) totalProgenitor++;
					else if (Cells[i][j]==3) totalMature++;
					else System.err.println ("wrong cell type");
				}
			
		// Not so new	
		if (timestep==0) System.out.println ("% Timestep\t Cells\t Stem Cells \t Progenitor\t Mature");
		System.out.println(timestep+"\t"+totalCells+"\t"+totalStem+"\t"+totalProgenitor+"\t"+totalMature+"\t"+births+"\t"+deaths+"\t"+((float)births/deaths));
        System.err.println(asymmetricRatio+" "+maxProDivisions+" "+totalCells+" "+totalStem);
		timestep++;
        /*if (timestep==30000) {
            for (int i=0;i<size;i++)
                for (int j=0;j<size;j++) {
                            if ((random.nextInt(5)<4) && (Vasculature[i][j]==1)) Vasculature[i][j]=0;
                }
        }*/
        
        // Now let's write down the data
        try {
            File dir = new File ("./text");
			dir.mkdir ();
            FileWriter outFile1 = new FileWriter("./text/cells"+timestep);
            PrintWriter outCells = new PrintWriter(outFile1);
            FileWriter outFile2 = new FileWriter("./text/oxygen"+timestep);
            PrintWriter outO2 = new PrintWriter(outFile2);
            for (int i=0;i<size;i++) {
                for (int j=0;j<size;j++){
                    outCells.print(Cells[i][j]+", ");
                    outO2.print(Oxygen[i][j]+", ");
                }
                outCells.println("");
                outO2.println("");
            }
            outCells.close();
            outO2.close();
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(-1);
        }
	}

	
	void resetCells ()
	{
		int centre = size/2;
		int radius=50;
		Cells = new int [size][size];
		
		// Let's set the empty space first.
		for (int i=0;i<size;i++)
			for (int j=0;j<size;j++) {
				Cells[i][j]=0;
			}

		Cells[centre][centre]=1;
	}	

	public boolean iterateCells()
 	{
		if (cellList==null) cellList = new Bag (size*size); 
		for (int i = 0; i < size; i++)
			for (int j = 0; j < size; j++) {
				if (Cells[i][j] > 0)  { // All cell types have Cell > 0 
					int[] p = new int[2];
					p[0] = i; p[1] = j;
					cellList.add(p);
				}
			}

		while (cellList.size() != 0) {
			// Select the next lattice element at random
			int randomElemIndex=0;
			if (cellList.size()>1) randomElemIndex = random.nextInt(cellList.size()-1);
	   		int[] point = (int[])cellList.get(randomElemIndex); 
		   	int rI = point[0];
		   	int rJ = point[1];

			cellList.remove(randomElemIndex); // Remove it from the cell list
		   	int cell = Cells[rI][rJ];
	   
			// Cell death
			if ((Oxygen[rI][rJ]<0.01f)) {
				if (random.nextInt(5)==0) {
					Age[rI][rJ]=0;
					Cells[rI][rJ]=0;
					deaths++;
				}
			} else if ((cell==3) && (Age[rI][rJ]>maxMatureCellAge)) {
				Age[rI][rJ]=0;
				Cells[rI][rJ]=0;
				deaths++;

			}
			// Do we have space for division or motility ?
			else if (vacantSites(rI,rJ)>0)
				if (proliferation[cell]>=random.nextFloat()) {// If tossing the coin we are to proliferate...
					if ((cell==1) || ((cell==2) && (Age[rI][rJ]<maxProDivisions))) // AND the cell is stem or progenitor ...
						if (Oxygen[rI][rJ]>consumptionDivision) { // AND the oxygen concentration is enough for division...
							Oxygen[rI][rJ]=Oxygen[rI][rJ]-consumptionDivision;
							// Producing daughter cell in an empty neigbhbouring random site
							int[] daughter = findEmptySite (rI,rJ);
							births++; 
							if (cell==1) { // stem cell
                                    if (asymmetricRatio>random.nextFloat()) Cells[daughter[0]][daughter[1]]=1;
                                    else Cells[daughter[0]][daughter[1]]=2; // Otherwise differentiate
                               
							} else if (cell==2) {
								if (Age[rI][rJ]<maxProDivisions-1) {
									Cells[daughter[0]][daughter[1]]=2;
									Age[rI][rJ]++;
									Age[daughter[0]][daughter[1]]=Age[rI][rJ];
								} else {
									Cells[daughter[0]][daughter[1]]=3;
									Cells[rI][rJ]=3;
									Age[rI][rJ]=0;
									Age[daughter[0]][daughter[1]]=Age[rI][rJ];
								}
							}
						} 
				} else if (pMotility>random.nextFloat()) { // Maybe we can try migration?
						int[] daughter = findEmptySite (rI,rJ);
						Cells[daughter[0]][daughter[1]]=cell;
						Cells[rI][rJ]=0;
						Age[daughter[0]][daughter[1]]=Age[rI][rJ];
						Age[rI][rJ]=0;
						System.err.println ("moving "+rI+", "+rJ);
				}
			// Aging for mature cells
			if (cell==3) Age[rI][rJ]++;
	  	}
 	return true;
 }
	
final int vacantSites (int x, int y)
{
	// We assume that necrotic material counts as neighbour?
	int total=0;
	int[] p = new int [2];
		
	p=convertCoordinates (x+1,y-1);
	if (Cells[p[0]][p[1]]==0) total++;
	p=convertCoordinates (x+1,y);
	if (Cells[p[0]][p[1]]==0) total++;
	p=convertCoordinates (x+1,y+1);
	if (Cells[p[0]][p[1]]==0) total++;
	p=convertCoordinates (x,y-1);
	if (Cells[p[0]][p[1]]==0) total++;
	p=convertCoordinates (x,y+1);
	if (Cells[p[0]][p[1]]==0) total++;
	p=convertCoordinates (x-1,y-1);
	if (Cells[p[0]][p[1]]==0) total++;
	p=convertCoordinates (x-1,y);
	if (Cells[p[0]][p[1]]==0) total++;
	p=convertCoordinates (x-1,y+1);
	if (Cells[p[0]][p[1]]==0) total++;
	return total;
}
	

int[] findEmptySite (int x, int y)
{
	LinkedList vacantSites = new LinkedList();
	int[] tp1 = new int[2];
	int[] tp2 = new int[2];
	int[] tp3 = new int[2];
	int[] tp4 = new int[2];
	int[] tp5 = new int[2];
	int[] tp6 = new int[2];
	int[] tp7 = new int[2];
	int[] tp8 = new int[2];
	
	tp1=convertCoordinates (x+1,y-1);
	if (Cells[tp1[0]][tp1[1]]==0) vacantSites.add(tp1);
	tp2=convertCoordinates (x+1,y);
	if (Cells[tp2[0]][tp2[1]]==0) vacantSites.add(tp2);
	tp3=convertCoordinates (x+1,y+1);
	if (Cells[tp3[0]][tp3[1]]==0) vacantSites.add(tp3);
	tp4=convertCoordinates (x,y-1);
	if (Cells[tp4[0]][tp4[1]]==0) vacantSites.add(tp4);
	tp5=convertCoordinates (x,y+1);
	if (Cells[tp5[0]][tp5[1]]==0) vacantSites.add(tp5);
	tp6=convertCoordinates (x-1,y-1);
	if (Cells[tp6[0]][tp6[1]]==0) vacantSites.add(tp6);
	tp7=convertCoordinates (x-1,y);
	if (Cells[tp7[0]][tp7[1]]==0) vacantSites.add(tp7);
	tp8=convertCoordinates (x-1,y+1);
	if (Cells[tp8[0]][tp8[1]]==0) vacantSites.add(tp8);
	
	// Now let's see where.
	if (vacantSites.size() > 0) { // Now choose a vacant one, otherwise return the original location
		// pick a vacant site and return it
		int vacantElemIndex = random.nextInt(vacantSites.size());
		int[] p = (int[])vacantSites.get(vacantElemIndex);
		return (int[])p;	
	} else {
		int[] p = new int[2];
		p[0] = x; p[1] = y; // Just return the original
		System.out.println ("wrong!:"+vacantSites (x,y)+" - "+vacantSites.size());
		return p;
	}

}


public void iterateOxygen()
{
	//
	//float kDe = 0.03f;
	float kDe = 0.001728f;
	float[][] newOxygen = new float[size][size];
	for (int rI = 0; rI < size; rI++)
		for (int rJ = 0; rJ < size; rJ++) {
			// Determine the actual coordinates for top (-1,0), left(0,-1), right(0,1), below(1,0)
			// using periodic boundary conditions
			int[] top = convertCoordinates(rI - 1, rJ);
			int[] left = convertCoordinates(rI, rJ - 1);
			int[] right = convertCoordinates(rI, rJ + 1);
			int[] below = convertCoordinates(rI + 1, rJ);
			// Diffusion
			newOxygen[rI][rJ]
				= Oxygen[rI][rJ] + (kDe *
				(Oxygen[top[0]][top[1]]
				+ Oxygen[left[0]][left[1]]
				+ Oxygen[right[0]][right[1]]
				+ Oxygen[below[0]][below[1]]
				- 4.0f * Oxygen[rI][rJ]));

			// Consumption	
			if (Cells[rI][rJ]>0) {
					newOxygen[rI][rJ]=newOxygen[rI][rJ]-consumptionBasal/100;
			}
			
			// Production
			if ((Vasculature[rI][rJ]==1) && (Cells[rI][rJ]==0)) {
				newOxygen[rI][rJ]=1.0f;
			} 
			// Sanity check
			if (newOxygen[rI][rJ]>1.0f) newOxygen[rI][rJ]=1.0f;
			else if (newOxygen[rI][rJ]<0.0f) newOxygen[rI][rJ]=0.0f; 
		}
	Oxygen = newOxygen;	
}


	public float [][] getOxygen() { return Oxygen; }
	public int [][] getCells () { return Cells; }
	public int [][] getVasculature() {return Vasculature;}
};
