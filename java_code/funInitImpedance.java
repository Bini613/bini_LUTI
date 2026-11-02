// Function in Main. Just action (returns nothing). No arguments.
// Sets every link's starting impedance and free-flow travel time.
// Arterials are recognised from geometry (both ends on one of the four axes through the centre node),
// so this works the same whether the network was built or loaded.
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
    if (arterial) { //arterial=true if one of the above conditions is met, saying link lies on arterial
        l.varImpedance = parImpedanceArterial;
        arterialCount++;
    }
    else l.varImpedance = parImpedanceLocal + uniform(-parImpedanceLocalSpread, parImpedanceLocalSpread);
    l.varFlowVol = 0;
    l.varTravelTime = l.funFreeFlowTime();   // no congestion until capacities are calibrated
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
