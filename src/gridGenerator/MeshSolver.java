package gridGenerator;

/**
 * Class containing methods which perform the main algorithmic computations throughout the mesh generation process.
 */

public class MeshSolver {

    public static final double e = 2.7182818284590452;
    private double[][] x_new, y_new, x_old, y_old;
    private double deltaZi, deltaEta;
    private int length, height;
    
    public MeshSolver(double[][] x_new, double[][] y_new, int length, int height, double deltaZi, double deltaEta) {
        this.x_new = x_new;
        this.y_new = y_new;
        this.length = length;
        this.height = height;
        this.deltaZi = deltaZi;
        this.deltaEta = deltaEta;
    }
    
    public MeshSolver(double[][] x_old, double[][] y_old, int length, int height) {
        this.x_old = x_old;
        this.y_old = y_old;
        this.length = length;
        this.height = height;
    }
    
    // Make an initial guess for the grid using Transfinite Interpolation
    public void initializeGrid() {

        for (int j = 1; j < height - 1; j++) {
            double dy = j / (height - 1.0);

            for (int i = 1; i < length - 1; i++) {
                double dx = i / (length - 1.0);
                x_old[j][i] = (1.0 - dy)
                        * x_old[0][i]
                        + dy
                        * x_old[height - 1][i]
                        + (1.0 - dx)
                        * x_old[j][0]
                        + dx
                        * x_old[j][length - 1]
                        - (dx * dy * x_old[height - 1][length - 1] + (1.0 - dx)
                                * dy * x_old[height - 1][0] + dx * (1.0 - dy)
                                * x_old[0][length - 1] + (1.0 - dx)
                                * (1.0 - dy) * x_old[0][0]);

                y_old[j][i] = (1.0 - dy)
                        * y_old[0][i]
                        + dy
                        * y_old[height - 1][i]
                        + (1.0 - dx)
                        * y_old[j][0]
                        + dx
                        * y_old[j][length - 1]
                        - (dx * dy * y_old[height - 1][length - 1] + (1.0 - dx)
                                * dy * y_old[height - 1][0] + dx * (1.0 - dy)
                                * y_old[0][length - 1] + (1.0 - dx)
                                * (1.0 - dy) * y_old[0][0]);
            }
        }

    }

    // Assemble the coefficients for the tridiagonal matrices if stretching is
    // disabled
    public void assembleCoeff(double[][] b, double[][] a, double[][] deTerm, double[][] dTerm, 
                    double[][] eTerm) {

        for (int i = 1; i < length - 1; i++) {
            for (int j = 1; j < height - 1; j++) {

                double xiNext = x_new[j][i + 1];
                double xiPrev = x_new[j][i - 1];
                double xjNext = x_new[j + 1][i];
                double xjPrev = x_new[j - 1][i];

                double yiNext = y_new[j][i + 1];
                double yiPrev = y_new[j][i - 1];
                double yjNext = y_new[j + 1][i];
                double yjPrev = y_new[j - 1][i];

                double xijNext = x_new[j + 1][i + 1];
                double xijPrev = x_new[j - 1][i - 1];
                double xiPrevjNext = x_new[j + 1][i - 1];
                double xiNextjPrev = x_new[j - 1][i + 1];

                double yijNext = y_new[j + 1][i + 1];
                double yijPrev = y_new[j - 1][i - 1];
                double yiPrevjNext = y_new[j + 1][i - 1];
                double yiNextjPrev = y_new[j - 1][i + 1];

                double x1 = 0.5 * (xiNext - xiPrev) / deltaZi;
                double x2 = 0.5 * (xjNext - xjPrev) / deltaEta;
                double y1 = 0.5 * (yiNext - yiPrev) / deltaZi;
                double y2 = 0.5 * (yjNext - yjPrev) / deltaEta;

                double g11 = x1 * x1 + y1 * y1;
                double g22 = x2 * x2 + y2 * y2;
                double g12 = x1 * x2 + y1 * y2;

                b[j][i] = 2.0 * (g11 / (deltaEta * deltaEta) + g22
                        / (deltaZi * deltaZi));
                a[j][i] = g11 / (deltaEta * deltaEta);
                deTerm[j][i] = g22 / (deltaZi * deltaZi);
                dTerm[j][i] = -0.5 * g12
                        * (xijNext + xijPrev - xiNextjPrev - xiPrevjNext)
                        / (deltaZi * deltaEta);
                eTerm[j][i] = -0.5 * g12
                        * (yijNext + yijPrev - yiNextjPrev - yiPrevjNext)
                        / (deltaZi * deltaEta);
            }
        }

    }

    // Solve the tridiagonal matrices using the TDMA algorithm if stretching is
    // disabled
    public void solveTDMA(double[][] phi, double[][] a, double[][] b, double[][] deTerm, double[][] dTerm) {

        int imax = length - 2, jmax = height - 2;
        int imin = 0, jmin = 0;
        double[] P = new double[height], Q = new double[length], bArr = new double[height];

        // Set P(1) to 0 since a(1) = c(1) = 0
        P[jmin] = 0.0;

        // Start West-East sweep
        for (int i = imin + 1; i <= imax; i++) {

            // Set Q(1) to x(1) since x(i) = P(i)x(i+1) + Q(i) and P(1) = 0
            Q[jmin] = phi[jmin][i];

            // Start South-North traverse
            for (int j = jmin + 1; j <= jmax; j++) {

                // Assemble TDMA coefficients, rename North, South, East and
                // West as follows

                // Store a's = c's in P
                P[j] = a[j][i];
                // Store d's/e's in Qx/Qy
                Q[j] = deTerm[j][i] * (phi[j][i + 1] + phi[j][i - 1])
                        + dTerm[j][i];
                // Store b's in bArr
                bArr[j] = b[j][i];

                // Calculate coefficients of recursive formuli
                double term = 1.0 / (bArr[j] - P[j] * P[j - 1]);
                Q[j] = (Q[j] + P[j] * Q[j - 1]) * term;
                P[j] = P[j] * term;

            }

            // Obtain new values of phi (either x or y)
            for (int j = jmax - 1; j > jmin; j--)
                phi[j][i] = P[j] * phi[j + 1][i] + Q[j];

        }

    }

}
