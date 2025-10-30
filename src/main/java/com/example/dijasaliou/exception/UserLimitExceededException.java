package com.example.dijasaliou.exception;

/**
 * Exception levée quand un admin tente de créer un utilisateur
 * mais que la limite du plan est atteinte
 */
public class UserLimitExceededException extends RuntimeException {

    private final String planName;
    private final long currentCount;
    private final int maxAllowed;

    public UserLimitExceededException(String planName, long currentCount, int maxAllowed) {
        super(String.format(
            "Limite d'utilisateurs atteinte pour le plan %s (%d/%d). " +
            "Veuillez passer à un plan supérieur pour ajouter plus d'utilisateurs.",
            planName, currentCount, maxAllowed
        ));
        this.planName = planName;
        this.currentCount = currentCount;
        this.maxAllowed = maxAllowed;
    }

    public String getPlanName() {
        return planName;
    }

    public long getCurrentCount() {
        return currentCount;
    }

    public int getMaxAllowed() {
        return maxAllowed;
    }
}
