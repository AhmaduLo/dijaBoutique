package com.example.dijasaliou.service;

import com.example.dijasaliou.dto.ImportPreviewDto;
import com.example.dijasaliou.dto.ImportPreviewDto.ErreurLigne;
import com.example.dijasaliou.dto.ImportResultatDto;
import com.example.dijasaliou.entity.*;
import com.example.dijasaliou.repository.AchatRepository;
import com.example.dijasaliou.repository.DepenseRepository;
import com.example.dijasaliou.repository.VenteRepository;
import jakarta.validation.ConstraintViolationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ImportService {

    private static final int  MAX_LIGNES  = 1000;
    private static final long MAX_OCTETS  = 5L * 1024 * 1024; // 5 MB
    private static final int  BATCH_SIZE  = 50;

    private static final DateTimeFormatter FMT_FR  = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter FMT_ISO = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    // Clés des maps de données parsées (évite les doublons de littéraux — SonarLint S1192)
    private static final String K_UNITE              = "unite";
    private static final String K_PRIX_VENTE         = "prixVente";
    private static final String K_DATE_ACHAT_PARSED  = "dateAchatParsed";
    private static final String K_FOURNISSEUR        = "fournisseur";
    private static final String K_DATE_VENTE_PARSED  = "dateVenteParsed";
    private static final String K_CATEGORIE          = "categorie";
    private static final String K_NOTES              = "notes";
    private static final String K_STATUT             = "statut";
    private static final String STATUT_VALIDE        = "VALIDE";

    private final AchatRepository   achatRepository;
    private final VenteRepository   venteRepository;
    private final DepenseRepository depenseRepository;
    private final StockService      stockService;
    private final TenantService     tenantService;

    /** Auto-injection différée pour appeler les méthodes @Transactional via le proxy Spring */
    @Autowired @Lazy
    private ImportService self;

    // ─────────────────────────────────────────────────────────────────────────
    // PRÉVISUALISATION
    // ─────────────────────────────────────────────────────────────────────────
    public ImportPreviewDto previsualiser(MultipartFile fichier, String typeHint,
            Double margeDefaut, UserEntity utilisateur) {

        validerFichier(fichier);

        List<String[]> rows = parseFichier(fichier);
        if (rows.size() < 2)
            throw new IllegalArgumentException("Fichier vide ou sans données");

        String[] headers     = rows.get(0);
        String   type        = detecterType(headers, typeHint);
        Map<String, Integer> idx = indexColonnes(headers);

        if (rows.size() - 1 > MAX_LIGNES)
            throw new IllegalArgumentException("Trop de lignes (max " + MAX_LIGNES + " par import)");

        List<Map<String, Object>> apercu  = new ArrayList<>();
        List<ErreurLigne>         erreurs = new ArrayList<>();
        int lignesValides   = 0;
        int lignesInvalides = 0;

        for (int i = 1; i < rows.size(); i++) {
            String[] cols = rows.get(i);
            if (estLigneVide(cols)) continue;

            try {
                Map<String, Object> donnees = parseLigne(cols, i + 1, idx, type, margeDefaut);
                lignesValides++;
                if (apercu.size() < 5) apercu.add(filtrerPourApercu(donnees));
            } catch (IllegalArgumentException e) {
                lignesInvalides++;
                erreurs.add(ErreurLigne.builder()
                        .numeroLigne(i + 1)
                        .colonne(extraireColonne(e.getMessage()))
                        .message(e.getMessage())
                        .build());
            }
        }

        return ImportPreviewDto.builder()
                .type(type)
                .totalLignes(lignesValides + lignesInvalides)
                .lignesValides(lignesValides)
                .lignesInvalides(lignesInvalides)
                .apercu(apercu)
                .erreurs(erreurs)
                .build();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // FILTRE APERÇU — retire les champs internes (LocalDate) non attendus par le frontend
    // ─────────────────────────────────────────────────────────────────────────
    private Map<String, Object> filtrerPourApercu(Map<String, Object> donnees) {
        Map<String, Object> apercu = new LinkedHashMap<>(donnees);
        apercu.remove(K_DATE_ACHAT_PARSED);
        apercu.remove(K_DATE_VENTE_PARSED);
        apercu.remove("dateDepenseParsed");
        apercu.remove("numeroLigne");
        return apercu;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CONFIRMATION / IMPORT EN BASE
    // Phase 1 (sans @Transactional) : parsing + résolution prix stock
    // Phase 2 (@Transactional)       : sauvegarde via self-proxy
    // Séparer les deux phases évite que stockService.obtenirStockParNomProduit()
    // (qui est @Transactional readOnly) marque la tx d'écriture comme rollback-only
    // lorsqu'il lance une RuntimeException ("Produit non trouvé").
    // ─────────────────────────────────────────────────────────────────────────
    public ImportResultatDto confirmer(MultipartFile fichier, String typeHint,
            Double margeDefaut, UserEntity utilisateur) {

        validerFichier(fichier);

        List<String[]> rows = parseFichier(fichier);
        if (rows.size() < 2)
            throw new IllegalArgumentException("Fichier vide ou sans données");

        String[] headers = rows.get(0);
        String   type    = detecterType(headers, typeHint);
        Map<String, Integer> idx = indexColonnes(headers);

        if (rows.size() - 1 > MAX_LIGNES)
            throw new IllegalArgumentException("Trop de lignes (max " + MAX_LIGNES + " par import)");

        // ── Phase 1 : parsing (appels stockService sans tx parente) ──────────
        List<Map<String, Object>> donneesParsees = new ArrayList<>();
        List<ImportResultatDto.ErreurLigne> erreursParse = new ArrayList<>();
        int ignoreesParsePhase = 0;

        for (int i = 1; i < rows.size(); i++) {
            String[] cols = rows.get(i);
            if (estLigneVide(cols)) continue;

            try {
                donneesParsees.add(parseLigne(cols, i + 1, idx, type, margeDefaut));
            } catch (IllegalArgumentException e) {
                ignoreesParsePhase++;
                erreursParse.add(ImportResultatDto.ErreurLigne.builder()
                        .numeroLigne(i + 1)
                        .message(nettoyerMessage(e.getMessage()))
                        .build());
            }
        }

        // ── Phase 2 : sauvegarde transactionnelle via proxy Spring ────────────
        // L'invalidation du cache est faite ICI (hors @Transactional) et non dans
        // sauvegarderDonneesParsees(), afin de garantir que le cache n'est évincé
        // qu'APRÈS le commit. Sans ça, le cache pouvait être vidé avant commit,
        // puis re-rempli avec les anciennes données avant que le commit ne soit visible.
        ImportResultatDto resultat = self.sauvegarderDonneesParsees(
                donneesParsees, type, utilisateur, erreursParse, ignoreesParsePhase);

        if (resultat.getImportees() > 0 && (type.equals("achats") || type.equals("ventes"))) {
            TenantEntity tenant = tenantService.getCurrentTenant();
            stockService.invalidateStockCache(tenant.getTenantUuid());
        }

        return resultat;
    }

    /**
     * Phase 2 de l'import : sauvegarde transactionnelle des données déjà parsées.
     * Méthode publique nécessaire pour que Spring AOP intercepte @Transactional.
     */
    @Transactional
    public ImportResultatDto sauvegarderDonneesParsees(
            List<Map<String, Object>> donneesParsees,
            String type,
            UserEntity utilisateur,
            List<ImportResultatDto.ErreurLigne> erreursParse,
            int ignoreesParsePhase) {

        TenantEntity tenant = tenantService.getCurrentTenant();

        // Pré-charger les existants en UNE seule requête puis comparer en mémoire
        // (évite N+1 requêtes — 1000 lignes = 1 requête au lieu de 1000)
        Set<String> cleesExistantes = chargerCleesDoublons(donneesParsees, type);

        List<ImportResultatDto.ErreurLigne> tousErreurs = new ArrayList<>(erreursParse);
        int importees = 0;
        int ignorees  = ignoreesParsePhase;

        List<Object> batch = new ArrayList<>();

        for (Map<String, Object> donnees : donneesParsees) {
            Object entite = construireEntite(donnees, type, utilisateur, tenant);
            if (entite == null) continue;

            String cle = cleDoublon(entite, type);
            if (cle != null && cleesExistantes.contains(cle)) {
                ignorees++;
                tousErreurs.add(ImportResultatDto.ErreurLigne.builder()
                        .numeroLigne((Integer) donnees.get("numeroLigne"))
                        .message("Cette ligne existe déjà — elle a été ignorée pour éviter les doublons.")
                        .build());
                continue;
            }
            // Ajouter au set pour détecter aussi les doublons intra-fichier
            if (cle != null) cleesExistantes.add(cle);

            batch.add(entite);
            if (batch.size() >= BATCH_SIZE) {
                importees += sauvegarderBatch(batch, type);
                batch.clear();
            }
        }

        if (!batch.isEmpty()) {
            importees += sauvegarderBatch(batch, type);
        }

        return ImportResultatDto.builder()
                .type(type)
                .totalTraitees(importees + ignorees)
                .importees(importees)
                .ignorees(ignorees)
                .erreurs(tousErreurs)
                .message(importees + " " + type + " importé(s) avec succès")
                .build();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // DÉTECTION ET VALIDATION DU TYPE
    // Le typeHint (envoyé par le frontend) est la source de vérité.
    // On vérifie que le fichier contient bien les colonnes du type déclaré.
    // ─────────────────────────────────────────────────────────────────────────
    private String detecterType(String[] headers, String typeHint) {
        Set<String> cols = Arrays.stream(headers)
                .map(this::normaliser)
                .collect(Collectors.toSet());

        // Si le type est déclaré par le frontend → validation stricte
        if (typeHint != null && !typeHint.isBlank()) {
            String type = typeHint.trim().toLowerCase();
            validerColonnesPourType(type, cols);
            return type;
        }

        // Fallback auto-détection (cas rare — typeHint toujours envoyé par le frontend)
        if (cols.contains("prixachat")) return "achats";
        if (cols.contains("description") && cols.contains("montant")) return "depenses";
        if (cols.contains("nomproduit")) return "ventes";

        throw new IllegalArgumentException(
                "Type d'import non spécifié et impossible à détecter automatiquement. " +
                "Colonnes attendues : nomProduit+quantite+prixAchat (achats), " +
                "nomProduit+quantite (ventes), description+montant (dépenses).");
    }

    /**
     * Vérifie que le fichier contient les colonnes obligatoires pour le type déclaré.
     * Empêche d'importer un fichier ventes dans la section achats et vice-versa.
     */
    private void validerColonnesPourType(String type, Set<String> cols) {
        switch (type) {
            case "achats" -> {
                List<String> manquantes = new ArrayList<>();
                if (!cols.contains("nomproduit")) manquantes.add("nomProduit");
                if (!cols.contains("quantite"))   manquantes.add("quantite");
                if (!cols.contains("prixachat"))  manquantes.add("prixAchat");
                if (!manquantes.isEmpty())
                    throw new IllegalArgumentException(
                            "Ce fichier ne correspond pas à un import d'achats. " +
                            "Colonnes manquantes : " + String.join(", ", manquantes) + ". " +
                            "Vérifiez que vous importez le bon fichier.");
            }
            case "ventes" -> {
                List<String> manquantes = new ArrayList<>();
                if (!cols.contains("nomproduit")) manquantes.add("nomProduit");
                if (!cols.contains("quantite"))   manquantes.add("quantite");
                if (!manquantes.isEmpty())
                    throw new IllegalArgumentException(
                            "Ce fichier ne correspond pas à un import de ventes. " +
                            "Colonnes manquantes : " + String.join(", ", manquantes) + ". " +
                            "Vérifiez que vous importez le bon fichier.");
                if (cols.contains("prixachat"))
                    throw new IllegalArgumentException(
                            "Ce fichier semble être un fichier d'achats (colonne prixAchat détectée). " +
                            "Vous êtes dans la section ventes — vérifiez que vous importez le bon fichier.");
            }
            case "depenses" -> {
                List<String> manquantes = new ArrayList<>();
                if (!cols.contains("description")) manquantes.add("description");
                if (!cols.contains("montant"))     manquantes.add("montant");
                if (!manquantes.isEmpty())
                    throw new IllegalArgumentException(
                            "Ce fichier ne correspond pas à un import de dépenses. " +
                            "Colonnes manquantes : " + String.join(", ", manquantes) + ". " +
                            "Vérifiez que vous importez le bon fichier.");
            }
            default -> throw new IllegalArgumentException(
                    "Type d'import invalide : '" + type + "'. Types acceptés : achats, ventes, depenses.");
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PARSING DU FICHIER (CSV ou Excel)
    // ─────────────────────────────────────────────────────────────────────────
    private List<String[]> parseFichier(MultipartFile fichier) {
        String nom = fichier.getOriginalFilename();
        if (nom != null && (nom.endsWith(".xlsx") || nom.endsWith(".xls"))) {
            return parseExcel(fichier);
        }
        return parseCsv(fichier);
    }

    private List<String[]> parseCsv(MultipartFile fichier) {
        List<String[]> rows = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(fichier.getInputStream(), StandardCharsets.UTF_8))) {

            String line;
            String delimiter = null;
            while ((line = reader.readLine()) != null) {
                if (delimiter == null) {
                    // Supprimer le BOM UTF-8 si présent
                    if (line.startsWith("\uFEFF")) line = line.substring(1);
                    // Auto-détection du délimiteur depuis la ligne d'en-tête
                    delimiter = line.contains(";") ? ";" : ",";
                }
                if (line.isBlank()) continue;
                rows.add(line.split(delimiter, -1));
            }
        } catch (IOException e) {
            throw new IllegalArgumentException("Fichier CSV illisible ou corrompu. Vérifiez que le fichier n'est pas endommagé.");
        }
        return rows;
    }

    private List<String[]> parseExcel(MultipartFile fichier) {
        List<String[]> rows = new ArrayList<>();
        try (Workbook wb = ouvrirWorkbook(fichier)) {
            Sheet sheet = wb.getSheetAt(0);
            int nbCols = 0;

            // Calculer le nb de colonnes depuis la première ligne
            Row header = sheet.getRow(0);
            if (header != null) nbCols = header.getLastCellNum();

            for (int i = 0; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;
                String[] cols = new String[nbCols];
                for (int j = 0; j < nbCols; j++) {
                    Cell cell = row.getCell(j);
                    cols[j] = cell != null ? lireCelluleCommeString(cell) : "";
                }
                rows.add(cols);
            }
        } catch (IOException e) {
            throw new IllegalArgumentException("Fichier Excel illisible ou corrompu. Vérifiez que le fichier n'est pas endommagé et qu'il est bien au format .xlsx ou .xls.");
        }
        return rows;
    }

    private Workbook ouvrirWorkbook(MultipartFile fichier) throws IOException {
        String nom = fichier.getOriginalFilename();
        if (nom != null && nom.endsWith(".xls")) return new HSSFWorkbook(fichier.getInputStream());
        return new XSSFWorkbook(fichier.getInputStream());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // INDEX DES COLONNES PAR NOM
    // ─────────────────────────────────────────────────────────────────────────
    private Map<String, Integer> indexColonnes(String[] headers) {
        Map<String, Integer> idx = new LinkedHashMap<>();
        for (int i = 0; i < headers.length; i++) {
            idx.put(normaliser(headers[i]), i);
        }
        return idx;
    }

    private String normaliser(String s) {
        if (s == null) return "";
        return s.trim()
                .toLowerCase()
                .replace("é", "e").replace("è", "e").replace("ê", "e")
                .replace("à", "a").replace("â", "a")
                .replace("î", "i").replace("ô", "o").replace("û", "u")
                .replace(" ", "").replace("_", "");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PARSING D'UNE LIGNE
    // ─────────────────────────────────────────────────────────────────────────
    private Map<String, Object> parseLigne(String[] cols, int numLigne,
            Map<String, Integer> idx, String type, Double margeDefaut) {
        return switch (type) {
            case "achats"   -> parseAchat(cols, numLigne, idx, margeDefaut);
            case "ventes"   -> parseVente(cols, numLigne, idx);
            case "depenses" -> parseDepense(cols, numLigne, idx);
            default         -> throw new IllegalArgumentException("Type inconnu : " + type);
        };
    }

    // ── ACHAT ──────────────────────────────────────────────────────────────
    private Map<String, Object> parseAchat(String[] cols, int numLigne,
            Map<String, Integer> idx, Double margeDefaut) {
        Map<String, Object> d = new LinkedHashMap<>();
        d.put("numeroLigne", numLigne);

        String nomProduit = col(cols, idx, "nomproduit");
        if (nomProduit == null || nomProduit.isBlank())
            throw new IllegalArgumentException("[nomProduit] Nom du produit obligatoire");
        nomProduit = nomProduit.trim();
        if (nomProduit.length() < 2)
            throw new IllegalArgumentException("[nomProduit] Nom du produit trop court (2 caractères minimum)");
        if (nomProduit.length() > 100)
            throw new IllegalArgumentException("[nomProduit] Nom du produit trop long (100 caractères maximum)");
        d.put("nomProduit", nomProduit);

        Double quantite = colDouble(cols, idx, "quantite");
        if (quantite == null || quantite <= 0)
            throw new IllegalArgumentException("[quantite] Quantité manquante ou invalide (doit être > 0)");
        d.put("quantite", quantite);

        String unite = col(cols, idx, "unite");
        d.put(K_UNITE, unite != null && !unite.isBlank() ? unite.trim() : "pièce");

        Double prixAchat = colDouble(cols, idx, "prixachat");
        if (prixAchat == null || prixAchat <= 0)
            throw new IllegalArgumentException("[prixAchat] Prix achat manquant ou invalide");
        d.put("prixAchat", prixAchat);

        Double prixVente = colDouble(cols, idx, "prixvente");
        Map.Entry<Double, String> prixResolu = resolverPrixAchat(nomProduit, prixAchat, prixVente, margeDefaut);
        d.put(K_PRIX_VENTE, prixResolu.getKey());
        d.put("sourcePrixVente", prixResolu.getValue());

        LocalDate date = colDate(cols, idx, "dateachat");
        d.put("dateAchat", date != null ? date.toString() : LocalDate.now().toString());
        d.put(K_DATE_ACHAT_PARSED, date != null ? date : LocalDate.now());

        String fournisseur = col(cols, idx, "fournisseur");
        if (fournisseur != null) fournisseur = fournisseur.trim();
        if (fournisseur != null && fournisseur.length() > 100) fournisseur = fournisseur.substring(0, 100);
        d.put(K_FOURNISSEUR, fournisseur);

        d.put(K_STATUT, STATUT_VALIDE);
        return d;
    }

    // ── VENTE ──────────────────────────────────────────────────────────────
    private Map<String, Object> parseVente(String[] cols, int numLigne, Map<String, Integer> idx) {
        Map<String, Object> d = new LinkedHashMap<>();
        d.put("numeroLigne", numLigne);

        String nomProduit = col(cols, idx, "nomproduit");
        if (nomProduit == null || nomProduit.isBlank())
            throw new IllegalArgumentException("[nomProduit] Nom du produit obligatoire");
        nomProduit = nomProduit.trim();
        if (nomProduit.length() < 2)
            throw new IllegalArgumentException("[nomProduit] Nom du produit trop court (2 caractères minimum)");
        if (nomProduit.length() > 100)
            throw new IllegalArgumentException("[nomProduit] Nom du produit trop long (100 caractères maximum)");
        d.put("nomProduit", nomProduit);

        Double quantite = colDouble(cols, idx, "quantite");
        if (quantite == null || quantite <= 0)
            throw new IllegalArgumentException("[quantite] Quantité manquante ou invalide (doit être > 0)");

        // Vérifier le stock disponible avant d'accepter la ligne
        try {
            var stock = stockService.obtenirStockParNomProduit(nomProduit);
            double dispo = stock.getStockDisponible() != null ? stock.getStockDisponible() : 0.0;
            if (quantite > dispo)
                throw new IllegalArgumentException(String.format(
                        "[quantite] Stock insuffisant pour '%s' : disponible %.0f, demandé %.0f",
                        nomProduit, dispo, quantite));
        } catch (IllegalArgumentException e) {
            throw e; // relancer les erreurs métier (stock insuffisant ou produit introuvable)
        } catch (RuntimeException e) {
            String suggestion = suggererProduit(nomProduit);
            String msg = "[nomProduit] Produit '" + nomProduit + "' introuvable dans le stock";
            throw new IllegalArgumentException(suggestion != null ? msg + " — " + suggestion : msg + " — vérifiez le nom exact");
        }

        d.put("quantite", quantite);

        Double prixVente = colDouble(cols, idx, "prixvente");
        Map.Entry<Double, String> prixResolu = resolverPrixVente(nomProduit, prixVente);
        d.put(K_PRIX_VENTE, prixResolu.getKey());
        d.put("sourcePrixVente", prixResolu.getValue());

        String nomClient = col(cols, idx, "nomclient");
        String client = (nomClient != null && !nomClient.isBlank()) ? nomClient.trim() : "Client anonyme";
        if (client.length() > 100) client = client.substring(0, 100);
        d.put("nomClient", client);

        LocalDate date = colDate(cols, idx, "datevente");
        d.put("dateVente", date != null ? date.toString() : LocalDate.now().toString());
        d.put(K_DATE_VENTE_PARSED, date != null ? date : LocalDate.now());

        String modePaiement = col(cols, idx, "modepaiement");
        d.put("modePaiement", mappingModePaiement(modePaiement));

        d.put(K_STATUT, STATUT_VALIDE);
        return d;
    }

    // ── DÉPENSE ────────────────────────────────────────────────────────────
    private Map<String, Object> parseDepense(String[] cols, int numLigne, Map<String, Integer> idx) {
        Map<String, Object> d = new LinkedHashMap<>();
        d.put("numeroLigne", numLigne);

        String description = col(cols, idx, "description");
        if (description == null || description.isBlank())
            throw new IllegalArgumentException("[description] Description obligatoire");
        description = description.trim();
        if (description.length() < 3)
            throw new IllegalArgumentException("[description] Description trop courte (3 caractères minimum)");
        if (description.length() > 200)
            throw new IllegalArgumentException("[description] Description trop longue (200 caractères maximum)");
        d.put("description", description);

        Double montant = colDouble(cols, idx, "montant");
        if (montant == null || montant <= 0)
            throw new IllegalArgumentException("[montant] Montant manquant ou invalide (doit être > 0)");
        d.put("montant", montant);

        String categorieStr = col(cols, idx, "categorie");
        d.put(K_CATEGORIE, mappingCategorie(categorieStr).name());

        LocalDate date = colDate(cols, idx, "datedepense");
        d.put("dateDepense", date != null ? date.toString() : LocalDate.now().toString());
        d.put("dateDepenseParsed", date != null ? date : LocalDate.now());

        String notes = col(cols, idx, "notes");
        if (notes != null) {
            notes = notes.trim();
            if (notes.length() > 500) notes = notes.substring(0, 500);
        }
        d.put(K_NOTES, notes);

        d.put(K_STATUT, STATUT_VALIDE);
        return d;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CONSTRUCTION DES ENTITÉS
    // ─────────────────────────────────────────────────────────────────────────
    private Object construireEntite(Map<String, Object> d, String type,
            UserEntity utilisateur, TenantEntity tenant) {
        return switch (type) {
            case "achats"   -> construireAchat(d, utilisateur, tenant);
            case "ventes"   -> construireVente(d, utilisateur, tenant);
            case "depenses" -> construireDepense(d, utilisateur, tenant);
            default         -> null;
        };
    }

    private AchatEntity construireAchat(Map<String, Object> d,
            UserEntity utilisateur, TenantEntity tenant) {
        LocalDate date     = (LocalDate) d.get(K_DATE_ACHAT_PARSED);
        Double prixAchat   = (Double) d.get("prixAchat");
        Double prixVente   = (Double) d.get(K_PRIX_VENTE);
        Double quantite    = (Double) d.get("quantite");
        BigDecimal bdPrixAchat = BigDecimal.valueOf(prixAchat);
        BigDecimal bdQuantite  = BigDecimal.valueOf(quantite);
        return AchatEntity.builder()
                .nomProduit((String) d.get("nomProduit"))
                .quantite(quantite)
                .unite((String) d.get(K_UNITE))
                .prixUnitaire(bdPrixAchat)
                .prixTotal(bdPrixAchat.multiply(bdQuantite))
                .prixVenteSuggere(BigDecimal.valueOf(prixVente))
                .dateAchat(date.atStartOfDay())
                .fournisseur((String) d.get(K_FOURNISSEUR))
                .utilisateur(utilisateur)
                .tenant(tenant)
                .build();
    }

    private VenteEntity construireVente(Map<String, Object> d,
            UserEntity utilisateur, TenantEntity tenant) {
        LocalDate date   = (LocalDate) d.get(K_DATE_VENTE_PARSED);
        Double prixVente = (Double) d.get(K_PRIX_VENTE);
        Double quantite  = (Double) d.get("quantite");
        BigDecimal bdPrixVente = BigDecimal.valueOf(prixVente);
        BigDecimal bdQuantite  = BigDecimal.valueOf(quantite);
        VenteEntity.ModePaiementVente mode =
                (VenteEntity.ModePaiementVente) d.get("modePaiement");
        return VenteEntity.builder()
                .nomProduit((String) d.get("nomProduit"))
                .quantite(quantite)
                .prixUnitaire(bdPrixVente)
                .prixTotal(bdPrixVente.multiply(bdQuantite))
                .client((String) d.get("nomClient"))
                .dateVente(date.atStartOfDay())
                .modePaiement(mode)
                .utilisateur(utilisateur)
                .tenant(tenant)
                .build();
    }

    private DepenseEntity construireDepense(Map<String, Object> d,
            UserEntity utilisateur, TenantEntity tenant) {
        LocalDate date = (LocalDate) d.get("dateDepenseParsed");
        DepenseEntity.CategorieDepense categorie;
        try {
            categorie = DepenseEntity.CategorieDepense.valueOf((String) d.get(K_CATEGORIE));
        } catch (IllegalArgumentException e) {
            categorie = DepenseEntity.CategorieDepense.AUTRE;
        }
        return DepenseEntity.builder()
                .libelle((String) d.get("description"))
                .montant(BigDecimal.valueOf((Double) d.get("montant")))
                .categorie(categorie)
                .dateDepense(date.atStartOfDay())
                .notes((String) d.get(K_NOTES))
                .utilisateur(utilisateur)
                .tenant(tenant)
                .build();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // DÉTECTION DES DOUBLONS — chargement unique avant la boucle
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Charge en UNE seule requête tous les enregistrements existants couvrant
     * la plage de dates du fichier, puis retourne un Set de clés composites.
     * Complexité : O(1) requête SQL quelle que soit la taille du fichier.
     */
    private Set<String> chargerCleesDoublons(List<Map<String, Object>> donneesParsees, String type) {
        if (donneesParsees.isEmpty() || type.equals("depenses")) return new HashSet<>();

        String dateKey = type.equals("achats") ? K_DATE_ACHAT_PARSED : K_DATE_VENTE_PARSED;

        LocalDate minDate = donneesParsees.stream()
                .map(d -> (LocalDate) d.get(dateKey))
                .filter(Objects::nonNull)
                .min(Comparator.naturalOrder())
                .orElse(LocalDate.now());
        LocalDate maxDate = donneesParsees.stream()
                .map(d -> (LocalDate) d.get(dateKey))
                .filter(Objects::nonNull)
                .max(Comparator.naturalOrder())
                .orElse(LocalDate.now());

        return switch (type) {
            case "achats" -> achatRepository
                    .findByDateAchatBetween(minDate.atStartOfDay(), maxDate.atTime(23, 59, 59))
                    .stream()
                    .map(a -> cleAchat(a.getNomProduit(), a.getQuantite(), a.getPrixUnitaire(), a.getDateAchat().toLocalDate()))
                    .collect(Collectors.toCollection(HashSet::new));
            case "ventes" -> venteRepository
                    .findByDateVenteBetween(minDate.atStartOfDay(), maxDate.atTime(23, 59, 59))
                    .stream()
                    .map(v -> cleVente(v.getNomProduit(), v.getQuantite(), v.getPrixUnitaire(), v.getDateVente().toLocalDate()))
                    .collect(Collectors.toCollection(HashSet::new));
            default -> new HashSet<>();
        };
    }

    /** Retourne la clé composite d'un doublon pour une entité déjà construite. */
    private String cleDoublon(Object entite, String type) {
        return switch (type) {
            case "achats" -> {
                AchatEntity a = (AchatEntity) entite;
                yield cleAchat(a.getNomProduit(), a.getQuantite(), a.getPrixUnitaire(), a.getDateAchat().toLocalDate());
            }
            case "ventes" -> {
                VenteEntity v = (VenteEntity) entite;
                yield cleVente(v.getNomProduit(), v.getQuantite(), v.getPrixUnitaire(), v.getDateVente().toLocalDate());
            }
            default -> null;
        };
    }

    private String cleAchat(String nom, Double quantite, BigDecimal prix, LocalDate date) {
        return nom.toLowerCase() + "|" + quantite + "|" + prix.toPlainString() + "|" + date;
    }

    private String cleVente(String nom, Double quantite, BigDecimal prix, LocalDate date) {
        return nom.toLowerCase() + "|" + quantite + "|" + prix.toPlainString() + "|" + date;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // SAUVEGARDE PAR LOTS
    // ─────────────────────────────────────────────────────────────────────────
    @SuppressWarnings("unchecked")
    private int sauvegarderBatch(List<Object> batch, String type) {
        try {
            switch (type) {
                case "achats"   -> achatRepository.saveAll((List<AchatEntity>)(List<?>)batch);
                case "ventes"   -> venteRepository.saveAll((List<VenteEntity>)(List<?>)batch);
                case "depenses" -> depenseRepository.saveAll((List<DepenseEntity>)(List<?>)batch);
            }
            return batch.size();
        } catch (ConstraintViolationException e) {
            log.error("Contrainte violée lors de l'import '{}' : {}", type, e.getMessage());
            throw new IllegalArgumentException(
                    "Données invalides dans le fichier : vérifiez que les quantités et les prix sont bien supérieurs à zéro.");
        } catch (DataIntegrityViolationException e) {
            log.error("Violation d'intégrité lors de l'import '{}' : {}", type, e.getMessage());
            throw new IllegalArgumentException(
                    "Conflit de données lors de l'enregistrement : vérifiez qu'il n'y a pas de doublons dans votre fichier.");
        } catch (Exception e) {
            log.error("Erreur inattendue lors de l'import '{}' : {}", type, e.getMessage(), e);
            throw new IllegalArgumentException(
                    "Erreur lors de l'enregistrement. Vérifiez votre fichier et réessayez.");
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // LECTURE DES COLONNES PAR NOM (CSV ou Excel normalisé)
    // ─────────────────────────────────────────────────────────────────────────
    private String col(String[] cols, Map<String, Integer> idx, String nomNormalise) {
        Integer i = idx.get(nomNormalise);
        if (i == null || i >= cols.length) return null;
        String v = cols[i].trim();
        return v.isBlank() ? null : v;
    }

    private Double colDouble(String[] cols, Map<String, Integer> idx, String nomNormalise) {
        String v = col(cols, idx, nomNormalise);
        if (v == null) return null;
        try {
            return Double.parseDouble(v.replace(",", ".").replace(" ", ""));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private LocalDate colDate(String[] cols, Map<String, Integer> idx, String nomNormalise) {
        String v = col(cols, idx, nomNormalise);
        if (v == null) return null;
        for (DateTimeFormatter fmt : List.of(FMT_FR, FMT_ISO)) {
            try { return LocalDate.parse(v, fmt); }
            catch (DateTimeParseException ignored) {}
        }
        return null;
    }

    private String lireCelluleCommeString(Cell cell) {
        return switch (cell.getCellType()) {
            case STRING  -> cell.getStringCellValue().trim();
            case NUMERIC -> DateUtil.isCellDateFormatted(cell)
                    ? cell.getLocalDateTimeCellValue().toLocalDate().format(FMT_ISO)
                    : formaterNombreExcel(cell.getNumericCellValue());
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            case FORMULA -> {
                try {
                    yield cell.getCachedFormulaResultType() == CellType.NUMERIC
                            ? formaterNombreExcel(cell.getNumericCellValue())
                            : cell.getRichStringCellValue().getString();
                } catch (Exception e) {
                    yield "";
                }
            }
            default -> "";
        };
    }

    /**
     * Formate un nombre Excel en String en conservant les décimales.
     * Si le nombre est entier (ex: 100.0), retourne "100" sans ".0".
     * Sinon retourne la valeur décimale (ex: "12.99").
     */
    private String formaterNombreExcel(double valeur) {
        long entier = (long) valeur;
        return valeur == entier ? String.valueOf(entier) : String.valueOf(valeur);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // UTILITAIRES
    // ─────────────────────────────────────────────────────────────────────────
    private void validerFichier(MultipartFile fichier) {
        if (fichier == null || fichier.isEmpty())
            throw new IllegalArgumentException("Fichier manquant");
        if (fichier.getSize() > MAX_OCTETS)
            throw new IllegalArgumentException("Fichier trop volumineux (max 5 MB)");

        String nom = fichier.getOriginalFilename();
        String ct  = fichier.getContentType();
        boolean formatValide = (nom != null && (nom.endsWith(".csv") || nom.endsWith(".xlsx") || nom.endsWith(".xls")))
                || (ct != null && (ct.contains("csv") || ct.contains("spreadsheet")
                        || ct.contains("excel") || ct.contains("text/plain")));
        if (!formatValide)
            throw new IllegalArgumentException("Format invalide — .csv, .xlsx ou .xls uniquement");
    }

    private boolean estLigneVide(String[] cols) {
        for (String c : cols) if (c != null && !c.isBlank()) return false;
        return true;
    }

    private String extraireColonne(String message) {
        if (message != null && message.startsWith("[") && message.contains("]"))
            return message.substring(1, message.indexOf("]"));
        return null;
    }

    /** Supprime le préfixe technique [colonne] pour afficher un message lisible par le client. */
    /**
     * Cherche un produit dont le nom ressemble à nomProduit parmi les achats du tenant.
     * Retourne une chaîne de suggestion (avec le stock disponible) ou null si rien trouvé.
     * Exemple : "produit similaire trouvé : 'sucre 50 kg' (stock disponible : 45)"
     */
    private String suggererProduit(String nomProduit) {
        try {
            TenantEntity tenant = tenantService.getCurrentTenant();
            String[] mots = nomProduit.toLowerCase().trim().split("\\s+");
            Map<String, Integer> scores = new LinkedHashMap<>();

            for (String mot : mots) {
                if (mot.length() < 3) continue;
                achatRepository.findByNomProduitContainingAndTenant(mot, tenant)
                        .forEach(a -> scores.merge(a.getNomProduit(), 1, Integer::sum));
            }

            if (scores.isEmpty()) return null;

            String meilleur = scores.entrySet().stream()
                    .max(Map.Entry.comparingByValue())
                    .map(Map.Entry::getKey)
                    .orElse(null);

            if (meilleur == null || meilleur.equalsIgnoreCase(nomProduit)) return null;

            try {
                var stock = stockService.obtenirStockParNomProduit(meilleur);
                double dispo = stock.getStockDisponible() != null ? stock.getStockDisponible() : 0.0;
                return String.format("produit similaire trouvé : '%s' (stock disponible : %.0f)", meilleur, dispo);
            } catch (RuntimeException ignored) {
                return String.format("produit similaire trouvé : '%s'", meilleur);
            }
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private String nettoyerMessage(String message) {
        if (message == null) return "Erreur inconnue dans cette ligne.";
        if (message.startsWith("[") && message.contains("]")) {
            int idx = message.indexOf("]");
            return message.substring(idx + 1).stripLeading();
        }
        return message;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // RÉSOLUTION DU PRIX DE VENTE (extraites pour réduire la complexité cognitive)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Pour les ACHATS : prix saisi → stock → marge automatique.
     * Ne lance jamais d'exception : la marge est toujours le dernier recours.
     */
    private Map.Entry<Double, String> resolverPrixAchat(
            String nomProduit, double prixAchat, Double prixVenteSaisi, double margeDefaut) {
        if (prixVenteSaisi != null && prixVenteSaisi > 0) {
            return Map.entry(prixVenteSaisi, "SAISI");
        }
        try {
            var stock = stockService.obtenirStockParNomProduit(nomProduit);
            if (stock.getPrixMoyenVente() != null
                    && stock.getPrixMoyenVente().compareTo(BigDecimal.ZERO) > 0) {
                return Map.entry(stock.getPrixMoyenVente().doubleValue(), "STOCK");
            }
        } catch (RuntimeException ignored) {
            // Produit absent du stock → marge automatique
        }
        return Map.entry(prixAchat * (1.0 + margeDefaut / 100.0), "AUTO");
    }

    /**
     * Pour les VENTES : prix saisi → stock obligatoire.
     * Lance IllegalArgumentException si le produit est introuvable ou sans prix.
     */
    private Map.Entry<Double, String> resolverPrixVente(String nomProduit, Double prixVenteSaisi) {
        if (prixVenteSaisi != null && prixVenteSaisi > 0) {
            return Map.entry(prixVenteSaisi, "SAISI");
        }
        try {
            var stock = stockService.obtenirStockParNomProduit(nomProduit);
            if (stock.getPrixMoyenVente() != null
                    && stock.getPrixMoyenVente().compareTo(BigDecimal.ZERO) > 0) {
                return Map.entry(stock.getPrixMoyenVente().doubleValue(), "STOCK");
            }
            throw new IllegalArgumentException(
                    "[prixVente] Prix de vente manquant pour '" + nomProduit + "' — ajoutez la colonne prixVente");
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new IllegalArgumentException(
                    "[prixVente] Prix de vente manquant pour '" + nomProduit + "' — produit introuvable dans le stock");
        }
    }

    private VenteEntity.ModePaiementVente mappingModePaiement(String valeur) {
        if (valeur == null || valeur.isBlank()) return VenteEntity.ModePaiementVente.ESPECES;
        return switch (valeur.trim().toUpperCase()) {
            case "ESPECES", "CASH", "LIQUIDE", "ESPECE" -> VenteEntity.ModePaiementVente.ESPECES;
            case "WAVE"                                  -> VenteEntity.ModePaiementVente.WAVE;
            case "ORANGE", "ORANGE_MONEY", "OM"          -> VenteEntity.ModePaiementVente.ORANGE_MONEY;
            case "CREDIT", "CRÉDIT"                      -> VenteEntity.ModePaiementVente.CREDIT;
            default                                      -> VenteEntity.ModePaiementVente.ESPECES;
        };
    }

    private DepenseEntity.CategorieDepense mappingCategorie(String valeur) {
        if (valeur == null || valeur.isBlank()) return DepenseEntity.CategorieDepense.AUTRE;
        try {
            return DepenseEntity.CategorieDepense.valueOf(valeur.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            for (DepenseEntity.CategorieDepense cat : DepenseEntity.CategorieDepense.values()) {
                if (cat.getLibelle().equalsIgnoreCase(valeur.trim())) return cat;
            }
            return DepenseEntity.CategorieDepense.AUTRE;
        }
    }
}
