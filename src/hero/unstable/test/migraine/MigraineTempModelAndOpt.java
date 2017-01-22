/*
* To change this license header, choose License Headers in Project Properties.
* To change this template file, choose Tools | Templates
* and open the template in the editor.
*/
package hero.unstable.test.migraine;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.channels.FileChannel;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.LinkedList;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import hero.core.algorithm.metaheuristic.ga.SimpleGeneticAlgorithm;
import hero.core.algorithm.metaheuristic.moge.AbstractProblemGE;
import hero.core.algorithm.metaheuristic.moge.GrammaticalEvolution;
import hero.core.algorithm.metaheuristic.moge.Phenotype;
import hero.core.operator.comparator.SimpleDominance;
import hero.core.operator.crossover.SinglePointCrossover;
import hero.core.operator.evaluator.AbstractPopEvaluator;
import hero.core.operator.mutation.IntegerFlipMutation;
import hero.core.operator.selection.BinaryTournament;
import hero.core.problem.Solution;
import hero.core.problem.Solutions;
import hero.core.problem.Variable;
import hero.unstable.util.NormalizedDataTable;
import hero.core.util.compiler.MyCompiler;
import hero.core.util.compiler.MyLoader;
import hero.core.util.logger.HeroLogger;
import hero.unstable.util.MatrixToFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author José Luis Risco Martín <jlrisco at ucm.es>
 * @author Josué Pagán Ortiz <jpagan at ucm.es>
 */
public class MigraineTempModelAndOpt extends AbstractProblemGE {
    
    private static final Logger logger = Logger.getLogger(MigraineTempModelAndOpt.class.getName());
    protected int threadId;
    protected MyCompiler compiler;
    protected AbstractPopEvaluator evaluator;
    protected NormalizedDataTable dataTable;
    protected Properties properties;
    protected Optimization opt;
    protected int idxGeneration = 0;
    protected double bestFitness = Double.MAX_VALUE;
    
    static protected GrammaticalEvolution algorithm = null;
    
    public MigraineTempModelAndOpt(Properties properties, int threadId) throws IOException {
        super(properties.getProperty("BnfPathFile"), Integer.parseInt(properties.getProperty("NumOfObjectives")));
        this.properties = properties;
        this.threadId = threadId;
        
        compiler = new MyCompiler(properties.getProperty("WorkDir"), properties.getProperty("ClassPathSeparator"));
        dataTable = new NormalizedDataTable(this, properties.getProperty("TrainingPath"), Boolean.valueOf(properties.getProperty("NormalizeTable", "true")), Integer.valueOf(properties.getProperty("IdxBegin", "-1")), Integer.valueOf(properties.getProperty("IdxEnd", "-1")));
    }
    
