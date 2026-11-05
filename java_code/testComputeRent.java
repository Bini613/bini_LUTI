// TEMPORARY TEST. Paste in Main's On startup, after funCalibrateDensity();
// Recomputes every node's rent with funComputeRent from its stored density, and compares with the stored varRent
// (set by GridPoint's funUpdateRent, which still has its own copy of the formula). The largest difference should be 0.
double worstDiff = 0;
for (GridPoint g : gridPoints) {
    if (g.varNearbyLand <= 0) continue;   // safety: funUpdateRent skips these too
    double r = funComputeRent(g.varCrowding / g.varNearbyLand);
    worstDiff = max(worstDiff, abs(r - g.varRent));
}
traceln("funComputeRent vs stored rent, largest difference: " + String.format("%.2e", worstDiff) + " (should be 0)");   // TEST
