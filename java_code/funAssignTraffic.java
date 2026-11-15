// Function in Main. Returns value, type: double (the final relative gap). No arguments.
// Equilibrium traffic assignment by the Frank-Wolfe algorithm.
// Needs link capacities set first (funInitImpedance, via Link funUpdateCapacity).
// During each pass:
//   1. Load all demand onto the current fastest routes (funComputeTargetFlows) -> each link's varTargetFlowVol
//   2. Convergence check (from pass 2 on cold starts): relative gap = (T_actual - T_fastest) / T_actual,
//      T_actual = sum over links of: varFlowVol * varTravelTime (time spent on the current flows),
//      T_fastest = time everyone would spend on the current fastest routes (returned by funComputeTargetFlows).
//      Both use the same current link times. Stop once the gap is below parAssignGapTolerance.
//   3. Blend: varFlowVol += lambda * (varTargetFlowVol - varFlowVol), with lambda (0 to 1) found by bisection as the step that
//      minimises the Beckmann objective Z along the line from the current flows to the target (line search, see 'slope' below). Objective Z is a measure for which minimisation results in the selfish equilibrium.
//      A cold start's first pass takes the target fully (lambda = 1).
//   4. Update each link's travel time to the congested adjusted travel time using funUpdateTravelTime inside that link
// Ends with every link's varFlowVol and varTravelTime at (or near) equilibrium,
// and varAccessTimes and varAccessDistances (filled by funComputeTargetFlows) holding the times and fastest-route-distances from every node to every activity.
// If it converges, the stop comes before the last time update, so varAccessTimes matches the final link times exactly. Otherwise one more funUpdateTravelTime() will occur, but with enough steps permitted, then that final travel times it calculates will be only marginally different from the varAccessTimes found in the last loop. 

// Warm start: if links already carry flows from a previous assignment (e.g. before a relocation round), start from them;
// the line search then decides how much each pass replaces. On a cold start (no flows) the first pass takes the target fully.
boolean warm = false;
for (Link l : links) {
    if (l.varFlowVol > 0) { warm = true; break; } //warm start is true as soon as first link with flow is detected
}

// Node demand, built once per assignment: nodeDemand[a][k] = total weight of the citizens living at node k for activity a.
// Citizens don't move during an assignment, so it is the same on every pass; only the routes (and so the link flows) change.
int nNodes = parCols * parCols;
double[][] nodeDemand = new double[activities.size()][nNodes];
for (Citizen c : citizens) {
    int k = c.varHome.parIndex;   // stacked column index of the citizen's home
    for (int a = 0; a < activities.size(); a++) nodeDemand[a][k] += c.varActivityWeights[a];   // += so citizens sharing a node add up
}

double gap = 1;   // relative gap; 1 until it can first be measured (pass 2 on a cold start, pass 1 on a warm start)

// Frank-Wolfe line-search slope: dZ/dlambda along the straight line from the current flows to the target flows.
//   dZ/dlambda = sum over links of (target - flow) * BPR time at the blended flow (flow + lambda * (target - flow))
// Z = Beckmann objective (sum over links of the area under the BPR curve up to the link's flow); its minimum is the selfish equilibrium.
// Reads each link's current flow and target when called, so this one definition serves every pass.
java.util.function.DoubleUnaryOperator slope = lambda -> {
    double s = 0;
    for (Link l : links) {
        double d = l.varTargetFlowVol - l.varFlowVol;
        if (d == 0) continue;   // flow doesn't change along the line: contributes nothing
        double v = l.varFlowVol + lambda * d; //Hypothetical blended flow for this lambda
        s += d * l.funFreeFlowTime() * (1 + parCongestionAlpha * pow(v / l.varCapacity, parCongestionBeta));   // same BPR as funUpdateTravelTime
    }
    return s;
};

