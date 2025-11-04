// ReceiverProjections.java
// Compile: javac ReceiverProjections.java
// Run:     java ReceiverProjections

import java.util.LinkedHashMap;
import java.util.Map;

public class ReceiverProjections {

    // ======= TUNABLE WEIGHTS / CONSTANTS =======
    // Base blend
    public static double BASE_WEIGHT_SPORTSBOOK = 0.60;
    public static double BASE_WEIGHT_SEASON_AVG = 0.40;

    // Spread (more negative is better for WR volume)
    public static double W_SPREAD = 0.12;
    public static double SPREAD_CAP = 10.0;

    // Defense vs WRs (compare to league baseline)
    public static double W_DEF_WR = 0.18;
    public static double LEAGUE_BASELINE_WR_YPG_ALLOWED = 150.0;
    public static double DEF_DELTA_CAP = 60.0;

    // Game total (O/U) — lightly weighted
    public static double W_TOTAL = 0.06;
    public static double LEAGUE_BASELINE_TOTAL = 44.5;
    public static double TOTAL_DELTA_CAP = 12.0;

    // QB line vs QB avg (relative delta)
    public static double W_QB = 0.16;
    public static double QB_DELTA_CAP_PCT = 0.25; // +/-25%

    // Weather penalties (multiplicative)
    public static double PENALTY_RAIN = 0.92;
    public static double PENALTY_SNOW = 0.90;

    // Wind thresholds (mph -> multiplier). Last matching threshold wins.
    // You can tweak or add rows as needed.
    public static int[] WIND_THRESHOLDS_MPH =    { 0, 10, 15, 20, 25, 30 };
    public static double[] WIND_FACTORS =        {1.00, .98, .95, .92, .88, .85};

    // Safety caps on overall multiplier
    public static double MIN_MULTIPLIER = 0.65;
    public static double MAX_MULTIPLIER = 1.45;

    // ======= INPUT HOLDER =======
    public static class Inputs {
        public String receiverName = "Example WR";
        public double sportsbookLine = 62.5;
        public double seasonAvgRecYards = 58.3;
        public double qbPassYardsLine = 255.0;
        public double qbSeasonAvgPassYards = 240.0;
        public double spread = -2.5;                  // negative = underdog (better for volume)
        public double gameTotalOU = 46.5;
        public double defenseAvgWrYardsAllowed = 162.0;
        public boolean isRain = false;
        public boolean isSnow = false;
        public double windMph = 12.0;

        public Inputs() {}

        public Inputs set(String receiverName,
                          double sportsbookLine,
                          double seasonAvgRecYards,
                          double qbPassYardsLine,
                          double qbSeasonAvgPassYards,
                          double spread,
                          double gameTotalOU,
                          double defenseAvgWrYardsAllowed,
                          boolean isRain,
                          boolean isSnow,
                          double windMph) {
            this.receiverName = receiverName;
            this.sportsbookLine = sportsbookLine;
            this.seasonAvgRecYards = seasonAvgRecYards;
            this.qbPassYardsLine = qbPassYardsLine;
            this.qbSeasonAvgPassYards = qbSeasonAvgPassYards;
            this.spread = spread;
            this.gameTotalOU = gameTotalOU;
            this.defenseAvgWrYardsAllowed = defenseAvgWrYardsAllowed;
            this.isRain = isRain;
            this.isSnow = isSnow;
            this.windMph = windMph;
            return this;
        }
    }

