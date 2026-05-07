Проект: Подкидной дурак на Java для Apache NetBeans

Как открыть:
1. Распакуйте ZIP.
2. В Apache NetBeans выберите File -> Open Project.
3. Укажите папку DurakNetBeansProject.
4. Запустите проект кнопкой Run или через класс durakgame.Main.

Что реализовано:
- Графический интерфейс: Java Swing, окно DurakFrame.
- Игра: упрощённая партия в подкидного дурака 1 на 1 против компьютера.
- Колода: 36 карт, козырь, раздача до 6 карт, атака, защита, взятие, бита, проверка победителя.
- Java Collections Framework: ArrayList, List, Comparator и коллекции карт/ходов.
- Механизм обработки исключений: GameException для неправильных ходов игрока.
- Java Stream API: фильтрация возможных атак/защит, сбор карт со стола, поиск минимального козыря, сортировка вариантов хода.
- Java Multithreading: отдельный поток компьютера через ExecutorService и Runnable.

Что изменено в этой версии:
- UDP и всё сетевое взаимодействие полностью удалены.
- Удалён класс NetworkAnnouncer.java.
- Игра теперь полностью автономная: нет клиента, сервера, сокетов и сетевых сообщений.
- В коде добавлены понятные комментарии около многопоточности: где создаётся ExecutorService, зачем нужен Runnable, почему GUI обновляется через SwingUtilities.invokeLater.

Основные классы:
- Main.java — точка входа.
- DurakFrame.java — Swing GUI и запуск фонового потока компьютера.
- GameEngine.java — логика партии.
- Card.java, Suit.java, Rank.java, Deck.java, Player.java, MovePair.java — модель игры.
- GameException.java — пользовательское исключение.
