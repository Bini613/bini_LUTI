// Function in Main. Returns value, type: GridPoint. Arguments: c (Citizen)
// Citizen c's location choice (multinomial logit). Returns the chosen node, which may be its current home (staying).
// Does not move the citizen: the caller then calls funMoveCitizen(c, chosen), in the same event, so later choices see updated rents.
// Needs varDensityRef calibrated (funCalibrateDensity) and varAccessTimes / varAccessDistances filled (funAssignTraffic).
//
// Candidates: every node within parRelocationRange (metres) of the current home, including home; if home is the only one found,
// the range is widened one grid step at a time until somewhere to move appears. Excluded:
// except nodes already holding another citizen, nodes at the land floor (varAvailableLand <= parMinAvailableLand),
// and isolated nodes (no links). An unreachable activity stops the run with an error, as this should never be the case. Sharing a node with an activity is allowed.
//
// Utility of candidate k (all terms in rand per month, negative = bad):
//   U_rent = -VOM * R_k                                   R_k = rent excluding c itself: funComputeRent(funComputeDensity(k, home, 1))
//   U_cost = -VOM * f * sum_a( w_a * cost * dist_ka )     f = parTripsPerMonth, cost = parTravelCostPerMetre, dist from varAccessDistances
//   U_time = -VOT * f * sum_a( w_a * t_ka )               t from varAccessTimes (equilibrium travel times, minutes)
//   U_priv = -VOP * parRentRef * rho_k / varDensityRef    rho_k = density excluding c itself; VOP is a multiplier: the crowding cost at the
//                                                         reference density, as a fraction of the reference rent (e.g. 0.1 = like 10% extra rent).
//                                                         VOP can be negative (likes crowding)
//   U_move = -VOM * parRelocationAmortizedCost            only if k is not the current home
//   V_k = U_rent + U_cost + U_time + U_priv + U_move
// Choice probability: P_k = exp(mu * V_k) / sum_j exp(mu * V_j), mu = parLogitScale.
// The largest V is subtracted from each V before exp to avoid overflow (doesn't change the probabilities).
// Terms switched off by a zero coefficient (parTravelCostPerMetre = 0, or this citizen's VOP = 0) are skipped to save computation time.

if (varDensityRef <= 0) error("funChooseHome called before varDensityRef was calibrated (run funCalibrateDensity first)");
if (varAccessTimes == null || varAccessDistances == null) error("funChooseHome called before access times exist (run funAssignTraffic first)");

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
int nCand = 0;
double maxV = Double.NEGATIVE_INFINITY;
while (true) {
    int reach = (int) floor(searchRange);   // range in whole grid steps: only this square around home is checked
    int maxCand = (2 * reach + 1) * (2 * reach + 1);
    if (cand.length < maxCand) {   // make room for the wider search, keeping the candidates found so far
        cand = Arrays.copyOf(cand, maxCand); //keep everything in cand, but extend the array to the new size maxCand. One first run, goes from 0 slots to maxCand starting size. Every pass thereafter, extends by how much maxCand grows when you extend reach at the bottom of the last pass.
        V = Arrays.copyOf(V, maxCand);
    }

    for (int x = max(0, home.parXIndex - reach); x <= min(parCols - 1, home.parXIndex + reach); x++) {
        for (int y = max(0, home.parYIndex - reach); y <= min(parCols - 1, home.parYIndex + reach); y++) {
            double d = sqrt(pow(x - home.parXIndex, 2) + pow(y - home.parYIndex, 2));   // distance from home in grid steps
            if (d > searchRange) continue;   // corners of the square lie outside the search circle. When d is equal, node will be checked, hence 'or equal' can be condition below.
            if (d <= checkedRange) continue; // already checked on an earlier pass
            GridPoint k = varGrid[x][y];
            if (k.colLinks.isEmpty()) continue;   // isolated node (no links): can't live there
            boolean isHome = (k == home);
            if (!isHome && k.varResident != null) continue;                         // another citizen lives there
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
            double uRent = -c.parVOM * funComputeRent(density);
            double uTravelCost = useCost ? -c.parVOM * parTripsPerMonth * parTravelCostPerMetre * sumWeightedDist : 0;
            double uTime = -c.parVOT * parTripsPerMonth * sumWeightedTime;
            double uPriv = usePriv ? -c.parVOP * parRentRef * density / varDensityRef : 0;
            double uMove = isHome ? 0 : -c.parVOM * parRelocationAmortizedCost;

            cand[nCand] = k; //gridPoint of candidate added to array of candidate locations
            V[nCand] = uRent + uTravelCost + uTime + uPriv + uMove; //utility of candidate added to array of candidate utilities
            maxV = max(maxV, V[nCand]); //keep track of highest utility so far (expected to be least negative)
            nCand++;

            if (c == citizens.get(0) && isHome) funTestPrint("Citizen 0 home (" + x + "," + y + ") utility terms: rent " + roundToDecimal(uRent, 0) + ", travel cost " + roundToDecimal(uTravelCost, 0)
                    + ", travel time " + roundToDecimal(uTime, 0) + ", privacy " + roundToDecimal(uPriv, 0) + ", total " + roundToDecimal(V[nCand - 1], 0));   // TEST. nCand - 1 because we already incremented nCand, but are still talking about he one in this iteration.
        }
    }
    if (nCand > 1) break;              // at least one place to move to: done searching
    double farthestCorner = sqrt(pow(max(home.parXIndex, parCols - 1 - home.parXIndex), 2) + pow(max(home.parYIndex, parCols - 1 - home.parYIndex), 2));   // grid steps
    if (searchRange >= farthestCorner) break;   // the search circle already covers the whole grid: nowhere else to go, so home is the only option
    checkedRange = searchRange;        // everything within the current range has now been checked
    searchRange += 1;                  // only home found: widen the search by one grid step and try again
}
if (nCand == 0) return home;   // safety: nothing valid (should never happen, since home is always a candidate), so stay

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

if (c == citizens.get(0)) funTestPrint("Citizen 0: " + nCand + " candidates, chose (" + cand[pick].parXIndex + "," + cand[pick].parYIndex + ")"
        + (cand[pick] == home ? " (stay)" : "") + " with probability " + roundToDecimal(e[pick] / total, 3) + ", utility " + roundToDecimal(V[pick], 0));   // TEST
return cand[pick]; //of the candidate list, the one with index 'pick' was chosen, and function returns its gridPoint
