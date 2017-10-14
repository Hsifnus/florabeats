package com.melonfishy.florabeats;

import android.content.Context;
import android.util.Log;

import com.melonfishy.florabeats.data.RhythmPlant;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

public class RhythmTrack {
    public class Step {

        private ArrayList<Integer> notes;
        private ArrayList<String> descriptors;
        private int measure, bpm;
        private Double time, startTime;
        private boolean pulse, randomize;

        public Step(ArrayList<Integer> n, ArrayList<String> desc, int m, int b, double t, boolean p, boolean r) {
            notes = n;
            descriptors = desc;
            measure = m;
            time = t;
            startTime = (notes.size() > 0) ? time - (2 * stepTime(BPM)) : time;
            pulse = p;
            randomize = r;
            bpm = b;
        }

        public ArrayList<Integer> getNotes() {
            return notes;
        }

        public ArrayList<String> getDescriptors() {
            return descriptors;
        }

        public double getTime() {
            return time;
        }

        public double getStartTime() {
            return startTime;
        }

        public boolean getPulse() {
            return pulse;
        }

        public int getMeasure() {
            return measure;
        }

        public boolean getRandomize() {
            return randomize;
        }

        public int getBpm() {
            return bpm;
        }
    }

    private String filename, songName, difficulty, plantData, audioname;
    private ArrayList<Step> stepData;
    private int BPM, SPB, measureNum, upbeats, noteCount, difficultyLevel;
    private double currentTime;
    int lineNum;

    public RhythmTrack(String file, Context context) {
        currentTime = 0.0;
        lineNum = 1;
        filename = file;
        BPM = -1;
        SPB = 1;
        stepData = new ArrayList<>();
        measureNum = 0;
        upbeats = 4;
        noteCount = 0;
        difficultyLevel = 1;
        try {
            InputStream stream = context.getResources().getAssets().open(filename);
            BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
            String curr = reader.readLine();
            while (curr != null) {
                parseLine(curr);
                curr = reader.readLine();
            }
            stream.close();
        } catch (IOException e) {
            throw new IllegalArgumentException("Invalid stepfile filename: " + filename);
        }
        Log.d("RhythmTrack", "Step Data Size: " + stepData.size());
        seeStepData();
    }

