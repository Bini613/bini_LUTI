// TEMPORARY TEST. Paste in Main's On startup, after funPlaceAgents();
// Prints one citizen's weights, the average largest weight (unevenness), and the worst deviation of any citizen's sum from 1.
// Then, for VOT and VOM: the average and relative spread across citizens; for VOP: the average, standard deviation and share negative.
// All compared with the Main parameters.
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

// Sample mean and relative spread (sd / mean) of VOT and VOM (lognormal), next to the target values
String[] names = {"VOT", "VOM"};
double[] targetMeans = {parVOTMean, parVOMMean};
double[] targetSpreads = {parVOTSpread, parVOMSpread};
for (int p = 0; p < 2; p++) {   // p = 0, 1 for VOT, VOM
    double sum = 0, sumSq = 0;
    for (Citizen c : citizens) {
        double v = (p == 0) ? c.parVOT : c.parVOM;
        sum += v;
        sumSq += v * v;
    }
    double mean = sum / citizens.size();
    double sd = sqrt(max(0, sumSq / citizens.size() - mean * mean));
    double spread = mean > 0 ? sd / mean : 0;
    traceln(names[p] + ": mean " + roundToDecimal(mean, 3) + " (target " + targetMeans[p] + "), spread " + roundToDecimal(spread, 3) + " (target " + targetSpreads[p] + ")");   // TEST
}

// VOP (normal): sample mean and standard deviation, and the share of citizens who enjoy crowding (negative VOP)
double vopSum = 0, vopSumSq = 0;
int vopNegative = 0;
for (Citizen c : citizens) {
    vopSum += c.parVOP;
    vopSumSq += c.parVOP * c.parVOP;
    if (c.parVOP < 0) vopNegative++;
}
double vopMean = vopSum / citizens.size();
double vopSd = sqrt(max(0, vopSumSq / citizens.size() - vopMean * vopMean));
traceln("VOP: mean " + roundToDecimal(vopMean, 3) + " (target " + parVOPMean + "), standard deviation " + roundToDecimal(vopSd, 3) + " (target " + parVOPStandardDeviation
        + "), negative (enjoy crowding): " + roundToDecimal(100.0 * vopNegative / citizens.size(), 1) + "%");   // TEST

// Same check for activities' VOM, VOA and VOL (few activities, so expect a looser match to the targets)
String[] aNames = {"Activity VOM", "Activity VOA", "Activity VOL"};
double[] aTargetMeans = {parActivityVOMMean, parActivityVOAMean, parActivityVOLMean};
double[] aTargetSpreads = {parActivityVOMSpread, parActivityVOASpread, parActivityVOLSpread};
for (int p = 0; p < 3; p++) {   // p = 0, 1, 2 for VOM, VOA, VOL
    double sum = 0, sumSq = 0;
    for (Activity a : activities) {
        double v = (p == 0) ? a.parVOM : (p == 1) ? a.parVOA : a.parVOL;
        sum += v;
        sumSq += v * v;
    }
    double mean = sum / activities.size();
    double sd = sqrt(max(0, sumSq / activities.size() - mean * mean));
    double spread = mean > 0 ? sd / mean : 0;
    traceln(aNames[p] + ": mean " + roundToDecimal(mean, 3) + " (target " + aTargetMeans[p] + "), spread " + roundToDecimal(spread, 3) + " (target " + aTargetSpreads[p] + ")");   // TEST
}
