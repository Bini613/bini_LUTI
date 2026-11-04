// Function in Main. Just action (returns nothing). Arguments: j (GridPoint), dCrowding (double), dLand (double)
// Node j has changed: its crowding by dCrowding (e.g. +1 / -1 when a citizen moves in / out) and/or
// its available land by dLand (new land - old land, after an investment).
// Every node k within the cutoff of j (including j itself, where the weight is 1) receives that change, scaled by distance:
//   varCrowding[k]   += dCrowding * exp(-d / r)
//   varNearbyLand[k] += dLand     * exp(-d / r)
// d = straight-line distance between j and k in metres, r = parRentInfluenceDecayLength (metres), cutoff = parRentInfluenceDecayLength_CutOffMultiple * r.
// The full build (funBuildRentSurface) uses this same function, so the build and later updates always agree.

double cutoff = parRentInfluenceDecayLength_CutOffMultiple * parRentInfluenceDecayLength;          // metres
int reach = (int) ceil(cutoff / parStepLength);          // cutoff in whole grid steps: only this square around j is checked
for (int x = max(0, j.parXIndex - reach); x <= min(parCols - 1, j.parXIndex + reach); x++) {
    for (int y = max(0, j.parYIndex - reach); y <= min(parCols - 1, j.parYIndex + reach); y++) {
        double d = sqrt(pow(x - j.parXIndex, 2) + pow(y - j.parYIndex, 2)) * parStepLength;
        if (d > cutoff) continue;   // corners of the square lie outside the cutoff circle
        double weight = exp(-d / parRentInfluenceDecayLength);
        GridPoint k = varGrid[x][y];
        k.varCrowding += dCrowding * weight;
        k.varNearbyLand += dLand * weight;
        k.funUpdateRent();
    }
}
