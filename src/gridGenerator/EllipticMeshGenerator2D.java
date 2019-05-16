
package gridGenerator;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Scanner;
import java.util.Arrays;

import javax.imageio.stream.FileImageOutputStream;
import javax.imageio.stream.ImageOutputStream;
import javax.swing.JFrame;
import javax.swing.WindowConstants;

import org.math.plot.Plot2DPanel;
import org.math.plot.plotObjects.BaseLabel;


public class EllipticMeshGenerator2D {

    private static double deltaX = 1.0, deltaY = 1.0;
    private static int boundaryType = 7;
    private static String boundaryName = "";
    private static double amplitude, stepSize, radius, centerX;
    private static double startX = 0.0, startY = 0.0;
    private static double endX = 100.0, endY = 100.0;
    private static double[][] x_old, y_old, x_new, y_new;
    private static int length;
    private static int height;
    private static double b[][], a[][], c[][], deTerm[][], dTerm[][],
            eTerm[][];
    private static double deltaZi = 1.0, deltaEta = 1.0;
    private static double xdiffmax = 100.0, ydiffmax = 100.0;
    private static int count = 0;
    private static final double diffThreshold = 1E-30, omegaSOR = 1000000.0;
    private static Font plotFont;
    private static Plot2DPanel plot;
    private static Scanner in;
    private static PrintWriter outputInfo, outputInitial, outputFinal;
    private static double animationFPS = 1;
    private static int animationFrameCount = 5;

