// Function in Main. Returns value, type: boolean. Arguments: c (Citizen)
// Citizen c's relocation turn: evaluates every candidate node, chooses one (multinomial logit), stores the chosen node's
// values and the expected welfare on c, then moves c there with funMoveCitizen (nothing changes if it stays at home).
// Returns true if c moved, false if it stayed.
// Needs the density surface built (funBuildDensitySurface) and varAccessTimes / varAccessDistances filled (funAssignTraffic).
//
// Candidates: every node within parRelocationRange (metres) of the current home, including home; if home is the only one found,
// the range is widened one grid step at a time until somewhere to move appears. Excluded: nodes at the land floor
// (varAvailableLand <= parMinAvailableLand) and isolated nodes (no links). An unreachable activity stops the run with an error, as this should never be the case.
// If more nodes qualify than parNumberOfCandidates allows, home plus parNumberOfCandidates - 1 others drawn at random are evaluated.
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

if (parNumberOfCandidates < 2) error("funEvaluateAndMoveCitizen: parNumberOfCandidates must be at least 2 (home plus one other node)");

// Step 1: gather the eligible nodes other than home within searchRange of home (cheap checks only, nothing evaluated yet).
// If none is found, widen the range by one grid step and search again, so that moving is always an option
// (stops once the whole grid has been searched). Each wider pass only checks the new ring of nodes (beyond the previous range).
double searchRange = parRelocationRange / parStepLength;   // starting search range, in grid steps (all distances in this loop are in grid steps)
double checkedRange = -1;                  // nodes at or within this distance (grid steps) have already been checked (-1: none yet)
ArrayList<GridPoint> pool = new ArrayList<>();   // eligible nodes other than home
while (true) {
    int reach = (int) floor(searchRange);   // range in whole grid steps: only this square around home is checked
    for (int x = max(0, home.parXIndex - reach); x <= min(parCols - 1, home.parXIndex + reach); x++) {
        for (int y = max(0, home.parYIndex - reach); y <= min(parCols - 1, home.parYIndex + reach); y++) {
            int dx = x - home.parXIndex, dy = y - home.parYIndex;
            double d = sqrt(dx * dx + dy * dy);   // distance from home in grid steps
            if (d > searchRange) continue;   // corners of the square lie outside the search circle
            if (d <= checkedRange) continue; // already checked on an earlier pass
            GridPoint k = varGrid[x][y];
            if (k == home) continue;                                   // home is always a candidate, added separately below
            if (k.colLinks.isEmpty()) continue;                        // isolated node (no links): can't live there
            if (k.varAvailableLand <= parMinAvailableLand) continue;   // no land left to live on
            pool.add(k);
        }
    }
    if (!pool.isEmpty()) break;        // at least one place to move to: done searching
    double farthestCorner = sqrt(pow(max(home.parXIndex, parCols - 1 - home.parXIndex), 2) + pow(max(home.parYIndex, parCols - 1 - home.parYIndex), 2));   // grid steps
    if (searchRange >= farthestCorner) break;   // the search circle already covers the whole grid: nowhere else to go, so home is the only option
    checkedRange = searchRange;        // everything within the current range has now been checked
    searchRange += 1;                  // nothing found: widen the search by one grid step and try again
}

// Step 2: if there are more eligible nodes than allowed, draw parNumberOfCandidates - 1 of them at random (home takes the last place).
// Partial shuffle: each of the first m places swaps with a random place at or after it, so the first m nodes are a random draw without repeats.
int m = min(pool.size(), parNumberOfCandidates - 1);
if (pool.size() > m) {
    for (int i = 0; i < m; i++) Collections.swap(pool, i, uniform_discr(i, pool.size() - 1));
}

// Step 3: evaluate the candidates: home (index 0) plus the m drawn nodes
int nCand = m + 1;
GridPoint[] cand = new GridPoint[nCand];
cand[0] = home;
for (int i = 0; i < m; i++) cand[i + 1] = pool.get(i);
double[] V = new double[nCand];
// Per candidate values, kept to store the chosen one's on the citizen.
// The sub-utilities are not kept: each is just the value times -VOM / -VOT / -VOP, so they are recomputed for the chosen one only.
double[] candRent = new double[nCand];
double[] candTravelCost = new double[nCand];
double[] candTravelTime = new double[nCand];
double[] candPriv = new double[nCand];
double maxV = Double.NEGATIVE_INFINITY;
for (int i = 0; i < nCand; i++) {
    GridPoint k = cand[i];
    boolean isHome = (i == 0);

    // Travel: weighted sums over all activities of time and (if used) distance from k
    int idx = k.parIndex;
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

    V[i] = uRent + uTravelCost + uTravelTime + uPriv + uMove;
    candRent[i] = rent;
    candTravelCost[i] = travelCost;
    candTravelTime[i] = travelTime;
    candPriv[i] = priv;
    maxV = max(maxV, V[i]); //keep track of highest utility so far (expected to be least negative)

    if (c == citizens.get(0) && isHome) funTestPrint("Citizen 0 home (" + k.parXIndex + "," + k.parYIndex + ") utility terms: rent " + roundToDecimal(uRent, 0) + ", travel cost " + roundToDecimal(uTravelCost, 0)
            + ", travel time " + roundToDecimal(uTravelTime, 0) + ", privacy " + roundToDecimal(uPriv, 0) + ", total " + roundToDecimal(V[i], 0));   // TEST
}

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
c.varRent = candRent[pick];
c.varTravelCost = candTravelCost[pick];
c.varTravelTime = candTravelTime[pick];
c.varPriv = candPriv[pick];
c.varURent = -c.parVOM * c.varRent;                // same formulas as the sub-utilities in the candidate loop
c.varUTravelCost = -c.parVOM * c.varTravelCost;    // 0 when cost is off (travelCost was 0)
c.varUTravelTime = -c.parVOT * c.varTravelTime;
c.varUPriv = usePriv ? -c.parVOP * c.varPriv : 0;
c.varExpectedWelfare = maxV + log(total) / parLogitScale;   // total already has maxV factored out (see the MNL loop above)

if (c == citizens.get(0)) funTestPrint("Citizen 0: " + nCand + " candidates, chose (" + cand[pick].parXIndex + "," + cand[pick].parYIndex + ")"
        + (cand[pick] == home ? " (stay)" : "") + " with probability " + roundToDecimal(e[pick] / total, 3) + ", utility " + roundToDecimal(V[pick], 0));   // TEST

// Move to the chosen node (of the candidate list, the one with index 'pick'); funMoveCitizen does nothing if it is home
boolean moved = (cand[pick] != home);
funMoveCitizen(c, cand[pick]);
return moved;