    private void parseLine(String line) {
        switch(lineNum) {
            case 1:
                songName = line;
                lineNum++;
                break;
            case 2:
                difficulty = line;
                lineNum++;
                break;
            case 3:
                try {
                    difficultyLevel = Integer.valueOf(line);
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                    throw new IllegalArgumentException(
                            "Line 3 of stepfile must contain the difficulty level of the song!");
                }
                lineNum++;
                break;
            case 4:
                plantData = line;
                lineNum++;
                break;
            case 5:
                try {
                    upbeats = Integer.valueOf(line);
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                    throw new IllegalArgumentException(
                            "Line 5 of stepfile must contain the number of upbeats in the song!");
                }
                lineNum++;
                break;
            case 6:
                audioname = line;
                lineNum++;
                break;
            default:
                String[] tokens = line.split("\\s+");
                if (tokens.length == 0) {
                    return;
                }
                ArrayList<Integer> notes = new ArrayList<>();
                Step step;
                boolean randomize = false;
                switch (tokens[0]) {
                    case "<>":
                        for (int i = 1; i < tokens.length; i++) {
                            String token = tokens[i];
                            String[] subtokens = token.split(":");
                            if (subtokens.length != 2) {
                                throw new IllegalArgumentException("Malformed diamond token: "
                                        + token + " at line number " + lineNum + ", measure number "
                                + measureNum);
                            }
                            for (String s : subtokens) {
                                switch(subtokens[0]) {
                                    case "BPM":
                                        int previousBPM = BPM;
                                        BPM = Integer.valueOf(subtokens[1]);
                                        if (previousBPM == -1) {
                                            for (int j = 0; j < upbeats; j++) {
                                                Step startStep = new Step(new ArrayList<Integer>(),
                                                        null, measureNum, BPM, currentTime,
                                                        true, false);
                                                stepData.add(startStep);
                                                currentTime += stepTime(BPM);
                                            }
                                        }
                                        break;
                                    case "SPB":
                                        SPB = Integer.valueOf(subtokens[1]);
                                        break;
                                }
                            }
                        }
                        break;
                    case "\\":
                        if (BPM <= 0) {
                            throw new IllegalArgumentException("BPM " +
                                    "needs to be established before parsing any beats!");
                        }
                        if (tokens.length < 2 || !tokens[1].matches("M[0-9]+")) {
                            throw new IllegalArgumentException("Malformed backslash token: measure" +
                                    " number required immediately after token");
                        } else {
                            measureNum = Integer.valueOf(tokens[1].replace("M", ""));
                        }
                        for (int i = 2; i < tokens.length; i++) {
                            try {
                                if (tokens[i].equals("R")) {
                                    randomize = true;
                                } else {
                                    notes.add(Integer.valueOf(tokens[i]));
                                }
                            } catch (NumberFormatException e) {
                                e.printStackTrace();
                                throw new IllegalArgumentException("Invalid token: " + tokens[i]);
                            }
                        }
                        step = new Step(notes, null, measureNum, BPM, currentTime, true, randomize);
                        stepData.add(step);
                        noteCount += notes.size();
                        currentTime += stepTime(BPM) / SPB;
                        lineNum++;
                        break;
                    case "/":
                        if (BPM <= 0) {
                            throw new IllegalArgumentException("BPM " +
                                    "needs to be established before parsing any beats!");
                        }
                        for (int i = 1; i < tokens.length; i++) {
                            try {
                                if (tokens[i].equals("R")) {
                                    randomize = true;
                                } else {
                                    notes.add(Integer.valueOf(tokens[i]));
                                }
                            } catch (NumberFormatException e) {
                                e.printStackTrace();
                                throw new IllegalArgumentException("Invalid token: " + tokens[i]);
                            }
                        }
                        step = new Step(notes, null, measureNum, BPM, currentTime, true, randomize);
                        stepData.add(step);
                        noteCount += notes.size();
                        currentTime += stepTime(BPM) / SPB;
                        lineNum++;
                        break;
                    case "-":
                        if (BPM <= 0) {
                            throw new IllegalArgumentException("BPM " +
                                    "needs to be established before parsing any beats!");
                        }
                        for (int i = 1; i < tokens.length; i++) {
                            try {
                                if (tokens[i].equals("R")) {
                                    randomize = true;
                                } else {
                                    notes.add(Integer.valueOf(tokens[i]));
                                }
                            } catch (NumberFormatException e) {
                                e.printStackTrace();
                                throw new IllegalArgumentException("Invalid token: " + tokens[i]);
                            }
                        }
                        step = new Step(notes, null, measureNum, BPM, currentTime, false, randomize);
                        stepData.add(step);
                        noteCount += notes.size();
                        currentTime += stepTime(BPM) / SPB;
                        lineNum++;
                        break;
                }
        }
    }

    public String getFilename() {
        return filename;
    }

    public String getSongName() {
        return songName;
    }

    public static double stepTime(int BPM) {
        return (60.0 / BPM) * 1000.0;
    }

    private void seeStepData() {
        Log.d("RhythmTrack", "Song: " + getSongName() + ", Notes: " + getNoteCount());
        for (Step step : stepData) {
            Log.d("RhythmTrack", "Measure: " + step.getMeasure()
            + ", Pulse: " + step.getPulse() + ", Randomize: " + step.getRandomize()
                    + ", Notes: " + step.getNotes() + ", Time: " + step.getTime());
        }
    }

    public String getPlantData() {
        return plantData;
    }

    public String getDifficulty() {
        return difficulty;
    }

    public int getNoteCount() {
        return noteCount;
    }

    public ArrayList<Step> getStepData() {
        return stepData;
    }

    public int getUpbeats() {
        return upbeats;
    }

    public int getDifficultyLevel() {
        return difficultyLevel;
    }

    public String getAudioname() {
        return audioname;
    }
}