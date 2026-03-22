package com.example.dijasaliou.aspect;

import com.example.dijasaliou.annotation.RequiresPlan;
import com.example.dijasaliou.entity.TenantEntity;
import com.example.dijasaliou.service.TenantService;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/**
 * Tests unitaires — PlanRestrictionAspect.
 *
 * Vérifie que l'aspect bloque ou autorise l'accès selon le plan du tenant,
 * sans démarrer de contexte Spring (pure Mockito).
 *
 * Stratégie : des méthodes privées annotées @RequiresPlan dans cette classe
 * servent de "cibles" simulées ; on récupère leur objet Method par réflexion
 * pour alimenter le mock MethodSignature.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Tests unitaires — PlanRestrictionAspect")
class PlanRestrictionAspectTest {

    @Mock
    private TenantService tenantService;

    @Mock
    private JoinPoint joinPoint;

    @Mock
    private MethodSignature methodSignature;

    @InjectMocks
    private PlanRestrictionAspect aspect;

    // ── Méthodes annotées utilisées comme cibles de test ──────────────────────

    @RequiresPlan(plans = {TenantEntity.Plan.PREMIUM, TenantEntity.Plan.ENTREPRISE})
    private void methodeReserveePremiumEntreprise() {}

    @RequiresPlan(plans = {TenantEntity.Plan.BASIC, TenantEntity.Plan.PREMIUM, TenantEntity.Plan.ENTREPRISE})
    private void methodeReserveeBasicEtPlus() {}

    @RequiresPlan(plans = {TenantEntity.Plan.PREMIUM}, message = "Accès réservé au plan Premium.")
    private void methodeAvecMessagePersonnalise() {}

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void configurerMethode(String nomMethode) throws NoSuchMethodException {
        Method method = PlanRestrictionAspectTest.class.getDeclaredMethod(nomMethode);
        when(joinPoint.getSignature()).thenReturn(methodSignature);
        when(methodSignature.getMethod()).thenReturn(method);
    }

    private TenantEntity tenantAvecPlan(TenantEntity.Plan plan) {
        return TenantEntity.builder()
                .tenantUuid("uuid-test")
                .nomEntreprise("Boutique Test")
                .numeroTelephone("+221771111111")
                .plan(plan)
                .build();
    }

    @BeforeEach
    void setUp() {
        when(tenantService.isTenantDefined()).thenReturn(true);
    }

    // ── Sans tenant en contexte ───────────────────────────────────────────────

    @Test
    @DisplayName("Pas de tenant en contexte (SUPER_ADMIN) → laisse passer sans vérification")
    void sansTenantContexte_laissePasser() throws NoSuchMethodException {
        configurerMethode("methodeReserveePremiumEntreprise");
        when(tenantService.isTenantDefined()).thenReturn(false);

        assertThatNoException().isThrownBy(() -> aspect.checkPlanRestriction(joinPoint));
    }

    // ── Plan autorisé ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("Plan PREMIUM → méthode réservée PREMIUM/ENTREPRISE → autorisé")
    void planPREMIUM_methodePremiumEntreprise_autorise() throws NoSuchMethodException {
        configurerMethode("methodeReserveePremiumEntreprise");
        when(tenantService.getCurrentTenant()).thenReturn(tenantAvecPlan(TenantEntity.Plan.PREMIUM));

        assertThatNoException().isThrownBy(() -> aspect.checkPlanRestriction(joinPoint));
    }

    @Test
    @DisplayName("Plan ENTREPRISE → méthode réservée PREMIUM/ENTREPRISE → autorisé")
    void planENTREPRISE_methodePremiumEntreprise_autorise() throws NoSuchMethodException {
        configurerMethode("methodeReserveePremiumEntreprise");
        when(tenantService.getCurrentTenant()).thenReturn(tenantAvecPlan(TenantEntity.Plan.ENTREPRISE));

        assertThatNoException().isThrownBy(() -> aspect.checkPlanRestriction(joinPoint));
    }

    @Test
    @DisplayName("Plan BASIC → méthode réservée BASIC/PREMIUM/ENTREPRISE → autorisé")
    void planBASIC_methodeBasicEtPlus_autorise() throws NoSuchMethodException {
        configurerMethode("methodeReserveeBasicEtPlus");
        when(tenantService.getCurrentTenant()).thenReturn(tenantAvecPlan(TenantEntity.Plan.BASIC));

        assertThatNoException().isThrownBy(() -> aspect.checkPlanRestriction(joinPoint));
    }

    // ── Plan refusé ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("Plan BASIC → méthode réservée PREMIUM/ENTREPRISE → PlanRestrictionException")
    void planBASIC_methodePremiumEntreprise_refuse() throws NoSuchMethodException {
        configurerMethode("methodeReserveePremiumEntreprise");
        when(tenantService.getCurrentTenant()).thenReturn(tenantAvecPlan(TenantEntity.Plan.BASIC));

        assertThatThrownBy(() -> aspect.checkPlanRestriction(joinPoint))
                .isInstanceOf(PlanRestrictionAspect.PlanRestrictionException.class);
    }

    @Test
    @DisplayName("Plan GRATUIT → méthode réservée BASIC/PREMIUM/ENTREPRISE → PlanRestrictionException")
    void planGRATUIT_methodeBasicEtPlus_refuse() throws NoSuchMethodException {
        configurerMethode("methodeReserveeBasicEtPlus");
        when(tenantService.getCurrentTenant()).thenReturn(tenantAvecPlan(TenantEntity.Plan.GRATUIT));

        assertThatThrownBy(() -> aspect.checkPlanRestriction(joinPoint))
                .isInstanceOf(PlanRestrictionAspect.PlanRestrictionException.class);
    }

    // ── Contenu du message d'erreur ───────────────────────────────────────────

    @Test
    @DisplayName("Message par défaut mentionne les plans autorisés et le plan actuel")
    void messageDefaut_contientPlansEtPlanActuel() throws NoSuchMethodException {
        configurerMethode("methodeReserveePremiumEntreprise");
        when(tenantService.getCurrentTenant()).thenReturn(tenantAvecPlan(TenantEntity.Plan.BASIC));

        assertThatThrownBy(() -> aspect.checkPlanRestriction(joinPoint))
                .isInstanceOf(PlanRestrictionAspect.PlanRestrictionException.class)
                .hasMessageContaining("Premium")   // libellé du plan PREMIUM
                .hasMessageContaining("Entreprise") // libellé du plan ENTREPRISE
                .hasMessageContaining("Basic");     // libellé du plan actuel BASIC
    }

    @Test
    @DisplayName("Message personnalisé dans l'annotation → utilisé à la place du message par défaut")
    void messagePersonnalise_utiliseLeMessageAnnotation() throws NoSuchMethodException {
        configurerMethode("methodeAvecMessagePersonnalise");
        when(tenantService.getCurrentTenant()).thenReturn(tenantAvecPlan(TenantEntity.Plan.BASIC));

        assertThatThrownBy(() -> aspect.checkPlanRestriction(joinPoint))
                .isInstanceOf(PlanRestrictionAspect.PlanRestrictionException.class)
                .hasMessage("Accès réservé au plan Premium.");
    }
}
