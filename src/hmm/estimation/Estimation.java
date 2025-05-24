package hmm.estimation;
import hmm.data.ExperimentData;
import hmm.data.Model;
import hmm.data.PredictionEstimation;

import java.util.*;

/*
 * © 2025 hendrowunga, University of Sanata Dharma
 * Created on 5/25/25
 */
public class Estimation {
    /**
     * Menggunakan probabilitas forward-backward untuk mendapatkan state yang paling mungkin di setiap langkah.
     * Ini adalah state dengan probabilitas marginal P(q_t = i | O) tertinggi di setiap langkah t.
     * P(q_t = i | O) sebanding dengan alpha(t, i) * beta(t, i).
     *
     * @param forwardBackwardProb Hasil dari CalcForwardBackwardProbabilities.
     * @return List indeks state paling mungkin di setiap langkah waktu.
     */
    public static List<Integer> getMostProbableStates(List<List<Map.Entry<Double, Double>>> forwardBackwardProb) {
        int maxtime = forwardBackwardProb.size();
        List<Integer> mostProbableStates = new ArrayList<>(maxtime);

        for (int t = 0; t < maxtime; ++t) {
            List<Map.Entry<Double, Double>> stepProbs = forwardBackwardProb.get(t);
            int nstates = stepProbs.size();
            double maxProbProduct = -1.0; // Inisialisasi dengan nilai yang lebih kecil dari produk prob non-nol
            int mostProbableStateIndex = -1;

            // C++ uses std::max_element with a lambda comparing (prev.first * prev.second < next.first * next.second).
            // This finds the index of the element with the maximum product of alpha and beta.
            for (int i = 0; i < nstates; ++i) {
                Map.Entry<Double, Double> alphaBetaPair = stepProbs.get(i);
                double probProduct = alphaBetaPair.getKey() * alphaBetaPair.getValue();

                if (probProduct > maxProbProduct) {
                    maxProbProduct = probProduct;
                    mostProbableStateIndex = i;
                }
            }

            // Jika tidak ada state yang ditemukan (semua probProduct <= 0.0) dan ada hmm.data untuk langkah ini
            if (mostProbableStateIndex == -1 && nstates > 0) {
                // Ini bisa terjadi jika semua probabilitas alpha*beta adalah 0 atau negatif (kasus rare).
                // Mengikuti perilaku std::max_element jika semua sama (misal semua 0), itu akan
                // mengembalikan iterator ke elemen *pertama* yang memiliki nilai maksimum (dalam kasus ini, 0).
                // Jadi, jika semua 0.0, itu akan mengembalikan indeks 0. Mari ikuti ini.
                mostProbableStateIndex = 0; // Pilih state pertama (indeks 0) jika semua produk prob 0 atau negatif
            }


            mostProbableStates.add(mostProbableStateIndex);
        }

        return mostProbableStates;
    }

    /**
     * Menggabungkan hasil prediksi state ke dalam Confusion Matrix.
     * confusionMatrix[i][j] adalah jumlah elemen dengan prediksi state i saat state sebenarnya j.
     *
     * @param realData Data eksperimen asli (dengan state sebenarnya).
     * @param predictedStates List state yang diprediksi (hasil Viterbi atau FB).
     * @param model Model HMM terkait (untuk jumlah state).
     * @return Confusion Matrix (array 2D).
     * @throws IllegalArgumentException Jika ukuran hmm.data asli dan hmm.data prediksi tidak cocok.
     */
    public static int[][] combineConfusionMatrix(ExperimentData realData,
                                                 List<Integer> predictedStates, Model model) {
        int maxtime = predictedStates.size();
        int nstates = model.stateIndexToName.size(); // Gunakan jumlah state dari model
        int[][] confusionMatrix = new int[nstates][nstates]; // Defaultnya 0

        // Cek apakah ukuran hmm.data asli dan prediksi cocok
        if (maxtime != realData.timeStateSymbol.size()) {
            throw new IllegalArgumentException("Predicted states (" + maxtime + " steps) and real hmm.data (" + realData.timeStateSymbol.size() + " steps) must have the same size.");
        }

        for (int t = 0; t < maxtime; ++t) {
            int predictedInd = predictedStates.get(t);
            int realInd = realData.timeStateSymbol.get(t).stateIndex; // Mengakses TimeStateSymbolTuple

            // Cek batas indeks sebelum akses array
            if (predictedInd < 0 || predictedInd >= nstates) {
                throw new IndexOutOfBoundsException("Predicted state index " + predictedInd + " out of bounds [0, " + (nstates - 1) + "] at step " + t);
            }
            if (realInd < 0 || realInd >= nstates) {
                throw new IndexOutOfBoundsException("Real state index " + realInd + " out of bounds [0, " + (nstates - 1) + "] at step " + t);
            }


            confusionMatrix[predictedInd][realInd]++;
        }

        return confusionMatrix;
    }

    /**
     * Menggunakan confusion matrix untuk menghitung estimasi hasil prediksi.
     *
     * @param confusionMatrix Confusion matrix.
     * @return List hasil estimasi prediksi untuk setiap state.
     * @throws IllegalArgumentException Jika confusion matrix bukan matriks persegi.
     */
    public static List<PredictionEstimation> getStatePredictionEstimations(int[][] confusionMatrix) {
        int nstates = confusionMatrix.length;
        if (nstates == 0 || confusionMatrix[0].length != nstates) {
            throw new IllegalArgumentException("Confusion matrix must be a square matrix.");
        }

        List<PredictionEstimation> estimations = new ArrayList<>(nstates);
        int[] colSums = new int[nstates]; // Defaultnya 0
        int[] rowSums = new int[nstates]; // Defaultnya 0

        // bagian: siapkan penjumlahan baris dan kolom
        for (int i = 0; i < nstates; ++i) {
            for (int j = 0; j < nstates; ++j) {
                rowSums[i] += confusionMatrix[i][j];
                colSums[j] += confusionMatrix[i][j]; // Indeks kolom j
            }
        }

        // Total observasi adalah jumlah semua entri di matriks
        int totalObservations = 0;
        for (int sum : rowSums) {
            totalObservations += sum;
        }
        // Alternatif menggunakan streams: totalObservations = IntStream.of(rowSums).sum();


        // bagian: hitung estimasi prediksi untuk setiap state
        for (int state = 0; state < nstates; ++state) {
            PredictionEstimation estimation = new PredictionEstimation();
            estimation.truePositives = confusionMatrix[state][state];
            estimation.falsePositives = rowSums[state] - confusionMatrix[state][state];
            estimation.falseNegatives = colSums[state] - confusionMatrix[state][state];

            // True Negatives: Total observasi dikurangi semua yang terkait dengan state ini
            // (TP + FP + FN)
            // Total - (jumlah baris 'state' + jumlah kolom 'state' - TP_state)
            // Total - rowSums[state] - colSums[state] + confusionMatrix[state][state]
            estimation.trueNegatives = totalObservations - estimation.truePositives - estimation.falsePositives - estimation.falseNegatives;


            // hitung f-measure
            double precision = 0.0;
            double recall = 0.0;

            if (rowSums[state] != 0) {
                precision = (double) estimation.truePositives / rowSums[state];
            }

            if (colSums[state] != 0) {
                recall = (double) estimation.truePositives / colSums[state];
            }

            // Hindari pembagian dengan nol untuk f-measure
            if (precision + recall == 0.0) {
                estimation.fMeasure = 0.0;
            } else {
                estimation.fMeasure = 2.0 * (precision * recall) / (precision + recall);
            }


            estimations.add(estimation);
        }

        return estimations;
    }
}