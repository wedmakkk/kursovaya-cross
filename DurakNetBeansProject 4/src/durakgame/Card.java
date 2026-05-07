package durakgame;

import java.util.Objects;

// Модель одной карты: масть + достоинство. Immutable-класс, чтобы карту нельзя было случайно изменить.
public final class Card implements Comparable<Card> {
    private final Suit suit;
    private final Rank rank;

    public Card(Suit suit, Rank rank) {
        // Objects.requireNonNull защищает от создания карты без масти или достоинства.
        this.suit = Objects.requireNonNull(suit);
        this.rank = Objects.requireNonNull(rank);
    }

    public Suit getSuit() { return suit; }
    public Rank getRank() { return rank; }

    // Проверка правила боя: старшая карта той же масти или любой козырь против некозыря.
    public boolean beats(Card other, Suit trump) {
        if (other == null) return false;
        if (suit == other.suit && rank.getValue() > other.rank.getValue()) return true;
        return suit == trump && other.suit != trump;
    }

    public boolean isTrump(Suit trump) { return suit == trump; }

    @Override
    public int compareTo(Card other) {
        // Сортировка сначала по масти, затем по значению карты.
        int suitCompare = this.suit.ordinal() - other.suit.ordinal();
        return suitCompare != 0 ? suitCompare : this.rank.getValue() - other.rank.getValue();
    }

    @Override
    public String toString() { return rank.getLabel() + suit.getSymbol(); }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Card)) return false;
        Card card = (Card) o;
        return suit == card.suit && rank == card.rank;
    }

    @Override
    public int hashCode() { return Objects.hash(suit, rank); }
}
