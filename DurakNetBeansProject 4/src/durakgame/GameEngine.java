package durakgame;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

// Главный класс игровой логики: здесь хранятся колода, игроки, стол и текущее состояние хода.
public class GameEngine {
    // Перечисление состояний игры помогает не путать, кто сейчас ходит или защищается.
    public enum Turn { HUMAN_ATTACK, HUMAN_DEFEND, COMPUTER_THINK, GAME_OVER }

    private Deck deck;
    private final Player human = new Player("Игрок");
    private final Player computer = new Player("Компьютер");
    private final List<MovePair> table = new ArrayList<>(); // Карты на столе храним в списке пар атака-защита.
    private Turn turn;
    private boolean humanAttacker;
    private String lastMessage = "";

    // Полная инициализация новой партии.
    public void newGame() {
        deck = new Deck();
        human.clear();
        computer.clear();
        table.clear();
        deck.dealTo(human, 6);
        deck.dealTo(computer, 6);
        // Первый ходит игрок с младшим козырем.
        humanAttacker = humanLowestTrumpValue() <= computerLowestTrumpValue();
        turn = humanAttacker ? Turn.HUMAN_ATTACK : Turn.COMPUTER_THINK;
        lastMessage = "Новая игра. Козырь: " + deck.getTrump().getTitle() + ". "
                + (humanAttacker ? "Вы ходите." : "Первым ходит компьютер.");
    }

    private int humanLowestTrumpValue() { return lowestTrumpValue(human); }
    private int computerLowestTrumpValue() { return lowestTrumpValue(computer); }

    // Stream API: ищем минимальный козырь в руке игрока.
    private int lowestTrumpValue(Player p) {
        return p.getHand().stream()
                .filter(c -> c.getSuit() == deck.getTrump())
                .mapToInt(c -> c.getRank().getValue())
                .min().orElse(100);
    }

    // Ход игрока в атаке. При нарушении правил бросается GameException.
    public void humanAttack(Card card) throws GameException {
        ensure(turn == Turn.HUMAN_ATTACK, "Сейчас не ваш ход атаки.");
        validateAttack(human, card);
        human.remove(card);
        table.add(new MovePair(card));
        lastMessage = "Вы подкинули " + card + ". Компьютер защищается.";
        turn = Turn.COMPUTER_THINK;
    }

    // Защита игрока: выбранная карта должна бить первую открытую карту на столе.
    public void humanDefend(Card card) throws GameException {
        ensure(turn == Turn.HUMAN_DEFEND, "Сейчас не ваша защита.");
        Optional<MovePair> open = firstOpenPair();
        ensure(open.isPresent(), "На столе нет карты, которую нужно бить.");
        Card attack = open.get().getAttack();
        ensure(card.beats(attack, deck.getTrump()), card + " не бьёт " + attack + ".");
        human.remove(card);
        open.get().cover(card);
        lastMessage = "Вы отбили " + attack + " картой " + card + ".";
        if (allCovered()) turn = Turn.COMPUTER_THINK;
    }

    // Игрок завершает атаку только когда все карты компьютером отбиты.
    public void humanDone() throws GameException {
        ensure(turn == Turn.HUMAN_ATTACK, "Закончить ход можно только во время вашей атаки.");
        ensure(!table.isEmpty() && allCovered(), "Сначала компьютер должен отбиться от всех карт.");
        finishRound(true);
        humanAttacker = false;
        turn = Turn.COMPUTER_THINK;
        lastMessage = "Ход завершён. Теперь атакует компьютер.";
    }

    // Игрок берёт карты со стола, если не может отбиться.
    public void humanTake() throws GameException {
        ensure(turn == Turn.HUMAN_DEFEND, "Взять можно только во время вашей защиты.");
        human.addAll(collectTableCards());
        table.clear();
        drawAfterRound(computer, human);
        humanAttacker = false;
        turn = Turn.COMPUTER_THINK;
        checkGameOver();
        if (turn != Turn.GAME_OVER) lastMessage = "Вы взяли карты. Компьютер ходит снова.";
    }

    // Один шаг искусственного интеллекта компьютера.
    public void computerStep() {
        if (turn == Turn.GAME_OVER) return;
        try {
            if (humanAttacker) computerDefendOrTake(); else computerAttackOrFinish();
        } catch (GameException ex) {
            // Исключение не роняет программу, а превращается в сообщение для пользователя.
            lastMessage = ex.getMessage();
        }
        checkGameOver();
    }

