// Function in Activity. Just action (returns nothing). No arguments. Called by funPlaceAgents right after each activity is created (not when a saved state is loaded).
// Draws this activity's preferences, each from its own lognormal distribution:
//   parVOM: value of money (sensitivity to rent)
//   parVOA: value of accessibility (rand per minute of weighted average travel time to citizens)
//   parVOL: value of location (rand per metre of weighted average straight-line distance to citizens)

// Lognormal draw from a mean and a relative spread, as in Citizen's funBuildCitizenPreferences:
//   sigma^2 = ln(1 + spread^2),  mu = ln(mean) - sigma^2 / 2
java.util.function.DoubleBinaryOperator drawLognormal = (mean, spread) -> {
    if (mean <= 0) return 0;   // a mean of 0 switches that preference off
    double sigma = sqrt(log(1 + spread * spread));
    double mu = log(mean) - sigma * sigma / 2;
    return exp(mu + sigma * normal());   // normal() with no arguments = standard normal (mean 0, sd 1)
};
parVOM = drawLognormal.applyAsDouble(main.parActivityVOMMean, main.parActivityVOMSpread);
parVOA = drawLognormal.applyAsDouble(main.parActivityVOAMean, main.parActivityVOASpread);
parVOL = drawLognormal.applyAsDouble(main.parActivityVOLMean, main.parActivityVOLSpread);
