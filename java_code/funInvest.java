// Function in Main. Returns value, type: boolean. No arguments. Called by funRunCheckpoint at each spread equilibrium
// when welfare hasn't peaked yet (funCheckWelfarePeak), straight after funAssignTraffic, so flows are fresh.
// Upgrades parNumUpgrades links. Link costs are not considered.
// Returns true if any link's impedance changed, false if no link could be upgraded at all (funRunCheckpoint then ends the run).
//
// Each upgrade:
//   1. Draw a link at random, weighted by score V^2 / C  (= V * V/C: busy AND congested links are favoured).
//      Links with no flow score 0 and are never drawn. Each link can be drawn at most once per investment
//      (scores aren't recomputed between draws, since flows only change at the next assignment).
//   2. New impedance = current - parUpgradeAggresiveness, but never below parImpedanceMin.
//   3. Land check: the wider road must not push either end node's land below parMinAvailableLand.
//      If it would, the link is instead upgraded as far as the land allows (if at all), then blacklisted.
//      That partial upgrade doesn't count towards parNumUpgrades, so an investment can upgrade slightly more than planned.
//   4. Otherwise apply the full step with funApplyImpedanceChange (capacity, travel time, end-node land, nearby density and rent).
//      A link that reaches parImpedanceMin is blacklisted too.
// Blacklisted links (Link variable varUpgradeBlocked) are never drawn again. Blacklisting is permanent: impedance only
// goes down and roads only take more land. Stops early if no link is left to draw.
//
// Land-limited impedance: with slack S = the smaller of the two end nodes' (parCellLand - road land - parMinAvailableLand),
// the road may grow by at most S:  0.5 * w_ref * L * (I_new^-delta - I^-delta) <= S
//   =>  I_new = (I^-delta + S / (0.5 * w_ref * L))^(-1/delta)
//
// Random draws use the investment's own generator varInvestRandom, so investing doesn't shift the random sequence of
// the relocations (which would mix the investment's effect with random-number noise when comparing runs).
// Uses:
//   Main parameters  parNumUpgrades (int), parUpgradeAggresiveness (double, impedance subtracted per upgrade), parImpedanceMin
//   Main variable    varInvestRandom (java.util.Random, initial value: new java.util.Random(parSeed + 3))
//   Link variable    varUpgradeBlocked (boolean, initial value false)

double delta = parRoadLandUsageExponent;
Set<Link> drawn = new HashSet<>();   // links already drawn in this investment (upgraded or blacklisted)
int upgrades = 0;          // full steps (count towards parNumUpgrades)
int partialUpgrades = 0;   // land-limited upgrades just before blacklisting (don't count)
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
    double halfArea = 0.5 * varRoadWidthRef * pick.parLength;   // road land per unit of I^-delta, at each end node
    double slack = Double.POSITIVE_INFINITY;                     // land each end node can still give up (smaller of the two)
    for (GridPoint g : new GridPoint[] {pick.parNodeA, pick.parNodeB}) {
        double roadLand = 0;
        for (Link l : g.colLinks) roadLand += 0.5 * varRoadWidthRef * l.parLength * pow(l.varImpedance, -delta);
        slack = min(slack, parCellLand - roadLand - parMinAvailableLand);
    }
    double extraRoadLand = halfArea * (pow(newImpedance, -delta) - pow(pick.varImpedance, -delta));
    if (extraRoadLand > slack) {   // full step needs too much land: upgrade as far as the land allows, then blacklist
        if (slack > 0) {
            double landLimitedImpedance = pow(pow(pick.varImpedance, -delta) + slack / halfArea, -1 / delta);
            funApplyImpedanceChange(pick, landLimitedImpedance);
            partialUpgrades++;
        }
        pick.varUpgradeBlocked = true; // If there was some land left, if statement above fills it with road, then we block it from future upgrades here. If there already was no land left, then jump straight to blocking it here.
        blacklisted++;
        continue;
    }

    // 4. Apply the full step
    funApplyImpedanceChange(pick, newImpedance);
    upgrades++;
    if (newImpedance <= parImpedanceMin) {   // reached the minimum: no further upgrades possible
        pick.varUpgradeBlocked = true;
        blacklisted++;
    }
}

funRuntimePrint("Investment at turn " + varTurnCount + " (round " + varRounds + "): "
				+ upgrades + " of " + parNumUpgrades + " links upgraded, " + partialUpgrades + " land-limited upgrades, "
      			+ blacklisted + " newly blacklisted" + (upgrades < parNumUpgrades ? " (no more upgradable links)" : ""));
return upgrades + partialUpgrades > 0;
