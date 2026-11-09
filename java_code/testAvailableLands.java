// TEMPORARY TEST. Function in Main. Just action (returns nothing). No arguments.
// Call it in Main's On startup after funBuildDensitySurface (so funApplyImpedanceChange can update rent too).
// Finds the node whose roads take the largest share of its cell at the start, then upgrades that node's widest road
// (lowest impedance) to a freeway (parImpedanceMin) and prints the share again.
// Part 2 (bottom): finds a junction where an arterial crosses a local road, and upgrades it step by step up to a
// freeway crossing a freeway, printing the share after each step (the worst case to tune parameters against).
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

// Part 2: junction of an arterial and a local road.
// Looks for a node with exactly 2 arterial links (the arterial passing through) and exactly 2 local links,
// preferring one where the two local links point in opposite directions (a local road crossing straight over the arterial).
// Skips the node upgraded above, so its earlier upgrade doesn't affect this check.
// Arterials are recognised from geometry, with the same rule as funInitImpedance (both ends on one of the four axes
// through the centre node, links touching the centre excluded). Impedance can't be used: both road types get a random
// spread, so no arterial has exactly parImpedanceArterial, and the two ranges can overlap.
int ci = parCols / 2;
int cj = parCols / 2;
java.util.function.Predicate<Link> isArterial = l -> {
    int xa = l.parNodeA.parXIndex - ci, ya = l.parNodeA.parYIndex - cj;
    int xb = l.parNodeB.parXIndex - ci, yb = l.parNodeB.parYIndex - cj;
    if ((xa == 0 && ya == 0) || (xb == 0 && yb == 0)) return false;   // touches the centre: kept local
    return (ya == 0 && yb == 0) || (xa == 0 && xb == 0) || (xa == ya && xb == yb) || (xa == -ya && xb == -yb);
};
GridPoint junction = null;
List<Link> jArterials = null, jLocals = null;
for (GridPoint g : gridPoints) {
    if (g == worst) continue;
    List<Link> art = new ArrayList<>();
    List<Link> loc = new ArrayList<>();
    for (Link l : g.colLinks) {
        if (isArterial.test(l)) art.add(l);
        else loc.add(l);
    }
    if (art.size() != 2 || loc.size() != 2) continue;
    GridPoint o1 = (loc.get(0).parNodeA == g) ? loc.get(0).parNodeB : loc.get(0).parNodeA;   // far end of each local link
    GridPoint o2 = (loc.get(1).parNodeA == g) ? loc.get(1).parNodeB : loc.get(1).parNodeA;
    boolean straight = (o1.parXIndex - g.parXIndex) == -(o2.parXIndex - g.parXIndex)
                    && (o1.parYIndex - g.parYIndex) == -(o2.parYIndex - g.parYIndex);   // opposite directions from g
    if (junction == null || straight) {   // take the first match, but replace it with a straight crossing if one turns up
        junction = g; jArterials = art; jLocals = loc;
    }
    if (straight) break;
}
if (junction == null) {
    traceln("No node found with exactly 2 arterial and 2 local links");   // TEST
    return;
}

// Prints the junction's road share and its available land (after the floor)
java.util.function.BiConsumer<String, GridPoint> report = (label, g) ->
    traceln(label + ": roads take " + roundToDecimal(100 * roadLandRatio.applyAsDouble(g), 1) + "% of the cell, available land: "
            + roundToDecimal(g.varAvailableLand, 0) + " m^2");   // TEST

report.accept("Junction (" + junction.parXIndex + "," + junction.parYIndex + "), " + junction.colLinks.size() + " links, at start", junction);

for (Link l : jLocals) funApplyImpedanceChange(l, parImpedanceArterial);
report.accept("  Two local roads -> arterials (impedance " + parImpedanceArterial + ")", junction);

for (Link l : jLocals) funApplyImpedanceChange(l, parImpedanceMin);
report.accept("  Two local roads -> freeways (impedance " + parImpedanceMin + ")", junction);

for (Link l : jArterials) funApplyImpedanceChange(l, parImpedanceMin);
report.accept("  Arterial -> freeway too: freeway crossing freeway", junction);
