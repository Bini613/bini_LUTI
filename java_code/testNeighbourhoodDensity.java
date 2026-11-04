// TEMPORARY TEST. Paste at the bottom of Main's On startup.
// Adds 1 crowding at the centre node, prints the crowding it spread along the row to the right (compare with exp(-d/r)),
// then removes it again and checks every node is back to 0.
GridPoint testCentre = varGrid[parCols / 2][parCols / 2];
funUpdateNeighbourhoodDensity(testCentre, 1, 0);

String s = "";
for (int step = 0; testCentre.parXIndex + step < parCols && step * parStepLength <= parRentInfluenceDecayLength_CutOffMultiple * parRentInfluenceDecayLength + parStepLength; step++) { //+ parStepLength too take us one step past cutoff so we can see thst it worked.
    double got = varGrid[testCentre.parXIndex + step][testCentre.parYIndex].varCrowding;
    double expected = exp(-step * parStepLength / parRentInfluenceDecayLength);
    s += step + " steps: " + roundToDecimal(got, 3) + " (exp " + roundToDecimal(expected, 3) + ");  ";
}
traceln("Crowding spread from centre: " + s);   // TEST: last value printed should be 0 (beyond the cutoff)

funUpdateNeighbourhoodDensity(testCentre, -1, 0);
double worst = 0;
for (GridPoint g : gridPoints) worst = max(worst, abs(g.varCrowding));
traceln("After removing it again, largest leftover crowding: " + worst);   // TEST: should be 0 (or a tiny rounding error)
