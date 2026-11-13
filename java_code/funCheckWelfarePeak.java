// Function in Main. Returns value, type: boolean. No arguments. Called by funRunCheckpoint at each spread equilibrium.
// Returns true once citizen welfare is in sustained decline, i.e. further investment is making citizens worse off.
//
// Measure: mean over all citizens of varExpectedWelfare (logsum stored on each citizen's last turn, rand per month).
// After an investment, the next check only comes once the spread has settled again, so every citizen has had a turn
// since the investment. On the loaded path the values come from savedState.txt (funLoadState), so the first check
// matches the straight-through run.
//
// Each check's mean is appended to colWelfareHistory (Collection in Main: ArrayList of Double), one value per check:
// the first is the no-investment city, then one after each investment. Unlike the spread history it is never cleared.
// Decline rule: a straight line is fitted (least squares) through the last K = parWelfareSlopeWindow values,
// with x = 0, 1, ..., K-1 (check number) and y = mean welfare:
//   slope = sum((x - x_mean) * (y - y_mean)) / sum((x - x_mean)^2)      (rand per month, per investment)
//   - slope < -parWelfareSlopeThreshold: sustained decline, return true (stop investing).
//   - otherwise: return false (keep investing). Also false until there are K values.
// Every point in the window counts, so one noisy check can't stop the run on its own.
// The best mean so far and the check it came from are kept in varBestMeanWelfare and varBestWelfareCheck
// (checks are numbered from 1: check 1 = no investment, check k = after k-1 investments),
// so the optimum can be reported even though the stop comes later.
// Uses Main variables varBestMeanWelfare (double, initial value Double.NEGATIVE_INFINITY) and varBestWelfareCheck (int),
// and Main parameters parWelfareSlopeWindow (int, at least 2, e.g. 5) and parWelfareSlopeThreshold (double, rand per month, e.g. 0).

double sum = 0;
for (Citizen c : citizens) sum += c.varExpectedWelfare;
double meanW = sum / citizens.size();

colWelfareHistory.add(meanW);
int n = colWelfareHistory.size();   // number of checks so far, which is also this check's number (1 = no investment yet)
if (meanW > varBestMeanWelfare) {
    varBestMeanWelfare = meanW;
    varBestWelfareCheck = n;
}

int window = parWelfareSlopeWindow;
if (n < window) {   // not enough history yet to judge a trend
    funRuntimePrint("Mean citizen welfare " + roundToDecimal(meanW, 1) + " at check " + n + " (turn " + varTurnCount + "). Best mean welfare so far: "
            + roundToDecimal(varBestMeanWelfare, 1) + ", at check " + varBestWelfareCheck + "; slope not calculated until " + window + " checks are recorded");
    return false;
}

// Least-squares slope through the last 'window' values (x = 0 .. window-1)
int first = n - window;                    // index of the oldest value in the window
double xMean = (window - 1) / 2.0;         // mean of 0, 1, ..., window-1. From arithmetic sum formula (Gauss method derives this)
double ySum = 0;
for (int i = first; i < n; i++) ySum += colWelfareHistory.get(i);
double yMean = ySum / window;
double sxy = 0, sxx = 0;
for (int i = first; i < n; i++) {
    double dx = (i - first) - xMean;
    sxy += dx * (colWelfareHistory.get(i) - yMean);
    sxx += dx * dx;
}
double slope = sxy / sxx;   // rand per month, per investment
boolean declining = slope < -parWelfareSlopeThreshold; // Slope being less than threshold means more negative, therefore greater decline.

funRuntimePrint("Mean citizen welfare " + roundToDecimal(meanW, 1) + " at check " + n + " (turn " + varTurnCount + "), slope over last "
        + window + " checks " + roundToDecimal(slope, 2) + " per investment (threshold -" + parWelfareSlopeThreshold + ")"
        + (declining ? ": sustained decline" : "") + ", best " + roundToDecimal(varBestMeanWelfare, 1) + " at check " + varBestWelfareCheck);
return declining;
