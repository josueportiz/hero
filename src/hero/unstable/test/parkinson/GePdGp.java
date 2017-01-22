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
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import hero.core.algorithm.metaheuristic.ga.SimpleGeneticAlgorithm;
import hero.core.algorithm.metaheuristic.moge.AbstractProblemGE;
import hero.core.algorithm.metaheuristic.moge.Phenotype;
import hero.core.operator.comparator.SimpleDominance;
import hero.core.operator.crossover.SinglePointCrossover;
import hero.core.operator.evaluator.AbstractPopEvaluator;
import hero.core.operator.evaluator.AbstractPopPredictor;
import hero.core.operator.mutation.IntegerFlipMutation;
import hero.core.operator.selection.BinaryTournament;
import hero.core.optimization.threads.MasterWorkerThreads;
import hero.core.problem.Solution;
import hero.core.problem.Solutions;
import hero.core.problem.Variable;
import hero.unstable.util.classification.ClassifierEvaluator;
import hero.unstable.util.classification.Quantizer;
import hero.core.util.compiler.MyCompiler;
import hero.core.util.compiler.MyLoader;
import hero.core.util.logger.HeroLogger;
import hero.unstable.util.Maths;


public class GePdGp extends AbstractProblemGE {
    
    private static final Logger logger = Logger.getLogger(GePdGp.class.getName());
    private static boolean whoWas = false;
    
    private static int CURRENT_THREAD_ID = 1;
    protected int threadId;
    protected MyCompiler compiler;
    protected DataTable dataTable = null;
    protected AbstractPopPredictor predictor;
    protected Properties properties;
    protected AbstractPopEvaluator evaluator;
    protected ClassifierEvaluator classifierEval;
    protected Quantizer classifier;
    protected String kindClassifier;
    protected int[] selectedSamples;
    protected int pdLevelCol;
    protected int IDCol;
    
    protected double[][] resultsMatrix;
    protected double[][] bestResultsMatrix;
    
    protected double bestClassRate = Double.NEGATIVE_INFINITY;
    protected double bestMacroAvgTPR = Double.NEGATIVE_INFINITY;
    protected double bestMacroAvgTNR = Double.NEGATIVE_INFINITY;
    protected double bestMacroAvgF = Double.NEGATIVE_INFINITY;
    protected double bestMacroAvgPPV = Double.NEGATIVE_INFINITY;
    protected String bestExpression = null;
    protected Solution<Variable<Integer>> bestSolution = null;
    protected int bestSolIdx;
    protected int bestNumGeneration;
    
    @Override
    public GePdGp clone() {
        GePdGp clone = null;
        try {
            clone = new GePdGp(properties);
            clone.dataTable = this.dataTable;
            clone.selectedSamples = this.selectedSamples;
            clone.pdLevelCol = this.pdLevelCol;
            clone.IDCol = this.IDCol;
            
            clone.bestClassRate = Double.NEGATIVE_INFINITY;
            clone.bestMacroAvgTPR = Double.NEGATIVE_INFINITY;
            clone.bestMacroAvgTNR = Double.NEGATIVE_INFINITY;
            clone.bestMacroAvgF = Double.NEGATIVE_INFINITY;
            clone.bestMacroAvgPPV = Double.NEGATIVE_INFINITY;
            clone.bestExpression = null;
            clone.bestSolution = null;
        } catch (IOException ex) {
            logger.severe(ex.getLocalizedMessage());
        }
        return clone;
    }
    
    public GePdGp(Properties properties) throws IOException {
        super(properties.getProperty("BnfPathFile"), 1);
        this.properties = properties;
        this.threadId = CURRENT_THREAD_ID++;
        compiler = new MyCompiler(properties.getProperty("WorkDir"), properties.getProperty("ClassPathSeparator"));
        
        // Get the classifier and the evaluator of metrics
        kindClassifier = properties.getProperty("Classifier");
        switch (kindClassifier) {
            case "quantizer":
                classifier = new Quantizer(kindClassifier, Integer.valueOf(properties.getProperty("MaxPDLevel")));
                classifierEval = new ClassifierEvaluator(Integer.valueOf(properties.getProperty("MaxPDLevel"))+1);
                break;
            case "dichotomizer":
                classifier = new Quantizer(kindClassifier, 1);
                classifierEval = new ClassifierEvaluator(2);
                break;
        }
    }
    
    
    public void generateCodeAndCompile(Solutions<Variable<Integer>> solutions) throws Exception {
        // Phenotype generation
        ArrayList<String> phenotypes = new ArrayList<>();
        for (Solution<Variable<Integer>> solution : solutions) {
            Phenotype phenotype = super.generatePhenotype(solution);
            if (super.correctSol) {
                phenotypes.add(phenotype.toString());
            } else {
                phenotypes.add("return 0;");
            }
        }
        // Compilation process:
        File file = new File(compiler.getWorkDir() + File.separator + "PopPredictor" + threadId + ".java");
        BufferedWriter writer = new BufferedWriter(new FileWriter(file));
        writer.write(AbstractPopPredictor.generateClassCode(threadId, phenotypes));
        writer.flush();
        writer.close();
        LinkedList<String> filePaths = new LinkedList<>();
        filePaths.add(file.getAbsolutePath());
        boolean sucess = compiler.compile(filePaths);
        if (!sucess) {
            logger.severe("Unable to compile, with errors:");
            logger.severe(compiler.getOutput());
        }
    }
    
