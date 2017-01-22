/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package hero.unstable.util.classification;

import hero.unstable.test.parkinson.ClusteringBinaryPD;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import weka.core.Instances;
import weka.core.converters.ConverterUtils;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.Remove;
import weka.filters.unsupervised.instance.RemovePercentage;

/**
 *
 * @author josueportiz
 */
public class wekaData {
    protected Instances dataTraining = null;
    protected Instances dataTest = null;
    protected Instances dataOriginal = null;
    private final Logger logger;

    public wekaData(String dataPath, double percentageClaseControl, int classIdx, Logger logger){
        this.logger = logger;
        setData(dataPath, percentageClaseControl, classIdx);
    }
     
    
    public Instances getOriginalData(){
        return dataOriginal;
    }

    public Instances getTrainingData(){
        return dataTraining;
    }

    public Instances getTestData(){
        return dataTest;
    }
        
    /**
     * Type of data is: "training" or "test"
     */
    public Instances filterAttributes(String type, List<Integer> attributes){
        // Copy data:        
        Instances auxData = null;
        auxData = (type.equals("training")) ? dataTraining : dataTest;
        
        
        // REMOVE non selected attributes (features)
        String[] options = new String[2];
        options[0] = "-R";                                    // "range"
        options[1] = attributes.toString().replace("[", "").replace("]", "");  //  range of attributes
        Remove remove = new Remove();                         // new instance of filter
        try {
            remove.setOptions(options);                           // set options
            remove.setInputFormat(auxData); // inform filter about dataset **AFTER** setting options
            auxData = Filter.useFilter(auxData, remove); // Data with selected feautres
            
        } catch (Exception ex) {
            logger.info(ClusteringBinaryPD.class.getName());
            ex.printStackTrace();
        }   
        return auxData;
    }
   
    public void setData(String dataPath, double percentageClaseControl, int classIdx){
        // Load data
        //Instances data = IO.csvToInstances(dataPath);
        ConverterUtils.DataSource source = null;
        try {
            source = new ConverterUtils.DataSource(dataPath);
            dataOriginal = source.getDataSet();
        } catch (Exception ex) {
            logger.info(ClusteringBinaryPD.class.getName());
            ex.printStackTrace();
        }
        
        // Set first column as CLASS
        dataOriginal.setClassIndex(classIdx);
        //logger.info("Data correctly loaded from " + dataPath);
        //logger.info("Data filtered: Class is the FIRST column");
        //logger.info("Number of attributes: " + data.numAttributes() );
        //logger.info("Number of instances: " + data.numInstances() );
        // Get TRAINING and TEST sets:
        RemovePercentage splitter = new RemovePercentage();        
        
        try {
            splitter.setInvertSelection(true);
            splitter.setPercentage(percentageClaseControl);
            splitter.setInputFormat(dataOriginal);
            dataTraining = Filter.useFilter(dataOriginal, splitter);
            
            splitter = new RemovePercentage();
            splitter.setPercentage(percentageClaseControl);
            splitter.setInputFormat(dataOriginal);
            dataTest = Filter.useFilter(dataOriginal, splitter); 
        } catch (Exception ex) {
            Logger.getLogger(ClusteringBinaryPD.class.getName()).log(Level.SEVERE, null, ex);
        }         
    }

}
