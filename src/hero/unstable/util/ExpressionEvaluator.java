/*
* To change this license header, choose License Headers in Project Properties.
* To change this template file, choose Tools | Templates
* and open the template in the editor.
*/
package hero.unstable.util;

import hero.core.algorithm.metaheuristic.moge.AbstractProblemGE;
import hero.core.operator.evaluator.AbstractPopEvaluator;
import hero.core.problem.Solution;
import hero.core.problem.Variable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.logging.Logger;

/**
 *
 * @author josueportiz
 */
public class ExpressionEvaluator {
    private static final Logger logger = Logger.getLogger(ExpressionEvaluator.class.getName());
    protected int idxBegin = -1;
    protected int idxEnd = -1;
    protected AbstractProblemGE problem;
    protected double bestFitness = Double.POSITIVE_INFINITY;
    protected ArrayList<double[]> dataTable = new ArrayList<>();
    
    public ExpressionEvaluator(AbstractProblemGE problem, ArrayList<double[]> dataTable, int idxBegin, int idxEnd) throws IOException {
        this.problem = problem;
        this.idxBegin = (idxBegin == -1) ? 0 : idxBegin;
        this.idxEnd = (idxEnd == -1) ? dataTable.size() : idxEnd;
    }
    
    public ExpressionEvaluator(AbstractProblemGE problem) throws IOException {
        this.problem = problem;
        this.idxBegin =  0;
        this.idxEnd = -1;
    }
    
    
    public String functionAsString(Solution<Variable<Integer>> solution){
        return problem.generatePhenotype(solution).toString();
    }
    
    public String evaluateExpression(AbstractPopEvaluator evaluator, Solution<Variable<Integer>> solution, int idx) {
        String functionAsString = functionAsString(solution);
        
        double fitness = computeFitness(evaluator, idx);
        if (fitness < bestFitness) {
            bestFitness = fitness;
            for (int i = 0; i < dataTable.get(0).length; ++i) {
                if (i == 0) {
                    functionAsString = functionAsString.replaceAll("getVariable\\(" + i + ",", "yr\\(");
                } else if (i == dataTable.get(0).length - 1) {
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
        
        solution.getObjectives().set(0, fitness);
        
        return functionAsString;
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
            num += Math.pow(timeTable.get(i)[0] - timeTable.get(i)[dataTable.get(0).length], 2.0);
            den += Math.pow(timeTable.get(i)[0] - meanXref, 2.0);
        }
        fitness = (Math.sqrt(num) / Math.sqrt(den));
        return fitness;
    }
    
    public double computeFitness(AbstractPopEvaluator evaluator, int idx) {
        evaluator.evaluateExpression(idx);
        ArrayList<double[]> timeTable = evaluator.getDataTable();
        return computeFitness(timeTable);
    }
}
