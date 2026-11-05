// Function in Main. Returns value, type: double. Arguments: density (double)
// The rent formula, kept in one place. Given a density (agents per m2 of usable land nearby), returns the rent:
//   R = parRentMin + (parRentRef - parRentMin) * (density / varDensityRef)^parRentDensityElasticity
// parRentRef is the rent at the city's average starting density (varDensityRef, set once by funCalibrateDensity).
// parRentMin is the rent with nobody nearby: rent approaches it smoothly as density falls, never below it.
// The caller chooses which density to pass (everyone included for a node's stored rent, or excluding the agent for its choice),
// and must only call this once varDensityRef is calibrated (otherwise the run stops with an error).
if (varDensityRef <= 0) error("funComputeRent called before varDensityRef was calibrated (run funCalibrateDensity first)");
return parRentMin + (parRentRef - parRentMin) * pow(max(0, density) / varDensityRef, parRentDensityElasticity);   // max(0, ...) guards against tiny negative rounding leftovers
