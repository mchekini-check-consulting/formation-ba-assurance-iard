// Page d'accueil : l'offre d'assurance habitation comprise en un coup d'œil,
// sans compte ni appel à un conseiller.
// Les valeurs entre crochets ([PRIX]...) sont à fournir par le métier.

function Icone({ children, className = 'size-6' }) {
  return (
    <svg
      viewBox="0 0 24 24"
      aria-hidden="true"
      className={`${className} flex-none fill-none stroke-current stroke-2 [stroke-linecap:round] [stroke-linejoin:round]`}
    >
      {children}
    </svg>
  )
}

const coche = <path d="M5 12.5l4.5 4.5L19 7.5" />
const croix = <path d="M6 6l12 12M18 6L6 18" />

const resume = [
  { libelle: 'Pour', valeur: 'Locataires et propriétaires' },
  { libelle: 'Logement', valeur: 'Appartement ou maison' },
  { libelle: 'Inclus', valeur: '6 garanties essentielles' },
  { libelle: 'Engagement', valeur: 'Aucun, résiliable en ligne' },
]

const profils = [
  {
    titre: 'Vous êtes locataire',
    texte:
      "Vous louez un appartement ou une maison : c'est l'assurance que votre propriétaire vous demande.",
    icone: <path d="M8 11a3 3 0 100-6 3 3 0 000 6zM10.5 9.5L20 19M16 15l2-2M18 17l2-2" />,
  },
  {
    titre: 'Vous êtes propriétaire',
    texte:
      'Vous habitez le logement que vous possédez, en appartement ou en maison.',
    icone: <path d="M3 11l9-7 9 7M5 9.5V20h14V9.5M10 20v-6h4v6" />,
  },
]

const horsOffre = [
  'Une résidence secondaire',
  'Un logement que vous mettez en location',
  'Un local professionnel',
]

const garanties = [
  {
    titre: 'Incendie et explosion',
    texte: 'Votre logement et vos meubles, même en cas de court-circuit.',
    icone: <path d="M12 3c1 3 5 5 5 10a5 5 0 01-10 0c0-2 1-3.5 2-4.5 0 2 1 3 2 3 0-3-1-5 1-8.5z" />,
  },
  {
    titre: 'Dégâts des eaux',
    texte: "Fuite, infiltration ou débordement, chez vous ou chez le voisin.",
    icone: <path d="M12 3s-6 7-6 11a6 6 0 0012 0c0-4-6-11-6-11z" />,
  },
  {
    titre: 'Vol et vandalisme',
    texte: 'Vos biens volés ou abîmés, portes et serrures comprises.',
    icone: (
      <>
        <rect x="5" y="11" width="14" height="10" rx="2" />
        <path d="M8 11V8a4 4 0 018 0v3" />
      </>
    ),
  },
  {
    titre: 'Tempête, grêle et neige',
    texte: 'Toiture, fenêtres et volets endommagés par les intempéries.',
    icone: <path d="M7 15a4 4 0 01.5-8A5 5 0 0117 8a3.5 3.5 0 010 7H7zM9 18l-1 2M13 18l-1 2M17 18l-1 2" />,
  },
  {
    titre: 'Catastrophes naturelles',
    texte: 'Inondation, sécheresse ou séisme reconnus par arrêté.',
    icone: <path d="M3 17c2-2 4-2 6 0s4 2 6 0 4-2 6 0M3 12c2-2 4-2 6 0s4 2 6 0 4-2 6 0" />,
  },
  {
    titre: 'Responsabilité civile',
    texte: 'Les dommages que vous ou votre famille causez à autrui.',
    icone: (
      <>
        <circle cx="9" cy="8" r="3.5" />
        <path d="M3 20c0-3.5 2.7-6 6-6s6 2.5 6 6M16 4.5a3.5 3.5 0 010 7M18 14c2 .8 3 3 3 6" />
      </>
    ),
  },
]

const options = ['Objets de valeur', 'Jardin', 'Piscine']

const nonCouvert = [
  'Les dommages causés volontairement',
  "L'usure ou le défaut d'entretien du logement",
  'Le matériel professionnel',
]

const questions = [
  {
    question: 'Dois-je créer un compte pour connaître mon prix ?',
    reponse:
      "Non. Le prix de départ est affiché sur cette page ; le compte n'est demandé qu'au moment de souscrire.",
  },
  {
    question: 'De quoi dépend le prix exact ?',
    reponse:
      'De la surface du logement, de la ville, de la valeur de vos biens et des options choisies.',
  },
  {
    question: 'Puis-je résilier quand je veux ?',
    reponse:
      "Oui, après un an de contrat, à tout moment et sans frais. La première année, en cas de déménagement ou de changement de situation.",
  },
  {
    question: 'Quand reçois-je mon attestation ?',
    reponse:
      'Immédiatement : elle est disponible dans votre espace dès la signature du contrat.',
  },
]

