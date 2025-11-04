# receiver_projections.py

from dataclasses import dataclass

@dataclass
class ReceiverInputs:
    # Core player/team inputs (edit these for the player/game)
    receiver_name: str = "Example WR"
    sportsbook_line: float = 62.5          # Sportsbook receiving yards line
    season_avg_rec_yards: float = 58.3     # Player's season avg receiving yards
    qb_pass_yards_line: float = 255.0      # QB passing yards line
    qb_season_avg_pass_yards: float = 240.0# QB season avg passing yards
    spread: float = +3.5                   # Team spread (negative = favored to trail ↑ pass volume)
    game_total_ou: float = 45.5            # Over/Under total points for the game
    defense_avg_wr_yards_allowed: float = 155.0  # Opponent DEF avg yards allowed to WRs per game

    # Weather (simple flags; wind uses mph)
    is_rain: bool = False
    is_snow: bool = False
    wind_mph: float = 0.0


class ReceiverProjections:
    """
    ReceiverProjections computes a receiving yards projection by:
      1) Blending sportsbook line with season average (base).
      2) Adjusting for:
         - Game script via spread (more negative is better for WR volume).
         - Defense avg yards allowed to WRs.
         - Game total (O/U) (light weight).
         - QB pass yards line vs QB avg.
         - Weather: rain, snow, wind (penalties).
    All weights, caps, and baselines are easy to tweak below.
    """

    # ====== GLOBAL TUNABLES / WEIGHTS (edit to taste) ======
    # Base blend between sportsbook line and player season average
    BASE_WEIGHT_SPORTSBOOK = 0.60
    BASE_WEIGHT_SEASON_AVG = 0.40

    # Game script via spread: negative spread (underdog) -> bump; positive -> slight penalty
    # Scale by how extreme the spread is up to SPREAD_CAP points
    W_SPREAD = 0.12        # overall influence (as a proportion multiplier range)
    SPREAD_CAP = 10.0      # cap absolute spread considered

    # Defense vs WRs: compare to league baseline
    W_DEF_WR = 0.18
    LEAGUE_BASELINE_WR_YPG_ALLOWED = 150.0 # league avg yards allowed to WRs per game (tunable)
    DEF_DELTA_CAP = 60.0   # cap how far above/below baseline we consider

    # Game total (O/U): lighter weight
    W_TOTAL = 0.06
    LEAGUE_BASELINE_TOTAL = 44.5
    TOTAL_DELTA_CAP = 12.0

    # QB line vs QB avg: if QB line > avg, boost WR projection
    W_QB = 0.16
    QB_DELTA_CAP_PCT = 0.25  # cap relative delta at +/-25% (i.e., ±0.25)

    # Weather penalties (multiplicative)
    PENALTY_RAIN = 0.92
    PENALTY_SNOW = 0.90
    # Wind schedule (piecewise). These multiply cumulatively with rain/snow if present.
    WIND_THRESHOLDS = [
        (0, 1.00),
        (10, 0.98),
        (15, 0.95),
        (20, 0.92),
        (25, 0.88),
        (30, 0.85),
    ]

    # Safety caps on final adjustments
    MIN_MULTIPLIER = 0.65
    MAX_MULTIPLIER = 1.45

    def __init__(self, inputs: ReceiverInputs):
        self.inp = inputs

    @staticmethod
    def _clamp(x: float, lo: float, hi: float) -> float:
        return max(lo, min(hi, x))

    def _base_yards(self) -> float:
        """Weighted blend of sportsbook line and player's season average."""
        w1 = self.BASE_WEIGHT_SPORTSBOOK
        w2 = self.BASE_WEIGHT_SEASON_AVG
        return w1 * self.inp.sportsbook_line + w2 * self.inp.season_avg_rec_yards

    def _spread_multiplier(self) -> float:
        """
        More negative spread -> team more likely to trail -> more passing -> boost.
        Map spread into [-SPREAD_CAP, +SPREAD_CAP], then scale by W_SPREAD.
        """
        # For underdogs (negative), benefit; for favorites (positive), slight penalty.
        clamped = self._clamp(self.inp.spread, -self.SPREAD_CAP, self.SPREAD_CAP)
        # Invert sign so negative (good) becomes positive contribution
        norm = -clamped / self.SPREAD_CAP  # ∈ [-1, 1], where underdog (neg spread) -> + value
        return 1.0 + norm * self.W_SPREAD

    def _def_multiplier(self) -> float:
        """
        Defense vs WRs adjustment: compare opponent's allowed WR yards to league baseline.
        """
        delta = self.inp.defense_avg_wr_yards_allowed - self.LEAGUE_BASELINE_WR_YPG_ALLOWED
        delta = self._clamp(delta, -self.DEF_DELTA_CAP, self.DEF_DELTA_CAP)
        norm = delta / self.DEF_DELTA_CAP  # ∈ [-1, 1]
        return 1.0 + norm * self.W_DEF_WR

    def _total_multiplier(self) -> float:
        """
        Game total O/U adjustment (lighter weight).
        """
        delta = self.inp.game_total_ou - self.LEAGUE_BASELINE_TOTAL
        delta = self._clamp(delta, -self.TOTAL_DELTA_CAP, self.TOTAL_DELTA_CAP)
        norm = delta / self.TOTAL_DELTA_CAP  # ∈ [-1, 1]
        return 1.0 + norm * self.W_TOTAL

    def _qb_multiplier(self) -> float:
        """
        QB line vs season avg (relative): if line >> avg, bump WR projection.
        Use capped relative delta.
        """
        avg = max(self.inp.qb_season_avg_pass_yards, 1e-6)  # safety
        rel = (self.inp.qb_pass_yards_line - avg) / avg
        rel = self._clamp(rel, -self.QB_DELTA_CAP_PCT, self.QB_DELTA_CAP_PCT)
        # Map rel ∈ [-cap, +cap] to [-1, +1] by dividing by cap:
        norm = rel / self.QB_DELTA_CAP_PCT
        return 1.0 + norm * self.W_QB

    def _weather_multiplier(self) -> float:
        """
        Multiplicative penalties for rain, snow, and wind.
        """
        mult = 1.0
        if self.inp.is_rain:
            mult *= self.PENALTY_RAIN
        if self.inp.is_snow:
            mult *= self.PENALTY_SNOW

        # Wind penalty by threshold
        wind_pen = 1.0
        for thresh, factor in self.WIND_THRESHOLDS:
            if self.inp.wind_mph >= thresh:
                wind_pen = factor
        mult *= wind_pen
        return mult

    def project(self) -> dict:
        """
        Returns a dict with:
          - projection (float)
          - components (dict of each multiplier and base)
        """
        base = self._base_yards()
        m_spread = self._spread_multiplier()
        m_def = self._def_multiplier()
        m_total = self._total_multiplier()
        m_qb = self._qb_multiplier()
        m_weather = self._weather_multiplier()

        raw_multiplier = m_spread * m_def * m_total * m_qb * m_weather
        final_multiplier = self._clamp(raw_multiplier, self.MIN_MULTIPLIER, self.MAX_MULTIPLIER)
        projection = base * final_multiplier

        return {
            "receiver": self.inp.receiver_name,
            "projection": round(projection, 1),
            "base_yards": round(base, 1),
            "final_multiplier": round(final_multiplier, 4),
            "uncapped_multiplier": round(raw_multiplier, 4),
            "components": {
                "spread_multiplier": round(m_spread, 4),
                "def_multiplier": round(m_def, 4),
                "total_multiplier": round(m_total, 4),
                "qb_multiplier": round(m_qb, 4),
                "weather_multiplier": round(m_weather, 4),
            },
            "inputs": self.inp.__dict__,
            "weights": {
                "BASE_WEIGHT_SPORTSBOOK": self.BASE_WEIGHT_SPORTSBOOK,
                "BASE_WEIGHT_SEASON_AVG": self.BASE_WEIGHT_SEASON_AVG,
                "W_SPREAD": self.W_SPREAD,
                "SPREAD_CAP": self.SPREAD_CAP,
                "W_DEF_WR": self.W_DEF_WR,
                "LEAGUE_BASELINE_WR_YPG_ALLOWED": self.LEAGUE_BASELINE_WR_YPG_ALLOWED,
                "DEF_DELTA_CAP": self.DEF_DELTA_CAP,
                "W_TOTAL": self.W_TOTAL,
                "LEAGUE_BASELINE_TOTAL": self.LEAGUE_BASELINE_TOTAL,
                "TOTAL_DELTA_CAP": self.TOTAL_DELTA_CAP,
                "W_QB": self.W_QB,
                "QB_DELTA_CAP_PCT": self.QB_DELTA_CAP_PCT,
                "PENALTY_RAIN": self.PENALTY_RAIN,
                "PENALTY_SNOW": self.PENALTY_SNOW,
                "WIND_THRESHOLDS": self.WIND_THRESHOLDS,
                "MIN_MULTIPLIER": self.MIN_MULTIPLIER,
                "MAX_MULTIPLIER": self.MAX_MULTIPLIER,
            },
        }


# ===== Example usage (delete or adapt) =====
if __name__ == "__main__":
    # Fill these with the actual game inputs you care about:
    inputs = ReceiverInputs(
        receiver_name="Sample WR",
        sportsbook_line=62.5,
        season_avg_rec_yards=58.3,
        qb_pass_yards_line=255.0,
        qb_season_avg_pass_yards=240.0,
        spread=-2.5,  # Underdog helps (more negative)
        game_total_ou=46.5,
        defense_avg_wr_yards_allowed=162.0,
        is_rain=False,
        is_snow=False,
        wind_mph=12.0,
    )

    model = ReceiverProjections(inputs)
    result = model.project()
    print(result)
