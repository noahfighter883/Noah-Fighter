# Noah Fighter

Data analytics and UX/UI design. Senior Computer Science major (minoring in Mathematics and Economics) at Skidmore College, graduating May 2027 and available from August 2027.

**Full portfolio, case studies, and resume:** [noahfighter883.github.io](https://noahfighter883.github.io)

---

## About

I turn messy data into decisions. I work in SQL and Python to find what's actually driving a result, from NFL salary-cap spending to subscription churn, then present it through dashboards and interactive reports. I got into UX/UI because I like fixing things that technically work but are frustrating to use, and that background helps me present analysis in a way people actually use.

I'm looking for data-centered roles (data analyst, data science, and product analytics) where I can pair rigorous analysis with clear communication.

---

## Experience

**Student Data Analyst** · Skidmore College Men's Soccer & Men's Ice Hockey · Saratoga Springs, NY · Oct 2026 to present
- Turn Hudl and Spiideo game data into coach-ready information for two NCAA Division III programs, men's soccer and men's ice hockey.
- Design and build web-based HTML dashboards that give the men's soccer coaching staff a new way to explore team data.
- Provide live in-game faceoff and shot statistics to the men's ice hockey head coach, giving real-time numbers to work from.

**Product Design & UX Intern** · IL-Team Studio · Barcelona, Spain · Jan to Aug 2026
- Designed and built multiple client-facing websites, translating brand and market research into functional, production-ready pages.
- Created high-fidelity UI prototypes in Figma to pitch mock products to potential investors, iterating on stakeholder feedback.
- Conducted competitive UI/UX analysis of European fashion brands, identifying patterns that directly informed prototype decisions.

---

## Data Projects

### NFL Position Value: Which Positions Are Worth Paying For?
Team-level analysis of how salary-cap spending by position relates to team success across 416 team-seasons (2013 to 2025). Quarterback spending has the clearest payoff, about +0.26 points per game for each point of cap share, while tight end spending fails a placebo test and looks like noise. Comes with an interactive dashboard and a written case study of the method and its limits. Python, statistical modeling, data visualization.
[Live site](https://nfl-position-value.vercel.app) · [Code](https://github.com/noahfighter883/nfl-position-value)

### Telco Churn & Revenue at Risk
Answers which customers are churning and how much recurring revenue is at stake. A DuckDB SQL pipeline with data-quality checks and significance tests finds one segment (month-to-month, fiber optic, electronic check) churning at 60% with $330,812 a year at risk, over 3x any other. A retention scenario shows a campaign breaks even if it saves just 8% of those customers. SQL, Python, Tableau.
[Live dashboard](https://public.tableau.com/app/profile/noah.fighter/viz/TelcoChurnRevenueatRisk/Dashboard1) · [Code](https://github.com/noahfighter883/telco-churn-revenue-analysis)

### NFL Win Probability Model
An XGBoost model trained from scratch on 166k plays of NFL data (0.845 AUC), paired with an interactive React replay of five of the most dramatic games in recent seasons.
[Live site](https://nfl-win-probability.vercel.app) · [Code](https://github.com/noahfighter883/nfl-win-probability)

### Line Board
Finds mispriced fantasy football players by comparing two independent markets, Vegas season-long props and fantasy draft ADP, and surfaces the gap between them as a ranked board.
[Live site](https://line-board-ten.vercel.app) · [Code](https://github.com/noahfighter883/LineBoard)

### What Actually Pays Off in a Developer Career
An interactive breakdown of the 2025 Stack Overflow Developer Survey (49,191 respondents), built around findings that go beyond the usual "AI is changing everything" takes.
[Live site](https://dev-path-report.vercel.app) · [Code](https://github.com/noahfighter883/dev-path-report)

### DynastyEvaluator
A fantasy football analytics dashboard for dynasty leagues. Plug in a Sleeper league and it ranks every team by dynasty value, redraft value, or projected points, broken down by position.
[Live site](https://dynasty-fantasy-football-dashboard.vercel.app) · [Code](https://github.com/noahfighter883/DynastyFantasyFootballDashboard)

### OB1 Odysseys: Save the Planet with Data
Designed and built the site for OB1 Odysseys, which turns eco-tourism travel into short-form PSAs, each backed by a full data-driven case. The flagship case tracks 25 years of wildfire data along the Pacific Crest Trail: mega-fires are now roughly 3.4 times more frequent than a decade ago.
[Live site](https://www.ob1odysseys.com) · [Code](https://github.com/noahfighter883/ob1-odysseys)

### More data and modeling projects
- **[Olist E-Commerce SQL Analytics](https://github.com/noahfighter883/olist-ecommerce-sql-analytics):** 100k Brazilian e-commerce orders analyzed in pure SQL: revenue trends, RFM segmentation, and how delivery delays tank review scores.
- **[NYC Taxi Analytics Engineering](https://github.com/noahfighter883/nyc-taxi-analytics-engineering):** a dbt and DuckDB warehouse on a full year of NYC Yellow Taxi data (39.7M trips), with layered models, incremental loading, and an SCD Type 2 dimension.
- **[TdModel](https://github.com/noahfighter883/TdModel)** ([live](https://td-model.vercel.app)): flags NFL RB, WR, and TE players due for touchdown regression, using an expected-touchdowns baseline by yardline plus red-zone opportunity shifts from roster changes.
- **[CBB Win Probability Model](https://github.com/noahfighter883/cbb-win-probability-model)** (Java): estimates college basketball matchup win probabilities from NET, strength of record, adjusted efficiencies, and quadrant records.
- **[NFL Receiver Projections](https://github.com/noahfighter883/nfl-receiver-projections)** (Python): weekly wide receiver projections combining sportsbook lines, implied team totals, game script, weather, and matchup data.
- **[NBA Points Model](https://github.com/noahfighter883/nba-points-model)** (C): projects player scoring from sportsbook lines, season averages, pace, and defense versus position.
- **[NBA Assists Predictor](https://github.com/noahfighter883/nba-assists-predictor)** (C): projects player assists from sportsbook lines, recent potential assists, pace, and role context.

---

## UX/UI Projects

### Registrar: Course Registration Redesign
UX/UI case study redesigning a Banner-style college registration system end to end, from a heuristic-evaluation audit to a fully interactive Next.js prototype that fixes broken filters, ambiguous validation states, and buried information.
[Live site](https://registrar-ux-redesign.vercel.app) · [Code](https://github.com/noahfighter883/registrar-ux-redesign)

### MyRefills: Prescription Refill & Care Navigation
UX/UI case study redesigning prescription refills for elderly patients and their caregivers, grounded in Medicare Part D adherence research and Pew tech-adoption data. Iterated on real usability-testing findings and holds a 100/100 Lighthouse accessibility score across every screen.
[Live site](https://prescription-refill-ux.vercel.app) · [Code](https://github.com/noahfighter883/prescription-refill-ux)

---

## Other Projects

- **[Blackjack Simulation](https://github.com/noahfighter883/blackjack-simulation)** (Java): console blackjack with splits, doubles, bankroll tracking, and dealer logic, built with an object-oriented design.
- **[Sudoku Solver](https://github.com/noahfighter883/Sudoku-Solver)** (Python): a recursive backtracking solver that enforces row, column, and subgrid constraints.

---

## Skills

**Data & Technical:** SQL, Python, data analysis, data visualization, Tableau, statistical modeling, machine learning, dashboards, regression analysis, sports analytics, data storytelling, hypothesis testing, XGBoost, DuckDB, dbt, React, Next.js, TypeScript, Java, C, Git/GitHub, Vercel
**Design:** Figma, UX design, high-fidelity prototyping, wireframing, competitive UX analysis, design systems

---

## Contact

Email: noahfighter@skidmore.edu
LinkedIn: [noah-fighter-b62130341](https://www.linkedin.com/in/noah-fighter-b62130341)
Portfolio: [noahfighter883.github.io](https://noahfighter883.github.io)
