package durakgame;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

// Игрок хранит имя и руку. Рука реализована через ArrayList из Java Collections Framework.
public class Player {
    private final String name;
    private final List<Card> hand = new ArrayList<>();

    public Player(String name) { this.name = name; }
    public String getName() { return name; }
    public List<Card> getHand() { return hand; }
    public void add(Card card) { hand.add(card); sort(null); }
    public void addAll(List<Card> cards) { hand.addAll(cards); sort(null); }
    public boolean remove(Card card) { return hand.remove(card); }
    public int count() { return hand.size(); }
    public boolean hasCards() { return !hand.isEmpty(); }
    public void clear() { hand.clear(); }

    // Сортировка руки: сначала обычные карты, затем козыри, внутри — по масти и рангу.
    public void sort(Suit trump) {
        Comparator<Card> comparator = Comparator
                .comparing((Card c) -> trump != null && c.getSuit() == trump)
                .thenComparing(Card::getSuit)
                .thenComparing(c -> c.getRank().getValue());
        Collections.sort(hand, comparator);
    }

    // Stream API: собираем достоинства карт на столе и ищем, что можно подкинуть.
    public List<Card> possibleAttacks(List<MovePair> table) {
        if (table.isEmpty()) return new ArrayList<>(hand);
        List<Rank> ranks = table.stream()
                .flatMap(p -> p.isCovered()
                        ? java.util.stream.Stream.of(p.getAttack().getRank(), p.getDefense().getRank())
                        : java.util.stream.Stream.of(p.getAttack().getRank()))
                .distinct()
                .collect(Collectors.toList());
        return hand.stream().filter(c -> ranks.contains(c.getRank())).collect(Collectors.toList());
    }

    // Stream API: фильтруем карты, которыми можно отбить атакующую карту.
    public List<Card> possibleDefenses(Card attack, Suit trump) {
        return hand.stream().filter(c -> c.beats(attack, trump)).collect(Collectors.toList());
    }
}
