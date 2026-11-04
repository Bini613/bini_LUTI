// Function in GridPoint. Just action (returns nothing). No arguments.
// Sets varRent, this node's market rent (everyone included), from its stored neighbourhood values:
//   density = varCrowding / varNearbyLand   (agents nearby per m2 of usable land nearby)
//   R = parRentMin + (parRentRef - parRentMin) * (density / varDensityRef)^parRentDensityElasticity
// parRentRef is the rent at the city's average starting density (varDensityRef, set once by funCalibrateDensity).
// parRentMin is the rent with nobody nearby (baseline land value): rent approaches it smoothly as density falls, never below it.
// Does nothing until varDensityRef has been calibrated, so it is safe to call during the first build.
if (main.varDensityRef <= 0 || varNearbyLand <= 0) return;
double density = max(0, varCrowding) / varNearbyLand;   // max(0, ...) guards against tiny negative rounding leftovers after moves
varRent = main.parRentMin + (main.parRentRef - main.parRentMin) * pow(density / main.varDensityRef, main.parRentDensityElasticity);
