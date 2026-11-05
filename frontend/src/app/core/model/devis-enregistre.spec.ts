import { formaterDate, messageReprise, RepriseDevis } from './devis-enregistre';
import { logementVide } from './devis';
import { reponsesVides } from './recommandation';

describe('devis enregistré', () => {
  const reprise = (partiel: Partial<RepriseDevis>): RepriseDevis => ({
    reference: 'ref-1',
    logement: logementVide(),
    besoins: reponsesVides(),
    formule: 'confort',
    options: [],
    franchise: '300',
    prixMensuel: 14.3,
    ancienPrixMensuel: null,
    dateFinValidite: '2026-12-05',
    ...partiel,
  });

  it('affiche la date au format français', () => {
    expect(formaterDate('2026-12-05')).toBe('05/12/2026');
  });

  it('rappelle la garantie d’un devis encore valable', () => {
    expect(messageReprise(reprise({}))).toBe("Votre devis est repris. Son prix est garanti jusqu'au 05/12/2026.");
  });

  it('AC6 : annonce l’ancien et le nouveau prix d’un devis expiré', () => {
    const message = messageReprise(reprise({ ancienPrixMensuel: 12.5 }));
    expect(message).toContain('avait expiré');
    expect(message).toContain('de 12,50 € à 14,30 € par mois');
  });

  it('AC6 : annonce le recalcul même si le prix ne change pas', () => {
    const message = messageReprise(reprise({ ancienPrixMensuel: 14.3 }));
    expect(message).toContain('recalculé');
    expect(message).toContain('il reste à 14,30 € par mois');
  });
});
