// Function in Link. Just action (returns nothing). No arguments.
// Sets this link's capacity from its impedance: C = parCapacityRef * I^(-parCapacityGamma). Call whenever the impedance changes.
varCapacity = main.parCapacityRef * pow(varImpedance, -main.parCapacityGamma);
