package hmm.data;

import java.util.*;
import java.io.InputStream;
import java.io.IOException;

/*
 * © 2025 hendrowunga, University of Sanata Dharma
 * Created on 5/25/25
 */
public class Model {
    /// jumlah simbol emisi yang berbeda
    public int alphabetSize;

    /// konversi nama state (string) ke indeks (integer)
    public Map<String, Integer> stateNameToIndex;

    /// konversi indeks state ke nama state
    public List<String> stateIndexToName;

    /// element[i][j] adalah probabilitas transisi dari state i ke state j.
    /// State pertama adalah 'begin', state terakhir adalah 'end'.
    public double[][] transitionProb;

    /// element[i][j] adalah probabilitas emisi simbol j dari state i.
    public double[][] stateSymbolProb;

    /**
     * Membaca deskripsi model dari stream.
     * Asumsi stream memiliki format yang benar.
     * Menggunakan Scanner untuk kemudahan parsing token.
     *
     * @param modelSource InputStream untuk membaca model.
     * @throws IOException Jika terjadi masalah I/O saat membaca file.
     * @throws IllegalArgumentException Jika hmm.data model tidak valid (misal: kurang dari 2 state).
     * @throws NoSuchElementException Jika format file tidak sesuai dan Scanner tidak bisa menemukan token yang diharapkan.
     */
    public void readModel(InputStream modelSource) throws IOException, IllegalArgumentException, NoSuchElementException {
        Scanner scanner = new Scanner(modelSource);
        // Menggunakan try-finally jika Scanner tidak dibuat dalam try-with-resources di luar
        // Untuk kesederhanaan porting, kita asumsikan Scanner dibuat di sini dan tidak perlu ditutup di sini,
        // karena InputStream dasarnya dikelola oleh pemanggil.
        // Dalam aplikasi nyata, menggunakan try-with-resources untuk Scanner di method main lebih baik.

        // bagian: membaca states
        int nstates = scanner.nextInt();

        if (nstates < 2) {
            throw new IllegalArgumentException("There must be at least two states: begin and end");
        }

        stateNameToIndex = new HashMap<>();
        stateIndexToName = new ArrayList<>();
        for (int i = 0; i < nstates; ++i) {
            String stateName = scanner.next();
            stateNameToIndex.put(stateName, i);
            stateIndexToName.add(stateName);
        }

        // bagian: membaca alphabet
        alphabetSize = scanner.nextInt();

        // bagian: membaca transisi
        int ntransitions = scanner.nextInt();
        transitionProb = new double[nstates][nstates]; // Defaultnya 0.0

        for (int i = 0; i < ntransitions; ++i) {
            String stateName = scanner.next();
            String targetStateName = scanner.next();
            double prob = scanner.nextDouble();

            Integer fromInd = stateNameToIndex.get(stateName);
            Integer toInd = stateNameToIndex.get(targetStateName);

            if (fromInd == null || toInd == null) {
                throw new IllegalArgumentException("Unknown state name in transitions: " + stateName + " or " + targetStateName);
            }

            if (fromInd + 1 == nstates) {
                throw new IllegalArgumentException("Transition from the ending state is forbidden (state: " + stateName + ")");
            }

            if (toInd == 0) {
                throw new IllegalArgumentException("Transition to the starting state is forbidden (state: " + targetStateName + ")");
            }

            transitionProb[fromInd][toInd] = prob;
        }

        // bagian: membaca probabilitas emisi state-simbol
        int nemissions = scanner.nextInt();
        stateSymbolProb = new double[nstates][alphabetSize]; // Defaultnya 0.0

        for (int i = 0; i < nemissions; ++i) {
            String stateName = scanner.next();
            String symbol = scanner.next(); // Menggunakan String untuk kemudahan baca
            double prob = scanner.nextDouble();

            Integer stateInd = stateNameToIndex.get(stateName);
            if (stateInd == null) {
                throw new IllegalArgumentException("Unknown state name in emissions: " + stateName);
            }

            int symbolInd = symbolToInd(symbol); // Memanggil helper function
            if (symbolInd < 0 || symbolInd >= alphabetSize) {
                throw new IllegalArgumentException("Symbol '" + symbol + "' is out of expected alphabet range.");
            }


            stateSymbolProb[stateInd][symbolInd] = prob;
        }
    }

    /**
     * Mengkonversi simbol emisi (karakter tunggal) ke indeks.
     * Asumsi simbol adalah karakter 'a'..'z' ASCII.
     */
    public static int symbolToInd(String symbol) { // Dibuat public static karena dipanggil dari ExperimentData
        if (symbol == null || symbol.length() != 1) {
            throw new IllegalArgumentException("Symbol must be a single character string (a-z)");
        }
        char firstChar = symbol.charAt(0);
        if (firstChar >= 'a' && firstChar <= 'z') {
            return firstChar - 'a';
        } else {
            throw new IllegalArgumentException("Symbol '" + symbol + "' is not a lowercase letter (a-z)");
        }
    }
}