package durakgame;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

// Колода из 36 карт. Здесь используется Java Collections Framework: ArrayList, List, Collections.shuffle.
public class Deck {
    private final List<Card> cards = new ArrayList<>();
    private Suit trump;

    public Deck() {
        // Формируем полную колоду из всех мастей и достоинств.
        for (Suit suit : Suit.values()) {
            for (Rank rank : Rank.values()) {
                cards.add(new Card(suit, rank));
            }
        }
        Collections.shuffle(cards); // Перемешивание колоды стандартным методом коллекций.
        trump = cards.get(cards.size() - 1).getSuit(); // Масть последней карты назначается козырной.
    }

    public Suit getTrump() { return trump; }
    public int size() { return cards.size(); }

    // Optional показывает, что карта может отсутствовать, если колода пуста.
    public Optional<Card> draw() {
        if (cards.isEmpty()) return Optional.empty();
        return Optional.of(cards.remove(0));
    }

    // Добор карт игроком до нужного количества.
    public void dealTo(Player player, int maxHand) {
        while (player.count() < maxHand && !cards.isEmpty()) {
            player.add(draw().get());
        }
        player.sort(trump);
    }
}