    // ======= RESULT HOLDER =======
    public static class Result {
        public String receiver;
        public double projection;
        public double baseYards;
        public double finalMultiplier;
        public double uncappedMultiplier;
        public Map<String, Double> components = new LinkedHashMap<>();
        public Inputs inputsSnapshot;

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("Receiver: ").append(receiver).append("\n");
            sb.append("Base Yards: ").append(round1(baseYards)).append("\n");
            sb.append("Projection: ").append(round1(projection)).append("\n");
            sb.append("Final Multiplier: ").append(round4(finalMultiplier))
              .append(" (uncapped ").append(round4(uncappedMultiplier)).append(")\n");
            sb.append("Components:\n");
            for (Map.Entry<String, Double> e : components.entrySet()) {
                sb.append("  ").append(e.getKey()).append(": ").append(round4(e.getValue())).append("\n");
            }
            return sb.toString();
        }
    }

    // ======= MODEL =======
    private final Inputs inp;

    public ReceiverProjections(Inputs inputs) {
        this.inp = inputs;
    }

    public Result project() {
        double base = baseYards();
        double mSpread = spreadMultiplier();
        double mDef = defenseMultiplier();
        double mTotal = totalMultiplier();
        double mQb = qbMultiplier();
        double mWeather = weatherMultiplier();

        double rawMult = mSpread * mDef * mTotal * mQb * mWeather;
        double finalMult = clamp(rawMult, MIN_MULTIPLIER, MAX_MULTIPLIER);
        double proj = base * finalMult;

        Result r = new Result();
        r.receiver = inp.receiverName;
        r.baseYards = base;
        r.projection = proj;
        r.finalMultiplier = finalMult;
        r.uncappedMultiplier = rawMult;
        r.inputsSnapshot = copyInputs(inp);

        r.components.put("spread_multiplier", mSpread);
        r.components.put("def_multiplier", mDef);
        r.components.put("total_multiplier", mTotal);
        r.components.put("qb_multiplier", mQb);
        r.components.put("weather_multiplier", mWeather);

        return r;
    }

    // ======= PIECES =======
    private double baseYards() {
        return BASE_WEIGHT_SPORTSBOOK * inp.sportsbookLine
             + BASE_WEIGHT_SEASON_AVG * inp.seasonAvgRecYards;
    }

    private double spreadMultiplier() {
        // Clamp spread to [-SPREAD_CAP, +SPREAD_CAP]
        double c = clamp(inp.spread, -SPREAD_CAP, SPREAD_CAP);
        // negative (underdog) is beneficial: invert sign
        double norm = -c / SPREAD_CAP; // ∈ [-1, 1]
        return 1.0 + norm * W_SPREAD;
    }

    private double defenseMultiplier() {
        double delta = inp.defenseAvgWrYardsAllowed - LEAGUE_BASELINE_WR_YPG_ALLOWED;
        delta = clamp(delta, -DEF_DELTA_CAP, DEF_DELTA_CAP);
        double norm = delta / DEF_DELTA_CAP; // ∈ [-1, 1]
        return 1.0 + norm * W_DEF_WR;
    }

    private double totalMultiplier() {
        double delta = inp.gameTotalOU - LEAGUE_BASELINE_TOTAL;
        delta = clamp(delta, -TOTAL_DELTA_CAP, TOTAL_DELTA_CAP);
        double norm = delta / TOTAL_DELTA_CAP; // ∈ [-1, 1]
        return 1.0 + norm * W_TOTAL;
    }

    private double qbMultiplier() {
        double avg = Math.max(inp.qbSeasonAvgPassYards, 1e-6);
        double rel = (inp.qbPassYardsLine - avg) / avg;
        rel = clamp(rel, -QB_DELTA_CAP_PCT, QB_DELTA_CAP_PCT);
        double norm = rel / QB_DELTA_CAP_PCT; // ∈ [-1, 1]
        return 1.0 + norm * W_QB;
    }

    private double weatherMultiplier() {
        double mult = 1.0;
        if (inp.isRain) mult *= PENALTY_RAIN;
        if (inp.isSnow) mult *= PENALTY_SNOW;
        mult *= windFactor(inp.windMph);
        return mult;
    }

    private static double windFactor(double windMph) {
        double factor = 1.0;
        for (int i = 0; i < WIND_THRESHOLDS_MPH.length; i++) {
            if (windMph >= WIND_THRESHOLDS_MPH[i]) {
                factor = WIND_FACTORS[i];
            }
        }
        return factor;
    }

    // ======= HELPERS =======
    private static double clamp(double x, double lo, double hi) {
        return Math.max(lo, Math.min(hi, x));
    }

    private static double round1(double x) {
        return Math.round(x * 10.0) / 10.0;
    }

    private static double round4(double x) {
        return Math.round(x * 10000.0) / 10000.0;
    }

    private static Inputs copyInputs(Inputs src) {
        Inputs c = new Inputs();
        c.receiverName = src.receiverName;
        c.sportsbookLine = src.sportsbookLine;
        c.seasonAvgRecYards = src.seasonAvgRecYards;
        c.qbPassYardsLine = src.qbPassYardsLine;
        c.qbSeasonAvgPassYards = src.qbSeasonAvgPassYards;
        c.spread = src.spread;
        c.gameTotalOU = src.gameTotalOU;
        c.defenseAvgWrYardsAllowed = src.defenseAvgWrYardsAllowed;
        c.isRain = src.isRain;
        c.isSnow = src.isSnow;
        c.windMph = src.windMph;
        return c;
    }

    // ======= EXAMPLE USAGE =======
    public static void main(String[] args) {
        Inputs inputs = new Inputs().set(
            "Sample WR",
            62.5,   // sportsbook receiving yards line
            58.3,   // season avg receiving yards
            255.0,  // QB pass yards line
            240.0,  // QB season avg pass yards
            -2.5,   // spread (negative: underdog -> potential boost)
            46.5,   // game total O/U
            162.0,  // defense avg WR yards allowed
            false,  // rain
            false,  // snow
            12.0    // wind mph
        );

        ReceiverProjections model = new ReceiverProjections(inputs);
        Result r = model.project();
        System.out.println(r);
    }
}
