package hmm.data;

/*
 * © 2025 hendrowunga, University of Sanata Dharma
 * Created on 5/25/25
 */
public class PredictionEstimation {
    public int truePositives;
    public int falsePositives;
    public int trueNegatives;
    public int falseNegatives;
    public double fMeasure;

    @Override
    public String toString() {
        return "True Positives=" + truePositives +
                ", False Positives=" + falsePositives +
                ", True Negatives=" + trueNegatives +
                ", False Negatives=" + falseNegatives +
                ", f-measure=" + String.format("%.4f", fMeasure); // Format untuk 4 desimal
    }
}