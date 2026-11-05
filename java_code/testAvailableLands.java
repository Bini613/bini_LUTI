// TEMPORARY TEST. Function in Main. Just action (returns nothing). No arguments.
// Call it in Main's On startup after funCalibrateDensity (so funApplyImpedanceChange can update rent too).
// Finds the node whose roads take the largest share of its cell at the start, then upgrades that node's widest road
// (lowest impedance) to a freeway (parImpedanceMin) and prints the share again.
// Road land is computed without the floor, so a share above 100% means the roads would need more than the whole cell.

// Road land at a node / cell land, same formula as GridPoint's funUpdateAvailableLand but without the floor
java.util.function.ToDoubleFunction<GridPoint> roadLandRatio = g -> {
    double roadLand = 0;
    for (Link l : g.colLinks) roadLand += 0.5 * varRoadWidthRef * l.parLength * pow(l.varImpedance, -parRoadLandUsageExponent);
    return roadLand / parCellLand;
};

// Worst-case node at the start
GridPoint worst = null;
double worstRatio = -1;
for (GridPoint g : gridPoints) {
    double r = roadLandRatio.applyAsDouble(g);
    if (r > worstRatio) { worstRatio = r; worst = g; }
}

// Its widest road = the link with the lowest impedance
Link widest = null;
for (Link l : worst.colLinks) {
    if (widest == null || l.varImpedance < widest.varImpedance) widest = l;
}

traceln("Worst node at start: (" + worst.parXIndex + "," + worst.parYIndex + "), " + worst.colLinks.size() + " links, roads take "
        + roundToDecimal(100 * worstRatio, 1) + "% of the cell, widest road impedance " + roundToDecimal(widest.varImpedance, 2));   // TEST

funApplyImpedanceChange(widest, parImpedanceMin);

traceln("After upgrading that road to a freeway (impedance " + parImpedanceMin + "): roads take "
        + roundToDecimal(100 * roadLandRatio.applyAsDouble(worst), 1) + "% of the cell (above 100% = roads need more than the whole cell), available land now "
        + roundToDecimal(worst.varAvailableLand, 0) + " m^2");   // TEST
