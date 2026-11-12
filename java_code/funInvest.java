// Function in Main. Just action (returns nothing). No arguments. Called by funRunCheckpoint at each density equilibrium
// when welfare hasn't peaked yet (funCheckWelfarePeak), straight after funAssignTraffic, so flows are fresh.
// Upgrades parNumUpgrades links. Link costs are not considered.
//
// Each upgrade:
//   1. Draw a link at random, weighted by score V^2 / C  (= V * V/C: busy AND congested links are favoured).
//      Links with no flow score 0 and are never drawn. Each link can be drawn at most once per investment
//      (scores aren't recomputed between draws, since flows only change at the next assignment).
//   2. New impedance = current - parUpgradeAggresiveness, but never below parImpedanceMin.
//   3. Permanent blacklist (Link variable varUpgradeBlocked) if the link is already at parImpedanceMin, or if the wider
//      road would push either end node's land below parMinAvailableLand. A blacklisted link is skipped and another is
//      drawn in its place. Blacklisting is permanent: impedance only goes down and roads only take more land.
//   4. Otherwise apply it with funApplyImpedanceChange (capacity, travel time, end-node land, nearby density and rent).
// Stops early if no link is left to draw (prints how many upgrades were made).
//
// Random draws use the investment's own generator varInvestRandom, so investing doesn't shift the random sequence of
// the relocations (which would mix the investment's effect with random-number noise when comparing runs).
// Uses:
//   Main parameters  parNumUpgrades (int), parUpgradeAggresiveness (double, impedance subtracted per upgrade), parImpedanceMin
//   Main variable    varInvestRandom (java.util.Random, initial value: new java.util.Random(parSeed + 3))
//   Link variable    varUpgradeBlocked (boolean, initial value false)

Set<Link> drawn = new HashSet<>();   // links already drawn in this investment (upgraded or blacklisted)
int upgrades = 0;
int blacklisted = 0;
while (upgrades < parNumUpgrades) {
    // 1. Weighted draw among links not blacklisted, not yet drawn, and carrying flow
    double total = 0;
    for (Link l : links) {
        if (l.varUpgradeBlocked || drawn.contains(l) || l.varFlowVol <= 0) continue;
        total += l.varFlowVol * l.varFlowVol / l.varCapacity;
    }
    if (total <= 0) break;   // nothing left to upgrade
    double r = varInvestRandom.nextDouble() * total;   // random point along the stacked scores
    Link pick = null;
    for (Link l : links) {
        if (l.varUpgradeBlocked || drawn.contains(l) || l.varFlowVol <= 0) continue;
        pick = l;   // keeps the last eligible link in case rounding leaves r just past the end
        r -= l.varFlowVol * l.varFlowVol / l.varCapacity;
        if (r < 0) break;
    }
    drawn.add(pick);

    // 2. New impedance
    if (pick.varImpedance <= parImpedanceMin) {   // already at the minimum: can't be upgraded
        pick.varUpgradeBlocked = true;
        blacklisted++;
        continue;
    }
    double newImpedance = max(parImpedanceMin, pick.varImpedance - parUpgradeAggresiveness);

    // 3. Land check at both end nodes (unfloored land, same formula as GridPoint.funUpdateAvailableLand)
    double extraRoadLand = 0.5 * varRoadWidthRef * pick.parLength
            * (pow(newImpedance, -parRoadLandUsageExponent) - pow(pick.varImpedance, -parRoadLandUsageExponent));
    boolean landOK = true;
    for (GridPoint g : new GridPoint[] {pick.parNodeA, pick.parNodeB}) {
        double roadLand = 0;
        for (Link l : g.colLinks) roadLand += 0.5 * varRoadWidthRef * l.parLength * pow(l.varImpedance, -parRoadLandUsageExponent);
        if (parCellLand - roadLand - extraRoadLand < parMinAvailableLand) landOK = false;
    }
    if (!landOK) {
        pick.varUpgradeBlocked = true;
        blacklisted++;
        continue;
    }

    // 4. Apply
    funApplyImpedanceChange(pick, newImpedance);
    upgrades++;
}

funRuntimePrint("Investment: " + upgrades + " of " + parNumUpgrades + " links upgraded, " + blacklisted + " newly blacklisted"
        + (upgrades < parNumUpgrades ? " (no more upgradable links)" : ""));
