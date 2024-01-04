import weka.classifiers.Evaluation;
import weka.classifiers.functions.LinearRegression;
import weka.classifiers.functions.MultilayerPerceptron;
import weka.classifiers.trees.DecisionStump;
import weka.classifiers.trees.M5P;
import weka.classifiers.trees.RandomForest;
import weka.classifiers.meta.Bagging;
import weka.core.Instances;
import weka.core.converters.CSVLoader;
import weka.classifiers.trees.M5P;

public class RegressionPrediction {

    public static void main(String[] args) throws Exception {
        // Load CSV data
        CSVLoader loader = new CSVLoader();
        loader.setFile(new java.io.File("D:\\Metric-tool\\Metrics\\zookeeper\\AllPredict.csv"));
        Instances data = loader.getDataSet();

        // Select six columns by name
        // Set the class index (index of the target variable column)
        data.setClassIndex(data.numAttributes() - 1);


        // Create instances for each classifier
        RandomForest randomForest = new RandomForest();
        M5P m5p = new M5P();  //
        LinearRegression linearRegression = new LinearRegression();
        MultilayerPerceptron multilayerPerceptron = new MultilayerPerceptron();
        Bagging bagging = new Bagging();

        // Evaluate each classifier using 10-fold cross-validation
        evaluateClassifier(randomForest, data);
        evaluateClassifier(m5p,  data);
        evaluateClassifier(linearRegression, data);
        evaluateClassifier(multilayerPerceptron, data);
        evaluateClassifier(bagging, data);

    }


    private static void evaluateClassifier(weka.classifiers.Classifier classifier, Instances data) throws Exception {
        // Perform 10-fold cross-validation
        Evaluation evaluation = new Evaluation(data);
        evaluation.crossValidateModel(classifier, data, 10, new java.util.Random(1));

        // Output results
        System.out.println("Classifier: " + classifier.getClass().getSimpleName());
        System.out.println("Mean Absolute Error: " + evaluation.meanAbsoluteError());
        System.out.println("Root Mean Squared Error: " + evaluation.rootMeanSquaredError());
        System.out.println("R-squared: " + evaluation.correlationCoefficient());
        System.out.println("-------------------------------------");


        // Additional Output for Numeric Predictions
//        for (int i = 0; i < data.numInstances(); i++) {
//            double trueValue = data.instance(i).classValue();
//            double predictedValue = evaluation.predictions().get(i).predicted();
//            System.out.println("Instance " + i + ": Actual=" + trueValue + ", Predicted=" + predictedValue);
//        }
    }
}
