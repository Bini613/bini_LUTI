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
        double prob = parBaseLinkProb * funJunctionsFactor(a, b) * funAngleFactor(a, b);
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
            boolean centreDiagonal = (i == ci && j == cj) && d[0] != 0 && d[1] != 0;   // diagonal arterials skip their first step out of the centre, so the centre is a 4-way junction, not 8-way
            if (!centreDiagonal && funFindLink(a, b) == null) {
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

// Stitch dead ends: join each road end (a dead end that is not a stub) to a neighbour that already has links,
// picked at random weighted by the junction and angle factors. If it has none, the road extends onto an empty
// neighbour (same weighting, so straight on is favoured) and tries again from there, until it joins the network
// or has nowhere left to go, in which case it stays a dead end. Dead ends are visited in random order, so there's
// no spatial bias. Stubs (dead-end links whose other end is a junction, 3+ links) are left alone, from either side.
Runnable stitchDeadEnds = () -> {
    int[][] allDirs = {{1,0}, {-1,0}, {0,1}, {0,-1}, {1,1}, {1,-1}, {-1,1}, {-1,-1}};
    List<GridPoint> deadEndNodes = new ArrayList<>();
    Set<GridPoint> stubTips = new HashSet<>();   // dead-end nodes of stubs: never joined, from either side (pruning decides which stubs stay)
    for (GridPoint g : gridPoints) {
        if (g.colLinks.size() != 1) continue;
        Link e = g.colLinks.get(0);
        GridPoint other = (e.parNodeA == g) ? e.parNodeB : e.parNodeA;
        if (other.colLinks.size() >= 3) stubTips.add(g);   // a stub: kept as a dead end
        else deadEndNodes.add(g);
    }
    Collections.shuffle(deadEndNodes, getDefaultRandomGenerator());
    int deadEndsJoined = 0;
    int extensionLinks = 0;
    for (GridPoint p : deadEndNodes) {
        if (p.colLinks.size() != 1) continue;   // no longer a dead end: another dead end joined onto it earlier in this loop
        GridPoint cur = p;   // the current end of the road; moves along as the road extends
        while (true) {
            // Sort cur's valid neighbours: ones that already have links (joining one ends the road there),
            // and empty ones (extending onto one moves the road's end along)
            List<GridPoint> linkedNbrs = new ArrayList<>();
            List<GridPoint> emptyNbrs = new ArrayList<>();
            for (int[] d : allDirs) {
                int ni = cur.parXIndex + d[0];
                int nj = cur.parYIndex + d[1];
                if (ni < 0 || ni >= parCols || nj < 0 || nj >= parCols) continue;   // off grid
                GridPoint b = varGrid[ni][nj];
                if (stubTips.contains(b)) continue;            // the tip of a stub: joining it would remove the stub
                if (funFindLink(cur, b) != null) continue;     // already linked (the road cur is the end of)
                if (d[0] != 0 && d[1] != 0 && funFindLink(varGrid[ni][cur.parYIndex], varGrid[cur.parXIndex][nj]) != null) continue;   // diagonal would cross
                if (b.colLinks.isEmpty()) emptyNbrs.add(b);
                else linkedNbrs.add(b);
            }
            // Join the network if possible; otherwise extend onto an empty neighbour; otherwise stop
            boolean joining = !linkedNbrs.isEmpty();
            List<GridPoint> nbrs = joining ? linkedNbrs : emptyNbrs;
            if (nbrs.isEmpty()) break;   // nowhere left to go: stays a dead end
            // Weighted pick (roulette wheel), as in connectIsolated and bridgeClusters
            List<Double> weights = new ArrayList<>();
            double total = 0;
            for (GridPoint b : nbrs) {
                double w = funJunctionsFactor(cur, b) * funAngleFactor(cur, b);
                weights.add(w);
                total += w;
            }
            GridPoint chosen;
            if (total <= 0) chosen = randomFrom(nbrs);   // every weight is zero: fall back to an unweighted choice
            else {
                double r = uniform(0, total);
                int idx = 0;
                while (idx < nbrs.size() - 1 && r >= weights.get(idx)) {
                    r -= weights.get(idx);
                    idx++;
                }
                chosen = nbrs.get(idx);
            }
            funAddLink(cur, chosen);
            if (joining) {
                deadEndsJoined++;
                break;   // joined the network: done with this road end
            }
            extensionLinks++;
            cur = chosen;   // extended onto an empty node, which is now the road's end
        }
    }
    traceln("Dead ends joined: " + deadEndsJoined + ", extension links added: " + extensionLinks);   // TEST
};

// Prune stubs at random until dead-end links / all links is no more than parDeadEndMaxRatio.
// Removing a stub never creates a new dead end (the junction keeps 2+ links), so roads aren't eaten back.
// Stops early if no stubs are left, even if the ratio is still above the target.
// Removing a stub leaves its dead-end node with no links (isolated).
Runnable pruneStubs = () -> {
    int pruned = 0;
    while (true) {
        // Recount every time, since each removal changes link counts
        int deadEndCount = 0;
        List<Link> stubs = new ArrayList<>();
        for (Link l : links) {
            int dA = l.parNodeA.colLinks.size();
            int dB = l.parNodeB.colLinks.size();
            if (dA == 1 || dB == 1) {
                deadEndCount++;
                if ((dA == 1 && dB >= 3) || (dB == 1 && dA >= 3)) stubs.add(l);
            }
        }
        if (stubs.isEmpty() || (double) deadEndCount / links.size() <= parDeadEndMaxRatio) break;
        Link l = randomFrom(stubs);
        l.parNodeA.colLinks.remove(l);
        l.parNodeB.colLinks.remove(l);
        remove_links(l);
        pruned++;
    }
    traceln("Dead-end stubs pruned: " + pruned);   // TEST
};

// Prune whole chains at random. A chain is a run of links joined end to end through nodes with exactly 2 links;
// it ends at nodes with 1 link (dead end) or 3+ links (junction). Closed loops of 2-link nodes are chains too.
// Each chain gets its length (grid steps) and the average distance of its link midpoints from the centre (grid steps).
// It is then deleted unless it passes both rolls:
//   stays with probability exp(-0.5 * (avgDist * parRadialRoadDispersion)^2)   (far from centre: less likely to stay)
//   is cut with probability exp(-0.5 * (length / parChainCutLength)^2)        (short: more likely to be cut)
// All chains are found and rolled first, then deleted, so deleting one doesn't change the others.
// Chains containing an arterial link are never deleted. parChainCutLength = 0 switches the length roll off.
Runnable pruneChains = () -> {
    // Is link l part of an arterial? (same geometry as the arterial stage: centre row, column and the two diagonals,
    // except the diagonal links touching the centre, which the arterial stage skips)
    java.util.function.Predicate<Link> isArterial = l -> {
        int ax = l.parNodeA.parXIndex - ci, ay = l.parNodeA.parYIndex - cj;
        int bx = l.parNodeB.parXIndex - ci, by = l.parNodeB.parYIndex - cj;
        if (ay == 0 && by == 0) return true;   // horizontal arterial
        if (ax == 0 && bx == 0) return true;   // vertical arterial
        boolean touchesCentre = (ax == 0 && ay == 0) || (bx == 0 && by == 0);
        if (ax == ay && bx == by && !touchesCentre) return true;     // diagonal arterial
        if (ax == -ay && bx == -by && !touchesCentre) return true;   // other diagonal arterial
        return false;
    };

    // Walk a chain from node 'start' along link 'first', until reaching a node without exactly 2 links
    // (or arriving back at 'start', for a closed loop). Returns the chain's links in order.
    java.util.function.BiFunction<GridPoint, Link, List<Link>> walk = (start, first) -> {
        List<Link> chain = new ArrayList<>();
        GridPoint cur = start;
        Link e = first;
        while (true) {
            chain.add(e);
            GridPoint nxt = (e.parNodeA == cur) ? e.parNodeB : e.parNodeA;
            if (nxt.colLinks.size() != 2 || nxt == start) break;   // reached a chain end (or closed the loop)
            e = (nxt.colLinks.get(0) == e) ? nxt.colLinks.get(1) : nxt.colLinks.get(0);   // the link out the other side
            cur = nxt;
        }
        return chain;
    };

    // 1. Find every chain once
    List<List<Link>> chains = new ArrayList<>();
    Set<Link> seen = new HashSet<>();
    for (GridPoint g : gridPoints) {   // open chains: start from each chain end, along each of its links
        if (g.colLinks.size() == 2) continue;
        for (Link first : g.colLinks) {
            if (seen.contains(first)) continue;   // already walked from the chain's other end
            List<Link> chain = walk.apply(g, first);
            seen.addAll(chain);
            chains.add(chain);
        }
    }
    for (Link first : links) {   // closed loops: whatever links are left over
        if (seen.contains(first)) continue;
        List<Link> chain = walk.apply(first.parNodeA, first);
        seen.addAll(chain);
        chains.add(chain);
    }

    // 2. Measure and roll each chain
    List<List<Link>> toDelete = new ArrayList<>();
    int arterialChains = 0;
    for (List<Link> chain : chains) {
        boolean arterial = false;
        double length = 0;    // grid steps (a diagonal link is about 1.41)
        double distSum = 0;   // sum of link midpoint distances from the centre, grid steps
        for (Link l : chain) {
            if (isArterial.test(l)) arterial = true;
            int dx = l.parNodeB.parXIndex - l.parNodeA.parXIndex;
            int dy = l.parNodeB.parYIndex - l.parNodeA.parYIndex;
            length += sqrt(dx * dx + dy * dy);   // 1 for straight links, 1.41 for diagonals
            double mx = (l.parNodeA.parXIndex + l.parNodeB.parXIndex) / 2.0 - ci;
            double my = (l.parNodeA.parYIndex + l.parNodeB.parYIndex) / 2.0 - cj;
            distSum += sqrt(mx * mx + my * my);
        }
        if (arterial) { arterialChains++; continue; }
        double avgDist = distSum / length;
        double zDist = avgDist * parRadialRoadDispersion;
        double pDist = exp(-0.5 * zDist * zDist);
        double pLen= 0;
        if (parChainCutLength > 0) {
            double zLen = length / parChainCutLength;
            pLen = exp(-0.5 * zLen * zLen);
        }
        if (!randomTrue(pDist) || randomTrue(pLen)) toDelete.add(chain);
    }

    // 3. Delete the chosen chains (their nodes stay, possibly with no links)
    int linksRemoved = 0;
    for (List<Link> chain : toDelete) {
        for (Link l : chain) {
            l.parNodeA.colLinks.remove(l);
            l.parNodeB.colLinks.remove(l);
            remove_links(l);
            linksRemoved++;
        }
    }
    traceln("Chains: " + chains.size() + " (" + arterialChains + " on arterials, kept), deleted: " + toDelete.size() + ", links removed: " + linksRemoved);   // TEST
};

// Run the repair stages (comment a line out to switch that stage off)
bridgeClusters.run();
traceln("Links after bridging: " + links.size());   // TEST
// Dead-end smoothing sandwich: stitch road ends, prune stubs, then stitch again.
// Pruning can lower a junction to 2 links, turning its remaining stubs into ordinary road ends; the second stitch joins those.
stitchDeadEnds.run();
pruneStubs.run();
stitchDeadEnds.run();
traceln("Links after smoothing dead ends: " + links.size());   // TEST
pruneChains.run();
traceln("Links after pruning chains: " + links.size());   // TEST

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
