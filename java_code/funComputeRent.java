// Function in Main. Returns value, type: double. Arguments: density (double)
// The rent formula, kept in one place. Given a density (agents per m2 of usable land nearby), returns the rent:
//   R = parRentMin + (parRentRef - parRentMin) * (density / parDensityRef)^parRentDensityElasticity
// parRentRef is the rent at the reference density parDensityRef (Main parameter, agents/m^2), set by hand rather than
// calibrated from the starting positions, so that different starting positions are priced on the same scale.
// parRentMin is the rent with nobody nearby: rent approaches it smoothly as density falls, never below it.
// The caller chooses which density to pass (everyone included for a node's stored rent, or excluding the agent for its choice).
if (parDensityRef <= 0) error("funComputeRent: parDensityRef must be positive");
return parRentMin + (parRentRef - parRentMin) * pow(max(0, density) / parDensityRef, parRentDensityElasticity);   // max(0, ...) guards against tiny negative rounding leftovers
