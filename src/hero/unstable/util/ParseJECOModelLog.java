/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/** 
* Comments:
* Desestimate inputs without dependance of Inputs
*/

package hero.unstable.util;


import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author jlrisco
 * @author josueportiz
 */
public class ParseJECOModelLog implements Comparable<ParseJECOModelLog> {

    public static final String BEST_FIT_STR = "Best FIT=";
    public static final String EXPR_STR = "; Expression=";

    protected double fitness;
    protected String expression;

    public ParseJECOModelLog(double fitness, String expression, boolean removeAutorregressive) {
        this.fitness = fitness;
        this.expression = expression;
    }

    public static ParseJECOModelLog parse(String line, int numInputs, boolean removeAutorregressive) {
        int numVariables = numInputs + 2;
        int bestFitIdx = line.indexOf(BEST_FIT_STR);
        if (bestFitIdx < 0) {
            return null;
        }
        int exprIdx = line.indexOf(EXPR_STR);
        if (exprIdx < 0) {
            return null;
        }
        
       String sub = line.substring(bestFitIdx + BEST_FIT_STR.length(), exprIdx);
        double bestFit = Double.valueOf(sub);
        String exprStr = line.substring(exprIdx + EXPR_STR.length()).replaceAll(" ", "");
        
        /* Desestimate inputs without dependance of Inputs*/
        if (removeAutorregressive) {
            String patternStr = "getVariable\\([1-9]{1}";
            String patternStrMy = "My";
            
            Pattern pattern = Pattern.compile(patternStr);
            Pattern patternMy = Pattern.compile(patternStrMy);
            
            Matcher matcher = pattern.matcher(line);
            Matcher matcher2 = patternMy.matcher(line);
            
            if(!matcher.find() && !matcher2.find()){
                System.out.println("This expression does not depend on the Inputs. Avoided:");
                System.out.println("\tFit = " + bestFit + ". Expression = " + exprStr);
                return null;
            }
        }
        
        for (int j = 0; j < numVariables; ++j) {
            if (j == 0) {
                exprStr = exprStr.replaceAll("yr\\(", "getVariable\\(" + j + ",");
            } else if (j == numVariables - 1) {
                exprStr = exprStr.replaceAll("yp\\(", "getVariable\\(" + j + ",");
            } else {
                exprStr = exprStr.replaceAll("u" + j + "\\(", "getVariable\\(" + j + ",");
            }

        }
        ParseJECOModelLog record = new ParseJECOModelLog(bestFit, exprStr, removeAutorregressive);
        return record;
    }

    @Override
    public String toString() {
        StringBuilder buffer = new StringBuilder();
        buffer.append(fitness);
        buffer.append(", ");
        buffer.append(expression);
        return buffer.toString();
    }

    @Override
    public int compareTo(ParseJECOModelLog right) {
        double fitLeft = fitness;
        double fitRight = right.fitness;
        if (fitLeft > fitRight) {
            return -1;
        }
        if (fitLeft < fitRight) {
            return 1;
        }
        return 0;
    }
}
