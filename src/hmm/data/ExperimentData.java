package hmm.data;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

/*
 * © 2025 hendrowunga, University of Sanata Dharma
 * Created on 5/25/25
 */
public class ExperimentData {
    /// Triple hmm.data: (waktu, indeks state, indeks simbol yang diemisikan)
    public List<TimeStateSymbolTuple> timeStateSymbol;

    /**
     * Membaca hmm.data eksperimen dari stream.
     * Asumsi stream memiliki format yang benar.
     *
     * @param model      Model HMM terkait untuk referensi nama state.
     * @param dataSource InputStream untuk membaca hmm.data eksperimen.
     * @throws IOException              Jika terjadi masalah I/O.
     * @throws IllegalArgumentException Jika hmm.data eksperimen tidak valid.
     * @throws NoSuchElementException   Jika format file tidak sesuai.
     */
    public void readExperimentData(Model model, InputStream dataSource) throws IOException, IllegalArgumentException, NoSuchElementException {
        Scanner scanner = new Scanner(dataSource);
        // Seperti Model.java, asumsikan InputStream/Scanner dikelola di luar.

        int nsteps = scanner.nextInt();

        if (nsteps == 0) {
            throw new IllegalArgumentException("Empty experiment hmm.data");
        }

        timeStateSymbol = new ArrayList<>(nsteps);

        for (int i = 0; i < nsteps; ++i) {
            int stepNumber = scanner.nextInt();
            String stateName = scanner.next();
            String symbol = scanner.next(); // String untuk kemudahan baca

            Integer stateInd = model.stateNameToIndex.get(stateName);
            if (stateInd == null) {
                throw new IllegalArgumentException("Unknown state name in experiment hmm.data: " + stateName);
            }

            int symbolInd = Model.symbolToInd(symbol); // Panggil helper function dari Model
            if (symbolInd < 0 || symbolInd >= model.alphabetSize) {
                throw new IllegalArgumentException("Symbol '" + symbol + "' in experiment hmm.data is out of model's alphabet range.");
            }


            timeStateSymbol.add(new TimeStateSymbolTuple(stepNumber, stateInd, symbolInd));
        }
    }
}