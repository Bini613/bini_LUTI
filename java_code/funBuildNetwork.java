// Function in Main. Returns nothing. No arguments.
// Builds the road network in this order:
//   1. Grid of nodes
//   2. Arterials through the centre (horizontal, vertical, two diagonals)
//   3. Radial bell-curve thinning: decides which nodes roads may reach
//   4. Potential links between the remaining (accessible) nodes
//   5. Random passes: links grow out from the existing network (chains continue or branch)
//   6. Resolve dead ends
//   7. Test traces, then save to savedNetwork.txt

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
Set<GridPoint> arterialTips = new HashSet<>();   // the arterials' end nodes at the grid edge: never treated as dead ends
for (int[] d : axes) {
    for (int s = -1; s <= 1; s += 2) {   // walk outwards from the centre, both ways
        int i = ci, j = cj;
        while (true) {
            int ni = i + s * d[0];
            int nj = j + s * d[1];
            if (ni < 0 || ni >= parCols || nj < 0 || nj >= parCols) { // reached edge
                arterialTips.add(varGrid[i][j]);
                break;
            }
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
        double dist = sqrt(dx * dx + dy * dy);   
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

// 6. Dead ends: one at a time, pick a dead end that hasn't been decided yet, roll whether to resolve it, and if so
//    walk it towards the centre until it joins the network. Lengths and probabilities are recalculated before every
//    pick, since each resolution can change other dead ends' chains (e.g. joining a chain splits it at a new junction).
//    Each dead end is decided once: resolved, stuck, or left hanging.

// 6a. measureDeadEnds: fills tips and chainLengthsfills chainLengths (Map: tip → chain length) with every current dead end (arterial tips excluded).
//     A dead end's chain: walk back from the tip through 2-link nodes to the first node without exactly 2 links
//     (a junction, or the other tip of an isolated segment). Length in grid steps (diagonal = 1.41).
Map<GridPoint, Double> chainLengths = new LinkedHashMap<>();
Runnable measureDeadEnds = () -> {
    chainLengths.clear();
    for (GridPoint t : gridPoints) {
        if (t.colLinks.size() != 1) continue;     // not a dead end
        if (arterialTips.contains(t)) continue;   // an arterial's end at the grid edge: not a dead end
        double len = 0;
        GridPoint cur = t;
        Link e = t.colLinks.get(0);
        while (true) {
            GridPoint nxt = (e.parNodeA == cur) ? e.parNodeB : e.parNodeA;
            int dx = nxt.parXIndex - cur.parXIndex;
            int dy = nxt.parYIndex - cur.parYIndex;
            len += sqrt(dx * dx + dy * dy);
            if (nxt.colLinks.size() != 2) break;   // reached the chain's other end
            e = (nxt.colLinks.get(0) == e) ? nxt.colLinks.get(1) : nxt.colLinks.get(0);   // the link out the other side
            cur = nxt;
        }
        chainLengths.put(t, len);
    }
};

// 6b. walkToCentre: from a tip, step to the neighbour closest to the centre, until reaching a node that already has links.
//     Each step is the closest-to-centre neighbour that is on the grid, not already linked to the current node,
//     doesn't cross an existing diagonal, and is strictly closer to the centre than the current node (so the walk
//     can't go round in circles). Joining its own chain is allowed. Ties are broken by shuffling the direction order.
//     Returns null if the walk joined the network, or the node it got stuck at (the road's new dead end).
List<int[]> allDirs = Arrays.asList(new int[][]{{1,0}, {-1,0}, {0,1}, {0,-1}, {1,1}, {1,-1}, {-1,1}, {-1,-1}});   // a list so it can be shuffled
int[] walkLinks = {0};   // links added by all walks (an array so the lambda can change it)
java.util.function.Function<GridPoint, GridPoint> walkToCentre = start -> {
    GridPoint cur = start;
    while (true) {
        double curDist = sqrt(pow(cur.parXIndex - ci, 2) + pow(cur.parYIndex - cj, 2));
        GridPoint best = null;
        double bestDist = curDist;   // a step must get strictly closer than this
        Collections.shuffle(allDirs, getDefaultRandomGenerator());   // random direction order every step, so ties have no directional bias
        for (int[] d : allDirs) {
            int ni = cur.parXIndex + d[0];
            int nj = cur.parYIndex + d[1];
            if (ni < 0 || ni >= parCols || nj < 0 || nj >= parCols) continue;   // off grid
            GridPoint b = varGrid[ni][nj];
            //if (!accessible.contains(b)) continue; // made inaccessible by radial thinning. Commented out by default since most tips only formed due to inaccessible neighbours.
            if (funFindLink(cur, b) != null) continue;                            // the existing link itself
            if (d[0] != 0 && d[1] != 0 && funFindLink(varGrid[ni][cur.parYIndex], varGrid[cur.parXIndex][nj]) != null) continue;   // diagonal would cross
            double bDist = sqrt(pow(ni - ci, 2) + pow(nj - cj, 2));
            if (bDist < bestDist) {
                bestDist = bDist;
                best = b;
            }
        }
        if (best == null) return cur;   // no step gets closer: stuck, stays a dead end
        boolean joins = !best.colLinks.isEmpty();   // check before linking
        funAddLink(cur, best);
        walkLinks[0]++;
        if (joins) return null;   // reached the network (possibly its own chain): done
        cur = best;               // stepped onto an empty node: keep walking
    }
};

// 6c. One dead end at a time, until every dead end has been decided
Set<GridPoint> decided = new HashSet<>();   // dead ends already rolled (resolved, stuck or left hanging)
measureDeadEnds.run();
int deadEndsBefore = chainLengths.size();
int chosenCount = 0;
int resolved = 0;
int stuck = 0;
while (true) {
    measureDeadEnds.run();   // fresh lengths, since the last resolution may have changed them
    List<GridPoint> undecided = new ArrayList<>();
    for (GridPoint tip : chainLengths.keySet()) if (!decided.contains(tip)) undecided.add(tip);
    if (undecided.isEmpty()) break;

    // Pick one undecided dead end at random, and roll it: probability scales linearly with chain length,
    // from parEndStubResolveProb (shortest current dead end) to 1 (longest)
    GridPoint tip = randomFrom(undecided);
    double len = chainLengths.get(tip);
    decided.add(tip);
    double lMin = Collections.min(chainLengths.values());
    double lMax = Collections.max(chainLengths.values());
    double frac = (lMax > lMin) ? (len - lMin) / (lMax - lMin) : 1;   // all the same length: all count as longest
    double pChoose = parEndStubResolveProb + (1 - parEndStubResolveProb) * frac;
    if (!randomTrue(pChoose)) continue;   // left hanging

    chosenCount++;
    GridPoint stuckAt = walkToCentre.apply(tip);
    if (stuckAt == null) resolved++;
    else {
        stuck++;
        decided.add(stuckAt);   // the stuck road's new end: walking it again would get stuck the same way
    }
}
traceln("Dead ends: " + deadEndsBefore + ", chosen: " + chosenCount + ", resolved: " + resolved + ", stuck: " + stuck + ", links added: " + walkLinks[0]);   // TEST

// 7. Tests
// Count dead ends (1 link) and junctions (3+ links) after resolving
int deadEnds = 0;
int junctions = 0;
for (GridPoint p : gridPoints) {
    if (p.colLinks.size() == 1 && !arterialTips.contains(p)) deadEnds++;   // arterial tips aren't counted
    else if (p.colLinks.size() >= 3) junctions++;
}
traceln("Dead ends left: " + deadEnds + ", junctions: " + junctions);   // TEST

// Count grid squares that have both diagonals (should be 0)
int crossings = 0;
for (int i = 0; i + 1 < parCols; i++) {
    for (int j = 1; j < parCols; j++) {
        if (funFindLink(varGrid[i][j], varGrid[i+1][j-1]) != null && funFindLink(varGrid[i+1][j], varGrid[i][j-1]) != null) crossings++;
    }
}
traceln("Crossing diagonals: " + crossings + " (should be 0)");   // TEST

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
