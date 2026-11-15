// Function in Link. Just action (returns nothing). No arguments. Call whenever the impedance changes.
// Sets this link's number of lanes and its capacity from its impedance:
//   varLanes    = (parImpedanceLocal / I)^parLanesGammaExponent
//               (a local road has 1 lane each way; lower impedance = more lanes. Fractions stand for wider, faster lanes,
//                so lanes need not be whole numbers. The exponent lets parImpedanceLocal be raised, e.g. for speed bumps and
//                stop signs on local roads, while a smaller exponent keeps the lanes of higher-order roads about the same)
//   varCapacity = varLanes * (3600 / 2) / (parPeakHourConcentration * parPeoplePerCitizen)
// 3600 / 2: one vehicle per lane every 2 seconds (minimum peak headway) = 1800 vehicles per lane per hour.
// Link flows are counted in citizens (each citizen's activity weights sum to 1, one one-way trip) when they should be multiplied by parPeoplePerCitizen to reflect that each citizen represents a cerain number of real people (too many to simulate one-one). 
// The sub-utilities themselves don't need to be multiplied by parPeoplePerCitizen, since they represent sub-utilities per person evaluating, and if we multiplied them, then each term (rent, time, cost) would change by the same proportion; we would just adjust the logit scale to compensate, and the net result would be identical.
//   parPeakHourConcentration share of the computed trips that fall in the peak hour (0 to 1, default 0.5), which is where we are evaluating in the in the traffic assignments for utility calculations:
//                            higher = traffic more concentrated in the peak = more congestion
// Rather than scaling every flow by parPeoplePerCitizen and parPeakHourConcentration, capacity is divided by both instead (same V/C), so V/C in BPR (funUpdateTravelTime)
// is the real peak-hour V/C.
// Free-flow speed stays parVRef / I (funFreeFlowTime). Road land per node uses varLanes (funNodeRoadLand).
varLanes = pow(main.parImpedanceLocal / varImpedance, main.parLanesGammaExponent);
varCapacity = varLanes * (3600.0 / 2) / (main.parPeakHourConcentration * main.parPeoplePerCitizen);
