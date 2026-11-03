// Function in Citizen. Just action (returns nothing). No arguments. Called from Citizen's On startup.
// Fills varActivityWeights with one weight per activity (index a matches main.activities.get(a)), summing to 1.
// Each raw weight is drawn from gamma(parActivityWeightEvenness, 1), then all are divided by their sum.
// Smaller evenness = each citizen concentrates on a few activities; larger = trips spread nearly evenly.
// Activities must already exist when this runs (funPlaceAgents creates them before citizens).
int n = main.parNumActivities;
varActivityWeights = new double[n];
double total = 0;
for (int a = 0; a < n; a++) {
    varActivityWeights[a] = gamma(main.parActivityWeightEvenness, 1);   // scale 1: it cancels when normalising
    total += varActivityWeights[a];
}
if (total <= 0) {   // safety: with a tiny evenness every draw can round to 0, so fall back to an even split
    for (int a = 0; a < n; a++) varActivityWeights[a] = 1.0 / n;
}
else {
    for (int a = 0; a < n; a++) varActivityWeights[a] /= total;   // normalise
}
