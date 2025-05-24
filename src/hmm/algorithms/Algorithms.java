package hmm.algorithms;

import hmm.data.ExperimentData;
import hmm.data.Model;

import java.util.*;

/*
 * © 2025 hendrowunga, University of Sanata Dharma
 * Created on 5/25/25
 */
public class Algorithms {

    // Menggunakan -1 untuk merepresentasikan state yang tidak terdefinisi
    private static final int HMM_UNDEFINED_STATE = -1;

    /**
     * Fungsi bantu untuk menghitung probabilitas state baru untuk langkah Viterbi.
     */
    private static double calcNewStateProbability(int stepNumber, int prevState,
                                                  int curState, int curSymbol,
                                                  Model model, // Gunakan kelas Model dari package hmm.hmm.data
                                                  double[][] sequenceProbability) {
        double prevProbability = 1.0;

        if (stepNumber == 0) {
            if (prevState == 0) { // Hanya state 'begin' (indeks 0) yang punya prob non-nol di langkah 0
                prevProbability = 1.0; // Probabilitas memulai di state 0 adalah 1
            } else {
                prevProbability = 0.0; // State lain tidak bisa menjadi state sebelumnya di langkah 0
            }
        } else {
            // Untuk langkah > 0, probabilitas sebelumnya diambil dari tabel sequenceProbability
            if (stepNumber - 1 < 0 || stepNumber - 1 >= sequenceProbability.length ||
                    prevState < 0 || prevState >= sequenceProbability[stepNumber - 1].length) {
                // Ini seharusnya tidak terjadi jika langkah-langkah DP dihitung dengan benar
                throw new IndexOutOfBoundsException("Invalid index access in sequenceProbability at step " + stepNumber);
            }
            prevProbability = sequenceProbability[stepNumber - 1][prevState];
        }

        // Pastikan indeks state dan simbol valid sebelum akses array probabilitas
        if (prevState < 0 || prevState >= model.transitionProb.length ||
                curState < 0 || curState >= model.transitionProb[0].length) {
            throw new IndexOutOfBoundsException("Invalid state index accessing transitionProb [" + prevState + "][" + curState + "]");
        }
        if (curState < 0 || curState >= model.stateSymbolProb.length ||
                curSymbol < 0 || curSymbol >= model.stateSymbolProb[0].length) {
            throw new IndexOutOfBoundsException("Invalid state/symbol index accessing stateSymbolProb [" + curState + "][" + curSymbol + "]");
        }


        return (prevProbability *
                model.transitionProb[prevState][curState] *
                model.stateSymbolProb[curState][curSymbol]);
    }

    /**
     * Fungsi bantu untuk menemukan state sebelumnya terbaik selama langkah Viterbi.
     */
    private static int findBestTransitionSource(int stepNumber, int curState,
                                                int curSymbol, Model model,
                                                double[][] sequenceProbability) {
        if (stepNumber == 0) {
            return 0; // State sebelumnya untuk langkah 0 selalu state 'begin' (indeks 0)
        }

        int nstates = model.transitionProb.length;
        double bestProbValue = -1.0;
        int bestPrevState = HMM_UNDEFINED_STATE;

        for (int prevState = 0; prevState < nstates; ++prevState) {
            double curProb = calcNewStateProbability(stepNumber, prevState,
                    curState, curSymbol,
                    model, sequenceProbability);

            if (curProb > bestProbValue) {
                bestProbValue = curProb;
                bestPrevState = prevState;
            }
        }

        // C++ comment says result won't be undefined if nstates >= 2.
        // Jika bestProbValue tetap -1.0, artinya tidak ada path yang mungkin ke state curState dari
        // state manapun di langkah sebelumnya dengan probabilitas > 0.
        // Kita bisa melemparkan exception atau mengembalukan UNDEFINED.
        // Mengikuti C++, asumsikan ini tidak akan terjadi pada hmm.data yang valid.
        // Namun, menambahkan check atau mengembalikan UNDEFINED_STATE dan menanganinya di luar lebih aman.
        // Mengembalikan bestPrevState saja (yang mungkin -1 jika tidak ada path)
        return bestPrevState;
    }

