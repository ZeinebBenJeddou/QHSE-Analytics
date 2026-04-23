import { Injectable } from '@angular/core';
import { StatutNettoyage } from '../enums/statut-nettoyage.enum';
import { NiveauVariation } from '../enums/niveau-variation.enum';
import { Tendance } from '../enums/tendance.enum';

@Injectable({
  providedIn: 'root'
})
export class StatusColorService {
  getCleaningStatusColor(status: StatutNettoyage): string {
    switch (status) {
      case StatutNettoyage.OK:
        return 'success';
      case StatutNettoyage.CORRIGE:
        return 'info';
      case StatutNettoyage.MANQUANT:
        return 'disabled';
      case StatutNettoyage.INVALIDE:
        return 'error';
      case StatutNettoyage.SUSPECT:
        return 'warning';
      case StatutNettoyage.IGNORE:
        return 'disabled';
      default:
        return 'default';
    }
  }

  getVariationLevelColor(level: NiveauVariation): string {
    switch (level) {
      case NiveauVariation.FAIBLE:
        return 'success';
      case NiveauVariation.MODERE:
        return 'warning';
      case NiveauVariation.CRITIQUE:
        return 'error';
      default:
        return 'default';
    }
  }

  getTendanceIcon(tendance: Tendance): string {
    switch (tendance) {
      case Tendance.HAUSSE:
        return 'trending_up';
      case Tendance.BAISSE:
        return 'trending_down';
      case Tendance.STABLE:
        return 'trending_flat';
      default:
        return 'trending_flat';
    }
  }

  getCleaningStatusLabel(status: StatutNettoyage): string {
    switch (status) {
      case StatutNettoyage.OK:
        return 'OK';
      case StatutNettoyage.CORRIGE:
        return 'Corrigé';
      case StatutNettoyage.MANQUANT:
        return 'Manquant';
      case StatutNettoyage.INVALIDE:
        return 'Invalide';
      case StatutNettoyage.SUSPECT:
        return 'Suspect';
      case StatutNettoyage.IGNORE:
        return 'Ignoré';
      default:
        return status;
    }
  }
}
