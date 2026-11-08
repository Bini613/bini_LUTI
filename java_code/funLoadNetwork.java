// Function in Main. Returns value, type: boolean. No arguments.
// Loads the grid and links from savedNetwork.txt (written by funBuildNetwork).
// Returns true if loaded; false if there is no usable file, in which case nothing has been created.
java.io.File f = new java.io.File("savedNetwork.txt");
if (!f.exists()) {
    traceln("No saved network found");
    return false;
}

// Read and check the whole file before creating anything, so a bad file leaves the model untouched
List<int[]> savedLinks = new ArrayList<>();
try {
    List<String> lines = java.nio.file.Files.readAllLines(f.toPath());
    if (Integer.parseInt(lines.get(0).trim()) != parCols) {
        traceln("Saved network is for a different grid size");
        return false;
    }
    for (int k = 1; k < lines.size(); k++) {
        String line = lines.get(k).trim();
        if (line.isEmpty()) continue;
        String[] v = line.split(",");
        savedLinks.add(new int[]{Integer.parseInt(v[0]), Integer.parseInt(v[1]), Integer.parseInt(v[2]), Integer.parseInt(v[3])});
    }
} catch (Exception e) {
    traceln("Could not read saved network: " + e.getMessage());
    return false;
}

// Grid (same as in funBuildNetwork)
varGrid = new GridPoint[parCols][parCols];
for (int i = 0; i < parCols; i++) {
    for (int j = 0; j < parCols; j++) {
        GridPoint p = add_gridPoints(i, j, i * parCols + j);   // third argument: parIndex, the node's stacked column index
        p.setXY(i * parSpacing, j * parSpacing);
        varGrid[i][j] = p;
    }
}

// Links
for (int[] s : savedLinks) {
    funAddLink(varGrid[s[0]][s[1]], varGrid[s[2]][s[3]]);
}
traceln("Network loaded: " + links.size() + " links");
return true;
