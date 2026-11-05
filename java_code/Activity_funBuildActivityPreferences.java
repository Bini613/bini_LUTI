// Function in Activity. Just action (returns nothing). No arguments. Called from Activity's On startup.
// Draws this activity's preferences, each from its own lognormal distribution:
//   parVOM: value of money (sensitivity to rent)
//   parVOL: value of location (rand per metre of weighted average distance to citizens)

// Lognormal draw from a mean and a relative spread, as in Citizen's funBuildPreferences:
//   sigma^2 = ln(1 + spread^2),  mu = ln(mean) - sigma^2 / 2
java.util.function.DoubleBinaryOperator drawLognormal = (mean, spread) -> {
    if (mean <= 0) return 0;   // a mean of 0 switches that preference off
    double sigma = sqrt(log(1 + spread * spread));
    double mu = log(mean) - sigma * sigma / 2;
    return exp(mu + sigma * normal());   // normal() with no arguments = standard normal (mean 0, sd 1)
};
parVOM = drawLognormal.applyAsDouble(main.parActivityVOMMean, main.parActivityVOMSpread);
parVOL = drawLognormal.applyAsDouble(main.parActivityVOLMean, main.parActivityVOLSpread);
