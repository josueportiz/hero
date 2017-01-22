/*
* Copyright (C) 2010-2015 José Luis Risco Martín <jlrisco@ucm.es> and
* José Manuel Colmenar Verdugo <josemanuel.colmenar@urjc.es>
*
* This program is free software: you can redistribute it and/or modify
* it under the terms of the GNU General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
* (at your option) any later version.
*
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License
* along with this program.  If not, see <http://www.gnu.org/licenses/>.
*
* Contributors:
*  - Josué Pagán Ortíz
*  - José Luis Risco Martín
*/
package hero.unstable.test.parkinson;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Properties;
import java.util.Random;
import java.util.logging.Logger;
import hero.core.util.StringManagement;

/**
 * Class to manage a data table. The data table is passed
 * to this class as a regular data table.
 *
 * @author José Luis Risco Martín
 * @author Josué Pagán Ortiz
 */
public class DataTable {
    
    private static final Logger logger = Logger.getLogger(DataTable.class.getName());
    
    protected Properties problemProperties;
    protected ArrayList<double[]> table = new ArrayList<>();
    protected ArrayList<double[]> onlyFeatures = new ArrayList<>();
    protected ArrayList<double[]> idx = new ArrayList<>();
    protected ArrayList<double[]> yr = new ArrayList<>();
    protected ArrayList tableNames = new ArrayList<>();
    protected ArrayList<int[]> trainingFolds = new ArrayList<>();
    protected ArrayList<int[]> validationFold = new ArrayList<>();
    
    
    protected int idxBegin = -1;
    protected int idxEnd = -1;
    protected int numInputColumns = 0;
    protected double[] xLs = null;
    protected double[] xHs = null;
    
    protected int lengthIni = 0;
    protected int lengthEnd = 0;
    protected int[][] foldsIdxs;
        
    protected String featuresFile;
    protected String featuresNames;


    
    public DataTable(Properties problemProperties, String type, int idxBegin, int idxEnd) throws IOException {
        this.problemProperties = problemProperties;

        logger.info("Reading data file ...");
        setPaths(type);
        
        readData(featuresFile, table);
        readHead(featuresNames, tableNames);

        this.idxBegin = (idxBegin == -1) ? 0 : idxBegin;
        this.idxEnd = (idxEnd == -1) ? table.size() : idxEnd;
        
        logger.info("Evaluation interval: [" + this.idxBegin + "," + this.idxEnd + ")");
        logger.info("... done.");

        // Extract features, Ids and output:
        int pdLevelCol = Integer.valueOf(problemProperties.getProperty("PDLevelCol"));
        int idCol = Integer.valueOf(problemProperties.getProperty("IDCol"));
        double[] element;
        double[] featuresLine;
        
        for (int j=0; j<table.size(); j++){
            int featCounter = 0;
            featuresLine = new double[numInputColumns-2];
            
            for (int f=0; f<table.get(0).length; f++){    
                if (f==idCol){
                    element = new double[1];
                    element[0] = table.get(j)[f];
                    idx.add(element);
                }
                else if (f==pdLevelCol){
                    element = new double[1];
                    element[0] = table.get(j)[f];
                    yr.add(element);                    
                } else{
                    featuresLine[featCounter++] = table.get(j)[f];
                }
            }
            onlyFeatures.add(featuresLine);            
        }
        numInputColumns = 0;
        
        // Generate folds
        int n = 0;
        if ("training".equals(type)){
            n = Integer.valueOf(problemProperties.getProperty("N"));
        } else {
            n = 1;
        }
        boolean crossVal;
        
        if (("training".equals(type)) && ("yes".equals(problemProperties.getProperty("NFoldCrossVal")))) {
            crossVal = true;
        } else {
            crossVal = false;
        }
        randomizeDataSelection(table.size(), n, crossVal);
    }
    
    public DataTable(Properties problemProperties, String type) throws IOException {
        this(problemProperties, type, -1, -1);
    }
    
       
    public final void readData(String dataPath, ArrayList<double[]> dataTable) throws IOException {
        File file = new File(dataPath);
                
        if (file.exists()){
            
            try (BufferedReader reader = new BufferedReader(new FileReader(new File(dataPath)))) {
                String line;
                
                while ((line = reader.readLine()) != null) {
                    if (line.isEmpty() || line.startsWith("#")) {
                        continue;
                    }
                    String[] parts = line.split(";");
                    if (parts.length == 1){
                        parts = line.split(",");
                    }
                    if (parts.length > numInputColumns) {
                        numInputColumns = parts.length;
                    }
                    
                    double[] dataLine = new double[numInputColumns];
                    for (int j = 0; j < numInputColumns; j++) {
                        dataLine[j] = Double.valueOf(parts[j]);
                    }
                    
                    dataTable.add(dataLine);
                }
                reader.close();
            }
        }
        else {
            logger.finer("File: " + dataPath + " DOES NOT EXIST");
        }
    }
        

