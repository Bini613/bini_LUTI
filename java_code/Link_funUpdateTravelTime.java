// Function in Link. Just action (returns nothing). No arguments.
// Bureau of Public Roads (BPR) congestion function: sets varTravelTime from the free-flow time and the volume/capacity ratio.
//   t = t0 * (1 + parCongestionAlpha * (V / C)^parCongestionBeta)
varTravelTime = funFreeFlowTime() * (1 + main.parCongestionAlpha * pow(varFlowVol / varCapacity, main.parCongestionBeta));
