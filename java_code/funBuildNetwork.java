varGrid = new GridPoint[parCols][parCols];
for (int i = 0; i < parCols; i++) {
    for (int j = 0; j < parCols; j++) {
        GridPoint p = add_gridPoints(i, j, i * parCols + j);   // third argument: parIndex, the node's stacked column index
        p.setXY(i * parSpacing, j * parSpacing);
        varGrid[i][j] = p;
    }
}

// Random links (y points down in AnyLogic, so N is j-1).
// 1. List every potential link once: each node's E and N links, plus both diagonals of the grid square to its NE.
List<GridPoint[]> potentialLinks = new ArrayList<>();
for (int i = 0; i < parCols; i++) { //pan across the x axis starting from the left
    for (int j = 0; j < parCols; j++) { //pan across the y axis starting from the top
        GridPoint p = varGrid[i][j];
        if (i + 1 < parCols) potentialLinks.add(new GridPoint[]{p, varGrid[i+1][j]});   // E
        if (j - 1 >= 0) potentialLinks.add(new GridPoint[]{p, varGrid[i][j-1]});        // N
        if (i + 1 < parCols && j - 1 >= 0) {   // this node owns a NE square
            potentialLinks.add(new GridPoint[]{p, varGrid[i+1][j-1]});                  // (i,j) to (i+1,j-1) --NE link from node p
            potentialLinks.add(new GridPoint[]{varGrid[i+1][j], varGrid[i][j-1]});      // (i+1,j) to (i,j-1) --NW link from node one step right
        }
    }
}
// 2. Repeated passes until the share of nodes with at least one link reaches parInitialRoadBuildCoverage.
//    Each pass shuffles the remaining candidates (so the order has no spatial bias) and rolls each one;
//    rejected candidates get another chance next pass, when more roads exist for the factors to favour.
//    Shuffling uses AnyLogic's random generator, so runs with the same seed give the same network.
int connectedNodes = 0;   // nodes with at least one link
int passes = 0;
List<GridPoint[]> remaining = potentialLinks;
while ((double) connectedNodes / gridPoints.size() < parInitialRoadBuildCoverage && !remaining.isEmpty() && passes < parMaxRandomPasses) {
    passes++;
    Collections.shuffle(remaining, getDefaultRandomGenerator());
    List<GridPoint[]> rejected = new ArrayList<>();
    // 3. Accept each with a probability adjusted at both ends for junctions/dead ends and turn sharpness
    for (GridPoint[] pl : remaining) {
        if ((double) connectedNodes / gridPoints.size() >= parInitialRoadBuildCoverage) break;   // target reached mid-pass
        GridPoint a = pl[0];
        GridPoint b = pl[1];
        boolean diagonal = a.parXIndex != b.parXIndex && a.parYIndex != b.parYIndex;
        if (diagonal && funFindLink(varGrid[b.parXIndex][a.parYIndex], varGrid[a.parXIndex][b.parYIndex]) != null) continue;   // would cross the square's other diagonal: dropped for good
        double prob = parLinkBaseProb * funJunctionsFactor(a, b) * funAngleFactor(a, b);
        if (randomTrue(min(1.0, prob))) {
            if (a.colLinks.isEmpty()) connectedNodes++;
            if (b.colLinks.isEmpty()) connectedNodes++;
            funAddLink(a, b);
        }
        else rejected.add(pl);   // try again next pass
    }
    remaining = rejected;
}
traceln("Random stage: " + passes + " passes, coverage " + (double) connectedNodes / gridPoints.size());   // TEST
traceln("Links after random stage: " + links.size());   // TEST

// Four arterials through the central node: horizontal, vertical, two diagonals
int ci = parCols / 2; //truncates if parCols is odd, Java being zero indexed, this is exact centre. For even parCols, then we get one increment off centre further from origin.
int cj = parCols / 2;
int[][] axes = {{1,0}, {0,1}, {1,1}, {1,-1}};
int arterialLinksAdded = 0;
int diagonalsRemoved = 0;
for (int[] d : axes) {
    for (int s = -1; s <= 1; s += 2) {   // walk outwards from the centre, both ways
        int i = ci, j = cj;
        while (true) {
            int ni = i + s * d[0];
            int nj = j + s * d[1];
            if (ni < 0 || ni >= parCols || nj < 0 || nj >= parCols) break; // reached edge
            GridPoint a = varGrid[i][j];
            GridPoint b = varGrid[ni][nj];
            if (funFindLink(a, b) == null) {
                if (d[0] != 0 && d[1] != 0) {   // <-- if we are building a diagonal arterial on this run: remove the opposite diagonal of this square if it exists
                    Link cross = funFindLink(varGrid[ni][j], varGrid[i][nj]);
                    if (cross != null) {
                        cross.parNodeA.colLinks.remove(cross);
                        cross.parNodeB.colLinks.remove(cross);
                        remove_links(cross);
                        diagonalsRemoved++;
                    }
                }
                funAddLink(a, b);
                arterialLinksAdded++;
            }
            i = ni;
            j = nj;
        }
    }
}
traceln("Arterial links added: " + arterialLinksAdded);   // TEST
traceln("Diagonals removed for arterials: " + diagonalsRemoved);   // TEST

