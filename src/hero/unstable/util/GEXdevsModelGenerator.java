/*
* Copyright (C) 2014-2016 José Luis Risco Martín <jlrisco@ucm.es> and
* Saurabh Mittal <smittal@duniptech.com>
*
* This program is free software: you can redistribute it and/or modify
* it under the terms of the GNU Lesser General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
* (at your option) any later version.
*
* This library is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU General Public License for more details.
*
* You should have received a copy of the GNU Lesser General Public
* License along with this library.  If not, see <http://www.gnu.org/licenses/>.
*
* Contributors:
*  - José Luis Risco Martín
*  - Josué Pagán Ortiz
*/
package hero.unstable.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 *
 * @author jlrisco
 * @author josueportiz
 */
public class GEXdevsModelGenerator {
    protected int NUM_INPUTS = 4;
    protected Boolean REMOVE_AUTO_REGRESSIVE = true;
    protected String modelPath;
    protected int horizon;
    protected static String storeModelPath;
    protected int moduleModelNum;
    
    protected ArrayList<LinkedList<ParseJECOModelLog>> models = new ArrayList<>();
    protected LinkedList<ParseJECOModelLog> allModels = new LinkedList<>();
    
    public GEXdevsModelGenerator(String modelPath, int horizon, int moduleModelNum) {
        this.modelPath = modelPath;
        this.horizon = horizon;
        this.moduleModelNum = moduleModelNum;
    }
    
    public void fillModels() throws IOException {
        LinkedList<ParseJECOModelLog> modelM = new LinkedList<>();
        
        try (BufferedReader reader = new BufferedReader(new FileReader(new File(modelPath)))) {
            String line = reader.readLine();
            while (line != null) {
                ParseJECOModelLog record = ParseJECOModelLog.parse(line, NUM_INPUTS, REMOVE_AUTO_REGRESSIVE);
                if (record != null) {
                    modelM.add(record);
                    allModels.add(record);
                }
                line = reader.readLine();
            }
        }
        models.add(modelM);
    }
    
    public void sortModels() {
        Collections.sort(allModels);
        for (int m = 0; m < models.size(); ++m) {
            LinkedList<ParseJECOModelLog> modelM = models.get(0);
            Collections.sort(modelM);
        }
        System.out.println("M* sorted:");
        System.out.println(allModels.toString());
    }
    
