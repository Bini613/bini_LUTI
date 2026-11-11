// Function in Main. Returns nothing. No arguments.
// Builds the road network in this order:
//   1. Grid of nodes
//   2. Arterials through the centre (horizontal, vertical, two diagonals)
//   3. Radial bell-curve thinning: decides which nodes roads may reach
//   4. Potential links between the remaining (accessible) nodes
//   5. Random passes: links grow out from the existing network (chains continue or branch)
//   6. Test traces, then save to savedNetwork.txt
// Dead ends are not resolved yet.

// 1. Grid
varGrid = new GridPoint[parCols][parCols];
for (int i = 0; i < parCols; i++) {
    for (int j = 0; j < parCols; j++) {
        GridPoint p = add_gridPoints(i, j, i * parCols + j);   // third argument: parIndex, the node's stacked column index
        p.setXY(i * parSpacing, j * parSpacing);
        varGrid[i][j] = p;
    }
}

// 2. Four arterials through the central node: horizontal, vertical, two diagonals.
//    Built on an empty grid, so no crossing checks are needed here.
int ci = parCols / 2; // truncates if parCols is odd; for even parCols, one step off centre, away from the origin
int cj = parCols / 2;
int[][] axes = {{1,0}, {0,1}, {1,1}, {1,-1}};
int arterialLinksAdded = 0;
for (int[] d : axes) {
    for (int s = -1; s <= 1; s += 2) {   // walk outwards from the centre, both ways
        int i = ci, j = cj;
        while (true) {
            int ni = i + s * d[0];
            int nj = j + s * d[1];
            if (ni < 0 || ni >= parCols || nj < 0 || nj >= parCols) break; // reached edge
            boolean centreDiagonal = (i == ci && j == cj) && d[0] != 0 && d[1] != 0;   // diagonal arterials skip their first step out of the centre, so the centre is a 4-way junction, not 8-way
            if (!centreDiagonal) {
                funAddLink(varGrid[i][j], varGrid[ni][nj]);
                arterialLinksAdded++;
            }
            i = ni;
            j = nj;
        }
    }
}
traceln("Arterial links added: " + arterialLinksAdded);   // TEST

// 3. Radial bell-curve thinning: each node is made inaccessible with probability
//    1 - exp(-0.5 * ((dist - parThinningStartingNode) * parRadialRoadDispersion)^2), dist = grid steps from the centre.
//    Nodes within parThinningStartingNode steps of the centre are never thinned (probability 0).
//    Arterial nodes (already linked) always stay accessible, so branches can start anywhere along them.
//    parRadialRoadDispersion = 0 keeps every node.
Set<GridPoint> accessible = new HashSet<>();
for (GridPoint g : gridPoints) {
    if (g.colLinks.isEmpty() && parRadialRoadDispersion > 0) {
        double dx = g.parXIndex - ci;
        double dy = g.parYIndex - cj;
        double dist = max(0, sqrt(dx * dx + dy * dy));   
        if (dist >= parThinningStartingNode) { // 0 up to parThinningStartingNode steps out, so no thinning there
            double exponent = - 0.5 * pow(parRadialRoadDispersion * (dist - parThinningStartingNode), 2);
            if (randomTrue(1 - exp(exponent))) continue;   // made inaccessible, so not added to accessible list below
        }
    }
    accessible.add(g);
}
traceln("Accessible nodes: " + accessible.size() + " of " + gridPoints.size());   // TEST

// 4. Potential links: every node's E and N links, plus both diagonals of the grid square to its NE
//    (y points down in AnyLogic, so N is j-1). Both ends must be accessible, and arterial links are skipped.
List<GridPoint[]> potentialLinks = new ArrayList<>();
java.util.function.BiConsumer<GridPoint, GridPoint> addPotential = (a, b) -> {
    if (accessible.contains(a) && accessible.contains(b) && funFindLink(a, b) == null)
        potentialLinks.add(new GridPoint[]{a, b});
};
for (int i = 0; i < parCols; i++) { // pan across the x axis starting from the left
    for (int j = 0; j < parCols; j++) { // pan across the y axis starting from the top
        GridPoint p = varGrid[i][j];
        if (i + 1 < parCols) addPotential.accept(p, varGrid[i+1][j]);   // E
        if (j - 1 >= 0) addPotential.accept(p, varGrid[i][j-1]);        // N
        if (i + 1 < parCols && j - 1 >= 0) {   // this node owns a NE square
            addPotential.accept(p, varGrid[i+1][j-1]);                  // (i,j) to (i+1,j-1): NE diagonal from p
            addPotential.accept(varGrid[i+1][j], varGrid[i][j-1]);      // (i+1,j) to (i,j-1): NW diagonal from the node one step right
        }
    }
}
traceln("Potential links: " + potentialLinks.size());   // TEST

