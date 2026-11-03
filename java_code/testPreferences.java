// TEMPORARY TEST. Paste in Main's On startup, after funPlaceAgents();
// Prints one citizen's weights, the average largest weight (unevenness), and the worst deviation of any citizen's sum from 1.
// Then, for VOT, VOM and VOP: the average and relative spread across citizens, to compare with the Main parameters.
String s = "";
for (double w : citizens.get(0).varActivityWeights) s += roundToDecimal(w, 3) + " ; ";
traceln("Citizen 0 weights: " + s);   // TEST

double sumLargest = 0;
double worstSumError = 0;
for (Citizen c : citizens) {
    double sum = 0;
    double largest = 0;
    for (double w : c.varActivityWeights) {
        sum += w;
        largest = max(largest, w); //updates if the current loop's weight is bigger than the last
    }
    sumLargest += largest;
    worstSumError = max(worstSumError, abs(sum - 1));
}
traceln("Avg largest weight: " + roundToDecimal(sumLargest / citizens.size(), 3)
        + " (even split would be " + roundToDecimal(1.0 / parNumActivities, 3) + "), worst sum error: " + String.format("%.2e", worstSumError));   // TEST. %.2e = scientific notation with 2 decimals, e.g. 2.22e-16

// Sample mean and relative spread (sd / mean) of each preference, next to the target values
String[] names = {"VOT", "VOM", "VOP"};
double[] targetMeans = {parVOTMean, parVOMMean, parVOPMean};
double[] targetSpreads = {parVOTSpread, parVOMSpread, parVOPSpread};
for (int p = 0; p < 3; p++) {   // p = 0, 1, 2 for VOT, VOM, VOP
    double sum = 0, sumSq = 0;
    for (Citizen c : citizens) {
        double v = (p == 0) ? c.parVOT : (p == 1) ? c.parVOM : c.parVOP;
        sum += v;
        sumSq += v * v;
    }
    double mean = sum / citizens.size();
    double sd = sqrt(max(0, sumSq / citizens.size() - mean * mean));
    double spread = mean > 0 ? sd / mean : 0;
    traceln(names[p] + ": mean " + roundToDecimal(mean, 3) + " (target " + targetMeans[p] + "), spread " + roundToDecimal(spread, 3) + " (target " + targetSpreads[p] + ")");   // TEST
}