    public static void main(String args[]) {

        String initialFileName = "Initial_Grid.txt";
        String infoFileName = "Grid_Info.txt";
        String finalFileName = "Final_Grid.txt";

        try {
            outputInitial = new PrintWriter(new FileOutputStream(
                    initialFileName, false));
            outputFinal = new PrintWriter(new FileOutputStream(finalFileName,
                    false));
            outputInfo = new PrintWriter(new FileOutputStream(infoFileName,
                    false));
        } catch (FileNotFoundException e) {
            System.out.println("File error. Program aborted.");
            System.exit(0);
        }

        in = new Scanner(System.in);
        System.out.println("Enter a starting X value");
        startX = in.nextDouble();
        System.out.println("Enter an ending X value");
        endX = in.nextDouble();
        System.out.println("Enter a starting Y value");
        startY = in.nextDouble();
        System.out.println("Enter an ending Y value");
        endY = in.nextDouble();
        outputInfo.println("X interval: [" + startX + ", " + endX + "]");
        outputInfo.println("Y interval: [" + startY + ", " + endY + "]");
        System.out
                .println("Enter a resolution value in the x direction");
        deltaX = in.nextDouble();
        System.out
                .println("Enter a resolution value in the ydirection");        
        deltaY = in.nextDouble();
        
        length = (int) ((endX - startX) / deltaX) + 1;
        height = (int) ((endY - startY) / deltaY) + 1;
        outputInfo.println("Resolution value: " + deltaX);


         boundaryName = "Semi-ellipse with a circular puncture";
         System.out.println("\nEnter an center location");
         centerX = in.nextDouble();
         System.out.println("\nEnter the radius of the circle");
         radius = in.nextDouble();


        outputInfo.println("\nBoundary type: " + boundaryName);
        outputInfo.println();
        in.nextLine();


        x_old = new double[height][length];
        y_old = new double[height][length];
        x_new = new double[height][length];
        y_new = new double[height][length];
        
        MeshHelper meshHelper = new MeshHelper(length, height, deltaX, deltaY);

        // ----------DEFINE GRID BOUNDARIES----------

        meshHelper.setBoundaries(startX, startY, endX, endY, boundaryType, amplitude,centerX, stepSize, radius, x_old, y_old);

        // ----------LINEAR INTERPOLATION FOR INTERIOR NODES----------

        MeshSolver meshSolverInitial = new MeshSolver(x_old, y_old, length, height);
        meshSolverInitial.initializeGrid();
        /*
         * Display plot of the initial guess grid
         */        
        MeshStatistics meshStatisticsInitial = new MeshStatistics(length, height, outputInfo);

        System.out.println("\nInitial\n");
        outputInfo.println("\nInitial\n");
        outputInfo.println();
        meshStatisticsInitial.checkOrthogonalityBoundary(x_old, y_old);
        System.out.println();
        outputInfo.println("");
        meshStatisticsInitial.checkOrthogonalityInterior(x_old, y_old);
        System.out.println();
        outputInfo.println("");

        double avgARI = meshStatisticsInitial.calcAvgAR(x_old, y_old);
        double arStdDevI = meshStatisticsInitial.calcStdDevAR(x_old, y_old, avgARI);

        System.out.println("\nThe average aspect ratio of all cells is: "
                + avgARI);
        System.out.println("The standard deviation of all aspect ratios is: "
                + arStdDevI);

        outputInfo.println("\nThe average aspect ratio of all cells is: "
                + avgARI);
        outputInfo.println("The standard deviation of all aspect ratios is: "
                + arStdDevI);
        outputInfo.println();

        String initialName = "Initial grid (Transfinite Interpolation) with "
                + boundaryName + " boundary";

        outputInitial.println(initialName);
        outputInitial.println();
        outputInitial.println("Grid points");
        outputInitial.println();
        outputInitial.println("   X\t\t\t\tY");
        outputInitial.println();
        
        // ----------- FOR ANIMATION ------------
        
        // Specifiy frames per second and slowdown factor
        double slowMo = 1.0;
        ImageOutputStream animationOutput = null;
        GifSequenceWriter animationWriter = null;

        try {
            // Initialize the gif writer
            animationOutput = new FileImageOutputStream(new File("animatedMesh.gif"));
            animationWriter = new GifSequenceWriter(animationOutput, 1, (int)(1000*slowMo/animationFPS), true);
        } catch (IOException e) {
            e.printStackTrace();
        }

        setUpGrid();
        plotInterior(initialName, Color.blue, x_old, y_old, true, true);
        plotHorizontalGridLines(initialName, Color.blue, x_old, y_old);
        plotVerticalGridLines(initialName, Color.blue, x_old, y_old);
        try {
            printGrid(initialName, x_old, y_old, true, animationWriter);
        } catch (IOException e2) {
            e2.printStackTrace();
        }

        /*
         * Set old matrices to new
         */

        meshHelper.copyMatricesOldToNew(x_new, x_old);
        meshHelper.copyMatricesOldToNew(y_new, y_old);
        
        // ----------- MAIN LOOP -----------

        while (xdiffmax > diffThreshold || ydiffmax > diffThreshold) {

            // ----------SET UP TRIDIAGONAL MATRICES----------

            b = new double[height][length];
            a = new double[height][length];
            c = new double[height][length];
            deTerm = new double[height][length];
            dTerm = new double[height][length];
            eTerm = new double[height][length];
            
            MeshSolver meshSolver = new MeshSolver(x_new, y_new, length, height, deltaZi, deltaEta);

            // Assemble cofficients
         
            meshSolver.assembleCoeff(b, a, deTerm, dTerm, eTerm);

            // ----------SOLVE MATRICES USING THE THOMAS ALGORITHM----------

            meshSolver.solveTDMA(x_new, a, b, deTerm, dTerm);

            // Put eTerms in dTerm array for calculating solution of y
            for (int i = 1; i < length - 1; i++) {
                for (int j = 1; j < height - 1; j++) {
                    dTerm[j][i] = eTerm[j][i];
                }
            }

            
            meshSolver.solveTDMA(y_new, a, b, deTerm, dTerm);

            xdiffmax = meshHelper.computeMaxDiff(x_new, x_old);
            ydiffmax = meshHelper.computeMaxDiff(y_new, y_old);

            /*
             * Set old matrices to new
             */

            meshHelper.copyMatricesNewToOld(x_new, x_old, omegaSOR);
            meshHelper.copyMatricesNewToOld(y_new, y_old, omegaSOR);

            count++;
            
            // if count is a multiple of animationFrameCount, print the grid to produce an animated effect
            
            if (count % animationFrameCount == 0) {
                Color darkGreen = new Color(71, 168, 54);


                String name = "Frame " + count + " " + 
                        " grid with " + boundaryName + " boundary";
                
                setUpGrid();
                plotInterior(name, darkGreen, x_new, y_new, false, false);
                plotHorizontalGridLines(name, darkGreen, x_new, y_new);
                plotVerticalGridLines(name, darkGreen, x_new, y_new);
                try {
                    printGrid(name, x_new, y_new, true, animationWriter);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        }

        /*
         * Display plot of the final transformed grid
         */
        Color darkGreen = new Color(71, 168, 54);


        String finalName = "Final "
                + " grid with " + boundaryName + " boundary";

        outputFinal.println(finalName);
        outputFinal.println();
        outputFinal.println("Grid points");
        outputFinal.println();
        outputFinal.println("   X\t\t\t\tY");
        outputFinal.println();

        setUpGrid();
        plotInterior(finalName, darkGreen, x_new, y_new, false, true);
        plotHorizontalGridLines(finalName, darkGreen, x_new, y_new);
        plotVerticalGridLines(finalName, darkGreen, x_new, y_new);
        try {
            printGrid(finalName, x_new, y_new, true, animationWriter);
        } catch (IOException e1) {
            e1.printStackTrace();
        }
        
        try {
            animationWriter.close();
            animationOutput.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        /*
         * Assess the grid's quality by determining its orthogonality and aspect
         * ratio statistics
         */
        
        MeshStatistics meshStatisticsFinal = new MeshStatistics(length, height, outputInfo);

        System.out.println("\nFinal" + "\n");
        outputInfo.println("\nFinal" + "\n");
        System.out.println("Completed in " + count + " iterations\n");
        outputInfo.println();
        outputInfo.println("Completed in " + count + " iterations\n");
        meshStatisticsFinal.checkOrthogonalityBoundary(x_new, y_new);
        System.out.println();
        outputInfo.println();
        meshStatisticsFinal.checkOrthogonalityInterior(x_new, y_new);

        double avgARF = meshStatisticsFinal.calcAvgAR(x_new, y_new);
        double arStdDevF = meshStatisticsFinal.calcStdDevAR(x_new, y_new, avgARF);

        System.out.println("\nThe average aspect ratio of all cells is: "
                + avgARF);
        System.out.println("The standard deviation of all aspect ratios is: "
                + arStdDevF);

        outputInfo.println();
        outputInfo.println("\nThe average aspect ratio of all cells is: "
                + avgARF);
        outputInfo.println("The standard deviation of all aspect ratios is: "
                + arStdDevF);

        outputInitial.close();
        outputFinal.close();
        outputInfo.close();
       

    }

    // Create necessary grid objects
    public static void setUpGrid() {

        plotFont = new Font(Font.MONOSPACED, Font.PLAIN, 12);
        plot = new Plot2DPanel();

    }

    // Plot interior grid nodes
    public static void plotInterior(String name, Color color, double[][] phi1,
            double[][] phi2, boolean printInitial, boolean writeToFile) {

        // Get X and Y coordinates of all nodes into separate arrays
        double[] X = new double[length * height], Y = new double[length
                * height];
        int iter = 0;

        while (iter <= length * height - 1)
            for (int j = 0; j < height; j++) {
                for (int i = 0; i < length; i++) {
                    X[iter] = phi1[j][i];
                    Y[iter] = phi2[j][i];
                    if (writeToFile) {
                        if (printInitial) {
                            outputInitial.printf("%.10f \t\t %.10f", X[iter],
                                    Y[iter]);
                            outputInitial.println();
                        } else {
                            outputFinal
                                    .printf("%.10f \t\t %.10f", X[iter], Y[iter]);
                            outputFinal.println();
                        }
                    }
                    iter++;
                }
            }

        // Plot grid nodes
        // plot.addScatterPlot(name, color, X, Y);

    }

    // Draw horizontal grid lines
    public static void plotHorizontalGridLines(String name, Color color,
            double[][] phi1, double[][] phi2) {

        double[][] xHoriz = new double[height][length], yHoriz = new double[height][length];
        for (int j = 0; j < height; j++) {
            for (int i = 0; i < length; i++) {
                xHoriz[j][i] = phi1[j][i];
                yHoriz[j][i] = phi2[j][i];
            }
        }

        for (int j = 0; j < height; j++) {
            plot.addLinePlot(name, color, xHoriz[j], yHoriz[j]);
        }

    }

    // Draw vertical grid lines
    public static void plotVerticalGridLines(String name, Color color,
            double[][] phi1, double[][] phi2) {

        double[][] xVert = new double[length][height], yVert = new double[length][height];
        for (int i = 0; i < length; i++) {
            for (int j = 0; j < height; j++) {
                xVert[i][j] = phi1[j][i];
                yVert[i][j] = phi2[j][i];
            }
        }

        for (int i = 0; i < length; i++) {
            plot.addLinePlot(name, color, xVert[i], yVert[i]);
        }

    }

    // Display the grid
    public static void printGrid(String name, double[][] phi1, double[][] phi2, boolean animate, 
                    GifSequenceWriter gifWriter) throws IOException {

        // Find the farthest nodes away from the center on the boundaries
        double maxl = phi1[0][0], maxt = phi2[height - 1][0], maxr = phi1[0][length - 1], maxb = phi2[0][0];

        for (int j = 0; j < height; j++) {

            if (phi1[j][0] < maxl)
                maxl = phi1[j][0];
            if (phi1[j][length - 1] > maxr)
                maxr = phi1[j][length - 1];

        }

        for (int i = 0; i < length; i++) {

            if (phi2[0][i] < maxb)
                maxb = phi2[0][i];
            if (phi2[height - 1][i] > maxt)
                maxt = phi2[height - 1][i];

        }

        plot.setFixedBounds(0, maxl, maxr);
        plot.setFixedBounds(1, maxb, maxt);
        plot.setAxisLabels("X", "Y");
        plot.getAxis(0).setLabelPosition(0.5, -0.1);
        plot.getAxis(0).setLabelFont(plotFont);
        plot.getAxis(1).setLabelPosition(-0.15, 0.5);
        plot.getAxis(1).setLabelFont(plotFont);
        BaseLabel title1 = new BaseLabel(name, Color.BLACK, 0.5, 1.1);
        title1.setFont(plotFont);
        plot.addPlotable(title1);
        JFrame frame1 = new JFrame(name);
        frame1.setSize(1000, 1000);
        frame1.setContentPane(plot);
        frame1.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        
        if (!animate) {
            frame1.setVisible(true);
        } else {
            //frame1.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
            frame1.setVisible(true);
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            // Create image from JFrame and write to the animation
            BufferedImage image = new BufferedImage(1000,1000, BufferedImage.TYPE_INT_RGB);
            Graphics2D graphics2D = image.createGraphics();
            frame1.paint(graphics2D);
            gifWriter.writeToSequence(image);
            //frame1.dispatchEvent(new WindowEvent(frame1, WindowEvent.WINDOW_CLOSING));
        }
        
    }

}