package gridGenerator;

/**
 * Class containing helper methods for setting up the visualized meshes at different stages throughout the generation process.
 * 
 */

public class MeshHelper {
    
    private int length, height;
    private double deltaX, deltaY;
    
    public MeshHelper(int length, int height, double deltaX, double deltaY) {
        this.length = length;
        this.height = height;
        this.deltaX = deltaX;
        this.deltaY = deltaY;
    }
    
    // Set the boundaries for the grid
    public void setBoundaries(double startX, double startY, double endX, double endY, 
                    int boundaryType, double amplitude,double centerX, double stepSize, double radius, 
                    double[][] x_old, double[][] y_old) {

        
    	/*
         * Set grid boundary nodes in following order: south, west, north, east
         */
        double x = startX;
        double y = startY;

        for (int i = 0; i < length; i++) {
            y = startY;
            
           // Crazy function

                                    
                
            //Crazy math function
                	// y = amplitude*Math.exp(-((10*x/60)-3.5)*((10*x/60)-3.5))+amplitude*Math.exp(-((6*x/60)-7.0)*((6*x/60)-7.0));
            if (x <= (centerX + radius) && x >= (centerX - radius))
               y = startY + Math.sqrt(radius*radius - (x - centerX)*(x - centerX));
            else 
               y = startY;
          
            
            x_old[0][i] = x;
            y_old[0][i] = y;
            x += deltaX;
        }

        // West, north and east boundaries defined as solid edges
        x = startX;
        y = startY;

        for (int j = 0; j < height; j++) {
            x_old[j][0] = x; //10*Math.exp(-((6*y/60)-7.0)*((6*y/60)-7.0)); // <--crazy function; 
            y_old[j][0] = y;
            y += deltaY;
        }

        x = startX;
        y = endY;

        for (int i = 0; i < length; i++) {
        	if(x < ((startX + endX)/2)) {
        		y = (startY) + ((endY  - startY))*(Math.sqrt(1 -((x/(endX - startX))*(x/(endX - startX)))));

        		x_old[height - 1][i] = x;
        		y_old[height - 1][i] = y; /*-5*Math.exp(-((20*x/60)-3.5)*((20*x/60)-3.5))-
                            20*Math.exp(-((10*x/60)-7.0)*((10*x/60)-7.0))-
                            15*Math.exp(-((26*x/100)-20.0)*((26*x/100)-20.0))+100;*///<-- crazy function; 
        		x += (endX - startX)/(2*length);
        	}
/*        	else {
        		x_old[height- 1][i] = centerX;
        		y_old[height -1][i] = (startY) + ((endY  - startY))*(Math.sqrt(1 -((x/(endX - startX))*(x/(endX - startX)))));
        	}
*/
        }

        x = endX;
        y = startY;
        
	    for (int j = 0; j < height; j++) {
	        if (x >=  ((startX + endX)/2)) {
	        	x = (startX) + ((endX  - startX))*(Math.sqrt(1 -((y/(endY - startY))*(y/(endY - startY)))));
	        	x_old[j][length -1] = x; //10*Math.exp(-((6*y/60)-7.0)*((6*y/60)-7.0)); // <--crazy function; 
	        	y_old[j][length -1] = y;
	        	y += (((endY  - startY))*(Math.sqrt(0.75)))/height;
	        }
/*	        	else {
	        		x_old[j][length -1] = centerX;
	        		y_old[j][length -1] = (startY) + ((endY  - startY))*(Math.sqrt(1 -((x/(endX - startX))*(x/(endX - startX)))));
	        	}
*/
	    
        }
    
     //Internal Boundary
     /*   for (int i =0 ; i < length; i++) {
        	for (int j=0 ; j < height; j++) {
        	
        	}
        }
        */
        
        
    }

    // Compute the maximum difference between corresponding elements from two 2D
    // grids
    public double computeMaxDiff(double[][] phi_new, double[][] phi_old) {

        double phidiffmax = Math.abs(phi_new[1][1] - phi_old[1][1]);

        for (int j = 1; j < height - 1; j++) {
            for (int i = 1; i < length - 1; i++) {
                double phidiff = Math.abs(phi_new[j][i] - phi_old[j][i]);

                if (phidiff > phidiffmax)
                    phidiffmax = phidiff;
            }
        }

        return phidiffmax;

    }

    // Copy the old matrix into the new one (during set-up phase)
    public void copyMatricesOldToNew(double[][] phi_new,
            double[][] phi_old) {

        // Copy old matrices into new for difference comparison
        for (int j = 0; j < height; j++) {
            for (int i = 0; i < length; i++) {
                phi_new[j][i] = phi_old[j][i];
            }
        }

    }

    // Copy the new matrix into the old one (during solving phase)
    public void copyMatricesNewToOld(double[][] phi_new,
            double[][] phi_old, double omegaSOR) {

        // Copy new matrices into old for difference comparison with SOR
        // (successive over-relaxation)
        for (int j = 0; j < height; j++) {
            for (int i = 0; i < length; i++) {
                phi_old[j][i] = phi_old[j][i] + omegaSOR
                        * (phi_new[j][i] - phi_old[j][i]);
            }
        }

    }
        
}
