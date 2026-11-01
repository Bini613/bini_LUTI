varGrid = new GridPoint[parCols][parCols];
for (int i = 0; i < parCols; i++) {
    for (int j = 0; j < parCols; j++) {
        GridPoint p = add_gridPoints(i, j);
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
// 2. Shuffle, so the order links are considered in has no spatial bias.
//    Uses AnyLogic's random generator, so runs with the same seed give the same network.
Collections.shuffle(potentialLinks, getDefaultRandomNumberGenerator());
// 3. Accept each with a probability adjusted at both ends for junctions/dead ends and turn sharpness
for (GridPoint[] pl : potentialLinks) {
    GridPoint a = pl[0];
    GridPoint b = pl[1];
    boolean diagonal = a.parXIndex != b.parXIndex && a.parYIndex != b.parYIndex;
    if (diagonal && funFindLink(varGrid[b.parXIndex][a.parYIndex], varGrid[a.parXIndex][b.parYIndex]) != null) continue;   // would cross the square's other diagonal
    double prob = parLinkProb * funJunctionsFactor(a) * funJunctionsFactor(b) * funAngleFactor(a, b) * funAngleFactor(b, a);
    if (randomTrue(min(1.0, prob))) funAddLink(a, b);
}

// Four arterials through the central node: horizontal, vertical, two diagonals
int ci = parCols / 2; //truncates if parCols is odd, Java being zero indexed, this is exact centre. For even parCols, then we get one increment off centre closer to origin.
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

// Give every isolated node one link to a random neighbour (diagonals that would cross an existing link are excluded)
int[][] allDirs = {{1,0}, {-1,0}, {0,1}, {0,-1}, {1,1}, {1,-1}, {-1,1}, {-1,-1}};
for (GridPoint p : gridPoints) {
    if (p.colLinks.isEmpty()) {
        List<GridPoint> nbrs = new ArrayList<>();
        for (int[] d : allDirs) { //this loop builds a list of nieghbours around the grindpoint in question, excluding neighbours for whom a new link would cross an existing link (applicable to diagonals)
            int ni = p.parXIndex + d[0];
            int nj = p.parYIndex + d[1];
            if (ni < 0 || ni >= parCols || nj < 0 || nj >= parCols) continue;   // off grid
            if (d[0] != 0 && d[1] != 0 && funFindLink(varGrid[ni][p.parYIndex], varGrid[p.parXIndex][nj]) != null) continue;   // diagonal would cross
            nbrs.add(varGrid[ni][nj]);
        }
        funAddLink(p, randomFrom(nbrs));
    }
}

// Join separate clusters into one network
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

    // Pick a random candidate; discard stale ones whose outside node has been reached since it was recorded,
    // and diagonal ones that would cross an existing link (checked at pick time, since earlier bridges may have blocked them)
    GridPoint[] pair;
    boolean blocked;
    do {
        int k = uniform_discr(0, candidates.size() - 1);
        pair = candidates.get(k);
        candidates.set(k, candidates.get(candidates.size() - 1));   // remove item k quickly: overwrite it with the last item...
        candidates.remove(candidates.size() - 1);                   // ...then drop the last item
        GridPoint a = pair[0];
        GridPoint b = pair[1];
        blocked = a.parXIndex != b.parXIndex && a.parYIndex != b.parYIndex
                  && funFindLink(varGrid[b.parXIndex][a.parYIndex], varGrid[a.parXIndex][b.parYIndex]) != null;
    } while (reached.contains(pair[1]) || blocked); //the above loop cuts down the candidate list until a valid pair is found, and the loop ceases, regardless of how many valid and invalid pairs remain. Valid in this case means pairs for whom one is not inside the island/cluster called 'reached' found in the previous queue-based while loop ('the flood fill' while loop), and which don't cross an existing diagonal. Horizontal/vertical candidates are never blocked, so a valid pair always remains.
    // Link the pair, then flood-fill from the newly joined node next pass
    funAddLink(pair[0], pair[1]); //Notice the location of this link bridging islands is randomized because it was randomly drawn from a sea of candidates using the uniform_discr function
    reached.add(pair[1]); //Remember that the previous loop only stopped when pair[1] was found to be not a reached point, so now that we've linked it in above statement, it becomes reached, and 
    queue.add(pair[1]); //this means when we go back to top of while(true) loop, exploration will resume from this new connection
    bridgesAdded++;
}
traceln("Bridging links added: " + bridgesAdded);   // TEST

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
