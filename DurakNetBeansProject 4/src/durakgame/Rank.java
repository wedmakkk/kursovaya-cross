package durakgame;

// Достоинства карт от шестёрки до туза. value нужен для сравнения силы карт.
public enum Rank {
    SIX(6, "6"), SEVEN(7, "7"), EIGHT(8, "8"), NINE(9, "9"), TEN(10, "10"), JACK(11, "В"), QUEEN(12, "Д"), KING(13, "К"), ACE(14, "Т");
    private final int value;
    private final String label;

    Rank(int value, String label) { this.value = value; this.label = label; }
    public int getValue() { return value; }
    public String getLabel() { return label; }
}