int pass;
for (pass = 1; pass <= parAssignMaxIterations; pass++) {
    double tFastest = funComputeTargetFlows(nodeDemand);   // Returns the fastest cumulative travel time based on the existing link travel times. Also updates the varTargetFlowVol for each link, as the combined flows that would result from everyone using their fastest routes with the existing travel times. Hence any previous congestion adjustments to link travel times are captured. But until equilibrium is achieved, those flows are not the actual flows, because they don't consider extra readjustments citizens will make in light of the new congestion this routing creates. At equilibrium, the routes the blended flows use are already the fastest, so no route can save anyone time, and the blended flows are final.

    if (warm || pass > 1) {   // 2. on a cold start, pass 1 has no flows yet, so nothing to compare
        double tActual = 0;
        for (Link l : links) tActual += l.varFlowVol * l.varTravelTime; //combined actual travel time. Equivalent to totalFastestTime calculated in funComputeTargetFlows, except using blended flows from previous passes. Links with zero flow contribute zero to the sum, so it's effectively summing through all links that have routings through them. In funComputeTargetFlows method, we grouped by entire routes, adding all their entire times. But each route's entire time is the sum of all the times of all its links, weighted a certain amount. 
        //Here we are breaking apart that sum back into individual links for all the routes, and recombining, this time grouping by links rather than routes. Previously, each link as a component of each route, was the product of its route weight and travel time. Now combining that component for all the routes that passed through this link, will have travel time stay the same, but the weights all add up into the flow volume. So the total contribution of that link to the grand sum, is each passing route's weight*the links travel time: the travel time factors out, and the sum of weights IS the links flow volume.
        gap = (tActual - tFastest) / tActual;
        if (gap < parAssignGapTolerance) break;   // converged: current flows and times are consistent, so stop here
    }

    // 3. Step size: the lambda in [0,1] that minimises Z along the line from the current flows to the target.
    //    The slope only rises with lambda (BPR times rise with flow), so it crosses zero at most once.
    double lambda;
    if (!warm && pass == 1) lambda = 1;               // cold start: no flows yet (no demand routed), so take the target fully
    else if (slope.applyAsDouble(1) <= 0) lambda = 1; // Z still falling at the target: the full step is best
    else {                                            // bisection: slope < 0 at 0 (target is downhill), > 0 at 1, find the crossing
        double lo = 0, hi = 1;
        for (int i = 0; i < 20; i++) {                // 20 halvings: lambda to within 1e-6
            double mid = 0.5 * (lo + hi);
            if (slope.applyAsDouble(mid) > 0) hi = mid; else lo = mid;
        }
        lambda = 0.5 * (lo + hi);                     // midpoint of the final bracket
    }

    for (Link l : links) {
        l.varFlowVol += lambda * (l.varTargetFlowVol - l.varFlowVol);   // move lambda of the way to the target
        l.funUpdateTravelTime();                                        // 4. Congestion adjusted time (BPR) from the new flow
    }
    funClearTreeCache();   // 5. link times just changed, so every cached fastest-path tree is out of date: the next pass builds fresh ones.
                           //    A converged assignment stops at the gap check above, before this point, so its final pass's trees stay cached
                           //    (they match the final link times). A capped assignment ends here, with the cache empty.
}// Effectively, the first pass creates the initial target flows, and updates the times based on that. Every subsequent pass updates the target based on the last one's times, then reblends the flows, and then updates the times accordingly, ready for the next pass. Repeated until convergence condition is met. 

// Largest V/C in the network (Main variable), used to scale link colours from gray (least) to red (most congested)
varMaxVCRatio = 0;
for (Link l : links) varMaxVCRatio = max(varMaxVCRatio, l.varFlowVol / l.varCapacity);

// TEST: passes used, final gap, busiest link V/C, and how much slower the network is than free-flow overall
double sumT = 0, sumT0 = 0;
for (Link l : links) {
    sumT += l.varFlowVol * l.varTravelTime;
    sumT0 += l.varFlowVol * l.funFreeFlowTime();
}
boolean converged = pass <= parAssignMaxIterations;
funTestPrint("Assignment (" + (warm ? "warm start" : "cold start") + ") " + (converged ? "converged" : "NOT converged") + " after " + min(pass, parAssignMaxIterations) + " passes, gap: " + String.format("%.2e", gap)
        + ", busiest link V/C " + roundToDecimal(varMaxVCRatio, 2) + ", congested/free-flow time: " + roundToDecimal(sumT / sumT0, 3));   // TEST
// Runtime check: report every assignment that hit the iteration ceiling (parAssignMaxIterations) without reaching parAssignGapTolerance.
// Should be rare; frequent reports mean the tolerance is too tight, the ceiling too low, or assignments come too often / after too big a change.
if (!converged) {
    varAssignCapHits++;   // Main variable (int, initial 0): running count of capped assignments
    funRuntimePrint("Assignment hit the iteration ceiling (" + parAssignMaxIterations + " passes), gap " + String.format("%.2f", gap * 100) + "%"
            + " (tolerance " + String.format("%.2f", parAssignGapTolerance * 100) + "%). Capped assignments so far: " + varAssignCapHits + ", turn " + varTurnCount);
}
return gap;
