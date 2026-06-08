package com.example.dijasaliou.controller;

import com.example.dijasaliou.annotation.RequiresPlan;
import com.example.dijasaliou.dto.ActiverCaisseRequest;
import com.example.dijasaliou.dto.CaisseSoldeDto;
import com.example.dijasaliou.dto.MouvementCaisseRequest;
import com.example.dijasaliou.dto.TransfertCaisseRequest;
import com.example.dijasaliou.entity.TenantEntity;
import com.example.dijasaliou.entity.UserEntity;
import com.example.dijasaliou.service.CaisseService;
import com.example.dijasaliou.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * Endpoints du module Caisse multi-comptes.
 *
 * Réservé au plan BUSINESS via @RequiresPlan.
 * Réservé aux rôles ADMIN / GERANT.
 *
 *   GET  /api/caisse                  → solde actuel (multi-comptes)
 *   POST /api/caisse/activer          → activer la caisse (soldes initiaux)
 *   POST /api/caisse/transfert        → transférer d'un compte vers un autre
 *   POST /api/caisse/mouvement-manuel → entrée ou sortie manuelle
 */
@RestController
@RequestMapping("/caisse")
@RequiredArgsConstructor
public class CaisseController {

    private final CaisseService caisseService;
    private final UserService   userService;

    @GetMapping
    @PreAuthorize("hasAnyAuthority('GERANT', 'ADMIN')")
    @RequiresPlan(
            plans = {TenantEntity.Plan.BUSINESS},
            message = "Le module Caisse est réservé au plan BUSINESS"
    )
    public ResponseEntity<CaisseSoldeDto> getSolde() {
        return ResponseEntity.ok(caisseService.getSoldeActuel());
    }

    @PostMapping("/activer")
    @PreAuthorize("hasAnyAuthority('GERANT', 'ADMIN')")
    @RequiresPlan(
            plans = {TenantEntity.Plan.BUSINESS},
            message = "Le module Caisse est réservé au plan BUSINESS"
    )
    public ResponseEntity<CaisseSoldeDto> activer(@Valid @RequestBody ActiverCaisseRequest request,
                                                  Authentication authentication) {
        UserEntity user = userService.obtenirUtilisateurParEmail(authentication.getName());
        String userUuid = user != null ? String.valueOf(user.getId()) : null;
        return ResponseEntity.ok(caisseService.activerCaisse(request, userUuid));
    }

    @PostMapping("/transfert")
    @PreAuthorize("hasAnyAuthority('GERANT', 'ADMIN')")
    @RequiresPlan(
            plans = {TenantEntity.Plan.BUSINESS},
            message = "Le module Caisse est réservé au plan BUSINESS"
    )
    public ResponseEntity<CaisseSoldeDto> creerTransfert(@Valid @RequestBody TransfertCaisseRequest request,
                                                         Authentication authentication) {
        UserEntity user = userService.obtenirUtilisateurParEmail(authentication.getName());
        String userUuid = user != null ? String.valueOf(user.getId()) : null;
        return ResponseEntity.ok(caisseService.creerTransfert(request, userUuid));
    }

    @PostMapping("/mouvement-manuel")
    @PreAuthorize("hasAnyAuthority('GERANT', 'ADMIN')")
    @RequiresPlan(
            plans = {TenantEntity.Plan.BUSINESS},
            message = "Le module Caisse est réservé au plan BUSINESS"
    )
    public ResponseEntity<CaisseSoldeDto> creerMouvementManuel(@Valid @RequestBody MouvementCaisseRequest request,
                                                               Authentication authentication) {
        UserEntity user = userService.obtenirUtilisateurParEmail(authentication.getName());
        String userUuid = user != null ? String.valueOf(user.getId()) : null;
        return ResponseEntity.ok(caisseService.creerMouvementManuel(request, userUuid));
    }
}
