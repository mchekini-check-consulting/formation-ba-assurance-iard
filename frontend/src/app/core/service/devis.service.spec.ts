import { RECO_CONFORT, tarifDetaille } from '../model/tarif.exemples';
import { TestBed } from '@angular/core/testing';
import { logementVide } from '../model/devis';
import { reponsesVides } from '../model/recommandation';
import { DevisService } from './devis.service';

describe('DevisService', () => {
  let service: DevisService;

  beforeEach(() => {
    sessionStorage.clear();
    service = TestBed.inject(DevisService);
  });

  describe('US-12', () => {
    const reprise = {
      reference: 'ref-9',
      logement: { ...logementVide(), surface: '60', typeLogement: 'appartement' as const, adresse: '12 rue des Lilas' },
      besoins: { ...reponsesVides(), valeurContenu: 5000 },
      formule: 'confort' as const,
      options: ['assistance-depannage' as const],
      franchise: '300' as const,
      prixMensuel: 12.5,
      ancienPrixMensuel: null,
      dateFinValidite: '2026-12-05',
    };

    it('AC5 : remet toutes les réponses en place', () => {
      service.restaurer(reprise);
      expect(service.logement().adresse).toBe('12 rue des Lilas');
      expect(service.besoins().valeurContenu).toBe(5000);
      expect(service.reference()).toBe('ref-9');
      expect(service.formuleChoisie()).toBe('confort');
      expect(service.optionsChoisies()).toEqual(['assistance-depannage']);
      expect(service.franchiseChoisie()).toBe('300');
    });

    it('oublie le mandat signé sur un autre devis', () => {
      service.mandatSigne.set(true);
      service.referenceDossier.set('ancien');
      service.restaurer(reprise);
      expect(service.mandatSigne()).toBe(false);
      expect(service.referenceDossier()).toBeNull();
    });

    it('RG1 : relit le prix d’origine auprès du serveur plutôt que de le recalculer', () => {
      service.tarif.set(tarifDetaille());
      service.restaurer(reprise);
      expect(service.tarif()).toBeNull();
      expect(service.prixMensuel()).toBeNull();
    });

    it('prépare la demande seulement si le devis est complet', () => {
      expect(service.demandeDevis()).toBeNull();
      service.restaurer(reprise);
      expect(service.demandeDevis()?.reference).toBe('ref-9');
    });

    it('AC2 : retrouve les réponses après un rechargement', () => {
      service.restaurer(reprise);
      service.conserverBrouillon();
      TestBed.resetTestingModule();
      const recharge = TestBed.inject(DevisService);
      recharge.restaurerBrouillon();
      expect(recharge.logement().adresse).toBe('12 rue des Lilas');
      expect(recharge.reference()).toBe('ref-9');
      expect(recharge.franchiseChoisie()).toBe('300');
    });

    it('n’a rien à restaurer sans copie ou après son effacement', () => {
      service.restaurerBrouillon();
      expect(service.reference()).toBeNull();
      service.restaurer(reprise);
      service.conserverBrouillon();
      service.oublierBrouillon();
      expect(sessionStorage.getItem('assureasy.devis')).toBeNull();
    });

    it('ignore une copie illisible', () => {
      sessionStorage.setItem('assureasy.devis', '{illisible');
      service.restaurerBrouillon();
      expect(service.reference()).toBeNull();
    });

    it('fonctionne sans stockage disponible', () => {
      const refus = () => {
        throw new Error('stockage bloqué');
      };
      const ecriture = vi.spyOn(Storage.prototype, 'setItem').mockImplementation(refus);
      const effacement = vi.spyOn(Storage.prototype, 'removeItem').mockImplementation(refus);
      expect(() => service.conserverBrouillon()).not.toThrow();
      expect(() => service.oublierBrouillon()).not.toThrow();
      ecriture.mockRestore();
      effacement.mockRestore();
    });
  });

  it('la cotisation vient du dernier tarif du serveur', () => {
    expect(service.prixMensuel()).toBeNull();
    service.tarif.set(tarifDetaille('confort', ['objets-valeur'], '500'));
    expect(service.prixMensuel()).toBe(13.72);
  });

  it('oublie le conseil dès qu’une réponse change', () => {
    service.validerRecommandation('ref-1', RECO_CONFORT);
    service.mettreAJourBesoins({ valeurContenu: 25000 });
    expect(service.recommandation()).toBeNull();
    expect(service.reference()).toBeNull();
  });

  it('sélectionne la formule conseillée une fois la recommandation enregistrée', () => {
    service.validerRecommandation('ref-1', RECO_CONFORT);
    expect(service.reference()).toBe('ref-1');
    expect(service.formuleChoisie()).toBe('confort');
  });

  it('oublie la référence dès qu’une réponse change', () => {
    service.validerRecommandation('ref-1', RECO_CONFORT);
    service.mettreAJourBesoins({ litiges: true });
    expect(service.reference()).toBeNull();
  });
});