    /**
     * Fungsi bantu untuk mendapatkan probabilitas kumulatif langkah forward.
     */
    private static double calcForwardStepProbability(int stepNumber, int curState,
                                                     Model model, ExperimentData data,
                                                     double[][] forwardStateProbability) {
        int nstates = model.transitionProb.length;
        int curSymbol = data.timeStateSymbol.get(stepNumber).symbolIndex; // Mengakses TimeStateSymbolTuple

        // Pastikan indeks state dan simbol valid
        if (curState < 0 || curState >= model.stateSymbolProb.length ||
                curSymbol < 0 || curSymbol >= model.stateSymbolProb[0].length) {
            throw new IndexOutOfBoundsException("Invalid state/symbol index access in stateSymbolProb [" + curState + "][" + curSymbol + "]");
        }


        if (stepNumber == 0) {
            // Probabilitas untuk langkah 0 adalah prob transisi dari state 'begin' (0)
            // dikalikan prob emisi simbol saat ini dari curState.
            if (0 < 0 || 0 >= model.transitionProb.length ||
                    curState < 0 || curState >= model.transitionProb[0].length) {
                throw new IndexOutOfBoundsException("Invalid state index access in transitionProb at step 0");
            }
            return model.transitionProb[0][curState] * model.stateSymbolProb[curState][curSymbol];
        } else {
            double prevCumulativeProb = 0.0;

            for (int prevState = 0; prevState < nstates; ++prevState) {
                // Pastikan indeks valid saat mengakses tabel probabilitas forward dan transisi
                if (stepNumber - 1 < 0 || stepNumber - 1 >= forwardStateProbability.length ||
                        prevState < 0 || prevState >= forwardStateProbability[stepNumber - 1].length) {
                    throw new IndexOutOfBoundsException("Invalid index access in forwardStateProbability at step " + (stepNumber - 1));
                }
                if (prevState < 0 || prevState >= model.transitionProb.length ||
                        curState < 0 || curState >= model.transitionProb[0].length) {
                    throw new IndexOutOfBoundsException("Invalid state index access in transitionProb [" + prevState + "][" + curState + "]");
                }

                prevCumulativeProb += (forwardStateProbability[stepNumber - 1][prevState] *
                        model.transitionProb[prevState][curState]);
            }

            return prevCumulativeProb * model.stateSymbolProb[curState][curSymbol];
        }
    }

    /**
     * Fungsi bantu untuk mendapatkan probabilitas kumulatif langkah backward.
     */
    private static double calcBackwardStepProbability(int stepNumber, int curState,
                                                      Model model, ExperimentData data,
                                                      double[][] backwardStateProbability) {
        int nstates = model.transitionProb.length;
        int maxtime = data.timeStateSymbol.size();

        if (stepNumber + 1 == maxtime) {
            return 1.0; // Probabilitas untuk menjelaskan urutan kosong (setelah langkah terakhir) adalah 1.
        } else {
            // Simbol yang diemisikan di *langkah berikutnya* (stepNumber + 1)
            int nextSymbol = data.timeStateSymbol.get(stepNumber + 1).symbolIndex; // Mengakses TimeStateSymbolTuple
            double nextCumulativeProb = 0.0;

            for (int nextState = 0; nextState < nstates; ++nextState) {
                // Pastikan indeks valid saat mengakses tabel probabilitas transisi, emisi, dan backward
                if (curState < 0 || curState >= model.transitionProb.length ||
                        nextState < 0 || nextState >= model.transitionProb[0].length) {
                    throw new IndexOutOfBoundsException("Invalid state index access in transitionProb [" + curState + "][" + nextState + "]");
                }
                if (nextState < 0 || nextState >= model.stateSymbolProb.length ||
                        nextSymbol < 0 || nextSymbol >= model.stateSymbolProb[0].length) {
                    throw new IndexOutOfBoundsException("Invalid state/symbol index access in stateSymbolProb [" + nextState + "][" + nextSymbol + "]");
                }
                if (stepNumber + 1 < 0 || stepNumber + 1 >= backwardStateProbability.length ||
                        nextState < 0 || nextState >= backwardStateProbability[stepNumber + 1].length) {
                    throw new IndexOutOfBoundsException("Invalid index access in backwardStateProbability at step " + (stepNumber + 1));
                }


                nextCumulativeProb += (model.transitionProb[curState][nextState] *
                        model.stateSymbolProb[nextState][nextSymbol] *
                        backwardStateProbability[stepNumber + 1][nextState]);
            }

            return nextCumulativeProb;
        }
    }


