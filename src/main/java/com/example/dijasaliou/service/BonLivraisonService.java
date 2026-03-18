package com.example.dijasaliou.service;

import com.example.dijasaliou.dto.BonLivraisonDto;
import com.example.dijasaliou.dto.CreateBonLivraisonRequest;
import com.example.dijasaliou.entity.BonLivraisonEntity;
import com.example.dijasaliou.entity.LigneBLEntity;
import com.example.dijasaliou.entity.TenantEntity;
import com.example.dijasaliou.entity.UserEntity;
import com.example.dijasaliou.repository.BonLivraisonRepository;
import com.example.dijasaliou.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    public List<BonLivraisonDto> getTous() {
        return bonLivraisonRepository.findAllByOrderByCreatedDateDesc()
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    /**
     * Retourne un bon de livraison par ID
     */
    public BonLivraisonDto getParId(Long id) {
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
    public BonLivraisonDto marquerLivre(Long id) {
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
    public BonLivraisonDto annuler(Long id) {
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
    public void supprimer(Long id) {
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
        long count = bonLivraisonRepository.countByNumeroBLStartingWith(prefix);
        return prefix + String.format("%04d", count + 1);
    }
}
