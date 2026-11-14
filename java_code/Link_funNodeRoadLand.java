// Function in Link. Returns value, type: double. No arguments.
// Land this road takes from EACH of its two end nodes (the road's land is split equally between them), in m^2:
//   0.5 * length * width * parCarLandUsage,   width = parRoadWidthRef * varLanes
// parRoadWidthRef: width of one lane (m). parCarLandUsage: multiplier for the land the road takes beyond its
// carriageway (verges, pavements, parking, junctions...). Road land grows linearly with lanes.
// Uses varLanes, so funUpdateCapacity must have run since the last impedance change (funApplyImpedanceChange,
// funInitImpedance and funLoadState all call it before any land is computed).
return 0.5 * parLength * main.parRoadWidthRef * varLanes * main.parCarLandUsage;
