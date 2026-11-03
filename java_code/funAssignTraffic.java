// Function in Main. Returns value, type: double (the final relative gap). No arguments.
// Equilibrium traffic assignment by the method of successive averages (MSA).
// Needs varCapacityRef calibrated and link capacities set first (funCalibrateCapacity).
// During each pass (with MSA step count n, see warm start below):
//   1. Load all demand onto the current fastest routes (funComputeTargetFlows) -> each link's varTargetFlowVol
//   2. Convergence check (from pass 2 on cold starts): relative gap = (T_actual - T_fastest) / T_actual,
//      T_actual = sum over links of: varFlowVol * varTravelTime (time spent on the current flows),
//      T_fastest = time everyone would spend on the current fastest routes (returned by funComputeTargetFlows).
//      Both use the same current link times. Stop once the gap is below parAssignGapTolerance.
//   3. Blend: varFlowVol += (varTargetFlowVol - varFlowVol) / n   (pass 1 takes the target fully, pass 2 half, pass 3 a third..., helps convergence)
//   4. Update each link's travel time to the congested adjusted travel time using funUpdateTravelTime inside that link
// Ends with every link's varFlowVol and varTravelTime at (or near) equilibrium.

// Warm start: if links already carry flows from a previous assignment (e.g. before a relocation round), keep them
// and start the MSA step count n at parAssignWarmStartStep instead of 1, so the first blend only partly replaces them.
// Larger parAssignWarmStartStep = smaller steps = more trust in the old flows. On the very first call there are no flows, so n starts at 1.
boolean warm = false;
for (Link l : links) {
    if (l.varFlowVol > 0) { warm = true; break; } //warm start is true as soon as first link with flow is detected
}
int nStart = warm ? parAssignWarmStartStep : 1;

double gap = 1;   // relative gap; 1 until it can first be measured (pass 2 on a cold start, pass 1 on a warm start)
int pass;
for (pass = 1; pass <= parAssignMaxIterations; pass++) {
    int n = nStart + pass - 1;   // MSA step count: this pass blends in 1/n of the target

    double tFastest = funComputeTargetFlows();   // Returns the fastest cumulative travel time based on the existing link travel times. Also updates the varTargetFlowVol for each link, as the combined flows that would result from everyone using their fastest routes with the existing travel times. Hence any previous congestion adjustments to link travel times are captured. But until equilibrium is achieved, those flows are not the actual flows, because they don't consider extra readjustments citizens will make in light of the new congestion this routing creates. At equilibrium, the routes the blended flows use are already the fastest, so no route can save anyone time, and the blended flows are final.

    if (warm || pass > 1) {   // 2. on a cold start, pass 1 has no flows yet, so nothing to compare
        double tActual = 0;
        for (Link l : links) tActual += l.varFlowVol * l.varTravelTime; //combined actual travel time. Equivalent to totalFastestTime calculated in funComputeTargetFlows, except using blended flows from previous passes. Links with zero flow contribute zero to the sum, so it's effectively summing through all links that have routings through them. In funComputeTargetFlows method, we grouped by entire routes, adding all their entire times. But each route's entire time is the sum of all the times of all its links, weighted a certain amount. 
        //Here we are breaking apart that sum back into individual links for all the routes, and recombining, this time grouping by links rather than routes. Previously, each link as a component of each route, was the product of its route weight and travel time. Now combining that component for all the routes that passed through this link, will have travel time stay the same, but the weights all add up into the flow volume. So the total contribution of that link to the grand sum, is each passing route's weight*the links travel time: the travel time factors out, and the sum of weights IS the links flow volume.
        gap = (tActual - tFastest) / tActual;
        if (gap < parAssignGapTolerance) break;   // converged: current flows and times are consistent, so stop here
    }

    for (Link l : links) {
        l.varFlowVol += (l.varTargetFlowVol - l.varFlowVol) / n;   // 3. MSA blend
        l.funUpdateTravelTime();                                   // 4. Congestion adjusted time (BPR) from the new flow
    }
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
traceln("Assignment (" + (warm ? "warm start, n from " + nStart : "cold start") + ") " + (converged ? "converged" : "NOT converged") + " after " + min(pass, parAssignMaxIterations) + " passes, gap: " + String.format("%.2e", gap)
        + ", busiest link V/C " + roundToDecimal(varMaxVCRatio, 2) + ", congested/free-flow time: " + roundToDecimal(sumT / sumT0, 3));   // TEST
return gap;