    /**
     * Menemukan urutan state tersembunyi yang paling mungkin.
     * Implementasi berdasarkan algoritma Viterbi (Dynamic Programming).
     *
     * @param model Model HMM.
     * @param data Data observasi/eksperimen.
     * @return List indeks state tersembunyi yang diprediksi.
     */
    public static List<Integer> findMostProbableStateSequence(Model model, ExperimentData data) {
        int nstates = model.transitionProb.length;
        int maxtime = data.timeStateSymbol.size();

        if (maxtime == 0) {
            return new ArrayList<>(); // Data kosong
        }


        // sequenceProbability[i][j] adalah probabilitas urutan state paling mungkin
        // untuk observasi 1..i yang state terakhirnya adalah state dengan indeks j.
        double[][] sequenceProbability = new double[maxtime][nstates];

        // prevSeqState[i][j] adalah state sebelumnya yang mengarah ke
        // state j di langkah i dalam urutan paling mungkin.
        int[][] prevSeqState = new int[maxtime][nstates]; // Menggunakan int, -1 untuk undefined

        // bagian: hitung probabilitas untuk algoritma Viterbi (DP)
        for (int t = 0; t < maxtime; ++t) {
            int curSymbol = data.timeStateSymbol.get(t).symbolIndex; // Mengakses TimeStateSymbolTuple
            for (int curState = 0; curState < nstates; ++curState) {
                int bestPrevState = findBestTransitionSource(t, curState, curSymbol, model, sequenceProbability);
                // Jika bestPrevState adalah UNDEFINED, artinya tidak ada path ke sini.
                // Probabilitas harusnya 0 dalam kasus ini.
                double bestProbValue = 0.0;
                if (bestPrevState != HMM_UNDEFINED_STATE) {
                    bestProbValue = calcNewStateProbability(t, bestPrevState, curState, curSymbol, model, sequenceProbability);
                }


                sequenceProbability[t][curState] = bestProbValue;
                prevSeqState[t][curState] = bestPrevState; // Simpan state sebelumnya
            }
        }

        // bagian: kumpulkan urutan paling mungkin dalam urutan terbalik
        List<Integer> mostProbableSeq = new ArrayList<>();
        int curStep = maxtime - 1;

        // Temukan state terakhir dari urutan paling mungkin (state dengan probabilitas tertinggi di langkah terakhir)
        int curState = -1;
        double maxProb = -1.0;
        for (int i = 0; i < nstates; ++i) {
            if (sequenceProbability[curStep][i] > maxProb) {
                maxProb = sequenceProbability[curStep][i];
                curState = i;
            }
        }

        // Jika maxtime > 0, mostProbableStateIndex harus sudah terupdate
        if (curState == -1) {
            // Ini bisa terjadi jika semua probabilitas di langkah terakhir adalah 0.
            // Mengikuti C++, ini seharusnya tidak terjadi pada hmm.data/model valid.
            // Mari tambahkan pesan peringatan dan kembalikan list kosong.
            System.err.println("Warning: Could not find a probable last state in Viterbi algorithm. All probabilities at the last step were zero or negative.");
            return new ArrayList<>();
        }


        // Backtrack dari langkah terakhir (maxtime-1) sampai langkah 0
        for (; curStep >= 0; --curStep) {
            mostProbableSeq.add(curState); // Tambahkan state saat ini (urutan terbalik)
            if (curStep > 0) { // Jangan mencari state sebelumnya jika sudah di langkah 0
                curState = prevSeqState[curStep][curState];
                if (curState == HMM_UNDEFINED_STATE) {
                    // Ini mengindikasikan masalah dalam backtracking,
                    // mungkin karena tidak ada path yang valid ditemukan di langkah forward.
                    System.err.println("Error during Viterbi backtracking: Encountered undefined previous state at step " + curStep);
                    // Hentikan backtracking dan kembalikan urutan yang tidak lengkap
                    break;
                }
            }
        }


        // Urutkan list untuk mendapatkan urutan yang benar (0..maxtime-1)
        Collections.reverse(mostProbableSeq);

        return mostProbableSeq;
    }

