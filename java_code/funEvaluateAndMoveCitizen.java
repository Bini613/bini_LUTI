// Function in Main. Returns value, type: boolean. Arguments: c (Citizen)
// Citizen c's relocation turn: evaluates every candidate node, chooses one (multinomial logit), stores the chosen node's
// values and the expected welfare on c, then moves c there with funMoveCitizen (nothing changes if it stays at home).
// Returns true if c moved, false if it stayed.
// Needs the density surface built (funBuildDensitySurface) and varAccessTimes / varAccessDistances filled (funAssignTraffic).
//
// Candidates: every node within parRelocationRange (metres) of the current home, including home; if home is the only one found,
// the range is widened one grid step at a time until somewhere to move appears. Excluded: nodes at the land floor
// (varAvailableLand <= parMinAvailableLand) and isolated nodes (no links). An unreachable activity stops the run with an error, as this should never be the case.
// Occupied nodes are allowed: any number of citizens and activities may share a node (crowding is priced through rent and privacy).
//
// Utility of candidate k (all terms in rand per month, negative = bad):
//   U_rent = -VOM * R_k                                   R_k = rent excluding c itself: funComputeRent(funComputeDensity(k, home, 1))
//   U_cost = -VOM * f * sum_a( w_a * cost * dist_ka )     f = parTripsPerMonth, cost = parTravelCostPerMetre, dist from varAccessDistances
//   U_time = -VOT * f * sum_a( w_a * t_ka )               t from varAccessTimes (equilibrium travel times, minutes)
//   U_priv = -VOP * parRentRef * rho_k / parDensityRef   rho_k = density excluding c itself; VOP is a multiplier: the crowding cost at the
//                                                         reference density, as a fraction of the reference rent (e.g. 0.1 = like 10% extra rent).
//                                                         VOP can be negative (likes crowding)
//   U_move = -VOM * parRelocationAmortizedCost            only if k is not the current home
//   V_k = U_rent + U_cost + U_time + U_priv + U_move
// Choice probability: P_k = exp(mu * V_k) / sum_j exp(mu * V_j), mu = parLogitScale.
// The largest V is subtracted from each V before exp to avoid overflow (doesn't change the probabilities).
// Terms switched off by a zero coefficient (parTravelCostPerMetre = 0, or this citizen's VOP = 0) are skipped to save computation time.
//
// Stored on c every turn (stay or move), for the node it ends up on (values as seen excluding c itself):
//   varRent        rent (rand per month)                          varURent        = -VOM * varRent
//   varTravelCost  travel cost (rand per month; 0 if switched off) varUTravelCost  = -VOM * varTravelCost
//   varTravelTime  travel time (minutes per month)                varUTravelTime  = -VOT * varTravelTime
//   varPriv        crowding cost scale: parRentRef * density / parDensityRef (rand per month at VOP = 1)
//                                                                 varUPriv        = -VOP * varPriv  (0 if switched off)
//   varExpectedWelfare  logsum over all candidates: maxV + ln(sum_k exp(mu * (V_k - maxV))) / mu  (rand per month; only differences are meaningful)

if (varAccessTimes == null || varAccessDistances == null) error("funEvaluateAndMoveCitizen called before access times exist (run funAssignTraffic first)");

GridPoint home = c.varHome;
int nAct = activities.size();
boolean useCost = parTravelCostPerMetre != 0;
boolean usePriv = c.parVOP != 0;

