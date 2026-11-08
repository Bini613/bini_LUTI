// Function in Main. Returns value, type: boolean. Arguments: act (Activity)
// Activity act's relocation turn: evaluates every candidate node, chooses one (multinomial logit), stores the chosen node's
// values and the expected welfare on act, then moves act there with funMoveActivity (nothing changes if it stays).
// Returns true if act moved, false if it stayed.
// Needs the density surface built (funBuildDensitySurface).
//
// Candidates: every node within parRelocationRange (metres) of the current location, including itself; if it is the only one found,
// the range is widened one grid step at a time until somewhere to move appears. Excluded: nodes at the land floor
// (varAvailableLand <= parMinAvailableLand) and isolated nodes (no links).
// Occupied nodes are allowed: any number of activities and citizens may share a node (crowding is priced through rent).
//
// Utility of candidate k (all terms in rand per month, negative = bad):
//   U_rent = -VOM * R_k                          R_k = rent excluding act itself: funComputeRent(funComputeDensity(k, here, parActivityCrowdingWeight))
//   U_loc  = -VOL * D_k                          D_k = weighted average straight-line distance (metres) from k to all citizens' homes,
//                                                weighted by each citizen's weight for this activity: sum_c( w_c * d_kc ) / sum_c( w_c ). Effectively average distance per trip, since each citizen's w_c is their number of trips per direction per day to this activity
//   U_move = -VOM * parRelocationAmortizedCost   only if k is not the current location
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

// Citizens' homes and their weights for this activity, gathered once (reused for every candidate)
int nCit = citizens.size();
int[] hx = new int[nCit];
int[] hy = new int[nCit];
double[] w = new double[nCit];
double sumW = 0;
for (int i = 0; i < nCit; i++) {
    Citizen c = citizens.get(i);
    hx[i] = c.varHome.parXIndex;
    hy[i] = c.varHome.parYIndex;
    w[i] = c.varActivityWeights[ai];
    sumW += w[i];
}
if (sumW <= 0) error("funEvaluateAndMoveActivity: no citizen has any weight for activity " + ai);

// Search for candidates within searchRange of the current location, widening one grid step at a time if current location is the only
// candidate found (as in funEvaluateAndMoveCitizen). Each wider pass only checks the new ring of nodes, keeping earlier candidates.
double searchRange = parRelocationRange / parStepLength;   // starting search range, in grid steps (all distances in this loop are in grid steps)
double checkedRange = -1;                                  // nodes at or within this distance (grid steps) have already been checked (-1: none yet)
GridPoint[] cand = new GridPoint[0];
double[] V = new double[0];
// Per candidate, kept to store the chosen one's on the activity. Index i is the same term in both arrays:
double[][] vals = new double[0][];       // {rent, average distance}
double[][] subUtils = new double[0][];   // {uRent, uLoc}
int nCand = 0;
double maxV = Double.NEGATIVE_INFINITY;
while (true) {
    int reach = (int) floor(searchRange);   // range in whole grid steps: only this square around the current location is checked
    int maxCand = (2 * reach + 1) * (2 * reach + 1);
    if (cand.length < maxCand) {   // make room for the wider search, keeping the candidates found so far
        cand = Arrays.copyOf(cand, maxCand);
        V = Arrays.copyOf(V, maxCand);
        vals = Arrays.copyOf(vals, maxCand);
        subUtils = Arrays.copyOf(subUtils, maxCand);
    }

    for (int x = max(0, here.parXIndex - reach); x <= min(parCols - 1, here.parXIndex + reach); x++) {
        for (int y = max(0, here.parYIndex - reach); y <= min(parCols - 1, here.parYIndex + reach); y++) {
            int dx = x - here.parXIndex, dy = y - here.parYIndex;
            double d = sqrt(dx * dx + dy * dy);   // distance from current location in grid steps (dx * dx is much cheaper than pow(dx, 2))
            if (d > searchRange) continue;   // corners of the square lie outside the search circle
            if (d <= checkedRange) continue; // already checked on an earlier pass
            GridPoint k = varGrid[x][y];
            if (k.colLinks.isEmpty()) continue;   // isolated node (no links): can't locate there
            boolean isHere = (k == here);
            if (!isHere && k.varAvailableLand <= parMinAvailableLand) continue;     // no land left to locate on

            // Weighted average straight-line distance from k to citizens' homes (metres)
            double sumWD = 0;
            for (int i = 0; i < nCit; i++) {
                int cx = x - hx[i], cy = y - hy[i];
                sumWD += w[i] * sqrt(cx * cx + cy * cy);
            }
            double avgDist = sumWD / sumW * parStepLength;

            // Sub-utilities
            double density = funComputeDensity(k, here, /* ownWeight = */ parActivityCrowdingWeight);   // excluding act itself
            double rent = funComputeRent(density);   // rand per month
            double uRent = -act.parVOM * rent;
            double uLoc = -act.parVOL * avgDist;
            double uMove = isHere ? 0 : -act.parVOM * parRelocationAmortizedCost;

            cand[nCand] = k; //gridPoint of candidate added to array of candidate locations
            V[nCand] = uRent + uLoc + uMove;
            vals[nCand] = new double[]{rent, avgDist};
            subUtils[nCand] = new double[]{uRent, uLoc};
            maxV = max(maxV, V[nCand]);
            nCand++;

            if (act == activities.get(0) && isHere) funTestPrint("Activity 0 location (" + x + "," + y + ") utility terms: rent " + roundToDecimal(uRent, 0)
                    + ", location " + roundToDecimal(uLoc, 0) + " (avg distance " + roundToDecimal(avgDist, 0) + " m), total " + roundToDecimal(V[nCand - 1], 0));   // TEST
        }
    }
    if (nCand > 1) break;              // at least one place to move to: done searching
    double farthestCorner = sqrt(pow(max(here.parXIndex, parCols - 1 - here.parXIndex), 2) + pow(max(here.parYIndex, parCols - 1 - here.parYIndex), 2));   // grid steps
    if (searchRange >= farthestCorner) break;   // the search circle already covers the whole grid: nowhere else to go
    checkedRange = searchRange;        // everything within the current range has now been checked
    searchRange += 1;                  // only the current location found: widen the search by one grid step and try again
}
if (nCand == 0) return false;   // safety: nothing valid (should never happen, since the current location is always a candidate), so stay

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
act.varRent = vals[pick][0];
act.varLoc = vals[pick][1];
act.varURent = subUtils[pick][0];
act.varULoc = subUtils[pick][1];
act.varExpectedWelfare = maxV + log(total) / parLogitScale;   // total already has maxV factored out (see the MNL loop above)

if (act == activities.get(0)) funTestPrint("Activity 0: " + nCand + " candidates, chose (" + cand[pick].parXIndex + "," + cand[pick].parYIndex + ")"
        + (cand[pick] == here ? " (stay)" : "") + " with probability " + roundToDecimal(e[pick] / total, 3) + ", utility " + roundToDecimal(V[pick], 0));   // TEST

// Move to the chosen node; funMoveActivity does nothing if it is the current location
boolean moved = (cand[pick] != here);
funMoveActivity(act, cand[pick]);
return moved;
