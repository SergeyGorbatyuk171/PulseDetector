package com.example.sergey.pulsdetector2;

import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;

/**
 * Created by sergey on 14.07.18.
 */

public class RelaxMeasurer {

    Integer window_size = 512;
    Integer shift = 10;
    PulseCalculator pc = new PulseCalculator(3, .0);
    Integer[] mockresult = {40,40,36,36,99,40,99,36,36,40,43,
            102,36,102,40,102,102,86,102,86,102,102,96,102,
            96,96,96,96,96,96,96,96,96,96,96,96,96,56,36,36,
            56,96,36,36,36,96,96,36,96,43,43,73,36,43,69,56,
            69,36,43,69,59,69,59,40,69,59,69,73,59,76,73,59,
            73,63,73,40,40,73,63,73,40,63,66,66,40,66,83,76,
            83,149,66,59,83,66,83,66,83,66,66,79,66,66,66,
            66,66,66,66,66,66,66,79,79,79,79,66,66,66};

    public ArrayList<Integer> getPulse(ArrayList<Integer> redsums){
        double fps = 28;//(float)redsums.size() / 60;
//        for (int i = 0; i < (redsums.size() - window_size) % shift; i++){
//            redsums.remove(0);
//        }
//
//        ArrayList<Integer> results = pc.processFloatingWindow(redsums, fps, shift, window_size);
//        StringBuilder sb = new StringBuilder();
//        sb.append("{");
//        for (Integer i:
//             results) {
//            sb.append(i+",");
//        }
//
//        Log.e("reds", redsums.size()+"");
//        Log.e("ress", sb.toString());
//        Log.e("fps", fps+"");

        ArrayList<Integer> results = new ArrayList<>(Arrays.asList(mockresult));
        ArrayList<Double> timestamps = new ArrayList<>(results.size());
        for (int i = 0; i<results.size(); i++){
            timestamps.add((256+i*shift)/fps);
        }

        cleanResults(results, timestamps);

        doRegression(results, timestamps, 3);

        return results;
    }

    private void cleanResults(ArrayList<Integer> results, ArrayList<Double> timestamps){
        ArrayList<Integer> cpres = (ArrayList<Integer>)results.clone();
        Collections.sort(cpres);
        int i=0;
        Double median;
        if (cpres.size() % 2 == 0){
            median = (cpres.get(cpres.size()/2) + cpres.get(cpres.size()/2 - 1))/2.0;
        }
        else{
            median = cpres.get(cpres.size()/2)+0.;
        }
        Log.e("Median", median.toString());

        while (i < results.size()){
            if ((results.get(i) < 0.65*median) || (results.get(i) > 1.35*median)){
                results.remove(i);
                timestamps.remove(i);
            }
            else {
                i++;
            }
        }
    }

    private void doRegression(ArrayList<Integer> results, ArrayList<Double> timestamps, int deg){
        double[][] X = new double[timestamps.size()][deg+1];
        for(int i = 0; i < timestamps.size(); i++){
            X[i][0] = 1.;
        }
        for(int i = 0; i < timestamps.size(); i++){
            for(int j = 1; j < deg+1; j++){
                X[i][j] = X[i][j-1]*timestamps.get(i);
            }
        }

        double[][] y = new double[results.size()][1];
        for (int i = 0; i<results.size(); i++){
            y[i][0] = results.get(i);
        }

        StringBuilder sbts = new StringBuilder();
        StringBuilder sbr = new StringBuilder();
        sbr.append("[");
        sbts.append("[");
        for (int i=0; i< results.size(); i++){
            sbr.append(results.get(i)+",");
            sbts.append(timestamps.get(i)+",");
        }
        sbr.append("]");
        Log.e("results", sbr.toString());
        sbts.append("]");
        Log.e("tss", sbts.toString());

        double coefs[][] = multiply(multiply(invert(multiply(transpose(X), X)), transpose(X)), y);
        StringBuilder sbc = new StringBuilder();
        sbc.append("[");
        for(int i=0; i<=deg; i++){
            sbc.append(coefs[i][0]+",");
        }
        sbc.append("]");
        Log.e("coefs", sbc.toString());

    }

