// Function in Main. Just action (returns nothing). No arguments.
// Builds every node's varCrowding and varNearbyLand from scratch, by resetting them to 0 and then letting each node j
// contribute its own crowding n[j] and available land L[j] through funUpdateNeighbourhoodDensity (the same function later updates use).
//   n[j] = citizens living at j + parActivityCrowdingWeight * activities at j
// Needs agents placed and varAvailableLand set (funSetAvailableLands) first.
// Rent is also recomputed along the way, but only once varDensityRef is calibrated (funCalibrateDensity), so at startup call the present function first.

for (GridPoint g : gridPoints) {
    g.varCrowding = 0;
    g.varNearbyLand = 0;
}

double[] n = new double[parCols * parCols];   // crowding at each node, index k = parXIndex * parCols + parYIndex
for (Citizen c : citizens) n[c.varHome.parXIndex * parCols + c.varHome.parYIndex] += 1;
for (Activity a : activities) n[a.varLocation.parXIndex * parCols + a.varLocation.parYIndex] += parActivityCrowdingWeight; //Common universal weight as simplification in this model
//Empty nodes will just have n=0, which will affect densities in funUpdateNeighbourhoodDensity() as intended

for (GridPoint g : gridPoints) {
    funUpdateNeighbourhoodDensity(g, n[g.parXIndex * parCols + g.parYIndex], g.varAvailableLand);// Both arguments are normally changes (dCrowding, dLand). Since this function first resets varCrowding and varNearbyLand to 0,
// the change at each node is from zero to its full value: its crowding n[k] and its available land varAvailableLand.
}
