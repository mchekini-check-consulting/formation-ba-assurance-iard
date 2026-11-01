import { cleanup, render, screen, within } from '@testing-library/react'
import { afterEach, describe, expect, it } from 'vitest'
import LandingHabitation from './LandingHabitation'

afterEach(cleanup)

describe('LandingHabitation', () => {
  it("résume l'offre dès le haut de la page", () => {
    render(<LandingHabitation />)
    const resume = screen.getByRole('complementary', { name: "L'offre en bref" })
    expect(within(resume).getByText('Locataires et propriétaires')).toBeTruthy()
    expect(within(resume).getByText('Appartement ou maison')).toBeTruthy()
    expect(within(resume).getByText('[PRIX] €')).toBeTruthy()
  })

  it('indique pour qui est faite l’offre et ce qu’elle ne couvre pas', () => {
    render(<LandingHabitation />)
    expect(screen.getByRole('heading', { name: 'Vous êtes locataire' })).toBeTruthy()
    expect(screen.getByRole('heading', { name: 'Vous êtes propriétaire' })).toBeTruthy()
    expect(screen.getByText('Une résidence secondaire')).toBeTruthy()
  })

  it('liste les six garanties incluses', () => {
    render(<LandingHabitation />)
    for (const garantie of [
      'Incendie et explosion',
      'Dégâts des eaux',
      'Vol et vandalisme',
      'Tempête, grêle et neige',
      'Catastrophes naturelles',
      'Responsabilité civile',
    ]) {
      expect(screen.getByRole('heading', { name: garantie })).toBeTruthy()
    }
  })

  it('montre le prix sans demander de compte', () => {
    render(<LandingHabitation />)
    expect(
      screen.getByRole('heading', { name: 'À partir de [PRIX] € par mois' }),
    ).toBeTruthy()
    expect(
      screen.getByText('Dois-je créer un compte pour connaître mon prix ?'),
    ).toBeTruthy()
  })

  it('mène aux sections depuis le menu', () => {
    render(<LandingHabitation />)
    const menu = screen.getByRole('navigation', { name: 'Navigation principale' })
    const liens = within(menu)
      .getAllByRole('link')
      .map((lien) => lien.getAttribute('href'))
    expect(liens).toEqual(['#pour-qui', '#garanties', '#prix', '#questions'])
    for (const lien of liens) {
      expect(document.querySelector(lien)).toBeTruthy()
    }
  })
})