    @Override
    public void evaluate(Solutions<Variable<Integer>> solutions) {
        try {
            this.generateCodeAndCompile(solutions);
            // And now we evaluate all the solutions with the compiled file:
            predictor = (AbstractPopPredictor) (new MyLoader(compiler.getWorkDir())).loadClass("PopPredictor" + threadId).newInstance();
            
            // For each solution
            for (int s = 0; s < solutions.size(); ++s) {
    
                Solution<Variable<Integer>> solution = solutions.get(s);
                classifierEval.resetConfusionMatrix();
                //logger.info("Solución: " + generatePhenotype(solution).toString());
                
                computeFolds(evaluator, solution, s, selectedSamples);
                
                double cr = classifierEval.getClassificationRate();
                double macroPPV = classifierEval.getMacroAveragePrecision();
                double macroTPR = classifierEval.getMacroAverageSensitivity();
                double macroTNR = classifierEval.getMacroAverageSpecificity();
                double macroFvalue = classifierEval.getMacroFValue();
                
                // Return the value to the algorithm:
                solution.getObjectives().set(0, 1-macroFvalue); //(1-macroFvalue) to maximize the F-value
                
                if (macroFvalue > bestMacroAvgF) {
                    bestSolution = solution;
                    bestSolIdx = s;
                    bestExpression = generatePhenotype(solution).toString();
                    bestClassRate = cr;
                    bestMacroAvgTPR = macroTPR;
                    bestMacroAvgTNR = macroTNR;
                    bestMacroAvgPPV = macroPPV;
                    bestMacroAvgF = macroFvalue;
                    logger.info("BEST FOUND, Thread-Id: " + threadId + ", Macro F-value=" + (100*macroFvalue) + "; Expresion=" + bestExpression);
                    
                    bestResultsMatrix = resultsMatrix;
                }
            }

            
            
            // Josele
            //for (int i = 0; i < solutions.size(); ++i) {
            //    predictor.updatePredictor(dataTable, i);
            //    //double fit = dataTable.computeFIT();
            //    double fit = 0.0;
            //    for(double[] row : dataTable.getFeaturesTable("table")) {
            //        if(row[0]!=row[dataTable.getPredictorColumn()]) {
            //            fit++;
            //        }
            //    }
            //    if (fit < bestFitness) {
            //        bestFitness = fit;
            //       LOGGER.info("Best FIT=" + Math.round(100 * (1 - bestFitness)) + "%");
            //    }
            //    solutions.get(i).getObjectives().set(0, fit);
            //}
            //// end Josele
            
            
        } catch (Exception ex) {
            Logger.getLogger(GePdGp.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    @Override
    public void evaluate(Solution<Variable<Integer>> solution, Phenotype phenotype) {
        logger.severe("The solutions should be already evaluated. You should not see this message.");
    }
        
    
    
    public void computeFolds(AbstractPopEvaluator evaluator, Solution<Variable<Integer>> solution, int solIdx, int[] data) {
        predictor.updatePredictor(dataTable, solIdx);
        int it = 0;
        resultsMatrix = new double[data.length][2];
        
        // For every patient apply the solution
        for (int j = 0; j < data.length; j++) {
            int p = data[j];
            
            //System.out.println("Patient: GA" + (int)dataTable.table.get(p)[IDCol]);
            evaluator.setDataTable((ArrayList<double[]>) dataTable.getFeaturesTable(p, p+1));
            
            // Compute and classify GE:
            int resultGE = (int)dataTable.getFeaturesTable("table").get(0)[0];
            int originalValue = (int)dataTable.getFeaturesTable("table").get(p)[dataTable.getPredictorColumn()];
            
            resultsMatrix[it][0] = resultGE;
            resultsMatrix[it++][1] = originalValue;
            int qResult = 0;
            
            if (!Double.isNaN(resultGE)){
                qResult = resultGE;
            } else {
                // Store as a misclassification (max difference). This
                // happens mostly when an exercise is not available for a patient.
                if (originalValue == 1) {
                    qResult = 0;
                } else {
                    qResult = 1;
                }
                if (whoWas) {
                    logger.info("NaN result for patient GA" +  (int)dataTable.getFeaturesTable("table").get(p)[IDCol]);
                }
            }
            classifierEval.setValue(originalValue, qResult, 1);
            
            if ((originalValue != qResult) && whoWas) {
                logger.info("Misclassification of patient GA" +  (int)dataTable.getFeaturesTable("table").get(p)[IDCol] + " . Original: " + originalValue + ", resultGE: " + qResult);
            }
        }
        
    }
    
    
    public static Properties loadProperties(String propertiesFilePath) {
        Properties properties = new Properties();
        try {
            properties.load(new BufferedReader(new FileReader(new File(propertiesFilePath))));
            File clsDir = new File(properties.getProperty("WorkDir"));
            URLClassLoader sysloader = (URLClassLoader) ClassLoader.getSystemClassLoader();
            Class<URLClassLoader> sysclass = URLClassLoader.class;
            Method method = sysclass.getDeclaredMethod("addURL", new Class[]{URL.class});
            method.setAccessible(true);
            method.invoke(sysloader, new Object[]{clsDir.toURI().toURL()});
        } catch (Exception ex) {
            logger.severe(ex.getLocalizedMessage());
        }
        return properties;
    }
    
    public int[][] getTrainingFolds(int[][] patientsIdxs, int currentFolding) {
        int[][] trainingFolds = new int[patientsIdxs.length-1][patientsIdxs[0].length];
        
        // Leave one group out
        int rNor = 0;
        for (int r=0; r<patientsIdxs.length; r++) {
            if ( r != currentFolding) {
                trainingFolds[rNor++] = patientsIdxs[r];
            }
        }
        return trainingFolds;
    }
    
    public int[][] getValidationFold(int[][] patientsIdxs, int currentFolding) throws IOException {
        int[][] validationFold = new int[1][patientsIdxs[0].length];
        if (properties.getProperty("readExternalFile").equals("yes")){
            validationFold = dataTable.getPatientsIdXs(properties.getProperty("validationFile"));
        } else {
            validationFold[0] = patientsIdxs[currentFolding];
        }
        return validationFold;
    }
    
    
    public void loadData(String type) throws IOException{
        dataTable = new DataTable(properties, type, Integer.valueOf(properties.getProperty("IdxBegin", "-1")), Integer.valueOf(properties.getProperty("IdxEnd", "-1")));
        // Get the clinical information
        pdLevelCol = Integer.valueOf(properties.getProperty("PDLevelCol"));
        IDCol = Integer.valueOf(properties.getProperty("IDCol"));
    }
    
    public static void main(String[] args) {
        String propertiesFilePath = "test" + File.separator + "pd" + File.separator + GePdGp.class.getSimpleName() + ".properties";
        
        if (args.length == 1) {
            propertiesFilePath = args[0];
        } else if (args.length >= 2) {
            propertiesFilePath = args[0];
        }
        
        try {
            // TRAINING
            Properties properties = loadProperties(propertiesFilePath);
            HeroLogger.setup(properties.getProperty("LoggerBasePath") + ".log", Level.parse(properties.getProperty("LoggerLevel")));
            
            /////////////////////////////////////////
            // Variables to store the results:
            double[] classRateAllFolds = new double[Integer.valueOf(properties.getProperty("N"))];
            double[] macroSensitivityAllFolds = new double[Integer.valueOf(properties.getProperty("N"))];
            double[] macroSpecificityAllFolds = new double[Integer.valueOf(properties.getProperty("N"))];
            double[] macroPrecisionAllFolds = new double[Integer.valueOf(properties.getProperty("N"))];
            String[] expressionAllFolds = new String[Integer.valueOf(properties.getProperty("N"))];
            double[] macroFValueAllFolds = new double[Integer.valueOf(properties.getProperty("N"))];
            double[] sensitivityAllFolds = new double[Integer.valueOf(properties.getProperty("N"))];
            double[] specificityAllFolds = new double[Integer.valueOf(properties.getProperty("N"))];
            double[] precisionAllFolds = new double[Integer.valueOf(properties.getProperty("N"))];
            double[] fValueAllFolds = new double[Integer.valueOf(properties.getProperty("N"))];
            
            GePdGp problem;
            // If N-fold cross-validation: first run it and calculate metrics.
            if ("yes".equals(properties.getProperty("NFoldCrossVal"))) {
                // For each fold
                for (int i=0; i<Integer.valueOf(properties.getProperty("N")); i++){
                    if (Integer.valueOf(properties.getProperty("N")) > 1){
                        logger.info("Starting Folding Num: " + i);
                    } else {
                        logger.info("Starting the only fold...");
                    }
                    // New problem and new algorihm for each fold:
                    problem = new GePdGp(properties);
                    problem.loadData("training");
                    
                    // Select the current fold
                    problem.selectedSamples = problem.dataTable.getPatientsIdXs("training", i, true);
                    
                    IntegerFlipMutation<Variable<Integer>> mutationOperator = new IntegerFlipMutation<>(problem, 1.0 / problem.reader.getRules().size());
                    SinglePointCrossover<Variable<Integer>> crossoverOperator = new SinglePointCrossover<>(problem, SinglePointCrossover.DEFAULT_FIXED_CROSSOVER_POINT, SinglePointCrossover.DEFAULT_PROBABILITY, SinglePointCrossover.AVOID_REPETITION_IN_FRONT);
                    SimpleDominance<Variable<Integer>> comparator = new SimpleDominance<>();
                    BinaryTournament<Variable<Integer>> selectionOp = new BinaryTournament<>(comparator);
                    SimpleGeneticAlgorithm<Variable<Integer>> algorithm = new SimpleGeneticAlgorithm<>(problem, Integer.valueOf(properties.getProperty("NumIndividuals")), Integer.valueOf(properties.getProperty("NumGenerations")), true, mutationOperator, crossoverOperator, selectionOp);
                    
                    // Call optimization problem:
                    Solutions<Variable<Integer>> popAfterExecution = new Solutions<>();
                    switch (properties.getProperty("Parallelization")) {
                        case "yes":
                            MasterWorkerThreads<Variable<Integer>> masterWorker = new MasterWorkerThreads<>(algorithm, problem, Integer.valueOf(properties.getProperty("NumCores")));
                            popAfterExecution = masterWorker.execute();
                            break;
                        default:
                            algorithm.initialize();
                            popAfterExecution = algorithm.execute();
                    }
                    
                    // Take the best of all threads:
                    Solution<Variable<Integer>> bestSolution = popAfterExecution.get(0);
                    
                    // Reset everything:
                    problem.classifierEval.resetConfusionMatrix();
                    problem.bestClassRate = Double.NEGATIVE_INFINITY;
                    problem.bestMacroAvgTPR = Double.NEGATIVE_INFINITY;
                    problem.bestMacroAvgTNR = Double.NEGATIVE_INFINITY;
                    problem.bestMacroAvgF = Double.NEGATIVE_INFINITY;
                    problem.bestMacroAvgPPV = Double.NEGATIVE_INFINITY;
                    problem.bestResultsMatrix = null;
                    
                    // Validate the best function with the holded fold
                    // This is the result of the training of this folder
                    problem.selectedSamples = problem.dataTable.getPatientsIdXs("validation", i, true);
                    
                    // Track misclassifications:
                    whoWas = true;
                    
                    // Evaluate the hold folding with the best solution found (each thread):
                    Solutions<Variable<Integer>> tempSolutions = new Solutions<>();
                    tempSolutions.add(bestSolution);
                    problem.evaluate(tempSolutions);
                    
                    // Store the result of the training with this fold:
                    macroFValueAllFolds[i] = problem.classifierEval.getMacroFValue();
                    classRateAllFolds[i] = problem.classifierEval.getClassificationRate();
                    macroSensitivityAllFolds[i] = problem.classifierEval.getMacroAverageSensitivity();
                    macroSpecificityAllFolds[i] = problem.classifierEval.getMacroAverageSpecificity();
                    macroPrecisionAllFolds[i] = problem.classifierEval.getMacroAveragePrecision();
                    expressionAllFolds[i] = problem.generatePhenotype(bestSolution).toString();
                    fValueAllFolds[i] = problem.classifierEval.getFValue(1);
                    sensitivityAllFolds[i] = problem.classifierEval.getSensitivity(1);
                    specificityAllFolds[i] = problem.classifierEval.getSpecificity(1);
                    precisionAllFolds[i] = problem.classifierEval.getPrecision(1);
                    
                    logger.info("validationOfFold,averageAllClasses," + i + "," + (100*macroFValueAllFolds[i]) + "," + (100*classRateAllFolds[i]) +  "," + (100*macroPrecisionAllFolds[i]) + "," + 100*(macroSensitivityAllFolds[i]));
                    logger.info("validationOfFold,averageClass1," + i + "," + (100*fValueAllFolds[i]) + "," + (100*classRateAllFolds[i]) +  "," + (100*precisionAllFolds[i]) + "," + 100*(sensitivityAllFolds[i]));
                    
                    // Print the confussion matrix
                    int[][] cf = problem.classifierEval.getConfusionMatrix();
                    logger.info("Confussion Matrix:");
                    logger.info("     |F|T|");
                    logger.info("     |---|");
                    logger.info("F_GE |" + cf[0][0] + "|" + cf[0][1] + "|");
                    logger.info("     |---|");
                    logger.info("T_GE |" + cf[1][0] + "|" + cf[1][1] + "|");
                    logger.info("     |---|");
                    
                    whoWas = false;
                }
                // Finally calculate the final expression, result of training (OUT OF THE IF)
                
                // Get metrics from training:
                logger.info("TRAINING,averageAllClasses," + (100*Maths.mean(macroFValueAllFolds)) + "," + (100*Maths.std(macroFValueAllFolds)) + "," + (100*Maths.mean(classRateAllFolds)) + "," + (100*Maths.std(classRateAllFolds)) + "," + (100*Maths.mean(macroSensitivityAllFolds)) +  "," + (100*Maths.std(macroSensitivityAllFolds)) + "," + (100*Maths.mean(macroSpecificityAllFolds)) + "," + (100*Maths.std(macroSpecificityAllFolds)) + "," + (100*Maths.mean(macroPrecisionAllFolds)) + "," + (100*Maths.std(macroPrecisionAllFolds)));
                logger.info("TRAINING,averageClass1," + (100*Maths.mean(fValueAllFolds)) + "," + (100*Maths.std(fValueAllFolds)) + "," + (100*Maths.mean(classRateAllFolds)) + "," + (100*Maths.std(classRateAllFolds)) + "," + (100*Maths.mean(sensitivityAllFolds)) +  "," + (100*Maths.std(sensitivityAllFolds)) + "," + (100*Maths.mean(specificityAllFolds)) + "," + (100*Maths.std(specificityAllFolds)) + "," + (100*Maths.mean(precisionAllFolds)) + "," + (100*Maths.std(precisionAllFolds)));
            }
            
            // FINAL TRAINING. Use all the data:
            if (properties.getProperty("trainingAllPatients").equals("yes")) {
                
                // New problem and new algorihm to compute all the patients:
                problem = new GePdGp(properties);
                problem.loadData("training");
                problem.classifierEval.resetConfusionMatrix();
                
                // Track misclassifications:
                whoWas = false;
                
                // Select all the patients:        
                problem.selectedSamples = problem.dataTable.getPatientsIdXs("", -1, false);

                
                IntegerFlipMutation<Variable<Integer>> mutationOperator = new IntegerFlipMutation<>(problem, 1.0 / problem.reader.getRules().size());
                SinglePointCrossover<Variable<Integer>> crossoverOperator = new SinglePointCrossover<>(problem, SinglePointCrossover.DEFAULT_FIXED_CROSSOVER_POINT, SinglePointCrossover.DEFAULT_PROBABILITY, SinglePointCrossover.AVOID_REPETITION_IN_FRONT);
                SimpleDominance<Variable<Integer>> comparator = new SimpleDominance<>();
                BinaryTournament<Variable<Integer>> selectionOp = new BinaryTournament<>(comparator);
                SimpleGeneticAlgorithm<Variable<Integer>> algorithm = new SimpleGeneticAlgorithm<>(problem, Integer.valueOf(properties.getProperty("NumIndividuals")), Integer.valueOf(properties.getProperty("NumGenerations")), true, mutationOperator, crossoverOperator, selectionOp);
                
                // Call optimization problem:
                Solutions<Variable<Integer>> popAfterExecution = new Solutions<>();
                
                switch (properties.getProperty("Parallelization")) {
                    case "yes":
                        MasterWorkerThreads<Variable<Integer>> masterWorker = new MasterWorkerThreads<>(algorithm, problem, Integer.valueOf(properties.getProperty("NumCores")));
                        popAfterExecution = masterWorker.execute();
                        break;
                    default:
                        algorithm.initialize();
                        popAfterExecution = algorithm.execute();
                }
                
                // Take the best solution:
                Solution<Variable<Integer>> bestSolution = popAfterExecution.get(0);
                String bestExpression = problem.generatePhenotype(bestSolution).toString();
                
                // Evaluate the best solution found (all threads):
                Solutions<Variable<Integer>> tempSolutions = new Solutions<>();
                tempSolutions.add(bestSolution);
                problem.evaluate(tempSolutions);
                
                logger.info("Final Training...");
                switch (problem.kindClassifier) {
                    case "dichotomizer":
                        // Print the confussion matrix
                        int[][] cf = problem.classifierEval.getConfusionMatrix();
                        logger.info("Confussion Matrix:");
                        logger.info("     |F|T|");
                        logger.info("     |---|");
                        logger.info("F_GE |" + cf[0][0] + "|" + cf[0][1] + "|");
                        logger.info("     |---|");
                        logger.info("T_GE |" + cf[1][0] + "|" + cf[1][1] + "|");
                        logger.info("     |---|");
                        logger.info("FINAL_TRAINING,PD class 1," + (100*problem.classifierEval.getFValue(1)) + "," + (100*problem.classifierEval.getClassificationRate()) +  "," + (100*problem.classifierEval.getPrecision(1)) + "," + 100*(problem.classifierEval.getSensitivity(1)) + "," + 100*(problem.classifierEval.getSpecificity(1)));
                        break;
                }
                logger.info("FINAL_TRAINING,All classes," + (100*problem.classifierEval.getMacroFValue()) + "," + (100*problem.classifierEval.getClassificationRate()) +  "," + (100*problem.classifierEval.getMacroAveragePrecision()) + "," + 100*(problem.classifierEval.getMacroAverageSensitivity()) + "," + 100*(problem.classifierEval.getMacroAverageSpecificity()) + "," + bestExpression);
                logger.info("...final training done");
                
                /* Print resutlsGE VS real values of the training*/
                //logger.info("resultsGE,realValue");
                //for (int y=0; y<problem.bestResultsMatrix.length; y++){
                //    logger.info(problem.bestResultsMatrix[y][0] + "," + problem.bestResultsMatrix[y][1]);
                ///}
                
                ////////////////////////////////////////////////////////////////////
                // TEST
                // Take the solution found with all the patients. Evaluate over the test data-set:
                logger.info("TEST:");
                
                // Track misclassifications:
                whoWas = false;
                
                problem = new GePdGp(properties);
                problem.loadData("test");
                problem.classifierEval.resetConfusionMatrix();
                
                // Select all the patients:
                problem.selectedSamples = problem.dataTable.getPatientsIdXs("", -1, false);
                
                // Evaluate the best solution found (all threads):
                problem.evaluate(tempSolutions);
                
                
                switch (problem.kindClassifier) {
                    case "dichotomizer":
                        // Print the confussion matrix
                        int[][] cf = problem.classifierEval.getConfusionMatrix();
                        logger.info("Confussion Matrix:");
                        logger.info("     |F|T|");
                        logger.info("     |---|");
                        logger.info("F_GE |" + cf[0][0] + "|" + cf[0][1] + "|");
                        logger.info("     |---|");
                        logger.info("T_GE |" + cf[1][0] + "|" + cf[1][1] + "|");
                        logger.info("     |---|");
                        logger.info("FINAL_TEST,PD class 1," + (100*problem.classifierEval.getFValue(1)) + "," + (100*problem.classifierEval.getClassificationRate()) +  "," + (100*problem.classifierEval.getPrecision(1)) + "," + 100*(problem.classifierEval.getSensitivity(1)) + "," + 100*(problem.classifierEval.getSpecificity(1)));
                        break;
                }
                logger.info("FINAL_TEST,All classes," + (100*problem.classifierEval.getMacroFValue()) + "," + (100*problem.classifierEval.getClassificationRate()) +  "," + (100*problem.classifierEval.getMacroAveragePrecision()) + "," + 100*(problem.classifierEval.getMacroAverageSensitivity()) + "," + 100*(problem.classifierEval.getMacroAverageSpecificity()) + "," + bestExpression);
            }
        } catch (IOException ex) {
            Logger.getLogger(GePdGp.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
