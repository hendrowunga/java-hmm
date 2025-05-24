package hmm;

import hmm.algorithms.Algorithms;
import hmm.data.ExperimentData;
import hmm.data.Model;
import hmm.data.PredictionEstimation;
import hmm.estimation.Estimation;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

/*
 * © 2025 hendrowunga, University of Sanata Dharma
 * Created on 5/25/25
 */
//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
public class Main {

    private static void showUsage(String programName) {
        System.err.println("Usage: java " + programName
                + " path_to_model path_to_data");
    }

    private static void printPredictionEstimation(int stateInd,
                                                  PredictionEstimation estimation, // Gunakan kelas dari hmm.hmm.data
                                                  Model model) { // Gunakan kelas dari hmm.hmm.data
        // Pastikan indeks state valid sebelum mencetak nama
        if (model != null && model.stateIndexToName != null && stateInd >= 0 && stateInd < model.stateIndexToName.size()) {
            System.out.println("State " + model.stateIndexToName.get(stateInd)
                    + " => " + estimation.toString());
        } else {
            System.err.println("Warning: Attempted to print hmm.estimation for invalid state index: " + stateInd + ". Model or state names might be uninitialized.");
        }
    }

    public static void main(String[] args) {
        // bagian: periksa argumen
        if (args.length < 2) {
            showUsage("hmm.hmm.Main"); // Gunakan nama kelas utama Java
            System.exit(1); // Keluar dengan kode error
        }

        String modelFilePath = args[0];
        String dataFilePath = args[1];

        // bagian: baca model dan hmm.data
        Model model = new Model(); // Instansiasi kelas dari hmm.hmm.data
        ExperimentData data = new ExperimentData(); // Instansiasi kelas dari hmm.hmm.data

        // Menggunakan try-with-resources untuk memastikan stream ditutup
        try (InputStream modelStream = new FileInputStream(modelFilePath);
             InputStream dataStream = new FileInputStream(dataFilePath)) {

            model.readModel(modelStream); // Panggil method dari objek model
            data.readExperimentData(model, dataStream); // Panggil method dari objek hmm.data

        } catch (FileNotFoundException e) {
            System.err.println("ERROR: File not found. Details: " + e.getMessage());
            System.exit(1);
        } catch (IllegalArgumentException e) {
            System.err.println("ERROR: Problem with model or hmm.data format/content. Details: " + e.getMessage());
            System.exit(1);
        } catch (NoSuchElementException e) {
            // Terjadi jika Scanner tidak bisa menemukan token yang diharapkan (misal, file berakhir tiba-tiba atau format salah)
            System.err.println("ERROR: Unexpected end of file or incorrect format in model/hmm.data file. Details: " + e.getMessage());
            System.err.println("Please check the input file format according to the specification.");
            System.exit(1);
        } catch (IOException e) {
            System.err.println("ERROR: Fatal IO problem while reading files. Details: " + e.getMessage());
            System.exit(1);
        } catch (Exception e) {
            // Menangkap exception lain yang tidak terduga saat membaca file
            System.err.println("ERROR: An unexpected error occurred during file reading. Details: " + e.getMessage());
            e.printStackTrace(); // Cetak stack trace untuk debugging
            System.exit(1);
        }

        // Pastikan model dan hmm.data berhasil dimuat sebelum melanjutkan
        if (model.stateIndexToName == null || model.stateIndexToName.size() < 2 || data.timeStateSymbol == null) {
            System.err.println("ERROR: Model or hmm.data was not loaded correctly or is empty.");
            System.exit(1);
        }

        // Minimum states beyond begin/end is needed for meaningful hmm.estimation output loop
        boolean hasIntermediateStates = (model.stateIndexToName.size() > 2);
        if (!hasIntermediateStates) {
            System.err.println("Warning: Model must contain states beyond 'begin' and 'end' for meaningful hmm.estimation output.");
        }


        // bagian: jalankan dan estimasi prediksi Viterbi
        System.out.println("Viterbi algorithm state prediction estimations:");
        try {
            List<Integer> mostProbableSeq = Algorithms.findMostProbableStateSequence(model, data); // Panggil dari kelas Algorithms

            // Pastikan hasil Viterbi memiliki ukuran yang sama dengan hmm.data, kecuali jika hmm.data kosong
            if (data.timeStateSymbol.size() > 0 && mostProbableSeq.size() != data.timeStateSymbol.size()) {
                System.err.println("Warning: Viterbi algorithm result size mismatch with hmm.data size. Skipping Viterbi hmm.estimation output.");
                // Tidak perlu keluar, biarkan coba Forward-Backward
            } else if (data.timeStateSymbol.size() == 0) {
                System.out.println("No hmm.data to process for Viterbi hmm.estimation.");
            }
            else {
                // Proses estimasi Viterbi jika hasilnya valid
                int[][] confusionMatrixViterbi = Estimation.combineConfusionMatrix(data, mostProbableSeq, model); // Panggil dari kelas Estimation
                List<PredictionEstimation> estimationsViterbi = Estimation.getStatePredictionEstimations(confusionMatrixViterbi); // Panggil dari kelas Estimation

                // Lewati state pertama dan terakhir (begin dan end)
                if (hasIntermediateStates) {
                    for (int i = 1; i < estimationsViterbi.size() - 1; ++i) {
                        printPredictionEstimation(i, estimationsViterbi.get(i), model);
                    }
                }
            }

        } catch (Exception e) {
            System.err.println("ERROR: An error occurred during Viterbi processing or hmm.estimation. Details: " + e.getMessage());
            e.printStackTrace(); // Cetak stack trace untuk debugging
            // Tidak perlu System.exit(1) di sini agar Forward-Backward bisa tetap jalan jika Viterbi error
        }


        System.out.println("\n");

        // bagian: jalankan dan estimasi prediksi Forward-Backward
        System.out.println("Forward-backward algorithm state prediction estimations:");
        try {
            List<List<Map.Entry<Double, Double>>> forwardBackwardProb =
                    Algorithms.calcForwardBackwardProbabilities(model, data); // Panggil dari kelas Algorithms

            // Pastikan hasil FB memiliki ukuran langkah yang sama dengan hmm.data, kecuali jika hmm.data kosong
            if (data.timeStateSymbol.size() > 0 && forwardBackwardProb.size() != data.timeStateSymbol.size()) {
                System.err.println("Warning: Forward-Backward algorithm result size mismatch with hmm.data size. Skipping F-B hmm.estimation output.");
            } else if (data.timeStateSymbol.size() == 0) {
                System.out.println("No hmm.data to process for Forward-Backward hmm.estimation.");
            }
            else {
                // Proses estimasi FB jika hasilnya valid
                List<Integer> mostProbableStates =
                        Estimation.getMostProbableStates(forwardBackwardProb); // Panggil dari kelas Estimation

                // Pastikan hasil mostProbableStates memiliki ukuran langkah yang sama
                if (mostProbableStates.size() != data.timeStateSymbol.size()) {
                    System.err.println("Warning: getMostProbableStates did not produce a sequence matching the hmm.data size. Skipping F-B hmm.estimation output.");
                } else {
                    int[][] confusionMatrixFB = Estimation.combineConfusionMatrix(data, mostProbableStates, model); // Panggil dari kelas Estimation
                    List<PredictionEstimation> estimationsFB = Estimation.getStatePredictionEstimations(confusionMatrixFB); // Panggil dari kelas Estimation

                    // Lewati state pertama dan terakhir (begin dan end)
                    if (hasIntermediateStates) {
                        for (int i = 1; i < estimationsFB.size() - 1; ++i) {
                            printPredictionEstimation(i, estimationsFB.get(i), model);
                        }
                    }
                }
            }


        } catch (Exception e) {
            System.err.println("ERROR: An error occurred during Forward-Backward processing or hmm.estimation. Details: " + e.getMessage());
            e.printStackTrace(); // Cetak stack trace untuk debugging
            System.exit(1); // Keluar jika F-B error, karena biasanya ini yang terakhir
        }

        System.out.println("\n");

        System.exit(0); // Keluar dengan kode sukses jika sampai sini
    }
}