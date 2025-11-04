// CBBPredictor.java
// Compile: javac CBBPredictor.java
// Run:     java CBBPredictor
//
// This program estimates win probability using:
// - NET ranking (lower is better)
// - Strength of Record (SOR) rank (lower is better)
// - Adjusted Off/Def Efficiency (higher off, lower def)
// - Quadrant win/loss performance (Q1..Q4)
// All weights and scaling constants are near the top for easy tuning.

import java.util.*;

public class CBBPredictor {

    // ===================== TUNABLE WEIGHTS =====================
    // Contribution weights (these should sum to ~1.0 but don't have to)
    static double W_NET_ADV           = 0.25;  // NET rank advantage
    static double W_SOR_ADV           = 0.20;  // SOR rank advantage
    static double W_EFF_MARGIN_ADV    = 0.35;  // Adj efficiency margin difference
    static double W_QUADRANTS_ADV     = 0.20;  // Quadrant performance difference

    // Optional: home court adjustment (add to total score before logistic)
    // Positive boosts the listed home team’s odds. ~0.10 is a mild nudge.
    static double HOME_COURT_BONUS    = 0.10;

    // ===================== SCALING CONSTANTS =====================
    // Divide rank differences by these so each feature lands in a reasonable range ~[-1, +1].
    static double NET_RANK_SCALE      = 50.0;  // typical useful spread
    static double SOR_RANK_SCALE      = 50.0;

    // Efficiency margin = (AdjO - AdjD). Difference is divided by this scale.
    static double EFF_MARGIN_SCALE    = 10.0;  // 10 pts/100 poss is a big difference

    // Logistic slope: larger = more confident probabilities for the same score
    static double LOGISTIC_SLOPE      = 1.35;

    // ===================== QUADRANT SETTINGS =====================
    // We compute a weighted win rate across quadrants (Laplace-smoothed),
    // then compare teams (TeamA minus TeamB).
    static double Q1_WEIGHT = 0.50;
    static double Q2_WEIGHT = 0.25;
    static double Q3_WEIGHT = 0.15;
    static double Q4_WEIGHT = 0.10;

    // Laplace smoothing for win% per quadrant: (W + alpha) / (W + L + 2*alpha)
    static double QUAD_LAPLACE_ALPHA = 1.0;

    // ===================== DATA CLASSES =====================
    public static class TeamStats {
        public final String team;
        public final int netRank;              // lower is better
        public final int sorRank;              // lower is better
        public final double adjOffEff;         // points per 100 poss (adjusted)
        public final double adjDefEff;         // points per 100 poss (adjusted)
        public final int q1Wins, q1Losses;
        public final int q2Wins, q2Losses;
        public final int q3Wins, q3Losses;
        public final int q4Wins, q4Losses;

        public TeamStats(String team,
                         int netRank, int sorRank,
                         double adjOffEff, double adjDefEff,
                         int q1W, int q1L, int q2W, int q2L,
                         int q3W, int q3L, int q4W, int q4L) {
            this.team = team;
            this.netRank = netRank;
            this.sorRank = sorRank;
            this.adjOffEff = adjOffEff;
            this.adjDefEff = adjDefEff;
            this.q1Wins = q1W; this.q1Losses = q1L;
            this.q2Wins = q2W; this.q2Losses = q2L;
            this.q3Wins = q3W; this.q3Losses = q3L;
            this.q4Wins = q4W; this.q4Losses = q4L;
        }

        public double efficiencyMargin() {
            return adjOffEff - adjDefEff;
        }
    }

    public static class MatchupResult {
        public final String teamHome;
        public final String teamAway;
        public final double score;        // pre-logistic combined score (A - B + home bonus if A is home)
        public final double homeWinProb;  // probability (0..1) that home team wins
        public final Map<String, Double> components; // feature-level contributions

        public MatchupResult(String home, String away, double score, double p, Map<String, Double> comps) {
            this.teamHome = home;
            this.teamAway = away;
            this.score = score;
            this.homeWinProb = p;
            this.components = comps;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append(teamHome).append(" vs ").append(teamAway).append("\n");
            sb.append("Home team win probability: ").append(pct(homeWinProb)).append("\n");
            sb.append("Model score (pre-logistic): ").append(r4(score)).append("\n");
            sb.append("Components (home minus away):\n");
            for (var e : components.entrySet()) {
                sb.append("  ").append(e.getKey()).append(": ").append(r4(e.getValue())).append("\n");
            }
            return sb.toString();
        }
    }