    public final void readHead(String dataPath, ArrayList headTable) throws IOException {
        File file = new File(dataPath);
        if (file.exists()){
            
            try (BufferedReader reader = new BufferedReader(new FileReader(new File(dataPath)))) {
                String line;
                
                while ((line = reader.readLine()) != null) {
                    if (line.isEmpty() || line.startsWith("#")) {
                        continue;
                    }
                    String[] parts = line.split(";");
                    if (parts.length == 1){
                        parts = line.split(",");
                    }
                    
                    for (int j = 0; j < parts.length; j++) {
                        headTable.add(parts[j]);
                    }                    
                }
                reader.close();
            }
        }
        else {
            logger.finer("File: " + dataPath + " DOES NOT EXIST");
        }
    }

    public double[] getOutput(){
        double[] yrData = new double[yr.size()];
        for (int i=0; i<yr.size(); i++){
            yrData[i] = yr.get(i)[0];
        }
        return yrData;           
    }
    
    public double[] getIdxs(){
        double[] idxData = new double[idx.size()];
        for (int i=0; i<idx.size(); i++){
            idxData[i] = idx.get(i)[0];
        }
        return idxData;         
    }
    
    public int getPredictorColumn() {
        return numInputColumns;
    }

    public ArrayList<double[]> getFeaturesTable(String type) {
         switch (type) {
            case "table":
                return table;
            case "names":
                return tableNames;
            case "features":
                return onlyFeatures;
            default:
                return table;
        }
    }
    
    public ArrayList<double[]> getFeaturesTable(int idx1, int idx2) {
        return new ArrayList(table.subList(idx1, idx2));           
    }

    public int[] getPatientsIdXs(String type, int foldIteration, boolean crossVal) throws IOException{
        int n = Integer.valueOf(problemProperties.getProperty("N"));
        int[] dataFolds = new int[0];
        
        switch (type) {
            case "training":
                dataFolds = trainingFolds.get(foldIteration);
                return dataFolds;                    
            case "validation":
                dataFolds = validationFold.get(foldIteration);
                    return dataFolds;
            default: 
                dataFolds = new int[n*(table.size()/n)];
                //for (int f = 0; f < foldsIdxs.length-1; f++){
                for (int f = 0; f < foldsIdxs.length; f++){
                    for (int e = 0; e < foldsIdxs[0].length; e++){
                        dataFolds[(f*n)+e] = foldsIdxs[f][e];
                    }
                }
                return dataFolds;
        }                    
    }
    
    /*
    josue: This should be removed to use getIdxs(). A lot of changes should be made.
    */
    public int[][] getPatientsIdXs(String fileIdxsPatients) throws IOException{
        ArrayList<double[]> tempIdxs = new ArrayList<>();
        readData(problemProperties.getProperty("DataPathBase") + fileIdxsPatients, tempIdxs);
        foldsIdxs = new int[tempIdxs.size()][1];
        
        for (int i=0; i<tempIdxs.size(); i++){
            foldsIdxs[i][0] = (int)tempIdxs.get(i)[0];
        }
        
        return foldsIdxs;
    }
    
    public void randomizeDataSelection(int elements, int groups, boolean randomize){
        int[][] folds = new int[groups][elements/groups];
        int[] elementsAvailable = new int[elements];
        
        for (int i=0; i<= elements-1; i++){
            elementsAvailable[i] = i;
        }
        if (randomize) {
            // Implementing Fisher–Yates shuffle
            Random rnd = new Random();
            for (int i = elementsAvailable.length - 1; i > 0; i--) {
                int index = rnd.nextInt(i + 1);
                // Simple swap
                int a = elementsAvailable[index];
                elementsAvailable[index] = elementsAvailable[i];
                elementsAvailable[i] = a;
            }
        }
        
        for (int i=0; i<= groups-1; i++){
            for (int j=0; j<= (elements/groups)-1; j++){
                folds[i][j] = elementsAvailable[j+(i*elements/groups)];
            }
        }
        
        // Create training and validations folds
        // Training folds
        for (int foldIteration = 0; foldIteration < folds.length; foldIteration++){
            int counter = 0;
            int[] dataFolds = new int[folds[0].length*(elements/groups)];
            for (int f = 0; f < folds.length; f++){
                if (f != foldIteration){
                    for (int e = 0; e < (elements/groups); e++){
                        dataFolds[counter++] = folds[f][e];
                    }
                }
            }
            trainingFolds.add(dataFolds);
        }
        
        // Validation folds
        for (int foldIteration = 0; foldIteration < folds.length; foldIteration++){
            int[] dataFolds = new int[(elements/groups)];
            for (int e = 0; e < (elements/groups); e++){
                dataFolds[e] = folds[foldIteration][e];
            }        
            validationFold.add(dataFolds);
        }                    
        
        // All folds
        foldsIdxs = folds;
    }
    
    public final void setPaths(String type) {
        String dataPath = problemProperties.getProperty("DataPathBase");
        featuresNames = (dataPath + problemProperties.getProperty("FeaturesNamesPath"));

        switch (type) {
            case "training":
                featuresFile = (dataPath + problemProperties.getProperty("FeaturesTrainingPath"));
                break;
            case "test":
                featuresFile = (dataPath + problemProperties.getProperty("FeaturesTestPath"));
                break;
        }
    }
}