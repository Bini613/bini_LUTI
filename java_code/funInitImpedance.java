// Function in Main. Just action (returns nothing). No arguments.
// Sets every link's starting impedance, capacity (from parCapacityRef, via Link funUpdateCapacity) and free-flow travel time.
// Arterials are recognised from geometry (both ends on one of the four axes through the centre node),
// so this works the same whether the network was built or loaded.
// Links touching the centre node itself are kept local, so the centre doesn't start as an 8-way arterial junction
// (whose roads would take more land than the whole cell, especially once upgraded).
// Both road types vary by the same random spread (+- parImpedanceLocalSpread) around their own base value:
// parImpedanceArterial for arterials, parImpedanceLocal for all other links. Setting parImpedanceArterial equal to
// parImpedanceLocal makes arterials ordinary local roads.
int ci = parCols / 2; //As in funBuildNetwork, odd numbers will be truncated,
int cj = parCols / 2; //and considering Java is zero indexed, odd numbers get exact centre index, evens get one increment further from origin
int arterialCount = 0;
for (Link l : links) {
    int xa = l.parNodeA.parXIndex - ci, ya = l.parNodeA.parYIndex - cj;   // node A distance relative to centre
    int xb = l.parNodeB.parXIndex - ci, yb = l.parNodeB.parYIndex - cj;   // node B distance relative to centre
    boolean arterial = (ya == 0 && yb == 0)          // both ends of link are on horizontal axis 
                    || (xa == 0 && xb == 0)          // both ends of link are on vertical axis
                    || (xa == ya && xb == yb)        // both ends of link are on diagonal down-right
                    || (xa == -ya && xb == -yb);     // both ends of link are on diagonal up-right
    boolean touchesCentre = (xa == 0 && ya == 0) || (xb == 0 && yb == 0);   // either end is the centre node
    if (touchesCentre) arterial = false;   // links at the centre stay local
    double base = arterial ? parImpedanceArterial : parImpedanceLocal;   // arterial=true if one of the above conditions is met, saying link lies on arterial
    if (arterial) arterialCount++;
    l.varImpedance = base + uniform(-parImpedanceLocalSpread, parImpedanceLocalSpread);   // same spread for both road types
    l.funUpdateCapacity();   // C = parCapacityRef * I^(-gamma)
    l.varFlowVol = 0;
    l.varTravelTime = l.funFreeFlowTime();   // no flow yet, so no congestion until the first assignment
}

// TEST: arterial count, impedance range and average free-flow time
double minI = Double.MAX_VALUE, maxI = 0, sumT = 0;
for (Link l : links) {
    minI = min(minI, l.varImpedance); //every loop, min impedance is set to new link's impedance if it is smaller than last link's impedance
    maxI = max(maxI, l.varImpedance); //same as above comment, but for impedances that are larger than last link's
    sumT += l.varTravelTime;
}
traceln("Arterial links: " + arterialCount + " of " + links.size());   // TEST
traceln("Impedance range: " + roundToDecimal(minI, 2) + " to " + roundToDecimal(maxI, 2) + ", avg free-flow time: " + roundToDecimal(sumT / links.size(), 2) + " minutes");   // TEST