    private static double[][] multiply(double[][] A, double[][] B) {

        int aRows = A.length;
        int aColumns = A[0].length;
        int bRows = B.length;
        int bColumns = B[0].length;

        if (aColumns != bRows) {
            throw new IllegalArgumentException("A:Rows: " + aColumns + " did not match B:Columns " + bRows + ".");
        }

        double[][] C = new double[aRows][bColumns];
        for (int i = 0; i < aRows; i++) {
            for (int j = 0; j < bColumns; j++) {
                C[i][j] = 0.00000;
            }
        }

        for (int i = 0; i < aRows; i++) { // aRow
            for (int j = 0; j < bColumns; j++) { // bColumn
                for (int k = 0; k < aColumns; k++) { // aColumn
                    C[i][j] += A[i][k] * B[k][j];
                }
            }
        }

        return C;
    }

    public static double[][] transpose(double [][] m){
        double[][] temp = new double[m[0].length][m.length];
        for (int i = 0; i < m.length; i++)
            for (int j = 0; j < m[0].length; j++)
                temp[j][i] = m[i][j];
        return temp;
    }

    private static double[][] invert(double a[][])
    {
        int n = a.length;
        double x[][] = new double[n][n];
        double b[][] = new double[n][n];
        int index[] = new int[n];
        for (int i=0; i<n; ++i)
            b[i][i] = 1;
        // Transform the matrix into an upper triangle
        gaussian(a, index);
        // Update the matrix b[i][j] with the ratios stored
        for (int i=0; i<n-1; ++i)
            for (int j=i+1; j<n; ++j)
                for (int k=0; k<n; ++k)
                    b[index[j]][k] -= a[index[j]][i]*b[index[i]][k];
        // Perform backward substitutions
        for (int i=0; i<n; ++i)
        {
            x[n-1][i] = b[index[n-1]][i]/a[index[n-1]][n-1];
            for (int j=n-2; j>=0; --j)
            {
                x[j][i] = b[index[j]][i];
                for (int k=j+1; k<n; ++k)
                {
                    x[j][i] -= a[index[j]][k]*x[k][i];
                }
                x[j][i] /= a[index[j]][j];
            }
        }
        return x;
    }

    // Method to carry out the partial-pivoting Gaussian
    // elimination.  Here index[] stores pivoting order.
    private static void gaussian(double a[][], int index[]) {
        int n = index.length;
        double c[] = new double[n];
        // Initialize the index
        for (int i = 0; i < n; ++i)
            index[i] = i;
        // Find the rescaling factors, one from each row
        for (int i = 0; i < n; ++i) {
            double c1 = 0;
            for (int j = 0; j < n; ++j) {
                double c0 = Math.abs(a[i][j]);
                if (c0 > c1) c1 = c0;
            }
            c[i] = c1;
        }
        // Search the pivoting element from each column
        int k = 0;
        for (int j = 0; j < n - 1; ++j) {
            double pi1 = 0;
            for (int i = j; i < n; ++i) {
                double pi0 = Math.abs(a[index[i]][j]);
                pi0 /= c[index[i]];
                if (pi0 > pi1) {
                    pi1 = pi0;
                    k = i;
                }
            }
            // Interchange rows according to the pivoting order
            int itmp = index[j];
            index[j] = index[k];
            index[k] = itmp;
            for (int i = j + 1; i < n; ++i) {
                double pj = a[index[i]][j] / a[index[j]][j];
                // Record pivoting ratios below the diagonal
                a[index[i]][j] = pj;

                // Modify other elements accordingly
                for (int l = j + 1; l < n; ++l)
                    a[index[i]][l] -= pj * a[index[j]][l];
            }
        }
    }
}
