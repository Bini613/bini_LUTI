varGrid = new GridPoint[parCols][parRows];
for (int i = 0; i < parCols; i++) {
    for (int j = 0; j < parRows; j++) {
        GridPoint p = add_gridPoints(i, j);
        p.setXY(i * parSpacing, j * parSpacing);
        varGrid[i][j] = p;
    }
}

int[][] dirs = {{1,0}, {0,-1}, {1,-1}, {-1,-1}};   // E, N, NE, NW (y points down in AnyLogic)
for (int i = 0; i < parCols; i++) {
    for (int j = 0; j < parRows; j++) {
        for (int[] d : dirs) {
            int ni = i + d[0];
            int nj = j + d[1];
            if (ni < 0 || ni >= parCols || nj < 0 || nj >= parRows) continue;   // off grid
            if (randomTrue(parLinkProb)) {
                GridPoint a = varGrid[i][j];
                GridPoint b = varGrid[ni][nj];
                Link l = add_links(a, b, 0);   // 0 = parLength placeholder; Link's On startup overwrites it
                l.setXY(a.getX(), a.getY());
                a.colLinks.add(l);
                b.colLinks.add(l);
            }
        }
    }
}

// Give every isolated node one link to a random neighbour
int[][] allDirs = {{1,0}, {-1,0}, {0,1}, {0,-1}, {1,1}, {1,-1}, {-1,1}, {-1,-1}};
for (GridPoint p : gridPoints) {
    if (p.colLinks.isEmpty()) {
        List<GridPoint> nbrs = new ArrayList<>();
        for (int[] d : allDirs) {
            int ni = p.parXIndex + d[0];
            int nj = p.parYIndex + d[1];
            if (ni >= 0 && ni < parCols && nj >= 0 && nj < parRows) nbrs.add(varGrid[ni][nj]);
        }
        GridPoint b = randomFrom(nbrs);
        Link l = add_links(p, b, 0);
        l.setXY(p.getX(), p.getY());
        p.colLinks.add(l);
        b.colLinks.add(l);
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
            if (ni >= 0 && ni < parCols && nj >= 0 && nj < parRows && !reached.contains(varGrid[ni][nj]))
                candidates.add(new GridPoint[]{p, varGrid[ni][nj]});//each candidate is a pair of nodes that are disconnected
        }
        for (Link l : p.colLinks) {
            GridPoint q = (l.parNodeA == p) ? l.parNodeB : l.parNodeA;
            if (reached.add(q)) queue.add(q); // Condition here is doing double duty, by adding and returning true or false: if node q hasn't already been reached, then add it to the queue, otherwise don't, and move to next link. When all links are done, the next 'unexplored' node in the queue has the process repeated for all its links ('unexplored'=has not yet been the subject p of the while loop), itself expanding the queue in the process (unless all its connected nodes were already reached).
        }
    }
    if (reached.size() == gridPoints.size()) break;   // all connected. The loop above could have an empty queue but not have explored all regions, since disconnected clusters never entered the queue, because entering the queue requires node q to have been reached by an existing intracluster link. This conditional break here only occurs when all the separate clusters have been explored.

    // Pick a random candidate; discard stale ones whose outside node has been reached since it was recorded
    GridPoint[] pair;
    do {
        int k = uniform_discr(0, candidates.size() - 1);
        pair = candidates.get(k);
        candidates.set(k, candidates.get(candidates.size() - 1));   // remove item k quickly: overwrite it with the last item...
        candidates.remove(candidates.size() - 1);                   // ...then drop the last item
    } while (reached.contains(pair[1])); //the above loop cuts down the candidate list until a valid pair is found, and the loop ceases, regardless of how many valid and invalid pairs remain. Valid in this case means pairs for whom one is not inside the island/cluster called 'reached' found in the previous queue-based while loop ('the flood fill' while loop)
    // Link the pair, then flood-fill from the newly joined node next pass
    Link l = add_links(pair[0], pair[1], 0); //Notice the locatoin of this link bridging islands is randomized because it was randomly drawn from a sea of candidates using the uniform_discr function
    l.setXY(pair[0].getX(), pair[0].getY());
    pair[0].colLinks.add(l);
    pair[1].colLinks.add(l);
    reached.add(pair[1]);
    queue.add(pair[1]);
    bridgesAdded++;
}
traceln("Bridging links added: " + bridgesAdded);   // TEST

traceln("Avg links per node: " + 2.0 * links.size() / gridPoints.size());   // TEST