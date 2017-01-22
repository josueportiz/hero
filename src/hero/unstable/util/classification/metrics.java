/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package hero.unstable.util.classification;

import hero.unstable.util.Maths;
import hero.unstable.util.MathsVector;
import java.util.ArrayList;

/**
 *
 * @author josueportiz
 */
public class metrics {
    public static double averageSilhouette(ArrayList<double[]> instances, int[][] cluster, int k){
        double[] s = new double[instances.size()];
        double[] B = new double[k];
        
        for (int n = 0; n < instances.size(); n++){
            int cn = cluster[n][1];
            
            // Cumulative distances and elements in the clusters
            double[] sums = new double[k];
            double[] elts = new double[k];
            for (int b = 0; b < k; b++){
                sums[b] = 0.0;
                elts[b] = 0.0;
            }
            
            // Compute distances:
            for (int m = 0; m < instances.size(); m++){
                int cm = cluster[m][1];
                double distance = MathsVector.euclideanDistance(instances.get(n), instances.get(m));
                
                if (n != m){
                    sums[cm] += distance;
                    elts[cm] += 1;
                }
            }
            
            // Calculate w(n) and b(n):
            // w(n): is the average distance from the nth point to the other points in its own cluster
            // b(n): min,k {B(n k)}
            //    B(n, k): is the average distance from the nth point to the other points in cluster k.
            double w = sums[cn]/elts[cn];
            double b = Double.MAX_VALUE;
            for (int i = 0; i < k; i++){
                if ((i != cn) && ((sums[i]/elts[i]) < b)) {
                    b = sums[i]/elts[i];
                }
            }
            
            // Store the silohuette for element nth:
            double [] auxArray = new double[2];
            auxArray[0] = w;
            auxArray[1] = b;
            s[n] = (b-w)/Maths.max(auxArray);
        }
        
        // Compute the average silhouette:
        double sum = 0.0;
        for (int i = 0; i < s.length; i++) {
            sum += s[i];
        }        
    return sum/s.length;
    }
}
