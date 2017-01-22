/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package hero.unstable.util;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author josueportiz
 */
public class MatrixToFile {
    protected File file;
    protected String fileName;
    protected BufferedWriter writer;
    
    public MatrixToFile(String fileName){
        this.fileName = fileName;
        this.file = new File(fileName);
        this.writer = null;
        
        try {
            writer = new BufferedWriter(new FileWriter(file, false));
        } catch (IOException ex) {
            Logger.getLogger(MatrixToFile.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public void addRow(double[] row) {
        try {
            String rowAsString = Arrays.toString(row)
                    .replace(", ", ",")  //spaces
                    .replace("[", "")  //remove the right bracket
                    .replace("]", "")  //remove the left bracket
                    .trim();           //remove trailing spaces from partially initialized arrays
            writer.write(rowAsString + "\n");
            writer.flush();
                
        } catch (IOException ex) {
            Logger.getLogger(MatrixToFile.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public void addRow(double value) {
        double[] valueAsArray = new double[1];
        valueAsArray[0] = value;
        addRow(valueAsArray);
    }
    
     public void addRow(int value) {
        int[][] valueAsArray = new int[1][1];
        valueAsArray[0][0] = value;        
        addMatrix(copyFromIntArray(valueAsArray));
    }
     
    public void addMatrix(double[][] matrix) {
        for (double[] matrix1 : matrix) {
            addRow(matrix1);
        }
    }
    
    public void addMatrix(int[][] matrix) {
        addMatrix(copyFromIntArray(matrix));
    }
    
    public static double[][] copyFromIntArray(int[][] source) {
        double[][] dest = new double[source.length][source[0].length];
        for(int i=0; i<source.length; i++) {
            for(int j=0; j< source[i].length; j++) {                
                dest[i][j] = source[i][j];
            }
        }
        return dest;   
    }
}