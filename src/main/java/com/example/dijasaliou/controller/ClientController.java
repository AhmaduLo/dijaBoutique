package com.example.dijasaliou.controller;

import com.example.dijasaliou.dto.ClientDto;
import com.example.dijasaliou.dto.PagedResponse;
import com.example.dijasaliou.service.ClientService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/clients")

@Slf4j
@RequiredArgsConstructor
public class ClientController {

    private final ClientService clientService;

    @GetMapping
    @PreAuthorize("hasAnyAuthority('ADMIN', 'GERANT')")
    public ResponseEntity<PagedResponse<ClientDto>> obtenirTous(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String search) {
        return ResponseEntity.ok(clientService.obtenirClientsPagines(page, size, search));
    }

    @GetMapping("/search")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'GERANT')")
    public ResponseEntity<List<ClientDto>> rechercher(@RequestParam(required = false) String q) {
        return ResponseEntity.ok(clientService.rechercherClients(q));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'GERANT')")
    public ResponseEntity<ClientDto> obtenirParId(@PathVariable Long id) {
        return ResponseEntity.ok(clientService.obtenirClientParId(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('ADMIN', 'GERANT')")
    public ResponseEntity<ClientDto> creer(@RequestBody Map<String, String> body) {
        String nom = body.get("nom");
        String telephone = body.get("telephone");
        return ResponseEntity.ok(clientService.creerClient(nom, telephone));
    }
}
