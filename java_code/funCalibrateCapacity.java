// Function in Main. Just action (returns nothing). No arguments.
// Run once at startup, after impedances are set, agents placed and preferences built.
// Sets varCapacityRef so that, with free-flow routes, the busiest link's V/C equals parTargetVCRatio,
// then sets every link's capacity from it.
//   Each link:     C[l] = C_ref * I[l]^(-gamma),  so  V[l] / C[l] = V[l] * I[l]^gamma / C_ref
//   Busiest link:  the one with the largest V[l] * I[l]^gamma
//   Solving:       C_ref = max over links of (V[l] * I[l]^gamma) / parTargetVCRatio
// Free-flow times don't depend on capacity, so one all-or-nothing load is enough: no iteration needed.

for (Link l : links) l.varTravelTime = l.funFreeFlowTime();   // Initialises each links travel time as it free-flow travel time
funComputeTargetFlows();   // fills each link's varTargetFlowVol (return value not needed here)

double maxVI = 0;   // largest V * I^gamma over all links
for (Link l : links) {
    maxVI = max(maxVI, l.varTargetFlowVol * pow(l.varImpedance, parCapacityGamma)); //updates max if this iteration's value is bigger than previously set max
}
if (maxVI <= 0) error("Capacity calibration failed: no link carries any flow");

varCapacityRef = maxVI / parTargetVCRatio;
for (Link l : links) l.funUpdateCapacity();   // This sets the capacities of each link, which will be unchanged going forwards, until investments are made. C[l] = varCapacityRef * I[l]^(-gamma) . 

// TEST: the busiest link's V/C should equal parTargetVCRatio
double maxVC = 0;
for (Link l : links) maxVC = max(maxVC, l.varTargetFlowVol / l.varCapacity);
traceln("Capacity calibrated. varCapacityRef: " + roundToDecimal(varCapacityRef, 3) + ", busiest link V/C: " + roundToDecimal(maxVC, 3) + " (target: " + parTargetVCRatio + ")");   // TEST
