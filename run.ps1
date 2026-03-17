# Script de lancement automatique pour VSCode
# Ce script charge les variables .env et lance l'application Spring Boot

Write-Host ""
Write-Host "=====================================================" -ForegroundColor Cyan
Write-Host "  🚀 Dija Saliou - Lancement Automatique" -ForegroundColor Cyan
Write-Host "=====================================================" -ForegroundColor Cyan
Write-Host ""

# Vérifier que .env existe
if (-Not (Test-Path ".env")) {
    Write-Host "❌ Erreur: Fichier .env introuvable !" -ForegroundColor Red
    Write-Host ""
    Write-Host "📋 Solution:" -ForegroundColor Yellow
    Write-Host "  1. Créer un fichier .env à la racine du projet" -ForegroundColor White
    Write-Host "  2. Copier le contenu de .env.example" -ForegroundColor White
    Write-Host "  3. Remplir avec vos vraies clés Stripe" -ForegroundColor White
    Write-Host ""
    Write-Host "Voir CONFIGURATION_VSCODE.md pour plus de détails." -ForegroundColor Yellow
    Write-Host ""
    exit 1
}

Write-Host "🔧 Chargement des variables d'environnement..." -ForegroundColor Cyan
Write-Host ""

$variablesLoaded = 0

Get-Content .env | ForEach-Object {
    # Ignorer les lignes vides et les commentaires
    if ($_ -match '^([^=#]+)=(.*)$') {
        $key = $matches[1].Trim()
        $value = $matches[2].Trim()

        [System.Environment]::SetEnvironmentVariable($key, $value, "Process")
        Write-Host "  ✅ $key chargé" -ForegroundColor Green
        $variablesLoaded++
    }
}

Write-Host ""
Write-Host "✅ $variablesLoaded variable(s) chargée(s) avec succès" -ForegroundColor Green
Write-Host ""
Write-Host "=====================================================" -ForegroundColor Cyan
Write-Host ""

# Vérifier les variables critiques
$missingVars = @()

if (-Not $env:STRIPE_PUBLIC_KEY) { $missingVars += "STRIPE_PUBLIC_KEY" }
if (-Not $env:STRIPE_SECRET_KEY) { $missingVars += "STRIPE_SECRET_KEY" }
if (-Not $env:STRIPE_WEBHOOK_SECRET) { $missingVars += "STRIPE_WEBHOOK_SECRET" }

if ($missingVars.Count -gt 0) {
    Write-Host "⚠️  Attention: Variables manquantes:" -ForegroundColor Yellow
    $missingVars | ForEach-Object {
        Write-Host "  ❌ $_" -ForegroundColor Red
    }
    Write-Host ""
    Write-Host "L'application risque de ne pas démarrer correctement." -ForegroundColor Yellow
    Write-Host "Voir .env.example pour la liste complète des variables." -ForegroundColor Yellow
    Write-Host ""
}

Write-Host "🚀 Lancement de l'application Spring Boot..." -ForegroundColor Cyan
Write-Host ""
Write-Host "=====================================================" -ForegroundColor Cyan
Write-Host ""

# Lancer l'application
./mvnw.cmd spring-boot:run
