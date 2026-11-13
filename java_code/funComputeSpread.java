// Function in Main. Returns value, type: double. No arguments.
// The citizens' spread: the standard distance of their homes from the citizens' mean position (centroid), in metres,
//   spread = sqrt( (1/N) * sum over citizens of ((x - x_mean)^2 + (y - y_mean)^2) )   (the 2D standard deviation of positions)
// A compact city has a small spread; expansion raises it and contraction lowers it.
// Used by funCheckSpreadEquilibrium (equilibrium check) and testPrintSpread (final report), so both measure it the same way.

// Centroid of the citizens' homes (grid steps), then the mean squared distance from it
double sumX = 0, sumY = 0;
for (Citizen c : citizens) {
    sumX += c.varHome.parXIndex;
    sumY += c.varHome.parYIndex;
}
int nCit = citizens.size();
double meanX = sumX / nCit;
double meanY = sumY / nCit;
double sumSq = 0;
for (Citizen c : citizens) {
    double dx = c.varHome.parXIndex - meanX;
    double dy = c.varHome.parYIndex - meanY;
    sumSq += dx * dx + dy * dy;
}
return sqrt(sumSq / nCit) * parStepLength;   // spread in metres
