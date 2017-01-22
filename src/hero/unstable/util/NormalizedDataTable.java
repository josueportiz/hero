/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package hero.unstable.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.logging.Logger;
import hero.core.algorithm.metaheuristic.moge.AbstractProblemGE;
import hero.core.operator.evaluator.AbstractPopEvaluator;
import hero.core.problem.Solution;
import hero.core.problem.Variable;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.logging.Level;

/**
 * Class to manage a normalized data table. Originally, the data table is passed
 * to this class as a regular data table. After the constructor, the data table
 * is normalized in the interval [1,2].
 *
 * @author José Luis Risco Martín
 */
public class NormalizedDataTable {

    private static final Logger logger = Logger.getLogger(NormalizedDataTable.class.getName());

    protected AbstractProblemGE problem;
    protected String trainingPath = null;
    protected ArrayList<double[]> trainingTable = new ArrayList<>();
    protected int idxBegin = -1;
    protected int idxEnd = -1;
    protected int numInputColumns = 0;
    protected int numTotalColumns = 0;
    protected double[] xLs = null;
    protected double[] xHs = null;

    protected double bestFitness = Double.POSITIVE_INFINITY;
    protected File resultsFileExp;
    protected File resultsFileFit;
    protected BufferedWriter writerExpressions = null;
    protected BufferedWriter writerFit = null;

    public NormalizedDataTable(AbstractProblemGE problem, String trainingPath, boolean normalizeTable, int idxBegin, int idxEnd) throws IOException {
        this.problem = problem;
        this.trainingPath = trainingPath;
        logger.info("Reading data file ...");
        fillDataTable(trainingPath, trainingTable);
        this.idxBegin = (idxBegin == -1) ? 0 : idxBegin;
        this.idxEnd = (idxEnd == -1) ? trainingTable.size() : idxEnd;
        logger.info("Evaluation interval: [" + this.idxBegin + "," + this.idxEnd + ")");
        logger.info("Normalization: " + normalizeTable);
        if (normalizeTable) {
            normalize(1.0, 2.0);
        }
        logger.info("... done.");       
    }

    public NormalizedDataTable(AbstractProblemGE problem, String trainingPath, boolean normalizeTable) throws IOException {
        this(problem, trainingPath, normalizeTable, -1, -1);
    }

