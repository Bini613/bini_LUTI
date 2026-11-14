// Function in Main. Returns value, type: boolean. Arguments: act (Activity)
// Activity act's relocation turn: evaluates every candidate node, chooses one (multinomial logit), stores the chosen node's
// values and the expected welfare on act, then moves act there with funMoveActivity (nothing changes if it stays).
// Returns true if act moved, false if it stayed.
// Needs the density surface built (funBuildDensitySurface).
//
// Candidates: every node within parRelocationRange (metres) of the current location, including itself; if it is the only one found,
// the range is widened one grid step at a time until somewhere to move appears. Excluded: nodes at the land floor
// (varAvailableLand <= parMinAvailableLand) and isolated nodes (no links).
// If more nodes qualify than parNumberOfCandidates allows, the current location plus parNumberOfCandidates - 1 others drawn at random are evaluated.
// Occupied nodes are allowed: any number of activities and citizens may share a node (crowding is priced through rent).
//
// Utility of candidate k (all terms in rand per month, negative = bad):
//   U_rent = -VOM * R_k                          R_k = rent excluding act itself: funComputeRent(funComputeDensity(k, here, parActivityCrowdingWeight))
//                                                Switched off (U_rent = 0) when parActivityRentToggle is false; VOM still scales U_move.
//                                                R_k is still computed and stored (varRent) either way, for outputs.
//   U_loc  = -VOL * D_k                          D_k = weighted average straight-line distance (metres) from k to all citizens' homes,
//                                                weighted by each citizen's weight for this activity: sum_c( w_c * d_kc ) / sum_c( w_c ). Effectively average distance per trip, since each citizen's w_c is their number of trips per direction per day to this activity
//   U_move = -VOM * parActivityRelocationAmortizedCost   only if k is not the current location
//                                                (activities' own moving cost, separate from citizens' parRelocationAmortizedCost)
//   V_k = U_rent + U_loc + U_move
// Choice probability: P_k = exp(mu * V_k) / sum_j(exp(mu * V_j)), mu = parLogitScale (shared with citizens' choice).
// The largest V is subtracted from each V before exp to avoid overflow and underflow (doesn't change the probabilities).
//
// Stored on act every turn (stay or move), for the node it ends up on (values as seen excluding act itself):
//   varRent   rent (rand per month)                                    varURent = -VOM * varRent
//   varLoc    weighted average distance to citizens' homes (metres)    varULoc  = -VOL * varLoc
//   varExpectedWelfare  logsum over all candidates: maxV + ln(sum_k exp(mu * (V_k - maxV))) / mu  (rand per month; only differences are meaningful)

GridPoint here = act.varLocation;
int ai = act.getIndex();   // this activity's index: matches the position of its weight in each citizen's varActivityWeights, so long as no agents are added or removed during simulation. Otherwise, different coding solution would be needed.

// Citizens' weights for this activity, summed per home node, gathered once (reused for every candidate).
// Citizens sharing a node are the same distance from any candidate, so each candidate loops over occupied nodes instead of every citizen.
double[] nodeW = new double[parCols * parCols];   // total weight per node, index parIndex
double sumW = 0;
for (Citizen c : citizens) {
    nodeW[c.varHome.parIndex] += c.varActivityWeights[ai];
    sumW += c.varActivityWeights[ai];
}
// Compress to just the nodes with weight
int nOcc = 0;
for (double nodeWeight : nodeW) if (nodeWeight > 0) nOcc++;
int[] hx = new int[nOcc];
int[] hy = new int[nOcc];
double[] w = new double[nOcc];
int i0 = 0;
for (GridPoint g : gridPoints) {
    if (nodeW[g.parIndex] <= 0) continue;
    hx[i0] = g.parXIndex;
    hy[i0] = g.parYIndex;
    w[i0] = nodeW[g.parIndex];
    i0++;
}
if (sumW <= 0) error("funEvaluateAndMoveActivity: no citizen has any weight for activity " + ai);

if (parNumberOfCandidates < 2) error("funEvaluateAndMoveActivity: parNumberOfCandidates must be at least 2 (current location plus one other node)");

// Step 1: gather the eligible nodes other than the current location within searchRange of it (cheap checks only, nothing evaluated yet).
// If none is found, widen the range one grid step at a time (as in funEvaluateAndMoveCitizen). Each wider pass only checks the new ring of nodes.
double searchRange = parRelocationRange / parStepLength;   // starting search range, in grid steps (all distances in this loop are in grid steps)
double checkedRange = -1;                                  // nodes at or within this distance (grid steps) have already been checked (-1: none yet)
ArrayList<GridPoint> pool = new ArrayList<>();   // eligible nodes other than the current location
while (true) {
    int reach = (int) floor(searchRange);   // range in whole grid steps: only this square around the current location is checked
    for (int x = max(0, here.parXIndex - reach); x <= min(parCols - 1, here.parXIndex + reach); x++) {
        for (int y = max(0, here.parYIndex - reach); y <= min(parCols - 1, here.parYIndex + reach); y++) {
            int dx = x - here.parXIndex, dy = y - here.parYIndex;
            double d = sqrt(dx * dx + dy * dy);   // distance from current location in grid steps
            if (d > searchRange) continue;   // corners of the square lie outside the search circle
            if (d <= checkedRange) continue; // already checked on an earlier pass
            GridPoint k = varGrid[x][y];
            if (k == here) continue;                                   // current location is always a candidate, added separately below
            if (k.colLinks.isEmpty()) continue;                        // isolated node (no links): can't locate there
            if (k.varAvailableLand <= parMinAvailableLand) continue;   // no land left to locate on
            pool.add(k);
        }
    }
    if (!pool.isEmpty()) break;        // at least one place to move to: done searching
    double farthestCorner = sqrt(pow(max(here.parXIndex, parCols - 1 - here.parXIndex), 2) + pow(max(here.parYIndex, parCols - 1 - here.parYIndex), 2));   // grid steps
    if (searchRange >= farthestCorner) break;   // the search circle already covers the whole grid: nowhere else to go
    checkedRange = searchRange;        // everything within the current range has now been checked
    searchRange += 1;                  // nothing found: widen the search by one grid step and try again
}

