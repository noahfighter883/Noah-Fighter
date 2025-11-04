// Blackjack.java
// Console Blackjack with Hit / Stand / Double / Split (single split).
// Rules:
// - 52-card deck, reshuffled each round
// - Dealer stands on soft 17
// - Blackjack pays 3:2
// - Double Down: only on first action (2-card hand); exactly one card then stand
// - Split: only if first two cards are same rank. One split max per round.
//   - If splitting Aces: one card dealt to each split hand, then auto-stand (typical rule)
// - Push returns bet
// - Simple bankroll and betting loop

import java.util.*;

public class Blackjack {

    // ====== Card / Deck ======
    enum Suit { CLUBS, DIAMONDS, HEARTS, SPADES }
    enum Rank {
        TWO(2), THREE(3), FOUR(4), FIVE(5), SIX(6), SEVEN(7), EIGHT(8), NINE(9),
        TEN(10), JACK(10), QUEEN(10), KING(10), ACE(11);
        final int value;
        Rank(int v) { this.value = v; }
    }
    static class Card {
        final Suit suit;
        final Rank rank;
        Card(Suit s, Rank r) { suit = s; rank = r; }
        @Override public String toString() {
            String r = switch (rank) {
                case JACK -> "J";
                case QUEEN -> "Q";
                case KING -> "K";
                case ACE -> "A";
                default -> String.valueOf(rank.value);
            };
            String s = switch (suit) {
                case CLUBS -> "♣";
                case DIAMONDS -> "♦";
                case HEARTS -> "♥";
                case SPADES -> "♠";
            };
            return r + s;
        }
    }
    static class Deck {
        private final List<Card> cards = new ArrayList<>();
        private final Random rng = new Random();
        Deck() { reset(); }
        final void reset() {
            cards.clear();
            for (Suit s: Suit.values())
                for (Rank r: Rank.values())
                    cards.add(new Card(s, r));
            shuffle();
        }
        void shuffle() { Collections.shuffle(cards, rng); }
        Card deal() {
            if (cards.isEmpty()) reset();
            return cards.remove(cards.size()-1);
        }
    }

    // ====== Hand ======
    static class Hand {
        final List<Card> cards = new ArrayList<>();
        double bet = 0.0;
        boolean doubled = false;
        boolean isSplitAces = false; // special rule: one card and stand

        void add(Card c) { cards.add(c); }
        int value() {
            int sum = 0, aces = 0;
            for (Card c: cards) {
                sum += c.rank.value;
                if (c.rank == Rank.ACE) aces++;
            }
            // Count Aces as 1 if needed
            while (sum > 21 && aces > 0) {
                sum -= 10; // turn an Ace from 11 to 1
                aces--;
            }
            return sum;
        }
        boolean isBlackjack() { return cards.size() == 2 && value() == 21; }
        boolean isBusted() { return value() > 21; }
        boolean canDouble() { return cards.size() == 2 && !doubled; }
        boolean canSplit() {
            return cards.size() == 2 && cards.get(0).rank == cards.get(1).rank;
        }
        @Override public String toString() {
            return cards + " (" + value() + ")";
        }
    }

    // ====== Game ======
    static class Game {
        final Scanner in = new Scanner(System.in);
        final Deck deck = new Deck();
        double bankroll = 500.0; // start amount (editable)
        boolean allowReshuffleEachRound = true; // simple: fresh deck each round

