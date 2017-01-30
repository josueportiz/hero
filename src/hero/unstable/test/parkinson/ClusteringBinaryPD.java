/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package hero.unstable.test.parkinson;

import hero.core.algorithm.metaheuristic.moga.NSGAII;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import hero.core.operator.crossover.SinglePointCrossover;
import hero.core.operator.mutation.BooleanMutation;
import hero.core.operator.selection.BinaryTournamentNSGAII;
import hero.core.problem.Problem;
import hero.core.problem.Solution;
import hero.core.problem.Solutions;
import hero.core.problem.Variable;
import hero.core.util.logger.HeroLogger;
import hero.unstable.util.classification.wekaClassifier;
import hero.unstable.util.MatrixToFile;
import hero.unstable.util.SolutionAsArray;
import hero.unstable.util.classification.wekaData;
import java.util.ArrayList;
import java.util.List;
import weka.classifiers.Evaluation;
import weka.core.Instances;

/**
 *
 * @author Josué Pagán Ortiz <jpagan at ucm.es>
 */
public class ClusteringBinaryPD extends Problem<Variable<Boolean>>  {

    private static final Logger logger = Logger.getLogger(ClusteringBinaryPD.class.getName());
    protected int threadId;
    protected Properties properties;
    protected int idxGeneration = 0;
    static protected SolutionAsArray solutionToArray;
    protected double bestAccuracy = Double.MIN_VALUE;
    protected double numOfGoodSolutions = 1;
    protected wekaClassifier bestClassifier;

    protected wekaClassifier cls;
    protected wekaData data;        
    
    public ClusteringBinaryPD(Properties properties, wekaData data, wekaClassifier cls, int threadId) throws IOException, Exception {
        super(Integer.parseInt(properties.getProperty("NumTotalFeatures")), Integer.parseInt(properties.getProperty("NumOfObjectives")));
        this.properties = properties;
        this.threadId = threadId;       
        
        // Set WEKA data:
        this.data = data;
            
        // Set WEKA classifier:
        this.cls = cls;        
    }

    @Override
    public void evaluate(Solutions<Variable<Boolean>> solutions) {
        idxGeneration++;
        for (int i = 0; i < solutions.size(); i++) {
            Solution<Variable<Boolean>> solution = solutions.get(i);
            
            // Select features:
            List<Integer> remainingFeatures = solutionAsList(solution);
            
            // Filter TRAINING data:
            Instances filteredData = data.filterAttributes("training", remainingFeatures);

            // Evaluate TRAINING:           
            try {
                Evaluation result = cls.classify(filteredData);
                
                // Store metrics of the solution:
                double avgFValue = result.weightedFMeasure();
                double avgTPR = result.weightedPrecision();
                double avgPPV = result.weightedRecall();
                
                solution.getObjectives().set(0, 1-avgFValue);
                //solution.getObjectives().set(1, (double)filteredData.numAttributes());
                
                logger.info("Average F-value = " + avgFValue);
                if (avgFValue > bestAccuracy){
                    logger.info("Best F-value = " + avgFValue + ". Number of features =  " +  filteredData.numAttributes() + ". For gen:" + (idxGeneration-1) + ", solution: " + i + ". avgTPR: " + avgTPR + ", avgPPV: " + avgPPV);
                    logger.info("Number of good solutions found: " + numOfGoodSolutions++);
                    bestAccuracy = avgFValue;
                    bestClassifier = cls;
                    
                    // Measure overfitting
                    logger.info("Overffiting: "+ (avgFValue -
                            cls.classify(data.filterAttributes("test", remainingFeatures)).fMeasure(1)));//weightedFMeasure()));                    
                }            
            } catch (Exception ex) {
                Logger.getLogger(ClusteringBinaryPD.class.getName()).log(Level.SEVERE, null, ex);
            }
        } //END FOR SOLUTIONS        
    }
    
    @Override
    public void evaluate(Solution<Variable<Boolean>> solution) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    
    @Override
    public ClusteringBinaryPD clone() {
        ClusteringBinaryPD clone = null;
        try {
            clone = new ClusteringBinaryPD(properties, data, cls, threadId + 1);
        } catch (Exception ex) {
            Logger.getLogger(ClusteringBinaryPD.class.getName()).log(Level.SEVERE, null, ex);
        }
        return clone;
    }
    
