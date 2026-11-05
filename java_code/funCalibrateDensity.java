// Function in Main. Just action (returns nothing). No arguments.
// Run once at startup, right after funBuildDensitySurface. Never re-run: varDensityRef must stay fixed for the whole run.
// Sets varDensityRef to the average starting density over occupied nodes (nodes with a citizen and/or an activity),
// so a typical occupied location pays parRentRef. Empty nodes are left out, so the result doesn't depend on how much
// empty grid surrounds the starting population.
//   varDensityRef = (1/N_occupied) * sum over occupied nodes of (varCrowding / varNearbyLand)
// Then computes every node's rent for the first time.

Set<GridPoint> occupied = new HashSet<>();   // a Set holds each node once, even if a citizen and an activity share it
for (Citizen c : citizens) occupied.add(c.varHome);
for (Activity a : activities) occupied.add(a.varLocation);

double sum = 0;
int count = 0;
for (GridPoint g : occupied) {
    if (g.varNearbyLand <= 0) continue;   // safety: should not happen, since every node keeps at least parMinAvailableLand
    sum += g.varCrowding / g.varNearbyLand; //local density added to sum of all occupied densities
    count++; //count of occupied nodes that are not skipped for having zero land value 
    //count at the end should match occupied.size() since there should be no zero land values, but code is kept this way just in case
}
if (count == 0 || sum <= 0) error("Density calibration failed: no crowding or no land found. Run funBuildDensitySurface first.");
varDensityRef = sum / count;

for (GridPoint g : gridPoints) g.varRent = funComputeRent(funComputeDensity(g, g, /* ownWeight = */ 0)); //With varDensityRef set, we can initialise all rents (everyone included)

// TEST: reference density, rent range over all nodes, and average rent over occupied nodes
// (equals parRentRef exactly when parRentDensityElasticity = 1, close to it otherwise)
double minR = Double.MAX_VALUE, maxR = 0, sumOccupiedR = 0;
for (GridPoint g : gridPoints) {
    minR = min(minR, g.varRent); //Climbs down to minimum rent
    maxR = max(maxR, g.varRent); //Climbs up to maximum rent
}
for (GridPoint g : occupied) sumOccupiedR += g.varRent;
traceln("Density calibrated over " + occupied.size() + " occupied nodes. varDensityRef: " + String.format("%.3e", varDensityRef) + " agents/m2. Rent range: " + roundToDecimal(minR, 0)
        + " to " + roundToDecimal(maxR, 0) + ", average on occupied nodes " + roundToDecimal(sumOccupiedR / occupied.size(), 0) + " (parRentRef " + parRentRef + ", parRentMin " + parRentMin + ")");   // TEST
