package durakgame;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

// Главное окно игры. Вся графика сделана на Java Swing без сторонних библиотек.
public class DurakFrame extends JFrame {
    private final GameEngine engine = new GameEngine();
    // ExecutorService запускает ход компьютера в отдельном потоке, чтобы интерфейс не зависал.
    private final ExecutorService aiExecutor = Executors.newSingleThreadExecutor();

    // Элементы интерфейса: подписи, стол, рука игрока, журнал и кнопки управления.
    private final JLabel statusLabel = new JLabel(" ");
    private final JLabel trumpLabel = new JLabel(" ");
    private final JLabel computerLabel = new JLabel(" ");
    private final JPanel tablePanel = new JPanel(new GridLayout(0, 1, 6, 6));
    private final JPanel handPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
    private final JTextArea logArea = new JTextArea(8, 60);
    private final JButton doneButton = new JButton("Завершить ход");
    private final JButton takeButton = new JButton("Взять");
    private final JButton newGameButton = new JButton("Новая игра");

    public DurakFrame() {
        super("Подкидной дурак — Java Swing");
        // Сетевого взаимодействия больше нет: игра полностью автономная и работает только локально.
        initUi();
        initEvents();
        startNewGame();
    }

    // Создание и размещение Swing-компонентов на форме.
    private void initUi() {
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setMinimumSize(new Dimension(900, 620));
        setLocationRelativeTo(null);

        JPanel top = new JPanel(new GridLayout(3, 1));
        statusLabel.setFont(statusLabel.getFont().deriveFont(Font.BOLD, 15f));
        top.add(statusLabel);
        top.add(trumpLabel);
        top.add(computerLabel);
        top.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        add(top, BorderLayout.NORTH);

        JPanel center = new JPanel(new BorderLayout(8, 8));
        tablePanel.setBorder(BorderFactory.createTitledBorder("Стол"));
        center.add(tablePanel, BorderLayout.CENTER);
        logArea.setEditable(false);
        logArea.setLineWrap(true);
        center.add(new JScrollPane(logArea), BorderLayout.SOUTH);
        center.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        add(center, BorderLayout.CENTER);

        JPanel bottom = new JPanel(new BorderLayout());
        handPanel.setBorder(BorderFactory.createTitledBorder("Ваши карты — нажмите на карту, чтобы походить или отбиться"));
        bottom.add(new JScrollPane(handPanel), BorderLayout.CENTER);

        JPanel controls = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        controls.add(doneButton);
        controls.add(takeButton);
        controls.add(newGameButton);
        bottom.add(controls, BorderLayout.SOUTH);
        add(bottom, BorderLayout.SOUTH);
    }

    // Назначение обработчиков событий для кнопок и закрытия окна.
    private void initEvents() {
        newGameButton.addActionListener(e -> startNewGame());
        doneButton.addActionListener(e -> runPlayerAction(() -> engine.humanDone()));
        takeButton.addActionListener(e -> runPlayerAction(() -> engine.humanTake()));
        addWindowListener(new WindowAdapter() {
            @Override public void windowClosed(WindowEvent e) {
                // Останавливаем ExecutorService, чтобы фоновый поток компьютера завершился при закрытии окна.
                aiExecutor.shutdownNow();
            }
        });
    }

    private void startNewGame() {
        engine.newGame();
        logArea.setText("");
        appendLog(engine.getLastMessage());
        refresh();
        maybeComputerMove();
    }

    // Общая обёртка для действий игрока: обработка исключений + обновление интерфейса.
    private void runPlayerAction(ThrowingAction action) {
        try {
            action.run();
            appendLog(engine.getLastMessage());
            refresh();
            maybeComputerMove();
        } catch (GameException ex) {
            // Ошибки правил показываем пользователю в диалоговом окне, а приложение продолжает работать.
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Недопустимый ход", JOptionPane.WARNING_MESSAGE);
        }
    }

