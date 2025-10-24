import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * Utilitaire pour générer un hash BCrypt
 *
 * Compilez et exécutez avec :
 * javac -cp "target/classes;C:\Users\...\.m2\repository\org\springframework\security\spring-security-crypto\6.x.x\*.jar" GeneratePasswordHash.java
 * java -cp ".;target/classes;..." GeneratePasswordHash
 *
 * OU simplement exécutez depuis votre IDE
 */
public class GeneratePasswordHash {
    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

        // Mot de passe par défaut pour le premier ADMIN
        String password = "admin123";
        String hash = encoder.encode(password);

        System.out.println("=".repeat(80));
        System.out.println("HASH BCRYPT GÉNÉRÉ");
        System.out.println("=".repeat(80));
        System.out.println();
        System.out.println("Mot de passe : " + password);
        System.out.println();
        System.out.println("Hash BCrypt :");
        System.out.println(hash);
        System.out.println();
        System.out.println("=".repeat(80));
        System.out.println("Copiez ce hash dans le fichier create_admin.sql");
        System.out.println("=".repeat(80));
    }
}
