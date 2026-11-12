// Function in Link. Returns value, type: double. No arguments.
// Free-flow travel time on this link in minutes: length / speed, where speed = parVRef / impedance.
double v = main.parVRef / varImpedance;   // parVRef already converted to m/min
return parLength / v; //time = distance / speed [minutes]
