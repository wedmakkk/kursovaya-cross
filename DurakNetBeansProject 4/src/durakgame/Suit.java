package durakgame;

// Масти карт: символ нужен для кнопок, title — для текстовых сообщений.
public enum Suit {
    HEARTS("♥", "Черви"), DIAMONDS("♦", "Бубны"), CLUBS("♣", "Трефы"), SPADES("♠", "Пики");
    private final String symbol;
    private final String title;

    Suit(String symbol, String title) { this.symbol = symbol; this.title = title; }
    public String getSymbol() { return symbol; }
    public String getTitle() { return title; }
}