    // ===================== CORE MODEL =====================

    /**
     * Predict probability the HOME team wins over the AWAY team.
     * Internally builds a feature score = sum(weights * advantages) and
     * passes it through a logistic transform.
     */
    public static MatchupResult predict(TeamStats home, TeamStats away) {
        Map<String, Double> comps = new LinkedHashMap<>();

        // 1) NET advantage (lower rank is better) -> use (away - home)/scale
        double netAdv = ((double)away.netRank - home.netRank) / NET_RANK_SCALE;
        double netTerm = W_NET_ADV * netAdv;
        comps.put("NET_adv", netTerm);

        // 2) SOR advantage (lower rank is better)
        double sorAdv = ((double)away.sorRank - home.sorRank) / SOR_RANK_SCALE;
        double sorTerm = W_SOR_ADV * sorAdv;
        comps.put("SOR_adv", sorTerm);

        // 3) Adjusted efficiency margin advantage
        double emHome = home.efficiencyMargin();
        double emAway = away.efficiencyMargin();
        double emAdv = (emHome - emAway) / EFF_MARGIN_SCALE;
        double emTerm = W_EFF_MARGIN_ADV * emAdv;
        comps.put("AdjEffMargin_adv", emTerm);

        // 4) Quadrant performance advantage (weighted smoothed win%)
        double quadHome = weightedQuadrantScore(home);
        double quadAway = weightedQuadrantScore(away);
        double quadAdv = quadHome - quadAway;     // already in roughly [0, 1] range
        double quadTerm = W_QUADRANTS_ADV * quadAdv;
        comps.put("Quadrants_adv", quadTerm);

        // Sum contributions
        double score = netTerm + sorTerm + emTerm + quadTerm;

        // Add home court bonus
        score += HOME_COURT_BONUS;
        comps.put("HomeCourt_bonus", HOME_COURT_BONUS);

        // Logistic to probability
        double pHome = logistic(score, LOGISTIC_SLOPE);

        return new MatchupResult(home.team, away.team, score, pHome, comps);
    }

    // Weighted smoothed quadrant score (0..1-ish)
    private static double weightedQuadrantScore(TeamStats t) {
        double q1 = smoothedWinRate(t.q1Wins, t.q1Losses, QUAD_LAPLACE_ALPHA);
        double q2 = smoothedWinRate(t.q2Wins, t.q2Losses, QUAD_LAPLACE_ALPHA);
        double q3 = smoothedWinRate(t.q3Wins, t.q3Losses, QUAD_LAPLACE_ALPHA);
        double q4 = smoothedWinRate(t.q4Wins, t.q4Losses, QUAD_LAPLACE_ALPHA);

        double wsum = Q1_WEIGHT + Q2_WEIGHT + Q3_WEIGHT + Q4_WEIGHT;
        if (wsum == 0) return 0.5; // neutral fallback

        return (Q1_WEIGHT*q1 + Q2_WEIGHT*q2 + Q3_WEIGHT*q3 + Q4_WEIGHT*q4) / wsum;
    }

    private static double smoothedWinRate(int wins, int losses, double alpha) {
        // (W + alpha) / (W + L + 2*alpha)
        return (wins + alpha) / (wins + losses + 2.0*alpha);
    }

    private static double logistic(double x, double slope) {
        double z = slope * x;
        return 1.0 / (1.0 + Math.exp(-z));
    }

    // ===================== DEMO =====================
    public static void main(String[] args) {
        // ----- Example teams (replace with real numbers) -----
        TeamStats home = new TeamStats(
                "Home U",
                12,    // NET rank (lower better)
                18,    // SOR rank (lower better)
                118.5, // Adj Off Eff
                96.2,  // Adj Def Eff
                6, 3,  // Q1 W-L
                5, 2,  // Q2 W-L
                6, 1,  // Q3 W-L
                6, 0   // Q4 W-L
        );

        TeamStats away = new TeamStats(
                "Away State",
                19,
                26,
                114.2,
                98.0,
                4, 5,
                6, 3,
                7, 1,
                7, 0
        );

        MatchupResult res = predict(home, away);
        System.out.println(res);

        // If you want the AWAY team listed as home (e.g., neutral or actual venue),
        // swap the arguments or change HOME_COURT_BONUS accordingly.
    }

    // ===================== HELPERS =====================
    private static String pct(double p) {
        return String.format(Locale.US, "%.1f%%", 100.0*p);
    }
    private static String r4(double x) {
        return String.format(Locale.US, "%.4f", x);
    }
}
