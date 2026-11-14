// Function in Main. Just action (returns nothing). Arguments: j (GridPoint), dCrowding (double), dLand (double)
// Node j has changed: its crowding by dCrowding (e.g. +1 / -1 when a citizen moves in / out) and/or
// its available land by dLand (new land - old land, after an investment).
// Every node k within the cutoff of j (including j itself, where the weight is 1) receives that change, scaled by distance:
//   varCrowding[k]   += dCrowding * exp(-d / r)
//   varNearbyLand[k] += dLand     * exp(-d / r)
// d = straight-line distance between j and k in metres, r = parRentInfluenceDecayLength (metres), cutoff = parRentInfluenceDecayLength_CutOffMultiple * r.
// The full build (funBuildDensitySurface) uses this same function, so the build and later updates always agree.
// The weights exp(-d / r) are read from varDensityKernel (built by funBuildDensitySurface), indexed by the absolute grid offsets from j.

int reach = varDensityKernel.length - 1;   // cutoff in whole grid steps (size of the kernel table): only this square around j is checked
for (int x = max(0, j.parXIndex - reach); x <= min(parCols - 1, j.parXIndex + reach); x++) {
    for (int y = max(0, j.parYIndex - reach); y <= min(parCols - 1, j.parYIndex + reach); y++) {
        double weight = varDensityKernel[abs(x - j.parXIndex)][abs(y - j.parYIndex)];
        if (weight == 0) continue;   // corners of the square lie outside the cutoff circle
        GridPoint k = varGrid[x][y];
        k.varCrowding += dCrowding * weight;
        k.varNearbyLand += dLand * weight;
        // Update k's stored rent (everyone included). Skipped for inaccessible nodes (no rent, see funBuildDensitySurface),
        // and while funBuildDensitySurface is building the surface, which sets all rents once at the end
        if (!varPauseRentUpdates && !k.colLinks.isEmpty()) {
            k.varRent = funComputeRent(funComputeDensity(k, k, /* ownWeight = */ 0));
        }
    }
}
