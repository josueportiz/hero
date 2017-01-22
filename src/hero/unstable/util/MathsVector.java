/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package hero.unstable.util;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

/**
 *
 * @author josueportiz
 */
public class MathsVector {
    public static double[] sum(double[] a, double[] b) {
        double[] suma = new double[a.length];
        for (int i = 0; i < a.length; i++){
            suma[i] = a[i] + b[i];
        }
        return suma;
    }
    
    public static double[] div(double[] a, double scalar) {
        double[] div = new double[a.length];
        for (int i = 0; i < a.length; i++){
            div[i] = a[i]/scalar;
        }
        return div;
    }
    
    
    public static double euclideanDistance(double[] a, double[] b) {
        double diff_square_sum = 0.0;
        for (int i = 0; i < a.length; i++) {
            diff_square_sum += (a[i] - b[i]) * (a[i] - b[i]);
        }
        return Math.sqrt(diff_square_sum);
    }
    
    public static void main(String[] args){
        int vectorLenght = 2;
        Random randomGenerator = new Random();
        double[] v1 = new double[vectorLenght];
        double[] v2 = new double[vectorLenght];
        
        for (int i = 0; i < vectorLenght; i++){
            v1[i] = randomGenerator.nextDouble();
            v2[i] = randomGenerator.nextDouble();
        }
                
        System.out.println("v1 = " + Arrays.toString(v1));
        System.out.println("v2 = " + Arrays.toString(v2));
        System.out.println("Euclidean distance = " + euclideanDistance(v1, v2));
    }
}
