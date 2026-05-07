package durakgame;

// Собственное исключение для ошибок игровой логики: неверный ход, нельзя взять, нельзя подкинуть и т.п.
public class GameException extends Exception {
    public GameException(String message) { super(message); }
}
