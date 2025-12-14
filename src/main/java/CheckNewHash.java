import org.mindrot.jbcrypt.BCrypt;

public class CheckNewHash {
    public static void main(String[] args) {
        String newHash = "$2a$10$ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789AB";
        String password = "admin123";

        System.out.println("=== ПРОВЕРКА НОВОГО ХЭША ===");
        System.out.println("Хэш: " + newHash);
        System.out.println("Длина: " + newHash.length());
        System.out.println("Пароль: " + password);

        boolean check = BCrypt.checkpw(password, newHash);
        System.out.println("BCrypt.checkpw результат: " + check);

        if (!check) {
            System.out.println("\nХэш НЕ подходит. Генерируем новый...");
            String correctHash = BCrypt.hashpw(password, BCrypt.gensalt());
            System.out.println("Правильный хэш: " + correctHash);
            System.out.println("Проверка: " + BCrypt.checkpw(password, correctHash));
        }
    }
}