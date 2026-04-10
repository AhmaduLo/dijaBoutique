package com.example.dijasaliou.service;

import com.example.dijasaliou.dto.StockDto;
import com.example.dijasaliou.entity.*;
import com.example.dijasaliou.entity.CreditClientEntity.StatutCredit;
import com.example.dijasaliou.repository.*;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.ColumnText;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfPageEventHelper;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class RapportService {

    private final VenteService venteService;
    private final AchatRepository achatRepository;
    private final DepenseRepository depenseRepository;
    private final StockService stockService;
    private final CreditClientRepository creditClientRepository;
    private final BonLivraisonRepository bonLivraisonRepository;
    private final TenantService tenantService;
    private final DeviseService deviseService;

    /** Stocke le symbole de la devise de rapport pour la requête en cours (thread-safe). */
    private final ThreadLocal<String> symboleRapport = ThreadLocal.withInitial(() -> "FCFA");
    /** Stocke le taux de la devise de rapport (1 unité = X XOF). */
    private final ThreadLocal<Double> tauxRapport = ThreadLocal.withInitial(() -> 1.0);

    // ── Couleurs ──────────────────────────────────────────────────────────────
    private static final Color C_PRIMAIRE   = new Color(30, 58, 95);
    private static final Color C_SECONDAIRE = new Color(41, 128, 185);
    private static final Color C_HEADER_TAB = new Color(52, 73, 94);
    private static final Color C_LIGNE_PAIR = new Color(236, 240, 241);
    private static final Color C_VERT       = new Color(39, 174, 96);
    private static final Color C_ROUGE      = new Color(192, 57, 43);
    private static final Color C_GRIS       = new Color(127, 140, 141);
    private static final Color C_ORANGE     = new Color(230, 126, 34);

    // ── Polices ───────────────────────────────────────────────────────────────
    private static final Font F_SECTION     = new Font(Font.HELVETICA, 11, Font.BOLD,   Color.WHITE);
    private static final Font F_CORPS       = new Font(Font.HELVETICA, 9,  Font.NORMAL, Color.BLACK);
    private static final Font F_CORPS_B     = new Font(Font.HELVETICA, 9,  Font.BOLD,   Color.BLACK);
    private static final Font F_SMALL       = new Font(Font.HELVETICA, 8,  Font.NORMAL, C_GRIS);
    private static final Font F_GARDE_TITRE = new Font(Font.HELVETICA, 26, Font.BOLD,   Color.WHITE);
    private static final Font F_GARDE_SOUS  = new Font(Font.HELVETICA, 13, Font.NORMAL, new Color(200, 220, 240));

    private static final DateTimeFormatter FMT_DATE     = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter FMT_DATETIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    // ─────────────────────────────────────────────────────────────────────────
    // POINT D'ENTRÉE
    // ─────────────────────────────────────────────────────────────────────────
    /**
     * @param devise Code devise pour le rapport (ex: "EUR", "XOF"). Si null,
     *               utilise la devise préférée du tenant.
     */
    public byte[] genererRapportPdf(LocalDate debut, LocalDate fin, String devise) {

        TenantEntity tenant    = tenantService.getCurrentTenant();
        String tenantUuid      = tenant.getTenantUuid();
        LocalDateTime debutDt  = debut.atStartOfDay();
        LocalDateTime finDt    = fin.atTime(23, 59, 59);

        // ── Devise de rapport : paramètre explicite, sinon préférence du tenant ──
        String codeDeviseRapport;
        if (devise != null && !devise.isBlank()) {
            codeDeviseRapport = devise.toUpperCase().trim();
        } else {
            codeDeviseRapport = (tenant.getDevisePreferee() != null) ? tenant.getDevisePreferee() : "XOF";
        }
        try {
            DeviseEntity deviseRapport = deviseService.obtenirDeviseParCode(codeDeviseRapport);
            symboleRapport.set(deviseRapport.getSymbole());
            tauxRapport.set(deviseRapport.getTauxChange());
        } catch (RuntimeException e) {
            symboleRapport.set("FCFA");
            tauxRapport.set(1.0);
        }

        // ── Collecte des données ──────────────────────────────────────────────
        List<VenteEntity>   ventes   = venteService.obtenirVentesParPeriode(debut, fin);
        List<AchatEntity>   achats   = achatRepository.findByDateAchatBetween(debutDt, finDt);
        List<DepenseEntity> depenses = depenseRepository.findByDateDepenseBetween(debutDt, finDt);
        List<StockDto>      stocks   = stockService.obtenirTousLesStocks(codeDeviseRapport);

        // sumMontantRestantActif/sumMontantInitialActif retournent des montants en XOF (montant × tauxChangeApplique)
        // On divise par le taux du rapport pour obtenir la devise demandée
        double tauxRapportVal = tauxRapport.get() > 0 ? tauxRapport.get() : 1.0;
        BigDecimal montantDuCredits = creditClientRepository.sumMontantRestantActif(StatutCredit.SOLDE, tenantUuid)
                .divide(BigDecimal.valueOf(tauxRapportVal), 2, RoundingMode.HALF_UP);
        BigDecimal montantInitialCredits = creditClientRepository.sumMontantInitialActif(StatutCredit.SOLDE, tenantUuid)
                .divide(BigDecimal.valueOf(tauxRapportVal), 2, RoundingMode.HALF_UP);
        long nbCreditsActifs  = creditClientRepository.countCreditsActifs(StatutCredit.SOLDE, tenantUuid);
        long nbCreditsRetard  = creditClientRepository.countCreditsEnRetard(StatutCredit.SOLDE, LocalDate.now(), tenantUuid);

        List<BonLivraisonEntity> bls = bonLivraisonRepository
                .findAllWithSearch(null, null, debutDt, finDt, PageRequest.of(0, 1000))
                .getContent();

        // ── Agrégats (convertis vers la devise de rapport) ────────────────────
        BigDecimal ca            = somme(ventes.stream().map(v -> convertirMontant(v.getPrixTotal(), v.getTauxChangeApplique())).collect(Collectors.toList()));
        BigDecimal totalAchats   = somme(achats.stream().map(a -> convertirMontant(a.getPrixTotal(), a.getTauxChangeApplique())).collect(Collectors.toList()));
        BigDecimal totalDepenses = somme(depenses.stream().map(d -> convertirMontant(d.getMontant(), d.getTauxChangeApplique())).collect(Collectors.toList()));
        BigDecimal benefice      = ca.subtract(totalAchats).subtract(totalDepenses);

        // ── Période précédente ────────────────────────────────────────────────
        long      nbJours     = ChronoUnit.DAYS.between(debut, fin) + 1;
        LocalDate debutPrev   = debut.minusDays(nbJours);
        LocalDate finPrev     = debut.minusDays(1);
        List<VenteEntity>   ventesPrev   = venteService.obtenirVentesParPeriode(debutPrev, finPrev);
        List<AchatEntity>   achatsPrev   = achatRepository.findByDateAchatBetween(debutPrev.atStartOfDay(), finPrev.atTime(23, 59, 59));
        List<DepenseEntity> depensesPrev = depenseRepository.findByDateDepenseBetween(debutPrev.atStartOfDay(), finPrev.atTime(23, 59, 59));
        BigDecimal caPrev            = somme(ventesPrev.stream().map(v -> convertirMontant(v.getPrixTotal(), v.getTauxChangeApplique())).collect(Collectors.toList()));
        BigDecimal totalAchatsPrev   = somme(achatsPrev.stream().map(a -> convertirMontant(a.getPrixTotal(), a.getTauxChangeApplique())).collect(Collectors.toList()));
        BigDecimal totalDepensesPrev = somme(depensesPrev.stream().map(d -> convertirMontant(d.getMontant(), d.getTauxChangeApplique())).collect(Collectors.toList()));
        BigDecimal beneficePrev      = caPrev.subtract(totalAchatsPrev).subtract(totalDepensesPrev);

        // ── Numéro de rapport ─────────────────────────────────────────────────
        String numRapport = String.format("RPT-%d%02d-%04d",
                debut.getYear(), debut.getMonthValue(),
                (int)(System.currentTimeMillis() % 9000) + 1000);

        // ── Génération PDF ────────────────────────────────────────────────────
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Document doc = new Document(PageSize.A4, 40, 40, 70, 50);

        try {
            PdfWriter writer = PdfWriter.getInstance(doc, baos);
            writer.setPageEvent(new PiedDePageEvent(tenant.getNomEntreprise(), numRapport));
            doc.open();

            ajouterPageGarde(doc, tenant, debut, fin, numRapport);
            doc.newPage();

            titreSection(doc, "1. RÉSUMÉ EXÉCUTIF", C_PRIMAIRE);
            ajouterResumeExecutif(doc, ca, totalAchats, totalDepenses, benefice,
                    ventes.size(), achats.size(), depenses.size());
            espace(doc);

            titreSection(doc, "2. TOP 5 PRODUITS LES PLUS VENDUS", C_PRIMAIRE);
            ajouterTop5ProduitsVendus(doc, ventes, ca);
            espace(doc);

            titreSection(doc, "3. RÉPARTITION PAR MODE DE PAIEMENT", C_PRIMAIRE);
            ajouterRepartitionModesPaiement(doc, ventes, ca);
            espace(doc);

            titreSection(doc, "4. ÉTAT DU STOCK", C_PRIMAIRE);
            ajouterEtatStock(doc, stocks);
            espace(doc);

            titreSection(doc, "5. CRÉDITS CLIENTS", C_PRIMAIRE);
            ajouterCreditsClients(doc, montantDuCredits, montantInitialCredits, nbCreditsActifs, nbCreditsRetard);
            espace(doc);

            titreSection(doc, "6. INDICATEURS OPÉRATIONNELS", C_PRIMAIRE);
            ajouterIndicateursOperationnels(doc, ventes, achats, depenses, benefice, ca);
            espace(doc);

            titreSection(doc, "7. COMPARAISON AVEC LA PÉRIODE PRÉCÉDENTE", C_PRIMAIRE);
            ajouterComparaisonPeriode(doc, ca, caPrev, totalAchats, totalAchatsPrev,
                    totalDepenses, totalDepensesPrev, benefice, beneficePrev,
                    ventes.size(), ventesPrev.size(), debutPrev, finPrev);
            espace(doc);

            titreSection(doc, "8. TOP 5 PRODUITS LES PLUS ACHETÉS", C_PRIMAIRE);
            ajouterTop5ProduitsAchetes(doc, achats, totalAchats);
            espace(doc);

            titreSection(doc, "9. DÉPENSES PAR CATÉGORIE", C_PRIMAIRE);
            ajouterDepensesParCategorie(doc, depenses, totalDepenses);
            espace(doc);

            titreSection(doc, "10. BONS DE LIVRAISON", C_PRIMAIRE);
            ajouterBonsLivraison(doc, bls);
            espace(doc);

            titreSection(doc, "11. RELEVÉ CHRONOLOGIQUE DES VENTES", C_PRIMAIRE);
            ajouterReleve(doc, ventes);
            espace(doc);

            doc.newPage();
            titreSection(doc, "12. CERTIFICATION ET SIGNATURE", C_PRIMAIRE);
            ajouterCertification(doc, tenant, debut, fin, numRapport);

            doc.close();
        } catch (DocumentException e) {
            throw new RuntimeException("Erreur lors de la génération du rapport PDF", e);
        }

        return baos.toByteArray();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PAGE DE GARDE
    // ─────────────────────────────────────────────────────────────────────────
    private void ajouterPageGarde(Document doc, TenantEntity tenant,
            LocalDate debut, LocalDate fin, String numRapport) throws DocumentException {

        PdfPTable table = new PdfPTable(1);
        table.setWidthPercentage(100);

        // Bloc supérieur coloré
        PdfPCell cellHaut = new PdfPCell();
        cellHaut.setBackgroundColor(C_PRIMAIRE);
        cellHaut.setBorder(Rectangle.NO_BORDER);
        cellHaut.setPadding(50);
        cellHaut.setFixedHeight(380);

        Paragraph pApp = new Paragraph("HeasyStock",
                new Font(Font.HELVETICA, 11, Font.BOLD, new Color(150, 200, 240)));
        pApp.setSpacingAfter(20);
        cellHaut.addElement(pApp);

        Paragraph pTitre = new Paragraph("RAPPORT D'ACTIVITÉ", F_GARDE_TITRE);
        pTitre.setSpacingAfter(8);
        cellHaut.addElement(pTitre);

        Paragraph pNom = new Paragraph(tenant.getNomEntreprise().toUpperCase(), F_GARDE_SOUS);
        pNom.setSpacingAfter(35);
        cellHaut.addElement(pNom);

        Paragraph pPeriode = new Paragraph(
                "Période : " + debut.format(FMT_DATE) + " — " + fin.format(FMT_DATE),
                new Font(Font.HELVETICA, 13, Font.BOLD, new Color(200, 220, 240)));
        pPeriode.setSpacingAfter(10);
        cellHaut.addElement(pPeriode);

        Paragraph pNum = new Paragraph("N° " + numRapport,
                new Font(Font.HELVETICA, 10, Font.NORMAL, new Color(150, 180, 220)));
        cellHaut.addElement(pNum);

        table.addCell(cellHaut);

        // Bloc inférieur blanc
        PdfPCell cellBas = new PdfPCell();
        cellBas.setBorder(Rectangle.NO_BORDER);
        cellBas.setPadding(30);
        cellBas.setFixedHeight(130);

        Paragraph pGenere = new Paragraph(
                "Généré le " + LocalDateTime.now().format(FMT_DATETIME), F_SMALL);
        pGenere.setSpacingAfter(8);
        cellBas.addElement(pGenere);

        cellBas.addElement(new Paragraph(
                "Document confidentiel — Usage interne uniquement",
                new Font(Font.HELVETICA, 9, Font.ITALIC, C_GRIS)));
        table.addCell(cellBas);

        doc.add(table);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // RÉSUMÉ EXÉCUTIF
    // ─────────────────────────────────────────────────────────────────────────
    private void ajouterResumeExecutif(Document doc, BigDecimal ca, BigDecimal totalAchats,
            BigDecimal totalDepenses, BigDecimal benefice,
            int nbVentes, int nbAchats, int nbDepenses) throws DocumentException {

        PdfPTable t1 = new PdfPTable(4);
        t1.setWidthPercentage(100);
        t1.setWidths(new float[]{25, 25, 25, 25});
        cellKpi(t1, "Chiffre d'Affaires",  formaterMontant(ca),            C_SECONDAIRE);
        cellKpi(t1, "Total Achats",        formaterMontant(totalAchats),   C_ORANGE);
        cellKpi(t1, "Total Dépenses",      formaterMontant(totalDepenses), C_ORANGE);
        cellKpi(t1, "Bénéfice Net",        formaterMontant(benefice),
                benefice.compareTo(BigDecimal.ZERO) >= 0 ? C_VERT : C_ROUGE);
        doc.add(t1);

        PdfPTable t2 = new PdfPTable(3);
        t2.setWidthPercentage(100);
        t2.setSpacingBefore(6);
        cellKpi(t2, "Nombre de Ventes",   String.valueOf(nbVentes),   C_PRIMAIRE);
        cellKpi(t2, "Nombre d'Achats",    String.valueOf(nbAchats),   C_PRIMAIRE);
        cellKpi(t2, "Nombre de Dépenses", String.valueOf(nbDepenses), C_PRIMAIRE);
        doc.add(t2);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // TOP 5 PRODUITS VENDUS
    // ─────────────────────────────────────────────────────────────────────────
    private void ajouterTop5ProduitsVendus(Document doc, List<VenteEntity> ventes, BigDecimal caTotal)
            throws DocumentException {

        Map<String, BigDecimal[]> stats = new LinkedHashMap<>();
        for (VenteEntity v : ventes) {
            stats.computeIfAbsent(v.getNomProduit(), k -> new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});
            stats.get(v.getNomProduit())[0] = stats.get(v.getNomProduit())[0].add(BigDecimal.valueOf(v.getQuantite()));
            stats.get(v.getNomProduit())[1] = stats.get(v.getNomProduit())[1]
                    .add(convertirMontant(v.getPrixTotal(), v.getTauxChangeApplique()));
        }

        List<Map.Entry<String, BigDecimal[]>> top5 = stats.entrySet().stream()
                .sorted((a, b) -> b.getValue()[1].compareTo(a.getValue()[1]))
                .limit(5).collect(Collectors.toList());

        PdfPTable table = tableau(new float[]{8, 42, 18, 22, 15},
                "Rang", "Produit", "Qté vendue", "CA généré", "% CA");

        for (int i = 0; i < top5.size(); i++) {
            Map.Entry<String, BigDecimal[]> e = top5.get(i);
            BigDecimal pct = pourcent(e.getValue()[1], caTotal);
            ligne(table, i % 2 == 0, "#" + (i + 1), e.getKey(),
                    fmt0(e.getValue()[0]), formaterMontant(e.getValue()[1]), pct + "%");
        }
        if (top5.isEmpty()) ligneVide(table, 5, "Aucune vente sur cette période");
        doc.add(table);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // RÉPARTITION MODES PAIEMENT
    // ─────────────────────────────────────────────────────────────────────────
    private void ajouterRepartitionModesPaiement(Document doc, List<VenteEntity> ventes, BigDecimal caTotal)
            throws DocumentException {

        Map<String, BigDecimal> totaux = new LinkedHashMap<>();
        totaux.put("ESPECES", BigDecimal.ZERO);
        totaux.put("WAVE", BigDecimal.ZERO);
        totaux.put("ORANGE_MONEY", BigDecimal.ZERO);
        totaux.put("CREDIT", BigDecimal.ZERO);

        for (VenteEntity v : ventes) {
            String mode = v.getModePaiement() != null ? v.getModePaiement().name() : "ESPECES";
            totaux.merge(mode, convertirMontant(v.getPrixTotal(), v.getTauxChangeApplique()), BigDecimal::add);
        }

        PdfPTable table = tableau(new float[]{40, 35, 25}, "Mode de paiement", "Montant", "Pourcentage");

        int i = 0;
        for (Map.Entry<String, BigDecimal> e : totaux.entrySet()) {
            BigDecimal pct = pourcent(e.getValue(), caTotal);
            ligne(table, i++ % 2 == 0, libellePaiement(e.getKey()), formaterMontant(e.getValue()), pct + "%");
        }
        doc.add(table);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ÉTAT DU STOCK
    // ─────────────────────────────────────────────────────────────────────────
    private void ajouterEtatStock(Document doc, List<StockDto> stocks) throws DocumentException {

        long nbEnStock  = stocks.stream().filter(s -> s.getStockDisponible() != null && s.getStockDisponible() > 0).count();
        long nbRupture  = stocks.stream().filter(s -> s.getStockDisponible() == null || s.getStockDisponible() <= 0).count();
        BigDecimal valTotale = stocks.stream()
                .map(StockDto::getValeurStock).filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);

        PdfPTable resume = new PdfPTable(3);
        resume.setWidthPercentage(100);
        resume.setSpacingAfter(8);
        cellKpi(resume, "Produits en stock",    String.valueOf(nbEnStock),           C_VERT);
        cellKpi(resume, "Ruptures de stock",    String.valueOf(nbRupture),           nbRupture > 0 ? C_ROUGE : C_VERT);
        cellKpi(resume, "Valeur totale stock",  formaterMontant(valTotale),          C_PRIMAIRE);
        doc.add(resume);

        PdfPTable table = tableau(new float[]{35, 15, 15, 25, 15},
                "Produit", "Stock dispo", "Unité", "Valeur stock", "Statut");

        List<StockDto> sorted = stocks.stream()
                .sorted(Comparator.comparingDouble(s -> s.getStockDisponible() != null ? s.getStockDisponible() : 0.0))
                .limit(20).collect(Collectors.toList());

        for (int i = 0; i < sorted.size(); i++) {
            StockDto s = sorted.get(i);
            ligne(table, i % 2 == 0,
                    s.getNomProduit(),
                    fmt0(s.getStockDisponible() != null ? BigDecimal.valueOf(s.getStockDisponible()) : BigDecimal.ZERO),
                    s.getUnite() != null ? s.getUnite() : "pièce",
                    formaterMontant(s.getValeurStock()),
                    s.getStatut() != null ? s.getStatut().getLibelle() : "-");
        }
        if (stocks.isEmpty()) ligneVide(table, 5, "Aucun produit en stock");
        doc.add(table);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CRÉDITS CLIENTS
    // ─────────────────────────────────────────────────────────────────────────
    private void ajouterCreditsClients(Document doc, BigDecimal montantDu, BigDecimal montantInitial,
            long nbActifs, long nbRetard) throws DocumentException {

        if (montantDu == null)      montantDu = BigDecimal.ZERO;
        if (montantInitial == null) montantInitial = BigDecimal.ZERO;

        BigDecimal recouvre = montantInitial.subtract(montantDu);
        BigDecimal taux = montantInitial.compareTo(BigDecimal.ZERO) > 0
                ? recouvre.multiply(new BigDecimal("100")).divide(montantInitial, 1, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        PdfPTable table = new PdfPTable(4);
        table.setWidthPercentage(100);
        cellKpi(table, "Montant total dû",      formaterMontant(montantDu),  C_ROUGE);
        cellKpi(table, "Montant recouvré",      formaterMontant(recouvre),   C_VERT);
        cellKpi(table, "Taux de recouvrement",  taux + "%",                  C_SECONDAIRE);
        cellKpi(table, "Crédits en retard",     String.valueOf(nbRetard),    nbRetard > 0 ? C_ROUGE : C_VERT);
        doc.add(table);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // INDICATEURS OPÉRATIONNELS
    // ─────────────────────────────────────────────────────────────────────────
    private void ajouterIndicateursOperationnels(Document doc, List<VenteEntity> ventes,
            List<AchatEntity> achats, List<DepenseEntity> depenses,
            BigDecimal benefice, BigDecimal ca) throws DocumentException {

        BigDecimal panierMoyen = ventes.isEmpty() ? BigDecimal.ZERO
                : ca.divide(BigDecimal.valueOf(ventes.size()), 0, RoundingMode.HALF_UP);
        BigDecimal totalAchats   = somme(achats.stream().map(a -> convertirMontant(a.getPrixTotal(), a.getTauxChangeApplique())).collect(Collectors.toList()));
        BigDecimal margeBrute    = ca.subtract(totalAchats);
        BigDecimal tauxMarge     = ca.compareTo(BigDecimal.ZERO) > 0
                ? margeBrute.multiply(new BigDecimal("100")).divide(ca, 1, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        PdfPTable table = tableau(new float[]{55, 45}, "Indicateur", "Valeur");
        String[][] lignes = {
            {"Nombre de ventes",            String.valueOf(ventes.size())},
            {"Panier moyen par vente",      formaterMontant(panierMoyen)},
            {"Nombre d'achats enregistrés", String.valueOf(achats.size())},
            {"Nombre de dépenses",          String.valueOf(depenses.size())},
            {"Marge brute (CA – Achats)",   formaterMontant(margeBrute)},
            {"Taux de marge brute",         tauxMarge + "%"},
            {"Bénéfice net",                formaterMontant(benefice)},
        };
        for (int i = 0; i < lignes.length; i++) {
            ligne(table, i % 2 == 0, lignes[i][0], lignes[i][1]);
        }
        doc.add(table);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // COMPARAISON PÉRIODE PRÉCÉDENTE
    // ─────────────────────────────────────────────────────────────────────────
    private void ajouterComparaisonPeriode(Document doc,
            BigDecimal ca, BigDecimal caPrev,
            BigDecimal achats, BigDecimal achatsPrev,
            BigDecimal depenses, BigDecimal depensesPrev,
            BigDecimal benefice, BigDecimal beneficePrev,
            int nbVentes, int nbVentesPrev,
            LocalDate debutPrev, LocalDate finPrev) throws DocumentException {

        Paragraph pPrev = new Paragraph(
                "Période précédente : " + debutPrev.format(FMT_DATE) + " — " + finPrev.format(FMT_DATE), F_SMALL);
        pPrev.setSpacingAfter(5);
        doc.add(pPrev);

        PdfPTable table = tableau(new float[]{35, 25, 25, 20},
                "Indicateur", "Période actuelle", "Période précédente", "Variation");

        Object[][] data = {
            {"Chiffre d'affaires", ca,       caPrev,       false},
            {"Total achats",       achats,   achatsPrev,   false},
            {"Total dépenses",     depenses, depensesPrev, false},
            {"Bénéfice net",       benefice, beneficePrev, false},
        };

        for (int i = 0; i < data.length; i++) {
            BigDecimal cur  = (BigDecimal) data[i][1];
            BigDecimal prev = (BigDecimal) data[i][2];
            String variation = variation(prev, cur);
            boolean positif = variation.startsWith("+");
            Color cVar = positif ? C_VERT : variation.startsWith("-") ? C_ROUGE : C_GRIS;
            Color bg = i % 2 == 0 ? C_LIGNE_PAIR : Color.WHITE;

            table.addCell(cellulaCorps((String) data[i][0], bg));
            table.addCell(cellulaCorps(formaterMontant(cur), bg));
            table.addCell(cellulaCorps(formaterMontant(prev), bg));
            PdfPCell cVar_ = new PdfPCell(new Phrase(variation, new Font(Font.HELVETICA, 9, Font.BOLD, cVar)));
            cVar_.setBackgroundColor(bg);
            cVar_.setPadding(5);
            cVar_.setHorizontalAlignment(Element.ALIGN_CENTER);
            cVar_.setBorderWidth(0.5f);
            cVar_.setBorderColor(new Color(220, 220, 220));
            table.addCell(cVar_);
        }

        // Ligne nb ventes
        String varV = variation(BigDecimal.valueOf(nbVentesPrev), BigDecimal.valueOf(nbVentes));
        Color cVarV = varV.startsWith("+") ? C_VERT : varV.startsWith("-") ? C_ROUGE : C_GRIS;
        table.addCell(cellulaCorps("Nombre de ventes", C_LIGNE_PAIR));
        table.addCell(cellulaCorps(String.valueOf(nbVentes), C_LIGNE_PAIR));
        table.addCell(cellulaCorps(String.valueOf(nbVentesPrev), C_LIGNE_PAIR));
        PdfPCell cVarVCell = new PdfPCell(new Phrase(varV, new Font(Font.HELVETICA, 9, Font.BOLD, cVarV)));
        cVarVCell.setBackgroundColor(C_LIGNE_PAIR);
        cVarVCell.setPadding(5);
        cVarVCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cVarVCell.setBorderWidth(0.5f);
        cVarVCell.setBorderColor(new Color(220, 220, 220));
        table.addCell(cVarVCell);

        doc.add(table);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // TOP 5 PRODUITS ACHETÉS
    // ─────────────────────────────────────────────────────────────────────────
    private void ajouterTop5ProduitsAchetes(Document doc, List<AchatEntity> achats, BigDecimal totalAchats)
            throws DocumentException {

        Map<String, BigDecimal[]> stats = new LinkedHashMap<>();
        for (AchatEntity a : achats) {
            stats.computeIfAbsent(a.getNomProduit(), k -> new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});
            stats.get(a.getNomProduit())[0] = stats.get(a.getNomProduit())[0].add(BigDecimal.valueOf(a.getQuantite()));
            stats.get(a.getNomProduit())[1] = stats.get(a.getNomProduit())[1]
                    .add(convertirMontant(a.getPrixTotal(), a.getTauxChangeApplique()));
        }

        List<Map.Entry<String, BigDecimal[]>> top5 = stats.entrySet().stream()
                .sorted((a, b) -> b.getValue()[1].compareTo(a.getValue()[1]))
                .limit(5).collect(Collectors.toList());

        PdfPTable table = tableau(new float[]{8, 42, 18, 22, 15},
                "Rang", "Produit", "Qté achetée", "Montant total", "% achats");

        for (int i = 0; i < top5.size(); i++) {
            Map.Entry<String, BigDecimal[]> e = top5.get(i);
            BigDecimal pct = pourcent(e.getValue()[1], totalAchats);
            ligne(table, i % 2 == 0, "#" + (i + 1), e.getKey(),
                    fmt0(e.getValue()[0]), formaterMontant(e.getValue()[1]), pct + "%");
        }
        if (top5.isEmpty()) ligneVide(table, 5, "Aucun achat sur cette période");
        doc.add(table);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // DÉPENSES PAR CATÉGORIE
    // ─────────────────────────────────────────────────────────────────────────
    private void ajouterDepensesParCategorie(Document doc, List<DepenseEntity> depenses,
            BigDecimal totalDepenses) throws DocumentException {

        Map<String, BigDecimal> parCat = new LinkedHashMap<>();
        for (DepenseEntity d : depenses) {
            String cat = d.getCategorie() != null ? d.getCategorie().getLibelle() : "Autre";
            parCat.merge(cat, convertirMontant(d.getMontant(), d.getTauxChangeApplique()), BigDecimal::add);
        }

        List<Map.Entry<String, BigDecimal>> sorted = parCat.entrySet().stream()
                .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
                .collect(Collectors.toList());

        PdfPTable table = tableau(new float[]{50, 30, 20}, "Catégorie", "Montant", "Pourcentage");

        for (int i = 0; i < sorted.size(); i++) {
            Map.Entry<String, BigDecimal> e = sorted.get(i);
            BigDecimal pct = pourcent(e.getValue(), totalDepenses);
            ligne(table, i % 2 == 0, e.getKey(), formaterMontant(e.getValue()), pct + "%");
        }
        if (sorted.isEmpty()) ligneVide(table, 3, "Aucune dépense sur cette période");
        doc.add(table);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // BONS DE LIVRAISON
    // ─────────────────────────────────────────────────────────────────────────
    private void ajouterBonsLivraison(Document doc, List<BonLivraisonEntity> bls) throws DocumentException {

        long nbLivres  = bls.stream().filter(b -> b.getStatut() == BonLivraisonEntity.Statut.LIVRE).count();
        long nbAttente = bls.stream().filter(b -> b.getStatut() == BonLivraisonEntity.Statut.EN_ATTENTE
                || b.getStatut() == BonLivraisonEntity.Statut.EN_COURS).count();
        long nbAnnules = bls.stream().filter(b -> b.getStatut() == BonLivraisonEntity.Statut.ANNULE).count();

        PdfPTable resume = new PdfPTable(3);
        resume.setWidthPercentage(100);
        resume.setSpacingAfter(8);
        cellKpi(resume, "Livrés",     String.valueOf(nbLivres),  C_VERT);
        cellKpi(resume, "En attente", String.valueOf(nbAttente), C_ORANGE);
        cellKpi(resume, "Annulés",    String.valueOf(nbAnnules), C_ROUGE);
        doc.add(resume);

        if (!bls.isEmpty()) {
            PdfPTable table = tableau(new float[]{20, 35, 25, 20}, "N° BL", "Client", "Date", "Statut");
            List<BonLivraisonEntity> limites = bls.stream().limit(15).collect(Collectors.toList());
            for (int i = 0; i < limites.size(); i++) {
                BonLivraisonEntity bl = limites.get(i);
                String dateStr = bl.getCreatedDate() != null ? bl.getCreatedDate().format(FMT_DATE) : "-";
                ligne(table, i % 2 == 0, bl.getNumeroBL(), bl.getClientNom(), dateStr, libelleStatutBL(bl.getStatut()));
            }
            doc.add(table);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // RELEVÉ CHRONOLOGIQUE
    // ─────────────────────────────────────────────────────────────────────────
    private void ajouterReleve(Document doc, List<VenteEntity> ventes) throws DocumentException {
        Paragraph pInfo = new Paragraph("(Limité aux 30 dernières ventes)", F_SMALL);
        pInfo.setSpacingAfter(4);
        doc.add(pInfo);

        PdfPTable table = tableau(new float[]{22, 30, 10, 23, 20},
                "Date", "Produit", "Qté", "Montant", "Mode paiement");

        List<VenteEntity> dernieres = ventes.stream()
                .filter(v -> v.getDateVente() != null)
                .sorted(Comparator.comparing(VenteEntity::getDateVente).reversed())
                .limit(30).collect(Collectors.toList());

        for (int i = 0; i < dernieres.size(); i++) {
            VenteEntity v = dernieres.get(i);
            ligne(table, i % 2 == 0,
                    v.getDateVente().format(FMT_DATETIME),
                    v.getNomProduit(),
                    fmt0(BigDecimal.valueOf(v.getQuantite())),
                    formaterMontant(convertirMontant(v.getPrixTotal(), v.getTauxChangeApplique())),
                    libellePaiement(v.getModePaiement() != null ? v.getModePaiement().name() : "ESPECES"));
        }
        if (ventes.isEmpty()) ligneVide(table, 5, "Aucune vente sur cette période");
        doc.add(table);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CERTIFICATION
    // ─────────────────────────────────────────────────────────────────────────
    private void ajouterCertification(Document doc, TenantEntity tenant,
            LocalDate debut, LocalDate fin, String numRapport) throws DocumentException {

        Paragraph p1 = new Paragraph(
                "Je soussigné(e), gérant(e) de l'entreprise " + tenant.getNomEntreprise() +
                ", certifie que les informations contenues dans ce rapport (N° " + numRapport + ") " +
                "reflètent fidèlement l'activité de la période du " + debut.format(FMT_DATE) +
                " au " + fin.format(FMT_DATE) + ".", F_CORPS);
        p1.setSpacingBefore(15);
        p1.setSpacingAfter(20);
        doc.add(p1);

        Paragraph pDate = new Paragraph(
                "Fait le " + LocalDate.now().format(FMT_DATE) + ", à ________________", F_CORPS);
        pDate.setSpacingAfter(50);
        doc.add(pDate);

        PdfPTable tableSig = new PdfPTable(2);
        tableSig.setWidthPercentage(100);

        PdfPCell cs1 = new PdfPCell();
        cs1.setBorder(Rectangle.NO_BORDER);
        cs1.setPadding(10);
        Paragraph ps1 = new Paragraph("Signature du gérant :", F_CORPS_B);
        ps1.setSpacingAfter(50);
        cs1.addElement(ps1);
        cs1.addElement(new Paragraph("_______________________________", F_CORPS));
        tableSig.addCell(cs1);

        PdfPCell cs2 = new PdfPCell();
        cs2.setBorder(Rectangle.NO_BORDER);
        cs2.setPadding(10);
        Paragraph ps2 = new Paragraph("Cachet de l'entreprise :", F_CORPS_B);
        ps2.setSpacingAfter(50);
        cs2.addElement(ps2);
        cs2.addElement(new Paragraph("[                                              ]", F_CORPS));
        tableSig.addCell(cs2);
        doc.add(tableSig);

        Paragraph pMention = new Paragraph(
                "\nDocument généré automatiquement par HeasyStock. " +
                "Rapport strictement confidentiel, à usage interne uniquement.",
                new Font(Font.HELVETICA, 8, Font.ITALIC, C_GRIS));
        pMention.setAlignment(Element.ALIGN_CENTER);
        doc.add(pMention);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // UTILITAIRES PDF
    // ─────────────────────────────────────────────────────────────────────────
    private void titreSection(Document doc, String titre, Color couleur) throws DocumentException {
        PdfPTable t = new PdfPTable(1);
        t.setWidthPercentage(100);
        t.setSpacingBefore(10);
        t.setSpacingAfter(6);
        PdfPCell c = new PdfPCell(new Phrase(titre, F_SECTION));
        c.setBackgroundColor(couleur);
        c.setPadding(7);
        c.setBorder(Rectangle.NO_BORDER);
        t.addCell(c);
        doc.add(t);
    }

    private void cellKpi(PdfPTable table, String label, String valeur, Color couleur) {
        PdfPCell cell = new PdfPCell();
        cell.setBorderColor(new Color(200, 210, 220));
        cell.setBorderWidth(1);
        cell.setPadding(10);
        cell.setBackgroundColor(new Color(248, 250, 252));
        Paragraph pLab = new Paragraph(label, F_SMALL);
        pLab.setSpacingAfter(4);
        cell.addElement(pLab);
        cell.addElement(new Paragraph(valeur, new Font(Font.HELVETICA, 13, Font.BOLD, couleur)));
        table.addCell(cell);
    }

    private PdfPTable tableau(float[] largeurs, String... entetes) throws DocumentException {
        PdfPTable table = new PdfPTable(largeurs.length);
        table.setWidthPercentage(100);
        table.setWidths(largeurs);
        for (String e : entetes) {
            PdfPCell c = new PdfPCell(new Phrase(e, F_SECTION));
            c.setBackgroundColor(C_HEADER_TAB);
            c.setPadding(6);
            c.setBorder(Rectangle.NO_BORDER);
            c.setHorizontalAlignment(Element.ALIGN_CENTER);
            table.addCell(c);
        }
        return table;
    }

    private void ligne(PdfPTable table, boolean pair, String... valeurs) {
        Color bg = pair ? C_LIGNE_PAIR : Color.WHITE;
        for (String v : valeurs) {
            PdfPCell c = new PdfPCell(new Phrase(v != null ? v : "-", F_CORPS));
            c.setBackgroundColor(bg);
            c.setPadding(5);
            c.setBorderWidth(0.5f);
            c.setBorderColor(new Color(220, 220, 220));
            table.addCell(c);
        }
    }

    private void ligneVide(PdfPTable table, int cols, String message) {
        PdfPCell c = new PdfPCell(new Phrase(message, F_SMALL));
        c.setColspan(cols);
        c.setHorizontalAlignment(Element.ALIGN_CENTER);
        c.setPadding(10);
        c.setBorderColor(new Color(220, 220, 220));
        table.addCell(c);
    }

    private PdfPCell cellulaCorps(String texte, Color bg) {
        PdfPCell c = new PdfPCell(new Phrase(texte != null ? texte : "-", F_CORPS));
        c.setBackgroundColor(bg);
        c.setPadding(5);
        c.setBorderWidth(0.5f);
        c.setBorderColor(new Color(220, 220, 220));
        return c;
    }

    private void espace(Document doc) throws DocumentException {
        doc.add(new Paragraph(" "));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // UTILITAIRES CALCUL
    // ─────────────────────────────────────────────────────────────────────────
    private BigDecimal somme(List<BigDecimal> valeurs) {
        return valeurs.stream().filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal pourcent(BigDecimal valeur, BigDecimal total) {
        if (total == null || total.compareTo(BigDecimal.ZERO) == 0) return BigDecimal.ZERO;
        return valeur.multiply(new BigDecimal("100")).divide(total, 1, RoundingMode.HALF_UP);
    }

    private String fmt0(BigDecimal v) {
        if (v == null) return "0";
        return String.format("%.0f", v.doubleValue());
    }

    /**
     * Convertit un montant depuis sa devise d'origine (via tauxStocke) vers la devise de rapport.
     * tauxStocke = taux appliqué au moment de la saisie (1 unité devise d'origine = tauxStocke XOF).
     */
    private BigDecimal convertirMontant(BigDecimal montant, Double tauxStocke) {
        if (montant == null) return BigDecimal.ZERO;
        double taux = (tauxStocke != null && tauxStocke > 0) ? tauxStocke : 1.0;
        double tauxDest = tauxRapport.get();
        if (tauxDest <= 0) tauxDest = 1.0;
        // montant → XOF → devise rapport
        double montantXof = montant.doubleValue() * taux;
        double montantRapport = montantXof / tauxDest;
        return BigDecimal.valueOf(montantRapport).setScale(2, RoundingMode.HALF_UP);
    }

    private String formaterMontant(BigDecimal montant) {
        if (montant == null) return "0 " + symboleRapport.get();
        NumberFormat nf = NumberFormat.getNumberInstance(Locale.FRANCE);
        nf.setMaximumFractionDigits(2);
        nf.setMinimumFractionDigits(0);
        return nf.format(montant) + " " + symboleRapport.get();
    }

    private String variation(BigDecimal avant, BigDecimal apres) {
        if (avant == null || avant.compareTo(BigDecimal.ZERO) == 0) {
            return apres != null && apres.compareTo(BigDecimal.ZERO) > 0 ? "+∞%" : "0%";
        }
        if (apres == null) apres = BigDecimal.ZERO;
        BigDecimal v = apres.subtract(avant)
                .multiply(new BigDecimal("100"))
                .divide(avant.abs(), 1, RoundingMode.HALF_UP);
        return (v.compareTo(BigDecimal.ZERO) >= 0 ? "+" : "") + v + "%";
    }

    private String libellePaiement(String mode) {
        if (mode == null) return "Espèces";
        return switch (mode) {
            case "ESPECES"      -> "Espèces";
            case "WAVE"         -> "Wave";
            case "ORANGE_MONEY" -> "Orange Money";
            case "CREDIT"       -> "Crédit";
            default             -> mode;
        };
    }

    private String libelleStatutBL(BonLivraisonEntity.Statut s) {
        if (s == null) return "-";
        return switch (s) {
            case LIVRE      -> "Livré";
            case EN_ATTENTE -> "En attente";
            case EN_COURS   -> "En cours";
            case ANNULE     -> "Annulé";
        };
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PIED DE PAGE (chaque page)
    // ─────────────────────────────────────────────────────────────────────────
    private static class PiedDePageEvent extends PdfPageEventHelper {
        private final String nomEntreprise;
        private final String numRapport;

        PiedDePageEvent(String nomEntreprise, String numRapport) {
            this.nomEntreprise = nomEntreprise;
            this.numRapport    = numRapport;
        }

        @Override
        public void onEndPage(PdfWriter writer, Document doc) {
            PdfContentByte cb = writer.getDirectContent();
            Font f = new Font(Font.HELVETICA, 8, Font.NORMAL, new Color(127, 140, 141));

            cb.setColorStroke(new Color(200, 210, 220));
            cb.moveTo(doc.left(), doc.bottom() - 10);
            cb.lineTo(doc.right(), doc.bottom() - 10);
            cb.stroke();

            ColumnText.showTextAligned(cb, Element.ALIGN_LEFT,
                    new Phrase(nomEntreprise + " — " + numRapport, f),
                    doc.left(), doc.bottom() - 22, 0);

            ColumnText.showTextAligned(cb, Element.ALIGN_RIGHT,
                    new Phrase("Page " + writer.getPageNumber(), f),
                    doc.right(), doc.bottom() - 22, 0);
        }
    }
}
