/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package hero.unstable.util;

import hero.core.algorithm.metaheuristic.moge.AbstractProblemGE;
import hero.core.algorithm.metaheuristic.moge.Phenotype;
import hero.core.problem.Problem;
import hero.core.problem.Solution;
import hero.core.problem.Variable;
import hero.core.util.bnf.Production;
import hero.core.util.bnf.Rule;

/**
 * @author jpagan@ucm.es
 * @version 1.0
 */
public class SolutionAsArray {    
    AbstractProblemGE problemGE;
    Problem<Variable<Boolean>> problemNSGAII;
    
    public SolutionAsArray(AbstractProblemGE problem){
        this.problemGE = problem;
    }
      
    public SolutionAsArray(Problem<Variable<Boolean>> problem){
        this.problemNSGAII = problem;
    }

    /**
     *
     * @param solution
     * @return
     */
    public String functionAsString(Solution<Variable<Integer>> solution){
        return problemGE.generatePhenotype(solution).toString();
    }
    
    public double[] getArray(Solution<Variable<Integer>> solution){
        String functionAsString = functionAsString(solution);
        return getArray(functionAsString);
    }
    
    public double[] getArray(String functionAsString){
        double[] fucntionAsArray;
        
        String[] parts = functionAsString.split(",");
        fucntionAsArray = new double[parts.length];
        
        for (int i = 0; i < parts.length; i++){
            fucntionAsArray[i] = Double.valueOf(parts[i]);
        }
        
        return fucntionAsArray;
    }
}
