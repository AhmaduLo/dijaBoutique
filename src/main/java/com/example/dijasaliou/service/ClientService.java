package com.example.dijasaliou.service;

import com.example.dijasaliou.dto.ClientDto;
import com.example.dijasaliou.dto.PagedResponse;
import com.example.dijasaliou.entity.ClientEntity;
import com.example.dijasaliou.entity.CreditClientEntity;
import com.example.dijasaliou.entity.TenantEntity;
import com.example.dijasaliou.repository.ClientRepository;
import com.example.dijasaliou.repository.CreditClientRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class ClientService {

    private final ClientRepository clientRepository;
    private final CreditClientRepository creditClientRepository;
    private final TenantService tenantService;

    public List<ClientDto> rechercherClients(String search) {
        String searchParam = (search != null && !search.isBlank()) ? search : null;
        String tenantUuid = tenantService.getCurrentTenant().getTenantUuid();
        List<ClientEntity> clients = clientRepository.findAllWithSearch(searchParam, tenantUuid);
        Map<Long, Long> countParClient = countCreditsActifsBatch(clients, tenantUuid);
        return clients.stream()
                .map(c -> ClientDto.fromEntity(c, countParClient.getOrDefault(c.getId(), 0L)))
                .collect(Collectors.toList());
    }

    public PagedResponse<ClientDto> obtenirClientsPagines(int page, int size, String search) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "nom"));
        String searchParam = (search != null && !search.isBlank()) ? search : null;
        String tenantUuid = tenantService.getCurrentTenant().getTenantUuid();
        Page<ClientEntity> pageResult = clientRepository.findAllWithSearchPaged(searchParam, tenantUuid, pageable);
        List<ClientEntity> clients = pageResult.getContent();
        Map<Long, Long> countParClient = countCreditsActifsBatch(clients, tenantUuid);
        Page<ClientDto> dtoPage = pageResult.map(c -> ClientDto.fromEntity(c, countParClient.getOrDefault(c.getId(), 0L)));
        return PagedResponse.from(dtoPage);
    }

    @Transactional
    public ClientDto creerClient(String nom, String telephone) {
        if (nom == null || nom.isBlank()) {
            throw new IllegalArgumentException("Le nom du client est obligatoire");
        }
        TenantEntity tenant = tenantService.getCurrentTenant();
        ClientEntity client = ClientEntity.builder()
                .nom(nom.trim())
                .telephone(telephone != null ? telephone.trim() : null)
                .tenant(tenant)
                .build();
        ClientEntity saved = clientRepository.save(client);
        log.info("Client créé : {} (tenant: {})", saved.getNom(), tenant.getTenantUuid());
        return ClientDto.fromEntity(saved, 0L);
    }

    public ClientDto obtenirClientParId(Long id) {
        ClientEntity client = clientRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Client introuvable : " + id));
        String tenantUuid = tenantService.getCurrentTenant().getTenantUuid();
        return ClientDto.fromEntity(client, countCreditsActifs(id, tenantUuid));
    }

    private long countCreditsActifs(Long clientId, String tenantUuid) {
        return creditClientRepository.countCreditsActifsByClientId(clientId, CreditClientEntity.StatutCredit.SOLDE, tenantUuid);
    }

    /**
     * Compte les crédits actifs pour tous les clients en une seule requête SQL.
     * Évite le problème N+1 (1 requête au lieu de 1 par client).
     */
    private Map<Long, Long> countCreditsActifsBatch(List<ClientEntity> clients, String tenantUuid) {
        if (clients.isEmpty()) return Map.of();
        List<Long> ids = clients.stream().map(ClientEntity::getId).collect(Collectors.toList());
        return creditClientRepository
                .countCreditsActifsByClientIds(ids, CreditClientEntity.StatutCredit.SOLDE, tenantUuid)
                .stream()
                .collect(Collectors.toMap(
                        row -> (Long) row[0],
                        row -> (Long) row[1]
                ));
    }
}
