/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package hero.unstable.test.migraine;

import java.util.ArrayList;
import hero.core.operator.evaluator.AbstractPopEvaluator;
import org.apache.commons.math3.complex.Complex;
import org.apache.commons.math3.transform.DftNormalization;
import org.apache.commons.math3.transform.FastFourierTransformer;
import org.apache.commons.math3.transform.TransformType;

/**
 *
 * @author José Luis Risco Martín <jlrisco at ucm.es>
 */
public class PopEvaluator extends AbstractPopEvaluator {

    public double getVariable(int idxVar, int k) {
        if (k < 0) {
            return 0.0;
        } else {
            return dataTable.get(k)[idxVar];
        }
    }

    public double MyInc(int from, int to, int idxVar) {
        return (getVariable(idxVar, to) - getVariable(idxVar, from)) / (to - from + 1);
    }
    
    public Double[] MyDrv(int from, int to, int idxVar) {
        Double[] dif = new Double[from-to];
        for (int i = from; i < to; ++i) {
            dif[i] = getVariable(idxVar, i+1) - getVariable(idxVar, i);
        }
        return dif;
    }

    public double MyIncFFT(int from, int to, int idxVar) {
        double[] fftIdxVar = myFFT(from, to, idxVar);
        return (fftIdxVar[fftIdxVar.length - 1] - fftIdxVar[0]) / (fftIdxVar.length);
    }

    public double MySum(int from, int to, int idxVar) {
        double res = 0.0;
        for (int i = from; i <= to; ++i) {
            res += getVariable(idxVar, i);
        }
        return res;
    }

    public double MySumFFT(int from, int to, int idxVar) {
        double res = 0.0;
        double[] fftIdxVar = myFFT(from, to, idxVar);
        for (int i = 0; i < fftIdxVar.length; ++i) {
            res += fftIdxVar[i];
        }
        return res;
    }

    public double MyMax(int from, int to, int idxVar) {
        double res = Double.NEGATIVE_INFINITY;
        double val;
        for (int i = from; i <= to; ++i) {
            val = getVariable(idxVar, i);
            if (val > res) {
                res = val;
            }
        }
        return res;
    }

    public double MyMaxFFT(int from, int to, int idxVar) {
        double res = Double.NEGATIVE_INFINITY;
        double[] fftIdxVar = myFFT(from, to, idxVar);
        for (int i = 0; i < fftIdxVar.length; ++i) {
            if (fftIdxVar[i] > res) {
                res = fftIdxVar[i];
            }
        }
        return res;
    }

    public double MyMin(int from, int to, int idxVar) {
        double res = Double.POSITIVE_INFINITY;
        double val;
        for (int i = from; i <= to; ++i) {
            val = getVariable(idxVar, i);
            if (val < res) {
                res = val;
            }
        }
        return res;
    }

    public double MyMinFFT(int from, int to, int idxVar) {
        double res = Double.POSITIVE_INFINITY;
        double[] fftIdxVar = myFFT(from, to, idxVar);
        for (int i = 0; i < fftIdxVar.length; ++i) {
            if (fftIdxVar[i] < res) {
                res = fftIdxVar[i];
            }
        }
        return res;
    }

    public double MyAvg(int from, int to, int idxVar) {
        int size = to - from + 1;
        double res = MySum(from, to, idxVar) / size;
        return res;
    }

    public double MyStd(int from, int to, int idxVar) {
        double mean = MyAvg(from, to, idxVar);
        int N = to - from + 1;
        double sum = 0.0;
        for (int i = from; i <= to; ++i) {
            sum += Math.pow((getVariable(idxVar, i)-mean), 2);
        }
        
        double res = Math.sqrt(sum / (N-1));
        return res;
    }

    public double MyAvgFFT(int from, int to, int idxVar) {
        int size = to - from + 1;
        double res = MySumFFT(from, to, idxVar) / size;
        return res;
    }

    @Override
    public void evaluateExpression(int idxExpr) {
        double[] rowPred = dataTable.get(0);
        rowPred[rowPred.length - 1] = rowPred[0];
        for (int k = 0; k < dataTable.size() - 1; ++k) {
            rowPred = dataTable.get(k + 1);
            rowPred[rowPred.length - 1] = evaluate(idxExpr, k);
        }
    }

    public double[] myFFT(int from, int to, int idxVar) {
        FastFourierTransformer fastFourierTransformer = new FastFourierTransformer(DftNormalization.STANDARD);
        Complex[] values = new Complex[to - from + 1];
        for (int i = 0; i < values.length; ++i) {
            values[i] = new Complex(getVariable(idxVar, from + i), 0);
        }
        Complex[] complexValues = fastFourierTransformer.transform(values, TransformType.FORWARD);
        double[] result = new double[complexValues.length];
        for (int i = 0; i < complexValues.length; ++i) {
            result[i] = complexValues[i].abs();
        }
        return result;
    }

    @Override
    public double evaluate(int x, int y) {
        return 0;
    }

    public static void main(String[] args) {
        int numRows = 32;
        int numColumns = 1;
        PopEvaluator popEvaluator = new PopEvaluator();
        popEvaluator.dataTable = new ArrayList<>();
        for (int i = 0; i < numRows; ++i) {
            double[] row = new double[numColumns];
            for (int j = 0; j < row.length; ++j) {
                row[j] = Math.random();
            }
            popEvaluator.dataTable.add(row);
        }
        double[] result = popEvaluator.myFFT(0, numRows - 1, 0);
        System.out.println("Size: " + result.length);
        for (int i = 0; i < result.length; ++i) {
            System.out.println(result[i]);
        }
        System.out.println("---------------------------------------");
        System.out.println("MyIncFFT=" + popEvaluator.MyIncFFT(0, numRows - 1, 0));
        System.out.println("MySumFFT=" + popEvaluator.MySumFFT(0, numRows - 1, 0));
        System.out.println("MyMaxFFT=" + popEvaluator.MyMaxFFT(0, numRows - 1, 0));
        System.out.println("MyMinFFT=" + popEvaluator.MyMinFFT(0, numRows - 1, 0));
        System.out.println("MyAvgFFT=" + popEvaluator.MyAvgFFT(0, numRows - 1, 0));
    }

}