    /**
     * @param solution
     * @return Return solutions as a list
     * WARNING!: DATA DEPENDANT --> First col is CLASS!!
     */
    public static List<Integer> solutionAsList (Solution<Variable<Boolean>> solution){
        ArrayList <Variable<Boolean>> selectedFeatures = solution.getVariables();
        List<Integer> solAsList = new ArrayList<>();
        
        // Put it in an List
        for (int j = 1; j <= selectedFeatures.size(); j++) {
            //System.out.println(Boolean.toString(selectedFeatures.get(j).getValue()));
            if (selectedFeatures.get(j-1).getValue()){
                solAsList.add(j+1);  // First col is CLASS
            }
        }
        return solAsList;
    }
        
      
    @Override
    public Solutions<Variable<Boolean>> newRandomSetOfSolutions(int size) {
      Solutions<Variable<Boolean>> solutions = new Solutions<Variable<Boolean>>();
        for (int i=0; i<size; ++i) {
          Solution<Variable<Boolean>> solution = new Solution<Variable<Boolean>>(numberOfObjectives);
          for (int j = 0; j < numberOfVariables; ++j) {
              boolean value = Math.random() < 0.5;              
              solution.getVariables().add(new Variable<Boolean>(value));
          }
          solutions.add(solution);
        }
        return solutions;
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
    
    public static void main(String[] args) throws Exception {
        String propertiesFilePath = "test" + File.separator + "parkinson" + File.separator + ClusteringBinaryPD.class.getSimpleName() + ".properties";
        
        int threadId = 1;
        if (args.length == 1) {
            propertiesFilePath = args[0];
        } else if (args.length >= 2) {
            propertiesFilePath = args[0];
            threadId = Integer.valueOf(args[1]);
        }
        
        Properties properties = loadProperties(propertiesFilePath);
        
        HeroLogger.setup(properties.getProperty("LoggerBasePath") + "-" + threadId + ".log", Level.parse(properties.getProperty("LoggerLevel")));
        
        // First create the problem
        ClusteringBinaryPD problem = null;
        wekaData data = new wekaData(properties.getProperty("TrainingPath"),
                Double.parseDouble(properties.getProperty("TrainingTestPercentage")),
                Integer.parseInt(properties.getProperty("ClassAttributeColumn")),
                logger);
        
        wekaClassifier cls = new wekaClassifier(properties.getProperty("Classifier"), "",
                Integer.parseInt(properties.getProperty("Seed")), 
                Integer.parseInt(properties.getProperty("CrossValidation")));

        problem = new ClusteringBinaryPD(properties, data, cls, threadId);
        
        // Second create the algorithm
        NSGAII<Variable<Boolean>> nsga2 = new NSGAII<Variable<Boolean>>(problem,
                Integer.valueOf(properties.getProperty("NumIndividuals")),
                Integer.valueOf(properties.getProperty("NumGenerations")), 
                new BooleanMutation<>(1.0 / problem.getNumberOfVariables()), 
                new SinglePointCrossover<>(problem),
                new BinaryTournamentNSGAII<Variable<Boolean>>());
        
        nsga2.initialize();
        Solutions<Variable<Boolean>> solutions = nsga2.execute();
        
        

        String[] typesSols = {"pareto-front"};
        for (String s: typesSols){
            String solutionsCSVFileName = properties.getProperty("SolutionsCSVFileName") +
                    properties.getProperty("Classifier") + "_" + s + "_" + properties.getProperty("NumGenerations") + "g";
            MatrixToFile solutionsAsMatrix = new MatrixToFile(solutionsCSVFileName + ".csv");
            MatrixToFile bestMetricsTrainingAsMatrix = new MatrixToFile(solutionsCSVFileName + "_metrics_train.csv");
            MatrixToFile bestMetricsTestAsMatrix = new MatrixToFile(solutionsCSVFileName + "_metrics_test.csv");
            
            Solutions<Variable<Boolean>> allSolutions = null; 
            if (s.equals("best")){
                /* After everything store all the solutions in the last generation
                File to store the best solutions (individuals): */
                //allSolutions = nsga2.getLastGeneration();
            }
            else {
                allSolutions = solutions; // Pareto-front: non dominated solutions
                logger.info("FOUND " + allSolutions.size() + " NON-DOMINATED SOLUTIONS");
                logger.info("---------------------------------");           
            }
            
            // Declare variables:
            ArrayList <Variable<Boolean>> solutionAsArrayList = null;
            double[] solutionAsArray = null;
            
            for (int i = 0; i < allSolutions.size(); ++i) {
                Solution<Variable<Boolean>> solution = allSolutions.get(i);
                  
                double[][] metricsTrainAsArray = new double[solution.getVariables().size()][10]; // Mix train (first row) and test results (second row)
                double[][] metricsTestAsArray = new double[solution.getVariables().size()][10]; // Mix train (first row) and test results (second row)
                
                // Re-EVALUATE and TEST the BEST solutions to get the results:
                // Select features:
                List<Integer> remainingFeatures = solutionAsList(solution);
                
                
                // Set TRAINING and TEST data:
                Instances filteredTrainData = data.filterAttributes("training", remainingFeatures);
                Instances filteredTestData = data.filterAttributes("test", remainingFeatures);
                
                // Evaluate TRAINING and TRAIN:
                try {
                    // TRAINING data:
                    Evaluation result = cls.classify(filteredTrainData);
                    metricsTrainAsArray[i] = cls.getMetrics(result);
                    
                    // TEST data:
                    result = cls.classify(filteredTestData);
                    metricsTestAsArray[i] = cls.getMetrics(result);
                } catch (Exception ex) {
                    Logger.getLogger(ClusteringBinaryPD.class.getName()).log(Level.SEVERE, null, ex);
                }
                
                // Store the solution and the metrics:
                if (s.equals("pareto-front")){
                    solutionAsArrayList = solution.getVariables();
                    solutionAsArray = new double[solutionAsArrayList.size()];
                    for (int j = 0; j < solutionAsArray.length; j++) {
                        solutionAsArray[j] = solutionAsArrayList.get(j).getValue() ? 1.0 : 0.0;
                    }
                    solutionsAsMatrix.addRow(solutionAsArray);
                    //logger.info("Non-dominated solution " + i + ". Avg. TRAINING F-value = " + (1-solution.getObjective(0)) + ". Number of features = " + solution.getObjective(1));
                    logger.info("Non-dominated solution " + i + ". Avg. TRAINING F-value = " + (1-solution.getObjective(0)));
                }
                bestMetricsTrainingAsMatrix.addRow(metricsTrainAsArray[i]);
                bestMetricsTestAsMatrix.addRow(metricsTestAsArray[i]);
            }
        }
    }
}
    