    @Override
    public void evaluate(Solutions<Variable<Integer>> solutions) {
        String solutionsCSVFileName = "";
        
        if (idxGeneration < Integer.valueOf(properties.getProperty("NumGenerations"))){
            // File to store the solutions (individuals):
            solutionsCSVFileName = properties.getProperty("SolutionsCSVFileName") + "_gen-" + idxGeneration++ + ".csv";
            
            FileChannel fileChannel = null;
            try {
                // Lock the function:
                fileChannel = FileChannel.open(Paths.get(compiler.getWorkDir() + File.separator + "evaluate.txt"), StandardOpenOption.WRITE);
                fileChannel.lock();
            } catch (Exception ex) {
                logger.severe(ex.getLocalizedMessage());
            }
            StringBuilder currentJavaFile = new StringBuilder();
            
            currentJavaFile.append("public class PopEvaluator").append(threadId).append(" extends hero.unstable.test.migraine.PopEvaluator {\n\n");
            
            currentJavaFile.append("\tpublic double evaluate(int idxExpr, int k) {\n");
            currentJavaFile.append("\t\tdouble result = 0.0;\n");
            currentJavaFile.append("\t\ttry {\n");
            
            currentJavaFile.append("\t\t\tswitch(idxExpr) {\n");
            for (int i = 0; i < solutions.size(); ++i) {
                currentJavaFile.append("\t\t\t\tcase ").append(i).append(":\n");
                Solution<Variable<Integer>> solution = solutions.get(i);
                Phenotype phenotype = generatePhenotype(solution);
                if (correctSol) {
                    currentJavaFile.append("\t\t\t\t\tresult = ").append(phenotype.toString()).append(";\n");
                } else {
                    currentJavaFile.append("\t\t\t\t\tresult = Double.POSITIVE_INFINITY;\n");
                }
                currentJavaFile.append("\t\t\t\t\tbreak;\n");
            }
            currentJavaFile.append("\t\t\t\tdefault:\n");
            currentJavaFile.append("\t\t\t\t\tresult = Double.POSITIVE_INFINITY;\n");
            currentJavaFile.append("\t\t\t}\n"); // End switch
            
            currentJavaFile.append("\t\t}\n"); // End try
            currentJavaFile.append("\t\tcatch (Exception ee) {\n");
            currentJavaFile.append("\t\t\t// System.err.println(ee.getLocalizedMessage());\n");
            currentJavaFile.append("\t\t\tresult = Double.POSITIVE_INFINITY;\n");
            currentJavaFile.append("\t\t}\n"); // End catch
            currentJavaFile.append("\t\tif(Double.isNaN(result)) {\n");
            currentJavaFile.append("\t\t\tresult = Double.POSITIVE_INFINITY;\n");
            currentJavaFile.append("\t\t}\n");
            currentJavaFile.append("\t\treturn result;\n");
            currentJavaFile.append("\t}\n");
            currentJavaFile.append("}\n");
            
            // Compilation process:
            try {
                File file = new File(compiler.getWorkDir() + File.separator + "PopEvaluator" + threadId + ".java");
                BufferedWriter writer = new BufferedWriter(new FileWriter(file));
                writer.write(currentJavaFile.toString());
                writer.flush();
                writer.close();
                LinkedList<String> filePaths = new LinkedList<>();
                filePaths.add(file.getAbsolutePath());
                boolean sucess = compiler.compile(filePaths);
                if (!sucess) {
                    logger.severe("Unable to compile, with errors:");
                    logger.severe(compiler.getOutput());
                }
            } catch (Exception ex) {
                logger.severe(ex.getLocalizedMessage());
            }
            // And now we evaluate all the solutions with the compiled file:
            evaluator = null;
            try {
                evaluator = (AbstractPopEvaluator) (new MyLoader(compiler.getWorkDir())).loadClass("PopEvaluator" + threadId).newInstance();
                evaluator.setDataTable(dataTable.getTrainingTable());
            } catch (Exception ex) {
                logger.severe(ex.getLocalizedMessage());
            }
            
            
            // Print solutions (EXPRESSIONS)
            String fileNameExpressions = properties.getProperty("FileNameExpressions");
            String fileNameFit = properties.getProperty("FileNameFit");
            File resultsFileExp = new File(fileNameExpressions);
            File resultsFileFit = new File(fileNameFit);
            
            BufferedWriter writerExpressions = null;
            BufferedWriter writerFit = null;
            
            try {
                writerExpressions = new BufferedWriter(new FileWriter(resultsFileExp, false));
                writerFit = new BufferedWriter(new FileWriter(resultsFileFit, false));
            } catch (IOException ex) {
                Logger.getLogger(MigraineTempModelAndOpt.class.getName()).log(Level.SEVERE, null, ex);
            }
            
            // josueportiz: Get FIT (Objective 0) and store it for each EXPRESSION:
            for (int i = 0; i < solutions.size(); ++i) {
                Solution<Variable<Integer>> solution = solutions.get(i);
                String expression = dataTable.functionAsString(solution);
                double fitness = dataTable.evaluate(evaluator, solution, i);
                
                double energy = energySensors(expression);
                double clkCycles = (double) computeClkCycles(expression);
                if (false) {
                    try {
                        // Save solution and fitness
                        writerExpressions.write(expression + "\n");
                        writerExpressions.flush();
                        
                        // Save fitness and the other objectives
                        writerFit.write(100* (1- fitness) + "," + clkCycles + "," + energy + "\n");
                        writerFit.flush();
                    } catch (IOException ex) {
                        Logger.getLogger(MigraineTempModelAndOpt.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
                if (Double.isNaN(fitness)) {
                    logger.info("I have a NaN number here");
                }
                
                // Store OBJECTIVES
                solution.getObjectives().set(0, fitness);
                solution.getObjectives().set(1, clkCycles);
                solution.getObjectives().set(2, energy);
                if (fitness < bestFitness){
                    logger.info("Best fitness = " + fitness + ". For gen:" + (idxGeneration-1) + ", solution: " + i + "fit=" + (100 * (1 - fitness)) + "; clk= " + clkCycles + "; energy= " + energy + "; Expresion=" + expression);
                    bestFitness = fitness;
                }
                else {
                    logger.info("fit=" + (100 * (1 - fitness)) + "; clk= " + clkCycles + "; energy= " + energy);
                }
            }
            
            try {
                if (fileChannel != null) {
                    System.gc();
                    fileChannel.close();
                }
            } catch (Exception ex) {
                logger.severe(ex.getLocalizedMessage());
            }
        }
        else { // Last generation is not taken into account to get the best solutions
            /* After everything store all the solutions in the last generation
            File to store the best solutions (individuals): */
            
            /* After everything is finished, get the non-dominated pareto front: */
            Solutions<Variable<Integer>> nonDominatedSolutions = algorithm.getCurrentSolution();
            
            // Print them all:
            logger.info("FOUND " + nonDominatedSolutions.size() + " NON-DOMINATED SOLUTIONS");
            logger.info("---------------------------------");
            
            
            /*
            Josue:  these lines (till for loop) are copied from the beginning of the code
            */
            // Print solutions (EXPRESSIONS)
            String fileNameExpressions = properties.getProperty("FileNameExpressions");
            File resultsFileExp = new File(fileNameExpressions);
            
            BufferedWriter writerExpressions = null;
            
            try {
                writerExpressions = new BufferedWriter(new FileWriter(resultsFileExp, false));
            } catch (IOException ex) {
                Logger.getLogger(MigraineTempModelAndOpt.class.getName()).log(Level.SEVERE, null, ex);
            }
              
            for (int i = 0; i < nonDominatedSolutions.size(); i++){
                Solution<Variable<Integer>> solution = nonDominatedSolutions.get(i);
                double fitness = solution.getObjective(0);
                double clkCycles = solution.getObjective(1);
                double energy = solution.getObjective(2);
                
                // Store the solution:
                String text = "Non-dominated solution " + i + ". Accuracy = " + 100*(1-fitness) + ", Clk cycles: " + clkCycles + ", Energy: " + energy + ". Expression: " + dataTable.functionAsString(solution);
                try {                    
                    // Save solution and fitness
                    writerExpressions.write(text + "\n");
                    writerExpressions.flush();
              
                } catch (IOException ex) {
                    Logger.getLogger(MigraineTempModelAndOpt.class.getName()).log(Level.SEVERE, null, ex);
                }
                logger.info(text);
            }
        }
    }
    
    /*
    *    Calculate clock cycles. Count repetitions of mathematical functions.
    */
    public int computeClkCycles(String expression){
        int totalClkCycles = Integer.MAX_VALUE;
        int count = 0;
        int horizons = 0;
        String predHor = properties.getProperty("PredictionHorizon");
        
        // Load my available simple functions and its clock cycles:
        String simpleFunctions = properties.getProperty("MySimpleFunctions");
        String[] simpleParts = simpleFunctions.split(",");
        String mixedFunctions = properties.getProperty("MyMixedFunctions");
        String[] mixedParts = mixedFunctions.split(",");
        
        String clkCyclesSimpleStr = properties.getProperty("MyClkCyclesSimple");
        String[] clkCyclesSimpleParts = clkCyclesSimpleStr.split(",");
        String clkCyclesMixedStr = properties.getProperty("MyClkCyclesMixed");
        String[] clkCyclesMixedParts = clkCyclesMixedStr.split(",");
        
        String[] sensorsIdxsParts = properties.getProperty("SensorsIdxs").split(",");
        
        // Parse expressions firstly:
        String replaceAll = expression.replaceAll("Math.pow\\(10,[+-][0-9]\\)", "pow");
        replaceAll = replaceAll.replaceAll(",k-" + properties.getProperty("PredictionHorizon") + ",[0-9]", "");
        replaceAll = replaceAll.replaceAll("k-", ""); // WARNING: do not forget to SUBSTRACT the PREDICTION HORIZON
        
        // Identify sensors:
        String[] sensorsToProcessParts = properties.getProperty("SensorProcessiongIdx").split(",");
        String[] sensorsToProcessClkParts = properties.getProperty("ClkProcessing").split(",");
        Pattern pS = Pattern.compile("k-[0-9]+,k-");
        Matcher mS = pS.matcher(expression);
        boolean b2 = expression.contains("getVariable(");

        if (mS.find() | b2){ // If there exist one sensor at least
            totalClkCycles = 0;
            
            for (int i = 0; i < sensorsToProcessParts.length; i++) {
                if (expression.contains(",k-" + predHor + "," + sensorsToProcessParts[i]) |
                        expression.contains("getVariable("  + Integer.toString(Integer.valueOf(sensorsToProcessParts[i]) - 1))){
                    totalClkCycles += Integer.valueOf(sensorsToProcessClkParts[i]);
                }
            }
            
            // Identify repetitions:
            int[] clkCyclesSimple = new int[clkCyclesSimpleParts.length];
            int[] clkCyclesMixed = new int[clkCyclesMixedParts.length];
            
            for (int i = 0; i < simpleParts.length; i++) {
                clkCyclesSimple[i] = Integer.valueOf(clkCyclesSimpleParts[i]);
            }
            for (int i = 0; i < mixedParts.length; i++) {
                clkCyclesMixed[i] = Integer.valueOf(clkCyclesMixedParts[i]);
            }
            // Functions witout temporal behavior:
            for (int i = 0; i < simpleParts.length; i++) {
                Pattern p = Pattern.compile(simpleParts[i]);
                Matcher m = p.matcher(replaceAll);
                
                count = 0;
                while(m.find()){
                    count++;
                }
                totalClkCycles += count*clkCyclesSimple[i];
            }
            
            // Functions with temporal behavior:
            for (int i = 0; i < mixedParts.length; i++) {
                Pattern p = Pattern.compile(mixedParts[i] + "\\(-?\\d+");
                Matcher m = p.matcher(replaceAll);
                
                horizons = 0;
                count = 0;
                
                while (m.find()){
                    pS = Pattern.compile("-?\\d+");
                    mS = pS.matcher(m.group());
                    
                    while(mS.find()){
                        horizons += Integer.valueOf(mS.group()) - Integer.valueOf(properties.getProperty("PredictionHorizon"));
                        count++;
                    }
                    
                    if (mixedParts[i].equals("MyAvg")){
                        int posSum = Integer.MIN_VALUE;
                        int posDiv = Integer.MIN_VALUE;
                        
                        for (int j =0; j < simpleParts.length; j++){
                            if (simpleParts[j].equals("\\+")){
                                posSum = j;
                            }
                            else if (simpleParts[j].equals("\\/")){
                                posDiv = j;
                            }
                        }
                        totalClkCycles += horizons*clkCyclesSimple[posSum] + count*clkCyclesSimple[posDiv];
                    }
                    
                    else if (mixedParts[i].equals("MyStd")){
                        int posSum = Integer.MIN_VALUE;
                        int posSub = Integer.MIN_VALUE;
                        int posMul = Integer.MIN_VALUE;
                        int posDiv = Integer.MIN_VALUE;
                        
                        for (int j =0; j < simpleParts.length; j++){
                            if (simpleParts[j].equals("\\+")){
                                posSum = j;
                            }
                            else if (simpleParts[j].equals("\\-")){
                                posSub = j;
                            }
                            else if (simpleParts[j].equals("\\*")){
                                posMul = j;
                            }
                            else if (simpleParts[j].equals("\\/")){
                                posDiv = j;
                            }
                        }
                        // 2*N Sums and N substractions, nad N multiplications and 2 divisions
                        totalClkCycles += 2*horizons*clkCyclesSimple[posSum] +
                                horizons*clkCyclesSimple[posSub] +
                                horizons*clkCyclesSimple[posMul] +
                                2*count*clkCyclesSimple[posDiv];
                    }
                    else {
                        totalClkCycles += horizons*clkCyclesMixed[i];
                    }
                }
            }
        }
        return totalClkCycles;
    }
    
    public double energySensors(String expression){
        double totalEnergy = Double.MAX_VALUE;
        
        // Identify sensors:
        String[] sensorsIdxsParts = properties.getProperty("SensorsIdxs").split(",");
        String predHor = properties.getProperty("PredictionHorizon");
        String[] sensorsConsumptionParts = properties.getProperty("SensorsConsumption").split(",");
       
        boolean b2 = expression.contains("getVariable(");
        Pattern pS = Pattern.compile("k-[0-9]+,k-");
        Matcher mS = pS.matcher(expression);
        
        
        if (mS.find() | b2 ){ // If there exist one sensor at least
             totalEnergy = 0.0;
            
            for (int i = 0; i < sensorsIdxsParts.length; i++){
                if (expression.contains(",k-" + predHor + "," + sensorsIdxsParts[i]) |
                        expression.contains("getVariable("  + Integer.toString(Integer.valueOf(sensorsIdxsParts[i]) - 1))){
                    totalEnergy += Double.valueOf(sensorsConsumptionParts[i]);
                }
            }
        }
        return totalEnergy;
    }
    
    @Override
    public void evaluate(Solution<Variable<Integer>> solution) {
        logger.severe("The solutions should be already evaluated. You should not see this message.");
    }
    
    @Override
    public void evaluate(Solution<Variable<Integer>> solution, Phenotype phenotype) {
        logger.severe("The solutions should be already evaluated. You should not see this message.");
    }
    
    @Override
    public MigraineTempModelAndOpt clone() {
        MigraineTempModelAndOpt clone = null;
        try {
            clone = new MigraineTempModelAndOpt(properties, threadId + 1);
        } catch (IOException ex) {
            logger.severe(ex.getLocalizedMessage());
        }
        return clone;
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
    
    public static void main(String[] args) {
        String propertiesFilePath = "test" + File.separator + "migraine" + File.separator + MigraineTempModelAndOpt.class.getSimpleName() + ".properties";
        
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
        MigraineTempModelAndOpt problem = null;
        try {
            problem = new MigraineTempModelAndOpt(properties, threadId);
        } catch (IOException ex) {
            logger.severe(ex.getLocalizedMessage());
        }
        
        // Second create the algorithm
        IntegerFlipMutation<Variable<Integer>> mutationOperator = new IntegerFlipMutation<>(problem, 1.0 / problem.reader.getRules().size());
        SinglePointCrossover<Variable<Integer>> crossoverOperator = new SinglePointCrossover<>(problem, SinglePointCrossover.DEFAULT_FIXED_CROSSOVER_POINT, SinglePointCrossover.DEFAULT_PROBABILITY, SinglePointCrossover.AVOID_REPETITION_IN_FRONT);
        SimpleDominance<Variable<Integer>> comparator = new SimpleDominance<>();
        
        double probMutation = 1.0 / problem.reader.getRules().size();
        double probCrossOver = SinglePointCrossover.DEFAULT_PROBABILITY;
        algorithm = new GrammaticalEvolution(problem, Integer.valueOf(properties.getProperty("NumIndividuals")), Integer.valueOf(properties.getProperty("NumGenerations")), probMutation, probCrossOver);
        
        
        // OLD ALGORITHM (only GE expression):
        BinaryTournament<Variable<Integer>> selectionOp = new BinaryTournament<>(comparator);
        //SimpleGeneticAlgorithm<Variable<Integer>> algorithm = new SimpleGeneticAlgorithm<>(problem, Integer.valueOf(properties.getProperty("NumIndividuals")), Integer.valueOf(properties.getProperty("NumGenerations")), true, mutationOperator, crossoverOperator, selectionOp);
        algorithm.initialize();
        algorithm.execute();
    }
}
