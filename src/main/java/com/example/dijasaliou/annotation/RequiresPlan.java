package com.example.dijasaliou.annotation;

import com.example.dijasaliou.entity.TenantEntity;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation pour restreindre l'accès à certaines fonctionnalités selon le plan d'abonnement
 *
 * UTILISATION :
 * @RequiresPlan(plans = {Plan.PREMIUM, Plan.ENTREPRISE})
 * public ResponseEntity<byte[]> exporterAchats() { ... }
 *
 * → Cette méthode sera accessible uniquement aux plans PREMIUM et ENTREPRISE
 * → Les utilisateurs avec plan BASIC recevront une erreur 403 avec message explicite
 *
 * EXEMPLE D'UTILISATION DANS LE PROJET :
 * - Export individuel des achats : @RequiresPlan(plans = {Plan.PREMIUM, Plan.ENTREPRISE})
 * - Export individuel des ventes : @RequiresPlan(plans = {Plan.PREMIUM, Plan.ENTREPRISE})
 * - Export individuel des dépenses : @RequiresPlan(plans = {Plan.PREMIUM, Plan.ENTREPRISE})
 * - Export des rapports globaux : Pas d'annotation (accessible à tous les plans payants)
 */
@Target(ElementType.METHOD) // S'applique uniquement aux méthodes
@Retention(RetentionPolicy.RUNTIME) // L'annotation est disponible à l'exécution
public @interface RequiresPlan {
    /**
     * Liste des plans autorisés à accéder à cette fonctionnalité
     */
    TenantEntity.Plan[] plans();

    /**
     * Message d'erreur personnalisé (optionnel)
     * Si non fourni, un message par défaut sera utilisé
     */
    String message() default "";
}
