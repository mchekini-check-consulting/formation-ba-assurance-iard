import { Route } from '@angular/router';
import { ADRESSES, LIVRAISONS, navigationClient, navigationInterne } from './core/config/raccourcis';
import { CadreEspace } from './feature/cadre-espace/cadre-espace';
import { routes } from './app.routes';

/** Une route et ses parents, avec son adresse complète (« espace-client/contrats/:numero »). */
interface Chemin {
  adresse: string;
  route: Route;
  parents: Route[];
}

function aplatir(liste: Route[], prefixe = '', parents: Route[] = []): Chemin[] {
  return liste.flatMap((route) => {
    const adresse = [prefixe, route.path ?? ''].filter(Boolean).join('/');
    return [{ adresse, route, parents }, ...aplatir(route.children ?? [], adresse, [...parents, route])];
  });
}

/** La page qui affiche cette adresse : la route la plus profonde (« :numero » accepte n'importe quel segment). */
function routeDe(adresse: string): Chemin | undefined {
  const segments = adresse.replace(/^\//, '').split('/');
  const trouves = aplatir(routes).filter((chemin) => {
    if (!chemin.route.loadComponent && !chemin.route.component) return false;
    const attendus = chemin.adresse.split('/').filter(Boolean);
    return (
      attendus.length === segments.length && attendus.every((s, i) => s.startsWith(':') || s === segments[i])
    );
  });
  return trouves.sort((a, b) => b.parents.length - a.parents.length)[0];
}

async function composant(route: Route): Promise<unknown> {
  return route.component ?? (await route.loadComponent?.());
}

/** Vrai si la route est rendue dans le cadre commun : un de ses parents affiche CadreEspace. */
async function dansLeCadre(chemin: Chemin): Promise<boolean> {
  for (const parent of chemin.parents) {
    if ((await composant(parent)) === CadreEspace) return true;
  }
  return false;
}

/**
 * US-18 : règle de l'espace client. Toute page accessible depuis la barre latérale (pages client présentes et futures
 * déclarées dans LIVRAISONS/ADRESSES, pages internes) s'affiche dans le cadre commun : en-tête et barre latérale
 * verticale à gauche. Ce test échoue si une page est ajoutée hors du gabarit de route.
 */
describe('règle : les pages de la barre latérale s’affichent dans le cadre commun (US-18)', () => {
  const toutLivre = Object.fromEntries(Object.keys(LIVRAISONS).map((cle) => [cle, true])) as typeof LIVRAISONS;
  const adresses = [
    ...navigationClient(toutLivre, ADRESSES).map((entree) => entree.adresse),
    ...navigationInterne().map((entree) => entree.adresse),
  ];

  it('la barre latérale propose au moins les pages client et internes actuelles', () => {
    expect(adresses).toEqual(
      expect.arrayContaining([
        '/espace-client',
        '/espace-client/contrats',
        '/espace-client/devis',
        '/espace-client/documents',
        '/back-office/clients',
        '/admin/profils',
        '/admin/acces-refuses',
      ]),
    );
  });

  const entrees: [string, string][] = [...navigationClient(toutLivre, ADRESSES), ...navigationInterne()].map((e) => [
    e.libelle,
    e.adresse,
  ]);

  it.each(entrees)(
    '« %s » (%s) existe et s’affiche dans le cadre',
    async (_libelle, adresse) => {
      const chemin = routeDe(adresse);
      expect(chemin, `aucune route pour ${adresse}`).toBeDefined();
      expect(await dansLeCadre(chemin!), `${adresse} n'est pas rendue dans CadreEspace`).toBe(true);
    },
  );

  it('toute page sous /espace-client, /back-office ou /admin (fiches comprises) s’affiche dans le cadre', async () => {
    const espaces = /^(espace-client|back-office|admin)(\/|$)/;
    const pages = aplatir(routes).filter(
      (chemin) => espaces.test(chemin.adresse) && (chemin.route.component || chemin.route.loadComponent),
    );
    const horsCadre: string[] = [];
    for (const page of pages) {
      const estLeCadre = (await composant(page.route)) === CadreEspace;
      if (!estLeCadre && !(await dansLeCadre(page))) horsCadre.push(page.adresse);
    }

    expect(pages.length).toBeGreaterThanOrEqual(10);
    expect(horsCadre).toEqual([]);
  });

  it('les gardes de droits sont conservées sur chaque page (US-04)', () => {
    const pages = aplatir(routes).filter(
      (chemin) => /^(espace-client|back-office|admin)\//.test(chemin.adresse + '/') && chemin.parents.length > 0,
    );
    expect(pages.every((page) => (page.route.canActivate ?? []).length > 0)).toBe(true);
  });
});
