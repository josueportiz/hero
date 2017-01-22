package hero.unstable.test.migraine;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import hero.core.operator.evaluator.AbstractPopEvaluator;
import hero.unstable.util.NormalizedDataTable;
import hero.core.util.logger.HeroLogger;

/**
 *
 * @author José Luis Risco Martín <jlrisco at ucm.es>
 */
public class PopEvaluatorTester {

    private static final Logger logger = Logger.getLogger(PopEvaluatorTester.class.getName());

    protected AbstractPopEvaluator evaluator = null;
    protected NormalizedDataTable dataTable = null;
    protected int numIndividuals = 0;

    public PopEvaluatorTester(String trainingPath, AbstractPopEvaluator evaluator, int numIndividuals) throws IOException {
        this.dataTable = new NormalizedDataTable(null, trainingPath, false, -1, -1);
        this.evaluator = evaluator;
        this.numIndividuals = numIndividuals;
    }

    public void evaluate() {
        evaluator.setDataTable(dataTable.getTrainingTable());
        for (int i = 0; i < numIndividuals; ++i) {
            double fitness = dataTable.computeFitness(evaluator, i);
            logger.info("Fitness of individual " + i + "=" + fitness);
        }
    }

    public static void main(String[] args) {
        HeroLogger.setup(Level.INFO);
        try {
            PopEvaluatorTester popEvaluatorTester = new PopEvaluatorTester("/home/jlrisco/Trabajo/Investigación/Artículos/WorkingPapers/2016-gecco-migraine/Data/Training/PatientA-12.csv",  null, 250);
            popEvaluatorTester.evaluate();
        } catch (IOException ex) {
            Logger.getLogger(PopEvaluatorTester.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