// Step 2: if there are more eligible nodes than allowed, draw parNumberOfCandidates - 1 of them at random (the current location takes the last place).
// Partial shuffle: each of the first m places swaps with a random place at or after it, so the first m nodes are a random draw without repeats.
int m = min(pool.size(), parNumberOfCandidates - 1);
if (pool.size() > m) {
    for (int i = 0; i < m; i++) Collections.swap(pool, i, uniform_discr(i, pool.size() - 1));
}

// Step 3: evaluate the candidates: the current location (index 0) plus the m drawn nodes
int nCand = m + 1;
GridPoint[] cand = new GridPoint[nCand];
cand[0] = here;
for (int i = 0; i < m; i++) cand[i + 1] = pool.get(i);
double[] V = new double[nCand];
// Per candidate values, kept to store the chosen one's on the activity.
// The sub-utilities are not kept: each is just the value times -VOM / -VOL, so they are recomputed for the chosen one only.
double[] candRent = new double[nCand];
double[] candDist = new double[nCand];   // weighted average distance to citizens' homes (metres)
double maxV = Double.NEGATIVE_INFINITY;
for (int i = 0; i < nCand; i++) {
    GridPoint k = cand[i];
    boolean isHere = (i == 0);

    // Weighted average straight-line distance from k to citizens' homes (metres)
    double sumWD = 0;
    for (int j = 0; j < nOcc; j++) {
        int cx = k.parXIndex - hx[j], cy = k.parYIndex - hy[j];
        sumWD += w[j] * sqrt(cx * cx + cy * cy);
    }
    double avgDist = sumWD / sumW * parStepLength;

    // Sub-utilities
    double density = funComputeDensity(k, here, /* ownWeight = */ parActivityCrowdingWeight);   // excluding act itself
    double rent = funComputeRent(density);   // rand per month
    double uRent = parActivityRentToggle ? -act.parVOM * rent : 0;   // rent sub-utility can be switched off globally
    double uLoc = -act.parVOL * avgDist;
    double uMove = isHere ? 0 : -act.parVOM * parActivityRelocationAmortizedCost;

    V[i] = uRent + uLoc + uMove;
    candRent[i] = rent;
    candDist[i] = avgDist;
    maxV = max(maxV, V[i]);

    if (act == activities.get(0) && isHere) funTestPrint("Activity 0 location (" + k.parXIndex + "," + k.parYIndex + ") utility terms: rent " + roundToDecimal(uRent, 0)
            + ", location " + roundToDecimal(uLoc, 0) + " (avg distance " + roundToDecimal(avgDist, 0) + " m), total " + roundToDecimal(V[i], 0));   // TEST
}

// MNL: weights exp(mu * (V - maxV)), then a weighted random pick
double[] e = new double[nCand];
double total = 0;
for (int i = 0; i < nCand; i++) {
    e[i] = exp(parLogitScale * (V[i] - maxV));
    total += e[i];
}
double r = uniform(0, total);
int pick = 0;
while (pick < nCand - 1 && r >= e[pick]) {   // walk along the stacked weights until r falls inside one
    r -= e[pick];
    pick++;
}

// Store the chosen node's values and the expected welfare (logsum) on the activity
act.varRent = candRent[pick];
act.varLoc = candDist[pick];
act.varURent = parActivityRentToggle ? -act.parVOM * act.varRent : 0;   // same formulas as the sub-utilities in the candidate loop
act.varULoc = -act.parVOL * act.varLoc;
act.varExpectedWelfare = maxV + log(total) / parLogitScale;   // total already has maxV factored out (see the MNL loop above)

if (act == activities.get(0)) funTestPrint("Activity 0: " + nCand + " candidates, chose (" + cand[pick].parXIndex + "," + cand[pick].parYIndex + ")"
        + (cand[pick] == here ? " (stay)" : "") + " with probability " + roundToDecimal(e[pick] / total, 3) + ", utility " + roundToDecimal(V[pick], 0));   // TEST

// Move to the chosen node; funMoveActivity does nothing if it is the current location
boolean moved = (cand[pick] != here);
funMoveActivity(act, cand[pick]);
return moved;
