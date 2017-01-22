/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package hero.unstable.test.migraine;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 *
 * @author josueportiz
 */
public class Optimization {
    Properties properties;
    String optPath = null;
    String program = null;
    String optDataPath = null;
    List<String> commands = new ArrayList<String>();
    ProcessBuilder pb = null;
    
    public Optimization(Properties prop) throws IOException {
        this.properties = prop;
        // Variables 
        optPath = properties.getProperty("OptPath");
        program = properties.getProperty("Optimizer");
        optDataPath = properties.getProperty("OptDataPath");  
        
        // Build command:        
        // Add arguments:
        // This program is a binary, not a bash command. Thus this line:
        // commands.add("/bin/bash");
        // is not necessary        
        commands.add(optPath + program);
        commands.add("dummyFileName.csv");
        commands.add("dummyFileName.csv");
        commands.add("dummyFileName.csv");
                
        // Run macro on target:
        pb = new ProcessBuilder(commands);
        pb.directory(new File(optPath));
        pb.redirectErrorStream(true);
        
        // Set environmental variables:
        Map env  = pb.environment();
        env.put("LD_LIBRARY_PATH", "/usr/local/MATLAB/MATLAB_Runtime/v901/bin/glnxa64/:/usr/local/MATLAB/MATLAB_Runtime/v901/runtime/glnxa64/");
        env.put("XAPPLRESDIR", "/usr/local/MATLAB/MATLAB_Runtime/v901/X11/app-defaults");
        env.put("PATH", "$PATH:$LD_LIBRARY_PATH");
        env.put("PATH", "$PATH:$XAPPLRESDIR");
    }
    
    public double[][] getRanking(String expressionsFile, String fitFile, String fileName) throws IOException, InterruptedException{
        // Variables:
        String file = optDataPath + fileName;
        File f = new File(file);
        double[][] dataTable = new double[Integer.parseUnsignedInt(properties.getProperty("NumIndividuals"))][5];

        // Create command:
        //commands.set(commands.size()-1, commands.get(commands.size()-1) + " " + file); // Always substitute, never add
        commands.set(commands.size()-3, expressionsFile);
        commands.set(commands.size()-2, fitFile);
        commands.set(commands.size()-1, file);

        System.out.println(commands);
        
        // Run code:
        Process process = pb.start();
        
        // Check result:
        if (process.waitFor() == 0) {
            System.out.println("Success!");
            
            // Read file content:
            if (f.exists() && !f.isDirectory()) {
                BufferedReader reader = new BufferedReader(new FileReader(f));
                String line;
                int idxLine = 0;
                
                while ((line = reader.readLine()) != null) {
                    if (line.isEmpty() || line.startsWith("#")) {
                        continue;
                    }
                    String[] parts = line.split(",");
                    
                    for (int j = 0; j < parts.length; ++j) {
                        dataTable[idxLine][j] = Double.valueOf(parts[j]);
                    }
                    idxLine++;
                }
                reader.close();
            }
        }
        else{
            System.out.println("Error! Command: " + commands + " did not work.");
            System.exit(0);
        }
        return dataTable;
    }
}
