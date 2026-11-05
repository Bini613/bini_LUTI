// Function in Main. Just action (returns nothing). Arguments: l (Link), newImpedance (double)
// Applies a road investment (or any impedance change) to one link and everything that depends on it:
//   1. Link: new impedance, then capacity and travel time from it.
//      Travel time must be refreshed here: funAssignTraffic warm-starts from current link times and checks convergence
//      on its first pass, so a stale time could make it stop before the upgraded link is ever re-timed.
//   2. Land: both end nodes lose (or regain) land, since road land scales with impedance^(-parRoadLandUsageExponent).
//      Each end node's change in land is spread to its neighbourhood's varNearbyLand, which also updates rent there.
// The investment strategy decides which link and what newImpedance (e.g. never below parImpedanceMin); this function just applies it.

l.varImpedance = newImpedance;
l.funUpdateCapacity();
l.funUpdateTravelTime(); //this function inherently will use the updated free flow time, because its freeflow time is not a variable, but the return of funFreeFlowTime function which itself depends on the latest impedance

GridPoint[] ends = {l.parNodeA, l.parNodeB};
for (GridPoint g : ends) {
    double oldLand = g.varAvailableLand;
    g.funUpdateAvailableLand(); //this function actually clears and then recalculates the available land at that node (equivalent to updating, but can also be used in startup land calculation). On this occasion, the update is used to account for impedance of a link changing. g.varAvailableLand will change if the impedance changed.
    double dLand = g.varAvailableLand - oldLand;   // negative when the road is widened
    if (dLand != 0) funUpdateNeighbourhoodDensity(g, 0, dLand); //rent update included in here
}