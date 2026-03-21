package com.example.dijasaliou.aspect;

import com.example.dijasaliou.annotation.RequiresPlan;
import com.example.dijasaliou.entity.TenantEntity;
import com.example.dijasaliou.service.TenantService;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.util.Arrays;

/**
 * Aspect AOP pour vérifier les restrictions de plan d'abonnement
 *
 * FONCTIONNEMENT :
 * 1. Intercepte toutes les méthodes annotées avec @RequiresPlan
 * 2. Récupère le plan du tenant actuel
 * 3. Vérifie si le plan est dans la liste des plans autorisés
 * 4. Si non autorisé → Lance une exception avec message explicite
 * 5. Si autorisé → Laisse la méthode s'exécuter normalement
 *
 * EXEMPLE :
 * - Utilisateur avec plan BASIC appelle une méthode @RequiresPlan(plans = {PREMIUM, ENTREPRISE})
 * → Erreur 403 : "Fonctionnalité réservée aux plans PREMIUM, ENTREPRISE. Veuillez mettre à jour votre abonnement."
 */
@Aspect
@Component
@Slf4j
public class PlanRestrictionAspect {

    private final TenantService tenantService;

    public PlanRestrictionAspect(TenantService tenantService) {
        this.tenantService = tenantService;
    }

    /**
     * Intercepte AVANT l'exécution de toute méthode annotée avec @RequiresPlan
     *
     * @Before : S'exécute AVANT la méthode
     * @annotation : Cible les méthodes avec l'annotation @RequiresPlan
     */
    @Before("@annotation(com.example.dijasaliou.annotation.RequiresPlan)")
    public void checkPlanRestriction(JoinPoint joinPoint) {
        // 1. Récupérer l'annotation depuis la méthode
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        RequiresPlan requiresPlan = signature.getMethod().getAnnotation(RequiresPlan.class);

        // 2. Si pas de tenant dans le contexte (SUPER_ADMIN ou non authentifié),
        //    on laisse passer — Spring Security gère l'authentification séparément
        if (!tenantService.isTenantDefined()) {
            log.debug("Pas de tenant en contexte — vérification du plan ignorée");
            return;
        }

        // 3. Récupérer le tenant actuel
        TenantEntity tenant = tenantService.getCurrentTenant();

        // 4. Récupérer le plan du tenant
        TenantEntity.Plan currentPlan = tenant.getPlan();

        // 5. Vérifier si le plan actuel est dans la liste des plans autorisés
        TenantEntity.Plan[] allowedPlans = requiresPlan.plans();
        boolean isAllowed = Arrays.asList(allowedPlans).contains(currentPlan);

        // 6. Si pas autorisé, lancer une exception
        if (!isAllowed) {
            String allowedPlansStr = Arrays.stream(allowedPlans)
                    .map(TenantEntity.Plan::getLibelle)
                    .reduce((a, b) -> a + ", " + b)
                    .orElse("");

            String customMessage = requiresPlan.message();
            String errorMessage;

            if (customMessage != null && !customMessage.isEmpty()) {
                errorMessage = customMessage;
            } else {
                errorMessage = String.format(
                        "Cette fonctionnalité est réservée aux plans %s. " +
                                "Votre plan actuel (%s) ne permet pas d'accéder à cette fonctionnalité. " +
                                "Veuillez mettre à jour votre abonnement pour y accéder.",
                        allowedPlansStr,
                        currentPlan.getLibelle()
                );
            }

            log.warn("🚫 Accès refusé : Tenant {} avec plan {} a tenté d'accéder à une fonctionnalité réservée aux plans {}",
                    tenant.getTenantUuid(),
                    currentPlan,
                    allowedPlansStr);

            throw new PlanRestrictionException(errorMessage);
        }

        log.debug("✅ Accès autorisé : Tenant {} avec plan {} peut accéder à cette fonctionnalité",
                tenant.getTenantUuid(),
                currentPlan);
    }

    /**
     * Exception personnalisée pour les restrictions de plan
     */
    public static class PlanRestrictionException extends RuntimeException {
        public PlanRestrictionException(String message) {
            super(message);
        }
    }
}