        void run() {
            System.out.println("=== Console Blackjack ===");
            System.out.println("Start bankroll: $" + bankroll);
            System.out.println("Blackjack pays 3:2 • Dealer stands on soft 17");
            System.out.println("Commands: H = Hit, S = Stand, D = Double, P = Split (when allowed), Q = Quit\n");

            while (bankroll > 0.0) {
                if (allowReshuffleEachRound) deck.reset();
                double bet = promptBet();
                if (bet == -1) break;
                if (bet > bankroll) {
                    System.out.println("You can't bet more than your bankroll.");
                    continue;
                }

                Hand dealer = new Hand();
                Hand player = new Hand();
                player.bet = bet;

                // initial deal
                player.add(deck.deal());
                dealer.add(deck.deal());
                player.add(deck.deal());
                dealer.add(deck.deal());

                System.out.println("\nDealer shows: " + dealer.cards.get(0));
                System.out.println("Your hand: " + player);

                // Check naturals
                boolean playerBJ = player.isBlackjack();
                boolean dealerBJ = dealer.isBlackjack();
                if (playerBJ || dealerBJ) {
                    revealDealer(dealer);
                    if (playerBJ && dealerBJ) {
                        System.out.println("Push: both have Blackjack.");
                        // no money change
                    } else if (playerBJ) {
                        double win = bet * 1.5;
                        bankroll += win;
                        System.out.println("Blackjack! You win $" + win + " (pays 3:2).");
                    } else {
                        bankroll -= bet;
                        System.out.println("Dealer has Blackjack. You lose $" + bet + ".");
                    }
                    showBankroll();
                    if (!playAgain()) break;
                    else continue;
                }

                // Player play: support single split
                List<Hand> playerHands = new ArrayList<>();
                playerHands.add(player);
                boolean splitUsed = false;

                // Offer split if allowed
                if (player.canSplit()) {
                    System.out.print("You can split (" + player.cards.get(0) + " " + player.cards.get(1) + "). Split? (P to split, anything else to skip): ");
                    String s = in.nextLine().trim().toUpperCase(Locale.ROOT);
                    if (s.equals("P")) {
                        if (bankroll < bet) {
                            System.out.println("Not enough bankroll to split (need another $" + bet + ").");
                        } else {
                            bankroll -= bet; // place second bet
                            Hand h1 = new Hand(); h1.bet = bet;
                            Hand h2 = new Hand(); h2.bet = bet;

                            Card c1 = player.cards.get(0);
                            Card c2 = player.cards.get(1);
                            h1.add(c1);
                            h2.add(c2);

                            // If split Aces rule: one card to each, forced stand
                            if (c1.rank == Rank.ACE && c2.rank == Rank.ACE) {
                                h1.isSplitAces = true;
                                h2.isSplitAces = true;
                            }

                            // Deal one card to each split hand
                            h1.add(deck.deal());
                            h2.add(deck.deal());

                            playerHands.clear();
                            playerHands.add(h1);
                            playerHands.add(h2);
                            splitUsed = true;

                            System.out.println("Split hands:");
                            for (int i = 0; i < playerHands.size(); i++) {
                                System.out.println("  Hand " + (i+1) + ": " + playerHands.get(i));
                            }
                        }
                    }
                }

                // Play each player hand
                for (int idx = 0; idx < playerHands.size(); idx++) {
                    Hand ph = playerHands.get(idx);
                    System.out.println("\n-- Playing your Hand " + (playerHands.size() > 1 ? (idx+1) : "") + ": " + ph);

                    // If split Aces: auto-stand after one card each
                    if (ph.isSplitAces) {
                        System.out.println("Split Aces: one card only -> auto-stand.");
                        continue;
                    }

                    boolean firstAction = true;
                    while (true) {
                        System.out.print("Choose action [H=Hit, S=Stand" +
                                (ph.canDouble() && firstAction && bankroll >= ph.bet ? ", D=Double" : "") +
                                "]: ");
                        String cmd = in.nextLine().trim().toUpperCase(Locale.ROOT);

                        if (cmd.equals("D") && ph.canDouble() && firstAction && bankroll >= ph.bet) {
                            // Double down: take one card, double bet, then stand
                            bankroll -= ph.bet;
                            ph.bet *= 2.0;
                            ph.doubled = true;
                            Card c = deck.deal();
                            ph.add(c);
                            System.out.println("You doubled and drew " + c + ". Hand: " + ph);
                            if (ph.isBusted()) System.out.println("Busted!");
                            break;
                        } else if (cmd.equals("H")) {
                            Card c = deck.deal();
                            ph.add(c);
                            System.out.println("You drew " + c + ". Hand: " + ph);
                            if (ph.isBusted()) {
                                System.out.println("Busted!");
                                break;
                            }
                        } else if (cmd.equals("S")) {
                            System.out.println("You stand with " + ph.value() + ".");
                            break;
                        } else {
                            System.out.println("Invalid or unavailable choice.");
                            continue;
                        }
                        firstAction = false;
                    }
                }

                // If all player hands busted, dealer doesn't need to play
                boolean anyAlive = playerHands.stream().anyMatch(h -> !h.isBusted());
                // Dealer play
                if (anyAlive) {
                    revealDealer(dealer);
                    dealerPlay(dealer);
                }

                // Resolve each hand
                for (int i = 0; i < playerHands.size(); i++) {
                    Hand ph = playerHands.get(i);
                    String tag = playerHands.size() > 1 ? ("Hand " + (i+1)) : "Hand";
                    if (ph.isBusted()) {
                        bankroll -= ph.bet;
                        System.out.println(tag + " loses $" + ph.bet + " (bust).");
                        continue;
                    }
                    int dv = dealer.value();
                    int pv = ph.value();
                    if (dealer.isBusted()) {
                        bankroll += ph.bet;
                        System.out.println(tag + " wins $" + ph.bet + " (dealer bust).");
                    } else if (pv > dv) {
                        bankroll += ph.bet;
                        System.out.println(tag + " wins $" + ph.bet + ".");
                    } else if (pv < dv) {
                        bankroll -= ph.bet;
                        System.out.println(tag + " loses $" + ph.bet + ".");
                    } else {
                        System.out.println(tag + " pushes (no money won or lost).");
                    }
                }

                showBankroll();
                if (!playAgain()) break;
            }

            System.out.println("\nThanks for playing!");
        }