    // Нажатие на карту означает либо атаку, либо защиту — зависит от текущего состояния игры.
    private void onCardClicked(Card card) {
        GameEngine.Turn turn = engine.getTurn();
        if (turn == GameEngine.Turn.HUMAN_ATTACK) {
            runPlayerAction(() -> engine.humanAttack(card));
        } else if (turn == GameEngine.Turn.HUMAN_DEFEND) {
            runPlayerAction(() -> engine.humanDefend(card));
        } else {
            JOptionPane.showMessageDialog(this, "Сейчас думает компьютер.", "Подождите", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    // Ход компьютера выполняется в отдельном потоке, чтобы окно Swing не зависало.
    private void maybeComputerMove() {
        if (engine.getTurn() != GameEngine.Turn.COMPUTER_THINK) return;
        setButtonsEnabled(false);

        // Runnable — самый простой вариант для потока: задача ничего не возвращает,
        // а только делает ход компьютера и потом просит интерфейс обновиться.
        Runnable computerTask = () -> {
            try {
                // Небольшая пауза имитирует размышление компьютера.
                // Пауза выполняется НЕ в GUI-потоке, поэтому окно не замораживается.
                Thread.sleep(650);
            } catch (InterruptedException ignored) {
                // Если приложение закрывают во время паузы, корректно помечаем поток прерванным.
                Thread.currentThread().interrupt();
                return;
            }

            // Логика хода компьютера выполняется в фоновом потоке ExecutorService.
            engine.computerStep();

            // Swing-компоненты можно менять только в GUI-потоке,
            // поэтому обновление окна передаём через SwingUtilities.invokeLater.
            SwingUtilities.invokeLater(() -> {
                appendLog(engine.getLastMessage());
                refresh();
                // Иногда компьютер должен сделать несколько действий подряд,
                // поэтому после обновления проверяем, нужен ли ещё один фоновый ход.
                if (engine.getTurn() == GameEngine.Turn.COMPUTER_THINK) maybeComputerMove();
            });
        };

        // ExecutorService запускает Runnable в отдельном рабочем потоке.
        aiExecutor.submit(computerTask);
    }

    // Перерисовка всех данных: статус, стол, карты игрока и доступность кнопок.
    private void refresh() {
        statusLabel.setText("Состояние: " + turnText(engine.getTurn()) + " | Колода: " + engine.getDeckSize());
        trumpLabel.setText("Козырь: " + engine.getTrump().getTitle() + " " + engine.getTrump().getSymbol());
        computerLabel.setText("У компьютера карт: " + engine.getComputer().count());

        tablePanel.removeAll();
        if (engine.getTable().isEmpty()) {
            tablePanel.add(new JLabel("На столе пока нет карт."));
        } else {
            // forEach по коллекции пар на столе — простой пример работы с Collections.
            engine.getTable().forEach(pair -> {
                JLabel label = new JLabel(pair.toString());
                label.setFont(label.getFont().deriveFont(Font.BOLD, 24f));
                label.setBorder(BorderFactory.createEmptyBorder(8, 16, 8, 16));
                tablePanel.add(label);
            });
        }

        handPanel.removeAll();
        // Для каждой карты игрока создаётся отдельная кнопка.
        engine.getHuman().getHand().forEach(card -> {
            JButton button = new JButton(card.toString());
            button.setFont(button.getFont().deriveFont(Font.BOLD, 20f));
            button.setPreferredSize(new Dimension(72, 48));
            if (card.getSuit() == Suit.HEARTS || card.getSuit() == Suit.DIAMONDS) {
                button.setForeground(Color.RED.darker());
            }
            button.setEnabled(engine.getTurn() == GameEngine.Turn.HUMAN_ATTACK || engine.getTurn() == GameEngine.Turn.HUMAN_DEFEND);
            button.addActionListener(e -> onCardClicked(card));
            handPanel.add(button);
        });

        doneButton.setEnabled(engine.getTurn() == GameEngine.Turn.HUMAN_ATTACK && engine.allCovered());
        takeButton.setEnabled(engine.getTurn() == GameEngine.Turn.HUMAN_DEFEND);
        if (engine.getTurn() == GameEngine.Turn.GAME_OVER) {
            setButtonsEnabled(false);
            newGameButton.setEnabled(true);
        }
        revalidate();
        repaint();
    }

    // Включение или выключение кнопок во время хода компьютера.
    private void setButtonsEnabled(boolean enabled) {
        doneButton.setEnabled(enabled);
        takeButton.setEnabled(enabled);
        for (java.awt.Component component : handPanel.getComponents()) component.setEnabled(enabled);
    }

    // Человекочитаемый текст для состояния игры.
    private String turnText(GameEngine.Turn turn) {
        switch (turn) {
            case HUMAN_ATTACK: return "ваша атака";
            case HUMAN_DEFEND: return "ваша защита";
            case COMPUTER_THINK: return "ходит компьютер";
            case GAME_OVER: return "игра окончена";
            default: return turn.toString();
        }
    }

    private void appendLog(String text) { logArea.append(text + System.lineSeparator()); }

    // Функциональный интерфейс позволяет передавать методы, которые могут бросить GameException.
    private interface ThrowingAction { void run() throws GameException; }
}
