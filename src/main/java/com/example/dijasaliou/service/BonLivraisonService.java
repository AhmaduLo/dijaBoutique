package com.example.dijasaliou.service;

import com.example.dijasaliou.dto.BonLivraisonDto;
import com.example.dijasaliou.dto.CreateBonLivraisonRequest;
import com.example.dijasaliou.dto.PagedResponse;
import com.example.dijasaliou.entity.BonLivraisonEntity;
import com.example.dijasaliou.entity.LigneBLEntity;
import com.example.dijasaliou.entity.TenantEntity;
import com.example.dijasaliou.entity.UserEntity;
import com.example.dijasaliou.repository.BonLivraisonRepository;
import com.example.dijasaliou.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class BonLivraisonService {

    private final BonLivraisonRepository bonLivraisonRepository;
    private final TenantService tenantService;
    private final UserRepository userRepository;

    public BonLivraisonService(BonLivraisonRepository bonLivraisonRepository,
                                TenantService tenantService,
                                UserRepository userRepository) {
        this.bonLivraisonRepository = bonLivraisonRepository;
        this.tenantService = tenantService;
        this.userRepository = userRepository;
    }

    /**
     * Retourne tous les bons de livraison du tenant courant (filtrés par Hibernate)
     */
    @Transactional(readOnly = true)
    public List<BonLivraisonDto> getTous() {
        return bonLivraisonRepository.findAllByOrderByCreatedDateDesc()
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    /**
     * Retourne les bons de livraison paginés avec filtre statut, recherche et plage de dates optionnels
     */
    @Transactional(readOnly = true)
    public PagedResponse<BonLivraisonDto> getTousPagines(int page, int size, String search, String statut, LocalDate dateDebut, LocalDate dateFin) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdDate"));
        String searchParam = (search != null && !search.isBlank()) ? search : null;
        BonLivraisonEntity.Statut statutParam = null;
        if (statut != null && !statut.isBlank() && !statut.equals("TOUS")) {
            try { statutParam = BonLivraisonEntity.Statut.valueOf(statut); } catch (IllegalArgumentException ignored) { /* valeur inconnue → pas de filtre statut */ }
        }
        LocalDateTime debutDT = (dateDebut != null) ? dateDebut.atStartOfDay() : null;
        LocalDateTime finDT = (dateFin != null) ? dateFin.atTime(23, 59, 59) : null;
        Page<BonLivraisonEntity> blPage = bonLivraisonRepository.findAllWithSearch(statutParam, searchParam, debutDT, finDT, pageable);
        Page<BonLivraisonDto> dtoPage = blPage.map(this::toDto);
        return PagedResponse.from(dtoPage);
    }

    /**
     * Retourne un bon de livraison par ID
     */
    @Transactional(readOnly = true)
    public BonLivraisonDto getParId(String id) {
        BonLivraisonEntity bl = bonLivraisonRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Bon de livraison non trouvé : " + id));
        return toDto(bl);
    }

    /**
     * Crée un nouveau bon de livraison
     */
    @Transactional
    public BonLivraisonDto creer(CreateBonLivraisonRequest request) {
        TenantEntity tenant = tenantService.getCurrentTenant();
        String numeroBL = genererNumeroBL();

        BonLivraisonEntity bl = BonLivraisonEntity.builder()
                .numeroBL(numeroBL)
                .tenant(tenant)
                .clientNom(request.getClientNom())
                .adresseLivraison(request.getAdresseLivraison())
                .telephoneClient(request.getTelephoneClient())
                .note(request.getNote())
                .datePrevueLivraison(request.getDatePrevueLivraison())
                .statut(BonLivraisonEntity.Statut.EN_ATTENTE)
                .build();

        List<LigneBLEntity> lignes = request.getLignes().stream()
                .map(l -> LigneBLEntity.builder()
                        .bonLivraison(bl)
                        .nomProduit(l.getNomProduit())
                        .quantite(l.getQuantite())
                        .unite(l.getUnite() != null ? l.getUnite() : "pièce")
                        .build())
                .collect(Collectors.toList());

        bl.setLignes(lignes);
        BonLivraisonEntity saved = bonLivraisonRepository.save(bl);

        log.info("[BL] Créé : {} — Client: {} — {} produits",
                numeroBL, request.getClientNom(), request.getLignes().size());

        return toDto(saved);
    }

    /**
     * Marque un bon de livraison comme LIVRÉ
     */
    @Transactional
    public BonLivraisonDto marquerLivre(String id) {
        BonLivraisonEntity bl = bonLivraisonRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Bon de livraison non trouvé : " + id));

        bl.setStatut(BonLivraisonEntity.Statut.LIVRE);
        bl.setDateLivraisonEffective(LocalDateTime.now());
        BonLivraisonEntity saved = bonLivraisonRepository.save(bl);

        log.info("[BL] Marqué comme livré : {}", bl.getNumeroBL());
        return toDto(saved);
    }

    /**
     * Annule un bon de livraison
     */
    @Transactional
    public BonLivraisonDto annuler(String id) {
        BonLivraisonEntity bl = bonLivraisonRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Bon de livraison non trouvé : " + id));

        bl.setStatut(BonLivraisonEntity.Statut.ANNULE);
        BonLivraisonEntity saved = bonLivraisonRepository.save(bl);

        log.info("[BL] Annulé : {}", bl.getNumeroBL());
        return toDto(saved);
    }

    /**
     * Supprime un bon de livraison
     */
    @Transactional
    public void supprimer(String id) {
        BonLivraisonEntity bl = bonLivraisonRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Bon de livraison non trouvé : " + id));

        bonLivraisonRepository.delete(bl);
        log.info("[BL] Supprimé : {}", bl.getNumeroBL());
    }

    // ==================== PRIVÉ ====================

    private BonLivraisonDto toDto(BonLivraisonEntity bl) {
        UserEntity admin = bl.getTenant() != null
                ? userRepository.findFirstByTenantAndRole(bl.getTenant(), UserEntity.Role.ADMIN).orElse(null)
                : null;
        return BonLivraisonDto.fromEntity(bl, admin);
    }

    private String genererNumeroBL() {
        String prefix = "BL-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMM")) + "-";
        int maxSeq = bonLivraisonRepository.findMaxSequenceForPrefix(prefix);
        return prefix + String.format("%04d", maxSeq + 1);
    }
}