// 5. Repeated passes until all accesible nodes are linked or max number of passes has occured.
//    Each pass shuffles the remaining candidates (so the order has no spatial bias) and rolls each one;
//    rejected candidates get another chance next pass, when more roads exist for the factors to favour.
//    With parNewStubProb = 0, a link between two unlinked nodes has probability 0, so roads only grow from the
//    existing network, starting from the arterials. Shuffling uses AnyLogic's random generator (reproducible by seed).
int connectedNodes = 0;   // accessible nodes with at least one link
for (GridPoint g : accessible) if (!g.colLinks.isEmpty()) connectedNodes++; //Start the count with the existing arterials
int passes = 0;
List<GridPoint[]> remaining = potentialLinks;
while (!remaining.isEmpty() && passes < parMaxRandomPasses) {
    passes++;
    Collections.shuffle(remaining, getDefaultRandomGenerator());
    List<GridPoint[]> rejected = new ArrayList<>();
    for (GridPoint[] pl : remaining) {
        GridPoint a = pl[0];
        GridPoint b = pl[1];
        boolean diagonal = a.parXIndex != b.parXIndex && a.parYIndex != b.parYIndex;
        if (diagonal && funFindLink(varGrid[b.parXIndex][a.parYIndex], varGrid[a.parXIndex][b.parYIndex]) != null) continue;   // would cross the square's other diagonal: dropped for good (not added to rejected list which could receive links on future passes.)
        double prob = parBaseLinkProb * funJunctionsFactor(a, b) * funAngleFactor(a, b);
        if (randomTrue(min(1.0, prob))) { //min of 1 incase prob returns >1 (it shouldn't if probs are set up correctly)
            if (a.colLinks.isEmpty()) connectedNodes++;
            if (b.colLinks.isEmpty()) connectedNodes++;
            funAddLink(a, b);
        }
        else rejected.add(pl);   // try again next pass
    }
    remaining = rejected;
}
traceln("Random stage: " + passes + " passes, coverage of accessible nodes " + (double) connectedNodes / accessible.size());   // TEST
traceln("Links after random stage: " + links.size());   // TEST

// 6. Tests
// Count grid squares that have both diagonals (should be 0)
int crossings = 0;
for (int i = 0; i + 1 < parCols; i++) {
    for (int j = 1; j < parCols; j++) {
        if (funFindLink(varGrid[i][j], varGrid[i+1][j-1]) != null && funFindLink(varGrid[i+1][j], varGrid[i][j-1]) != null) crossings++;
    }
}
traceln("Crossing diagonals: " + crossings + " (should be 0)");   // TEST

// Count dead ends (1 link) and junctions (3+ links)
int deadEnds = 0;
int junctions = 0;
for (GridPoint p : gridPoints) {
    if (p.colLinks.size() == 1) deadEnds++;
    else if (p.colLinks.size() >= 3) junctions++;
}
traceln("Dead ends: " + deadEnds + ", junctions: " + junctions);   // TEST
traceln("Avg links per node: " + 2.0 * links.size() / gridPoints.size());   // TEST (Average is calculated by taking out every node independently and counting its links, adding to total and dividing by nubmer of links. Every link contributes to the count of both nodes it touches, so each link counts twice towards the total)

// Save the network to a text file in the model folder, so later runs can load it instead of rebuilding.
// First line: grid size. Then one line per link: x,y of one end, x,y of the other end. Overwritten on every build.
try (java.io.PrintWriter out = new java.io.PrintWriter("savedNetwork.txt")) {
    out.println(parCols);
    for (Link l : links) {
        out.println(l.parNodeA.parXIndex + "," + l.parNodeA.parYIndex + "," + l.parNodeB.parXIndex + "," + l.parNodeB.parYIndex);
    }
    traceln("Network saved: " + links.size() + " links");
} catch (Exception e) {
    traceln("Could not save network: " + e.getMessage());
}
