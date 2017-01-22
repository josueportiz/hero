/*
 * Copyright (C) 2010-2016 José Luis Risco Martín <jlrisco@ucm.es>
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
 *  - José Luis Risco Martín
 */
package hero.core.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.logging.Logger;
import hero.core.algorithm.metaheuristic.moge.AbstractProblemGE;

/**
 * Class to manage a data table. 
 *
 * @author José Luis Risco Martín
 */
public class DataTable {

    private static final Logger LOGGER = Logger.getLogger(DataTable.class.getName());
    protected AbstractProblemGE problem;
    protected String path = null;
    protected ArrayList<double[]> data = new ArrayList<>();
    protected int numInputColumns = 0;
    protected int numTotalColumns = 0;

    protected double bestFitness = Double.POSITIVE_INFINITY;

    public DataTable(AbstractProblemGE problem, String dataPath) throws IOException {
        this(dataPath);
        this.problem = problem;
    }
    
    public DataTable(String dataPath) throws IOException {
        this.path = dataPath;
        LOGGER.info("Reading data file ...");
        loadData(dataPath);
        LOGGER.info("... done.");
    }

    public final void loadData(String dataPath) throws IOException {
        data.clear();
        BufferedReader reader = new BufferedReader(new FileReader(new File(dataPath)));
        String line;
        while ((line = reader.readLine()) != null) {
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            String[] parts = line.split("[\\s,;]+");
            if (parts.length > numInputColumns) {
                numInputColumns = parts.length;
                numTotalColumns = numInputColumns + 1;
            }
            double[] dataLine = new double[numTotalColumns];
            for (int j = 0; j < numInputColumns; ++j) {
                dataLine[j] = Double.valueOf(parts[j]);
            }
            data.add(dataLine);
        }
        reader.close();
    }

    public double computeFIT() {
        double meanXref = 0.0;
        for (int i = 0; i < data.size(); ++i) {
            meanXref += data.get(i)[0];
        }
        meanXref = meanXref / data.size();

        double num = 0.0, den = 0.0;
        for (int i = 0; i < data.size(); ++i) {
            num += Math.pow(data.get(i)[0] - data.get(i)[numInputColumns], 2.0);
            den += Math.pow(data.get(i)[0] - meanXref, 2.0);
        }
        double fit = (Math.sqrt(num) / Math.sqrt(den));
        return fit;
    }

    public ArrayList<double[]> getData(ArrayList<Integer> rows, ArrayList<Integer> cols) {
        ArrayList<double[]> selectedData = new ArrayList<double[]>();        
        double[] auxData = new double[cols.size()];
        int auxC = 0;
        
        if (rows.size() == 0){
            for (int t = 0; t < data.size(); t++){
                rows.add(t);
            }
        }
        if (cols.size() == 0){
            LOGGER.severe("Please introduce an array that indicates the desired columns.");
        }
        for (int i = 0; i < rows.size(); ++i) {
            auxData = new double[cols.size()];
            auxC = 0;
            double[] r = data.get(rows.get(i));
            for (int j = 0; j < cols.size(); ++j) {
                auxData[auxC++] = r[cols.get(j)];
            }
            selectedData.add(auxData);
        }
               
        return selectedData;
    }
    
    public ArrayList<double[]> getData() {
        return data;
    }
    
    public String getPath() {
        return path;
    }
    
    public int getPredictorColumn() {
        return numInputColumns;
    }
}
