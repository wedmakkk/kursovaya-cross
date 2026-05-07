package durakgame;

// Пара карт на столе: атакующая карта и карта, которой её отбили.
public class MovePair {
    private final Card attack;
    private Card defense;

    public MovePair(Card attack) { this.attack = attack; }
    public Card getAttack() { return attack; }
    public Card getDefense() { return defense; }

    // Если defense не null, значит атакующая карта уже побита.
    public boolean isCovered() { return defense != null; }
    public void cover(Card card) { this.defense = card; }

    @Override
    public String toString() { return attack + " → " + (defense == null ? "?" : defense.toString()); }
}
