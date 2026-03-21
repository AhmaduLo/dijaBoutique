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
        return clientRepository.findAllWithSearch(searchParam).stream()
                .map(c -> ClientDto.fromEntity(c, countCreditsActifs(c.getId())))
                .collect(Collectors.toList());
    }

    public PagedResponse<ClientDto> obtenirClientsPagines(int page, int size, String search) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "nom"));
        String searchParam = (search != null && !search.isBlank()) ? search : null;
        Page<ClientEntity> pageResult = clientRepository.findAllWithSearchPaged(searchParam, pageable);
        Page<ClientDto> dtoPage = pageResult.map(c -> ClientDto.fromEntity(c, countCreditsActifs(c.getId())));
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
        return ClientDto.fromEntity(client, countCreditsActifs(id));
    }

    private long countCreditsActifs(Long clientId) {
        return creditClientRepository.countCreditsActifsByClientId(clientId, CreditClientEntity.StatutCredit.SOLDE);
    }
}
