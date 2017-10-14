package com.melonfishy.florabeats.data;

import android.support.annotation.NonNull;

import java.util.ArrayList;
import java.util.HashMap;

public class RhythmPlant {

    public static final int STARTING_HEALTH = 40;
    public static final int GROWTH_THRESHOLD = 90;
    public static final int DEATH_THRESHOLD = 0;
    private static final String DATA_PARSE_ERROR = "Failed to parse rhythm plant data: ";

    public class PlantNode {

        int level, health;
        Integer parentID, ID;
        ArrayList<PlantNode> children;
        PlantNode parent;

        PlantNode(PlantNode p, Integer pid, Integer id) {
            parent = p;
            ID = id;
            parentID = pid;
            level = (p == null) ? 1 : p.getLevel() + 1;
            health = STARTING_HEALTH;
            children = new ArrayList<>();
        }

        void addChild(@NonNull PlantNode p) {
            children.add(p);
        }

        public int getLevel() {
            return level;
        }

        Integer getParentID() {
            return parentID;
        }

        public PlantNode getParent() {
            return parent;
        }

        public ArrayList<PlantNode> getChildren() {
            return children;
        }

        public Integer getID() {
            return ID;
        }

        public boolean isPresent() {
            return (parent != null) ? (parent.health >= GROWTH_THRESHOLD || level <= 2)
                    && health > DEATH_THRESHOLD : health > DEATH_THRESHOLD;
        }
    }

    private HashMap<Integer, PlantNode> directory;
    private int size;

    public RhythmPlant() {
        directory = new HashMap<>();
        PlantNode root = new PlantNode(null, null, 0);
        directory.put(0, root);
        size = 1;
    }

    public RhythmPlant(String data) {
        String[] tokens;
        if (data.startsWith("[") && data.endsWith("]")) {
            data = data.replaceAll("\\[", "").replaceAll("\\]", "");
            tokens = data.split(",\\s*");
        } else {
            throw new IllegalArgumentException(DATA_PARSE_ERROR +
                    "missing square brackets around data string");
        }

        directory = new HashMap<>();
        PlantNode root = new PlantNode(null, null, 0);
        directory.put(0, root);
        size = 1;

        for (int i = 1; i < tokens.length; i++) {
            String[] subtokens = tokens[i].split("_");
            if (subtokens.length != 2) {
                throw new IllegalArgumentException(DATA_PARSE_ERROR + "token " +
                        "(" + tokens[i] + ") " +
                        "does not contain two subtokens");
            }
            try {
                if (Integer.valueOf(subtokens[0]) == i) {
                    if (!subtokens[1].equals("null")) {
                        addChild(Integer.valueOf(subtokens[1]));
                    }
                }
            } catch (NumberFormatException e) {
                e.printStackTrace();
                throw new IllegalArgumentException(DATA_PARSE_ERROR + "token " +
                        "(" + tokens[i] + ") " +
                        "has subtokens of non-integer type");
            }
        }
    }

    public PlantNode getRoot() {
        return directory.get(0);
    }

    public int getSize() {
        return size;
    }

    public PlantNode getNode(int index) {
        return directory.get(index);
    }

    public int getLevel(int index) {
        return directory.get(index).getLevel();
    }

    public int getHash(int index) {
        return directory.get(index).hashCode();
    }

    public void addChild(int index) {
        PlantNode child = new PlantNode(getNode(index), index, size);
        getNode(index).addChild(child);
        directory.put(size, child);
        size++;
    }

    public String exportDataString() {
        StringBuilder builder = new StringBuilder();
        builder.append("[");
        for (int count = 0; count < directory.size(); count++) {
            if (count > 0) {
                builder.append(", ");
            }
            builder.append(count);
            builder.append("_");
            Integer parentID = getNode(count).getParentID();
            builder.append((parentID == null) ? "null" : parentID.toString());
        }
        return builder.append("]").toString();
    }

    public boolean equals(Object other) {
        if (!(other instanceof RhythmPlant)) {
            return false;
        } else {
            RhythmPlant otherPlant = (RhythmPlant) other;
            if (getSize() == otherPlant.getSize()) {
                for (int i = 0; i < getSize(); i++) {
                    if (getNode(i).getParentID() == null) {
                        continue;
                    }
                    if (!getNode(i).getParentID().equals(otherPlant.getNode(i).getParentID())) {
                        return false;
                    }
                }
                return true;
            } else {
                return false;
            }
        }
    }

    public HashMap<Integer, Integer> generateLevelHistogram() {
        HashMap<Integer, Integer> result = new HashMap<>();
        for (int count = 0; count < directory.size(); count++) {
            int level = getLevel(count);
            if (result.containsKey(level)) {
                result.put(level, result.get(level) + 1);
            } else {
                result.put(level, 1);
            }
        }
        return result;
    }

}