function LandingHabitation() {
  return (
    <div className="bg-white font-sans text-encre leading-relaxed">
      <header className="border-b border-bordure">
        <div className="mx-auto flex max-w-6xl flex-wrap items-center justify-between gap-4 px-6 py-4">
          <a
            href="#"
            className="flex items-center gap-2.5 text-2xl font-extrabold text-encre no-underline"
          >
            <span className="flex size-10 items-center justify-center rounded-full bg-rouge text-creme">
              <svg viewBox="0 0 24 24" aria-hidden="true" className="size-5">
                <path
                  d="M12 2.5l8 3v6.5c0 5-3.5 8.5-8 9.5-4.5-1-8-4.5-8-9.5V5.5z"
                  className="fill-none stroke-current [stroke-width:2.4] [stroke-linejoin:round]"
                />
                <path
                  d="M12 6v11.5c-2.8-.9-4.8-3.2-4.8-6.3V8z"
                  className="fill-current"
                />
              </svg>
            </span>
            Assur'Easy
          </a>
          <nav
            aria-label="Navigation principale"
            className="flex flex-wrap gap-7 text-[15px] font-semibold"
          >
            {[
              ['#pour-qui', 'Pour qui ?'],
              ['#garanties', 'Garanties'],
              ['#prix', 'Prix'],
              ['#questions', 'Questions'],
            ].map(([lien, libelle]) => (
              <a
                key={lien}
                href={lien}
                className="text-encre no-underline hover:text-rouge"
              >
                {libelle}
              </a>
            ))}
          </nav>
        </div>
      </header>

      <main>
        <section className="mx-auto flex max-w-6xl flex-wrap items-center gap-14 px-6 pt-16 pb-20">
          <div className="flex flex-[1_1_480px] flex-col gap-5">
            <p className="text-sm font-extrabold tracking-[0.08em] text-rouge uppercase">
              Assurance habitation
            </p>
            <h1 className="text-4xl leading-[1.05] font-extrabold tracking-tight sm:text-[56px]">
              Votre logement protégé, sans mauvaise surprise.
            </h1>
            <p className="max-w-xl text-lg text-gris">
              Ce qui est couvert, ce qui ne l'est pas, et combien ça coûte :
              tout est sur cette page. Pas besoin de créer un compte ni
              d'appeler un conseiller.
            </p>
            <div className="flex flex-wrap gap-3 pt-2">
              <a
                href="#garanties"
                className="rounded-lg bg-rouge px-6 py-4 font-bold text-white no-underline hover:bg-rouge-fonce"
              >
                Voir ce qui est couvert
              </a>
              <a
                href="#prix"
                className="rounded-lg border border-encre px-6 py-4 font-bold text-encre no-underline"
              >
                Voir le prix
              </a>
            </div>
          </div>

          <aside
            aria-labelledby="titre-resume"
            className="flex-[1_1_380px] rounded-xl border border-t-[6px] border-bordure border-t-rouge bg-white p-8 shadow-[0_16px_40px_rgba(0,0,0,0.08)]"
          >
            <h2 id="titre-resume" className="text-xl font-extrabold">
              L'offre en bref
            </h2>
            <dl className="mt-4 divide-y divide-bordure">
              {resume.map((ligne) => (
                <div
                  key={ligne.libelle}
                  className="flex justify-between gap-4 py-3"
                >
                  <dt className="text-gris">{ligne.libelle}</dt>
                  <dd className="text-right font-bold">{ligne.valeur}</dd>
                </div>
              ))}
            </dl>
            <p className="mt-4 rounded-lg bg-rouge-pale p-4 text-center">
              À partir de{' '}
              <strong className="text-2xl text-rouge">[PRIX] €</strong> /mois
            </p>
          </aside>
        </section>

        <section id="pour-qui" className="bg-fond">
          <div className="mx-auto flex max-w-6xl flex-col gap-10 px-6 py-24">
            <h2 className="text-3xl font-extrabold tracking-tight sm:text-[42px]">
              Est-ce fait pour vous ?
            </h2>
            <div className="grid gap-5 md:grid-cols-3">
              {profils.map((profil) => (
                <div
                  key={profil.titre}
                  className="flex flex-col gap-3 rounded-xl bg-white p-7"
                >
                  <span className="flex size-12 items-center justify-center rounded-xl bg-rouge-pale text-rouge">
                    <Icone>{profil.icone}</Icone>
                  </span>
                  <h3 className="text-xl font-extrabold">{profil.titre}</h3>
                  <p className="text-gris">{profil.texte}</p>
                </div>
              ))}
              <div className="flex flex-col gap-3 rounded-xl border border-bordure bg-white p-7">
                <h3 className="text-xl font-extrabold">
                  Cette offre ne couvre pas
                </h3>
                <ul className="flex flex-col gap-2">
                  {horsOffre.map((cas) => (
                    <li key={cas} className="flex gap-2 text-gris">
                      <Icone className="mt-0.5 size-5 text-gris">{croix}</Icone>
                      {cas}
                    </li>
                  ))}
                </ul>
              </div>
            </div>
          </div>
        </section>

        <section
          id="garanties"
          className="mx-auto flex max-w-6xl flex-col gap-10 px-6 py-24"
        >
          <h2 className="text-3xl font-extrabold tracking-tight sm:text-[42px]">
            Ce qui est inclus dans votre contrat
          </h2>
          <ul className="grid gap-5 sm:grid-cols-2 lg:grid-cols-3">
            {garanties.map((garantie) => (
              <li
                key={garantie.titre}
                className="flex gap-4 rounded-xl border border-bordure p-6"
              >
                <span className="flex size-12 flex-none items-center justify-center rounded-xl bg-rouge text-white">
                  <Icone>{garantie.icone}</Icone>
                </span>
                <div>
                  <h3 className="text-lg font-extrabold">{garantie.titre}</h3>
                  <p className="text-gris">{garantie.texte}</p>
                </div>
              </li>
            ))}
          </ul>

          <div className="grid gap-5 md:grid-cols-2">
            <div className="rounded-xl bg-fond p-7">
              <h3 className="text-xl font-extrabold">En option</h3>
              <ul className="mt-3 flex flex-wrap gap-2">
                {options.map((option) => (
                  <li
                    key={option}
                    className="rounded-full border border-bordure bg-white px-4 py-2 font-semibold"
                  >
                    + {option}
                  </li>
                ))}
              </ul>
            </div>
            <div className="rounded-xl bg-fond p-7">
              <h3 className="text-xl font-extrabold">Ce qui n'est pas couvert</h3>
              <ul className="mt-3 flex flex-col gap-2">
                {nonCouvert.map((cas) => (
                  <li key={cas} className="flex gap-2 text-gris">
                    <Icone className="mt-0.5 size-5">{croix}</Icone>
                    {cas}
                  </li>
                ))}
              </ul>
              <p className="mt-3 text-sm text-gris">
                Franchise : [MONTANT] € par sinistre.
              </p>
            </div>
          </div>
        </section>

        <section id="prix" className="bg-rouge text-white">
          <div className="mx-auto flex max-w-6xl flex-wrap items-center justify-between gap-8 px-6 py-16">
            <div className="flex flex-col gap-2">
              <h2 className="text-3xl font-extrabold tracking-tight sm:text-[42px]">
                À partir de [PRIX] € par mois
              </h2>
              <p className="max-w-xl text-white/85">
                Le prix exact dépend de la surface, de la ville, de la valeur de
                vos biens et des options choisies.
              </p>
            </div>
            <ul className="flex flex-col gap-2 font-semibold">
              {[
                'Sans engagement',
                'Paiement mensuel ou annuel',
                'Attestation immédiate',
              ].map((argument) => (
                <li key={argument} className="flex items-center gap-2">
                  <Icone className="size-5">{coche}</Icone>
                  {argument}
                </li>
              ))}
            </ul>
          </div>
        </section>

        <section
          id="questions"
          className="mx-auto flex max-w-3xl flex-col gap-8 px-6 py-24"
        >
          <h2 className="text-3xl font-extrabold tracking-tight sm:text-[42px]">
            Vos questions
          </h2>
          <div className="flex flex-col gap-3">
            {questions.map((item) => (
              <details
                key={item.question}
                className="group rounded-xl border border-bordure p-5 open:bg-fond"
              >
                <summary className="flex cursor-pointer list-none items-center justify-between gap-4 font-bold">
                  {item.question}
                  <span
                    aria-hidden="true"
                    className="text-2xl text-rouge transition group-open:rotate-45"
                  >
                    +
                  </span>
                </summary>
                <p className="mt-3 text-gris">{item.reponse}</p>
              </details>
            ))}
          </div>
        </section>
      </main>

      <footer className="bg-encre text-[#cfcfcf]">
        <div className="mx-auto flex max-w-6xl flex-wrap justify-between gap-4 px-6 py-10 text-sm">
          <p>
            © Assur'Easy · ORIAS n° [NUMÉRO] · Entreprise régie par le Code des
            assurances
          </p>
          <div className="flex flex-wrap gap-5">
            <a href="#" className="text-white">
              Mentions légales
            </a>
            <a href="#" className="text-white">
              Données personnelles
            </a>
            <a href="#" className="text-white">
              Réclamations
            </a>
          </div>
        </div>
      </footer>
    </div>
  )
}

export default LandingHabitation