    public void writeEvaluator() throws IOException {
        for (int h= horizon; h <= horizon; h += 10){            
            for (int i = moduleModelNum; i <= moduleModelNum; i++){
                BufferedWriter writer = new BufferedWriter(new FileWriter(new File(storeModelPath + "/GEModel" + h + "_" + i + ".java")));
                
                writer.append("/*\n");
                writer.append(" * Copyright (C) 2014-2016 José Luis Risco Martín <jlrisco@ucm.es> and \n");
                writer.append(" * Saurabh Mittal <smittal@duniptech.com>\n");
                writer.append(" *\n");
                writer.append(" * This program is free software: you can redistribute it and/or modify\n");
                writer.append(" * it under the terms of the GNU Lesser General Public License as published by\n");
                writer.append(" * the Free Software Foundation, either version 3 of the License, or\n");
                writer.append(" * (at your option) any later version.\n");
                writer.append(" *\n");
                writer.append(" * This library is distributed in the hope that it will be useful,\n");
                writer.append(" * but WITHOUT ANY WARRANTY; without even the implied warranty of\n");
                writer.append(" * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the\n");
                writer.append(" * GNU General Public License for more details.\n");
                writer.append(" *\n");
                writer.append(" * You should have received a copy of the GNU Lesser General Public\n");
                writer.append(" * License along with this library.  If not, see <http://www.gnu.org/licenses/>.\n");
                writer.append(" *\n");
                writer.append(" * Contributors:\n");
                writer.append(" *  - José Luis Risco Martín\n");
                writer.append(" *  - Josué Pagán Ortiz\n");
                writer.append(" */\n");
                writer.append("package xdevs.lib.migraine.models;\n");
                writer.append("\n");
                writer.append("import xdevs.core.modeling.Atomic;\n");
                writer.append("import xdevs.core.modeling.InPort;\n");
                writer.append("import xdevs.core.modeling.OutPort;\n");
                writer.append("\n");
                writer.append("import java.util.ArrayList;\n");
                writer.append("import org.apache.commons.math3.complex.Complex;\n");
                writer.append("import org.apache.commons.math3.transform.DftNormalization;\n");
                writer.append("import org.apache.commons.math3.transform.FastFourierTransformer;\n");
                writer.append("import org.apache.commons.math3.transform.TransformType;\n");
                writer.append("\n");
                writer.append("/**\n");
                writer.append(" *\n");
                writer.append(" * @author josueportiz\n");
                writer.append(" */\n");
                writer.append("public class GEModel" + h + "_" + i + " extends Atomic {\n");
                writer.append("\n");
                writer.append("\tpublic InPort<double[]> iData = new InPort<>(\"iData\");\n");
                writer.append("\tpublic InPort<Integer> iK = new InPort<>(\"iK\");\n");
                writer.append("\tpublic OutPort<Double> oYhat = new OutPort<>(\"oYhat\");\n");
                writer.append("\n");
                writer.append("\t\n");
                writer.append("\tprotected Integer NUM_INPUTS = 5;\n");
                writer.append("\tprotected ArrayList<double[]> dataToEvaluate = new ArrayList<>();\n");
                writer.append("\tprotected Double yHat = 0.0;\n");
                writer.append("\tprotected double[] iUYYhat = new double[NUM_INPUTS+1];\n");
                writer.append("\t\n");
                writer.append("\tpublic GEModel" + h + "_" + i + "(String name) {\n");
                writer.append("\t\tsuper(name);\n");
                writer.append("\t\taddInPort(iData);\n");
                writer.append("\t\taddOutPort(oYhat);\n");
                writer.append("\t}\n");
                writer.append("\t\n");
                writer.append("\tpublic GEModel" + h + "_" + i + "() {\n");
                writer.append("\t\tthis(GEModel" + h + "_" + i + ".class.getName());\n");
                writer.append("\t}\n");
                writer.append("\n");
                writer.append("\t@Override\n");
                writer.append("\tpublic void initialize() {\n");
                writer.append("\t\tthis.passivate();\n");
                writer.append("\t}\n");
                writer.append("\n");
                writer.append("\t@Override\n");
                writer.append("\tpublic void exit() {\n");
                writer.append("\t}\n");
                writer.append("\n");
                writer.append("\t@Override\n");
                writer.append("\tpublic void deltint() {\n");
                writer.append("\t\tsuper.passivate();\n");
                writer.append("\t}\n");
                writer.append("\n");
                writer.append("\t@Override\n");
                writer.append("\tpublic void deltext(double e) {\n");
                writer.append("\t\tdouble[] tempValueAtIData = iData.getSingleValue();\n");
                writer.append("\t\tif ((tempValueAtIData != null) && (!iK.isEmpty())){\n");
                writer.append("\t\t\tSystem.arraycopy(tempValueAtIData, 0, iUYYhat, 0, NUM_INPUTS);\n");
                writer.append("\t\t\tiUYYhat[NUM_INPUTS] = yHat;\n");
                writer.append("\t\t\tdataToEvaluate.add(iUYYhat);\n");
                writer.append("\t\t\tyHat = evaluate(iK.getSingleValue());\n");
                writer.append("\t\t\tsuper.activate();\n");
                writer.append("\t\t\t}\n");                
                writer.append("\t}\n");
                writer.append("\n");
                writer.append("\t@Override\n");
                writer.append("\tpublic void lambda() {\n");
                writer.append("\t\toYhat.addValue(yHat);\n");
                writer.append("\t\tsuper.passivate();\n");
                writer.append("\t}\n");
                writer.append("\t\n");
                
                /* Evaluator */
                writer.append("\tpublic Double evaluate(int k) {\n");
                String expression = allModels.get(0).expression; // Get only the best model (the first one)
                expression = expression.replaceAll("getVariable\\(" + (1 + NUM_INPUTS) + ",", "getVariable\\(" + (1 + NUM_INPUTS) + ",");
                writer.append("\t\t\t\t\tDouble predictors = " + expression + ";\n");
                writer.append("\t\t\treturn predictors;\n");
                writer.append("\t}\n");
                
                
                /* My functions */
                writer.append("public double MyDrvFFT(int from, int to, int idxVar) {\n");
                writer.append("double[] fftIdxVar = myFFT(from, to, idxVar);\n");
                writer.append("return (fftIdxVar[fftIdxVar.length - 1] - fftIdxVar[0]) / (fftIdxVar.length);\n");
                writer.append("}\n");
                
                writer.append("public double MyMaxFFT(int from, int to, int idxVar) {\n");
                writer.append("double res = Double.NEGATIVE_INFINITY;\n");
                writer.append("double[] fftIdxVar = myFFT(from, to, idxVar);\n");
                writer.append("for (int i = 0; i < fftIdxVar.length; ++i) {\n");
                writer.append("if (fftIdxVar[i] > res) {\n");
                writer.append("res = fftIdxVar[i];\n");
                writer.append("}\n");
                writer.append("}\n");
                writer.append("return res;\n");
                writer.append("}\n");
                
                
                writer.append("public double MyMinFFT(int from, int to, int idxVar) {\n");
                writer.append("double res = Double.POSITIVE_INFINITY;\n");
                writer.append("double[] fftIdxVar = myFFT(from, to, idxVar);\n");
                writer.append("for (int i = 0; i < fftIdxVar.length; ++i) {\n");
                writer.append("if (fftIdxVar[i] < res) {\n");
                writer.append("res = fftIdxVar[i];\n");
                writer.append("}\n");
                writer.append("}\n");
                writer.append("return res;\n");
                writer.append("}\n");
                
                
                writer.append("public double MyAvgFFT(int from, int to, int idxVar) {\n");
                writer.append("int size = to - from + 1;\n");
                writer.append("double res = MySumFFT(from, to, idxVar) / size;\n");
                writer.append("return res;\n");
                writer.append("}\n");
                
                
                writer.append("public double MyMax(int from, int to, int idxVar) {\n");
                writer.append("double res = Double.NEGATIVE_INFINITY;\n");
                writer.append("double val;\n");
                writer.append("for (int i = from; i <= to; ++i) {\n");
                writer.append("val = getVariable(idxVar, i);\n");
                writer.append("if (val > res) {\n");
                writer.append("res = val;\n");
                writer.append("}\n");
                writer.append("}\n");
                writer.append("return res;\n");
                writer.append("}\n");
                
                writer.append("public double MyMin(int from, int to, int idxVar) {\n");
                writer.append("double res = Double.POSITIVE_INFINITY;\n");
                writer.append("double val;\n");
                writer.append("for (int i = from; i <= to; ++i) {\n");
                writer.append("val = getVariable(idxVar, i);\n");
                writer.append("if (val < res) {\n");
                writer.append("res = val;\n");
                writer.append("}\n");
                writer.append("}\n");
                writer.append("return res;\n");
                writer.append("}  \n");
                
                writer.append("public double MyAvg(int from, int to, int idxVar) {\n");
                writer.append("int size = to - from + 1;\n");
                writer.append("double res = MySum(from, to, idxVar) / size;\n");
                writer.append("return res;\n");
                writer.append("}\n");
                
                writer.append("public double MyStd(int from, int to, int idxVar) {\n");
                writer.append("double mean = MyAvg(from, to, idxVar);\n");
                writer.append("int N = to - from + 1;\n");
                writer.append("double sum = 0.0;\n");
                writer.append("for (int i = from; i <= to; ++i) {\n");
                writer.append("sum += Math.pow((getVariable(idxVar, i)-mean), 2);\n");
                writer.append("}\n");
                writer.append("double res = Math.sqrt(sum / (N-1));\n");
                writer.append("return res;\n");
                writer.append("}\n");
                
                writer.append("public double MySum(int from, int to, int idxVar) {\n");
                writer.append("double res = 0.0;\n");
                writer.append("for (int i = from; i <= to; ++i) {\n");
                writer.append("res += getVariable(idxVar, i);\n");
                writer.append("}\n");
                writer.append("return res;\n");
                writer.append("}\n");
                
                writer.append("public double MySumFFT(int from, int to, int idxVar) {\n");
                writer.append("double res = 0.0;\n");
                writer.append("double[] fftIdxVar = myFFT(from, to, idxVar);\n");
                writer.append("for (int i = 0; i < fftIdxVar.length; ++i) {\n");
                writer.append("res += fftIdxVar[i];\n");
                writer.append("}\n");
                writer.append("return res;\n");
                writer.append("}\n");
                
                writer.append("public double[] myFFT(int from, int to, int idxVar) {\n");
                writer.append("FastFourierTransformer fastFourierTransformer = new FastFourierTransformer(DftNormalization.STANDARD);\n");
                writer.append("Complex[] values = new Complex[to - from + 1];\n");
                writer.append("for (int i = 0; i < values.length; ++i) {\n");
                writer.append("values[i] = new Complex(getVariable(idxVar, from + i), 0);\n");
                writer.append("}\n");
                writer.append("Complex[] complexValues = fastFourierTransformer.transform(values, TransformType.FORWARD);\n");
                writer.append("double[] result = new double[complexValues.length];\n");
                writer.append("for (int i = 0; i < complexValues.length; ++i) {\n");
                writer.append("result[i] = complexValues[i].abs();\n");
                writer.append("}\n");
                writer.append("return result;\n");
                writer.append("}\n");
                
                writer.append("\tpublic double MyDrv(int from, int to, int idxVar) {\n");
                writer.append("\treturn (getVariable(idxVar, to) - getVariable(idxVar, from)) / (to - from + 1);\n");
                writer.append("\t}\n");
                writer.append("\n");
                
                /**
                 * Utils
                 * */
                writer.append("\tpublic double getVariable(int idxVar, int k) {\n");
                writer.append("\t\tif (k < 0) {\n");
                writer.append("\t\t\treturn 0.0;\n");
                writer.append("\t\t}\n");
                writer.append("\t\treturn dataToEvaluate.get(k)[idxVar];\n");
                writer.append("\t}\n");
                
                writer.append("}\n");
                writer.close();
            }
        }
    }
    
    
    public static void main(String[] args) throws IOException {
        int horizon = 10;
        String patientNum = "2";
        String patientName = "A";
        
        // MODELS:
        // 10 minutes horizon:
        //   patienA : {5, 6, 10}
        //   patienB : {2, 4, 8}
        // 20 minutes horizon:
        //   patienA : {3, 4, 11}
        //   patienB : {2, 5, 6}
        // 30 minutes horizon:
        //   patienA : {10, 14, 15}
        //   patienB : {1, 2, 6}
        
        //int[] selectedModels = {3, 4, 11};

        int[] selectedModels = {1};

        ArrayList<String> modelsPath = new ArrayList<>();
        // General:
        /*
        File currentDirFile = new File(".");
        String currentDir = currentDirFile.getAbsolutePath();
        */
        
        // Manual:
        String currentDir = "/home/josueportiz/Documentos/greendisc/hero/src/hero/unstable/util";                
        //String modelsBasePath = "/home/josueportiz/Dropbox/parsedDataPatients/patients/patient_" + patientNum + "/experiments/ge/resultsGE/Experiments-" + horizon;
        String modelsBasePath = "/home/josueportiz/Drive/PhD-Josue/about_papers/DATE_2017/specialSession/partial_results/geModels";

        // Example log file:      
        //String modelPath = currentDir + "outputModelsTestFile.log";
        
        // Add selected models:
        
        for (int i = 0; i < selectedModels.length; i++){
            modelsPath.add(modelsBasePath + "/Patient" + patientName +  "-" + selectedModels[i] + "-Experiment-" + horizon + ".log");
        }
                
        storeModelPath = currentDir + "/models";
        File storeModelPathDir = new File(storeModelPath);
        
        // If the directory does not exist, create it
        if (!storeModelPathDir.exists()) {
            System.out.println("creating directory: " + storeModelPath);
            boolean result = false;            
            try{
                storeModelPathDir.mkdir();
                result = true;
            }
            catch(SecurityException se){
                //handle it
            }
            if(result) {
                System.out.println(storeModelPathDir + " Created");
            }
        }
        
        for (int i = 0; i < modelsPath.size(); i++){
            String modelPath = modelsPath.get(i);
            GEXdevsModelGenerator modelGenerator = new GEXdevsModelGenerator(modelPath, horizon, i+1);
            try {
                modelGenerator.fillModels();
                modelGenerator.sortModels();
                modelGenerator.writeEvaluator();
            } catch (IOException ex) {
                Logger.getLogger(GEXdevsModelGenerator.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
}
