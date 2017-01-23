/*
* To change this license header, choose License Headers in Project Properties.
* To change this template file, choose Tools | Templates
* and open the template in the editor.
*/
package hero.unstable.util.classification;

import java.util.Random;
import weka.classifiers.Classifier;
import weka.classifiers.Evaluation;
import weka.classifiers.bayes.BayesNet;
import weka.classifiers.functions.MultilayerPerceptron;
import weka.classifiers.lazy.IBk;
import weka.classifiers.meta.AdaBoostM1;
import weka.classifiers.rules.ZeroR;
import weka.classifiers.trees.J48;
import weka.classifiers.trees.RandomForest;
import weka.core.Instances;

/**
 * @author jpagan@ucm.es
 * @version 1.0
 *
 * Classification using different models. All of the are extracted from WEKA library
 * @see <a href="http://www.cs.waikato.ac.nz/ml/weka/"> WEKA </a>
 */

public class wekaClassifier {
    /**
     * Traning and test sets are necessary for classification and evaluation
     */
    private Instances[] trainingData;
    private Instances[] testData;
    
    private Classifier classifier;
    private String nameClassifier;
    private int seed;
    private int folds;
    
    public wekaClassifier(Classifier classifier){
        this.classifier = classifier;
        this.nameClassifier = classifier.getClass().getName();
        this.seed = 1;
        this.folds = 10;
    }
    
    public wekaClassifier(String nameClassifier, String classifierOpt, int seed, int folds) throws Exception{
        String[] opts = classifierOpt.split(" ");
        this.seed = seed;
        this.folds = folds;
        
        // Create classifier
        if (nameClassifier.equals("AdaBoostM1")){
            this.classifier = new AdaBoostM1();            
        }
        else if (nameClassifier.equals("J48")){
            this.classifier = new AdaBoostM1();            
        }
        else if (nameClassifier.equals("RandomForest")){
            this.classifier = new RandomForest();            
        }
        else if (nameClassifier.equals("Bayes")){
            this.classifier = new BayesNet();            
        }
        else if (nameClassifier.equals("knn")){
            this.classifier = new IBk();            
        }
        else if (nameClassifier.equals("ZeroR")){
            this.classifier = new ZeroR();            
        }
        else if (nameClassifier.equals("NN")){
            this.classifier = new MultilayerPerceptron();            
        }
        else {
            this.classifier = new ZeroR();            
        }

        
        this.nameClassifier = classifier.getClass().getName();
    }
    
    public wekaClassifier(){
        this.classifier = new J48();
        this.nameClassifier = classifier.getClass().getName();
        this.seed = 1;
        this.folds = 10;
    }
    

    /** Result as:
     * [correctClassified, TPR(class True), TPR(class False), avgTPR, PPV(class True), PPV(class False), avgPPV,  Fvalue(class True), Fvalue(class False), avgFvalue]   
     * @param result
     * @return 10 metrics
     */
    public double[] getMetrics(Evaluation result){
        double[] metrics = new double[10];
        
        metrics[0] = result.pctCorrect()/100;
      
        metrics[1] = result.precision(0);
        metrics[2] = result.precision(1);
        metrics[3] = result.weightedPrecision();
        
        metrics[4] = result.recall(0);
        metrics[5] = result.recall(1);
        metrics[6] = result.weightedRecall();

        metrics[7] = result.fMeasure(0);
        metrics[8] = result.fMeasure(1);
        metrics[9] = result.weightedFMeasure();
        
        return metrics;
    }
    
    public Evaluation classify(Instances data) throws Exception{
        data.setClassIndex(0);

        // Randomize data
        Evaluation eval = new Evaluation(data);
        Random rand    = new Random(seed);
       
        // Perform cross-validation
        eval.crossValidateModel(classifier, data, folds, rand);

        // output evaluation
        String result = eval.toClassDetailsString();
        /*
        System.out.println();
        System.out.println("=== Setup ===");
        System.out.println("Clasiffier: " + classifier.toString());
        System.out.println("Dataset: " + data.relationName());
        System.out.println("Folds: " + folds);
        System.out.println("Seed: " + seed);
        System.out.println();
        System.out.println(eval.toSummaryString("=== " + folds + "-fold Cross-validation ===", false));
        */
        //System.out.println(result);
        return eval;
    }
}