// Repair stages, defined here as local functions (lambdas) and run at the bottom of this block.
// Each is a Runnable: a block of code stored in a variable, executed by calling .run() on it.

// Give every isolated node one link to a neighbour (diagonals that would cross an existing link are excluded).
// The neighbour is picked at random, weighted by the same junction and angle factors as the random stage,
// so links that the random stage would favour are favoured here too. One link is always added.
Runnable connectIsolated = () -> {
    int[][] allDirs = {{1,0}, {-1,0}, {0,1}, {0,-1}, {1,1}, {1,-1}, {-1,1}, {-1,-1}};
    int isolatedLinksAdded = 0;
    List<GridPoint> order = new ArrayList<>();
    for (GridPoint g : gridPoints) order.add(g);   // copy the population into a plain list that can be shuffled
    Collections.shuffle(order, getDefaultRandomGenerator());   // random order, so bias of early fixes on later fixes is spread throughout grid
    for (GridPoint p : order) {
        if (p.colLinks.isEmpty()) {
            List<GridPoint> nbrs = new ArrayList<>();
            for (int[] d : allDirs) { //this loop builds a list of nieghbours around the grindpoint in question, excluding neighbours for whom a new link would cross an existing link (applicable to diagonals)
                int ni = p.parXIndex + d[0];
                int nj = p.parYIndex + d[1];
                if (ni < 0 || ni >= parCols || nj < 0 || nj >= parCols) continue;   // off grid
                if (d[0] != 0 && d[1] != 0 && funFindLink(varGrid[ni][p.parYIndex], varGrid[p.parXIndex][nj]) != null) continue;   // diagonal would cross
                nbrs.add(varGrid[ni][nj]);
            }
            // Weight each neighbour with the same factors as the random stage (parLinkProb is left out: it's the same for every neighbour)
            List<Double> weights = new ArrayList<>();
            double total = 0;
            for (GridPoint b : nbrs) {
                double w = funJunctionsFactor(p, b) * funAngleFactor(p, b);
                weights.add(w);
                total += w;
            }
            GridPoint chosen;
            if (total <= 0) chosen = randomFrom(nbrs);   // every weight is zero: fall back to an unweighted choice
            else {
                // Below is a weighted pick. We line up all the weights as bars of length in a line, and the following algorithm draws a random point r along that length, and then figures out the index of the neighbour whose weight bar r landed upon.
                double r
                    = uniform(0, total);
                int idx = 0;
                while (idx < nbrs.size() - 1 && r >= weights.get(idx)) {
                    r -= weights.get(idx);
                    idx++;
                }
                chosen = nbrs.get(idx);
            }
            funAddLink(p, chosen);
            isolatedLinksAdded++;
        }
    }
    traceln("Isolated-node links added: " + isolatedLinksAdded);   // TEST
};

