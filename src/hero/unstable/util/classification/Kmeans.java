/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package hero.unstable.util.classification;

import hero.unstable.util.Maths;
import hero.unstable.util.MathsVector;
import java.util.ArrayList;
import java.util.Random;

/**
 * @author Josué Pagán Ortiz <jpagan at ucm.es>
 */

public class Kmeans {
    protected ArrayList<double[]> instances = new ArrayList<>();
    protected int k = 0;
    protected double[][] centroids;
    protected int[][] cluster;
    protected int numFeatures = 0;
    protected int[][] bestCluster;
    protected double bestL = Double.MAX_VALUE;
    
    public Kmeans(int k, ArrayList<double[]> instances){
        this.k = k;
        this.instances = instances;
        this.cluster = new int[instances.size()][2]; // Instances index & num cluster that belongs to
        this.numFeatures = instances.get(1).length;
        this.centroids = new double[k][numFeatures];
        
        for (int n = 0; n < instances.size(); n++){
            cluster[n][0] = n;
        }
    }
    
    public void runClassifier(){        
        for (int t = 0; t < 10; t++){
            double L = Double.MAX_VALUE;
            
            centroids = initCentroids("further-first");
            double distance = 0.0;
            boolean converged = false;
            ArrayList<Integer> elementsInCluster;
            
            
            while (!converged) {
                double[] distances = new double[instances.size()];
                
                // Adssign element n to closest center
                for (int n = 0; n < instances.size(); n++){
                    double[] element = instances.get(n);
                    double minDistance = Double.MAX_VALUE;
                    
                    for (int c = 0; c < centroids.length; c++){
                        distance = MathsVector.euclideanDistance(element, centroids[c]);
                        if (distance < minDistance){
                            cluster[n][1] = c; // It vary along the search
                            minDistance = distance;
                        }
                    }
                    distances[n] = minDistance;
                }
                
                // Re-estimate centroid of cluster k
                for (int c = 0; c < centroids.length; c++){
                    double[] sum = new double[numFeatures];
                    int nEle = 0;
                    for (int s = 0; s < sum.length; s++){
                        sum[s] = 0.0;
                    }
                    
                    for (int n = 0; n < instances.size(); n++){
                        if (cluster[n][1] == c){
                            sum = MathsVector.sum(sum, instances.get(n));
                            nEle += 1;
                        }
                    }
                    centroids[c] = MathsVector.div(sum, (double)(nEle));
                }
                
                // Check convergence:
                double sumL = 0.0;
                for (int l = 0; l < distances.length; l++){
                    sumL += distances[l];
                }
                if (sumL == L){
                    converged = true;
                }
                else {
                    L = sumL;
                }
            } // End K-means
            
            if (L < bestL){
                bestL = L;
                bestCluster = cluster;
            }
        }
        cluster = bestCluster;
    }
    
    public double[][] initCentroids(String type){        
        Random randomGenerator = new Random();
        double[][] initCentroids = new double[k][numFeatures];
        double distance = 0.0;
        
        if ("further-first".equals(type)){
            for (int i = 0; i < k; i++) {
                // Randomly initialize centroid for kth cluster
                // Furthest-first heuristic method for semi-random initialization:
                // 1) Pick a random sample m for the first centroid:
                initCentroids[0] = instances.get(randomGenerator.nextInt(instances.size()));
                
                // 2) Find the sample that is as far as possible from all previously
                //    assigned centroids.
                for (int j = 1; j < k; j++){
                    double maxDistance = 0.0;
                    for (int s = 0; s < instances.size(); s++){
                        for (int jj = 0; jj < j; jj++){
                            distance = MathsVector.euclideanDistance(instances.get(s), initCentroids[jj]);
                            if (distance > maxDistance){
                                maxDistance = distance;
                                initCentroids[j] = instances.get(s); // It may vary along the search
                            }
                        }
                    }
                    
                }
                
            }
        }
        else if (type == "k++"){
            System.err.println("Not supported yet.");
        }
    return initCentroids;
    }
    
    public int[][] getClassification(){
        return cluster;
    }
    
    public double[][] getCentroids(){
        return centroids;
    }
    
}