    public final void fillDataTable(String dataPath, ArrayList<double[]> dataTable) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(new File(dataPath)));
        String line;
        while ((line = reader.readLine()) != null) {
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            String[] parts = line.split(";");
            if (parts.length > numInputColumns) {
                numInputColumns = parts.length;
                numTotalColumns = numInputColumns + 1;
            }
            double[] dataLine = new double[numTotalColumns];
            for (int j = 0; j < numInputColumns; ++j) {
                dataLine[j] = Double.valueOf(parts[j]);
            }
            dataTable.add(dataLine);
        }
        reader.close();
    }

    public String functionAsString(Solution<Variable<Integer>> solution) {
        return problem.generatePhenotype(solution).toString();        
    }
    
    public double evaluate(AbstractPopEvaluator evaluator, Solution<Variable<Integer>> solution, int idx) {
        String functionAsString = functionAsString(solution);
        double fitness = computeFitness(evaluator, idx);
        if (fitness < bestFitness) {
            bestFitness = fitness;
            for (int i = 0; i < numTotalColumns; ++i) {
                if (i == 0) {
                    functionAsString = functionAsString.replaceAll("getVariable\\(" + i + ",", "yr\\(");
                } else if (i == numTotalColumns - 1) {
                    functionAsString = functionAsString.replaceAll("getVariable\\(" + i + ",", "yp\\(");
                } else {
                    functionAsString = functionAsString.replaceAll("getVariable\\(" + i + ",", "u" + i + "\\(");
                }
            }
            logger.info("Best FIT=" + (100 * (1 - bestFitness)) + "; Expresion=" + functionAsString);
        }
        
        // Save solution and fitness
        //try {
            //writerExpressions.write(functionAsString + "\n");

            if (fitness < bestFitness) {
                bestFitness = fitness;
                logger.info("Best FIT=" + (100 * (1 - bestFitness)) + "; Expresion=" + functionAsString);
            }
            
            //writerExpressions.flush();
            
            // Save fitness
            //writerFit.write(100* (1- fitness) + "\n");
            //writerFit.flush();
//            writerFit.close();
//            writerExpressions.close();
       // } catch (IOException ex) {
        //    Logger.getLogger(NormalizedDataTable.class.getName()).log(Level.SEVERE, null, ex);
        //}       
        return fitness;
    }

    public double computeFitness(AbstractPopEvaluator evaluator, int idx) {
        evaluator.evaluateExpression(idx);
        ArrayList<double[]> timeTable = evaluator.getDataTable();
        return computeFitness(timeTable);
    }

    public final void normalize(double yL, double yH) {
        logger.info("Normalizing data in [" + yL + ", " + yH + "] ...");
        xLs = new double[numInputColumns];
        xHs = new double[numInputColumns];
        for (int i = 0; i < numInputColumns; ++i) {
            xLs[i] = Double.POSITIVE_INFINITY;
            xHs[i] = Double.NEGATIVE_INFINITY;
        }
        // We compute first minimum and maximum values:
        ArrayList<double[]> fullTable = new ArrayList<>();
        fullTable.addAll(trainingTable);
        for (int i = 0; i < fullTable.size(); ++i) {
            double[] row = fullTable.get(i);
            for (int j = 0; j < numInputColumns; ++j) {
                if (xLs[j] > row[j]) {
                    xLs[j] = row[j];
                }
                if (xHs[j] < row[j]) {
                    xHs[j] = row[j];
                }
            }
        }

        // Now we compute "m" and "n", being y = m*x + n
        // y is the new data
        // x is the old data
        double[] m = new double[numInputColumns];
        double[] n = new double[numInputColumns];
        for (int j = 0; j < numInputColumns; ++j) {
            m[j] = (yH - yL) / (xHs[j] - xLs[j]);
            n[j] = yL - m[j] * xLs[j];
        }
        // Finally, we normalize ...
        for (int i = 0; i < fullTable.size(); ++i) {
            double[] row = fullTable.get(i);
            for (int j = 0; j < numInputColumns; ++j) {
                row[j] = m[j] * row[j] + n[j];
            }
        }

        // ... and report the values of both xLs and xHs ...
        StringBuilder xLsAsString = new StringBuilder();
        StringBuilder xHsAsString = new StringBuilder();
        for (int j = 0; j < numInputColumns; ++j) {
            if (j > 0) {
                xLsAsString.append(", ");
                xHsAsString.append(", ");
            } else {
                xLsAsString.append("xLs=[");
                xHsAsString.append("xHs=[");
            }
            xLsAsString.append(xLs[j]);
            xHsAsString.append(xHs[j]);
        }
        xLsAsString.append("]");
        xHsAsString.append("]");
        logger.info(xLsAsString.toString());
        logger.info(xHsAsString.toString());
        logger.info("... done.");
    }

    public double computeFitness(ArrayList<double[]> timeTable) {
        double meanXref = 0.0;
        for (int i = idxBegin; i < idxEnd; ++i) {
            meanXref += timeTable.get(i)[0];
        }
        meanXref = meanXref / (idxEnd - idxBegin);

        double num = 0.0, den = 0.0;
        double fitness = 0;
        for (int i = idxBegin; i < idxEnd; ++i) {
            num += Math.pow(timeTable.get(i)[0] - timeTable.get(i)[numInputColumns], 2.0);
            den += Math.pow(timeTable.get(i)[0] - meanXref, 2.0);
        }
        fitness = (Math.sqrt(num) / Math.sqrt(den));
        return fitness;
    }

    public ArrayList<double[]> getTrainingTable() {
        return trainingTable;
    }

}