    private void computerDefendOrTake() throws GameException {
        Optional<MovePair> open = firstOpenPair();
        if (!open.isPresent()) {
            if (allCovered()) {
                turn = Turn.HUMAN_ATTACK;
                lastMessage = "Компьютер отбился. Можете подкинуть ещё или завершить ход.";
            }
            return;
        }
        Card attack = open.get().getAttack();
        // Компьютер выбирает самую дешёвую подходящую карту для защиты.
        Optional<Card> defense = computer.possibleDefenses(attack, deck.getTrump()).stream()
                .min(cardComparator());
        if (defense.isPresent()) {
            computer.remove(defense.get());
            open.get().cover(defense.get());
            lastMessage = "Компьютер отбил " + attack + " картой " + defense.get() + ".";
            turn = Turn.HUMAN_ATTACK;
        } else {
            // Если защиты нет, компьютер забирает все карты со стола.
            computer.addAll(collectTableCards());
            table.clear();
            drawAfterRound(human, computer);
            humanAttacker = true;
            turn = Turn.HUMAN_ATTACK;
            lastMessage = "Компьютер не смог отбиться и взял карты. Вы ходите снова.";
        }
    }

    private void computerAttackOrFinish() throws GameException {
        if (table.isEmpty() || allCovered()) {
            if (!table.isEmpty()) {
                finishRound(true);
                humanAttacker = true;
                turn = Turn.HUMAN_ATTACK;
                lastMessage = "Компьютер завершил ход. Карты ушли в биту. Теперь ходите вы.";
                return;
            }
            // Stream API: варианты атаки сортируются, чтобы компьютер ходил младшей картой.
            List<Card> variants = computer.possibleAttacks(table).stream()
                    .sorted(cardComparator()).collect(Collectors.toList());
            if (variants.isEmpty()) throw new GameException("У компьютера нет карт для хода.");
            Card attack = variants.get(0);
            computer.remove(attack);
            table.add(new MovePair(attack));
            turn = Turn.HUMAN_DEFEND;
            lastMessage = "Компьютер ходит: " + attack + ". Выберите карту для защиты или нажмите «Взять».";
        }
    }

    // Завершение раунда: карты со стола уходят в биту, затем игроки добирают карты.
    private void finishRound(boolean drawCards) {
        table.clear();
        if (drawCards) {
            if (humanAttacker) drawAfterRound(human, computer); else drawAfterRound(computer, human);
        }
        checkGameOver();
    }

    // Добор сначала атакующим, затем защищающимся — как в правилах дурака.
    private void drawAfterRound(Player attacker, Player defender) {
        deck.dealTo(attacker, 6);
        deck.dealTo(defender, 6);
    }

    // Stream API: разворачиваем пары атака-защита в один список карт.
    private List<Card> collectTableCards() {
        return table.stream()
                .flatMap(p -> p.isCovered()
                        ? java.util.stream.Stream.of(p.getAttack(), p.getDefense())
                        : java.util.stream.Stream.of(p.getAttack()))
                .collect(Collectors.toList());
    }

    // Проверка правил подкидывания и генерация понятных ошибок через GameException.
    private void validateAttack(Player player, Card card) throws GameException {
        ensure(player.getHand().contains(card), "Такой карты нет в руке.");
        ensure(table.size() < 6, "На столе уже максимум карт для атаки.");
        ensure(computer.count() > table.size(), "Нельзя подкинуть больше карт, чем есть у защищающегося.");
        List<Card> possible = player.possibleAttacks(table);
        ensure(possible.contains(card), "Подкидывать можно только карту того же достоинства, что уже есть на столе.");
    }

    // Находим первую карту на столе, которая ещё не отбита.
    private Optional<MovePair> firstOpenPair() {
        return table.stream().filter(p -> !p.isCovered()).findFirst();
    }

    public boolean allCovered() { return !table.isEmpty() && table.stream().allMatch(MovePair::isCovered); }

    // Компаратор для выбора слабейшей карты: обычные карты раньше козырей, затем по достоинству.
    private Comparator<Card> cardComparator() {
        return Comparator.comparing((Card c) -> c.getSuit() == deck.getTrump())
                .thenComparing(c -> c.getRank().getValue());
    }

    // Победа проверяется только когда колода закончилась.
    private void checkGameOver() {
        if (deck != null && deck.size() == 0) {
            if (!human.hasCards() && !computer.hasCards()) {
                turn = Turn.GAME_OVER;
                lastMessage = "Ничья: у обоих закончились карты.";
            } else if (!human.hasCards()) {
                turn = Turn.GAME_OVER;
                lastMessage = "Поздравляем! Вы выиграли.";
            } else if (!computer.hasCards()) {
                turn = Turn.GAME_OVER;
                lastMessage = "Компьютер выиграл. Вы остались в дураках.";
            }
        }
    }

    // Универсальная проверка условия. Если условие нарушено — бросаем собственное исключение.
    private void ensure(boolean condition, String message) throws GameException {
        if (!condition) throw new GameException(message);
    }

    // Геттеры используются графическим интерфейсом для обновления экрана.
    public Player getHuman() { return human; }
    public Player getComputer() { return computer; }
    public List<MovePair> getTable() { return table; }
    public Suit getTrump() { return deck.getTrump(); }
    public int getDeckSize() { return deck.size(); }
    public Turn getTurn() { return turn; }
    public String getLastMessage() { return lastMessage; }
    public boolean isHumanAttacker() { return humanAttacker; }
}
