// Function in Citizen. Just action (returns nothing). No arguments. Called from Citizen's On startup.
// Draws this citizen's preferences:
//   varActivityWeights: one weight per activity (index a matches main.activities.get(a)), summing to 1.
//   parVOT, parVOM: value of time and money, each drawn from its own lognormal distribution (always positive).
//   parVOP: value of privacy, drawn from a normal distribution, so it can be negative (see below).
// Activities must already exist when this runs (funPlaceAgents creates them before citizens).

// Activity weights: each raw weight is drawn from gamma(parActivityWeightEvenness, 1), then all are divided by their sum.
// Smaller evenness = each citizen concentrates on a few activities; larger = trips spread nearly evenly.
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

// Lognormal draw from a mean and a relative spread (coefficient of variation, e.g. 0.3 = +-30%).
// A lognormal is exp(normal), so the mean and spread are first converted to those of the underlying normal:
//   sigma^2 = ln(1 + spread^2),  mu = ln(mean) - sigma^2 / 2
java.util.function.DoubleBinaryOperator drawLognormal = (mean, spread) -> {
    if (mean <= 0) return 0;   // a mean of 0 switches that preference off
    double sigma = sqrt(log(1 + spread * spread));
    double mu = log(mean) - sigma * sigma / 2;
    return exp(mu + sigma * normal());   // normal() with no arguments = standard normal (mean 0, sd 1)
};
parVOT = drawLognormal.applyAsDouble(main.parVOTMean, main.parVOTSpread);
parVOM = drawLognormal.applyAsDouble(main.parVOMMean, main.parVOMSpread);

// Value of privacy: normal distribution with mean parVOPMean and standard deviation parVOPStandardDeviation.
// Positive VOP = dislikes crowding; negative VOP = enjoys it (likes vibrant, busy areas for their own sake).
// VOP is a multiplier relative to the reference density and reference rent: the privacy utility is
// -VOP * main.parRentRef * (density / main.varDensityRef), so VOP is the crowding cost of living at the city's typical
// occupied starting density, as a fraction of the reference rent (e.g. 0.1 = feels like 10% extra rent).
// Written as mean + sd * normal() rather than AnyLogic's normal(sd, mean): same distribution, and it simply returns the mean when sd = 0.
// Mean 0 and sd 0 switches privacy off (every citizen gets VOP = 0).
parVOP = main.parVOPMean + main.parVOPStandardDeviation * normal();