        // ====== Helpers ======
        double promptBet() {
            while (true) {
                System.out.print("\nEnter your bet (or Q to quit) [Bankroll $" + String.format(Locale.US, "%.2f", bankroll) + "]: ");
                String s = in.nextLine().trim();
                if (s.equalsIgnoreCase("Q")) return -1;
                try {
                    double b = Double.parseDouble(s);
                    if (b <= 0) {
                        System.out.println("Bet must be positive.");
                        continue;
                    }
                    if (b > bankroll) {
                        System.out.println("Bet cannot exceed bankroll.");
                        continue;
                    }
                    return b;
                } catch (NumberFormatException e) {
                    System.out.println("Enter a valid number or Q.");
                }
            }
        }

        void revealDealer(Hand dealer) {
            System.out.println("Dealer's hand: " + dealer);
        }

        // Dealer stands on soft 17
        void dealerPlay(Hand dealer) {
            while (true) {
                int val = dealer.value();
                boolean soft = isSoft(dealer);
                if (val < 17) {
                    Card c = deck.deal();
                    dealer.add(c);
                    System.out.println("Dealer hits: " + dealer);
                    if (dealer.isBusted()) {
                        System.out.println("Dealer busts!");
                        return;
                    }
                } else if (val == 17 && soft == true) {
                    // stands on soft 17 per rule (change if you want hit on soft 17)
                    System.out.println("Dealer stands on soft 17: " + dealer);
                    return;
                } else {
                    System.out.println("Dealer stands: " + dealer);
                    return;
                }
            }
        }

        boolean isSoft(Hand h) {
            // Soft if an Ace counted as 11 is present
            int sum = 0, aces = 0;
            for (Card c: h.cards) {
                sum += c.rank.value;
                if (c.rank == Rank.ACE) aces++;
            }
            while (sum > 21 && aces > 0) {
                sum -= 10;
                aces--;
            }
            // If any Ace still counted as 11, soft
            for (Card c: h.cards) {
                if (c.rank == Rank.ACE) {
                    // recompute with this ace as 11 assumption
                    // If current value minus (rank.value=11) + 11 == value(), then it's still 11
                    // Simpler: If initial sum <= 21 and we had at least one Ace before reductions
                    // We'll compute more directly:
                    // If there exists a configuration where one Ace is 11 -> value() unchanged and <=21 -> soft
                }
            }
            // A simpler and reliable way:
            // If value <= 11 - but this doesn't detect softness. Let's compute explicitly:
            int hardTotal = 0;
            int aceCount = 0;
            for (Card c: h.cards) {
                if (c.rank == Rank.ACE) {
                    aceCount++;
                    hardTotal += 1;
                } else {
                    hardTotal += c.rank.value;
                }
            }
            // If we can add 10 (turn one Ace from 1 to 11) without busting, it's soft
            return (aceCount > 0 && hardTotal + 10 <= 21);
        }

        void showBankroll() {
            System.out.println("Bankroll: $" + String.format(Locale.US, "%.2f", bankroll));
        }

        boolean playAgain() {
            while (true) {
                System.out.print("\nPlay another round? (Y/N): ");
                String s = in.nextLine().trim().toUpperCase(Locale.ROOT);
                if (s.equals("Y")) return true;
                if (s.equals("N")) return false;
                System.out.println("Please enter Y or N.");
            }
        }
    }

    public static void main(String[] args) {
        new Game().run();
    }
}