    /**
     * Menghitung pasangan nilai alpha dan beta untuk setiap momen waktu.
     * Implementasi berdasarkan algoritma Forward-Backward.
     *
     * @param model Model HMM.
     * @param data Data observasi/eksperimen.
     * @return List dari List pasangan (alpha, beta) untuk setiap langkah waktu dan state.
     *         Menggunakan SimpleEntry<Double, Double> sebagai pengganti Pair.
     */
    public static List<List<Map.Entry<Double, Double>>> calcForwardBackwardProbabilities(Model model, ExperimentData data) {
        int nstates = model.transitionProb.length;
        int maxtime = data.timeStateSymbol.size();

        if (maxtime == 0) {
            return new ArrayList<>(); // Data kosong
        }

        // forwardStateProbability[t][i] = alpha(t, i)
        double[][] forwardStateProbability = new double[maxtime][nstates];

        // bagian: hitung probabilitas forward
        for (int t = 0; t < maxtime; ++t) {
            for (int curState = 0; curState < nstates; ++curState) {
                double cumulativePrevProbability =
                        calcForwardStepProbability(t, curState, model, data, forwardStateProbability);
                forwardStateProbability[t][curState] = cumulativePrevProbability;
            }
        }

        // backwardStateProbability[t][i] = beta(t, i)
        double[][] backwardStateProbability = new double[maxtime][nstates];
        // Inisialisasi backward step terakhir (maxtime - 1). beta(maxtime-1, i) = 1
        // (kecuali untuk transisi ke state end).
        // Sesuai C++:
        // backwardStateProbability[maxtime - 1][i] akan dihitung oleh loop di bawah
        // dan CalcBackwardStepProbability mengembalikan 1.0 jika stepNumber + 1 == maxtime


        // bagian: hitung probabilitas backward
        for (int t = maxtime - 1; t >= 0; --t) {
            for (int curState = 0; curState < nstates; ++curState) {
                double cumulativeNextProbability =
                        calcBackwardStepProbability(t, curState, model, data, backwardStateProbability);
                backwardStateProbability[t][curState] = cumulativeNextProbability;
            }
        }


        // bagian: gabungkan hasil (alpha dan beta)
        List<List<Map.Entry<Double, Double>>> forwardBackwardProbability = new ArrayList<>(maxtime);
        for (int t = 0; t < maxtime; ++t) {
            List<Map.Entry<Double, Double>> stepProbabilities = new ArrayList<>(nstates);
            for (int curState = 0; curState < nstates; ++curState) {
                stepProbabilities.add(new AbstractMap.SimpleEntry<>( // Whats???
                        forwardStateProbability[t][curState],
                        backwardStateProbability[t][curState]
                ));
            }
            forwardBackwardProbability.add(stepProbabilities);
        }

        return forwardBackwardProbability;
    }
}