// Join separate clusters into one network
Runnable bridgeClusters = () -> {
    int[][] allDirs = {{1,0}, {-1,0}, {0,1}, {0,-1}, {1,1}, {1,-1}, {-1,1}, {-1,-1}};
    Set<GridPoint> reached = new HashSet<>();
    ArrayDeque<GridPoint> queue = new ArrayDeque<>();
    reached.add(varGrid[0][0]);
    queue.add(varGrid[0][0]);
    List<GridPoint[]> candidates = new ArrayList<>();   // pairs {inside node, outside grid neighbour}, kept across passes
    int bridgesAdded = 0;
    while (true) {
        // Flood-fill: find every node reachable from what's already reached
        while (!queue.isEmpty()) {
            GridPoint p = queue.poll();
            // Record p's unreached grid neighbours as possible bridges (each node is explored once, so scanned once)
            for (int[] d : allDirs) {
                int ni = p.parXIndex + d[0];
                int nj = p.parYIndex + d[1];
                if (ni >= 0 && ni < parCols && nj >= 0 && nj < parCols && !reached.contains(varGrid[ni][nj]))
                    candidates.add(new GridPoint[]{p, varGrid[ni][nj]});//each candidate is a pair of nodes that are disconnected
            }
            for (Link l : p.colLinks) {
                GridPoint q = (l.parNodeA == p) ? l.parNodeB : l.parNodeA;
                if (reached.add(q)) queue.add(q); // Condition here is doing double duty, by adding and returning true or false: if node q hasn't already been reached, then add it to the queue, otherwise don't, and move to next link. When all links are done, the next 'unexplored' node in the queue has the process repeated for all its links ('unexplored'=has not yet been the subject p of the while loop), itself expanding the queue in the process (unless all its connected nodes were already reached).
            }
        }
        if (reached.size() == gridPoints.size()) break;   // all connected. The loop above could have an empty queue but not have explored all regions, since disconnected clusters never entered the queue, because entering the queue requires node q to have been reached by an existing intracluster link. This conditional break here only occurs when all the separate clusters have been explored.

        // Drop invalid candidates: stale ones whose outside node has been reached since it was recorded,
        // and diagonal ones that would cross an existing link (earlier bridges may have blocked them).
        // Horizontal/vertical candidates are never blocked, so at least one valid candidate always remains.
        List<GridPoint[]> valid = new ArrayList<>();
        for (GridPoint[] c : candidates) { 
            GridPoint a = c[0];
            GridPoint b = c[1];
            if (reached.contains(b)) continue;   // stale
            boolean blocked = a.parXIndex != b.parXIndex && a.parYIndex != b.parYIndex
                              && funFindLink(varGrid[b.parXIndex][a.parYIndex], varGrid[a.parXIndex][b.parYIndex]) != null;
            if (blocked) continue;
            valid.add(c);
        }
        candidates = valid; //Previous loop created 'valid' list which purged all 'stale' entries. This becomes new candidate list

        // Pick one candidate at random, weighted by the same junction and angle factors as the random stage.
        // Weights are recalculated every pass, since each new bridge changes link counts.
        List<Double> weights = new ArrayList<>();
        double total = 0;
        for (GridPoint[] c : candidates) {
            double w = funJunctionsFactor(c[0], c[1]) * funAngleFactor(c[0], c[1]);
            weights.add(w);
            total += w;
        }
        GridPoint[] pair;
        if (total <= 0) pair = randomFrom(candidates);   // every weight is zero: fall back to an unweighted choice
        else {
            // Weighted pick (roulette wheel), as in connectIsolated
            double r = uniform(0, total);
            int idx = 0;
            while (idx < candidates.size() - 1 && r >= weights.get(idx)) {
                r -= weights.get(idx);
                idx++;
            }
            pair = candidates.get(idx);
        }
        // Link the pair, then flood-fill from the newly joined node next pass
        funAddLink(pair[0], pair[1]);
        reached.add(pair[1]); //Remember that the previous loop only stopped when pair[1] was found to be not a reached point, so now that we've linked it in above statement, it becomes reached, and
        queue.add(pair[1]); //this means when we go back to top of while(true) loop, exploration will resume from this new connection
        bridgesAdded++;
    }
    traceln("Bridging links added: " + bridgesAdded);   // TEST
};

// Prune dead-end links (links with a node of only one link at either end) at random,
// until dead-end links / all links is no more than parDeadEndMaxRatio.
// Removing a dead-end link leaves its dead-end node with no links (isolated).
Runnable pruneDeadEnds = () -> {
    int pruned = 0;
    while (true) {
        // Recount every time: removing a link can turn its other end into a new dead end
        List<Link> deadEndLinks = new ArrayList<>();
        for (Link l : links) {
            if (l.parNodeA.colLinks.size() == 1 || l.parNodeB.colLinks.size() == 1) deadEndLinks.add(l);
        }
        if (deadEndLinks.isEmpty() || (double) deadEndLinks.size() / links.size() <= parDeadEndMaxRatio) break;
        Link l = randomFrom(deadEndLinks);
        l.parNodeA.colLinks.remove(l);
        l.parNodeB.colLinks.remove(l);
        remove_links(l);
        pruned++;
    }
    traceln("Dead-end links pruned: " + pruned);   // TEST
};

// Run the repair stages (comment a line out to switch that stage off)
//connectIsolated.run();
//traceln("Links after isolated-node fix: " + links.size());   // TEST
bridgeClusters.run();
traceln("Links after bridging: " + links.size());   // TEST
pruneDeadEnds.run();
traceln("Links after pruning: " + links.size());   // TEST

// TEST: count grid squares that have both diagonals (should be 0)
int crossings = 0;
for (int i = 0; i + 1 < parCols; i++) { //boundary is i+1<, because we reach right edge and square evaluates rightward
    for (int j = 1; j < parCols; j++) {  //bonudary is j<, because we reach bottom edge and evaluate upwards
        if (funFindLink(varGrid[i][j], varGrid[i+1][j-1]) != null && funFindLink(varGrid[i+1][j], varGrid[i][j-1]) != null) crossings++;
    }
}
traceln("Crossing diagonals: " + crossings);   // TEST

// TEST: count dead ends (1 link) and junctions (3+ links)
int deadEnds = 0;
int junctions = 0;
for (GridPoint p : gridPoints) {
    if (p.colLinks.size() == 1) deadEnds++;
    else if (p.colLinks.size() >= 3) junctions++;
}
traceln("Dead ends: " + deadEnds + ", junctions: " + junctions);   // TEST

traceln("Avg links per node: " + 2.0 * links.size() / gridPoints.size());   // TEST

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
