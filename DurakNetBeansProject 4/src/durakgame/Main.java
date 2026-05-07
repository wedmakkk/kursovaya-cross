package durakgame;

import javax.swing.SwingUtilities;

// Точка входа в программу.
public class Main {
    public static void main(String[] args) {
        // Swing-интерфейс запускается в Event Dispatch Thread, как требует Java Swing.
        SwingUtilities.invokeLater(() -> new DurakFrame().setVisible(true));
    }
}