// Search for candidates within searchRange of home. If home is the only candidate found, widen the range by one grid step
// and search again, so that moving is always an option (stops once the whole grid has been searched).
// Each wider pass only checks the new ring of nodes (beyond the previous range): nodes inside it were already checked,
// and the candidates found there are kept.
double searchRange = parRelocationRange / parStepLength;   // starting search range, in grid steps (all distances in this loop are in grid steps)
double checkedRange = -1;                  // nodes at or within this distance (grid steps) have already been checked (-1: none yet)
GridPoint[] cand = new GridPoint[0];
double[] V = new double[0];
// Per candidate, kept to store the chosen one's on the citizen. Index i is the same term in both arrays:
double[][] vals = new double[0][];    // {rent, travel cost, travel time, priv}
double[][] subUtils = new double[0][];   // {uRent, uTravelCost, uTravelTime, uPriv}
int nCand = 0;
double maxV = Double.NEGATIVE_INFINITY;
while (true) {
    int reach = (int) floor(searchRange);   // range in whole grid steps: only this square around home is checked
    int maxCand = (2 * reach + 1) * (2 * reach + 1);
    if (cand.length < maxCand) {   // make room for the wider search, keeping the candidates found so far
        cand = Arrays.copyOf(cand, maxCand); //keep everything in cand, but extend the array to the new size maxCand. One first run, goes from 0 slots to maxCand starting size. Every pass thereafter, extends by how much maxCand grows when you extend reach at the bottom of the last pass.
        V = Arrays.copyOf(V, maxCand);
        vals = Arrays.copyOf(vals, maxCand);
        subUtils = Arrays.copyOf(subUtils, maxCand);
    }

    for (int x = max(0, home.parXIndex - reach); x <= min(parCols - 1, home.parXIndex + reach); x++) {
        for (int y = max(0, home.parYIndex - reach); y <= min(parCols - 1, home.parYIndex + reach); y++) {
            int dx = x - home.parXIndex, dy = y - home.parYIndex;
            double d = sqrt(dx * dx + dy * dy);   // distance from home in grid steps (dx * dx is much cheaper than pow(dx, 2))
            if (d > searchRange) continue;   // corners of the square lie outside the search circle. When d is equal, node will be checked, hence 'or equal' can be condition below.
            if (d <= checkedRange) continue; // already checked on an earlier pass
            GridPoint k = varGrid[x][y];
            if (k.colLinks.isEmpty()) continue;   // isolated node (no links): can't live there
            boolean isHome = (k == home);
            if (!isHome && k.varAvailableLand <= parMinAvailableLand) continue;     // no land left to live on

            // Travel: weighted sums over all activities of time and (if used) distance from k
            int idx = x * parCols + y; //index in stacked column format
            double sumWeightedTime = 0, sumWeightedDist = 0;
            for (int a = 0; a < nAct; a++) {
                double t = varAccessTimes[a][idx];
                if (t == Double.POSITIVE_INFINITY) { error("An activity is unreachable, grid repair must have failed."); }   // no route to activity a
                sumWeightedTime += c.varActivityWeights[a] * t;
                if (useCost) sumWeightedDist += c.varActivityWeights[a] * varAccessDistances[a][idx];
            }

            // Sub-utilities
            double density = funComputeDensity(k, home, /* ownWeight = */ 1);   // excluding c itself, hence ownWeight = 1
            double rent = funComputeRent(density);                                              // rand per month
            double travelCost = useCost ? parTripsPerMonth * parTravelCostPerMetre * sumWeightedDist : 0;   // rand per month
            double travelTime = parTripsPerMonth * sumWeightedTime;                             // minutes per month
            double priv = usePriv ? parRentRef * density / parDensityRef : 0;                    // rand per month at VOP = 1 (equals parRentRef at the reference density)
            double uRent = -c.parVOM * rent;
            double uTravelCost = -c.parVOM * travelCost;
            double uTravelTime = -c.parVOT * travelTime;
            double uPriv = usePriv ? -c.parVOP * priv : 0;
            double uMove = isHome ? 0 : -c.parVOM * parRelocationAmortizedCost;

            cand[nCand] = k; //gridPoint of candidate added to array of candidate locations
            V[nCand] = uRent + uTravelCost + uTravelTime + uPriv + uMove; //utility of candidate added to array of candidate utilities
            vals[nCand] = new double[]{rent, travelCost, travelTime, priv};
            subUtils[nCand] = new double[]{uRent, uTravelCost, uTravelTime, uPriv};
            maxV = max(maxV, V[nCand]); //keep track of highest utility so far (expected to be least negative)
            nCand++;

            if (c == citizens.get(0) && isHome) funTestPrint("Citizen 0 home (" + x + "," + y + ") utility terms: rent " + roundToDecimal(uRent, 0) + ", travel cost " + roundToDecimal(uTravelCost, 0)
                    + ", travel time " + roundToDecimal(uTravelTime, 0) + ", privacy " + roundToDecimal(uPriv, 0) + ", total " + roundToDecimal(V[nCand - 1], 0));   // TEST. nCand - 1 because we already incremented nCand, but are still talking about he one in this iteration.
        }
    }
    if (nCand > 1) break;              // at least one place to move to: done searching
    double farthestCorner = sqrt(pow(max(home.parXIndex, parCols - 1 - home.parXIndex), 2) + pow(max(home.parYIndex, parCols - 1 - home.parYIndex), 2));   // grid steps
    if (searchRange >= farthestCorner) break;   // the search circle already covers the whole grid: nowhere else to go, so home is the only option
    checkedRange = searchRange;        // everything within the current range has now been checked
    searchRange += 1;                  // only home found: widen the search by one grid step and try again
}
if (nCand == 0) return false;   // safety: nothing valid (should never happen, since home is always a candidate), so stay

// MNL: weights exp(mu * (V - maxV)), then a weighted random pick
double[] e = new double[nCand];
double total = 0;
for (int i = 0; i < nCand; i++) {
    e[i] = exp(parLogitScale * (V[i] - maxV)); //substract max utility so that best (least negative) option has exponent of zero, probabilities won't change because this subtraction can be taken out of exponents of coefficients and factored out of MNL equation and cancelled out with numerator. Scaling term controls the extent to which higher utilities are favoured over lower ones in probability pick.
    total += e[i]; //Denominator for MNL probability equation
}
double r = uniform(0, total); //picks at random from a line of stacked bars, each of their own weight (length)
int pick = 0;
while (pick < nCand - 1 && r >= e[pick]) {   // walk along the stacked weights until r falls inside one
    r -= e[pick];
    pick++;
}

// Store the chosen node's values and the expected welfare (logsum) on the citizen
c.varRent = vals[pick][0];
c.varTravelCost = vals[pick][1];
c.varTravelTime = vals[pick][2];
c.varPriv = vals[pick][3];
c.varURent = subUtils[pick][0];
c.varUTravelCost = subUtils[pick][1];
c.varUTravelTime = subUtils[pick][2];
c.varUPriv = subUtils[pick][3];
c.varExpectedWelfare = maxV + log(total) / parLogitScale;   // total already has maxV factored out (see the MNL loop above)

if (c == citizens.get(0)) funTestPrint("Citizen 0: " + nCand + " candidates, chose (" + cand[pick].parXIndex + "," + cand[pick].parYIndex + ")"
        + (cand[pick] == home ? " (stay)" : "") + " with probability " + roundToDecimal(e[pick] / total, 3) + ", utility " + roundToDecimal(V[pick], 0));   // TEST

// Move to the chosen node (of the candidate list, the one with index 'pick'); funMoveCitizen does nothing if it is home
boolean moved = (cand[pick] != home);
funMoveCitizen(c, cand[pick]);
return moved;
